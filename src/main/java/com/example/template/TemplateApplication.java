package com.example.template;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@EnableScheduling
public class TemplateApplication {

    @RestController
    class RootController {
        @GetMapping("/")
        String redirect() {
            return "Apollo Flight Scraper is running. Visit <a href='/swagger-ui.html'>Swagger UI</a> for API docs.";
        }
    }

    public static void main(String[] args) {
        SpringApplication.run(TemplateApplication.class, args);
    }
}
