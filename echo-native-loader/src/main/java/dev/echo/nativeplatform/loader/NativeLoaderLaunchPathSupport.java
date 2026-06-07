package dev.echo.nativeplatform.loader;

import java.nio.file.Path;
import java.util.List;

public final class NativeLoaderLaunchPathSupport {
    public static final String SERVICE_ID = "echo.native.launch_path_support";

    private NativeLoaderLaunchPathSupport() {
    }

    public static Path argumentPath(List<String> args, String name) {
        if (args == null || name == null || name.isBlank()) {
            return null;
        }
        for (int index = 0; index < args.size() - 1; index++) {
            if (name.equals(args.get(index))) {
                return Path.of(args.get(index + 1)).toAbsolutePath().normalize();
            }
        }
        return null;
    }

    public static String sanitizeResourceIdPart(String value) {
        return value == null ? "" : value.replaceAll("[^a-zA-Z0-9_.-]", "_");
    }
}
