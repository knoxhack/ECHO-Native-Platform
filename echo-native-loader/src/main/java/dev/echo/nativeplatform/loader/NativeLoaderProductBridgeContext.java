package dev.echo.nativeplatform.loader;

import java.nio.file.Path;
import java.util.Map;

public record NativeLoaderProductBridgeContext(
        String packId,
        String moduleId,
        Path productRoot,
        Path moduleRoot,
        Map<String, Object> descriptorAccess
) {
    public NativeLoaderProductBridgeContext {
        packId = packId == null || packId.isBlank() ? "unknown_pack" : packId.trim();
        moduleId = moduleId == null || moduleId.isBlank() ? "unknown_module" : moduleId.trim();
        productRoot = productRoot == null ? Path.of("").toAbsolutePath().normalize() : productRoot.toAbsolutePath().normalize();
        moduleRoot = moduleRoot == null ? productRoot.resolve("modules").resolve(moduleId).normalize() : moduleRoot.toAbsolutePath().normalize();
        descriptorAccess = descriptorAccess == null ? Map.of() : Map.copyOf(descriptorAccess);
    }
}
