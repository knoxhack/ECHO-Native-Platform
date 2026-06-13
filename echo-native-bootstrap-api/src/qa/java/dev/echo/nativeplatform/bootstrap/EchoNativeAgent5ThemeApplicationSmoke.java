package dev.echo.nativeplatform.bootstrap;

import dev.echo.nativeplatform.loader.NativeLoaderTheme;
import dev.echo.nativeplatform.loader.NativeLoaderThemeResolver;

import java.util.LinkedHashMap;
import java.util.Map;

public final class EchoNativeAgent5ThemeApplicationSmoke {
    private EchoNativeAgent5ThemeApplicationSmoke() {
    }

    public static Map<String, Object> capture(String packId, int moduleCount, int itemCount, int missionCount, int regionCount) {
        Map<String, Object> dataSources = EchoNativeAgent5UiHandlerRegistry.dataSources();
        Map<String, Object> settings = object(dataSources.get("settings"));
        Map<String, Object> terminal = object(dataSources.get("terminal"));
        Map<String, Object> settingsSurface = EchoNativeAgent5ModuleSurfaceRenderers.renderSettings(Map.of(), dataSources);
        Map<String, Object> terminalSurface = EchoNativeAgent5ModuleSurfaceRenderers.renderTerminal(Map.of(
                "focusedControl", "terminal:input",
                "mouseRouted", true,
                "terminalBuffer", terminal.get("command"),
                "terminalOutput", terminal.get("readyLine"),
                "terminalCommandExecuted", true
        ), dataSources);
        Map<String, Object> hostModel = EchoNativeAgent5ScreenHostModel.render(
                "SETTINGS",
                Map.of(),
                packId,
                moduleCount,
                itemCount,
                missionCount,
                regionCount
        );
        NativeLoaderTheme nativeTheme = NativeLoaderThemeResolver.activeTheme();
        Map<String, String> tokens = Map.of(
                "accentColor", NativeLoaderTheme.hex(nativeTheme.color("accent")),
                "warningColor", NativeLoaderTheme.hex(nativeTheme.color("warning")),
                "density", nativeTheme.token("density"),
                "terminal.prompt", nativeTheme.token("consolePrompt")
        );
        String themeId = String.valueOf(settings.get("theme"));
        Map<String, Object> smoke = new LinkedHashMap<>();
        smoke.put("themeApplicationSmokeClass", EchoNativeAgent5ThemeApplicationSmoke.class.getSimpleName());
        smoke.put("themeId", themeId);
        smoke.put("nativeLoaderThemeId", nativeTheme.id());
        smoke.put("nativeLoaderThemeMode", nativeTheme.mode().id());
        smoke.put("nativeLoaderThemeSource", nativeTheme.source());
        smoke.put("nativeLoaderThemeFallbackUsed", nativeTheme.fallbackUsed());
        smoke.put("nativeLoaderThemeCoreAvailable", nativeTheme.themeCoreAvailable());
        smoke.put("settingsProfile", settings.get("profile"));
        smoke.put("inputMode", settings.get("inputMode"));
        smoke.put("tokens", tokens);
        smoke.put("settingsSurfaceRenderer", settingsSurface.get("moduleRendererClass"));
        smoke.put("terminalSurfaceRenderer", terminalSurface.get("moduleRendererClass"));
        smoke.put("settingsSurfaceLines", settingsSurface.get("lines"));
        smoke.put("terminalSurfaceLines", terminalSurface.get("lines"));
        smoke.put("hostHeaderLines", hostModel.get("headerLines"));
        smoke.put("adapterCoreBridge", true);
        smoke.put("serviceCodeExecuted", true);
        smoke.put("passed", NativeLoaderTheme.BUILT_IN_ID.equals(nativeTheme.id())
                && "ashfall-accessible".equals(settings.get("profile"))
                && lines(settingsSurface).stream()
                .anyMatch(line -> line.contains("Settings: profile ashfall-accessible"))
                && lines(settingsSurface).stream().anyMatch(line -> line.contains("Theme: ashfall-agent5"))
                && lines(terminalSurface).stream()
                .anyMatch(line -> line.contains(EchoNativeAgent5UiExpectedValues.terminalOutput()))
                && strings(hostModel.get("headerLines")).stream().anyMatch(line -> line.contains("Pack: " + packId))
                && "ECHO>".equals(tokens.get("terminal.prompt")));
        return Map.copyOf(smoke);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> object(Object value) {
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Map.of();
    }

    @SuppressWarnings("unchecked")
    private static java.util.List<String> lines(Map<String, Object> model) {
        Object value = model.get("lines");
        if (value == null) {
            value = model.get("surfaceLines");
        }
        if (value == null) {
            value = model.get("headerLines");
        }
        if (value instanceof java.util.List<?> list) {
            return (java.util.List<String>) list;
        }
        return java.util.List.of();
    }

    @SuppressWarnings("unchecked")
    private static java.util.List<String> strings(Object value) {
        if (value instanceof java.util.List<?> list) {
            return (java.util.List<String>) list;
        }
        return java.util.List.of();
    }
}
