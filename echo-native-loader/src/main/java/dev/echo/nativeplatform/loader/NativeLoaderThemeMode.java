package dev.echo.nativeplatform.loader;

import java.util.Locale;

public enum NativeLoaderThemeMode {
    LOADER_DEFAULT("loader_default"),
    MODPACK("modpack"),
    OFF("off");

    private final String id;

    NativeLoaderThemeMode(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public static NativeLoaderThemeMode from(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT)
                .replace('-', '_')
                .replace('.', '_');
        return switch (normalized) {
            case "modpack", "theme_core", "themecore", "pack", "pack_theme" -> MODPACK;
            case "off", "vanilla", "vanilla_safe", "disabled" -> OFF;
            default -> LOADER_DEFAULT;
        };
    }
}
