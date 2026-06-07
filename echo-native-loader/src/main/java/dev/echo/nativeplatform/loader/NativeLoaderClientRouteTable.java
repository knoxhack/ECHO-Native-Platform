package dev.echo.nativeplatform.loader;

import dev.echo.nativeplatform.contracts.EchoNativeLoadStatus;
import dev.echo.nativeplatform.contracts.EchoNativeClientRouteRegistry.NativeClientSurfaceLifecycleEvent;
import dev.echo.nativeplatform.contracts.EchoNativeClientRouteRegistry.NativeClientSurfaceLifecycle;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Process-local native client route table populated from SDK UI surface registrations.
 */
public final class NativeLoaderClientRouteTable {
    private static final Map<String, Map<String, Object>> ROUTES = new LinkedHashMap<>();
    private static final Map<String, String> PRIMARY_ROUTE_BY_TYPE = new LinkedHashMap<>();
    private static final Map<String, List<String>> ROUTES_BY_TYPE = new LinkedHashMap<>();
    private static final Map<String, List<NativeClientRouteActionHandlerEntry>> ACTION_HANDLERS = new LinkedHashMap<>();
    private static final Map<String, Map<String, Map<String, Object>>> ACTIONS_BY_TYPE = new LinkedHashMap<>();
    private static final Map<String, Map<String, String>> ACTION_ROUTE_BY_TYPE = new LinkedHashMap<>();
    private static final Map<String, Map<String, List<Map<String, Object>>>> INPUT_BINDINGS_BY_TYPE = new LinkedHashMap<>();
    private static final List<Map<String, Object>> ACTION_DISPATCH_EVENTS = new ArrayList<>();
    private static final List<Map<String, Object>> INPUT_DISPATCH_EVENTS = new ArrayList<>();
    private static final Map<String, NativeClientSurfaceLifecycle> LIFECYCLES_BY_TYPE = new LinkedHashMap<>();
    private static final Map<String, List<NativeClientSurfaceLifecycleEvent>> LIFECYCLE_EVENTS_BY_TYPE = new LinkedHashMap<>();

    private NativeLoaderClientRouteTable() {
    }

    public static synchronized EchoNativeLoadStatus registerRoute(
            String moduleId,
            String surfaceId,
            String surfaceType,
            Map<String, Object> config,
            Map<String, Object> evidence,
            boolean trustedMutation
    ) {
        if (blank(moduleId) || blank(surfaceId) || blank(surfaceType)) {
            return EchoNativeLoadStatus.FAILED;
        }
        String normalizedType = normalizeSurfaceType(surfaceType);
        String routeKey = routeKey(moduleId, surfaceId);
        Map<String, Object> route = new LinkedHashMap<>();
        route.put("moduleId", moduleId.trim());
        route.put("surfaceId", surfaceId.trim());
        route.put("surfaceType", normalizedType);
        route.put("trustedMutation", trustedMutation);
        route.put("status", trustedMutation ? EchoNativeLoadStatus.MUTATED.name() : EchoNativeLoadStatus.REGISTERED.name());
        route.put("config", config == null ? Map.of() : Map.copyOf(config));
        route.put("evidence", evidence == null ? Map.of() : Map.copyOf(evidence));
        NativeClientSurfaceLifecycle lifecycle = LIFECYCLES_BY_TYPE.getOrDefault(
                normalizedType,
                inferredLifecycle(normalizedType, config, evidence));
        route.put("lifecycle", lifecycle.toEvidence());
        ROUTES.put(routeKey, Map.copyOf(route));
        List<String> routesForType = new ArrayList<>(ROUTES_BY_TYPE.getOrDefault(normalizedType, List.of()));
        routesForType.remove(routeKey);
        routesForType.add(routeKey);
        ROUTES_BY_TYPE.put(normalizedType, List.copyOf(routesForType));
        PRIMARY_ROUTE_BY_TYPE.putIfAbsent(normalizedType, routeKey);
        if (trustedMutation) {
            PRIMARY_ROUTE_BY_TYPE.put(normalizedType, routeKey);
        }
        LIFECYCLES_BY_TYPE.putIfAbsent(normalizedType, lifecycle);
        recordDefaultMountEvent(normalizedType, lifecycle);
        return trustedMutation ? EchoNativeLoadStatus.MUTATED : EchoNativeLoadStatus.REGISTERED;
    }

    public static synchronized boolean hasRoute(String surfaceType) {
        return PRIMARY_ROUTE_BY_TYPE.containsKey(normalizeSurfaceType(surfaceType));
    }

    public static synchronized boolean hasTrustedRoute(String surfaceType) {
        Map<String, Object> route = routeForSurface(surfaceType);
        return Boolean.TRUE.equals(route.get("trustedMutation"));
    }

    public static EchoNativeLoadStatus registerActionHandler(
            String surfaceType,
            NativeClientRouteActionHandler handler
    ) {
        return registerActionHandler(surfaceType, "", handler);
    }

    public static EchoNativeLoadStatus registerActionHandler(
            String surfaceType,
            String handlerId,
            NativeClientRouteActionHandler handler
    ) {
        if (blank(surfaceType) || handler == null) {
            return EchoNativeLoadStatus.FAILED;
        }
        synchronized (NativeLoaderClientRouteTable.class) {
            String normalizedType = normalizeSurfaceType(surfaceType);
            List<NativeClientRouteActionHandlerEntry> handlers = new ArrayList<>(
                    ACTION_HANDLERS.getOrDefault(normalizedType, List.of()));
            String safeHandlerId = handlerId == null ? "" : handlerId.trim();
            if (!safeHandlerId.isBlank()) {
                String stableHandlerId = safeHandlerId;
                handlers.removeIf(entry -> stableHandlerId.equals(entry.handlerId()));
            } else {
                safeHandlerId = "anonymous:" + handlers.size();
            }
            handlers.add(new NativeClientRouteActionHandlerEntry(safeHandlerId, handler));
            ACTION_HANDLERS.put(normalizedType, List.copyOf(handlers));
        }
        return EchoNativeLoadStatus.MUTATED;
    }

    public static EchoNativeLoadStatus registerActions(
            String surfaceType,
            Map<String, Map<String, Object>> actions
    ) {
        return registerActions("", "", surfaceType, actions);
    }

    public static EchoNativeLoadStatus registerActions(
            String moduleId,
            String surfaceId,
            String surfaceType,
            Map<String, Map<String, Object>> actions
    ) {
        if (blank(surfaceType) || actions == null) {
            return EchoNativeLoadStatus.FAILED;
        }
        synchronized (NativeLoaderClientRouteTable.class) {
            String normalizedType = normalizeSurfaceType(surfaceType);
            Map<String, Map<String, Object>> safeActions = copyActions(actions);
            Map<String, Map<String, Object>> mergedActions = new LinkedHashMap<>(
                    ACTIONS_BY_TYPE.getOrDefault(normalizedType, Map.of()));
            mergedActions.putAll(safeActions);
            ACTIONS_BY_TYPE.put(normalizedType, Map.copyOf(mergedActions));
            String ownerRouteKey = routeKeyForActionOwner(moduleId, surfaceId, normalizedType);
            if (!ownerRouteKey.isBlank()) {
                Map<String, String> actionRoutes = new LinkedHashMap<>(
                        ACTION_ROUTE_BY_TYPE.getOrDefault(normalizedType, Map.of()));
                for (String actionId : safeActions.keySet()) {
                    actionRoutes.put(actionId, ownerRouteKey);
                }
                ACTION_ROUTE_BY_TYPE.put(normalizedType, Map.copyOf(actionRoutes));
            }
        }
        return EchoNativeLoadStatus.MUTATED;
    }

