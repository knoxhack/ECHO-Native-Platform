package dev.echo.nativeplatform.loader;

import dev.echo.nativeplatform.contracts.EchoNativeServiceRegistry;

import java.nio.file.Path;

public record NativeLoaderRuntimeHostContext(
        String packId,
        String moduleId,
        EchoNativeServiceRegistry serviceRegistry,
        Path savesDirectory,
        String runtimeHostId,
        boolean runtimeHostRegistered,
        NativeLoaderLiveRuntimeAttachment liveRuntimeAttachment,
        NativeLoaderLiveRuntimeBridge liveRuntimeBridge
) {
    public NativeLoaderRuntimeHostContext(String packId, String moduleId, EchoNativeServiceRegistry serviceRegistry) {
        this(packId, moduleId, serviceRegistry, null);
    }

    public NativeLoaderRuntimeHostContext(
            String packId,
            String moduleId,
            EchoNativeServiceRegistry serviceRegistry,
            Path savesDirectory
    ) {
        this(
                packId,
                moduleId,
                serviceRegistry,
                savesDirectory,
                defaultRuntimeHostId(packId, moduleId),
                true,
                NativeLoaderLiveRuntimeAttachment.unattached(),
                NativeLoaderLiveRuntimeBridge.UNATTACHED
        );
    }

    public NativeLoaderRuntimeHostContext(
            String packId,
            String moduleId,
            EchoNativeServiceRegistry serviceRegistry,
            Path savesDirectory,
            String runtimeHostId,
            boolean runtimeHostRegistered
    ) {
        this(
                packId,
                moduleId,
                serviceRegistry,
                savesDirectory,
                runtimeHostId,
                runtimeHostRegistered,
                NativeLoaderLiveRuntimeAttachment.unattached(),
                NativeLoaderLiveRuntimeBridge.UNATTACHED
        );
    }

    public NativeLoaderRuntimeHostContext(
            String packId,
            String moduleId,
            EchoNativeServiceRegistry serviceRegistry,
            Path savesDirectory,
            NativeLoaderLiveRuntimeAttachment liveRuntimeAttachment
    ) {
        this(
                packId,
                moduleId,
                serviceRegistry,
                savesDirectory,
                defaultRuntimeHostId(packId, moduleId),
                true,
                liveRuntimeAttachment,
                NativeLoaderLiveRuntimeBridge.UNATTACHED
        );
    }

    public NativeLoaderRuntimeHostContext(
            String packId,
            String moduleId,
            EchoNativeServiceRegistry serviceRegistry,
            Path savesDirectory,
            NativeLoaderLiveRuntimeAttachment liveRuntimeAttachment,
            NativeLoaderLiveRuntimeBridge liveRuntimeBridge
    ) {
        this(
                packId,
                moduleId,
                serviceRegistry,
                savesDirectory,
                defaultRuntimeHostId(packId, moduleId),
                true,
                liveRuntimeAttachment,
                liveRuntimeBridge
        );
    }

    public NativeLoaderRuntimeHostContext {
        packId = packId == null || packId.isBlank() ? "unknown_pack" : packId.trim();
        moduleId = moduleId == null || moduleId.isBlank() ? "unknown_module" : moduleId.trim();
        runtimeHostId = runtimeHostId == null || runtimeHostId.isBlank()
                ? defaultRuntimeHostId(packId, moduleId)
                : runtimeHostId.trim();
        liveRuntimeAttachment = liveRuntimeAttachment == null
                ? NativeLoaderLiveRuntimeAttachment.unattached()
                : liveRuntimeAttachment;
        liveRuntimeBridge = liveRuntimeBridge == null
                ? NativeLoaderLiveRuntimeBridge.UNATTACHED
                : liveRuntimeBridge;
    }

    private static String defaultRuntimeHostId(String packId, String moduleId) {
        String normalizedPack = packId == null || packId.isBlank() ? "unknown_pack" : packId.trim();
        String normalizedModule = moduleId == null || moduleId.isBlank() ? "unknown_module" : moduleId.trim();
        return normalizedPack + ":" + normalizedModule + ":native_loader_runtime_host";
    }
}
