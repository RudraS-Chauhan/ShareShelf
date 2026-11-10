package com.shareshelf.dao;

import com.shareshelf.model.Student;
import com.shareshelf.util.DBConnection;
import com.shareshelf.util.DataAccessException;
import com.shareshelf.util.PasswordUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;

/**
 * All SQL for the {@code students} table. Every query uses a PreparedStatement with ? placeholders
 * (never string concatenation) so user input can never be run as SQL.
 *
 * Passwords: register() hashes the raw password before storing it, and login() compares against the
 * stored hash, so no other class ever deals with password storage. Students returned from here have
 * their password field cleared.
 */
public class StudentDAO {

    /** @return true if the student was created; false if the ID already exists. */
    public boolean register(Student s) {
        String sql = "INSERT INTO students (student_id, name, branch, year, email, hostel_or_day, password, whatsapp_number) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, s.getStudentId());
            ps.setString(2, s.getName());
            ps.setString(3, s.getBranch());
            ps.setInt(4, s.getYear());
            ps.setString(5, s.getEmail());
            ps.setString(6, s.getHostelOrDay());
            ps.setString(7, PasswordUtil.hash(s.getPassword()));
            ps.setString(8, s.getWhatsappNumber());
            return ps.executeUpdate() == 1;
        } catch (SQLIntegrityConstraintViolationException e) {
            return false;   // two people registered the same ID at the same moment
        } catch (SQLException e) {
            throw new DataAccessException("Could not register student " + s.getStudentId(), e);
        }
    }

    /** @return the student when the ID and password match, otherwise null. */
    public Student login(String studentId, String password) {
        String sql = "SELECT * FROM students WHERE student_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next() || !PasswordUtil.verify(password, rs.getString("password"))) {
                    return null;
                }
                return mapRow(rs);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not log in student " + studentId, e);
        }
    }

    public boolean isDuplicateId(String studentId) {
        String sql = "SELECT 1 FROM students WHERE student_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not check student id " + studentId, e);
        }
    }

    /** @return the student, or null if there is no such ID. */
    public Student findById(String studentId) {
        String sql = "SELECT * FROM students WHERE student_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load student " + studentId, e);
        }
    }

    private Student mapRow(ResultSet rs) throws SQLException {
        Student s = new Student();
        s.setStudentId(rs.getString("student_id"));
        s.setName(rs.getString("name"));
        s.setBranch(rs.getString("branch"));
        s.setYear(rs.getInt("year"));
        s.setEmail(rs.getString("email"));
        s.setHostelOrDay(rs.getString("hostel_or_day"));
        s.setWhatsappNumber(rs.getString("whatsapp_number"));
        // password deliberately left null: the hash never leaves the DAO
        return s;
    }
}
