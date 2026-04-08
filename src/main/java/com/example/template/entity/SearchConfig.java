package com.example.template.entity;

import com.example.template.enums.TripType;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "search_config")
public class SearchConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "origin_airport", nullable = false, length = 10)
    private String originAirport;

    @Column(name = "destination_airport", nullable = false, length = 10)
    private String destinationAirport;

    @Column(name = "search_from_date")
    private LocalDate searchFromDate;

    @Column(name = "search_to_date")
    private LocalDate searchToDate;

    @Column(name = "min_price")
    private Integer minPrice;

    @Column(name = "max_price")
    private Integer maxPrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "trip_type", nullable = false, length = 20)
    private TripType tripType = TripType.BOTH;

    @Column(name = "poll_interval_minutes", nullable = false)
    private Integer pollIntervalMinutes = 360;

    @Column(nullable = false)
    private Boolean enabled = true;

    @Column(name = "last_scraped_at")
    private OffsetDateTime lastScrapedAt;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    // Getters and setters

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getOriginAirport() { return originAirport; }
    public void setOriginAirport(String originAirport) { this.originAirport = originAirport; }

    public String getDestinationAirport() { return destinationAirport; }
    public void setDestinationAirport(String destinationAirport) { this.destinationAirport = destinationAirport; }

    public LocalDate getSearchFromDate() { return searchFromDate; }
    public void setSearchFromDate(LocalDate searchFromDate) { this.searchFromDate = searchFromDate; }

    public LocalDate getSearchToDate() { return searchToDate; }
    public void setSearchToDate(LocalDate searchToDate) { this.searchToDate = searchToDate; }

    public Integer getMinPrice() { return minPrice; }
    public void setMinPrice(Integer minPrice) { this.minPrice = minPrice; }

    public Integer getMaxPrice() { return maxPrice; }
    public void setMaxPrice(Integer maxPrice) { this.maxPrice = maxPrice; }

    public TripType getTripType() { return tripType; }
    public void setTripType(TripType tripType) { this.tripType = tripType; }

    public Integer getPollIntervalMinutes() { return pollIntervalMinutes; }
    public void setPollIntervalMinutes(Integer pollIntervalMinutes) { this.pollIntervalMinutes = pollIntervalMinutes; }

    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }

    public OffsetDateTime getLastScrapedAt() { return lastScrapedAt; }
    public void setLastScrapedAt(OffsetDateTime lastScrapedAt) { this.lastScrapedAt = lastScrapedAt; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}
