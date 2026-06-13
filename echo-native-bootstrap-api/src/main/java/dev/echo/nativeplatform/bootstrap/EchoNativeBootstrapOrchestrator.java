package dev.echo.nativeplatform.bootstrap;

import dev.echo.nativeplatform.loader.NativeLoaderRuntimeHostSupport;
import dev.echo.nativeplatform.loader.NativeLoaderRuntimeHostFlow;
import dev.echo.nativeplatform.loader.NativeLoaderAdapterCoreFlow;
import dev.echo.nativeplatform.loader.NativeLoaderAdapterCoreProbe;
import dev.echo.nativeplatform.loader.NativeLoaderAdapterCoreRuntimeMutations;
import dev.echo.nativeplatform.loader.NativeLoaderAdapterCoreMachineRuntimeActions;
import dev.echo.nativeplatform.loader.NativeLoaderAdapterCoreScannerRuntimeActions;
import dev.echo.nativeplatform.loader.NativeLoaderAdapterCoreGameplayRuntimeActions;
import dev.echo.nativeplatform.loader.NativeLoaderInventoryActionSupport;
import dev.echo.nativeplatform.loader.NativeLoaderInventoryMutationSupport;
import dev.echo.nativeplatform.loader.NativeLoaderEnvironmentFlow;
import dev.echo.nativeplatform.loader.NativeLoaderModuleSurfaceFlow;
import dev.echo.nativeplatform.loader.NativeLoaderResourcePackFlow;
import dev.echo.nativeplatform.loader.NativeLoaderProductGameplayFlow;
import dev.echo.nativeplatform.loader.NativeLoaderBridgeFlow;
import dev.echo.nativeplatform.loader.NativeLoaderContentConstructor;
import dev.echo.nativeplatform.loader.NativeLoaderContentActionFlow;
import dev.echo.nativeplatform.loader.NativeLoaderContentFallbackResolver;
import dev.echo.nativeplatform.loader.NativeLoaderClientReflectionSupport;
import dev.echo.nativeplatform.loader.NativeLoaderWorldBlockSetter;
import dev.echo.nativeplatform.loader.NativeLoaderJsonSupport;
import dev.echo.nativeplatform.contracts.EchoNativeBootstrapProductProfile;
import dev.echo.nativeplatform.contracts.EchoNativeBootstrapProductProfile.NativePhysicalActionRoute;
import dev.echo.nativeplatform.contracts.EchoNativeBootstrapProductProfile.NativeUiActionRoute;
import dev.echo.nativeplatform.contracts.EchoNativeBootstrapProductProfile.NativeUiSurfaceRoute;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Windowed Native Loader bootstrap handoff and live-client bridge surface.
 *
 * <p>This class is not product launch authority. Product validation, module
 * loading, package readiness, and pre-window release gates live in
 * echo-native-product-launcher and echo-native-loader. Bootstrap keeps the
 * marker/handoff plus the in-client bridge delegates needed after Minecraft is
 * already running.</p>
 */