    public static synchronized Map<String, Map<String, Map<String, Object>>> declaredActions() {
        Map<String, Map<String, Map<String, Object>>> snapshot = new LinkedHashMap<>();
        for (Map.Entry<String, Map<String, Map<String, Object>>> surfaceEntry : ACTIONS_BY_TYPE.entrySet()) {
            Map<String, Map<String, Object>> actions = new LinkedHashMap<>();
            for (Map.Entry<String, Map<String, Object>> actionEntry : surfaceEntry.getValue().entrySet()) {
                actions.put(actionEntry.getKey(), Map.copyOf(actionEntry.getValue()));
            }
            snapshot.put(surfaceEntry.getKey(), Map.copyOf(actions));
        }
        return Map.copyOf(snapshot);
    }

    public static EchoNativeLoadStatus registerInputBinding(
            String surfaceType,
            String actionId,
            Map<String, Object> binding
    ) {
        if (blank(surfaceType) || blank(actionId) || binding == null || binding.isEmpty()) {
            return EchoNativeLoadStatus.FAILED;
        }
        String normalizedType = normalizeSurfaceType(surfaceType);
        String safeActionId = actionId.trim();
        Map<String, Object> safeBinding = Map.copyOf(binding);
        synchronized (NativeLoaderClientRouteTable.class) {
            Map<String, List<Map<String, Object>>> bindingsByAction = new LinkedHashMap<>(
                    INPUT_BINDINGS_BY_TYPE.getOrDefault(normalizedType, Map.of()));
            List<Map<String, Object>> bindings = new ArrayList<>(
                    bindingsByAction.getOrDefault(safeActionId, List.of()));
            if (!containsBinding(bindings, safeBinding)) {
                bindings.add(safeBinding);
            }
            bindingsByAction.put(safeActionId, List.copyOf(bindings));
            INPUT_BINDINGS_BY_TYPE.put(normalizedType, Map.copyOf(bindingsByAction));
        }
        return EchoNativeLoadStatus.MUTATED;
    }

    public static synchronized Map<String, Map<String, List<Map<String, Object>>>> inputBindings() {
        Map<String, Map<String, List<Map<String, Object>>>> snapshot = new LinkedHashMap<>();
        for (Map.Entry<String, Map<String, List<Map<String, Object>>>> surfaceEntry : INPUT_BINDINGS_BY_TYPE.entrySet()) {
            Map<String, List<Map<String, Object>>> actions = new LinkedHashMap<>();
            for (Map.Entry<String, List<Map<String, Object>>> actionEntry : surfaceEntry.getValue().entrySet()) {
                actions.put(actionEntry.getKey(), List.copyOf(actionEntry.getValue()));
            }
            snapshot.put(surfaceEntry.getKey(), Map.copyOf(actions));
        }
        return Map.copyOf(snapshot);
    }

    public static boolean dispatchInputBinding(
            String keyMapping,
            int keyCode,
            String inputType
    ) {
        return dispatchInputBindingStatus(keyMapping, keyCode, inputType) == EchoNativeLoadStatus.MUTATED;
    }

    public static EchoNativeLoadStatus dispatchInputBindingStatus(
            String keyMapping,
            int keyCode,
            String inputType
    ) {
        return dispatchInputBindingStatus(keyMapping, keyCode, inputType, Map.of());
    }

    public static EchoNativeLoadStatus dispatchInputBindingStatus(
            String keyMapping,
            int keyCode,
            String inputType,
            Map<String, Object> metadata
    ) {
        String safeKeyMapping = keyMapping == null ? "" : keyMapping.trim();
        String safeInputType = inputType == null ? "" : inputType.trim().toLowerCase(Locale.ROOT);
        Map<String, Object> safeInputMetadata = metadata == null ? Map.of() : Map.copyOf(metadata);
        if (safeKeyMapping.isBlank() && keyCode < 0) {
            recordInputDispatch(
                    safeKeyMapping,
                    keyCode,
                    safeInputType,
                    safeInputMetadata,
                    List.of(),
                    EchoNativeLoadStatus.UNSUPPORTED);
            return EchoNativeLoadStatus.UNSUPPORTED;
        }
        List<InputDispatchTarget> targets = new ArrayList<>();
        synchronized (NativeLoaderClientRouteTable.class) {
            for (Map.Entry<String, Map<String, List<Map<String, Object>>>> surfaceEntry : INPUT_BINDINGS_BY_TYPE.entrySet()) {
                for (Map.Entry<String, List<Map<String, Object>>> actionEntry : surfaceEntry.getValue().entrySet()) {
                    for (Map<String, Object> binding : actionEntry.getValue()) {
                        if (bindingMatches(binding, safeKeyMapping, keyCode, safeInputType)) {
                            targets.add(new InputDispatchTarget(
                                    surfaceEntry.getKey(),
                                    actionEntry.getKey(),
                                    binding
                            ));
                        }
                    }
                }
            }
        }
        boolean dispatched = false;
        List<Map<String, Object>> targetEvidence = new ArrayList<>();
        for (InputDispatchTarget target : targets) {
            Map<String, Object> dispatchMetadata = inputDispatchMetadata(
                    safeKeyMapping,
                    keyCode,
                    safeInputType,
                    safeInputMetadata,
                    target.binding());
            EchoNativeLoadStatus targetStatus = dispatchStatus(target.surfaceType(), target.actionId(), dispatchMetadata);
            boolean handled = targetStatus == EchoNativeLoadStatus.MUTATED;
            Map<String, Object> route = routeForAction(target.surfaceType(), target.actionId());
            Map<String, Object> evidence = new LinkedHashMap<>();
            evidence.put("surfaceType", target.surfaceType());
            evidence.put("actionId", target.actionId());
            evidence.put("binding", target.binding());
            evidence.put("metadata", dispatchMetadata);
            evidence.put("status", targetStatus.name());
            evidence.put("handled", handled);
            if (!route.isEmpty()) {
                evidence.put("route", route);
                evidence.put("routeModuleId", route.getOrDefault("moduleId", ""));
                evidence.put("routeSurfaceId", route.getOrDefault("surfaceId", ""));
                evidence.put("routeTrustedMutation", route.getOrDefault("trustedMutation", false));
                evidence.put("routeStatus", route.getOrDefault("status", ""));
            }
            targetEvidence.add(Map.copyOf(evidence));
            if (handled) {
                dispatched = true;
                publishLifecycleEvent(new NativeClientSurfaceLifecycleEvent(
                        target.surfaceType(),
                        "input",
                        target.actionId(),
                        dispatchMetadata
                ));
            }
        }
        EchoNativeLoadStatus status = dispatched ? EchoNativeLoadStatus.MUTATED : EchoNativeLoadStatus.UNSUPPORTED;
        recordInputDispatch(safeKeyMapping, keyCode, safeInputType, safeInputMetadata, targetEvidence, status);
        return status;
    }

    public static synchronized Map<String, Object> inputDispatchEvidence() {
        return Map.of(
                "dispatchCount", INPUT_DISPATCH_EVENTS.size(),
                "events", List.copyOf(INPUT_DISPATCH_EVENTS),
                "summary", inputDispatchSummary()
        );
    }

    public static synchronized Map<String, Object> latestInputDispatchEvent() {
        if (INPUT_DISPATCH_EVENTS.isEmpty()) {
            return Map.of();
        }
        return INPUT_DISPATCH_EVENTS.get(INPUT_DISPATCH_EVENTS.size() - 1);
    }

