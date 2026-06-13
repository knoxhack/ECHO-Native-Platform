package dev.echo.nativeplatform.bootstrap;

import dev.echo.nativeplatform.loader.NativeLoaderTheme;
import dev.echo.nativeplatform.loader.NativeLoaderThemeResolver;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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
        Map<String, Object> resolverScenarios = resolverScenarios();
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
        smoke.put("resolverScenarios", resolverScenarios);
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
                && "ECHO>".equals(tokens.get("terminal.prompt"))
                && Boolean.TRUE.equals(resolverScenarios.get("passed")));
        return Map.copyOf(smoke);
    }

    private static Map<String, Object> resolverScenarios() {
        String previousMode = System.getProperty(NativeLoaderThemeResolver.THEME_MODE_PROPERTY);
        String previousId = System.getProperty(NativeLoaderThemeResolver.THEME_ID_PROPERTY);
        Map<String, Object> scenarios = new LinkedHashMap<>();
        try {
            System.setProperty(NativeLoaderThemeResolver.THEME_MODE_PROPERTY, "loader_default");
            System.setProperty(NativeLoaderThemeResolver.THEME_ID_PROPERTY, "echo_native:missing_theme");
            NativeLoaderThemeResolver.configure(
                    () -> "modpack",
                    () -> "qa_console",
                    NativeLoaderThemeResolver.class::getClassLoader
            );
            NativeLoaderTheme invalidId = NativeLoaderThemeResolver.refresh();
            boolean invalidFallsBack = NativeLoaderTheme.BUILT_IN_ID.equals(invalidId.id())
                    && invalidId.fallbackUsed()
                    && "loader_default".equals(invalidId.mode().id())
                    && "built_in_invalid_requested_id".equals(invalidId.source());
            scenarios.put("invalidThemeIdFallsBack", invalidFallsBack);
            scenarios.put("invalidThemeIdEvidence", invalidId.evidence());

            System.setProperty(NativeLoaderThemeResolver.THEME_MODE_PROPERTY, "loader_default");
            System.setProperty(NativeLoaderThemeResolver.THEME_ID_PROPERTY, NativeLoaderTheme.BUILT_IN_ID);
            NativeLoaderThemeResolver.configure(
                    () -> "modpack",
                    () -> "qa_console",
                    NativeLoaderThemeResolver.class::getClassLoader
            );
            NativeLoaderTheme systemOverride = NativeLoaderThemeResolver.refresh();
            boolean systemPropertiesOverrideProfile = NativeLoaderTheme.BUILT_IN_ID.equals(systemOverride.id())
                    && "loader_default".equals(systemOverride.mode().id())
                    && !systemOverride.fallbackUsed();
            scenarios.put("systemPropertiesOverrideProfile", systemPropertiesOverrideProfile);
            scenarios.put("systemPropertyOverrideEvidence", systemOverride.evidence());

            Path root = Files.createTempDirectory("echo-agent5-theme-resource");
            Path resource = root.resolve("data").resolve("echothemecore").resolve("themes").resolve("qa_console.json");
            Files.createDirectories(resource.getParent());
            Files.writeString(resource, """
                    {
                      "id": "echothemecore:qa_console",
                      "display_name": "QA Console",
                      "colors": {
                        "background": "#010203",
                        "panel": "#102030CC",
                        "panel_alt": "#203040AA",
                        "border": "#11AAFF",
                        "border_soft": "#225577",
                        "text": "#F1FFFF",
                        "muted_text": "#88AACC",
                        "accent": "#44EEFF",
                        "warning": "#FFCC44",
                        "success": "#66FF99",
                        "selection": "#FF55DD",
                        "primary": "#11AAFF"
                      }
                    }
                    """, StandardCharsets.UTF_8);
            try (URLClassLoader loader = new URLClassLoader(
                    new URL[]{root.toUri().toURL()},
                    NativeLoaderThemeResolver.class.getClassLoader()
            )) {
                System.setProperty(NativeLoaderThemeResolver.THEME_MODE_PROPERTY, "loader_default");
                System.setProperty(NativeLoaderThemeResolver.THEME_ID_PROPERTY, "qa_console");
                NativeLoaderThemeResolver.configure(
                        () -> "modpack",
                        () -> "qa_console",
                        () -> loader
                );
                NativeLoaderTheme loaderDefaultResource = NativeLoaderThemeResolver.refresh();
                boolean loaderDefaultIgnoresThemeCore = NativeLoaderTheme.BUILT_IN_ID.equals(loaderDefaultResource.id())
                        && "loader_default".equals(loaderDefaultResource.mode().id())
                        && loaderDefaultResource.fallbackUsed()
                        && "built_in_invalid_requested_id".equals(loaderDefaultResource.source());
                scenarios.put("loaderDefaultIgnoresThemeCoreResource", loaderDefaultIgnoresThemeCore);
                scenarios.put("loaderDefaultResourceEvidence", loaderDefaultResource.evidence());

                System.setProperty(NativeLoaderThemeResolver.THEME_MODE_PROPERTY, "modpack");
                System.setProperty(NativeLoaderThemeResolver.THEME_ID_PROPERTY, "qa_console");
                NativeLoaderThemeResolver.configure(
                        () -> "loader_default",
                        () -> "",
                        () -> loader
                );
                NativeLoaderTheme resourceTheme = NativeLoaderThemeResolver.refresh();
                boolean resourceThemeUsed = "echothemecore:qa_console".equals(resourceTheme.id())
                        && "modpack".equals(resourceTheme.mode().id())
                        && "themecore_resource".equals(resourceTheme.source())
                        && "themecore".equals(resourceTheme.token("themeFamily"))
                        && "ECHO Native Loader".equals(resourceTheme.token("identityLabel"))
                        && !resourceTheme.fallbackUsed();
                scenarios.put("modpackUsesThemeCoreResource", resourceThemeUsed);
                scenarios.put("themeCoreResourceEvidence", resourceTheme.evidence());
            }
            scenarios.put("passed", invalidFallsBack
                    && systemPropertiesOverrideProfile
                    && Boolean.TRUE.equals(scenarios.get("loaderDefaultIgnoresThemeCoreResource"))
                    && Boolean.TRUE.equals(scenarios.get("modpackUsesThemeCoreResource")));
        } catch (Exception exception) {
            scenarios.put("passed", false);
            scenarios.put("failureKind", exception.getClass().getSimpleName());
            scenarios.put("failureMessage", exception.getMessage() == null ? "" : exception.getMessage());
        } finally {
            restore(NativeLoaderThemeResolver.THEME_MODE_PROPERTY, previousMode);
            restore(NativeLoaderThemeResolver.THEME_ID_PROPERTY, previousId);
            NativeLoaderThemeResolver.configure(
                    () -> "",
                    () -> "",
                    NativeLoaderThemeResolver.class::getClassLoader
            );
        }
        return Map.copyOf(scenarios);
    }

    private static void restore(String key, String value) {
        if (value == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, value);
        }
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
