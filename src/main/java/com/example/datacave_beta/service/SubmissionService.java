package com.example.datacave_beta.service;

import com.example.datacave_beta.model.Acceptance;
import com.example.datacave_beta.model.Quest;
import com.example.datacave_beta.model.Submission;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubmissionService {

    private final Store store;

    /** Mark a quest as started for a contributor (idempotent). */
    public Acceptance acceptQuest(Long contributorId, Long questId) {
        Optional<Acceptance> existing = store.acceptances().stream()
                .filter(a -> a.getContributorId().equals(contributorId)
                        && a.getQuestId().equals(questId)
                        && !"ABANDONED".equals(a.getStatus()))
                .findFirst();
        if (existing.isPresent()) return existing.get();

        long id = store.nextAcceptanceId();
        Acceptance a = Acceptance.builder()
                .id(id).contributorId(contributorId).questId(questId)
                .status("ACTIVE").acceptedAt(System.currentTimeMillis())
                .build();
        store.putAcceptance(a);
        return a;
    }

    /**
     * Create a submission. Optionally stores an uploaded file (photo/audio).
     * For the prototype, submissions are auto-reviewed: high quality => ACCEPTED
     * and the reward is credited (reflected in the dashboard).
     */
    public Submission submit(Long contributorId, Long questId, String type,
                             MultipartFile file, Map<String, Object> payload) throws IOException {
        Quest quest = store.quest(questId);
        if (quest == null) throw new IllegalArgumentException("Unknown quest " + questId);

        // ensure the quest is accepted
        acceptQuest(contributorId, questId);

        String storedName = null;
        String originalName = null;
        String contentType = null;
        Long size = null;

        if (file != null && !file.isEmpty()) {
            Files.createDirectories(store.uploadDir());
            String ext = extensionOf(file.getOriginalFilename(), file.getContentType());
            storedName = "sub_" + UUID.randomUUID().toString().replace("-", "") + ext;
            Path target = store.uploadDir().resolve(storedName);
            file.transferTo(target.toAbsolutePath());
            originalName = file.getOriginalFilename();
            contentType = file.getContentType();
            size = file.getSize();
        }

        // Auto-review: prototype quality model.
        int quality = ThreadLocalRandom.current().nextInt(85, 100);
        boolean accepted = quality >= 80; // always true here, but models the gate
        long now = System.currentTimeMillis();

        long id = store.nextSubmissionId();
        Submission s = Submission.builder()
                .id(id)
                .questId(questId)
                .questTitle(quest.getTitle())
                .contributorId(contributorId)
                .type(type != null ? type : inferType(quest.getCategory()))
                .fileName(storedName)
                .originalName(originalName)
                .contentType(contentType)
                .fileSize(size)
                .payload(payload)
                .status(accepted ? "ACCEPTED" : "PENDING_REVIEW")
                .reward(accepted ? quest.getReward() : 0.0)
                .qualityScore(quality)
                .submittedAt(now)
                .reviewedAt(accepted ? now : 0)
                .build();
        store.putSubmission(s);

        if (accepted) {
            quest.setCompletedCount(quest.getCompletedCount() + 1);
            store.touch();
        }
        log.info("Submission {} for quest {} by contributor {} -> {} (${})",
                id, questId, contributorId, s.getStatus(), s.getReward());
        return s;
    }

    public List<Submission> forContributor(Long contributorId) {
        return store.submissions().stream()
                .filter(s -> s.getContributorId().equals(contributorId))
                .sorted(Comparator.comparingLong(Submission::getSubmittedAt).reversed())
                .toList();
    }

    public Optional<Submission> submission(Long id) {
        return store.submissions().stream().filter(s -> s.getId().equals(id)).findFirst();
    }

    public Path fileFor(Submission s) {
        if (s.getFileName() == null) return null;
        return store.uploadDir().resolve(s.getFileName());
    }

    private String inferType(String category) {
        if (category == null) return "DATA";
        return switch (category) {
            case "Audio" -> "AUDIO";
            case "Image" -> "PHOTO";
            case "Motion & Location" -> "LOCATION";
            case "Health Data" -> "SURVEY";
            default -> "LABEL";
        };
    }

    private String extensionOf(String name, String contentType) {
        if (name != null && name.contains(".")) {
            return name.substring(name.lastIndexOf('.'));
        }
        if (contentType != null) {
            if (contentType.contains("jpeg") || contentType.contains("jpg")) return ".jpg";
            if (contentType.contains("png")) return ".png";
            if (contentType.contains("m4a") || contentType.contains("mp4")) return ".m4a";
            if (contentType.contains("mpeg") || contentType.contains("mp3")) return ".mp3";
            if (contentType.contains("wav")) return ".wav";
            if (contentType.contains("webm")) return ".webm";
            if (contentType.contains("caf")) return ".caf";
        }
        return ".bin";
    }
}
