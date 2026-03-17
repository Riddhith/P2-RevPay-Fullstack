package com.revature.revpay.dao;

import com.revature.revpay.model.Notification;
import com.revature.revpay.model.NotificationPreference;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Repository
public class NotificationDAO {

    private static final Logger log = LogManager.getLogger(NotificationDAO.class);
    private final DataSource dataSource;

    public NotificationDAO(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    private Notification mapRow(ResultSet rs) throws SQLException {
        Notification n = new Notification();
        n.setNotifId(rs.getLong("notif_id"));
        n.setUserId(rs.getLong("user_id"));
        n.setTitle(rs.getString("title"));
        n.setMessage(rs.getString("message"));
        n.setNotifType(rs.getString("notif_type"));
        n.setRead(rs.getInt("is_read") == 1);
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null)
            n.setCreatedAt(ts.toLocalDateTime());
        return n;
    }

    public void save(Notification notif) {
        String sql = "INSERT INTO NOTIFICATIONS (user_id, title, message, notif_type, is_read) VALUES (?,?,?,?,0)";
        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql, new String[] { "NOTIF_ID" })) {
            ps.setLong(1, notif.getUserId());
            ps.setString(2, notif.getTitle());
            ps.setString(3, notif.getMessage());
            ps.setString(4, notif.getNotifType());
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("Error save notification: {}", e.getMessage(), e);
        }
    }

    public List<Notification> findByUserId(Long userId) {
        List<Notification> list = new ArrayList<>();
        String sql = "SELECT * FROM NOTIFICATIONS WHERE user_id=? ORDER BY created_at DESC";
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

    public long countUnread(Long userId) {
        String sql = "SELECT COUNT(*) FROM NOTIFICATIONS WHERE user_id=? AND is_read=0";
        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next())
                return rs.getLong(1);
        } catch (SQLException e) {
            log.error("Error countUnread: {}", e.getMessage(), e);
        }
        return 0;
    }

    public void markRead(Long notifId) {
        String sql = "UPDATE NOTIFICATIONS SET is_read=1 WHERE notif_id=?";
        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, notifId);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("Error markRead: {}", e.getMessage(), e);
        }
    }

    public void markAllRead(Long userId) {
        String sql = "UPDATE NOTIFICATIONS SET is_read=1 WHERE user_id=?";
        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("Error markAllRead: {}", e.getMessage(), e);
        }
    }

    // ---- Preferences ----

    public void savePreference(NotificationPreference pref) {
        String sql = "MERGE INTO NOTIFICATION_PREFS dest " +
                "USING (SELECT ? AS user_id, ? AS notif_type FROM DUAL) src " +
                "ON (dest.user_id = src.user_id AND dest.notif_type = src.notif_type) " +
                "WHEN MATCHED THEN UPDATE SET dest.is_enabled=? " +
                "WHEN NOT MATCHED THEN INSERT (user_id, notif_type, is_enabled) VALUES (?,?,?)";
        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, pref.getUserId());
            ps.setString(2, pref.getNotifType());
            ps.setInt(3, pref.isEnabled() ? 1 : 0);
            ps.setLong(4, pref.getUserId());
            ps.setString(5, pref.getNotifType());
            ps.setInt(6, pref.isEnabled() ? 1 : 0);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("Error savePreference: {}", e.getMessage(), e);
        }
    }

    public List<NotificationPreference> findPreferencesByUserId(Long userId) {
        List<NotificationPreference> list = new ArrayList<>();
        String sql = "SELECT * FROM NOTIFICATION_PREFS WHERE user_id=?";
        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                NotificationPreference p = new NotificationPreference();
                p.setPrefId(rs.getLong("pref_id"));
                p.setUserId(rs.getLong("user_id"));
                p.setNotifType(rs.getString("notif_type"));
                p.setEnabled(rs.getInt("is_enabled") == 1);
                list.add(p);
            }
        } catch (SQLException e) {
            log.error("Error findPreferencesByUserId: {}", e.getMessage(), e);
        }
        return list;
    }

    public boolean isPreferenceEnabled(Long userId, String notifType) {
        String sql = "SELECT is_enabled FROM NOTIFICATION_PREFS WHERE user_id=? AND notif_type=?";
        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setString(2, notifType);
            ResultSet rs = ps.executeQuery();
            if (rs.next())
                return rs.getInt(1) == 1;
        } catch (SQLException e) {
            log.error("Error isPreferenceEnabled: {}", e.getMessage(), e);
        }
        return true; // Default: enabled
    }
}
