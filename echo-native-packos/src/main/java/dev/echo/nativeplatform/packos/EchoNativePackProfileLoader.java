package dev.echo.nativeplatform.packos;

import dev.echo.nativeplatform.contracts.EchoNativePackProfile;
import dev.echo.nativeplatform.diagnostics.EchoNativeJson;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public final class EchoNativePackProfileLoader {
    public EchoNativePackProfile load(Path fixtureRoot) throws IOException {
        Path profilePath = fixtureRoot.resolve("echo.pack.json").normalize();
        Map<String, Object> profile = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(profilePath)));
        Map<String, Object> loader = EchoNativeJson.asObject(profile.get("loader"));
        return new EchoNativePackProfile(
                string(profile.get("schema")),
                string(profile.get("id")),
                string(profile.get("name")),
                string(profile.get("status")),
                string(profile.get("rootModule")),
                string(profile.get("minecraftVersion")),
                string(loader.get("kind")),
                string(loader.get("version")),
                EchoNativeJson.stringList(profile.get("requiredModules")),
                EchoNativeJson.stringList(profile.get("requiredFeatures")),
                EchoNativeJson.stringList(profile.get("optionalFeatures")),
                profilePath
        );
    }

    private static String string(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
