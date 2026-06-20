package dev.echo.nativeplatform.loader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class NativeLoaderWorldStartupFlow {
    public static final String SERVICE_ID = "echo.native.world_startup_flow";
    private final String nativeGameDirProperty;

    public NativeLoaderWorldStartupFlow(String nativeGameDirProperty) {
        this.nativeGameDirProperty = nativeGameDirProperty == null ? "" : nativeGameDirProperty;
    }

    public Map<String, Object> apply(String packId, List<String> remainingArgs) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("bridge", "native_loader.world_startup");
        data.put("packId", packId == null ? "" : packId);
        data.put("productId", NativeLoaderAshfallWorldStartupService.PRODUCT_ID);
        data.put("nativeLoaderOwnedWorldPolicy", true);
        data.put("forcedWorldPreset", NativeLoaderAshfallWorldStartupService.WORLD_PRESET_ID);
        data.put("vanillaWorldCreationFallbackAllowed", false);
        data.put("autoOpenRequested", NativeLoaderAshfallWorldStartupService.productWorldAutoOpen());
        data.put("ashfallProduct", ashfallProduct(packId));
        if (!ashfallProduct(packId)) {
            data.put("applied", false);
            data.put("blocked", false);
            data.put("skipped", true);
            data.put("summary", "World startup bridge skipped because this is not the Ashfall product.");
            return Map.copyOf(data);
        }
        if (!NativeLoaderAshfallWorldStartupService.productWorldAutoOpen()) {
            data.put("applied", false);
            data.put("blocked", false);
            data.put("skipped", true);
            data.put("summary", "World startup bridge skipped because product auto-open is disabled.");
            return Map.copyOf(data);
        }
        Path gameDir = gameDir(remainingArgs);
        data.put("gameDir", gameDir == null ? "" : gameDir.toString());
        if (gameDir == null) {
            data.put("applied", false);
            data.put("blocked", true);
            data.put("failureKind", "missing_game_dir");
            data.put("summary", "Native Loader cannot prepare the Ashfall product world without --gameDir.");
            return Map.copyOf(data);
        }
        try {
            NativeLoaderAshfallWorldStartupService.StartupPlan plan =
                    NativeLoaderAshfallWorldStartupService.prepare(gameDir);
            Map<String, Object> planReport = plan.toReport();
            data.put("applied", plan.action() != NativeLoaderAshfallWorldStartupService.StartupAction.BLOCKED);
            data.put("blocked", plan.action() == NativeLoaderAshfallWorldStartupService.StartupAction.BLOCKED);
            data.put("skipped", false);
            data.put("startupAction", plan.action().name());
            data.put("createsFreshProductWorld", plan.createsFreshProductWorld());
            data.put("opensMarkedProductWorld", plan.opensMarkedProductWorld());
            data.put("blocksVanillaFallback", plan.blocksVanillaFallback());
            data.put("worldFolder", plan.folder());
            data.put("worldName", plan.worldName());
            data.put("worldPreset", plan.worldPreset());
            data.put("datapackFile", plan.datapackFile());
            data.put("stagedDatapack", plan.stagedDatapack().toString());
            data.put("stagedDatapackReady", Files.isRegularFile(plan.stagedDatapack())
                    && NativeLoaderAshfallWorldStartupService.isValidProductDatapack(plan.stagedDatapack()));
            data.put("productWorldMarker", plan.marker().toString());
            data.put("productWorldMarkerWritten", Files.isRegularFile(plan.marker()));
            data.put("plan", planReport);
            data.put("failureKind", plan.failureKind());
            data.put("summary", plan.action() == NativeLoaderAshfallWorldStartupService.StartupAction.BLOCKED
                    ? "Native Loader blocked Ashfall product world startup before Minecraft handoff."
                    : "Native Loader prepared Ashfall product world startup before Minecraft handoff.");
        } catch (Throwable exception) {
            data.put("applied", false);
            data.put("blocked", true);
            data.put("failureKind", exception.getClass().getSimpleName());
            data.put("failureMessage", exception.getMessage() == null ? "" : exception.getMessage());
            data.put("summary", "Native Loader failed while preparing Ashfall product world startup before Minecraft handoff.");
        }
        return Map.copyOf(data);
    }

    public static boolean blocksHandoff(Map<String, Object> runtimeBridge) {
        Map<String, Object> worldStartupBridge = object(runtimeBridge.get("worldStartupBridge"));
        return Boolean.TRUE.equals(worldStartupBridge.get("blocked"));
    }

    public static String blockMessage(Map<String, Object> runtimeBridge) {
        Map<String, Object> worldStartupBridge = object(runtimeBridge.get("worldStartupBridge"));
        String failureKind = text(worldStartupBridge.get("failureKind"));
        String summary = text(worldStartupBridge.get("summary"));
        if (failureKind.isBlank()) {
            return summary.isBlank() ? "Native Loader blocked product world startup." : summary;
        }
        return summary + " Failure: " + failureKind;
    }

    private Path gameDir(List<String> remainingArgs) {
        Path fromArgs = argumentPath(remainingArgs, "--gameDir");
        if (fromArgs != null) {
            return fromArgs;
        }
        if (nativeGameDirProperty.isBlank()) {
            return null;
        }
        String configured = System.getProperty(nativeGameDirProperty, "").trim();
        return configured.isBlank() ? null : Path.of(configured).toAbsolutePath().normalize();
    }

    private static Path argumentPath(List<String> args, String name) {
        if (args == null) {
            return null;
        }
        for (int index = 0; index < args.size() - 1; index++) {
            if (name.equals(args.get(index))) {
                return Path.of(args.get(index + 1)).toAbsolutePath().normalize();
            }
        }
        return null;
    }

    private static boolean ashfallProduct(String packId) {
        String normalized = packId == null ? "" : packId.trim().toLowerCase(Locale.ROOT);
        return "ashfall".equals(normalized)
                || "ashfall-native-edition".equals(normalized)
                || "ashfall-native-loader".equals(normalized)
                || "echo-ashfall-native-loader".equals(normalized)
                || NativeLoaderAshfallWorldStartupService.PRODUCT_ID.equals(normalized);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> object(Object value) {
        return value instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
    }

    private static String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
