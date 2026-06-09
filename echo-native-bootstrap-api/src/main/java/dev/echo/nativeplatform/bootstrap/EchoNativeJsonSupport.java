package dev.echo.nativeplatform.bootstrap;

import dev.echo.nativeplatform.loader.NativeLoaderJsonSupport;

final class EchoNativeJsonSupport {
    private EchoNativeJsonSupport() {
    }

    static Object parse(String text) {
        return NativeLoaderJsonSupport.parse(text);
    }
}
