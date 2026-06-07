package dev.echo.nativeplatform.bootstrap;

import java.util.Map;

public final class EchoNativeAgent5LiveSurfaceAcceptance {
    private EchoNativeAgent5LiveSurfaceAcceptance() {
    }

    public static Map<String, Object> assess(
            boolean setScreenInvoked,
            String currentScreenClass,
            String expectedScreenClass,
            String currentMode,
            String expectedMode
    ) {
        String screenClass = currentScreenClass == null ? "" : currentScreenClass;
        String expectedClass = expectedScreenClass == null ? "" : expectedScreenClass;
        String mode = normalizeMode(currentMode);
        String expected = normalizeMode(expectedMode);
        boolean accepted = setScreenInvoked
                && !screenClass.isBlank()
                && (screenClass.equals(expectedClass) || screenClass.endsWith("." + simpleName(expectedClass)))
                && expected.equals(mode);
        return Map.of(
                "accepted", accepted,
                "setScreenInvoked", setScreenInvoked,
                "currentScreenClass", screenClass,
                "expectedScreenClass", expectedClass,
                "currentMode", mode,
                "expectedMode", expected,
                "effect", accepted ? "live_surface:accepted:" + expected : "live_surface:rejected:" + expected,
                "adapterCoreBridge", true,
                "serviceCodeExecuted", accepted
        );
    }

    private static String simpleName(String className) {
        int dot = className.lastIndexOf('.');
        return dot < 0 ? className : className.substring(dot + 1);
    }

    private static String normalizeMode(String mode) {
        String normalized = mode == null ? "" : mode.trim().toUpperCase(java.util.Locale.ROOT);
        return normalized.isBlank() ? "TERMINAL" : normalized;
    }
}
