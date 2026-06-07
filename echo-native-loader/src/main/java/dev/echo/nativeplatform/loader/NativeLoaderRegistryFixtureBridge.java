package dev.echo.nativeplatform.loader;

import dev.echo.nativeplatform.contracts.EchoNativeBootstrapProductProfile;
import dev.echo.nativeplatform.contracts.EchoNativeBootstrapProductProfile.NativeSourceBackedContentMapping;
import dev.echo.nativeplatform.contracts.EchoNativeBootstrapProductProfile.NativeSourceContractFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class NativeLoaderRegistryFixtureBridge {
    public static final String SERVICE_ID = "echo.native.registry_fixture_bridge";

    private NativeLoaderRegistryFixtureBridge() {
    }

    public static void apply(
            EchoNativeBootstrapProductProfile profile,
            String nativeGameDirProperty,
            String packId,
            List<String> modules,
            List<String> discoveredItemIds,
            List<String> discoveredBlockIds,
            Throwable nativeRegistryException,
            Map<String, Object> data
    ) throws IOException {
        List<Map<String, Object>> registeredBlocks = discoveredBlockIds.stream()
                .sorted(String::compareTo)
                .map(blockId -> {
                    Map<String, Object> block = new LinkedHashMap<>();
                    block.put("blockId", blockId);
                    block.put("itemId", blockId);
                    block.put("bridgeKind", "fixture_native_block_registry");
                    return block;
                })
                .toList();
        List<String> registeredBlockItems = registeredBlocks.stream()
                .map(block -> String.valueOf(block.get("itemId")))
                .toList();
        List<String> registeredContentItems = discoveredItemIds.stream()
                .sorted(String::compareTo)
                .toList();
        List<Map<String, Object>> registeredModuleItems =
                realModuleRepresentativeItems(modules, registeredContentItems, registeredBlockItems);
        List<String> registeredItemIds = new ArrayList<>();
        registeredItemIds.addAll(registeredBlockItems);
        registeredItemIds.addAll(registeredContentItems);
        registeredItemIds = registeredItemIds.stream().distinct().sorted(String::compareTo).toList();

        List<Map<String, Object>> augmentedTabs = List.of(
                fixtureCreativeVisibility("minecraft:search", "search_backed_creative_visibility", registeredItemIds),
                fixtureCreativeVisibility("minecraft:functional_blocks", "safe_vanilla_fallback", registeredBlockItems)
        );
        int visibleItemCount = augmentedTabs.stream()
                .mapToInt(tab -> integer(tab.get("itemCount")))
                .sum();
        Path registryPath = writeFixtureRegistry(
                nativeGameDirProperty,
                packId,
                registeredItemIds,
                registeredBlocks,
                registeredModuleItems,
                augmentedTabs
        );

        data.put("applied", true);
        data.put("registryMutated", false);
        data.put("nativeRegistryMutated", false);
        data.put("fixtureRegistryMutated", registryPath != null);
        data.put("fixtureRegistryPath", registryPath == null ? "" : registryPath.toString());
        data.put("fixtureOnlyRegistryEvidence", true);
        data.put("fixtureRegistryDoesNotSatisfyNativeParity", true);
        data.put("runtimeRegistryUnavailable", true);
        data.put("runtimeRegistryFailureKind", nativeRegistryException.getClass().getSimpleName());
        data.put("runtimeRegistryFailureMessage", failureMessage(nativeRegistryException));
        data.put("registeredItemCount", registeredItemIds.size());
        data.put("registeredModuleItemCount", registeredModuleItems.size());
        data.put("registeredContentItemCount", registeredContentItems.size());
        data.put("registeredBlockCount", registeredBlocks.size());
        data.put("registeredBlockItemCount", registeredBlockItems.size());
        data.put("attemptedCreativeTabCount", 0);
        data.put("registeredCreativeTabCount", 0);
        data.put("augmentedCreativeTabCount", 0);
        data.put("fixtureAugmentedCreativeTabCount", augmentedTabs.size());
        data.put("nativeCreativeTabBridgeApplied", false);
        data.put("fixtureCreativeTabBridgeApplied", true);
        data.put("nativeBetaItemWrapperAvailable", false);
        data.put("nativeBetaBlockWrapperAvailable", false);
        data.put("nativeBetaFunctionalItemCount", 0);
        data.put("nativeBetaFunctionalBlockCount", 0);
        data.put("creativeContentVisible", false);
        data.put("fixtureCreativeContentVisible", visibleItemCount > 0);
        data.put("registeredItems", registeredItemIds);
        data.put("registeredContentItems", registeredContentItems);
        data.put("registeredBlocks", registeredBlocks);
        data.put("registeredBlockItems", registeredBlockItems);
        data.put("registeredModuleItems", registeredModuleItems);
        data.put("registeredCreativeTabs", List.of());
        data.put("augmentedCreativeTabs", augmentedTabs);
        data.put("visibleItemCount", 0);
        data.put("visibleModuleItemCount", 0);
        data.put("fixtureVisibleItemCount", visibleItemCount);
        data.put("fixtureVisibleModuleItemCount", registeredModuleItems.size());
        data.put("visibleItems", List.of());
        data.put("visibleModuleItems", List.of());
        data.put("fixtureVisibleItems", registeredItemIds);
        data.put("fixtureVisibleModuleItems", registeredModuleItems.stream()
                .map(item -> String.valueOf(item.getOrDefault("itemId", "")))
                .filter(itemId -> !itemId.isBlank())
                .toList());
        data.put("sourceBackedProductItemMappingCount", sourceBackedItemMappings(profile).size());
        data.put("sourceBackedProductBlockMappingCount", sourceBackedBlockMappings(profile).size());
        data.put("sourceBackedProductItemMappings", sourceBackedItemMappings(profile));
        data.put("sourceBackedProductBlockMappings", sourceBackedBlockMappings(profile));
        data.put("nativeRegistrySourceContractFiles", sourceContractFiles(profile));
        data.put("nativeRegistryRuntimeGapCount", runtimeGaps(profile, nativeRegistryException).size());
        data.put("nativeRegistryRuntimeGaps", runtimeGaps(profile, nativeRegistryException));
        data.put("summary", "AdapterCore native registry bridge wrote fixture-local registry evidence only. Minecraft registry classes were not present in the authorized CLI activation process, so fixture items are not counted as native registry mutation, creative visibility, or functional item/block parity.");
    }

    public static List<Map<String, Object>> sourceBackedItemMappings(EchoNativeBootstrapProductProfile profile) {
        return sourceBackedMappings(profile.nativeSourceBackedItemMappings());
    }

    public static List<Map<String, Object>> sourceBackedBlockMappings(EchoNativeBootstrapProductProfile profile) {
        return sourceBackedMappings(profile.nativeSourceBackedBlockMappings());
    }

    public static List<Map<String, Object>> sourceContractFiles(EchoNativeBootstrapProductProfile profile) {
        List<Map<String, Object>> files = new ArrayList<>();
        for (NativeSourceContractFile file : profile.nativeRegistrySourceContractFiles()) {
            if (file == null || file.kind() == null || file.kind().isBlank()
                    || file.path() == null || file.path().isBlank()) {
                continue;
            }
            Map<String, Object> source = new LinkedHashMap<>();
            source.put("kind", file.kind());
            source.put("path", file.path());
            source.put("sourceBacked", true);
            files.add(Map.copyOf(source));
        }
        return List.copyOf(files);
    }

    public static List<Map<String, Object>> realModuleRepresentativeItems(
            List<String> modules,
            List<String> contentItems,
            List<String> blockItems
    ) {
        List<Map<String, Object>> representatives = new ArrayList<>();
        if (modules == null || modules.isEmpty()) {
            return representatives;
        }
        for (String module : modules.stream().map(NativeLoaderRegistryFixtureBridge::lowerContentId).distinct().sorted().toList()) {
            if (module.isBlank()) {
                continue;
            }
            String itemId = firstNamespaceContentId(module, contentItems);
            String source = "item";
            if (itemId.isBlank()) {
                itemId = firstNamespaceContentId(module, blockItems);
                source = "block_item";
            }
            if (itemId.isBlank()) {
                continue;
            }
            Map<String, Object> representative = new LinkedHashMap<>();
            representative.put("moduleId", module);
            representative.put("itemId", itemId);
            representative.put("path", pathOf(itemId));
            representative.put("namespace", namespaceOf(itemId));
            representative.put("source", source);
            representative.put("bridgeKind", "real_addon_content_representative");
            representatives.add(representative);
        }
        return representatives;
    }

    private static List<Map<String, Object>> sourceBackedMappings(
            List<NativeSourceBackedContentMapping> mappings
    ) {
        List<Map<String, Object>> data = new ArrayList<>();
        for (NativeSourceBackedContentMapping mapping : mappings) {
            if (mapping == null || mapping.id() == null || mapping.id().isBlank()) {
                continue;
            }
            data.add(sourceBackedMapping(mapping));
        }
        return List.copyOf(data);
    }

    private static Map<String, Object> sourceBackedMapping(NativeSourceBackedContentMapping sourceMapping) {
        Map<String, Object> mapping = new LinkedHashMap<>();
        mapping.put("id", sourceMapping.id());
        mapping.put("family", sourceMapping.family() == null ? "" : sourceMapping.family());
        mapping.put("sourcePath", sourceMapping.sourcePath() == null ? "" : sourceMapping.sourcePath());
        mapping.put("sourceClass", sourceMapping.sourceClass() == null ? "" : sourceMapping.sourceClass());
        mapping.put("nativeBridgeMethod", sourceMapping.nativeBridgeMethod() == null ? "" : sourceMapping.nativeBridgeMethod());
        mapping.put("sourceBacked", true);
        return Map.copyOf(mapping);
    }

    private static List<Map<String, Object>> runtimeGaps(
            EchoNativeBootstrapProductProfile profile,
            Throwable nativeRegistryException
    ) {
        String failureKind = nativeRegistryException == null ? "" : nativeRegistryException.getClass().getSimpleName();
        String failureMessage = nativeRegistryException == null ? "" : failureMessage(nativeRegistryException);
        return List.of(
                runtimeGap(
                        "minecraft_registry_classes_missing",
                        failureKind,
                        failureMessage,
                        "live_minecraft_registry_runtime",
                        "Launch through a product runtime handoff where net.minecraft registry classes are present and registry calls can mutate the trusted native or live registry bridge."
                ),
                runtimeGap(
                        "trusted_registry_bridge_not_attached",
                        failureKind,
                        failureMessage,
                        "trusted_native_registry_bridge",
                        "Attach a release-trusted NativeLoaderLiveRegistryBridge or first-class native registry bridge before claiming registry parity."
                )
        );
    }

    private static Map<String, Object> runtimeGap(
            String id,
            String failureKind,
            String failureMessage,
            String requiredCapability,
            String remediation
    ) {
        Map<String, Object> gap = new LinkedHashMap<>();
        gap.put("id", id);
        gap.put("missing", true);
        gap.put("requiredCapability", requiredCapability);
        gap.put("failureKind", failureKind);
        gap.put("failureMessage", failureMessage);
        gap.put("remediation", remediation);
        return gap;
    }

    private static Map<String, Object> fixtureCreativeVisibility(String tabId, String strategy, List<String> itemIds) {
        Map<String, Object> tab = new LinkedHashMap<>();
        List<String> items = itemIds.stream()
                .filter(itemId -> itemId != null && !itemId.isBlank())
                .distinct()
                .sorted(String::compareTo)
                .toList();
        tab.put("tabId", tabId);
        tab.put("strategy", strategy);
        tab.put("customTabCreated", false);
        tab.put("safeVanillaFallback", true);
        tab.put("searchBacked", true);
        tab.put("itemCount", items.size());
        tab.put("items", items.stream().limit(256).toList());
        return tab;
    }

    private static String firstNamespaceContentId(String namespace, List<String> ids) {
        String safeNamespace = lowerContentId(namespace);
        if (safeNamespace.isBlank() || ids == null || ids.isEmpty()) {
            return "";
        }
        return ids.stream()
                .map(NativeLoaderRegistryFixtureBridge::lowerContentId)
                .filter(id -> namespaceOf(id).equals(safeNamespace))
                .sorted(String::compareTo)
                .findFirst()
                .orElse("");
    }

    private static Path writeFixtureRegistry(
            String nativeGameDirProperty,
            String packId,
            List<String> registeredItemIds,
            List<Map<String, Object>> registeredBlocks,
            List<Map<String, Object>> registeredModuleItems,
            List<Map<String, Object>> augmentedTabs
    ) throws IOException {
        String gameDir = System.getProperty(nativeGameDirProperty, "");
        if (gameDir.isBlank()) {
            return null;
        }
        Path path = Path.of(gameDir).resolve("echo-native").resolve("adaptercore-fixture-registry.json")
                .toAbsolutePath().normalize();
        Files.createDirectories(path.getParent());
        Map<String, Object> registry = new LinkedHashMap<>();
        registry.put("schema", "echo.native.fixture_registry.v1");
        registry.put("packId", packId);
        registry.put("registeredItems", registeredItemIds);
        registry.put("registeredBlocks", registeredBlocks);
        registry.put("registeredModuleItems", registeredModuleItems);
        registry.put("augmentedCreativeTabs", augmentedTabs);
        registry.put("summary", "Fixture-local AdapterCore registry consumed by the native loader activation bridge when Minecraft registry classes are not available in the CLI process.");
        Files.writeString(path, writeJson(registry), StandardCharsets.UTF_8);
        return path;
    }

    private static String lowerContentId(String contentId) {
        return contentId == null ? "" : contentId.trim().toLowerCase(java.util.Locale.ROOT);
    }

    private static String namespaceOf(String contentId) {
        String value = lowerContentId(contentId);
        int separator = value.indexOf(':');
        return separator < 0 ? "" : value.substring(0, separator);
    }

    private static String pathOf(String contentId) {
        String value = lowerContentId(contentId);
        int separator = value.indexOf(':');
        return separator < 0 || separator + 1 >= value.length() ? "" : value.substring(separator + 1);
    }

    private static int integer(Object value) {
        return value instanceof Number number ? number.intValue() : 0;
    }

    private static String failureMessage(Throwable exception) {
        String message = exception.getMessage();
        Throwable cause = exception.getCause();
        if ((message == null || message.isBlank()) && cause != null) {
            message = cause.getClass().getSimpleName() + ": " + cause.getMessage();
        }
        return message == null || message.isBlank() ? exception.getClass().getSimpleName() : message;
    }

    private static String writeJson(Object value) {
        StringBuilder builder = new StringBuilder();
        writeValue(builder, value, 0);
        builder.append('\n');
        return builder.toString();
    }

    private static void writeValue(StringBuilder builder, Object value, int indent) {
        if (value == null) {
            builder.append("null");
        } else if (value instanceof String string) {
            builder.append('"').append(escape(string)).append('"');
        } else if (value instanceof Number || value instanceof Boolean) {
            builder.append(value);
        } else if (value instanceof Map<?, ?> map) {
            writeObject(builder, map, indent);
        } else if (value instanceof Iterable<?> iterable) {
            writeArray(builder, iterable, indent);
        } else {
            builder.append('"').append(escape(String.valueOf(value))).append('"');
        }
    }

    private static void writeObject(StringBuilder builder, Map<?, ?> map, int indent) {
        Map<String, Object> sorted = new TreeMap<>();
        map.forEach((key, value) -> sorted.put(String.valueOf(key), value));
        builder.append('{');
        if (!sorted.isEmpty()) {
            int index = 0;
            for (Map.Entry<String, Object> entry : sorted.entrySet()) {
                builder.append('\n').append("  ".repeat(indent + 1));
                builder.append('"').append(escape(entry.getKey())).append("\": ");
                writeValue(builder, entry.getValue(), indent + 1);
                if (++index < sorted.size()) {
                    builder.append(',');
                }
            }
            builder.append('\n').append("  ".repeat(indent));
        }
        builder.append('}');
    }

    private static void writeArray(StringBuilder builder, Iterable<?> iterable, int indent) {
        List<Object> items = new ArrayList<>();
        iterable.forEach(items::add);
        builder.append('[');
        if (!items.isEmpty()) {
            for (int index = 0; index < items.size(); index++) {
                builder.append('\n').append("  ".repeat(indent + 1));
                writeValue(builder, items.get(index), indent + 1);
                if (index + 1 < items.size()) {
                    builder.append(',');
                }
            }
            builder.append('\n').append("  ".repeat(indent));
        }
        builder.append(']');
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
