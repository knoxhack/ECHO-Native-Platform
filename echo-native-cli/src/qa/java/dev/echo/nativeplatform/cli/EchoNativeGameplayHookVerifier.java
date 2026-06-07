package dev.echo.nativeplatform.cli;

import dev.echo.nativeplatform.contracts.EchoNativeAddonDescriptor;
import dev.echo.nativeplatform.contracts.EchoNativeDiagnostic;
import dev.echo.nativeplatform.contracts.EchoNativeIssueSeverity;
import dev.echo.nativeplatform.diagnostics.EchoNativeJson;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class EchoNativeGameplayHookVerifier {
    EchoNativeGameplayHookOutcome verify(
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

        Map<String, Object> m22Completion = data(reports.get("phase13-m22-completion.json"));
        Map<String, Object> markerReport = data(reports.get("native-live-activation-marker.json"));
        Map<String, Object> safetyStatus = data(reports.get("native-live-activation-safety-status.json"));
        Map<String, Object> baselinePlayability = data(reports.get("minecraft-baseline-playability.json"));
        Map<String, Object> adapterCoreGameplayEvidence = data(reports.get("adaptercore-runtime-gameplay-handler-evidence.json"));

        boolean m22Complete = bool(m22Completion, "phase13M22Complete");
        boolean activationMarkerPresent = bool(markerReport, "activationMarkerPresent");
        boolean activationMarkerWritten = bool(markerReport, "activationMarkerWritten");
        boolean activationSafetyOk = bool(safetyStatus, "phase13M22Complete") && !bool(safetyStatus, "externalCommandExecuted");
        boolean baselinePlayable = bool(baselinePlayability, "baselinePlayable");
        boolean adapterCoreRuntimeBridgeActive = bool(adapterCoreGameplayEvidence, "adapterCoreRuntimeBridgeActive");
        boolean adapterCoreFirstPlayableLoopReady = bool(adapterCoreGameplayEvidence, "firstPlayableLoopReady");
        boolean liveGameplayHandlersAttached = bool(adapterCoreGameplayEvidence, "liveGameplayHandlersAttached");
        boolean adapterCoreGameplayHandlersAttached = bool(adapterCoreGameplayEvidence, "adapterCoreGameplayHandlersAttached")
                || liveGameplayHandlersAttached;
        boolean adapterCoreReplayVerified = bool(adapterCoreGameplayEvidence, "adapterCoreGameplayHandlerReplayVerified")
                || bool(adapterCoreGameplayEvidence, "gameplayHandlerExecuted");
        boolean gameplayHandlerExecuted = bool(adapterCoreGameplayEvidence, "gameplayHandlerExecuted");
        int adapterCoreRequiredHandlerCount = intValue(adapterCoreGameplayEvidence, "requiredHandlerCount");
        int adapterCoreAttachedHandlerCount = intValue(adapterCoreGameplayEvidence, "attachedHandlerCount");
        int adapterCoreExecutedHandlerCount = intValue(adapterCoreGameplayEvidence, "executedHandlerCount");
        int adapterCoreVerifiedHandlerCount = intValue(adapterCoreGameplayEvidence, "verifiedHandlerCount");
        List<Map<String, Object>> adapterCoreHandlerContracts = handlerContracts(adapterCoreGameplayEvidence);
        int adapterCoreVerifiedContractCount = verifiedContractCount(adapterCoreHandlerContracts);
        int adapterCoreVerifiedExecutionCount = verifiedExecutionCount(adapterCoreGameplayEvidence);
        boolean adapterCoreGameplayHandlersVerified = adapterCoreRuntimeBridgeActive
                && adapterCoreFirstPlayableLoopReady
                && adapterCoreGameplayHandlersAttached
                && adapterCoreReplayVerified
                && gameplayHandlerExecuted
                && adapterCoreRequiredHandlerCount > 0
                && adapterCoreAttachedHandlerCount >= adapterCoreRequiredHandlerCount
                && adapterCoreExecutedHandlerCount >= adapterCoreRequiredHandlerCount
                && adapterCoreVerifiedHandlerCount >= adapterCoreRequiredHandlerCount
                && adapterCoreVerifiedContractCount >= adapterCoreRequiredHandlerCount
                && adapterCoreVerifiedExecutionCount >= adapterCoreRequiredHandlerCount;

        String markerPathText = String.valueOf(markerReport.getOrDefault(
                "markerPath",
                "fixtures/" + fixture.getFileName() + "/isolated-runtime/game/echo-native/module-activation.json"
        ));
        Path markerPath = Path.of("").toAbsolutePath().normalize().resolve(markerPathText).normalize();
        if (!Files.isRegularFile(markerPath)) {
            markerPath = fixture.resolve("isolated-runtime/game/echo-native/module-activation.json").normalize();
        }
        Path gameDirectory = fixture.resolve("isolated-runtime/game").normalize();
        Path latestLog = gameDirectory.resolve("logs/latest.log").normalize();
        Path crashReportsDirectory = gameDirectory.resolve("crash-reports").normalize();
        Path savesDirectory = gameDirectory.resolve("saves").normalize();

        String latestLogText = Files.isRegularFile(latestLog) ? Files.readString(latestLog) : "";
        if (!Files.isRegularFile(latestLog)) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-GAMEPLAY-HOOK-LATEST-LOG-MISSING",
                    EchoNativeIssueSeverity.ERROR,
                    "Latest isolated runtime log is missing",
                    "M23 needs the isolated runtime latest.log to distinguish vanilla baseline evidence from product gameplay hook evidence.",
                    null,
                    packId,
                    List.of(relativePath(latestLog)),
                    "Run the tester native path, create or load a world, then rerun gameplay hook verification."
            ));
        }
        if (!Files.isRegularFile(markerPath)) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-GAMEPLAY-HOOK-ACTIVATION-MARKER-MISSING",
                    EchoNativeIssueSeverity.ERROR,
                    "Native live activation marker is missing",
                    "M23 requires the M22 activation marker written inside the isolated runtime.",
                    null,
                    packId,
                    List.of(relativePath(markerPath)),
                    "Run phase13 activate bootstrap --authorized before verifying gameplay hooks."
            ));
        }

        Map<String, Object> markerFile = Files.isRegularFile(markerPath)
                ? EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(markerPath)))
                : Map.of();
        Map<String, Object> markerRuntimeBridge = EchoNativeJson.asObject(markerFile.get("runtimeBridge"));
        Map<String, Object> markerGameplayBridge = EchoNativeJson.asObject(markerRuntimeBridge.get("ashfallGameplayBridge"));
        Map<String, Object> agent7WorldLiveHostHookEvidence = EchoNativeJson.asObject(
                markerGameplayBridge.get("agent7WorldLiveHostHookEvidence")
        );
        if (!agent7ExactWorldLiveHostHooksVerified(agent7WorldLiveHostHookEvidence)) {
            Map<String, Object> sidecarEvidence = agent7WorldLiveHostHookEvidenceFromSidecar(markerPath);
            if (!sidecarEvidence.isEmpty()) {
                agent7WorldLiveHostHookEvidence = sidecarEvidence;
            }
        }
        boolean agent7WorldLiveHostHooksVerified = agent7ExactWorldLiveHostHooksVerified(agent7WorldLiveHostHookEvidence);
        int agent7WorldLiveHostVerifiedHookCount = agent7WorldLiveHostHooksVerified
                ? intValue(agent7WorldLiveHostHookEvidence, "verifiedHookCount")
                : 0;
        int agent7WorldLiveHostRequiredHookCount = intValue(agent7WorldLiveHostHookEvidence, "requiredHookCount");
        if (!markerGameplayBridge.isEmpty()
                && (bool(markerGameplayBridge, "liveGameplayHandlersAttached")
                || intValue(markerGameplayBridge, "liveGameplayHookVerifiedCount") > 0)) {
            adapterCoreRuntimeBridgeActive = bool(markerFile, "adapterCoreRuntimeBridgeActive")
                    || bool(markerRuntimeBridge, "adapterCoreRuntimeBridgeActive");
            adapterCoreFirstPlayableLoopReady = bool(markerGameplayBridge, "firstPlayableLoopReady");
            liveGameplayHandlersAttached = bool(markerGameplayBridge, "liveGameplayHandlersAttached");
            adapterCoreGameplayHandlersAttached = bool(markerGameplayBridge, "adapterCoreGameplayHandlersAttached")
                    || liveGameplayHandlersAttached
                    || bool(markerGameplayBridge, "partialLiveGameplayHandlersAttached");
            adapterCoreReplayVerified = bool(markerGameplayBridge, "adapterCoreGameplayHandlerReplayVerified")
                    || bool(markerGameplayBridge, "gameplayHandlerExecuted");
            gameplayHandlerExecuted = bool(markerGameplayBridge, "gameplayHandlerExecuted");
            adapterCoreRequiredHandlerCount = Math.max(
                    intValue(markerGameplayBridge, "liveGameplayRequiredHookCount"),
                    intValue(markerGameplayBridge, "requiredHandlerCount")
            );
            adapterCoreAttachedHandlerCount = Math.max(
                    intValue(markerGameplayBridge, "attachedHandlerCount"),
                    intValue(markerGameplayBridge, "liveGameplayHookVerifiedCount")
            );
            adapterCoreExecutedHandlerCount = Math.max(
                    intValue(markerGameplayBridge, "executedHandlerCount"),
                    intValue(markerGameplayBridge, "liveGameplayHookVerifiedCount")
            );
            adapterCoreVerifiedHandlerCount = intValue(markerGameplayBridge, "liveGameplayHookVerifiedCount");
            adapterCoreHandlerContracts = handlerContracts(markerGameplayBridge);
            adapterCoreVerifiedContractCount = verifiedContractCount(adapterCoreHandlerContracts);
            adapterCoreVerifiedExecutionCount = verifiedExecutionCount(markerGameplayBridge);
            adapterCoreGameplayHandlersVerified = adapterCoreRuntimeBridgeActive
                    && adapterCoreFirstPlayableLoopReady
                    && adapterCoreGameplayHandlersAttached
                    && adapterCoreReplayVerified
                    && gameplayHandlerExecuted
                    && liveGameplayHandlersAttached
                    && adapterCoreRequiredHandlerCount > 0
                    && adapterCoreAttachedHandlerCount >= adapterCoreRequiredHandlerCount
                    && adapterCoreExecutedHandlerCount >= adapterCoreRequiredHandlerCount
                    && adapterCoreVerifiedHandlerCount >= adapterCoreRequiredHandlerCount
                    && adapterCoreVerifiedContractCount >= adapterCoreRequiredHandlerCount
                    && adapterCoreVerifiedExecutionCount >= adapterCoreRequiredHandlerCount;
        }
        List<String> markedModules = modules(markerFile, markerReport);
        Map<String, Boolean> markerModuleHookVerified = moduleHookVerifiedById(markerFile);
        boolean markerMatchesDescriptorCount = markedModules.size() == descriptors.size();

        String lowerLog = latestLogText.toLowerCase(Locale.ROOT);
        List<String> crashReports = fileNames(crashReportsDirectory);
        List<String> saveNames = directoryNames(savesDirectory);
        boolean playerJoinedWorld = latestLogText.contains("EchoNativeTester joined the game");
        boolean survivalModeObserved = latestLogText.contains("Set own game mode to Survival Mode");
        boolean playerDeathObserved = latestLogText.contains("EchoNativeTester was slain by");
        boolean chunksSaved = latestLogText.contains("Saving chunks for level");
        boolean allDimensionsSaved = latestLogText.contains("ThreadedAnvilChunkStorage: All dimensions are saved");
        boolean cleanShutdownObserved = latestLogText.contains("Stopping server") || latestLogText.contains("Stopping!");
        boolean crashSignal = lowerLog.contains("reported exception")
                || lowerLog.contains("crash report")
                || lowerLog.contains("exception in thread")
                || lowerLog.contains("failed to start");
        boolean noCrashEvidence = crashReports.isEmpty() && !crashSignal;
        boolean vanillaPlayLoopObserved = playerJoinedWorld
                && survivalModeObserved
                && playerDeathObserved
                && chunksSaved
                && cleanShutdownObserved
                && noCrashEvidence;

        List<ModuleHookEvidence> moduleEvidence = descriptors.stream()
                .sorted(Comparator.comparing(EchoNativeAddonDescriptor::id))
                .map(descriptor -> new ModuleHookEvidence(
                        descriptor.id(),
                        descriptor.kind(),
                        descriptor.role(),
                        markedModules.contains(descriptor.id()),
                        Boolean.TRUE.equals(markerModuleHookVerified.get(descriptor.id())),
                        Boolean.TRUE.equals(markerModuleHookVerified.get(descriptor.id()))
                                ? "live_gameplay_hook_verified"
                                : "activation_marker_written_gameplay_hook_pending"
                ))
                .toList();
        int markedModuleCount = (int) moduleEvidence.stream().filter(ModuleHookEvidence::activationMarkerWritten).count();
        int gameplayHookVerifiedCount = adapterCoreGameplayHandlersVerified ? adapterCoreVerifiedHandlerCount : 0;
        if (m22Complete && activationMarkerPresent && baselinePlayable && !adapterCoreGameplayHandlersVerified) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-PRODUCT-GAMEPLAY-HOOKS-PENDING",
                    EchoNativeIssueSeverity.WARNING,
                    "Native product gameplay hooks are not verified yet",
                    "The isolated native path now proves Minecraft baseline play evidence and an ECHO activation marker, but AdapterCore runtime gameplay handler replay evidence is missing or incomplete.",
                    null,
                    packId,
                    List.of("reports/echo-native/" + packId + "/adaptercore-runtime-gameplay-handler-evidence.json", "reports/echo-native/" + packId + "/native-product-playable-readiness.json"),
                    "Regenerate AdapterCore runtime gameplay handler evidence before declaring native product playability ready."
            ));
        }

        boolean prerequisiteReady = m22Complete
                && activationMarkerPresent
                && activationMarkerWritten
                && activationSafetyOk
                && baselinePlayable
                && markerMatchesDescriptorCount;
        boolean m23Complete = prerequisiteReady && diagnostics.stream().noneMatch(EchoNativeGameplayHookVerifier::isBlocking);
        boolean nativeProductPlayableReady = m23Complete && adapterCoreGameplayHandlersVerified;
        List<EchoNativeDiagnostic> sortedDiagnostics = diagnostics.stream()
                .sorted(Comparator.comparing(EchoNativeDiagnostic::code)
                        .thenComparing(diagnostic -> diagnostic.moduleId() == null ? "" : diagnostic.moduleId())
                        .thenComparing(EchoNativeDiagnostic::summary))
                .toList();

        return new EchoNativeGameplayHookOutcome(
                packId,
                nativeProductGameplayHookEvidence(packId, latestLog, markerPath, saveNames, crashReports, playerJoinedWorld, survivalModeObserved, playerDeathObserved, chunksSaved, allDimensionsSaved, cleanShutdownObserved, noCrashEvidence, vanillaPlayLoopObserved, adapterCoreRuntimeBridgeActive, adapterCoreFirstPlayableLoopReady, liveGameplayHandlersAttached, gameplayHandlerExecuted, markedModuleCount, adapterCoreRequiredHandlerCount, adapterCoreAttachedHandlerCount, adapterCoreExecutedHandlerCount, gameplayHookVerifiedCount, adapterCoreHandlerContracts, adapterCoreGameplayHandlersVerified, agent7WorldLiveHostHookEvidence, agent7WorldLiveHostHooksVerified, agent7WorldLiveHostVerifiedHookCount, agent7WorldLiveHostRequiredHookCount, sortedDiagnostics),
                nativeModuleGameplayHookStatus(packId, moduleEvidence, markedModuleCount, gameplayHookVerifiedCount, adapterCoreRequiredHandlerCount, adapterCoreAttachedHandlerCount, adapterCoreExecutedHandlerCount, adapterCoreHandlerContracts, adapterCoreGameplayHandlersVerified, liveGameplayHandlersAttached, agent7WorldLiveHostHookEvidence, agent7WorldLiveHostHooksVerified, agent7WorldLiveHostVerifiedHookCount, agent7WorldLiveHostRequiredHookCount, sortedDiagnostics),
                nativeProductPlayableReadiness(packId, baselinePlayable, vanillaPlayLoopObserved, adapterCoreFirstPlayableLoopReady, markedModuleCount, descriptors.size(), adapterCoreRequiredHandlerCount, gameplayHookVerifiedCount, nativeProductPlayableReady, liveGameplayHandlersAttached, agent7WorldLiveHostHooksVerified, agent7WorldLiveHostVerifiedHookCount, agent7WorldLiveHostRequiredHookCount, sortedDiagnostics),
                phase13M23Completion(packId, m23Complete, baselinePlayable, vanillaPlayLoopObserved, adapterCoreFirstPlayableLoopReady, markedModuleCount, descriptors.size(), adapterCoreRequiredHandlerCount, gameplayHookVerifiedCount, nativeProductPlayableReady, liveGameplayHandlersAttached, agent7WorldLiveHostHooksVerified, agent7WorldLiveHostVerifiedHookCount, agent7WorldLiveHostRequiredHookCount, sortedDiagnostics),
                phase13M24Readiness(packId, m23Complete, nativeProductPlayableReady, Math.max(0, adapterCoreRequiredHandlerCount - gameplayHookVerifiedCount), sortedDiagnostics),
                sortedDiagnostics
        );
    }

    private static Map<String, Object> nativeProductGameplayHookEvidence(
            String packId,
            Path latestLog,
            Path markerPath,
            List<String> saveNames,
            List<String> crashReports,
            boolean playerJoinedWorld,
            boolean survivalModeObserved,
            boolean playerDeathObserved,
            boolean chunksSaved,
            boolean allDimensionsSaved,
            boolean cleanShutdownObserved,
            boolean noCrashEvidence,
            boolean vanillaPlayLoopObserved,
            boolean adapterCoreRuntimeBridgeActive,
            boolean adapterCoreFirstPlayableLoopReady,
            boolean liveGameplayHandlersAttached,
            boolean gameplayHandlerExecuted,
            int markedModuleCount,
            int requiredHandlerCount,
            int attachedHandlerCount,
            int executedHandlerCount,
            int gameplayHookVerifiedCount,
            List<Map<String, Object>> handlerContracts,
            boolean adapterCoreGameplayHandlersVerified,
            Map<String, Object> agent7WorldLiveHostHookEvidence,
            boolean agent7WorldLiveHostHooksVerified,
            int agent7WorldLiveHostVerifiedHookCount,
            int agent7WorldLiveHostRequiredHookCount,
            List<EchoNativeDiagnostic> diagnostics
    ) throws IOException {
        Map<String, Object> data = base("phase13_m23_native_product_gameplay_hook_evidence", diagnostics);
        data.put("activationMarkerByteSize", Files.isRegularFile(markerPath) ? Files.size(markerPath) : 0);
        data.put("activationMarkerPath", relativePath(markerPath));
        data.put("activationMarkerPresent", Files.isRegularFile(markerPath));
        data.put("activationMarkerSha256", sha256Of(markerPath));
        data.put("allDimensionsSaved", allDimensionsSaved);
        data.put("chunksSaved", chunksSaved);
        data.put("cleanShutdownObserved", cleanShutdownObserved);
        data.put("crashReportCount", crashReports.size());
        data.put("crashReports", crashReports);
        data.put("adapterCoreRuntimeBridgeActive", adapterCoreRuntimeBridgeActive);
        data.put("adapterCoreFirstPlayableLoopReady", adapterCoreFirstPlayableLoopReady);
        data.put("adapterCoreGameplayHandlersVerified", adapterCoreGameplayHandlersVerified);
        data.put("agent7WorldLiveHostHookEvidence", agent7WorldLiveHostHookEvidence);
        data.put("agent7WorldLiveHostHooksVerified", agent7WorldLiveHostHooksVerified);
        data.put("agent7WorldLiveHostHookVerifiedCount", agent7WorldLiveHostVerifiedHookCount);
        data.put("agent7WorldLiveHostRequiredHookCount", agent7WorldLiveHostRequiredHookCount);
        data.put("adapterCoreGameplayHandlersAttached", attachedHandlerCount >= requiredHandlerCount && requiredHandlerCount > 0);
        data.put("adapterCoreGameplayHandlerReplayVerified", adapterCoreGameplayHandlersVerified && !liveGameplayHandlersAttached);
        data.put("attachedHandlerCount", attachedHandlerCount);
        data.put("controlledAdapterCoreReplayVerified", adapterCoreGameplayHandlersVerified && !liveGameplayHandlersAttached);
        data.put("evidenceMode", liveGameplayHandlersAttached
                ? "live_minecraft_client_interaction_probe"
                : "controlled_native_bootstrap_adaptercore_replay");
        data.put("executedHandlerCount", executedHandlerCount);
        data.put("gameplayHookVerifiedCount", gameplayHookVerifiedCount);
        data.put("controlledReplayVerifiedHandlerCount", liveGameplayHandlersAttached ? 0 : gameplayHookVerifiedCount);
        data.put("liveGameplayHookVerifiedCount", liveGameplayHandlersAttached ? gameplayHookVerifiedCount : 0);
        data.put("handlerContracts", handlerContracts);
        data.put("latestLog", relativePath(latestLog));
        data.put("latestLogPresent", Files.isRegularFile(latestLog));
        data.put("liveGameplayHandlersAttached", liveGameplayHandlersAttached);
        data.put("liveMinecraftProcessHooksClaimed", liveGameplayHandlersAttached);
        data.put("liveGameplayHookBlockedReason", liveGameplayHandlersAttached ? "" : "live_minecraft_process_hook_attachment_unproven");
        data.put("markedModuleCount", markedModuleCount);
        data.put("noCrashEvidence", noCrashEvidence);
        data.put("packId", packId);
        data.put("playerDeathObserved", playerDeathObserved);
        data.put("playerJoinedWorld", playerJoinedWorld);
        data.put("saveNames", saveNames);
        data.put("survivalModeObserved", survivalModeObserved);
        data.put("gameplayHandlerExecuted", gameplayHandlerExecuted);
        data.put("requiredHandlerCount", requiredHandlerCount);
        data.put("vanillaPlayLoopObserved", vanillaPlayLoopObserved);
        data.put("summary", adapterCoreGameplayHandlersVerified && liveGameplayHandlersAttached
                ? "M23 accepted live Minecraft client interaction evidence for all required AdapterCore product gameplay handlers."
                : adapterCoreGameplayHandlersVerified
                ? "M23 accepted controlled AdapterCore runtime replay evidence for product gameplay handlers; no live Minecraft process hooks are claimed."
                : vanillaPlayLoopObserved
                ? "M23 found strong isolated Minecraft play-loop evidence, but no complete AdapterCore gameplay handler replay evidence yet."
                : "M23 could not prove the isolated Minecraft play-loop evidence needed before product gameplay hook verification.");
        return data;
    }

    private static Map<String, Object> nativeModuleGameplayHookStatus(
            String packId,
            List<ModuleHookEvidence> moduleEvidence,
            int markedModuleCount,
            int gameplayHookVerifiedCount,
            int requiredHandlerCount,
            int attachedHandlerCount,
            int executedHandlerCount,
            List<Map<String, Object>> handlerContracts,
            boolean adapterCoreGameplayHandlersVerified,
            boolean liveGameplayHandlersAttached,
            Map<String, Object> agent7WorldLiveHostHookEvidence,
            boolean agent7WorldLiveHostHooksVerified,
            int agent7WorldLiveHostVerifiedHookCount,
            int agent7WorldLiveHostRequiredHookCount,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m23_native_module_gameplay_hook_status", diagnostics);
        data.put("adapterCoreGameplayHandlerVerifiedCount", gameplayHookVerifiedCount);
        data.put("agent7WorldLiveHostHookEvidence", agent7WorldLiveHostHookEvidence);
        data.put("agent7WorldLiveHostHooksVerified", agent7WorldLiveHostHooksVerified);
        data.put("agent7WorldLiveHostHookVerifiedCount", agent7WorldLiveHostVerifiedHookCount);
        data.put("agent7WorldLiveHostRequiredHookCount", agent7WorldLiveHostRequiredHookCount);
        data.put("adapterCoreGameplayHandlersAttached", attachedHandlerCount >= requiredHandlerCount && requiredHandlerCount > 0);
        data.put("adapterCoreGameplayHandlerReplayVerified", adapterCoreGameplayHandlersVerified && !liveGameplayHandlersAttached);
        data.put("attachedHandlerCount", attachedHandlerCount);
        data.put("controlledAdapterCoreReplayVerified", adapterCoreGameplayHandlersVerified && !liveGameplayHandlersAttached);
        data.put("evidenceMode", liveGameplayHandlersAttached
                ? "live_minecraft_client_interaction_probe"
                : "controlled_native_bootstrap_adaptercore_replay");
        data.put("executedHandlerCount", executedHandlerCount);
        data.put("gameplayHookVerifiedCount", gameplayHookVerifiedCount);
        data.put("controlledReplayVerifiedHandlerCount", liveGameplayHandlersAttached ? 0 : gameplayHookVerifiedCount);
        data.put("liveGameplayHookVerifiedCount", liveGameplayHandlersAttached ? gameplayHookVerifiedCount : 0);
        data.put("handlerContracts", handlerContracts);
        data.put("liveMinecraftProcessHooksClaimed", liveGameplayHandlersAttached);
        data.put("markedModuleCount", markedModuleCount);
        data.put("moduleGameplayHookVerifiedCount", 0);
        data.put("moduleCount", moduleEvidence.size());
        data.put("modules", moduleEvidence.stream().map(EchoNativeGameplayHookVerifier::moduleData).toList());
        data.put("packId", packId);
        data.put("requiredHandlerCount", requiredHandlerCount);
        data.put("summary", adapterCoreGameplayHandlersVerified && liveGameplayHandlersAttached
                ? "AdapterCore live Minecraft interaction probes verify all required product gameplay handler contracts."
                : adapterCoreGameplayHandlersVerified
                ? "AdapterCore runtime replay verifies product gameplay handler contracts; per-module live Minecraft process hooks are not claimed by this report."
                : "All discovered modules remain activation-marker visible only; AdapterCore runtime gameplay handler replay is still pending.");
        return data;
    }

    private static Map<String, Object> nativeProductPlayableReadiness(
            String packId,
            boolean baselinePlayable,
            boolean vanillaPlayLoopObserved,
            boolean adapterCoreFirstPlayableLoopReady,
            int markedModuleCount,
            int descriptorCount,
            int requiredHandlerCount,
            int gameplayHookVerifiedCount,
            boolean nativeProductPlayableReady,
            boolean liveGameplayHandlersAttached,
            boolean agent7WorldLiveHostHooksVerified,
            int agent7WorldLiveHostVerifiedHookCount,
            int agent7WorldLiveHostRequiredHookCount,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m23_native_product_playable_readiness", diagnostics);
        data.put("nativeProductPlayableReady", nativeProductPlayableReady);
        data.put("adapterCoreFirstPlayableLoopReady", adapterCoreFirstPlayableLoopReady);
        data.put("agent7WorldLiveHostHooksVerified", agent7WorldLiveHostHooksVerified);
        data.put("agent7WorldLiveHostHookVerifiedCount", agent7WorldLiveHostVerifiedHookCount);
        data.put("agent7WorldLiveHostRequiredHookCount", agent7WorldLiveHostRequiredHookCount);
        data.put("controlledAdapterCoreReplayVerified", !liveGameplayHandlersAttached
                && gameplayHookVerifiedCount == requiredHandlerCount && requiredHandlerCount > 0);
        data.put("evidenceMode", liveGameplayHandlersAttached
                ? "live_minecraft_client_interaction_probe"
                : "controlled_native_bootstrap_adaptercore_replay");
        data.put("gameplayHookVerifiedCount", gameplayHookVerifiedCount);
        data.put("controlledReplayVerifiedHandlerCount", liveGameplayHandlersAttached ? 0 : gameplayHookVerifiedCount);
        data.put("liveGameplayHookVerifiedCount", liveGameplayHandlersAttached ? gameplayHookVerifiedCount : 0);
        data.put("liveMinecraftProcessHooksClaimed", liveGameplayHandlersAttached);
        data.put("markedModuleCount", markedModuleCount);
        data.put("minecraftBaselinePlayable", baselinePlayable);
        data.put("moduleCount", descriptorCount);
        data.put("packId", packId);
        data.put("remainingGameplayHookCount", Math.max(0, requiredHandlerCount - gameplayHookVerifiedCount));
        data.put("requiredHandlerCount", requiredHandlerCount);
        data.put("vanillaPlayLoopObserved", vanillaPlayLoopObserved);
        data.put("summary", nativeProductPlayableReady && liveGameplayHandlersAttached
                ? "Native product playable readiness is reconciled with live Minecraft AdapterCore gameplay handler evidence."
                : nativeProductPlayableReady
                ? "Native product playable readiness is reconciled with controlled AdapterCore runtime gameplay handler replay evidence."
                : "Native product playability remains closed until controlled AdapterCore gameplay handler replay evidence is complete.");
        return data;
    }

    private static Map<String, Object> phase13M23Completion(
            String packId,
            boolean m23Complete,
            boolean baselinePlayable,
            boolean vanillaPlayLoopObserved,
            boolean adapterCoreFirstPlayableLoopReady,
            int markedModuleCount,
            int descriptorCount,
            int requiredHandlerCount,
            int gameplayHookVerifiedCount,
            boolean nativeProductPlayableReady,
            boolean liveGameplayHandlersAttached,
            boolean agent7WorldLiveHostHooksVerified,
            int agent7WorldLiveHostVerifiedHookCount,
            int agent7WorldLiveHostRequiredHookCount,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m23_gameplay_hook_evidence_completion", diagnostics);
        data.put("nativeProductPlayableReady", nativeProductPlayableReady);
        data.put("adapterCoreFirstPlayableLoopReady", adapterCoreFirstPlayableLoopReady);
        data.put("agent7WorldLiveHostHooksVerified", agent7WorldLiveHostHooksVerified);
        data.put("agent7WorldLiveHostHookVerifiedCount", agent7WorldLiveHostVerifiedHookCount);
        data.put("agent7WorldLiveHostRequiredHookCount", agent7WorldLiveHostRequiredHookCount);
        data.put("controlledAdapterCoreReplayVerified", !liveGameplayHandlersAttached
                && gameplayHookVerifiedCount == requiredHandlerCount && requiredHandlerCount > 0);
        data.put("evidenceMode", liveGameplayHandlersAttached
                ? "live_minecraft_client_interaction_probe"
                : "controlled_native_bootstrap_adaptercore_replay");
        data.put("gameplayHookVerifiedCount", gameplayHookVerifiedCount);
        data.put("controlledReplayVerifiedHandlerCount", liveGameplayHandlersAttached ? 0 : gameplayHookVerifiedCount);
        data.put("liveGameplayHookVerifiedCount", liveGameplayHandlersAttached ? gameplayHookVerifiedCount : 0);
        data.put("liveMinecraftProcessHooksClaimed", liveGameplayHandlersAttached);
        data.put("markedModuleCount", markedModuleCount);
        data.put("minecraftBaselinePlayable", baselinePlayable);
        data.put("moduleCount", descriptorCount);
        data.put("packId", packId);
        data.put("phase13M23Complete", m23Complete);
        data.put("phase13M24Ready", m23Complete && !nativeProductPlayableReady);
        data.put("remainingGameplayHookCount", Math.max(0, requiredHandlerCount - gameplayHookVerifiedCount));
        data.put("requiredHandlerCount", requiredHandlerCount);
        data.put("vanillaPlayLoopObserved", vanillaPlayLoopObserved);
        data.put("summary", m23Complete && liveGameplayHandlersAttached
                ? "M23 is complete against live Minecraft AdapterCore gameplay handler evidence."
                : m23Complete
                ? "M23 is complete against controlled AdapterCore runtime gameplay handler replay evidence; no live Minecraft process hooks are claimed."
                : "M23 remains blocked until M22, baseline play, and activation marker evidence are present.");
        return data;
    }

    private static Map<String, Object> phase13M24Readiness(
            String packId,
            boolean m23Complete,
            boolean nativeProductPlayableReady,
            int remainingGameplayHookCount,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m24_readiness", diagnostics);
        data.put("nativeProductPlayableReady", nativeProductPlayableReady);
        data.put("nextCommand", "phase13 bridge gameplay-hooks <fixture>");
        data.put("nextMilestone", "phase13.m24.native_gameplay_hook_bridge_mvp");
        data.put("packId", packId);
        data.put("phase13M24Ready", m23Complete && !nativeProductPlayableReady);
        data.put("remainingGameplayHookCount", remainingGameplayHookCount);
        data.put("summary", m23Complete && nativeProductPlayableReady
                ? "M24 is not required for this M23 gate because controlled AdapterCore runtime gameplay handler replay evidence is complete."
                : m23Complete
                ? "M24 may start: implement the native gameplay hook bridge MVP against the verified baseline evidence."
                : "M24 remains blocked until M23 gameplay hook evidence verification completes.");
        return data;
    }

    private static Map<String, Object> moduleData(ModuleHookEvidence evidence) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("activationMarkerWritten", evidence.activationMarkerWritten());
        data.put("gameplayHookEvidence", evidence.gameplayHookVerified());
        data.put("id", evidence.id());
        data.put("kind", evidence.kind());
        data.put("liveGameplayHookVerified", evidence.gameplayHookVerified());
        data.put("role", evidence.role());
        data.put("state", evidence.state());
        return data;
    }

    private static List<Map<String, Object>> handlerContracts(Map<String, Object> adapterCoreGameplayEvidence) {
        Object raw = adapterCoreGameplayEvidence.get("handlerContracts");
        if (!(raw instanceof List<?>)) {
            raw = adapterCoreGameplayEvidence.get("handlers");
        }
        if (!(raw instanceof List<?> list)) {
            return List.of();
        }
        List<Map<String, Object>> contracts = new ArrayList<>();
        for (Object item : list) {
            Map<String, Object> contract = EchoNativeJson.asObject(item);
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("adapterCoreContract", String.valueOf(contract.getOrDefault("adapterCoreContract", "")));
            data.put("attached", Boolean.TRUE.equals(contract.get("attached")));
            boolean minecraftRuntimeAccessed = Boolean.TRUE.equals(contract.get("minecraftRuntimeAccessed"));
            data.put("adapterCoreReplayVerified", Boolean.TRUE.equals(contract.get("adapterCoreReplayVerified"))
                    || Boolean.TRUE.equals(contract.get("liveGameplayHookVerified")));
            data.put("event", String.valueOf(contract.getOrDefault("event", "")));
            data.put("handler", String.valueOf(contract.getOrDefault("handler", "")));
            data.put("liveGameplayHookVerified",
                    Boolean.TRUE.equals(contract.get("liveGameplayHookVerified")) && minecraftRuntimeAccessed);
            data.put("minecraftRuntimeAccessed", minecraftRuntimeAccessed);
            data.put("nativeLoaderBackend", String.valueOf(contract.getOrDefault("nativeLoaderBackend", "")));
            data.put("standaloneRuntimeBackend", String.valueOf(contract.getOrDefault("standaloneRuntimeBackend", "")));
            contracts.add(data);
        }
        contracts.sort(Comparator.comparing(contract -> String.valueOf(contract.get("event"))));
        return List.copyOf(contracts);
    }

    private static int verifiedContractCount(List<Map<String, Object>> contracts) {
        int count = 0;
        for (Map<String, Object> contract : contracts) {
            if (Boolean.TRUE.equals(contract.get("attached"))
                    && Boolean.TRUE.equals(contract.get("adapterCoreReplayVerified"))
                    && !blank(contract, "adapterCoreContract")
                    && !blank(contract, "nativeLoaderBackend")
                    && !blank(contract, "standaloneRuntimeBackend")) {
                count++;
            }
        }
        return count;
    }

    private static int verifiedExecutionCount(Map<String, Object> adapterCoreGameplayEvidence) {
        Object raw = adapterCoreGameplayEvidence.get("handlerExecutions");
        if (!(raw instanceof List<?> list)) {
            return 0;
        }
        int count = 0;
        for (Object item : list) {
            Map<String, Object> execution = EchoNativeJson.asObject(item);
            if (Boolean.TRUE.equals(execution.get("executed"))
                    && (Boolean.TRUE.equals(execution.get("adapterCoreReplayVerified"))
                    || Boolean.TRUE.equals(execution.get("liveGameplayHookVerified")))
                    && !blank(execution, "adapterCoreContract")
                    && !blank(execution, "handler")) {
                count++;
            }
        }
        return count;
    }

    private static boolean agent7ExactWorldLiveHostHooksVerified(Map<String, Object> evidence) {
        int requiredCount = intValue(evidence, "requiredHookCount");
        int verifiedCount = intValue(evidence, "verifiedHookCount");
        int exactCallbackEvidenceCount = intValue(evidence, "exactCallbackEvidenceCount");
        if (requiredCount <= 0
                || verifiedCount < requiredCount
                || exactCallbackEvidenceCount < requiredCount
                || !Boolean.TRUE.equals(evidence.get("allRequiredHooksVerified"))
                || !"echo.agent7.native_exact_live_hook_evidence.v1".equals(String.valueOf(evidence.getOrDefault("sourceSchema", "")))) {
            return false;
        }
        List<Map<String, Object>> hooks = objectList(evidence.get("hooks"));
        if (hooks.size() < requiredCount) {
            return false;
        }
        int exactVerifiedHooks = 0;
        for (Map<String, Object> hook : hooks) {
            boolean exactHookVerified = Boolean.TRUE.equals(hook.get("minecraftRuntimeAccessed"))
                    && Boolean.TRUE.equals(hook.get("liveGameplayHookVerified"))
                    && "exact_neoforge_callback_observed".equals(String.valueOf(hook.getOrDefault("evidenceMode", "")));
            if (exactHookVerified) {
                exactVerifiedHooks++;
            }
        }
        return exactVerifiedHooks >= requiredCount;
    }

    private static Map<String, Object> agent7WorldLiveHostHookEvidenceFromSidecar(Path markerPath) throws IOException {
        Path sidecar = agent7SidecarPath(markerPath);
        if (!Files.isRegularFile(sidecar)) {
            return Map.of();
        }
        if (!Files.isRegularFile(markerPath)) {
            return Map.of();
        }
        long sidecarLastModifiedMillis = Files.getLastModifiedTime(sidecar).toMillis();
        long markerLastModifiedMillis = Files.getLastModifiedTime(markerPath).toMillis();
        boolean sidecarFreshForMarker = sidecarLastModifiedMillis >= markerLastModifiedMillis;
        if (!sidecarFreshForMarker) {
            return Map.of();
        }
        Map<String, Object> exactSnapshot = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(sidecar)));
        if (!"echo.agent7.native_exact_live_hook_evidence.v1".equals(String.valueOf(exactSnapshot.getOrDefault("schema", "")))
                || !Boolean.TRUE.equals(exactSnapshot.get("directPersistenceWritten"))) {
            return Map.of();
        }
        int requiredCount = intValue(exactSnapshot, "requiredHookCount");
        int verifiedCount = intValue(exactSnapshot, "verifiedHookCount");
        List<Map<String, Object>> hooks = new ArrayList<>();
        for (Map<String, Object> exactHook : objectList(exactSnapshot.get("hooks"))) {
            boolean verified = Boolean.TRUE.equals(exactHook.get("liveGameplayHookVerified"));
            Map<String, Object> hook = new LinkedHashMap<>(exactHook);
            hook.put("candidateLiveRuntimeSignalObserved", verified);
            hook.put("blockedReason", verified ? "" : "exact_neoforge_callback_not_observed");
            hooks.add(hook);
        }
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("schema", "echo.agent7.world_live_host_hook_evidence.v1");
        evidence.put("source", "direct_sidecar");
        evidence.put("sourceSchema", exactSnapshot.getOrDefault("schema", ""));
        evidence.put("directPersistenceWritten", Boolean.TRUE.equals(exactSnapshot.get("directPersistenceWritten")));
        evidence.put("sidecarFreshForMarker", true);
        evidence.put("sidecarLastModifiedMillis", sidecarLastModifiedMillis);
        evidence.put("markerLastModifiedMillis", markerLastModifiedMillis);
        evidence.put("minecraftRuntimeAccessed", hooks.stream()
                .anyMatch(hook -> Boolean.TRUE.equals(hook.get("minecraftRuntimeAccessed"))));
        evidence.put("requiredHookCount", requiredCount);
        evidence.put("candidateLiveSignalCount", verifiedCount);
        evidence.put("exactCallbackEvidenceCount", verifiedCount);
        evidence.put("verifiedHookCount", verifiedCount);
        evidence.put("allRequiredHooksVerified", requiredCount > 0 && verifiedCount == requiredCount);
        evidence.put("hooks", hooks);
        evidence.put("summary", requiredCount > 0 && verifiedCount == requiredCount
                ? "Agent 7 world/weather/hazard live host hooks were verified from direct exact callback sidecar evidence."
                : "Agent 7 direct exact callback sidecar evidence is incomplete.");
        return evidence;
    }

    private static Path agent7SidecarPath(Path markerPath) throws IOException {
        if (Files.isRegularFile(markerPath)) {
            Map<String, Object> marker = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(markerPath)));
            Map<String, Object> runtimeBridge = EchoNativeJson.asObject(marker.get("runtimeBridge"));
            Map<String, Object> gameplayBridge = EchoNativeJson.asObject(runtimeBridge.get("ashfallGameplayBridge"));
            String configured = String.valueOf(gameplayBridge.getOrDefault(
                    "agent7DirectLiveHookEvidencePath",
                    marker.getOrDefault("agent7DirectLiveHookEvidencePath", "")
            ));
            if (!configured.isBlank()) {
                Path configuredPath = Path.of(configured);
                if (configuredPath.isAbsolute()) {
                    return configuredPath.normalize();
                }
                Path parent = markerPath.toAbsolutePath().normalize().getParent();
                return parent == null ? configuredPath.toAbsolutePath().normalize() : parent.resolve(configuredPath).normalize();
            }
        }
        Path parent = markerPath.toAbsolutePath().normalize().getParent();
        return parent == null
                ? Path.of("agent7-live-hook-evidence.json").toAbsolutePath().normalize()
                : parent.resolve("agent7-live-hook-evidence.json");
    }

    private static List<Map<String, Object>> objectList(Object raw) {
        if (!(raw instanceof List<?> list)) {
            return List.of();
        }
        List<Map<String, Object>> objects = new ArrayList<>();
        for (Object item : list) {
            objects.add(EchoNativeJson.asObject(item));
        }
        return List.copyOf(objects);
    }

    private static boolean blank(Map<String, Object> data, String key) {
        return String.valueOf(data.getOrDefault(key, "")).isBlank();
    }

    private static List<String> modules(Map<String, Object> markerFile, Map<String, Object> markerReport) {
        Object raw = markerFile.get("modules");
        if (!(raw instanceof List<?> list)) {
            raw = markerReport.get("modules");
        }
        if (!(raw instanceof List<?> list)) {
            return List.of();
        }
        List<String> modules = new ArrayList<>();
        for (Object item : list) {
            Map<String, Object> module = EchoNativeJson.asObject(item);
            String id = String.valueOf(module.getOrDefault("id", ""));
            if (!id.isBlank()) {
                modules.add(id);
            }
        }
        modules.sort(String::compareTo);
        return List.copyOf(modules);
    }

    private static Map<String, Boolean> moduleHookVerifiedById(Map<String, Object> markerFile) {
        Object raw = markerFile.get("modules");
        if (!(raw instanceof List<?> list)) {
            return Map.of();
        }
        Map<String, Boolean> modules = new LinkedHashMap<>();
        for (Object item : list) {
            Map<String, Object> module = EchoNativeJson.asObject(item);
            String id = String.valueOf(module.getOrDefault("id", ""));
            if (!id.isBlank()) {
                modules.put(id, Boolean.TRUE.equals(module.get("liveGameplayHookVerified")));
            }
        }
        return Map.copyOf(modules);
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
                    "ECHO-NATIVE-GAMEPLAY-HOOK-REPORT-MISSING",
                    EchoNativeIssueSeverity.ERROR,
                    "Gameplay hook verifier required report missing",
                    "M23 requires " + reportName + " before gameplay hook evidence can be verified.",
                    null,
                    packId,
                    List.of(relativePath(fixture)),
                    "Regenerate the M20-M22 native reports before running phase13 verify gameplay-hooks."
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
                    "ECHO-NATIVE-GAMEPLAY-HOOK-UPSTREAM-BLOCKED",
                    EchoNativeIssueSeverity.ERROR,
                    "Gameplay hook verifier upstream report is not accepted",
                    "M23 requires PASS or PASS_WITH_WARNINGS " + reportName + ".",
                    null,
                    packId,
                    List.of(relativePath(reportPath)),
                    "Resolve upstream native activation and playability diagnostics before verifying gameplay hooks."
            ));
        }
    }

    private static Map<String, Object> data(Map<String, Object> report) {
        return EchoNativeJson.asObject(report == null ? null : report.get("data"));
    }

    private static boolean bool(Map<String, Object> data, String key) {
        return Boolean.TRUE.equals(data.get(key));
    }

    private static int intValue(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException exception) {
            return 0;
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
        data.put("nativeExtractionStarted", false);
        data.put("phase", phase);
        data.put("processLaunched", false);
        data.put("registryInjected", false);
        data.put("registryMutated", false);
        data.put("serviceCodeExecuted", false);
        data.put("transformsEnabled", false);
        data.put("transformsPerformed", false);
        data.put("unsafeRuntimeWorkStarted", false);
        return data;
    }

    private static boolean isBlocking(EchoNativeDiagnostic diagnostic) {
        return diagnostic.severity() == EchoNativeIssueSeverity.ERROR
                || diagnostic.severity() == EchoNativeIssueSeverity.FATAL;
    }

    private static String sha256Of(Path path) throws IOException {
        if (!Files.isRegularFile(path)) {
            return "";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(Files.readAllBytes(path))).toUpperCase(Locale.ROOT);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }

    private static String relativePath(Path path) {
        Path root = Path.of("").toAbsolutePath().normalize();
        Path normalized = path.toAbsolutePath().normalize();
        if (normalized.startsWith(root)) {
            return root.relativize(normalized).toString().replace('\\', '/');
        }
        return path.toString().replace('\\', '/');
    }

    private record ModuleHookEvidence(
            String id,
            String kind,
            String role,
            boolean activationMarkerWritten,
            boolean gameplayHookVerified,
            String state
    ) {
    }
}
