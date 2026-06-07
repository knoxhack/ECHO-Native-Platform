package dev.echo.nativeplatform.bootstrap;

import dev.echo.nativeplatform.contracts.EchoNativeBootstrapProductProfile;
import dev.echo.nativeplatform.contracts.EchoNativeBootstrapProductProfile.NativePhysicalActionRoute;
import dev.echo.nativeplatform.contracts.EchoNativeBootstrapProductProfile.NativeUiActionRoute;
import dev.echo.nativeplatform.contracts.EchoNativeBootstrapProductProfile.NativeUiSurfaceRoute;
import dev.echo.nativeplatform.loader.NativeLoaderClientUiPlayableActions;
import dev.echo.nativeplatform.loader.NativeLoaderClientUiRuntimeActions;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

final class EchoNativeBootstrapUiClientFlow {
    private final EchoNativeBootstrapProductProfile bootstrapProfile;
    private final EchoNativeProductProfileCore productProfile;
    private final Context context;

    EchoNativeBootstrapUiClientFlow(
            EchoNativeBootstrapProductProfile bootstrapProfile,
            EchoNativeProductProfileCore productProfile,
            Context context
    ) {
        this.bootstrapProfile = bootstrapProfile;
        this.productProfile = productProfile;
        this.context = context;
    }

    boolean useScanner() {
        return NativeLoaderClientUiPlayableActions.mutationAccepted(useScannerEvidence());
    }

    Map<String, Object> useScannerEvidence() {
        return NativeLoaderClientUiPlayableActions.useScanner(playableActionContext());
    }

    boolean grantItem(String itemId, int count) {
        return NativeLoaderClientUiPlayableActions.mutationAccepted(grantItemEvidence(itemId, count));
    }

    Map<String, Object> grantItemEvidence(String itemId, int count) {
        return NativeLoaderClientUiPlayableActions.grantItem(playableActionContext(), itemId, count);
    }

    Map<String, Object> executeTerminalCommand(String command, String output) {
        return NativeLoaderClientUiRuntimeActions.executeTerminalCommand(runtimeActionContext(), command, output);
    }

    Map<String, Object> executeIndexSearch(String query, String output) {
        return NativeLoaderClientUiRuntimeActions.executeIndexSearch(runtimeActionContext(), query, output);
    }

    Map<String, Object> executeHudRefresh(
            int health,
            String hazard,
            String mission,
            String cinematicCue
    ) {
        return NativeLoaderClientUiRuntimeActions.executeHudRefresh(
                runtimeActionContext(),
                health,
                hazard,
                mission,
                cinematicCue
        );
    }

    Map<String, Object> executeMissionLogUpdate(
            String missionId,
            String missionTitle,
            String missionObjective,
            double missionProgress,
            String missionStatus,
            String missionUpdateLine
    ) {
        return NativeLoaderClientUiRuntimeActions.executeMissionLogUpdate(
                runtimeActionContext(),
                missionId,
                missionTitle,
                missionObjective,
                missionProgress,
                missionStatus,
                missionUpdateLine
        );
    }

    Map<String, Object> executeSurfaceOpen(String surface, String effect) {
        return NativeLoaderClientUiRuntimeActions.executeSurfaceOpen(runtimeActionContext(), surface, effect);
    }

    Map<String, Object> executeMachineSurfaceOpen(Map<String, Object> machineContext) {
        return NativeLoaderClientUiRuntimeActions.executeMachineSurfaceOpen(runtimeActionContext(), machineContext);
    }

    Map<String, Object> executeRuntimeEvent(String runtimeActionId, Map<String, Object> payload) {
        return NativeLoaderClientUiRuntimeActions.executeRuntimeEvent(runtimeActionContext(), runtimeActionId, payload);
    }

    Map<String, Object> executeSaveDataMutation(
            String runtimeActionId,
            String scope,
            String key,
            Map<String, Object> payload
    ) {
        return NativeLoaderClientUiRuntimeActions.executeSaveDataMutation(
                runtimeActionContext(),
                runtimeActionId,
                scope,
                key,
                payload
        );
    }

    String screenIdForSurface(String surface) {
        return productProfile.screenIdForSurface(surface);
    }

    String canonicalIdForSurface(String surface) {
        return productProfile.canonicalIdForSurface(surface);
    }

    String targetForSurface(String surface) {
        return productProfile.targetForSurface(surface);
    }

    boolean hostActionGateActive() {
        return NativeLoaderClientUiRuntimeActions.hostActionGateActive();
    }

