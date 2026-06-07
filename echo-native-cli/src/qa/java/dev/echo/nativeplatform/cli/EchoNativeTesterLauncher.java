package dev.echo.nativeplatform.cli;

import dev.echo.nativeplatform.bootstrap.EchoNativeBootstrapMain;
import dev.echo.nativeplatform.contracts.EchoNativeAddonDescriptor;
import dev.echo.nativeplatform.contracts.EchoNativeDiagnostic;
import dev.echo.nativeplatform.contracts.EchoNativeIssueSeverity;
import dev.echo.nativeplatform.diagnostics.EchoNativeJson;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * QA source-set launch helper. Release/native-client startup must use the
 * product launcher path, not this report-driven tester path.
 */
final class EchoNativeTesterLauncher {
    private static final String PLACEHOLDER_MAIN_CLASS = "minecraft-client-main-class";
    private static final String NATIVE_LOADER_TESTER_LABEL = "EXPERIMENTAL ECHO NATIVE LOADER - INTERNAL TEST ONLY";
    private static final String NATIVE_LOADER_VERSION = "ECHO Native Loader 26.1.2";
    private static final String NATIVE_LOADER_VERSION_TYPE = "echo-native-loader-beta";
    private static final String NATIVE_PLATFORM_ARTIFACT_VERSION = "0.1.0-native-beta";

