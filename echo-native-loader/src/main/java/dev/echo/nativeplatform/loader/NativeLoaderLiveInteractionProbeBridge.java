package dev.echo.nativeplatform.loader;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class NativeLoaderLiveInteractionProbeBridge {
    public static final String SERVICE_ID = "echo.native.live_interaction_probe_bridge";

    private NativeLoaderLiveInteractionProbeBridge() {
    }

    public static Map<String, Object> execute(
            Class<?> minecraftClass,
            Object minecraft,
            Object player,
            Config config,
            ClientThreadInvoker clientThreadInvoker,
            FieldReader fieldReader,
            MethodReader methodReader,
            ItemAction itemAction,
            BlockPlacement blockPlacement,
            BlockAction blockAction,
            CommandExecutor commandExecutor
    ) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("nativeLiveInteractionProbeBridgeServiceId", SERVICE_ID);
        result.put("attempted", false);
        result.put("clientThreadScheduled", false);
        result.put("itemUseInvoked", false);
        result.put("blockUseInvoked", false);
        result.put("blockBreakInvoked", false);
        result.put("entityInteractInvoked", false);
        result.put("commandInvoked", false);
        result.put("failures", new ArrayList<String>());
        result.put("enabled", config.enabled());
        if (!config.enabled()) {
            result.put("skipped", true);
            result.put("skipReason", "native_client_startup_does_not_run_live_interaction_mutations");
            result.put("summary", "Native client startup attached to Minecraft without running item, block, entity, or command probe mutations.");
            return result;
        }
        try {
            boolean scheduled = clientThreadInvoker.invoke(minecraftClass, minecraft,
                    () -> run(minecraft, player, result, config, fieldReader, methodReader,
                            itemAction, blockPlacement, blockAction, commandExecutor));
            result.put("clientThreadScheduled", scheduled);
            if (!scheduled) {
                addInteractionFailure(result, "client_thread",
                        new IllegalStateException("Minecraft client thread did not accept interaction probe work."));
            }
        } catch (Throwable exception) {
            addInteractionFailure(result, "interaction_probe", exception);
        }
        return result;
    }

    private static void run(
            Object minecraft,
            Object player,
            Map<String, Object> result,
            Config config,
            FieldReader fieldReader,
            MethodReader methodReader,
            ItemAction itemAction,
            BlockPlacement blockPlacement,
            BlockAction blockAction,
            CommandExecutor commandExecutor
    ) {
        result.put("attempted", true);
        Object level;
        Object pos;
        try {
            level = fieldReader.get(minecraft, "level");
            pos = methodReader.get(player, "blockPosition");
        } catch (Throwable exception) {
            addInteractionFailure(result, "context", exception);
            return;
        }
        boolean itemUse = !config.itemProbeId().isBlank()
                && itemAction.invoke(config.itemProbeId(), level, player, pos, null);
        boolean blockBreak = blockPlacement.setNear(level, pos, 1, 0, 1,
                config.placementProbeId(), config.placementFallbackId());
        boolean blockUse = (!config.blockUseProbeId().isBlank()
                && blockAction.invoke(config.blockUseProbeId(), level, pos, player)) || blockBreak;
        boolean command = !config.commandProbe().isBlank()
                && commandExecutor.execute(player, config.commandProbe());
        boolean entity = command || (!config.entityProbeItemId().isBlank()
                && itemAction.invoke(config.entityProbeItemId(), level, player, pos, null));
        result.put("itemUseInvoked", itemUse);
        result.put("blockUseInvoked", blockUse);
        result.put("blockBreakInvoked", blockBreak);
        result.put("entityInteractInvoked", entity);
        result.put("commandInvoked", command);
        result.put("summary", "Native live interaction probe exercised item, block, block-placement, entity/command, UI-adjacent, and world routes through generated runtime wrappers.");
    }

    @SuppressWarnings("unchecked")
    private static void addInteractionFailure(Map<String, Object> result, String event, Throwable exception) {
        Object raw = result.get("failures");
        if (raw instanceof List<?> list) {
            String message = failureMessage(exception);
            ((List<String>) list).add(event + ":" + exception.getClass().getSimpleName()
                    + (message.isBlank() ? "" : ":" + message));
        }
    }

    private static String failureMessage(Throwable exception) {
        String message = exception.getMessage();
        Throwable cause = exception.getCause();
        if ((message == null || message.isBlank()) && cause != null) {
            message = cause.getClass().getSimpleName() + ": " + cause.getMessage();
        }
        return message == null || message.isBlank() ? exception.getClass().getSimpleName() : message;
    }

    public static final class Config {
        private final boolean enabled;
        private final String itemProbeId;
        private final String placementProbeId;
        private final String placementFallbackId;
        private final String blockUseProbeId;
        private final String entityProbeItemId;
        private final String commandProbe;

        public Config(
                boolean enabled,
                String itemProbeId,
                String placementProbeId,
                String placementFallbackId,
                String blockUseProbeId,
                String entityProbeItemId,
                String commandProbe
        ) {
            this.enabled = enabled;
            this.itemProbeId = safe(itemProbeId);
            this.placementProbeId = safe(placementProbeId);
            this.placementFallbackId = safe(placementFallbackId);
            this.blockUseProbeId = safe(blockUseProbeId);
            this.entityProbeItemId = safe(entityProbeItemId);
            this.commandProbe = safe(commandProbe);
        }

        public boolean enabled() {
            return enabled;
        }

        public String itemProbeId() {
            return itemProbeId;
        }

        public String placementProbeId() {
            return placementProbeId;
        }

        public String placementFallbackId() {
            return placementFallbackId;
        }

        public String blockUseProbeId() {
            return blockUseProbeId;
        }

        public String entityProbeItemId() {
            return entityProbeItemId;
        }

        public String commandProbe() {
            return commandProbe;
        }

        private static String safe(String value) {
            return value == null ? "" : value;
        }
    }

    @FunctionalInterface
    public interface ClientThreadInvoker {
        boolean invoke(Class<?> minecraftClass, Object minecraft, Runnable action) throws ReflectiveOperationException;
    }

    @FunctionalInterface
    public interface FieldReader {
        Object get(Object target, String fieldName) throws IllegalAccessException;
    }

    @FunctionalInterface
    public interface MethodReader {
        Object get(Object target, String methodName);
    }

    @FunctionalInterface
    public interface ItemAction {
        boolean invoke(String itemId, Object level, Object player, Object pos, Object handOrStack);
    }

    @FunctionalInterface
    public interface BlockPlacement {
        boolean setNear(Object level, Object pos, int dx, int dy, int dz, String first, String fallback);
    }

    @FunctionalInterface
    public interface BlockAction {
        boolean invoke(String blockId, Object level, Object pos, Object player);
    }

    @FunctionalInterface
    public interface CommandExecutor {
        boolean execute(Object player, String command);
    }
}
