package dev.echo.nativeplatform.bootstrap;

import dev.echo.nativeplatform.loader.NativeLoaderWorldStartupFlow;

import java.util.List;
import java.util.Map;

final class EchoNativeBootstrapWorldStartupFlow {
    private final NativeLoaderWorldStartupFlow delegate;

    EchoNativeBootstrapWorldStartupFlow(String nativeGameDirProperty) {
        this.delegate = new NativeLoaderWorldStartupFlow(nativeGameDirProperty);
    }

    Map<String, Object> apply(String packId, List<String> remainingArgs) {
        return delegate.apply(packId, remainingArgs);
    }

    static boolean blocksHandoff(Map<String, Object> runtimeBridge) {
        return NativeLoaderWorldStartupFlow.blocksHandoff(runtimeBridge);
    }
}
