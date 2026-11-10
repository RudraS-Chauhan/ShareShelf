package com.shareshelf.dao;

import com.shareshelf.model.ListingStatus;
import com.shareshelf.model.ListingType;
import com.shareshelf.model.Request;
import com.shareshelf.model.RequestStatus;
import com.shareshelf.util.DBConnection;
import com.shareshelf.util.DataAccessException;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * All SQL for the {@code requests} table.
 *
 * accept() and complete() change two tables at once (requests + resource_items), so they run as real
 * JDBC transactions: setAutoCommit(false) ... commit(), with rollback() if anything goes wrong.
 * Every status change also checks the CURRENT status in its WHERE clause, so a stale page or a
 * double click can never push a request through an invalid transition.
 */
public class RequestDAO {

    /** Request columns plus item title/type/status, both names and both reliability counts. */
    private static final String SELECT_DETAILED =
            "SELECT q.*, i.title AS item_title, i.listing_type AS listing_type, i.status AS item_status, i.suggested_days AS item_suggested_days, "
            + "rs.name AS requester_name, os.name AS owner_name, "
            + "(SELECT COUNT(*) FROM requests c WHERE c.status = 'COMPLETED' "
            + "   AND (c.owner_id = q.requester_id OR c.requester_id = q.requester_id)) AS requester_completed, "
            + "(SELECT COUNT(*) FROM requests c WHERE c.status = 'COMPLETED' "
            + "   AND (c.owner_id = q.owner_id OR c.requester_id = q.owner_id)) AS owner_completed "
            + "FROM requests q "
            + "JOIN resource_items i ON i.item_id = q.item_id "
            + "JOIN students rs ON rs.student_id = q.requester_id "
            + "JOIN students os ON os.student_id = q.owner_id ";

    // ------------------------------------------------------------ create / simple transitions

