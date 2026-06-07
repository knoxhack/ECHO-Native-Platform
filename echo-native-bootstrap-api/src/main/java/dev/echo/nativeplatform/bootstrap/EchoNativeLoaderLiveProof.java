package dev.echo.nativeplatform.bootstrap;

import dev.echo.nativeplatform.loader.NativeLoaderProductPlayableRuntimeEvidence;

import dev.echo.nativeplatform.loader.NativeLoaderLiveProofService;
import dev.echo.nativeplatform.loader.NativeLoaderLiveProofSidecar;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

final class EchoNativeLoaderLiveProof {
    private final NativeLoaderLiveProofService delegate;

    EchoNativeLoaderLiveProof(String playableRuntimeKey, List<String> requiredLiveMutationSurfaces) {
        this.delegate = new NativeLoaderLiveProofService(playableRuntimeKey, requiredLiveMutationSurfaces);
    }

    Map<String, Object> create(
            String realMainClass,
            Map<String, Object> liveClientProbe,
            Map<String, Object> nativeClientUiBridge,
            Map<String, Object> productGameplayBridge,
            Map<String, Object> serviceBridge,
            Map<String, Map<String, Object>> nativeActivations,
            NativeLoaderProductPlayableRuntimeEvidence.Config evidenceConfig,
            Predicate<Map<String, Object>> nativeActivationLoaded
    ) {
        return delegate.create(
                realMainClass,
                liveClientProbe,
                nativeClientUiBridge,
                productGameplayBridge,
                serviceBridge,
                nativeActivations,
                config(evidenceConfig),
                nativeActivationLoaded
        );
    }

    Map<String, Object> normalize(
            Map<String, Object> proof,
            NativeLoaderProductPlayableRuntimeEvidence.Config evidenceConfig
    ) {
        return delegate.normalize(proof, config(evidenceConfig));
    }

    Map<String, Object> writeCurrentRunProof(
            Path markerPath,
            Map<String, Object> proof,
            NativeLoaderProductPlayableRuntimeEvidence.Config evidenceConfig,
            NativeLoaderLiveProofSidecar.JsonWriter writer
    ) throws IOException {
        return NativeLoaderLiveProofSidecar.writeCurrentRunProof(
                markerPath,
                proof,
                delegate,
                config(evidenceConfig),
                writer
        );
    }

    private static NativeLoaderLiveProofService.Config config(
            NativeLoaderProductPlayableRuntimeEvidence.Config evidenceConfig
    ) {
        NativeLoaderProductPlayableRuntimeEvidence.Config safe =
                NativeLoaderProductPlayableRuntimeEvidence.Config.safe(evidenceConfig);
        return new NativeLoaderLiveProofService.Config(
                safe.adapterCoreServiceId(),
                safe.namespace(),
                safe.nativeLoaderBackendClass(),
                safe.nativeLoaderRuntimeLane(),
                safe.nativeMinecraftRuntimeHostClass(),
                safe.nativeMinecraftRuntimeHostId(),
                safe.compatibilityDelegateClass(),
                safe.compatibilityDelegateId(),
                safe.hudLedgerTarget()
        );
    }
}