final class EchoNativeBootstrapOrchestrator {
    public static final String MAIN_CLASS = "dev.echo.nativeplatform.bootstrap.EchoNativeBootstrapMain";
    private static final String NATIVE_LOADER_PROPERTY = "echo.native.loader";
    private static final String NATIVE_BOOTSTRAP_PROFILE_CLASS_PROPERTY = "echo.native.bootstrap.profileClass";
    private static final String NATIVE_LOADER_MAIN_LABEL_PROPERTY = "echo.native.loader.label";
    private static final String NATIVE_LOADER_CLIENT_LABEL_PROPERTY = "echo.native.loader.clientLabel";
    private static final String NATIVE_LOADER_WINDOW_TITLE_PROPERTY = "echo.native.loader.windowTitle";
    private static final String NATIVE_MODULE_CLASSPATH_PROPERTY = "echo.native.moduleClasspath";
    private static final String NATIVE_GAME_DIR_PROPERTY = "echo.native.gameDir";
    private static final String NATIVE_SERVICE_REGISTRY_PATH_PROPERTY = "echo.native.serviceRegistryPath";
    private static final String NATIVE_RUNTIME_HOST_ID_PROPERTY = "echo.native.runtime.host.id";
    private static final String NATIVE_RUNTIME_HOST_ID_ENV = "ECHO_NATIVE_RUNTIME_HOST_ID";
    private static final String NATIVE_RUNTIME_MODE_PROPERTY = "echo.native.runtime.mode";
    private static final String NATIVE_RUNTIME_MODE_ENV = "ECHO_NATIVE_RUNTIME_MODE";
    private static final String AGENT7_LIVE_HOOK_SNAPSHOT_FORCE_PROPERTY = "echo.agent7.liveHookSnapshot.force";
    private static final String AGENT7_LIVE_HOOK_SNAPSHOT_MAX_POLLS_PROPERTY = "echo.agent7.liveHookSnapshotMaxPolls";
    private static final String AGENT7_LIVE_HOOK_SNAPSHOT_POLL_MILLIS_PROPERTY = "echo.agent7.liveHookSnapshotPollMillis";
    private static final String AGENT7_LIVE_HOOK_DIRECT_EVIDENCE_PATH_PROPERTY = "echo.agent7.liveHookEvidencePath";
    private static final EchoNativeBootstrapMinecraftRuntimeFlow MINECRAFT_RUNTIME_FLOW =
            new EchoNativeBootstrapMinecraftRuntimeFlow(NATIVE_MODULE_CLASSPATH_PROPERTY, EchoNativeBootstrapMain.class);
    private static final EchoNativeBootstrapProductProfile BOOTSTRAP_PROFILE = bootstrapProfile();
    private static final EchoNativeProductProfileCore PRODUCT_PROFILE =
            new EchoNativeProductProfileCore(BOOTSTRAP_PROFILE);
    private static final NativeLoaderRuntimeHostSupport.Context RUNTIME_HOST_CONTEXT =
            new NativeLoaderRuntimeHostSupport.Context(
                    BOOTSTRAP_PROFILE,
                    MINECRAFT_RUNTIME_FLOW::runtimeClass,
                    EchoNativeBootstrapOrchestrator::nativeMinecraftInstance,
                    EchoNativeBootstrapMain::nativeClientModuleClassLoader,
                    NATIVE_RUNTIME_HOST_ID_PROPERTY,
                    NATIVE_RUNTIME_HOST_ID_ENV,
                    NATIVE_RUNTIME_MODE_PROPERTY,
                    NATIVE_RUNTIME_MODE_ENV
            );
    private static final NativeLoaderAdapterCoreRuntimeMutations.Context ADAPTER_CORE_RUNTIME_MUTATION_CONTEXT =
            new NativeLoaderAdapterCoreRuntimeMutations.Context(
                    RUNTIME_HOST_CONTEXT,
                    EchoNativeBootstrapOrchestrator::resolveNativeBetaItemId,
                    EchoNativeBootstrapOrchestrator::resolveNativeBetaBlockId,
                    EchoNativeBootstrapOrchestrator::nativeBetaBlockFallback,
                    PRODUCT_PROFILE::hudActionKey,
                    MINECRAFT_RUNTIME_FLOW::intMethod
            );
    private static final NativeLoaderAdapterCoreMachineRuntimeActions.Context ADAPTER_CORE_MACHINE_ACTION_CONTEXT =
            new NativeLoaderAdapterCoreMachineRuntimeActions.Context(
                    RUNTIME_HOST_CONTEXT,
                    MINECRAFT_RUNTIME_FLOW::runtimeClass,
                    EchoNativeBootstrapOrchestrator::nativeProductGameplayClass
            );
    private static final NativeLoaderAdapterCoreScannerRuntimeActions.Context ADAPTER_CORE_SCANNER_ACTION_CONTEXT =
            new NativeLoaderAdapterCoreScannerRuntimeActions.Context(
                    RUNTIME_HOST_CONTEXT,
                    MINECRAFT_RUNTIME_FLOW::runtimeClass,
                    EchoNativeBootstrapOrchestrator::nativeProductGameplayClass
            );
    private static final NativeLoaderAdapterCoreGameplayRuntimeActions.Context ADAPTER_CORE_GAMEPLAY_ACTION_CONTEXT =
            new NativeLoaderAdapterCoreGameplayRuntimeActions.Context(
                    RUNTIME_HOST_CONTEXT,
                    MINECRAFT_RUNTIME_FLOW::runtimeClass,
                    EchoNativeBootstrapOrchestrator::nativeProductGameplayClass,
                    EchoNativeBootstrapOrchestrator::nativeProductId,
                    EchoNativeBootstrapOrchestrator::resolveNativeBetaItemId,
                    MINECRAFT_RUNTIME_FLOW::registryValue,
                    EchoNativeBootstrapOrchestrator::heldItemStack
            );
    private static final NativeLoaderRuntimeHostFlow RUNTIME_HOST_FLOW =
            new NativeLoaderRuntimeHostFlow(
                    RUNTIME_HOST_CONTEXT,
                    ADAPTER_CORE_RUNTIME_MUTATION_CONTEXT,
                    ADAPTER_CORE_GAMEPLAY_ACTION_CONTEXT,
                    EchoNativeBootstrapOrchestrator::resolveNativeBetaItemId
            );
    private static final NativeLoaderAdapterCoreFlow ADAPTER_CORE_FLOW =
            new NativeLoaderAdapterCoreFlow(
                    ADAPTER_CORE_RUNTIME_MUTATION_CONTEXT,
                    ADAPTER_CORE_MACHINE_ACTION_CONTEXT,
                    ADAPTER_CORE_SCANNER_ACTION_CONTEXT,
                    ADAPTER_CORE_GAMEPLAY_ACTION_CONTEXT,
                    EchoNativeBootstrapOrchestrator::isCreativePlayer
            );
    private static final EchoNativeBootstrapUiClientFlow UI_CLIENT_FLOW =
            new EchoNativeBootstrapUiClientFlow(BOOTSTRAP_PROFILE, PRODUCT_PROFILE, uiClientFlowContext());
    private static final String NATIVE_PRODUCT_GAMEPLAY_BRIDGE_KEY =
            BOOTSTRAP_PROFILE.nativeGameplayBridgeKey();
    private static final String NATIVE_PRODUCT_PLAYABLE_RUNTIME_KEY =
            BOOTSTRAP_PROFILE.nativePlayableRuntimeKey();
    private static final NativeLoaderContentFallbackResolver CONTENT_FALLBACK =
            new NativeLoaderContentFallbackResolver(
                    BOOTSTRAP_PROFILE,
                    NATIVE_MODULE_CLASSPATH_PROPERTY,
                    MINECRAFT_RUNTIME_FLOW::runtimeClass,
                    PRODUCT_PROFILE::actionKey,
                    PRODUCT_PROFILE::blockFallback
            );
    private static final NativeLoaderBridgeFlow BRIDGE_FLOW =
            new NativeLoaderBridgeFlow(
                    BOOTSTRAP_PROFILE,
                    NATIVE_MODULE_CLASSPATH_PROPERTY,
                    NATIVE_GAME_DIR_PROPERTY,
                    EchoNativeBootstrapMain.class,
                    MINECRAFT_RUNTIME_FLOW::ensureVanillaBootstrap,
                    MINECRAFT_RUNTIME_FLOW::runtimeClass,
                    EchoNativeBootstrapOrchestrator::discoverEchoItemIds,
                    EchoNativeBootstrapOrchestrator::discoverEchoBlockIds,
                    EchoNativeBootstrapOrchestrator::newNativeBetaItem,
                    EchoNativeBootstrapOrchestrator::newNativeBetaBlock
            );
    private static final NativeLoaderContentConstructor.Context CONTENT_CONSTRUCTOR_CONTEXT =
            new NativeLoaderContentConstructor.Context(
                    BOOTSTRAP_PROFILE,
                    CONTENT_FALLBACK,
                    MINECRAFT_RUNTIME_FLOW::runtimeClass,
                    BRIDGE_FLOW::generatedContentBridgeConfig
            );
    private static final NativeLoaderWorldBlockSetter.Context WORLD_BLOCK_SETTER_CONTEXT =
            new NativeLoaderWorldBlockSetter.Context(
                    EchoNativeBootstrapOrchestrator::resolveNativeBetaBlockId,
                    EchoNativeBootstrapOrchestrator::nativeBetaBlockFallback,
                    MINECRAFT_RUNTIME_FLOW::registryValue,
                    MINECRAFT_RUNTIME_FLOW::runtimeClass,
                    MINECRAFT_RUNTIME_FLOW::intMethod
            );
    private static final NativeLoaderInventoryActionSupport.Context INVENTORY_ACTION_CONTEXT =
            new NativeLoaderInventoryActionSupport.Context(
                    EchoNativeBootstrapOrchestrator::resolveNativeBetaItemId,
                    CONTENT_FALLBACK::discoverContentId,
                    EchoNativeBootstrapOrchestrator::echoItemIds,
                    EchoNativeBootstrapOrchestrator::giveNativeBetaItem,
                    NativeLoaderClientReflectionSupport::optionalFieldValue,
                    NativeLoaderClientReflectionSupport::optionalMethodValue,
                    MINECRAFT_RUNTIME_FLOW::runtimeClass
            );
    private static final NativeLoaderInventoryMutationSupport.Context INVENTORY_MUTATION_CONTEXT =
            new NativeLoaderInventoryMutationSupport.Context(
                    MINECRAFT_RUNTIME_FLOW::runtimeClass,
                    NativeLoaderClientReflectionSupport::optionalMethodValue,
                    NativeLoaderClientReflectionSupport::optionalFieldValue,
                    MINECRAFT_RUNTIME_FLOW::registryValue,
                    MINECRAFT_RUNTIME_FLOW::intMethod
            );
    private static final NativeLoaderContentActionFlow CONTENT_ACTION_FLOW =
            new NativeLoaderContentActionFlow(
                    CONTENT_FALLBACK,
                    INVENTORY_ACTION_CONTEXT,
                    INVENTORY_MUTATION_CONTEXT,
                    WORLD_BLOCK_SETTER_CONTEXT,
                    EchoNativeBootstrapOrchestrator::giveNativeBetaItem
            );
    private static final NativeLoaderModuleSurfaceFlow.Context MODULE_SURFACE_CONTEXT =
            new NativeLoaderModuleSurfaceFlow.Context(
                    NATIVE_GAME_DIR_PROPERTY,
                    EchoNativeBootstrapOrchestrator::isActiveProductNamespace,
                    PRODUCT_PROFILE::isMachinePath,
                    EchoNativeBootstrapOrchestrator::blockPosText,
                    MINECRAFT_RUNTIME_FLOW::isClientSideLevel,
                    NativeLoaderClientReflectionSupport::optionalMethodValue,
                    MINECRAFT_RUNTIME_FLOW::intMethod,
                    NativeLoaderJsonSupport::writeAtomically,
                    EchoNativeLiveUiBridge::openRealModuleSurfaceFromGameplay
            );
    private static final EchoNativeBootstrapModuleRuntimeFlow MODULE_RUNTIME_FLOW =
            new EchoNativeBootstrapModuleRuntimeFlow(BOOTSTRAP_PROFILE, PRODUCT_PROFILE, moduleRuntimeFlowContext());
    private static final EchoNativeBootstrapProductActionFlow PRODUCT_ACTION_FLOW =
            new EchoNativeBootstrapProductActionFlow(BOOTSTRAP_PROFILE, PRODUCT_PROFILE, productActionFlowContext());
    private static final NativeLoaderEnvironmentFlow LOADER_ENVIRONMENT_FLOW =
            new NativeLoaderEnvironmentFlow(
                    NATIVE_LOADER_PROPERTY,
                    NATIVE_LOADER_MAIN_LABEL_PROPERTY,
                    NATIVE_LOADER_CLIENT_LABEL_PROPERTY,
                    NATIVE_LOADER_WINDOW_TITLE_PROPERTY,
                    BOOTSTRAP_PROFILE.nativeLoaderMainLabel(),
                    BOOTSTRAP_PROFILE.nativeLoaderClientLabel(),
                    BOOTSTRAP_PROFILE.nativeLoaderWindowTitle()
            );
    private static final NativeLoaderProductGameplayFlow PRODUCT_GAMEPLAY_FLOW =
            new NativeLoaderProductGameplayFlow(
                    BOOTSTRAP_PROFILE,
                    NATIVE_MODULE_CLASSPATH_PROPERTY,
                    MINECRAFT_RUNTIME_FLOW::nativeModuleClassLoader
            );
    private static final EchoNativeBootstrapPlayableRuntimeFlow PLAYABLE_RUNTIME_FLOW =
            new EchoNativeBootstrapPlayableRuntimeFlow(
                    BOOTSTRAP_PROFILE,
                    PRODUCT_PROFILE,
                    NATIVE_PRODUCT_PLAYABLE_RUNTIME_KEY,
                    playableRuntimeFlowContext()
            );
    private static final NativeLoaderResourcePackFlow RESOURCE_PACK_FLOW =
            new NativeLoaderResourcePackFlow(BOOTSTRAP_PROFILE, NATIVE_MODULE_CLASSPATH_PROPERTY);
    private static final EchoNativeBootstrapRuntimeBridgeFlow RUNTIME_BRIDGE_FLOW =
            new EchoNativeBootstrapRuntimeBridgeFlow(
                    BOOTSTRAP_PROFILE,
                    NATIVE_PRODUCT_GAMEPLAY_BRIDGE_KEY,
                    LOADER_ENVIRONMENT_FLOW,
                    RESOURCE_PACK_FLOW,
                    BRIDGE_FLOW,
                    PRODUCT_GAMEPLAY_FLOW,
                    PLAYABLE_RUNTIME_FLOW
            );
    private static final EchoNativeBootstrapActivationFlow ACTIVATION_FLOW =
            new EchoNativeBootstrapActivationFlow(
                    MAIN_CLASS,
                    BOOTSTRAP_PROFILE,
                    NATIVE_GAME_DIR_PROPERTY,
                    NATIVE_MODULE_CLASSPATH_PROPERTY,
                    NATIVE_SERVICE_REGISTRY_PATH_PROPERTY,
                    NATIVE_PRODUCT_GAMEPLAY_BRIDGE_KEY,
                    NATIVE_PRODUCT_PLAYABLE_RUNTIME_KEY,
                    AGENT7_LIVE_HOOK_DIRECT_EVIDENCE_PATH_PROPERTY,
                    AGENT7_LIVE_HOOK_SNAPSHOT_FORCE_PROPERTY,
                    AGENT7_LIVE_HOOK_SNAPSHOT_MAX_POLLS_PROPERTY,
                    AGENT7_LIVE_HOOK_SNAPSHOT_POLL_MILLIS_PROPERTY,
                    new EchoNativeBootstrapActivationFlow.Context(
                            MINECRAFT_RUNTIME_FLOW::runtimeClass,
                            LOADER_ENVIRONMENT_FLOW::nativeLoaderClientLabel,
                            MINECRAFT_RUNTIME_FLOW::nativeModuleClassLoader,
                            BRIDGE_FLOW,
                            PRODUCT_GAMEPLAY_FLOW,
                            PLAYABLE_RUNTIME_FLOW,
                            EchoNativeBootstrapOrchestrator::adapterCoreProbe
                    )
            );
    private EchoNativeBootstrapOrchestrator() {
    }

