package dev.echo.nativeplatform.cli;

import dev.echo.nativeplatform.contracts.EchoNativeDiagnostic;
import dev.echo.nativeplatform.contracts.EchoNativeIssueSeverity;
import dev.echo.nativeplatform.diagnostics.EchoNativeJson;
import dev.echo.nativeplatform.loader.NativeLoaderAshfallWorldStartupService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

final class EchoNativePlayableBetaVerifier {
    private static final String NATIVE_PRODUCT_GAMEPLAY_BRIDGE_KEY = "nativeProductGameplayBridge";
    private static final String NATIVE_PRODUCT_PLAYABLE_RUNTIME_KEY = "nativeProductPlayableRuntime";

    EchoNativePlayableBetaOutcome verify(
            String packId,
            Path fixture,
            Map<String, Path> requiredReports
    ) throws IOException {
        List<EchoNativeDiagnostic> diagnostics = new ArrayList<>();
        Map<String, Map<String, Object>> reports = new LinkedHashMap<>();
        for (Map.Entry<String, Path> entry : requiredReports.entrySet()) {
            Map<String, Object> report = readRequiredReport(entry.getValue(), fixture, packId, entry.getKey(), diagnostics);
            reports.put(entry.getKey(), report);
            checkAcceptedReport(entry.getKey(), entry.getValue(), report, packId, diagnostics);
        }

        int expectedGameplayHookSignalCount = intValue(data(reports.get("gameplay-hook-signal-audit.json")), "moduleCount");
        List<Map<String, Object>> gates = gates(packId, reports, expectedGameplayHookSignalCount, diagnostics);
        Map<String, Object> liveActivation = readLiveActivationMarker(fixture, packId, diagnostics);
        gates = new ArrayList<>(gates);
        gates.addAll(liveRuntimeGates(packId, fixture, liveActivation, expectedGameplayHookSignalCount, diagnostics));
        gates = gates.stream()
                .sorted(Comparator.comparing(gate -> String.valueOf(gate.get("report")) + ":" + gate.get("field")))
                .toList();
        boolean ready = diagnostics.stream().noneMatch(EchoNativePlayableBetaVerifier::isBlocking)
                && gates.stream().allMatch(gate -> Boolean.TRUE.equals(gate.get("pass")));
        List<String> completedChecks = ready ? List.of(
                "m25_complete",
                "m26_readiness_confirmed",
                "gameplay_hook_signal_audit_pass",
                "gameplay_hook_signal_status_pass",
                "native_product_playable_beta_gate_pass",
                "first_playtest_candidate_ready",
                "support_bundle_ready",
                "rollback_notes_ready",
                "known_limitations_ready",
                "crash_collection_ready",
                "native_runtime_content_visible",
                "native_runtime_registry_applied",
                "native_runtime_resources_applied",
                "native_runtime_product_worldgen_resources_safe",
                "native_runtime_playable_crash_zone_materialized",
                "native_runtime_functional_item_block_wrappers_ready",
                "native_runtime_terminal_lens_index_hud_routes_ready",
                "native_loader_beta_gate_ready"
        ) : List.of();
        int gameplayHookSignalCount = intValue(data(reports.get("gameplay-hook-signal-audit.json")), "signalCount");
        int moduleCount = intValue(data(reports.get("gameplay-hook-signal-audit.json")), "moduleCount");
        List<EchoNativeDiagnostic> sortedDiagnostics = diagnostics.stream()
                .sorted(Comparator.comparing(EchoNativeDiagnostic::code)
                        .thenComparing(diagnostic -> diagnostic.moduleId() == null ? "" : diagnostic.moduleId())
                        .thenComparing(EchoNativeDiagnostic::summary))
                .toList();

        return new EchoNativePlayableBetaOutcome(
                packId,
                phase13M26Completion(packId, ready, gameplayHookSignalCount, moduleCount, gates, completedChecks, sortedDiagnostics),
                nativeLoaderPlayableBetaReadiness(packId, ready, gameplayHookSignalCount, moduleCount, gates, completedChecks, sortedDiagnostics),
                nativeProductLoaderBetaStatus(packId, ready, gameplayHookSignalCount, moduleCount, completedChecks, sortedDiagnostics),
                internalTesterBetaGate(packId, ready, gameplayHookSignalCount, moduleCount, gates, completedChecks, sortedDiagnostics),
                sortedDiagnostics
        );
    }

    private static List<Map<String, Object>> gates(
            String packId,
            Map<String, Map<String, Object>> reports,
            int expectedGameplayHookSignalCount,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        List<Map<String, Object>> gates = new ArrayList<>();
        gates.add(booleanGate(packId, reports, "phase13-m25-completion.json", "phase13M25Complete", true, diagnostics));
        gates.add(booleanGate(packId, reports, "phase13-m25-completion.json", "phase13M26Ready", true, diagnostics));
        gates.add(numberGate(packId, reports, "phase13-m25-completion.json", "gameplayHookSignalCount", expectedGameplayHookSignalCount, diagnostics));
        gates.add(booleanGate(packId, reports, "phase13-m26-readiness.json", "phase13M26Ready", true, diagnostics));
        gates.add(numberGate(packId, reports, "phase13-m26-readiness.json", "remainingGameplayHookCount", 0, diagnostics));
        gates.add(booleanGate(packId, reports, "gameplay-hook-signal-audit.json", "signalsCoverRequiredModules", true, diagnostics));
        gates.add(numberGate(packId, reports, "gameplay-hook-signal-audit.json", "signalCount", expectedGameplayHookSignalCount, diagnostics));
        gates.add(booleanGate(packId, reports, "gameplay-hook-signal-status.json", "signalsAcceptedAsEvidence", true, diagnostics));
        gates.add(numberGate(packId, reports, "gameplay-hook-signal-status.json", "gameplayHookSignalCount", expectedGameplayHookSignalCount, diagnostics));
        gates.add(booleanGate(packId, reports, "native-product-playable-gate.json", "nativeProductPlayableReady", true, diagnostics));
        gates.add(numberGate(packId, reports, "native-product-playable-gate.json", "remainingGameplayHookCount", 0, diagnostics));
        gates.add(booleanGate(packId, reports, "phase13-m19-completion.json", "phase13M19Complete", true, diagnostics));
        gates.add(booleanGate(packId, reports, "phase13-m19-completion.json", "playtestCandidateReady", true, diagnostics));
        gates.add(booleanGate(packId, reports, "first-playtest-open-gate.json", "safeToOpenFirstPlaytest", true, diagnostics));
        gates.add(booleanGate(packId, reports, "first-playtest-open-gate.json", "publicPlaytestOpen", false, diagnostics));
        gates.add(booleanGate(packId, reports, "phase13-native-loader-beta-gate.json", "internalTesterBetaReady", true, diagnostics));
        gates.add(booleanGate(packId, reports, "phase13-native-loader-beta-gate.json", "publicBetaReady", false, diagnostics));
        gates.add(booleanGate(packId, reports, "first-playtest-support-bundle.json", "supportBundleExportReady", true, diagnostics));
        gates.add(booleanGate(packId, reports, "first-playtest-rollback-notes.json", "rollbackNotesReady", true, diagnostics));
        gates.add(booleanGate(packId, reports, "first-playtest-known-limitations.json", "knownLimitationsReady", true, diagnostics));
        gates.add(booleanGate(packId, reports, "first-playtest-crash-report-collection.json", "crashReportCollectionReady", true, diagnostics));
        return gates.stream()
                .sorted(Comparator.comparing(gate -> String.valueOf(gate.get("report")) + ":" + gate.get("field")))
                .toList();
    }

