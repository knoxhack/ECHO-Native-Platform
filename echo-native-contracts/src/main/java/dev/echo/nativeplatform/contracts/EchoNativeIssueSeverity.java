package dev.echo.nativeplatform.contracts;

@EchoNativeApiStatus(value = EchoNativeApiStability.BETA, since = "0.1.0-native-beta")
public enum EchoNativeIssueSeverity {
    NOTICE,
    WARNING,
    ERROR,
    FATAL
}
