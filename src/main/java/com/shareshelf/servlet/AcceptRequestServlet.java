package com.shareshelf.servlet;

import com.shareshelf.model.Request;
import com.shareshelf.model.RequestStatus;
import com.shareshelf.model.ResourceItem;
import com.shareshelf.util.AppConstants;
import com.shareshelf.util.ValidationUtil;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import java.sql.Date;
import java.time.LocalDate;

/**
 * POST /request/accept - the OWNER accepts a pending request.
 * For BORROW listings the owner's chosen duration (or the listing's suggestion) becomes the
 * expected return date. The two table updates happen in one transaction inside RequestDAO.accept().
 */
@WebServlet("/request/accept")
public class AcceptRequestServlet extends RequestActionServlet {

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
        if (!RequestStatus.PENDING.name().equals(request.getStatus())) {
            return Outcome.failure("This request has already been handled.");
        }
        ResourceItem item = listingDAO.findById(request.getItemId());
        if (item == null) {
            return Outcome.failure("The listing no longer exists.");
        }

        Date returnDate = null;
        if (item.isBorrow()) {
            int fallback = item.getSuggestedDays() > 0 ? item.getSuggestedDays() : AppConstants.DEFAULT_BORROW_DAYS;
            int days = ValidationUtil.parseIntOr(req.getParameter("days"), fallback);
            if (days < 1 || days > AppConstants.MAX_BORROW_DAYS) {
                return Outcome.failure("Choose a borrow duration between 1 and " + AppConstants.MAX_BORROW_DAYS + " days.");
            }
            returnDate = Date.valueOf(LocalDate.now().plusDays(days));
        }

        if (requestDAO.accept(request.getRequestId(), request.getItemId(), returnDate)) {
            return Outcome.success("Accepted. \"" + item.getTitle() + "\" is now reserved for "
                    + request.getRequesterName() + (returnDate != null ? ", due back on " + returnDate + "." : "."));
        }
        return Outcome.failure("Could not accept: the item is no longer available.");
    }
}
