package com.shareshelf.util;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/** Unit tests for the input-validation rules (no database or server needed). */
class ValidationUtilTest {

    @Test
    void studentIdIsTrimmedAndUpperCased() {
        assertEquals("25SCSE1180735", ValidationUtil.normalizeStudentId("  25scse1180735 "));
        assertEquals("", ValidationUtil.normalizeStudentId(null));
    }

    @Test
    void studentIdAcceptsOnlyLettersAndDigits() {
        assertTrue(ValidationUtil.isValidStudentId("25SCSE1180735"));
        assertFalse(ValidationUtil.isValidStudentId("25SC"));            // too short
        assertFalse(ValidationUtil.isValidStudentId("25SCSE-1180735"));  // illegal character
        assertFalse(ValidationUtil.isValidStudentId("25SCSE1180735'; DROP TABLE students;--"));
        assertFalse(ValidationUtil.isValidStudentId(null));
    }

    @Test
    void onlyCollegeEmailsAreAccepted() {
        assertTrue(ValidationUtil.isCollegeEmail("rudra.25scse1180735@galgotiasuniversity.ac.in"));
        assertTrue(ValidationUtil.isCollegeEmail("  RUDRA.25SCSE1180735@GalgotiasUniversity.AC.IN "));
        assertFalse(ValidationUtil.isCollegeEmail("rudra@gmail.com"));
        assertFalse(ValidationUtil.isCollegeEmail("@galgotiasuniversity.ac.in"));
        assertFalse(ValidationUtil.isCollegeEmail("rudra@galgotiasuniversity.ac.in.evil.com"));
        assertFalse(ValidationUtil.isCollegeEmail(null));
    }

    @Test
    void emailMustContainTheStudentsOwnAdmissionNumber() {
        assertTrue(ValidationUtil.emailContainsId("rudra.25scse1180735@galgotiasuniversity.ac.in", "25SCSE1180735"));
        assertFalse(ValidationUtil.emailContainsId("rudra.25scse9999999@galgotiasuniversity.ac.in", "25SCSE1180735"));
        assertFalse(ValidationUtil.emailContainsId("rudra@galgotiasuniversity.ac.in", ""));
    }

    @Test
    void passwordNeedsEightCharsWithLetterAndDigit() {
        assertTrue(ValidationUtil.isStrongPassword("Passw0rd!x"));
        assertFalse(ValidationUtil.isStrongPassword("short1"));
        assertFalse(ValidationUtil.isStrongPassword("onlyletters"));
        assertFalse(ValidationUtil.isStrongPassword("12345678"));
        assertFalse(ValidationUtil.isStrongPassword(null));
    }

    @Test
    void whatsappNumbersAreNormalisedForWaMe() {
        assertEquals("919876543210", ValidationUtil.normalizeWhatsapp("9876543210"));
        assertEquals("919876543210", ValidationUtil.normalizeWhatsapp("+91 98765 43210"));
        assertEquals("919876543210", ValidationUtil.normalizeWhatsapp("919876543210"));
        assertNull(ValidationUtil.normalizeWhatsapp("12345"));
        assertNull(ValidationUtil.normalizeWhatsapp("1234567890"));  // Indian mobiles start with 6-9
        assertNull(ValidationUtil.normalizeWhatsapp(null));
    }

    @Test
    void helpersAreNullSafe() {
        assertTrue(ValidationUtil.isBlank("   "));
        assertTrue(ValidationUtil.isBlank(null));
        assertFalse(ValidationUtil.isBlank(" a "));
        assertEquals("abc", ValidationUtil.limit("  abcdef ", 3));
        assertEquals("", ValidationUtil.limit(null, 5));
        assertEquals(7, ValidationUtil.parseIntOr("oops", 7));
        assertEquals(3, ValidationUtil.parseIntOr("3", 7));
    }
}
