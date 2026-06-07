package dev.echo.nativeplatform.cli;

import dev.echo.nativeplatform.contracts.EchoNativeDiagnostic;
import dev.echo.nativeplatform.contracts.EchoNativeIssueSeverity;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class EchoNativeBetaSessionNoteValidator {
    private static final int TARGET_INTERNAL_SESSION_COUNT = 3;
    private static final Pattern FIXTURE_LOG_EVIDENCE_PATTERN = Pattern.compile(
            "fixtures/[A-Za-z0-9_./-]+/(?:latest\\.log|[A-Za-z0-9_.-]+\\.log\\.gz|[A-Za-z0-9_.-]+\\.log)"
    );

    EchoNativeBetaSessionNoteValidationOutcome validate(String packId, Path fixture) throws IOException {
        List<EchoNativeDiagnostic> diagnostics = new ArrayList<>();
        List<Map<String, Object>> notes = notes(fixture);
        int currentM29QualifiedCount = 0;
        int completeSoakStandardCount = 0;
        int distinctM30LogEvidenceCount = distinctM30LogEvidenceCount(notes);
        int ignoredDraftCount = 0;
        for (Map<String, Object> note : notes) {
            if (Boolean.TRUE.equals(note.get("ignored"))) {
                ignoredDraftCount++;
                continue;
            }
            if (Boolean.TRUE.equals(note.get("qualifiesForCurrentM29Count"))) {
                currentM29QualifiedCount++;
            }
            if (Boolean.TRUE.equals(note.get("completeForM30SoakStandard"))) {
                completeSoakStandardCount++;
            }
        }

        if (currentM29QualifiedCount < TARGET_INTERNAL_SESSION_COUNT) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-BETA-SESSION-NOTE-COUNT-LOW",
                    EchoNativeIssueSeverity.WARNING,
                    "More native-loader beta session notes are required",
                    "The validator found " + currentM29QualifiedCount + " current M29-qualified session note(s); target is " + TARGET_INTERNAL_SESSION_COUNT + " before M30 can pass.",
                    null,
                    packId,
                    List.of("fixtures/" + packId + "/native-loader-beta-feedback", "fixtures/" + packId + "/native-loader-beta-notes"),
                    "Run and document additional real tester sessions, then rerun beta feedback, beta soak, and M30 verification."
            ));
        }

        if (notes.isEmpty()) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-BETA-SESSION-NOTES-MISSING",
                    EchoNativeIssueSeverity.WARNING,
                    "No native-loader beta session notes were found",
                    "No note, feedback, draft, or template files were found for the native-loader beta session validator.",
                    null,
                    packId,
                    List.of("fixtures/" + packId + "/native-loader-beta-feedback", "fixtures/" + packId + "/native-loader-beta-notes"),
                    "Add real tester session notes after actual native-loader beta sessions."
            ));
        }

        List<EchoNativeDiagnostic> sortedDiagnostics = diagnostics.stream()
                .sorted(Comparator.comparing(EchoNativeDiagnostic::code)
                        .thenComparing(diagnostic -> diagnostic.moduleId() == null ? "" : diagnostic.moduleId())
                        .thenComparing(EchoNativeDiagnostic::summary))
                .toList();

        return new EchoNativeBetaSessionNoteValidationOutcome(
                packId,
                noteValidation(packId, notes, currentM29QualifiedCount, completeSoakStandardCount, distinctM30LogEvidenceCount, ignoredDraftCount, sortedDiagnostics),
                validationStatus(packId, notes, currentM29QualifiedCount, completeSoakStandardCount, distinctM30LogEvidenceCount, ignoredDraftCount, sortedDiagnostics),
                sortedDiagnostics
        );
    }

    private static Map<String, Object> noteValidation(
            String packId,
            List<Map<String, Object>> notes,
            int currentM29QualifiedCount,
            int completeSoakStandardCount,
            int distinctM30LogEvidenceCount,
            int ignoredDraftCount,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m29_beta_session_note_validation", diagnostics);
        data.put("completeForM30SoakStandardCount", completeSoakStandardCount);
        data.put("currentM29QualifiedSessionCount", currentM29QualifiedCount);
        data.put("distinctM30LogEvidenceCount", distinctM30LogEvidenceCount);
        data.put("ignoredDraftCount", ignoredDraftCount);
        data.put("noteCount", notes.size());
        data.put("notes", notes);
        data.put("packId", packId);
        data.put("publicBetaOpen", false);
        data.put("targetInternalSessionCount", TARGET_INTERNAL_SESSION_COUNT);
        data.put("summary", notes.isEmpty()
                ? "No native-loader beta session notes were available for validation."
                : "Native-loader beta session notes were validated without changing the M29/M30 gate.");
        return data;
    }

    private static Map<String, Object> validationStatus(
            String packId,
            List<Map<String, Object>> notes,
            int currentM29QualifiedCount,
            int completeSoakStandardCount,
            int distinctM30LogEvidenceCount,
            int ignoredDraftCount,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m29_beta_session_note_validation_status", diagnostics);
        int remaining = Math.max(0, TARGET_INTERNAL_SESSION_COUNT - currentM29QualifiedCount);
        int remainingDistinctEvidence = Math.max(0, TARGET_INTERNAL_SESSION_COUNT - distinctM30LogEvidenceCount);
        data.put("completeForM30SoakStandardCount", completeSoakStandardCount);
        data.put("currentM29QualifiedSessionCount", currentM29QualifiedCount);
        data.put("distinctM30LogEvidenceCount", distinctM30LogEvidenceCount);
        data.put("ignoredDraftCount", ignoredDraftCount);
        data.put("m30EvidenceGateChanged", true);
        data.put("noteCount", notes.size());
        data.put("packId", packId);
        data.put("publicBetaOpen", false);
        data.put("remainingM30DistinctEvidenceCount", remainingDistinctEvidence);
        data.put("remainingQualifiedSessionCount", remaining);
        data.put("targetInternalSessionCount", TARGET_INTERNAL_SESSION_COUNT);
        data.put("validationOnly", true);
        data.put("summary", remaining > 0
                ? "Session note validation still needs " + remaining + " additional current M29-qualified real session note(s)."
                : remainingDistinctEvidence > 0
                        ? "Session note validation sees enough current M29-qualified notes, but M30 still needs " + remainingDistinctEvidence + " additional distinct preserved log evidence item(s)."
                        : "Session note validation sees enough current M29-qualified notes and distinct M30 evidence; rerun beta soak and M30 verification.");
        return data;
    }

    private static int distinctM30LogEvidenceCount(List<Map<String, Object>> notes) {
        Set<String> evidencePaths = new LinkedHashSet<>();
        for (Map<String, Object> note : notes) {
            if (!Boolean.TRUE.equals(note.get("completeForM30SoakStandard"))) {
                continue;
            }
            Object rawPaths = note.get("logEvidencePaths");
            if (rawPaths instanceof List<?> paths) {
                for (Object path : paths) {
                    if (path != null) {
                        evidencePaths.add(String.valueOf(path));
                    }
                }
            }
        }
        return evidencePaths.size();
    }

    private static List<Map<String, Object>> notes(Path fixture) throws IOException {
        List<Map<String, Object>> notes = new ArrayList<>();
        collectNotes(notes, "native_loader_beta_feedback", fixture.resolve("native-loader-beta-feedback"));
        collectNotes(notes, "native_loader_beta_notes", fixture.resolve("native-loader-beta-notes"));
        notes.sort(Comparator.<Map<String, Object>, String>comparing(note -> String.valueOf(note.get("kind")))
                .thenComparing(note -> String.valueOf(note.get("path"))));
        return notes;
    }

    private static void collectNotes(List<Map<String, Object>> notes, String kind, Path directory) throws IOException {
        if (!Files.isDirectory(directory)) {
            return;
        }
        try (var stream = Files.walk(directory)) {
            for (Path file : stream.filter(Files::isRegularFile).sorted().toList()) {
                String name = file.getFileName().toString().toLowerCase();
                if (!name.endsWith(".md") && !name.endsWith(".txt") && !name.endsWith(".json")) {
                    continue;
                }
                notes.add(note(kind, directory, file));
            }
        }
    }

    private static Map<String, Object> note(String kind, Path directory, Path file) throws IOException {
        String text = Files.readString(file);
        String lower = text.toLowerCase();
        boolean ignored = isIgnoredDraft(directory, file);
        boolean testerPresent = hasTester(lower);
        boolean passResult = hasPassResult(lower);
        boolean mainMenuReached = hasMainMenu(lower);
        boolean worldCreateOrLoadReported = hasWorldCreateOrLoad(lower);
        boolean spawnReached = hasSpawn(lower);
        boolean noCrashReported = hasNoCrash(lower);
        boolean latestLogPreserved = hasLatestLog(lower);
        List<String> logEvidencePaths = logEvidencePaths(text);
        boolean currentM29Qualified = !ignored && testerPresent && passResult && worldCreateOrLoadReported && noCrashReported;
        boolean completeForM30SoakStandard = currentM29Qualified && mainMenuReached && spawnReached && latestLogPreserved && !logEvidencePaths.isEmpty();
        List<String> missingFields = missingFields(ignored, testerPresent, passResult, mainMenuReached, worldCreateOrLoadReported, spawnReached, noCrashReported, latestLogPreserved, logEvidencePaths);

        Map<String, Object> note = new LinkedHashMap<>();
        note.put("byteSize", Files.size(file));
        note.put("completeForM30SoakStandard", completeForM30SoakStandard);
        note.put("ignored", ignored);
        note.put("ignoredReason", ignored ? "draft_or_template" : "");
        note.put("kind", kind);
        note.put("latestLogPreserved", latestLogPreserved);
        note.put("logEvidencePaths", logEvidencePaths);
        note.put("mainMenuReached", mainMenuReached);
        note.put("missingFields", missingFields);
        note.put("noCrashReported", noCrashReported);
        note.put("path", relativePath(file));
        note.put("qualifiesForCurrentM29Count", currentM29Qualified);
        note.put("spawnReached", spawnReached);
        note.put("tester", testerName(text));
        note.put("testerPresent", testerPresent);
        note.put("passResult", passResult);
        note.put("worldCreateOrLoadReported", worldCreateOrLoadReported);
        return note;
    }

    private static List<String> missingFields(
            boolean ignored,
            boolean testerPresent,
            boolean passResult,
            boolean mainMenuReached,
            boolean worldCreateOrLoadReported,
            boolean spawnReached,
            boolean noCrashReported,
            boolean latestLogPreserved,
            List<String> logEvidencePaths
    ) {
        if (ignored) {
            return List.of("not_evidence_draft_or_template");
        }
        List<String> missing = new ArrayList<>();
        if (!testerPresent) {
            missing.add("tester");
        }
        if (!passResult) {
            missing.add("result_pass");
        }
        if (!mainMenuReached) {
            missing.add("main_menu_reached");
        }
        if (!worldCreateOrLoadReported) {
            missing.add("world_create_or_load");
        }
        if (!spawnReached) {
            missing.add("spawn_reached");
        }
        if (!noCrashReported) {
            missing.add("no_crash");
        }
        if (!latestLogPreserved) {
            missing.add("latest_log_preserved");
        }
        if (latestLogPreserved && logEvidencePaths.isEmpty()) {
            missing.add("fixture_relative_log_evidence_path");
        }
        return missing;
    }

    private static boolean isIgnoredDraft(Path directory, Path file) {
        Path relative = directory.relativize(file);
        String path = relative.toString().replace('\\', '/').toLowerCase();
        String name = file.getFileName().toString().toLowerCase();
        return path.contains("/_drafts/")
                || path.startsWith("_drafts/")
                || name.contains("draft")
                || name.contains("template")
                || name.contains("example")
                || name.contains("sample");
    }

    private static boolean hasTester(String lower) {
        return lower.contains("tester:") && !lower.contains("tester: \n") && !lower.contains("tester: <");
    }

    private static boolean hasPassResult(String lower) {
        return lower.contains("result: pass") || lower.contains("result=pass");
    }

    private static boolean hasMainMenu(String lower) {
        return lower.contains("main menu reached")
                || lower.contains("main menu loaded")
                || lower.contains("mainmenu: reached")
                || lower.contains("mainmenu=reached")
                || lower.contains("mainmenureached: true")
                || lower.contains("mainmenureached=true");
    }

    private static boolean hasWorldCreateOrLoad(String lower) {
        if (lower.contains("not_reached")) {
            return false;
        }
        return lower.contains("world was created")
                || lower.contains("new world was created")
                || lower.contains("world loaded")
                || lower.contains("create and load")
                || lower.contains("created and loaded")
                || lower.contains("worldcreateorload: created")
                || lower.contains("worldcreateorload: loaded")
                || lower.contains("worldcreateorload: both")
                || lower.contains("worldcreateorload=created")
                || lower.contains("worldcreateorload=loaded")
                || lower.contains("worldcreateorload=both");
    }

    private static boolean hasSpawn(String lower) {
        return lower.contains("spawn reached")
                || lower.contains("reached spawn")
                || lower.contains("spawn: reached")
                || lower.contains("spawn=reached")
                || lower.contains("spawnreached: true")
                || lower.contains("spawnreached=true");
    }

    private static boolean hasNoCrash(String lower) {
        return lower.contains("no crash") || lower.contains("crashreported=false");
    }

    private static boolean hasLatestLog(String lower) {
        return lower.contains("latest.log preserved")
                || lower.contains("latest log preserved")
                || lower.contains("latestlog: preserved")
                || lower.contains("latestlog=preserved")
                || lower.contains("latestlogpreserved: true")
                || lower.contains("latestlogpreserved=true");
    }

    private static List<String> logEvidencePaths(String text) {
        Set<String> paths = new LinkedHashSet<>();
        Matcher matcher = FIXTURE_LOG_EVIDENCE_PATTERN.matcher(text.replace('\\', '/'));
        while (matcher.find()) {
            paths.add(matcher.group());
        }
        return new ArrayList<>(paths);
    }

    private static String testerName(String text) {
        for (String line : text.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.toLowerCase().startsWith("tester:")) {
                String value = trimmed.substring("tester:".length()).trim();
                return value.isBlank() ? "unknown" : value;
            }
        }
        return "unknown";
    }

    private static Map<String, Object> base(String phase, List<EchoNativeDiagnostic> diagnostics) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("bytecodeMutated", false);
        data.put("cacheMutated", false);
        data.put("classloaderCreated", false);
        data.put("commandExecuted", false);
        data.put("configsMutated", false);
        data.put("diagnosticCount", diagnostics.size());
        data.put("diagnosticsCaptured", true);
        data.put("downloadAllowed", false);
        data.put("downloadsStarted", false);
        data.put("filesystemMutated", false);
        data.put("gameClassesResolved", false);
        data.put("gameProcessLaunched", false);
        data.put("generatedEvidenceAt", Instant.EPOCH.toString());
        data.put("jarsMutated", false);
        data.put("launcherInstallsMutated", false);
        data.put("libraryDownloadStarted", false);
        data.put("minecraftLaunched", false);
        data.put("nativeExtractionStarted", false);
        data.put("phase", phase);
        data.put("processLaunched", false);
        data.put("publicBetaOpen", false);
        data.put("registryInjected", false);
        data.put("registryMutated", false);
        data.put("savesMutated", false);
        data.put("serviceCodeExecuted", false);
        data.put("transformsEnabled", false);
        data.put("transformsPerformed", false);
        data.put("unsafeRuntimeWorkStarted", false);
        data.put("userCachesMutated", false);
        return data;
    }

    private static String relativePath(Path path) {
        Path root = Path.of("").toAbsolutePath().normalize();
        Path normalized = path.toAbsolutePath().normalize();
        if (normalized.startsWith(root)) {
            return root.relativize(normalized).toString().replace('\\', '/');
        }
        return path.toString().replace('\\', '/');
    }
}
