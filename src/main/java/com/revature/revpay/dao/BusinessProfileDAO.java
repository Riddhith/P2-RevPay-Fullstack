package com.revature.revpay.dao;

import com.revature.revpay.model.BusinessProfile;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;

@Repository
public class BusinessProfileDAO {
    private static final Logger log = LogManager.getLogger(BusinessProfileDAO.class);
    private final DataSource dataSource;

    public BusinessProfileDAO(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    private BusinessProfile mapRow(ResultSet rs) throws SQLException {
        BusinessProfile bp = new BusinessProfile();
        bp.setProfileId(rs.getLong("profile_id"));
        bp.setUserId(rs.getLong("user_id"));
        bp.setBusinessName(rs.getString("business_name"));
        bp.setBusinessType(rs.getString("business_type"));
        bp.setTaxId(rs.getString("tax_id"));
        bp.setGstNumber(rs.getString("gst_number"));
        bp.setAddress(rs.getString("address"));
        bp.setStatus(rs.getString("status"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null)
            bp.setCreatedAt(ts.toLocalDateTime());
        return bp;
    }

    public Long save(BusinessProfile bp) {
        String sql = "INSERT INTO BUSINESS_PROFILES (user_id, business_name, business_type, tax_id, gst_number, address, status) VALUES (?,?,?,?,?,?,'PENDING')";
        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql, new String[] { "PROFILE_ID" })) {
            ps.setLong(1, bp.getUserId());
            ps.setString(2, bp.getBusinessName());
            ps.setString(3, bp.getBusinessType());
            ps.setString(4, bp.getTaxId());
            ps.setString(5, bp.getGstNumber());
            ps.setString(6, bp.getAddress());
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next())
                return rs.getLong(1);
        } catch (SQLException e) {
            log.error("Error save profile: {}", e.getMessage(), e);
        }
        return null;
    }

    public BusinessProfile findByUserId(Long userId) {
        String sql = "SELECT * FROM BUSINESS_PROFILES WHERE user_id=?";
        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next())
                return mapRow(rs);
        } catch (SQLException e) {
            log.error("Error findByUserId: {}", e.getMessage(), e);
        }
        return null;
    }

    public void update(BusinessProfile bp) {
        String sql = "UPDATE BUSINESS_PROFILES SET business_name=?, business_type=?, tax_id=?, gst_number=?, address=? WHERE user_id=?";
        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, bp.getBusinessName());
            ps.setString(2, bp.getBusinessType());
            ps.setString(3, bp.getTaxId());
            ps.setString(4, bp.getGstNumber());
            ps.setString(5, bp.getAddress());
            ps.setLong(6, bp.getUserId());
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("Error update: {}", e.getMessage(), e);
        }
    }

    public java.util.List<BusinessProfile> findAllPending() {
        java.util.List<BusinessProfile> list = new java.util.ArrayList<>();
        String sql = "SELECT * FROM BUSINESS_PROFILES WHERE status='PENDING' ORDER BY created_at ASC";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            log.error("Error findAllPending: {}", e.getMessage(), e);
        }
        return list;
    }

    public java.util.List<BusinessProfile> findAllApproved() {
        java.util.List<BusinessProfile> list = new java.util.ArrayList<>();
        String sql = "SELECT * FROM BUSINESS_PROFILES WHERE status='APPROVED' ORDER BY created_at DESC";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            log.error("Error findAllApproved: {}", e.getMessage(), e);
        }
        return list;
    }

    public BusinessProfile findById(Long profileId) {
        String sql = "SELECT * FROM BUSINESS_PROFILES WHERE profile_id=?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, profileId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) {
            log.error("Error findById: {}", e.getMessage(), e);
        }
        return null;
    }

    public void updateStatus(Long profileId, String status) {
        String sql = "UPDATE BUSINESS_PROFILES SET status=? WHERE profile_id=?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setLong(2, profileId);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("Error updateStatus: {}", e.getMessage(), e);
        }
    }

    public void updateGstStatus(Long profileId, String status, String gstNumber) {
        String sql = "UPDATE BUSINESS_PROFILES SET status=?, gst_number=? WHERE profile_id=?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setString(2, gstNumber);
            ps.setLong(3, profileId);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("Error updateGstStatus: {}", e.getMessage(), e);
        }
    }
    public long countPendingApprovals() {
        String sql = "SELECT COUNT(*) FROM BUSINESS_PROFILES WHERE status='PENDING'";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getLong(1);
        } catch (SQLException e) {
            log.error("Error countPendingApprovals: {}", e.getMessage(), e);
        }
        return 0;
    }
}
