package dev.echo.nativeplatform.contracts;

import java.util.List;
import java.util.Map;

@EchoNativeApiStatus(value = EchoNativeApiStability.INTERNAL, since = "0.1.0-native-beta")
public record EchoNativeLocalRuntimeArtifactInventory(
        String inventoryId,
        boolean inventoryComplete,
        boolean repoLocalOnly,
        boolean downloadsAllowed,
        boolean filesystemMutated,
        int plannedArtifactCount,
        int candidateArtifactCount,
        int approvedCandidateCount,
        int unresolvedArtifactCount,
        List<String> approvedRoots,
        List<Map<String, Object>> artifacts
) {
}
