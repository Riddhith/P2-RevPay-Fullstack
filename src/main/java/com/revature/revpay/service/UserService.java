package com.revature.revpay.service;

import com.revature.revpay.dao.BusinessProfileDAO;
import com.revature.revpay.dao.NotificationDAO;
import com.revature.revpay.dao.UserDAO;
import com.revature.revpay.model.BusinessProfile;
import com.revature.revpay.model.NotificationPreference;
import com.revature.revpay.model.User;
import com.revature.revpay.util.PasswordUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

@Service
public class UserService {

    private static final Logger log = LogManager.getLogger(UserService.class);

    private final UserDAO userDAO;
    private final BusinessProfileDAO businessProfileDAO;
    private final NotificationDAO notificationDAO;

    public UserService(UserDAO userDAO, BusinessProfileDAO businessProfileDAO, NotificationDAO notificationDAO) {
        this.userDAO = userDAO;
        this.businessProfileDAO = businessProfileDAO;
        this.notificationDAO = notificationDAO;
    }

    public User findByEmailOrPhone(String emailOrPhone) {
        return userDAO.findByEmailOrPhone(emailOrPhone);
    }

    public User findById(Long userId) {
        return userDAO.findById(userId);
    }

    public User findByUsernameOrEmailOrPhone(String input) {
        return userDAO.findByUsernameOrEmailOrPhone(input);
    }

    public List<User> findByRole(String role) {
        return userDAO.findByRole(role);
    }

    /**
     * Register a personal account user
     */
    public Long registerPersonal(String fullName, String email, String phone, String username, String password) {
        if (userDAO.emailExists(email))
            throw new RuntimeException("Email already registered");
        if (userDAO.usernameExists(username))
            throw new RuntimeException("Username already taken");

        User user = new User();
        user.setFullName(fullName);
        user.setEmail(email);
        user.setPhone(phone);
        user.setUsername(username);
        user.setPasswordHash(PasswordUtil.hash(password));
        user.setAccountType("PERSONAL");
        user.setWalletBalance(BigDecimal.ZERO);

        Long userId = userDAO.save(user);
        if (userId != null) {
            initDefaultNotificationPrefs(userId);
            log.info("Personal user registered: {}", email);
        }
        return userId;
    }

    /**
     * Register a business account user + business profile
     */
    public Long registerBusiness(String fullName, String email, String phone, String username, String password,
            String businessName, String businessType, String taxId, String address) {
        if (userDAO.emailExists(email))
            throw new RuntimeException("Email already registered");
        if (userDAO.usernameExists(username))
            throw new RuntimeException("Username already taken");

        User user = new User();
        user.setFullName(fullName);
        user.setEmail(email);
        user.setPhone(phone);
        user.setUsername(username);
        user.setPasswordHash(PasswordUtil.hash(password));
        user.setAccountType("BUSINESS");
        user.setWalletBalance(BigDecimal.ZERO);

        Long userId = userDAO.save(user);
        if (userId != null) {
            BusinessProfile bp = new BusinessProfile();
            bp.setUserId(userId);
            bp.setBusinessName(businessName);
            bp.setBusinessType(businessType);
            bp.setTaxId(taxId);
            bp.setAddress(address);
            businessProfileDAO.save(bp);
            initDefaultNotificationPrefs(userId);
            
            // Notify admins of new business registration
            List<User> admins = userDAO.findByRole("ADMIN");
            for (User admin : admins) {
                com.revature.revpay.model.Notification notif = new com.revature.revpay.model.Notification(
                    admin.getUserId(), 
                    "New Business Registration", 
                    "User " + fullName + " has registered a new business. Review in the Admin Dashboard for GST Validation.", 
                    "SYSTEM"
                );
                notificationDAO.save(notif);
            }
            
            log.info("Business user registered: {}", email);
        }
        return userId;
    }

    private void initDefaultNotificationPrefs(Long userId) {
        List<String> types = Arrays.asList("TRANSACTION", "MONEY_REQUEST", "CARD_CHANGE", "LOW_BALANCE", "LOAN",
                "INVOICE");
        for (String type : types) {
            notificationDAO.savePreference(new NotificationPreference(userId, type, true));
        }
    }

    public boolean changePassword(Long userId, String currentPassword, String newPassword) {
        User user = userDAO.findById(userId);
        if (user == null || !PasswordUtil.matches(currentPassword, user.getPasswordHash())) {
            return false;
        }
        userDAO.updatePassword(userId, PasswordUtil.hash(newPassword));
        log.info("Password changed for user: {}", userId);
        return true;
    }

    public void updateProfile(Long userId, String fullName, String phone) {
        User user = userDAO.findById(userId);
        if (user != null) {
            user.setFullName(fullName);
            user.setPhone(phone);
            userDAO.updateProfile(user);
        }
    }

    public BusinessProfile getBusinessProfile(Long userId) {
        return businessProfileDAO.findByUserId(userId);
    }

    public void updateBusinessProfile(Long userId, String businessName, String businessType, String taxId,
            String gstNumber, String address) {
        BusinessProfile bp = businessProfileDAO.findByUserId(userId);
        if (bp == null) {
            bp = new BusinessProfile();
            bp.setUserId(userId);
            bp.setStatus("PENDING");
        }
        bp.setBusinessName(businessName);
        bp.setBusinessType(businessType);
        bp.setTaxId(taxId);
        bp.setGstNumber(gstNumber);
        bp.setAddress(address);
        if (bp.getProfileId() == null) {
            businessProfileDAO.save(bp);
        } else {
            businessProfileDAO.update(bp);
        }
    }
}
