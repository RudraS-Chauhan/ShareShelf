package com.shareshelf.servlet;

import com.shareshelf.dao.StudentDAO;
import com.shareshelf.model.Student;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

@WebServlet("/login")
public class LoginServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // These two names MUST exactly match the name="..." attributes in index.html's login form
        String studentId = request.getParameter("studentId");
        String password = request.getParameter("password");

        StudentDAO studentDAO = new StudentDAO();
        Student student = studentDAO.login(studentId, password);

        if (student != null) {
            // Login successful — HttpSession is how the server "remembers" this browser
            // across future page requests, without the student re-entering credentials each time
            HttpSession session = request.getSession();
            session.setAttribute("studentId", student.getStudentId());
            session.setAttribute("studentName", student.getName());

            response.sendRedirect("dashboard.html");
        } else {
            // Wrong ID/password — send back to login with a flag in the URL
            response.sendRedirect("index.html?error=invalid");
        }
    }
}