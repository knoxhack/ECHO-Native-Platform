package dev.echo.nativeplatform.loader;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public final class NativeLoaderResourceAssets {
    public static final String SERVICE_ID = "echo.native.resource_assets";

    private NativeLoaderResourceAssets() {
    }

    public static String fallbackLanguageJson(
            String itemGroupTranslationKey,
            String itemGroupTranslationName,
            Map<String, String> moduleItemPaths,
            Map<String, byte[]> entries
    ) {
        Map<String, String> translations = new TreeMap<>();
        if (itemGroupTranslationKey != null && !itemGroupTranslationKey.isBlank()) {
            translations.put(itemGroupTranslationKey, itemGroupTranslationName == null ? "" : itemGroupTranslationName);
        }
        for (Map.Entry<String, String> entry : safeMap(moduleItemPaths).entrySet()) {
            translations.put("item.echo_native." + entry.getValue(), "ECHO Module: " + entry.getKey());
        }
        for (String name : safeMap(entries).keySet()) {
            if (!name.startsWith("assets/") || name.startsWith("assets/minecraft/")) {
                continue;
            }
            if (name.contains("/items/") && name.endsWith(".json")) {
                String id = resourceId(name, "/items/");
                if (!id.isBlank()) {
                    translations.putIfAbsent("item." + id.replace('/', '.'), titleFromId(id));
                }
            } else if (name.contains("/blockstates/") && name.endsWith(".json")) {
                String id = resourceId(name, "/blockstates/");
                if (!id.isBlank()) {
                    String title = titleFromId(id);
                    translations.putIfAbsent("block." + id.replace('/', '.'), title);
                    translations.putIfAbsent("item." + id.replace('/', '.'), title);
                }
            }
        }
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        int index = 0;
        for (Map.Entry<String, String> entry : translations.entrySet()) {
            if (index++ > 0) {
                json.append(",\n");
            }
            json.append("  \"")
                    .append(escape(entry.getKey()))
                    .append("\": \"")
                    .append(escape(entry.getValue()))
                    .append("\"");
        }
        json.append("\n}\n");
        return json.toString();
    }

    public static List<String> normalizeTechBlockstates(
            Map<String, byte[]> entries,
            Collection<String> nativeModuleNamespacePrefixes
    ) {
        List<String> normalized = new ArrayList<>();
        Map<String, byte[]> safeEntries = safeMap(entries);
        List<String> names = safeEntries.keySet().stream()
                .filter(name -> name.startsWith("assets/") && name.contains("/blockstates/") && name.endsWith(".json"))
                .sorted(String::compareTo)
                .toList();
        for (String name : names) {
            String[] parts = name.split("/");
            if (parts.length < 4 || !"blockstates".equals(parts[2])) {
                continue;
            }
            String namespace = parts[1];
            String blockPath = name.substring(("assets/" + namespace + "/blockstates/").length(), name.length() - ".json".length());
            if (!isNativeModuleNamespace(namespace, nativeModuleNamespacePrefixes)) {
                continue;
            }
            String modelPath = nativeBlockstateModelPath(safeEntries, namespace, blockPath);
            if (modelPath.isBlank()) {
                continue;
            }
            String current = new String(safeEntries.get(name), StandardCharsets.UTF_8);
            if (isNativePlaceholderSafeBlockstate(current)) {
                continue;
            }
            if (shouldPreserveNativeBlockstate(namespace, blockPath, current)) {
                continue;
            }
            String normalizedJson = """
                    {
                      "variants": {
                        "": {
                          "model": "%s:block/%s"
                        }
                      }
                    }
                    """.formatted(escape(namespace), escape(modelPath));
            safeEntries.put(name, normalizedJson.getBytes(StandardCharsets.UTF_8));
            normalized.add(namespace + ":" + blockPath);
        }
        return normalized;
    }

    private static <K, V> Map<K, V> safeMap(Map<K, V> map) {
        return map == null ? Map.of() : map;
    }

    private static String resourceId(String name, String folder) {
        int namespaceStart = "assets/".length();
        int namespaceEnd = name.indexOf('/', namespaceStart);
        int folderStart = name.indexOf(folder);
        if (namespaceEnd < namespaceStart || folderStart < 0 || folderStart + folder.length() >= name.length()) {
            return "";
        }
        String namespace = name.substring(namespaceStart, namespaceEnd);
        String path = name.substring(folderStart + folder.length(), name.length() - ".json".length());
        if (namespace.isBlank() || path.isBlank()) {
            return "";
        }
        return namespace + ":" + path;
    }

    private static String titleFromId(String id) {
        int separator = id.indexOf(':');
        String path = separator >= 0 ? id.substring(separator + 1) : id;
        String[] words = path.replace('/', '_').replace('-', '_').split("_+");
        StringBuilder title = new StringBuilder();
        for (String word : words) {
            if (word.isBlank()) {
                continue;
            }
            if (!title.isEmpty()) {
                title.append(' ');
            }
            title.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) {
                title.append(word.substring(1));
            }
        }
        return title.isEmpty() ? id : title.toString();
    }

    private static boolean isNativeModuleNamespace(
            String namespace,
            Collection<String> nativeModuleNamespacePrefixes
    ) {
        String value = lowerContentId(namespace);
        if (value.isBlank()) {
            return false;
        }
        for (String prefix : nativeModuleNamespacePrefixes == null ? List.<String>of() : nativeModuleNamespacePrefixes) {
            String safePrefix = lowerContentId(prefix);
            if (!safePrefix.isBlank() && value.startsWith(safePrefix)) {
                return true;
            }
        }
        return false;
    }

    private static String lowerContentId(String contentId) {
        return contentId == null ? "" : contentId.trim().toLowerCase(Locale.ROOT);
    }

    private static String nativeBlockstateModelPath(Map<String, byte[]> entries, String namespace, String blockPath) {
        List<String> candidates = new ArrayList<>();
        candidates.add(blockPath);
        candidates.add(blockPath + "_post");
        candidates.add(blockPath + "_side");
        candidates.add(blockPath + "_side_tall");
        candidates.add(blockPath + "_inventory");
        if (blockPath.endsWith("_wall")) {
            String base = blockPath.substring(0, blockPath.length() - "_wall".length());
            candidates.add(base);
            candidates.add(base + "_wall_post");
            candidates.add(base + "_wall_side");
            candidates.add(base + "_wall_side_tall");
            candidates.add(base + "_wall_inventory");
            candidates.add(base + "_post");
            candidates.add(base + "_side");
            candidates.add(base + "_inventory");
        }
        if (blockPath.endsWith("_stairs")) {
            String base = blockPath.substring(0, blockPath.length() - "_stairs".length());
            candidates.add(base);
            candidates.add(base + "_stairs_inner");
            candidates.add(base + "_stairs_outer");
        }
        if (blockPath.endsWith("_slab")) {
            String base = blockPath.substring(0, blockPath.length() - "_slab".length());
            candidates.add(base);
            candidates.add(base + "_slab_top");
        }
        for (String candidate : candidates.stream().distinct().toList()) {
            if (entries.containsKey("assets/" + namespace + "/models/block/" + candidate + ".json")) {
                return candidate;
            }
        }
        return "";
    }

    private static boolean shouldPreserveNativeBlockstate(String namespace, String blockPath, String json) {
        return false;
    }

    private static boolean isNativePlaceholderSafeBlockstate(String json) {
        String compact = json.replaceAll("\\s+", "");
        return compact.startsWith("{\"variants\":{\"\":{\"model\":")
                && compact.endsWith("}}}");
    }

    private static String escape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}
