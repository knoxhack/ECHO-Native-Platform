package dev.echo.nativeplatform.contracts;

import java.nio.file.Files;
import java.nio.file.Path;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

public final class EchoNativeActivationSurfaceRegistrar {
    private static final String EVENT_HOST_SERVICE_ID = "echo_native.event_host";
    private static final String LIFECYCLE_HOST_SERVICE_ID = "echo_native.lifecycle_host";
    private static final String REGISTRY_HOST_SERVICE_ID = "echo.native.registry.host";
    private static final String RESOURCE_HOST_SERVICE_ID = "echo.native.resource_host";
    private static final String NETWORK_HOST_SERVICE_ID = "echo.native.network_host";
    private static final String CONFIG_HOST_SERVICE_ID = "echo.native.config_host";
    private static final String COMMAND_HOST_SERVICE_ID = "echo.native.command_host";
    private static final String TYPED_REGISTRY_SERVICE_ID = "echo.native.registry";
    private static final String TYPED_CAPABILITY_SERVICE_ID = "echo.native.capabilities";
    private static final List<String> ADAPTER_DOMAIN_HOST_SERVICE_IDS = List.of(
            RESOURCE_HOST_SERVICE_ID,
            NETWORK_HOST_SERVICE_ID,
            CONFIG_HOST_SERVICE_ID,
            COMMAND_HOST_SERVICE_ID
    );

    private EchoNativeActivationSurfaceRegistrar() {
    }

    public static void discoverActivationEntrypoint(EchoNativeModuleLoadContext context, Class<?> entrypointType) {
        context.attribute("nativeEntrypointBridge", "direct_native_activation_entrypoint");
        context.attribute("nativeEntrypointDelegateClass", className(entrypointType));
        context.attribute("nativeActivationEntrypoint", true);
        recordLifecycleCallback(context, "onModuleDiscovered");
    }

    public static Map<String, Object> activation(
            EchoNativeModuleLoadContext context,
            Supplier<Map<String, Object>> activationSupplier
    ) {
        Map<String, Object> existing = object(context.attributes().get("nativeActivation"));
        if (!existing.isEmpty()) {
            return existing;
        }
        Map<String, Object> activation = object(activationSupplier == null ? null : activationSupplier.get());
        context.attribute("nativeActivation", activation);
        context.attribute("nativeActivationStage", string(activation.get("activationStage")));
        context.attribute("nativeAdapterCodeExecuted", Boolean.TRUE.equals(activation.get("nativeAdapterCodeExecuted")));
        context.attribute("nativeRegistryMutationClaimed", Boolean.TRUE.equals(activation.get("registryMutated")));
        context.attribute("nativeTransformsPerformedClaimed", Boolean.TRUE.equals(activation.get("transformsPerformed")));
        context.attribute("nativeServiceCodeExecutedClaimed", Boolean.TRUE.equals(activation.get("serviceCodeExecuted")));
        return activation;
    }

    public static Map<String, String> bridgeContext(EchoNativeModuleLoadContext context) {
        Map<String, String> data = new LinkedHashMap<>();
        data.put("moduleId", context.descriptor().id());
        data.put("moduleName", context.descriptor().name());
        data.put("moduleVersion", context.descriptor().version());
        data.put("runtime", "echo_native");
        data.put("loader", "echo-native-loader");
        data.put("packId", string(context.attributes().getOrDefault("packId", "native-loader")));
        data.put("descriptorPath", context.descriptor().descriptorPath() == null
                ? ""
                : context.descriptor().descriptorPath().toString());
        String repoRoot = inferRepoRoot(context.descriptor().descriptorPath());
        if (!repoRoot.isBlank()) {
            data.put("repoRoot", repoRoot);
        }
        for (Map.Entry<String, Object> entry : context.attributes().entrySet()) {
            data.putIfAbsent(entry.getKey(), string(entry.getValue()));
        }
        return data;
    }

    public static void registerServices(
            EchoNativeModuleLoadContext context,
            Object entrypoint,
            Map<String, Object> activation,
            String moduleServiceSuffix,
            String lifecycleAction
    ) {
        recordLifecycleCallback(context, "onRegister");
        String serviceSuffix = normalized(moduleServiceSuffix);
        if (serviceSuffix.isBlank()) {
            serviceSuffix = "native.module.entrypoint";
        }
        String action = lifecycleAction == null || lifecycleAction.isBlank()
                ? "direct_native_module_entrypoint"
                : lifecycleAction;
        context.registerService(
                "module." + normalized(context.descriptor().id()) + "." + serviceSuffix,
                entrypoint,
                "lifecycle",
                "diagnostics",
                "adaptercore"
        );
        if (Boolean.TRUE.equals(activation.get("adapterCoreUsed"))
                || !list(activation.get("registeredFeatureContracts")).isEmpty()) {
            context.registerService(
                    "adaptercore." + normalized(context.descriptor().id()) + ".contract",
                    activation,
                    surfaces(activation, "adaptercore")
            );
        }
        int featureContractCount = registerFeatureContracts(context, activation);
        if (Boolean.TRUE.equals(activation.get("adapterCoreUsed"))
                || !list(activation.get("registeredFeatureContracts")).isEmpty()) {
            recordTypedCapabilityMutation(
                    context,
                    "adaptercore",
                    "register_adaptercore_contract",
                    "adaptercore." + normalized(context.descriptor().id()) + ".contract",
                    Map.of(
                            "source", "activation.adaptercore.contract",
                            "adapterCoreUsed", Boolean.TRUE.equals(activation.get("adapterCoreUsed")),
                            "featureContractCount", featureContractCount
                    )
            );
        }
        registerEventHooks(context, activation);
        registerLifecyclePhases(context, activation);
        int adapterDomainCount = registerAdapterDomains(context, activation);
        int runtimeTargetCount = registerRuntimeTargets(context, activation);
        context.recordMutation(
                "lifecycle",
                action,
                className(entrypoint),
                EchoNativeLoadStatus.REGISTERED
        );
        if (featureContractCount > 0) {
            context.recordMutation(
                    "features",
                    "native_feature_contracts_projected",
                    featureContractCount + " contract(s)",
                    EchoNativeLoadStatus.MUTATED
            );
        }
        if (adapterDomainCount > 0) {
            context.recordMutation(
                    "adaptercore",
                    "native_adapter_domains_projected",
                    adapterDomainCount + " domain(s)",
                    EchoNativeLoadStatus.MUTATED
            );
        }
        if (runtimeTargetCount > 0) {
            context.recordMutation(
                    "runtime",
                    "native_runtime_targets_projected",
                    runtimeTargetCount + " target(s)",
                    EchoNativeLoadStatus.MUTATED
            );
        }
    }

