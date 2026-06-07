package dev.echo.nativeplatform.loader;

enum EchoNativeModuleLoadPolicy {
    DEVELOPMENT(false),
    RELEASE(true);

    private final boolean release;

    EchoNativeModuleLoadPolicy(boolean release) {
        this.release = release;
    }

    boolean release() {
        return release;
    }
}
