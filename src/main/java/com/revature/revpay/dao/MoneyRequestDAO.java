package com.revature.revpay.dao;

import com.revature.revpay.model.MoneyRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Repository
public class MoneyRequestDAO {

    private static final Logger log = LogManager.getLogger(MoneyRequestDAO.class);
    private final DataSource dataSource;

    public MoneyRequestDAO(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    private MoneyRequest mapRow(ResultSet rs) throws SQLException {
        MoneyRequest mr = new MoneyRequest();
        mr.setRequestId(rs.getLong("request_id"));
        mr.setRequesterId(rs.getLong("requester_id"));
        mr.setRequesteeId(rs.getLong("requestee_id"));
        mr.setAmount(rs.getBigDecimal("amount"));
        mr.setPurpose(rs.getString("purpose"));
        mr.setStatus(rs.getString("status"));
        try {
            mr.setRequesterName(rs.getString("requester_name"));
        } catch (SQLException ignored) {
        }
        try {
            mr.setRequesterEmail(rs.getString("requester_email"));
        } catch (SQLException ignored) {
        }
        try {
            mr.setRequesteeName(rs.getString("requestee_name"));
        } catch (SQLException ignored) {
        }
        try {
            mr.setRequesteeEmail(rs.getString("requestee_email"));
        } catch (SQLException ignored) {
        }
        Timestamp ca = rs.getTimestamp("created_at");
        if (ca != null)
            mr.setCreatedAt(ca.toLocalDateTime());
        Timestamp ua = rs.getTimestamp("updated_at");
        if (ua != null)
            mr.setUpdatedAt(ua.toLocalDateTime());
        return mr;
    }

    public Long save(MoneyRequest mr) {
        String sql = "INSERT INTO MONEY_REQUESTS (requester_id, requestee_id, amount, purpose, status) VALUES (?,?,?,?,'PENDING')";
        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql, new String[] { "REQUEST_ID" })) {
            ps.setLong(1, mr.getRequesterId());
            ps.setLong(2, mr.getRequesteeId());
            ps.setBigDecimal(3, mr.getAmount());
            ps.setString(4, mr.getPurpose());
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next())
                return rs.getLong(1);
        } catch (SQLException e) {
            log.error("Error save: {}", e.getMessage(), e);
        }
        return null;
    }

    public MoneyRequest findById(Long requestId) {
        String sql = "SELECT mr.*, " +
                "req.full_name AS requester_name, req.email AS requester_email, " +
                "ree.full_name AS requestee_name, ree.email AS requestee_email " +
                "FROM MONEY_REQUESTS mr " +
                "JOIN USERS req ON mr.requester_id = req.user_id " +
                "JOIN USERS ree ON mr.requestee_id = ree.user_id " +
                "WHERE mr.request_id=?";
        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, requestId);
            ResultSet rs = ps.executeQuery();
            if (rs.next())
                return mapRow(rs);
        } catch (SQLException e) {
            log.error("Error findById: {}", e.getMessage(), e);
        }
        return null;
    }

    // Requests sent TO this user (they need to accept/decline)
    public List<MoneyRequest> findIncoming(Long userId) {
        List<MoneyRequest> list = new ArrayList<>();
        String sql = "SELECT mr.*, " +
                "req.full_name AS requester_name, req.email AS requester_email, " +
                "ree.full_name AS requestee_name, ree.email AS requestee_email " +
                "FROM MONEY_REQUESTS mr " +
                "JOIN USERS req ON mr.requester_id = req.user_id " +
                "JOIN USERS ree ON mr.requestee_id = ree.user_id " +
                "WHERE mr.requestee_id=? ORDER BY mr.created_at DESC";
        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next())
                list.add(mapRow(rs));
        } catch (SQLException e) {
            log.error("Error findIncoming: {}", e.getMessage(), e);
        }
        return list;
    }

    // Requests made BY this user
    public List<MoneyRequest> findOutgoing(Long userId) {
        List<MoneyRequest> list = new ArrayList<>();
        String sql = "SELECT mr.*, " +
                "req.full_name AS requester_name, req.email AS requester_email, " +
                "ree.full_name AS requestee_name, ree.email AS requestee_email " +
                "FROM MONEY_REQUESTS mr " +
                "JOIN USERS req ON mr.requester_id = req.user_id " +
                "JOIN USERS ree ON mr.requestee_id = ree.user_id " +
                "WHERE mr.requester_id=? ORDER BY mr.created_at DESC";
        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next())
                list.add(mapRow(rs));
        } catch (SQLException e) {
            log.error("Error findOutgoing: {}", e.getMessage(), e);
        }
        return list;
    }

    public void updateStatus(Long requestId, String status) {
        String sql = "UPDATE MONEY_REQUESTS SET status=?, updated_at=CURRENT_TIMESTAMP WHERE request_id=?";
        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setLong(2, requestId);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("Error updateStatus: {}", e.getMessage(), e);
        }
    }

    public long countIncomingPending(Long userId) {
        String sql = "SELECT COUNT(*) FROM MONEY_REQUESTS WHERE requestee_id=? AND status='PENDING'";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getLong(1);
        } catch (SQLException e) {
            log.error("Error countIncomingPending: {}", e.getMessage(), e);
        }
        return 0;
    }
}
