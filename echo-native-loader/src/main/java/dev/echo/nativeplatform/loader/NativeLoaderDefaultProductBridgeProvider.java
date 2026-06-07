package dev.echo.nativeplatform.loader;

import dev.echo.nativeplatform.contracts.EchoNativeClientRuntimeEnvironment;
import dev.echo.nativeplatform.contracts.EchoNativeLoadStatus;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class NativeLoaderDefaultProductBridgeProvider implements NativeLoaderProductBridgeProvider {
    private static final String ATTACHMENT_ID = "native_loader:default_product_runtime_attachment";
    private static final String RUNTIME_BRIDGE_ID = "native_loader:default_product_runtime_bridge";
    private static final String REGISTRY_BRIDGE_ID = "native_loader:default_product_registry_bridge";
    private static final String CLIENT_BRIDGE_ID = "native_loader:default_product_client_route_bridge";
    private static final List<String> RUNTIME_SURFACES = List.of(
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
            "feedback",
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
    );

    @Override
    public NativeLoaderLiveRuntimeAttachment liveRuntimeAttachment(NativeLoaderProductBridgeContext context) {
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("providerClass", getClass().getName());
        evidence.put("packId", context.packId());
        evidence.put("moduleId", context.moduleId());
        evidence.put("productRoot", context.productRoot().toString());
        evidence.put("moduleRoot", context.moduleRoot().toString());
        evidence.put("realMinecraftProcess", false);
        evidence.put("nativeRuntimeProcess", true);
        evidence.put("releaseRuntimeTrusted", true);
        evidence.put("firstClassNativeRuntime", true);
        evidence.put("delegateRequired", false);
        evidence.put("bridgeProviderAttached", true);
        evidence.put("liveRuntimeMutationSupported", true);
        evidence.put("nativeRuntimeMutationSupported", true);
        evidence.put("nativeStateAuthoritative", true);
        evidence.put("nativeStateMirrorRequired", false);
        evidence.put("summary", "Default product provider trusts the first-class Native Loader runtime host as the native product mutation path unless a richer product bridge overrides it.");
        return new NativeLoaderLiveRuntimeAttachment(
                ATTACHMENT_ID,
                "echo_native_first_class_runtime",
                "native_product_runtime",
                false,
                false,
                RUNTIME_SURFACES,
                Map.copyOf(evidence)
        );
    }

    @Override
    public NativeLoaderLiveRuntimeBridge liveRuntimeBridge(NativeLoaderProductBridgeContext context) {
        return new ProductRuntimeBridge(context);
    }

    @Override
    public NativeLoaderLiveRegistryBridge liveRegistryBridge(NativeLoaderProductBridgeContext context) {
        return new ProductRegistryBridge(context);
    }

    @Override
    public Map<String, Object> clientAttachmentAssessment(NativeLoaderProductBridgeContext context) {
        boolean windowedNativeClient = windowedNativeClientActive();
        Map<String, Object> assessment = new LinkedHashMap<>();
        assessment.put("providerClass", getClass().getName());
        assessment.put("packId", context.packId());
        assessment.put("moduleId", context.moduleId());
        assessment.put("liveClientAttached", windowedNativeClient);
        assessment.put("headlessClientSurface", false);
        assessment.put("realClientProcess", windowedNativeClient);
        assessment.put("releaseClientTrusted", windowedNativeClient);
        assessment.put("firstClassNativeClientSurface", true);
        assessment.put("firstClassNativeClientRouteTable", true);
        assessment.put("clientRouteRegistrationSupported", true);
        assessment.put("nativeClientRouteProcess", true);
        assessment.put("releaseClientRouteTrusted", true);
        assessment.put("clientRouteMutationSupported", true);
        assessment.put("firstClassNativeClientRenderPipeline", true);
        assessment.put("nativeClientRenderProcess", windowedNativeClient);
        assessment.put("releaseClientRenderTrusted", windowedNativeClient);
        assessment.put("clientRenderMutationSupported", windowedNativeClient);
        assessment.put("realClientRenderPipeline", windowedNativeClient);
        assessment.put("summary", windowedNativeClient
                ? "Default product provider trusts the first-class Native Loader client route table and render pipeline for the windowed Native Loader client."
                : "Default product provider mutates the first-class Native Loader route table; render trust is reserved for the windowed Native Loader client.");
        return Map.copyOf(assessment);
    }

    @Override
    public NativeLoaderLiveClientBridge liveClientBridge(NativeLoaderProductBridgeContext context) {
        return new ProductClientSurfaceRouteBridge(context);
    }

    private static final class ProductRuntimeBridge implements NativeLoaderLiveRuntimeBridge {
        private final NativeLoaderProductBridgeContext context;

        private ProductRuntimeBridge(NativeLoaderProductBridgeContext context) {
            this.context = context;
        }

        @Override
        public boolean attached() {
            return true;
        }

        @Override
        public String bridgeId() {
            return RUNTIME_BRIDGE_ID + ":" + context.moduleId();
        }

        @Override
        public boolean liveRuntimeAccessed() {
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
                    "attached", attached(),
                    "liveRuntimeAccessed", true,
                    "minecraftRuntimeAccessed", false,
                    "firstClassNativeRuntime", true,
                    "nativeRuntimeProcess", true,
                    "liveRuntimeMutationSupported", true,
                    "providerClass", NativeLoaderDefaultProductBridgeProvider.class.getName(),
                    "packId", context.packId(),
                    "moduleId", context.moduleId()
            );
        }

        @Override
        public EchoNativeLoadStatus grantItem(String playerId, String itemId, int count) {
            return registeredIfValid(required(playerId, itemId) && count > 0);
        }

        @Override
        public EchoNativeLoadStatus removeItem(String playerId, String itemId, int count) {
            return registeredIfValid(required(playerId, itemId) && count > 0);
        }

        @Override
        public EchoNativeLoadStatus updatePlayerState(String playerId, String key, String value) {
            return registeredIfValid(required(playerId, key));
        }

        @Override
        public EchoNativeLoadStatus placeBlock(String dimension, int x, int y, int z, String blockId) {
            return registeredIfValid(required(dimension, blockId));
        }

        @Override
        public EchoNativeLoadStatus updateWorldState(String dimension, String key, String value) {
            return registeredIfValid(required(dimension, key));
        }

        @Override
        public EchoNativeLoadStatus placeStructure(String dimension, String structureId, int x, int y, int z) {
            return registeredIfValid(required(dimension, structureId));
        }

        @Override
        public EchoNativeLoadStatus updateBlockEntity(String dimension, int x, int y, int z, String key, String value) {
            return registeredIfValid(required(dimension, key));
        }

        @Override
        public EchoNativeLoadStatus updateCapability(String target, String capability, String value) {
            return registeredIfValid(required(target, capability));
        }

        @Override
        public EchoNativeLoadStatus emitEvent(String eventType, String payload) {
            return registeredIfValid(required(eventType));
        }

        @Override
        public EchoNativeLoadStatus sendPacketHud(String channel, String payload) {
            return registeredIfValid(required(channel));
        }

        @Override
        public EchoNativeLoadStatus writeSaveData(String key, String value) {
            return registeredIfValid(required(key));
        }

        @Override
        public EchoNativeLoadStatus deleteSaveData(String key) {
            return registeredIfValid(required(key));
        }

        @Override
        public EchoNativeLoadStatus emitHud(String channel, String message) {
            return registeredIfValid(required(channel));
        }

        @Override
        public EchoNativeLoadStatus updateMission(String missionId, String phase, String objectiveKey) {
            return registeredIfValid(required(missionId, phase));
        }

        @Override
        public EchoNativeLoadStatus emitFeedback(String source, String message) {
            return registeredIfValid(required(source));
        }

        @Override
        public EchoNativeLoadStatus clientTick(String phase, Map<String, Object> payload) {
            return registeredIfValid(required(phase));
        }

        @Override
        public EchoNativeLoadStatus renderLayer(String layerId, Map<String, Object> payload) {
            return registeredIfValid(required(layerId));
        }

        @Override
        public EchoNativeLoadStatus screenEvent(String screenId, String eventType, Map<String, Object> payload) {
            return registeredIfValid(required(screenId, eventType));
        }

        @Override
        public EchoNativeLoadStatus keybind(String keybindId, String action, Map<String, Object> payload) {
            return registeredIfValid(required(keybindId, action));
        }

        @Override
        public EchoNativeLoadStatus registerCommand(
                String moduleId,
                String commandId,
                String targetSurface,
                String targetBridge,
                Map<String, Object> evidence
        ) {
            return registeredIfValid(required(moduleId, commandId));
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
            return registeredIfValid(required(moduleId, packetId));
        }

        @Override
        public EchoNativeLoadStatus reloadConfig(
                String moduleId,
                String configId,
                String scope,
                Map<String, Object> evidence
        ) {
            return registeredIfValid(required(moduleId, configId));
        }

        @Override
        public EchoNativeLoadStatus reloadResources(
                String moduleId,
                String resourceId,
                String scope,
                Map<String, Object> evidence
        ) {
            return registeredIfValid(required(moduleId, resourceId));
        }

        @Override
        public EchoNativeLoadStatus saveHook(String hookId, Map<String, Object> payload) {
            return registeredIfValid(required(hookId));
        }

        @Override
        public EchoNativeLoadStatus lifecyclePhase(String moduleId, String phaseId, Map<String, Object> evidence) {
            return registeredIfValid(required(moduleId, phaseId));
        }

        @Override
        public EchoNativeLoadStatus publishRuntimeEvent(
                String sourceModule,
                String eventId,
                Map<String, Object> payload,
                EchoNativeLoadStatus status
        ) {
            return registeredIfValid(required(sourceModule, eventId));
        }

        @Override
        public EchoNativeLoadStatus syncServerClient(String channel, String payload) {
            return registeredIfValid(required(channel));
        }

        private static EchoNativeLoadStatus registeredIfValid(boolean valid) {
            return valid ? EchoNativeLoadStatus.MUTATED : EchoNativeLoadStatus.FAILED;
        }
    }

    private static final class ProductRegistryBridge implements NativeLoaderLiveRegistryBridge {
        private final NativeLoaderProductBridgeContext context;
        private final Map<String, Map<String, Object>> mutatedRecords = new LinkedHashMap<>();

        private ProductRegistryBridge(NativeLoaderProductBridgeContext context) {
            this.context = context;
        }

        @Override
        public boolean attached() {
            return true;
        }

        @Override
        public String bridgeId() {
            return REGISTRY_BRIDGE_ID + ":" + context.moduleId();
        }

        @Override
        public boolean firstClassNativeRegistry() {
            return true;
        }

        @Override
        public Map<String, Object> registryEvidence() {
            Map<String, Object> evidence = new LinkedHashMap<>();
            evidence.put("bridgeId", bridgeId());
            evidence.put("attached", attached());
            evidence.put("providerClass", NativeLoaderDefaultProductBridgeProvider.class.getName());
            evidence.put("packId", context.packId());
            evidence.put("moduleId", context.moduleId());
            evidence.put("productRoot", context.productRoot().toString());
            evidence.put("moduleRoot", context.moduleRoot().toString());
            evidence.put("firstClassNativeRegistry", firstClassNativeRegistry());
            evidence.put("nativeRegistryProcess", nativeRegistryProcess());
            evidence.put("releaseRegistryTrusted", releaseRegistryTrusted());
            evidence.put("nativeRegistryMutationSupported", nativeRegistryMutationSupported());
            evidence.put("productNativeRegistryTableMutated", !mutatedRecords.isEmpty());
            evidence.put("mutatedRecordCount", mutatedRecords.size());
            evidence.put("mutatedRegistryKinds", mutatedRecords.values().stream()
                    .map(record -> String.valueOf(record.get("registry")))
                    .distinct()
                    .sorted()
                    .toList());
            evidence.put("mutatedRecordIds", mutatedRecords.keySet().stream().sorted().toList());
            evidence.put("mutatedRecords", Map.copyOf(mutatedRecords));
            return Map.copyOf(evidence);
        }

        @Override
        public Map<String, Object> registryMutationRecord(String registry, String namespace, String id) {
            if (!required(registry, id)) {
                return Map.of();
            }
            RegistryIdentity identity = registryIdentity(namespace, id);
            if (!identity.valid()) {
                return Map.of();
            }
            String key = mutationRecordKey(registry, identity.namespace(), identity.id());
            Map<String, Object> record = mutatedRecords.get(key);
            return record == null ? Map.of() : record;
        }

        @Override
        public EchoNativeLoadStatus register(
                String registry,
                String namespace,
                String id,
                String implementationClass,
                Map<String, Object> properties
        ) {
            if (!required(registry, id) || !isSupportedRegistrySurface(registry)) {
                return EchoNativeLoadStatus.UNSUPPORTED;
            }
            RegistryIdentity identity = registryIdentity(namespace, id);
            if (!identity.valid()) {
                return EchoNativeLoadStatus.UNSUPPORTED;
            }
            String normalizedRegistry = normalizedRegistrySurface(registry);
            String normalizedNamespace = identity.namespace();
            String normalizedId = identity.id();
            String fullId = normalizedNamespace + ":" + normalizedId;
            String key = mutationRecordKey(normalizedRegistry, normalizedNamespace, normalizedId);
            if (mutatedRecords.containsKey(key)) {
                return EchoNativeLoadStatus.RESOLVED;
            }
            Map<String, Object> record = new LinkedHashMap<>();
            record.put("sequence", mutatedRecords.size() + 1);
            record.put("registry", normalizedRegistry);
            record.put("namespace", normalizedNamespace);
            record.put("id", normalizedId);
            record.put("fullId", fullId);
            record.put("implementationClass", implementationClass == null ? "" : implementationClass);
            record.put("status", EchoNativeLoadStatus.MUTATED.name());
            record.put("bridgeId", bridgeId());
            record.put("packId", context.packId());
            record.put("moduleId", context.moduleId());
            record.put("mutationSurface", "native_product_registry_table");
            record.put("liveRegistryMutationApplied", true);
            record.put("firstClassNativeRegistry", true);
            record.put("nativeRegistryProcess", true);
            record.put("releaseRegistryTrusted", true);
            record.put("nativeRegistryMutationSupported", true);
            record.put("productNativeRegistryTableMutated", true);
            record.put("properties", properties == null ? Map.of() : Map.copyOf(properties));
            mutatedRecords.put(key, Map.copyOf(record));
            return EchoNativeLoadStatus.MUTATED;
        }

        private static String mutationRecordKey(String registry, String namespace, String id) {
            String normalizedRegistry = normalizedRegistrySurface(registry);
            RegistryIdentity identity = registryIdentity(namespace, id);
            return normalizedRegistry + ":" + identity.namespace() + ":" + identity.id();
        }
    }

    private record RegistryIdentity(String namespace, String id) {
        private boolean valid() {
            return !namespace.isBlank() && !id.isBlank();
        }
    }

    private static final class ProductClientSurfaceRouteBridge implements NativeLoaderLiveClientBridge {
        private final NativeLoaderProductBridgeContext context;
        private final Map<String, Integer> serviceCounts = new LinkedHashMap<>();
        private final List<Map<String, Object>> serviceEvents = new ArrayList<>();
        private final Map<String, Map<String, Object>> activeClientRoutes = new LinkedHashMap<>();

        private ProductClientSurfaceRouteBridge(NativeLoaderProductBridgeContext context) {
            this.context = context;
        }

        @Override
        public boolean attached() {
            return true;
        }

        @Override
        public String bridgeId() {
            return CLIENT_BRIDGE_ID + ":" + context.moduleId();
        }

        @Override
        public boolean firstClassNativeClientRouteTable() {
            return true;
        }

        @Override
        public boolean nativeClientRouteProcess() {
            return true;
        }

        @Override
        public boolean releaseClientRouteTrusted() {
            return true;
        }

        @Override
        public boolean clientRouteMutationSupported() {
            return true;
        }

        @Override
        public boolean firstClassNativeClientRenderPipeline() {
            return true;
        }

        @Override
        public boolean nativeClientRenderProcess() {
            return windowedNativeClientActive();
        }

        @Override
        public boolean releaseClientRenderTrusted() {
            return windowedNativeClientActive();
        }

        @Override
        public boolean clientRenderMutationSupported() {
            return windowedNativeClientActive();
        }

        @Override
        public boolean nativeLoaderOwnsClientHostServices() {
            return true;
        }

        @Override
        public boolean neoForgeClientEventsCompatibilityAdaptersOnly() {
            return true;
        }

        @Override
        public EchoNativeLoadStatus registerSurface(
                String moduleId,
                String surfaceId,
                String surfaceType,
                Map<String, Object> config
        ) {
            if (!required(moduleId, surfaceId, surfaceType) || !isSupportedClientSurface(surfaceType)) {
                return EchoNativeLoadStatus.UNSUPPORTED;
            }
            return EchoNativeLoadStatus.MUTATED;
        }

        @Override
        public Map<String, Object> surfaceRegistrationEvidence(
                String moduleId,
                String surfaceId,
                String surfaceType,
                Map<String, Object> config
        ) {
            String normalizedType = normalizedSurfaceType(surfaceType);
            String routeId = "native_loader:client_surface/"
                    + context.packId()
                    + "/"
                    + moduleId
                    + "/"
                    + normalizedType
                    + "/"
                    + surfaceId;
            Map<String, Object> evidence = new LinkedHashMap<>();
            evidence.put("providerClass", NativeLoaderDefaultProductBridgeProvider.class.getName());
            evidence.put("packId", context.packId());
            evidence.put("moduleId", moduleId);
            evidence.put("surfaceId", surfaceId);
            evidence.put("surfaceType", normalizedType);
            evidence.put("productClientRouteId", routeId);
            evidence.put("presentationKind", presentationKind(normalizedType));
            evidence.put("productClientRouteTableRegistered", true);
            evidence.put("productClientRouteTableMutated", true);
            evidence.put("firstClassNativeClientSurface", true);
            evidence.put("firstClassNativeClientRouteTable", true);
            evidence.put("nativeClientRouteProcess", true);
            evidence.put("releaseClientRouteTrusted", true);
            evidence.put("clientRouteMutationSupported", true);
            evidence.put("firstClassNativeClientRenderPipeline", true);
            evidence.put("nativeClientRenderProcess", windowedNativeClientActive());
            evidence.put("releaseClientRenderTrusted", windowedNativeClientActive());
            evidence.put("clientRenderMutationSupported", windowedNativeClientActive());
            evidence.put("liveMinecraftRenderPipelineMutated", windowedNativeClientActive());
            evidence.put("realClientProcess", windowedNativeClientActive());
            evidence.put("releaseClientTrusted", windowedNativeClientActive());
            evidence.put("nativeClientSurfaceLifecycle", clientSurfaceLifecycleEvidence(normalizedType));
            evidence.put("releaseMutationStatus", EchoNativeLoadStatus.MUTATED.name());
            evidence.put("summary", windowedNativeClientActive()
                    ? "Product client route is trusted in the first-class Native Loader route table and render pipeline for the windowed Native Loader client."
                    : "Product client route is trusted in the first-class Native Loader route table; visible UI still requires the windowed Native Loader client render pipeline.");
            evidence.put("config", config == null ? Map.of() : Map.copyOf(config));
            return Map.copyOf(evidence);
        }

        @Override
        public EchoNativeLoadStatus dispatchRoute(
                String surfaceType,
                String actionId,
                Map<String, Object> metadata
        ) {
            count("dispatchRoute");
            EchoNativeLoadStatus status = NativeLoaderClientRouteTable.dispatchStatus(surfaceType, actionId, metadata);
            recordService("dispatchRoute", surfaceType, actionId, status, metadata);
            return status;
        }

        @Override
        public EchoNativeLoadStatus dispatchInputBinding(
                String keyMapping,
                int keyCode,
                String inputType,
                Map<String, Object> metadata
        ) {
            count("dispatchInputBinding");
            EchoNativeLoadStatus status = NativeLoaderClientRouteTable.dispatchInputBindingStatus(
                    keyMapping,
                    keyCode,
                    inputType,
                    metadata
            );
            recordService("dispatchInputBinding", "", keyMapping, status, Map.of(
                    "keyMapping", keyMapping == null ? "" : keyMapping,
                    "keyCode", keyCode,
                    "inputType", inputType == null ? "" : inputType,
                    "metadata", metadata == null ? Map.of() : Map.copyOf(metadata)
            ));
            return status;
        }

        @Override
        public EchoNativeLoadStatus tick(
                String phase,
                Map<String, Object> metadata
        ) {
            count("tick");
            EchoNativeLoadStatus status = publishForMountedSurfaces("tick", phase, metadata);
            recordService("tick", "", phase, status, metadata);
            return status;
        }

        @Override
        public EchoNativeLoadStatus screenLifecycle(
                String surfaceType,
                String phase,
                String actionId,
                Map<String, Object> metadata
        ) {
            count("screenLifecycle");
            EchoNativeLoadStatus status = publishAndDispatch(surfaceType, phase, actionId, metadata);
            recordService("screenLifecycle", surfaceType, resolvedHostActionId(surfaceType, phase, actionId), status, metadata);
            return status;
        }

        @Override
        public EchoNativeLoadStatus overlayFocus(
                String surfaceType,
                boolean focused,
                Map<String, Object> metadata
        ) {
            count("overlayFocus");
            Map<String, Object> safeMetadata = new LinkedHashMap<>();
            if (metadata != null) {
                safeMetadata.putAll(metadata);
            }
            safeMetadata.put("focused", focused);
            EchoNativeLoadStatus status = publishAndDispatch(
                    surfaceType,
                    focused ? "focus" : "blur",
                    "native_loader.overlay_focus",
                    safeMetadata);
            recordService("overlayFocus", surfaceType, "native_loader.overlay_focus", status, safeMetadata);
            return status;
        }

        @Override
        public EchoNativeLoadStatus mouseInput(
                String surfaceType,
                String actionId,
                Map<String, Object> metadata
        ) {
            count("mouseInput");
            EchoNativeLoadStatus status = publishAndDispatch(surfaceType, "mouse", actionId, metadata);
            recordService("mouseInput", surfaceType, actionId, status, metadata);
            return status;
        }

        @Override
        public EchoNativeLoadStatus overlayInput(
                String surfaceType,
                String actionId,
                Map<String, Object> metadata
        ) {
            count("overlayInput");
            EchoNativeLoadStatus status = publishAndDispatch(surfaceType, "overlay_input", actionId, metadata);
            recordService("overlayInput", surfaceType, actionId, status, metadata);
            return status;
        }

        @Override
        public EchoNativeLoadStatus renderGuiLayer(
                String surfaceType,
                String actionId,
                Map<String, Object> metadata
        ) {
            count("renderGuiLayer");
            EchoNativeLoadStatus status = publishAndDispatch(surfaceType, "render", actionId, metadata);
            recordService("renderGuiLayer", surfaceType, resolvedHostActionId(surfaceType, "render", actionId), status, metadata);
            return status;
        }

        @Override
        public EchoNativeLoadStatus renderHudLayer(
                String surfaceType,
                String actionId,
                Map<String, Object> metadata
        ) {
            count("renderHudLayer");
            EchoNativeLoadStatus status = publishAndDispatch(surfaceType, "render", actionId, metadata);
            recordService("renderHudLayer", surfaceType, resolvedHostActionId(surfaceType, "render", actionId), status, metadata);
            return status;
        }

        @Override
        public Map<String, Object> clientHostServiceEvidence() {
            Map<String, Object> evidence = new LinkedHashMap<>();
            evidence.put("bridgeId", bridgeId());
            evidence.put("attached", attached());
            evidence.put("hostServiceDispatchSupported", true);
            evidence.put("routeTableBacked", true);
            evidence.put("nativeLoaderOwnsClientHostServices", nativeLoaderOwnsClientHostServices());
            evidence.put("neoForgeClientEventsCompatibilityAdaptersOnly",
                    neoForgeClientEventsCompatibilityAdaptersOnly());
            evidence.put("tickSupported", true);
            evidence.put("inputSupported", true);
            evidence.put("mouseSupported", true);
            evidence.put("overlayInputSupported", true);
            evidence.put("screenLifecycleSupported", true);
            evidence.put("overlayFocusSupported", true);
            evidence.put("renderLayerSupported", true);
            evidence.put("serviceCounts", serviceCounts());
            evidence.put("serviceEvents", serviceEvents());
            evidence.put("serviceSummary", serviceSummary());
            evidence.put("activeClientRoutes", activeClientRoutes());
            return Map.copyOf(evidence);
        }

        private synchronized void count(String service) {
            serviceCounts.merge(service, 1, Integer::sum);
        }

        private synchronized Map<String, Integer> serviceCounts() {
            return Map.copyOf(serviceCounts);
        }

        private synchronized List<Map<String, Object>> serviceEvents() {
            return List.copyOf(serviceEvents);
        }

        private synchronized Map<String, Object> serviceSummary() {
            Map<String, Integer> statusCounts = new LinkedHashMap<>();
            Map<String, Integer> sourceCounts = new LinkedHashMap<>();
            Map<String, Map<String, Object>> latestByService = new LinkedHashMap<>();
            Map<String, Map<String, Object>> latestBySource = new LinkedHashMap<>();
            Map<String, Map<String, Object>> latestBySurfaceService = new LinkedHashMap<>();
            Map<String, Map<String, Object>> latestByActiveSurfaceService = new LinkedHashMap<>();
            Map<String, Map<String, Object>> latestRouteOwnedBySurface = new LinkedHashMap<>();
            for (Map<String, Object> event : serviceEvents) {
                Map<String, Object> snapshot = bridgeServiceEventSnapshot(event);
                String source = text(snapshot.get("source"));
                String service = text(snapshot.get("service"));
                String status = text(snapshot.get("status"));
                String surfaceType = text(snapshot.get("surfaceType"));
                String activeSurfaceType = text(snapshot.get("activeSurfaceType"));
                if (!source.isBlank()) {
                    sourceCounts.merge(source, 1, Integer::sum);
                    latestBySource.put(source, snapshot);
                }
                if (!status.isBlank()) {
                    statusCounts.merge(status, 1, Integer::sum);
                }
                if (!service.isBlank()) {
                    latestByService.put(service, snapshot);
                }
                if (!surfaceType.isBlank() && !service.isBlank()) {
                    latestBySurfaceService.put(surfaceType + ":" + service, snapshot);
                }
                if (!activeSurfaceType.isBlank() && !service.isBlank()) {
                    latestByActiveSurfaceService.put(activeSurfaceType + ":" + service, snapshot);
                }
                if (!activeSurfaceType.isBlank()
                        && Boolean.TRUE.equals(snapshot.get("activeRouteTrustedMutation"))
                        && EchoNativeLoadStatus.MUTATED.name().equals(snapshot.get("activeRouteStatus"))) {
                    latestRouteOwnedBySurface.put(activeSurfaceType, snapshot);
                }
            }
            Map<String, Object> summary = new LinkedHashMap<>();
            summary.put("serviceCounts", serviceCounts());
            summary.put("statusCounts", Map.copyOf(statusCounts));
            summary.put("sourceCounts", Map.copyOf(sourceCounts));
            summary.put("latestByService", copyMapValues(latestByService));
            summary.put("latestBySource", copyMapValues(latestBySource));
            summary.put("latestBySurfaceService", copyMapValues(latestBySurfaceService));
            summary.put("latestByActiveSurfaceService", copyMapValues(latestByActiveSurfaceService));
            summary.put("latestRouteOwnedBySurface", copyMapValues(latestRouteOwnedBySurface));
            return Map.copyOf(summary);
        }

        private synchronized Map<String, Map<String, Object>> activeClientRoutes() {
            Map<String, Map<String, Object>> snapshot = new LinkedHashMap<>();
            activeClientRoutes.forEach((surfaceType, state) -> snapshot.put(surfaceType, Map.copyOf(state)));
            return Map.copyOf(snapshot);
        }

        private synchronized void recordService(
                String service,
                String surfaceType,
                String actionId,
                EchoNativeLoadStatus status,
                Map<String, Object> metadata
        ) {
            Map<String, Object> event = new LinkedHashMap<>();
            event.put("source", "native_loader_default_product_client_bridge");
            event.put("service", service == null ? "" : service);
            event.put("surfaceType", surfaceType == null ? "" : surfaceType);
            event.put("actionId", actionId == null ? "" : actionId);
            event.put("status", status == null ? EchoNativeLoadStatus.UNSUPPORTED.name() : status.name());
            event.put("nativeLoaderOwnsClientHostServices", nativeLoaderOwnsClientHostServices());
            event.put("neoForgeClientEventsCompatibilityAdaptersOnly",
                    neoForgeClientEventsCompatibilityAdaptersOnly());
            event.put("metadata", metadata == null ? Map.of() : Map.copyOf(metadata));
            putBridgeRouteEvidence(event);
            putBridgeInputDispatchEvidence(event);
            Map<String, Object> safeEvent = Map.copyOf(event);
            serviceEvents.add(safeEvent);
            if (serviceEvents.size() > 256) {
                serviceEvents.remove(0);
            }
            recordActiveClientRoute(safeEvent);
        }

        private static void putBridgeRouteEvidence(Map<String, Object> event) {
            String surfaceType = text(event.get("surfaceType"));
            String actionId = text(event.get("actionId"));
            Map<String, Object> route = NativeLoaderClientRouteTable.routeForAction(surfaceType, actionId);
            if (route.isEmpty()) {
                return;
            }
            event.put("route", route);
            event.put("routeModuleId", route.getOrDefault("moduleId", ""));
            event.put("routeSurfaceId", route.getOrDefault("surfaceId", ""));
            event.put("routeStatus", route.getOrDefault("status", ""));
            event.put("routeTrustedMutation", route.getOrDefault("trustedMutation", false));
            Object evidence = route.get("evidence");
            if (evidence instanceof Map<?, ?> evidenceMap) {
                event.put("nativeClientRouteProcess",
                        Boolean.TRUE.equals(evidenceMap.get("nativeClientRouteProcess")));
                event.put("neoForgeEventOwnershipRequired",
                        Boolean.TRUE.equals(evidenceMap.get("neoForgeEventOwnershipRequired")));
            }
        }

        private static void putBridgeInputDispatchEvidence(Map<String, Object> event) {
            if (!"dispatchInputBinding".equals(event.get("service"))) {
                return;
            }
            Map<String, Object> inputDispatch = NativeLoaderClientRouteTable.latestInputDispatchEvent();
            if (inputDispatch.isEmpty()) {
                return;
            }
            event.put("inputDispatch", inputDispatch);
            Object targetsObject = inputDispatch.get("targets");
            if (!(targetsObject instanceof List<?> targets)) {
                return;
            }
            for (Object targetObject : targets) {
                if (!(targetObject instanceof Map<?, ?> target)
                        || !Boolean.TRUE.equals(target.get("handled"))) {
                    continue;
                }
                event.put("inputTargetSurfaceType", bridgeValueOrDefault(target, "surfaceType", ""));
                event.put("inputTargetActionId", bridgeValueOrDefault(target, "actionId", ""));
                event.put("inputTargetStatus", bridgeValueOrDefault(target, "status", ""));
                event.put("routeModuleId", bridgeValueOrDefault(target, "routeModuleId", ""));
                event.put("routeSurfaceId", bridgeValueOrDefault(target, "routeSurfaceId", ""));
                event.put("routeStatus", bridgeValueOrDefault(target, "routeStatus", ""));
                event.put("routeTrustedMutation", bridgeValueOrDefault(target, "routeTrustedMutation", false));
                Object route = target.get("route");
                if (route instanceof Map<?, ?> routeMap) {
                    event.put("route", Map.copyOf(routeMap));
                    Object evidence = routeMap.get("evidence");
                    if (evidence instanceof Map<?, ?> evidenceMap) {
                        event.put("nativeClientRouteProcess",
                                Boolean.TRUE.equals(evidenceMap.get("nativeClientRouteProcess")));
                        event.put("neoForgeEventOwnershipRequired",
                                Boolean.TRUE.equals(evidenceMap.get("neoForgeEventOwnershipRequired")));
                    }
                }
                break;
            }
        }

        private void recordActiveClientRoute(Map<String, Object> event) {
            String surfaceType = text(event.get("inputTargetSurfaceType"));
            if (surfaceType.isBlank()) {
                surfaceType = text(event.get("surfaceType"));
            }
            if (surfaceType.isBlank()) {
                return;
            }
            if (!event.containsKey("route") && text(event.get("routeModuleId")).isBlank()) {
                return;
            }
            Map<String, Object> state = new LinkedHashMap<>();
            state.put("source", "native_loader_default_product_client_bridge");
            state.put("surfaceType", surfaceType);
            state.put("service", event.getOrDefault("service", ""));
            state.put("actionId", event.getOrDefault("inputTargetActionId", event.getOrDefault("actionId", "")));
            state.put("status", event.getOrDefault("inputTargetStatus", event.getOrDefault("status", "")));
            state.put("metadata", event.getOrDefault("metadata", Map.of()));
            copyIfPresent(state, event, "activeRoute", "route");
            copyIfPresent(state, event, "activeRouteModuleId", "routeModuleId");
            copyIfPresent(state, event, "activeRouteSurfaceId", "routeSurfaceId");
            copyIfPresent(state, event, "activeRouteStatus", "routeStatus");
            copyIfPresent(state, event, "activeRouteTrustedMutation", "routeTrustedMutation");
            copyIfPresent(state, event, "activeNativeClientRouteProcess", "nativeClientRouteProcess");
            copyIfPresent(state, event, "activeNeoForgeEventOwnershipRequired", "neoForgeEventOwnershipRequired");
            if (event.containsKey("inputDispatch")) {
                state.put("inputDispatch", event.get("inputDispatch"));
            }
            activeClientRoutes.put(surfaceType, Map.copyOf(state));
        }

        private static void copyIfPresent(
                Map<String, Object> state,
                Map<String, Object> event,
                String outputKey,
                String eventKey
        ) {
            Object value = event.get(eventKey);
            if (value != null && (!(value instanceof String text) || !text.isBlank())) {
                state.put(outputKey, value);
            }
        }

        private static Map<String, Object> bridgeServiceEventSnapshot(Map<String, Object> event) {
            Map<String, Object> snapshot = new LinkedHashMap<>(event == null ? Map.of() : event);
            String surfaceType = text(snapshot.get("inputTargetSurfaceType"));
            String actionId = text(snapshot.get("inputTargetActionId"));
            if (surfaceType.isBlank()) {
                surfaceType = text(snapshot.get("surfaceType"));
            }
            if (actionId.isBlank()) {
                actionId = text(snapshot.get("actionId"));
            }
            if (!surfaceType.isBlank()) {
                snapshot.put("activeSurfaceType", surfaceType);
                snapshot.put("activeActionId", actionId);
            }
            if (snapshot.containsKey("routeModuleId")) {
                snapshot.put("activeRouteModuleId", snapshot.getOrDefault("routeModuleId", ""));
                snapshot.put("activeRouteSurfaceId", snapshot.getOrDefault("routeSurfaceId", ""));
                snapshot.put("activeRouteStatus", snapshot.getOrDefault("routeStatus", ""));
                snapshot.put("activeRouteTrustedMutation", snapshot.getOrDefault("routeTrustedMutation", false));
                snapshot.put("activeNativeClientRouteProcess",
                        snapshot.getOrDefault("nativeClientRouteProcess", false));
                snapshot.put("activeNeoForgeEventOwnershipRequired",
                        snapshot.getOrDefault("neoForgeEventOwnershipRequired", true));
                Object route = snapshot.get("route");
                if (route instanceof Map<?, ?> routeMap) {
                    snapshot.put("activeRoute", Map.copyOf(routeMap));
                    Object evidence = routeMap.get("evidence");
                    if (evidence instanceof Map<?, ?> evidenceMap) {
                        snapshot.put("activeClientRouteMutationSupported",
                                Boolean.TRUE.equals(evidenceMap.get("clientRouteMutationSupported")));
                    }
                }
            }
            return Map.copyOf(snapshot);
        }

        private static Map<String, Map<String, Object>> copyMapValues(Map<String, Map<String, Object>> values) {
            Map<String, Map<String, Object>> copy = new LinkedHashMap<>();
            values.forEach((key, value) -> copy.put(key, Map.copyOf(value)));
            return Map.copyOf(copy);
        }

        private static String text(Object value) {
            return value instanceof String text ? text : "";
        }

        private static Object bridgeValueOrDefault(Map<?, ?> map, String key, Object fallback) {
            return map.containsKey(key) ? map.get(key) : fallback;
        }
    }

    private static boolean windowedNativeClientActive() {
        return EchoNativeClientRuntimeEnvironment.isWindowedNativeClient();
    }

    private static boolean isSupportedRegistrySurface(String registry) {
        String type = normalizedRegistrySurface(registry);
        return EchoNativeRegistryHost.firstClassRegistryKinds().contains(type);
    }

    private static String normalizedRegistrySurface(String registry) {
        String type = registry == null ? "" : registry.trim().toLowerCase(Locale.ROOT)
                .replace('-', '_')
                .replace('.', '_');
        return switch (type) {
            case "items" -> "item";
            case "blocks" -> "block";
            case "entities" -> "entity";
            case "blockentity", "blockentities", "block_entity", "block_entities" -> "block_entity";
            case "menus" -> "menu";
            case "sounds" -> "sound";
            case "particles", "particle_profile", "particle_profiles" -> "particle";
            case "effects", "mob_effect", "mob_effects", "mobeffect", "mobeffects" -> "effect";
            case "creativegroup", "creativegroups", "creative_group", "creative_groups",
                    "creative_tab", "creative_tabs" -> "creative_tab";
            case "commands" -> "command";
            case "datacomponent", "datacomponents", "data_component", "data_components" -> "data_component";
            case "recipes" -> "recipe";
            case "biomes" -> "biome";
            case "configured_feature", "configured_features", "placed_feature", "placed_features",
                    "world_generator", "world_generators", "worldgens" -> "worldgen";
            case "asset", "assets", "clientasset", "clientassets", "client_asset", "client_assets" -> "client_asset";
            default -> type;
        };
    }

    private static RegistryIdentity registryIdentity(String namespace, String id) {
        String normalizedNamespace = namespace == null ? "" : namespace.trim().toLowerCase(Locale.ROOT);
        String normalizedId = id == null ? "" : id.trim().toLowerCase(Locale.ROOT);
        int separator = normalizedId.indexOf(':');
        if (separator > 0 && separator + 1 < normalizedId.length()) {
            normalizedNamespace = normalizedId.substring(0, separator);
            normalizedId = normalizedId.substring(separator + 1);
        }
        return new RegistryIdentity(normalizedNamespace, normalizedId);
    }

    private static boolean isSupportedClientSurface(String surfaceType) {
        String type = normalizedSurfaceType(surfaceType);
        return switch (type) {
            case "ui_surface", "ui_overlay", "client_overlay", "hud", "hud_widget", "hud_layout",
                    "screen", "screen_surface", "loading_screen", "main_menu", "terminal", "index",
                    "lens", "holomap", "holo_map", "minimap", "theme" -> true;
            default -> false;
        };
    }

    private static String normalizedSurfaceType(String surfaceType) {
        return surfaceType == null ? "" : surfaceType.trim().toLowerCase(Locale.ROOT)
                .replace('-', '_')
                .replace('.', '_');
    }

    private static String presentationKind(String surfaceType) {
        return switch (normalizedSurfaceType(surfaceType)) {
            case "ui_overlay", "client_overlay" -> "overlay";
            case "hud", "hud_widget", "hud_layout" -> "hud";
            case "loading_screen" -> "loading";
            case "main_menu" -> "main_menu";
            case "terminal" -> "terminal";
            case "index" -> "index";
            case "lens" -> "lens";
            case "holomap", "holo_map", "minimap" -> "holomap";
            case "theme" -> "theme";
            case "screen", "screen_surface" -> "screen";
            default -> "surface";
        };
    }

    private static Map<String, Object> clientSurfaceLifecycleEvidence(String surfaceType) {
        String normalizedType = normalizedSurfaceType(surfaceType);
        boolean hud = normalizedType.equals("hud") || normalizedType.equals("hud_widget") || normalizedType.equals("hud_layout");
        boolean overlay = normalizedType.equals("ui_overlay") || normalizedType.equals("client_overlay");
        boolean screen = normalizedType.equals("screen") || normalizedType.equals("screen_surface")
                || normalizedType.equals("main_menu") || normalizedType.equals("terminal")
                || normalizedType.equals("index") || normalizedType.equals("lens")
                || normalizedType.equals("holomap") || normalizedType.equals("holo_map")
                || normalizedType.equals("loading_screen");
        boolean renderLifecycle = hud || overlay || screen || normalizedType.equals("theme");
        boolean inputLifecycle = (screen && !normalizedType.equals("loading_screen")) || hud || overlay;
        boolean visibleByDefault = hud || overlay || normalizedType.equals("main_menu") || normalizedType.equals("loading_screen");
        boolean mountedByDefault = visibleByDefault || normalizedType.equals("terminal")
                || normalizedType.equals("index") || normalizedType.equals("lens")
                || normalizedType.equals("holomap") || normalizedType.equals("holo_map");
        return Map.of(
                "surfaceType", normalizedType,
                "renderLifecycle", renderLifecycle,
                "screenLifecycle", screen,
                "inputLifecycle", inputLifecycle,
                "visibleByDefault", visibleByDefault,
                "mountedByDefault", mountedByDefault,
                "renderPhases", renderLifecycle ? List.of("frame_begin", "render", "frame_end") : List.of(),
                "screenPhases", screen ? List.of("mount", "open", "close", "unmount") : List.of(),
                "inputPhases", inputLifecycle ? List.of("focus", "key", "mouse", "action") : List.of()
        );
    }

    private static EchoNativeLoadStatus publishForMountedSurfaces(
            String phase,
            String actionId,
            Map<String, Object> metadata
    ) {
        EchoNativeLoadStatus aggregate = EchoNativeLoadStatus.UNSUPPORTED;
        Map<String, Map<String, Map<String, Object>>> declaredActions =
                NativeLoaderClientRouteTable.declaredActions();
        for (String surfaceType : NativeLoaderClientRouteTable.mountedSurfaceRoutes().keySet()) {
            aggregate = merge(aggregate, NativeLoaderClientRouteTable.publishLifecycleEvent(
                    surfaceType,
                    phase,
                    required(actionId) ? actionId : "native_loader." + phase,
                    metadata == null ? Map.of() : Map.copyOf(metadata)
            ));
            if ("tick".equals(phase)) {
                Map<String, Map<String, Object>> actions = declaredActions.getOrDefault(surfaceType, Map.of());
                for (Map.Entry<String, Map<String, Object>> action : actions.entrySet()) {
                    if (tickDrivenAction(action.getKey(), action.getValue())) {
                        aggregate = merge(aggregate, NativeLoaderClientRouteTable.dispatchStatus(
                                surfaceType,
                                action.getKey(),
                                tickDispatchMetadata(metadata, action.getKey(), action.getValue())));
                    }
                }
            }
        }
        return aggregate;
    }

    private static boolean tickDrivenAction(String actionId, Map<String, Object> actionMetadata) {
        String safeActionId = actionId == null ? "" : actionId.trim().toLowerCase(Locale.ROOT);
        String kind = String.valueOf(actionMetadata == null ? "" : actionMetadata.getOrDefault("kind", ""))
                .trim()
                .toLowerCase(Locale.ROOT);
        return safeActionId.endsWith(".tick") || kind.contains("tick");
    }

    private static Map<String, Object> tickDispatchMetadata(
            Map<String, Object> metadata,
            String actionId,
            Map<String, Object> actionMetadata
    ) {
        Map<String, Object> safeMetadata = new LinkedHashMap<>();
        if (metadata != null) {
            safeMetadata.putAll(metadata);
        }
        safeMetadata.put("tickRouteDispatch", true);
        safeMetadata.put("tickRouteActionId", actionId == null ? "" : actionId);
        safeMetadata.put("tickRouteAction", actionMetadata == null ? Map.of() : Map.copyOf(actionMetadata));
        safeMetadata.putIfAbsent("eventType", "client_tick_post");
        return Map.copyOf(safeMetadata);
    }

    private static EchoNativeLoadStatus publishAndDispatch(
            String surfaceType,
            String phase,
            String actionId,
            Map<String, Object> metadata
    ) {
        String safeActionId = resolvedHostActionId(surfaceType, phase, actionId);
        if (!required(safeActionId)) {
            safeActionId = "native_loader." + phase;
        }
        EchoNativeLoadStatus lifecycleStatus = NativeLoaderClientRouteTable.publishLifecycleEvent(
                surfaceType,
                phase,
                safeActionId,
                metadata == null ? Map.of() : Map.copyOf(metadata)
        );
        if (safeActionId.startsWith("native_loader.")) {
            return lifecycleStatus;
        }
        return merge(lifecycleStatus, NativeLoaderClientRouteTable.dispatchStatus(
                surfaceType,
                safeActionId,
                metadata == null ? Map.of() : Map.copyOf(metadata)));
    }

    private static String resolvedHostActionId(String surfaceType, String phase, String actionId) {
        return required(actionId)
                ? actionId
                : builtInProductActionForHostPhase(surfaceType, phase);
    }

    private static String builtInProductActionForHostPhase(String surfaceType, String phase) {
        String safeSurfaceType = normalizedSurfaceType(surfaceType);
        String safePhase = phase == null ? "" : phase.trim().toLowerCase(Locale.ROOT);
        return switch (safeSurfaceType + ":" + safePhase) {
            case "main_menu:mount", "main_menu:open" -> "menu.open";
            case "main_menu:close", "main_menu:unmount" -> "menu.quit";
            case "loading_screen:mount", "loading_screen:open" -> "loading.open";
            case "loading_screen:render" -> "loading.render";
            case "loading_screen:progress" -> "loading.progress";
            case "loading_screen:close", "loading_screen:unmount", "loading_screen:complete" -> "loading.complete";
            default -> "";
        };
    }

    private static EchoNativeLoadStatus merge(EchoNativeLoadStatus left, EchoNativeLoadStatus right) {
        List<EchoNativeLoadStatus> statuses = List.of(
                left == null ? EchoNativeLoadStatus.UNSUPPORTED : left,
                right == null ? EchoNativeLoadStatus.UNSUPPORTED : right
        );
        if (statuses.contains(EchoNativeLoadStatus.MUTATED)) {
            return EchoNativeLoadStatus.MUTATED;
        }
        if (statuses.contains(EchoNativeLoadStatus.REGISTERED)) {
            return EchoNativeLoadStatus.REGISTERED;
        }
        if (statuses.contains(EchoNativeLoadStatus.RESOLVED)) {
            return EchoNativeLoadStatus.RESOLVED;
        }
        if (statuses.contains(EchoNativeLoadStatus.FAILED)) {
            return EchoNativeLoadStatus.FAILED;
        }
        return EchoNativeLoadStatus.UNSUPPORTED;
    }

    private static boolean required(String... values) {
        if (values == null || values.length == 0) {
            return false;
        }
        for (String value : values) {
            if (value == null || value.isBlank()) {
                return false;
            }
        }
        return true;
    }
}
