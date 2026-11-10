package com.shareshelf.servlet;

import com.shareshelf.model.Request;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;

/** POST /request/cancel - the REQUESTER withdraws their own pending request. */
@WebServlet("/request/cancel")
public class CancelRequestServlet extends RequestActionServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected boolean mayAct(Request request, String studentId) {
        return request.getRequesterId().equals(studentId);
    }

    @Override
    protected Outcome perform(HttpServletRequest req, Request request) {
        if (requestDAO.cancel(request.getRequestId())) {
            return Outcome.success("Your request for \"" + request.getItemTitle() + "\" was cancelled.");
        }
        return Outcome.failure("Only a pending request can be cancelled.");
    }
}
