package com.shareshelf.dao;

import com.shareshelf.model.ListingStatus;
import com.shareshelf.model.ResourceItem;
import com.shareshelf.util.DBConnection;
import com.shareshelf.util.DataAccessException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** All SQL for the {@code resource_items} table (listings). */
public class ListingDAO {

    /** Item columns plus the owner's name and their completed-exchange count (reliability signal). */
    private static final String SELECT_WITH_OWNER =
            "SELECT i.*, s.name AS owner_name, "
            + "(SELECT COUNT(*) FROM requests c WHERE c.status = 'COMPLETED' "
            + "   AND (c.owner_id = i.owner_id OR c.requester_id = i.owner_id)) AS owner_completed "
            + "FROM resource_items i JOIN students s ON s.student_id = i.owner_id ";

    /** Creates a listing with status AVAILABLE and stores the generated id back into the item. */
    public boolean insert(ResourceItem item) {
        String sql = "INSERT INTO resource_items "
                + "(owner_id, title, description, category, listing_type, item_condition, location, suggested_days, status) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'AVAILABLE')";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, item.getOwnerId());
            ps.setString(2, item.getTitle());
            ps.setString(3, item.getDescription());
            ps.setString(4, item.getCategory());
            ps.setString(5, item.getListingType());
            ps.setString(6, item.getItemCondition());
            ps.setString(7, item.getLocation());
            if (item.getSuggestedDays() > 0) {
                ps.setInt(8, item.getSuggestedDays());
            } else {
                ps.setNull(8, Types.INTEGER);
            }
            if (ps.executeUpdate() != 1) {
                return false;
            }
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    item.setItemId(keys.getInt(1));
                }
            }
            return true;
        } catch (SQLException e) {
            throw new DataAccessException("Could not create listing '" + item.getTitle() + "'", e);
        }
    }

    /** AVAILABLE listings whose title contains the keyword (case-insensitive, spaces trimmed). */
    public List<ResourceItem> search(String keyword) {
        return searchAvailable(keyword, null, null);
    }

    /** AVAILABLE listings in a category and/or of a listing type (null or blank = any). */
    public List<ResourceItem> filter(String category, String listingType) {
        return searchAvailable(null, category, listingType);
    }

    /** The combined search used by the Search page: keyword + category + type, all optional. */
    public List<ResourceItem> searchAvailable(String keyword, String category, String listingType) {
        StringBuilder sql = new StringBuilder(SELECT_WITH_OWNER).append("WHERE i.status = 'AVAILABLE'");
        List<Object> params = new ArrayList<>();

        String kw = keyword == null ? "" : keyword.trim();
        if (!kw.isEmpty()) {
            sql.append(" AND LOWER(i.title) LIKE ? ESCAPE '!'");
            params.add("%" + escapeLike(kw.toLowerCase()) + "%");
        }
        if (category != null && !category.isBlank()) {
            sql.append(" AND i.category = ?");
            params.add(category.trim());
        }
        if (listingType != null && !listingType.isBlank()) {
            sql.append(" AND i.listing_type = ?");
            params.add(listingType.trim().toUpperCase());
        }
        sql.append(" ORDER BY i.item_id DESC");
        return queryList(sql.toString(), params);
    }

    /** Every listing the student owns, in any status (for the "My Listings" section). */
    public List<ResourceItem> findByOwner(String ownerId) {
        return queryList(SELECT_WITH_OWNER + "WHERE i.owner_id = ? ORDER BY i.item_id DESC", List.of((Object) ownerId));
    }

    /** @return true if a row was changed. */
    public boolean updateStatus(int itemId, String newStatus) {
        ListingStatus.valueOf(newStatus);   // rejects anything that is not a real status
        String sql = "UPDATE resource_items SET status = ? WHERE item_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newStatus);
            ps.setInt(2, itemId);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new DataAccessException("Could not update listing " + itemId, e);
        }
    }

    /** @return the listing, or null if there is no such id. */
    public ResourceItem findById(int itemId) {
        List<ResourceItem> found = queryList(SELECT_WITH_OWNER + "WHERE i.item_id = ?", List.of((Object) itemId));
        return found.isEmpty() ? null : found.get(0);
    }

    // ---------------------------------------------------------------- reports

    /** Number of listings per status; every status is present, even with 0. */
    public Map<String, Integer> countByStatus() {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (ListingStatus s : ListingStatus.values()) {
            counts.put(s.name(), 0);
        }
        counts.putAll(groupCount("SELECT status AS k, COUNT(*) AS n FROM resource_items GROUP BY status"));
        return counts;
    }

    /** Number of listings per category (only categories that have listings). */
    public Map<String, Integer> countByCategory() {
        return groupCount("SELECT category AS k, COUNT(*) AS n FROM resource_items GROUP BY category ORDER BY n DESC, category");
    }

    // ---------------------------------------------------------------- helpers

    private List<ResourceItem> queryList(String sql, List<Object> params) {
        List<ResourceItem> items = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    items.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load listings", e);
        }
        return items;
    }

    private Map<String, Integer> groupCount(String sql) {
        Map<String, Integer> result = new LinkedHashMap<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.put(rs.getString("k"), rs.getInt("n"));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load listing counts", e);
        }
        return result;
    }

    private ResourceItem mapRow(ResultSet rs) throws SQLException {
        ResourceItem item = new ResourceItem();
        item.setItemId(rs.getInt("item_id"));
        item.setOwnerId(rs.getString("owner_id"));
        item.setTitle(rs.getString("title"));
        item.setDescription(rs.getString("description"));
        item.setCategory(rs.getString("category"));
        item.setListingType(rs.getString("listing_type"));
        item.setItemCondition(rs.getString("item_condition"));
        item.setLocation(rs.getString("location"));
        item.setSuggestedDays(rs.getInt("suggested_days"));   // NULL reads as 0
        item.setStatus(rs.getString("status"));
        item.setOwnerName(rs.getString("owner_name"));
        item.setOwnerCompleted(rs.getInt("owner_completed"));
        return item;
    }

    /** Makes %, _ and ! in what the student typed match literally inside a LIKE pattern. */
    private static String escapeLike(String s) {
        return s.replace("!", "!!").replace("%", "!%").replace("_", "!_");
    }
}
