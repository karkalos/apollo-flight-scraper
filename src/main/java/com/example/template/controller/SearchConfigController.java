package com.example.template.controller;

import com.example.template.entity.SearchConfig;
import com.example.template.service.SearchConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/config")
@Tag(name = "Search Configuration", description = "Manage flight search configurations")
public class SearchConfigController {

    private final SearchConfigService configService;

    public SearchConfigController(SearchConfigService configService) {
        this.configService = configService;
    }

    @GetMapping
    @Operation(summary = "List all search configurations")
    public List<SearchConfig> listConfigs() {
        return configService.findAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a single search configuration")
    public SearchConfig getConfig(@PathVariable Long id) {
        return configService.findById(id);
    }

    @PostMapping
    @Operation(summary = "Create a new search configuration")
    public SearchConfig createConfig(@RequestBody SearchConfig config) {
        return configService.create(config);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a search configuration")
    public SearchConfig updateConfig(@PathVariable Long id, @RequestBody SearchConfig config) {
        return configService.update(id, config);
    }

    @PatchMapping("/{id}/toggle")
    @Operation(summary = "Toggle enabled/disabled status")
    public SearchConfig toggleConfig(@PathVariable Long id) {
        return configService.toggleEnabled(id);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a search configuration")
    public ResponseEntity<Void> deleteConfig(@PathVariable Long id) {
        configService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
