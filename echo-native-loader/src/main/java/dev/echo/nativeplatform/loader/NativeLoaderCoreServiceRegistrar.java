package dev.echo.nativeplatform.loader;

import dev.echo.nativeplatform.contracts.EchoNativeServiceRegistry;

import java.util.List;

public final class NativeLoaderCoreServiceRegistrar {
    public static final String CORE_MODULE_ID = "echocore";
    public static final String LOADER_MODULE_ID = "echo-native-loader";
    public static final String RUNTIME_HOST_ID = "echo-native-loader:native_loader_runtime_host";
    public static final String STANDALONE_MODULE_ID = "standalone";

    private NativeLoaderCoreServiceRegistrar() {
    }

    public static NativeLoaderLifecycleEventHost registerCoreServices(
            EchoNativeServiceRegistry serviceRegistry,
            String adapterCoreServiceId
    ) {
        return registerCoreServices(
                serviceRegistry,
                adapterCoreServiceId,
                NativeLoaderLiveRuntimeAttachment.unattached(),
                NativeLoaderLiveRuntimeBridge.UNATTACHED
        );
    }

    public static NativeLoaderLifecycleEventHost registerCoreServices(
            EchoNativeServiceRegistry serviceRegistry,
            String adapterCoreServiceId,
            NativeLoaderLiveRuntimeAttachment liveRuntimeAttachment,
            NativeLoaderLiveRuntimeBridge liveRuntimeBridge
    ) {
        if (serviceRegistry == null) {
            throw new IllegalArgumentException("serviceRegistry is required");
        }
        if (adapterCoreServiceId == null || adapterCoreServiceId.isBlank()) {
            throw new IllegalArgumentException("adapterCoreServiceId is required");
        }
        NativeLoaderLiveRuntimeAttachment safeAttachment = liveRuntimeAttachment == null
                ? NativeLoaderLiveRuntimeAttachment.unattached()
                : liveRuntimeAttachment;
        NativeLoaderLiveRuntimeBridge safeBridge = liveRuntimeBridge == null
                ? NativeLoaderLiveRuntimeBridge.UNATTACHED
                : liveRuntimeBridge;
        NativeLoaderRuntimeHost host;
        try {
            host = new NativeLoaderRuntimeHost(new NativeLoaderRuntimeHostContext(
                    LOADER_MODULE_ID,
                    LOADER_MODULE_ID,
                    serviceRegistry,
                    null,
                    RUNTIME_HOST_ID,
                    true,
                    safeAttachment,
                    safeBridge
            ));
        } catch (NoClassDefFoundError error) {
            throw new IllegalStateException("Native Loader core services require the full loader runtime classpath. "
                    + "registrarCodeSource=" + codeSource(NativeLoaderCoreServiceRegistrar.class)
                    + ", registrarClassLoader=" + classLoaderName(NativeLoaderCoreServiceRegistrar.class.getClassLoader())
                    + ", javaClassPath=" + System.getProperty("java.class.path", ""),
                    error);
        }
        NativeLoaderServiceBridge bridge = new NativeLoaderServiceBridge(serviceRegistry);
        NativeLoaderMutationLedger ledger = new NativeLoaderMutationLedger();
        NativeLoaderNetworkHost networkHost = new NativeLoaderNetworkHost(safeBridge);
        NativeLoaderConfigHost configHost = new NativeLoaderConfigHost(safeBridge);
        NativeLoaderCommandHost commandHost = new NativeLoaderCommandHost(safeBridge);
        NativeLoaderLifecycleEventHost lifecycleEventHost = new NativeLoaderLifecycleEventHost(safeBridge);
        NativeLoaderAdapterCoreBackend backend = new NativeLoaderAdapterCoreBackend(
                host,
                bridge,
                ledger,
                commandHost,
                networkHost,
                configHost,
                lifecycleEventHost
        );

        registerRuntimeHost(serviceRegistry, host);
        registerAdapterCoreBackend(serviceRegistry, adapterCoreServiceId, backend);
        registerRegistryHost(serviceRegistry, new EchoNativeRegistryHost());
        registerResourceHost(serviceRegistry, new NativeLoaderResourceHost());
        registerNetworkHost(serviceRegistry, networkHost);
        registerConfigHost(serviceRegistry, configHost);
        registerCommandHost(serviceRegistry, commandHost);
        NativeLoaderClientUiHost clientUiHost = new NativeLoaderClientUiHost();
        registerClientUiHost(serviceRegistry, clientUiHost);
        registerClientWindowPump(serviceRegistry, new NativeLoaderClientWindowPump(clientUiHost));
        registerLifecycleEventHost(serviceRegistry, lifecycleEventHost);
        return lifecycleEventHost;
    }

