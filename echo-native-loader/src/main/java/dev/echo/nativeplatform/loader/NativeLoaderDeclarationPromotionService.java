package dev.echo.nativeplatform.loader;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class NativeLoaderDeclarationPromotionService {
    private NativeLoaderDeclarationPromotionService() {
    }

    public static Map<String, Object> promoteDeclarations(
            Map<String, Object> runtimeBridge,
            Map<String, Map<String, Object>> nativeActivations,
            RegistryBridgeApplier registryBridgeApplier,
            String packId,
            List<String> modules
    ) {
        Map<String, Object> enriched = promoteRegistryDeclarations(
                runtimeBridge,
                nativeActivations,
                registryBridgeApplier,
                packId,
                modules
        );
        enriched = promoteResourceDeclarations(enriched, nativeActivations);
        enriched = promoteClientUiDeclarations(enriched, nativeActivations);
        return Map.copyOf(enriched);
    }

    public interface RegistryBridgeApplier {
        Map<String, Object> apply(
                String packId,
                List<String> modules,
                List<String> sdkItemIds,
                List<String> sdkBlockIds,
                List<String> sdkCreativeTabIds,
                List<Map<String, Object>> sdkCreativeTabDeclarations,
                List<Map<String, Object>> sdkAllRegistryDeclarations
        );
    }

    private static Map<String, Object> promoteRegistryDeclarations(
            Map<String, Object> runtimeBridge,
            Map<String, Map<String, Object>> nativeActivations,
            RegistryBridgeApplier registryBridgeApplier,
            String packId,
            List<String> modules
    ) {
        Map<String, Object> sdkRegistryDeclarations = aggregateSdkRegistryDeclarations(nativeActivations);
        List<String> sdkItemIds = stringList(sdkRegistryDeclarations.get("itemIds"));
        List<String> sdkBlockIds = stringList(sdkRegistryDeclarations.get("blockIds"));
        List<String> sdkCreativeTabIds = stringList(sdkRegistryDeclarations.get("creativeTabIds"));
        List<Map<String, Object>> sdkCreativeTabDeclarations =
                objectList(sdkRegistryDeclarations.get("creativeTabDeclarations"));
        List<Map<String, Object>> sdkAllRegistryDeclarations =
                objectList(sdkRegistryDeclarations.get("declarations"));
        Map<String, Object> enriched = new LinkedHashMap<>(runtimeBridge);
        if (!sdkItemIds.isEmpty()
                || !sdkBlockIds.isEmpty()
                || !sdkCreativeTabIds.isEmpty()
                || !sdkAllRegistryDeclarations.isEmpty()) {
            Map<String, Object> registryBridge = new LinkedHashMap<>(registryBridgeApplier.apply(
                    packId,
                    modules,
                    sdkItemIds,
                    sdkBlockIds,
                    sdkCreativeTabIds,
                    sdkCreativeTabDeclarations,
                    sdkAllRegistryDeclarations
            ));
            registryBridge.put("sdkRegistryDeclarations", sdkRegistryDeclarations);
            registryBridge.put("registryRebuiltAfterNativeActivation", true);
            enriched.put("registryBridge", Map.copyOf(registryBridge));
            enriched.putAll(NativeLoaderAdapterCoreMarkerFields.withContentBridgeActive(enriched));
        } else {
            enriched.put("sdkRegistryDeclarations", sdkRegistryDeclarations);
        }
        return Map.copyOf(enriched);
    }

    private static Map<String, Object> promoteResourceDeclarations(
            Map<String, Object> runtimeBridge,
            Map<String, Map<String, Object>> nativeActivations
    ) {
        Map<String, Object> sdkResourceDeclarations = aggregateSdkResourceDeclarations(nativeActivations);
        Map<String, Object> resourceBridge = new LinkedHashMap<>(object(runtimeBridge.get("resourceBridge")));
        resourceBridge.put("sdkResourceDeclarations", sdkResourceDeclarations);
        resourceBridge.put("sdkResourceDeclarationCount",
                integer(sdkResourceDeclarations.get("declarationCount")));
        resourceBridge.put("sdkWorldStartupResourceCount",
                integer(sdkResourceDeclarations.get("worldStartupResourceCount")));
        resourceBridge.put("sdkResourceIds",
                sdkResourceDeclarations.getOrDefault("resourceIds", List.of()));
        resourceBridge.put("sdkResourceTypes",
                sdkResourceDeclarations.getOrDefault("resourceTypes", List.of()));
        resourceBridge.put("worldStartupResourcesPromoted",
                Boolean.TRUE.equals(sdkResourceDeclarations.get("worldStartupResourcesPromoted")));
        resourceBridge.put("nativeResourceHostRequiredByModuleDeclarations",
                Boolean.TRUE.equals(sdkResourceDeclarations.get("nativeResourceHostRequired")));
        resourceBridge.put("applied",
                Boolean.TRUE.equals(resourceBridge.get("applied"))
                        || Boolean.TRUE.equals(sdkResourceDeclarations.get("applied")));
        Map<String, Object> enriched = new LinkedHashMap<>(runtimeBridge);
        enriched.put("resourceBridge", Map.copyOf(resourceBridge));
        return Map.copyOf(enriched);
    }

    private static Map<String, Object> promoteClientUiDeclarations(
            Map<String, Object> runtimeBridge,
            Map<String, Map<String, Object>> nativeActivations
    ) {
        Map<String, Object> sdkClientUiDeclarations = aggregateSdkClientUiDeclarations(nativeActivations);
        Map<String, Object> nativeClientUiBridge = new LinkedHashMap<>(object(runtimeBridge.get("nativeClientUiBridge")));
        nativeClientUiBridge.put("sdkClientUiDeclarations", sdkClientUiDeclarations);
        nativeClientUiBridge.put("moduleDeclaredClientSurfaceCount",
                integer(sdkClientUiDeclarations.get("declarationCount")));
        nativeClientUiBridge.put("moduleDeclaredClientSurfaceIds",
                sdkClientUiDeclarations.getOrDefault("surfaceIds", List.of()));
        nativeClientUiBridge.put("moduleDeclaredClientSurfaceTypes",
                sdkClientUiDeclarations.getOrDefault("surfaceTypes", List.of()));
        List<String> expectedClientSurfaceTypes =
                normalizedStringList(nativeClientUiBridge.get("profileExpectedClientSurfaceTypes"));
        List<String> declaredClientSurfaceTypes =
                normalizedStringList(sdkClientUiDeclarations.getOrDefault("surfaceTypes", List.of()));
        List<String> missingProfileClientSurfaceTypes = missingValues(
                expectedClientSurfaceTypes,
                declaredClientSurfaceTypes
        );
        nativeClientUiBridge.put("profileExpectedClientSurfaceTypes", expectedClientSurfaceTypes);
        nativeClientUiBridge.put("profileDeclaredClientSurfaceTypes", declaredClientSurfaceTypes);
        nativeClientUiBridge.put("profileMissingClientSurfaceTypes", missingProfileClientSurfaceTypes);
        nativeClientUiBridge.put("profileExtraClientSurfaceTypes", extraValues(
                declaredClientSurfaceTypes,
                expectedClientSurfaceTypes
        ));
        nativeClientUiBridge.put("profileClientSurfaceContractSatisfied",
                expectedClientSurfaceTypes.isEmpty() || missingProfileClientSurfaceTypes.isEmpty());
        nativeClientUiBridge.put("liveClientBridgeRequiredByModuleDeclarations",
                Boolean.TRUE.equals(sdkClientUiDeclarations.get("liveClientBridgeRequired")));
        if (Boolean.TRUE.equals(sdkClientUiDeclarations.get("applied"))) {
            nativeClientUiBridge.put("moduleDeclaredClientSurfacesPromoted", true);
        }
        Map<String, Object> enriched = new LinkedHashMap<>(runtimeBridge);
        enriched.put("nativeClientUiBridge", Map.copyOf(nativeClientUiBridge));
        return Map.copyOf(enriched);
    }

    private static Map<String, Object> aggregateSdkRegistryDeclarations(
            Map<String, Map<String, Object>> nativeActivations
    ) {
        Set<String> itemIds = new LinkedHashSet<>();
        Set<String> blockIds = new LinkedHashSet<>();
        Set<String> creativeTabIds = new LinkedHashSet<>();
        List<Map<String, Object>> declarations = new ArrayList<>();
        for (Map.Entry<String, Map<String, Object>> entry : nativeActivations.entrySet()) {
            if (!nativeActivationLoaded(entry.getValue())) {
                continue;
            }
            for (Map<String, Object> mutation : objectList(entry.getValue().get("moduleLifecycleRecords"))) {
                addRegistryMutationDeclaration(entry.getKey(), mutation, itemIds, blockIds, creativeTabIds, declarations);
            }
            for (Map<String, Object> mutation : objectList(entry.getValue().get("mutations"))) {
                addRegistryMutationDeclaration(entry.getKey(), mutation, itemIds, blockIds, creativeTabIds, declarations);
            }
            Map<String, Object> registryBridge = object(entry.getValue().get("registryBridge"));
            for (Map<String, Object> registration : objectList(registryBridge.get("registrations"))) {
                String registry = normalizeRegistry(String.valueOf(registration.getOrDefault("registry", "")));
                String id = normalizeContentId(String.valueOf(registration.getOrDefault("id", "")), entry.getKey());
                if (registry.isBlank() || id.isBlank()) {
                    continue;
                }
                Map<String, Object> declaration = new LinkedHashMap<>(registration);
                declaration.put("moduleId", entry.getKey());
                declaration.put("registry", registry);
                declaration.put("id", id);
                declarations.add(Map.copyOf(declaration));
                addRegistryId(registry, id, itemIds, blockIds, creativeTabIds);
            }
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("applied", !declarations.isEmpty());
        data.put("bridge", "echo_native.sdk_registry_declarations");
        data.put("declarationCount", declarations.size());
        data.put("itemIds", List.copyOf(itemIds));
        data.put("blockIds", List.copyOf(blockIds));
        data.put("creativeTabIds", List.copyOf(creativeTabIds));
        data.put("creativeTabDeclarations", declarations.stream()
                .filter(declaration -> "creative_tab".equals(String.valueOf(declaration.getOrDefault("registry", ""))))
                .toList());
        data.put("declarations", List.copyOf(declarations));
        data.put("summary", declarations.isEmpty()
                ? "No loaded native module SDK registry declarations were available."
                : "Loaded native module SDK registry declarations were promoted as Native Loader registry inputs.");
        return Map.copyOf(data);
    }

    private static Map<String, Object> aggregateSdkClientUiDeclarations(
            Map<String, Map<String, Object>> nativeActivations
    ) {
        Set<String> surfaceIds = new LinkedHashSet<>();
        Set<String> surfaceTypes = new LinkedHashSet<>();
        List<Map<String, Object>> declarations = new ArrayList<>();
        for (Map.Entry<String, Map<String, Object>> entry : nativeActivations.entrySet()) {
            if (!nativeActivationLoaded(entry.getValue())) {
                continue;
            }
            Map<String, Object> registryBridge = object(entry.getValue().get("registryBridge"));
            for (Map<String, Object> registration : objectList(registryBridge.get("registrations"))) {
                String registry = normalizeRegistry(String.valueOf(registration.getOrDefault("registry", "")));
                if (!isClientUiRegistry(registry)) {
                    continue;
                }
                String id = normalizeContentId(String.valueOf(registration.getOrDefault("id", "")), entry.getKey());
                if (id.isBlank()) {
                    continue;
                }
                String surfaceType = surfaceType(registry);
                Map<String, Object> declaration = new LinkedHashMap<>(registration);
                declaration.put("moduleId", entry.getKey());
                declaration.put("registry", registry);
                declaration.put("surfaceId", id);
                declaration.put("surfaceType", surfaceType);
                declaration.put("liveClientBridgeRequired", true);
                declarations.add(Map.copyOf(declaration));
                surfaceIds.add(id);
                surfaceTypes.add(surfaceType);
            }
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("applied", !declarations.isEmpty());
        data.put("bridge", "echo_native.sdk_client_ui_declarations");
        data.put("declarationCount", declarations.size());
        data.put("surfaceIds", List.copyOf(surfaceIds));
        data.put("surfaceTypes", List.copyOf(surfaceTypes));
        data.put("declarations", List.copyOf(declarations));
        data.put("liveClientBridgeRequired", !declarations.isEmpty());
        data.put("summary", declarations.isEmpty()
                ? "No loaded native module client UI declarations were available."
                : "Loaded native module client UI declarations were promoted as Native Loader client bridge inputs.");
        return Map.copyOf(data);
    }

    private static Map<String, Object> aggregateSdkResourceDeclarations(
            Map<String, Map<String, Object>> nativeActivations
    ) {
        Set<String> resourceIds = new LinkedHashSet<>();
        Set<String> resourceTypes = new LinkedHashSet<>();
        List<Map<String, Object>> declarations = new ArrayList<>();
        for (Map.Entry<String, Map<String, Object>> entry : nativeActivations.entrySet()) {
            if (!nativeActivationLoaded(entry.getValue())) {
                continue;
            }
            Map<String, Object> registryBridge = object(entry.getValue().get("registryBridge"));
            for (Map<String, Object> registration : objectList(registryBridge.get("registrations"))) {
                String registry = normalizeRegistry(String.valueOf(registration.getOrDefault("registry", "")));
                if (!isResourceRegistry(registry)) {
                    continue;
                }
                String id = normalizeContentId(String.valueOf(registration.getOrDefault("id", "")), entry.getKey());
                if (id.isBlank()) {
                    continue;
                }
                Map<String, Object> declaration = new LinkedHashMap<>(registration);
                declaration.put("moduleId", entry.getKey());
                declaration.put("registry", registry);
                declaration.put("resourceId", id);
                declaration.put("resourceType", resourceType(registry));
                declaration.put("nativeResourceHostRequired", true);
                declaration.put("worldStartupInput", isWorldStartupResource(registry));
                declarations.add(Map.copyOf(declaration));
                resourceIds.add(id);
                resourceTypes.add(resourceType(registry));
            }
        }
        long worldStartupResourceCount = declarations.stream()
                .filter(declaration -> Boolean.TRUE.equals(declaration.get("worldStartupInput")))
                .count();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("applied", !declarations.isEmpty());
        data.put("bridge", "echo_native.sdk_resource_declarations");
        data.put("declarationCount", declarations.size());
        data.put("worldStartupResourceCount", worldStartupResourceCount);
        data.put("resourceIds", List.copyOf(resourceIds));
        data.put("resourceTypes", List.copyOf(resourceTypes));
        data.put("declarations", List.copyOf(declarations));
        data.put("nativeResourceHostRequired", !declarations.isEmpty());
        data.put("worldStartupResourcesPromoted", worldStartupResourceCount > 0);
        data.put("summary", declarations.isEmpty()
                ? "No loaded native module SDK resource declarations were available."
                : "Loaded native module SDK resource declarations were promoted as Native Loader resource inputs.");
        return Map.copyOf(data);
    }

    private static void addRegistryMutationDeclaration(
            String moduleId,
            Map<String, Object> mutation,
            Set<String> itemIds,
            Set<String> blockIds,
            Set<String> creativeTabIds,
            List<Map<String, Object>> declarations
    ) {
        if (!"registry".equals(String.valueOf(mutation.getOrDefault("surface", "")))) {
            return;
        }
        if (!String.valueOf(mutation.getOrDefault("action", "")).contains("native_registry_host_registered")) {
            return;
        }
        if (!"MUTATED".equals(String.valueOf(mutation.getOrDefault("status", "")))) {
            return;
        }
        String target = String.valueOf(mutation.getOrDefault("target", ""));
        int separator = target.indexOf(':');
        if (separator < 1 || separator + 1 >= target.length()) {
            return;
        }
        String registry = normalizeRegistry(target.substring(0, separator));
        String id = normalizeContentId(target.substring(separator + 1), moduleId);
        if (registry.isBlank() || id.isBlank()) {
            return;
        }
        Map<String, Object> declaration = new LinkedHashMap<>();
        declaration.put("moduleId", moduleId);
        declaration.put("registry", registry);
        declaration.put("id", id);
        declaration.put("source", "module_lifecycle_registry_mutation");
        declaration.put("status", "MUTATED");
        declarations.add(Map.copyOf(declaration));
        addRegistryId(registry, id, itemIds, blockIds, creativeTabIds);
    }

    private static void addRegistryId(
            String registry,
            String id,
            Set<String> itemIds,
            Set<String> blockIds,
            Set<String> creativeTabIds
    ) {
        switch (registry) {
            case "item" -> itemIds.add(id);
            case "block" -> blockIds.add(id);
            case "creative_tab" -> creativeTabIds.add(id);
            default -> {
                // Other native registry surfaces are tracked but are not vanilla item/block registry inputs yet.
            }
        }
    }

    private static boolean nativeActivationLoaded(Map<String, Object> activation) {
        return activation != null
                && Boolean.TRUE.equals(activation.get("activated"))
                && Boolean.TRUE.equals(activation.get("nativeAdapterCodeExecuted"))
                && !String.valueOf(activation.getOrDefault("entrypoint", "")).isBlank()
                && !String.valueOf(activation.getOrDefault("loadedClassName", "")).isBlank();
    }

    private static List<String> stringList(Object value) {
        if (!(value instanceof Iterable<?> iterable)) {
            return List.of();
        }
        List<String> list = new ArrayList<>();
        for (Object item : iterable) {
            if (item != null) {
                list.add(String.valueOf(item));
            }
        }
        return List.copyOf(list);
    }

    private static List<String> normalizedStringList(Object value) {
        List<String> result = new ArrayList<>();
        for (String item : stringList(value)) {
            String normalized = normalizeSurfaceType(item);
            if (!normalized.isBlank() && !result.contains(normalized)) {
                result.add(normalized);
            }
        }
        return List.copyOf(result);
    }

    private static List<String> missingValues(List<String> expected, List<String> actual) {
        List<String> missing = new ArrayList<>();
        for (String value : expected == null ? List.<String>of() : expected) {
            if (!actual.contains(value)) {
                missing.add(value);
            }
        }
        return List.copyOf(missing);
    }

    private static List<String> extraValues(List<String> actual, List<String> expected) {
        List<String> extra = new ArrayList<>();
        for (String value : actual == null ? List.<String>of() : actual) {
            if (!expected.contains(value)) {
                extra.add(value);
            }
        }
        return List.copyOf(extra);
    }

    private static String normalizeRegistry(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase().replace('-', '_').replace('.', '_');
        return switch (normalized) {
            case "items" -> "item";
            case "blocks" -> "block";
            case "entities" -> "entity";
            case "blockentity", "blockentities", "block_entity", "block_entities" -> "block_entity";
            case "menus" -> "menu";
            case "sounds" -> "sound";
            case "particles", "particle_profile", "particle_profiles" -> "particle";
            case "effects", "mob_effect", "mob_effects", "mobeffect", "mobeffects" -> "effect";
            case "commands" -> "command";
            case "datacomponent", "datacomponents", "data_component", "data_components" -> "data_component";
            case "recipes" -> "recipe";
            case "creativegroup", "creativegroups", "creative_group", "creative_groups",
                    "creative_tab", "creative_tabs" -> "creative_tab";
            case "biomes" -> "biome";
            case "configured_feature", "configured_features", "placed_feature", "placed_features",
                    "world_generator", "world_generators", "worldgens" -> "worldgen";
            case "asset", "assets", "clientasset", "clientassets", "client_asset", "client_assets" -> "client_asset";
            default -> normalized;
        };
    }

    private static boolean isClientUiRegistry(String registry) {
        return switch (registry) {
            case "ui_surface", "ui_overlay", "hud", "hud_widget", "hud_layout",
                    "screen", "screen_surface", "client_overlay", "loading_screen", "main_menu",
                    "terminal", "index", "lens", "holomap", "holo_map", "minimap", "theme" -> true;
            default -> false;
        };
    }

    private static boolean isResourceRegistry(String registry) {
        return switch (registry) {
            case "resource", "resources", "resource_profile", "resource_pack", "resourcepack",
                    "data", "data_pack", "datapack", "recipe", "recipes", "loot", "loot_table",
                    "loot_tables", "loottables", "tag", "tags", "sound", "sounds", "structure",
                    "structures", "worldgen", "world_generator", "world_preset", "world_template",
                    "asset", "assets", "ui_screen", "ui_screens", "theme", "themes", "theme_tokens",
                    "ui_skin", "ui_skins", "render_profile", "render_profiles", "asset_kit",
                    "asset_kits", "block_palette", "block_palettes", "screen_markup", "screen_layout",
                    "screen_layouts", "style", "styles", "data_provider", "data_providers" -> true;
            default -> registry.endsWith("_resource")
                    || registry.endsWith("_resources")
                    || registry.endsWith("_data");
        };
    }

    private static String resourceType(String registry) {
        return switch (registry) {
            case "resourcepack" -> "resource_pack";
            case "datapack" -> "data_pack";
            case "loot", "loottables" -> "loot_table";
            case "recipes" -> "recipe";
            case "tags" -> "tag";
            case "sounds" -> "sound";
            case "structures" -> "structure";
            case "assets" -> "asset";
            case "themes" -> "theme";
            case "ui_skins" -> "ui_skin";
            case "render_profiles" -> "render_profile";
            case "asset_kits" -> "asset_kit";
            case "block_palettes" -> "block_palette";
            case "screen_layouts" -> "screen_layout";
            case "styles" -> "style";
            case "data_providers" -> "data_provider";
            default -> registry;
        };
    }

    private static boolean isWorldStartupResource(String registry) {
        return switch (registry) {
            case "data_pack", "datapack", "worldgen", "world_generator", "world_preset",
                    "world_template", "structure", "structures", "tag", "tags" -> true;
            default -> false;
        };
    }

    private static String surfaceType(String registry) {
        return switch (registry) {
            case "ui_surface" -> "ui_surface";
            case "ui_overlay" -> "ui_overlay";
            case "client_overlay" -> "client_overlay";
            case "hud", "hud_widget", "hud_layout" -> registry;
            case "screen", "screen_surface" -> registry;
            case "loading_screen" -> "loading_screen";
            case "main_menu" -> "main_menu";
            case "terminal" -> "terminal";
            case "index" -> "index";
            case "lens" -> "lens";
            case "holomap", "holo_map", "minimap" -> "holomap";
            case "theme" -> "theme";
            default -> "screen_surface";
        };
    }

    private static String normalizeSurfaceType(String value) {
        String text = value == null ? "" : value.trim().toLowerCase(java.util.Locale.ROOT);
        StringBuilder result = new StringBuilder();
        boolean previousSeparator = false;
        for (int index = 0; index < text.length(); index++) {
            char ch = text.charAt(index);
            if ((ch >= 'a' && ch <= 'z') || (ch >= '0' && ch <= '9')) {
                result.append(ch);
                previousSeparator = false;
            } else if (!previousSeparator) {
                result.append('_');
                previousSeparator = true;
            }
        }
        while (result.length() > 0 && result.charAt(result.length() - 1) == '_') {
            result.deleteCharAt(result.length() - 1);
        }
        return switch (result.toString()) {
            case "mainmenu" -> "main_menu";
            case "loading" -> "loading_screen";
            case "holo_map", "minimap", "mini_map" -> "holomap";
            default -> result.toString();
        };
    }

    private static String normalizeContentId(String value, String moduleId) {
        String text = value == null ? "" : value.trim().toLowerCase();
        if (text.isBlank() || text.contains(":")) {
            return text;
        }
        int separator = text.indexOf('.');
        if (separator > 0 && separator + 1 < text.length()) {
            return text.substring(0, separator) + ":" + text.substring(separator + 1).replace('.', '_');
        }
        String namespace = moduleId == null ? "" : moduleId.trim().toLowerCase();
        return namespace.isBlank() ? text.replace('.', '_') : namespace + ":" + text.replace('.', '_');
    }

    private static Map<String, Object> object(Object value) {
        if (!(value instanceof Map<?, ?> map)) {
            return Map.of();
        }
        Map<String, Object> object = new LinkedHashMap<>();
        map.forEach((key, item) -> object.put(String.valueOf(key), item));
        return Map.copyOf(object);
    }

    private static List<Map<String, Object>> objectList(Object value) {
        if (!(value instanceof Iterable<?> iterable)) {
            return List.of();
        }
        List<Map<String, Object>> objects = new ArrayList<>();
        for (Object item : iterable) {
            Map<String, Object> object = object(item);
            if (!object.isEmpty()) {
                objects.add(object);
            }
        }
        return List.copyOf(objects);
    }

    private static int integer(Object value) {
        return value instanceof Number number ? number.intValue() : 0;
    }
}
