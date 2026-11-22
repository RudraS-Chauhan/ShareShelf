package com.shareshelf.util;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TtlCacheTest {

    @Test
    void loadsOnceInsideTheWindowThenAgain() {
        AtomicLong now = new AtomicLong(0);
        AtomicInteger loads = new AtomicInteger();
        TtlCache<Integer> cache = new TtlCache<>(1000, loads::incrementAndGet, now::get);
        assertEquals(1, cache.get());
        now.set(999);
        assertEquals(1, cache.get());
        assertEquals(1, loads.get());
        now.set(1000);
        assertEquals(2, cache.get());
    }

    @Test
    void keepsOldValueWhenReloadFails() {
        AtomicLong now = new AtomicLong(0);
        AtomicInteger calls = new AtomicInteger();
        TtlCache<String> cache = new TtlCache<>(10, () -> {
            if (calls.incrementAndGet() > 1) { throw new RuntimeException("db asleep"); }
            return "fresh";
        }, now::get);
        assertEquals("fresh", cache.get());
        now.set(50);
        assertEquals("fresh", cache.get());
    }

    @Test
    void firstLoadFailureIsReported() {
        TtlCache<String> cache = new TtlCache<>(10, () -> { throw new IllegalStateException("x"); }, () -> 0);
        assertThrows(IllegalStateException.class, cache::get);
    }
}