    public static void registerContent(EchoNativeModuleLoadContext context, Map<String, Object> activation) {
        Map<String, Object> bridge = object(activation.get("registryBridge"));
        Object registryHost = nativeHost(context, REGISTRY_HOST_SERVICE_ID);
        Object commandHost = nativeHost(context, COMMAND_HOST_SERVICE_ID);
        Object resourceHost = nativeHost(context, RESOURCE_HOST_SERVICE_ID);
        Object networkHost = nativeHost(context, NETWORK_HOST_SERVICE_ID);
        Object configHost = nativeHost(context, CONFIG_HOST_SERVICE_ID);
        int nativeRegistryMutationCount = 0;
        int nativeCommandHostRegistryCount = 0;
        int nativeResourceHostRegistryCount = 0;
        int nativeNetworkHostRegistryCount = 0;
        int nativeConfigHostRegistryCount = 0;
        int nativeClientRouteSdkCount = 0;
        int nativeClientRouteSdkMutatedCount = 0;
        int nativeClientWindowPumpAvailableCount = 0;
        for (Map<String, Object> registration : objectList(bridge.get("registrations"))) {
            String registry = string(registration.get("registry"));
            String id = string(registration.get("id"));
            if (registry.isBlank() || id.isBlank()) {
                continue;
            }
            Map<String, Object> evidence = new LinkedHashMap<>(registration);
            if (creativeTabAlreadyRegistered(context, registryHost, registry, id)) {
                evidence.put("nativeCreativeTabPreexisting", true);
            }
            String nativeRegistryStatus = registerNativeRegistry(context, registryHost, registry, id, registration);
            evidence.put("nativeRegistryHostStatus", nativeRegistryStatus);
            boolean nativeRegistryMutated = "MUTATED".equals(nativeRegistryStatus);
            evidence.put("nativeRegistryHostMutated", nativeRegistryMutated);
            if (nativeRegistryMutated) {
                nativeRegistryMutationCount++;
                context.recordMutation(
                        "registry",
                        "native_registry_host_registered",
                        registry + ":" + id,
                    EchoNativeLoadStatus.MUTATED
                );
                recordTypedRegistryMutation(
                        context,
                        "registry",
                        "native_registry_host_registered",
                        declaredContentId(context.descriptor().id(), id),
                        evidence
                );
            }
            EchoNativeLoadStatus nativeCommandStatus = registerNativeCommandRegistration(
                    context,
                    commandHost,
                    registry,
                    id,
                    registration
            );
            if (nativeCommandStatus != null && nativeCommandStatus != EchoNativeLoadStatus.UNSUPPORTED) {
                evidence.put("nativeCommandHostStatus", nativeCommandStatus.name());
                evidence.put("nativeCommandHostQueued", nativeCommandStatus == EchoNativeLoadStatus.REGISTERED
                        || nativeCommandStatus == EchoNativeLoadStatus.MUTATED
                        || nativeCommandStatus == EchoNativeLoadStatus.RESOLVED);
                if (nativeCommandStatus == EchoNativeLoadStatus.MUTATED) {
                    nativeCommandHostRegistryCount++;
                    context.recordMutation(
                            "commands",
                            "native_command_host_registered",
                            registry + ":" + id,
                            EchoNativeLoadStatus.MUTATED
                    );
                }
                context.registerService(
                        "command_host." + normalized(context.descriptor().id()) + "." + normalized(id),
                        Map.of("kind", "command_host_registration", "evidence", Map.copyOf(evidence)),
                        "commands",
                        "native_host"
                );
            }
            EchoNativeLoadStatus nativeResourceStatus = registerNativeResourceRegistration(
                    context,
                    resourceHost,
                    registry,
                    id,
                    registration
            );
            if (nativeResourceStatus != null && nativeResourceStatus != EchoNativeLoadStatus.UNSUPPORTED) {
                evidence.put("nativeResourceHostStatus", nativeResourceStatus.name());
                evidence.put("nativeResourceHostMounted", nativeResourceStatus == EchoNativeLoadStatus.REGISTERED
                        || nativeResourceStatus == EchoNativeLoadStatus.MUTATED
                        || nativeResourceStatus == EchoNativeLoadStatus.RESOLVED);
                if (nativeResourceStatus == EchoNativeLoadStatus.MUTATED) {
                    nativeResourceHostRegistryCount++;
                    context.recordMutation(
                            "resources",
                            "native_resource_host_mounted",
                            registry + ":" + id,
                            EchoNativeLoadStatus.MUTATED
                    );
                }
                context.registerService(
                        "resource_host." + normalized(context.descriptor().id()) + "." + normalized(id),
                        Map.of("kind", "resource_host_registration", "evidence", Map.copyOf(evidence)),
                        "resources",
                        "native_host"
                );
            }
            EchoNativeLoadStatus nativeNetworkStatus = registerNativeNetworkRegistration(
                    context,
                    networkHost,
                    registry,
                    id,
                    registration
            );
            if (nativeNetworkStatus != null && nativeNetworkStatus != EchoNativeLoadStatus.UNSUPPORTED) {
                evidence.put("nativeNetworkHostStatus", nativeNetworkStatus.name());
                evidence.put("nativeNetworkHostBound", nativeNetworkStatus == EchoNativeLoadStatus.REGISTERED
                        || nativeNetworkStatus == EchoNativeLoadStatus.MUTATED
                        || nativeNetworkStatus == EchoNativeLoadStatus.RESOLVED);
                if (nativeNetworkStatus == EchoNativeLoadStatus.MUTATED) {
                    nativeNetworkHostRegistryCount++;
                    context.recordMutation(
                            "networking",
                            "native_network_host_bound",
                            registry + ":" + id,
                            EchoNativeLoadStatus.MUTATED
                    );
                }
                context.registerService(
                        "network_host." + normalized(context.descriptor().id()) + "." + normalized(id),
                        Map.of("kind", "network_host_registration", "evidence", Map.copyOf(evidence)),
                        "networking",
                        "native_host"
                );
            }
            EchoNativeLoadStatus nativeConfigStatus = registerNativeConfigRegistration(
                    context,
                    configHost,
                    registry,
                    id,
                    registration
            );
            if (nativeConfigStatus != null && nativeConfigStatus != EchoNativeLoadStatus.UNSUPPORTED) {
                evidence.put("nativeConfigHostStatus", nativeConfigStatus.name());
                evidence.put("nativeConfigHostRegistered", nativeConfigStatus == EchoNativeLoadStatus.REGISTERED
                        || nativeConfigStatus == EchoNativeLoadStatus.MUTATED
                        || nativeConfigStatus == EchoNativeLoadStatus.RESOLVED);
                if (nativeConfigStatus == EchoNativeLoadStatus.MUTATED) {
                    nativeConfigHostRegistryCount++;
                    context.recordMutation(
                            "config",
                            "native_config_host_registered",
                            registry + ":" + id,
                            EchoNativeLoadStatus.MUTATED
                    );
                }
                context.registerService(
                        "config_host." + normalized(context.descriptor().id()) + "." + normalized(id),
                        Map.of("kind", "config_host_registration", "evidence", Map.copyOf(evidence)),
                        "config",
                        "native_host"
                );
            }
            String prefix = "service".equals(registry) ? "service." : "content." + normalized(registry) + ".";
            context.registerService(
                    prefix + normalized(id),
                    Map.of("kind", "registry_registration", "evidence", Map.copyOf(evidence)),
                    "registry",
                    normalized(registry)
            );
            annotateClientWindowPumpRegistration(context, evidence);
            EchoNativeLoadStatus clientUiStatus = mountClientUiSurface(context, registry, id, evidence);
            if (Boolean.TRUE.equals(evidence.get("nativeClientWindowPumpServiceAvailable"))) {
                nativeClientWindowPumpAvailableCount++;
            }
            EchoNativeLoadStatus clientRouteSdkStatus = registerNativeClientRouteSdk(
                    context,
                    registry,
                    id,
                    evidence,
                    clientUiStatus
            );
            if (clientRouteSdkStatus != null && clientRouteSdkStatus != EchoNativeLoadStatus.UNSUPPORTED) {
                nativeClientRouteSdkCount++;
                evidence.put("nativeClientRouteSdkStatus", clientRouteSdkStatus.name());
                if (clientRouteSdkStatus == EchoNativeLoadStatus.MUTATED) {
                    nativeClientRouteSdkMutatedCount++;
                    context.recordMutation(
                            "ui",
                            "native_client_route_sdk_registered",
                            id,
                            EchoNativeLoadStatus.MUTATED
                    );
                }
            }
            if (isUiRegistrationStatus(clientUiStatus)) {
                context.recordMutation(
                        "ui",
                        "native_client_ui_surface_registered",
                        id,
                        clientUiStatus
                );
            }
        }
        if (nativeRegistryMutationCount > 0) {
            context.attribute("nativeRegistryHostMutationCount", nativeRegistryMutationCount);
        }
        if (nativeCommandHostRegistryCount > 0) {
            context.attribute("nativeCommandHostRegistryQueuedCount", nativeCommandHostRegistryCount);
        }
        if (nativeResourceHostRegistryCount > 0) {
            context.attribute("nativeResourceHostRegistryMountedCount", nativeResourceHostRegistryCount);
        }
        if (nativeNetworkHostRegistryCount > 0) {
            context.attribute("nativeNetworkHostRegistryBoundCount", nativeNetworkHostRegistryCount);
        }
        if (nativeConfigHostRegistryCount > 0) {
            context.attribute("nativeConfigHostRegistryRegisteredCount", nativeConfigHostRegistryCount);
        }
        if (nativeClientRouteSdkCount > 0) {
            context.attribute("nativeClientRouteSdkRegistrationCount", nativeClientRouteSdkCount);
            context.attribute("nativeClientRouteSdkMutatedCount", nativeClientRouteSdkMutatedCount);
        }
        if (nativeClientWindowPumpAvailableCount > 0) {
            context.attribute("nativeClientWindowPumpAvailableCount", nativeClientWindowPumpAvailableCount);
        }
    }

