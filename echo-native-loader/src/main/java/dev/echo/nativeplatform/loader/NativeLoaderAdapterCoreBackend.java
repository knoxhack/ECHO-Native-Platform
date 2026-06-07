package dev.echo.nativeplatform.loader;

import dev.echo.nativeplatform.contracts.EchoNativeLoadStatus;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public final class NativeLoaderAdapterCoreBackend {
    public static final String SERVICE_ID = "adaptercore.native_loader.backend";
    private static final String RUNTIME_LANE = "Native Loader";

    private final NativeLoaderRuntimeHost host;
    private final NativeLoaderServiceBridge bridge;
    private final NativeLoaderMutationLedger ledger;
    private final NativeLoaderCommandHost commandHost;
    private final NativeLoaderNetworkHost networkHost;
    private final NativeLoaderConfigHost configHost;
    private final NativeLoaderLifecycleEventHost lifecycleEventHost;

    public NativeLoaderAdapterCoreBackend(
            NativeLoaderRuntimeHost host,
            NativeLoaderServiceBridge bridge,
            NativeLoaderMutationLedger ledger
    ) {
        this(host, bridge, ledger, null, null, null, null);
    }

    public NativeLoaderAdapterCoreBackend(
            NativeLoaderRuntimeHost host,
            NativeLoaderServiceBridge bridge,
            NativeLoaderMutationLedger ledger,
            NativeLoaderCommandHost commandHost,
            NativeLoaderNetworkHost networkHost,
            NativeLoaderConfigHost configHost,
            NativeLoaderLifecycleEventHost lifecycleEventHost
    ) {
        this.host = host;
        this.bridge = bridge;
        this.ledger = ledger;
        this.commandHost = commandHost;
        this.networkHost = networkHost;
        this.configHost = configHost;
        this.lifecycleEventHost = lifecycleEventHost;
    }

    public NativeLoaderRuntimeHost host() {
        return host;
    }

    public NativeLoaderServiceBridge serviceBridge() {
        return bridge;
    }

    public NativeLoaderMutationLedger mutationLedger() {
        return ledger;
    }

    public Map<String, Object> serviceBridgeReport() {
        return bridge.toReport();
    }

    public Map<String, Object> runtimeHostReport() {
        return host.runtimeHostReport();
    }

    public Map<String, Object> snapshot() {
        return host.snapshot();
    }

    public Map<String, Object> serviceSurfaceReport(String surface) {
        return bridge.toSurfaceReport(surface);
    }

    public NativeLoaderMutationLedger.MutationRecord grantItem(String playerId, String itemId, int count) {
        return mutate(
                "inventory",
                "grant_item",
                playerId + ":" + itemId,
                () -> host.snapshot().get("inventory"),
                () -> host.grantItem(playerId, itemId, count)
        );
    }

    public NativeLoaderMutationLedger.MutationRecord removeItem(String playerId, String itemId, int count) {
        return mutate(
                "inventory",
                "remove_item",
                playerId + ":" + itemId,
                () -> host.snapshot().get("inventory"),
                () -> host.removeItem(playerId, itemId, count)
        );
    }

    public NativeLoaderMutationLedger.MutationRecord updatePlayerState(String playerId, String key, String value) {
        return mutate(
                "player_state",
                "update",
                playerId + ":" + key,
                () -> host.snapshot().get("playerState"),
                () -> host.updatePlayerState(playerId, key, value)
        );
    }

    public NativeLoaderMutationLedger.MutationRecord placeBlock(String dimension, int x, int y, int z, String blockId) {
        return mutate(
                "world_blocks",
                "place_block",
                dimension + ":" + x + "," + y + "," + z,
                () -> host.snapshot().get("worldBlocks"),
                () -> host.placeBlock(dimension, x, y, z, blockId)
        );
    }

    public NativeLoaderMutationLedger.MutationRecord updateWorldState(String dimension, String key, String value) {
        return mutate(
                "world_state",
                "update",
                dimension + ":" + key,
                () -> host.snapshot().get("worldState"),
                () -> host.updateWorldState(dimension, key, value)
        );
    }

    public NativeLoaderMutationLedger.MutationRecord placeStructure(String dimension, String structureId, int x, int y, int z) {
        return mutate(
                "structures",
                "place_structure",
                dimension + ":" + x + "," + y + "," + z,
                () -> host.snapshot().get("structures"),
                () -> host.placeStructure(dimension, structureId, x, y, z)
        );
    }

    public NativeLoaderMutationLedger.MutationRecord updateBlockEntity(String dimension, int x, int y, int z, String key, String value) {
        return mutate(
                "block_entities",
                "update",
                dimension + ":" + x + "," + y + "," + z + ":" + key,
                () -> host.snapshot().get("blockEntities"),
                () -> host.updateBlockEntity(dimension, x, y, z, key, value)
        );
    }

    public NativeLoaderMutationLedger.MutationRecord updateCapability(String target, String capability, String value) {
        return mutate(
                "capabilities",
                "update",
                target + ":" + capability,
                () -> host.snapshot().get("capabilities"),
                () -> host.updateCapability(target, capability, value)
        );
    }

    public NativeLoaderMutationLedger.MutationRecord emitEvent(String eventType, String payload) {
        return mutate(
                "events",
                "emit",
                eventType,
                () -> host.snapshot().get("events"),
                () -> host.emitEvent(eventType, payload)
        );
    }

    public NativeLoaderMutationLedger.MutationRecord sendPacketHud(String channel, String payload) {
        return mutate(
                "packets_hud",
                "send",
                channel,
                () -> host.snapshot().get("packetsHud"),
                () -> host.sendPacketHud(channel, payload)
        );
    }

    public NativeLoaderMutationLedger.MutationRecord writeSaveData(String key, String value) {
        return mutate(
                "save_data",
                "write",
                key,
                () -> host.snapshot().get("saveData"),
                () -> host.writeSaveData(key, value)
        );
    }

    public NativeLoaderMutationLedger.MutationRecord deleteSaveData(String key) {
        return mutate(
                "save_data",
                "delete",
                key,
                () -> host.snapshot().get("saveData"),
                () -> host.deleteSaveData(key)
        );
    }

    public NativeLoaderMutationLedger.MutationRecord emitHud(String channel, String message) {
        return mutate(
                "hud",
                "notify",
                channel,
                () -> host.snapshot().get("hud"),
                () -> host.emitHud(channel, message)
        );
    }

    public NativeLoaderMutationLedger.MutationRecord updateMission(String missionId, String phase, String objectiveKey) {
        return mutate(
                "missions",
                "update",
                missionId + ":" + phase + ":" + (objectiveKey == null ? "" : objectiveKey),
                () -> host.snapshot().get("missions"),
                () -> host.updateMission(missionId, phase, objectiveKey)
        );
    }

    public NativeLoaderMutationLedger.MutationRecord emitFeedback(String source, String message) {
        return mutate(
                "feedback",
                "emit",
                source,
                () -> host.snapshot().get("feedback"),
                () -> host.emitFeedback(source, message)
        );
    }

    public NativeLoaderMutationLedger.MutationRecord unsupportedRuntimeHook(
            String normalizedSurface,
            String declaredSurface,
            String targetId
    ) {
        String surface = normalizedSurface == null || normalizedSurface.isBlank()
                ? "runtime_hooks"
                : normalizedSurface.trim();
        String target = (declaredSurface == null || declaredSurface.isBlank() ? surface : declaredSurface)
                + ":"
                + (targetId == null ? "" : targetId);
        return unsupported(surface, "runtime_hook.unsupported", target, SERVICE_ID);
    }

    public NativeLoaderMutationLedger.MutationRecord clientTick(String phase, Map<String, Object> payload) {
        return mutate(
                "client_tick",
                "tick",
                phase,
                () -> host.snapshot().get("clientTicks"),
                () -> host.clientTick(phase, payload)
        );
    }

    public NativeLoaderMutationLedger.MutationRecord renderLayer(String layerId, Map<String, Object> payload) {
        return mutate(
                "render_layers",
                "render",
                layerId,
                () -> host.snapshot().get("renderLayers"),
                () -> host.renderLayer(layerId, payload)
        );
    }

    public NativeLoaderMutationLedger.MutationRecord screenEvent(String screenId, String eventType, Map<String, Object> payload) {
        return mutate(
                "screen_events",
                eventType,
                screenId,
                () -> host.snapshot().get("screenEvents"),
                () -> host.screenEvent(screenId, eventType, payload)
        );
    }

    public NativeLoaderMutationLedger.MutationRecord keybind(String keybindId, String action, Map<String, Object> payload) {
        return mutate(
                "keybinds",
                action,
                keybindId,
                () -> host.snapshot().get("keybinds"),
                () -> host.keybind(keybindId, action, payload)
        );
    }

    public NativeLoaderMutationLedger.MutationRecord reloadResources(
            String moduleId,
            String resourceId,
            String scope,
            Map<String, Object> evidence
    ) {
        return mutate(
                "resource_reloads",
                "reload",
                moduleId + ":" + resourceId,
                () -> host.snapshot().get("resourceReloads"),
                () -> host.reloadResources(moduleId, resourceId, scope, evidence)
        );
    }

    public NativeLoaderMutationLedger.MutationRecord saveHook(String hookId, Map<String, Object> payload) {
        return mutate(
                "save_hooks",
                "hook",
                hookId,
                () -> host.snapshot().get("saveHooks"),
                () -> host.saveHook(hookId, payload)
        );
    }

    public NativeLoaderMutationLedger.MutationRecord syncServerClient(String channel, String payload) {
        return mutate(
                "server_client_sync",
                "sync",
                channel,
                () -> host.snapshot().get("serverClientSync"),
                () -> host.syncServerClient(channel, payload)
        );
    }

    public NativeLoaderMutationLedger.MutationRecord registerCommand(
            String moduleId,
            String commandId,
            String targetSurface,
            String targetBridge,
            Map<String, Object> evidence
    ) {
        if (commandHost == null) {
            return unsupported("commands", "register", commandId, NativeLoaderCommandHost.SERVICE_ID);
        }
        return mutateWithService(
                "commands",
                "register",
                commandId,
                NativeLoaderCommandHost.SERVICE_ID,
                commandHost::toReport,
                () -> commandHost.registerDeclaredCommand(moduleId, commandId, targetSurface, targetBridge, evidence)
        );
    }

    public NativeLoaderMutationLedger.MutationRecord registerNetworkPacket(
            String moduleId,
            String packetId,
            String surface,
            String sourceRuntimeTarget,
            List<String> consumers,
            Map<String, Object> evidence
    ) {
        if (networkHost == null) {
            return unsupported("network_channels", "register", packetId, NativeLoaderNetworkHost.SERVICE_ID);
        }
        return mutateWithService(
                "network_channels",
                "register",
                packetId,
                NativeLoaderNetworkHost.SERVICE_ID,
                networkHost::toReport,
                () -> networkHost.registerDeclaredPacket(
                        moduleId,
                        packetId,
                        surface,
                        sourceRuntimeTarget,
                        consumers == null ? List.of() : List.copyOf(consumers),
                        evidence)
        );
    }

    public NativeLoaderMutationLedger.MutationRecord reloadConfig(
            String moduleId,
            String configId,
            String scope,
            Map<String, Object> evidence
    ) {
        if (configHost == null) {
            return unsupported("config_reloads", "reload", configId, NativeLoaderConfigHost.SERVICE_ID);
        }
        return mutateWithService(
                "config_reloads",
                "reload",
                configId,
                NativeLoaderConfigHost.SERVICE_ID,
                configHost::toReport,
                () -> configHost.registerConfig(moduleId, configId, scope, evidence)
        );
    }

    public NativeLoaderMutationLedger.MutationRecord lifecyclePhase(
            String moduleId,
            String phaseId,
            Map<String, Object> evidence
    ) {
        if (lifecycleEventHost == null) {
            return unsupported("lifecycle_phases", "phase", phaseId, NativeLoaderLifecycleEventHost.LIFECYCLE_SERVICE_ID);
        }
        return mutateWithService(
                "lifecycle_phases",
                "phase",
                phaseId,
                NativeLoaderLifecycleEventHost.LIFECYCLE_SERVICE_ID,
                lifecycleEventHost::toReport,
                () -> {
                    lifecycleEventHost.recordDeclaredLifecyclePhase(moduleId, phaseId, evidence);
                    return EchoNativeLoadStatus.MUTATED;
                }
        );
    }

    public NativeLoaderMutationLedger.MutationRecord publishRuntimeEvent(
            String sourceModule,
            String eventId,
            Map<String, Object> payload,
            String status
    ) {
        if (lifecycleEventHost == null) {
            return unsupported("events", "publish", eventId, NativeLoaderLifecycleEventHost.EVENT_SERVICE_ID);
        }
        return mutateWithService(
                "events",
                "publish",
                eventId,
                NativeLoaderLifecycleEventHost.EVENT_SERVICE_ID,
                lifecycleEventHost::toReport,
                () -> {
                    lifecycleEventHost.publish(sourceModule, eventId, payload, loadStatus(status));
                    return EchoNativeLoadStatus.MUTATED;
                }
        );
    }

    private NativeLoaderMutationLedger.MutationRecord mutate(
            String surface,
            String action,
            String target,
            Supplier<Object> snapshot,
            Supplier<EchoNativeLoadStatus> operation
    ) {
        return mutateWithService(surface, action, target, SERVICE_ID, snapshot, operation);
    }

    private NativeLoaderMutationLedger.MutationRecord mutateWithService(
            String surface,
            String action,
            String target,
            String serviceId,
            Supplier<Object> snapshot,
            Supplier<EchoNativeLoadStatus> operation
    ) {
        var activeSurfaceServices = bridge.activeRuntimeServicesForSurface(surface);
        NativeLoaderResolvedRuntimeService service = bridge.resolve(serviceId, surface).orElse(null);
        if (service == null) {
            return ledger.record(
                    surface,
                    action,
                    target,
                    EchoNativeLoadStatus.UNSUPPORTED,
                    "",
                    "",
                    serviceId,
                    null,
                    getClass().getName(),
                    host.getClass().getName(),
                    RUNTIME_LANE,
                    host.runtimeHostId(),
                    host.runtimeHostRegistered(),
                    activeSurfaceServices,
                    runtimeEvidence(surface, null)
            );
        }
        Object before = snapshot.get();
        EchoNativeLoadStatus status = operation.get();
        Object after = snapshot.get();
        return ledger.record(
                surface,
                action,
                target,
                status,
                before,
                after,
                serviceId,
                service,
                getClass().getName(),
                host.getClass().getName(),
                RUNTIME_LANE,
                host.runtimeHostId(),
                host.runtimeHostRegistered(),
                activeSurfaceServices,
                runtimeEvidence(surface, after)
        );
    }

    private NativeLoaderMutationLedger.MutationRecord unsupported(
            String surface,
            String action,
            String target,
            String serviceId
    ) {
        return ledger.record(
                surface,
                action,
                target,
                EchoNativeLoadStatus.UNSUPPORTED,
                "",
                "",
                serviceId,
                bridge.resolve(serviceId, surface).orElse(null),
                getClass().getName(),
                host.getClass().getName(),
                RUNTIME_LANE,
            host.runtimeHostId(),
            host.runtimeHostRegistered(),
            bridge.activeRuntimeServicesForSurface(surface),
            runtimeEvidence(surface, null)
        );
    }

    private Map<String, Object> runtimeEvidence(String surface, Object surfaceSnapshot) {
        Map<String, Object> evidence = new LinkedHashMap<>(host.runtimeHostReport());
        evidence.put("adapterCoreSurface", surface == null ? "" : surface);
        boolean serviceSnapshot = surfaceSnapshot instanceof Map<?, ?> report
                && (report.containsKey("liveRuntimeReleaseProofSatisfied")
                || report.containsKey("minecraftRuntimeAccessed")
                || report.containsKey("liveRuntimeMutationCount"));
        boolean directHostSurfaceMutated = !serviceSnapshot && hostSurfaceMutated(surface, evidence);
        evidence.put("liveRuntimeSurfaceMutationSatisfied", directHostSurfaceMutated);
        if (directHostSurfaceMutated
                && bool(evidence.get("liveRuntimeAccessed"))
                && bool(evidence.get("minecraftRuntimeAccessed"))
                && bool(evidence.get("liveRuntimeMutationSupported"))) {
            Map<String, Object> surfaceProofEvidence = hostSurfaceProofEvidence(surface, evidence);
            evidence.put("adapterCoreSurfaceDispatchId", String.valueOf(surfaceProofEvidence.getOrDefault("liveRuntimeDispatchId", "")));
            evidence.put("surfaceLiveRuntimeReleaseProofSatisfied", true);
            evidence.put("surfaceLiveRuntimeAccessed", true);
            evidence.put("surfaceMinecraftRuntimeAccessed", true);
            evidence.put("surfaceLiveRuntimeMutationCount", intValue(evidence.get("liveRuntimeBridgeMutationCount")));
            evidence.put("surfaceLiveRuntimeProofEvidence", surfaceProofEvidence);
        }
        if (surfaceSnapshot instanceof Map<?, ?> report && serviceSnapshot) {
            boolean serviceProof = bool(report.get("liveRuntimeReleaseProofSatisfied"));
            boolean serviceLiveAccess = bool(report.get("liveRuntimeAccessed"));
            boolean serviceMinecraftAccess = bool(report.get("minecraftRuntimeAccessed"));
            boolean serviceLiveMutation = intValue(report.get("liveRuntimeMutationCount")) > 0;
            Map<String, Object> surfaceProofEvidence = subsystemSurfaceProofEvidence(surface, report);
            boolean serviceConcreteProof = subsystemSurfaceProofSatisfied(surfaceProofEvidence);
            evidence.put("adapterCoreSurfaceDispatchId", String.valueOf(surfaceProofEvidence.getOrDefault("liveRuntimeDispatchId", "")));
            evidence.put("surfaceLiveRuntimeReleaseProofSatisfied", serviceProof);
            evidence.put("surfaceLiveRuntimeAccessed", serviceLiveAccess);
            evidence.put("surfaceMinecraftRuntimeAccessed", serviceMinecraftAccess);
            evidence.put("surfaceLiveRuntimeMutationCount", intValue(report.get("liveRuntimeMutationCount")));
            evidence.put("surfaceLiveRuntimeProofEvidence", surfaceProofEvidence);
            if (serviceProof && serviceLiveAccess && serviceMinecraftAccess && serviceLiveMutation && serviceConcreteProof) {
                evidence.put("liveRuntimeReleaseProofSatisfied", true);
                evidence.put("liveRuntimeSurfaceMutationSatisfied", true);
            }
        }
        return Map.copyOf(evidence);
    }

    private static EchoNativeLoadStatus loadStatus(String value) {
        if (value == null || value.isBlank()) {
            return EchoNativeLoadStatus.DISCOVERED;
        }
        try {
            return EchoNativeLoadStatus.valueOf(value.trim());
        } catch (IllegalArgumentException ignored) {
            return EchoNativeLoadStatus.DISCOVERED;
        }
    }

    private static boolean hostSurfaceMutated(String surface, Map<String, Object> evidence) {
        Object proofs = evidence.get("liveRuntimeBridgeProofBySurface");
        if (proofs instanceof Map<?, ?> proofMap && proofMap.containsKey(surface)) {
            return Boolean.TRUE.equals(proofMap.get(surface));
        }
        return false;
    }

    private static Map<String, Object> hostSurfaceProofEvidence(String surface, Map<String, Object> evidence) {
        Object proofs = evidence.get("liveRuntimeBridgeProofEvidenceBySurface");
        if (proofs instanceof Map<?, ?> proofMap && proofMap.get(surface) instanceof Map<?, ?> surfaceEvidence) {
            Map<String, Object> copy = new LinkedHashMap<>();
            surfaceEvidence.forEach((key, value) -> copy.put(String.valueOf(key), value));
            return Map.copyOf(copy);
        }
        return Map.of();
    }

    private static Map<String, Object> subsystemSurfaceProofEvidence(String surface, Map<?, ?> report) {
        for (String entryKey : subsystemEntryKeys(surface)) {
            Object entries = report.get(entryKey);
            if (!(entries instanceof Iterable<?> iterable)) {
                continue;
            }
            Map<String, Object> latest = Map.of();
            for (Object item : iterable) {
                Map<String, Object> entry = objectMap(item);
                Map<String, Object> proof = subsystemEntryProofEvidence(entry);
                if (subsystemSurfaceProofSatisfied(proof)) {
                    latest = proof;
                }
            }
            if (!latest.isEmpty()) {
                return latest;
            }
        }
        return Map.of();
    }

    private static List<String> subsystemEntryKeys(String surface) {
        return switch (surface == null ? "" : surface) {
            case "commands" -> List.of("commands");
            case "network_channels" -> List.of("packets");
            case "config_reloads" -> List.of("configs");
            case "lifecycle_phases" -> List.of("lifecycleEvents");
            case "events" -> List.of("publishedEvents");
            default -> List.of();
        };
    }

    private static Map<String, Object> subsystemEntryProofEvidence(Map<String, Object> entry) {
        Map<String, Object> proof = new LinkedHashMap<>();
        proof.putAll(objectMap(entry.get("evidence")));
        proof.putAll(objectMap(entry.get("liveRuntimeEvidence")));
        for (String key : List.of(
                "liveRuntimeDispatchId",
                "liveRuntimeSurface",
                "subsystemLiveRuntimeDispatchProofSatisfied",
                "liveMinecraftMutation",
                "minecraftRuntimeAccessed",
                "liveRuntimeDispatchMinecraftAccessed",
                "liveRuntimeDispatchMutationSupported",
                "liveRuntimeDispatchLiveMutation")) {
            if (entry.containsKey(key)) {
                proof.put(key, entry.get(key));
            }
        }
        return Map.copyOf(proof);
    }

    private static boolean subsystemSurfaceProofSatisfied(Map<String, Object> proof) {
        return !proof.isEmpty()
                && bool(proof.get("subsystemLiveRuntimeDispatchProofSatisfied"))
                && bool(proof.get("liveMinecraftMutation"))
                && bool(proof.get("minecraftRuntimeAccessed"))
                && bool(proof.get("liveRuntimeDispatchMinecraftAccessed"))
                && bool(proof.get("liveRuntimeDispatchMutationSupported"))
                && bool(proof.get("liveRuntimeDispatchLiveMutation"))
                && !String.valueOf(proof.getOrDefault("liveRuntimeDispatchId", "")).isBlank()
                && !String.valueOf(proof.getOrDefault("liveRuntimeSurface", "")).isBlank();
    }

    private static Map<String, Object> objectMap(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> copy = new LinkedHashMap<>();
            map.forEach((key, item) -> copy.put(String.valueOf(key), item));
            return Map.copyOf(copy);
        }
        return Map.of();
    }

    private static boolean bool(Object value) {
        return Boolean.TRUE.equals(value) || "true".equalsIgnoreCase(String.valueOf(value));
    }

    private static int intValue(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }
}
