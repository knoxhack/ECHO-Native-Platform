package dev.echo.nativeplatform.loader;

import dev.echo.nativeplatform.contracts.EchoNativeLoadStatus;

import java.util.List;
import java.util.Map;

/**
 * Typed bridge from the Native Loader registry host into a live Minecraft registry path.
 *
 * <p>The registry host keeps deterministic native registration records for
 * package and module-readiness checks. Full registry parity attaches an
 * implementation of this bridge so item, block, entity, block entity, menu,
 * sound, particle, effect, command, data component, recipe, creative tab, biome,
 * worldgen, and client asset registrations are dispatched to the live runtime
 * registry instead of remaining only loader-side declarations.
 * Returning {@link EchoNativeLoadStatus#REGISTERED} records descriptor acceptance
 * only; release gates require first-class registry declarations to return
 * {@link EchoNativeLoadStatus#MUTATED} with a correlated
 * {@link #registryMutationRecord(String, String, String)} once the native
 * registry table changes.</p>
 */
public interface NativeLoaderLiveRegistryBridge {
    NativeLoaderLiveRegistryBridge UNATTACHED = new NativeLoaderLiveRegistryBridge() {
    };

    default boolean attached() {
        return false;
    }

    default String bridgeId() {
        return "native_loader:unattached_live_registry_bridge";
    }

    default boolean firstClassNativeRegistry() {
        return false;
    }

    default boolean nativeRegistryProcess() {
        return firstClassNativeRegistry();
    }

    default boolean releaseRegistryTrusted() {
        return firstClassNativeRegistry() && nativeRegistryProcess();
    }

    default boolean nativeRegistryMutationSupported() {
        return firstClassNativeRegistry() && releaseRegistryTrusted();
    }

    default Map<String, Object> registryEvidence() {
        return Map.of(
                "bridgeId", bridgeId(),
                "attached", attached(),
                "firstClassNativeRegistry", firstClassNativeRegistry(),
                "nativeRegistryProcess", nativeRegistryProcess(),
                "releaseRegistryTrusted", releaseRegistryTrusted(),
                "nativeRegistryMutationSupported", nativeRegistryMutationSupported(),
                "mutatedRecordCount", 0,
                "mutatedRecordIds", List.of(),
                "mutatedRecords", Map.of()
        );
    }

    default Map<String, Object> registryMutationRecord(String registry, String namespace, String id) {
        return Map.of();
    }

    default EchoNativeLoadStatus register(
            String registry,
            String namespace,
            String id,
            String implementationClass,
            Map<String, Object> properties
    ) {
        return EchoNativeLoadStatus.UNSUPPORTED;
    }
}