    public static void ready(EchoNativeModuleLoadContext context) {
        context.attribute("nativeActivationEntrypointReady", true);
        recordLifecycleCallback(context, "onResourcesReady");
        recordLifecycleCallback(context, "onFirstTick");
    }

    public static void recordLifecycleCallback(EchoNativeModuleLoadContext context, String callback) {
        if (context == null || callback == null || callback.isBlank()) {
            return;
        }
        context.recordMutation(
                "lifecycle",
                "native_lifecycle_callback_executed",
                callback,
                EchoNativeLoadStatus.MUTATED
        );
    }

    private static int registerFeatureContracts(EchoNativeModuleLoadContext context, Map<String, Object> data) {
        int registered = 0;
        for (Object contract : list(data.get("registeredFeatureContracts"))) {
            String id = string(contract);
            if (!id.isBlank()) {
                context.registerService(
                        "feature." + normalized(context.descriptor().id()) + "." + normalized(id),
                        Map.of("kind", "feature_contract", "id", id),
                        "features",
                        "contracts"
                );
                recordTypedRegistryMutation(
                        context,
                        "features",
                        "register_feature_contract",
                        id,
                        Map.of(
                                "source", "activation.registeredFeatureContracts",
                                "contractId", id,
                                "moduleId", context.descriptor().id()
                        )
                );
                registered++;
            }
        }
        return registered;
    }

    private static void registerEventHooks(EchoNativeModuleLoadContext context, Map<String, Object> data) {
        Map<String, Object> bridge = object(data.get("eventBridge"));
        Object eventHost = nativeHost(context, EVENT_HOST_SERVICE_ID);
        Object commandHost = nativeHost(context, COMMAND_HOST_SERVICE_ID);
        int nativeSubscriptionCount = 0;
        int nativeCommandHookCount = 0;
        for (Map<String, Object> hook : objectList(bridge.get("hooks"))) {
            String event = string(hook.get("event"));
            String handler = string(hook.get("handler"));
            if (!event.isBlank() && !handler.isBlank()) {
                Map<String, Object> evidence = new LinkedHashMap<>(hook);
                if (subscribeNativeEventHook(context, eventHost, event, handler, hook)) {
                    nativeSubscriptionCount++;
                    evidence.put("nativeEventHostSubscribed", true);
                    context.recordMutation(
                            "events",
                            "native_event_handler_subscribed",
                            event + "#" + handler,
                            EchoNativeLoadStatus.MUTATED
                    );
                } else {
                    evidence.put("nativeEventHostSubscribed", false);
                }
                EchoNativeLoadStatus nativeCommandStatus = registerNativeCommandHook(
                        context,
                        commandHost,
                        event,
                        handler,
                        hook
                );
                if (nativeCommandStatus != null && nativeCommandStatus != EchoNativeLoadStatus.UNSUPPORTED) {
                    evidence.put("nativeCommandHostStatus", nativeCommandStatus.name());
                    evidence.put("nativeCommandHostQueued", nativeCommandStatus == EchoNativeLoadStatus.REGISTERED
                            || nativeCommandStatus == EchoNativeLoadStatus.MUTATED
                            || nativeCommandStatus == EchoNativeLoadStatus.RESOLVED);
                    if (nativeCommandStatus == EchoNativeLoadStatus.MUTATED) {
                        nativeCommandHookCount++;
                        context.recordMutation(
                                "commands",
                                "native_command_host_hook_queued",
                                event + "#" + handler,
                                EchoNativeLoadStatus.MUTATED
                        );
                    }
                    context.registerService(
                            "command_host." + normalized(context.descriptor().id()) + "."
                                    + normalized(event) + "." + normalized(handler),
                            Map.of("kind", "command_event_hook", "evidence", Map.copyOf(evidence)),
                            "commands",
                            "native_host"
                    );
                }
                context.registerService(
                        "event." + normalized(event) + "." + normalized(handler),
                        Map.of("kind", "event_hook", "evidence", Map.copyOf(evidence)),
                        "events",
                        normalized(event)
                );
            }
        }
        if (nativeSubscriptionCount > 0) {
            context.attribute("nativeEventHostSubscriptionCount", nativeSubscriptionCount);
        }
        if (nativeCommandHookCount > 0) {
            context.attribute("nativeCommandHostEventHookQueuedCount", nativeCommandHookCount);
        }
    }

