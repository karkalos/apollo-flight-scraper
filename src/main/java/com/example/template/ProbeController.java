package com.example.template;

import jakarta.persistence.EntityManager;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProbeController {

    private final EntityManager entityManager;

    public ProbeController(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @GetMapping("/db-check")
    public String check() {
        try {
            Object result = entityManager.createNativeQuery("SELECT NOW()").getSingleResult();
            return "Database Time: " + result;
        } catch (Exception e) {
            return "Database Connection Failed: " + e.getMessage();
        }
    }
}
