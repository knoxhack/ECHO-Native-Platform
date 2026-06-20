package dev.echo.nativeplatform.loader;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class NativeLoaderResourceSanitizer {
    public static final String SERVICE_ID = "echo.native.resource_sanitizer";

    private NativeLoaderResourceSanitizer() {
    }

    public static byte[] sanitizeProductDatapackEntry(
            String name,
            byte[] bytes,
            String namespace,
            String worldgenStructurePrefix
    ) {
        if (bytes == null || name == null || !name.endsWith(".json")) {
            return bytes;
        }
        String json = new String(bytes, StandardCharsets.UTF_8);
        String safeWorldgenStructurePrefix = worldgenStructurePrefix == null ? "" : worldgenStructurePrefix;
        if (!safeWorldgenStructurePrefix.isBlank() && name.startsWith(safeWorldgenStructurePrefix)) {
            json = json.replaceAll("(?m)^\\s*\"adapt_noise\"\\s*:\\s*true\\s*,?\\s*\\R", "");
        }
        String safeNamespace = namespace == null ? "" : namespace;
        if (name.startsWith("data/" + safeNamespace + "/worldgen/")
                || name.startsWith("data/" + safeNamespace + "/tags/block/")
                || name.startsWith("data/minecraft/worldgen/")
                || name.startsWith("data/minecraft/tags/block/")) {
            json = sanitizeAshfallNativeWorldgenFeatureReferences(sanitizeNativeRegistryUnsafeJson(name, json));
        }
        if (name.startsWith("data/" + safeNamespace + "/tags/block/")
                || name.startsWith("data/minecraft/tags/block/")) {
            json = sanitizeMinecraftBlockTagJson(name, json);
        }
        return json.getBytes(StandardCharsets.UTF_8);
    }

    static String sanitizeAshfallNativeWorldgenFeatureReferences(String json) {
        if (json == null) {
            return "";
        }
        Pattern featurePattern = Pattern.compile("(\"feature\"\\s*:\\s*\")minecraft:([a-z0-9_]+)(\")");
        Matcher matcher = featurePattern.matcher(json);
        return matcher.replaceAll(matchResult -> {
            String featureId = matchResult.group(2);
            String replacement = rewriteAshfallLegacyFeatureId(featureId);
            if (replacement == null || replacement.isBlank()) {
                return matchResult.group();
            }
            return Matcher.quoteReplacement(matchResult.group(1) + replacement + matchResult.group(3));
        });
    }

    private static String rewriteAshfallLegacyFeatureId(String featureId) {
        if (featureId == null || featureId.isBlank()) {
            return "minecraft:" + featureId;
        }
        String featureIdReplacement = switch (featureId) {
            case "dead_bush_patch", "dead_bush" -> "echoashfallprotocol:ash_bush_patch";
            case "dead_bush_patches" -> "echoashfallprotocol:ash_bushes";
            case "dirt_patch" -> "echoashfallprotocol:contaminated_soil_patch";
            case "gravel_patch", "gravel_scatter", "gravel_dense_patch", "gravel_surface_patch" ->
                    "echoashfallprotocol:rubble_scatter";
            case "iron_bars_patch" -> "echoashfallprotocol:rusted_metal_debris_patch";
            case "iron_bars_scatter" -> "echoashfallprotocol:rusted_metal_scatter";
            case "iron_block_placement" -> "echoashfallprotocol:bio_lab_leak";
            case "iron_ore_scatter" -> "echoashfallprotocol:scrap_ore_scatter";
            case "iron_blocks" -> "echoashfallprotocol:rubble_scatter";
            case "mud_patch" -> "echoashfallprotocol:acid_mud_patch";
            case "stone_patch" -> "echoashfallprotocol:ash_layer_patch";
            case "stone_pile" -> "echoashfallprotocol:concrete_rubble_pile";
            case "stone_scatter" -> "echoashfallprotocol:ash_stone_scatter";
            case "amethyst_block_cluster", "packed_ice_cluster" -> "echoashfallprotocol:blue_ice_crystal_cluster";
            case "amethyst_block_patch", "packed_ice_scatter" -> "echoashfallprotocol:frozen_conduit_scatter";
            case "snow_patch" -> "echoashfallprotocol:scorched_ash_patch";
            case "dead_bush_block" -> "minecraft:dead_bush";
            default -> null;
        };
        if (featureIdReplacement != null) {
            return featureIdReplacement;
        }
        if (featureId.endsWith("_dense_patch")) {
            return "echoashfallprotocol:scattered_bones_dense_patch";
        }
        if (featureId.endsWith("_surface_patch")) {
            return "echoashfallprotocol:deep_ash_surface_patch";
        }
        if (featureId.endsWith("_patch")) {
            return "echoashfallprotocol:contaminated_soil_patch";
        }
        if (featureId.endsWith("_pile")) {
            return "echoashfallprotocol:concrete_rubble_pile";
        }
        if (featureId.endsWith("_scatter")) {
            return "echoashfallprotocol:rubble_scatter";
        }
        if (featureId.endsWith("_cluster")) {
            return "echoashfallprotocol:blue_ice_crystal_cluster";
        }
        if (featureId.endsWith("_placement")) {
            return "echoashfallprotocol:bio_lab_leak";
        }
        return "minecraft:" + featureId;
    }

    public static String sanitizeNativeRegistryUnsafeJson(String json) {
        return sanitizeNativeRegistryUnsafeJson("", json);
    }

    public static String sanitizeNativeRegistryUnsafeJson(String name, String json) {
        if (json == null) {
            return "";
        }
        String sanitized = json
                .replace("echoblockworks:ashstone_raw_stairs", "minecraft:stone_stairs")
                .replace("echoblockworks:ashstone_debris_stairs", "minecraft:stone_stairs")
                .replace("echoblockworks:ashstone_brick_stairs", "minecraft:stone_brick_stairs")
                .replace("echoblockworks:ashstone_smooth_stairs", "minecraft:stone_stairs")
                .replace("echoblockworks:ashstone_raw_slab", "minecraft:stone_slab")
                .replace("echoblockworks:ashstone_debris_slab", "minecraft:stone_slab")
                .replace("echoblockworks:reclamation_glass_hydroponic_panel", "minecraft:green_stained_glass")
                .replace("echoblockworks:reclamation_glass_framed_glass", "minecraft:glass")
                .replace("echoblockworks:reinforced_metal_grate", "minecraft:iron_bars")
                .replace("echoblockworks:hanging_wire", "minecraft:iron_bars")
                .replace("minecraft:chain", "minecraft:iron_bars")
                .replace("echoblockworks:rubble_pile", "minecraft:gravel")
                .replace("echoblockworks:orbital_hull_white_hull", "minecraft:iron_block")
                .replace("echoblockworks:blackbox_vault_archive_panel", "minecraft:chiseled_deepslate")
                .replace("echoblockworks:blackbox_vault_dark_alloy", "minecraft:deepslate_tiles")
                .replace("echoblockworks:rusted_metal_hazard_stripe", "minecraft:yellow_concrete")
                .replace("echoblockworks:charred_concrete_warning_stripe", "minecraft:black_concrete")
                .replace("echoblockworks:rusted_metal_dark_plate", "minecraft:deepslate")
                .replace("echoblockworks:rusted_metal_panel", "minecraft:iron_block")
                .replace("echoblockworks:charred_concrete_smooth", "minecraft:blackstone")
                .replace("echoblockworks:ashstone_debris", "minecraft:gravel")
                .replace("echoblockworks:ashstone_brick", "minecraft:stone_bricks")
                .replace("echoblockworks:ashstone_smooth", "minecraft:smooth_stone")
                .replace("echoblockworks:ashstone_raw", "minecraft:stone")
                .replace("#minecraft:iron_barss", "minecraft:iron_bars")
                .replace("minecraft:iron_barss", "minecraft:iron_bars")
                .replace("minecraft:iron_bars_command_block", "minecraft:iron_bars");
        sanitized = sanitizeBlockworksRegistryReferences(sanitized);
        if (isBlockTagJson(name)) {
            sanitized = sanitizeEchoNamespaceBlockTagReferences(sanitized);
        }
        if (isBiomeJson(name)) {
            return sanitizeAshfallNativeBiomePlacedFeatureReferences(sanitized);
        }
        return sanitizeAshfallNativeWorldgenBlockReferences(sanitized);
    }

    private static boolean isBlockTagJson(String name) {
        String normalized = name == null ? "" : name.replace('\\', '/').toLowerCase(java.util.Locale.ROOT);
        return normalized.contains("/tags/block/") && normalized.endsWith(".json");
    }

    private static boolean isBiomeJson(String name) {
        String normalized = name == null ? "" : name.replace('\\', '/').toLowerCase(java.util.Locale.ROOT);
        return normalized.contains("/worldgen/biome/") && normalized.endsWith(".json");
    }

    private static String sanitizeBlockworksRegistryReferences(String json) {
        if (json == null || json.isBlank()) {
            return json == null ? "" : json;
        }
        Pattern pattern = Pattern.compile("\"#?echoblockworks:([a-z0-9_]+)\"");
        Matcher matcher = pattern.matcher(json);
        return matcher.replaceAll(matchResult ->
                Matcher.quoteReplacement("\"" + blockworksStandIn(matchResult.group(1)) + "\""));
    }

    private static String sanitizeEchoNamespaceBlockTagReferences(String json) {
        if (json == null || json.isBlank()) {
            return json == null ? "" : json;
        }
        Pattern pattern = Pattern.compile("\"#?(echo[a-z0-9_]*):([a-z0-9_./-]+)\"");
        Matcher matcher = pattern.matcher(json);
        return matcher.replaceAll(matchResult ->
                Matcher.quoteReplacement("\"" + blockTagStandIn(matchResult.group(1), matchResult.group(2)) + "\""));
    }

    private static String blockworksStandIn(String blockId) {
        String id = blockId == null ? "" : blockId;
        if (id.endsWith("_wall")) {
            return "minecraft:cobblestone_wall";
        }
        if (id.endsWith("_stairs")) {
            return "minecraft:stone_stairs";
        }
        if (id.endsWith("_slab")) {
            return "minecraft:stone_slab";
        }
        if (id.contains("glass")) {
            return "minecraft:glass";
        }
        if (id.contains("ashstone")) {
            return "minecraft:stone";
        }
        if (id.contains("charred_concrete")) {
            return "minecraft:blackstone";
        }
        if (id.contains("blackbox") || id.contains("deepslate")) {
            return "minecraft:deepslate_tiles";
        }
        if (id.contains("rusted_metal") || id.contains("reinforced_metal")
                || id.contains("orbital_hull") || id.contains("cable")) {
            return "minecraft:iron_block";
        }
        return "minecraft:stone";
    }

    private static String blockTagStandIn(String namespace, String blockId) {
        String id = blockId == null ? "" : blockId;
        if ("echoblockworks".equals(namespace)) {
            return blockworksStandIn(id);
        }
        if (id.endsWith("_wall")) {
            return "minecraft:cobblestone_wall";
        }
        if (id.endsWith("_stairs")) {
            return "minecraft:stone_stairs";
        }
        if (id.endsWith("_slab")) {
            return "minecraft:stone_slab";
        }
        if (id.contains("bush") || id.contains("grass") || id.contains("sapling")
                || id.contains("fungus") || id.contains("scrub") || id.contains("reed")) {
            return "minecraft:dead_bush";
        }
        if (id.contains("puddle") || id.contains("sludge") || id.contains("mud")) {
            return "minecraft:mud";
        }
        if (id.contains("glass")) {
            return "minecraft:glass";
        }
        if (id.contains("wood") || id.contains("log")) {
            return "minecraft:oak_log";
        }
        if (id.contains("ore")) {
            return "minecraft:iron_ore";
        }
        if (id.contains("workbench") || id.contains("bench") || id.contains("table")
                || id.contains("station") || id.contains("rack") || id.contains("forge")) {
            return "minecraft:iron_block";
        }
        return ashfallWorldgenStandIn(id);
    }


    private static String sanitizeAshfallNativeBiomePlacedFeatureReferences(String json) {
        if (json == null || json.isBlank()) {
            return json == null ? "" : json;
        }
        Pattern featurePattern = Pattern.compile("\"minecraft:([a-z0-9_]+)\"");
        Matcher matcher = featurePattern.matcher(json);
        return matcher.replaceAll(matchResult -> {
            String featureId = matchResult.group(1);
            String replacement = rewriteAshfallLegacyPlacedFeatureId(featureId);
            if (replacement == null || replacement.isBlank()) {
                return matchResult.group();
            }
            return Matcher.quoteReplacement("\"" + replacement + "\"");
        });
    }

    private static String rewriteAshfallLegacyPlacedFeatureId(String featureId) {
        if (featureId == null || featureId.isBlank()) {
            return null;
        }
        String featureIdReplacement = switch (featureId) {
            case "dead_bush", "dead_bush_patch", "dead_bush_patches" -> "echoashfallprotocol:ash_bushes";
            case "dirt_patch" -> "echoashfallprotocol:contaminated_soil_patches";
            case "gravel_patch", "gravel_scatter", "gravel_dense_patch", "gravel_surface_patch" ->
                    "echoashfallprotocol:rubble_scatter";
            case "iron_bars_patch", "iron_bars_scatter" -> "echoashfallprotocol:rusted_metal_debris_scatter";
            case "iron_block_placement", "iron_blocks" -> "echoashfallprotocol:bio_lab_leaks";
            case "iron_ore_scatter" -> "echoashfallprotocol:wasteland_scrap_ore";
            case "mud_patch" -> "echoashfallprotocol:acidic_sludge_pools";
            case "stone_patch" -> "echoashfallprotocol:ash_layer_patches";
            case "stone_pile" -> "echoashfallprotocol:concrete_rubble_piles";
            case "stone_scatter" -> "echoashfallprotocol:rubble_scatter";
            case "amethyst_block_cluster", "amethyst_block_patch" -> "echoashfallprotocol:crystal_energy_deposit";
            case "packed_ice_cluster", "packed_ice_scatter" -> "echoashfallprotocol:cryogenic_ruins_blue_ice_crystals";
            case "snow_patch" -> "echoashfallprotocol:fallout_dust_patches";
            default -> null;
        };
        if (featureIdReplacement != null) {
            return featureIdReplacement;
        }
        if (featureId.endsWith("_dense_patch")) {
            return "echoashfallprotocol:scattered_bones_dense";
        }
        if (featureId.endsWith("_surface_patch")) {
            return "echoashfallprotocol:deep_ash_surface";
        }
        if (featureId.endsWith("_patch")) {
            return "echoashfallprotocol:contaminated_soil_patches";
        }
        if (featureId.endsWith("_pile")) {
            return "echoashfallprotocol:concrete_rubble_piles";
        }
        if (featureId.endsWith("_scatter")) {
            return "echoashfallprotocol:rubble_scatter";
        }
        if (featureId.endsWith("_cluster")) {
            return "echoashfallprotocol:crystal_energy_deposit";
        }
        if (featureId.endsWith("_placement")) {
            return "echoashfallprotocol:bio_lab_leaks";
        }
        return null;
    }

    private static String sanitizeAshfallNativeWorldgenBlockReferences(String json) {
        String sanitized = json == null ? "" : json;
        for (String blockId : ASHFALL_WORLDGEN_BLOCK_IDS.stream()
                .sorted((left, right) -> Integer.compare(right.length(), left.length()))
                .toList()) {
            sanitized = replaceAshfallBlockStateField(sanitized, blockId, ashfallWorldgenStandIn(blockId));
            sanitized = replaceAshfallBlockStringReference(sanitized, blockId, ashfallWorldgenStandIn(blockId));
        }
        return sanitized.replace("minecraft:dead_bush_block", "minecraft:dead_bush");
    }

    private static String replaceAshfallBlockStateField(String json, String blockId, String standIn) {
        Pattern pattern = Pattern.compile(
                "(\"(?:block|Name)\"\\s*:\\s*\")echoashfallprotocol:" + Pattern.quote(blockId) + "(\")"
        );
        Matcher matcher = pattern.matcher(json);
        return matcher.replaceAll("$1" + Matcher.quoteReplacement(standIn) + "$2");
    }

    private static String replaceAshfallBlockStringReference(String json, String blockId, String standIn) {
        return json
                .replace("\"echoashfallprotocol:" + blockId + "\"", "\"" + standIn + "\"")
                .replace("\"#echoashfallprotocol:" + blockId + "\"", "\"" + standIn + "\"");
    }

    private static String ashfallWorldgenStandIn(String blockId) {
        return switch (blockId) {
            case "acid_mud", "toxic_puddle", "radioactive_sludge", "acidic_sludge" -> "minecraft:mud";
            case "ash_bush", "burnt_fern", "burnt_grass", "dry_grass", "toxic_grass",
                    "nuclear_grass", "wasteland_grass", "wasteland_reed", "mutated_sapling",
                    "irradiated_cactus", "nuclear_fungus", "thorn_scrub", "rusty_wheat",
                    "mutated_bush", "toxic_moss" -> "minecraft:dead_bush";
            case "burnt_tall_grass", "dry_tall_grass", "toxic_tall_grass", "nuclear_tall_grass",
                    "wasteland_tall_grass" -> "minecraft:tall_grass";
            case "ash_layer", "fallout_dust" -> "minecraft:snow";
            case "ash_stone", "wasteland_stone", "wasteland_trace_rubble", "toxic_slagstone",
                    "irradiated_shale", "cryogenic_fractured_stone", "crash_slag", "riftstone",
                    "concrete_rubble", "nexus_scar_stone", "rebar_block", "shattered_glass" -> "minecraft:stone";
            case "ashen_wasteland_dirt", "burnt_wasteland_soil", "contaminated_soil",
                    "nexus_cracked_soil", "wasteland_dirt", "wasteland_grass_block",
                    "mutated_wasteland_grass_block", "toxic_wasteland_grass_block",
                    "irradiated_crust", "permafrost" -> "minecraft:dirt";
            case "blue_ice_crystal", "frozen_conduit" -> "minecraft:packed_ice";
            case "cable_bundle", "corroded_pipe", "twisted_metal", "rusted_metal_debris",
                    "rusted_metal_sheet", "industrial_aggregate" -> "minecraft:iron_bars";
            case "charred_wood_log", "dead_wood_log" -> "minecraft:oak_log";
            case "mutated_leaves_gray", "mutated_leaves_purple" -> "minecraft:oak_leaves";
            case "cracked_asphalt", "oil_stained_concrete", "scorched_ash", "deep_ash",
                    "debris_block", "concrete_chunk", "rubble", "scattered_bones" -> "minecraft:gravel";
            case "echo_crystal", "energized_fissure", "ooze_crystal", "uranium_crystal" -> "minecraft:amethyst_block";
            case "scrap_ore" -> "minecraft:iron_ore";
            case "structure_cache", "supply_crate", "workshop_block", "trade_counter",
                    "weapon_rack", "bio_processing_station", "spore_garden", "map_table",
                    "survey_table" -> "minecraft:barrel";
            case "contaminant_condenser", "crystalline_synthesizer",
                    "drop_pod_hull", "nexus_capacitor", "nexus_core", "radiation_block",
                    "toxic_waste_barrel" -> "minecraft:iron_block";
            default -> "minecraft:stone";
        };
    }

    private static final List<String> ASHFALL_WORLDGEN_BLOCK_IDS = List.of(
            "acid_mud",
            "acidic_sludge",
            "ash_bush",
            "ash_layer",
            "ash_stone",
            "ashen_wasteland_dirt",
            "bio_processing_station",
            "blue_ice_crystal",
            "burnt_fern",
            "burnt_grass",
            "burnt_tall_grass",
            "burnt_wasteland_soil",
            "cable_bundle",
            "charred_wood_log",
            "concrete_chunk",
            "concrete_rubble",
            "contaminant_condenser",
            "contaminated_soil",
            "corroded_pipe",
            "cracked_asphalt",
            "cracked_earth",
            "crash_slag",
            "crystalline_synthesizer",
            "cryogenic_fractured_stone",
            "dead_wood_log",
            "debris_block",
            "deep_ash",
            "drop_pod_hull",
            "dry_grass",
            "dry_tall_grass",
            "echo_crystal",
            "energized_fissure",
            "fallout_dust",
            "frozen_conduit",
            "industrial_aggregate",
            "irradiated_cactus",
            "irradiated_crust",
            "irradiated_shale",
            "map_table",
            "mutated_bush",
            "mutated_leaves_gray",
            "mutated_leaves_purple",
            "mutated_sapling",
            "mutated_wasteland_grass_block",
            "nexus_capacitor",
            "nexus_core",
            "nexus_cracked_soil",
            "nexus_scar_stone",
            "nuclear_fungus",
            "nuclear_grass",
            "nuclear_tall_grass",
            "oil_stained_concrete",
            "ooze_crystal",
            "permafrost",
            "radiation_block",
            "radioactive_sludge",
            "rebar_block",
            "riftstone",
            "rubble",
            "rusted_metal_debris",
            "rusted_metal_sheet",
            "rusty_wheat",
            "scattered_bones",
            "scorched_ash",
            "scrap_ore",
            "shattered_glass",
            "spore_garden",
            "structure_cache",
            "supply_crate",
            "survey_table",
            "thorn_scrub",
            "toxic_grass",
            "toxic_moss",
            "toxic_puddle",
            "toxic_slagstone",
            "toxic_tall_grass",
            "toxic_wasteland_grass_block",
            "toxic_waste_barrel",
            "trade_counter",
            "twisted_metal",
            "uranium_crystal",
            "wasteland_dirt",
            "wasteland_grass",
            "wasteland_grass_block",
            "wasteland_reed",
            "wasteland_stone",
            "wasteland_tall_grass",
            "wasteland_trace_rubble",
            "weapon_rack",
            "workshop_block"
    );

    public static String sanitizeMinecraftBlockTagJson(String name, String json) {
        if (json == null) {
            return "";
        }
        String normalized = name == null ? "" : name.replace('\\', '/');
        if (normalized.endsWith("/walls.json")) {
            return json
                    .replace("minecraft:iron_bars_wall", "minecraft:cobblestone_wall")
                    .replace("minecraft:iron_block_wall", "minecraft:cobblestone_wall")
                    .replace("minecraft:yellow_concrete_wall", "minecraft:cobblestone_wall")
                    .replace("minecraft:deepslate_wall", "minecraft:cobbled_deepslate_wall")
                    .replace("minecraft:stone_wall", "minecraft:cobblestone_wall")
                    .replace("minecraft:stone_bricks_wall", "minecraft:stone_brick_wall")
                    .replace("minecraft:gravel_wall", "minecraft:cobblestone_wall")
                    .replace("minecraft:smooth_stone_wall", "minecraft:cobblestone_wall")
                    .replace("minecraft:black_concrete_wall", "minecraft:blackstone_wall")
                    .replace("minecraft:chiseled_deepslate_wall", "minecraft:cobbled_deepslate_wall")
                    .replace("minecraft:deepslate_tiles_wall", "minecraft:deepslate_tile_wall");
        }
        if (normalized.endsWith("/stairs.json")) {
            return json
                    .replace("minecraft:iron_bars_stairs", "minecraft:stone_stairs")
                    .replace("minecraft:iron_block_stairs", "minecraft:stone_stairs")
                    .replace("minecraft:yellow_concrete_stairs", "minecraft:stone_stairs")
                    .replace("minecraft:deepslate_stairs", "minecraft:cobbled_deepslate_stairs")
                    .replace("minecraft:black_concrete_stairs", "minecraft:blackstone_stairs")
                    .replace("minecraft:chiseled_deepslate_stairs", "minecraft:cobbled_deepslate_stairs")
                    .replace("minecraft:deepslate_tiles_stairs", "minecraft:deepslate_tile_stairs");
        }
        if (normalized.endsWith("/slabs.json")) {
            return json
                    .replace("minecraft:iron_bars_slab", "minecraft:stone_slab")
                    .replace("minecraft:iron_block_slab", "minecraft:stone_slab")
                    .replace("minecraft:yellow_concrete_slab", "minecraft:stone_slab")
                    .replace("minecraft:deepslate_slab", "minecraft:cobbled_deepslate_slab")
                    .replace("minecraft:stone_bricks_slab", "minecraft:stone_brick_slab")
                    .replace("minecraft:black_concrete_slab", "minecraft:blackstone_slab")
                    .replace("minecraft:chiseled_deepslate_slab", "minecraft:cobbled_deepslate_slab")
                    .replace("minecraft:deepslate_tiles_slab", "minecraft:deepslate_tile_slab");
        }
        return json;
    }
}
