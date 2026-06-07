package dev.echo.nativeplatform.loader;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;

/**
 * Builds the Native Loader live-client proof used by release gates.
 *
 * <p>Bootstrap may collect live client signals during handoff, but the proof
 * rules that decide whether those signals allow a gameplay-ready claim live in
 * the Native Loader service layer.</p>
 */
public final class NativeLoaderLiveProofService {
    public static final String SERVICE_ID = "echo.native.live_proof.service";

    private final String playableRuntimeKey;
    private final List<String> requiredLiveMutationSurfaces;

    public NativeLoaderLiveProofService(String playableRuntimeKey, List<String> requiredLiveMutationSurfaces) {
        this.playableRuntimeKey = playableRuntimeKey == null ? "" : playableRuntimeKey;
        this.requiredLiveMutationSurfaces = requiredLiveMutationSurfaces == null
                ? List.of()
                : List.copyOf(requiredLiveMutationSurfaces);
    }

    public Map<String, Object> create(
            String realMainClass,
            Map<String, Object> liveClientProbe,
            Map<String, Object> nativeClientUiBridge,
            Map<String, Object> productGameplayBridge,
            Map<String, Object> serviceBridge,
            Map<String, Map<String, Object>> nativeActivations,
            Config evidenceConfig,
            Predicate<Map<String, Object>> nativeActivationLoaded
    ) {
        Map<String, Object> playableBetaRuntime = object(liveClientProbe.get(playableRuntimeKey));
        List<Map<String, Object>> mutationLedger = objectList(playableBetaRuntime.get("mutationLedger"));
        Set<String> liveProofSurfaces = liveProofSurfaces(mutationLedger, evidenceConfig);
        boolean liveClientAttached = Boolean.TRUE.equals(liveClientProbe.get("executed"))
                && (!realMainClass.isBlank() || Boolean.TRUE.equals(liveClientProbe.get("preservedExistingLiveEvidence")));
        boolean bootstrapEnteredLiveClient = liveClientAttached
                && Boolean.TRUE.equals(liveClientProbe.get("clientRuntimeAccessed"))
                && Boolean.TRUE.equals(liveClientProbe.get("clientThreadScheduled"));
        int loadedNativeModuleCount = 0;
        boolean nativeAdapterCodeExecuted = false;
        for (Map<String, Object> activation : nativeActivations.values()) {
            if (!nativeActivationLoaded.test(activation)) {
                continue;
            }
            loadedNativeModuleCount++;
            nativeAdapterCodeExecuted = nativeAdapterCodeExecuted
                    || Boolean.TRUE.equals(activation.get("nativeAdapterCodeExecuted"));
        }
        boolean nativeModuleClassesLoaded = bootstrapEnteredLiveClient
                && loadedNativeModuleCount > 0
                && nativeAdapterCodeExecuted;
        boolean nativeServiceRegistryInitialized = bootstrapEnteredLiveClient
                && Boolean.TRUE.equals(serviceBridge.get("applied"))
                && integer(serviceBridge.get("runtimeInitializedServiceCount")) > 0;
        boolean mutationLedgerRecorded = bootstrapEnteredLiveClient && !mutationLedger.isEmpty();
        boolean nativeLoaderAdapterCoreBackendResolved = bootstrapEnteredLiveClient
                && mutationLedger.stream().anyMatch(record -> mutationRecordBackendResolved(record, evidenceConfig));
        boolean nativeMinecraftRuntimeHostResolved = bootstrapEnteredLiveClient
                && mutationLedger.stream().anyMatch(record -> mutationRecordUsesNativeMinecraftDelegate(record, evidenceConfig));
        boolean adapterCoreCallEnteredNativeLoaderBackend = nativeLoaderAdapterCoreBackendResolved;
        boolean playerOrWorldMutation = bootstrapEnteredLiveClient
                && (liveProofSurfaces.contains("inventory") || liveProofSurfaces.contains("world_blocks"));
        boolean liveSaveDataWrite = bootstrapEnteredLiveClient
                && liveProofSurfaces.contains("save_data");
        boolean liveHudNotificationEmitted = bootstrapEnteredLiveClient
                && liveProofSurfaces.contains("hud");
        boolean adapterCoreRuntimeHostAvailable = bootstrapEnteredLiveClient
                && nativeLoaderAdapterCoreBackendResolved
                && (playerOrWorldMutation
                || liveSaveDataWrite
                || liveHudNotificationEmitted
                || nativeServiceRegistryInitialized
                || Boolean.TRUE.equals(productGameplayBridge.get("gameplayHandlerExecuted"))
                || Boolean.TRUE.equals(productGameplayBridge.get("liveGameplayHandlersAttached")));
        boolean nativeClientUiHostAttached = Boolean.TRUE.equals(nativeClientUiBridge.get("clientUiHostAttached"));
        boolean nativeUiFallbackHostAttached = Boolean.TRUE.equals(nativeClientUiBridge.get("fallbackHostAttached"));
        boolean nativeHeadlessUiHostAttached = Boolean.TRUE.equals(nativeClientUiBridge.get("headlessUiHostAttached"));
        boolean nativeUiClientThreadAccepted = Boolean.TRUE.equals(nativeClientUiBridge.get("clientThreadAccepted"));
        boolean nativeUiLiveWindowHandlePresent = Boolean.TRUE.equals(nativeClientUiBridge.get("liveWindowHandlePresent"));
        int moduleDeclaredClientSurfaceCount = integer(nativeClientUiBridge.get("moduleDeclaredClientSurfaceCount"));
        int moduleDeclaredClientSurfaceLiveRoutableCount =
                integer(nativeClientUiBridge.get("moduleDeclaredClientSurfaceLiveRoutableCount"));
        int moduleDeclaredClientSurfaceLiveMutatedCount =
                integer(nativeClientUiBridge.get("moduleDeclaredClientSurfaceLiveMutatedCount"));
        int profileExpectedClientSurfaceCount =
                integer(nativeClientUiBridge.get("profileExpectedClientSurfaceCount"));
        boolean profileClientSurfaceContractSatisfied = profileExpectedClientSurfaceCount == 0
                || Boolean.TRUE.equals(nativeClientUiBridge.get("profileClientSurfaceContractSatisfied"));
        boolean moduleDeclaredClientSurfacesPromoted = moduleDeclaredClientSurfaceCount == 0
                || Boolean.TRUE.equals(nativeClientUiBridge.get("moduleDeclaredClientSurfacesPromoted"));
        boolean moduleDeclaredClientSurfacesLiveAttached = moduleDeclaredClientSurfaceCount == 0
                || (moduleDeclaredClientSurfaceLiveMutatedCount == moduleDeclaredClientSurfaceCount
                && liveUiHostAttachedCandidate(
                bootstrapEnteredLiveClient,
                nativeClientUiHostAttached,
                nativeUiClientThreadAccepted,
                nativeUiLiveWindowHandlePresent,
                nativeUiFallbackHostAttached,
                nativeHeadlessUiHostAttached
        ));
        boolean liveUiHostAttached = bootstrapEnteredLiveClient
                && nativeClientUiHostAttached
                && nativeUiClientThreadAccepted
                && nativeUiLiveWindowHandlePresent
                && !nativeUiFallbackHostAttached
                && !nativeHeadlessUiHostAttached;

        Map<String, Object> targets = new LinkedHashMap<>();
        putTarget(targets, "minecraftClientLaunchedOrAttached", liveClientAttached,
                "realMainClass=" + (realMainClass.isBlank() ? "<none>" : realMainClass));
        putTarget(targets, "bootstrapEnteredLiveClient", bootstrapEnteredLiveClient,
                "liveClientProbe.executed/clientRuntimeAccessed/clientThreadScheduled");
        putTarget(targets, "nativeModuleClassesLoaded", nativeModuleClassesLoaded,
                loadedNativeModuleCount + " loaded native module class(es)");
        putTarget(targets, "nativeServiceRegistryInitialized", nativeServiceRegistryInitialized,
                integer(serviceBridge.get("runtimeInitializedServiceCount")) + " runtime-initialized service(s)");
        putTarget(targets, "nativeLoaderAdapterCoreBackendResolved", nativeLoaderAdapterCoreBackendResolved,
                "resolved Native Loader backend ledger records=" + mutationLedger.stream()
                        .filter(record -> mutationRecordBackendResolved(record, evidenceConfig))
                        .count());
        putTarget(targets, "nativeMinecraftRuntimeHostResolved", nativeMinecraftRuntimeHostResolved,
                "Native Loader live Minecraft delegate records=" + mutationLedger.stream()
                        .filter(record -> mutationRecordUsesNativeMinecraftDelegate(record, evidenceConfig))
                        .count());
        putTarget(targets, "adapterCoreRuntimeHostAvailable", adapterCoreRuntimeHostAvailable,
                "AdapterCore host evidence from service bridge, gameplay bridge, or mutation surfaces");
        putTarget(targets, "nativeMutationLedgerRecorded", mutationLedgerRecorded,
                mutationLedger.size() + " live mutation ledger record(s)");
        putTarget(targets, "livePlayerOrWorldMutation", playerOrWorldMutation,
                "live-proof ledger surfaces=" + liveProofSurfaces);
        putTarget(targets, "liveSaveDataWrite", liveSaveDataWrite,
                playableBetaRuntime.getOrDefault("saveDataScope", "") + "/"
                        + playableBetaRuntime.getOrDefault("saveDataKey", ""));
        putTarget(targets, "liveUiHostAttached", liveUiHostAttached,
                "clientUiHostAttached=" + nativeClientUiHostAttached
                        + ", clientThreadAccepted=" + nativeUiClientThreadAccepted
                        + ", liveWindowHandlePresent=" + nativeUiLiveWindowHandlePresent
                        + ", fallbackHostAttached=" + nativeUiFallbackHostAttached
                        + ", headlessUiHostAttached=" + nativeHeadlessUiHostAttached);
        putTarget(targets, "moduleClientUiDeclarationsPromoted", moduleDeclaredClientSurfacesPromoted,
                moduleDeclaredClientSurfaceCount + " module-declared client UI surface(s)");
        putTarget(targets, "profileClientSurfaceContractSatisfied", profileClientSurfaceContractSatisfied,
                "expectedTypes=" + nativeClientUiBridge.getOrDefault("profileExpectedClientSurfaceTypes", List.of())
                        + ", declaredTypes="
                        + nativeClientUiBridge.getOrDefault("profileDeclaredClientSurfaceTypes", List.of())
                        + ", missingTypes="
                        + nativeClientUiBridge.getOrDefault("profileMissingClientSurfaceTypes", List.of()));
        putTarget(targets, "moduleDeclaredClientSurfacesLiveAttached", moduleDeclaredClientSurfacesLiveAttached,
                "routable=" + moduleDeclaredClientSurfaceLiveRoutableCount
                        + ", mutated=" + moduleDeclaredClientSurfaceLiveMutatedCount
                        + ", total=" + moduleDeclaredClientSurfaceCount
                        + ", moduleDeclaredClientSurfaceIds="
                        + nativeClientUiBridge.getOrDefault("moduleDeclaredClientSurfaceIds", List.of()));
        putTarget(targets, "liveHudNotificationEmitted", liveHudNotificationEmitted,
                "HUD proof requires a real HUD route or UI host notification, not chat/actionbar fallback");

        List<String> missingTargets = missingTargets(targets);
        boolean complete = missingTargets.isEmpty();
        boolean partial = complete || bootstrapEnteredLiveClient || anyTargetPassed(targets);
        Map<String, Object> proof = new LinkedHashMap<>();
        proof.put("schema", "echo.native_loader.live_client_proof.v1");
        proof.put("serviceId", SERVICE_ID);
        proof.put("proofScope", "live_minecraft_client_runtime");
        proof.put("status", complete ? "MUTATED" : partial ? "PARTIAL" : "INCOMPLETE");
        proof.put("complete", complete);
        proof.put("gameplayReadyClaimAllowed", complete);
        proof.put("liveClientGameplayReadyClaimAllowed", complete);
        proof.put("nativeHostMutationClaimAllowed", playerOrWorldMutation || liveSaveDataWrite || liveHudNotificationEmitted);
        proof.put("minecraftClientLaunchedOrAttached", liveClientAttached);
        proof.put("bootstrapEnteredLiveClient", bootstrapEnteredLiveClient);
        proof.put("nativeModuleClassesLoaded", nativeModuleClassesLoaded);
        proof.put("nativeServiceRegistryInitialized", nativeServiceRegistryInitialized);
        proof.put("nativeLoaderAdapterCoreBackendResolved", nativeLoaderAdapterCoreBackendResolved);
        proof.put("nativeMinecraftRuntimeHostResolved", nativeMinecraftRuntimeHostResolved);
        proof.put("adapterCoreCallEnteredNativeLoaderBackend", adapterCoreCallEnteredNativeLoaderBackend);
        proof.put("adapterCoreRuntimeHostAvailable", adapterCoreRuntimeHostAvailable);
        proof.put("nativeMutationLedgerRecorded", mutationLedgerRecorded);
        proof.put("livePlayerOrWorldMutation", playerOrWorldMutation);
        proof.put("liveSaveDataWrite", liveSaveDataWrite);
        proof.put("liveUiHostAttached", liveUiHostAttached);
        proof.put("nativeClientUiHostAttached", nativeClientUiHostAttached);
        proof.put("nativeUiClientThreadAccepted", nativeUiClientThreadAccepted);
        proof.put("nativeUiLiveWindowHandlePresent", nativeUiLiveWindowHandlePresent);
        proof.put("nativeUiFallbackHostAttached", nativeUiFallbackHostAttached);
        proof.put("nativeHeadlessUiHostAttached", nativeHeadlessUiHostAttached);
        proof.put("moduleDeclaredClientSurfaceCount", moduleDeclaredClientSurfaceCount);
        proof.put("moduleDeclaredClientSurfaceIds",
                nativeClientUiBridge.getOrDefault("moduleDeclaredClientSurfaceIds", List.of()));
        proof.put("moduleDeclaredClientSurfaceTypes",
                nativeClientUiBridge.getOrDefault("moduleDeclaredClientSurfaceTypes", List.of()));
        proof.put("moduleDeclaredClientSurfaceLiveRoutableCount", moduleDeclaredClientSurfaceLiveRoutableCount);
        proof.put("moduleDeclaredClientSurfaceLiveMutatedCount", moduleDeclaredClientSurfaceLiveMutatedCount);
        proof.put("moduleDeclaredClientSurfaceBindings",
                nativeClientUiBridge.getOrDefault("moduleDeclaredClientSurfaceBindings", List.of()));
        proof.put("profileExpectedClientSurfaceCount", profileExpectedClientSurfaceCount);
        proof.put("profileExpectedClientSurfaceTypes",
                nativeClientUiBridge.getOrDefault("profileExpectedClientSurfaceTypes", List.of()));
        proof.put("profileDeclaredClientSurfaceTypes",
                nativeClientUiBridge.getOrDefault("profileDeclaredClientSurfaceTypes", List.of()));
        proof.put("profileMissingClientSurfaceTypes",
                nativeClientUiBridge.getOrDefault("profileMissingClientSurfaceTypes", List.of()));
        proof.put("profileExtraClientSurfaceTypes",
                nativeClientUiBridge.getOrDefault("profileExtraClientSurfaceTypes", List.of()));
        proof.put("profileClientSurfaceContractSatisfied", profileClientSurfaceContractSatisfied);
        proof.put("moduleDeclaredClientSurfacesPromoted", moduleDeclaredClientSurfacesPromoted);
        proof.put("moduleDeclaredClientSurfacesLiveAttached", moduleDeclaredClientSurfacesLiveAttached);
        proof.put("liveHudNotificationEmitted", liveHudNotificationEmitted);
        proof.put("loadedNativeModuleClassCount", loadedNativeModuleCount);
        proof.put("runtimeInitializedServiceCount", integer(serviceBridge.get("runtimeInitializedServiceCount")));
        proof.put("serverBlocksPlaced", integer(playableBetaRuntime.get("serverBlocksPlaced")));
        proof.put("clientBlocksPlaced", integer(playableBetaRuntime.get("clientBlocksPlaced")));
        proof.put("saveDataScope", playableBetaRuntime.getOrDefault("saveDataScope", ""));
        proof.put("saveDataKey", playableBetaRuntime.getOrDefault("saveDataKey", ""));
        proof.put("hudNotificationEvidence", playableBetaRuntime.getOrDefault("hudNotificationEvidence", Map.of()));
        proof.put("mutationLedger", mutationLedger);
        proof.put("mutationLedgerMutatedSurfaces", liveProofSurfaces.stream().toList());
        proof.put("mutationLedgerLiveProofSurfaces", liveProofSurfaces.stream().toList());
        proof.put("requiredMutationSurfaces", requiredLiveMutationSurfaces);
        proof.put("requiredMutationSurfacesMutated", liveProofSurfaces.containsAll(requiredLiveMutationSurfaces));
        proof.put("targets", targets);
        proof.put("requiredTargets", targets.keySet().stream().toList());
        proof.put("missingTargets", missingTargets);
        proof.put("summary", complete
                ? "Native Loader live proof satisfied every Minecraft client, module, service, mutation, save, UI, and HUD target."
                : "Native Loader live proof is not gameplay-ready until every required live-client target records real evidence.");
        return proof;
    }

