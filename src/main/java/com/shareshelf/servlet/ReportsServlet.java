package com.shareshelf.servlet;

import com.shareshelf.dao.ListingDAO;
import com.shareshelf.dao.RequestDAO;
import com.shareshelf.listener.AppLifecycleListener;
import com.shareshelf.util.StaleReservationChecker;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * GET /reports        - live counts (each number is a COUNT query), plus the overdue-checker status.
 * GET /reports?format=csv - the same numbers as a downloadable CSV file (the extra-effort feature).
 */
@WebServlet("/reports")
public class ReportsServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;

    private final transient ListingDAO listingDAO = new ListingDAO();
    private final transient RequestDAO requestDAO = new RequestDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Map<String, Integer> listingStatus = listingDAO.countByStatus();
        Map<String, Integer> listingCategory = listingDAO.countByCategory();
        Map<String, Integer> requestStatus = requestDAO.countByStatus();
        Map<String, Integer> topRequested = requestDAO.topRequestedCategories(3);

        if ("csv".equals(req.getParameter("format"))) {
            writeCsv(resp, listingStatus, listingCategory, requestStatus, topRequested);
            return;
        }

        req.setAttribute("totalListings", listingStatus.values().stream().mapToInt(Integer::intValue).sum());
        req.setAttribute("totalRequests", requestStatus.values().stream().mapToInt(Integer::intValue).sum());
        req.setAttribute("listingStatus", listingStatus);
        req.setAttribute("listingCategory", listingCategory);
        req.setAttribute("requestStatus", requestStatus);
        req.setAttribute("topRequested", topRequested);
        req.setAttribute("checker", getServletContext().getAttribute(AppLifecycleListener.CHECKER_ATTRIBUTE));
        forward(req, resp, "reports");
    }

    private void writeCsv(HttpServletResponse resp, Map<String, Integer> listingStatus,
                          Map<String, Integer> listingCategory, Map<String, Integer> requestStatus,
                          Map<String, Integer> topRequested) throws IOException {
        resp.setContentType("text/csv");
        resp.setCharacterEncoding("UTF-8");
        resp.setHeader("Content-Disposition", "attachment; filename=\"shareshelf-report.csv\"");
        PrintWriter out = resp.getWriter();
        out.println("section,name,count");
        writeRows(out, "Listings by status", listingStatus);
        writeRows(out, "Listings by category", listingCategory);
        writeRows(out, "Requests by status", requestStatus);
        writeRows(out, "Most requested categories", topRequested);
        out.println("meta,generated at," + LocalDateTime.now().withNano(0));

        Object checker = getServletContext().getAttribute(AppLifecycleListener.CHECKER_ATTRIBUTE);
        if (checker instanceof StaleReservationChecker) {
            out.println("meta,requests flagged overdue since startup," + ((StaleReservationChecker) checker).getTotalFlagged());
        }
    }

    private void writeRows(PrintWriter out, String section, Map<String, Integer> rows) {
        for (Map.Entry<String, Integer> row : rows.entrySet()) {
            out.println(csv(section) + "," + csv(row.getKey()) + "," + row.getValue());
        }
    }

    /** Quotes a CSV field when needed. */
    private String csv(String value) {
        String text = value == null ? "" : value;
        return (text.contains(",") || text.contains("\"")) ? "\"" + text.replace("\"", "\"\"") + "\"" : text;
    }
}
