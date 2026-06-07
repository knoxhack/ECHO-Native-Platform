package dev.echo.nativeplatform.cli;

import dev.echo.nativeplatform.contracts.EchoNativeDiagnostic;
import dev.echo.nativeplatform.contracts.EchoNativeIssueSeverity;
import dev.echo.nativeplatform.contracts.EchoNativeIsolatedLaunchExecutionEligibility;
import dev.echo.nativeplatform.contracts.EchoNativeLaunchArtifactResolutionStatus;
import dev.echo.nativeplatform.contracts.EchoNativeLocalRuntimeArtifactMap;
import dev.echo.nativeplatform.contracts.EchoNativePhase13M17ArtifactReadiness;
import dev.echo.nativeplatform.diagnostics.EchoNativeJson;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class EchoNativeArtifactMapper {
    EchoNativeArtifactMappingOutcome map(
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

        Path manifestPath = fixture.resolve("runtime-artifacts.json").normalize();
        Map<String, RuntimeArtifactMapping> explicitMappings = readExplicitMappings(manifestPath, fixture, packId, diagnostics);
        List<Map<String, Object>> artifacts = artifactMappings(
                fixture,
                reports.getOrDefault("classpath-builder-plan.json", Map.of()),
                reports.getOrDefault("native-extraction-plan.json", Map.of()),
                explicitMappings
        );
        for (Map<String, Object> artifact : artifacts) {
            if (!"local-file".equals(artifact.get("classification"))) {
                diagnostics.add(new EchoNativeDiagnostic(
                        "ECHO-NATIVE-M17-ARTIFACT-NOT-LOCAL",
                        EchoNativeIssueSeverity.ERROR,
                        "Local runtime artifact is not mapped to a safe local file",
                        artifact.get("id") + " is classified as " + artifact.get("classification") + " and cannot be used for an isolated Minecraft launch.",
                        null,
                        packId,
                        List.of(String.valueOf(artifact.get("sourceReport"))),
                        "Add an explicit fixture-local runtime-artifacts.json mapping to a checked-in or otherwise approved local test artifact, or keep M17 as a controlled failure."
                ));
            }
        }

        diagnostics = unique(diagnostics);
        boolean upstreamReady = diagnostics.stream().noneMatch(diagnostic ->
                "ECHO-NATIVE-M17-UPSTREAM-REPORT-BLOCKED".equals(diagnostic.code())
                        || "ECHO-NATIVE-M17-SAFETY-VIOLATION".equals(diagnostic.code())
                        || "ECHO-NATIVE-M17-REPORT-MISSING".equals(diagnostic.code())
                        || "ECHO-NATIVE-M17-RUNTIME-ARTIFACT-MANIFEST-INVALID".equals(diagnostic.code()));
        int mappedCount = (int) artifacts.stream().filter(artifact -> "local-file".equals(artifact.get("classification"))).count();
        int missingCount = artifacts.size() - mappedCount;
        boolean localArtifactsReady = upstreamReady && !artifacts.isEmpty() && missingCount == 0;
        List<String> completedChecks = localArtifactsReady ? List.of(
                "upstream_m17_reports_pass",
                "artifact_manifest_loaded",
                "classpath_artifacts_mapped",
                "native_artifacts_mapped",
                "no_downloads_or_extraction",
                "no_launch_or_classloader"
        ) : List.of();

        EchoNativeLocalRuntimeArtifactMap artifactMap = new EchoNativeLocalRuntimeArtifactMap(
                "phase13.m17.local_runtime_artifact.map",
                localArtifactsReady,
                Files.isRegularFile(manifestPath),
                false,
                false,
                false,
                artifacts.size(),
                mappedCount,
                missingCount,
                relativeFixturePath(fixture, manifestPath),
                artifacts
        );
        EchoNativeLaunchArtifactResolutionStatus resolutionStatus = new EchoNativeLaunchArtifactResolutionStatus(
                "phase13.m17.launch_artifact_resolution.status",
                localArtifactsReady,
                true,
                false,
                false,
                false,
                false,
                mappedCount,
                missingCount,
                completedChecks
        );
        EchoNativeIsolatedLaunchExecutionEligibility eligibility = new EchoNativeIsolatedLaunchExecutionEligibility(
                "phase13.m17.isolated_launch_execution.eligibility",
                localArtifactsReady,
                upstreamReady,
                localArtifactsReady,
                true,
                false,
                false,
                false,
                false,
                requiredReports.keySet().stream().sorted().toList()
        );
        EchoNativePhase13M17ArtifactReadiness readiness = new EchoNativePhase13M17ArtifactReadiness(
                "phase13.m17.artifact.readiness",
                localArtifactsReady,
                localArtifactsReady,
                localArtifactsReady,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                completedChecks
        );

        return new EchoNativeArtifactMappingOutcome(
                packId,
                localRuntimeArtifactMap(packId, artifactMap, diagnostics),
                launchArtifactResolutionStatus(packId, resolutionStatus, diagnostics),
                isolatedLaunchExecutionEligibility(packId, eligibility, diagnostics),
                phase13M17ArtifactReadiness(packId, readiness, diagnostics),
                diagnostics
        );
    }

    private static Map<String, RuntimeArtifactMapping> readExplicitMappings(
            Path manifestPath,
            Path fixture,
            String packId,
            List<EchoNativeDiagnostic> diagnostics
    ) throws IOException {
        if (!Files.isRegularFile(manifestPath)) {
            return Map.of();
        }
        try {
            Map<String, Object> manifest = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(manifestPath)));
            Object entries = manifest.get("artifacts");
            if (!(entries instanceof List<?> list)) {
                return Map.of();
            }
            Map<String, RuntimeArtifactMapping> mappings = new LinkedHashMap<>();
            for (Object raw : list) {
                Map<String, Object> item = EchoNativeJson.asObject(raw);
                String id = String.valueOf(item.getOrDefault("id", ""));
                String localPath = String.valueOf(item.getOrDefault("localPath", ""));
                String source = String.valueOf(item.getOrDefault("source", ""));
                boolean reviewed = Boolean.TRUE.equals(item.get("reviewed"));
                boolean approved = Boolean.TRUE.equals(item.get("approved"));
                boolean downloadsAllowed = Boolean.TRUE.equals(item.get("downloadsAllowed"));
                boolean extractionAllowed = Boolean.TRUE.equals(item.get("extractionAllowed"));
                String reviewStatus = String.valueOf(item.getOrDefault("reviewStatus", ""));
                if (!id.isBlank() && !localPath.isBlank()) {
                    Path resolved = fixture.resolve(localPath).normalize();
                    if (reviewed
                            && approved
                            && "approved".equals(reviewStatus)
                            && !downloadsAllowed
                            && !extractionAllowed
                            && isApprovedLocalPath(resolved)) {
                        mappings.put(id, new RuntimeArtifactMapping(localPath, source, reviewed, approved, reviewStatus));
                    } else {
                        diagnostics.add(new EchoNativeDiagnostic(
                                "ECHO-NATIVE-M17-RUNTIME-ARTIFACT-MAPPING-NOT-APPROVED",
                                EchoNativeIssueSeverity.ERROR,
                                "Runtime artifact mapping is not an approved local mapping",
                                id + " must be reviewed, approved, non-downloading, non-extracting, relative, and inside an approved repo-local artifact root.",
                                null,
                                packId,
                                List.of(relativeFixturePath(fixture, manifestPath)),
                                "Review the mapping against local-runtime-artifact-inventory.json and keep only approved repo-local artifact paths."
                        ));
                    }
                }
            }
            return Map.copyOf(mappings);
        } catch (RuntimeException ex) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-M17-RUNTIME-ARTIFACT-MANIFEST-INVALID",
                    EchoNativeIssueSeverity.ERROR,
                    "Runtime artifact manifest is invalid",
                    "Could not parse " + relativeFixturePath(fixture, manifestPath) + ": " + ex.getMessage(),
                    null,
                    packId,
                    List.of(relativeFixturePath(fixture, manifestPath)),
                    "Fix runtime-artifacts.json before mapping local launch artifacts."
            ));
            return Map.of();
        }
    }

    private static List<Map<String, Object>> artifactMappings(
            Path fixture,
            Map<String, Object> classpathPlan,
            Map<String, Object> nativeExtractionPlan,
            Map<String, RuntimeArtifactMapping> explicitMappings
    ) {
        List<Map<String, Object>> artifacts = new ArrayList<>();
        Map<String, Object> classpathData = EchoNativeJson.asObject(classpathPlan.get("data"));
        Object entries = classpathData.get("plannedEntries");
        if (entries instanceof List<?> list) {
            for (Object raw : list) {
                Map<String, Object> entry = EchoNativeJson.asObject(raw);
                String id = "classpath:" + String.valueOf(entry.getOrDefault("id", "unknown"));
                artifacts.add(mappedArtifact(fixture, id, String.valueOf(entry.getOrDefault("plannedPath", "")), "classpath-builder-plan.json", explicitMappings));
            }
        }
        Map<String, Object> nativeData = EchoNativeJson.asObject(nativeExtractionPlan.get("data"));
        Object nativeEntries = nativeData.get("plannedNativeEntries");
        if (nativeEntries instanceof List<?> list) {
            for (Object raw : list) {
                Map<String, Object> entry = EchoNativeJson.asObject(raw);
                String id = "native:" + String.valueOf(entry.getOrDefault("id", "unknown"));
                artifacts.add(mappedArtifact(fixture, id, String.valueOf(entry.getOrDefault("plannedExtractionPath", entry.getOrDefault("plannedPath", ""))), "native-extraction-plan.json", explicitMappings));
            }
        }
        artifacts.sort(Comparator.comparing(item -> String.valueOf(item.get("id"))));
        return List.copyOf(artifacts);
    }

    private static Map<String, Object> mappedArtifact(
            Path fixture,
            String id,
            String plannedPath,
            String sourceReport,
            Map<String, RuntimeArtifactMapping> explicitMappings
    ) {
        RuntimeArtifactMapping mapping = explicitMappings.get(id);
        String localPath = mapping == null ? "" : mapping.localPath();
        Path resolved = localPath.isBlank() ? null : fixture.resolve(localPath).normalize();
        boolean localFile = resolved != null && Files.isRegularFile(resolved);
        String classification;
        if (localFile) {
            classification = "local-file";
        } else if (!localPath.isBlank()) {
            classification = "mapped-missing";
        } else if (plannedPath.startsWith("planned://")) {
            classification = "planned-only";
        } else if (plannedPath.isBlank()) {
            classification = "missing";
        } else {
            classification = Files.isRegularFile(Path.of(plannedPath)) ? "local-file" : "unverified-path";
        }

        Map<String, Object> item = new LinkedHashMap<>();
        item.put("classification", classification);
        item.put("downloadAllowed", false);
        item.put("filesystemMutated", false);
        item.put("id", id);
        item.put("local", localFile);
        item.put("localPath", localFile ? relativeFixturePath(fixture, resolved) : localPath);
        item.put("mappingApproved", mapping != null && mapping.approved());
        item.put("mappingReviewed", mapping != null && mapping.reviewed());
        item.put("mappingSource", mapping == null ? "" : mapping.source());
        item.put("reviewStatus", mapping == null ? "" : mapping.reviewStatus());
        item.put("nativeExtractionStarted", false);
        item.put("plannedPath", plannedPath);
        item.put("runtimeResolved", localFile);
        item.put("sourceReport", sourceReport);
        return item;
    }

    private static boolean isApprovedLocalPath(Path path) {
        if (path.isAbsolute() && path.toString().contains(":")) {
            return false;
        }
        Path workspace = Path.of("").toAbsolutePath().normalize();
        Path repoRoot = workspace.getParent() == null ? workspace : workspace.getParent();
        Path normalized = path.toAbsolutePath().normalize();
        String text = normalized.toString().replace('\\', '/').toLowerCase();
        if (text.contains("/.local/")
                || text.contains("/.codex-backups/")
                || text.contains("/quarantine/")
                || text.contains("/.gradle/")
                || text.contains("/users/")) {
            return false;
        }
        return normalized.startsWith(repoRoot.resolve("build").resolve("tmp").resolve("echo-1.1.3-mods").normalize())
                || normalized.startsWith(repoRoot.resolve("build").resolve("tmp").resolve("echo-native-m17-mods").normalize())
                || normalized.startsWith(repoRoot.resolve("addons").normalize())
                || normalized.startsWith(repoRoot.resolve("core").normalize())
                || normalized.startsWith(workspace.normalize());
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
        if (!"PASS".equals(report.get("status"))) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-M17-UPSTREAM-REPORT-BLOCKED",
                    EchoNativeIssueSeverity.ERROR,
                    "Phase 13 M17 artifact mapping upstream report is not PASS",
                    "Artifact mapping requires PASS " + reportName + ".",
                    null,
                    packId,
                    List.of(relativeReportPath(path)),
                    "Regenerate and resolve M17 preflight reports before mapping launch artifacts."
            ));
        }
        if (hasUnsafeRuntimeWork(data)) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-M17-SAFETY-VIOLATION",
                    EchoNativeIssueSeverity.ERROR,
                    "Phase 13 M17 artifact mapping input contains unsafe runtime work",
                    reportName + " indicates runtime work that is not safe for artifact mapping.",
                    null,
                    packId,
                    List.of(relativeReportPath(path)),
                    "Keep artifact mapping report-only: no launch, command execution, downloads, extraction, classloader, transforms, registry injection, or mutation."
            ));
        }
        diagnostics.addAll(reportDiagnostics(report, packId));
    }

    private static Map<String, Object> localRuntimeArtifactMap(
            String packId,
            EchoNativeLocalRuntimeArtifactMap map,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m17_local_runtime_artifact_map", diagnostics);
        data.put("artifactMappingReady", map.artifactMappingReady());
        data.put("artifacts", map.artifacts());
        data.put("downloadsAllowed", map.downloadsAllowed());
        data.put("extractionAllowed", map.extractionAllowed());
        data.put("filesystemMutated", map.filesystemMutated());
        data.put("localArtifactManifestPath", map.localArtifactManifestPath());
        data.put("localArtifactManifestPresent", map.localArtifactManifestPresent());
        data.put("mapId", map.mapId());
        data.put("mappedArtifactCount", map.mappedArtifactCount());
        data.put("missingArtifactCount", map.missingArtifactCount());
        data.put("packId", packId);
        data.put("plannedArtifactCount", map.plannedArtifactCount());
        data.put("summary", map.artifactMappingReady()
                ? "All planned launch artifacts map to verified local files."
                : "Planned launch artifacts are not yet mapped to verified local files.");
        return data;
    }

    private static Map<String, Object> launchArtifactResolutionStatus(
            String packId,
            EchoNativeLaunchArtifactResolutionStatus status,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m17_launch_artifact_resolution_status", diagnostics);
        data.put("artifactsResolved", status.artifactsResolved());
        data.put("completedChecks", status.completedChecks());
        data.put("downloadsAllowed", status.downloadsAllowed());
        data.put("filesystemMutated", status.filesystemMutated());
        data.put("libraryDownloadStarted", status.libraryDownloadStarted());
        data.put("missingArtifactCount", status.missingArtifactCount());
        data.put("missingArtifactsBecomeDiagnostics", status.missingArtifactsBecomeDiagnostics());
        data.put("nativeExtractionStarted", status.nativeExtractionStarted());
        data.put("packId", packId);
        data.put("resolvedArtifactCount", status.resolvedArtifactCount());
        data.put("statusId", status.statusId());
        data.put("summary", status.artifactsResolved()
                ? "Launch artifact resolution is ready for a separately gated isolated launch attempt."
                : "Launch artifact resolution is blocked by missing local artifacts.");
        return data;
    }

    private static Map<String, Object> isolatedLaunchExecutionEligibility(
            String packId,
            EchoNativeIsolatedLaunchExecutionEligibility eligibility,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m17_isolated_launch_execution_eligibility", diagnostics);
        data.put("classloaderCreated", eligibility.classloaderCreated());
        data.put("commandExecuted", eligibility.commandExecuted());
        data.put("eligibleForLaunchAttempt", eligibility.eligibleForLaunchAttempt());
        data.put("filesystemMutated", eligibility.filesystemMutated());
        data.put("gameClassesResolved", eligibility.gameClassesResolved());
        data.put("localArtifactsReady", eligibility.localArtifactsReady());
        data.put("packId", packId);
        data.put("processLaunchStillGated", eligibility.processLaunchStillGated());
        data.put("requiredReports", eligibility.requiredReports());
        data.put("summary", eligibility.eligibleForLaunchAttempt()
                ? "Artifact mapping permits a later isolated launch attempt command to decide whether to launch."
                : "Artifact mapping keeps isolated launch execution in controlled-failure mode.");
        data.put("upstreamSafetyPassed", eligibility.upstreamSafetyPassed());
        data.put("eligibilityId", eligibility.eligibilityId());
        return data;
    }

    private static Map<String, Object> phase13M17ArtifactReadiness(
            String packId,
            EchoNativePhase13M17ArtifactReadiness readiness,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m17_artifact_readiness", diagnostics);
        data.put("classloaderCreated", readiness.classloaderCreated());
        data.put("commandExecuted", readiness.commandExecuted());
        data.put("completedChecks", readiness.completedChecks());
        data.put("filesystemMutated", readiness.filesystemMutated());
        data.put("gameClassesResolved", readiness.gameClassesResolved());
        data.put("libraryDownloadStarted", readiness.libraryDownloadStarted());
        data.put("localArtifactsReady", readiness.localArtifactsReady());
        data.put("nativeExtractionStarted", readiness.nativeExtractionStarted());
        data.put("packId", packId);
        data.put("phase13M17ArtifactReady", readiness.phase13M17ArtifactReady());
        data.put("processLaunched", readiness.processLaunched());
        data.put("readinessId", readiness.readinessId());
        data.put("safeForIsolatedLaunchAttempt", readiness.safeForIsolatedLaunchAttempt());
        data.put("summary", readiness.phase13M17ArtifactReady()
                ? "M17 launch artifacts are mapped and ready for the isolated launch attempt gate."
                : "M17 launch artifacts are not ready; the launch attempt must remain controlled failure.");
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
        data.put("phase", phase);
        data.put("processLaunched", false);
        data.put("registryInjected", false);
        data.put("registryMutated", false);
        data.put("reportOnly", true);
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
                    "ECHO-NATIVE-M17-REPORT-MISSING",
                    EchoNativeIssueSeverity.ERROR,
                    "Phase 13 M17 artifact mapping required report missing",
                    "Artifact mapping requires " + reportName + ".",
                    null,
                    packId,
                    List.of(fixture.toString().replace('\\', '/')),
                    "Generate M17 preflight reports before mapping launch artifacts."
            ));
            return Map.of();
        }
        return EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(reportPath)));
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
                || Boolean.TRUE.equals(data.get("resourceRuntimeAccessed"))
                || Boolean.TRUE.equals(data.get("minecraftResourceManagerTouched"))
                || Boolean.TRUE.equals(data.get("minecraftRegistryTouched"))
                || Boolean.TRUE.equals(data.get("registryInjected"))
                || Boolean.TRUE.equals(data.get("registryMutated"))
                || Boolean.TRUE.equals(data.get("liveNetworkingStarted"))
                || Boolean.TRUE.equals(data.get("socketOpened"))
                || Boolean.TRUE.equals(data.get("clientConnectionOpened"))
                || Boolean.TRUE.equals(data.get("serverConnectionOpened"))
                || Boolean.TRUE.equals(data.get("packetSent"))
                || Boolean.TRUE.equals(data.get("packetReceived"))
                || Boolean.TRUE.equals(data.get("transformsEnabled"))
                || Boolean.TRUE.equals(data.get("transformsPerformed"))
                || Boolean.TRUE.equals(data.get("minecraftBytecodeTransformed"))
                || Boolean.TRUE.equals(data.get("addonBytecodeTransformed"))
                || Boolean.TRUE.equals(data.get("bytecodeMutated"))
                || Boolean.TRUE.equals(data.get("filesystemMutated"))
                || Boolean.TRUE.equals(data.get("unsafeRuntimeWorkStarted"));
    }

    private static List<EchoNativeDiagnostic> reportDiagnostics(Map<String, Object> report, String packId) {
        Object issues = report.get("issues");
        if (!(issues instanceof List<?> list)) {
            return List.of();
        }
        return list.stream()
                .filter(Map.class::isInstance)
                .map(item -> EchoNativeJson.asObject(item))
                .sorted(Comparator.comparing(item -> String.valueOf(item.get("code")) + ":" + item.get("summary")))
                .map(item -> new EchoNativeDiagnostic(
                        String.valueOf(item.getOrDefault("code", "ECHO-NATIVE-UPSTREAM-DIAGNOSTIC")),
                        EchoNativeIssueSeverity.ERROR,
                        String.valueOf(item.getOrDefault("title", "Upstream diagnostic")),
                        String.valueOf(item.getOrDefault("summary", "Upstream Phase 13 report is not PASS.")),
                        item.get("moduleId") == null ? null : String.valueOf(item.get("moduleId")),
                        packId,
                        EchoNativeJson.stringList(item.get("likelyFiles")),
                        String.valueOf(item.getOrDefault("suggestedFix", "Resolve upstream diagnostics first."))
                ))
                .toList();
    }

    private static List<EchoNativeDiagnostic> unique(List<EchoNativeDiagnostic> diagnostics) {
        Map<String, EchoNativeDiagnostic> byKey = new LinkedHashMap<>();
        for (EchoNativeDiagnostic diagnostic : diagnostics) {
            byKey.put(diagnostic.code() + "|" + diagnostic.moduleId() + "|" + diagnostic.summary(), diagnostic);
        }
        return byKey.values().stream()
                .sorted(Comparator.comparing(EchoNativeDiagnostic::code)
                        .thenComparing(diagnostic -> diagnostic.moduleId() == null ? "" : diagnostic.moduleId())
                        .thenComparing(EchoNativeDiagnostic::summary))
                .toList();
    }

    private static String relativeReportPath(Path path) {
        Path root = Path.of("").toAbsolutePath().normalize();
        Path normalized = path.toAbsolutePath().normalize();
        if (normalized.startsWith(root)) {
            return root.relativize(normalized).toString().replace('\\', '/');
        }
        return path.toString().replace('\\', '/');
    }

    private static String relativeFixturePath(Path fixture, Path path) {
        Path normalizedFixture = fixture.toAbsolutePath().normalize();
        Path normalizedPath = path.toAbsolutePath().normalize();
        if (normalizedPath.startsWith(normalizedFixture)) {
            return normalizedFixture.relativize(normalizedPath).toString().replace('\\', '/');
        }
        return path.toString().replace('\\', '/');
    }

    private record RuntimeArtifactMapping(
            String localPath,
            String source,
            boolean reviewed,
            boolean approved,
            String reviewStatus
    ) {
    }
}
