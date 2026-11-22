package com.shareshelf.util;

import java.util.function.LongSupplier;
import java.util.function.Supplier;

/**
 * Remembers one value for a short time so a busy public page does not hit the database on every visit.
 * If the loader fails and an older value exists, the old value is served instead of an error.
 */
public class TtlCache<T> {

    private final long ttlMillis;
    private final Supplier<T> loader;
    private final LongSupplier clock;
    private T value;
    private long loadedAt;
    private boolean loaded;

    public TtlCache(long ttlMillis, Supplier<T> loader) {
        this(ttlMillis, loader, System::currentTimeMillis);
    }

    /** Clock is injectable so tests do not have to wait. */
    TtlCache(long ttlMillis, Supplier<T> loader, LongSupplier clock) {
        this.ttlMillis = ttlMillis;
        this.loader = loader;
        this.clock = clock;
    }

    public synchronized T get() {
        long now = clock.getAsLong();
        if (!loaded || now - loadedAt >= ttlMillis) {
            try {
                value = loader.get();
                loadedAt = now;
                loaded = true;
            } catch (RuntimeException e) {
                if (!loaded) {
                    throw e;   // nothing older to fall back on
                }
            }
        }
        return value;
    }
}
