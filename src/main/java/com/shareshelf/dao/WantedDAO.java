package com.shareshelf.dao;

import com.shareshelf.model.Wanted;
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

/** The "wanted" board: students post what they need, others answer with "I have this". */
public class WantedDAO {

    /** @return the new post id, or -1 when the student already has the maximum number of open posts. */
    public int add(String studentId, String title, String category, String note) {
        if (countOpen(studentId) >= AppConstants.MAX_OPEN_WANTED) {
            return -1;
        }
        String sql = "INSERT INTO wanted_posts (student_id, title, category, note, status, created_at) VALUES (?, ?, ?, ?, 'OPEN', ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, studentId);
            ps.setString(2, title);
            ps.setString(3, category);
            ps.setString(4, note == null || note.isEmpty() ? null : note);
            ps.setObject(5, LocalDateTime.now(AppConstants.CAMPUS_ZONE));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not save the wanted post", e);
        }
    }

    public int countOpen(String studentId) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM wanted_posts WHERE student_id = ? AND status = 'OPEN'")) {
            ps.setString(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not count wanted posts", e);
        }
    }

    /** Open posts from the last 30 days, newest first. */
    public List<Wanted> listOpen() {
        String sql = "SELECT w.*, s.name, s.branch, s.year FROM wanted_posts w JOIN students s ON s.student_id = w.student_id "
                + "WHERE w.status = 'OPEN' AND w.created_at >= ? ORDER BY w.post_id DESC LIMIT 60";
        List<Wanted> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, LocalDateTime.now(AppConstants.CAMPUS_ZONE).minusDays(AppConstants.WANTED_DAYS));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load the wanted board", e);
        }
        return list;
    }

    /** @return the post, or null */
    public Wanted findById(int postId) {
        String sql = "SELECT w.*, s.name, s.branch, s.year FROM wanted_posts w JOIN students s ON s.student_id = w.student_id WHERE w.post_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, postId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load the wanted post", e);
        }
    }

    /** Only the poster can close their own post. @return true if it was open and is now closed. */
    public boolean close(int postId, String studentId) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE wanted_posts SET status = 'CLOSED' WHERE post_id = ? AND student_id = ? AND status = 'OPEN'")) {
            ps.setInt(1, postId);
            ps.setString(2, studentId);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new DataAccessException("Could not close the wanted post", e);
        }
    }

    /** Records "I have this" once per person per post, so the poster is not spammed. @return true the first time. */
    public boolean offerOnce(int postId, String offererId) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("INSERT IGNORE INTO wanted_offers (post_id, offerer_id) VALUES (?, ?)")) {
            ps.setInt(1, postId);
            ps.setString(2, offererId);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new DataAccessException("Could not save the offer", e);
        }
    }

    private static Wanted map(ResultSet rs) throws SQLException {
        Wanted w = new Wanted();
        w.setPostId(rs.getInt("post_id"));
        w.setStudentId(rs.getString("student_id"));
        w.setTitle(rs.getString("title"));
        w.setCategory(rs.getString("category"));
        w.setNote(rs.getString("note"));
        w.setCreatedAt(rs.getObject("created_at", LocalDateTime.class));
        w.setStudentName(rs.getString("name"));
        w.setBranch(rs.getString("branch"));
        w.setYear(rs.getInt("year"));
        return w;
    }
}