    private static void registerLifecyclePhases(EchoNativeModuleLoadContext context, Map<String, Object> data) {
        Map<String, Object> bridge = object(data.get("lifecycleBridge"));
        Object lifecycleHost = nativeHost(context, LIFECYCLE_HOST_SERVICE_ID);
        int nativeLifecycleRecordCount = 0;
        for (Map<String, Object> phase : objectList(bridge.get("phases"))) {
            String id = string(phase.get("id"));
            if (!id.isBlank()) {
                Map<String, Object> evidence = new LinkedHashMap<>(phase);
                if (recordNativeLifecyclePhase(context, lifecycleHost, id, phase)) {
                    nativeLifecycleRecordCount++;
                    evidence.put("nativeLifecycleHostRecorded", true);
                    context.recordMutation(
                            "lifecycle",
                            "native_lifecycle_phase_recorded",
                            id,
                            EchoNativeLoadStatus.MUTATED
                    );
                } else {
                    evidence.put("nativeLifecycleHostRecorded", false);
                }
                context.registerService(
                        "lifecycle." + normalized(context.descriptor().id()) + "." + normalized(id),
                        Map.of("kind", "lifecycle_phase", "evidence", Map.copyOf(evidence)),
                        "lifecycle"
                );
            }
        }
        if (nativeLifecycleRecordCount > 0) {
            context.attribute("nativeLifecycleHostRecordCount", nativeLifecycleRecordCount);
        }
    }

    private static Object nativeHost(EchoNativeModuleLoadContext context, String serviceId) {
        return context.serviceRegistry()
                .service("echocore", serviceId)
                .or(() -> context.serviceRegistry().service(serviceId))
                .orElse(null);
    }

    private static boolean subscribeNativeEventHook(
            EchoNativeModuleLoadContext context,
            Object eventHost,
            String event,
            String handler,
            Map<String, Object> hook
    ) {
        if (eventHost == null) {
            return false;
        }
        try {
            Method subscribe = eventHost.getClass().getMethod(
                    "subscribeDeclaredHook",
                    String.class,
                    String.class,
                    String.class,
                    Map.class
            );
            subscribe.invoke(eventHost, context.descriptor().id(), event, handler, Map.copyOf(hook));
            return true;
        } catch (ReflectiveOperationException | IllegalArgumentException exception) {
            context.attribute("nativeEventHostSubscriptionError",
                    exception.getClass().getSimpleName() + ": " + exception.getMessage());
            return false;
        }
    }

    private static boolean recordNativeLifecyclePhase(
            EchoNativeModuleLoadContext context,
            Object lifecycleHost,
            String phaseId,
            Map<String, Object> phase
    ) {
        if (lifecycleHost == null) {
            return false;
        }
        try {
            Method record = lifecycleHost.getClass().getMethod(
                    "recordDeclaredLifecyclePhase",
                    String.class,
                    String.class,
                    Map.class
            );
            record.invoke(lifecycleHost, context.descriptor().id(), phaseId, Map.copyOf(phase));
            return true;
        } catch (ReflectiveOperationException | IllegalArgumentException exception) {
            context.attribute("nativeLifecycleHostRecordError",
                    exception.getClass().getSimpleName() + ": " + exception.getMessage());
            return false;
        }
    }

    private static String registerNativeRegistry(
            EchoNativeModuleLoadContext context,
            Object registryHost,
            String registry,
            String id,
            Map<String, Object> registration
    ) {
        if (registryHost == null) {
            return "UNAVAILABLE";
        }
        try {
            Method registerDeclared = registryHost.getClass().getMethod(
                    "registerDeclared",
                    String.class,
                    String.class,
                    String.class,
                    Map.class
            );
            Object status = registerDeclared.invoke(
                    registryHost,
                    context.descriptor().id(),
                    registry,
                    id,
                    Map.copyOf(registration)
            );
            return status == null ? "" : String.valueOf(status);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            context.attribute("nativeRegistryHostRegistrationError",
                    exception.getClass().getSimpleName() + ": " + exception.getMessage());
            return "FAILED";
        }
    }

    private static boolean creativeTabAlreadyRegistered(
            EchoNativeModuleLoadContext context,
            Object registryHost,
            String registry,
            String id
    ) {
        if (registryHost == null || !isCreativeTabRegistry(registry)) {
            return false;
        }
        try {
            Method creativeTab = registryHost.getClass().getMethod("creativeTab", String.class);
            return creativeTab.invoke(registryHost, declaredContentId(context.descriptor().id(), id)) != null;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            return false;
        }
    }

    private static boolean isCreativeTabRegistry(String registry) {
        String normalized = string(registry).trim().toLowerCase(Locale.ROOT).replace('-', '_').replace('.', '_');
        return "creative_tab".equals(normalized) || "creative_tabs".equals(normalized);
    }

    private static String declaredContentId(String moduleId, String id) {
        String normalized = string(id).trim().toLowerCase(Locale.ROOT);
        if (normalized.contains(":")) {
            return normalized;
        }
        String namespace = string(moduleId).trim().toLowerCase(Locale.ROOT);
        return namespace.isBlank() || normalized.isBlank() ? normalized : namespace + ":" + normalized;
    }

    private static EchoNativeLoadStatus registerNativeResourceRegistration(
            EchoNativeModuleLoadContext context,
            Object resourceHost,
            String registry,
            String id,
            Map<String, Object> registration
    ) {
        if (!isResourceRegistry(registry)) {
            return EchoNativeLoadStatus.UNSUPPORTED;
        }
        Map<String, Object> evidence = new LinkedHashMap<>(registration);
        evidence.put("source", "activation.registryBridge.resource");
        evidence.put("moduleId", context.descriptor().id());
        evidence.put("resourceRegistry", registry);
        return registerNativeResource(
                context,
                resourceHost,
                id,
                resourceSurfaceType(registry),
                evidence
        );
    }

    private static EchoNativeLoadStatus registerNativeNetworkRegistration(
            EchoNativeModuleLoadContext context,
            Object networkHost,
            String registry,
            String id,
            Map<String, Object> registration
    ) {
        if (!isNetworkRegistry(registry)) {
            return EchoNativeLoadStatus.UNSUPPORTED;
        }
        Map<String, Object> evidence = new LinkedHashMap<>(registration);
        evidence.put("source", "activation.registryBridge.network");
        evidence.put("moduleId", context.descriptor().id());
        evidence.put("networkRegistry", registry);
        return registerNativeNetworkPacket(
                context,
                networkHost,
                id,
                normalized(registry),
                "registryBridge." + normalized(registry),
                stringList(registration.get("consumers")),
                evidence
        );
    }

    private static EchoNativeLoadStatus registerNativeConfigRegistration(
            EchoNativeModuleLoadContext context,
            Object configHost,
            String registry,
            String id,
            Map<String, Object> registration
    ) {
        if (!isConfigRegistry(registry)) {
            return EchoNativeLoadStatus.UNSUPPORTED;
        }
        Map<String, Object> evidence = new LinkedHashMap<>(registration);
        evidence.put("source", "activation.registryBridge.config");
        evidence.put("moduleId", context.descriptor().id());
        evidence.put("configRegistry", registry);
        return registerNativeConfig(
                context,
                configHost,
                id,
                normalized(registry),
                evidence
        );
    }

