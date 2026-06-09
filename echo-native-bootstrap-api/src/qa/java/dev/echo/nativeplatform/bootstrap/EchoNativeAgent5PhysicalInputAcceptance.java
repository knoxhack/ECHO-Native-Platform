package dev.echo.nativeplatform.bootstrap;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class EchoNativeAgent5PhysicalInputAcceptance {
    private EchoNativeAgent5PhysicalInputAcceptance() {
    }

    public static Map<String, Object> assess(
            Map<String, Object> physicalHotkey,
            Map<String, Object> liveSurfaceAcceptance
    ) {
        Map<String, Object> hotkey = physicalHotkey == null ? Map.of() : physicalHotkey;
        Map<String, Object> acceptance = liveSurfaceAcceptance == null ? Map.of() : liveSurfaceAcceptance;
        String key = firstNonBlank(hotkey.get("key"), hotkey.get("hotkey"));
        String surface = normalizeSurface(hotkey.get("surface"));
        String expectedSurface = normalizeSurface(acceptance.get("expectedMode"));
        String acceptedSurface = normalizeSurface(acceptance.get("currentMode"));
        boolean physicalHandled = Boolean.TRUE.equals(hotkey.get("observed"))
                || Boolean.TRUE.equals(hotkey.get("handled"));
        boolean liveAccepted = Boolean.TRUE.equals(acceptance.get("accepted"));
        boolean accepted = physicalHandled
                && liveAccepted
                && !surface.isBlank()
                && (surface.equals(expectedSurface) || surface.equals(acceptedSurface));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("accepted", accepted);
        result.put("physicalHandled", physicalHandled);
        result.put("liveSurfaceAccepted", liveAccepted);
        result.put("key", key);
        result.put("surface", surface);
        result.put("acceptedSurface", acceptedSurface);
        result.put("expectedSurface", expectedSurface);
        result.put("acceptedScreenClass", text(acceptance.get("currentScreenClass")));
        result.put("effect", accepted
                ? "physical_input_acceptance:" + key + "->" + surface
                : "physical_input_acceptance:rejected:" + (surface.isBlank() ? "none" : surface));
        result.put("adapterCoreBridge", true);
        result.put("serviceCodeExecuted", accepted);
        return Map.copyOf(result);
    }

    private static String normalizeSurface(Object value) {
        return text(value).trim().toUpperCase(Locale.ROOT);
    }

    private static String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static String firstNonBlank(Object first, Object second) {
        String firstText = text(first);
        return firstText.isBlank() ? text(second) : firstText;
    }
}
