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

    @Test
    void everyKnownSchoolCodeIsNamedAndParsesFromARealShapedId() {
        String[][] ids = {
                {"25GSFC2000001", "GSFC", "School of Finance and Commerce"},
                {"26BAS1100001", "BAS", "B.Sc (Maths Hons)"},
                {"25GPTC0000001", "GPTC", "Diploma (Polytechnic)"},
                {"25SODT1000001", "SODT", "School of Defence Technology"},
                {"25GSOL0000001", "GSOL", "School of Law"},
                {"24GSOB1000001", "GSOB", "School of Business (BBA)"},
                {"25SMAS1000001", "SMAS", "B.Optom (Optometry)"},
                {"25SALM1000001", "SALM", "BBA (Aviation)"},
                {"26BAS1100001", "BAS", "B.Sc (Maths Hons)"},
                {"26SOAI1000001", "SOAI", "School of Artificial Intelligence"},
                {"25SCSE1000001", "SCSE", "School of Computer Science and Engineering"}};
        for (String[] row : ids) {
            StudentIdInfo info = StudentIdInfo.parse(row[0]);
            assertEquals(row[1], info.getSchoolCode(), row[0]);
            assertEquals(row[2], info.getSchoolName(), row[0]);
        }
    }
}
