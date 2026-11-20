package com.shareshelf.util;

import java.time.LocalDate;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads what an admission number tells us, e.g. 25SCSE1180735:
 *   25      the year of admission (2025), so the batch is "Class of 2029" (4-year programme)
 *   SCSE    the school (School of Computer Science and Engineering)
 *   1180735 the unique serial number
 * Registration still accepts any ID of 6 to 20 letters/digits, so older and newer batches and other
 * schools work too. This class only adds friendly information when the ID follows the usual shape.
 */
public final class StudentIdInfo {

    private static final Pattern SHAPE = Pattern.compile("^(\\d{2})([A-Z]{2,8})(\\d{3,10})$");
    private static final int PROGRAMME_YEARS = 4;

    private final int admissionYear;
    private final String schoolCode;

    private StudentIdInfo(int admissionYear, String schoolCode) {
        this.admissionYear = admissionYear;
        this.schoolCode = schoolCode;
    }

    /** @return the parsed info, or null when the ID does not follow the 25SCSE1234567 shape. */
    public static StudentIdInfo parse(String studentId) {
        if (studentId == null) {
            return null;
        }
        Matcher m = SHAPE.matcher(studentId.trim().toUpperCase(Locale.ROOT));
        if (!m.matches()) {
            return null;
        }
        return new StudentIdInfo(2000 + Integer.parseInt(m.group(1)), m.group(2));
    }

    public int getAdmissionYear() { return admissionYear; }
    public int getBatchYear() { return admissionYear + PROGRAMME_YEARS; }
    public String getSchoolCode() { return schoolCode; }

    /** Full school name when we know the code, otherwise just the code. */
    public String getSchoolName() {
        return AppConstants.SCHOOLS.getOrDefault(schoolCode, schoolCode);
    }

    /** Year of study today (1 = first year). The academic year starts in July. Never below 1. */
    public int studyYear(LocalDate today) {
        int academicStart = today.getMonthValue() >= 7 ? today.getYear() : today.getYear() - 1;
        return Math.max(1, academicStart - admissionYear + 1);
    }

    /** "Class of 2029 · School of Computer Science and Engineering (SCSE)" */
    public String summary() {
        String name = getSchoolName();
        return "Class of " + getBatchYear() + " · " + (name.equals(schoolCode) ? schoolCode : name + " (" + schoolCode + ")");
    }
}
