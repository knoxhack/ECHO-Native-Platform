package dev.echo.nativeplatform.loader;

import java.util.List;
import java.util.Map;

public final class NativeLoaderProductPlayableRuntimeActions {
    public static final String SERVICE_ID = "echo.native.product_playable_runtime_actions";
    private static final int[][] FEATURE_OFFSETS = {
            {3, 0},
            {5, 0},
            {7, 0},
            {-3, 0},
            {-5, 0}
    };

    private NativeLoaderProductPlayableRuntimeActions() {
    }

    public static boolean grantStarterTools(Config config, Object player, ItemGranter itemGranter) {
        boolean granted = false;
        for (String itemId : config.starterToolItems()) {
            if (itemId == null || itemId.isBlank()) {
                continue;
            }
            granted |= itemGranter.grant(player, itemId, 1);
        }
        return granted;
    }

    public static int sendStarterCommands(
            Config config,
            Object player,
            Map<String, Object> result,
            CommandExecutor commandExecutor
    ) {
        List<String> commands = config.starterCommands();
        int sent = 0;
        int setBlocks = 0;
        for (String command : commands) {
            if (command == null || command.isBlank()) {
                continue;
            }
            if (commandExecutor.execute(player, command)) {
                sent++;
                if (command.startsWith("setblock ")) {
                    setBlocks++;
                }
            }
        }
        result.put("serverCommandList", commands);
        result.put("serverSetBlockCommandsSent", setBlocks);
        return sent;
    }

    public static int paintClientStarterRegion(
            Config config,
            Object minecraft,
            Object player,
            Map<String, Object> result,
            FieldReader fieldReader,
            MethodReader methodReader,
            IntMethodReader intMethodReader,
            BlockSetter blockSetter
    ) {
        if (!config.hasStarterRegionBlocks()) {
            result.put("clientStarterRegionSkipped", "no_product_starter_region_blocks_configured");
            return 0;
        }
        Object level;
        try {
            level = fieldReader.get(minecraft, "level");
        } catch (Throwable exception) {
            addFailure(result, "client_level", exception);
            return 0;
        }
        Object center = methodReader.get(player, "blockPosition");
        if (level == null || center == null) {
            addFailure(result, "client_scaffold", new IllegalStateException("Missing level or player position"));
            return 0;
        }
        try {
            return paintStarterRegionAt(config, level, center, intMethodReader, blockSetter);
        } catch (Throwable exception) {
            addFailure(result, "client_scaffold", exception);
            return 0;
        }
    }

    public static int paintServerStarterRegion(
            Config config,
            Object minecraft,
            Object clientPlayer,
            Map<String, Object> result,
            MethodReader methodReader,
            IntMethodReader intMethodReader,
            BlockSetter blockSetter
    ) {
        if (!config.hasStarterRegionBlocks()) {
            result.put("serverStarterRegionSkipped", "no_product_starter_region_blocks_configured");
            return 0;
        }
        Object server = methodReader.get(minecraft, "getSingleplayerServer");
        Object playerUuid = methodReader.get(clientPlayer, "getUUID");
        if (server == null || !(playerUuid instanceof java.util.UUID uuid)) {
            addFailure(result, "server_scaffold", new IllegalStateException("Missing integrated server or player UUID"));
            return 0;
        }
        int[] placed = new int[]{0};
        Throwable[] failure = new Throwable[1];
        Runnable action = () -> {
            try {
                Object playerList = methodReader.get(server, "getPlayerList");
                Object serverPlayer = playerList == null
                        ? null
                        : playerList.getClass().getMethod("getPlayer", java.util.UUID.class).invoke(playerList, uuid);
                Object level = serverPlayer == null ? null : methodReader.get(serverPlayer, "serverLevel");
                if (level == null && serverPlayer != null) {
                    level = methodReader.get(serverPlayer, "level");
                }
                Object center = serverPlayer == null ? null : methodReader.get(serverPlayer, "blockPosition");
                if (level == null || center == null) {
                    throw new IllegalStateException("Missing server player=" + (serverPlayer != null)
                            + ", level=" + (level != null)
                            + ", position=" + (center != null));
                }
                placed[0] = paintStarterRegionAt(config, level, center, intMethodReader, blockSetter);
            } catch (Throwable exception) {
                failure[0] = exception;
            }
        };
        try {
            java.lang.reflect.Method execute = server.getClass().getMethod("execute", Runnable.class);
            java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
            execute.invoke(server, (Runnable) () -> {
                try {
                    action.run();
                } finally {
                    latch.countDown();
                }
            });
            if (!latch.await(5L, java.util.concurrent.TimeUnit.SECONDS)) {
                addFailure(result, "server_scaffold", new IllegalStateException("Timed out waiting for server placement"));
                return placed[0];
            }
        } catch (NoSuchMethodException exception) {
            action.run();
        } catch (Throwable exception) {
            failure[0] = exception;
        }
        if (failure[0] != null) {
            addFailure(result, "server_scaffold", failure[0]);
        }
        return placed[0];
    }

