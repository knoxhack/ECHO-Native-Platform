package dev.echo.nativeplatform.bootstrap;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import dev.echo.nativeplatform.loader.NativeLoaderProductPlayableRuntimeEvidence;

public final class EchoNativeLoaderLiveProofTruthGateVerifier {
    private static final String NATIVE_LOADER_SERVICE_ID = "adaptercore.native_loader.backend";
    private static final String NATIVE_LOADER_BACKEND_CLASS =
            "dev.echo.nativeplatform.loader.NativeLoaderAdapterCoreBackend";
    private static final String NATIVE_LOADER_HOST_CLASS =
            "com.knoxhack.echoashfallprotocol.event.NativeLoaderEchoRuntimeHost";
    private static final String NATIVE_MINECRAFT_HOST_CLASS =
            "com.knoxhack.echoashfallprotocol.event.NativeMinecraftEchoRuntimeHost";
    private static final String NATIVE_MINECRAFT_HOST_ID =
            "echoashfallprotocol:native_minecraft_runtime_host";

    private EchoNativeLoaderLiveProofTruthGateVerifier() {
    }

    public static void main(String[] args) throws Exception {
        verifyNoHandoffSidecar();
        verifyMarkerModuleRequiresLoadedClass();

        Map<String, Object> emptyProof = invokeProof("", Map.of(), Map.of(), Map.of(), Map.of(), Map.of());
        require(!"MUTATED".equals(emptyProof.get("status")), "empty live proof must not report mutated proof");
        require(Boolean.FALSE.equals(emptyProof.get("complete")), "empty live proof must not be complete");
        require(Boolean.FALSE.equals(emptyProof.get("gameplayReadyClaimAllowed")),
                "empty live proof must not allow gameplay-ready claims");
        verifyNormalizeRejectsStaleCompleteMutationTargets();
        verifyLiveProofRequiresLoadedClass();
        verifyLiveProofRequiresMutationLedger();
        verifyLiveProofRequiresNativeLoaderBackendLedger();
        verifyLiveProofRequiresNativeLoaderBackendResolution();
        verifyLiveProofRequiresRecordLevelLiveRuntimeProof();
        verifyLiveProofRejectsNativeLoaderCompatibilityFallback();
        verifyLiveProofRejectsNeoForgeLiveMinecraftDelegate();
        verifyLiveProofRequiresRegisteredNativeLoaderHost();
        verifyLiveProofRequiresNativeLoaderHostEntrypoint();
        verifyLiveProofRequiresRealClientUiHost();
        verifyLiveProofRejectsHeadlessUiFallback();

        Map<String, Object> partialProof = invokeProof(
                "net.minecraft.client.main.Main",
                liveClientProbe(false, false),
                uiBridge(true),
                ashfallGameplayBridge(),
                serviceBridge(),
                nativeActivations()
        );
        require("PARTIAL".equals(partialProof.get("status")), "proof without HUD output must stay partial");
        require(Boolean.FALSE.equals(partialProof.get("complete")), "proof without HUD output must not complete");
        require(Boolean.FALSE.equals(partialProof.get("gameplayReadyClaimAllowed")),
                "partial proof must not allow gameplay-ready claims");
        require(Boolean.TRUE.equals(partialProof.get("nativeHostMutationClaimAllowed")),
                "partial proof may allow native host mutation claims when save/world evidence exists");
        require(list(partialProof, "missingTargets").contains("liveHudNotificationEmitted"),
                "partial proof must name missing HUD notification evidence");

        Map<String, Object> labelOnlyProof = invokeProof(
                "net.minecraft.client.main.Main",
                liveClientProbe(false, true),
                uiBridge(true),
                ashfallGameplayBridge(),
                serviceBridge(),
                nativeActivations()
        );
        require("PARTIAL".equals(labelOnlyProof.get("status")), "hudLabelSent alone must stay partial");
        require(list(labelOnlyProof, "missingTargets").contains("liveHudNotificationEmitted"),
                "hudLabelSent alone must not satisfy the HUD notification target");

        Map<String, Object> completeProof = invokeProof(
                "net.minecraft.client.main.Main",
                liveClientProbe(true, false),
                uiBridge(true),
                ashfallGameplayBridge(),
                serviceBridge(),
                nativeActivations()
        );
        require("MUTATED".equals(completeProof.get("status")), "complete live proof must report MUTATED");
        require(Boolean.TRUE.equals(completeProof.get("complete")), "complete live proof must be complete");
        require(Boolean.TRUE.equals(completeProof.get("gameplayReadyClaimAllowed")),
                "complete live proof may allow gameplay-ready claims");
        require(list(completeProof, "missingTargets").isEmpty(), "complete live proof must have no missing targets");
        require(Boolean.TRUE.equals(completeProof.get("nativeLoaderAdapterCoreBackendResolved")),
                "complete live proof must resolve the Native Loader AdapterCore backend");
        require(Boolean.TRUE.equals(completeProof.get("nativeMinecraftRuntimeHostResolved")),
                "complete live proof must resolve the Native Minecraft runtime host");

        System.out.println("native loader live proof truth gate PASS"
                + " sidecar=no_handoff_incomplete incomplete=false-positive guarded partial=missing_hud"
                + " label_only=blocked loaded_class_required=true backend_resolution_required=true"
                + " native_minecraft_delegate_required=true"
                + " host_registry_required=true host_entry_required=true real_ui_host_required=true"
                + " complete=all_targets");
    }