    public Map<String, Object> normalize(Map<String, Object> proof, Config evidenceConfig) {
        Map<String, Object> selected = new LinkedHashMap<>(proof == null ? Map.of() : proof);
        selected.put("serviceId", SERVICE_ID);
        List<Map<String, Object>> mutationLedger = objectList(selected.get("mutationLedger"));
        boolean nativeMinecraftRuntimeHostResolved = Boolean.TRUE.equals(selected.get("nativeMinecraftRuntimeHostResolved"))
                || mutationLedger.stream().anyMatch(record -> mutationRecordUsesNativeMinecraftDelegate(record, evidenceConfig));
        selected.put("nativeMinecraftRuntimeHostResolved", nativeMinecraftRuntimeHostResolved);
        Set<String> liveProofSurfaces = liveProofSurfaces(mutationLedger, evidenceConfig);
        boolean playerOrWorldMutation = liveProofSurfaces.contains("inventory") || liveProofSurfaces.contains("world_blocks");
        boolean liveSaveDataWrite = liveProofSurfaces.contains("save_data");
        boolean liveHudNotificationEmitted = liveProofSurfaces.contains("hud");
        boolean requiredMutationSurfacesMutated = liveProofSurfaces.containsAll(requiredLiveMutationSurfaces);

        Map<String, Object> targets = new LinkedHashMap<>(object(selected.get("targets")));
        putTarget(targets, "nativeMinecraftRuntimeHostResolved", nativeMinecraftRuntimeHostResolved,
                "Native Loader live Minecraft delegate records=" + mutationLedger.stream()
                        .filter(record -> mutationRecordUsesNativeMinecraftDelegate(record, evidenceConfig))
                        .count());
        putTarget(targets, "livePlayerOrWorldMutation", playerOrWorldMutation,
                "live-proof ledger surfaces=" + liveProofSurfaces);
        putTarget(targets, "liveSaveDataWrite", liveSaveDataWrite,
                selected.getOrDefault("saveDataScope", "") + "/" + selected.getOrDefault("saveDataKey", ""));
        putTarget(targets, "liveHudNotificationEmitted", liveHudNotificationEmitted,
                "HUD proof requires a real HUD route or UI host notification, not chat/actionbar fallback");
        selected.put("targets", targets);
        selected.put("requiredTargets", targets.keySet().stream().toList());
        selected.put("mutationLedgerMutatedSurfaces", liveProofSurfaces.stream().toList());
        selected.put("mutationLedgerLiveProofSurfaces", liveProofSurfaces.stream().toList());
        selected.put("requiredMutationSurfacesMutated", requiredMutationSurfacesMutated);
        selected.put("livePlayerOrWorldMutation", playerOrWorldMutation);
        selected.put("liveSaveDataWrite", liveSaveDataWrite);
        selected.put("liveHudNotificationEmitted", liveHudNotificationEmitted);
        selected.put("nativeHostMutationClaimAllowed", playerOrWorldMutation || liveSaveDataWrite || liveHudNotificationEmitted);

        List<String> missingTargets = missingTargets(targets);
        boolean complete = missingTargets.isEmpty() && requiredMutationSurfacesMutated;
        selected.put("missingTargets", missingTargets);
        selected.put("complete", complete);
        selected.put("gameplayReadyClaimAllowed", complete);
        selected.put("liveClientGameplayReadyClaimAllowed", complete);
        selected.put("status", complete ? "MUTATED" : anyTargetPassed(targets) ? "PARTIAL" : "INCOMPLETE");
        selected.put("summary", complete
                ? "Native Loader live proof satisfied every Minecraft client, module, service, mutation, save, UI, HUD, and Native Minecraft runtime host target."
                : "Native Loader live proof is not gameplay-ready until every required live-client target records real evidence.");
        return selected;
    }

