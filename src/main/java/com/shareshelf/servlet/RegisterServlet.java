package com.shareshelf.servlet;

import com.shareshelf.dao.StudentDAO;
import com.shareshelf.model.Student;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet("/register")
public class RegisterServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // These 8 names must exactly match the name="..." attributes in index.html's register form
        String studentId = request.getParameter("studentId");   // Admission Number
        String name = request.getParameter("name");
        String branch = request.getParameter("branch");
        String yearText = request.getParameter("year");
        String email = request.getParameter("email");
        String hostelOrDay = request.getParameter("hostelOrDay");
        String whatsappNumber = request.getParameter("whatsappNumber");
        String password = request.getParameter("password");

        // Basic safety: none of the required fields may be missing
        if (studentId == null || email == null || password == null || yearText == null) {
            response.sendRedirect("index.html?error=registration_failed");
            return;
        }

        studentId = studentId.trim().toUpperCase();
        email = email.trim().toLowerCase();

        // Check 1: college email domain -- the "only Galgotias students" gate
        if (!email.endsWith("@galgotiasuniversity.ac.in")) {
            response.sendRedirect("index.html?error=invalid_email");
            return;
        }

        // Check 2: the admission number must appear in the college email
        // e.g. rudra.25scse1180735@... contains 25SCSE1180735
        if (!email.contains(studentId.toLowerCase())) {
            response.sendRedirect("index.html?error=email_mismatch");
            return;
        }

        StudentDAO studentDAO = new StudentDAO();

        // Check 3: duplicate admission number
        if (studentDAO.isDuplicateId(studentId)) {
            response.sendRedirect("index.html?error=duplicate_id");
            return;
        }

        try {
            int year = Integer.parseInt(yearText);

            Student student = new Student();
            student.setStudentId(studentId);
            student.setName(name);
            student.setBranch(branch);
            student.setYear(year);
            student.setEmail(email);
            student.setHostelOrDay(hostelOrDay);
            student.setWhatsappNumber(whatsappNumber);
            student.setPassword(password);

            boolean success = studentDAO.register(student);

            if (success) {
                response.sendRedirect("index.html?registered=true");
            } else {
                response.sendRedirect("index.html?error=registration_failed");
            }
        } catch (NumberFormatException e) {
            response.sendRedirect("index.html?error=invalid_year");
        }
    }
}