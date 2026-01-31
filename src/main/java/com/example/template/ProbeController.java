package com.example.template;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProbeController {

    private final JdbcTemplate jdbcTemplate;

    public ProbeController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/db-check")
    public String check() {
        try {
            return "Database Time: " + jdbcTemplate.queryForObject("SELECT NOW()", String.class);
        } catch (Exception e) {
            return "Database Connection Failed: " + e.getMessage();
        }
    }
}
