package dev.echo.nativeplatform.loader;

import java.util.function.Function;

public final class NativeLoaderAdapterCoreGameplayRuntimeActions {
    public static final String SERVICE_ID = "echo.native.adaptercore_gameplay_runtime_actions";

    private NativeLoaderAdapterCoreGameplayRuntimeActions() {
    }

    public static boolean waterBottleUsed(
            Context context,
            Object level,
            Object player,
            Object handOrStack,
            String itemId,
            String path
    ) {
        int hydration = hasAny(path, "clean_water") ? 30
                : hasAny(path, "boiled_water") ? 24
                : hasAny(path, "filtered_water") ? 18
                : 8;
        int nutrition = hasAny(path, "clean_water") ? 3
                : hasAny(path, "boiled_water", "filtered_water") ? 2
                : 1;
        float saturation = hasAny(path, "clean_water") ? 0.45F
                : hasAny(path, "boiled_water") ? 0.35F
                : hasAny(path, "filtered_water") ? 0.25F
                : 0.10F;
        boolean nausea = !hasAny(path, "clean_water", "boiled_water", "filtered_water");
        return NativeLoaderRuntimeHostSupport.invokeForServerPlayer(level, player,
                (serverPlayer, serverLevel) -> waterBottleUsedOnServer(
                        context,
                        serverPlayer,
                        handOrStack,
                        itemId,
                        hydration,
                        nutrition,
                        saturation,
                        nausea),
                context.runtimeHostContext());
    }

    public static boolean crudeFilterUsed(Context context, Object level, Object player, Object handOrStack) {
        return NativeLoaderRuntimeHostSupport.invokeForServerPlayer(level, player,
                (serverPlayer, serverLevel) -> {
                    Object stack = serverItemStack(context, serverPlayer, handOrStack, context.productId("crude_filter"));
                    Object hand = interactionHand(context, handOrStack);
                    if (stack == null || hand == null) {
                        return false;
                    }
                    Class<?> serverPlayerClass = Class.forName(context.runtimeClass("server.level.ServerPlayer"));
                    Class<?> itemStackClass = Class.forName(context.runtimeClass("world.item.ItemStack"));
                    Class<?> handClass = Class.forName(context.runtimeClass("world.InteractionHand"));
                    Class<?> earlyRuntimeClass = Class.forName(context.gameplayClass("early_event_runtime"));
                    Object result = earlyRuntimeClass
                            .getMethod("crudeFilterUsed", serverPlayerClass, itemStackClass, handClass)
                            .invoke(null, serverPlayer, stack, hand);
                    return NativeLoaderRuntimeHostSupport.resultMutated(result);
                },
                context.runtimeHostContext());
    }

    public static boolean handWarmerUsed(Context context, Object level, Object player, Object handOrStack, int warmthDelta) {
        return NativeLoaderRuntimeHostSupport.invokeForServerPlayer(level, player,
                (serverPlayer, serverLevel) -> {
                    Object stack = serverItemStack(context, serverPlayer, handOrStack, context.productId("hand_warmer"));
                    if (stack == null) {
                        return false;
                    }
                    Class<?> serverPlayerClass = Class.forName(context.runtimeClass("server.level.ServerPlayer"));
                    Class<?> itemStackClass = Class.forName(context.runtimeClass("world.item.ItemStack"));
                    Class<?> earlyRuntimeClass = Class.forName(context.gameplayClass("early_event_runtime"));
                    Object result = earlyRuntimeClass
                            .getMethod("handWarmerUsed", serverPlayerClass, itemStackClass, int.class)
                            .invoke(null, serverPlayer, stack, Math.max(1, warmthDelta));
                    return NativeLoaderRuntimeHostSupport.resultMutated(result);
                },
                context.runtimeHostContext());
    }

