package dev.echo.nativeplatform.loader;

import dev.echo.nativeplatform.contracts.EchoNativeRegistryContentDefinition;
import dev.echo.nativeplatform.contracts.EchoNativeRegistryContentSnapshot;
import dev.echo.nativeplatform.contracts.EchoNativeRegistryRuntimeResolution;
import dev.echo.nativeplatform.diagnostics.EchoNativeJson;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

public final class EchoNativeAgent4RegistrySmokeMain {
    private static final List<String> REQUIRED_RESOURCE_KINDS = List.of(
            "assets",
            "data",
            "models",
            "blockstates",
            "textures",
            "lang",
            "recipes",
            "lootTables",
            "tags",
            "structures",
            "worldRegions",
            "worldHazards",
            "sounds",
            "uiThemes",
            "terminalPages",
            "missionJson"
    );
    private static final List<String> DATA_DEFINITION_KINDS = List.of(
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

    private EchoNativeAgent4RegistrySmokeMain() {
    }

    public static void main(String[] args) throws Exception {
        EchoNativeRegistryContentBackend backend = new EchoNativeRegistryContentBackend();
        EchoNativeRegistryContentSnapshot snapshot = loadSnapshot(args, backend);

        EchoNativeRegistryRuntimeResolution resolution = backend.resolve(snapshot);
        if (!resolution.requiredAssetIssues().isEmpty()) {
            throw new IllegalStateException("Native registry content has missing assets: " + resolution.requiredAssetIssues());
        }
        if (!resolution.jsonParseIssues().isEmpty()) {
            throw new IllegalStateException("Native registry content has invalid JSON resources: " + resolution.jsonParseIssues());
        }
        if (!resolution.unresolvedModelReferences().isEmpty()
                || !resolution.unresolvedTextureReferences().isEmpty()
                || !resolution.unresolvedSoundReferences().isEmpty()) {
            throw new IllegalStateException("Native registry content has unresolved render references: models="
                    + resolution.unresolvedModelReferences()
                    + " textures=" + resolution.unresolvedTextureReferences()
                    + " sounds=" + resolution.unresolvedSoundReferences());
        }
        if (!resolution.resourcePackMaterialized()) {
            throw new IllegalStateException("Native registry content did not materialize a resource pack");
        }
        if (resolution.materializedResourceCount() <= 0
                || resolution.resourceFileFingerprintsByKind().isEmpty()) {
            throw new IllegalStateException("Native registry content did not materialize audited resource files");
        }
        assertMaterializedResourceFiles(resolution);
        assertResourceKindIndex(resolution);
        assertResourceFileFingerprints(resolution);
        assertResourceManifestBinding(resolution);
        assertResourcePaths(resolution);
        assertResourceNamespaceIndex(resolution);
        assertResourceContentFingerprints(resolution);
        assertMaterializedJsonFiles(resolution);
        assertDataDefinitionIndex(resolution);
        assertJsonContentFingerprints(resolution);
        assertRegistrySourcePaths(snapshot, resolution);
        assertDataDefinitionPaths(resolution);
        assertMaterializedRuntimeAssetReferences(resolution);
        assertBlockstateAssetBindings(resolution);
        assertRendererAssetBindings(resolution);
        assertTextureAssetBindings(resolution);
        assertTypedContentAssetPaths(resolution);
        assertRenderDependencyBindings(resolution);
        assertRenderDependencyPaths(resolution);
        assertContentAssetBindings(resolution);
        assertResolvedContentAssetPaths(resolution);
        assertRequiredAssetChecks(resolution);
        assertLangContentLookup(resolution);
        assertLangAssetBindings(resolution);
        assertStagedRecipeLootBindings(snapshot, resolution);
        assertRecipeDataPaths(resolution);
        assertLootDataPaths(resolution);
        assertRecipeLootLookupIndexes(resolution);
        assertItemRecipeLootRuntimeLinks(resolution);
        assertRegistryJsonContentFingerprints(resolution);
        assertStagedSoundBindings(resolution);
        assertSoundDefinitionDataPaths(resolution);
        assertSoundDependencyBindings(resolution);
        assertSoundAssetPaths(resolution);
        assertSoundAssetEventLookup(resolution);
        assertStagedStructureBindings(snapshot, resolution);
        assertStructureAssetPaths(resolution);
        assertStructureReferenceLookup(resolution);
        assertCatalogJsonContentFingerprints(resolution);
        assertStagedCatalogBindings(resolution);
        assertTerminalIndexDataPaths(resolution);
        assertMissionWorldDataPaths(resolution);
        assertRequiredResourceKinds(resolution.resourceIdsByKind(), "Native");
        assertRegisteredGameplayContent(resolution);
        assertTagDataPaths(resolution);
        assertTagValueLookup(resolution);
        assertContentTagMembership(resolution);
        assertSearchAndCreativeRuntimeViews(resolution);
        assertDuplicateAndTagOverlayState(resolution);
        if (resolution.assetIndexEntryCount() <= 0
                || resolution.dataDefinitionEntryCount() <= 0
                || resolution.jsonResourceIdsByKind().isEmpty()
                || resolution.dataDefinitionIdsByKind().isEmpty()
                || resolution.jsonContentFingerprintsById().isEmpty()
                || resolution.resourcePathsById().isEmpty()
                || resolution.resourceIdsByNamespace().isEmpty()
                || resolution.resourceContentFingerprintsByPath().isEmpty()
                || resolution.registrySourcePathsById().isEmpty()) {
            throw new IllegalStateException("Native registry content did not load the audited asset index and data JSON");
        }
        if (resolution.langKeysByContentId().isEmpty()
                || resolution.langValuesByContentId().isEmpty()
                || resolution.langAssetsByContentId().isEmpty()
                || resolution.contentIdsByLangKey().isEmpty()
                || resolution.contentIdsByLangValue().isEmpty()
                || resolution.contentAssetBindingsById().isEmpty()
                || resolution.resolvedContentAssetPathsById().isEmpty()
                || resolution.requiredAssetChecksByContentId().isEmpty()
                || resolution.contentIdsByAssetId().isEmpty()
                || resolution.rendererAssetsByContentId().isEmpty()
                || resolution.uiAssets().isEmpty()
                || resolution.uiAssetPathsById().isEmpty()) {
            throw new IllegalStateException("Native registry content did not resolve lang, renderer assets, and UI assets");
        }
        if (resolution.recipesById().isEmpty()
                || resolution.recipeInputsById().isEmpty()
                || resolution.recipeIdsByInputId().isEmpty()
                || resolution.recipeIdsByOutputId().isEmpty()
                || resolution.recipeTypesById().isEmpty()
                || resolution.recipeDataPathsById().isEmpty()
                || resolution.lootById().isEmpty()
                || resolution.lootDataPathsById().isEmpty()
                || resolution.lootTableIdsByEntryId().isEmpty()
                || resolution.soundsById().isEmpty()
                || resolution.soundDefinitionDataPathsById().isEmpty()
                || resolution.soundAssetPathsById().isEmpty()
                || resolution.soundEventIdsByAssetId().isEmpty()
                || resolution.structuresById().isEmpty()
                || resolution.structureAssetPathsById().isEmpty()
                || resolution.structureTypesById().isEmpty()
                || resolution.structureIdsByReferenceId().isEmpty()
                || resolution.tagsById().isEmpty()
                || resolution.tagDataPathsById().isEmpty()
                || resolution.tagIdsByValueId().isEmpty()
                || resolution.contentTagIdsByContentId().isEmpty()
                || resolution.creativeGroupsById().isEmpty()
                || resolution.creativeGroupIdsByContentId().isEmpty()
                || resolution.searchVisibleContentIds().isEmpty()
                || resolution.searchIndexTermsByContentId().isEmpty()
                || resolution.terminalPages().isEmpty()
                || resolution.terminalPageDataPathsById().isEmpty()
                || resolution.indexEntries().isEmpty()
                || resolution.indexEntryDataPathsById().isEmpty()
                || resolution.missionJsonIds().isEmpty()
                || resolution.worldRegionIds().isEmpty()
                || resolution.worldHazardIds().isEmpty()
                || resolution.missionJsonDataPathsById().isEmpty()
                || resolution.worldRegionDataPathsById().isEmpty()
                || resolution.worldHazardDataPathsById().isEmpty()
                || resolution.uiThemeIds().isEmpty()
                || resolution.itemRecipeIdsByContentId().isEmpty()
                || resolution.itemLootTableIdsByContentId().isEmpty()
                || resolution.terminalPageReferencesById().isEmpty()
                || resolution.indexEntryReferencesById().isEmpty()
                || resolution.missionJsonReferencesById().isEmpty()
                || resolution.worldRegionReferencesById().isEmpty()
                || resolution.worldHazardReferencesById().isEmpty()
                || resolution.registryJsonContentFingerprintsByKind().isEmpty()
                || resolution.catalogJsonContentFingerprintsByKind().isEmpty()) {
            throw new IllegalStateException("Native registry content bindings did not resolve the full registry/data surface");
        }
        writeResolutionReport(args, resolution);
        System.out.println("agent4 native registry content backend PASS definitions=" + resolution.registeredContentCount());
    }

    private static EchoNativeRegistryContentSnapshot loadSnapshot(
            String[] args,
            EchoNativeRegistryContentBackend backend
    ) throws Exception {
        Path contractReport = args.length > 0
                ? Path.of(args[0])
                : Path.of("..", "reports", "echo", "assets", "adaptercore-registry-contracts.json");
        Path resourceIndexReport = args.length > 1
                ? Path.of(args[1])
                : Path.of("..", "reports", "echo", "assets", "all-addon-resource-index.json");
        if (Files.isRegularFile(contractReport) && Files.isRegularFile(resourceIndexReport)) {
            return backend.loadContractReport(contractReport, resourceIndexReport);
        }
        return sampleSnapshot();
    }

    private static EchoNativeRegistryContentSnapshot sampleSnapshot() {
        return new EchoNativeRegistryContentSnapshot(
                List.of(
                        EchoNativeRegistryContentDefinition.block(
                                "echoagent4:test_block",
                                "echoagent4",
                                "assets/echoagent4/blockstates/test_block.json",
                                "assets/echoagent4/models/block/test_block.json",
                                "assets/echoagent4/textures/block/test_block.png",
                                "block.echoagent4.test_block"
                        ),
                        EchoNativeRegistryContentDefinition.item(
                                "echoagent4:test_item",
                                "echoagent4",
                                "assets/echoagent4/models/item/test_item.json",
                                "assets/echoagent4/textures/item/test_item.png",
                                "item.echoagent4.test_item",
                                List.of("echoagent4:test_recipe"),
                                List.of("echoagent4:blocks/test_block"),
                                true
                        ),
                        EchoNativeRegistryContentDefinition.recipe(
                                "echoagent4:test_recipe",
                                "echoagent4",
                                "minecraft:crafting_shaped",
                                List.of("minecraft:stone"),
                                List.of("echoagent4:test_item"),
                                "data/echoagent4/recipes/test_recipe.json"
                        ),
                        EchoNativeRegistryContentDefinition.lootTable(
                                "echoagent4:blocks/test_block",
                                "echoagent4",
                                List.of("echoagent4:test_item"),
                                "data/echoagent4/loot_tables/blocks/test_block.json"
                        ),
                        new EchoNativeRegistryContentDefinition(
                                "structures",
                                "echoagent4:test_structure",
                                "echoagent4",
                                "nbt",
                                "",
                                "",
                                "",
                                "",
                                List.of(),
                                List.of(),
                                List.of("echoagent4:test_block"),
                                "data/echoagent4/structures/test_structure.nbt",
                                false,
                                1
                        ),
                        EchoNativeRegistryContentDefinition.tag(
                                "minecraft:mineable/pickaxe",
                                "echoagent4",
                                "blocks",
                                List.of("echoagent4:test_block"),
                                "data/minecraft/tags/block/mineable/pickaxe.json",
                                2
                        )
                ),
                List.of("terminal/agent4/test"),
                List.of("echoagent4:test_item")
        );
    }

    private static void writeResolutionReport(String[] args, EchoNativeRegistryRuntimeResolution resolution) throws Exception {
        Path reportPath = args.length > 2
                ? Path.of(args[2])
                : Path.of("..", "reports", "echo", "assets", "native-runtime-resolution-java.json");
        Path manifestPath = reportPath.resolveSibling("native-materialized-resource-pack-manifest.json");
        Files.createDirectories(reportPath.toAbsolutePath().getParent());
        Files.writeString(manifestPath, EchoNativeJson.write(resourceManifest(resolution)), StandardCharsets.UTF_8);
        Files.writeString(reportPath, EchoNativeJson.write(report(resolution, manifestPath)), StandardCharsets.UTF_8);
    }

    private static Map<String, Object> report(EchoNativeRegistryRuntimeResolution resolution, Path manifestPath) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("schema", "echo.agent4.native_runtime_resolution_java.v1");
        data.put("runtimeId", resolution.runtimeId());
        data.put("contentIdsByRegistry", resolution.contentIdsByRegistry());
        data.put("recipesById", resolution.recipesById());
        data.put("recipeInputsById", resolution.recipeInputsById());
        data.put("recipeIdsByInputId", resolution.recipeIdsByInputId());
        data.put("recipeIdsByOutputId", resolution.recipeIdsByOutputId());
        data.put("recipeTypesById", resolution.recipeTypesById());
        data.put("recipeDataPathsById", resolution.recipeDataPathsById());
        data.put("lootById", resolution.lootById());
        data.put("lootDataPathsById", resolution.lootDataPathsById());
        data.put("lootTableIdsByEntryId", resolution.lootTableIdsByEntryId());
        data.put("soundsById", resolution.soundsById());
        data.put("soundDefinitionDataPathsById", resolution.soundDefinitionDataPathsById());
        data.put("structuresById", resolution.structuresById());
        data.put("structureTypesById", resolution.structureTypesById());
        data.put("structureIdsByReferenceId", resolution.structureIdsByReferenceId());
        data.put("structureAssetPathsById", resolution.structureAssetPathsById());
        data.put("tagsById", resolution.tagsById());
        data.put("tagDataPathsById", resolution.tagDataPathsById());
        data.put("tagIdsByValueId", resolution.tagIdsByValueId());
        data.put("contentTagIdsByContentId", resolution.contentTagIdsByContentId());
        data.put("creativeGroupsById", resolution.creativeGroupsById());
        data.put("creativeGroupIdsByContentId", resolution.creativeGroupIdsByContentId());
        data.put("resourceIdsByKind", resolution.resourceIdsByKind());
        data.put("resourcePathsById", resolution.resourcePathsById());
        data.put("resourceIdsByNamespace", resolution.resourceIdsByNamespace());
        data.put("resourceContentFingerprintsByPath", resolution.resourceContentFingerprintsByPath());
        data.put("resourceFileFingerprintsByKind", resolution.resourceFileFingerprintsByKind());
        data.put("langKeysByContentId", resolution.langKeysByContentId());
        data.put("langValuesByContentId", resolution.langValuesByContentId());
        data.put("langAssetsByContentId", resolution.langAssetsByContentId());
        data.put("contentIdsByLangKey", resolution.contentIdsByLangKey());
        data.put("contentIdsByLangValue", resolution.contentIdsByLangValue());
        data.put("jsonResourceIdsByKind", resolution.jsonResourceIdsByKind());
        data.put("dataDefinitionIdsByKind", resolution.dataDefinitionIdsByKind());
        data.put("dataDefinitionPathsById", resolution.dataDefinitionPathsById());
        data.put("jsonContentFingerprintsById", resolution.jsonContentFingerprintsById());
        data.put("registrySourcePathsById", resolution.registrySourcePathsById());
        data.put("jsonParseIssues", resolution.jsonParseIssues());
        data.put("modelDependenciesById", resolution.modelDependenciesById());
        data.put("textureDependenciesById", resolution.textureDependenciesById());
        data.put("modelDependencyPathsById", resolution.modelDependencyPathsById());
        data.put("textureDependencyPathsById", resolution.textureDependencyPathsById());
        data.put("soundDependenciesById", resolution.soundDependenciesById());
        data.put("soundEventIdsByAssetId", resolution.soundEventIdsByAssetId());
        data.put("soundAssetPathsById", resolution.soundAssetPathsById());
        data.put("unresolvedModelReferences", resolution.unresolvedModelReferences());
        data.put("unresolvedTextureReferences", resolution.unresolvedTextureReferences());
        data.put("unresolvedSoundReferences", resolution.unresolvedSoundReferences());
        data.put("terminalPages", resolution.terminalPages());
        data.put("indexEntries", resolution.indexEntries());
        data.put("terminalPageDataPathsById", resolution.terminalPageDataPathsById());
        data.put("indexEntryDataPathsById", resolution.indexEntryDataPathsById());
        data.put("missionJsonIds", resolution.missionJsonIds());
        data.put("worldRegionIds", resolution.worldRegionIds());
        data.put("worldHazardIds", resolution.worldHazardIds());
        data.put("missionJsonDataPathsById", resolution.missionJsonDataPathsById());
        data.put("worldRegionDataPathsById", resolution.worldRegionDataPathsById());
        data.put("worldHazardDataPathsById", resolution.worldHazardDataPathsById());
        data.put("uiThemeIds", resolution.uiThemeIds());
        data.put("terminalPageReferencesById", resolution.terminalPageReferencesById());
        data.put("indexEntryReferencesById", resolution.indexEntryReferencesById());
        data.put("missionJsonReferencesById", resolution.missionJsonReferencesById());
        data.put("worldRegionReferencesById", resolution.worldRegionReferencesById());
        data.put("worldHazardReferencesById", resolution.worldHazardReferencesById());
        data.put("requiredAssetIssues", resolution.requiredAssetIssues());
        data.put("requiredAssetValidationMode", resolution.materializedResourceCount() > 0 ? "resource-index" : "field-presence");
        data.put("resourceReady", resolution.resourcePackMaterialized());
        data.put("materializedResourceCount", resolution.materializedResourceCount());
        data.put("resourceManifestFingerprint", resolution.resourceManifestFingerprint());
        data.put("materializedResourceManifestPath", manifestPath.toAbsolutePath().normalize().toString().replace("\\", "/"));
        data.put("materializedResourceManifestFingerprint", resolution.resourceManifestFingerprint());
        data.put("materializedResourceRootPath", materializedResourceRoot().toString().replace("\\", "/"));
        data.put("materializedResourceFileCount", regularFileCount(materializedResourceRoot()));
        data.put("materializedResourceByteCount", regularFileByteCount(materializedResourceRoot()));
        data.put("materializedJsonFileCount", stagedJsonFileCount(materializedResourceRoot()));
        data.put("assetIndexEntryCount", resolution.assetIndexEntryCount());
        data.put("dataDefinitionEntryCount", resolution.dataDefinitionEntryCount());
        data.put("duplicateResourceIdsByKind", resolution.duplicateResourceIdsByKind());
        data.put("mergeableTagOverlayIds", resolution.mergeableTagOverlayIds());
        data.put("blockingDuplicateResourceIds", resolution.blockingDuplicateResourceIds());
        data.put("registeredContentCount", resolution.registeredContentCount());
        data.put("searchVisibleContentIds", resolution.searchVisibleContentIds());
        data.put("searchVisibleContentCount", resolution.searchVisibleContentCount());
        data.put("searchIndexTermsByContentId", resolution.searchIndexTermsByContentId());
        data.put("searchContentIdsByTerm", resolution.searchContentIdsByTerm());
        data.put("contentAssetBindingsById", resolution.contentAssetBindingsById());
        data.put("resolvedContentAssetPathsById", resolution.resolvedContentAssetPathsById());
        data.put("requiredAssetChecksByContentId", resolution.requiredAssetChecksByContentId());
        data.put("contentIdsByAssetId", resolution.contentIdsByAssetId());
        data.put("blockstateAssetsByContentId", resolution.blockstateAssetsByContentId());
        data.put("rendererAssetsByContentId", resolution.rendererAssetsByContentId());
        data.put("textureAssetsByContentId", resolution.textureAssetsByContentId());
        data.put("blockstateAssetPathsByContentId", resolution.blockstateAssetPathsByContentId());
        data.put("rendererAssetPathsByContentId", resolution.rendererAssetPathsByContentId());
        data.put("textureAssetPathsByContentId", resolution.textureAssetPathsByContentId());
        data.put("langAssetPathsByContentId", resolution.langAssetPathsByContentId());
        data.put("uiAssets", resolution.uiAssets());
        data.put("uiAssetPathsById", resolution.uiAssetPathsById());
        data.put("textureForgeSpecIds", resolution.textureForgeSpecIds());
        data.put("textureForgeSpecOutputPathsById", resolution.textureForgeSpecOutputPathsById());
        data.put("textureForgeSpecStatusById", resolution.textureForgeSpecStatusById());
        data.put("textureForgeSpecStyleFamilyById", resolution.textureForgeSpecStyleFamilyById());
        data.put("textureForgeSpecPromptFieldsById", resolution.textureForgeSpecPromptFieldsById());
        data.put("textureForgeSpecDataPathsById", resolution.textureForgeSpecDataPathsById());
        data.put("tagMergedSourceCountsById", resolution.tagMergedSourceCountsById());
        data.put("mergedTagOverlayCount", resolution.mergedTagOverlayCount());
        data.put("itemRecipeIdsByContentId", resolution.itemRecipeIdsByContentId());
        data.put("itemLootTableIdsByContentId", resolution.itemLootTableIdsByContentId());
        data.put("registryJsonContentFingerprintsByKind", resolution.registryJsonContentFingerprintsByKind());
        data.put("catalogJsonContentFingerprintsByKind", resolution.catalogJsonContentFingerprintsByKind());
        return data;
    }

