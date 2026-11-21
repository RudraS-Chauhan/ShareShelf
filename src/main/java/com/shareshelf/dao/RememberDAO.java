package com.shareshelf.dao;

import com.shareshelf.util.AppConstants;
import com.shareshelf.util.DBConnection;
import com.shareshelf.util.DataAccessException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;

/**
 * "Keep me signed in on this device". The normal login lives in server memory and is lost whenever the
 * free host restarts or sleeps. This stores a long random token instead: the browser keeps
 * "selector:token" in a cookie, the database keeps only a SHA-256 hash of the token, so a leaked
 * database cannot be used to sign in. Logging out deletes the token.
 */
public class RememberDAO {

    private static final SecureRandom RANDOM = new SecureRandom();

    /** Creates a token for this student. @return the cookie value "selector:token". */
    public String issue(String studentId) {
        String selector = randomHex(12);   // 24 characters, finds the row
        String token = randomHex(32);      // 64 characters, the secret
        LocalDateTime expires = LocalDateTime.now(AppConstants.CAMPUS_ZONE).plusDays(AppConstants.REMEMBER_DAYS);
        try (Connection conn = DBConnection.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM remember_tokens WHERE expires_at < ?")) {
                ps.setObject(1, LocalDateTime.now(AppConstants.CAMPUS_ZONE));   // tidy up old rows
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO remember_tokens (student_id, selector, token_hash, expires_at) VALUES (?, ?, ?, ?)")) {
                ps.setString(1, studentId);
                ps.setString(2, selector);
                ps.setString(3, sha256(token));
                ps.setObject(4, expires);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not create the sign-in token", e);
        }
        return selector + ":" + token;
    }

    /** @return the student id for a valid, unexpired cookie value; otherwise null. */
    public String studentFor(String cookieValue) {
        if (cookieValue == null) {
            return null;
        }
        int colon = cookieValue.indexOf(':');
        if (colon != 24 || cookieValue.length() != 24 + 1 + 64) {
            return null;
        }
        String selector = cookieValue.substring(0, colon);
        String token = cookieValue.substring(colon + 1);
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT student_id, token_hash, expires_at FROM remember_tokens WHERE selector = ?")) {
            ps.setString(1, selector);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                boolean sameToken = MessageDigest.isEqual(   // constant-time comparison
                        rs.getString(2).getBytes(StandardCharsets.UTF_8), sha256(token).getBytes(StandardCharsets.UTF_8));
                LocalDateTime expires = rs.getObject(3, LocalDateTime.class);
                boolean live = expires != null && expires.isAfter(LocalDateTime.now(AppConstants.CAMPUS_ZONE));
                return sameToken && live ? rs.getString(1) : null;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not check the sign-in token", e);
        }
    }

    /** Logout: the token stops working immediately. */
    public void delete(String cookieValue) {
        if (cookieValue == null || cookieValue.length() < 24) {
            return;
        }
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM remember_tokens WHERE selector = ?")) {
            ps.setString(1, cookieValue.substring(0, 24));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Could not remove the sign-in token", e);
        }
    }

    private static String randomHex(int bytes) {
        byte[] b = new byte[bytes];
        RANDOM.nextBytes(b);
        StringBuilder sb = new StringBuilder();
        for (byte x : b) {
            sb.append(String.format("%02x", x));
        }
        return sb.toString();
    }

    private static String sha256(String text) {
        try {
            byte[] d = MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte x : d) {
                sb.append(String.format("%02x", x));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
