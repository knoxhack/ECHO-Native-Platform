package dev.echo.nativeplatform.bootstrap;

import dev.echo.nativeplatform.loader.NativeLoaderUiActionRouter;

import java.util.Map;

public final class EchoNativeAgent5UiActionRouter {
    private EchoNativeAgent5UiActionRouter() {
    }

    private static void configure() {
        NativeLoaderUiActionRouter.configure(new NativeLoaderUiActionRouter.Context(
                EchoNativeAgent5UiHandlerRegistry::dataSources,
                EchoNativeAgent5UiHandlerRegistry::executeTerminal,
                EchoNativeAgent5UiHandlerRegistry::searchIndex,
                EchoNativeAgent5UiHandlerRegistry::scanLens,
                EchoNativeAgent5UiHandlerRegistry::recover,
                EchoNativeAgent5UiHandlerRegistry::notificationQueue,
                EchoNativeBootstrapMain::nativeRecoveryItemId
        ));
    }

    public static String focusPath(String mode, String previousMode) {
        configure();
        return NativeLoaderUiActionRouter.focusPath(mode, previousMode);
    }

    public static Map<String, Object> routeInitialFocus(String mode, String previousMode) {
        configure();
        return NativeLoaderUiActionRouter.routeInitialFocus(mode, previousMode);
    }

    public static Map<String, Object> routeCharacter(
            String mode,
            String focusedControl,
            String terminalBuffer,
            String indexBuffer,
            char codePoint
    ) {
        configure();
        return NativeLoaderUiActionRouter.routeCharacter(mode, focusedControl, terminalBuffer, indexBuffer, codePoint);
    }

    public static Map<String, Object> routeKey(String keyName, String mode, String previousMode) {
        configure();
        return NativeLoaderUiActionRouter.routeKey(keyName, mode, previousMode);
    }

    public static Map<String, Object> routeEditKey(
            String keyName,
            String mode,
            String focusedControl,
            String terminalBuffer,
            String indexBuffer
    ) {
        configure();
        return NativeLoaderUiActionRouter.routeEditKey(keyName, mode, focusedControl, terminalBuffer, indexBuffer);
    }

    public static Map<String, Object> routeMouseClick(String mode, String previousMode, Map<String, Object> state) {
        configure();
        return NativeLoaderUiActionRouter.routeMouseClick(mode, previousMode, state);
    }

    public static Map<String, Object> routeListNavigation(String keyName, String mode, int selectedIndex) {
        configure();
        return NativeLoaderUiActionRouter.routeListNavigation(keyName, mode, selectedIndex);
    }

    public static Map<String, Object> routeNotificationDismiss(Object notifications) {
        configure();
        return NativeLoaderUiActionRouter.routeNotificationDismiss(notifications);
    }

    public static Map<String, Object> routeSettingsAdjustment(String selectedOption, double hudScale, boolean subtitles) {
        configure();
        return NativeLoaderUiActionRouter.routeSettingsAdjustment(selectedOption, hudScale, subtitles);
    }

    public static Map<String, Object> routePauseOption(String selectedOption, String previousMode) {
        configure();
        return NativeLoaderUiActionRouter.routePauseOption(selectedOption, previousMode);
    }

    public static Map<String, Object> routeMainMenuOption(String selectedOption) {
        configure();
        return NativeLoaderUiActionRouter.routeMainMenuOption(selectedOption);
    }

    public static Map<String, Object> routeHudUpdate(Map<String, Object> state) {
        configure();
        return NativeLoaderUiActionRouter.routeHudUpdate(state);
    }

    public static Map<String, Object> routeCameraCinematicFrame(Map<String, Object> state) {
        configure();
        return NativeLoaderUiActionRouter.routeCameraCinematicFrame(state);
    }

    public static Map<String, Object> routeMissionLogUpdate(Map<String, Object> state) {
        configure();
        return NativeLoaderUiActionRouter.routeMissionLogUpdate(state);
    }

    public static Map<String, Object> activate(String mode, Map<String, Object> state) {
        configure();
        return NativeLoaderUiActionRouter.activate(mode, state);
    }
}