    private static int paintStarterRegionAt(
            Config config,
            Object level,
            Object center,
            IntMethodReader intMethodReader,
            BlockSetter blockSetter
    ) throws ReflectiveOperationException {
        int x = intMethodReader.get(center, "getX");
        int y = intMethodReader.get(center, "getY");
        int z = intMethodReader.get(center, "getZ");
        int placed = 0;
        for (int dx = -7; dx <= 7; dx++) {
            for (int dz = -7; dz <= 7; dz++) {
                placed += blockSetter.set(level, x + dx, y - 1, z + dz,
                        Math.abs(dx) <= 2 && Math.abs(dz) <= 2
                                ? config.starterRegionCoreBlockId()
                                : config.starterRegionTerrainBlockId())
                        ? 1 : 0;
                if ((Math.abs(dx) + Math.abs(dz)) % 4 == 0) {
                    placed += blockSetter.set(level, x + dx, y, z + dz, config.starterRegionSurfaceBlockId()) ? 1 : 0;
                }
            }
        }
        placed += placeStarterRegionFeatures(config, level, x, y, z, blockSetter);
        return placed;
    }

    private static int placeStarterRegionFeatures(
            Config config,
            Object level,
            int x,
            int y,
            int z,
            BlockSetter blockSetter
    ) {
        List<String> featureBlocks = config.starterRegionFeatureBlockIds();
        int placed = 0;
        int count = Math.min(FEATURE_OFFSETS.length, featureBlocks.size());
        for (int index = 0; index < count; index++) {
            int[] offset = FEATURE_OFFSETS[index];
            placed += blockSetter.set(level, x + offset[0], y, z + offset[1], featureBlocks.get(index)) ? 1 : 0;
        }
        return placed;
    }

    @SuppressWarnings("unchecked")
    private static void addFailure(Map<String, Object> result, String event, Throwable exception) {
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
        private final List<String> starterToolItems;
        private final List<String> starterCommands;
        private final String starterRegionTerrainBlockId;
        private final String starterRegionSurfaceBlockId;
        private final String starterRegionCoreBlockId;
        private final List<String> starterRegionFeatureBlockIds;

        public Config(
                List<String> starterToolItems,
                List<String> starterCommands,
                String starterRegionTerrainBlockId,
                String starterRegionSurfaceBlockId,
                String starterRegionCoreBlockId,
                List<String> starterRegionFeatureBlockIds
        ) {
            this.starterToolItems = List.copyOf(starterToolItems == null ? List.of() : starterToolItems);
            this.starterCommands = List.copyOf(starterCommands == null ? List.of() : starterCommands);
            this.starterRegionTerrainBlockId = starterRegionTerrainBlockId == null ? "" : starterRegionTerrainBlockId;
            this.starterRegionSurfaceBlockId = starterRegionSurfaceBlockId == null ? "" : starterRegionSurfaceBlockId;
            this.starterRegionCoreBlockId = starterRegionCoreBlockId == null ? "" : starterRegionCoreBlockId;
            this.starterRegionFeatureBlockIds = List.copyOf(starterRegionFeatureBlockIds == null
                    ? List.of()
                    : starterRegionFeatureBlockIds);
        }

        public boolean hasStarterRegionBlocks() {
            return !starterRegionTerrainBlockId.isBlank()
                    && !starterRegionSurfaceBlockId.isBlank()
                    && !starterRegionCoreBlockId.isBlank();
        }

        public List<String> starterToolItems() {
            return starterToolItems;
        }

        public List<String> starterCommands() {
            return starterCommands;
        }

        public String starterRegionTerrainBlockId() {
            return starterRegionTerrainBlockId;
        }

        public String starterRegionSurfaceBlockId() {
            return starterRegionSurfaceBlockId;
        }

        public String starterRegionCoreBlockId() {
            return starterRegionCoreBlockId;
        }

        public List<String> starterRegionFeatureBlockIds() {
            return starterRegionFeatureBlockIds;
        }
    }

    @FunctionalInterface
    public interface ItemGranter {
        boolean grant(Object player, String itemId, int count);
    }

    @FunctionalInterface
    public interface CommandExecutor {
        boolean execute(Object player, String command);
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
    public interface IntMethodReader {
        int get(Object target, String methodName) throws ReflectiveOperationException;
    }

    @FunctionalInterface
    public interface BlockSetter {
        boolean set(Object level, int x, int y, int z, String blockId);
    }
}
