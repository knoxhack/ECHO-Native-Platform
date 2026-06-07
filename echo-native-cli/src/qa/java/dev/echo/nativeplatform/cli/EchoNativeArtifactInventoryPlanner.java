package dev.echo.nativeplatform.cli;

import dev.echo.nativeplatform.contracts.EchoNativeDiagnostic;
import dev.echo.nativeplatform.contracts.EchoNativeIssueSeverity;
import dev.echo.nativeplatform.contracts.EchoNativeLocalRuntimeArtifactInventory;
import dev.echo.nativeplatform.diagnostics.EchoNativeJson;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

final class EchoNativeArtifactInventoryPlanner {
    EchoNativeArtifactInventoryOutcome inventory(
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

        Path workspace = Path.of("").toAbsolutePath().normalize();
        Path repoRoot = workspace.getParent() == null ? workspace : workspace.getParent();
        List<SearchRoot> roots = approvedRoots(workspace, repoRoot);
        List<Map<String, Object>> artifacts = artifactInventory(
                repoRoot,
                reports.getOrDefault("classpath-builder-plan.json", Map.of()),
                reports.getOrDefault("native-extraction-plan.json", Map.of()),
                roots
        );

        for (Map<String, Object> artifact : artifacts) {
            if (!Boolean.TRUE.equals(artifact.get("runtimeResolved"))) {
                diagnostics.add(new EchoNativeDiagnostic(
                        "ECHO-NATIVE-M17-ARTIFACT-INVENTORY-UNRESOLVED",
                        EchoNativeIssueSeverity.ERROR,
                        "Repo-local runtime artifact inventory did not resolve a launch artifact",
                        artifact.get("id") + " is " + artifact.get("resolution") + " with "
                                + artifact.get("candidateCount") + " approved repo-local candidate(s).",
                        null,
                        packId,
                        List.of(String.valueOf(artifact.get("sourceReport"))),
                        "Add an explicit fixture runtime-artifacts.json mapping after reviewing approved candidates, or keep M17 as a controlled failure."
                ));
            }
        }

        diagnostics = unique(diagnostics);
        int candidateCount = artifacts.stream()
                .mapToInt(item -> ((Number) item.get("candidateCount")).intValue())
                .sum();
        int resolvedCount = (int) artifacts.stream()
                .filter(item -> Boolean.TRUE.equals(item.get("runtimeResolved")))
                .count();
        int unresolvedCount = artifacts.size() - resolvedCount;
        boolean inventoryComplete = diagnostics.stream().noneMatch(diagnostic ->
                "ECHO-NATIVE-M17-UPSTREAM-REPORT-BLOCKED".equals(diagnostic.code())
                        || "ECHO-NATIVE-M17-SAFETY-VIOLATION".equals(diagnostic.code())
                        || "ECHO-NATIVE-M17-REPORT-MISSING".equals(diagnostic.code()));

        EchoNativeLocalRuntimeArtifactInventory inventory = new EchoNativeLocalRuntimeArtifactInventory(
                "phase13.m17.local_runtime_artifact.inventory",
                inventoryComplete,
                true,
                false,
                false,
                artifacts.size(),
                candidateCount,
                candidateCount,
                unresolvedCount,
                roots.stream().map(root -> relativePath(repoRoot, root.path())).sorted().toList(),
                artifacts
        );

        return new EchoNativeArtifactInventoryOutcome(
                packId,
                localRuntimeArtifactInventory(packId, inventory, diagnostics),
                diagnostics
        );
    }

