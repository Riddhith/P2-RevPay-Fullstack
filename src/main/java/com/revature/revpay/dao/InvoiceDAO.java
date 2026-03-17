package com.revature.revpay.dao;

import com.revature.revpay.model.Invoice;
import com.revature.revpay.model.InvoiceItem;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class InvoiceDAO {

    private static final Logger log = LogManager.getLogger(InvoiceDAO.class);
    private final DataSource dataSource;

    public InvoiceDAO(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    private Invoice mapRow(ResultSet rs) throws SQLException {
        Invoice inv = new Invoice();
        inv.setInvoiceId(rs.getLong("invoice_id"));
        inv.setBusinessUserId(rs.getLong("business_user_id"));
        inv.setInvoiceNumber(rs.getString("invoice_number"));
        inv.setCustomerName(rs.getString("customer_name"));
        inv.setCustomerEmail(rs.getString("customer_email"));
        inv.setCustomerAddress(rs.getString("customer_address"));
        inv.setSubtotal(rs.getBigDecimal("subtotal"));
        inv.setTaxTotal(rs.getBigDecimal("tax_total"));
        inv.setTotalAmount(rs.getBigDecimal("total_amount"));
        inv.setStatus(rs.getString("status"));
        inv.setPaymentTerms(rs.getString("payment_terms"));
        inv.setNotes(rs.getString("notes"));
        Date dd = rs.getDate("due_date");
        if (dd != null)
            inv.setDueDate(dd.toLocalDate());
        Timestamp ca = rs.getTimestamp("created_at");
        if (ca != null)
            inv.setCreatedAt(ca.toLocalDateTime());
        Timestamp ua = rs.getTimestamp("updated_at");
        if (ua != null)
            inv.setUpdatedAt(ua.toLocalDateTime());
        return inv;
    }

    public Long save(Invoice inv) {
        String sql = "INSERT INTO INVOICES (business_user_id, customer_name, customer_email, customer_address, " +
                "subtotal, tax_total, total_amount, status, payment_terms, due_date, notes) " +
                "VALUES (?,?,?,?,?,?,?,?,?,?,?)";
        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql, new String[] { "INVOICE_ID" })) {
            ps.setLong(1, inv.getBusinessUserId());
            ps.setString(2, inv.getCustomerName());
            ps.setString(3, inv.getCustomerEmail());
            ps.setString(4, inv.getCustomerAddress());
            ps.setBigDecimal(5, inv.getSubtotal() != null ? inv.getSubtotal() : BigDecimal.ZERO);
            ps.setBigDecimal(6, inv.getTaxTotal() != null ? inv.getTaxTotal() : BigDecimal.ZERO);
            ps.setBigDecimal(7, inv.getTotalAmount() != null ? inv.getTotalAmount() : BigDecimal.ZERO);
            ps.setString(8, inv.getStatus() != null ? inv.getStatus() : "DRAFT");
            ps.setString(9, inv.getPaymentTerms());
            ps.setDate(10, inv.getDueDate() != null ? Date.valueOf(inv.getDueDate()) : null);
            ps.setString(11, inv.getNotes());
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                long id = rs.getLong(1);
                inv.setInvoiceId(id);
                if (inv.getItems() != null) {
                    for (InvoiceItem item : inv.getItems()) {
                        item.setInvoiceId(id);
                        saveItem(item, conn);
                    }
                }
                return id;
            }
        } catch (SQLException e) {
            log.error("Error save invoice: {}", e.getMessage(), e);
        }
        return null;
    }

    private void saveItem(InvoiceItem item, Connection conn) throws SQLException {
        String sql = "INSERT INTO INVOICE_ITEMS (invoice_id, description, quantity, unit_price, tax_rate, line_total) "
                +
                "VALUES (?,?,?,?,?,?)";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setLong(1, item.getInvoiceId());
        ps.setString(2, item.getDescription());
        ps.setBigDecimal(3, item.getQuantity());
        ps.setBigDecimal(4, item.getUnitPrice());
        ps.setBigDecimal(5, item.getTaxRate() != null ? item.getTaxRate() : BigDecimal.ZERO);
        ps.setBigDecimal(6, item.computeLineTotal());
        ps.executeUpdate();
    }

    public Invoice findById(Long invoiceId) {
        String sql = "SELECT * FROM INVOICES WHERE invoice_id=?";
        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, invoiceId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Invoice inv = mapRow(rs);
                inv.setItems(findItems(invoiceId, conn));
                return inv;
            }
        } catch (SQLException e) {
            log.error("Error findById: {}", e.getMessage(), e);
        }
        return null;
    }

    private List<InvoiceItem> findItems(Long invoiceId, Connection conn) throws SQLException {
        List<InvoiceItem> items = new ArrayList<>();
        PreparedStatement ps = conn.prepareStatement("SELECT * FROM INVOICE_ITEMS WHERE invoice_id=?");
        ps.setLong(1, invoiceId);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            InvoiceItem it = new InvoiceItem();
            it.setItemId(rs.getLong("item_id"));
            it.setInvoiceId(rs.getLong("invoice_id"));
            it.setDescription(rs.getString("description"));
            it.setQuantity(rs.getBigDecimal("quantity"));
            it.setUnitPrice(rs.getBigDecimal("unit_price"));
            it.setTaxRate(rs.getBigDecimal("tax_rate"));
            it.setLineTotal(rs.getBigDecimal("line_total"));
            items.add(it);
        }
        return items;
    }

    public List<Invoice> findByUserId(Long userId) {
        List<Invoice> list = new ArrayList<>();
        String sql = "SELECT * FROM INVOICES WHERE business_user_id=? ORDER BY created_at DESC";
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

    public List<Invoice> findByCustomerEmail(String email) {
        List<Invoice> list = new ArrayList<>();
        String sql = "SELECT * FROM INVOICES WHERE customer_email=? ORDER BY created_at DESC";
        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            while (rs.next())
                list.add(mapRow(rs));
        } catch (SQLException e) {
            log.error("Error findByCustomerEmail: {}", e.getMessage(), e);
        }
        return list;
    }

    public void updateStatus(Long invoiceId, String status) {
        String sql = "UPDATE INVOICES SET status=?, updated_at=CURRENT_TIMESTAMP WHERE invoice_id=?";
        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setLong(2, invoiceId);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("Error updateStatus: {}", e.getMessage(), e);
        }
    }

    public void delete(Long invoiceId) {
        String sql = "DELETE FROM INVOICES WHERE invoice_id=?";
        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, invoiceId);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("Error delete: {}", e.getMessage(), e);
        }
    }

    public long countByStatus(Long userId, String status) {
        String sql = "SELECT COUNT(*) FROM INVOICES WHERE business_user_id=? AND status=?";
        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setString(2, status);
            ResultSet rs = ps.executeQuery();
            if (rs.next())
                return rs.getLong(1);
        } catch (SQLException e) {
            log.error("Error countByStatus: {}", e.getMessage(), e);
        }
        return 0;
    }

    public BigDecimal sumByStatus(Long userId, String status) {
        String sql = "SELECT NVL(SUM(total_amount),0) FROM INVOICES WHERE business_user_id=? AND status=?";
        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setString(2, status);
            ResultSet rs = ps.executeQuery();
            if (rs.next())
                return rs.getBigDecimal(1);
        } catch (SQLException e) {
            log.error("Error sumByStatus: {}", e.getMessage(), e);
        }
        return BigDecimal.ZERO;
    }

    public List<Map<String, Object>> getTopCustomersByVolume(Long businessUserId) {
        List<Map<String, Object>> topCustomers = new ArrayList<>();
        String sql = "SELECT customer_name, SUM(total_amount) as volume " +
                     "FROM INVOICES " +
                     "WHERE business_user_id=? AND status='PAID' " +
                     "GROUP BY customer_name " +
                     "ORDER BY volume DESC " +
                     "FETCH FIRST 5 ROWS ONLY";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, businessUserId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Map<String, Object> map = new HashMap<>();
                map.put("customerName", rs.getString("customer_name"));
                map.put("volume", rs.getBigDecimal("volume"));
                topCustomers.add(map);
            }
        } catch (SQLException e) {
            log.error("Error getTopCustomersByVolume: {}", e.getMessage(), e);
        }
        return topCustomers;
    }

    public long countPendingForCustomer(String email) {
        String sql = "SELECT COUNT(*) FROM INVOICES WHERE customer_email=? AND status='SENT'";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getLong(1);
        } catch (SQLException e) {
            log.error("Error countPendingForCustomer: {}", e.getMessage(), e);
        }
        return 0;
    }
}
