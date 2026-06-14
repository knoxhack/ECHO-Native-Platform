package dev.echo.nativeplatform.loader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * Native Loader-owned Ashfall product world policy. Minecraft-facing callers
 * execute the returned plan, but folder choice, datapack staging, old-save
 * guardrails, and fail-closed behavior live here.
 */
public final class NativeLoaderAshfallWorldStartupService {
    public static final String PRODUCT_ID = "echoashfallprotocol";
    public static final String WORLD_PRESET_ID = "echoashfallprotocol:ashfall_wasteland";
    public static final String DEFAULT_WORLD_FOLDER = "echo_native_ashfall_wasteland";
    public static final String DEFAULT_WORLD_NAME = "ECHO Native Ashfall";
    public static final String DEFAULT_DATAPACK_FILE = "echo-native-ashfall-datapack.zip";
    public static final String PRODUCT_WORLD_MARKER = "echo-native-product-world.json";
    public static final String PRODUCT_WORLD_OPEN_MARKER = "echo-native-product-world-open.json";
    private static final Map<DatapackValidationKey, Boolean> DATAPACK_VALIDATION_CACHE = new ConcurrentHashMap<>();
    private static final List<String> NATIVE_REGISTRY_UNSAFE_DATAPACK_TOKENS = List.of(
            "echoblockworks:ashstone_raw",
            "echoblockworks:hanging_wire",
            "minecraft:chain"
    );
    private static final List<String> NATIVE_REGISTRY_SAFE_DATAPACK_STAND_INS = List.of(
            "minecraft:stone",
            "minecraft:iron_bars"
    );
    static final List<String> LEGACY_UNBOUND_MINECRAFT_WORLDGEN_FEATURE_IDS = List.of(
            "minecraft:amethyst_block_cluster",
            "minecraft:amethyst_block_patch",
            "minecraft:dead_bush_patch",
            "minecraft:dead_bush_patches",
            "minecraft:dirt_patch",
            "minecraft:gravel_dense_patch",
            "minecraft:gravel_patch",
            "minecraft:gravel_scatter",
            "minecraft:gravel_surface_patch",
            "minecraft:iron_bars_patch",
            "minecraft:iron_bars_scatter",
            "minecraft:iron_block_placement",
            "minecraft:iron_blocks",
            "minecraft:iron_ore_scatter",
            "minecraft:mud_patch",
            "minecraft:packed_ice_cluster",
            "minecraft:packed_ice_scatter",
            "minecraft:snow_patch",
            "minecraft:stone_patch",
            "minecraft:stone_pile",
            "minecraft:stone_scatter"
    );

    private NativeLoaderAshfallWorldStartupService() {
    }

