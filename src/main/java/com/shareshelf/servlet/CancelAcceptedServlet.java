package com.shareshelf.servlet;

import com.shareshelf.model.ListingStatus;
import com.shareshelf.model.Request;
import com.shareshelf.model.RequestStatus;
import com.shareshelf.util.AppConstants;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;

/**
 * POST /request/cancel-accepted - the lender OR the borrower cancels an accepted request before the
 * pickup, with a reason. The item goes back on the shelf and the other person is told.
 */
@WebServlet("/request/cancel-accepted")
public class CancelAcceptedServlet extends RequestActionServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected boolean mayAct(Request request, String studentId) {
        return request.getOwnerId().equals(studentId) || request.getRequesterId().equals(studentId);
    }

    @Override
    protected Outcome perform(HttpServletRequest req, Request request) {
        String reason = param(req, "reason");
        if (!AppConstants.CANCEL_REASONS.contains(reason)) {
            return Outcome.failure("Please choose a reason from the list.");
        }
        if (!RequestStatus.ACCEPTED.name().equals(request.getStatus())
                || !ListingStatus.RESERVED.name().equals(request.getItemStatus())) {
            return Outcome.failure("This can only be cancelled before the pickup.");
        }
        if (!requestTx.cancelAccepted(request.getRequestId(), request.getItemId(), reason)) {
            return Outcome.failure("Could not cancel: the pickup may have just happened.");
        }
        boolean byOwner = request.getOwnerId().equals(currentStudentId(req));
        String who = byOwner ? request.getOwnerName() : request.getRequesterName();
        notifier.add(byOwner ? request.getRequesterId() : request.getOwnerId(),
                who + " cancelled \"" + request.getItemTitle() + "\" (" + reason + ").", "/my-requests");
        return Outcome.success("Cancelled. \"" + request.getItemTitle() + "\" is available again.");
    }
}
