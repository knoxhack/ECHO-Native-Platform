package dev.echo.nativeplatform.loader;

import dev.echo.nativeplatform.contracts.EchoNativeAddonDescriptor;
import dev.echo.nativeplatform.contracts.EchoNativeDiagnostic;
import dev.echo.nativeplatform.contracts.EchoNativePackProfile;

import java.util.List;

public record EchoNativeScanResult(
        EchoNativePackProfile packProfile,
        List<EchoNativeAddonDescriptor> descriptors,
        List<EchoNativeDiagnostic> diagnostics
) {
}
