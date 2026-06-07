package dev.echo.nativeplatform.contracts;

import java.util.List;
import java.util.Map;

@EchoNativeApiStatus(value = EchoNativeApiStability.INTERNAL, since = "0.1.0-native-beta")
public record EchoNativeLocalRuntimeArtifactMap(
        String mapId,
        boolean artifactMappingReady,
        boolean localArtifactManifestPresent,
        boolean downloadsAllowed,
        boolean extractionAllowed,
        boolean filesystemMutated,
        int plannedArtifactCount,
        int mappedArtifactCount,
        int missingArtifactCount,
        String localArtifactManifestPath,
        List<Map<String, Object>> artifacts
) {
}