    public static EchoNativeLoadStatus registerLifecycle(
            String surfaceType,
            NativeClientSurfaceLifecycle lifecycle
    ) {
        if (blank(surfaceType) || lifecycle == null) {
            return EchoNativeLoadStatus.FAILED;
        }
        synchronized (NativeLoaderClientRouteTable.class) {
            String normalizedType = normalizeSurfaceType(surfaceType);
            NativeClientSurfaceLifecycle normalizedLifecycle = normalizedLifecycle(surfaceType, lifecycle);
            LIFECYCLES_BY_TYPE.put(normalizedType, normalizedLifecycle);
            refreshRouteLifecycleEvidence(normalizedType, normalizedLifecycle);
            recordDefaultMountEvent(normalizedType, normalizedLifecycle);
        }
        return EchoNativeLoadStatus.MUTATED;
    }

    public static synchronized NativeClientSurfaceLifecycle lifecycle(String surfaceType) {
        String normalizedType = normalizeSurfaceType(surfaceType);
        return LIFECYCLES_BY_TYPE.getOrDefault(normalizedType, NativeClientSurfaceLifecycle.empty(normalizedType));
    }

    public static synchronized Map<String, NativeClientSurfaceLifecycle> lifecycles() {
        return Map.copyOf(LIFECYCLES_BY_TYPE);
    }

    public static EchoNativeLoadStatus publishLifecycleEvent(NativeClientSurfaceLifecycleEvent event) {
        if (event == null || blank(event.surfaceType()) || blank(event.phase())) {
            return EchoNativeLoadStatus.FAILED;
        }
        String normalizedType = normalizeSurfaceType(event.surfaceType());
        NativeClientSurfaceLifecycleEvent normalizedEvent = new NativeClientSurfaceLifecycleEvent(
                normalizedType,
                event.phase(),
                event.actionId(),
                event.metadata()
        );
        synchronized (NativeLoaderClientRouteTable.class) {
            if (!PRIMARY_ROUTE_BY_TYPE.containsKey(normalizedType)) {
                return EchoNativeLoadStatus.UNSUPPORTED;
            }
            List<NativeClientSurfaceLifecycleEvent> events = new ArrayList<>(
                    LIFECYCLE_EVENTS_BY_TYPE.getOrDefault(normalizedType, List.of()));
            events.add(normalizedEvent);
            LIFECYCLE_EVENTS_BY_TYPE.put(normalizedType, List.copyOf(events));
        }
        return EchoNativeLoadStatus.MUTATED;
    }

    public static synchronized Map<String, List<NativeClientSurfaceLifecycleEvent>> lifecycleEvents() {
        return Map.copyOf(LIFECYCLE_EVENTS_BY_TYPE);
    }

    public static synchronized Map<String, Object> lifecycleEventEvidence() {
        int eventCount = 0;
        for (List<NativeClientSurfaceLifecycleEvent> events : LIFECYCLE_EVENTS_BY_TYPE.values()) {
            eventCount += events.size();
        }
        return Map.of(
                "eventCount", eventCount,
                "eventsBySurface", lifecycleEvents(),
                "summary", lifecycleEventSummary()
        );
    }

    public static synchronized EchoNativeLoadStatus publishLifecycleEvent(
            String surfaceType,
            String phase,
            String actionId,
            Map<String, Object> metadata
    ) {
        return publishLifecycleEvent(new NativeClientSurfaceLifecycleEvent(
                surfaceType,
                phase,
                actionId,
                metadata
        ));
    }

    public static EchoNativeLoadStatus screenLifecycle(
            String surfaceType,
            String phase,
            String actionId,
            Map<String, Object> metadata
    ) {
        String normalizedType = normalizeSurfaceType(surfaceType);
        String safeActionId = actionId == null ? "" : actionId.trim();
        if (blank(safeActionId)) {
            safeActionId = builtInProductActionForHostPhase(normalizedType, phase);
        }
        Map<String, Object> enrichedMetadata = new LinkedHashMap<>(metadata == null ? Map.of() : metadata);
        enrichedMetadata.put("nativeLoaderUiHostService", "screen_lifecycle");
        enrichedMetadata.put("nativeLoaderUiHostSurface", normalizedType);
        enrichedMetadata.put("nativeLoaderUiHostAction", safeActionId);
        enrichedMetadata.put("nativeLoaderScreenLifecycleHandoff", true);
        Map<String, Object> safeMetadata = Map.copyOf(enrichedMetadata);
        if (blank(safeActionId) || safeActionId.startsWith("native_loader.")) {
            return publishLifecycleEvent(surfaceType, phase, safeActionId, safeMetadata);
        }
        EchoNativeLoadStatus dispatchStatus = dispatchStatus(normalizedType, safeActionId, safeMetadata);
        EchoNativeLoadStatus lifecycleStatus = publishLifecycleEvent(surfaceType, phase, safeActionId, safeMetadata);
        return dispatchStatus == EchoNativeLoadStatus.MUTATED ? dispatchStatus : lifecycleStatus;
    }

    public static synchronized Map<String, Map<String, Object>> mountedSurfaceRoutes() {
        return lifecycleRouteSnapshot(true);
    }

    public static synchronized Map<String, Map<String, Object>> visibleSurfaceRoutes() {
        return lifecycleRouteSnapshot(false);
    }

    public static boolean dispatch(String surfaceType, String actionId) {
        return dispatchStatus(surfaceType, actionId) == EchoNativeLoadStatus.MUTATED;
    }

    public static boolean dispatch(String surfaceType, String actionId, Map<String, Object> metadata) {
        return dispatchStatus(surfaceType, actionId, metadata) == EchoNativeLoadStatus.MUTATED;
    }

    public static EchoNativeLoadStatus dispatchStatus(String surfaceType, String actionId) {
        return dispatchStatus(surfaceType, actionId, Map.of());
    }

