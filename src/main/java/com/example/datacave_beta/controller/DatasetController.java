package com.example.datacave_beta.controller;

import com.example.datacave_beta.model.Dataset;
import com.example.datacave_beta.service.CatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/datasets")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class DatasetController {

    private final CatalogService catalog;

    @GetMapping
    public List<Dataset> getAll() {
        return catalog.allDatasets();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Dataset> getById(@PathVariable Long id) {
        return catalog.dataset(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }
}
