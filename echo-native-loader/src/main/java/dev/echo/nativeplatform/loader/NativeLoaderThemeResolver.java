package dev.echo.nativeplatform.loader;

import dev.echo.nativeplatform.diagnostics.EchoNativeJson;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

public final class NativeLoaderThemeResolver {
    public static final String SERVICE_ID = "echo.native.loader_theme_resolver";
    public static final String THEME_MODE_PROPERTY = "echo.native.loaderThemeMode";
    public static final String THEME_ID_PROPERTY = "echo.native.loaderThemeId";

    private static volatile Supplier<String> productThemeMode = () -> "";
    private static volatile Supplier<String> productThemeId = () -> "";
    private static volatile Supplier<ClassLoader> themeClassLoader =
            NativeLoaderThemeResolver.class::getClassLoader;
    private static volatile NativeLoaderTheme lastTheme =
            NativeLoaderTheme.builtIn(NativeLoaderThemeMode.LOADER_DEFAULT, "built_in", false);

    private NativeLoaderThemeResolver() {
    }

    public static void configure(
            Supplier<String> profileThemeMode,
            Supplier<String> profileThemeId,
            Supplier<ClassLoader> classLoader
    ) {
        productThemeMode = profileThemeMode == null ? () -> "" : profileThemeMode;
        productThemeId = profileThemeId == null ? () -> "" : profileThemeId;
        themeClassLoader = classLoader == null ? NativeLoaderThemeResolver.class::getClassLoader : classLoader;
        refresh();
    }

    public static NativeLoaderTheme refresh() {
        NativeLoaderTheme theme = resolve();
        lastTheme = theme;
        return theme;
    }

    public static NativeLoaderTheme activeTheme() {
        return lastTheme == null ? refresh() : lastTheme;
    }

    public static Map<String, Object> activeThemeEvidence() {
        Map<String, Object> evidence = new LinkedHashMap<>(activeTheme().evidence());
        evidence.put("nativeLoaderThemeResolverServiceId", SERVICE_ID);
        evidence.put("nativeLoaderThemeModeProperty", clean(System.getProperty(THEME_MODE_PROPERTY)));
        evidence.put("nativeLoaderThemeIdProperty", clean(System.getProperty(THEME_ID_PROPERTY)));
        return Map.copyOf(evidence);
    }

    private static NativeLoaderTheme resolve() {
        String configuredMode = firstNonBlank(System.getProperty(THEME_MODE_PROPERTY), safe(productThemeMode));
        String configuredId = firstNonBlank(System.getProperty(THEME_ID_PROPERTY), safe(productThemeId));
        NativeLoaderThemeMode mode = NativeLoaderThemeMode.from(configuredMode);
        ClassLoader loader = safeClassLoader();
        boolean themeCoreAvailable = themeCoreAvailable(loader);
        if (mode == NativeLoaderThemeMode.MODPACK) {
            Optional<NativeLoaderTheme> theme = themeCoreTheme(configuredId, loader, themeCoreAvailable);
            if (theme.isPresent()) {
                return theme.get();
            }
            return NativeLoaderTheme.builtIn(mode, "built_in_fallback", themeCoreAvailable).withFallbackUsed(true);
        }
        if (mode == NativeLoaderThemeMode.OFF) {
            return NativeLoaderTheme.builtIn(mode, "off_identity_fallback", themeCoreAvailable);
        }
        boolean fallbackUsed = !configuredId.isBlank()
                && !NativeLoaderTheme.BUILT_IN_ID.equalsIgnoreCase(configuredId.trim());
        return NativeLoaderTheme.builtIn(mode, fallbackUsed ? "built_in_invalid_requested_id" : "built_in",
                themeCoreAvailable).withFallbackUsed(fallbackUsed);
    }

    private static Optional<NativeLoaderTheme> themeCoreTheme(
            String requestedThemeId,
            ClassLoader loader,
            boolean themeCoreAvailable
    ) {
        Optional<NativeLoaderTheme> runtime = themeCoreRuntimeTheme(requestedThemeId, loader, themeCoreAvailable);
        if (runtime.isPresent()) {
            return runtime;
        }
        return themeCoreResourceTheme(requestedThemeId, loader, themeCoreAvailable);
    }

    private static Optional<NativeLoaderTheme> themeCoreRuntimeTheme(
            String requestedThemeId,
            ClassLoader loader,
            boolean themeCoreAvailable
    ) {
        try {
            Class<?> api = Class.forName("com.knoxhack.echothemecore.api.EchoThemeApi", true, loader);
            Object theme = api.getMethod("getClientTheme").invoke(null);
            NativeLoaderTheme resolved = fromThemeCoreObject(theme, themeCoreAvailable);
            if (resolved == null) {
                return Optional.empty();
            }
            if (!requestedThemeId.isBlank() && !themeIdMatches(resolved.id(), requestedThemeId)) {
                return Optional.empty();
            }
            return Optional.of(resolved);
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return Optional.empty();
        }
    }

