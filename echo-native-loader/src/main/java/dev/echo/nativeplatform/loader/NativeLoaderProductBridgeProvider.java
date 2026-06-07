package dev.echo.nativeplatform.loader;

import java.util.Map;

public interface NativeLoaderProductBridgeProvider {
    default NativeLoaderLiveRuntimeAttachment liveRuntimeAttachment(NativeLoaderProductBridgeContext context) {
        return NativeLoaderLiveRuntimeAttachment.unattached();
    }

    default NativeLoaderLiveRuntimeBridge liveRuntimeBridge(NativeLoaderProductBridgeContext context) {
        return NativeLoaderLiveRuntimeBridge.UNATTACHED;
    }

    default NativeLoaderLiveRegistryBridge liveRegistryBridge(NativeLoaderProductBridgeContext context) {
        return NativeLoaderLiveRegistryBridge.UNATTACHED;
    }

    default Map<String, Object> clientAttachmentAssessment(NativeLoaderProductBridgeContext context) {
        return Map.of();
    }

    default NativeLoaderLiveClientBridge liveClientBridge(NativeLoaderProductBridgeContext context) {
        return NativeLoaderLiveClientBridge.UNATTACHED;
    }

    default Map<String, Object> productHookPlan(NativeLoaderProductBridgeContext context) {
        return Map.of();
    }
}
