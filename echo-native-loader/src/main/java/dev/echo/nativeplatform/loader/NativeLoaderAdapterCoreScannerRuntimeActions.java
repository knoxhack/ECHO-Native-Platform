package dev.echo.nativeplatform.loader;

import java.util.function.Function;

public final class NativeLoaderAdapterCoreScannerRuntimeActions {
    public static final String SERVICE_ID = "echo.native.adaptercore_scanner_runtime_actions";

    private NativeLoaderAdapterCoreScannerRuntimeActions() {
    }

    public static boolean use(Context context, Object level, Object player, String source, boolean deepScan) {
        return NativeLoaderRuntimeHostSupport.resultMutated(useResult(context, level, player, source, deepScan));
    }

    public static Object useResult(Context context, Object level, Object player, String source, boolean deepScan) {
        Object serverPlayer = NativeLoaderRuntimeHostSupport.serverPlayer(player, context.runtimeHostContext());
        if (serverPlayer == null) {
            return null;
        }
        Object serverLevel = NativeLoaderRuntimeHostSupport.serverLevel(level, serverPlayer, context.runtimeHostContext());
        if (serverLevel == null) {
            return null;
        }
        Object server = NativeLoaderRuntimeHostSupport.server(player, serverPlayer, context.runtimeHostContext());
        Object[] result = new Object[1];
        NativeLoaderRuntimeHostSupport.invokeOnServer(server, () -> {
            result[0] = useOnServer(context, serverPlayer, source, deepScan);
            return NativeLoaderRuntimeHostSupport.resultMutated(result[0]);
        });
        return result[0];
    }

    private static Object useOnServer(Context context, Object serverPlayer, String source, boolean deepScan)
            throws ReflectiveOperationException {
        Class<?> serverPlayerClass = Class.forName(context.runtimeClass("server.level.ServerPlayer"));
        Class<?> scannerServiceClass = Class.forName(context.gameplayClass("poi_scanner_service"));
        Class<?> scanHitClass = Class.forName(context.gameplayClass("poi_scan_hit"));
        Object hit = scannerServiceClass.getMethod("scan", serverPlayerClass).invoke(null, serverPlayer);
        Class<?> explorationRuntimeClass = Class.forName(context.gameplayClass("exploration_runtime"));
        Class<?> interactionHandClass = Class.forName(context.runtimeClass("world.InteractionHand"));
        Object mainHand = interactionHandClass.getField("MAIN_HAND").get(null);
        return explorationRuntimeClass
                .getMethod(
                        "portableScannerUsed",
                        serverPlayerClass,
                        scanHitClass,
                        String.class,
                        boolean.class,
                        interactionHandClass,
                        int.class,
                        boolean.class,
                        int.class)
                .invoke(
                        null,
                        serverPlayer,
                        hit,
                        source,
                        deepScan,
                        mainHand,
                        deepScan ? 3 : 1,
                        deepScan,
                        deepScan ? 2 : 0);
    }

    public record Context(
            NativeLoaderRuntimeHostSupport.Context runtimeHostContext,
            Function<String, String> runtimeClass,
            Function<String, String> gameplayClass
    ) {
        private String runtimeClass(String suffix) {
            return runtimeClass.apply(suffix);
        }

        private String gameplayClass(String role) {
            return gameplayClass.apply(role);
        }
    }
}
