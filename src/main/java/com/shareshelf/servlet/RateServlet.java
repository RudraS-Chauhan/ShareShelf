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
 * /rate?requestId=N - the borrower rates the owner after a completed borrow. (The owner rates the
 * borrower inside the return scan.) Until it is done the borrower cannot send new requests.
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
        if (ratingDAO.add(r.getRequestId(), r.getRequesterId(), r.getOwnerId(), stars, null, comment)) {
            flash(req, "success", "Thanks! Your rating for " + r.getOwnerName() + " is saved.");
        } else {
            flash(req, "error", "Could not save the rating.");
        }
        redirect(req, resp, "/dashboard");
    }

    /** The request, if this student is its borrower, it is completed, and not rated yet; else redirects away. */
    private Request load(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Request r = requestDAO.findById(ValidationUtil.parseIntOr(req.getParameter("requestId"), -1));
        String me = currentStudentId(req);
        if (r == null || !r.isBorrow() || !r.getRequesterId().equals(me)
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
        req.setAttribute("request", r);
        return r;
    }
}
