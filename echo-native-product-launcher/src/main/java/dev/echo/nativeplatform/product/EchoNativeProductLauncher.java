package dev.echo.nativeplatform.product;

import dev.echo.nativeplatform.contracts.EchoNativeAddonDescriptor;
import dev.echo.nativeplatform.contracts.EchoNativeDiagnostic;
import dev.echo.nativeplatform.contracts.EchoNativeIssueSeverity;
import dev.echo.nativeplatform.contracts.EchoNativeLoadStatus;
import dev.echo.nativeplatform.contracts.EchoNativeModuleDescriptor;
import dev.echo.nativeplatform.contracts.EchoNativeModuleEntrypoint;
import dev.echo.nativeplatform.contracts.EchoNativePackProfile;
import dev.echo.nativeplatform.contracts.EchoNativeRegisteredService;
import dev.echo.nativeplatform.contracts.EchoNativeModuleLoadResult;
import dev.echo.nativeplatform.contracts.EchoNativeServiceRegistry;
import dev.echo.nativeplatform.contracts.EchoNativeTransformCompatibilityPolicy;
import dev.echo.nativeplatform.loader.EchoNativeDescriptorScanner;
import dev.echo.nativeplatform.loader.EchoNativeModuleClassLoader;
import dev.echo.nativeplatform.loader.EchoNativeModuleLoadTruthGate;
import dev.echo.nativeplatform.loader.EchoNativeModuleLoader;
import dev.echo.nativeplatform.loader.EchoNativeRegistryHost;
import dev.echo.nativeplatform.loader.EchoNativeScanResult;
import dev.echo.nativeplatform.loader.NativeLoaderAdapterCoreBackend;
import dev.echo.nativeplatform.loader.NativeLoaderClientUiHost;
import dev.echo.nativeplatform.loader.NativeLoaderClientRouteTable;
import dev.echo.nativeplatform.loader.NativeLoaderCommandHost;
import dev.echo.nativeplatform.loader.NativeLoaderConfigHost;
import dev.echo.nativeplatform.loader.NativeLoaderLifecycleEventHost;
import dev.echo.nativeplatform.loader.NativeLoaderLiveClientBridge;
import dev.echo.nativeplatform.loader.NativeLoaderLiveRegistryBridge;
import dev.echo.nativeplatform.loader.NativeLoaderLiveRuntimeBridge;
import dev.echo.nativeplatform.loader.NativeLoaderLiveRuntimeAttachment;
import dev.echo.nativeplatform.loader.NativeLoaderMutationLedger;
import dev.echo.nativeplatform.loader.NativeLoaderNetworkHost;
import dev.echo.nativeplatform.loader.NativeLoaderRuntimeHost;
import dev.echo.nativeplatform.loader.NativeLoaderRuntimeHostContext;
import dev.echo.nativeplatform.loader.NativeLoaderResourceHost;
import dev.echo.nativeplatform.loader.NativeLoaderServiceBridge;
import dev.echo.nativeplatform.loader.NativeLoaderTypedHostService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Product-oriented native launcher path.
 *
 * <p>This path scans descriptors, registers the native host services, and loads
 * module entrypoints directly. It does not require report/planner inputs and it
 * does not write report artifacts.</p>
 */
public final class EchoNativeProductLauncher {
    private final EchoNativeDescriptorScanner scanner = new EchoNativeDescriptorScanner();
    private final EchoNativeModuleLoader moduleLoader = new EchoNativeModuleLoader();
    private final EchoNativeModuleLoadTruthGate truthGate = new EchoNativeModuleLoadTruthGate();

    public EchoNativeProductLaunchOutcome launch(Path productRoot, boolean requireMutation) throws IOException {
        return launch(productRoot, new EchoNativeProductLaunchOptions(requireMutation, true, true));
    }

    public EchoNativeProductLaunchOutcome launchModuleOnly(Path productRoot, boolean requireMutation) throws IOException {
        return launch(productRoot, new EchoNativeProductLaunchOptions(requireMutation, false, false));
    }

