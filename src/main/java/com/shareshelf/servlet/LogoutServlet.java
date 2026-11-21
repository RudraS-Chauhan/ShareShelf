package com.shareshelf.servlet;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

/** GET /logout - ends the session and returns to the login page. */
@WebServlet("/logout")
public class LogoutServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String remembered = com.shareshelf.util.RememberCookie.read(request);
        if (remembered != null) {
            try {
                new com.shareshelf.dao.RememberDAO().delete(remembered);
            } catch (RuntimeException e) {
                // the cookie is cleared below anyway, and an orphan token cannot be used without it
            }
            com.shareshelf.util.RememberCookie.clear(request, response);
        }
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        redirect(request, response, "/index.jsp?logged_out=true");
    }
}
