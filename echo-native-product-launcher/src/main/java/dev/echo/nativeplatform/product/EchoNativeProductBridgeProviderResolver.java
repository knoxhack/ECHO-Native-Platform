package dev.echo.nativeplatform.product;

import dev.echo.nativeplatform.contracts.EchoNativeAddonDescriptor;
import dev.echo.nativeplatform.contracts.EchoNativeDiagnostic;
import dev.echo.nativeplatform.contracts.EchoNativeIssueSeverity;
import dev.echo.nativeplatform.contracts.EchoNativeModuleDescriptor;
import dev.echo.nativeplatform.loader.EchoNativeModuleClassLoader;
import dev.echo.nativeplatform.loader.NativeLoaderDefaultProductBridgeProvider;
import dev.echo.nativeplatform.loader.NativeLoaderLiveClientBridge;
import dev.echo.nativeplatform.loader.NativeLoaderLiveRegistryBridge;
import dev.echo.nativeplatform.loader.NativeLoaderLiveRuntimeAttachment;
import dev.echo.nativeplatform.loader.NativeLoaderLiveRuntimeBridge;
import dev.echo.nativeplatform.loader.NativeLoaderProductBridgeContext;
import dev.echo.nativeplatform.loader.NativeLoaderProductBridgeProvider;

