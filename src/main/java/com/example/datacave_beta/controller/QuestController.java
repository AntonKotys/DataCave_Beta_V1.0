package com.example.datacave_beta.controller;

import com.example.datacave_beta.model.Acceptance;
import com.example.datacave_beta.model.IaaReport;
import com.example.datacave_beta.model.Quest;
import com.example.datacave_beta.model.QuestRequest;
import com.example.datacave_beta.service.CatalogService;
import com.example.datacave_beta.service.IaaService;
import com.example.datacave_beta.service.SubmissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/quests")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class QuestController {

    private final CatalogService catalog;
    private final SubmissionService submissions;
    private final IaaService iaaService;

    @GetMapping
    public List<Quest> getAll(@RequestParam(required = false) String category) {
        if (category != null && !category.isBlank() && !category.equalsIgnoreCase("all")) {
            return catalog.questsByCategory(category);
        }
        return catalog.allQuests();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Quest> getById(@PathVariable Long id) {
        return catalog.quest(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    /** Company-side: publish a new quest. */
    @PostMapping
    public Quest create(@RequestBody QuestRequest quest) {
        return catalog.createQuest(quest);
    }

    /** Contributor starts a quest. Returns 400 with a reason if the quest is gated (B). */
    @PostMapping("/{id}/accept")
    public ResponseEntity<?> accept(@PathVariable Long id,
                                    @RequestParam(defaultValue = "1") Long contributorId) {
        if (catalog.quest(id).isEmpty()) return ResponseEntity.notFound().build();
        try {
            return ResponseEntity.ok(submissions.acceptQuest(contributorId, id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(403).body(Map.of("error", e.getMessage(), "locked", true));
        }
    }

    /** Inter-annotator agreement report for a quest (D). */
    @GetMapping("/{id}/iaa")
    public ResponseEntity<IaaReport> iaa(@PathVariable Long id) {
        if (catalog.quest(id).isEmpty()) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(iaaService.forQuest(id));
    }
}
