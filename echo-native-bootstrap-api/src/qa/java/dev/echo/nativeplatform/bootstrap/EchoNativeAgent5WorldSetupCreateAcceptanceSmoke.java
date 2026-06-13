package dev.echo.nativeplatform.bootstrap;

import dev.echo.nativeplatform.loader.NativeLoaderAshfallWorldStartupService;
import dev.echo.nativeplatform.loader.NativeLoaderUiActionRouter;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class EchoNativeAgent5WorldSetupCreateAcceptanceSmoke {
    private EchoNativeAgent5WorldSetupCreateAcceptanceSmoke() {
    }

    public static Map<String, Object> capture() {
        String previousGameDir = System.getProperty(NativeLoaderUiActionRouter.WORLD_SETUP_GAME_DIR_PROPERTY);
        String previousFolder = System.getProperty("echo.native.productWorldFolder");
        String previousName = System.getProperty("echo.native.productWorldName");
        String previousDatapack = System.getProperty("echo.native.productWorldDatapack");
        String previousGameMode = System.getProperty("echo.native.productWorldGameMode");
        try {
            Path gameDir = Files.createTempDirectory("echo-agent5-world-setup-create");
            String folder = "agent5_world_setup";
            String datapack = "agent5-world-setup.zip";
            System.setProperty(NativeLoaderUiActionRouter.WORLD_SETUP_GAME_DIR_PROPERTY, gameDir.toString());
            System.setProperty("echo.native.productWorldFolder", folder);
            System.setProperty("echo.native.productWorldName", "Agent 5 World Setup");
            System.setProperty("echo.native.productWorldDatapack", datapack);
            System.setProperty("echo.native.productWorldGameMode", "survival");
            writeValidDatapack(gameDir.resolve("echo-native").resolve("worldgen").resolve(datapack));

            Map<String, Object> route = EchoNativeAgent5UiActionRouter.routeWorldSetupCreate();
            Path saveDir = gameDir.resolve("saves").resolve(folder);
            Path marker = saveDir.resolve(NativeLoaderAshfallWorldStartupService.PRODUCT_WORLD_MARKER);
            Path openMarker = saveDir.resolve(NativeLoaderAshfallWorldStartupService.PRODUCT_WORLD_OPEN_MARKER);
            Path stagedDatapack = saveDir.resolve("datapacks").resolve(datapack);
            boolean accepted = Boolean.TRUE.equals(route.get("handled"))
                    && Boolean.TRUE.equals(route.get("worldSetupPrepared"))
                    && !Boolean.TRUE.equals(route.get("worldSetupBlocked"))
                    && Boolean.TRUE.equals(route.get("nativeProductWorldOpenDispatchRecorded"))
                    && "CREATE_NEW".equals(route.get("worldSetupStartupAction"))
                    && "MISSION_LOG".equals(route.get("destinationMode"))
                    && "world_setup:create".equals(route.get("effect"))
                    && Files.isRegularFile(marker)
                    && Files.isRegularFile(openMarker)
                    && Files.isRegularFile(stagedDatapack)
                    && NativeLoaderAshfallWorldStartupService.isValidProductDatapack(stagedDatapack)
                    && Files.readString(openMarker).contains("\"minecraftDispatch\": \"NativeLoaderWorldSetup.create\"")
                    && Boolean.FALSE.equals(route.get("vanillaWorldCreationFallbackAllowed"));

            Map<String, Object> smoke = new LinkedHashMap<>();
            smoke.put("worldSetupCreateAcceptanceSmokeClass",
                    EchoNativeAgent5WorldSetupCreateAcceptanceSmoke.class.getSimpleName());
            smoke.put("serviceCodeExecuted", true);
            smoke.put("adapterCoreBridge", true);
            smoke.put("passed", accepted);
            smoke.put("route", route);
            smoke.put("gameDir", gameDir.toString());
            smoke.put("saveDir", saveDir.toString());
            smoke.put("productWorldMarkerWritten", Files.isRegularFile(marker));
            smoke.put("productWorldOpenDispatchWritten", Files.isRegularFile(openMarker));
            smoke.put("stagedDatapackReady", Files.isRegularFile(stagedDatapack)
                    && NativeLoaderAshfallWorldStartupService.isValidProductDatapack(stagedDatapack));
            smoke.put("nativeLoaderOwnedWorldPolicy", route.get("nativeLoaderOwnedWorldPolicy"));
            smoke.put("vanillaWorldCreationFallbackAllowed", route.get("vanillaWorldCreationFallbackAllowed"));
            smoke.put("forcedWorldPreset", route.get("forcedWorldPreset"));
            return Map.copyOf(smoke);
        } catch (Exception exception) {
            return Map.of(
                    "worldSetupCreateAcceptanceSmokeClass",
                    EchoNativeAgent5WorldSetupCreateAcceptanceSmoke.class.getSimpleName(),
                    "serviceCodeExecuted", true,
                    "passed", false,
                    "failureKind", exception.getClass().getSimpleName(),
                    "failureMessage", exception.getMessage() == null ? "" : exception.getMessage()
            );
        } finally {
            restore(NativeLoaderUiActionRouter.WORLD_SETUP_GAME_DIR_PROPERTY, previousGameDir);
            restore("echo.native.productWorldFolder", previousFolder);
            restore("echo.native.productWorldName", previousName);
            restore("echo.native.productWorldDatapack", previousDatapack);
            restore("echo.native.productWorldGameMode", previousGameMode);
        }
    }

    private static void writeValidDatapack(Path zipPath) throws Exception {
        Files.createDirectories(zipPath.getParent());
        Map<String, String> entries = new LinkedHashMap<>();
        entries.put("pack.mcmeta", "{\"pack\":{\"min_format\":[101,1],\"max_format\":[101,1]}}\n");
        for (String entry : NativeLoaderAshfallWorldStartupService.requiredDatapackEntries()) {
            if (!"pack.mcmeta".equals(entry)) {
                entries.put(entry, "{}\n");
            }
        }
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(zipPath), StandardCharsets.UTF_8)) {
            for (Map.Entry<String, String> entry : entries.entrySet()) {
                zip.putNextEntry(new ZipEntry(entry.getKey()));
                zip.write(entry.getValue().getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
        }
    }

    private static void restore(String key, String value) {
        if (value == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, value);
        }
    }
}