    public EchoNativeProductLaunchOutcome launch(Path productRoot, EchoNativeProductLaunchOptions options) throws IOException {
        EchoNativeScanResult scanResult = scanner.scanProduct(productRoot);
        String packId = scanResult.packProfile() == null ? "unknown_pack" : scanResult.packProfile().id();
        List<EchoNativeDiagnostic> diagnostics = new ArrayList<>(scanResult.diagnostics());
        boolean requireMutation = options != null && options.requireMutation();
        boolean releaseMode = options != null && options.releaseMode();
        boolean requireLiveRuntime = options != null && options.requireLiveRuntime();
        EchoNativeProductBootstrapProfileReport bootstrapProfile = scanResult.packProfile() == null
                ? EchoNativeProductBootstrapProfileReport.unavailable(packId)
                : EchoNativeProductBootstrapProfileResolver.resolve(scanResult.packProfile(), scanResult.descriptors());
        if (releaseMode) {
            diagnostics.addAll(releaseBootstrapProfileDiagnostics(packId, bootstrapProfile));
        }
        if (scanResult.packProfile() == null || hasBlocking(diagnostics)) {
            return new EchoNativeProductLaunchOutcome(
                    packId,
                    0,
                    0,
                    0,
                    0,
                    0,
                    requireMutation,
                    releaseMode,
                    requireLiveRuntime,
                    false,
                    bootstrapProfile,
                    EchoNativeProductPreWindowAssertionReport.unavailable(),
                    EchoNativeProductRuntimeCapabilityReport.unavailable(),
                    EchoNativeProductHookReport.empty(),
                    List.of(),
                    List.copyOf(diagnostics)
            );
        }

        Map<String, EchoNativeAddonDescriptor> availableModules = new LinkedHashMap<>();
        for (EchoNativeAddonDescriptor descriptor : scanResult.descriptors()) {
            availableModules.put(descriptor.id(), descriptor);
            if (releaseMode) {
                diagnostics.addAll(releaseClasspathDiagnostics(packId, descriptor));
                diagnostics.addAll(releaseEntrypointApiDiagnostics(packId, descriptor));
            }
            diagnostics.addAll(EchoNativeTransformCompatibilityPolicy.evaluate(packId, descriptor).diagnostics());
        }
        EchoNativeProductLaunchOptions effectiveOptions = options == null
                ? new EchoNativeProductLaunchOptions(requireMutation, releaseMode, requireLiveRuntime)
                : options;
        EchoNativeProductBridgeProviderResolver.Resolution bridgeProviderResolution =
                EchoNativeProductBridgeProviderResolver.resolve(
                        productRoot,
                        packId,
                        scanResult.descriptors(),
                        effectiveOptions
                );
        effectiveOptions = bridgeProviderResolution.options();
        diagnostics.addAll(bridgeProviderResolution.diagnostics());
        requireMutation = effectiveOptions.requireMutation();
        releaseMode = effectiveOptions.releaseMode();
        requireLiveRuntime = effectiveOptions.requireLiveRuntime();
        if (hasBlocking(diagnostics)) {
            return new EchoNativeProductLaunchOutcome(
                    packId,
                    scanResult.descriptors().size(),
                    0,
                    0,
                    0,
                    scanResult.descriptors().size(),
                    requireMutation,
                    releaseMode,
                    requireLiveRuntime,
                    false,
                    bootstrapProfile,
                    EchoNativeProductPreWindowAssertionReport.unavailable(),
                    EchoNativeProductRuntimeCapabilityReport.unavailable(),
                    EchoNativeProductHookReport.empty(),
                    scanResult.descriptors().stream()
                            .map(descriptor -> new EchoNativeProductModuleLaunch(
                                    descriptor.id(),
                                    EchoNativeLoadStatus.FAILED,
                                    EchoNativeLoadStatus.FAILED,
                                    false,
                                    false,
                                    false,
                                    false,
                                    List.of(),
                                    List.of("Release preflight blocked native product launch before module loading.")
                            ))
                            .toList(),
                    List.copyOf(diagnostics)
            );
        }

        NativeHostServices nativeHostServices = registerNativeHostServices(productRoot, packId, effectiveOptions);
        for (EchoNativeAddonDescriptor descriptor : availableModules.values()) {
            registerDescriptorIntent(nativeHostServices, descriptor);
        }
        List<EchoNativeProductHookExecution> hookExecutions = new ArrayList<>(applyProductHooks(
                nativeHostServices,
                effectiveOptions.hookPlan(),
                false
        ));
        List<EchoNativeProductModuleLaunch> moduleLaunches = new ArrayList<>();
        List<EchoNativeModuleLoadResult> moduleResults = new ArrayList<>();
        int loaded = 0;
        int registered = 0;
        int mutated = 0;
        int failed = 0;

        for (EchoNativeAddonDescriptor descriptor : availableModules.values()) {
            EchoNativeModuleLoadResult result = moduleLoader.load(
                    descriptor,
                    nativeHostServices.serviceRegistry(),
                    availableModules
            );
            moduleResults.add(result);
            nativeHostServices.lifecycleEventHost().recordModuleLoad(result);
            EchoNativeModuleLoadTruthGate.TruthReport truth = truthGate.verify(result);
            boolean accepted = truth.loaded() && truth.registered() && (!requireMutation || truth.mutated());
            if (truth.loaded()) {
                loaded++;
            }
            if (truth.registered()) {
                registered++;
            }
            if (truth.mutated()) {
                mutated++;
            }
            if (!accepted || result.status() == EchoNativeLoadStatus.FAILED || result.status() == EchoNativeLoadStatus.UNSUPPORTED) {
                failed++;
            }
            moduleLaunches.add(new EchoNativeProductModuleLaunch(
                    descriptor.id(),
                    result.status(),
                    truth.honestStatus(),
                    truth.loaded(),
                    truth.registered(),
                    truth.mutated(),
                    accepted,
                    result.diagnostics(),
                    truth.failures()
            ));
        }
        hookExecutions.addAll(applyProductHooks(
                nativeHostServices,
                effectiveOptions.hookPlan(),
                true
        ));
        for (EchoNativeAddonDescriptor descriptor : availableModules.values()) {
            registerDescriptorCreativeTabIntent(nativeHostServices, descriptor);
        }
        EchoNativeProductHookReport hooks = hookReport(nativeHostServices, hookExecutions);
        EchoNativeProductRuntimeCapabilityReport runtimeCapabilities = runtimeCapabilityReport(nativeHostServices);
        EchoNativeProductPreWindowAssertionReport preWindowAssertions = preWindowAssertionReport(
                scanResult,
                bootstrapProfile,
                nativeHostServices,
                runtimeCapabilities,
                requireLiveRuntime
        );
        shutdownLoadedModules(moduleResults, nativeHostServices);
        if (releaseMode) {
            diagnostics.addAll(releaseRuntimeCapabilityDiagnostics(packId, runtimeCapabilities));
            diagnostics.addAll(preWindowAssertionDiagnostics(packId, preWindowAssertions, requireLiveRuntime));
        }
        boolean modulesAccepted = failed == 0 && loaded == scanResult.descriptors().size()
                && registered == scanResult.descriptors().size()
                && (!requireMutation || mutated == scanResult.descriptors().size());
        boolean runtimeAccepted = !requireLiveRuntime || runtimeCapabilities.fullReleaseRuntimeReady();
        boolean preWindowAccepted = preWindowAssertions.moduleReleaseReady()
                && (!requireLiveRuntime || preWindowAssertions.productWindowReady());
        boolean accepted = modulesAccepted && runtimeAccepted && (!releaseMode || preWindowAccepted);

        if (!modulesAccepted) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-PRODUCT-LAUNCH-INCOMPLETE",
                    EchoNativeIssueSeverity.ERROR,
                    "Native product launch did not reach the required module state",
                    "The product launcher ran without report inputs, but at least one module failed the required loaded/registered"
                            + (requireMutation ? "/mutated" : "")
                            + " gates.",
                    null,
                    packId,
                    List.of(),
                    "Inspect the printed module status and fix classpath, dependency, service registration, or runtime hook gaps."
            ));
        }
        if (!runtimeAccepted) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-PRODUCT-RUNTIME-INCOMPLETE",
                    EchoNativeIssueSeverity.ERROR,
                    "Native product launch requires a trusted runtime/client surface",
                    "The launch was asked to require release runtime parity, but the Native Loader host still lacks a trusted native/live runtime dispatch path or the client UI host is headless.",
                    null,
                    packId,
                    List.of(
                            "echo-native-platform/echo-native-loader/src/main/java/dev/echo/nativeplatform/loader/NativeLoaderRuntimeHost.java",
                            "echo-native-platform/echo-native-loader/src/main/java/dev/echo/nativeplatform/loader/NativeLoaderClientUiHost.java"
                    ),
                    "Attach a trusted first-class Native Loader runtime and client/render pipeline, or run without --release for module-only launch checks."
            ));
        }

        return new EchoNativeProductLaunchOutcome(
                packId,
                scanResult.descriptors().size(),
                loaded,
                registered,
                mutated,
                failed,
                requireMutation,
                releaseMode,
                requireLiveRuntime,
                accepted,
                bootstrapProfile,
                preWindowAssertions,
                runtimeCapabilities,
                hooks,
                List.copyOf(moduleLaunches),
                List.copyOf(diagnostics)
        );
    }

    private void shutdownLoadedModules(
            List<EchoNativeModuleLoadResult> moduleResults,
            NativeHostServices nativeHostServices
    ) {
        for (EchoNativeModuleLoadResult moduleResult : moduleResults) {
            moduleLoader.shutdown(moduleResult, nativeHostServices.serviceRegistry());
        }
    }

    private static NativeHostServices registerNativeHostServices(
            Path productRoot,
            String packId,
            EchoNativeProductLaunchOptions options
    ) {
        EchoNativeServiceRegistry serviceRegistry = new EchoNativeServiceRegistry();
        NativeLoaderLiveRuntimeAttachment liveRuntimeAttachment = options == null
                ? NativeLoaderLiveRuntimeAttachment.unattached()
                : options.liveRuntimeAttachment();
        NativeLoaderLiveRuntimeBridge liveRuntimeBridge = options == null
                ? NativeLoaderLiveRuntimeBridge.UNATTACHED
                : options.liveRuntimeBridge();
        EchoNativeRegistryHost registryHost = new EchoNativeRegistryHost();
        registryHost.attachLiveBridge(options == null ? NativeLoaderLiveRegistryBridge.UNATTACHED : options.liveRegistryBridge());
        serviceRegistry.register("echocore", EchoNativeRegistryHost.SERVICE_ID, registryHost,
                nativeRegistryServiceSurfaces(),
                EchoNativeRegistryHost.class.getName());
        NativeLoaderCommandHost commandHost = new NativeLoaderCommandHost(liveRuntimeBridge);
        serviceRegistry.register("echocore", NativeLoaderCommandHost.SERVICE_ID, commandHost,
                List.of("commands", "command", "server.commands", "command.queue", "adaptercore.native_command"),
                NativeLoaderCommandHost.class.getName());
        NativeLoaderConfigHost configHost = new NativeLoaderConfigHost(liveRuntimeBridge);
        serviceRegistry.register("echocore", NativeLoaderConfigHost.SERVICE_ID, configHost,
                List.of("config", "configs", "configuration", "config_schema", "config_reloads", "client.config", "server.config"),
                NativeLoaderConfigHost.class.getName());
        NativeLoaderResourceHost resourceHost = new NativeLoaderResourceHost();
        serviceRegistry.register("echocore", NativeLoaderResourceHost.SERVICE_ID, resourceHost,
                List.of("resources", "resource", "assets", "data", "resource_pack", "data_pack",
                        "recipes", "loot", "tags", "worldgen", "world_preset", "world_template",
                        "ui_screens", "theme", "theme_tokens", "ui_skin", "render_profile",
                        "asset_kit", "block_palette", "screen_markup", "screen_layout", "style",
                        "data_provider"),
                NativeLoaderResourceHost.class.getName());
        NativeLoaderNetworkHost networkHost = new NativeLoaderNetworkHost(liveRuntimeBridge);
        serviceRegistry.register("echocore", NativeLoaderNetworkHost.SERVICE_ID, networkHost,
                List.of("network", "networking", "network_payload", "packet", "payload", "packets",
                        "channels", "network_channels", "adaptercore.native_runtime_packet", "packets_hud", "server_client_sync"),
                NativeLoaderNetworkHost.class.getName());

        NativeLoaderRuntimeHostContext hostContext = new NativeLoaderRuntimeHostContext(
                packId,
                "echocore",
                serviceRegistry,
                productRoot.toAbsolutePath().normalize().resolve("runtime").resolve("saves"),
                liveRuntimeAttachment,
                liveRuntimeBridge
        );
        NativeLoaderRuntimeHost runtimeHost = new NativeLoaderRuntimeHost(hostContext);
        NativeLoaderServiceBridge serviceBridge = new NativeLoaderServiceBridge(serviceRegistry);
        NativeLoaderMutationLedger ledger = new NativeLoaderMutationLedger();
        NativeLoaderLifecycleEventHost lifecycleEventHost = new NativeLoaderLifecycleEventHost(liveRuntimeBridge);
        lifecycleEventHost.subscribe("echocore", "echo_native.module_load_completed", event -> Map.of(
                "handler", "echo-native-product-launcher.module-load",
                "moduleId", String.valueOf(event.payload().getOrDefault("moduleId", "")),
                "status", event.status()
        ));
        serviceRegistry.register("echocore", NativeLoaderRuntimeHost.SERVICE_ID, runtimeHost,
                runtimeHost.supportedSurfaces(),
                NativeLoaderRuntimeHost.class.getName());
        serviceRegistry.register("echocore", NativeLoaderLifecycleEventHost.LIFECYCLE_SERVICE_ID, lifecycleEventHost,
                List.of("lifecycle", "lifecycle.phases", "common_setup", "client_setup", "server_setup", "ready",
                        "shutdown", "lifecycle_phases"),
                NativeLoaderLifecycleEventHost.class.getName());
        serviceRegistry.register("echocore", NativeLoaderLifecycleEventHost.EVENT_SERVICE_ID, lifecycleEventHost,
                List.of("events", "event", "module.load", "module.ready", "world.tick", "player", "runtime.spine"),
                NativeLoaderLifecycleEventHost.class.getName());
        NativeLoaderAdapterCoreBackend backend = new NativeLoaderAdapterCoreBackend(
                runtimeHost,
                serviceBridge,
                ledger,
                commandHost,
                networkHost,
                configHost,
                lifecycleEventHost);
        serviceRegistry.register("echocore", NativeLoaderAdapterCoreBackend.SERVICE_ID, backend,
                List.of("adaptercore", "inventory", "player_state", "world_blocks", "world_state", "structures",
                        "block_entities", "capabilities", "events", "packets_hud", "hud", "save_data",
                        "missions", "feedback", "client_tick", "render_layers", "screen_events", "keybinds",
                        "commands", "network_channels", "config_reloads", "resource_reloads", "save_hooks",
                        "lifecycle_phases", "server_client_sync"),
                NativeLoaderAdapterCoreBackend.class.getName());

        NativeLoaderClientUiHost clientUiHost = new NativeLoaderClientUiHost();
        clientUiHost.attach(options == null ? headlessClientAssessment() : options.clientAttachmentAssessment());
        clientUiHost.attachLiveBridge(options == null ? NativeLoaderLiveClientBridge.UNATTACHED : options.liveClientBridge());
        serviceRegistry.register("echocore", NativeLoaderClientUiHost.SERVICE_ID, clientUiHost,
                List.of("ui", "surfaces", "client", "hud", "hud_widget", "hud_layout", "screens", "screen_surface",
                        "overlays", "client_overlay", "loading_screen", "main_menu", "terminal", "index", "lens",
                        "holomap"),
                NativeLoaderClientUiHost.class.getName());
        registerTypedAsdkHostServices(serviceRegistry);

        return new NativeHostServices(
                serviceRegistry,
                registryHost,
                lifecycleEventHost,
                commandHost,
                configHost,
                resourceHost,
                networkHost,
                backend,
                runtimeHost,
                clientUiHost
        );
    }

    private static void registerTypedAsdkHostServices(EchoNativeServiceRegistry serviceRegistry) {
        registerTypedAsdkHostService(serviceRegistry, "echo.native.registry", "registry", "registries", "content");
        registerTypedAsdkHostService(serviceRegistry, "echo.native.lifecycle", "lifecycle", "game_tests", "health");
        registerTypedAsdkHostService(serviceRegistry, "echo.native.events", "events", "subscriptions", "publish");
        registerTypedAsdkHostService(serviceRegistry, "echo.native.commands", "commands", "server.commands");
        registerTypedAsdkHostService(serviceRegistry, "echo.native.config", "config", "configs", "reload");
        registerTypedAsdkHostService(serviceRegistry, "echo.native.network", "network", "packets", "payloads");
        registerTypedAsdkHostService(serviceRegistry, "echo.native.resources", "resources", "assets", "data", "datagen");
        registerTypedAsdkHostService(serviceRegistry, "echo.native.capabilities", "capabilities", "integrations");
        registerTypedAsdkHostService(serviceRegistry, "echo.native.attachments", "attachments", "player.data");
        registerTypedAsdkHostService(serviceRegistry, "echo.native.worldgen", "worldgen", "features", "structures");
        registerTypedAsdkHostService(serviceRegistry, "echo.native.render", "render", "hud", "overlays");
        registerTypedAsdkHostService(serviceRegistry, "echo.native.screens", "screens", "menus", "keybinds");
        registerTypedAsdkHostService(serviceRegistry, "echo.native.save_data", "save_data", "world.save", "module.save");
    }

    private static void registerTypedAsdkHostService(
            EchoNativeServiceRegistry serviceRegistry,
            String serviceId,
            String... surfaces
    ) {
        serviceRegistry.register(
                "echocore",
                serviceId,
                new NativeLoaderTypedHostService(serviceId),
                List.of(surfaces),
                NativeLoaderTypedHostService.class.getName());
    }

    private static List<String> nativeRegistryServiceSurfaces() {
        LinkedHashSet<String> surfaces = new LinkedHashSet<>();
        surfaces.add("registry");
        for (String kind : EchoNativeRegistryHost.firstClassRegistryKinds()) {
            surfaces.add(kind);
            surfaces.add(switch (kind) {
                case "item" -> "items";
                case "block" -> "blocks";
                case "entity" -> "entities";
                case "block_entity" -> "block_entities";
                case "menu" -> "menus";
                case "sound" -> "sounds";
                case "particle" -> "particles";
                case "effect" -> "effects";
                case "command" -> "commands";
                case "data_component" -> "data_components";
                case "recipe" -> "recipes";
                case "creative_tab" -> "creative_tabs";
                case "biome" -> "biomes";
                case "worldgen" -> "world_generators";
                case "client_asset" -> "client_assets";
                default -> kind;
            });
        }
        surfaces.add("worldgens");
        return List.copyOf(surfaces);
    }

    private static Map<String, Object> headlessClientAssessment() {
        return Map.of(
                "launcher", "echo-native-product-launcher",
                "liveClientAttached", false,
                "headlessClientSurface", true
        );
    }

    private static void registerDescriptorIntent(NativeHostServices services, EchoNativeAddonDescriptor descriptor) {
        registerDescriptorResourceIntent(services, descriptor);
        for (String domain : adapterCoreDomains(descriptor)) {
            Map<String, Object> evidence = descriptorDomainEvidence(descriptor, domain);
            services.commandHost().registerDescriptorDomain(descriptor.id(), domain, evidence);
            services.configHost().registerDescriptorDomain(descriptor.id(), domain, evidence);
            services.resourceHost().registerDescriptorDomain(descriptor.id(), domain, evidence);
            services.networkHost().registerDescriptorDomain(descriptor.id(), domain, evidence);
        }
        for (String provided : descriptor.provides()) {
            Map<String, Object> evidence = descriptorCapabilityEvidence(descriptor, "provides", provided);
            services.commandHost().registerDescriptorDomain(descriptor.id(), provided, evidence);
            services.configHost().registerDescriptorDomain(descriptor.id(), provided, evidence);
            services.resourceHost().registerDescriptorDomain(descriptor.id(), provided, evidence);
            services.networkHost().registerDescriptorDomain(descriptor.id(), provided, evidence);
        }
        for (String consumed : descriptor.consumes()) {
            Map<String, Object> evidence = descriptorCapabilityEvidence(descriptor, "consumes", consumed);
            services.commandHost().registerDescriptorDomain(descriptor.id(), consumed, evidence);
            services.configHost().registerDescriptorDomain(descriptor.id(), consumed, evidence);
            services.resourceHost().registerDescriptorDomain(descriptor.id(), consumed, evidence);
            services.networkHost().registerDescriptorDomain(descriptor.id(), consumed, evidence);
        }
    }

    private static void registerDescriptorResourceIntent(
            NativeHostServices services,
            EchoNativeAddonDescriptor descriptor
    ) {
        if (!hasNativeEntrypoint(descriptor)) {
            return;
        }
        for (DescriptorResourceIntent intent : descriptorResourceIntents(descriptor)) {
            Map<String, Object> evidence = new LinkedHashMap<>();
            evidence.put("source", "descriptor.native_module_resource_projection");
            evidence.put("descriptorPath", descriptorPath(descriptor));
            evidence.put("moduleId", text(descriptor.id()));
            evidence.put("resourceId", intent.resourceId());
            evidence.put("resourceType", intent.resourceType());
            evidence.put("sourcePath", intent.sourcePath());
            evidence.put("nativeResourceProjection", true);
            evidence.put("executionMode", "native_resource_host_mount");
            evidence.put("summary", "Descriptor-projected module resource mounted from the native classpath resource tree.");
            services.resourceHost().registerResource(
                    text(descriptor.id()),
                    intent.resourceId(),
                    intent.resourceType(),
                    Map.copyOf(evidence)
            );
        }
    }

    private static List<DescriptorResourceIntent> descriptorResourceIntents(EchoNativeAddonDescriptor descriptor) {
        Map<String, DescriptorResourceIntent> intents = new LinkedHashMap<>();
        String moduleId = text(descriptor.id());
        if (moduleId.isBlank()) {
            return List.of();
        }
        for (Path path : descriptorContentPaths(descriptor)) {
            discoverDescriptorResourceIntents(path, moduleId, intents);
        }
        return List.copyOf(intents.values());
    }

    private static void discoverDescriptorResourceIntents(
            Path path,
            String moduleId,
            Map<String, DescriptorResourceIntent> intents
    ) {
        if (path == null || !Files.exists(path)) {
            return;
        }
        try {
            if (Files.isDirectory(path)) {
                discoverDescriptorResourceIntentsFromDirectory(path, moduleId, intents);
            } else if (Files.isRegularFile(path)
                    && path.getFileName() != null
                    && path.getFileName().toString().toLowerCase(java.util.Locale.ROOT).endsWith(".jar")) {
                discoverDescriptorResourceIntentsFromJar(path, moduleId, intents);
            }
        } catch (IOException ignored) {
            // Descriptor resource projection is best-effort; release diagnostics report broken classpaths.
        }
    }

    private static void discoverDescriptorResourceIntentsFromDirectory(
            Path root,
            String moduleId,
            Map<String, DescriptorResourceIntent> intents
    ) throws IOException {
        try (var stream = Files.walk(root)) {
            stream.filter(Files::isRegularFile)
                    .map(root::relativize)
                    .map(Path::toString)
                    .map(name -> name.replace('\\', '/'))
                    .forEach(name -> discoverDescriptorResourceIntentFromName(name, moduleId, intents));
        }
    }

    private static void discoverDescriptorResourceIntentsFromJar(
            Path jar,
            String moduleId,
            Map<String, DescriptorResourceIntent> intents
    ) throws IOException {
        try (ZipInputStream input = new ZipInputStream(Files.newInputStream(jar))) {
            ZipEntry entry;
            while ((entry = input.getNextEntry()) != null) {
                if (!entry.isDirectory()) {
                    discoverDescriptorResourceIntentFromName(entry.getName().replace('\\', '/'), moduleId, intents);
                }
            }
        }
    }

    private static void discoverDescriptorResourceIntentFromName(
            String name,
            String moduleId,
            Map<String, DescriptorResourceIntent> intents
    ) {
        if (name == null || name.isBlank()) {
            return;
        }
        String normalizedName = name.replace('\\', '/');
        if (normalizedName.startsWith("assets/" + moduleId + "/")) {
            putDescriptorResourceIntent(
                    intents,
                    moduleId + ":client_resources",
                    "resource_pack",
                    "assets/" + moduleId
            );
            if (normalizedName.contains("/eui/")) {
                putDescriptorResourceIntent(
                        intents,
                        moduleId + ":eui_resources",
                        "screen_markup",
                        "assets/" + moduleId + "/eui"
                );
            }
            return;
        }
        if (!normalizedName.startsWith("data/")) {
            return;
        }
        String[] parts = normalizedName.split("/");
        if (parts.length < 3) {
            return;
        }
        String namespace = parts[1];
        String domain = parts[2];
        if (!isValidContentPath(namespace)) {
            return;
        }
        putDescriptorResourceIntent(
                intents,
                namespace + ":datapack",
                "data_pack",
                "data/" + namespace
        );
        switch (domain) {
            case "worldgen" -> discoverWorldgenResourceIntent(normalizedName, namespace, intents);
            case "recipe", "recipes" -> putDescriptorResourceIntent(
                    intents,
                    namespace + ":recipes",
                    "recipe",
                    "data/" + namespace + "/" + domain
            );
            case "loot_table", "loot_tables" -> putDescriptorResourceIntent(
                    intents,
                    namespace + ":loot_tables",
                    "loot",
                    "data/" + namespace + "/" + domain
            );
            case "tags" -> putDescriptorResourceIntent(
                    intents,
                    namespace + ":tags",
                    "tag",
                    "data/" + namespace + "/tags"
            );
            case "structures", "structure" -> putDescriptorResourceIntent(
                    intents,
                    namespace + ":structures",
                    "structure",
                    "data/" + namespace + "/" + domain
            );
            default -> {
            }
        }
    }

    private static void discoverWorldgenResourceIntent(
            String name,
            String namespace,
            Map<String, DescriptorResourceIntent> intents
    ) {
        String prefix = "data/" + namespace + "/worldgen/";
        if (!name.startsWith(prefix)) {
            return;
        }
        String rest = name.substring(prefix.length());
        if (rest.startsWith("world_preset/") && name.endsWith(".json")) {
            String preset = rest.substring("world_preset/".length(), rest.length() - ".json".length());
            if (isValidContentPath(preset)) {
                putDescriptorResourceIntent(
                        intents,
                        namespace + ":" + preset,
                        "world_preset",
                        prefix + "world_preset"
                );
            }
            return;
        }
        if (rest.startsWith("structure/") || rest.startsWith("structure_set/")) {
            putDescriptorResourceIntent(
                    intents,
                    namespace + ":worldgen_structures",
                    "structure",
                    prefix + "structure"
            );
            return;
        }
        putDescriptorResourceIntent(
                intents,
                namespace + ":worldgen",
                "worldgen",
                prefix
        );
    }

    private static void putDescriptorResourceIntent(
            Map<String, DescriptorResourceIntent> intents,
            String resourceId,
            String resourceType,
            String sourcePath
    ) {
        if (resourceId == null || resourceId.isBlank() || resourceType == null || resourceType.isBlank()) {
            return;
        }
        String key = resourceType + ":" + resourceId;
        intents.putIfAbsent(key, new DescriptorResourceIntent(resourceId, resourceType, sourcePath));
    }

    private static void registerDescriptorCreativeTabIntent(
            NativeHostServices services,
            EchoNativeAddonDescriptor descriptor
    ) {
        if (!hasNativeEntrypoint(descriptor)) {
            return;
        }
        String moduleId = text(descriptor.id());
        if (moduleId.isBlank()) {
            return;
        }
        List<Map<String, Object>> declarations = descriptorCreativeTabDeclarations(descriptor);
        if (declarations.isEmpty()) {
            return;
        }
        List<String> discoveredItemIds = descriptorCreativeTabItemIds(descriptor);
        for (Map<String, Object> declaration : declarations) {
            String tabId = normalizedContentId(declaration.get("id"));
            if (tabId.isBlank()) {
                continue;
            }
            if (services.registryHost().creativeTab(tabId) != null) {
                continue;
            }
            List<String> declaredItemIds = normalizedContentIds(declaration.get("itemIds"));
            List<String> itemIds = declaredItemIds.isEmpty() ? discoveredItemIds : declaredItemIds;
            List<String> surfaceIds = normalizedTextList(declaration.get("surfaceIds"));
            Map<String, Object> evidence = new LinkedHashMap<>(declaration);
            evidence.put("source", "descriptor.native_module_creative_tab_declaration");
            evidence.put("descriptorPath", descriptorPath(descriptor));
            evidence.put("moduleId", moduleId);
            evidence.put("moduleName", text(descriptor.name()).isBlank() ? moduleId : descriptor.name());
            evidence.put("implementationClass", "native://descriptor/creative_tab_declaration");
            evidence.put("nativeSdkSurface", "creative_tab");
            evidence.put("nativeRegistryProjection", false);
            evidence.put("descriptorDeclaredNativeCreativeTab", true);
            evidence.put("planned", true);
            evidence.put("executionMode", "native_registry_host_registration");
            evidence.put("id", tabId);
            evidence.put("registry", "creative_tab");
            evidence.put("itemIds", itemIds);
            evidence.put("itemCount", itemIds.size());
            evidence.put("nativeCreativeTabItemsDeclared", !itemIds.isEmpty());
            evidence.put("nativeCreativeTabItemsDescriptorDeclared", !declaredItemIds.isEmpty());
            evidence.put("nativeCreativeTabItemsDiscoveredFromClasspath", declaredItemIds.isEmpty() && !itemIds.isEmpty());
            evidence.put("surfaceIds", surfaceIds);
            evidence.put("surfaceCount", surfaceIds.size());
            evidence.put("nativeCreativeTabRegistryBacked", discoveredItemIds.containsAll(itemIds));
            evidence.put("summary", "Descriptor-declared native creative tab registered through the Native Loader registry host.");
            services.registryHost().registerDeclared(
                    moduleId,
                    "creative_tab",
                    tabId,
                    Map.copyOf(evidence)
            );
        }
    }

    private static List<String> descriptorCreativeTabItemIds(EchoNativeAddonDescriptor descriptor) {
        String namespace = text(descriptor.id());
        if (namespace.isBlank()) {
            return List.of();
        }
        Set<String> itemIds = new TreeSet<>();
        for (Path path : descriptorContentPaths(descriptor)) {
            discoverItemIds(path, namespace, itemIds);
        }
        return List.copyOf(itemIds);
    }

    private static List<Map<String, Object>> descriptorCreativeTabDeclarations(EchoNativeAddonDescriptor descriptor) {
        if (descriptor == null || descriptor.access() == null) {
            return List.of();
        }
        List<Map<String, Object>> declarations = new ArrayList<>();
        addCreativeTabDeclarations(declarations, descriptor.access().get("nativeCreativeTabs"));
        addCreativeTabDeclarations(declarations, descriptor.access().get("creativeTabs"));
        Object nativeRegistry = descriptor.access().get("nativeRegistry");
        if (nativeRegistry instanceof Map<?, ?> registry) {
            addCreativeTabDeclarations(declarations, registry.get("creativeTabs"));
            addCreativeTabDeclarations(declarations, registry.get("creativeTabDeclarations"));
        }
        return List.copyOf(declarations);
    }

    private static void addCreativeTabDeclarations(List<Map<String, Object>> declarations, Object value) {
        if (!(value instanceof Iterable<?> iterable)) {
            return;
        }
        for (Object item : iterable) {
            if (!(item instanceof Map<?, ?> map)) {
                continue;
            }
            Map<String, Object> declaration = new LinkedHashMap<>();
            map.forEach((key, entryValue) -> declaration.put(String.valueOf(key), entryValue));
            String id = normalizedContentId(declaration.get("id"));
            if (id.isBlank()) {
                continue;
            }
            declaration.put("registry", "creative_tab");
            declaration.put("id", id);
            declarations.add(Map.copyOf(declaration));
        }
    }

    private static List<Path> descriptorContentPaths(EchoNativeAddonDescriptor descriptor) {
        LinkedHashSet<Path> paths = new LinkedHashSet<>();
        if (descriptor.descriptorPath() != null
                && descriptor.descriptorPath().getParent() != null
                && descriptor.descriptorPath().getParent().getParent() != null) {
            paths.add(descriptor.descriptorPath().getParent().getParent().toAbsolutePath().normalize());
        }
        EchoNativeModuleDescriptor moduleDescriptor = EchoNativeModuleDescriptor.fromAddon(descriptor);
        paths.addAll(moduleDescriptor.classpath());
        return List.copyOf(paths);
    }

    private static void discoverItemIds(Path path, String namespace, Set<String> itemIds) {
        if (path == null || !Files.exists(path)) {
            return;
        }
        try {
            if (Files.isDirectory(path)) {
                discoverItemIdsFromDirectory(path, namespace, itemIds);
            } else if (Files.isRegularFile(path)
                    && path.getFileName() != null
                    && path.getFileName().toString().toLowerCase(java.util.Locale.ROOT).endsWith(".jar")) {
                discoverItemIdsFromJar(path, namespace, itemIds);
            }
        } catch (IOException ignored) {
            // Creative tab projection is best-effort; bad classpath entries are reported by release diagnostics.
        }
    }

    private static void discoverItemIdsFromDirectory(Path root, String namespace, Set<String> itemIds)
            throws IOException {
        try (var stream = Files.walk(root)) {
            stream.filter(Files::isRegularFile)
                    .map(root::relativize)
                    .map(Path::toString)
                    .map(name -> name.replace('\\', '/'))
                    .forEach(name -> discoverItemIdFromResourceName(name, namespace, itemIds));
        }
    }

    private static void discoverItemIdsFromJar(Path jar, String namespace, Set<String> itemIds) throws IOException {
        try (ZipInputStream input = new ZipInputStream(Files.newInputStream(jar))) {
            ZipEntry entry;
            while ((entry = input.getNextEntry()) != null) {
                discoverItemIdFromResourceName(entry.getName().replace('\\', '/'), namespace, itemIds);
            }
        }
    }

    private static void discoverItemIdFromResourceName(String name, String namespace, Set<String> itemIds) {
        if (name == null || !name.endsWith(".json") || !name.startsWith("assets/")) {
            return;
        }
        String[] parts = name.split("/");
        if (parts.length < 4 || !namespace.equals(parts[1])) {
            return;
        }
        String path = "";
        if ("items".equals(parts[2])) {
            path = name.substring(("assets/" + namespace + "/items/").length(), name.length() - ".json".length());
        } else if (parts.length >= 5 && "models".equals(parts[2]) && "item".equals(parts[3])) {
            path = name.substring(("assets/" + namespace + "/models/item/").length(), name.length() - ".json".length());
        }
        if (isValidContentPath(path)) {
            itemIds.add(namespace + ":" + path);
        }
    }

    private static List<String> normalizedContentIds(Object value) {
        if (!(value instanceof Iterable<?> iterable)) {
            return List.of();
        }
        List<String> ids = new ArrayList<>();
        for (Object item : iterable) {
            String id = normalizedContentId(item);
            if (!id.isBlank() && !ids.contains(id)) {
                ids.add(id);
            }
        }
        return List.copyOf(ids);
    }

    private static String normalizedContentId(Object value) {
        String id = text(value).toLowerCase(java.util.Locale.ROOT);
        int separator = id.indexOf(':');
        if (separator < 1 || separator + 1 >= id.length()) {
            return "";
        }
        String namespace = id.substring(0, separator).trim();
        String path = id.substring(separator + 1).trim();
        return isValidContentPath(namespace) && isValidContentPath(path) ? namespace + ":" + path : "";
    }

    private static List<String> normalizedTextList(Object value) {
        if (!(value instanceof Iterable<?> iterable)) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        for (Object item : iterable) {
            String text = text(item);
            if (!text.isBlank() && !values.contains(text)) {
                values.add(text);
            }
        }
        return List.copyOf(values);
    }

    private static boolean isValidContentPath(String value) {
        return value != null
                && value.matches("[a-z0-9_./-]+")
                && !value.contains("//")
                && !value.startsWith("/")
                && !value.endsWith("/");
    }

    private static List<EchoNativeProductHookExecution> applyProductHooks(
            NativeHostServices services,
            EchoNativeProductHookPlan plan,
            boolean registryHooksOnly
    ) {
        if (plan == null || plan.isEmpty()) {
            return List.of();
        }
        List<EchoNativeProductHookExecution> executions = new ArrayList<>();
        for (EchoNativeProductHookPlan.RegistryHook hook : plan.registryHooks()) {
            if (!registryHooksOnly) {
                continue;
            }
            EchoNativeLoadStatus status;
            if (isCreativeTabRegistry(hook.registry())
                    && services.registryHost().creativeTab(declaredContentId(hook.moduleId(), hook.id())) != null) {
                status = EchoNativeLoadStatus.RESOLVED;
            } else {
                status = services.registryHost().registerDeclared(
                        hook.moduleId(),
                        hook.registry(),
                        hook.id(),
                        hook.properties()
                );
            }
            executions.add(hookExecution("registry", hook.moduleId(), hook.registry() + ":" + hook.id(), status, hook.properties()));
        }
        if (registryHooksOnly) {
            return List.copyOf(executions);
        }
        for (EchoNativeProductHookPlan.LifecycleHook hook : plan.lifecycleHooks()) {
            NativeLoaderMutationLedger.MutationRecord record = services.adapterCoreBackend().lifecyclePhase(
                    hook.moduleId(),
                    hook.phaseId(),
                    hook.evidence()
            );
            executions.add(hookExecution(
                    "lifecycle",
                    hook.moduleId(),
                    hook.phaseId(),
                    adapterCoreLiveProofExecutionStatus(record),
                    record.toReport()
            ));
        }
        for (EchoNativeProductHookPlan.EventSubscriptionHook hook : plan.eventSubscriptions()) {
            services.lifecycleEventHost().subscribeDeclaredHook(
                    hook.moduleId(),
                    hook.eventId(),
                    hook.handlerId(),
                    hook.evidence()
            );
            executions.add(hookExecution(
                    "event_subscription",
                    hook.moduleId(),
                    hook.eventId() + ":" + hook.handlerId(),
                    EchoNativeLoadStatus.RESOLVED,
                    hook.evidence()
            ));
        }
        for (EchoNativeProductHookPlan.EventPublishHook hook : plan.eventsToPublish()) {
            NativeLoaderLifecycleEventHost.PublishedEvent event = services.lifecycleEventHost().publish(
                    hook.sourceModule(),
                    hook.eventId(),
                    hook.payload(),
                    EchoNativeLoadStatus.MUTATED
            );
            executions.add(hookExecution(
                    "event_publish",
                    hook.sourceModule(),
                    hook.eventId(),
                    event.liveMinecraftMutation() ? EchoNativeLoadStatus.MUTATED : EchoNativeLoadStatus.UNSUPPORTED,
                    event.toReport()
            ));
        }
        for (EchoNativeProductHookPlan.CommandHook hook : plan.commandHooks()) {
            NativeLoaderMutationLedger.MutationRecord record = services.adapterCoreBackend().registerCommand(
                    hook.moduleId(),
                    hook.commandId(),
                    hook.targetSurface(),
                    hook.targetBridge(),
                    hook.evidence()
            );
            executions.add(hookExecution("command", hook.moduleId(), hook.commandId(),
                    adapterCoreLiveProofExecutionStatus(record), record.toReport()));
        }
        for (EchoNativeProductHookPlan.NetworkHook hook : plan.networkHooks()) {
            NativeLoaderMutationLedger.MutationRecord record = services.adapterCoreBackend().registerNetworkPacket(
                    hook.moduleId(),
                    hook.packetId(),
                    hook.surface(),
                    hook.sourceRuntimeTarget(),
                    hook.consumers(),
                    hook.evidence()
            );
            executions.add(hookExecution("network", hook.moduleId(), hook.packetId(),
                    adapterCoreLiveProofExecutionStatus(record), record.toReport()));
        }
        for (EchoNativeProductHookPlan.ResourceHook hook : plan.resourceHooks()) {
            EchoNativeLoadStatus status = services.resourceHost().registerResource(
                    hook.moduleId(),
                    hook.resourceId(),
                    hook.resourceType(),
                    hook.evidence()
            );
            executions.add(hookExecution(
                    "resource",
                    hook.moduleId(),
                    hook.resourceId(),
                    resourceHookExecutionStatus(status, hook.evidence()),
                    hook.evidence()
            ));
        }
        for (EchoNativeProductHookPlan.ConfigHook hook : plan.configHooks()) {
            NativeLoaderMutationLedger.MutationRecord record = services.adapterCoreBackend().reloadConfig(
                    hook.moduleId(),
                    hook.configId(),
                    hook.scope(),
                    hook.evidence()
            );
            executions.add(hookExecution("config", hook.moduleId(), hook.configId(),
                    adapterCoreLiveProofExecutionStatus(record), record.toReport()));
        }
        for (EchoNativeProductHookPlan.RuntimeHook hook : plan.runtimeHooks()) {
            NativeLoaderMutationLedger.MutationRecord record = applyRuntimeHook(services, hook);
            executions.add(hookExecution(
                    "runtime",
                    hook.moduleId(),
                    hook.surface() + ":" + hook.targetId(),
                    runtimeHookExecutionStatus(record),
                    record.toReport()
            ));
        }
        for (EchoNativeProductHookPlan.ClientSurfaceHook hook : plan.clientSurfaceHooks()) {
            EchoNativeLoadStatus status = services.clientUiHost().registerSurfaceStatus(
                    hook.moduleId(),
                    hook.surfaceId(),
                    hook.surfaceType(),
                    hook.config()
            );
            executions.add(hookExecution(
                    "client_ui",
                    hook.moduleId(),
                    hook.surfaceId(),
                    clientUiHookExecutionStatus(services, hook.moduleId(), hook.surfaceId(), status),
                    hook.config()
            ));
        }
        for (EchoNativeProductHookPlan.ProductWorldHook hook : plan.productWorldHooks()) {
            Map<String, Object> evidence = productWorldHookEvidence(hook);
            NativeLoaderMutationLedger.MutationRecord record = services.adapterCoreBackend().writeSaveData(
                    "echo.native.productWorld." + hook.moduleId(),
                    hook.productWorldPreset()
            );
            NativeLoaderMutationLedger.MutationRecord policyRecord = services.adapterCoreBackend().writeSaveData(
                    "echo.native.productWorldPolicy." + hook.moduleId(),
                    hook.vanillaSavePolicy()
            );
            Map<String, Object> report = new LinkedHashMap<>(evidence);
            report.put("productWorldSaveRecord", record.toReport());
            report.put("productWorldPolicySaveRecord", policyRecord.toReport());
            report.put("nativeProductWorldDefault", adapterCoreLiveProofSatisfied(record)
                    && adapterCoreLiveProofSatisfied(policyRecord));
            executions.add(hookExecution(
                    "product_world",
                    hook.moduleId(),
                    hook.worldId(),
                    successfulNativeHookStatus(record.status()) && successfulNativeHookStatus(policyRecord.status())
                            ? EchoNativeLoadStatus.RESOLVED
                            : EchoNativeLoadStatus.UNSUPPORTED,
                    Map.copyOf(report)
            ));
        }
        for (EchoNativeProductHookPlan.ProductOnboardingHook hook : plan.productOnboardingHooks()) {
            Map<String, Object> report = productOnboardingHookReport(services, hook);
            executions.add(hookExecution(
                    "product_onboarding",
                    hook.moduleId(),
                    hook.spawnProfile(),
                    Boolean.TRUE.equals(report.get("nativeProductOnboardingLiveProofMutated"))
                            ? EchoNativeLoadStatus.MUTATED
                            : EchoNativeLoadStatus.UNSUPPORTED,
                    report
            ));
        }
        for (EchoNativeProductHookPlan.SaveDataHook hook : plan.saveDataHooks()) {
            NativeLoaderMutationLedger.MutationRecord record = hook.delete()
                    ? services.adapterCoreBackend().deleteSaveData(hook.key())
                    : services.adapterCoreBackend().writeSaveData(hook.key(), hook.value());
            executions.add(hookExecution("save_data", "echocore", hook.key(),
                    adapterCoreLiveProofExecutionStatus(record), record.toReport()));
        }
        return List.copyOf(executions);
    }

    private static boolean isCreativeTabRegistry(String registry) {
        String normalized = text(registry).trim().toLowerCase().replace('-', '_');
        return "creative_tab".equals(normalized) || "creative_tabs".equals(normalized);
    }

    private static String declaredContentId(String moduleId, String id) {
        String normalized = normalizedContentId(id);
        if (!normalized.isBlank()) {
            return normalized;
        }
        String namespace = text(moduleId).trim().toLowerCase(java.util.Locale.ROOT);
        String localId = text(id).trim().toLowerCase(java.util.Locale.ROOT);
        return isValidContentPath(namespace) && isValidContentPath(localId) ? namespace + ":" + localId : "";
    }

    private static NativeLoaderMutationLedger.MutationRecord applyRuntimeHook(
            NativeHostServices services,
            EchoNativeProductHookPlan.RuntimeHook hook
    ) {
        String surface = normalizeRuntimeSurface(hook.surface());
        String action = hook.action() == null || hook.action().isBlank() ? "apply" : hook.action().trim();
        Map<String, Object> payload = hook.payload() == null ? Map.of() : hook.payload();
        NativeLoaderAdapterCoreBackend backend = services.adapterCoreBackend();
        return switch (surface) {
            case "inventory" -> removeAction(action)
                    ? backend.removeItem(
                            text(payload.getOrDefault("playerId", "player:native-product")),
                            text(payload.getOrDefault("itemId", hook.targetId())),
                            intValue(payload.getOrDefault("count", 1)))
                    : backend.grantItem(
                            text(payload.getOrDefault("playerId", "player:native-product")),
                            text(payload.getOrDefault("itemId", hook.targetId())),
                            intValue(payload.getOrDefault("count", 1)));
            case "player_state" -> backend.updatePlayerState(
                    text(payload.getOrDefault("playerId", "player:native-product")),
                    text(payload.getOrDefault("key", hook.targetId())),
                    text(payload.getOrDefault("value", action))
            );
            case "client_tick" -> backend.clientTick(text(payload.getOrDefault("phase", hook.targetId())), payload);
            case "render_layers" -> backend.renderLayer(text(payload.getOrDefault("layerId", hook.targetId())), payload);
            case "screen_events" -> backend.screenEvent(
                    text(payload.getOrDefault("screenId", hook.targetId())),
                    text(payload.getOrDefault("eventType", action)),
                    payload);
            case "keybinds" -> backend.keybind(
                    text(payload.getOrDefault("keybindId", hook.targetId())),
                    text(payload.getOrDefault("action", action)),
                    payload);
            case "world_blocks" -> backend.placeBlock(
                    text(payload.getOrDefault("dimension", "minecraft:overworld")),
                    intValue(payload.get("x")),
                    intValue(payload.getOrDefault("y", 80)),
                    intValue(payload.get("z")),
                    text(payload.getOrDefault("blockId", hook.targetId()))
            );
            case "world_state" -> backend.updateWorldState(
                    text(payload.getOrDefault("dimension", "minecraft:overworld")),
                    text(payload.getOrDefault("key", hook.targetId())),
                    text(payload.getOrDefault("value", action))
            );
            case "structures" -> backend.placeStructure(
                    text(payload.getOrDefault("dimension", "minecraft:overworld")),
                    text(payload.getOrDefault("structureId", hook.targetId())),
                    intValue(payload.get("x")),
                    intValue(payload.getOrDefault("y", 80)),
                    intValue(payload.get("z"))
            );
            case "block_entities" -> backend.updateBlockEntity(
                    text(payload.getOrDefault("dimension", "minecraft:overworld")),
                    intValue(payload.get("x")),
                    intValue(payload.getOrDefault("y", 80)),
                    intValue(payload.get("z")),
                    text(payload.getOrDefault("key", action)),
                    text(payload.getOrDefault("value", action))
            );
            case "capabilities" -> backend.updateCapability(
                    text(payload.getOrDefault("target", "player:native-product")),
                    text(payload.getOrDefault("capability", hook.targetId())),
                    text(payload.getOrDefault("value", action))
            );
            case "events" -> backend.emitEvent(
                    text(payload.getOrDefault("eventId", hook.targetId())),
                    text(payload.getOrDefault("payload", action)));
            case "packets_hud" -> backend.sendPacketHud(
                    text(payload.getOrDefault("channel", hook.targetId())),
                    text(payload.getOrDefault("payload", action)));
            case "save_data" -> deleteAction(action)
                    ? backend.deleteSaveData(text(payload.getOrDefault("key", hook.targetId())))
                    : backend.writeSaveData(
                            text(payload.getOrDefault("key", hook.targetId())),
                            text(payload.getOrDefault("value", action)));
            case "hud" -> backend.emitHud(
                    text(payload.getOrDefault("channel", hook.targetId())),
                    text(payload.getOrDefault("message", action))
            );
            case "missions" -> backend.updateMission(
                    text(payload.getOrDefault("missionId", hook.targetId())),
                    text(payload.getOrDefault("phase", action)),
                    text(payload.getOrDefault("objectiveKey", "native_product_runtime_hook"))
            );
            case "commands" -> backend.registerCommand(
                    hook.moduleId(),
                    text(payload.getOrDefault("commandId", hook.targetId())),
                    text(payload.getOrDefault("targetSurface", action)),
                    text(payload.getOrDefault("targetBridge", "native.runtime"))
                            .isBlank() ? "native.runtime" : text(payload.getOrDefault("targetBridge", "native.runtime")),
                    payload
            );
            case "network_channels" -> backend.registerNetworkPacket(
                    hook.moduleId(),
                    text(payload.getOrDefault("packetId", hook.targetId())),
                    text(payload.getOrDefault("surface", action)),
                    text(payload.getOrDefault("sourceRuntimeTarget", "native.runtime"))
                            .isBlank() ? "native.runtime" : text(payload.getOrDefault("sourceRuntimeTarget", "native.runtime")),
                    stringList(payload.get("consumers")),
                    payload
            );
            case "config_reloads" -> backend.reloadConfig(
                    hook.moduleId(),
                    text(payload.getOrDefault("configId", hook.targetId())),
                    text(payload.getOrDefault("scope", action)),
                    payload);
            case "resource_reloads" -> backend.reloadResources(
                    hook.moduleId(),
                    text(payload.getOrDefault("resourceId", hook.targetId())),
                    text(payload.getOrDefault("scope", action)),
                    payload);
            case "save_hooks" -> backend.saveHook(text(payload.getOrDefault("hookId", hook.targetId())), payload);
            case "lifecycle_phases" -> backend.lifecyclePhase(
                    hook.moduleId(),
                    text(payload.getOrDefault("phaseId", hook.targetId())),
                    payload);
            case "server_client_sync" -> backend.syncServerClient(
                    text(payload.getOrDefault("channel", hook.targetId())),
                    String.valueOf(payload.getOrDefault("payload", action))
            );
            default -> backend.unsupportedRuntimeHook(surface, hook.surface(), hook.targetId());
        };
    }

    private static EchoNativeLoadStatus runtimeHookExecutionStatus(
            NativeLoaderMutationLedger.MutationRecord record
    ) {
        return adapterCoreLiveProofExecutionStatus(record);
    }

    private static EchoNativeLoadStatus adapterCoreLiveProofExecutionStatus(
            NativeLoaderMutationLedger.MutationRecord record
    ) {
        if (record == null || record.status() != EchoNativeLoadStatus.MUTATED) {
            return record == null ? EchoNativeLoadStatus.UNSUPPORTED : record.status();
        }
        return adapterCoreLiveProofSatisfied(record) ? EchoNativeLoadStatus.MUTATED : EchoNativeLoadStatus.UNSUPPORTED;
    }

    private static boolean adapterCoreLiveProofSatisfied(
            NativeLoaderMutationLedger.MutationRecord record
    ) {
        return record != null
                && record.status() == EchoNativeLoadStatus.MUTATED
                && record.liveRuntimeReleaseProofSatisfied()
                && record.liveRuntimeSurfaceMutationSatisfied();
    }

    private static EchoNativeLoadStatus loadStatus(String value) {
        try {
            return EchoNativeLoadStatus.valueOf(text(value));
        } catch (IllegalArgumentException ignored) {
            return EchoNativeLoadStatus.UNSUPPORTED;
        }
    }

    private static boolean successfulNativeHookStatus(EchoNativeLoadStatus status) {
        return status != null && status != EchoNativeLoadStatus.FAILED && status != EchoNativeLoadStatus.UNSUPPORTED;
    }

    private static EchoNativeLoadStatus resourceHookExecutionStatus(
            EchoNativeLoadStatus status,
            Map<String, Object> evidence
    ) {
        if (status == null || status == EchoNativeLoadStatus.FAILED || status == EchoNativeLoadStatus.UNSUPPORTED) {
            return status == null ? EchoNativeLoadStatus.UNSUPPORTED : status;
        }
        Map<String, Object> safeEvidence = evidence == null ? Map.of() : evidence;
        return Boolean.TRUE.equals(safeEvidence.get("liveMinecraftMutation"))
                && Boolean.TRUE.equals(safeEvidence.get("minecraftRuntimeAccessed"))
                ? EchoNativeLoadStatus.MUTATED
                : EchoNativeLoadStatus.RESOLVED;
    }

    private static EchoNativeLoadStatus clientUiHookExecutionStatus(
            NativeHostServices services,
            String moduleId,
            String surfaceId,
            EchoNativeLoadStatus status
    ) {
        if (status == null || status == EchoNativeLoadStatus.FAILED || status == EchoNativeLoadStatus.UNSUPPORTED) {
            return status == null ? EchoNativeLoadStatus.UNSUPPORTED : status;
        }
        Map<String, Object> surface = services.clientUiHost()
                .surfaces()
                .getOrDefault(text(moduleId) + ":" + text(surfaceId), Map.of());
        Map<String, Object> bridgeEvidence = objectMap(surface.get("liveClientBridgeEvidence"));
        boolean liveClientMutation = Boolean.TRUE.equals(surface.get("liveClientBridgeMutated"))
                && Boolean.TRUE.equals(bridgeEvidence.get("nativeClientRouteProcess"))
                && Boolean.TRUE.equals(bridgeEvidence.get("releaseClientRouteTrusted"))
                && Boolean.TRUE.equals(bridgeEvidence.get("clientRouteMutationSupported"))
                && !Boolean.TRUE.equals(bridgeEvidence.get("neoForgeEventOwnershipRequired"));
        return liveClientMutation ? EchoNativeLoadStatus.MUTATED : EchoNativeLoadStatus.RESOLVED;
    }

    private static Map<String, Object> objectMap(Object value) {
        if (!(value instanceof Map<?, ?> map)) {
            return Map.of();
        }
        Map<String, Object> result = new LinkedHashMap<>();
        map.forEach((key, entry) -> result.put(text(key), entry));
        return Map.copyOf(result);
    }

    private static boolean removeAction(String action) {
        String normalized = text(action).trim().toLowerCase(java.util.Locale.ROOT).replace('-', '_');
        return "remove".equals(normalized)
                || "remove_item".equals(normalized)
                || "take".equals(normalized)
                || "consume".equals(normalized);
    }

    private static boolean deleteAction(String action) {
        String normalized = text(action).trim().toLowerCase(java.util.Locale.ROOT).replace('-', '_');
        return "delete".equals(normalized)
                || "delete_save".equals(normalized)
                || "remove".equals(normalized)
                || "clear".equals(normalized);
    }

    private static String normalizeRuntimeSurface(String surface) {
        String normalized = surface == null ? "" : surface.trim().toLowerCase(java.util.Locale.ROOT)
                .replace('-', '_')
                .replace('.', '_');
        return switch (normalized) {
            case "inventory", "item", "items", "grant_item" -> "inventory";
            case "player", "player_state", "persistent_state" -> "player_state";
            case "client_tick", "tick", "clienttick" -> "client_tick";
            case "render", "render_layer", "render_layers", "client_render" -> "render_layers";
            case "screen", "screen_event", "screen_events", "client_screen" -> "screen_events";
            case "keybind", "keybinds", "input", "client_input" -> "keybinds";
            case "block", "blocks", "world_block", "world_blocks", "place_block" -> "world_blocks";
            case "world", "world_state" -> "world_state";
            case "structure", "structures", "place_structure" -> "structures";
            case "block_entity", "block_entities", "blockentity", "blockentities" -> "block_entities";
            case "capability", "capabilities", "player_capability", "energy_capability" -> "capabilities";
            case "event", "events", "runtime_event" -> "events";
            case "packet", "packets", "packet_hud", "packets_hud", "network_hud" -> "packets_hud";
            case "save_data", "saved_data", "write_save" -> "save_data";
            case "hud", "notification", "hud_notification" -> "hud";
            case "command", "commands", "server_command", "server_commands", "native_command" -> "commands";
            case "network_channel", "network_channels", "channel", "channels", "native_packet" -> "network_channels";
            case "config", "config_reload", "config_reloads", "configuration" -> "config_reloads";
            case "resource_reload", "resource_reloads", "reload_resources", "data_reload" -> "resource_reloads";
            case "save", "save_hook", "save_hooks", "save_lifecycle" -> "save_hooks";
            case "lifecycle", "lifecycle_phase", "lifecycle_phases" -> "lifecycle_phases";
            case "mission", "missions", "quest", "quests" -> "missions";
            case "sync", "server_client_sync", "server_client", "network_sync" -> "server_client_sync";
            default -> normalized;
        };
    }

    private static String string(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static int intValue(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(text(value));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private static List<String> stringList(Object value) {
        if (value instanceof Iterable<?> iterable) {
            List<String> values = new ArrayList<>();
            for (Object item : iterable) {
                String text = text(item);
                if (!text.isBlank()) {
                    values.add(text);
                }
            }
            return List.copyOf(values);
        }
        String text = text(value);
        return text.isBlank() ? List.of() : List.of(text);
    }

    private static Map<String, Object> productWorldHookEvidence(EchoNativeProductHookPlan.ProductWorldHook hook) {
        Map<String, Object> evidence = new LinkedHashMap<>(hook.evidence());
        evidence.put("moduleId", hook.moduleId());
        evidence.put("worldId", hook.worldId());
        evidence.put("defaultWorldMode", hook.defaultWorldMode());
        evidence.put("productWorldPreset", hook.productWorldPreset());
        evidence.put("productDatapack", hook.productDatapack());
        evidence.put("productResourcePack", hook.productResourcePack());
        evidence.put("vanillaSavePolicy", hook.vanillaSavePolicy());
        evidence.put("nativeProductWorldHook", true);
        evidence.put("nativeProductWorldDefaultRequested", true);
        evidence.put("oldVanillaSaveGuard", !text(hook.vanillaSavePolicy()).isBlank());
        evidence.put("summary", text(evidence.get("summary")).isBlank()
                ? "Native Loader product launch requests the product world preset by default and guards old vanilla saves."
                : evidence.get("summary"));
        return Map.copyOf(evidence);
    }

    private static Map<String, Object> productOnboardingHookReport(
            NativeHostServices services,
            EchoNativeProductHookPlan.ProductOnboardingHook hook
    ) {
        List<NativeLoaderMutationLedger.MutationRecord> records = new ArrayList<>();
        records.add(services.adapterCoreBackend().updatePlayerState(
                hook.playerId(),
                "nativeProductOnboardingProfile",
                hook.spawnProfile()
        ));
        if (!text(hook.spawnDimension()).isBlank()) {
            records.add(services.adapterCoreBackend().updateWorldState(
                    hook.spawnDimension(),
                    "nativeProductSpawnProfile",
                    hook.spawnProfile()
            ));
        }
        if (!text(hook.spawnDimension()).isBlank() && !text(hook.spawnStructureId()).isBlank()) {
            records.add(services.adapterCoreBackend().placeStructure(
                    hook.spawnDimension(),
                    hook.spawnStructureId(),
                    0,
                    80,
                    0
            ));
        }
        if (!text(hook.starterItemId()).isBlank()) {
            records.add(services.adapterCoreBackend().grantItem(
                    hook.playerId(),
                    hook.starterItemId(),
                    1
            ));
        }
        if (!text(hook.missionId()).isBlank()) {
            records.add(services.adapterCoreBackend().updateMission(
                    hook.missionId(),
                    text(hook.missionPhase()).isBlank() ? "started" : hook.missionPhase(),
                    hook.objectiveKey()
            ));
        }
        if (!text(hook.hudChannel()).isBlank() && !text(hook.briefing()).isBlank()) {
            records.add(services.adapterCoreBackend().emitHud(
                    hook.hudChannel(),
                    hook.briefing()
            ));
        }
        records.add(services.adapterCoreBackend().writeSaveData(
                "echo.native.productOnboarding." + hook.moduleId(),
                hook.spawnProfile()
        ));
        Map<String, Object> report = new LinkedHashMap<>(hook.evidence());
        report.put("moduleId", hook.moduleId());
        report.put("playerId", hook.playerId());
        report.put("spawnProfile", hook.spawnProfile());
        report.put("spawnDimension", hook.spawnDimension());
        report.put("spawnStructureId", hook.spawnStructureId());
        report.put("starterItemId", hook.starterItemId());
        report.put("missionId", hook.missionId());
        report.put("missionPhase", hook.missionPhase());
        report.put("objectiveKey", hook.objectiveKey());
        report.put("hudChannel", hook.hudChannel());
        report.put("nativeProductOnboardingHook", true);
        report.put("placeholderGameplayStartup", false);
        report.put("testScaffoldStartup", false);
        report.put("mutationRecords", records.stream().map(NativeLoaderMutationLedger.MutationRecord::toReport).toList());
        report.put("mutationRecordCount", records.size());
        report.put("mutatedRecordCount", records.stream()
                .filter(record -> successfulNativeHookStatus(record.status()))
                .count());
        report.put("liveProofMutationRecordCount", records.stream()
                .filter(EchoNativeProductLauncher::adapterCoreLiveProofSatisfied)
                .count());
        report.put("nativeProductOnboardingMutated", records.stream()
                .allMatch(record -> successfulNativeHookStatus(record.status())));
        report.put("nativeProductOnboardingLiveProofMutated", records.stream()
                .allMatch(EchoNativeProductLauncher::adapterCoreLiveProofSatisfied));
        report.put("summary", text(report.get("summary")).isBlank()
                ? "Native product onboarding mutates spawn, starter, mission, HUD, and save state through the Native Loader runtime."
                : report.get("summary"));
        return Map.copyOf(report);
    }

    private static EchoNativeProductHookExecution hookExecution(
            String surface,
            String moduleId,
            String hookId,
            EchoNativeLoadStatus status,
            Map<String, Object> evidence
    ) {
        return new EchoNativeProductHookExecution(
                surface == null ? "" : surface.trim(),
                moduleId == null ? "" : moduleId.trim(),
                hookId == null ? "" : hookId.trim(),
                status == null ? EchoNativeLoadStatus.UNSUPPORTED : status,
                evidence == null ? Map.of() : Map.copyOf(evidence)
        );
    }

    private static EchoNativeProductHookReport hookReport(
            NativeHostServices services,
            List<EchoNativeProductHookExecution> executions
    ) {
        List<EchoNativeProductHookExecution> safeExecutions = executions == null ? List.of() : List.copyOf(executions);
        Map<String, Object> clientReport = new LinkedHashMap<>();
        clientReport.put("serviceId", NativeLoaderClientUiHost.SERVICE_ID);
        clientReport.put("attached", services.clientUiHost().attached());
        clientReport.put("liveClientBridgeAttached", services.clientUiHost().liveClientBridgeAttached());
        clientReport.put("liveClientBridgeId", services.clientUiHost().liveClientBridgeId());
        clientReport.put("firstClassNativeClientRouteTable",
                services.clientUiHost().firstClassNativeClientRouteTable());
        clientReport.put("nativeClientRouteProcess", services.clientUiHost().nativeClientRouteProcess());
        clientReport.put("releaseClientRouteTrusted", services.clientUiHost().releaseClientRouteTrusted());
        clientReport.put("clientRouteMutationSupported", services.clientUiHost().clientRouteMutationSupported());
        clientReport.put("firstClassNativeClientRenderPipeline",
                services.clientUiHost().firstClassNativeClientRenderPipeline());
        clientReport.put("nativeClientRenderProcess", services.clientUiHost().nativeClientRenderProcess());
        clientReport.put("releaseClientRenderTrusted", services.clientUiHost().releaseClientRenderTrusted());
        clientReport.put("clientRenderMutationSupported", services.clientUiHost().clientRenderMutationSupported());
        clientReport.put("surfaceCount", services.clientUiHost().surfaceCount());
        clientReport.put("liveClientBridgeAcceptedSurfaceCount",
                services.clientUiHost().liveClientBridgeAcceptedSurfaceCount());
        clientReport.put("liveClientBridgeMutatedSurfaceCount",
                services.clientUiHost().liveClientBridgeMutatedSurfaceCount());
        clientReport.put("nativeClientRouteTableMutatedSurfaceCount",
                services.clientUiHost().nativeClientRouteTableMutatedSurfaceCount());
        clientReport.put("trustedClientRouteMutatedSurfaceCount",
                services.clientUiHost().trustedClientRouteMutatedSurfaceCount());
        clientReport.put("clientAssessment", services.clientUiHost().clientAssessment());
        clientReport.put("surfaces", services.clientUiHost().surfaces());
        clientReport.put("routeHostEvidence", services.clientUiHost().routeHostEvidence());

        return new EchoNativeProductHookReport(
                safeExecutions.size(),
                (int) safeExecutions.stream().filter(execution -> execution.status() == EchoNativeLoadStatus.MUTATED).count(),
                (int) services.lifecycleEventHost().publishedEvents().stream()
                        .filter(NativeLoaderLifecycleEventHost.PublishedEvent::handlerExecuted)
                        .count(),
                services.runtimeHost().mutatedSurfaces(),
                safeExecutions,
                services.registryHost().toReport(),
                services.lifecycleEventHost().toReport(),
                services.commandHost().toReport(),
                services.networkHost().toReport(),
                services.resourceHost().toReport(),
                services.configHost().toReport(),
                Map.copyOf(clientReport),
                services.adapterCoreBackend().mutationLedger().toReport()
        );
    }

    private static List<EchoNativeDiagnostic> releaseClasspathDiagnostics(
            String packId,
            EchoNativeAddonDescriptor descriptor
    ) {
        List<EchoNativeDiagnostic> diagnostics = new ArrayList<>();
        EchoNativeModuleDescriptor moduleDescriptor = EchoNativeModuleDescriptor.fromAddon(descriptor);
        List<Path> releaseClasspath = releaseClasspath(moduleDescriptor);
        if (releaseClasspath.isEmpty()) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-RELEASE-CLASSPATH-MISSING",
                    EchoNativeIssueSeverity.ERROR,
                    "Release launch requires resolved native classpath entries",
                    "Module '" + descriptor.id()
                            + "' declares a native entrypoint but no release native classpath entries could be resolved.",
                    descriptor.id(),
                    packId,
                    List.of(descriptorPath(descriptor)),
                    "Package the module with addon.jar/lib entries, declare access.nativeClasspath, or use "
                            + EchoNativeModuleDescriptor.INFERRED_CLASSPATH_TOKEN
                            + " so the scanner can resolve module output/artifact paths before release launch."
            ));
        }
        if (moduleDescriptor.compatibilityClasspathFallback()) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-RELEASE-CLASSPATH-FALLBACK",
                    EchoNativeIssueSeverity.ERROR,
                    "Release launch requires classpath entries without compatibility fallback",
                    "Module '" + descriptor.id()
                            + "' resolved native classpath through undeclared or unresolved fallback entries.",
                    descriptor.id(),
                    packId,
                    List.of(descriptorPath(descriptor)),
                    "Package the module and declare explicit artifact paths or "
                            + EchoNativeModuleDescriptor.INFERRED_CLASSPATH_TOKEN
                            + " resolving to existing module output/artifact paths before release launch."
            ));
        }
        List<String> missingEntries = moduleDescriptor.declaredClasspath().stream()
                .filter(path -> !Files.exists(path))
                .map(EchoNativeProductLauncher::path)
                .toList();
        if (!missingEntries.isEmpty()) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-RELEASE-CLASSPATH-ENTRY-MISSING",
                    EchoNativeIssueSeverity.ERROR,
                    "Release launch native classpath entries are missing",
                    "Module '" + descriptor.id()
                            + "' declares nativeClasspath entries that do not exist: "
                            + String.join(", ", missingEntries),
                    descriptor.id(),
                    packId,
                    List.of(descriptorPath(descriptor)),
                    "Build/package the module so every release nativeClasspath entry exists."
            ));
        }
        if (isSourceTreeDescriptor(descriptor.descriptorPath())) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-RELEASE-DESCRIPTOR-SOURCE-TREE",
                    EchoNativeIssueSeverity.ERROR,
                    "Release launch requires packaged descriptors",
                    "Module '" + descriptor.id()
                            + "' was discovered from src/main/resources instead of a packaged product/module descriptor.",
                    descriptor.id(),
                    packId,
                    List.of(descriptorPath(descriptor)),
                    "Build a product artifact layout, such as modules/<module>/META-INF/echo.mod.json with packaged classpath entries."
            ));
        }
        return List.copyOf(diagnostics);
    }

    private static EchoNativeProductPreWindowAssertionReport preWindowAssertionReport(
            EchoNativeScanResult scanResult,
            EchoNativeProductBootstrapProfileReport bootstrapProfile,
            NativeHostServices services,
            EchoNativeProductRuntimeCapabilityReport capabilities,
            boolean requireProductWindow
    ) {
        EchoNativePackProfile packProfile = scanResult.packProfile();
        List<EchoNativeAddonDescriptor> descriptors = scanResult.descriptors();
        long nativeEntrypointModules = descriptors.stream().filter(EchoNativeProductLauncher::hasNativeEntrypoint).count();
        long classpathReadyModules = descriptors.stream()
                .filter(EchoNativeProductLauncher::hasNativeEntrypoint)
                .filter(EchoNativeProductLauncher::moduleClasspathPreWindowReady)
                .count();
        List<String> requiredServiceIds = requiredNativeServiceIds();
        Set<String> registeredServiceIds = new TreeSet<>();
        Set<String> registeredServiceImplementations = new TreeSet<>();
        for (EchoNativeRegisteredService service : services.serviceRegistry().registeredServices()) {
            registeredServiceIds.add(service.serviceId());
            registeredServiceImplementations.add(service.implementationClass());
        }
        List<String> missingServiceIds = requiredServiceIds.stream()
                .filter(serviceId -> !registeredServiceIds.contains(serviceId))
                .toList();
        boolean classpathReady = nativeEntrypointModules > 0 && classpathReadyModules == nativeEntrypointModules;
        boolean serviceProvidersReady = missingServiceIds.isEmpty();
        boolean productProfileReady = packProfile != null
                && !text(packProfile.id()).isBlank()
                && !text(packProfile.rootModule()).isBlank()
                && descriptors.stream().anyMatch(descriptor -> text(descriptor.id()).equals(packProfile.rootModule()));
        boolean ashfallProduct = packProfile != null && "ashfall".equalsIgnoreCase(text(packProfile.id()));
        boolean ashfallProfileReady = !ashfallProduct
                || (productProfileReady
                && "echoashfallprotocol".equals(text(packProfile.rootModule()))
                && packProfile.requiredFeatures().contains("ashfall.world")
                && bootstrapProfile != null
                && bootstrapProfile.profileDriven());
        int routeCount = NativeLoaderClientRouteTable.routes().size();
        boolean routeRegistryReady = capabilities.clientSurfaceCount() > 0 && routeCount > 0;
        boolean resourceHostReady = capabilities.productResourcesReady();
        boolean ashfallDatapackWorldPresetReady = !ashfallProduct
                || (capabilities.dataPackResourceCount() > 0
                && capabilities.worldgenResourceCount() > 0
                && capabilities.worldPresetResourceCount() > 0);
        String blessedLaunchTask = "startNativeClient";
        String internalHandoffTask = "runNativeBootstrapClient";
        String bootstrapMainClass = "dev.echo.nativeplatform.bootstrap.EchoNativeBootstrapMain";
        String realMainClass = "net.minecraft.client.main.Main";
        boolean bootstrapHandoffAuthorityReady = true;
        boolean moduleReleaseReady = classpathReady
                && serviceProvidersReady
                && productProfileReady
                && ashfallProfileReady
                && bootstrapHandoffAuthorityReady;
        boolean productWindowReady = requireProductWindow
                && capabilities.fullReleaseRuntimeReady()
                && moduleReleaseReady
                && routeRegistryReady
                && resourceHostReady
                && ashfallDatapackWorldPresetReady;
        return new EchoNativeProductPreWindowAssertionReport(
                nativeEntrypointModules,
                classpathReadyModules,
                classpathReady,
                requiredServiceIds,
                List.copyOf(registeredServiceIds),
                missingServiceIds,
                serviceProvidersReady,
                productProfileReady,
                ashfallProfileReady,
                routeCount,
                routeRegistryReady,
                capabilities.mountedResourceCount(),
                capabilities.dataPackResourceCount(),
                capabilities.worldgenResourceCount(),
                capabilities.worldPresetResourceCount(),
                capabilities.resourcePackResourceCount(),
                resourceHostReady,
                ashfallDatapackWorldPresetReady,
                List.copyOf(registeredServiceImplementations),
                blessedLaunchTask,
                internalHandoffTask,
                bootstrapMainClass,
                realMainClass,
                bootstrapHandoffAuthorityReady,
                moduleReleaseReady,
                productWindowReady
        );
    }

    private static boolean moduleClasspathPreWindowReady(EchoNativeAddonDescriptor descriptor) {
        Map<String, Object> access = descriptor.access() == null ? Map.of() : descriptor.access();
        Object rawClasspath = access.get("nativeClasspath");
        if (!(rawClasspath instanceof List<?> classpath) || classpath.isEmpty()) {
            return false;
        }
        Path moduleRoot = packagedModuleRoot(descriptor.descriptorPath());
        if (moduleRoot == null) {
            return false;
        }
        boolean hasClasspathEntry = false;
        for (Object item : classpath) {
            String raw = text(item);
            if (raw.isBlank() || EchoNativeModuleDescriptor.INFERRED_CLASSPATH_TOKEN.equals(raw)) {
                return false;
            }
            Path path = Path.of(raw);
            Path resolved = path.isAbsolute() ? path.normalize() : moduleRoot.resolve(path).normalize();
            if (!Files.exists(resolved)) {
                return false;
            }
            hasClasspathEntry = true;
        }
        return hasClasspathEntry;
    }

    private static Path packagedModuleRoot(Path descriptorPath) {
        if (descriptorPath == null) {
            return null;
        }
        Path metaInf = descriptorPath.toAbsolutePath().normalize().getParent();
        if (metaInf == null || !"META-INF".equals(metaInf.getFileName().toString())) {
            return null;
        }
        Path moduleRoot = metaInf.getParent();
        if (moduleRoot == null || moduleRoot.getParent() == null) {
            return null;
        }
        return "modules".equals(moduleRoot.getParent().getFileName().toString()) ? moduleRoot : null;
    }

    private static List<String> requiredNativeServiceIds() {
        return List.of(
                EchoNativeRegistryHost.SERVICE_ID,
                NativeLoaderCommandHost.SERVICE_ID,
                NativeLoaderConfigHost.SERVICE_ID,
                NativeLoaderResourceHost.SERVICE_ID,
                NativeLoaderNetworkHost.SERVICE_ID,
                NativeLoaderRuntimeHost.SERVICE_ID,
                NativeLoaderLifecycleEventHost.LIFECYCLE_SERVICE_ID,
                NativeLoaderLifecycleEventHost.EVENT_SERVICE_ID,
                NativeLoaderAdapterCoreBackend.SERVICE_ID,
                NativeLoaderClientUiHost.SERVICE_ID
        );
    }

    private static List<EchoNativeDiagnostic> preWindowAssertionDiagnostics(
            String packId,
            EchoNativeProductPreWindowAssertionReport report,
            boolean requireProductWindow
    ) {
        List<EchoNativeDiagnostic> diagnostics = new ArrayList<>();
        if (!report.nativeModuleClasspathReady()) {
            diagnostics.add(preWindowDiagnostic(
                    packId,
                    "ECHO-NATIVE-PREWINDOW-CLASSPATH",
                    "Native product pre-window classpath assertion failed",
                    "Only " + report.classpathReadyModuleCount() + "/" + report.nativeEntrypointModuleCount()
                            + " native entrypoint module(s) have resolved classpath entries without compatibility fallback.",
                    "Package every native entrypoint module with explicit native classpath entries before the product window can be created."
            ));
        }
        if (!report.serviceProvidersReady()) {
            diagnostics.add(preWindowDiagnostic(
                    packId,
                    "ECHO-NATIVE-PREWINDOW-SERVICES",
                    "Native product pre-window service-provider assertion failed",
                    "The Native Loader service registry is missing required service(s): "
                            + String.join(", ", report.missingServiceIds()),
                    "Register all Native Loader host services in the product launcher before handing off to any client window."
            ));
        }
        if (!report.productProfileReady() || !report.ashfallProfileReady()) {
            diagnostics.add(preWindowDiagnostic(
                    packId,
                    "ECHO-NATIVE-PREWINDOW-PROFILE",
                    "Native product pre-window product-profile assertion failed",
                    "The product profile/root module/bootstrap profile is not ready for native-owned launch.",
                    "Resolve the pack root descriptor and Ashfall native bootstrap profile before creating the Native Loader window."
            ));
        }
        if (requireProductWindow && !report.routeRegistryReady()) {
            diagnostics.add(preWindowDiagnostic(
                    packId,
                    "ECHO-NATIVE-PREWINDOW-ROUTES",
                    "Native product pre-window route-registry assertion failed",
                    "The client route registry contains " + report.routeRegistryCount()
                            + " route(s); module surfaces must populate Terminal, Index, Lens, HoloMap, HUD, menu, and loading routes before the window opens.",
                    "Route module-declared client surfaces through NativeLoaderClientUiHost and NativeLoaderClientRouteTable before full product launch."
            ));
        }
        if (requireProductWindow && !report.resourceHostReady()) {
            diagnostics.add(preWindowDiagnostic(
                    packId,
                    "ECHO-NATIVE-PREWINDOW-RESOURCES",
                    "Native product pre-window resource-host assertion failed",
                    "Native resource host counts: mounted=" + report.mountedResourceCount()
                            + ", datapack=" + report.dataPackResourceCount()
                            + ", worldgen=" + report.worldgenResourceCount()
                            + ", world_preset=" + report.worldPresetResourceCount()
                            + ", resource_pack=" + report.resourcePackResourceCount() + ".",
                    "Mount product data packs, worldgen, world presets, and client resources through NativeLoaderResourceHost before full product launch."
            ));
        }
        if (requireProductWindow && !report.ashfallDatapackWorldPresetReady()) {
            diagnostics.add(preWindowDiagnostic(
                    packId,
                    "ECHO-NATIVE-PREWINDOW-ASHFALL-WORLD",
                    "Native product pre-window Ashfall datapack/world-preset assertion failed",
                    "Ashfall launch requires a native datapack, worldgen declarations, and a world preset before world startup.",
                    "Package or stage the Ashfall datapack/world preset on the Native Loader path before creating or opening the product world."
            ));
        }
        if (!report.bootstrapHandoffAuthorityReady()) {
            diagnostics.add(preWindowDiagnostic(
                    packId,
                    "ECHO-NATIVE-PREWINDOW-HANDOFF-AUTHORITY",
                    "Native product pre-window bootstrap handoff authority assertion failed",
                    "Expected blessed launch task startNativeClient, internal handoff task runNativeBootstrapClient, bootstrap main EchoNativeBootstrapMain, and real Minecraft main net.minecraft.client.main.Main.",
                    "Keep product launch validation in EchoNativeProductLauncher before handing off through the internal bootstrap task."
            ));
        }
        return List.copyOf(diagnostics);
    }

    private static EchoNativeDiagnostic preWindowDiagnostic(
            String packId,
            String code,
            String summary,
            String detail,
            String remediation
    ) {
        return new EchoNativeDiagnostic(
                code,
                EchoNativeIssueSeverity.ERROR,
                summary,
                detail,
                null,
                packId,
                List.of(
                        "echo-native-platform/echo-native-product-launcher/src/main/java/dev/echo/nativeplatform/product/EchoNativeProductLauncher.java",
                        "echo-native-platform/echo-native-loader/src/main/java/dev/echo/nativeplatform/loader"
                ),
                remediation
        );
    }

    private static List<Path> releaseClasspath(EchoNativeModuleDescriptor moduleDescriptor) {
        return moduleDescriptor.declaredClasspath().isEmpty()
                ? moduleDescriptor.generatedClasspath()
                : moduleDescriptor.declaredClasspath();
    }

    private static List<EchoNativeDiagnostic> releaseEntrypointApiDiagnostics(
            String packId,
            EchoNativeAddonDescriptor descriptor
    ) {
        EchoNativeModuleDescriptor moduleDescriptor = EchoNativeModuleDescriptor.fromAddon(descriptor);
        if (!moduleDescriptor.hasEntrypoint()) {
            return List.of();
        }
        if (moduleDescriptor.classpath().isEmpty()) {
            return List.of();
        }
        try (EchoNativeModuleClassLoader classLoader = new EchoNativeModuleClassLoader(
                moduleDescriptor.classpath(),
                EchoNativeProductLauncher.class.getClassLoader()
        )) {
            Class<?> entrypointType = classLoader.loadClass(moduleDescriptor.entrypoint());
            if (EchoNativeModuleEntrypoint.class.isAssignableFrom(entrypointType)) {
                return List.of();
            }
            boolean legacyActivateNative = hasLegacyActivateNative(entrypointType);
            return List.of(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-RELEASE-ENTRYPOINT-API",
                    EchoNativeIssueSeverity.ERROR,
                    "Release launch requires direct native entrypoint API",
                    "Module '" + descriptor.id() + "' declares nativeEntrypoint '"
                            + moduleDescriptor.entrypoint()
                            + "', but that class does not implement EchoNativeModuleEntrypoint"
                            + (legacyActivateNative ? " and still exposes legacy activateNative(Map)." : "."),
                    descriptor.id(),
                    packId,
                    List.of(descriptorPath(descriptor)),
                    "Implement EchoNativeModuleEntrypoint or EchoNativeModuleAdapter directly; legacy activateNative(Map) shims are non-release only."
            ));
        } catch (ClassNotFoundException exception) {
            return List.of(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-RELEASE-ENTRYPOINT-UNRESOLVED",
                    EchoNativeIssueSeverity.ERROR,
                    "Release launch native entrypoint class is unresolved",
                    "Module '" + descriptor.id() + "' declares nativeEntrypoint '"
                            + moduleDescriptor.entrypoint()
                            + "', but it could not be loaded from the resolved native classpath.",
                    descriptor.id(),
                    packId,
                    List.of(descriptorPath(descriptor)),
                    "Package the native entrypoint class and every required dependency into access.nativeClasspath."
            ));
        } catch (LinkageError | RuntimeException exception) {
            return List.of(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-RELEASE-ENTRYPOINT-LINKAGE",
                    EchoNativeIssueSeverity.ERROR,
                    "Release launch native entrypoint class cannot link",
                    "Module '" + descriptor.id() + "' declares nativeEntrypoint '"
                            + moduleDescriptor.entrypoint()
                            + "', but class loading failed with "
                            + exception.getClass().getSimpleName() + ": " + exception.getMessage(),
                    descriptor.id(),
                    packId,
                    List.of(descriptorPath(descriptor)),
                    "Package the entrypoint and dependency classes required to link the native entrypoint without NeoForge-only runtime assumptions."
            ));
        } catch (IOException exception) {
            return List.of(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-RELEASE-ENTRYPOINT-CLASSPATH-CLOSE",
                    EchoNativeIssueSeverity.WARNING,
                    "Release launch native entrypoint classpath cleanup failed",
                    "Module '" + descriptor.id() + "' entrypoint API was checked, but closing the temporary classloader failed: "
                            + exception.getMessage(),
                    descriptor.id(),
                    packId,
                    List.of(descriptorPath(descriptor)),
                    "Inspect the native classpath for locked jars or filesystem access issues."
            ));
        }
    }

    private static boolean hasLegacyActivateNative(Class<?> entrypointType) {
        try {
            entrypointType.getMethod("activateNative", Map.class);
            return true;
        } catch (NoSuchMethodException exception) {
            return false;
        }
    }

    private static List<EchoNativeDiagnostic> releaseBootstrapProfileDiagnostics(
            String packId,
            EchoNativeProductBootstrapProfileReport bootstrapProfile
    ) {
        if (bootstrapProfile != null && bootstrapProfile.profileDriven()) {
            return List.of();
        }
        String rootModule = bootstrapProfile == null ? "" : bootstrapProfile.rootModuleId();
        String descriptorPath = bootstrapProfile == null ? "" : bootstrapProfile.descriptorPath();
        return List.of(new EchoNativeDiagnostic(
                "ECHO-NATIVE-PRODUCT-BOOTSTRAP-PROFILE-MISSING",
                EchoNativeIssueSeverity.WARNING,
                "Release launch has no root-addon bootstrap profile",
                "The product launcher will still run the generic module launch path, but no root addon descriptor supplied access.nativeBootstrapProfile.",
                rootModule,
                packId,
                descriptorPath == null || descriptorPath.isBlank() ? List.of() : List.of(descriptorPath),
                "Declare access.nativeBootstrapProfile on the pack root addon descriptor when the product needs profile-specific bootstrap behavior."
        ));
    }

    private static List<String> adapterCoreDomains(EchoNativeAddonDescriptor descriptor) {
        if (descriptor.access() == null || !(descriptor.access().get("adapterCore") instanceof Map<?, ?> adapterCore)) {
            return List.of();
        }
        Object domains = adapterCore.get("domains");
        if (!(domains instanceof List<?> list)) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (Object item : list) {
            String value = item == null ? "" : String.valueOf(item).trim();
            if (!value.isBlank()) {
                result.add(value);
            }
        }
        return List.copyOf(result);
    }

    private static Map<String, Object> descriptorDomainEvidence(EchoNativeAddonDescriptor descriptor, String domain) {
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("source", "descriptor.access.adapterCore.domains");
        evidence.put("domain", domain);
        evidence.put("descriptorPath", descriptorPath(descriptor));
        evidence.put("moduleId", descriptor.id());
        evidence.put("liveMinecraftMutation", false);
        evidence.put("minecraftRuntimeAccessed", false);
        return Map.copyOf(evidence);
    }

    private static boolean hasNativeEntrypoint(EchoNativeAddonDescriptor descriptor) {
        return descriptor != null
                && descriptor.access() != null
                && !text(descriptor.access().get("nativeEntrypoint")).isBlank();
    }

    private static String text(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private static EchoNativeProductRuntimeCapabilityReport runtimeCapabilityReport(NativeHostServices services) {
        NativeLoaderRuntimeHost runtimeHost = services.runtimeHost();
        NativeLoaderResourceHost resourceHost = services.resourceHost();
        NativeLoaderClientUiHost clientUiHost = services.clientUiHost();
        NativeLoaderLifecycleEventHost lifecycleEventHost = services.lifecycleEventHost();
        NativeLoaderCommandHost commandHost = services.commandHost();
        NativeLoaderConfigHost configHost = services.configHost();
        NativeLoaderNetworkHost networkHost = services.networkHost();
        NativeLoaderMutationLedger mutationLedger = services.adapterCoreBackend().mutationLedger();
        Map<String, Object> runtimeReport = runtimeHost.runtimeHostReport();
        Map<String, Object> clientAssessment = clientUiHost.clientAssessment();
        Map<String, Object> lifecycleReport = lifecycleEventHost.toReport();
        Map<String, Object> commandReport = commandHost.toReport();
        Map<String, Object> configReport = configHost.toReport();
        Map<String, Object> networkReport = networkHost.toReport();
        return new EchoNativeProductRuntimeCapabilityReport(
                runtimeHost.runtimeHostRegistered(),
                runtimeHost.firstClassNativeRuntime(),
                runtimeHost.delegateRequired(),
                runtimeHost.liveMinecraftAttached(),
                runtimeHost.releaseRuntimeTrusted(),
                runtimeHost.liveRuntimeBridgeAttached(),
                services.registryHost().liveRegistryBridgeAttached(),
                services.registryHost().firstClassNativeRegistry(),
                services.registryHost().releaseRegistryTrusted(),
                services.registryHost().totalRegistered(),
                services.registryHost().liveRegistryBridgeMutatedEntryCount(),
                services.registryHost().nativeRegistryHostMutatedEntryCount(),
                services.registryHost().trustedRegistryMutatedEntryCount(),
                services.registryHost().registeredOnlyFirstClassRegistryKinds(),
                services.registryHost().registeredOnlyFirstClassRegistryIds(),
                services.registryHost().registeredOnlyFirstClassRegistryIdsByKind(),
                services.registryHost().failedFirstClassRegistryKinds(),
                services.registryHost().failedFirstClassRegistryIds(),
                services.registryHost().failedFirstClassRegistryIdsByKind(),
                services.registryHost().untrustedMutationFirstClassRegistryKinds(),
                services.registryHost().untrustedMutationReasonCounts(),
                services.registryHost().untrustedMutationReasonCountsByKind(),
                services.registryHost().registryBridgeMutationReconciliation(),
                runtimeHost.context().savesDirectory() != null,
                intValue(runtimeReport.get("fallbackMirrorMutationCount")),
                resourceHost.mountedResourceCount(),
                resourceHost.worldStartupResourceCount(),
                resourceHost.worldgenResourceCount(),
                resourceHost.worldPresetResourceCount(),
                resourceHost.dataPackResourceCount(),
                resourceHost.resourcePackResourceCount(),
                resourceHost.structureResourceCount(),
                resourceHost.tagResourceCount(),
                lifecycleEventHost.lifecycleEventCount(),
                lifecycleEventHost.failedLifecycleEventCount(),
                lifecycleEventHost.publishedEventCount(),
                lifecycleEventHost.eventSubscriptionCount(),
                lifecycleEventHost.executedEventHandlerCount(),
                lifecycleEventHost.liveRuntimeMutationCount(),
                booleanValue(lifecycleReport.get("minecraftRuntimeAccessed")),
                booleanValue(lifecycleReport.get("liveRuntimeReleaseProofSatisfied")),
                commandHost.queuedCommandCount(),
                commandHost.commandFailureCount(),
                commandHost.liveRuntimeMutationCount(),
                booleanValue(commandReport.get("minecraftRuntimeAccessed")),
                booleanValue(commandReport.get("liveRuntimeReleaseProofSatisfied")),
                configHost.registeredConfigCount(),
                configHost.liveRuntimeMutationCount(),
                booleanValue(configReport.get("minecraftRuntimeAccessed")),
                booleanValue(configReport.get("liveRuntimeReleaseProofSatisfied")),
                networkHost.boundPacketCount(),
                networkHost.packetFailureCount(),
                networkHost.liveRuntimeMutationCount(),
                booleanValue(networkReport.get("minecraftRuntimeAccessed")),
                booleanValue(networkReport.get("liveRuntimeReleaseProofSatisfied")),
                mutationLedger.recordCount(),
                mutationLedger.mutatedRecordCount(),
                mutationLedger.liveRuntimeProofRecordCount(),
                adapterCoreLiveRuntimeProofSurfaces(mutationLedger),
                mutationLedger.liveRuntimeProofRecordCountBySurface("save_data"),
                runtimeHost.supportedSurfaces(),
                runtimeHost.mutatedSurfaces(),
                clientUiHost.attached(),
                clientUiHost.liveClientBridgeAttached(),
                clientUiHost.nativeLoaderOwnsClientHostServices(),
                clientUiHost.neoForgeClientEventsCompatibilityAdaptersOnly(),
                booleanValue(clientAssessment.get("liveClientAttached")),
                releaseClientTrusted(clientAssessment),
                booleanValue(clientAssessment.get("headlessClientSurface")),
                clientUiHost.firstClassNativeClientRouteTable(),
                clientUiHost.nativeClientRouteProcess(),
                clientUiHost.releaseClientRouteTrusted(),
                clientUiHost.clientRouteMutationSupported(),
                clientUiHost.firstClassNativeClientRenderPipeline(),
                clientUiHost.nativeClientRenderProcess(),
                clientUiHost.releaseClientRenderTrusted(),
                clientUiHost.clientRenderMutationSupported(),
                clientUiHost.surfaceCount(),
                clientUiHost.liveClientBridgeAcceptedSurfaceCount(),
                clientUiHost.liveClientBridgeMutatedSurfaceCount(),
                clientUiHost.nativeClientRouteTableMutatedSurfaceCount(),
                clientUiHost.trustedClientRouteMutatedSurfaceCount()
        );
    }

    private static List<String> adapterCoreLiveRuntimeProofSurfaces(NativeLoaderMutationLedger mutationLedger) {
        Set<String> surfaces = new LinkedHashSet<>();
        for (NativeLoaderMutationLedger.MutationRecord record : mutationLedger.records()) {
            if (adapterCoreTopLevelProofSatisfied(record)) {
                surfaces.add(record.surface());
            }
        }
        return List.copyOf(surfaces);
    }

    private static boolean adapterCoreTopLevelProofSatisfied(NativeLoaderMutationLedger.MutationRecord record) {
        if (record.status() != EchoNativeLoadStatus.MUTATED
                || !record.liveRuntimeAccessed()
                || !record.minecraftRuntimeAccessed()
                || !record.liveRuntimeMutationSupported()
                || record.mirrorOnlyReleaseProof()
                || !record.liveRuntimeReleaseProofSatisfied()
                || !record.liveRuntimeSurfaceMutationSatisfied()
                || text(record.surface()).isBlank()) {
            return false;
        }
        Map<String, Object> recordReport = record.toReport();
        Map<String, Object> proofEvidence = objectMap(recordReport.get("surfaceLiveRuntimeProofEvidence"));
        boolean dispatchProof = booleanValue(proofEvidence.get("subsystemLiveRuntimeDispatchProofSatisfied"))
                || booleanValue(proofEvidence.get("liveRuntimeDispatchProofSatisfied"));
        boolean minecraftAccess = booleanValue(proofEvidence.get("minecraftRuntimeAccessed"))
                || booleanValue(proofEvidence.get("liveRuntimeDispatchMinecraftAccessed"));
        boolean liveMutation = booleanValue(proofEvidence.get("liveMinecraftMutation"))
                || booleanValue(proofEvidence.get("liveRuntimeDispatchLiveMutation"));
        return !proofEvidence.isEmpty()
                && dispatchProof
                && minecraftAccess
                && booleanValue(proofEvidence.get("liveRuntimeDispatchMutationSupported"))
                && liveMutation
                && !text(recordReport.get("liveRuntimeDispatchId")).isBlank()
                && text(recordReport.get("liveRuntimeDispatchId"))
                .equals(text(record.runtimeEvidence().get("adapterCoreSurfaceDispatchId")))
                && !text(recordReport.get("liveRuntimeSurface")).isBlank();
    }

    private static List<EchoNativeDiagnostic> releaseRuntimeCapabilityDiagnostics(
            String packId,
            EchoNativeProductRuntimeCapabilityReport capabilities
    ) {
        List<EchoNativeDiagnostic> diagnostics = new ArrayList<>();
        if (!capabilities.firstClassNativeRuntime() && !capabilities.liveMinecraftAttached()) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-RELEASE-RUNTIME-MISSING",
                    EchoNativeIssueSeverity.WARNING,
                    "Release launch has no trusted runtime attachment",
                    "The product launcher registered the Native Loader runtime host, but no live Minecraft runtime or first-class native runtime attachment was accepted.",
                    null,
                    packId,
                    List.of("echo-native-platform/echo-native-loader/src/main/java/dev/echo/nativeplatform/loader/NativeLoaderRuntimeHost.java"),
                    "Attach either a trusted first-class native runtime or a trusted live Minecraft runtime before claiming full public release parity."
            ));
        }
        if ((capabilities.firstClassNativeRuntime() || capabilities.liveMinecraftAttached())
                && !capabilities.liveRuntimeTrusted()) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-RELEASE-RUNTIME-UNTRUSTED",
                    EchoNativeIssueSeverity.WARNING,
                    "Release launch runtime attachment is not trusted release evidence",
                    "The product launcher has a runtime attachment, but it is missing explicit release-trusted evidence for either a native runtime process or a real Minecraft process.",
                    null,
                    packId,
                    List.of("echo-native-platform/echo-native-loader/src/main/java/dev/echo/nativeplatform/loader/NativeLoaderLiveRuntimeAttachment.java"),
                    "Set releaseRuntimeTrusted=true with nativeRuntimeProcess=true for a first-class Native Loader runtime, or with realMinecraftProcess=true for a live Minecraft runtime."
            ));
        } else if (capabilities.liveRuntimeTrusted() && !capabilities.nativeRuntimeDispatchReady()) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-RELEASE-RUNTIME-DISPATCH-MISSING",
                    EchoNativeIssueSeverity.WARNING,
                    "Release launch runtime attachment has no trusted dispatch path",
                    "The product launcher has trusted runtime evidence, but AdapterCore host calls still have neither an attached"
                            + " NativeLoaderLiveRuntimeBridge nor a trusted first-class Native Loader runtime dispatch path.",
                    null,
                    packId,
                    List.of(
                            "echo-native-platform/echo-native-loader/src/main/java/dev/echo/nativeplatform/loader/NativeLoaderRuntimeHost.java",
                            "echo-native-platform/echo-native-loader/src/main/java/dev/echo/nativeplatform/loader/NativeLoaderLiveRuntimeBridge.java"
                    ),
                    "Use the first-class Native Loader runtime host as the trusted mutation path, or attach a NativeLoaderLiveRuntimeBridge that dispatches AdapterCore operations to a real live runtime."
            ));
        }
        if (capabilities.liveRuntimeTrusted() && !capabilities.agent5LiveRuntimeSurfaceProofReady()) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-RELEASE-RUNTIME-LIVE-PROOF-MISSING",
                    EchoNativeIssueSeverity.WARNING,
                    "Release launch runtime systems are missing live mutation proof",
                    "Agent 5 runtime counts: lifecycle_live_mutations=" + capabilities.lifecycleLiveRuntimeMutationCount()
                            + " lifecycle_minecraft_accessed=" + capabilities.lifecycleMinecraftRuntimeAccessed()
                            + " lifecycle_live_release_proof=" + capabilities.lifecycleLiveRuntimeReleaseProofSatisfied()
                            + ", command_live_mutations=" + capabilities.commandLiveRuntimeMutationCount()
                            + " command_minecraft_accessed=" + capabilities.commandMinecraftRuntimeAccessed()
                            + " command_live_release_proof=" + capabilities.commandLiveRuntimeReleaseProofSatisfied()
                            + ", config_live_mutations=" + capabilities.configLiveRuntimeMutationCount()
                            + " config_minecraft_accessed=" + capabilities.configMinecraftRuntimeAccessed()
                            + " config_live_release_proof=" + capabilities.configLiveRuntimeReleaseProofSatisfied()
                            + ", network_live_mutations=" + capabilities.networkLiveRuntimeMutationCount()
                            + " network_minecraft_accessed=" + capabilities.networkMinecraftRuntimeAccessed()
                            + " network_live_release_proof=" + capabilities.networkLiveRuntimeReleaseProofSatisfied()
                            + ", adaptercore_live_proof_records=" + capabilities.adapterCoreLiveRuntimeProofRecordCount()
                            + ", adaptercore_live_proof_surfaces=" + capabilities.adapterCoreLiveRuntimeProofSurfaces()
                            + ", required_adaptercore_live_proof_surfaces="
                            + EchoNativeProductRuntimeCapabilityReport.requiredAgent5AdapterCoreLiveProofSurfaces()
                            + ". Release runtime parity cannot be satisfied by a trusted attachment marker or mirror state alone.",
                    null,
                    packId,
                    List.of(
                            "echo-native-platform/echo-native-loader/src/main/java/dev/echo/nativeplatform/loader/NativeLoaderLiveRuntimeBridge.java",
                            "echo-native-platform/echo-native-loader/src/main/java/dev/echo/nativeplatform/loader/NativeLoaderAdapterCoreBackend.java"
                    ),
                    "Dispatch lifecycle, command, config, network, and AdapterCore runtime surfaces through an attached NativeLoaderLiveRuntimeBridge before claiming live runtime parity."
            ));
        }
        if (!capabilities.liveRegistryBridgeAttached() && !capabilities.firstClassNativeRegistry()) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-RELEASE-REGISTRY-BRIDGE-MISSING",
                    EchoNativeIssueSeverity.WARNING,
                    "Release launch registry host has no trusted registry bridge",
                    "Native registry declarations still have no attached live Minecraft registry bridge or first-class native registry bridge.",
                    null,
                    packId,
                    List.of("echo-native-platform/echo-native-loader/src/main/java/dev/echo/nativeplatform/loader/NativeLoaderLiveRegistryBridge.java"),
                    "Attach either a trusted first-class native registry bridge or a NativeLoaderLiveRegistryBridge that dispatches to the live Minecraft registry."
            ));
        }
        if (capabilities.firstClassNativeRegistry() && !capabilities.releaseRegistryTrusted()) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-RELEASE-REGISTRY-UNTRUSTED",
                    EchoNativeIssueSeverity.WARNING,
                    "Release launch native registry bridge is not trusted release evidence",
                    "The product launcher has a first-class native registry bridge, but it is missing explicit release-trusted registry evidence.",
                    null,
                    packId,
                    List.of("echo-native-platform/echo-native-loader/src/main/java/dev/echo/nativeplatform/loader/NativeLoaderLiveRegistryBridge.java"),
                    "Set releaseRegistryTrusted=true and nativeRegistryProcess=true on first-party native registry bridges before claiming registry parity."
            ));
        }
        if (capabilities.registryEntryCount() > 0
                && capabilities.trustedRegistryMutatedEntryCount() < capabilities.registryEntryCount()) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-RELEASE-REGISTRY-DECLARATIONS-NOT-MUTATED",
                    EchoNativeIssueSeverity.WARNING,
                    "Release launch registry declarations did not all mutate through a trusted registry bridge",
                    "The native registry host recorded " + capabilities.registryEntryCount()
                            + " declaration(s), but only "
                            + capabilities.trustedRegistryMutatedEntryCount()
                            + " mutated through a trusted live or native registry bridge.",
                    null,
                    packId,
                    List.of("echo-native-platform/echo-native-loader/src/main/java/dev/echo/nativeplatform/loader/EchoNativeRegistryHost.java"),
                    "Dispatch every module-declared item, block, entity, block entity, menu, sound, particle, effect, command, data component, recipe, creative tab, biome, worldgen, and client asset through a trusted registry bridge before claiming full registry parity."
            ));
        }
        if (!capabilities.registeredOnlyFirstClassRegistryKinds().isEmpty()
                || !capabilities.registeredOnlyFirstClassRegistryIds().isEmpty()
                || !capabilities.registeredOnlyFirstClassRegistryIdsByKind().isEmpty()) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-RELEASE-REGISTRY-REGISTERED-ONLY",
                    EchoNativeIssueSeverity.WARNING,
                    "Release launch registry declarations did not reach trusted mutation",
                    "The native registry host recorded first-class registry kind(s) "
                            + capabilities.registeredOnlyFirstClassRegistryKinds()
                            + " without complete trusted live mutation. Non-mutated first-class ids: "
                            + capabilities.registeredOnlyFirstClassRegistryIds() + ".",
                    null,
                    packId,
                    List.of(
                            "echo-native-platform/echo-native-loader/src/main/java/dev/echo/nativeplatform/loader/EchoNativeRegistryHost.java",
                            "echo-native-platform/echo-native-loader/src/main/java/dev/echo/nativeplatform/loader/NativeLoaderLiveRegistryBridge.java"
                    ),
                    "Dispatch every first-class registry declaration through a trusted live/native registry bridge; REGISTERED, RESOLVED, LOADED, DISCOVERED, or UNSUPPORTED is not release registry mutation."
            ));
        }
        if (!capabilities.failedFirstClassRegistryKinds().isEmpty()
                || !capabilities.failedFirstClassRegistryIds().isEmpty()
                || !capabilities.failedFirstClassRegistryIdsByKind().isEmpty()) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-RELEASE-REGISTRY-FAILED",
                    EchoNativeIssueSeverity.WARNING,
                    "Release launch registry declarations failed before mutation",
                    "The native registry host recorded failed first-class registry kind(s) "
                            + capabilities.failedFirstClassRegistryKinds()
                            + ". Failed first-class ids: "
                            + capabilities.failedFirstClassRegistryIds() + ".",
                    null,
                    packId,
                    List.of(
                            "echo-native-platform/echo-native-loader/src/main/java/dev/echo/nativeplatform/loader/EchoNativeRegistryHost.java",
                            "echo-native-platform/echo-native-loader/src/main/java/dev/echo/nativeplatform/loader/NativeLoaderLiveRegistryBridge.java"
                    ),
                    "Fix invalid, duplicate, or live-bridge-failed first-class registry declarations before claiming registry parity; failed declarations are not registered-only evidence and cannot satisfy release mutation proof."
            ));
        }
        if (!capabilities.untrustedMutationFirstClassRegistryKinds().isEmpty()
                || !capabilities.untrustedMutationReasonCounts().isEmpty()
                || !capabilities.untrustedMutationReasonCountsByKind().isEmpty()) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-RELEASE-REGISTRY-MUTATION-UNTRUSTED",
                    EchoNativeIssueSeverity.WARNING,
                    "Release launch registry mutations are missing per-entry live proof",
                    "The native registry host saw MUTATED status for first-class registry kind(s) "
                            + capabilities.untrustedMutationFirstClassRegistryKinds()
                            + ", but those entries did not include trusted live mutation proof. Reason counts: "
                            + capabilities.untrustedMutationReasonCounts() + ".",
                    null,
                    packId,
                    List.of(
                            "echo-native-platform/echo-native-loader/src/main/java/dev/echo/nativeplatform/loader/EchoNativeRegistryHost.java",
                            "echo-native-platform/echo-native-loader/src/main/java/dev/echo/nativeplatform/loader/NativeLoaderLiveRegistryBridge.java"
                    ),
                    "Return a correlated registryMutationRecord for every first-class registry mutation with matching identity and first-class/native-process/release-trusted metadata; status-only MUTATED evidence is not release-trusted."
            ));
        }
        if (!capabilities.registryBridgeMutationEvidenceReconciled()) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-RELEASE-REGISTRY-BRIDGE-EVIDENCE-MISMATCH",
                    EchoNativeIssueSeverity.WARNING,
                    "Release launch registry bridge aggregate evidence does not match trusted entries",
                    "The native registry host accepted trusted per-entry mutation proof, but the bridge aggregate mutation ids, count, or record map did not reconcile. "
                            + "Missing from bridge evidence: "
                            + capabilities.registryBridgeMutationReconciliationList("missingFromBridgeEvidence")
                            + "; aggregate ids without trusted entries: "
                            + capabilities.registryBridgeMutationReconciliationList("bridgeEvidenceWithoutTrustedEntry")
                            + "; trusted entries missing from aggregate record map: "
                            + capabilities.registryBridgeMutationReconciliationList("trustedRecordsMissingFromBridgeRecordMap")
                            + "; aggregate record-map ids without trusted entries: "
                            + capabilities.registryBridgeMutationReconciliationList("bridgeRecordMapWithoutTrustedEntry")
                            + "; aggregate record payload mismatches: "
                            + capabilities.registryBridgeMutationReconciliationList("bridgeRecordMapProofMismatches")
                            + ".",
                    null,
                    packId,
                    List.of(
                            "echo-native-platform/echo-native-loader/src/main/java/dev/echo/nativeplatform/loader/EchoNativeRegistryHost.java",
                            "echo-native-platform/echo-native-loader/src/main/java/dev/echo/nativeplatform/loader/NativeLoaderLiveRegistryBridge.java"
                    ),
                    "Keep liveRegistryBridgeEvidence.mutatedRecordIds, mutatedRecordCount, and mutatedRecords in the same registry:namespace:id key shape as the host's trusted per-entry mutation records before claiming registry parity."
            ));
        }
        if (capabilities.mountedResourceCount() == 0) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-RELEASE-RESOURCES-MISSING",
                    EchoNativeIssueSeverity.WARNING,
                    "Release launch mounted no native product resources",
                    "The product launcher did not mount any resources through NativeLoaderResourceHost, so client assets, datapacks, worldgen, and world presets are not proven on the native path.",
                    null,
                    packId,
                    List.of("echo-native-platform/echo-native-loader/src/main/java/dev/echo/nativeplatform/loader/NativeLoaderResourceHost.java"),
                    "Route module-declared datapacks, resource packs, worldgen, world presets, tags, and structures through the native resource host before claiming product release readiness."
            ));
        }
        if (!capabilities.productWorldStartupResourcesReady()) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-RELEASE-WORLD-STARTUP-RESOURCES-INCOMPLETE",
                    EchoNativeIssueSeverity.WARNING,
                    "Release launch world startup resources are incomplete",
                    "Native resource host counts: datapack=" + capabilities.dataPackResourceCount()
                            + ", worldgen=" + capabilities.worldgenResourceCount()
                            + ", world_preset=" + capabilities.worldPresetResourceCount()
                            + ", world_startup_total=" + capabilities.worldStartupResourceCount()
                            + ". A product world cannot be treated as ready without all required startup resource lanes.",
                    null,
                    packId,
                    List.of("echo-native-platform/echo-native-loader/src/main/java/dev/echo/nativeplatform/loader/NativeLoaderResourceHost.java"),
                    "Mount at least one native datapack, worldgen declaration, and world_preset declaration before the product path can create a product-specific world instead of a vanilla-looking save."
            ));
        }
        if (!capabilities.productClientResourcesReady()) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-RELEASE-CLIENT-RESOURCES-MISSING",
                    EchoNativeIssueSeverity.WARNING,
                    "Release launch client resources are not mounted",
                    "Native resource host mounted " + capabilities.resourcePackResourceCount()
                            + " resource-pack declaration(s). Product menus, HUDs, overlays, item models, and loading screens need client resources on the native path.",
                    null,
                    packId,
                    List.of("echo-native-platform/echo-native-loader/src/main/java/dev/echo/nativeplatform/loader/NativeLoaderResourceHost.java"),
                    "Mount the product resource pack through the native resource host before claiming client UI/resource parity."
            ));
        }
        if (!capabilities.nativeLifecycleReady()) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-RELEASE-LIFECYCLE-INCOMPLETE",
                    EchoNativeIssueSeverity.WARNING,
                    "Release launch lifecycle host is not complete",
                    "Native lifecycle host counts: lifecycle_events=" + capabilities.lifecycleEventCount()
                            + ", failed_lifecycle_events=" + capabilities.failedLifecycleEventCount()
                            + ". Module phases must be recorded without lifecycle failures before release readiness.",
                    null,
                    packId,
                    List.of("echo-native-platform/echo-native-loader/src/main/java/dev/echo/nativeplatform/loader/NativeLoaderLifecycleEventHost.java"),
                    "Record every module lifecycle phase through NativeLoaderLifecycleEventHost and clear lifecycle failures before claiming lifecycle parity."
            ));
        }
        if (!capabilities.nativeEventsReady()) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-RELEASE-EVENTS-INCOMPLETE",
                    EchoNativeIssueSeverity.WARNING,
                    "Release launch event bridge has no executed native handlers",
                    "Native event host counts: subscriptions=" + capabilities.eventSubscriptionCount()
                            + ", published_events=" + capabilities.publishedEventCount()
                            + ", executed_handlers=" + capabilities.executedEventHandlerCount()
                            + ", lifecycle_event_live_release_proof="
                            + capabilities.lifecycleLiveRuntimeReleaseProofSatisfied()
                            + ". Descriptor-level event declarations and handler execution without live proof are not enough for release parity.",
                    null,
                    packId,
                    List.of("echo-native-platform/echo-native-loader/src/main/java/dev/echo/nativeplatform/loader/NativeLoaderLifecycleEventHost.java"),
                    "Subscribe declared hooks and dispatch native events through the product event host with live Minecraft mutation proof before claiming event/lifecycle parity."
            ));
        }
        if (!capabilities.nativeCommandHostReady()) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-RELEASE-COMMAND-HOST-INCOMPLETE",
                    EchoNativeIssueSeverity.WARNING,
                    "Release launch command host is not ready",
                    "Native command host counts: queued_commands=" + capabilities.queuedCommandCount()
                            + ", failures=" + capabilities.commandFailureCount()
                            + ". Commands must be routed into the native command host, not left as descriptor text.",
                    null,
                    packId,
                    List.of("echo-native-platform/echo-native-loader/src/main/java/dev/echo/nativeplatform/loader/NativeLoaderCommandHost.java"),
                    "Queue module-declared commands through NativeLoaderCommandHost and resolve command host failures before claiming command parity."
            ));
        }
        if (!capabilities.nativeConfigHostReady()) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-RELEASE-CONFIG-HOST-INCOMPLETE",
                    EchoNativeIssueSeverity.WARNING,
                    "Release launch config host has no registered configs",
                    "Native config host registered " + capabilities.registeredConfigCount()
                            + " config declaration(s). Public release needs native-owned client/server config surfaces.",
                    null,
                    packId,
                    List.of("echo-native-platform/echo-native-loader/src/main/java/dev/echo/nativeplatform/loader/NativeLoaderConfigHost.java"),
                    "Register product config schemas or config surfaces through NativeLoaderConfigHost before claiming config parity."
            ));
        }
        if (!capabilities.nativeNetworkHostReady()) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-RELEASE-NETWORK-HOST-INCOMPLETE",
                    EchoNativeIssueSeverity.WARNING,
                    "Release launch network host is not ready",
                    "Native network host counts: bound_packets=" + capabilities.boundNetworkPacketCount()
                            + ", failures=" + capabilities.networkFailureCount()
                            + ". Networking cannot be treated as Forge-parity until packets are bound through the native host.",
                    null,
                    packId,
                    List.of("echo-native-platform/echo-native-loader/src/main/java/dev/echo/nativeplatform/loader/NativeLoaderNetworkHost.java"),
                    "Bind product runtime packets through NativeLoaderNetworkHost and clear packet host failures before claiming networking parity."
            ));
        }
        if (!capabilities.adapterCoreMutationsReady()) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-RELEASE-ADAPTERCORE-MUTATIONS-MISSING",
                    EchoNativeIssueSeverity.WARNING,
                    "Release launch has no trusted AdapterCore runtime mutations",
                    "Native AdapterCore ledger counts: records=" + capabilities.adapterCoreMutationRecordCount()
                            + ", mutated_records=" + capabilities.adapterCoreMutatedRecordCount()
                            + ". AdapterCore host calls must commit native runtime mutations for release parity.",
                    null,
                    packId,
                    List.of("echo-native-platform/echo-native-loader/src/main/java/dev/echo/nativeplatform/loader/NativeLoaderAdapterCoreBackend.java"),
                    "Route inventory, world, events, HUD, packets, mission, and save operations through NativeLoaderAdapterCoreBackend before claiming runtime parity."
            ));
        }
        if (capabilities.fallbackMirrorMutationCount() > 0) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-RELEASE-FALLBACK-MIRROR-MUTATION",
                    EchoNativeIssueSeverity.WARNING,
                    "Release launch still contains fallback mirror runtime mutation",
                    "Native Loader runtime host recorded " + capabilities.fallbackMirrorMutationCount()
                            + " fallback mirror mutation(s). Public release proof requires live Minecraft runtime mutation, not fallback native mirror state.",
                    null,
                    packId,
                    List.of("echo-native-platform/echo-native-loader/src/main/java/dev/echo/nativeplatform/loader/NativeLoaderRuntimeHost.java"),
                    "Route every release-proof runtime mutation through an attached NativeLoaderLiveRuntimeBridge before claiming Agent 5 runtime parity."
            ));
        }
        if (!capabilities.saveDataHooksReady()) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-RELEASE-SAVE-HOOKS-MISSING",
                    EchoNativeIssueSeverity.WARNING,
                    "Release launch has no native save-data mutation evidence",
                    "Native AdapterCore live save-data proof count is " + capabilities.saveDataMutationCount()
                            + ". Public release needs save hooks to mutate through the live native runtime, not just describe save support or mirror save data.",
                    null,
                    packId,
                    List.of("echo-native-platform/echo-native-loader/src/main/java/dev/echo/nativeplatform/loader/NativeLoaderAdapterCoreBackend.java"),
                    "Commit at least one product save-data write or delete through the live Native Loader AdapterCore backend before claiming save-data parity."
            ));
        }
        if (capabilities.headlessClientSurface()
                || (!capabilities.liveClientAttached() && !capabilities.firstClassNativeClientRenderPipeline())) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-RELEASE-CLIENT-HEADLESS",
                    EchoNativeIssueSeverity.WARNING,
                    "Release launch has no trusted client render pipeline",
                    "The product launcher registered the client UI contract surface, but no live client render pipeline or first-class native client render pipeline is attached.",
                    null,
                    packId,
                    List.of("echo-native-platform/echo-native-loader/src/main/java/dev/echo/nativeplatform/loader/NativeLoaderClientUiHost.java"),
                    "Back NativeLoaderClientUiHost with either the native client window/render pipeline or a trusted first-class native client render pipeline before treating this as Forge-parity client runtime."
            ));
        } else if (capabilities.firstClassNativeClientRenderPipeline()
                && (!capabilities.nativeClientRenderProcess()
                || !capabilities.releaseClientRenderTrusted()
                || !capabilities.clientRenderMutationSupported())) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-RELEASE-CLIENT-RENDER-UNTRUSTED",
                    EchoNativeIssueSeverity.WARNING,
                    "Release launch native client render pipeline is not trusted release evidence",
                    "The product launcher has a first-class native client render pipeline, but it is missing the native process, release trust, or mutation support flags.",
                    null,
                    packId,
                    List.of("echo-native-platform/echo-native-loader/src/main/java/dev/echo/nativeplatform/loader/NativeLoaderLiveClientBridge.java"),
                    "Set nativeClientRenderProcess=true, releaseClientRenderTrusted=true, and clientRenderMutationSupported=true on first-party native client render bridges before treating HUD, loading, and screen hooks as release-grade."
            ));
        } else if (!capabilities.trustedClientRenderPipelineReady()) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-RELEASE-CLIENT-UNTRUSTED",
                    EchoNativeIssueSeverity.WARNING,
                    "Release launch client attachment is not trusted release evidence",
                    "The product launcher has a non-headless client attachment, but it is missing explicit live-client or first-class native-render release evidence.",
                    null,
                    packId,
                    List.of("echo-native-platform/echo-native-loader/src/main/java/dev/echo/nativeplatform/loader/NativeLoaderClientUiHost.java"),
                    "Attach the live client/render pipeline with releaseClientTrusted=true and realClientProcess=true, or attach a trusted first-class native client render pipeline."
            ));
        }
        if (!capabilities.liveClientBridgeAttached()) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-RELEASE-CLIENT-BRIDGE-MISSING",
                    EchoNativeIssueSeverity.WARNING,
                    "Release launch client attachment has no typed live bridge",
                    "UI surface registrations still have no attached"
                            + " NativeLoaderLiveClientBridge implementation.",
                    null,
                    packId,
                    List.of("echo-native-platform/echo-native-loader/src/main/java/dev/echo/nativeplatform/loader/NativeLoaderLiveClientBridge.java"),
                    "Attach a NativeLoaderLiveClientBridge that dispatches screen, HUD, and overlay registrations to the live client/render pipeline."
            ));
        }
        if (capabilities.liveClientBridgeAttached()
                && (!capabilities.nativeLoaderOwnsClientHostServices()
                || capabilities.neoForgeClientEventsCompatibilityAdaptersOnly())) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-RELEASE-CLIENT-NEOFORGE-OWNERSHIP",
                    EchoNativeIssueSeverity.WARNING,
                    "Release launch client host services are not Native Loader owned",
                    "The live client bridge is attached, but client tick, screen, input, HUD, and route services still report NeoForge compatibility-adapter ownership or missing Native Loader host-service authority.",
                    null,
                    packId,
                    List.of("echo-native-platform/echo-native-loader/src/main/java/dev/echo/nativeplatform/loader/NativeLoaderLiveClientBridge.java"),
                    "Use a NativeLoaderLiveClientBridge with nativeLoaderOwnsClientHostServices=true and neoForgeClientEventsCompatibilityAdaptersOnly=false before claiming Native Loader client runtime ownership."
            ));
        }
        if (capabilities.firstClassNativeClientRouteTable()
                && (!capabilities.nativeClientRouteProcess()
                || !capabilities.releaseClientRouteTrusted()
                || !capabilities.clientRouteMutationSupported())) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-RELEASE-CLIENT-ROUTE-TABLE-UNTRUSTED",
                    EchoNativeIssueSeverity.WARNING,
                    "Release launch native client route table is not trusted mutation evidence",
                    "The product launcher has a first-class native client route table, but it is missing the native process, release trust, or mutation support flags.",
                    null,
                    packId,
                    List.of("echo-native-platform/echo-native-loader/src/main/java/dev/echo/nativeplatform/loader/NativeLoaderLiveClientBridge.java"),
                    "Set nativeClientRouteProcess=true, releaseClientRouteTrusted=true, and clientRouteMutationSupported=true on first-party native client route bridges before treating route registration as mutation."
            ));
        }
        if (capabilities.clientSurfaceCount() > 0
                && capabilities.trustedClientRouteMutatedSurfaceCount() < capabilities.clientSurfaceCount()) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-RELEASE-CLIENT-SURFACES-NOT-MUTATED",
                    EchoNativeIssueSeverity.WARNING,
                    "Release launch client UI surfaces did not all mutate through a trusted client route",
                    "The client UI host recorded " + capabilities.clientSurfaceCount()
                            + " surface registration(s), but only "
                            + capabilities.trustedClientRouteMutatedSurfaceCount()
                            + " mutated through a trusted live client bridge or native client route table.",
                    null,
                    packId,
                    List.of("echo-native-platform/echo-native-loader/src/main/java/dev/echo/nativeplatform/loader/NativeLoaderClientUiHost.java"),
                    "Dispatch every module-declared screen, HUD, overlay, loading, and menu surface through a trusted client route before claiming the route table is complete."
            ));
        }
        return List.copyOf(diagnostics);
    }

    private static Map<String, Object> descriptorCapabilityEvidence(
            EchoNativeAddonDescriptor descriptor,
            String source,
            String capability
    ) {
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("source", "descriptor." + source);
        evidence.put("capability", capability);
        evidence.put("descriptorPath", descriptorPath(descriptor));
        evidence.put("moduleId", descriptor.id());
        evidence.put("liveMinecraftMutation", false);
        evidence.put("minecraftRuntimeAccessed", false);
        return Map.copyOf(evidence);
    }

    private static boolean usesInferredClasspath(EchoNativeAddonDescriptor descriptor) {
        Object nativeClasspath = descriptor.access() == null ? null : descriptor.access().get("nativeClasspath");
        if (!(nativeClasspath instanceof List<?> entries)) {
            return false;
        }
        return entries.stream()
                .anyMatch(entry -> EchoNativeModuleDescriptor.INFERRED_CLASSPATH_TOKEN.equals(String.valueOf(entry).trim()));
    }

    private static boolean isSourceTreeDescriptor(Path descriptorPath) {
        if (descriptorPath == null) {
            return false;
        }
        return descriptorPath.normalize().toString().replace('\\', '/')
                .contains("/src/main/resources/META-INF/echo.mod.json");
    }

    private static String descriptorPath(EchoNativeAddonDescriptor descriptor) {
        return descriptor.descriptorPath() == null ? "" : descriptor.descriptorPath().toString().replace('\\', '/');
    }

    private static String path(Path path) {
        return path == null ? "" : path.toString().replace('\\', '/');
    }

    private static boolean booleanValue(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof String text) {
            return Boolean.parseBoolean(text.trim());
        }
        return false;
    }

    private static boolean releaseClientTrusted(Map<String, Object> clientAssessment) {
        return booleanValue(clientAssessment.get("liveClientAttached"))
                && !booleanValue(clientAssessment.get("headlessClientSurface"))
                && booleanValue(clientAssessment.get("realClientProcess"))
                && booleanValue(clientAssessment.get("releaseClientTrusted"));
    }

    private static boolean hasBlocking(List<EchoNativeDiagnostic> diagnostics) {
        return diagnostics.stream()
                .anyMatch(diagnostic -> diagnostic.severity() == EchoNativeIssueSeverity.ERROR
                        || diagnostic.severity() == EchoNativeIssueSeverity.FATAL);
    }

    private record DescriptorResourceIntent(
            String resourceId,
            String resourceType,
            String sourcePath
    ) {
    }

    private record NativeHostServices(
            EchoNativeServiceRegistry serviceRegistry,
            EchoNativeRegistryHost registryHost,
            NativeLoaderLifecycleEventHost lifecycleEventHost,
            NativeLoaderCommandHost commandHost,
            NativeLoaderConfigHost configHost,
            NativeLoaderResourceHost resourceHost,
            NativeLoaderNetworkHost networkHost,
            NativeLoaderAdapterCoreBackend adapterCoreBackend,
            NativeLoaderRuntimeHost runtimeHost,
            NativeLoaderClientUiHost clientUiHost
    ) {
    }

    public record EchoNativeProductLaunchOptions(
            boolean requireMutation,
            boolean releaseMode,
            boolean requireLiveRuntime,
            NativeLoaderLiveRuntimeAttachment liveRuntimeAttachment,
            NativeLoaderLiveRuntimeBridge liveRuntimeBridge,
            NativeLoaderLiveRegistryBridge liveRegistryBridge,
            Map<String, Object> clientAttachmentAssessment,
            NativeLoaderLiveClientBridge liveClientBridge,
            EchoNativeProductHookPlan hookPlan
    ) {
        public EchoNativeProductLaunchOptions {
            requireMutation = requireMutation || releaseMode;
            liveRuntimeAttachment = liveRuntimeAttachment == null
                    ? NativeLoaderLiveRuntimeAttachment.unattached()
                    : liveRuntimeAttachment;
            liveRuntimeBridge = liveRuntimeBridge == null
                    ? NativeLoaderLiveRuntimeBridge.UNATTACHED
                    : liveRuntimeBridge;
            liveRegistryBridge = liveRegistryBridge == null
                    ? NativeLoaderLiveRegistryBridge.UNATTACHED
                    : liveRegistryBridge;
            clientAttachmentAssessment = clientAttachmentAssessment == null
                    ? headlessClientAssessment()
                    : Map.copyOf(clientAttachmentAssessment);
            liveClientBridge = liveClientBridge == null
                    ? NativeLoaderLiveClientBridge.UNATTACHED
                    : liveClientBridge;
            hookPlan = hookPlan == null ? EchoNativeProductHookPlan.empty() : hookPlan;
        }

        public EchoNativeProductLaunchOptions(
                boolean requireMutation,
                boolean releaseMode,
                boolean requireLiveRuntime,
                NativeLoaderLiveRuntimeAttachment liveRuntimeAttachment,
                NativeLoaderLiveRuntimeBridge liveRuntimeBridge,
                Map<String, Object> clientAttachmentAssessment,
                NativeLoaderLiveClientBridge liveClientBridge,
                EchoNativeProductHookPlan hookPlan
        ) {
            this(
                    requireMutation,
                    releaseMode,
                    requireLiveRuntime,
                    liveRuntimeAttachment,
                    liveRuntimeBridge,
                    NativeLoaderLiveRegistryBridge.UNATTACHED,
                    clientAttachmentAssessment,
                    liveClientBridge,
                    hookPlan
            );
        }

        public EchoNativeProductLaunchOptions(
                boolean requireMutation,
                boolean releaseMode,
                boolean requireLiveRuntime,
                NativeLoaderLiveRuntimeAttachment liveRuntimeAttachment,
                NativeLoaderLiveRuntimeBridge liveRuntimeBridge,
                Map<String, Object> clientAttachmentAssessment,
                EchoNativeProductHookPlan hookPlan
        ) {
            this(
                    requireMutation,
                    releaseMode,
                    requireLiveRuntime,
                    liveRuntimeAttachment,
                    liveRuntimeBridge,
                    NativeLoaderLiveRegistryBridge.UNATTACHED,
                    clientAttachmentAssessment,
                    NativeLoaderLiveClientBridge.UNATTACHED,
                    hookPlan
            );
        }

        public EchoNativeProductLaunchOptions(
                boolean requireMutation,
                boolean releaseMode,
                boolean requireLiveRuntime,
                NativeLoaderLiveRuntimeAttachment liveRuntimeAttachment,
                Map<String, Object> clientAttachmentAssessment
        ) {
            this(
                    requireMutation,
                    releaseMode,
                    requireLiveRuntime,
                    liveRuntimeAttachment,
                    NativeLoaderLiveRuntimeBridge.UNATTACHED,
                    NativeLoaderLiveRegistryBridge.UNATTACHED,
                    clientAttachmentAssessment,
                    NativeLoaderLiveClientBridge.UNATTACHED,
                    EchoNativeProductHookPlan.empty()
            );
        }

        public EchoNativeProductLaunchOptions(
                boolean requireMutation,
                boolean releaseMode,
                boolean requireLiveRuntime,
                NativeLoaderLiveRuntimeAttachment liveRuntimeAttachment,
                Map<String, Object> clientAttachmentAssessment,
                EchoNativeProductHookPlan hookPlan
        ) {
            this(
                    requireMutation,
                    releaseMode,
                    requireLiveRuntime,
                    liveRuntimeAttachment,
                    NativeLoaderLiveRuntimeBridge.UNATTACHED,
                    NativeLoaderLiveRegistryBridge.UNATTACHED,
                    clientAttachmentAssessment,
                    NativeLoaderLiveClientBridge.UNATTACHED,
                    hookPlan
            );
        }

        public EchoNativeProductLaunchOptions(boolean requireMutation, boolean releaseMode, boolean requireLiveRuntime) {
            this(
                    requireMutation,
                    releaseMode,
                    requireLiveRuntime,
                    NativeLoaderLiveRuntimeAttachment.unattached(),
                    NativeLoaderLiveRuntimeBridge.UNATTACHED,
                    NativeLoaderLiveRegistryBridge.UNATTACHED,
                    headlessClientAssessment(),
                    NativeLoaderLiveClientBridge.UNATTACHED,
                    EchoNativeProductHookPlan.empty()
            );
        }

        public EchoNativeProductLaunchOptions(boolean requireMutation, boolean releaseMode) {
            this(requireMutation, releaseMode, false);
        }
    }

    public record EchoNativeProductLaunchOutcome(
            String packId,
            int totalModules,
            int loadedModules,
            int registeredModules,
            int mutatedModules,
            int failedModules,
            boolean requireMutation,
            boolean releaseMode,
            boolean requireLiveRuntime,
            boolean accepted,
            EchoNativeProductBootstrapProfileReport bootstrapProfile,
            EchoNativeProductPreWindowAssertionReport preWindowAssertions,
            EchoNativeProductRuntimeCapabilityReport runtimeCapabilities,
            EchoNativeProductHookReport hookReport,
            List<EchoNativeProductModuleLaunch> modules,
            List<EchoNativeDiagnostic> diagnostics
    ) {
    }

    public record EchoNativeProductBootstrapProfileReport(
            String packId,
            String rootModuleId,
            String bootstrapProfileClass,
            String descriptorId,
            String descriptorPath,
            boolean profileDriven
    ) {
        public static EchoNativeProductBootstrapProfileReport unavailable(String packId) {
            return new EchoNativeProductBootstrapProfileReport(
                    packId == null ? "" : packId,
                    "",
                    "",
                    "",
                    "",
                    false
            );
        }
    }

    public record EchoNativeProductPreWindowAssertionReport(
            long nativeEntrypointModuleCount,
            long classpathReadyModuleCount,
            boolean nativeModuleClasspathReady,
            List<String> requiredServiceIds,
            List<String> registeredServiceIds,
            List<String> missingServiceIds,
            boolean serviceProvidersReady,
            boolean productProfileReady,
            boolean ashfallProfileReady,
            int routeRegistryCount,
            boolean routeRegistryReady,
            int mountedResourceCount,
            int dataPackResourceCount,
            int worldgenResourceCount,
            int worldPresetResourceCount,
            int resourcePackResourceCount,
            boolean resourceHostReady,
            boolean ashfallDatapackWorldPresetReady,
            List<String> serviceProviderImplementations,
            String blessedLaunchTask,
            String internalHandoffTask,
            String bootstrapMainClass,
            String realMainClass,
            boolean bootstrapHandoffAuthorityReady,
            boolean moduleReleaseReady,
            boolean productWindowReady
    ) {
        public static EchoNativeProductPreWindowAssertionReport unavailable() {
            return new EchoNativeProductPreWindowAssertionReport(
                    0,
                    0,
                    false,
                    requiredNativeServiceIds(),
                    List.of(),
                    requiredNativeServiceIds(),
                    false,
                    false,
                    false,
                    0,
                    false,
                    0,
                    0,
                    0,
                    0,
                    0,
                    false,
                    false,
                    List.of(),
                    "startNativeClient",
                    "runNativeBootstrapClient",
                    "dev.echo.nativeplatform.bootstrap.EchoNativeBootstrapMain",
                    "net.minecraft.client.main.Main",
                    false,
                    false,
                    false
            );
        }
    }

    public record EchoNativeProductRuntimeCapabilityReport(
            boolean runtimeHostRegistered,
            boolean firstClassNativeRuntime,
            boolean delegateRequired,
            boolean liveMinecraftAttached,
            boolean liveRuntimeTrusted,
            boolean liveRuntimeBridgeAttached,
            boolean liveRegistryBridgeAttached,
            boolean firstClassNativeRegistry,
            boolean releaseRegistryTrusted,
            int registryEntryCount,
            int liveRegistryBridgeMutatedEntryCount,
            int nativeRegistryHostMutatedEntryCount,
            int trustedRegistryMutatedEntryCount,
            List<String> registeredOnlyFirstClassRegistryKinds,
            List<String> registeredOnlyFirstClassRegistryIds,
            Map<String, List<String>> registeredOnlyFirstClassRegistryIdsByKind,
            List<String> failedFirstClassRegistryKinds,
            List<String> failedFirstClassRegistryIds,
            Map<String, List<String>> failedFirstClassRegistryIdsByKind,
            List<String> untrustedMutationFirstClassRegistryKinds,
            Map<String, Integer> untrustedMutationReasonCounts,
            Map<String, Map<String, Integer>> untrustedMutationReasonCountsByKind,
            Map<String, Object> registryBridgeMutationReconciliation,
            boolean savesDirectoryConfigured,
            int fallbackMirrorMutationCount,
            int mountedResourceCount,
            int worldStartupResourceCount,
            int worldgenResourceCount,
            int worldPresetResourceCount,
            int dataPackResourceCount,
            int resourcePackResourceCount,
            int structureResourceCount,
            int tagResourceCount,
            int lifecycleEventCount,
            int failedLifecycleEventCount,
            int publishedEventCount,
            int eventSubscriptionCount,
            int executedEventHandlerCount,
            int lifecycleLiveRuntimeMutationCount,
            boolean lifecycleMinecraftRuntimeAccessed,
            boolean lifecycleLiveRuntimeReleaseProofSatisfied,
            int queuedCommandCount,
            int commandFailureCount,
            int commandLiveRuntimeMutationCount,
            boolean commandMinecraftRuntimeAccessed,
            boolean commandLiveRuntimeReleaseProofSatisfied,
            int registeredConfigCount,
            int configLiveRuntimeMutationCount,
            boolean configMinecraftRuntimeAccessed,
            boolean configLiveRuntimeReleaseProofSatisfied,
            int boundNetworkPacketCount,
            int networkFailureCount,
            int networkLiveRuntimeMutationCount,
            boolean networkMinecraftRuntimeAccessed,
            boolean networkLiveRuntimeReleaseProofSatisfied,
            int adapterCoreMutationRecordCount,
            int adapterCoreMutatedRecordCount,
            int adapterCoreLiveRuntimeProofRecordCount,
            List<String> adapterCoreLiveRuntimeProofSurfaces,
            int saveDataMutationCount,
            List<String> supportedRuntimeSurfaces,
            List<String> mutatedRuntimeSurfaces,
            boolean clientUiHostAttached,
            boolean liveClientBridgeAttached,
            boolean nativeLoaderOwnsClientHostServices,
            boolean neoForgeClientEventsCompatibilityAdaptersOnly,
            boolean liveClientAttached,
            boolean liveClientTrusted,
            boolean headlessClientSurface,
            boolean firstClassNativeClientRouteTable,
            boolean nativeClientRouteProcess,
            boolean releaseClientRouteTrusted,
            boolean clientRouteMutationSupported,
            boolean firstClassNativeClientRenderPipeline,
            boolean nativeClientRenderProcess,
            boolean releaseClientRenderTrusted,
            boolean clientRenderMutationSupported,
            int clientSurfaceCount,
            int liveClientBridgeAcceptedSurfaceCount,
            int liveClientBridgeMutatedSurfaceCount,
            int nativeClientRouteTableMutatedSurfaceCount,
            int trustedClientRouteMutatedSurfaceCount
    ) {
        public static EchoNativeProductRuntimeCapabilityReport unavailable() {
            return new EchoNativeProductRuntimeCapabilityReport(
                    false, false, true, false, false, false, false, false, false,
                    0, 0, 0, 0,
                    List.of(), List.of(), Map.of(), List.of(), List.of(), Map.of(), List.of(), Map.of(), Map.of(), Map.of(),
                    false, 0,
                    0, 0, 0, 0, 0, 0, 0, 0,
                    0, 0, 0, 0, 0, 0, false, false,
                    0, 0, 0, false, false,
                    0, 0, false, false,
                    0, 0, 0, false, false,
                    0, 0, 0, List.of(), 0,
                    List.of(), List.of(),
                    false, false, false, true, false, false, true,
                    false, false, false, false,
                    false, false, false, false,
                    0, 0, 0, 0, 0
            );
        }

        public boolean fullReleaseRuntimeReady() {
            return runtimeHostRegistered
                    && firstClassNativeRuntime
                    && !delegateRequired
                    && liveRuntimeTrusted
                    && nativeRuntimeDispatchReady()
                    && agent5LiveRuntimeSurfaceProofReady()
                    && liveRegistryBridgeAttached
                    && registryBlockersClear()
                    && registryBridgeMutationEvidenceReconciled()
                    && (registryEntryCount == 0 || trustedRegistryMutatedEntryCount == registryEntryCount)
                    && nativeLifecycleReady()
                    && nativeEventsReady()
                    && nativeCommandHostReady()
                    && nativeConfigHostReady()
                    && nativeNetworkHostReady()
                    && adapterCoreMutationsReady()
                    && saveDataHooksReady()
                    && productResourcesReady()
                    && clientUiHostAttached
                    && liveClientBridgeAttached
                    && nativeLoaderOwnsClientHostServices
                    && !neoForgeClientEventsCompatibilityAdaptersOnly
                    && !headlessClientSurface
                    && trustedClientRenderPipelineReady()
                    && (clientSurfaceCount == 0 || trustedClientRouteMutatedSurfaceCount == clientSurfaceCount);
        }

        public boolean registryBlockersClear() {
            return registeredOnlyFirstClassRegistryKinds.isEmpty()
                    && registeredOnlyFirstClassRegistryIds.isEmpty()
                    && registeredOnlyFirstClassRegistryIdsByKind.isEmpty()
                    && failedFirstClassRegistryKinds.isEmpty()
                    && failedFirstClassRegistryIds.isEmpty()
                    && failedFirstClassRegistryIdsByKind.isEmpty()
                    && untrustedMutationFirstClassRegistryKinds.isEmpty()
                    && untrustedMutationReasonCounts.isEmpty()
                    && untrustedMutationReasonCountsByKind.isEmpty();
        }

        public boolean registryBridgeMutationEvidenceReconciled() {
            Object reconciled = registryBridgeMutationReconciliation.get("bridgeEvidenceMatchesTrustedEntries");
            return Boolean.TRUE.equals(reconciled);
        }

        public List<String> registryBridgeMutationReconciliationList(String key) {
            Object value = registryBridgeMutationReconciliation.get(key);
            if (!(value instanceof Iterable<?> iterable)) {
                return List.of();
            }
            List<String> items = new ArrayList<>();
            for (Object item : iterable) {
                String text = String.valueOf(item).trim();
                if (!text.isBlank()) {
                    items.add(text);
                }
            }
            return List.copyOf(items);
        }

        public boolean nativeRuntimeDispatchReady() {
            return liveRuntimeBridgeAttached;
        }

        public boolean agent5LiveRuntimeSurfaceProofReady() {
            return liveRuntimeBridgeAttached
                    && fallbackMirrorMutationCount == 0
                    && lifecycleLiveRuntimeMutationCount > 0
                    && lifecycleMinecraftRuntimeAccessed
                    && lifecycleLiveRuntimeReleaseProofSatisfied
                    && commandLiveRuntimeMutationCount > 0
                    && commandMinecraftRuntimeAccessed
                    && commandLiveRuntimeReleaseProofSatisfied
                    && configLiveRuntimeMutationCount > 0
                    && configMinecraftRuntimeAccessed
                    && configLiveRuntimeReleaseProofSatisfied
                    && networkLiveRuntimeMutationCount > 0
                    && networkMinecraftRuntimeAccessed
                    && networkLiveRuntimeReleaseProofSatisfied
                    && adapterCoreLiveRuntimeProofRecordCount > 0
                    && adapterCoreLiveRuntimeProofSurfaces.containsAll(requiredAgent5AdapterCoreLiveProofSurfaces());
        }

        public static List<String> requiredAgent5AdapterCoreLiveProofSurfaces() {
            return List.of(
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
                    "save_data",
                    "hud",
                    "client_tick",
                    "render_layers",
                    "screen_events",
                    "keybinds",
                    "resource_reloads",
                    "save_hooks",
                    "server_client_sync",
                    "commands",
                    "network_channels",
                    "config_reloads",
                    "lifecycle_phases"
            );
        }

        public boolean productResourcesReady() {
            return mountedResourceCount > 0
                    && productWorldStartupResourcesReady()
                    && productClientResourcesReady();
        }

        public boolean productWorldStartupResourcesReady() {
            return worldStartupResourceCount > 0
                    && dataPackResourceCount > 0
                    && worldgenResourceCount > 0
                    && worldPresetResourceCount > 0;
        }

        public boolean productClientResourcesReady() {
            return resourcePackResourceCount > 0;
        }

        public boolean nativeLifecycleReady() {
            return lifecycleEventCount > 0
                    && failedLifecycleEventCount == 0
                    && lifecycleLiveRuntimeReleaseProofSatisfied;
        }

        public boolean nativeEventsReady() {
            return eventSubscriptionCount > 0
                    && publishedEventCount > 0
                    && executedEventHandlerCount > 0
                    && lifecycleLiveRuntimeReleaseProofSatisfied;
        }

        public boolean nativeCommandHostReady() {
            return queuedCommandCount > 0
                    && commandFailureCount == 0
                    && commandLiveRuntimeReleaseProofSatisfied;
        }

        public boolean nativeConfigHostReady() {
            return registeredConfigCount > 0
                    && configLiveRuntimeReleaseProofSatisfied;
        }

        public boolean nativeNetworkHostReady() {
            return boundNetworkPacketCount > 0
                    && networkFailureCount == 0
                    && networkLiveRuntimeReleaseProofSatisfied;
        }

        public boolean adapterCoreMutationsReady() {
            return adapterCoreMutationRecordCount > 0
                    && adapterCoreMutatedRecordCount > 0
                    && adapterCoreLiveRuntimeProofRecordCount > 0;
        }

        public boolean saveDataHooksReady() {
            return saveDataMutationCount > 0;
        }

        public boolean trustedClientRenderPipelineReady() {
            return liveClientTrusted
                    || (firstClassNativeClientRenderPipeline
                    && nativeClientRenderProcess
                    && releaseClientRenderTrusted
                    && clientRenderMutationSupported);
        }
    }

    public record EchoNativeProductModuleLaunch(
            String moduleId,
            EchoNativeLoadStatus claimedStatus,
            EchoNativeLoadStatus honestStatus,
            boolean loaded,
            boolean registered,
            boolean mutated,
            boolean accepted,
            List<String> diagnostics,
            List<String> failures
    ) {
    }

    public record EchoNativeProductHookReport(
            int executionCount,
            int mutatedExecutionCount,
            int publishedEventHandlerExecutionCount,
            List<String> runtimeMutatedSurfaces,
            List<EchoNativeProductHookExecution> executions,
            Map<String, Object> registryHost,
            Map<String, Object> lifecycleEventHost,
            Map<String, Object> commandHost,
            Map<String, Object> networkHost,
            Map<String, Object> resourceHost,
            Map<String, Object> configHost,
            Map<String, Object> clientUiHost,
            List<Map<String, Object>> mutationLedger
    ) {
        public static EchoNativeProductHookReport empty() {
            return new EchoNativeProductHookReport(
                    0,
                    0,
                    0,
                    List.of(),
                    List.of(),
                    Map.of(),
                    Map.of(),
                    Map.of(),
                    Map.of(),
                    Map.of(),
                    Map.of(),
                    Map.of(),
                    List.of()
            );
        }
    }

    public record EchoNativeProductHookExecution(
            String surface,
            String moduleId,
            String hookId,
            EchoNativeLoadStatus status,
            Map<String, Object> evidence
    ) {
        public EchoNativeProductHookExecution {
            evidence = evidence == null ? Map.of() : Map.copyOf(evidence);
        }
    }
}
