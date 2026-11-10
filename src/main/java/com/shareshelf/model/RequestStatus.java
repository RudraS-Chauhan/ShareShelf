package com.shareshelf.model;

/** Lifecycle of a request: PENDING -> ACCEPTED/REJECTED/CANCELLED -> COMPLETED (or OVERDUE if late). */
public enum RequestStatus {
    PENDING, ACCEPTED, REJECTED, CANCELLED, COMPLETED, OVERDUE
}
