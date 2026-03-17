package com.revature.revpay.dao;

import com.revature.revpay.model.LoanApplication;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Repository
public class LoanApplicationDAO {
    private static final Logger log = LogManager.getLogger(LoanApplicationDAO.class);
    private final DataSource dataSource;

    public LoanApplicationDAO(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    private LoanApplication mapRow(ResultSet rs) throws SQLException {
        LoanApplication la = new LoanApplication();
        la.setLoanId(rs.getLong("loan_id"));
        la.setUserId(rs.getLong("user_id"));
        la.setLoanAmount(rs.getBigDecimal("loan_amount"));
        la.setPurpose(rs.getString("purpose"));
        la.setTenureMonths(rs.getInt("tenure_months"));
        la.setInterestRate(rs.getBigDecimal("interest_rate"));
        la.setMonthlyEmi(rs.getBigDecimal("monthly_emi"));
        la.setStatus(rs.getString("status"));
        BigDecimal amtPaid = rs.getBigDecimal("amount_paid");
        la.setAmountPaid(amtPaid != null ? amtPaid : BigDecimal.ZERO);
        la.setDocPath(rs.getString("doc_path"));
        try {
            la.setApplicantName(rs.getString("applicant_name"));
        } catch (SQLException ignored) {
        }
        Timestamp ca = rs.getTimestamp("created_at");
        if (ca != null)
            la.setCreatedAt(ca.toLocalDateTime());
        Timestamp ua = rs.getTimestamp("updated_at");
        if (ua != null)
            la.setUpdatedAt(ua.toLocalDateTime());
        return la;
    }

    public Long save(LoanApplication la) {
        String sql = "INSERT INTO LOAN_APPLICATIONS (user_id, loan_amount, purpose, tenure_months, interest_rate, status, doc_path) VALUES (?,?,?,?,?,?,?)";
        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql, new String[] { "LOAN_ID" })) {
            ps.setLong(1, la.getUserId());
            ps.setBigDecimal(2, la.getLoanAmount());
            ps.setString(3, la.getPurpose());
            ps.setInt(4, la.getTenureMonths());
            ps.setBigDecimal(5, la.getInterestRate() != null ? la.getInterestRate() : BigDecimal.valueOf(12));
            ps.setString(6, "PENDING");
            ps.setString(7, la.getDocPath());
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next())
                return rs.getLong(1);
        } catch (SQLException e) {
            log.error("Error save loan: {}", e.getMessage(), e);
        }
        return null;
    }

    public LoanApplication findById(Long loanId) {
        String sql = "SELECT la.*, u.full_name AS applicant_name FROM LOAN_APPLICATIONS la JOIN USERS u ON la.user_id=u.user_id WHERE la.loan_id=?";
        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, loanId);
            ResultSet rs = ps.executeQuery();
            if (rs.next())
                return mapRow(rs);
        } catch (SQLException e) {
            log.error("Error findById: {}", e.getMessage(), e);
        }
        return null;
    }

    public List<LoanApplication> findByUserId(Long userId) {
        List<LoanApplication> list = new ArrayList<>();
        String sql = "SELECT la.*, u.full_name AS applicant_name FROM LOAN_APPLICATIONS la JOIN USERS u ON la.user_id=u.user_id WHERE la.user_id=? ORDER BY la.created_at DESC";
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

    public List<LoanApplication> findAllPending() {
        List<LoanApplication> list = new ArrayList<>();
        String sql = "SELECT la.*, u.full_name AS applicant_name FROM LOAN_APPLICATIONS la JOIN USERS u ON la.user_id=u.user_id WHERE la.status='PENDING' ORDER BY la.created_at ASC";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            log.error("Error findAllPending: {}", e.getMessage(), e);
        }
        return list;
    }

    public List<LoanApplication> findAllApproved() {
        List<LoanApplication> list = new ArrayList<>();
        String sql = "SELECT la.*, u.full_name AS applicant_name FROM LOAN_APPLICATIONS la JOIN USERS u ON la.user_id=u.user_id WHERE la.status IN ('APPROVED', 'ACTIVE', 'CLOSED') ORDER BY la.created_at DESC";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            log.error("Error findAllApproved: {}", e.getMessage(), e);
        }
        return list;
    }

    public void updateStatus(Long loanId, String status) {
        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(
                        "UPDATE LOAN_APPLICATIONS SET status=?, updated_at=CURRENT_TIMESTAMP WHERE loan_id=?")) {
            ps.setString(1, status);
            ps.setLong(2, loanId);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("Error updateStatus: {}", e.getMessage(), e);
        }
    }

    public void addRepayment(Long loanId, BigDecimal amount) {
        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(
                        "UPDATE LOAN_APPLICATIONS SET amount_paid=amount_paid+?, updated_at=CURRENT_TIMESTAMP WHERE loan_id=?")) {
            ps.setBigDecimal(1, amount);
            ps.setLong(2, loanId);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("Error addRepayment: {}", e.getMessage(), e);
        }
    }

    public long countPendingForAdmin() {
        String sql = "SELECT COUNT(*) FROM LOAN_APPLICATIONS WHERE status='PENDING'";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getLong(1);
        } catch (SQLException e) {
            log.error("Error countPendingForAdmin: {}", e.getMessage(), e);
        }
        return 0;
    }

    public long countPendingForBusiness(Long userId) {
        String sql = "SELECT COUNT(*) FROM LOAN_APPLICATIONS WHERE user_id=? AND status='PENDING'";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getLong(1);
        } catch (SQLException e) {
            log.error("Error countPendingForBusiness: {}", e.getMessage(), e);
        }
        return 0;
    }
}
