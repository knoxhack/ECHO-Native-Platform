package dev.echo.nativeplatform.loader;

import java.util.Map;
import java.util.function.Function;

public final class NativeLoaderAdapterCoreMachineRuntimeActions {
    public static final String SERVICE_ID = "echo.native.adaptercore_machine_runtime_actions";

    private NativeLoaderAdapterCoreMachineRuntimeActions() {
    }

    public static boolean useBlock(Context context, Object level, Object player, Object pos, String machineId) {
        return NativeLoaderRuntimeHostSupport.invokeForServerPlayer(level, player,
                (serverPlayer, serverLevel) -> {
                    Object blockPos = blockPos(context, serverPlayer, pos);
                    if (blockPos == null) {
                        return false;
                    }
                    Class<?> serverPlayerClass = Class.forName(context.runtimeClass("server.level.ServerPlayer"));
                    Class<?> levelClass = Class.forName(context.runtimeClass("world.level.Level"));
                    Class<?> blockPosClass = Class.forName(context.runtimeClass("core.BlockPos"));
                    Class<?> machineHostClass = Class.forName(context.gameplayClass("machine_runtime_host"));
                    Object result = machineHostClass
                            .getMethod("dispatchUseBlock", serverPlayerClass,
                                    levelClass, blockPosClass, String.class)
                            .invoke(null, serverPlayer, serverLevel, blockPos, machineId);
                    return NativeLoaderRuntimeHostSupport.resultMutated(result);
                },
                context.runtimeHostContext());
    }

    public static boolean stateChanged(
            Context context,
            Object level,
            Object player,
            Object pos,
            String machineId,
            int energyStored,
            int progress,
            boolean active,
            String source
    ) {
        return NativeLoaderRuntimeHostSupport.invokeForServerPlayer(level, player,
                (serverPlayer, serverLevel) -> {
                    Object blockPos = blockPos(context, serverPlayer, pos);
                    if (blockPos == null) {
                        return false;
                    }
                    Map<String, Object> state = Map.of(
                            "energyStored", Math.max(0, energyStored),
                            "progress", Math.max(0, progress),
                            "active", active,
                            "source", source == null ? "native_client_machine_state" : source);
                    Class<?> serverPlayerClass = Class.forName(context.runtimeClass("server.level.ServerPlayer"));
                    Class<?> levelClass = Class.forName(context.runtimeClass("world.level.Level"));
                    Class<?> blockPosClass = Class.forName(context.runtimeClass("core.BlockPos"));
                    Class<?> machineHostClass = Class.forName(context.gameplayClass("machine_runtime_host"));
                    Object result = machineHostClass
                            .getMethod("dispatchNativeMachineState", serverPlayerClass,
                                    levelClass, blockPosClass, String.class, Map.class, String.class)
                            .invoke(null, serverPlayer, serverLevel, blockPos, machineId, state, source);
                    return NativeLoaderRuntimeHostSupport.resultMutated(result);
                },
                context.runtimeHostContext());
    }

    public static boolean tick(Context context, Object level, Object player, Object pos, String machineId) {
        return NativeLoaderRuntimeHostSupport.invokeForServerPlayer(level, player,
                (serverPlayer, serverLevel) -> {
                    Object blockPos = blockPos(context, serverPlayer, pos);
                    if (blockPos == null) {
                        return false;
                    }
                    Class<?> serverPlayerClass = Class.forName(context.runtimeClass("server.level.ServerPlayer"));
                    Class<?> levelClass = Class.forName(context.runtimeClass("world.level.Level"));
                    Class<?> blockPosClass = Class.forName(context.runtimeClass("core.BlockPos"));
                    Class<?> machineHostClass = Class.forName(context.gameplayClass("machine_runtime_host"));
                    Object result = machineHostClass
                            .getMethod("dispatchNativeMachineTick", serverPlayerClass,
                                    levelClass, blockPosClass, String.class)
                            .invoke(null, serverPlayer, serverLevel, blockPos, machineId);
                    return NativeLoaderRuntimeHostSupport.resultMutated(result);
                },
                context.runtimeHostContext());
    }