    private static Map<String, Object> resourceManifest(EchoNativeRegistryRuntimeResolution resolution) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("schema", "echo.agent4.native_materialized_resource_pack_manifest.v1");
        data.put("runtimeId", resolution.runtimeId());
        data.put("materializedResourceCount", resolution.materializedResourceCount());
        data.put("materializedResourceRootPath", materializedResourceRoot().toString().replace("\\", "/"));
        data.put("materializedResourceFileCount", regularFileCount(materializedResourceRoot()));
        data.put("materializedResourceByteCount", regularFileByteCount(materializedResourceRoot()));
        data.put("resourcePathsById", resolution.resourcePathsById());
        data.put("resourceIdsByNamespace", resolution.resourceIdsByNamespace());
        data.put("resourceContentFingerprintsByPath", resolution.resourceContentFingerprintsByPath());
        data.put("resourceFileFingerprintsByKind", resolution.resourceFileFingerprintsByKind());
        data.put("resourceManifestFingerprint", resolution.resourceManifestFingerprint());
        return data;
    }

    private static Path materializedResourceRoot() {
        return EchoNativeRegistryContentBackend.MATERIALIZED_RESOURCE_PACK_ROOT.toAbsolutePath().normalize();
    }

    private static void assertMaterializedResourceFiles(EchoNativeRegistryRuntimeResolution resolution) {
        Path root = materializedResourceRoot();
        long actualFileCount = regularFileCount(root);
        long expectedFileCount = (long) resolution.materializedResourceCount() - resolution.mergedTagOverlayCount();
        if (actualFileCount != expectedFileCount) {
            throw new IllegalStateException("Native materialized resource file count mismatch: expected="
                    + expectedFileCount + " actual=" + actualFileCount + " root=" + root);
        }
        if (regularFileByteCount(root) <= 0) {
            throw new IllegalStateException("Native materialized resource pack is empty: " + root);
        }
    }

    private static void assertResourceKindIndex(EchoNativeRegistryRuntimeResolution resolution) {
        ArrayList<String> issues = new ArrayList<>();
        int resourceIdCount = 0;
        for (String kind : REQUIRED_RESOURCE_KINDS) {
            List<String> ids = resolution.resourceIdsByKind().getOrDefault(kind, List.of());
            if (ids.isEmpty()) {
                issues.add("missing resource kind index: " + kind);
                continue;
            }
            resourceIdCount += ids.size();
            for (String id : ids) {
                if (id == null || id.isBlank() || !id.contains(":")) {
                    issues.add("malformed resource ID in kind " + kind + ": " + id);
                }
            }
        }
        if (resourceIdCount != resolution.materializedResourceCount()) {
            issues.add("resource kind index count mismatch: expected="
                    + resolution.materializedResourceCount() + " actual=" + resourceIdCount);
        }
        if (!issues.isEmpty()) {
            throw new IllegalStateException("Native resource kind indexes are invalid: " + issues);
        }
    }

    private static void assertResourceManifestBinding(EchoNativeRegistryRuntimeResolution resolution) {
        String expected = resourceManifestFingerprint(resolution);
        if (!expected.equals(resolution.resourceManifestFingerprint())) {
            throw new IllegalStateException("Native resource manifest fingerprint mismatch: expected="
                    + expected + " actual=" + resolution.resourceManifestFingerprint());
        }
    }

    private static void assertResourceFileFingerprints(EchoNativeRegistryRuntimeResolution resolution) {
        ArrayList<String> issues = new ArrayList<>();
        int fingerprintCount = 0;
        for (String kind : REQUIRED_RESOURCE_KINDS) {
            List<String> ids = resolution.resourceIdsByKind().getOrDefault(kind, List.of());
            List<String> fingerprints = resolution.resourceFileFingerprintsByKind().getOrDefault(kind, List.of());
            if (!ids.isEmpty() && fingerprints.isEmpty()) {
                issues.add("missing resource file fingerprints for kind " + kind);
            }
            fingerprintCount += fingerprints.size();
            for (String fingerprint : fingerprints) {
                if (!isResourceFileFingerprint(fingerprint)) {
                    issues.add("malformed resource file fingerprint: " + fingerprint);
                }
            }
        }
        if (fingerprintCount != resolution.materializedResourceCount()) {
            issues.add("resource file fingerprint count mismatch: expected="
                    + resolution.materializedResourceCount() + " actual=" + fingerprintCount);
        }
        if (!issues.isEmpty()) {
            throw new IllegalStateException("Native resource file fingerprint bindings are invalid: " + issues);
        }
    }

    private static boolean isResourceFileFingerprint(String fingerprint) {
        String[] parts = fingerprint.split("\\|", 4);
        if (parts.length != 4 || parts[0].isBlank() || parts[3].isBlank()) {
            return false;
        }
        if (!parts[1].matches("[0-9a-f]{64}")) {
            return false;
        }
        try {
            return Long.parseLong(parts[2]) > 0;
        } catch (NumberFormatException exception) {
            return false;
        }
    }

    private static void assertMaterializedJsonFiles(EchoNativeRegistryRuntimeResolution resolution) {
        long parsedJsonCount = parseStagedJsonFiles(materializedResourceRoot());
        if (parsedJsonCount < resolution.dataDefinitionEntryCount()) {
            throw new IllegalStateException("Native materialized JSON file count is too low: parsed="
                    + parsedJsonCount + " dataDefinitions=" + resolution.dataDefinitionEntryCount());
        }
    }

    private static void assertJsonContentFingerprints(EchoNativeRegistryRuntimeResolution resolution) {
        Map<String, List<String>> actual = stagedJsonContentFingerprintsById(
                materializedResourceRoot(),
                resolution.resourcePathsById()
        );
        if (actual.isEmpty() || !actual.equals(resolution.jsonContentFingerprintsById())) {
            throw new IllegalStateException("Native materialized JSON content fingerprints do not match parsed staged JSON");
        }
    }

    private static void assertDataDefinitionIndex(EchoNativeRegistryRuntimeResolution resolution) {
        Map<String, List<String>> expected = dataDefinitionIdsByKind(resolution.jsonResourceIdsByKind());
        if (expected.isEmpty() || !expected.equals(resolution.dataDefinitionIdsByKind())) {
            throw new IllegalStateException("Native data definition index does not match loaded JSON resources");
        }
        for (Map.Entry<String, List<String>> entry : resolution.dataDefinitionIdsByKind().entrySet()) {
            if (entry.getValue().isEmpty()) {
                throw new IllegalStateException("Native data definition kind has no IDs: " + entry.getKey());
            }
            for (String id : entry.getValue()) {
                if (!resolution.jsonContentFingerprintsById().containsKey(entry.getKey() + "|" + id)) {
                    throw new IllegalStateException("Native data definition JSON did not load content: "
                            + entry.getKey() + "|" + id);
                }
            }
        }
    }

    private static void assertDataDefinitionPaths(EchoNativeRegistryRuntimeResolution resolution) {
        Path root = materializedResourceRoot();
        ArrayList<String> issues = new ArrayList<>();
        TreeSet<String> expectedKeys = new TreeSet<>();
        for (Map.Entry<String, List<String>> entry : resolution.dataDefinitionIdsByKind().entrySet()) {
            for (String id : entry.getValue()) {
                expectedKeys.add(entry.getKey() + "|" + id);
            }
        }
        if (!expectedKeys.equals(new TreeSet<>(resolution.dataDefinitionPathsById().keySet()))) {
            issues.add("data definition path keys do not match data definition IDs");
        }
        for (Map.Entry<String, List<String>> entry : resolution.dataDefinitionPathsById().entrySet()) {
            List<String> expectedPaths = resolution.resourcePathsById().getOrDefault(entry.getKey(), List.of()).stream()
                    .filter(path -> path.endsWith(".json"))
                    .sorted()
                    .toList();
            if (!expectedPaths.equals(entry.getValue())) {
                issues.add("data definition paths do not match resource paths: " + entry.getKey());
            }
            if (!resolution.jsonContentFingerprintsById().containsKey(entry.getKey())) {
                issues.add("data definition path has no loaded JSON fingerprint: " + entry.getKey());
            }
            for (String relativePath : entry.getValue()) {
                Path resolved = root.resolve(relativePath).normalize();
                if (!resolved.startsWith(root) || !Files.isRegularFile(resolved)) {
                    issues.add("data definition path does not resolve: " + entry.getKey() + " -> " + relativePath);
                    continue;
                }
                parseJsonFile(resolved);
            }
        }
        if (!issues.isEmpty()) {
            throw new IllegalStateException("Native data definition paths are invalid: " + issues);
        }
    }

    private static Map<String, List<String>> dataDefinitionIdsByKind(Map<String, List<String>> jsonResourceIdsByKind) {
        Map<String, List<String>> ids = new LinkedHashMap<>();
        for (String kind : DATA_DEFINITION_KINDS) {
            List<String> values = jsonResourceIdsByKind.getOrDefault(kind, List.of());
            if (!values.isEmpty()) {
                ids.put(kind, values);
            }
        }
        return ids;
    }

    private static void assertResourcePaths(EchoNativeRegistryRuntimeResolution resolution) {
        Path root = materializedResourceRoot();
        TreeSet<String> expectedKeys = new TreeSet<>();
        for (Map.Entry<String, List<String>> entry : resolution.resourceIdsByKind().entrySet()) {
            for (String id : entry.getValue()) {
                expectedKeys.add(entry.getKey() + "|" + id);
            }
        }
        TreeSet<String> actualKeys = new TreeSet<>(resolution.resourcePathsById().keySet());
        ArrayList<String> issues = new ArrayList<>();
        if (!expectedKeys.equals(actualKeys)) {
            issues.add("resource path keys do not match audited resource IDs");
        }
        for (Map.Entry<String, List<String>> entry : resolution.resourcePathsById().entrySet()) {
            if (entry.getValue().isEmpty()) {
                issues.add("resource path list is empty: " + entry.getKey());
            }
            for (String relativePath : entry.getValue()) {
                if (relativePath.isBlank()) {
                    issues.add("resource path is blank: " + entry.getKey());
                    continue;
                }
                Path path = root.resolve(relativePath).normalize();
                if (!path.startsWith(root) || !Files.isRegularFile(path)) {
                    issues.add("resource path missing: " + entry.getKey() + " -> " + relativePath);
                }
            }
        }
        if (!issues.isEmpty()) {
            throw new IllegalStateException("Native materialized resource paths are invalid: " + issues);
        }
    }

    private static void assertResourceContentFingerprints(EchoNativeRegistryRuntimeResolution resolution) {
        Map<String, String> actual = stagedResourceContentFingerprints(materializedResourceRoot());
        if (actual.isEmpty() || !actual.equals(resolution.resourceContentFingerprintsByPath())) {
            throw new IllegalStateException("Native materialized resource content fingerprints do not match staged files");
        }
        if (!actual.keySet().equals(new TreeSet<>(flattenedResourcePaths(resolution.resourcePathsById())))) {
            throw new IllegalStateException("Native materialized resource content fingerprints do not match resource paths");
        }
    }

    private static void assertResourceNamespaceIndex(EchoNativeRegistryRuntimeResolution resolution) {
        Map<String, List<String>> expected = resourceIdsByNamespace(resolution.resourcePathsById());
        if (expected.isEmpty() || !expected.equals(resolution.resourceIdsByNamespace())) {
            throw new IllegalStateException("Native resource namespace index does not match materialized resource IDs");
        }
        TreeSet<String> flattened = new TreeSet<>();
        for (List<String> values : resolution.resourceIdsByNamespace().values()) {
            flattened.addAll(values);
        }
        if (!flattened.equals(new TreeSet<>(resolution.resourcePathsById().keySet()))) {
            throw new IllegalStateException("Native resource namespace index does not cover all materialized resource IDs");
        }
    }

    private static Map<String, List<String>> resourceIdsByNamespace(Map<String, List<String>> resourcePathsById) {
        Map<String, TreeSet<String>> grouped = new java.util.TreeMap<>();
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
            grouped.computeIfAbsent(id.substring(0, namespaceSeparator), ignored -> new TreeSet<>())
                    .add(resourceKey);
        }
        Map<String, List<String>> result = new LinkedHashMap<>();
        for (Map.Entry<String, TreeSet<String>> entry : grouped.entrySet()) {
            result.put(entry.getKey(), List.copyOf(entry.getValue()));
        }
        return result;
    }

    private static void assertRegistrySourcePaths(
            EchoNativeRegistryContentSnapshot snapshot,
            EchoNativeRegistryRuntimeResolution resolution
    ) {
        Path root = materializedResourceRoot();
        Map<String, List<String>> expected = registrySourcePaths(snapshot);
        ArrayList<String> issues = new ArrayList<>();
        if (!expected.equals(resolution.registrySourcePathsById())) {
            issues.add("registry source paths do not match staged source reference");
        }
        for (Map.Entry<String, List<String>> entry : resolution.registrySourcePathsById().entrySet()) {
            if (entry.getValue().isEmpty()) {
                issues.add("registry source path list is empty: " + entry.getKey());
            }
            for (String relativePath : entry.getValue()) {
                Path path = root.resolve(relativePath).normalize();
                if (!Files.isRegularFile(path)) {
                    issues.add("registry source path missing: " + entry.getKey() + " -> " + relativePath);
                }
            }
        }
        if (!issues.isEmpty()) {
            throw new IllegalStateException("Native registry source paths are invalid: " + issues);
        }
    }

    private static Map<String, List<String>> registrySourcePaths(EchoNativeRegistryContentSnapshot snapshot) {
        LinkedHashMap<String, List<String>> paths = new LinkedHashMap<>();
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

    private static void assertMaterializedRuntimeAssetReferences(EchoNativeRegistryRuntimeResolution resolution) {
        Path root = materializedResourceRoot();
        ArrayList<String> missing = new ArrayList<>();
        for (String modelId : resolution.rendererAssetsByContentId().values()) {
            requireStagedResource(root, modelId, ".json", missing, "renderer model");
        }
        for (List<String> modelIds : resolution.modelDependenciesById().values()) {
            for (String modelId : modelIds) {
                requireStagedResource(root, modelId, ".json", missing, "model dependency");
            }
        }
        for (List<String> textureIds : resolution.textureDependenciesById().values()) {
            for (String textureId : textureIds) {
                requireStagedResource(root, textureId, ".png", missing, "texture dependency");
            }
        }
        for (String uiAssetId : resolution.uiAssets()) {
            requireStagedUiResource(root, uiAssetId, missing);
        }
        assertUiAssetPaths(root, resolution);
        assertUiThemeCatalog(root, resolution);
        assertTextureForgeSpecRegistry(root, resolution);
        assertMaterializedLangValues(root, resolution);
        if (!missing.isEmpty()) {
            throw new IllegalStateException("Native materialized runtime asset references are missing: " + missing);
        }
    }

    private static void assertRenderDependencyBindings(EchoNativeRegistryRuntimeResolution resolution) {
        Path root = materializedResourceRoot();
        ArrayList<String> missing = new ArrayList<>();
        if (resolution.modelDependenciesById().isEmpty()) {
            missing.add("model dependency map is empty");
        }
        if (resolution.textureDependenciesById().isEmpty()) {
            missing.add("texture dependency map is empty");
        }
        for (Map.Entry<String, List<String>> entry : resolution.modelDependenciesById().entrySet()) {
            requireRenderDependencySource(root, entry.getKey(), missing, "model dependency source");
            if (entry.getValue().isEmpty()) {
                missing.add("model dependency list is empty: " + entry.getKey());
            }
            for (String modelId : entry.getValue()) {
                requireStagedResource(root, modelId, ".json", missing, "model dependency target");
            }
        }
        for (Map.Entry<String, List<String>> entry : resolution.textureDependenciesById().entrySet()) {
            requireRenderDependencySource(root, entry.getKey(), missing, "texture dependency source");
            if (entry.getValue().isEmpty()) {
                missing.add("texture dependency list is empty: " + entry.getKey());
            }
            for (String textureId : entry.getValue()) {
                requireStagedResource(root, textureId, ".png", missing, "texture dependency target");
            }
        }
        if (!missing.isEmpty()) {
            throw new IllegalStateException("Native render dependency bindings are invalid: " + missing);
        }
    }

    private static void assertRenderDependencyPaths(EchoNativeRegistryRuntimeResolution resolution) {
        Path root = materializedResourceRoot();
        ArrayList<String> issues = new ArrayList<>();
        assertDependencyPaths(
                root,
                "model",
                resolution.modelDependenciesById(),
                resolution.modelDependencyPathsById(),
                ".json",
                issues
        );
        assertDependencyPaths(
                root,
                "texture",
                resolution.textureDependenciesById(),
                resolution.textureDependencyPathsById(),
                ".png",
                issues
        );
        if (!issues.isEmpty()) {
            throw new IllegalStateException("Native render dependency paths are invalid: " + issues);
        }
    }

    private static void assertDependencyPaths(
            Path root,
            String name,
            Map<String, List<String>> dependenciesById,
            Map<String, List<String>> pathsById,
            String defaultExtension,
            List<String> issues
    ) {
        Map<String, List<String>> expected = expectedDependencyPaths(dependenciesById, defaultExtension);
        if (!expected.equals(pathsById)) {
            issues.add(name + " dependency path map does not match dependency IDs");
        }
        for (Map.Entry<String, List<String>> entry : pathsById.entrySet()) {
            if (entry.getValue().isEmpty()) {
                issues.add(name + " dependency path list is empty: " + entry.getKey());
            }
            for (String relativePath : entry.getValue()) {
                Path resolved = root.resolve(relativePath).normalize();
                if (!resolved.startsWith(root) || !Files.isRegularFile(resolved)) {
                    issues.add(name + " dependency path does not resolve: " + entry.getKey() + " -> " + relativePath);
                }
            }
        }
    }

    private static Map<String, List<String>> expectedDependencyPaths(
            Map<String, List<String>> dependenciesById,
            String defaultExtension
    ) {
        LinkedHashMap<String, List<String>> expected = new LinkedHashMap<>();
        dependenciesById.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    List<String> paths = entry.getValue().stream()
                            .filter(id -> !id.startsWith("minecraft:"))
                            .map(id -> stagedResourceRelativePath(id, defaultExtension))
                            .distinct()
                            .sorted()
                            .toList();
                    if (!paths.isEmpty()) {
                        expected.put(entry.getKey(), paths);
                    }
                });
        return expected;
    }

    private static void assertBlockstateAssetBindings(EchoNativeRegistryRuntimeResolution resolution) {
        Path root = materializedResourceRoot();
        TreeSet<String> blocks = new TreeSet<>(resolution.contentIdsByRegistry().getOrDefault("blocks", List.of()));
        ArrayList<String> issues = new ArrayList<>();
        if (!blocks.equals(new TreeSet<>(resolution.blockstateAssetsByContentId().keySet()))) {
            issues.add("blockstate asset keys do not match registered blocks");
        }
        for (Map.Entry<String, String> entry : resolution.blockstateAssetsByContentId().entrySet()) {
            if (!blocks.contains(entry.getKey())) {
                issues.add("blockstate asset content is not a registered block: " + entry.getKey());
            }
            requireStagedResource(root, entry.getValue(), ".json", issues, "blockstate asset");
        }
        if (!issues.isEmpty()) {
            throw new IllegalStateException("Native blockstate asset bindings are invalid: " + issues);
        }
    }

    private static void assertRendererAssetBindings(EchoNativeRegistryRuntimeResolution resolution) {
        Path root = materializedResourceRoot();
        TreeSet<String> registered = registeredGameplayIds(resolution);
        ArrayList<String> issues = new ArrayList<>();
        if (resolution.rendererAssetsByContentId().isEmpty()) {
            issues.add("renderer asset map is empty");
        }
        for (Map.Entry<String, String> entry : resolution.rendererAssetsByContentId().entrySet()) {
            if (!registered.contains(entry.getKey())) {
                issues.add("renderer asset content is not registered: " + entry.getKey());
            }
            String modelId = entry.getValue();
            if (modelId == null || modelId.isBlank()) {
                issues.add("renderer asset model is blank: " + entry.getKey());
            } else {
                requireStagedResource(root, modelId, ".json", issues, "renderer asset model");
            }
        }
        if (!issues.isEmpty()) {
            throw new IllegalStateException("Native renderer asset bindings are invalid: " + issues);
        }
    }

    private static void assertTextureAssetBindings(EchoNativeRegistryRuntimeResolution resolution) {
        Path root = materializedResourceRoot();
        TreeSet<String> registered = registeredGameplayIds(resolution);
        ArrayList<String> issues = new ArrayList<>();
        if (resolution.textureAssetsByContentId().isEmpty()) {
            issues.add("texture asset map is empty");
        }
        for (Map.Entry<String, String> entry : resolution.textureAssetsByContentId().entrySet()) {
            if (!registered.contains(entry.getKey())) {
                issues.add("texture asset content is not registered: " + entry.getKey());
            }
            String textureId = entry.getValue();
            if (textureId == null || textureId.isBlank()) {
                issues.add("texture asset is blank: " + entry.getKey());
            } else {
                requireStagedResource(root, textureId, ".png", issues, "texture asset");
            }
        }
        if (!issues.isEmpty()) {
            throw new IllegalStateException("Native texture asset bindings are invalid: " + issues);
        }
    }

    private static void assertTypedContentAssetPaths(EchoNativeRegistryRuntimeResolution resolution) {
        Path root = materializedResourceRoot();
        ArrayList<String> issues = new ArrayList<>();
        assertTypedAssetPathMap(root, "blockstate", resolution.blockstateAssetsByContentId(),
                resolution.blockstateAssetPathsByContentId(), ".json", issues);
        assertTypedAssetPathMap(root, "renderer", resolution.rendererAssetsByContentId(),
                resolution.rendererAssetPathsByContentId(), ".json", issues);
        assertTypedAssetPathMap(root, "texture", resolution.textureAssetsByContentId(),
                resolution.textureAssetPathsByContentId(), ".png", issues);
        assertTypedAssetPathMap(root, "lang", resolution.langAssetsByContentId(),
                resolution.langAssetPathsByContentId(), ".json", issues);
        if (!issues.isEmpty()) {
            throw new IllegalStateException("Native typed content asset paths are invalid: " + issues);
        }
    }

    private static void assertTypedAssetPathMap(
            Path root,
            String name,
            Map<String, String> assetIdsByContentId,
            Map<String, String> pathsByContentId,
            String defaultExtension,
            List<String> issues
    ) {
        Map<String, String> expected = expectedTypedAssetPaths(assetIdsByContentId, defaultExtension);
        if (!expected.equals(pathsByContentId)) {
            issues.add(name + " asset path map does not match asset IDs");
        }
        for (Map.Entry<String, String> entry : pathsByContentId.entrySet()) {
            Path resolved = root.resolve(entry.getValue()).normalize();
            if (!resolved.startsWith(root) || !Files.isRegularFile(resolved)) {
                issues.add(name + " asset path does not resolve: " + entry.getKey() + " -> " + entry.getValue());
            }
        }
    }

    private static Map<String, String> expectedTypedAssetPaths(
            Map<String, String> assetIdsByContentId,
            String defaultExtension
    ) {
        LinkedHashMap<String, String> expected = new LinkedHashMap<>();
        assetIdsByContentId.entrySet().stream()
                .filter(entry -> !entry.getValue().isBlank()
                        && entry.getValue().indexOf(':') > 0
                        && !entry.getValue().startsWith("minecraft:"))
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> expected.put(
                        entry.getKey(),
                        stagedResourceRelativePath(entry.getValue(), defaultExtension)
                ));
        return expected;
    }

    private static void assertContentAssetBindings(EchoNativeRegistryRuntimeResolution resolution) {
        Path root = materializedResourceRoot();
        TreeSet<String> registered = registeredGameplayIds(resolution);
        ArrayList<String> issues = new ArrayList<>();
        if (!registered.equals(new TreeSet<>(resolution.contentAssetBindingsById().keySet()))) {
            issues.add("content asset binding keys do not match registered gameplay IDs");
        }
        for (Map.Entry<String, List<String>> entry : resolution.contentAssetBindingsById().entrySet()) {
            String contentId = entry.getKey();
            Map<String, String> binding = bindingFields(entry.getValue());
            String registry = binding.getOrDefault("registry", "");
            if (registry.isBlank()) {
                issues.add(contentId + " missing registry binding");
            }
            if ("blocks".equals(registry)) {
                requireStagedResource(root, binding.get("blockstate"), ".json", issues, "content blockstate");
            }
            String model = binding.getOrDefault("model", "");
            String texture = binding.getOrDefault("texture", "");
            String langKey = binding.getOrDefault("langKey", "");
            String langValue = binding.getOrDefault("langValue", "");
            if (!model.equals(resolution.rendererAssetsByContentId().get(contentId))) {
                issues.add(contentId + " model binding does not match renderer asset");
            }
            if (!langKey.equals(resolution.langKeysByContentId().get(contentId))
                    || !langValue.equals(resolution.langValuesByContentId().get(contentId))) {
                issues.add(contentId + " lang binding does not match runtime lang maps");
            }
            requireStagedResource(root, model, ".json", issues, "content model");
            requireStagedResource(root, texture, ".png", issues, "content texture");
            if (langKey.isBlank() || langValue.isBlank()) {
                issues.add(contentId + " missing lang binding");
            }
        }
        if (!contentAssetLookup(resolution.contentAssetBindingsById()).equals(resolution.contentIdsByAssetId())) {
            issues.add("content asset lookup does not match content asset bindings");
        }
        for (Map.Entry<String, List<String>> entry : resolution.contentIdsByAssetId().entrySet()) {
            String assetId = entry.getKey();
            if (entry.getValue().isEmpty()) {
                issues.add(assetId + " has no content IDs");
            }
            requireStagedContentAsset(root, assetId, issues);
            for (String contentId : entry.getValue()) {
                if (!registered.contains(contentId)) {
                    issues.add("content asset lookup targets unregistered content: " + assetId + " -> " + contentId);
                    continue;
                }
                Map<String, String> binding = bindingFields(resolution.contentAssetBindingsById().getOrDefault(contentId, List.of()));
                if (!assetId.equals(binding.get("blockstate"))
                        && !assetId.equals(binding.get("model"))
                        && !assetId.equals(binding.get("texture"))) {
                    issues.add("content asset lookup entry is unresolved: " + assetId + " -> " + contentId);
                }
            }
        }
        if (!issues.isEmpty()) {
            throw new IllegalStateException("Native content asset runtime bindings are invalid: " + issues);
        }
    }

    private static void assertResolvedContentAssetPaths(EchoNativeRegistryRuntimeResolution resolution) {
        Path root = materializedResourceRoot();
        ArrayList<String> issues = new ArrayList<>();
        if (!new TreeSet<>(resolution.contentAssetBindingsById().keySet())
                .equals(new TreeSet<>(resolution.resolvedContentAssetPathsById().keySet()))) {
            issues.add("resolved content asset path keys do not match content asset binding keys");
        }
        for (Map.Entry<String, List<String>> entry : resolution.resolvedContentAssetPathsById().entrySet()) {
            if (entry.getValue().isEmpty()) {
                issues.add(entry.getKey() + " has no resolved content asset paths");
            }
            boolean hasRendererJson = false;
            boolean hasLang = false;
            for (String relativePath : entry.getValue()) {
                Path path = root.resolve(relativePath).normalize();
                if (!path.startsWith(root) || !Files.isRegularFile(path)) {
                    issues.add(entry.getKey() + " resolved asset path missing: " + relativePath);
                    continue;
                }
                hasRendererJson = hasRendererJson || relativePath.endsWith(".json");
                hasLang = hasLang || relativePath.endsWith("/lang/en_us.json");
            }
            if (!hasRendererJson || !hasLang) {
                issues.add(entry.getKey() + " did not resolve both renderer JSON and lang resource paths");
            }
        }
        if (!issues.isEmpty()) {
            throw new IllegalStateException("Native resolved content asset paths are invalid: " + issues);
        }
    }

    private static void assertRequiredAssetChecks(EchoNativeRegistryRuntimeResolution resolution) {
        Path root = materializedResourceRoot();
        ArrayList<String> issues = new ArrayList<>();
        if (!new TreeSet<>(resolution.contentAssetBindingsById().keySet())
                .equals(new TreeSet<>(resolution.requiredAssetChecksByContentId().keySet()))) {
            issues.add("required asset check keys do not match content asset binding keys");
        }
        for (Map.Entry<String, List<String>> entry : resolution.requiredAssetChecksByContentId().entrySet()) {
            String contentId = entry.getKey();
            Map<String, String> checks = bindingFields(entry.getValue());
            Map<String, String> bindings = bindingFields(resolution.contentAssetBindingsById().getOrDefault(contentId, List.of()));
            for (String key : List.of("registry", "blockstate", "model", "texture", "langKey", "langValue")) {
                String expected = bindings.getOrDefault(key, "");
                if (!expected.isBlank() && !expected.equals(checks.get(key))) {
                    issues.add(contentId + " required asset check mismatch for " + key);
                }
            }
            TreeSet<String> checkedPaths = new TreeSet<>();
            for (String check : entry.getValue()) {
                if (check.startsWith("resolvedPath=")) {
                    checkedPaths.add(check.substring("resolvedPath=".length()));
                }
            }
            TreeSet<String> resolvedPaths = new TreeSet<>(resolution.resolvedContentAssetPathsById().getOrDefault(contentId, List.of()));
            if (!checkedPaths.equals(resolvedPaths)) {
                issues.add(contentId + " required asset paths do not match resolved paths");
            }
            boolean hasLang = false;
            boolean hasRendererJson = false;
            for (String relativePath : checkedPaths) {
                Path path = root.resolve(relativePath).normalize();
                if (!path.startsWith(root) || !Files.isRegularFile(path)) {
                    issues.add(contentId + " required asset path missing: " + relativePath);
                    continue;
                }
                hasLang = hasLang || relativePath.endsWith("/lang/en_us.json");
                hasRendererJson = hasRendererJson || relativePath.endsWith(".json");
            }
            if (!hasLang || !hasRendererJson) {
                issues.add(contentId + " required asset checks did not include lang and renderer JSON paths");
            }
        }
        if (!issues.isEmpty()) {
            throw new IllegalStateException("Native required asset checks are invalid: " + issues);
        }
    }

    private static Map<String, List<String>> contentAssetLookup(Map<String, List<String>> bindingsByContentId) {
        LinkedHashMap<String, List<String>> assetsByContentId = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> entry : bindingsByContentId.entrySet()) {
            Map<String, String> binding = bindingFields(entry.getValue());
            ArrayList<String> assets = new ArrayList<>();
            for (String key : List.of("blockstate", "model", "texture")) {
                String assetId = binding.getOrDefault(key, "");
                if (!assetId.isBlank()) {
                    assets.add(assetId);
                }
            }
            assetsByContentId.put(entry.getKey(), assets);
        }
        return reverseLookup(assetsByContentId);
    }

    private static void requireStagedContentAsset(Path root, String assetId, ArrayList<String> issues) {
        if (assetId.contains(":textures/")) {
            requireStagedResource(root, assetId, ".png", issues, "content asset lookup");
        } else if (assetId.contains(":models/") || assetId.contains(":blockstates/")) {
            requireStagedResource(root, assetId, ".json", issues, "content asset lookup");
        } else {
            issues.add("content asset lookup uses unknown asset kind: " + assetId);
        }
    }

    private static void assertLangContentLookup(EchoNativeRegistryRuntimeResolution resolution) {
        Path root = materializedResourceRoot();
        TreeSet<String> registered = registeredGameplayIds(resolution);
        Map<String, String> stagedLangValues = stagedLangValues(root);
        ArrayList<String> issues = new ArrayList<>();
        if (!reverseStringLookup(resolution.langKeysByContentId()).equals(resolution.contentIdsByLangKey())) {
            issues.add("lang key lookup does not match lang keys");
        }
        if (!reverseStringLookup(resolution.langValuesByContentId()).equals(resolution.contentIdsByLangValue())) {
            issues.add("lang value lookup does not match lang values");
        }
        for (Map.Entry<String, List<String>> entry : resolution.contentIdsByLangKey().entrySet()) {
            String langKey = entry.getKey();
            if (!stagedLangValues.containsKey(langKey)) {
                issues.add("lang key lookup missing staged lang key: " + langKey);
            }
            for (String contentId : entry.getValue()) {
                if (!registered.contains(contentId)) {
                    issues.add("lang key lookup targets unregistered content: " + langKey + " -> " + contentId);
                } else if (!langKey.equals(resolution.langKeysByContentId().get(contentId))) {
                    issues.add("lang key lookup entry is unresolved: " + langKey + " -> " + contentId);
                }
            }
        }
        for (Map.Entry<String, List<String>> entry : resolution.contentIdsByLangValue().entrySet()) {
            String langValue = entry.getKey();
            for (String contentId : entry.getValue()) {
                String langKey = resolution.langKeysByContentId().get(contentId);
                if (!registered.contains(contentId)) {
                    issues.add("lang value lookup targets unregistered content: " + langValue + " -> " + contentId);
                } else if (!langValue.equals(resolution.langValuesByContentId().get(contentId))) {
                    issues.add("lang value lookup entry is unresolved: " + langValue + " -> " + contentId);
                } else if (!langValue.equals(stagedLangValues.get(langKey))) {
                    issues.add("lang value lookup missing staged value: " + contentId + " -> " + langValue);
                }
            }
        }
        if (!issues.isEmpty()) {
            throw new IllegalStateException("Native lang content lookup is invalid: " + issues);
        }
    }

    private static void assertLangAssetBindings(EchoNativeRegistryRuntimeResolution resolution) {
        Path root = materializedResourceRoot();
        ArrayList<String> issues = new ArrayList<>();
        if (!new TreeSet<>(resolution.langKeysByContentId().keySet())
                .equals(new TreeSet<>(resolution.langAssetsByContentId().keySet()))) {
            issues.add("lang asset keys do not match localized content IDs");
        }
        for (Map.Entry<String, String> entry : resolution.langAssetsByContentId().entrySet()) {
            String contentId = entry.getKey();
            String assetId = entry.getValue();
            int separator = contentId.indexOf(':');
            if (separator <= 0) {
                issues.add("lang asset content ID is invalid: " + contentId);
                continue;
            }
            String expectedAsset = contentId.substring(0, separator) + ":lang/en_us";
            if (!expectedAsset.equals(assetId)) {
                issues.add("lang asset namespace mismatch: " + contentId + " -> " + assetId);
            }
            requireStagedResource(root, assetId, ".json", issues, "lang asset");
        }
        if (!issues.isEmpty()) {
            throw new IllegalStateException("Native lang asset bindings are invalid: " + issues);
        }
    }

    private static Map<String, List<String>> reverseStringLookup(Map<String, String> valuesById) {
        LinkedHashMap<String, List<String>> listValuesById = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : valuesById.entrySet()) {
            if (entry.getValue() != null && !entry.getValue().isBlank()) {
                listValuesById.put(entry.getKey(), List.of(entry.getValue()));
            }
        }
        return reverseLookup(listValuesById);
    }

    private static void assertStagedRecipeLootBindings(
            EchoNativeRegistryContentSnapshot snapshot,
            EchoNativeRegistryRuntimeResolution resolution
    ) {
        Path root = materializedResourceRoot();
        ArrayList<String> issues = new ArrayList<>();
        for (EchoNativeRegistryContentDefinition definition : snapshot.definitions()) {
            if (!"recipes".equals(definition.registry()) && !"lootTables".equals(definition.registry())) {
                continue;
            }
            Path stagedPath = stagedPathFromSource(root, definition.source());
            if (!Files.isRegularFile(stagedPath)) {
                issues.add(definition.registry() + "=" + definition.id() + " missing staged file " + stagedPath);
                continue;
            }
            Object parsed = parseJsonFile(stagedPath);
            if ("recipes".equals(definition.registry())) {
                Map<String, Object> object = EchoNativeJson.asObject(parsed);
                List<String> inputs = extractIds(firstPresent(object, "ingredients", "key", parsed));
                List<String> outputs = extractIds(firstPresent(object, "result", "results", parsed));
                if (!inputs.equals(resolution.recipeInputsById().getOrDefault(definition.id(), List.of()))) {
                    issues.add("recipe inputs mismatch " + definition.id());
                }
                if (!outputs.equals(resolution.recipesById().getOrDefault(definition.id(), List.of()))) {
                    issues.add("recipe outputs mismatch " + definition.id());
                }
            } else {
                List<String> entries = extractIds(parsed);
                if (!entries.equals(resolution.lootById().getOrDefault(definition.id(), List.of()))) {
                    issues.add("loot entries mismatch " + definition.id());
                }
            }
        }
        if (!issues.isEmpty()) {
            throw new IllegalStateException("Native staged recipe/loot bindings do not match runtime bindings: " + issues);
        }
    }

    private static void assertRecipeDataPaths(EchoNativeRegistryRuntimeResolution resolution) {
        Path root = materializedResourceRoot();
        ArrayList<String> issues = new ArrayList<>();
        if (!new TreeSet<>(resolution.recipesById().keySet())
                .equals(new TreeSet<>(resolution.recipeDataPathsById().keySet()))) {
            issues.add("recipe data path keys do not match runtime recipes");
        }
        for (Map.Entry<String, String> entry : resolution.recipeDataPathsById().entrySet()) {
            Path resolved = root.resolve(entry.getValue()).normalize();
            if (!Files.isRegularFile(resolved)) {
                issues.add("recipe data path does not resolve: " + entry.getKey() + " -> " + entry.getValue());
                continue;
            }
            if (!resolved.getFileName().toString().endsWith(".json")) {
                issues.add("recipe data path is not JSON: " + entry.getKey() + " -> " + entry.getValue());
                continue;
            }
            Object parsed = parseJsonFile(resolved);
            Map<String, Object> object = EchoNativeJson.asObject(parsed);
            Object type = object.get("type");
            if (!String.valueOf(type).equals(resolution.recipeTypesById().get(entry.getKey()))) {
                issues.add("recipe type mismatch from data path: " + entry.getKey());
            }
            List<String> inputs = extractIds(firstPresent(object, "ingredients", "key", parsed));
            List<String> outputs = extractIds(firstPresent(object, "result", "results", parsed));
            if (!inputs.equals(resolution.recipeInputsById().getOrDefault(entry.getKey(), List.of()))) {
                issues.add("recipe inputs mismatch from data path: " + entry.getKey());
            }
            if (!outputs.equals(resolution.recipesById().getOrDefault(entry.getKey(), List.of()))) {
                issues.add("recipe outputs mismatch from data path: " + entry.getKey());
            }
        }
        if (!issues.isEmpty()) {
            throw new IllegalStateException("Native recipe data paths are invalid: " + issues);
        }
    }

    private static void assertLootDataPaths(EchoNativeRegistryRuntimeResolution resolution) {
        Path root = materializedResourceRoot();
        ArrayList<String> issues = new ArrayList<>();
        if (!new TreeSet<>(resolution.lootById().keySet())
                .equals(new TreeSet<>(resolution.lootDataPathsById().keySet()))) {
            issues.add("loot data path keys do not match runtime loot tables");
        }
        for (Map.Entry<String, String> entry : resolution.lootDataPathsById().entrySet()) {
            Path resolved = root.resolve(entry.getValue()).normalize();
            if (!Files.isRegularFile(resolved)) {
                issues.add("loot data path does not resolve: " + entry.getKey() + " -> " + entry.getValue());
                continue;
            }
            if (!resolved.getFileName().toString().endsWith(".json")) {
                issues.add("loot data path is not JSON: " + entry.getKey() + " -> " + entry.getValue());
                continue;
            }
            List<String> entries = extractIds(parseJsonFile(resolved));
            if (!entries.equals(resolution.lootById().getOrDefault(entry.getKey(), List.of()))) {
                issues.add("loot entries mismatch from data path: " + entry.getKey());
            }
        }
        if (!issues.isEmpty()) {
            throw new IllegalStateException("Native loot data paths are invalid: " + issues);
        }
    }

    private static void assertItemRecipeLootRuntimeLinks(EchoNativeRegistryRuntimeResolution resolution) {
        TreeSet<String> registeredItems = new TreeSet<>(resolution.contentIdsByRegistry().getOrDefault("items", List.of()));
        ArrayList<String> issues = new ArrayList<>();
        if (!registeredItems.equals(new TreeSet<>(resolution.itemRecipeIdsByContentId().keySet()))) {
            issues.add("item recipe link keys do not match registered items");
        }
        if (!registeredItems.equals(new TreeSet<>(resolution.itemLootTableIdsByContentId().keySet()))) {
            issues.add("item loot link keys do not match registered items");
        }
        for (Map.Entry<String, List<String>> entry : resolution.itemRecipeIdsByContentId().entrySet()) {
            for (String recipeId : entry.getValue()) {
                List<String> outputs = resolution.recipesById().get(recipeId);
                if (outputs == null) {
                    issues.add("item recipe link is unresolved: " + entry.getKey() + " -> " + recipeId);
                } else if (!outputs.contains(entry.getKey())) {
                    issues.add("item recipe link does not output item: " + entry.getKey() + " -> " + recipeId);
                }
            }
        }
        for (Map.Entry<String, List<String>> entry : resolution.itemLootTableIdsByContentId().entrySet()) {
            for (String lootTableId : entry.getValue()) {
                List<String> entries = resolution.lootById().get(lootTableId);
                if (entries == null) {
                    issues.add("item loot link is unresolved: " + entry.getKey() + " -> " + lootTableId);
                } else if (!entries.contains(entry.getKey())) {
                    issues.add("item loot link does not contain item: " + entry.getKey() + " -> " + lootTableId);
                }
            }
        }
        if (!issues.isEmpty()) {
            throw new IllegalStateException("Native item recipe/loot runtime links are invalid: " + issues);
        }
    }

    private static void assertRecipeLootLookupIndexes(EchoNativeRegistryRuntimeResolution resolution) {
        ArrayList<String> issues = new ArrayList<>();
        if (!reverseLookup(resolution.recipeInputsById()).equals(resolution.recipeIdsByInputId())) {
            issues.add("recipe input lookup index does not match recipe inputs");
        }
        if (!reverseLookup(resolution.recipesById()).equals(resolution.recipeIdsByOutputId())) {
            issues.add("recipe output lookup index does not match recipe outputs");
        }
        if (!reverseLookup(resolution.lootById()).equals(resolution.lootTableIdsByEntryId())) {
            issues.add("loot entry lookup index does not match loot tables");
        }
        for (Map.Entry<String, List<String>> entry : resolution.recipeIdsByInputId().entrySet()) {
            for (String recipeId : entry.getValue()) {
                if (!resolution.recipeInputsById().getOrDefault(recipeId, List.of()).contains(entry.getKey())) {
                    issues.add("recipe lookup entry is unresolved: " + entry.getKey() + " -> " + recipeId);
                }
            }
        }
        for (Map.Entry<String, List<String>> entry : resolution.recipeIdsByOutputId().entrySet()) {
            for (String recipeId : entry.getValue()) {
                if (!resolution.recipesById().getOrDefault(recipeId, List.of()).contains(entry.getKey())) {
                    issues.add("recipe output lookup entry is unresolved: " + entry.getKey() + " -> " + recipeId);
                }
            }
        }
        for (Map.Entry<String, List<String>> entry : resolution.lootTableIdsByEntryId().entrySet()) {
            for (String lootTableId : entry.getValue()) {
                if (!resolution.lootById().getOrDefault(lootTableId, List.of()).contains(entry.getKey())) {
                    issues.add("loot lookup entry is unresolved: " + entry.getKey() + " -> " + lootTableId);
                }
            }
        }
        if (!issues.isEmpty()) {
            throw new IllegalStateException("Native recipe/loot runtime lookup indexes are invalid: " + issues);
        }
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

    private static void assertStagedSoundBindings(EchoNativeRegistryRuntimeResolution resolution) {
        Path root = materializedResourceRoot();
        LinkedHashMap<String, List<String>> soundEvents = new LinkedHashMap<>();
        LinkedHashMap<String, List<String>> soundDependencies = new LinkedHashMap<>();
        ArrayList<String> missingFiles = new ArrayList<>();
        try (var paths = Files.walk(root.resolve("assets"))) {
            List<Path> soundJsonFiles = paths.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().equals("sounds.json"))
                    .sorted()
                    .toList();
            for (Path path : soundJsonFiles) {
                String namespace = path.getParent().getFileName().toString();
                Map<String, Object> json = EchoNativeJson.asObject(parseJsonFile(path));
                for (Map.Entry<String, Object> entry : json.entrySet()) {
                    Map<String, Object> soundEvent = EchoNativeJson.asObject(entry.getValue());
                    Object sounds = soundEvent.get("sounds");
                    String soundEventId = namespace + ":" + entry.getKey();
                    soundEvents.put(soundEventId, extractIds(sounds));
                    List<String> dependencies = normalizedSoundDependencies(sounds, namespace);
                    if (!dependencies.isEmpty()) {
                        soundDependencies.put(soundEventId, dependencies);
                    }
                    for (String dependency : dependencies) {
                        if (!dependency.startsWith("minecraft:") && !Files.isRegularFile(stagedSoundPath(root, dependency))) {
                            missingFiles.add(soundEventId + " -> " + dependency);
                        }
                    }
                }
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to read materialized native sound bindings under " + root, exception);
        }
        if (!soundEvents.equals(resolution.soundsById())) {
            throw new IllegalStateException("Native staged sounds.json events do not match runtime sounds");
        }
        if (!soundDependencies.equals(resolution.soundDependenciesById())) {
            throw new IllegalStateException("Native staged sound dependencies do not match runtime sound dependencies");
        }
        if (!missingFiles.isEmpty()) {
            throw new IllegalStateException("Native staged sound files are missing: " + missingFiles);
        }
    }

    private static void assertSoundDefinitionDataPaths(EchoNativeRegistryRuntimeResolution resolution) {
        Path root = materializedResourceRoot();
        ArrayList<String> issues = new ArrayList<>();
        if (!new TreeSet<>(resolution.soundsById().keySet())
                .equals(new TreeSet<>(resolution.soundDefinitionDataPathsById().keySet()))) {
            issues.add("sound definition data path keys do not match runtime sound events");
        }
        for (Map.Entry<String, String> entry : resolution.soundDefinitionDataPathsById().entrySet()) {
            Path resolved = root.resolve(entry.getValue()).normalize();
            if (!Files.isRegularFile(resolved)) {
                issues.add("sound definition data path does not resolve: " + entry.getKey() + " -> " + entry.getValue());
                continue;
            }
            if (!resolved.getFileName().toString().equals("sounds.json")) {
                issues.add("sound definition data path is not sounds.json: " + entry.getKey() + " -> " + entry.getValue());
                continue;
            }
            Object parsed = parseJsonFile(resolved);
            Map<String, Object> object = EchoNativeJson.asObject(parsed);
            int separator = entry.getKey().indexOf(':');
            if (separator <= 0 || separator + 1 >= entry.getKey().length()) {
                issues.add("sound definition event ID is invalid: " + entry.getKey());
                continue;
            }
            String namespace = entry.getKey().substring(0, separator);
            String eventName = entry.getKey().substring(separator + 1);
            Object event = object.get(eventName);
            if (event == null) {
                issues.add("sound definition event is missing from sounds.json: " + entry.getKey());
                continue;
            }
            Map<String, Object> soundEvent = EchoNativeJson.asObject(event);
            Object sounds = soundEvent.get("sounds");
            if (!extractIds(sounds).equals(resolution.soundsById().getOrDefault(entry.getKey(), List.of()))) {
                issues.add("sound definition sounds mismatch: " + entry.getKey());
            }
            List<String> dependencies = normalizedSoundDependencies(sounds, namespace);
            if (!dependencies.equals(resolution.soundDependenciesById().getOrDefault(entry.getKey(), List.of()))) {
                issues.add("sound definition dependencies mismatch: " + entry.getKey());
            }
        }
        if (!issues.isEmpty()) {
            throw new IllegalStateException("Native sound definition data paths are invalid: " + issues);
        }
    }

    private static void assertSoundAssetEventLookup(EchoNativeRegistryRuntimeResolution resolution) {
        Path root = materializedResourceRoot();
        ArrayList<String> issues = new ArrayList<>();
        if (!reverseLookup(resolution.soundDependenciesById()).equals(resolution.soundEventIdsByAssetId())) {
            issues.add("sound asset event lookup does not match sound dependencies");
        }
        for (Map.Entry<String, List<String>> entry : resolution.soundEventIdsByAssetId().entrySet()) {
            String assetId = entry.getKey();
            if (entry.getValue().isEmpty()) {
                issues.add("sound asset lookup has no events: " + assetId);
            }
            if (!assetId.startsWith("minecraft:") && !Files.isRegularFile(stagedSoundPath(root, assetId))) {
                issues.add("sound asset lookup missing staged file: " + assetId);
            }
            for (String soundEventId : entry.getValue()) {
                if (!resolution.soundDependenciesById().getOrDefault(soundEventId, List.of()).contains(assetId)) {
                    issues.add("sound asset lookup entry is unresolved: " + assetId + " -> " + soundEventId);
                }
            }
        }
        if (!issues.isEmpty()) {
            throw new IllegalStateException("Native sound asset event lookup is invalid: " + issues);
        }
    }

    private static void assertSoundDependencyBindings(EchoNativeRegistryRuntimeResolution resolution) {
        Path root = materializedResourceRoot();
        ArrayList<String> issues = new ArrayList<>();
        if (resolution.soundDependenciesById().isEmpty()) {
            issues.add("sound dependency map is empty");
        }
        for (Map.Entry<String, List<String>> entry : resolution.soundDependenciesById().entrySet()) {
            if (!resolution.soundsById().containsKey(entry.getKey())) {
                issues.add("sound dependency source is not a sound event: " + entry.getKey());
            }
            if (entry.getValue().isEmpty()) {
                issues.add("sound dependency list is empty: " + entry.getKey());
            }
            for (String assetId : entry.getValue()) {
                if (!assetId.startsWith("minecraft:") && !Files.isRegularFile(stagedSoundPath(root, assetId))) {
                    issues.add("sound dependency target missing staged file: " + entry.getKey() + " -> " + assetId);
                }
            }
        }
        if (!issues.isEmpty()) {
            throw new IllegalStateException("Native sound dependency bindings are invalid: " + issues);
        }
    }

    private static void assertSoundAssetPaths(EchoNativeRegistryRuntimeResolution resolution) {
        Path root = materializedResourceRoot();
        TreeSet<String> dependencyAssets = new TreeSet<>();
        for (List<String> assets : resolution.soundDependenciesById().values()) {
            for (String assetId : assets) {
                if (!assetId.startsWith("minecraft:")) {
                    dependencyAssets.add(assetId);
                }
            }
        }
        ArrayList<String> issues = new ArrayList<>();
        if (!dependencyAssets.equals(new TreeSet<>(resolution.soundAssetPathsById().keySet()))) {
            issues.add("sound asset path keys do not match sound dependency assets");
        }
        for (Map.Entry<String, String> entry : resolution.soundAssetPathsById().entrySet()) {
            Path resolved = root.resolve(entry.getValue()).normalize();
            if (!Files.isRegularFile(resolved)) {
                issues.add("sound asset path does not resolve: " + entry.getKey() + " -> " + entry.getValue());
            }
            if (!stagedSoundPath(root, entry.getKey()).equals(resolved)) {
                issues.add("sound asset path is not canonical: " + entry.getKey() + " -> " + entry.getValue());
            }
        }
        if (!issues.isEmpty()) {
            throw new IllegalStateException("Native sound asset paths are invalid: " + issues);
        }
    }

    private static List<String> normalizedSoundDependencies(Object sounds, String namespace) {
        TreeSet<String> dependencies = new TreeSet<>();
        if (sounds instanceof List<?> list) {
            for (Object sound : list) {
                String name = soundName(sound);
                if (!name.isBlank()) {
                    dependencies.add(normalizeSoundAssetId(name, namespace));
                }
            }
        }
        return List.copyOf(dependencies);
    }

    private static String soundName(Object sound) {
        if (sound instanceof String string) {
            return string;
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

    private static Path stagedSoundPath(Path root, String soundId) {
        int separator = soundId.indexOf(':');
        if (separator <= 0 || separator + 1 >= soundId.length()) {
            return root.resolve(soundId.replace(":", "/") + ".ogg").normalize();
        }
        String namespace = soundId.substring(0, separator);
        String valuePath = soundId.substring(separator + 1);
        String filename = valuePath.endsWith(".ogg") ? valuePath : valuePath + ".ogg";
        return root.resolve("assets").resolve(namespace).resolve(filename).normalize();
    }

    private static void assertStagedStructureBindings(
            EchoNativeRegistryContentSnapshot snapshot,
            EchoNativeRegistryRuntimeResolution resolution
    ) {
        Path root = materializedResourceRoot();
        ArrayList<String> issues = new ArrayList<>();
        for (EchoNativeRegistryContentDefinition definition : snapshot.definitions()) {
            if (!"structures".equals(definition.registry())) {
                continue;
            }
            Path stagedPath = stagedPathFromSource(root, definition.source());
            if (!Files.isRegularFile(stagedPath)) {
                issues.add("structure=" + definition.id() + " missing staged file " + stagedPath);
                continue;
            }
            if (size(stagedPath) <= 0) {
                issues.add("structure=" + definition.id() + " empty staged file " + stagedPath);
            }
            String stagedType = structureType(stagedPath);
            if (!stagedType.equals(resolution.structureTypesById().get(definition.id()))) {
                issues.add("structure type mismatch " + definition.id() + " staged=" + stagedType);
            }
            if ("json".equals(stagedType)) {
                List<String> references = extractIds(parseJsonFile(stagedPath));
                if (!references.equals(resolution.structuresById().getOrDefault(definition.id(), List.of()))) {
                    issues.add("structure references mismatch " + definition.id());
                }
            } else if (!resolution.structuresById().getOrDefault(definition.id(), List.of()).isEmpty()) {
                issues.add("binary structure has unexpected runtime references " + definition.id());
            }
        }
        if (!issues.isEmpty()) {
            throw new IllegalStateException("Native staged structure bindings do not match runtime structures: " + issues);
        }
    }

    private static void assertStructureReferenceLookup(EchoNativeRegistryRuntimeResolution resolution) {
        ArrayList<String> issues = new ArrayList<>();
        if (!reverseLookup(resolution.structuresById()).equals(resolution.structureIdsByReferenceId())) {
            issues.add("structure reference lookup does not match structure references");
        }
        for (Map.Entry<String, List<String>> entry : resolution.structureIdsByReferenceId().entrySet()) {
            String referenceId = entry.getKey();
            if (entry.getValue().isEmpty()) {
                issues.add("structure reference lookup has no structures: " + referenceId);
            }
            for (String structureId : entry.getValue()) {
                if (!resolution.structuresById().getOrDefault(structureId, List.of()).contains(referenceId)) {
                    issues.add("structure reference lookup entry is unresolved: " + referenceId + " -> " + structureId);
                }
            }
        }
        if (!issues.isEmpty()) {
            throw new IllegalStateException("Native structure reference lookup is invalid: " + issues);
        }
    }

    private static void assertStructureAssetPaths(EchoNativeRegistryRuntimeResolution resolution) {
        Path root = materializedResourceRoot();
        ArrayList<String> issues = new ArrayList<>();
        if (!new TreeSet<>(resolution.structuresById().keySet())
                .equals(new TreeSet<>(resolution.structureAssetPathsById().keySet()))) {
            issues.add("structure asset path keys do not match runtime structures");
        }
        for (Map.Entry<String, String> entry : resolution.structureAssetPathsById().entrySet()) {
            Path resolved = root.resolve(entry.getValue()).normalize();
            if (!Files.isRegularFile(resolved)) {
                issues.add("structure asset path does not resolve: " + entry.getKey() + " -> " + entry.getValue());
                continue;
            }
            if (size(resolved) <= 0) {
                issues.add("structure asset path is empty: " + entry.getKey() + " -> " + entry.getValue());
            }
            String type = structureType(resolved);
            if (!type.equals(resolution.structureTypesById().get(entry.getKey()))) {
                issues.add("structure asset type mismatch: " + entry.getKey() + " -> " + entry.getValue());
            }
        }
        if (!issues.isEmpty()) {
            throw new IllegalStateException("Native structure asset paths are invalid: " + issues);
        }
    }

    private static String structureType(Path path) {
        String filename = path.getFileName().toString();
        if (filename.endsWith(".json")) {
            return "json";
        }
        if (filename.endsWith(".nbt")) {
            return "nbt";
        }
        return "unknown";
    }

    private static void assertCatalogJsonContentFingerprints(EchoNativeRegistryRuntimeResolution resolution) {
        Map<String, List<String>> expected = catalogJsonContentFingerprintsByKind(materializedResourceRoot(), resolution);
        ArrayList<String> issues = new ArrayList<>();
        if (expected.isEmpty() || !expected.equals(resolution.catalogJsonContentFingerprintsByKind())) {
            issues.add("catalog JSON content fingerprints do not match staged catalog JSON");
        }
        for (Map.Entry<String, List<String>> entry : resolution.catalogJsonContentFingerprintsByKind().entrySet()) {
            if (entry.getValue().isEmpty()) {
                issues.add("catalog JSON fingerprint list is empty: " + entry.getKey());
            }
        }
        if (!issues.isEmpty()) {
            throw new IllegalStateException("Native catalog JSON content bindings are invalid: " + issues);
        }
    }

    private static void assertRegistryJsonContentFingerprints(EchoNativeRegistryRuntimeResolution resolution) {
        Map<String, List<String>> expected = registryJsonContentFingerprintsByKind(materializedResourceRoot(), resolution);
        ArrayList<String> issues = new ArrayList<>();
        if (expected.isEmpty() || !expected.equals(resolution.registryJsonContentFingerprintsByKind())) {
            issues.add("registry JSON content fingerprints do not match staged registry JSON");
        }
        for (Map.Entry<String, List<String>> entry : resolution.registryJsonContentFingerprintsByKind().entrySet()) {
            if (entry.getValue().isEmpty()) {
                issues.add("registry JSON fingerprint list is empty: " + entry.getKey());
            }
        }
        if (!issues.isEmpty()) {
            throw new IllegalStateException("Native registry JSON content bindings are invalid: " + issues);
        }
    }

    private static Map<String, List<String>> registryJsonContentFingerprintsByKind(
            Path root,
            EchoNativeRegistryRuntimeResolution resolution
    ) {
        Map<String, List<String>> registries = new LinkedHashMap<>();
        addCatalogJsonFingerprints(registries, root, resolution.resourcePathsById(), "recipes", "recipes", null);
        addCatalogJsonFingerprints(registries, root, resolution.resourcePathsById(), "lootTables", "lootTables", null);
        addCatalogJsonFingerprints(registries, root, resolution.resourcePathsById(), "tags", "tags", null);
        addCatalogJsonFingerprints(registries, root, resolution.resourcePathsById(), "structures", "structures", null);
        addCatalogJsonFingerprints(registries, root, resolution.resourcePathsById(), "sounds", "sounds", null);
        return registries;
    }

    private static Map<String, List<String>> catalogJsonContentFingerprintsByKind(
            Path root,
            EchoNativeRegistryRuntimeResolution resolution
    ) {
        Map<String, List<String>> catalogs = new LinkedHashMap<>();
        addCatalogJsonFingerprints(catalogs, root, resolution.resourcePathsById(), "terminalPages", "terminalPages", null);
        addCatalogJsonFingerprints(catalogs, root, resolution.resourcePathsById(), "indexEntries", "data", "/echoindex/entries/");
        addCatalogJsonFingerprints(catalogs, root, resolution.resourcePathsById(), "missionJson", "missionJson", null);
        addCatalogJsonFingerprints(catalogs, root, resolution.resourcePathsById(), "worldRegions", "worldRegions", null);
        addCatalogJsonFingerprints(catalogs, root, resolution.resourcePathsById(), "worldHazards", "worldHazards", null);
        addCatalogJsonFingerprints(catalogs, root, resolution.resourcePathsById(), "uiThemes", "uiThemes", null);
        return catalogs;
    }

    private static void addCatalogJsonFingerprints(
            Map<String, List<String>> catalogs,
            Path root,
            Map<String, List<String>> resourcePathsById,
            String catalogKind,
            String resourceKind,
            String requiredPathMarker
    ) {
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
                Path path = root.resolve(relativePath).normalize();
                if (Files.isRegularFile(path)) {
                    values.add(id + "|" + relativePath + "|" + canonicalJsonFingerprint(path));
                }
            }
        }
        if (!values.isEmpty()) {
            values.sort(String::compareTo);
            catalogs.put(catalogKind, List.copyOf(values));
        }
    }

    private static void assertStagedCatalogBindings(EchoNativeRegistryRuntimeResolution resolution) {
        Path root = materializedResourceRoot();
        assertCatalogMatches(
                root, "terminal pages", "echoterminal/pages/",
                resolution.terminalPages(), resolution.terminalPageReferencesById()
        );
        assertCatalogMatches(
                root, "index entries", "echoindex/entries/",
                resolution.indexEntries(), resolution.indexEntryReferencesById()
        );
        assertCatalogMatches(
                root, "mission JSON", "missioncore/missions/",
                resolution.missionJsonIds(), resolution.missionJsonReferencesById()
        );
        assertCatalogMatches(
                root, "world regions", "echoworldcore/world_regions/",
                resolution.worldRegionIds(), resolution.worldRegionReferencesById()
        );
        assertCatalogMatches(
                root, "world hazards", "echoworldcore/world_hazards/",
                resolution.worldHazardIds(), resolution.worldHazardReferencesById()
        );
    }

    private static void assertTerminalIndexDataPaths(EchoNativeRegistryRuntimeResolution resolution) {
        Path root = materializedResourceRoot();
        ArrayList<String> issues = new ArrayList<>();
        assertCatalogDataPaths(
                root,
                "terminal page",
                resolution.terminalPages(),
                resolution.terminalPageDataPathsById(),
                resolution.terminalPageReferencesById(),
                issues
        );
        assertCatalogDataPaths(
                root,
                "index entry",
                resolution.indexEntries(),
                resolution.indexEntryDataPathsById(),
                resolution.indexEntryReferencesById(),
                issues
        );
        if (!issues.isEmpty()) {
            throw new IllegalStateException("Native terminal/index data paths are invalid: " + issues);
        }
    }

    private static void assertMissionWorldDataPaths(EchoNativeRegistryRuntimeResolution resolution) {
        Path root = materializedResourceRoot();
        ArrayList<String> issues = new ArrayList<>();
        assertCatalogDataPaths(
                root,
                "mission JSON",
                resolution.missionJsonIds(),
                resolution.missionJsonDataPathsById(),
                resolution.missionJsonReferencesById(),
                issues
        );
        assertCatalogDataPaths(
                root,
                "world region",
                resolution.worldRegionIds(),
                resolution.worldRegionDataPathsById(),
                resolution.worldRegionReferencesById(),
                issues
        );
        assertCatalogDataPaths(
                root,
                "world hazard",
                resolution.worldHazardIds(),
                resolution.worldHazardDataPathsById(),
                resolution.worldHazardReferencesById(),
                issues
        );
        if (!issues.isEmpty()) {
            throw new IllegalStateException("Native mission/world data paths are invalid: " + issues);
        }
    }

    private static void assertCatalogDataPaths(
            Path root,
            String catalogName,
            List<String> ids,
            Map<String, String> pathsById,
            Map<String, List<String>> referencesById,
            List<String> issues
    ) {
        if (!new TreeSet<>(ids).equals(new TreeSet<>(pathsById.keySet()))) {
            issues.add(catalogName + " data path keys do not match runtime IDs");
        }
        for (Map.Entry<String, String> entry : pathsById.entrySet()) {
            Path resolved = root.resolve(entry.getValue()).normalize();
            if (!Files.isRegularFile(resolved)) {
                issues.add(catalogName + " data path does not resolve: " + entry.getKey() + " -> " + entry.getValue());
                continue;
            }
            if (!resolved.getFileName().toString().endsWith(".json")) {
                issues.add(catalogName + " data path is not JSON: " + entry.getKey() + " -> " + entry.getValue());
                continue;
            }
            List<String> references = extractIds(parseJsonFile(resolved));
            if (!references.equals(referencesById.getOrDefault(entry.getKey(), List.of()))) {
                issues.add(catalogName + " references mismatch from data path: " + entry.getKey());
            }
        }
    }

    private static void assertCatalogMatches(
            Path root,
            String catalogName,
            String pathPrefix,
            List<String> expectedIds,
            Map<String, List<String>> expectedReferences
    ) {
        List<String> stagedIds = stagedCatalogIds(root, pathPrefix);
        if (!stagedIds.equals(expectedIds)) {
            throw new IllegalStateException("Native staged " + catalogName + " catalog does not match runtime IDs");
        }
        Map<String, List<String>> stagedReferences = stagedCatalogReferences(root, pathPrefix);
        if (!stagedReferences.equals(expectedReferences)) {
            throw new IllegalStateException("Native staged " + catalogName + " references do not match runtime bindings");
        }
    }

    private static List<String> stagedCatalogIds(Path root, String pathPrefix) {
        Path dataRoot = root.resolve("data");
        if (!Files.isDirectory(dataRoot)) {
            return List.of();
        }
        try (var paths = Files.walk(dataRoot)) {
            return paths.filter(Files::isRegularFile)
                    .filter(path -> path.toString().replace("\\", "/").endsWith(".json"))
                    .filter(path -> stagedDataValuePath(dataRoot, path).startsWith(pathPrefix))
                    .peek(EchoNativeAgent4RegistrySmokeMain::parseJsonFile)
                    .map(path -> stagedDataId(dataRoot, path))
                    .sorted()
                    .toList();
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to read native staged catalog " + pathPrefix, exception);
        }
    }

    private static Map<String, List<String>> stagedCatalogReferences(Path root, String pathPrefix) {
        Path dataRoot = root.resolve("data");
        if (!Files.isDirectory(dataRoot)) {
            return Map.of();
        }
        try (var paths = Files.walk(dataRoot)) {
            Map<String, List<String>> references = new LinkedHashMap<>();
            paths.filter(Files::isRegularFile)
                    .filter(path -> path.toString().replace("\\", "/").endsWith(".json"))
                    .filter(path -> stagedDataValuePath(dataRoot, path).startsWith(pathPrefix))
                    .sorted()
                    .forEach(path -> references.put(stagedDataId(dataRoot, path), extractIds(parseJsonFile(path))));
            return references;
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to read native staged catalog references " + pathPrefix, exception);
        }
    }

    private static String stagedDataId(Path dataRoot, Path path) {
        Path relative = dataRoot.relativize(path);
        String namespace = relative.getName(0).toString();
        String valuePath = relative.subpath(1, relative.getNameCount()).toString().replace("\\", "/");
        if (valuePath.endsWith(".json")) {
            valuePath = valuePath.substring(0, valuePath.length() - 5);
        }
        return namespace + ":" + valuePath;
    }

    private static String stagedDataValuePath(Path dataRoot, Path path) {
        Path relative = dataRoot.relativize(path);
        if (relative.getNameCount() < 2) {
            return "";
        }
        String valuePath = relative.subpath(1, relative.getNameCount()).toString().replace("\\", "/");
        if (valuePath.endsWith(".json")) {
            valuePath = valuePath.substring(0, valuePath.length() - 5);
        }
        return valuePath;
    }

    private static Path stagedPathFromSource(Path root, String source) {
        if (source == null || source.isBlank()) {
            return root.resolve("__missing_source__").normalize();
        }
        String normalized = source.replace("\\", "/");
        String marker = "/src/main/resources/";
        int index = normalized.indexOf(marker);
        if (index >= 0) {
            return root.resolve(normalized.substring(index + marker.length())).normalize();
        }
        return root.resolve(normalized).normalize();
    }

    private static Object parseJsonFile(Path path) {
        try {
            return EchoNativeJson.parse(Files.readString(path));
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to parse staged native binding file " + path, exception);
        }
    }

    private static Object firstPresent(Map<String, Object> object, String firstKey, String secondKey, Object fallback) {
        if (object.containsKey(firstKey)) {
            return object.get(firstKey);
        }
        if (object.containsKey(secondKey)) {
            return object.get(secondKey);
        }
        return fallback;
    }

    private static Map<String, String> bindingFields(List<String> values) {
        Map<String, String> fields = new LinkedHashMap<>();
        for (String value : values) {
            int index = value.indexOf('=');
            if (index > 0) {
                fields.put(value.substring(0, index), value.substring(index + 1));
            }
        }
        return fields;
    }

    private static List<String> extractIds(Object value) {
        TreeSet<String> ids = new TreeSet<>();
        collectIds(value, ids);
        return List.copyOf(ids);
    }

    private static void collectIds(Object value, TreeSet<String> ids) {
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

    private static void assertMaterializedLangValues(
            Path root,
            EchoNativeRegistryRuntimeResolution resolution
    ) {
        Map<String, String> stagedLangValues = stagedLangValues(root);
        ArrayList<String> missing = new ArrayList<>();
        for (Map.Entry<String, String> entry : resolution.langKeysByContentId().entrySet()) {
            String contentId = entry.getKey();
            String langKey = entry.getValue();
            String expectedValue = resolution.langValuesByContentId().get(contentId);
            if (langKey == null || expectedValue == null || !expectedValue.equals(stagedLangValues.get(langKey))) {
                missing.add(contentId + " -> " + langKey);
            }
        }
        if (!missing.isEmpty()) {
            throw new IllegalStateException("Native materialized lang values are missing or mismatched: " + missing);
        }
    }

    private static Map<String, String> stagedLangValues(Path root) {
        LinkedHashMap<String, String> values = new LinkedHashMap<>();
        try (var paths = Files.walk(root)) {
            List<Path> langFiles = paths.filter(Files::isRegularFile)
                    .filter(EchoNativeAgent4RegistrySmokeMain::isLangJson)
                    .sorted()
                    .toList();
            for (Path path : langFiles) {
                Map<String, Object> parsed = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(path)));
                for (Map.Entry<String, Object> entry : parsed.entrySet()) {
                    if (entry.getValue() instanceof String string) {
                        values.put(entry.getKey(), string);
                    }
                }
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to read materialized native lang files under " + root, exception);
        }
        if (values.isEmpty()) {
            throw new IllegalStateException("Native materialized resource pack has no lang values under " + root);
        }
        return values;
    }

    private static void requireStagedResource(
            Path root,
            String resourceId,
            String defaultExtension,
            List<String> missing,
            String referenceType
    ) {
        if (resourceId == null || resourceId.isBlank() || resourceId.startsWith("minecraft:")) {
            return;
        }
        Path path = stagedResourcePath(root, resourceId, defaultExtension);
        if (!Files.isRegularFile(path)) {
            missing.add(referenceType + "=" + resourceId + " path=" + path);
        }
    }

    private static void requireRenderDependencySource(
            Path root,
            String resourceId,
            List<String> missing,
            String referenceType
    ) {
        if (resourceId == null || resourceId.isBlank() || resourceId.startsWith("minecraft:")) {
            return;
        }
        if (resourceId.contains(":blockstates/") || resourceId.contains(":models/")) {
            requireStagedResource(root, resourceId, ".json", missing, referenceType);
        }
    }

    private static void requireStagedUiResource(Path root, String resourceId, List<String> missing) {
        if (resourceId == null || resourceId.isBlank() || resourceId.startsWith("minecraft:")) {
            return;
        }
        for (Path candidate : stagedUiResourcePaths(root, resourceId)) {
            if (Files.isRegularFile(candidate)) {
                return;
            }
        }
        missing.add("ui asset=" + resourceId + " candidates=" + stagedUiResourcePaths(root, resourceId));
    }

    private static void assertUiAssetPaths(Path root, EchoNativeRegistryRuntimeResolution resolution) {
        ArrayList<String> issues = new ArrayList<>();
        if (!new TreeSet<>(resolution.uiAssets()).equals(new TreeSet<>(resolution.uiAssetPathsById().keySet()))) {
            issues.add("UI asset path keys do not match UI assets");
        }
        for (String uiAsset : resolution.uiAssets()) {
            String relativePath = resolution.uiAssetPathsById().getOrDefault(uiAsset, "");
            if (relativePath.isBlank()) {
                issues.add("UI asset missing path: " + uiAsset);
                continue;
            }
            Path resolved = root.resolve(relativePath).normalize();
            if (!Files.isRegularFile(resolved)) {
                issues.add("UI asset path does not resolve: " + uiAsset + " -> " + relativePath);
            }
            if (!stagedUiResourcePaths(root, uiAsset).contains(resolved)) {
                issues.add("UI asset path is not a staged candidate: " + uiAsset + " -> " + relativePath);
            }
        }
        if (!issues.isEmpty()) {
            throw new IllegalStateException("Native UI asset paths are invalid: " + issues);
        }
    }

    private static void assertUiThemeCatalog(Path root, EchoNativeRegistryRuntimeResolution resolution) {
        ArrayList<String> issues = new ArrayList<>();
        if (resolution.uiThemeIds().isEmpty()) {
            issues.add("UI theme catalog is empty");
        }
        for (String themeId : resolution.uiThemeIds()) {
            if (!resolution.uiAssets().contains(themeId)) {
                issues.add("UI theme is not present in runtime UI assets: " + themeId);
            }
            String relativePath = resolution.uiAssetPathsById().getOrDefault(themeId, "");
            if (relativePath.isBlank()) {
                issues.add("UI theme missing mounted asset path: " + themeId);
                continue;
            }
            Path resolved = root.resolve(relativePath).normalize();
            if (!Files.isRegularFile(resolved)) {
                issues.add("UI theme path does not resolve: " + themeId + " -> " + relativePath);
            }
            if (!stagedUiResourcePaths(root, themeId).contains(resolved)) {
                issues.add("UI theme path is not a staged candidate: " + themeId + " -> " + relativePath);
            }
        }
        if (!issues.isEmpty()) {
            throw new IllegalStateException("Native UI theme catalog is invalid: " + issues);
        }
    }

    private static void assertTextureForgeSpecRegistry(Path root, EchoNativeRegistryRuntimeResolution resolution) {
        ArrayList<String> issues = new ArrayList<>();
        TreeSet<String> specIds = new TreeSet<>(resolution.textureForgeSpecIds());
        if (specIds.isEmpty()) {
            issues.add("TextureForge spec registry is empty");
        }
        requireSameKeys(specIds, resolution.textureForgeSpecOutputPathsById(), "output paths", issues);
        requireSameKeys(specIds, resolution.textureForgeSpecStatusById(), "statuses", issues);
        requireSameKeys(specIds, resolution.textureForgeSpecStyleFamilyById(), "style families", issues);
        requireSameKeys(specIds, resolution.textureForgeSpecPromptFieldsById(), "prompt fields", issues);
        requireSameKeys(specIds, resolution.textureForgeSpecDataPathsById(), "data paths", issues);
        for (String specId : specIds) {
            String outputPath = resolution.textureForgeSpecOutputPathsById().getOrDefault(specId, "");
            if (!outputPath.startsWith("assets/") || !outputPath.contains("/textures/") || !outputPath.endsWith(".png")) {
                issues.add("TextureForge spec output path is invalid: " + specId + " -> " + outputPath);
            }
            if (resolution.textureForgeSpecStatusById().getOrDefault(specId, "").isBlank()) {
                issues.add("TextureForge spec status is blank: " + specId);
            }
            if (resolution.textureForgeSpecStyleFamilyById().getOrDefault(specId, "").isBlank()) {
                issues.add("TextureForge spec style family is blank: " + specId);
            }
            List<String> promptFields = resolution.textureForgeSpecPromptFieldsById().getOrDefault(specId, List.of());
            if (promptFields.stream().noneMatch(value -> value.startsWith("notes="))
                    || promptFields.stream().noneMatch(value -> value.startsWith("defaultResolution="))) {
                issues.add("TextureForge spec prompt fields are incomplete: " + specId);
            }
            String dataPath = resolution.textureForgeSpecDataPathsById().getOrDefault(specId, "");
            Path resolved = root.resolve(dataPath).normalize();
            if (dataPath.isBlank() || !resolved.startsWith(root) || !Files.isRegularFile(resolved)) {
                issues.add("TextureForge spec data path does not resolve: " + specId + " -> " + dataPath);
            } else {
                Object parsed = parseJsonFile(resolved);
                if (EchoNativeJson.asObject(parsed).get("assets") == null) {
                    issues.add("TextureForge spec data path does not contain assets: " + specId);
                }
            }
        }
        if (!issues.isEmpty()) {
            throw new IllegalStateException("Native TextureForge spec registry is invalid: " + issues);
        }
    }

    private static void requireSameKeys(
            TreeSet<String> expected,
            Map<String, ?> actual,
            String label,
            List<String> issues
    ) {
        if (!expected.equals(new TreeSet<>(actual.keySet()))) {
            issues.add("TextureForge spec " + label + " keys do not match spec IDs");
        }
    }

    private static List<Path> stagedUiResourcePaths(Path root, String resourceId) {
        int separator = resourceId.indexOf(':');
        if (separator <= 0 || separator + 1 >= resourceId.length()) {
            return List.of(root.resolve(resourceId.replace(":", "/")).normalize());
        }
        String namespace = resourceId.substring(0, separator);
        String valuePath = resourceId.substring(separator + 1);
        String filename = hasExtension(valuePath) ? valuePath : valuePath + ".json";
        return List.of(
                root.resolve("data").resolve(namespace).resolve(filename).normalize(),
                root.resolve("assets").resolve(namespace).resolve(filename).normalize()
        );
    }

    private static Path stagedResourcePath(Path root, String resourceId, String defaultExtension) {
        int separator = resourceId.indexOf(':');
        if (separator <= 0 || separator + 1 >= resourceId.length()) {
            return root.resolve(resourceId.replace(":", "/")).normalize();
        }
        String namespace = resourceId.substring(0, separator);
        String valuePath = resourceId.substring(separator + 1);
        String filename = hasExtension(valuePath) ? valuePath : valuePath + defaultExtension;
        return root.resolve("assets").resolve(namespace).resolve(filename).normalize();
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

    private static boolean hasExtension(String valuePath) {
        int slash = valuePath.lastIndexOf('/');
        int dot = valuePath.lastIndexOf('.');
        return dot > slash;
    }

    private static void assertRequiredResourceKinds(Map<String, List<String>> resourceIdsByKind, String runtimeName) {
        ArrayList<String> missing = new ArrayList<>();
        for (String kind : REQUIRED_RESOURCE_KINDS) {
            if (resourceIdsByKind.getOrDefault(kind, List.of()).isEmpty()) {
                missing.add(kind);
            }
        }
        if (!missing.isEmpty()) {
            throw new IllegalStateException(runtimeName + " registry content missing audited resource kinds: " + missing);
        }
    }

    private static void assertRegisteredGameplayContent(EchoNativeRegistryRuntimeResolution resolution) {
        Map<String, List<String>> contentIds = resolution.contentIdsByRegistry();
        ArrayList<String> missing = new ArrayList<>();
        for (String registry : List.of("blocks", "items", "entities")) {
            if (contentIds.getOrDefault(registry, List.of()).isEmpty()) {
                missing.add(registry);
            }
        }
        if (!missing.isEmpty()) {
            throw new IllegalStateException("Native registry content did not register gameplay content: " + missing);
        }
        int contentIdCount = contentIds.values().stream().mapToInt(List::size).sum();
        if (contentIdCount != resolution.registeredContentCount()) {
            throw new IllegalStateException("Native registry content count mismatch: contentIds="
                    + contentIdCount + " registered=" + resolution.registeredContentCount());
        }
    }

    private static void assertContentTagMembership(EchoNativeRegistryRuntimeResolution resolution) {
        TreeSet<String> registered = registeredGameplayIds(resolution);
        ArrayList<String> issues = new ArrayList<>();
        if (!registered.equals(new TreeSet<>(resolution.contentTagIdsByContentId().keySet()))) {
            issues.add("content tag membership keys do not match registered gameplay IDs");
        }
        boolean hasMembership = false;
        for (Map.Entry<String, List<String>> entry : resolution.contentTagIdsByContentId().entrySet()) {
            for (String tagId : entry.getValue()) {
                List<String> tagValues = resolution.tagsById().get(tagId);
                if (tagValues == null) {
                    issues.add("content tag membership uses unknown tag: " + entry.getKey() + " -> " + tagId);
                } else if (!tagValues.contains(entry.getKey())) {
                    issues.add("content tag membership missing tag value: " + entry.getKey() + " -> " + tagId);
                } else {
                    hasMembership = true;
                }
            }
        }
        if (!hasMembership) {
            issues.add("content tag membership has no tagged gameplay content");
        }
        if (!issues.isEmpty()) {
            throw new IllegalStateException("Native content tag runtime membership is invalid: " + issues);
        }
    }

    private static void assertTagValueLookup(EchoNativeRegistryRuntimeResolution resolution) {
        ArrayList<String> issues = new ArrayList<>();
        if (!reverseLookup(resolution.tagsById()).equals(resolution.tagIdsByValueId())) {
            issues.add("tag value lookup does not match tag values");
        }
        for (Map.Entry<String, List<String>> entry : resolution.tagIdsByValueId().entrySet()) {
            String valueId = entry.getKey();
            if (entry.getValue().isEmpty()) {
                issues.add("tag value lookup has no tags: " + valueId);
            }
            for (String tagId : entry.getValue()) {
                if (!resolution.tagsById().getOrDefault(tagId, List.of()).contains(valueId)) {
                    issues.add("tag value lookup entry is unresolved: " + valueId + " -> " + tagId);
                }
            }
        }
        if (!issues.isEmpty()) {
            throw new IllegalStateException("Native tag value lookup is invalid: " + issues);
        }
    }

    private static void assertTagDataPaths(EchoNativeRegistryRuntimeResolution resolution) {
        Path root = materializedResourceRoot();
        ArrayList<String> issues = new ArrayList<>();
        if (!new TreeSet<>(resolution.tagsById().keySet())
                .equals(new TreeSet<>(resolution.tagDataPathsById().keySet()))) {
            issues.add("tag data path keys do not match runtime tags");
        }
        for (Map.Entry<String, List<String>> entry : resolution.tagDataPathsById().entrySet()) {
            TreeSet<String> mergedValues = new TreeSet<>();
            if (resolution.tagMergedSourceCountsById().getOrDefault(entry.getKey(), 0) < entry.getValue().size()) {
                issues.add("tag data paths exceed merged source count: " + entry.getKey());
            }
            for (String relativePath : entry.getValue()) {
                Path resolved = root.resolve(relativePath).normalize();
                if (!Files.isRegularFile(resolved)) {
                    issues.add("tag data path does not resolve: " + entry.getKey() + " -> " + relativePath);
                    continue;
                }
                if (!resolved.getFileName().toString().endsWith(".json")) {
                    issues.add("tag data path is not JSON: " + entry.getKey() + " -> " + relativePath);
                    continue;
                }
                mergedValues.addAll(extractIds(parseJsonFile(resolved)));
            }
            if (!new ArrayList<>(mergedValues).equals(resolution.tagsById().getOrDefault(entry.getKey(), List.of()))) {
                issues.add("tag values mismatch from data paths: " + entry.getKey());
            }
        }
        if (!issues.isEmpty()) {
            throw new IllegalStateException("Native tag data paths are invalid: " + issues);
        }
    }

    private static void assertSearchAndCreativeRuntimeViews(EchoNativeRegistryRuntimeResolution resolution) {
        TreeSet<String> registered = registeredGameplayIds(resolution);
        TreeSet<String> visible = new TreeSet<>(resolution.searchVisibleContentIds());
        ArrayList<String> issues = new ArrayList<>();
        if (resolution.searchVisibleContentCount() != resolution.searchVisibleContentIds().size()) {
            issues.add("search visible count field does not match runtime list");
        }
        if (!visible.equals(new TreeSet<>(resolution.searchIndexTermsByContentId().keySet()))) {
            issues.add("search index keys do not match visible content");
        }
        if (!reverseLookup(resolution.searchIndexTermsByContentId()).equals(resolution.searchContentIdsByTerm())) {
            issues.add("search term content lookup does not match search index terms");
        }
        for (String contentId : resolution.searchVisibleContentIds()) {
            if (!registered.contains(contentId)) {
                issues.add("search visible content is not registered: " + contentId);
            }
            List<String> runtimeTerms = resolution.searchIndexTermsByContentId().getOrDefault(contentId, List.of());
            List<String> expectedTerms = searchTerms(contentId, resolution);
            if (runtimeTerms.isEmpty()) {
                issues.add("search index entry has no terms: " + contentId);
            } else if (!runtimeTerms.equals(expectedTerms)) {
                issues.add("search index terms mismatch: " + contentId);
            }
        }
        for (Map.Entry<String, List<String>> entry : resolution.searchContentIdsByTerm().entrySet()) {
            if (entry.getKey().isBlank()) {
                issues.add("search term lookup contains blank term");
            }
            if (entry.getValue().isEmpty()) {
                issues.add("search term lookup has no visible content: " + entry.getKey());
            }
            for (String contentId : entry.getValue()) {
                if (!visible.contains(contentId)) {
                    issues.add("search term lookup content is not visible: " + entry.getKey() + " -> " + contentId);
                }
                if (!resolution.searchIndexTermsByContentId().getOrDefault(contentId, List.of()).contains(entry.getKey())) {
                    issues.add("search term lookup entry is unresolved: " + entry.getKey() + " -> " + contentId);
                }
            }
        }
        for (Map.Entry<String, List<String>> group : resolution.creativeGroupsById().entrySet()) {
            if (group.getValue().isEmpty()) {
                issues.add("creative group is empty: " + group.getKey());
            }
            for (String contentId : group.getValue()) {
                if (!registered.contains(contentId)) {
                    issues.add("creative group entry is not registered: " + group.getKey() + " -> " + contentId);
                }
                if (!visible.contains(contentId)) {
                    issues.add("creative group entry is not search visible: " + group.getKey() + " -> " + contentId);
                }
            }
        }
        if (!reverseLookup(resolution.creativeGroupsById()).equals(resolution.creativeGroupIdsByContentId())) {
            issues.add("creative group membership lookup does not match creative groups");
        }
        for (Map.Entry<String, List<String>> entry : resolution.creativeGroupIdsByContentId().entrySet()) {
            String contentId = entry.getKey();
            if (entry.getValue().isEmpty()) {
                issues.add("creative group membership has no groups: " + contentId);
            }
            if (!registered.contains(contentId)) {
                issues.add("creative group membership content is not registered: " + contentId);
            }
            if (!visible.contains(contentId)) {
                issues.add("creative group membership content is not search visible: " + contentId);
            }
            for (String groupId : entry.getValue()) {
                if (!resolution.creativeGroupsById().getOrDefault(groupId, List.of()).contains(contentId)) {
                    issues.add("creative group membership entry is unresolved: " + contentId + " -> " + groupId);
                }
            }
        }
        if (!issues.isEmpty()) {
            throw new IllegalStateException("Native search and creative runtime views are invalid: " + issues);
        }
    }

    private static List<String> searchTerms(String contentId, EchoNativeRegistryRuntimeResolution resolution) {
        TreeSet<String> terms = new TreeSet<>();
        addSearchTerms(terms, contentId);
        addSearchTerms(terms, resolution.langValuesByContentId().getOrDefault(contentId, ""));
        for (String tagId : resolution.contentTagIdsByContentId().getOrDefault(contentId, List.of())) {
            addSearchTerms(terms, tagId);
        }
        return List.copyOf(terms);
    }

    private static void addSearchTerms(TreeSet<String> terms, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        for (String term : value.toLowerCase(java.util.Locale.ROOT).split("[^a-z0-9]+")) {
            if (!term.isBlank()) {
                terms.add(term);
            }
        }
    }

    private static TreeSet<String> registeredGameplayIds(EchoNativeRegistryRuntimeResolution resolution) {
        TreeSet<String> ids = new TreeSet<>();
        ids.addAll(resolution.contentIdsByRegistry().getOrDefault("blocks", List.of()));
        ids.addAll(resolution.contentIdsByRegistry().getOrDefault("items", List.of()));
        ids.addAll(resolution.contentIdsByRegistry().getOrDefault("entities", List.of()));
        return ids;
    }

    private static void assertDuplicateAndTagOverlayState(EchoNativeRegistryRuntimeResolution resolution) {
        if (!resolution.blockingDuplicateResourceIds().isEmpty()) {
            throw new IllegalStateException("Native registry content has blocking duplicate resources: "
                    + resolution.blockingDuplicateResourceIds());
        }
        ArrayList<String> overlayIds = new ArrayList<>();
        int computedOverlayCount = 0;
        for (Map.Entry<String, Integer> entry : resolution.tagMergedSourceCountsById().entrySet()) {
            if (entry.getValue() > 1) {
                overlayIds.add(entry.getKey());
                computedOverlayCount += entry.getValue() - 1;
            }
        }
        if (computedOverlayCount != resolution.mergedTagOverlayCount()) {
            throw new IllegalStateException("Native tag overlay count mismatch: computed="
                    + computedOverlayCount + " reported=" + resolution.mergedTagOverlayCount());
        }
        if (resolution.mergedTagOverlayCount() > 0 && resolution.mergeableTagOverlayIds().isEmpty()) {
            throw new IllegalStateException("Native tag overlays merged without mergeable overlay IDs");
        }
        if (!overlayIds.containsAll(resolution.mergeableTagOverlayIds())) {
            throw new IllegalStateException("Native mergeable tag overlay IDs do not match source counts: "
                    + resolution.mergeableTagOverlayIds());
        }
    }

    private static long regularFileCount(Path root) {
        if (!Files.exists(root)) {
            return 0L;
        }
        try (var paths = Files.walk(root)) {
            return paths.filter(Files::isRegularFile).count();
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to count materialized native resource files", exception);
        }
    }

    private static Map<String, String> stagedResourceContentFingerprints(Path root) {
        Map<String, String> fingerprints = new LinkedHashMap<>();
        if (!Files.exists(root)) {
            return fingerprints;
        }
        try (var paths = Files.walk(root)) {
            List<Path> files = paths.filter(Files::isRegularFile)
                    .sorted()
                    .toList();
            for (Path path : files) {
                fingerprints.put(root.relativize(path).toString().replace("\\", "/"), fileFingerprint(path));
            }
            return fingerprints;
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to fingerprint materialized native resource files", exception);
        }
    }

    private static TreeSet<String> flattenedResourcePaths(Map<String, List<String>> resourcePathsById) {
        TreeSet<String> paths = new TreeSet<>();
        for (List<String> values : resourcePathsById.values()) {
            paths.addAll(values);
        }
        return paths;
    }

    private static String fileFingerprint(Path path) {
        try {
            byte[] bytes = Files.readAllBytes(path);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(bytes)) + "|" + bytes.length;
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to fingerprint resource file " + path, exception);
        }
    }

    private static long regularFileByteCount(Path root) {
        if (!Files.exists(root)) {
            return 0L;
        }
        try (var paths = Files.walk(root)) {
            return paths.filter(Files::isRegularFile)
                    .mapToLong(EchoNativeAgent4RegistrySmokeMain::size)
                    .sum();
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to size materialized native resource files", exception);
        }
    }

    private static long size(Path path) {
        try {
            return Files.size(path);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to size resource file " + path, exception);
        }
    }

    private static long stagedJsonFileCount(Path root) {
        if (!Files.exists(root)) {
            return 0L;
        }
        try (var paths = Files.walk(root)) {
            return paths.filter(Files::isRegularFile)
                    .filter(EchoNativeAgent4RegistrySmokeMain::isJsonLike)
                    .count();
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to count materialized native JSON files", exception);
        }
    }

    private static long parseStagedJsonFiles(Path root) {
        if (!Files.exists(root)) {
            throw new IllegalStateException("Native materialized resource root is missing: " + root);
        }
        try (var paths = Files.walk(root)) {
            List<Path> jsonFiles = paths.filter(Files::isRegularFile)
                    .filter(EchoNativeAgent4RegistrySmokeMain::isJsonLike)
                    .sorted()
                    .toList();
            for (Path path : jsonFiles) {
                EchoNativeJson.parse(Files.readString(path));
            }
            return jsonFiles.size();
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to parse materialized native JSON resources under " + root, exception);
        }
    }

    private static Map<String, List<String>> stagedJsonContentFingerprintsById(
            Path root,
            Map<String, List<String>> resourcePathsById
    ) {
        Map<String, List<String>> fingerprints = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> entry : resourcePathsById.entrySet()) {
            ArrayList<String> values = new ArrayList<>();
            for (String relativePath : entry.getValue()) {
                if (!relativePath.endsWith(".json")) {
                    continue;
                }
                Path path = root.resolve(relativePath).normalize();
                if (!path.startsWith(root) || !Files.isRegularFile(path)) {
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
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to fingerprint parsed JSON resource " + path, exception);
        }
    }

    private static boolean isJsonLike(Path path) {
        String normalized = path.toString().replace("\\", "/");
        return normalized.endsWith(".json") || normalized.endsWith(".mcmeta");
    }

    private static boolean isLangJson(Path path) {
        String normalized = path.toString().replace("\\", "/");
        return normalized.contains("/assets/") && normalized.contains("/lang/") && normalized.endsWith(".json");
    }

    private static String resourceManifestFingerprint(EchoNativeRegistryRuntimeResolution resolution) {
        ArrayList<String> entries = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : resolution.resourceFileFingerprintsByKind().entrySet()) {
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
}