    public static Map<String, Object> liveProofMarkerFields(String proofPath, Map<String, Object> proof) {
        Map<String, Object> safeProof = proof == null ? Map.of() : proof;
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("nativeLoaderLiveProofServiceId", SERVICE_ID);
        fields.put("nativeLoaderLiveProofPath", proofPath == null ? "" : proofPath);
        fields.put("nativeLoaderLiveProofStatus", safeProof.get("status"));
        fields.put("nativeLoaderLiveProofComplete", Boolean.TRUE.equals(safeProof.get("complete")));
        fields.put("nativeLoaderLiveClientGameplayReady", Boolean.TRUE.equals(safeProof.get("complete")));
        fields.put("nativeLoaderLiveProofMissingTargets", safeProof.getOrDefault("missingTargets", List.of()));
        fields.put("nativeLoaderLiveProof", safeProof);
        return Map.copyOf(fields);
    }

    private static Set<String> liveProofSurfaces(List<Map<String, Object>> ledger, Config config) {
        Set<String> surfaces = new TreeSet<>();
        for (Map<String, Object> record : ledger) {
            if ("MUTATED".equals(String.valueOf(record.getOrDefault("status", "")))
                    && mutationRecordResolved(record, config)
                    && mutationRecordLiveRuntimeProofSatisfied(record)) {
                String surface = String.valueOf(record.getOrDefault("surface", ""));
                if (!surface.isBlank()) {
                    surfaces.add(surface);
                }
            }
        }
        return surfaces;
    }

