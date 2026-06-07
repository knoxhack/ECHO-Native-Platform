package dev.echo.nativeplatform.contracts;

import java.util.List;
import java.util.Map;

@EchoNativeApiStatus(value = EchoNativeApiStability.BETA, since = "0.1.0-native-beta")
public record EchoNativeModuleLoadResult(
        EchoNativeModuleDescriptor descriptor,
        EchoNativeLoadStatus status,
        List<EchoNativeLifecyclePhase> phases,
        List<EchoNativeLifecycleRecord> lifecyclePhaseHistory,
        String loadedClassName,
        String loadedClassLoaderName,
        boolean loadedByModuleClassLoader,
        String constructedEntrypointClassName,
        List<String> resolvedDependencies,
        List<String> missingDependencies,
        List<EchoNativeRegisteredService> registeredServices,
        List<EchoNativeMutationReceipt> mutationReceipts,
        List<Map<String, Object>> mutations,
        List<String> diagnostics
) {
    public EchoNativeModuleLoadResult(
            EchoNativeModuleDescriptor descriptor,
            EchoNativeLoadStatus status,
            List<EchoNativeLifecyclePhase> phases,
            List<EchoNativeLifecycleRecord> lifecyclePhaseHistory,
            String loadedClassName,
            String loadedClassLoaderName,
            boolean loadedByModuleClassLoader,
            String constructedEntrypointClassName,
            List<String> resolvedDependencies,
            List<String> missingDependencies,
            List<EchoNativeRegisteredService> registeredServices,
            List<Map<String, Object>> mutations,
            List<String> diagnostics
    ) {
        this(
                descriptor,
                status,
                phases,
                lifecyclePhaseHistory,
                loadedClassName,
                loadedClassLoaderName,
                loadedByModuleClassLoader,
                constructedEntrypointClassName,
                resolvedDependencies,
                missingDependencies,
                registeredServices,
                List.of(),
                mutations,
                diagnostics
        );
    }

    public boolean loaded() {
        return status == EchoNativeLoadStatus.LOADED
                || status == EchoNativeLoadStatus.REGISTERED
                || status == EchoNativeLoadStatus.MUTATED;
    }

    public boolean registered() {
        return status == EchoNativeLoadStatus.REGISTERED || status == EchoNativeLoadStatus.MUTATED;
    }

    public boolean mutated() {
        return status == EchoNativeLoadStatus.MUTATED;
    }
}
