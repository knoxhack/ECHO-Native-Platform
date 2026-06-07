package dev.echo.nativeplatform.contracts;

import java.util.List;

@EchoNativeApiStatus(value = EchoNativeApiStability.BETA, since = "0.1.0-native-beta")
public record EchoNativeRegisteredService(
        String moduleId,
        String serviceId,
        String implementationClass,
        List<String> surfaces
) {
}
