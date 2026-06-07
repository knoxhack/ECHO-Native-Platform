package dev.echo.nativeplatform.loader;

import dev.echo.nativeplatform.contracts.EchoNativeAddonDescriptor;
import dev.echo.nativeplatform.contracts.EchoNativeModuleLoadResult;
import dev.echo.nativeplatform.contracts.EchoNativeRegisteredService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class NativeLoaderActivationReports {
    public static final String SERVICE_ID = "echo.native.activation_reports";

    private NativeLoaderActivationReports() {
    }

    public static Map<String, Object> activationReport(
            String packId,
            EchoNativeModuleLoadResult result,
            NativeLoaderLifecycleEventHost lifecycleEventHost,
            List<String> requiredLifecycleCallbacks
    ) {
        List<String> requiredCallbacks = requiredLifecycleCallbacks == null
                ? List.of()
                : List.copyOf(requiredLifecycleCallbacks);
        Map<String, Object> report = new LinkedHashMap<>(EchoNativeModuleLoader.toReport(result));
        Map<String, Object> lifecycleEventHostReport = lifecycleEventHost == null
                ? Map.of()
                : lifecycleEventHost.toReport();
        Map<String, Object> lifecycleBridge = lifecycleBridge(result);
        Map<String, Object> eventBridge = eventBridge(result, lifecycleEventHostReport);
        Map<String, Object> serviceBridge = serviceBridge(result);
        Map<String, Object> registryBridge = registryBridge(result);
        List<Map<String, Object>> registeredServices = result.registeredServices().stream()
                .filter(service -> !service.serviceId().startsWith("content."))
                .map(NativeLoaderActivationReports::registeredServiceRecord)
                .toList();
        List<Map<String, Object>> registeredContent = result.registeredServices().stream()
                .filter(service -> service.serviceId().startsWith("content."))
                .map(NativeLoaderActivationReports::registeredServiceRecord)
                .toList();
        report.put("packId", packId);
        report.put("nativeActivationReportServiceId", SERVICE_ID);
        report.put("attempted", true);
        report.put("nativeLoaderLifecycleAttempted", true);
        report.put("nativeLoaderLifecycleFallback", false);
        report.put("loadedClassLoader", result.loadedClassLoaderName());
        report.put("activated", result.loaded() && result.registered());
        report.put("nativeAdapterCodeExecuted", result.loaded());
        report.put("addonServiceCodeExecuted", result.registered());
        report.put("serviceCodeExecuted", serviceCodeExecuted(result));
        report.put("registryInjected", false);
        report.put("registryMutated", Boolean.TRUE.equals(registryBridge.get("registryMutated")));
        report.put("transformsPerformed", false);
        report.put("activationStage", result.registered()
                ? "native_loader_lifecycle_registered"
                : result.loaded()
                ? "native_loader_lifecycle_loaded_without_services"
                : "native_loader_lifecycle_" + result.status().name().toLowerCase(Locale.ROOT));
        report.put("nativeLifecycleDispatch", lifecycleDispatch(result, requiredCallbacks));
        report.put("lifecycleBridge", lifecycleBridge);
        report.put("eventBridge", eventBridge);
        report.put("serviceBridge", serviceBridge);
        report.put("registryBridge", registryBridge);
        report.put("nativeLoaderLifecycleEventHost", lifecycleEventHostReport);
        report.put("nativeLifecycleHostEventCount", integer(lifecycleEventHostReport.get("lifecycleEventCount")));
        report.put("nativeEventHostSubscriptionCount", integer(lifecycleEventHostReport.get("eventSubscriptionCount")));
        report.put("nativeEventHostPublishedEventCount", integer(lifecycleEventHostReport.get("publishedEventCount")));
        report.put("nativeEventHostPublishedHandlerCount", publishedEventHandlerCount(lifecycleEventHostReport));
        report.put("nativeEventHostHandlerExecuted", publishedEventHandlerCount(lifecycleEventHostReport) > 0);
        report.put("registeredServices", registeredServices);
        report.put("registeredContent", registeredContent);
        report.put("registeredServiceCount", registeredServices.size());
        report.put("registeredContentCount", registeredContent.size());
        report.put("registeredFeatureContracts", featureContracts(result));
        report.put("adapterDomains", adapterDomains(result));
        report.put("runtimeTargets", runtimeTargets(result));
        report.put("nativeLoadedModuleState", EchoNativeLoadedModuleState.from(result));
        report.put("summary", result.registered()
                ? "Native Loader discovered the descriptor, loaded the module class, constructed the entrypoint, and registered services/content."
                : result.loaded()
                ? "Native Loader loaded the module class, but the module registered no services/content."
                : "Native Loader attempted the module lifecycle and did not load/register the module.");
        return Map.copyOf(report);
    }

    public static Map<String, Object> failureReport(
            String packId,
            EchoNativeAddonDescriptor descriptor,
            Throwable exception
    ) {
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("packId", packId);
        report.put("nativeActivationReportServiceId", SERVICE_ID);
        report.put("moduleId", descriptor.id());
        report.put("entrypoint", text(descriptor.access().get("nativeEntrypoint")));
        report.put("attempted", true);
        report.put("activated", false);
        report.put("nativeLoaderLifecycleAttempted", true);
        report.put("nativeLoaderLifecycleFallback", false);
        report.put("nativeAdapterCodeExecuted", false);
        report.put("addonServiceCodeExecuted", false);
        report.put("serviceCodeExecuted", false);
        report.put("loadedClassName", "");
        report.put("loadedClassLoader", "");
        report.put("activationStage", "native_loader_lifecycle_failed");
        report.put("failureKind", exception.getClass().getSimpleName());
        report.put("diagnostics", List.of(exception.getMessage() == null ? exception.getClass().getName() : exception.getMessage()));
        report.put("registryInjected", false);
        report.put("registryMutated", false);
        report.put("transformsPerformed", false);
        report.put("summary", "Native Loader lifecycle failed before module services/content could be registered.");
        return Map.copyOf(report);
    }

    public static Map<String, Object> unloadedEntrypointReport(
            String packId,
            String moduleId,
            String entrypoint
    ) {
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("packId", packId);
        report.put("nativeActivationReportServiceId", SERVICE_ID);
        report.put("moduleId", moduleId);
        report.put("entrypoint", entrypoint);
        report.put("attempted", true);
        report.put("activated", false);
        report.put("nativeLoaderLifecycleAttempted", false);
        report.put("nativeLoaderLifecycleFallback", false);
        report.put("nativeLoaderLifecycleFallbackReason", "release_loader_requires_descriptor_backed_lifecycle");
        report.put("nativeAdapterCodeExecuted", false);
        report.put("addonServiceCodeExecuted", false);
        report.put("serviceCodeExecuted", false);
        report.put("loadedClassName", "");
        report.put("loadedClassLoader", "");
        report.put("activationStage", "native_loader_descriptor_not_loaded");
        report.put("failureKind", "NativeLoaderDescriptorMissing");
        report.put("diagnostics", List.of("Native entrypoint '" + entrypoint
                + "' was not activated because release loading requires direct phase entrypoints."));
        report.put("registryInjected", false);
        report.put("registryMutated", false);
        report.put("transformsPerformed", false);
        report.put("summary", "Native Loader release mode requires descriptor-backed EchoNativeModuleEntrypoint loading.");
        return Map.copyOf(report);
    }

    public static String moduleServiceKey(String moduleId, String serviceId) {
        return String.valueOf(moduleId) + "::" + String.valueOf(serviceId);
    }

    private static Map<String, Object> registeredServiceRecord(EchoNativeRegisteredService service) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("moduleId", service.moduleId());
        item.put("serviceId", service.serviceId());
        item.put("moduleServiceKey", moduleServiceKey(service.moduleId(), service.serviceId()));
        item.put("implementationClass", service.implementationClass());
        item.put("surfaces", service.surfaces());
        return Map.copyOf(item);
    }

    private static Map<String, Object> lifecycleDispatch(
            EchoNativeModuleLoadResult result,
            List<String> requiredCallbacks
    ) {
        List<Map<String, Object>> callbacks = requiredCallbacks.stream()
                .map(callback -> {
                    boolean called = lifecycleCallbackExecuted(result, callback);
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("callback", callback);
                    item.put("called", called);
                    item.put("implementedByEntrypoint", called);
                    item.put("dispatchMode", called
                            ? "echo_native_module_lifecycle_callback_mutation"
                            : "not_dispatched_by_current_runtime_phase");
                    item.put("summary", called
                            ? "Native Loader runtime invoked " + callback + " and recorded a lifecycle mutation."
                            : "Native Loader runtime has not invoked " + callback + " for this activation.");
                    return Map.copyOf(item);
                })
                .toList();
        long calledCallbackCount = callbacks.stream()
                .filter(callback -> Boolean.TRUE.equals(callback.get("called")))
                .count();
        Map<String, Object> dispatch = new LinkedHashMap<>();
        dispatch.put("callbacks", callbacks);
        dispatch.put("calledCallbackCount", calledCallbackCount);
        dispatch.put("requiredCallbackCount", requiredCallbacks.size());
        dispatch.put("allRequiredCallbacksCalled",
                !requiredCallbacks.isEmpty() && calledCallbackCount == requiredCallbacks.size());
        dispatch.put("dispatchMode", "echo_native_module_lifecycle_callback_mutations");
        return Map.copyOf(dispatch);
    }

    private static Map<String, Object> lifecycleBridge(EchoNativeModuleLoadResult result) {
        List<Map<String, Object>> phases = result.lifecyclePhaseHistory().stream()
                .map(record -> {
                    boolean phaseCodeExecuted = runtimeLifecyclePhaseAttempted(record.phase().name());
                    boolean phaseSucceeded = phaseCodeExecuted && !record.failed();
                    Map<String, Object> phase = new LinkedHashMap<>();
                    phase.put("id", record.phase().name().toLowerCase(Locale.ROOT));
                    phase.put("status", record.status().name());
                    phase.put("summary", record.detail());
                    phase.put("planned", true);
                    phase.put("safeHookRun", phaseSucceeded);
                    phase.put("phaseCodeExecuted", phaseCodeExecuted);
                    phase.put("hookExecutionMode", phaseCodeExecuted
                            ? "echo_native_module_entrypoint_phase"
                            : "echo_native_module_loader_preflight");
                    phase.put("gameplayHandlerExecuted", false);
                    phase.put("failed", record.failed());
                    phase.put("failures", record.failures());
                    return Map.copyOf(phase);
                })
                .toList();
        long executedPhaseCount = phases.stream()
                .filter(phase -> Boolean.TRUE.equals(phase.get("phaseCodeExecuted")))
                .count();
        long safePhaseCount = phases.stream()
                .filter(phase -> Boolean.TRUE.equals(phase.get("safeHookRun")))
                .count();
        long callbackCount = lifecycleCallbackMutationCount(result);
        boolean lifecycleCodeExecuted = executedPhaseCount > 0 || callbackCount > 0;
        Map<String, Object> bridge = new LinkedHashMap<>();
        bridge.put("applied", !phases.isEmpty());
        bridge.put("bridge", "echo_native.module_lifecycle");
        bridge.put("moduleCount", 1);
        bridge.put("phaseCount", phases.size());
        bridge.put("executedPhaseCount", executedPhaseCount);
        bridge.put("phases", phases);
        bridge.put("safeLifecycleHooksRun", safePhaseCount > 0 || callbackCount > 0);
        bridge.put("safeLifecycleHookRunCount", safePhaseCount + callbackCount);
        bridge.put("lifecycleCallbackMutationCount", callbackCount);
        bridge.put("hookExecutionMode", lifecycleCodeExecuted
                ? "echo_native_module_entrypoint_phase_and_callback_mutations"
                : "none");
        bridge.put("lifecycleCodeExecuted", lifecycleCodeExecuted);
        bridge.put("addonServiceCodeExecuted", result.registered());
        bridge.put("summary", result.registered()
                ? "Native Loader lifecycle executed runtime entrypoint phases and registered module services/content."
                : lifecycleCodeExecuted
                ? "Native Loader lifecycle executed runtime entrypoint phase(s) without reaching registered state."
                : "Native Loader lifecycle did not reach an executable entrypoint phase.");
        return Map.copyOf(bridge);
    }

    private static Map<String, Object> eventBridge(
            EchoNativeModuleLoadResult result,
            Map<String, Object> lifecycleEventHost
    ) {
        int nativeEventHostSubscriptionCount = integer(lifecycleEventHost.get("eventSubscriptionCount"));
        int nativePublishedEventCount = integer(lifecycleEventHost.get("publishedEventCount"));
        int nativePublishedEventHandlerCount = publishedEventHandlerCount(lifecycleEventHost);
        boolean nativeEventHostHandlerExecuted = nativePublishedEventHandlerCount > 0
                || objectList(lifecycleEventHost.get("publishedEvents")).stream()
                .anyMatch(event -> Boolean.TRUE.equals(event.get("handlerExecuted")));
        List<Map<String, Object>> hooks = result.registeredServices().stream()
                .filter(service -> service.serviceId().startsWith("event."))
                .map(service -> {
                    String eventId = firstSurfaceAfter(service, "events", service.serviceId());
                    String handlerId = service.serviceId();
                    boolean hookHandlerExecuted = publishedEventHandled(lifecycleEventHost, eventId);
                    Map<String, Object> hook = new LinkedHashMap<>();
                    hook.put("event", eventId);
                    hook.put("handler", handlerId);
                    hook.put("summary", service.implementationClass());
                    hook.put("attached", true);
                    hook.put("handlerSubscribed", true);
                    hook.put("runtimeEventPublished", publishedEventSeen(lifecycleEventHost, eventId));
                    hook.put("safeHookRun", hookHandlerExecuted);
                    hook.put("hookExecutionMode", hookHandlerExecuted
                            ? "echo_native_event_host_published_handler"
                            : "echo_native_registered_event_service");
                    hook.put("handlerExecuted", hookHandlerExecuted);
                    hook.put("gameplayHandlerExecuted", false);
                    return Map.copyOf(hook);
                })
                .toList();
        boolean applied = !hooks.isEmpty() || nativeEventHostSubscriptionCount > 0 || nativePublishedEventCount > 0;
        Map<String, Object> bridge = new LinkedHashMap<>();
        bridge.put("applied", applied);
        bridge.put("bridge", "echo_native.event_services");
        bridge.put("moduleCount", applied ? 1 : 0);
        bridge.put("hookCount", hooks.size());
        bridge.put("hooks", hooks);
        bridge.put("safeEventHookAttachedCount", hooks.size());
        bridge.put("safeEventHooksRun", nativeEventHostHandlerExecuted);
        bridge.put("safeEventHookRunCount", nativePublishedEventHandlerCount);
        bridge.put("handlerSubscribed", !hooks.isEmpty() || nativeEventHostSubscriptionCount > 0);
        bridge.put("runtimeEventPublished", nativePublishedEventCount > 0);
        bridge.put("nativeEventHostAttached", !lifecycleEventHost.isEmpty());
        bridge.put("nativeEventHostSubscriptionCount", nativeEventHostSubscriptionCount);
        bridge.put("nativePublishedEventCount", nativePublishedEventCount);
        bridge.put("nativePublishedEventHandlerCount", nativePublishedEventHandlerCount);
        bridge.put("nativePublishedEvents", objectList(lifecycleEventHost.get("publishedEvents")));
        bridge.put("handlerExecuted", nativeEventHostHandlerExecuted);
        bridge.put("gameplayHandlerExecuted", false);
        bridge.put("addonServiceCodeExecuted", !hooks.isEmpty());
        bridge.put("summary", nativeEventHostHandlerExecuted
                ? "Native Loader event host published " + nativePublishedEventCount
                + " event(s) and executed " + nativePublishedEventHandlerCount + " subscribed handler(s)."
                : applied
                ? "Native Loader event host attached/subscribed runtime events; no subscribed handler has executed yet."
                : "No Native Loader event services or runtime event host evidence were available.");
        return Map.copyOf(bridge);
    }

    private static Map<String, Object> serviceBridge(EchoNativeModuleLoadResult result) {
        boolean serviceCodeExecuted = serviceCodeExecuted(result);
        List<Map<String, Object>> services = result.registeredServices().stream()
                .filter(service -> !service.serviceId().startsWith("content."))
                .map(service -> service(service, serviceCodeExecuted))
                .toList();
        Map<String, Object> bridge = new LinkedHashMap<>();
        bridge.put("applied", !services.isEmpty());
        bridge.put("bridge", "echo_native.service_registry");
        bridge.put("approvedServiceCount", services.size());
        bridge.put("startedServiceCount", services.size());
        bridge.put("runtimeInitializedServiceCount", services.size());
        bridge.put("serviceCodeExecuted", serviceCodeExecuted);
        bridge.put("serviceRegistryInitialized", !services.isEmpty());
        bridge.put("minecraftRuntimeAccessed", false);
        bridge.put("serviceExecutionMode", services.isEmpty()
                ? "none"
                : serviceCodeExecuted
                ? "echo_native_module_register_services_executed"
                : "echo_native_service_registry_handles");
        bridge.put("services", services);
        return Map.copyOf(bridge);
    }

    private static Map<String, Object> registryBridge(EchoNativeModuleLoadResult result) {
        List<Map<String, Object>> registryMutations = registryHostMutations(result);
        List<Map<String, Object>> registrations = result.registeredServices().stream()
                .filter(service -> service.serviceId().startsWith("content."))
                .map(service -> {
                    Map<String, String> registryAndId = contentRegistryAndId(service.serviceId());
                    String registry = registryAndId.getOrDefault("registry", "");
                    String id = registryAndId.getOrDefault("id", service.serviceId());
                    boolean nativeRegistryHostMutated = registryMutationMatched(registryMutations, registry, id);
                    Map<String, Object> registration = new LinkedHashMap<>();
                    registration.put("registry", registry);
                    registration.put("id", id);
                    registration.put("summary", service.implementationClass());
                    registration.put("moduleId", service.moduleId());
                    registration.put("status", nativeRegistryHostMutated
                            ? "MUTATED"
                            : "UNSUPPORTED_BY_NATIVE_REGISTRY_HOST");
                    registration.put("source", nativeRegistryHostMutated
                            ? "echo_native_registry_host"
                            : "echo_native_service_registry");
                    registration.put("nativeRegistryHostRegistered", nativeRegistryHostMutated);
                    registration.put("nativeRegistryHostStatus", nativeRegistryHostMutated ? "MUTATED" : "UNSUPPORTED");
                    return Map.copyOf(registration);
                })
                .toList();
        long nativeRegistryHostMutationCount = registrations.stream()
                .filter(registration -> Boolean.TRUE.equals(registration.get("nativeRegistryHostRegistered")))
                .count();
        Map<String, Object> bridge = new LinkedHashMap<>();
        bridge.put("applied", !registrations.isEmpty());
        bridge.put("registryMutated", nativeRegistryHostMutationCount > 0);
        bridge.put("nativeRegistryHostMutationCount", nativeRegistryHostMutationCount);
        bridge.put("registrationCount", registrations.size());
        bridge.put("registrations", registrations);
        bridge.put("registeredContentCount", registrations.size());
        bridge.put("executionMode", nativeRegistryHostMutationCount > 0
                ? "echo_native_registry_host"
                : "echo_native_service_registry_unsupported_by_host");
        return Map.copyOf(bridge);
    }

    private static List<String> featureContracts(EchoNativeModuleLoadResult result) {
        return result.registeredServices().stream()
                .map(EchoNativeRegisteredService::serviceId)
                .filter(serviceId -> serviceId.startsWith("feature."))
                .map(serviceId -> serviceId.substring("feature.".length()))
                .sorted()
                .toList();
    }

    private static List<String> adapterDomains(EchoNativeModuleLoadResult result) {
        return result.registeredServices().stream()
                .map(EchoNativeRegisteredService::serviceId)
                .filter(serviceId -> serviceId.startsWith("adapter_domain."))
                .map(serviceId -> serviceId.substring(serviceId.lastIndexOf('.') + 1))
                .sorted()
                .toList();
    }

    private static List<String> runtimeTargets(EchoNativeModuleLoadResult result) {
        return result.registeredServices().stream()
                .map(EchoNativeRegisteredService::serviceId)
                .filter(serviceId -> serviceId.startsWith("runtime_target."))
                .map(serviceId -> serviceId.substring(serviceId.lastIndexOf('.') + 1))
                .sorted()
                .toList();
    }

    private static Map<String, Object> service(EchoNativeRegisteredService service, boolean serviceCodeExecuted) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", service.serviceId());
        item.put("serviceId", service.serviceId());
        item.put("role", firstSurfaceAfter(service, "", "native_loader_service"));
        item.put("summary", service.implementationClass());
        item.put("approved", true);
        item.put("started", true);
        item.put("state", "started_in_echo_native_service_registry");
        item.put("runtimeStateInitialized", true);
        item.put("minecraftRuntimeAccessed", false);
        item.put("serviceCodeExecuted", serviceCodeExecuted);
        item.put("serviceRegistryInitialized", true);
        item.put("executionMode", serviceCodeExecuted
                ? "echo_native_module_service_mutation_executed"
                : "echo_native_service_registry_handle_started");
        item.put("features", List.of());
        item.put("surfaces", service.surfaces());
        return Map.copyOf(item);
    }

    private static boolean lifecycleCallbackExecuted(EchoNativeModuleLoadResult result, String callback) {
        String expected = text(callback);
        if (expected.isBlank()) {
            return false;
        }
        return result.mutations().stream()
                .filter(mutation -> "lifecycle".equals(String.valueOf(mutation.getOrDefault("surface", ""))))
                .filter(mutation -> String.valueOf(mutation.getOrDefault("action", ""))
                        .contains("lifecycle_callback_executed"))
                .anyMatch(mutation -> expected.equals(String.valueOf(mutation.getOrDefault("target", "")))
                        && "MUTATED".equals(String.valueOf(mutation.getOrDefault("status", ""))));
    }

    private static long lifecycleCallbackMutationCount(EchoNativeModuleLoadResult result) {
        return result.mutations().stream()
                .filter(mutation -> "lifecycle".equals(String.valueOf(mutation.getOrDefault("surface", ""))))
                .filter(mutation -> String.valueOf(mutation.getOrDefault("action", ""))
                        .contains("lifecycle_callback_executed"))
                .filter(mutation -> "MUTATED".equals(String.valueOf(mutation.getOrDefault("status", ""))))
                .count();
    }

    private static boolean runtimeLifecyclePhaseAttempted(String phase) {
        return switch (text(phase).toUpperCase(Locale.ROOT)) {
            case "CONSTRUCT",
                    "REGISTER_SERVICES",
                    "REGISTER_CONTENT",
                    "COMMON_SETUP",
                    "CLIENT_SETUP",
                    "SERVER_SETUP",
                    "READY" -> true;
            default -> false;
        };
    }

    private static List<Map<String, Object>> registryHostMutations(EchoNativeModuleLoadResult result) {
        List<Map<String, Object>> mutations = new ArrayList<>();
        for (Map<String, Object> mutation : result.mutations()) {
            if (!"registry".equals(String.valueOf(mutation.getOrDefault("surface", "")))) {
                continue;
            }
            String action = String.valueOf(mutation.getOrDefault("action", ""));
            if (!action.contains("native_registry_host_registered")) {
                continue;
            }
            if (!"MUTATED".equals(String.valueOf(mutation.getOrDefault("status", "")))) {
                continue;
            }
            mutations.add(Map.copyOf(mutation));
        }
        return List.copyOf(mutations);
    }

    private static boolean registryMutationMatched(
            List<Map<String, Object>> mutations,
            String registry,
            String id
    ) {
        String expected = normalizeRegistryTarget(registry, id);
        for (Map<String, Object> mutation : mutations) {
            String target = normalizeRegistryTarget("", String.valueOf(mutation.getOrDefault("target", "")));
            if (expected.equals(target)) {
                return true;
            }
        }
        return false;
    }

    private static String normalizeRegistryTarget(String registry, String id) {
        String safeRegistry = registry == null ? "" : registry.trim();
        String safeId = id == null ? "" : id.trim();
        if (safeRegistry.isBlank()) {
            int separator = safeId.indexOf(':');
            if (separator > 0) {
                safeRegistry = safeId.substring(0, separator);
                safeId = safeId.substring(separator + 1);
            }
        }
        return normalizeRegistryName(safeRegistry) + ":" + normalizeRegistryId(safeId);
    }

    private static String normalizeRegistryName(String registry) {
        String normalized = registry == null
                ? ""
                : registry.trim().toLowerCase(Locale.ROOT)
                .replace('-', '_')
                .replace('.', '_');
        return switch (normalized) {
            case "items" -> "item";
            case "blocks" -> "block";
            case "entities" -> "entity";
            case "blockentity", "blockentities", "block_entities" -> "block_entity";
            case "menus" -> "menu";
            case "sounds" -> "sound";
            case "particles" -> "particle";
            case "creativegroup", "creativegroups", "creative_group", "creative_groups",
                    "creative_tabs" -> "creative_tab";
            case "commands" -> "command";
            case "datacomponent", "datacomponents", "data_components" -> "data_component";
            default -> normalized;
        };
    }

    private static String normalizeRegistryId(String id) {
        String text = id == null ? "" : id.trim().toLowerCase(Locale.ROOT);
        StringBuilder normalized = new StringBuilder();
        boolean previousSeparator = false;
        for (int index = 0; index < text.length(); index++) {
            char ch = text.charAt(index);
            if ((ch >= 'a' && ch <= 'z') || (ch >= '0' && ch <= '9')) {
                normalized.append(ch);
                previousSeparator = false;
            } else if (!previousSeparator) {
                normalized.append('.');
                previousSeparator = true;
            }
        }
        while (normalized.length() > 0 && normalized.charAt(normalized.length() - 1) == '.') {
            normalized.deleteCharAt(normalized.length() - 1);
        }
        return normalized.toString();
    }

    private static boolean serviceCodeExecuted(EchoNativeModuleLoadResult result) {
        return result.mutations().stream()
                .anyMatch(mutation -> "service".equals(String.valueOf(mutation.getOrDefault("surface", "")))
                        && "MUTATED".equals(String.valueOf(mutation.getOrDefault("status", ""))));
    }

    private static boolean publishedEventSeen(Map<String, Object> lifecycleEventHost, String eventId) {
        String expected = normalizeEventId(eventId);
        for (Map<String, Object> event : objectList(lifecycleEventHost.get("publishedEvents"))) {
            if (expected.equals(normalizeEventId(String.valueOf(event.getOrDefault("eventId", ""))))) {
                return true;
            }
        }
        return false;
    }

    private static boolean publishedEventHandled(Map<String, Object> lifecycleEventHost, String eventId) {
        String expected = normalizeEventId(eventId);
        for (Map<String, Object> event : objectList(lifecycleEventHost.get("publishedEvents"))) {
            if (!expected.equals(normalizeEventId(String.valueOf(event.getOrDefault("eventId", ""))))) {
                continue;
            }
            for (Map<String, Object> handlerResult : objectList(event.get("handlerResults"))) {
                if (Boolean.TRUE.equals(handlerResult.get("handled"))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static int publishedEventHandlerCount(Map<String, Object> lifecycleEventHost) {
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

    private static String normalizeEventId(String eventId) {
        String text = eventId == null ? "" : eventId.trim().toLowerCase(Locale.ROOT);
        StringBuilder normalized = new StringBuilder();
        boolean previousSeparator = false;
        for (int index = 0; index < text.length(); index++) {
            char ch = text.charAt(index);
            if ((ch >= 'a' && ch <= 'z') || (ch >= '0' && ch <= '9')) {
                normalized.append(ch);
                previousSeparator = false;
            } else if (!previousSeparator) {
                normalized.append('.');
                previousSeparator = true;
            }
        }
        while (normalized.length() > 0 && normalized.charAt(normalized.length() - 1) == '.') {
            normalized.deleteCharAt(normalized.length() - 1);
        }
        return normalized.toString();
    }

    private static String firstSurfaceAfter(EchoNativeRegisteredService service, String ignored, String fallback) {
        for (String surface : service.surfaces()) {
            if (!surface.isBlank() && !surface.equals(ignored)) {
                return surface;
            }
        }
        return fallback;
    }

    private static Map<String, String> contentRegistryAndId(String serviceId) {
        String text = serviceId.startsWith("content.") ? serviceId.substring("content.".length()) : serviceId;
        int separator = text.indexOf('.');
        if (separator < 0) {
            return Map.of("registry", "content", "id", text);
        }
        return Map.of(
                "registry", text.substring(0, separator),
                "id", text.substring(separator + 1)
        );
    }

    private static String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static int integer(Object value) {
        return value instanceof Number number ? number.intValue() : 0;
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
}
