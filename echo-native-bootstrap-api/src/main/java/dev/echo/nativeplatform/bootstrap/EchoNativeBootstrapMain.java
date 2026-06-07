package dev.echo.nativeplatform.bootstrap;

import dev.echo.nativeplatform.contracts.EchoNativeBootstrapProductProfile.NativePhysicalActionRoute;
import dev.echo.nativeplatform.contracts.EchoNativeBootstrapProductProfile.NativeUiActionRoute;
import dev.echo.nativeplatform.contracts.EchoNativeBootstrapProductProfile.NativeUiSurfaceRoute;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Public bootstrap entrypoint and hook delegate surface.
 *
 * <p>Native product launch authority lives in echo-native-product-launcher;
 * this entrypoint only delegates to the bootstrap handoff once the blessed
 * startNativeClient path has passed pre-window validation.</p>
 */
public final class EchoNativeBootstrapMain {
    public static final String MAIN_CLASS = "dev.echo.nativeplatform.bootstrap.EchoNativeBootstrapMain";
    public static final String AUTHORIZED_HANDOFF_PROPERTY = "echo.native.bootstrap.authorizedHandoff";
    public static final String AUTHORIZED_HANDOFF_VALUE = "startNativeClient";

    private EchoNativeBootstrapMain() {
    }

    public static void main(String[] args) throws Exception {
        requireBlessedHandoff();
        EchoNativeBootstrapOrchestrator.main(args);
    }

    private static void requireBlessedHandoff() {
        String handoff = System.getProperty(AUTHORIZED_HANDOFF_PROPERTY, "");
        if (!AUTHORIZED_HANDOFF_VALUE.equals(handoff)) {
            throw new IllegalStateException(
                    "EchoNativeBootstrapMain is an internal Native Loader handoff. "
                            + "Launch the product client through ./gradlew startNativeClient after product-launcher "
                            + "pre-window validation. Expected -D" + AUTHORIZED_HANDOFF_PROPERTY + "="
                            + AUTHORIZED_HANDOFF_VALUE + "."
            );
        }
    }

    public static Path writeActivationMarker(
            Path markerPath,
            String packId,
            String realMainClass,
            List<String> modules
    ) throws IOException {
        return EchoNativeBootstrapOrchestrator.writeActivationMarker(markerPath, packId, realMainClass, modules);
    }

