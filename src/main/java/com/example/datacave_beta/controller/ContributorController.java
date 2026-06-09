package com.example.datacave_beta.controller;

import com.example.datacave_beta.model.Contributor;
import com.example.datacave_beta.model.ContributorStats;
import com.example.datacave_beta.model.Submission;
import com.example.datacave_beta.service.DashboardService;
import com.example.datacave_beta.service.ExpertService;
import com.example.datacave_beta.service.Store;
import com.example.datacave_beta.service.SubmissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/contributor")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ContributorController {

    private final DashboardService dashboardService;
    private final SubmissionService submissionService;
    private final ExpertService expertService;
    private final Store store;

    @GetMapping("/{id}/dashboard")
    public ResponseEntity<ContributorStats> getDashboard(@PathVariable Long id) {
        ContributorStats stats = dashboardService.dashboard(id);
        return stats == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(stats);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Contributor> profile(@PathVariable Long id) {
        Contributor c = store.contributor(id);
        return c == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(c);
    }

    @GetMapping("/{id}/submissions")
    public List<Submission> submissions(@PathVariable Long id) {
        return submissionService.forContributor(id);
    }

    /** Apply for Verified Expert status in one or more domains (B). Prototype: instant verification. */
    @PostMapping("/{id}/verify-expert")
    public ResponseEntity<?> verifyExpert(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        try {
            @SuppressWarnings("unchecked")
            List<String> domains = (List<String>) body.getOrDefault("domains", List.of());
            return ResponseEntity.ok(expertService.verify(id, domains));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
