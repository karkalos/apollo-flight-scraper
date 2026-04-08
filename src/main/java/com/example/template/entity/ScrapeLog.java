package com.example.template.entity;

import com.example.template.enums.ScrapeStatus;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "scrape_log")
@JsonIgnoreProperties({"config", "hibernateLazyInitializer"})
public class ScrapeLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "config_id")
    private SearchConfig config;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ScrapeStatus status;

    @Column(name = "flights_found")
    private Integer flightsFound = 0;

    @Column(name = "new_flights_saved")
    private Integer newFlightsSaved = 0;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "started_at")
    private OffsetDateTime startedAt;

    @Column(name = "finished_at")
    private OffsetDateTime finishedAt;

    @PrePersist
    protected void onCreate() {
        startedAt = OffsetDateTime.now();
    }

    // Getters and setters

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public SearchConfig getConfig() { return config; }
    public void setConfig(SearchConfig config) { this.config = config; }

    public ScrapeStatus getStatus() { return status; }
    public void setStatus(ScrapeStatus status) { this.status = status; }

    public Integer getFlightsFound() { return flightsFound; }
    public void setFlightsFound(Integer flightsFound) { this.flightsFound = flightsFound; }

    public Integer getNewFlightsSaved() { return newFlightsSaved; }
    public void setNewFlightsSaved(Integer newFlightsSaved) { this.newFlightsSaved = newFlightsSaved; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public OffsetDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(OffsetDateTime startedAt) { this.startedAt = startedAt; }

    public OffsetDateTime getFinishedAt() { return finishedAt; }
    public void setFinishedAt(OffsetDateTime finishedAt) { this.finishedAt = finishedAt; }
}