    private static boolean mutationRecordResolved(Map<String, Object> record, Config config) {
        return mutationRecordBackendResolved(record, config)
                && mutationRecordUsesNativeMinecraftDelegate(record, config);
    }

    private static boolean mutationRecordLiveRuntimeProofSatisfied(Map<String, Object> record) {
        return Boolean.TRUE.equals(record.get("liveRuntimeAccessed"))
                && Boolean.TRUE.equals(record.get("minecraftRuntimeAccessed"))
                && Boolean.TRUE.equals(record.get("liveRuntimeMutationSupported"))
                && !Boolean.TRUE.equals(record.get("mirrorOnlyReleaseProof"))
                && Boolean.TRUE.equals(record.get("liveRuntimeReleaseProofSatisfied"))
                && Boolean.TRUE.equals(record.get("liveRuntimeSurfaceMutationSatisfied"))
                && concreteSurfaceProofSatisfied(record);
    }

    private static boolean concreteSurfaceProofSatisfied(Map<String, Object> record) {
        Map<String, Object> proof = object(record.get("surfaceLiveRuntimeProofEvidence"));
        boolean dispatchProof = Boolean.TRUE.equals(proof.get("subsystemLiveRuntimeDispatchProofSatisfied"))
                || Boolean.TRUE.equals(proof.get("liveRuntimeDispatchProofSatisfied"));
        boolean minecraftAccess = Boolean.TRUE.equals(proof.get("minecraftRuntimeAccessed"))
                || Boolean.TRUE.equals(proof.get("liveRuntimeDispatchMinecraftAccessed"));
        boolean liveMutation = Boolean.TRUE.equals(proof.get("liveMinecraftMutation"))
                || Boolean.TRUE.equals(proof.get("liveRuntimeDispatchLiveMutation"));
        return !proof.isEmpty()
                && dispatchProof
                && minecraftAccess
                && Boolean.TRUE.equals(proof.get("liveRuntimeDispatchMutationSupported"))
                && liveMutation
                && !String.valueOf(proof.getOrDefault("liveRuntimeDispatchId", "")).isBlank()
                && String.valueOf(proof.getOrDefault("liveRuntimeDispatchId", ""))
                .equals(String.valueOf(record.getOrDefault("adapterCoreSurfaceDispatchId", "")))
                && String.valueOf(record.getOrDefault("surface", ""))
                .equals(String.valueOf(proof.getOrDefault("liveRuntimeSurface", "")));
    }

