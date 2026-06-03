package com.example.datacave_beta.controller;

import com.example.datacave_beta.model.Acceptance;
import com.example.datacave_beta.model.Quest;
import com.example.datacave_beta.model.QuestRequest;
import com.example.datacave_beta.service.CatalogService;
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

    /** Contributor starts a quest. */
    @PostMapping("/{id}/accept")
    public ResponseEntity<Acceptance> accept(@PathVariable Long id,
                                             @RequestParam(defaultValue = "1") Long contributorId) {
        if (catalog.quest(id).isEmpty()) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(submissions.acceptQuest(contributorId, id));
    }
}
