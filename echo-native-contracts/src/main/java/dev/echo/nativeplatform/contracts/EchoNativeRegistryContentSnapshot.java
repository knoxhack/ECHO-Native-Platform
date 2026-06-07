package dev.echo.nativeplatform.contracts;

import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@EchoNativeApiStatus(value = EchoNativeApiStability.BETA, since = "0.1.0-native-beta")
public record EchoNativeRegistryContentSnapshot(
        List<EchoNativeRegistryContentDefinition> definitions,
        List<String> terminalPages,
        List<String> indexEntries,
        Map<String, List<String>> resourceIdsByKind,
        Map<String, List<String>> resourcePathsById,
        Map<String, List<String>> resourceFileFingerprintsByKind,
        Map<String, String> langKeysByContentId,
        Map<String, String> langValuesByContentId,
        Map<String, List<String>> jsonResourceIdsByKind,
        List<String> jsonParseIssues,
        Map<String, List<String>> modelDependenciesById,
        Map<String, List<String>> textureDependenciesById,
        Map<String, List<String>> soundDependenciesById,
        List<String> unresolvedModelReferences,
        List<String> unresolvedTextureReferences,
        List<String> unresolvedSoundReferences
) {
    public EchoNativeRegistryContentSnapshot(
            List<EchoNativeRegistryContentDefinition> definitions,
            List<String> terminalPages,
            List<String> indexEntries
    ) {
        this(definitions, terminalPages, indexEntries, Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), List.of(), Map.of(), Map.of(), Map.of(), List.of(), List.of(), List.of());
    }

    public EchoNativeRegistryContentSnapshot(
            List<EchoNativeRegistryContentDefinition> definitions,
            List<String> terminalPages,
            List<String> indexEntries,
            Map<String, List<String>> resourceIdsByKind
    ) {
        this(definitions, terminalPages, indexEntries, resourceIdsByKind, Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), List.of(), Map.of(), Map.of(), Map.of(), List.of(), List.of(), List.of());
    }

    public EchoNativeRegistryContentSnapshot {
        definitions = List.copyOf(Objects.requireNonNull(definitions, "definitions"));
        terminalPages = List.copyOf(Objects.requireNonNull(terminalPages, "terminalPages"));
        indexEntries = List.copyOf(Objects.requireNonNull(indexEntries, "indexEntries"));
        resourceIdsByKind = copyMap(resourceIdsByKind, "resourceIdsByKind");
        resourcePathsById = copyMap(resourcePathsById, "resourcePathsById");
        resourceFileFingerprintsByKind = copyMap(resourceFileFingerprintsByKind, "resourceFileFingerprintsByKind");
        langKeysByContentId = Map.copyOf(Objects.requireNonNull(langKeysByContentId, "langKeysByContentId"));
        langValuesByContentId = Map.copyOf(Objects.requireNonNull(langValuesByContentId, "langValuesByContentId"));
        jsonResourceIdsByKind = copyMap(jsonResourceIdsByKind, "jsonResourceIdsByKind");
        jsonParseIssues = List.copyOf(Objects.requireNonNull(jsonParseIssues, "jsonParseIssues"));
        modelDependenciesById = copyMap(modelDependenciesById, "modelDependenciesById");
        textureDependenciesById = copyMap(textureDependenciesById, "textureDependenciesById");
        soundDependenciesById = copyMap(soundDependenciesById, "soundDependenciesById");
        unresolvedModelReferences = List.copyOf(Objects.requireNonNull(unresolvedModelReferences, "unresolvedModelReferences"));
        unresolvedTextureReferences = List.copyOf(Objects.requireNonNull(unresolvedTextureReferences, "unresolvedTextureReferences"));
        unresolvedSoundReferences = List.copyOf(Objects.requireNonNull(unresolvedSoundReferences, "unresolvedSoundReferences"));
    }

    public Map<String, List<String>> contentIdsByRegistry() {
        Map<String, List<String>> ids = new LinkedHashMap<>();
        for (EchoNativeRegistryContentDefinition definition : definitions) {
            ids.computeIfAbsent(definition.registry(), ignored -> new java.util.ArrayList<>())
                    .add(definition.id());
        }
        Map<String, List<String>> sorted = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> entry : ids.entrySet()) {
            sorted.put(entry.getKey(), entry.getValue().stream().sorted().toList());
        }
        return sorted;
    }

    public List<String> requiredAssetIssues() {
        if (resourceIdsByKind.isEmpty()) {
            return definitions.stream()
                    .flatMap(definition -> definition.requiredAssetIssues().stream()
                            .map(issue -> definition.id() + " " + issue))
                    .toList();
        }
        List<String> issues = new ArrayList<>();
        Set<String> blockstates = Set.copyOf(resourceIdsByKind.getOrDefault("blockstates", List.of()));
        Set<String> models = Set.copyOf(resourceIdsByKind.getOrDefault("models", List.of()));
        Set<String> textures = Set.copyOf(resourceIdsByKind.getOrDefault("textures", List.of()));
        for (EchoNativeRegistryContentDefinition definition : definitions) {
            if ("blocks".equals(definition.registry())) {
                requireResource(issues, definition.id(), "blockstate", definition.blockstate(), blockstates);
                requireResource(issues, definition.id(), "model", definition.model(), models);
                requireResource(issues, definition.id(), "texture", definition.texture(), textures);
                requireLang(issues, definition.id(), definition.lang(), langKeysByContentId, langValuesByContentId);
            } else if ("items".equals(definition.registry()) || "entities".equals(definition.registry())) {
                requireResource(issues, definition.id(), "model", definition.model(), models);
                requireResource(issues, definition.id(), "texture", definition.texture(), textures);
                requireLang(issues, definition.id(), definition.lang(), langKeysByContentId, langValuesByContentId);
            }
        }
        return List.copyOf(issues);
    }

    public int searchVisibleContentCount() {
        return searchVisibleContentIds().size();
    }

    public List<String> searchVisibleContentIds() {
        return definitions.stream()
                .filter(EchoNativeRegistryContentDefinition::searchVisible)
                .map(EchoNativeRegistryContentDefinition::id)
                .sorted()
                .toList();
    }

    public int mergedTagOverlayCount() {
        return definitions.stream()
                .filter(definition -> "tags".equals(definition.registry()))
                .mapToInt(definition -> Math.max(0, definition.mergedSourceCount() - 1))
                .sum();
    }

    public Map<String, Integer> tagMergedSourceCountsById() {
        Map<String, Integer> counts = new LinkedHashMap<>();
        definitions.stream()
                .filter(definition -> "tags".equals(definition.registry()))
                .sorted(java.util.Comparator.comparing(EchoNativeRegistryContentDefinition::id))
                .forEach(definition -> counts.put(definition.id(), definition.mergedSourceCount()));
        return Map.copyOf(counts);
    }

    public int materializedResourceCount() {
        return resourceIdsByKind.values().stream().mapToInt(List::size).sum();
    }

    public int assetIndexEntryCount() {
        return countKinds("assets", "blockstates", "models", "textures", "lang", "sounds", "uiThemes", "terminalPages");
    }

    public int dataDefinitionEntryCount() {
        return countKinds("data", "recipes", "lootTables", "tags", "structures", "worldRegions", "worldHazards", "missionJson");
    }

    public Map<String, List<String>> duplicateResourceIdsByKind() {
        Map<String, List<String>> duplicates = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> entry : resourceIdsByKind.entrySet()) {
            Map<String, Integer> counts = new LinkedHashMap<>();
            for (String id : entry.getValue()) {
                counts.put(id, counts.getOrDefault(id, 0) + 1);
            }
            List<String> duplicateIds = counts.entrySet().stream()
                    .filter(item -> item.getValue() > 1)
                    .map(Map.Entry::getKey)
                    .sorted()
                    .toList();
            if (!duplicateIds.isEmpty()) {
                duplicates.put(entry.getKey(), duplicateIds);
            }
        }
        return Map.copyOf(duplicates);
    }

    public List<String> mergeableTagOverlayIds() {
        return duplicateResourceIdsByKind().getOrDefault("tags", List.of());
    }

    public List<String> blockingDuplicateResourceIds() {
        return duplicateResourceIdsByKind().entrySet().stream()
                .filter(entry -> !"tags".equals(entry.getKey()))
                .flatMap(entry -> entry.getValue().stream().map(id -> entry.getKey() + "|" + id))
                .sorted()
                .toList();
    }

    private int countKinds(String... kinds) {
        int count = 0;
        for (String kind : kinds) {
            count += resourceIdsByKind.getOrDefault(kind, List.of()).size();
        }
        return count;
    }

    private static Map<String, List<String>> copyMap(Map<String, List<String>> values, String fieldName) {
        Objects.requireNonNull(values, fieldName);
        Map<String, List<String>> copy = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> entry : values.entrySet()) {
            copy.put(entry.getKey(), List.copyOf(entry.getValue()));
        }
        return copy;
    }

    private static void requireResource(
            List<String> issues,
            String contentId,
            String assetKind,
            String assetId,
            Set<String> mountedIds
    ) {
        if (assetId == null || assetId.isBlank()) {
            issues.add(contentId + " missing " + assetKind);
            return;
        }
        if (!assetId.startsWith("minecraft:") && !mountedIds.contains(assetId)) {
            issues.add(contentId + " unresolved " + assetKind + " " + assetId);
        }
    }

    private static void requireLang(
            List<String> issues,
            String contentId,
            String lang,
            Map<String, String> langKeysByContentId,
            Map<String, String> langValuesByContentId
    ) {
        if (lang == null || lang.isBlank()) {
            issues.add(contentId + " missing lang");
            return;
        }
        if (langKeysByContentId.isEmpty() && langValuesByContentId.isEmpty()) {
            return;
        }
        if (!langKeysByContentId.containsKey(contentId)) {
            issues.add(contentId + " missing lang key");
            return;
        }
        String mountedValue = langValuesByContentId.get(contentId);
        if (mountedValue == null || mountedValue.isBlank()) {
            issues.add(contentId + " unresolved lang key " + langKeysByContentId.get(contentId));
        } else if (!mountedValue.equals(lang)) {
            issues.add(contentId + " lang value mismatch " + langKeysByContentId.get(contentId));
        }
    }
}