    private static EchoNativeLoadStatus registerNativeResource(
            EchoNativeModuleLoadContext context,
            Object resourceHost,
            String resourceId,
            String resourceType,
            Map<String, Object> evidence
    ) {
        if (resourceHost == null) {
            return EchoNativeLoadStatus.UNSUPPORTED;
        }
        try {
            Method register = resourceHost.getClass().getMethod(
                    "registerResource",
                    String.class,
                    String.class,
                    String.class,
                    Map.class
            );
            Object status = register.invoke(
                    resourceHost,
                    context.descriptor().id(),
                    resourceId,
                    resourceType,
                    Map.copyOf(evidence)
            );
            EchoNativeLoadStatus loadStatus = loadStatus(status);
            return loadStatus == null ? EchoNativeLoadStatus.UNSUPPORTED : loadStatus;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            context.attribute("nativeResourceHostRegistrationError",
                    exception.getClass().getSimpleName() + ": " + exception.getMessage());
            return EchoNativeLoadStatus.FAILED;
        }
    }

    private static EchoNativeLoadStatus registerNativeNetworkPacket(
            EchoNativeModuleLoadContext context,
            Object networkHost,
            String packetId,
            String surface,
            String sourceRuntimeTarget,
            List<String> consumers,
            Map<String, Object> evidence
    ) {
        if (networkHost == null) {
            return EchoNativeLoadStatus.UNSUPPORTED;
        }
        try {
            Method register = networkHost.getClass().getMethod(
                    "registerDeclaredPacket",
                    String.class,
                    String.class,
                    String.class,
                    String.class,
                    List.class,
                    Map.class
            );
            Object status = register.invoke(
                    networkHost,
                    context.descriptor().id(),
                    packetId,
                    surface,
                    sourceRuntimeTarget,
                    consumers == null ? List.of() : List.copyOf(consumers),
                    Map.copyOf(evidence)
            );
            EchoNativeLoadStatus loadStatus = loadStatus(status);
            return loadStatus == null ? EchoNativeLoadStatus.UNSUPPORTED : loadStatus;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            context.attribute("nativeNetworkHostRegistrationError",
                    exception.getClass().getSimpleName() + ": " + exception.getMessage());
            return EchoNativeLoadStatus.FAILED;
        }
    }

    private static EchoNativeLoadStatus registerNativeConfig(
            EchoNativeModuleLoadContext context,
            Object configHost,
            String configId,
            String scope,
            Map<String, Object> evidence
    ) {
        if (configHost == null) {
            return EchoNativeLoadStatus.UNSUPPORTED;
        }
        try {
            Method register = configHost.getClass().getMethod(
                    "registerConfig",
                    String.class,
                    String.class,
                    String.class,
                    Map.class
            );
            Object status = register.invoke(
                    configHost,
                    context.descriptor().id(),
                    configId,
                    scope,
                    Map.copyOf(evidence)
            );
            EchoNativeLoadStatus loadStatus = loadStatus(status);
            return loadStatus == null ? EchoNativeLoadStatus.UNSUPPORTED : loadStatus;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            context.attribute("nativeConfigHostRegistrationError",
                    exception.getClass().getSimpleName() + ": " + exception.getMessage());
            return EchoNativeLoadStatus.FAILED;
        }
    }

    private static EchoNativeLoadStatus registerNativeCommandRegistration(
            EchoNativeModuleLoadContext context,
            Object commandHost,
            String registry,
            String id,
            Map<String, Object> registration
    ) {
        if (!isCommandRegistry(registry)) {
            return EchoNativeLoadStatus.UNSUPPORTED;
        }
        Map<String, Object> evidence = new LinkedHashMap<>(registration);
        evidence.put("source", "activation.registryBridge.command");
        evidence.put("moduleId", context.descriptor().id());
        evidence.put("commandRegistry", registry);
        return registerNativeCommand(
                context,
                commandHost,
                id,
                normalized(registry),
                "registryBridge." + normalized(registry),
                evidence,
                "nativeCommandHostRegistrationError"
        );
    }

    private static EchoNativeLoadStatus registerNativeCommandHook(
            EchoNativeModuleLoadContext context,
            Object commandHost,
            String event,
            String handler,
            Map<String, Object> hook
    ) {
        if (!isCommandEvent(event)) {
            return EchoNativeLoadStatus.UNSUPPORTED;
        }
        Map<String, Object> evidence = new LinkedHashMap<>(hook);
        evidence.put("source", "activation.eventBridge.command");
        evidence.put("moduleId", context.descriptor().id());
        evidence.put("commandEvent", event);
        return registerNativeCommand(
                context,
                commandHost,
                handler,
                normalized(event),
                "eventBridge." + normalized(event),
                evidence,
                "nativeCommandHostHookError"
        );
    }

    private static EchoNativeLoadStatus registerNativeCommand(
            EchoNativeModuleLoadContext context,
            Object commandHost,
            String commandId,
            String targetSurface,
            String targetBridge,
            Map<String, Object> evidence,
            String errorAttribute
    ) {
        if (commandHost == null) {
            return EchoNativeLoadStatus.UNSUPPORTED;
        }
        try {
            Method register = commandHost.getClass().getMethod(
                    "registerDeclaredCommand",
                    String.class,
                    String.class,
                    String.class,
                    String.class,
                    Map.class
            );
            Object status = register.invoke(
                    commandHost,
                    context.descriptor().id(),
                    commandId,
                    targetSurface,
                    targetBridge,
                    Map.copyOf(evidence)
            );
            EchoNativeLoadStatus loadStatus = loadStatus(status);
            return loadStatus == null ? EchoNativeLoadStatus.UNSUPPORTED : loadStatus;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            context.attribute(errorAttribute,
                    exception.getClass().getSimpleName() + ": " + exception.getMessage());
            return EchoNativeLoadStatus.FAILED;
        }
    }

    private static int registerAdapterDomains(EchoNativeModuleLoadContext context, Map<String, Object> data) {
        int registered = 0;
        int nativeHostProjectionCount = 0;
        for (Object domain : list(data.get("adapterDomains"))) {
            String id = string(domain);
            if (!id.isBlank()) {
                List<Map<String, Object>> nativeHostProjections = registerNativeAdapterDomain(context, id, data);
                context.registerService(
                        "adapter_domain." + normalized(context.descriptor().id()) + "." + normalized(id),
                        Map.of("kind", "adapter_domain", "id", id),
                        "adaptercore",
                        normalized(id)
                );
                if (!nativeHostProjections.isEmpty()) {
                    nativeHostProjectionCount += nativeHostProjections.size();
                    context.registerService(
                            "adapter_domain_host." + normalized(context.descriptor().id()) + "." + normalized(id),
                            Map.of(
                                    "kind", "adapter_domain_host_projection",
                                    "id", id,
                                    "nativeHostProjections", List.copyOf(nativeHostProjections)
                            ),
                            "adaptercore",
                            normalized(id),
                            "native_host_projection"
                    );
                }
                registered++;
            }
        }
        if (nativeHostProjectionCount > 0) {
            context.attribute("nativeAdapterDomainHostProjectionCount", nativeHostProjectionCount);
        }
        return registered;
    }