    private static NativeLoaderTheme fromThemeCoreObject(Object theme, boolean themeCoreAvailable) {
        if (theme == null) {
            return null;
        }
        try {
            String id = invokeString(theme, "id");
            String displayName = invokeString(theme, "displayName");
            Object colorsObject = theme.getClass().getMethod("colors").invoke(theme);
            Map<String, Integer> colors = themeCoreColors(colorsObject);
            Map<String, String> tokens = new LinkedHashMap<>(NativeLoaderTheme
                    .builtIn(NativeLoaderThemeMode.MODPACK, "built_in", themeCoreAvailable)
                    .tokens());
            tokens.put("themeFamily", "themecore");
            tokens.put("mainMenuTitle", "ECHO NATIVE LOADER // " + displayName.toUpperCase(Locale.ROOT));
            return new NativeLoaderTheme(
                    id,
                    displayName,
                    NativeLoaderThemeMode.MODPACK,
                    "themecore_runtime",
                    false,
                    themeCoreAvailable,
                    colors,
                    tokens
            );
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return null;
        }
    }

    private static Map<String, Integer> themeCoreColors(Object colorsObject) throws ReflectiveOperationException {
        NativeLoaderTheme fallback = NativeLoaderTheme.builtIn(
                NativeLoaderThemeMode.MODPACK,
                "built_in",
                true
        );
        Map<String, Integer> colors = new LinkedHashMap<>();
        colors.put("background", color(colorsObject, "background", fallback.color("background")));
        colors.put("panel", withAlpha(color(colorsObject, "panel", fallback.color("panel")), 221));
        colors.put("panelAlt", withAlpha(color(colorsObject, "panelAlt", fallback.color("panelAlt")), 170));
        colors.put("line", color(colorsObject, "border", fallback.color("line")));
        colors.put("lineSoft", color(colorsObject, "borderSoft", fallback.color("lineSoft")));
        colors.put("text", color(colorsObject, "text", fallback.color("text")));
        colors.put("mutedText", color(colorsObject, "mutedText", fallback.color("mutedText")));
        colors.put("accent", color(colorsObject, "accent", fallback.color("accent")));
        colors.put("warning", color(colorsObject, "warning", fallback.color("warning")));
        colors.put("success", color(colorsObject, "success", fallback.color("success")));
        colors.put("selection", color(colorsObject, "selection", fallback.color("selection")));
        colors.put("identity", color(colorsObject, "primary", fallback.color("identity")));
        colors.put("loadingTrack", withAlpha(color(colorsObject, "primary", fallback.color("loadingTrack")), 102));
        colors.put("loadingFill", color(colorsObject, "primary", fallback.color("loadingFill")));
        return Map.copyOf(colors);
    }

