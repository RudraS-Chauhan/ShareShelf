package com.shareshelf.servlet;

import com.shareshelf.dao.ListingDAO;
import com.shareshelf.dao.RequestDAO;
import com.shareshelf.model.Request;
import com.shareshelf.model.RequestStatus;
import com.shareshelf.util.AppConstants;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/** GET /dashboard - the student's listings, pending requests on them, and a few headline numbers. */
@WebServlet("/dashboard")
public class DashboardServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;

    private final transient ListingDAO listingDAO = new ListingDAO();
    private final transient RequestDAO requestDAO = new RequestDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String studentId = currentStudentId(req);

        List<Request> received = requestDAO.findByOwner(studentId);
        List<Request> pending = received.stream()
                .filter(r -> RequestStatus.PENDING.name().equals(r.getStatus()))
                .collect(Collectors.toList());
        long overdue = received.stream()
                .filter(r -> RequestStatus.OVERDUE.name().equals(r.getStatus()))
                .count();

        req.setAttribute("myListings", listingDAO.findByOwner(studentId));
        req.setAttribute("pendingRequests", pending);
        req.setAttribute("overdueCount", overdue);
        req.setAttribute("completedCount", requestDAO.countCompleted(studentId));
        req.setAttribute("borrowDays", AppConstants.BORROW_DAYS);
        forward(req, resp, "dashboard");
    }
}
