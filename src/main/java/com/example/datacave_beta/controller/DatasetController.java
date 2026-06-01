package com.example.datacave_beta.controller;

import com.example.datacave_beta.model.Dataset;
import com.example.datacave_beta.service.DataService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/datasets")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class DatasetController {

    private final DataService dataService;

    @GetMapping
    public List<Dataset> getAll() {
        return dataService.getAllDatasets();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Dataset> getById(@PathVariable Long id) {
        return dataService.getDatasetById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
}
