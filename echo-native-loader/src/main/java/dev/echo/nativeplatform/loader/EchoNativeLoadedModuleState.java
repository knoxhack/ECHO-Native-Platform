package dev.echo.nativeplatform.loader;

import dev.echo.nativeplatform.contracts.EchoNativeModuleDescriptor;
import dev.echo.nativeplatform.contracts.EchoNativeModuleLoadResult;
import dev.echo.nativeplatform.contracts.EchoNativeRegisteredService;
import dev.echo.nativeplatform.contracts.EchoNativeLifecycleRecord;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoNativeLoadedModuleState {
    private EchoNativeLoadedModuleState() {
    }

    public static Map<String, Object> from(EchoNativeModuleLoadResult result) {
        Map<String, Object> state = new LinkedHashMap<>();
        EchoNativeModuleDescriptor descriptor = result.descriptor();
        state.put("schema", "echo.native.loaded_module_state.v1");
        state.put("moduleId", descriptor.id());
        state.put("status", result.status().name());
        state.put("descriptor", descriptor(descriptor));
        state.put("resolvedDependencies", result.resolvedDependencies());
        state.put("missingDependencies", result.missingDependencies());
        state.put("classpath", classpath(descriptor));
        state.put("classloader", classloader(result));
        state.put("loadedClassName", result.loadedClassName());
        state.put("constructedEntrypointClassName", result.constructedEntrypointClassName());
        state.put("entrypointConstructed", !result.constructedEntrypointClassName().isBlank());
        state.put("registeredServices", services(result.registeredServices(), false));
        state.put("registeredContent", services(result.registeredServices(), true));
        state.put("registeredServiceCount", services(result.registeredServices(), false).size());
        state.put("registeredContentCount", services(result.registeredServices(), true).size());
        state.put("lifecyclePhases", result.phases().stream().map(Enum::name).toList());
        state.put("lifecyclePhaseHistory", result.lifecyclePhaseHistory().stream()
                .map(EchoNativeLifecycleRecord::toReport)
                .toList());
        state.put("moduleLifecycleRecords", result.mutations());
        state.put("typedMutationReceipts", result.mutationReceipts().stream()
                .map(receipt -> receipt.toReport())
                .toList());
        state.put("typedHostMutationReceiptCount", result.mutationReceipts().stream()
                .filter(receipt -> "MUTATED".equals(receipt.status().name()))
                .count());
        state.put("failure", failure(result));
        state.put("diagnostics", result.diagnostics());
        state.put("activationClaimAllowed", result.loaded() && result.registered());
        state.put("nativeHostMutationClaimAllowed", false);
        state.put("gameplayReadyClaimAllowed", false);
        return Map.copyOf(state);
    }

    private static Map<String, Object> descriptor(EchoNativeModuleDescriptor descriptor) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", descriptor.id());
        data.put("name", descriptor.name());
        data.put("version", descriptor.version());
        data.put("kind", descriptor.kind());
        data.put("role", descriptor.role());
        data.put("entrypoint", descriptor.entrypoint());
        data.put("side", descriptor.side().name());
        data.put("requires", descriptor.requires());
        data.put("optional", descriptor.optional());
        data.put("provides", descriptor.provides());
        data.put("descriptorPath", path(descriptor.descriptorPath()));
        data.put("nativeClasspathDeclared", descriptor.nativeClasspathDeclared());
        data.put("inferredClasspathRequested", descriptor.inferredClasspathRequested());
        data.put("compatibilityClasspathFallback", descriptor.compatibilityClasspathFallback());
        data.put("declaredClasspath", descriptor.declaredClasspath().stream()
                .map(EchoNativeLoadedModuleState::path)
                .toList());
        data.put("generatedClasspath", descriptor.generatedClasspath().stream()
                .map(EchoNativeLoadedModuleState::path)
                .toList());
        return Map.copyOf(data);
    }

    private static List<String> classpath(EchoNativeModuleDescriptor descriptor) {
        return descriptor.classpath().stream()
                .map(EchoNativeLoadedModuleState::path)
                .toList();
    }

    private static Map<String, Object> classloader(EchoNativeModuleLoadResult result) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("implementationClass", result.loadedClassLoaderName());
        data.put("loadedByModuleClassLoader", result.loadedByModuleClassLoader());
        data.put("loadedClassName", result.loadedClassName());
        return Map.copyOf(data);
    }

    private static List<Map<String, Object>> services(List<EchoNativeRegisteredService> services, boolean contentOnly) {
        return services.stream()
                .filter(service -> service.serviceId().startsWith("content.") == contentOnly)
                .map(service -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("moduleId", service.moduleId());
                    item.put("serviceId", service.serviceId());
                    item.put("moduleServiceKey", service.moduleId() + "::" + service.serviceId());
                    item.put("implementationClass", service.implementationClass());
                    item.put("serviceInstanceAttached", true);
                    item.put("serviceInstanceClass", service.implementationClass());
                    item.put("surfaces", service.surfaces());
                    return Map.copyOf(item);
                })
                .toList();
    }

    private static Map<String, Object> failure(EchoNativeModuleLoadResult result) {
        Map<String, Object> failure = new LinkedHashMap<>();
        boolean failed = "FAILED".equals(result.status().name()) || "UNSUPPORTED".equals(result.status().name());
        failure.put("failed", failed);
        failure.put("status", failed ? result.status().name() : "");
        failure.put("diagnostics", failed ? result.diagnostics() : List.of());
        return Map.copyOf(failure);
    }

    private static String path(Path path) {
        return path == null ? "" : path.toString().replace('\\', '/');
    }
}
