package com.shareshelf.servlet;

import com.shareshelf.dao.ListingDAO;
import com.shareshelf.dao.RequestDAO;
import com.shareshelf.model.Request;
import com.shareshelf.util.ValidationUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Template for every "do something to one request" servlet (accept, reject, cancel, hand over,
 * complete). doPost() is fixed and does the shared work every time:
 *   1. read the requestId and load the request
 *   2. make sure the logged-in student is allowed to act on it (owners can't touch requests that
 *      aren't theirs - test case 8 / FINAL_SPEC rule)
 *   3. let the subclass perform the action
 *   4. flash the result and redirect (Post/Redirect/Get, so refreshing never re-submits)
 * Subclasses only say WHO may act and WHAT happens.
 */
public abstract class RequestActionServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;

    protected final transient RequestDAO requestDAO = new RequestDAO();
    protected final transient ListingDAO listingDAO = new ListingDAO();

    /** Result of an action: a message for the student and whether it worked. */
    protected record Outcome(boolean ok, String message) {
        static Outcome success(String message) {
            return new Outcome(true, message);
        }

        static Outcome failure(String message) {
            return new Outcome(false, message);
        }
    }

    /** Is this student allowed to perform the action on this request? */
    protected abstract boolean mayAct(Request request, String studentId);

    /** Carries out the action. Called only after mayAct() returned true. */
    protected abstract Outcome perform(HttpServletRequest req, Request request);

    /** Page to return to when the form did not say (hidden "back" field). */
    protected String defaultBackPage() {
        return "my-requests";
    }

    @Override
    protected final void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String studentId = currentStudentId(req);
        int requestId = ValidationUtil.parseIntOr(req.getParameter("requestId"), -1);
        Request request = requestId > 0 ? requestDAO.findById(requestId) : null;

        Outcome outcome;
        if (request == null) {
            outcome = Outcome.failure("That request could not be found.");
        } else if (!mayAct(request, studentId)) {
            outcome = Outcome.failure("You can't do that: this request isn't yours to change.");
        } else {
            outcome = perform(req, request);
        }

        flash(req, outcome.ok() ? "success" : "error", outcome.message());
        redirectBack(req, resp, defaultBackPage());
    }
}
