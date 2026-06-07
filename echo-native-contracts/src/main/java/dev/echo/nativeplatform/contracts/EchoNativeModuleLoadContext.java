package dev.echo.nativeplatform.contracts;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoNativeModuleLoadContext {
    private final EchoNativeModuleDescriptor descriptor;
    private final EchoNativeServiceRegistry serviceRegistry;
    private final Map<String, Object> attributes;
    private final List<Map<String, Object>> mutations;
    private final EchoNativeMutationLedger mutationLedger;
    private final List<String> resolvedDependencies;
    private final List<String> missingDependencies;

    public EchoNativeModuleLoadContext(
            EchoNativeModuleDescriptor descriptor,
            EchoNativeServiceRegistry serviceRegistry,
            Map<String, Object> attributes
    ) {
        this.descriptor = descriptor;
        this.serviceRegistry = serviceRegistry;
        this.attributes = new LinkedHashMap<>(attributes);
        this.mutations = new ArrayList<>();
        this.mutationLedger = new EchoNativeMutationLedger();
        this.resolvedDependencies = new ArrayList<>();
        this.missingDependencies = new ArrayList<>();
    }

    public EchoNativeModuleDescriptor descriptor() {
        return descriptor;
    }

    public EchoNativeServiceRegistry serviceRegistry() {
        return serviceRegistry;
    }

    public Map<String, Object> attributes() {
        return Map.copyOf(attributes);
    }

    public void attribute(String key, Object value) {
        attributes.put(key, value);
    }

    public void registerService(String serviceId, Object service, String... surfaces) {
        serviceRegistry.register(descriptor.id(), serviceId, service, java.util.List.of(surfaces));
    }

    public void recordMutation(String surface, String action, String target, EchoNativeLoadStatus status) {
        Map<String, Object> mutation = new LinkedHashMap<>();
        mutation.put("surface", surface);
        mutation.put("action", action);
        mutation.put("target", target);
        mutation.put("status", status.name());
        mutation.put("moduleId", descriptor.id());
        mutations.add(mutation);
    }

    public void recordMutation(EchoNativeMutationReceipt receipt) {
        EchoNativeMutationReceipt typed = mutationLedger.append(receipt);
        mutations.add(typed.toReport());
    }

    public List<Map<String, Object>> mutations() {
        return List.copyOf(mutations);
    }

    public List<EchoNativeMutationReceipt> mutationReceipts() {
        return mutationLedger.receipts();
    }

    public void resolveDependency(String moduleId) {
        resolvedDependencies.add(moduleId);
    }

    public void missingDependency(String moduleId) {
        missingDependencies.add(moduleId);
    }

    public List<String> resolvedDependencies() {
        return List.copyOf(resolvedDependencies);
    }

    public List<String> missingDependencies() {
        return List.copyOf(missingDependencies);
    }
}
