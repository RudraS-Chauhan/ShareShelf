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
     * Where handovers happen. A fixed list of public campus spots keeps exact locations
     * out of the app (no GPS, no home addresses) and gives students a sensible default.
     */
    public static final Set<String> LOCATIONS = set(
            "Library", "Main Gate", "Canteen / Food Court", "Admin Block",
            "Academic Block lobby", "Hostel gate", "Sports complex");

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
