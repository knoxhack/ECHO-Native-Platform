package dev.echo.nativeplatform.testkit;

import java.nio.file.Path;

public final class EchoNativeFixturePaths {
    private EchoNativeFixturePaths() {
    }

    public static Path ashfall() {
        return Path.of("fixtures", "ashfall");
    }

    public static Path brokenPack() {
        return Path.of("fixtures", "broken-pack");
    }
}
