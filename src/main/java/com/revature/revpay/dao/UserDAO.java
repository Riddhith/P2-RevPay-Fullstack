package com.revature.revpay.dao;

import com.revature.revpay.model.User;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Repository
public class UserDAO {

    private static final Logger log = LogManager.getLogger(UserDAO.class);
    private final DataSource dataSource;

    public UserDAO(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    private User mapRow(ResultSet rs) throws SQLException {
        User u = new User();
        u.setUserId(rs.getLong("user_id"));
        u.setFullName(rs.getString("full_name"));
        u.setEmail(rs.getString("email"));
        u.setPhone(rs.getString("phone"));
        u.setUsername(rs.getString("username"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setRole(rs.getString("role"));
        u.setAccountType(rs.getString("account_type"));
        u.setWalletBalance(rs.getBigDecimal("wallet_balance"));
        u.setActive(rs.getInt("is_active") == 1);
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null)
            u.setCreatedAt(createdAt.toLocalDateTime());
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null)
            u.setUpdatedAt(updatedAt.toLocalDateTime());
        return u;
    }

    public Long save(User user) {
        String sql = "INSERT INTO USERS (full_name, email, phone, username, password_hash, role, account_type, wallet_balance, is_active) "
                +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, 1)";
        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql, new String[] { "USER_ID" })) {
            ps.setString(1, user.getFullName());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getPhone());
            ps.setString(4, user.getUsername());
            ps.setString(5, user.getPasswordHash());
            ps.setString(6, user.getRole() != null ? user.getRole() : "USER");
            ps.setString(7, user.getAccountType());
            ps.setBigDecimal(8, user.getWalletBalance() != null ? user.getWalletBalance() : BigDecimal.ZERO);
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next())
                return rs.getLong(1);
        } catch (SQLException e) {
            log.error("Error saving user: {}", e.getMessage(), e);
        }
        return null;
    }

    public User findById(Long userId) {
        String sql = "SELECT * FROM USERS WHERE user_id = ?";
        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next())
                return mapRow(rs);
        } catch (SQLException e) {
            log.error("Error findById: {}", e.getMessage(), e);
        }
        return null;
    }

    public User findByEmail(String email) {
        String sql = "SELECT * FROM USERS WHERE email = ?";
        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            if (rs.next())
                return mapRow(rs);
        } catch (SQLException e) {
            log.error("Error findByEmail: {}", e.getMessage(), e);
        }
        return null;
    }

    public User findByPhone(String phone) {
        String sql = "SELECT * FROM USERS WHERE phone = ?";
        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, phone);
            ResultSet rs = ps.executeQuery();
            if (rs.next())
                return mapRow(rs);
        } catch (SQLException e) {
            log.error("Error findByPhone: {}", e.getMessage(), e);
        }
        return null;
    }

    public User findByUsername(String username) {
        String sql = "SELECT * FROM USERS WHERE username = ?";
        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (rs.next())
                return mapRow(rs);
        } catch (SQLException e) {
            log.error("Error findByUsername: {}", e.getMessage(), e);
        }
        return null;
    }

    public User findByEmailOrPhone(String input) {
        String sql = "SELECT * FROM USERS WHERE email = ? OR phone = ?";
        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, input);
            ps.setString(2, input);
            ResultSet rs = ps.executeQuery();
            if (rs.next())
                return mapRow(rs);
        } catch (SQLException e) {
            log.error("Error findByEmailOrPhone: {}", e.getMessage(), e);
        }
        return null;
    }

    // Find user by username, email, or phone (for send/request money)
    public User findByUsernameOrEmailOrPhone(String input) {
        String sql = "SELECT * FROM USERS WHERE username = ? OR email = ? OR phone = ?";
        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, input);
            ps.setString(2, input);
            ps.setString(3, input);
            ResultSet rs = ps.executeQuery();
            if (rs.next())
                return mapRow(rs);
        } catch (SQLException e) {
            log.error("Error findByUsernameOrEmailOrPhone: {}", e.getMessage(), e);
        }
        return null;
    }

    public void updateProfile(User user) {
        String sql = "UPDATE USERS SET full_name=?, phone=?, updated_at=CURRENT_TIMESTAMP WHERE user_id=?";
        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user.getFullName());
            ps.setString(2, user.getPhone());
            ps.setLong(3, user.getUserId());
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("Error updateProfile: {}", e.getMessage(), e);
        }
    }

    public void updatePassword(Long userId, String newPasswordHash) {
        String sql = "UPDATE USERS SET password_hash=?, updated_at=CURRENT_TIMESTAMP WHERE user_id=?";
        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newPasswordHash);
            ps.setLong(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("Error updatePassword: {}", e.getMessage(), e);
        }
    }

    public void updateBalance(Long userId, BigDecimal newBalance) {
        String sql = "UPDATE USERS SET wallet_balance=?, updated_at=CURRENT_TIMESTAMP WHERE user_id=?";
        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBigDecimal(1, newBalance);
            ps.setLong(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("Error updateBalance: {}", e.getMessage(), e);
        }
    }

    public boolean emailExists(String email) {
        String sql = "SELECT COUNT(*) FROM USERS WHERE email = ?";
        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            if (rs.next())
                return rs.getInt(1) > 0;
        } catch (SQLException e) {
            log.error("Error emailExists: {}", e.getMessage(), e);
        }
        return false;
    }

    public boolean usernameExists(String username) {
        String sql = "SELECT COUNT(*) FROM USERS WHERE username = ?";
        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (rs.next())
                return rs.getInt(1) > 0;
        } catch (SQLException e) {
            log.error("Error usernameExists: {}", e.getMessage(), e);
        }
        return false;
    }

    public List<User> findByRole(String role) {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM USERS WHERE role = ?";
        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, role);
            ResultSet rs = ps.executeQuery();
            while (rs.next())
                users.add(mapRow(rs));
        } catch (SQLException e) {
            log.error("Error findByRole: {}", e.getMessage(), e);
        }
        return users;
    }

    public List<User> findAll() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM USERS ORDER BY created_at DESC";
        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next())
                users.add(mapRow(rs));
        } catch (SQLException e) {
            log.error("Error findAll: {}", e.getMessage(), e);
        }
        return users;
    }
}
