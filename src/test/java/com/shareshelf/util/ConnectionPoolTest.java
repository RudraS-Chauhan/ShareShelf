package com.shareshelf.util;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConnectionPoolTest {

    /** A stand-in connection that records what the pool does to it. */
    static class Fake {
        boolean closed, autoCommit = true, valid = true;
        int rollbacks;
        final Connection conn = (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(),
                new Class<?>[]{Connection.class}, (p, m, a) -> {
                    switch (m.getName()) {
                        case "close": closed = true; return null;
                        case "isClosed": return closed;
                        case "getAutoCommit": return autoCommit;
                        case "setAutoCommit": autoCommit = (Boolean) a[0]; return null;
                        case "rollback": rollbacks++; return null;
                        case "isValid": return valid;
                        default: return null;
                    }
                });
    }

    private final List<Fake> opened = new ArrayList<>();
    private final AtomicLong now = new AtomicLong(0);

    private ConnectionPool pool(int max, long lifetime, long validateAfter, long waitMs) {
        return new ConnectionPool(() -> { Fake f = new Fake(); opened.add(f); return f.conn; },
                max, lifetime, validateAfter, waitMs, now::get);
    }

    @Test
    void reusesTheSameConnectionInsteadOfOpeningNewOnes() throws SQLException {
        ConnectionPool pool = pool(4, 100_000, 100_000, 100);
        for (int i = 0; i < 5; i++) {
            try (Connection c = pool.getConnection()) {
                assertFalse(c.isClosed());
            }
        }
        assertEquals(1, opened.size(), "five borrows, one real connection");
        assertEquals(1, pool.idleCount());
    }

    @Test
    void closingTwiceIsHarmlessAndClosedHandleIsUnusable() throws SQLException {
        ConnectionPool pool = pool(2, 100_000, 100_000, 100);
        Connection c = pool.getConnection();
        c.close();
        c.close();
        assertTrue(c.isClosed());
        assertEquals(1, pool.idleCount(), "returned once, not twice");
        assertThrows(SQLException.class, c::getAutoCommit);
        assertFalse(opened.get(0).closed, "the real connection stays open");
    }

    @Test
    void unfinishedTransactionIsRolledBackAndAutoCommitRestored() throws SQLException {
        ConnectionPool pool = pool(2, 100_000, 100_000, 100);
        try (Connection c = pool.getConnection()) {
            c.setAutoCommit(false);
        }
        assertEquals(1, opened.get(0).rollbacks);
        assertTrue(opened.get(0).autoCommit);
    }

    @Test
    void waitsThenFailsClearlyWhenEveryConnectionIsBusy() throws SQLException {
        ConnectionPool pool = pool(1, 100_000, 100_000, 50);
        try (Connection held = pool.getConnection()) {
            assertThrows(SQLException.class, pool::getConnection);
        }
        try (Connection again = pool.getConnection()) {   // freed, so it works again
            assertFalse(again.isClosed());
        }
    }

    @Test
    void replacesConnectionsThatAreTooOld() throws SQLException {
        ConnectionPool pool = pool(2, 1000, 100_000, 100);
        pool.getConnection().close();
        now.set(1000);
        pool.getConnection().close();
        assertEquals(2, opened.size());
        assertTrue(opened.get(0).closed, "the old one is closed for real");
    }

    @Test
    void testsLongIdleConnectionsAndDropsDeadOnes() throws SQLException {
        ConnectionPool pool = pool(2, 100_000, 500, 100);
        pool.getConnection().close();
        opened.get(0).valid = false;      // the host dropped it while idle
        now.set(600);
        try (Connection c = pool.getConnection()) {
            assertFalse(c.isClosed());
        }
        assertEquals(2, opened.size(), "dead connection replaced");
    }

    @Test
    void openFailureDoesNotLeakAPermit() {
        ConnectionPool pool = new ConnectionPool(() -> { throw new SQLException("db down"); }, 1, 1000, 1000, 50, now::get);
        assertThrows(SQLException.class, pool::getConnection);
        assertThrows(SQLException.class, pool::getConnection);   // would say "busy" if the permit had leaked
    }

    @Test
    void shutdownClosesIdleConnectionsAndRefusesNewLoans() throws SQLException {
        ConnectionPool pool = pool(2, 100_000, 100_000, 100);
        pool.getConnection().close();
        pool.shutdown();
        assertTrue(opened.get(0).closed);
        assertThrows(SQLException.class, pool::getConnection);
    }
}
