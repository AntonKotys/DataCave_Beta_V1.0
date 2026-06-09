package com.example.datacave_beta.controller;

import com.example.datacave_beta.model.Dataset;
import com.example.datacave_beta.service.CatalogService;
import com.example.datacave_beta.service.FreshnessService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/datasets")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class DatasetController {

    private final CatalogService catalog;
    private final FreshnessService freshnessService;

    @GetMapping
    public List<Dataset> getAll() {
        return catalog.allDatasets();
    }

    /** Datasets enriched with live freshness scores + staleness alerts (G). */
    @GetMapping("/freshness")
    public List<Map<String, Object>> freshness() {
        return freshnessService.datasetsWithFreshness();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Dataset> getById(@PathVariable Long id) {
        return catalog.dataset(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }
}
