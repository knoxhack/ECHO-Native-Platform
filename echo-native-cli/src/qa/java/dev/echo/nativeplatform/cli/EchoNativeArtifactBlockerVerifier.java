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

final class EchoNativeArtifactBlockerVerifier {
    EchoNativeArtifactBlockerOutcome verify(
            String packId,
            Path fixture,
            Map<String, Path> requiredReports
    ) throws IOException {
        List<EchoNativeDiagnostic> diagnostics = new ArrayList<>();
        Map<String, Map<String, Object>> reports = new LinkedHashMap<>();
        for (Map.Entry<String, Path> entry : requiredReports.entrySet()) {
            Map<String, Object> report = readRequiredReport(entry.getValue(), fixture, packId, entry.getKey(), diagnostics);
            reports.put(entry.getKey(), report);
            checkReport(entry.getKey(), report, entry.getValue(), packId, diagnostics);
        }

        Map<String, Object> artifactMap = reports.getOrDefault("local-runtime-artifact-map.json", Map.of());
        Map<String, Object> artifactData = EchoNativeJson.asObject(artifactMap.get("data"));
        List<Map<String, Object>> blockers = blockers(artifactData);
        int plannedCount = number(artifactData.get("plannedArtifactCount"));
        int mappedCount = number(artifactData.get("mappedArtifactCount"));
        int missingCount = blockers.size();
        boolean artifactMappingReady = Boolean.TRUE.equals(artifactData.get("artifactMappingReady"));
        boolean phase13M17Documented = diagnostics.isEmpty();
        boolean phase13M18Ready = phase13M17Documented
                && artifactMappingReady
                && missingCount == 0
                && "PASS".equals(reports.getOrDefault("phase13-m17-launch-status.json", Map.of()).get("status"));

        List<Map<String, Object>> actions = resolutionActions(blockers);
        List<String> completedChecks = phase13M17Documented ? List.of(
                "m17_readiness_report_present",
                "artifact_inventory_report_present",
                "artifact_map_report_present",
                "launch_attempt_report_present",
                "missing_artifacts_documented",
                "unsafe_runtime_work_not_started"
        ) : List.of();

        return new EchoNativeArtifactBlockerOutcome(
                packId,
                artifactBlockers(packId, plannedCount, mappedCount, missingCount, phase13M17Documented, blockers, diagnostics),
                blockerResolutionPlan(packId, phase13M17Documented, actions, diagnostics),
                m18Readiness(packId, phase13M18Ready, phase13M17Documented, artifactMappingReady, missingCount, completedChecks, diagnostics),
                diagnostics.stream()
                        .sorted(Comparator.comparing(EchoNativeDiagnostic::code)
                                .thenComparing(diagnostic -> diagnostic.moduleId() == null ? "" : diagnostic.moduleId())
                                .thenComparing(EchoNativeDiagnostic::summary))
                        .toList()
        );
    }

    private static List<Map<String, Object>> blockers(Map<String, Object> artifactData) {
        Object rawArtifacts = artifactData.get("artifacts");
        if (!(rawArtifacts instanceof List<?> artifacts)) {
            return List.of();
        }
        List<Map<String, Object>> blockers = new ArrayList<>();
        for (Object raw : artifacts) {
            Map<String, Object> artifact = EchoNativeJson.asObject(raw);
            if (Boolean.TRUE.equals(artifact.get("local")) || "local-file".equals(artifact.get("classification"))) {
                continue;
            }
            String id = String.valueOf(artifact.getOrDefault("id", "unknown"));
            Map<String, Object> blocker = new LinkedHashMap<>();
            blocker.put("artifactId", id);
            blocker.put("blockerType", blockerType(id));
            blocker.put("classification", String.valueOf(artifact.getOrDefault("classification", "unknown")));
            blocker.put("downloadAllowed", false);
            blocker.put("extractionAllowed", false);
            blocker.put("filesystemMutated", false);
            blocker.put("localPath", String.valueOf(artifact.getOrDefault("localPath", "")));
            blocker.put("plannedPath", String.valueOf(artifact.getOrDefault("plannedPath", "")));
            blocker.put("requiresHumanReview", true);
            blocker.put("resolutionPolicy", resolutionPolicy(id));
            blocker.put("runtimeFixtureContract", runtimeFixtureContract(id));
            blocker.put("runtimeResolved", false);
            blocker.put("sourceReport", String.valueOf(artifact.getOrDefault("sourceReport", "")));
            blockers.add(blocker);
        }
        blockers.sort(Comparator.comparing(item -> String.valueOf(item.get("artifactId"))));
        return List.copyOf(blockers);
    }

