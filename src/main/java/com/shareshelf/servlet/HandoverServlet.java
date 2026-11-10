package com.shareshelf.servlet;

import com.shareshelf.model.ListingStatus;
import com.shareshelf.model.Request;
import com.shareshelf.model.RequestStatus;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;

/**
 * POST /request/handover - the OWNER confirms they have physically handed over a BORROW item.
 * The listing moves RESERVED -> BORROWED; the return-date clock keeps running on the request.
 */
@WebServlet("/request/handover")
public class HandoverServlet extends RequestActionServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected boolean mayAct(Request request, String studentId) {
        return request.getOwnerId().equals(studentId);
    }

    @Override
    protected Outcome perform(HttpServletRequest req, Request request) {
        boolean validRequest = RequestStatus.ACCEPTED.name().equals(request.getStatus()) && request.isBorrow();
        boolean validItem = ListingStatus.RESERVED.name().equals(request.getItemStatus());
        if (!validRequest || !validItem) {
            return Outcome.failure("Only an accepted borrow request that is still reserved can be handed over.");
        }
        if (listingDAO.updateStatus(request.getItemId(), ListingStatus.BORROWED.name())) {
            return Outcome.success("\"" + request.getItemTitle() + "\" is now marked as borrowed.");
        }
        return Outcome.failure("Could not update the listing.");
    }
}
