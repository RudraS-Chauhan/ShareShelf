package com.shareshelf.util;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
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

    private static Set<String> set(String... values) {
        Set<String> s = new LinkedHashSet<>();
        Collections.addAll(s, values);
        return Collections.unmodifiableSet(s);
    }
}
