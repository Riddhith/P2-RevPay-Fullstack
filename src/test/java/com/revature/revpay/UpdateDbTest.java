package com.revature.revpay;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
public class UpdateDbTest {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    public void updateAdminPassword() {
        String sql = "UPDATE USERS SET password_hash = '$2a$10$XjJH0pb3lkkFR9VDjNx0NucZTwzLfVp/UZKhP8IcX8FFWe02ovUnK' WHERE username = 'admin'";
        jdbcTemplate.update(sql);
        System.out.println("ADMIN PASSWORD UPDATED!");
    }
}
