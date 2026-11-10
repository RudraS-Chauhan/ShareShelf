package com.shareshelf.util;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/** Unit tests for password hashing. */
class PasswordUtilTest {

    @Test
    void correctPasswordVerifies() {
        String stored = PasswordUtil.hash("Passw0rd!x");
        assertTrue(PasswordUtil.verify("Passw0rd!x", stored));
    }

    @Test
    void wrongPasswordIsRejected() {
        String stored = PasswordUtil.hash("Passw0rd!x");
        assertFalse(PasswordUtil.verify("passw0rd!x", stored));
        assertFalse(PasswordUtil.verify("", stored));
    }

    @Test
    void hashIsNeverThePlaintextAndIsSalted() {
        String a = PasswordUtil.hash("Passw0rd!x");
        String b = PasswordUtil.hash("Passw0rd!x");
        assertFalse(a.contains("Passw0rd"));
        assertNotEquals(a, b, "same password must produce different hashes (random salt)");
        assertTrue(a.length() <= 255, "must fit the password column");
    }

    @Test
    void malformedStoredValuesNeverVerify() {
        assertFalse(PasswordUtil.verify("Passw0rd!x", "Passw0rd!x"));      // legacy plaintext
        assertFalse(PasswordUtil.verify("Passw0rd!x", "pbkdf2$abc$###$###"));
        assertFalse(PasswordUtil.verify("Passw0rd!x", null));
        assertFalse(PasswordUtil.verify(null, PasswordUtil.hash("x1234567")));
    }
}
