package com.shareshelf.model;

/** Lifecycle of a listing: AVAILABLE -> RESERVED -> BORROWED -> (AVAILABLE again | CLOSED). */
public enum ListingStatus {
    AVAILABLE, RESERVED, BORROWED, CLOSED
}