    private static List<Map<String, Object>> registerNativeAdapterDomain(
            EchoNativeModuleLoadContext context,
            String domain,
            Map<String, Object> activation
    ) {
        List<Map<String, Object>> projections = new ArrayList<>();
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("moduleId", context.descriptor().id());
        evidence.put("domain", domain);
        evidence.put("entrypoint", string(activation.get("entrypoint")));
        evidence.put("activationStage", string(activation.get("activationStage")));
        evidence.put("source", "activation.adapterDomains");
        for (String serviceId : ADAPTER_DOMAIN_HOST_SERVICE_IDS) {
            Object host = nativeHost(context, serviceId);
            if (host == null) {
                continue;
            }
            EchoNativeLoadStatus status = registerDescriptorDomain(context, host, serviceId, domain, evidence);
            if (status == null || status == EchoNativeLoadStatus.UNSUPPORTED) {
                continue;
            }
            Map<String, Object> projection = new LinkedHashMap<>();
            projection.put("serviceId", serviceId);
            projection.put("hostClass", host.getClass().getName());
            projection.put("domain", domain);
            projection.put("status", status.name());
            projections.add(Map.copyOf(projection));
            context.recordMutation(
                    nativeHostSurface(serviceId),
                    "native_adapter_domain_projected",
                    domain,
                    status
            );
            if (status == EchoNativeLoadStatus.MUTATED) {
                Map<String, Object> typedEvidence = new LinkedHashMap<>(evidence);
                typedEvidence.put("source", "activation.adapterDomains.native_host_projection");
                typedEvidence.put("serviceId", serviceId);
                typedEvidence.put("hostClass", host.getClass().getName());
                recordTypedCapabilityMutation(
                        context,
                        "adaptercore",
                        "native_adapter_domain_projected",
                        domain + "@" + serviceId,
                        typedEvidence
                );
            }
        }
        return List.copyOf(projections);
    }

    private static void recordTypedRegistryMutation(
            EchoNativeModuleLoadContext context,
            String surface,
            String action,
            String target,
            Map<String, Object> evidence
    ) {
        Object host = nativeHost(context, TYPED_REGISTRY_SERVICE_ID);
        if (!(host instanceof EchoNativeRegistryService registryService)) {
            return;
        }
        try {
            recordTypedReceipt(context, registryService.register(serviceMutation(context, surface, action, target, evidence)));
        } catch (RuntimeException exception) {
            context.attribute("typedRegistryMutationReceiptError",
                    exception.getClass().getSimpleName() + ": " + exception.getMessage());
        }
    }

    private static void recordTypedCapabilityMutation(
            EchoNativeModuleLoadContext context,
            String surface,
            String action,
            String target,
            Map<String, Object> evidence
    ) {
        Object host = nativeHost(context, TYPED_CAPABILITY_SERVICE_ID);
        if (!(host instanceof EchoNativeCapabilityService capabilityService)) {
            return;
        }
        try {
            recordTypedReceipt(context, capabilityService.registerIntegration(
                    serviceMutation(context, surface, action, target, evidence)));
        } catch (RuntimeException exception) {
            context.attribute("typedCapabilityMutationReceiptError",
                    exception.getClass().getSimpleName() + ": " + exception.getMessage());
        }
    }

    private static EchoNativeServiceMutation serviceMutation(
            EchoNativeModuleLoadContext context,
            String surface,
            String action,
            String target,
            Map<String, Object> evidence
    ) {
        Map<String, Object> typedEvidence = new LinkedHashMap<>(evidence == null ? Map.of() : evidence);
        typedEvidence.putIfAbsent("moduleId", context.descriptor().id());
        typedEvidence.put("typedMutationProof", true);
        return new EchoNativeServiceMutation(
                context.descriptor().id(),
                normalized(surface).isBlank() ? "native" : surface,
                normalized(action).isBlank() ? "mutate" : action,
                target,
                context.descriptor().side(),
                typedEvidence
        );
    }

    private static void recordTypedReceipt(EchoNativeModuleLoadContext context, EchoNativeMutationReceipt receipt) {
        if (receipt != null) {
            context.recordMutation(receipt);
        }
    }

    private static EchoNativeLoadStatus registerDescriptorDomain(
            EchoNativeModuleLoadContext context,
            Object host,
            String serviceId,
            String domain,
            Map<String, Object> evidence
    ) {
        try {
            Method register = host.getClass().getMethod(
                    "registerDescriptorDomain",
                    String.class,
                    String.class,
                    Map.class
            );
            Object status = register.invoke(
                    host,
                    context.descriptor().id(),
                    domain,
                    Map.copyOf(evidence)
            );
            return loadStatus(status);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            context.attribute(
                    "nativeAdapterDomainHostProjectionError." + normalized(serviceId),
                    exception.getClass().getSimpleName() + ": " + exception.getMessage()
            );
            return EchoNativeLoadStatus.FAILED;
        }
    }