    private static void verifyNoHandoffSidecar() throws Exception {
        Path dir = Files.createTempDirectory("echo-native-loader-live-proof-");
        Path markerPath = dir.resolve("module-activation.json");
        EchoNativeBootstrapMain.writeActivationMarker(markerPath, "ashfall", "", List.of("echoashfallprotocol"), Map.of());
        Path sidecarPath = dir.resolve("native-loader-live-proof.json");
        require(Files.isRegularFile(sidecarPath), "native loader live proof sidecar must be written");
        Map<String, Object> marker = parseJsonObject(markerPath);
        Map<String, Object> sidecar = parseJsonObject(sidecarPath);
        require(sidecarPath.toString().equals(marker.get("nativeLoaderLiveProofPath")),
                "activation marker must point at the native loader live proof sidecar");
        require(!"MUTATED".equals(sidecar.get("status")), "no-handoff sidecar must not report mutated proof");
        require(Boolean.FALSE.equals(sidecar.get("complete")), "no-handoff sidecar must not be complete");
        require(Boolean.FALSE.equals(sidecar.get("gameplayReadyClaimAllowed")),
                "no-handoff sidecar must not allow gameplay-ready claims");
        require(Boolean.FALSE.equals(sidecar.get("nativeHostMutationClaimAllowed")),
                "no-handoff sidecar must not allow native host mutation claims");
        require(list(sidecar, "mutationLedgerLiveProofSurfaces").isEmpty(),
                "no-handoff sidecar must expose no live-proof mutation surfaces");
        require(Boolean.FALSE.equals(marker.get("nativeLoaderLiveClientGameplayReady")),
                "no-handoff marker must not allow live gameplay readiness");
        require(list(sidecar, "missingTargets").contains("minecraftClientLaunchedOrAttached"),
                "no-handoff sidecar must name missing client launch/attach evidence");
    }

    private static void verifyMarkerModuleRequiresLoadedClass() throws Exception {
        Map<String, Object> staleActivation = new LinkedHashMap<>();
        staleActivation.put("activated", true);
        staleActivation.put("nativeAdapterCodeExecuted", true);
        staleActivation.put("entrypoint", "com.knoxhack.echoashfallprotocol.EchoAshfallProtocolNativeModule");
        Map<String, Object> module = EchoNativeActivationModuleSnapshot.module(
                "echoashfallprotocol",
                staleActivation,
                "echoashfallprotocol:scanner",
                true,
                ashfallGameplayBridge()
        );
        require(Boolean.FALSE.equals(module.get("nativeModuleActivated")),
                "marker module rows must not activate without loadedClassName evidence");
        require(Boolean.FALSE.equals(module.get("liveGameplayHookVerified")),
                "marker module rows must not verify gameplay hooks without loadedClassName evidence");
        require("native_module_class_load_evidence_missing".equals(module.get("state")),
                "marker module rows must name missing loaded-class evidence");
    }

    private static void verifyNormalizeRejectsStaleCompleteMutationTargets() {
        Map<String, Object> staleProof = new LinkedHashMap<>();
        staleProof.put("status", "MUTATED");
        staleProof.put("complete", true);
        staleProof.put("gameplayReadyClaimAllowed", true);
        staleProof.put("liveClientGameplayReadyClaimAllowed", true);
        staleProof.put("nativeHostMutationClaimAllowed", true);
        staleProof.put("requiredMutationSurfacesMutated", true);
        staleProof.put("mutationLedger", object(liveClientProbeWithMirrorOnlyNativeLoaderLedger()
                .get("ashfallPlayableBetaRuntime")).get("mutationLedger"));
        Map<String, Object> targets = new LinkedHashMap<>();
        for (String target : List.of(
                "minecraftClientLaunchedOrAttached",
                "bootstrapEnteredLiveClient",
                "nativeModuleClassesLoaded",
                "nativeServiceRegistryInitialized",
                "nativeLoaderAdapterCoreBackendResolved",
                "nativeMinecraftRuntimeHostResolved",
                "adapterCoreRuntimeHostAvailable",
                "nativeMutationLedgerRecorded",
                "livePlayerOrWorldMutation",
                "liveSaveDataWrite",
                "liveUiHostAttached",
                "moduleClientUiDeclarationsPromoted",
                "profileClientSurfaceContractSatisfied",
                "moduleDeclaredClientSurfacesLiveAttached",
                "liveHudNotificationEmitted"
        )) {
            targets.put(target, Map.of("passed", true, "evidence", "stale_sidecar_truth"));
        }
        staleProof.put("targets", targets);

        Map<String, Object> normalized = normalizeProof(staleProof);
        require(!"MUTATED".equals(normalized.get("status")),
                "normalize must not preserve stale MUTATED status without record-level live proof");
        require(Boolean.FALSE.equals(normalized.get("complete")),
                "normalize must clear stale complete=true without record-level live proof");
        require(Boolean.FALSE.equals(normalized.get("gameplayReadyClaimAllowed")),
                "normalize must clear stale gameplay-ready claim without record-level live proof");
        require(Boolean.FALSE.equals(normalized.get("nativeHostMutationClaimAllowed")),
                "normalize must clear stale native host mutation claim without record-level live proof");
        require(Boolean.FALSE.equals(normalized.get("requiredMutationSurfacesMutated")),
                "normalize must recompute required mutation surfaces from live-proof records");
        require(list(normalized, "mutationLedgerLiveProofSurfaces").isEmpty(),
                "normalize must expose no live-proof surfaces for mirror-only records");
        require(list(normalized, "missingTargets").contains("livePlayerOrWorldMutation"),
                "normalize must mark stale player/world mutation target missing");
        require(list(normalized, "missingTargets").contains("liveSaveDataWrite"),
                "normalize must mark stale save-data target missing");
        require(list(normalized, "missingTargets").contains("liveHudNotificationEmitted"),
                "normalize must mark stale HUD target missing");
    }

