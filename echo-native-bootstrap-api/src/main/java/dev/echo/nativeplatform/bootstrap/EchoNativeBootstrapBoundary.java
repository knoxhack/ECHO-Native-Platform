package dev.echo.nativeplatform.bootstrap;

public record EchoNativeBootstrapBoundary(
        boolean classloaderCreated,
        boolean gameClassesResolved,
        boolean processLaunched,
        boolean commandExecuted,
        boolean registryInjected,
        boolean filesystemMutated
) {
}