    private static EchoNativeLoadStatus loadStatus(Object status) {
        if (status instanceof EchoNativeLoadStatus loadStatus) {
            return loadStatus;
        }
        String text = string(status);
        if (text.isBlank()) {
            return null;
        }
        try {
            return EchoNativeLoadStatus.valueOf(text);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private static String nativeHostSurface(String serviceId) {
        return switch (serviceId) {
            case RESOURCE_HOST_SERVICE_ID -> "resources";
            case NETWORK_HOST_SERVICE_ID -> "networking";
            case CONFIG_HOST_SERVICE_ID -> "config";
            case COMMAND_HOST_SERVICE_ID -> "commands";
            default -> "adaptercore";
        };
    }

    private static boolean isResourceRegistry(String registry) {
        String id = normalized(registry);
        return id.equals("resource")
                || id.equals("resources")
                || id.equals("resource.profile")
                || id.equals("resource.pack")
                || id.equals("resourcepack")
                || id.equals("data")
                || id.equals("data.pack")
                || id.equals("datapack")
                || id.equals("recipe")
                || id.equals("recipes")
                || id.equals("loot")
                || id.equals("loot.table")
                || id.equals("loot.tables")
                || id.equals("loottables")
                || id.equals("tag")
                || id.equals("tags")
                || id.equals("sound")
                || id.equals("sounds")
                || id.equals("structure")
                || id.equals("structures")
                || id.equals("worldgen")
                || id.equals("world.generator")
                || id.equals("world.preset")
                || id.equals("world.template")
                || id.equals("theme")
                || id.equals("themes")
                || id.equals("theme.tokens")
                || id.equals("ui.skin")
                || id.equals("ui.skins")
                || id.equals("render.profile")
                || id.equals("render.profiles")
                || id.equals("asset.kit")
                || id.equals("asset.kits")
                || id.equals("block.palette")
                || id.equals("block.palettes")
                || id.equals("screen.markup")
                || id.equals("screen.layout")
                || id.equals("screen.layouts")
                || id.equals("style")
                || id.equals("styles")
                || id.equals("data.provider")
                || id.equals("data.providers")
                || id.endsWith(".resource")
                || id.endsWith(".resources")
                || id.endsWith(".data");
    }

    private static boolean isNetworkRegistry(String registry) {
        String id = normalized(registry);
        return id.equals("network")
                || id.equals("networking")
                || id.equals("network.payload")
                || id.equals("network.payloads")
                || id.equals("network.hook")
                || id.equals("packet")
                || id.equals("packets")
                || id.equals("payload")
                || id.equals("payloads")
                || id.equals("channel")
                || id.equals("channels")
                || id.endsWith(".packet")
                || id.endsWith(".packets")
                || id.endsWith(".payload")
                || id.endsWith(".payloads");
    }

    private static boolean isConfigRegistry(String registry) {
        String id = normalized(registry);
        return id.equals("config")
                || id.equals("configs")
                || id.equals("configuration")
                || id.equals("config.schema")
                || id.equals("client.config")
                || id.equals("server.config")
                || id.startsWith("config.")
                || id.endsWith(".config")
                || id.endsWith(".config.schema");
    }

    private static boolean isCommandRegistry(String registry) {
        String id = normalized(registry);
        return id.equals("command")
                || id.equals("commands")
                || id.equals("server.commands")
                || id.endsWith(".commands")
                || id.equals("commands.register");
    }

    private static boolean isCommandEvent(String event) {
        String id = normalized(event);
        return id.equals("commands.register")
                || id.equals("command.register")
                || id.equals("server.commands.register")
                || id.endsWith(".commands.register")
                || id.endsWith(".command.register");
    }

    private static int registerRuntimeTargets(EchoNativeModuleLoadContext context, Map<String, Object> data) {
        int registered = 0;
        for (Object target : list(data.get("runtimeTargets"))) {
            String id = string(target);
            if (!id.isBlank()) {
                context.registerService(
                        "runtime_target." + normalized(context.descriptor().id()) + "." + normalized(id),
                        Map.of("kind", "runtime_target", "id", id),
                        "runtime"
                );
                registered++;
            }
        }
        return registered;
    }

    private static void annotateClientWindowPumpRegistration(
            EchoNativeModuleLoadContext context,
            Map<String, Object> registration
    ) {
        if (registration == null) {
            return;
        }
        Object windowPump = context.serviceRegistry()
                .service("echocore", "echo.native.client_window_pump")
                .or(() -> context.serviceRegistry().service("echo.native.client_window_pump"))
                .orElse(null);
        boolean available = windowPump != null;
        registration.put("nativeClientWindowPumpServiceAvailable", available);
        if (available) {
            registration.put("nativeClientWindowPumpServiceId", "echo.native.client_window_pump");
            registration.put("nativeClientWindowPumpServiceClass", windowPump.getClass().getName());
            context.attribute("nativeClientWindowPumpAvailable", true);
        } else {
            context.attribute("nativeClientWindowPumpSkipped", "echo.native.client_window_pump service not available");
        }
    }

    private static EchoNativeLoadStatus mountClientUiSurface(
            EchoNativeModuleLoadContext context,
            String registry,
            String id,
            Map<String, Object> registration
    ) {
        String surfaceType = clientUiSurfaceType(registry);
        if (surfaceType.isBlank()) {
            return EchoNativeLoadStatus.UNSUPPORTED;
        }
        Object uiHost = context.serviceRegistry()
                .service("echocore", "echo.native.client_ui_host")
                .or(() -> context.serviceRegistry().service("echo.native.client_ui_host"))
                .orElse(null);
        if (uiHost == null) {
            context.attribute("nativeClientUiHostMountSkipped", "echo.native.client_ui_host service not available");
            return EchoNativeLoadStatus.UNSUPPORTED;
        }
        try {
            Method registerSurfaceStatus = uiHost.getClass().getMethod(
                    "registerSurfaceStatus",
                    String.class,
                    String.class,
                    String.class,
                    Map.class
            );
            Object status = registerSurfaceStatus.invoke(
                    uiHost,
                    context.descriptor().id(),
                    id,
                    surfaceType,
                    Map.copyOf(registration)
            );
            EchoNativeLoadStatus nativeStatus = uiStatus(status);
            context.attribute("nativeClientUiHostMountStatus", nativeStatus.name());
            if (isUiRegistrationStatus(nativeStatus)) {
                context.attribute("nativeClientUiHostMounted", true);
                return nativeStatus;
            }
            context.attribute("nativeClientUiHostMountSkipped", "echo.native.client_ui_host returned " + nativeStatus.name());
            return nativeStatus;
        } catch (NoSuchMethodException exception) {
            return mountLegacyClientUiSurface(context, uiHost, registry, id, registration);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            context.attribute("nativeClientUiHostMountSkipped",
                    exception.getClass().getSimpleName() + ": " + exception.getMessage());
            return EchoNativeLoadStatus.FAILED;
        }
    }

    private static EchoNativeLoadStatus registerNativeClientRouteSdk(
            EchoNativeModuleLoadContext context,
            String registry,
            String id,
            Map<String, Object> registration,
            EchoNativeLoadStatus clientUiStatus
    ) {
        String surfaceType = clientUiSurfaceType(registry);
        if (surfaceType.isBlank()) {
            return EchoNativeLoadStatus.UNSUPPORTED;
        }
        EchoNativeClientRouteRegistry routeRegistry = EchoNativeClientRouteRegistries.get();
        if (routeRegistry == EchoNativeClientRouteRegistry.NOOP) {
            context.attribute("nativeClientRouteSdkSkipped", "EchoNativeClientRouteRegistry provider not available");
            return EchoNativeLoadStatus.UNSUPPORTED;
        }
        Map<String, Object> safeRegistration = registration == null ? Map.of() : Map.copyOf(registration);
        Map<String, Object> routeEvidence = new LinkedHashMap<>(safeRegistration);
        routeEvidence.put("nativeClientRouteSdk", "echo-native-client-route-registry");
        routeEvidence.put("nativeClientUiHostStatus", clientUiStatus == null
                ? EchoNativeLoadStatus.UNSUPPORTED.name()
                : clientUiStatus.name());
        routeEvidence.put("nativeClientRouteTrustedMutation", clientUiStatus == EchoNativeLoadStatus.MUTATED);
        EchoNativeLoadStatus routeStatus = routeRegistry.registerRoute(
                context.descriptor().id(),
                id,
                surfaceType,
                safeRegistration,
                Map.copyOf(routeEvidence),
                clientUiStatus == EchoNativeLoadStatus.MUTATED
        );
        EchoNativeLoadStatus actionStatus = registerNativeClientRouteActions(routeRegistry, surfaceType, safeRegistration);
        if (actionStatus != EchoNativeLoadStatus.UNSUPPORTED) {
            context.attribute("nativeClientRouteSdkActionStatus." + normalized(id), actionStatus.name());
        }
        return routeStatus == null ? EchoNativeLoadStatus.UNSUPPORTED : routeStatus;
    }

    private static EchoNativeLoadStatus registerNativeClientRouteActions(
            EchoNativeClientRouteRegistry routeRegistry,
            String surfaceType,
            Map<String, Object> registration
    ) {
        Map<String, Map<String, Object>> actions = actionMap(registration.get("actions"));
        if (actions.isEmpty()) {
            return EchoNativeLoadStatus.UNSUPPORTED;
        }
        return routeRegistry.registerActions(surfaceType, actions);
    }

    private static EchoNativeLoadStatus mountLegacyClientUiSurface(
            EchoNativeModuleLoadContext context,
            Object uiHost,
            String registry,
            String id,
            Map<String, Object> registration
    ) {
        String surfaceType = clientUiSurfaceType(registry);
        if (surfaceType.isBlank()) {
            return EchoNativeLoadStatus.UNSUPPORTED;
        }
        try {
            Method registerSurface = uiHost.getClass().getMethod(
                    "registerSurface",
                    String.class,
                    String.class,
                    String.class,
                    Map.class
            );
            registerSurface.invoke(
                    uiHost,
                    context.descriptor().id(),
                    id,
                    surfaceType,
                    Map.copyOf(registration)
            );
            context.attribute("nativeClientUiHostMounted", true);
            context.attribute("nativeClientUiHostMountStatus", "LEGACY_VOID_REGISTER_SURFACE");
            return EchoNativeLoadStatus.MUTATED;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            context.attribute("nativeClientUiHostMountSkipped",
                    exception.getClass().getSimpleName() + ": " + exception.getMessage());
            return EchoNativeLoadStatus.FAILED;
        }
    }

    private static boolean isUiRegistrationStatus(EchoNativeLoadStatus status) {
        return status == EchoNativeLoadStatus.MUTATED
                || status == EchoNativeLoadStatus.REGISTERED
                || status == EchoNativeLoadStatus.RESOLVED;
    }

    private static EchoNativeLoadStatus uiStatus(Object status) {
        String statusText = string(status);
        if (statusText.isBlank()) {
            return EchoNativeLoadStatus.FAILED;
        }
        try {
            return EchoNativeLoadStatus.valueOf(statusText);
        } catch (IllegalArgumentException exception) {
            return EchoNativeLoadStatus.FAILED;
        }
    }

    private static boolean isClientUiRegistry(String registry) {
        return !clientUiSurfaceType(registry).isBlank();
    }

    private static String clientUiSurfaceType(String registry) {
        String id = normalized(registry);
        return switch (id) {
            case "ui.surface", "ui.surfaces", "ui" -> "ui_surface";
            case "ui.overlay", "ui.overlays", "overlay", "overlays" -> "ui_overlay";
            case "client.overlay", "client.overlays" -> "client_overlay";
            case "hud", "huds" -> "hud";
            case "hud.widget", "hud.widgets" -> "hud_widget";
            case "hud.layout", "hud.layouts" -> "hud_layout";
            case "screen", "screens" -> "screen";
            case "screen.surface", "screen.surfaces", "screen.host", "screen.hosts" -> "screen_surface";
            case "loading.screen", "loading.screens", "loading", "load.screen" -> "loading_screen";
            case "main.menu", "main.menus", "mainmenu", "mainmenus" -> "main_menu";
            case "terminal", "eui" -> "terminal";
            case "index", "recipe.index", "recipe.browser", "inventory.overlay" -> "index";
            case "lens", "scanner.lens", "field.lens" -> "lens";
            case "holomap", "holo.map", "map", "fullscreen.map", "mini.map", "minimap" -> "holomap";
            case "theme.surface", "theme.overlay", "theme.ui" -> "theme";
            default -> "";
        };
    }

    private static Map<String, Map<String, Object>> actionMap(Object value) {
        if (!(value instanceof Map<?, ?> map)) {
            return Map.of();
        }
        Map<String, Map<String, Object>> actions = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            String actionId = string(entry.getKey());
            if (actionId.isBlank() || !(entry.getValue() instanceof Map<?, ?> action)) {
                continue;
            }
            Map<String, Object> typedAction = new LinkedHashMap<>();
            for (Map.Entry<?, ?> actionEntry : action.entrySet()) {
                if (actionEntry.getKey() != null) {
                    typedAction.put(String.valueOf(actionEntry.getKey()), actionEntry.getValue());
                }
            }
            actions.put(actionId, Map.copyOf(typedAction));
        }
        return Map.copyOf(actions);
    }

    private static String resourceSurfaceType(String registry) {
        String id = normalized(registry);
        return switch (id) {
            case "resource.pack", "resourcepack", "resourcepacks" -> "resource_pack";
            case "data.pack", "datapack", "datapacks" -> "data_pack";
            case "loot", "loottables" -> "loot_table";
            case "recipes" -> "recipe";
            case "tags" -> "tag";
            case "sounds" -> "sound";
            case "structures" -> "structure";
            case "assets" -> "asset";
            case "ui.screen", "ui.screens" -> "ui_screen";
            case "world.generator" -> "worldgen";
            case "world.preset" -> "world_preset";
            case "world.template" -> "world_template";
            case "theme.tokens" -> "theme_tokens";
            case "ui.skin", "ui.skins" -> "ui_skin";
            case "render.profile", "render.profiles" -> "render_profile";
            case "asset.kit", "asset.kits" -> "asset_kit";
            case "block.palette", "block.palettes" -> "block_palette";
            case "screen.markup" -> "screen_markup";
            case "screen.layout", "screen.layouts" -> "screen_layout";
            default -> id.replace('.', '_');
        };
    }

    private static String inferRepoRoot(Path descriptorPath) {
        if (descriptorPath == null) {
            return "";
        }
        Path current = descriptorPath.toAbsolutePath().normalize();
        while (current != null) {
            if (Files.exists(current.resolve("settings.gradle")) && Files.isDirectory(current.resolve("echo-native-platform"))) {
                return current.toString();
            }
            current = current.getParent();
        }
        return "";
    }

    private static String[] surfaces(Map<String, Object> data, String fallback) {
        List<String> result = new ArrayList<>();
        result.add(fallback);
        for (Object domain : list(data.get("adapterDomains"))) {
            String value = normalized(string(domain));
            if (!value.isBlank() && !result.contains(value)) {
                result.add(value);
            }
        }
        return result.toArray(String[]::new);
    }

    private static Map<String, Object> object(Object value) {
        if (!(value instanceof Map<?, ?> map)) {
            return Map.of();
        }
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (entry.getKey() != null) {
                result.put(String.valueOf(entry.getKey()), entry.getValue());
            }
        }
        return result;
    }