    List<String> supportedActionIds() {
        return NativeLoaderClientUiRuntimeActions.supportedActionIds(runtimeActionContext());
    }

    List<NativeUiActionRoute> actionRoutes() {
        return productProfile.uiActionRoutes();
    }

    Map<String, List<String>> dataSourceRoots() {
        return productProfile.uiDataSourceRoots();
    }

    Map<String, String> defaultContentIds() {
        return productProfile.uiDefaultContentIds();
    }

    String defaultContentId(String key, String fallback) {
        return productProfile.uiDefaultContentId(key, fallback);
    }

    List<String> mainMenuOptions() {
        return productProfile.mainMenuOptions();
    }

    List<NativePhysicalActionRoute> physicalActionRoutes() {
        return productProfile.physicalActionRoutes();
    }

    List<NativeUiSurfaceRoute> surfaceRoutes() {
        return productProfile.uiSurfaceRoutes();
    }

    String clientScreenClassName(String surface) {
        return productProfile.clientScreenClassName(surface);
    }

    Class<?> clientScreenClass(String surface) throws ClassNotFoundException {
        String className = clientScreenClassName(surface);
        if (className.isBlank()) {
            return null;
        }
        return Class.forName(className, true, context.nativeModuleClassLoader().get());
    }

    List<String> clientHudRendererClassNames() {
        return productProfile.clientHudRendererClassNames();
    }

    List<String> clientLoadingRendererClassNames() {
        return productProfile.clientLoadingRendererClassNames();
    }

    ClassLoader clientModuleClassLoader() {
        return context.nativeModuleClassLoader().get();
    }

    List<String> productHotkeys() {
        return productProfile.uiProductHotkeys();
    }

    Map<String, String> hotkeyConflicts() {
        return productProfile.uiHotkeyConflicts();
    }

    String recoveryItemId() {
        return productProfile.recoveryItemId();
    }

    String indexSearchQuery() {
        return productProfile.indexSearchQuery();
    }

    String lensFallbackTarget() {
        return productProfile.lensFallbackTarget();
    }

    String machineScreenId() {
        return productProfile.machineScreenId();
    }

    String machineEffectPrefix() {
        return productProfile.machineEffectPrefix();
    }

    String machineRecipeCatalogSourcePath() {
        return productProfile.machineRecipeCatalogSourcePath();
    }

    Map<String, Object> gameplayBridge(Map<String, Object> runtimeBridge, String productGameplayBridgeKey) {
        return productProfile.gameplayBridge(runtimeBridge, productGameplayBridgeKey);
    }

    String namespace() {
        return productProfile.namespace();
    }

    private NativeLoaderClientUiRuntimeActions.Context runtimeActionContext() {
        return new NativeLoaderClientUiRuntimeActions.Context(
                bootstrapProfile.nativeUiActionScannerUsed(),
                bootstrapProfile.nativeUiActionUseScanner(),
                bootstrapProfile.nativeUiActionGrantItem(),
                bootstrapProfile.nativeUiActionTerminalCommand(),
                bootstrapProfile.nativeUiActionIndexSearch(),
                bootstrapProfile.nativeUiActionHudRefresh(),
                bootstrapProfile.nativeUiActionMissionLogUpdate(),
                bootstrapProfile.nativeUiActionSurfaceOpen(),
                bootstrapProfile.nativeUiActionIndexBookmark(),
                bootstrapProfile.nativeUiActionHoloMapState(),
                bootstrapProfile.nativeUiActionSignalOsTerminal(),
                bootstrapProfile.nativeUiActionCommand(),
                bootstrapProfile.nativeUiEventCommandExecution(),
                bootstrapProfile.nativeUiEventTerminalOpened(),
                bootstrapProfile.nativeUiEventClientTick(),
                bootstrapProfile.nativeUiEventMissionObjectiveCompleted(),
                this::screenIdForSurface,
                this::canonicalIdForSurface,
                this::targetForSurface,
                this::defaultContentId,
                bootstrapProfile::nativeMachineScreenId,
                () -> bootstrapProfile.id("machine"),
                bootstrapProfile::nativeMachineEffectPrefix,
                context.runtimeHost()::get,
                context.grantRuntimeHost()::apply,
                context.actionRuntimeHost()::apply,
                context.runtimeSurfaceSupported()::test,
                context.runtimeActionSupported()::test,
                context.putSelectedRuntimeHostEvidence()::accept,
                context.putMissingNativeRuntimeHostEvidence()::accept,
                context.putNativeResultEvidence()::accept,
                context.publishEvent()::publish,
                context.writeSaveData()::write
        );
    }

