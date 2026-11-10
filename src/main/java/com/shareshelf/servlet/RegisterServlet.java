package com.shareshelf.servlet;

import com.shareshelf.dao.StudentDAO;
import com.shareshelf.model.Student;
import com.shareshelf.util.ValidationUtil;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/** POST /register - validates the form, then creates the student (password is hashed in StudentDAO). */
@WebServlet("/register")
public class RegisterServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;

    private final transient StudentDAO studentDAO = new StudentDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        redirect(request, response, "/index.jsp");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // These names must exactly match the name="..." attributes in index.jsp's register form
        String studentId = ValidationUtil.normalizeStudentId(request.getParameter("studentId"));   // admission number
        String name = param(request, "name");
        String branch = param(request, "branch");
        String yearText = param(request, "year");
        String email = param(request, "email").toLowerCase();
        String hostelOrDay = param(request, "hostelOrDay");
        String whatsappRaw = param(request, "whatsappNumber");
        String password = request.getParameter("password");
        boolean consent = "yes".equals(request.getParameter("consent"));

        // Check 0: nothing required may be missing
        if (studentId.isEmpty() || name.isEmpty() || branch.isEmpty() || yearText.isEmpty()
                || email.isEmpty() || hostelOrDay.isEmpty() || whatsappRaw.isEmpty()
                || password == null || password.isEmpty()) {
            fail(request, response, "missing_fields");
            return;
        }
        if (!ValidationUtil.isValidStudentId(studentId)) {
            fail(request, response, "invalid_id");
            return;
        }

        // Check 1: college email domain - the "only Galgotias students" gate
        if (!ValidationUtil.isCollegeEmail(email)) {
            fail(request, response, "invalid_email");
            return;
        }

        // Check 2: the admission number must appear in the college email,
        // e.g. rudra.25scse1180735@... contains 25SCSE1180735
        if (!ValidationUtil.emailContainsId(email, studentId)) {
            fail(request, response, "email_mismatch");
            return;
        }

        // Check 3: password strength
        if (!ValidationUtil.isStrongPassword(password)) {
            fail(request, response, "weak_password");
            return;
        }

        // Check 4: WhatsApp number (needed for the overdue-only contact feature) and the consent for it
        String whatsapp = ValidationUtil.normalizeWhatsapp(whatsappRaw);
        if (whatsapp == null) {
            fail(request, response, "invalid_whatsapp");
            return;
        }
        if (!consent) {
            fail(request, response, "no_consent");
            return;
        }

        // Check 5: other fields
        int year = ValidationUtil.parseIntOr(yearText, -1);
        if (year < 1 || year > 4) {
            fail(request, response, "invalid_year");
            return;
        }
        if (!"Hostel".equals(hostelOrDay) && !"Day Scholar".equals(hostelOrDay)) {
            fail(request, response, "missing_fields");
            return;
        }

        // Check 6: duplicate admission number
        if (studentDAO.isDuplicateId(studentId)) {
            fail(request, response, "duplicate_id");
            return;
        }

        Student student = new Student();
        student.setStudentId(studentId);
        student.setName(ValidationUtil.limit(name, 100));
        student.setBranch(ValidationUtil.limit(branch, 50));
        student.setYear(year);
        student.setEmail(ValidationUtil.limit(email, 100));
        student.setHostelOrDay(hostelOrDay);
        student.setWhatsappNumber(whatsapp);
        student.setPassword(password);

        if (studentDAO.register(student)) {
            redirect(request, response, "/index.jsp?registered=true");
        } else {
            fail(request, response, "duplicate_id");
        }
    }

    private void fail(HttpServletRequest request, HttpServletResponse response, String code) throws IOException {
        redirect(request, response, "/index.jsp?tab=register&error=" + code);
    }
}