    private static List<Object> list(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        return List.copyOf(list);
    }

    private static List<String> stringList(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (Object item : list) {
            String text = string(item);
            if (!text.isBlank()) {
                result.add(text);
            }
        }
        return List.copyOf(result);
    }

    private static List<Map<String, Object>> objectList(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object item : list) {
            Map<String, Object> object = object(item);
            if (!object.isEmpty()) {
                result.add(object);
            }
        }
        return List.copyOf(result);
    }

    private static String normalized(String value) {
        String text = value == null ? "" : value.toLowerCase(Locale.ROOT);
        StringBuilder result = new StringBuilder();
        boolean previousSeparator = false;
        for (int index = 0; index < text.length(); index++) {
            char ch = text.charAt(index);
            if ((ch >= 'a' && ch <= 'z') || (ch >= '0' && ch <= '9')) {
                result.append(ch);
                previousSeparator = false;
            } else if (!previousSeparator) {
                result.append('.');
                previousSeparator = true;
            }
        }
        while (result.length() > 0 && result.charAt(result.length() - 1) == '.') {
            result.deleteCharAt(result.length() - 1);
        }
        return result.toString();
    }

    private static String className(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof Class<?> type) {
            return type.getName();
        }
        return value.getClass().getName();
    }

    private static String string(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
