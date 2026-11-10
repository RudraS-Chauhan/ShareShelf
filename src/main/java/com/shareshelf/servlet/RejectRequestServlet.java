package com.shareshelf.servlet;

import com.shareshelf.model.Request;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;

/** POST /request/reject - the OWNER declines a pending request. */
@WebServlet("/request/reject")
public class RejectRequestServlet extends RequestActionServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected boolean mayAct(Request request, String studentId) {
        return request.getOwnerId().equals(studentId);
    }

    @Override
    protected String defaultBackPage() {
        return "dashboard";
    }

    @Override
    protected Outcome perform(HttpServletRequest req, Request request) {
        if (requestDAO.reject(request.getRequestId())) {
            return Outcome.success("Request from " + request.getRequesterName() + " rejected.");
        }
        return Outcome.failure("This request has already been handled.");
    }
}