    public static StartupPlan prepare(Path gameDir) {
        Path safeGameDir = gameDir == null ? Path.of(".").toAbsolutePath().normalize() : gameDir.toAbsolutePath().normalize();
        String folder = productWorldFolder();
        String datapackFile = productDatapackFile();
        if (!isSafeProductWorldFolder(folder)) {
            return StartupPlan.blocked(
                    folder,
                    productWorldName(),
                    datapackFile,
                    WORLD_PRESET_ID,
                    productGameMode(),
                    "invalid_product_world_folder",
                    List.of(
                            "The configured Ashfall product world folder is not a single safe save-folder name:",
                            folder,
                            "Refusing to resolve product world paths outside the Minecraft saves directory."
                    ));
        }
        if (!isSafeProductDatapackFile(datapackFile)) {
            return StartupPlan.blocked(
                    folder,
                    productWorldName(),
                    datapackFile,
                    WORLD_PRESET_ID,
                    productGameMode(),
                    "invalid_product_world_datapack_file",
                    List.of(
                            "The configured Ashfall product datapack file is not a single safe zip file name:",
                            datapackFile,
                            "Refusing to resolve product datapack paths outside Native Loader staging locations."
                    ));
        }
        Path saveDir = saveDir(safeGameDir, folder);
        Path levelDat = saveDir.resolve("level.dat");
        if (Files.isRegularFile(levelDat) && !isValidProductWorldMarker(saveDir.resolve(PRODUCT_WORLD_MARKER), folder)) {
            return StartupPlan.blocked(
                    folder,
                    productWorldName(),
                    productDatapackFile(),
                    WORLD_PRESET_ID,
                    productGameMode(),
                    "old_vanilla_save_guard",
                    List.of(
                            "The configured folder already contains a vanilla or unmarked save:",
                            folder,
                            "Native Loader will not mark or patch it as an Ashfall product world.",
                            "Set echo.native.productWorldFolder to a new folder or move the old save out of the product path."
                    ));
        }

        DatapackStageResult datapack = stageProductDatapack(safeGameDir, folder);
        if (!datapack.ready()) {
            return StartupPlan.blocked(
                    folder,
                    productWorldName(),
                    productDatapackFile(),
                    WORLD_PRESET_ID,
                    productGameMode(),
                    "missing_product_datapack",
                    List.of(
                            "Native Ashfall worldgen datapack is missing or incomplete.",
                            "Refusing to open vanilla world creation from the product path.",
                            "Expected preset: " + WORLD_PRESET_ID
                    ),
                    datapack.toReport());
        }

        if (!writeProductWorldMarker(safeGameDir, folder)) {
            return StartupPlan.blocked(
                    folder,
                    productWorldName(),
                    productDatapackFile(),
                    WORLD_PRESET_ID,
                    productGameMode(),
                    "product_world_marker_write_failed",
                    List.of(
                            "Native Loader could not write a valid Ashfall product world marker:",
                            saveDir.resolve(PRODUCT_WORLD_MARKER).toString(),
                            "Refusing to create or open an unmarked product world."
                    ),
                    datapack.toReport());
        }
        boolean existingMarkedWorld = Files.isRegularFile(levelDat) && isMarkedProductWorld(safeGameDir, folder);
        return new StartupPlan(
                existingMarkedWorld ? StartupAction.OPEN_EXISTING : StartupAction.CREATE_NEW,
                folder,
                productWorldName(),
                productDatapackFile(),
                WORLD_PRESET_ID,
                productGameMode(),
                "",
                List.of(),
                saveDir,
                saveDir.resolve("datapacks").resolve(productDatapackFile()),
                saveDir.resolve(PRODUCT_WORLD_MARKER),
                datapack.toReport());
    }

    public static boolean productWorldAutoOpen() {
        return Boolean.parseBoolean(cleanProperty("echo.native.productWorldAutoOpen", "false"));
    }

    public static String configuredProductWorldFolder() {
        return productWorldFolder();
    }

    public static String configuredProductWorldName() {
        return productWorldName();
    }

    public static String configuredProductDatapackFile() {
        return productDatapackFile();
    }

    public static String configuredProductGameMode() {
        return productGameMode();
    }

    public static List<String> requiredDatapackEntries() {
        return List.of(
                "pack.mcmeta",
                "data/echoashfallprotocol/dimension/wasteland_overworld.json",
                "data/echoashfallprotocol/dimension/prefall_archives.json",
                "data/echoashfallprotocol/dimension_type/prefall_archives.json",
                "data/echoashfallprotocol/worldgen/world_preset/ashfall_wasteland.json",
                "data/minecraft/worldgen/world_preset/normal.json",
                "data/echoashfallprotocol/worldgen/noise_settings/wasteland_overworld.json",
                "data/echoashfallprotocol/worldgen/biome/the_wasteland.json"
        );
    }

    public static boolean isValidProductDatapack(Path datapack) {
        DatapackValidationKey key = DatapackValidationKey.from(datapack);
        if (key != null) {
            Boolean cached = DATAPACK_VALIDATION_CACHE.get(key);
            if (cached != null) {
                return cached;
            }
        }
        Set<String> entries = new TreeSet<>();
        try (ZipFile zip = new ZipFile(datapack.toFile())) {
            Enumeration<? extends ZipEntry> zipEntries = zip.entries();
            while (zipEntries.hasMoreElements()) {
                ZipEntry entry = zipEntries.nextElement();
                if (!entry.isDirectory()) {
                    String name = entry.getName().replace('\\', '/');
                    entries.add(name);
                    if (isJsonProductWorldgenEntry(name)) {
                        try (InputStream input = zip.getInputStream(entry)) {
                            if (isNativeRegistryUnsafeDatapackEntry(name, input.readAllBytes())) {
                                return false;
                            }
                        }
                    }
                }
            }
        } catch (IOException exception) {
            return false;
        }
        boolean valid = entries.containsAll(requiredDatapackEntries());
        if (key != null) {
            DATAPACK_VALIDATION_CACHE.put(key, valid);
        }
        return valid;
    }

