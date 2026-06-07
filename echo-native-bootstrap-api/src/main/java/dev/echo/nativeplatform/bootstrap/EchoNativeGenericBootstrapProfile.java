package dev.echo.nativeplatform.bootstrap;

import dev.echo.nativeplatform.contracts.EchoNativeBootstrapProductProfile;

import java.util.List;

final class EchoNativeGenericBootstrapProfile implements EchoNativeBootstrapProductProfile {
    static final EchoNativeBootstrapProductProfile INSTANCE = new EchoNativeGenericBootstrapProfile();

    private EchoNativeGenericBootstrapProfile() {
    }

    @Override
    public String namespace() {
        return "echo_native";
    }

    @Override
    public String nativeLoaderMainLabel() {
        return "ECHO Native Loader";
    }

    @Override
    public String nativeLoaderClientLabel() {
        return "ECHO Native Loader";
    }

    @Override
    public String nativeLoaderSessionMessage() {
        return "[ECHO Native Loader] Native product session active.";
    }

    @Override
    public String nativeLoaderWindowTitle() {
        return "ECHO Native Loader";
    }

    @Override
    public String nativeLoaderAdapterCoreServiceId() {
        return "adaptercore.native_loader.backend";
    }

    @Override
    public String nativeLoaderRuntimeHostClass() {
        return "";
    }

    @Override
    public String nativeMinecraftRuntimeHostClass() {
        return "";
    }

    @Override
    public String nativeMinecraftRuntimeHostId() {
        return "";
    }

    @Override
    public String nativeLoaderBackendClass() {
        return "dev.echo.nativeplatform.loader.NativeLoaderAdapterCoreBackend";
    }

    @Override
    public String nativeLoaderRuntimeLane() {
        return "Native Loader";
    }

    @Override
    public String nativeUiActionCommand() {
        return "native.ui.product_command";
    }

    @Override
    public List<String> requiredGameplayHandlerEvents() {
        return List.of(
                "player_join",
                "client_tick",
                "world_tick",
                "item_use",
                "block_place",
                "block_break",
                "entity_interact",
                "screen_open",
                "command_execution",
                "save_load",
                "resource_reload"
        );
    }

    @Override
    public List<String> requiredAgent7WorldLiveHooks() {
        return List.of();
    }

    @Override
    public List<String> requiredLiveMutationSurfaces() {
        return List.of(
                "inventory",
                "world_blocks",
                "save_data",
                "hud"
        );
    }

    @Override
    public List<NativeEntityDefinition> nativeEntities() {
        return List.of();
    }
}
