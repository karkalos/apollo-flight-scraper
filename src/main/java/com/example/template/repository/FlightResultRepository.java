package com.example.template.repository;

import com.example.template.entity.FlightResult;
import com.example.template.enums.TripType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public interface FlightResultRepository extends JpaRepository<FlightResult, Long> {

    boolean existsByChecksum(String checksum);

    @Query("SELECT f FROM FlightResult f WHERE " +
           "(:fromDate IS NULL OR f.departureDate >= :fromDate) AND " +
           "(:toDate IS NULL OR f.departureDate <= :toDate) AND " +
           "(:minPrice IS NULL OR f.price >= :minPrice) AND " +
           "(:maxPrice IS NULL OR f.price <= :maxPrice) AND " +
           "(:tripType IS NULL OR f.tripType = :tripType)")
    Page<FlightResult> findFiltered(
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("minPrice") Integer minPrice,
            @Param("maxPrice") Integer maxPrice,
            @Param("tripType") TripType tripType,
            Pageable pageable);

    @Query("SELECT f FROM FlightResult f WHERE " +
           "(:tripType IS NULL OR f.tripType = :tripType) " +
           "ORDER BY f.price ASC")
    List<FlightResult> findCheapest(@Param("tripType") TripType tripType, Pageable pageable);

    @Modifying
    @Query("DELETE FROM FlightResult f WHERE f.scrapedAt < :cutoff")
    int deleteOlderThan(@Param("cutoff") OffsetDateTime cutoff);
}
