package com.shareshelf.dao;

import com.shareshelf.model.ListingStatus;
import com.shareshelf.model.ListingType;
import com.shareshelf.model.Request;
import com.shareshelf.model.RequestStatus;
import com.shareshelf.util.AppConstants;
import com.shareshelf.util.DBConnection;
import com.shareshelf.util.DataAccessException;

import java.sql.Connection;
import java.sql.Date;
import java.time.LocalDateTime;
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
 * Reads, inserts and single-row status changes. The multi-table transactions (accept, pickup, cancel,
 * return, contact reveal and the background sweeps) are in {@link RequestTransactionService}.
 * Every status change checks the CURRENT status in its WHERE clause, so a stale page or a
 * double click can never push a request through an invalid transition.
 */
public class RequestDAO implements Dao<Request, Integer> {

    /** Request columns plus item title/type/status, both names and both reliability counts. */
    private static final String SELECT_DETAILED =
            "SELECT q.*, i.title AS item_title, i.listing_type AS listing_type, i.status AS item_status, i.suggested_days AS item_suggested_days, "
            + "i.suggested_hours AS item_suggested_hours, rs.branch AS requester_branch, rs.year AS requester_year, "
            + "(SELECT COUNT(*) FROM requests l WHERE l.requester_id = q.requester_id AND l.was_overdue = TRUE) AS requester_late, "
            + "rs.name AS requester_name, os.name AS owner_name, "
            + "(SELECT COUNT(*) FROM requests c WHERE c.status = 'COMPLETED' "
            + "   AND (c.owner_id = q.requester_id OR c.requester_id = q.requester_id)) AS requester_completed, "
            + "(SELECT COUNT(*) FROM requests c WHERE c.status = 'COMPLETED' "
            + "   AND (c.owner_id = q.owner_id OR c.requester_id = q.owner_id)) AS owner_completed, "
            + stars("q.requester_id", "requester") + stars("q.owner_id", "owner")
            + "q.request_id AS rid "
            + "FROM requests q "
            + "JOIN resource_items i ON i.item_id = q.item_id "
            + "JOIN students rs ON rs.student_id = q.requester_id "
            + "JOIN students os ON os.student_id = q.owner_id ";

    /** Average stars and count for one student, counting only ratings whose other side has rated too. */
    private static String stars(String who, String alias) {
        String revealed = " FROM ratings r WHERE r.ratee_id = " + who + " AND EXISTS (SELECT 1 FROM ratings o "
                + "WHERE o.request_id = r.request_id AND o.rater_id = r.ratee_id)";
        return "(SELECT AVG(r.stars)" + revealed + ") AS " + alias + "_stars, "
                + "(SELECT COUNT(*)" + revealed + ") AS " + alias + "_stars_n, ";
    }

    // ------------------------------------------------------------ create / simple transitions

    /** Creates a PENDING request and stores the generated id back into r. */
    @Override
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

    /** Remembers what the owner saw when the item came back (OK, DAMAGED or MISSING). */
    public void setReturnCondition(int requestId, String condition) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("UPDATE requests SET return_condition = ? WHERE request_id = ?")) {
            ps.setString(1, condition);
            ps.setInt(2, requestId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Could not save the return condition", e);
        }
    }

    /**
     * A short fingerprint of the state of every request this student is part of. The browser compares it
     * every few seconds; when it changes, something happened (a new request, an accept, a pickup, a return).
     */
    public String stateSignature(String studentId) {
        String sql = "SELECT q.request_id, q.status, i.status FROM requests q JOIN resource_items i ON i.item_id = q.item_id "
                + "WHERE q.owner_id = ? OR q.requester_id = ? ORDER BY q.request_id";
        StringBuilder sb = new StringBuilder();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, studentId);
            ps.setString(2, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    sb.append(rs.getInt(1)).append(rs.getString(2)).append(rs.getString(3)).append(';');
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not read request states", e);
        }
        return Integer.toHexString(sb.toString().hashCode());
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
    @Override
    public Request findById(Integer requestId) {
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
        r.setItemSuggestedHours(rs.getInt("item_suggested_hours"));
        r.setRequesterBranch(rs.getString("requester_branch"));
        r.setRequesterYear(rs.getInt("requester_year"));
        r.setRequesterLate(rs.getInt("requester_late"));
        r.setMeetTime(rs.getObject("meet_time", LocalDateTime.class));
        r.setDueAt(rs.getObject("due_at", LocalDateTime.class));
        r.setMeetNote(rs.getString("meet_note"));
        r.setRequesterCompleted(rs.getInt("requester_completed"));
        r.setOwnerCompleted(rs.getInt("owner_completed"));
        r.setBorrowHours(rs.getInt("borrow_hours"));
        r.setCancelReason(rs.getString("cancel_reason"));
        r.setReturnCondition(rs.getString("return_condition"));
        r.setRequesterStars(rs.getDouble("requester_stars"));
        r.setRequesterStarsN(rs.getInt("requester_stars_n"));
        r.setOwnerStars(rs.getDouble("owner_stars"));
        r.setOwnerStarsN(rs.getInt("owner_stars_n"));
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
