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

        // greeting by campus time, first name only
        int hour = java.time.LocalTime.now(AppConstants.CAMPUS_ZONE).getHour();
        req.setAttribute("greeting", hour < 5 ? "Good night" : hour < 12 ? "Good morning" : hour < 17 ? "Good afternoon" : "Good evening");
        String fullName = String.valueOf(req.getSession().getAttribute("studentName"));
        req.setAttribute("firstName", fullName.trim().isEmpty() ? "there" : fullName.trim().split("\\s+")[0]);

        // "what should I do next" counts, from data we already load
        List<Request> mine = requestDAO.findByRequester(studentId);
        long handoversReady = received.stream().filter(r -> "ACCEPTED".equals(r.getStatus())).count()
                + mine.stream().filter(r -> "ACCEPTED".equals(r.getStatus())).count();
        long toReturn = mine.stream().filter(r -> "BORROWED".equals(r.getStatus()) || "OVERDUE".equals(r.getStatus())).count();
        req.setAttribute("handoversReady", handoversReady);
        req.setAttribute("toReturn", toReturn);

        // level and stars (the same numbers other students see on the profile)
        int completed = requestDAO.countCompleted(studentId);
        req.setAttribute("level", com.shareshelf.util.TrustLevel.of(completed));
        req.setAttribute("myProfile", new com.shareshelf.dao.ProfileDAO().load(studentId));

        req.setAttribute("myListings", listingDAO.findByOwner(studentId));
        req.setAttribute("pendingRequests", pending);
        req.setAttribute("overdueCount", overdue);
        req.setAttribute("completedCount", requestDAO.countCompleted(studentId));
        req.setAttribute("durations", AppConstants.durationOptions());
        forward(req, resp, "dashboard");
    }
}
