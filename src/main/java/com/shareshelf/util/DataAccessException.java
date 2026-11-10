package com.shareshelf.util;

/**
 * Thrown by the DAO layer when the database fails unexpectedly (connection down, SQL error).
 * It is unchecked so DAO method signatures stay the same as in DAO_CONTRACT.md; the servlets'
 * base class catches it in one place and shows a friendly error page.
 *
 * Expected business outcomes (duplicate ID, nothing to update) are NOT exceptions:
 * the DAOs return false / null for those.
 */
public class DataAccessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
