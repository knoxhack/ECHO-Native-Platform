package dev.echo.nativeplatform.bootstrap;

/**
 * No-window exit target for executable Native Loader pre-window verification.
 */
public final class EchoNativeBootstrapPreflightExitMain {
    private EchoNativeBootstrapPreflightExitMain() {
    }

    public static void main(String[] args) {
        System.out.println("ECHO Native Loader pre-window assertions verified; Minecraft window launch skipped.");
    }
}