    private static EchoNativeBootstrapProductProfile bootstrapProfile() {
        String profileClassName = System.getProperty(NATIVE_BOOTSTRAP_PROFILE_CLASS_PROPERTY, "").trim();
        if (profileClassName.isBlank()) {
            return EchoNativeGenericBootstrapProfile.INSTANCE;
        }
        try {
            Class<?> type = Class.forName(profileClassName, true, MINECRAFT_RUNTIME_FLOW.nativeModuleClassLoader());
            if (!EchoNativeBootstrapProductProfile.class.isAssignableFrom(type)) {
                throw new IllegalArgumentException(profileClassName
                        + " does not implement " + EchoNativeBootstrapProductProfile.class.getName());
            }
            return (EchoNativeBootstrapProductProfile) type.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException exception) {
            throw new IllegalArgumentException("Unable to create native bootstrap product profile " + profileClassName, exception);
        }
    }

    public static void main(String[] args) throws Exception {
        EchoNativeBootstrapEntrypointRunner.run(args, bootstrapEntrypointContext());
    }

    private static EchoNativeBootstrapEntrypointRunner.Context bootstrapEntrypointContext() {
        return new EchoNativeBootstrapEntrypointRunner.Context(
                MINECRAFT_RUNTIME_FLOW::ensureVanillaBootstrap,
                RUNTIME_BRIDGE_FLOW::apply,
                ACTIVATION_FLOW::writeActivationMarker,
                BRIDGE_FLOW::entityRegistryBridgeConfig,
                RESOURCE_PACK_FLOW::installInternalModuleResourcePackMount
        );
    }

