package dev.echo.nativeplatform.bootstrap;

import dev.echo.nativeplatform.loader.NativeLoaderActivationModuleSnapshot;

import java.util.Map;

final class EchoNativeActivationModuleSnapshot {
    private EchoNativeActivationModuleSnapshot() {
    }

    static Map<String, Object> module(
            String id,
            Map<String, Object> activation,
            String creativeItemId,
            boolean creativeContentVisible,
            Map<String, Object> productGameplayBridge
    ) {
        return NativeLoaderActivationModuleSnapshot.module(
                id,
                activation,
                creativeItemId,
                creativeContentVisible,
                productGameplayBridge
        );
    }

    static boolean nativeActivationLoaded(Map<String, Object> activation) {
        return NativeLoaderActivationModuleSnapshot.nativeActivationLoaded(activation);
    }
}