    private static void verifyLiveProofRequiresLoadedClass() throws Exception {
        Map<String, Object> proof = invokeProof(
                "net.minecraft.client.main.Main",
                liveClientProbe(true, false),
                uiBridge(true),
                ashfallGameplayBridge(),
                serviceBridge(),
                nativeActivationsWithoutLoadedClass()
        );
        require("PARTIAL".equals(proof.get("status")), "proof without loadedClassName must stay partial");
        require(Boolean.FALSE.equals(proof.get("complete")), "proof without loadedClassName must not complete");
        require(Boolean.FALSE.equals(proof.get("gameplayReadyClaimAllowed")),
                "proof without loadedClassName must not allow gameplay-ready claims");
        require(list(proof, "missingTargets").contains("nativeModuleClassesLoaded"),
                "proof without loadedClassName must name missing native module class evidence");
    }

    private static void verifyLiveProofRequiresMutationLedger() throws Exception {
        Map<String, Object> proof = invokeProof(
                "net.minecraft.client.main.Main",
                liveClientProbeWithoutMutationLedger(),
                uiBridge(true),
                ashfallGameplayBridge(),
                serviceBridge(),
                nativeActivations()
        );
        require("PARTIAL".equals(proof.get("status")), "proof without mutation ledger must stay partial");
        require(Boolean.FALSE.equals(proof.get("complete")), "proof without mutation ledger must not complete");
        require(Boolean.FALSE.equals(proof.get("gameplayReadyClaimAllowed")),
                "proof without mutation ledger must not allow gameplay-ready claims");
        require(list(proof, "missingTargets").contains("nativeMutationLedgerRecorded"),
                "proof without mutation ledger must name missing ledger evidence");
        require(list(proof, "missingTargets").contains("livePlayerOrWorldMutation"),
                "summary booleans must not satisfy player/world mutation without ledger records");
        require(list(proof, "missingTargets").contains("liveSaveDataWrite"),
                "summary booleans must not satisfy save-data mutation without ledger records");
        require(list(proof, "missingTargets").contains("liveHudNotificationEmitted"),
                "summary booleans must not satisfy HUD mutation without ledger records");
    }

    private static void verifyLiveProofRequiresNativeLoaderBackendLedger() throws Exception {
        Map<String, Object> proof = invokeProof(
                "net.minecraft.client.main.Main",
                liveClientProbeWithFallbackLedger(),
                uiBridge(true),
                ashfallGameplayBridge(),
                serviceBridge(),
                nativeActivations()
        );
        require("PARTIAL".equals(proof.get("status")), "fallback-service mutation ledger must stay partial");
        require(Boolean.FALSE.equals(proof.get("complete")),
                "fallback-service mutation ledger must not complete Native Loader proof");
        require(Boolean.FALSE.equals(proof.get("gameplayReadyClaimAllowed")),
                "fallback-service mutation ledger must not allow gameplay-ready claims");
        require(list(proof, "missingTargets").contains("livePlayerOrWorldMutation"),
                "fallback-service ledger must not satisfy player/world mutation");
        require(list(proof, "missingTargets").contains("liveSaveDataWrite"),
                "fallback-service ledger must not satisfy save-data mutation");
        require(list(proof, "missingTargets").contains("liveHudNotificationEmitted"),
                "fallback-service ledger must not satisfy HUD mutation");
    }

    private static void verifyLiveProofRequiresNativeLoaderBackendResolution() throws Exception {
        Map<String, Object> proof = invokeProof(
                "net.minecraft.client.main.Main",
                liveClientProbeWithUnresolvedNativeLoaderLedger(),
                uiBridge(true),
                ashfallGameplayBridge(),
                serviceBridge(),
                nativeActivations()
        );
        require("PARTIAL".equals(proof.get("status")), "native-service-id-only mutation ledger must stay partial");
        require(Boolean.FALSE.equals(proof.get("complete")),
                "native-service-id-only mutation ledger must not complete Native Loader proof");
        require(Boolean.FALSE.equals(proof.get("gameplayReadyClaimAllowed")),
                "native-service-id-only mutation ledger must not allow gameplay-ready claims");
        require(list(proof, "missingTargets").contains("nativeLoaderAdapterCoreBackendResolved"),
                "native-service-id-only ledger must name missing backend resolution");
        require(list(proof, "missingTargets").contains("livePlayerOrWorldMutation"),
                "native-service-id-only ledger must not satisfy player/world mutation");
        require(list(proof, "missingTargets").contains("liveSaveDataWrite"),
                "native-service-id-only ledger must not satisfy save-data mutation");
        require(list(proof, "missingTargets").contains("liveHudNotificationEmitted"),
                "native-service-id-only ledger must not satisfy HUD mutation");
    }

