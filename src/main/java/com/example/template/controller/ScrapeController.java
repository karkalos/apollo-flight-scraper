package com.example.template.controller;

import com.example.template.entity.ScrapeLog;
import com.example.template.entity.SearchConfig;
import com.example.template.repository.ScrapeLogRepository;
import com.example.template.service.FlightSearchService;
import com.example.template.service.SearchConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/scrape")
@Tag(name = "Scrape Control", description = "Trigger and monitor scraping runs")
public class ScrapeController {

    private final FlightSearchService flightSearchService;
    private final SearchConfigService searchConfigService;
    private final ScrapeLogRepository scrapeLogRepository;

    public ScrapeController(FlightSearchService flightSearchService,
                            SearchConfigService searchConfigService,
                            ScrapeLogRepository scrapeLogRepository) {
        this.flightSearchService = flightSearchService;
        this.searchConfigService = searchConfigService;
        this.scrapeLogRepository = scrapeLogRepository;
    }

    @PostMapping("/trigger")
    @Operation(summary = "Trigger a manual scrape (async, returns 202)")
    public ResponseEntity<String> triggerScrape(
            @RequestParam(defaultValue = "1") Long configId) {
        SearchConfig config = searchConfigService.findById(configId);

        CompletableFuture.runAsync(() -> flightSearchService.executeScrape(config));

        return ResponseEntity.accepted()
                .body("Scrape triggered for config: " + config.getName() + " (id=" + configId + ")");
    }

    @GetMapping("/status")
    @Operation(summary = "Get recent scrape execution logs")
    public List<ScrapeLog> getStatus(@RequestParam(defaultValue = "10") int limit) {
        return scrapeLogRepository.findAllByOrderByStartedAtDesc(PageRequest.of(0, limit));
    }
}
