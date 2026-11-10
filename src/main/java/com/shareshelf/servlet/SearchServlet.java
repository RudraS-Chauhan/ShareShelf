package com.shareshelf.servlet;

import com.shareshelf.dao.ListingDAO;
import com.shareshelf.model.ListingType;
import com.shareshelf.util.AppConstants;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/** GET /search - browse AVAILABLE listings, optionally filtered by keyword, category and type. */
@WebServlet("/search")
public class SearchServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;

    private final transient ListingDAO listingDAO = new ListingDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String keyword = param(req, "q");   // trimmed, so "  calculator  " still matches
        String category = param(req, "category");
        String type = param(req, "type").toUpperCase();

        // Ignore filter values that are not real options (e.g. edited in the URL)
        if (!AppConstants.CATEGORIES.contains(category)) {
            category = "";
        }
        if (ListingType.fromString(type) == null) {
            type = "";
        }

        req.setAttribute("items", listingDAO.searchAvailable(keyword, category, type));
        req.setAttribute("q", keyword);
        req.setAttribute("selectedCategory", category);
        req.setAttribute("selectedType", type);
        req.setAttribute("filtered", !keyword.isEmpty() || !category.isEmpty() || !type.isEmpty());
        req.setAttribute("categories", AppConstants.CATEGORIES);
        req.setAttribute("types", ListingType.values());
        forward(req, resp, "search");
    }
}
