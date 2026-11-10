package com.shareshelf.model;

/** What the owner is offering. Only BORROW listings come back, so only they get a return date. */
public enum ListingType {
    BORROW("Borrow"),
    EXCHANGE("Exchange"),
    DONATE("Donate"),
    GIVEAWAY("Giveaway");

    private final String label;

    ListingType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /** Parses a form value case-insensitively; returns null when it is not a valid type. */
    public static ListingType fromString(String value) {
        if (value == null) {
            return null;
        }
        for (ListingType t : values()) {
            if (t.name().equalsIgnoreCase(value.trim())) {
                return t;
            }
        }
        return null;
    }
}