    public static boolean isMarkedProductWorld(Path gameDir, String folder) {
        Path saveDir = saveDir(gameDir, folder);
        return isValidProductWorldMarker(saveDir.resolve(PRODUCT_WORLD_MARKER), folder)
                && Files.isRegularFile(saveDir.resolve("datapacks").resolve(productDatapackFile()))
                && isValidProductDatapack(saveDir.resolve("datapacks").resolve(productDatapackFile()));
    }

    private static boolean isValidProductWorldMarker(Path marker, String folder) {
        if (!Files.isRegularFile(marker)) {
            return false;
        }
        if (!isSafeProductDatapackFile(productDatapackFile())) {
            return false;
        }
        String expectedFolder = folder == null ? "" : folder.trim();
        if (expectedFolder.isBlank()) {
            return false;
        }
        try {
            String text = Files.readString(marker, StandardCharsets.UTF_8);
            return text.contains("\"schema\": \"echo.native.product_world.v1\"")
                    && text.contains("\"product\": \"" + PRODUCT_ID + "\"")
                    && text.contains("\"worldPreset\": \"" + WORLD_PRESET_ID + "\"")
                    && text.contains("\"datapack\": \"" + productDatapackFile() + "\"")
                    && text.contains("\"folder\": \"" + expectedFolder + "\"")
                    && text.contains("\"ownedBy\": \"NativeLoaderAshfallWorldStartupService\"");
        } catch (IOException exception) {
            return false;
        }
    }

    public static Map<String, Object> liveProductWorldEvidence(
            Path gameDir,
            boolean minecraftLevelPresent,
            boolean playerPresent
    ) {
        Path safeGameDir = gameDir == null ? Path.of(".").toAbsolutePath().normalize() : gameDir.toAbsolutePath().normalize();
        String folder = productWorldFolder();
        String datapackFile = productDatapackFile();
        boolean folderValid = isSafeProductWorldFolder(folder);
        boolean datapackFileValid = isSafeProductDatapackFile(datapackFile);
        Path saveDir = folderValid
                ? saveDir(safeGameDir, folder)
                : safeGameDir.resolve("saves").resolve("__invalid_product_world_folder__").normalize();
        Path marker = saveDir.resolve(PRODUCT_WORLD_MARKER);
        Path datapack = saveDir.resolve("datapacks")
                .resolve(datapackFileValid ? datapackFile : "__invalid_product_datapack_file__.zip");
        Path levelDat = saveDir.resolve("level.dat");
        boolean markerExists = folderValid && Files.isRegularFile(marker);
        boolean markerFolderMatches = folderValid && markerFolderMatches(marker, folder);
        boolean markerValid = folderValid && datapackFileValid && isValidProductWorldMarker(marker, folder);
        boolean datapackReady = folderValid && datapackFileValid && Files.isRegularFile(datapack) && isValidProductDatapack(datapack);
        boolean levelDatPresent = folderValid && Files.isRegularFile(levelDat);
        boolean opened = minecraftLevelPresent && playerPresent && levelDatPresent && markerValid && datapackReady;
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("schema", "echo.native.ashfall_live_product_world_evidence.v1");
        evidence.put("owner", NativeLoaderAshfallWorldStartupService.class.getName());
        evidence.put("productId", PRODUCT_ID);
        evidence.put("worldFolder", folder);
        evidence.put("productWorldFolderValid", folderValid);
        evidence.put("worldName", productWorldName());
        evidence.put("worldPreset", WORLD_PRESET_ID);
        evidence.put("datapackFile", datapackFile);
        evidence.put("productWorldDatapackFileValid", datapackFileValid);
        evidence.put("gameDir", safeGameDir.toString());
        evidence.put("saveDir", saveDir.toString());
        evidence.put("productWorldMarker", marker.toString());
        evidence.put("productWorldMarkerWritten", markerExists);
        evidence.put("productWorldMarkerFolderMatches", markerFolderMatches);
        evidence.put("productWorldMarkerValid", markerValid);
        evidence.put("stagedDatapack", datapack.toString());
        evidence.put("stagedDatapackReady", datapackReady);
        evidence.put("levelDat", levelDat.toString());
        evidence.put("productWorldLevelDatPresent", levelDatPresent);
        evidence.put("minecraftLevelPresent", minecraftLevelPresent);
        evidence.put("playerPresent", playerPresent);
        evidence.put("nativeProductWorldOpened", opened);
        evidence.put("nativeLoaderOwnedWorldPolicy", true);
        evidence.put("forcedWorldPreset", WORLD_PRESET_ID);
        evidence.put("vanillaWorldCreationFallbackAllowed", false);
        return Map.copyOf(evidence);
    }