    private static boolean mutationRecordBackendResolved(Map<String, Object> record, Config config) {
        Config safeConfig = Config.safe(config);
        return safeConfig.adapterCoreServiceId().equals(String.valueOf(record.getOrDefault("serviceId", "")))
                && safeConfig.namespace().equals(String.valueOf(record.getOrDefault("resolvedModuleId", "")))
                && safeConfig.nativeLoaderBackendClass().equals(String.valueOf(record.getOrDefault("backendClass", "")))
                && safeConfig.nativeLoaderRuntimeLane().equals(String.valueOf(record.getOrDefault("runtimeLane", "")))
                && Boolean.TRUE.equals(record.get("runtimeHostRegistered"))
                && Boolean.TRUE.equals(record.get("adapterCoreCallEnteredNativeLoaderHost"))
                && Boolean.TRUE.equals(record.get("adapterCoreCallEnteredNativeLoaderBackend"))
                && !Boolean.TRUE.equals(record.get("compatibilityFallbackUsed"))
                && String.valueOf(record.getOrDefault("runtimeHostClass", "")).contains("NativeLoader")
                && !String.valueOf(record.getOrDefault("resolvedServiceClass", "")).isBlank();
    }

    private static boolean mutationRecordUsesNativeMinecraftDelegate(Map<String, Object> record, Config config) {
        Config safeConfig = Config.safe(config);
        String liveClass = String.valueOf(record.getOrDefault("liveMinecraftDelegateClass", ""));
        String liveId = String.valueOf(record.getOrDefault("liveMinecraftDelegateId", ""));
        String compatibilityBackendClass = String.valueOf(record.getOrDefault("compatibilityBackendClass", ""));
        String compatibilityDelegate = String.valueOf(record.getOrDefault("compatibilityDelegate", ""));
        String after = String.valueOf(record.getOrDefault("after", ""));
        boolean afterNamesNativeMinecraftDelegate = after.contains("liveMinecraftDelegateClass="
                + safeConfig.nativeMinecraftRuntimeHostClass())
                && after.contains("liveMinecraftDelegateId=" + safeConfig.nativeMinecraftRuntimeHostId());
        String blockedCompatibilityDelegateClass = safeConfig.compatibilityDelegateClass();
        String blockedCompatibilityDelegateId = safeConfig.compatibilityDelegateId();
        boolean afterNamesCompatibilityDelegate = (!blockedCompatibilityDelegateClass.isBlank()
                && (after.contains("liveMinecraftDelegateClass=" + blockedCompatibilityDelegateClass)
                || after.contains("compatibilityBackendClass=" + blockedCompatibilityDelegateClass)))
                || (!blockedCompatibilityDelegateId.isBlank()
                && (after.contains("liveMinecraftDelegateId=" + blockedCompatibilityDelegateId)
                || after.contains("compatibilityDelegate=" + blockedCompatibilityDelegateId)))
                || after.contains("compatibilityFallbackUsed=true");
        if (liveClass.isBlank() && afterNamesNativeMinecraftDelegate) {
            liveClass = safeConfig.nativeMinecraftRuntimeHostClass();
        }
        if (liveId.isBlank() && afterNamesNativeMinecraftDelegate) {
            liveId = safeConfig.nativeMinecraftRuntimeHostId();
        }
        return safeConfig.nativeMinecraftRuntimeHostClass().equals(liveClass)
                && safeConfig.nativeMinecraftRuntimeHostId().equals(liveId)
                && compatibilityBackendClass.isBlank()
                && compatibilityDelegate.isBlank()
                && !afterNamesCompatibilityDelegate
                && !Boolean.TRUE.equals(record.get("compatibilityFallbackUsed"));
    }

