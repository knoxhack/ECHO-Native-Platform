package dev.echo.nativeplatform.contracts;

@EchoNativeApiStatus(value = EchoNativeApiStability.BETA, since = "0.1.0-native-beta")
public enum EchoNativeLifecyclePhase {
    DISCOVER,
    RESOLVE,
    LOAD_CLASSES,
    CONSTRUCT,
    REGISTER_SERVICES,
    REGISTER_CONTENT,
    COMMON_SETUP,
    CLIENT_SETUP,
    SERVER_SETUP,
    READY,
    SHUTDOWN,

    // Legacy planning phases kept for existing dry-run reports.
    VALIDATE,
    PLAN_SERVICES,
    PLAN_BOOTSTRAP,
    READY_FOR_PHASE_13,
    BLOCKED
}
