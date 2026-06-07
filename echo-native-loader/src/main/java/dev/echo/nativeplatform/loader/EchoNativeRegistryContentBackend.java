package dev.echo.nativeplatform.loader;

import dev.echo.nativeplatform.contracts.EchoNativeRegistryContentDefinition;
import dev.echo.nativeplatform.contracts.EchoNativeRegistryContentSnapshot;
import dev.echo.nativeplatform.contracts.EchoNativeRegistryRuntimeResolution;
import dev.echo.nativeplatform.diagnostics.EchoNativeJson;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

public final class EchoNativeRegistryContentBackend {
    public static final String RUNTIME_ID = "echo-native-loader";
    public static final Path MATERIALIZED_RESOURCE_PACK_ROOT =
            materializedResourcePackRoot();
    private static final Set<String> DATA_DEFINITION_KINDS = Set.of(
            "data",
            "recipes",
            "lootTables",
            "tags",
            "structures",
            "worldRegions",
            "worldHazards",
            "uiThemes",
            "terminalPages",
            "missionJson"
    );

    private static Path materializedResourcePackRoot() {
        String configuredRoot = System.getProperty("echo.native.registry.resourcePackRoot", "").trim();
        if (!configuredRoot.isEmpty()) {
            return Path.of(configuredRoot);
        }
        return Path.of("..", "reports", "echo", "assets", "native-materialized-resource-pack");
    }

