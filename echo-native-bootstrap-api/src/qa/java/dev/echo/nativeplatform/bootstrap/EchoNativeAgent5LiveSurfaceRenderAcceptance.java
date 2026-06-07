package dev.echo.nativeplatform.bootstrap;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class EchoNativeAgent5LiveSurfaceRenderAcceptance {
    private EchoNativeAgent5LiveSurfaceRenderAcceptance() {
    }

    public static Map<String, Object> assess(
            Map<String, Object> liveSurfaceAcceptance,
            Map<String, Object> hostSnapshot
    ) {
        Map<String, Object> acceptance = liveSurfaceAcceptance == null ? Map.of() : liveSurfaceAcceptance;
        Map<String, Object> snapshot = hostSnapshot == null ? Map.of() : hostSnapshot;
        String acceptedSurface = normalizeSurface(acceptance.get("currentMode"));
        String expectedSurface = normalizeSurface(acceptance.get("expectedMode"));
        String renderedSurface = normalizeSurface(snapshot.get("surface"));
        List<String> surfaceLines = strings(snapshot.get("surfaceLines"));
        String screenTitle = text(snapshot.get("screenTitle"));
        String footerLine = text(snapshot.get("footerLine"));
        String moduleRendererClass = text(snapshot.get("moduleRendererClass"));
        boolean accepted = Boolean.TRUE.equals(acceptance.get("accepted"))
                && Boolean.TRUE.equals(snapshot.get("opened"))
                && renderedSurface.equals(expectedSurface)
                && renderedSurface.equals(acceptedSurface)
                && !surfaceLines.isEmpty()
                && screenTitle.contains(renderedSurface)
                && footerLine.contains("M Terminal")
                && !moduleRendererClass.isBlank();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("accepted", accepted);
        result.put("surface", renderedSurface);
        result.put("acceptedSurface", acceptedSurface);
        result.put("expectedSurface", expectedSurface);
        result.put("renderedLineCount", surfaceLines.size());
        result.put("screenTitle", screenTitle);
        result.put("moduleRendererClass", moduleRendererClass);
        result.put("effect", accepted
                ? "live_surface_render:accepted:" + renderedSurface
                : "live_surface_render:rejected:" + (renderedSurface.isBlank() ? "none" : renderedSurface));
        result.put("adapterCoreBridge", true);
        result.put("serviceCodeExecuted", accepted);
        return Map.copyOf(result);
    }

    @SuppressWarnings("unchecked")
    private static List<String> strings(Object value) {
        if (value instanceof List<?> list) {
            return (List<String>) list;
        }
        return List.of();
    }

    private static String normalizeSurface(Object value) {
        return text(value).trim().toUpperCase(Locale.ROOT);
    }

    private static String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
