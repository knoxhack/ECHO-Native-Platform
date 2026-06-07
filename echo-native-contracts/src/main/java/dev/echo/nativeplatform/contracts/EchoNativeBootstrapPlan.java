package dev.echo.nativeplatform.contracts;

import java.util.List;

@EchoNativeApiStatus(value = EchoNativeApiStability.INTERNAL, since = "0.1.0-native-beta")
public record EchoNativeBootstrapPlan(
        String packId,
        String minecraftVersion,
        EchoNativeAccessPolicy accessPolicy,
        List<String> classpathEntries,
        List<String> nativeLibraryEntries,
        List<String> launchArguments,
        List<String> moduleLoadOrder,
        List<EchoNativeServiceDescriptor> services,
        List<EchoNativeDiagnostic> diagnostics
) {
}
