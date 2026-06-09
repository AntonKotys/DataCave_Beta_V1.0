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
    private final Map<Long, AnnotationSchema> schemas = new ConcurrentHashMap<>();
    private final Map<Long, StandingQuest> standingQuests = new ConcurrentHashMap<>();

    private final AtomicLong questSeq = new AtomicLong(100);
    private final AtomicLong datasetSeq = new AtomicLong(100);
    private final AtomicLong acceptanceSeq = new AtomicLong(1);
    private final AtomicLong submissionSeq = new AtomicLong(1);
    private final AtomicLong schemaSeq = new AtomicLong(100);
    private final AtomicLong standingSeq = new AtomicLong(100);

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
    public Collection<Contributor> contributors() { return contributors.values(); }
    public Collection<Acceptance> acceptances() { return acceptances.values(); }
    public Collection<Submission> submissions() { return submissions.values(); }
    public Collection<AnnotationSchema> schemas() { return schemas.values(); }
    public AnnotationSchema schema(Long id) { return schemas.get(id); }
    public Collection<StandingQuest> standingQuests() { return standingQuests.values(); }
    public StandingQuest standingQuest(Long id) { return standingQuests.get(id); }

    public long nextQuestId() { return questSeq.incrementAndGet(); }
    public long nextDatasetId() { return datasetSeq.incrementAndGet(); }
    public long nextAcceptanceId() { return acceptanceSeq.incrementAndGet(); }
    public long nextSubmissionId() { return submissionSeq.incrementAndGet(); }
    public long nextSchemaId() { return schemaSeq.incrementAndGet(); }
    public long nextStandingId() { return standingSeq.incrementAndGet(); }

    public void putQuest(Quest q) { quests.put(q.getId(), q); save(); }
    public void putDataset(Dataset d) { datasets.put(d.getId(), d); save(); }
    public void putContributor(Contributor c) { contributors.put(c.getId(), c); save(); }
    public void putAcceptance(Acceptance a) { acceptances.put(a.getId(), a); save(); }
    public void putSubmission(Submission s) { submissions.put(s.getId(), s); save(); }
    public void putSchema(AnnotationSchema s) { schemas.put(s.getId(), s); save(); }
    public void putStandingQuest(StandingQuest s) { standingQuests.put(s.getId(), s); save(); }
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
            if (snap.schemas != null) snap.schemas.forEach(s -> schemas.put(s.getId(), s));
            if (snap.standingQuests != null) snap.standingQuests.forEach(s -> standingQuests.put(s.getId(), s));
            questSeq.set(Math.max(questSeq.get(), maxId(quests.keySet())));
            datasetSeq.set(Math.max(datasetSeq.get(), maxId(datasets.keySet())));
            acceptanceSeq.set(Math.max(acceptanceSeq.get(), maxId(acceptances.keySet())));
            submissionSeq.set(Math.max(submissionSeq.get(), maxId(submissions.keySet())));
            schemaSeq.set(Math.max(schemaSeq.get(), maxId(schemas.keySet())));
            standingSeq.set(Math.max(standingSeq.get(), maxId(standingQuests.keySet())));
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
            snap.schemas = new ArrayList<>(schemas.values());
            snap.standingQuests = new ArrayList<>(standingQuests.values());
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
        public List<AnnotationSchema> schemas = new ArrayList<>();
        public List<StandingQuest> standingQuests = new ArrayList<>();
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

        // --- Freshness (G): set collection date + domain decay per dataset -----
        long day = 24L * 3600 * 1000;
        setFreshness(1L, now - 40 * day, 365);   // urban audio — slow decay, ~40d old
        setFreshness(2L, now - 12 * day, 90);     // retail images — 12d old
        setFreshness(3L, now - 70 * day, 365);    // dialect speech — 70d old
        setFreshness(4L, now - 25 * day, 365);    // sleep survey — 25d old

        // --- Annotation schema library (N) -------------------------------------
        seedSchema("Fashion Garment Attributes", "Fashion", "Rich garment metadata for style/recommendation models.", "1.2", 96, List.of(
            field("garment_type", "single-choice", "Primary garment category", List.of("Dress", "Top", "Trousers", "Skirt", "Outerwear", "Footwear", "Accessory"), true),
            field("primary_color", "text", "Dominant color", null, true),
            field("pattern", "single-choice", "Surface pattern", List.of("Solid", "Striped", "Floral", "Checked", "Graphic", "Other"), true),
            field("style_aesthetic", "single-choice", "Overall aesthetic", List.of("Casual", "Smart casual", "Formal", "Streetwear", "Athleisure", "Bohemian"), true),
            field("occasion", "multi-choice", "Suitable occasions", List.of("Work", "Everyday", "Evening", "Sport", "Special event"), false),
            field("trend_score", "number", "Current trend relevance (1-10)", null, false)
        ));
        seedSchema("Surgical Instrument Annotation", "Surgical", "Pixel-precise instrument labels for surgical perception models. Expert-only.", "2.0", 99, List.of(
            field("instrument_type", "single-choice", "Instrument visible", List.of("Grasper", "Scissors", "Clip applier", "Hook", "Needle driver", "Irrigator", "Specimen bag"), true),
            field("bounding_box", "text", "Box convention: enclose FULL shaft + tip", null, true),
            field("tissue_contact", "boolean", "Is the instrument in contact with tissue?", null, true),
            field("occlusion", "single-choice", "Visibility of the instrument", List.of("Full", "Partial", "Heavily occluded"), true)
        ));
        seedSchema("Ad Creative Reaction", "Marketing", "Genuine consumer reaction signals to a creative asset (not expert opinion).", "1.0", 90, List.of(
            field("emotional_response", "single-choice", "Your gut emotional reaction", List.of("Love it", "Like it", "Neutral", "Dislike it", "Hate it"), true),
            field("authenticity", "number", "How authentic does it feel? (1-5)", null, true),
            field("purchase_intent", "single-choice", "Would this make you consider buying?", List.of("Definitely", "Maybe", "No"), true),
            field("first_word", "text", "First word that comes to mind", null, false)
        ));
        seedSchema("Sentiment & Emotion", "NLP", "Standard text/clip sentiment classification with confidence.", "1.1", 93, List.of(
            field("label", "single-choice", "Overall sentiment", List.of("Positive", "Neutral", "Negative", "Mixed"), true),
            field("confidence", "number", "Your confidence (1-5)", null, true)
        ));
        seedSchema("Handwriting Sample Analysis", "OCR", "Labels for handwriting digitization quests — used by handwriting recognition and OCR models.", "1.0", 95, List.of(
            field("writing_style", "single-choice", "Overall handwriting style", List.of("Print", "Cursive", "Mixed", "Block capitals"), true),
            field("legibility", "single-choice", "How clearly machine-readable is the text?", List.of("Excellent", "Good", "Fair", "Poor"), true),
            field("language", "text", "Language of the handwritten text (e.g. English, Georgian, German)", null, true),
            field("ink_color", "single-choice", "Ink or pen color", List.of("Black", "Blue", "Red", "Green", "Pencil", "Other"), true),
            field("paper_type", "single-choice", "Background / paper type", List.of("Lined", "Blank", "Graph", "Other"), true),
            field("slant", "single-choice", "Direction of letter slant", List.of("Left", "Upright", "Right"), false),
            field("text_content", "text", "Transcribe the handwritten text exactly as written", null, true)
        ));

        // --- Demand-side flags on quests ---------------------------------------
        // Expert-only specialist quests (B)
        tagQuest(6L, "Verified Expert", true, "medical", null);
        tagQuest(9L, "Verified Expert", true, "medical", null);
        // Tier-gated + schema-attached quests (B + N)
        tagQuest(4L, "Silver Mapper", false, null, null);
        tagQuest(2L, null, false, "fashion", 101L);   // shelf/fashion images -> Fashion schema
        tagQuest(3L, null, false, null, 104L);          // emotion labeling -> Sentiment schema
        tagQuest(10L, null, false, null, 104L);         // sentiment tagging -> Sentiment schema
        tagQuest(8L, null, false, null, 104L);          // traffic classification -> Sentiment-style schema
        tagQuest(7L, null, false, null, 105L);          // handwriting samples -> Handwriting schema

        // --- Standing quest / subscription (E) ---------------------------------
        long sid = nextStandingId();
        standingQuests.put(sid, StandingQuest.builder()
            .id(sid).title("Weekly Fashion Trend Imagery").company("Atman (Fashion AI)")
            .category("Image").description("Fresh, expert-labeled trend imagery delivered every week so the trend model never goes stale.")
            .rewardPerSubmission(3.50).batchSize(150).frequency("WEEKLY").schemaId(101L)
            .status("ACTIVE").createdAt(now - 21 * day).nextDeliveryAt(now + 2 * day)
            .deliveriesCompleted(3)
            .deliveries(new java.util.ArrayList<>(List.of(
                Delivery.builder().batchNumber(1).deliveredAt(now - 21 * day).sampleCount(150).freshnessScore(40).note("Initial batch").build(),
                Delivery.builder().batchNumber(2).deliveredAt(now - 14 * day).sampleCount(150).freshnessScore(60).note("Weekly refresh").build(),
                Delivery.builder().batchNumber(3).deliveredAt(now - 7 * day).sampleCount(150).freshnessScore(85).note("Weekly refresh").build()
            )))
            .build());

        log.info("Seeded DataCave demo data ({} schemas, {} standing quests).", schemas.size(), standingQuests.size());
    }

    private void setFreshness(long datasetId, long collectedAt, int decayDays) {
        Dataset d = datasets.get(datasetId);
        if (d != null) { d.setCollectedAt(collectedAt); d.setDomainDecayDays(decayDays); }
    }

    private void tagQuest(long questId, String requiredTier, boolean expertOnly, String domain, Long schemaId) {
        Quest q = quests.get(questId);
        if (q == null) return;
        q.setRequiredTier(requiredTier);
        q.setExpertOnly(expertOnly);
        q.setDomain(domain);
        q.setSchemaId(schemaId);
    }

    private SchemaField field(String name, String type, String desc, List<String> options, boolean required) {
        return SchemaField.builder().name(name).type(type).description(desc).options(options).required(required).build();
    }

    private void seedSchema(String name, String vertical, String desc, String version, int rating, List<SchemaField> fields) {
        long id = nextSchemaId();
        schemas.put(id, AnnotationSchema.builder()
            .id(id).name(name).vertical(vertical).description(desc).version(version)
            .rating(rating).usageCount(0).fields(fields).build());
    }
}
