package com.shareshelf.util;

import java.util.regex.Pattern;

/**
 * Small, pure helper methods for validating form input. No servlet or database code here,
 * which makes them easy to unit test.
 */
public final class ValidationUtil {

    private static final Pattern STUDENT_ID = Pattern.compile("^[A-Z0-9]{6,20}$");
    private static final Pattern PASSWORD_LETTER = Pattern.compile(".*[A-Za-z].*");
    private static final Pattern PASSWORD_DIGIT = Pattern.compile(".*\\d.*");

    private ValidationUtil() {
    }

    public static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    /** Trims and upper-cases an admission number, e.g. " 25scse1180735 " -> "25SCSE1180735". */
    public static String normalizeStudentId(String raw) {
        return raw == null ? "" : raw.trim().toUpperCase();
    }

    /** Admission numbers are 6-20 letters/digits only (also keeps odd characters out of the DB). */
    public static boolean isValidStudentId(String id) {
        return id != null && STUDENT_ID.matcher(id).matches();
    }

    /** Only college addresses may register. */
    public static boolean isCollegeEmail(String email) {
        return email != null && email.trim().toLowerCase().endsWith(AppConstants.COLLEGE_EMAIL_DOMAIN)
                && email.indexOf('@') > 0;
    }

    /** The admission number must appear in the college email (e.g. rudra.25scse1180735@...). */
    public static boolean emailContainsId(String email, String studentId) {
        return email != null && studentId != null && !studentId.isEmpty()
                && email.toLowerCase().contains(studentId.toLowerCase());
    }

    /** At least 8 characters with a letter and a digit. */
    public static boolean isStrongPassword(String password) {
        return password != null && password.length() >= 8
                && PASSWORD_LETTER.matcher(password).matches()
                && PASSWORD_DIGIT.matcher(password).matches();
    }

    /**
     * Normalises an Indian mobile number to the format wa.me needs (91 + 10 digits).
     * Accepts "9876543210", "919876543210", "+91 98765 43210". Returns null if it is not valid.
     */
    public static String normalizeWhatsapp(String raw) {
        if (raw == null) {
            return null;
        }
        String digits = raw.replaceAll("[^0-9]", "");
        if (digits.length() == 10) {
            digits = "91" + digits;
        }
        if (digits.length() == 12 && digits.startsWith("91") && digits.charAt(2) >= '6') {
            return digits;
        }
        return null;
    }

    /** Returns the value cut to at most max characters (null-safe, trimmed). */
    public static String limit(String s, int max) {
        if (s == null) {
            return "";
        }
        String t = s.trim();
        return t.length() <= max ? t : t.substring(0, max);
    }

    /** Parses an int, returning fallback when the text is missing or not a number. */
    public static int parseIntOr(String s, int fallback) {
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException | NullPointerException e) {
            return fallback;
        }
    }
}
