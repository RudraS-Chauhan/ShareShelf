package com.shareshelf.util;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

/**
 * A small connection pool. Opening a database connection to a cloud database costs a TLS handshake and a login
 * (often 0.3 to 1 second on free hosting), and one page can run several queries, so opening a fresh connection
 * for every query made every click slow. This keeps a few connections open and lends them out.
 *
 * <p>The DAO code does not change: it still calls {@code getConnection()} and closes the result with
 * try-with-resources. Closing a pooled connection hands it back instead of closing it. On return, any unfinished
 * transaction is rolled back and auto-commit is switched back on, so one request can never leak state into the next.
 * Connections older than {@code maxLifetimeMs} are replaced, and one that sat idle for a while is checked before reuse,
 * because free database tiers drop quiet connections.
 */
public class ConnectionPool {

    /** How the pool opens a brand-new physical connection. */
    public interface Opener {
        Connection open() throws SQLException;
    }

    private static final class Pooled {
        final Connection raw;
        final long createdAt;
        long lastUsed;

        Pooled(Connection raw, long now) {
            this.raw = raw;
            this.createdAt = now;
            this.lastUsed = now;
        }
    }

    private final Opener opener;
    private final Semaphore permits;
    private final long maxLifetimeMs;
    private final long validateAfterIdleMs;
    private final long waitMs;
    private final LongSupplier clock;
    private final Deque<Pooled> idle = new ArrayDeque<>();
    private volatile boolean shutDown;

    public ConnectionPool(Opener opener, int maxSize, long maxLifetimeMs, long validateAfterIdleMs, long waitMs) {
        this(opener, maxSize, maxLifetimeMs, validateAfterIdleMs, waitMs, System::currentTimeMillis);
    }

    /** The clock can be replaced so tests do not have to wait. */
    ConnectionPool(Opener opener, int maxSize, long maxLifetimeMs, long validateAfterIdleMs, long waitMs, LongSupplier clock) {
        this.opener = opener;
        this.permits = new Semaphore(maxSize, true);
        this.maxLifetimeMs = maxLifetimeMs;
        this.validateAfterIdleMs = validateAfterIdleMs;
        this.waitMs = waitMs;
        this.clock = clock;
    }

    /** Lends a connection. Close it (try-with-resources) to give it back. */
    public Connection getConnection() throws SQLException {
        if (shutDown) {
            throw new SQLException("Connection pool is shut down");
        }
        try {
            if (!permits.tryAcquire(waitMs, TimeUnit.MILLISECONDS)) {
                throw new SQLException("All database connections are busy, try again");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SQLException("Interrupted while waiting for a database connection", e);
        }
        try {
            Pooled p = takeUsable();
            if (p == null) {
                p = new Pooled(opener.open(), clock.getAsLong());
            }
            return wrap(p);
        } catch (SQLException | RuntimeException e) {
            permits.release();
            throw e;
        }
    }

    /** Closes every idle connection and refuses new loans (called when the app stops). */
    public void shutdown() {
        shutDown = true;
        synchronized (idle) {
            while (!idle.isEmpty()) {
                closeQuietly(idle.pollFirst().raw);
            }
        }
    }

    /** Number of connections waiting in the pool (for tests and logs). */
    public int idleCount() {
        synchronized (idle) {
            return idle.size();
        }
    }

    private Pooled takeUsable() {
        while (true) {
            Pooled p;
            synchronized (idle) {
                p = idle.pollFirst();
            }
            if (p == null) {
                return null;
            }
            long now = clock.getAsLong();
            if (now - p.createdAt >= maxLifetimeMs) {
                closeQuietly(p.raw);
                continue;
            }
            if (now - p.lastUsed >= validateAfterIdleMs) {
                try {
                    if (!p.raw.isValid(2)) {
                        closeQuietly(p.raw);
                        continue;
                    }
                } catch (SQLException e) {
                    closeQuietly(p.raw);
                    continue;
                }
            }
            return p;
        }
    }

    private void giveBack(Pooled p) {
        try {
            if (shutDown || p.raw.isClosed()) {
                closeQuietly(p.raw);
                return;
            }
            if (!p.raw.getAutoCommit()) {
                p.raw.rollback();
                p.raw.setAutoCommit(true);
            }
            p.lastUsed = clock.getAsLong();
            synchronized (idle) {
                idle.addFirst(p);
            }
        } catch (SQLException e) {
            closeQuietly(p.raw);   // a connection we cannot reset is not safe to reuse
        } finally {
            permits.release();
        }
    }

    private Connection wrap(Pooled p) {
        boolean[] closed = {false};
        InvocationHandler handler = (proxy, method, args) -> {
            String name = method.getName();
            if (name.equals("close")) {
                if (!closed[0]) {
                    closed[0] = true;
                    giveBack(p);
                }
                return null;
            }
            if (name.equals("isClosed")) {
                return closed[0] || p.raw.isClosed();
            }
            if (closed[0]) {
                throw new SQLException("Connection was already closed");
            }
            try {
                return method.invoke(p.raw, args);
            } catch (InvocationTargetException e) {
                throw e.getCause();
            }
        };
        return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(), new Class<?>[]{Connection.class}, handler);
    }

    private static void closeQuietly(Connection c) {
        try {
            c.close();
        } catch (SQLException | RuntimeException ignored) {
            // nothing useful to do
        }
    }
}
