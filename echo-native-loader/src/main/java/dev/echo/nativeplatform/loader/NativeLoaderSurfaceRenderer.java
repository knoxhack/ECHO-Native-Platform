package dev.echo.nativeplatform.loader;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class NativeLoaderSurfaceRenderer {
    public static final String SERVICE_ID = "echo.native.surface_renderer";
    private static final String COMPATIBILITY_RENDERER_CLASS = "EchoNativeAgent5SurfaceRenderer";

    private NativeLoaderSurfaceRenderer() {
    }

    public static Map<String, Object> render(
            String mode,
            Map<String, Object> state,
            Map<String, Object> dataSources
    ) {
        return render(mode, state, dataSources, NativeLoaderModuleSurfaceRenderers.Context.empty());
    }

    public static Map<String, Object> render(
            String mode,
            Map<String, Object> state,
            Map<String, Object> dataSources,
            NativeLoaderModuleSurfaceRenderers.Context context
    ) {
        String normalizedMode = normalizeMode(mode);
        Map<String, Object> source = dataSources == null ? Map.of() : dataSources;
        ArrayList<String> lines = new ArrayList<>();
        String focusPath = focusPath(normalizedMode, string(state, "previousMode", "WIKI"));
        String moduleRendererClass = "";
        if (productActionSurfaceMode(normalizedMode, source)) {
            moduleRendererClass = addModuleLines(
                    lines,
                    NativeLoaderModuleSurfaceRenderers.renderProductActionSurface(state, source, context)
            );
        } else switch (normalizedMode) {
            case "MAIN_MENU" -> moduleRendererClass = addModuleLines(
                    lines,
                    NativeLoaderModuleSurfaceRenderers.renderMainMenu(state, source, context)
            );
            case "TERMINAL" -> moduleRendererClass = addModuleLines(
                    lines,
                    NativeLoaderModuleSurfaceRenderers.renderTerminal(state, source, context)
            );
            case "INDEX" -> moduleRendererClass = addModuleLines(
                    lines,
                    NativeLoaderModuleSurfaceRenderers.renderIndex(state, source, context)
            );
            case "LENS" -> moduleRendererClass = addModuleLines(
                    lines,
                    NativeLoaderModuleSurfaceRenderers.renderLens(state, source, context)
            );
            case "MISSION_LOG" -> moduleRendererClass = addModuleLines(
                    lines,
                    NativeLoaderModuleSurfaceRenderers.renderMissionLog(state, source, context)
            );
            case "SETTINGS" -> moduleRendererClass = addModuleLines(
                    lines,
                    NativeLoaderModuleSurfaceRenderers.renderSettings(state, source, context)
            );
            case "PAUSE" -> moduleRendererClass = addModuleLines(
                    lines,
                    NativeLoaderModuleSurfaceRenderers.renderPause(state, source, context)
            );
            case "RECOVERY" -> moduleRendererClass = addModuleLines(
                    lines,
                    NativeLoaderModuleSurfaceRenderers.renderRecovery(state, source, context)
            );
            case "HOLOMAP" -> moduleRendererClass = addModuleLines(
                    lines,
                    NativeLoaderModuleSurfaceRenderers.renderHolomap(state, source, context)
            );
            case "WIKI" -> moduleRendererClass = addModuleLines(
                    lines,
                    NativeLoaderModuleSurfaceRenderers.renderWiki(state, source, context)
            );
            case "SIGNALOS" -> moduleRendererClass = addModuleLines(
                    lines,
                    NativeLoaderModuleSurfaceRenderers.renderSignalos(state, source, context)
            );
            case "MACHINE" -> moduleRendererClass = addModuleLines(
                    lines,
                    NativeLoaderModuleSurfaceRenderers.renderMachine(state, source, context)
            );
            case "HUD" -> moduleRendererClass = addModuleLines(
                    lines,
                    NativeLoaderModuleSurfaceRenderers.renderHud(state, source, context)
            );
            default -> moduleRendererClass = addModuleLines(
                    lines,
                    NativeLoaderModuleSurfaceRenderers.renderMainMenu(state, source, context)
            );
        }
        Map<String, Object> model = new LinkedHashMap<>();
        model.put("mode", normalizedMode);
        model.put("focusPath", focusPath);
        model.put("lines", List.copyOf(lines));
        model.put("adapterCoreBridge", true);
        model.put("serviceCodeExecuted", true);
        model.put("rendererClass", COMPATIBILITY_RENDERER_CLASS);
        model.put("surfaceRendererClass", NativeLoaderSurfaceRenderer.class.getSimpleName());
        model.put("nativeLoaderSurfaceRendererServiceId", SERVICE_ID);
        model.put("moduleRendererClass", moduleRendererClass);
        return Map.copyOf(model);
    }

    private static String addModuleLines(List<String> lines, Map<String, Object> model) {
        lines.addAll(strings(model.get("lines")));
        return String.valueOf(model.get("moduleRendererClass"));
    }

    private static void renderTerminal(
            List<String> lines,
            Map<String, Object> state,
            Map<String, Object> source,
            String focusPath
    ) {
        Map<String, Object> terminal = object(source.get("terminal"));
        lines.add("Terminal: " + terminal.get("title") + "    Source: " + terminal.get("sourcePath"));
        lines.add("Command: " + terminal.get("command") + "    Prompt: " + terminal.get("prompt"));
        lines.add("Focus: " + focusLabel(focusPath, state)
                + "    Input: " + typedOrPlaceholder(string(state, "terminalBuffer", "")));
        boolean executed = bool(state, "terminalCommandExecuted");
        String output = string(state, "terminalOutput", "awaiting command input");
        lines.add(executed ? terminal.get("command") + " -> " + output + " host=native-client" : output);
    }

    private static void renderIndex(
            List<String> lines,
            Map<String, Object> state,
            Map<String, Object> source,
            String focusPath
    ) {
        Map<String, Object> index = object(source.get("index"));
        lines.add("Index: " + index.get("title") + "    Entries: " + index.get("recordCount"));
        lines.add("Source: " + index.get("sourcePath"));
        lines.add("Focus: " + focusLabel(focusPath, state)
                + "    Query: " + typedOrPlaceholder(string(state, "indexBuffer", "")));
        boolean executed = bool(state, "indexSearchExecuted");
        String output = string(state, "indexOutput", "search field focused");
        lines.add(executed ? index.get("query") + " -> " + output : output);
    }

    private static void renderLens(
            List<String> lines,
            Map<String, Object> state,
            Map<String, Object> source,
            String focusPath
    ) {
        Map<String, Object> lens = object(source.get("lens"));
        lines.add("Lens: " + lens.get("title") + "    Target: " + lens.get("target"));
        lines.add("Profile source: " + lens.get("sourcePath"));
        lines.add("Focus: " + focusLabel(focusPath, state));
        boolean executed = bool(state, "lensScanExecuted");
        String output = string(state, "lensOutput", "target awaiting scan");
        lines.add(executed ? "scan locked -> " + output : output);
    }

    private static void renderMissionLog(List<String> lines, Map<String, Object> source) {
        Map<String, Object> mission = object(source.get("missionLog"));
        lines.add("Mission: " + mission.get("title") + "    Source: " + mission.get("sourcePath"));
        lines.add("Objective: " + mission.get("objective"));
        lines.add("Status: " + mission.get("status") + "    Progress: 25%");
    }

    private static void renderSettings(List<String> lines, Map<String, Object> source) {
        Map<String, Object> settings = object(source.get("settings"));
        lines.add("Settings: profile " + settings.get("profile"));
        lines.add("Theme: " + settings.get("theme") + "    Input: " + settings.get("inputMode"));
        lines.add("HUD scale: " + settings.get("hudScale") + "    Subtitles: enabled");
    }

    private static void renderPause(List<String> lines, Map<String, Object> state) {
        lines.add("Pause: previous screen " + string(state, "previousMode", "WIKI"));
        lines.add("Options: Resume, Settings, Save Snapshot, Quit to Main Menu");
        lines.add("Press Esc to resume the previous Agent 5 screen.");
    }

    private static void renderRecovery(
            List<String> lines,
            Map<String, Object> state,
            Map<String, Object> source,
            String focusPath
    ) {
        Map<String, Object> recovery = object(source.get("deathRecovery"));
        lines.add("Death Recovery: press Enter to recover.");
        lines.add("Recovery point: " + recovery.get("recoveryPoint"));
        lines.add("Focus: " + focusLabel(focusPath, state));
        lines.add(string(state, "recoveryOutput", "Status: WAITING"));
    }

    private static void renderHolomap(List<String> lines, Map<String, Object> state, Map<String, Object> source) {
        Map<String, Object> holomap = object(source.get("holomap"));
        String output = string(state, "holomapOutput", "");
        if (output.isBlank()) {
            output = "";
        }
        lines.add("HoloMap: " + holomap.get("layerName") + "    Layer: " + holomap.get("layer"));
        lines.add("Marker: " + holomap.get("marker") + "    Source: " + holomap.get("sourcePath"));
        lines.add("Waypoint focus: " + holomap.get("focus"));
        lines.add(output);
    }

    private static void renderWiki(List<String> lines, Map<String, Object> state, Map<String, Object> source) {
        Map<String, Object> wiki = object(source.get("wiki"));
        String output = string(state, "wikiOutput", "");
        if (output.isBlank()) {
            output = "";
        }
        lines.add("Wiki: " + wiki.get("page") + "    Category: " + wiki.get("guide"));
        lines.add("Source: " + wiki.get("sourcePath"));
        lines.add(String.valueOf(wiki.get("summary")));
        lines.add(output);
    }

    private static void renderMainMenu(List<String> lines, Map<String, Object> source) {
        Map<String, Object> mainMenu = object(source.get("mainMenu"));
        lines.add("Main Menu: native routes");
        lines.add("Options: " + String.join(", ", strings(mainMenu.get("options"))));
    }

    private static boolean productActionSurfaceMode(String mode, Map<String, Object> source) {
        Map<String, Object> productActionSurface = object(source.get("productActionSurface"));
        return mode.equals(productActionSurface.get("surface"));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> object(Object value) {
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Map.of();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private static String normalizeMode(String mode) {
        String normalized = normalize(mode).toUpperCase(java.util.Locale.ROOT);
        return normalized.isBlank() ? "TERMINAL" : normalized;
    }

    private static String focusPath(String mode, String previousMode) {
        return switch (mode) {
            case "TERMINAL" -> "terminal:input";
            case "INDEX" -> "index:search";
            case "LENS" -> "lens:scan";
            case "RECOVERY" -> "recovery:recover";
            case "PAUSE" -> "pause:resume:" + (previousMode == null || previousMode.isBlank() ? "WIKI" : previousMode);
            default -> mode.toLowerCase(java.util.Locale.ROOT) + ":surface";
        };
    }

    private static String focusLabel(String focusPath, Map<String, Object> state) {
        return focusPath.equals(string(state, "focusedControl", ""))
                && (bool(state, "mouseRouted") || bool(state, "initialFocusRouted"))
                ? focusPath + " ready"
                : focusPath + " waiting";
    }

    private static String typedOrPlaceholder(String value) {
        return value == null || value.isBlank() ? "_" : value;
    }

    private static String string(Map<String, Object> values, String key, String fallback) {
        if (values == null) {
            return fallback;
        }
        Object value = values.get(key);
        return value == null ? fallback : String.valueOf(value);
    }

    private static boolean bool(Map<String, Object> values, String key) {
        return values != null && Boolean.TRUE.equals(values.get(key));
    }

    @SuppressWarnings("unchecked")
    private static List<String> strings(Object value) {
        if (value instanceof List<?> list) {
            return (List<String>) list;
        }
        return List.of();
    }
}
