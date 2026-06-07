package dev.echo.nativeplatform.bootstrap;

import dev.echo.nativeplatform.contracts.EchoNativeAddonDescriptor;
import dev.echo.nativeplatform.contracts.EchoNativeApiStability;
import dev.echo.nativeplatform.contracts.EchoNativeModuleLoadResult;
import dev.echo.nativeplatform.contracts.EchoNativeRuntimeSide;
import dev.echo.nativeplatform.contracts.EchoNativeServiceRegistry;
import dev.echo.nativeplatform.contracts.EchoNativeTransformCompatibilityPolicy;
import dev.echo.nativeplatform.contracts.EchoNativeTrustLevel;
import dev.echo.nativeplatform.loader.EchoNativeDescriptorScanner;
import dev.echo.nativeplatform.loader.EchoNativeLoadedModuleStateStore;
import dev.echo.nativeplatform.loader.EchoNativeModuleLoader;
import dev.echo.nativeplatform.loader.EchoNativeScanResult;
import dev.echo.nativeplatform.loader.NativeLoaderCoreServiceRegistrar;
import dev.echo.nativeplatform.loader.NativeLoaderLifecycleEventHost;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

final class EchoNativeBootstrapActivationRunner {
    private EchoNativeBootstrapActivationRunner() {
    }

    static ActivationRun run(Config config) {
        if (config == null) {
            throw new IllegalArgumentException("config is required");
        }
        Map<String, Map<String, Object>> activations = new TreeMap<>();
        Map<String, EchoNativeAddonDescriptor> descriptors = nativeLifecycleDescriptors(config);
        Map<String, EchoNativeAddonDescriptor> runtimeDescriptors = runtimeDescriptors(config, descriptors);

        EchoNativeServiceRegistry serviceRegistry = new EchoNativeServiceRegistry();
        NativeLoaderLifecycleEventHost lifecycleEventHost = NativeLoaderCoreServiceRegistrar.registerCoreServices(
                serviceRegistry,
                config.adapterCoreServiceId()
        );
        EchoNativeModuleLoader loader = new EchoNativeModuleLoader(config.moduleClassLoader());
        EchoNativeLoadedModuleStateStore loadedModuleStateStore = new EchoNativeLoadedModuleStateStore();
        Path loadedModuleStateDirectory = loadedModuleStateDirectory(config.markerPath());
        for (EchoNativeAddonDescriptor descriptor : nativeLifecycleLoadOrder(runtimeDescriptors)) {
            try {
                EchoNativeModuleLoadResult result = loader.load(descriptor, serviceRegistry, runtimeDescriptors);
                lifecycleEventHost.publishSubscribedEventsForModule(
                        descriptor.id(),
                        Map.of(
                                "moduleId", descriptor.id(),
                                "status", result.status().name(),
                                "dispatchSource", "EchoNativeBootstrapActivationRunner"
                        ),
                        result.status()
                );
                lifecycleEventHost.recordModuleLoad(result);
                Map<String, Object> report = new LinkedHashMap<>(
                        config.reportFactory().create(config.packId(), result, lifecycleEventHost)
                );
                report.put("transformCompatibilityPolicy", transformCompatibilityPolicy(config.packId(), descriptor));
                attachLoadedModuleState(loadedModuleStateStore, loadedModuleStateDirectory, result, report);
                activations.put(descriptor.id(), Map.copyOf(report));
            } catch (Throwable exception) {
                activations.put(
                        descriptor.id(),
                        config.failureFactory().create(config.packId(), descriptor, exception)
                );
            }
        }
        for (Map.Entry<String, String> entry : config.nativeEntrypoints().entrySet()) {
            if (!activations.containsKey(entry.getKey())) {
                activations.put(
                        entry.getKey(),
                        config.unloadedFactory().create(config.packId(), entry.getKey(), entry.getValue())
                );
            }
        }
        return new ActivationRun(Map.copyOf(activations), serviceRegistry);
    }

    private static Map<String, EchoNativeAddonDescriptor> nativeLifecycleDescriptors(Config config) {
        Map<String, EchoNativeAddonDescriptor> descriptors = new TreeMap<>();
        if (config.fixtureRoot() != null) {
            try {
                EchoNativeScanResult scan = new EchoNativeDescriptorScanner().scan(config.fixtureRoot());
                for (EchoNativeAddonDescriptor descriptor : scan.descriptors()) {
                    String explicitEntrypoint = config.nativeEntrypoints().get(descriptor.id());
                    String descriptorEntrypoint = string(descriptor.access().get("nativeEntrypoint"));
                    if (explicitEntrypoint != null || !descriptorEntrypoint.isBlank()) {
                        descriptors.put(descriptor.id(), descriptorWithEntrypoint(descriptor, explicitEntrypoint));
                    }
                }
            } catch (RuntimeException ignored) {
                // Fallback descriptors below keep native loading explicit when fixture scanning is unavailable.
            }
        }
        config.nativeEntrypoints().forEach((moduleId, entrypoint) ->
                descriptors.putIfAbsent(moduleId, syntheticNativeDescriptor(moduleId, entrypoint, config.markerPath())));
        return Map.copyOf(descriptors);
    }

