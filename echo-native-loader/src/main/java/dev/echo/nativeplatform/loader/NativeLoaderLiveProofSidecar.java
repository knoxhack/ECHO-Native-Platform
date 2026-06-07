package dev.echo.nativeplatform.loader;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public final class NativeLoaderLiveProofSidecar {
    public static final String SERVICE_ID = "echo.native.live_proof.sidecar";

    private NativeLoaderLiveProofSidecar() {
    }

    public static Path proofPath(Path markerPath) {
        return markerPath.toAbsolutePath().normalize().getParent().resolve("native-loader-live-proof.json");
    }

    public static Map<String, Object> prepareCurrentRunProof(
            Path markerPath,
            Map<String, Object> proof,
            NativeLoaderLiveProofService liveProof,
            NativeLoaderLiveProofService.Config evidenceConfig
    ) {
        Path proofPath = proofPath(markerPath);
        Map<String, Object> selected = new LinkedHashMap<>(proof == null ? Map.of() : proof);
        selected.put("nativeLoaderLiveProofSidecarServiceId", SERVICE_ID);
        selected.put("proofPath", proofPath.toString());
        selected.put("currentRunTruthGate", true);
        selected.put("preservedExistingCompleteProof", false);
        selected.put("preservedAfterIncompleteCandidate", false);
        selected.put("staleCompleteProofCanMaskCurrentRun", false);
        return liveProof.normalize(selected, evidenceConfig);
    }

    public static Map<String, Object> writeCurrentRunProof(
            Path markerPath,
            Map<String, Object> proof,
            NativeLoaderLiveProofService liveProof,
            NativeLoaderLiveProofService.Config evidenceConfig,
            JsonWriter writer
    ) throws IOException {
        Map<String, Object> selected = prepareCurrentRunProof(markerPath, proof, liveProof, evidenceConfig);
        writer.write(proofPath(markerPath), selected);
        return selected;
    }

    @FunctionalInterface
    public interface JsonWriter {
        void write(Path path, Map<String, Object> value) throws IOException;
    }
}