    private static EchoNativeBootstrapUiClientFlow.Context uiClientFlowContext() {
        return new EchoNativeBootstrapUiClientFlow.Context(
                PRODUCT_PROFILE::path,
                RUNTIME_HOST_FLOW::selectedRuntimeHostConfigured,
                RUNTIME_HOST_FLOW::selectedRegisteredRuntimeHost,
                RUNTIME_HOST_FLOW::runtimeSurfaceSupported,
                RUNTIME_HOST_FLOW::runtimeActionSupported,
                RUNTIME_HOST_FLOW::putSelectedHostEvidence,
                RUNTIME_HOST_FLOW::putMissingHostEvidence,
                RUNTIME_HOST_FLOW::putResultEvidence,
                RUNTIME_HOST_FLOW::uiRuntimeHost,
                RUNTIME_HOST_FLOW::uiGrantRuntimeHost,
                RUNTIME_HOST_FLOW::uiActionRuntimeHost,
                ADAPTER_CORE_FLOW::publishEvent,
                ADAPTER_CORE_FLOW::writeSaveData,
                RUNTIME_HOST_FLOW::clientCallerOnly,
                RUNTIME_HOST_FLOW::standaloneMode,
                RUNTIME_HOST_FLOW::standaloneRuntimeHost,
                RUNTIME_HOST_FLOW::standaloneHostStatus,
                RUNTIME_HOST_FLOW::minecraftInstance,
                RUNTIME_HOST_FLOW::minecraftField,
                ADAPTER_CORE_FLOW::scannerUseResult,
                RUNTIME_HOST_FLOW::resolveRuntimeItemId,
                EchoNativeBootstrapOrchestrator::resolveNativeBetaItemId,
                RUNTIME_HOST_FLOW::serverPlayer,
                RUNTIME_HOST_FLOW::serverLevel,
                RUNTIME_HOST_FLOW::server,
                (server, action) -> RUNTIME_HOST_FLOW.invokeOnServer(server, action == null ? null : action::run),
                RUNTIME_HOST_FLOW::runtimeHost,
                RUNTIME_HOST_FLOW::putRuntimeHostEvidence,
                ADAPTER_CORE_FLOW::grantItemResult,
                RUNTIME_HOST_FLOW::resultMutated,
                MINECRAFT_RUNTIME_FLOW::nativeModuleClassLoader
        );
    }

