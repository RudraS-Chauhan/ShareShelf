package com.shareshelf.dao;

import com.shareshelf.util.DBConnection;
import com.shareshelf.util.DataAccessException;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;

/** Whole-campus totals for the public login page. Read-only, no personal data. */
public class StatsDAO {

    /** @return students, items shared and completed exchanges (each handover that finished with a return). */
    public Map<String, Object> campusImpact() {
        String sql = "SELECT (SELECT COUNT(*) FROM students), "
                + "(SELECT COUNT(*) FROM resource_items), "
                + "(SELECT COUNT(*) FROM requests WHERE status = 'COMPLETED')";
        try (Connection conn = DBConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            Map<String, Object> m = new LinkedHashMap<>();
            rs.next();
            m.put("students", rs.getInt(1));
            m.put("items", rs.getInt(2));
            m.put("exchanges", rs.getInt(3));
            return m;
        } catch (SQLException e) {
            throw new DataAccessException("Could not load campus totals", e);
        }
    }
}