    private static List<Map<String, Object>> resolutionActions(List<Map<String, Object>> blockers) {
        List<Map<String, Object>> actions = new ArrayList<>();
        int order = 0;
        for (Map<String, Object> blocker : blockers) {
            String id = String.valueOf(blocker.get("artifactId"));
            Map<String, Object> action = new LinkedHashMap<>();
            action.put("artifactId", id);
            action.put("actionId", "phase13.m17.resolve." + safeId(id));
            action.put("actionOrder", order++);
            action.put("actionType", actionType(id));
            action.put("downloadAllowed", false);
            action.put("extractionAllowed", false);
            action.put("filesystemMutationAllowed", false);
            action.put("manualReviewRequired", true);
            action.put("recommendedNextStep", recommendedNextStep(id));
            action.put("runtimeFixtureContract", runtimeFixtureContract(id));
            actions.add(action);
        }
        return List.copyOf(actions);
    }

    private static String blockerType(String artifactId) {
        if (artifactId.startsWith("native:")) {
            return "native_extraction_blocked";
        }
        if ("classpath:minecraft-client-coordinate".equals(artifactId)) {
            return "minecraft_runtime_coordinate_unresolved";
        }
        if ("classpath:echo-native-bootstrap-api".equals(artifactId)) {
            return "native_bootstrap_api_artifact_missing";
        }
        if (artifactId.startsWith("classpath:echo")) {
            return "echo_module_artifact_missing";
        }
        return "runtime_artifact_missing";
    }

    private static String resolutionPolicy(String artifactId) {
        if (artifactId.startsWith("native:")) {
            return "Requires a future explicitly approved local native artifact fixture; this gate must not extract natives.";
        }
        if ("classpath:minecraft-client-coordinate".equals(artifactId)) {
            return "Requires a future explicitly approved local Minecraft client artifact fixture; this gate must not download runtime libraries.";
        }
        if ("classpath:echo-native-bootstrap-api".equals(artifactId)) {
            return "Add or build a repo-local native bootstrap API jar, then review and map it in runtime-artifacts.json.";
        }
        if (artifactId.startsWith("classpath:echo")) {
            return "Produce a repo-local module jar through normal build outputs, then review and map it in runtime-artifacts.json.";
        }
        return "Add a reviewed repo-local mapping only after the artifact source is understood.";
    }

    private static Map<String, Object> runtimeFixtureContract(String artifactId) {
        if ("classpath:minecraft-client-coordinate".equals(artifactId)) {
            return fixtureContract(
                    artifactId,
                    "minecraft_client_jar",
                    "local-runtime/minecraft/26.1.2/client/minecraft-client-26.1.2.jar",
                    "minecraft-runtime-fixture",
                    false,
                    "Copy an already-authorized local Minecraft client jar into the fixture path outside the native CLI, then add the approved mapping. The native CLI must not download it."
            );
        }
        if ("native:minecraft-26.1.2-natives".equals(artifactId)) {
            return fixtureContract(
                    artifactId,
                    "minecraft_native_archive",
                    "local-runtime/minecraft/26.1.2/natives/minecraft-26.1.2-natives.zip",
                    "minecraft-native-fixture",
                    false,
                    "Copy an already-authorized local native archive into the fixture path outside the native CLI, then add the approved mapping. The native CLI must not extract it."
            );
        }
        return Map.of();
    }

    private static Map<String, Object> fixtureContract(
            String artifactId,
            String artifactKind,
            String expectedLocalPath,
            String mappingSource,
            boolean extractionAllowed,
            String operatorAction
    ) {
        Map<String, Object> contract = new LinkedHashMap<>();
        contract.put("artifactKind", artifactKind);
        contract.put("expectedFixturePath", expectedLocalPath);
        contract.put("operatorAction", operatorAction);
        contract.put("requiredRuntimeArtifactsJsonEntry", runtimeArtifactEntry(artifactId, expectedLocalPath, mappingSource, extractionAllowed));
        contract.put("reviewRequirements", List.of(
                "reviewed=true",
                "approved=true",
                "reviewStatus=approved",
                "downloadsAllowed=false",
                "extractionAllowed=" + extractionAllowed,
                "localPath is fixture-relative",
                "localPath does not point to a user home, launcher cache, .minecraft directory, or Gradle cache"
        ));
        contract.put("safeToAutoPopulate", false);
        return contract;
    }