    private static List<Map<String, Object>> liveRuntimeGates(
            String packId,
            Path fixture,
            Map<String, Object> liveActivation,
            int expectedModuleCount,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        if (liveActivation.isEmpty()) {
            return List.of();
        }
        List<Map<String, Object>> gates = new ArrayList<>();
        List<Map<String, Object>> modules = objectList(liveActivation.get("modules"));
        long liveGameplayVerified = modules.stream()
                .filter(module -> Boolean.TRUE.equals(module.get("liveGameplayHookVerified")))
                .count();
        gates.add(gate("isolated-runtime/game/echo-native/module-activation.json", "moduleCount", expectedModuleCount, modules.size(), modules.size() == expectedModuleCount));
        gates.add(liveNumberGate(packId, "liveGameplayHookVerifiedCount", expectedModuleCount, liveGameplayVerified, diagnostics));
        gates.add(liveBooleanGate(packId, "registryMutated", true, liveActivation.get("registryMutated"), diagnostics,
                "The running native client must prove ECHO content was applied to Minecraft registries before beta can open."));
        gates.add(liveBooleanGate(packId, "registryInjected", false, liveActivation.get("registryInjected"), diagnostics,
                "The running native client must not use unsafe registry injection."));
        gates.add(liveBooleanGate(packId, "minecraftResourcesApplied", true, liveActivation.get("minecraftResourcesApplied"), diagnostics,
                "The running native client must prove ECHO resources/data were applied to Minecraft's resource layer before beta can open."));
        gates.add(liveBooleanGate(packId, "creativeContentVisible", true, liveActivation.get("creativeContentVisible"), diagnostics,
                "The running native client must prove ECHO creative/in-game content is visible before beta can open."));
        gates.add(liveBooleanGate(packId, "adapterCoreRuntimeBridgeActive", true, liveActivation.get("adapterCoreRuntimeBridgeActive"), diagnostics,
                "AdapterCore must run the native runtime bridge, not only marker-plan descriptors."));
        Map<String, Object> productGameplayEvidence = productGameplayEvidence(liveActivation);
        gates.add(liveBooleanGate(packId, "nativeProductGameplayContentApplied", true, productGameplayEvidence.get("applied"), diagnostics,
                "The running native client must prove product mission/world/progression data was discovered through AdapterCore before beta can open."));
        gates.add(liveBooleanGate(packId, "nativeFirstPlayableLoopReady", true, productGameplayEvidence.get("firstPlayableLoopReady"), diagnostics,
                "The running native client must prove a first product mission/world/progression loop is available before beta can open."));
        gates.add(liveMinimumGate(packId, "nativeProductMissionDefinitionCount", 1, intValue(productGameplayEvidence, "missionDefinitionCount"), diagnostics,
                "The running native client must prove product mission definitions are visible through the native gameplay bridge before beta can open."));
        gates.add(liveMinimumGate(packId, "nativeProductWorldRegionCount", 1, intValue(productGameplayEvidence, "worldRegionCount"), diagnostics,
                "The running native client must prove product world-region data is visible through the native gameplay bridge before beta can open."));
        gates.add(liveMinimumGate(packId, "nativeProductProgressionAdvancementCount", 1, intValue(productGameplayEvidence, "progressionAdvancementCount"), diagnostics,
                "The running native client must prove product progression advancement data is visible through the native gameplay bridge before beta can open."));
        Map<String, Object> runtimeBridge = EchoNativeJson.asObject(liveActivation.get("runtimeBridge"));
        Map<String, Object> registryBridge = EchoNativeJson.asObject(runtimeBridge.get("registryBridge"));
        Map<String, Object> resourceBridge = EchoNativeJson.asObject(runtimeBridge.get("resourceBridge"));
        Map<String, Object> worldStartupBridge = EchoNativeJson.asObject(runtimeBridge.get("worldStartupBridge"));
        Map<String, Object> datapackBridge = EchoNativeJson.asObject(resourceBridge.get("datapackBridge"));
        Map<String, Object> lifecycleBridge = EchoNativeJson.asObject(runtimeBridge.get("lifecycleBridge"));
        Map<String, Object> eventBridge = EchoNativeJson.asObject(runtimeBridge.get("eventBridge"));
        Map<String, Object> serviceBridge = EchoNativeJson.asObject(runtimeBridge.get("serviceBridge"));
        Map<String, Object> liveClientProbe = EchoNativeJson.asObject(runtimeBridge.get("liveClientProbe"));
        Map<String, Object> playableBetaRuntime = playableRuntime(liveClientProbe);
        gates.add(liveBooleanGate(packId, "liveClientProbeExecuted", true, liveClientProbe.get("executed"), diagnostics,
                "The running native client must prove it reached a live Minecraft player/world before beta can open."));
        gates.add(liveBooleanGate(packId, "nativeProductWorldOpenedLive", true, liveClientProbe.get("nativeProductWorldOpened"), diagnostics,
                "The running native client must prove the marked Ashfall product world reached live Minecraft player/world state before beta can open."));
        gates.add(liveBooleanGate(packId, "nativeProductWorldLevelDatPresentLive", true, liveClientProbe.get("nativeProductWorldLevelDatPresent"), diagnostics,
                "The running native client must prove Minecraft created/opened level.dat in the marked Ashfall product world."));
        gates.add(liveBooleanGate(packId, "nativeProductWorldMarkerWrittenLive", true, liveClientProbe.get("nativeProductWorldMarkerWritten"), diagnostics,
                "The running native client must prove the live world is marked as an Ashfall Native Loader product world."));
        gates.add(liveBooleanGate(packId, "nativeProductWorldDatapackStagedLive", true, liveClientProbe.get("nativeProductWorldDatapackStaged"), diagnostics,
                "The running native client must prove the live Ashfall product world has the required product datapack staged."));
        gates.add(liveBooleanGate(packId, "nativeProductWorldPresetForcedLive", true, liveClientProbe.get("nativeProductWorldPresetForced"), diagnostics,
                "The running native client must prove the live Ashfall product world is tied to the forced Ashfall preset."));
        gates.add(liveBooleanGate(packId, "nativeProductPlayableRuntimeAttempted", true, playableBetaRuntime.get("attempted"), diagnostics,
                "The running native client must attempt the product playable runtime bridge before beta can open."));
        gates.add(liveBooleanGate(packId, "nativeProductPlayableCrashZoneMaterialized", true, playableBetaRuntime.get("crashZoneMaterialized"), diagnostics,
                "The running native client must materialize visible product crash-zone terrain/blocks before beta can open."));
        gates.add(liveBooleanGate(packId, "nativeProductPlayableStarterItemsGranted", true, playableBetaRuntime.get("starterItemsGranted"), diagnostics,
                "The running native client must grant starter product/ECHO tools before beta can open."));
        gates.add(liveBooleanGate(packId, "nativeProductPlayableRuntimeFailuresEmpty", true, listEmpty(playableBetaRuntime.get("failures")), diagnostics,
                "The running native client must report no product playable runtime scaffold failures before beta can open."));
        gates.add(liveBooleanGate(packId, "nativeProductPlayableTerminalLensIndexHudRoutesReady", true, playableBetaRuntime.get("terminalLensIndexHudRoutesReady"), diagnostics,
                "The running native client must prove Terminal, Lens, Index, and HUD routes are ready from gameplay before beta can open."));
        Set<String> mutatedLedgerSurfaces = mutatedLiveMutationSurfaces(playableBetaRuntime.get("mutationLedger"));
        gates.add(liveBooleanGate(packId, "nativeProductPlayableMutationLedgerRecorded", true, !objectList(playableBetaRuntime.get("mutationLedger")).isEmpty(), diagnostics,
                "The running native client must record a Native Loader mutation ledger instead of relying on summary booleans."));
        gates.add(liveMinimumGate(packId, "nativeProductPlayableMutatedSurfaceCount", 4, mutatedLedgerSurfaces.size(), diagnostics,
                "The running native client must prove inventory, world block, save-data, and HUD surfaces mutated through AdapterCore."));
        gates.add(liveBooleanGate(packId, "nativeProductPlayableRequiredMutationSurfacesMutated", true, mutatedLedgerSurfaces.containsAll(Set.of("inventory", "world_blocks", "save_data", "hud")), diagnostics,
                "The running native client must mutate every required AdapterCore Native Loader surface before beta can open."));
        gates.add(liveMinimumGate(packId, "nativeProductPlayableServerBlocksPlaced", 1, intValue(playableBetaRuntime, "serverBlocksPlaced"), diagnostics,
                "The running native client must prove product blocks were placed through the integrated server so the beta is not only a client-side visual."));
        gates.add(liveMinimumGate(packId, "nativeProductPlayableServerCommandsSent", 1, intValue(playableBetaRuntime, "serverCommandsSent"), diagnostics,
                "The running native client must prove the server command path for product starter terrain/tools is available before beta can open."));
        gates.add(liveMinimumGate(packId, "nativeProductPlayableClientBlocksPlaced", 1, intValue(playableBetaRuntime, "clientBlocksPlaced"), diagnostics,
                "The running native client must prove a client-side visual fallback exists for product starter terrain before beta can open."));
        gates.add(liveBooleanGate(packId, "nativeProductDatapackSafeModeGuardActive", true,
                firstPresent(datapackBridge, "nativeProductDatapackSafeModeGuardActive", "safeModeGuardActive"),
                diagnostics,
                "The running native client must block unsafe generated save datapacks so Minecraft does not enter Safe Mode."));
        Map<String, Object> nativeLoaderResourceHost = EchoNativeJson.asObject(resourceBridge.get("nativeLoaderResourceHost"));
        int preWorldCreationMountCount = firstIntValue(resourceBridge, "nativeResourceHostPreWorldCreationMountCount");
        if (preWorldCreationMountCount == 0) {
            preWorldCreationMountCount = firstIntValue(nativeLoaderResourceHost, "mountedPreWorldCreationResourceCount");
        }
        int preWorldCreationDataPackMountCount =
                firstIntValue(resourceBridge, "nativeResourceHostPreWorldCreationDataPackMountCount");
        if (preWorldCreationDataPackMountCount == 0) {
            preWorldCreationDataPackMountCount = firstIntValue(nativeLoaderResourceHost, "mountedDataPackResourceCount");
        }
        int preWorldCreationResourcePackMountCount =
                firstIntValue(resourceBridge, "nativeResourceHostPreWorldCreationResourcePackMountCount");
        if (preWorldCreationResourcePackMountCount == 0) {
            preWorldCreationResourcePackMountCount = firstIntValue(nativeLoaderResourceHost, "mountedResourcePackResourceCount");
        }
        Object mountedBeforeRegistryWorldCreation = firstPresent(resourceBridge,
                "nativeResourceHostMountedBeforeRegistryWorldCreation");
        if (mountedBeforeRegistryWorldCreation == null) {
            mountedBeforeRegistryWorldCreation = preWorldCreationMountCount > 0
                    && preWorldCreationDataPackMountCount > 0
                    && preWorldCreationResourcePackMountCount > 0;
        }
        gates.add(liveBooleanGate(packId, "nativeResourceHostMountedBeforeRegistryWorldCreation", true,
                mountedBeforeRegistryWorldCreation,
                diagnostics,
                "The running native client must prove NativeLoaderResourceHost mounted product packs before registry/world creation."));
        gates.add(liveMinimumGate(packId, "nativeResourceHostPreWorldCreationMountCount", 2,
                preWorldCreationMountCount,
                diagnostics,
                "The running native client must prove both product resource and datapack mounts are registered before registry/world creation."));
        gates.add(liveMinimumGate(packId, "nativeResourceHostPreWorldCreationDataPackMountCount", 1,
                preWorldCreationDataPackMountCount,
                diagnostics,
                "The running native client must prove the product datapack is mounted before registry/world creation."));
        gates.add(liveMinimumGate(packId, "nativeResourceHostPreWorldCreationResourcePackMountCount", 1,
                preWorldCreationResourcePackMountCount,
                diagnostics,
                "The running native client must prove the hidden module resource pack is mounted before registry/world creation."));
        gates.add(liveBooleanGate(packId, "nativeProductWorldStartupBridgeApplied", true,
                worldStartupBridge.get("applied"),
                diagnostics,
                "The running native client must prove Native Loader prepared the Ashfall product world before Minecraft handoff."));
        gates.add(liveBooleanGate(packId, "nativeProductWorldStartupBridgeNotBlocked", true,
                !Boolean.TRUE.equals(worldStartupBridge.get("blocked")),
                diagnostics,
                "The running native client must prove product world startup did not fall through a blocked or vanilla fallback path."));
        gates.add(liveBooleanGate(packId, "nativeProductWorldMarkerWritten", true,
                worldStartupBridge.get("productWorldMarkerWritten"),
                diagnostics,
                "The running native client must prove the marked Ashfall product world was written before beta can open."));
        gates.add(liveBooleanGate(packId, "nativeProductWorldDatapackStaged", true,
                worldStartupBridge.get("stagedDatapackReady"),
                diagnostics,
                "The running native client must prove the Ashfall product world datapack was staged before beta can open."));
        gates.add(liveBooleanGate(packId, "nativeProductWorldPresetForced", true,
                NativeLoaderAshfallWorldStartupService.WORLD_PRESET_ID.equals(firstPresent(worldStartupBridge, "worldPreset", "forcedWorldPreset")),
                diagnostics,
                "The running native client must prove Native Loader forced the Ashfall world preset before beta can open."));
        gates.add(liveMinimumGate(packId, "nativeProductWorldgenBiomeCount", 1,
                firstIntValue(resourceBridge, "nativeProductWorldgenBiomeCount", "productWorldgenBiomeCount"),
                diagnostics,
                "The running native client must prove product biome JSON exists in generated native resources before beta can open."));
        gates.add(liveBooleanGate(packId, "nativeProductWorldgenPresetPresent", true,
                firstPresent(resourceBridge, "nativeProductWorldgenPresetPresent", "productWorldgenPresetPresent"),
                diagnostics,
                "The running native client must prove the generated native resources contain the overworld world-preset definition before beta can open."));
        gates.add(liveBooleanGate(packId, "nativeProductOverworldSettingsPresent", true,
                firstPresent(resourceBridge, "nativeProductOverworldSettingsPresent", "nativeProductWorldgenRootMarkerPresent", "productWorldgenRootMarkerPresent"),
                diagnostics,
                "The running native client must prove the generated native resources contain the configured product overworld/root settings before beta can open."));
        gates.add(liveMinimumGate(packId, "registeredBlockCount", 1, intValue(registryBridge, "registeredBlockCount"), diagnostics,
                "The running native client must prove at least one ECHO block was registered into Minecraft's block registry before beta can open."));
        gates.add(liveBooleanGate(packId, "nativeBetaItemWrapperAvailable", true, registryBridge.get("nativeBetaItemWrapperAvailable"), diagnostics,
                "The running native client must prove native beta items are functional wrappers, not inert vanilla items."));
        gates.add(liveBooleanGate(packId, "nativeBetaBlockWrapperAvailable", true, registryBridge.get("nativeBetaBlockWrapperAvailable"), diagnostics,
                "The running native client must prove native beta blocks are functional wrappers, not inert vanilla blocks."));
        gates.add(liveMinimumGate(packId, "nativeBetaFunctionalItemCount", 1, intValue(registryBridge, "nativeBetaFunctionalItemCount"), diagnostics,
                "The running native client must prove at least one functional native beta item wrapper was registered."));
        gates.add(liveMinimumGate(packId, "nativeBetaFunctionalBlockCount", 1, intValue(registryBridge, "nativeBetaFunctionalBlockCount"), diagnostics,
                "The running native client must prove at least one functional native beta block wrapper was registered."));
        gates.add(liveBooleanGate(packId, "nativeCreativeTabBridgeApplied", true, registryBridge.get("nativeCreativeTabBridgeApplied"), diagnostics,
                "The running native client must prove ECHO content was attached to a creative tab path before beta can open."));
        gates.add(liveBooleanGate(packId, "nativeCreativeModuleTabContentVisible", true,
                registryBridge.get("nativeCreativeModuleTabContentVisible"), diagnostics,
                "The running native client must prove native module creative tabs are populated from native registry content."));
        gates.add(liveBooleanGate(packId, "nativeCreativeModuleTabRegistryBacked", true,
                registryBridge.get("nativeCreativeModuleTabRegistryBacked"), diagnostics,
                "The running native client must prove declared creative tab items are backed by native registry content."));
        gates.add(liveMinimumGate(packId, "nativeCreativeModuleTabVisibleItemCount", 1,
                intValue(registryBridge, "nativeCreativeModuleTabVisibleItemCount"), diagnostics,
                "The running native client must prove at least one native registry item is visible through a native module creative tab."));
        gates.add(liveMinimumGate(packId, "visibleCreativeTabPathCount", 1,
                intValue(registryBridge, "visibleCreativeTabPathCount"), diagnostics,
                "The running native client must prove ECHO content was surfaced through at least one stable creative tab path before beta can open."));
        gates.add(liveMinimumGate(packId, "safeLifecycleHookRunCount", 1, intValue(lifecycleBridge, "safeLifecycleHookRunCount"), diagnostics,
                "The running native client must prove AdapterCore ran safe lifecycle hooks before beta can open."));
        gates.add(liveMinimumGate(packId, "safeEventHookRunCount", 1, intValue(eventBridge, "safeEventHookRunCount"), diagnostics,
                "The running native client must prove AdapterCore attached and ran safe event hook descriptors before beta can open."));
        gates.add(liveMinimumGate(packId, "runtimeInitializedServiceCount", 1, runtimeInitializedServiceCount(serviceBridge), diagnostics,
                "The running native client must prove AdapterCore started approved native service handles, not only marker-only module registrations."));
        gates.add(liveBooleanGate(packId, "agent3GameplaySurfaceCoverageReady", true, agent3GameplaySurfaceCoverageReady(liveActivation), diagnostics,
                "The running native client must prove player/recovery, HoloMap/Lens/Wiki, weather/sound/atmosphere, and screen-safe UI native service surfaces are all initialized before beta can open."));
        gates.add(liveMinimumGate(packId, "agent3ReadySurfaceCount", 4, agent3GameplaySurfaceReadyCount(liveActivation), diagnostics,
                "The running native client must prove all four Agent 3 gameplay surface groups have initialized AdapterCore native service handles before beta can open."));
        if (modules.size() != expectedModuleCount) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-LIVE-MODULE-COUNT-BLOCKED",
                    EchoNativeIssueSeverity.ERROR,
                    "Live native module count does not match required product module count",
                    "module-activation.json reports " + modules.size() + " modules; expected " + expectedModuleCount + ".",
                    null,
                    packId,
                    List.of(relativeReportPath(fixture.resolve("isolated-runtime/game/echo-native/module-activation.json"))),
                    "Restart the native loader after regenerating the product fixture and runtime artifact map."
            ));
        }
        return gates;
    }

    private static Map<String, Object> productGameplayEvidence(Map<String, Object> liveActivation) {
        Map<String, Object> runtimeBridge = EchoNativeJson.asObject(liveActivation.get("runtimeBridge"));
        Map<String, Object> productGameplayBridge = EchoNativeJson.asObject(runtimeBridge.get(NATIVE_PRODUCT_GAMEPLAY_BRIDGE_KEY));
        if (!productGameplayBridge.isEmpty()) {
            return productGameplayEvidence(
                    "runtime_bridge",
                    intValue(productGameplayBridge, "missionDefinitionCount"),
                    intValue(productGameplayBridge, "worldRegionCount"),
                    intValue(productGameplayBridge, "progressionAdvancementCount"),
                    Boolean.TRUE.equals(productGameplayBridge.get("applied"))
                            || Boolean.TRUE.equals(liveActivation.get("nativeProductGameplayContentApplied")),
                    Boolean.TRUE.equals(productGameplayBridge.get("firstPlayableLoopReady"))
                            || Boolean.TRUE.equals(liveActivation.get("nativeFirstPlayableLoopReady")));
        }
        long markerMissions = intValue(liveActivation, "nativeProductMissionDefinitionCount");
        long markerWorldRegions = intValue(liveActivation, "nativeProductWorldRegionCount");
        long markerProgression = intValue(liveActivation, "nativeProductProgressionAdvancementCount");
        if (liveActivation.containsKey("nativeProductGameplayContentApplied")
                || liveActivation.containsKey("nativeFirstPlayableLoopReady")
                || liveActivation.containsKey("nativeGameplayHandlerExecuted")
                || markerMissions > 0
                || markerWorldRegions > 0
                || markerProgression > 0) {
            return productGameplayEvidence(
                    "live_marker",
                    markerMissions,
                    markerWorldRegions,
                    markerProgression,
                    Boolean.TRUE.equals(liveActivation.get("nativeProductGameplayContentApplied")),
                    Boolean.TRUE.equals(liveActivation.get("nativeFirstPlayableLoopReady")));
        }
        Path resourcePack = liveResourcePack(liveActivation);
        if (resourcePack == null || !Files.isRegularFile(resourcePack)) {
            return productGameplayEvidence("missing", 0, 0, 0);
        }
        Map<String, String> contentPrefixes = productGameplayContentDataPrefixes(runtimeBridge);
        if (contentPrefixes.isEmpty()) {
            return productGameplayEvidence("missing_content_prefixes", 0, 0, 0);
        }
        Set<String> missions = new TreeSet<>();
        Set<String> worldRegions = new TreeSet<>();
        Set<String> progression = new TreeSet<>();
        String missionsPrefix = contentPrefix(contentPrefixes, "missions");
        String worldRegionsPrefix = contentPrefix(contentPrefixes, "world_regions", "worldRegions");
        String progressionPrefix = contentPrefix(contentPrefixes, "progression_advancements", "progressionAdvancements", "advancements");
        try (ZipInputStream input = new ZipInputStream(Files.newInputStream(resourcePack))) {
            ZipEntry entry;
            while ((entry = input.getNextEntry()) != null) {
                String name = entry.getName().replace('\\', '/');
                if (entry.isDirectory() || !name.endsWith(".json")) {
                    continue;
                }
                if (!missionsPrefix.isBlank() && name.startsWith(missionsPrefix)) {
                    addDataPath(missions, name, missionsPrefix);
                } else if (!worldRegionsPrefix.isBlank() && name.startsWith(worldRegionsPrefix)) {
                    addDataPath(worldRegions, name, worldRegionsPrefix);
                } else if (!progressionPrefix.isBlank() && name.startsWith(progressionPrefix)) {
                    addDataPath(progression, name, progressionPrefix);
                }
            }
        } catch (IOException exception) {
            return productGameplayEvidence("resource_pack_unreadable", 0, 0, 0);
        }
        return productGameplayEvidence("resource_pack_prefix_map", missions.size(), worldRegions.size(), progression.size());
    }

    private static Map<String, Object> playableRuntime(Map<String, Object> liveClientProbe) {
        Map<String, Object> productRuntime = EchoNativeJson.asObject(liveClientProbe.get(NATIVE_PRODUCT_PLAYABLE_RUNTIME_KEY));
        if (!productRuntime.isEmpty()) {
            return productRuntime;
        }
        for (Map.Entry<String, Object> entry : liveClientProbe.entrySet()) {
            String key = String.valueOf(entry.getKey()).toLowerCase(java.util.Locale.ROOT);
            if (NATIVE_PRODUCT_PLAYABLE_RUNTIME_KEY.toLowerCase(java.util.Locale.ROOT).equals(key)
                    || !key.endsWith("playablebetaruntime")) {
                continue;
            }
            Map<String, Object> runtime = EchoNativeJson.asObject(entry.getValue());
            if (!runtime.isEmpty()) {
                return runtime;
            }
        }
        return Map.of();
    }

    private static int runtimeInitializedServiceCount(Map<String, Object> serviceBridge) {
        int initialized = intValue(serviceBridge, "runtimeInitializedServiceCount");
        if (initialized > 0) {
            return initialized;
        }
        return intValue(serviceBridge, "startedServiceCount");
    }

    private static boolean agent3GameplaySurfaceCoverageReady(Map<String, Object> liveActivation) {
        if (Boolean.TRUE.equals(liveActivation.get("nativeAgent3GameplaySurfaceCoverageReady"))) {
            return true;
        }
        Map<String, Object> runtimeBridge = EchoNativeJson.asObject(liveActivation.get("runtimeBridge"));
        Map<String, Object> serviceBridge = EchoNativeJson.asObject(runtimeBridge.get("serviceBridge"));
        return Boolean.TRUE.equals(serviceBridge.get("agent3GameplaySurfaceCoverageReady"));
    }

    private static int agent3GameplaySurfaceReadyCount(Map<String, Object> liveActivation) {
        Object markerRaw = liveActivation.get("nativeAgent3GameplaySurfaceReadyCount");
        if (markerRaw instanceof Number number) {
            return number.intValue();
        }
        Map<String, Object> runtimeBridge = EchoNativeJson.asObject(liveActivation.get("runtimeBridge"));
        Map<String, Object> serviceBridge = EchoNativeJson.asObject(runtimeBridge.get("serviceBridge"));
        return intValue(serviceBridge, "agent3ReadySurfaceCount");
    }

    private static Map<String, Object> productGameplayEvidence(
            String source,
            long missionDefinitionCount,
            long worldRegionCount,
            long progressionAdvancementCount
    ) {
        return productGameplayEvidence(source, missionDefinitionCount, worldRegionCount, progressionAdvancementCount, false, false);
    }

    private static Map<String, Object> productGameplayEvidence(
            String source,
            long missionDefinitionCount,
            long worldRegionCount,
            long progressionAdvancementCount,
            boolean applied,
            boolean firstPlayableLoopReady
    ) {
        boolean dataDiscovered = missionDefinitionCount > 0 && worldRegionCount > 0 && progressionAdvancementCount > 0;
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("applied", applied);
        evidence.put("dataDiscovered", dataDiscovered);
        evidence.put("source", source);
        evidence.put("firstPlayableLoopReady", firstPlayableLoopReady);
        evidence.put("missionDefinitionCount", missionDefinitionCount);
        evidence.put("worldRegionCount", worldRegionCount);
        evidence.put("progressionAdvancementCount", progressionAdvancementCount);
        return evidence;
    }

    private static Path liveResourcePack(Map<String, Object> liveActivation) {
        Map<String, Object> runtimeBridge = EchoNativeJson.asObject(liveActivation.get("runtimeBridge"));
        Map<String, Object> resourceBridge = EchoNativeJson.asObject(runtimeBridge.get("resourceBridge"));
        Object raw = resourceBridge.get("resourcePack");
        if (raw == null || String.valueOf(raw).isBlank()) {
            return null;
        }
        return Path.of(String.valueOf(raw));
    }

    private static Map<String, String> productGameplayContentDataPrefixes(Map<String, Object> runtimeBridge) {
        Map<String, Object> productGameplayBridge = EchoNativeJson.asObject(runtimeBridge.get(NATIVE_PRODUCT_GAMEPLAY_BRIDGE_KEY));
        Map<String, String> prefixes = stringMap(productGameplayBridge.get("contentDataPrefixes"));
        if (!prefixes.isEmpty()) {
            return prefixes;
        }
        Map<String, Object> resourceBridge = EchoNativeJson.asObject(runtimeBridge.get("resourceBridge"));
        return stringMap(firstPresent(resourceBridge, "nativeProductGameplayContentDataPrefixes", "contentDataPrefixes"));
    }

    private static Map<String, String> stringMap(Object value) {
        Map<String, Object> raw = EchoNativeJson.asObject(value);
        Map<String, String> strings = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : raw.entrySet()) {
            if (entry.getValue() == null || String.valueOf(entry.getValue()).isBlank()) {
                continue;
            }
            strings.put(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
        }
        return strings;
    }

    private static String contentPrefix(Map<String, String> prefixes, String... keys) {
        for (String key : keys) {
            String direct = normalizedContentPrefix(prefixes.get(key));
            if (!direct.isBlank()) {
                return direct;
            }
            String normalizedKey = key.toLowerCase(java.util.Locale.ROOT);
            for (Map.Entry<String, String> entry : prefixes.entrySet()) {
                if (entry.getKey().toLowerCase(java.util.Locale.ROOT).equals(normalizedKey)) {
                    return normalizedContentPrefix(entry.getValue());
                }
            }
        }
        return "";
    }

    private static String normalizedContentPrefix(String prefix) {
        if (prefix == null) {
            return "";
        }
        String normalized = prefix.trim().replace('\\', '/');
        if (normalized.isBlank()) {
            return "";
        }
        return normalized.endsWith("/") ? normalized : normalized + "/";
    }

    private static void addDataPath(Set<String> ids, String name, String prefix) {
        String path = name.substring(prefix.length(), name.length() - ".json".length());
        if (!path.isBlank()) {
            ids.add(path);
        }
    }

    private static Map<String, Object> liveBooleanGate(
            String packId,
            String field,
            boolean expected,
            Object actual,
            List<EchoNativeDiagnostic> diagnostics,
            String detail
    ) {
        boolean pass = Boolean.valueOf(expected).equals(actual);
        if (!pass) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-LIVE-CONTENT-VISIBILITY-BLOCKED",
                    EchoNativeIssueSeverity.ERROR,
                    "Live native runtime content visibility is not ready",
                    detail + " Expected " + field + "=" + expected + " but found " + (actual == null ? "missing" : actual) + ".",
                    null,
                    packId,
                    List.of("fixtures/" + packId + "/isolated-runtime/game/echo-native/module-activation.json"),
                    "Implement AdapterCore native resource, registry, event, and service bridges before marking native product playable beta ready."
            ));
        }
        return gate("isolated-runtime/game/echo-native/module-activation.json", field, expected, actual, pass);
    }

    private static Map<String, Object> liveMinimumGate(
            String packId,
            String field,
            long minimum,
            long actual,
            List<EchoNativeDiagnostic> diagnostics,
            String detail
    ) {
        boolean pass = actual >= minimum;
        if (!pass) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-LIVE-MINIMUM-GATE-BLOCKED",
                    EchoNativeIssueSeverity.ERROR,
                    "Live native runtime minimum gate is not ready",
                    detail + " Expected " + field + ">=" + minimum + " but found " + actual + ".",
                    null,
                    packId,
                    List.of("fixtures/" + packId + "/isolated-runtime/game/echo-native/module-activation.json"),
                    "Complete the AdapterCore native runtime bridge evidence before marking native product playable beta ready."
            ));
        }
        return gate("isolated-runtime/game/echo-native/module-activation.json", field, minimum, actual, pass);
    }

    private static Map<String, Object> liveNumberGate(
            String packId,
            String field,
            long expected,
            long actual,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        boolean pass = expected == actual;
        if (!pass) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-LIVE-GAMEPLAY-HOOK-BLOCKED",
                    EchoNativeIssueSeverity.ERROR,
                    "Live native gameplay hooks are not verified in the running client",
                    "The running client must verify " + expected + " live gameplay hooks, but verified " + actual + ".",
                    null,
                    packId,
                    List.of("fixtures/" + packId + "/isolated-runtime/game/echo-native/module-activation.json"),
                    "Replace marker-only gameplay hook evidence with actual runtime hook visibility evidence."
            ));
        }
        return gate("isolated-runtime/game/echo-native/module-activation.json", field, expected, actual, pass);
    }

    private static Map<String, Object> phase13M26Completion(
            String packId,
            boolean ready,
            int gameplayHookSignalCount,
            int moduleCount,
            List<Map<String, Object>> gates,
            List<String> completedChecks,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m26_playable_beta_gate_closeout", diagnostics);
        data.put("nativeProductPlayableReady", ready);
        data.put("blockedReportCount", diagnostics.stream().filter(EchoNativePlayableBetaVerifier::isBlocking).count());
        data.put("completedChecks", completedChecks);
        data.put("gameplayHookSignalCount", gameplayHookSignalCount);
        data.put("gateCount", gates.size());
        data.put("gates", gates);
        data.put("internalTesterBetaReady", ready);
        data.put("missingGameplayHookSignalCount", Math.max(0, moduleCount - gameplayHookSignalCount));
        data.put("moduleCount", moduleCount);
        data.put("packId", packId);
        data.put("phase13M26Complete", ready);
        data.put("publicReleaseReady", false);
        data.put("summary", ready
                ? "Phase 13 M26 is complete: native product playability is ready for internal native-loader testers."
                : "Phase 13 M26 remains blocked by playable beta gate evidence.");
        return data;
    }

    private static Map<String, Object> nativeLoaderPlayableBetaReadiness(
            String packId,
            boolean ready,
            int gameplayHookSignalCount,
            int moduleCount,
            List<Map<String, Object>> gates,
            List<String> completedChecks,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m26_native_loader_playable_beta_readiness", diagnostics);
        data.put("completedChecks", completedChecks);
        data.put("gameplayHookSignalCount", gameplayHookSignalCount);
        data.put("gateCount", gates.size());
        data.put("internalTesterBetaReady", ready);
        data.put("missingGameplayHookSignalCount", Math.max(0, moduleCount - gameplayHookSignalCount));
        data.put("nativeLoaderPlayableBetaReady", ready);
        data.put("nextMilestone", ready ? "native_loader_beta_tester_iteration" : "phase13.m26.playable_beta_gate_closeout");
        data.put("packId", packId);
        data.put("publicReleaseReady", false);
        data.put("summary", ready
                ? "Native loader playable beta readiness is PASS for internal product testers."
                : "Native loader playable beta readiness remains blocked.");
        return data;
    }

    private static Map<String, Object> nativeProductLoaderBetaStatus(
            String packId,
            boolean ready,
            int gameplayHookSignalCount,
            int moduleCount,
            List<String> completedChecks,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m26_native_product_loader_beta_status", diagnostics);
        data.put("nativeProductPlayableReady", ready);
        data.put("completedChecks", completedChecks);
        data.put("experimentalNativeLoaderLabelRequired", true);
        data.put("gameplayHookSignalCount", gameplayHookSignalCount);
        data.put("internalTesterBetaReady", ready);
        data.put("missingGameplayHookSignalCount", Math.max(0, moduleCount - gameplayHookSignalCount));
        data.put("packId", packId);
        data.put("publicReleaseReady", false);
        data.put("requiredTesterLabel", "EXPERIMENTAL ECHO NATIVE LOADER - INTERNAL TEST ONLY");
        data.put("summary", ready
                ? "Native product loader beta status is ready for internal tester iteration."
                : "Native product loader beta status remains blocked.");
        return data;
    }

    private static Map<String, Object> internalTesterBetaGate(
            String packId,
            boolean ready,
            int gameplayHookSignalCount,
            int moduleCount,
            List<Map<String, Object>> gates,
            List<String> completedChecks,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m26_internal_tester_beta_gate", diagnostics);
        data.put("completedChecks", completedChecks);
        data.put("gameplayHookSignalCount", gameplayHookSignalCount);
        data.put("gateCount", gates.size());
        data.put("gates", gates);
        data.put("internalTesterBetaOpen", ready);
        data.put("internalTesterBetaReady", ready);
        data.put("missingGameplayHookSignalCount", Math.max(0, moduleCount - gameplayHookSignalCount));
        data.put("packId", packId);
        data.put("publicBetaReady", false);
        data.put("publicReleaseReady", false);
        data.put("summary", ready
                ? "Internal tester beta gate is open for native product loader testing; public release remains closed."
                : "Internal tester beta gate remains closed.");
        return data;
    }

    private static Map<String, Object> booleanGate(
            String packId,
            Map<String, Map<String, Object>> reports,
            String reportName,
            String field,
            boolean expected,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Object actual = data(reports.get(reportName)).get(field);
        boolean pass = Boolean.valueOf(expected).equals(actual);
        if (!pass) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-PHASE13-M26-GATE-FIELD-BLOCKED",
                    EchoNativeIssueSeverity.ERROR,
                    "Phase 13 M26 playable beta gate field is not ready",
                    reportName + " must report " + field + "=" + expected + " before internal beta can open.",
                    null,
                    packId,
                    List.of("reports/echo-native/" + packId + "/" + reportName),
                    "Regenerate the M25 gameplay hook and tester package report chain before M26."
            ));
        }
        return gate(reportName, field, expected, actual, pass);
    }

    private static Map<String, Object> numberGate(
            String packId,
            Map<String, Map<String, Object>> reports,
            String reportName,
            String field,
            long expected,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Object actual = data(reports.get(reportName)).get(field);
        boolean pass = Long.valueOf(expected).equals(asLong(actual));
        if (!pass) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-PHASE13-M26-GATE-COUNT-BLOCKED",
                    EchoNativeIssueSeverity.ERROR,
                    "Phase 13 M26 playable beta gate count is not ready",
                    reportName + " must report " + field + "=" + expected + " before internal beta can open.",
                    null,
                    packId,
                    List.of("reports/echo-native/" + packId + "/" + reportName),
                    "Regenerate the M25 gameplay hook signal reports before M26."
            ));
        }
        return gate(reportName, field, expected, actual, pass);
    }

    private static Map<String, Object> gate(String reportName, String field, Object expected, Object actual, boolean pass) {
        Map<String, Object> gate = new LinkedHashMap<>();
        gate.put("report", reportName);
        gate.put("field", field);
        gate.put("expected", expected);
        gate.put("actual", actual == null ? "" : actual);
        gate.put("pass", pass);
        gate.put("downloadAllowed", false);
        gate.put("nativeExtractionStarted", false);
        gate.put("processLaunched", false);
        gate.put("classloaderCreated", false);
        gate.put("filesystemMutated", false);
        return gate;
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
                    "ECHO-NATIVE-PHASE13-M26-UPSTREAM-REPORT-BLOCKED",
                    EchoNativeIssueSeverity.ERROR,
                    "Phase 13 M26 upstream report is not accepted",
                    "M26 requires PASS or accepted PASS_WITH_WARNINGS " + reportName + ".",
                    null,
                    packId,
                    List.of(relativeReportPath(reportPath)),
                    "Resolve upstream playable beta diagnostics before M26."
            ));
        }
        if (hasUnsafeRuntimeWork(data(report))) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-PHASE13-M26-SAFETY-VIOLATION",
                    EchoNativeIssueSeverity.ERROR,
                    "Phase 13 M26 upstream report contains unsafe runtime work",
                    reportName + " indicates work that is not allowed during playable beta closeout.",
                    null,
                    packId,
                    List.of(relativeReportPath(reportPath)),
                    "Keep M26 report-only: no downloads, user cache mutation, launcher mutation, save/config mutation, transforms, or registry mutation."
            ));
        }
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
                    "ECHO-NATIVE-PHASE13-M26-REPORT-MISSING",
                    EchoNativeIssueSeverity.ERROR,
                    "Phase 13 M26 required report missing",
                    "M26 requires " + reportName + ".",
                    null,
                    packId,
                    List.of(fixture.toString().replace('\\', '/')),
                    "Regenerate M25 gameplay hook evidence and tester package reports before M26."
            ));
            return Map.of();
        }
        return EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(reportPath)));
    }

    private static Map<String, Object> readLiveActivationMarker(
            Path fixture,
            String packId,
            List<EchoNativeDiagnostic> diagnostics
    ) throws IOException {
        Path marker = fixture.resolve("isolated-runtime/game/echo-native/module-activation.json");
        if (!Files.isRegularFile(marker)) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-LIVE-ACTIVATION-MARKER-MISSING",
                    EchoNativeIssueSeverity.ERROR,
                    "Live native activation marker is missing",
                    "Playable beta requires live runtime evidence from module-activation.json, not only generated planning reports.",
                    null,
                    packId,
                    List.of(relativeReportPath(marker)),
                    "Launch the native loader after implementing AdapterCore runtime bridge visibility and rerun M26."
            ));
            return Map.of();
        }
        return EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(marker)));
    }

    private static Map<String, Object> base(String phase, List<EchoNativeDiagnostic> diagnostics) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("bytecodeMutated", false);
        data.put("cacheMutated", false);
        data.put("classloaderCreated", false);
        data.put("commandExecuted", false);
        data.put("configsMutated", false);
        data.put("diagnosticCount", diagnostics.size());
        data.put("diagnosticsCaptured", true);
        data.put("downloadAllowed", false);
        data.put("downloadsStarted", false);
        data.put("filesystemMutated", false);
        data.put("gameClassesResolved", false);
        data.put("generatedEvidenceAt", Instant.EPOCH.toString());
        data.put("jarsMutated", false);
        data.put("launcherInstallsMutated", false);
        data.put("libraryDownloadStarted", false);
        data.put("nativeExtractionStarted", false);
        data.put("phase", phase);
        data.put("processLaunched", false);
        data.put("publicPlaytestOpen", false);
        data.put("registryInjected", false);
        data.put("registryMutated", false);
        data.put("savesMutated", false);
        data.put("serviceCodeExecuted", false);
        data.put("transformsEnabled", false);
        data.put("transformsPerformed", false);
        data.put("unsafeRuntimeWorkStarted", false);
        data.put("userCachesMutated", false);
        return data;
    }

    private static Map<String, Object> data(Map<String, Object> report) {
        return EchoNativeJson.asObject(report == null ? null : report.get("data"));
    }

    private static int intValue(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        return 0;
    }

    private static int firstIntValue(Map<String, Object> data, String... keys) {
        for (String key : keys) {
            if (data.containsKey(key)) {
                return intValue(data, key);
            }
        }
        return 0;
    }

    private static Object firstPresent(Map<String, Object> data, String... keys) {
        for (String key : keys) {
            if (data.containsKey(key)) {
                return data.get(key);
            }
        }
        return null;
    }

    private static Long asLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String string && !string.isBlank()) {
            try {
                return Long.parseLong(string);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private static List<Map<String, Object>> objectList(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        List<Map<String, Object>> objects = new ArrayList<>();
        for (Object item : list) {
            objects.add(EchoNativeJson.asObject(item));
        }
        return objects;
    }

    private static boolean listEmpty(Object value) {
        return value instanceof List<?> list && list.isEmpty();
    }

    private static Set<String> mutatedLiveMutationSurfaces(Object value) {
        Set<String> surfaces = new TreeSet<>();
        for (Map<String, Object> record : objectList(value)) {
            if (!"MUTATED".equals(String.valueOf(record.getOrDefault("status", "")))) {
                continue;
            }
            String surface = String.valueOf(record.getOrDefault("surface", ""));
            if (!surface.isBlank()) {
                surfaces.add(surface);
            }
        }
        return surfaces;
    }

    private static boolean hasUnsafeRuntimeWork(Map<String, Object> data) {
        return Boolean.TRUE.equals(data.get("downloadsStarted"))
                || Boolean.TRUE.equals(data.get("downloadAllowed"))
                || Boolean.TRUE.equals(data.get("libraryDownloadStarted"))
                || Boolean.TRUE.equals(data.get("cacheMutated"))
                || Boolean.TRUE.equals(data.get("userCachesMutated"))
                || Boolean.TRUE.equals(data.get("launcherInstallsMutated"))
                || Boolean.TRUE.equals(data.get("savesMutated"))
                || Boolean.TRUE.equals(data.get("configsMutated"))
                || Boolean.TRUE.equals(data.get("jarsMutated"))
                || Boolean.TRUE.equals(data.get("nativeExtractionStarted"))
                || Boolean.TRUE.equals(data.get("processLaunched"))
                || Boolean.TRUE.equals(data.get("commandExecuted"))
                || Boolean.TRUE.equals(data.get("classloaderCreated"))
                || Boolean.TRUE.equals(data.get("gameClassesResolved"))
                || Boolean.TRUE.equals(data.get("serviceCodeExecuted"))
                || Boolean.TRUE.equals(data.get("registryInjected"))
                || Boolean.TRUE.equals(data.get("registryMutated"))
                || Boolean.TRUE.equals(data.get("transformsEnabled"))
                || Boolean.TRUE.equals(data.get("transformsPerformed"))
                || Boolean.TRUE.equals(data.get("bytecodeMutated"))
                || Boolean.TRUE.equals(data.get("filesystemMutated"))
                || Boolean.TRUE.equals(data.get("unsafeRuntimeWorkStarted"));
    }

    private static boolean isBlocking(EchoNativeDiagnostic diagnostic) {
        return diagnostic.severity() == EchoNativeIssueSeverity.ERROR
                || diagnostic.severity() == EchoNativeIssueSeverity.FATAL;
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
