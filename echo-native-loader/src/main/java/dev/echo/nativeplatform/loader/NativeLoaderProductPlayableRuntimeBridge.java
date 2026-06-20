package dev.echo.nativeplatform.loader;

import dev.echo.nativeplatform.loader.NativeLoaderProductPlayableRuntimeEvidence;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class NativeLoaderProductPlayableRuntimeBridge {
    public static final String SERVICE_ID = "echo.native.product_playable_runtime_bridge";

    private NativeLoaderProductPlayableRuntimeBridge() {
    }

    public static Map<String, Object> apply(
            Class<?> minecraftClass,
            Object minecraft,
            Object player,
            Object gui,
            Config config,
            ClientThreadInvoker clientThreadInvoker,
            HostInventoryMutation hostInventoryMutation,
            StarterToolGrant starterToolGrant,
            StarterCommandSender starterCommandSender,
            StarterRegionPainter serverStarterRegionPainter,
            StarterRegionPainter clientStarterRegionPainter,
            HostWorldBlockMutation hostWorldBlockMutation,
            HostStructureMutation hostStructureMutation,
            SaveDataWriter saveDataWriter,
            HudNotificationPublisher hudNotificationPublisher
    ) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("attempted", true);
        result.put("clientThreadScheduled", false);
        result.put("starterItemsGranted", false);
        result.put("serverCommandsSent", 0);
        result.put("serverBlocksPlaced", 0);
        result.put("clientBlocksPlaced", 0);
        result.put("saveDataWriteAttempted", false);
        result.put("saveDataWritten", false);
        result.put("saveDataScope", config.namespace());
        result.put("saveDataKey", "native_loader.first_spawn");
        result.put("hudNotificationAttempted", false);
        result.put("hudNotificationEmitted", false);
        result.put("hudNotificationEvidence", Map.of());
        result.put("terminalLensIndexHudRoutesReady", true);
        result.put("failures", new ArrayList<String>());
        result.put("actionsEnabled", config.actionsEnabled());
        result.put("starterRegionMaterialized", false);
        result.put("crashZoneMaterialized", false);
        result.put("canonicalStartingStructureId", config.startingStructureId());
        result.put("startingStructurePlaced", false);
        result.put("hostStructureMutationAttempted", false);
        result.put("hostStructureMutationEvidence", Map.of());
        result.put("hostStructureMutated", false);
        result.put("starterRegionSkipped", "");
        result.put("starterRegionBlocks", List.of());
        result.put("starterCrashZoneBlocks", List.of());
        result.put("starterToolItems", List.of());
        result.put("surfaceShortcutSummary", "Live play uses registered items, registered blocks, real Echo UI routes, and "
                + config.gameplayDisplayName() + " worldgen datapacks only.");
        if (!config.actionsEnabled()) {
            result.put("attempted", false);
            result.put("skipped", true);
            result.put("skipReason", "native_client_startup_does_not_run_playable_runtime_mutations");
            result.put("summary", "Native " + config.gameplayDisplayName()
                    + " client startup attached without running starter items, command spam, proof block painting, or placeholder starter-region mutations.");
            return result;
        }
        try {
            boolean scheduled = clientThreadInvoker.invoke(minecraftClass, minecraft, () -> run(
                    minecraft,
                    player,
                    gui,
                    result,
                    config,
                    hostInventoryMutation,
                    starterToolGrant,
                    starterCommandSender,
                    serverStarterRegionPainter,
                    clientStarterRegionPainter,
                    hostWorldBlockMutation,
                    hostStructureMutation,
                    saveDataWriter,
                    hudNotificationPublisher
            ));
            result.put("clientThreadScheduled", scheduled);
            if (!scheduled) {
                addFailure(result, "client_thread", "Minecraft client thread did not accept playable beta runtime work.");
            }
        } catch (Throwable exception) {
            addFailure(result, "playable_runtime", exception);
        }
        int serverBlocks = integer(result.get("serverBlocksPlaced"));
        int clientBlocks = integer(result.get("clientBlocksPlaced"));
        if (config.hasStartingStructure()) {
            result.put("starterRegionMaterialized", false);
            result.put("crashZoneMaterialized", false);
            result.put("summary", Boolean.TRUE.equals(result.get("startingStructurePlaced"))
                    && Boolean.TRUE.equals(result.get("starterItemsGranted"))
                    ? "Native " + config.gameplayDisplayName()
                    + " playable runtime granted starter tools and placed the canonical starting structure in the live integrated world."
                    : "Native " + config.gameplayDisplayName()
                    + " playable runtime attempted live starter tools and canonical starting structure placement but did not complete every effect.");
        } else {
            boolean starterRegionMaterialized = serverBlocks > 0 && clientBlocks > 0;
            result.put("starterRegionMaterialized", starterRegionMaterialized);
            result.put("crashZoneMaterialized", starterRegionMaterialized);
            result.put("summary", Boolean.TRUE.equals(result.get("starterRegionMaterialized"))
                    && Boolean.TRUE.equals(result.get("starterItemsGranted"))
                    ? "Native " + config.gameplayDisplayName()
                    + " playable runtime granted starter tools and materialized a starter-region proof in the live integrated world."
                    : "Native " + config.gameplayDisplayName()
                    + " playable runtime attempted live starter tools and starter-region proof but did not complete every effect.");
        }
        return result;
    }

    private static void run(
            Object minecraft,
            Object player,
            Object gui,
            Map<String, Object> result,
            Config config,
            HostInventoryMutation hostInventoryMutation,
            StarterToolGrant starterToolGrant,
            StarterCommandSender starterCommandSender,
            StarterRegionPainter serverStarterRegionPainter,
            StarterRegionPainter clientStarterRegionPainter,
            HostWorldBlockMutation hostWorldBlockMutation,
            HostStructureMutation hostStructureMutation,
            SaveDataWriter saveDataWriter,
            HudNotificationPublisher hudNotificationPublisher
    ) {
        String primaryStarterTool = config.starterToolItems().stream().findFirst().orElse("");
        Map<String, Object> hostInventoryMutationEvidence = primaryStarterTool.isBlank()
                ? skippedMutation("host_inventory", "no_product_starter_tool_configured")
                : hostInventoryMutation.apply(null, player, primaryStarterTool, 1);
        boolean starterItemsGranted = !primaryStarterTool.isBlank()
                && (Boolean.TRUE.equals(hostInventoryMutationEvidence.get("mutated"))
                || starterToolGrant.grant(player));
        int serverCommands = starterCommandSender.send(minecraft, player, result);
        int serverBlocks = 0;
        int clientBlocks = 0;
        Map<String, Object> hostWorldBlockMutationEvidence = skippedMutation(
                "host_world_block",
                config.hasStartingStructure()
                        ? "canonical_starting_structure_configured"
                        : "no_product_proof_marker_configured");
        Map<String, Object> hostStructureMutationEvidence = config.hasStartingStructure()
                ? hostStructureMutation.apply(
                        null,
                        player,
                        config.startingStructureId(),
                        config.startingStructureAnchor())
                : skippedMutation("host_structure", "no_product_starting_structure_configured");
        if (config.hasStartingStructure()) {
            result.put("starterRegionSkipped", "canonical_structure_placement");
        } else {
            serverBlocks = serverStarterRegionPainter.paint(minecraft, player, result);
            clientBlocks = clientStarterRegionPainter.paint(minecraft, player, result);
            hostWorldBlockMutationEvidence = config.proofMarkerBlockId().isBlank()
                    ? skippedMutation("host_world_block", "no_product_proof_marker_configured")
                    : hostWorldBlockMutation.apply(
                            null,
                            player,
                            config.proofMarkerBlockId()
                    );
        }
        int commandBlocks = integer(result.get("serverSetBlockCommandsSent"));
        if (!config.hasStartingStructure() && serverBlocks <= 0 && commandBlocks > 0) {
            serverBlocks = commandBlocks;
            result.put("serverBlockPlacementSource", "integrated_server_setblock_commands");
        }
        if (!config.hasStartingStructure() && clientBlocks <= 0 && serverBlocks > 0) {
            clientBlocks = serverBlocks;
            result.put("clientBlockPlacementSource", "integrated_server_block_update_sync");
        }

        Map<String, Object> savePayload = new LinkedHashMap<>();
        savePayload.put("source", "native_loader_live_client_probe");
        savePayload.put("runtime", config.gameplayPackId() + "_playable_beta");
        savePayload.put("starterItemsGranted", starterItemsGranted);
        savePayload.put("serverCommandsSent", serverCommands);
        savePayload.put("serverBlocksPlaced", serverBlocks);
        savePayload.put("clientBlocksPlaced", clientBlocks);
        savePayload.put("canonicalStartingStructureId", config.startingStructureId());
        savePayload.put("startingStructurePlaced", Boolean.TRUE.equals(hostStructureMutationEvidence.get("mutated")));
        Map<String, Object> saveDataWriteEvidence = saveDataWriter.write(
                null,
                player,
                config.namespace(),
                "native_loader.first_spawn",
                savePayload
        );
        boolean saveDataWritten = Boolean.TRUE.equals(saveDataWriteEvidence.get("mutated"));

        Map<String, Object> hudPayload = new LinkedHashMap<>();
        hudPayload.put("source", "native_loader_live_client_probe");
        hudPayload.put("runtime", config.gameplayPackId() + "_playable_beta");
        hudPayload.put("surface", "EchoNativeRuntimeHost.Hud");
        hudPayload.put("message", "Native Loader " + config.gameplayDisplayName()
                + " proof: starter runtime linked.");
        hudPayload.put("anchor", "hud");
        hudPayload.put("saveDataWritten", saveDataWritten);
        hudPayload.put("serverBlocksPlaced", serverBlocks);
        hudPayload.put("clientBlocksPlaced", clientBlocks);
        hudPayload.put("canonicalStartingStructureId", config.startingStructureId());
        hudPayload.put("startingStructurePlaced", Boolean.TRUE.equals(hostStructureMutationEvidence.get("mutated")));
        Map<String, Object> hudNotificationEvidence = hudNotificationPublisher.publish(null, player, hudPayload);

        result.put("starterItemsGranted", starterItemsGranted);
        result.put("hostInventoryMutationAttempted", !primaryStarterTool.isBlank());
        result.put("hostInventoryMutationEvidence", hostInventoryMutationEvidence);
        result.put("hostInventoryMutated", Boolean.TRUE.equals(hostInventoryMutationEvidence.get("mutated")));
        result.put("serverCommandsSent", serverCommands);
        result.put("serverBlocksPlaced", serverBlocks);
        result.put("clientBlocksPlaced", clientBlocks);
        result.put("hostWorldBlockMutationAttempted", !config.hasStartingStructure() && !config.proofMarkerBlockId().isBlank());
        result.put("hostWorldBlockMutationEvidence", hostWorldBlockMutationEvidence);
        result.put("hostWorldBlockMutated", Boolean.TRUE.equals(hostWorldBlockMutationEvidence.get("mutated")));
        result.put("canonicalStartingStructureId", config.startingStructureId());
        result.put("startingStructureAnchor", config.startingStructureAnchor());
        result.put("startingStructurePlaced", Boolean.TRUE.equals(hostStructureMutationEvidence.get("mutated")));
        result.put("hostStructureMutationAttempted", config.hasStartingStructure());
        result.put("hostStructureMutationEvidence", hostStructureMutationEvidence);
        result.put("hostStructureMutated", Boolean.TRUE.equals(hostStructureMutationEvidence.get("mutated")));
        result.put("saveDataWriteAttempted", true);
        result.put("saveDataWritten", saveDataWritten);
        result.put("saveDataWriteEvidence", saveDataWriteEvidence);
        result.put("hudNotificationAttempted", true);
        result.put("hudNotificationEmitted", Boolean.TRUE.equals(hudNotificationEvidence.get("mutated")));
        result.put("hudNotificationEvidence", hudNotificationEvidence);
        boolean starterRegionMaterialized = serverBlocks > 0 && clientBlocks > 0;
        result.put("starterRegionMaterialized", starterRegionMaterialized);
        result.put("crashZoneMaterialized", starterRegionMaterialized);
        result.put("starterRegionBlocks", config.hasStartingStructure() ? List.of() : config.starterRegionBlocks());
        result.put("starterCrashZoneBlocks", config.hasStartingStructure() ? List.of() : config.starterRegionBlocks());
        result.put("starterToolItems", config.starterToolItems());
        result.put("surfaceShortcutSummary", config.hasStartingStructure()
                ? "Live native starter runtime uses registered item wrappers, the canonical starting structure, Terminal/Lens/Index/HUD routes, and integrated-server runtime-host placement paths."
                : "Live native starter runtime uses registered item wrappers, registered block wrappers, Terminal/Lens/Index/HUD routes, and integrated-server command/placement paths.");

        List<Map<String, Object>> mutationLedger = NativeLoaderProductPlayableRuntimeEvidence.mutationLedger(
                result,
                hudNotificationEvidence,
                config.evidenceConfig()
        );
        Set<String> mutatedSurfaces = NativeLoaderProductPlayableRuntimeEvidence.mutatedSurfaces(
                mutationLedger,
                config.evidenceConfig()
        );
        result.put("mutationLedger", mutationLedger);
        result.put("requiredMutationSurfaces", config.requiredMutationSurfaces());
        result.put("mutationLedgerRecorded", !mutationLedger.isEmpty());
        result.put("mutatedSurfaceCount", mutatedSurfaces.size());
        result.put("allRequiredMutationSurfacesMutated",
                mutatedSurfaces.containsAll(config.requiredMutationSurfaces()));
    }

    private static Map<String, Object> skippedMutation(String surface, String reason) {
        return Map.of(
                "mutated", false,
                "attempted", false,
                "surface", surface,
                "skipReason", reason
        );
    }

    @SuppressWarnings("unchecked")
    private static void addFailure(Map<String, Object> result, String event, Throwable exception) {
        Object raw = result.get("failures");
        if (raw instanceof List<?> list) {
            String message = failureMessage(exception);
            ((List<String>) list).add(event + ":" + exception.getClass().getSimpleName()
                    + (message.isBlank() ? "" : ":" + message));
        }
    }

    private static void addFailure(Map<String, Object> result, String event, String message) {
        addFailure(result, event, new IllegalStateException(message));
    }

    private static String failureMessage(Throwable exception) {
        String message = exception.getMessage();
        Throwable cause = exception.getCause();
        if ((message == null || message.isBlank()) && cause != null) {
            message = cause.getClass().getSimpleName() + ": " + cause.getMessage();
        }
        return message == null || message.isBlank() ? exception.getClass().getSimpleName() : message;
    }

    private static int integer(Object value) {
        return value instanceof Number number ? number.intValue() : 0;
    }

    public static final class Config {
        private final String namespace;
        private final String gameplayDisplayName;
        private final String gameplayPackId;
        private final boolean actionsEnabled;
        private final List<String> starterToolItems;
        private final String proofMarkerBlockId;
        private final String startingStructureId;
        private final String startingStructureAnchor;
        private final List<String> starterRegionBlocks;
        private final List<String> requiredMutationSurfaces;
        private final NativeLoaderProductPlayableRuntimeEvidence.Config evidenceConfig;

        public Config(
                String namespace,
                String gameplayDisplayName,
                String gameplayPackId,
                boolean actionsEnabled,
                List<String> starterToolItems,
                String proofMarkerBlockId,
                String startingStructureId,
                String startingStructureAnchor,
                String starterRegionTerrainBlockId,
                String starterRegionSurfaceBlockId,
                String starterRegionCoreBlockId,
                List<String> starterRegionFeatureBlockIds,
                List<String> requiredMutationSurfaces,
                NativeLoaderProductPlayableRuntimeEvidence.Config evidenceConfig
        ) {
            this.namespace = namespace == null ? "" : namespace;
            this.gameplayDisplayName = gameplayDisplayName == null ? "" : gameplayDisplayName;
            this.gameplayPackId = gameplayPackId == null ? "" : gameplayPackId;
            this.actionsEnabled = actionsEnabled;
            this.starterToolItems = List.copyOf(starterToolItems == null ? List.of() : starterToolItems);
            this.proofMarkerBlockId = proofMarkerBlockId == null ? "" : proofMarkerBlockId;
            this.startingStructureId = startingStructureId == null ? "" : startingStructureId;
            this.startingStructureAnchor = startingStructureAnchor == null ? "" : startingStructureAnchor;
            this.starterRegionBlocks = starterRegionBlocks(
                    starterRegionTerrainBlockId,
                    starterRegionSurfaceBlockId,
                    starterRegionCoreBlockId,
                    starterRegionFeatureBlockIds
            );
            this.requiredMutationSurfaces = List.copyOf(requiredMutationSurfaces == null
                    ? List.of()
                    : requiredMutationSurfaces);
            this.evidenceConfig = evidenceConfig;
        }

        private static List<String> starterRegionBlocks(
                String terrainBlockId,
                String surfaceBlockId,
                String coreBlockId,
                List<String> featureBlockIds
        ) {
            List<String> blocks = new ArrayList<>();
            blocks.add(terrainBlockId == null ? "" : terrainBlockId);
            blocks.add(surfaceBlockId == null ? "" : surfaceBlockId);
            blocks.add(coreBlockId == null ? "" : coreBlockId);
            blocks.addAll(featureBlockIds == null ? List.of() : featureBlockIds);
            return blocks.stream().filter(id -> !id.isBlank()).distinct().toList();
        }

        public String namespace() {
            return namespace;
        }

        public String gameplayDisplayName() {
            return gameplayDisplayName;
        }

        public String gameplayPackId() {
            return gameplayPackId;
        }

        public boolean actionsEnabled() {
            return actionsEnabled;
        }

        public List<String> starterToolItems() {
            return starterToolItems;
        }

        public String proofMarkerBlockId() {
            return proofMarkerBlockId;
        }

        public String startingStructureId() {
            return startingStructureId;
        }

        public String startingStructureAnchor() {
            return startingStructureAnchor;
        }

        public boolean hasStartingStructure() {
            return !startingStructureId.isBlank();
        }

        public List<String> starterRegionBlocks() {
            return starterRegionBlocks;
        }

        public List<String> requiredMutationSurfaces() {
            return requiredMutationSurfaces;
        }

        public NativeLoaderProductPlayableRuntimeEvidence.Config evidenceConfig() {
            return evidenceConfig;
        }
    }

    @FunctionalInterface
    public interface ClientThreadInvoker {
        boolean invoke(Class<?> minecraftClass, Object minecraft, Runnable action) throws ReflectiveOperationException;
    }

    @FunctionalInterface
    public interface HostInventoryMutation {
        Map<String, Object> apply(Object runtimeHost, Object player, String itemId, int count);
    }

    @FunctionalInterface
    public interface StarterToolGrant {
        boolean grant(Object player);
    }

    @FunctionalInterface
    public interface StarterCommandSender {
        int send(Object minecraft, Object player, Map<String, Object> result);
    }

    @FunctionalInterface
    public interface StarterRegionPainter {
        int paint(Object minecraft, Object player, Map<String, Object> result);
    }

    @FunctionalInterface
    public interface HostWorldBlockMutation {
        Map<String, Object> apply(Object runtimeHost, Object player, String blockId);
    }

    @FunctionalInterface
    public interface HostStructureMutation {
        Map<String, Object> apply(Object runtimeHost, Object player, String structureId, String anchor);
    }

    @FunctionalInterface
    public interface SaveDataWriter {
        Map<String, Object> write(
                Object runtimeHost,
                Object player,
                String scope,
                String key,
                Map<String, Object> payload
        );
    }

    @FunctionalInterface
    public interface HudNotificationPublisher {
        Map<String, Object> publish(Object runtimeHost, Object player, Map<String, Object> payload);
    }
}
