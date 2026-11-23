package com.shareshelf.dao;

import com.shareshelf.model.ListingStatus;
import com.shareshelf.model.ListingType;
import com.shareshelf.model.RequestStatus;
import com.shareshelf.util.AppConstants;
import com.shareshelf.util.DBConnection;
import com.shareshelf.util.DataAccessException;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * The multi-table WORKFLOWS of a request: accept, pickup, cancel, return, contact reveal, plus the two
 * background sweeps (overdue, unclaimed). Each one changes several rows or tables at once, so each runs
 * as a real JDBC transaction: {@code setAutoCommit(false)}, {@code SELECT ... FOR UPDATE} to lock the
 * rows, {@code commit()}, and {@code rollback()} if anything fails.
 *
 * Reads and single-row updates live in {@link RequestDAO}; this class only coordinates writes. Splitting
 * them keeps each file small, and the transaction pattern stays in one place.
 */
public class RequestTransactionService {

    /** Returned by {@link #revealContact} when borrower and owner registered the same WhatsApp number. */
    public static final String SAME_NUMBER = "same-number";


    /**
     * TRANSACTION: the owner accepts a request.
     *  1. request PENDING -> ACCEPTED, with the expected return date (null for non-BORROW listings)
     *  2. listing AVAILABLE -> RESERVED
     *  3. every other PENDING request on the same listing -> REJECTED (the item is taken)
     * Either all three happen or none do.
     *
     * @param dueAt     when the item must be back (null for non-BORROW listings); also stored as a plain date
     * @param meetTime  optional agreed meeting time
     * @param meetNote  optional note on where exactly / how to recognise each other
     * @return false (and nothing changed) if the request is no longer pending or the item is no longer available.
     */
    public boolean accept(int requestId, int itemId, LocalDateTime dueAt, LocalDateTime meetTime, String meetNote) {
        return accept(requestId, itemId, dueAt, meetTime, meetNote, null);
    }

