package com.example.template.service;

import com.example.template.entity.FlightResult;
import com.example.template.entity.ScrapeLog;
import com.example.template.entity.SearchConfig;
import com.example.template.enums.ScrapeStatus;
import com.example.template.enums.TripType;
import com.example.template.repository.FlightResultRepository;
import com.example.template.repository.ScrapeLogRepository;
import com.example.template.repository.SearchConfigRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

@Service
public class FlightSearchService {

    private static final Logger log = LoggerFactory.getLogger(FlightSearchService.class);

    private final ApolloScraperService scraperService;
    private final FlightResultRepository flightResultRepository;
    private final ScrapeLogRepository scrapeLogRepository;
    private final SearchConfigRepository searchConfigRepository;

    public FlightSearchService(ApolloScraperService scraperService,
                               FlightResultRepository flightResultRepository,
                               ScrapeLogRepository scrapeLogRepository,
                               SearchConfigRepository searchConfigRepository) {
        this.scraperService = scraperService;
        this.flightResultRepository = flightResultRepository;
        this.scrapeLogRepository = scrapeLogRepository;
        this.searchConfigRepository = searchConfigRepository;
    }

    @Transactional
    public ScrapeLog executeScrape(SearchConfig config) {
        ScrapeLog scrapeLog = new ScrapeLog();
        scrapeLog.setConfig(config);
        scrapeLog.setStatus(ScrapeStatus.RUNNING);
        scrapeLog = scrapeLogRepository.save(scrapeLog);

        try {
            List<Map<String, String>> rawResults = scraperService.scrape(config);
            scrapeLog.setFlightsFound(rawResults.size());

            int newFlights = 0;
            for (Map<String, String> raw : rawResults) {
                FlightResult flight = mapToFlightResult(raw, config);
                if (flight != null && !flightResultRepository.existsByChecksum(flight.getChecksum())) {
                    flightResultRepository.save(flight);
                    newFlights++;
                }
            }

            scrapeLog.setNewFlightsSaved(newFlights);
            scrapeLog.setStatus(ScrapeStatus.SUCCESS);
            scrapeLog.setFinishedAt(OffsetDateTime.now());

            config.setLastScrapedAt(OffsetDateTime.now());
            searchConfigRepository.save(config);

            log.info("Scrape completed for '{}': {} found, {} new saved",
                    config.getName(), rawResults.size(), newFlights);

        } catch (Exception e) {
            scrapeLog.setStatus(ScrapeStatus.FAILED);
            scrapeLog.setErrorMessage(e.getMessage());
            scrapeLog.setFinishedAt(OffsetDateTime.now());
            log.error("Scrape failed for '{}': {}", config.getName(), e.getMessage());
        }

        return scrapeLogRepository.save(scrapeLog);
    }

    private FlightResult mapToFlightResult(Map<String, String> raw, SearchConfig config) {
        try {
            FlightResult flight = new FlightResult();
            flight.setConfig(config);

            String priceStr = raw.get("price");
            if (priceStr != null) {
                // Remove non-numeric characters (currency symbols, spaces, etc.)
                String cleanPrice = priceStr.replaceAll("[^0-9]", "");
                if (!cleanPrice.isEmpty()) {
                    flight.setPrice(Integer.parseInt(cleanPrice));
                }
            }

            flight.setDepartureDate(parseDate(raw.get("departureDate")));
            flight.setReturnDate(parseDate(raw.get("returnDate")));
            flight.setDepartureTime(raw.get("departureTime"));
            flight.setArrivalTime(raw.get("arrivalTime"));
            flight.setAirline(raw.get("airline"));
            flight.setBookingUrl(raw.get("bookingUrl"));

            String tripTypeStr = raw.get("tripType");
            if (tripTypeStr != null) {
                flight.setTripType(TripType.valueOf(tripTypeStr));
            }

            // Compute checksum for deduplication
            String checksumInput = String.join("|",
                    nullSafe(flight.getPrice()),
                    nullSafe(flight.getDepartureDate()),
                    nullSafe(flight.getReturnDate()),
                    nullSafe(flight.getAirline()),
                    nullSafe(flight.getTripType()));
            flight.setChecksum(sha256(checksumInput));

            return flight;
        } catch (Exception e) {
            log.warn("Failed to map flight result: {}", e.getMessage());
            return null;
        }
    }

    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) return null;
        try {
            return LocalDate.parse(dateStr.trim());
        } catch (Exception e) {
            // Try common Swedish date formats
            try {
                // Handle "2024-06-15" or "15 jun" etc.
                return LocalDate.parse(dateStr.trim().substring(0, 10));
            } catch (Exception ex) {
                log.debug("Could not parse date: {}", dateStr);
                return null;
            }
        }
    }

    private String nullSafe(Object obj) {
        return obj != null ? obj.toString() : "";
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