    public static boolean itemConsumed(Context context, Object level, Object player, Object handOrStack) {
        return NativeLoaderRuntimeHostSupport.invokeForServerPlayer(level, player,
                (serverPlayer, serverLevel) -> {
                    Object stack = serverItemStack(context, serverPlayer, handOrStack, "");
                    if (stack == null) {
                        return false;
                    }
                    Class<?> serverPlayerClass = Class.forName(context.runtimeClass("server.level.ServerPlayer"));
                    Class<?> itemStackClass = Class.forName(context.runtimeClass("world.item.ItemStack"));
                    Class<?> earlyRuntimeClass = Class.forName(context.gameplayClass("early_event_runtime"));
                    Object result = earlyRuntimeClass
                            .getMethod("itemConsumed", serverPlayerClass, itemStackClass)
                            .invoke(null, serverPlayer, stack);
                    return NativeLoaderRuntimeHostSupport.resultMutated(result);
                },
                context.runtimeHostContext());
    }

    public static boolean directRealItemUse(Context context, Object level, Object player, Object handOrStack, String itemId) {
        String resolvedItemId = context.itemResolver().apply(itemId);
        if (resolvedItemId.isBlank()) {
            return false;
        }
        return NativeLoaderRuntimeHostSupport.invokeForServerPlayer(level, player,
                (serverPlayer, serverLevel) -> {
                    Object item = context.registryValue().get("ITEM", resolvedItemId);
                    if (item == null || NativeLoaderGeneratedContentBridge.ITEM_CLASS_NAME.equals(item.getClass().getName())) {
                        return false;
                    }
                    Object hand = interactionHand(context, handOrStack);
                    if (hand == null) {
                        return false;
                    }
                    Class<?> levelClass = Class.forName(context.runtimeClass("world.level.Level"));
                    Class<?> playerClass = Class.forName(context.runtimeClass("world.entity.player.Player"));
                    Class<?> handClass = Class.forName(context.runtimeClass("world.InteractionHand"));
                    Object result = item.getClass()
                            .getMethod("use", levelClass, playerClass, handClass)
                            .invoke(item, serverLevel, serverPlayer, hand);
                    return interactionResultAccepted(result);
                },
                context.runtimeHostContext());
    }

    public static boolean radAwayUsed(Context context, Object level, Object player, String source) {
        return NativeLoaderRuntimeHostSupport.invokeForServerPlayer(level, player,
                (serverPlayer, serverLevel) -> {
                    Class<?> serverPlayerClass = Class.forName(context.runtimeClass("server.level.ServerPlayer"));
                    Class<?> hazardRuntimeClass = Class.forName(context.gameplayClass("hazard_runtime"));
                    Object result = hazardRuntimeClass
                            .getMethod("radAwayUsed", serverPlayerClass, String.class)
                            .invoke(null, serverPlayer, source);
                    return NativeLoaderRuntimeHostSupport.resultMutated(result);
                },
                context.runtimeHostContext());
    }

    public static boolean filterCartridgeUsed(
            Context context,
            Object level,
            Object player,
            String itemId,
            String tierName,
            int tier,
            int refillAmount
    ) {
        return NativeLoaderRuntimeHostSupport.invokeForServerPlayer(level, player,
                (serverPlayer, serverLevel) -> {
                    Class<?> serverPlayerClass = Class.forName(context.runtimeClass("server.level.ServerPlayer"));
                    Class<?> hazardRuntimeClass = Class.forName(context.gameplayClass("hazard_runtime"));
                    Object result = hazardRuntimeClass
                            .getMethod("filterCartridgeUsed", serverPlayerClass,
                                    String.class, String.class, int.class, int.class, String.class)
                            .invoke(null, serverPlayer, itemId, tierName, tier, refillAmount, "native_client_item_use");
                    return NativeLoaderRuntimeHostSupport.resultMutated(result);
                },
                context.runtimeHostContext());
    }

    public static boolean dataLogRecovered(Context context, Object level, Object player, String logType, String title) {
        return NativeLoaderRuntimeHostSupport.invokeForServerPlayer(level, player,
                (serverPlayer, serverLevel) -> {
                    Class<?> serverPlayerClass = Class.forName(context.runtimeClass("server.level.ServerPlayer"));
                    Class<?> explorationRuntimeClass = Class.forName(context.gameplayClass("exploration_runtime"));
                    Object result = explorationRuntimeClass
                            .getMethod("dataLogRecovered", serverPlayerClass, String.class, String.class)
                            .invoke(null, serverPlayer, logType, title);
                    return NativeLoaderRuntimeHostSupport.resultMutated(result);
                },
                context.runtimeHostContext());
    }

