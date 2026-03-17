package com.revature.revpay.dao;

import com.revature.revpay.model.PaymentMethod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Repository
public class PaymentMethodDAO {

    private static final Logger log = LogManager.getLogger(PaymentMethodDAO.class);
    private final DataSource dataSource;

    public PaymentMethodDAO(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    private PaymentMethod mapRow(ResultSet rs) throws SQLException {
        PaymentMethod pm = new PaymentMethod();
        pm.setMethodId(rs.getLong("method_id"));
        pm.setUserId(rs.getLong("user_id"));
        pm.setCardType(rs.getString("card_type"));
        pm.setCardNumberEnc(rs.getString("card_number_enc"));
        pm.setLastFour(rs.getString("last_four"));
        pm.setExpiryMonth(rs.getObject("expiry_month") != null ? rs.getInt("expiry_month") : null);
        pm.setExpiryYear(rs.getObject("expiry_year") != null ? rs.getInt("expiry_year") : null);
        pm.setCardholderName(rs.getString("cardholder_name"));
        pm.setBillingAddress(rs.getString("billing_address"));
        pm.setPinHash(rs.getString("pin_hash"));
        pm.setDefault(rs.getInt("is_default") == 1);
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null)
            pm.setCreatedAt(ts.toLocalDateTime());
        pm.setBalance(rs.getBigDecimal("balance"));
        return pm;
    }

    public Long save(PaymentMethod pm) {
        String sql = "INSERT INTO PAYMENT_METHODS (user_id, card_type, card_number_enc, last_four, " +
                "expiry_month, expiry_year, cardholder_name, billing_address, pin_hash, is_default, balance) " +
                "VALUES (?,?,?,?,?,?,?,?,?,?,?)";
        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql, new String[] { "METHOD_ID" })) {
            ps.setLong(1, pm.getUserId());
            ps.setString(2, pm.getCardType());
            ps.setString(3, pm.getCardNumberEnc());
            ps.setString(4, pm.getLastFour());
            ps.setObject(5, pm.getExpiryMonth());
            ps.setObject(6, pm.getExpiryYear());
            ps.setString(7, pm.getCardholderName());
            ps.setString(8, pm.getBillingAddress());
            ps.setString(9, pm.getPinHash());
            ps.setInt(10, pm.isDefault() ? 1 : 0);
            ps.setBigDecimal(11, pm.getBalance() != null ? pm.getBalance() : java.math.BigDecimal.valueOf(10000.00));
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next())
                return rs.getLong(1);
        } catch (SQLException e) {
            log.error("Error saving payment method: {}", e.getMessage(), e);
        }
        return null;
    }

    public List<PaymentMethod> findByUserId(Long userId) {
        List<PaymentMethod> list = new ArrayList<>();
        String sql = "SELECT * FROM PAYMENT_METHODS WHERE user_id=? ORDER BY is_default DESC, created_at DESC";
        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next())
                list.add(mapRow(rs));
        } catch (SQLException e) {
            log.error("Error findByUserId: {}", e.getMessage(), e);
        }
        return list;
    }

    public PaymentMethod findById(Long methodId) {
        String sql = "SELECT * FROM PAYMENT_METHODS WHERE method_id=?";
        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, methodId);
            ResultSet rs = ps.executeQuery();
            if (rs.next())
                return mapRow(rs);
        } catch (SQLException e) {
            log.error("Error findById: {}", e.getMessage(), e);
        }
        return null;
    }

    public void delete(Long methodId) {
        String sql = "DELETE FROM PAYMENT_METHODS WHERE method_id=?";
        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, methodId);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("Error delete: {}", e.getMessage(), e);
        }
    }

    public void setDefault(Long methodId, Long userId) {
        try (Connection conn = dataSource.getConnection()) {
            // Clear existing default
            PreparedStatement ps1 = conn.prepareStatement(
                    "UPDATE PAYMENT_METHODS SET is_default=0 WHERE user_id=?");
            ps1.setLong(1, userId);
            ps1.executeUpdate();
            // Set new default
            PreparedStatement ps2 = conn.prepareStatement(
                    "UPDATE PAYMENT_METHODS SET is_default=1 WHERE method_id=? AND user_id=?");
            ps2.setLong(1, methodId);
            ps2.setLong(2, userId);
            ps2.executeUpdate();
        } catch (SQLException e) {
            log.error("Error setDefault: {}", e.getMessage(), e);
        }
    }

    public void update(PaymentMethod pm) {
        String sql = "UPDATE PAYMENT_METHODS SET cardholder_name=?, billing_address=? WHERE method_id=? AND user_id=?";
        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, pm.getCardholderName());
            ps.setString(2, pm.getBillingAddress());
            ps.setLong(3, pm.getMethodId());
            ps.setLong(4, pm.getUserId());
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("Error update: {}", e.getMessage(), e);
        }
    }

    public void updatePin(Long methodId, String pinHash) {
        String sql = "UPDATE PAYMENT_METHODS SET pin_hash=? WHERE method_id=?";
        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, pinHash);
            ps.setLong(2, methodId);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("Error updatePin: {}", e.getMessage(), e);
        }
    }

    public void updateBalance(Long methodId, java.math.BigDecimal newBalance) {
        String sql = "UPDATE PAYMENT_METHODS SET balance=? WHERE method_id=?";
        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBigDecimal(1, newBalance);
            ps.setLong(2, methodId);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("Error updateBalance: {}", e.getMessage(), e);
        }
    }
}
