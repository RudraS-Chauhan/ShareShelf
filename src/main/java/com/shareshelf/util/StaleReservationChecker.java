package com.shareshelf.util;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntSupplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The background worker that flags overdue requests (the "Multithreading" requirement).
 *
 * It is a plain Runnable. AppLifecycleListener schedules it on a daemon thread, so the
 * scan keeps running every few minutes while students use the site normally. Each run asks the
 * DAO to mark every ACCEPTED request whose expected return date has passed as OVERDUE.
 *
 * Thread safety: the web threads only READ lastRun / totalFlagged (for the Reports page) while the
 * worker thread WRITES them, so they are volatile / atomic.
 */
public class StaleReservationChecker implements Runnable {

    private static final Logger LOG = Logger.getLogger(StaleReservationChecker.class.getName());

    private final IntSupplier overdueMarker;          // e.g. requestDao::markOverdue
    private final AtomicInteger totalFlagged = new AtomicInteger();
    private volatile LocalDateTime lastRun;           // null until the first scan finishes
    private volatile String lastError;                // null when the last scan succeeded

    /** @param overdueMarker does the actual work and returns how many requests were flagged. */
    public StaleReservationChecker(IntSupplier overdueMarker) {
        this.overdueMarker = overdueMarker;
    }

    @Override
    public void run() {
        // Never let an exception escape: a scheduled task that throws is silently cancelled,
        // and the site would stop flagging overdue requests until the next restart.
        try {
            int flagged = overdueMarker.getAsInt();
            totalFlagged.addAndGet(flagged);
            lastError = null;
            if (flagged > 0) {
                LOG.info("Overdue check: flagged " + flagged + " request(s) as OVERDUE");
            }
        } catch (RuntimeException e) {
            lastError = e.getMessage();
            LOG.log(Level.WARNING, "Overdue check failed; will retry on the next cycle", e);
        } finally {
            lastRun = LocalDateTime.now();
        }
    }

    public LocalDateTime getLastRun() {
        return lastRun;
    }

    public int getTotalFlagged() {
        return totalFlagged.get();
    }

    public String getLastError() {
        return lastError;
    }
}