    public static EchoNativeLoadStatus dispatchStatus(String surfaceType, String actionId, Map<String, Object> metadata) {
        String normalizedType = normalizeSurfaceType(surfaceType);
        String safeActionId = actionId == null ? "" : actionId.trim();
        Map<String, Object> safeMetadata = metadata == null ? Map.of() : Map.copyOf(metadata);
        List<NativeClientRouteActionHandlerEntry> handlers;
        Map<String, Object> route;
        Map<String, Object> action;
        boolean knownAction;
        synchronized (NativeLoaderClientRouteTable.class) {
            handlers = List.copyOf(ACTION_HANDLERS.getOrDefault(normalizedType, List.of()));
            action = actionFor(normalizedType, safeActionId);
            route = routeForAction(normalizedType, safeActionId);
            knownAction = actionKnown(normalizedType, safeActionId);
        }
        if (handlers.isEmpty()) {
            recordActionDispatch(normalizedType, safeActionId, route, action, safeMetadata,
                    EchoNativeLoadStatus.UNSUPPORTED, "no_handlers", 0);
            return EchoNativeLoadStatus.UNSUPPORTED;
        }
        if (!Boolean.TRUE.equals(route.get("trustedMutation"))) {
            recordActionDispatch(normalizedType, safeActionId, route, action, safeMetadata,
                    EchoNativeLoadStatus.UNSUPPORTED, "untrusted_or_missing_route", 0);
            return EchoNativeLoadStatus.UNSUPPORTED;
        }
        if (!knownAction) {
            recordActionDispatch(normalizedType, safeActionId, route, action, safeMetadata,
                    EchoNativeLoadStatus.UNSUPPORTED, "unknown_action", 0);
            return EchoNativeLoadStatus.UNSUPPORTED;
        }
        NativeClientRouteActionContext context = new NativeClientRouteActionContext(
                normalizedType,
                safeActionId,
                route,
                action,
                safeMetadata
        );
        NativeClientRouteHandlerSelection handlerSelection = selectHandlersForRoute(handlers, route);
        for (NativeClientRouteActionHandlerEntry entry : handlerSelection.orderedHandlers()) {
            try {
                if (entry.handler().dispatch(context)) {
                    publishLifecycleEvent(new NativeClientSurfaceLifecycleEvent(
                            normalizedType,
                            "action",
                            safeActionId,
                            Map.of(
                                    "source", "native_loader_route_dispatch",
                                    "action", action,
                                    "dispatchMetadata", safeMetadata,
                                    "route", route
                        )
                    ));
                    recordActionDispatch(normalizedType, safeActionId, route, action, safeMetadata,
                            EchoNativeLoadStatus.MUTATED, "handled", 0, entry.handlerId(), handlerSelection);
                    return EchoNativeLoadStatus.MUTATED;
                }
            } catch (RuntimeException | LinkageError exception) {
                // Keep trying module-owned handlers for shared surfaces such as client_overlay and hud.
                recordActionDispatch(normalizedType, safeActionId, route, action, safeMetadata,
                        EchoNativeLoadStatus.RESOLVED, "handler_exception", 1, entry.handlerId(), handlerSelection);
            }
        }
        recordActionDispatch(normalizedType, safeActionId, route, action, safeMetadata,
                EchoNativeLoadStatus.UNSUPPORTED, "unhandled", 0, "", handlerSelection);
        return EchoNativeLoadStatus.UNSUPPORTED;
    }

    public static synchronized Map<String, Object> routeForSurface(String surfaceType) {
        String key = PRIMARY_ROUTE_BY_TYPE.get(normalizeSurfaceType(surfaceType));
        if (key == null) {
            return Map.of();
        }
        return ROUTES.getOrDefault(key, Map.of());
    }

    public static synchronized Map<String, Object> routeForAction(String surfaceType, String actionId) {
        String normalizedType = normalizeSurfaceType(surfaceType);
        String safeActionId = actionId == null ? "" : actionId.trim();
        String key = ACTION_ROUTE_BY_TYPE.getOrDefault(normalizedType, Map.of()).get(safeActionId);
        if (key == null) {
            key = PRIMARY_ROUTE_BY_TYPE.get(normalizedType);
        }
        if (key == null) {
            return Map.of();
        }
        return ROUTES.getOrDefault(key, Map.of());
    }

    private static NativeClientRouteHandlerSelection selectHandlersForRoute(
            List<NativeClientRouteActionHandlerEntry> handlers,
            Map<String, Object> route
    ) {
        if (handlers == null || handlers.isEmpty()) {
            return new NativeClientRouteHandlerSelection(List.of(), List.of(), List.of(), false);
        }
        String routeSurfaceId = text(route, "surfaceId");
        if (routeSurfaceId.isBlank()) {
            return new NativeClientRouteHandlerSelection(
                    List.copyOf(handlers),
                    List.of(),
                    handlers.stream().map(NativeClientRouteActionHandlerEntry::handlerId).toList(),
                    false);
        }
        List<NativeClientRouteActionHandlerEntry> ownerHandlers = new ArrayList<>();
        List<NativeClientRouteActionHandlerEntry> fallbackHandlers = new ArrayList<>();
        for (NativeClientRouteActionHandlerEntry handler : handlers) {
            if (handlerIdMatchesRoute(handler.handlerId(), routeSurfaceId)) {
                ownerHandlers.add(handler);
            } else {
                fallbackHandlers.add(handler);
            }
        }
        if (ownerHandlers.isEmpty()) {
            return new NativeClientRouteHandlerSelection(
                    List.copyOf(handlers),
                    List.of(),
                    fallbackHandlers.stream().map(NativeClientRouteActionHandlerEntry::handlerId).toList(),
                    false);
        }
        List<NativeClientRouteActionHandlerEntry> orderedHandlers = new ArrayList<>(ownerHandlers);
        orderedHandlers.addAll(fallbackHandlers);
        return new NativeClientRouteHandlerSelection(
                List.copyOf(orderedHandlers),
                ownerHandlers.stream().map(NativeClientRouteActionHandlerEntry::handlerId).toList(),
                fallbackHandlers.stream().map(NativeClientRouteActionHandlerEntry::handlerId).toList(),
                true);
    }

    private static boolean handlerIdMatchesRoute(String handlerId, String routeSurfaceId) {
        if (handlerId == null || routeSurfaceId == null || routeSurfaceId.isBlank()) {
            return false;
        }
        String safeHandlerId = handlerId.trim();
        String safeSurfaceId = routeSurfaceId.trim();
        return safeHandlerId.equals(safeSurfaceId) || safeHandlerId.startsWith(safeSurfaceId + ":");
    }

    private static String builtInProductActionForHostPhase(String surfaceType, String phase) {
        String safeSurfaceType = normalizeSurfaceType(surfaceType);
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

    public static synchronized Map<String, List<Map<String, Object>>> routesBySurfaceType() {
        Map<String, List<Map<String, Object>>> snapshot = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> entry : ROUTES_BY_TYPE.entrySet()) {
            List<Map<String, Object>> routes = new ArrayList<>();
            for (String routeKey : entry.getValue()) {
                Map<String, Object> route = ROUTES.get(routeKey);
                if (route != null) {
                    routes.add(route);
                }
            }
            snapshot.put(entry.getKey(), List.copyOf(routes));
        }
        return Map.copyOf(snapshot);
    }

    public static synchronized Map<String, Map<String, Map<String, Object>>> actionRouteEvidence() {
        Map<String, Map<String, Map<String, Object>>> snapshot = new LinkedHashMap<>();
        for (Map.Entry<String, Map<String, String>> surfaceEntry : ACTION_ROUTE_BY_TYPE.entrySet()) {
            Map<String, Map<String, Object>> actionRoutes = new LinkedHashMap<>();
            for (Map.Entry<String, String> actionEntry : surfaceEntry.getValue().entrySet()) {
                Map<String, Object> route = ROUTES.get(actionEntry.getValue());
                if (route != null) {
                    actionRoutes.put(actionEntry.getKey(), route);
                }
            }
            snapshot.put(surfaceEntry.getKey(), Map.copyOf(actionRoutes));
        }
        return Map.copyOf(snapshot);
    }

    public static synchronized Map<String, Object> actionDispatchEvidence() {
        return Map.of(
                "dispatchCount", ACTION_DISPATCH_EVENTS.size(),
                "events", List.copyOf(ACTION_DISPATCH_EVENTS),
                "summary", actionDispatchSummary()
        );
    }

    public static synchronized Map<String, Map<String, Object>> routes() {
        return Map.copyOf(ROUTES);
    }

    public static synchronized Map<String, Map<String, Map<String, Object>>> actions() {
        Map<String, Map<String, Map<String, Object>>> snapshot = new LinkedHashMap<>();
        for (Map.Entry<String, Map<String, Map<String, Object>>> surfaceEntry : ACTIONS_BY_TYPE.entrySet()) {
            Map<String, Map<String, Object>> actions = new LinkedHashMap<>();
            for (Map.Entry<String, Map<String, Object>> actionEntry : surfaceEntry.getValue().entrySet()) {
                actions.put(actionEntry.getKey(), Map.copyOf(actionEntry.getValue()));
            }
            snapshot.put(surfaceEntry.getKey(), Map.copyOf(actions));
        }
        return Map.copyOf(snapshot);
    }

