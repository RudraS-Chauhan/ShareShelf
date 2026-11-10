package com.shareshelf.servlet;

import com.shareshelf.dao.StudentDAO;
import com.shareshelf.model.Student;
import com.shareshelf.util.ValidationUtil;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

/** POST /login - checks the ID and password, then starts a session. */
@WebServlet("/login")
public class LoginServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;

    private final transient StudentDAO studentDAO = new StudentDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        redirect(request, response, "/index.jsp");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // These two names MUST exactly match the name="..." attributes in index.jsp's login form
        String studentId = ValidationUtil.normalizeStudentId(request.getParameter("studentId"));
        String password = request.getParameter("password");   // not trimmed: spaces may be part of it

        if (studentId.isEmpty() || password == null || password.isEmpty()) {
            redirect(request, response, "/index.jsp?error=invalid");
            return;
        }

        Student student = studentDAO.login(studentId, password);

        if (student != null) {
            // HttpSession is how the server "remembers" this browser across page requests
            HttpSession session = request.getSession();
            request.changeSessionId();   // new session id after login (prevents session fixation)
            session.setAttribute("studentId", student.getStudentId());
            session.setAttribute("studentName", student.getName());
            redirect(request, response, "/dashboard");
        } else {
            redirect(request, response, "/index.jsp?error=invalid");
        }
    }
}
