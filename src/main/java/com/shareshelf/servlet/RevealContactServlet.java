package com.shareshelf.servlet;

import com.shareshelf.dao.RequestDAO;
import com.shareshelf.model.Request;
import com.shareshelf.util.ValidationUtil;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * POST /request/reveal - the privacy safety net.
 * This is the ONLY place a student's WhatsApp number ever leaves the database. RequestDAO.revealContact()
 * returns it only when the request is OVERDUE and the caller is the request's owner, and it logs the reveal.
 * On success the owner is sent straight to a wa.me chat with a prefilled message.
 */
@WebServlet("/request/reveal")
public class RevealContactServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;

    private final transient RequestDAO requestDAO = new RequestDAO();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String ownerId = currentStudentId(req);
        int requestId = ValidationUtil.parseIntOr(req.getParameter("requestId"), -1);

        String number = requestId > 0 ? requestDAO.revealContact(requestId, ownerId) : null;
        if (number == null || !number.matches("[0-9]{10,15}")) {
            flash(req, "error", "Contact details are only available to the owner of an overdue request.");
            redirect(req, resp, "/my-requests");
            return;
        }

        Request request = requestDAO.findById(requestId);
        String text = "Hi " + request.getRequesterName() + ", this is " + request.getOwnerName()
                + " from ShareShelf. \"" + request.getItemTitle() + "\" was due back on "
                + request.getExpectedReturnDate() + ". Could we arrange the return?";
        String encoded = URLEncoder.encode(text, StandardCharsets.UTF_8).replace("+", "%20");
        resp.sendRedirect("https://wa.me/" + number + "?text=" + encoded);
    }
}