    private static boolean markerFolderMatches(Path marker, String folder) {
        if (!Files.isRegularFile(marker) || folder == null || folder.isBlank()) {
            return false;
        }
        try {
            return Files.readString(marker, StandardCharsets.UTF_8)
                    .contains("\"folder\": \"" + folder.trim() + "\"");
        } catch (IOException exception) {
            return false;
        }
    }

    public static boolean recordProductWorldOpenDispatch(StartupPlan plan, String dispatchKind) {
        if (!dispatchPlanReady(plan)) {
            return false;
        }
        Path marker = plan.saveDir().resolve(PRODUCT_WORLD_OPEN_MARKER);
        String safeDispatchKind = dispatchKind == null || dispatchKind.isBlank()
                ? plan.action().name()
                : dispatchKind.trim();
        String json = "{\n"
                + "  \"schema\": \"echo.native.product_world_open_dispatch.v1\",\n"
                + "  \"product\": \"" + PRODUCT_ID + "\",\n"
                + "  \"worldFolder\": \"" + escape(plan.folder()) + "\",\n"
                + "  \"worldName\": \"" + escape(plan.worldName()) + "\",\n"
                + "  \"worldPreset\": \"" + WORLD_PRESET_ID + "\",\n"
                + "  \"datapack\": \"" + escape(plan.datapackFile()) + "\",\n"
                + "  \"startupAction\": \"" + plan.action().name() + "\",\n"
                + "  \"minecraftDispatch\": \"" + escape(safeDispatchKind) + "\",\n"
                + "  \"saveDir\": \"" + escape(plan.saveDir().toString()) + "\",\n"
                + "  \"productWorldMarker\": \"" + escape(plan.marker().toString()) + "\",\n"
                + "  \"stagedDatapack\": \"" + escape(plan.stagedDatapack().toString()) + "\",\n"
                + "  \"nativeLoaderOwnedWorldPolicy\": true,\n"
                + "  \"forcedWorldPreset\": \"" + WORLD_PRESET_ID + "\",\n"
                + "  \"vanillaWorldCreationFallbackAllowed\": false,\n"
                + "  \"ownedBy\": \"NativeLoaderAshfallWorldStartupService\"\n"
                + "}\n";
        try {
            Files.createDirectories(marker.getParent());
            Files.writeString(marker, json, StandardCharsets.UTF_8);
            return true;
        } catch (IOException ignored) {
            return false;
        }
    }