    public static boolean deployEntityRoute(Context context, Object level, Object player, Object pos, String source) {
        return NativeLoaderRuntimeHostSupport.invokeForServerPlayer(level, player,
                (serverPlayer, serverLevel) -> {
                    Class<?> serverPlayerClass = Class.forName(context.runtimeClass("server.level.ServerPlayer"));
                    Class<?> blockPosClass = Class.forName(context.runtimeClass("core.BlockPos"));
                    Object routePos = blockPosClass.isInstance(pos) ? pos : optionalMethodValue(serverPlayer, "blockPosition");
                    if (routePos != null && !blockPosClass.isInstance(routePos)) {
                        routePos = null;
                    }
                    Class<?> lateRuntimeClass = Class.forName(context.gameplayClass("late_runtime"));
                    Object result = lateRuntimeClass
                            .getMethod("scoutDroneRoute", serverPlayerClass,
                                    String.class, String.class, blockPosClass, String.class)
                            .invoke(null, serverPlayer, "native_client_deploy_entity", "summon", routePos, source);
                    return NativeLoaderRuntimeHostSupport.resultMutated(result);
                },
                context.runtimeHostContext());
    }

    public static boolean cacheOpened(Context context, Object level, Object player, Object pos, String source) {
        return blockPosAction(context, level, player, pos,
                (serverPlayer, serverLevel, blockPos, serverPlayerClass, blockPosClass) -> {
                    Class<?> explorationRuntimeClass = Class.forName(context.gameplayClass("exploration_runtime"));
                    Object result = explorationRuntimeClass
                            .getMethod("cacheOpened", serverPlayerClass, blockPosClass, String.class)
                            .invoke(null, serverPlayer, blockPos, source);
                    return NativeLoaderRuntimeHostSupport.resultMutated(result);
                });
    }

    public static boolean relayActivated(
            Context context,
            Object level,
            Object player,
            Object pos,
            String relayId,
            String relayType,
            String source
    ) {
        return blockPosAction(context, level, player, pos,
                (serverPlayer, serverLevel, blockPos, serverPlayerClass, blockPosClass) -> {
                    Class<?> lateRuntimeClass = Class.forName(context.gameplayClass("late_runtime"));
                    Object result = lateRuntimeClass
                            .getMethod("relayActivated", serverPlayerClass,
                                    String.class, String.class, blockPosClass, String.class)
                            .invoke(null, serverPlayer, relayId, relayType, blockPos, source);
                    return NativeLoaderRuntimeHostSupport.resultMutated(result);
                });
    }

    public static boolean powerNodeState(
            Context context,
            Object level,
            Object player,
            Object pos,
            boolean active,
            int activeNodeCount,
            String source
    ) {
        return blockPosAction(context, level, player, pos,
                (serverPlayer, serverLevel, blockPos, serverPlayerClass, blockPosClass) -> {
                    Class<?> lateRuntimeClass = Class.forName(context.gameplayClass("late_runtime"));
                    Object result = lateRuntimeClass
                            .getMethod("powerNodeState", serverPlayerClass,
                                    blockPosClass, boolean.class, int.class, String.class)
                            .invoke(null, serverPlayer, blockPos, active, activeNodeCount, source);
                    return NativeLoaderRuntimeHostSupport.resultMutated(result);
                });
    }

    public static boolean nexusCapacitorState(
            Context context,
            Object level,
            Object player,
            Object pos,
            int storedEnergy,
            int capacity,
            String source
    ) {
        return blockPosAction(context, level, player, pos,
                (serverPlayer, serverLevel, blockPos, serverPlayerClass, blockPosClass) -> {
                    Class<?> lateRuntimeClass = Class.forName(context.gameplayClass("late_runtime"));
                    Object result = lateRuntimeClass
                            .getMethod("nexusCapacitorState", serverPlayerClass,
                                    blockPosClass, int.class, int.class, String.class)
                            .invoke(null, serverPlayer, blockPos, storedEnergy, capacity, source);
                    return NativeLoaderRuntimeHostSupport.resultMutated(result);
                });
    }

