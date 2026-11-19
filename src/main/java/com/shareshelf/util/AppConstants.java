package com.shareshelf.util;

import java.time.ZoneId;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Fixed vocabularies used by forms, validation and servlets (one source of truth). */
public final class AppConstants {

    private AppConstants() {
    }

    /** Only addresses on this domain may register. */
    public static final String COLLEGE_EMAIL_DOMAIN = "@galgotiasuniversity.ac.in";

    /** The five categories (Hostel Items was dropped on purpose, see FINAL_SPEC.md section 2). */
    public static final Set<String> CATEGORIES = set(
            "Academic Materials", "Lab Equipment", "Stationery",
            "Event Supplies", "Electronics & Accessories");

    public static final Set<String> CONDITIONS = set("New", "Like New", "Good", "Fair");

    /**
     * Where handovers happen: fixed public campus spots (no GPS, no home addresses).
     * The first five are academic blocks, which also need a floor (see BLOCK_LOCATIONS).
     */
    public static final Set<String> LOCATIONS = set(
            "AI-DS Block", "A Block", "B Block", "C Block", "D Block",
            "Cafeteria", "Library",
            "Ground in front of C Block", "Ground in front of D Block");

    /** Spots inside a building: the student must also pick a floor. */
    public static final Set<String> BLOCK_LOCATIONS = set(
            "AI-DS Block", "A Block", "B Block", "C Block", "D Block");

    /** Floors offered for a block. Edit this list if a block has more or fewer floors. */
    public static final Set<String> FLOORS = set(
            "Ground floor", "1st floor", "2nd floor", "3rd floor", "4th floor", "5th floor");

    /** Longest landmark text accepted (e.g. "near the water cooler"). */
    public static final int MAX_LANDMARK = 60;

    /**
     * Builds the single text stored in resource_items.location, e.g.
     * "C Block, 2nd floor - near the water cooler". Floor is only used for blocks.
     */
    public static String buildLocation(String place, String floor, String landmark) {
        StringBuilder sb = new StringBuilder(place);
        if (BLOCK_LOCATIONS.contains(place) && floor != null && !floor.isEmpty()) {
            sb.append(", ").append(floor);
        }
        if (landmark != null && !landmark.isEmpty()) {
            sb.append(" - ").append(landmark);
        }
        return sb.toString();
    }

    /** Borrow durations (in days) offered in the forms. */
    public static final List<Integer> BORROW_DAYS = List.of(1, 3, 7, 14, 30);

    public static final int DEFAULT_BORROW_DAYS = 7;

    /** Upper limit accepted by the server even if the form is tampered with. */
    public static final int MAX_BORROW_DAYS = 90;

    /** All times shown and compared in the app are campus time (the server itself may run in UTC). */
    public static final ZoneId CAMPUS_ZONE = ZoneId.of("Asia/Kolkata");

    /**
     * Borrow durations in HOURS, shortest first: a class slot (1-4 h), a day, then days to a month.
     * One list drives the listing form, the owner's accept form and the stored due time.
     */
    public static final List<Integer> DURATION_HOURS = List.of(1, 2, 4, 8, 24, 72, 168, 336, 720);

    public static final int DEFAULT_DURATION_HOURS = 168;   // 7 days

    /** Longest duration the server accepts, even if the form is tampered with. */
    public static final int MAX_DURATION_HOURS = MAX_BORROW_DAYS * 24;

    /** How far ahead a meeting time may be set, in days. */
    public static final int MAX_MEET_DAYS_AHEAD = 14;

    /** Longest meeting note accepted (e.g. "Table opposite the left water outlet, blue hoodie"). */
    public static final int MAX_MEET_NOTE = 200;

    /** "2 hours", "1 day", "3 days": the text shown for a duration given in hours. */
    public static String durationLabel(int hours) {
        if (hours < 24 || hours % 24 != 0) {
            return hours + (hours == 1 ? " hour" : " hours");
        }
        int days = hours / 24;
        return days + (days == 1 ? " day" : " days");
    }

    /** hours -> label, in display order, for the dropdowns. */
    public static Map<Integer, String> durationOptions() {
        Map<Integer, String> options = new LinkedHashMap<>();
        for (int h : DURATION_HOURS) {
            options.put(h, durationLabel(h));
        }
        return Collections.unmodifiableMap(options);
    }

    private static Set<String> set(String... values) {
        Set<String> s = new LinkedHashSet<>();
        Collections.addAll(s, values);
        return Collections.unmodifiableSet(s);
    }

    // ---- Phase 2: QR handover, ratings ----
    /** How long a QR token / 6-digit code stays valid. */
    public static final int HANDOVER_MINUTES = 10;
    /** Wrong 6-digit guesses allowed before the code is locked. */
    public static final int MAX_CODE_ATTEMPTS = 5;
    /** Condition the owner records when the item comes back. */
    public static final List<String> RETURN_CONDITIONS = List.of("OK", "DAMAGED", "MISSING");
    public static final int MAX_RATING_COMMENT = 200;
    /** A profile badge appears after this many completed exchanges. */
    public static final int BADGE_MIN_EXCHANGES = 3;
    public static final double BADGE_MIN_STARS = 4.0;
}
