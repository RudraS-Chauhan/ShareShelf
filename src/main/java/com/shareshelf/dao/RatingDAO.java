package com.shareshelf.dao;

import com.shareshelf.util.AppConstants;
import com.shareshelf.util.DBConnection;
import com.shareshelf.util.DataAccessException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Ratings given at the end of a borrow. One per person per request; hidden until both sides have rated. */
public class RatingDAO {

    /** A finished borrow this student still has to rate (as lender or as borrower). */
    public static final class Pending {
        private final int requestId;
        private final String itemTitle, otherId, otherName;
        private final boolean rateBorrower;   // true when this student was the lender, so they rate the borrower

        public Pending(int requestId, String itemTitle, String otherId, String otherName, boolean rateBorrower) {
            this.requestId = requestId;
            this.itemTitle = itemTitle;
            this.otherId = otherId;
            this.otherName = otherName;
            this.rateBorrower = rateBorrower;
        }

        // plain getters: the JSP expression language reads bean properties, not record accessors
        public int getRequestId() { return requestId; }
        public String getItemTitle() { return itemTitle; }
        public String getOtherId() { return otherId; }
        public String getOtherName() { return otherName; }
        public boolean isRateBorrower() { return rateBorrower; }
        public String getRoleText() { return rateBorrower ? "borrower" : "lender"; }
    }

    /**
     * Saves a rating. @return false if stars are outside 1-5 or this person already rated this request.
     * The caller has already checked that the rater really took part in the request.
     */
    public boolean add(int requestId, String raterId, String rateeId, int stars, String condition, String comment) {
        if (stars < 1 || stars > 5) {
            return false;
        }
        String sql = "INSERT INTO ratings (request_id, rater_id, ratee_id, stars, item_condition, comment, created_at) "
                + "SELECT ?, ?, ?, ?, ?, ?, ? FROM DUAL WHERE NOT EXISTS "
                + "(SELECT 1 FROM ratings WHERE request_id = ? AND rater_id = ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, requestId);
            ps.setString(2, raterId);
            ps.setString(3, rateeId);
            ps.setInt(4, stars);
            ps.setString(5, condition);
            ps.setString(6, comment);
            ps.setObject(7, LocalDateTime.now(AppConstants.CAMPUS_ZONE));
            ps.setInt(8, requestId);
            ps.setString(9, raterId);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            // two clicks at once: the UNIQUE key refuses the second one
            if (e instanceof java.sql.SQLIntegrityConstraintViolationException) {
                return false;
            }
            throw new DataAccessException("Could not save the rating", e);
        }
    }

    /** Finished borrows (either side) that have no rating from this student yet, oldest first. */
    public List<Pending> pendingFor(String studentId) {
        String sql = "SELECT q.request_id, i.title, q.owner_id, os.name, q.requester_id, rs.name FROM requests q "
                + "JOIN resource_items i ON i.item_id = q.item_id "
                + "JOIN students os ON os.student_id = q.owner_id "
                + "JOIN students rs ON rs.student_id = q.requester_id "
                + "WHERE (q.requester_id = ? OR q.owner_id = ?) AND q.status = 'COMPLETED' AND i.listing_type = 'BORROW' "
                + "AND NOT EXISTS (SELECT 1 FROM ratings r WHERE r.request_id = q.request_id AND r.rater_id = ?) "
                + "ORDER BY q.request_id";
        List<Pending> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, studentId);
            ps.setString(2, studentId);
            ps.setString(3, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    boolean iAmOwner = studentId.equals(rs.getString(3));
                    list.add(iAmOwner
                            ? new Pending(rs.getInt(1), rs.getString(2), rs.getString(5), rs.getString(6), true)
                            : new Pending(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4), false));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load pending ratings", e);
        }
        return list;
    }

    /** True if this person already rated this request. */
    public boolean hasRated(int requestId, String raterId) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT 1 FROM ratings WHERE request_id = ? AND rater_id = ?")) {
            ps.setInt(1, requestId);
            ps.setString(2, raterId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not check the rating", e);
        }
    }
}