    public static boolean nexusState(Context context, Object level, Object player, String state, String source) {
        return NativeLoaderRuntimeHostSupport.invokeForServerPlayer(level, player,
                (serverPlayer, serverLevel) -> {
                    Class<?> serverPlayerClass = Class.forName(context.runtimeClass("server.level.ServerPlayer"));
                    Class<?> campaignClass = Class.forName(context.gameplayClass("campaign_data"));
                    Class<?> worldDataClass = Class.forName(context.gameplayClass("world_state_data"));
                    Class<?> lateRuntimeClass = Class.forName(context.gameplayClass("late_runtime"));
                    Object result = lateRuntimeClass
                            .getMethod("nexusState", serverPlayerClass,
                                    campaignClass, worldDataClass, String.class, String.class)
                            .invoke(null, serverPlayer, null, null, state, source);
                    return NativeLoaderRuntimeHostSupport.resultMutated(result);
                },
                context.runtimeHostContext());
    }

    public static boolean shelterSlept(Context context, Object level, Object player, boolean wakeImmediately, boolean updateLevel) {
        return NativeLoaderRuntimeHostSupport.invokeForServerPlayer(level, player,
                (serverPlayer, serverLevel) -> {
                    Class<?> serverPlayerClass = Class.forName(context.runtimeClass("server.level.ServerPlayer"));
                    Class<?> earlyRuntimeClass = Class.forName(context.gameplayClass("early_event_runtime"));
                    Object result = earlyRuntimeClass
                            .getMethod("shelterSlept", serverPlayerClass, boolean.class, boolean.class)
                            .invoke(null, serverPlayer, wakeImmediately, updateLevel);
                    return NativeLoaderRuntimeHostSupport.resultMutated(result);
                },
                context.runtimeHostContext());
    }

    public static boolean dirtyWaterCollected(Context context, Object level, Object player, Object pos) {
        return blockPosAction(context, level, player, pos,
                (serverPlayer, serverLevel, blockPos, serverPlayerClass, blockPosClass) -> {
                    Class<?> earlyRuntimeClass = Class.forName(context.gameplayClass("early_event_runtime"));
                    Object result = earlyRuntimeClass
                            .getMethod("dirtyWaterCollected", serverPlayerClass, blockPosClass)
                            .invoke(null, serverPlayer, blockPos);
                    return NativeLoaderRuntimeHostSupport.resultMutated(result);
                });
    }

    public static boolean waterFiltered(Context context, Object level, Object player, String source) {
        return NativeLoaderRuntimeHostSupport.invokeForServerPlayer(level, player,
                (serverPlayer, serverLevel) -> {
                    Class<?> serverPlayerClass = Class.forName(context.runtimeClass("server.level.ServerPlayer"));
                    Class<?> earlyRuntimeClass = Class.forName(context.gameplayClass("early_event_runtime"));
                    Object result = earlyRuntimeClass
                            .getMethod("waterFiltered", serverPlayerClass, String.class)
                            .invoke(null, serverPlayer, source);
                    return NativeLoaderRuntimeHostSupport.resultMutated(result);
                },
                context.runtimeHostContext());
    }

    public static boolean radiationCleanserUsed(
            Context context,
            Object level,
            Object player,
            Object pos,
            String inputItem,
            String outputItem
    ) {
        return blockPosAction(context, level, player, pos,
                (serverPlayer, serverLevel, blockPos, serverPlayerClass, blockPosClass) -> {
                    Class<?> hazardRuntimeClass = Class.forName(context.gameplayClass("hazard_runtime"));
                    Object result = hazardRuntimeClass
                            .getMethod("radiationCleanserUsed", serverPlayerClass,
                                    blockPosClass, String.class, String.class)
                            .invoke(null, serverPlayer, blockPos, inputItem, outputItem);
                    return NativeLoaderRuntimeHostSupport.resultMutated(result);
                });
    }

