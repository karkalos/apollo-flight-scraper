package com.example.template.service;

import com.example.template.entity.SearchConfig;
import com.example.template.repository.SearchConfigRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@ConditionalOnProperty(name = "apollo.scraper.scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class ScheduledScrapeService {

    private static final Logger log = LoggerFactory.getLogger(ScheduledScrapeService.class);

    private final SearchConfigRepository searchConfigRepository;
    private final FlightSearchService flightSearchService;

    public ScheduledScrapeService(SearchConfigRepository searchConfigRepository,
                                  FlightSearchService flightSearchService) {
        this.searchConfigRepository = searchConfigRepository;
        this.flightSearchService = flightSearchService;
    }

    @Scheduled(fixedDelayString = "${apollo.scraper.scheduling.check-interval-ms:300000}")
    public void checkAndScrape() {
        List<SearchConfig> enabledConfigs = searchConfigRepository.findByEnabledTrue();

        for (SearchConfig config : enabledConfigs) {
            if (isDueForScrape(config)) {
                log.info("Triggering scheduled scrape for config: {} (id={})", config.getName(), config.getId());
                try {
                    flightSearchService.executeScrape(config);
                } catch (Exception e) {
                    log.error("Scheduled scrape failed for config {}: {}", config.getId(), e.getMessage());
                }
            }
        }
    }

    private boolean isDueForScrape(SearchConfig config) {
        if (config.getLastScrapedAt() == null) {
            return true;
        }
        OffsetDateTime nextScrapeTime = config.getLastScrapedAt()
                .plusMinutes(config.getPollIntervalMinutes());
        return OffsetDateTime.now().isAfter(nextScrapeTime);
    }
}
