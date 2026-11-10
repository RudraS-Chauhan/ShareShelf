package com.shareshelf.servlet;

import com.shareshelf.dao.ListingDAO;
import com.shareshelf.dao.RequestDAO;
import com.shareshelf.model.ListingStatus;
import com.shareshelf.model.Request;
import com.shareshelf.model.ResourceItem;
import com.shareshelf.util.ValidationUtil;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * POST /request/send - a student asks for someone's listing.
 * Rules enforced here: the listing must exist and be AVAILABLE (closed/reserved listings reject
 * requests), you cannot request your own listing, and you cannot send a second PENDING request
 * for the same item.
 */
@WebServlet("/request/send")
public class SendRequestServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;
    private static final int MAX_MESSAGE = 255;

    private final transient ListingDAO listingDAO = new ListingDAO();
    private final transient RequestDAO requestDAO = new RequestDAO();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String studentId = currentStudentId(req);
        int itemId = ValidationUtil.parseIntOr(req.getParameter("itemId"), -1);
        String message = param(req, "message");

        ResourceItem item = itemId > 0 ? listingDAO.findById(itemId) : null;
        String error = null;
        if (item == null) {
            error = "That listing could not be found.";
        } else if (!ListingStatus.AVAILABLE.name().equals(item.getStatus())) {
            error = "Sorry, \"" + item.getTitle() + "\" is no longer available.";
        } else if (item.getOwnerId().equals(studentId)) {
            error = "You can't request your own listing.";
        } else if (message.length() > MAX_MESSAGE) {
            error = "Your message can be at most " + MAX_MESSAGE + " characters.";
        } else if (requestDAO.hasDuplicatePending(itemId, studentId)) {
            error = "You already have a pending request for \"" + item.getTitle() + "\".";
        }
        if (error != null) {
            flash(req, "error", error);
            redirect(req, resp, "/search");
            return;
        }

        Request request = new Request();
        request.setItemId(itemId);
        request.setRequesterId(studentId);
        request.setOwnerId(item.getOwnerId());   // taken from the listing, never from the form
        request.setMessage(message);

        if (requestDAO.insert(request)) {
            flash(req, "success", "Request sent to " + item.getOwnerName() + " for \"" + item.getTitle() + "\".");
            redirect(req, resp, "/my-requests");
        } else {
            flash(req, "error", "Your request could not be sent. Please try again.");
            redirect(req, resp, "/search");
        }
    }
}
