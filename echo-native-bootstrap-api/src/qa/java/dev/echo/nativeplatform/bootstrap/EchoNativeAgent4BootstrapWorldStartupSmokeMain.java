package dev.echo.nativeplatform.bootstrap;

import dev.echo.nativeplatform.loader.NativeLoaderAshfallWorldStartupService;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class EchoNativeAgent4BootstrapWorldStartupSmokeMain {
    private EchoNativeAgent4BootstrapWorldStartupSmokeMain() {
    }

    public static void main(String[] args) throws Exception {
        String previousFolder = System.getProperty("echo.native.productWorldFolder");
        String previousName = System.getProperty("echo.native.productWorldName");
        String previousDatapack = System.getProperty("echo.native.productWorldDatapack");
        String previousAutoOpen = System.getProperty("echo.native.productWorldAutoOpen");
        String previousHandoff = System.getProperty(EchoNativeBootstrapMain.AUTHORIZED_HANDOFF_PROPERTY);
        try {
            requireBootstrapWorldStartupBridgeAppliesBeforeHandoff();
            requireBootstrapWorldStartupBridgeBlocksMissingDatapack();
            requireBootstrapMainWritesWorldStartupActivationEvidence();
            System.out.println("agent4 bootstrap world startup smoke PASS");
        } finally {
            restore("echo.native.productWorldFolder", previousFolder);
            restore("echo.native.productWorldName", previousName);
            restore("echo.native.productWorldDatapack", previousDatapack);
            restore("echo.native.productWorldAutoOpen", previousAutoOpen);
            restore(EchoNativeBootstrapMain.AUTHORIZED_HANDOFF_PROPERTY, previousHandoff);
        }
    }

    private static void requireBootstrapWorldStartupBridgeAppliesBeforeHandoff() throws Exception {
        Path gameDir = Files.createTempDirectory("echo-agent4-bootstrap-world-startup");
        configure("agent4_bootstrap_world", "Agent 4 Bootstrap Ashfall", "agent4-bootstrap-datapack.zip", true);
        writeValidDatapack(gameDir.resolve("echo-native").resolve("worldgen").resolve("agent4-bootstrap-datapack.zip"));

        EchoNativeBootstrapWorldStartupFlow flow = new EchoNativeBootstrapWorldStartupFlow("echo.native.gameDir");
        Map<String, Object> bridge = flow.apply("ashfall", List.of("--gameDir", gameDir.toString()));
        require(Boolean.TRUE.equals(bridge.get("applied")), "bootstrap world startup bridge should apply");
        require(!Boolean.TRUE.equals(bridge.get("blocked")), "bootstrap world startup bridge should not block valid startup");
        require(Boolean.TRUE.equals(bridge.get("createsFreshProductWorld")), "bootstrap bridge should create fresh product world plan");
        require(Boolean.TRUE.equals(bridge.get("productWorldMarkerWritten")), "bootstrap bridge should write product world marker");
        require(Boolean.TRUE.equals(bridge.get("stagedDatapackReady")), "bootstrap bridge should stage a valid product datapack");
        require(NativeLoaderAshfallWorldStartupService.WORLD_PRESET_ID.equals(bridge.get("worldPreset")),
                "bootstrap bridge should force the Ashfall preset");
        require(!EchoNativeBootstrapWorldStartupFlow.blocksHandoff(Map.of("worldStartupBridge", bridge)),
                "valid bootstrap bridge should allow Minecraft handoff");
    }

    private static void requireBootstrapWorldStartupBridgeBlocksMissingDatapack() throws Exception {
        Path gameDir = Files.createTempDirectory("echo-agent4-bootstrap-world-startup-blocked");
        configure("agent4_bootstrap_missing", "Agent 4 Bootstrap Ashfall", "agent4-missing.zip", true);

        EchoNativeBootstrapWorldStartupFlow flow = new EchoNativeBootstrapWorldStartupFlow("echo.native.gameDir");
        Map<String, Object> bridge = flow.apply("ashfall", List.of("--gameDir", gameDir.toString()));
        require(Boolean.TRUE.equals(bridge.get("blocked")), "missing datapack should block bootstrap handoff");
        require(!Boolean.TRUE.equals(bridge.get("applied")), "missing datapack should not apply startup");
        require("missing_product_datapack".equals(bridge.get("failureKind")),
                "missing datapack should report product datapack failure");
        require(EchoNativeBootstrapWorldStartupFlow.blocksHandoff(Map.of("worldStartupBridge", bridge)),
                "blocked bootstrap bridge should prevent Minecraft handoff");
    }

    private static void requireBootstrapMainWritesWorldStartupActivationEvidence() throws Exception {
        Path gameDir = Files.createTempDirectory("echo-agent4-bootstrap-main-world-startup");
        Path marker = gameDir.resolve("echo-native").resolve("module-activation.json");
        configure("agent4_bootstrap_main_world", "Agent 4 Bootstrap Main Ashfall",
                "agent4-bootstrap-main-datapack.zip", true);
        System.setProperty(
                EchoNativeBootstrapMain.AUTHORIZED_HANDOFF_PROPERTY,
                EchoNativeBootstrapMain.AUTHORIZED_HANDOFF_VALUE
        );
        writeValidDatapack(gameDir.resolve("echo-native").resolve("worldgen").resolve("agent4-bootstrap-main-datapack.zip"));

        EchoNativeBootstrapMain.main(new String[]{
                "--echo-marker", marker.toString(),
                "--echo-pack-id", "ashfall",
                "--gameDir", gameDir.toString()
        });

        String activation = Files.readString(marker, StandardCharsets.UTF_8);
        require(activation.contains("\"worldStartupBridge\""),
                "bootstrap activation marker should include worldStartupBridge");
        require(activation.contains("\"productWorldMarkerWritten\": true"),
                "bootstrap activation marker should prove product marker was written");
        require(activation.contains("\"stagedDatapackReady\": true"),
                "bootstrap activation marker should prove product datapack was staged");
        require(activation.contains("\"worldPreset\": \"" + NativeLoaderAshfallWorldStartupService.WORLD_PRESET_ID + "\""),
                "bootstrap activation marker should record forced Ashfall world preset");
        require(Files.isRegularFile(gameDir.resolve("saves")
                        .resolve("agent4_bootstrap_main_world")
                        .resolve(NativeLoaderAshfallWorldStartupService.PRODUCT_WORLD_MARKER)),
                "bootstrap main should write product world marker before handoff");
    }

    private static void writeValidDatapack(Path zipPath) throws Exception {
        Files.createDirectories(zipPath.getParent());
        Map<String, String> entries = new LinkedHashMap<>();
        entries.put("pack.mcmeta", """
                {
                  "pack": {
                    "description": "Agent 4 bootstrap Ashfall worldgen",
                    "min_format": [101, 1],
                    "max_format": [101, 1]
                  }
                }
                """);
        entries.put("data/echoashfallprotocol/dimension/wasteland_overworld.json", "{}");
        entries.put("data/echoashfallprotocol/dimension/prefall_archives.json", "{}");
        entries.put("data/echoashfallprotocol/dimension_type/prefall_archives.json", "{}");
        entries.put("data/echoashfallprotocol/worldgen/world_preset/ashfall_wasteland.json", "{}");
        entries.put("data/minecraft/worldgen/world_preset/normal.json", "{}");
        entries.put("data/echoashfallprotocol/worldgen/noise_settings/wasteland_overworld.json", "{}");
        entries.put("data/echoashfallprotocol/worldgen/biome/the_wasteland.json", "{}");
        try (ZipOutputStream output = new ZipOutputStream(Files.newOutputStream(zipPath))) {
            for (Map.Entry<String, String> entry : entries.entrySet()) {
                output.putNextEntry(new ZipEntry(entry.getKey()));
                output.write(entry.getValue().getBytes(StandardCharsets.UTF_8));
                output.closeEntry();
            }
        }
    }

    private static void configure(String folder, String name, String datapack, boolean autoOpen) {
        System.setProperty("echo.native.productWorldFolder", folder);
        System.setProperty("echo.native.productWorldName", name);
        System.setProperty("echo.native.productWorldDatapack", datapack);
        System.setProperty("echo.native.productWorldAutoOpen", Boolean.toString(autoOpen));
    }

    private static void restore(String key, String previous) {
        if (previous == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, previous);
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }
}
