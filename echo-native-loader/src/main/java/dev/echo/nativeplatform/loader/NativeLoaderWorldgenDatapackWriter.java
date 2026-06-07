package dev.echo.nativeplatform.loader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public final class NativeLoaderWorldgenDatapackWriter {
    public static final String SERVICE_ID = "echo.native.worldgen_datapack_writer";
    private static final List<String> NATIVE_REGISTRY_UNSAFE_DATAPACK_TOKENS = List.of(
            "echoblockworks:ashstone_raw",
            "echoblockworks:hanging_wire",
            "minecraft:chain"
    );
    private static final List<String> NATIVE_REGISTRY_SAFE_DATAPACK_STAND_INS = List.of(
            "minecraft:stone",
            "minecraft:iron_bars"
    );

    private NativeLoaderWorldgenDatapackWriter() {
    }

    public static int writeProductWorldgenDatapack(
            Path resourcePack,
            Path datapack,
            String nativeSaveDatapackDescription,
            Collection<String> nativeSaveDatapackEntryPrefixes,
            String nativeStructureTemplateTargetPrefix,
            String nativeStructureTemplateSourcePrefix,
            String namespace,
            String nativeWorldgenStructurePrefix,
            String nativeWorldgenBiomePrefix
    ) throws IOException {
        Map<String, byte[]> entries = new TreeMap<>();
        entries.put("pack.mcmeta", NativeLoaderPackMetadata.dataPackMcmeta(nativeSaveDatapackDescription));
        try (ZipFile input = new ZipFile(resourcePack.toFile())) {
            Enumeration<? extends ZipEntry> zipEntries = input.entries();
            while (zipEntries.hasMoreElements()) {
                ZipEntry entry = zipEntries.nextElement();
                String name = entry.getName().replace('\\', '/');
                if (entry.isDirectory() || !NativeLoaderWorldgenDatapackPolicy.isProductWorldgenDatapackEntry(
                        name,
                        nativeSaveDatapackEntryPrefixes,
                        nativeStructureTemplateTargetPrefix
                ) || entries.containsKey(name)) {
                    continue;
                }
                try (InputStream entryInput = input.getInputStream(entry)) {
                    entries.put(name, sanitizeNativeRegistryUnsafeDatapackEntry(
                            name,
                            entryInput.readAllBytes(),
                            namespace,
                            nativeWorldgenStructurePrefix
                    ));
                }
            }
        }
        NativeLoaderWorldgenDatapackPolicy.mirrorProductStructureTemplates(
                entries,
                nativeStructureTemplateSourcePrefix,
                nativeStructureTemplateTargetPrefix
        );
        NativeLoaderBiomeSpawnSanitizer.sanitizeNativeProductBiomeSpawns(
                entries,
                nativeWorldgenBiomePrefix,
                namespace
        );
        putNativeWorldgenCompatibilityTags(entries);
        try (ZipOutputStream output = new ZipOutputStream(Files.newOutputStream(datapack))) {
            for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
                output.putNextEntry(new ZipEntry(entry.getKey()));
                output.write(entry.getValue());
                output.closeEntry();
            }
        }
        return entries.size();
    }

    private static void putNativeWorldgenCompatibilityTags(Map<String, byte[]> entries) {
        if (entries == null) {
            return;
        }
        entries.putIfAbsent(
                "data/minecraft/tags/block/overworld_carver_replaceables.json",
                """
                        {
                          "replace": false,
                          "values": [
                            "minecraft:stone",
                            "minecraft:granite",
                            "minecraft:diorite",
                            "minecraft:andesite",
                            "minecraft:dirt",
                            "minecraft:grass_block",
                            "minecraft:deepslate",
                            "minecraft:tuff",
                            "minecraft:sand",
                            "minecraft:gravel"
                          ]
                        }
                        """.getBytes(StandardCharsets.UTF_8)
        );
        entries.putIfAbsent(
                "data/minecraft/tags/block/nether_carver_replaceables.json",
                """
                        {
                          "replace": false,
                          "values": [
                            "minecraft:netherrack",
                            "minecraft:basalt",
                            "minecraft:blackstone"
                          ]
                        }
                        """.getBytes(StandardCharsets.UTF_8)
        );
    }

    private static byte[] sanitizeNativeRegistryUnsafeDatapackEntry(
            String name,
            byte[] bytes,
            String namespace,
            String nativeWorldgenStructurePrefix
    ) {
        if (bytes == null) {
            return bytes;
        }
        String productNamespace = namespace == null ? "" : namespace;
        boolean nativeWorldgenJson = name != null
                && (name.startsWith("data/" + productNamespace + "/worldgen/")
                || name.startsWith("data/minecraft/worldgen/")
                || name.startsWith("data/minecraft/tags/block/"))
                && name.endsWith(".json");
        if (!nativeWorldgenJson) {
            return NativeLoaderResourceSanitizer.sanitizeProductDatapackEntry(
                    name,
                    bytes,
                    productNamespace,
                    nativeWorldgenStructurePrefix
            );
        }
        String json = new String(bytes, StandardCharsets.UTF_8);
        boolean unsafeNativeRegistryToken = NATIVE_REGISTRY_UNSAFE_DATAPACK_TOKENS.stream().anyMatch(json::contains);
        String sanitizedJson = NativeLoaderResourceSanitizer.sanitizeAshfallNativeWorldgenFeatureReferences(
                NativeLoaderResourceSanitizer.sanitizeNativeRegistryUnsafeJson(json)
        );
        if (name != null && name.startsWith("data/minecraft/tags/block/")) {
            sanitizedJson = NativeLoaderResourceSanitizer.sanitizeMinecraftBlockTagJson(name, sanitizedJson);
        }
        if (!unsafeNativeRegistryToken && sanitizedJson.equals(json)) {
            return NativeLoaderResourceSanitizer.sanitizeProductDatapackEntry(
                    name,
                    bytes,
                    productNamespace,
                    nativeWorldgenStructurePrefix
            );
        }
        for (String standIn : NATIVE_REGISTRY_SAFE_DATAPACK_STAND_INS) {
            if (sanitizedJson.contains(standIn)) {
                return sanitizedJson.getBytes(StandardCharsets.UTF_8);
            }
        }
        return sanitizedJson.getBytes(StandardCharsets.UTF_8);
    }

    private static Collection<String> safeCollection(Collection<String> values) {
        return values == null ? List.of() : values;
    }
}
