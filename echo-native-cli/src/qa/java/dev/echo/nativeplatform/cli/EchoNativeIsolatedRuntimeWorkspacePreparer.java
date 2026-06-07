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

final class EchoNativeIsolatedRuntimeWorkspacePreparer {
    EchoNativeIsolatedRuntimeWorkspaceOutcome prepare(
            String packId,
            Path fixture,
            Map<String, Path> requiredReports
    ) throws IOException {
        List<EchoNativeDiagnostic> diagnostics = new ArrayList<>();
        Map<String, Map<String, Object>> reports = new LinkedHashMap<>();
        for (Map.Entry<String, Path> entry : requiredReports.entrySet()) {
            Map<String, Object> report = readRequiredReport(entry.getValue(), fixture, packId, entry.getKey(), diagnostics);
            reports.put(entry.getKey(), report);
            checkReport(entry.getKey(), entry.getValue(), report, packId, diagnostics);
        }

        boolean phase1Complete = Boolean.TRUE.equals(data(reports.get("native-loader-reality-audit.json")).get("phase1Complete"));
        if (!phase1Complete) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-ISOLATED-RUNTIME-PHASE1-INCOMPLETE",
                    EchoNativeIssueSeverity.ERROR,
                    "Launch reality audit must pass before workspace materialization",
                    "The isolated runtime workspace requires a completed launch reality audit so the beta path is grounded in current launch evidence.",
                    null,
                    packId,
                    List.of("reports/echo-native/" + packId + "/native-loader-reality-audit.json"),
                    "Run phase13 audit launch-reality <fixture> and resolve blocking diagnostics before preparing the isolated runtime workspace."
            ));
        }

        List<EchoNativeDiagnostic> preMaterializationDiagnostics = diagnostics.stream()
                .sorted(Comparator.comparing(EchoNativeDiagnostic::code)
                        .thenComparing(diagnostic -> diagnostic.moduleId() == null ? "" : diagnostic.moduleId())
                        .thenComparing(EchoNativeDiagnostic::summary))
                .toList();
        boolean canMaterialize = preMaterializationDiagnostics.stream().noneMatch(EchoNativeIsolatedRuntimeWorkspacePreparer::isBlocking);
        Path workspaceRoot = fixture.resolve("isolated-runtime");
        List<Path> requiredDirectories = requiredDirectories(workspaceRoot);
        if (canMaterialize) {
            for (Path directory : requiredDirectories) {
                Files.createDirectories(directory);
            }
        }

        List<Map<String, Object>> directories = directoryReports(requiredDirectories);
        boolean workspaceMaterialized = canMaterialize && directories.stream().allMatch(directory -> Boolean.TRUE.equals(directory.get("exists")));
        List<EchoNativeDiagnostic> sortedDiagnostics = diagnostics.stream()
                .sorted(Comparator.comparing(EchoNativeDiagnostic::code)
                        .thenComparing(diagnostic -> diagnostic.moduleId() == null ? "" : diagnostic.moduleId())
                        .thenComparing(EchoNativeDiagnostic::summary))
                .toList();

        return new EchoNativeIsolatedRuntimeWorkspaceOutcome(
                packId,
                workspacePlan(packId, fixture, workspaceRoot, directories, sortedDiagnostics),
                materialization(packId, workspaceRoot, workspaceMaterialized, canMaterialize, directories, sortedDiagnostics),
                safetyStatus(packId, workspaceMaterialized, canMaterialize, sortedDiagnostics),
                sortedDiagnostics
        );
    }

    private static List<Path> requiredDirectories(Path workspaceRoot) {
        return List.of(
                workspaceRoot,
                workspaceRoot.resolve("game"),
                workspaceRoot.resolve("game/logs"),
                workspaceRoot.resolve("game/crash-reports"),
                workspaceRoot.resolve("natives"),
                workspaceRoot.resolve("output"),
                workspaceRoot.resolve("temp")
        );
    }

    private static List<Map<String, Object>> directoryReports(List<Path> directories) {
        return directories.stream()
                .sorted()
                .map(directory -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("path", relativePath(directory));
                    item.put("exists", Files.isDirectory(directory));
                    item.put("fixtureLocal", true);
                    item.put("userHomePath", false);
                    item.put("cachePath", false);
                    item.put("launcherInstallPath", false);
                    return item;
                })
                .toList();
    }

    private static Map<String, Object> workspacePlan(
            String packId,
            Path fixture,
            Path workspaceRoot,
            List<Map<String, Object>> directories,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("native_loader_isolated_runtime_workspace_plan", diagnostics);
        data.put("fixture", relativePath(fixture));
        data.put("isolatedRuntimeWorkspaceRoot", relativePath(workspaceRoot));
        data.put("packId", packId);
        data.put("phase2Complete", diagnostics.stream().noneMatch(EchoNativeIsolatedRuntimeWorkspacePreparer::isBlocking));
        data.put("requiredDirectories", directories);
        data.put("requiredDirectoryCount", directories.size());
        data.put("summary", "Fixture-local isolated runtime workspace directories are planned for a future controlled process launcher.");
        data.put("workspacePurpose", "native_loader_beta_isolated_minecraft_process");
        return data;
    }

    private static Map<String, Object> materialization(
            String packId,
            Path workspaceRoot,
            boolean workspaceMaterialized,
            boolean materializationAllowed,
            List<Map<String, Object>> directories,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("native_loader_isolated_runtime_workspace_materialization", diagnostics);
        long presentCount = directories.stream().filter(directory -> Boolean.TRUE.equals(directory.get("exists"))).count();
        data.put("fixtureWorkspaceMaterializationAllowed", materializationAllowed);
        data.put("fixtureWorkspaceMaterialized", workspaceMaterialized);
        data.put("isolatedRuntimeWorkspaceRoot", relativePath(workspaceRoot));
        data.put("missingDirectoryCount", directories.size() - presentCount);
        data.put("packId", packId);
        data.put("presentDirectoryCount", presentCount);
        data.put("requiredDirectories", directories);
        data.put("requiredDirectoryCount", directories.size());
        data.put("summary", workspaceMaterialized
                ? "Isolated runtime workspace directories are present under the fixture."
                : "Isolated runtime workspace directories were not materialized because upstream gates are blocked.");
        return data;
    }

    private static Map<String, Object> safetyStatus(
            String packId,
            boolean workspaceMaterialized,
            boolean materializationAllowed,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("native_loader_isolated_runtime_workspace_safety_status", diagnostics);
        data.put("fixtureWorkspaceMaterializationAllowed", materializationAllowed);
        data.put("fixtureWorkspaceMaterialized", workspaceMaterialized);
        data.put("isolatedRuntimeWorkspaceReady", workspaceMaterialized);
        data.put("nativeLoaderBetaReady", false);
        data.put("nextImplementationPhase", workspaceMaterialized ? "phase3.real_process_launch_harness" : "phase2.isolated_runtime_workspace");
        data.put("packId", packId);
        data.put("summary", workspaceMaterialized
                ? "Phase 2 isolated runtime workspace is ready; the next slice is the controlled process launch harness."
                : "Phase 2 isolated runtime workspace is blocked by upstream diagnostics.");
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
                    "ECHO-NATIVE-ISOLATED-RUNTIME-REPORT-MISSING",
                    EchoNativeIssueSeverity.ERROR,
                    "Isolated runtime workspace required report missing",
                    "Preparing the isolated runtime workspace requires " + reportName + ".",
                    null,
                    packId,
                    List.of(fixture.toString().replace('\\', '/')),
                    "Regenerate launch reality, runtime fixture, and M17 reports before preparing the isolated runtime workspace."
            ));
            return Map.of();
        }
        return EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(reportPath)));
    }

    private static void checkReport(
            String reportName,
            Path path,
            Map<String, Object> report,
            String packId,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        if (report.isEmpty()) {
            return;
        }
        String status = String.valueOf(report.getOrDefault("status", "MISSING"));
        if (!"PASS".equals(status) && !"PASS_WITH_WARNINGS".equals(status)) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-ISOLATED-RUNTIME-UPSTREAM-BLOCKED",
                    EchoNativeIssueSeverity.ERROR,
                    "Isolated runtime workspace upstream report is not PASS",
                    "Preparing the isolated runtime workspace requires PASS or accepted PASS_WITH_WARNINGS " + reportName + ".",
                    null,
                    packId,
                    List.of(relativePath(path)),
                    "Resolve upstream runtime fixture and launch audit reports before workspace materialization."
            ));
        }
        if (hasUnsafeRuntimeWork(data(report))) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-ISOLATED-RUNTIME-UNSAFE-WORK",
                    EchoNativeIssueSeverity.ERROR,
                    "Isolated runtime workspace upstream report contains unsafe runtime work",
                    reportName + " indicates unsafe runtime work that cannot be carried into the native loader beta workspace.",
                    null,
                    packId,
                    List.of(relativePath(path)),
                    "Keep downloads, native extraction, classloading, process launch, registry mutation, and user install mutation out of Phase 2."
            ));
        }
    }

    private static Map<String, Object> base(String phase, List<EchoNativeDiagnostic> diagnostics) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("bytecodeMutated", false);
        data.put("cacheMutated", false);
        data.put("classloaderCreated", false);
        data.put("commandExecuted", false);
        data.put("configMutationAllowed", false);
        data.put("diagnosticCount", diagnostics.size());
        data.put("diagnosticsCaptured", true);
        data.put("downloadAllowed", false);
        data.put("downloadsAllowed", false);
        data.put("filesystemMutated", false);
        data.put("gameClassesResolved", false);
        data.put("gameProcessLaunched", false);
        data.put("libraryDownloadStarted", false);
        data.put("minecraftLaunched", false);
        data.put("nativeExtractionStarted", false);
        data.put("packMutationAllowed", false);
        data.put("phase", phase);
        data.put("processLaunched", false);
        data.put("registryInjected", false);
        data.put("registryMutated", false);
        data.put("saveMutationAllowed", false);
        data.put("transformsEnabled", false);
        data.put("transformsPerformed", false);
        data.put("unsafeRuntimeWorkStarted", false);
        data.put("userInstallMutationAllowed", false);
        return data;
    }

    private static Map<String, Object> data(Map<String, Object> report) {
        return EchoNativeJson.asObject(report == null ? null : report.get("data"));
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
                || Boolean.TRUE.equals(data.get("unsafeRuntimeWorkStarted"));
    }

    private static boolean isBlocking(EchoNativeDiagnostic diagnostic) {
        return diagnostic.severity() == EchoNativeIssueSeverity.ERROR
                || diagnostic.severity() == EchoNativeIssueSeverity.FATAL;
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