    private static boolean liveUiHostAttachedCandidate(
            boolean bootstrapEnteredLiveClient,
            boolean nativeClientUiHostAttached,
            boolean nativeUiClientThreadAccepted,
            boolean nativeUiLiveWindowHandlePresent,
            boolean nativeUiFallbackHostAttached,
            boolean nativeHeadlessUiHostAttached
    ) {
        return bootstrapEnteredLiveClient
                && nativeClientUiHostAttached
                && nativeUiClientThreadAccepted
                && nativeUiLiveWindowHandlePresent
                && !nativeUiFallbackHostAttached
                && !nativeHeadlessUiHostAttached;
    }

    private static void putTarget(Map<String, Object> targets, String key, boolean passed, String evidence) {
        Map<String, Object> target = new LinkedHashMap<>();
        target.put("passed", passed);
        target.put("evidence", evidence == null ? "" : evidence);
        targets.put(key, target);
    }

    private static List<String> missingTargets(Map<String, Object> targets) {
        List<String> missing = new ArrayList<>();
        for (Map.Entry<String, Object> entry : targets.entrySet()) {
            if (!Boolean.TRUE.equals(object(entry.getValue()).get("passed"))) {
                missing.add(entry.getKey());
            }
        }
        return missing;
    }

    private static boolean anyTargetPassed(Map<String, Object> targets) {
        for (Object value : targets.values()) {
            if (Boolean.TRUE.equals(object(value).get("passed"))) {
                return true;
            }
        }
        return false;
    }

