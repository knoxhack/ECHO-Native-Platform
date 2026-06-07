package dev.echo.nativeplatform.contracts;

import java.util.List;

@EchoNativeApiStatus(value = EchoNativeApiStability.INTERNAL, since = "0.1.0-native-beta")
public record EchoNativeClasspathClassloaderCompatibility(
        String compatibilityId,
        boolean compatible,
        boolean classpathPlannedOnly,
        boolean classloaderCreated,
        boolean productionClassloader,
        boolean resolvesRuntimeClasses,
        boolean processLaunched,
        boolean filesystemMutated,
        long classpathEntryCount,
        List<String> checkedInputs
) {
}
