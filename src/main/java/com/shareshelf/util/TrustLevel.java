package com.shareshelf.util;

/**
 * The friendly "level" shown on the dashboard. It only counts COMPLETED exchanges (lender or borrower),
 * so it cannot be gained by listing junk or sending requests. Levels are display only: they unlock nothing.
 * Level 3 lines up with the badge rule in {@link AppConstants#BADGE_MIN_EXCHANGES}.
 */
public final class TrustLevel {

    private static final int[] FROM = {0, 1, AppConstants.BADGE_MIN_EXCHANGES, 6, 10};
    private static final String[] NAME = {"Newcomer", "Sharer", "Trusted sharer", "Campus helper", "Campus star"};
    private static final String[] ICON = {"🌱", "🤝", "🏅", "🚀", "⭐"};

    private final int completed;
    private final int index;

    private TrustLevel(int completed, int index) {
        this.completed = completed;
        this.index = index;
    }

    /** @param completed finished exchanges (negative values count as 0) */
    public static TrustLevel of(int completed) {
        int c = Math.max(0, completed);
        int idx = 0;
        for (int i = 0; i < FROM.length; i++) {
            if (c >= FROM[i]) {
                idx = i;
            }
        }
        return new TrustLevel(c, idx);
    }

    /** 1 to 5. */
    public int getLevel() { return index + 1; }
    public String getName() { return NAME[index]; }
    public String getIcon() { return ICON[index]; }
    public int getCompleted() { return completed; }
    public boolean isMax() { return index == FROM.length - 1; }

    /** Exchanges still needed for the next level, or 0 at the top level. */
    public int getToNext() { return isMax() ? 0 : FROM[index + 1] - completed; }

    public String getNextName() { return isMax() ? "" : NAME[index + 1]; }

    /** Progress inside the current level, 0 to 100 (100 at the top level). */
    public int getPercent() {
        if (isMax()) {
            return 100;
        }
        int span = FROM[index + 1] - FROM[index];
        return (int) Math.round(100.0 * (completed - FROM[index]) / span);
    }
}
