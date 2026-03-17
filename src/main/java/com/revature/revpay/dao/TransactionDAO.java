package com.revature.revpay.dao;

import com.revature.revpay.model.Transaction;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Repository
public class TransactionDAO {

    private static final Logger log = LogManager.getLogger(TransactionDAO.class);
    private final DataSource dataSource;

    public TransactionDAO(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    private Transaction mapRow(ResultSet rs) throws SQLException {
        Transaction t = new Transaction();
        t.setTxnId(rs.getLong("txn_id"));
        t.setSenderId(rs.getObject("sender_id") != null ? rs.getLong("sender_id") : null);
        t.setReceiverId(rs.getObject("receiver_id") != null ? rs.getLong("receiver_id") : null);
        t.setAmount(rs.getBigDecimal("amount"));
        t.setTxnType(rs.getString("txn_type"));
        t.setStatus(rs.getString("status"));
        t.setNote(rs.getString("note"));
        t.setReferenceId(rs.getString("reference_id"));
        // Try to get joined names
        try {
            t.setSenderName(rs.getString("sender_name"));
        } catch (SQLException ignored) {
        }
        try {
            t.setReceiverName(rs.getString("receiver_name"));
        } catch (SQLException ignored) {
        }
        Timestamp ts = rs.getTimestamp("txn_timestamp");
        if (ts != null)
            t.setTxnTimestamp(ts.toLocalDateTime());
        return t;
    }

    public Long save(Transaction txn) {
        String sql = "INSERT INTO TRANSACTIONS (sender_id, receiver_id, amount, txn_type, status, note, reference_id) "
                +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql, new String[] { "TXN_ID" })) {
            if (txn.getSenderId() != null)
                ps.setLong(1, txn.getSenderId());
            else
                ps.setNull(1, Types.NUMERIC);
            if (txn.getReceiverId() != null)
                ps.setLong(2, txn.getReceiverId());
            else
                ps.setNull(2, Types.NUMERIC);
            ps.setBigDecimal(3, txn.getAmount());
            ps.setString(4, txn.getTxnType());
            ps.setString(5, txn.getStatus() != null ? txn.getStatus() : "COMPLETED");
            ps.setString(6, txn.getNote());
            ps.setString(7, txn.getReferenceId());
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next())
                return rs.getLong(1);
        } catch (SQLException e) {
            log.error("Error saving transaction: {}", e.getMessage(), e);
        }
        return null;
    }

    public Transaction findById(Long txnId) {
        String sql = "SELECT t.*, s.full_name AS sender_name, r.full_name AS receiver_name " +
                "FROM TRANSACTIONS t " +
                "LEFT JOIN USERS s ON t.sender_id = s.user_id " +
                "LEFT JOIN USERS r ON t.receiver_id = r.user_id " +
                "WHERE t.txn_id = ?";
        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, txnId);
            ResultSet rs = ps.executeQuery();
            if (rs.next())
                return mapRow(rs);
        } catch (SQLException e) {
            log.error("Error findById: {}", e.getMessage(), e);
        }
        return null;
    }

    public List<Transaction> findByUserId(Long userId, String type, LocalDate from, LocalDate to,
            BigDecimal minAmount, BigDecimal maxAmount,
            String search, int page, int pageSize) {
        List<Transaction> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
                "SELECT t.*, s.full_name AS sender_name, r.full_name AS receiver_name " +
                        "FROM TRANSACTIONS t " +
                        "LEFT JOIN USERS s ON t.sender_id = s.user_id " +
                        "LEFT JOIN USERS r ON t.receiver_id = r.user_id " +
                        "WHERE (t.sender_id = ? OR t.receiver_id = ?) ");
        List<Object> params = new ArrayList<>();
        params.add(userId);
        params.add(userId);

        if (type != null && !type.isBlank()) {
            sql.append("AND t.txn_type = ? ");
            params.add(type);
        }
        if (from != null) {
            sql.append("AND t.txn_timestamp >= ? ");
            params.add(Timestamp.valueOf(from.atStartOfDay()));
        }
        if (to != null) {
            sql.append("AND t.txn_timestamp <= ? ");
            params.add(Timestamp.valueOf(to.plusDays(1).atStartOfDay()));
        }
        if (minAmount != null) {
            sql.append("AND t.amount >= ? ");
            params.add(minAmount);
        }
        if (maxAmount != null) {
            sql.append("AND t.amount <= ? ");
            params.add(maxAmount);
        }
        if (search != null && !search.isBlank()) {
            sql.append("AND (LOWER(s.full_name) LIKE ? OR LOWER(r.full_name) LIKE ? OR TO_CHAR(t.txn_id) = ?) ");
            String like = "%" + search.toLowerCase() + "%";
            params.add(like);
            params.add(like);
            params.add(search);
        }
        sql.append("ORDER BY t.txn_timestamp DESC");

        // Oracle pagination
        String paginated = "SELECT * FROM (SELECT a.*, ROWNUM rn FROM (" + sql + ") a WHERE ROWNUM <= ?) WHERE rn > ?";
        int offset = (page - 1) * pageSize;

        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(paginated)) {
            int idx = 1;
            for (Object p : params) {
                if (p instanceof String)
                    ps.setString(idx++, (String) p);
                else if (p instanceof Long)
                    ps.setLong(idx++, (Long) p);
                else if (p instanceof Timestamp)
                    ps.setTimestamp(idx++, (Timestamp) p);
                else if (p instanceof BigDecimal)
                    ps.setBigDecimal(idx++, (BigDecimal) p);
                else
                    ps.setObject(idx++, p);
            }
            ps.setInt(idx++, offset + pageSize);
            ps.setInt(idx, offset);
            ResultSet rs = ps.executeQuery();
            while (rs.next())
                list.add(mapRow(rs));
        } catch (SQLException e) {
            log.error("Error findByUserId: {}", e.getMessage(), e);
        }
        return list;
    }

    public List<Transaction> findAllByUserId(Long userId) {
        List<Transaction> list = new ArrayList<>();
        String sql = "SELECT t.*, s.full_name AS sender_name, r.full_name AS receiver_name " +
                "FROM TRANSACTIONS t " +
                "LEFT JOIN USERS s ON t.sender_id = s.user_id " +
                "LEFT JOIN USERS r ON t.receiver_id = r.user_id " +
                "WHERE t.sender_id = ? OR t.receiver_id = ? " +
                "ORDER BY t.txn_timestamp DESC";
        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setLong(2, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next())
                list.add(mapRow(rs));
        } catch (SQLException e) {
            log.error("Error findAllByUserId: {}", e.getMessage(), e);
        }
        return list;
    }

    public List<Transaction> findRecentByUserId(Long userId, int limit) {
        List<Transaction> list = new ArrayList<>();
        String sql = "SELECT * FROM (SELECT t.*, s.full_name AS sender_name, r.full_name AS receiver_name " +
                "FROM TRANSACTIONS t " +
                "LEFT JOIN USERS s ON t.sender_id = s.user_id " +
                "LEFT JOIN USERS r ON t.receiver_id = r.user_id " +
                "WHERE t.sender_id = ? OR t.receiver_id = ? " +
                "ORDER BY t.txn_timestamp DESC) WHERE ROWNUM <= ?";
        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setLong(2, userId);
            ps.setInt(3, limit);
            ResultSet rs = ps.executeQuery();
            while (rs.next())
                list.add(mapRow(rs));
        } catch (SQLException e) {
            log.error("Error findRecentByUserId: {}", e.getMessage(), e);
        }
        return list;
    }

    public BigDecimal getTotalByType(Long userId, String txnType) {
        String sql = "SELECT NVL(SUM(amount),0) FROM TRANSACTIONS WHERE sender_id=? AND txn_type=? AND status='COMPLETED'";
        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setString(2, txnType);
            ResultSet rs = ps.executeQuery();
            if (rs.next())
                return rs.getBigDecimal(1);
        } catch (SQLException e) {
            log.error("Error getTotalByType: {}", e.getMessage(), e);
        }
        return BigDecimal.ZERO;
    }

    public BigDecimal getTotalReceived(Long userId) {
        String sql = "SELECT NVL(SUM(amount),0) FROM TRANSACTIONS WHERE receiver_id=? AND status='COMPLETED'";
        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next())
                return rs.getBigDecimal(1);
        } catch (SQLException e) {
            log.error("Error getTotalReceived: {}", e.getMessage(), e);
        }
        return BigDecimal.ZERO;
    }
}
