package dev.echo.nativeplatform.loader;

import java.nio.charset.StandardCharsets;

public final class NativeLoaderPackMetadata {
    public static final String SERVICE_ID = "echo.native.pack_metadata";
    public static final int RESOURCE_PACK_MAJOR_FORMAT = 84;
    public static final int RESOURCE_PACK_MINOR_FORMAT = 0;
    public static final int DATA_PACK_MAJOR_FORMAT = 101;
    public static final int DATA_PACK_MINOR_FORMAT = 1;

    private NativeLoaderPackMetadata() {
    }

    public static byte[] resourcePackMcmeta(String description) {
        return packMcmeta(description, RESOURCE_PACK_MAJOR_FORMAT, RESOURCE_PACK_MINOR_FORMAT);
    }

    public static byte[] dataPackMcmeta(String description) {
        return packMcmeta(description, DATA_PACK_MAJOR_FORMAT, DATA_PACK_MINOR_FORMAT);
    }

    public static byte[] packMcmeta(String description, int majorFormat, int minorFormat) {
        return """
                {
                  "pack": {
                    "description": "%s",
                    "min_format": [
                      %d,
                      %d
                    ],
                    "max_format": [
                      %d,
                      %d
                    ]
                  }
                }
                """.formatted(
                escape(description == null ? "" : description),
                majorFormat,
                minorFormat,
                majorFormat,
                minorFormat
        ).getBytes(StandardCharsets.UTF_8);
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}
