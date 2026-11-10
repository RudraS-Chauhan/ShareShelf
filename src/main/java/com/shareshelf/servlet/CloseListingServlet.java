package com.shareshelf.servlet;

import com.shareshelf.dao.ListingDAO;
import com.shareshelf.model.ListingStatus;
import com.shareshelf.model.ResourceItem;
import com.shareshelf.util.ValidationUtil;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/** POST /listing/close - the OWNER takes an AVAILABLE listing off the shelf. */
@WebServlet("/listing/close")
public class CloseListingServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;

    private final transient ListingDAO listingDAO = new ListingDAO();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        int itemId = ValidationUtil.parseIntOr(req.getParameter("itemId"), -1);
        ResourceItem item = itemId > 0 ? listingDAO.findById(itemId) : null;

        if (item == null) {
            flash(req, "error", "That listing could not be found.");
        } else if (!item.getOwnerId().equals(currentStudentId(req))) {
            flash(req, "error", "You can only close your own listings.");
        } else if (!ListingStatus.AVAILABLE.name().equals(item.getStatus())) {
            flash(req, "error", "Only an available listing can be closed. Finish its current request first.");
        } else if (listingDAO.updateStatus(itemId, ListingStatus.CLOSED.name())) {
            flash(req, "success", "\"" + item.getTitle() + "\" is closed and no longer shown in search.");
        } else {
            flash(req, "error", "The listing could not be closed.");
        }
        redirect(req, resp, "/dashboard");
    }
}