    public static Path writeActivationMarker(
            Path markerPath,
            String packId,
            String realMainClass,
            List<String> modules,
            Map<String, String> nativeEntrypoints
    ) throws IOException {
        return EchoNativeBootstrapOrchestrator.writeActivationMarker(
                markerPath,
                packId,
                realMainClass,
                modules,
                nativeEntrypoints
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
        return EchoNativeBootstrapOrchestrator.writeActivationMarker(
                markerPath,
                packId,
                realMainClass,
                modules,
                nativeEntrypoints,
                runtimeBridge
        );
    }

    public static Object onNativeBetaItemUse(String itemId, Object level, Object player, Object hand) {
        return EchoNativeBootstrapOrchestrator.onNativeBetaItemUse(itemId, level, player, hand);
    }

    public static Object onNativeBetaItemUseOn(String itemId, Object context) {
        return EchoNativeBootstrapOrchestrator.onNativeBetaItemUseOn(itemId, context);
    }

    public static boolean onNativeBetaItemBarVisible(String itemId, Object stack) {
        return EchoNativeBootstrapOrchestrator.onNativeBetaItemBarVisible(itemId, stack);
    }

    public static int onNativeBetaItemBarWidth(String itemId, Object stack) {
        return EchoNativeBootstrapOrchestrator.onNativeBetaItemBarWidth(itemId, stack);
    }

    public static int onNativeBetaItemBarColor(String itemId, Object stack) {
        return EchoNativeBootstrapOrchestrator.onNativeBetaItemBarColor(itemId, stack);
    }

    public static Object onNativeBetaBlockUse(String blockId, Object level, Object pos, Object player) {
        return EchoNativeBootstrapOrchestrator.onNativeBetaBlockUse(blockId, level, pos, player);
    }

    public static void onNativeBetaBlockPlaced(String blockId, Object level, Object pos) {
        EchoNativeBootstrapOrchestrator.onNativeBetaBlockPlaced(blockId, level, pos);
    }

    public static Map<String, Object> nativeGameplaySurfaceContextForMode(String mode) {
        return EchoNativeBootstrapOrchestrator.nativeGameplaySurfaceContextForMode(mode);
    }

    static String nativeProductId(String path) {
        return EchoNativeBootstrapOrchestrator.nativeProductId(path);
    }

    static boolean nativeEchoModuleRuntimeMutationAccepted(Map<String, Object> report) {
        return EchoNativeBootstrapOrchestrator.nativeEchoModuleRuntimeMutationAccepted(report);
    }

    static boolean invokeNativeProductHook(Map<String, Object> report, String role) {
        return EchoNativeBootstrapOrchestrator.invokeNativeProductHook(report, role);
    }

    public static boolean useNativeScannerFromUi() {
        return EchoNativeBootstrapOrchestrator.useNativeScannerFromUi();
    }

    public static Map<String, Object> useNativeScannerFromUiEvidence() {
        return EchoNativeBootstrapOrchestrator.useNativeScannerFromUiEvidence();
    }

    public static boolean grantNativeItemFromUi(String itemId, int count) {
        return EchoNativeBootstrapOrchestrator.grantNativeItemFromUi(itemId, count);
    }

    public static Map<String, Object> grantNativeItemFromUiEvidence(String itemId, int count) {
        return EchoNativeBootstrapOrchestrator.grantNativeItemFromUiEvidence(itemId, count);
    }

    public static Map<String, Object> executeNativeTerminalCommandFromUi(String command, String output) {
        return EchoNativeBootstrapOrchestrator.executeNativeTerminalCommandFromUi(command, output);
    }

    public static Map<String, Object> executeNativeIndexSearchFromUi(String query, String output) {
        return EchoNativeBootstrapOrchestrator.executeNativeIndexSearchFromUi(query, output);
    }

    public static Map<String, Object> executeNativeHudRefreshFromUi(
            int health,
            String hazard,
            String mission,
            String cinematicCue
    ) {
        return EchoNativeBootstrapOrchestrator.executeNativeHudRefreshFromUi(health, hazard, mission, cinematicCue);
    }

    public static Map<String, Object> executeNativeMissionLogUpdateFromUi(
            String missionId,
            String missionTitle,
            String missionObjective,
            double missionProgress,
            String missionStatus,
            String missionUpdateLine
    ) {
        return EchoNativeBootstrapOrchestrator.executeNativeMissionLogUpdateFromUi(
                missionId,
                missionTitle,
                missionObjective,
                missionProgress,
                missionStatus,
                missionUpdateLine
        );
    }

    public static Map<String, Object> executeNativeSurfaceOpenFromUi(String surface, String effect) {
        return EchoNativeBootstrapOrchestrator.executeNativeSurfaceOpenFromUi(surface, effect);
    }

    public static Map<String, Object> executeNativeMachineSurfaceOpenFromGameplay(Map<String, Object> context) {
        return EchoNativeBootstrapOrchestrator.executeNativeMachineSurfaceOpenFromGameplay(context);
    }

    public static Map<String, Object> executeNativeUiRuntimeEventFromUi(
            String runtimeActionId,
            Map<String, Object> payload
    ) {
        return EchoNativeBootstrapOrchestrator.executeNativeUiRuntimeEventFromUi(runtimeActionId, payload);
    }

    public static Map<String, Object> executeNativeUiSaveDataMutationFromUi(
            String runtimeActionId,
            String scope,
            String key,
            Map<String, Object> payload
    ) {
        return EchoNativeBootstrapOrchestrator.executeNativeUiSaveDataMutationFromUi(
                runtimeActionId,
                scope,
                key,
                payload
        );
    }

    static String nativeUiScreenIdForSurface(String surface) {
        return EchoNativeBootstrapOrchestrator.nativeUiScreenIdForSurface(surface);
    }

    static String nativeUiCanonicalIdForSurface(String surface) {
        return EchoNativeBootstrapOrchestrator.nativeUiCanonicalIdForSurface(surface);
    }

    static String nativeUiTargetForSurface(String surface) {
        return EchoNativeBootstrapOrchestrator.nativeUiTargetForSurface(surface);
    }

    static String nativeLensFallbackTarget() {
        return EchoNativeBootstrapOrchestrator.nativeLensFallbackTarget();
    }

    public static boolean nativeUiHostActionGateActive() {
        return EchoNativeBootstrapOrchestrator.nativeUiHostActionGateActive();
    }

    public static List<String> nativeUiSupportedActionIds() {
        return EchoNativeBootstrapOrchestrator.nativeUiSupportedActionIds();
    }

    static List<NativeUiActionRoute> nativeUiActionRoutes() {
        return EchoNativeBootstrapOrchestrator.nativeUiActionRoutes();
    }

    static Map<String, List<String>> nativeUiDataSourceRoots() {
        return EchoNativeBootstrapOrchestrator.nativeUiDataSourceRoots();
    }

    static Map<String, String> nativeUiDefaultContentIds() {
        return EchoNativeBootstrapOrchestrator.nativeUiDefaultContentIds();
    }

    static List<String> nativeMainMenuOptions() {
        return EchoNativeBootstrapOrchestrator.nativeMainMenuOptions();
    }

    static List<NativePhysicalActionRoute> nativePhysicalActionRoutes() {
        return EchoNativeBootstrapOrchestrator.nativePhysicalActionRoutes();
    }

    static List<NativeUiSurfaceRoute> nativeUiSurfaceRoutes() {
        return EchoNativeBootstrapOrchestrator.nativeUiSurfaceRoutes();
    }

    static String nativeClientScreenClassName(String surface) {
        return EchoNativeBootstrapOrchestrator.nativeClientScreenClassName(surface);
    }

    static Class<?> nativeClientScreenClass(String surface) throws ClassNotFoundException {
        return EchoNativeBootstrapOrchestrator.nativeClientScreenClass(surface);
    }

    static List<String> nativeClientHudRendererClassNames() {
        return EchoNativeBootstrapOrchestrator.nativeClientHudRendererClassNames();
    }

    static List<String> nativeClientLoadingRendererClassNames() {
        return EchoNativeBootstrapOrchestrator.nativeClientLoadingRendererClassNames();
    }

    static ClassLoader nativeClientModuleClassLoader() {
        return EchoNativeBootstrapOrchestrator.nativeClientModuleClassLoader();
    }

    static List<String> nativeUiProductHotkeys() {
        return EchoNativeBootstrapOrchestrator.nativeUiProductHotkeys();
    }

    static Map<String, String> nativeUiHotkeyConflicts() {
        return EchoNativeBootstrapOrchestrator.nativeUiHotkeyConflicts();
    }

    public static String nativeRecoveryItemId() {
        return EchoNativeBootstrapOrchestrator.nativeRecoveryItemId();
    }

    static String nativeIndexSearchQuery() {
        return EchoNativeBootstrapOrchestrator.nativeIndexSearchQuery();
    }

    static String nativeMachineScreenId() {
        return EchoNativeBootstrapOrchestrator.nativeMachineScreenId();
    }

    static String nativeMachineEffectPrefix() {
        return EchoNativeBootstrapOrchestrator.nativeMachineEffectPrefix();
    }

    static String nativeMachineRecipeCatalogSourcePath() {
        return EchoNativeBootstrapOrchestrator.nativeMachineRecipeCatalogSourcePath();
    }

    static Map<String, Object> nativeProductGameplayBridge(Map<String, Object> runtimeBridge) {
        return EchoNativeBootstrapOrchestrator.nativeProductGameplayBridge(runtimeBridge);
    }

    public static String nativeProductNamespace() {
        return EchoNativeBootstrapOrchestrator.nativeProductNamespace();
    }
}