    private static EchoNativeBootstrapModuleRuntimeFlow.Context moduleRuntimeFlowContext() {
        return new EchoNativeBootstrapModuleRuntimeFlow.Context(
                MODULE_SURFACE_CONTEXT,
                MINECRAFT_RUNTIME_FLOW::runtimeClass,
                EchoNativeBootstrapOrchestrator::resolveNativeBetaItemId,
                EchoNativeBootstrapOrchestrator::resolveNativeBetaBlockId
        );
    }

    private static EchoNativeBootstrapProductActionFlow.Context productActionFlowContext() {
        return new EchoNativeBootstrapProductActionFlow.Context(
                MODULE_SURFACE_CONTEXT,
                MINECRAFT_RUNTIME_FLOW::runtimeClass,
                MINECRAFT_RUNTIME_FLOW::interactionResult,
                MINECRAFT_RUNTIME_FLOW::isClientSideLevel,
                EchoNativeBootstrapOrchestrator::invokeNativeEchoModuleRuntime,
                EchoNativeBootstrapOrchestrator::isEchoNamespace,
                CONTENT_ACTION_FLOW::heldItemStack,
                ADAPTER_CORE_FLOW::waterBottleUsed,
                ADAPTER_CORE_FLOW::removeConsumableItem,
                CONTENT_ACTION_FLOW::isCreativePlayer,
                CONTENT_ACTION_FLOW::giveItem,
                ADAPTER_CORE_FLOW::radAwayUsed,
                CONTENT_ACTION_FLOW::executeCommand,
                RUNTIME_HOST_FLOW::invokeRealItemUse,
                ADAPTER_CORE_FLOW::filterCartridgeUsed,
                ADAPTER_CORE_FLOW::crudeFilterUsed,
                ADAPTER_CORE_FLOW::itemConsumed,
                CONTENT_ACTION_FLOW::healPlayer,
                ADAPTER_CORE_FLOW::handWarmerUsed,
                CONTENT_ACTION_FLOW::damageOrShrinkItemStack,
                ADAPTER_CORE_FLOW::scannerUse,
                EchoNativeBootstrapOrchestrator::registryContains,
                ADAPTER_CORE_FLOW::deployEntityRoute,
                ADAPTER_CORE_FLOW::dataLogRecovered,
                CONTENT_ACTION_FLOW::firstItem,
                CONTENT_ACTION_FLOW::hasItem,
                ADAPTER_CORE_FLOW::removeItem,
                ADAPTER_CORE_FLOW::useBlock,
                ADAPTER_CORE_FLOW::receiveEnergy,
                ADAPTER_CORE_FLOW::extractEnergy,
                ADAPTER_CORE_FLOW::insertItem,
                ADAPTER_CORE_FLOW::extractItem,
                ADAPTER_CORE_FLOW::machineTick,
                ADAPTER_CORE_FLOW::powerNodeState,
                ADAPTER_CORE_FLOW::researchLabAnalyze,
                ADAPTER_CORE_FLOW::terminalOpened,
                ADAPTER_CORE_FLOW::waterFiltered,
                EchoNativeBootstrapOrchestrator::nativeRecoveryItemId
        );
    }

    private static EchoNativeBootstrapPlayableRuntimeFlow.Context playableRuntimeFlowContext() {
        return new EchoNativeBootstrapPlayableRuntimeFlow.Context(
                LOADER_ENVIRONMENT_FLOW::nativeLoaderActive,
                LOADER_ENVIRONMENT_FLOW::nativeLoaderMainLabel,
                LOADER_ENVIRONMENT_FLOW::nativeLoaderClientLabel,
                LOADER_ENVIRONMENT_FLOW::nativeLoaderWindowTitle,
                EchoNativeBootstrapOrchestrator::nativeGameDir,
                MINECRAFT_RUNTIME_FLOW::runtimeClass,
                NativeLoaderClientReflectionSupport::invokeOnClientThread,
                NativeLoaderClientReflectionSupport::optionalFieldValue,
                NativeLoaderClientReflectionSupport::optionalMethodValue,
                PRODUCT_ACTION_FLOW::nativeBetaItemAction,
                CONTENT_ACTION_FLOW::setAnyBlockNear,
                PRODUCT_ACTION_FLOW::nativeBetaBlockAction,
                CONTENT_ACTION_FLOW::executeCommand,
                ADAPTER_CORE_FLOW::grantItemEvidence,
                CONTENT_ACTION_FLOW::giveItem,
                MINECRAFT_RUNTIME_FLOW::intMethod,
                CONTENT_ACTION_FLOW::setBlock,
                ADAPTER_CORE_FLOW::placeWorldBlock,
                ADAPTER_CORE_FLOW::writeSaveDataEvidence,
                ADAPTER_CORE_FLOW::publishHudNotification
        );
    }