import java.lang.reflect.Constructor;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class EchoNativeProductBridgeProviderResolver {
    private static final List<String> PROVIDER_KEYS = List.of(
            "nativeProductBridgeProvider",
            "nativeLiveBridgeProvider",
            "nativeLiveRuntimeBridgeProvider"
    );

    private EchoNativeProductBridgeProviderResolver() {
    }

    static Resolution resolve(
            Path productRoot,
            String packId,
            List<EchoNativeAddonDescriptor> descriptors,
            EchoNativeProductLauncher.EchoNativeProductLaunchOptions baseOptions
    ) {
        EchoNativeProductLauncher.EchoNativeProductLaunchOptions effectiveOptions = baseOptions == null
                ? new EchoNativeProductLauncher.EchoNativeProductLaunchOptions(false, false, false)
                : baseOptions;
        List<EchoNativeDiagnostic> diagnostics = new ArrayList<>();
        List<EchoNativeAddonDescriptor> safeDescriptors = descriptors == null ? List.of() : List.copyOf(descriptors);
        boolean providerMerged = false;

        for (EchoNativeAddonDescriptor descriptor : safeDescriptors) {
            String providerClassName = providerClassName(descriptor);
            if (providerClassName.isBlank()) {
                continue;
            }
            try {
                NativeLoaderProductBridgeProvider provider = instantiateProvider(descriptor, providerClassName);
                NativeLoaderProductBridgeContext context = new NativeLoaderProductBridgeContext(
                        packId,
                        descriptor.id(),
                        productRoot,
                        moduleRoot(productRoot, descriptor),
                        descriptor.access()
                );
                effectiveOptions = merge(effectiveOptions, provider, context);
                providerMerged = true;
            } catch (ReflectiveOperationException | RuntimeException exception) {
                diagnostics.add(providerDiagnostic(
                        packId,
                        descriptor,
                        providerClassName,
                        effectiveOptions.releaseMode(),
                        exception
                ));
            }
        }
        if (!providerMerged) {
            effectiveOptions = merge(
                    effectiveOptions,
                    new NativeLoaderDefaultProductBridgeProvider(),
                    defaultProductBridgeContext(productRoot, packId)
            );
        }
        return new Resolution(effectiveOptions, List.copyOf(diagnostics));
    }

    private static NativeLoaderProductBridgeContext defaultProductBridgeContext(Path productRoot, String packId) {
        Path normalizedProductRoot = productRoot == null ? Path.of(".").toAbsolutePath().normalize()
                : productRoot.toAbsolutePath().normalize();
        return new NativeLoaderProductBridgeContext(
                packId,
                "echocore",
                normalizedProductRoot,
                normalizedProductRoot,
                Map.of(
                        "nativeProductBridgeProvider",
                        NativeLoaderDefaultProductBridgeProvider.class.getName()
                )
        );
    }

    private static NativeLoaderProductBridgeProvider instantiateProvider(
            EchoNativeAddonDescriptor descriptor,
            String providerClassName
    ) throws ReflectiveOperationException {
        EchoNativeModuleDescriptor moduleDescriptor = EchoNativeModuleDescriptor.fromAddon(descriptor);
        EchoNativeModuleClassLoader classLoader = new EchoNativeModuleClassLoader(
                moduleDescriptor.classpath(),
                EchoNativeProductBridgeProviderResolver.class.getClassLoader()
        );
        Class<?> type = classLoader.loadClass(providerClassName);
        if (!NativeLoaderProductBridgeProvider.class.isAssignableFrom(type)) {
            throw new ClassCastException(providerClassName + " does not implement "
                    + NativeLoaderProductBridgeProvider.class.getName());
        }
        Constructor<?> constructor = type.getDeclaredConstructor();
        constructor.setAccessible(true);
        return NativeLoaderProductBridgeProvider.class.cast(constructor.newInstance());
    }

    private static EchoNativeProductLauncher.EchoNativeProductLaunchOptions merge(
            EchoNativeProductLauncher.EchoNativeProductLaunchOptions options,
            NativeLoaderProductBridgeProvider provider,
            NativeLoaderProductBridgeContext context
    ) {
        NativeLoaderLiveRuntimeAttachment runtimeAttachment = options.liveRuntimeAttachment();
        NativeLoaderLiveRuntimeBridge runtimeBridge = options.liveRuntimeBridge();
        NativeLoaderLiveRegistryBridge registryBridge = options.liveRegistryBridge();
        NativeLoaderLiveClientBridge clientBridge = options.liveClientBridge();
        NativeLoaderLiveRuntimeBridge explicitRuntimeBridge = options.liveRuntimeBridge();
        NativeLoaderLiveRegistryBridge explicitRegistryBridge = options.liveRegistryBridge();
        EchoNativeProductHookPlan hookPlan = options.hookPlan();
        Map<String, Object> explicitClientAssessment = options.clientAttachmentAssessment();
        NativeLoaderLiveClientBridge explicitClientBridge = options.liveClientBridge();
        Map<String, Object> clientAssessment = new LinkedHashMap<>(explicitClientAssessment);

        NativeLoaderLiveRuntimeAttachment providedAttachment = provider.liveRuntimeAttachment(context);
        if (runtimeAttachmentPreferred(runtimeAttachment, providedAttachment)) {
            runtimeAttachment = providedAttachment;
        }

        NativeLoaderLiveRuntimeBridge providedRuntimeBridge = provider.liveRuntimeBridge(context);
        if (providedRuntimeBridge != null && providedRuntimeBridge.attached() && !runtimeBridge.attached()) {
            runtimeBridge = providedRuntimeBridge;
        }

        NativeLoaderLiveRegistryBridge providedRegistryBridge = provider.liveRegistryBridge(context);
        boolean explicitLiveRuntimeWithoutRegistry = options.requireLiveRuntime()
                && explicitRuntimeBridge.attached()
                && explicitClientBridge.attached()
                && !explicitRegistryBridge.attached();
        if (providedRegistryBridge != null
                && providedRegistryBridge.attached()
                && !registryBridge.attached()
                && !explicitLiveRuntimeWithoutRegistry) {
            registryBridge = providedRegistryBridge;
        }

        Map<String, Object> providedClientAssessment = provider.clientAttachmentAssessment(context);
        if (providedClientAssessment != null && !providedClientAssessment.isEmpty()) {
            clientAssessment.putAll(providedClientAssessment);
        }
        preserveExplicitLiveClientAttachment(
                explicitClientAssessment,
                clientAssessment,
                explicitClientBridge
        );

        NativeLoaderLiveClientBridge providedClientBridge = provider.liveClientBridge(context);
        if (providedClientBridge != null && providedClientBridge.attached() && !clientBridge.attached()) {
            clientBridge = providedClientBridge;
        }

        hookPlan = mergeHookPlans(hookPlan, productHookPlan(provider.productHookPlan(context)));

        return new EchoNativeProductLauncher.EchoNativeProductLaunchOptions(
                options.requireMutation(),
                options.releaseMode(),
                options.requireLiveRuntime(),
                runtimeAttachment,
                runtimeBridge,
                registryBridge,
                Map.copyOf(clientAssessment),
                clientBridge,
                hookPlan
        );
    }

    private static void preserveExplicitLiveClientAttachment(
            Map<String, Object> explicitClientAssessment,
            Map<String, Object> clientAssessment,
            NativeLoaderLiveClientBridge explicitClientBridge
    ) {
        if (explicitClientBridge == null || !explicitClientBridge.attached()) {
            return;
        }
        for (String key : List.of(
                "launcher",
                "liveClientAttached",
                "headlessClientSurface",
                "realClientProcess",
                "releaseClientTrusted",
                "clientAttachment"
        )) {
            if (explicitClientAssessment.containsKey(key)) {
                clientAssessment.put(key, explicitClientAssessment.get(key));
            }
        }
    }

    private static EchoNativeProductHookPlan mergeHookPlans(
            EchoNativeProductHookPlan base,
            EchoNativeProductHookPlan addition
    ) {
        EchoNativeProductHookPlan safeBase = base == null ? EchoNativeProductHookPlan.empty() : base;
        EchoNativeProductHookPlan safeAddition = addition == null ? EchoNativeProductHookPlan.empty() : addition;
        if (safeBase.isEmpty()) {
            return safeAddition;
        }
        if (safeAddition.isEmpty()) {
            return safeBase;
        }
        return new EchoNativeProductHookPlan(
                concat(safeBase.registryHooks(), safeAddition.registryHooks()),
                concat(safeBase.lifecycleHooks(), safeAddition.lifecycleHooks()),
                concat(safeBase.eventSubscriptions(), safeAddition.eventSubscriptions()),
                concat(safeBase.eventsToPublish(), safeAddition.eventsToPublish()),
                concat(safeBase.commandHooks(), safeAddition.commandHooks()),
                concat(safeBase.networkHooks(), safeAddition.networkHooks()),
                concat(safeBase.resourceHooks(), safeAddition.resourceHooks()),
                concat(safeBase.configHooks(), safeAddition.configHooks()),
                concat(safeBase.runtimeHooks(), safeAddition.runtimeHooks()),
                concat(safeBase.clientSurfaceHooks(), safeAddition.clientSurfaceHooks()),
                concat(safeBase.productWorldHooks(), safeAddition.productWorldHooks()),
                concat(safeBase.productOnboardingHooks(), safeAddition.productOnboardingHooks()),
                concat(safeBase.saveDataHooks(), safeAddition.saveDataHooks())
        );
    }

    private static EchoNativeProductHookPlan productHookPlan(Map<String, Object> plan) {
        if (plan == null || plan.isEmpty()) {
            return EchoNativeProductHookPlan.empty();
        }
        return new EchoNativeProductHookPlan(
                registryHooks(plan.get("registryHooks")),
                lifecycleHooks(plan.get("lifecycleHooks")),
                eventSubscriptionHooks(plan.get("eventSubscriptions")),
                eventPublishHooks(plan.get("eventsToPublish")),
                commandHooks(plan.get("commandHooks")),
                networkHooks(plan.get("networkHooks")),
                resourceHooks(plan.get("resourceHooks")),
                configHooks(plan.get("configHooks")),
                runtimeHooks(plan.get("runtimeHooks")),
                clientSurfaceHooks(plan.get("clientSurfaceHooks")),
                productWorldHooks(plan.get("productWorldHooks")),
                productOnboardingHooks(plan.get("productOnboardingHooks")),
                saveDataHooks(plan.get("saveDataHooks"))
        );
    }

    private static List<EchoNativeProductHookPlan.RegistryHook> registryHooks(Object value) {
        List<EchoNativeProductHookPlan.RegistryHook> hooks = new ArrayList<>();
        for (Map<String, Object> hook : objectList(value)) {
            String moduleId = string(hook.get("moduleId"));
            String registry = string(hook.get("registry"));
            String id = string(hook.get("id"));
            if (!moduleId.isBlank() && !registry.isBlank() && !id.isBlank()) {
                hooks.add(new EchoNativeProductHookPlan.RegistryHook(
                        moduleId,
                        registry,
                        id,
                        object(hook.get("properties"))
                ));
            }
        }
        return List.copyOf(hooks);
    }

    private static List<EchoNativeProductHookPlan.LifecycleHook> lifecycleHooks(Object value) {
        List<EchoNativeProductHookPlan.LifecycleHook> hooks = new ArrayList<>();
        for (Map<String, Object> hook : objectList(value)) {
            String moduleId = string(hook.get("moduleId"));
            String phaseId = string(hook.get("phaseId"));
            if (!moduleId.isBlank() && !phaseId.isBlank()) {
                hooks.add(new EchoNativeProductHookPlan.LifecycleHook(
                        moduleId,
                        phaseId,
                        object(hook.get("evidence"))
                ));
            }
        }
        return List.copyOf(hooks);
    }

    private static List<EchoNativeProductHookPlan.EventSubscriptionHook> eventSubscriptionHooks(Object value) {
        List<EchoNativeProductHookPlan.EventSubscriptionHook> hooks = new ArrayList<>();
        for (Map<String, Object> hook : objectList(value)) {
            String moduleId = string(hook.get("moduleId"));
            String eventId = string(hook.get("eventId"));
            String handlerId = string(hook.get("handlerId"));
            if (!moduleId.isBlank() && !eventId.isBlank() && !handlerId.isBlank()) {
                hooks.add(new EchoNativeProductHookPlan.EventSubscriptionHook(
                        moduleId,
                        eventId,
                        handlerId,
                        object(hook.get("evidence"))
                ));
            }
        }
        return List.copyOf(hooks);
    }

    private static List<EchoNativeProductHookPlan.EventPublishHook> eventPublishHooks(Object value) {
        List<EchoNativeProductHookPlan.EventPublishHook> hooks = new ArrayList<>();
        for (Map<String, Object> hook : objectList(value)) {
            String sourceModule = string(hook.get("sourceModule"));
            String eventId = string(hook.get("eventId"));
            if (!sourceModule.isBlank() && !eventId.isBlank()) {
                hooks.add(new EchoNativeProductHookPlan.EventPublishHook(
                        sourceModule,
                        eventId,
                        object(hook.get("payload"))
                ));
            }
        }
        return List.copyOf(hooks);
    }

    private static List<EchoNativeProductHookPlan.CommandHook> commandHooks(Object value) {
        List<EchoNativeProductHookPlan.CommandHook> hooks = new ArrayList<>();
        for (Map<String, Object> hook : objectList(value)) {
            String moduleId = string(hook.get("moduleId"));
            String commandId = string(hook.get("commandId"));
            if (!moduleId.isBlank() && !commandId.isBlank()) {
                hooks.add(new EchoNativeProductHookPlan.CommandHook(
                        moduleId,
                        commandId,
                        string(hook.get("targetSurface")),
                        string(hook.get("targetBridge")),
                        object(hook.get("evidence"))
                ));
            }
        }
        return List.copyOf(hooks);
    }

    private static List<EchoNativeProductHookPlan.NetworkHook> networkHooks(Object value) {
        List<EchoNativeProductHookPlan.NetworkHook> hooks = new ArrayList<>();
        for (Map<String, Object> hook : objectList(value)) {
            String moduleId = string(hook.get("moduleId"));
            String packetId = string(hook.get("packetId"));
            if (!moduleId.isBlank() && !packetId.isBlank()) {
                hooks.add(new EchoNativeProductHookPlan.NetworkHook(
                        moduleId,
                        packetId,
                        string(hook.get("surface")),
                        string(hook.get("sourceRuntimeTarget")),
                        stringList(hook.get("consumers")),
                        object(hook.get("evidence"))
                ));
            }
        }
        return List.copyOf(hooks);
    }

    private static List<EchoNativeProductHookPlan.ResourceHook> resourceHooks(Object value) {
        List<EchoNativeProductHookPlan.ResourceHook> hooks = new ArrayList<>();
        for (Map<String, Object> hook : objectList(value)) {
            String moduleId = string(hook.get("moduleId"));
            String resourceId = string(hook.get("resourceId"));
            String resourceType = string(hook.get("resourceType"));
            if (!moduleId.isBlank() && !resourceId.isBlank() && !resourceType.isBlank()) {
                hooks.add(new EchoNativeProductHookPlan.ResourceHook(
                        moduleId,
                        resourceId,
                        resourceType,
                        object(hook.get("evidence"))
                ));
            }
        }
        return List.copyOf(hooks);
    }

    private static List<EchoNativeProductHookPlan.ConfigHook> configHooks(Object value) {
        List<EchoNativeProductHookPlan.ConfigHook> hooks = new ArrayList<>();
        for (Map<String, Object> hook : objectList(value)) {
            String moduleId = string(hook.get("moduleId"));
            String configId = string(hook.get("configId"));
            if (!moduleId.isBlank() && !configId.isBlank()) {
                hooks.add(new EchoNativeProductHookPlan.ConfigHook(
                        moduleId,
                        configId,
                        string(hook.get("scope")),
                        object(hook.get("evidence"))
                ));
            }
        }
        return List.copyOf(hooks);
    }

    private static List<EchoNativeProductHookPlan.RuntimeHook> runtimeHooks(Object value) {
        List<EchoNativeProductHookPlan.RuntimeHook> hooks = new ArrayList<>();
        for (Map<String, Object> hook : objectList(value)) {
            String moduleId = string(hook.get("moduleId"));
            String surface = string(hook.get("surface"));
            String targetId = string(hook.get("targetId"));
            if (targetId.isBlank()) {
                targetId = string(hook.get("id"));
            }
            if (!moduleId.isBlank() && !surface.isBlank() && !targetId.isBlank()) {
                hooks.add(new EchoNativeProductHookPlan.RuntimeHook(
                        moduleId,
                        surface,
                        targetId,
                        string(hook.get("action")),
                        object(hook.get("payload"))
                ));
            }
        }
        return List.copyOf(hooks);
    }

    private static List<EchoNativeProductHookPlan.ClientSurfaceHook> clientSurfaceHooks(Object value) {
        List<EchoNativeProductHookPlan.ClientSurfaceHook> hooks = new ArrayList<>();
        for (Map<String, Object> hook : objectList(value)) {
            String moduleId = string(hook.get("moduleId"));
            String surfaceId = string(hook.get("surfaceId"));
            String surfaceType = string(hook.get("surfaceType"));
            if (!moduleId.isBlank() && !surfaceId.isBlank() && !surfaceType.isBlank()) {
                hooks.add(new EchoNativeProductHookPlan.ClientSurfaceHook(
                        moduleId,
                        surfaceId,
                        surfaceType,
                        object(hook.get("config"))
                ));
            }
        }
        return List.copyOf(hooks);
    }

    private static List<EchoNativeProductHookPlan.ProductWorldHook> productWorldHooks(Object value) {
        List<EchoNativeProductHookPlan.ProductWorldHook> hooks = new ArrayList<>();
        for (Map<String, Object> hook : objectList(value)) {
            String moduleId = string(hook.get("moduleId"));
            String worldId = string(hook.get("worldId"));
            String productWorldPreset = string(hook.get("productWorldPreset"));
            if (!moduleId.isBlank() && !worldId.isBlank() && !productWorldPreset.isBlank()) {
                hooks.add(new EchoNativeProductHookPlan.ProductWorldHook(
                        moduleId,
                        worldId,
                        string(hook.get("defaultWorldMode")),
                        productWorldPreset,
                        string(hook.get("productDatapack")),
                        string(hook.get("productResourcePack")),
                        string(hook.get("vanillaSavePolicy")),
                        object(hook.get("evidence"))
                ));
            }
        }
        return List.copyOf(hooks);
    }

    private static List<EchoNativeProductHookPlan.SaveDataHook> saveDataHooks(Object value) {
        List<EchoNativeProductHookPlan.SaveDataHook> hooks = new ArrayList<>();
        for (Map<String, Object> hook : objectList(value)) {
            String key = string(hook.get("key"));
            if (!key.isBlank()) {
                hooks.add(new EchoNativeProductHookPlan.SaveDataHook(
                        key,
                        string(hook.get("value")),
                        booleanValue(hook.get("delete"))
                ));
            }
        }
        return List.copyOf(hooks);
    }

    private static List<EchoNativeProductHookPlan.ProductOnboardingHook> productOnboardingHooks(Object value) {
        List<EchoNativeProductHookPlan.ProductOnboardingHook> hooks = new ArrayList<>();
        for (Map<String, Object> hook : objectList(value)) {
            String moduleId = string(hook.get("moduleId"));
            String playerId = string(hook.get("playerId"));
            String spawnProfile = string(hook.get("spawnProfile"));
            if (!moduleId.isBlank() && !playerId.isBlank() && !spawnProfile.isBlank()) {
                hooks.add(new EchoNativeProductHookPlan.ProductOnboardingHook(
                        moduleId,
                        playerId,
                        spawnProfile,
                        string(hook.get("spawnDimension")),
                        string(hook.get("spawnStructureId")),
                        string(hook.get("starterItemId")),
                        string(hook.get("missionId")),
                        string(hook.get("missionPhase")),
                        string(hook.get("objectiveKey")),
                        string(hook.get("hudChannel")),
                        string(hook.get("briefing")),
                        object(hook.get("evidence"))
                ));
            }
        }
        return List.copyOf(hooks);
    }

    private static boolean runtimeAttachmentPreferred(
            NativeLoaderLiveRuntimeAttachment current,
            NativeLoaderLiveRuntimeAttachment candidate
    ) {
        if (candidate == null) {
            return false;
        }
        NativeLoaderLiveRuntimeAttachment safeCurrent = current == null
                ? NativeLoaderLiveRuntimeAttachment.unattached()
                : current;
        if (candidate.releaseRuntimeTrusted() && !safeCurrent.releaseRuntimeTrusted()) {
            return true;
        }
        if (candidate.liveMinecraftAttached() && !safeCurrent.liveMinecraftAttached()) {
            return true;
        }
        if (safeCurrent.liveMinecraftAttached()) {
            return false;
        }
        return candidate.firstClassNativeRuntime() && !safeCurrent.firstClassNativeRuntime();
    }

    private static String providerClassName(EchoNativeAddonDescriptor descriptor) {
        Map<String, Object> access = descriptor.access() == null ? Map.of() : descriptor.access();
        for (String key : PROVIDER_KEYS) {
            String value = string(access.get(key));
            if (!value.isBlank()) {
                return value;
            }
        }
        Object bridgeBlock = access.get("nativeLiveBridges");
        if (bridgeBlock instanceof Map<?, ?> bridgeMap) {
            String value = string(bridgeMap.get("provider"));
            if (!value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private static Path moduleRoot(Path productRoot, EchoNativeAddonDescriptor descriptor) {
        Path descriptorPath = descriptor.descriptorPath();
        if (descriptorPath != null
                && descriptorPath.getParent() != null
                && descriptorPath.getParent().getParent() != null) {
            return descriptorPath.getParent().getParent().toAbsolutePath().normalize();
        }
        return productRoot.toAbsolutePath().normalize().resolve("modules").resolve(descriptor.id()).normalize();
    }

    private static EchoNativeDiagnostic providerDiagnostic(
            String packId,
            EchoNativeAddonDescriptor descriptor,
            String providerClassName,
            boolean releaseMode,
            Exception exception
    ) {
        EchoNativeIssueSeverity severity = releaseMode ? EchoNativeIssueSeverity.ERROR : EchoNativeIssueSeverity.WARNING;
        return new EchoNativeDiagnostic(
                "ECHO-NATIVE-LIVE-BRIDGE-PROVIDER-INVALID",
                severity,
                "Native Loader live bridge provider could not be attached",
                "Descriptor '" + descriptor.id() + "' declares live bridge provider '" + providerClassName
                        + "', but the product launcher could not instantiate it: " + exception.getMessage(),
                descriptor.id(),
                packId,
                descriptor.descriptorPath() == null
                        ? List.of()
                        : List.of(descriptor.descriptorPath().toString().replace('\\', '/')),
                "Ensure access.nativeProductBridgeProvider names a no-arg class on the packaged nativeClasspath that implements NativeLoaderProductBridgeProvider."
        );
    }

    private static <T> List<T> concat(List<T> left, List<T> right) {
        List<T> result = new ArrayList<>();
        if (left != null) {
            result.addAll(left);
        }
        if (right != null) {
            result.addAll(right);
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

    private static Map<String, Object> object(Object value) {
        if (!(value instanceof Map<?, ?> map)) {
            return Map.of();
        }
        Map<String, Object> object = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (entry.getKey() != null) {
                object.put(String.valueOf(entry.getKey()), entry.getValue());
            }
        }
        return Map.copyOf(object);
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

    private static boolean booleanValue(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        return Boolean.parseBoolean(string(value));
    }

    private static String string(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    record Resolution(
            EchoNativeProductLauncher.EchoNativeProductLaunchOptions options,
            List<EchoNativeDiagnostic> diagnostics
    ) {
    }
}
