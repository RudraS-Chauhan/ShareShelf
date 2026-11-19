package com.shareshelf.servlet;

import com.shareshelf.dao.RatingDAO;
import com.shareshelf.dao.RequestDAO;
import com.shareshelf.model.Request;
import com.shareshelf.model.RequestStatus;
import com.shareshelf.util.AppConstants;
import com.shareshelf.util.ValidationUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * /rate?requestId=N - each side rates the other after a completed borrow. The lender lands here right after the
 * return code is verified. Until it is done the person cannot send or accept new requests.
 */
@WebServlet("/rate")
public class RateServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;
    private final transient RequestDAO requestDAO = new RequestDAO();
    private final transient RatingDAO ratingDAO = new RatingDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Request r = load(req, resp);
        if (r != null) {
            forward(req, resp, "rate");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Request r = load(req, resp);
        if (r == null) {
            return;
        }
        int stars = ValidationUtil.parseIntOr(req.getParameter("stars"), 0);
        String comment = ValidationUtil.limit(req.getParameter("comment"), 400).replaceAll("\\p{Cntrl}", " ");
        if (stars < 1 || stars > 5 || comment.length() > AppConstants.MAX_RATING_COMMENT) {
            req.setAttribute("error", "Choose 1 to 5 stars. A comment can be at most " + AppConstants.MAX_RATING_COMMENT + " characters.");
            forward(req, resp, "rate");
            return;
        }
        boolean iAmOwner = r.getOwnerId().equals(currentStudentId(req));
        String ratee = iAmOwner ? r.getRequesterId() : r.getOwnerId();
        if (ratingDAO.add(r.getRequestId(), currentStudentId(req), ratee, stars, null, comment)) {
            flash(req, "success", "Thanks! Your rating for " + (iAmOwner ? r.getRequesterName() : r.getOwnerName()) + " is saved.");
        } else {
            flash(req, "error", "Could not save the rating.");
        }
        redirect(req, resp, "/dashboard");
    }

    /** The request, if this student is its borrower, it is completed, and not rated yet; else redirects away. */
    private Request load(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Request r = requestDAO.findById(ValidationUtil.parseIntOr(req.getParameter("requestId"), -1));
        String me = currentStudentId(req);
        if (r == null || !r.isBorrow() || !(r.getRequesterId().equals(me) || r.getOwnerId().equals(me))
                || !RequestStatus.COMPLETED.name().equals(r.getStatus())) {
            flash(req, "error", "There is nothing to rate here.");
            redirect(req, resp, "/dashboard");
            return null;
        }
        if (ratingDAO.hasRated(r.getRequestId(), me)) {
            flash(req, "success", "You have already rated this one. Thank you!");
            redirect(req, resp, "/dashboard");
            return null;
        }
        boolean iAmOwner = r.getOwnerId().equals(me);
        req.setAttribute("request", r);
        req.setAttribute("otherName", iAmOwner ? r.getRequesterName() : r.getOwnerName());
        req.setAttribute("otherId", iAmOwner ? r.getRequesterId() : r.getOwnerId());
        req.setAttribute("roleText", iAmOwner ? "borrower" : "lender");
        return r;
    }
}
