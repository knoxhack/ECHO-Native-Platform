package dev.echo.nativeplatform.contracts;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@EchoNativeApiStatus(value = EchoNativeApiStability.INTERNAL, since = "0.1.0-native-beta")
public record EchoNativeRegistryRuntimeResolution(
        String runtimeId,
        Map<String, List<String>> contentIdsByRegistry,
        Map<String, List<String>> recipesById,
        Map<String, List<String>> recipeInputsById,
        Map<String, List<String>> recipeIdsByInputId,
        Map<String, List<String>> recipeIdsByOutputId,
        Map<String, String> recipeTypesById,
        Map<String, String> recipeDataPathsById,
        Map<String, List<String>> lootById,
        Map<String, String> lootDataPathsById,
        Map<String, List<String>> lootTableIdsByEntryId,
        Map<String, List<String>> soundsById,
        Map<String, String> soundDefinitionDataPathsById,
        Map<String, List<String>> structuresById,
        Map<String, String> structureTypesById,
        Map<String, List<String>> structureIdsByReferenceId,
        Map<String, String> structureAssetPathsById,
        Map<String, List<String>> tagsById,
        Map<String, List<String>> tagDataPathsById,
        Map<String, List<String>> tagIdsByValueId,
        Map<String, List<String>> contentTagIdsByContentId,
        Map<String, List<String>> creativeGroupsById,
        Map<String, List<String>> creativeGroupIdsByContentId,
        Map<String, List<String>> resourceIdsByKind,
        Map<String, List<String>> resourcePathsById,
        Map<String, List<String>> resourceIdsByNamespace,
        Map<String, String> resourceContentFingerprintsByPath,
        Map<String, List<String>> resourceFileFingerprintsByKind,
        String resourceManifestFingerprint,
        Map<String, String> langKeysByContentId,
        Map<String, String> langValuesByContentId,
        Map<String, String> langAssetsByContentId,
        Map<String, List<String>> contentIdsByLangKey,
        Map<String, List<String>> contentIdsByLangValue,
        Map<String, List<String>> jsonResourceIdsByKind,
        Map<String, List<String>> dataDefinitionIdsByKind,
        Map<String, List<String>> dataDefinitionPathsById,
        Map<String, List<String>> jsonContentFingerprintsById,
        Map<String, List<String>> registrySourcePathsById,
        List<String> jsonParseIssues,
        Map<String, List<String>> modelDependenciesById,
        Map<String, List<String>> textureDependenciesById,
        Map<String, List<String>> modelDependencyPathsById,
        Map<String, List<String>> textureDependencyPathsById,
        Map<String, List<String>> soundDependenciesById,
        Map<String, List<String>> soundEventIdsByAssetId,
        Map<String, String> soundAssetPathsById,
        List<String> unresolvedModelReferences,
        List<String> unresolvedTextureReferences,
        List<String> unresolvedSoundReferences,
        List<String> terminalPages,
        List<String> indexEntries,
        Map<String, String> terminalPageDataPathsById,
        Map<String, String> indexEntryDataPathsById,
        List<String> missionJsonIds,
        List<String> worldRegionIds,
        List<String> worldHazardIds,
        Map<String, String> missionJsonDataPathsById,
        Map<String, String> worldRegionDataPathsById,
        Map<String, String> worldHazardDataPathsById,
        List<String> uiThemeIds,
        List<String> requiredAssetIssues,
        boolean resourcePackMaterialized,
        int materializedResourceCount,
        int assetIndexEntryCount,
        int dataDefinitionEntryCount,
        Map<String, List<String>> duplicateResourceIdsByKind,
        List<String> mergeableTagOverlayIds,
        List<String> blockingDuplicateResourceIds,
        int registeredContentCount,
        List<String> searchVisibleContentIds,
        int searchVisibleContentCount,
        Map<String, List<String>> searchIndexTermsByContentId,
        Map<String, List<String>> searchContentIdsByTerm,
        Map<String, List<String>> contentAssetBindingsById,
        Map<String, List<String>> resolvedContentAssetPathsById,
        Map<String, List<String>> requiredAssetChecksByContentId,
        Map<String, List<String>> contentIdsByAssetId,
        Map<String, String> blockstateAssetsByContentId,
        Map<String, String> rendererAssetsByContentId,
        Map<String, String> textureAssetsByContentId,
        Map<String, String> blockstateAssetPathsByContentId,
        Map<String, String> rendererAssetPathsByContentId,
        Map<String, String> textureAssetPathsByContentId,
        Map<String, String> langAssetPathsByContentId,
        List<String> uiAssets,
        Map<String, String> uiAssetPathsById,
        List<String> textureForgeSpecIds,
        Map<String, String> textureForgeSpecOutputPathsById,
        Map<String, String> textureForgeSpecStatusById,
        Map<String, String> textureForgeSpecStyleFamilyById,
        Map<String, List<String>> textureForgeSpecPromptFieldsById,
        Map<String, String> textureForgeSpecDataPathsById,
        Map<String, Integer> tagMergedSourceCountsById,
        int mergedTagOverlayCount,
        Map<String, List<String>> itemRecipeIdsByContentId,
        Map<String, List<String>> itemLootTableIdsByContentId,
        Map<String, List<String>> registryJsonContentFingerprintsByKind,
        Map<String, List<String>> catalogJsonContentFingerprintsByKind,
        Map<String, List<String>> terminalPageReferencesById,
        Map<String, List<String>> indexEntryReferencesById,
        Map<String, List<String>> missionJsonReferencesById,
        Map<String, List<String>> worldRegionReferencesById,
        Map<String, List<String>> worldHazardReferencesById
) {
    public EchoNativeRegistryRuntimeResolution {
        runtimeId = requireText(runtimeId, "runtimeId");
        contentIdsByRegistry = copyMap(contentIdsByRegistry);
        recipesById = copyMap(recipesById);
        recipeInputsById = copyMap(recipeInputsById);
        recipeIdsByInputId = copyMap(recipeIdsByInputId);
        recipeIdsByOutputId = copyMap(recipeIdsByOutputId);
        recipeTypesById = Map.copyOf(Objects.requireNonNull(recipeTypesById, "recipeTypesById"));
        recipeDataPathsById = Map.copyOf(Objects.requireNonNull(recipeDataPathsById, "recipeDataPathsById"));
        lootById = copyMap(lootById);
        lootDataPathsById = Map.copyOf(Objects.requireNonNull(lootDataPathsById, "lootDataPathsById"));
        lootTableIdsByEntryId = copyMap(lootTableIdsByEntryId);
        soundsById = copyMap(soundsById);
        soundDefinitionDataPathsById = Map.copyOf(Objects.requireNonNull(soundDefinitionDataPathsById, "soundDefinitionDataPathsById"));
        structuresById = copyMap(structuresById);
        structureTypesById = Map.copyOf(Objects.requireNonNull(structureTypesById, "structureTypesById"));
        structureIdsByReferenceId = copyMap(structureIdsByReferenceId);
        structureAssetPathsById = Map.copyOf(Objects.requireNonNull(structureAssetPathsById, "structureAssetPathsById"));
        tagsById = copyMap(tagsById);
        tagDataPathsById = copyMap(tagDataPathsById);
        tagIdsByValueId = copyMap(tagIdsByValueId);
        contentTagIdsByContentId = copyMap(contentTagIdsByContentId);
        creativeGroupsById = copyMap(creativeGroupsById);
        creativeGroupIdsByContentId = copyMap(creativeGroupIdsByContentId);
        resourceIdsByKind = copyMap(resourceIdsByKind);
        resourcePathsById = copyMap(resourcePathsById);
        resourceIdsByNamespace = copyMap(resourceIdsByNamespace);
        resourceContentFingerprintsByPath = Map.copyOf(Objects.requireNonNull(resourceContentFingerprintsByPath, "resourceContentFingerprintsByPath"));
        resourceFileFingerprintsByKind = copyMap(resourceFileFingerprintsByKind);
        resourceManifestFingerprint = requireText(resourceManifestFingerprint, "resourceManifestFingerprint");
        langKeysByContentId = Map.copyOf(Objects.requireNonNull(langKeysByContentId, "langKeysByContentId"));
        langValuesByContentId = Map.copyOf(Objects.requireNonNull(langValuesByContentId, "langValuesByContentId"));
        langAssetsByContentId = Map.copyOf(Objects.requireNonNull(langAssetsByContentId, "langAssetsByContentId"));
        contentIdsByLangKey = copyMap(contentIdsByLangKey);
        contentIdsByLangValue = copyMap(contentIdsByLangValue);
        jsonResourceIdsByKind = copyMap(jsonResourceIdsByKind);
        dataDefinitionIdsByKind = copyMap(dataDefinitionIdsByKind);
        dataDefinitionPathsById = copyMap(dataDefinitionPathsById);
        jsonContentFingerprintsById = copyMap(jsonContentFingerprintsById);
        registrySourcePathsById = copyMap(registrySourcePathsById);
        jsonParseIssues = List.copyOf(Objects.requireNonNull(jsonParseIssues, "jsonParseIssues"));
        modelDependenciesById = copyMap(modelDependenciesById);
        textureDependenciesById = copyMap(textureDependenciesById);
        modelDependencyPathsById = copyMap(modelDependencyPathsById);
        textureDependencyPathsById = copyMap(textureDependencyPathsById);
        soundDependenciesById = copyMap(soundDependenciesById);
        soundEventIdsByAssetId = copyMap(soundEventIdsByAssetId);
        soundAssetPathsById = Map.copyOf(Objects.requireNonNull(soundAssetPathsById, "soundAssetPathsById"));
        unresolvedModelReferences = List.copyOf(Objects.requireNonNull(unresolvedModelReferences, "unresolvedModelReferences"));
        unresolvedTextureReferences = List.copyOf(Objects.requireNonNull(unresolvedTextureReferences, "unresolvedTextureReferences"));
        unresolvedSoundReferences = List.copyOf(Objects.requireNonNull(unresolvedSoundReferences, "unresolvedSoundReferences"));
        duplicateResourceIdsByKind = copyMap(duplicateResourceIdsByKind);
        mergeableTagOverlayIds = List.copyOf(Objects.requireNonNull(mergeableTagOverlayIds, "mergeableTagOverlayIds"));
        blockingDuplicateResourceIds = List.copyOf(Objects.requireNonNull(blockingDuplicateResourceIds, "blockingDuplicateResourceIds"));
        terminalPages = List.copyOf(Objects.requireNonNull(terminalPages, "terminalPages"));
        indexEntries = List.copyOf(Objects.requireNonNull(indexEntries, "indexEntries"));
        terminalPageDataPathsById = Map.copyOf(Objects.requireNonNull(terminalPageDataPathsById, "terminalPageDataPathsById"));
        indexEntryDataPathsById = Map.copyOf(Objects.requireNonNull(indexEntryDataPathsById, "indexEntryDataPathsById"));
        missionJsonIds = List.copyOf(Objects.requireNonNull(missionJsonIds, "missionJsonIds"));
        worldRegionIds = List.copyOf(Objects.requireNonNull(worldRegionIds, "worldRegionIds"));
        worldHazardIds = List.copyOf(Objects.requireNonNull(worldHazardIds, "worldHazardIds"));
        missionJsonDataPathsById = Map.copyOf(Objects.requireNonNull(missionJsonDataPathsById, "missionJsonDataPathsById"));
        worldRegionDataPathsById = Map.copyOf(Objects.requireNonNull(worldRegionDataPathsById, "worldRegionDataPathsById"));
        worldHazardDataPathsById = Map.copyOf(Objects.requireNonNull(worldHazardDataPathsById, "worldHazardDataPathsById"));
        uiThemeIds = List.copyOf(Objects.requireNonNull(uiThemeIds, "uiThemeIds"));
        requiredAssetIssues = List.copyOf(Objects.requireNonNull(requiredAssetIssues, "requiredAssetIssues"));
        searchVisibleContentIds = List.copyOf(Objects.requireNonNull(searchVisibleContentIds, "searchVisibleContentIds"));
        searchIndexTermsByContentId = copyMap(searchIndexTermsByContentId);
        searchContentIdsByTerm = copyMap(searchContentIdsByTerm);
        contentAssetBindingsById = copyMap(contentAssetBindingsById);
        resolvedContentAssetPathsById = copyMap(resolvedContentAssetPathsById);
        requiredAssetChecksByContentId = copyMap(requiredAssetChecksByContentId);
        contentIdsByAssetId = copyMap(contentIdsByAssetId);
        blockstateAssetsByContentId = Map.copyOf(Objects.requireNonNull(blockstateAssetsByContentId, "blockstateAssetsByContentId"));
        rendererAssetsByContentId = Map.copyOf(Objects.requireNonNull(rendererAssetsByContentId, "rendererAssetsByContentId"));
        textureAssetsByContentId = Map.copyOf(Objects.requireNonNull(textureAssetsByContentId, "textureAssetsByContentId"));
        blockstateAssetPathsByContentId = Map.copyOf(Objects.requireNonNull(blockstateAssetPathsByContentId, "blockstateAssetPathsByContentId"));
        rendererAssetPathsByContentId = Map.copyOf(Objects.requireNonNull(rendererAssetPathsByContentId, "rendererAssetPathsByContentId"));
        textureAssetPathsByContentId = Map.copyOf(Objects.requireNonNull(textureAssetPathsByContentId, "textureAssetPathsByContentId"));
        langAssetPathsByContentId = Map.copyOf(Objects.requireNonNull(langAssetPathsByContentId, "langAssetPathsByContentId"));
        uiAssets = List.copyOf(Objects.requireNonNull(uiAssets, "uiAssets"));
        uiAssetPathsById = Map.copyOf(Objects.requireNonNull(uiAssetPathsById, "uiAssetPathsById"));
        textureForgeSpecIds = List.copyOf(Objects.requireNonNull(textureForgeSpecIds, "textureForgeSpecIds"));
        textureForgeSpecOutputPathsById = Map.copyOf(Objects.requireNonNull(textureForgeSpecOutputPathsById, "textureForgeSpecOutputPathsById"));
        textureForgeSpecStatusById = Map.copyOf(Objects.requireNonNull(textureForgeSpecStatusById, "textureForgeSpecStatusById"));
        textureForgeSpecStyleFamilyById = Map.copyOf(Objects.requireNonNull(textureForgeSpecStyleFamilyById, "textureForgeSpecStyleFamilyById"));
        textureForgeSpecPromptFieldsById = copyMap(textureForgeSpecPromptFieldsById);
        textureForgeSpecDataPathsById = Map.copyOf(Objects.requireNonNull(textureForgeSpecDataPathsById, "textureForgeSpecDataPathsById"));
        tagMergedSourceCountsById = Map.copyOf(Objects.requireNonNull(tagMergedSourceCountsById, "tagMergedSourceCountsById"));
        itemRecipeIdsByContentId = copyMap(itemRecipeIdsByContentId);
        itemLootTableIdsByContentId = copyMap(itemLootTableIdsByContentId);
        registryJsonContentFingerprintsByKind = copyMap(registryJsonContentFingerprintsByKind);
        catalogJsonContentFingerprintsByKind = copyMap(catalogJsonContentFingerprintsByKind);
        terminalPageReferencesById = copyMap(terminalPageReferencesById);
        indexEntryReferencesById = copyMap(indexEntryReferencesById);
        missionJsonReferencesById = copyMap(missionJsonReferencesById);
        worldRegionReferencesById = copyMap(worldRegionReferencesById);
        worldHazardReferencesById = copyMap(worldHazardReferencesById);
    }

    private static Map<String, List<String>> copyMap(Map<String, List<String>> values) {
        Objects.requireNonNull(values, "values");
        Map<String, List<String>> copy = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> entry : values.entrySet()) {
            copy.put(requireText(entry.getKey(), "map key"), List.copyOf(entry.getValue()));
        }
        return copy;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
