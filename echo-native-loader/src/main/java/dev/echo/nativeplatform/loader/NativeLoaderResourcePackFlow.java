package dev.echo.nativeplatform.loader;

import dev.echo.nativeplatform.contracts.EchoNativeBootstrapProductProfile;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public final class NativeLoaderResourcePackFlow {
    public static final String SERVICE_ID = "echo.native.resource_pack_flow";
    private final EchoNativeBootstrapProductProfile profile;
    private final String moduleClasspathProperty;

    public NativeLoaderResourcePackFlow(
            EchoNativeBootstrapProductProfile profile,
            String moduleClasspathProperty
    ) {
        this.profile = profile;
        this.moduleClasspathProperty = moduleClasspathProperty == null ? "" : moduleClasspathProperty;
    }

    public Map<String, Object> apply(String packId, List<String> remainingArgs, List<String> modules) {
        return NativeLoaderProductResourceDatapackBridge.apply(
                profile,
                moduleClasspathProperty,
                packId,
                remainingArgs,
                modules
        );
    }

    public void installInternalModuleResourcePackMount(
            String packId,
            Map<String, Object> resourceBridge,
            List<String> modules,
            Path markerPath
    ) {
        NativeLoaderProductResourceDatapackBridge.installInternalModuleResourcePackMount(
                profile,
                packId,
                resourceBridge,
                modules,
                markerPath,
                NativeLoaderModuleResourcePack::startClientRepositoryMountThread
        );
    }
}