    public static boolean medBayUsed(
            Context context,
            Object level,
            Object player,
            Object pos,
            boolean firstTreatment,
            int energyStored,
            int mutationCount
    ) {
        return blockPosAction(context, level, player, pos,
                (serverPlayer, serverLevel, blockPos, serverPlayerClass, blockPosClass) -> {
                    Class<?> hazardRuntimeClass = Class.forName(context.gameplayClass("hazard_runtime"));
                    Object result = hazardRuntimeClass
                            .getMethod("medBayUsed", serverPlayerClass,
                                    blockPosClass, boolean.class, int.class, int.class)
                            .invoke(null, serverPlayer, blockPos, firstTreatment, energyStored, mutationCount);
                    return NativeLoaderRuntimeHostSupport.resultMutated(result);
                });
    }

    public static boolean atmosphericScrubberUsed(
            Context context,
            Object level,
            Object player,
            Object pos,
            float beforeRadiation,
            float afterRadiation,
            int radius
    ) {
        return blockPosAction(context, level, player, pos,
                (serverPlayer, serverLevel, blockPos, serverPlayerClass, blockPosClass) -> {
                    Class<?> hazardRuntimeClass = Class.forName(context.gameplayClass("hazard_runtime"));
                    Object result = hazardRuntimeClass
                            .getMethod("atmosphericScrubberUsed", serverPlayerClass,
                                    blockPosClass, float.class, float.class, int.class)
                            .invoke(null, serverPlayer, blockPos, beforeRadiation, afterRadiation, radius);
                    return NativeLoaderRuntimeHostSupport.resultMutated(result);
                });
    }

    public static boolean labObjective(
            Context context,
            Object level,
            Object player,
            Object pos,
            String objective,
            String source
    ) {
        return blockPosAction(context, level, player, pos,
                (serverPlayer, serverLevel, blockPos, serverPlayerClass, blockPosClass) -> {
                    Class<?> hazardRuntimeClass = Class.forName(context.gameplayClass("hazard_runtime"));
                    Object result = hazardRuntimeClass
                            .getMethod("labObjective", serverPlayerClass,
                                    String.class, blockPosClass, String.class)
                            .invoke(null, serverPlayer, objective, blockPos, source);
                    return NativeLoaderRuntimeHostSupport.resultMutated(result);
                });
    }

    public static boolean researchLabAnalyze(Context context, Object level, Object player, String source) {
        return NativeLoaderRuntimeHostSupport.invokeForServerPlayer(level, player,
                (serverPlayer, serverLevel) -> {
                    Class<?> serverPlayerClass = Class.forName(context.runtimeClass("server.level.ServerPlayer"));
                    Class<?> explorationRuntimeClass = Class.forName(context.gameplayClass("exploration_runtime"));
                    Object result = explorationRuntimeClass
                            .getMethod("analyzeFirstSchematicAtResearchLab", serverPlayerClass, String.class)
                            .invoke(null, serverPlayer, source);
                    return NativeLoaderRuntimeHostSupport.resultMutated(result);
                },
                context.runtimeHostContext());
    }

    public static boolean terminalOpened(Context context, Object level, Object player, String pageId) {
        return NativeLoaderRuntimeHostSupport.invokeForServerPlayer(level, player,
                (serverPlayer, serverLevel) -> {
                    Class<?> serverPlayerClass = Class.forName(context.runtimeClass("server.level.ServerPlayer"));
                    Class<?> triggerRuntimeClass = Class.forName(context.gameplayClass("mission_trigger_runtime"));
                    triggerRuntimeClass
                            .getMethod("terminalOpened", serverPlayerClass, String.class)
                            .invoke(null, serverPlayer, pageId);
                    return true;
                },
                context.runtimeHostContext());
    }

    private static boolean waterBottleUsedOnServer(
            Context context,
            Object serverPlayer,
            Object handOrStack,
            String itemId,
            int hydration,
            int nutrition,
            float saturation,
            boolean nausea
    ) throws ReflectiveOperationException {
        Object stack = serverItemStack(context, serverPlayer, handOrStack, itemId);
        if (stack == null) {
            return false;
        }
        Class<?> serverPlayerClass = Class.forName(context.runtimeClass("server.level.ServerPlayer"));
        Class<?> itemStackClass = Class.forName(context.runtimeClass("world.item.ItemStack"));
        Class<?> earlyRuntimeClass = Class.forName(context.gameplayClass("early_event_runtime"));
        Object result = earlyRuntimeClass
                .getMethod("waterBottleDrunk", serverPlayerClass, itemStackClass,
                        int.class, int.class, float.class, boolean.class)
                .invoke(null, serverPlayer, stack, hydration, nutrition, saturation, nausea);
        return NativeLoaderRuntimeHostSupport.resultMutated(result);
    }

