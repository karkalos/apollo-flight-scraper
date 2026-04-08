package com.example.template.entity;

import com.example.template.enums.TripType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "flight_result")
@JsonIgnoreProperties({"config", "hibernateLazyInitializer"})
public class FlightResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "config_id")
    private SearchConfig config;

    private Integer price;

    @Column(length = 10)
    private String currency = "SEK";

    @Column(name = "departure_date")
    private LocalDate departureDate;

    @Column(name = "return_date")
    private LocalDate returnDate;

    @Column(name = "departure_time", length = 10)
    private String departureTime;

    @Column(name = "arrival_time", length = 10)
    private String arrivalTime;

    @Column(name = "return_departure_time", length = 10)
    private String returnDepartureTime;

    @Column(name = "return_arrival_time", length = 10)
    private String returnArrivalTime;

    private String airline;

    @Enumerated(EnumType.STRING)
    @Column(name = "trip_type", length = 20)
    private TripType tripType;

    @Column(name = "booking_url", columnDefinition = "TEXT")
    private String bookingUrl;

    @Column(nullable = false, length = 64)
    private String checksum;

    @Column(name = "scraped_at")
    private OffsetDateTime scrapedAt;

    @PrePersist
    protected void onCreate() {
        scrapedAt = OffsetDateTime.now();
    }

    // Getters and setters

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public SearchConfig getConfig() { return config; }
    public void setConfig(SearchConfig config) { this.config = config; }

    public Integer getPrice() { return price; }
    public void setPrice(Integer price) { this.price = price; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public LocalDate getDepartureDate() { return departureDate; }
    public void setDepartureDate(LocalDate departureDate) { this.departureDate = departureDate; }

    public LocalDate getReturnDate() { return returnDate; }
    public void setReturnDate(LocalDate returnDate) { this.returnDate = returnDate; }

    public String getDepartureTime() { return departureTime; }
    public void setDepartureTime(String departureTime) { this.departureTime = departureTime; }

    public String getArrivalTime() { return arrivalTime; }
    public void setArrivalTime(String arrivalTime) { this.arrivalTime = arrivalTime; }

    public String getReturnDepartureTime() { return returnDepartureTime; }
    public void setReturnDepartureTime(String returnDepartureTime) { this.returnDepartureTime = returnDepartureTime; }

    public String getReturnArrivalTime() { return returnArrivalTime; }
    public void setReturnArrivalTime(String returnArrivalTime) { this.returnArrivalTime = returnArrivalTime; }

    public String getAirline() { return airline; }
    public void setAirline(String airline) { this.airline = airline; }

    public TripType getTripType() { return tripType; }
    public void setTripType(TripType tripType) { this.tripType = tripType; }

    public String getBookingUrl() { return bookingUrl; }
    public void setBookingUrl(String bookingUrl) { this.bookingUrl = bookingUrl; }

    public String getChecksum() { return checksum; }
    public void setChecksum(String checksum) { this.checksum = checksum; }

    public OffsetDateTime getScrapedAt() { return scrapedAt; }
    public void setScrapedAt(OffsetDateTime scrapedAt) { this.scrapedAt = scrapedAt; }
}
