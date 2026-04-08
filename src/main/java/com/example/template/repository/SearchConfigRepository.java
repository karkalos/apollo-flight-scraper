package com.example.template.repository;

import com.example.template.entity.SearchConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SearchConfigRepository extends JpaRepository<SearchConfig, Long> {

    List<SearchConfig> findByEnabledTrue();
}