    public static synchronized Map<String, Object> actionHandlerEvidence() {
        Map<String, Object> evidence = new LinkedHashMap<>();
        for (Map.Entry<String, List<NativeClientRouteActionHandlerEntry>> entry : ACTION_HANDLERS.entrySet()) {
            Map<String, Object> surfaceEvidence = new LinkedHashMap<>();
            surfaceEvidence.put("handlerCount", entry.getValue().size());
            surfaceEvidence.put("handlerIds", entry.getValue().stream()
                    .map(NativeClientRouteActionHandlerEntry::handlerId)
                    .toList());
            evidence.put(entry.getKey(), Map.copyOf(surfaceEvidence));
        }
        return Map.copyOf(evidence);
    }

    public static synchronized void clear() {
        ROUTES.clear();
        PRIMARY_ROUTE_BY_TYPE.clear();
        ROUTES_BY_TYPE.clear();
        ACTION_HANDLERS.clear();
        ACTIONS_BY_TYPE.clear();
        ACTION_ROUTE_BY_TYPE.clear();
        INPUT_BINDINGS_BY_TYPE.clear();
        ACTION_DISPATCH_EVENTS.clear();
        INPUT_DISPATCH_EVENTS.clear();
        LIFECYCLES_BY_TYPE.clear();
        LIFECYCLE_EVENTS_BY_TYPE.clear();
    }

    private static String routeKey(String moduleId, String surfaceId) {
        return moduleId.trim() + ":" + surfaceId.trim();
    }

