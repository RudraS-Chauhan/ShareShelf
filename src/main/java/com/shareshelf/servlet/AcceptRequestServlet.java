package com.shareshelf.servlet;

import com.shareshelf.model.Request;
import com.shareshelf.model.RequestStatus;
import com.shareshelf.model.ResourceItem;
import com.shareshelf.util.AppConstants;
import com.shareshelf.util.ValidationUtil;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

/**
 * POST /request/accept - the OWNER accepts a pending request.
 * The owner may also set a meeting time and a note on where exactly to meet.
 * For BORROW listings the owner's chosen duration (or the listing's suggestion), counted from the
 * meeting time (or from now if none was set), becomes the exact return deadline. The two table updates happen in one transaction inside RequestDAO.accept().
 */
@WebServlet("/request/accept")
public class AcceptRequestServlet extends RequestActionServlet {

    private static final long serialVersionUID = 1L;

    private static final java.time.format.DateTimeFormatter DUE_FORMAT =
            java.time.format.DateTimeFormatter.ofPattern("EEE d MMM, h:mm a", java.util.Locale.ENGLISH);

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

        LocalDateTime now = LocalDateTime.now(AppConstants.CAMPUS_ZONE);

        // optional meeting time (a <input type="datetime-local"> value such as 2026-11-18T15:00)
        LocalDateTime meetTime = null;
        String meetText = ValidationUtil.limit(req.getParameter("meetTime"), 40);
        if (!meetText.isEmpty()) {
            try {
                meetTime = LocalDateTime.parse(meetText);
            } catch (DateTimeParseException e) {
                return Outcome.failure("That meeting time is not valid.");
            }
            if (meetTime.isBefore(now.minusMinutes(5)) || meetTime.isAfter(now.plusDays(AppConstants.MAX_MEET_DAYS_AHEAD))) {
                return Outcome.failure("Pick a meeting time from now up to " + AppConstants.MAX_MEET_DAYS_AHEAD + " days ahead.");
            }
        }
        String meetNote = ValidationUtil.limit(req.getParameter("meetNote"), 1000).replaceAll("\\p{Cntrl}", " ");
        if (meetNote.length() > AppConstants.MAX_MEET_NOTE) {
            return Outcome.failure("The meeting note can be at most " + AppConstants.MAX_MEET_NOTE + " characters.");
        }

        if (!new com.shareshelf.dao.RatingDAO().pendingFor(currentStudentId(req)).isEmpty()) {
            return Outcome.failure("Please rate your last borrower first (yellow banner), then you can accept new requests.");
        }

        LocalDateTime dueAt = null;     // stays null for borrow items: the clock starts when the pickup is verified
        Integer borrowHours = null;
        if (item.isBorrow()) {
            int fallback = item.getDurationHours() > 0 ? item.getDurationHours() : AppConstants.DEFAULT_DURATION_HOURS;
            int hours = ValidationUtil.parseIntOr(req.getParameter("hours"), fallback);
            if (!AppConstants.DURATION_HOURS.contains(hours)) {
                return Outcome.failure("Choose one of the borrow durations from the list.");
            }
            borrowHours = hours;
        }

        if (requestDAO.accept(request.getRequestId(), request.getItemId(), dueAt, meetTime, meetNote, borrowHours)) {
            notifier.add(request.getRequesterId(), request.getOwnerName() + " accepted your request for \"" + item.getTitle() + "\""
                    + (meetTime != null ? ". Meet: " + meetTime.format(DUE_FORMAT) : ".")
                    + (borrowHours != null ? " You can keep it " + AppConstants.durationLabel(borrowHours) + " from pickup." : ""), "/my-requests");
            return Outcome.success("Accepted. \"" + item.getTitle() + "\" is now reserved for "
                    + request.getRequesterName() + (borrowHours != null ? " for " + AppConstants.durationLabel(borrowHours) + ", counted from pickup." : "."));
        }
        return Outcome.failure("Could not accept: the item is no longer available.");
    }
}
