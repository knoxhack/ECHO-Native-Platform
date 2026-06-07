package dev.echo.nativeplatform.loader;

import dev.echo.nativeplatform.diagnostics.EchoNativeJson;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class NativeLoaderJsonSupport {
    public static final String SERVICE_ID = "echo.native.json_support";
    private static final ConcurrentMap<Path, Object> WRITE_LOCKS = new ConcurrentHashMap<>();

    private NativeLoaderJsonSupport() {
    }

    public static void writeAtomically(Path path, Object value) throws IOException {
        Path normalized = path.toAbsolutePath().normalize();
        Object lock = WRITE_LOCKS.computeIfAbsent(normalized, ignored -> new Object());
        synchronized (lock) {
            writeAtomicallyLocked(normalized, value);
        }
    }

    private static void writeAtomicallyLocked(Path normalized, Object value) throws IOException {
        Files.createDirectories(normalized.getParent());
        Path temp = Files.createTempFile(normalized.getParent(), normalized.getFileName() + ".", ".tmp");
        try {
            Files.writeString(
                    temp,
                    write(value),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE
            );
            try {
                Files.move(temp, normalized, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException ignored) {
                Files.move(temp, normalized, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    public static Object parse(String text) {
        return EchoNativeJson.parse(text);
    }

    public static String write(Object value) {
        return EchoNativeJson.write(value);
    }
}
