package com.shareshelf.servlet;

import com.shareshelf.dao.StudentDAO;
import com.shareshelf.util.ValidationUtil;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * POST /profile/whatsapp - lets a student correct their own WhatsApp number (a typo at registration would otherwise
 * break the overdue contact feature for good). The password is asked again, so a borrowed laptop or an open session
 * cannot quietly redirect the overdue chat to someone else's phone. Only the logged-in student's own number can change.
 */
@WebServlet("/profile/whatsapp")
public class UpdateWhatsappServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;
    private final transient StudentDAO studentDAO = new StudentDAO();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String me = currentStudentId(req);
        String number = ValidationUtil.normalizeWhatsapp(ValidationUtil.limit(req.getParameter("whatsappNumber"), 20));
        String password = req.getParameter("password");

        if (number == null) {
            flash(req, "error", "Enter a valid 10-digit Indian mobile number (WhatsApp).");
        } else if (password == null || password.isEmpty() || studentDAO.login(me, password) == null) {
            flash(req, "error", "Wrong password, so your WhatsApp number was not changed.");
        } else {
            studentDAO.updateWhatsapp(me, number);
            flash(req, "success", "Your WhatsApp number was updated.");
        }
        redirect(req, resp, "/profile");
    }
}
