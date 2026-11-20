package com.shareshelf.util;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class StudentIdInfoTest {

    @Test
    void readsBatchSchoolAndYearOfStudy() {
        StudentIdInfo i = StudentIdInfo.parse("25SCSE1180735");
        assertEquals(2025, i.getAdmissionYear());
        assertEquals(2029, i.getBatchYear());
        assertEquals("SCSE", i.getSchoolCode());
        assertEquals("School of Computer Science and Engineering", i.getSchoolName());
        assertEquals(2, i.studyYear(LocalDate.of(2026, 11, 20)), "Nov 2026 is the second year of the 2025 batch");
        assertEquals(1, i.studyYear(LocalDate.of(2025, 8, 1)));
        assertEquals(1, i.studyYear(LocalDate.of(2024, 1, 1)), "never below 1");
        assertEquals(4, i.studyYear(LocalDate.of(2029, 1, 10)));
    }

    @Test
    void worksForOtherBatchesAndSchoolsAndIgnoresOddShapes() {
        assertEquals(2028, StudentIdInfo.parse("24soai1234567").getBatchYear(), "lower case is fine");
        assertEquals("School of Artificial Intelligence", StudentIdInfo.parse("24SOAI1234567").getSchoolName());
        assertEquals("XYZ", StudentIdInfo.parse("26XYZ1234567").getSchoolName(), "unknown school shows its code");
        assertTrue(StudentIdInfo.parse("26XYZ1234567").summary().startsWith("Class of 2030"));
        assertNull(StudentIdInfo.parse("ADMIN"));
        assertNull(StudentIdInfo.parse(null));
        // registration itself stays permissive, so odd but valid IDs still work
        assertTrue(ValidationUtil.isValidStudentId("22BTECH123456"));
    }
}
