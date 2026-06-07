package dev.echo.nativeplatform.contracts;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@EchoNativeApiStatus(value = EchoNativeApiStability.BETA, since = "0.1.0-native-beta")
public record EchoNativeAddonDescriptor(
        String schema,
        String id,
        String name,
        String version,
        String kind,
        String role,
        String entrypoint,
        EchoNativeRuntimeSide side,
        EchoNativeTrustLevel trustLevel,
        EchoNativeApiStability apiStability,
        boolean official,
        boolean standalone,
        List<String> requires,
        List<String> optional,
        List<String> provides,
        List<String> consumes,
        List<String> transforms,
        Map<String, Object> access,
        Path descriptorPath
) {
}
