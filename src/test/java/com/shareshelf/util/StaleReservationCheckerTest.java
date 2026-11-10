package com.shareshelf.util;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/** Unit tests for the overdue-checker task, using a fake "marker" instead of a database. */
class StaleReservationCheckerTest {

    @Test
    void countsFlaggedRequestsAndRecordsTheScanTime() {
        StaleReservationChecker checker = new StaleReservationChecker(() -> 2);
        assertNull(checker.getLastRun());
        checker.run();
        checker.run();
        assertEquals(4, checker.getTotalFlagged());
        assertNotNull(checker.getLastRun());
        assertNull(checker.getLastError());
    }

    @Test
    void aFailingScanDoesNotKillTheThreadAndRecoversNextTime() {
        AtomicInteger calls = new AtomicInteger();
        StaleReservationChecker checker = new StaleReservationChecker(() -> {
            if (calls.incrementAndGet() == 1) {
                throw new DataAccessException("database is down", new RuntimeException());
            }
            return 1;
        });

        assertDoesNotThrow(checker::run);                 // exception is caught, not propagated
        assertEquals("database is down", checker.getLastError());
        assertNotNull(checker.getLastRun());

        checker.run();                                    // next cycle works again
        assertNull(checker.getLastError());
        assertEquals(1, checker.getTotalFlagged());
    }
}
