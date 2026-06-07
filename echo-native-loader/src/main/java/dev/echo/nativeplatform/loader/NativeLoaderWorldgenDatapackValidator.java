package dev.echo.nativeplatform.loader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class NativeLoaderWorldgenDatapackValidator {
    public static final String SERVICE_ID = "echo.native.worldgen_datapack_validator";

    private NativeLoaderWorldgenDatapackValidator() {
    }

    public static Map<String, Object> validateProductWorldgenDatapack(
            Path datapack,
            String nativeWorldPresetMirrorSource,
            String nativeWorldPresetMirrorTarget,
            String nativeSourceResourceRootMarker,
            Map<String, String> nativeSaveDatapackRequiredEntriesByValidationKey,
            String nativeStructureTemplateSourcePrefix,
            String nativeStructureTemplateTargetPrefix,
            String nativeWorldgenBiomePrefix
    ) throws IOException {
        Set<String> names = new TreeSet<>();
        String mcmeta = "";
        try (ZipFile input = new ZipFile(datapack.toFile())) {
            java.util.Enumeration<? extends ZipEntry> entries = input.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String name = entry.getName().replace('\\', '/');
                if (entry.isDirectory()) {
                    continue;
                }
                names.add(name);
                if ("pack.mcmeta".equals(name)) {
                    mcmeta = new String(input.getInputStream(entry).readAllBytes(), StandardCharsets.UTF_8);
                }
            }
        }
        List<String> missing = new ArrayList<>();
        requireZipEntry(names, "pack.mcmeta", missing);
        requireZipEntry(names, nativeWorldPresetMirrorSource, missing);
        requireZipEntry(names, nativeWorldPresetMirrorTarget, missing);
        requireZipEntry(names, nativeSourceResourceRootMarker, missing);
        for (String requiredEntry : safeMap(nativeSaveDatapackRequiredEntriesByValidationKey).values()) {
            requireZipEntry(names, requiredEntry, missing);
        }
        String sourcePrefix = nativeStructureTemplateSourcePrefix == null ? "" : nativeStructureTemplateSourcePrefix;
        String targetPrefix = nativeStructureTemplateTargetPrefix == null ? "" : nativeStructureTemplateTargetPrefix;
        String biomePrefix = nativeWorldgenBiomePrefix == null ? "" : nativeWorldgenBiomePrefix;
        boolean pluralStructureTemplatesPresent = names.stream()
                .anyMatch(name -> !targetPrefix.isBlank()
                        && name.startsWith(targetPrefix)
                        && name.endsWith(".nbt"));
        boolean structureReadme = names.stream()
                .anyMatch(name -> ((!sourcePrefix.isBlank() && name.startsWith(sourcePrefix))
                        || (!targetPrefix.isBlank() && name.startsWith(targetPrefix)))
                        && (name.toLowerCase(java.util.Locale.ROOT).contains("/readme")
                        || name.toLowerCase(java.util.Locale.ROOT).endsWith(".txt")
                        || name.toLowerCase(java.util.Locale.ROOT).endsWith(".md")));
        boolean packFormat84 = mcmeta.contains("\"pack_format\"") && mcmeta.contains("84");
        boolean packRangePresent = mcmeta.contains("\"min_format\"") && mcmeta.contains("\"max_format\"");
        if (structureReadme) {
            missing.add("remove README/text entries under product structure template folders");
        }
        if (packFormat84 || !packRangePresent) {
            missing.add("pack.mcmeta source-like min_format/max_format");
        }
        int biomeCount = (int) names.stream()
                .filter(name -> !biomePrefix.isBlank()
                        && name.startsWith(biomePrefix)
                        && name.endsWith(".json"))
                .count();
        int structureCount = (int) names.stream()
                .filter(name -> !targetPrefix.isBlank()
                        && name.startsWith(targetPrefix)
                        && name.endsWith(".nbt"))
                .count();
        if (biomeCount <= 0) {
            missing.add(biomePrefix.isBlank() ? "product worldgen biome JSON" : biomePrefix + "*.json");
        }
        if (structureCount <= 0) {
            missing.add(targetPrefix.isBlank() ? "product structure templates" : targetPrefix + "*.nbt");
        }
        boolean accepted = missing.isEmpty();
        Map<String, Object> validation = new LinkedHashMap<>();
        validation.put("path", datapack.toString());
        validation.put("accepted", accepted);
        validation.put("entryCount", names.size());
        validation.put("biomeCount", biomeCount);
        validation.put("structureCount", structureCount);
        validation.put("normalPresetPresent", names.contains(nullSafe(nativeWorldPresetMirrorSource)));
        validation.put("productPresetPresent", names.contains(nullSafe(nativeWorldPresetMirrorTarget)));
        validation.put("productRootMarkerPresent", names.contains(nullSafe(nativeSourceResourceRootMarker)));
        for (Map.Entry<String, String> requiredEntry :
                safeMap(nativeSaveDatapackRequiredEntriesByValidationKey).entrySet()) {
            validation.put(requiredEntry.getKey(), names.contains(requiredEntry.getValue()));
        }
        validation.put("pluralStructureTemplatesPresent", pluralStructureTemplatesPresent);
        validation.put("stalePluralStructuresPresent", false);
        validation.put("packFormat84Present", packFormat84);
        validation.put("packRangePresent", packRangePresent);
        validation.put("failures", List.copyOf(missing));
        if (!accepted) {
            throw new IOException("Unsafe product save datapack: " + String.join(", ", missing));
        }
        return Map.copyOf(validation);
    }

    private static void requireZipEntry(Set<String> names, String required, List<String> missing) {
        if (required == null || required.isBlank()) {
            return;
        }
        if (!names.contains(required)) {
            missing.add(required);
        }
    }

    private static Map<String, String> safeMap(Map<String, String> value) {
        return value == null ? Map.of() : value;
    }

    private static String nullSafe(String value) {
        return value == null ? "" : value;
    }
}
