package com.example.template;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/messages")
public class MessageController {

    private final JdbcTemplate jdbcTemplate;

    public MessageController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostMapping
    public String saveMessage(@RequestBody String content) {
        jdbcTemplate.update("INSERT INTO messages (content, created_at) VALUES (?, ?)",
                content, OffsetDateTime.now(ZoneOffset.UTC));
        return "Message saved: " + content;
    }

    @GetMapping
    public List<Message> getMessages() {
        return jdbcTemplate.query("SELECT id, content, created_at FROM messages",
                (rs, rowNum) -> new Message(
                        rs.getLong("id"),
                        rs.getString("content"),
                        rs.getTimestamp("created_at").toInstant().atOffset(ZoneOffset.UTC)));
    }
}
