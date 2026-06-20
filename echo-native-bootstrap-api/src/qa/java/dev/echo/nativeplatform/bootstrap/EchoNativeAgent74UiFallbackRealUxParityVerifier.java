package dev.echo.nativeplatform.bootstrap;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoNativeAgent74UiFallbackRealUxParityVerifier {
    private EchoNativeAgent74UiFallbackRealUxParityVerifier() {
    }

    public static void main(String[] args) {
        Map<String, Object> contract = EchoNativeLiveUiBridge.contractSnapshot();
        Map<String, Object> dataSources = EchoNativeAgent5UiHandlerRegistry.dataSources();

        require(Boolean.TRUE.equals(contract.get("adapterCoreBridge")),
                "Agent 74 UI fallback audit requires the AdapterCore-backed native UI contract.");
        require(Boolean.FALSE.equals(contract.get("standaloneDuplicateGameplaySystem")),
                "Agent 74 UI fallback audit must not validate a standalone duplicate gameplay system.");

        Map<String, Object> terminal = requireTerminal(dataSources);
        Map<String, Object> index = requireIndex(dataSources);
        Map<String, Object> lens = requireLens(dataSources);
        Map<String, Object> hud = requireHud(dataSources);

        List<Map<String, Object>> transcript = List.of(terminal, index, lens, hud);
        require(transcript.stream().allMatch(step -> Boolean.TRUE.equals(step.get("opened"))),
                "Terminal, Index, Lens, and HUD fallback surfaces must open.");
        require(transcript.stream().allMatch(step -> Boolean.TRUE.equals(step.get("executed"))),
                "Terminal, Index, Lens, and HUD fallback actions must execute.");
        require(transcript.stream().allMatch(step -> Boolean.TRUE.equals(step.get("adapterCoreBridge"))),
                "Terminal, Index, Lens, and HUD fallback steps must carry AdapterCore bridge evidence.");

        System.out.println("agent74 ui fallback real ux parity PASS audited=Terminal,Index,Lens,HUD"
                + " nativeHandlers=4 nativeSurfaces=4"
                + " standaloneReference=EchoRuntimeAgent5UiParitySmokeHarness"
                + " liveMinecraftHooks=not_attached");
    }

    private static Map<String, Object> requireTerminal(Map<String, Object> dataSources) {
        Map<String, Object> terminalData = object(dataSources.get("terminal"));
        String command = String.valueOf(terminalData.get("command"));
        Map<String, Object> handler = EchoNativeAgent5UiHandlerRegistry.executeTerminal(command);
        require(Boolean.TRUE.equals(handler.get("handled")), "Terminal status command must execute.");
        require(EchoNativeAgent5UiExpectedValues.terminalOutput().equals(handler.get("output")),
                "Terminal command output must match the terminal page ready line.");

        Map<String, Object> surface = EchoNativeAgent5UiHandlerRegistry.renderSurface("TERMINAL", Map.of(
                "focusedControl", "terminal:input",
                "mouseRouted", true,
                "terminalBuffer", command,
                "terminalOutput", String.valueOf(handler.get("output")),
                "terminalCommandExecuted", true
        ));
        requireSurface(surface, "EchoNativeTerminalSurfaceRenderer",
                command + " -> " + EchoNativeAgent5UiExpectedValues.terminalOutput());
        return step("Terminal", "echoterminal:terminal", handler, surface);
    }

    private static Map<String, Object> requireIndex(Map<String, Object> dataSources) {
        Map<String, Object> indexData = object(dataSources.get("index"));
        String query = String.valueOf(indexData.get("query"));
        Map<String, Object> handler = EchoNativeAgent5UiHandlerRegistry.searchIndex(query);
        require(Boolean.TRUE.equals(handler.get("handled")), "Index search must execute.");
        require(EchoNativeAgent5UiExpectedValues.indexSearchOutput().equals(handler.get("output")),
                "Index search output must match source-backed index entry data.");

        Map<String, Object> surface = EchoNativeAgent5UiHandlerRegistry.renderSurface("INDEX", Map.of(
                "focusedControl", "index:search",
                "mouseRouted", true,
                "indexBuffer", query,
                "indexOutput", String.valueOf(handler.get("output")),
                "indexSearchExecuted", true
        ));
        requireSurface(surface, "EchoNativeIndexSurfaceRenderer",
                query + " -> " + EchoNativeAgent5UiExpectedValues.indexSearchOutput());
        return step("Index", "echoindex:index", handler, surface);
    }

    private static Map<String, Object> requireLens(Map<String, Object> dataSources) {
        Map<String, Object> lensData = object(dataSources.get("lens"));
        String target = String.valueOf(lensData.get("target"));
        Map<String, Object> handler = EchoNativeAgent5UiHandlerRegistry.scanLens(target);
        require(Boolean.TRUE.equals(handler.get("handled")), "Lens scan must execute.");
        require(String.valueOf(handler.get("output")).contains(EchoNativeAgent5UiExpectedValues.lensOutput()),
                "Lens scan output must match source-backed scan profile data.");

        Map<String, Object> surface = EchoNativeAgent5UiHandlerRegistry.renderSurface("LENS", Map.of(
                "focusedControl", "lens:scan",
                "initialFocusRouted", true,
                "lensOutput", String.valueOf(handler.get("output")),
                "lensScanExecuted", true
        ));
        requireSurface(surface, "EchoNativeLensSurfaceRenderer",
                EchoNativeAgent5UiExpectedValues.lensOutput());
        require(lines(surface).stream().anyMatch(line -> line.contains("scan locked ->")),
                "EchoNativeLensSurfaceRenderer must render an executed Lens scan line.");
        return step("Lens", "echolens:lens", handler, surface);
    }

    private static Map<String, Object> requireHud(Map<String, Object> dataSources) {
        Map<String, Object> hudData = object(dataSources.get("hud"));
        Map<String, Object> handler = EchoNativeAgent5UiHandlerRegistry.renderHud();
        require(Boolean.TRUE.equals(handler.get("handled")), "HUD render handler must execute.");
        require(String.valueOf(handler.get("output")).contains(String.valueOf(hudData.get("hazard"))),
                "HUD output must include the reference hazard line.");

        Map<String, Object> surface = EchoNativeAgent5UiHandlerRegistry.renderSurface("HUD", Map.of(
                "hudHealth", hudData.get("health"),
                "hudHazard", hudData.get("hazard"),
                "hudMission", hudData.get("mission"),
                "hudUpdateOutput", String.valueOf(handler.get("output"))
        ));
        requireSurface(surface, "EchoNativeHudSurfaceRenderer", EchoNativeAgent5UiExpectedValues.hudLineToken());
        return step("HUD", "echohudcore:hud", handler, surface);
    }

    private static void requireSurface(Map<String, Object> surface, String rendererClass, String expectedLine) {
        require(Boolean.TRUE.equals(surface.get("serviceCodeExecuted")),
                rendererClass + " must execute service code.");
        require(rendererClass.equals(surface.get("moduleRendererClass")),
                "Surface must use " + rendererClass + ".");
        require(lines(surface).stream().anyMatch(line -> line.contains(expectedLine)),
                rendererClass + " must render expected UX line: " + expectedLine);
    }

    private static Map<String, Object> step(
            String surfaceName,
            String surfaceId,
            Map<String, Object> handler,
            Map<String, Object> surface
    ) {
        Map<String, Object> step = new LinkedHashMap<>();
        step.put("surface", surfaceName);
        step.put("surfaceId", surfaceId);
        step.put("opened", Boolean.TRUE.equals(surface.get("serviceCodeExecuted")));
        step.put("executed", Boolean.TRUE.equals(handler.get("handled")));
        step.put("adapterCoreBridge", Boolean.TRUE.equals(handler.get("adapterCoreBridge"))
                && Boolean.TRUE.equals(surface.get("adapterCoreBridge")));
        step.put("handlerService", handler.get("serviceId"));
        step.put("moduleRendererClass", surface.get("moduleRendererClass"));
        step.put("output", handler.get("output"));
        return Map.copyOf(step);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> object(Object value) {
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Map.of();
    }

    @SuppressWarnings("unchecked")
    private static List<String> lines(Map<String, Object> model) {
        Object value = model.get("lines");
        if (value instanceof List<?> list) {
            return (List<String>) list;
        }
        return List.of();
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
