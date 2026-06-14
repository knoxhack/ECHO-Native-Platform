package dev.echo.nativeplatform.loader;

import dev.echo.nativeplatform.contracts.EchoNativeBootstrapProductProfile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class NativeLoaderContentFallbackResolver {
    public static final String SERVICE_ID = "echo.native.content_fallback_resolver";

    private final EchoNativeBootstrapProductProfile profile;
    private final String moduleClasspathProperty;
    private final Function<String, String> runtimeClass;
    private final Function<String, String> actionKey;
    private final Function<String, String> blockFallback;
    private volatile List<String> discoveredItemIds;
    private volatile List<String> discoveredBlockIds;

    public NativeLoaderContentFallbackResolver(
            EchoNativeBootstrapProductProfile profile,
            String moduleClasspathProperty,
            Function<String, String> runtimeClass,
            Function<String, String> actionKey,
            Function<String, String> blockFallback
    ) {
        this.profile = profile;
        this.moduleClasspathProperty = moduleClasspathProperty == null ? "" : moduleClasspathProperty;
        this.runtimeClass = runtimeClass;
        this.actionKey = actionKey;
        this.blockFallback = blockFallback;
    }

    public String resolveItemId(String requestedId) {
        String id = lowerContentId(requestedId);
        if (id.isBlank()) {
            return "";
        }
        if (registryContains("ITEM", id)) {
            return id;
        }
        if (isActiveNamespace(namespaceOf(id))) {
            String resolved = discoverContentId(cachedItemIds(), namespaceOf(id), pathOf(id));
            if (!resolved.isBlank() && registryContains("ITEM", resolved)) {
                return resolved;
            }
            resolved = discoverContentId(cachedItemIds(), namespaceOf(id), actionKey.apply(pathOf(id)));
            if (!resolved.isBlank() && registryContains("ITEM", resolved)) {
                return resolved;
            }
            resolved = discoverContentId(cachedItemIds(), namespaceOf(id), "");
            if (!resolved.isBlank() && registryContains("ITEM", resolved)) {
                return resolved;
            }
            return "";
        }
        return registryContains("ITEM", id) ? id : "";
    }

    public String resolveBlockId(String requestedId) {
        String id = lowerContentId(requestedId);
        if (id.isBlank()) {
            return "";
        }
        if (registryContains("BLOCK", id)) {
            return id;
        }
        if (isActiveNamespace(namespaceOf(id))) {
            String resolved = discoverContentId(cachedBlockIds(), namespaceOf(id), pathOf(id));
            if (!resolved.isBlank() && registryContains("BLOCK", resolved)) {
                return resolved;
            }
            resolved = discoverContentId(cachedBlockIds(), namespaceOf(id), actionKey.apply(pathOf(id)));
            if (!resolved.isBlank() && registryContains("BLOCK", resolved)) {
                return resolved;
            }
            resolved = discoverContentId(cachedBlockIds(), namespaceOf(id), "");
            if (!resolved.isBlank() && registryContains("BLOCK", resolved)) {
                return resolved;
            }
            return "";
        }
        return registryContains("BLOCK", id) ? id : "";
    }

    public boolean registryContains(String registryField, String contentId) {
        try {
            Class<?> builtInRegistriesClass = Class.forName(runtimeClass.apply("core.registries.BuiltInRegistries"));
            Object registry = builtInRegistriesClass.getField(registryField).get(null);
            return registryContainsExact(registry, nativeIdentifier(contentId));
        } catch (Throwable ignored) {
            return false;
        }
    }

    public boolean isActiveNamespace(String namespace) {
        String value = lowerContentId(namespace);
        if (value.isBlank()) {
            return false;
        }
        for (String prefix : profile.nativeModuleNamespacePrefixes()) {
            String safePrefix = lowerContentId(prefix);
            if (!safePrefix.isBlank() && value.startsWith(safePrefix)) {
                return true;
            }
        }
        return false;
    }

    public boolean prefersRealBlock(String blockId) {
        String id = lowerContentId(blockId);
        return hasBlockConstructorBinding(id)
                || hasSourceBackedBlockMapping(id);
    }

    public boolean prefersRealBlock(String namespace, String blockPath) {
        if (namespace == null || blockPath == null) {
            return false;
        }
        return prefersRealBlock(lowerContentId(namespace) + ":" + lowerContentId(blockPath));
    }

    public boolean prefersRealItem(String itemId) {
        String id = lowerContentId(itemId);
        return hasItemConstructorBinding(id)
                || hasSourceBackedItemMapping(id);
    }

    public boolean requiresItemShim(String itemId) {
        String id = lowerContentId(itemId);
        String namespace = namespaceOf(id);
        String path = pathOf(id);
        if (("echoterminal".equals(namespace) && "echo_terminal_remote".equals(path))
                || ("echowiki".equals(namespace) && "guide_book".equals(path))
                || ("signalos".equals(namespace) && "data_drive".equals(path))
                || (isActiveNamespace(namespace)
                && hasAny(path, "terminal", "guide", "wiki", "manual", "index", "lens", "scanner", "holomap", "map", "route"))) {
            return true;
        }
        if (!lowerContentId(profile.namespace()).equals(namespace)) {
            return false;
        }
        return hasAny(path, profile.nativeItemShimPathHints());
    }

    public boolean requiresBlockShim(String blockId) {
        String id = lowerContentId(blockId);
        String namespace = namespaceOf(id);
        String path = pathOf(id);
        if (("echoterminal".equals(namespace) && "echo_terminal".equals(path))
                || ("signalos".equals(namespace)
                && Set.of("terminal", "workstation", "server_rack", "network_relay").contains(path))) {
            return true;
        }
        if (!lowerContentId(profile.namespace()).equals(namespace)) {
            return hasAny(path,
                    "terminal", "server", "rack", "machine", "controller", "power", "generator", "relay",
                    "workstation", "workbench", "station", "table", "crop", "tray", "forge", "bench", "dock");
        }
        return hasAny(path, profile.nativeBlockShimPathHints());
    }

    public String discoverContentId(List<String> ids, String namespace, String hint) {
        String safeNamespace = lowerContentId(namespace);
        String safeHint = lowerContentId(hint);
        if (ids == null || ids.isEmpty() || safeNamespace.isBlank()) {
            return "";
        }
        if (!safeHint.isBlank()) {
            for (String id : ids) {
                if (namespaceOf(id).equals(safeNamespace) && pathOf(id).equals(safeHint)) {
                    return id;
                }
            }
            for (String id : ids) {
                if (namespaceOf(id).equals(safeNamespace) && pathOf(id).contains(safeHint)) {
                    return id;
                }
            }
        }
        for (String id : ids) {
            if (namespaceOf(id).equals(safeNamespace)) {
                return id;
            }
        }
        return "";
    }

    public List<String> cachedItemIds() {
        List<String> ids = discoveredItemIds;
        if (ids != null) {
            return ids;
        }
        try {
            ids = discoverItemIds();
        } catch (Throwable ignored) {
            ids = List.of();
        }
        discoveredItemIds = ids;
        return ids;
    }

    public List<String> cachedBlockIds() {
        List<String> ids = discoveredBlockIds;
        if (ids != null) {
            return ids;
        }
        try {
            ids = discoverBlockIds();
        } catch (Throwable ignored) {
            ids = List.of();
        }
        discoveredBlockIds = ids;
        return ids;
    }

    public List<String> discoverItemIds() throws IOException {
        Set<String> itemIds = new TreeSet<>();
        for (String item : contentClasspaths()) {
            if (item.isBlank()) {
                continue;
            }
            Path path = Path.of(item);
            if (!isContentClasspathCandidate(path)
                    && !path.toString().toLowerCase(java.util.Locale.ROOT).contains("build")) {
                continue;
            }
            if (!Files.isRegularFile(path) && !Files.isDirectory(path)) {
                continue;
            }
            discoverItemIds(path, itemIds);
        }
        return List.copyOf(itemIds);
    }

    public List<String> discoverBlockIds() throws IOException {
        Set<String> blockIds = new TreeSet<>();
        for (String item : contentClasspaths()) {
            if (item.isBlank()) {
                continue;
            }
            Path path = Path.of(item);
            if (!isContentClasspathCandidate(path)
                    && !path.toString().toLowerCase(java.util.Locale.ROOT).contains("build")) {
                continue;
            }
            if (!Files.isRegularFile(path) && !Files.isDirectory(path)) {
                continue;
            }
            discoverBlockIds(path, blockIds);
        }
        return List.copyOf(blockIds);
    }

    public String blockFallback(String blockId) {
        return blockFallback.apply(blockId);
    }

    private boolean hasItemConstructorBinding(String itemId) {
        String id = lowerContentId(itemId);
        for (var binding : profile.nativeItemConstructorBindings()) {
            if (binding != null && id.equals(lowerContentId(binding.id()))) {
                return true;
            }
        }
        return false;
    }

    private boolean hasSourceBackedItemMapping(String itemId) {
        String id = lowerContentId(itemId);
        for (var mapping : profile.nativeSourceBackedItemMappings()) {
            if (mapping != null && id.equals(lowerContentId(mapping.id()))) {
                return true;
            }
        }
        return false;
    }

    private boolean hasBlockConstructorBinding(String blockId) {
        String id = lowerContentId(blockId);
        for (var binding : profile.nativeBlockConstructorBindings()) {
            if (binding != null && id.equals(lowerContentId(binding.id()))) {
                return true;
            }
        }
        return false;
    }

    private boolean hasSourceBackedBlockMapping(String blockId) {
        String id = lowerContentId(blockId);
        for (var mapping : profile.nativeSourceBackedBlockMappings()) {
            if (mapping != null && id.equals(lowerContentId(mapping.id()))) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasAny(String value, String... needles) {
        String safeValue = lowerContentId(value);
        if (safeValue.isBlank() || needles == null || needles.length == 0) {
            return false;
        }
        for (String needle : needles) {
            if (!lowerContentId(needle).isBlank() && safeValue.contains(lowerContentId(needle))) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasAny(String value, List<String> needles) {
        String safeValue = lowerContentId(value);
        if (safeValue.isBlank() || needles == null || needles.isEmpty()) {
            return false;
        }
        for (String needle : needles) {
            if (!lowerContentId(needle).isBlank() && safeValue.contains(lowerContentId(needle))) {
                return true;
            }
        }
        return false;
    }

    private boolean registryContainsExact(Object registry, Object identifier) {
        if (registry == null || identifier == null) {
            return false;
        }
        Class<?> identifierClass = identifier.getClass();
        try {
            Object contains = registry.getClass().getMethod("containsKey", identifierClass).invoke(registry, identifier);
            if (contains instanceof Boolean value) {
                return value;
            }
        } catch (Throwable ignored) {
            // Some registry implementations expose Optional/getKey instead of containsKey.
        }
        try {
            Object optional = registry.getClass().getMethod("getOptional", identifierClass).invoke(registry, identifier);
            if (optional instanceof java.util.Optional<?> value) {
                return value.isPresent();
            }
        } catch (Throwable ignored) {
            // Fall through to exact key comparison for defaulted registries.
        }
        try {
            Object value = registry.getClass().getMethod("getValue", identifierClass).invoke(registry, identifier);
            if (value == null) {
                return false;
            }
            Object actualIdentifier = registry.getClass().getMethod("getKey", Object.class).invoke(registry, value);
            return identifier.equals(actualIdentifier);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private Object nativeIdentifier(String contentId) throws ReflectiveOperationException {
        Class<?> identifierClass = Class.forName(runtimeClass.apply("resources.Identifier"));
        String[] parts = splitContentId(contentId);
        return identifierClass.getMethod("fromNamespaceAndPath", String.class, String.class)
                .invoke(null, parts[0], parts[1]);
    }

    private List<String> contentClasspaths() {
        List<String> entries = new java.util.ArrayList<>();
        String classpath = System.getProperty("java.class.path", "");
        for (String item : classpath.split(java.util.regex.Pattern.quote(File.pathSeparator))) {
            if (!item.isBlank()) {
                entries.add(item);
            }
        }
        entries.addAll(NativeLoaderClasspathSupport.nativeModuleClasspathEntries(moduleClasspathProperty));
        return entries.stream().distinct().toList();
    }

    private boolean isContentClasspathCandidate(Path path) {
        if (path == null || path.getFileName() == null) {
            return false;
        }
        String filename = path.getFileName().toString().toLowerCase(java.util.Locale.ROOT);
        for (String prefix : profile.nativeModuleNamespacePrefixes()) {
            String safePrefix = lowerContentId(prefix);
            if (!safePrefix.isBlank() && filename.startsWith(safePrefix)) {
                return true;
            }
        }
        return false;
    }

    private void discoverBlockIds(Path jar, Set<String> blockIds) throws IOException {
        if (Files.isDirectory(jar)) {
            try (java.util.stream.Stream<Path> stream = Files.walk(jar)) {
                stream.filter(Files::isRegularFile)
                        .map(jar::relativize)
                        .map(Path::toString)
                        .map(name -> name.replace('\\', '/'))
                        .filter(name -> name.endsWith(".json") && name.startsWith("assets/"))
                        .forEach(name -> discoverBlockIdFromResourceName(name, blockIds));
            }
            return;
        }
        try (ZipInputStream input = new ZipInputStream(Files.newInputStream(jar))) {
            ZipEntry entry;
            while ((entry = input.getNextEntry()) != null) {
                String name = entry.getName().replace('\\', '/');
                discoverBlockIdFromResourceName(name, blockIds);
            }
        }
    }

    private void discoverItemIds(Path jar, Set<String> itemIds) throws IOException {
        if (Files.isDirectory(jar)) {
            try (java.util.stream.Stream<Path> stream = Files.walk(jar)) {
                stream.filter(Files::isRegularFile)
                        .map(jar::relativize)
                        .map(Path::toString)
                        .map(name -> name.replace('\\', '/'))
                        .filter(name -> name.endsWith(".json") && name.startsWith("assets/"))
                        .forEach(name -> discoverItemIdFromResourceName(name, itemIds));
            }
            return;
        }
        try (ZipInputStream input = new ZipInputStream(Files.newInputStream(jar))) {
            ZipEntry entry;
            while ((entry = input.getNextEntry()) != null) {
                String name = entry.getName().replace('\\', '/');
                discoverItemIdFromResourceName(name, itemIds);
            }
        }
    }

    private void discoverBlockIdFromResourceName(String name, Set<String> blockIds) {
        if (name == null || !name.endsWith(".json") || !name.startsWith("assets/")) {
            return;
        }
        String[] parts = name.split("/");
        if (parts.length < 4 || !"blockstates".equals(parts[2])) {
            return;
        }
        String namespace = parts[1];
        String path = name.substring(("assets/" + namespace + "/blockstates/").length(), name.length() - ".json".length());
        if (!path.isBlank() && isValidContentIdPart(namespace) && isValidContentPath(path) && isActiveNamespace(namespace)) {
            blockIds.add(namespace + ":" + path);
        }
    }

    private void discoverItemIdFromResourceName(String name, Set<String> itemIds) {
        if (name == null || !name.endsWith(".json") || !name.startsWith("assets/")) {
            return;
        }
        String[] parts = name.split("/");
        if (parts.length < 4) {
            return;
        }
        String namespace = parts[1];
        String path = "";
        if ("items".equals(parts[2])) {
            path = name.substring(("assets/" + namespace + "/items/").length(), name.length() - ".json".length());
        } else if (parts.length >= 5 && "models".equals(parts[2]) && "item".equals(parts[3])) {
            path = name.substring(("assets/" + namespace + "/models/item/").length(), name.length() - ".json".length());
        }
        if (!path.isBlank() && isValidContentIdPart(namespace) && isValidContentPath(path) && isActiveNamespace(namespace)) {
            itemIds.add(namespace + ":" + path);
        }
    }

    private static boolean isValidContentIdPart(String value) {
        return value.matches("[a-z0-9_.-]+");
    }

    private static boolean isValidContentPath(String value) {
        return value.matches("[a-z0-9_./-]+") && !value.contains("//") && !value.startsWith("/") && !value.endsWith("/");
    }

    private static String namespaceOf(String contentId) {
        String[] parts = splitContentId(contentId);
        return lowerContentId(parts[0]);
    }

    private static String pathOf(String contentId) {
        String[] parts = splitContentId(contentId);
        return lowerContentId(parts[1]);
    }

    private static String[] splitContentId(String contentId) {
        String value = contentId == null ? "" : contentId.trim();
        int colon = value.indexOf(':');
        if (colon <= 0 || colon >= value.length() - 1) {
            return new String[]{"minecraft", value.isBlank() ? "air" : value};
        }
        return new String[]{value.substring(0, colon), value.substring(colon + 1)};
    }

    private static String lowerContentId(String contentId) {
        return contentId == null ? "" : contentId.trim().toLowerCase(java.util.Locale.ROOT);
    }
}