    private static void verifyLiveProofRequiresRecordLevelLiveRuntimeProof() throws Exception {
        Map<String, Object> proof = invokeProof(
                "net.minecraft.client.main.Main",
                liveClientProbeWithMirrorOnlyNativeLoaderLedger(),
                uiBridge(true),
                ashfallGameplayBridge(),
                serviceBridge(),
                nativeActivations()
        );
        require("PARTIAL".equals(proof.get("status")),
                "resolved Native Loader ledger without record-level live proof must stay partial");
        require(Boolean.FALSE.equals(proof.get("complete")),
                "resolved Native Loader ledger without record-level live proof must not complete proof");
        require(Boolean.FALSE.equals(proof.get("gameplayReadyClaimAllowed")),
                "resolved Native Loader ledger without record-level live proof must not allow gameplay-ready claims");
        require(Boolean.FALSE.equals(proof.get("nativeHostMutationClaimAllowed")),
                "resolved Native Loader ledger without record-level live proof must not allow host mutation claims");
        require(Boolean.TRUE.equals(proof.get("nativeLoaderAdapterCoreBackendResolved")),
                "record-level proof regression should still resolve the backend so the live-proof target is isolated");
        require(Boolean.TRUE.equals(proof.get("nativeMinecraftRuntimeHostResolved")),
                "record-level proof regression should still resolve the Native Minecraft delegate");
        require(list(proof, "missingTargets").contains("livePlayerOrWorldMutation"),
                "resolved ledger without record-level live proof must not satisfy player/world mutation");
        require(list(proof, "missingTargets").contains("liveSaveDataWrite"),
                "resolved ledger without record-level live proof must not satisfy save-data mutation");
        require(list(proof, "missingTargets").contains("liveHudNotificationEmitted"),
                "resolved ledger without record-level live proof must not satisfy HUD mutation");
        require(list(proof, "mutationLedgerLiveProofSurfaces").isEmpty(),
                "resolved ledger without record-level live proof must expose no live-proof surfaces");
    }

    private static void verifyLiveProofRejectsNativeLoaderCompatibilityFallback() throws Exception {
        Map<String, Object> proof = invokeProof(
                "net.minecraft.client.main.Main",
                liveClientProbeWithNativeLoaderCompatibilityFallbackLedger(),
                uiBridge(true),
                ashfallGameplayBridge(),
                serviceBridge(),
                nativeActivations()
        );
        require("PARTIAL".equals(proof.get("status")),
                "Native Loader service-id ledger using compatibility fallback must stay partial");
        require(Boolean.FALSE.equals(proof.get("complete")),
                "Native Loader compatibility fallback ledger must not complete Native Loader proof");
        require(Boolean.FALSE.equals(proof.get("gameplayReadyClaimAllowed")),
                "Native Loader compatibility fallback ledger must not allow gameplay-ready claims");
        require(list(proof, "missingTargets").contains("nativeLoaderAdapterCoreBackendResolved"),
                "Native Loader compatibility fallback ledger must name missing backend resolution");
        require(list(proof, "missingTargets").contains("livePlayerOrWorldMutation"),
                "Native Loader compatibility fallback ledger must not satisfy player/world mutation");
        require(list(proof, "missingTargets").contains("liveSaveDataWrite"),
                "Native Loader compatibility fallback ledger must not satisfy save-data mutation");
        require(list(proof, "missingTargets").contains("liveHudNotificationEmitted"),
                "Native Loader compatibility fallback ledger must not satisfy HUD mutation");
    }

    private static void verifyLiveProofRejectsNeoForgeLiveMinecraftDelegate() throws Exception {
        Map<String, Object> proof = invokeProof(
                "net.minecraft.client.main.Main",
                liveClientProbeWithNeoForgeLiveMinecraftDelegateLedger(),
                uiBridge(true),
                ashfallGameplayBridge(),
                serviceBridge(),
                nativeActivations()
        );
        require("PARTIAL".equals(proof.get("status")),
                "Native Loader proof using a NeoForge live Minecraft delegate must stay partial");
        require(Boolean.FALSE.equals(proof.get("complete")),
                "NeoForge live Minecraft delegate must not complete Native Loader proof");
        require(Boolean.FALSE.equals(proof.get("gameplayReadyClaimAllowed")),
                "NeoForge live Minecraft delegate must not allow gameplay-ready claims");
        require(Boolean.TRUE.equals(proof.get("nativeLoaderAdapterCoreBackendResolved")),
                "NeoForge live delegate regression should still show backend resolution so the delegate target is isolated");
        require(Boolean.FALSE.equals(proof.get("nativeMinecraftRuntimeHostResolved")),
                "NeoForge live delegate regression must not satisfy Native Minecraft host resolution");
        require(list(proof, "missingTargets").contains("nativeMinecraftRuntimeHostResolved"),
                "NeoForge live delegate regression must name missing Native Minecraft host resolution");
    }


    private static void verifyLiveProofRequiresRegisteredNativeLoaderHost() throws Exception {
        Map<String, Object> proof = invokeProof(
                "net.minecraft.client.main.Main",
                liveClientProbeWithUnregisteredNativeLoaderHostLedger(),
                uiBridge(true),
                ashfallGameplayBridge(),
                serviceBridge(),
                nativeActivations()
        );
        require("PARTIAL".equals(proof.get("status")), "unregistered Native Loader host ledger must stay partial");
        require(Boolean.FALSE.equals(proof.get("complete")),
                "unregistered Native Loader host ledger must not complete Native Loader proof");
        require(Boolean.FALSE.equals(proof.get("gameplayReadyClaimAllowed")),
                "unregistered Native Loader host ledger must not allow gameplay-ready claims");
        require(list(proof, "missingTargets").contains("nativeLoaderAdapterCoreBackendResolved"),
                "unregistered Native Loader host ledger must name missing backend resolution");
        require(list(proof, "missingTargets").contains("livePlayerOrWorldMutation"),
                "unregistered Native Loader host ledger must not satisfy player/world mutation");
        require(list(proof, "missingTargets").contains("liveSaveDataWrite"),
                "unregistered Native Loader host ledger must not satisfy save-data mutation");
        require(list(proof, "missingTargets").contains("liveHudNotificationEmitted"),
                "unregistered Native Loader host ledger must not satisfy HUD mutation");
    }

