package dev.echo.nativeplatform.contracts;

import java.util.List;
import java.util.Map;

@EchoNativeApiStatus(value = EchoNativeApiStability.INTERNAL, since = "0.1.0-native-beta")
public record EchoNativeAddonRuntimeDescriptorSnapshot(
        String snapshotId,
        boolean complete,
        boolean deterministicOrder,
        boolean descriptorDataOnly,
        int descriptorCount,
        List<String> orderedModuleIds,
        List<Map<String, Object>> descriptors
) {
}