    /** Creates a PENDING request and stores the generated id back into r. */
    public boolean insert(Request r) {
        String sql = "INSERT INTO requests (item_id, requester_id, owner_id, message, status) VALUES (?, ?, ?, ?, 'PENDING')";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, r.getItemId());
            ps.setString(2, r.getRequesterId());
            ps.setString(3, r.getOwnerId());
            ps.setString(4, r.getMessage());
            if (ps.executeUpdate() != 1) {
                return false;
            }
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    r.setRequestId(keys.getInt(1));
                }
            }
            return true;
        } catch (SQLException e) {
            throw new DataAccessException("Could not create request for item " + r.getItemId(), e);
        }
    }

    /** True if this student already has a PENDING request on this item. */
    public boolean hasDuplicatePending(int itemId, String requesterId) {
        String sql = "SELECT 1 FROM requests WHERE item_id = ? AND requester_id = ? AND status = 'PENDING'";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, itemId);
            ps.setString(2, requesterId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not check for duplicate requests", e);
        }
    }

    /** Owner rejects a PENDING request. @return true if it was pending and is now REJECTED. */
    public boolean reject(int requestId) {
        return updateIfStatus(requestId, RequestStatus.REJECTED, RequestStatus.PENDING);
    }

    /** Requester withdraws a PENDING request. @return true if it was pending and is now CANCELLED. */
    public boolean cancel(int requestId) {
        return updateIfStatus(requestId, RequestStatus.CANCELLED, RequestStatus.PENDING);
    }

    // ------------------------------------------------------------ transactions

    /**
     * TRANSACTION: the owner accepts a request.
     *  1. request PENDING -> ACCEPTED, with the expected return date (null for non-BORROW listings)
     *  2. listing AVAILABLE -> RESERVED
     *  3. every other PENDING request on the same listing -> REJECTED (the item is taken)
     * Either all three happen or none do.
     *
     * @return false (and nothing changed) if the request is no longer pending or the item is no longer available.
     */
    public boolean accept(int requestId, int itemId, Date expectedReturnDate) {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int accepted;
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE requests SET status = 'ACCEPTED', expected_return_date = ? "
                        + "WHERE request_id = ? AND item_id = ? AND status = 'PENDING'")) {
                    if (expectedReturnDate != null) {
                        ps.setDate(1, expectedReturnDate);
                    } else {
                        ps.setNull(1, Types.DATE);
                    }
                    ps.setInt(2, requestId);
                    ps.setInt(3, itemId);
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
        String sql = "UPDATE requests SET status = 'OVERDUE' "
                + "WHERE status = 'ACCEPTED' AND expected_return_date IS NOT NULL AND expected_return_date < CURDATE()";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Could not flag overdue requests", e);
        }
    }

    // ------------------------------------------------------------ queries

    /** Requests this student has SENT, newest first. */
    public List<Request> findByRequester(String studentId) {
        return queryList(SELECT_DETAILED + "WHERE q.requester_id = ? ORDER BY q.request_id DESC", studentId);
    }

    /** Requests RECEIVED on this student's listings, newest first. */
    public List<Request> findByOwner(String studentId) {
        return queryList(SELECT_DETAILED + "WHERE q.owner_id = ? ORDER BY q.request_id DESC", studentId);
    }

    /** @return the request, or null if there is no such id. */
    public Request findById(int requestId) {
        List<Request> found = queryList(SELECT_DETAILED + "WHERE q.request_id = ?", requestId);
        return found.isEmpty() ? null : found.get(0);
    }

    // ------------------------------------------------------------ reliability + reports

    /** Reliability signal: how many exchanges this student has completed (as owner or requester). */
    public int countCompleted(String studentId) {
        String sql = "SELECT COUNT(*) FROM requests WHERE status = 'COMPLETED' AND (owner_id = ? OR requester_id = ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, studentId);
            ps.setString(2, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not count completed exchanges", e);
        }
    }

    /** Number of requests per status; every status is present, even with 0. */
    public Map<String, Integer> countByStatus() {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (RequestStatus s : RequestStatus.values()) {
            counts.put(s.name(), 0);
        }
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT status, COUNT(*) AS n FROM requests GROUP BY status");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                counts.put(rs.getString("status"), rs.getInt("n"));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not count requests", e);
        }
        return counts;
    }

    /** Categories ranked by how many requests their listings received (the "innovation" stat). */
    public Map<String, Integer> topRequestedCategories(int limit) {
        String sql = "SELECT i.category AS category, COUNT(*) AS n FROM requests q "
                + "JOIN resource_items i ON i.item_id = q.item_id "
                + "GROUP BY i.category ORDER BY n DESC, i.category LIMIT ?";
        Map<String, Integer> result = new LinkedHashMap<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.put(rs.getString("category"), rs.getInt("n"));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not rank categories", e);
        }
        return result;
    }

    // ------------------------------------------------------------ helpers

    private boolean updateIfStatus(int requestId, RequestStatus newStatus, RequestStatus requiredStatus) {
        String sql = "UPDATE requests SET status = ? WHERE request_id = ? AND status = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newStatus.name());
            ps.setInt(2, requestId);
            ps.setString(3, requiredStatus.name());
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new DataAccessException("Could not update request " + requestId, e);
        }
    }

    private List<Request> queryList(String sql, Object param) {
        List<Request> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load requests", e);
        }
        return list;
    }

    private Request mapRow(ResultSet rs) throws SQLException {
        Request r = new Request();
        r.setRequestId(rs.getInt("request_id"));
        r.setItemId(rs.getInt("item_id"));
        r.setRequesterId(rs.getString("requester_id"));
        r.setOwnerId(rs.getString("owner_id"));
        r.setMessage(rs.getString("message"));
        r.setStatus(rs.getString("status"));
        r.setExpectedReturnDate(rs.getDate("expected_return_date"));
        r.setContactRevealed(rs.getBoolean("contact_revealed"));
        r.setRevealedAt(rs.getTimestamp("revealed_at"));
        r.setCreatedAt(rs.getTimestamp("created_at"));
        r.setItemTitle(rs.getString("item_title"));
        r.setListingType(rs.getString("listing_type"));
        r.setItemStatus(rs.getString("item_status"));
        r.setItemSuggestedDays(rs.getInt("item_suggested_days"));   // NULL reads as 0
        r.setRequesterName(rs.getString("requester_name"));
        r.setOwnerName(rs.getString("owner_name"));
        r.setRequesterCompleted(rs.getInt("requester_completed"));
        r.setOwnerCompleted(rs.getInt("owner_completed"));
        return r;
    }

    private static void rollbackQuietly(Connection conn) {
        try {
            conn.rollback();
        } catch (SQLException ignored) {
            // the original exception is the one worth reporting
        }
    }
}