    private static Map<String, Object> runtimeArtifactEntry(
            String artifactId,
            String localPath,
            String source,
            boolean extractionAllowed
    ) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("id", artifactId);
        entry.put("localPath", localPath);
        entry.put("source", source);
        entry.put("reviewed", true);
        entry.put("approved", true);
        entry.put("reviewStatus", "approved");
        entry.put("downloadsAllowed", false);
        entry.put("extractionAllowed", extractionAllowed);
        return entry;
    }

    private static String actionType(String artifactId) {
        if (artifactId.startsWith("native:")) {
            return "document_native_fixture_requirement";
        }
        if ("classpath:minecraft-client-coordinate".equals(artifactId)) {
            return "document_minecraft_runtime_fixture_requirement";
        }
        return "resolve_repo_local_artifact_mapping";
    }

    private static String recommendedNextStep(String artifactId) {
        if (artifactId.startsWith("native:")) {
            return "Keep M17 launch blocked until a reviewed local native fixture exists; do not extract natives during this phase.";
        }
        if ("classpath:minecraft-client-coordinate".equals(artifactId)) {
            return "Keep M17 launch blocked until a reviewed local Minecraft client fixture exists; do not download it from the native CLI.";
        }
        return "Inspect the module build output and add a reviewed runtime-artifacts.json mapping only if a safe repo-local jar exists.";
    }

    private static void checkReport(
            String reportName,
            Map<String, Object> report,
            Path path,
            String packId,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        if (report.isEmpty()) {
            return;
        }
        Map<String, Object> data = EchoNativeJson.asObject(report.get("data"));
        if ("phase13-m17-readiness.json".equals(reportName) && !"PASS".equals(report.get("status"))) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-M17-BLOCKER-GATE-UPSTREAM-FAILED",
                    EchoNativeIssueSeverity.ERROR,
                    "M17 artifact blocker gate upstream readiness is not PASS",
                    "Artifact blocker documentation requires PASS phase13-m17-readiness.json.",
                    null,
                    packId,
                    List.of(relativeReportPath(path)),
                    "Regenerate and resolve Phase 13 M17 preflight before documenting remaining artifact blockers."
            ));
        }
        if (hasUnsafeRuntimeWork(data)) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-M17-BLOCKER-GATE-SAFETY-VIOLATION",
                    EchoNativeIssueSeverity.ERROR,
                    "M17 artifact blocker gate input contains unsafe runtime work",
                    reportName + " indicates work that is not allowed while documenting M17 blockers.",
                    null,
                    packId,
                    List.of(relativeReportPath(path)),
                    "Keep the blocker gate report-only: no launch, command execution, downloads, extraction, classloader, transforms, registry injection, or mutation."
            ));
        }
    }

    private static Map<String, Object> artifactBlockers(
            String packId,
            int plannedCount,
            int mappedCount,
            int missingCount,
            boolean documented,
            List<Map<String, Object>> blockers,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m17_artifact_blockers", diagnostics);
        data.put("artifactBlockerCount", missingCount);
        data.put("artifactBlockersDocumented", documented);
        data.put("blockers", blockers);
        data.put("mappedArtifactCount", mappedCount);
        data.put("packId", packId);
        data.put("phase13M17LaunchBlocked", missingCount > 0);
        data.put("plannedArtifactCount", plannedCount);
        data.put("summary", missingCount == 0
                ? "No remaining M17 local runtime artifact blockers are present."
                : "M17 remains blocked by documented local runtime artifact gaps.");
        return data;
    }

    private static Map<String, Object> blockerResolutionPlan(
            String packId,
            boolean documented,
            List<Map<String, Object>> actions,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m17_blocker_resolution_plan", diagnostics);
        data.put("actionCount", actions.size());
        data.put("actions", actions);
        data.put("blockerResolutionDocumented", documented);
        data.put("downloadsAllowed", false);
        data.put("extractionAllowed", false);
        data.put("packId", packId);
        data.put("summary", actions.isEmpty()
                ? "No M17 artifact resolution actions are needed."
                : "M17 artifact resolution requires reviewed repo-local mappings or future approved runtime fixtures.");
        return data;
    }

    private static Map<String, Object> m18Readiness(
            String packId,
            boolean m18Ready,
            boolean documented,
            boolean artifactMappingReady,
            int missingCount,
            List<String> completedChecks,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m18_readiness", diagnostics);
        data.put("artifactBlockersDocumented", documented);
        data.put("artifactMappingReady", artifactMappingReady);
        data.put("completedChecks", completedChecks);
        data.put("missingArtifactCount", missingCount);
        data.put("packId", packId);
        data.put("phase13M18Ready", m18Ready);
        data.put("playtestCandidateReady", false);
        data.put("summary", m18Ready
                ? "M17 artifact and launch gates are ready for the Ashfall native smoke session."
                : "M18 remains blocked until M17 launch artifact and controlled launch gates pass.");
        return data;
    }

    private static Map<String, Object> base(String phase, List<EchoNativeDiagnostic> diagnostics) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("bytecodeMutated", false);
        data.put("cacheMutated", false);
        data.put("classloaderCreated", false);
        data.put("commandExecuted", false);
        data.put("diagnosticCount", diagnostics.size());
        data.put("diagnosticsCaptured", true);
        data.put("downloadAllowed", false);
        data.put("dryRunOnly", true);
        data.put("filesystemMutated", false);
        data.put("gameClassesResolved", false);
        data.put("gameProcessLaunched", false);
        data.put("libraryDownloadStarted", false);
        data.put("minecraftLaunched", false);
        data.put("nativeExtractionStarted", false);
        data.put("processLaunched", false);
        data.put("registryInjected", false);
        data.put("registryMutated", false);
        data.put("reportOnly", true);
        data.put("phase", phase);
        data.put("transformsEnabled", false);
        data.put("transformsPerformed", false);
        data.put("unsafeRuntimeWorkStarted", false);
        return data;
    }

    private static Map<String, Object> readRequiredReport(
            Path reportPath,
            Path fixture,
            String packId,
            String reportName,
            List<EchoNativeDiagnostic> diagnostics
    ) throws IOException {
        if (!Files.isRegularFile(reportPath)) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-M17-BLOCKER-GATE-REPORT-MISSING",
                    EchoNativeIssueSeverity.ERROR,
                    "M17 artifact blocker gate required report missing",
                    "Artifact blocker documentation requires " + reportName + ".",
                    null,
                    packId,
                    List.of(fixture.toString().replace('\\', '/')),
                    "Run the M17 preflight, inventory, mapping, and isolated launch attempt reports before documenting blockers."
            ));
            return Map.of();
        }
        return EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(reportPath)));
    }

    private static int number(Object value) {
        return value instanceof Number number ? number.intValue() : 0;
    }

    private static String safeId(String artifactId) {
        return artifactId.replace(':', '.').replace('/', '.').replace('_', '-');
    }

    private static boolean hasUnsafeRuntimeWork(Map<String, Object> data) {
        return Boolean.TRUE.equals(data.get("remoteManifestDownloaded"))
                || Boolean.TRUE.equals(data.get("libraryDownloadStarted"))
                || Boolean.TRUE.equals(data.get("cacheMutated"))
                || Boolean.TRUE.equals(data.get("nativeExtractionStarted"))
                || Boolean.TRUE.equals(data.get("nativeFilesExtracted"))
                || Boolean.TRUE.equals(data.get("processLaunched"))
                || Boolean.TRUE.equals(data.get("gameProcessLaunched"))
                || Boolean.TRUE.equals(data.get("minecraftLaunched"))
                || Boolean.TRUE.equals(data.get("commandExecuted"))
                || Boolean.TRUE.equals(data.get("classloaderCreated"))
                || Boolean.TRUE.equals(data.get("productionClassloader"))
                || Boolean.TRUE.equals(data.get("resolvesRuntimeClasses"))
                || Boolean.TRUE.equals(data.get("gameClassesResolved"))
                || Boolean.TRUE.equals(data.get("minecraftClassesResolved"))
                || Boolean.TRUE.equals(data.get("addonCodeExecuted"))
                || Boolean.TRUE.equals(data.get("realAddonCodeExecuted"))
                || Boolean.TRUE.equals(data.get("serviceCodeExecuted"))
                || Boolean.TRUE.equals(data.get("registryInjected"))
                || Boolean.TRUE.equals(data.get("registryMutated"))
                || Boolean.TRUE.equals(data.get("transformsEnabled"))
                || Boolean.TRUE.equals(data.get("transformsPerformed"))
                || Boolean.TRUE.equals(data.get("filesystemMutated"))
                || Boolean.TRUE.equals(data.get("unsafeRuntimeWorkStarted"));
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
