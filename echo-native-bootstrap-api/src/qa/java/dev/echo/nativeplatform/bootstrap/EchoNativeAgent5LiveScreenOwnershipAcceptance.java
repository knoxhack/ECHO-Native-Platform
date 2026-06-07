package dev.echo.nativeplatform.bootstrap;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class EchoNativeAgent5LiveScreenOwnershipAcceptance {
    private EchoNativeAgent5LiveScreenOwnershipAcceptance() {
    }

    public static Map<String, Object> assess(
            Map<String, Object> liveSurfaceAcceptance,
            boolean currentScreenIsGeneratedInstance,
            String currentScreenClass,
            String expectedScreenClass,
            String currentMode,
            String expectedMode
    ) {
        Map<String, Object> surface = liveSurfaceAcceptance == null ? Map.of() : liveSurfaceAcceptance;
        String currentClass = currentScreenClass == null ? "" : currentScreenClass;
        String expectedClass = expectedScreenClass == null ? "" : expectedScreenClass;
        String mode = normalize(currentMode);
        String expected = normalize(expectedMode);
        boolean classMatches = currentClass.equals(expectedClass)
                || (!expectedClass.isBlank() && currentClass.endsWith("." + simpleName(expectedClass)));
        boolean accepted = Boolean.TRUE.equals(surface.get("accepted"))
                && currentScreenIsGeneratedInstance
                && classMatches
                && mode.equals(expected);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("accepted", accepted);
        result.put("surfaceAccepted", Boolean.TRUE.equals(surface.get("accepted")));
        result.put("currentScreenIsGeneratedInstance", currentScreenIsGeneratedInstance);
        result.put("currentScreenClass", currentClass);
        result.put("expectedScreenClass", expectedClass);
        result.put("currentMode", mode);
        result.put("expectedMode", expected);
        result.put("effect", accepted
                ? "live_screen_ownership:accepted:" + expected
                : "live_screen_ownership:rejected:" + (expected.isBlank() ? "none" : expected));
        result.put("adapterCoreBridge", true);
        result.put("serviceCodeExecuted", true);
        return Map.copyOf(result);
    }

    public static Map<String, Object> smoke() {
        Map<String, Object> surface = Map.of(
                "accepted", true,
                "currentMode", "TERMINAL",
                "expectedMode", "TERMINAL"
        );
        Map<String, Object> accepted = assess(
                surface,
                true,
                "dev.echo.nativeplatform.generated.EchoNativeDashboardScreen",
                "dev.echo.nativeplatform.generated.EchoNativeDashboardScreen",
                "TERMINAL",
                "TERMINAL"
        );
        Map<String, Object> rejectedNoSurface = assess(
                Map.of("accepted", false),
                true,
                "dev.echo.nativeplatform.generated.EchoNativeDashboardScreen",
                "dev.echo.nativeplatform.generated.EchoNativeDashboardScreen",
                "TERMINAL",
                "TERMINAL"
        );
        Map<String, Object> rejectedWrongInstance = assess(
                surface,
                false,
                "dev.echo.nativeplatform.generated.EchoNativeDashboardScreen",
                "dev.echo.nativeplatform.generated.EchoNativeDashboardScreen",
                "TERMINAL",
                "TERMINAL"
        );
        Map<String, Object> rejectedWrongMode = assess(
                surface,
                true,
                "dev.echo.nativeplatform.generated.EchoNativeDashboardScreen",
                "dev.echo.nativeplatform.generated.EchoNativeDashboardScreen",
                "INDEX",
                "TERMINAL"
        );
        boolean passed = Boolean.TRUE.equals(accepted.get("accepted"))
                && Boolean.FALSE.equals(rejectedNoSurface.get("accepted"))
                && Boolean.FALSE.equals(rejectedWrongInstance.get("accepted"))
                && Boolean.FALSE.equals(rejectedWrongMode.get("accepted"));
        return Map.of(
                "liveScreenOwnershipAcceptanceClass",
                EchoNativeAgent5LiveScreenOwnershipAcceptance.class.getSimpleName(),
                "accepted", accepted,
                "rejectedNoSurface", rejectedNoSurface,
                "rejectedWrongInstance", rejectedWrongInstance,
                "rejectedWrongMode", rejectedWrongMode,
                "passed", passed,
                "adapterCoreBridge", true,
                "serviceCodeExecuted", true
        );
    }

    private static String simpleName(String className) {
        int dot = className.lastIndexOf('.');
        return dot < 0 ? className : className.substring(dot + 1);
    }

    private static String normalize(String value) {
        String normalized = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
        return normalized.isBlank() ? "TERMINAL" : normalized;
    }
}