    public EchoNativeRegistryContentSnapshot loadContractReport(Path contractReport, Path resourceIndexReport) throws IOException {
        Map<String, Object> contract = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(contractReport)));
        Map<String, Object> resourceIndex = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(resourceIndexReport)));
        List<EchoNativeRegistryContentDefinition> definitions = new ArrayList<>();
        readBlocks(contract, definitions);
        readItems(contract, definitions);
        readEntities(contract, definitions);
        readRecipes(contract, definitions);
        readLoot(contract, definitions);
        readSounds(contract, definitions);
        readStructures(contract, definitions);
        readTags(contract, definitions);
        readCreativeGroups(contract, definitions);
        Map<String, String> translationsByKey = langTranslationsByKey(resourceIndex);
        Map<String, String> langKeysByContentId = langKeysByContentId(contract, translationsByKey);
        return new EchoNativeRegistryContentSnapshot(
                definitions,
                resourceIds(resourceIndex, "terminalPages"),
                indexEntries(resourceIndex),
                resourceIdsByKind(resourceIndex),
                resourcePathsById(resourceIndex),
                resourceFileFingerprintsByKind(resourceIndex),
                langKeysByContentId,
                langValuesByContentId(langKeysByContentId, translationsByKey),
                jsonResourceIdsByKind(resourceIndex),
                jsonParseIssues(resourceIndex),
                modelDependenciesById(resourceIndex),
                textureDependenciesById(resourceIndex),
                soundDependenciesById(resourceIndex),
                unresolvedModelReferences(resourceIndex),
                unresolvedTextureReferences(resourceIndex),
                unresolvedSoundReferences(resourceIndex)
        );
    }

    public EchoNativeRegistryRuntimeResolution resolve(EchoNativeRegistryContentSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        Map<String, List<String>> recipes = bindByRegistry(snapshot, "recipes", BindingSource.OUTPUTS);
        Map<String, List<String>> recipeInputs = bindByRegistry(snapshot, "recipes", BindingSource.INPUTS);
        Map<String, String> recipeTypes = kindByRegistry(snapshot, "recipes");
        Map<String, List<String>> loot = bindByRegistry(snapshot, "lootTables", BindingSource.ENTRIES);
        Map<String, List<String>> sounds = bindByRegistry(snapshot, "sounds", BindingSource.ENTRIES);
        Map<String, List<String>> structures = bindByRegistry(snapshot, "structures", BindingSource.ENTRIES);
        Map<String, String> structureTypes = kindByRegistry(snapshot, "structures");
        Map<String, List<String>> tags = bindByRegistry(snapshot, "tags", BindingSource.ENTRIES);
        Map<String, List<String>> contentTags = contentTagIds(snapshot, tags);
        Map<String, List<String>> creativeGroups = bindByRegistry(snapshot, "creativeGroups", BindingSource.ENTRIES);
        Map<String, String> blockstateAssets = mapBlockstateAssets(snapshot);
        Map<String, String> rendererAssets = mapRendererAssets(snapshot);
        Map<String, String> textureAssets = mapTextureAssets(snapshot);
        Map<String, List<String>> contentAssets = contentAssetBindings(snapshot);
        Map<String, List<String>> resolvedContentAssetPaths = resolvedContentAssetPaths(snapshot, contentAssets);
        Map<String, List<String>> searchIndexTerms = searchIndexTerms(snapshot, contentTags);
        List<String> uiAssets = uiAssets(snapshot);
        boolean resourcePackMaterialized = materializeResourcePack(snapshot);

        return new EchoNativeRegistryRuntimeResolution(
                RUNTIME_ID,
                registerContentDefinitions(snapshot),
                recipes,
                recipeInputs,
                reverseLookup(recipeInputs),
                reverseLookup(recipes),
                recipeTypes,
                recipeDataPathsById(snapshot),
                loot,
                lootDataPathsById(snapshot),
                reverseLookup(loot),
                sounds,
                soundDefinitionDataPathsById(snapshot),
                structures,
                structureTypes,
                buildStructureReferenceLookup(structures),
                structureAssetPathsById(snapshot),
                tags,
                tagDataPathsById(snapshot),
                buildTagValueLookup(tags),
                contentTags,
                creativeGroups,
                buildCreativeGroupMembership(creativeGroups),
                snapshot.resourceIdsByKind(),
                snapshot.resourcePathsById(),
                resourceIdsByNamespace(snapshot.resourcePathsById()),
                stagedResourceContentFingerprints(MATERIALIZED_RESOURCE_PACK_ROOT),
                snapshot.resourceFileFingerprintsByKind(),
                resourceManifestFingerprint(snapshot.resourceFileFingerprintsByKind()),
                snapshot.langKeysByContentId(),
                snapshot.langValuesByContentId(),
                langAssetsByContentId(snapshot.langKeysByContentId()),
                buildLangLookup(snapshot.langKeysByContentId()),
                buildLangLookup(snapshot.langValuesByContentId()),
                snapshot.jsonResourceIdsByKind(),
                dataDefinitionIdsByKind(snapshot.jsonResourceIdsByKind()),
                dataDefinitionPathsById(snapshot),
                stagedJsonContentFingerprintsById(MATERIALIZED_RESOURCE_PACK_ROOT, snapshot.resourcePathsById()),
                registrySourcePathsById(snapshot),
                snapshot.jsonParseIssues(),
                snapshot.modelDependenciesById(),
                snapshot.textureDependenciesById(),
                buildDependencyPaths(snapshot.modelDependenciesById(), ".json"),
                buildDependencyPaths(snapshot.textureDependenciesById(), ".png"),
                snapshot.soundDependenciesById(),
                buildSoundAssetEventLookup(snapshot.soundDependenciesById()),
                buildSoundAssetPaths(snapshot.soundDependenciesById()),
                snapshot.unresolvedModelReferences(),
                snapshot.unresolvedTextureReferences(),
                snapshot.unresolvedSoundReferences(),
                snapshot.terminalPages(),
                snapshot.indexEntries(),
                terminalPageDataPathsById(snapshot),
                indexEntryDataPathsById(snapshot),
                resourceIds(snapshot, "missionJson"),
                resourceIds(snapshot, "worldRegions"),
                resourceIds(snapshot, "worldHazards"),
                missionJsonDataPathsById(snapshot),
                worldRegionDataPathsById(snapshot),
                worldHazardDataPathsById(snapshot),
                resourceIds(snapshot, "uiThemes"),
                snapshot.requiredAssetIssues(),
                resourcePackMaterialized,
                snapshot.materializedResourceCount(),
                snapshot.assetIndexEntryCount(),
                snapshot.dataDefinitionEntryCount(),
                snapshot.duplicateResourceIdsByKind(),
                snapshot.mergeableTagOverlayIds(),
                snapshot.blockingDuplicateResourceIds(),
                snapshot.definitions().size(),
                snapshot.searchVisibleContentIds(),
                snapshot.searchVisibleContentCount(),
                searchIndexTerms,
                reverseLookup(searchIndexTerms),
                contentAssets,
                resolvedContentAssetPaths,
                requiredAssetChecksByContentId(contentAssets, resolvedContentAssetPaths),
                buildContentAssetLookup(contentAssets),
                blockstateAssets,
                rendererAssets,
                textureAssets,
                contentAssetPaths(blockstateAssets, ".json"),
                contentAssetPaths(rendererAssets, ".json"),
                contentAssetPaths(textureAssets, ".png"),
                contentAssetPaths(langAssetsByContentId(snapshot.langKeysByContentId()), ".json"),
                uiAssets,
                buildUiAssetPaths(uiAssets),
                textureForgeSpecIds(snapshot),
                textureForgeSpecOutputPathsById(snapshot),
                textureForgeSpecStatusById(snapshot),
                textureForgeSpecStyleFamilyById(snapshot),
                textureForgeSpecPromptFieldsById(snapshot),
                textureForgeSpecDataPathsById(snapshot),
                snapshot.tagMergedSourceCountsById(),
                snapshot.mergedTagOverlayCount(),
                itemLinks(snapshot, BindingSource.OUTPUTS),
                itemLinks(snapshot, BindingSource.ENTRIES),
                registryJsonContentFingerprintsByKind(MATERIALIZED_RESOURCE_PACK_ROOT, snapshot.resourcePathsById()),
                catalogJsonContentFingerprintsByKind(MATERIALIZED_RESOURCE_PACK_ROOT, snapshot.resourcePathsById()),
                catalogReferencesByKind(snapshot, "terminalPages"),
                catalogReferencesByKind(snapshot, "data"),
                catalogReferencesByKind(snapshot, "missionJson"),
                catalogReferencesByKind(snapshot, "worldRegions"),
                catalogReferencesByKind(snapshot, "worldHazards")
        );
    }

    private static void readBlocks(Map<String, Object> contract, List<EchoNativeRegistryContentDefinition> definitions) {
        for (Map<String, Object> item : objects(contract.get("blocks"))) {
            definitions.add(EchoNativeRegistryContentDefinition.block(
                    text(item, "id"),
                    text(item, "addon"),
                    text(item, "blockstate"),
                    text(item, "model"),
                    text(item, "texture"),
                    text(item, "lang")
            ));
        }
    }

    private static void readItems(Map<String, Object> contract, List<EchoNativeRegistryContentDefinition> definitions) {
        for (Map<String, Object> item : objects(contract.get("items"))) {
            definitions.add(EchoNativeRegistryContentDefinition.item(
                    text(item, "id"),
                    text(item, "addon"),
                    text(item, "model"),
                    text(item, "texture"),
                    text(item, "lang"),
                    EchoNativeJson.stringList(item.get("recipes")),
                    EchoNativeJson.stringList(item.get("lootTables")),
                    Boolean.TRUE.equals(item.get("searchVisible"))
            ));
        }
    }

    private static void readEntities(Map<String, Object> contract, List<EchoNativeRegistryContentDefinition> definitions) {
        for (Map<String, Object> item : objects(contract.get("entities"))) {
            definitions.add(EchoNativeRegistryContentDefinition.entity(
                    text(item, "id"),
                    text(item, "addon"),
                    text(item, "model"),
                    text(item, "texture"),
                    text(item, "lang")
            ));
        }
    }

    private static void readRecipes(Map<String, Object> contract, List<EchoNativeRegistryContentDefinition> definitions) {
        for (Map<String, Object> item : objects(contract.get("recipes"))) {
            definitions.add(EchoNativeRegistryContentDefinition.recipe(
                    text(item, "id"),
                    text(item, "addon"),
                    text(item, "type"),
                    EchoNativeJson.stringList(item.get("inputs")),
                    EchoNativeJson.stringList(item.get("outputs")),
                    text(item, "source")
            ));
        }
    }

    private static void readLoot(Map<String, Object> contract, List<EchoNativeRegistryContentDefinition> definitions) {
        for (Map<String, Object> item : objects(contract.get("loot"))) {
            definitions.add(EchoNativeRegistryContentDefinition.lootTable(
                    text(item, "id"),
                    text(item, "addon"),
                    EchoNativeJson.stringList(item.get("entries")),
                    text(item, "source")
            ));
        }
    }

    private static void readSounds(Map<String, Object> contract, List<EchoNativeRegistryContentDefinition> definitions) {
        for (Map<String, Object> item : objects(contract.get("sounds"))) {
            definitions.add(new EchoNativeRegistryContentDefinition(
                    "sounds", text(item, "id"), text(item, "addon"), "", "", "", "", "",
                    List.of(), List.of(), EchoNativeJson.stringList(item.get("sounds")),
                    text(item, "source"), false, 1
            ));
        }
    }

    private static void readStructures(Map<String, Object> contract, List<EchoNativeRegistryContentDefinition> definitions) {
        for (Map<String, Object> item : objects(contract.get("structures"))) {
            definitions.add(new EchoNativeRegistryContentDefinition(
                    "structures", text(item, "id"), text(item, "addon"), text(item, "kind"), "", "", "", "",
                    List.of(), List.of(), EchoNativeJson.stringList(item.get("references")),
                    text(item, "source"), false, 1
            ));
        }
    }

    private static void readTags(Map<String, Object> contract, List<EchoNativeRegistryContentDefinition> definitions) {
        for (Map<String, Object> item : objects(contract.get("tags"))) {
            definitions.add(EchoNativeRegistryContentDefinition.tag(
                    text(item, "id"),
                    text(item, "addon"),
                    text(item, "kind"),
                    EchoNativeJson.stringList(item.get("values")),
                    text(item, "source"),
                    integer(item.get("mergedSourceCount"), 1)
            ));
        }
    }

    private static void readCreativeGroups(Map<String, Object> contract, List<EchoNativeRegistryContentDefinition> definitions) {
        for (Map<String, Object> item : objects(contract.get("creativeGroups"))) {
            definitions.add(new EchoNativeRegistryContentDefinition(
                    "creativeGroups", text(item, "id"), text(item, "addon"), "", "", "", "", "",
                    List.of(), List.of(), EchoNativeJson.stringList(item.get("entries")),
                    text(item, "source"), false, 1
            ));
        }
    }

    private static List<String> resourceIds(Map<String, Object> resourceIndex, String kind) {
        return objects(resourceIndex.get("entries")).stream()
                .filter(item -> kind.equals(text(item, "kind")))
                .map(item -> text(item, "id"))
                .sorted()
                .toList();
    }

    private static List<String> resourceIds(EchoNativeRegistryContentSnapshot snapshot, String kind) {
        return snapshot.resourceIdsByKind().getOrDefault(kind, List.of()).stream().sorted().toList();
    }

    private static List<String> indexEntries(Map<String, Object> resourceIndex) {
        return objects(resourceIndex.get("entries")).stream()
                .filter(item -> text(item, "path").replace("\\", "/").contains("/echoindex/entries/"))
                .map(item -> text(item, "id"))
                .sorted()
                .toList();
    }

    private static Map<String, List<String>> resourceIdsByKind(Map<String, Object> resourceIndex) {
        Map<String, List<String>> ids = new LinkedHashMap<>();
        for (Map<String, Object> item : objects(resourceIndex.get("entries"))) {
            ids.computeIfAbsent(text(item, "kind"), ignored -> new ArrayList<>())
                    .add(text(item, "id"));
        }
        Map<String, List<String>> sorted = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> entry : ids.entrySet()) {
            sorted.put(entry.getKey(), entry.getValue().stream().sorted().toList());
        }
        return sorted;
    }

    private static Map<String, List<String>> resourcePathsById(Map<String, Object> resourceIndex) {
        Map<String, List<String>> paths = new LinkedHashMap<>();
        for (Map<String, Object> item : objects(resourceIndex.get("entries"))) {
            Path path = resourcePackPath(Path.of(text(item, "path")));
            paths.computeIfAbsent(text(item, "kind") + "|" + text(item, "id"), ignored -> new ArrayList<>())
                    .add(path.toString().replace("\\", "/"));
        }
        Map<String, List<String>> sorted = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> entry : paths.entrySet()) {
            sorted.put(entry.getKey(), entry.getValue().stream().distinct().sorted().toList());
        }
        return sorted;
    }

    private static Map<String, List<String>> resourceIdsByNamespace(Map<String, List<String>> resourcePathsById) {
        Map<String, List<String>> ids = new TreeMap<>();
        for (String resourceKey : resourcePathsById.keySet()) {
            int separator = resourceKey.indexOf('|');
            if (separator < 0 || separator == resourceKey.length() - 1) {
                continue;
            }
            String id = resourceKey.substring(separator + 1);
            int namespaceSeparator = id.indexOf(':');
            if (namespaceSeparator <= 0) {
                continue;
            }
            ids.computeIfAbsent(id.substring(0, namespaceSeparator), ignored -> new ArrayList<>())
                    .add(resourceKey);
        }
        Map<String, List<String>> sorted = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> entry : ids.entrySet()) {
            sorted.put(entry.getKey(), entry.getValue().stream().distinct().sorted().toList());
        }
        return sorted;
    }

    private static Map<String, List<String>> resourceFileFingerprintsByKind(Map<String, Object> resourceIndex) {
        Map<String, List<String>> fingerprints = new LinkedHashMap<>();
        for (Map<String, Object> item : objects(resourceIndex.get("entries"))) {
            fingerprints.computeIfAbsent(text(item, "kind"), ignored -> new ArrayList<>())
                    .add(resourceFingerprint(item));
        }
        Map<String, List<String>> sorted = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> entry : fingerprints.entrySet()) {
            sorted.put(entry.getKey(), entry.getValue().stream().sorted().toList());
        }
        return sorted;
    }

    private static String resourceFingerprint(Map<String, Object> item) {
        return text(item, "id")
                + "|" + text(item, "sha256")
                + "|" + text(item, "size")
                + "|" + text(item, "path").replace("\\", "/");
    }

    private static Map<String, List<String>> jsonResourceIdsByKind(Map<String, Object> resourceIndex) {
        Map<String, List<String>> ids = new LinkedHashMap<>();
        for (Map<String, Object> item : objects(resourceIndex.get("entries"))) {
            if (!isJsonResource(item)) {
                continue;
            }
            try {
                EchoNativeJson.parse(Files.readString(Path.of(text(item, "path"))));
                ids.computeIfAbsent(text(item, "kind"), ignored -> new ArrayList<>())
                        .add(text(item, "id"));
            } catch (RuntimeException | IOException ignored) {
                // jsonParseIssues carries the failing resource identity.
            }
        }
        Map<String, List<String>> sorted = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> entry : ids.entrySet()) {
            sorted.put(entry.getKey(), entry.getValue().stream().sorted().toList());
        }
        return sorted;
    }

    private static Map<String, List<String>> dataDefinitionIdsByKind(Map<String, List<String>> jsonResourceIdsByKind) {
        Map<String, List<String>> ids = new LinkedHashMap<>();
        for (String kind : DATA_DEFINITION_KINDS.stream().sorted().toList()) {
            List<String> values = jsonResourceIdsByKind.getOrDefault(kind, List.of());
            if (!values.isEmpty()) {
                ids.put(kind, values);
            }
        }
        return ids;
    }

    private static Map<String, List<String>> dataDefinitionPathsById(EchoNativeRegistryContentSnapshot snapshot) {
        Map<String, List<String>> paths = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> entry : dataDefinitionIdsByKind(snapshot.jsonResourceIdsByKind()).entrySet()) {
            String kind = entry.getKey();
            for (String id : entry.getValue()) {
                List<String> resolved = snapshot.resourcePathsById()
                        .getOrDefault(kind + "|" + id, List.of())
                        .stream()
                        .filter(path -> path.endsWith(".json"))
                        .sorted()
                        .toList();
                if (!resolved.isEmpty()) {
                    paths.put(kind + "|" + id, resolved);
                }
            }
        }
        return paths;
    }

    private static List<String> jsonParseIssues(Map<String, Object> resourceIndex) {
        ArrayList<String> issues = new ArrayList<>();
        for (Map<String, Object> item : objects(resourceIndex.get("entries"))) {
            if (!isJsonResource(item)) {
                continue;
            }
            try {
                EchoNativeJson.parse(Files.readString(Path.of(text(item, "path"))));
            } catch (RuntimeException | IOException exception) {
                issues.add(resourceIssueId(item));
            }
        }
        return issues.stream().sorted().toList();
    }

    private static boolean isJsonResource(Map<String, Object> item) {
        return text(item, "path").replace("\\", "/").endsWith(".json");
    }

    private static String resourceIssueId(Map<String, Object> item) {
        return text(item, "kind") + "|" + text(item, "id") + "|" + text(item, "path").replace("\\", "/");
    }

    private static Map<String, List<String>> modelDependenciesById(Map<String, Object> resourceIndex) {
        Map<String, List<String>> dependencies = new LinkedHashMap<>();
        for (Map<String, Object> item : objects(resourceIndex.get("entries"))) {
            String kind = text(item, "kind");
            if (!"blockstates".equals(kind) && !"models".equals(kind)) {
                continue;
            }
            Object data = readJsonResource(item);
            String namespace = namespaceFromId(text(item, "id"));
            ArrayList<String> refs = new ArrayList<>();
            if ("blockstates".equals(kind)) {
                for (String value : extractKeyValues(data, "model")) {
                    refs.add(normalizeModelId(value, namespace));
                }
            } else if (data instanceof Map<?, ?> map && map.get("parent") instanceof String parent) {
                refs.add(normalizeParentModelId(parent, namespace));
            }
            if (!refs.isEmpty()) {
                dependencies.put(text(item, "id"), refs.stream().distinct().sorted().toList());
            }
        }
        return dependencies;
    }

    private static Map<String, List<String>> textureDependenciesById(Map<String, Object> resourceIndex) {
        Map<String, List<String>> dependencies = new LinkedHashMap<>();
        for (Map<String, Object> item : objects(resourceIndex.get("entries"))) {
            if (!"models".equals(text(item, "kind"))) {
                continue;
            }
            Object data = readJsonResource(item);
            if (!(data instanceof Map<?, ?> map) || !(map.get("textures") instanceof Map<?, ?> textures)) {
                continue;
            }
            String namespace = namespaceFromId(text(item, "id"));
            ArrayList<String> refs = new ArrayList<>();
            for (Object value : textures.values()) {
                if (value instanceof String texture && !texture.startsWith("#")) {
                    refs.add(normalizeTextureId(texture, namespace));
                }
            }
            if (!refs.isEmpty()) {
                dependencies.put(text(item, "id"), refs.stream().distinct().sorted().toList());
            }
        }
        return dependencies;
    }

    private static List<String> unresolvedModelReferences(Map<String, Object> resourceIndex) {
        Set<String> mountedModels = Set.copyOf(resourceIdsByKind(resourceIndex).getOrDefault("models", List.of()));
        ArrayList<String> issues = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : modelDependenciesById(resourceIndex).entrySet()) {
            for (String modelId : entry.getValue()) {
                if (!modelId.startsWith("minecraft:") && !mountedModels.contains(modelId)) {
                    issues.add(entry.getKey() + "|" + modelId);
                }
            }
        }
        return issues.stream().sorted().toList();
    }

    private static List<String> unresolvedTextureReferences(Map<String, Object> resourceIndex) {
        Set<String> mountedTextures = Set.copyOf(resourceIdsByKind(resourceIndex).getOrDefault("textures", List.of()));
        ArrayList<String> issues = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : textureDependenciesById(resourceIndex).entrySet()) {
            for (String textureId : entry.getValue()) {
                if (!textureId.startsWith("minecraft:") && !mountedTextures.contains(textureId)) {
                    issues.add(entry.getKey() + "|" + textureId);
                }
            }
        }
        return issues.stream().sorted().toList();
    }

    private static Map<String, List<String>> soundDependenciesById(Map<String, Object> resourceIndex) {
        Map<String, List<String>> dependencies = new LinkedHashMap<>();
        for (Map<String, Object> item : objects(resourceIndex.get("entries"))) {
            if (!"sounds".equals(text(item, "kind"))) {
                continue;
            }
            Object data = readJsonResource(item);
            if (!(data instanceof Map<?, ?> soundsJson)) {
                continue;
            }
            String namespace = namespaceFromId(text(item, "id"));
            for (Map.Entry<?, ?> entry : soundsJson.entrySet()) {
                if (!(entry.getValue() instanceof Map<?, ?> soundEvent)) {
                    continue;
                }
                Object sounds = soundEvent.get("sounds");
                ArrayList<String> refs = new ArrayList<>();
                if (sounds instanceof List<?> list) {
                    for (Object sound : list) {
                        String soundName = soundName(sound);
                        if (!soundName.isBlank()) {
                            refs.add(normalizeSoundAssetId(soundName, namespace));
                        }
                    }
                }
                if (!refs.isEmpty()) {
                    dependencies.put(namespace + ":" + entry.getKey(), refs.stream().distinct().sorted().toList());
                }
            }
        }
        return dependencies;
    }

    private static List<String> unresolvedSoundReferences(Map<String, Object> resourceIndex) {
        Set<String> mountedAssets = Set.copyOf(resourceIdsByKind(resourceIndex).getOrDefault("assets", List.of()));
        ArrayList<String> issues = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : soundDependenciesById(resourceIndex).entrySet()) {
            for (String soundId : entry.getValue()) {
                if (!soundId.startsWith("minecraft:") && !mountedAssets.contains(soundId)) {
                    issues.add(entry.getKey() + "|" + soundId);
                }
            }
        }
        return issues.stream().sorted().toList();
    }

    private static String soundName(Object sound) {
        if (sound instanceof String text) {
            return text;
        }
        if (sound instanceof Map<?, ?> map && map.get("name") instanceof String name) {
            return name;
        }
        return "";
    }

    private static String normalizeSoundAssetId(String value, String namespace) {
        String soundNamespace = namespace;
        String path = value;
        if (value.contains(":")) {
            String[] parts = value.split(":", 2);
            soundNamespace = parts[0];
            path = parts[1];
        }
        if (path.endsWith(".ogg")) {
            path = path.substring(0, path.length() - 4);
        }
        if (!path.startsWith("sounds/")) {
            path = "sounds/" + path;
        }
        return soundNamespace + ":" + path;
    }

    private static Object readJsonResource(Map<String, Object> item) {
        try {
            return EchoNativeJson.parse(Files.readString(Path.of(text(item, "path"))));
        } catch (RuntimeException | IOException ignored) {
            return Map.of();
        }
    }

    private static List<String> extractKeyValues(Object value, String wanted) {
        ArrayList<String> values = new ArrayList<>();
        collectKeyValues(value, wanted, values);
        return values;
    }

    private static void collectKeyValues(Object value, String wanted, List<String> values) {
        if (value instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (wanted.equals(String.valueOf(entry.getKey())) && entry.getValue() instanceof String text) {
                    values.add(text);
                } else {
                    collectKeyValues(entry.getValue(), wanted, values);
                }
            }
        } else if (value instanceof List<?> list) {
            for (Object item : list) {
                collectKeyValues(item, wanted, values);
            }
        }
    }

    private static String normalizeModelId(String value, String namespace) {
        String modelNamespace = namespace;
        String path = value;
        if (value.contains(":")) {
            String[] parts = value.split(":", 2);
            modelNamespace = parts[0];
            path = parts[1];
        }
        if (!path.startsWith("models/")) {
            path = "models/" + path;
        }
        return modelNamespace + ":" + path;
    }

    private static String normalizeParentModelId(String value, String namespace) {
        if (!value.contains(":") && (value.startsWith("item/") || value.startsWith("block/") || value.startsWith("builtin/"))) {
            return normalizeModelId("minecraft:" + value, namespace);
        }
        return normalizeModelId(value, namespace);
    }

    private static String normalizeTextureId(String value, String namespace) {
        String textureNamespace = namespace;
        String path = value;
        if (value.contains(":")) {
            String[] parts = value.split(":", 2);
            textureNamespace = parts[0];
            path = parts[1];
        }
        if (!path.startsWith("textures/")) {
            path = "textures/" + path;
        }
        return textureNamespace + ":" + path;
    }

    private static String namespaceFromId(String id) {
        return id.contains(":") ? id.split(":", 2)[0] : "";
    }

    private static Map<String, String> langKeysByContentId(
            Map<String, Object> contract,
            Map<String, String> translationsByKey
    ) {
        TreeMap<String, String> keys = new TreeMap<>();
        putLangKeys(keys, contract.get("blocks"), "blocks", translationsByKey);
        putLangKeys(keys, contract.get("items"), "items", translationsByKey);
        putLangKeys(keys, contract.get("entities"), "entities", translationsByKey);
        return new LinkedHashMap<>(keys);
    }

    private static void putLangKeys(
            Map<String, String> keys,
            Object values,
            String registry,
            Map<String, String> translationsByKey
    ) {
        for (Map<String, Object> item : objects(values)) {
            String contentId = text(item, "id");
            String langKey = text(item, "langKey");
            if (langKey.isBlank()) {
                langKey = deriveLangKey(registry, contentId, translationsByKey);
            }
            if (!contentId.isBlank() && !langKey.isBlank()) {
                keys.put(contentId, langKey);
            }
        }
    }

    private static String deriveLangKey(String registry, String contentId, Map<String, String> translationsByKey) {
        String[] parts = contentId.split(":", 2);
        if (parts.length != 2) {
            return "";
        }
        String namespace = parts[0];
        String name = parts[1];
        if ("blocks".equals(registry)) {
            return firstExisting(translationsByKey, "block." + namespace + "." + name);
        }
        if ("items".equals(registry)) {
            return firstExisting(translationsByKey, "item." + namespace + "." + name, "block." + namespace + "." + name);
        }
        if ("entities".equals(registry)) {
            String shortName = name.contains("/") ? name.substring(name.lastIndexOf('/') + 1) : name;
            return firstExisting(translationsByKey, "entity." + namespace + "." + name, "spell." + namespace + "." + shortName);
        }
        return "";
    }

    private static String firstExisting(Map<String, String> translationsByKey, String... candidates) {
        for (String candidate : candidates) {
            if (translationsByKey.containsKey(candidate)) {
                return candidate;
            }
        }
        return "";
    }

    private static Map<String, String> langTranslationsByKey(Map<String, Object> resourceIndex) throws IOException {
        TreeMap<String, String> translations = new TreeMap<>();
        for (Map<String, Object> item : objects(resourceIndex.get("entries"))) {
            if (!"lang".equals(text(item, "kind")) || !text(item, "path").endsWith(".json")) {
                continue;
            }
            Map<String, Object> data = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(text(item, "path")))));
            for (Map.Entry<String, Object> entry : data.entrySet()) {
                translations.put(entry.getKey(), String.valueOf(entry.getValue()));
            }
        }
        return new LinkedHashMap<>(translations);
    }

    private static Map<String, String> langValuesByContentId(
            Map<String, String> langKeysByContentId,
            Map<String, String> translationsByKey
    ) {
        LinkedHashMap<String, String> values = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : langKeysByContentId.entrySet()) {
            values.put(entry.getKey(), translationsByKey.getOrDefault(entry.getValue(), ""));
        }
        return values;
    }

    private static Map<String, String> langAssetsByContentId(Map<String, String> langKeysByContentId) {
        Map<String, String> assets = new LinkedHashMap<>();
        for (String contentId : langKeysByContentId.keySet()) {
            int separator = contentId.indexOf(':');
            if (separator > 0) {
                assets.put(contentId, contentId.substring(0, separator) + ":lang/en_us");
            }
        }
        return assets;
    }

    private static List<Map<String, Object>> objects(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        return list.stream().map(EchoNativeJson::asObject).toList();
    }

    private static String text(Map<String, Object> item, String key) {
        Object value = item.get(key);
        if (value instanceof Number number) {
            double doubleValue = number.doubleValue();
            long longValue = number.longValue();
            if (Double.isFinite(doubleValue) && doubleValue == longValue) {
                return Long.toString(longValue);
            }
        }
        return value == null ? "" : String.valueOf(value);
    }

    private static int integer(Object value, int fallback) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    public boolean materializeResourcePack(EchoNativeRegistryContentSnapshot snapshot) {
        return stageResources(snapshot, MATERIALIZED_RESOURCE_PACK_ROOT) > 0;
    }

    public Map<String, List<String>> registerContentDefinitions(EchoNativeRegistryContentSnapshot snapshot) {
        return snapshot.contentIdsByRegistry();
    }

    private static Map<String, String> stagedResourceContentFingerprints(Path root) {
        Path normalizedRoot = root.toAbsolutePath().normalize();
        Map<String, String> fingerprints = new TreeMap<>();
        if (!Files.exists(normalizedRoot)) {
            return fingerprints;
        }
        try (var paths = Files.walk(normalizedRoot)) {
            for (Path path : paths.filter(Files::isRegularFile).sorted().toList()) {
                if (!Files.isRegularFile(path)) {
                    continue;
                }
                String relative = normalizedRoot.relativize(path).toString().replace("\\", "/");
                String fingerprint = fileFingerprintIfPresent(path);
                if (fingerprint != null) {
                    fingerprints.put(relative, fingerprint);
                }
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to fingerprint materialized native resource pack", exception);
        }
        return fingerprints;
    }

    private static String fileFingerprintIfPresent(Path path) {
        try {
            return fileFingerprint(path);
        } catch (IllegalStateException exception) {
            if (exception.getCause() instanceof java.nio.file.NoSuchFileException) {
                return null;
            }
            throw exception;
        }
    }

    private static String fileFingerprint(Path path) {
        try {
            byte[] bytes = Files.readAllBytes(path);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(bytes)) + "|" + bytes.length;
        } catch (IOException | NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Failed to fingerprint staged resource " + path, exception);
        }
    }

    private static Map<String, List<String>> stagedJsonContentFingerprintsById(
            Path root,
            Map<String, List<String>> resourcePathsById
    ) {
        Path normalizedRoot = root.toAbsolutePath().normalize();
        Map<String, List<String>> fingerprints = new TreeMap<>();
        for (Map.Entry<String, List<String>> entry : resourcePathsById.entrySet()) {
            ArrayList<String> values = new ArrayList<>();
            for (String relativePath : entry.getValue()) {
                if (!relativePath.endsWith(".json")) {
                    continue;
                }
                Path path = normalizedRoot.resolve(relativePath).normalize();
                if (!path.startsWith(normalizedRoot) || !Files.isRegularFile(path)) {
                    continue;
                }
                values.add(relativePath + "|" + canonicalJsonFingerprint(path));
            }
            if (!values.isEmpty()) {
                values.sort(String::compareTo);
                fingerprints.put(entry.getKey(), List.copyOf(values));
            }
        }
        return fingerprints;
    }

    private static String canonicalJsonFingerprint(Path path) {
        try {
            String canonical = writeJson(EchoNativeJson.parse(Files.readString(path)));
            byte[] bytes = canonical.getBytes(StandardCharsets.UTF_8);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(bytes)) + "|" + bytes.length;
        } catch (IOException | NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Failed to fingerprint staged JSON resource " + path, exception);
        }
    }

    private static int stageResources(EchoNativeRegistryContentSnapshot snapshot, Path targetRoot) {
        try {
            Path root = targetRoot.toAbsolutePath().normalize();
            clearDirectory(root);
            Files.createDirectories(root);
            Map<Path, List<Path>> sourcesByTarget = new LinkedHashMap<>();
            for (List<String> fingerprints : snapshot.resourceFileFingerprintsByKind().values()) {
                for (String fingerprint : fingerprints) {
                    String[] parts = fingerprint.split("\\|", 4);
                    if (parts.length < 4) {
                        throw new IllegalStateException("Invalid resource fingerprint: " + fingerprint);
                    }
                    Path source = Path.of(parts[3]).toAbsolutePath().normalize();
                    if (!Files.isRegularFile(source)) {
                        throw new IllegalStateException("Audited native resource is missing: " + source);
                    }
                    Path target = root.resolve(resourcePackPath(source)).normalize();
                    if (!target.startsWith(root)) {
                        throw new IllegalStateException("Refusing to materialize outside resource pack root: " + target);
                    }
                    sourcesByTarget.computeIfAbsent(target, ignored -> new ArrayList<>()).add(source);
                }
            }
            for (Map.Entry<Path, List<Path>> entry : sourcesByTarget.entrySet()) {
                Files.createDirectories(entry.getKey().getParent());
                if (entry.getValue().size() == 1) {
                    Files.copy(entry.getValue().get(0), entry.getKey(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
                } else if (isTagPath(entry.getKey())) {
                    writeMergedTag(entry.getValue(), entry.getKey());
                } else {
                    throw new IllegalStateException("Non-tag resource path collision while materializing native resource pack: " + entry.getKey());
                }
            }
            return sourcesByTarget.size();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to materialize native resource pack", exception);
        }
    }

    private static boolean isTagPath(Path target) {
        String normalized = target.toString().replace("\\", "/");
        return normalized.contains("/tags/") && normalized.endsWith(".json");
    }

    private static void writeMergedTag(List<Path> sources, Path target) throws IOException {
        List<Object> values = new ArrayList<>();
        Set<String> seen = new java.util.LinkedHashSet<>();
        for (Path source : sources) {
            Map<String, Object> tag = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(source)));
            if (Boolean.TRUE.equals(tag.get("replace"))) {
                values.clear();
                seen.clear();
            }
            Object rawValues = tag.get("values");
            if (rawValues instanceof List<?> list) {
                for (Object value : list) {
                    String key = String.valueOf(value);
                    if (seen.add(key)) {
                        values.add(value);
                    }
                }
            }
        }
        Map<String, Object> merged = new LinkedHashMap<>();
        merged.put("replace", false);
        merged.put("values", values);
        Files.writeString(target, writeJson(merged), StandardCharsets.UTF_8);
    }

    private static String writeJson(Object value) {
        StringBuilder builder = new StringBuilder();
        appendJson(builder, value);
        builder.append('\n');
        return builder.toString();
    }

    private static void appendJson(StringBuilder builder, Object value) {
        if (value == null) {
            builder.append("null");
        } else if (value instanceof String string) {
            builder.append('"').append(escape(string)).append('"');
        } else if (value instanceof Number || value instanceof Boolean) {
            builder.append(value);
        } else if (value instanceof Map<?, ?> map) {
            builder.append('{');
            int index = 0;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (index++ > 0) {
                    builder.append(',');
                }
                builder.append('"').append(escape(String.valueOf(entry.getKey()))).append("\":");
                appendJson(builder, entry.getValue());
            }
            builder.append('}');
        } else if (value instanceof Iterable<?> iterable) {
            builder.append('[');
            int index = 0;
            for (Object item : iterable) {
                if (index++ > 0) {
                    builder.append(',');
                }
                appendJson(builder, item);
            }
            builder.append(']');
        } else {
            builder.append('"').append(escape(String.valueOf(value))).append('"');
        }
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private static void clearDirectory(Path root) throws IOException {
        if (!Files.exists(root)) {
            return;
        }
        try (var paths = Files.walk(root)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                if (!path.equals(root)) {
                    Files.deleteIfExists(path);
                }
            }
        }
    }

    private static Path resourcePackPath(Path source) {
        String normalized = source.toString().replace("\\", "/");
        String marker = "/src/main/resources/";
        int index = normalized.indexOf(marker);
        if (index >= 0) {
            return Path.of(normalized.substring(index + marker.length()));
        }
        return Path.of(source.getFileName().toString());
    }

    private static Map<String, List<String>> bindByRegistry(
            EchoNativeRegistryContentSnapshot snapshot,
            String registry,
            BindingSource source
    ) {
        Map<String, List<String>> bindings = new LinkedHashMap<>();
        for (EchoNativeRegistryContentDefinition definition : snapshot.definitions()) {
            if (registry.equals(definition.registry())) {
                bindings.put(definition.id(), source.values(definition));
            }
        }
        return bindings;
    }

    private static Map<String, String> kindByRegistry(EchoNativeRegistryContentSnapshot snapshot, String registry) {
        Map<String, String> kinds = new LinkedHashMap<>();
        for (EchoNativeRegistryContentDefinition definition : snapshot.definitions()) {
            if (registry.equals(definition.registry())) {
                kinds.put(definition.id(), definition.kind());
            }
        }
        return kinds;
    }

    private static Map<String, String> mapRendererAssets(EchoNativeRegistryContentSnapshot snapshot) {
        Map<String, String> rendererAssets = new LinkedHashMap<>();
        for (EchoNativeRegistryContentDefinition definition : snapshot.definitions()) {
            if ("blocks".equals(definition.registry())
                    || "items".equals(definition.registry())
                    || "entities".equals(definition.registry())) {
                rendererAssets.put(definition.id(), definition.model());
            }
        }
        return rendererAssets;
    }

    private static Map<String, String> mapBlockstateAssets(EchoNativeRegistryContentSnapshot snapshot) {
        Map<String, String> blockstateAssets = new LinkedHashMap<>();
        for (EchoNativeRegistryContentDefinition definition : snapshot.definitions()) {
            if ("blocks".equals(definition.registry()) && !definition.blockstate().isBlank()) {
                blockstateAssets.put(definition.id(), definition.blockstate());
            }
        }
        return blockstateAssets;
    }

    private static Map<String, String> mapTextureAssets(EchoNativeRegistryContentSnapshot snapshot) {
        Map<String, String> textureAssets = new LinkedHashMap<>();
        for (EchoNativeRegistryContentDefinition definition : snapshot.definitions()) {
            if (("blocks".equals(definition.registry())
                    || "items".equals(definition.registry())
                    || "entities".equals(definition.registry()))
                    && !definition.texture().isBlank()) {
                textureAssets.put(definition.id(), definition.texture());
            }
        }
        return textureAssets;
    }

    private static Map<String, List<String>> contentAssetBindings(EchoNativeRegistryContentSnapshot snapshot) {
        Map<String, List<String>> bindings = new LinkedHashMap<>();
        for (EchoNativeRegistryContentDefinition definition : snapshot.definitions()) {
            if (!"blocks".equals(definition.registry())
                    && !"items".equals(definition.registry())
                    && !"entities".equals(definition.registry())) {
                continue;
            }
            ArrayList<String> assets = new ArrayList<>();
            assets.add("registry=" + definition.registry());
            if (!definition.blockstate().isBlank()) {
                assets.add("blockstate=" + definition.blockstate());
            }
            assets.add("model=" + definition.model());
            assets.add("texture=" + definition.texture());
            assets.add("langKey=" + snapshot.langKeysByContentId().getOrDefault(definition.id(), ""));
            assets.add("langValue=" + snapshot.langValuesByContentId().getOrDefault(definition.id(), ""));
            bindings.put(definition.id(), assets);
        }
        return bindings;
    }

    private static Map<String, List<String>> reverseLookup(Map<String, List<String>> valuesById) {
        Map<String, List<String>> reverse = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> entry : valuesById.entrySet()) {
            for (String value : entry.getValue()) {
                reverse.computeIfAbsent(value, ignored -> new ArrayList<>()).add(entry.getKey());
            }
        }
        Map<String, List<String>> sorted = new LinkedHashMap<>();
        reverse.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> sorted.put(entry.getKey(), entry.getValue().stream().sorted().toList()));
        return sorted;
    }

    private static Map<String, List<String>> buildSoundAssetEventLookup(Map<String, List<String>> soundDependenciesById) {
        return reverseLookup(soundDependenciesById);
    }

    private static Map<String, List<String>> buildDependencyPaths(
            Map<String, List<String>> dependenciesById,
            String defaultExtension
    ) {
        Map<String, List<String>> paths = new TreeMap<>();
        for (Map.Entry<String, List<String>> entry : dependenciesById.entrySet()) {
            List<String> resolved = entry.getValue().stream()
                    .filter(id -> !id.startsWith("minecraft:"))
                    .map(id -> stagedResourceRelativePath(id, defaultExtension))
                    .distinct()
                    .sorted()
                    .toList();
            if (!resolved.isEmpty()) {
                paths.put(entry.getKey(), resolved);
            }
        }
        return new LinkedHashMap<>(paths);
    }

    private static Map<String, String> contentAssetPaths(Map<String, String> assetIdsByContentId, String defaultExtension) {
        Map<String, String> paths = new TreeMap<>();
        for (Map.Entry<String, String> entry : assetIdsByContentId.entrySet()) {
            String assetId = entry.getValue();
            if (!assetId.isBlank() && assetId.indexOf(':') > 0 && !assetId.startsWith("minecraft:")) {
                paths.put(entry.getKey(), stagedResourceRelativePath(assetId, defaultExtension));
            }
        }
        return new LinkedHashMap<>(paths);
    }

    private static String stagedResourceRelativePath(String resourceId, String defaultExtension) {
        int separator = resourceId.indexOf(':');
        if (separator <= 0 || separator + 1 >= resourceId.length()) {
            return resourceId.replace(":", "/");
        }
        String namespace = resourceId.substring(0, separator);
        String valuePath = resourceId.substring(separator + 1);
        String filename = hasExtension(valuePath) ? valuePath : valuePath + defaultExtension;
        return "assets/" + namespace + "/" + filename;
    }

    private static Map<String, String> soundDefinitionDataPathsById(EchoNativeRegistryContentSnapshot snapshot) {
        Map<String, String> paths = new LinkedHashMap<>();
        for (EchoNativeRegistryContentDefinition definition : snapshot.definitions()) {
            if (!"sounds".equals(definition.registry())) {
                continue;
            }
            List<String> sourcePaths = stagedSourcePaths(definition.source());
            if (!sourcePaths.isEmpty()) {
                paths.put(definition.id(), sourcePaths.get(0));
            }
        }
        return paths;
    }

    private static Map<String, String> buildSoundAssetPaths(Map<String, List<String>> soundDependenciesById) {
        Map<String, String> paths = new TreeMap<>();
        for (List<String> assets : soundDependenciesById.values()) {
            for (String assetId : assets) {
                if (!assetId.startsWith("minecraft:")) {
                    paths.put(assetId, stagedSoundRelativePath(assetId));
                }
            }
        }
        return new LinkedHashMap<>(paths);
    }

    private static String stagedSoundRelativePath(String soundId) {
        int separator = soundId.indexOf(':');
        if (separator <= 0 || separator + 1 >= soundId.length()) {
            return soundId.replace(":", "/") + ".ogg";
        }
        String namespace = soundId.substring(0, separator);
        String valuePath = soundId.substring(separator + 1);
        String filename = valuePath.endsWith(".ogg") ? valuePath : valuePath + ".ogg";
        return "assets/" + namespace + "/" + filename;
    }

    private static Map<String, List<String>> buildStructureReferenceLookup(Map<String, List<String>> structuresById) {
        return reverseLookup(structuresById);
    }

    private static Map<String, String> recipeDataPathsById(EchoNativeRegistryContentSnapshot snapshot) {
        Map<String, String> paths = new LinkedHashMap<>();
        for (EchoNativeRegistryContentDefinition definition : snapshot.definitions()) {
            if (!"recipes".equals(definition.registry())) {
                continue;
            }
            List<String> sourcePaths = stagedSourcePaths(definition.source());
            if (!sourcePaths.isEmpty()) {
                paths.put(definition.id(), sourcePaths.get(0));
            }
        }
        return paths;
    }

    private static Map<String, String> lootDataPathsById(EchoNativeRegistryContentSnapshot snapshot) {
        Map<String, String> paths = new LinkedHashMap<>();
        for (EchoNativeRegistryContentDefinition definition : snapshot.definitions()) {
            if (!"lootTables".equals(definition.registry())) {
                continue;
            }
            List<String> sourcePaths = stagedSourcePaths(definition.source());
            if (!sourcePaths.isEmpty()) {
                paths.put(definition.id(), sourcePaths.get(0));
            }
        }
        return paths;
    }

    private static Map<String, String> structureAssetPathsById(EchoNativeRegistryContentSnapshot snapshot) {
        Map<String, String> paths = new LinkedHashMap<>();
        for (EchoNativeRegistryContentDefinition definition : snapshot.definitions()) {
            if (!"structures".equals(definition.registry())) {
                continue;
            }
            List<String> sourcePaths = stagedSourcePaths(definition.source());
            if (!sourcePaths.isEmpty()) {
                paths.put(definition.id(), sourcePaths.get(0));
            }
        }
        return paths;
    }

    private static Map<String, List<String>> buildTagValueLookup(Map<String, List<String>> tagsById) {
        return reverseLookup(tagsById);
    }

    private static Map<String, List<String>> tagDataPathsById(EchoNativeRegistryContentSnapshot snapshot) {
        Map<String, List<String>> paths = new LinkedHashMap<>();
        for (EchoNativeRegistryContentDefinition definition : snapshot.definitions()) {
            if (!"tags".equals(definition.registry())) {
                continue;
            }
            String relativePath = stagedTagRelativePath(definition.id());
            if (!relativePath.isBlank()) {
                paths.put(definition.id(), List.of(relativePath));
            }
        }
        return paths;
    }

    private static String stagedTagRelativePath(String tagId) {
        int separator = tagId.indexOf(':');
        if (separator <= 0 || separator + 1 >= tagId.length()) {
            return "";
        }
        String namespace = tagId.substring(0, separator);
        String valuePath = tagId.substring(separator + 1);
        return "data/" + namespace + "/" + valuePath + ".json";
    }

    private static Map<String, List<String>> buildCreativeGroupMembership(Map<String, List<String>> creativeGroupsById) {
        return reverseLookup(creativeGroupsById);
    }

    private static Map<String, String> terminalPageDataPathsById(EchoNativeRegistryContentSnapshot snapshot) {
        return dataPathsById(snapshot.resourcePathsById(), "terminalPages", snapshot.terminalPages());
    }

    private static Map<String, String> indexEntryDataPathsById(EchoNativeRegistryContentSnapshot snapshot) {
        return dataPathsById(snapshot.resourcePathsById(), "data", snapshot.indexEntries());
    }

    private static Map<String, String> missionJsonDataPathsById(EchoNativeRegistryContentSnapshot snapshot) {
        return dataPathsById(snapshot.resourcePathsById(), "missionJson", resourceIds(snapshot, "missionJson"));
    }

    private static Map<String, String> worldRegionDataPathsById(EchoNativeRegistryContentSnapshot snapshot) {
        return dataPathsById(snapshot.resourcePathsById(), "worldRegions", resourceIds(snapshot, "worldRegions"));
    }

    private static Map<String, String> worldHazardDataPathsById(EchoNativeRegistryContentSnapshot snapshot) {
        return dataPathsById(snapshot.resourcePathsById(), "worldHazards", resourceIds(snapshot, "worldHazards"));
    }

    private static Map<String, String> dataPathsById(
            Map<String, List<String>> resourcePathsById,
            String kind,
            List<String> ids
    ) {
        Map<String, String> paths = new LinkedHashMap<>();
        for (String id : ids) {
            List<String> resourcePaths = resourcePathsById.getOrDefault(kind + "|" + id, List.of());
            if (!resourcePaths.isEmpty()) {
                paths.put(id, resourcePaths.get(0));
            }
        }
        return paths;
    }

    private static Map<String, List<String>> buildContentAssetLookup(Map<String, List<String>> contentAssetBindingsById) {
        Map<String, List<String>> assetsByContentId = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> entry : contentAssetBindingsById.entrySet()) {
            ArrayList<String> assets = new ArrayList<>();
            for (String binding : entry.getValue()) {
                int separator = binding.indexOf('=');
                if (separator <= 0 || separator + 1 >= binding.length()) {
                    continue;
                }
                String key = binding.substring(0, separator);
                String value = binding.substring(separator + 1);
                if (("blockstate".equals(key) || "model".equals(key) || "texture".equals(key)) && !value.isBlank()) {
                    assets.add(value);
                }
            }
            assetsByContentId.put(entry.getKey(), assets);
        }
        return reverseLookup(assetsByContentId);
    }

    private static Map<String, List<String>> resolvedContentAssetPaths(
            EchoNativeRegistryContentSnapshot snapshot,
            Map<String, List<String>> contentAssetBindingsById
    ) {
        Map<String, List<String>> resolved = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> entry : contentAssetBindingsById.entrySet()) {
            Set<String> paths = new java.util.TreeSet<>();
            for (String binding : entry.getValue()) {
                int separator = binding.indexOf('=');
                if (separator <= 0 || separator + 1 >= binding.length()) {
                    continue;
                }
                String key = binding.substring(0, separator);
                String value = binding.substring(separator + 1);
                if (value.isBlank()) {
                    continue;
                }
                if ("blockstate".equals(key)) {
                    addResourcePaths(paths, snapshot.resourcePathsById(), "blockstates", value);
                    for (String modelId : snapshot.modelDependenciesById().getOrDefault(value, List.of())) {
                        addModelClosure(paths, modelId, snapshot, new java.util.HashSet<>());
                    }
                } else if ("model".equals(key)) {
                    addModelClosure(paths, value, snapshot, new java.util.HashSet<>());
                } else if ("texture".equals(key)) {
                    addResourcePaths(paths, snapshot.resourcePathsById(), "textures", value);
                } else if ("langKey".equals(key)) {
                    addLangPath(paths, snapshot.resourcePathsById(), entry.getKey());
                }
            }
            if (!paths.isEmpty()) {
                resolved.put(entry.getKey(), List.copyOf(paths));
            }
        }
        return resolved;
    }

    private static Map<String, List<String>> requiredAssetChecksByContentId(
            Map<String, List<String>> contentAssetBindingsById,
            Map<String, List<String>> resolvedContentAssetPathsById
    ) {
        Map<String, List<String>> checks = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> entry : contentAssetBindingsById.entrySet()) {
            Map<String, String> fields = bindingFields(entry.getValue());
            ArrayList<String> values = new ArrayList<>();
            for (String key : List.of("registry", "blockstate", "model", "texture", "langKey", "langValue")) {
                String value = fields.getOrDefault(key, "");
                if (!value.isBlank()) {
                    values.add(key + "=" + value);
                }
            }
            for (String path : resolvedContentAssetPathsById.getOrDefault(entry.getKey(), List.of())) {
                values.add("resolvedPath=" + path);
            }
            checks.put(entry.getKey(), List.copyOf(values));
        }
        return checks;
    }

    private static Map<String, String> bindingFields(List<String> values) {
        Map<String, String> fields = new LinkedHashMap<>();
        for (String value : values) {
            int separator = value.indexOf('=');
            if (separator > 0 && separator + 1 < value.length()) {
                fields.put(value.substring(0, separator), value.substring(separator + 1));
            }
        }
        return fields;
    }

    private static void addModelClosure(
            Set<String> paths,
            String modelId,
            EchoNativeRegistryContentSnapshot snapshot,
            Set<String> visited
    ) {
        if (!visited.add(modelId)) {
            return;
        }
        addResourcePaths(paths, snapshot.resourcePathsById(), "models", modelId);
        for (String textureId : snapshot.textureDependenciesById().getOrDefault(modelId, List.of())) {
            addResourcePaths(paths, snapshot.resourcePathsById(), "textures", textureId);
        }
        for (String parentModelId : snapshot.modelDependenciesById().getOrDefault(modelId, List.of())) {
            addModelClosure(paths, parentModelId, snapshot, visited);
        }
    }

    private static void addLangPath(
            Set<String> paths,
            Map<String, List<String>> resourcePathsById,
            String contentId
    ) {
        int separator = contentId.indexOf(':');
        if (separator <= 0) {
            return;
        }
        addResourcePaths(paths, resourcePathsById, "lang", contentId.substring(0, separator) + ":lang/en_us");
    }

    private static void addResourcePaths(
            Set<String> paths,
            Map<String, List<String>> resourcePathsById,
            String kind,
            String resourceId
    ) {
        if (resourceId.startsWith("minecraft:")) {
            return;
        }
        paths.addAll(resourcePathsById.getOrDefault(kind + "|" + resourceId, List.of()));
    }

    private static Map<String, List<String>> buildLangLookup(Map<String, String> valuesByContentId) {
        Map<String, List<String>> listValuesByContentId = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : valuesByContentId.entrySet()) {
            if (!entry.getValue().isBlank()) {
                listValuesByContentId.put(entry.getKey(), List.of(entry.getValue()));
            }
        }
        return reverseLookup(listValuesByContentId);
    }

    private static Map<String, List<String>> registrySourcePathsById(EchoNativeRegistryContentSnapshot snapshot) {
        Map<String, List<String>> paths = new LinkedHashMap<>();
        for (EchoNativeRegistryContentDefinition definition : snapshot.definitions()) {
            List<String> sourcePaths = stagedSourcePaths(definition.source());
            if (!sourcePaths.isEmpty()) {
                paths.put(definition.registry() + ":" + definition.id(), sourcePaths);
            }
        }
        return paths;
    }

    private static List<String> stagedSourcePaths(String source) {
        ArrayList<String> paths = new ArrayList<>();
        if (source == null || source.isBlank()) {
            return paths;
        }
        for (String rawSource : source.split(";")) {
            String normalized = rawSource.replace("\\", "/").trim();
            String marker = "/src/main/resources/";
            int index = normalized.indexOf(marker);
            if (index >= 0) {
                paths.add(normalized.substring(index + marker.length()));
            } else if (normalized.startsWith("assets/") || normalized.startsWith("data/")) {
                paths.add(normalized);
            }
        }
        return paths.stream().distinct().sorted().toList();
    }

    private static Map<String, List<String>> contentTagIds(
            EchoNativeRegistryContentSnapshot snapshot,
            Map<String, List<String>> tags
    ) {
        Map<String, List<String>> membership = new LinkedHashMap<>();
        Set<String> registered = registeredGameplayIds(snapshot);
        for (String contentId : registered.stream().sorted().toList()) {
            membership.put(contentId, new ArrayList<>());
        }
        for (Map.Entry<String, List<String>> tag : tags.entrySet()) {
            for (String value : tag.getValue()) {
                if (registered.contains(value)) {
                    membership.get(value).add(tag.getKey());
                }
            }
        }
        Map<String, List<String>> sorted = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> entry : membership.entrySet()) {
            sorted.put(entry.getKey(), entry.getValue().stream().sorted().toList());
        }
        return sorted;
    }

    private static Set<String> registeredGameplayIds(EchoNativeRegistryContentSnapshot snapshot) {
        Set<String> registered = new java.util.TreeSet<>();
        Map<String, List<String>> contentIds = snapshot.contentIdsByRegistry();
        registered.addAll(contentIds.getOrDefault("blocks", List.of()));
        registered.addAll(contentIds.getOrDefault("items", List.of()));
        registered.addAll(contentIds.getOrDefault("entities", List.of()));
        return registered;
    }

    private static Map<String, List<String>> searchIndexTerms(
            EchoNativeRegistryContentSnapshot snapshot,
            Map<String, List<String>> contentTags
    ) {
        Set<String> visible = Set.copyOf(snapshot.searchVisibleContentIds());
        Map<String, List<String>> termsByContent = new LinkedHashMap<>();
        for (String contentId : visible.stream().sorted().toList()) {
            Set<String> terms = new java.util.TreeSet<>();
            addSearchTerms(terms, contentId);
            addSearchTerms(terms, snapshot.langValuesByContentId().getOrDefault(contentId, ""));
            for (String tagId : contentTags.getOrDefault(contentId, List.of())) {
                addSearchTerms(terms, tagId);
            }
            termsByContent.put(contentId, List.copyOf(terms));
        }
        return termsByContent;
    }

    private static void addSearchTerms(Set<String> terms, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        for (String term : value.toLowerCase(java.util.Locale.ROOT).split("[^a-z0-9]+")) {
            if (!term.isBlank()) {
                terms.add(term);
            }
        }
    }

    private static List<String> uiAssets(EchoNativeRegistryContentSnapshot snapshot) {
        ArrayList<String> assets = new ArrayList<>();
        assets.addAll(snapshot.terminalPages());
        assets.addAll(snapshot.indexEntries());
        assets.addAll(resourceIds(snapshot, "uiThemes"));
        return assets.stream().sorted().toList();
    }

    private static Map<String, String> buildUiAssetPaths(List<String> uiAssets) {
        Map<String, String> paths = new LinkedHashMap<>();
        for (String uiAsset : uiAssets) {
            paths.put(uiAsset, stagedUiAssetPath(uiAsset));
        }
        return paths;
    }

    private static String stagedUiAssetPath(String resourceId) {
        int separator = resourceId.indexOf(':');
        if (separator <= 0 || separator + 1 >= resourceId.length()) {
            return resourceId.replace(":", "/");
        }
        String namespace = resourceId.substring(0, separator);
        String valuePath = resourceId.substring(separator + 1);
        String filename = hasExtension(valuePath) ? valuePath : valuePath + ".json";
        String root = valuePath.startsWith("echoindex/entries/") || valuePath.startsWith("echoterminal/pages/")
                ? "data"
                : "assets";
        return root + "/" + namespace + "/" + filename;
    }

    private static List<String> textureForgeSpecIds(EchoNativeRegistryContentSnapshot snapshot) {
        return textureForgeSpecEntries(snapshot).stream().map(TextureForgeSpecEntry::id).toList();
    }

    private static Map<String, String> textureForgeSpecOutputPathsById(EchoNativeRegistryContentSnapshot snapshot) {
        Map<String, String> values = new LinkedHashMap<>();
        for (TextureForgeSpecEntry entry : textureForgeSpecEntries(snapshot)) {
            values.put(entry.id(), entry.outputPath());
        }
        return values;
    }

    private static Map<String, String> textureForgeSpecStatusById(EchoNativeRegistryContentSnapshot snapshot) {
        Map<String, String> values = new LinkedHashMap<>();
        for (TextureForgeSpecEntry entry : textureForgeSpecEntries(snapshot)) {
            values.put(entry.id(), entry.status());
        }
        return values;
    }

    private static Map<String, String> textureForgeSpecStyleFamilyById(EchoNativeRegistryContentSnapshot snapshot) {
        Map<String, String> values = new LinkedHashMap<>();
        for (TextureForgeSpecEntry entry : textureForgeSpecEntries(snapshot)) {
            values.put(entry.id(), entry.styleFamily());
        }
        return values;
    }

    private static Map<String, List<String>> textureForgeSpecPromptFieldsById(EchoNativeRegistryContentSnapshot snapshot) {
        Map<String, List<String>> values = new LinkedHashMap<>();
        for (TextureForgeSpecEntry entry : textureForgeSpecEntries(snapshot)) {
            values.put(entry.id(), entry.promptFields());
        }
        return values;
    }

    private static Map<String, String> textureForgeSpecDataPathsById(EchoNativeRegistryContentSnapshot snapshot) {
        Map<String, String> values = new LinkedHashMap<>();
        for (TextureForgeSpecEntry entry : textureForgeSpecEntries(snapshot)) {
            values.put(entry.id(), entry.dataPath());
        }
        return values;
    }

    private static List<TextureForgeSpecEntry> textureForgeSpecEntries(EchoNativeRegistryContentSnapshot snapshot) {
        Path root = MATERIALIZED_RESOURCE_PACK_ROOT.toAbsolutePath().normalize();
        ArrayList<TextureForgeSpecEntry> specs = new ArrayList<>();
        for (Map.Entry<String, List<String>> resource : snapshot.resourcePathsById().entrySet()) {
            if (!resource.getKey().startsWith("assets|")) {
                continue;
            }
            for (String relativePath : resource.getValue()) {
                String normalized = relativePath.replace("\\", "/");
                if (!normalized.endsWith(".json") || !normalized.contains("/textureforge/specs/")) {
                    continue;
                }
                Path path = root.resolve(normalized).normalize();
                if (!path.startsWith(root) || !Files.isRegularFile(path)) {
                    continue;
                }
                Map<String, Object> data = EchoNativeJson.asObject(readJsonPath(path));
                String namespace = namespaceFromAssetPath(normalized);
                String styleFamily = text(data, "styleFamily");
                String defaultResolution = text(data, "defaultResolution");
                for (Map<String, Object> asset : objects(data.get("assets"))) {
                    String rawId = text(asset, "id");
                    if (rawId.isBlank()) {
                        continue;
                    }
                    String specId = rawId.contains(":") ? rawId : namespace + ":" + rawId;
                    specs.add(new TextureForgeSpecEntry(
                            specId,
                            textureForgeOutputPath(namespace, text(asset, "outputPath")),
                            text(asset, "status"),
                            styleFamily,
                            textureForgePromptFields(asset, defaultResolution),
                            normalized
                    ));
                }
            }
        }
        specs.sort(java.util.Comparator.comparing(TextureForgeSpecEntry::id));
        return List.copyOf(specs);
    }

    private static String namespaceFromAssetPath(String relativePath) {
        String normalized = relativePath.replace("\\", "/");
        if (!normalized.startsWith("assets/")) {
            return "";
        }
        String remainder = normalized.substring("assets/".length());
        int separator = remainder.indexOf('/');
        return separator > 0 ? remainder.substring(0, separator) : "";
    }

    private static String textureForgeOutputPath(String namespace, String outputPath) {
        if (outputPath.isBlank()) {
            return "";
        }
        String normalized = outputPath.replace("\\", "/");
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        if (normalized.startsWith("assets/")) {
            return normalized;
        }
        return "assets/" + namespace + "/" + normalized;
    }

    private static List<String> textureForgePromptFields(Map<String, Object> asset, String defaultResolution) {
        ArrayList<String> fields = new ArrayList<>();
        addPromptField(fields, "defaultResolution", defaultResolution);
        addPromptField(fields, "kind", text(asset, "kind"));
        addPromptField(fields, "textureType", text(asset, "textureType"));
        addPromptField(fields, "promptPriority", text(asset, "promptPriority"));
        addPromptField(fields, "sheetGroup", text(asset, "sheetGroup"));
        addPromptField(fields, "notes", text(asset, "notes"));
        addPromptField(fields, "silhouetteNotes", text(asset, "silhouetteNotes"));
        addPromptField(fields, "minecraftReadabilityNotes", text(asset, "minecraftReadabilityNotes"));
        addPromptList(fields, "colorPaletteHints", asset.get("colorPaletteHints"));
        addPromptList(fields, "mustHave", asset.get("mustHave"));
        addPromptList(fields, "avoid", asset.get("avoid"));
        fields.sort(String::compareTo);
        return List.copyOf(fields);
    }

    private static void addPromptField(List<String> fields, String key, String value) {
        if (value != null && !value.isBlank()) {
            fields.add(key + "=" + value);
        }
    }

    private static void addPromptList(List<String> fields, String key, Object value) {
        List<String> values = EchoNativeJson.stringList(value);
        if (!values.isEmpty()) {
            fields.add(key + "=" + String.join(";", values));
        }
    }

    private static boolean hasExtension(String valuePath) {
        int slash = valuePath.lastIndexOf('/');
        int dot = valuePath.lastIndexOf('.');
        return dot > slash;
    }

    private static Map<String, List<String>> itemLinks(
            EchoNativeRegistryContentSnapshot snapshot,
            BindingSource source
    ) {
        Map<String, List<String>> links = new LinkedHashMap<>();
        for (EchoNativeRegistryContentDefinition definition : snapshot.definitions()) {
            if ("items".equals(definition.registry())) {
                links.put(definition.id(), source.values(definition));
            }
        }
        return links;
    }

    private static Map<String, List<String>> catalogReferencesByKind(
            EchoNativeRegistryContentSnapshot snapshot,
            String kind
    ) {
        Map<String, List<String>> references = new LinkedHashMap<>();
        for (String fingerprint : snapshot.resourceFileFingerprintsByKind().getOrDefault(kind, List.of())) {
            String[] parts = fingerprint.split("\\|", 4);
            if (parts.length < 4 || !parts[3].replace("\\", "/").endsWith(".json")) {
                continue;
            }
            String id = parts[0];
            if ("data".equals(kind) && !parts[3].replace("\\", "/").contains("/echoindex/entries/")) {
                continue;
            }
            references.put(id, extractIds(readJsonPath(Path.of(parts[3]))));
        }
        return references;
    }

    private static Map<String, List<String>> catalogJsonContentFingerprintsByKind(
            Path root,
            Map<String, List<String>> resourcePathsById
    ) {
        Map<String, List<String>> catalogs = new LinkedHashMap<>();
        addCatalogJsonFingerprints(catalogs, root, resourcePathsById, "terminalPages", "terminalPages", null);
        addCatalogJsonFingerprints(catalogs, root, resourcePathsById, "indexEntries", "data", "/echoindex/entries/");
        addCatalogJsonFingerprints(catalogs, root, resourcePathsById, "missionJson", "missionJson", null);
        addCatalogJsonFingerprints(catalogs, root, resourcePathsById, "worldRegions", "worldRegions", null);
        addCatalogJsonFingerprints(catalogs, root, resourcePathsById, "worldHazards", "worldHazards", null);
        addCatalogJsonFingerprints(catalogs, root, resourcePathsById, "uiThemes", "uiThemes", null);
        return catalogs;
    }

    private static Map<String, List<String>> registryJsonContentFingerprintsByKind(
            Path root,
            Map<String, List<String>> resourcePathsById
    ) {
        Map<String, List<String>> registries = new LinkedHashMap<>();
        addRegistryJsonFingerprints(registries, root, resourcePathsById, "recipes", "recipes");
        addRegistryJsonFingerprints(registries, root, resourcePathsById, "lootTables", "lootTables");
        addRegistryJsonFingerprints(registries, root, resourcePathsById, "tags", "tags");
        addRegistryJsonFingerprints(registries, root, resourcePathsById, "structures", "structures");
        addRegistryJsonFingerprints(registries, root, resourcePathsById, "sounds", "sounds");
        return registries;
    }

    private static void addRegistryJsonFingerprints(
            Map<String, List<String>> registries,
            Path root,
            Map<String, List<String>> resourcePathsById,
            String registryKind,
            String resourceKind
    ) {
        addCatalogJsonFingerprints(registries, root, resourcePathsById, registryKind, resourceKind, null);
    }

    private static String resourceManifestFingerprint(Map<String, List<String>> resourceFileFingerprintsByKind) {
        ArrayList<String> entries = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : resourceFileFingerprintsByKind.entrySet()) {
            for (String value : entry.getValue()) {
                entries.add(entry.getKey() + "|" + value);
            }
        }
        entries.sort(String::compareTo);
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (String entry : entries) {
                digest.update(entry.getBytes(StandardCharsets.UTF_8));
                digest.update((byte) '\n');
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 not available", exception);
        }
    }

    private static void addCatalogJsonFingerprints(
            Map<String, List<String>> catalogs,
            Path root,
            Map<String, List<String>> resourcePathsById,
            String catalogKind,
            String resourceKind,
            String requiredPathMarker
    ) {
        Path normalizedRoot = root.toAbsolutePath().normalize();
        ArrayList<String> values = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : resourcePathsById.entrySet()) {
            String prefix = resourceKind + "|";
            if (!entry.getKey().startsWith(prefix)) {
                continue;
            }
            String id = entry.getKey().substring(prefix.length());
            for (String relativePath : entry.getValue()) {
                if (!relativePath.endsWith(".json")
                        || (requiredPathMarker != null && !relativePath.contains(requiredPathMarker))) {
                    continue;
                }
                Path path = normalizedRoot.resolve(relativePath).normalize();
                if (!path.startsWith(normalizedRoot) || !Files.isRegularFile(path)) {
                    continue;
                }
                values.add(id + "|" + relativePath + "|" + canonicalJsonFingerprint(path));
            }
        }
        if (!values.isEmpty()) {
            values.sort(String::compareTo);
            catalogs.put(catalogKind, List.copyOf(values));
        }
    }

    private static Object readJsonPath(Path path) {
        try {
            return EchoNativeJson.parse(Files.readString(path));
        } catch (RuntimeException | IOException ignored) {
            return Map.of();
        }
    }

    private static List<String> extractIds(Object value) {
        Set<String> ids = new java.util.TreeSet<>();
        collectIds(value, ids);
        return List.copyOf(ids);
    }

    private static void collectIds(Object value, Set<String> ids) {
        if (value instanceof String string) {
            if (string.contains(":") || string.startsWith("#")) {
                ids.add(string);
            }
        } else if (value instanceof Map<?, ?> map) {
            for (Object item : map.values()) {
                collectIds(item, ids);
            }
        } else if (value instanceof List<?> list) {
            for (Object item : list) {
                collectIds(item, ids);
            }
        }
    }

    private record TextureForgeSpecEntry(
            String id,
            String outputPath,
            String status,
            String styleFamily,
            List<String> promptFields,
            String dataPath
    ) {
    }

    private enum BindingSource {
        INPUTS {
            @Override
            List<String> values(EchoNativeRegistryContentDefinition definition) {
                return definition.inputs();
            }
        },
        OUTPUTS {
            @Override
            List<String> values(EchoNativeRegistryContentDefinition definition) {
                return definition.outputs();
            }
        },
        ENTRIES {
            @Override
            List<String> values(EchoNativeRegistryContentDefinition definition) {
                return definition.entries();
            }
        };

        abstract List<String> values(EchoNativeRegistryContentDefinition definition);
    }
}