    private static void verifyLiveProofRequiresNativeLoaderHostEntrypoint() throws Exception {
        Map<String, Object> proof = invokeProof(
                "net.minecraft.client.main.Main",
                liveClientProbeWithNativeLoaderHostLedgerWithoutEntrypoint(),
                uiBridge(true),
                ashfallGameplayBridge(),
                serviceBridge(),
                nativeActivations()
        );
        require("PARTIAL".equals(proof.get("status")),
                "Native Loader host ledger without Native Loader entrypoint evidence must stay partial");
        require(Boolean.FALSE.equals(proof.get("complete")),
                "Native Loader host ledger without entrypoint evidence must not complete Native Loader proof");
        require(Boolean.FALSE.equals(proof.get("gameplayReadyClaimAllowed")),
                "Native Loader host ledger without entrypoint evidence must not allow gameplay-ready claims");
        require(list(proof, "missingTargets").contains("nativeLoaderAdapterCoreBackendResolved"),
                "ledger without Native Loader entrypoint evidence must name missing backend resolution");
        require(list(proof, "missingTargets").contains("livePlayerOrWorldMutation"),
                "ledger without Native Loader entrypoint evidence must not satisfy player/world mutation");
        require(list(proof, "missingTargets").contains("liveSaveDataWrite"),
                "ledger without Native Loader entrypoint evidence must not satisfy save-data mutation");
        require(list(proof, "missingTargets").contains("liveHudNotificationEmitted"),
                "ledger without Native Loader entrypoint evidence must not satisfy HUD mutation");
    }

    private static void verifyLiveProofRequiresRealClientUiHost() throws Exception {
        Map<String, Object> proof = invokeProof(
                "net.minecraft.client.main.Main",
                liveClientProbe(true, false),
                uiBridgeWithoutLiveWindow(),
                ashfallGameplayBridge(),
                serviceBridge(),
                nativeActivations()
        );
        require("PARTIAL".equals(proof.get("status")),
                "client UI host without live window evidence must stay partial");
        require(Boolean.FALSE.equals(proof.get("complete")),
                "client UI host without live window evidence must not complete Native Loader proof");
        require(Boolean.FALSE.equals(proof.get("gameplayReadyClaimAllowed")),
                "client UI host without live window evidence must not allow gameplay-ready claims");
        require(Boolean.FALSE.equals(proof.get("liveUiHostAttached")),
                "client UI host without live window evidence must not satisfy live UI attachment");
        require(list(proof, "missingTargets").contains("liveUiHostAttached"),
                "client UI host without live window evidence must name missing live UI attachment");
    }

    private static void verifyLiveProofRejectsHeadlessUiFallback() throws Exception {
        Map<String, Object> proof = invokeProof(
                "net.minecraft.client.main.Main",
                liveClientProbe(true, false),
                headlessUiBridge(),
                ashfallGameplayBridge(),
                serviceBridge(),
                nativeActivations()
        );
        require("PARTIAL".equals(proof.get("status")),
                "headless/fallback UI host must stay partial");
        require(Boolean.FALSE.equals(proof.get("complete")),
                "headless/fallback UI host must not complete Native Loader proof");
        require(Boolean.FALSE.equals(proof.get("gameplayReadyClaimAllowed")),
                "headless/fallback UI host must not allow gameplay-ready claims");
        require(Boolean.FALSE.equals(proof.get("liveUiHostAttached")),
                "headless/fallback UI host must not satisfy live UI attachment");
        require(list(proof, "missingTargets").contains("liveUiHostAttached"),
                "headless/fallback UI host must name missing live UI attachment");
    }

    private static Map<String, Object> invokeProof(
            String realMainClass,
            Map<String, Object> liveClientProbe,
            Map<String, Object> nativeClientUiBridge,
            Map<String, Object> ashfallGameplayBridge,
            Map<String, Object> serviceBridge,
            Map<String, Map<String, Object>> nativeActivations
    ) throws Exception {
        EchoNativeLoaderLiveProof proof = new EchoNativeLoaderLiveProof(
                "ashfallPlayableBetaRuntime",
                List.of("inventory", "world_blocks", "save_data", "hud")
        );
        return proof.create(
                realMainClass,
                liveClientProbe,
                nativeClientUiBridge,
                ashfallGameplayBridge,
                serviceBridge,
                nativeActivations,
                evidenceConfig(),
                EchoNativeActivationModuleSnapshot::nativeActivationLoaded
        );
    }

    private static Map<String, Object> normalizeProof(Map<String, Object> proof) {
        EchoNativeLoaderLiveProof liveProof = new EchoNativeLoaderLiveProof(
                "ashfallPlayableBetaRuntime",
                List.of("inventory", "world_blocks", "save_data", "hud")
        );
        return liveProof.normalize(proof, evidenceConfig());
    }

