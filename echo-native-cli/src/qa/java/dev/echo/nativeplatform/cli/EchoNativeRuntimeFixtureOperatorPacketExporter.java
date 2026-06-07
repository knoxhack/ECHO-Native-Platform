package dev.echo.nativeplatform.cli;

import dev.echo.nativeplatform.contracts.EchoNativeDiagnostic;
import dev.echo.nativeplatform.contracts.EchoNativeIssueSeverity;
import dev.echo.nativeplatform.diagnostics.EchoNativeJson;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class EchoNativeRuntimeFixtureOperatorPacketExporter {
    EchoNativeRuntimeFixtureOperatorPacketOutcome export(
            String packId,
            Path fixture,
            Map<String, Path> requiredReports
    ) throws IOException {
        List<EchoNativeDiagnostic> diagnostics = new ArrayList<>();
        Map<String, Object> handoff = readRequiredReport(requiredReports.get("runtime-fixture-handoff.json"), packId, "runtime-fixture-handoff.json", diagnostics);
        Map<String, Object> runbook = readRequiredReport(requiredReports.get("runtime-fixture-validation-runbook.json"), packId, "runtime-fixture-validation-runbook.json", diagnostics);
        Map<String, Object> approvalDraft = readRequiredReport(requiredReports.get("runtime-fixture-approval-draft.json"), packId, "runtime-fixture-approval-draft.json", diagnostics);
        Map<String, Object> hashReview = readRequiredReport(requiredReports.get("runtime-fixture-hash-review.json"), packId, "runtime-fixture-hash-review.json", diagnostics);
        Map<String, Object> m17Completion = readRequiredReport(requiredReports.get("phase13-m17-completion.json"), packId, "phase13-m17-completion.json", diagnostics);
        Map<String, Object> fullRoadmap = readRequiredReport(requiredReports.get("phase13-first-playtest-full-roadmap.json"), packId, "phase13-first-playtest-full-roadmap.json", diagnostics);

        List<Map<String, Object>> handoffItems = listFromData(handoff, "handoffItems");
        List<Object> requiredFiles = rawList(EchoNativeJson.asObject(runbook.get("data")).get("requiredFiles"));
        List<Object> commands = rawList(EchoNativeJson.asObject(runbook.get("data")).get("requiredCommands"));
        List<Object> artifactEntries = rawList(EchoNativeJson.asObject(runbook.get("data")).get("requiredRuntimeArtifactsJsonEntries"));
        List<Object> approvalEntries = rawList(EchoNativeJson.asObject(runbook.get("data")).get("requiredRuntimeFixtureApprovalEntries"));
        List<Object> approvalWorkflow = rawList(EchoNativeJson.asObject(runbook.get("data")).get("approvalEvidenceWorkflow"));
        List<Object> reviewChecklist = rawList(EchoNativeJson.asObject(runbook.get("data")).get("reviewChecklist"));
        Map<String, Object> approvalDraftData = EchoNativeJson.asObject(approvalDraft.get("data"));
        Map<String, Object> hashReviewData = EchoNativeJson.asObject(hashReview.get("data"));
        Map<String, Object> m17Data = EchoNativeJson.asObject(m17Completion.get("data"));
        Map<String, Object> roadmapData = EchoNativeJson.asObject(fullRoadmap.get("data"));

        if (handoffItems.isEmpty()) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-RUNTIME-FIXTURE-OPERATOR-PACKET-HANDOFF-MISSING",
                    EchoNativeIssueSeverity.ERROR,
                    "Runtime fixture operator packet handoff is missing",
                    "The operator packet needs runtime fixture handoff items before it can describe M17 intake.",
                    null,
                    packId,
                    List.of(relativeReportPath(requiredReports.get("runtime-fixture-handoff.json"))),
                    "Run phase13 prepare runtime-fixture-handoff before exporting the operator packet."
            ));
        }
        if (requiredFiles.isEmpty()) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-RUNTIME-FIXTURE-OPERATOR-PACKET-FILES-MISSING",
                    EchoNativeIssueSeverity.ERROR,
                    "Runtime fixture operator packet required files are missing",
                    "The operator packet needs required fixture paths from runtime-fixture-validation-runbook.json.",
                    null,
                    packId,
                    List.of(relativeReportPath(requiredReports.get("runtime-fixture-validation-runbook.json"))),
                    "Regenerate the runtime fixture validation runbook."
            ));
        }

        long pendingFileCount = handoffItems.stream()
                .filter(item -> !Boolean.TRUE.equals(item.get("handoffComplete")))
                .count();
        if (pendingFileCount > 0 && !handoffItems.isEmpty()) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-RUNTIME-FIXTURE-OPERATOR-PACKET-PENDING",
                    EchoNativeIssueSeverity.WARNING,
                    "Runtime fixture operator packet is pending local artifacts",
                    pendingFileCount + " runtime fixture handoff items still need operator-supplied local files and reviewed approval evidence.",
                    null,
                    packId,
                    handoffItems.stream()
                            .map(item -> fixture.resolve(String.valueOf(item.getOrDefault("expectedFixturePath", ""))).normalize().toString().replace('\\', '/'))
                            .toList(),
                    "Supply already-authorized local runtime artifacts outside the CLI, then rerun the M17 approval and integrity chain."
            ));
        }

        List<EchoNativeDiagnostic> sortedDiagnostics = diagnostics.stream()
                .sorted(Comparator.comparing(EchoNativeDiagnostic::code)
                        .thenComparing(diagnostic -> diagnostic.moduleId() == null ? "" : diagnostic.moduleId())
                        .thenComparing(EchoNativeDiagnostic::summary))
                .toList();

        return new EchoNativeRuntimeFixtureOperatorPacketOutcome(
                packId,
                operatorPacket(packId, fixture, handoffItems, requiredFiles, commands, artifactEntries, approvalEntries,
                        approvalWorkflow, reviewChecklist, approvalDraftData, hashReviewData, m17Data, roadmapData, sortedDiagnostics),
                sortedDiagnostics
        );
    }

    private static Map<String, Object> operatorPacket(
            String packId,
            Path fixture,
            List<Map<String, Object>> handoffItems,
            List<Object> requiredFiles,
            List<Object> commands,
            List<Object> artifactEntries,
            List<Object> approvalEntries,
            List<Object> approvalWorkflow,
            List<Object> reviewChecklist,
            Map<String, Object> approvalDraftData,
            Map<String, Object> hashReviewData,
            Map<String, Object> m17Data,
            Map<String, Object> roadmapData,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        boolean packetReady = !handoffItems.isEmpty() && !requiredFiles.isEmpty();
        Map<String, Object> data = base("phase13_m17_runtime_fixture_operator_packet", diagnostics);
        data.put("approvalDraft", approvalDraftData.getOrDefault("approvalDraft", Map.of()));
        data.put("approvalDraftEntryCount", approvalDraftData.getOrDefault("draftEntryCount", 0));
        data.put("approvalEvidenceWorkflow", approvalWorkflow);
        data.put("approvalFile", fixture.resolve("runtime-fixture-approvals.json").toString().replace('\\', '/'));
        data.put("blockerCount", m17Data.getOrDefault("blockedReportCount", 0));
        data.put("firstIncompleteMilestone", roadmapData.getOrDefault("firstIncompleteMilestone", ""));
        data.put("firstPlaytestOpen", false);
        data.put("handoffItemCount", handoffItems.size());
        data.put("handoffItems", handoffItems);
        data.put("hashComputedCount", hashReviewData.getOrDefault("hashComputedCount", 0));
        data.put("hashReviewItems", hashReviewData.getOrDefault("hashReviewItems", List.of()));
        data.put("operatorPacketReady", packetReady);
        data.put("packId", packId);
        data.put("phase13M17Complete", Boolean.TRUE.equals(m17Data.get("phase13M17Complete")));
        data.put("phase13M17LaunchBlocked", true);
        data.put("requiredApprovalEntries", approvalEntries);
        data.put("requiredApprovalEntryCount", approvalEntries.size());
        data.put("requiredCommands", commands);
        data.put("requiredFiles", requiredFiles);
        data.put("requiredFileCount", requiredFiles.size());
        data.put("requiredRuntimeArtifactsJsonEntries", artifactEntries);
        data.put("requiredRuntimeArtifactsJsonEntryCount", artifactEntries.size());
        data.put("reviewChecklist", reviewChecklist);
        data.put("runtimeFilesReady", !handoffItems.isEmpty()
                && handoffItems.stream().allMatch(item -> Boolean.TRUE.equals(item.get("handoffComplete"))));
        data.put("summary", packetReady
                ? "M17 runtime fixture operator packet is ready for manual artifact intake; first playtest remains closed."
                : "M17 runtime fixture operator packet is incomplete; regenerate upstream M17 reports.");
        return data;
    }

    private static List<Map<String, Object>> listFromData(Map<String, Object> report, String key) {
        Object raw = EchoNativeJson.asObject(report.get("data")).get(key);
        if (!(raw instanceof List<?> list)) {
            return List.of();
        }
        List<Map<String, Object>> items = new ArrayList<>();
        for (Object item : list) {
            Map<String, Object> map = EchoNativeJson.asObject(item);
            if (!map.isEmpty()) {
                items.add(map);
            }
        }
        items.sort(Comparator.comparing(item -> String.valueOf(item.getOrDefault("artifactId", ""))));
        return List.copyOf(items);
    }

    private static List<Object> rawList(Object raw) {
        if (!(raw instanceof List<?> list)) {
            return List.of();
        }
        return List.copyOf(list);
    }

    private static Map<String, Object> readRequiredReport(
            Path reportPath,
            String packId,
            String reportName,
            List<EchoNativeDiagnostic> diagnostics
    ) throws IOException {
        if (reportPath == null || !Files.isRegularFile(reportPath)) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-RUNTIME-FIXTURE-OPERATOR-PACKET-REPORT-MISSING",
                    EchoNativeIssueSeverity.ERROR,
                    "Runtime fixture operator packet required report missing",
                    "Runtime fixture operator packet requires " + reportName + ".",
                    null,
                    packId,
                    reportPath == null ? List.of() : List.of(relativeReportPath(reportPath)),
                    "Regenerate the M17 runtime fixture handoff, approval draft, integrity, closeout, and roadmap reports first."
            ));
            return Map.of();
        }
        return EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(reportPath)));
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

    private static String relativeReportPath(Path path) {
        if (path == null) {
            return "";
        }
        Path root = Path.of("").toAbsolutePath().normalize();
        Path normalized = path.toAbsolutePath().normalize();
        if (normalized.startsWith(root)) {
            return root.relativize(normalized).toString().replace('\\', '/');
        }
        return path.toString().replace('\\', '/');
    }
}