    private NativeLoaderClientUiPlayableActions.Context playableActionContext() {
        return new NativeLoaderClientUiPlayableActions.Context(
                bootstrapProfile.nativeUiActionScannerUsed(),
                bootstrapProfile.nativeUiActionGrantItem(),
                this::recoveryItemId,
                context.productPath()::apply,
                this::screenIdForSurface,
                this::targetForSurface,
                this::defaultContentId,
                context.selectedRuntimeHostConfigured()::get,
                context.selectedRegisteredRuntimeHost()::get,
                context.runtimeSurfaceSupported()::test,
                context.runtimeActionSupported()::test,
                context.putSelectedRuntimeHostEvidence()::accept,
                context.publishEvent()::publish,
                context.putNativeResultEvidence()::accept,
                context.clientCallerOnly()::get,
                context.standaloneMode()::get,
                context.standaloneRuntimeHost()::get,
                context.standaloneHostStatus()::status,
                context.minecraftInstance()::get,
                context.minecraftField()::apply,
                context.scannerUseResult()::scan,
                context.resolveRuntimeItemId()::apply,
                context.resolveItemId()::apply,
                context.runtimeServerPlayer()::apply,
                context.runtimeServerLevel()::apply,
                context.runtimeServer()::apply,
                context.invokeOnServer()::invoke,
                context.runtimeHostForPlayer()::apply,
                context.putNativeRuntimeHostEvidence()::accept,
                context.grantItemResult()::grant,
                context.runtimeResultMutated()::test
        );
    }

    record Context(
            Function<String, String> productPath,
            Supplier<Boolean> selectedRuntimeHostConfigured,
            Supplier<Object> selectedRegisteredRuntimeHost,
            BiPredicate<Object, String> runtimeSurfaceSupported,
            BiPredicate<Object, String> runtimeActionSupported,
            BiConsumer<Map<String, Object>, Object> putSelectedRuntimeHostEvidence,
            Consumer<Map<String, Object>> putMissingNativeRuntimeHostEvidence,
            BiConsumer<Map<String, Object>, Object> putNativeResultEvidence,
            Supplier<Object> runtimeHost,
            Function<Object, Object> grantRuntimeHost,
            Function<Object, Object> actionRuntimeHost,
            EventPublisher publishEvent,
            SaveDataWriter writeSaveData,
            Supplier<Boolean> clientCallerOnly,
            Supplier<Boolean> standaloneMode,
            Supplier<Object> standaloneRuntimeHost,
            StandaloneHostStatus standaloneHostStatus,
            Supplier<Object> minecraftInstance,
            BiFunction<Object, String, Object> minecraftField,
            ScannerUseResult scannerUseResult,
            BiFunction<Object, String, String> resolveRuntimeItemId,
            Function<String, String> resolveItemId,
            Function<Object, Object> runtimeServerPlayer,
            BiFunction<Object, Object, Object> runtimeServerLevel,
            BiFunction<Object, Object, Object> runtimeServer,
            ServerInvoker invokeOnServer,
            BiFunction<Object, Object, Object> runtimeHostForPlayer,
            BiConsumer<Map<String, Object>, Object> putNativeRuntimeHostEvidence,
            GrantItemResult grantItemResult,
            Predicate<Object> runtimeResultMutated,
            Supplier<ClassLoader> nativeModuleClassLoader
    ) {
    }

    @FunctionalInterface
    interface EventPublisher {
        Object publish(
                Object runtimeHost,
                String eventId,
                Map<String, Object> payload,
                String idempotencyKey
        ) throws ReflectiveOperationException;
    }

    @FunctionalInterface
    interface SaveDataWriter {
        Object write(
                Object runtimeHost,
                String scope,
                String key,
                Map<String, Object> payload,
                String idempotencyKey
        ) throws ReflectiveOperationException;
    }

    @FunctionalInterface
    interface ScannerUseResult {
        Object scan(Object level, Object player, String source, boolean deepScan) throws Throwable;
    }

    @FunctionalInterface
    interface GrantItemResult {
        Object grant(Object runtimeHost, String itemId, int count) throws ReflectiveOperationException;
    }

    @FunctionalInterface
    interface StandaloneHostStatus {
        String status(Object host, String methodName, Object... args);
    }

    @FunctionalInterface
    interface ServerInvoker {
        boolean invoke(Object server, NativeLoaderClientUiPlayableActions.ServerAction action);
    }
}
