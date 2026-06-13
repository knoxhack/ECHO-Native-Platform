package dev.echo.nativeplatform.loader;

import dev.echo.nativeplatform.contracts.EchoNativeBootstrapProductProfile;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class NativeLoaderRuntimeBridgeAggregator {
    private NativeLoaderRuntimeBridgeAggregator() {
    }

    public static Map<String, Object> aggregateSdkRegistryDeclarations(
            Map<String, Map<String, Object>> nativeActivations
    ) {
        Set<String> itemIds = new LinkedHashSet<>();
        Set<String> blockIds = new LinkedHashSet<>();
        Set<String> creativeTabIds = new LinkedHashSet<>();
        List<Map<String, Object>> declarations = new ArrayList<>();
        for (Map.Entry<String, Map<String, Object>> entry : nativeActivations.entrySet()) {
            if (!nativeActivationLoaded(entry.getValue())) {
                continue;
            }
            for (Map<String, Object> mutation : objectList(entry.getValue().get("moduleLifecycleRecords"))) {
                addRegistryMutationDeclaration(entry.getKey(), mutation, itemIds, blockIds, creativeTabIds, declarations);
            }
            for (Map<String, Object> mutation : objectList(entry.getValue().get("mutations"))) {
                addRegistryMutationDeclaration(entry.getKey(), mutation, itemIds, blockIds, creativeTabIds, declarations);
            }
            Map<String, Object> registryBridge = object(entry.getValue().get("registryBridge"));
            for (Map<String, Object> registration : objectList(registryBridge.get("registrations"))) {
                String registry = normalizeRegistry(String.valueOf(registration.getOrDefault("registry", "")));
                String id = normalizeContentId(String.valueOf(registration.getOrDefault("id", "")), entry.getKey());
                if (registry.isBlank() || id.isBlank()) {
                    continue;
                }
                Map<String, Object> declaration = new LinkedHashMap<>(registration);
                declaration.put("moduleId", entry.getKey());
                declaration.put("registry", registry);
                declaration.put("id", id);
                declarations.add(Map.copyOf(declaration));
                addRegistryId(registry, id, itemIds, blockIds, creativeTabIds);
            }
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("applied", !declarations.isEmpty());
        data.put("bridge", "echo_native.sdk_registry_declarations");
        data.put("declarationCount", declarations.size());
        data.put("itemIds", List.copyOf(itemIds));
        data.put("blockIds", List.copyOf(blockIds));
        data.put("creativeTabIds", List.copyOf(creativeTabIds));
        data.put("creativeTabDeclarations", declarations.stream()
                .filter(declaration -> "creative_tab".equals(String.valueOf(declaration.getOrDefault("registry", ""))))
                .toList());
        data.put("declarations", List.copyOf(declarations));
        data.put("summary", declarations.isEmpty()
                ? "No loaded native module SDK registry declarations were available."
                : "Loaded native module SDK registry declarations were promoted as bootstrap registry inputs.");
        return Map.copyOf(data);
    }

    static Map<String, Object> aggregateSdkClientUiDeclarations(
            Map<String, Map<String, Object>> nativeActivations
    ) {
        Set<String> surfaceIds = new LinkedHashSet<>();
        Set<String> surfaceTypes = new LinkedHashSet<>();
        List<Map<String, Object>> declarations = new ArrayList<>();
        for (Map.Entry<String, Map<String, Object>> entry : nativeActivations.entrySet()) {
            if (!nativeActivationLoaded(entry.getValue())) {
                continue;
            }
            Map<String, Object> registryBridge = object(entry.getValue().get("registryBridge"));
            for (Map<String, Object> registration : objectList(registryBridge.get("registrations"))) {
                String registry = normalizeRegistry(String.valueOf(registration.getOrDefault("registry", "")));
                if (!isClientUiRegistry(registry)) {
                    continue;
                }
                String id = normalizeContentId(String.valueOf(registration.getOrDefault("id", "")), entry.getKey());
                if (id.isBlank()) {
                    continue;
                }
                String surfaceType = surfaceType(registry);
                Map<String, Object> declaration = new LinkedHashMap<>(registration);
                declaration.put("moduleId", entry.getKey());
                declaration.put("registry", registry);
                declaration.put("surfaceId", id);
                declaration.put("surfaceType", surfaceType);
                declaration.put("liveClientBridgeRequired", true);
                declarations.add(Map.copyOf(declaration));
                surfaceIds.add(id);
                surfaceTypes.add(surfaceType);
            }
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("applied", !declarations.isEmpty());
        data.put("bridge", "echo_native.sdk_client_ui_declarations");
        data.put("declarationCount", declarations.size());
        data.put("surfaceIds", List.copyOf(surfaceIds));
        data.put("surfaceTypes", List.copyOf(surfaceTypes));
        data.put("declarations", List.copyOf(declarations));
        data.put("liveClientBridgeRequired", !declarations.isEmpty());
        data.put("summary", declarations.isEmpty()
                ? "No loaded native module client UI declarations were available."
                : "Loaded native module client UI declarations were promoted as live UI bridge inputs.");
        return Map.copyOf(data);
    }

    static Map<String, Object> aggregateSdkResourceDeclarations(
            Map<String, Map<String, Object>> nativeActivations
    ) {
        Set<String> resourceIds = new LinkedHashSet<>();
        Set<String> resourceTypes = new LinkedHashSet<>();
        List<Map<String, Object>> declarations = new ArrayList<>();
        for (Map.Entry<String, Map<String, Object>> entry : nativeActivations.entrySet()) {
            if (!nativeActivationLoaded(entry.getValue())) {
                continue;
            }
            Map<String, Object> registryBridge = object(entry.getValue().get("registryBridge"));
            for (Map<String, Object> registration : objectList(registryBridge.get("registrations"))) {
                String registry = normalizeRegistry(String.valueOf(registration.getOrDefault("registry", "")));
                if (!isResourceRegistry(registry)) {
                    continue;
                }
                String id = normalizeContentId(String.valueOf(registration.getOrDefault("id", "")), entry.getKey());
                if (id.isBlank()) {
                    continue;
                }
                Map<String, Object> declaration = new LinkedHashMap<>(registration);
                declaration.put("moduleId", entry.getKey());
                declaration.put("registry", registry);
                declaration.put("resourceId", id);
                declaration.put("resourceType", resourceType(registry));
                declaration.put("nativeResourceHostRequired", true);
                declaration.put("worldStartupInput", isWorldStartupResource(registry));
                declarations.add(Map.copyOf(declaration));
                resourceIds.add(id);
                resourceTypes.add(resourceType(registry));
            }
        }
        long worldStartupResourceCount = declarations.stream()
                .filter(declaration -> Boolean.TRUE.equals(declaration.get("worldStartupInput")))
                .count();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("applied", !declarations.isEmpty());
        data.put("bridge", "echo_native.sdk_resource_declarations");
        data.put("declarationCount", declarations.size());
        data.put("worldStartupResourceCount", worldStartupResourceCount);
        data.put("resourceIds", List.copyOf(resourceIds));
        data.put("resourceTypes", List.copyOf(resourceTypes));
        data.put("declarations", List.copyOf(declarations));
        data.put("nativeResourceHostRequired", !declarations.isEmpty());
        data.put("worldStartupResourcesPromoted", worldStartupResourceCount > 0);
        data.put("summary", declarations.isEmpty()
                ? "No loaded native module SDK resource declarations were available."
                : "Loaded native module SDK resource declarations were promoted as bootstrap resource inputs.");
        return Map.copyOf(data);
    }

    static Map<String, Object> aggregateTransformBridge(
            Map<String, Map<String, Object>> nativeActivations
    ) {
        List<Map<String, Object>> modules = new ArrayList<>();
        Set<String> supportedNativeDeclarations = new LinkedHashSet<>();
        int transformRequestModuleCount = 0;
        int plannedNativeProjectionCount = 0;
        boolean allCompatible = true;
        boolean bytecodeMutationAllowed = false;
        for (Map.Entry<String, Map<String, Object>> entry : nativeActivations.entrySet()) {
            Map<String, Object> policy = object(entry.getValue().get("transformCompatibilityPolicy"));
            if (policy.isEmpty()) {
                continue;
            }
            boolean hasTransformRequests = Boolean.TRUE.equals(policy.get("hasTransformRequests"));
            boolean compatible = Boolean.TRUE.equals(policy.get("compatible"));
            List<String> moduleNativeDeclarations = stringList(policy.get("supportedNativeDeclarations"));
            supportedNativeDeclarations.addAll(moduleNativeDeclarations);
            plannedNativeProjectionCount += integer(policy.get("plannedNativeProjectionCount"));
            if (hasTransformRequests) {
                transformRequestModuleCount++;
            }
            if (!compatible) {
                allCompatible = false;
            }
            if (Boolean.TRUE.equals(policy.get("bytecodeMutationAllowed"))
                    || Boolean.TRUE.equals(policy.get("minecraftBytecodeMutationAllowed"))
                    || Boolean.TRUE.equals(policy.get("addonBytecodeMutationAllowed"))) {
                bytecodeMutationAllowed = true;
            }
            Map<String, Object> module = new LinkedHashMap<>();
            module.put("moduleId", entry.getKey());
            module.put("hasTransformRequests", hasTransformRequests);
            module.put("compatible", compatible);
            module.put("decision", String.valueOf(policy.getOrDefault("decision", "")));
            module.put("nativeProjectionReplacementPlanned",
                    Boolean.TRUE.equals(policy.get("nativeProjectionReplacementPlanned")));
            module.put("plannedNativeProjectionCount", integer(policy.get("plannedNativeProjectionCount")));
            module.put("replacementCoverageComplete",
                    Boolean.TRUE.equals(policy.get("replacementCoverageComplete")));
            module.put("supportedNativeDeclarations", moduleNativeDeclarations);
            module.put("declaredForgeStyleTransforms",
                    stringList(policy.get("declaredForgeStyleTransforms")));
            module.put("declaredNativeReplacements",
                    stringList(policy.get("declaredNativeReplacements")));
            modules.add(Map.copyOf(module));
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("applied", transformRequestModuleCount > 0 || plannedNativeProjectionCount > 0);
        data.put("bridge", "echo_native.transform_compatibility");
        data.put("moduleCount", modules.size());
        data.put("transformRequestModuleCount", transformRequestModuleCount);
        data.put("compatible", allCompatible);
        data.put("transformPlanningOnly", true);
        data.put("transformsEnabled", false);
        data.put("transformsPerformed", false);
        data.put("bytecodeMutated", false);
        data.put("bytecodeMutationAllowed", bytecodeMutationAllowed);
        data.put("nativeProjectionReplacementPlanned", plannedNativeProjectionCount > 0);
        data.put("plannedNativeProjectionCount", plannedNativeProjectionCount);
        data.put("supportedNativeDeclarations", List.copyOf(supportedNativeDeclarations));
        data.put("modules", List.copyOf(modules));
        data.put("summary", plannedNativeProjectionCount > 0
                ? "Native Loader transform compatibility selected supported native projection replacements without bytecode mutation."
                : "No native transform replacement projections were required by loaded module descriptors.");
        return Map.copyOf(data);
    }

    private static void addRegistryMutationDeclaration(
            String moduleId,
            Map<String, Object> mutation,
            Set<String> itemIds,
            Set<String> blockIds,
            Set<String> creativeTabIds,
            List<Map<String, Object>> declarations
    ) {
        if (!"registry".equals(String.valueOf(mutation.getOrDefault("surface", "")))) {
            return;
        }
        if (!String.valueOf(mutation.getOrDefault("action", "")).contains("native_registry_host_registered")) {
            return;
        }
        if (!"MUTATED".equals(String.valueOf(mutation.getOrDefault("status", "")))) {
            return;
        }
        String target = String.valueOf(mutation.getOrDefault("target", ""));
        int separator = target.indexOf(':');
        if (separator < 1 || separator + 1 >= target.length()) {
            return;
        }
        String registry = normalizeRegistry(target.substring(0, separator));
        String id = normalizeContentId(target.substring(separator + 1), moduleId);
        if (registry.isBlank() || id.isBlank()) {
            return;
        }
        Map<String, Object> declaration = new LinkedHashMap<>();
        declaration.put("moduleId", moduleId);
        declaration.put("registry", registry);
        declaration.put("id", id);
        declaration.put("source", "module_lifecycle_registry_mutation");
        declaration.put("status", "MUTATED");
        declarations.add(Map.copyOf(declaration));
        addRegistryId(registry, id, itemIds, blockIds, creativeTabIds);
    }

    private static void addRegistryId(
            String registry,
            String id,
            Set<String> itemIds,
            Set<String> blockIds,
            Set<String> creativeTabIds
    ) {
        switch (registry) {
            case "item" -> itemIds.add(id);
            case "block" -> blockIds.add(id);
            case "creative_tab" -> creativeTabIds.add(id);
            default -> {
                // Other native registry surfaces are tracked but are not vanilla item/block registry inputs yet.
            }
        }
    }

    static Map<String, Object> aggregateLifecycleBridge(
            Map<String, Map<String, Object>> nativeActivations,
            Config config
    ) {
        List<Map<String, Object>> modules = new ArrayList<>();
        int phaseCount = 0;
        int executedPhaseCount = 0;
        int safePhaseCount = 0;
        int callbackCount = 0;
        int callbackModuleCount = 0;
        boolean allRequiredCallbacksCalled = !nativeActivations.isEmpty();
        List<String> requiredCallbacks = config.profile().requiredNativeLifecycleCallbacks();
        for (Map.Entry<String, Map<String, Object>> entry : nativeActivations.entrySet()) {
            if (!nativeActivationClassLoaded(entry.getValue())) {
                allRequiredCallbacksCalled = false;
                continue;
            }
            Map<String, Object> lifecycle = object(entry.getValue().get("lifecycleBridge"));
            List<Map<String, Object>> phases = objectList(lifecycle.get("phases"));
            Map<String, Object> callbackDispatch = object(entry.getValue().get("nativeLifecycleDispatch"));
            List<Map<String, Object>> callbacks = objectList(callbackDispatch.get("callbacks"));
            int moduleCallbackCount = calledCallbackCount(callbacks);
            if (!Boolean.TRUE.equals(callbackDispatch.get("allRequiredCallbacksCalled"))
                    || moduleCallbackCount != requiredCallbacks.size()) {
                allRequiredCallbacksCalled = false;
            }
            if (moduleCallbackCount > 0) {
                callbackModuleCount++;
                callbackCount += moduleCallbackCount;
            }
            List<Map<String, Object>> safePhases = phases.stream()
                    .map(NativeLoaderRuntimeBridgeAggregator::safeLifecyclePhase)
                    .toList();
            int moduleExecutedPhaseCount = countTrue(safePhases, "phaseCodeExecuted");
            int moduleSafePhaseCount = countTrue(safePhases, "safeHookRun");
            if (phases.isEmpty() && moduleCallbackCount == 0) {
                continue;
            }
            Map<String, Object> module = new LinkedHashMap<>();
            module.put("moduleId", entry.getKey());
            module.put("phaseCount", phases.size());
            module.put("executedPhaseCount", moduleExecutedPhaseCount);
            module.put("phases", safePhases);
            module.put("requiredCallbackCount", requiredCallbacks.size());
            module.put("calledCallbackCount", moduleCallbackCount);
            module.put("allRequiredCallbacksCalled", Boolean.TRUE.equals(callbackDispatch.get("allRequiredCallbacksCalled")));
            module.put("callbacks", callbacks);
            modules.add(module);
            phaseCount += phases.size();
            executedPhaseCount += moduleExecutedPhaseCount;
            safePhaseCount += moduleSafePhaseCount;
        }
        boolean lifecycleCodeExecuted = executedPhaseCount > 0 || callbackCount > 0;
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("applied", lifecycleCodeExecuted);
        data.put("bridge", "adaptercore.native_lifecycle");
        data.put("moduleCount", modules.size());
        data.put("phaseCount", phaseCount);
        data.put("executedPhaseCount", executedPhaseCount);
        data.put("requiredCallbacks", requiredCallbacks);
        data.put("callbackModuleCount", callbackModuleCount);
        data.put("calledCallbackCount", callbackCount);
        data.put("expectedCallbackCount", nativeActivations.size() * requiredCallbacks.size());
        data.put("allRequiredCallbacksCalled", allRequiredCallbacksCalled);
        data.put("safeLifecycleHooksRun", safePhaseCount > 0 || callbackCount > 0);
        data.put("safeLifecycleHookRunCount", safePhaseCount + callbackCount);
        data.put("hookExecutionMode", lifecycleCodeExecuted
                ? "adaptercore_native_lifecycle_runtime_phase_and_callback_mutations"
                : "none");
        data.put("lifecycleCodeExecuted", lifecycleCodeExecuted);
        data.put("addonServiceCodeExecuted", callbackCount > 0);
        data.put("modules", modules);
        data.put("summary", lifecycleCodeExecuted
                ? "AdapterCore native lifecycle bridge consumed runtime module lifecycle phase results and callback mutation records."
                : "No AdapterCore native lifecycle contracts were available.");
        return data;
    }

    static Map<String, Object> aggregateEventBridge(Map<String, Map<String, Object>> nativeActivations) {
        List<Map<String, Object>> modules = new ArrayList<>();
        int hookCount = 0;
        Map<String, Object> lifecycleEventHost = nativeLoaderLifecycleEventHost(nativeActivations);
        int nativeEventHostSubscriptionCount = integer(lifecycleEventHost.get("eventSubscriptionCount"));
        int nativePublishedEventCount = integer(lifecycleEventHost.get("publishedEventCount"));
        int nativePublishedEventHandlerCount = nativePublishedEventHandlerCount(lifecycleEventHost);
        boolean nativeEventHostHandlerExecuted = nativePublishedEventHandlerCount > 0
                || objectList(lifecycleEventHost.get("publishedEvents")).stream()
                .anyMatch(event -> Boolean.TRUE.equals(event.get("handlerExecuted")));
        for (Map.Entry<String, Map<String, Object>> entry : nativeActivations.entrySet()) {
            if (!nativeActivationLoaded(entry.getValue())) {
                continue;
            }
            Map<String, Object> eventBridge = object(entry.getValue().get("eventBridge"));
            List<Map<String, Object>> hooks = objectList(eventBridge.get("hooks"));
            if (hooks.isEmpty()) {
                continue;
            }
            Map<String, Object> module = new LinkedHashMap<>();
            module.put("moduleId", entry.getKey());
            module.put("hookCount", hooks.size());
            module.put("hooks", hooks.stream()
                    .map(NativeLoaderRuntimeBridgeAggregator::safeEventHook)
                    .toList());
            modules.add(module);
            hookCount += hooks.size();
        }
        boolean applied = hookCount > 0 || nativeEventHostSubscriptionCount > 0 || nativePublishedEventCount > 0;
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("applied", applied);
        data.put("bridge", "adaptercore.native_event");
        data.put("moduleCount", modules.size());
        data.put("hookCount", hookCount);
        data.put("safeEventHookAttachedCount", hookCount);
        data.put("safeEventHooksRun", nativeEventHostHandlerExecuted);
        data.put("safeEventHookRunCount", nativePublishedEventHandlerCount);
        data.put("nativeEventHostAttached", !lifecycleEventHost.isEmpty());
        data.put("nativeEventHostSubscriptionCount", nativeEventHostSubscriptionCount);
        data.put("nativePublishedEventCount", nativePublishedEventCount);
        data.put("nativePublishedEventHandlerCount", nativePublishedEventHandlerCount);
        data.put("nativeEventHostHandlerExecuted", nativeEventHostHandlerExecuted);
        data.put("nativePublishedEvents", objectList(lifecycleEventHost.get("publishedEvents")));
        data.put("hookExecutionMode", nativeEventHostHandlerExecuted
                ? "echo_native_event_host_published_handlers"
                : hookCount > 0 || nativeEventHostSubscriptionCount > 0
                ? "echo_native_event_hooks_attached_waiting_for_runtime_events"
                : "none");
        data.put("safeLifecycleHooksRun", nativeEventHostHandlerExecuted);
        data.put("handlerExecuted", nativeEventHostHandlerExecuted);
        data.put("gameplayHandlerExecuted", false);
        data.put("addonServiceCodeExecuted", hookCount > 0 || nativeEventHostSubscriptionCount > 0);
        data.put("modules", modules);
        data.put("summary", nativeEventHostHandlerExecuted
                ? "AdapterCore native event host published " + nativePublishedEventCount
                + " event(s) and executed " + nativePublishedEventHandlerCount
                + " declared safe handler(s) without executing gameplay handlers."
                : hookCount > 0 || nativeEventHostSubscriptionCount > 0
                ? "AdapterCore native event bridge attached/subscribed declared event hooks; runtime event publication has not executed handlers yet."
                : "No AdapterCore native event hook contracts were available.");
        return data;
    }

    static Map<String, Object> nativeLoaderLifecycleEventHost(Map<String, Map<String, Object>> nativeActivations) {
        Map<String, Object> selected = Map.of();
        int selectedScore = -1;
        for (Map<String, Object> activation : nativeActivations.values()) {
            Map<String, Object> host = object(activation.get("nativeLoaderLifecycleEventHost"));
            if (host.isEmpty()) {
                continue;
            }
            int score = integer(host.get("lifecycleEventCount"))
                    + integer(host.get("publishedEventCount"))
                    + integer(host.get("eventSubscriptionCount"));
            if (score > selectedScore) {
                selected = host;
                selectedScore = score;
            }
        }
        return selected.isEmpty() ? Map.of() : Map.copyOf(selected);
    }

    static Map<String, Object> aggregateServiceBridge(
            Map<String, Map<String, Object>> nativeActivations,
            Config config
    ) {
        List<Map<String, Object>> services = new ArrayList<>();
        for (Map.Entry<String, Map<String, Object>> entry : nativeActivations.entrySet()) {
            if (!nativeActivationLoaded(entry.getValue())) {
                continue;
            }
            Map<String, Object> serviceBridge = object(entry.getValue().get("serviceBridge"));
            List<Map<String, Object>> bridgedServices = objectList(serviceBridge.get("services"));
            if (!bridgedServices.isEmpty()) {
                for (Map<String, Object> bridgedService : bridgedServices) {
                    services.add(safeNativeService(entry.getKey(), bridgedService, config));
                }
                continue;
            }
            Map<String, Object> registryBridge = object(entry.getValue().get("registryBridge"));
            for (Map<String, Object> registration : objectList(registryBridge.get("registrations"))) {
                if (!"service".equals(String.valueOf(registration.getOrDefault("registry", "")))) {
                    continue;
                }
                Map<String, Object> service = new LinkedHashMap<>();
                service.put("moduleId", entry.getKey());
                service.put("serviceId", String.valueOf(registration.getOrDefault("id", "")));
                service.put("role", "registry_service_contract");
                service.put("summary", String.valueOf(registration.getOrDefault("summary", "")));
                service.put("approved", true);
                service.put("started", true);
                service.put("state", "started_as_adaptercore_native_service_handle");
                service.put("runtimeStateInitialized", true);
                service.put("minecraftRuntimeAccessed", false);
                service.put("serviceCodeExecuted", false);
                service.put("features", List.of());
                service.put("surfaces", inferredAgent3Surfaces(entry.getKey(), config));
                services.add(service);
            }
        }
        if (services.stream().noneMatch(service -> stringList(service.get("surfaces")).contains("screen_safe_ui"))
                && nativeActivationLoaded(object(nativeActivations.get("echoscreencore")))) {
            Map<String, Object> service = new LinkedHashMap<>();
            service.put("moduleId", "echoscreencore");
            service.put("serviceId", "echoscreencore:screen_safe_ui");
            service.put("role", "native_screen_safe_ui_host");
            service.put("summary", "Native UI activation provides the Echo module browser, Echo content browser, search-backed creative visibility, and safe vanilla fallback host without NeoForge screen bridges.");
            service.put("approved", true);
            service.put("started", true);
            service.put("state", "started_as_adaptercore_native_service_handle");
            service.put("runtimeStateInitialized", true);
            service.put("minecraftRuntimeAccessed", false);
            service.put("serviceCodeExecuted", false);
            service.put("features", List.of("echo_module_browser_screen", "echo_content_browser_screen", "search_backed_creative_visibility", "safe_vanilla_fallback"));
            service.put("surfaces", List.of("screen_safe_ui"));
            services.add(service);
        }
        services.sort(Comparator.comparing(service -> String.valueOf(service.get("serviceId"))));
        long runtimeInitializedCount = services.stream()
                .filter(service -> Boolean.TRUE.equals(service.get("runtimeStateInitialized")))
                .count();
        long serviceCodeExecutedCount = services.stream()
                .filter(service -> Boolean.TRUE.equals(service.get("serviceCodeExecuted")))
                .count();
        boolean minecraftRuntimeAccessed = services.stream()
                .anyMatch(service -> Boolean.TRUE.equals(service.get("minecraftRuntimeAccessed")));
        List<Map<String, Object>> agent3SurfaceCoverage = agent3SurfaceCoverage(services, config);
        long readyAgent3Surfaces = agent3SurfaceCoverage.stream()
                .filter(surface -> Boolean.TRUE.equals(surface.get("covered")))
                .count();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("applied", !services.isEmpty());
        data.put("bridge", "adaptercore.native_service");
        data.put("agent3GameplaySurfaceCoverage", agent3SurfaceCoverage);
        data.put("agent3GameplaySurfaceCoverageReady",
                !agent3SurfaceCoverage.isEmpty() && readyAgent3Surfaces == agent3SurfaceCoverage.size());
        data.put("agent3ReadySurfaceCount", readyAgent3Surfaces);
        data.put("agent3RequiredSurfaceCount", agent3SurfaceCoverage.size());
        data.put("approvedServiceCount", services.size());
        data.put("startedServiceCount", services.size());
        data.put("runtimeInitializedServiceCount", runtimeInitializedCount);
        data.put("serviceHandleStartedCount", services.size());
        data.put("serviceCodeExecutedCount", serviceCodeExecutedCount);
        data.put("serviceHandlesStarted", !services.isEmpty());
        data.put("serviceCodeExecuted", serviceCodeExecutedCount > 0);
        data.put("minecraftRuntimeAccessed", minecraftRuntimeAccessed);
        data.put("serviceExecutionMode", serviceCodeExecutedCount > 0
                ? "adaptercore_native_service_code"
                : runtimeInitializedCount > 0
                ? "adaptercore_native_service_handles"
                : "inert_native_handles");
        data.put("services", services);
        data.put("summary", services.isEmpty()
                ? "No approved AdapterCore native service registrations were available."
                : serviceCodeExecutedCount > 0
                ? "AdapterCore native service bridge executed " + serviceCodeExecutedCount
                + " approved service code path(s)."
                : "AdapterCore native service bridge started approved "
                + config.profile().nativeGameplayDisplayName()
                + " service handles without executing Minecraft-bound service code.");
        return data;
    }

    private static Map<String, Object> safeLifecyclePhase(Map<String, Object> phase) {
        Map<String, Object> safe = new LinkedHashMap<>();
        safe.put("id", String.valueOf(phase.getOrDefault("id", "")));
        safe.put("summary", String.valueOf(phase.getOrDefault("summary", "")));
        safe.put("planned", true);
        safe.put("safeHookRun", Boolean.TRUE.equals(phase.get("safeHookRun")));
        safe.put("phaseCodeExecuted", Boolean.TRUE.equals(phase.get("phaseCodeExecuted"))
                || Boolean.TRUE.equals(phase.get("safeHookRun")));
        safe.put("status", String.valueOf(phase.getOrDefault("status", "")));
        safe.put("hookExecutionMode", String.valueOf(phase.getOrDefault(
                "hookExecutionMode",
                phase.getOrDefault("executionMode", "adaptercore_data_only_lifecycle_hook")
        )));
        safe.put("gameplayHandlerExecuted", false);
        safe.put("failed", Boolean.TRUE.equals(phase.get("failed")));
        safe.put("failures", stringList(phase.get("failures")));
        return safe;
    }

    private static Map<String, Object> safeEventHook(Map<String, Object> hook) {
        Map<String, Object> safe = new LinkedHashMap<>();
        safe.put("event", String.valueOf(hook.getOrDefault("event", "")));
        safe.put("handler", String.valueOf(hook.getOrDefault("handler", "")));
        safe.put("summary", String.valueOf(hook.getOrDefault("summary", "")));
        safe.put("attached", true);
        safe.put("handlerSubscribed", Boolean.TRUE.equals(hook.get("handlerSubscribed"))
                || Boolean.TRUE.equals(hook.get("nativeEventHostSubscribed"))
                || Boolean.TRUE.equals(hook.get("subscribed")));
        safe.put("runtimeEventPublished", Boolean.TRUE.equals(hook.get("runtimeEventPublished")));
        safe.put("safeHookRun", Boolean.TRUE.equals(hook.get("safeHookRun"))
                || Boolean.TRUE.equals(hook.get("handlerExecuted")));
        safe.put("hookExecutionMode", String.valueOf(hook.getOrDefault(
                "hookExecutionMode",
                hook.getOrDefault("executionMode", "native_event_host_subscription")
        )));
        safe.put("handlerExecuted", Boolean.TRUE.equals(hook.get("handlerExecuted")));
        safe.put("gameplayHandlerExecuted", false);
        return safe;
    }

    private static int calledCallbackCount(List<Map<String, Object>> callbacks) {
        int count = 0;
        for (Map<String, Object> callback : callbacks) {
            if (Boolean.TRUE.equals(callback.get("called"))) {
                count++;
            }
        }
        return count;
    }

    private static int countTrue(List<Map<String, Object>> records, String key) {
        int count = 0;
        for (Map<String, Object> record : records) {
            if (Boolean.TRUE.equals(record.get(key))) {
                count++;
            }
        }
        return count;
    }

    private static int nativePublishedEventHandlerCount(Map<String, Object> lifecycleEventHost) {
        int count = 0;
        for (Map<String, Object> event : objectList(lifecycleEventHost.get("publishedEvents"))) {
            int handlerCount = integer(event.get("handlerCount"));
            if (handlerCount > 0) {
                count += handlerCount;
            } else if (Boolean.TRUE.equals(event.get("handlerExecuted"))) {
                count++;
            }
        }
        return count;
    }

    private static Map<String, Object> safeNativeService(
            String moduleId,
            Map<String, Object> source,
            Config config
    ) {
        Map<String, Object> service = new LinkedHashMap<>();
        service.put("moduleId", moduleId);
        service.put("serviceId", String.valueOf(source.getOrDefault("id", "")));
        service.put("role", String.valueOf(source.getOrDefault("role", "")));
        service.put("summary", String.valueOf(source.getOrDefault("summary", "")));
        service.put("approved", Boolean.TRUE.equals(source.get("approved")));
        service.put("started", Boolean.TRUE.equals(source.get("started")));
        service.put("state", String.valueOf(source.getOrDefault("state", "started_as_adaptercore_native_service_handle")));
        service.put("runtimeStateInitialized",
                Boolean.TRUE.equals(source.get("runtimeStateInitialized")) || Boolean.TRUE.equals(source.get("started")));
        service.put("minecraftRuntimeAccessed", Boolean.TRUE.equals(source.get("minecraftRuntimeAccessed")));
        service.put("serviceCodeExecuted", Boolean.TRUE.equals(source.get("serviceCodeExecuted")));
        service.put("features", stringList(source.get("features")));
        List<String> surfaces = stringList(source.get("surfaces"));
        service.put("surfaces", normalizeAgent3Surfaces(surfaces.isEmpty() ? inferredAgent3Surfaces(moduleId, config) : surfaces));
        return service;
    }

    private static List<String> normalizeAgent3Surfaces(List<String> surfaces) {
        Set<String> normalized = new java.util.TreeSet<>(surfaces);
        if (normalized.contains("ui_hud_screen_safe")
                || normalized.contains("screen_safe_onboarding")
                || normalized.contains("hud.mission_tracker")
                || normalized.contains("terminal.card")) {
            normalized.add("screen_safe_ui");
        }
        return List.copyOf(normalized);
    }

    private static List<Map<String, Object>> agent3SurfaceCoverage(
            List<Map<String, Object>> services,
            Config config
    ) {
        List<String> requiredSurfaces = config.profile().requiredNativeServiceSurfaces();
        List<Map<String, Object>> coverage = new ArrayList<>();
        for (String surface : requiredSurfaces) {
            List<String> surfaceServices = services.stream()
                    .filter(service -> Boolean.TRUE.equals(service.get("runtimeStateInitialized")))
                    .filter(service -> stringList(service.get("surfaces")).contains(surface))
                    .map(service -> String.valueOf(service.get("serviceId")))
                    .sorted()
                    .toList();
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("surface", surface);
            entry.put("covered", !surfaceServices.isEmpty());
            entry.put("runtimeInitializedServiceCount", surfaceServices.size());
            entry.put("services", surfaceServices);
            coverage.add(entry);
        }
        return coverage;
    }

    private static List<String> inferredAgent3Surfaces(String moduleId, Config config) {
        return config.profile().nativeServiceSurfaceModules().getOrDefault(moduleId, List.of());
    }

    private static boolean nativeActivationLoaded(Map<String, Object> activation) {
        return activation != null
                && Boolean.TRUE.equals(activation.get("activated"))
                && Boolean.TRUE.equals(activation.get("nativeAdapterCodeExecuted"))
                && !String.valueOf(activation.getOrDefault("entrypoint", "")).isBlank()
                && !String.valueOf(activation.getOrDefault("loadedClassName", "")).isBlank();
    }

    private static boolean nativeActivationClassLoaded(Map<String, Object> activation) {
        return activation != null
                && Boolean.TRUE.equals(activation.get("nativeAdapterCodeExecuted"))
                && !String.valueOf(activation.getOrDefault("entrypoint", "")).isBlank()
                && !String.valueOf(activation.getOrDefault("loadedClassName", "")).isBlank();
    }

    private static int integer(Object value) {
        return value instanceof Number number ? number.intValue() : 0;
    }

    private static String normalizeRegistry(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase().replace('-', '_').replace('.', '_');
        return switch (normalized) {
            case "items" -> "item";
            case "blocks" -> "block";
            case "entities" -> "entity";
            case "blockentity", "blockentities", "block_entity", "block_entities" -> "block_entity";
            case "menus" -> "menu";
            case "sounds" -> "sound";
            case "particles", "particle_profile", "particle_profiles" -> "particle";
            case "effects", "mob_effect", "mob_effects", "mobeffect", "mobeffects" -> "effect";
            case "commands" -> "command";
            case "datacomponent", "datacomponents", "data_component", "data_components" -> "data_component";
            case "recipes" -> "recipe";
            case "creativegroup", "creativegroups", "creative_group", "creative_groups",
                    "creative_tab", "creative_tabs" -> "creative_tab";
            case "biomes" -> "biome";
            case "configured_feature", "configured_features", "placed_feature", "placed_features",
                    "world_generator", "world_generators", "worldgens" -> "worldgen";
            case "asset", "assets", "clientasset", "clientassets", "client_asset", "client_assets" -> "client_asset";
            default -> normalized;
        };
    }

    private static boolean isClientUiRegistry(String registry) {
        return switch (registry) {
            case "ui_surface", "ui_overlay", "hud", "hud_widget", "hud_layout",
                    "screen", "screen_surface", "client_overlay", "loading_screen", "main_menu", "world_setup",
                    "terminal", "index", "lens", "holomap", "holo_map", "minimap", "theme" -> true;
            default -> false;
        };
    }

    private static boolean isResourceRegistry(String registry) {
        return switch (registry) {
            case "resource", "resources", "resource_profile", "resource_pack", "resourcepack",
                    "data", "data_pack", "datapack", "recipe", "recipes", "loot", "loot_table",
                    "loot_tables", "loottables", "tag", "tags", "sound", "sounds", "structure",
                    "structures", "worldgen", "world_generator", "world_preset", "world_template",
                    "asset", "assets", "ui_screen", "ui_screens", "theme", "themes", "theme_tokens",
                    "ui_skin", "ui_skins", "render_profile", "render_profiles", "asset_kit",
                    "asset_kits", "block_palette", "block_palettes", "screen_markup", "screen_layout",
                    "screen_layouts", "style", "styles", "data_provider", "data_providers" -> true;
            default -> registry.endsWith("_resource")
                    || registry.endsWith("_resources")
                    || registry.endsWith("_data");
        };
    }

    private static String resourceType(String registry) {
        return switch (registry) {
            case "resourcepack" -> "resource_pack";
            case "datapack" -> "data_pack";
            case "loot", "loottables" -> "loot_table";
            case "recipes" -> "recipe";
            case "tags" -> "tag";
            case "sounds" -> "sound";
            case "structures" -> "structure";
            case "assets" -> "asset";
            case "themes" -> "theme";
            case "ui_skins" -> "ui_skin";
            case "render_profiles" -> "render_profile";
            case "asset_kits" -> "asset_kit";
            case "block_palettes" -> "block_palette";
            case "screen_layouts" -> "screen_layout";
            case "styles" -> "style";
            case "data_providers" -> "data_provider";
            default -> registry;
        };
    }

    private static boolean isWorldStartupResource(String registry) {
        return switch (registry) {
            case "data_pack", "datapack", "worldgen", "world_generator", "world_preset",
                    "world_template", "structure", "structures", "tag", "tags" -> true;
            default -> false;
        };
    }

    private static String surfaceType(String registry) {
        return switch (registry) {
            case "ui_surface" -> "ui_surface";
            case "ui_overlay" -> "ui_overlay";
            case "client_overlay" -> "client_overlay";
            case "hud", "hud_widget", "hud_layout" -> registry;
            case "screen", "screen_surface" -> registry;
            case "loading_screen" -> "loading_screen";
            case "main_menu" -> "main_menu";
            case "world_setup", "worldsetup", "world_creation", "worldcreation" -> "world_setup";
            case "terminal" -> "terminal";
            case "index" -> "index";
            case "lens" -> "lens";
            case "holomap", "holo_map", "minimap" -> "holomap";
            case "theme" -> "theme";
            default -> "screen_surface";
        };
    }

    private static String normalizeContentId(String value, String moduleId) {
        String text = value == null ? "" : value.trim().toLowerCase();
        if (text.isBlank() || text.contains(":")) {
            return text;
        }
        int separator = text.indexOf('.');
        if (separator > 0 && separator + 1 < text.length()) {
            return text.substring(0, separator) + ":" + text.substring(separator + 1).replace('.', '_');
        }
        String namespace = moduleId == null ? "" : moduleId.trim().toLowerCase();
        return namespace.isBlank() ? text.replace('.', '_') : namespace + ":" + text.replace('.', '_');
    }

    private static Map<String, Object> object(Object value) {
        if (!(value instanceof Map<?, ?> map)) {
            return Map.of();
        }
        Map<String, Object> object = new LinkedHashMap<>();
        map.forEach((key, item) -> object.put(String.valueOf(key), item));
        return object;
    }

    private static List<Map<String, Object>> objectList(Object value) {
        if (!(value instanceof Iterable<?> iterable)) {
            return List.of();
        }
        List<Map<String, Object>> list = new ArrayList<>();
        for (Object item : iterable) {
            if (item instanceof Map<?, ?> map) {
                Map<String, Object> object = new LinkedHashMap<>();
                map.forEach((key, child) -> object.put(String.valueOf(key), child));
                list.add(object);
            }
        }
        return list;
    }

    private static List<String> stringList(Object value) {
        if (!(value instanceof Iterable<?> iterable)) {
            return List.of();
        }
        List<String> list = new ArrayList<>();
        for (Object item : iterable) {
            if (item != null) {
                list.add(String.valueOf(item));
            }
        }
        return list;
    }

    public static final class Config {
        private final EchoNativeBootstrapProductProfile profile;

        public Config(EchoNativeBootstrapProductProfile profile) {
            this.profile = profile;
        }

        EchoNativeBootstrapProductProfile profile() {
            return profile;
        }
    }
}
