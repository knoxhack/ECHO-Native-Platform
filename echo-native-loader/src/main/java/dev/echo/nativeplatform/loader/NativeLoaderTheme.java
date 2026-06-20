package dev.echo.nativeplatform.loader;

import java.util.LinkedHashMap;
import java.util.Map;

public record NativeLoaderTheme(
        String id,
        String displayName,
        NativeLoaderThemeMode mode,
        String source,
        boolean fallbackUsed,
        boolean themeCoreAvailable,
        Map<String, Integer> colors,
        Map<String, String> tokens
) {
    public static final String BUILT_IN_ID = "echo_native:loader_blue_console";

    public NativeLoaderTheme {
        id = id == null || id.isBlank() ? BUILT_IN_ID : id.trim();
        displayName = displayName == null || displayName.isBlank() ? "Ashfall Terminal Console" : displayName.trim();
        mode = mode == null ? NativeLoaderThemeMode.LOADER_DEFAULT : mode;
        source = source == null || source.isBlank() ? "built_in" : source.trim();
        colors = colors == null ? Map.of() : Map.copyOf(colors);
        tokens = tokens == null ? Map.of() : Map.copyOf(tokens);
    }

    public static NativeLoaderTheme builtIn(NativeLoaderThemeMode mode, String source, boolean themeCoreAvailable) {
        Map<String, Integer> colors = new LinkedHashMap<>();
        colors.put("background", 0xEE020914);
        colors.put("panel", 0xDD071827);
        colors.put("panelAlt", 0xAA0A2140);
        colors.put("line", 0xFF20D7FF);
        colors.put("lineSoft", 0xFF126D8A);
        colors.put("text", 0xFFE8FBFF);
        colors.put("mutedText", 0xFF76AFC2);
        colors.put("accent", 0xFF00E5FF);
        colors.put("warning", 0xFFFFD166);
        colors.put("success", 0xFF7CFFB2);
        colors.put("selection", 0xFFFF2BD6);
        colors.put("identity", 0xFF20D7FF);
        colors.put("loadingTrack", 0x6600E5FF);
        colors.put("loadingFill", 0xFF00E5FF);

        Map<String, String> tokens = new LinkedHashMap<>();
        tokens.put("identityLabel", "ECHO Ashfall Terminal");
        tokens.put("consolePrompt", "ASHFALL>");
        tokens.put("mainMenuTitle", "ASHFALL TERMINAL // MAIN MENU");
        tokens.put("loadingTitle", "ASHFALL TERMINAL // LOADING");
        tokens.put("worldSetupTitle", "ASHFALL TERMINAL // CREATE SIMULATION");
        tokens.put("themeFamily", "ashfall_terminal");
        tokens.put("density", "compact");
        return new NativeLoaderTheme(BUILT_IN_ID, "Ashfall Terminal Console",
                mode == null ? NativeLoaderThemeMode.LOADER_DEFAULT : mode,
                source == null || source.isBlank() ? "built_in" : source,
                false,
                themeCoreAvailable,
                colors,
                tokens);
    }

    public NativeLoaderTheme withFallbackUsed(boolean used) {
        return new NativeLoaderTheme(id, displayName, mode, source, used, themeCoreAvailable, colors, tokens);
    }

    public int color(String key) {
        Integer value = colors.get(key);
        if (value != null) {
            return value;
        }
        return builtIn(mode, "built_in", themeCoreAvailable).colors().getOrDefault(key, 0xFFFFFFFF);
    }

    public String token(String key) {
        String value = tokens.get(key);
        if (value != null && !value.isBlank()) {
            return value;
        }
        return builtIn(mode, "built_in", themeCoreAvailable).tokens().getOrDefault(key, "");
    }

    public Map<String, Object> evidence() {
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("nativeLoaderThemeId", id);
        evidence.put("nativeLoaderThemeDisplayName", displayName);
        evidence.put("nativeLoaderThemeMode", mode.id());
        evidence.put("nativeLoaderThemeSource", source);
        evidence.put("nativeLoaderThemeFallbackUsed", fallbackUsed);
        evidence.put("nativeLoaderThemeCoreAvailable", themeCoreAvailable);
        evidence.put("nativeLoaderThemeIdentityLabel", token("identityLabel"));
        evidence.put("nativeLoaderThemeConsolePrompt", token("consolePrompt"));
        evidence.put("nativeLoaderThemeColors", colorEvidence());
        evidence.put("nativeLoaderThemeTokens", tokens);
        return Map.copyOf(evidence);
    }

    private Map<String, String> colorEvidence() {
        Map<String, String> values = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : colors.entrySet()) {
            values.put(entry.getKey(), hex(entry.getValue()));
        }
        return Map.copyOf(values);
    }

    public static String hex(int argb) {
        int alpha = (argb >>> 24) & 255;
        int red = (argb >>> 16) & 255;
        int green = (argb >>> 8) & 255;
        int blue = argb & 255;
        return String.format("#%02X%02X%02X%02X", red, green, blue, alpha);
    }
}
