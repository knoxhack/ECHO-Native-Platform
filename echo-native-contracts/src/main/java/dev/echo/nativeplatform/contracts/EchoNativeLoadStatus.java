package dev.echo.nativeplatform.contracts;

@EchoNativeApiStatus(value = EchoNativeApiStability.BETA, since = "0.1.0-native-beta")
public enum EchoNativeLoadStatus {
    DISCOVERED,
    RESOLVED,
    LOADED,
    REGISTERED,
    MUTATED,
    FAILED,
    UNSUPPORTED
}