    private static Optional<NativeLoaderTheme> themeCoreResourceTheme(
            String requestedThemeId,
            ClassLoader loader,
            boolean themeCoreAvailable
    ) {
        String localId = localThemeId(requestedThemeId);
        if (localId.isBlank()) {
            localId = "echo_platform";
        }
        String resource = "data/echothemecore/themes/" + localId + ".json";
        try (InputStream input = loader.getResourceAsStream(resource)) {
            if (input == null) {
                return Optional.empty();
            }
            String json = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            Map<String, Object> object = EchoNativeJson.asObject(EchoNativeJson.parse(json));
            return Optional.of(fromThemeCoreJson(object, themeCoreAvailable, resource));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    private static NativeLoaderTheme fromThemeCoreJson(
            Map<String, Object> object,
            boolean themeCoreAvailable,
            String resource
    ) {
        NativeLoaderTheme fallback = NativeLoaderTheme.builtIn(
                NativeLoaderThemeMode.MODPACK,
                "built_in",
                themeCoreAvailable
        );
        Map<String, Object> rawColors = EchoNativeJson.asObject(object.get("colors"));
        Map<String, Integer> colors = new LinkedHashMap<>();
        colors.put("background", themeJsonColor(rawColors, "background", fallback.color("background")));
        colors.put("panel", themeJsonColor(rawColors, "panel", fallback.color("panel")));
        colors.put("panelAlt", themeJsonColor(rawColors, "panel_alt", fallback.color("panelAlt")));
        colors.put("line", themeJsonColor(rawColors, "border", fallback.color("line")));
        colors.put("lineSoft", themeJsonColor(rawColors, "border_soft", fallback.color("lineSoft")));
        colors.put("text", themeJsonColor(rawColors, "text", fallback.color("text")));
        colors.put("mutedText", themeJsonColor(rawColors, "muted_text", fallback.color("mutedText")));
        colors.put("accent", themeJsonColor(rawColors, "accent", fallback.color("accent")));
        colors.put("warning", themeJsonColor(rawColors, "warning", fallback.color("warning")));
        colors.put("success", themeJsonColor(rawColors, "success", fallback.color("success")));
        colors.put("selection", themeJsonColor(rawColors, "selection", fallback.color("selection")));
        colors.put("identity", themeJsonColor(rawColors, "primary", fallback.color("identity")));
        colors.put("loadingTrack", withAlpha(themeJsonColor(rawColors, "primary", fallback.color("loadingTrack")), 102));
        colors.put("loadingFill", themeJsonColor(rawColors, "primary", fallback.color("loadingFill")));
        Map<String, String> tokens = new LinkedHashMap<>(fallback.tokens());
        String displayName = string(object.get("display_name"), fallback.displayName());
        tokens.put("themeFamily", "themecore");
        tokens.put("mainMenuTitle", "ECHO NATIVE LOADER // " + displayName.toUpperCase(Locale.ROOT));
        tokens.put("themeResource", resource);
        return new NativeLoaderTheme(
                string(object.get("id"), "echothemecore:" + localThemeId(displayName)),
                displayName,
                NativeLoaderThemeMode.MODPACK,
                "themecore_resource",
                false,
                themeCoreAvailable,
                colors,
                tokens
        );
    }

    private static boolean themeCoreAvailable(ClassLoader loader) {
        try {
            Class.forName("com.knoxhack.echothemecore.api.EchoThemeApi", false, loader);
            return true;
        } catch (ClassNotFoundException | LinkageError ignored) {
            return false;
        }
    }

    private static int color(Object colorsObject, String methodName, int fallback) throws ReflectiveOperationException {
        if (colorsObject == null) {
            return fallback;
        }
        Method method = colorsObject.getClass().getMethod(methodName);
        Object value = method.invoke(colorsObject);
        return value instanceof Number number ? number.intValue() : fallback;
    }

    private static int themeJsonColor(Map<String, Object> colors, String key, int fallback) {
        return parseColor(string(colors.get(key), ""), fallback);
    }

    private static int parseColor(String raw, int fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        String value = raw.trim();
        if (value.startsWith("#")) {
            value = value.substring(1);
        }
        try {
            if (value.length() == 6) {
                return 0xFF000000 | Integer.parseUnsignedInt(value, 16);
            }
            if (value.length() == 8) {
                long rgba = Long.parseLong(value, 16);
                int red = (int) ((rgba >>> 24) & 255);
                int green = (int) ((rgba >>> 16) & 255);
                int blue = (int) ((rgba >>> 8) & 255);
                int alpha = (int) (rgba & 255);
                return ((alpha & 255) << 24) | ((red & 255) << 16) | ((green & 255) << 8) | (blue & 255);
            }
        } catch (NumberFormatException ignored) {
            return fallback;
        }
        return fallback;
    }

    private static int withAlpha(int argb, int alpha) {
        return ((alpha & 255) << 24) | (argb & 0x00FFFFFF);
    }

    private static String invokeString(Object target, String methodName) throws ReflectiveOperationException {
        Object value = target.getClass().getMethod(methodName).invoke(target);
        return string(value, "");
    }

    private static boolean themeIdMatches(String actual, String requested) {
        String safeActual = clean(actual).toLowerCase(Locale.ROOT);
        String safeRequested = clean(requested).toLowerCase(Locale.ROOT);
        return safeActual.equals(safeRequested)
                || localThemeId(safeActual).equals(localThemeId(safeRequested));
    }

    private static String localThemeId(String value) {
        String safe = clean(value).toLowerCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        int colon = safe.indexOf(':');
        return colon >= 0 ? safe.substring(colon + 1) : safe;
    }

    private static String safe(Supplier<String> supplier) {
        try {
            return clean(supplier.get());
        } catch (Throwable ignored) {
            return "";
        }
    }

    private static ClassLoader safeClassLoader() {
        try {
            ClassLoader loader = themeClassLoader.get();
            return loader == null ? NativeLoaderThemeResolver.class.getClassLoader() : loader;
        } catch (Throwable ignored) {
            return NativeLoaderThemeResolver.class.getClassLoader();
        }
    }

    private static String firstNonBlank(String first, String second) {
        String safeFirst = clean(first);
        return safeFirst.isBlank() ? clean(second) : safeFirst;
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private static String string(Object value, String fallback) {
        return value == null ? fallback : String.valueOf(value);
    }
}
