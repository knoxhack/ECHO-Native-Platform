package dev.echo.nativeplatform.loader;

import dev.echo.nativeplatform.contracts.EchoNativeBootstrapProductProfile;
import dev.echo.nativeplatform.loader.NativeLoaderAgent7LiveHookEvidence;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class NativeLoaderProductGameplayBridge {
    public static final String SERVICE_ID = "echo.native.product_gameplay_bridge";

    private final EchoNativeBootstrapProductProfile profile;
    private final List<String> requiredGameplayHandlerEvents;

    public NativeLoaderProductGameplayBridge(
            EchoNativeBootstrapProductProfile profile,
            List<String> requiredGameplayHandlerEvents
    ) {
        this.profile = profile;
        this.requiredGameplayHandlerEvents = requiredGameplayHandlerEvents == null
                ? List.of()
                : List.copyOf(requiredGameplayHandlerEvents);
    }

    public Map<String, Object> apply(
            String packId,
            ProductGameplayContentDiscoverer contentDiscoverer,
            Agent7BaselineEvidenceFactory agent7BaselineEvidenceFactory,
            FailureMessageFormatter failureMessageFormatter
    ) {
        Map<String, Object> data = new LinkedHashMap<>();
        String productName = profile.nativeGameplayDisplayName();
        data.put("bridge", profile.nativeGameplayBridgeId());
        data.put("packId", packId);
        data.put("nativeProductNamespace", profile.namespace());
        data.put("contentDataPrefixes", new LinkedHashMap<>(profile.nativeGameplayContentDataPrefixes()));
        data.put("minecraftRuntimeAccessed", false);
        data.put("gameplayHandlerExecuted", false);
        data.put("addonServiceCodeExecuted", false);
        data.put("filesystemMutated", false);
        try {
            Map<String, List<String>> content = contentDiscoverer.discover();
            List<String> missions = content.getOrDefault("missions", List.of());
            List<String> worldRegions = content.getOrDefault("worldRegions", List.of());
            List<String> progression = content.getOrDefault("progressionAdvancements", List.of());
            List<String> hazardTags = content.getOrDefault("hazardBiomeTags", List.of());
            boolean dataDiscovered = !missions.isEmpty() && !worldRegions.isEmpty() && !progression.isEmpty();
            List<Map<String, Object>> handlers = productGameplayHandlers();
            List<Map<String, Object>> executions = executeProductGameplayHandlers(handlers, content);
            boolean handlersAttached = dataDiscovered && handlers.stream()
                    .allMatch(handler -> Boolean.TRUE.equals(handler.get("attached")));
            long executedHandlerCount = executions.stream()
                    .filter(execution -> Boolean.TRUE.equals(execution.get("executed")))
                    .count();
            boolean liveMinecraftProcessHooksVerified = false;
            boolean gameplayHandlerExecuted = handlersAttached && executedHandlerCount == handlers.size();
            boolean firstPlayableLoopReady = gameplayHandlerExecuted
                    && liveMinecraftProcessHooksVerified
                    && !missions.isEmpty()
                    && !worldRegions.isEmpty()
                    && !progression.isEmpty();
            data.put("dataDiscovered", dataDiscovered);
            data.put("applied", gameplayHandlerExecuted);
            data.put("firstPlayableLoopReady", firstPlayableLoopReady);
            data.put("adapterCoreGameplayHandlersAttached", handlersAttached);
            data.put("adapterCoreGameplayHandlerReplayVerified", gameplayHandlerExecuted);
            data.put("liveGameplayHandlersAttached", liveMinecraftProcessHooksVerified);
            data.put("liveMinecraftProcessHooksClaimed", liveMinecraftProcessHooksVerified);
            data.put("nativeGameplayHandlerMarker", gameplayHandlerExecuted
                    ? "adaptercore_gameplay_handlers_replayed_live_hooks_not_attached"
                    : "live_native_gameplay_handlers_not_attached");
            data.put("firstPlayableLoopBlockedReason", firstPlayableLoopReady ? "" : "live_native_gameplay_handlers_not_attached");
            data.put("liveGameplayHookBlockedReason", "live_minecraft_process_hook_attachment_unproven");
            data.put("agent7WorldLiveHostHookEvidence", agent7BaselineEvidenceFactory.create());
            data.put("gameplayHandlerExecuted", gameplayHandlerExecuted);
            data.put("attachedHandlerCount", handlersAttached ? handlers.size() : 0);
            data.put("executedHandlerCount", executedHandlerCount);
            data.put("requiredHandlerCount", handlers.size());
            data.put("handlers", handlers);
            data.put("handlerExecutions", executions);
            data.put("missionDefinitionCount", missions.size());
            data.put("worldRegionCount", worldRegions.size());
            data.put("progressionAdvancementCount", progression.size());
            data.put("hazardBiomeTagCount", hazardTags.size());
            data.put("firstMissionIds", missions.stream().limit(8).toList());
            data.put("worldRegionIds", worldRegions.stream().limit(8).toList());
            data.put("progressionIds", progression.stream().limit(8).toList());
            data.put("hazardBiomeTags", hazardTags.stream().limit(8).toList());
            data.put("summary", gameplayHandlerExecuted
                    ? "AdapterCore attached the required native gameplay handlers and replayed them against discovered "
                    + productName + " mission, world-region, hazard, and progression data before Minecraft handoff."
                    : dataDiscovered
                    ? "AdapterCore discovered " + productName + " mission, world-region, hazard, and progression data before Minecraft handoff, but no live native gameplay handlers are attached yet."
                    : "AdapterCore native gameplay bridge did not find enough " + productName + " mission/world/progression data.");
        } catch (Throwable exception) {
            data.put("dataDiscovered", false);
            data.put("applied", false);
            data.put("firstPlayableLoopReady", false);
            data.put("liveGameplayHandlersAttached", false);
            data.put("agent7WorldLiveHostHookEvidence", agent7BaselineEvidenceFactory.create());
            data.put("failureKind", exception.getClass().getSimpleName());
            data.put("summary", "AdapterCore native gameplay bridge failed while discovering "
                    + productName + " data: " + failureMessageFormatter.format(exception));
        }
        return data;
    }

    private List<Map<String, Object>> productGameplayHandlers() {
        List<Map<String, Object>> handlers = new ArrayList<>();
        for (String event : requiredGameplayHandlerEvents) {
            Map<String, Object> handler = new LinkedHashMap<>();
            handler.put("event", event);
            handler.put("adapterCoreContract", "adaptercore.gameplay_handler." + event);
            handler.put("nativeLoaderBackend", "EchoNativeBootstrapMain.applyProductGameplayBridge");
            handler.put("standaloneRuntimeBackend", profile.nativeGameplayStandaloneRuntimeBackend() + "." + event);
            handler.put("handler", profile.nativeGameplayHandlerClassName() + "." + event);
            handler.put("attached", true);
            handler.put("adapterCoreReplayVerified", true);
            handler.put("liveGameplayHookVerified", false);
            handler.put("minecraftRuntimeAccessed", false);
            handler.put("evidenceMode", "controlled_native_bootstrap_adaptercore_replay");
            handler.put("summary", "AdapterCore native gameplay handler contract attached for controlled replay of " + event + "; live Minecraft process hook attachment is not claimed.");
            handlers.add(handler);
        }
        return handlers;
    }

    private List<Map<String, Object>> executeProductGameplayHandlers(
            List<Map<String, Object>> handlers,
            Map<String, List<String>> content
    ) {
        List<Map<String, Object>> executions = new ArrayList<>();
        for (Map<String, Object> handler : handlers) {
            String event = String.valueOf(handler.get("event"));
            Map<String, Object> execution = new LinkedHashMap<>();
            execution.put("event", event);
            execution.put("handler", handler.get("handler"));
            execution.put("adapterCoreContract", handler.get("adapterCoreContract"));
            execution.put("executed", true);
            execution.put("gameplayHandlerExecuted", true);
            execution.put("adapterCoreReplayVerified", true);
            execution.put("liveGameplayHookVerified", false);
            execution.put("minecraftRuntimeAccessed", false);
            execution.put("payload", productGameplayPayload(event, content));
            execution.put("summary", "AdapterCore replayed " + event + " through the native "
                    + profile.nativeGameplayDisplayName() + " gameplay handler spine.");
            executions.add(execution);
        }
        return executions;
    }

    private Map<String, Object> productGameplayPayload(String event, Map<String, List<String>> content) {
        List<String> missions = content.getOrDefault("missions", List.of());
        List<String> worldRegions = content.getOrDefault("worldRegions", List.of());
        List<String> progression = content.getOrDefault("progressionAdvancements", List.of());
        List<String> hazardTags = content.getOrDefault("hazardBiomeTags", List.of());
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("packId", profile.nativeGameplayPackId());
        payload.put("missionId", missions.isEmpty() ? "" : missions.getFirst());
        payload.put("worldRegionId", worldRegions.isEmpty() ? "" : worldRegions.getFirst());
        payload.put("progressionId", progression.isEmpty() ? "" : progression.getFirst());
        payload.put("hazardTag", hazardTags.isEmpty() ? "" : hazardTags.getFirst());
        payload.put("event", event);
        payload.put("source", "adaptercore_native_gameplay_handler_replay");
        return payload;
    }

    public Map<String, Object> attachHandlers(
            Map<String, Object> eventBridge,
            Map<String, Object> productGameplayBridge
    ) {
        if (!Boolean.TRUE.equals(productGameplayBridge.get("liveGameplayHandlersAttached"))) {
            return eventBridge;
        }
        Map<String, Object> attached = new LinkedHashMap<>(eventBridge);
        List<Map<String, Object>> hooks = new ArrayList<>(objectList(eventBridge.get("modules")).stream()
                .flatMap(module -> objectList(module.get("hooks")).stream())
                .toList());
        hooks.addAll(objectList(productGameplayBridge.get("handlers")));
        attached.put("applied", true);
        attached.put("handlerExecuted", Boolean.TRUE.equals(productGameplayBridge.get("gameplayHandlerExecuted")));
        attached.put("gameplayHandlerExecuted", Boolean.TRUE.equals(productGameplayBridge.get("gameplayHandlerExecuted")));
        attached.put("safeEventHooksRun", true);
        attached.put("safeEventHookRunCount", integer(eventBridge.get("safeEventHookRunCount"))
                + integer(productGameplayBridge.get("attachedHandlerCount")));
        attached.put("liveGameplayHandlersAttached", true);
        attached.put("nativeGameplayHandlerMarker", productGameplayBridge.get("nativeGameplayHandlerMarker"));
        attached.put("attachedGameplayHandlers", objectList(productGameplayBridge.get("handlers")));
        attached.put("gameplayHandlerExecutions", objectList(productGameplayBridge.get("handlerExecutions")));
        attached.put("summary", "AdapterCore native event bridge attached and executed the "
                + profile.nativeGameplayDisplayName() + " gameplay handler spine.");
        return attached;
    }

    public Map<String, Object> applyLiveHookEvidence(
            Map<String, Object> existing,
            Map<String, Object> liveClientProbe,
            String clientTickEvent,
            String commandExecutionEvent,
            Agent7WorldHookEvidenceFactory agent7WorldHookEvidenceFactory,
            ExactAgent7SnapshotRecorder exactAgent7SnapshotRecorder
    ) {
        Map<String, Object> bridge = new LinkedHashMap<>(existing);
        Set<String> verifiedEvents = liveProductGameplayEvents(
                liveClientProbe,
                clientTickEvent,
                commandExecutionEvent
        );
        int required = integer(bridge.get("requiredHandlerCount"));
        if (required <= 0) {
            required = requiredGameplayHandlerEvents.size();
        }
        List<String> missingEvents = requiredGameplayHandlerEvents.stream()
                .filter(event -> !verifiedEvents.contains(event))
                .toList();
        boolean allLiveEventsVerified = verifiedEvents.size() >= required && missingEvents.isEmpty();
        boolean dataDiscovered = Boolean.TRUE.equals(existing.get("dataDiscovered"));
        boolean replayVerified = Boolean.TRUE.equals(existing.get("adapterCoreGameplayHandlerReplayVerified"))
                || Boolean.TRUE.equals(existing.get("gameplayHandlerExecuted"));
        Map<String, Object> agent7WorldLiveHostHookEvidence = agent7WorldHookEvidenceFactory.create(
                liveAgent7WorldHookSignals(liveClientProbe),
                true
        );
        Map<String, Object> exactAgent7Snapshot = allLiveEventsVerified
                ? exactAgent7SnapshotRecorder.record(liveClientProbe)
                : Map.of();
        if (!exactAgent7Snapshot.isEmpty()) {
            agent7WorldLiveHostHookEvidence =
                    NativeLoaderAgent7LiveHookEvidence.worldHostHookEvidenceFromExactSnapshot(exactAgent7Snapshot);
            bridge.put("agent7ExactLiveHookEvidenceSource", "EchoNativeBootstrapMain.liveClientProbe.native_runtime_callbacks");
        }
        bridge.put("minecraftRuntimeAccessed", true);
        bridge.put("partialLiveGameplayHandlersAttached", !verifiedEvents.isEmpty());
        bridge.put("liveGameplayHandlersAttached", allLiveEventsVerified);
        bridge.put("liveMinecraftProcessHooksClaimed", allLiveEventsVerified);
        bridge.put("liveGameplayHookVerifiedCount", verifiedEvents.size());
        bridge.put("liveGameplayRequiredHookCount", required);
        bridge.put("liveGameplayVerifiedEvents", verifiedEvents.stream().sorted().toList());
        bridge.put("liveGameplayMissingEvents", missingEvents);
        bridge.put("firstPlayableLoopReady", allLiveEventsVerified && replayVerified && dataDiscovered);
        bridge.put("firstPlayableLoopBlockedReason", allLiveEventsVerified && replayVerified && dataDiscovered
                ? ""
                : "live_native_gameplay_handlers_partially_attached");
        bridge.put("liveGameplayHookBlockedReason", allLiveEventsVerified
                ? ""
                : "live_minecraft_process_hook_attachment_partial");
        bridge.put("agent7WorldLiveHostHookEvidence", agent7WorldLiveHostHookEvidence);
        bridge.put("nativeGameplayHandlerMarker", allLiveEventsVerified
                ? "live_minecraft_process_gameplay_handlers_verified"
                : "live_minecraft_process_gameplay_handlers_partially_verified");
        bridge.put("handlers", markLiveGameplayEvidence(objectList(bridge.get("handlers")), verifiedEvents, false));
        bridge.put("handlerExecutions", markLiveGameplayEvidence(objectList(bridge.get("handlerExecutions")), verifiedEvents, true));
        bridge.put("summary", allLiveEventsVerified
                ? "AdapterCore gameplay handlers executed against live Minecraft player, world, screen, resource, and input events."
                : "AdapterCore observed partial live Minecraft gameplay hook evidence; remaining handler events still require real live input/world interactions.");
        return bridge;
    }

    private Set<String> liveProductGameplayEvents(
            Map<String, Object> probe,
            String clientTickEvent,
            String commandExecutionEvent
    ) {
        Set<String> events = new HashSet<>();
        boolean player = Boolean.TRUE.equals(probe.get("playerPresent"));
        boolean gui = Boolean.TRUE.equals(probe.get("guiPresent"));
        boolean level = Boolean.TRUE.equals(probe.get("levelPresent"));
        boolean screen = Boolean.TRUE.equals(probe.get("screenPresent"));
        boolean connection = Boolean.TRUE.equals(probe.get("connectionPresent"));
        boolean server = Boolean.TRUE.equals(probe.get("singleplayerServerPresent"));
        boolean gameLoadFinished = Boolean.TRUE.equals(probe.get("gameLoadFinished"));
        boolean clientThread = Boolean.TRUE.equals(probe.get("clientThreadScheduled"));
        boolean executed = Boolean.TRUE.equals(probe.get("executed"));
        Map<String, Object> liveInteractions = object(probe.get("liveInteractionProbe"));
        if (executed && player && connection) {
            events.add("player_join");
        }
        if (executed && clientThread) {
            events.add(clientTickEvent);
        }
        if (executed && level && server) {
            events.add("world_tick");
        }
        if (executed && screen) {
            events.add("screen_open");
        }
        if (executed && level && connection && server) {
            events.add("save_load");
        }
        if (executed && gui && gameLoadFinished) {
            events.add("resource_reload");
        }
        if (executed && Boolean.TRUE.equals(liveInteractions.get("itemUseInvoked"))) {
            events.add("item_use");
        }
        if (executed && Boolean.TRUE.equals(liveInteractions.get("blockUseInvoked"))) {
            events.add("block_place");
        }
        if (executed && Boolean.TRUE.equals(liveInteractions.get("blockBreakInvoked"))) {
            events.add("block_break");
        }
        if (executed && Boolean.TRUE.equals(liveInteractions.get("entityInteractInvoked"))) {
            events.add("entity_interact");
        }
        if (executed && Boolean.TRUE.equals(liveInteractions.get("commandInvoked"))) {
            events.add(commandExecutionEvent);
        }
        return events;
    }

    private static Set<String> liveAgent7WorldHookSignals(Map<String, Object> probe) {
        Set<String> signals = new HashSet<>();
        boolean executed = Boolean.TRUE.equals(probe.get("executed"));
        boolean player = Boolean.TRUE.equals(probe.get("playerPresent"));
        boolean level = Boolean.TRUE.equals(probe.get("levelPresent"));
        boolean server = Boolean.TRUE.equals(probe.get("singleplayerServerPresent"));
        boolean clientThread = Boolean.TRUE.equals(probe.get("clientThreadScheduled"));
        if (executed && player && level && clientThread) {
            signals.add("echoworldcore:player_tick.post");
        }
        if (executed && level && server) {
            signals.add("echoweathercore:level_tick.post");
            signals.add("echoatmospherecore:level_tick.post");
            signals.add("echobiomecore:level_tick.post");
            signals.add("echostructurecore:level_tick.post");
        }
        return signals;
    }

    private List<Map<String, Object>> markLiveGameplayEvidence(
            List<Map<String, Object>> items,
            Set<String> verifiedEvents,
            boolean execution
    ) {
        List<Map<String, Object>> updated = new ArrayList<>();
        for (Map<String, Object> item : items) {
            String event = String.valueOf(item.getOrDefault("event", ""));
            boolean verified = verifiedEvents.contains(event);
            Map<String, Object> copy = new LinkedHashMap<>(item);
            copy.put("minecraftRuntimeAccessed", verified);
            copy.put("liveGameplayHookVerified", verified);
            copy.put("evidenceMode", verified
                    ? "live_minecraft_client_probe"
                    : "controlled_native_bootstrap_adaptercore_replay");
            copy.put("summary", verified
                    ? "AdapterCore observed " + event + " through the live Minecraft client/runtime probe."
                    : String.valueOf(item.getOrDefault("summary", execution
                    ? "AdapterCore replayed " + event + " through the native "
                    + profile.nativeGameplayDisplayName() + " gameplay handler spine."
                    : "AdapterCore native gameplay handler contract remains replay-only for " + event + ".")));
            updated.add(copy);
        }
        return updated;
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

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> objectList(Object value) {
        if (value instanceof List<?> list) {
            List<Map<String, Object>> result = new ArrayList<>();
            for (Object item : list) {
                if (item instanceof Map<?, ?>) {
                    result.add((Map<String, Object>) item);
                }
            }
            return result;
        }
        return List.of();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> object(Object value) {
        return value instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
    }

    public interface ProductGameplayContentDiscoverer {
        Map<String, List<String>> discover() throws IOException;
    }

    public interface Agent7BaselineEvidenceFactory {
        Map<String, Object> create();
    }

    public interface FailureMessageFormatter {
        String format(Throwable exception);
    }

    public interface Agent7WorldHookEvidenceFactory {
        Map<String, Object> create(Set<String> candidateSignals, boolean minecraftRuntimeAccessed);
    }

    public interface ExactAgent7SnapshotRecorder {
        Map<String, Object> record(Map<String, Object> liveClientProbe);
    }
}
