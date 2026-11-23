package com.shareshelf.servlet;

import com.shareshelf.dao.ProfileDAO;
import com.shareshelf.model.Profile;
import com.shareshelf.util.ValidationUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/** GET /profile or /profile?id=STUDENTID - a student's track record and ratings. Any logged-in student can look. */
@WebServlet("/profile")
public class ProfileServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;
    private final transient ProfileDAO profileDAO = new ProfileDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String id = ValidationUtil.limit(req.getParameter("id"), 20);
        if (id.isEmpty()) {
            id = currentStudentId(req);
        }
        Profile p = profileDAO.load(id);
        if (p == null) {
            flash(req, "error", "That student was not found.");
            redirect(req, resp, "/dashboard");
            return;
        }
        req.setAttribute("profile", p);
        com.shareshelf.util.StudentIdInfo info = com.shareshelf.util.StudentIdInfo.parse(p.getStudentId());
        if (info != null) {
            req.setAttribute("idSummary", info.summary());
            req.setAttribute("studyYear", info.studyYear(java.time.LocalDate.now(com.shareshelf.util.AppConstants.CAMPUS_ZONE)));
        }
        boolean isMe = id.equals(currentStudentId(req));
        req.setAttribute("isMe", isMe);
        if (isMe) {
            // only the owner of the account ever sees even a hint of their own number: the last 4 digits
            com.shareshelf.model.Student me = new com.shareshelf.dao.StudentDAO().findById(id);
            String n = me == null ? null : me.getWhatsappNumber();
            if (n != null && n.length() >= 4) {
                req.setAttribute("whatsappEnding", n.substring(n.length() - 4));
            }
        }
        forward(req, resp, "profile");
    }
}
