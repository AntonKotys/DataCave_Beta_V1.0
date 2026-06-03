package com.example.datacave_beta.service;

import com.example.datacave_beta.model.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * In-memory data store with best-effort JSON snapshot persistence so prototype
 * state (accepted quests, submissions, earnings) survives a restart. Uploaded
 * files are stored on disk separately.
 */
@Component
@Slf4j
public class Store {

    private final Map<Long, Quest> quests = new ConcurrentHashMap<>();
    private final Map<Long, Dataset> datasets = new ConcurrentHashMap<>();
    private final Map<Long, Contributor> contributors = new ConcurrentHashMap<>();
    private final Map<Long, Acceptance> acceptances = new ConcurrentHashMap<>();
    private final Map<Long, Submission> submissions = new ConcurrentHashMap<>();

    private final AtomicLong questSeq = new AtomicLong(100);
    private final AtomicLong datasetSeq = new AtomicLong(100);
    private final AtomicLong acceptanceSeq = new AtomicLong(1);
    private final AtomicLong submissionSeq = new AtomicLong(1);

    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${datacave.data-dir:data}")
    private String dataDir;

    private Path stateFile() {
        return Path.of(dataDir, "state.json");
    }

    public Path uploadDir() {
        return Path.of(dataDir, "uploads");
    }

    // --- accessors -----------------------------------------------------------

    public Collection<Quest> quests() { return quests.values(); }
    public Quest quest(Long id) { return quests.get(id); }
    public Collection<Dataset> datasets() { return datasets.values(); }
    public Dataset dataset(Long id) { return datasets.get(id); }
    public Contributor contributor(Long id) { return contributors.get(id); }
    public Collection<Acceptance> acceptances() { return acceptances.values(); }
    public Collection<Submission> submissions() { return submissions.values(); }

    public long nextQuestId() { return questSeq.incrementAndGet(); }
    public long nextDatasetId() { return datasetSeq.incrementAndGet(); }
    public long nextAcceptanceId() { return acceptanceSeq.incrementAndGet(); }
    public long nextSubmissionId() { return submissionSeq.incrementAndGet(); }

    public void putQuest(Quest q) { quests.put(q.getId(), q); save(); }
    public void putDataset(Dataset d) { datasets.put(d.getId(), d); save(); }
    public void putAcceptance(Acceptance a) { acceptances.put(a.getId(), a); save(); }
    public void putSubmission(Submission s) { submissions.put(s.getId(), s); save(); }
    public void touch() { save(); }

    // --- persistence ---------------------------------------------------------

    @PostConstruct
    void init() {
        try {
            Files.createDirectories(uploadDir());
        } catch (IOException e) {
            log.warn("Could not create upload dir: {}", e.getMessage());
        }
        if (!load()) {
            seed();
            save();
        }
    }

    @PreDestroy
    void shutdown() { save(); }

    @SuppressWarnings("unchecked")
    private boolean load() {
        File f = stateFile().toFile();
        if (!f.exists()) return false;
        try {
            Snapshot snap = mapper.readValue(f, Snapshot.class);
            snap.quests.forEach(q -> quests.put(q.getId(), q));
            snap.datasets.forEach(d -> datasets.put(d.getId(), d));
            snap.contributors.forEach(c -> contributors.put(c.getId(), c));
            snap.acceptances.forEach(a -> acceptances.put(a.getId(), a));
            snap.submissions.forEach(s -> submissions.put(s.getId(), s));
            questSeq.set(Math.max(questSeq.get(), maxId(quests.keySet())));
            datasetSeq.set(Math.max(datasetSeq.get(), maxId(datasets.keySet())));
            acceptanceSeq.set(Math.max(acceptanceSeq.get(), maxId(acceptances.keySet())));
            submissionSeq.set(Math.max(submissionSeq.get(), maxId(submissions.keySet())));
            log.info("Loaded DataCave state: {} quests, {} submissions, {} acceptances",
                    quests.size(), submissions.size(), acceptances.size());
            return !quests.isEmpty();
        } catch (Exception e) {
            log.warn("Could not load state, seeding fresh: {}", e.getMessage());
            return false;
        }
    }

    private long maxId(Set<Long> ids) {
        return ids.stream().mapToLong(Long::longValue).max().orElse(0);
    }

    public synchronized void save() {
        try {
            Files.createDirectories(Path.of(dataDir));
            Snapshot snap = new Snapshot();
            snap.quests = new ArrayList<>(quests.values());
            snap.datasets = new ArrayList<>(datasets.values());
            snap.contributors = new ArrayList<>(contributors.values());
            snap.acceptances = new ArrayList<>(acceptances.values());
            snap.submissions = new ArrayList<>(submissions.values());
            mapper.writerWithDefaultPrettyPrinter().writeValue(stateFile().toFile(), snap);
        } catch (Exception e) {
            log.warn("Could not persist state: {}", e.getMessage());
        }
    }

