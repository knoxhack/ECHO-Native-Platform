package dev.echo.nativeplatform.loader;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

public final class NativeLoaderAdapterCoreRuntimeMutations {
    public static final String SERVICE_ID = "echo.native.adaptercore_runtime_mutations";

    private NativeLoaderAdapterCoreRuntimeMutations() {
    }

    public static boolean removeItem(Context context, Object level, Object player, String itemId, int count) {
        try {
            String resolvedItemId = context.itemResolver().apply(itemId);
            if (resolvedItemId.isBlank()) {
                return false;
            }
            return NativeLoaderRuntimeHostSupport.invokeForServerPlayer(level, player,
                    (serverPlayer, serverLevel) -> removeItemOnServer(
                            context,
                            serverPlayer,
                            serverLevel,
                            resolvedItemId,
                            Math.max(1, count)),
                    context.runtimeHostContext());
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean removeItemOnServer(
            Context context,
            Object serverPlayer,
            Object level,
            String itemId,
            int count
    ) throws ReflectiveOperationException {
        Object runtimeHost = runtimeHost(context, serverPlayer, level);
        if (runtimeHost == null) {
            return false;
        }
        Object inventory = runtimeHost.getClass().getMethod("playerInventory").invoke(runtimeHost);
        java.lang.reflect.Method remove = runtimeHostMethod(inventory, "remove", 4);
        Class<?>[] parameterTypes = remove.getParameterTypes();
        Object playerRef = runtimePlayerRef(context, runtimeHost, parameterTypes[0]);
        Object mutationContext = runtimeHost.getClass()
                .getMethod("context", String.class, String.class, String.class)
                .invoke(runtimeHost,
                        "native_client.remove_item." + compactActionKey(itemId),
                        "EchoNativeRuntimeHost.PlayerInventory",
                        "remove");
        if (!parameterTypes[3].isInstance(mutationContext)) {
            mutationContext = runtimeMutationContext(
                    context,
                    runtimeHost,
                    parameterTypes[3],
                    "native_client.remove_item." + compactActionKey(itemId),
                    "EchoNativeRuntimeHost.PlayerInventory",
                    "remove");
        }
        Object result = remove.invoke(inventory, playerRef, itemId, Math.max(1, count), mutationContext);
        return NativeLoaderRuntimeHostSupport.resultMutated(result);
    }

    public static boolean grantItem(Context context, Object serverPlayer, Object level, String itemId, int count)
            throws ReflectiveOperationException {
        Object runtimeHost = runtimeHost(context, serverPlayer, level);
        if (runtimeHost == null) {
            return false;
        }
        return grantItem(context, runtimeHost, itemId, count);
    }

    public static boolean grantItem(Context context, Object runtimeHost, String itemId, int count)
            throws ReflectiveOperationException {
        return NativeLoaderRuntimeHostSupport.resultMutated(grantItemResult(context, runtimeHost, itemId, count));
    }

    public static Object grantItemResult(Context context, Object runtimeHost, String itemId, int count)
            throws ReflectiveOperationException {
        if (runtimeHost == null) {
            return null;
        }
        Object inventory = runtimeHost.getClass().getMethod("playerInventory").invoke(runtimeHost);
        java.lang.reflect.Method grant = runtimeHostMethod(inventory, "grant", 3);
        Class<?>[] parameterTypes = grant.getParameterTypes();
        Object playerRef = runtimePlayerRef(context, runtimeHost, parameterTypes[0]);
        Object stack = parameterTypes[1]
                .getConstructor(String.class, int.class, Map.class)
                .newInstance(itemId, Math.max(1, count), Map.of());
        Object mutationContext = runtimeMutationContext(
                context,
                runtimeHost,
                parameterTypes[2],
                "native_client.grant_item." + compactActionKey(itemId),
                "EchoNativeRuntimeHost.PlayerInventory",
                "grant");
        return grant.invoke(inventory, playerRef, stack, mutationContext);
    }

    public static Map<String, Object> grantItemEvidence(
            Context context,
            Object level,
            Object player,
            String itemId,
            int count
    ) {
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("attempted", true);
        evidence.put("mutated", false);
        evidence.put("nativeInterface", "EchoNativeRuntimeHost.PlayerInventory");
        evidence.put("nativeMethod", "grant");
        evidence.put("requestedItemId", itemId == null ? "" : itemId);
        evidence.put("requestedCount", Math.max(1, count));
        boolean invoked = NativeLoaderRuntimeHostSupport.invokeForServerPlayer(level, player, (serverPlayer, serverLevel) -> {
            Object runtimeHost = runtimeHost(context, serverPlayer, serverLevel);
            if (runtimeHost == null) {
                NativeLoaderRuntimeHostSupport.putMissingHostEvidence(evidence);
                return false;
            }
            String resolvedItemId = context.itemResolver().apply(itemId);
            if (resolvedItemId.isBlank()) {
                evidence.put("failureKind", "missing_item_id");
                return false;
            }
            evidence.put("itemId", resolvedItemId);
            putRuntimeHostEvidence(context, evidence, runtimeHost);
            try {
                Object result = grantItemResult(context, runtimeHost, resolvedItemId, Math.max(1, count));
                NativeLoaderRuntimeHostSupport.putResultEvidence(evidence, result);
                return NativeLoaderRuntimeHostSupport.resultMutated(result);
            } catch (Throwable failure) {
                NativeLoaderRuntimeHostSupport.putInvocationFailure(evidence, "player_inventory.grant", failure);
                return false;
            }
        }, context.runtimeHostContext());
        evidence.put("serverInvocationCompleted", invoked);
        return Map.copyOf(evidence);
    }

    public static Map<String, Object> placeWorldBlock(
            Context context,
            Object level,
            Object player,
            String blockId
    ) {
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("attempted", true);
        evidence.put("mutated", false);
        evidence.put("nativeInterface", "EchoNativeRuntimeHost.WorldBlocks");
        evidence.put("nativeMethod", "setBlock");
        evidence.put("requestedBlockId", blockId == null ? "" : blockId);
        boolean invoked = NativeLoaderRuntimeHostSupport.invokeForServerPlayer(level, player, (serverPlayer, serverLevel) -> {
            Object runtimeHost = runtimeHost(context, serverPlayer, serverLevel);
            if (runtimeHost == null) {
                NativeLoaderRuntimeHostSupport.putMissingHostEvidence(evidence);
                return false;
            }
            Object center = optionalMethodValue(serverPlayer, "blockPosition");
            if (center == null) {
                evidence.put("failureKind", "missing_player_position");
                return false;
            }
            String resolvedBlockId = context.blockResolver().apply(blockId);
            if (resolvedBlockId.isBlank()) {
                resolvedBlockId = context.blockFallback().apply(blockId);
            }
            if (resolvedBlockId.isBlank()) {
                evidence.put("failureKind", "missing_block_id");
                return false;
            }
            evidence.put("blockId", resolvedBlockId);
            putRuntimeHostEvidence(context, evidence, runtimeHost);
            int x = context.intMethodReader().get(center, "getX") + 1;
            int y = context.intMethodReader().get(center, "getY");
            int z = context.intMethodReader().get(center, "getZ") + 1;
            evidence.put("target", Map.of(
                    "dimensionId", runtimeDimensionId(runtimeHost),
                    "x", x,
                    "y", y,
                    "z", z));
            try {
                if (!"minecraft:air".equals(resolvedBlockId)) {
                    Object resetResult = setBlockResult(context, runtimeHost, x, y, z, "minecraft:air");
                    evidence.put("prePlacementStatus", NativeLoaderRuntimeHostSupport.resultStatus(resetResult));
                    evidence.put("prePlacementMutated", NativeLoaderRuntimeHostSupport.resultMutated(resetResult));
                }
                Object result = setBlockResult(context, runtimeHost, x, y, z, resolvedBlockId);
                NativeLoaderRuntimeHostSupport.putResultEvidence(evidence, result);
                return NativeLoaderRuntimeHostSupport.resultMutated(result);
            } catch (Throwable failure) {
                NativeLoaderRuntimeHostSupport.putInvocationFailure(evidence, "world_blocks.setBlock", failure);
                return false;
            }
        }, context.runtimeHostContext());
        evidence.put("serverInvocationCompleted", invoked);
        return Map.copyOf(evidence);
    }

    public static Map<String, Object> placeStructure(
            Context context,
            Object level,
            Object player,
            String structureId,
            String anchor
    ) {
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("attempted", true);
        evidence.put("mutated", false);
        evidence.put("nativeInterface", "EchoNativeRuntimeHost.Structures");
        evidence.put("nativeMethod", "placeStructure");
        evidence.put("requestedStructureId", structureId == null ? "" : structureId);
        evidence.put("requestedAnchor", anchor == null ? "" : anchor);
        boolean invoked = NativeLoaderRuntimeHostSupport.invokeForServerPlayer(level, player, (serverPlayer, serverLevel) -> {
            Object runtimeHost = runtimeHost(context, serverPlayer, serverLevel);
            if (runtimeHost == null) {
                NativeLoaderRuntimeHostSupport.putMissingHostEvidence(evidence);
                return false;
            }
            Object center = optionalMethodValue(serverPlayer, "blockPosition");
            if (center == null) {
                evidence.put("failureKind", "missing_player_position");
                return false;
            }
            String resolvedStructureId = structureId == null ? "" : structureId.trim();
            if (resolvedStructureId.isBlank()) {
                evidence.put("failureKind", "missing_structure_id");
                return false;
            }
            putRuntimeHostEvidence(context, evidence, runtimeHost);
            int x = context.intMethodReader().get(center, "getX");
            int y = context.intMethodReader().get(center, "getY");
            int z = context.intMethodReader().get(center, "getZ");
            evidence.put("structureId", resolvedStructureId);
            evidence.put("anchor", anchor == null ? "" : anchor);
            evidence.put("target", Map.of(
                    "dimensionId", runtimeDimensionId(runtimeHost),
                    "x", x,
                    "y", y,
                    "z", z));
            try {
                Object result = placeStructureResult(
                        context,
                        runtimeHost,
                        resolvedStructureId,
                        x,
                        y,
                        z,
                        anchor);
                NativeLoaderRuntimeHostSupport.putResultEvidence(evidence, result);
                return NativeLoaderRuntimeHostSupport.resultMutated(result);
            } catch (Throwable failure) {
                NativeLoaderRuntimeHostSupport.putInvocationFailure(evidence, "structures.placeStructure", failure);
                return false;
            }
        }, context.runtimeHostContext());
        evidence.put("serverInvocationCompleted", invoked);
        return Map.copyOf(evidence);
    }

    public static boolean writeSaveData(
            Context context,
            Object level,
            Object player,
            String scope,
            String key,
            Map<String, Object> payload
    ) {
        return NativeLoaderRuntimeHostSupport.invokeForServerPlayer(level, player,
                (serverPlayer, serverLevel) -> writeSaveDataOnServer(
                        context,
                        serverPlayer,
                        serverLevel,
                        scope,
                        key,
                        payload
                ),
                context.runtimeHostContext());
    }

    public static Map<String, Object> writeSaveDataEvidence(
            Context context,
            Object level,
            Object player,
            String scope,
            String key,
            Map<String, Object> payload
    ) {
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("attempted", true);
        evidence.put("mutated", false);
        evidence.put("nativeInterface", "EchoNativeRuntimeHost.SaveData");
        evidence.put("nativeMethod", "write");
        evidence.put("scope", scope == null ? "" : scope);
        evidence.put("key", key == null ? "" : key);
        boolean invoked = NativeLoaderRuntimeHostSupport.invokeForServerPlayer(level, player, (serverPlayer, serverLevel) -> {
            Object runtimeHost = runtimeHost(context, serverPlayer, serverLevel);
            if (runtimeHost == null) {
                NativeLoaderRuntimeHostSupport.putMissingHostEvidence(evidence);
                return false;
            }
            putRuntimeHostEvidence(context, evidence, runtimeHost);
            try {
                Object result = writeSaveData(
                        context,
                        runtimeHost,
                        scope,
                        key,
                        payload,
                        "native_client.save_data." + compactActionKey(scope + "." + key));
                NativeLoaderRuntimeHostSupport.putResultEvidence(evidence, result);
                return NativeLoaderRuntimeHostSupport.resultMutated(result);
            } catch (Throwable failure) {
                NativeLoaderRuntimeHostSupport.putInvocationFailure(evidence, "save_data.write", failure);
                return false;
            }
        }, context.runtimeHostContext());
        evidence.put("serverInvocationCompleted", invoked);
        return Map.copyOf(evidence);
    }

    public static Object writeSaveData(
            Context context,
            Object runtimeHost,
            String scope,
            String key,
            Map<String, Object> payload,
            String idempotencyKey
    ) throws ReflectiveOperationException {
        if (runtimeHost == null) {
            return null;
        }
        Object saveData = runtimeHost.getClass().getMethod("saveData").invoke(runtimeHost);
        if (saveData == null) {
            return null;
        }
        java.lang.reflect.Method write = runtimeHostMethod(saveData, "write", 2);
        Class<?>[] parameterTypes = write.getParameterTypes();
        Object nativeSaveData = parameterTypes[0]
                .getConstructor(String.class, String.class, Map.class)
                .newInstance(scope, key, payload == null ? Map.of() : payload);
        Object mutationContext = runtimeMutationContext(
                context,
                runtimeHost,
                parameterTypes[1],
                idempotencyKey == null || idempotencyKey.isBlank()
                        ? "native_client.save_data." + compactActionKey(scope + "." + key)
                        : idempotencyKey,
                "EchoNativeRuntimeHost.SaveData",
                "write");
        return write.invoke(saveData, nativeSaveData, mutationContext);
    }

    public static Object publishEvent(
            Context context,
            Object runtimeHost,
            String eventId,
            Map<String, Object> payload,
            String idempotencyKey
    ) throws ReflectiveOperationException {
        if (runtimeHost == null) {
            return null;
        }
        Object events = runtimeHost.getClass().getMethod("events").invoke(runtimeHost);
        if (events == null) {
            return null;
        }
        java.lang.reflect.Method publish = runtimeHostMethod(events, "publish", 2);
        Class<?>[] parameterTypes = publish.getParameterTypes();
        java.lang.reflect.RecordComponent[] components = parameterTypes[0].getRecordComponents();
        Class<?> playerRefClass = components == null || components.length < 2
                ? Class.forName("com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost$NativePlayerRef")
                : components[1].getType();
        Object playerRef = runtimePlayerRef(context, runtimeHost, playerRefClass);
        Object nativeEvent = parameterTypes[0]
                .getConstructor(String.class, playerRefClass, Map.class)
                .newInstance(eventId, playerRef, payload == null ? Map.of() : payload);
        Object mutationContext = runtimeMutationContext(
                context,
                runtimeHost,
                parameterTypes[1],
                idempotencyKey == null || idempotencyKey.isBlank()
                        ? "native_client.event." + compactActionKey(eventId)
                        : idempotencyKey,
                "EchoNativeRuntimeHost.Events",
                "publish");
        return publish.invoke(events, nativeEvent, mutationContext);
    }

    public static Map<String, Object> publishHudNotification(
            Context context,
            Object level,
            Object player,
            Map<String, Object> payload
    ) {
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("attempted", true);
        evidence.put("mutated", false);
        evidence.put("nativeInterface", "EchoNativeRuntimeHost.Hud");
        evidence.put("nativeMethod", "publishNotification");
        evidence.put("payload", payload == null ? Map.of() : payload);
        Object serverPlayer = NativeLoaderRuntimeHostSupport.serverPlayer(player, context.runtimeHostContext());
        if (serverPlayer == null) {
            evidence.put("failureKind", "missing_server_player");
            return evidence;
        }
        Object serverLevel = NativeLoaderRuntimeHostSupport.serverLevel(level, serverPlayer, context.runtimeHostContext());
        if (serverLevel == null) {
            evidence.put("failureKind", "missing_server_level");
            return evidence;
        }
        Object server = NativeLoaderRuntimeHostSupport.server(player, serverPlayer, context.runtimeHostContext());
        boolean mutated = NativeLoaderRuntimeHostSupport.invokeOnServer(server, () -> {
            try {
                Map<String, Object> serverEvidence = publishHudNotificationOnServer(
                        context,
                        serverPlayer,
                        serverLevel,
                        payload == null ? Map.of() : payload
                );
                evidence.putAll(serverEvidence);
                return Boolean.TRUE.equals(serverEvidence.get("mutated"));
            } catch (Throwable failure) {
                NativeLoaderRuntimeHostSupport.putInvocationFailure(evidence, "hud.publishNotification", failure);
                return false;
            }
        });
        evidence.put("mutated", mutated);
        return evidence;
    }

    public static void putRuntimeHostEvidence(Context context, Map<String, Object> evidence, Object runtimeHost) {
        if (evidence == null || runtimeHost == null) {
            return;
        }
        String runtimeHostId = NativeLoaderRuntimeHostSupport.hostId(runtimeHost);
        evidence.put("runtimeHostClass", runtimeHost.getClass().getName());
        evidence.put("runtimeHostId", runtimeHostId);
        evidence.put("runtimeHostLane", NativeLoaderRuntimeHostSupport.hostLane(runtimeHost));
        evidence.put("compatibilityDelegate", NativeLoaderRuntimeHostSupport.compatibilityDelegate(runtimeHost));
        evidence.put("runtimePlayerId", runtimePlayerId(runtimeHost));
        evidence.put("runtimeHostRegistered", NativeLoaderRuntimeHostSupport.hostRegistered(runtimeHostId));
        Map<String, Object> capabilities = NativeLoaderRuntimeHostSupport.capabilitiesSnapshot(runtimeHostId);
        if (!capabilities.isEmpty()) {
            evidence.put("runtimeHostCapabilities", capabilities);
        }
    }

    public static String runtimeDimensionId(Object runtimeHost) {
        Object directDimensionId = optionalMethodValue(runtimeHost, "dimensionId");
        if (directDimensionId != null && !String.valueOf(directDimensionId).isBlank()) {
            return String.valueOf(directDimensionId);
        }
        Object hostContext = optionalMethodValue(runtimeHost, "context");
        Object dimensionId = optionalMethodValue(hostContext, "dimensionId");
        return dimensionId == null ? "" : String.valueOf(dimensionId);
    }

    private static boolean writeSaveDataOnServer(
            Context context,
            Object serverPlayer,
            Object level,
            String scope,
            String key,
            Map<String, Object> payload
    ) throws ReflectiveOperationException {
        Object runtimeHost = runtimeHost(context, serverPlayer, level);
        if (runtimeHost == null) {
            return false;
        }
        Object result = writeSaveData(
                context,
                runtimeHost,
                scope,
                key,
                payload,
                "native_client.save_data." + compactActionKey(scope + "." + key));
        return NativeLoaderRuntimeHostSupport.resultMutated(result);
    }

    private static Object setBlockResult(Context context, Object runtimeHost, int x, int y, int z, String blockId)
            throws ReflectiveOperationException {
        if (runtimeHost == null) {
            return null;
        }
        Object blocks = runtimeHost.getClass().getMethod("worldBlocks").invoke(runtimeHost);
        java.lang.reflect.Method setBlock = runtimeHostMethod(blocks, "setBlock", 3);
        Class<?>[] parameterTypes = setBlock.getParameterTypes();
        Object blockRef = parameterTypes[0]
                .getConstructor(String.class, int.class, int.class, int.class)
                .newInstance(runtimeDimensionId(runtimeHost), x, y, z);
        Object blockState = parameterTypes[1]
                .getConstructor(String.class, Map.class)
                .newInstance(blockId, Map.of());
        Object mutationContext = runtimeMutationContext(
                context,
                runtimeHost,
                parameterTypes[2],
                "native_client.world_block." + compactActionKey(blockId + "." + x + "." + y + "." + z),
                "EchoNativeRuntimeHost.WorldBlocks",
                "setBlock");
        return setBlock.invoke(blocks, blockRef, blockState, mutationContext);
    }

    private static Object placeStructureResult(
            Context context,
            Object runtimeHost,
            String structureId,
            int x,
            int y,
            int z,
            String anchor
    ) throws ReflectiveOperationException {
        if (runtimeHost == null) {
            return null;
        }
        Object structures = runtimeHost.getClass().getMethod("structures").invoke(runtimeHost);
        java.lang.reflect.Method placeStructure = runtimeHostMethod(structures, "placeStructure", 2);
        Class<?>[] parameterTypes = placeStructure.getParameterTypes();
        Map<String, Object> constraints = Map.of(
                "source", "native_loader_playable_runtime",
                "minimumStartingSurfaceY", 48);
        Object placement = parameterTypes[0]
                .getConstructor(String.class, String.class, int.class, int.class, int.class, String.class, Map.class)
                .newInstance(
                        structureId,
                        runtimeDimensionId(runtimeHost),
                        x,
                        y,
                        z,
                        anchor == null ? "" : anchor,
                        constraints);
        Object mutationContext = runtimeMutationContext(
                context,
                runtimeHost,
                parameterTypes[1],
                "native_client.structure." + compactActionKey(structureId + "." + x + "." + y + "." + z),
                "EchoNativeRuntimeHost.Structures",
                "placeStructure");
        return placeStructure.invoke(structures, placement, mutationContext);
    }

    private static Map<String, Object> publishHudNotificationOnServer(
            Context context,
            Object serverPlayer,
            Object level,
            Map<String, Object> payload
    ) throws ReflectiveOperationException {
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("attempted", true);
        evidence.put("mutated", false);
        evidence.put("nativeInterface", "EchoNativeRuntimeHost.Hud");
        evidence.put("nativeMethod", "publishNotification");
        Object runtimeHost = runtimeHost(context, serverPlayer, level);
        if (runtimeHost == null) {
            NativeLoaderRuntimeHostSupport.putMissingHostEvidence(evidence);
            return evidence;
        }
        Object hud = runtimeHost.getClass().getMethod("hud").invoke(runtimeHost);
        if (hud == null) {
            evidence.put("failureKind", "missing_hud_host");
            return evidence;
        }
        java.lang.reflect.Method publishNotification = runtimeHostMethod(hud, "publishNotification", 3);
        Class<?>[] parameterTypes = publishNotification.getParameterTypes();
        Object playerRef = runtimePlayerRef(context, runtimeHost, parameterTypes[0]);
        Object mutationContext = runtimeHost.getClass()
                .getMethod("context", String.class, String.class, String.class)
                .invoke(runtimeHost,
                        context.hudActionKey().apply("live_proof"),
                        "EchoNativeRuntimeHost.Hud",
                        "publishNotification");
        if (!parameterTypes[2].isInstance(mutationContext)) {
            mutationContext = runtimeMutationContext(
                    context,
                    runtimeHost,
                    parameterTypes[2],
                    context.hudActionKey().apply("live_proof"),
                    "EchoNativeRuntimeHost.Hud",
                    "publishNotification");
        }
        Object result = publishNotification.invoke(hud, playerRef, payload, mutationContext);
        NativeLoaderRuntimeHostSupport.putResultEvidence(evidence, result);
        Map<String, Object> snapshot = object(evidence.get("resultSnapshot"));
        evidence.put("packetSent", Boolean.TRUE.equals(snapshot.get("packetSent")));
        evidence.put("chatFallbackShown", Boolean.TRUE.equals(snapshot.get("chatFallbackShown")));
        putRuntimeHostEvidence(context, evidence, runtimeHost);
        return evidence;
    }

    private static Object runtimeHost(Context context, Object serverPlayer, Object level) {
        return NativeLoaderRuntimeHostSupport.runtimeHost(serverPlayer, level, context.runtimeHostContext());
    }

    private static java.lang.reflect.Method runtimeHostMethod(Object target, String methodName, int parameterCount)
            throws NoSuchMethodException {
        if (target == null) {
            throw new NoSuchMethodException("Missing target for " + methodName);
        }
        for (java.lang.reflect.Method method : target.getClass().getMethods()) {
            if (method.getName().equals(methodName) && method.getParameterCount() == parameterCount) {
                method.trySetAccessible();
                return method;
            }
        }
        for (java.lang.reflect.Method method : target.getClass().getDeclaredMethods()) {
            if (method.getName().equals(methodName) && method.getParameterCount() == parameterCount) {
                method.trySetAccessible();
                return method;
            }
        }
        throw new NoSuchMethodException(target.getClass().getName() + "." + methodName + "/" + parameterCount);
    }

    private static Object runtimePlayerRef(Context context, Object runtimeHost, Class<?> playerRefClass)
            throws ReflectiveOperationException {
        Object playerRef = optionalMethodValue(runtimeHost, "playerRef");
        if (playerRefClass.isInstance(playerRef)) {
            return playerRef;
        }
        return playerRefClass.getConstructor(String.class).newInstance(runtimePlayerId(runtimeHost));
    }

    private static String runtimePlayerId(Object runtimeHost) {
        Object hostContext = optionalMethodValue(runtimeHost, "context");
        Object playerId = optionalMethodValue(hostContext, "playerId");
        if (playerId != null && !String.valueOf(playerId).isBlank()) {
            return String.valueOf(playerId);
        }
        return "native-client";
    }

    private static Object runtimeMutationContext(
            Context context,
            Object runtimeHost,
            Class<?> nativeMutationContextClass,
            String idempotencyKey,
            String nativeInterface,
            String nativeMethod
    ) throws ReflectiveOperationException {
        try {
            Object mutationContext = runtimeHost.getClass()
                    .getMethod("context", String.class, String.class, String.class)
                    .invoke(runtimeHost, idempotencyKey, nativeInterface, nativeMethod);
            if (nativeMutationContextClass.isInstance(mutationContext)) {
                return mutationContext;
            }
        } catch (Throwable ignored) {
            // Fall through to the public AdapterCore context record for hosts that do not expose NeoForge helpers.
        }
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("nativeInterface", nativeInterface == null ? "" : nativeInterface);
        metadata.put("nativeMethod", nativeMethod == null ? "" : nativeMethod);
        metadata.put("hostRuntime", runtimeHost == null ? "" : runtimeHost.getClass().getName());
        metadata.put("runtimeLane", NativeLoaderRuntimeHostSupport.hostLane(runtimeHost));
        metadata.put("compatibilityDelegate", NativeLoaderRuntimeHostSupport.compatibilityDelegate(runtimeHost));
        return nativeMutationContextClass
                .getConstructor(String.class, String.class, String.class, String.class, long.class, Map.class)
                .newInstance(
                        runtimeModuleId(runtimeHost),
                        runtimeDimensionId(runtimeHost),
                        idempotencyKey,
                        "SERVER",
                        runtimeGameTime(runtimeHost),
                        Map.copyOf(metadata));
    }

    private static String runtimeModuleId(Object runtimeHost) {
        Object hostContext = optionalMethodValue(runtimeHost, "context");
        Object moduleId = optionalMethodValue(hostContext, "moduleId");
        return moduleId == null || String.valueOf(moduleId).isBlank() ? "native_client" : String.valueOf(moduleId);
    }

    private static long runtimeGameTime(Object runtimeHost) {
        Object hostContext = optionalMethodValue(runtimeHost, "context");
        Object gameTime = optionalMethodValue(hostContext, "gameTime");
        return gameTime instanceof Number number ? number.longValue() : 0L;
    }

    private static String compactActionKey(String value) {
        String key = value == null ? "" : value.trim().toLowerCase(java.util.Locale.ROOT);
        key = key.replaceAll("[^a-z0-9_.:-]+", "_");
        return key.isBlank() ? "unknown" : key;
    }

    private static Map<String, Object> object(Object value) {
        if (!(value instanceof Map<?, ?> map)) {
            return Map.of();
        }
        Map<String, Object> object = new LinkedHashMap<>();
        map.forEach((key, item) -> object.put(String.valueOf(key), item));
        return object;
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
            Function<String, String> itemResolver,
            Function<String, String> blockResolver,
            Function<String, String> blockFallback,
            Function<String, String> hudActionKey,
            IntMethodReader intMethodReader
    ) {
    }

    @FunctionalInterface
    public interface IntMethodReader {
        int get(Object target, String methodName) throws ReflectiveOperationException;
    }
}