    public static NativeLoaderRuntimeHost createStandaloneRuntimeHost() {
        EchoNativeServiceRegistry serviceRegistry = new EchoNativeServiceRegistry();
        return new NativeLoaderRuntimeHost(new NativeLoaderRuntimeHostContext(
                STANDALONE_MODULE_ID,
                STANDALONE_MODULE_ID,
                serviceRegistry
        ));
    }

    private static String codeSource(Class<?> type) {
        try {
            if (type.getProtectionDomain() == null
                    || type.getProtectionDomain().getCodeSource() == null
                    || type.getProtectionDomain().getCodeSource().getLocation() == null) {
                return "";
            }
            return String.valueOf(type.getProtectionDomain().getCodeSource().getLocation());
        } catch (SecurityException exception) {
            return "unavailable:" + exception.getClass().getSimpleName();
        }
    }

    private static String classLoaderName(ClassLoader loader) {
        return loader == null ? "bootstrap" : loader.getClass().getName();
    }

    private static void registerRuntimeHost(EchoNativeServiceRegistry serviceRegistry, NativeLoaderRuntimeHost host) {
        serviceRegistry.register(
                CORE_MODULE_ID,
                NativeLoaderRuntimeHost.SERVICE_ID,
                host,
                List.of(
                        "runtime",
                        "adaptercore",
                        "inventory",
                        "player_state",
                        "world_blocks",
                        "world_state",
                        "structures",
                        "block_entities",
                        "capabilities",
                        "missions",
                        "events",
                        "networking",
                        "packets_hud",
                        "hud",
                        "save_data",
                        "client_tick",
                        "render_layers",
                        "screen_events",
                        "keybinds",
                        "commands",
                        "network_channels",
                        "config_reloads",
                        "resource_reloads",
                        "save_hooks",
                        "lifecycle_phases",
                        "server_client_sync"
                ),
                NativeLoaderRuntimeHost.class.getName()
        );
    }

    private static void registerAdapterCoreBackend(
            EchoNativeServiceRegistry serviceRegistry,
            String adapterCoreServiceId,
            NativeLoaderAdapterCoreBackend backend
    ) {
        serviceRegistry.register(
                LOADER_MODULE_ID,
                adapterCoreServiceId,
                backend,
                List.of(
                        "inventory",
                        "player_state",
                        "world_blocks",
                        "world_state",
                        "structures",
                        "block_entities",
                        "capabilities",
                        "missions",
                        "events",
                        "packets_hud",
                        "hud",
                        "save_data",
                        "client_tick",
                        "render_layers",
                        "screen_events",
                        "keybinds",
                        "commands",
                        "network_channels",
                        "config_reloads",
                        "resource_reloads",
                        "save_hooks",
                        "lifecycle_phases",
                        "server_client_sync"
                ),
                NativeLoaderAdapterCoreBackend.class.getName()
        );
    }

    private static void registerRegistryHost(EchoNativeServiceRegistry serviceRegistry, EchoNativeRegistryHost host) {
        serviceRegistry.register(
                CORE_MODULE_ID,
                EchoNativeRegistryHost.SERVICE_ID,
                host,
                List.of(
                        "registry",
                        "content",
                        "item",
                        "items",
                        "block",
                        "blocks",
                        "entity",
                        "entities",
                        "block_entity",
                        "menu",
                        "sound",
                        "particle",
                        "effect",
                        "effects",
                        "creative_tab",
                        "command",
                        "data_component"
                ),
                EchoNativeRegistryHost.class.getName()
        );
    }