    private static Map<String, EchoNativeAddonDescriptor> runtimeDescriptors(
            Config config,
            Map<String, EchoNativeAddonDescriptor> descriptors
    ) {
        Map<String, EchoNativeAddonDescriptor> runtimeDescriptors = new TreeMap<>();
        descriptors.forEach((moduleId, descriptor) ->
                runtimeDescriptors.put(
                        moduleId,
                        descriptorWithRuntimeClasspath(
                                descriptor,
                                config.nativeEntrypoints().get(moduleId),
                                config.runtimeClasspath()
                        )
                ));
        return Map.copyOf(runtimeDescriptors);
    }

    private static void attachLoadedModuleState(
            EchoNativeLoadedModuleStateStore stateStore,
            Path stateDirectory,
            EchoNativeModuleLoadResult result,
            Map<String, Object> report
    ) {
        if (stateDirectory == null) {
            report.put("loadedModuleStateWritten", false);
            report.put("loadedModuleStatePath", "");
            return;
        }
        try {
            EchoNativeLoadedModuleStateStore.StoredState state = stateStore.write(stateDirectory, result);
            report.put("loadedModuleStateWritten", true);
            report.put("loadedModuleStatePath", state.normalizedPath());
            report.put("loadedModuleState", state.state());
        } catch (IOException exception) {
            report.put("loadedModuleStateWritten", false);
            report.put("loadedModuleStatePath", "");
            report.put("loadedModuleStateFailure", exception.getClass().getName() + ": "
                    + (exception.getMessage() == null ? "" : exception.getMessage()));
        }
    }

    private static EchoNativeAddonDescriptor descriptorWithEntrypoint(EchoNativeAddonDescriptor descriptor, String entrypoint) {
        if (entrypoint == null || entrypoint.isBlank()) {
            return descriptor;
        }
        Map<String, Object> access = new LinkedHashMap<>(descriptor.access());
        access.put("nativeEntrypoint", entrypoint);
        return descriptorWithAccess(descriptor, Map.copyOf(access));
    }

    private static EchoNativeAddonDescriptor descriptorWithRuntimeClasspath(
            EchoNativeAddonDescriptor descriptor,
            String entrypoint,
            List<String> runtimeClasspath
    ) {
        Map<String, Object> access = new LinkedHashMap<>(descriptorWithEntrypoint(descriptor, entrypoint).access());
        List<String> classpath = new ArrayList<>(stringList(access.get("nativeClasspath")));
        for (String item : runtimeClasspath) {
            if (!item.isBlank() && !classpath.contains(item)) {
                classpath.add(item);
            }
        }
        access.put("nativeClasspath", List.copyOf(classpath));
        return descriptorWithAccess(descriptor, Map.copyOf(access));
    }

    private static EchoNativeAddonDescriptor descriptorWithAccess(
            EchoNativeAddonDescriptor descriptor,
            Map<String, Object> access
    ) {
        return new EchoNativeAddonDescriptor(
                descriptor.schema(),
                descriptor.id(),
                descriptor.name(),
                descriptor.version(),
                descriptor.kind(),
                descriptor.role(),
                descriptor.entrypoint(),
                descriptor.side(),
                descriptor.trustLevel(),
                descriptor.apiStability(),
                descriptor.official(),
                descriptor.standalone(),
                descriptor.requires(),
                descriptor.optional(),
                descriptor.provides(),
                descriptor.consumes(),
                descriptor.transforms(),
                access,
                descriptor.descriptorPath()
        );
    }

    private static EchoNativeAddonDescriptor syntheticNativeDescriptor(String moduleId, String entrypoint, Path markerPath) {
        Map<String, Object> access = new LinkedHashMap<>();
        access.put("nativeEntrypoint", entrypoint);
        return new EchoNativeAddonDescriptor(
                "echo.mod.v1",
                moduleId,
                moduleId,
                "0.0.0",
                "addon",
                "native_module",
                entrypoint,
                EchoNativeRuntimeSide.COMMON,
                EchoNativeTrustLevel.OFFICIAL,
                EchoNativeApiStability.BETA,
                true,
                true,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                Map.copyOf(access),
                markerPath == null ? null : markerPath.toAbsolutePath().normalize()
        );
    }

    private static List<EchoNativeAddonDescriptor> nativeLifecycleLoadOrder(
            Map<String, EchoNativeAddonDescriptor> descriptors
    ) {
        List<EchoNativeAddonDescriptor> ordered = new ArrayList<>();
        Set<String> loaded = new HashSet<>();
        List<EchoNativeAddonDescriptor> remaining = new ArrayList<>(descriptors.values());
        remaining.sort(Comparator.comparing(EchoNativeAddonDescriptor::id));
        while (!remaining.isEmpty()) {
            int before = remaining.size();
            for (int index = 0; index < remaining.size(); index++) {
                EchoNativeAddonDescriptor descriptor = remaining.get(index);
                boolean dependenciesReady = descriptor.requires().stream()
                        .filter(descriptors::containsKey)
                        .allMatch(loaded::contains);
                if (dependenciesReady) {
                    ordered.add(descriptor);
                    loaded.add(descriptor.id());
                    remaining.remove(index);
                    index--;
                }
            }
            if (remaining.size() == before) {
                ordered.addAll(remaining.stream()
                        .sorted(Comparator.comparing(EchoNativeAddonDescriptor::id))
                        .toList());
                break;
            }
        }
        return List.copyOf(ordered);
    }

