package com.example.template.repository;

import com.example.template.entity.ScrapeLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ScrapeLogRepository extends JpaRepository<ScrapeLog, Long> {

    List<ScrapeLog> findAllByOrderByStartedAtDesc(Pageable pageable);
}