    private static void registerResourceHost(EchoNativeServiceRegistry serviceRegistry, NativeLoaderResourceHost host) {
        serviceRegistry.register(
                CORE_MODULE_ID,
                NativeLoaderResourceHost.SERVICE_ID,
                host,
                List.of(
                        "resources",
                        "resource",
                        "assets",
                        "data",
                        "resource_pack",
                        "data_pack",
                        "recipes",
                        "loot",
                        "tags",
                        "sounds",
                        "structures",
                        "worldgen",
                        "world_preset",
                        "world_template",
                        "ui.screens",
                        "theme",
                        "theme_tokens",
                        "ui_skin",
                        "render_profile",
                        "asset_kit",
                        "block_palette",
                        "screen_markup",
                        "screen_layout",
                        "style",
                        "data_provider"
                ),
                NativeLoaderResourceHost.class.getName()
        );
    }

    private static void registerNetworkHost(EchoNativeServiceRegistry serviceRegistry, NativeLoaderNetworkHost host) {
        serviceRegistry.register(
                CORE_MODULE_ID,
                NativeLoaderNetworkHost.SERVICE_ID,
                host,
                List.of(
                        "network",
                        "networking",
                        "network_payload",
                        "packet",
                        "payload",
                        "packets",
                        "channels",
                        "network_channels",
                        "adaptercore.native_runtime_packet",
                        "packets_hud"
                ),
                NativeLoaderNetworkHost.class.getName()
        );
    }

    private static void registerConfigHost(EchoNativeServiceRegistry serviceRegistry, NativeLoaderConfigHost host) {
        serviceRegistry.register(
                CORE_MODULE_ID,
                NativeLoaderConfigHost.SERVICE_ID,
                host,
                List.of(
                        "config",
                        "configs",
                        "configuration",
                        "config_schema",
                        "config_reloads",
                        "client.config",
                        "server.config"
                ),
                NativeLoaderConfigHost.class.getName()
        );
    }

    private static void registerCommandHost(EchoNativeServiceRegistry serviceRegistry, NativeLoaderCommandHost host) {
        serviceRegistry.register(
                CORE_MODULE_ID,
                NativeLoaderCommandHost.SERVICE_ID,
                host,
                List.of(
                        "command",
                        "commands",
                        "server.commands",
                        "command.queue",
                        "adaptercore.native_command"
                ),
                NativeLoaderCommandHost.class.getName()
        );
    }

    private static void registerClientUiHost(EchoNativeServiceRegistry serviceRegistry, NativeLoaderClientUiHost host) {
        serviceRegistry.register(
                CORE_MODULE_ID,
                NativeLoaderClientUiHost.SERVICE_ID,
                host,
                List.of(
                        "client_ui",
                        "ui",
                        "ui_surface",
                        "ui_overlay",
                        "client_overlay",
                        "ui.screens",
                        "hud",
                        "hud_widget",
                        "hud_layout",
                        "screen",
                        "screen_surface",
                        "loading_screen",
                        "main_menu",
                        "world_setup",
                        "terminal",
                        "index",
                        "lens",
                        "holomap",
                        "client.screen.open"
                ),
                NativeLoaderClientUiHost.class.getName()
        );
    }

    private static void registerClientWindowPump(
            EchoNativeServiceRegistry serviceRegistry,
            NativeLoaderClientWindowPump pump
    ) {
        serviceRegistry.register(
                CORE_MODULE_ID,
                NativeLoaderClientWindowPump.SERVICE_ID,
                pump,
                List.of(
                        "client_window_pump",
                        "window_pump",
                        "client.tick",
                        "client.input",
                        "client.mouse",
                        "client.screen.lifecycle",
                        "client.overlay.focus",
                        "client.gui_layer",
                        "client.hud_layer"
                ),
                NativeLoaderClientWindowPump.class.getName()
        );
    }

    private static void registerLifecycleEventHost(
            EchoNativeServiceRegistry serviceRegistry,
            NativeLoaderLifecycleEventHost lifecycleEventHost
    ) {
        serviceRegistry.register(
                CORE_MODULE_ID,
                NativeLoaderLifecycleEventHost.LIFECYCLE_SERVICE_ID,
                lifecycleEventHost,
                List.of("lifecycle", "lifecycle_phases", "lifecycle.phases", "events", "adaptercore"),
                NativeLoaderLifecycleEventHost.class.getName()
        );
        serviceRegistry.register(
                CORE_MODULE_ID,
                NativeLoaderLifecycleEventHost.EVENT_SERVICE_ID,
                lifecycleEventHost,
                List.of("events", "lifecycle", "adaptercore"),
                NativeLoaderLifecycleEventHost.class.getName()
        );
    }
}
