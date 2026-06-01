package com.example.datacave_beta.controller;

import com.example.datacave_beta.model.Quest;
import com.example.datacave_beta.service.DataService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/quests")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class QuestController {

    private final DataService dataService;

    @GetMapping
    public List<Quest> getAll(@RequestParam(required = false) String category) {
        if (category != null && !category.isBlank()) {
            return dataService.getQuestsByCategory(category);
        }
        return dataService.getAllQuests();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Quest> getById(@PathVariable Long id) {
        return dataService.getQuestById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
}