    private static NativeLoaderProductPlayableRuntimeEvidence.Config evidenceConfig() {
        return new NativeLoaderProductPlayableRuntimeEvidence.Config(
                NATIVE_LOADER_SERVICE_ID,
                "echoashfallprotocol",
                NATIVE_LOADER_BACKEND_CLASS,
                "Native Loader",
                NATIVE_MINECRAFT_HOST_CLASS,
                NATIVE_MINECRAFT_HOST_ID,
                "com.knoxhack.echoashfallprotocol.event.NeoForgeEchoRuntimeHost",
                "echoashfallprotocol:neoforge_runtime_host",
                "ashfall.hud"
        );
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> parseJsonObject(Path path) throws Exception {
        Object parsed = EchoNativeJsonSupport.parse(Files.readString(path, StandardCharsets.UTF_8));
        if (!(parsed instanceof Map<?, ?> raw)) {
            return Map.of();
        }
        Map<String, Object> object = new LinkedHashMap<>();
        raw.forEach((key, value) -> object.put(String.valueOf(key), value));
        return object;
    }

    private static Map<String, Object> liveClientProbe(boolean hudNotificationEmitted, boolean hudLabelSent) {
        Map<String, Object> probe = new LinkedHashMap<>();
        probe.put("executed", true);
        probe.put("clientRuntimeAccessed", true);
        probe.put("clientThreadScheduled", true);
        probe.put("hudLabelSent", hudLabelSent);
        Map<String, Object> playable = new LinkedHashMap<>();
        playable.put("starterItemsGranted", true);
        playable.put("serverBlocksPlaced", 9);
        playable.put("clientBlocksPlaced", 9);
        playable.put("saveDataWritten", true);
        playable.put("saveDataScope", "echoashfallprotocol");
        playable.put("saveDataKey", "native_loader.first_spawn");
        playable.put("hudNotificationEmitted", hudNotificationEmitted);
        playable.put("hudNotificationEvidence", Map.of(
                "nativeInterface", "EchoNativeRuntimeHost.Hud",
                "nativeMethod", "publishNotification",
                "mutated", hudNotificationEmitted));
        playable.put("mutationLedger", mutationLedger(hudNotificationEmitted));
        playable.put("mutationLedgerRecorded", true);
        playable.put("mutatedSurfaceCount", hudNotificationEmitted ? 4 : 3);
        playable.put("allRequiredMutationSurfacesMutated", hudNotificationEmitted);
        probe.put("ashfallPlayableBetaRuntime", playable);
        return probe;
    }

    private static Map<String, Object> liveClientProbeWithoutMutationLedger() {
        Map<String, Object> probe = liveClientProbe(true, false);
        Map<String, Object> playable = new LinkedHashMap<>(object(probe.get("ashfallPlayableBetaRuntime")));
        playable.remove("mutationLedger");
        playable.remove("mutationLedgerRecorded");
        playable.remove("mutatedSurfaceCount");
        playable.remove("allRequiredMutationSurfacesMutated");
        probe.put("ashfallPlayableBetaRuntime", playable);
        return probe;
    }

    private static Map<String, Object> liveClientProbeWithFallbackLedger() {
        Map<String, Object> probe = liveClientProbe(true, false);
        Map<String, Object> playable = new LinkedHashMap<>(object(probe.get("ashfallPlayableBetaRuntime")));
        playable.put("mutationLedger", mutationLedgerWithService("adaptercore.neoforge_compat.backend", true));
        probe.put("ashfallPlayableBetaRuntime", playable);
        return probe;
    }

    private static Map<String, Object> liveClientProbeWithUnresolvedNativeLoaderLedger() {
        Map<String, Object> probe = liveClientProbe(true, false);
        Map<String, Object> playable = new LinkedHashMap<>(object(probe.get("ashfallPlayableBetaRuntime")));
        playable.put("mutationLedger", mutationLedgerWithService(NATIVE_LOADER_SERVICE_ID, true, false));
        probe.put("ashfallPlayableBetaRuntime", playable);
        return probe;
    }

    private static Map<String, Object> liveClientProbeWithUnregisteredNativeLoaderHostLedger() {
        Map<String, Object> probe = liveClientProbe(true, false);
        Map<String, Object> playable = new LinkedHashMap<>(object(probe.get("ashfallPlayableBetaRuntime")));
        playable.put("mutationLedger", mutationLedgerWithService(NATIVE_LOADER_SERVICE_ID, true, true, false));
        probe.put("ashfallPlayableBetaRuntime", playable);
        return probe;
    }

    private static Map<String, Object> liveClientProbeWithMirrorOnlyNativeLoaderLedger() {
        Map<String, Object> probe = liveClientProbe(true, false);
        Map<String, Object> playable = new LinkedHashMap<>(object(probe.get("ashfallPlayableBetaRuntime")));
        List<Map<String, Object>> ledger = mutationLedgerWithService(NATIVE_LOADER_SERVICE_ID, true, true, true)
                .stream()
                .map(record -> {
                    Map<String, Object> copy = new LinkedHashMap<>(record);
                    copy.put("liveRuntimeAccessed", true);
                    copy.put("minecraftRuntimeAccessed", true);
                    copy.put("liveRuntimeMutationSupported", true);
                    copy.put("liveRuntimeReleaseProofSatisfied", false);
                    copy.put("liveRuntimeSurfaceMutationSatisfied", false);
                    copy.put("mirrorOnlyReleaseProof", true);
                    return Map.copyOf(copy);
                })
                .toList();
        playable.put("mutationLedger", ledger);
        probe.put("ashfallPlayableBetaRuntime", playable);
        return probe;
    }

    private static Map<String, Object> liveClientProbeWithNativeLoaderHostLedgerWithoutEntrypoint() {
        Map<String, Object> probe = liveClientProbe(true, false);
        Map<String, Object> playable = new LinkedHashMap<>(object(probe.get("ashfallPlayableBetaRuntime")));
        List<Map<String, Object>> ledger = mutationLedgerWithService(NATIVE_LOADER_SERVICE_ID, true, true, true)
                .stream()
                .map(record -> {
                    Map<String, Object> copy = new LinkedHashMap<>(record);
                    copy.put("adapterCoreCallEnteredNativeLoaderHost", false);
                    return Map.copyOf(copy);
                })
                .toList();
        playable.put("mutationLedger", ledger);
        probe.put("ashfallPlayableBetaRuntime", playable);
        return probe;
    }

    private static Map<String, Object> liveClientProbeWithNativeLoaderCompatibilityFallbackLedger() {
        Map<String, Object> probe = liveClientProbe(true, false);
        Map<String, Object> playable = new LinkedHashMap<>(object(probe.get("ashfallPlayableBetaRuntime")));
        List<Map<String, Object>> ledger = mutationLedgerWithService(NATIVE_LOADER_SERVICE_ID, true, true, true)
                .stream()
                .map(record -> {
                    Map<String, Object> copy = new LinkedHashMap<>(record);
                    copy.put("compatibilityFallbackUsed", true);
                    return Map.copyOf(copy);
                })
                .toList();
        playable.put("mutationLedger", ledger);
        probe.put("ashfallPlayableBetaRuntime", playable);
        return probe;
    }

    private static Map<String, Object> liveClientProbeWithNeoForgeLiveMinecraftDelegateLedger() {
        Map<String, Object> probe = liveClientProbe(true, false);
        Map<String, Object> playable = new LinkedHashMap<>(object(probe.get("ashfallPlayableBetaRuntime")));
        List<Map<String, Object>> ledger = mutationLedgerWithService(NATIVE_LOADER_SERVICE_ID, true, true, true)
                .stream()
                .map(record -> {
                    Map<String, Object> copy = new LinkedHashMap<>(record);
                    copy.put("liveMinecraftDelegateId", "echoashfallprotocol:neoforge_runtime_host");
                    copy.put("liveMinecraftDelegateClass",
                            "com.knoxhack.echoashfallprotocol.event.NeoForgeEchoRuntimeHost");
                    copy.put("compatibilityDelegate", "echoashfallprotocol:neoforge_runtime_host");
                    copy.put("compatibilityBackendClass",
                            "com.knoxhack.echoashfallprotocol.event.NeoForgeEchoRuntimeHost");
                    return Map.copyOf(copy);
                })
                .toList();
        playable.put("mutationLedger", ledger);
        probe.put("ashfallPlayableBetaRuntime", playable);
        return probe;
    }

    private static List<Map<String, Object>> mutationLedger(boolean hudNotificationEmitted) {
        return mutationLedgerWithService(NATIVE_LOADER_SERVICE_ID, hudNotificationEmitted);
    }

    private static List<Map<String, Object>> mutationLedgerWithService(String serviceId, boolean hudNotificationEmitted) {
        boolean nativeLoaderService = NATIVE_LOADER_SERVICE_ID.equals(serviceId);
        return mutationLedgerWithService(serviceId, hudNotificationEmitted, nativeLoaderService, nativeLoaderService);
    }

    private static List<Map<String, Object>> mutationLedgerWithService(
            String serviceId,
            boolean hudNotificationEmitted,
            boolean resolvedNativeLoaderBackend
    ) {
        return mutationLedgerWithService(
                serviceId,
                hudNotificationEmitted,
                resolvedNativeLoaderBackend,
                resolvedNativeLoaderBackend);
    }

    private static List<Map<String, Object>> mutationLedgerWithService(
            String serviceId,
            boolean hudNotificationEmitted,
            boolean resolvedNativeLoaderBackend,
            boolean runtimeHostRegistered
    ) {
        return List.of(
                mutationRecord(1, "inventory", true, serviceId, resolvedNativeLoaderBackend, runtimeHostRegistered),
                mutationRecord(2, "world_blocks", true, serviceId, resolvedNativeLoaderBackend, runtimeHostRegistered),
                mutationRecord(3, "save_data", true, serviceId, resolvedNativeLoaderBackend, runtimeHostRegistered),
                mutationRecord(4, "hud", hudNotificationEmitted, serviceId, resolvedNativeLoaderBackend, runtimeHostRegistered)
        );
    }

    private static Map<String, Object> mutationRecord(
            int sequence,
            String surface,
            boolean mutated,
            String serviceId,
            boolean resolvedNativeLoaderBackend,
            boolean runtimeHostRegistered
    ) {
        Map<String, Object> record = new LinkedHashMap<>();
        record.put("sequence", sequence);
        record.put("surface", surface);
        record.put("action", surface + ".proof");
        record.put("target", "ashfall.live_proof");
        record.put("status", mutated ? "MUTATED" : "FAILED");
        record.put("serviceId", serviceId);
        if (resolvedNativeLoaderBackend) {
            record.put("resolvedModuleId", "echoashfallprotocol");
            record.put("resolvedServiceClass", NATIVE_LOADER_HOST_CLASS);
            record.put("backendClass", NATIVE_LOADER_BACKEND_CLASS);
            record.put("runtimeHostClass", NATIVE_LOADER_HOST_CLASS);
            record.put("runtimeLane", "Native Loader");
            record.put("runtimeHostId", "echoashfallprotocol:native_loader_runtime_host");
            record.put("runtimeHostRegistered", runtimeHostRegistered);
            record.put("adapterCoreCallEnteredNativeLoaderHost", true);
            record.put("adapterCoreCallEnteredNativeLoaderBackend", true);
            record.put("adapterCoreBackendClass", NATIVE_LOADER_BACKEND_CLASS);
            record.put("nativeLoaderBackendAttached", true);
            record.put("nativeLoaderBackendRecordStatus", mutated ? "MUTATED" : "FAILED");
            record.put("nativeLoaderRuntimeHostClass", NATIVE_LOADER_HOST_CLASS);
            record.put("compatibilityFallbackUsed", false);
            record.put("compatibilityDelegate", "");
            record.put("compatibilityBackendClass", "");
            record.put("liveMinecraftDelegateId", NATIVE_MINECRAFT_HOST_ID);
            record.put("liveMinecraftDelegateClass", NATIVE_MINECRAFT_HOST_CLASS);
            record.put("liveRuntimeAccessed", true);
            record.put("minecraftRuntimeAccessed", true);
            record.put("liveRuntimeMutationSupported", mutated);
            record.put("mirrorOnlyReleaseProof", false);
            record.put("liveRuntimeReleaseProofSatisfied", mutated);
            record.put("liveRuntimeSurfaceMutationSatisfied", mutated);
            if (mutated) {
                record.put("surfaceLiveRuntimeProofEvidence", surfaceProofEvidence(surface, sequence));
                record.put("adapterCoreSurfaceDispatchId", "proof:" + surface + ":" + sequence);
                record.put("liveRuntimeDispatchId", "proof:" + surface + ":" + sequence);
                record.put("liveRuntimeSurface", surface);
            }
        }
        return record;
    }

    private static Map<String, Object> surfaceProofEvidence(String surface, int sequence) {
        String dispatchId = "proof:" + surface + ":" + sequence;
        return Map.of(
                "subsystemLiveRuntimeDispatchProofSatisfied", true,
                "liveRuntimeDispatchProofSatisfied", true,
                "minecraftRuntimeAccessed", true,
                "liveRuntimeDispatchMinecraftAccessed", true,
                "liveRuntimeDispatchMutationSupported", true,
                "liveMinecraftMutation", true,
                "liveRuntimeDispatchLiveMutation", true,
                "liveRuntimeDispatchId", dispatchId,
                "liveRuntimeSurface", surface
        );
    }

    private static Map<String, Object> uiBridge(boolean attached) {
        Map<String, Object> bridge = new LinkedHashMap<>();
        bridge.put("clientUiHostAttached", attached);
        bridge.put("clientThreadAccepted", attached);
        bridge.put("liveWindowHandlePresent", attached);
        bridge.put("fallbackHostAttached", false);
        bridge.put("headlessUiHostAttached", false);
        return bridge;
    }

    private static Map<String, Object> uiBridgeWithoutLiveWindow() {
        Map<String, Object> bridge = uiBridge(true);
        bridge.put("liveWindowHandlePresent", false);
        return bridge;
    }

    private static Map<String, Object> headlessUiBridge() {
        Map<String, Object> bridge = new LinkedHashMap<>();
        bridge.put("clientUiHostAttached", false);
        bridge.put("clientThreadAccepted", false);
        bridge.put("liveWindowHandlePresent", false);
        bridge.put("fallbackHostAttached", true);
        bridge.put("headlessUiHostAttached", true);
        return bridge;
    }

    private static Map<String, Object> ashfallGameplayBridge() {
        Map<String, Object> bridge = new LinkedHashMap<>();
        bridge.put("gameplayHandlerExecuted", true);
        bridge.put("liveGameplayHandlersAttached", true);
        return bridge;
    }

    private static Map<String, Object> serviceBridge() {
        Map<String, Object> bridge = new LinkedHashMap<>();
        bridge.put("applied", true);
        bridge.put("runtimeInitializedServiceCount", 3);
        return bridge;
    }

    private static Map<String, Map<String, Object>> nativeActivations() {
        Map<String, Object> activation = new LinkedHashMap<>();
        activation.put("activated", true);
        activation.put("nativeAdapterCodeExecuted", true);
        activation.put("entrypoint", "dev.echo.nativeplatform.samples.AshfallNativeSampleModule");
        activation.put("loadedClassName", "dev.echo.nativeplatform.samples.AshfallNativeSampleModule");
        return Map.of("echoashfallnativeproof", activation);
    }

    private static Map<String, Map<String, Object>> nativeActivationsWithoutLoadedClass() {
        Map<String, Object> activation = new LinkedHashMap<>();
        activation.put("activated", true);
        activation.put("nativeAdapterCodeExecuted", true);
        activation.put("entrypoint", "dev.echo.nativeplatform.samples.AshfallNativeSampleModule");
        return Map.of("echoashfallnativeproof", activation);
    }

    private static List<Object> list(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        return List.copyOf(list);
    }

    private static Map<String, Object> object(Object value) {
        if (!(value instanceof Map<?, ?> map)) {
            return Map.of();
        }
        Map<String, Object> object = new LinkedHashMap<>();
        map.forEach((key, item) -> object.put(String.valueOf(key), item));
        return object;
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