    private static int integer(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    private static List<Map<String, Object>> objectList(Object value) {
        if (!(value instanceof Iterable<?> iterable)) {
            return List.of();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object item : iterable) {
            if (item instanceof Map<?, ?> map) {
                Map<String, Object> object = new LinkedHashMap<>();
                map.forEach((key, child) -> object.put(String.valueOf(key), child));
                result.add(Map.copyOf(object));
            }
        }
        return List.copyOf(result);
    }

    private static Map<String, Object> object(Object value) {
        if (!(value instanceof Map<?, ?> map)) {
            return Map.of();
        }
        Map<String, Object> object = new LinkedHashMap<>();
        map.forEach((key, item) -> object.put(String.valueOf(key), item));
        return Map.copyOf(object);
    }

    public record Config(
            String adapterCoreServiceId,
            String namespace,
            String nativeLoaderBackendClass,
            String nativeLoaderRuntimeLane,
            String nativeMinecraftRuntimeHostClass,
            String nativeMinecraftRuntimeHostId,
            String compatibilityDelegateClass,
            String compatibilityDelegateId,
            String hudLedgerTarget
    ) {
        public Config {
            adapterCoreServiceId = text(adapterCoreServiceId);
            namespace = text(namespace);
            nativeLoaderBackendClass = text(nativeLoaderBackendClass);
            nativeLoaderRuntimeLane = text(nativeLoaderRuntimeLane);
            nativeMinecraftRuntimeHostClass = text(nativeMinecraftRuntimeHostClass);
            nativeMinecraftRuntimeHostId = text(nativeMinecraftRuntimeHostId);
            compatibilityDelegateClass = text(compatibilityDelegateClass);
            compatibilityDelegateId = text(compatibilityDelegateId);
            hudLedgerTarget = text(hudLedgerTarget);
        }

        static Config safe(Config config) {
            return config == null ? new Config("", "", "", "", "", "", "", "", "") : config;
        }

        private static String text(String value) {
            return value == null ? "" : value;
        }
    }
}
