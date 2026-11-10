package com.shareshelf.servlet;

import com.shareshelf.model.Request;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;

/**
 * POST /request/complete - the OWNER confirms the exchange is finished (item returned, or given away).
 * RequestDAO.complete() updates the request and the listing together in one transaction.
 */
@WebServlet("/request/complete")
public class CompleteRequestServlet extends RequestActionServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected boolean mayAct(Request request, String studentId) {
        return request.getOwnerId().equals(studentId);
    }

    @Override
    protected Outcome perform(HttpServletRequest req, Request request) {
        if (requestDAO.complete(request.getRequestId(), request.getItemId())) {
            String next = request.isBorrow() ? "It is available to borrow again." : "The listing is now closed.";
            return Outcome.success("Marked as completed. " + next);
        }
        return Outcome.failure("Only an accepted or overdue request can be completed.");
    }
}
