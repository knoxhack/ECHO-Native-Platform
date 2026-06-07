package dev.echo.nativeplatform.loader;

import java.util.List;
import java.util.Map;

public record NativeLoaderLiveRuntimeAttachment(
        String attachmentId,
        String runtimeKind,
        String runtimeMode,
        boolean liveMinecraftAttached,
        boolean delegateRequired,
        List<String> supportedSurfaces,
        Map<String, Object> evidence
) {
    public static NativeLoaderLiveRuntimeAttachment unattached() {
        return new NativeLoaderLiveRuntimeAttachment(
                "native_loader:unattached",
                "echo_native_first_class_runtime",
                "native_product_runtime",
                false,
                false,
                List.of(),
                Map.of("attachment", "none")
        );
    }

    public NativeLoaderLiveRuntimeAttachment {
        attachmentId = attachmentId == null || attachmentId.isBlank()
                ? "native_loader:unattached"
                : attachmentId.trim();
        runtimeKind = runtimeKind == null || runtimeKind.isBlank()
                ? "echo_native_first_class_runtime"
                : runtimeKind.trim();
        runtimeMode = runtimeMode == null || runtimeMode.isBlank()
                ? "native_product_runtime"
                : runtimeMode.trim();
        supportedSurfaces = supportedSurfaces == null ? List.of() : List.copyOf(supportedSurfaces);
        evidence = evidence == null ? Map.of() : Map.copyOf(evidence);
    }

    public boolean releaseRuntimeTrusted() {
        return !delegateRequired
                && bool(evidence.get("releaseRuntimeTrusted"))
                && ((liveMinecraftAttached && bool(evidence.get("realMinecraftProcess")))
                || (firstClassNativeRuntime() && bool(evidence.get("nativeRuntimeProcess"))));
    }

    public boolean firstClassNativeRuntime() {
        return !"native_loader:unattached".equals(attachmentId)
                && ("echo_native_first_class_runtime".equals(runtimeKind)
                || bool(evidence.get("firstClassNativeRuntime")));
    }

    public boolean nativeRuntimeProcess() {
        return firstClassNativeRuntime() && bool(evidence.get("nativeRuntimeProcess"));
    }

    public Map<String, Object> toReport() {
        return Map.of(
                "attachmentId", attachmentId,
                "runtimeKind", runtimeKind,
                "runtimeMode", runtimeMode,
                "firstClassNativeRuntime", firstClassNativeRuntime(),
                "nativeRuntimeProcess", nativeRuntimeProcess(),
                "liveMinecraftAttached", liveMinecraftAttached,
                "delegateRequired", delegateRequired,
                "releaseRuntimeTrusted", releaseRuntimeTrusted(),
                "supportedSurfaces", supportedSurfaces,
                "evidence", evidence
        );
    }

    private static boolean bool(Object value) {
        return Boolean.TRUE.equals(value) || "true".equalsIgnoreCase(String.valueOf(value));
    }
}
