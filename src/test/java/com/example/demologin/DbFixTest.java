package com.example.demologin;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
public class DbFixTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    public void fixKitchenIdColumn() {
        System.out.println("Executing manual database migration to drop kitchen_id NOT NULL constraint...");
        jdbcTemplate.execute("ALTER TABLE orders MODIFY COLUMN kitchen_id VARCHAR(10) NULL");
        System.out.println("✅ SUCCESSFULLY ALTERED orders TABLE!");
    }
}
