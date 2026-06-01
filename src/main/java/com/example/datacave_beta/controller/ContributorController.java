package com.example.datacave_beta.controller;

import com.example.datacave_beta.model.ContributorStats;
import com.example.datacave_beta.service.DataService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/contributor")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ContributorController {

    private final DataService dataService;

    @GetMapping("/{id}/dashboard")
    public ContributorStats getDashboard(@PathVariable Long id) {
        return dataService.getContributorStats(id);
    }
}
