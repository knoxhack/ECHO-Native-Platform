package dev.echo.nativeplatform.loader;

import dev.echo.nativeplatform.diagnostics.EchoNativeJson;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Native Loader client resource mount. Minecraft internally models mod jars as
 * packs, so ECHO exposes module classpath resources through the same required,
 * hidden PackRepository path instead of a user-selected resource pack.
 */
public final class NativeLoaderModuleResourcePack {
    public static final String PACK_ID = "echo_native_modules";
    private static final String PACK_TITLE = "ECHO Native Module Resources";
    private static final int MAX_ATTEMPTS = 15000;
    private static final long POLL_MILLIS = 2L;

    private NativeLoaderModuleResourcePack() {
    }

    public static void startClientRepositoryMountThread(
            String packId,
            Path resourcePackCache,
            List<String> modules,
            List<String> productModuleSourcePathMarkers,
            Path evidencePath
    ) {
        Thread thread = new Thread(
                () -> mountWhenClientRepositoryExists(
                        packId,
                        resourcePackCache,
                        modules,
                        productModuleSourcePathMarkers,
                        evidencePath
                ),
                "echo-native-module-resource-pack"
        );
        thread.setDaemon(true);
        thread.start();
    }

    private static void mountWhenClientRepositoryExists(
            String packId,
            Path resourcePackCache,
            List<String> modules,
            List<String> productModuleSourcePathMarkers,
            Path evidencePath
    ) {
        Map<String, Object> evidence = baseEvidence(packId, resourcePackCache, modules);
        try {
            ResourceIndex resourceIndex = ResourceIndex.load(resourcePackCache, productModuleSourcePathMarkers);
            evidence.put("primaryResourceSource", "module_classpath");
            evidence.put("moduleClasspathResourceEntryCount", resourceIndex.moduleEntryCount());
            evidence.put("moduleClasspathResourceSourceCount", resourceIndex.moduleSources().size());
            evidence.put("moduleClasspathResourceSources", resourceIndex.moduleSources().stream().map(Path::toString).sorted().toList());
            evidence.put("invalidMinecraftResourceIdentifierEntryCount", resourceIndex.invalidIdentifierEntryCount());
            evidence.put("cacheFallbackResourceEntryCount", resourceIndex.cacheFallbackEntryCount());
            evidence.put("cacheCompatibilityRepairEntryCount", resourceIndex.cacheCompatibilityRepairEntryCount());
            evidence.put("resourcePackCacheOnly", false);
            evidence.put("materializedCacheFallback", resourceIndex.cacheFallbackEntryCount() > 0);
            evidence.put("materializedCacheCompatibilityRepair", resourceIndex.cacheCompatibilityRepairEntryCount() > 0);
            evidence.put("packResourcesImplementation", NativeLoaderModuleResourcePack.class.getName());
            if (resourceIndex.entries().isEmpty()) {
                evidence.put("mounted", false);
                evidence.put("failureKind", "missing_module_resources");
                evidence.put("summary", "Native module resources were not mounted because no module classpath assets or fallback assets were available.");
                writeEvidence(evidencePath, evidence);
                return;
            }
            for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
                Object minecraft = minecraftInstance();
                Object repository = minecraft == null ? null : fieldValue(minecraft, "resourcePackRepository");
                Object resourceManager = minecraft == null ? null : fieldValue(minecraft, "resourceManager");
                boolean clientReady = clientReadyForLatePackMount(minecraft);
                if (repository != null && resourceManager != null && clientReady) {
                    boolean beforeResourceManager = false;
                    Object source = repositorySource(resourceIndex);
                    installRepositorySource(repository, source);
                    repository.getClass().getMethod("reload").invoke(repository);
                    boolean managedReloadRequested = managedReloadRequested();
                    if (managedReloadRequested) {
                        minecraft.getClass().getMethod("reloadResourcePacks").invoke(minecraft);
                    }
                    evidence.put("attempts", attempt);
                    evidence.put("mounted", true);
                    evidence.put("mountedBeforeResourceManager", beforeResourceManager);
                    evidence.put("mountedBeforeInitialResourceReload", false);
                    evidence.put("waitedForPostInitialClientState", true);
                    evidence.put("clientScreenClass", className(fieldValue(minecraft, "screen")));
                    evidence.put("clientLevelPresent", fieldValue(minecraft, "level") != null);
                    evidence.put("mountPoint", "PackRepository.sources");
                    evidence.put("requiredPack", true);
                    evidence.put("hiddenPack", true);
                    evidence.put("hiddenPackReason", "native_loader_required_pack_repository_source");
                    evidence.put("nativeManagedPack", true);
                    evidence.put("managedReloadRequested", managedReloadRequested);
                    evidence.put("managedReloadOptInProperty", "echo.native.moduleResourcePackReload");
                    evidence.put("optionalToggleExposed", false);
                    evidence.put("userFacingOptionalPack", false);
                    evidence.put("summary", managedReloadRequested
                            ? "Native module classpath resources were mounted through the ECHO PackResources facade after the first client screen or world existed, then an opt-in managed reload was requested."
                            : "Native module classpath resources were mounted through the ECHO PackResources facade after the first client screen or world existed; startup managed reload was skipped to avoid Minecraft constructor registry-freeze crashes.");
                    writeEvidence(evidencePath, evidence);
                    return;
                }
                Thread.sleep(POLL_MILLIS);
            }
            evidence.put("mounted", false);
            evidence.put("failureKind", "client_repository_timeout");
            evidence.put("summary", "Native module resource mount timed out before Minecraft exposed the client PackRepository.");
            writeEvidence(evidencePath, evidence);
        } catch (Throwable exception) {
            evidence.put("mounted", false);
            evidence.put("failureKind", exception.getClass().getSimpleName());
            evidence.put("failureMessage", String.valueOf(exception.getMessage()));
            evidence.put("summary", "Native module resource mount failed inside the client runtime.");
            writeEvidence(evidencePath, evidence);
        }
    }

    private static Object repositorySource(ResourceIndex resourceIndex) throws ReflectiveOperationException {
        Class<?> sourceClass = Class.forName("net.minecraft.server.packs.repository.RepositorySource");
        return Proxy.newProxyInstance(
                sourceClass.getClassLoader(),
                new Class<?>[]{sourceClass},
                (proxy, method, args) -> {
                    if ("hashCode".equals(method.getName()) && method.getParameterCount() == 0) {
                        return PACK_ID.hashCode();
                    }
                    if ("equals".equals(method.getName()) && method.getParameterCount() == 1) {
                        return proxy == args[0];
                    }
                    if ("loadPacks".equals(method.getName()) && args != null && args.length == 1) {
                        @SuppressWarnings("unchecked")
                        Consumer<Object> consumer = (Consumer<Object>) args[0];
                        Object pack = createPack(resourceIndex);
                        if (pack != null) {
                            consumer.accept(pack);
                        }
                        return null;
                    }
                    if ("toString".equals(method.getName())) {
                            return "NativeLoaderModuleResourcePack[" + resourceIndex.entries().size() + " entries]";
                    }
                    return null;
                }
        );
    }

    private static boolean managedReloadRequested() {
        return Boolean.getBoolean("echo.native.moduleResourcePackReload");
    }

    private static boolean clientReadyForLatePackMount(Object minecraft) {
        if (minecraft == null) {
            return false;
        }
        if (fieldValue(minecraft, "level") != null) {
            return true;
        }
        Object screen = fieldValue(minecraft, "screen");
        String screenClass = className(screen);
        return !screenClass.isBlank()
                && !screenClass.endsWith(".GenericMessageScreen")
                && !screenClass.equals("net.minecraft.client.gui.screens.GenericMessageScreen");
    }

    private static String className(Object value) {
        return value == null ? "" : value.getClass().getName();
    }

    private static void installRepositorySource(Object repository, Object source) throws ReflectiveOperationException {
        Field sourcesField = findField(repository.getClass(), "sources");
        sourcesField.setAccessible(true);
        Object rawSources = sourcesField.get(repository);
        Set<Object> sources = new LinkedHashSet<>();
        if (rawSources instanceof Iterable<?> iterable) {
            for (Object item : iterable) {
                sources.add(item);
            }
        }
        sources.add(source);
        sourcesField.set(repository, sources);
    }

    private static Object createPack(ResourceIndex resourceIndex) throws ReflectiveOperationException {
        Class<?> componentClass = Class.forName("net.minecraft.network.chat.Component");
        Class<?> packSourceClass = Class.forName("net.minecraft.server.packs.repository.PackSource");
        Class<?> locationInfoClass = Class.forName("net.minecraft.server.packs.PackLocationInfo");
        Class<?> packClass = Class.forName("net.minecraft.server.packs.repository.Pack");
        Class<?> resourcesSupplierClass = Class.forName("net.minecraft.server.packs.repository.Pack$ResourcesSupplier");
        Class<?> metadataClass = Class.forName("net.minecraft.server.packs.repository.Pack$Metadata");
        Class<?> compatibilityClass = Class.forName("net.minecraft.server.packs.repository.PackCompatibility");
        Class<?> featureFlagSetClass = Class.forName("net.minecraft.world.flag.FeatureFlagSet");
        Class<?> positionClass = Class.forName("net.minecraft.server.packs.repository.Pack$Position");
        Class<?> selectionConfigClass = Class.forName("net.minecraft.server.packs.PackSelectionConfig");

        Object title = componentClass.getMethod("literal", String.class).invoke(null, PACK_TITLE);
        Object source = enumOrField(packSourceClass, "BUILT_IN", "DEFAULT");
        Object locationInfo = locationInfoClass
                .getConstructor(String.class, componentClass, packSourceClass, Optional.class)
                .newInstance(PACK_ID, title, source, Optional.empty());
        Object supplier = resourcesSupplier(resourceIndex);
        Object compatibility = Enum.valueOf(compatibilityClass.asSubclass(Enum.class), "COMPATIBLE");
        Object requestedFeatures = featureFlagSetClass.getMethod("of").invoke(null);
        Object metadata = metadataClass
                .getConstructor(componentClass, compatibilityClass, featureFlagSetClass, List.class)
                .newInstance(title, compatibility, requestedFeatures, List.of());
        Object position = Enum.valueOf(positionClass.asSubclass(Enum.class), "TOP");
        Object selectionConfig = selectionConfigClass
                .getConstructor(boolean.class, positionClass, boolean.class)
                .newInstance(true, position, true);
        return packClass
                .getConstructor(locationInfoClass, resourcesSupplierClass, metadataClass, selectionConfigClass)
                .newInstance(locationInfo, supplier, metadata, selectionConfig);
    }

    private static Object resourcesSupplier(ResourceIndex resourceIndex) throws ReflectiveOperationException {
        Class<?> supplierClass = Class.forName("net.minecraft.server.packs.repository.Pack$ResourcesSupplier");
        return Proxy.newProxyInstance(
                supplierClass.getClassLoader(),
                new Class<?>[]{supplierClass},
                (proxy, method, args) -> {
                    if ("openPrimary".equals(method.getName()) || "openFull".equals(method.getName())) {
                        return packResources(args[0], resourceIndex);
                    }
                    if ("toString".equals(method.getName())) {
                        return "EchoNativeModuleResourceSupplier[" + resourceIndex.entries().size() + " entries]";
                    }
                    return null;
                }
        );
    }

    private static Object packResources(Object locationInfo, ResourceIndex resourceIndex) throws ReflectiveOperationException {
        Class<?> packResourcesClass = Class.forName("net.minecraft.server.packs.PackResources");
        return Proxy.newProxyInstance(
                packResourcesClass.getClassLoader(),
                new Class<?>[]{packResourcesClass},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getRootResource" -> rootResource(args);
                    case "getResource" -> resource(resourceIndex, args);
                    case "listResources" -> {
                        listResources(resourceIndex, args);
                        yield null;
                    }
                    case "getNamespaces" -> namespaces(resourceIndex, args[0]);
                    case "getMetadataSection" -> metadataSection(args[0]);
                    case "location" -> locationInfo;
                    case "packId" -> PACK_ID;
                    case "knownPackInfo" -> Optional.empty();
                    case "close" -> null;
                    case "toString" -> "EchoNativeModulePackResources[" + resourceIndex.entries().size() + " entries]";
                    default -> null;
                }
        );
    }

    private static Object rootResource(Object[] args) throws ReflectiveOperationException {
        String[] segments = args != null && args.length == 1 && args[0] instanceof String[] array ? array : new String[0];
        if ("pack.mcmeta".equals(String.join("/", segments))) {
            return ioSupplier(packMcmetaBytes());
        }
        return null;
    }

    private static Object resource(ResourceIndex resourceIndex, Object[] args) throws ReflectiveOperationException {
        String path = packPath(args[0], args[1]);
        byte[] bytes = resourceIndex.entries().get(path);
        return bytes == null ? null : ioSupplier(bytes);
    }

    @SuppressWarnings("unchecked")
    private static void listResources(ResourceIndex resourceIndex, Object[] args) throws ReflectiveOperationException {
        String directory = packDirectory(args[0]);
        String namespace = String.valueOf(args[1]);
        if (!isValidIdentifierNamespace(namespace)) {
            return;
        }
        String path = trimSlashes(String.valueOf(args[2]));
        String prefix = directory + "/" + namespace + "/" + (path.isBlank() ? "" : path + "/");
        BiConsumer<Object, Object> output = (BiConsumer<Object, Object>) args[3];
        Class<?> identifierClass = Class.forName("net.minecraft.resources.Identifier");
        Method identifierFactory = identifierClass.getMethod("fromNamespaceAndPath", String.class, String.class);
        for (Map.Entry<String, byte[]> entry : resourceIndex.entries().entrySet()) {
            if (!entry.getKey().startsWith(prefix)) {
                continue;
            }
            String idPath = entry.getKey().substring((directory + "/" + namespace + "/").length());
            if (idPath.isBlank() || idPath.endsWith("/")) {
                continue;
            }
            if (!isValidIdentifierPath(idPath)) {
                continue;
            }
            Object id = identifierFactory.invoke(null, namespace, idPath);
            output.accept(id, ioSupplier(entry.getValue()));
        }
    }

    private static Set<String> namespaces(ResourceIndex resourceIndex, Object packType) throws ReflectiveOperationException {
        String directory = packDirectory(packType);
        Set<String> namespaces = new LinkedHashSet<>();
        String prefix = directory + "/";
        for (String entry : resourceIndex.entries().keySet()) {
            if (!entry.startsWith(prefix)) {
                continue;
            }
            int start = prefix.length();
            int end = entry.indexOf('/', start);
            if (end > start) {
                String namespace = entry.substring(start, end);
                if (isValidIdentifierNamespace(namespace)) {
                    namespaces.add(namespace);
                }
            }
        }
        return namespaces;
    }

    private static Object metadataSection(Object metadataSectionType) throws ReflectiveOperationException {
        Object name = metadataSectionType.getClass().getMethod("name").invoke(metadataSectionType);
        if (!"pack".equals(String.valueOf(name))) {
            return null;
        }
        Class<?> componentClass = Class.forName("net.minecraft.network.chat.Component");
        Class<?> packFormatClass = Class.forName("net.minecraft.server.packs.metadata.pack.PackFormat");
        Class<?> inclusiveRangeClass = Class.forName("net.minecraft.util.InclusiveRange");
        Class<?> packMetadataClass = Class.forName("net.minecraft.server.packs.metadata.pack.PackMetadataSection");
        Object title = componentClass.getMethod("literal", String.class).invoke(null, PACK_TITLE);
        Object format = packFormatClass.getMethod("of", int.class, int.class).invoke(null, 84, 0);
        Object range = inclusiveRangeClass.getConstructor(Comparable.class).newInstance(format);
        return packMetadataClass.getConstructor(componentClass, inclusiveRangeClass).newInstance(title, range);
    }

    private static String packPath(Object packType, Object identifier) throws ReflectiveOperationException {
        String directory = packDirectory(packType);
        Class<?> identifierClass = Class.forName("net.minecraft.resources.Identifier");
        String namespace = String.valueOf(identifierClass.getMethod("getNamespace").invoke(identifier));
        String path = String.valueOf(identifierClass.getMethod("getPath").invoke(identifier));
        return directory + "/" + namespace + "/" + path;
    }

    private static String packDirectory(Object packType) throws ReflectiveOperationException {
        return String.valueOf(packType.getClass().getMethod("getDirectory").invoke(packType));
    }

    private static Object ioSupplier(byte[] bytes) throws ReflectiveOperationException {
        Class<?> ioSupplierClass = Class.forName("net.minecraft.server.packs.resources.IoSupplier");
        byte[] copy = bytes.clone();
        return Proxy.newProxyInstance(
                ioSupplierClass.getClassLoader(),
                new Class<?>[]{ioSupplierClass},
                (proxy, method, args) -> {
                    if ("get".equals(method.getName())) {
                        return new ByteArrayInputStream(copy);
                    }
                    if ("toString".equals(method.getName())) {
                        return "EchoNativeIoSupplier[" + copy.length + " bytes]";
                    }
                    return null;
                }
        );
    }

    private static byte[] packMcmetaBytes() {
        return ("{\"pack\":{\"description\":\"" + PACK_TITLE + "\",\"pack_format\":{\"major\":84,\"minor\":0},"
                + "\"supported_formats\":{\"min_inclusive\":{\"major\":84,\"minor\":0},"
                + "\"max_inclusive\":{\"major\":84,\"minor\":0}}}}\n").getBytes(StandardCharsets.UTF_8);
    }

    private static Object enumOrField(Class<?> type, String first, String fallback) throws ReflectiveOperationException {
        if (type.isEnum()) {
            try {
                return Enum.valueOf(type.asSubclass(Enum.class), first);
            } catch (IllegalArgumentException ignored) {
                return Enum.valueOf(type.asSubclass(Enum.class), fallback);
            }
        }
        try {
            return type.getField(first).get(null);
        } catch (ReflectiveOperationException ignored) {
            return type.getField(fallback).get(null);
        }
    }

    private static Object minecraftInstance() {
        try {
            Class<?> minecraftClass = Class.forName("net.minecraft.client.Minecraft");
            try {
                return minecraftClass.getMethod("getInstance").invoke(null);
            } catch (ReflectiveOperationException ignored) {
                return staticFieldValue(minecraftClass, "instance");
            }
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static Object staticFieldValue(Class<?> type, String name) {
        try {
            Field field = type.getDeclaredField(name);
            field.setAccessible(true);
            return field.get(null);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static Object fieldValue(Object target, String name) {
        try {
            Field field = findField(target.getClass(), name);
            field.setAccessible(true);
            return field.get(target);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static Field findField(Class<?> startType, String name) throws NoSuchFieldException {
        Class<?> type = startType;
        while (type != null) {
            try {
                return type.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                type = type.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name);
    }

    private static boolean isValidIdentifierNamespace(String namespace) {
        if (namespace == null || namespace.isBlank()) {
            return false;
        }
        for (int index = 0; index < namespace.length(); index++) {
            char value = namespace.charAt(index);
            if ((value >= 'a' && value <= 'z')
                    || (value >= '0' && value <= '9')
                    || value == '_'
                    || value == '-'
                    || value == '.') {
                continue;
            }
            return false;
        }
        return true;
    }

    private static boolean isValidIdentifierPath(String path) {
        if (path == null || path.isBlank()) {
            return false;
        }
        for (int index = 0; index < path.length(); index++) {
            char value = path.charAt(index);
            if ((value >= 'a' && value <= 'z')
                    || (value >= '0' && value <= '9')
                    || value == '_'
                    || value == '-'
                    || value == '.'
                    || value == '/') {
                continue;
            }
            return false;
        }
        return true;
    }

    private record ResourceIndex(
            Map<String, byte[]> entries,
            int moduleEntryCount,
            int cacheFallbackEntryCount,
            int cacheCompatibilityRepairEntryCount,
            List<Path> moduleSources
    ) {
        private static ResourceIndex load(
                Path resourcePackCache,
                List<String> productModuleSourcePathMarkers
        ) throws IOException {
            Map<String, byte[]> entries = new LinkedHashMap<>();
            List<Path> moduleSources = new ArrayList<>();
            int moduleEntries = 0;
            for (String raw : System.getProperty("java.class.path", "").split(java.io.File.pathSeparator)) {
                if (raw.isBlank()) {
                    continue;
                }
                Path path = Path.of(raw).toAbsolutePath().normalize();
                if (!isEchoModuleSource(path, productModuleSourcePathMarkers)) {
                    continue;
                }
                int added = loadModuleSource(path, entries);
                if (added > 0) {
                    moduleEntries += added;
                    moduleSources.add(path);
                }
            }
            CacheLoadResult cacheLoad = Files.isRegularFile(resourcePackCache)
                    ? loadZip(resourcePackCache, entries, true)
                    : new CacheLoadResult(0, 0);
            return new ResourceIndex(
                    Map.copyOf(entries),
                    moduleEntries,
                    cacheLoad.added(),
                    cacheLoad.repaired(),
                    List.copyOf(moduleSources)
            );
        }

        private static boolean isEchoModuleSource(Path path, List<String> productModuleSourcePathMarkers) {
            String normalized = path.toString().replace('\\', '/').toLowerCase(java.util.Locale.ROOT);
            if (normalized.contains("/echo-native-m17-mods/")
                    || normalized.endsWith("/echo-native-m17-mods")) {
                return true;
            }
            for (String marker : productModuleSourcePathMarkers == null ? List.<String>of() : productModuleSourcePathMarkers) {
                if (!marker.isBlank() && normalized.contains(marker.replace('\\', '/').toLowerCase(java.util.Locale.ROOT))) {
                    return true;
                }
            }
            return false;
        }

        private static int loadModuleSource(Path path, Map<String, byte[]> entries) throws IOException {
            if (Files.isRegularFile(path) && path.getFileName().toString().endsWith(".jar")) {
                return loadZip(path, entries, false).added();
            }
            if (!Files.isDirectory(path)) {
                return 0;
            }
            int added = 0;
            try (var stream = Files.walk(path)) {
                for (Path file : stream.filter(Files::isRegularFile).toList()) {
                    String entry = path.relativize(file).toString().replace('\\', '/');
                    if (!isAssetEntry(entry) || entries.containsKey(entry)) {
                        continue;
                    }
                    entries.put(entry, Files.readAllBytes(file));
                    added++;
                }
            }
            return added;
        }

        private static CacheLoadResult loadZip(Path archive, Map<String, byte[]> entries, boolean fallbackOnly) throws IOException {
            int added = 0;
            int repaired = 0;
            try (ZipFile zip = new ZipFile(archive.toFile())) {
                Enumeration<? extends ZipEntry> zipEntries = zip.entries();
                while (zipEntries.hasMoreElements()) {
                    ZipEntry zipEntry = zipEntries.nextElement();
                    if (zipEntry.isDirectory()) {
                        continue;
                    }
                    String name = zipEntry.getName();
                    if (!isAssetEntry(name)) {
                        continue;
                    }
                    byte[] bytes;
                    try (var input = zip.getInputStream(zipEntry)) {
                        bytes = input.readAllBytes();
                    }
                    if (entries.containsKey(name)) {
                        if (fallbackOnly && shouldRepairBlockstate(name, entries.get(name), bytes)) {
                            entries.put(name, bytes);
                            repaired++;
                        }
                        continue;
                    }
                    entries.put(name, bytes);
                    added++;
                }
            }
            return new CacheLoadResult(added, repaired);
        }

        private static boolean isAssetEntry(String entry) {
            return entry.startsWith("assets/")
                    && !entry.endsWith("/")
                    && !entry.contains("/../")
                    && !entry.startsWith("../");
        }

        private int invalidIdentifierEntryCount() {
            int invalid = 0;
            for (String entry : entries.keySet()) {
                if (!entry.startsWith("assets/")) {
                    continue;
                }
                int namespaceStart = "assets/".length();
                int namespaceEnd = entry.indexOf('/', namespaceStart);
                if (namespaceEnd <= namespaceStart) {
                    invalid++;
                    continue;
                }
                String namespace = entry.substring(namespaceStart, namespaceEnd);
                String path = entry.substring(namespaceEnd + 1);
                if (!isValidIdentifierNamespace(namespace) || !isValidIdentifierPath(path)) {
                    invalid++;
                }
            }
            return invalid;
        }

        private static boolean shouldRepairBlockstate(String entry, byte[] primary, byte[] fallback) {
            if (!entry.contains("/blockstates/") || !entry.endsWith(".json")) {
                return false;
            }
            String primaryText = new String(primary, StandardCharsets.UTF_8);
            String fallbackText = new String(fallback, StandardCharsets.UTF_8);
            return normalizedFallbackBlockstate(fallbackText)
                    && ((primaryText.contains("\"variants\"") && primaryText.contains("="))
                    || primaryText.contains("\"multipart\""));
        }

        private static boolean normalizedFallbackBlockstate(String fallbackText) {
            return fallbackText.contains("\"variants\"")
                    && fallbackText.contains("\"\"")
                    && !fallbackText.contains("\"multipart\"")
                    && !fallbackText.contains("=");
        }
    }

    private record CacheLoadResult(int added, int repaired) {
    }

    private static String trimSlashes(String value) {
        String result = value.replace('\\', '/');
        while (result.startsWith("/")) {
            result = result.substring(1);
        }
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    private static Map<String, Object> baseEvidence(String packId, Path resourcePackCache, List<String> modules) {
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("schema", "echo.native.module_resource_pack_mount.v1");
        evidence.put("generatedAt", Instant.now().toString());
        evidence.put("packId", packId);
        evidence.put("moduleCount", modules.size());
        evidence.put("modules", modules.stream().sorted().toList());
        evidence.put("packResourcesImplementation", NativeLoaderModuleResourcePack.class.getName());
        evidence.put("packIdInternal", PACK_ID);
        evidence.put("resourcePackCache", resourcePackCache.toString());
        evidence.put("resourcePackCacheOnly", false);
        evidence.put("serverDataMountedSeparately", true);
        evidence.put("fallbacksOnlyForMissingAssets", true);
        evidence.put("mounted", false);
        return evidence;
    }

    private static void writeEvidence(Path evidencePath, Map<String, Object> evidence) {
        try {
            Files.createDirectories(evidencePath.getParent());
            Files.writeString(evidencePath, EchoNativeJson.write(evidence), StandardCharsets.UTF_8);
        } catch (IOException ignored) {
        }
    }
}
