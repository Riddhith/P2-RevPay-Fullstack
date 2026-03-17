package com.revature.revpay.service;

import com.revature.revpay.dao.BusinessProfileDAO;
import com.revature.revpay.dao.InvoiceDAO;
import com.revature.revpay.dao.LoanApplicationDAO;
import com.revature.revpay.dao.TransactionDAO;
import com.revature.revpay.dao.UserDAO;
import com.revature.revpay.model.BusinessProfile;
import com.revature.revpay.model.User;
import com.revature.revpay.model.Invoice;
import com.revature.revpay.model.InvoiceItem;
import com.revature.revpay.model.LoanApplication;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class BusinessService {

    private static final Logger log = LogManager.getLogger(BusinessService.class);

    private final InvoiceDAO invoiceDAO;
    private final LoanApplicationDAO loanApplicationDAO;
    private final TransactionDAO transactionDAO;
    private final NotificationService notificationService;
    private final BusinessProfileDAO businessProfileDAO;
    private final UserDAO userDAO;

    public BusinessService(InvoiceDAO invoiceDAO, LoanApplicationDAO loanApplicationDAO,
            TransactionDAO transactionDAO, NotificationService notificationService, 
            BusinessProfileDAO businessProfileDAO, UserDAO userDAO) {
        this.invoiceDAO = invoiceDAO;
        this.loanApplicationDAO = loanApplicationDAO;
        this.transactionDAO = transactionDAO;
        this.notificationService = notificationService;
        this.businessProfileDAO = businessProfileDAO;
        this.userDAO = userDAO;
    }

    // ---- Invoices ----

    public Long createInvoice(Invoice invoice) {
        BusinessProfile bp = businessProfileDAO.findByUserId(invoice.getBusinessUserId());
        if (bp == null || !"APPROVED".equals(bp.getStatus())) {
            throw new IllegalStateException("Business account is pending approval or rejected. Cannot create invoice.");
        }

        // Compute totals from items
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal taxTotal = BigDecimal.ZERO;
        if (invoice.getItems() != null) {
            for (InvoiceItem item : invoice.getItems()) {
                BigDecimal base = item.getQuantity().multiply(item.getUnitPrice());
                BigDecimal tax = item.getTaxRate() != null
                        ? base.multiply(item.getTaxRate()).divide(BigDecimal.valueOf(100))
                        : BigDecimal.ZERO;
                subtotal = subtotal.add(base);
                taxTotal = taxTotal.add(tax);
            }
        }
        invoice.setSubtotal(subtotal);
        invoice.setTaxTotal(taxTotal);
        invoice.setTotalAmount(subtotal.add(taxTotal));
        Long id = invoiceDAO.save(invoice);
        
        // Check if customer email matches a personal user and notify them
        if (invoice.getCustomerEmail() != null && !invoice.getCustomerEmail().isBlank()) {
            User customer = userDAO.findByEmailOrPhone(invoice.getCustomerEmail());
            if (customer != null && "PERSONAL".equals(customer.getAccountType())) {
                notificationService.sendInvoiceNotification(customer.getUserId(), 
                    "New Invoice Received", 
                    "You have received a new draft invoice from " + bp.getBusinessName() + " for ₹" + invoice.getTotalAmount() + ".");
            }
        }
        return id;
    }

    public List<Invoice> getInvoices(Long userId) {
        return invoiceDAO.findByUserId(userId);
    }

    public Invoice getInvoice(Long invoiceId) {
        return invoiceDAO.findById(invoiceId);
    }

    public void markInvoicePaid(Long invoiceId, Long userId) {
        log.info("Attempting to mark invoice {} as paid. Requested for user: {}", invoiceId, userId);
        Invoice inv = invoiceDAO.findById(invoiceId);
        if (inv != null) {
            log.info("Invoice {} found. Its business_user_id is: {}. Passed user_id is: {}", invoiceId, inv.getBusinessUserId(), userId);
            if (inv.getBusinessUserId().equals(userId)) {
                invoiceDAO.updateStatus(invoiceId, "PAID");
                log.info("Invoice {} status successfully updated to PAID in DAO.", invoiceId);
                notificationService.sendInvoiceNotification(userId,
                        "Invoice Paid", "Invoice " + inv.getInvoiceNumber() + " has been marked as paid.");
            } else {
                log.warn("MISMATH: Invoice {} belongs to business_user_id {} but user {} attempted to mark it paid.", invoiceId, inv.getBusinessUserId(), userId);
                throw new SecurityException("Unauthorized: This invoice does not belong to the calling business user.");
            }
        } else {
            log.warn("Invoice {} not found in database.", invoiceId);
            throw new IllegalArgumentException("Invoice not found.");
        }
    }

    public void sendInvoice(Long invoiceId, Long userId) {
        invoiceDAO.updateStatus(invoiceId, "SENT");
        Invoice invoice = invoiceDAO.findById(invoiceId);
        if (invoice != null && invoice.getCustomerEmail() != null && !invoice.getCustomerEmail().isBlank()) {
            User customer = userDAO.findByEmailOrPhone(invoice.getCustomerEmail());
            if (customer != null && "PERSONAL".equals(customer.getAccountType())) {
                BusinessProfile bp = businessProfileDAO.findByUserId(invoice.getBusinessUserId());
                notificationService.sendInvoiceNotification(customer.getUserId(), 
                    "Invoice Sent", 
                    bp.getBusinessName() + " has sent you invoice #" + invoiceId + " for ₹" + invoice.getTotalAmount() + ".");
            }
        }
    }

    public void cancelInvoice(Long invoiceId, Long userId) {
        invoiceDAO.updateStatus(invoiceId, "CANCELLED");
    }

    public void deleteInvoice(Long invoiceId) {
        invoiceDAO.delete(invoiceId);
    }

    // ---- Loans ----

    public Long applyForLoan(LoanApplication loan) {
        Long id = loanApplicationDAO.save(loan);
        if (id != null) {
            notificationService.sendLoanNotification(loan.getUserId(),
                    "Loan Application Submitted",
                    "Your loan application for ₹" + loan.getLoanAmount() + " is under review.");
        }
        return id;
    }

    public List<LoanApplication> getLoans(Long userId) {
        return loanApplicationDAO.findByUserId(userId);
    }

    public LoanApplication getLoan(Long loanId) {
        return loanApplicationDAO.findById(loanId);
    }

    public void makeRepayment(Long loanId, Long userId, BigDecimal amount) {
        LoanApplication loan = loanApplicationDAO.findById(loanId);
        if (loan != null && loan.getUserId().equals(userId)) {
            User user = userDAO.findById(userId);
            if (user.getWalletBalance().compareTo(amount) < 0) {
                throw new IllegalStateException("Insufficient wallet balance for this repayment.");
            }
            // Deduct from wallet
            userDAO.updateBalance(userId, user.getWalletBalance().subtract(amount));
            
            // Add repayment to loan
            loanApplicationDAO.addRepayment(loanId, amount);
            notificationService.sendLoanNotification(userId,
                    "Loan Repayment", "Repayment of ₹" + amount + " recorded for loan #" + loanId);
            
            // Notify admins of the repayment
            List<User> admins = userDAO.findByRole("ADMIN");
            for (User admin : admins) {
                notificationService.send(admin.getUserId(), "Loan Repayment Received", 
                    "User " + user.getFullName() + " has repaid ₹" + amount + " towards loan #" + loanId + ".", "SYSTEM");
            }

            // Check if fully repaid
            BigDecimal paid = loan.getAmountPaid().add(amount);
            if (paid.compareTo(loan.getLoanAmount()) >= 0) {
                loanApplicationDAO.updateStatus(loanId, "CLOSED");
            }
        }
    }

    // ---- Analytics ----

    public BigDecimal getTotalReceived(Long userId) {
        return transactionDAO.getTotalReceived(userId);
    }

    public BigDecimal getTotalSent(Long userId) {
        return transactionDAO.getTotalByType(userId, "SEND");
    }

    public BigDecimal getPaidInvoicesTotal(Long userId) {
        return invoiceDAO.sumByStatus(userId, "PAID");
    }

    public BigDecimal getOutstandingInvoicesTotal(Long userId) {
        return invoiceDAO.sumByStatus(userId, "SENT");
    }

    public List<java.util.Map<String, Object>> getTopCustomersByVolume(Long userId) {
        return invoiceDAO.getTopCustomersByVolume(userId);
    }
}
