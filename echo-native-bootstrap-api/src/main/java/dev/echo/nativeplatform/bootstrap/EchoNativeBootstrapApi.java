package dev.echo.nativeplatform.bootstrap;

/**
 * Marker surface for the Native Loader bootstrap API artifact.
 *
 * <p>The executable bootstrap path lives in {@link EchoNativeBootstrapMain};
 * this type only exposes artifact identity and the static no-op boundary
 * defaults used by callers that need a bootstrap API capability marker.</p>
 */
public final class EchoNativeBootstrapApi {
    public static final String ARTIFACT_ID = "echo-native-bootstrap-api";
    public static final String PHASE = "native_bootstrap_api";

    private EchoNativeBootstrapApi() {
    }

    public static EchoNativeBootstrapBoundary boundary() {
        return new EchoNativeBootstrapBoundary(
                false,
                false,
                false,
                false,
                false,
                false
        );
    }
}
