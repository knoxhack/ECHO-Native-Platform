package dev.echo.nativeplatform.loader;

import dev.echo.nativeplatform.loader.NativeLoaderAshfallWorldStartupService.StartupAction;
import dev.echo.nativeplatform.loader.NativeLoaderAshfallWorldStartupService.StartupPlan;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public final class EchoNativeAgent4WorldStartupSmokeMain {
    private EchoNativeAgent4WorldStartupSmokeMain() {
    }

    public static void main(String[] args) throws Exception {
        String previousFolder = System.getProperty("echo.native.productWorldFolder");
        String previousName = System.getProperty("echo.native.productWorldName");
        String previousDatapack = System.getProperty("echo.native.productWorldDatapack");
        String previousGameMode = System.getProperty("echo.native.productWorldGameMode");
        String previousModuleClasspath = System.getProperty("echo.native.moduleClasspath");
        try {
            requireFreshProductWorldPlan();
            requireMarkedProductWorldReopenPlan();
            requireDispatchRejectsUnsafeOrStalePlans();
            requireUnsafeProductWorldFolderGuard();
            requireUnsafeProductDatapackFileGuard();
            requireOldVanillaSaveGuard();
            requireInvalidProductMarkerSaveGuard();
            requireMismatchedProductMarkerFolderGuard();
            requireMissingDatapackBlocksVanillaFallback();
            requireUnsafeNativeRegistryDatapackBlocksVanillaFallback();
            requireStaleSaveDatapackIsRestagedFromCleanSource();
            requireResourceHostPreWorldMountsRequireRealFiles();
            requireBundledProductDatapackMaterializationSanitizesAdaptNoise();
            requireBundledProductDatapackMaterializationSanitizesUserLogRegistryRefs();
            if (args.length > 0) {
                requirePackagedAshfallDatapackStartupPlan(Path.of(args[0]));
            }
            writeJsonReportIfRequested(args.length > 0);
            System.out.println("agent4 native ashfall world startup smoke PASS");
        } finally {
            restore("echo.native.productWorldFolder", previousFolder);
            restore("echo.native.productWorldName", previousName);
            restore("echo.native.productWorldDatapack", previousDatapack);
            restore("echo.native.productWorldGameMode", previousGameMode);
            restore("echo.native.moduleClasspath", previousModuleClasspath);
        }
    }

    private static void writeJsonReportIfRequested(boolean packagedDatapackVerified) throws Exception {
        String configured = System.getProperty("echo.native.agent4.worldStartupReport");
        if (configured == null || configured.isBlank()) {
            return;
        }
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("schema", "echo.native.agent4.world_startup_smoke.v1");
        report.put("generatedAt", "1970-01-01T00:00:00Z");
        report.put("status", "PASS");
        report.put("runtime", "echo_native");
        report.put("moduleIds", List.of("echoashfallprotocol", "echobiomecore", "echoworldcore", "echoblockworks"));
        report.put("featureBuckets", List.of("worldgen", "blocks", "items", "save_data"));
        report.put("trustedMutations", List.of(
                "fresh product world plan creates Ashfall world",
                "marked product world plan reopens product save",
                "unsafe vanilla fallback and unsafe registry datapack paths are rejected",
                "pre-world datapack/resource-pack mounts require existing files",
                "stale save-local datapack is restaged from clean Native source"
        ));
        report.put("visibleRoutes", List.of("NativeLoaderAshfallWorldStartupService.prepare"));
        report.put("saveEvidence", List.of(
                "product world open marker, staged datapack, level.dat, player, and level evidence verified"
        ));
        report.put("networkEvidence", List.of());
        report.put("packagedDatapackVerified", packagedDatapackVerified);
        report.put("blockers", List.of());
        NativeLoaderJsonSupport.writeAtomically(Path.of(configured), report);
    }

    private static void requireFreshProductWorldPlan() throws Exception {
        Path gameDir = Files.createTempDirectory("echo-agent4-world-startup-create");
        configure("agent4_create_world", "Agent 4 Ashfall", "agent4-ashfall-datapack.zip", "survival");
        writeValidDatapack(gameDir.resolve("echo-native").resolve("worldgen").resolve("agent4-ashfall-datapack.zip"));

        StartupPlan plan = NativeLoaderAshfallWorldStartupService.prepare(gameDir);
        require(plan.action() == StartupAction.CREATE_NEW, "fresh product path should create a new Ashfall world");
        require(plan.createsFreshProductWorld(), "plan should identify fresh product world creation");
        require(plan.worldPreset().equals(NativeLoaderAshfallWorldStartupService.WORLD_PRESET_ID),
                "fresh product plan should force the Ashfall world preset");
        require(!Boolean.TRUE.equals(plan.toReport().get("vanillaWorldCreationFallbackAllowed")),
                "fresh product plan should disallow vanilla world creation fallback");
        require(Files.isRegularFile(plan.stagedDatapack()), "fresh product plan should stage the required datapack");
        require(NativeLoaderAshfallWorldStartupService.isValidProductDatapack(plan.stagedDatapack()),
                "fresh product staged datapack should contain required worldgen entries");
        Path dispatchMarker = plan.saveDir().resolve(NativeLoaderAshfallWorldStartupService.PRODUCT_WORLD_OPEN_MARKER);
        require(!Files.exists(dispatchMarker), "fresh prepare should not claim Minecraft world-open dispatch before menu dispatch");
        require(NativeLoaderAshfallWorldStartupService.recordProductWorldOpenDispatch(
                        plan,
                        "Minecraft.createWorldOpenFlows.createFreshLevel"),
                "fresh product world dispatch marker should be written by Native Loader");
        String dispatch = Files.readString(dispatchMarker, StandardCharsets.UTF_8);
        require(dispatch.contains("\"minecraftDispatch\": \"Minecraft.createWorldOpenFlows.createFreshLevel\""),
                "fresh dispatch marker should record the Minecraft world creation call");
        require(dispatch.contains("\"nativeLoaderOwnedWorldPolicy\": true"),
                "fresh dispatch marker should record Native Loader world ownership");
        require(dispatch.contains("\"vanillaWorldCreationFallbackAllowed\": false"),
                "fresh dispatch marker should disallow vanilla fallback");
        Map<String, Object> preparedEvidence = NativeLoaderAshfallWorldStartupService.liveProductWorldEvidence(gameDir, false, false);
        require(!Boolean.TRUE.equals(preparedEvidence.get("nativeProductWorldOpened")),
                "prepared product world evidence should not claim the world opened before level.dat exists");
        Files.writeString(plan.saveDir().resolve("level.dat"), "opened ashfall level", StandardCharsets.UTF_8);
        Map<String, Object> openedEvidence = NativeLoaderAshfallWorldStartupService.liveProductWorldEvidence(gameDir, true, true);
        require(Boolean.TRUE.equals(openedEvidence.get("nativeProductWorldOpened")),
                "opened product world evidence should require marker, datapack, level.dat, player, and level");
        require(Boolean.TRUE.equals(openedEvidence.get("productWorldLevelDatPresent")),
                "opened product world evidence should report level.dat");
        String marker = Files.readString(plan.marker(), StandardCharsets.UTF_8);
        require(marker.contains("\"worldPreset\": \"" + NativeLoaderAshfallWorldStartupService.WORLD_PRESET_ID + "\""),
                "fresh product marker should record the forced world preset");
        require(marker.contains("\"ownedBy\": \"NativeLoaderAshfallWorldStartupService\""),
                "fresh product marker should record Native Loader world startup ownership");
    }

    private static void requireMarkedProductWorldReopenPlan() throws Exception {
        Path gameDir = Files.createTempDirectory("echo-agent4-world-startup-open");
        configure("agent4_marked_world", "Agent 4 Ashfall", "agent4-open-datapack.zip", "adventure");
        writeValidDatapack(gameDir.resolve("datapacks").resolve("agent4-open-datapack.zip"));

        StartupPlan created = NativeLoaderAshfallWorldStartupService.prepare(gameDir);
        Files.writeString(created.saveDir().resolve("level.dat"), "marked ashfall level", StandardCharsets.UTF_8);

        StartupPlan opened = NativeLoaderAshfallWorldStartupService.prepare(gameDir);
        require(opened.action() == StartupAction.OPEN_EXISTING, "marked product world should reopen, not recreate");
        require(opened.opensMarkedProductWorld(), "plan should identify marked product world open");
        require(opened.gameMode().equals("adventure"), "plan should preserve configured product game mode");
        require(NativeLoaderAshfallWorldStartupService.recordProductWorldOpenDispatch(
                        opened,
                        "Minecraft.createWorldOpenFlows.openWorld"),
                "marked product world dispatch marker should be written by Native Loader");
        String dispatch = Files.readString(
                opened.saveDir().resolve(NativeLoaderAshfallWorldStartupService.PRODUCT_WORLD_OPEN_MARKER),
                StandardCharsets.UTF_8);
        require(dispatch.contains("\"minecraftDispatch\": \"Minecraft.createWorldOpenFlows.openWorld\""),
                "marked product world dispatch marker should record the Minecraft open call");
    }

    private static void requireDispatchRejectsUnsafeOrStalePlans() throws Exception {
        Path gameDir = Files.createTempDirectory("echo-agent4-world-startup-dispatch-guard");
        configure("agent4_dispatch_guard_world", "Agent 4 Ashfall", "agent4-dispatch-guard.zip", "survival");
        writeValidDatapack(gameDir.resolve("echo-native").resolve("worldgen").resolve("agent4-dispatch-guard.zip"));

        StartupPlan plan = NativeLoaderAshfallWorldStartupService.prepare(gameDir);
        StartupPlan unsafeFolderPlan = new StartupPlan(
                StartupAction.CREATE_NEW,
                "../agent4_dispatch_guard_escape",
                plan.worldName(),
                plan.datapackFile(),
                plan.worldPreset(),
                plan.gameMode(),
                "",
                List.of(),
                plan.saveDir(),
                plan.stagedDatapack(),
                plan.marker(),
                plan.datapackStage());
        require(!NativeLoaderAshfallWorldStartupService.recordProductWorldOpenDispatch(
                        unsafeFolderPlan,
                        "Minecraft.createWorldOpenFlows.createFreshLevel"),
                "dispatch marker should reject unsafe product folder plans");

        StartupPlan staleDatapackPlan = new StartupPlan(
                StartupAction.CREATE_NEW,
                plan.folder(),
                plan.worldName(),
                "different-datapack.zip",
                plan.worldPreset(),
                plan.gameMode(),
                "",
                List.of(),
                plan.saveDir(),
                plan.stagedDatapack(),
                plan.marker(),
                plan.datapackStage());
        require(!NativeLoaderAshfallWorldStartupService.recordProductWorldOpenDispatch(
                        staleDatapackPlan,
                        "Minecraft.createWorldOpenFlows.createFreshLevel"),
                "dispatch marker should reject stale or mismatched datapack plans");
        require(Files.notExists(plan.saveDir().resolve(NativeLoaderAshfallWorldStartupService.PRODUCT_WORLD_OPEN_MARKER)),
                "rejected dispatch plans must not write a product world open marker");
    }

    private static void requireUnsafeProductWorldFolderGuard() throws Exception {
        Path gameDir = Files.createTempDirectory("echo-agent4-world-startup-folder-escape");
        configure("../agent4_escape_save", "Agent 4 Ashfall", "agent4-folder-escape-datapack.zip", "survival");
        writeValidDatapack(gameDir.resolve("echo-native").resolve("worldgen").resolve("agent4-folder-escape-datapack.zip"));

        StartupPlan plan = NativeLoaderAshfallWorldStartupService.prepare(gameDir);
        require(plan.action() == StartupAction.BLOCKED, "unsafe product world folder should block product startup");
        require(plan.blocksVanillaFallback(), "unsafe product world folder should prevent fallback");
        require(plan.failureKind().equals("invalid_product_world_folder"),
                "unsafe product world folder should report invalid folder failure");
        require(Files.notExists(gameDir.resolve("agent4_escape_save")),
                "unsafe product world folder must not resolve or create paths outside saves");
        Map<String, Object> liveEvidence = NativeLoaderAshfallWorldStartupService.liveProductWorldEvidence(gameDir, true, true);
        require(!Boolean.TRUE.equals(liveEvidence.get("productWorldFolderValid")),
                "unsafe product world folder live evidence should report invalid folder");
        require(!Boolean.TRUE.equals(liveEvidence.get("nativeProductWorldOpened")),
                "unsafe product world folder live evidence must not claim an opened Ashfall product world");

        for (String placeholderFolder : List.of("$world", "${world}", "%WORLD%")) {
            configure(placeholderFolder, "Agent 4 Ashfall", "agent4-folder-escape-datapack.zip", "survival");
            StartupPlan placeholderPlan = NativeLoaderAshfallWorldStartupService.prepare(gameDir);
            require(placeholderPlan.action() == StartupAction.BLOCKED,
                    "unresolved placeholder product world folder should block product startup: " + placeholderFolder);
            require(placeholderPlan.failureKind().equals("invalid_product_world_folder"),
                    "unresolved placeholder product world folder should report invalid folder failure: " + placeholderFolder);
            require(Files.notExists(gameDir.resolve("saves").resolve(placeholderFolder)),
                    "unresolved placeholder product world folder must not create a save folder: " + placeholderFolder);
        }
    }

    private static void requireUnsafeProductDatapackFileGuard() throws Exception {
        Path gameDir = Files.createTempDirectory("echo-agent4-world-startup-datapack-escape");
        configure("agent4_datapack_escape_save", "Agent 4 Ashfall", "../agent4-escape-datapack.zip", "survival");
        writeValidDatapack(gameDir.resolve("echo-native").resolve("worldgen").resolve("agent4-safe-source.zip"));

        StartupPlan plan = NativeLoaderAshfallWorldStartupService.prepare(gameDir);
        require(plan.action() == StartupAction.BLOCKED, "unsafe product datapack file should block product startup");
        require(plan.blocksVanillaFallback(), "unsafe product datapack file should prevent fallback");
        require(plan.failureKind().equals("invalid_product_world_datapack_file"),
                "unsafe product datapack file should report invalid datapack filename failure");
        require(Files.notExists(gameDir.resolve("saves").resolve("agent4_datapack_escape_save").resolve("agent4-escape-datapack.zip")),
                "unsafe product datapack file must not resolve or create paths outside save datapacks");
        Map<String, Object> liveEvidence = NativeLoaderAshfallWorldStartupService.liveProductWorldEvidence(gameDir, true, true);
        require(!Boolean.TRUE.equals(liveEvidence.get("productWorldDatapackFileValid")),
                "unsafe product datapack live evidence should report invalid datapack filename");
        require(!Boolean.TRUE.equals(liveEvidence.get("nativeProductWorldOpened")),
                "unsafe product datapack live evidence must not claim an opened Ashfall product world");
    }

    private static void requireOldVanillaSaveGuard() throws Exception {
        Path gameDir = Files.createTempDirectory("echo-agent4-world-startup-vanilla");
        configure("agent4_vanilla_save", "Agent 4 Ashfall", "agent4-vanilla-datapack.zip", "survival");
        Files.createDirectories(gameDir.resolve("saves").resolve("agent4_vanilla_save"));
        Files.writeString(gameDir.resolve("saves").resolve("agent4_vanilla_save").resolve("level.dat"),
                "vanilla save", StandardCharsets.UTF_8);
        writeValidDatapack(gameDir.resolve("echo-native").resolve("worldgen").resolve("agent4-vanilla-datapack.zip"));

        StartupPlan plan = NativeLoaderAshfallWorldStartupService.prepare(gameDir);
        require(plan.action() == StartupAction.BLOCKED, "old vanilla save should block product startup");
        require(plan.blocksVanillaFallback(), "old vanilla save block should prevent fallback");
        require(plan.failureKind().equals("old_vanilla_save_guard"), "old vanilla save should report guard failure");
    }

    private static void requireInvalidProductMarkerSaveGuard() throws Exception {
        Path gameDir = Files.createTempDirectory("echo-agent4-world-startup-invalid-marker");
        configure("agent4_invalid_marker_save", "Agent 4 Ashfall", "agent4-invalid-marker-datapack.zip", "survival");
        Path saveDir = gameDir.resolve("saves").resolve("agent4_invalid_marker_save");
        Files.createDirectories(saveDir.resolve("datapacks"));
        Files.writeString(saveDir.resolve("level.dat"), "vanilla save with forged marker", StandardCharsets.UTF_8);
        Files.writeString(saveDir.resolve(NativeLoaderAshfallWorldStartupService.PRODUCT_WORLD_MARKER),
                "{\"schema\":\"echo.native.product_world.v1\",\"product\":\"minecraft\"}", StandardCharsets.UTF_8);
        writeValidDatapack(gameDir.resolve("echo-native").resolve("worldgen").resolve("agent4-invalid-marker-datapack.zip"));
        writeValidDatapack(saveDir.resolve("datapacks").resolve("agent4-invalid-marker-datapack.zip"));

        StartupPlan plan = NativeLoaderAshfallWorldStartupService.prepare(gameDir);
        require(plan.action() == StartupAction.BLOCKED, "invalid product marker save should block product startup");
        require(plan.blocksVanillaFallback(), "invalid product marker block should prevent fallback");
        require(plan.failureKind().equals("old_vanilla_save_guard"),
                "invalid product marker save should use the old/untrusted save guard");
        Map<String, Object> liveEvidence = NativeLoaderAshfallWorldStartupService.liveProductWorldEvidence(gameDir, true, true);
        require(Boolean.TRUE.equals(liveEvidence.get("productWorldMarkerWritten")),
                "invalid product marker live evidence should still report marker file presence");
        require(!Boolean.TRUE.equals(liveEvidence.get("productWorldMarkerValid")),
                "invalid product marker live evidence should distinguish invalid marker content");
        require(!Boolean.TRUE.equals(liveEvidence.get("nativeProductWorldOpened")),
                "invalid product marker live evidence must not claim an opened Ashfall product world");
    }

    private static void requireMismatchedProductMarkerFolderGuard() throws Exception {
        Path gameDir = Files.createTempDirectory("echo-agent4-world-startup-marker-folder");
        configure("agent4_marker_folder_save", "Agent 4 Ashfall", "agent4-marker-folder-datapack.zip", "survival");
        Path saveDir = gameDir.resolve("saves").resolve("agent4_marker_folder_save");
        Files.createDirectories(saveDir.resolve("datapacks"));
        Files.writeString(saveDir.resolve("level.dat"), "save with copied product marker", StandardCharsets.UTF_8);
        Files.writeString(saveDir.resolve(NativeLoaderAshfallWorldStartupService.PRODUCT_WORLD_MARKER),
                "{\n"
                        + "  \"schema\": \"echo.native.product_world.v1\",\n"
                        + "  \"product\": \"" + NativeLoaderAshfallWorldStartupService.PRODUCT_ID + "\",\n"
                        + "  \"worldPreset\": \"" + NativeLoaderAshfallWorldStartupService.WORLD_PRESET_ID + "\",\n"
                        + "  \"datapack\": \"agent4-marker-folder-datapack.zip\",\n"
                        + "  \"folder\": \"different_product_folder\",\n"
                        + "  \"ownedBy\": \"NativeLoaderAshfallWorldStartupService\"\n"
                        + "}\n",
                StandardCharsets.UTF_8);
        writeValidDatapack(gameDir.resolve("echo-native").resolve("worldgen").resolve("agent4-marker-folder-datapack.zip"));
        writeValidDatapack(saveDir.resolve("datapacks").resolve("agent4-marker-folder-datapack.zip"));

        StartupPlan plan = NativeLoaderAshfallWorldStartupService.prepare(gameDir);
        require(plan.action() == StartupAction.BLOCKED, "mismatched product marker folder should block product startup");
        require(plan.blocksVanillaFallback(), "mismatched product marker folder should prevent fallback");
        require(plan.failureKind().equals("old_vanilla_save_guard"),
                "mismatched product marker folder should use the old/untrusted save guard");
        Map<String, Object> liveEvidence = NativeLoaderAshfallWorldStartupService.liveProductWorldEvidence(gameDir, true, true);
        require(!Boolean.TRUE.equals(liveEvidence.get("productWorldMarkerFolderMatches")),
                "mismatched product marker live evidence should distinguish copied marker folder");
        require(!Boolean.TRUE.equals(liveEvidence.get("productWorldMarkerValid")),
                "mismatched product marker live evidence should reject copied markers");
        require(!Boolean.TRUE.equals(liveEvidence.get("nativeProductWorldOpened")),
                "mismatched product marker live evidence must not claim an opened Ashfall product world");
    }

    private static void requireMissingDatapackBlocksVanillaFallback() throws Exception {
        Path gameDir = Files.createTempDirectory("echo-agent4-world-startup-missing");
        configure("agent4_missing_datapack", "Agent 4 Ashfall", "agent4-missing-datapack.zip", "survival");

        StartupPlan plan = NativeLoaderAshfallWorldStartupService.prepare(gameDir);
        require(plan.action() == StartupAction.BLOCKED, "missing datapack should block product startup");
        require(plan.failureKind().equals("missing_product_datapack"), "missing datapack should report product datapack failure");
        require(!Boolean.TRUE.equals(plan.toReport().get("vanillaWorldCreationFallbackAllowed")),
                "missing datapack block should disallow vanilla fallback");
    }

    private static void requireUnsafeNativeRegistryDatapackBlocksVanillaFallback() throws Exception {
        Path gameDir = Files.createTempDirectory("echo-agent4-world-startup-unsafe-registry");
        configure("agent4_unsafe_registry_datapack", "Agent 4 Ashfall",
                "agent4-unsafe-registry-datapack.zip", "survival");
        Path datapack = gameDir.resolve("echo-native").resolve("worldgen").resolve("agent4-unsafe-registry-datapack.zip");
        writeUnsafeRegistryDatapack(datapack);

        require(!NativeLoaderAshfallWorldStartupService.isValidProductDatapack(datapack),
                "datapack validation should reject native-unregistered processor block references");
        StartupPlan plan = NativeLoaderAshfallWorldStartupService.prepare(gameDir);
        require(plan.action() == StartupAction.BLOCKED,
                "unsafe native registry datapack should block product startup");
        require(plan.failureKind().equals("missing_product_datapack"),
                "unsafe native registry datapack should fail as missing a usable product datapack");
        require(!Boolean.TRUE.equals(plan.toReport().get("vanillaWorldCreationFallbackAllowed")),
                "unsafe native registry datapack block should disallow vanilla fallback");
    }

    private static void requireStaleSaveDatapackIsRestagedFromCleanSource() throws Exception {
        Path gameDir = Files.createTempDirectory("echo-agent4-world-startup-stale-save-datapack");
        configure("agent4_stale_save_datapack", "Agent 4 Ashfall",
                "agent4-stale-save-datapack.zip", "survival");
        Path cleanSource = gameDir.resolve("echo-native").resolve("worldgen").resolve("agent4-stale-save-datapack.zip");
        Path saveDir = gameDir.resolve("saves").resolve("agent4_stale_save_datapack");
        Path staleSaveDatapack = saveDir.resolve("datapacks").resolve("agent4-stale-save-datapack.zip");
        writeValidDatapack(cleanSource);
        writeUnsafeRegistryDatapack(staleSaveDatapack);
        Files.writeString(saveDir.resolve("level.dat"), "marked product save with stale datapack", StandardCharsets.UTF_8);
        Files.writeString(saveDir.resolve(NativeLoaderAshfallWorldStartupService.PRODUCT_WORLD_MARKER),
                "{\n"
                        + "  \"schema\": \"echo.native.product_world.v1\",\n"
                        + "  \"product\": \"" + NativeLoaderAshfallWorldStartupService.PRODUCT_ID + "\",\n"
                        + "  \"worldPreset\": \"" + NativeLoaderAshfallWorldStartupService.WORLD_PRESET_ID + "\",\n"
                        + "  \"datapack\": \"agent4-stale-save-datapack.zip\",\n"
                        + "  \"folder\": \"agent4_stale_save_datapack\",\n"
                        + "  \"ownedBy\": \"NativeLoaderAshfallWorldStartupService\"\n"
                        + "}\n",
                StandardCharsets.UTF_8);

        require(!NativeLoaderAshfallWorldStartupService.isValidProductDatapack(staleSaveDatapack),
                "stale save-local datapack fixture should be invalid before migration");
        StartupPlan plan = NativeLoaderAshfallWorldStartupService.prepare(gameDir);
        require(plan.action() == StartupAction.OPEN_EXISTING,
                "marked product save with stale datapack should reopen after restaging from the clean product source");
        require(NativeLoaderAshfallWorldStartupService.isValidProductDatapack(plan.stagedDatapack()),
                "restaged save-local datapack should be valid after migration");
        @SuppressWarnings("unchecked")
        Map<String, Object> datapackStage = (Map<String, Object>) plan.toReport().get("datapackStage");
        require("native_loader_staged_source".equals(datapackStage.get("sourceKind")),
                "stale save-local datapack should not be reused as an existing save datapack: " + datapackStage);
        require(Integer.valueOf(1).equals(datapackStage.get("copiedCount")),
                "stale save-local datapack migration should copy exactly one clean source datapack: " + datapackStage);
    }

    private static void requireResourceHostPreWorldMountsRequireRealFiles() throws Exception {
        Path root = Files.createTempDirectory("echo-agent4-resource-host-real-files");
        NativeLoaderResourceHost host = new NativeLoaderResourceHost();
        require(host.registerPreWorldCreationMount(
                "echoashfallprotocol",
                "missing_datapack",
                "data_pack",
                root.resolve("missing-datapack.zip"),
                Map.of()).name().equals("FAILED"),
                "resource host must reject missing pre-world datapack files");
        require(host.registerPreWorldCreationMount(
                "echoashfallprotocol",
                "missing_resource_pack",
                "resource_pack",
                root.resolve("missing-resource-pack.zip"),
                Map.of()).name().equals("FAILED"),
                "resource host must reject missing pre-world resource-pack files");
        require(host.toReport().get("mountedPreWorldCreationResourceCount").equals(0),
                "missing pre-world pack paths must not inflate mount counts");

        Path datapack = root.resolve("echo-native-ashfall-datapack.zip");
        Path resourcePack = root.resolve("echo-native-ashfall-resources.zip");
        Files.writeString(datapack, "agent4 datapack proof", StandardCharsets.UTF_8);
        Files.writeString(resourcePack, "agent4 resource pack proof", StandardCharsets.UTF_8);
        require(host.registerPreWorldCreationMount(
                "echoashfallprotocol",
                "real_datapack",
                "data_pack",
                datapack,
                Map.of("mountPhase", "before_registry_and_world_creation")).name().equals("MUTATED"),
                "resource host must accept an existing pre-world datapack file");
        require(host.registerPreWorldCreationMount(
                "echoashfallprotocol",
                "real_resource_pack",
                "resource_pack",
                resourcePack,
                Map.of("mountPhase", "before_registry_and_world_creation")).name().equals("MUTATED"),
                "resource host must accept an existing pre-world resource-pack file");
        Map<String, Object> report = host.toReport();
        require(report.get("mountedPreWorldCreationResourceCount").equals(2),
                "real pre-world pack files should count as mounted resources");
        require(report.get("mountedDataPackResourceCount").equals(1),
                "real pre-world datapack file should count as a datapack mount");
        require(report.get("mountedResourcePackResourceCount").equals(1),
                "real pre-world resource-pack file should count as a resource-pack mount");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> resources = (List<Map<String, Object>>) report.get("resources");
        for (Map<String, Object> resource : resources) {
            @SuppressWarnings("unchecked")
            Map<String, Object> evidence = (Map<String, Object>) resource.get("evidence");
            require(Boolean.TRUE.equals(evidence.get("mountFilePresent")),
                    "accepted pre-world resource mount evidence must prove the file existed");
        }
    }

    private static void requirePackagedAshfallDatapackStartupPlan(Path datapack) throws Exception {
        Path source = datapack.toAbsolutePath().normalize();
        require(Files.isRegularFile(source), "packaged Ashfall datapack should exist: " + source);
        require(NativeLoaderAshfallWorldStartupService.isValidProductDatapack(source),
                "packaged Ashfall datapack should contain required worldgen entries");
        requirePackagedAshfallDatapackContents(source);

        Path gameDir = Files.createTempDirectory("echo-agent4-world-startup-packaged");
        configure("agent4_packaged_ashfall_world", "Agent 4 Packaged Ashfall",
                source.getFileName().toString(), "survival");
        Path stagedSource = gameDir.resolve("echo-native").resolve("worldgen").resolve(source.getFileName());
        Files.createDirectories(stagedSource.getParent());
        Files.copy(source, stagedSource);

        StartupPlan plan = NativeLoaderAshfallWorldStartupService.prepare(gameDir);
        require(plan.action() == StartupAction.CREATE_NEW,
                "packaged Ashfall datapack should create a fresh product world plan");
        require(plan.worldPreset().equals(NativeLoaderAshfallWorldStartupService.WORLD_PRESET_ID),
                "packaged Ashfall datapack plan should force the Ashfall world preset");
        require(NativeLoaderAshfallWorldStartupService.isValidProductDatapack(plan.stagedDatapack()),
                "packaged Ashfall datapack should remain valid after Native Loader staging");
        require(plan.toReport().containsKey("nativeLoaderOwnedWorldPolicy"),
                "packaged Ashfall datapack plan should report Native Loader world policy ownership");
        require(!Boolean.TRUE.equals(plan.toReport().get("vanillaWorldCreationFallbackAllowed")),
                "packaged Ashfall datapack plan should disallow vanilla world creation fallback");
    }

    private static void requireBundledProductDatapackMaterializationSanitizesAdaptNoise() throws Exception {
        Path gameDir = Files.createTempDirectory("echo-agent4-world-startup-bundled");
        Path moduleRoot = Files.createTempDirectory("echo-agent4-world-startup-bundled-module");
        configure("agent4_bundled_materialized", "Agent 4 Bundled Materialized Ashfall",
                "agent4-bundled-materialized.zip", "survival");
        System.setProperty("echo.native.moduleClasspath", moduleRoot.toString());
        writeBundledProductDatapackResources(moduleRoot);

        StartupPlan plan = NativeLoaderAshfallWorldStartupService.prepare(gameDir);
        require(plan.action() == StartupAction.CREATE_NEW,
                "bundled classpath product datapack should create a fresh product world plan");
        @SuppressWarnings("unchecked")
        Map<String, Object> datapackStage = (Map<String, Object>) plan.toReport().get("datapackStage");
        require("native_loader_bundled_product_worldgen".equals(datapackStage.get("sourceKind")),
                "bundled classpath product datapack should be materialized by Native Loader: " + plan.toReport());
        require(NativeLoaderAshfallWorldStartupService.isValidProductDatapack(plan.stagedDatapack()),
                "bundled classpath materialized datapack should contain required worldgen entries");
        requirePackagedAshfallDatapackContents(plan.stagedDatapack());
    }

    private static void requireBundledProductDatapackMaterializationSanitizesUserLogRegistryRefs() throws Exception {
        Path gameDir = Files.createTempDirectory("echo-agent4-world-startup-bundled-user-log");
        Path moduleRoot = Files.createTempDirectory("echo-agent4-world-startup-bundled-user-log-module");
        configure("agent4_bundled_user_log_materialized", "Agent 4 Bundled User Log Ashfall",
                "agent4-bundled-user-log-materialized.zip", "survival");
        System.setProperty("echo.native.moduleClasspath", moduleRoot.toString());
        writeBundledProductDatapackResources(moduleRoot);

        StartupPlan plan = NativeLoaderAshfallWorldStartupService.prepare(gameDir);
        require(plan.action() == StartupAction.CREATE_NEW,
                "bundled user-log product datapack should create a fresh product world plan");
        require(NativeLoaderAshfallWorldStartupService.isValidProductDatapack(plan.stagedDatapack()),
                "bundled user-log materialized datapack should validate after registry-safe sanitization");
        requirePackagedAshfallDatapackContents(plan.stagedDatapack());
    }

    private static void requirePackagedAshfallDatapackContents(Path datapack) throws Exception {
        Set<String> names = new HashSet<>();
        String packMcmeta = "";
        boolean structureTemplatePresent = false;
        boolean unsafeRegistryIdsPresent = false;
        boolean unsafeUserLogRegistryRefsPresent = false;
        boolean legacyUnboundMinecraftWorldgenIdsPresent = false;
        boolean adaptNoisePresent = false;
        boolean structureReadmePresent = false;
        try (ZipFile zip = new ZipFile(datapack.toFile())) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String name = entry.getName().replace('\\', '/');
                if (entry.isDirectory()) {
                    continue;
                }
                names.add(name);
                String lower = name.toLowerCase(java.util.Locale.ROOT);
                if (name.startsWith("data/echoashfallprotocol/structures/") && name.endsWith(".nbt")) {
                    structureTemplatePresent = true;
                }
                if ((name.startsWith("data/echoashfallprotocol/structure/")
                        || name.startsWith("data/echoashfallprotocol/structures/"))
                        && (lower.endsWith(".md") || lower.endsWith(".txt") || lower.contains("/readme"))) {
                    structureReadmePresent = true;
                }
                if (name.endsWith(".json") || "pack.mcmeta".equals(name)) {
                    String text = new String(zip.getInputStream(entry).readAllBytes(), StandardCharsets.UTF_8);
                    if ("pack.mcmeta".equals(name)) {
                        packMcmeta = text;
                    }
                    adaptNoisePresent |= text.contains("\"adapt_noise\"");
                    unsafeRegistryIdsPresent |= text.contains("echoblockworks:")
                            || text.contains("\"minecraft:chain\"");
                    unsafeUserLogRegistryRefsPresent |= text.contains("minecraft:iron_bars_command_block")
                            || text.contains("minecraft:iron_barss")
                            || text.contains("\"echoashfallprotocol:wasteland_stone\"")
                            || text.contains("\"echoashfallprotocol:debris_block\"")
                            || text.contains("\"echoashfallprotocol:toxic_puddle\"")
                            || text.contains("\"echoashfallprotocol:toxic_moss\"")
                            || text.contains("\"#echoashfallprotocol:toxic_puddle\"")
                            || text.contains("\"#echoashfallprotocol:toxic_moss\"");
                    legacyUnboundMinecraftWorldgenIdsPresent |= NativeLoaderAshfallWorldStartupService
                            .LEGACY_UNBOUND_MINECRAFT_WORLDGEN_FEATURE_IDS.stream()
                            .anyMatch(id -> text.contains("\"" + id + "\""));
                }
            }
        }
        require(packMcmeta.contains("\"min_format\"") && packMcmeta.contains("\"max_format\""),
                "packaged Ashfall datapack should use Native Loader pack range metadata");
        require(!packMcmeta.contains("\"pack_format\"") || !packMcmeta.contains("84"),
                "packaged Ashfall datapack should not keep stale source pack_format 84 metadata");
        require(names.contains("data/minecraft/worldgen/world_preset/normal.json"),
                "packaged Ashfall datapack should include mirrored vanilla normal world preset");
        require(names.contains("data/echoashfallprotocol/worldgen/world_preset/ashfall_wasteland.json"),
                "packaged Ashfall datapack should include Ashfall product world preset");
        require(structureTemplatePresent,
                "packaged Ashfall datapack should include mirrored plural structure templates");
        require(!structureReadmePresent,
                "packaged Ashfall datapack should not include README/text entries under structure templates");
        require(!adaptNoisePresent,
                "packaged Ashfall datapack should remove adapt_noise from staged worldgen JSON");
        require(!unsafeRegistryIdsPresent,
                "packaged Ashfall datapack should sanitize native-unregistered registry ids from JSON output");
        require(!unsafeUserLogRegistryRefsPresent,
                "packaged Ashfall datapack should sanitize user-log missing registry refs from JSON output");
        require(!legacyUnboundMinecraftWorldgenIdsPresent,
                "packaged Ashfall datapack should not reference unbound legacy minecraft worldgen feature ids");
    }

    private static void writeValidDatapack(Path zipPath) throws Exception {
        Files.createDirectories(zipPath.getParent());
        Map<String, String> entries = new LinkedHashMap<>();
        entries.put("pack.mcmeta", """
                {
                  "pack": {
                    "description": "Agent 4 Ashfall product worldgen",
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

    private static void writeBundledProductDatapackResources(Path root) throws Exception {
        Map<String, String> entries = new LinkedHashMap<>();
        entries.put("data/echoashfallprotocol/dimension/wasteland_overworld.json", "{}");
        entries.put("data/echoashfallprotocol/dimension/prefall_archives.json", "{}");
        entries.put("data/echoashfallprotocol/dimension_type/prefall_archives.json", "{}");
        entries.put("data/echoashfallprotocol/worldgen/world_preset/ashfall_wasteland.json", "{}");
        entries.put("data/minecraft/worldgen/world_preset/normal.json", "{}");
        entries.put("data/echoashfallprotocol/worldgen/noise_settings/wasteland_overworld.json", "{}");
        entries.put("data/echoashfallprotocol/worldgen/biome/the_wasteland.json", "{}");
        entries.put("data/echoashfallprotocol/worldgen/structure/bundled_probe.json", """
                {
                  "type": "minecraft:jigsaw",
                  "biomes": "#echoashfallprotocol:has_structure/bundled_probe",
                  "adapt_noise": true,
                  "spawn_overrides": {},
                  "step": "surface_structures",
                  "start_pool": "echoashfallprotocol:bundled_probe",
                  "size": 1,
                  "start_height": {"absolute": 0},
                  "max_distance_from_center": 16
                }
                """);
        entries.put("data/echoashfallprotocol/structures/bundled_probe.nbt", "agent4-bundled-structure-template");
        entries.put("data/echoashfallprotocol/worldgen/processor_list/bundled_probe.json", """
                {
                  "processors": [
                    {
                      "processor_type": "minecraft:rule",
                      "rules": [
                        {
                          "location_predicate": {"predicate_type": "minecraft:always_true"},
                          "input_predicate": {
                            "predicate_type": "minecraft:block_match",
                            "block": "echoblockworks:ashstone_raw"
                          },
                          "output_state": {"Name": "echoblockworks:hanging_wire"}
                        }
                      ]
                    }
                  ]
                }
                """);
        entries.put("data/echoashfallprotocol/worldgen/configured_feature/user_log_bad_blocks.json", """
                {
                  "type": "minecraft:simple_block",
                  "config": {
                    "to_place": {
                      "type": "minecraft:simple_state_provider",
                      "state": {"Name": "echoashfallprotocol:wasteland_stone"}
                    }
                  },
                  "blocks": [
                    "echoashfallprotocol:debris_block"
                  ]
                }
                """);
        entries.put("data/minecraft/tags/block/dragon_immune.json", """
                {
                  "replace": false,
                  "values": [
                    "minecraft:iron_bars_command_block"
                  ]
                }
                """);
        entries.put("data/minecraft/tags/block/mineable/pickaxe.json", """
                {
                  "replace": false,
                  "values": [
                    "#minecraft:iron_barss"
                  ]
                }
                """);
        entries.put("data/echoashfallprotocol/tags/block/toxic_air_sources.json", """
                {
                  "replace": false,
                  "values": [
                    "echoashfallprotocol:toxic_puddle",
                    "echoashfallprotocol:toxic_moss"
                  ]
                }
                """);
        for (Map.Entry<String, String> entry : entries.entrySet()) {
            Path path = root.resolve(entry.getKey().replace('/', java.io.File.separatorChar));
            Files.createDirectories(path.getParent());
            Files.writeString(path, entry.getValue(), StandardCharsets.UTF_8);
        }
    }

    private static void writeUnsafeRegistryDatapack(Path zipPath) throws Exception {
        Files.createDirectories(zipPath.getParent());
        Map<String, String> entries = new LinkedHashMap<>();
        entries.put("pack.mcmeta", """
                {
                  "pack": {
                    "description": "Agent 4 unsafe Ashfall product worldgen",
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
        entries.put("data/echoashfallprotocol/worldgen/processor_list/unsafe_native_registry.json", """
                {
                  "processors": [
                    {
                      "processor_type": "minecraft:rule",
                      "rules": [
                        {
                          "location_predicate": {"predicate_type": "minecraft:always_true"},
                          "input_predicate": {
                            "predicate_type": "minecraft:block_match",
                            "block": "echoblockworks:ashstone_raw"
                          },
                          "output_state": {"Name": "echoblockworks:ashstone_debris"}
                        },
                        {
                          "location_predicate": {"predicate_type": "minecraft:always_true"},
                          "input_predicate": {
                            "predicate_type": "minecraft:block_match",
                            "block": "minecraft:chain"
                          },
                          "output_state": {"Name": "minecraft:iron_bars"}
                        }
                      ]
                    }
                  ]
                }
                """);
        try (ZipOutputStream output = new ZipOutputStream(Files.newOutputStream(zipPath))) {
            for (Map.Entry<String, String> entry : entries.entrySet()) {
                output.putNextEntry(new ZipEntry(entry.getKey()));
                output.write(entry.getValue().getBytes(StandardCharsets.UTF_8));
                output.closeEntry();
            }
        }
    }

    private static void configure(String folder, String name, String datapack, String gameMode) {
        System.setProperty("echo.native.productWorldFolder", folder);
        System.setProperty("echo.native.productWorldName", name);
        System.setProperty("echo.native.productWorldDatapack", datapack);
        System.setProperty("echo.native.productWorldGameMode", gameMode);
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