    /** JSON snapshot container. */
    public static class Snapshot {
        public List<Quest> quests = new ArrayList<>();
        public List<Dataset> datasets = new ArrayList<>();
        public List<Contributor> contributors = new ArrayList<>();
        public List<Acceptance> acceptances = new ArrayList<>();
        public List<Submission> submissions = new ArrayList<>();
    }

    // --- seed ----------------------------------------------------------------

    private void seed() {
        List.of(
            Quest.builder().id(1L).title("Urban Street Audio Collection").description("Record 30 seconds of ambient street noise in your city — traffic, voices, environment. No music.").category("Audio").categoryIcon("🎙️").reward(2.50).difficulty("Easy").deadline("Jun 15, 2026").requiredCount(500).completedCount(312).company("SoundAI Labs").tags(List.of("audio", "urban", "ambient")).status("OPEN").estimatedMinutes(5).build(),
            Quest.builder().id(2L).title("Grocery Shelf Photo Mapping").description("Photograph grocery store shelves in your local supermarket. Capture product labels clearly.").category("Image").categoryIcon("📸").reward(3.75).difficulty("Easy").deadline("Jun 20, 2026").requiredCount(1000).completedCount(734).company("RetailVision AI").tags(List.of("image", "retail", "computer-vision")).status("OPEN").estimatedMinutes(10).build(),
            Quest.builder().id(3L).title("Emotion Labeling – Short Video Clips").description("Watch 10-second video clips and rate the emotional content displayed. Multiple choice labeling.").category("Text & Label").categoryIcon("🏷️").reward(1.80).difficulty("Easy").deadline("Jun 25, 2026").requiredCount(2000).completedCount(1450).company("EmotiSense").tags(List.of("labeling", "emotion", "video")).status("OPEN").estimatedMinutes(8).build(),
            Quest.builder().id(4L).title("Dialect Speech Recording – Georgian").description("Read 20 short sentences aloud in your native Georgian dialect. Clear pronunciation required.").category("Audio").categoryIcon("🎙️").reward(5.00).difficulty("Medium").deadline("Jul 1, 2026").requiredCount(300).completedCount(89).company("LinguaCore").tags(List.of("audio", "speech", "language")).status("OPEN").estimatedMinutes(15).build(),
            Quest.builder().id(5L).title("Pedestrian Path Tracking").description("Capture your current location while walking your commute route. Helps train mobility models.").category("Motion & Location").categoryIcon("📍").reward(4.20).difficulty("Medium").deadline("Jul 5, 2026").requiredCount(800).completedCount(421).company("UrbanFlow Analytics").tags(List.of("location", "motion", "mobility")).status("OPEN").estimatedMinutes(20).build(),
            Quest.builder().id(6L).title("Medical Symptom Survey – Sleep Patterns").description("Complete a structured survey about your sleep quality and habits. Consent required.").category("Health Data").categoryIcon("🩺").reward(8.00).difficulty("Medium").deadline("Jul 10, 2026").requiredCount(1500).completedCount(203).company("HealthAI Research").tags(List.of("health", "survey", "sleep")).status("OPEN").estimatedMinutes(15).build(),
            Quest.builder().id(7L).title("Handwriting Digitization Samples").description("Write a short paragraph by hand and photograph it. Helps train handwriting recognition models.").category("Image").categoryIcon("📸").reward(3.00).difficulty("Easy").deadline("Jul 15, 2026").requiredCount(600).completedCount(580).company("DocuSense AI").tags(List.of("image", "handwriting", "ocr")).status("OPEN").estimatedMinutes(12).build(),
            Quest.builder().id(8L).title("Real-time Traffic Scene Classification").description("Review road images and classify conditions, hazards, and vehicle types.").category("Text & Label").categoryIcon("🏷️").reward(2.20).difficulty("Medium").deadline("Jun 30, 2026").requiredCount(5000).completedCount(3800).company("AutoDrive Labs").tags(List.of("labeling", "traffic", "automotive")).status("OPEN").estimatedMinutes(6).build(),
            Quest.builder().id(9L).title("Wearable Heart Rate Survey").description("Report a week of resting heart-rate readings from your smartwatch or fitness tracker.").category("Health Data").categoryIcon("🩺").reward(12.00).difficulty("Hard").deadline("Jul 20, 2026").requiredCount(200).completedCount(34).company("CardioAI").tags(List.of("health", "wearable", "biometric")).status("OPEN").estimatedMinutes(10).build(),
            Quest.builder().id(10L).title("Product Review Sentiment Tagging").description("Read product reviews and tag them: positive, negative, neutral, or mixed.").category("Text & Label").categoryIcon("🏷️").reward(2.00).difficulty("Easy").deadline("Jun 28, 2026").requiredCount(10000).completedCount(6200).company("ShopMind AI").tags(List.of("text", "nlp", "sentiment")).status("OPEN").estimatedMinutes(20).build()
        ).forEach(q -> quests.put(q.getId(), q));

        List.of(
            Dataset.builder().id(1L).name("Urban Audio Dataset v2").description("500+ hours of consented urban ambient audio across 12 cities. Fully labeled and ethics-verified.").category("Audio").size("48 GB").format("WAV / JSON metadata").price(4500).license("Commercial Research License").qualityScore(94).ethicsVerified(true).consentDocumented(true).gdprCompliant(true).contributorCount(312).lastUpdated("May 2026").tags(List.of("audio", "urban", "ambient")).build(),
            Dataset.builder().id(2L).name("Retail Shelf Image Corpus").description("220,000 annotated grocery shelf images from 8 countries. Bounding boxes on 1,200+ product categories.").category("Image").size("130 GB").format("JPEG / COCO JSON").price(8900).license("Enterprise License").qualityScore(91).ethicsVerified(true).consentDocumented(true).gdprCompliant(true).contributorCount(734).lastUpdated("May 2026").tags(List.of("image", "retail", "cv")).build(),
            Dataset.builder().id(3L).name("Multilingual Dialect Speech Pack").description("Recorded speech in 14 under-resourced language dialects. Native speakers, verified pronunciation.").category("Audio").size("22 GB").format("MP3 / TextGrid").price(6200).license("Research License").qualityScore(97).ethicsVerified(true).consentDocumented(true).gdprCompliant(true).contributorCount(89).lastUpdated("Apr 2026").tags(List.of("audio", "speech", "language")).build(),
            Dataset.builder().id(4L).name("Sleep Quality Survey Dataset").description("Structured sleep pattern surveys from 203 verified contributors with demographic metadata.").category("Health").size("2.1 GB").format("CSV / Parquet").price(3100).license("Medical Research License").qualityScore(89).ethicsVerified(true).consentDocumented(true).gdprCompliant(true).contributorCount(203).lastUpdated("May 2026").tags(List.of("health", "survey", "wellness")).build()
        ).forEach(d -> datasets.put(d.getId(), d));

        Contributor alex = Contributor.builder()
            .id(1L).name("Alex M.").tier("Silver Mapper").tierProgress(340).tierTarget(500)
            .reputationScore(88).globalRank(1204).streak(12)
            .baseEarnings(847.30).baseWeekly(42.50).baseMonthly(163.20).baseCompleted(73)
            .badges(List.of(
                Badge.builder().name("First Quest").icon("🚀").description("Completed your first quest").earnedDate("Jan 2026").build(),
                Badge.builder().name("Audio Expert").icon("🎙️").description("Completed 20+ audio quests").earnedDate("Feb 2026").build(),
                Badge.builder().name("Week Streak").icon("🔥").description("Active 7 days in a row").earnedDate("Mar 2026").build(),
                Badge.builder().name("Quality Star").icon("⭐").description("95%+ acceptance rate for 30 quests").earnedDate("Apr 2026").build(),
                Badge.builder().name("City Scout").icon("🗺️").description("Completed 5 location quests").earnedDate("Apr 2026").build(),
                Badge.builder().name("Top 5%").icon("🏆").description("Ranked in top 5% globally").earnedDate("May 2026").build()
            ))
            .seededEarnings(List.of(
                EarningsEntry.builder().questTitle("Real-time Traffic Scene Classification").amount(2.20).date("May 28, 2026").status("PAID").build(),
                EarningsEntry.builder().questTitle("Dialect Speech Recording – Georgian").amount(5.00).date("May 27, 2026").status("PAID").build(),
                EarningsEntry.builder().questTitle("Grocery Shelf Photo Mapping").amount(3.75).date("May 26, 2026").status("PAID").build()
            ))
            .build();
        contributors.put(alex.getId(), alex);

        // Pre-accept the first three quests so the dashboard shows active work.
        long now = System.currentTimeMillis();
        for (long qid : new long[]{1L, 2L, 3L}) {
            long id = nextAcceptanceId();
            acceptances.put(id, Acceptance.builder().id(id).contributorId(1L).questId(qid)
                    .status("ACTIVE").acceptedAt(now).build());
        }
        log.info("Seeded DataCave demo data.");
    }
}
