package dev.echo.nativeplatform.cli;

import dev.echo.nativeplatform.contracts.EchoNativeDiagnostic;
import dev.echo.nativeplatform.contracts.EchoNativeIssueSeverity;
import dev.echo.nativeplatform.diagnostics.EchoNativeJson;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class EchoNativeRuntimeFixtureApprovalDraftPlanner {
    EchoNativeRuntimeFixtureApprovalDraftOutcome plan(
            String packId,
            Path fixture,
            Map<String, Path> requiredReports
    ) throws IOException {
        List<EchoNativeDiagnostic> diagnostics = new ArrayList<>();
        Map<String, Object> handoff = readRequiredReport(requiredReports.get("runtime-fixture-handoff.json"), packId, "runtime-fixture-handoff.json", diagnostics);
        List<Map<String, Object>> handoffItems = handoffItems(handoff);
        if (handoffItems.isEmpty()) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-RUNTIME-FIXTURE-APPROVAL-DRAFT-HANDOFF-MISSING",
                    EchoNativeIssueSeverity.ERROR,
                    "Runtime fixture approval draft handoff items are missing",
                    "Approval draft planning requires runtime fixture handoff items.",
                    null,
                    packId,
                    List.of(relativeReportPath(requiredReports.get("runtime-fixture-handoff.json"))),
                    "Run phase13 prepare runtime-fixture-handoff before drafting approval evidence."
            ));
        }

        List<Map<String, Object>> draftEntries = new ArrayList<>();
        List<Map<String, Object>> hashReviews = new ArrayList<>();
        for (Map<String, Object> item : handoffItems) {
            String artifactId = String.valueOf(item.getOrDefault("artifactId", ""));
            String artifactKind = String.valueOf(item.getOrDefault("artifactKind", "runtime_fixture"));
            String expectedFixturePath = String.valueOf(item.getOrDefault("expectedFixturePath", ""));
            boolean relative = !Path.of(expectedFixturePath).isAbsolute() && !expectedFixturePath.contains("..");
            Path expectedPath = fixture.resolve(expectedFixturePath).normalize();
            boolean present = relative && Files.isRegularFile(expectedPath);
            long byteSize = present ? Files.size(expectedPath) : 0L;
            String sha256 = present ? sha256(expectedPath) : "<pending-file>";

            Map<String, Object> draft = baseItem(artifactId, artifactKind, expectedFixturePath);
            draft.put("approved", present);
            draft.put("byteSize", byteSize);
            draft.put("filePresent", present);
            draft.put("localPath", expectedFixturePath);
            draft.put("pathRelative", relative);
            draft.put("reviewReady", present);
            draft.put("reviewStatus", present ? "ready_for_review" : "pending_file");
            draft.put("reviewed", false);
            draft.put("sha256", sha256);
            draft.put("source", artifactId.startsWith("native:") ? "minecraft-native-fixture" : "minecraft-runtime-fixture");
            draftEntries.add(draft);

            Map<String, Object> review = baseItem(artifactId, artifactKind, expectedFixturePath);
            review.put("byteSize", byteSize);
            review.put("filePresent", present);
            review.put("hashAlgorithm", "SHA-256");
            review.put("hashComputed", present);
            review.put("pathRelative", relative);
            review.put("sha256", sha256);
            review.put("summary", present
                    ? "Hash evidence is ready for human review before runtime-fixture-approvals.json is updated."
                    : "Hash evidence is pending until the local runtime fixture file is supplied.");
            hashReviews.add(review);

            if (!present) {
                diagnostics.add(new EchoNativeDiagnostic(
                        "ECHO-NATIVE-RUNTIME-FIXTURE-APPROVAL-DRAFT-FILE-PENDING",
                        EchoNativeIssueSeverity.WARNING,
                        "Runtime fixture approval draft file is pending",
                        artifactId + " cannot include byteSize or sha256 until the local runtime fixture file exists.",
                        null,
                        packId,
                        List.of(fixture.resolve(expectedFixturePath).normalize().toString().replace('\\', '/')),
                        "Supply an already-authorized local artifact outside this CLI, then rerun the approval draft planner."
                ));
            }
        }

        draftEntries.sort(Comparator.comparing(entry -> String.valueOf(entry.get("artifactId"))));
        hashReviews.sort(Comparator.comparing(entry -> String.valueOf(entry.get("artifactId"))));
        List<EchoNativeDiagnostic> sortedDiagnostics = diagnostics.stream()
                .sorted(Comparator.comparing(EchoNativeDiagnostic::code)
                        .thenComparing(diagnostic -> diagnostic.moduleId() == null ? "" : diagnostic.moduleId())
                        .thenComparing(EchoNativeDiagnostic::summary))
                .toList();
        boolean draftReady = !draftEntries.isEmpty() && draftEntries.stream().allMatch(entry -> Boolean.TRUE.equals(entry.get("reviewReady")));
        return new EchoNativeRuntimeFixtureApprovalDraftOutcome(
                packId,
                approvalDraft(packId, fixture, draftReady, draftEntries, sortedDiagnostics),
                hashReview(packId, draftReady, hashReviews, sortedDiagnostics),
                sortedDiagnostics
        );
    }

    private static Map<String, Object> approvalDraft(
            String packId,
            Path fixture,
            boolean draftReady,
            List<Map<String, Object>> draftEntries,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m17_runtime_fixture_approval_draft", diagnostics);
        data.put("approvalFile", fixture.resolve("runtime-fixture-approvals.json").toString().replace('\\', '/'));
        data.put("approvalFileCreated", false);
        data.put("approvalFileMutated", false);
        data.put("approvalDraft", Map.of(
                "schema", "echo.native.runtime_fixture_approvals.v1",
                "packId", packId,
                "downloadsAllowed", false,
                "extractionAllowed", false,
                "filesystemMutated", false,
                "approvals", draftEntries
        ));
        data.put("draftEntryCount", draftEntries.size());
        data.put("draftReadyForHumanReview", draftReady);
        data.put("hashComputedCount", draftEntries.stream().filter(entry -> Boolean.TRUE.equals(entry.get("reviewReady"))).count());
        data.put("packId", packId);
        data.put("phase13M17LaunchBlocked", true);
        data.put("summary", draftReady
                ? "Runtime fixture approval draft hash evidence is ready for human review; no approval file was written."
                : "Runtime fixture approval draft remains pending supplied local runtime files.");
        return data;
    }

    private static Map<String, Object> hashReview(
            String packId,
            boolean draftReady,
            List<Map<String, Object>> hashReviews,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m17_runtime_fixture_hash_review", diagnostics);
        data.put("hashAlgorithm", "SHA-256");
        data.put("hashComputedCount", hashReviews.stream().filter(entry -> Boolean.TRUE.equals(entry.get("hashComputed"))).count());
        data.put("hashReviewItems", hashReviews);
        data.put("hashReviewItemCount", hashReviews.size());
        data.put("packId", packId);
        data.put("reviewReady", draftReady);
        data.put("summary", draftReady
                ? "Runtime fixture hash review evidence is ready."
                : "Runtime fixture hash review is waiting on local runtime files.");
        return data;
    }

    private static List<Map<String, Object>> handoffItems(Map<String, Object> handoff) {
        Map<String, Object> data = EchoNativeJson.asObject(handoff.get("data"));
        Object rawItems = data.get("handoffItems");
        if (!(rawItems instanceof List<?> list)) {
            return List.of();
        }
        List<Map<String, Object>> items = new ArrayList<>();
        for (Object raw : list) {
            Map<String, Object> item = EchoNativeJson.asObject(raw);
            if (!String.valueOf(item.getOrDefault("artifactId", "")).isBlank()) {
                items.add(item);
            }
        }
        items.sort(Comparator.comparing(item -> String.valueOf(item.get("artifactId"))));
        return List.copyOf(items);
    }

    private static Map<String, Object> readRequiredReport(
            Path reportPath,
            String packId,
            String reportName,
            List<EchoNativeDiagnostic> diagnostics
    ) throws IOException {
        if (reportPath == null || !Files.isRegularFile(reportPath)) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-RUNTIME-FIXTURE-APPROVAL-DRAFT-REPORT-MISSING",
                    EchoNativeIssueSeverity.ERROR,
                    "Runtime fixture approval draft required report missing",
                    "Runtime fixture approval draft planning requires " + reportName + ".",
                    null,
                    packId,
                    reportPath == null ? List.of() : List.of(relativeReportPath(reportPath)),
                    "Run phase13 prepare runtime-fixture-handoff before drafting approval evidence."
            ));
            return Map.of();
        }
        return EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(reportPath)));
    }

    private static Map<String, Object> baseItem(String artifactId, String artifactKind, String expectedFixturePath) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("artifactId", artifactId);
        item.put("artifactKind", artifactKind);
        item.put("classloaderCreated", false);
        item.put("commandExecuted", false);
        item.put("downloadsAllowed", false);
        item.put("expectedFixturePath", expectedFixturePath);
        item.put("extractionAllowed", false);
        item.put("filesystemMutated", false);
        item.put("gameClassesResolved", false);
        item.put("nativeExtractionStarted", false);
        item.put("processLaunched", false);
        item.put("registryInjected", false);
        item.put("registryMutated", false);
        item.put("safeToAutoPopulate", false);
        return item;
    }

    private static Map<String, Object> base(String phase, List<EchoNativeDiagnostic> diagnostics) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("approvalFileCreated", false);
        data.put("approvalFileMutated", false);
        data.put("bytecodeMutated", false);
        data.put("cacheMutated", false);
        data.put("classloaderCreated", false);
        data.put("commandExecuted", false);
        data.put("diagnosticCount", diagnostics.size());
        data.put("diagnosticsCaptured", true);
        data.put("downloadAllowed", false);
        data.put("downloadsAllowed", false);
        data.put("dryRunOnly", true);
        data.put("filesystemMutated", false);
        data.put("gameClassesResolved", false);
        data.put("gameProcessLaunched", false);
        data.put("libraryDownloadStarted", false);
        data.put("minecraftLaunched", false);
        data.put("nativeExtractionStarted", false);
        data.put("phase", phase);
        data.put("processLaunched", false);
        data.put("registryInjected", false);
        data.put("registryMutated", false);
        data.put("reportOnly", true);
        data.put("safeToAutoPopulate", false);
        data.put("transformsEnabled", false);
        data.put("transformsPerformed", false);
        data.put("unsafeRuntimeWorkStarted", false);
        return data;
    }

    private static String sha256(Path path) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream input = Files.newInputStream(path)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = input.read(buffer)) != -1) {
                    digest.update(buffer, 0, read);
                }
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 digest is unavailable", ex);
        }
    }

    private static String relativeReportPath(Path path) {
        Path root = Path.of("").toAbsolutePath().normalize();
        Path normalized = path.toAbsolutePath().normalize();
        if (normalized.startsWith(root)) {
            return root.relativize(normalized).toString().replace('\\', '/');
        }
        return path.toString().replace('\\', '/');
    }
}
