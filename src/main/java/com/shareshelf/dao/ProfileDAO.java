package com.shareshelf.dao;

import com.shareshelf.model.Profile;
import com.shareshelf.util.DBConnection;
import com.shareshelf.util.DataAccessException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Builds a public profile. A rating is only counted (and its comment only shown) once the other side
 * has rated the same request, so nobody can rate in revenge. Private data (email, WhatsApp) is never read here.
 */
public class ProfileDAO {

    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH);

    // "revealed" = the person being rated has rated this request too
    private static final String REVEALED = " AND EXISTS (SELECT 1 FROM ratings o WHERE o.request_id = r.request_id AND o.rater_id = r.ratee_id)";

    /** @return the profile, or null when the student does not exist. */
    public Profile load(String studentId) {
        Profile p = new Profile();
        try (Connection conn = DBConnection.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT student_id, name, branch, year, hostel_or_day FROM students WHERE student_id = ?")) {
                ps.setString(1, studentId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return null;
                    }
                    p.setStudentId(rs.getString(1));
                    p.setName(rs.getString(2));
                    p.setBranch(rs.getString(3));
                    p.setYear(rs.getInt(4));
                    p.setHostelOrDay(rs.getString(5));
                }
            }
            p.setCompleted(count(conn, "SELECT COUNT(*) FROM requests WHERE status = 'COMPLETED' AND (owner_id = ? OR requester_id = ?)", studentId, studentId));
            p.setLate(count(conn, "SELECT COUNT(*) FROM requests WHERE requester_id = ? AND was_overdue = TRUE", studentId));
            p.setListings(count(conn, "SELECT COUNT(*) FROM resource_items WHERE owner_id = ?", studentId));

            // as a borrower: the rater was the lender of that request
            double[] b = stars(conn, "q.owner_id = r.rater_id", studentId);
            p.setBorrowerStars(b[0]);
            p.setBorrowerStarsN((int) b[1]);
            // as a lender: the rater was the borrower of that request
            double[] l = stars(conn, "q.requester_id = r.rater_id", studentId);
            p.setLenderStars(l[0]);
            p.setLenderStarsN((int) l[1]);

            String sql = "SELECT r.stars, r.comment, rs.name, r.created_at, (q.owner_id = r.rater_id) AS rater_is_lender "
                    + "FROM ratings r JOIN requests q ON q.request_id = r.request_id JOIN students rs ON rs.student_id = r.rater_id "
                    + "WHERE r.ratee_id = ? AND r.comment IS NOT NULL AND r.comment <> ''" + REVEALED
                    + " ORDER BY r.rating_id DESC LIMIT 5";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, studentId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        LocalDateTime when = rs.getObject(4, LocalDateTime.class);
                        p.getReviews().add(new Profile.Review(rs.getInt(1), rs.getString(2), rs.getString(3),
                                rs.getBoolean(5) ? "borrower" : "lender", when == null ? "" : when.format(WHEN)));
                    }
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load the profile", e);
        }
        return p;
    }

    private static int count(Connection conn, String sql, String... args) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < args.length; i++) {
                ps.setString(i + 1, args[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    /** {average, count} of revealed ratings where the rater had the given role in the request. */
    private static double[] stars(Connection conn, String raterRole, String studentId) throws SQLException {
        String sql = "SELECT AVG(r.stars), COUNT(*) FROM ratings r JOIN requests q ON q.request_id = r.request_id "
                + "WHERE r.ratee_id = ? AND " + raterRole + REVEALED;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? new double[]{rs.getDouble(1), rs.getInt(2)} : new double[]{0, 0};
            }
        }
    }
}
