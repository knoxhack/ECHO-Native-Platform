package dev.echo.nativeplatform.contracts;

@EchoNativeApiStatus(value = EchoNativeApiStability.INTERNAL, since = "0.1.0-native-beta")
public record EchoNativeLoaderBoundaryState(
        String id,
        String requiredInput,
        boolean verified,
        boolean simulationOnly,
        boolean classloaderCreated,
        boolean processLaunched,
        boolean mutatedState,
        String summary
) {
}