    private static boolean blockPosAction(
            Context context,
            Object level,
            Object player,
            Object pos,
            BlockPosAction action
    ) {
        return NativeLoaderRuntimeHostSupport.invokeForServerPlayer(level, player,
                (serverPlayer, serverLevel) -> {
                    Object blockPos = blockPos(context, serverPlayer, pos);
                    if (blockPos == null) {
                        return false;
                    }
                    Class<?> serverPlayerClass = Class.forName(context.runtimeClass("server.level.ServerPlayer"));
                    Class<?> blockPosClass = Class.forName(context.runtimeClass("core.BlockPos"));
                    return action.run(serverPlayer, serverLevel, blockPos, serverPlayerClass, blockPosClass);
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

    private static Object serverItemStack(Context context, Object serverPlayer, Object handOrStack, String fallbackItemId) {
        try {
            Class<?> itemStackClass = tryClass(context.runtimeClass("world.item.ItemStack"));
            Object stack = context.heldItemStack().get(serverPlayer, handOrStack);
            if (itemStackClass != null && itemStackClass.isInstance(stack)) {
                return stack;
            }
            String resolved = fallbackItemId == null || fallbackItemId.isBlank()
                    ? ""
                    : context.itemResolver().apply(fallbackItemId);
            if (resolved.isBlank()) {
                return null;
            }
            Object item = context.registryValue().get("ITEM", resolved);
            if (item == null || itemStackClass == null) {
                return null;
            }
            Class<?> itemLikeClass = Class.forName(context.runtimeClass("world.level.ItemLike"));
            try {
                return itemStackClass.getConstructor(itemLikeClass, int.class).newInstance(item, 1);
            } catch (NoSuchMethodException ignored) {
                return itemStackClass.getConstructor(itemLikeClass).newInstance(item);
            }
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Object interactionHand(Context context, Object handOrStack) {
        try {
            Class<?> handClass = Class.forName(context.runtimeClass("world.InteractionHand"));
            if (handClass.isInstance(handOrStack)) {
                return handOrStack;
            }
            return handClass.getField("MAIN_HAND").get(null);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static boolean hasAny(String value, String... needles) {
        String haystack = value == null ? "" : value.trim().toLowerCase(java.util.Locale.ROOT);
        for (String needle : needles) {
            if (needle != null && !needle.isBlank() && haystack.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private static boolean interactionResultAccepted(Object result) {
        if (result == null) {
            return false;
        }
        String name = result instanceof Enum<?> enumValue
                ? enumValue.name()
                : String.valueOf(result);
        String normalized = name.toLowerCase(java.util.Locale.ROOT);
        return normalized.contains("success") || normalized.contains("consume");
    }

    private static Class<?> tryClass(String className) {
        try {
            return className == null || className.isBlank() ? null : Class.forName(className);
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
            Function<String, String> gameplayClass,
            Function<String, String> productId,
            Function<String, String> itemResolver,
            RegistryValueResolver registryValue,
            HeldItemStackResolver heldItemStack
    ) {
        private String runtimeClass(String suffix) {
            return runtimeClass.apply(suffix);
        }

        private String gameplayClass(String role) {
            return gameplayClass.apply(role);
        }

        private String productId(String path) {
            return productId.apply(path);
        }
    }

    @FunctionalInterface
    public interface HeldItemStackResolver {
        Object get(Object player, Object handOrStack);
    }

    @FunctionalInterface
    public interface RegistryValueResolver {
        Object get(String registryField, String contentId) throws ReflectiveOperationException;
    }

    @FunctionalInterface
    private interface BlockPosAction {
        boolean run(
                Object serverPlayer,
                Object serverLevel,
                Object blockPos,
                Class<?> serverPlayerClass,
                Class<?> blockPosClass
        ) throws Throwable;
    }
}