    /**
     * Same as above, for the QR flow: the deadline is NOT fixed yet. Only the agreed number of hours is
     * stored; the clock starts when the borrower's pickup is verified (see {@link #startBorrow}).
     *
     * @param borrowHours how long the borrower may keep the item once picked up (null = use dueAt as given)
     */
    public boolean accept(int requestId, int itemId, LocalDateTime dueAt, LocalDateTime meetTime, String meetNote,
                          Integer borrowHours) {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int accepted;
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE requests SET status = 'ACCEPTED', expected_return_date = ?, due_at = ?, meet_time = ?, meet_note = ?, borrow_hours = ?, accepted_at = ? "
                        + "WHERE request_id = ? AND item_id = ? AND status = 'PENDING'")) {
                    if (dueAt != null) {
                        ps.setDate(1, Date.valueOf(dueAt.toLocalDate()));
                        ps.setObject(2, dueAt);
                    } else {
                        ps.setNull(1, Types.DATE);
                        ps.setNull(2, Types.TIMESTAMP);
                    }
                    if (meetTime != null) {
                        ps.setObject(3, meetTime);
                    } else {
                        ps.setNull(3, Types.TIMESTAMP);
                    }
                    ps.setString(4, meetNote == null || meetNote.isEmpty() ? null : meetNote);
                    if (borrowHours != null) {
                        ps.setInt(5, borrowHours);
                    } else {
                        ps.setNull(5, Types.INTEGER);
                    }
                    ps.setObject(6, LocalDateTime.now(AppConstants.CAMPUS_ZONE));
                    ps.setInt(7, requestId);
                    ps.setInt(8, itemId);
                    accepted = ps.executeUpdate();
                }
                if (accepted != 1) {
                    conn.rollback();
                    return false;
                }

                int reserved;
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE resource_items SET status = 'RESERVED' WHERE item_id = ? AND status = 'AVAILABLE'")) {
                    ps.setInt(1, itemId);
                    reserved = ps.executeUpdate();
                }
                if (reserved != 1) {
                    conn.rollback();
                    return false;
                }

                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE requests SET status = 'REJECTED' "
                        + "WHERE item_id = ? AND status = 'PENDING' AND request_id <> ?")) {
                    ps.setInt(1, itemId);
                    ps.setInt(2, requestId);
                    ps.executeUpdate();
                }

                conn.commit();
                return true;
            } catch (SQLException e) {
                rollbackQuietly(conn);
                throw new DataAccessException("Could not accept request " + requestId, e);
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not accept request " + requestId, e);
        }
    }

    /**
     * TRANSACTION: the borrower's pickup was verified. Item RESERVED -> BORROWED and the borrow clock
     * starts NOW: due = now + the agreed hours (requests accepted before this feature keep their old due time).
     *
     * @return false (and nothing changed) if the request is not ACCEPTED or the item is not RESERVED.
     */
    public boolean startBorrow(int requestId, int itemId) {
        LocalDateTime now = LocalDateTime.now(AppConstants.CAMPUS_ZONE);
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int hours = 0;
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT borrow_hours FROM requests WHERE request_id = ? AND item_id = ? AND status = 'ACCEPTED' FOR UPDATE")) {
                    ps.setInt(1, requestId);
                    ps.setInt(2, itemId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            conn.rollback();
                            return false;
                        }
                        hours = rs.getInt(1);   // NULL reads as 0
                    }
                }
                int moved;
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE resource_items SET status = 'BORROWED' WHERE item_id = ? AND status = 'RESERVED'")) {
                    ps.setInt(1, itemId);
                    moved = ps.executeUpdate();
                }
                if (moved != 1) {
                    conn.rollback();
                    return false;
                }
                if (hours > 0) {
                    LocalDateTime due = now.plusHours(hours);
                    try (PreparedStatement ps = conn.prepareStatement(
                            "UPDATE requests SET due_at = ?, expected_return_date = ? WHERE request_id = ?")) {
                        ps.setObject(1, due);
                        ps.setDate(2, Date.valueOf(due.toLocalDate()));
                        ps.setInt(3, requestId);
                        ps.executeUpdate();
                    }
                }
                conn.commit();
                return true;
            } catch (SQLException e) {
                rollbackQuietly(conn);
                throw new DataAccessException("Could not start the borrow for request " + requestId, e);
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not start the borrow for request " + requestId, e);
        }
    }

    /**
     * TRANSACTION: either side cancels an ACCEPTED request before the pickup. Request -> CANCELLED,
     * item RESERVED -> AVAILABLE again, so someone else can ask for it.
     *
     * @return false (and nothing changed) if the request is not ACCEPTED or the item is no longer RESERVED
     *         (for example because the pickup was verified a moment earlier).
     */
    public boolean cancelAccepted(int requestId, int itemId, String reason) {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int a;
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE requests SET status = 'CANCELLED', cancel_reason = ? WHERE request_id = ? AND item_id = ? AND status = 'ACCEPTED'")) {
                    ps.setString(1, reason);
                    ps.setInt(2, requestId);
                    ps.setInt(3, itemId);
                    a = ps.executeUpdate();
                }
                int b = 0;
                if (a == 1) {
                    try (PreparedStatement ps = conn.prepareStatement(
                            "UPDATE resource_items SET status = 'AVAILABLE' WHERE item_id = ? AND status = 'RESERVED'")) {
                        ps.setInt(1, itemId);
                        b = ps.executeUpdate();
                    }
                }
                if (a != 1 || b != 1) {
                    conn.rollback();
                    return false;
                }
                conn.commit();
                return true;
            } catch (SQLException e) {
                rollbackQuietly(conn);
                throw new DataAccessException("Could not cancel request " + requestId, e);
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not cancel request " + requestId, e);
        }
    }

    /**
     * Background job: an accepted item that nobody picked up goes back on the shelf. Counts from the
     * meeting time (+2 hours) or, when there was none, from the accept time (+24 hours). Both students are told.
     *
     * @return how many reservations were released
     */
    public int releaseUnclaimed() {
        LocalDateTime now = LocalDateTime.now(AppConstants.CAMPUS_ZONE);
        String sql = "SELECT q.request_id, q.item_id, q.requester_id, q.owner_id, i.title FROM requests q "
                + "JOIN resource_items i ON i.item_id = q.item_id "
                + "WHERE q.status = 'ACCEPTED' AND i.status = 'RESERVED' AND ("
                + "(q.meet_time IS NOT NULL AND q.meet_time < ?) OR "
                + "(q.meet_time IS NULL AND q.accepted_at IS NOT NULL AND q.accepted_at < ?))";
        List<Object[]> due = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, now.minusHours(AppConstants.RELEASE_AFTER_MEET_HOURS));
            ps.setObject(2, now.minusHours(AppConstants.RELEASE_NO_MEET_HOURS));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    due.add(new Object[]{rs.getInt(1), rs.getInt(2), rs.getString(3), rs.getString(4), rs.getString(5)});
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not look for unclaimed reservations", e);
        }
        NotificationDAO notifier = new NotificationDAO();
        int released = 0;
        for (Object[] row : due) {
            if (cancelAccepted((int) row[0], (int) row[1], "Not picked up in time")) {
                released++;
                String msg = "\"" + row[4] + "\" was not picked up in time, so the reservation was released.";
                notifier.add((String) row[2], msg, "/my-requests");
                notifier.add((String) row[3], msg + " It is available again.", "/my-requests");
            }
        }
        return released;
    }


    /**
     * TRANSACTION: the owner marks the item as returned / handed over for good.
     *  1. request ACCEPTED or OVERDUE -> COMPLETED
     *  2. listing -> AVAILABLE again if it was a BORROW listing, otherwise CLOSED (it is gone)
     *
     * @return false (and nothing changed) if the request was not ACCEPTED or OVERDUE.
     */
    public boolean complete(int requestId, int itemId) {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int completed;
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE requests SET status = 'COMPLETED' "
                        + "WHERE request_id = ? AND item_id = ? AND status IN ('ACCEPTED', 'OVERDUE')")) {
                    ps.setInt(1, requestId);
                    ps.setInt(2, itemId);
                    completed = ps.executeUpdate();
                }
                if (completed != 1) {
                    conn.rollback();
                    return false;
                }

                String listingType = null;
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT listing_type FROM resource_items WHERE item_id = ? FOR UPDATE")) {
                    ps.setInt(1, itemId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            listingType = rs.getString(1);
                        }
                    }
                }
                if (listingType == null) {
                    conn.rollback();
                    return false;
                }

                ListingStatus next = ListingType.BORROW.name().equals(listingType)
                        ? ListingStatus.AVAILABLE : ListingStatus.CLOSED;
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE resource_items SET status = ? WHERE item_id = ?")) {
                    ps.setString(1, next.name());
                    ps.setInt(2, itemId);
                    ps.executeUpdate();
                }

                conn.commit();
                return true;
            } catch (SQLException e) {
                rollbackQuietly(conn);
                throw new DataAccessException("Could not complete request " + requestId, e);
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not complete request " + requestId, e);
        }
    }

    /**
     * The privacy safety net. Returns the requester's WhatsApp number ONLY if
     *  - the request is OVERDUE, and
     *  - ownerId is really the owner of this request.
     * Every successful reveal is logged on the request (contact_revealed = TRUE, revealed_at = now).
     * Returns null in every other case, so the number can never leak for an on-time borrow.
     * If the borrower registered with the very same number as the owner (for example two test accounts on one phone),
     * a chat link would just open the owner's own number, so nothing is revealed or logged and {@link #SAME_NUMBER}
     * is returned instead.
     */
    public String revealContact(int requestId, String ownerId) {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                String status = null;
                String requesterId = null;
                String realOwnerId = null;
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT status, requester_id, owner_id FROM requests WHERE request_id = ? FOR UPDATE")) {
                    ps.setInt(1, requestId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            status = rs.getString("status");
                            requesterId = rs.getString("requester_id");
                            realOwnerId = rs.getString("owner_id");
                        }
                    }
                }
                if (!RequestStatus.OVERDUE.name().equals(status) || realOwnerId == null || !realOwnerId.equals(ownerId)) {
                    conn.rollback();
                    return null;
                }

                String whatsapp = null;
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT whatsapp_number FROM students WHERE student_id = ?")) {
                    ps.setString(1, requesterId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            whatsapp = rs.getString(1);
                        }
                    }
                }
                if (whatsapp == null) {
                    conn.rollback();
                    return null;
                }

                String ownerNumber = null;
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT whatsapp_number FROM students WHERE student_id = ?")) {
                    ps.setString(1, realOwnerId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            ownerNumber = rs.getString(1);
                        }
                    }
                }
                if (whatsapp.equals(ownerNumber)) {
                    conn.rollback();   // a chat with this number would open the owner's own WhatsApp: reveal nothing
                    return SAME_NUMBER;
                }

                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE requests SET contact_revealed = TRUE, revealed_at = NOW() WHERE request_id = ?")) {
                    ps.setInt(1, requestId);
                    ps.executeUpdate();
                }
                // audit trail: who revealed which request, and when (FINAL_SPEC section 5)
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO contact_reveal_logs (request_id, owner_id) VALUES (?, ?)")) {
                    ps.setInt(1, requestId);
                    ps.setString(2, ownerId);
                    ps.executeUpdate();
                }

                conn.commit();
                return whatsapp;
            } catch (SQLException e) {
                rollbackQuietly(conn);
                throw new DataAccessException("Could not reveal contact for request " + requestId, e);
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not reveal contact for request " + requestId, e);
        }
    }

    // ------------------------------------------------------------ background thread

    /**
     * Flags every ACCEPTED request whose expected return date has passed as OVERDUE.
     * Called periodically by StaleReservationChecker.
     *
     * @return how many requests were flagged this time.
     */
    public int markOverdue() {
        // Compared against campus time (not the server clock, which may be UTC). Timed loans use due_at;
        // older requests only have the date, so they are overdue once that whole day has passed.
        LocalDateTime now = LocalDateTime.now(AppConstants.CAMPUS_ZONE);
        String sql = "UPDATE requests SET status = 'OVERDUE', was_overdue = TRUE "
                + "WHERE status = 'ACCEPTED' AND ((due_at IS NOT NULL AND due_at < ?) "
                + "OR (due_at IS NULL AND expected_return_date IS NOT NULL AND expected_return_date < ?))";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, now);
            ps.setDate(2, Date.valueOf(now.toLocalDate()));
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Could not flag overdue requests", e);
        }
    }


    private static void rollbackQuietly(Connection conn) {
        try {
            conn.rollback();
        } catch (SQLException ignored) {
            // the original exception is the one worth reporting
        }
    }
}
