package com.shareshelf.dao;

import com.shareshelf.util.AppConstants;
import com.shareshelf.util.DBConnection;
import com.shareshelf.util.DataAccessException;

import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;

/**
 * One-time handover / return proofs. The person who HOLDS the item side shows a QR (which holds a
 * long random token) and a 6-digit code; the other person scans or types it while standing next to them.
 *  - a token works once and only for its own request and kind (HANDOVER or RETURN)
 *  - it expires after AppConstants.HANDOVER_MINUTES
 *  - the short code is locked after AppConstants.MAX_CODE_ATTEMPTS wrong tries (stops guessing)
 */
public class HandoverDAO {

    public static final String HANDOVER = "HANDOVER";
    public static final String RETURN = "RETURN";

    private static final SecureRandom RANDOM = new SecureRandom();

    /** The token text and the 6-digit code shown to the other person. */
    public record Issued(String token, String code, LocalDateTime expiresAt) {
    }

    /** Creates a fresh token; older unused ones for the same request and kind stop working. */
    public Issued issue(int requestId, String kind) {
        byte[] bytes = new byte[16];
        RANDOM.nextBytes(bytes);
        StringBuilder hex = new StringBuilder();
        for (byte b : bytes) {
            hex.append(String.format("%02x", b));
        }
        String token = hex.toString();
        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        LocalDateTime expires = LocalDateTime.now(AppConstants.CAMPUS_ZONE).plusMinutes(AppConstants.HANDOVER_MINUTES);

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE handover_tokens SET used = TRUE WHERE request_id = ? AND kind = ? AND used = FALSE")) {
                    ps.setInt(1, requestId);
                    ps.setString(2, kind);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO handover_tokens (request_id, kind, token, code, expires_at) VALUES (?, ?, ?, ?, ?)")) {
                    ps.setInt(1, requestId);
                    ps.setString(2, kind);
                    ps.setString(3, token);
                    ps.setString(4, code);
                    ps.setObject(5, expires);
                    ps.executeUpdate();
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not create a handover code", e);
        }
        return new Issued(token, code, expires);
    }

    /** The request a token belongs to, or -1 if the token is unknown. Used to open the confirm page from a scan. */
    public int requestIdOfToken(String token) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT request_id FROM handover_tokens WHERE token = ?")) {
            ps.setString(1, token);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : -1;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not look up the token", e);
        }
    }

    /**
     * Uses up a proof. Exactly one of token / code must match the newest live proof for this
     * request and kind. A wrong code counts as an attempt.
     *
     * @return true once, for the first correct use; false for wrong, expired, locked or already-used proofs.
     */
    public boolean redeem(int requestId, String kind, String token, String code) {
        LocalDateTime now = LocalDateTime.now(AppConstants.CAMPUS_ZONE);
        try (Connection conn = DBConnection.getConnection()) {
            int id = -1;
            String realToken = null;
            String realCode = null;
            int attempts = 0;
            LocalDateTime expires = null;
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT token_id, token, code, attempts, expires_at FROM handover_tokens "
                    + "WHERE request_id = ? AND kind = ? AND used = FALSE ORDER BY token_id DESC LIMIT 1")) {
                ps.setInt(1, requestId);
                ps.setString(2, kind);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        id = rs.getInt(1);
                        realToken = rs.getString(2);
                        realCode = rs.getString(3);
                        attempts = rs.getInt(4);
                        expires = rs.getObject(5, LocalDateTime.class);
                    }
                }
            }
            if (id < 0 || expires == null || expires.isBefore(now) || attempts >= AppConstants.MAX_CODE_ATTEMPTS) {
                return false;
            }
            boolean good = (token != null && !token.isEmpty() && token.equals(realToken))
                    || (code != null && !code.isEmpty() && code.equals(realCode));
            if (!good) {
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE handover_tokens SET attempts = attempts + 1 WHERE token_id = ?")) {
                    ps.setInt(1, id);
                    ps.executeUpdate();
                }
                return false;
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE handover_tokens SET used = TRUE WHERE token_id = ? AND used = FALSE")) {
                ps.setInt(1, id);
                return ps.executeUpdate() == 1;   // guarded: two scans at once, only one wins
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not check the handover code", e);
        }
    }
}