    private static Path nativeGameDir() {
        String configured = System.getProperty(NATIVE_GAME_DIR_PROPERTY, "").trim();
        return configured.isBlank() ? Path.of(".").toAbsolutePath().normalize() : Path.of(configured).toAbsolutePath().normalize();
    }

    public static Path writeActivationMarker(
            Path markerPath,
            String packId,
            String realMainClass,
            List<String> modules
    ) throws IOException {
        return writeActivationMarker(markerPath, packId, realMainClass, modules, Map.of());
    }

    public static Path writeActivationMarker(
            Path markerPath,
            String packId,
            String realMainClass,
            List<String> modules,
            Map<String, String> nativeEntrypoints
    ) throws IOException {
        return writeActivationMarker(
                markerPath,
                packId,
                realMainClass,
                modules,
                nativeEntrypoints,
                RUNTIME_BRIDGE_FLOW.apply(packId, ACTIVATION_FLOW.activationArgsFromProperties(), modules, nativeEntrypoints)
        );
    }

    public static Path writeActivationMarker(
            Path markerPath,
            String packId,
            String realMainClass,
            List<String> modules,
            Map<String, String> nativeEntrypoints,
            Map<String, Object> runtimeBridge
    ) throws IOException {
        return ACTIVATION_FLOW.writeActivationMarker(
                markerPath,
                packId,
                realMainClass,
                modules,
                nativeEntrypoints,
                runtimeBridge
        );
    }

    private static Object newNativeBetaItem(
            String itemId,
            Class<?> itemClass,
            Class<?> propertiesClass,
            Object properties
    ) throws ReflectiveOperationException {
        return NativeLoaderContentConstructor.newItem(
                itemId,
                itemClass,
                propertiesClass,
                properties,
                CONTENT_CONSTRUCTOR_CONTEXT
        );
    }

    private static Object newNativeBetaBlock(
            String blockId,
            Class<?> blockClass,
            Class<?> blockPropertiesClass,
            Object blockProperties
    ) throws ReflectiveOperationException {
        return NativeLoaderContentConstructor.newBlock(
                blockId,
                blockClass,
                blockPropertiesClass,
                blockProperties,
                CONTENT_CONSTRUCTOR_CONTEXT
        );
    }

    private static boolean isActiveProductNamespace(String namespace) {
        return lowerContentId(BOOTSTRAP_PROFILE.namespace()).equals(lowerContentId(namespace));
    }

    public static Object onNativeBetaItemUse(String itemId, Object level, Object player, Object hand) {
        return PRODUCT_ACTION_FLOW.onItemUse(itemId, level, player, hand);
    }

    public static Object onNativeBetaItemUseOn(String itemId, Object context) {
        return PRODUCT_ACTION_FLOW.onItemUseOn(itemId, context);
    }

    public static boolean onNativeBetaItemBarVisible(String itemId, Object stack) {
        return PRODUCT_ACTION_FLOW.itemBarVisible(itemId, stack);
    }

    public static int onNativeBetaItemBarWidth(String itemId, Object stack) {
        return PRODUCT_ACTION_FLOW.itemBarWidth(itemId, stack);
    }

    public static int onNativeBetaItemBarColor(String itemId, Object stack) {
        return PRODUCT_ACTION_FLOW.itemBarColor(itemId, stack);
    }

    public static Object onNativeBetaBlockUse(String blockId, Object level, Object pos, Object player) {
        return PRODUCT_ACTION_FLOW.onBlockUse(blockId, level, pos, player);
    }

    public static void onNativeBetaBlockPlaced(String blockId, Object level, Object pos) {
        PRODUCT_ACTION_FLOW.onBlockPlaced(blockId, level, pos);
    }

    public static Map<String, Object> nativeGameplaySurfaceContextForMode(String mode) {
        return PRODUCT_ACTION_FLOW.gameplaySurfaceContextForMode(mode);
    }

    static List<Map<String, Object>> ashfallGameplayHandlers() {
        return BOOTSTRAP_PROFILE.requiredGameplayHandlerEvents().stream()
                .map(event -> Map.<String, Object>of(
                        "event", event,
                        "adapterCoreContract", "adaptercore.gameplay_handler." + event,
                        "standaloneRuntimeBackend", BOOTSTRAP_PROFILE.nativeGameplayStandaloneRuntimeBackend()
                                + "." + event,
                        "attached", true,
                        "adapterCoreReplayVerified", true,
                        "liveGameplayHookVerified", false,
                        "minecraftRuntimeAccessed", false
                ))
                .toList();
    }

