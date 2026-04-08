package com.example.template.service;

import com.example.template.entity.SearchConfig;
import com.example.template.repository.SearchConfigRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SearchConfigService {

    private final SearchConfigRepository repository;

    public SearchConfigService(SearchConfigRepository repository) {
        this.repository = repository;
    }

    public List<SearchConfig> findAll() {
        return repository.findAll();
    }

    public SearchConfig findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Search config not found: " + id));
    }

    public SearchConfig create(SearchConfig config) {
        return repository.save(config);
    }

    public SearchConfig update(Long id, SearchConfig updated) {
        SearchConfig existing = findById(id);
        existing.setName(updated.getName());
        existing.setOriginAirport(updated.getOriginAirport());
        existing.setDestinationAirport(updated.getDestinationAirport());
        existing.setSearchFromDate(updated.getSearchFromDate());
        existing.setSearchToDate(updated.getSearchToDate());
        existing.setMinPrice(updated.getMinPrice());
        existing.setMaxPrice(updated.getMaxPrice());
        existing.setTripType(updated.getTripType());
        existing.setPollIntervalMinutes(updated.getPollIntervalMinutes());
        existing.setEnabled(updated.getEnabled());
        return repository.save(existing);
    }

    public SearchConfig toggleEnabled(Long id) {
        SearchConfig config = findById(id);
        config.setEnabled(!config.getEnabled());
        return repository.save(config);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }
}
