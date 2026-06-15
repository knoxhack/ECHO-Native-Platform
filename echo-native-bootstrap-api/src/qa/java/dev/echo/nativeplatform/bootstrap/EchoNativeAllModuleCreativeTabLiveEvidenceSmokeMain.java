package dev.echo.nativeplatform.bootstrap;

import dev.echo.nativeplatform.bootstrap.EchoNativeAgent3CreativeTabPlanGateMain.TestComponent;
import dev.echo.nativeplatform.bootstrap.EchoNativeAgent3CreativeTabPlanGateMain.TestCreativeModeTab;
import dev.echo.nativeplatform.bootstrap.EchoNativeAgent3CreativeTabPlanGateMain.TestCreativeModeTabs;
import dev.echo.nativeplatform.bootstrap.EchoNativeAgent3CreativeTabPlanGateMain.TestIdentifier;
import dev.echo.nativeplatform.bootstrap.EchoNativeAgent3CreativeTabPlanGateMain.TestItem;
import dev.echo.nativeplatform.bootstrap.EchoNativeAgent3CreativeTabPlanGateMain.TestItemLike;
import dev.echo.nativeplatform.bootstrap.EchoNativeAgent3CreativeTabPlanGateMain.TestItemStack;
import dev.echo.nativeplatform.bootstrap.EchoNativeAgent3CreativeTabPlanGateMain.TestOutput;
import dev.echo.nativeplatform.bootstrap.EchoNativeAgent3CreativeTabPlanGateMain.TestRegistry;
import dev.echo.nativeplatform.bootstrap.EchoNativeAgent3CreativeTabPlanGateMain.TestTabVisibility;
import dev.echo.nativeplatform.contracts.EchoNativeBootstrapProductProfile;
import dev.echo.nativeplatform.contracts.EchoNativeBootstrapProductProfile.NativeEntityDefinition;
import dev.echo.nativeplatform.loader.NativeLoaderRegistryCreativeBridge;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public final class EchoNativeAllModuleCreativeTabLiveEvidenceSmokeMain {
    private static final Pattern JSON_ID = Pattern.compile("\"id\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern ITEM_GROUP_KEY = Pattern.compile("\"(itemGroup\\.[^\"]+)\"\\s*:");
    private static final Pattern REGISTER_ID =
            Pattern.compile("(?:ITEMS|BLOCK_ITEMS|BLOCKS)\\.register\\(\\s*\"([a-z0-9_./-]+)\"");
    private static final Pattern REGISTER_ITEM_ID =
            Pattern.compile("registerItem\\([^;]*?\"([a-z0-9_./-]+)\"", Pattern.DOTALL);
    private static final Pattern REGISTER_BLOCK_ID =
            Pattern.compile("registerBlock\\([^;]*?\"([a-z0-9_./-]+)\"", Pattern.DOTALL);
    private static final Pattern SIMPLE_ITEM_ID =
            Pattern.compile("\\bsimple\\(\\s*\"([a-z0-9_./-]+)\"");
    private static final Pattern HELPER_BLOCK_ID =
            Pattern.compile("\\b(?:block|ore)\\(\\s*\"([a-z0-9_./-]+)\"");

    private EchoNativeAllModuleCreativeTabLiveEvidenceSmokeMain() {
    }

    public static void main(String[] args) throws Exception {
        Path addonsRoot = echoModulesAddonsRoot();
        List<ModuleContent> modules = discoverModules(addonsRoot);
        ArrayList<Map<String, Object>> registeredCreativeTabs = new ArrayList<>();
        ArrayList<Map<String, Object>> moduleRows = new ArrayList<>();
        ArrayList<String> catalogItemIds = new ArrayList<>();
        ArrayList<String> expectedPlacedBlockIds = new ArrayList<>();

        for (ModuleContent module : modules) {
            TestRegistry<TestCreativeModeTab> creativeTabRegistry = new TestRegistry<>();
            TestRegistry<TestItemLike> itemRegistry = new TestRegistry<>();
            for (CreativeEntry entry : module.entries()) {
                TestIdentifier id = TestIdentifier.fromNamespaceAndPath(namespace(entry.itemId()), path(entry.itemId()));
                itemRegistry.put(id, new TestItem(entry.itemId()));
            }

            String tabId = module.moduleId() + ":native_modules";
            List<String> allItems = module.entries().stream().map(CreativeEntry::itemId).toList();
            List<String> blockItems = module.entries().stream()
                    .filter(CreativeEntry::block)
                    .map(CreativeEntry::itemId)
                    .toList();
            List<String> contentItems = module.entries().stream()
                    .filter(entry -> !entry.block())
                    .map(CreativeEntry::itemId)
                    .toList();
            List<Map<String, Object>> bridges = NativeLoaderRegistryCreativeBridge.registerNativeCreativeTabs(
                    new CatalogProfile(),
                    blockItems,
                    contentItems,
                    List.of(tabId),
                    TestIdentifier.class,
                    TestRegistry.class,
                    TestCreativeModeTab.class,
                    TestCreativeModeTabs.class,
                    TestComponent.class,
                    TestItemStack.class,
                    TestItemLike.class,
                    TestTabVisibility.class,
                    TestOutput.class,
                    creativeTabRegistry,
                    itemRegistry,
                    List.of(declaration(module, tabId, allItems))
            );
            registeredCreativeTabs.addAll(bridges);
            catalogItemIds.addAll(allItems);
            if (module.representativeBlockId() != null && !module.representativeBlockId().isBlank()) {
                expectedPlacedBlockIds.add(module.representativeBlockId());
            }
            moduleRows.add(moduleRow(module, bridges));
        }

        Map<String, Object> registryBridge = new LinkedHashMap<>();
        registryBridge.put("registeredCreativeTabs", registeredCreativeTabs);
        registryBridge.put("registeredCreativeTabCount", registeredCreativeTabs.size());
        registryBridge.put("nativeCreativeTabBridgeApplied", !registeredCreativeTabs.isEmpty());
        registryBridge.put("testRegistryBridgeApplied", true);
        registryBridge.put("liveCreativeInventoryOutput", false);
        registryBridge.put("creativeVisibilityBridgeApplied", false);
        registryBridge.put("nativeCreativeModuleTabRegistryBacked", false);
        registryBridge.put("nativeCreativeModuleTabContentVisible", false);
        registryBridge.put("visibleModuleItems", List.of());
        registryBridge.put("creativeTabSelectableItemIds", List.of());
        registryBridge.put("creativeTabPlayableItemIds", List.of());
        registryBridge.put("catalogItemIds", catalogItemIds);

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("schema", "echo.native.all_module_creative_tab_live_evidence.v1");
        report.put("generatedAt", "1970-01-01T00:00:00Z");
        report.put("status", "FAIL");
        report.put("runtime", "echo_native");
        report.put("evidenceKind", "catalog_test_registry_bridge");
        report.put("liveGameEvidence", false);
        report.put("blocker", "Catalog TestRegistry bridge output is not a real Minecraft creative inventory, hotbar selection, or gameplay-use proof.");
        report.put("moduleIds", modules.stream().map(ModuleContent::moduleId).toList());
        report.put("registryBackedModuleIds", List.of());
        report.put("visibleParentModuleIds", List.of());
        report.put("visibleSearchModuleIds", List.of());
        report.put("selectableModuleIds", List.of());
        report.put("playableModuleIds", List.of());
        report.put("selectableItemIds", List.of());
        report.put("playableItemIds", List.of());
        report.put("placedBlockIds", List.of());
        report.put("catalogItemIds", catalogItemIds);
        report.put("expectedPlacedBlockIds", expectedPlacedBlockIds);
        report.put("modules", moduleRows);
        report.put("runtimeBridge", Map.of("registryBridge", registryBridge));

        Path reportPath = Path.of("reports", "echo-native", "all-module-creative-tab-live-evidence.json")
                .toAbsolutePath()
                .normalize();
        Files.createDirectories(reportPath.getParent());
        Files.writeString(reportPath, toJson(report) + "\n", StandardCharsets.UTF_8);
        System.out.println("native all-module creative tab live evidence requires live client proof modules="
                + modules.size() + " tabs=" + registeredCreativeTabs.size());
    }

    private static Map<String, Object> declaration(ModuleContent module, String tabId, List<String> allItems) {
        Map<String, Object> declaration = new LinkedHashMap<>();
        declaration.put("registry", "creative_tab");
        declaration.put("id", tabId);
        declaration.put("titleKey", module.titleKey());
        declaration.put("iconItem", allItems.isEmpty() ? "" : allItems.get(0));
        declaration.put("itemIds", allItems);
        declaration.put("surfaceIds", List.of("creative_inventory"));
        declaration.put("orderAnchor", "minecraft:building_blocks");
        declaration.put("orderStrategy", "with_tabs_before_anchor");
        declaration.put("searchVisibility", "parent_and_search_tabs");
        declaration.put("searchVisible", true);
        return declaration;
    }

    private static Map<String, Object> moduleRow(ModuleContent module, List<Map<String, Object>> bridges) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("moduleId", module.moduleId());
        row.put("registryBacked", false);
        row.put("visibleParent", false);
        row.put("visibleSearch", false);
        row.put("selectable", false);
        row.put("playable", false);
        row.put("testRegistryBacked", true);
        row.put("selectedItemId", module.representativeItemId());
        row.put("expectedPlayMutation", module.representativeBlockId().isBlank() ? "creative_item_activate" : "creative_block_place");
        row.put("expectedEntries", module.entries().stream().map(CreativeEntry::itemId).toList());
        row.put("missingCreativeTabEntries", module.entries().stream().map(CreativeEntry::itemId).toList());
        row.put("missingCreativeSearchEntries", module.entries().stream().map(CreativeEntry::itemId).toList());
        row.put("blockers", List.of(
                "catalog TestRegistry bridge is not live Minecraft creative inventory output",
                "no hotbar or inventory selection proof",
                "no gameplay use or block-place proof"
        ));
        row.put("registeredCreativeTabs", bridges);
        return row;
    }

    private static List<ModuleContent> discoverModules(Path addonsRoot) throws IOException {
        ArrayList<ModuleContent> modules = new ArrayList<>();
        try (Stream<Path> stream = Files.list(addonsRoot)) {
            for (Path moduleRoot : stream.filter(Files::isDirectory).sorted().toList()) {
                Path descriptor = moduleRoot.resolve("src/main/resources/META-INF/echo.mod.json");
                if (!Files.isRegularFile(descriptor)) {
                    continue;
                }
                String descriptorText = Files.readString(descriptor, StandardCharsets.UTF_8);
                String moduleId = match(JSON_ID, descriptorText, moduleRoot.getFileName().toString())
                        .toLowerCase(Locale.ROOT);
                Path javaRoot = moduleRoot.resolve("src/main/java");
                Path resourceRoot = moduleRoot.resolve("src/main/resources");
                List<Path> javaFiles = files(javaRoot, ".java");
                List<Path> resourceFiles = files(resourceRoot, "");
                String sourceText = joinedSource(javaFiles);
                List<String> resourcePaths = resourceFiles.stream()
                        .map(resourceRoot::relativize)
                        .map(Path::toString)
                        .map(path -> path.replace('\\', '/').toLowerCase(Locale.ROOT))
                        .toList();
                EntryCatalog entries = expectedEntries(moduleId, resourcePaths, sourceText);
                if (entries.entries().isEmpty()) {
                    continue;
                }
                List<String> itemGroupKeys = itemGroupKeys(resourceFiles);
                String titleKey = itemGroupKeys.isEmpty() ? "itemGroup." + moduleId : itemGroupKeys.get(0);
                modules.add(new ModuleContent(moduleId, titleKey, entries.entries()));
            }
        }
        modules.sort(Comparator.comparing(ModuleContent::moduleId));
        return List.copyOf(modules);
    }

    private static EntryCatalog expectedEntries(
            String moduleId,
            List<String> resourcePaths,
            String sourceText
    ) {
        LinkedHashMap<String, Boolean> entries = new LinkedHashMap<>();
        for (String resourcePath : resourcePaths) {
            String[] parts = resourcePath.split("/");
            if (parts.length >= 5
                    && parts[0].equals("assets")
                    && parts[2].equals("models")
                    && parts[3].equals("item")
                    && resourcePath.endsWith(".json")) {
                String name = resourcePath.substring(
                        ("assets/" + parts[1] + "/models/item/").length(),
                        resourcePath.length() - ".json".length()
                );
                if (!name.contains("/")) {
                    entries.putIfAbsent(parts[1] + ":" + name, false);
                }
            }
            if (parts.length >= 4
                    && parts[0].equals("assets")
                    && parts[2].equals("blockstates")
                    && resourcePath.endsWith(".json")) {
                String name = resourcePath.substring(
                        ("assets/" + parts[1] + "/blockstates/").length(),
                        resourcePath.length() - ".json".length()
                );
                entries.put(parts[1] + ":" + name, true);
            }
        }
        addSourceEntries(entries, moduleId, false, REGISTER_ID, sourceText);
        addSourceEntries(entries, moduleId, false, REGISTER_ITEM_ID, sourceText);
        addSourceEntries(entries, moduleId, true, REGISTER_BLOCK_ID, sourceText);
        addSourceEntries(entries, moduleId, false, SIMPLE_ITEM_ID, sourceText);
        addSourceEntries(entries, moduleId, true, HELPER_BLOCK_ID, sourceText);

        ArrayList<CreativeEntry> result = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (Map.Entry<String, Boolean> entry : entries.entrySet()) {
            String itemId = entry.getKey().replace('\\', '/').toLowerCase(Locale.ROOT);
            if (itemId.matches("[a-z0-9_.-]+:[a-z0-9_./-]+") && seen.add(itemId)) {
                result.add(new CreativeEntry(itemId, entry.getValue()));
            }
        }
        return new EntryCatalog(List.copyOf(result));
    }

    private static void addSourceEntries(
            Map<String, Boolean> entries,
            String moduleId,
            boolean block,
            Pattern pattern,
            String sourceText
    ) {
        for (String id : matches(pattern, sourceText)) {
            String itemId = moduleId + ":" + id.toLowerCase(Locale.ROOT);
            if (block) {
                entries.put(itemId, true);
            } else {
                entries.putIfAbsent(itemId, false);
            }
        }
    }

    private static List<Path> files(Path root, String suffix) throws IOException {
        if (!Files.isDirectory(root)) {
            return List.of();
        }
        try (Stream<Path> stream = Files.walk(root)) {
            return stream.filter(Files::isRegularFile)
                    .filter(path -> suffix.isBlank() || path.toString().endsWith(suffix))
                    .sorted()
                    .toList();
        }
    }

    private static String joinedSource(List<Path> files) throws IOException {
        StringBuilder builder = new StringBuilder();
        for (Path file : files) {
            String relative = file.toString().replace('\\', '/');
            if (!(relative.contains("CreativeTab")
                    || relative.contains("Items")
                    || relative.contains("Blocks")
                    || relative.contains("ContentDefinitions")
                    || relative.contains("Machines")
                    || relative.contains("NativeModule")
                    || relative.contains("ProductBridgeProvider")
                    || relative.contains("/registry/"))) {
                continue;
            }
            builder.append(Files.readString(file, StandardCharsets.UTF_8)).append('\n');
        }
        return builder.toString();
    }

    private static List<String> itemGroupKeys(List<Path> resourceFiles) throws IOException {
        LinkedHashSet<String> keys = new LinkedHashSet<>();
        for (Path file : resourceFiles) {
            if (!file.toString().endsWith(".json")) {
                continue;
            }
            Matcher matcher = ITEM_GROUP_KEY.matcher(Files.readString(file, StandardCharsets.UTF_8));
            while (matcher.find()) {
                keys.add(matcher.group(1));
            }
        }
        return List.copyOf(keys);
    }

    private static Path echoModulesAddonsRoot() {
        String configured = System.getProperty("echo.modules.root");
        if (configured == null || configured.isBlank()) {
            configured = System.getenv("ECHO_MODULES_ROOT");
        }
        Path root = configured == null || configured.isBlank()
                ? Path.of("..", "ECHO-Modules", "addons")
                : Path.of(configured);
        Path normalized = root.toAbsolutePath().normalize();
        if (Files.isDirectory(normalized.resolve("addons"))) {
            return normalized.resolve("addons");
        }
        return normalized;
    }

    private static List<String> matches(Pattern pattern, String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        LinkedHashSet<String> values = new LinkedHashSet<>();
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            values.add(matcher.group(1));
        }
        return List.copyOf(values);
    }

    private static String match(Pattern pattern, String text, String fallback) {
        Matcher matcher = pattern.matcher(text == null ? "" : text);
        return matcher.find() ? matcher.group(1) : fallback;
    }

    private static String namespace(String itemId) {
        int index = itemId.indexOf(':');
        return index < 0 ? "" : itemId.substring(0, index);
    }

    private static String path(String itemId) {
        int index = itemId.indexOf(':');
        return index < 0 ? itemId : itemId.substring(index + 1);
    }

    private static String toJson(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof String string) {
            return "\"" + escape(string) + "\"";
        }
        if (value instanceof Number || value instanceof Boolean) {
            return String.valueOf(value);
        }
        if (value instanceof Map<?, ?> map) {
            StringBuilder builder = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!first) {
                    builder.append(",");
                }
                first = false;
                builder.append(toJson(String.valueOf(entry.getKey()))).append(":").append(toJson(entry.getValue()));
            }
            return builder.append("}").toString();
        }
        if (value instanceof Iterable<?> iterable) {
            StringBuilder builder = new StringBuilder("[");
            boolean first = true;
            for (Object item : iterable) {
                if (!first) {
                    builder.append(",");
                }
                first = false;
                builder.append(toJson(item));
            }
            return builder.append("]").toString();
        }
        return toJson(String.valueOf(value));
    }

    private static String escape(String value) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            switch (ch) {
                case '\\' -> builder.append("\\\\");
                case '"' -> builder.append("\\\"");
                case '\n' -> builder.append("\\n");
                case '\r' -> builder.append("\\r");
                case '\t' -> builder.append("\\t");
                default -> builder.append(ch);
            }
        }
        return builder.toString();
    }

    private record EntryCatalog(List<CreativeEntry> entries) {}

    private record CreativeEntry(String itemId, boolean block) {}

    private record ModuleContent(String moduleId, String titleKey, List<CreativeEntry> entries) {
        String representativeBlockId() {
            return entries.stream().filter(CreativeEntry::block).map(CreativeEntry::itemId).findFirst().orElse("");
        }

        String representativeItemId() {
            String block = representativeBlockId();
            if (!block.isBlank()) {
                return block;
            }
            return entries.isEmpty() ? "" : entries.get(0).itemId();
        }
    }

    private static final class CatalogProfile implements EchoNativeBootstrapProductProfile {
        @Override
        public String namespace() {
            return "echoashfallprotocol";
        }

        @Override
        public String nativeLoaderMainLabel() {
            return "ECHO";
        }

        @Override
        public String nativeLoaderClientLabel() {
            return "ECHO Native Client";
        }

        @Override
        public String nativeLoaderSessionMessage() {
            return "ECHO Native session";
        }

        @Override
        public String nativeLoaderWindowTitle() {
            return "ECHO Native";
        }

        @Override
        public String nativeLoaderAdapterCoreServiceId() {
            return "adaptercore";
        }

        @Override
        public String nativeLoaderRuntimeHostClass() {
            return "NativeRuntimeHost";
        }

        @Override
        public String nativeMinecraftRuntimeHostClass() {
            return "NativeMinecraftHost";
        }

        @Override
        public String nativeMinecraftRuntimeHostId() {
            return "minecraft";
        }

        @Override
        public String nativeLoaderBackendClass() {
            return "NativeBackend";
        }

        @Override
        public String nativeLoaderRuntimeLane() {
            return "echo.native";
        }

        @Override
        public String nativeUiActionCommand() {
            return "native.ui";
        }

        @Override
        public String nativeGameplayDisplayName() {
            return "ECHO";
        }

        @Override
        public Map<String, List<String>> nativeCreativeTabPreferredIcons() {
            return Map.of();
        }

        @Override
        public List<String> requiredGameplayHandlerEvents() {
            return List.of();
        }

        @Override
        public List<String> requiredAgent7WorldLiveHooks() {
            return List.of();
        }

        @Override
        public List<String> requiredLiveMutationSurfaces() {
            return List.of();
        }

        @Override
        public List<NativeEntityDefinition> nativeEntities() {
            return List.of();
        }
    }
}