    static Map<String, Object> applyAshfallGameplayBridge(String packId) {
        String safePackId = packId == null || packId.isBlank()
                ? BOOTSTRAP_PROFILE.nativeGameplayPackId()
                : packId;
        return Map.of(
                "packId", safePackId,
                "gameplayBridgeId", BOOTSTRAP_PROFILE.nativeGameplayBridgeId(),
                "gameplayHandlerClass", BOOTSTRAP_PROFILE.nativeGameplayHandlerClassName(),
                "handlers", ashfallGameplayHandlers(),
                "liveGameplayHandlersAttached", false,
                "liveMinecraftProcessHooksClaimed", false,
                "liveGameplayHookBlockedReason", "live_minecraft_process_hook_attachment_unproven"
        );
    }

    static String nativeProductId(String path) {
        return PRODUCT_ACTION_FLOW.productId(path);
    }

    static boolean nativeEchoModuleRuntimeMutationAccepted(Map<String, Object> report) {
        return PRODUCT_ACTION_FLOW.moduleRuntimeMutationAccepted(report);
    }

    private static Map<String, Object> invokeNativeEchoModuleRuntime(
            String namespace,
            String path,
            Object level,
            Object pos,
            Object player,
            boolean blockRoute
    ) {
        return MODULE_RUNTIME_FLOW.invoke(namespace, path, level, pos, player, blockRoute);
    }

    private static Object heldItemStack(Object player, Object handOrStack) {
        return NativeLoaderInventoryMutationSupport.heldItemStack(INVENTORY_MUTATION_CONTEXT, player, handOrStack);
    }

    private static boolean isCreativePlayer(Object player) {
        return NativeLoaderInventoryMutationSupport.isCreativePlayer(INVENTORY_MUTATION_CONTEXT, player);
    }

    static boolean invokeNativeProductHook(Map<String, Object> report, String role) {
        return MODULE_RUNTIME_FLOW.invokeProductHook(report, role);
    }

    private static String nativeProductGameplayClass(String role) {
        String className = BOOTSTRAP_PROFILE.nativeAdapterCoreGameplayClasses().get(lowerContentId(role));
        return className == null ? "" : className.trim();
    }

    private static String lowerContentId(String contentId) {
        return contentId == null ? "" : contentId.trim().toLowerCase(java.util.Locale.ROOT);
    }

    private static String resolveNativeBetaItemId(String requestedId) {
        return CONTENT_FALLBACK.resolveItemId(requestedId);
    }

    private static String resolveNativeBetaBlockId(String requestedId) {
        return CONTENT_FALLBACK.resolveBlockId(requestedId);
    }

    private static boolean registryContains(String registryField, String contentId) {
        return CONTENT_FALLBACK.registryContains(registryField, contentId);
    }

    private static boolean isEchoNamespace(String namespace) {
        return CONTENT_FALLBACK.isActiveNamespace(namespace);
    }

    private static List<String> echoItemIds() {
        return CONTENT_FALLBACK.cachedItemIds();
    }

    private static List<String> echoBlockIds() {
        return CONTENT_FALLBACK.cachedBlockIds();
    }

    public static boolean useNativeScannerFromUi() {
        return UI_CLIENT_FLOW.useScanner();
    }

    public static Map<String, Object> useNativeScannerFromUiEvidence() {
        return UI_CLIENT_FLOW.useScannerEvidence();
    }

    public static boolean grantNativeItemFromUi(String itemId, int count) {
        return UI_CLIENT_FLOW.grantItem(itemId, count);
    }

    public static Map<String, Object> grantNativeItemFromUiEvidence(String itemId, int count) {
        return UI_CLIENT_FLOW.grantItemEvidence(itemId, count);
    }

    public static Map<String, Object> executeNativeTerminalCommandFromUi(String command, String output) {
        return UI_CLIENT_FLOW.executeTerminalCommand(command, output);
    }

    public static Map<String, Object> executeNativeIndexSearchFromUi(String query, String output) {
        return UI_CLIENT_FLOW.executeIndexSearch(query, output);
    }

    public static Map<String, Object> executeNativeHudRefreshFromUi(
            int health,
            String hazard,
            String mission,
            String cinematicCue
    ) {
        return UI_CLIENT_FLOW.executeHudRefresh(health, hazard, mission, cinematicCue);
    }

    public static Map<String, Object> executeNativeMissionLogUpdateFromUi(
            String missionId,
            String missionTitle,
            String missionObjective,
            double missionProgress,
            String missionStatus,
            String missionUpdateLine
    ) {
        return UI_CLIENT_FLOW.executeMissionLogUpdate(
                missionId,
                missionTitle,
                missionObjective,
                missionProgress,
                missionStatus,
                missionUpdateLine
        );
    }

    public static Map<String, Object> executeNativeSurfaceOpenFromUi(String surface, String effect) {
        return UI_CLIENT_FLOW.executeSurfaceOpen(surface, effect);
    }

    public static Map<String, Object> executeNativeMachineSurfaceOpenFromGameplay(Map<String, Object> context) {
        return UI_CLIENT_FLOW.executeMachineSurfaceOpen(context);
    }

    public static Map<String, Object> executeNativeUiRuntimeEventFromUi(
            String runtimeActionId,
            Map<String, Object> payload
    ) {
        return UI_CLIENT_FLOW.executeRuntimeEvent(runtimeActionId, payload);
    }

    public static Map<String, Object> executeNativeUiSaveDataMutationFromUi(
            String runtimeActionId,
            String scope,
            String key,
            Map<String, Object> payload
    ) {
        return UI_CLIENT_FLOW.executeSaveDataMutation(runtimeActionId, scope, key, payload);
    }