    private static String routeKeyForActionOwner(String moduleId, String surfaceId, String normalizedType) {
        if (!blank(moduleId) && !blank(surfaceId)) {
            String key = routeKey(moduleId, surfaceId);
            Map<String, Object> route = ROUTES.get(key);
            if (route != null && normalizedType.equals(route.get("surfaceType"))) {
                return key;
            }
        }
        return PRIMARY_ROUTE_BY_TYPE.getOrDefault(normalizedType, "");
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private static String normalizeSurfaceType(String surfaceType) {
        return surfaceType == null ? "" : surfaceType.trim().toLowerCase(Locale.ROOT)
                .replace('-', '_')
                .replace('.', '_');
    }

    private static boolean actionKnown(String surfaceType, String actionId) {
        Map<String, Map<String, Object>> actions = ACTIONS_BY_TYPE.get(surfaceType);
        return actions == null || actions.isEmpty() || actions.containsKey(actionId);
    }

    private static Map<String, Object> actionFor(String surfaceType, String actionId) {
        Map<String, Map<String, Object>> actions = ACTIONS_BY_TYPE.get(surfaceType);
        if (actions == null || actions.isEmpty()) {
            return Map.of();
        }
        return actions.getOrDefault(actionId, Map.of());
    }

    private static Map<String, Map<String, Object>> copyActions(Map<String, Map<String, Object>> actions) {
        Map<String, Map<String, Object>> copy = new LinkedHashMap<>();
        for (Map.Entry<String, Map<String, Object>> entry : actions.entrySet()) {
            String actionId = entry.getKey() == null ? "" : entry.getKey().trim();
            if (!actionId.isBlank()) {
                copy.put(actionId, entry.getValue() == null ? Map.of() : Map.copyOf(entry.getValue()));
            }
        }
        return Map.copyOf(copy);
    }

    private static boolean containsBinding(List<Map<String, Object>> bindings, Map<String, Object> candidate) {
        String candidateName = String.valueOf(candidate.getOrDefault("keyMapping", ""));
        String candidateAction = String.valueOf(candidate.getOrDefault("action", ""));
        for (Map<String, Object> binding : bindings) {
            if (candidateName.equals(String.valueOf(binding.getOrDefault("keyMapping", "")))
                    && candidateAction.equals(String.valueOf(binding.getOrDefault("action", "")))) {
                return true;
            }
        }
        return false;
    }

    private static boolean bindingMatches(
            Map<String, Object> binding,
            String keyMapping,
            int keyCode,
            String inputType
    ) {
        String bindingKeyMapping = String.valueOf(binding.getOrDefault("keyMapping", "")).trim();
        if (!keyMapping.isBlank()) {
            if (!keyMapping.equals(bindingKeyMapping)) {
                return false;
            }
        } else if (keyCode < 0 || keyCode != intValue(binding.get("keyCode"), -1)) {
            return false;
        }
        String bindingInputType = String.valueOf(binding.getOrDefault("inputType", "")).trim().toLowerCase(Locale.ROOT);
        return inputType.isBlank() || bindingInputType.isBlank() || inputType.equals(bindingInputType);
    }

    private static synchronized void recordInputDispatch(
            String keyMapping,
            int keyCode,
            String inputType,
            Map<String, Object> metadata,
            List<Map<String, Object>> targets,
            EchoNativeLoadStatus status
    ) {
        EchoNativeLoadStatus safeStatus = status == null ? EchoNativeLoadStatus.UNSUPPORTED : status;
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("source", "native_loader_input_binding");
        event.put("keyMapping", keyMapping);
        event.put("keyCode", keyCode);
        event.put("inputType", inputType);
        event.put("metadata", metadata == null ? Map.of() : Map.copyOf(metadata));
        event.put("status", safeStatus.name());
        event.put("handled", safeStatus == EchoNativeLoadStatus.MUTATED);
        event.put("targetCount", targets == null ? 0 : targets.size());
        event.put("targets", targets == null ? List.of() : List.copyOf(targets));
        INPUT_DISPATCH_EVENTS.add(Map.copyOf(event));
        if (INPUT_DISPATCH_EVENTS.size() > 128) {
            INPUT_DISPATCH_EVENTS.remove(0);
        }
    }

    private static Map<String, Object> inputDispatchSummary() {
        Map<String, Integer> statusCounts = new LinkedHashMap<>();
        Map<String, Integer> metadataSourceCounts = new LinkedHashMap<>();
        Map<String, Map<String, Object>> latestByKeyMapping = new LinkedHashMap<>();
        Map<String, Map<String, Object>> latestByMetadataSource = new LinkedHashMap<>();
        Map<String, Map<String, Object>> latestHandledTargetBySurface = new LinkedHashMap<>();
        Map<String, Map<String, Object>> latestHandledTargetByMetadataSource = new LinkedHashMap<>();
        for (Map<String, Object> event : INPUT_DISPATCH_EVENTS) {
            String status = objectText(event.get("status"));
            statusCounts.merge(status, 1, Integer::sum);
            String keyMapping = objectText(event.get("keyMapping"));
            if (!keyMapping.isBlank()) {
                latestByKeyMapping.put(keyMapping, event);
            }
            String metadataSource = metadataSource(event);
            if (!metadataSource.isBlank()) {
                metadataSourceCounts.merge(metadataSource, 1, Integer::sum);
                latestByMetadataSource.put(metadataSource, event);
            }
            Object targetsObject = event.get("targets");
            if (!(targetsObject instanceof List<?> targets)) {
                continue;
            }
            for (Object targetObject : targets) {
                if (!(targetObject instanceof Map<?, ?> target)
                        || !Boolean.TRUE.equals(target.get("handled"))) {
                    continue;
                }
                String surfaceType = objectText(target.get("surfaceType"));
                if (!surfaceType.isBlank()) {
                    Map<String, Object> handled = new LinkedHashMap<>();
                    handled.put("source", "native_loader_input_binding");
                    handled.put("keyMapping", event.getOrDefault("keyMapping", ""));
                    handled.put("keyCode", event.getOrDefault("keyCode", -1));
                    handled.put("inputType", event.getOrDefault("inputType", ""));
                    handled.put("dispatchStatus", event.getOrDefault("status", ""));
                    handled.put("surfaceType", surfaceType);
                    handled.put("actionId", target.containsKey("actionId") ? target.get("actionId") : "");
                    handled.put("targetStatus", target.containsKey("status") ? target.get("status") : "");
                    handled.put("routeModuleId", target.containsKey("routeModuleId") ? target.get("routeModuleId") : "");
                    handled.put("routeSurfaceId", target.containsKey("routeSurfaceId") ? target.get("routeSurfaceId") : "");
                    handled.put("routeTrustedMutation", target.containsKey("routeTrustedMutation")
                            ? target.get("routeTrustedMutation")
                            : false);
                    handled.put("routeStatus", target.containsKey("routeStatus") ? target.get("routeStatus") : "");
                    Object route = target.get("route");
                    if (route instanceof Map<?, ?> routeMap) {
                        handled.put("route", Map.copyOf(routeMap));
                    }
                    Map<String, Object> handledSnapshot = Map.copyOf(handled);
                    latestHandledTargetBySurface.put(surfaceType, handledSnapshot);
                    if (!metadataSource.isBlank()) {
                        latestHandledTargetByMetadataSource.put(metadataSource + ":" + surfaceType, handledSnapshot);
                    }
                }
            }
        }
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("statusCounts", Map.copyOf(statusCounts));
        summary.put("metadataSourceCounts", Map.copyOf(metadataSourceCounts));
        summary.put("latestByKeyMapping", copyMapValues(latestByKeyMapping));
        summary.put("latestByMetadataSource", copyMapValues(latestByMetadataSource));
        summary.put("latestHandledTargetBySurface", copyMapValues(latestHandledTargetBySurface));
        summary.put("latestHandledTargetByMetadataSource", copyMapValues(latestHandledTargetByMetadataSource));
        return Map.copyOf(summary);
    }

    private static String metadataSource(Map<String, Object> event) {
        Object metadataObject = event.get("metadata");
        if (metadataObject instanceof Map<?, ?> metadata) {
            return objectText(metadata.get("source"));
        }
        return "";
    }

    private static Map<String, Object> inputDispatchMetadata(
            String keyMapping,
            int keyCode,
            String inputType,
            Map<String, Object> metadata,
            Map<String, Object> binding
    ) {
        Map<String, Object> next = new LinkedHashMap<>();
        if (metadata != null) {
            next.putAll(metadata);
        }
        next.putIfAbsent("source", "native_loader_input_binding");
        next.putIfAbsent("service", "input_binding");
        next.put("keyMapping", keyMapping);
        next.put("keyCode", keyCode);
        next.put("inputType", inputType);
        next.put("binding", binding == null ? Map.of() : Map.copyOf(binding));
        return Map.copyOf(next);
    }

    private static synchronized void recordActionDispatch(
            String surfaceType,
            String actionId,
            Map<String, Object> route,
            Map<String, Object> action,
            Map<String, Object> metadata,
            EchoNativeLoadStatus status,
            String outcome,
            int handlerExceptionCount
    ) {
        recordActionDispatch(
                surfaceType,
                actionId,
                route,
                action,
                metadata,
                status,
                outcome,
                handlerExceptionCount,
                ""
        );
    }

    private static synchronized void recordActionDispatch(
            String surfaceType,
            String actionId,
            Map<String, Object> route,
            Map<String, Object> action,
            Map<String, Object> metadata,
            EchoNativeLoadStatus status,
            String outcome,
            int handlerExceptionCount,
            String handledHandlerId
    ) {
        recordActionDispatch(
                surfaceType,
                actionId,
                route,
                action,
                metadata,
                status,
                outcome,
                handlerExceptionCount,
                handledHandlerId,
                NativeClientRouteHandlerSelection.EMPTY
        );
    }

    private static synchronized void recordActionDispatch(
            String surfaceType,
            String actionId,
            Map<String, Object> route,
            Map<String, Object> action,
            Map<String, Object> metadata,
            EchoNativeLoadStatus status,
            String outcome,
            int handlerExceptionCount,
            String handledHandlerId,
            NativeClientRouteHandlerSelection handlerSelection
    ) {
        Map<String, Object> event = new LinkedHashMap<>();
        NativeClientRouteHandlerSelection safeSelection =
                handlerSelection == null ? NativeClientRouteHandlerSelection.EMPTY : handlerSelection;
        event.put("source", "native_loader_route_dispatch");
        event.put("surfaceType", surfaceType == null ? "" : surfaceType);
        event.put("actionId", actionId == null ? "" : actionId);
        event.put("status", status == null ? EchoNativeLoadStatus.UNSUPPORTED.name() : status.name());
        event.put("outcome", outcome == null ? "" : outcome);
        event.put("handled", status == EchoNativeLoadStatus.MUTATED);
        event.put("handlerExceptionCount", handlerExceptionCount);
        event.put("handledHandlerId", handledHandlerId == null ? "" : handledHandlerId);
        event.put("routeModuleId", text(route, "moduleId"));
        event.put("routeSurfaceId", text(route, "surfaceId"));
        event.put("route", route == null ? Map.of() : Map.copyOf(route));
        event.put("action", action == null ? Map.of() : Map.copyOf(action));
        event.put("metadata", metadata == null ? Map.of() : Map.copyOf(metadata));
        event.put("ownerPreferredHandlers", safeSelection.ownerPreferred());
        event.put("ownerHandlerIds", safeSelection.ownerHandlerIds());
        event.put("fallbackHandlerIds", safeSelection.fallbackHandlerIds());
        event.put("handlerDispatchOrder", safeSelection.orderedHandlerIds());
        ACTION_DISPATCH_EVENTS.add(Map.copyOf(event));
        if (ACTION_DISPATCH_EVENTS.size() > 512) {
            ACTION_DISPATCH_EVENTS.remove(0);
        }
    }

    private static Map<String, Object> actionDispatchSummary() {
        Map<String, Integer> statusCounts = new LinkedHashMap<>();
        Map<String, Integer> metadataSourceCounts = new LinkedHashMap<>();
        Map<String, Map<String, Object>> latestBySurface = new LinkedHashMap<>();
        Map<String, Map<String, Object>> latestHandledBySurface = new LinkedHashMap<>();
        Map<String, Map<String, Object>> latestUnsupportedBySurface = new LinkedHashMap<>();
        Map<String, Map<String, Object>> latestByMetadataSource = new LinkedHashMap<>();
        Map<String, Map<String, Object>> latestHandledByMetadataSource = new LinkedHashMap<>();
        for (Map<String, Object> event : ACTION_DISPATCH_EVENTS) {
            String status = objectText(event.get("status"));
            statusCounts.merge(status, 1, Integer::sum);
            String metadataSource = metadataSource(event);
            if (!metadataSource.isBlank()) {
                metadataSourceCounts.merge(metadataSource, 1, Integer::sum);
                latestByMetadataSource.put(metadataSource, event);
            }
            String surfaceType = objectText(event.get("surfaceType"));
            if (surfaceType.isBlank()) {
                continue;
            }
            latestBySurface.put(surfaceType, event);
            if (Boolean.TRUE.equals(event.get("handled"))) {
                latestHandledBySurface.put(surfaceType, event);
                if (!metadataSource.isBlank()) {
                    latestHandledByMetadataSource.put(metadataSource + ":" + surfaceType, event);
                }
            }
            if (EchoNativeLoadStatus.UNSUPPORTED.name().equals(status)) {
                latestUnsupportedBySurface.put(surfaceType, event);
            }
        }
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("statusCounts", Map.copyOf(statusCounts));
        summary.put("metadataSourceCounts", Map.copyOf(metadataSourceCounts));
        summary.put("latestBySurface", copyMapValues(latestBySurface));
        summary.put("latestHandledBySurface", copyMapValues(latestHandledBySurface));
        summary.put("latestUnsupportedBySurface", copyMapValues(latestUnsupportedBySurface));
        summary.put("latestByMetadataSource", copyMapValues(latestByMetadataSource));
        summary.put("latestHandledByMetadataSource", copyMapValues(latestHandledByMetadataSource));
        return Map.copyOf(summary);
    }

    private static Map<String, Object> lifecycleEventSummary() {
        Map<String, Integer> eventCountBySurface = new LinkedHashMap<>();
        Map<String, Integer> phaseCounts = new LinkedHashMap<>();
        Map<String, Integer> metadataSourceCounts = new LinkedHashMap<>();
        Map<String, Map<String, Object>> latestBySurface = new LinkedHashMap<>();
        Map<String, Map<String, Object>> latestBySurfacePhase = new LinkedHashMap<>();
        Map<String, Map<String, Object>> latestBySurfaceAction = new LinkedHashMap<>();
        Map<String, Map<String, Object>> latestByMetadataSource = new LinkedHashMap<>();
        Map<String, Map<String, Object>> latestByMetadataSourceSurface = new LinkedHashMap<>();
        for (Map.Entry<String, List<NativeClientSurfaceLifecycleEvent>> entry : LIFECYCLE_EVENTS_BY_TYPE.entrySet()) {
            for (NativeClientSurfaceLifecycleEvent event : entry.getValue()) {
                Map<String, Object> evidence = lifecycleEventSnapshot(event);
                String surfaceType = objectText(evidence.get("surfaceType"));
                String phase = objectText(evidence.get("phase"));
                String actionId = objectText(evidence.get("actionId"));
                String metadataSource = metadataSource(evidence);
                if (!metadataSource.isBlank()) {
                    metadataSourceCounts.merge(metadataSource, 1, Integer::sum);
                    latestByMetadataSource.put(metadataSource, evidence);
                }
                if (surfaceType.isBlank()) {
                    continue;
                }
                eventCountBySurface.merge(surfaceType, 1, Integer::sum);
                if (!metadataSource.isBlank()) {
                    latestByMetadataSourceSurface.put(metadataSource + ":" + surfaceType, evidence);
                }
                if (!phase.isBlank()) {
                    phaseCounts.merge(phase, 1, Integer::sum);
                    latestBySurfacePhase.put(surfaceType + ":" + phase, evidence);
                }
                if (!actionId.isBlank()) {
                    latestBySurfaceAction.put(surfaceType + ":" + actionId, evidence);
                }
                latestBySurface.put(surfaceType, evidence);
            }
        }
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("eventCountBySurface", Map.copyOf(eventCountBySurface));
        summary.put("phaseCounts", Map.copyOf(phaseCounts));
        summary.put("metadataSourceCounts", Map.copyOf(metadataSourceCounts));
        summary.put("latestBySurface", copyMapValues(latestBySurface));
        summary.put("latestBySurfacePhase", copyMapValues(latestBySurfacePhase));
        summary.put("latestBySurfaceAction", copyMapValues(latestBySurfaceAction));
        summary.put("latestByMetadataSource", copyMapValues(latestByMetadataSource));
        summary.put("latestByMetadataSourceSurface", copyMapValues(latestByMetadataSourceSurface));
        return Map.copyOf(summary);
    }

    private static Map<String, Object> lifecycleEventSnapshot(NativeClientSurfaceLifecycleEvent event) {
        String surfaceType = normalizeSurfaceType(event.surfaceType());
        String actionId = event.actionId() == null ? "" : event.actionId().trim();
        Map<String, Object> route = routeForAction(surfaceType, actionId);
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("surfaceType", surfaceType);
        snapshot.put("phase", event.phase() == null ? "" : event.phase().trim());
        snapshot.put("actionId", actionId);
        Map<String, Object> metadata = event.metadata() == null ? Map.of() : Map.copyOf(event.metadata());
        snapshot.put("metadata", metadata);
        putIfPresent(snapshot, "metadataSource", metadata.get("source"));
        putIfPresent(snapshot, "metadataEventType", metadata.get("eventType"));
        putIfPresent(snapshot, "metadataService", metadata.get("service"));
        putIfPresent(snapshot, "nativeLoaderUiHostService", metadata.get("nativeLoaderUiHostService"));
        putIfPresent(snapshot, "nativeLoaderUiHostSurface", metadata.get("nativeLoaderUiHostSurface"));
        putIfPresent(snapshot, "nativeLoaderUiHostAction", metadata.get("nativeLoaderUiHostAction"));
        putIfPresent(snapshot, "nativeLoaderScreenLifecycleHandoff", metadata.get("nativeLoaderScreenLifecycleHandoff"));
        if (!route.isEmpty()) {
            snapshot.put("route", route);
            snapshot.put("routeModuleId", route.getOrDefault("moduleId", ""));
            snapshot.put("routeSurfaceId", route.getOrDefault("surfaceId", ""));
            snapshot.put("routeTrustedMutation", route.getOrDefault("trustedMutation", false));
            snapshot.put("routeStatus", route.getOrDefault("status", ""));
            Object evidenceObject = route.get("evidence");
            if (evidenceObject instanceof Map<?, ?> routeEvidence) {
                snapshot.put("nativeClientRouteProcess",
                        routeEvidence.containsKey("nativeClientRouteProcess")
                                ? routeEvidence.get("nativeClientRouteProcess")
                                : false);
                snapshot.put("neoForgeEventOwnershipRequired",
                        routeEvidence.containsKey("neoForgeEventOwnershipRequired")
                                ? routeEvidence.get("neoForgeEventOwnershipRequired")
                                : true);
                snapshot.put("clientRouteMutationSupported",
                        routeEvidence.containsKey("clientRouteMutationSupported")
                                ? routeEvidence.get("clientRouteMutationSupported")
                                : false);
            }
        }
        return Map.copyOf(snapshot);
    }

    private static Map<String, Map<String, Object>> copyMapValues(Map<String, Map<String, Object>> values) {
        Map<String, Map<String, Object>> copy = new LinkedHashMap<>();
        values.forEach((key, value) -> copy.put(key, Map.copyOf(value)));
        return Map.copyOf(copy);
    }

    private static String objectText(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static void putIfPresent(Map<String, Object> values, String key, Object value) {
        if (values != null && key != null && value != null && !String.valueOf(value).isBlank()) {
            values.put(key, value);
        }
    }

    private static String text(Map<String, Object> values, String key) {
        if (values == null || key == null) {
            return "";
        }
        Object value = values.get(key);
        return value == null ? "" : String.valueOf(value);
    }

    private static int intValue(Object value, int fallback) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(String.valueOf(value).trim());
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private static void refreshRouteLifecycleEvidence(
            String normalizedType,
            NativeClientSurfaceLifecycle lifecycle
    ) {
        for (Map.Entry<String, Map<String, Object>> entry : new LinkedHashMap<>(ROUTES).entrySet()) {
            Map<String, Object> route = entry.getValue();
            if (!normalizedType.equals(route.get("surfaceType"))) {
                continue;
            }
            Map<String, Object> refreshed = new LinkedHashMap<>(route);
            refreshed.put("lifecycle", lifecycle.toEvidence());
            ROUTES.put(entry.getKey(), Map.copyOf(refreshed));
        }
    }

    private static Map<String, Map<String, Object>> lifecycleRouteSnapshot(boolean mounted) {
        Map<String, Map<String, Object>> snapshot = new LinkedHashMap<>();
        for (Map.Entry<String, NativeClientSurfaceLifecycle> entry : LIFECYCLES_BY_TYPE.entrySet()) {
            NativeClientSurfaceLifecycle lifecycle = entry.getValue();
            boolean include = mounted ? lifecycle.mountedByDefault() : lifecycle.visibleByDefault();
            if (!include) {
                continue;
            }
            Map<String, Object> route = routeForSurface(entry.getKey());
            if (!route.isEmpty()) {
                snapshot.put(entry.getKey(), route);
            }
        }
        return Map.copyOf(snapshot);
    }

    private static void recordDefaultMountEvent(
            String normalizedType,
            NativeClientSurfaceLifecycle lifecycle
    ) {
        if (lifecycle == null
                || (!lifecycle.mountedByDefault() && !lifecycle.visibleByDefault())
                || !PRIMARY_ROUTE_BY_TYPE.containsKey(normalizedType)) {
            return;
        }
        List<NativeClientSurfaceLifecycleEvent> existingEvents = LIFECYCLE_EVENTS_BY_TYPE.getOrDefault(
                normalizedType,
                List.of());
        for (NativeClientSurfaceLifecycleEvent event : existingEvents) {
            if ("mount".equals(event.phase())
                    && "native_loader.default_mount".equals(event.actionId())
                    && Boolean.TRUE.equals(event.metadata().get("defaultLifecycleEvent"))) {
                return;
            }
        }
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("defaultLifecycleEvent", true);
        metadata.put("visibleByDefault", lifecycle.visibleByDefault());
        metadata.put("mountedByDefault", lifecycle.mountedByDefault());
        metadata.put("source", "native_loader_client_route_table");
        metadata.put("lifecycle", lifecycle.toEvidence());
        List<NativeClientSurfaceLifecycleEvent> events = new ArrayList<>(existingEvents);
        events.add(new NativeClientSurfaceLifecycleEvent(
                normalizedType,
                "mount",
                "native_loader.default_mount",
                Map.copyOf(metadata)));
        LIFECYCLE_EVENTS_BY_TYPE.put(normalizedType, List.copyOf(events));
    }

    private static NativeClientSurfaceLifecycle inferredLifecycle(
            String surfaceType,
            Map<String, Object> config,
            Map<String, Object> evidence
    ) {
        String normalizedType = normalizeSurfaceType(surfaceType);
        boolean hud = normalizedType.equals("hud") || normalizedType.equals("hud_widget") || normalizedType.equals("hud_layout");
        boolean overlay = normalizedType.equals("ui_overlay") || normalizedType.equals("client_overlay");
        boolean screen = normalizedType.equals("screen") || normalizedType.equals("screen_surface")
                || normalizedType.equals("main_menu") || normalizedType.equals("terminal")
                || normalizedType.equals("index") || normalizedType.equals("lens")
                || normalizedType.equals("holomap") || normalizedType.equals("holo_map")
                || normalizedType.equals("loading_screen");
        boolean renderLifecycle = hud || overlay || screen || normalizedType.equals("theme");
        boolean inputLifecycle = screen && !normalizedType.equals("loading_screen") || hud || overlay;
        boolean visibleByDefault = hud || overlay || normalizedType.equals("main_menu") || normalizedType.equals("loading_screen");
        boolean mountedByDefault = visibleByDefault || normalizedType.equals("terminal")
                || normalizedType.equals("index") || normalizedType.equals("lens")
                || normalizedType.equals("holomap") || normalizedType.equals("holo_map");
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("source", "native_loader_route_table_inference");
        metadata.put("config", config == null ? Map.of() : Map.copyOf(config));
        metadata.put("evidence", evidence == null ? Map.of() : Map.copyOf(evidence));
        return new NativeClientSurfaceLifecycle(
                normalizedType,
                renderLifecycle,
                screen,
                inputLifecycle,
                visibleByDefault,
                mountedByDefault,
                renderLifecycle ? List.of("frame_begin", "render", "frame_end") : List.of(),
                screen ? List.of("mount", "open", "close", "unmount") : List.of(),
                inputLifecycle ? List.of("focus", "key", "mouse", "action") : List.of(),
                Map.copyOf(metadata)
        );
    }

    private static NativeClientSurfaceLifecycle normalizedLifecycle(
            String surfaceType,
            NativeClientSurfaceLifecycle lifecycle
    ) {
        String normalizedType = normalizeSurfaceType(surfaceType);
        return new NativeClientSurfaceLifecycle(
                normalizedType,
                lifecycle.renderLifecycle(),
                lifecycle.screenLifecycle(),
                lifecycle.inputLifecycle(),
                lifecycle.visibleByDefault(),
                lifecycle.mountedByDefault(),
                lifecycle.renderPhases(),
                lifecycle.screenPhases(),
                lifecycle.inputPhases(),
                lifecycle.metadata()
        );
    }

    @FunctionalInterface
    public interface NativeClientRouteActionHandler {
        boolean dispatch(NativeClientRouteActionContext context);
    }

    private record NativeClientRouteHandlerSelection(
            List<NativeClientRouteActionHandlerEntry> orderedHandlers,
            List<String> ownerHandlerIds,
            List<String> fallbackHandlerIds,
            boolean ownerPreferred
    ) {
        private static final NativeClientRouteHandlerSelection EMPTY =
                new NativeClientRouteHandlerSelection(List.of(), List.of(), List.of(), false);

        private NativeClientRouteHandlerSelection {
            orderedHandlers = orderedHandlers == null ? List.of() : List.copyOf(orderedHandlers);
            ownerHandlerIds = ownerHandlerIds == null ? List.of() : List.copyOf(ownerHandlerIds);
            fallbackHandlerIds = fallbackHandlerIds == null ? List.of() : List.copyOf(fallbackHandlerIds);
        }

        private List<String> orderedHandlerIds() {
            return orderedHandlers.stream()
                    .map(NativeClientRouteActionHandlerEntry::handlerId)
                    .toList();
        }
    }

    private record NativeClientRouteActionHandlerEntry(
            String handlerId,
            NativeClientRouteActionHandler handler
    ) {
    }

    public record NativeClientRouteActionContext(
            String surfaceType,
            String actionId,
            Map<String, Object> route,
            Map<String, Object> action,
            Map<String, Object> metadata
    ) {
        public NativeClientRouteActionContext {
            surfaceType = normalizeSurfaceType(surfaceType);
            actionId = actionId == null ? "" : actionId.trim();
            route = route == null ? Map.of() : Map.copyOf(route);
            action = action == null ? Map.of() : Map.copyOf(action);
            metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
        }
    }

    private record InputDispatchTarget(
            String surfaceType,
            String actionId,
            Map<String, Object> binding
    ) {
        private InputDispatchTarget {
            binding = binding == null ? Map.of() : Map.copyOf(binding);
        }
    }
}
