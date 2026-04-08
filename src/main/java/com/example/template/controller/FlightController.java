package com.example.template.controller;

import com.example.template.entity.FlightResult;
import com.example.template.enums.TripType;
import com.example.template.repository.FlightResultRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/flights")
@Tag(name = "Flights", description = "Flight search results")
public class FlightController {

    private final FlightResultRepository flightResultRepository;

    public FlightController(FlightResultRepository flightResultRepository) {
        this.flightResultRepository = flightResultRepository;
    }

    @GetMapping
    @Operation(summary = "Search flights with filters and pagination")
    public Page<FlightResult> getFlights(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) Integer minPrice,
            @RequestParam(required = false) Integer maxPrice,
            @RequestParam(required = false) TripType tripType,
            @RequestParam(defaultValue = "price") String sortBy,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Sort sort = Sort.by(Sort.Direction.ASC, sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        return flightResultRepository.findFiltered(fromDate, toDate, minPrice, maxPrice, tripType, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a single flight result")
    public ResponseEntity<FlightResult> getFlightById(@PathVariable Long id) {
        return flightResultRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/cheapest")
    @Operation(summary = "Get cheapest flights per departure date")
    public List<FlightResult> getCheapest(
            @RequestParam(required = false) TripType tripType,
            @RequestParam(defaultValue = "10") int limit) {
        return flightResultRepository.findCheapest(tripType, PageRequest.of(0, limit));
    }

    @DeleteMapping("/old")
    @Transactional
    @Operation(summary = "Delete flight results older than specified days")
    public ResponseEntity<String> deleteOldFlights(
            @RequestParam(defaultValue = "30") int olderThanDays) {
        OffsetDateTime cutoff = OffsetDateTime.now().minusDays(olderThanDays);
        int deleted = flightResultRepository.deleteOlderThan(cutoff);
        return ResponseEntity.ok("Deleted " + deleted + " old flight results");
    }
}
