package dev.echo.nativeplatform.loader;

import dev.echo.nativeplatform.contracts.EchoNativeLoadStatus;
import dev.echo.nativeplatform.contracts.EchoNativeServiceRegistry;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class EchoNativeAgent5RuntimeTruthGateMain {
    private static final List<String> REQUIRED_AGENT5_LIVE_PROOF_SURFACES = List.of(
            "inventory",
            "player_state",
            "world_blocks",
            "world_state",
            "structures",
            "block_entities",
            "capabilities",
            "missions",
            "events",
            "packets_hud",
            "save_data",
            "hud",
            "client_tick",
            "render_layers",
            "screen_events",
            "keybinds",
            "resource_reloads",
            "save_hooks",
            "server_client_sync",
            "commands",
            "network_channels",
            "config_reloads",
            "lifecycle_phases"
    );

    private EchoNativeAgent5RuntimeTruthGateMain() {
    }

    public static void main(String[] args) throws IOException {
        Path reportPath = args.length > 0
                ? Path.of(args[0]).toAbsolutePath().normalize()
                : Path.of("build/agent5/runtime-truth-gate/agent5-runtime-truth-gate.json")
                .toAbsolutePath()
                .normalize();

        NativeLoaderRuntimeHost mirrorOnlyHost = new NativeLoaderRuntimeHost(new NativeLoaderRuntimeHostContext(
                "echoashfallprotocol",
                "echoadaptercore",
                new EchoNativeServiceRegistry()
        ));
        require(mirrorOnlyHost.grantItem("player:mirror", "echoashfallprotocol:drop_pod_beacon", 1)
                        == EchoNativeLoadStatus.MUTATED,
                "mirror-only host should still mutate native state for persistence.");
        Map<String, Object> mirrorReport = mirrorOnlyHost.runtimeHostReport();
        require(Boolean.TRUE.equals(mirrorReport.get("mirrorOnlyReleaseProof")),
                "mirror-only host must be marked as mirror-only release proof.");
        require(Boolean.FALSE.equals(mirrorReport.get("liveRuntimeReleaseProofSatisfied")),
                "mirror-only host must not satisfy live runtime release proof.");

        NativeLoaderLiveProofService projectedLiveProofService =
                new NativeLoaderLiveProofService("agent5Runtime", List.of("inventory"));
        Map<String, Object> projectedLedgerRecord = new LinkedHashMap<>();
        projectedLedgerRecord.put("surface", "inventory");
        projectedLedgerRecord.put("status", EchoNativeLoadStatus.MUTATED.name());
        projectedLedgerRecord.put("serviceId", "adaptercore.runtime");
        projectedLedgerRecord.put("resolvedModuleId", "echoashfallprotocol");
        projectedLedgerRecord.put("backendClass", "dev.echo.nativeplatform.loader.NativeLoaderAdapterCoreBackend");
        projectedLedgerRecord.put("runtimeLane", "native_loader_runtime");
        projectedLedgerRecord.put("runtimeHostRegistered", true);
        projectedLedgerRecord.put("adapterCoreCallEnteredNativeLoaderHost", true);
        projectedLedgerRecord.put("adapterCoreCallEnteredNativeLoaderBackend", true);
        projectedLedgerRecord.put("runtimeHostClass", "dev.echo.nativeplatform.loader.NativeLoaderRuntimeHost");
        projectedLedgerRecord.put("resolvedServiceClass", "dev.echo.nativeplatform.loader.NativeLoaderRuntimeHost");
        projectedLedgerRecord.put("liveMinecraftDelegateClass", "qa.LiveMinecraftRuntimeHost");
        projectedLedgerRecord.put("liveMinecraftDelegateId", "qa:live-minecraft-runtime");
        projectedLedgerRecord.put("liveRuntimeAccessed", true);
        projectedLedgerRecord.put("minecraftRuntimeAccessed", true);
        projectedLedgerRecord.put("liveRuntimeMutationSupported", true);
        projectedLedgerRecord.put("liveRuntimeReleaseProofSatisfied", true);
        projectedLedgerRecord.put("liveRuntimeSurfaceMutationSatisfied", true);
        Map<String, Object> projectedLiveProof = projectedLiveProofService.create(
                "",
                Map.of(
                        "executed", true,
                        "preservedExistingLiveEvidence", true,
                        "clientRuntimeAccessed", true,
                        "clientThreadScheduled", true,
                        "agent5Runtime", Map.of("mutationLedger", List.of(Map.copyOf(projectedLedgerRecord)))
                ),
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                new NativeLoaderLiveProofService.Config(
                        "adaptercore.runtime",
                        "echoashfallprotocol",
                        "dev.echo.nativeplatform.loader.NativeLoaderAdapterCoreBackend",
                        "native_loader_runtime",
                        "qa.LiveMinecraftRuntimeHost",
                        "qa:live-minecraft-runtime",
                        "",
                        "",
                        "hud"
                ),
                ignored -> false
        );
        require(((List<?>) projectedLiveProof.get("mutationLedgerLiveProofSurfaces")).isEmpty(),
                "live proof service must reject top-level projected ledger flags without concrete surface proof: "
                        + projectedLiveProof);
        require(Boolean.FALSE.equals(projectedLiveProof.get("nativeHostMutationClaimAllowed")),
                "live proof service must not allow native host mutation claims from projected flags: "
                        + projectedLiveProof);
        Map<String, Object> staleDispatchLedgerRecord = new LinkedHashMap<>(projectedLedgerRecord);
        staleDispatchLedgerRecord.put("adapterCoreSurfaceDispatchId", "agent5:inventory:current");
        staleDispatchLedgerRecord.put("surfaceLiveRuntimeProofEvidence", Map.of(
                "liveRuntimeDispatchId", "agent5:inventory:stale",
                "liveRuntimeSurface", "inventory",
                "liveRuntimeDispatchProofSatisfied", true,
                "liveRuntimeDispatchMinecraftAccessed", true,
                "liveRuntimeDispatchMutationSupported", true,
                "liveRuntimeDispatchLiveMutation", true
        ));
        Map<String, Object> staleDispatchLiveProof = projectedLiveProofService.create(
                "",
                Map.of(
                        "executed", true,
                        "preservedExistingLiveEvidence", true,
                        "clientRuntimeAccessed", true,
                        "clientThreadScheduled", true,
                        "agent5Runtime", Map.of("mutationLedger", List.of(Map.copyOf(staleDispatchLedgerRecord)))
                ),
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                new NativeLoaderLiveProofService.Config(
                        "adaptercore.runtime",
                        "echoashfallprotocol",
                        "dev.echo.nativeplatform.loader.NativeLoaderAdapterCoreBackend",
                        "native_loader_runtime",
                        "qa.LiveMinecraftRuntimeHost",
                        "qa:live-minecraft-runtime",
                        "",
                        "",
                        "hud"
                ),
                ignored -> false
        );
        require(((List<?>) staleDispatchLiveProof.get("mutationLedgerLiveProofSurfaces")).isEmpty(),
                "live proof service must reject stale dispatch proof whose id does not match the AdapterCore record: "
                        + staleDispatchLiveProof);
        require(Boolean.FALSE.equals(staleDispatchLiveProof.get("nativeHostMutationClaimAllowed")),
                "live proof service must not allow mutation claims from stale dispatch proof: "
                        + staleDispatchLiveProof);
        NativeLoaderProductPlayableRuntimeEvidence.Config liveReceiptConfig =
                new NativeLoaderProductPlayableRuntimeEvidence.Config(
                        NativeLoaderAdapterCoreBackend.SERVICE_ID,
                        "echoashfallprotocol",
                        NativeLoaderAdapterCoreBackend.class.getName(),
                        "Native Loader",
                        "com.knoxhack.echoashfallprotocol.event.NativeMinecraftEchoRuntimeHost",
                        "echoashfallprotocol:native_minecraft_runtime_host",
                        "",
                        "",
                        "hud"
                );
        Map<String, Object> projectedRuntimeResult = new LinkedHashMap<>();
        projectedRuntimeResult.put("hostInventoryMutated", true);
        projectedRuntimeResult.put("hostInventoryMutationEvidence", liveNativeLoaderBackendReceipt(
                "inventory",
                "grantItem",
                "native_client.grant_item.echoterminal:echo_terminal_remote",
                "EchoNativeRuntimeHost.PlayerInventory"));
        projectedRuntimeResult.put("hostWorldBlockMutated", true);
        projectedRuntimeResult.put("hostWorldBlockMutationEvidence", liveNativeLoaderBackendReceipt(
                "world_blocks",
                "placeBlock",
                "native_client.world_block.echoashfallprotocol:acid_mud.0.64.0",
                "EchoNativeRuntimeHost.WorldBlocks"));
        projectedRuntimeResult.put("saveDataWritten", true);
        projectedRuntimeResult.put("saveDataScope", "echoashfallprotocol");
        projectedRuntimeResult.put("saveDataKey", "native_loader.first_spawn");
        projectedRuntimeResult.put("saveDataWriteEvidence", liveNativeLoaderBackendReceipt(
                "save_data",
                "writeSaveData",
                "native_client.save_data.echoashfallprotocol.native_loader.first_spawn",
                "EchoNativeRuntimeHost.SaveData"));
        Map<String, Object> hudBackendReceipt = liveNativeLoaderBackendReceipt(
                "hud",
                "emitHud",
                "native_client.hud_notification.ashfall_live_proof",
                "EchoNativeRuntimeHost.Hud");
        List<Map<String, Object>> projectedRuntimeLedger =
                NativeLoaderProductPlayableRuntimeEvidence.mutationLedger(
                        projectedRuntimeResult,
                        hudBackendReceipt,
                        liveReceiptConfig);
        Set<String> projectedRuntimeSurfaces =
                NativeLoaderProductPlayableRuntimeEvidence.mutatedSurfaces(
                        projectedRuntimeLedger,
                        liveReceiptConfig);
        require(projectedRuntimeSurfaces.containsAll(List.of("inventory", "world_blocks", "save_data", "hud")),
                "product playable runtime evidence must trust a direct Native Loader backend receipt even when "
                        + "runtimeHostRegistered is stale false: " + projectedRuntimeLedger);
        Map<String, Object> projectedRuntimeLiveProof = new NativeLoaderLiveProofService(
                "agent5Runtime",
                List.of("inventory", "world_blocks", "save_data", "hud")
        ).create(
                "net.minecraft.client.main.Main",
                Map.of(
                        "executed", true,
                        "preservedExistingLiveEvidence", true,
                        "clientRuntimeAccessed", true,
                        "clientThreadScheduled", true,
                        "agent5Runtime", Map.of(
                                "mutationLedger", projectedRuntimeLedger,
                                "saveDataScope", "echoashfallprotocol",
                                "saveDataKey", "native_loader.first_spawn"
                        )
                ),
                Map.of(
                        "clientUiHostAttached", true,
                        "clientThreadAccepted", true,
                        "liveWindowHandlePresent", true,
                        "fallbackHostAttached", false,
                        "headlessUiHostAttached", false
                ),
                Map.of(),
                Map.of(
                        "applied", true,
                        "runtimeInitializedServiceCount", 1
                ),
                Map.of(
                        "echoashfallprotocol", Map.of("nativeAdapterCodeExecuted", true)
                ),
                new NativeLoaderLiveProofService.Config(
                        NativeLoaderAdapterCoreBackend.SERVICE_ID,
                        "echoashfallprotocol",
                        NativeLoaderAdapterCoreBackend.class.getName(),
                        "Native Loader",
                        "com.knoxhack.echoashfallprotocol.event.NativeMinecraftEchoRuntimeHost",
                        "echoashfallprotocol:native_minecraft_runtime_host",
                        "",
                        "",
                        "hud"
                ),
                ignored -> true
        );
        require(Boolean.TRUE.equals(projectedRuntimeLiveProof.get("complete")),
                "direct Native Loader backend receipts must satisfy live proof surfaces: "
                        + projectedRuntimeLiveProof);
        require(Boolean.TRUE.equals(projectedRuntimeLiveProof.get("requiredMutationSurfacesMutated")),
                "direct Native Loader backend receipts must satisfy required mutation surfaces: "
                        + projectedRuntimeLiveProof);
        Map<String, Object> projectedStructureStartResult = new LinkedHashMap<>(projectedRuntimeResult);
        projectedStructureStartResult.put("canonicalStartingStructureId", "echoashfallprotocol:drop_pod");
        projectedStructureStartResult.put("startingStructurePlaced", true);
        projectedStructureStartResult.put("hostWorldBlockMutated", false);
        projectedStructureStartResult.put("hostStructureMutated", true);
        projectedStructureStartResult.put("starterRegionMaterialized", false);
        projectedStructureStartResult.put("starterRegionSkipped", "canonical_structure_placement");
        projectedStructureStartResult.put("hostStructureMutationEvidence", liveNativeLoaderBackendReceipt(
                "structures",
                "placeStructure",
                "native_client.structure.echoashfallprotocol:drop_pod.0.64.0",
                "EchoNativeRuntimeHost.Structures"));
        List<Map<String, Object>> projectedStructureStartLedger =
                NativeLoaderProductPlayableRuntimeEvidence.mutationLedger(
                        projectedStructureStartResult,
                        hudBackendReceipt,
                        liveReceiptConfig);
        Set<String> projectedStructureStartSurfaces =
                NativeLoaderProductPlayableRuntimeEvidence.mutatedSurfaces(
                        projectedStructureStartLedger,
                        liveReceiptConfig);
        require(projectedStructureStartSurfaces.containsAll(List.of("inventory", "structures", "save_data", "hud"))
                        && !projectedStructureStartSurfaces.contains("world_blocks"),
                "canonical Ashfall starting structure must satisfy structures instead of world_blocks: "
                        + projectedStructureStartLedger);
        Map<String, Object> projectedStructureStartLiveProof = new NativeLoaderLiveProofService(
                "agent5Runtime",
                List.of("inventory", "structures", "save_data", "hud")
        ).create(
                "net.minecraft.client.main.Main",
                Map.of(
                        "executed", true,
                        "preservedExistingLiveEvidence", true,
                        "clientRuntimeAccessed", true,
                        "clientThreadScheduled", true,
                        "agent5Runtime", Map.of(
                                "mutationLedger", projectedStructureStartLedger,
                                "saveDataScope", "echoashfallprotocol",
                                "saveDataKey", "native_loader.first_spawn"
                        )
                ),
                Map.of(
                        "clientUiHostAttached", true,
                        "clientThreadAccepted", true,
                        "liveWindowHandlePresent", true,
                        "fallbackHostAttached", false,
                        "headlessUiHostAttached", false
                ),
                Map.of(),
                Map.of(
                        "applied", true,
                        "runtimeInitializedServiceCount", 1
                ),
                Map.of(
                        "echoashfallprotocol", Map.of("nativeAdapterCodeExecuted", true)
                ),
                new NativeLoaderLiveProofService.Config(
                        NativeLoaderAdapterCoreBackend.SERVICE_ID,
                        "echoashfallprotocol",
                        NativeLoaderAdapterCoreBackend.class.getName(),
                        "Native Loader",
                        "com.knoxhack.echoashfallprotocol.event.NativeMinecraftEchoRuntimeHost",
                        "echoashfallprotocol:native_minecraft_runtime_host",
                        "",
                        "",
                        "hud"
                ),
                ignored -> true
        );
        require(Boolean.TRUE.equals(projectedStructureStartLiveProof.get("requiredMutationSurfacesMutated")),
                "canonical Ashfall starting structure must satisfy required structures mutation surfaces: "
                        + projectedStructureStartLiveProof);

        NativeLoaderRuntimeHost feedbackOnlyHost = new NativeLoaderRuntimeHost(new NativeLoaderRuntimeHostContext(
                "echoashfallprotocol",
                "echoadaptercore",
                new EchoNativeServiceRegistry(),
                null,
                new NativeLoaderLiveRuntimeAttachment(
                        "agent5:feedback_only_live_runtime_truth_gate",
                        "minecraft_client_runtime",
                        "native_loader_feedback_only_runtime",
                        true,
                        false,
                        List.of("feedback"),
                        Map.of(
                                "releaseRuntimeTrusted", true,
                                "realMinecraftProcess", true,
                                "nativeRuntimeProcess", true,
                                "agent5TruthGate", true
                        )
                ),
                new LiveMinecraftBridge()
        ));
        require(feedbackOnlyHost.emitFeedback("agent5.truth_gate", "diagnostic-only")
                        == EchoNativeLoadStatus.MUTATED,
                "feedback-only bridge should still record diagnostic feedback.");
        Map<String, Object> feedbackOnlyReport = feedbackOnlyHost.runtimeHostReport();
        require(((List<?>) feedbackOnlyReport.get("mutatedSurfaces")).contains("feedback"),
                "feedback-only host must keep feedback visible as diagnostic mutation: " + feedbackOnlyReport);
        require(Boolean.FALSE.equals(feedbackOnlyReport.get("liveRuntimeSurfaceMutationCoverageSatisfied")),
                "feedback-only mutation must not satisfy release surface coverage: " + feedbackOnlyReport);
        require(Boolean.FALSE.equals(feedbackOnlyReport.get("liveRuntimeReleaseProofSatisfied")),
                "feedback-only live mutation must not satisfy runtime release proof: " + feedbackOnlyReport);
        EchoNativeServiceRegistry feedbackOnlyRegistry = new EchoNativeServiceRegistry();
        NativeLoaderServiceBridge feedbackOnlyServiceBridge = new NativeLoaderServiceBridge(feedbackOnlyRegistry);
        NativeLoaderMutationLedger feedbackOnlyLedger = new NativeLoaderMutationLedger();
        NativeLoaderAdapterCoreBackend feedbackOnlyBackend = new NativeLoaderAdapterCoreBackend(
                feedbackOnlyHost,
                feedbackOnlyServiceBridge,
                feedbackOnlyLedger
        );
        feedbackOnlyRegistry.register(
                "echoadaptercore",
                NativeLoaderAdapterCoreBackend.SERVICE_ID,
                feedbackOnlyBackend,
                List.of("feedback"),
                NativeLoaderAdapterCoreBackend.class.getName()
        );
        NativeLoaderMutationLedger.MutationRecord feedbackOnlyRecord =
                feedbackOnlyBackend.emitFeedback("agent5.truth_gate", "adaptercore-diagnostic-only");
        require(feedbackOnlyRecord.status() == EchoNativeLoadStatus.MUTATED,
                "AdapterCore feedback should still mutate diagnostic feedback state.");
        require(!feedbackOnlyRecord.liveRuntimeReleaseProofSatisfied(),
                "AdapterCore feedback must not be a release-proof ledger record: " + feedbackOnlyRecord.toReport());
        require(feedbackOnlyLedger.liveRuntimeProofRecordCount() == 0,
                "AdapterCore feedback-only ledger must not count release proof records.");

        NonMinecraftLiveBridge nonMinecraftBridge = new NonMinecraftLiveBridge();
        NativeLoaderRuntimeHost nonMinecraftLiveHost = new NativeLoaderRuntimeHost(new NativeLoaderRuntimeHostContext(
                "echoashfallprotocol",
                "echoadaptercore",
                new EchoNativeServiceRegistry(),
                null,
                new NativeLoaderLiveRuntimeAttachment(
                        "agent5:non_minecraft_live_runtime_truth_gate",
                        "headless_native_runtime",
                        "native_loader_live_non_minecraft_runtime",
                        false,
                        false,
                        List.of("inventory"),
                        Map.of(
                                "releaseRuntimeTrusted", true,
                                "realMinecraftProcess", false,
                                "nativeRuntimeProcess", true,
                                "agent5TruthGate", true
                        )
                ),
                nonMinecraftBridge
        ));
        require(nonMinecraftLiveHost.grantItem("player:non_minecraft", "echoashfallprotocol:drop_pod_beacon", 1)
                        == EchoNativeLoadStatus.MUTATED,
                "non-Minecraft live bridge should be able to mutate for negative proof coverage.");
        Map<String, Object> nonMinecraftReport = nonMinecraftLiveHost.runtimeHostReport();
        require(Boolean.FALSE.equals(nonMinecraftReport.get("minecraftRuntimeAccessed")),
                "non-Minecraft live bridge must report no Minecraft runtime access.");
        require(Boolean.FALSE.equals(nonMinecraftReport.get("liveRuntimeReleaseProofSatisfied")),
                "live mutation without Minecraft runtime access must not satisfy release proof.");
        NativeLoaderCommandHost nonMinecraftCommandHost = new NativeLoaderCommandHost(nonMinecraftBridge);
        NativeLoaderNetworkHost nonMinecraftNetworkHost = new NativeLoaderNetworkHost(nonMinecraftBridge);
        NativeLoaderConfigHost nonMinecraftConfigHost = new NativeLoaderConfigHost(nonMinecraftBridge);
        NativeLoaderLifecycleEventHost nonMinecraftLifecycleHost = new NativeLoaderLifecycleEventHost(nonMinecraftBridge);
        require(nonMinecraftCommandHost.registerDeclaredCommand(
                        "echoashfallprotocol",
                        "ashfall.non_minecraft_command",
                        "commands",
                        "adaptercore.native_command",
                        Map.of("source", "agent5_truth_gate")) == EchoNativeLoadStatus.MUTATED,
                "non-Minecraft command host should mutate for negative proof coverage.");
        require(nonMinecraftNetworkHost.registerDeclaredPacket(
                        "echoashfallprotocol",
                        "ashfall:non_minecraft_packet",
                        "network_channels",
                        "adaptercore.native_runtime_packet",
                        List.of("terminal"),
                        Map.of("source", "agent5_truth_gate")) == EchoNativeLoadStatus.MUTATED,
                "non-Minecraft network host should mutate for negative proof coverage.");
        require(nonMinecraftConfigHost.registerConfig(
                        "echoashfallprotocol",
                        "ashfall-non-minecraft-config",
                        "server.config",
                        Map.of("source", "agent5_truth_gate")) == EchoNativeLoadStatus.MUTATED,
                "non-Minecraft config host should mutate for negative proof coverage.");
        nonMinecraftLifecycleHost.recordDeclaredLifecyclePhase(
                "echoashfallprotocol",
                "non_minecraft_phase",
                Map.of("source", "agent5_truth_gate"));
        nonMinecraftLifecycleHost.publish(
                "echoashfallprotocol",
                "ashfall.non_minecraft_event",
                Map.of("source", "agent5_truth_gate"),
                EchoNativeLoadStatus.MUTATED);
        Map<String, Object> nonMinecraftCommandReport = nonMinecraftCommandHost.toReport();
        Map<String, Object> nonMinecraftNetworkReport = nonMinecraftNetworkHost.toReport();
        Map<String, Object> nonMinecraftConfigReport = nonMinecraftConfigHost.toReport();
        Map<String, Object> nonMinecraftLifecycleReport = nonMinecraftLifecycleHost.toReport();
        for (Map<String, Object> subsystemReport : List.of(
                nonMinecraftCommandReport,
                nonMinecraftNetworkReport,
                nonMinecraftConfigReport,
                nonMinecraftLifecycleReport
        )) {
            require(Boolean.FALSE.equals(subsystemReport.get("minecraftRuntimeAccessed")),
                    "non-Minecraft subsystem report must not claim Minecraft runtime access: " + subsystemReport);
            require(Boolean.FALSE.equals(subsystemReport.get("liveRuntimeReleaseProofSatisfied")),
                    "non-Minecraft subsystem mutation must not satisfy release proof: " + subsystemReport);
        }

        LiveRuntimeInaccessibleSubsystemBridge inaccessibleSubsystemBridge = new LiveRuntimeInaccessibleSubsystemBridge();
        NativeLoaderCommandHost inaccessibleCommandHost = new NativeLoaderCommandHost(inaccessibleSubsystemBridge);
        NativeLoaderNetworkHost inaccessibleNetworkHost = new NativeLoaderNetworkHost(inaccessibleSubsystemBridge);
        NativeLoaderConfigHost inaccessibleConfigHost = new NativeLoaderConfigHost(inaccessibleSubsystemBridge);
        NativeLoaderLifecycleEventHost inaccessibleLifecycleHost = new NativeLoaderLifecycleEventHost(inaccessibleSubsystemBridge);
        require(inaccessibleCommandHost.registerDeclaredCommand(
                        "echoashfallprotocol",
                        "ashfall.inaccessible_live_command",
                        "commands",
                        "adaptercore.native_command",
                        Map.of("source", "agent5_truth_gate")) == EchoNativeLoadStatus.MUTATED,
                "live-runtime-inaccessible command declaration should still be queued.");
        require(inaccessibleNetworkHost.registerDeclaredPacket(
                        "echoashfallprotocol",
                        "ashfall:inaccessible_live_packet",
                        "network_channels",
                        "adaptercore.native_runtime_packet",
                        List.of("terminal"),
                        Map.of("source", "agent5_truth_gate")) == EchoNativeLoadStatus.MUTATED,
                "live-runtime-inaccessible network declaration should still be bound.");
        require(inaccessibleConfigHost.registerConfig(
                        "echoashfallprotocol",
                        "ashfall-inaccessible-live-config",
                        "server.config",
                        Map.of("source", "agent5_truth_gate")) == EchoNativeLoadStatus.MUTATED,
                "live-runtime-inaccessible config declaration should still be registered.");
        inaccessibleLifecycleHost.recordDeclaredLifecyclePhase(
                "echoashfallprotocol",
                "inaccessible_live_phase",
                Map.of("source", "agent5_truth_gate"));
        inaccessibleLifecycleHost.publish(
                "echoashfallprotocol",
                "ashfall.inaccessible_live_event",
                Map.of("source", "agent5_truth_gate"),
                EchoNativeLoadStatus.MUTATED);
        for (Map<String, Object> subsystemReport : List.of(
                inaccessibleCommandHost.toReport(),
                inaccessibleNetworkHost.toReport(),
                inaccessibleConfigHost.toReport(),
                inaccessibleLifecycleHost.toReport()
        )) {
            require(Boolean.FALSE.equals(subsystemReport.get("liveRuntimeAccessed")),
                    "live-runtime-inaccessible subsystem report must expose missing live runtime access: " + subsystemReport);
            require(number(subsystemReport.get("liveRuntimeMutationCount")) == 0,
                    "subsystem live proof must not count otherwise-valid dispatch evidence when liveRuntimeAccessed=false: "
                            + subsystemReport);
            require(Boolean.FALSE.equals(subsystemReport.get("liveRuntimeMutationCoverageSatisfied")),
                    "subsystem live proof must fail coverage when liveRuntimeAccessed=false: " + subsystemReport);
            require(Boolean.FALSE.equals(subsystemReport.get("liveRuntimeReleaseProofSatisfied")),
                    "subsystem live proof must fail release proof when liveRuntimeAccessed=false: " + subsystemReport);
        }

        UnstampedMinecraftSubsystemBridge unstampedSubsystemBridge = new UnstampedMinecraftSubsystemBridge();
        NativeLoaderCommandHost unstampedCommandHost = new NativeLoaderCommandHost(unstampedSubsystemBridge);
        NativeLoaderNetworkHost unstampedNetworkHost = new NativeLoaderNetworkHost(unstampedSubsystemBridge);
        NativeLoaderConfigHost unstampedConfigHost = new NativeLoaderConfigHost(unstampedSubsystemBridge);
        NativeLoaderLifecycleEventHost unstampedLifecycleHost = new NativeLoaderLifecycleEventHost(unstampedSubsystemBridge);
        require(unstampedCommandHost.registerDeclaredCommand(
                        "echoashfallprotocol",
                        "ashfall.unstamped_command",
                        "commands",
                        "adaptercore.native_command",
                        Map.of(
                                "source", "agent5_truth_gate",
                                "liveRuntimeDispatchProofSatisfied", true,
                                "liveRuntimeDispatchMinecraftAccessed", true,
                                "liveRuntimeDispatchMutationSupported", true,
                                "liveRuntimeDispatchLiveMutation", true)) == EchoNativeLoadStatus.MUTATED,
                "unstamped Minecraft command bridge should still queue native command mutation.");
        require(unstampedNetworkHost.registerDeclaredPacket(
                        "echoashfallprotocol",
                        "ashfall:unstamped_packet",
                        "network_channels",
                        "adaptercore.native_runtime_packet",
                        List.of("terminal"),
                        Map.of("source", "agent5_truth_gate")) == EchoNativeLoadStatus.MUTATED,
                "unstamped Minecraft network bridge should still bind native packet mutation.");
        require(unstampedConfigHost.registerConfig(
                        "echoashfallprotocol",
                        "ashfall-unstamped-config",
                        "server.config",
                        Map.of("source", "agent5_truth_gate")) == EchoNativeLoadStatus.MUTATED,
                "unstamped Minecraft config bridge should still register native config mutation.");
        unstampedLifecycleHost.recordDeclaredLifecyclePhase(
                "echoashfallprotocol",
                "unstamped_phase",
                Map.of("source", "agent5_truth_gate"));
        unstampedLifecycleHost.publish(
                "echoashfallprotocol",
                "ashfall.unstamped_event",
                Map.of("source", "agent5_truth_gate"),
                EchoNativeLoadStatus.MUTATED);
        for (Map<String, Object> subsystemReport : List.of(
                unstampedCommandHost.toReport(),
                unstampedNetworkHost.toReport(),
                unstampedConfigHost.toReport(),
                unstampedLifecycleHost.toReport()
        )) {
            require(number(subsystemReport.get("liveRuntimeMutationCount")) == 0,
                    "unstamped subsystem report must not count bare MUTATED returns as live proof: " + subsystemReport);
            require(Boolean.FALSE.equals(subsystemReport.get("liveMinecraftMutation")),
                    "unstamped subsystem report must not infer live Minecraft mutation from bridge globals: " + subsystemReport);
            require(Boolean.FALSE.equals(subsystemReport.get("liveRuntimeReleaseProofSatisfied")),
                    "unstamped subsystem report must not satisfy release proof: " + subsystemReport);
        }

        StaleSubsystemDispatchBridge staleSubsystemBridge = new StaleSubsystemDispatchBridge();
        NativeLoaderCommandHost staleSubsystemCommandHost = new NativeLoaderCommandHost(staleSubsystemBridge);
        NativeLoaderNetworkHost staleSubsystemNetworkHost = new NativeLoaderNetworkHost(staleSubsystemBridge);
        NativeLoaderConfigHost staleSubsystemConfigHost = new NativeLoaderConfigHost(staleSubsystemBridge);
        NativeLoaderLifecycleEventHost staleSubsystemLifecycleHost = new NativeLoaderLifecycleEventHost(staleSubsystemBridge);
        require(staleSubsystemCommandHost.registerDeclaredCommand(
                        "echoashfallprotocol",
                        "ashfall.stale.command",
                        "commands",
                        "adaptercore.native_command",
                        Map.of("source", "agent5_truth_gate")) == EchoNativeLoadStatus.MUTATED,
                "stale subsystem command declaration should still be queued.");
        require(staleSubsystemNetworkHost.registerDeclaredPacket(
                        "echoashfallprotocol",
                        "ashfall:stale_packet",
                        "network_channels",
                        "adaptercore.native_runtime_packet",
                        List.of("terminal"),
                        Map.of("source", "agent5_truth_gate")) == EchoNativeLoadStatus.MUTATED,
                "stale subsystem network declaration should still be bound.");
        require(staleSubsystemConfigHost.registerConfig(
                        "echoashfallprotocol",
                        "ashfall-stale-config",
                        "server.config",
                        Map.of("source", "agent5_truth_gate")) == EchoNativeLoadStatus.MUTATED,
                "stale subsystem config declaration should still be registered.");
        staleSubsystemLifecycleHost.recordDeclaredLifecyclePhase(
                "echoashfallprotocol",
                "stale_phase",
                Map.of("source", "agent5_truth_gate"));
        staleSubsystemLifecycleHost.publish(
                "echoashfallprotocol",
                "ashfall.stale_event",
                Map.of("source", "agent5_truth_gate"),
                EchoNativeLoadStatus.MUTATED);
        for (Map<String, Object> subsystemReport : List.of(
                staleSubsystemCommandHost.toReport(),
                staleSubsystemNetworkHost.toReport(),
                staleSubsystemConfigHost.toReport(),
                staleSubsystemLifecycleHost.toReport()
        )) {
            require(number(subsystemReport.get("liveRuntimeMutationCount")) == 0,
                    "stale subsystem dispatch ids must not count as live proof: " + subsystemReport);
            require(Boolean.FALSE.equals(subsystemReport.get("liveRuntimeMutationCoverageSatisfied")),
                    "stale subsystem dispatch ids must fail coverage: " + subsystemReport);
            require(Boolean.FALSE.equals(subsystemReport.get("liveRuntimeReleaseProofSatisfied")),
                    "stale subsystem dispatch ids must fail release proof: " + subsystemReport);
        }

        MissingSurfaceSubsystemDispatchBridge missingSurfaceSubsystemBridge = new MissingSurfaceSubsystemDispatchBridge();
        NativeLoaderCommandHost missingSurfaceSubsystemCommandHost = new NativeLoaderCommandHost(missingSurfaceSubsystemBridge);
        NativeLoaderNetworkHost missingSurfaceSubsystemNetworkHost = new NativeLoaderNetworkHost(missingSurfaceSubsystemBridge);
        NativeLoaderConfigHost missingSurfaceSubsystemConfigHost = new NativeLoaderConfigHost(missingSurfaceSubsystemBridge);
        NativeLoaderLifecycleEventHost missingSurfaceSubsystemLifecycleHost = new NativeLoaderLifecycleEventHost(missingSurfaceSubsystemBridge);
        require(missingSurfaceSubsystemCommandHost.registerDeclaredCommand(
                        "echoashfallprotocol",
                        "ashfall.missing_surface.command",
                        "commands",
                        "adaptercore.native_command",
                        Map.of("source", "agent5_truth_gate")) == EchoNativeLoadStatus.MUTATED,
                "missing-surface subsystem command declaration should still be queued.");
        require(missingSurfaceSubsystemNetworkHost.registerDeclaredPacket(
                        "echoashfallprotocol",
                        "ashfall:missing_surface_packet",
                        "network_channels",
                        "adaptercore.native_runtime_packet",
                        List.of("terminal"),
                        Map.of("source", "agent5_truth_gate")) == EchoNativeLoadStatus.MUTATED,
                "missing-surface subsystem network declaration should still be bound.");
        require(missingSurfaceSubsystemConfigHost.registerConfig(
                        "echoashfallprotocol",
                        "ashfall-missing-surface-config",
                        "server.config",
                        Map.of("source", "agent5_truth_gate")) == EchoNativeLoadStatus.MUTATED,
                "missing-surface subsystem config declaration should still be registered.");
        missingSurfaceSubsystemLifecycleHost.recordDeclaredLifecyclePhase(
                "echoashfallprotocol",
                "missing_surface_phase",
                Map.of("source", "agent5_truth_gate"));
        missingSurfaceSubsystemLifecycleHost.publish(
                "echoashfallprotocol",
                "ashfall.missing_surface_event",
                Map.of("source", "agent5_truth_gate"),
                EchoNativeLoadStatus.MUTATED);
        for (Map<String, Object> subsystemReport : List.of(
                missingSurfaceSubsystemCommandHost.toReport(),
                missingSurfaceSubsystemNetworkHost.toReport(),
                missingSurfaceSubsystemConfigHost.toReport(),
                missingSurfaceSubsystemLifecycleHost.toReport()
        )) {
            require(number(subsystemReport.get("liveRuntimeMutationCount")) == 0,
                    "missing subsystem liveRuntimeSurface evidence must not count as live proof: " + subsystemReport);
            require(Boolean.FALSE.equals(subsystemReport.get("liveRuntimeMutationCoverageSatisfied")),
                    "missing subsystem liveRuntimeSurface evidence must fail coverage: " + subsystemReport);
            require(Boolean.FALSE.equals(subsystemReport.get("liveRuntimeReleaseProofSatisfied")),
                    "missing subsystem liveRuntimeSurface evidence must fail release proof: " + subsystemReport);
        }

        WrongSurfaceSubsystemDispatchBridge wrongSurfaceSubsystemBridge = new WrongSurfaceSubsystemDispatchBridge();
        NativeLoaderCommandHost wrongSurfaceSubsystemCommandHost = new NativeLoaderCommandHost(wrongSurfaceSubsystemBridge);
        NativeLoaderNetworkHost wrongSurfaceSubsystemNetworkHost = new NativeLoaderNetworkHost(wrongSurfaceSubsystemBridge);
        NativeLoaderConfigHost wrongSurfaceSubsystemConfigHost = new NativeLoaderConfigHost(wrongSurfaceSubsystemBridge);
        NativeLoaderLifecycleEventHost wrongSurfaceSubsystemLifecycleHost = new NativeLoaderLifecycleEventHost(wrongSurfaceSubsystemBridge);
        require(wrongSurfaceSubsystemCommandHost.registerDeclaredCommand(
                        "echoashfallprotocol",
                        "ashfall.wrong_surface.command",
                        "commands",
                        "adaptercore.native_command",
                        Map.of("source", "agent5_truth_gate")) == EchoNativeLoadStatus.MUTATED,
                "wrong-surface subsystem command declaration should still be queued.");
        require(wrongSurfaceSubsystemNetworkHost.registerDeclaredPacket(
                        "echoashfallprotocol",
                        "ashfall:wrong_surface_packet",
                        "network_channels",
                        "adaptercore.native_runtime_packet",
                        List.of("terminal"),
                        Map.of("source", "agent5_truth_gate")) == EchoNativeLoadStatus.MUTATED,
                "wrong-surface subsystem network declaration should still be bound.");
        require(wrongSurfaceSubsystemConfigHost.registerConfig(
                        "echoashfallprotocol",
                        "ashfall-wrong-surface-config",
                        "server.config",
                        Map.of("source", "agent5_truth_gate")) == EchoNativeLoadStatus.MUTATED,
                "wrong-surface subsystem config declaration should still be registered.");
        wrongSurfaceSubsystemLifecycleHost.recordDeclaredLifecyclePhase(
                "echoashfallprotocol",
                "wrong_surface_phase",
                Map.of("source", "agent5_truth_gate"));
        wrongSurfaceSubsystemLifecycleHost.publish(
                "echoashfallprotocol",
                "ashfall.wrong_surface_event",
                Map.of("source", "agent5_truth_gate"),
                EchoNativeLoadStatus.MUTATED);
        for (Map<String, Object> subsystemReport : List.of(
                wrongSurfaceSubsystemCommandHost.toReport(),
                wrongSurfaceSubsystemNetworkHost.toReport(),
                wrongSurfaceSubsystemConfigHost.toReport(),
                wrongSurfaceSubsystemLifecycleHost.toReport()
        )) {
            require(number(subsystemReport.get("liveRuntimeMutationCount")) == 0,
                    "wrong subsystem liveRuntimeSurface evidence must not count as live proof: " + subsystemReport);
            require(Boolean.FALSE.equals(subsystemReport.get("liveRuntimeMutationCoverageSatisfied")),
                    "wrong subsystem liveRuntimeSurface evidence must fail coverage: " + subsystemReport);
            require(Boolean.FALSE.equals(subsystemReport.get("liveRuntimeReleaseProofSatisfied")),
                    "wrong subsystem liveRuntimeSurface evidence must fail release proof: " + subsystemReport);
        }

        MissingSubsystemSideEffectEvidenceBridge missingSubsystemSideEffectBridge =
                new MissingSubsystemSideEffectEvidenceBridge();
        NativeLoaderCommandHost missingSubsystemSideEffectCommandHost =
                new NativeLoaderCommandHost(missingSubsystemSideEffectBridge);
        NativeLoaderNetworkHost missingSubsystemSideEffectNetworkHost =
                new NativeLoaderNetworkHost(missingSubsystemSideEffectBridge);
        NativeLoaderConfigHost missingSubsystemSideEffectConfigHost =
                new NativeLoaderConfigHost(missingSubsystemSideEffectBridge);
        NativeLoaderLifecycleEventHost missingSubsystemSideEffectLifecycleHost =
                new NativeLoaderLifecycleEventHost(missingSubsystemSideEffectBridge);
        require(missingSubsystemSideEffectCommandHost.registerDeclaredCommand(
                        "echoashfallprotocol",
                        "ashfall.missing_side_effect.command",
                        "commands",
                        "adaptercore.native_command",
                        Map.of("source", "agent5_truth_gate")) == EchoNativeLoadStatus.MUTATED,
                "missing-side-effect subsystem command declaration should still be queued.");
        require(missingSubsystemSideEffectNetworkHost.registerDeclaredPacket(
                        "echoashfallprotocol",
                        "ashfall:missing_side_effect_packet",
                        "network_channels",
                        "adaptercore.native_runtime_packet",
                        List.of("terminal"),
                        Map.of("source", "agent5_truth_gate")) == EchoNativeLoadStatus.MUTATED,
                "missing-side-effect subsystem network declaration should still be bound.");
        require(missingSubsystemSideEffectConfigHost.registerConfig(
                        "echoashfallprotocol",
                        "ashfall-missing-side-effect-config",
                        "server.config",
                        Map.of("source", "agent5_truth_gate")) == EchoNativeLoadStatus.MUTATED,
                "missing-side-effect subsystem config declaration should still be registered.");
        missingSubsystemSideEffectLifecycleHost.recordDeclaredLifecyclePhase(
                "echoashfallprotocol",
                "missing_side_effect_phase",
                Map.of("source", "agent5_truth_gate"));
        missingSubsystemSideEffectLifecycleHost.publish(
                "echoashfallprotocol",
                "ashfall.missing_side_effect_event",
                Map.of("source", "agent5_truth_gate"),
                EchoNativeLoadStatus.MUTATED);
        for (Map<String, Object> subsystemReport : List.of(
                missingSubsystemSideEffectCommandHost.toReport(),
                missingSubsystemSideEffectNetworkHost.toReport(),
                missingSubsystemSideEffectConfigHost.toReport(),
                missingSubsystemSideEffectLifecycleHost.toReport()
        )) {
            require(number(subsystemReport.get("liveRuntimeMutationCount")) == 0,
                    "subsystem proof without runtime side-effect evidence must not count as live proof: "
                            + subsystemReport);
            require(Boolean.FALSE.equals(subsystemReport.get("liveRuntimeMutationCoverageSatisfied")),
                    "subsystem proof without runtime side-effect evidence must fail coverage: " + subsystemReport);
            require(Boolean.FALSE.equals(subsystemReport.get("liveRuntimeReleaseProofSatisfied")),
                    "subsystem proof without runtime side-effect evidence must fail release proof: "
                            + subsystemReport);
        }

        PartialSubsystemMinecraftBridge partialSubsystemBridge = new PartialSubsystemMinecraftBridge();
        NativeLoaderCommandHost partialSubsystemCommandHost = new NativeLoaderCommandHost(partialSubsystemBridge);
        NativeLoaderNetworkHost partialSubsystemNetworkHost = new NativeLoaderNetworkHost(partialSubsystemBridge);
        NativeLoaderConfigHost partialSubsystemConfigHost = new NativeLoaderConfigHost(partialSubsystemBridge);
        NativeLoaderLifecycleEventHost partialSubsystemLifecycleHost = new NativeLoaderLifecycleEventHost(partialSubsystemBridge);
        require(partialSubsystemCommandHost.registerDeclaredCommand(
                        "echoashfallprotocol",
                        "ashfall.partial.live",
                        "commands",
                        "adaptercore.native_command",
                        Map.of("source", "agent5_truth_gate")) == EchoNativeLoadStatus.MUTATED,
                "partial subsystem command live declaration should be accepted.");
        require(partialSubsystemCommandHost.registerDeclaredCommand(
                        "echoashfallprotocol",
                        "ashfall.partial.fallback",
                        "commands",
                        "adaptercore.native_command",
                        Map.of("source", "agent5_truth_gate")) == EchoNativeLoadStatus.MUTATED,
                "partial subsystem command fallback declaration should still be queued.");
        require(partialSubsystemNetworkHost.registerDeclaredPacket(
                        "echoashfallprotocol",
                        "ashfall:partial_live",
                        "network_channels",
                        "adaptercore.native_runtime_packet",
                        List.of("terminal"),
                        Map.of("source", "agent5_truth_gate")) == EchoNativeLoadStatus.MUTATED,
                "partial subsystem packet live declaration should be accepted.");
        require(partialSubsystemNetworkHost.registerDeclaredPacket(
                        "echoashfallprotocol",
                        "ashfall:partial_fallback",
                        "network_channels",
                        "adaptercore.native_runtime_packet",
                        List.of("terminal"),
                        Map.of("source", "agent5_truth_gate")) == EchoNativeLoadStatus.MUTATED,
                "partial subsystem packet fallback declaration should still be bound.");
        require(partialSubsystemConfigHost.registerConfig(
                        "echoashfallprotocol",
                        "ashfall-partial-live",
                        "server.config",
                        Map.of("source", "agent5_truth_gate")) == EchoNativeLoadStatus.MUTATED,
                "partial subsystem config live declaration should be accepted.");
        require(partialSubsystemConfigHost.registerConfig(
                        "echoashfallprotocol",
                        "ashfall-partial-fallback",
                        "server.config",
                        Map.of("source", "agent5_truth_gate")) == EchoNativeLoadStatus.MUTATED,
                "partial subsystem config fallback declaration should still be registered.");
        partialSubsystemLifecycleHost.recordDeclaredLifecyclePhase(
                "echoashfallprotocol",
                "partial_live_phase",
                Map.of("source", "agent5_truth_gate"));
        partialSubsystemLifecycleHost.recordDeclaredLifecyclePhase(
                "echoashfallprotocol",
                "partial_fallback_phase",
                Map.of("source", "agent5_truth_gate"));
        partialSubsystemLifecycleHost.publish(
                "echoashfallprotocol",
                "ashfall.partial_live_event",
                Map.of("source", "agent5_truth_gate"),
                EchoNativeLoadStatus.MUTATED);
        partialSubsystemLifecycleHost.publish(
                "echoashfallprotocol",
                "ashfall.partial_fallback_event",
                Map.of("source", "agent5_truth_gate"),
                EchoNativeLoadStatus.MUTATED);
        Map<String, Object> partialSubsystemCommandReport = partialSubsystemCommandHost.toReport();
        Map<String, Object> partialSubsystemNetworkReport = partialSubsystemNetworkHost.toReport();
        Map<String, Object> partialSubsystemConfigReport = partialSubsystemConfigHost.toReport();
        Map<String, Object> partialSubsystemLifecycleReport = partialSubsystemLifecycleHost.toReport();
        for (Map<String, Object> subsystemReport : List.of(
                partialSubsystemCommandReport,
                partialSubsystemNetworkReport,
                partialSubsystemConfigReport,
                partialSubsystemLifecycleReport
        )) {
            require(Boolean.TRUE.equals(subsystemReport.get("minecraftRuntimeAccessed")),
                    "partial subsystem report must still be backed by Minecraft runtime access: " + subsystemReport);
            require(number(subsystemReport.get("liveRuntimeMutationCount")) > 0,
                    "partial subsystem report must include at least one live mutation for negative coverage: " + subsystemReport);
            require(Boolean.TRUE.equals(subsystemReport.get("partialLiveMinecraftMutation")),
                    "partial subsystem report must expose partial live Minecraft mutation as diagnostic evidence: " + subsystemReport);
            require(Boolean.FALSE.equals(subsystemReport.get("liveMinecraftMutation")),
                    "partial subsystem report must not claim release-grade live Minecraft mutation: " + subsystemReport);
            require(Boolean.FALSE.equals(subsystemReport.get("liveRuntimeMutationCoverageSatisfied")),
                    "partial subsystem report must reject incomplete live mutation coverage: " + subsystemReport);
            require(Boolean.FALSE.equals(subsystemReport.get("liveRuntimeReleaseProofSatisfied")),
                    "partial subsystem report must not satisfy release proof with incomplete live mutation coverage: " + subsystemReport);
        }

        PartialMinecraftBridge partialBridge = new PartialMinecraftBridge();
        EchoNativeServiceRegistry partialRegistry = new EchoNativeServiceRegistry();
        NativeLoaderRuntimeHost partialHost = new NativeLoaderRuntimeHost(new NativeLoaderRuntimeHostContext(
                "echoashfallprotocol",
                "echoadaptercore",
                partialRegistry,
                null,
                new NativeLoaderLiveRuntimeAttachment(
                        "agent5:partial_minecraft_live_runtime_truth_gate",
                        "minecraft_client_runtime",
                        "native_loader_partial_live_minecraft_runtime",
                        true,
                        false,
                        List.of("inventory", "resource_reloads"),
                        Map.of(
                                "releaseRuntimeTrusted", true,
                                "realMinecraftProcess", true,
                                "nativeRuntimeProcess", true,
                                "agent5TruthGate", true
                        )
                ),
                partialBridge
        ));
        NativeLoaderServiceBridge partialServiceBridge = new NativeLoaderServiceBridge(partialRegistry);
        NativeLoaderMutationLedger partialLedger = new NativeLoaderMutationLedger();
        NativeLoaderAdapterCoreBackend partialBackend = new NativeLoaderAdapterCoreBackend(
                partialHost,
                partialServiceBridge,
                partialLedger
        );
        partialRegistry.register(
                "echoadaptercore",
                NativeLoaderAdapterCoreBackend.SERVICE_ID,
                partialBackend,
                List.of("inventory", "resource_reloads"),
                NativeLoaderAdapterCoreBackend.class.getName()
        );
        require(partialBackend.grantItem("player:partial", "echoashfallprotocol:drop_pod_beacon", 1).status()
                        == EchoNativeLoadStatus.MUTATED,
                "partial Minecraft bridge should mutate inventory live.");
        NativeLoaderMutationLedger.MutationRecord partialResourceRecord = partialBackend.reloadResources(
                "echoashfallprotocol",
                "partial_mirror_reload",
                "data_pack",
                Map.of("source", "agent5_truth_gate"));
        require(partialResourceRecord.status() == EchoNativeLoadStatus.MUTATED,
                "partial Minecraft bridge should still allow mirror resource reload mutation.");
        require(!partialResourceRecord.liveRuntimeSurfaceMutationSatisfied(),
                "mirror fallback resource reload must not be counted as live surface proof after another surface mutated live.");
        require(partialLedger.liveRuntimeProofRecordCount() == 1,
                "partial ledger must count only the live-mutated inventory record as live proof.");
        Map<String, Object> partialRuntimeHostReport = partialHost.runtimeHostReport();
        require(Boolean.FALSE.equals(partialRuntimeHostReport.get("liveRuntimeSurfaceMutationCoverageSatisfied")),
                "partial runtime host must reject mixed live and mirror surface mutation coverage.");
        require(Boolean.FALSE.equals(partialRuntimeHostReport.get("liveRuntimeReleaseProofSatisfied")),
                "partial runtime host must not satisfy release proof when any mutated surface lacks live bridge mutation.");
        require(((List<?>) partialRuntimeHostReport.get("liveRuntimeUnbridgedMutatedSurfaces")).contains("resourceReloads"),
                "partial runtime host must name mirror-fallback resourceReloads as an unbridged mutated surface: "
                        + partialRuntimeHostReport);

        NativeLoaderRuntimeHost inaccessibleDirectHost = new NativeLoaderRuntimeHost(new NativeLoaderRuntimeHostContext(
                "echoashfallprotocol",
                "echoadaptercore",
                new EchoNativeServiceRegistry(),
                null,
                new NativeLoaderLiveRuntimeAttachment(
                        "agent5:live_runtime_inaccessible_direct_surface_runtime_truth_gate",
                        "minecraft_client_runtime",
                        "native_loader_live_runtime_inaccessible_direct_surface_runtime",
                        true,
                        false,
                        List.of("inventory"),
                        Map.of(
                                "releaseRuntimeTrusted", true,
                                "realMinecraftProcess", true,
                                "nativeRuntimeProcess", true,
                                "agent5TruthGate", true
                        )
                ),
                new LiveRuntimeInaccessibleDirectSurfaceBridge()
        ));
        require(inaccessibleDirectHost.grantItem(
                        "player:inaccessible_direct",
                        "echoashfallprotocol:drop_pod_beacon",
                        1)
                        == EchoNativeLoadStatus.MUTATED,
                "live-runtime-inaccessible direct bridge should still mutate native inventory state.");
        Map<String, Object> inaccessibleDirectReport = inaccessibleDirectHost.runtimeHostReport();
        require(Boolean.FALSE.equals(inaccessibleDirectReport.get("liveRuntimeAccessed")),
                "live-runtime-inaccessible direct runtime host must expose missing live runtime access: "
                        + inaccessibleDirectReport);
        require(Boolean.FALSE.equals(inaccessibleDirectReport.get("liveRuntimeSurfaceMutationCoverageSatisfied")),
                "direct runtime proof must not count otherwise-valid bridge evidence when liveRuntimeAccessed=false: "
                        + inaccessibleDirectReport);
        require(Boolean.FALSE.equals(inaccessibleDirectReport.get("liveRuntimeReleaseProofSatisfied")),
                "direct runtime release proof must fail when liveRuntimeAccessed=false: " + inaccessibleDirectReport);
        require(((List<?>) inaccessibleDirectReport.get("liveRuntimeUnbridgedMutatedSurfaces")).contains("inventory"),
                "live-runtime-inaccessible direct runtime host must name inventory as unbridged: "
                        + inaccessibleDirectReport);

        NativeLoaderRuntimeHost unstampedDirectHost = new NativeLoaderRuntimeHost(new NativeLoaderRuntimeHostContext(
                "echoashfallprotocol",
                "echoadaptercore",
                new EchoNativeServiceRegistry(),
                null,
                new NativeLoaderLiveRuntimeAttachment(
                        "agent5:unstamped_direct_surface_runtime_truth_gate",
                        "minecraft_client_runtime",
                        "native_loader_unstamped_direct_surface_runtime",
                        true,
                        false,
                        List.of("inventory", "client_tick"),
                        Map.of(
                                "releaseRuntimeTrusted", true,
                                "realMinecraftProcess", true,
                                "nativeRuntimeProcess", true,
                                "agent5TruthGate", true
                        )
                ),
                new UnstampedDirectSurfaceMinecraftBridge()
        ));
        require(unstampedDirectHost.grantItem(
                        "player:unstamped_direct",
                        "echoashfallprotocol:drop_pod_beacon",
                        1)
                        == EchoNativeLoadStatus.MUTATED,
                "unstamped direct surface bridge should still mutate native inventory state.");
        require(unstampedDirectHost.clientTick(
                        "client",
                        Map.of(
                                "source", "agent5_truth_gate",
                                "liveRuntimeDispatchProofSatisfied", true,
                                "liveRuntimeDispatchMinecraftAccessed", true,
                                "liveRuntimeDispatchMutationSupported", true,
                                "liveRuntimeDispatchLiveMutation", true))
                        == EchoNativeLoadStatus.MUTATED,
                "unstamped direct surface bridge should still mutate native client tick state.");
        Map<String, Object> unstampedDirectReport = unstampedDirectHost.runtimeHostReport();
        require(Boolean.FALSE.equals(unstampedDirectReport.get("liveRuntimeSurfaceMutationCoverageSatisfied")),
                "unstamped direct runtime host must reject bare MUTATED direct surface proof: " + unstampedDirectReport);
        require(Boolean.FALSE.equals(unstampedDirectReport.get("liveRuntimeReleaseProofSatisfied")),
                "unstamped direct runtime host must not satisfy release proof: " + unstampedDirectReport);
        require(((List<?>) unstampedDirectReport.get("liveRuntimeUnbridgedMutatedSurfaces")).contains("clientTicks"),
                "unstamped direct runtime host must name clientTicks as unbridged without dispatch proof: "
                        + unstampedDirectReport);
        require(((List<?>) unstampedDirectReport.get("liveRuntimeUnbridgedMutatedSurfaces")).contains("inventory"),
                "unstamped direct runtime host must name inventory as unbridged without per-surface bridge evidence: "
                        + unstampedDirectReport);
        NativeLoaderRuntimeHost unstampedAdapterHost = new NativeLoaderRuntimeHost(new NativeLoaderRuntimeHostContext(
                "echoashfallprotocol",
                "echoadaptercore",
                new EchoNativeServiceRegistry(),
                null,
                new NativeLoaderLiveRuntimeAttachment(
                        "agent5:unstamped_adapter_surface_runtime_truth_gate",
                        "minecraft_client_runtime",
                        "native_loader_unstamped_adapter_surface_runtime",
                        true,
                        false,
                        List.of("inventory"),
                        Map.of(
                                "releaseRuntimeTrusted", true,
                                "realMinecraftProcess", true,
                                "nativeRuntimeProcess", true,
                                "agent5TruthGate", true
                        )
                ),
                new UnstampedDirectSurfaceMinecraftBridge()
        ));
        EchoNativeServiceRegistry unstampedAdapterRegistry = new EchoNativeServiceRegistry();
        NativeLoaderServiceBridge unstampedAdapterServiceBridge = new NativeLoaderServiceBridge(unstampedAdapterRegistry);
        NativeLoaderMutationLedger unstampedAdapterLedger = new NativeLoaderMutationLedger();
        NativeLoaderAdapterCoreBackend unstampedAdapterBackend = new NativeLoaderAdapterCoreBackend(
                unstampedAdapterHost,
                unstampedAdapterServiceBridge,
                unstampedAdapterLedger
        );
        unstampedAdapterRegistry.register(
                "echoadaptercore",
                NativeLoaderAdapterCoreBackend.SERVICE_ID,
                unstampedAdapterBackend,
                List.of("inventory"),
                NativeLoaderAdapterCoreBackend.class.getName()
        );
        NativeLoaderMutationLedger.MutationRecord unstampedAdapterRecord = unstampedAdapterBackend.grantItem(
                "player:unstamped_adapter",
                "echoashfallprotocol:drop_pod_beacon",
                1);
        require(unstampedAdapterRecord.status() == EchoNativeLoadStatus.MUTATED,
                "unstamped AdapterCore bridge should still mutate native inventory state.");
        require(!unstampedAdapterRecord.liveRuntimeSurfaceMutationSatisfied(),
                "AdapterCore backend must not promote bare direct MUTATED bridge status to live surface proof: "
                        + unstampedAdapterRecord.toReport());
        require(unstampedAdapterLedger.liveRuntimeProofRecordCount() == 0,
                "AdapterCore ledger must not count unstamped direct bridge status as live runtime proof.");

        NativeLoaderRuntimeHost staleDirectHost = new NativeLoaderRuntimeHost(new NativeLoaderRuntimeHostContext(
                "echoashfallprotocol",
                "echoadaptercore",
                new EchoNativeServiceRegistry(),
                null,
                new NativeLoaderLiveRuntimeAttachment(
                        "agent5:stale_direct_surface_evidence_runtime_truth_gate",
                        "minecraft_client_runtime",
                        "native_loader_stale_direct_surface_evidence_runtime",
                        true,
                        false,
                        List.of("inventory"),
                        Map.of(
                                "releaseRuntimeTrusted", true,
                                "realMinecraftProcess", true,
                                "nativeRuntimeProcess", true,
                                "agent5TruthGate", true
                        )
                ),
                new StaleDirectSurfaceEvidenceMinecraftBridge()
        ));
        require(staleDirectHost.grantItem(
                        "player:stale_direct",
                        "echoashfallprotocol:drop_pod_beacon",
                        1)
                        == EchoNativeLoadStatus.MUTATED,
                "stale direct evidence bridge should still mutate native inventory state.");
        Map<String, Object> staleDirectReport = staleDirectHost.runtimeHostReport();
        require(Boolean.FALSE.equals(staleDirectReport.get("liveRuntimeSurfaceMutationCoverageSatisfied")),
                "stale same-surface evidence must not satisfy direct runtime proof: " + staleDirectReport);
        require(Boolean.FALSE.equals(staleDirectReport.get("liveRuntimeReleaseProofSatisfied")),
                "stale same-surface evidence must not satisfy release proof: " + staleDirectReport);
        require(((List<?>) staleDirectReport.get("liveRuntimeUnbridgedMutatedSurfaces")).contains("inventory"),
                "stale direct runtime host must name inventory as unbridged when dispatch ids do not match: "
                        + staleDirectReport);

        NativeLoaderRuntimeHost wrongSurfaceDirectHost = new NativeLoaderRuntimeHost(new NativeLoaderRuntimeHostContext(
                "echoashfallprotocol",
                "echoadaptercore",
                new EchoNativeServiceRegistry(),
                null,
                new NativeLoaderLiveRuntimeAttachment(
                        "agent5:wrong_surface_direct_evidence_runtime_truth_gate",
                        "minecraft_client_runtime",
                        "native_loader_wrong_surface_direct_evidence_runtime",
                        true,
                        false,
                        List.of("inventory"),
                        Map.of(
                                "releaseRuntimeTrusted", true,
                                "realMinecraftProcess", true,
                                "nativeRuntimeProcess", true,
                                "agent5TruthGate", true
                        )
                ),
                new WrongSurfaceDirectEvidenceMinecraftBridge()
        ));
        require(wrongSurfaceDirectHost.grantItem(
                        "player:wrong_surface_direct",
                        "echoashfallprotocol:drop_pod_beacon",
                        1)
                        == EchoNativeLoadStatus.MUTATED,
                "wrong-surface direct evidence bridge should still mutate native inventory state.");
        Map<String, Object> wrongSurfaceDirectReport = wrongSurfaceDirectHost.runtimeHostReport();
        require(Boolean.FALSE.equals(wrongSurfaceDirectReport.get("liveRuntimeSurfaceMutationCoverageSatisfied")),
                "wrong-surface evidence must not satisfy direct runtime proof: " + wrongSurfaceDirectReport);
        require(Boolean.FALSE.equals(wrongSurfaceDirectReport.get("liveRuntimeReleaseProofSatisfied")),
                "wrong-surface evidence must not satisfy release proof: " + wrongSurfaceDirectReport);
        require(((List<?>) wrongSurfaceDirectReport.get("liveRuntimeUnbridgedMutatedSurfaces")).contains("inventory"),
                "wrong-surface direct runtime host must name inventory as unbridged when liveRuntimeSurface mismatches: "
                        + wrongSurfaceDirectReport);

        NativeLoaderRuntimeHost missingSurfaceDirectHost = new NativeLoaderRuntimeHost(new NativeLoaderRuntimeHostContext(
                "echoashfallprotocol",
                "echoadaptercore",
                new EchoNativeServiceRegistry(),
                null,
                new NativeLoaderLiveRuntimeAttachment(
                        "agent5:missing_surface_direct_evidence_runtime_truth_gate",
                        "minecraft_client_runtime",
                        "native_loader_missing_surface_direct_evidence_runtime",
                        true,
                        false,
                        List.of("inventory"),
                        Map.of(
                                "releaseRuntimeTrusted", true,
                                "realMinecraftProcess", true,
                                "nativeRuntimeProcess", true,
                                "agent5TruthGate", true
                        )
                ),
                new MissingSurfaceDirectEvidenceMinecraftBridge()
        ));
        require(missingSurfaceDirectHost.grantItem(
                        "player:missing_surface_direct",
                        "echoashfallprotocol:drop_pod_beacon",
                        1)
                        == EchoNativeLoadStatus.MUTATED,
                "missing-surface direct evidence bridge should still mutate native inventory state.");
        Map<String, Object> missingSurfaceDirectReport = missingSurfaceDirectHost.runtimeHostReport();
        require(Boolean.FALSE.equals(missingSurfaceDirectReport.get("liveRuntimeSurfaceMutationCoverageSatisfied")),
                "missing liveRuntimeSurface evidence must not satisfy direct runtime proof: " + missingSurfaceDirectReport);
        require(Boolean.FALSE.equals(missingSurfaceDirectReport.get("liveRuntimeReleaseProofSatisfied")),
                "missing liveRuntimeSurface evidence must not satisfy release proof: " + missingSurfaceDirectReport);
        require(((List<?>) missingSurfaceDirectReport.get("liveRuntimeUnbridgedMutatedSurfaces")).contains("inventory"),
                "missing-surface direct runtime host must name inventory as unbridged when liveRuntimeSurface is absent: "
                        + missingSurfaceDirectReport);

        NativeLoaderRuntimeHost missingInventoryEvidenceHost = new NativeLoaderRuntimeHost(new NativeLoaderRuntimeHostContext(
                "echoashfallprotocol",
                "echoadaptercore",
                new EchoNativeServiceRegistry(),
                null,
                new NativeLoaderLiveRuntimeAttachment(
                        "agent5:missing_inventory_evidence_runtime_truth_gate",
                        "minecraft_client_runtime",
                        "native_loader_missing_inventory_evidence_runtime",
                        true,
                        false,
                        List.of("inventory"),
                        Map.of(
                                "releaseRuntimeTrusted", true,
                                "realMinecraftProcess", true,
                                "nativeRuntimeProcess", true,
                                "agent5TruthGate", true
                        )
                ),
                new MissingInventoryEvidenceMinecraftBridge()
        ));
        require(missingInventoryEvidenceHost.grantItem(
                        "player:missing_inventory_evidence",
                        "echoashfallprotocol:drop_pod_beacon",
                        1)
                        == EchoNativeLoadStatus.MUTATED,
                "missing-inventory-evidence bridge should still mutate native inventory state.");
        Map<String, Object> missingInventoryEvidenceReport = missingInventoryEvidenceHost.runtimeHostReport();
        require(Boolean.FALSE.equals(missingInventoryEvidenceReport.get("liveRuntimeSurfaceMutationCoverageSatisfied")),
                "inventory proof must require explicit inventory touch/mutation evidence: "
                        + missingInventoryEvidenceReport);
        require(Boolean.FALSE.equals(missingInventoryEvidenceReport.get("liveRuntimeReleaseProofSatisfied")),
                "inventory without live inventory evidence must not satisfy release proof: "
                        + missingInventoryEvidenceReport);
        require(((List<?>) missingInventoryEvidenceReport.get("liveRuntimeUnbridgedMutatedSurfaces")).contains("inventory"),
                "missing-inventory-evidence runtime host must name inventory as unbridged: "
                        + missingInventoryEvidenceReport);

        NativeLoaderRuntimeHost missingPlayerStateEvidenceHost = new NativeLoaderRuntimeHost(new NativeLoaderRuntimeHostContext(
                "echoashfallprotocol",
                "echoadaptercore",
                new EchoNativeServiceRegistry(),
                null,
                new NativeLoaderLiveRuntimeAttachment(
                        "agent5:missing_player_state_evidence_runtime_truth_gate",
                        "minecraft_client_runtime",
                        "native_loader_missing_player_state_evidence_runtime",
                        true,
                        false,
                        List.of("player_state"),
                        Map.of(
                                "releaseRuntimeTrusted", true,
                                "realMinecraftProcess", true,
                                "nativeRuntimeProcess", true,
                                "agent5TruthGate", true
                        )
                ),
                new MissingPlayerStateEvidenceMinecraftBridge()
        ));
        require(missingPlayerStateEvidenceHost.updatePlayerState(
                        "player:missing_player_state_evidence",
                        "ashfall.first_spawn",
                        "drop_pod_linked")
                        == EchoNativeLoadStatus.MUTATED,
                "missing-player-state-evidence bridge should still mutate native player state.");
        Map<String, Object> missingPlayerStateEvidenceReport = missingPlayerStateEvidenceHost.runtimeHostReport();
        require(Boolean.FALSE.equals(missingPlayerStateEvidenceReport.get("liveRuntimeSurfaceMutationCoverageSatisfied")),
                "player_state proof must require explicit player-state touch/mutation evidence: "
                        + missingPlayerStateEvidenceReport);
        require(Boolean.FALSE.equals(missingPlayerStateEvidenceReport.get("liveRuntimeReleaseProofSatisfied")),
                "player_state without live player-state evidence must not satisfy release proof: "
                        + missingPlayerStateEvidenceReport);
        require(((List<?>) missingPlayerStateEvidenceReport.get("liveRuntimeUnbridgedMutatedSurfaces")).contains("playerState"),
                "missing-player-state-evidence runtime host must name playerState as unbridged: "
                        + missingPlayerStateEvidenceReport);

        NativeLoaderRuntimeHost missingMissionEvidenceHost = new NativeLoaderRuntimeHost(new NativeLoaderRuntimeHostContext(
                "echoashfallprotocol",
                "echoadaptercore",
                new EchoNativeServiceRegistry(),
                null,
                new NativeLoaderLiveRuntimeAttachment(
                        "agent5:missing_mission_evidence_runtime_truth_gate",
                        "minecraft_client_runtime",
                        "native_loader_missing_mission_evidence_runtime",
                        true,
                        false,
                        List.of("missions"),
                        Map.of(
                                "releaseRuntimeTrusted", true,
                                "realMinecraftProcess", true,
                                "nativeRuntimeProcess", true,
                                "agent5TruthGate", true
                        )
                ),
                new MissingMissionEvidenceMinecraftBridge()
        ));
        require(missingMissionEvidenceHost.updateMission("ashfall:first_spawn", "linked", "terminal")
                        == EchoNativeLoadStatus.MUTATED,
                "missing-mission-evidence bridge should still mutate native mission state.");
        Map<String, Object> missingMissionEvidenceReport = missingMissionEvidenceHost.runtimeHostReport();
        require(Boolean.FALSE.equals(missingMissionEvidenceReport.get("liveRuntimeSurfaceMutationCoverageSatisfied")),
                "missions proof must require explicit player-state and mission-state evidence: "
                        + missingMissionEvidenceReport);
        require(Boolean.FALSE.equals(missingMissionEvidenceReport.get("liveRuntimeReleaseProofSatisfied")),
                "missions without live mission evidence must not satisfy release proof: "
                        + missingMissionEvidenceReport);
        require(((List<?>) missingMissionEvidenceReport.get("liveRuntimeUnbridgedMutatedSurfaces")).contains("missions"),
                "missing-mission-evidence runtime host must name missions as unbridged: "
                        + missingMissionEvidenceReport);

        NativeLoaderRuntimeHost missingWorldBlockEvidenceHost = new NativeLoaderRuntimeHost(new NativeLoaderRuntimeHostContext(
                "echoashfallprotocol",
                "echoadaptercore",
                new EchoNativeServiceRegistry(),
                null,
                new NativeLoaderLiveRuntimeAttachment(
                        "agent5:missing_world_block_evidence_runtime_truth_gate",
                        "minecraft_client_runtime",
                        "native_loader_missing_world_block_evidence_runtime",
                        true,
                        false,
                        List.of("world_blocks"),
                        Map.of(
                                "releaseRuntimeTrusted", true,
                                "realMinecraftProcess", true,
                                "nativeRuntimeProcess", true,
                                "agent5TruthGate", true
                        )
                ),
                new MissingWorldBlockEvidenceMinecraftBridge()
        ));
        require(missingWorldBlockEvidenceHost.placeBlock(
                        "minecraft:overworld",
                        0,
                        64,
                        0,
                        "echoashfallprotocol:drop_pod_beacon")
                        == EchoNativeLoadStatus.MUTATED,
                "missing-world-block-evidence bridge should still mutate native world-block state.");
        Map<String, Object> missingWorldBlockEvidenceReport = missingWorldBlockEvidenceHost.runtimeHostReport();
        require(Boolean.FALSE.equals(missingWorldBlockEvidenceReport.get("liveRuntimeSurfaceMutationCoverageSatisfied")),
                "world_blocks proof must require explicit block-state touch/mutation evidence: "
                        + missingWorldBlockEvidenceReport);
        require(Boolean.FALSE.equals(missingWorldBlockEvidenceReport.get("liveRuntimeReleaseProofSatisfied")),
                "world_blocks without live block-state evidence must not satisfy release proof: "
                        + missingWorldBlockEvidenceReport);
        require(((List<?>) missingWorldBlockEvidenceReport.get("liveRuntimeUnbridgedMutatedSurfaces")).contains("worldBlocks"),
                "missing-world-block-evidence runtime host must name worldBlocks as unbridged: "
                        + missingWorldBlockEvidenceReport);

        NativeLoaderRuntimeHost missingWorldStateEvidenceHost = new NativeLoaderRuntimeHost(new NativeLoaderRuntimeHostContext(
                "echoashfallprotocol",
                "echoadaptercore",
                new EchoNativeServiceRegistry(),
                null,
                new NativeLoaderLiveRuntimeAttachment(
                        "agent5:missing_world_state_evidence_runtime_truth_gate",
                        "minecraft_client_runtime",
                        "native_loader_missing_world_state_evidence_runtime",
                        true,
                        false,
                        List.of("world_state"),
                        Map.of(
                                "releaseRuntimeTrusted", true,
                                "realMinecraftProcess", true,
                                "nativeRuntimeProcess", true,
                                "agent5TruthGate", true
                        )
                ),
                new MissingSaveDataEvidenceMinecraftBridge()
        ));
        require(missingWorldStateEvidenceHost.updateWorldState(
                        "minecraft:overworld",
                        "marker.first_spawn",
                        "drop_pod_linked")
                        == EchoNativeLoadStatus.MUTATED,
                "missing-world-state-evidence bridge should still mutate native world-state state.");
        Map<String, Object> missingWorldStateEvidenceReport = missingWorldStateEvidenceHost.runtimeHostReport();
        require(Boolean.FALSE.equals(missingWorldStateEvidenceReport.get("liveRuntimeSurfaceMutationCoverageSatisfied")),
                "world_state proof must require explicit world-save evidence: "
                        + missingWorldStateEvidenceReport);
        require(Boolean.FALSE.equals(missingWorldStateEvidenceReport.get("liveRuntimeReleaseProofSatisfied")),
                "world_state without live world-save evidence must not satisfy release proof: "
                        + missingWorldStateEvidenceReport);
        require(((List<?>) missingWorldStateEvidenceReport.get("liveRuntimeUnbridgedMutatedSurfaces")).contains("worldState"),
                "missing-world-state-evidence runtime host must name worldState as unbridged: "
                        + missingWorldStateEvidenceReport);

        NativeLoaderRuntimeHost missingStructureEvidenceHost = new NativeLoaderRuntimeHost(new NativeLoaderRuntimeHostContext(
                "echoashfallprotocol",
                "echoadaptercore",
                new EchoNativeServiceRegistry(),
                null,
                new NativeLoaderLiveRuntimeAttachment(
                        "agent5:missing_structure_evidence_runtime_truth_gate",
                        "minecraft_client_runtime",
                        "native_loader_missing_structure_evidence_runtime",
                        true,
                        false,
                        List.of("structures"),
                        Map.of(
                                "releaseRuntimeTrusted", true,
                                "realMinecraftProcess", true,
                                "nativeRuntimeProcess", true,
                                "agent5TruthGate", true
                        )
                ),
                new MissingStructureEvidenceMinecraftBridge()
        ));
        require(missingStructureEvidenceHost.placeStructure(
                        "minecraft:overworld",
                        "echoashfallprotocol:drop_pod",
                        0,
                        64,
                        0)
                        == EchoNativeLoadStatus.MUTATED,
                "missing-structure-evidence bridge should still mutate native structure state.");
        Map<String, Object> missingStructureEvidenceReport = missingStructureEvidenceHost.runtimeHostReport();
        require(Boolean.FALSE.equals(missingStructureEvidenceReport.get("liveRuntimeSurfaceMutationCoverageSatisfied")),
                "structures proof must require explicit structure placement and world-save evidence: "
                        + missingStructureEvidenceReport);
        require(Boolean.FALSE.equals(missingStructureEvidenceReport.get("liveRuntimeReleaseProofSatisfied")),
                "structures without live structure/save evidence must not satisfy release proof: "
                        + missingStructureEvidenceReport);
        require(((List<?>) missingStructureEvidenceReport.get("liveRuntimeUnbridgedMutatedSurfaces")).contains("structures"),
                "missing-structure-evidence runtime host must name structures as unbridged: "
                        + missingStructureEvidenceReport);

        NativeLoaderRuntimeHost missingBlockEntityEvidenceHost = new NativeLoaderRuntimeHost(new NativeLoaderRuntimeHostContext(
                "echoashfallprotocol",
                "echoadaptercore",
                new EchoNativeServiceRegistry(),
                null,
                new NativeLoaderLiveRuntimeAttachment(
                        "agent5:missing_block_entity_evidence_runtime_truth_gate",
                        "minecraft_client_runtime",
                        "native_loader_missing_block_entity_evidence_runtime",
                        true,
                        false,
                        List.of("block_entities"),
                        Map.of(
                                "releaseRuntimeTrusted", true,
                                "realMinecraftProcess", true,
                                "nativeRuntimeProcess", true,
                                "agent5TruthGate", true
                        )
                ),
                new MissingBlockEntityEvidenceMinecraftBridge()
        ));
        require(missingBlockEntityEvidenceHost.updateBlockEntity(
                        "minecraft:overworld",
                        0,
                        64,
                        0,
                        "power",
                        "online")
                        == EchoNativeLoadStatus.MUTATED,
                "missing-block-entity-evidence bridge should still mutate native block-entity state.");
        Map<String, Object> missingBlockEntityEvidenceReport = missingBlockEntityEvidenceHost.runtimeHostReport();
        require(Boolean.FALSE.equals(missingBlockEntityEvidenceReport.get("liveRuntimeSurfaceMutationCoverageSatisfied")),
                "block_entities proof must require explicit block-entity touch/mutation evidence: "
                        + missingBlockEntityEvidenceReport);
        require(Boolean.FALSE.equals(missingBlockEntityEvidenceReport.get("liveRuntimeReleaseProofSatisfied")),
                "block_entities without live block-entity evidence must not satisfy release proof: "
                        + missingBlockEntityEvidenceReport);
        require(((List<?>) missingBlockEntityEvidenceReport.get("liveRuntimeUnbridgedMutatedSurfaces")).contains("blockEntities"),
                "missing-block-entity-evidence runtime host must name blockEntities as unbridged: "
                        + missingBlockEntityEvidenceReport);

        NativeLoaderRuntimeHost missingCapabilityEvidenceHost = new NativeLoaderRuntimeHost(new NativeLoaderRuntimeHostContext(
                "echoashfallprotocol",
                "echoadaptercore",
                new EchoNativeServiceRegistry(),
                null,
                new NativeLoaderLiveRuntimeAttachment(
                        "agent5:missing_capability_evidence_runtime_truth_gate",
                        "minecraft_client_runtime",
                        "native_loader_missing_capability_evidence_runtime",
                        true,
                        false,
                        List.of("capabilities"),
                        Map.of(
                                "releaseRuntimeTrusted", true,
                                "realMinecraftProcess", true,
                                "nativeRuntimeProcess", true,
                                "agent5TruthGate", true
                        )
                ),
                new MissingCapabilityEvidenceMinecraftBridge()
        ));
        require(missingCapabilityEvidenceHost.updateCapability(
                        "minecraft:overworld/0,64,0",
                        "receive_energy",
                        "25")
                        == EchoNativeLoadStatus.MUTATED,
                "missing-capability-evidence bridge should still mutate native capability state.");
        Map<String, Object> missingCapabilityEvidenceReport = missingCapabilityEvidenceHost.runtimeHostReport();
        require(Boolean.FALSE.equals(missingCapabilityEvidenceReport.get("liveRuntimeSurfaceMutationCoverageSatisfied")),
                "capabilities proof must require explicit capability touch/mutation evidence: "
                        + missingCapabilityEvidenceReport);
        require(Boolean.FALSE.equals(missingCapabilityEvidenceReport.get("liveRuntimeReleaseProofSatisfied")),
                "capabilities without live capability evidence must not satisfy release proof: "
                        + missingCapabilityEvidenceReport);
        require(((List<?>) missingCapabilityEvidenceReport.get("liveRuntimeUnbridgedMutatedSurfaces")).contains("capabilities"),
                "missing-capability-evidence runtime host must name capabilities as unbridged: "
                        + missingCapabilityEvidenceReport);

        NativeLoaderRuntimeHost missingSaveDataEvidenceHost = new NativeLoaderRuntimeHost(new NativeLoaderRuntimeHostContext(
                "echoashfallprotocol",
                "echoadaptercore",
                new EchoNativeServiceRegistry(),
                null,
                new NativeLoaderLiveRuntimeAttachment(
                        "agent5:missing_save_data_evidence_runtime_truth_gate",
                        "minecraft_client_runtime",
                        "native_loader_missing_save_data_evidence_runtime",
                        true,
                        false,
                        List.of("save_data"),
                        Map.of(
                                "releaseRuntimeTrusted", true,
                                "realMinecraftProcess", true,
                                "nativeRuntimeProcess", true,
                                "agent5TruthGate", true
                        )
                ),
                new MissingSaveDataEvidenceMinecraftBridge()
        ));
        require(missingSaveDataEvidenceHost.writeSaveData("ashfall:first_spawn", "complete")
                        == EchoNativeLoadStatus.MUTATED,
                "missing-save-data-evidence bridge should still mutate native save-data state.");
        Map<String, Object> missingSaveDataEvidenceReport = missingSaveDataEvidenceHost.runtimeHostReport();
        require(Boolean.FALSE.equals(missingSaveDataEvidenceReport.get("liveRuntimeSurfaceMutationCoverageSatisfied")),
                "save_data proof must require explicit world-save evidence: " + missingSaveDataEvidenceReport);
        require(Boolean.FALSE.equals(missingSaveDataEvidenceReport.get("liveRuntimeReleaseProofSatisfied")),
                "save_data without live world-save evidence must not satisfy release proof: "
                        + missingSaveDataEvidenceReport);
        require(((List<?>) missingSaveDataEvidenceReport.get("liveRuntimeUnbridgedMutatedSurfaces")).contains("saveData"),
                "missing-save-data-evidence runtime host must name saveData as unbridged: "
                        + missingSaveDataEvidenceReport);

        NativeLoaderRuntimeHost missingSaveHookEvidenceHost = new NativeLoaderRuntimeHost(new NativeLoaderRuntimeHostContext(
                "echoashfallprotocol",
                "echoadaptercore",
                new EchoNativeServiceRegistry(),
                null,
                new NativeLoaderLiveRuntimeAttachment(
                        "agent5:missing_save_hook_evidence_runtime_truth_gate",
                        "minecraft_client_runtime",
                        "native_loader_missing_save_hook_evidence_runtime",
                        true,
                        false,
                        List.of("save_hooks"),
                        Map.of(
                                "releaseRuntimeTrusted", true,
                                "realMinecraftProcess", true,
                                "nativeRuntimeProcess", true,
                                "agent5TruthGate", true
                        )
                ),
                new MissingSaveDataEvidenceMinecraftBridge()
        ));
        require(missingSaveHookEvidenceHost.saveHook("ashfall:world_save", Map.of("phase", "after_save"))
                        == EchoNativeLoadStatus.MUTATED,
                "missing-save-hook-evidence bridge should still mutate native save-hook state.");
        Map<String, Object> missingSaveHookEvidenceReport = missingSaveHookEvidenceHost.runtimeHostReport();
        require(Boolean.FALSE.equals(missingSaveHookEvidenceReport.get("liveRuntimeSurfaceMutationCoverageSatisfied")),
                "save_hooks proof must require explicit world-save evidence: " + missingSaveHookEvidenceReport);
        require(Boolean.FALSE.equals(missingSaveHookEvidenceReport.get("liveRuntimeReleaseProofSatisfied")),
                "save_hooks without live world-save evidence must not satisfy release proof: "
                        + missingSaveHookEvidenceReport);
        require(((List<?>) missingSaveHookEvidenceReport.get("liveRuntimeUnbridgedMutatedSurfaces")).contains("saveHooks"),
                "missing-save-hook-evidence runtime host must name saveHooks as unbridged: "
                        + missingSaveHookEvidenceReport);

        NativeLoaderRuntimeHost missingResourceReloadEvidenceHost = new NativeLoaderRuntimeHost(new NativeLoaderRuntimeHostContext(
                "echoashfallprotocol",
                "echoadaptercore",
                new EchoNativeServiceRegistry(),
                null,
                new NativeLoaderLiveRuntimeAttachment(
                        "agent5:missing_resource_reload_evidence_runtime_truth_gate",
                        "minecraft_client_runtime",
                        "native_loader_missing_resource_reload_evidence_runtime",
                        true,
                        false,
                        List.of("resource_reloads"),
                        Map.of(
                                "releaseRuntimeTrusted", true,
                                "realMinecraftProcess", true,
                                "nativeRuntimeProcess", true,
                                "agent5TruthGate", true
                        )
                ),
                new MissingResourceReloadEvidenceMinecraftBridge()
        ));
        require(missingResourceReloadEvidenceHost.reloadResources(
                        "echoashfallprotocol",
                        "ashfall_resources",
                        "client_resources",
                        Map.of("scope", "client_resources"))
                        == EchoNativeLoadStatus.MUTATED,
                "missing-resource-reload-evidence bridge should still mutate native resource reload state.");
        Map<String, Object> missingResourceReloadEvidenceReport = missingResourceReloadEvidenceHost.runtimeHostReport();
        require(Boolean.FALSE.equals(missingResourceReloadEvidenceReport.get("liveRuntimeSurfaceMutationCoverageSatisfied")),
                "resource_reloads proof must require explicit runtime resource save evidence: "
                        + missingResourceReloadEvidenceReport);
        require(Boolean.FALSE.equals(missingResourceReloadEvidenceReport.get("liveRuntimeReleaseProofSatisfied")),
                "resource_reloads without live resource save evidence must not satisfy release proof: "
                        + missingResourceReloadEvidenceReport);
        require(((List<?>) missingResourceReloadEvidenceReport.get("liveRuntimeUnbridgedMutatedSurfaces")).contains("resourceReloads"),
                "missing-resource-reload-evidence runtime host must name resourceReloads as unbridged: "
                        + missingResourceReloadEvidenceReport);

        NativeLoaderRuntimeHost missingPacketHudEvidenceHost = new NativeLoaderRuntimeHost(new NativeLoaderRuntimeHostContext(
                "echoashfallprotocol",
                "echoadaptercore",
                new EchoNativeServiceRegistry(),
                null,
                new NativeLoaderLiveRuntimeAttachment(
                        "agent5:missing_packet_hud_evidence_runtime_truth_gate",
                        "minecraft_client_runtime",
                        "native_loader_missing_packet_hud_evidence_runtime",
                        true,
                        false,
                        List.of("packets_hud"),
                        Map.of(
                                "releaseRuntimeTrusted", true,
                                "realMinecraftProcess", true,
                                "nativeRuntimeProcess", true,
                                "agent5TruthGate", true
                        )
                ),
                new MissingPacketHudEvidenceMinecraftBridge()
        ));
        require(missingPacketHudEvidenceHost.sendPacketHud("ashfall:first_spawn_hud", "terminal=linked")
                        == EchoNativeLoadStatus.MUTATED,
                "missing-packet-HUD-evidence bridge should still mutate native packet/HUD state.");
        Map<String, Object> missingPacketHudEvidenceReport = missingPacketHudEvidenceHost.runtimeHostReport();
        require(Boolean.FALSE.equals(missingPacketHudEvidenceReport.get("liveRuntimeSurfaceMutationCoverageSatisfied")),
                "packets_hud proof must require explicit packet evidence: "
                        + missingPacketHudEvidenceReport);
        require(Boolean.FALSE.equals(missingPacketHudEvidenceReport.get("liveRuntimeReleaseProofSatisfied")),
                "packets_hud without live packet evidence must not satisfy release proof: "
                        + missingPacketHudEvidenceReport);
        require(((List<?>) missingPacketHudEvidenceReport.get("liveRuntimeUnbridgedMutatedSurfaces")).contains("packetsHud"),
                "missing-packet-HUD-evidence runtime host must name packetsHud as unbridged: "
                        + missingPacketHudEvidenceReport);

        NativeLoaderRuntimeHost missingEventEvidenceHost = new NativeLoaderRuntimeHost(new NativeLoaderRuntimeHostContext(
                "echoashfallprotocol",
                "echoadaptercore",
                new EchoNativeServiceRegistry(),
                null,
                new NativeLoaderLiveRuntimeAttachment(
                        "agent5:missing_event_evidence_runtime_truth_gate",
                        "minecraft_client_runtime",
                        "native_loader_missing_event_evidence_runtime",
                        true,
                        false,
                        List.of("events"),
                        Map.of(
                                "releaseRuntimeTrusted", true,
                                "realMinecraftProcess", true,
                                "nativeRuntimeProcess", true,
                                "agent5TruthGate", true
                        )
                ),
                new MissingEventEvidenceMinecraftBridge()
        ));
        require(missingEventEvidenceHost.emitEvent("ashfall:first_spawn_event", "terminal=linked")
                        == EchoNativeLoadStatus.MUTATED,
                "missing-event-evidence bridge should still mutate native event state.");
        Map<String, Object> missingEventEvidenceReport = missingEventEvidenceHost.runtimeHostReport();
        require(Boolean.FALSE.equals(missingEventEvidenceReport.get("liveRuntimeSurfaceMutationCoverageSatisfied")),
                "events proof must require explicit runtime event publication evidence: "
                        + missingEventEvidenceReport);
        require(Boolean.FALSE.equals(missingEventEvidenceReport.get("liveRuntimeReleaseProofSatisfied")),
                "events without live event publication evidence must not satisfy release proof: "
                        + missingEventEvidenceReport);
        require(((List<?>) missingEventEvidenceReport.get("liveRuntimeUnbridgedMutatedSurfaces")).contains("events"),
                "missing-event-evidence runtime host must name events as unbridged: "
                        + missingEventEvidenceReport);

        NativeLoaderRuntimeHost missingClientSurfaceSaveEvidenceHost = new NativeLoaderRuntimeHost(new NativeLoaderRuntimeHostContext(
                "echoashfallprotocol",
                "echoadaptercore",
                new EchoNativeServiceRegistry(),
                null,
                new NativeLoaderLiveRuntimeAttachment(
                        "agent5:missing_client_surface_save_evidence_runtime_truth_gate",
                        "minecraft_client_runtime",
                        "native_loader_missing_client_surface_save_evidence_runtime",
                        true,
                        false,
                        List.of("client_tick"),
                        Map.of(
                                "releaseRuntimeTrusted", true,
                                "realMinecraftProcess", true,
                                "nativeRuntimeProcess", true,
                                "agent5TruthGate", true
                        )
                ),
                new MissingClientSurfaceSaveEvidenceMinecraftBridge()
        ));
        require(missingClientSurfaceSaveEvidenceHost.clientTick("client_tick_end", Map.of("tick", 1))
                        == EchoNativeLoadStatus.MUTATED,
                "missing-client-surface-save-evidence bridge should still mutate native client tick state.");
        Map<String, Object> missingClientSurfaceSaveEvidenceReport =
                missingClientSurfaceSaveEvidenceHost.runtimeHostReport();
        require(Boolean.FALSE.equals(missingClientSurfaceSaveEvidenceReport.get("liveRuntimeSurfaceMutationCoverageSatisfied")),
                "client_tick proof must require explicit runtime-surface save evidence: "
                        + missingClientSurfaceSaveEvidenceReport);
        require(Boolean.FALSE.equals(missingClientSurfaceSaveEvidenceReport.get("liveRuntimeReleaseProofSatisfied")),
                "client_tick without runtime-surface save evidence must not satisfy release proof: "
                        + missingClientSurfaceSaveEvidenceReport);
        require(((List<?>) missingClientSurfaceSaveEvidenceReport.get("liveRuntimeUnbridgedMutatedSurfaces")).contains("clientTicks"),
                "missing-client-surface-save-evidence runtime host must name clientTicks as unbridged: "
                        + missingClientSurfaceSaveEvidenceReport);

        NativeLoaderRuntimeHost missingHudEvidenceHost = new NativeLoaderRuntimeHost(new NativeLoaderRuntimeHostContext(
                "echoashfallprotocol",
                "echoadaptercore",
                new EchoNativeServiceRegistry(),
                null,
                new NativeLoaderLiveRuntimeAttachment(
                        "agent5:missing_hud_evidence_runtime_truth_gate",
                        "minecraft_client_runtime",
                        "native_loader_missing_hud_evidence_runtime",
                        true,
                        false,
                        List.of("hud"),
                        Map.of(
                                "releaseRuntimeTrusted", true,
                                "realMinecraftProcess", true,
                                "nativeRuntimeProcess", true,
                                "agent5TruthGate", true
                        )
                ),
                new MissingHudEvidenceMinecraftBridge()
        ));
        require(missingHudEvidenceHost.emitHud("ashfall:hud", "terminal=linked")
                        == EchoNativeLoadStatus.MUTATED,
                "missing-HUD-evidence bridge should still mutate native HUD state.");
        Map<String, Object> missingHudEvidenceReport = missingHudEvidenceHost.runtimeHostReport();
        require(Boolean.FALSE.equals(missingHudEvidenceReport.get("liveRuntimeSurfaceMutationCoverageSatisfied")),
                "hud proof must require explicit HUD notification evidence: "
                        + missingHudEvidenceReport);
        require(Boolean.FALSE.equals(missingHudEvidenceReport.get("liveRuntimeReleaseProofSatisfied")),
                "hud without notification evidence must not satisfy release proof: "
                        + missingHudEvidenceReport);
        require(((List<?>) missingHudEvidenceReport.get("liveRuntimeUnbridgedMutatedSurfaces")).contains("hud"),
                "missing-HUD-evidence runtime host must name hud as unbridged: "
                        + missingHudEvidenceReport);

        NativeLoaderRuntimeHost missingServerClientSyncPacketEvidenceHost = new NativeLoaderRuntimeHost(new NativeLoaderRuntimeHostContext(
                "echoashfallprotocol",
                "echoadaptercore",
                new EchoNativeServiceRegistry(),
                null,
                new NativeLoaderLiveRuntimeAttachment(
                        "agent5:missing_server_client_sync_packet_evidence_runtime_truth_gate",
                        "minecraft_client_runtime",
                        "native_loader_missing_server_client_sync_packet_evidence_runtime",
                        true,
                        false,
                        List.of("server_client_sync"),
                        Map.of(
                                "releaseRuntimeTrusted", true,
                                "realMinecraftProcess", true,
                                "nativeRuntimeProcess", true,
                                "agent5TruthGate", true
                        )
                ),
                new MissingServerClientSyncPacketEvidenceMinecraftBridge()
        ));
        require(missingServerClientSyncPacketEvidenceHost.syncServerClient("ashfall:sync", "terminal=linked")
                        == EchoNativeLoadStatus.MUTATED,
                "missing-server-client-sync-packet-evidence bridge should still mutate native sync state.");
        Map<String, Object> missingServerClientSyncPacketEvidenceReport =
                missingServerClientSyncPacketEvidenceHost.runtimeHostReport();
        require(Boolean.FALSE.equals(missingServerClientSyncPacketEvidenceReport.get("liveRuntimeSurfaceMutationCoverageSatisfied")),
                "server_client_sync proof must require explicit packet-send evidence: "
                        + missingServerClientSyncPacketEvidenceReport);
        require(Boolean.FALSE.equals(missingServerClientSyncPacketEvidenceReport.get("liveRuntimeReleaseProofSatisfied")),
                "server_client_sync without live packet evidence must not satisfy release proof: "
                        + missingServerClientSyncPacketEvidenceReport);
        require(((List<?>) missingServerClientSyncPacketEvidenceReport.get("liveRuntimeUnbridgedMutatedSurfaces")).contains("serverClientSync"),
                "missing-server-client-sync-packet-evidence runtime host must name serverClientSync as unbridged: "
                        + missingServerClientSyncPacketEvidenceReport);

        NativeLoaderRuntimeHost callerSeededSurfaceDirectHost = new NativeLoaderRuntimeHost(new NativeLoaderRuntimeHostContext(
                "echoashfallprotocol",
                "echoadaptercore",
                new EchoNativeServiceRegistry(),
                null,
                new NativeLoaderLiveRuntimeAttachment(
                        "agent5:caller_seeded_direct_surface_runtime_truth_gate",
                        "minecraft_client_runtime",
                        "native_loader_caller_seeded_direct_surface_runtime",
                        true,
                        false,
                        List.of("client_tick"),
                        Map.of(
                                "releaseRuntimeTrusted", true,
                                "realMinecraftProcess", true,
                                "nativeRuntimeProcess", true,
                                "agent5TruthGate", true
                        )
                ),
                new CallerSeededSurfaceDirectEvidenceMinecraftBridge()
        ));
        require(callerSeededSurfaceDirectHost.clientTick(
                        "client",
                        Map.of(
                                "source", "agent5_truth_gate",
                                "liveRuntimeSurface", "client_tick"))
                        == EchoNativeLoadStatus.MUTATED,
                "caller-seeded direct evidence bridge should still mutate native client tick state.");
        Map<String, Object> callerSeededSurfaceDirectReport = callerSeededSurfaceDirectHost.runtimeHostReport();
        require(Boolean.FALSE.equals(callerSeededSurfaceDirectReport.get("liveRuntimeSurfaceMutationCoverageSatisfied")),
                "caller-seeded liveRuntimeSurface evidence must be cleared before direct runtime proof: "
                        + callerSeededSurfaceDirectReport);
        require(Boolean.FALSE.equals(callerSeededSurfaceDirectReport.get("liveRuntimeReleaseProofSatisfied")),
                "caller-seeded liveRuntimeSurface evidence must not satisfy release proof: "
                        + callerSeededSurfaceDirectReport);
        require(((List<?>) callerSeededSurfaceDirectReport.get("liveRuntimeUnbridgedMutatedSurfaces")).contains("clientTicks"),
                "caller-seeded direct runtime host must name clientTicks as unbridged when bridge omits liveRuntimeSurface: "
                        + callerSeededSurfaceDirectReport);

        NativeLoaderMutationLedger spoofedSurfaceLedger = new NativeLoaderMutationLedger();
        NativeLoaderMutationLedger.MutationRecord spoofedSurfaceRecord = spoofedSurfaceLedger.record(
                "resource_reloads",
                "reload",
                "spoofed_surface_reload",
                EchoNativeLoadStatus.MUTATED,
                "",
                "",
                NativeLoaderAdapterCoreBackend.SERVICE_ID,
                null,
                NativeLoaderAdapterCoreBackend.class.getName(),
                NativeLoaderRuntimeHost.class.getName(),
                "adaptercore.native_runtime",
                "agent5:spoofed_surface_runtime",
                true,
                List.of(),
                Map.of(
                        "adapterCoreSurface", "inventory",
                        "liveRuntimeAccessed", true,
                        "minecraftRuntimeAccessed", true,
                        "liveRuntimeMutationSupported", true,
                        "liveRuntimeReleaseProofSatisfied", true,
                        "liveRuntimeSurfaceMutationSatisfied", true,
                        "surfaceLiveRuntimeReleaseProofSatisfied", true,
                        "surfaceLiveRuntimeAccessed", true,
                        "surfaceMinecraftRuntimeAccessed", true
                )
        );
        require(!spoofedSurfaceRecord.liveRuntimeSurfaceMutationSatisfied(),
                "ledger must reject surface mutation proof when evidence is scoped to another AdapterCore surface.");
        require(!spoofedSurfaceRecord.liveRuntimeReleaseProofSatisfied(),
                "ledger must reject release proof when evidence is scoped to another AdapterCore surface.");
        require(spoofedSurfaceLedger.liveRuntimeProofRecordCount() == 0,
                "ledger must not count mismatched surface proof as live runtime proof.");
        require(adapterCoreTopLevelProofAudit(spoofedSurfaceLedger).isEmpty(),
                "top-level proof audit must not include mismatched-surface proof records.");
        NativeLoaderMutationLedger statusOnlySurfaceLedger = new NativeLoaderMutationLedger();
        NativeLoaderMutationLedger.MutationRecord statusOnlySurfaceRecord = statusOnlySurfaceLedger.record(
                "inventory",
                "grant_item",
                "status_only_surface",
                EchoNativeLoadStatus.MUTATED,
                "",
                "",
                NativeLoaderAdapterCoreBackend.SERVICE_ID,
                null,
                NativeLoaderAdapterCoreBackend.class.getName(),
                NativeLoaderRuntimeHost.class.getName(),
                "adaptercore.native_runtime",
                "agent5:status_only_surface_runtime",
                true,
                List.of(),
                Map.of(
                        "adapterCoreSurface", "inventory",
                        "liveRuntimeAccessed", true,
                        "minecraftRuntimeAccessed", true,
                        "liveRuntimeMutationSupported", true,
                        "liveRuntimeReleaseProofSatisfied", true,
                        "liveRuntimeBridgeStatusBySurface", Map.of("inventory", "MUTATED")
                )
        );
        require(!statusOnlySurfaceRecord.liveRuntimeSurfaceMutationSatisfied(),
                "ledger must reject status-only direct surface evidence without bridge-owned proof.");
        require(!statusOnlySurfaceRecord.liveRuntimeReleaseProofSatisfied(),
                "ledger must reject status-only direct release proof without bridge-owned proof.");
        require(statusOnlySurfaceLedger.liveRuntimeProofRecordCount() == 0,
                "ledger must not count status-only bridge maps as live runtime proof.");
        require(adapterCoreTopLevelProofAudit(statusOnlySurfaceLedger).isEmpty(),
                "top-level proof audit must not include status-only bridge maps.");
        NativeLoaderMutationLedger staleDispatchProofLedger = new NativeLoaderMutationLedger();
        NativeLoaderMutationLedger.MutationRecord staleDispatchProofRecord = staleDispatchProofLedger.record(
                "inventory",
                "grant_item",
                "stale_dispatch_surface",
                EchoNativeLoadStatus.MUTATED,
                "",
                "",
                NativeLoaderAdapterCoreBackend.SERVICE_ID,
                null,
                NativeLoaderAdapterCoreBackend.class.getName(),
                NativeLoaderRuntimeHost.class.getName(),
                "adaptercore.native_runtime",
                "agent5:stale_dispatch_surface_runtime",
                true,
                List.of(),
                Map.ofEntries(
                        Map.entry("adapterCoreSurface", "inventory"),
                        Map.entry("adapterCoreSurfaceDispatchId", "agent5:inventory:current"),
                        Map.entry("liveRuntimeAccessed", true),
                        Map.entry("minecraftRuntimeAccessed", true),
                        Map.entry("liveRuntimeMutationSupported", true),
                        Map.entry("liveRuntimeReleaseProofSatisfied", true),
                        Map.entry("liveRuntimeSurfaceMutationSatisfied", true),
                        Map.entry("surfaceLiveRuntimeReleaseProofSatisfied", true),
                        Map.entry("surfaceLiveRuntimeAccessed", true),
                        Map.entry("surfaceMinecraftRuntimeAccessed", true),
                        Map.entry("surfaceLiveRuntimeProofEvidence", Map.of(
                                "liveRuntimeDispatchId", "agent5:inventory:stale",
                                "liveRuntimeSurface", "inventory",
                                "liveRuntimeDispatchProofSatisfied", true,
                                "liveRuntimeDispatchMinecraftAccessed", true,
                                "liveRuntimeDispatchMutationSupported", true,
                                "liveRuntimeDispatchLiveMutation", true
                        ))
                )
        );
        require(!staleDispatchProofRecord.liveRuntimeSurfaceMutationSatisfied(),
                "ledger must reject stale surface proof whose dispatch id does not match the AdapterCore record.");
        require(!staleDispatchProofRecord.liveRuntimeReleaseProofSatisfied(),
                "ledger must reject release proof when concrete proof dispatch id is stale.");
        require(staleDispatchProofLedger.liveRuntimeProofRecordCount() == 0,
                "ledger must not count stale dispatch proof as live runtime proof.");
        require(adapterCoreTopLevelProofAudit(staleDispatchProofLedger).isEmpty(),
                "top-level proof audit must not include stale dispatch proof.");
        NativeLoaderMutationLedger nonMinecraftSurfaceProofLedger = new NativeLoaderMutationLedger();
        NativeLoaderMutationLedger.MutationRecord nonMinecraftSurfaceProofRecord = nonMinecraftSurfaceProofLedger.record(
                "inventory",
                "grant_item",
                "non_minecraft_surface_proof",
                EchoNativeLoadStatus.MUTATED,
                "",
                "",
                NativeLoaderAdapterCoreBackend.SERVICE_ID,
                null,
                NativeLoaderAdapterCoreBackend.class.getName(),
                NativeLoaderRuntimeHost.class.getName(),
                "adaptercore.native_runtime",
                "agent5:non_minecraft_surface_proof_runtime",
                true,
                List.of(),
                Map.of(
                        "adapterCoreSurface", "inventory",
                        "liveRuntimeAccessed", true,
                        "minecraftRuntimeAccessed", false,
                        "liveRuntimeMutationSupported", true,
                        "liveRuntimeReleaseProofSatisfied", true,
                        "liveRuntimeSurfaceMutationSatisfied", true,
                        "surfaceLiveRuntimeReleaseProofSatisfied", true,
                        "surfaceLiveRuntimeAccessed", true,
                        "surfaceMinecraftRuntimeAccessed", true
                )
        );
        require(!nonMinecraftSurfaceProofRecord.liveRuntimeReleaseProofSatisfied(),
                "ledger must reject per-record release proof when top-level Minecraft runtime access is false.");
        require(nonMinecraftSurfaceProofLedger.liveRuntimeProofRecordCount() == 0,
                "ledger must not count scoped surface proof without top-level Minecraft runtime access.");
        require(adapterCoreTopLevelProofAudit(nonMinecraftSurfaceProofLedger).isEmpty(),
                "top-level proof audit must not include non-Minecraft surface proof.");
        NativeLoaderMutationLedger missingSurfaceLiveAccessLedger = new NativeLoaderMutationLedger();
        NativeLoaderMutationLedger.MutationRecord missingSurfaceLiveAccessRecord = missingSurfaceLiveAccessLedger.record(
                "inventory",
                "grant_item",
                "missing_surface_live_access",
                EchoNativeLoadStatus.MUTATED,
                "",
                "",
                NativeLoaderAdapterCoreBackend.SERVICE_ID,
                null,
                NativeLoaderAdapterCoreBackend.class.getName(),
                NativeLoaderRuntimeHost.class.getName(),
                "adaptercore.native_runtime",
                "agent5:missing_surface_live_access_runtime",
                true,
                List.of(),
                Map.of(
                        "adapterCoreSurface", "inventory",
                        "liveRuntimeAccessed", true,
                        "minecraftRuntimeAccessed", true,
                        "liveRuntimeMutationSupported", true,
                        "liveRuntimeReleaseProofSatisfied", true,
                        "surfaceLiveRuntimeReleaseProofSatisfied", true,
                        "surfaceMinecraftRuntimeAccessed", true
                )
        );
        require(!missingSurfaceLiveAccessRecord.liveRuntimeSurfaceMutationSatisfied(),
                "ledger must reject scoped surface proof without surface live runtime access.");
        require(!missingSurfaceLiveAccessRecord.liveRuntimeReleaseProofSatisfied(),
                "ledger must reject release proof without scoped surface live runtime access.");
        require(missingSurfaceLiveAccessLedger.liveRuntimeProofRecordCount() == 0,
                "ledger must not count scoped surface proof without surface live runtime access.");
        require(adapterCoreTopLevelProofAudit(missingSurfaceLiveAccessLedger).isEmpty(),
                "top-level proof audit must not include missing-surface-live-access proof.");

        NativeLoaderMutationLedger rawSaveMutationLedger = new NativeLoaderMutationLedger();
        rawSaveMutationLedger.record(
                "save_data",
                "write",
                "raw_save_mutation",
                EchoNativeLoadStatus.MUTATED,
                "",
                "mirror_save_value",
                NativeLoaderAdapterCoreBackend.SERVICE_ID,
                null,
                NativeLoaderAdapterCoreBackend.class.getName(),
                NativeLoaderRuntimeHost.class.getName(),
                "adaptercore.native_runtime",
                "agent5:raw_save_mutation_runtime",
                true,
                List.of(),
                Map.of(
                        "adapterCoreSurface", "save_data",
                        "liveRuntimeAccessed", false,
                        "minecraftRuntimeAccessed", false,
                        "liveRuntimeMutationSupported", false,
                        "runtimeSaveDataTouched", true
                )
        );
        require(rawSaveMutationLedger.mutatedRecordCountBySurface("save_data") == 1,
                "raw save-data mutation should remain visible as diagnostic ledger state.");
        require(adapterCoreTopLevelProofAudit(rawSaveMutationLedger).isEmpty(),
                "top-level proof audit must not include raw save-data mirror mutation.");
        require(rawSaveMutationLedger.liveRuntimeProofRecordCountBySurface("save_data") == 0,
                "raw save-data mutation must not count as live save-data release proof.");

        LiveMinecraftBridge liveBridge = new LiveMinecraftBridge();
        NativeLoaderLiveRuntimeAttachment attachment = new NativeLoaderLiveRuntimeAttachment(
                "agent5:live_minecraft_truth_gate",
                "minecraft_client_runtime",
                "native_loader_live_minecraft_runtime",
                true,
                false,
                List.of(
                        "inventory",
                        "player_state",
                        "world_blocks",
                        "world_state",
                        "structures",
                        "block_entities",
                        "capabilities",
                        "missions",
                        "events",
                        "packets_hud",
                        "hud",
                        "save_data",
                        "client_tick",
                        "render_layers",
                        "screen_events",
                        "keybinds",
                        "resource_reloads",
                        "save_hooks",
                        "server_client_sync"
                ),
                Map.of(
                        "releaseRuntimeTrusted", true,
                        "realMinecraftProcess", true,
                        "nativeRuntimeProcess", true,
                        "firstClassNativeRuntime", true,
                        "agent5TruthGate", true
                )
        );
        EchoNativeServiceRegistry liveRegistry = new EchoNativeServiceRegistry();
        NativeLoaderRuntimeHost liveHost = new NativeLoaderRuntimeHost(new NativeLoaderRuntimeHostContext(
                "echoashfallprotocol",
                "echoadaptercore",
                liveRegistry,
                null,
                attachment,
                liveBridge
        ));
        NativeLoaderServiceBridge adapterServiceBridge = new NativeLoaderServiceBridge(liveRegistry);
        NativeLoaderMutationLedger adapterLedger = new NativeLoaderMutationLedger();
        NativeLoaderCommandHost commandHost = new NativeLoaderCommandHost(liveBridge);
        NativeLoaderNetworkHost networkHost = new NativeLoaderNetworkHost(liveBridge);
        NativeLoaderConfigHost configHost = new NativeLoaderConfigHost(liveBridge);
        NativeLoaderLifecycleEventHost lifecycleHost = new NativeLoaderLifecycleEventHost(liveBridge);
        NativeLoaderAdapterCoreBackend adapterBackend = new NativeLoaderAdapterCoreBackend(
                liveHost,
                adapterServiceBridge,
                adapterLedger,
                commandHost,
                networkHost,
                configHost,
                lifecycleHost
        );
        liveRegistry.register(
                "echocore",
                NativeLoaderCommandHost.SERVICE_ID,
                commandHost,
                List.of("commands", "command", "server.commands", "command.queue", "adaptercore.native_command"),
                NativeLoaderCommandHost.class.getName()
        );
        liveRegistry.register(
                "echocore",
                NativeLoaderNetworkHost.SERVICE_ID,
                networkHost,
                List.of("network", "networking", "network_channels", "channels", "packet", "packets",
                        "adaptercore.native_runtime_packet", "packets_hud", "server_client_sync"),
                NativeLoaderNetworkHost.class.getName()
        );
        liveRegistry.register(
                "echocore",
                NativeLoaderConfigHost.SERVICE_ID,
                configHost,
                List.of("config", "configs", "configuration", "config_reloads", "client.config", "server.config"),
                NativeLoaderConfigHost.class.getName()
        );
        liveRegistry.register(
                "echocore",
                NativeLoaderLifecycleEventHost.LIFECYCLE_SERVICE_ID,
                lifecycleHost,
                List.of("lifecycle", "lifecycle_phases", "lifecycle.phases"),
                NativeLoaderLifecycleEventHost.class.getName()
        );
        liveRegistry.register(
                "echocore",
                NativeLoaderLifecycleEventHost.EVENT_SERVICE_ID,
                lifecycleHost,
                List.of("events", "event", "runtime.spine"),
                NativeLoaderLifecycleEventHost.class.getName()
        );
        liveRegistry.register(
                "echoadaptercore",
                NativeLoaderAdapterCoreBackend.SERVICE_ID,
                adapterBackend,
                List.of(
                        "inventory",
                        "player_state",
                        "world_blocks",
                        "world_state",
                        "structures",
                        "block_entities",
                        "capabilities",
                        "missions",
                        "events",
                        "packets_hud",
                        "hud",
                        "save_data",
                        "client_tick",
                        "render_layers",
                        "screen_events",
                        "keybinds",
                        "commands",
                        "network_channels",
                        "config_reloads",
                        "resource_reloads",
                        "save_hooks",
                        "lifecycle_phases",
                        "server_client_sync"
                ),
                NativeLoaderAdapterCoreBackend.class.getName()
        );
        requireMutated(adapterBackend.grantItem("player:live", "echoashfallprotocol:drop_pod_beacon", 1),
                "AdapterCore backend inventory mutation should route through live bridge.");
        requireMutated(adapterBackend.updatePlayerState("player:live", "ashfall.first_spawn", "drop_pod_linked"),
                "AdapterCore backend player state mutation should route through live bridge.");
        requireMutated(adapterBackend.placeBlock("minecraft:overworld", 0, 80, 0, "echoashfallprotocol:drop_pod_marker"),
                "AdapterCore backend block mutation should route through live bridge.");
        requireMutated(adapterBackend.updateWorldState("minecraft:overworld", "ashfall.weather", "ashfall_storm"),
                "AdapterCore backend world state mutation should route through live bridge.");
        requireMutated(adapterBackend.placeStructure("minecraft:overworld", "echoashfallprotocol:trusted_bridge_structure", 8, 80, 8),
                "AdapterCore backend structure placement should route through live bridge.");
        requireMutated(adapterBackend.updateBlockEntity(
                        "minecraft:overworld",
                        8,
                        80,
                        8,
                        "echoashfallprotocol:terminal_state",
                        "linked"),
                "AdapterCore backend block entity mutation should route through live bridge.");
        requireMutated(adapterBackend.updateCapability(
                        "player:live",
                        "echoashfallprotocol:ashfall_energy",
                        "64"),
                "AdapterCore backend capability mutation should route through live bridge.");
        requireMutated(adapterBackend.updateMission(
                        "ashfall:first_spawn",
                        "active",
                        "link_terminal"),
                "AdapterCore backend mission mutation should route through live bridge.");
        requireMutated(adapterBackend.emitEvent("ashfall.first_spawn", "player:live"),
                "AdapterCore backend event mutation should route through live bridge.");
        requireMutated(adapterBackend.sendPacketHud("ashfall:first_spawn_sync", "terminal=linked"),
                "AdapterCore backend packet/HUD mutation should route through live bridge.");
        requireMutated(adapterBackend.writeSaveData("ashfall:first_spawn", "complete"),
                "AdapterCore backend save mutation should route through live bridge.");
        requireMutated(adapterBackend.emitHud("ashfall:hud", "Drop pod linked."),
                "AdapterCore backend HUD mutation should route through live bridge.");
        requireMutated(adapterBackend.clientTick("client_tick_end", Map.of("tick", 1)),
                "AdapterCore backend client tick should route through live bridge.");
        requireMutated(adapterBackend.renderLayer("ashfall_hud_overlay", Map.of("frame", 1)),
                "AdapterCore backend render layer should route through live bridge.");
        requireMutated(adapterBackend.screenEvent("terminal", "open", Map.of("route", "ashfall:first_spawn")),
                "AdapterCore backend screen event should route through live bridge.");
        requireMutated(adapterBackend.keybind("echo.terminal", "press", Map.of("source", "keyboard")),
                "AdapterCore backend keybind should route through live bridge.");
        NativeLoaderMutationLedger.MutationRecord commandRecord = adapterBackend.registerCommand(
                        "echoashfallprotocol",
                        "ashfall.first_spawn",
                        "commands",
                        "adaptercore.native_command",
                        Map.of("source", "agent5_truth_gate"));
        requireMutated(commandRecord,
                "AdapterCore backend command registration should route through live bridge.");
        NativeLoaderMutationLedger.MutationRecord networkRecord = adapterBackend.registerNetworkPacket(
                        "echoashfallprotocol",
                        "ashfall:first_spawn_sync",
                        "packets_hud",
                        "adaptercore.native_runtime_packet",
                        List.of("terminal", "hud"),
                        Map.of("source", "agent5_truth_gate"));
        requireMutated(networkRecord,
                "AdapterCore backend network packet registration should route through live bridge.");
        NativeLoaderMutationLedger.MutationRecord configRecord = adapterBackend.reloadConfig(
                        "echoashfallprotocol",
                        "ashfall-runtime",
                        "server.config",
                        Map.of("source", "agent5_truth_gate"));
        requireMutated(configRecord,
                "AdapterCore backend config reload should route through live bridge.");
        requireMutated(adapterBackend.reloadResources("echoashfallprotocol", "ashfall_datapack", "data_pack", Map.of("reload", true)),
                "AdapterCore backend resource reload should route through live bridge.");
        requireMutated(adapterBackend.saveHook("world_save", Map.of("level", "ashfall")),
                "AdapterCore backend save hook should route through live bridge.");
        NativeLoaderMutationLedger.MutationRecord lifecycleRecord = adapterBackend.lifecyclePhase(
                        "echoashfallprotocol",
                        "client_tick",
                        Map.of("summary", "client tick phase bridged to live runtime"));
        requireMutated(lifecycleRecord,
                "AdapterCore backend lifecycle phase should route through live bridge.");
        NativeLoaderMutationLedger.MutationRecord runtimeEventRecord = adapterBackend.publishRuntimeEvent(
                        "echoashfallprotocol",
                        "ashfall.first_spawn",
                        Map.of("player", "player:live"),
                        EchoNativeLoadStatus.MUTATED.name());
        requireMutated(runtimeEventRecord,
                "AdapterCore backend runtime event should route through live bridge.");
        requireMutated(adapterBackend.syncServerClient("ashfall:sync", "terminal=linked"),
                "AdapterCore backend server/client sync should route through live bridge.");
        requireLedgerRecordTopLevelProof(commandRecord, "commands");
        requireLedgerRecordTopLevelProof(networkRecord, "network_channels");
        requireLedgerRecordTopLevelProof(configRecord, "config_reloads");
        requireLedgerRecordTopLevelProof(lifecycleRecord, "lifecycle_phases");
        requireLedgerRecordTopLevelProof(runtimeEventRecord, "events");

        DynamicAttachLiveMinecraftBridge mixedCoverageBridge = new DynamicAttachLiveMinecraftBridge();
        NativeLoaderCommandHost mixedCommandHost = new NativeLoaderCommandHost(mixedCoverageBridge);
        NativeLoaderNetworkHost mixedNetworkHost = new NativeLoaderNetworkHost(mixedCoverageBridge);
        NativeLoaderConfigHost mixedConfigHost = new NativeLoaderConfigHost(mixedCoverageBridge);
        NativeLoaderLifecycleEventHost mixedLifecycleHost = new NativeLoaderLifecycleEventHost(mixedCoverageBridge);
        mixedCommandHost.registerDeclaredCommand(
                "echoashfallprotocol",
                "ashfall.mirror_command",
                "commands",
                "adaptercore.native_command",
                Map.of("source", "agent5_mixed_coverage"));
        mixedNetworkHost.registerDeclaredPacket(
                "echoashfallprotocol",
                "ashfall:mirror_packet",
                "network_channels",
                "adaptercore.native_runtime_packet",
                List.of("terminal"),
                Map.of("source", "agent5_mixed_coverage"));
        mixedConfigHost.registerConfig(
                "echoashfallprotocol",
                "ashfall-mirror-config",
                "server.config",
                Map.of("source", "agent5_mixed_coverage"));
        mixedLifecycleHost.recordDeclaredLifecyclePhase(
                "echoashfallprotocol",
                "mirror_phase",
                Map.of("source", "agent5_mixed_coverage"));
        mixedCoverageBridge.setAttached(true);
        mixedCommandHost.registerDeclaredCommand(
                "echoashfallprotocol",
                "ashfall.live_command",
                "commands",
                "adaptercore.native_command",
                Map.of("source", "agent5_mixed_coverage"));
        mixedNetworkHost.registerDeclaredPacket(
                "echoashfallprotocol",
                "ashfall:live_packet",
                "network_channels",
                "adaptercore.native_runtime_packet",
                List.of("terminal"),
                Map.of("source", "agent5_mixed_coverage"));
        mixedConfigHost.registerConfig(
                "echoashfallprotocol",
                "ashfall-live-config",
                "server.config",
                Map.of("source", "agent5_mixed_coverage"));
        mixedLifecycleHost.recordDeclaredLifecyclePhase(
                "echoashfallprotocol",
                "live_phase",
                Map.of("source", "agent5_mixed_coverage"));
        require(Boolean.FALSE.equals(mixedCommandHost.toReport().get("liveRuntimeReleaseProofSatisfied")),
                "command release proof must fail when mirror declarations remain undispatched: " + mixedCommandHost.toReport());
        require(Boolean.FALSE.equals(mixedNetworkHost.toReport().get("liveRuntimeReleaseProofSatisfied")),
                "network release proof must fail when mirror declarations remain undispatched: " + mixedNetworkHost.toReport());
        require(Boolean.FALSE.equals(mixedConfigHost.toReport().get("liveRuntimeReleaseProofSatisfied")),
                "config release proof must fail when mirror declarations remain undispatched: " + mixedConfigHost.toReport());
        require(Boolean.FALSE.equals(mixedLifecycleHost.toReport().get("liveRuntimeReleaseProofSatisfied")),
                "lifecycle release proof must fail when mirror declarations remain undispatched: " + mixedLifecycleHost.toReport());

        DynamicAttachLiveMinecraftBridge mixedRuntimeBridge = new DynamicAttachLiveMinecraftBridge();
        NativeLoaderRuntimeHost mixedRuntimeHost = new NativeLoaderRuntimeHost(new NativeLoaderRuntimeHostContext(
                "echoashfallprotocol",
                "echoadaptercore",
                new EchoNativeServiceRegistry(),
                null,
                attachment,
                mixedRuntimeBridge
        ));
        require(mixedRuntimeHost.grantItem("player:mixed_runtime", "echoashfallprotocol:mirror_only_marker", 1)
                        == EchoNativeLoadStatus.MUTATED,
                "mixed runtime host should still record fallback mirror mutation before live bridge attachment.");
        mixedRuntimeBridge.setAttached(true);
        require(mixedRuntimeHost.grantItem("player:mixed_runtime", "echoashfallprotocol:live_marker", 1)
                        == EchoNativeLoadStatus.MUTATED,
                "mixed runtime host should mutate live after bridge attachment.");
        Map<String, Object> mixedRuntimeReport = mixedRuntimeHost.runtimeHostReport();
        require(number(mixedRuntimeReport.get("fallbackMirrorMutationCount")) == 1,
                "mixed runtime host must expose fallback mirror mutation count: " + mixedRuntimeReport);
        require(Boolean.FALSE.equals(mixedRuntimeReport.get("liveRuntimeReleaseProofSatisfied")),
                "runtime release proof must fail when fallback mirror mutation remains in the host: " + mixedRuntimeReport);

        Map<String, Object> liveReport = liveHost.runtimeHostReport();
        require(Boolean.FALSE.equals(liveReport.get("mirrorOnlyReleaseProof")),
                "live host must not be marked mirror-only.");
        require(Boolean.TRUE.equals(liveReport.get("liveRuntimeReleaseProofSatisfied")),
                "live host must satisfy live runtime release proof.");
        require(number(liveReport.get("liveRuntimeBridgeMutationCount")) >= 15,
                "live host should record live bridge mutations across gameplay/runtime surfaces.");
        require(Boolean.TRUE.equals(liveReport.get("minecraftRuntimeAccessed")),
                "live host must record Minecraft runtime access when the bridge represents a Minecraft process.");
        requireMutatedLiveRuntimeSurfaces(
                liveReport,
                List.of(
                        "inventory",
                        "player_state",
                        "world_blocks",
                        "world_state",
                        "structures",
                        "block_entities",
                        "capabilities",
                        "missions",
                        "events",
                        "packets_hud",
                        "save_data",
                        "hud",
                        "client_tick",
                        "render_layers",
                        "screen_events",
                        "keybinds",
                        "resource_reloads",
                        "save_hooks",
                        "server_client_sync"
                )
        );
        require(adapterLedger.mutatedRecordCount() >= 20,
                "AdapterCore backend ledger should record mutated live runtime operations.");
        require(adapterLedger.records().stream().allMatch(EchoNativeAgent5RuntimeTruthGateMain::liveLedgerRecord),
                "Every AdapterCore backend ledger record must carry live runtime proof.");
        List<String> liveAdapterCoreProofSurfaces = liveAdapterCoreProofSurfaces(adapterLedger);
        requireContainsAllStrings(
                "Agent 5 live-positive AdapterCore proof surfaces",
                liveAdapterCoreProofSurfaces,
                REQUIRED_AGENT5_LIVE_PROOF_SURFACES
        );
        List<Map<String, Object>> adapterCoreTopLevelProofAudit = adapterCoreTopLevelProofAudit(adapterLedger);
        require(adapterCoreTopLevelProofAudit.size() >= REQUIRED_AGENT5_LIVE_PROOF_SURFACES.size(),
                "Agent 5 proof audit must include top-level proof entries for required surfaces: "
                        + adapterCoreTopLevelProofAudit);
        requireContainsAllStrings(
                "Agent 5 top-level AdapterCore proof audit surfaces",
                adapterCoreTopLevelProofAudit.stream()
                        .map(item -> String.valueOf(item.get("surface")))
                        .distinct()
                        .toList(),
                REQUIRED_AGENT5_LIVE_PROOF_SURFACES
        );
        require(adapterCoreTopLevelProofAudit.stream().allMatch(EchoNativeAgent5RuntimeTruthGateMain::topLevelProofAuditRowSatisfied),
                "Agent 5 top-level AdapterCore proof audit rows must expose proof, dispatch id, and runtime surface: "
                        + adapterCoreTopLevelProofAudit);
        require(number(commandHost.toReport().get("liveRuntimeMutationCount")) == 1,
                "command host should mutate through live bridge.");
        require(Boolean.TRUE.equals(commandHost.toReport().get("liveRuntimeReleaseProofSatisfied")),
                "command host should satisfy live Minecraft release proof.");
        requireSubsystemEntryLiveEvidence(
                commandHost.toReport(),
                "commands",
                List.of("runtimeSurfaceSaveTouched", "runtimeSurfaceSaveMutated", "runtimeSaveDataTouched",
                        "liveSaveDataFileTouched", "runtimeSaveDataBackend", "saveFile")
        );
        require(number(networkHost.toReport().get("liveRuntimeMutationCount")) == 1,
                "network host should mutate through live bridge.");
        require(Boolean.TRUE.equals(networkHost.toReport().get("liveRuntimeReleaseProofSatisfied")),
                "network host should satisfy live Minecraft release proof.");
        requireSubsystemEntryLiveEvidence(
                networkHost.toReport(),
                "packets",
                List.of("runtimeSurfaceSaveTouched", "runtimeSurfaceSaveMutated", "runtimeSaveDataTouched",
                        "liveSaveDataFileTouched", "runtimeSaveDataBackend", "saveFile",
                        "runtimeSurfacePacketSent", "runtimeSurfacePacketMutated")
        );
        require(number(configHost.toReport().get("liveRuntimeMutationCount")) == 1,
                "config host should mutate through live bridge.");
        require(Boolean.TRUE.equals(configHost.toReport().get("liveRuntimeReleaseProofSatisfied")),
                "config host should satisfy live Minecraft release proof.");
        requireSubsystemEntryLiveEvidence(
                configHost.toReport(),
                "configs",
                List.of("runtimeSurfaceSaveTouched", "runtimeSurfaceSaveMutated", "runtimeSaveDataTouched",
                        "liveSaveDataFileTouched", "runtimeSaveDataBackend", "saveFile")
        );
        require(number(lifecycleHost.toReport().get("liveRuntimeMutationCount")) == 2,
                "lifecycle/event host should mutate phases and events through live bridge.");
        require(Boolean.TRUE.equals(lifecycleHost.toReport().get("liveRuntimeReleaseProofSatisfied")),
                "lifecycle/event host should satisfy live Minecraft release proof.");
        Object publishedEvents = lifecycleHost.toReport().get("publishedEvents");
        require(publishedEvents instanceof List<?> && !((List<?>) publishedEvents).isEmpty(),
                "lifecycle/event host should report live published runtime events.");
        List<?> eventReports = (List<?>) publishedEvents;
        Object latestEventReport = eventReports.get(eventReports.size() - 1);
        require(latestEventReport instanceof Map<?, ?>,
                "lifecycle/event host published event report should be structured: " + latestEventReport);
        Map<?, ?> latestEventMap = (Map<?, ?>) latestEventReport;
        Object eventEvidence = latestEventMap.get("liveRuntimeEvidence");
        require(eventEvidence instanceof Map<?, ?> eventEvidenceMap
                        && Boolean.TRUE.equals(eventEvidenceMap.get("runtimeSurfaceEventPublished"))
                        && Boolean.TRUE.equals(eventEvidenceMap.get("runtimeSurfaceEventMutated"))
                        && "world_save_file".equals(String.valueOf(eventEvidenceMap.get("runtimeSaveDataBackend")))
                        && eventEvidenceMap.get("saveFile") instanceof String eventSaveFile
                        && !eventSaveFile.isBlank(),
                "published runtime events must expose live Minecraft event/save evidence in their report: "
                        + latestEventMap);

        Map<String, Object> ashfallBridgeCoverage = ashfallLiveRuntimeBridgeCoverage();
        Map<String, Object> releaseAndCliProofCoverage = releaseAndCliProofCoverage();
        Map<String, Object> subsystemHostProofCoverage = subsystemHostProofCoverage();
        Map<String, Object> runtimeHostProofCoverage = runtimeHostProofCoverage();
        Map<String, Object> adapterCoreLedgerProofCoverage = adapterCoreLedgerProofCoverage();
        Map<String, Object> resourceHostProofSeparation = resourceHostProofSeparation();
        Map<String, Object> serviceRoutingCoverage = serviceRoutingCoverage();
        Map<String, Object> surfaceContractParityCoverage = surfaceContractParityCoverage();

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("schema", "echo.native.agent5.runtime_truth_gate.v1");
        report.put("status", "PASS");
        report.put("mirrorOnlyRejected", true);
        report.put("nonMinecraftLiveRuntimeRejected", true);
        report.put("liveRuntimeAccepted", true);
        report.put("ashfallLiveRuntimeBridgeCoverage", ashfallBridgeCoverage);
        report.put("releaseAndCliProofCoverage", releaseAndCliProofCoverage);
        report.put("subsystemHostProofCoverage", subsystemHostProofCoverage);
        report.put("runtimeHostProofCoverage", runtimeHostProofCoverage);
        report.put("adapterCoreLedgerProofCoverage", adapterCoreLedgerProofCoverage);
        report.put("resourceHostProofSeparation", resourceHostProofSeparation);
        report.put("serviceRoutingCoverage", serviceRoutingCoverage);
        report.put("surfaceContractParityCoverage", surfaceContractParityCoverage);
        report.put("mirrorOnlyRuntimeHostReport", mirrorReport);
        report.put("nonMinecraftLiveRuntimeHostReport", nonMinecraftReport);
        report.put("nonMinecraftSubsystemHostReports", Map.of(
                "command", nonMinecraftCommandReport,
                "network", nonMinecraftNetworkReport,
                "config", nonMinecraftConfigReport,
                "lifecycleEvent", nonMinecraftLifecycleReport
        ));
        report.put("partialSubsystemHostReports", Map.of(
                "command", partialSubsystemCommandReport,
                "network", partialSubsystemNetworkReport,
                "config", partialSubsystemConfigReport,
                "lifecycleEvent", partialSubsystemLifecycleReport
        ));
        report.put("partialMinecraftSurfaceProofLedger", partialLedger.toReport());
        report.put("partialMinecraftRuntimeHostReport", partialRuntimeHostReport);
        report.put("liveRuntimeHostReport", liveReport);
        report.put("projectedNativeLoaderBackendReceiptProof", projectedRuntimeLiveProof);
        report.put("projectedCanonicalStartingStructureRuntime", projectedStructureStartResult);
        report.put("projectedCanonicalStartingStructureLedger", projectedStructureStartLedger);
        report.put("projectedCanonicalStartingStructureLiveProof", projectedStructureStartLiveProof);
        report.put("liveAdapterCoreProofSurfaces", liveAdapterCoreProofSurfaces);
        report.put("adapterCoreTopLevelProofAudit", adapterCoreTopLevelProofAudit);
        report.put("adapterCoreBackendLedger", adapterLedger.toReport());
        report.put("adapterCoreBackendServiceBridgeReport", adapterServiceBridge.toReport());
        report.put("commandHostReport", commandHost.toReport());
        report.put("networkHostReport", networkHost.toReport());
        report.put("configHostReport", configHost.toReport());
        report.put("lifecycleEventHostReport", lifecycleHost.toReport());
        report.put("liveBridgeCalls", liveBridge.calls());
        report.put("exitGate", "Release proof fails when runtime parity is satisfied only by Native Loader mirror state, non-Minecraft live mutation, or report projection.");

        Files.createDirectories(reportPath.getParent());
        Files.writeString(reportPath, toJson(report) + "\n", StandardCharsets.UTF_8);
        System.out.println("Agent 5 runtime truth gate PASS " + reportPath);
    }

    private static int number(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.parseInt(String.valueOf(value));
    }

    private static Map<String, Object> objectMap(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> copy = new LinkedHashMap<>();
            map.forEach((key, item) -> copy.put(String.valueOf(key), item));
            return Map.copyOf(copy);
        }
        return Map.of();
    }

    private static Map<String, Object> liveNativeLoaderBackendReceipt(
            String surface,
            String methodName,
            String operationId,
            String nativeInterface
    ) {
        Map<String, Object> runtimeHostReport = new LinkedHashMap<>();
        runtimeHostReport.put("runtimeHostClass", "com.knoxhack.echoashfallprotocol.event.NativeLoaderEchoRuntimeHost");
        runtimeHostReport.put("runtimeHostId", "echoashfallprotocol:native_loader_runtime_host");
        runtimeHostReport.put("runtimeLane", "Native Loader");
        runtimeHostReport.put("liveMinecraftDelegateId", "echoashfallprotocol:native_minecraft_runtime_host");
        runtimeHostReport.put("liveMinecraftAttached", true);
        runtimeHostReport.put("nativeLoaderLiveRuntimeBridgeAttached", true);
        runtimeHostReport.put("nativeLoaderBackendAttached", true);
        runtimeHostReport.put("firstClassNativeRuntime", true);
        runtimeHostReport.put("compatibilityDelegate", "");

        Map<String, Object> backendResultSnapshot = new LinkedHashMap<>();
        backendResultSnapshot.put("resultStatus", EchoNativeLoadStatus.MUTATED.name());
        backendResultSnapshot.put("operationId", operationId);
        backendResultSnapshot.put("nativeInterface", nativeInterface);
        backendResultSnapshot.put("realNativeStateMutated", true);
        backendResultSnapshot.put("stateMutated", true);
        backendResultSnapshot.put("compatibilityDelegate", "");
        backendResultSnapshot.put("releaseProof", false);

        Map<String, Object> backendRecord = new LinkedHashMap<>();
        backendRecord.put("directNativeLoaderBackendCall", true);
        backendRecord.put("methodName", methodName);
        backendRecord.put("nativeLoaderBackendClass", NativeLoaderAdapterCoreBackend.class.getName());
        backendRecord.put("adapterCoreBackendClass", NativeLoaderAdapterCoreBackend.class.getName());
        backendRecord.put("runtimeHostId", "echoashfallprotocol:native_loader_runtime_host");
        backendRecord.put("runtimeLane", "Native Loader");
        backendRecord.put("status", EchoNativeLoadStatus.MUTATED.name());
        backendRecord.put("resultSnapshot", Map.copyOf(backendResultSnapshot));

        Map<String, Object> resultSnapshot = new LinkedHashMap<>();
        resultSnapshot.put("resultStatus", EchoNativeLoadStatus.MUTATED.name());
        resultSnapshot.put("operationId", operationId);
        resultSnapshot.put("nativeInterface", nativeInterface);
        resultSnapshot.put("adapterCoreBackendClass", NativeLoaderAdapterCoreBackend.class.getName());
        resultSnapshot.put("adapterCoreCallEnteredNativeLoaderHost", true);
        resultSnapshot.put("adapterCoreCallEnteredNativeLoaderBackend", true);
        resultSnapshot.put("nativeLoaderBackendAttached", true);
        resultSnapshot.put("nativeLoaderBackendRecord", Map.copyOf(backendRecord));
        resultSnapshot.put("nativeLoaderBackendRecordStatus", EchoNativeLoadStatus.MUTATED.name());
        resultSnapshot.put("nativeLoaderRuntimeHostClass",
                "com.knoxhack.echoashfallprotocol.event.NativeLoaderEchoRuntimeHost");
        resultSnapshot.put("nativeLoaderRuntimeHostId", "echoashfallprotocol:native_loader_runtime_host");
        resultSnapshot.put("runtimeLane", "Native Loader");
        resultSnapshot.put("liveMinecraftDelegateClass",
                "com.knoxhack.echoashfallprotocol.event.NativeMinecraftEchoRuntimeHost");
        resultSnapshot.put("liveMinecraftDelegateId", "echoashfallprotocol:native_minecraft_runtime_host");
        resultSnapshot.put("liveMinecraftAttached", true);
        resultSnapshot.put("nativeLoaderLiveRuntimeBridgeAttached", true);
        resultSnapshot.put("firstClassNativeRuntime", true);
        resultSnapshot.put("realNativeStateMutated", true);
        resultSnapshot.put("stateMutated", true);
        resultSnapshot.put("compatibilityFallbackUsed", false);
        resultSnapshot.put("compatibilityDelegate", "");
        resultSnapshot.put("compatibilityBackendClass", "");
        resultSnapshot.put("nativeLoaderRuntimeHostReport", Map.copyOf(runtimeHostReport));

        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("mutated", true);
        evidence.put("runtimeHostRegistered", false);
        evidence.put("runtimeHostClass", "com.knoxhack.echoashfallprotocol.event.NativeLoaderEchoRuntimeHost");
        evidence.put("nativeLoaderRuntimeHostClass",
                "com.knoxhack.echoashfallprotocol.event.NativeLoaderEchoRuntimeHost");
        evidence.put("runtimeHostId", "echoashfallprotocol:native_loader_runtime_host");
        evidence.put("runtimeHostLane", "Native Loader");
        evidence.put("adapterCoreBackendClass", NativeLoaderAdapterCoreBackend.class.getName());
        evidence.put("adapterCoreCallEnteredNativeLoaderHost", true);
        evidence.put("adapterCoreCallEnteredNativeLoaderBackend", true);
        evidence.put("nativeLoaderBackendAttached", true);
        evidence.put("nativeLoaderBackendRecordStatus", EchoNativeLoadStatus.MUTATED.name());
        evidence.put("nativeLoaderBackendRecord", Map.copyOf(backendRecord));
        evidence.put("resultSnapshot", Map.copyOf(resultSnapshot));
        evidence.put("compatibilityFallbackUsed", false);
        evidence.put("compatibilityDelegate", "");
        evidence.put("compatibilityBackendClass", "");
        evidence.put("surface", surface);
        return Map.copyOf(evidence);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void requireMutated(NativeLoaderMutationLedger.MutationRecord record, String message) {
        require(record.status() == EchoNativeLoadStatus.MUTATED, message + " status=" + record.status());
        require(liveLedgerRecord(record), message + " missing live runtime ledger proof.");
    }

    private static void requireLedgerRecordTopLevelProof(
            NativeLoaderMutationLedger.MutationRecord record,
            String expectedSurface
    ) {
        Map<String, Object> report = record.toReport();
        require(expectedSurface.equals(String.valueOf(report.get("surface"))),
                "AdapterCore ledger record surface mismatch: expected=" + expectedSurface + " report=" + report);
        Object proof = report.get("surfaceLiveRuntimeProofEvidence");
        require(proof instanceof Map<?, ?> && !((Map<?, ?>) proof).isEmpty(),
                "AdapterCore ledger record must expose top-level concrete proof evidence: " + report);
        Map<?, ?> proofMap = (Map<?, ?>) proof;
        require(Boolean.TRUE.equals(proofMap.get("subsystemLiveRuntimeDispatchProofSatisfied"))
                        || Boolean.TRUE.equals(proofMap.get("liveRuntimeDispatchProofSatisfied")),
                "AdapterCore ledger proof evidence must expose dispatch proof satisfaction: " + report);
        require(Boolean.TRUE.equals(proofMap.get("liveMinecraftMutation")),
                "AdapterCore ledger proof evidence must expose live Minecraft mutation: " + report);
        require(Boolean.TRUE.equals(proofMap.get("minecraftRuntimeAccessed")),
                "AdapterCore ledger proof evidence must expose Minecraft runtime access: " + report);
        require(!String.valueOf(report.get("liveRuntimeDispatchId")).isBlank(),
                "AdapterCore ledger record must expose top-level dispatch id: " + report);
        require(!String.valueOf(report.get("liveRuntimeSurface")).isBlank(),
                "AdapterCore ledger record must expose top-level live runtime surface: " + report);
    }

    private static void requireSubsystemEntryLiveEvidence(
            Map<String, Object> report,
            String entryKey,
            List<String> requiredEvidenceKeys
    ) {
        Object entries = report.get(entryKey);
        require(entries instanceof List<?> && !((List<?>) entries).isEmpty(),
                "Subsystem report must include entries for " + entryKey + ": " + report);
        Object firstEntry = ((List<?>) entries).get(0);
        require(firstEntry instanceof Map<?, ?>,
                "Subsystem report entry must be structured for " + entryKey + ": " + firstEntry);
        Object evidence = ((Map<?, ?>) firstEntry).get("evidence");
        require(evidence instanceof Map<?, ?>,
                "Subsystem report entry must include live evidence for " + entryKey + ": " + firstEntry);
        Map<?, ?> evidenceMap = (Map<?, ?>) evidence;
        require(Boolean.TRUE.equals(evidenceMap.get("subsystemLiveRuntimeDispatchProofSatisfied")),
                "Subsystem entry must expose dispatch proof satisfaction for " + entryKey + ": " + firstEntry);
        require(Boolean.TRUE.equals(evidenceMap.get("liveMinecraftMutation")),
                "Subsystem entry must expose live Minecraft mutation for " + entryKey + ": " + firstEntry);
        require("world_save_file".equals(String.valueOf(evidenceMap.get("runtimeSaveDataBackend"))),
                "Subsystem entry must expose live world-save backend for " + entryKey + ": " + firstEntry);
        for (String key : requiredEvidenceKeys) {
            require(evidenceMap.containsKey(key),
                    "Subsystem entry evidence missing " + key + " for " + entryKey + ": " + firstEntry);
        }
    }

    private static boolean liveLedgerRecord(NativeLoaderMutationLedger.MutationRecord record) {
        return record != null
                && record.liveRuntimeAccessed()
                && record.minecraftRuntimeAccessed()
                && record.liveRuntimeMutationSupported()
                && !record.mirrorOnlyReleaseProof()
                && record.liveRuntimeReleaseProofSatisfied()
                && record.liveRuntimeSurfaceMutationSatisfied()
                && record.liveRuntimeBridgeMutationCount() > 0;
    }

    private static List<String> liveAdapterCoreProofSurfaces(NativeLoaderMutationLedger ledger) {
        if (ledger == null) {
            return List.of();
        }
        return ledger.records().stream()
                .filter(EchoNativeAgent5RuntimeTruthGateMain::liveLedgerRecord)
                .map(NativeLoaderMutationLedger.MutationRecord::surface)
                .distinct()
                .toList();
    }

    private static List<Map<String, Object>> adapterCoreTopLevelProofAudit(NativeLoaderMutationLedger ledger) {
        if (ledger == null) {
            return List.of();
        }
        List<Map<String, Object>> audit = new ArrayList<>();
        for (NativeLoaderMutationLedger.MutationRecord record : ledger.records()) {
            if (!liveLedgerRecord(record)) {
                continue;
            }
            Map<String, Object> recordReport = record.toReport();
            Map<String, Object> proofEvidence = objectMap(recordReport.get("surfaceLiveRuntimeProofEvidence"));
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("sequence", record.sequence());
            item.put("surface", record.surface());
            item.put("action", record.action());
            item.put("target", record.target());
            item.put("liveRuntimeDispatchId", recordReport.getOrDefault("liveRuntimeDispatchId", ""));
            item.put("liveRuntimeSurface", recordReport.getOrDefault("liveRuntimeSurface", ""));
            item.put("proofEvidencePresent", !proofEvidence.isEmpty());
            item.put("subsystemLiveRuntimeDispatchProofSatisfied",
                    Boolean.TRUE.equals(proofEvidence.get("subsystemLiveRuntimeDispatchProofSatisfied")));
            item.put("liveRuntimeDispatchProofSatisfied",
                    Boolean.TRUE.equals(proofEvidence.get("liveRuntimeDispatchProofSatisfied")));
            item.put("liveMinecraftMutation", Boolean.TRUE.equals(proofEvidence.get("liveMinecraftMutation")));
            item.put("minecraftRuntimeAccessed", Boolean.TRUE.equals(proofEvidence.get("minecraftRuntimeAccessed")));
            item.put("liveRuntimeDispatchMinecraftAccessed",
                    Boolean.TRUE.equals(proofEvidence.get("liveRuntimeDispatchMinecraftAccessed")));
            item.put("liveRuntimeDispatchMutationSupported",
                    Boolean.TRUE.equals(proofEvidence.get("liveRuntimeDispatchMutationSupported")));
            item.put("liveRuntimeDispatchLiveMutation",
                    Boolean.TRUE.equals(proofEvidence.get("liveRuntimeDispatchLiveMutation")));
            audit.add(Map.copyOf(item));
        }
        return List.copyOf(audit);
    }

    private static boolean topLevelProofAuditRowSatisfied(Map<String, Object> item) {
        if (item == null) {
            return false;
        }
        boolean dispatchProof = Boolean.TRUE.equals(item.get("subsystemLiveRuntimeDispatchProofSatisfied"))
                || Boolean.TRUE.equals(item.get("liveRuntimeDispatchProofSatisfied"));
        boolean minecraftAccess = Boolean.TRUE.equals(item.get("minecraftRuntimeAccessed"))
                || Boolean.TRUE.equals(item.get("liveRuntimeDispatchMinecraftAccessed"));
        boolean mutationSupported = Boolean.TRUE.equals(item.get("liveRuntimeDispatchMutationSupported"));
        boolean liveMutation = Boolean.TRUE.equals(item.get("liveMinecraftMutation"))
                || Boolean.TRUE.equals(item.get("liveRuntimeDispatchLiveMutation"));
        return Boolean.TRUE.equals(item.get("proofEvidencePresent"))
                && dispatchProof
                && minecraftAccess
                && mutationSupported
                && liveMutation
                && !String.valueOf(item.getOrDefault("liveRuntimeDispatchId", "")).isBlank()
                && !String.valueOf(item.getOrDefault("liveRuntimeSurface", "")).isBlank();
    }

    private static void requireMutatedLiveRuntimeSurfaces(Map<String, Object> runtimeReport, List<String> surfaces) {
        Object value = runtimeReport.get("liveRuntimeBridgeStatusBySurface");
        require(value instanceof Map<?, ?>, "live runtime report must include per-surface bridge status.");
        Map<?, ?> statuses = (Map<?, ?>) value;
        for (String surface : surfaces) {
            require("MUTATED".equals(String.valueOf(statuses.get(surface))),
                    "live runtime bridge surface must be MUTATED: " + surface + " statuses=" + statuses);
        }
    }

    private static Map<String, Object> surfaceContractParityCoverage() throws IOException {
        Path launcherPath = resolveSourcePath("echo-native-platform/echo-native-product-launcher/src/main/java/dev/echo/nativeplatform/product/EchoNativeProductLauncher.java");
        String launcherSource = Files.readString(launcherPath, StandardCharsets.UTF_8);
        List<String> productRequiredSurfaces = extractListOfStringLiterals(
                launcherSource,
                "public static List<String> requiredAgent5AdapterCoreLiveProofSurfaces()"
        );
        requireSameStringSet(
                "product required Agent 5 AdapterCore live proof surfaces",
                productRequiredSurfaces,
                REQUIRED_AGENT5_LIVE_PROOF_SURFACES
        );

        Path ashfallBridgePath = resolveSourcePath("addons/echoashfallprotocol/src/main/java/com/knoxhack/echoashfallprotocol/event/NativeLoaderEchoRuntimeHost.java");
        String ashfallBridgeSource = Files.readString(ashfallBridgePath, StandardCharsets.UTF_8);
        List<String> ashfallAdvertisedSurfaces = extractListOfStringLiterals(
                ashfallBridgeSource,
                "LIVE_RUNTIME_SURFACES = List.of"
        );
        requireContainsAllStrings(
                "Ashfall live runtime bridge advertised surfaces",
                ashfallAdvertisedSurfaces,
                REQUIRED_AGENT5_LIVE_PROOF_SURFACES
        );
        List<String> allowedAshfallExtras = List.of("feedback");
        for (String surface : ashfallAdvertisedSurfaces) {
            require(REQUIRED_AGENT5_LIVE_PROOF_SURFACES.contains(surface) || allowedAshfallExtras.contains(surface),
                    "Ashfall live runtime bridge advertises non-Agent5 release surface without explicit diagnostic allowance: "
                            + surface + " advertised=" + ashfallAdvertisedSurfaces);
        }

        Path ashfallProviderPath = resolveSourcePath("addons/echoashfallprotocol/src/main/java/com/knoxhack/echoashfallprotocol/nativebridge/AshfallNativeProductBridgeProvider.java");
        String ashfallProviderSource = Files.readString(ashfallProviderPath, StandardCharsets.UTF_8);
        List<String> ashfallProviderSurfaces = extractListOfStringLiterals(
                ashfallProviderSource,
                "RUNTIME_SURFACES = List.of"
        );
        requireContainsAllStrings(
                "Ashfall product bridge provider runtime surfaces",
                ashfallProviderSurfaces,
                REQUIRED_AGENT5_LIVE_PROOF_SURFACES
        );
        require(ashfallProviderSource.contains("plan.put(\"runtimeHooks\", runtimeHooks(context))"),
                "Ashfall product bridge provider must publish runtimeHooks for AdapterCore runtime execution.");
        for (String surface : REQUIRED_AGENT5_LIVE_PROOF_SURFACES) {
            require(ashfallProviderSource.contains("runtimeHook(context, \"" + surface + "\""),
                    "Ashfall product bridge provider must publish runtime hook for Agent 5 surface: " + surface);
        }

        Path runtimeHostPath = resolveSourcePath("echo-native-platform/echo-native-loader/src/main/java/dev/echo/nativeplatform/loader/NativeLoaderRuntimeHost.java");
        String runtimeHostSource = Files.readString(runtimeHostPath, StandardCharsets.UTF_8);
        List<String> runtimeHostSurfaces = extractListOfStringLiterals(runtimeHostSource, "SUPPORTED_SURFACES = List.of");
        requireContainsAllStrings(
                "Native Loader runtime host supported surfaces",
                runtimeHostSurfaces,
                REQUIRED_AGENT5_LIVE_PROOF_SURFACES
        );

        Path registrarPath = resolveSourcePath("echo-native-platform/echo-native-loader/src/main/java/dev/echo/nativeplatform/loader/NativeLoaderCoreServiceRegistrar.java");
        String registrarSource = Files.readString(registrarPath, StandardCharsets.UTF_8);
        for (String surface : REQUIRED_AGENT5_LIVE_PROOF_SURFACES) {
            require(registrarSource.contains("\"" + surface + "\""),
                    "Native Loader core service registrar must advertise product-required Agent 5 surface: " + surface);
        }
        require(registrarSource.contains("NativeLoaderLiveRuntimeAttachment liveRuntimeAttachment")
                        && registrarSource.contains("NativeLoaderLiveRuntimeBridge liveRuntimeBridge"),
                "Native Loader core service registrar must expose a live-runtime-aware core service registration overload.");
        require(registrarSource.contains("new NativeLoaderNetworkHost(safeBridge)")
                        && registrarSource.contains("new NativeLoaderConfigHost(safeBridge)")
                        && registrarSource.contains("new NativeLoaderCommandHost(safeBridge)")
                        && registrarSource.contains("new NativeLoaderLifecycleEventHost(safeBridge)"),
                "Native Loader core service registrar must wire live runtime bridge into subsystem hosts.");
        require(registrarSource.contains("safeAttachment")
                        && registrarSource.contains("safeBridge")
                        && registrarSource.contains("new NativeLoaderRuntimeHostContext("),
                "Native Loader core service registrar must wire live runtime attachment and bridge into the runtime host context.");

        return Map.of(
                "productRequiredSurfaceCount", productRequiredSurfaces.size(),
                "ashfallAdvertisedSurfaceCount", ashfallAdvertisedSurfaces.size(),
                "ashfallProviderSurfaceCount", ashfallProviderSurfaces.size(),
                "runtimeHostSurfaceCount", runtimeHostSurfaces.size(),
                "productRequiredSurfaces", productRequiredSurfaces,
                "requiredSurfaceParityEnforced", true,
                "ashfallDiagnosticExtras", allowedAshfallExtras
        );
    }

    private static List<String> extractListOfStringLiterals(String source, String marker) {
        int markerIndex = source.indexOf(marker);
        require(markerIndex >= 0, "source must contain list marker: " + marker);
        int listIndex = source.indexOf("List.of", markerIndex);
        require(listIndex >= 0, "source marker must be followed by List.of: " + marker);
        int openIndex = source.indexOf('(', listIndex);
        require(openIndex >= 0, "List.of marker must include an opening parenthesis: " + marker);

        String body = parenthesizedBody(source, openIndex, marker);
        List<String> values = new ArrayList<>();
        for (int index = 0; index < body.length(); index++) {
            if (body.charAt(index) != '"') {
                continue;
            }
            StringBuilder value = new StringBuilder();
            index++;
            while (index < body.length()) {
                char ch = body.charAt(index);
                if (ch == '\\' && index + 1 < body.length()) {
                    value.append(body.charAt(index + 1));
                    index += 2;
                    continue;
                }
                if (ch == '"') {
                    break;
                }
                value.append(ch);
                index++;
            }
            values.add(value.toString());
        }
        require(!values.isEmpty(), "List.of marker must contain string literals: " + marker);
        return List.copyOf(values);
    }

    private static String parenthesizedBody(String source, int openIndex, String marker) {
        int depth = 0;
        boolean inString = false;
        boolean escape = false;
        for (int index = openIndex; index < source.length(); index++) {
            char ch = source.charAt(index);
            if (inString) {
                if (escape) {
                    escape = false;
                } else if (ch == '\\') {
                    escape = true;
                } else if (ch == '"') {
                    inString = false;
                }
                continue;
            }
            if (ch == '"') {
                inString = true;
                continue;
            }
            if (ch == '(') {
                depth++;
                continue;
            }
            if (ch == ')') {
                depth--;
                if (depth == 0) {
                    return source.substring(openIndex + 1, index);
                }
            }
        }
        throw new IllegalStateException("Unclosed List.of declaration for marker: " + marker);
    }

    private static void requireSameStringSet(String label, List<String> actual, List<String> expected) {
        require(actual.size() == expected.size()
                        && actual.containsAll(expected)
                        && expected.containsAll(actual),
                label + " must exactly match Agent 5 release-required surfaces. actual="
                        + actual + " expected=" + expected);
    }

    private static void requireContainsAllStrings(String label, List<String> actual, List<String> required) {
        for (String item : required) {
            require(actual.contains(item), label + " missing required surface: " + item + " actual=" + actual);
        }
    }

    private static void requireSubsystemHostProofFieldGuards(String label, Path sourcePath, List<String> proofFields)
            throws IOException {
        String source = Files.readString(sourcePath, StandardCharsets.UTF_8);
        for (String field : proofFields) {
            require(source.contains("Boolean.TRUE.equals(evidence.get(" + field + "))"),
                    "Native Loader " + label + " host must require live proof field " + field);
            require(source.contains("evidence.remove(" + field + ")"),
                    "Native Loader " + label + " host must clear caller-supplied live proof field " + field);
        }
    }

    private static Map<String, Object> ashfallLiveRuntimeBridgeCoverage() throws IOException {
        Path bridgePath = resolveSourcePath("addons/echoashfallprotocol/src/main/java/com/knoxhack/echoashfallprotocol/event/NativeLoaderEchoRuntimeHost.java");
        require(Files.isRegularFile(bridgePath),
                "Ashfall live runtime bridge source must be present for Agent 5 bridge coverage: " + bridgePath);
        String source = Files.readString(bridgePath, StandardCharsets.UTF_8);
        List<String> requiredBridgeMethods = List.of(
                "grantItem",
                "removeItem",
                "updatePlayerState",
                "placeBlock",
                "updateWorldState",
                "placeStructure",
                "updateBlockEntity",
                "updateCapability",
                "updateMission",
                "emitEvent",
                "sendPacketHud",
                "writeSaveData",
                "deleteSaveData",
                "emitHud",
                "clientTick",
                "renderLayer",
                "screenEvent",
                "keybind",
                "registerCommand",
                "registerNetworkPacket",
                "reloadConfig",
                "reloadResources",
                "saveHook",
                "lifecyclePhase",
                "publishRuntimeEvent",
                "syncServerClient",
                "runtimeEvidence",
                "liveRuntimeAccessed",
                "minecraftRuntimeAccessed",
                "liveRuntimeMutationSupported"
        );
        List<String> requiredRuntimeSurfaceMethods = List.of(
                "clientTick",
                "renderLayer",
                "screenEvent",
                "keybind",
                "registerCommand",
                "registerNetworkPacket",
                "reloadConfig",
                "reloadResources",
                "saveHook",
                "lifecyclePhase",
                "publishRuntimeEvent",
                "syncServerClient"
        );
        for (String surface : REQUIRED_AGENT5_LIVE_PROOF_SURFACES) {
            require(source.contains("\"" + surface + "\""),
                    "Ashfall live runtime bridge must advertise surface: " + surface);
        }
        for (String method : requiredBridgeMethods) {
            require(source.contains("case \"" + method + "\"")
                            || source.contains("\"" + method + "\".equals(method.getName())"),
                    "Ashfall live runtime bridge must handle NativeLoaderLiveRuntimeBridge method: " + method);
        }
        require(source.contains("compatibilityDelegate.runtimeSurfaces()."),
                "Ashfall live runtime bridge must dispatch Agent 5 methods through EchoNativeRuntimeHost.RuntimeSurfaces.");
        require(source.contains("stampLiveDispatchProof"),
                "Ashfall live runtime bridge must stamp per-dispatch live proof onto Native Loader evidence maps.");
        require(source.contains("evidence.putAll(snapshot)"),
                "Ashfall live runtime bridge must copy Minecraft RuntimeSurfaces snapshot side-effect evidence into dispatch payloads.");
        require(source.contains("Object liveRuntimeDispatchId = evidence.get(\"liveRuntimeDispatchId\")")
                        && source.contains("evidence.put(\"liveRuntimeDispatchId\", liveRuntimeDispatchId)"),
                "Ashfall live runtime bridge must preserve Loader-issued dispatch ids across Minecraft snapshot merges.");
        require(source.contains("new LinkedHashMap<>(payload == null ? Map.of() : payload)"),
                "Ashfall live runtime bridge must copy runtime surface payloads before stamping bridge evidence requirements.");
        require(source.contains("\"bridgeRequiresRuntimeEventEvidence\""),
                "Ashfall live runtime bridge must stamp whether a runtime surface requires event evidence.");
        require(source.contains("\"bridgeRequiresRuntimePacketEvidence\""),
                "Ashfall live runtime bridge must stamp whether a runtime surface requires packet evidence.");
        require(source.contains("\"liveRuntimeDispatchProofSatisfied\""),
                "Ashfall live runtime bridge must expose per-dispatch proof satisfaction to subsystem hosts.");
        require(source.contains("\"liveRuntimeDispatchMinecraftAccessed\""),
                "Ashfall live runtime bridge must expose per-dispatch Minecraft access to subsystem hosts.");
        require(source.contains("liveRuntimeSurfaceEvidence"),
                "Ashfall live runtime bridge must expose cached per-surface evidence for direct runtime operations.");
        require(source.contains("beginLiveRuntimeSurfaceDispatch"),
                "Ashfall live runtime bridge must receive dispatch ids for direct runtime surface proof freshness.");
        require(source.contains("activeLiveSurfaceDispatchIds"),
                "Ashfall live runtime bridge must track active dispatch ids per surface.");
        require(source.contains("\"liveRuntimeDispatchId\""),
                "Ashfall live runtime bridge must stamp dispatch ids into per-surface evidence.");
        require(source.contains("\"save_data\".equals(surface) || \"save_hooks\".equals(surface)"),
                "Ashfall live runtime bridge must apply save lifecycle live proof checks to save_data and save_hooks.");
        require(source.contains("\"resource_reloads\".equals(surface)"),
                "Ashfall live runtime bridge must apply runtime resource save proof checks to resource_reloads.");
        require(source.contains("saveDataLiveProofSatisfied"),
                "Ashfall live runtime bridge must require explicit live save-data proof before accepting save lifecycle surfaces.");
        require(source.contains("resourceReloadLiveProofSatisfied"),
                "Ashfall live runtime bridge must require explicit runtime resource save proof before accepting resource reload surfaces.");
        require(source.contains("directSurfaceLiveProofSatisfied"),
                "Ashfall live runtime bridge must require concrete per-surface live proof before accepting runtime surface evidence.");
        require(source.contains("\"runtimeSurfaceSaveTouched\""),
                "Ashfall live runtime bridge must require resource reload save-touch evidence.");
        require(source.contains("\"runtimeSurfaceSaveMutated\""),
                "Ashfall live runtime bridge must require resource reload save-mutation evidence.");
        for (String directBridgeProofField : List.of(
                "\"runtimeInventoryMutated\"",
                "\"runtimePlayerStateMutated\"",
                "\"runtimeWorldBlockMutated\"",
                "\"runtimeStructureMutated\"",
                "\"runtimeBlockEntityMutated\"",
                "\"runtimeCapabilityMutated\"",
                "\"runtimeEventMutated\"",
                "\"runtimePacketMutated\"",
                "\"runtimeHudNotificationMutated\"",
                "\"runtimeMissionStateMutated\"",
                "\"runtimeServerClientSyncMutated\"")) {
            require(source.contains(directBridgeProofField),
                    "Ashfall live runtime bridge must enforce direct surface proof field: "
                            + directBridgeProofField.replace("\"", ""));
        }
        require(source.contains("\"runtimeSaveDataBackend\""),
                "Ashfall live runtime bridge must require the explicit save-data backend evidence field.");
        require(source.contains("\"world_save_file\""),
                "Ashfall live runtime bridge must require the live world-save backend for save_data proof.");

        Path adapterContractPath = resolveSourcePath("addons/echoadaptercore/src/main/java/com/knoxhack/echo/adaptercore/EchoNativeRuntimeHost.java");
        String adapterContractSource = Files.readString(adapterContractPath, StandardCharsets.UTF_8);
        require(adapterContractSource.contains("interface RuntimeSurfaces"),
                "AdapterCore runtime host contract must expose first-class RuntimeSurfaces.");
        for (String method : requiredRuntimeSurfaceMethods) {
            require(adapterContractSource.contains(" " + method + "("),
                    "AdapterCore RuntimeSurfaces contract must expose method: " + method);
        }
        Path attachedHostPath = resolveSourcePath("addons/echoadaptercore/src/main/java/com/knoxhack/echo/adaptercore/EchoNativeLoaderAttachedRuntimeHost.java");
        String attachedHostSource = Files.readString(attachedHostPath, StandardCharsets.UTF_8);
        for (String backendMethod : List.of(
                "\"clientTick\"",
                "\"renderLayer\"",
                "\"screenEvent\"",
                "\"keybind\"",
                "\"registerCommand\"",
                "\"registerNetworkPacket\"",
                "\"reloadConfig\"",
                "\"reloadResources\"",
                "\"saveHook\"",
                "\"lifecyclePhase\"",
                "\"publishRuntimeEvent\"",
                "\"syncServerClient\""
        )) {
            require(attachedHostSource.contains(backendMethod),
                    "AdapterCore attached runtime host must dispatch through Native Loader backend method: "
                            + backendMethod.replace("\"", ""));
        }
        require(attachedHostSource.contains("new LinkedHashMap<>(report)")
                        && (attachedHostSource.contains("NativeResult.mutated(\"Native Loader backend mutated state.\", snapshot)")
                        || attachedHostSource.contains("NativeResult.mutated(\"Native Loader backend mutated live state.\", snapshot)")),
                "AdapterCore attached runtime host must copy Native Loader mutation reports into NativeResult snapshots.");
        require(attachedHostSource.contains("snapshot.put(\"adapterCoreEnteredNativeLoaderBackend\", true)")
                        && attachedHostSource.contains("snapshot.put(\"nativeLoaderBackendClass\"")
                        && attachedHostSource.contains("snapshot.put(\"runtimeHostId\", runtimeHostId())"),
                "AdapterCore attached runtime host snapshots must prove AdapterCore entered the Native Loader backend.");
        require(attachedHostSource.contains("adapterCoreLiveProofSatisfied")
                        && attachedHostSource.contains("adapterCoreRejectedMirrorOnlyMutation")
                        && attachedHostSource.contains("requiredProofField")
                        && attachedHostSource.contains("\"runtimeCommandRegistryMutated\"")
                        && attachedHostSource.contains("\"runtimeNetworkChannelMutated\"")
                        && attachedHostSource.contains("\"runtimeLifecyclePhaseMutated\""),
                "AdapterCore attached runtime host must reject backend MUTATED status unless live Minecraft and subsystem proof fields are present.");
        require(attachedHostSource.contains("case \"sendPacketHud\" -> \"runtimePacketMutated\""),
                "AdapterCore attached runtime host must require direct packet/HUD runtime mutation proof, not packetSent-only proof.");
        require(attachedHostSource.contains("saveDataMutationProofSatisfied")
                        && attachedHostSource.contains("case \"writeSaveData\", \"deleteSaveData\" -> \"runtimeSaveDataTouched\"")
                        && attachedHostSource.contains("boolDeep(snapshot, \"liveSaveDataFileTouched\")")
                        && attachedHostSource.contains("boolDeep(snapshot, \"runtimeSaveDataMutated\")")
                        && attachedHostSource.contains("boolDeep(snapshot, \"runtimeSurfaceSaveMutated\")"),
                "AdapterCore attached runtime host must require live world-save file proof and save mutation proof for save-data backend mutations.");
        require(attachedHostSource.contains("case \"clientTick\" -> \"runtimeSurfaceSaveMutated\"")
                        && attachedHostSource.contains("case \"renderLayer\" -> \"runtimeSurfaceSaveMutated\"")
                        && attachedHostSource.contains("case \"screenEvent\" -> \"runtimeSurfaceSaveMutated\"")
                        && attachedHostSource.contains("case \"keybind\" -> \"runtimeSurfaceSaveMutated\""),
                "AdapterCore attached runtime host must require concrete runtime-surface save mutation proof for tick/render/screen/keybind.");
        requireSubsystemHostProofFieldGuards(
                "command",
                resolveSourcePath("echo-native-platform/echo-native-loader/src/main/java/dev/echo/nativeplatform/loader/NativeLoaderCommandHost.java"),
                List.of("\"runtimeCommandRegistryTouched\"", "\"runtimeCommandRegistryMutated\""));
        requireSubsystemHostProofFieldGuards(
                "network",
                resolveSourcePath("echo-native-platform/echo-native-loader/src/main/java/dev/echo/nativeplatform/loader/NativeLoaderNetworkHost.java"),
                List.of("\"runtimeNetworkChannelTouched\"", "\"runtimeNetworkChannelMutated\"", "\"runtimeNetworkPacketSent\""));
        requireSubsystemHostProofFieldGuards(
                "config",
                resolveSourcePath("echo-native-platform/echo-native-loader/src/main/java/dev/echo/nativeplatform/loader/NativeLoaderConfigHost.java"),
                List.of("\"runtimeConfigReloadTouched\"", "\"runtimeConfigReloadMutated\""));
        requireSubsystemHostProofFieldGuards(
                "lifecycle/event",
                resolveSourcePath("echo-native-platform/echo-native-loader/src/main/java/dev/echo/nativeplatform/loader/NativeLoaderLifecycleEventHost.java"),
                List.of(
                        "\"runtimeLifecyclePhaseTouched\"",
                        "\"runtimeLifecyclePhaseMutated\"",
                        "\"runtimeEventTouched\"",
                        "\"runtimeEventMutated\"",
                        "\"runtimeEventPublished\""));
        require(source.contains("updatePlayerCapability")
                        && source.contains("operation = \"write_state\"")
                        && source.contains("\"runtimeCapabilityTarget\""),
                "Ashfall live runtime bridge must mutate player-scoped capability targets, not only block capability positions.");

        Path minecraftHostPath = resolveSourcePath("addons/echoashfallprotocol/src/main/java/com/knoxhack/echoashfallprotocol/event/MinecraftEchoRuntimeHost.java");
        String minecraftHostSource = Files.readString(minecraftHostPath, StandardCharsets.UTF_8);
        require(minecraftHostSource.contains("private final class MinecraftRuntimeSurfaces implements RuntimeSurfaces"),
                "Ashfall Minecraft runtime host must implement first-class RuntimeSurfaces.");
        require(minecraftHostSource.contains("new NativeSaveData(\"native_loader_runtime_hooks\""),
                "Ashfall Minecraft RuntimeSurfaces must write through Minecraft save data.");
        require(minecraftHostSource.contains("events().publish("),
                "Ashfall Minecraft RuntimeSurfaces must publish through Minecraft events where required.");
        require(minecraftHostSource.contains("packets().sendToPlayer("),
                "Ashfall Minecraft RuntimeSurfaces must send through Minecraft packets where required.");
        require(minecraftHostSource.contains("\"runtimeSurfaceSaveTouched\""),
                "Ashfall Minecraft RuntimeSurfaces must return save-touch evidence in live mutation snapshots.");
        require(minecraftHostSource.contains("\"runtimeSurfaceEventPublished\""),
                "Ashfall Minecraft RuntimeSurfaces must return event-publication evidence in live mutation snapshots.");
        require(minecraftHostSource.contains("\"runtimeSurfacePacketSent\""),
                "Ashfall Minecraft RuntimeSurfaces must return packet-send evidence in live mutation snapshots.");
        for (String subsystemEvidenceField : List.of(
                "\"runtimeCommandRegistryTouched\"",
                "\"runtimeNetworkChannelTouched\"",
                "\"runtimeConfigReloadTouched\"",
                "\"runtimeResourceReloadTouched\"",
                "\"runtimeSaveHookTouched\"",
                "\"runtimeLifecyclePhaseTouched\"",
                "\"runtimeServerClientSyncTouched\"")) {
            require(minecraftHostSource.contains(subsystemEvidenceField),
                    "Ashfall Minecraft RuntimeSurfaces must stamp subsystem-specific live evidence field: "
                            + subsystemEvidenceField.replace("\"", ""));
        }
        require(minecraftHostSource.contains("\"runtimeSurfaceRequiredEventSatisfied\""),
                "Ashfall Minecraft RuntimeSurfaces must report whether required event evidence was satisfied.");
        require(minecraftHostSource.contains("\"runtimeSurfaceRequiredPacketSatisfied\""),
                "Ashfall Minecraft RuntimeSurfaces must report whether required packet evidence was satisfied.");
        require(minecraftHostSource.contains("\"runtimeSurfaceLiveProofSatisfied\""),
                "Ashfall Minecraft RuntimeSurfaces must report exact live-proof satisfaction.");
        require(minecraftHostSource.contains("\"runtimeHudNotificationPublished\""),
                "Ashfall Minecraft HUD runtime path must expose HUD notification publication evidence.");
        require(minecraftHostSource.contains("\"runtimeHudNotificationMutated\""),
                "Ashfall Minecraft HUD runtime path must expose HUD notification mutation evidence.");
        require(minecraftHostSource.contains("\"runtimeInventoryTouched\""),
                "Ashfall Minecraft inventory runtime path must expose inventory touch evidence.");
        require(minecraftHostSource.contains("\"runtimeInventoryMutated\""),
                "Ashfall Minecraft inventory runtime path must expose inventory mutation evidence.");
        require(minecraftHostSource.contains("\"runtimePlayerStateTouched\""),
                "Ashfall Minecraft player-state runtime path must expose player-state touch evidence.");
        require(minecraftHostSource.contains("\"runtimePlayerStateMutated\""),
                "Ashfall Minecraft player-state runtime path must expose player-state mutation evidence.");
        require(minecraftHostSource.contains("\"runtimeMissionStateTouched\""),
                "Ashfall Minecraft mission runtime path must expose mission-state touch evidence.");
        require(minecraftHostSource.contains("\"runtimeMissionStateMutated\""),
                "Ashfall Minecraft mission runtime path must expose mission-state mutation evidence.");
        require(minecraftHostSource.contains("\"runtimeWorldBlockTouched\""),
                "Ashfall Minecraft world-block runtime path must expose block-state touch evidence.");
        require(minecraftHostSource.contains("\"runtimeWorldBlockMutated\""),
                "Ashfall Minecraft world-block runtime path must expose block-state mutation evidence.");
        require(minecraftHostSource.contains("\"runtimeStructurePlaced\""),
                "Ashfall Minecraft structure runtime path must expose structure placement evidence.");
        require(minecraftHostSource.contains("\"runtimeStructureMutated\""),
                "Ashfall Minecraft structure runtime path must expose structure mutation evidence.");
        require(minecraftHostSource.contains("\"runtimeBlockEntityTouched\""),
                "Ashfall Minecraft block-entity runtime path must expose block-entity touch evidence.");
        require(minecraftHostSource.contains("\"runtimeBlockEntityMutated\""),
                "Ashfall Minecraft block-entity runtime path must expose block-entity mutation evidence.");
        require(minecraftHostSource.contains("\"runtimeCapabilityTouched\""),
                "Ashfall Minecraft capability runtime path must expose capability touch evidence.");
        require(minecraftHostSource.contains("\"runtimeCapabilityMutated\""),
                "Ashfall Minecraft capability runtime path must expose capability mutation evidence.");
        require(minecraftHostSource.contains("boolean mutated = saveTouched && requiredEventSatisfied && requiredPacketSatisfied"),
                "Ashfall Minecraft RuntimeSurfaces must require save plus required event/packet evidence for mutation.");
        require(minecraftHostSource.contains("FAILED_REQUIRED_RUNTIME_SURFACE_EVIDENCE"),
                "Ashfall Minecraft RuntimeSurfaces must fail when required event/packet evidence is missing.");
        require(minecraftHostSource.contains("\"runtimeSaveDataTouched\""),
                "Ashfall Minecraft SaveData must return explicit live save-data touch evidence.");
        require(minecraftHostSource.contains("\"liveSaveDataFileTouched\""),
                "Ashfall Minecraft SaveData must return explicit world save-file touch evidence.");
        require(minecraftHostSource.contains("\"runtimeSaveDataBackend\", \"world_save_file\""),
                "Ashfall Minecraft SaveData must identify the live world save backend.");

        Map<String, Object> coverage = new LinkedHashMap<>();
        coverage.put("bridgePath", bridgePath.toString());
        coverage.put("adapterContractPath", adapterContractPath.toString());
        coverage.put("attachedHostPath", attachedHostPath.toString());
        coverage.put("minecraftRuntimeHostPath", minecraftHostPath.toString());
        coverage.put("requiredSurfaces", REQUIRED_AGENT5_LIVE_PROOF_SURFACES);
        coverage.put("requiredBridgeMethods", requiredBridgeMethods);
        coverage.put("requiredRuntimeSurfaceMethods", requiredRuntimeSurfaceMethods);
        coverage.put("adapterCoreRuntimeSurfacesContract", true);
        coverage.put("adapterCoreAttachedHostDispatchesRuntimeSurfaces", true);
        coverage.put("ashfallBridgeDelegatesToRuntimeSurfaces", true);
        coverage.put("ashfallBridgeStampsRuntimeSurfaceEvidenceRequirements", true);
        coverage.put("minecraftSaveMutationPath", true);
        coverage.put("minecraftEventMutationPath", true);
        coverage.put("minecraftPacketMutationPath", true);
        coverage.put("minecraftRuntimeSurfaceSnapshotEvidence", List.of(
                "runtimeSurfaceSaveTouched",
                "runtimeSurfaceEventPublished",
                "runtimeSurfacePacketSent",
                "runtimeSurfaceRequiredEventSatisfied",
                "runtimeSurfaceRequiredPacketSatisfied",
                "runtimeSurfaceLiveProofSatisfied"));
        coverage.put("minecraftDirectSaveDataSnapshotEvidence", List.of(
                "runtimeSaveDataTouched",
                "liveSaveDataFileTouched",
                "runtimeSaveDataBackend=world_save_file"));
        return Map.copyOf(coverage);
    }

    private static Map<String, Object> subsystemHostProofCoverage() throws IOException {
        Map<String, String> hostPaths = Map.of(
                "command", "echo-native-platform/echo-native-loader/src/main/java/dev/echo/nativeplatform/loader/NativeLoaderCommandHost.java",
                "network", "echo-native-platform/echo-native-loader/src/main/java/dev/echo/nativeplatform/loader/NativeLoaderNetworkHost.java",
                "config", "echo-native-platform/echo-native-loader/src/main/java/dev/echo/nativeplatform/loader/NativeLoaderConfigHost.java",
                "lifecycleEvent", "echo-native-platform/echo-native-loader/src/main/java/dev/echo/nativeplatform/loader/NativeLoaderLifecycleEventHost.java"
        );
        Map<String, Object> coverage = new LinkedHashMap<>();
        for (Map.Entry<String, String> host : hostPaths.entrySet()) {
            Path path = resolveSourcePath(host.getValue());
            String source = Files.readString(path, StandardCharsets.UTF_8);
            require(source.contains("boolean liveRuntimeReleaseProofSatisfied()"),
                    "Subsystem host must expose live runtime release proof predicate: " + host.getKey());
            require(source.contains("liveRuntimeBridge.minecraftRuntimeAccessed()"),
                    "Subsystem host release proof predicate must require Minecraft runtime access: " + host.getKey());
            require(source.contains("report.put(\"liveRuntimeReleaseProofSatisfied\", liveRuntimeReleaseProofSatisfied())"),
                    "Subsystem host report must include live runtime release proof predicate: " + host.getKey());
            require(source.contains("boolean liveRuntimeMutationCoverageSatisfied()"),
                    "Subsystem host must expose all-entry live mutation coverage predicate: " + host.getKey());
            require(source.contains("report.put(\"liveRuntimeMutationCoverageSatisfied\", liveRuntimeMutationCoverageSatisfied())"),
                    "Subsystem host report must include all-entry live mutation coverage: " + host.getKey());
            require(source.contains("&& liveRuntimeMutationCoverageSatisfied()"),
                    "Subsystem host release proof must require all-entry live mutation coverage: " + host.getKey());
            if ("lifecycleEvent".equals(host.getKey())) {
                require(source.contains("liveRuntimeEntryMutationProofCount()")
                                && source.contains("entryHasLiveRuntimeMutationProof")
                                && source.contains("report.put(\"liveRuntimeDispatchProofEntryCount\", liveRuntimeEntryMutationProofCount())")
                                && source.contains("report.put(\"liveRuntimeUnprovedDispatchEntryCount\""),
                        "Lifecycle/event host coverage must be tied to entry-level live dispatch proof, not raw mutation counters.");
            }
            require(source.contains("return status == EchoNativeLoadStatus.MUTATED\r\n                && liveRuntimeBridge.liveRuntimeAccessed()")
                            || source.contains("return status == EchoNativeLoadStatus.MUTATED\n                && liveRuntimeBridge.liveRuntimeAccessed()"),
                    "Subsystem host per-dispatch proof must require live runtime access before counting mutation: "
                            + host.getKey());
            require(source.contains("report.put(\"partialLiveMinecraftMutation\""),
                    "Subsystem host report must expose partial live Minecraft mutation separately: " + host.getKey());
            require(source.contains("report.put(\"liveMinecraftMutation\", liveRuntimeReleaseProofSatisfied())"),
                    "Subsystem host report must reserve liveMinecraftMutation for release-grade proof: " + host.getKey());
            require(source.contains("clearLiveDispatchProof"),
                    "Subsystem host must clear caller-supplied live dispatch proof before bridge dispatch: " + host.getKey());
            require(source.contains("liveRuntimeDispatchProofSatisfied"),
                    "Subsystem host must require bridge-stamped per-dispatch live proof: " + host.getKey());
            require(source.contains("liveRuntimeDispatchMinecraftAccessed"),
                    "Subsystem host must require per-dispatch Minecraft access evidence: " + host.getKey());
            require(source.contains("subsystemLiveRuntimeDispatchProofSatisfied"),
                    "Subsystem host must report whether each entry had bridge-stamped dispatch proof: " + host.getKey());
            require(source.contains("report.put(\"liveRuntimeDispatchId\", string(safeEvidence.get(\"liveRuntimeDispatchId\")))"),
                    "Subsystem host entry reports must expose bridge-stamped dispatch id at top level: "
                            + host.getKey());
            require(source.contains("report.put(\"liveRuntimeSurface\", string(safeEvidence.get(\"liveRuntimeSurface\")))"),
                    "Subsystem host entry reports must expose bridge-stamped runtime surface at top level: "
                            + host.getKey());
            require(source.contains("report.put(\"subsystemLiveRuntimeDispatchProofSatisfied\",\r\n                    Boolean.TRUE.equals(safeEvidence.get(\"subsystemLiveRuntimeDispatchProofSatisfied\")))")
                            || source.contains("report.put(\"subsystemLiveRuntimeDispatchProofSatisfied\",\n                    Boolean.TRUE.equals(safeEvidence.get(\"subsystemLiveRuntimeDispatchProofSatisfied\")))"),
                    "Subsystem host entry reports must expose bridge-stamped dispatch proof status at top level: "
                            + host.getKey());
            require(source.contains("report.put(\"liveMinecraftMutation\", Boolean.TRUE.equals(safeEvidence.get(\"liveMinecraftMutation\")))"),
                    "Subsystem host entry reports must expose live Minecraft mutation evidence at top level: "
                            + host.getKey());
            require(source.contains("report.put(\"minecraftRuntimeAccessed\", Boolean.TRUE.equals(safeEvidence.get(\"minecraftRuntimeAccessed\")))"),
                    "Subsystem host entry reports must expose Minecraft runtime access evidence at top level: "
                            + host.getKey());
            require(source.contains("beginLiveRuntimeSurfaceDispatch"),
                    "Subsystem host must expose current dispatch ids to the live bridge: " + host.getKey());
            require(source.contains("liveRuntimeDispatchId"),
                    "Subsystem host must require dispatch id correlation for live proof: " + host.getKey());
            require(source.contains("dispatchId.equals"),
                    "Subsystem host must reject stale or mismatched dispatch ids: " + host.getKey());
            require(source.contains("liveRuntimeSurfaceMatches"),
                    "Subsystem host must require live proof to name the expected subsystem surface: " + host.getKey());
            require(source.contains("\"liveRuntimeSurface\""),
                    "Subsystem host must inspect liveRuntimeSurface dispatch evidence: " + host.getKey());
            require(source.contains("evidence.remove(\"liveRuntimeSurface\")"),
                    "Subsystem host must clear caller-supplied liveRuntimeSurface before bridge dispatch: " + host.getKey());
            require(source.contains("subsystemRuntimeSideEffectSatisfied"),
                    "Subsystem host must require live runtime side-effect evidence beyond generic dispatch proof: "
                            + host.getKey());
            require(source.contains("\"runtimeSurfaceSaveTouched\""),
                    "Subsystem host must require runtime surface save-touch evidence: " + host.getKey());
            require(source.contains("\"runtimeSurfaceSaveMutated\""),
                    "Subsystem host must require runtime surface save-mutation evidence: " + host.getKey());
            require(source.contains("\"runtimeSaveDataTouched\""),
                    "Subsystem host must require runtime save-data touch evidence: " + host.getKey());
            require(source.contains("\"liveSaveDataFileTouched\""),
                    "Subsystem host must require live save-file touch evidence: " + host.getKey());
            require(source.contains("\"runtimeSaveDataBackend\"")
                            && source.contains("\"world_save_file\""),
                    "Subsystem host must require live world-save backend evidence: " + host.getKey());
            require(source.contains("saveFile") && source.contains("!saveFile.isBlank()"),
                    "Subsystem host must require a nonblank save-file path for live proof: " + host.getKey());
            require(source.contains("evidence.remove(\"runtimeSurfaceSaveTouched\")"),
                    "Subsystem host must clear caller-supplied runtime side-effect proof before bridge dispatch: "
                            + host.getKey());
            require(source.contains("evidence.remove(\"runtimeSurfaceSaveMutated\")")
                            && source.contains("evidence.remove(\"runtimeSaveDataTouched\")")
                            && source.contains("evidence.remove(\"runtimeSaveDataMutated\")")
                            && source.contains("evidence.remove(\"liveSaveDataFileTouched\")")
                            && source.contains("evidence.remove(\"runtimeSaveDataBackend\")")
                            && source.contains("evidence.remove(\"saveFile\")"),
                    "Subsystem host must clear caller-supplied world-save side-effect proof before bridge dispatch: "
                            + host.getKey());
            if ("network".equals(host.getKey())) {
                require(source.contains("\"runtimeSurfacePacketSent\""),
                        "Network host must require packet-send evidence for network channel proof.");
                require(source.contains("\"runtimeSurfacePacketMutated\""),
                        "Network host must require packet-mutation evidence for network channel proof.");
                require(source.contains("evidence.remove(\"runtimeSurfacePacketSent\")")
                                && source.contains("evidence.remove(\"runtimeSurfacePacketMutated\")"),
                        "Network host must clear caller-supplied packet side-effect proof before bridge dispatch.");
            }
            if ("lifecycleEvent".equals(host.getKey())) {
                require(source.contains("\"runtimeSurfaceEventPublished\""),
                        "Lifecycle/event host must require event-publish evidence for runtime event proof.");
                require(source.contains("\"runtimeSurfaceEventMutated\""),
                        "Lifecycle/event host must require event-mutation evidence for runtime event proof.");
                require(source.contains("Map.copyOf(livePayloadEvidence)")
                                && source.contains("report.put(\"liveRuntimeEvidence\""),
                        "Lifecycle/event host must report bridge-stamped runtime event evidence per published event.");
                require(source.contains("Map<String, Object> safeLiveEvidence")
                                && source.contains("report.put(\"liveRuntimeDispatchId\", string(safeLiveEvidence.get(\"liveRuntimeDispatchId\")))")
                                && source.contains("report.put(\"liveRuntimeSurface\", string(safeLiveEvidence.get(\"liveRuntimeSurface\")))")
                                && source.contains("Boolean.TRUE.equals(safeLiveEvidence.get(\"subsystemLiveRuntimeDispatchProofSatisfied\"))"),
                        "Lifecycle/event published event reports must expose bridge-stamped runtime proof fields at top level.");
                require(source.contains("evidence.remove(\"runtimeSurfaceEventPublished\")")
                                && source.contains("evidence.remove(\"runtimeSurfaceEventMutated\")"),
                        "Lifecycle/event host must clear caller-supplied event side-effect proof before bridge dispatch.");
            }
            coverage.put(host.getKey(), Map.of(
                    "path", path.toString(),
                    "requiresMinecraftRuntimeAccess", true,
                    "reportsLiveRuntimeReleaseProofSatisfied", true,
                    "requiresAllEntryLiveMutationCoverage", true,
                    "requiresPerDispatchProof", true,
                    "requiresRuntimeSideEffectProof", true,
                    "separatesPartialLiveMinecraftMutation", true,
                    "requiresDispatchIdCorrelation", true,
                    "requiresSubsystemSurfaceCorrelation", true
            ));
        }
        return Map.copyOf(coverage);
    }

    private static Map<String, Object> resourceHostProofSeparation() throws IOException {
        Path path = resolveSourcePath("echo-native-platform/echo-native-loader/src/main/java/dev/echo/nativeplatform/loader/NativeLoaderResourceHost.java");
        String source = Files.readString(path, StandardCharsets.UTF_8);
        require(source.contains("mountedEvidence.put(\"nativeResourceMountProven\", true)"),
                "Resource host must expose native resource mount proof separately from live Minecraft mutation.");
        require(source.contains("mountedEvidence.put(\"liveMinecraftMutation\", false)"),
                "Pre-world resource mounts must not be reported as live Minecraft mutation.");
        require(source.contains("report.put(\"nativeResourceMountProven\", preWorldCreationResourceCount() > 0)"),
                "Resource host report must expose native resource mount proof.");
        require(source.contains("report.put(\"liveMinecraftMutation\", false)"),
                "Resource host aggregate report must not turn filesystem mounts into live Minecraft mutation.");
        return Map.of(
                "path", path.toString(),
                "resourceMountProofSeparatedFromLiveMinecraftMutation", true
        );
    }

    private static Map<String, Object> runtimeHostProofCoverage() throws IOException {
        Path runtimeHostPath = resolveSourcePath("echo-native-platform/echo-native-loader/src/main/java/dev/echo/nativeplatform/loader/NativeLoaderRuntimeHost.java");
        String source = Files.readString(runtimeHostPath, StandardCharsets.UTF_8);
        require(source.contains("boolean liveRuntimeSurfaceMutationCoverageSatisfied()"),
                "Runtime host must expose live surface mutation coverage predicate.");
        require(source.contains("List<String> liveRuntimeUnbridgedMutatedSurfaces()"),
                "Runtime host must report unbridged native mutated surfaces.");
        require(source.contains("releaseProofMutatedSurfaceNames()"),
                "Runtime host must separate diagnostic mutations from release-proof mutated surfaces.");
        require(source.contains("filter(NativeLoaderRuntimeHost::releaseProofSurface)"),
                "Runtime host release proof must filter diagnostic-only surfaces.");
        require(source.contains("return !\"feedback\".equals(surface);"),
                "Runtime host feedback mutations must remain diagnostic-only for release proof.");
        require(source.contains("liveSurfaceForNativeSurface"),
                "Runtime host must map native mirror surface names to live bridge surface names.");
        require(source.contains("&& liveRuntimeSurfaceMutationCoverageSatisfied()"),
                "Runtime host release proof must require all mutated native surfaces to be live-bridged.");
        require(source.contains("report.put(\"liveRuntimeSurfaceMutationCoverageSatisfied\", liveRuntimeSurfaceMutationCoverageSatisfied())"),
                "Runtime host report must include live surface mutation coverage.");
        require(source.contains("report.put(\"liveRuntimeUnbridgedMutatedSurfaces\", liveRuntimeUnbridgedMutatedSurfaces())"),
                "Runtime host report must include unbridged mutated surfaces.");
        require(source.contains("liveRuntimeBridgeProofBySurface"),
                "Runtime host must track per-dispatch proof for evidence-carrying direct surfaces.");
        require(source.contains("liveRuntimeBridgeProofEvidenceBySurface")
                        && source.contains("report.put(\"liveRuntimeBridgeProofEvidenceBySurface\"")
                        && source.contains("copyNestedEvidence(liveRuntimeBridgeProofEvidenceBySurface)"),
                "Runtime host must report concrete bridge-stamped proof evidence per direct runtime surface.");
        require(source.contains("recordLiveRuntimeBridgeProof(surface, EchoNativeLoadStatus.UNSUPPORTED, \"\", dispatchEvidence, Map.of())"),
                "Runtime host must clear stale surface proof when no live bridge is attached.");
        require(source.contains("recordLiveRuntimeBridgeProof(surface, EchoNativeLoadStatus.FAILED, dispatchId, dispatchEvidence, Map.of())"),
                "Runtime host must clear stale surface proof when live dispatch fails before mirror fallback.");
        require(source.contains("directDispatchProofRequired"),
                "Runtime host must name direct surfaces that require bridge-stamped dispatch proof.");
        require(source.contains("clearLiveDispatchProof"),
                "Runtime host must clear caller-supplied direct dispatch proof before bridge dispatch.");
        require(source.contains("evidence.remove(\"liveRuntimeSurface\")"),
                "Runtime host must clear caller-supplied liveRuntimeSurface before direct bridge dispatch.");
        require(source.contains("evidence.remove(\"runtimeSaveDataMutated\")"),
                "Runtime host must clear caller-supplied save mutation proof before direct bridge dispatch.");
        require(source.contains("liveRuntimeDispatchProofSatisfied"),
                "Runtime host must require bridge-stamped direct dispatch proof.");
        require(source.contains("return status == EchoNativeLoadStatus.MUTATED\r\n                && context.liveRuntimeBridge().liveRuntimeAccessed()")
                        || source.contains("return status == EchoNativeLoadStatus.MUTATED\n                && context.liveRuntimeBridge().liveRuntimeAccessed()"),
                "Runtime host direct dispatch proof must require live runtime access before accepting per-surface proof.");
        require(source.contains("beginLiveRuntimeSurfaceDispatch"),
                "Runtime host must start each direct live runtime surface dispatch with a bridge-visible dispatch id.");
        require(source.contains("liveRuntimeDispatchId"),
                "Runtime host direct dispatch proof must carry a dispatch correlation id.");
        require(source.contains("dispatchId.equals"),
                "Runtime host direct dispatch proof must require bridge evidence to match the current dispatch id.");
        require(source.contains("directSurfaceEvidenceMatches"),
                "Runtime host direct dispatch proof must reject mismatched bridge surface evidence.");
        require(source.contains("\"liveRuntimeSurface\""),
                "Runtime host direct dispatch proof must inspect liveRuntimeSurface when bridge evidence provides it.");
        require(source.contains("!evidence.containsKey(\"liveRuntimeSurface\")"),
                "Runtime host direct dispatch proof must require explicit liveRuntimeSurface evidence.");
        require(source.contains("!actual.isBlank() && actual.equals"),
                "Runtime host direct dispatch proof must reject blank liveRuntimeSurface evidence.");
        require(source.contains("directSaveDataEvidenceSatisfied"),
                "Runtime host direct save_data/save_hooks/world_state proof must require explicit save lifecycle evidence.");
        require(source.contains("\"world_state\".equals(surface)"),
                "Runtime host direct world_state proof must include world_state.");
        require(source.contains("directResourceReloadEvidenceSatisfied"),
                "Runtime host direct resource_reloads proof must require explicit runtime resource save evidence.");
        require(source.contains("directPacketSurfaceEvidenceSatisfied"),
                "Runtime host direct packet-oriented proof must require explicit packet-send evidence.");
        require(source.contains("\"packets_hud\".equals(surface)")
                        && source.contains("\"runtimePacketSent\"")
                        && source.contains("\"runtimePacketMutated\"")
                        && source.contains("\"server_client_sync\".equals(surface)")
                        && source.contains("\"runtimeServerClientSyncPacketSent\"")
                        && source.contains("\"runtimeServerClientSyncMutated\""),
                "Runtime host direct packet proof must split packets_hud packet evidence from server_client_sync runtime-surface evidence.");
        require(source.contains("directEventSurfaceEvidenceSatisfied"),
                "Runtime host direct event proof must require explicit runtime event publication evidence.");
        require(source.contains("\"events\".equals(surface)")
                        && source.contains("\"runtimeEventTouched\"")
                        && source.contains("\"runtimeEventPublished\"")
                        && source.contains("\"runtimeEventMutated\""),
                "Runtime host direct event proof must include events and explicit runtime event evidence.");
        require(source.contains("directClientSurfaceSaveEvidenceSatisfied"),
                "Runtime host direct client-surface proof must require runtime-surface save evidence.");
        require(source.contains("List.of(\"client_tick\", \"render_layers\", \"screen_events\", \"keybinds\")"),
                "Runtime host direct client-surface proof must include tick/render/screen/keybind surfaces.");
        require(source.contains("directHudSurfaceEvidenceSatisfied"),
                "Runtime host direct HUD proof must require explicit HUD notification evidence.");
        require(source.contains("\"hud\".equals(surface)"),
                "Runtime host direct HUD proof must include hud.");
        require(source.contains("directInventorySurfaceEvidenceSatisfied"),
                "Runtime host direct inventory proof must require explicit inventory evidence.");
        require(source.contains("\"inventory\".equals(surface)"),
                "Runtime host direct inventory proof must include inventory.");
        require(source.contains("directPlayerStateSurfaceEvidenceSatisfied"),
                "Runtime host direct player_state proof must require explicit player-state evidence.");
        require(source.contains("\"player_state\".equals(surface)"),
                "Runtime host direct player_state proof must include player_state.");
        require(source.contains("directMissionSurfaceEvidenceSatisfied"),
                "Runtime host direct missions proof must require explicit mission evidence.");
        require(source.contains("\"missions\".equals(surface)"),
                "Runtime host direct missions proof must include missions.");
        require(source.contains("directWorldBlockSurfaceEvidenceSatisfied"),
                "Runtime host direct world_blocks proof must require explicit world-block evidence.");
        require(source.contains("\"world_blocks\".equals(surface)"),
                "Runtime host direct world_blocks proof must include world_blocks.");
        require(source.contains("directStructureSurfaceEvidenceSatisfied"),
                "Runtime host direct structures proof must require explicit structure evidence.");
        require(source.contains("\"structures\".equals(surface)"),
                "Runtime host direct structures proof must include structures.");
        require(source.contains("directBlockEntitySurfaceEvidenceSatisfied"),
                "Runtime host direct block_entities proof must require explicit block-entity evidence.");
        require(source.contains("\"block_entities\".equals(surface)"),
                "Runtime host direct block_entities proof must include block_entities.");
        require(source.contains("directCapabilitySurfaceEvidenceSatisfied"),
                "Runtime host direct capabilities proof must require explicit capability evidence.");
        require(source.contains("\"capabilities\".equals(surface)"),
                "Runtime host direct capabilities proof must include capabilities.");
        require(source.contains("\"resource_reloads\""),
                "Runtime host direct resource reload proof must include resource_reloads.");
        require(source.contains("\"server_client_sync\""),
                "Runtime host direct server/client sync proof must include server_client_sync.");
        require(source.contains("\"runtimeSurfaceSaveTouched\""),
                "Runtime host direct resource reload proof must require runtime surface save touch evidence.");
        require(source.contains("\"runtimeSurfaceSaveMutated\""),
                "Runtime host direct resource reload proof must require runtime surface save mutation evidence.");
        require(source.contains("\"save_hooks\""),
                "Runtime host direct save lifecycle proof must include save_hooks.");
        require(source.contains("\"runtimeSaveDataTouched\""),
                "Runtime host direct save lifecycle proof must require save-data touch evidence.");
        require(source.contains("\"liveSaveDataFileTouched\""),
                "Runtime host direct save lifecycle proof must require live world-save file evidence.");
        require(source.contains("\"runtimeSaveDataBackend\""),
                "Runtime host direct save lifecycle proof must require backend identity evidence.");
        require(source.contains("\"world_save_file\""),
                "Runtime host direct save lifecycle proof must require the live world-save backend.");
        require(source.contains("evidence.remove(\"runtimeSaveDataTouched\")"),
                "Runtime host must clear caller-supplied save-data proof before direct bridge dispatch.");
        require(source.contains("evidence.remove(\"runtimeSurfaceSaveTouched\")"),
                "Runtime host must clear caller-supplied resource reload save proof before direct bridge dispatch.");
        require(source.contains("evidence.remove(\"runtimeSurfacePacketSent\")"),
                "Runtime host must clear caller-supplied packet-send proof before direct bridge dispatch.");
        require(source.contains("evidence.remove(\"runtimeSurfacePacketMutated\")"),
                "Runtime host must clear caller-supplied packet-mutation proof before direct bridge dispatch.");
        require(source.contains("evidence.remove(\"runtimeSurfaceEventPublished\")"),
                "Runtime host must clear caller-supplied event-publication proof before direct bridge dispatch.");
        require(source.contains("evidence.remove(\"runtimeSurfaceEventMutated\")"),
                "Runtime host must clear caller-supplied event-mutation proof before direct bridge dispatch.");
        require(source.contains("evidence.remove(\"runtimeHudNotificationPublished\")"),
                "Runtime host must clear caller-supplied HUD publication proof before direct bridge dispatch.");
        require(source.contains("evidence.remove(\"runtimeHudNotificationMutated\")"),
                "Runtime host must clear caller-supplied HUD mutation proof before direct bridge dispatch.");
        require(source.contains("evidence.remove(\"runtimeInventoryTouched\")"),
                "Runtime host must clear caller-supplied inventory touch proof before direct bridge dispatch.");
        require(source.contains("evidence.remove(\"runtimeInventoryMutated\")"),
                "Runtime host must clear caller-supplied inventory mutation proof before direct bridge dispatch.");
        require(source.contains("evidence.remove(\"runtimePlayerStateTouched\")"),
                "Runtime host must clear caller-supplied player-state touch proof before direct bridge dispatch.");
        require(source.contains("evidence.remove(\"runtimePlayerStateMutated\")"),
                "Runtime host must clear caller-supplied player-state mutation proof before direct bridge dispatch.");
        require(source.contains("evidence.remove(\"runtimeMissionStateTouched\")"),
                "Runtime host must clear caller-supplied mission touch proof before direct bridge dispatch.");
        require(source.contains("evidence.remove(\"runtimeMissionStateMutated\")"),
                "Runtime host must clear caller-supplied mission mutation proof before direct bridge dispatch.");
        require(source.contains("evidence.remove(\"runtimeWorldBlockTouched\")"),
                "Runtime host must clear caller-supplied world-block touch proof before direct bridge dispatch.");
        require(source.contains("evidence.remove(\"runtimeWorldBlockMutated\")"),
                "Runtime host must clear caller-supplied world-block mutation proof before direct bridge dispatch.");
        require(source.contains("evidence.remove(\"runtimeStructurePlaced\")"),
                "Runtime host must clear caller-supplied structure placement proof before direct bridge dispatch.");
        require(source.contains("evidence.remove(\"runtimeStructureMutated\")"),
                "Runtime host must clear caller-supplied structure mutation proof before direct bridge dispatch.");
        require(source.contains("evidence.remove(\"runtimeBlockEntityTouched\")"),
                "Runtime host must clear caller-supplied block-entity touch proof before direct bridge dispatch.");
        require(source.contains("evidence.remove(\"runtimeBlockEntityMutated\")"),
                "Runtime host must clear caller-supplied block-entity mutation proof before direct bridge dispatch.");
        require(source.contains("evidence.remove(\"runtimeCapabilityTouched\")"),
                "Runtime host must clear caller-supplied capability touch proof before direct bridge dispatch.");
        require(source.contains("evidence.remove(\"runtimeCapabilityMutated\")"),
                "Runtime host must clear caller-supplied capability mutation proof before direct bridge dispatch.");
        Path liveBridgePath = resolveSourcePath("echo-native-platform/echo-native-loader/src/main/java/dev/echo/nativeplatform/loader/NativeLoaderLiveRuntimeBridge.java");
        String liveBridgeSource = Files.readString(liveBridgePath, StandardCharsets.UTF_8);
        require(liveBridgeSource.contains("liveRuntimeSurfaceEvidence"),
                "Live runtime bridge contract must expose per-surface evidence for no-argument direct mutations.");
        require(liveBridgeSource.contains("beginLiveRuntimeSurfaceDispatch"),
                "Live runtime bridge contract must expose dispatch-start correlation for per-surface evidence.");
        require(liveBridgeSource.contains("default boolean liveRuntimeAccessed()")
                        && liveBridgeSource.contains("return false;"),
                "Live runtime bridge contract must not treat attachment alone as live runtime access.");
        Path defaultProviderPath = resolveSourcePath("echo-native-platform/echo-native-loader/src/main/java/dev/echo/nativeplatform/loader/NativeLoaderDefaultProductBridgeProvider.java");
        String defaultProviderSource = Files.readString(defaultProviderPath, StandardCharsets.UTF_8);
        require(defaultProviderSource.contains("public boolean liveRuntimeAccessed()")
                        && defaultProviderSource.contains("return true;"),
                "Default product runtime bridge must explicitly declare native runtime access after the contract default was tightened.");
        for (String surface : List.of(
                "inventory",
                "player_state",
                "world_blocks",
                "world_state",
                "structures",
                "block_entities",
                "capabilities",
                "events",
                "packets_hud",
                "hud",
                "save_data",
                "missions",
                "client_tick",
                "render_layers",
                "screen_events",
                "keybinds",
                "commands",
                "network_channels",
                "config_reloads",
                "resource_reloads",
                "save_hooks",
                "lifecycle_phases",
                "server_client_sync")) {
            require(defaultProviderSource.contains("\"" + surface + "\""),
                    "Default product runtime bridge must advertise implemented Agent 5 surface: " + surface);
        }
        Map<String, Object> coverage = new LinkedHashMap<>();
        coverage.put("path", runtimeHostPath.toString());
        coverage.put("liveBridgePath", liveBridgePath.toString());
        coverage.put("defaultProviderPath", defaultProviderPath.toString());
        coverage.put("requiresAllMutatedSurfacesLiveBridged", true);
        coverage.put("reportsUnbridgedMutatedSurfaces", true);
        coverage.put("requiresDirectDispatchProofForEvidenceSurfaces", true);
        coverage.put("clearsCallerSeededDirectSurfaceEvidence", true);
        coverage.put("requiresDirectDispatchIdCorrelation", true);
        coverage.put("rejectsMismatchedDirectSurfaceEvidence", true);
        coverage.put("requiresExplicitDirectSurfaceEvidence", true);
        coverage.put("reportsDirectProofEvidenceBySurface", true);
        coverage.put("requiresExplicitDirectSaveDataEvidence", true);
        coverage.put("liveRuntimeAccessDefaultRequiresExplicitOverride", true);
        coverage.put("defaultProviderExplicitNativeRuntimeAccess", true);
        coverage.put("defaultProviderAdvertisesAgent5RuntimeSurfaces", true);
        return Map.copyOf(coverage);
    }

    private static Map<String, Object> adapterCoreLedgerProofCoverage() throws IOException {
        Path ledgerPath = resolveSourcePath("echo-native-platform/echo-native-loader/src/main/java/dev/echo/nativeplatform/loader/NativeLoaderMutationLedger.java");
        String ledgerSource = Files.readString(ledgerPath, StandardCharsets.UTF_8);
        require(ledgerSource.contains("liveRuntimeSurfaceMutationSatisfied"),
                "AdapterCore mutation ledger must carry per-surface live runtime proof.");
        require(ledgerSource.contains("record.liveRuntimeSurfaceMutationSatisfied()"),
                "AdapterCore live proof record count must require per-surface live runtime proof.");
        require(ledgerSource.contains("evidenceSurfaceMatches(surface, runtimeEvidence)"),
                "AdapterCore mutation ledger must scope explicit surface proof to the recorded AdapterCore surface.");
        require(ledgerSource.contains("surfaceReleaseProofSatisfied"),
                "AdapterCore mutation ledger must derive record-level release proof from scoped surface proof.");
        require(ledgerSource.contains("private static boolean liveRuntimeReleaseProofSatisfied(String surface"),
                "AdapterCore mutation ledger must expose a scoped release-proof predicate.");
        require(ledgerSource.contains("bool(runtimeEvidence.get(\"minecraftRuntimeAccessed\"))"),
                "AdapterCore mutation ledger record-level release proof must require top-level Minecraft runtime access.");
        require(ledgerSource.contains("bool(runtimeEvidence.get(\"liveRuntimeAccessed\"))")
                        && ledgerSource.contains("bool(runtimeEvidence.get(\"liveRuntimeMutationSupported\"))"),
                "AdapterCore mutation ledger record-level release proof must require top-level live runtime access/support.");
        require(ledgerSource.contains("surfaceLiveRuntimeAccessed"),
                "AdapterCore mutation ledger scoped surface proof must require surface live runtime access.");
        require(ledgerSource.contains("concreteSurfaceProofSatisfied(surface, runtimeEvidence)")
                        && ledgerSource.contains("concreteSurfaceProofSatisfied(record.surface(), record.runtimeEvidence())")
                        && ledgerSource.contains("surfaceLiveRuntimeProofEvidence")
                        && ledgerSource.contains("subsystemLiveRuntimeDispatchProofSatisfied")
                        && ledgerSource.contains("liveRuntimeDispatchProofSatisfied")
                        && ledgerSource.contains("liveRuntimeDispatchMinecraftAccessed")
                        && ledgerSource.contains("liveRuntimeDispatchMutationSupported")
                        && ledgerSource.contains("liveRuntimeDispatchLiveMutation")
                        && ledgerSource.contains("adapterCoreSurfaceDispatchId")
                        && ledgerSource.contains("value(proof.get(\"liveRuntimeDispatchId\")).equals(value(runtimeEvidence.get(\"adapterCoreSurfaceDispatchId\")))")
                        && ledgerSource.contains("value(surface).equals(value(proof.get(\"liveRuntimeSurface\")))"),
                "AdapterCore mutation ledger live proof records must require concrete surface dispatch proof evidence, not top-level projected flags.");
        require(ledgerSource.contains("releaseProofSurface(record.surface())"),
                "AdapterCore mutation ledger proof counts must filter diagnostic-only surfaces.");
        require(ledgerSource.contains("return !\"feedback\".equals(value(surface));"),
                "AdapterCore mutation ledger must keep feedback records diagnostic-only for release proof.");
        require(ledgerSource.contains("report.put(\"surfaceLiveRuntimeProofEvidence\", surfaceProofEvidence)")
                        && ledgerSource.contains("report.put(\"liveRuntimeDispatchId\", value(surfaceProofEvidence.get(\"liveRuntimeDispatchId\")))")
                        && ledgerSource.contains("report.put(\"liveRuntimeSurface\", value(surfaceProofEvidence.get(\"liveRuntimeSurface\")))"),
                "AdapterCore mutation ledger reports must expose concrete direct runtime proof evidence and dispatch correlation at top level.");
        require(!ledgerSource.contains("statusMap.get(value(surface))"),
                "AdapterCore mutation ledger must not promote status-only bridge maps to live surface proof.");

        Path backendPath = resolveSourcePath("echo-native-platform/echo-native-loader/src/main/java/dev/echo/nativeplatform/loader/NativeLoaderAdapterCoreBackend.java");
        String backendSource = Files.readString(backendPath, StandardCharsets.UTF_8);
        require(backendSource.contains("runtimeEvidence(surface, after)"),
                "AdapterCore backend must record runtime evidence after each surface operation.");
        require(backendSource.contains("surfaceLiveRuntimeReleaseProofSatisfied"),
                "AdapterCore backend must propagate subsystem surface release proof into ledger evidence.");
        require(backendSource.contains("surfaceLiveRuntimeAccessed"),
                "AdapterCore backend must propagate scoped surface live runtime access into ledger evidence.");
        require(backendSource.contains("serviceProof && serviceLiveAccess && serviceMinecraftAccess && serviceLiveMutation"),
                "AdapterCore backend must require subsystem live runtime access before promoting record-level proof.");
        require(backendSource.contains("subsystemSurfaceProofEvidence(surface, report)")
                        && backendSource.contains("subsystemSurfaceProofSatisfied(surfaceProofEvidence)")
                        && backendSource.contains("serviceConcreteProof")
                        && backendSource.contains("serviceProof && serviceLiveAccess && serviceMinecraftAccess && serviceLiveMutation && serviceConcreteProof"),
                "AdapterCore backend must require concrete subsystem entry proof before promoting aggregate subsystem host reports.");
        require(backendSource.contains("\"liveRuntimeDispatchMinecraftAccessed\"")
                        && backendSource.contains("\"liveRuntimeDispatchMutationSupported\"")
                        && backendSource.contains("\"liveRuntimeDispatchLiveMutation\"")
                        && backendSource.contains("bool(proof.get(\"liveRuntimeDispatchMinecraftAccessed\"))")
                        && backendSource.contains("bool(proof.get(\"liveRuntimeDispatchMutationSupported\"))")
                        && backendSource.contains("bool(proof.get(\"liveRuntimeDispatchLiveMutation\"))"),
                "AdapterCore backend subsystem proof promotion must require dispatch-level Minecraft access, mutation support, and live mutation evidence.");
        require(backendSource.contains("evidence.put(\"liveRuntimeReleaseProofSatisfied\", true)"),
                "AdapterCore backend must promote subsystem release proof into record-level live runtime proof.");
        require(backendSource.contains("hostSurfaceMutated(surface, evidence)"),
                "AdapterCore backend must derive direct host per-surface mutation evidence.");
        require(backendSource.contains("report.containsKey(\"liveRuntimeReleaseProofSatisfied\")"),
                "AdapterCore backend must not let ordinary runtime snapshot maps overwrite direct host proof.");
        require(backendSource.contains("liveRuntimeBridgeProofBySurface"),
                "AdapterCore backend direct host proof must honor runtime host per-surface proof, not only status.");
        require(backendSource.contains("liveRuntimeBridgeProofEvidenceBySurface")
                        && backendSource.contains("surfaceLiveRuntimeProofEvidence")
                        && backendSource.contains("adapterCoreSurfaceDispatchId")
                        && backendSource.contains("hostSurfaceProofEvidence(surface, evidence)"),
                "AdapterCore backend direct host proof must promote concrete per-surface bridge proof evidence into ledger records.");
        require(!backendSource.contains("statusMap.get(surface)"),
                "AdapterCore backend direct host proof must not promote bare liveRuntimeBridgeStatusBySurface entries.");

        return Map.of(
                "ledgerPath", ledgerPath.toString(),
                "backendPath", backendPath.toString(),
                "requiresPerSurfaceLiveRuntimeProof", true,
                "rejectsGlobalProofLeakage", true,
                "rejectsStatusOnlySurfaceProof", true,
                "requiresScopedRecordReleaseProof", true
        );
    }

    private static Map<String, Object> serviceRoutingCoverage() throws IOException {
        Path serviceBridgePath = resolveSourcePath("echo-native-platform/echo-native-loader/src/main/java/dev/echo/nativeplatform/loader/NativeLoaderServiceBridge.java");
        String serviceBridgeSource = Files.readString(serviceBridgePath, StandardCharsets.UTF_8);
        for (String alias : List.of(
                "\"adaptercore.native_command\"",
                "\"adaptercore.native_runtime_packet\"",
                "\"structures\"",
                "\"block_entities\"",
                "\"capabilities\"",
                "\"missions\"",
                "\"ashfall.missions\"",
                "\"client.tick.end\"",
                "\"client.render\"",
                "\"client.screen.open\"",
                "\"client.input\"",
                "\"config_reloads\"",
                "\"resource_reloads\"",
                "\"save_hooks\"",
                "\"lifecycle_phases\"",
                "\"server.client.sync\"")) {
            require(serviceBridgeSource.contains(alias),
                    "Native Loader service bridge must keep Agent 5 descriptor alias routed: " + alias);
        }

        Path registrarPath = resolveSourcePath("echo-native-platform/echo-native-loader/src/main/java/dev/echo/nativeplatform/loader/NativeLoaderCoreServiceRegistrar.java");
        String registrarSource = Files.readString(registrarPath, StandardCharsets.UTF_8);
        for (String surface : List.of(
                "\"client_tick\"",
                "\"render_layers\"",
                "\"screen_events\"",
                "\"keybinds\"",
                "\"commands\"",
                "\"network_channels\"",
                "\"structures\"",
                "\"block_entities\"",
                "\"capabilities\"",
                "\"missions\"",
                "\"config_reloads\"",
                "\"resource_reloads\"",
                "\"save_hooks\"",
                "\"lifecycle_phases\"",
                "\"server_client_sync\"")) {
            require(registrarSource.contains(surface),
                    "Native Loader core service registrar must expose Agent 5 surface: " + surface);
        }

        return Map.of(
                "serviceBridgePath", serviceBridgePath.toString(),
                "coreServiceRegistrarPath", registrarPath.toString(),
                "descriptorAliasesRouteToAgent5Services", true,
                "runtimeHostAndAdapterCoreExposeAgent5Surfaces", true
        );
    }

    private static Map<String, Object> releaseAndCliProofCoverage() throws IOException {
        Path launcherPath = resolveSourcePath("echo-native-platform/echo-native-product-launcher/src/main/java/dev/echo/nativeplatform/product/EchoNativeProductLauncher.java");
        String launcherSource = Files.readString(launcherPath, StandardCharsets.UTF_8);
        Path liveProofServicePath = resolveSourcePath("echo-native-platform/echo-native-loader/src/main/java/dev/echo/nativeplatform/loader/NativeLoaderLiveProofService.java");
        String liveProofServiceSource = Files.readString(liveProofServicePath, StandardCharsets.UTF_8);
        require(launcherSource.contains("&& agent5LiveRuntimeSurfaceProofReady()"),
                "Product full release readiness must require Agent 5 live runtime surface proof.");
        require(launcherSource.contains("public boolean agent5LiveRuntimeSurfaceProofReady()"),
                "Product runtime capability report must expose Agent 5 live runtime surface proof.");
        require(launcherSource.contains("fallbackMirrorMutationCount")
                        && launcherSource.contains("fallbackMirrorMutationCount == 0")
                        && launcherSource.contains("ECHO-NATIVE-RELEASE-FALLBACK-MIRROR-MUTATION"),
                "Product runtime capability report must expose fallback mirror mutation counts and block Agent 5 release proof when any fallback mirror mutation remains.");
        require(launcherSource.contains("public boolean nativeRuntimeDispatchReady()")
                        && launcherSource.contains("return liveRuntimeBridgeAttached;")
                        && !launcherSource.contains("|| (firstClassNativeRuntime && liveRuntimeTrusted && !delegateRequired)"),
                "Product runtime dispatch readiness must require an attached live runtime bridge, not first-class metadata alone.");
        for (String requiredProof : List.of(
                "liveRuntimeBridgeAttached",
                "lifecycleLiveRuntimeMutationCount > 0",
                "lifecycleMinecraftRuntimeAccessed",
                "lifecycleLiveRuntimeReleaseProofSatisfied",
                "commandLiveRuntimeMutationCount > 0",
                "commandMinecraftRuntimeAccessed",
                "commandLiveRuntimeReleaseProofSatisfied",
                "configLiveRuntimeMutationCount > 0",
                "configMinecraftRuntimeAccessed",
                "configLiveRuntimeReleaseProofSatisfied",
                "networkLiveRuntimeMutationCount > 0",
                "networkMinecraftRuntimeAccessed",
                "networkLiveRuntimeReleaseProofSatisfied",
                "adapterCoreLiveRuntimeProofRecordCount > 0",
                "adapterCoreLiveRuntimeProofSurfaces.containsAll(requiredAgent5AdapterCoreLiveProofSurfaces())"
        )) {
            require(launcherSource.contains(requiredProof),
                    "Product Agent 5 live proof predicate must require: " + requiredProof);
        }
        require(launcherSource.contains("private static List<String> adapterCoreLiveRuntimeProofSurfaces("),
                "Product runtime capability report must derive AdapterCore live proof surfaces from the ledger.");
        require(launcherSource.contains("private static boolean adapterCoreTopLevelProofSatisfied(")
                        && launcherSource.contains("surfaceLiveRuntimeProofEvidence")
                        && launcherSource.contains("subsystemLiveRuntimeDispatchProofSatisfied")
                        && launcherSource.contains("liveRuntimeDispatchProofSatisfied")
                        && launcherSource.contains("liveRuntimeDispatchMinecraftAccessed")
                        && launcherSource.contains("liveRuntimeDispatchMutationSupported")
                        && launcherSource.contains("liveRuntimeDispatchLiveMutation")
                        && launcherSource.contains("recordReport.get(\"liveRuntimeDispatchId\")")
                        && launcherSource.contains("adapterCoreSurfaceDispatchId")
                        && launcherSource.contains("recordReport.get(\"liveRuntimeSurface\")"),
                "Product AdapterCore live proof surfaces must require top-level per-dispatch proof evidence, not aggregate ledger state.");
        require(launcherSource.contains("public boolean nativeEventsReady()")
                        && launcherSource.contains("&& lifecycleLiveRuntimeReleaseProofSatisfied"),
                "Product event readiness must require live lifecycle/event runtime proof, not handler execution alone.");
        require(!launcherSource.contains("private static boolean nativeStateReady(")
                        && !launcherSource.contains("status == EchoNativeLoadStatus.MUTATED || status == EchoNativeLoadStatus.RESOLVED"),
                "Product launcher must not keep a generic nativeStateReady helper that treats RESOLVED as runtime mutation proof.");
        require(liveProofServiceSource.contains("liveProofSurfaces(mutationLedger, evidenceConfig)")
                        && liveProofServiceSource.contains("mutationRecordLiveRuntimeProofSatisfied(record)")
                        && liveProofServiceSource.contains("mutationLedgerLiveProofSurfaces")
                        && liveProofServiceSource.contains("liveRuntimeReleaseProofSatisfied")
                        && liveProofServiceSource.contains("liveRuntimeSurfaceMutationSatisfied")
                        && !liveProofServiceSource.contains("Set<String> mutatedSurfaces = mutatedSurfaces(mutationLedger, evidenceConfig)"),
                "Native Loader live proof service must derive gameplay/save/HUD readiness from record-level live proof surfaces, not raw mutated ledger surfaces.");
        require(liveProofServiceSource.contains("concreteSurfaceProofSatisfied(record)")
                        && liveProofServiceSource.contains("surfaceLiveRuntimeProofEvidence")
                        && liveProofServiceSource.contains("liveRuntimeDispatchProofSatisfied")
                        && liveProofServiceSource.contains("subsystemLiveRuntimeDispatchProofSatisfied")
                        && liveProofServiceSource.contains("liveRuntimeDispatchMinecraftAccessed")
                        && liveProofServiceSource.contains("liveRuntimeDispatchMutationSupported")
                        && liveProofServiceSource.contains("liveRuntimeDispatchLiveMutation")
                        && liveProofServiceSource.contains("adapterCoreSurfaceDispatchId"),
                "Native Loader live proof service must require concrete dispatch proof evidence before accepting ledger records as live proof.");
        require(liveProofServiceSource.contains("Set<String> liveProofSurfaces = liveProofSurfaces(mutationLedger, evidenceConfig);")
                        && liveProofServiceSource.contains("selected.put(\"nativeHostMutationClaimAllowed\", playerOrWorldMutation || liveSaveDataWrite || liveHudNotificationEmitted)")
                        && liveProofServiceSource.contains("selected.put(\"requiredMutationSurfacesMutated\", requiredMutationSurfacesMutated)")
                        && liveProofServiceSource.contains("boolean complete = missingTargets.isEmpty() && requiredMutationSurfacesMutated"),
                "Native Loader live proof normalization must recompute stale mutation targets from record-level live proof surfaces.");
        for (String requiredSurface : List.of(
                "\"inventory\"",
                "\"player_state\"",
                "\"world_blocks\"",
                "\"world_state\"",
                "\"structures\"",
                "\"block_entities\"",
                "\"capabilities\"",
                "\"missions\"",
                "\"events\"",
                "\"packets_hud\"",
                "\"save_data\"",
                "\"hud\"",
                "\"client_tick\"",
                "\"render_layers\"",
                "\"screen_events\"",
                "\"keybinds\"",
                "\"resource_reloads\"",
                "\"save_hooks\"",
                "\"server_client_sync\"",
                "\"commands\"",
                "\"network_channels\"",
                "\"config_reloads\"",
                "\"lifecycle_phases\""
        )) {
            require(launcherSource.contains(requiredSurface),
                    "Product Agent 5 live proof predicate must include required AdapterCore surface: " + requiredSurface);
        }
        for (String runtimeHookRoute : List.of(
                "case \"commands\" -> backend.registerCommand(",
                "case \"network_channels\" -> backend.registerNetworkPacket(",
                "case \"config_reloads\" -> backend.reloadConfig(",
                "case \"structures\" -> backend.placeStructure(",
                "case \"block_entities\" -> backend.updateBlockEntity(",
                "case \"capabilities\" -> backend.updateCapability(",
                "case \"missions\" -> backend.updateMission(",
                "case \"lifecycle_phases\" -> backend.lifecyclePhase(")) {
            require(launcherSource.contains(runtimeHookRoute),
                    "Product runtime hooks must route Agent 5 subsystem surface through AdapterCore: "
                            + runtimeHookRoute);
        }
        for (String runtimeHookPayloadField : List.of(
                "payload.getOrDefault(\"itemId\", hook.targetId())",
                "payload.getOrDefault(\"key\", hook.targetId())",
                "payload.getOrDefault(\"blockId\", hook.targetId())",
                "payload.getOrDefault(\"structureId\", hook.targetId())",
                "payload.getOrDefault(\"capability\", hook.targetId())",
                "payload.getOrDefault(\"eventId\", hook.targetId())",
                "payload.getOrDefault(\"channel\", hook.targetId())",
                "payload.getOrDefault(\"missionId\", hook.targetId())",
                "payload.getOrDefault(\"commandId\", hook.targetId())",
                "payload.getOrDefault(\"packetId\", hook.targetId())",
                "payload.getOrDefault(\"configId\", hook.targetId())",
                "payload.getOrDefault(\"resourceId\", hook.targetId())",
                "payload.getOrDefault(\"hookId\", hook.targetId())",
                "payload.getOrDefault(\"phaseId\", hook.targetId())")) {
            require(launcherSource.contains(runtimeHookPayloadField),
                    "Product runtime hooks must honor declared payload field before falling back to targetId: "
                            + runtimeHookPayloadField);
        }
        require(launcherSource.contains("removeAction(action)")
                        && launcherSource.contains("backend.removeItem("),
                "Product inventory runtime hooks must route remove/consume actions through AdapterCore removeItem.");
        require(launcherSource.contains("deleteAction(action)")
                        && launcherSource.contains("backend.deleteSaveData("),
                "Product save_data runtime hooks must route delete/clear actions through AdapterCore deleteSaveData.");
        require(launcherSource.contains("runtimeHookExecutionStatus(record)")
                        && launcherSource.contains("adapterCoreLiveProofExecutionStatus(record)")
                        && launcherSource.contains("record.liveRuntimeReleaseProofSatisfied()")
                        && launcherSource.contains("record.liveRuntimeSurfaceMutationSatisfied()"),
                "Product runtime hook executions must count as MUTATED only when AdapterCore recorded live Minecraft surface proof.");
        int adapterCoreLiveProofStatusUseCount =
                launcherSource.split("adapterCoreLiveProofExecutionStatus\\(record\\)", -1).length - 1;
        require(adapterCoreLiveProofStatusUseCount >= 1
                        && launcherSource.contains("return adapterCoreLiveProofExecutionStatus(record);")
                        && launcherSource.contains("return adapterCoreLiveProofSatisfied(record) ? EchoNativeLoadStatus.MUTATED : EchoNativeLoadStatus.UNSUPPORTED;"),
                "Product AdapterCore hook execution statuses must route through centralized live-proof gating.");
        for (String liveProofHookStatusRoute : List.of(
                "\"lifecycle\",",
                "\"command\", hook.moduleId(), hook.commandId(),",
                "\"network\", hook.moduleId(), hook.packetId(),",
                "\"config\", hook.moduleId(), hook.configId(),",
                "\"save_data\", \"echocore\", hook.key(),")) {
            int routeIndex = launcherSource.indexOf(liveProofHookStatusRoute);
            require(routeIndex >= 0
                            && launcherSource.indexOf("adapterCoreLiveProofExecutionStatus(record)", routeIndex) > routeIndex,
                    "Product AdapterCore hook execution must use live-proof status gating near: "
                            + liveProofHookStatusRoute);
        }
        require(launcherSource.contains("adapterCoreLiveProofSatisfied(record)")
                        && launcherSource.contains("adapterCoreLiveProofSatisfied(policyRecord)")
                        && launcherSource.contains(".allMatch(EchoNativeProductLauncher::adapterCoreLiveProofSatisfied)")
                        && launcherSource.contains(".filter(EchoNativeProductLauncher::adapterCoreLiveProofSatisfied)"),
                "Product world and onboarding hooks must require AdapterCore live proof instead of nativeStateReady mirror status.");
        require(launcherSource.contains("mutationLedger.liveRuntimeProofRecordCountBySurface(\"save_data\")")
                        && !launcherSource.contains("mutationLedger.mutatedRecordCountBySurface(\"save_data\")"),
                "Product save-data readiness must use live AdapterCore proof records, not raw save_data MUTATED status.");
        Path mutationLedgerPath = resolveSourcePath("echo-native-platform/echo-native-loader/src/main/java/dev/echo/nativeplatform/loader/NativeLoaderMutationLedger.java");
        String mutationLedgerSource = Files.readString(mutationLedgerPath, StandardCharsets.UTF_8);
        require(mutationLedgerSource.contains("public int liveRuntimeProofRecordCountBySurface(String surface)")
                        && mutationLedgerSource.contains("liveRuntimeProofRecord(record)"),
                "Native Loader mutation ledger must expose per-surface live-proof record counts.");
        require(launcherSource.contains("resourceHookExecutionStatus(status, hook.evidence())")
                        && launcherSource.contains("safeEvidence.get(\"liveMinecraftMutation\")")
                        && launcherSource.contains("safeEvidence.get(\"minecraftRuntimeAccessed\")"),
                "Product resource hook executions must not count mounted resources as live runtime mutation proof.");
        require(launcherSource.contains("clientUiHookExecutionStatus(services, hook.moduleId(), hook.surfaceId(), status)")
                        && launcherSource.contains("surface.get(\"liveClientBridgeMutated\")")
                        && launcherSource.contains("bridgeEvidence.get(\"nativeClientRouteProcess\")")
                        && launcherSource.contains("bridgeEvidence.get(\"releaseClientRouteTrusted\")")
                        && launcherSource.contains("bridgeEvidence.get(\"clientRouteMutationSupported\")")
                        && launcherSource.contains("bridgeEvidence.get(\"neoForgeEventOwnershipRequired\")"),
                "Product client UI hook executions must require live trusted client bridge mutation instead of route-table registration.");
        require(launcherSource.contains("clientUiHost.nativeLoaderOwnsClientHostServices()")
                        && launcherSource.contains("clientUiHost.neoForgeClientEventsCompatibilityAdaptersOnly()")
                        && launcherSource.contains("&& nativeLoaderOwnsClientHostServices")
                        && launcherSource.contains("&& !neoForgeClientEventsCompatibilityAdaptersOnly")
                        && launcherSource.contains("ECHO-NATIVE-RELEASE-CLIENT-NEOFORGE-OWNERSHIP"),
                "Product release readiness must reject live client bridges that remain NeoForge compatibility-adapter owned.");
        Path runtimeHostPath = resolveSourcePath("echo-native-platform/echo-native-loader/src/main/java/dev/echo/nativeplatform/loader/NativeLoaderRuntimeHost.java");
        String runtimeHostSource = Files.readString(runtimeHostPath, StandardCharsets.UTF_8);
        require(runtimeHostSource.contains("runtimeSaveDataMutated")
                        && runtimeHostSource.contains("runtimeSurfaceSaveMutated")
                        && runtimeHostSource.contains("directSaveDataEvidenceSatisfied"),
                "Runtime host save-data proof must require live save mutation evidence, not touched-only evidence.");
        require(runtimeHostSource.contains("directStructureSurfaceEvidenceSatisfied")
                        && runtimeHostSource.contains("runtimeStructureMutated")
                        && runtimeHostSource.contains("runtimeSaveDataMutated")
                        && runtimeHostSource.contains("runtimeSurfaceSaveMutated"),
                "Runtime host structure proof must require live save mutation evidence for persisted structure state.");
        require(launcherSource.contains("\"event_subscription\",")
                        && launcherSource.contains("EchoNativeLoadStatus.RESOLVED,")
                        && launcherSource.contains("event.liveMinecraftMutation() ? EchoNativeLoadStatus.MUTATED : EchoNativeLoadStatus.UNSUPPORTED"),
                "Product event hook executions must not count declaration or handler execution as live mutation proof.");
        require(launcherSource.contains("default -> backend.unsupportedRuntimeHook(surface, hook.surface(), hook.targetId())"),
                "Product runtime hooks must record unsupported runtime declarations without mutating feedback as proof.");
        Path adapterCoreBackendPath = resolveSourcePath("echo-native-platform/echo-native-loader/src/main/java/dev/echo/nativeplatform/loader/NativeLoaderAdapterCoreBackend.java");
        String adapterCoreBackendSource = Files.readString(adapterCoreBackendPath, StandardCharsets.UTF_8);
        require(adapterCoreBackendSource.contains("public NativeLoaderMutationLedger.MutationRecord unsupportedRuntimeHook("),
                "AdapterCore backend must expose a diagnostic-only unsupported runtime hook ledger path.");
        require(adapterCoreBackendSource.contains("return unsupported(surface, \"runtime_hook.unsupported\", target, SERVICE_ID);"),
                "Unsupported runtime hooks must be UNSUPPORTED ledger records, not feedback live mutations.");
        for (String runtimeHookAlias : List.of(
                "case \"command\", \"commands\", \"server_command\", \"server_commands\", \"native_command\" -> \"commands\"",
                "case \"network_channel\", \"network_channels\", \"channel\", \"channels\", \"native_packet\" -> \"network_channels\"",
                "case \"config\", \"config_reload\", \"config_reloads\", \"configuration\" -> \"config_reloads\"",
                "case \"structure\", \"structures\", \"place_structure\" -> \"structures\"",
                "case \"block_entity\", \"block_entities\", \"blockentity\", \"blockentities\" -> \"block_entities\"",
                "case \"capability\", \"capabilities\", \"player_capability\", \"energy_capability\" -> \"capabilities\"",
                "case \"mission\", \"missions\", \"quest\", \"quests\" -> \"missions\"",
                "case \"lifecycle\", \"lifecycle_phase\", \"lifecycle_phases\" -> \"lifecycle_phases\"")) {
            require(launcherSource.contains(runtimeHookAlias),
                    "Product runtime hooks must normalize Agent 5 subsystem aliases: " + runtimeHookAlias);
        }
        for (String oldOptionalProof : List.of(
                "(!liveRuntimeBridgeAttached || lifecycleLiveRuntimeMutationCount > 0)",
                "(!liveRuntimeBridgeAttached || commandLiveRuntimeMutationCount > 0)",
                "(!liveRuntimeBridgeAttached || configLiveRuntimeMutationCount > 0)",
                "(!liveRuntimeBridgeAttached || networkLiveRuntimeMutationCount > 0)",
                "(!liveRuntimeBridgeAttached || adapterCoreLiveRuntimeProofRecordCount > 0)"
        )) {
            require(!launcherSource.contains(oldOptionalProof),
                    "Release readiness must not make Agent 5 live proof optional: " + oldOptionalProof);
        }
        require(launcherSource.contains("ECHO-NATIVE-RELEASE-RUNTIME-LIVE-PROOF-MISSING"),
                "Product launcher diagnostics must report missing Agent 5 live proof.");
        require(launcherSource.contains("services.registryHost().creativeTab(tabId) != null"),
                "Product launcher descriptor creative-tab intent must be idempotent when the tab is already registered.");
        Path registryHostPath = resolveSourcePath("echo-native-platform/echo-native-loader/src/main/java/dev/echo/nativeplatform/loader/EchoNativeRegistryHost.java");
        String registryHostSource = Files.readString(registryHostPath, StandardCharsets.UTF_8);
        require(!registryHostSource.contains("\"creative_tab\".equals(kind) && existing != null && existing.status() == EchoNativeLoadStatus.MUTATED"),
                "Registry host must not silently resolve duplicate creative-tab redeclarations as release evidence.");

        Path readinessVerifierPath = resolveSourcePath("echo-native-platform/echo-native-product-launcher/src/qa/java/dev/echo/nativeplatform/product/EchoNativeProductLauncherReadinessVerifier.java");
        String readinessVerifierSource = Files.readString(readinessVerifierPath, StandardCharsets.UTF_8);
        Path productBuildPath = resolveSourcePath("echo-native-platform/build.gradle");
        String productBuildSource = Files.readString(productBuildPath, StandardCharsets.UTF_8);
        require(productBuildSource.contains("nativeProductReadinessPackageOutput"),
                "Product readiness Gradle task must use a dedicated readiness package output.");
        require(productBuildSource.contains("packageNativeProductReadinessLayout"),
                "Product readiness Gradle task must package into an isolated readiness layout.");
        require(productBuildSource.contains("dependsOn tasks.named('packageNativeProductReadinessLayout')"),
                "Product readiness verifier must depend on the isolated readiness package task.");
        require(productBuildSource.contains("nativeProductReadinessPackageOutput.get(), nativeProductSourceRoot.get()"),
                "Product readiness verifier must read the isolated readiness package path.");
        require(!productBuildSource.contains("dependsOn tasks.named('packageNativeProductLayout')\n    dependsOn nativeProductLauncherQaClassesTask"),
                "Product readiness verifier must not depend on the launch package task.");
        require(readinessVerifierSource.contains("\"release-mutation\".equals(phase)"),
                "Product readiness verifier must expose a focused release-mutation phase.");
        require(readinessVerifierSource.contains("\"trusted-evidence-without-bridges\".equals(phase)"),
                "Product readiness verifier must expose a focused trusted-evidence-without-bridges phase.");
        require(readinessVerifierSource.contains("release mutation requirement returned accepted="),
                "Product readiness verifier must trace release mutation outcomes.");
        require(readinessVerifierSource.contains("verifyTrustedEvidenceWithoutBridgesFailsGate"),
                "Product readiness verifier must factor the trusted-metadata-without-bridges negative gate.");
        require(readinessVerifierSource.contains("trusted evidence without bridges returned accepted="),
                "Product readiness verifier must trace trusted-metadata-without-bridges outcomes.");
        require(readinessVerifierSource.contains("!runtimeWithoutBridge.agent5LiveRuntimeSurfaceProofReady()"),
                "Product readiness verifier must reject trusted runtime evidence without Agent 5 live proof.");
        require(readinessVerifierSource.contains("reportProjectionRuntimeBridge()"),
                "Product readiness verifier must exercise report-only live runtime projection.");
        require(readinessVerifierSource.contains("!reportProjectionRuntime.agent5LiveRuntimeSurfaceProofReady()"),
                "Product readiness verifier must reject report-only projection as Agent 5 live proof.");
        require(readinessVerifierSource.contains("reportProjectionRuntime.adapterCoreLiveRuntimeProofRecordCount() == 0"),
                "Product readiness verifier must prove report-only projection creates no AdapterCore live proof records.");
        require(readinessVerifierSource.contains("staleDispatchRuntimeBridge()"),
                "Product readiness verifier must exercise stale live-runtime dispatch proof.");
        require(readinessVerifierSource.contains("!staleDispatchRuntime.agent5LiveRuntimeSurfaceProofReady()"),
                "Product readiness verifier must reject stale dispatch ids as Agent 5 live proof.");
        require(readinessVerifierSource.contains("staleDispatchRuntime.adapterCoreMutatedRecordCount() > 0"),
                "Product readiness verifier must prove stale dispatch bridge still mutates native AdapterCore records.");
        require(readinessVerifierSource.contains("staleDispatchRuntime.adapterCoreLiveRuntimeProofRecordCount() == 0"),
                "Product readiness verifier must prove stale dispatch bridge creates no AdapterCore live proof records.");
        require(readinessVerifierSource.contains("staleDispatchRuntime.commandLiveRuntimeMutationCount() == 0"),
                "Product readiness verifier must prove stale dispatch ids do not count command live mutations.");
        require(readinessVerifierSource.contains("staleDispatchRuntime.configLiveRuntimeMutationCount() == 0"),
                "Product readiness verifier must prove stale dispatch ids do not count config live mutations.");
        require(readinessVerifierSource.contains("staleDispatchRuntime.networkLiveRuntimeMutationCount() == 0"),
                "Product readiness verifier must prove stale dispatch ids do not count network live mutations.");
        require(readinessVerifierSource.contains("staleDispatchRuntime.lifecycleLiveRuntimeMutationCount() == 0"),
                "Product readiness verifier must prove stale dispatch ids do not count lifecycle live mutations.");
        require(readinessVerifierSource.contains("ECHO-NATIVE-RELEASE-RUNTIME-LIVE-PROOF-MISSING"),
                "Product readiness verifier must require a live-proof-missing diagnostic for stale dispatch proof.");
        require(readinessVerifierSource.contains("liveRuntimeDispatchId\", STALE_DISPATCH_ID"),
                "Product readiness stale bridge must overwrite dispatch ids with stale proof.");
        require(readinessVerifierSource.contains("wrongSubsystemSurfaceRuntimeBridge()"),
                "Product readiness verifier must exercise wrong subsystem liveRuntimeSurface proof.");
        require(readinessVerifierSource.contains("!wrongSubsystemSurfaceRuntime.agent5LiveRuntimeSurfaceProofReady()"),
                "Product readiness verifier must reject wrong subsystem surface proof as Agent 5 live proof.");
        require(readinessVerifierSource.contains("wrongSubsystemSurfaceRuntime.commandLiveRuntimeMutationCount() == 0"),
                "Product readiness verifier must prove wrong subsystem surface evidence does not count command live mutations.");
        require(readinessVerifierSource.contains("wrongSubsystemSurfaceRuntime.configLiveRuntimeMutationCount() == 0"),
                "Product readiness verifier must prove wrong subsystem surface evidence does not count config live mutations.");
        require(readinessVerifierSource.contains("wrongSubsystemSurfaceRuntime.networkLiveRuntimeMutationCount() == 0"),
                "Product readiness verifier must prove wrong subsystem surface evidence does not count network live mutations.");
        require(readinessVerifierSource.contains("wrongSubsystemSurfaceRuntime.lifecycleLiveRuntimeMutationCount() == 0"),
                "Product readiness verifier must prove wrong subsystem surface evidence does not count lifecycle live mutations.");
        require(readinessVerifierSource.contains("\"liveRuntimeSurface\", wrongSurface"),
                "Product readiness wrong subsystem surface bridge must stamp an incorrect liveRuntimeSurface.");
        for (String runtimeHookAssertion : List.of(
                "\"runtime\", \"structures:echoashfallprotocol:trusted_bridge_structure\"",
                "\"runtime\", \"block_entities:qa.trusted_bridge.block_entity\"",
                "\"runtime\", \"capabilities:qa.trusted_bridge.capability\"",
                "\"runtime\", \"missions:qa.trusted_bridge.mission\"",
                "\"runtime\", \"commands:qa.trusted_bridge.runtime_command\"",
                "\"runtime\", \"network_channels:qa.trusted_bridge.runtime_packet\"",
                "\"runtime\", \"config_reloads:qa.trusted_bridge.runtime_config\"",
                "\"runtime\", \"lifecycle_phases:qa.trusted_bridge.runtime_lifecycle\"")) {
            require(readinessVerifierSource.contains(runtimeHookAssertion),
                    "Product readiness verifier must exercise subsystem runtime hook routing: "
                            + runtimeHookAssertion);
        }
        require(readinessVerifierSource.contains("runtime.agent5LiveRuntimeSurfaceProofReady()"),
                "Product readiness verifier must accept trusted live bridge Agent 5 proof.");
        for (String verifierProof : List.of(
                "runtime.lifecycleMinecraftRuntimeAccessed()",
                "runtime.commandMinecraftRuntimeAccessed()",
                "runtime.configMinecraftRuntimeAccessed()",
                "runtime.networkMinecraftRuntimeAccessed()",
                "runtime.lifecycleLiveRuntimeReleaseProofSatisfied()",
                "runtime.commandLiveRuntimeReleaseProofSatisfied()",
                "runtime.configLiveRuntimeReleaseProofSatisfied()",
                "runtime.networkLiveRuntimeReleaseProofSatisfied()"
        )) {
            require(readinessVerifierSource.contains(verifierProof),
                    "Product readiness verifier must assert host Minecraft runtime access: " + verifierProof);
        }
        require(readinessVerifierSource.contains("runtime.adapterCoreLiveRuntimeProofSurfaces().containsAll("),
                "Product readiness verifier must assert AdapterCore live proof for every required Agent 5 surface.");
        require(readinessVerifierSource.contains("verifyUnsupportedRuntimeHooksStayDiagnosticOnly"),
                "Product readiness verifier must exercise unsupported runtime hooks as diagnostic-only records.");
        require(readinessVerifierSource.contains("\"unsupported_live_surface:qa.unsupported.runtime_hook\""),
                "Product readiness verifier must launch an unsupported runtime hook declaration.");
        require(readinessVerifierSource.contains("!runtime.adapterCoreLiveRuntimeProofSurfaces().contains(\"unsupported_live_surface\")"),
                "Product readiness verifier must prove unsupported runtime hook surfaces do not become live proof.");
        require(readinessVerifierSource.contains("!runtime.adapterCoreLiveRuntimeProofSurfaces().contains(\"feedback\")"),
                "Product readiness verifier must prove unsupported runtime hooks create no feedback live proof.");
        require(readinessVerifierSource.contains("!outcome.hookReport().runtimeMutatedSurfaces().contains(\"feedback\")"),
                "Product readiness verifier must prove unsupported runtime hooks do not mutate feedback proof.");

        Path resolverPath = resolveSourcePath("echo-native-platform/echo-native-product-launcher/src/main/java/dev/echo/nativeplatform/product/EchoNativeProductBridgeProviderResolver.java");
        String resolverSource = Files.readString(resolverPath, StandardCharsets.UTF_8);
        require(resolverSource.contains("preserveExplicitLiveClientAttachment"),
                "Bridge provider resolver must preserve explicit trusted live client proof.");
        require(resolverSource.contains("explicitClientBridge == null || !explicitClientBridge.attached()"),
                "Explicit client proof preservation must require a typed live client bridge.");
        require(resolverSource.contains("clientAssessment.put(key, explicitClientAssessment.get(key))"),
                "Provider metadata must not downgrade explicit trusted client attachment keys.");

        Path cliPath = resolveSourcePath("echo-native-platform/echo-native-cli/src/main/java/dev/echo/nativeplatform/cli/EchoNativeRuntimeCli.java");
        String cliSource = Files.readString(cliPath, StandardCharsets.UTF_8);
        for (String cliProof : List.of(
                "nativeMirrorMutationsProven",
                "liveRuntimeReleaseProofSatisfied",
                "mirrorOnlyReleaseProof",
                "ledger.liveRuntimeProofRecordCount() > 0",
                "Live proof failed: native mirror mutation is not live runtime release proof",
                "PROOF FAILED: Native Loader only produced native mirror mutations; no live runtime proof was attached."
        )) {
            require(cliSource.contains(cliProof),
                    "CLI prove-live must keep mirror mutation distinct from live proof: " + cliProof);
        }
        for (String cliSurface : List.of(
                "commandStatus",
                "packetStatus",
                "configStatus",
                "lifecycleStatus",
                "runtimeEventStatus"
        )) {
            require(cliSource.contains(cliSurface),
                    "CLI prove-live must exercise Agent 5 native surface: " + cliSurface);
        }

        Map<String, Object> coverage = new LinkedHashMap<>();
        coverage.put("launcherPath", launcherPath.toString());
        coverage.put("liveProofServicePath", liveProofServicePath.toString());
        coverage.put("adapterCoreBackendPath", adapterCoreBackendPath.toString());
        coverage.put("registryHostPath", registryHostPath.toString());
        coverage.put("readinessVerifierPath", readinessVerifierPath.toString());
        coverage.put("productBuildPath", productBuildPath.toString());
        coverage.put("bridgeProviderResolverPath", resolverPath.toString());
        coverage.put("cliPath", cliPath.toString());
        coverage.put("productReleaseRequiresAgent5LiveProof", true);
        coverage.put("readinessVerifierUsesIsolatedPackage", true);
        coverage.put("readinessVerifierHasFocusedReleaseMutationPhase", true);
        coverage.put("readinessVerifierHasFocusedTrustedMetadataNegativePhase", true);
        coverage.put("readinessVerifierRejectsTrustedMetadataOnly", true);
        coverage.put("readinessVerifierRejectsStaleDispatchProof", true);
        coverage.put("readinessVerifierRejectsWrongSubsystemSurfaceProof", true);
        coverage.put("descriptorCreativeTabsIdempotent", true);
        coverage.put("resolverPreservesExplicitTrustedClientProof", true);
        coverage.put("cliMirrorProofSeparatedFromLiveProof", true);
        coverage.put("liveProofServiceRequiresRecordLevelLiveRuntimeProof", true);
        return Map.copyOf(coverage);
    }

    private static Path resolveSourcePath(String relativePath) {
        String normalized = relativePath.replace('\\', '/');
        if (normalized.startsWith("addons/")) {
            Path path = echoModulesRoot().resolve(normalized.substring("addons/".length())).normalize();
            require(Files.isRegularFile(path), "Required source path is missing: " + path);
            return path;
        }
        if (normalized.startsWith("echo-native-platform/")) {
            Path path = nativePlatformRoot().resolve(normalized.substring("echo-native-platform/".length())).normalize();
            require(Files.isRegularFile(path), "Required source path is missing: " + path);
            return path;
        }
        Path path = Path.of(relativePath).toAbsolutePath().normalize();
        if (!Files.isRegularFile(path)) {
            path = Path.of("..").resolve(relativePath).toAbsolutePath().normalize();
        }
        require(Files.isRegularFile(path), "Required source path is missing: " + path);
        return path;
    }

    private static Path echoModulesRoot() {
        String configured = System.getProperty("echo.modules.root");
        if (configured == null || configured.isBlank()) {
            configured = System.getenv("ECHO_MODULES_ROOT");
        }
        if (configured != null && !configured.isBlank()) {
            return Path.of(configured).toAbsolutePath().normalize();
        }
        Path workspaceRoot = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize().getParent();
        Path workspaceModules = workspaceRoot == null
                ? Path.of("..", "ECHO-Modules", "addons")
                : workspaceRoot.resolve("ECHO-Modules").resolve("addons");
        if (Files.isDirectory(workspaceModules)) {
            return workspaceModules.toAbsolutePath().normalize();
        }
        Path legacyAddons = workspaceRoot == null
                ? Path.of("..", "addons")
                : workspaceRoot.resolve("addons");
        return legacyAddons.toAbsolutePath().normalize();
    }

    private static Path nativePlatformRoot() {
        Path current = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        if (Files.isDirectory(current.resolve("echo-native-loader"))) {
            return current;
        }
        Path workspaceRoot = current.getParent();
        if (workspaceRoot != null) {
            for (String candidate : List.of("ECHO-Native-Platform", "echo-native-platform")) {
                Path path = workspaceRoot.resolve(candidate).toAbsolutePath().normalize();
                if (Files.isDirectory(path.resolve("echo-native-loader"))) {
                    return path;
                }
            }
        }
        return current;
    }

    private static String toJson(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof String string) {
            return "\"" + string.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
        }
        if (value instanceof Number || value instanceof Boolean) {
            return String.valueOf(value);
        }
        if (value instanceof Map<?, ?> map) {
            StringBuilder out = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!first) {
                    out.append(',');
                }
                out.append('\n')
                        .append("  ")
                        .append(toJson(String.valueOf(entry.getKey())))
                        .append(": ")
                        .append(indent(toJson(entry.getValue())));
                first = false;
            }
            if (!map.isEmpty()) {
                out.append('\n');
            }
            return out.append('}').toString();
        }
        if (value instanceof Collection<?> collection) {
            StringBuilder out = new StringBuilder("[");
            boolean first = true;
            for (Object item : collection) {
                if (!first) {
                    out.append(',');
                }
                out.append('\n').append("  ").append(indent(toJson(item)));
                first = false;
            }
            if (!collection.isEmpty()) {
                out.append('\n');
            }
            return out.append(']').toString();
        }
        return toJson(String.valueOf(value));
    }

    private static String indent(String value) {
        return value.replace("\n", "\n  ");
    }

    private interface Agent5LiveRuntimeAccessBridge extends NativeLoaderLiveRuntimeBridge {
        @Override
        default boolean liveRuntimeAccessed() {
            return true;
        }
    }

    private static final class NonMinecraftLiveBridge implements Agent5LiveRuntimeAccessBridge {
        @Override
        public boolean attached() {
            return true;
        }

        @Override
        public String bridgeId() {
            return "agent5:non_minecraft_live_bridge";
        }

        @Override
        public boolean liveRuntimeMutationSupported() {
            return true;
        }

        @Override
        public Map<String, Object> runtimeEvidence() {
            return Map.of(
                    "bridgeId", bridgeId(),
                    "attached", true,
                    "liveRuntimeAccessed", true,
                    "minecraftRuntimeAccessed", false,
                    "liveRuntimeMutationSupported", true,
                    "realMinecraftProcess", false,
                    "agent5TruthGate", true
            );
        }

        @Override
        public EchoNativeLoadStatus grantItem(String playerId, String itemId, int count) {
            return EchoNativeLoadStatus.MUTATED;
        }

        @Override
        public EchoNativeLoadStatus registerCommand(
                String moduleId,
                String commandId,
                String targetSurface,
                String targetBridge,
                Map<String, Object> evidence
        ) {
            return EchoNativeLoadStatus.MUTATED;
        }

        @Override
        public EchoNativeLoadStatus registerNetworkPacket(
                String moduleId,
                String packetId,
                String surface,
                String sourceRuntimeTarget,
                List<String> consumers,
                Map<String, Object> evidence
        ) {
            return EchoNativeLoadStatus.MUTATED;
        }

        @Override
        public EchoNativeLoadStatus reloadConfig(
                String moduleId,
                String configId,
                String scope,
                Map<String, Object> evidence
        ) {
            return EchoNativeLoadStatus.MUTATED;
        }

        @Override
        public EchoNativeLoadStatus lifecyclePhase(String moduleId, String phaseId, Map<String, Object> evidence) {
            return EchoNativeLoadStatus.MUTATED;
        }

        @Override
        public EchoNativeLoadStatus publishRuntimeEvent(
                String sourceModule,
                String eventId,
                Map<String, Object> payload,
                EchoNativeLoadStatus status
        ) {
            return EchoNativeLoadStatus.MUTATED;
        }
    }

    private static final class LiveRuntimeInaccessibleSubsystemBridge implements Agent5LiveRuntimeAccessBridge {
        private final Map<String, String> activeDispatchIds = new java.util.LinkedHashMap<>();

        @Override
        public boolean attached() {
            return true;
        }

        @Override
        public String bridgeId() {
            return "agent5:live_runtime_inaccessible_subsystem_bridge";
        }

        @Override
        public boolean liveRuntimeAccessed() {
            return false;
        }

        @Override
        public boolean minecraftRuntimeAccessed() {
            return true;
        }

        @Override
        public boolean liveRuntimeMutationSupported() {
            return true;
        }

        @Override
        public Map<String, Object> runtimeEvidence() {
            return Map.of(
                    "bridgeId", bridgeId(),
                    "attached", true,
                    "liveRuntimeAccessed", false,
                    "minecraftRuntimeAccessed", true,
                    "liveRuntimeMutationSupported", true,
                    "realMinecraftProcess", true,
                    "liveRuntimeAccessMissing", true,
                    "agent5TruthGate", true
            );
        }

        @Override
        public void beginLiveRuntimeSurfaceDispatch(String surface, String dispatchId) {
            activeDispatchIds.put(surface, dispatchId == null ? "" : dispatchId);
        }

        @Override
        public EchoNativeLoadStatus registerCommand(
                String moduleId,
                String commandId,
                String targetSurface,
                String targetBridge,
                Map<String, Object> evidence
        ) {
            stampDispatch(evidence, "commands", "command:" + moduleId + ":" + commandId);
            return EchoNativeLoadStatus.MUTATED;
        }

        @Override
        public EchoNativeLoadStatus registerNetworkPacket(
                String moduleId,
                String packetId,
                String surface,
                String sourceRuntimeTarget,
                List<String> consumers,
                Map<String, Object> evidence
        ) {
            stampDispatch(evidence, "network_channels", "network:" + moduleId + ":" + packetId);
            return EchoNativeLoadStatus.MUTATED;
        }

        @Override
        public EchoNativeLoadStatus reloadConfig(
                String moduleId,
                String configId,
                String scope,
                Map<String, Object> evidence
        ) {
            stampDispatch(evidence, "config_reloads", "config:" + moduleId + ":" + configId);
            return EchoNativeLoadStatus.MUTATED;
        }

        @Override
        public EchoNativeLoadStatus lifecyclePhase(String moduleId, String phaseId, Map<String, Object> evidence) {
            stampDispatch(evidence, "lifecycle_phases", "lifecycle:" + moduleId + ":" + phaseId);
            return EchoNativeLoadStatus.MUTATED;
        }

        @Override
        public EchoNativeLoadStatus publishRuntimeEvent(
                String sourceModule,
                String eventId,
                Map<String, Object> payload,
                EchoNativeLoadStatus status
        ) {
            stampDispatch(payload, "events", "event:" + sourceModule + ":" + eventId);
            return EchoNativeLoadStatus.MUTATED;
        }

        private void stampDispatch(Map<String, Object> evidence, String surface, String operation) {
            if (evidence == null) {
                return;
            }
            evidence.put("liveRuntimeDispatchProofSatisfied", true);
            evidence.put("liveRuntimeDispatchMinecraftAccessed", true);
            evidence.put("liveRuntimeDispatchMutationSupported", true);
            evidence.put("liveRuntimeDispatchLiveMutation", true);
            evidence.put("liveRuntimeDispatchId", activeDispatchIds.getOrDefault(surface, ""));
            evidence.put("liveRuntimeDispatchBridgeId", bridgeId());
            evidence.put("liveRuntimeDispatchOperation", operation);
            evidence.put("liveRuntimeSurface", surface);
            evidence.put("runtimeSurfaceSaveTouched", true);
            evidence.put("runtimeSurfaceSaveMutated", true);
            evidence.put("runtimeSaveDataTouched", true);
            evidence.put("liveSaveDataFileTouched", true);
            evidence.put("runtimeSaveDataBackend", "world_save_file");
            evidence.put("saveFile", "agent5/live-runtime-inaccessible/" + operation.replace(':', '_') + ".properties");
            if ("network_channels".equals(surface)) {
                evidence.put("runtimeSurfacePacketSent", true);
                evidence.put("runtimeSurfacePacketMutated", true);
            }
            if ("events".equals(surface)) {
                evidence.put("runtimeSurfaceEventPublished", true);
                evidence.put("runtimeSurfaceEventMutated", true);
            }
        }
    }

    private static final class LiveRuntimeInaccessibleDirectSurfaceBridge implements Agent5LiveRuntimeAccessBridge {
        private final Map<String, String> activeDispatchIds = new java.util.LinkedHashMap<>();
        private final Map<String, Map<String, Object>> surfaceEvidence = new java.util.LinkedHashMap<>();

        @Override
        public boolean attached() {
            return true;
        }

        @Override
        public String bridgeId() {
            return "agent5:live_runtime_inaccessible_direct_surface_bridge";
        }

        @Override
        public boolean liveRuntimeAccessed() {
            return false;
        }

        @Override
        public boolean minecraftRuntimeAccessed() {
            return true;
        }

        @Override
        public boolean liveRuntimeMutationSupported() {
            return true;
        }

        @Override
        public Map<String, Object> runtimeEvidence() {
            return Map.of(
                    "bridgeId", bridgeId(),
                    "attached", true,
                    "liveRuntimeAccessed", false,
                    "minecraftRuntimeAccessed", true,
                    "liveRuntimeMutationSupported", true,
                    "realMinecraftProcess", true,
                    "liveRuntimeAccessMissing", true,
                    "agent5TruthGate", true
            );
        }

        @Override
        public void beginLiveRuntimeSurfaceDispatch(String surface, String dispatchId) {
            activeDispatchIds.put(surface, dispatchId == null ? "" : dispatchId);
            surfaceEvidence.remove(surface);
        }

        @Override
        public Map<String, Object> liveRuntimeSurfaceEvidence(String surface) {
            return surfaceEvidence.getOrDefault(surface, Map.of());
        }

        @Override
        public EchoNativeLoadStatus grantItem(String playerId, String itemId, int count) {
            Map<String, Object> evidence = new java.util.LinkedHashMap<>();
            evidence.put("liveRuntimeDispatchProofSatisfied", true);
            evidence.put("liveRuntimeDispatchMinecraftAccessed", true);
            evidence.put("liveRuntimeDispatchMutationSupported", true);
            evidence.put("liveRuntimeDispatchLiveMutation", true);
            evidence.put("liveRuntimeDispatchId", activeDispatchIds.getOrDefault("inventory", ""));
            evidence.put("liveRuntimeDispatchBridgeId", bridgeId());
            evidence.put("liveRuntimeSurface", "inventory");
            evidence.put("runtimeInventoryTouched", true);
            evidence.put("runtimeInventoryMutated", true);
            surfaceEvidence.put("inventory", Map.copyOf(evidence));
            return EchoNativeLoadStatus.MUTATED;
        }
    }

    private static final class PartialMinecraftBridge implements Agent5LiveRuntimeAccessBridge {
        private final Map<String, Map<String, Object>> surfaceEvidence = new java.util.LinkedHashMap<>();
        private final Map<String, String> activeDispatchIds = new java.util.LinkedHashMap<>();

        @Override
        public boolean attached() {
            return true;
        }

        @Override
        public String bridgeId() {
            return "agent5:partial_minecraft_live_bridge";
        }

        @Override
        public boolean minecraftRuntimeAccessed() {
            return true;
        }

        @Override
        public boolean liveRuntimeMutationSupported() {
            return true;
        }

        @Override
        public Map<String, Object> runtimeEvidence() {
            return Map.of(
                    "bridgeId", bridgeId(),
                    "attached", true,
                    "liveRuntimeAccessed", true,
                    "minecraftRuntimeAccessed", true,
                    "liveRuntimeMutationSupported", true,
                    "realMinecraftProcess", true,
                    "partialSurfaceCoverage", true,
                    "agent5TruthGate", true
            );
        }

        @Override
        public Map<String, Object> liveRuntimeSurfaceEvidence(String surface) {
            return surfaceEvidence.getOrDefault(surface, Map.of());
        }

        @Override
        public void beginLiveRuntimeSurfaceDispatch(String surface, String dispatchId) {
            surfaceEvidence.remove(surface);
            activeDispatchIds.put(surface, dispatchId);
        }

        @Override
        public EchoNativeLoadStatus grantItem(String playerId, String itemId, int count) {
            Map<String, Object> evidence = new java.util.LinkedHashMap<>();
            evidence.put("liveRuntimeDispatchProofSatisfied", true);
            evidence.put("liveRuntimeDispatchMinecraftAccessed", true);
            evidence.put("liveRuntimeDispatchMutationSupported", true);
            evidence.put("liveRuntimeDispatchLiveMutation", true);
            evidence.put("liveRuntimeDispatchId", activeDispatchIds.getOrDefault("inventory", ""));
            evidence.put("liveRuntimeDispatchBridgeId", bridgeId());
            evidence.put("liveRuntimeDispatchOperation", "inventory:" + playerId + ":" + itemId + ":" + count);
            evidence.put("liveRuntimeSurface", "inventory");
            evidence.put("runtimeInventoryTouched", true);
            evidence.put("runtimeInventoryMutated", true);
            surfaceEvidence.put("inventory", Map.copyOf(evidence));
            return EchoNativeLoadStatus.MUTATED;
        }
    }

    private static final class UnstampedMinecraftSubsystemBridge implements Agent5LiveRuntimeAccessBridge {
        @Override
        public boolean attached() {
            return true;
        }

        @Override
        public String bridgeId() {
            return "agent5:unstamped_minecraft_subsystem_bridge";
        }

        @Override
        public boolean minecraftRuntimeAccessed() {
            return true;
        }

        @Override
        public boolean liveRuntimeMutationSupported() {
            return true;
        }

        @Override
        public Map<String, Object> runtimeEvidence() {
            return Map.of(
                    "bridgeId", bridgeId(),
                    "attached", true,
                    "liveRuntimeAccessed", true,
                    "minecraftRuntimeAccessed", true,
                    "liveRuntimeMutationSupported", true,
                    "realMinecraftProcess", true,
                    "perDispatchProofMissing", true,
                    "agent5TruthGate", true
            );
        }

        @Override
        public EchoNativeLoadStatus registerCommand(
                String moduleId,
                String commandId,
                String targetSurface,
                String targetBridge,
                Map<String, Object> evidence
        ) {
            return EchoNativeLoadStatus.MUTATED;
        }

        @Override
        public EchoNativeLoadStatus registerNetworkPacket(
                String moduleId,
                String packetId,
                String surface,
                String sourceRuntimeTarget,
                List<String> consumers,
                Map<String, Object> evidence
        ) {
            return EchoNativeLoadStatus.MUTATED;
        }

        @Override
        public EchoNativeLoadStatus reloadConfig(
                String moduleId,
                String configId,
                String scope,
                Map<String, Object> evidence
        ) {
            return EchoNativeLoadStatus.MUTATED;
        }

        @Override
        public EchoNativeLoadStatus lifecyclePhase(String moduleId, String phaseId, Map<String, Object> evidence) {
            return EchoNativeLoadStatus.MUTATED;
        }

        @Override
        public EchoNativeLoadStatus publishRuntimeEvent(
                String sourceModule,
                String eventId,
                Map<String, Object> payload,
                EchoNativeLoadStatus status
        ) {
            return EchoNativeLoadStatus.MUTATED;
        }
    }

    private static final class StaleSubsystemDispatchBridge implements Agent5LiveRuntimeAccessBridge {
        @Override
        public boolean attached() {
            return true;
        }

        @Override
        public String bridgeId() {
            return "agent5:stale_subsystem_dispatch_bridge";
        }

        @Override
        public boolean minecraftRuntimeAccessed() {
            return true;
        }

        @Override
        public boolean liveRuntimeMutationSupported() {
            return true;
        }

        @Override
        public Map<String, Object> runtimeEvidence() {
            return Map.of(
                    "bridgeId", bridgeId(),
                    "attached", true,
                    "liveRuntimeAccessed", true,
                    "minecraftRuntimeAccessed", true,
                    "liveRuntimeMutationSupported", true,
                    "realMinecraftProcess", true,
                    "staleDispatchId", true,
                    "agent5TruthGate", true
            );
        }

        @Override
        public EchoNativeLoadStatus registerCommand(
                String moduleId,
                String commandId,
                String targetSurface,
                String targetBridge,
                Map<String, Object> evidence
        ) {
            stampStaleDispatch(evidence, "command:" + moduleId + ":" + commandId);
            return EchoNativeLoadStatus.MUTATED;
        }

        @Override
        public EchoNativeLoadStatus registerNetworkPacket(
                String moduleId,
                String packetId,
                String surface,
                String sourceRuntimeTarget,
                List<String> consumers,
                Map<String, Object> evidence
        ) {
            stampStaleDispatch(evidence, "network:" + moduleId + ":" + packetId);
            return EchoNativeLoadStatus.MUTATED;
        }

        @Override
        public EchoNativeLoadStatus reloadConfig(
                String moduleId,
                String configId,
                String scope,
                Map<String, Object> evidence
        ) {
            stampStaleDispatch(evidence, "config:" + moduleId + ":" + configId);
            return EchoNativeLoadStatus.MUTATED;
        }

        @Override
        public EchoNativeLoadStatus lifecyclePhase(String moduleId, String phaseId, Map<String, Object> evidence) {
            stampStaleDispatch(evidence, "lifecycle:" + moduleId + ":" + phaseId);
            return EchoNativeLoadStatus.MUTATED;
        }

        @Override
        public EchoNativeLoadStatus publishRuntimeEvent(
                String sourceModule,
                String eventId,
                Map<String, Object> payload,
                EchoNativeLoadStatus status
        ) {
            stampStaleDispatch(payload, "event:" + sourceModule + ":" + eventId);
            return EchoNativeLoadStatus.MUTATED;
        }

        private void stampStaleDispatch(Map<String, Object> evidence, String operation) {
            if (evidence == null) {
                return;
            }
            evidence.put("liveRuntimeDispatchProofSatisfied", true);
            evidence.put("liveRuntimeDispatchMinecraftAccessed", true);
            evidence.put("liveRuntimeDispatchMutationSupported", true);
            evidence.put("liveRuntimeDispatchLiveMutation", true);
            evidence.put("liveRuntimeDispatchId", "stale-subsystem-dispatch-id");
            evidence.put("liveRuntimeDispatchBridgeId", bridgeId());
            evidence.put("liveRuntimeDispatchOperation", operation);
        }
    }

    private static final class MissingSurfaceSubsystemDispatchBridge implements Agent5LiveRuntimeAccessBridge {
        private final Map<String, String> activeDispatchIds = new java.util.LinkedHashMap<>();

        @Override
        public boolean attached() {
            return true;
        }

        @Override
        public String bridgeId() {
            return "agent5:missing_surface_subsystem_dispatch_bridge";
        }

        @Override
        public boolean minecraftRuntimeAccessed() {
            return true;
        }

        @Override
        public boolean liveRuntimeMutationSupported() {
            return true;
        }

        @Override
        public Map<String, Object> runtimeEvidence() {
            return Map.of(
                    "bridgeId", bridgeId(),
                    "attached", true,
                    "liveRuntimeAccessed", true,
                    "minecraftRuntimeAccessed", true,
                    "liveRuntimeMutationSupported", true,
                    "realMinecraftProcess", true,
                    "missingSubsystemSurfaceEvidence", true,
                    "agent5TruthGate", true
            );
        }

        @Override
        public void beginLiveRuntimeSurfaceDispatch(String surface, String dispatchId) {
            activeDispatchIds.put(surface, dispatchId == null ? "" : dispatchId);
        }

        @Override
        public EchoNativeLoadStatus registerCommand(
                String moduleId,
                String commandId,
                String targetSurface,
                String targetBridge,
                Map<String, Object> evidence
        ) {
            stampDispatchWithoutSurface(evidence, "commands", "command:" + moduleId + ":" + commandId);
            return EchoNativeLoadStatus.MUTATED;
        }

        @Override
        public EchoNativeLoadStatus registerNetworkPacket(
                String moduleId,
                String packetId,
                String surface,
                String sourceRuntimeTarget,
                List<String> consumers,
                Map<String, Object> evidence
        ) {
            stampDispatchWithoutSurface(evidence, "network_channels", "network:" + moduleId + ":" + packetId);
            return EchoNativeLoadStatus.MUTATED;
        }

        @Override
        public EchoNativeLoadStatus reloadConfig(
                String moduleId,
                String configId,
                String scope,
                Map<String, Object> evidence
        ) {
            stampDispatchWithoutSurface(evidence, "config_reloads", "config:" + moduleId + ":" + configId);
            return EchoNativeLoadStatus.MUTATED;
        }

        @Override
        public EchoNativeLoadStatus lifecyclePhase(String moduleId, String phaseId, Map<String, Object> evidence) {
            stampDispatchWithoutSurface(evidence, "lifecycle_phases", "lifecycle:" + moduleId + ":" + phaseId);
            return EchoNativeLoadStatus.MUTATED;
        }

        @Override
        public EchoNativeLoadStatus publishRuntimeEvent(
                String sourceModule,
                String eventId,
                Map<String, Object> payload,
                EchoNativeLoadStatus status
        ) {
            stampDispatchWithoutSurface(payload, "events", "event:" + sourceModule + ":" + eventId);
            return EchoNativeLoadStatus.MUTATED;
        }

        private void stampDispatchWithoutSurface(Map<String, Object> evidence, String surface, String operation) {
            if (evidence == null) {
                return;
            }
            evidence.put("liveRuntimeDispatchProofSatisfied", true);
            evidence.put("liveRuntimeDispatchMinecraftAccessed", true);
            evidence.put("liveRuntimeDispatchMutationSupported", true);
            evidence.put("liveRuntimeDispatchLiveMutation", true);
            evidence.put("liveRuntimeDispatchId", activeDispatchIds.getOrDefault(surface, ""));
            evidence.put("liveRuntimeDispatchBridgeId", bridgeId());
            evidence.put("liveRuntimeDispatchOperation", operation);
        }
    }

    private static final class WrongSurfaceSubsystemDispatchBridge implements Agent5LiveRuntimeAccessBridge {
        private final Map<String, String> activeDispatchIds = new java.util.LinkedHashMap<>();

        @Override
        public boolean attached() {
            return true;
        }

        @Override
        public String bridgeId() {
            return "agent5:wrong_surface_subsystem_dispatch_bridge";
        }

        @Override
        public boolean minecraftRuntimeAccessed() {
            return true;
        }

        @Override
        public boolean liveRuntimeMutationSupported() {
            return true;
        }

        @Override
        public Map<String, Object> runtimeEvidence() {
            return Map.of(
                    "bridgeId", bridgeId(),
                    "attached", true,
                    "liveRuntimeAccessed", true,
                    "minecraftRuntimeAccessed", true,
                    "liveRuntimeMutationSupported", true,
                    "realMinecraftProcess", true,
                    "wrongSubsystemSurfaceEvidence", true,
                    "agent5TruthGate", true
            );
        }

        @Override
        public void beginLiveRuntimeSurfaceDispatch(String surface, String dispatchId) {
            activeDispatchIds.put(surface, dispatchId == null ? "" : dispatchId);
        }

        @Override
        public EchoNativeLoadStatus registerCommand(
                String moduleId,
                String commandId,
                String targetSurface,
                String targetBridge,
                Map<String, Object> evidence
        ) {
            stampDispatchWithWrongSurface(evidence, "commands", "network_channels", "command:" + moduleId + ":" + commandId);
            return EchoNativeLoadStatus.MUTATED;
        }

        @Override
        public EchoNativeLoadStatus registerNetworkPacket(
                String moduleId,
                String packetId,
                String surface,
                String sourceRuntimeTarget,
                List<String> consumers,
                Map<String, Object> evidence
        ) {
            stampDispatchWithWrongSurface(evidence, "network_channels", "config_reloads", "network:" + moduleId + ":" + packetId);
            return EchoNativeLoadStatus.MUTATED;
        }

        @Override
        public EchoNativeLoadStatus reloadConfig(
                String moduleId,
                String configId,
                String scope,
                Map<String, Object> evidence
        ) {
            stampDispatchWithWrongSurface(evidence, "config_reloads", "commands", "config:" + moduleId + ":" + configId);
            return EchoNativeLoadStatus.MUTATED;
        }

        @Override
        public EchoNativeLoadStatus lifecyclePhase(String moduleId, String phaseId, Map<String, Object> evidence) {
            stampDispatchWithWrongSurface(evidence, "lifecycle_phases", "events", "lifecycle:" + moduleId + ":" + phaseId);
            return EchoNativeLoadStatus.MUTATED;
        }

        @Override
        public EchoNativeLoadStatus publishRuntimeEvent(
                String sourceModule,
                String eventId,
                Map<String, Object> payload,
                EchoNativeLoadStatus status
        ) {
            stampDispatchWithWrongSurface(payload, "events", "lifecycle_phases", "event:" + sourceModule + ":" + eventId);
            return EchoNativeLoadStatus.MUTATED;
        }

        private void stampDispatchWithWrongSurface(
                Map<String, Object> evidence,
                String actualSurface,
                String wrongSurface,
                String operation
        ) {
            if (evidence == null) {
                return;
            }
            evidence.put("liveRuntimeDispatchProofSatisfied", true);
            evidence.put("liveRuntimeDispatchMinecraftAccessed", true);
            evidence.put("liveRuntimeDispatchMutationSupported", true);
            evidence.put("liveRuntimeDispatchLiveMutation", true);
            evidence.put("liveRuntimeDispatchId", activeDispatchIds.getOrDefault(actualSurface, ""));
            evidence.put("liveRuntimeDispatchBridgeId", bridgeId());
            evidence.put("liveRuntimeDispatchOperation", operation);
            evidence.put("liveRuntimeSurface", wrongSurface);
        }
    }

    private static final class MissingSubsystemSideEffectEvidenceBridge implements Agent5LiveRuntimeAccessBridge {
        private final Map<String, String> activeDispatchIds = new java.util.LinkedHashMap<>();

        @Override
        public boolean attached() {
            return true;
        }

        @Override
        public String bridgeId() {
            return "agent5:missing_subsystem_side_effect_evidence_bridge";
        }

        @Override
        public boolean minecraftRuntimeAccessed() {
            return true;
        }

        @Override
        public boolean liveRuntimeMutationSupported() {
            return true;
        }

        @Override
        public Map<String, Object> runtimeEvidence() {
            return Map.of(
                    "bridgeId", bridgeId(),
                    "attached", true,
                    "liveRuntimeAccessed", true,
                    "minecraftRuntimeAccessed", true,
                    "liveRuntimeMutationSupported", true,
                    "realMinecraftProcess", true,
                    "missingSubsystemSideEffectEvidence", true,
                    "agent5TruthGate", true
            );
        }

        @Override
        public void beginLiveRuntimeSurfaceDispatch(String surface, String dispatchId) {
            activeDispatchIds.put(surface, dispatchId == null ? "" : dispatchId);
        }

        @Override
        public EchoNativeLoadStatus registerCommand(
                String moduleId,
                String commandId,
                String targetSurface,
                String targetBridge,
                Map<String, Object> evidence
        ) {
            stampGenericDispatch(evidence, "commands", "command:" + moduleId + ":" + commandId);
            return EchoNativeLoadStatus.MUTATED;
        }

        @Override
        public EchoNativeLoadStatus registerNetworkPacket(
                String moduleId,
                String packetId,
                String surface,
                String sourceRuntimeTarget,
                List<String> consumers,
                Map<String, Object> evidence
        ) {
            stampGenericDispatch(evidence, "network_channels", "network:" + moduleId + ":" + packetId);
            return EchoNativeLoadStatus.MUTATED;
        }

        @Override
        public EchoNativeLoadStatus reloadConfig(
                String moduleId,
                String configId,
                String scope,
                Map<String, Object> evidence
        ) {
            stampGenericDispatch(evidence, "config_reloads", "config:" + moduleId + ":" + configId);
            return EchoNativeLoadStatus.MUTATED;
        }

        @Override
        public EchoNativeLoadStatus lifecyclePhase(String moduleId, String phaseId, Map<String, Object> evidence) {
            stampGenericDispatch(evidence, "lifecycle_phases", "lifecycle:" + moduleId + ":" + phaseId);
            return EchoNativeLoadStatus.MUTATED;
        }

        @Override
        public EchoNativeLoadStatus publishRuntimeEvent(
                String sourceModule,
                String eventId,
                Map<String, Object> payload,
                EchoNativeLoadStatus status
        ) {
            stampGenericDispatch(payload, "events", "event:" + sourceModule + ":" + eventId);
            return EchoNativeLoadStatus.MUTATED;
        }

        private void stampGenericDispatch(Map<String, Object> evidence, String surface, String operation) {
            if (evidence == null) {
                return;
            }
            evidence.put("liveRuntimeDispatchProofSatisfied", true);
            evidence.put("liveRuntimeDispatchMinecraftAccessed", true);
            evidence.put("liveRuntimeDispatchMutationSupported", true);
            evidence.put("liveRuntimeDispatchLiveMutation", true);
            evidence.put("liveRuntimeDispatchId", activeDispatchIds.getOrDefault(surface, ""));
            evidence.put("liveRuntimeDispatchBridgeId", bridgeId());
            evidence.put("liveRuntimeDispatchOperation", operation);
            evidence.put("liveRuntimeSurface", surface);
        }
    }

    private static final class UnstampedDirectSurfaceMinecraftBridge implements Agent5LiveRuntimeAccessBridge {
        @Override
        public boolean attached() {
            return true;
        }

        @Override
        public String bridgeId() {
            return "agent5:unstamped_direct_surface_minecraft_bridge";
        }

        @Override
        public boolean minecraftRuntimeAccessed() {
            return true;
        }

        @Override
        public boolean liveRuntimeMutationSupported() {
            return true;
        }

        @Override
        public Map<String, Object> runtimeEvidence() {
            return Map.of(
                    "bridgeId", bridgeId(),
                    "attached", true,
                    "liveRuntimeAccessed", true,
                    "minecraftRuntimeAccessed", true,
                    "liveRuntimeMutationSupported", true,
                    "realMinecraftProcess", true,
                    "perDispatchProofMissing", true,
                    "agent5TruthGate", true
            );
        }

        @Override
        public EchoNativeLoadStatus grantItem(String playerId, String itemId, int count) {
            return EchoNativeLoadStatus.MUTATED;
        }

        @Override
        public EchoNativeLoadStatus clientTick(String phase, Map<String, Object> payload) {
            return EchoNativeLoadStatus.MUTATED;
        }
    }

    private static final class StaleDirectSurfaceEvidenceMinecraftBridge implements Agent5LiveRuntimeAccessBridge {
        private final Map<String, Object> staleInventoryEvidence = Map.of(
                "liveRuntimeDispatchProofSatisfied", true,
                "liveRuntimeDispatchMinecraftAccessed", true,
                "liveRuntimeDispatchMutationSupported", true,
                "liveRuntimeDispatchLiveMutation", true,
                "liveRuntimeDispatchId", "stale-dispatch-id",
                "liveRuntimeDispatchBridgeId", "agent5:stale_direct_surface_evidence_bridge",
                "liveRuntimeSurface", "inventory"
        );

        @Override
        public boolean attached() {
            return true;
        }

        @Override
        public String bridgeId() {
            return "agent5:stale_direct_surface_evidence_bridge";
        }

        @Override
        public boolean minecraftRuntimeAccessed() {
            return true;
        }

        @Override
        public boolean liveRuntimeMutationSupported() {
            return true;
        }

        @Override
        public Map<String, Object> runtimeEvidence() {
            return Map.of(
                    "bridgeId", bridgeId(),
                    "attached", true,
                    "liveRuntimeAccessed", true,
                    "minecraftRuntimeAccessed", true,
                    "liveRuntimeMutationSupported", true,
                    "realMinecraftProcess", true,
                    "staleSurfaceEvidence", true,
                    "agent5TruthGate", true
            );
        }

        @Override
        public Map<String, Object> liveRuntimeSurfaceEvidence(String surface) {
            return "inventory".equals(surface) ? staleInventoryEvidence : Map.of();
        }

        @Override
        public EchoNativeLoadStatus grantItem(String playerId, String itemId, int count) {
            return EchoNativeLoadStatus.MUTATED;
        }
    }

    private static final class WrongSurfaceDirectEvidenceMinecraftBridge implements Agent5LiveRuntimeAccessBridge {
        private String inventoryDispatchId = "";

        @Override
        public boolean attached() {
            return true;
        }

        @Override
        public String bridgeId() {
            return "agent5:wrong_surface_direct_evidence_bridge";
        }

        @Override
        public boolean minecraftRuntimeAccessed() {
            return true;
        }

        @Override
        public boolean liveRuntimeMutationSupported() {
            return true;
        }

        @Override
        public Map<String, Object> runtimeEvidence() {
            return Map.of(
                    "bridgeId", bridgeId(),
                    "attached", true,
                    "liveRuntimeAccessed", true,
                    "minecraftRuntimeAccessed", true,
                    "liveRuntimeMutationSupported", true,
                    "realMinecraftProcess", true,
                    "wrongSurfaceEvidence", true,
                    "agent5TruthGate", true
            );
        }

        @Override
        public void beginLiveRuntimeSurfaceDispatch(String surface, String dispatchId) {
            if ("inventory".equals(surface)) {
                inventoryDispatchId = dispatchId == null ? "" : dispatchId;
            }
        }

        @Override
        public Map<String, Object> liveRuntimeSurfaceEvidence(String surface) {
            if (!"inventory".equals(surface)) {
                return Map.of();
            }
            return Map.of(
                    "liveRuntimeDispatchProofSatisfied", true,
                    "liveRuntimeDispatchMinecraftAccessed", true,
                    "liveRuntimeDispatchMutationSupported", true,
                    "liveRuntimeDispatchLiveMutation", true,
                    "liveRuntimeDispatchId", inventoryDispatchId,
                    "liveRuntimeDispatchBridgeId", bridgeId(),
                    "liveRuntimeSurface", "resource_reloads"
            );
        }

        @Override
        public EchoNativeLoadStatus grantItem(String playerId, String itemId, int count) {
            return EchoNativeLoadStatus.MUTATED;
        }
    }

    private static final class MissingSurfaceDirectEvidenceMinecraftBridge implements Agent5LiveRuntimeAccessBridge {
        private String inventoryDispatchId = "";

        @Override
        public boolean attached() {
            return true;
        }

        @Override
        public String bridgeId() {
            return "agent5:missing_surface_direct_evidence_bridge";
        }

        @Override
        public boolean minecraftRuntimeAccessed() {
            return true;
        }

        @Override
        public boolean liveRuntimeMutationSupported() {
            return true;
        }

        @Override
        public Map<String, Object> runtimeEvidence() {
            return Map.of(
                    "bridgeId", bridgeId(),
                    "attached", true,
                    "liveRuntimeAccessed", true,
                    "minecraftRuntimeAccessed", true,
                    "liveRuntimeMutationSupported", true,
                    "realMinecraftProcess", true,
                    "missingSurfaceEvidence", true,
                    "agent5TruthGate", true
            );
        }

        @Override
        public void beginLiveRuntimeSurfaceDispatch(String surface, String dispatchId) {
            if ("inventory".equals(surface)) {
                inventoryDispatchId = dispatchId == null ? "" : dispatchId;
            }
        }

        @Override
        public Map<String, Object> liveRuntimeSurfaceEvidence(String surface) {
            if (!"inventory".equals(surface)) {
                return Map.of();
            }
            return Map.of(
                    "liveRuntimeDispatchProofSatisfied", true,
                    "liveRuntimeDispatchMinecraftAccessed", true,
                    "liveRuntimeDispatchMutationSupported", true,
                    "liveRuntimeDispatchLiveMutation", true,
                    "liveRuntimeDispatchId", inventoryDispatchId,
                    "liveRuntimeDispatchBridgeId", bridgeId()
            );
        }

        @Override
        public EchoNativeLoadStatus grantItem(String playerId, String itemId, int count) {
            return EchoNativeLoadStatus.MUTATED;
        }
    }

    private static final class MissingInventoryEvidenceMinecraftBridge implements Agent5LiveRuntimeAccessBridge {
        private String inventoryDispatchId = "";

        @Override
        public boolean attached() {
            return true;
        }

        @Override
        public String bridgeId() {
            return "agent5:missing_inventory_evidence_bridge";
        }

        @Override
        public boolean minecraftRuntimeAccessed() {
            return true;
        }

        @Override
        public boolean liveRuntimeMutationSupported() {
            return true;
        }

        @Override
        public Map<String, Object> runtimeEvidence() {
            return Map.of(
                    "bridgeId", bridgeId(),
                    "attached", true,
                    "liveRuntimeAccessed", true,
                    "minecraftRuntimeAccessed", true,
                    "liveRuntimeMutationSupported", true,
                    "realMinecraftProcess", true,
                    "missingInventoryEvidence", true,
                    "agent5TruthGate", true
            );
        }

        @Override
        public void beginLiveRuntimeSurfaceDispatch(String surface, String dispatchId) {
            if ("inventory".equals(surface)) {
                inventoryDispatchId = dispatchId == null ? "" : dispatchId;
            }
        }

        @Override
        public Map<String, Object> liveRuntimeSurfaceEvidence(String surface) {
            if (!"inventory".equals(surface)) {
                return Map.of();
            }
            return Map.of(
                    "liveRuntimeDispatchProofSatisfied", true,
                    "liveRuntimeDispatchMinecraftAccessed", true,
                    "liveRuntimeDispatchMutationSupported", true,
                    "liveRuntimeDispatchLiveMutation", true,
                    "liveRuntimeDispatchId", inventoryDispatchId,
                    "liveRuntimeDispatchBridgeId", bridgeId(),
                    "liveRuntimeSurface", surface
            );
        }

        @Override
        public EchoNativeLoadStatus grantItem(String playerId, String itemId, int count) {
            return EchoNativeLoadStatus.MUTATED;
        }
    }

    private static final class MissingPlayerStateEvidenceMinecraftBridge implements Agent5LiveRuntimeAccessBridge {
        private String playerStateDispatchId = "";

        @Override
        public boolean attached() {
            return true;
        }

        @Override
        public String bridgeId() {
            return "agent5:missing_player_state_evidence_bridge";
        }

        @Override
        public boolean minecraftRuntimeAccessed() {
            return true;
        }

        @Override
        public boolean liveRuntimeMutationSupported() {
            return true;
        }

        @Override
        public Map<String, Object> runtimeEvidence() {
            return Map.of(
                    "bridgeId", bridgeId(),
                    "attached", true,
                    "liveRuntimeAccessed", true,
                    "minecraftRuntimeAccessed", true,
                    "liveRuntimeMutationSupported", true,
                    "realMinecraftProcess", true,
                    "missingPlayerStateEvidence", true,
                    "agent5TruthGate", true
            );
        }

        @Override
        public void beginLiveRuntimeSurfaceDispatch(String surface, String dispatchId) {
            if ("player_state".equals(surface)) {
                playerStateDispatchId = dispatchId == null ? "" : dispatchId;
            }
        }

        @Override
        public Map<String, Object> liveRuntimeSurfaceEvidence(String surface) {
            if (!"player_state".equals(surface)) {
                return Map.of();
            }
            return Map.of(
                    "liveRuntimeDispatchProofSatisfied", true,
                    "liveRuntimeDispatchMinecraftAccessed", true,
                    "liveRuntimeDispatchMutationSupported", true,
                    "liveRuntimeDispatchLiveMutation", true,
                    "liveRuntimeDispatchId", playerStateDispatchId,
                    "liveRuntimeDispatchBridgeId", bridgeId(),
                    "liveRuntimeSurface", surface
            );
        }

        @Override
        public EchoNativeLoadStatus updatePlayerState(String playerId, String key, String value) {
            return EchoNativeLoadStatus.MUTATED;
        }
    }

    private static final class MissingMissionEvidenceMinecraftBridge implements Agent5LiveRuntimeAccessBridge {
        private String missionDispatchId = "";

        @Override
        public boolean attached() {
            return true;
        }

        @Override
        public String bridgeId() {
            return "agent5:missing_mission_evidence_bridge";
        }

        @Override
        public boolean minecraftRuntimeAccessed() {
            return true;
        }

        @Override
        public boolean liveRuntimeMutationSupported() {
            return true;
        }

        @Override
        public Map<String, Object> runtimeEvidence() {
            return Map.of(
                    "bridgeId", bridgeId(),
                    "attached", true,
                    "liveRuntimeAccessed", true,
                    "minecraftRuntimeAccessed", true,
                    "liveRuntimeMutationSupported", true,
                    "realMinecraftProcess", true,
                    "missingMissionEvidence", true,
                    "agent5TruthGate", true
            );
        }

        @Override
        public void beginLiveRuntimeSurfaceDispatch(String surface, String dispatchId) {
            if ("missions".equals(surface)) {
                missionDispatchId = dispatchId == null ? "" : dispatchId;
            }
        }

        @Override
        public Map<String, Object> liveRuntimeSurfaceEvidence(String surface) {
            if (!"missions".equals(surface)) {
                return Map.of();
            }
            return Map.of(
                    "liveRuntimeDispatchProofSatisfied", true,
                    "liveRuntimeDispatchMinecraftAccessed", true,
                    "liveRuntimeDispatchMutationSupported", true,
                    "liveRuntimeDispatchLiveMutation", true,
                    "liveRuntimeDispatchId", missionDispatchId,
                    "liveRuntimeDispatchBridgeId", bridgeId(),
                    "liveRuntimeSurface", surface
            );
        }

        @Override
        public EchoNativeLoadStatus updateMission(String missionId, String phase, String objectiveKey) {
            return EchoNativeLoadStatus.MUTATED;
        }
    }

    private static final class MissingWorldBlockEvidenceMinecraftBridge implements Agent5LiveRuntimeAccessBridge {
        private String worldBlockDispatchId = "";

        @Override
        public boolean attached() {
            return true;
        }

        @Override
        public String bridgeId() {
            return "agent5:missing_world_block_evidence_bridge";
        }

        @Override
        public boolean minecraftRuntimeAccessed() {
            return true;
        }

        @Override
        public boolean liveRuntimeMutationSupported() {
            return true;
        }

        @Override
        public Map<String, Object> runtimeEvidence() {
            return Map.of(
                    "bridgeId", bridgeId(),
                    "attached", true,
                    "liveRuntimeAccessed", true,
                    "minecraftRuntimeAccessed", true,
                    "liveRuntimeMutationSupported", true,
                    "realMinecraftProcess", true,
                    "missingWorldBlockEvidence", true,
                    "agent5TruthGate", true
            );
        }

        @Override
        public void beginLiveRuntimeSurfaceDispatch(String surface, String dispatchId) {
            if ("world_blocks".equals(surface)) {
                worldBlockDispatchId = dispatchId == null ? "" : dispatchId;
            }
        }

        @Override
        public Map<String, Object> liveRuntimeSurfaceEvidence(String surface) {
            if (!"world_blocks".equals(surface)) {
                return Map.of();
            }
            return Map.of(
                    "liveRuntimeDispatchProofSatisfied", true,
                    "liveRuntimeDispatchMinecraftAccessed", true,
                    "liveRuntimeDispatchMutationSupported", true,
                    "liveRuntimeDispatchLiveMutation", true,
                    "liveRuntimeDispatchId", worldBlockDispatchId,
                    "liveRuntimeDispatchBridgeId", bridgeId(),
                    "liveRuntimeSurface", surface
            );
        }

        @Override
        public EchoNativeLoadStatus placeBlock(String dimension, int x, int y, int z, String blockId) {
            return EchoNativeLoadStatus.MUTATED;
        }
    }

    private static final class MissingStructureEvidenceMinecraftBridge implements Agent5LiveRuntimeAccessBridge {
        private String structureDispatchId = "";

        @Override
        public boolean attached() {
            return true;
        }

        @Override
        public String bridgeId() {
            return "agent5:missing_structure_evidence_bridge";
        }

        @Override
        public boolean minecraftRuntimeAccessed() {
            return true;
        }

        @Override
        public boolean liveRuntimeMutationSupported() {
            return true;
        }

        @Override
        public Map<String, Object> runtimeEvidence() {
            return Map.of(
                    "bridgeId", bridgeId(),
                    "attached", true,
                    "liveRuntimeAccessed", true,
                    "minecraftRuntimeAccessed", true,
                    "liveRuntimeMutationSupported", true,
                    "realMinecraftProcess", true,
                    "missingStructureEvidence", true,
                    "agent5TruthGate", true
            );
        }

        @Override
        public void beginLiveRuntimeSurfaceDispatch(String surface, String dispatchId) {
            if ("structures".equals(surface)) {
                structureDispatchId = dispatchId == null ? "" : dispatchId;
            }
        }

        @Override
        public Map<String, Object> liveRuntimeSurfaceEvidence(String surface) {
            if (!"structures".equals(surface)) {
                return Map.of();
            }
            return Map.of(
                    "liveRuntimeDispatchProofSatisfied", true,
                    "liveRuntimeDispatchMinecraftAccessed", true,
                    "liveRuntimeDispatchMutationSupported", true,
                    "liveRuntimeDispatchLiveMutation", true,
                    "liveRuntimeDispatchId", structureDispatchId,
                    "liveRuntimeDispatchBridgeId", bridgeId(),
                    "liveRuntimeSurface", surface
            );
        }

        @Override
        public EchoNativeLoadStatus placeStructure(String dimension, String structureId, int x, int y, int z) {
            return EchoNativeLoadStatus.MUTATED;
        }
    }

    private static final class MissingBlockEntityEvidenceMinecraftBridge implements Agent5LiveRuntimeAccessBridge {
        private String blockEntityDispatchId = "";

        @Override
        public boolean attached() {
            return true;
        }

        @Override
        public String bridgeId() {
            return "agent5:missing_block_entity_evidence_bridge";
        }

        @Override
        public boolean minecraftRuntimeAccessed() {
            return true;
        }

        @Override
        public boolean liveRuntimeMutationSupported() {
            return true;
        }

        @Override
        public Map<String, Object> runtimeEvidence() {
            return Map.of(
                    "bridgeId", bridgeId(),
                    "attached", true,
                    "liveRuntimeAccessed", true,
                    "minecraftRuntimeAccessed", true,
                    "liveRuntimeMutationSupported", true,
                    "realMinecraftProcess", true,
                    "missingBlockEntityEvidence", true,
                    "agent5TruthGate", true
            );
        }

        @Override
        public void beginLiveRuntimeSurfaceDispatch(String surface, String dispatchId) {
            if ("block_entities".equals(surface)) {
                blockEntityDispatchId = dispatchId == null ? "" : dispatchId;
            }
        }

        @Override
        public Map<String, Object> liveRuntimeSurfaceEvidence(String surface) {
            if (!"block_entities".equals(surface)) {
                return Map.of();
            }
            return Map.of(
                    "liveRuntimeDispatchProofSatisfied", true,
                    "liveRuntimeDispatchMinecraftAccessed", true,
                    "liveRuntimeDispatchMutationSupported", true,
                    "liveRuntimeDispatchLiveMutation", true,
                    "liveRuntimeDispatchId", blockEntityDispatchId,
                    "liveRuntimeDispatchBridgeId", bridgeId(),
                    "liveRuntimeSurface", surface
            );
        }

        @Override
        public EchoNativeLoadStatus updateBlockEntity(String dimension, int x, int y, int z, String key, String value) {
            return EchoNativeLoadStatus.MUTATED;
        }
    }

    private static final class MissingCapabilityEvidenceMinecraftBridge implements Agent5LiveRuntimeAccessBridge {
        private String capabilityDispatchId = "";

        @Override
        public boolean attached() {
            return true;
        }

        @Override
        public String bridgeId() {
            return "agent5:missing_capability_evidence_bridge";
        }

        @Override
        public boolean minecraftRuntimeAccessed() {
            return true;
        }

        @Override
        public boolean liveRuntimeMutationSupported() {
            return true;
        }

        @Override
        public Map<String, Object> runtimeEvidence() {
            return Map.of(
                    "bridgeId", bridgeId(),
                    "attached", true,
                    "liveRuntimeAccessed", true,
                    "minecraftRuntimeAccessed", true,
                    "liveRuntimeMutationSupported", true,
                    "realMinecraftProcess", true,
                    "missingCapabilityEvidence", true,
                    "agent5TruthGate", true
            );
        }

        @Override
        public void beginLiveRuntimeSurfaceDispatch(String surface, String dispatchId) {
            if ("capabilities".equals(surface)) {
                capabilityDispatchId = dispatchId == null ? "" : dispatchId;
            }
        }

        @Override
        public Map<String, Object> liveRuntimeSurfaceEvidence(String surface) {
            if (!"capabilities".equals(surface)) {
                return Map.of();
            }
            return Map.of(
                    "liveRuntimeDispatchProofSatisfied", true,
                    "liveRuntimeDispatchMinecraftAccessed", true,
                    "liveRuntimeDispatchMutationSupported", true,
                    "liveRuntimeDispatchLiveMutation", true,
                    "liveRuntimeDispatchId", capabilityDispatchId,
                    "liveRuntimeDispatchBridgeId", bridgeId(),
                    "liveRuntimeSurface", surface
            );
        }

        @Override
        public EchoNativeLoadStatus updateCapability(String target, String capability, String value) {
            return EchoNativeLoadStatus.MUTATED;
        }
    }

    private static final class MissingSaveDataEvidenceMinecraftBridge implements Agent5LiveRuntimeAccessBridge {
        private final Map<String, String> dispatchIds = new LinkedHashMap<>();

        @Override
        public boolean attached() {
            return true;
        }

        @Override
        public String bridgeId() {
            return "agent5:missing_save_data_evidence_bridge";
        }

        @Override
        public boolean minecraftRuntimeAccessed() {
            return true;
        }

        @Override
        public boolean liveRuntimeMutationSupported() {
            return true;
        }

        @Override
        public Map<String, Object> runtimeEvidence() {
            return Map.of(
                    "bridgeId", bridgeId(),
                    "attached", true,
                    "liveRuntimeAccessed", true,
                    "minecraftRuntimeAccessed", true,
                    "liveRuntimeMutationSupported", true,
                    "realMinecraftProcess", true,
                    "missingSaveDataWorldSaveEvidence", true,
                    "agent5TruthGate", true
            );
        }

        @Override
        public void beginLiveRuntimeSurfaceDispatch(String surface, String dispatchId) {
            if ("save_data".equals(surface) || "save_hooks".equals(surface) || "world_state".equals(surface)) {
                dispatchIds.put(surface, dispatchId == null ? "" : dispatchId);
            }
        }

        @Override
        public Map<String, Object> liveRuntimeSurfaceEvidence(String surface) {
            if (!"save_data".equals(surface) && !"save_hooks".equals(surface) && !"world_state".equals(surface)) {
                return Map.of();
            }
            return Map.of(
                    "liveRuntimeDispatchProofSatisfied", true,
                    "liveRuntimeDispatchMinecraftAccessed", true,
                    "liveRuntimeDispatchMutationSupported", true,
                    "liveRuntimeDispatchLiveMutation", true,
                    "liveRuntimeDispatchId", dispatchIds.getOrDefault(surface, ""),
                    "liveRuntimeDispatchBridgeId", bridgeId(),
                    "liveRuntimeSurface", surface
            );
        }

        @Override
        public EchoNativeLoadStatus writeSaveData(String key, String value) {
            return EchoNativeLoadStatus.MUTATED;
        }

        @Override
        public EchoNativeLoadStatus saveHook(String hookId, Map<String, Object> payload) {
            return EchoNativeLoadStatus.MUTATED;
        }

        @Override
        public EchoNativeLoadStatus updateWorldState(String dimension, String key, String value) {
            return EchoNativeLoadStatus.MUTATED;
        }
    }

    private static final class MissingResourceReloadEvidenceMinecraftBridge implements Agent5LiveRuntimeAccessBridge {
        private String resourceReloadDispatchId = "";

        @Override
        public boolean attached() {
            return true;
        }

        @Override
        public String bridgeId() {
            return "agent5:missing_resource_reload_evidence_bridge";
        }

        @Override
        public boolean minecraftRuntimeAccessed() {
            return true;
        }

        @Override
        public boolean liveRuntimeMutationSupported() {
            return true;
        }

        @Override
        public Map<String, Object> runtimeEvidence() {
            return Map.of(
                    "bridgeId", bridgeId(),
                    "attached", true,
                    "liveRuntimeAccessed", true,
                    "minecraftRuntimeAccessed", true,
                    "liveRuntimeMutationSupported", true,
                    "realMinecraftProcess", true,
                    "missingResourceReloadSaveEvidence", true,
                    "agent5TruthGate", true
            );
        }

        @Override
        public void beginLiveRuntimeSurfaceDispatch(String surface, String dispatchId) {
            if ("resource_reloads".equals(surface)) {
                resourceReloadDispatchId = dispatchId == null ? "" : dispatchId;
            }
        }

        @Override
        public Map<String, Object> liveRuntimeSurfaceEvidence(String surface) {
            if (!"resource_reloads".equals(surface)) {
                return Map.of();
            }
            return Map.of(
                    "liveRuntimeDispatchProofSatisfied", true,
                    "liveRuntimeDispatchMinecraftAccessed", true,
                    "liveRuntimeDispatchMutationSupported", true,
                    "liveRuntimeDispatchLiveMutation", true,
                    "liveRuntimeDispatchId", resourceReloadDispatchId,
                    "liveRuntimeDispatchBridgeId", bridgeId(),
                    "liveRuntimeSurface", surface
            );
        }

        @Override
        public EchoNativeLoadStatus reloadResources(
                String moduleId,
                String resourceId,
                String scope,
                Map<String, Object> evidence
        ) {
            return EchoNativeLoadStatus.MUTATED;
        }
    }

    private static final class MissingServerClientSyncPacketEvidenceMinecraftBridge implements Agent5LiveRuntimeAccessBridge {
        private String serverClientSyncDispatchId = "";

        @Override
        public boolean attached() {
            return true;
        }

        @Override
        public String bridgeId() {
            return "agent5:missing_server_client_sync_packet_evidence_bridge";
        }

        @Override
        public boolean minecraftRuntimeAccessed() {
            return true;
        }

        @Override
        public boolean liveRuntimeMutationSupported() {
            return true;
        }

        @Override
        public Map<String, Object> runtimeEvidence() {
            return Map.of(
                    "bridgeId", bridgeId(),
                    "attached", true,
                    "liveRuntimeAccessed", true,
                    "minecraftRuntimeAccessed", true,
                    "liveRuntimeMutationSupported", true,
                    "realMinecraftProcess", true,
                    "missingServerClientSyncPacketEvidence", true,
                    "agent5TruthGate", true
            );
        }

        @Override
        public void beginLiveRuntimeSurfaceDispatch(String surface, String dispatchId) {
            if ("server_client_sync".equals(surface)) {
                serverClientSyncDispatchId = dispatchId == null ? "" : dispatchId;
            }
        }

        @Override
        public Map<String, Object> liveRuntimeSurfaceEvidence(String surface) {
            if (!"server_client_sync".equals(surface)) {
                return Map.of();
            }
            return Map.of(
                    "liveRuntimeDispatchProofSatisfied", true,
                    "liveRuntimeDispatchMinecraftAccessed", true,
                    "liveRuntimeDispatchMutationSupported", true,
                    "liveRuntimeDispatchLiveMutation", true,
                    "liveRuntimeDispatchId", serverClientSyncDispatchId,
                    "liveRuntimeDispatchBridgeId", bridgeId(),
                    "liveRuntimeSurface", surface
            );
        }

        @Override
        public EchoNativeLoadStatus syncServerClient(String channel, String payload) {
            return EchoNativeLoadStatus.MUTATED;
        }
    }

    private static final class MissingPacketHudEvidenceMinecraftBridge implements Agent5LiveRuntimeAccessBridge {
        private String packetHudDispatchId = "";

        @Override
        public boolean attached() {
            return true;
        }

        @Override
        public String bridgeId() {
            return "agent5:missing_packet_hud_evidence_bridge";
        }

        @Override
        public boolean minecraftRuntimeAccessed() {
            return true;
        }

        @Override
        public boolean liveRuntimeMutationSupported() {
            return true;
        }

        @Override
        public Map<String, Object> runtimeEvidence() {
            return Map.of(
                    "bridgeId", bridgeId(),
                    "attached", true,
                    "liveRuntimeAccessed", true,
                    "minecraftRuntimeAccessed", true,
                    "liveRuntimeMutationSupported", true,
                    "realMinecraftProcess", true,
                    "missingPacketHudEvidence", true,
                    "agent5TruthGate", true
            );
        }

        @Override
        public void beginLiveRuntimeSurfaceDispatch(String surface, String dispatchId) {
            if ("packets_hud".equals(surface)) {
                packetHudDispatchId = dispatchId == null ? "" : dispatchId;
            }
        }

        @Override
        public Map<String, Object> liveRuntimeSurfaceEvidence(String surface) {
            if (!"packets_hud".equals(surface)) {
                return Map.of();
            }
            return Map.of(
                    "liveRuntimeDispatchProofSatisfied", true,
                    "liveRuntimeDispatchMinecraftAccessed", true,
                    "liveRuntimeDispatchMutationSupported", true,
                    "liveRuntimeDispatchLiveMutation", true,
                    "liveRuntimeDispatchId", packetHudDispatchId,
                    "liveRuntimeDispatchBridgeId", bridgeId(),
                    "liveRuntimeSurface", surface
            );
        }

        @Override
        public EchoNativeLoadStatus sendPacketHud(String channel, String payload) {
            return EchoNativeLoadStatus.MUTATED;
        }
    }

    private static final class MissingEventEvidenceMinecraftBridge implements Agent5LiveRuntimeAccessBridge {
        private String eventDispatchId = "";

        @Override
        public boolean attached() {
            return true;
        }

        @Override
        public String bridgeId() {
            return "agent5:missing_event_evidence_bridge";
        }

        @Override
        public boolean minecraftRuntimeAccessed() {
            return true;
        }

        @Override
        public boolean liveRuntimeMutationSupported() {
            return true;
        }

        @Override
        public Map<String, Object> runtimeEvidence() {
            return Map.of(
                    "bridgeId", bridgeId(),
                    "attached", true,
                    "liveRuntimeAccessed", true,
                    "minecraftRuntimeAccessed", true,
                    "liveRuntimeMutationSupported", true,
                    "realMinecraftProcess", true,
                    "missingEventEvidence", true,
                    "agent5TruthGate", true
            );
        }

        @Override
        public void beginLiveRuntimeSurfaceDispatch(String surface, String dispatchId) {
            if ("events".equals(surface)) {
                eventDispatchId = dispatchId == null ? "" : dispatchId;
            }
        }

        @Override
        public Map<String, Object> liveRuntimeSurfaceEvidence(String surface) {
            if (!"events".equals(surface)) {
                return Map.of();
            }
            return Map.of(
                    "liveRuntimeDispatchProofSatisfied", true,
                    "liveRuntimeDispatchMinecraftAccessed", true,
                    "liveRuntimeDispatchMutationSupported", true,
                    "liveRuntimeDispatchLiveMutation", true,
                    "liveRuntimeDispatchId", eventDispatchId,
                    "liveRuntimeDispatchBridgeId", bridgeId(),
                    "liveRuntimeSurface", surface
            );
        }

        @Override
        public EchoNativeLoadStatus emitEvent(String eventType, String payload) {
            return EchoNativeLoadStatus.MUTATED;
        }
    }

    private static final class MissingClientSurfaceSaveEvidenceMinecraftBridge implements Agent5LiveRuntimeAccessBridge {
        private String clientTickDispatchId = "";

        @Override
        public boolean attached() {
            return true;
        }

        @Override
        public String bridgeId() {
            return "agent5:missing_client_surface_save_evidence_bridge";
        }

        @Override
        public boolean minecraftRuntimeAccessed() {
            return true;
        }

        @Override
        public boolean liveRuntimeMutationSupported() {
            return true;
        }

        @Override
        public Map<String, Object> runtimeEvidence() {
            return Map.of(
                    "bridgeId", bridgeId(),
                    "attached", true,
                    "liveRuntimeAccessed", true,
                    "minecraftRuntimeAccessed", true,
                    "liveRuntimeMutationSupported", true,
                    "realMinecraftProcess", true,
                    "missingClientSurfaceSaveEvidence", true,
                    "agent5TruthGate", true
            );
        }

        @Override
        public void beginLiveRuntimeSurfaceDispatch(String surface, String dispatchId) {
            if ("client_tick".equals(surface)) {
                clientTickDispatchId = dispatchId == null ? "" : dispatchId;
            }
        }

        @Override
        public Map<String, Object> liveRuntimeSurfaceEvidence(String surface) {
            if (!"client_tick".equals(surface)) {
                return Map.of();
            }
            return Map.of(
                    "liveRuntimeDispatchProofSatisfied", true,
                    "liveRuntimeDispatchMinecraftAccessed", true,
                    "liveRuntimeDispatchMutationSupported", true,
                    "liveRuntimeDispatchLiveMutation", true,
                    "liveRuntimeDispatchId", clientTickDispatchId,
                    "liveRuntimeDispatchBridgeId", bridgeId(),
                    "liveRuntimeSurface", surface
            );
        }

        @Override
        public EchoNativeLoadStatus clientTick(String phase, Map<String, Object> payload) {
            return EchoNativeLoadStatus.MUTATED;
        }
    }

    private static final class MissingHudEvidenceMinecraftBridge implements Agent5LiveRuntimeAccessBridge {
        private String hudDispatchId = "";

        @Override
        public boolean attached() {
            return true;
        }

        @Override
        public String bridgeId() {
            return "agent5:missing_hud_evidence_bridge";
        }

        @Override
        public boolean minecraftRuntimeAccessed() {
            return true;
        }

        @Override
        public boolean liveRuntimeMutationSupported() {
            return true;
        }

        @Override
        public Map<String, Object> runtimeEvidence() {
            return Map.of(
                    "bridgeId", bridgeId(),
                    "attached", true,
                    "liveRuntimeAccessed", true,
                    "minecraftRuntimeAccessed", true,
                    "liveRuntimeMutationSupported", true,
                    "realMinecraftProcess", true,
                    "missingHudEvidence", true,
                    "agent5TruthGate", true
            );
        }

        @Override
        public void beginLiveRuntimeSurfaceDispatch(String surface, String dispatchId) {
            if ("hud".equals(surface)) {
                hudDispatchId = dispatchId == null ? "" : dispatchId;
            }
        }

        @Override
        public Map<String, Object> liveRuntimeSurfaceEvidence(String surface) {
            if (!"hud".equals(surface)) {
                return Map.of();
            }
            return Map.of(
                    "liveRuntimeDispatchProofSatisfied", true,
                    "liveRuntimeDispatchMinecraftAccessed", true,
                    "liveRuntimeDispatchMutationSupported", true,
                    "liveRuntimeDispatchLiveMutation", true,
                    "liveRuntimeDispatchId", hudDispatchId,
                    "liveRuntimeDispatchBridgeId", bridgeId(),
                    "liveRuntimeSurface", surface
            );
        }

        @Override
        public EchoNativeLoadStatus emitHud(String channel, String message) {
            return EchoNativeLoadStatus.MUTATED;
        }
    }

    private static final class CallerSeededSurfaceDirectEvidenceMinecraftBridge implements Agent5LiveRuntimeAccessBridge {
        @Override
        public boolean attached() {
            return true;
        }

        @Override
        public String bridgeId() {
            return "agent5:caller_seeded_surface_direct_evidence_bridge";
        }

        @Override
        public boolean minecraftRuntimeAccessed() {
            return true;
        }

        @Override
        public boolean liveRuntimeMutationSupported() {
            return true;
        }

        @Override
        public Map<String, Object> runtimeEvidence() {
            return Map.of(
                    "bridgeId", bridgeId(),
                    "attached", true,
                    "liveRuntimeAccessed", true,
                    "minecraftRuntimeAccessed", true,
                    "liveRuntimeMutationSupported", true,
                    "realMinecraftProcess", true,
                    "callerSeededSurfaceEvidence", true,
                    "agent5TruthGate", true
            );
        }

        @Override
        public EchoNativeLoadStatus clientTick(String phase, Map<String, Object> payload) {
            if (payload != null) {
                payload.put("liveRuntimeDispatchProofSatisfied", true);
                payload.put("liveRuntimeDispatchMinecraftAccessed", true);
                payload.put("liveRuntimeDispatchMutationSupported", true);
                payload.put("liveRuntimeDispatchLiveMutation", true);
                payload.put("liveRuntimeDispatchBridgeId", bridgeId());
                payload.put("liveRuntimeDispatchOperation", "client_tick:" + phase);
            }
            return EchoNativeLoadStatus.MUTATED;
        }
    }

    private static final class PartialSubsystemMinecraftBridge implements Agent5LiveRuntimeAccessBridge {
        @Override
        public boolean attached() {
            return true;
        }

        @Override
        public String bridgeId() {
            return "agent5:partial_subsystem_minecraft_live_bridge";
        }

        @Override
        public boolean minecraftRuntimeAccessed() {
            return true;
        }

        @Override
        public boolean liveRuntimeMutationSupported() {
            return true;
        }

        @Override
        public Map<String, Object> runtimeEvidence() {
            return Map.of(
                    "bridgeId", bridgeId(),
                    "attached", true,
                    "liveRuntimeAccessed", true,
                    "minecraftRuntimeAccessed", true,
                    "liveRuntimeMutationSupported", true,
                    "realMinecraftProcess", true,
                    "partialSubsystemCoverage", true,
                    "agent5TruthGate", true
            );
        }

        @Override
        public EchoNativeLoadStatus registerCommand(
                String moduleId,
                String commandId,
                String targetSurface,
                String targetBridge,
                Map<String, Object> evidence
        ) {
            if (commandId != null && commandId.endsWith(".live")) {
                stampLiveDispatch(evidence, "command:" + moduleId + ":" + commandId);
                return EchoNativeLoadStatus.MUTATED;
            }
            return EchoNativeLoadStatus.REGISTERED;
        }

        @Override
        public EchoNativeLoadStatus registerNetworkPacket(
                String moduleId,
                String packetId,
                String surface,
                String sourceRuntimeTarget,
                List<String> consumers,
                Map<String, Object> evidence
        ) {
            if (packetId != null && packetId.contains("partial_live")) {
                stampLiveDispatch(evidence, "network:" + moduleId + ":" + packetId);
                return EchoNativeLoadStatus.MUTATED;
            }
            return EchoNativeLoadStatus.REGISTERED;
        }

        @Override
        public EchoNativeLoadStatus reloadConfig(
                String moduleId,
                String configId,
                String scope,
                Map<String, Object> evidence
        ) {
            if (configId != null && configId.endsWith("-live")) {
                stampLiveDispatch(evidence, "config:" + moduleId + ":" + configId);
                return EchoNativeLoadStatus.MUTATED;
            }
            return EchoNativeLoadStatus.REGISTERED;
        }

        @Override
        public EchoNativeLoadStatus lifecyclePhase(String moduleId, String phaseId, Map<String, Object> evidence) {
            if (phaseId != null && phaseId.contains("_live_")) {
                stampLiveDispatch(evidence, "lifecycle:" + moduleId + ":" + phaseId);
                return EchoNativeLoadStatus.MUTATED;
            }
            return EchoNativeLoadStatus.REGISTERED;
        }

        @Override
        public EchoNativeLoadStatus publishRuntimeEvent(
                String sourceModule,
                String eventId,
                Map<String, Object> payload,
                EchoNativeLoadStatus status
        ) {
            if (eventId != null && eventId.contains(".partial_live_")) {
                stampLiveDispatch(payload, "event:" + sourceModule + ":" + eventId);
                return EchoNativeLoadStatus.MUTATED;
            }
            return EchoNativeLoadStatus.REGISTERED;
        }

        private void stampLiveDispatch(Map<String, Object> evidence, String operation) {
            if (evidence == null) {
                return;
            }
            evidence.put("liveRuntimeDispatchProofSatisfied", true);
            evidence.put("liveRuntimeDispatchMinecraftAccessed", true);
            evidence.put("liveRuntimeDispatchMutationSupported", true);
            evidence.put("liveRuntimeDispatchLiveMutation", true);
            evidence.put("liveRuntimeDispatchBridgeId", bridgeId());
            evidence.put("liveRuntimeDispatchOperation", operation);
            String surface = surfaceForOperation(operation);
            if (!surface.isBlank()) {
                evidence.put("liveRuntimeSurface", surface);
                stampRuntimeSideEffectEvidence(evidence, surface, operation);
            }
        }

        private void stampRuntimeSideEffectEvidence(Map<String, Object> evidence, String surface, String operation) {
            if (!List.of(
                    "commands",
                    "network_channels",
                    "config_reloads",
                    "lifecycle_phases",
                    "events").contains(surface)) {
                return;
            }
            evidence.put("runtimeSurfaceSaveTouched", true);
            evidence.put("runtimeSurfaceSaveMutated", true);
            evidence.put("runtimeSaveDataTouched", true);
            evidence.put("runtimeSaveDataMutated", true);
            evidence.put("liveSaveDataFileTouched", true);
            evidence.put("runtimeSaveDataBackend", "world_save_file");
            evidence.put("saveFile", "agent5/partial-subsystem-save/" + operation.replace(':', '_') + ".properties");
            if ("commands".equals(surface)) {
                evidence.put("runtimeCommandRegistryTouched", true);
                evidence.put("runtimeCommandRegistryMutated", true);
            }
            if ("network_channels".equals(surface)) {
                evidence.put("runtimeSurfacePacketSent", true);
                evidence.put("runtimeSurfacePacketMutated", true);
                evidence.put("runtimeNetworkChannelTouched", true);
                evidence.put("runtimeNetworkChannelMutated", true);
                evidence.put("runtimeNetworkPacketSent", true);
            }
            if ("config_reloads".equals(surface)) {
                evidence.put("runtimeConfigReloadTouched", true);
                evidence.put("runtimeConfigReloadMutated", true);
            }
            if ("lifecycle_phases".equals(surface)) {
                evidence.put("runtimeLifecyclePhaseTouched", true);
                evidence.put("runtimeLifecyclePhaseMutated", true);
            }
            if ("events".equals(surface)) {
                evidence.put("runtimeSurfaceEventPublished", true);
                evidence.put("runtimeSurfaceEventMutated", true);
                evidence.put("runtimeEventTouched", true);
                evidence.put("runtimeEventMutated", true);
                evidence.put("runtimeEventPublished", true);
            }
        }

        private String surfaceForOperation(String operation) {
            if (operation == null || operation.isBlank()) {
                return "";
            }
            String prefix = operation.contains(":") ? operation.substring(0, operation.indexOf(':')) : operation;
            return switch (prefix) {
                case "command" -> "commands";
                case "network" -> "network_channels";
                case "config" -> "config_reloads";
                case "lifecycle" -> "lifecycle_phases";
                case "event" -> "events";
                default -> prefix;
            };
        }
    }

    private static class LiveMinecraftBridge implements Agent5LiveRuntimeAccessBridge {
        private final List<String> calls = new java.util.ArrayList<>();
        private final Map<String, Map<String, Object>> surfaceEvidence = new java.util.LinkedHashMap<>();
        private final Map<String, String> activeDispatchIds = new java.util.LinkedHashMap<>();

        @Override
        public boolean attached() {
            return true;
        }

        @Override
        public String bridgeId() {
            return "agent5:live_minecraft_bridge";
        }

        @Override
        public boolean minecraftRuntimeAccessed() {
            return true;
        }

        @Override
        public boolean liveRuntimeMutationSupported() {
            return true;
        }

        @Override
        public Map<String, Object> runtimeEvidence() {
            return Map.of(
                    "bridgeId", bridgeId(),
                    "attached", true,
                    "liveRuntimeAccessed", true,
                    "minecraftRuntimeAccessed", true,
                    "liveRuntimeMutationSupported", true,
                    "realMinecraftProcess", true,
                    "agent5TruthGate", true
            );
        }

        @Override
        public Map<String, Object> liveRuntimeSurfaceEvidence(String surface) {
            return surfaceEvidence.getOrDefault(surface, Map.of());
        }

        @Override
        public void beginLiveRuntimeSurfaceDispatch(String surface, String dispatchId) {
            surfaceEvidence.remove(surface);
            activeDispatchIds.put(surface, dispatchId);
        }

        @Override
        public EchoNativeLoadStatus grantItem(String playerId, String itemId, int count) {
            return mutate("inventory:" + playerId + ":" + itemId + ":" + count);
        }

        @Override
        public EchoNativeLoadStatus removeItem(String playerId, String itemId, int count) {
            return mutate("inventory_remove:" + playerId + ":" + itemId + ":" + count);
        }

        @Override
        public EchoNativeLoadStatus updatePlayerState(String playerId, String key, String value) {
            return mutate("player_state:" + playerId + ":" + key);
        }

        @Override
        public EchoNativeLoadStatus placeBlock(String dimension, int x, int y, int z, String blockId) {
            return mutate("world_blocks:" + dimension + ":" + x + "," + y + "," + z + ":" + blockId);
        }

        @Override
        public EchoNativeLoadStatus placeStructure(String dimension, String structureId, int x, int y, int z) {
            return mutate("structures:" + dimension + ":" + structureId + ":" + x + "," + y + "," + z);
        }

        @Override
        public EchoNativeLoadStatus updateBlockEntity(String dimension, int x, int y, int z, String key, String value) {
            return mutate("block_entities:" + dimension + ":" + x + "," + y + "," + z + ":" + key);
        }

        @Override
        public EchoNativeLoadStatus updateCapability(String target, String capability, String value) {
            return mutate("capabilities:" + target + ":" + capability);
        }

        @Override
        public EchoNativeLoadStatus updateWorldState(String dimension, String key, String value) {
            return mutate("world_state:" + dimension + ":" + key);
        }

        @Override
        public EchoNativeLoadStatus emitEvent(String eventType, String payload) {
            return mutate("events:" + eventType);
        }

        @Override
        public EchoNativeLoadStatus sendPacketHud(String channel, String payload) {
            return mutate("packets_hud:" + channel);
        }

        @Override
        public EchoNativeLoadStatus writeSaveData(String key, String value) {
            return mutate("save_data:" + key);
        }

        @Override
        public EchoNativeLoadStatus deleteSaveData(String key) {
            return mutate("save_data_delete:" + key);
        }

        @Override
        public EchoNativeLoadStatus emitHud(String channel, String message) {
            return mutate("hud:" + channel);
        }

        @Override
        public EchoNativeLoadStatus updateMission(String missionId, String phase, String objectiveKey) {
            return mutate("missions:" + missionId + ":" + phase);
        }

        @Override
        public EchoNativeLoadStatus emitFeedback(String source, String message) {
            return mutate("feedback:" + source);
        }

        @Override
        public EchoNativeLoadStatus clientTick(String phase, Map<String, Object> payload) {
            return mutate("client_tick:" + phase, payload);
        }

        @Override
        public EchoNativeLoadStatus renderLayer(String layerId, Map<String, Object> payload) {
            return mutate("render_layers:" + layerId, payload);
        }

        @Override
        public EchoNativeLoadStatus screenEvent(String screenId, String eventType, Map<String, Object> payload) {
            return mutate("screen_events:" + screenId + ":" + eventType, payload);
        }

        @Override
        public EchoNativeLoadStatus keybind(String keybindId, String action, Map<String, Object> payload) {
            return mutate("keybinds:" + keybindId + ":" + action, payload);
        }

        @Override
        public EchoNativeLoadStatus reloadResources(
                String moduleId,
                String resourceId,
                String scope,
                Map<String, Object> evidence
        ) {
            return mutate("resource_reloads:" + moduleId + ":" + resourceId, evidence);
        }

        @Override
        public EchoNativeLoadStatus saveHook(String hookId, Map<String, Object> payload) {
            return mutate("save_hooks:" + hookId, payload);
        }

        @Override
        public EchoNativeLoadStatus syncServerClient(String channel, String payload) {
            return mutate("server_client_sync:" + channel);
        }

        @Override
        public EchoNativeLoadStatus registerCommand(
                String moduleId,
                String commandId,
                String targetSurface,
                String targetBridge,
                Map<String, Object> evidence
        ) {
            return mutate("command:" + moduleId + ":" + commandId, evidence);
        }

        @Override
        public EchoNativeLoadStatus registerNetworkPacket(
                String moduleId,
                String packetId,
                String surface,
                String sourceRuntimeTarget,
                List<String> consumers,
                Map<String, Object> evidence
        ) {
            return mutate("network:" + moduleId + ":" + packetId, evidence);
        }

        @Override
        public EchoNativeLoadStatus reloadConfig(
                String moduleId,
                String configId,
                String scope,
                Map<String, Object> evidence
        ) {
            return mutate("config:" + moduleId + ":" + configId, evidence);
        }

        @Override
        public EchoNativeLoadStatus lifecyclePhase(String moduleId, String phaseId, Map<String, Object> evidence) {
            return mutate("lifecycle:" + moduleId + ":" + phaseId, evidence);
        }

        @Override
        public EchoNativeLoadStatus publishRuntimeEvent(
                String sourceModule,
                String eventId,
                Map<String, Object> payload,
                EchoNativeLoadStatus status
        ) {
            return mutate("event:" + sourceModule + ":" + eventId, payload);
        }

        private EchoNativeLoadStatus mutate(String call) {
            calls.add(call);
            recordSurfaceEvidence(surfaceForCall(call), call);
            return EchoNativeLoadStatus.MUTATED;
        }

        private EchoNativeLoadStatus mutate(String call, Map<String, Object> evidence) {
            calls.add(call);
            stampLiveDispatch(evidence, call);
            recordSurfaceEvidence(surfaceForCall(call), call);
            return EchoNativeLoadStatus.MUTATED;
        }

        private void stampLiveDispatch(Map<String, Object> evidence, String operation) {
            if (evidence == null) {
                return;
            }
            evidence.put("liveRuntimeDispatchProofSatisfied", true);
            evidence.put("liveRuntimeDispatchMinecraftAccessed", true);
            evidence.put("liveRuntimeDispatchMutationSupported", true);
            evidence.put("liveRuntimeDispatchLiveMutation", true);
            evidence.put("liveRuntimeDispatchBridgeId", bridgeId());
            evidence.put("liveRuntimeDispatchOperation", operation);
            String surface = surfaceForCall(operation);
            if (!surface.isBlank()) {
                evidence.put("liveRuntimeSurface", surface);
                evidence.put("liveRuntimeDispatchId", activeDispatchIds.getOrDefault(surface, ""));
                stampRuntimeSurfaceSaveEvidence(evidence, surface, operation);
            }
        }

        private void recordSurfaceEvidence(String surface, String operation) {
            if (surface == null || surface.isBlank()) {
                return;
            }
            Map<String, Object> evidence = new java.util.LinkedHashMap<>();
            stampLiveDispatch(evidence, operation);
            evidence.put("liveRuntimeSurface", surface);
            evidence.put("liveRuntimeDispatchId", activeDispatchIds.getOrDefault(surface, ""));
            stampRuntimeSurfaceSaveEvidence(evidence, surface, operation);
            surfaceEvidence.put(surface, Map.copyOf(evidence));
        }

        private void stampRuntimeSurfaceSaveEvidence(Map<String, Object> evidence, String surface, String operation) {
            if ("inventory".equals(surface)) {
                evidence.put("runtimeInventoryTouched", true);
                evidence.put("runtimeInventoryMutated", true);
                return;
            }
            if ("player_state".equals(surface)) {
                evidence.put("runtimePlayerStateTouched", true);
                evidence.put("runtimePlayerStateMutated", true);
                return;
            }
            if ("missions".equals(surface)) {
                evidence.put("runtimePlayerStateTouched", true);
                evidence.put("runtimePlayerStateMutated", true);
                evidence.put("runtimeMissionStateTouched", true);
                evidence.put("runtimeMissionStateMutated", true);
                return;
            }
            if ("world_blocks".equals(surface)) {
                evidence.put("runtimeWorldBlockTouched", true);
                evidence.put("runtimeWorldBlockMutated", true);
                return;
            }
            if ("structures".equals(surface)) {
                evidence.put("runtimeStructurePlaced", true);
                evidence.put("runtimeStructureMutated", true);
                evidence.put("runtimeSaveDataTouched", true);
                evidence.put("runtimeSaveDataMutated", true);
                evidence.put("liveSaveDataFileTouched", true);
                evidence.put("runtimeSaveDataBackend", "world_save_file");
                evidence.put("saveFile", "agent5/live-world-save/" + operation.replace(':', '_') + ".properties");
                return;
            }
            if ("block_entities".equals(surface)) {
                evidence.put("runtimeBlockEntityTouched", true);
                evidence.put("runtimeBlockEntityMutated", true);
                return;
            }
            if ("capabilities".equals(surface)) {
                evidence.put("runtimeCapabilityTouched", true);
                evidence.put("runtimeCapabilityMutated", true);
                return;
            }
            if (!List.of(
                    "commands",
                    "network_channels",
                    "config_reloads",
                    "lifecycle_phases",
                    "events",
                    "packets_hud",
                    "hud",
                    "client_tick",
                    "render_layers",
                    "screen_events",
                    "keybinds",
                    "resource_reloads",
                    "world_state",
                    "save_data",
                    "save_hooks",
                    "server_client_sync").contains(surface)) {
                return;
            }
            if (List.of(
                    "commands",
                    "network_channels",
                    "config_reloads",
                    "lifecycle_phases",
                    "events",
                    "client_tick",
                    "render_layers",
                    "screen_events",
                    "keybinds",
                    "resource_reloads").contains(surface)) {
                evidence.put("runtimeSurfaceSaveTouched", true);
                evidence.put("runtimeSurfaceSaveMutated", true);
            }
            if ("network_channels".equals(surface) || "packets_hud".equals(surface)) {
                evidence.put("runtimeSurfacePacketSent", true);
                evidence.put("runtimeSurfacePacketMutated", true);
            }
            if ("packets_hud".equals(surface)) {
                evidence.put("runtimePacketSent", true);
                evidence.put("runtimePacketMutated", true);
            }
            if ("server_client_sync".equals(surface)) {
                evidence.put("runtimeSurfacePacketSent", true);
                evidence.put("runtimeSurfacePacketMutated", true);
                evidence.put("runtimeServerClientSyncPacketSent", true);
                evidence.put("runtimeServerClientSyncMutated", true);
            }
            if ("events".equals(surface)) {
                evidence.put("runtimeSurfaceEventPublished", true);
                evidence.put("runtimeSurfaceEventMutated", true);
            }
            if ("hud".equals(surface)) {
                evidence.put("runtimeHudNotificationPublished", true);
                evidence.put("runtimeHudNotificationMutated", true);
            }
            if ("commands".equals(surface)) {
                evidence.put("runtimeCommandRegistryTouched", true);
                evidence.put("runtimeCommandRegistryMutated", true);
            }
            if ("network_channels".equals(surface)) {
                evidence.put("runtimeNetworkChannelTouched", true);
                evidence.put("runtimeNetworkChannelMutated", true);
                evidence.put("runtimeNetworkPacketSent", true);
            }
            if ("config_reloads".equals(surface)) {
                evidence.put("runtimeConfigReloadTouched", true);
                evidence.put("runtimeConfigReloadMutated", true);
            }
            if ("lifecycle_phases".equals(surface)) {
                evidence.put("runtimeLifecyclePhaseTouched", true);
                evidence.put("runtimeLifecyclePhaseMutated", true);
            }
            if ("events".equals(surface)) {
                evidence.put("runtimeEventTouched", true);
                evidence.put("runtimeEventMutated", true);
                evidence.put("runtimeEventPublished", true);
            }
            evidence.put("runtimeSaveDataTouched", true);
            evidence.put("runtimeSaveDataMutated", true);
            evidence.put("liveSaveDataFileTouched", true);
            evidence.put("runtimeSaveDataBackend", "world_save_file");
            evidence.put("saveFile", "agent5/live-world-save/" + operation.replace(':', '_') + ".properties");
        }

        private String surfaceForCall(String call) {
            if (call == null || call.isBlank()) {
                return "";
            }
            String prefix = call.contains(":") ? call.substring(0, call.indexOf(':')) : call;
            return switch (prefix) {
                case "inventory_remove" -> "inventory";
                case "save_data_delete" -> "save_data";
                case "command" -> "commands";
                case "network" -> "network_channels";
                case "config" -> "config_reloads";
                case "lifecycle" -> "lifecycle_phases";
                case "event" -> "events";
                default -> prefix;
            };
        }

        private List<String> calls() {
            return List.copyOf(calls);
        }
    }

    private static final class DynamicAttachLiveMinecraftBridge extends LiveMinecraftBridge {
        private boolean attached;

        private void setAttached(boolean attached) {
            this.attached = attached;
        }

        @Override
        public boolean attached() {
            return attached;
        }

        @Override
        public Map<String, Object> runtimeEvidence() {
            Map<String, Object> evidence = new java.util.LinkedHashMap<>(super.runtimeEvidence());
            evidence.put("attached", attached);
            return Map.copyOf(evidence);
        }
    }
}