    private static boolean dispatchPlanReady(StartupPlan plan) {
        if (plan == null || plan.action() == StartupAction.BLOCKED) {
            return false;
        }
        try {
            if (!isSafeProductWorldFolder(plan.folder())
                    || !isSafeProductDatapackFile(plan.datapackFile())
                    || !WORLD_PRESET_ID.equals(plan.worldPreset())
                    || !productDatapackFile().equals(plan.datapackFile())) {
                return false;
            }
            Path saveDir = plan.saveDir().toAbsolutePath().normalize();
            Path marker = plan.marker().toAbsolutePath().normalize();
            Path stagedDatapack = plan.stagedDatapack().toAbsolutePath().normalize();
            Path expectedMarker = saveDir.resolve(PRODUCT_WORLD_MARKER).normalize();
            Path expectedDatapack = saveDir.resolve("datapacks").resolve(plan.datapackFile()).normalize();
            return marker.equals(expectedMarker)
                    && stagedDatapack.equals(expectedDatapack)
                    && isValidProductWorldMarker(marker, plan.folder())
                    && Files.isRegularFile(stagedDatapack)
                    && isValidProductDatapack(stagedDatapack);
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private static DatapackStageResult stageProductDatapack(Path gameDir, String folder) {
        Path target = saveDir(gameDir, folder).resolve("datapacks").resolve(productDatapackFile());
        if (Files.isRegularFile(target)) {
            if (isValidProductDatapack(target)) {
                return DatapackStageResult.ready(target, "existing_save_datapack", List.of(target), 0);
            }
            try {
                Files.deleteIfExists(target);
            } catch (IOException exception) {
                return DatapackStageResult.failed(target, "invalid_existing_datapack_delete_failed", List.of(target));
            }
        }
        List<Path> sources = productDatapackSources(gameDir, folder);
        for (Path source : sources) {
            if (!Files.isRegularFile(source) || !isValidProductDatapack(source)) {
                continue;
            }
            try {
                Files.createDirectories(target.getParent());
                Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                if (isValidProductDatapack(target)) {
                    return DatapackStageResult.ready(target, "native_loader_staged_source", sources, 1);
                }
                Files.deleteIfExists(target);
            } catch (IOException ignored) {
                // Keep searching; Native Loader must remain fail-closed.
            }
        }
        if (writeBundledProductDatapack(target)) {
            return DatapackStageResult.ready(target, "native_loader_bundled_product_worldgen", productDatapackResourceRoots(), 1);
        }
        return DatapackStageResult.failed(target, "missing_required_worldgen_entries", sources);
    }

    private static boolean writeBundledProductDatapack(Path target) {
        Map<String, byte[]> entries = new java.util.TreeMap<>();
        for (Path root : productDatapackResourceRoots()) {
            if (Files.isDirectory(root)) {
                copyBundledDirectoryEntries(root, entries);
            } else if (Files.isRegularFile(root)) {
                copyBundledZipEntries(root, entries);
            }
            if (entries.keySet().containsAll(requiredDatapackEntries())) {
                break;
            }
        }
        if (!entries.containsKey("pack.mcmeta")) {
            entries.put("pack.mcmeta", fallbackProductDatapackMcmeta());
        }
        if (!entries.keySet().containsAll(requiredDatapackEntries())) {
            return false;
        }
        try {
            Files.createDirectories(target.getParent());
            try (ZipOutputStream output = new ZipOutputStream(Files.newOutputStream(target))) {
                for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
                    output.putNextEntry(new ZipEntry(entry.getKey()));
                    output.write(entry.getValue());
                    output.closeEntry();
                }
            }
            if (isValidProductDatapack(target)) {
                return true;
            }
            Files.deleteIfExists(target);
        } catch (IOException ignored) {
            // Native product startup must not fall through to vanilla terrain.
        }
        return false;
    }

    private static void copyBundledDirectoryEntries(Path root, Map<String, byte[]> entries) {
        try (var stream = Files.walk(root)) {
            stream.filter(Files::isRegularFile).forEach(file -> {
                String name = root.relativize(file).toString().replace('\\', '/');
                if (!isBundledProductDatapackEntry(name) || entries.containsKey(name)) {
                    return;
                }
                try {
                    entries.put(name, sanitizeBundledProductDatapackEntry(name, Files.readAllBytes(file)));
                } catch (IOException ignored) {
                    // Keep collecting from other roots.
                }
            });
        } catch (IOException ignored) {
            // Keep collecting from other roots.
        }
    }

    private static void copyBundledZipEntries(Path root, Map<String, byte[]> entries) {
        try (ZipFile zip = new ZipFile(root.toFile())) {
            Enumeration<? extends ZipEntry> zipEntries = zip.entries();
            while (zipEntries.hasMoreElements()) {
                ZipEntry entry = zipEntries.nextElement();
                String name = entry.getName().replace('\\', '/');
                if (entry.isDirectory() || !isBundledProductDatapackEntry(name) || entries.containsKey(name)) {
                    continue;
                }
                try (InputStream input = zip.getInputStream(entry)) {
                    entries.put(name, sanitizeBundledProductDatapackEntry(name, input.readAllBytes()));
                }
            }
        } catch (IOException ignored) {
            // Keep collecting from other roots.
        }
    }

    private static byte[] sanitizeBundledProductDatapackEntry(String name, byte[] bytes) {
        return NativeLoaderResourceSanitizer.sanitizeProductDatapackEntry(
                name,
                bytes,
                PRODUCT_ID,
                "data/echoashfallprotocol/worldgen/structure/"
        );
    }

    private static boolean isNativeRegistryUnsafeDatapackEntry(String name, byte[] bytes) {
        if (bytes == null || !isJsonProductWorldgenEntry(name)) {
            return false;
        }
        String json = new String(bytes, StandardCharsets.UTF_8);
        if (NATIVE_REGISTRY_UNSAFE_DATAPACK_TOKENS.stream().anyMatch(json::contains)) {
            return true;
        }
        if (LEGACY_UNBOUND_MINECRAFT_WORLDGEN_FEATURE_IDS.stream()
                .anyMatch(id -> json.contains("\"" + id + "\""))) {
            return true;
        }
        byte[] sanitized = NativeLoaderResourceSanitizer.sanitizeProductDatapackEntry(
                name,
                bytes,
                PRODUCT_ID,
                "data/echoashfallprotocol/worldgen/structure/"
        );
        return !json.equals(new String(sanitized, StandardCharsets.UTF_8));
    }

    private static boolean isJsonProductWorldgenEntry(String name) {
        return name != null
                && (name.startsWith("data/echoashfallprotocol/worldgen/")
                || name.startsWith("data/echoashfallprotocol/tags/block/")
                || name.startsWith("data/minecraft/worldgen/")
                || name.startsWith("data/minecraft/tags/worldgen/")
                || name.startsWith("data/minecraft/tags/block/"))
                && name.endsWith(".json");
    }

    private static boolean isBundledProductDatapackEntry(String name) {
        if (name == null || name.isBlank()) {
            return false;
        }
        String normalized = name.replace('\\', '/');
        String lower = normalized.toLowerCase(java.util.Locale.ROOT);
        if ("pack.mcmeta".equals(normalized)) {
            return true;
        }
        if (lower.endsWith(".md") || lower.endsWith(".txt") || lower.contains("/readme")) {
            return false;
        }
        return productDatapackEntryPrefixes().stream().anyMatch(normalized::startsWith);
    }

    private static List<String> productDatapackEntryPrefixes() {
        return List.of(
                "data/echoashfallprotocol/worldgen/",
                "data/echoashfallprotocol/tags/worldgen/",
                "data/echoashfallprotocol/structure/",
                "data/echoashfallprotocol/structures/",
                "data/echoashfallprotocol/dimension/",
                "data/echoashfallprotocol/dimension_type/",
                "data/echoashfallprotocol/tags/block/",
                "data/minecraft/worldgen/",
                "data/minecraft/tags/worldgen/",
                "data/minecraft/tags/block/"
        );
    }

    private static List<Path> productDatapackResourceRoots() {
        Set<Path> roots = new LinkedHashSet<>();
        addClasspathRoots(roots, NativeLoaderClasspathSupport.nativeModuleClasspath("echo.native.moduleClasspath"));
        addClasspathRoots(roots, System.getProperty("java.class.path", ""));
        return List.copyOf(roots);
    }

    private static void addClasspathRoots(Set<Path> roots, String classpath) {
        if (classpath == null || classpath.isBlank()) {
            return;
        }
        for (String entry : classpath.split(java.util.regex.Pattern.quote(File.pathSeparator))) {
            if (entry == null || entry.isBlank()) {
                continue;
            }
            Path root = Path.of(entry).toAbsolutePath().normalize();
            String name = root.getFileName() == null ? "" : root.getFileName().toString().toLowerCase(java.util.Locale.ROOT);
            if (!name.contains("echoashfallprotocol")
                    && !Files.isDirectory(root.resolve("data").resolve("echoashfallprotocol"))) {
                continue;
            }
            addProductDatapackRoot(roots, root);
            addDerivedResourceRoot(roots, root);
        }
    }

    private static void addDerivedResourceRoot(Set<Path> roots, Path root) {
        String normalized = root.toString().replace('\\', '/');
        String classesPath = "/build/classes/java/main";
        if (normalized.endsWith(classesPath)) {
            addProductDatapackRoot(roots,
                    Path.of(normalized.substring(0, normalized.length() - classesPath.length())
                            + "/build/resources/main"));
        }
    }

    private static void addProductDatapackRoot(Set<Path> roots, Path root) {
        if (root == null) {
            return;
        }
        Path normalized = root.toAbsolutePath().normalize();
        if (Files.isDirectory(normalized) || Files.isRegularFile(normalized)) {
            roots.add(normalized);
        }
    }

    private static byte[] fallbackProductDatapackMcmeta() {
        String json = "{\n"
                + "  \"pack\": {\n"
                + "    \"description\": \"ECHO Native Ashfall worldgen\",\n"
                + "    \"min_format\": [101, 1],\n"
                + "    \"max_format\": [101, 1]\n"
                + "  }\n"
                + "}\n";
        return json.getBytes(StandardCharsets.UTF_8);
    }

    private static List<Path> productDatapackSources(Path gameDir, String folder) {
        String datapack = productDatapackFile();
        return List.of(
                gameDir.resolve("echo-native").resolve("worldgen").resolve(datapack),
                gameDir.resolve("datapacks").resolve(datapack),
                saveDir(gameDir, folder).resolve("datapacks").resolve(datapack));
    }

    private static boolean writeProductWorldMarker(Path gameDir, String folder) {
        Path marker = saveDir(gameDir, folder).resolve(PRODUCT_WORLD_MARKER);
        String json = "{\n"
                + "  \"schema\": \"echo.native.product_world.v1\",\n"
                + "  \"product\": \"" + PRODUCT_ID + "\",\n"
                + "  \"worldPreset\": \"" + WORLD_PRESET_ID + "\",\n"
                + "  \"datapack\": \"" + productDatapackFile() + "\",\n"
                + "  \"folder\": \"" + escape(folder) + "\",\n"
                + "  \"ownedBy\": \"NativeLoaderAshfallWorldStartupService\"\n"
                + "}\n";
        try {
            Files.createDirectories(marker.getParent());
            Files.writeString(marker, json, StandardCharsets.UTF_8);
            return isValidProductWorldMarker(marker, folder);
        } catch (IOException ignored) {
            return false;
        }
    }

    private static Path saveDir(Path gameDir, String folder) {
        return gameDir.toAbsolutePath().normalize().resolve("saves").resolve(folder).normalize();
    }

    private static boolean isSafeProductWorldFolder(String folder) {
        if (folder == null) {
            return false;
        }
        String value = folder.trim();
        if (value.isBlank()
                || value.equals(".")
                || value.equals("..")
                || value.contains("/")
                || value.contains("\\")
                || value.contains(":")
                || value.contains("$")
                || value.contains("{")
                || value.contains("}")
                || value.contains("%")
                || value.indexOf('\0') >= 0) {
            return false;
        }
        Path path = Path.of(value);
        return !path.isAbsolute() && path.getNameCount() == 1 && value.equals(path.getFileName().toString());
    }

    private static boolean isSafeProductDatapackFile(String datapackFile) {
        if (datapackFile == null) {
            return false;
        }
        String value = datapackFile.trim();
        if (value.isBlank()
                || value.equals(".")
                || value.equals("..")
                || !value.toLowerCase(java.util.Locale.ROOT).endsWith(".zip")
                || value.contains("/")
                || value.contains("\\")
                || value.contains(":")
                || value.indexOf('\0') >= 0) {
            return false;
        }
        Path path = Path.of(value);
        return !path.isAbsolute() && path.getNameCount() == 1 && value.equals(path.getFileName().toString());
    }

    private static String productWorldFolder() {
        return cleanProperty("echo.native.productWorldFolder", DEFAULT_WORLD_FOLDER);
    }

    private static String productWorldName() {
        return cleanProperty("echo.native.productWorldName", DEFAULT_WORLD_NAME);
    }

    private static String productDatapackFile() {
        return cleanProperty("echo.native.productWorldDatapack", DEFAULT_DATAPACK_FILE);
    }

    private static String productGameMode() {
        return cleanProperty("echo.native.productWorldGameMode", "survival").toLowerCase(java.util.Locale.ROOT);
    }

    private static String cleanProperty(String key, String fallback) {
        String value = System.getProperty(key, fallback);
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    public enum StartupAction {
        OPEN_EXISTING,
        CREATE_NEW,
        BLOCKED
    }

    public record StartupPlan(
            StartupAction action,
            String folder,
            String worldName,
            String datapackFile,
            String worldPreset,
            String gameMode,
            String failureKind,
            List<String> failureLines,
            Path saveDir,
            Path stagedDatapack,
            Path marker,
            Map<String, Object> datapackStage
    ) {
        static StartupPlan blocked(
                String folder,
                String worldName,
                String datapackFile,
                String worldPreset,
                String gameMode,
                String failureKind,
                List<String> failureLines
        ) {
            return blocked(folder, worldName, datapackFile, worldPreset, gameMode, failureKind, failureLines, Map.of());
        }

        static StartupPlan blocked(
                String folder,
                String worldName,
                String datapackFile,
                String worldPreset,
                String gameMode,
                String failureKind,
                List<String> failureLines,
                Map<String, Object> datapackStage
        ) {
            return new StartupPlan(
                    StartupAction.BLOCKED,
                    folder,
                    worldName,
                    datapackFile,
                    worldPreset,
                    gameMode,
                    failureKind,
                    failureLines == null ? List.of() : List.copyOf(failureLines),
                    Path.of(""),
                    Path.of(""),
                    Path.of(""),
                    datapackStage == null ? Map.of() : Map.copyOf(datapackStage));
        }

        public boolean createsFreshProductWorld() {
            return action == StartupAction.CREATE_NEW;
        }

        public boolean opensMarkedProductWorld() {
            return action == StartupAction.OPEN_EXISTING;
        }

        public boolean blocksVanillaFallback() {
            return action == StartupAction.BLOCKED;
        }

        public Map<String, Object> toReport() {
            Map<String, Object> report = new LinkedHashMap<>();
            report.put("schema", "echo.native.ashfall_world_startup_plan.v1");
            report.put("owner", NativeLoaderAshfallWorldStartupService.class.getName());
            report.put("action", action.name());
            report.put("folder", folder);
            report.put("worldName", worldName);
            report.put("datapackFile", datapackFile);
            report.put("worldPreset", worldPreset);
            report.put("gameMode", gameMode);
            report.put("failureKind", failureKind);
            report.put("failureLines", failureLines);
            report.put("saveDir", saveDir.toString());
            report.put("stagedDatapack", stagedDatapack.toString());
            report.put("marker", marker.toString());
            report.put("datapackStage", datapackStage);
            report.put("nativeLoaderOwnedWorldPolicy", true);
            report.put("forcedWorldPreset", WORLD_PRESET_ID);
            report.put("vanillaWorldCreationFallbackAllowed", false);
            return Map.copyOf(report);
        }
    }

    private record DatapackStageResult(
            boolean ready,
            Path target,
            String sourceKind,
            List<Path> searchedSources,
            int copiedCount
    ) {
        static DatapackStageResult ready(Path target, String sourceKind, List<Path> searchedSources, int copiedCount) {
            return new DatapackStageResult(true, target, sourceKind, List.copyOf(searchedSources), copiedCount);
        }

        static DatapackStageResult failed(Path target, String sourceKind, List<Path> searchedSources) {
            return new DatapackStageResult(false, target, sourceKind, List.copyOf(searchedSources), 0);
        }

        Map<String, Object> toReport() {
            Map<String, Object> report = new LinkedHashMap<>();
            report.put("ready", ready);
            report.put("target", target.toString());
            report.put("sourceKind", sourceKind);
            report.put("searchedSources", searchedSources.stream().map(Path::toString).toList());
            report.put("copiedCount", copiedCount);
            report.put("requiredEntries", requiredDatapackEntries());
            report.put("validProductDatapack", ready && isValidProductDatapack(target));
            return Map.copyOf(report);
        }
    }

    private record DatapackValidationKey(Path path, long size, long modifiedMillis) {
        private static DatapackValidationKey from(Path datapack) {
            if (datapack == null) {
                return null;
            }
            try {
                Path normalized = datapack.toAbsolutePath().normalize();
                if (!Files.isRegularFile(normalized)) {
                    return null;
                }
                return new DatapackValidationKey(
                        normalized,
                        Files.size(normalized),
                        Files.getLastModifiedTime(normalized).toMillis()
                );
            } catch (IOException exception) {
                return null;
            }
        }
    }
}
