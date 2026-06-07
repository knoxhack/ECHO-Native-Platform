package dev.echo.nativeplatform.loader;

import dev.echo.nativeplatform.contracts.EchoNativeBootstrapProductProfile;
import dev.echo.nativeplatform.contracts.EchoNativeModuleDescriptor;
import dev.echo.nativeplatform.contracts.EchoNativeModuleEntrypoint;
import dev.echo.nativeplatform.contracts.EchoNativeModuleLoadContext;
import dev.echo.nativeplatform.contracts.EchoNativeRegisteredService;
import dev.echo.nativeplatform.contracts.EchoNativeRuntimeSide;
import dev.echo.nativeplatform.contracts.EchoNativeServiceRegistry;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class NativeLoaderModuleActivationInvoker {
    public static final String SERVICE_ID = "echo.native.module_activation_invoker";

    private NativeLoaderModuleActivationInvoker() {
    }

    public static Map<String, Object> invoke(
            EchoNativeBootstrapProductProfile profile,
            String moduleId,
            String className
    ) {
        try {
            Class<?> type = Class.forName(className);
            Object instance = type.getConstructor().newInstance();
            String safeModuleId = lowerContentId(moduleId).isBlank() ? profile.namespace() : lowerContentId(moduleId);
            if (instance instanceof EchoNativeModuleEntrypoint entrypoint) {
                return invokeEntrypointPhases(profile, safeModuleId, className, entrypoint);
            }
            return Map.of(
                    "moduleId", safeModuleId,
                    "moduleClass", className,
                    "entrypointApi", "unsupported_legacy_entrypoint",
                    "legacyActivationBridgeUsed", false,
                    "activationStage", "release_loader_requires_direct_entrypoint"
            );
        } catch (Throwable ignored) {
            return Map.of();
        }
    }

    private static Map<String, Object> invokeEntrypointPhases(
            EchoNativeBootstrapProductProfile profile,
            String moduleId,
            String className,
            EchoNativeModuleEntrypoint entrypoint
    ) {
        EchoNativeServiceRegistry serviceRegistry = new EchoNativeServiceRegistry();
        NativeLoaderCoreServiceRegistrar.registerCoreServices(
                serviceRegistry,
                adapterCoreServiceId(profile)
        );
        EchoNativeModuleLoadContext context = new EchoNativeModuleLoadContext(
                new EchoNativeModuleDescriptor(
                        moduleId,
                        moduleId,
                        "",
                        "native_module",
                        "bootstrap_runtime",
                        className,
                        EchoNativeRuntimeSide.COMMON,
                        List.of(),
                        List.of(),
                        List.of(),
                        null,
                        List.of()
                ),
                serviceRegistry,
                new LinkedHashMap<>(Map.of(
                        "packId", profile.nativeGameplayPackId(),
                        "runtime", "echo_native",
                        "bootstrap", "echo_native_bootstrap"
                ))
        );
        Map<String, Object> activation = new LinkedHashMap<>();
        activation.put("moduleId", moduleId);
        activation.put("moduleClass", className);
        activation.put("entrypointApi", "EchoNativeModuleEntrypoint");
        activation.put("legacyActivationBridgeUsed", false);
        activation.put("coreServicesRegisteredBeforeActivation", true);
        try {
            entrypoint.discover(context);
            entrypoint.resolve(context);
            entrypoint.loadClasses(context);
            entrypoint.construct(context);
            entrypoint.registerServices(context);
            entrypoint.registerContent(context);
            entrypoint.commonSetup(context);
            entrypoint.clientSetup(context);
            entrypoint.serverSetup(context);
            entrypoint.ready(context);
            activation.put("phaseLifecycleExecuted", true);
            activation.put("phaseLifecycleFailed", false);
        } catch (Throwable exception) {
            activation.put("phaseLifecycleExecuted", true);
            activation.put("phaseLifecycleFailed", true);
            activation.put("phaseLifecycleFailure", exception.getClass().getName() + ": " + exception.getMessage());
        }
        activation.put("attributes", context.attributes());
        activation.put("mutations", context.mutations());
        activation.put("mutationCount", context.mutations().size());
        activation.put("registeredServiceCount", serviceRegistry.registeredServices().size());
        activation.put("registeredServices", serviceRegistry.registeredServices().stream()
                .map(NativeLoaderModuleActivationInvoker::registeredServiceSummary)
                .toList());
        activation.put("nativeActivationLoaded", !Boolean.TRUE.equals(activation.get("phaseLifecycleFailed")));
        return Map.copyOf(activation);
    }

    private static Map<String, Object> registeredServiceSummary(EchoNativeRegisteredService service) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("moduleId", service.moduleId());
        summary.put("serviceId", service.serviceId());
        summary.put("implementationClass", service.implementationClass());
        summary.put("surfaces", service.surfaces());
        return Map.copyOf(summary);
    }

    private static String adapterCoreServiceId(EchoNativeBootstrapProductProfile profile) {
        String id = profile == null ? "" : profile.nativeLoaderAdapterCoreServiceId();
        return id == null || id.isBlank() ? "adaptercore.native_loader.backend" : id;
    }

    private static String lowerContentId(String contentId) {
        return contentId == null ? "" : contentId.trim().toLowerCase(java.util.Locale.ROOT);
    }
}
