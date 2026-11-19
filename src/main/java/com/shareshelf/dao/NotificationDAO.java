package com.shareshelf.dao;

import com.shareshelf.model.Notification;
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
import java.util.logging.Level;
import java.util.logging.Logger;

/** In-app notifications. Sending one must never break the action that caused it, so add() only logs failures. */
public class NotificationDAO {

    private static final Logger LOG = Logger.getLogger(NotificationDAO.class.getName());

    /** Stores a notification for one student. Failures are logged, not thrown. */
    public void add(String studentId, String message, String link) {
        String sql = "INSERT INTO notifications (student_id, message, link, created_at) VALUES (?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, studentId);
            ps.setString(2, message.length() > 250 ? message.substring(0, 250) : message);
            ps.setString(3, link);
            ps.setObject(4, LocalDateTime.now(AppConstants.CAMPUS_ZONE));
            ps.executeUpdate();
        } catch (SQLException | DataAccessException e) {
            LOG.log(Level.WARNING, "Could not store notification for " + studentId, e);
        }
    }

    public int unreadCount(String studentId) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT COUNT(*) FROM notifications WHERE student_id = ? AND is_read = FALSE")) {
            ps.setString(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not count notifications", e);
        }
    }

    /** Newest first, at most 30. */
    public List<Notification> latest(String studentId) {
        List<Notification> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT * FROM notifications WHERE student_id = ? ORDER BY notification_id DESC LIMIT 30")) {
            ps.setString(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Notification n = new Notification();
                    n.setNotificationId(rs.getInt("notification_id"));
                    n.setMessage(rs.getString("message"));
                    n.setLink(rs.getString("link"));
                    n.setRead(rs.getBoolean("is_read"));
                    n.setCreatedAt(rs.getObject("created_at", LocalDateTime.class));
                    list.add(n);
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load notifications", e);
        }
        return list;
    }

    public void markAllRead(String studentId) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE notifications SET is_read = TRUE WHERE student_id = ? AND is_read = FALSE")) {
            ps.setString(1, studentId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Could not mark notifications read", e);
        }
    }
}