    private static List<Map<String, Object>> artifactInventory(
            Path repoRoot,
            Map<String, Object> classpathPlan,
            Map<String, Object> nativeExtractionPlan,
            List<SearchRoot> roots
    ) throws IOException {
        List<Map<String, Object>> artifacts = new ArrayList<>();
        Map<String, Object> classpathData = EchoNativeJson.asObject(classpathPlan.get("data"));
        Object entries = classpathData.get("plannedEntries");
        if (entries instanceof List<?> list) {
            for (Object raw : list) {
                Map<String, Object> entry = EchoNativeJson.asObject(raw);
                String id = String.valueOf(entry.getOrDefault("id", "unknown"));
                artifacts.add(inventoriedArtifact(
                        repoRoot,
                        "classpath:" + id,
                        id,
                        String.valueOf(entry.getOrDefault("entryKind", "classpath")),
                        String.valueOf(entry.getOrDefault("plannedPath", "")),
                        "classpath-builder-plan.json",
                        roots
                ));
            }
        }
        Map<String, Object> nativeData = EchoNativeJson.asObject(nativeExtractionPlan.get("data"));
        Object nativeEntries = nativeData.get("plannedNativeEntries");
        if (nativeEntries instanceof List<?> list) {
            for (Object raw : list) {
                Map<String, Object> entry = EchoNativeJson.asObject(raw);
                String id = String.valueOf(entry.getOrDefault("id", "unknown"));
                artifacts.add(inventoriedArtifact(
                        repoRoot,
                        "native:" + id,
                        id,
                        String.valueOf(entry.getOrDefault("entryKind", "native_library")),
                        String.valueOf(entry.getOrDefault("plannedExtractionPath", entry.getOrDefault("plannedPath", ""))),
                        "native-extraction-plan.json",
                        roots
                ));
            }
        }
        artifacts.sort(Comparator.comparing(item -> String.valueOf(item.get("id"))));
        return List.copyOf(artifacts);
    }

    private static Map<String, Object> inventoriedArtifact(
            Path repoRoot,
            String artifactId,
            String rawId,
            String artifactKind,
            String plannedPath,
            String sourceReport,
            List<SearchRoot> roots
    ) throws IOException {
        List<Map<String, Object>> candidates = findCandidates(repoRoot, rawId, artifactKind, roots);
        String resolution;
        if (candidates.isEmpty() && artifactId.startsWith("native:")) {
            resolution = "native-extraction-blocked";
        } else if (candidates.isEmpty() && "minecraft-client-coordinate".equals(rawId)) {
            resolution = "external-runtime-coordinate";
        } else if (candidates.isEmpty()) {
            resolution = "unresolved";
        } else {
            resolution = "repo-local-candidate";
        }

        Map<String, Object> item = new LinkedHashMap<>();
        item.put("artifactKind", artifactKind);
        item.put("approvedSourceOnly", true);
        item.put("candidateCount", candidates.size());
        item.put("candidates", candidates);
        item.put("downloadAllowed", false);
        item.put("filesystemMutated", false);
        item.put("id", artifactId);
        item.put("nativeExtractionStarted", false);
        item.put("plannedPath", plannedPath);
        item.put("resolution", resolution);
        item.put("runtimeResolved", false);
        item.put("sourceReport", sourceReport);
        return item;
    }

    private static List<Map<String, Object>> findCandidates(
            Path repoRoot,
            String rawId,
            String artifactKind,
            List<SearchRoot> roots
    ) throws IOException {
        if (artifactKind.contains("native")) {
            return List.of();
        }
        List<Map<String, Object>> candidates = new ArrayList<>();
        for (SearchRoot root : roots) {
            if (!Files.isDirectory(root.path())) {
                continue;
            }
            try (Stream<Path> stream = Files.walk(root.path(), root.depth())) {
                List<Path> matches = stream
                        .filter(Files::isRegularFile)
                        .filter(path -> path.getFileName().toString().endsWith(".jar"))
                        .filter(path -> matchesArtifact(rawId, path.getFileName().toString()))
                        .sorted(Comparator.comparing(path -> relativePath(repoRoot, path)))
                        .toList();
                for (Path match : matches) {
                    Map<String, Object> candidate = new LinkedHashMap<>();
                    candidate.put("approved", true);
                    candidate.put("path", relativePath(repoRoot, match));
                    candidate.put("source", root.kind());
                    candidate.put("trustedForAutoLaunch", false);
                    candidates.add(candidate);
                }
            }
        }
        return candidates.stream()
                .sorted(Comparator.comparing(item -> String.valueOf(item.get("path"))))
                .toList();
    }