    public static boolean insertItem(
            Context context,
            Object level,
            Object player,
            Object pos,
            String machineId,
            String itemId,
            int count
    ) {
        return machineItem(context, level, player, pos, machineId, itemId, count, "dispatchNativeMachineInsertItem");
    }

    public static boolean extractItem(
            Context context,
            Object level,
            Object player,
            Object pos,
            String machineId,
            String itemId,
            int count
    ) {
        return machineItem(context, level, player, pos, machineId, itemId, count, "dispatchNativeMachineExtractItem");
    }

    public static boolean receiveEnergy(Context context, Object level, Object player, Object pos, String machineId, int amount) {
        return machineEnergy(context, level, player, pos, machineId, amount, "dispatchNativeMachineReceiveEnergy");
    }

    public static boolean extractEnergy(Context context, Object level, Object player, Object pos, String machineId, int amount) {
        return machineEnergy(context, level, player, pos, machineId, amount, "dispatchNativeMachineExtractEnergy");
    }

    private static boolean machineItem(
            Context context,
            Object level,
            Object player,
            Object pos,
            String machineId,
            String itemId,
            int count,
            String methodName
    ) {
        return NativeLoaderRuntimeHostSupport.invokeForServerPlayer(level, player,
                (serverPlayer, serverLevel) -> {
                    Object blockPos = blockPos(context, serverPlayer, pos);
                    if (blockPos == null) {
                        return false;
                    }
                    Class<?> serverPlayerClass = Class.forName(context.runtimeClass("server.level.ServerPlayer"));
                    Class<?> levelClass = Class.forName(context.runtimeClass("world.level.Level"));
                    Class<?> blockPosClass = Class.forName(context.runtimeClass("core.BlockPos"));
                    Class<?> machineHostClass = Class.forName(context.gameplayClass("machine_runtime_host"));
                    Object result = machineHostClass
                            .getMethod(methodName, serverPlayerClass,
                                    levelClass, blockPosClass, String.class, String.class, int.class)
                            .invoke(null, serverPlayer, serverLevel, blockPos, machineId, itemId, Math.max(1, count));
                    return NativeLoaderRuntimeHostSupport.resultMutated(result);
                },
                context.runtimeHostContext());
    }

    private static boolean machineEnergy(
            Context context,
            Object level,
            Object player,
            Object pos,
            String machineId,
            int amount,
            String methodName
    ) {
        return NativeLoaderRuntimeHostSupport.invokeForServerPlayer(level, player,
                (serverPlayer, serverLevel) -> {
                    Object blockPos = blockPos(context, serverPlayer, pos);
                    if (blockPos == null) {
                        return false;
                    }
                    Class<?> serverPlayerClass = Class.forName(context.runtimeClass("server.level.ServerPlayer"));
                    Class<?> levelClass = Class.forName(context.runtimeClass("world.level.Level"));
                    Class<?> blockPosClass = Class.forName(context.runtimeClass("core.BlockPos"));
                    Class<?> machineHostClass = Class.forName(context.gameplayClass("machine_runtime_host"));
                    Object result = machineHostClass
                            .getMethod(methodName, serverPlayerClass, levelClass, blockPosClass, String.class, int.class)
                            .invoke(null, serverPlayer, serverLevel, blockPos, machineId, Math.max(1, amount));
                    return NativeLoaderRuntimeHostSupport.resultMutated(result);
                },
                context.runtimeHostContext());
    }

    private static Object blockPos(Context context, Object serverPlayer, Object pos) {
        try {
            Class<?> blockPosClass = Class.forName(context.runtimeClass("core.BlockPos"));
            if (blockPosClass.isInstance(pos)) {
                return pos;
            }
            Object playerPos = optionalMethodValue(serverPlayer, "blockPosition");
            return blockPosClass.isInstance(playerPos) ? playerPos : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Object optionalMethodValue(Object target, String methodName) {
        try {
            if (target == null || methodName == null || methodName.isBlank()) {
                return null;
            }
            java.lang.reflect.Method method = target.getClass().getMethod(methodName);
            method.trySetAccessible();
            return method.invoke(target);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            return null;
        }
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
