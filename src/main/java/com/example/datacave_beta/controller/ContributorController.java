package com.example.datacave_beta.controller;

import com.example.datacave_beta.model.ContributorStats;
import com.example.datacave_beta.model.Submission;
import com.example.datacave_beta.service.DashboardService;
import com.example.datacave_beta.service.SubmissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/contributor")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ContributorController {

    private final DashboardService dashboardService;
    private final SubmissionService submissionService;

    @GetMapping("/{id}/dashboard")
    public ResponseEntity<ContributorStats> getDashboard(@PathVariable Long id) {
        ContributorStats stats = dashboardService.dashboard(id);
        return stats == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(stats);
    }

    @GetMapping("/{id}/submissions")
    public List<Submission> submissions(@PathVariable Long id) {
        return submissionService.forContributor(id);
    }
}