    private static boolean matchesArtifact(String rawId, String fileName) {
        String normalized = rawId.replace("-api", "");
        return fileName.equals(rawId + ".jar")
                || fileName.startsWith(rawId + "-")
                || fileName.equals(normalized + ".jar")
                || fileName.startsWith(normalized + "-");
    }

    private static List<SearchRoot> approvedRoots(Path workspace, Path repoRoot) {
        List<SearchRoot> roots = new ArrayList<>();
        roots.add(new SearchRoot(repoRoot.resolve("build").resolve("tmp").resolve("echo-1.1.3-mods").normalize(), 1, "root-build-mods"));
        roots.add(new SearchRoot(repoRoot.resolve("addons").normalize(), 4, "addon-build-libs"));
        roots.add(new SearchRoot(repoRoot.resolve("core").normalize(), 4, "core-build-libs"));
        roots.add(new SearchRoot(workspace.normalize(), 4, "native-workspace-build-libs"));
        return roots.stream()
                .filter(root -> !relativePath(repoRoot, root.path()).startsWith(".local/"))
                .toList();
    }

    private static Map<String, Object> localRuntimeArtifactInventory(
            String packId,
            EchoNativeLocalRuntimeArtifactInventory inventory,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m17_local_runtime_artifact_inventory", diagnostics);
        data.put("approvedCandidateCount", inventory.approvedCandidateCount());
        data.put("approvedRoots", inventory.approvedRoots());
        data.put("artifacts", inventory.artifacts());
        data.put("candidateArtifactCount", inventory.candidateArtifactCount());
        data.put("downloadsAllowed", inventory.downloadsAllowed());
        data.put("filesystemMutated", inventory.filesystemMutated());
        data.put("inventoryComplete", inventory.inventoryComplete());
        data.put("inventoryId", inventory.inventoryId());
        data.put("packId", packId);
        data.put("plannedArtifactCount", inventory.plannedArtifactCount());
        data.put("repoLocalOnly", inventory.repoLocalOnly());
        String summary;
        if (!inventory.inventoryComplete()) {
            summary = "Approved repo-local artifact inventory is blocked by upstream diagnostics.";
        } else if (inventory.unresolvedArtifactCount() == 0) {
            summary = "Approved repo-local artifact inventory found candidates for every planned launch artifact.";
        } else {
            summary = "Approved repo-local artifact inventory still has unresolved planned launch artifacts.";
        }
        data.put("summary", summary);
        data.put("unresolvedArtifactCount", inventory.unresolvedArtifactCount());
        return data;
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
                    "Phase 13 M17 artifact inventory upstream report is not PASS",
                    "Artifact inventory requires PASS " + reportName + ".",
                    null,
                    packId,
                    List.of(relativeReportPath(path)),
                    "Regenerate and resolve M17 preflight reports before inventorying launch artifacts."
            ));
        }
        if (hasUnsafeRuntimeWork(data)) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-M17-SAFETY-VIOLATION",
                    EchoNativeIssueSeverity.ERROR,
                    "Phase 13 M17 artifact inventory input contains unsafe runtime work",
                    reportName + " indicates runtime work that is not safe for artifact inventory.",
                    null,
                    packId,
                    List.of(relativeReportPath(path)),
                    "Keep artifact inventory report-only: no launch, command execution, downloads, extraction, classloader, transforms, registry injection, or mutation."
            ));
        }
        diagnostics.addAll(reportDiagnostics(report, packId));
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
                    "Phase 13 M17 artifact inventory required report missing",
                    "Artifact inventory requires " + reportName + ".",
                    null,
                    packId,
                    List.of(fixture.toString().replace('\\', '/')),
                    "Generate M17 preflight reports before inventorying launch artifacts."
            ));
            return Map.of();
        }
        return EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(reportPath)));
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

    private static String relativePath(Path root, Path path) {
        Path normalizedRoot = root.toAbsolutePath().normalize();
        Path normalizedPath = path.toAbsolutePath().normalize();
        if (normalizedPath.startsWith(normalizedRoot)) {
            return normalizedRoot.relativize(normalizedPath).toString().replace('\\', '/');
        }
        return path.toString().replace('\\', '/');
    }

    private record SearchRoot(Path path, int depth, String kind) {
    }
}
