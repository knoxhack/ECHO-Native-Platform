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

final class EchoNativeRealProcessLaunchHarnessPlanner {
    private static final String PLACEHOLDER_MAIN_CLASS = "minecraft-client-main-class";

    EchoNativeRealProcessLaunchHarnessOutcome plan(
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

        Map<String, Object> workspaceSafety = data(reports.get("isolated-runtime-workspace-safety-status.json"));
        Map<String, Object> workspaceMaterialization = data(reports.get("isolated-runtime-workspace-materialization.json"));
        Map<String, Object> artifactMap = data(reports.get("local-runtime-artifact-map.json"));
        Map<String, Object> launchArguments = data(reports.get("launch-argument-builder-plan.json"));
        Map<String, Object> nativeExtraction = data(reports.get("native-extraction-plan.json"));

        boolean workspaceReady = Boolean.TRUE.equals(workspaceSafety.get("isolatedRuntimeWorkspaceReady"))
                && Boolean.TRUE.equals(workspaceMaterialization.get("fixtureWorkspaceMaterialized"));
        if (!workspaceReady) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-REAL-PROCESS-WORKSPACE-NOT-READY",
                    EchoNativeIssueSeverity.ERROR,
                    "Isolated runtime workspace is not ready",
                    "The real-process harness requires a prepared fixture-local isolated runtime workspace before command preview planning.",
                    null,
                    packId,
                    List.of("reports/echo-native/" + packId + "/isolated-runtime-workspace-safety-status.json"),
                    "Run phase13 prepare isolated-runtime <fixture> and resolve diagnostics before planning the real-process harness."
            ));
        }

        List<Map<String, Object>> artifacts = artifactEntries(artifactMap);
        long localArtifactCount = artifacts.stream().filter(artifact -> Boolean.TRUE.equals(artifact.get("local"))).count();
        long unresolvedArtifactCount = artifacts.size() - localArtifactCount;
        if (artifacts.isEmpty() || unresolvedArtifactCount > 0) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-REAL-PROCESS-ARTIFACTS-NOT-READY",
                    EchoNativeIssueSeverity.ERROR,
                    "Local runtime artifacts are not fully resolved",
                    "The real-process harness requires every classpath and native runtime artifact to be mapped to reviewed local fixture/workspace files.",
                    null,
                    packId,
                    List.of("reports/echo-native/" + packId + "/local-runtime-artifact-map.json"),
                    "Regenerate artifact mapping and runtime fixture integrity reports before planning the launch harness."
            ));
        }

        MainClassResolution mainClass = resolveMainClass(fixture, diagnostics, packId);
        boolean upstreamReady = diagnostics.stream().noneMatch(EchoNativeRealProcessLaunchHarnessPlanner::isBlocking);
        if (upstreamReady && !mainClass.resolved()) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-REAL-PROCESS-MAIN-CLASS-UNRESOLVED",
                    EchoNativeIssueSeverity.ERROR,
                    "Real-process launch main class is unresolved",
                    "The command preview cannot represent a real Minecraft client launch while it still contains the placeholder main class.",
                    null,
                    packId,
                    List.of(relativePath(fixture.resolve("local-runtime/minecraft/26.1.2/metadata/26.1.2.json"))),
                    "Stage a fixture-local version manifest with a reviewed mainClass value before marking the real-process harness ready."
            ));
        }
        boolean ready = diagnostics.stream().noneMatch(EchoNativeRealProcessLaunchHarnessPlanner::isBlocking);
        List<EchoNativeDiagnostic> sortedDiagnostics = diagnostics.stream()
                .sorted(Comparator.comparing(EchoNativeDiagnostic::code)
                        .thenComparing(diagnostic -> diagnostic.moduleId() == null ? "" : diagnostic.moduleId())
                        .thenComparing(EchoNativeDiagnostic::summary))
                .toList();

        String workspaceRoot = String.valueOf(workspaceMaterialization.getOrDefault("isolatedRuntimeWorkspaceRoot", relativePath(fixture.resolve("isolated-runtime"))));
        String gameDir = workspaceRoot + "/game";
        String nativesDir = workspaceRoot + "/natives";
        List<String> classpath = classpathEntries(fixture, artifacts);
        List<Map<String, Object>> previewArguments = commandLinePreviewArguments(packId, gameDir, nativesDir, classpath, launchArguments, mainClass.value());

        return new EchoNativeRealProcessLaunchHarnessOutcome(
                packId,
                harnessPlan(packId, fixture, workspaceRoot, artifacts, classpath, nativeExtraction, mainClass, ready, sortedDiagnostics),
                safetyGate(packId, ready, mainClass, artifacts.size(), localArtifactCount, unresolvedArtifactCount, sortedDiagnostics),
                commandLinePreview(packId, previewArguments, classpath, mainClass, sortedDiagnostics),
                environmentPlan(packId, workspaceRoot, gameDir, nativesDir, sortedDiagnostics),
                sortedDiagnostics
        );
    }

    private static Map<String, Object> harnessPlan(
            String packId,
            Path fixture,
            String workspaceRoot,
            List<Map<String, Object>> artifacts,
            List<String> classpath,
            Map<String, Object> nativeExtraction,
            MainClassResolution mainClass,
            boolean ready,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("native_loader_real_process_launch_harness_plan", diagnostics);
        data.put("fixture", relativePath(fixture));
        data.put("harnessPlanReady", ready);
        data.put("isolatedRuntimeWorkspaceRoot", workspaceRoot);
        data.put("localArtifactCount", artifacts.stream().filter(artifact -> Boolean.TRUE.equals(artifact.get("local"))).count());
        data.put("mainClass", mainClass.value());
        data.put("mainClassResolved", mainClass.resolved());
        data.put("mainClassSource", mainClass.source());
        data.put("mappedArtifactCount", artifacts.size());
        data.put("nativeEntryCount", asLong(nativeExtraction.getOrDefault("entryCount", 0)));
        data.put("nextImplementationPhase", ready ? "phase4.gated_process_execution" : "phase3.real_process_launch_harness");
        data.put("packId", packId);
        data.put("phase3Complete", ready);
        data.put("plannedClasspathEntryCount", classpath.size());
        data.put("processLaunchHarnessReady", ready);
        data.put("realProcessLaunchImplemented", false);
        data.put("summary", ready
                ? "Phase 3 real-process launch harness planning is ready; execution remains disabled for a later gate."
                : "Phase 3 real-process launch harness planning is blocked by diagnostics.");
        return data;
    }

    private static Map<String, Object> safetyGate(
            String packId,
            boolean ready,
            MainClassResolution mainClass,
            int mappedArtifactCount,
            long localArtifactCount,
            long unresolvedArtifactCount,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("native_loader_real_process_launch_safety_gate", diagnostics);
        data.put("commandExecutionAllowed", false);
        data.put("executionRequiresExplicitNextCommand", true);
        data.put("launchExecutionAllowed", false);
        data.put("localArtifactCount", localArtifactCount);
        data.put("mainClassResolved", mainClass.resolved());
        data.put("mappedArtifactCount", mappedArtifactCount);
        data.put("packId", packId);
        data.put("phase3Complete", ready);
        data.put("processLaunchHarnessReady", ready);
        data.put("safeForCommandPreview", ready);
        data.put("safeForProcessExecution", false);
        data.put("summary", ready
                ? "The process launch harness is ready for preview only; process execution is still disabled."
                : "The process launch harness safety gate is blocked.");
        data.put("unresolvedArtifactCount", unresolvedArtifactCount);
        return data;
    }

    private static Map<String, Object> commandLinePreview(
            String packId,
            List<Map<String, Object>> previewArguments,
            List<String> classpath,
            MainClassResolution mainClass,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("native_loader_real_process_command_line_preview", diagnostics);
        data.put("argumentCount", previewArguments.size());
        data.put("classpathEntryCount", classpath.size());
        data.put("commandLineMaterialized", true);
        data.put("commandLinePreview", previewArguments);
        data.put("commandPreviewOnly", true);
        data.put("executable", "java");
        data.put("mainClass", mainClass.value());
        data.put("mainClassResolved", mainClass.resolved());
        data.put("mainClassSource", mainClass.source());
        data.put("packId", packId);
        data.put("summary", "Command-line preview is materialized as deterministic report data only; it was not executed.");
        return data;
    }

    private static Map<String, Object> environmentPlan(
            String packId,
            String workspaceRoot,
            String gameDir,
            String nativesDir,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("native_loader_real_process_environment_plan", diagnostics);
        data.put("environmentVariables", List.of(
                env("ECHO_NATIVE_PACK_ID", packId),
                env("ECHO_NATIVE_WORKSPACE", workspaceRoot),
                env("ECHO_NATIVE_GAME_DIR", gameDir),
                env("ECHO_NATIVE_NATIVES_DIR", nativesDir)
        ));
        data.put("environmentVariableCount", 4);
        data.put("isolatedRuntimeWorkspaceRoot", workspaceRoot);
        data.put("packId", packId);
        data.put("redactsSecrets", true);
        data.put("secretSafe", true);
        data.put("summary", "Environment plan contains fixture-local values only and no environment dump.");
        data.put("workingDirectory", gameDir);
        return data;
    }

    private static List<Map<String, Object>> commandLinePreviewArguments(
            String packId,
            String gameDir,
            String nativesDir,
            List<String> classpath,
            Map<String, Object> launchArguments,
            String mainClass
    ) {
        List<Map<String, Object>> args = new ArrayList<>();
        args.add(argument(0, "javaExecutable", "java"));
        args.add(argument(1, "libraryPath", "-Djava.library.path=" + nativesDir));
        args.add(argument(2, "classpathSwitch", "-cp"));
        args.add(argument(3, "classpath", String.join(";", classpath)));
        args.add(argument(4, "mainClass", mainClass));
        args.add(argument(5, "gameDir", "--gameDir=" + gameDir));
        args.add(argument(6, "echoPack", "--echo-pack=" + packId));
        args.add(argument(7, "runtimeMode", "--echo-runtime-mode=native-loader-beta-preview"));
        args.add(argument(8, "launchBlocked", "--echo-launch-blocked=true"));
        Object planned = launchArguments.get("plannedArguments");
        if (planned instanceof List<?> list) {
            int order = 9;
            for (Object raw : list) {
                Map<String, Object> item = EchoNativeJson.asObject(raw);
                String id = String.valueOf(item.getOrDefault("id", "arg" + order));
                if ("gameDir".equals(id) || "classpath".equals(id) || "nativesDirectory".equals(id)) {
                    continue;
                }
                String name = String.valueOf(item.getOrDefault("name", id));
                String value = String.valueOf(item.getOrDefault("value", ""));
                args.add(argument(order++, "planned:" + id, name + (value.isBlank() ? "" : "=" + value)));
            }
        }
        return List.copyOf(args);
    }

    private static MainClassResolution resolveMainClass(
            Path fixture,
            List<EchoNativeDiagnostic> diagnostics,
            String packId
    ) {
        Path versionManifest = fixture.resolve("local-runtime/minecraft/26.1.2/metadata/26.1.2.json");
        if (!Files.isRegularFile(versionManifest)) {
            return new MainClassResolution(PLACEHOLDER_MAIN_CLASS, false, "placeholder_manifest_missing");
        }
        try {
            Map<String, Object> manifest = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(versionManifest)));
            String mainClass = String.valueOf(manifest.getOrDefault("mainClass", "")).trim();
            if (!mainClass.isBlank()) {
                return new MainClassResolution(mainClass, true, "fixture_local_version_manifest");
            }
            return new MainClassResolution(PLACEHOLDER_MAIN_CLASS, false, "placeholder_manifest_main_class_missing");
        } catch (RuntimeException | IOException exception) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-REAL-PROCESS-MANIFEST-INVALID",
                    EchoNativeIssueSeverity.ERROR,
                    "Fixture-local version manifest could not be parsed",
                    "The real-process launch harness could not parse the fixture-local version manifest for the client main class.",
                    null,
                    packId,
                    List.of(relativePath(versionManifest)),
                    "Replace the fixture-local version manifest with reviewed valid JSON before planning the launch harness."
            ));
            return new MainClassResolution(PLACEHOLDER_MAIN_CLASS, false, "placeholder_manifest_invalid");
        }
    }

    private static Map<String, Object> argument(int order, String id, String value) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("commandExecuted", false);
        item.put("id", id);
        item.put("order", order);
        item.put("orderKey", String.format("0-preview-%04d-%s", order, id));
        item.put("previewOnly", true);
        item.put("secretSafe", true);
        item.put("value", value);
        return item;
    }

    private static Map<String, Object> env(String name, String value) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("name", name);
        item.put("secret", false);
        item.put("value", value);
        return item;
    }

    private static List<Map<String, Object>> artifactEntries(Map<String, Object> artifactMap) {
        Object rawArtifacts = artifactMap.get("artifacts");
        if (!(rawArtifacts instanceof List<?> list)) {
            return List.of();
        }
        List<Map<String, Object>> artifacts = new ArrayList<>();
        for (Object raw : list) {
            Map<String, Object> artifact = EchoNativeJson.asObject(raw);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", String.valueOf(artifact.getOrDefault("id", "unknown")));
            item.put("local", Boolean.TRUE.equals(artifact.get("local")));
            item.put("localPath", String.valueOf(artifact.getOrDefault("localPath", "")));
            item.put("mappingApproved", Boolean.TRUE.equals(artifact.get("mappingApproved")));
            item.put("mappingReviewed", Boolean.TRUE.equals(artifact.get("mappingReviewed")));
            item.put("runtimeResolved", Boolean.TRUE.equals(artifact.get("runtimeResolved")));
            artifacts.add(item);
        }
        artifacts.sort(Comparator.comparing(item -> String.valueOf(item.get("id"))));
        return List.copyOf(artifacts);
    }

    private static List<String> classpathEntries(Path fixture, List<Map<String, Object>> artifacts) {
        String fixturePrefix = relativePath(fixture) + "/";
        return artifacts.stream()
                .filter(artifact -> String.valueOf(artifact.get("id")).startsWith("classpath:"))
                .map(artifact -> String.valueOf(artifact.get("localPath")))
                .map(path -> path.startsWith("local-runtime/") ? fixturePrefix + path : path)
                .sorted()
                .toList();
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

    private static Map<String, Object> readRequiredReport(
            Path reportPath,
            Path fixture,
            String packId,
            String reportName,
            List<EchoNativeDiagnostic> diagnostics
    ) throws IOException {
        if (!Files.isRegularFile(reportPath)) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-REAL-PROCESS-REPORT-MISSING",
                    EchoNativeIssueSeverity.ERROR,
                    "Real-process harness required report missing",
                    "Planning the real-process launch harness requires " + reportName + ".",
                    null,
                    packId,
                    List.of(fixture.toString().replace('\\', '/')),
                    "Regenerate Phase 2 isolated runtime and M17 launch reports before planning the real-process harness."
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
        boolean launchRealityWarning = "native-loader-reality-audit.json".equals(reportName) && "PASS_WITH_WARNINGS".equals(status);
        if (!"PASS".equals(status) && !launchRealityWarning) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-REAL-PROCESS-UPSTREAM-BLOCKED",
                    EchoNativeIssueSeverity.ERROR,
                    "Real-process harness upstream report is not PASS",
                    "Planning the real-process launch harness requires PASS " + reportName + ".",
                    null,
                    packId,
                    List.of(relativePath(path)),
                    "Resolve upstream runtime fixture, launch safety, and isolated workspace reports first."
            ));
        }
        if (hasUnsafeRuntimeWork(data(report))) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-REAL-PROCESS-UNSAFE-WORK",
                    EchoNativeIssueSeverity.ERROR,
                    "Real-process harness upstream report contains unsafe runtime work",
                    reportName + " indicates unsafe runtime work that cannot be carried into the launch harness planning gate.",
                    null,
                    packId,
                    List.of(relativePath(path)),
                    "Keep downloads, native extraction, classloading, process launch, registry mutation, and user install mutation out of Phase 3 planning."
            ));
        }
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

    private static Long asLong(Object value) {
        return value instanceof Number number ? number.longValue() : 0L;
    }

    private static String relativePath(Path path) {
        Path root = Path.of("").toAbsolutePath().normalize();
        Path normalized = path.toAbsolutePath().normalize();
        if (normalized.startsWith(root)) {
            return root.relativize(normalized).toString().replace('\\', '/');
        }
        return path.toString().replace('\\', '/');
    }

    private record MainClassResolution(String value, boolean resolved, String source) {
    }
}
