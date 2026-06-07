package dev.echo.nativeplatform.cli;

import dev.echo.nativeplatform.contracts.EchoNativeAddonDescriptor;
import dev.echo.nativeplatform.contracts.EchoNativeDiagnostic;
import dev.echo.nativeplatform.contracts.EchoNativeIssueSeverity;
import dev.echo.nativeplatform.diagnostics.EchoNativeJson;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class EchoNativeTesterEvidenceIntake {
    private static final DateTimeFormatter CRASH_REPORT_TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH.mm.ss");

    EchoNativeTesterEvidenceOutcome intake(
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
            checkAcceptedReport(entry.getKey(), entry.getValue(), report, packId, diagnostics);
        }

        Path gameDirectory = fixture.resolve("isolated-runtime/game").normalize();
        Path latestLog = gameDirectory.resolve("logs/latest.log").normalize();
        Path savesDirectory = gameDirectory.resolve("saves").normalize();
        Path crashReportsDirectory = gameDirectory.resolve("crash-reports").normalize();
        Path screenshotsDirectory = gameDirectory.resolve("screenshots").normalize();

        String latestLogText = "";
        if (Files.isRegularFile(latestLog)) {
            latestLogText = Files.readString(latestLog);
        } else {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-TESTER-EVIDENCE-LATEST-LOG-MISSING",
                    EchoNativeIssueSeverity.ERROR,
                    "Tester evidence latest.log is missing",
                    "The tester evidence intake requires the isolated runtime latest.log to verify the playable baseline.",
                    null,
                    packId,
                    List.of(relativePath(latestLog)),
                    "Run the authorized tester launch and load a world before taking tester evidence."
            ));
        }

        List<String> saveNames = directoryNames(savesDirectory);
        List<String> crashReports = fileNames(crashReportsDirectory);
        LocalDateTime latestLogSessionStart = inferLatestLogSessionStart(latestLog, latestLogText);
        List<String> activeCrashReports = activeCrashReports(crashReports, latestLogSessionStart);
        List<String> staleCrashReports = crashReports.stream()
                .filter(report -> !activeCrashReports.contains(report))
                .toList();
        List<String> screenshots = fileNames(screenshotsDirectory);
        boolean playerJoined = latestLogText.contains("EchoNativeTester joined the game");
        boolean spawnPrepared = containsAny(latestLogText, "Preparing spawn area", "Preparing spawn");
        boolean chunksSaved = latestLogText.contains("Saving chunks for level");
        boolean cleanShutdown = containsAny(latestLogText, "Stopping server", "Stopping!");
        boolean resourcesLoaded = containsAny(latestLogText, "Loaded 1515 recipes", "Loaded 1617 advancements", "Created: 1024x1024x0");
        boolean crashSignal = containsAny(
                latestLogText.toLowerCase(Locale.ROOT),
                "reported exception",
                "crash report",
                "exception in thread",
                "failed to start"
        );
        boolean worldSavePresent = !saveNames.isEmpty();
        boolean noCrashReports = activeCrashReports.isEmpty() && !crashSignal;
        boolean baselinePlayable = playerJoined && worldSavePresent && noCrashReports;

        if (!playerJoined) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-TESTER-EVIDENCE-JOIN-MISSING",
                    EchoNativeIssueSeverity.ERROR,
                    "Tester join evidence was not found",
                    "The isolated runtime latest.log does not show EchoNativeTester joining a world.",
                    null,
                    packId,
                    List.of(relativePath(latestLog)),
                    "Load or create a world through the tester launch command, then rerun evidence intake."
            ));
        }
        if (!worldSavePresent) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-TESTER-EVIDENCE-SAVE-MISSING",
                    EchoNativeIssueSeverity.ERROR,
                    "Tester world save evidence was not found",
                    "The isolated runtime saves directory does not contain a world save.",
                    null,
                    packId,
                    List.of(relativePath(savesDirectory)),
                    "Create or load a world through the tester launch command, then rerun evidence intake."
            ));
        }
        if (!noCrashReports) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-TESTER-EVIDENCE-CRASH-SIGNAL",
                    EchoNativeIssueSeverity.ERROR,
                    "Tester evidence contains crash signals",
                    "The isolated runtime contains crash reports or latest.log crash indicators.",
                    null,
                    packId,
                    List.of(relativePath(crashReportsDirectory), relativePath(latestLog)),
                    "Inspect the isolated crash reports and latest.log before moving the native loader beta forward."
            ));
        }

        Path activationMarkerPath = gameDirectory.resolve("echo-native/module-activation.json").normalize();
        Map<String, Object> activationMarker = readJsonObject(activationMarkerPath);
        int activeModuleCount = activeModuleCount(reports.get("native-product-module-activation-status.json"), activationMarker);
        int pendingBridgeCount = Math.max(0, descriptors.size() - activeModuleCount);
        boolean nativeProductModulesReady = nativeProductModulesReady(activationMarker, descriptors.size());
        if (!nativeProductModulesReady) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-PRODUCT-MODULES-PENDING-BRIDGE",
                    EchoNativeIssueSeverity.WARNING,
                    "Minecraft baseline is playable but native product modules still need live activation proof",
                    "Tester evidence proves the isolated native Minecraft path can create and load a world, but the live activation marker has not verified a complete Native Loader live proof with MUTATED inventory/world/save/HUD runtime host surfaces yet.",
                    null,
                    packId,
                    List.of(relativePath(activationMarkerPath)),
                    "Keep the tester world open until module-activation.json includes a complete nativeLoaderLiveProof with all required live mutation surfaces, then rerun evidence intake."
            ));
        }

        List<EchoNativeDiagnostic> sortedDiagnostics = diagnostics.stream()
                .sorted(Comparator.comparing(EchoNativeDiagnostic::code)
                        .thenComparing(diagnostic -> diagnostic.moduleId() == null ? "" : diagnostic.moduleId())
                        .thenComparing(EchoNativeDiagnostic::summary))
                .toList();

        return new EchoNativeTesterEvidenceOutcome(
                packId,
                testerPlayableEvidence(packId, latestLog, savesDirectory, crashReportsDirectory, screenshotsDirectory, saveNames, crashReports, activeCrashReports, staleCrashReports, latestLogSessionStart, screenshots, playerJoined, spawnPrepared, chunksSaved, cleanShutdown, resourcesLoaded, crashSignal, baselinePlayable, sortedDiagnostics),
                minecraftBaselinePlayability(packId, playerJoined, spawnPrepared, chunksSaved, cleanShutdown, resourcesLoaded, worldSavePresent, noCrashReports, baselinePlayable, sortedDiagnostics),
                nativeProductPlayableGap(packId, descriptors, activeModuleCount, pendingBridgeCount, baselinePlayable, activationMarker, nativeProductModulesReady, sortedDiagnostics),
                phase13M20Completion(packId, baselinePlayable, activeModuleCount, pendingBridgeCount, sortedDiagnostics),
                phase13M21Readiness(packId, baselinePlayable, pendingBridgeCount, sortedDiagnostics),
                sortedDiagnostics
        );
    }

    private static Map<String, Object> testerPlayableEvidence(
            String packId,
            Path latestLog,
            Path savesDirectory,
            Path crashReportsDirectory,
            Path screenshotsDirectory,
            List<String> saveNames,
            List<String> crashReports,
            List<String> activeCrashReports,
            List<String> staleCrashReports,
            LocalDateTime latestLogSessionStart,
            List<String> screenshots,
            boolean playerJoined,
            boolean spawnPrepared,
            boolean chunksSaved,
            boolean cleanShutdown,
            boolean resourcesLoaded,
            boolean crashSignal,
            boolean baselinePlayable,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_tester_playable_evidence", diagnostics);
        data.put("baselinePlayableEvidence", baselinePlayable);
        data.put("chunksSaved", chunksSaved);
        data.put("cleanShutdownObserved", cleanShutdown);
        data.put("crashReportCount", crashReports.size());
        data.put("crashReports", crashReports);
        data.put("activeCrashReportCount", activeCrashReports.size());
        data.put("activeCrashReports", activeCrashReports);
        data.put("staleCrashReportCount", staleCrashReports.size());
        data.put("staleCrashReports", staleCrashReports);
        data.put("crashSignalInLatestLog", crashSignal);
        data.put("crashReportsDirectory", relativePath(crashReportsDirectory));
        data.put("latestLog", relativePath(latestLog));
        data.put("latestLogPresent", Files.isRegularFile(latestLog));
        data.put("latestLogSessionStart", latestLogSessionStart == null ? "" : latestLogSessionStart.toString());
        data.put("packId", packId);
        data.put("playerJoinObserved", playerJoined);
        data.put("resourcesLoaded", resourcesLoaded);
        data.put("saveNames", saveNames);
        data.put("savesDirectory", relativePath(savesDirectory));
        data.put("screenshotCount", screenshots.size());
        data.put("screenshots", screenshots);
        data.put("screenshotsDirectory", relativePath(screenshotsDirectory));
        data.put("spawnPrepared", spawnPrepared);
        data.put("summary", baselinePlayable
                ? "Tester evidence confirms the isolated native Minecraft baseline can create or load a world."
                : "Tester evidence is incomplete; the isolated native Minecraft baseline is not yet proven playable.");
        data.put("worldSaveCount", saveNames.size());
        data.put("worldSavePresent", !saveNames.isEmpty());
        return data;
    }

    private static Map<String, Object> minecraftBaselinePlayability(
            String packId,
            boolean playerJoined,
            boolean spawnPrepared,
            boolean chunksSaved,
            boolean cleanShutdown,
            boolean resourcesLoaded,
            boolean worldSavePresent,
            boolean noCrashReports,
            boolean baselinePlayable,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_minecraft_baseline_playability", diagnostics);
        data.put("baselinePlayable", baselinePlayable);
        data.put("chunksSaved", chunksSaved);
        data.put("cleanShutdownObserved", cleanShutdown);
        data.put("minecraftClientStarted", true);
        data.put("minecraftWorldLoaded", playerJoined && spawnPrepared);
        data.put("noCrashReports", noCrashReports);
        data.put("packId", packId);
        data.put("playerJoinedWorld", playerJoined);
        data.put("resourcesLoaded", resourcesLoaded);
        data.put("worldSavePresent", worldSavePresent);
        data.put("summary", baselinePlayable
                ? "The native loader can start Minecraft and reach a playable singleplayer world baseline."
                : "The native loader playable baseline still needs more tester evidence.");
        return data;
    }

    private static Map<String, Object> nativeProductPlayableGap(
            String packId,
            List<EchoNativeAddonDescriptor> descriptors,
            int activeModuleCount,
            int pendingBridgeCount,
            boolean baselinePlayable,
            Map<String, Object> activationMarker,
            boolean nativeProductModulesReady,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_native_product_playable_gap", diagnostics);
        Map<String, Map<String, Object>> liveModules = liveModulesById(activationMarker);
        Map<String, Object> nativeLoaderLiveProof = EchoNativePlayableModuleGate.nativeLoaderLiveProof(activationMarker);
        List<Map<String, Object>> modules = descriptors.stream()
                .sorted(Comparator.comparing(EchoNativeAddonDescriptor::id))
                .map(descriptor -> {
                    Map<String, Object> liveModule = liveModules.getOrDefault(descriptor.id(), Map.of());
                    boolean liveVerified = Boolean.TRUE.equals(liveModule.get("liveGameplayHookVerified"));
                    Map<String, Object> module = new LinkedHashMap<>();
                    module.put("id", descriptor.id());
                    module.put("role", descriptor.role());
                    module.put("kind", descriptor.kind());
                    module.put("descriptorDiscovered", true);
                    module.put("liveActivationState", String.valueOf(liveModule.getOrDefault(
                            "state",
                            liveVerified ? "native_module_adapter_gameplay_verified" : "pending_native_runtime_bridge"
                    )));
                    module.put("activeInLiveClient", liveVerified);
                    module.put("requiredForNativeProduct", "pack_root".equals(descriptor.kind()) || "library".equals(descriptor.kind()) || "ui_pack".equals(descriptor.kind()));
                    module.put("requiredForPlayableNativeProduct", Boolean.TRUE.equals(module.get("requiredForNativeProduct")));
                    return module;
                })
                .toList();
        data.put("activationMarkerWritten", !activationMarker.isEmpty());
        data.put("activeModuleCount", activeModuleCount);
        data.put("nativeWorldLiveHostHooksVerified", Boolean.TRUE.equals(activationMarker.get("nativeWorldLiveHostHooksVerified")));
        data.put("nativeFirstPlayableLoopReady", Boolean.TRUE.equals(activationMarker.get("nativeFirstPlayableLoopReady")));
        data.put("descriptorCount", descriptors.size());
        data.put("minecraftBaselinePlayable", baselinePlayable);
        data.put("modules", modules);
        data.put("nativeLoaderLiveProofComplete", EchoNativePlayableModuleGate.liveRuntimeProofAccepted(activationMarker));
        data.put("nativeLoaderLiveProofStatus", nativeLoaderLiveProof.getOrDefault("status", ""));
        data.put("nativeLoaderLiveProofMissingTargets", nativeLoaderLiveProof.getOrDefault("missingTargets", List.of()));
        data.put("nativeLoaderLiveProofMutatedSurfaces", nativeLoaderLiveProof.getOrDefault("mutationLedgerMutatedSurfaces", List.of()));
        data.put("nativeLoaderLiveProofRequiredMutationSurfacesMutated",
                Boolean.TRUE.equals(nativeLoaderLiveProof.get("requiredMutationSurfacesMutated")));
        data.put("nativeLiveGameplayHandlersAttached", Boolean.TRUE.equals(activationMarker.get("nativeLiveGameplayHandlersAttached")));
        data.put("packId", packId);
        data.put("pendingBridgeCount", pendingBridgeCount);
        data.put("nativeProductModulesReady", nativeProductModulesReady);
        data.put("playableNativeProductModules", nativeProductModulesReady);
        data.put("summary", nativeProductModulesReady
                ? "Native product modules are playable: Minecraft baseline, module runtime bridge, product loop, live module hooks, and runtime host mutations are verified."
                : "Native product modules are not fully playable yet: Minecraft works through the native path, but live module activation still needs complete runtime host mutation proof.");
        return data;
    }

    private static Map<String, Object> phase13M20Completion(
            String packId,
            boolean baselinePlayable,
            int activeModuleCount,
            int pendingBridgeCount,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m20_tester_playable_baseline_completion", diagnostics);
        data.put("activeModuleCount", activeModuleCount);
        data.put("minecraftBaselinePlayable", baselinePlayable);
        data.put("packId", packId);
        data.put("pendingBridgeCount", pendingBridgeCount);
        data.put("phase13M20Complete", baselinePlayable);
        data.put("phase13M21Ready", baselinePlayable && pendingBridgeCount > 0);
        data.put("playableNativeProductModules", false);
        data.put("summary", baselinePlayable
                ? "M20 is complete: tester evidence proves a playable Minecraft baseline through the native loader."
                : "M20 is blocked until tester evidence proves a playable Minecraft baseline.");
        return data;
    }

    private static Map<String, Object> phase13M21Readiness(
            String packId,
            boolean baselinePlayable,
            int pendingBridgeCount,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m21_module_runtime_bridge_readiness", diagnostics);
        data.put("nativeProductModuleRuntimeBridgeStarted", false);
        data.put("classloaderCreated", false);
        data.put("gameClassesResolved", false);
        data.put("minecraftBaselinePlayable", baselinePlayable);
        data.put("moduleBridgeReadyToStart", baselinePlayable && pendingBridgeCount > 0);
        data.put("packId", packId);
        data.put("pendingBridgeCount", pendingBridgeCount);
        data.put("registryInjected", false);
        data.put("registryMutated", false);
        data.put("serviceCodeExecuted", false);
        data.put("transformsEnabled", false);
        data.put("transformsPerformed", false);
        data.put("summary", baselinePlayable
                ? "M21 may start as the next controlled slice: native module runtime bridge MVP and native product module activation evidence."
                : "M21 remains blocked until M20 tester playable baseline evidence passes.");
        return data;
    }

    private static int activeModuleCount(Map<String, Object> report, Map<String, Object> activationMarker) {
        int liveCount = liveModuleActiveCount(activationMarker);
        if (liveCount > 0) {
            return liveCount;
        }
        Map<String, Object> data = EchoNativeJson.asObject(report == null ? null : report.get("data"));
        Object value = data.get("activeModuleCount");
        return value instanceof Number number ? number.intValue() : 0;
    }

    private static boolean nativeProductModulesReady(Map<String, Object> activationMarker, int descriptorCount) {
        return EchoNativePlayableModuleGate.nativeProductModulesReady(
                activationMarker,
                descriptorCount,
                liveModuleActiveCount(activationMarker));
    }

    private static int liveModuleActiveCount(Map<String, Object> activationMarker) {
        Object modules = activationMarker.get("modules");
        if (!(modules instanceof Iterable<?> iterable)) {
            return 0;
        }
        int count = 0;
        for (Object raw : iterable) {
            Map<String, Object> module = EchoNativeJson.asObject(raw);
            if (Boolean.TRUE.equals(module.get("liveGameplayHookVerified"))) {
                count++;
            }
        }
        return count;
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

    private static List<String> directoryNames(Path directory) throws IOException {
        if (!Files.isDirectory(directory)) {
            return List.of();
        }
        try (var stream = Files.list(directory)) {
            return stream.filter(Files::isDirectory)
                    .map(path -> path.getFileName().toString())
                    .sorted()
                    .toList();
        }
    }

    private static List<String> fileNames(Path directory) throws IOException {
        if (!Files.isDirectory(directory)) {
            return List.of();
        }
        try (var stream = Files.list(directory)) {
            return stream.filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .sorted()
                    .toList();
        }
    }

    private static LocalDateTime inferLatestLogSessionStart(Path latestLog, String latestLogText) throws IOException {
        if (!Files.isRegularFile(latestLog)) {
            return null;
        }
        LocalDate logDate = LocalDateTime.ofInstant(Files.getLastModifiedTime(latestLog).toInstant(), ZoneId.systemDefault()).toLocalDate();
        for (String line : latestLogText.lines().toList()) {
            int start = line.indexOf('[');
            int end = line.indexOf(']');
            if (start != 0 || end < 9) {
                continue;
            }
            String timeText = line.substring(1, end);
            try {
                return LocalDateTime.of(logDate, LocalTime.parse(timeText));
            } catch (DateTimeParseException ignored) {
                // Keep scanning; Minecraft log prefixes are optional for early launcher messages.
            }
        }
        return LocalDateTime.ofInstant(Files.getLastModifiedTime(latestLog).toInstant(), ZoneId.systemDefault()).minusHours(2);
    }

    private static List<String> activeCrashReports(List<String> crashReports, LocalDateTime latestLogSessionStart) {
        if (latestLogSessionStart == null) {
            return crashReports;
        }
        return crashReports.stream()
                .filter(report -> {
                    LocalDateTime crashTime = crashReportTime(report);
                    return crashTime == null || !crashTime.isBefore(latestLogSessionStart);
                })
                .toList();
    }

    private static LocalDateTime crashReportTime(String name) {
        if (!name.startsWith("crash-") || name.length() < 25) {
            return null;
        }
        try {
            return LocalDateTime.parse(name.substring(6, 25), CRASH_REPORT_TIMESTAMP);
        } catch (DateTimeParseException exception) {
            return null;
        }
    }

    private static boolean containsAny(String text, String... needles) {
        for (String needle : needles) {
            if (text.contains(needle)) {
                return true;
            }
        }
        return false;
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
                    "ECHO-NATIVE-TESTER-EVIDENCE-REPORT-MISSING",
                    EchoNativeIssueSeverity.ERROR,
                    "Tester evidence required report missing",
                    "The tester evidence intake requires " + reportName + ".",
                    null,
                    packId,
                    List.of(relativePath(fixture)),
                    "Regenerate native tester launch reports before tester evidence intake."
            ));
            return Map.of();
        }
        return EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(reportPath)));
    }

    private static void checkAcceptedReport(
            String reportName,
            Path reportPath,
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
                    "ECHO-NATIVE-TESTER-EVIDENCE-UPSTREAM-BLOCKED",
                    EchoNativeIssueSeverity.ERROR,
                    "Tester evidence upstream report is not accepted",
                    "The tester evidence intake requires PASS or PASS_WITH_WARNINGS " + reportName + ".",
                    null,
                    packId,
                    List.of(relativePath(reportPath)),
                    "Resolve upstream native tester launch diagnostics before taking tester evidence."
            ));
        }
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
        data.put("gameProcessLaunched", false);
        data.put("generatedEvidenceAt", Instant.EPOCH.toString());
        data.put("libraryDownloadStarted", false);
        data.put("minecraftLaunched", false);
        data.put("nativeExtractionStarted", false);
        data.put("phase", phase);
        data.put("processLaunched", false);
        data.put("registryInjected", false);
        data.put("registryMutated", false);
        data.put("transformsEnabled", false);
        data.put("transformsPerformed", false);
        data.put("unsafeRuntimeWorkStarted", false);
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
