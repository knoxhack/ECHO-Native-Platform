package dev.echo.nativeplatform.loader;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class NativeLoaderWorldgenDatapackPolicy {
    public static final String SERVICE_ID = "echo.native.worldgen_datapack_policy";

    private NativeLoaderWorldgenDatapackPolicy() {
    }

    public static boolean isProductWorldgenDatapackEntry(
            String name,
            Collection<String> nativeSaveDatapackEntryPrefixes,
            String nativeStructureTemplateTargetPrefix
    ) {
        if (isUnsafeNativeSaveDatapackEntry(name, nativeStructureTemplateTargetPrefix)) {
            return false;
        }
        String lower = name == null ? "" : name.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".md") || lower.endsWith(".txt") || lower.contains("/readme")) {
            return false;
        }
        return safeCollection(nativeSaveDatapackEntryPrefixes).stream()
                .filter(prefix -> prefix != null && !prefix.isBlank())
                .anyMatch(name::startsWith);
    }

    public static boolean isUnsafeNativeSaveDatapackEntry(
            String name,
            String nativeStructureTemplateTargetPrefix
    ) {
        String lower = name == null ? "" : name.toLowerCase(Locale.ROOT);
        String structurePrefix = nativeStructureTemplateTargetPrefix == null ? "" : nativeStructureTemplateTargetPrefix;
        return !structurePrefix.isBlank()
                && lower.startsWith(structurePrefix.toLowerCase(Locale.ROOT))
                && (lower.endsWith(".md") || lower.endsWith(".txt") || lower.contains("/readme"));
    }

    public static void mirrorProductStructureTemplates(
            Map<String, byte[]> entries,
            String nativeStructureTemplateSourcePrefix,
            String nativeStructureTemplateTargetPrefix
    ) {
        String sourcePrefix = nativeStructureTemplateSourcePrefix == null ? "" : nativeStructureTemplateSourcePrefix;
        String targetPrefix = nativeStructureTemplateTargetPrefix == null ? "" : nativeStructureTemplateTargetPrefix;
        if (sourcePrefix.isBlank() || targetPrefix.isBlank() || entries == null) {
            return;
        }
        List<Map.Entry<String, byte[]>> singularTemplates = entries.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(sourcePrefix)
                        && entry.getKey().endsWith(".nbt"))
                .toList();
        for (Map.Entry<String, byte[]> entry : singularTemplates) {
            String pluralName = targetPrefix + entry.getKey().substring(sourcePrefix.length());
            entries.putIfAbsent(pluralName, entry.getValue());
        }
    }

    public static void mirrorProductWorldPreset(
            Map<String, byte[]> entries,
            String nativeWorldPresetMirrorSource,
            String nativeWorldPresetMirrorTarget
    ) {
        String source = nativeWorldPresetMirrorSource == null ? "" : nativeWorldPresetMirrorSource;
        String target = nativeWorldPresetMirrorTarget == null ? "" : nativeWorldPresetMirrorTarget;
        if (!source.isBlank() && !target.isBlank() && entries != null && entries.containsKey(source) && !entries.containsKey(target)) {
            entries.put(target, entries.get(source));
        }
    }

    private static Collection<String> safeCollection(Collection<String> values) {
        return values == null ? List.of() : values;
    }
}
