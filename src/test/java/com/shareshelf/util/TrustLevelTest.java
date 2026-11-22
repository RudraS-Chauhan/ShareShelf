package com.shareshelf.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrustLevelTest {

    @Test
    void levelsFollowCompletedExchanges() {
        assertEquals("Newcomer", TrustLevel.of(0).getName());
        assertEquals("Newcomer", TrustLevel.of(-5).getName());
        assertEquals("Sharer", TrustLevel.of(1).getName());
        assertEquals("Sharer", TrustLevel.of(2).getName());
        assertEquals("Trusted sharer", TrustLevel.of(AppConstants.BADGE_MIN_EXCHANGES).getName());
        assertEquals("Campus helper", TrustLevel.of(6).getName());
        assertEquals("Campus star", TrustLevel.of(10).getName());
        assertEquals(5, TrustLevel.of(500).getLevel());
    }

    @Test
    void progressTowardsTheNextLevel() {
        TrustLevel t = TrustLevel.of(1);          // Sharer (1) -> Trusted sharer (3)
        assertEquals(2, t.getToNext());
        assertEquals("Trusted sharer", t.getNextName());
        assertEquals(0, t.getPercent());
        assertEquals(50, TrustLevel.of(2).getPercent());
        assertFalse(t.isMax());
        TrustLevel top = TrustLevel.of(12);
        assertTrue(top.isMax());
        assertEquals(0, top.getToNext());
        assertEquals(100, top.getPercent());
    }
}