    static String nativeUiScreenIdForSurface(String surface) {
        return UI_CLIENT_FLOW.screenIdForSurface(surface);
    }

    static String nativeUiCanonicalIdForSurface(String surface) {
        return UI_CLIENT_FLOW.canonicalIdForSurface(surface);
    }

    static String nativeUiTargetForSurface(String surface) {
        return UI_CLIENT_FLOW.targetForSurface(surface);
    }

    public static boolean nativeUiHostActionGateActive() {
        return UI_CLIENT_FLOW.hostActionGateActive();
    }

    public static List<String> nativeUiSupportedActionIds() {
        return UI_CLIENT_FLOW.supportedActionIds();
    }

    static List<NativeUiActionRoute> nativeUiActionRoutes() {
        return UI_CLIENT_FLOW.actionRoutes();
    }

    static Map<String, List<String>> nativeUiDataSourceRoots() {
        return UI_CLIENT_FLOW.dataSourceRoots();
    }

    static Map<String, String> nativeUiDefaultContentIds() {
        return UI_CLIENT_FLOW.defaultContentIds();
    }

    static List<String> nativeMainMenuOptions() {
        return UI_CLIENT_FLOW.mainMenuOptions();
    }

    static String nativeLoaderThemeMode() {
        return BOOTSTRAP_PROFILE.nativeLoaderThemeMode();
    }

    static String nativeLoaderThemeId() {
        return BOOTSTRAP_PROFILE.nativeLoaderThemeId();
    }

    static List<NativePhysicalActionRoute> nativePhysicalActionRoutes() {
        return UI_CLIENT_FLOW.physicalActionRoutes();
    }

    static List<NativeUiSurfaceRoute> nativeUiSurfaceRoutes() {
        return UI_CLIENT_FLOW.surfaceRoutes();
    }

    static String nativeClientScreenClassName(String surface) {
        return UI_CLIENT_FLOW.clientScreenClassName(surface);
    }

    static Class<?> nativeClientScreenClass(String surface) throws ClassNotFoundException {
        return UI_CLIENT_FLOW.clientScreenClass(surface);
    }

    static List<String> nativeClientHudRendererClassNames() {
        return UI_CLIENT_FLOW.clientHudRendererClassNames();
    }

    static List<String> nativeClientLoadingRendererClassNames() {
        return UI_CLIENT_FLOW.clientLoadingRendererClassNames();
    }

    static ClassLoader nativeClientModuleClassLoader() {
        return UI_CLIENT_FLOW.clientModuleClassLoader();
    }

    static List<String> nativeUiProductHotkeys() {
        return UI_CLIENT_FLOW.productHotkeys();
    }

    static Map<String, String> nativeUiHotkeyConflicts() {
        return UI_CLIENT_FLOW.hotkeyConflicts();
    }

    public static String nativeRecoveryItemId() {
        return UI_CLIENT_FLOW.recoveryItemId();
    }

    static String nativeIndexSearchQuery() {
        return UI_CLIENT_FLOW.indexSearchQuery();
    }

    static String nativeLensFallbackTarget() {
        return UI_CLIENT_FLOW.lensFallbackTarget();
    }

    static String nativeMachineScreenId() {
        return UI_CLIENT_FLOW.machineScreenId();
    }

    static String nativeMachineEffectPrefix() {
        return UI_CLIENT_FLOW.machineEffectPrefix();
    }

    static String nativeMachineRecipeCatalogSourcePath() {
        return UI_CLIENT_FLOW.machineRecipeCatalogSourcePath();
    }

    static Map<String, Object> nativeProductGameplayBridge(Map<String, Object> runtimeBridge) {
        return UI_CLIENT_FLOW.gameplayBridge(runtimeBridge, NATIVE_PRODUCT_GAMEPLAY_BRIDGE_KEY);
    }

    public static String nativeProductNamespace() {
        return UI_CLIENT_FLOW.namespace();
    }

    private static boolean giveNativeBetaItem(Object player, String itemId, int count) {
        if (player == null || itemId == null || itemId.isBlank()) {
            return false;
        }
        try {
            String resolvedItemId = resolveNativeBetaItemId(itemId);
            if (resolvedItemId.isBlank()) {
                return false;
            }
            Object serverPlayer = RUNTIME_HOST_FLOW.serverPlayer(player);
            Object level = RUNTIME_HOST_FLOW.serverLevel(null, serverPlayer);
            Object server = RUNTIME_HOST_FLOW.server(player, serverPlayer);
            return RUNTIME_HOST_FLOW.invokeOnServer(server,
                    () -> ADAPTER_CORE_FLOW.grantItem(serverPlayer, level, resolvedItemId, Math.max(1, count)));
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static Object nativeMinecraftInstance() {
        return RUNTIME_HOST_FLOW.minecraftInstance();
    }

    private static String blockPosText(Object pos) {
        return NativeLoaderWorldBlockSetter.blockPosText(WORLD_BLOCK_SETTER_CONTEXT, pos);
    }

    private static List<String> discoverEchoItemIds() throws IOException {
        return CONTENT_FALLBACK.discoverItemIds();
    }

    private static List<String> discoverEchoBlockIds() throws IOException {
        return CONTENT_FALLBACK.discoverBlockIds();
    }

    private static String nativeBetaBlockFallback(String blockId) {
        return CONTENT_FALLBACK.blockFallback(blockId);
    }

    private static Map<String, Object> adapterCoreProbe() {
        return NativeLoaderAdapterCoreProbe.probe(MINECRAFT_RUNTIME_FLOW::nativeModuleClassLoader);
    }

}
