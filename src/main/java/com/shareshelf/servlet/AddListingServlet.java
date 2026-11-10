package com.shareshelf.servlet;

import com.shareshelf.dao.ListingDAO;
import com.shareshelf.model.ListingType;
import com.shareshelf.model.ResourceItem;
import com.shareshelf.util.AppConstants;
import com.shareshelf.util.ValidationUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * GET  /add-listing - shows the form.
 * POST /add-listing - validates it and creates the listing (status always starts as AVAILABLE;
 *                     the owner is taken from the session, never from the form).
 */
@WebServlet("/add-listing")
public class AddListingServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;

    private static final int MAX_TITLE = 100;
    private static final int MAX_DESCRIPTION = 1000;

    private final transient ListingDAO listingDAO = new ListingDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        showForm(req, resp, null);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String title = param(req, "title");
        String description = param(req, "description");
        String category = param(req, "category");
        ListingType type = ListingType.fromString(req.getParameter("listingType"));
        String condition = param(req, "itemCondition");
        String location = param(req, "location");

        String error = null;
        if (title.isEmpty()) {
            error = "Please enter a title.";
        } else if (title.length() > MAX_TITLE) {
            error = "The title can be at most " + MAX_TITLE + " characters.";
        } else if (description.length() > MAX_DESCRIPTION) {
            error = "The description can be at most " + MAX_DESCRIPTION + " characters.";
        } else if (!AppConstants.CATEGORIES.contains(category)) {
            error = "Please choose a category.";
        } else if (type == null) {
            error = "Please choose a listing type.";
        } else if (!AppConstants.CONDITIONS.contains(condition)) {
            error = "Please choose the item's condition.";
        } else if (!AppConstants.LOCATIONS.contains(location)) {
            error = "Please choose a meeting location.";
        }
        if (error != null) {
            showForm(req, resp, error);   // form comes back with what the student typed (${param.*})
            return;
        }

        int suggestedDays = 0;
        if (type == ListingType.BORROW) {
            suggestedDays = ValidationUtil.parseIntOr(req.getParameter("suggestedDays"), AppConstants.DEFAULT_BORROW_DAYS);
            if (!AppConstants.BORROW_DAYS.contains(suggestedDays)) {
                suggestedDays = AppConstants.DEFAULT_BORROW_DAYS;
            }
        }

        ResourceItem item = new ResourceItem();
        item.setOwnerId(currentStudentId(req));
        item.setTitle(title);
        item.setDescription(description);
        item.setCategory(category);
        item.setListingType(type.name());
        item.setItemCondition(condition);
        item.setLocation(location);
        item.setSuggestedDays(suggestedDays);

        if (listingDAO.insert(item)) {
            flash(req, "success", "\"" + title + "\" is now listed and visible to other students.");
            redirect(req, resp, "/dashboard");
        } else {
            showForm(req, resp, "The listing could not be saved. Please try again.");
        }
    }

    private void showForm(HttpServletRequest req, HttpServletResponse resp, String error)
            throws ServletException, IOException {
        req.setAttribute("formError", error);
        req.setAttribute("categories", AppConstants.CATEGORIES);
        req.setAttribute("types", ListingType.values());
        req.setAttribute("conditions", AppConstants.CONDITIONS);
        req.setAttribute("locations", AppConstants.LOCATIONS);
        req.setAttribute("borrowDays", AppConstants.BORROW_DAYS);
        forward(req, resp, "add-listing");
    }
}