    EchoNativeTesterLaunchOutcome launch(
            String packId,
            Path fixture,
            List<EchoNativeAddonDescriptor> descriptors,
            Map<String, Path> requiredReports
    ) throws IOException {
        List<EchoNativeDiagnostic> diagnostics = new ArrayList<>();
        Map<String, Map<String, Object>> reports = new LinkedHashMap<>();
        for (Map.Entry<String, Path> entry : requiredReports.entrySet()) {
            Map<String, Object> report = readRequiredReport(entry.getValue(), fixture, packId, entry.getKey(), diagnostics);
            reports.put(entry.getKey(), report);
            checkReport(entry.getKey(), entry.getValue(), report, packId, diagnostics);
        }

        Map<String, Object> preview = data(reports.get("real-process-command-line-preview.json"));
        Map<String, Object> environment = data(reports.get("real-process-environment-plan.json"));
        String executable = String.valueOf(preview.getOrDefault("executable", "java"));
        String resolvedMainClass = resolveMainClass(fixture, String.valueOf(preview.getOrDefault("mainClass", "")), diagnostics, packId);
        boolean mainClassResolved = !resolvedMainClass.isBlank() && !PLACEHOLDER_MAIN_CLASS.equals(resolvedMainClass);
        String classpath = previewArgument(preview, "classpath");
        String libraryPath = previewArgument(preview, "libraryPath");
        String gameDirArgument = previewArgument(preview, "gameDir");
        String workingDirectory = String.valueOf(environment.getOrDefault("workingDirectory", fixture.resolve("isolated-runtime/game").toString()));
        Path workingDirectoryPath = resolveWorkspacePath(workingDirectory);
        Path gameDirectory = resolveArgumentPath(gameDirArgument.startsWith("--gameDir=") ? gameDirArgument.substring("--gameDir=".length()) : workingDirectory);
        Path assetsDirectory = workingDirectoryPath.getParent().resolve("assets").toAbsolutePath().normalize();
        Path nativesDirectory = workingDirectoryPath.getParent().resolve("natives").toAbsolutePath().normalize();
        Path stdoutPath = workingDirectoryPath.resolve("logs/echo-native-tester-launch.out.log").normalize();
        Path stderrPath = workingDirectoryPath.resolve("logs/echo-native-tester-launch.err.log").normalize();

        boolean assetsReady = Files.isRegularFile(assetsDirectory.resolve("indexes/30.json"));
        if (!assetsReady) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-TESTER-LAUNCH-ASSETS-MISSING",
                    EchoNativeIssueSeverity.ERROR,
                    "Tester launch asset index is missing",
                    "The tester launch command requires fixture-local isolated assets before starting the client.",
                    null,
                    packId,
                    List.of(relativePath(assetsDirectory.resolve("indexes/30.json"))),
                    "Stage the reviewed local asset index and object store into the isolated runtime assets directory."
            ));
        }

        NativeExtractionRun extraction = NativeExtractionRun.skipped(nativesDirectory);
        if (diagnostics.stream().noneMatch(EchoNativeTesterLauncher::isBlocking)) {
            extraction = materializeNatives(fixture, nativesDirectory, packId, diagnostics);
        }

        boolean canStart = diagnostics.stream().noneMatch(EchoNativeTesterLauncher::isBlocking)
                && mainClassResolved
                && !classpath.isBlank()
                && Files.isDirectory(workingDirectoryPath)
                && extraction.ready();

        TesterProcess process = TesterProcess.skipped();
        if (canStart) {
            Optional<ProcessHandle> existing = findExistingTesterProcess(gameDirectory);
            if (existing.isPresent()) {
                process = TesterProcess.reused(existing.get().pid());
            } else if (liveClientProbeRunning(workingDirectoryPath.resolve("echo-native/live-client-probe.json"))) {
                process = TesterProcess.reused(-1L);
            } else {
                process = startProcess(
                        executable,
                        libraryPath,
                        classpath,
                        resolvedMainClass,
                        descriptors,
                        gameDirectory,
                        assetsDirectory,
                        workingDirectoryPath,
                        stdoutPath,
                        stderrPath,
                        packId,
                        diagnostics
                );
            }
        }

        Path activationMarkerPath = workingDirectoryPath.resolve("echo-native/module-activation.json").normalize();
        Map<String, Object> activationMarker =
                waitForActivationMarker(activationMarkerPath, process.started() && !process.reused(), descriptors.size());
        boolean nativeProductModulesReady = nativeProductModulesReady(activationMarker, descriptors.size());
        if (!nativeProductModulesReady) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-PRODUCT-MODULES-PENDING-BRIDGE",
                    EchoNativeIssueSeverity.WARNING,
                    "Native product modules are discovered but not fully active in the live client yet",
                    "The tester launch is still waiting for the live activation marker to prove module activation, a complete Native Loader live proof, and MUTATED inventory/world/save/HUD runtime host surfaces.",
                    null,
                    packId,
                    List.of(relativePath(activationMarkerPath)),
                    "Keep the tester world open until module-activation.json includes a complete nativeLoaderLiveProof with all required live mutation surfaces, then rerun tester launch or playable-beta verification."
            ));
        }

        List<EchoNativeDiagnostic> sortedDiagnostics = diagnostics.stream()
                .sorted(Comparator.comparing(EchoNativeDiagnostic::code)
                        .thenComparing(diagnostic -> diagnostic.moduleId() == null ? "" : diagnostic.moduleId())
                        .thenComparing(EchoNativeDiagnostic::summary))
                .toList();

        return new EchoNativeTesterLaunchOutcome(
                packId,
                testerLaunchPlan(packId, fixture, classpath, mainClassResolved, workingDirectoryPath, assetsDirectory, nativesDirectory, canStart, sortedDiagnostics),
                testerLaunchSafetyGate(packId, canStart, assetsReady, extraction, sortedDiagnostics),
                testerLaunchProcess(packId, process, mainClassResolved, sortedDiagnostics),
                testerLaunchSupportPaths(packId, workingDirectoryPath, assetsDirectory, nativesDirectory, stdoutPath, stderrPath, sortedDiagnostics),
                productBootstrapActivationPlan(packId, descriptors, sortedDiagnostics),
                productModuleActivationStatus(packId, descriptors, activationMarker, sortedDiagnostics),
                sortedDiagnostics
        );
    }

    private static TesterProcess startProcess(
            String executable,
            String libraryPath,
            String classpath,
            String mainClass,
            List<EchoNativeAddonDescriptor> descriptors,
            Path gameDirectory,
            Path assetsDirectory,
            Path workingDirectory,
            Path stdoutPath,
            Path stderrPath,
            String packId,
            List<EchoNativeDiagnostic> diagnostics
    ) throws IOException {
        Files.createDirectories(workingDirectory);
        Files.createDirectories(stdoutPath.getParent());
        Files.createDirectories(assetsDirectory);
        String javaExecutable = executable.isBlank() ? "java" : executable;
        List<String> javaArgs = new ArrayList<>();
        if (!libraryPath.isBlank()) {
            javaArgs.add(resolveLibraryPath(libraryPath));
        }
        javaArgs.add("-Decho.native.loader=true");
        javaArgs.add("-D" + EchoNativeBootstrapMain.AUTHORIZED_HANDOFF_PROPERTY
                + "=" + EchoNativeBootstrapMain.AUTHORIZED_HANDOFF_VALUE);
        javaArgs.add("-Decho.native.gameDir=" + gameDirectory);
        nativeBootstrapProfile(descriptors).ifPresent(profile ->
                javaArgs.add("-Decho.native.bootstrap.profileClass=" + profile));
        javaArgs.add("-cp");
        javaArgs.add(resolveClasspath(classpath));
        javaArgs.add(EchoNativeBootstrapMain.MAIN_CLASS);
        javaArgs.add("--echo-marker");
        javaArgs.add(workingDirectory.resolve("echo-native/module-activation.json").toString());
        javaArgs.add("--echo-pack-id");
        javaArgs.add(packId);
        javaArgs.add("--echo-real-main");
        javaArgs.add(mainClass);
        javaArgs.add("--echo-handoff");
        descriptors.stream()
                .map(EchoNativeAddonDescriptor::id)
                .sorted()
                .forEach(moduleId -> {
                    javaArgs.add("--echo-module");
                    javaArgs.add(moduleId);
                });
        descriptors.stream()
                .sorted(Comparator.comparing(EchoNativeAddonDescriptor::id))
                .forEach(descriptor -> nativeEntrypoint(descriptor).ifPresent(entrypoint -> {
                    javaArgs.add("--echo-native-entrypoint");
                    javaArgs.add(descriptor.id() + "=" + entrypoint);
                }));
        javaArgs.add("--username");
        javaArgs.add("EchoNativeTester");
        javaArgs.add("--version");
        javaArgs.add(NATIVE_LOADER_VERSION);
        javaArgs.add("--gameDir");
        javaArgs.add(gameDirectory.toString());
        javaArgs.add("--assetsDir");
        javaArgs.add(assetsDirectory.toString());
        javaArgs.add("--assetIndex");
        javaArgs.add("30");
        javaArgs.add("--uuid");
        javaArgs.add("00000000000000000000000000000000");
        javaArgs.add("--accessToken");
        javaArgs.add("0");
        javaArgs.add("--versionType");
        javaArgs.add(NATIVE_LOADER_VERSION_TYPE);
        Path quickPlayPath = gameDirectory.resolve("echo-native/quickplay.json").normalize();
        Optional<Path> quickPlaySave = latestSave(gameDirectory.resolve("saves"));
        if (quickPlayEnabled(packId, descriptors) && quickPlaySave.isPresent()) {
            materializeQuickPlay(quickPlayPath, quickPlaySave.orElseThrow());
            javaArgs.add("--quickPlayPath");
            javaArgs.add(quickPlayPath.toString());
            javaArgs.add("--quickPlaySingleplayer");
            javaArgs.add(quickPlaySave.orElseThrow().getFileName().toString());
        }
        Path launchArgsPath = workingDirectory.resolve("echo-native/native-client.args").toAbsolutePath().normalize();
        writeJavaArgFile(launchArgsPath, javaArgs);

        try {
            Process process = new ProcessBuilder(List.of(javaExecutable, "@" + launchArgsPath))
                    .directory(workingDirectory.toFile())
                    .redirectOutput(stdoutPath.toFile())
                    .redirectError(stderrPath.toFile())
                    .start();
            boolean exitedDuringStartup = process.waitFor(3, TimeUnit.SECONDS);
            if (exitedDuringStartup) {
                diagnostics.add(new EchoNativeDiagnostic(
                        "ECHO-NATIVE-TESTER-LAUNCH-PROCESS-EXITED",
                        EchoNativeIssueSeverity.ERROR,
                        "Native Loader client exited during startup",
                        "The authorized tester launch process started but exited with code " + process.exitValue()
                                + " before the client stayed open.",
                        null,
                        packId,
                        List.of(relativePath(stdoutPath), relativePath(stderrPath), relativePath(launchArgsPath)),
                        "Open the captured stdout/stderr logs and fix the startup failure before retrying Start Native Loader Client."
                ));
                return new TesterProcess(
                        true,
                        false,
                        process.pid(),
                        relativePath(stdoutPath),
                        relativePath(stderrPath),
                        relativePath(launchArgsPath),
                        "",
                        false,
                        process.exitValue()
                );
            }
            return new TesterProcess(
                    true,
                    false,
                    process.pid(),
                    relativePath(stdoutPath),
                    relativePath(stderrPath),
                    relativePath(launchArgsPath),
                    "",
                    true,
                    null
            );
        } catch (IOException exception) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-TESTER-LAUNCH-PROCESS-START-FAILED",
                    EchoNativeIssueSeverity.ERROR,
                    "Tester launch process could not start",
                    "The authorized tester launch command could not start the Java process: " + exception.getMessage(),
                    null,
                    packId,
                    List.of("reports/echo-native/" + packId + "/tester-launch-process.json"),
                    "Check the local Java executable, fixture-local classpath, isolated assets, and isolated natives before retrying."
            ));
            return TesterProcess.failed(relativePath(launchArgsPath), exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-TESTER-LAUNCH-PROCESS-INTERRUPTED",
                    EchoNativeIssueSeverity.ERROR,
                    "Native Loader client startup probe was interrupted",
                    "The authorized tester launch command was interrupted before it could verify the client stayed open.",
                    null,
                    packId,
                    List.of(relativePath(stdoutPath), relativePath(stderrPath), relativePath(launchArgsPath)),
                    "Retry Start Native Loader Client after stopping any stale native launch commands."
            ));
            return new TesterProcess(
                    true,
                    false,
                    processIdOrUnknown(),
                    relativePath(stdoutPath),
                    relativePath(stderrPath),
                    relativePath(launchArgsPath),
                    exception.toString(),
                    false,
                    null
            );
        }
    }

    private static void writeJavaArgFile(Path launchArgsPath, List<String> javaArgs) throws IOException {
        Files.createDirectories(launchArgsPath.getParent());
        StringBuilder builder = new StringBuilder();
        for (String arg : javaArgs) {
            builder.append(javaArgFileToken(arg)).append(System.lineSeparator());
        }
        Files.writeString(launchArgsPath, builder.toString(), StandardCharsets.UTF_8);
    }

    private static String javaArgFileToken(String arg) {
        if (arg == null || arg.isEmpty()) {
            return "\"\"";
        }
        boolean quoted = arg.chars().anyMatch(Character::isWhitespace)
                || arg.startsWith("@")
                || arg.startsWith("#")
                || arg.contains("\"");
        if (!quoted) {
            return arg;
        }
        return "\"" + arg.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private static Optional<Path> latestSave(Path savesDirectory) {
        if (!Files.isDirectory(savesDirectory)) {
            return Optional.empty();
        }
        try (var stream = Files.list(savesDirectory)) {
            return stream
                    .filter(Files::isDirectory)
                    .max(Comparator.comparing(EchoNativeTesterLauncher::lastModifiedOrEpoch)
                            .thenComparing(path -> path.getFileName().toString()));
        } catch (IOException exception) {
            return Optional.empty();
        }
    }

    private static boolean quickPlayEnabled(String packId, List<EchoNativeAddonDescriptor> descriptors) {
        String property = System.getProperty("echo.native.tester.quickPlay", "");
        if (!property.isBlank()) {
            return Boolean.parseBoolean(property);
        }
        String env = System.getenv("ECHO_NATIVE_TESTER_QUICKPLAY");
        if (env != null && !env.isBlank()) {
            return Boolean.parseBoolean(env);
        }
        return !"ashfall".equalsIgnoreCase(packId)
                && nativeBootstrapProfile(descriptors)
                .map(profile -> !profile.toLowerCase(Locale.ROOT).contains("ashfall"))
                .orElse(true);
    }

    private static void materializeQuickPlay(Path quickPlayPath, Path save) throws IOException {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("type", "singleplayer");
        entry.put("id", save.getFileName().toString());
        entry.put("name", save.getFileName().toString());
        entry.put("gamemode", "creative");
        entry.put("source", "EchoNativeTesterLauncher.materializeQuickPlay");
        Path levelDat = save.resolve("level.dat");
        if (Files.isRegularFile(levelDat)) {
            entry.put("levelDatLastModified", Files.getLastModifiedTime(levelDat).toInstant().toString());
        }
        Files.createDirectories(quickPlayPath.getParent());
        Files.writeString(quickPlayPath, EchoNativeJson.write(List.of(entry)), StandardCharsets.UTF_8);
    }

    private static Instant lastModifiedOrEpoch(Path path) {
        try {
            return Files.getLastModifiedTime(path).toInstant();
        } catch (IOException exception) {
            return Instant.EPOCH;
        }
    }

    private static Optional<ProcessHandle> findExistingTesterProcess(Path gameDirectory) {
        Path normalizedGameDirectory = gameDirectory.toAbsolutePath().normalize();
        String gameDir = normalizedGameDirectory.toString();
        String gameDirLower = gameDir.toLowerCase(Locale.ROOT);
        String argFileLower = normalizedGameDirectory.resolve("echo-native/native-client.args")
                .normalize()
                .toString()
                .toLowerCase(Locale.ROOT);
        return ProcessHandle.allProcesses()
                .filter(handle -> {
                    ProcessHandle.Info info = handle.info();
                    String command = info.command().orElse("").toLowerCase(Locale.ROOT);
                    if (!command.endsWith("java.exe") && !command.endsWith("java")) {
                        return false;
                    }
                    String arguments = info.arguments()
                            .map(items -> String.join(" ", items))
                            .orElse("")
                            .toLowerCase(Locale.ROOT);
                    String commandLine = info.commandLine().orElse("").toLowerCase(Locale.ROOT);
                    return arguments.contains(gameDirLower)
                            || arguments.contains(argFileLower)
                            || commandLine.contains(gameDirLower)
                            || commandLine.contains(argFileLower);
                })
                .findFirst();
    }

    private static boolean liveClientProbeRunning(Path probePath) {
        Map<String, Object> probe = readJsonObject(probePath);
        return Boolean.TRUE.equals(probe.get("minecraftRunning"))
                && Boolean.TRUE.equals(probe.get("windowPresent"));
    }

    private static Map<String, Object> testerLaunchPlan(
            String packId,
            Path fixture,
            String classpath,
            boolean mainClassResolved,
            Path workingDirectory,
            Path assetsDirectory,
            Path nativesDirectory,
            boolean canStart,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_tester_launch_plan", diagnostics);
        data.put("authorizationFlag", "--authorized");
        data.put("classpathEntryCount", classpath.isBlank() ? 0 : classpath.split(";").length);
        data.put("fixture", relativePath(fixture));
        data.put("bootstrapMainClass", EchoNativeBootstrapMain.MAIN_CLASS);
        data.put("mainClassResolved", mainClassResolved);
        data.put("processEntrypoint", "echo_native_bootstrap_wrapper");
        data.put("processStartEligible", canStart);
        data.put("testerLaunchCommandReady", canStart);
        data.put("labelText", NATIVE_LOADER_TESTER_LABEL);
        data.put("nativeLoaderTextLabelApplied", canStart);
        data.put("workingDirectory", relativePath(workingDirectory));
        data.put("assetsDirectory", relativePath(assetsDirectory));
        data.put("nativesDirectory", relativePath(nativesDirectory));
        data.put("summary", canStart
                ? "Tester launch command is ready to start or reuse an isolated native-loader Minecraft process."
                : "Tester launch command is blocked by missing fixture-local runtime inputs.");
        data.put("packId", packId);
        return data;
    }

    private static Optional<String> nativeEntrypoint(EchoNativeAddonDescriptor descriptor) {
        Object raw = descriptor.access().get("nativeEntrypoint");
        String value = raw == null ? "" : String.valueOf(raw).trim();
        return value.isBlank() ? Optional.empty() : Optional.of(value);
    }

    private static Optional<String> nativeBootstrapProfile(List<EchoNativeAddonDescriptor> descriptors) {
        return descriptors.stream()
                .sorted(Comparator.comparing(EchoNativeAddonDescriptor::id))
                .map(descriptor -> descriptor.access().get("nativeBootstrapProfile"))
                .map(raw -> raw == null ? "" : String.valueOf(raw).trim())
                .filter(value -> !value.isBlank())
                .findFirst();
    }

    private static Map<String, Object> testerLaunchSafetyGate(
            String packId,
            boolean canStart,
            boolean assetsReady,
            NativeExtractionRun extraction,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_tester_launch_safety_gate", diagnostics);
        data.put("assetsReady", assetsReady);
        data.put("automaticLaunchAllowed", false);
        data.put("operatorAuthorizationAccepted", true);
        data.put("processExecutionAllowed", canStart);
        data.put("safetyGatePassed", canStart);
        data.put("testerLaunchAllowed", canStart);
        putNativeExtraction(data, extraction);
        data.put("packId", packId);
        data.put("summary", canStart
                ? "Tester launch safety gate allows an explicitly authorized isolated process."
                : "Tester launch safety gate is blocked by diagnostics.");
        return data;
    }

    private static Map<String, Object> testerLaunchProcess(
            String packId,
            TesterProcess process,
            boolean mainClassResolved,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_tester_launch_process", diagnostics);
        data.put("commandExecuted", process.started() && !process.reused());
        data.put("gameClassesResolved", process.started());
        data.put("gameProcessLaunched", process.started());
        data.put("bootstrapMainClass", EchoNativeBootstrapMain.MAIN_CLASS);
        data.put("processEntrypoint", process.started() ? "echo_native_bootstrap_wrapper" : "not_started");
        data.put("mainClassResolved", mainClassResolved);
        data.put("minecraftLaunchAttempted", process.started());
        data.put("minecraftProcessStarted", process.started());
        data.put("packId", packId);
        data.put("processDetachedForTesting", process.started());
        data.put("processId", process.processId());
        data.put("processLaunched", process.started());
        data.put("processReused", process.reused());
        data.put("aliveAfterStartupProbe", process.aliveAfterStartupProbe());
        data.put("exitCodeAfterStartupProbe", process.exitCodeAfterStartupProbe());
        data.put("labelText", NATIVE_LOADER_TESTER_LABEL);
        data.put("nativeLoaderTextLabelApplied", process.started());
        data.put("launchArgsFile", process.launchArgsFile());
        data.put("processStartFailure", process.processStartFailure());
        data.put("stderrLog", process.stderrLog());
        data.put("stdoutLog", process.stdoutLog());
        data.put("summary", process.started()
                ? "Tester launch process is available for manual native-loader testing through the ECHO native bootstrap wrapper."
                : "Tester launch process was not started.");
        return data;
    }

    private static Map<String, Object> testerLaunchSupportPaths(
            String packId,
            Path workingDirectory,
            Path assetsDirectory,
            Path nativesDirectory,
            Path stdoutPath,
            Path stderrPath,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_tester_launch_support_paths", diagnostics);
        data.put("assetsDirectory", relativePath(assetsDirectory));
        data.put("crashReportsDirectory", relativePath(workingDirectory.resolve("crash-reports")));
        data.put("logsDirectory", relativePath(workingDirectory.resolve("logs")));
        data.put("nativesDirectory", relativePath(nativesDirectory));
        data.put("packId", packId);
        data.put("savesDirectory", relativePath(workingDirectory.resolve("saves")));
        data.put("stderrLog", relativePath(stderrPath));
        data.put("stdoutLog", relativePath(stdoutPath));
        data.put("supportBundleReady", true);
        data.put("workingDirectory", relativePath(workingDirectory));
        data.put("summary", "Tester launch support paths are fixture-local and isolated from user installs.");
        return data;
    }

    private static Map<String, Object> productBootstrapActivationPlan(
            String packId,
            List<EchoNativeAddonDescriptor> descriptors,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base(productPhase(packId, "bootstrap_activation_plan"), diagnostics);
        List<String> moduleIds = descriptors.stream().map(EchoNativeAddonDescriptor::id).sorted().toList();
        data.put("descriptorCount", descriptors.size());
        data.put("moduleIds", moduleIds);
        data.put("nativeBootstrapStarted", true);
        data.put("packId", packId);
        data.put("runtimeBridgeRequired", true);
        data.put("summary", "Native product module descriptors are ready for bootstrap activation, but live module execution still requires the native runtime bridge.");
        return data;
    }

    static Map<String, Object> productModuleActivationStatus(
            String packId,
            List<EchoNativeAddonDescriptor> descriptors,
            Map<String, Object> activationMarker,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        return EchoNativeProductActivationStatus.productModuleActivationStatus(
                packId,
                descriptors,
                activationMarker,
                diagnostics);
    }

    private static Map<String, Object> waitForActivationMarker(
            Path markerPath,
            boolean processStarted,
            int descriptorCount
    ) {
        Map<String, Object> latest = readJsonObject(markerPath);
        if (!processStarted || nativeProductModulesReady(latest, descriptorCount)) {
            return latest;
        }
        for (int attempt = 0; attempt < 900; attempt++) {
            latest = readJsonObject(markerPath);
            if (nativeProductModulesReady(latest, descriptorCount)) {
                return latest;
            }
            try {
                Thread.sleep(100L);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                return latest;
            }
        }
        return latest;
    }

    private static boolean nativeProductModulesReady(Map<String, Object> activationMarker, int descriptorCount) {
        return EchoNativePlayableModuleGate.nativeProductModulesReady(
                activationMarker,
                descriptorCount,
                nativeActivationCount(activationMarker));
    }

    private static int nativeActivationCount(Map<String, Object> activationMarker) {
        return (int) nativeActivationsById(activationMarker).values().stream()
                .filter(EchoNativeTesterLauncher::nativeActivationVerified)
                .count();
    }

    private static boolean nativeActivationVerified(Map<String, Object> nativeActivation) {
        return Boolean.TRUE.equals(nativeActivation.get("activated"))
                && Boolean.TRUE.equals(nativeActivation.get("nativeAdapterCodeExecuted"))
                && !String.valueOf(nativeActivation.getOrDefault("entrypoint", "")).isBlank()
                && !String.valueOf(nativeActivation.getOrDefault("loadedClassName", "")).isBlank();
    }

    private static Map<String, Map<String, Object>> liveModulesById(Map<String, Object> activationMarker) {
        Map<String, Map<String, Object>> modulesById = new LinkedHashMap<>();
        Object modules = activationMarker.get("modules");
        if (!(modules instanceof Iterable<?> iterable)) {
            return modulesById;
        }
        for (Object raw : iterable) {
            Map<String, Object> module = EchoNativeJson.asObject(raw);
            String id = String.valueOf(module.getOrDefault("id", ""));
            if (!id.isBlank()) {
                modulesById.put(id, module);
            }
        }
        return modulesById;
    }

    private static Map<String, Map<String, Object>> nativeActivationsById(Map<String, Object> activationMarker) {
        Map<String, Map<String, Object>> activationsById = new LinkedHashMap<>();
        Object activations = activationMarker.get("nativeActivations");
        if (!(activations instanceof Iterable<?> iterable)) {
            return activationsById;
        }
        for (Object raw : iterable) {
            Map<String, Object> activation = EchoNativeJson.asObject(raw);
            String id = String.valueOf(activation.getOrDefault("moduleId", ""));
            if (!id.isBlank()) {
                activationsById.put(id, activation);
            }
        }
        return activationsById;
    }

    private static Map<String, Object> readJsonObject(Path path) {
        if (!Files.isRegularFile(path)) {
            return Map.of();
        }
        try {
            return EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(path)));
        } catch (IOException | RuntimeException exception) {
            return Map.of();
        }
    }

    private static NativeExtractionRun materializeNatives(
            Path fixture,
            Path nativesDirectory,
            String packId,
            List<EchoNativeDiagnostic> diagnostics
    ) throws IOException {
        Path archive = fixture.resolve("local-runtime/minecraft/26.1.2/natives/minecraft-26.1.2-natives.zip");
        if (!Files.isRegularFile(archive)) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-TESTER-LAUNCH-NATIVE-ARCHIVE-MISSING",
                    EchoNativeIssueSeverity.ERROR,
                    "Tester launch native archive is missing",
                    "The tester launch command requires the fixture-local native archive.",
                    null,
                    packId,
                    List.of(relativePath(archive)),
                    "Stage the reviewed fixture-local native archive before retrying."
            ));
            return new NativeExtractionRun(true, false, archive, nativesDirectory, List.of());
        }

        Files.createDirectories(nativesDirectory);
        List<String> extracted = new ArrayList<>();
        try (ZipInputStream outer = new ZipInputStream(Files.newInputStream(archive))) {
            ZipEntry nativeJarEntry;
            while ((nativeJarEntry = outer.getNextEntry()) != null) {
                if (nativeJarEntry.isDirectory() || !nativeJarEntry.getName().toLowerCase(Locale.ROOT).endsWith(".jar")) {
                    continue;
                }
                byte[] jarBytes = readAll(outer);
                try (ZipInputStream nativeJar = new ZipInputStream(new ByteArrayInputStream(jarBytes))) {
                    ZipEntry nativeEntry;
                    while ((nativeEntry = nativeJar.getNextEntry()) != null) {
                        if (nativeEntry.isDirectory() || !isNativeLibrary(nativeEntry.getName())) {
                            continue;
                        }
                        String fileName = Path.of(nativeEntry.getName()).getFileName().toString();
                        Path target = nativesDirectory.resolve(fileName).normalize();
                        if (!target.startsWith(nativesDirectory)) {
                            throw new IOException("Native archive entry escaped isolated natives directory: " + nativeEntry.getName());
                        }
                        writeNativeFileIfChanged(target, readAll(nativeJar));
                        extracted.add(relativePath(target));
                    }
                }
            }
        }
        List<String> sortedExtracted = extracted.stream().sorted().toList();
        return new NativeExtractionRun(true, !sortedExtracted.isEmpty(), archive, nativesDirectory, sortedExtracted);
    }

    private static boolean isNativeLibrary(String entryName) {
        String lower = entryName.toLowerCase(Locale.ROOT);
        return lower.endsWith(".dll") || lower.endsWith(".so") || lower.endsWith(".dylib");
    }

    private static byte[] readAll(ZipInputStream stream) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        stream.transferTo(output);
        return output.toByteArray();
    }

    private static void writeNativeFileIfChanged(Path target, byte[] bytes) throws IOException {
        if (Files.isRegularFile(target)) {
            try {
                if (Arrays.equals(Files.readAllBytes(target), bytes)) {
                    return;
                }
            } catch (IOException ignored) {
                // Try the write below so locked or unreadable changed natives still fail with the target path.
            }
        }
        Files.write(target, bytes);
    }

    private static void putNativeExtraction(Map<String, Object> data, NativeExtractionRun extraction) {
        data.put("isolatedRuntimeFilesystemMutated", extraction.started() && extraction.ready());
        data.put("nativeArchive", relativePath(extraction.archive()));
        data.put("nativeExtractionReady", extraction.ready());
        data.put("nativeExtractionScope", "fixture_isolated_runtime");
        data.put("nativeExtractionStarted", extraction.started());
        data.put("nativeFilesExtracted", extraction.extractedFiles().size());
        data.put("nativesDirectory", relativePath(extraction.nativesDirectory()));
        data.put("userCacheMutated", false);
        data.put("userInstallMutated", false);
    }

    private static String resolveMainClass(
            Path fixture,
            String previewMainClass,
            List<EchoNativeDiagnostic> diagnostics,
            String packId
    ) {
        if (!previewMainClass.isBlank() && !PLACEHOLDER_MAIN_CLASS.equals(previewMainClass)) {
            return previewMainClass;
        }
        Path versionManifest = fixture.resolve("local-runtime/minecraft/26.1.2/metadata/26.1.2.json");
        if (!Files.isRegularFile(versionManifest)) {
            return previewMainClass;
        }
        try {
            Map<String, Object> manifest = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(versionManifest)));
            String mainClass = String.valueOf(manifest.getOrDefault("mainClass", ""));
            if (!mainClass.isBlank()) {
                return mainClass;
            }
        } catch (RuntimeException | IOException exception) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-TESTER-LAUNCH-MANIFEST-INVALID",
                    EchoNativeIssueSeverity.ERROR,
                    "Fixture-local version manifest could not be parsed",
                    "The tester launch command could not parse the fixture-local version manifest.",
                    null,
                    packId,
                    List.of(relativePath(versionManifest)),
                    "Replace the fixture-local version manifest with reviewed valid JSON."
            ));
        }
        return previewMainClass;
    }

    private static String previewArgument(Map<String, Object> preview, String id) {
        Object arguments = preview.get("commandLinePreview");
        if (!(arguments instanceof List<?> list)) {
            return "";
        }
        for (Object raw : list) {
            Map<String, Object> item = EchoNativeJson.asObject(raw);
            if (id.equals(String.valueOf(item.get("id")))) {
                return String.valueOf(item.getOrDefault("value", ""));
            }
        }
        return "";
    }

    private static String resolveClasspath(String classpath) {
        Path root = Path.of("").toAbsolutePath().normalize();
        List<String> entries = new ArrayList<>();
        addProjectOutputEntries(entries, root, "echo-native-contracts");
        addProjectOutputEntries(entries, root, "echo-native-diagnostics");
        addProjectOutputEntries(entries, root, "echo-native-packos");
        addProjectOutputEntries(entries, root, "echo-native-loader");
        addProjectOutputEntries(entries, root, "echo-native-bootstrap-api");
        for (String raw : classpath.split(";")) {
            if (raw.isBlank()) {
                continue;
            }
            Path path = Path.of(raw);
            addIfPresent(entries, path.isAbsolute() ? path.normalize() : root.resolve(path).normalize());
        }
        addIfPresent(entries, root.resolve("echo-native-diagnostics/build/libs/echo-native-diagnostics-" + NATIVE_PLATFORM_ARTIFACT_VERSION + ".jar"));
        addIfPresent(entries, root.resolve("echo-native-packos/build/libs/echo-native-packos-" + NATIVE_PLATFORM_ARTIFACT_VERSION + ".jar"));
        return String.join(System.getProperty("path.separator"), entries);
    }

    private static void addProjectOutputEntries(List<String> entries, Path root, String projectName) {
        addIfPresent(entries, root.resolve(projectName + "/build/classes/java/main"));
        addIfPresent(entries, root.resolve(projectName + "/build/resources/main"));
        addIfPresent(entries, root.resolve(projectName + "/build/libs/" + projectName + "-" + NATIVE_PLATFORM_ARTIFACT_VERSION + ".jar"));
    }

    private static void addIfPresent(List<String> entries, Path path) {
        Path normalized = path.normalize();
        String value = normalized.toString();
        if (Files.exists(normalized) && !entries.contains(value)) {
            entries.add(value);
        }
    }

    private static String resolveLibraryPath(String libraryPath) {
        String prefix = "-Djava.library.path=";
        if (!libraryPath.startsWith(prefix)) {
            return libraryPath;
        }
        String raw = libraryPath.substring(prefix.length());
        Path path = Path.of(raw);
        Path root = Path.of("").toAbsolutePath().normalize();
        Path resolved = path.isAbsolute() ? path.normalize() : root.resolve(path).normalize();
        return prefix + resolved;
    }

    private static Path resolveArgumentPath(String value) {
        Path path = Path.of(value);
        Path root = Path.of("").toAbsolutePath().normalize();
        return path.isAbsolute() ? path.normalize() : root.resolve(path).normalize();
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
                    "ECHO-NATIVE-TESTER-LAUNCH-REPORT-MISSING",
                    EchoNativeIssueSeverity.ERROR,
                    "Tester launch required report missing",
                    "The tester launch command requires " + reportName + ".",
                    null,
                    packId,
                    List.of(fixture.toString().replace('\\', '/')),
                    "Regenerate native loader beta readiness reports before tester launch."
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
        if (!"PASS".equals(String.valueOf(report.getOrDefault("status", "MISSING")))) {
            boolean liveContentOnly = isLiveContentOnlyBlocker(reportName, report);
            diagnostics.add(new EchoNativeDiagnostic(
                    liveContentOnly ? "ECHO-NATIVE-TESTER-LAUNCH-LIVE-CONTENT-PENDING" : "ECHO-NATIVE-TESTER-LAUNCH-UPSTREAM-BLOCKED",
                    liveContentOnly ? EchoNativeIssueSeverity.WARNING : EchoNativeIssueSeverity.ERROR,
                    liveContentOnly ? "Tester launch is collecting live content evidence" : "Tester launch upstream report is not PASS",
                    liveContentOnly
                            ? "The tester launch command is allowed to start so AdapterCore can prove live creative content visibility."
                            : "The tester launch command requires PASS " + reportName + ".",
                    null,
                    packId,
                    List.of(relativePath(path)),
                    liveContentOnly
                            ? "Open a world and the creative inventory, then rerun the native loader beta gate to promote this warning to PASS."
                            : "Resolve upstream native loader beta diagnostics before tester launch."
            ));
        }
    }

    private static boolean isLiveContentOnlyBlocker(String reportName, Map<String, Object> report) {
        if (!"phase13-native-loader-beta-gate.json".equals(reportName)
                && !"process-execution-readiness.json".equals(reportName)) {
            return false;
        }
        Object issues = report.get("issues");
        if (!(issues instanceof Iterable<?> iterable)) {
            return false;
        }
        int issueCount = 0;
        for (Object issue : iterable) {
            Map<String, Object> issueMap = EchoNativeJson.asObject(issue);
            if (!"ECHO-NATIVE-BETA-LIVE-CONTENT-BLOCKED".equals(String.valueOf(issueMap.get("code")))) {
                return false;
            }
            issueCount++;
        }
        return issueCount > 0;
    }

    private static Map<String, Object> data(Map<String, Object> report) {
        return EchoNativeJson.asObject(report == null ? null : report.get("data"));
    }

    private static String productPhase(String packId, String suffix) {
        return "phase13_" + phaseSegment(packId) + "_" + suffix;
    }

    private static String phaseSegment(String value) {
        String text = value == null ? "" : value.toLowerCase(Locale.ROOT);
        StringBuilder result = new StringBuilder();
        boolean previousSeparator = false;
        for (int index = 0; index < text.length(); index++) {
            char ch = text.charAt(index);
            if ((ch >= 'a' && ch <= 'z') || (ch >= '0' && ch <= '9')) {
                result.append(ch);
                previousSeparator = false;
            } else if (!previousSeparator) {
                result.append('_');
                previousSeparator = true;
            }
        }
        String normalized = result.toString();
        while (normalized.endsWith("_")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized.isBlank() ? "native_product" : normalized;
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
        data.put("filesystemMutated", false);
        data.put("gameClassesResolved", false);
        data.put("generatedEvidenceAt", Instant.EPOCH.toString());
        data.put("libraryDownloadStarted", false);
        data.put("minecraftLaunched", false);
        data.put("nativeExtractionStarted", false);
        data.put("phase", phase);
        data.put("registryInjected", false);
        data.put("registryMutated", false);
        data.put("transformsEnabled", false);
        data.put("transformsPerformed", false);
        data.put("unsafeRuntimeWorkStarted", false);
        return data;
    }

    private static boolean isBlocking(EchoNativeDiagnostic diagnostic) {
        return diagnostic.severity() == EchoNativeIssueSeverity.ERROR
                || diagnostic.severity() == EchoNativeIssueSeverity.FATAL;
    }

    private static Path resolveWorkspacePath(String path) {
        Path value = Path.of(path);
        if (value.isAbsolute()) {
            return value.normalize();
        }
        return Path.of("").toAbsolutePath().normalize().resolve(value).normalize();
    }

    private static String relativePath(Path path) {
        Path root = Path.of("").toAbsolutePath().normalize();
        Path normalized = path.toAbsolutePath().normalize();
        if (normalized.startsWith(root)) {
            return root.relativize(normalized).toString().replace('\\', '/');
        }
        return path.toString().replace('\\', '/');
    }

    private record NativeExtractionRun(
            boolean started,
            boolean ready,
            Path archive,
            Path nativesDirectory,
            List<String> extractedFiles
    ) {
        private static NativeExtractionRun skipped(Path nativesDirectory) {
            return new NativeExtractionRun(false, false, Path.of(""), nativesDirectory, List.of());
        }
    }

    private record TesterProcess(
            boolean started,
            boolean reused,
            long processId,
            String stdoutLog,
            String stderrLog,
            String launchArgsFile,
            String processStartFailure,
            boolean aliveAfterStartupProbe,
            Integer exitCodeAfterStartupProbe
    ) {
        private static TesterProcess skipped() {
            return new TesterProcess(false, false, -1L, "", "", "", "", false, null);
        }

        private static TesterProcess failed(String launchArgsFile, IOException exception) {
            return new TesterProcess(false, false, -1L, "", "", launchArgsFile, exception.toString(), false, null);
        }

        private static TesterProcess reused(long processId) {
            return new TesterProcess(true, true, processId, "", "", "", "", true, null);
        }
    }

    private static long processIdOrUnknown() {
        return -1L;
    }
}