    private static Path loadedModuleStateDirectory(Path markerPath) {
        if (markerPath == null) {
            return null;
        }
        return markerPath.toAbsolutePath().normalize().getParent().resolve("loaded-modules");
    }

    private static Map<String, Object> transformCompatibilityPolicy(
            String packId,
            EchoNativeAddonDescriptor descriptor
    ) {
        EchoNativeTransformCompatibilityPolicy.TransformCompatibilityReport report =
                EchoNativeTransformCompatibilityPolicy.evaluate(packId, descriptor);
        Map<String, Object> policy = new LinkedHashMap<>();
        policy.put("policyId", report.policyId());
        policy.put("moduleId", report.moduleId());
        policy.put("hasTransformRequests", report.hasTransformRequests());
        policy.put("compatible", report.compatible());
        policy.put("decision", report.policyDecision());
        policy.put("bytecodeMutationAllowed", report.bytecodeMutationAllowed());
        policy.put("minecraftBytecodeMutationAllowed", report.minecraftBytecodeMutationAllowed());
        policy.put("addonBytecodeMutationAllowed", report.addonBytecodeMutationAllowed());
        policy.put("bytecodeMutated", false);
        policy.put("transformPlanningOnly", true);
        policy.put("transformsEnabled", false);
        policy.put("declaredForgeStyleTransforms", report.declaredForgeStyleTransforms());
        policy.put("declaredNativeReplacements", report.declaredNativeReplacements());
        policy.put("declaredReplacementMappings", report.declaredReplacementMappings());
        policy.put("supportedNativeDeclarations", report.supportedNativeDeclarations());
        policy.put("replacementCoverageComplete", report.replacementCoverageComplete());
        policy.put("nativeProjectionReplacementPlanned", report.nativeProjectionReplacementPlanned());
        policy.put("plannedNativeProjectionCount", report.plannedNativeProjectionCount());
        policy.put("unmappedForgeStyleTransforms", report.unmappedForgeStyleTransforms());
        policy.put("unknownMappedForgeStyleTransforms", report.unknownMappedForgeStyleTransforms());
        policy.put("releasePolicySummary", report.releasePolicySummary());
        policy.put("diagnosticPathSummary", EchoNativeTransformCompatibilityPolicy.diagnosticPathSummary());
        return Map.copyOf(policy);
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

    private static String string(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    record Config(
            String packId,
            Map<String, String> nativeEntrypoints,
            Path markerPath,
            Path fixtureRoot,
            List<String> runtimeClasspath,
            ClassLoader moduleClassLoader,
            String adapterCoreServiceId,
            ActivationReportFactory reportFactory,
            FailureReportFactory failureFactory,
            UnloadedReportFactory unloadedFactory
    ) {
        Config {
            packId = packId == null || packId.isBlank() ? "native-loader" : packId;
            nativeEntrypoints = nativeEntrypoints == null ? Map.of() : Map.copyOf(nativeEntrypoints);
            runtimeClasspath = runtimeClasspath == null ? List.of() : List.copyOf(runtimeClasspath);
            moduleClassLoader = moduleClassLoader == null
                    ? EchoNativeBootstrapActivationRunner.class.getClassLoader()
                    : moduleClassLoader;
            if (adapterCoreServiceId == null || adapterCoreServiceId.isBlank()) {
                throw new IllegalArgumentException("adapterCoreServiceId is required");
            }
            if (reportFactory == null) {
                throw new IllegalArgumentException("reportFactory is required");
            }
            if (failureFactory == null) {
                throw new IllegalArgumentException("failureFactory is required");
            }
            if (unloadedFactory == null) {
                throw new IllegalArgumentException("unloadedFactory is required");
            }
        }
    }

    record ActivationRun(
            Map<String, Map<String, Object>> activations,
            EchoNativeServiceRegistry serviceRegistry
    ) {
    }

    @FunctionalInterface
    interface ActivationReportFactory {
        Map<String, Object> create(
                String packId,
                EchoNativeModuleLoadResult result,
                NativeLoaderLifecycleEventHost lifecycleEventHost
        );
    }

    @FunctionalInterface
    interface FailureReportFactory {
        Map<String, Object> create(String packId, EchoNativeAddonDescriptor descriptor, Throwable exception);
    }

    @FunctionalInterface
    interface UnloadedReportFactory {
        Map<String, Object> create(String packId, String moduleId, String entrypoint);
    }
}
