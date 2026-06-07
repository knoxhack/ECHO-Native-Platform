package dev.echo.nativeplatform.loader;

import dev.echo.nativeplatform.contracts.EchoNativeModuleLoadResult;
import dev.echo.nativeplatform.diagnostics.EchoNativeJson;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public final class EchoNativeLoadedModuleStateStore {
    public StoredState write(Path directory, EchoNativeModuleLoadResult result) throws IOException {
        Path normalizedDirectory = directory.toAbsolutePath().normalize();
        Files.createDirectories(normalizedDirectory);
        Path path = normalizedDirectory.resolve(result.descriptor().id() + ".json");
        Map<String, Object> state = EchoNativeLoadedModuleState.from(result);
        Files.writeString(path, EchoNativeJson.write(state), StandardCharsets.UTF_8);
        return new StoredState(path, state);
    }

    public record StoredState(Path path, Map<String, Object> state) {
        public String normalizedPath() {
            return path.toString().replace('\\', '/');
        }
    }
}
