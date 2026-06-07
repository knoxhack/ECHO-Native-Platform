package dev.echo.nativeplatform.loader;

import dev.echo.nativeplatform.contracts.EchoNativeBootstrapProductProfile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class NativeLoaderResourcePackBuilder {
    public static final String SERVICE_ID = "echo.native.resource_pack_builder";

    private NativeLoaderResourcePackBuilder() {
    }

    public record BuildResult(
            int copiedResources,
            int dataEntries,
            int productWorldgenBiomes,
            boolean productWorldgenPresetPresent,
            boolean productWorldgenRootMarkerPresent,
            boolean productOverworldSettingsPresent,
            List<String> normalizedTechBlockstates,
            List<String> safeModeGuardSkippedEntries,
            List<String> sourceBackedFallbackEntries,
            RecipeDataValidation recipeDataValidation
    ) {
    }

    public record RecipeDataValidation(
            int vanillaRecipeCount,
            int productVanillaRecipeCount,
            boolean productMachineRecipeCatalogPresent,
            int customSerializerGapCount,
            List<String> customRecipeTypes,
            List<Map<String, Object>> customSerializerGaps
    ) {
        public Map<String, Object> toMap() {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("vanillaRecipeCount", vanillaRecipeCount);
            data.put("productVanillaRecipeCount", productVanillaRecipeCount);
            data.put("productMachineRecipeCatalogPresent", productMachineRecipeCatalogPresent);
            data.put("customSerializerGapCount", customSerializerGapCount);
            data.put("customRecipeTypes", customRecipeTypes);
            data.put("customSerializerGaps", customSerializerGaps);
            data.put("summary", customSerializerGapCount == 0
                    ? "All materialized recipe JSON is backed by vanilla recipe serializers or the product native machine recipe catalog."
                    : "Materialized pack contains custom recipe/data JSON without native serializer loaders; these are recorded as file-specific gaps, not wired gameplay.");
            return data;
        }
    }

    public static BuildResult buildResourcePack(
            Path resourcePack,
            EchoNativeBootstrapProductProfile profile,
            String nativeModuleClasspathProperty
    ) throws IOException {
        Files.createDirectories(resourcePack.getParent());
        Map<String, byte[]> entries = new TreeMap<>();
        List<String> safeModeGuardSkippedEntries = new ArrayList<>();
        List<String> sourceBackedFallbackEntries = new ArrayList<>();
        entries.put("pack.mcmeta", NativeLoaderPackMetadata.resourcePackMcmeta(profile.nativeResourcePackDescription()));
        for (String item : NativeLoaderClasspathSupport.nativeContentClasspaths(nativeModuleClasspathProperty)) {
            if (item.isBlank()) {
                continue;
            }
            Path path = Path.of(item);
            if (!NativeLoaderClasspathSupport.isNativeContentClasspathCandidate(
                    path,
                    profile.nativeModuleNamespacePrefixes())
                    && !path.toString().toLowerCase(java.util.Locale.ROOT).contains("build")) {
                continue;
            }
            if (!Files.isRegularFile(path) && !Files.isDirectory(path)) {
                continue;
            }
            NativeLoaderResourceMaterializer.copyResourceEntries(
                    path,
                    entries,
                    safeModeGuardSkippedEntries,
                    profile.nativeStructureTemplateSourcePrefix(),
                    profile.nativeStructureTemplateTargetPrefix()
            );
        }
        NativeLoaderResourceMaterializer.copyRequiredProductWorldgenSourceEntries(
                entries,
                sourceBackedFallbackEntries,
                profile.nativeRequiredResourceEntries(),
                profile.nativeSourceResourceRootMarker(),
                profile.nativeStructureTemplateSourcePrefix(),
                profile.nativeStructureTemplateTargetPrefix()
        );
        NativeLoaderWorldgenDatapackPolicy.mirrorProductWorldPreset(
                entries,
                profile.nativeWorldPresetMirrorSource(),
                profile.nativeWorldPresetMirrorTarget()
        );
        NativeLoaderWorldgenDatapackPolicy.mirrorProductStructureTemplates(
                entries,
                profile.nativeStructureTemplateSourcePrefix(),
                profile.nativeStructureTemplateTargetPrefix()
        );
        NativeLoaderBiomeSpawnSanitizer.sanitizeNativeProductBiomeSpawns(
                entries,
                profile.nativeWorldgenBiomePrefix(),
                profile.namespace()
        );
        entries.put("assets/echo_native/lang/en_us.json", NativeLoaderResourceAssets.fallbackLanguageJson(
                profile.nativeItemGroupTranslationKey(),
                profile.nativeItemGroupTranslationName(),
                Map.of(),
                entries
        ).getBytes(StandardCharsets.UTF_8));
        List<String> normalizedTechBlockstates = NativeLoaderResourceAssets.normalizeTechBlockstates(
                entries,
                profile.nativeModuleNamespacePrefixes()
        );
        RecipeDataValidation recipeDataValidation = validateRecipeData(entries, profile);
        int dataEntries = (int) entries.keySet().stream().filter(name -> name.startsWith("data/")).count();
        int productWorldgenBiomes = (int) entries.keySet().stream()
                .filter(name -> !profile.nativeWorldgenBiomePrefix().isBlank()
                        && name.startsWith(profile.nativeWorldgenBiomePrefix())
                        && name.endsWith(".json"))
                .count();
        boolean productWorldgenPresetPresent =
                requiredEntryPresent(entries, profile.nativeWorldPresetMirrorSource())
                        && requiredEntryPresent(entries, profile.nativeWorldPresetMirrorTarget());
        boolean productWorldgenRootMarkerPresent =
                entries.containsKey(profile.nativeSourceResourceRootMarker());
        boolean productOverworldSettingsPresent = productOverworldSettingsPresent(entries, profile);
        try (ZipOutputStream output = new ZipOutputStream(Files.newOutputStream(resourcePack))) {
            for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
                output.putNextEntry(new ZipEntry(entry.getKey()));
                output.write(entry.getValue());
                output.closeEntry();
            }
        }
        return new BuildResult(
                Math.max(0, entries.size() - 1),
                dataEntries,
                productWorldgenBiomes,
                productWorldgenPresetPresent,
                productWorldgenRootMarkerPresent,
                productOverworldSettingsPresent,
                normalizedTechBlockstates,
                List.copyOf(safeModeGuardSkippedEntries),
                List.copyOf(sourceBackedFallbackEntries),
                recipeDataValidation
        );
    }

    private static boolean productOverworldSettingsPresent(
            Map<String, byte[]> entries,
            EchoNativeBootstrapProductProfile profile
    ) {
        String explicitEntry = profile.nativeSaveDatapackRequiredEntriesByValidationKey()
                .getOrDefault("overworldNoiseSettingsPresent", "");
        if (!explicitEntry.isBlank()) {
            return entries.containsKey(explicitEntry);
        }
        return profile.nativeRequiredResourceEntries().stream()
                .filter(entry -> entry != null
                        && entry.toLowerCase(java.util.Locale.ROOT).contains("/worldgen/noise_settings/")
                        && entry.toLowerCase(java.util.Locale.ROOT).contains("overworld"))
                .findFirst()
                .map(entries::containsKey)
                .orElse(false);
    }

    private static RecipeDataValidation validateRecipeData(
            Map<String, byte[]> entries,
            EchoNativeBootstrapProductProfile profile
    ) {
        int vanillaRecipeCount = 0;
        int productVanillaRecipeCount = 0;
        Set<String> customRecipeTypes = new TreeSet<>();
        List<Map<String, Object>> customSerializerGaps = new ArrayList<>();
        boolean productMachineRecipeCatalogPresent =
                !profile.nativeMachineRecipeCatalogPath().isBlank()
                        && entries.containsKey(profile.nativeMachineRecipeCatalogPath());
        for (String name : entries.keySet()) {
            if (!isRecipeLikeDataEntry(name)) {
                continue;
            }
            String type = extractJsonStringField(entries.get(name), "type");
            String namespace = dataNamespace(name);
            if (type.isBlank()) {
                customSerializerGaps.add(recipeSerializerGap(name, namespace, "<missing>",
                        "custom data loader gap: recipe-like JSON has no vanilla recipe type field", profile));
                continue;
            }
            if (isVanillaRecipeType(type)) {
                vanillaRecipeCount++;
                if (profile.namespace().equals(namespace)) {
                    productVanillaRecipeCount++;
                }
                continue;
            }
            customRecipeTypes.add(type);
            customSerializerGaps.add(recipeSerializerGap(name, namespace, type,
                    "missing native RecipeSerializer/DataLoader for custom recipe type", profile));
        }
        return new RecipeDataValidation(
                vanillaRecipeCount,
                productVanillaRecipeCount,
                productMachineRecipeCatalogPresent,
                customSerializerGaps.size(),
                List.copyOf(customRecipeTypes),
                customSerializerGaps.stream().map(Map::copyOf).toList()
        );
    }

    private static boolean isRecipeLikeDataEntry(String name) {
        return name.startsWith("data/")
                && name.endsWith(".json")
                && (name.contains("/recipe/") || name.contains("/recipes/") || name.contains("/station_recipes/"));
    }

    private static String dataNamespace(String name) {
        if (!name.startsWith("data/")) {
            return "";
        }
        int namespaceEnd = name.indexOf('/', "data/".length());
        return namespaceEnd < 0 ? "" : name.substring("data/".length(), namespaceEnd);
    }

    private static String productDataPath(String path, EchoNativeBootstrapProductProfile profile) {
        if (path == null || path.isBlank()) {
            return "data/" + profile.namespace() + "/";
        }
        return "data/" + profile.namespace() + "/" + path;
    }

    private static Map<String, Object> recipeSerializerGap(
            String path,
            String namespace,
            String type,
            String reason,
            EchoNativeBootstrapProductProfile profile
    ) {
        String family = recipeDataFamily(path, type);
        Map<String, Object> gap = new LinkedHashMap<>();
        gap.put("path", path);
        gap.put("namespace", namespace);
        gap.put("type", type);
        gap.put("gap", reason);
        gap.put("dataFamily", family);
        gap.put("specificFileGap", path);
        gap.put("coveredByNativeMachineRecipeCatalog", coveredByNativeMachineRecipeCatalog(path, type, profile));
        gap.put("nativeLoaderRequired", true);
        gap.put("nativeLoaderPresent", false);
        gap.put("nativeLoaderGap", nativeRecipeLoaderGap(namespace, type, family, profile));
        gap.put("neoforgeSerializerSource", neoForgeRecipeSerializerSource(namespace, type));
        gap.put("nativeBehaviorContractSource", recipeBehaviorContractSource(namespace, type, family, path, profile));
        gap.put("remediation", recipeSerializerRemediation(namespace, type, family));
        return gap;
    }

    private static boolean coveredByNativeMachineRecipeCatalog(
            String path,
            String type,
            EchoNativeBootstrapProductProfile profile
    ) {
        String safePath = path == null ? "" : path;
        return safePath.startsWith(productDataPath("recipe/", profile))
                && profile.nativeMachineRecipeCatalogTypes().contains(type);
    }

    private static String recipeDataFamily(String path, String type) {
        String safePath = lowerContentId(path);
        String safeType = lowerContentId(type);
        if (safePath.contains("/station_recipes/")) {
            return "station_recipes";
        }
        if (safeType.endsWith(":industrial_processing")) {
            return "industrial_processing";
        }
        if (safeType.endsWith(":convoy_station_processing")) {
            return "convoy_station_processing";
        }
        if (safeType.endsWith(":orbital_processing")) {
            return "orbital_processing";
        }
        if (safeType.endsWith(":nexus_processing")) {
            return "nexus_processing";
        }
        if (safeType.endsWith(":blackbox_processing")) {
            return "blackbox_processing";
        }
        return "custom_recipe_data";
    }

    private static String nativeRecipeLoaderGap(
            String namespace,
            String type,
            String family,
            EchoNativeBootstrapProductProfile profile
    ) {
        String safeNamespace = lowerContentId(namespace);
        String safeFamily = lowerContentId(family);
        if (profile.namespace().equals(safeNamespace)
                && !profile.nativeMachineRecipeCatalogSourcePath().isBlank()) {
            return profile.nativeMachineRecipeCatalogSourcePath();
        }
        String packageRoot = "signalos".equals(safeNamespace)
                ? "com/knoxhack/signalos"
                : "com/knoxhack/" + safeNamespace;
        return "addons/" + safeNamespace
                + "/src/main/java/" + packageRoot
                + "/nativebridge/" + pascalCase(safeFamily) + "NativeRecipeDataLoader.java";
    }

    private static String neoForgeRecipeSerializerSource(String namespace, String type) {
        String safeNamespace = lowerContentId(namespace);
        String safeType = lowerContentId(type);
        if ("echoblackboxprotocol".equals(safeNamespace) && safeType.endsWith(":blackbox_processing")) {
            return "addons/echoblackboxprotocol/src/main/java/com/knoxhack/echoblackboxprotocol/registry/ModRecipes.java";
        }
        if ("<missing>".equals(safeType)) {
            return "";
        }
        String packageRoot = "signalos".equals(safeNamespace)
                ? "com/knoxhack/signalos"
                : "com/knoxhack/" + safeNamespace;
        return "addons/" + safeNamespace
                + "/src/main/java/" + packageRoot
                + "/registry/ModRecipes.java";
    }

    private static String recipeBehaviorContractSource(
            String namespace,
            String type,
            String family,
            String path,
            EchoNativeBootstrapProductProfile profile
    ) {
        String safeNamespace = lowerContentId(namespace);
        String safeType = lowerContentId(type);
        if (profile.namespace().equals(safeNamespace)
                && !profile.nativeRecipeBehaviorContractSourcePath().isBlank()) {
            return profile.nativeRecipeBehaviorContractSourcePath();
        }
        if ("echoblackboxprotocol".equals(safeNamespace) && safeType.endsWith(":blackbox_processing")) {
            return "addons/echoblackboxprotocol/src/main/java/com/knoxhack/echoblackboxprotocol/recipe/BlackboxProcessingRecipe.java";
        }
        if ("station_recipes".equals(family)) {
            return path;
        }
        return neoForgeRecipeSerializerSource(namespace, type);
    }

    private static String recipeSerializerRemediation(String namespace, String type, String family) {
        if ("<missing>".equals(type)) {
            return "Add a typed native data loader for " + namespace + " " + family
                    + " JSON or move the files out of recipe-like directories so they are not loaded as recipes.";
        }
        return "Implement the native recipe/data loader listed in nativeLoaderGap using the NeoForge serializer or data JSON as the behavior contract, then remove this file from unsupportedCustomRecipeSerializerGaps.";
    }

    private static boolean isVanillaRecipeType(String type) {
        return Set.of(
                "minecraft:crafting_shaped",
                "minecraft:crafting_shapeless",
                "minecraft:smelting",
                "minecraft:blasting",
                "minecraft:smoking",
                "minecraft:campfire_cooking",
                "minecraft:stonecutting",
                "minecraft:smithing_transform",
                "minecraft:smithing_trim",
                "minecraft:crafting_special_armordye",
                "minecraft:crafting_special_bannerduplicate",
                "minecraft:crafting_special_bookcloning",
                "minecraft:crafting_special_firework_rocket",
                "minecraft:crafting_special_firework_star",
                "minecraft:crafting_special_firework_star_fade",
                "minecraft:crafting_special_mapcloning",
                "minecraft:crafting_special_mapextending",
                "minecraft:crafting_special_repairitem",
                "minecraft:crafting_special_shielddecoration",
                "minecraft:crafting_special_shulkerboxcoloring",
                "minecraft:crafting_special_suspiciousstew",
                "minecraft:crafting_special_tippedarrow"
        ).contains(type);
    }

    private static String extractJsonStringField(byte[] bytes, String fieldName) {
        String json = new String(bytes, StandardCharsets.UTF_8);
        int depth = 0;
        for (int index = 0; index < json.length(); index++) {
            char c = json.charAt(index);
            if (c == '"') {
                JsonStringToken key = readJsonStringToken(json, index);
                if (key == null) {
                    return "";
                }
                if (depth == 1 && fieldName.equals(key.value())) {
                    int colon = skipJsonWhitespace(json, key.endIndex() + 1);
                    if (colon < json.length() && json.charAt(colon) == ':') {
                        int valueStart = skipJsonWhitespace(json, colon + 1);
                        if (valueStart < json.length() && json.charAt(valueStart) == '"') {
                            JsonStringToken value = readJsonStringToken(json, valueStart);
                            return value == null ? "" : value.value().trim();
                        }
                    }
                }
                index = key.endIndex();
            } else if (c == '{' || c == '[') {
                depth++;
            } else if (c == '}' || c == ']') {
                depth = Math.max(0, depth - 1);
            }
        }
        return "";
    }

    private record JsonStringToken(String value, int endIndex) {
    }

    private static JsonStringToken readJsonStringToken(String json, int quoteStart) {
        StringBuilder value = new StringBuilder();
        boolean escaped = false;
        for (int index = quoteStart + 1; index < json.length(); index++) {
            char c = json.charAt(index);
            if (escaped) {
                value.append(c);
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else if (c == '"') {
                return new JsonStringToken(value.toString(), index);
            } else {
                value.append(c);
            }
        }
        return null;
    }

    private static int skipJsonWhitespace(String json, int start) {
        int index = start;
        while (index < json.length() && Character.isWhitespace(json.charAt(index))) {
            index++;
        }
        return index;
    }

    private static boolean requiredEntryPresent(Map<String, byte[]> entries, String required) {
        return required == null || required.isBlank() || entries.containsKey(required);
    }

    private static String lowerContentId(String contentId) {
        return contentId == null ? "" : contentId.trim().toLowerCase(java.util.Locale.ROOT);
    }

    private static String pascalCase(String path) {
        StringBuilder builder = new StringBuilder();
        for (String part : lowerContentId(path).split("[^a-z0-9]+")) {
            if (part.isBlank()) {
                continue;
            }
            builder.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                builder.append(part.substring(1));
            }
        }
        return builder.isEmpty() ? "Custom" : builder.toString();
    }
}
