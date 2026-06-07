package dev.echo.nativeplatform.contracts;

/**
 * Runtime mode for the native loader.
 *
 * <p>NATIVE_CLIENT: Native Loader is the primary runtime lane. It owns the
 *                   AdapterCore backend, mutation ledger, service bridge, and
 *                   native runtime host state directly. A live Minecraft host
 *                   may be attached as an optional compatibility fallback, but
 *                   it is not required for Native Loader state mutation.</p>
 *
 * <p>NEOFORGE:  Compatibility fallback. Delegates to the live NeoForge/Minecraft runtime.
 *                Every action must use the same canonical ID and event name as NeoForge.
 *                Used when the Native Loader cannot launch or when the user explicitly
 *                selects compatibility mode.</p>
 *
 * <p>STANDALONE: Offline product/runtime harness. Runs without NeoForge and uses
 *                the same native runtime-host state model for headless development.</p>
 *
 * <p>Lane priority: Native Loader (primary) â†’ NeoForge (fallback) â†’ Standalone (harness).</p>
 */
@EchoNativeApiStatus(value = EchoNativeApiStability.BETA, since = "0.1.0-native-beta")
public enum EchoNativeRuntimeMode {
    NATIVE_CLIENT,
    NEOFORGE,
    STANDALONE
}
