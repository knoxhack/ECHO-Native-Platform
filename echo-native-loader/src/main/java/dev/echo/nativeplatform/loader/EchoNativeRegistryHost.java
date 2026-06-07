package dev.echo.nativeplatform.loader;

import dev.echo.nativeplatform.contracts.EchoNativeLoadStatus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Host for native content registries. Every registration is recorded and can be reported.
 *
 * <p>This is the primary registry for the Native Loader runtime lane.
 * Items, blocks, entities, block entities, menus, sounds, particles, effects, commands,
 * data components, recipes, creative tabs, biomes, worldgen, and client assets are registered
 * here before any backend mutation occurs.</p>
 *
 * <p>Registration IDs are deterministic and collisions are rejected.</p>
 */
public final class EchoNativeRegistryHost {
    public static final String SERVICE_ID = "echo.native.registry.host";
    private static final List<String> FIRST_CLASS_REGISTRY_KINDS = List.of(
            "item",
            "block",
            "entity",
            "block_entity",
            "menu",
            "sound",
            "particle",
            "effect",
            "command",
            "data_component",
            "recipe",
            "creative_tab",
            "biome",
            "worldgen",
            "client_asset"
    );

    public static List<String> firstClassRegistryKinds() {
        return FIRST_CLASS_REGISTRY_KINDS;
    }

    private final Map<String, RegistryEntry> items = new LinkedHashMap<>();
    private final Map<String, RegistryEntry> blocks = new LinkedHashMap<>();
    private final Map<String, RegistryEntry> entities = new LinkedHashMap<>();
    private final Map<String, RegistryEntry> blockEntities = new LinkedHashMap<>();
    private final Map<String, RegistryEntry> menus = new LinkedHashMap<>();
    private final Map<String, RegistryEntry> sounds = new LinkedHashMap<>();
    private final Map<String, RegistryEntry> particles = new LinkedHashMap<>();
    private final Map<String, RegistryEntry> effects = new LinkedHashMap<>();
    private final Map<String, RegistryEntry> commands = new LinkedHashMap<>();
    private final Map<String, RegistryEntry> dataComponents = new LinkedHashMap<>();
    private final Map<String, RegistryEntry> recipes = new LinkedHashMap<>();
    private final Map<String, RegistryEntry> creativeTabs = new LinkedHashMap<>();
    private final Map<String, RegistryEntry> biomes = new LinkedHashMap<>();
    private final Map<String, RegistryEntry> worldgen = new LinkedHashMap<>();
    private final Map<String, RegistryEntry> clientAssets = new LinkedHashMap<>();
    private final List<RegistryFailure> failures = new ArrayList<>();
    private final AtomicInteger sequence = new AtomicInteger(0);
    private NativeLoaderLiveRegistryBridge liveRegistryBridge = NativeLoaderLiveRegistryBridge.UNATTACHED;

    public void attachLiveBridge(NativeLoaderLiveRegistryBridge liveRegistryBridge) {
        this.liveRegistryBridge = liveRegistryBridge == null
                ? NativeLoaderLiveRegistryBridge.UNATTACHED
                : liveRegistryBridge;
    }

    public EchoNativeLoadStatus registerItem(String id, String namespace, String implementationClass, Map<String, Object> properties) {
        return register(items, "item", id, namespace, implementationClass, properties);
    }

    public EchoNativeLoadStatus registerBlock(String id, String namespace, String implementationClass, Map<String, Object> properties) {
        return register(blocks, "block", id, namespace, implementationClass, properties);
    }

    public EchoNativeLoadStatus registerEntity(String id, String namespace, String implementationClass, Map<String, Object> properties) {
        return register(entities, "entity", id, namespace, implementationClass, properties);
    }

    public EchoNativeLoadStatus registerBlockEntity(String id, String namespace, String implementationClass, Map<String, Object> properties) {
        return register(blockEntities, "block_entity", id, namespace, implementationClass, properties);
    }

    public EchoNativeLoadStatus registerMenu(String id, String namespace, String implementationClass, Map<String, Object> properties) {
        return register(menus, "menu", id, namespace, implementationClass, properties);
    }

    public EchoNativeLoadStatus registerSound(String id, String namespace, String implementationClass, Map<String, Object> properties) {
        return register(sounds, "sound", id, namespace, implementationClass, properties);
    }

    public EchoNativeLoadStatus registerParticle(String id, String namespace, String implementationClass, Map<String, Object> properties) {
        return register(particles, "particle", id, namespace, implementationClass, properties);
    }

    public EchoNativeLoadStatus registerEffect(String id, String namespace, String implementationClass, Map<String, Object> properties) {
        return register(effects, "effect", id, namespace, implementationClass, properties);
    }

    public EchoNativeLoadStatus registerCreativeTab(String id, String namespace, String implementationClass, Map<String, Object> properties) {
        return register(creativeTabs, "creative_tab", id, namespace, implementationClass, properties);
    }

    public EchoNativeLoadStatus registerCommand(String id, String namespace, String implementationClass, Map<String, Object> properties) {
        return register(commands, "command", id, namespace, implementationClass, properties);
    }

    public EchoNativeLoadStatus registerDataComponent(String id, String namespace, String implementationClass, Map<String, Object> properties) {
        return register(dataComponents, "data_component", id, namespace, implementationClass, properties);
    }

    public EchoNativeLoadStatus registerRecipe(String id, String namespace, String implementationClass, Map<String, Object> properties) {
        return register(recipes, "recipe", id, namespace, implementationClass, properties);
    }

    public EchoNativeLoadStatus registerBiome(String id, String namespace, String implementationClass, Map<String, Object> properties) {
        return register(biomes, "biome", id, namespace, implementationClass, properties);
    }

    public EchoNativeLoadStatus registerWorldgen(String id, String namespace, String implementationClass, Map<String, Object> properties) {
        return register(worldgen, "worldgen", id, namespace, implementationClass, properties);
    }

    public EchoNativeLoadStatus registerClientAsset(String id, String namespace, String implementationClass, Map<String, Object> properties) {
        return register(clientAssets, "client_asset", id, namespace, implementationClass, properties);
    }

    public EchoNativeLoadStatus registerDeclared(
            String moduleId,
            String registry,
            String id,
            Map<String, Object> properties
    ) {
        if (registry == null || registry.isBlank()) {
            failures.add(new RegistryFailure("", id == null ? "" : id, "registry is required", sequence.incrementAndGet()));
            return EchoNativeLoadStatus.FAILED;
        }
        String normalizedRegistry = normalizeRegistry(registry);
        String namespace = namespace(moduleId, id);
        String localId = localId(id);
        Map<String, Object> safeProperties = properties == null ? Map.of() : Map.copyOf(properties);
        String implementationClass = string(safeProperties.get("implementationClass"));
        return switch (normalizedRegistry) {
            case "item" -> registerItem(localId, namespace, implementationClass, safeProperties);
            case "block" -> registerBlock(localId, namespace, implementationClass, safeProperties);
            case "entity" -> registerEntity(localId, namespace, implementationClass, safeProperties);
            case "block_entity" -> registerBlockEntity(localId, namespace, implementationClass, safeProperties);
            case "menu" -> registerMenu(localId, namespace, implementationClass, safeProperties);
            case "sound" -> registerSound(localId, namespace, implementationClass, safeProperties);
            case "particle" -> registerParticle(localId, namespace, implementationClass, safeProperties);
            case "effect" -> registerEffect(localId, namespace, implementationClass, safeProperties);
            case "creative_tab" -> registerCreativeTab(localId, namespace, implementationClass, safeProperties);
            case "command" -> registerCommand(localId, namespace, implementationClass, safeProperties);
            case "data_component" -> registerDataComponent(localId, namespace, implementationClass, safeProperties);
            case "recipe" -> registerRecipe(localId, namespace, implementationClass, safeProperties);
            case "biome" -> registerBiome(localId, namespace, implementationClass, safeProperties);
            case "worldgen" -> registerWorldgen(localId, namespace, implementationClass, safeProperties);
            case "client_asset" -> registerClientAsset(localId, namespace, implementationClass, safeProperties);
            case "recipe_backend", "recipe_category", "search_index", "loot_table", "tag",
                    "structure", "world_preset", "world_template", "resource", "data_pack",
                    "resource_pack", "ui_surface", "ui_overlay", "client_overlay", "hud",
                    "hud_widget", "hud_layout", "screen", "screen_surface", "loading_screen", "main_menu",
                    "terminal", "index", "lens", "holomap", "holo_map", "minimap", "theme", "theme_tokens",
                    "ui_skin", "render_profile", "asset_kit", "block_palette", "screen_action",
                    "screen_binding", "screen_component", "screen_layout", "screen_markup", "theme_bridge",
                    "data_provider", "style", "scanner", "saved_data", "service", "integration",
                    "network", "network_payload", "packet", "payload", "channel", "config" ->
                    EchoNativeLoadStatus.UNSUPPORTED;
            default -> {
                failures.add(new RegistryFailure(registry, id == null ? "" : id,
                        "unsupported native registry surface: " + registry, sequence.incrementAndGet()));
                yield EchoNativeLoadStatus.UNSUPPORTED;
            }
        };
    }

    public List<RegistryEntry> items() {
        return List.copyOf(items.values());
    }

    public List<RegistryEntry> blocks() {
        return List.copyOf(blocks.values());
    }

    public List<RegistryEntry> entities() {
        return List.copyOf(entities.values());
    }

    public List<RegistryEntry> blockEntities() {
        return List.copyOf(blockEntities.values());
    }

    public List<RegistryEntry> menus() {
        return List.copyOf(menus.values());
    }

    public List<RegistryEntry> sounds() {
        return List.copyOf(sounds.values());
    }

    public List<RegistryEntry> particles() {
        return List.copyOf(particles.values());
    }

    public List<RegistryEntry> effects() {
        return List.copyOf(effects.values());
    }

    public List<RegistryEntry> creativeTabs() {
        return List.copyOf(creativeTabs.values());
    }

    public RegistryEntry creativeTab(String id) {
        String key = id == null ? "" : id.trim().toLowerCase();
        if (key.isBlank()) {
            return null;
        }
        RegistryEntry direct = creativeTabs.get(key);
        if (direct != null) {
            return direct;
        }
        for (RegistryEntry entry : creativeTabs.values()) {
            if (key.equals(entry.id()) || key.equals(entry.fullId())) {
                return entry;
            }
        }
        return null;
    }

    public List<String> creativeTabItemIds(String id) {
        RegistryEntry entry = creativeTab(id);
        if (entry == null) {
            return List.of();
        }
        return stringList(entry.properties().get("itemIds"));
    }

    public List<String> creativeTabSurfaceIds(String id) {
        RegistryEntry entry = creativeTab(id);
        if (entry == null) {
            return List.of();
        }
        return stringList(entry.properties().get("surfaceIds"));
    }

    public int creativeTabItemCount(String id) {
        return creativeTabItemIds(id).size();
    }

    public int creativeTabSurfaceCount(String id) {
        return creativeTabSurfaceIds(id).size();
    }

    public Map<String, Integer> creativeTabItemCounts() {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (RegistryEntry entry : creativeTabs.values()) {
            counts.put(entry.fullId(), stringList(entry.properties().get("itemIds")).size());
        }
        return Map.copyOf(counts);
    }

    public Map<String, Integer> creativeTabSurfaceCounts() {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (RegistryEntry entry : creativeTabs.values()) {
            counts.put(entry.fullId(), stringList(entry.properties().get("surfaceIds")).size());
        }
        return Map.copyOf(counts);
    }

    public List<RegistryEntry> commands() {
        return List.copyOf(commands.values());
    }

    public List<RegistryEntry> dataComponents() {
        return List.copyOf(dataComponents.values());
    }

    public List<RegistryEntry> recipes() {
        return List.copyOf(recipes.values());
    }

    public List<RegistryEntry> biomes() {
        return List.copyOf(biomes.values());
    }

    public List<RegistryEntry> worldgen() {
        return List.copyOf(worldgen.values());
    }

    public List<RegistryEntry> clientAssets() {
        return List.copyOf(clientAssets.values());
    }

    public List<RegistryFailure> failures() {
        return List.copyOf(failures);
    }

    public Map<String, Object> toReport() {
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("serviceId", SERVICE_ID);
        report.put("items", items.size());
        report.put("blocks", blocks.size());
        report.put("entities", entities.size());
        report.put("blockEntities", blockEntities.size());
        report.put("menus", menus.size());
        report.put("sounds", sounds.size());
        report.put("particles", particles.size());
        report.put("effects", effects.size());
        report.put("creativeTabs", creativeTabs.size());
        report.put("commands", commands.size());
        report.put("dataComponents", dataComponents.size());
        report.put("recipes", recipes.size());
        report.put("biomes", biomes.size());
        report.put("worldgen", worldgen.size());
        report.put("clientAssets", clientAssets.size());
        report.put("failures", failures.size());
        report.put("creativeTabItemCounts", creativeTabItemCounts());
        report.put("creativeTabSurfaceCounts", creativeTabSurfaceCounts());
        report.put("liveRegistryBridgeAttached", liveRegistryBridgeAttached());
        report.put("liveRegistryBridgeId", liveRegistryBridgeId());
        report.put("liveRegistryBridgeEvidence", liveRegistryBridge.registryEvidence());
        report.put("firstClassNativeRegistry", firstClassNativeRegistry());
        report.put("nativeRegistryProcess", nativeRegistryProcess());
        report.put("releaseRegistryTrusted", releaseRegistryTrusted());
        report.put("nativeRegistryMutationSupported", nativeRegistryMutationSupported());
        report.put("totalRegistered", totalRegistered());
        report.put("liveRegistryBridgeMutatedEntries", liveRegistryBridgeMutatedEntryCount());
        report.put("nativeRegistryHostMutatedEntries", nativeRegistryHostMutatedEntryCount());
        report.put("trustedRegistryMutatedEntries", trustedRegistryMutatedEntryCount());
        report.put("trustedRegistryMutatedIds", trustedRegistryMutatedIds());
        report.put("trustedRegistryMutationRecordIds", trustedRegistryMutationRecordIds());
        report.put("firstClassRegistryKinds", FIRST_CLASS_REGISTRY_KINDS);
        report.put("registryKindReports", registryKindReports());
        report.put("registryMutationCoverage", registryMutationCoverage());
        report.put("registryBridgeMutationReconciliation", registryBridgeMutationReconciliation());
        report.put("registeredOnlyFirstClassRegistryKinds", registeredOnlyFirstClassRegistryKinds());
        report.put("registeredOnlyFirstClassRegistryKindCount", registeredOnlyFirstClassRegistryKinds().size());
        report.put("registeredOnlyFirstClassRegistryIds", registeredOnlyFirstClassRegistryIds());
        report.put("registeredOnlyFirstClassRegistryIdsByKind", registeredOnlyFirstClassRegistryIdsByKind());
        report.put("failedFirstClassRegistryKinds", failedFirstClassRegistryKinds());
        report.put("failedFirstClassRegistryKindCount", failedFirstClassRegistryKinds().size());
        report.put("failedFirstClassRegistryIds", failedFirstClassRegistryIds());
        report.put("failedFirstClassRegistryIdsByKind", failedFirstClassRegistryIdsByKind());
        report.put("untrustedMutationFirstClassRegistryKinds", untrustedMutationFirstClassRegistryKinds());
        report.put("untrustedMutationFirstClassRegistryKindCount", untrustedMutationFirstClassRegistryKinds().size());
        report.put("untrustedMutationReasonCounts", untrustedMutationReasonCounts());
        report.put("untrustedMutationReasonCountsByKind", untrustedMutationReasonCountsByKind());
        report.put("allDeclaredRegistryKindsTrusted", allDeclaredRegistryKindsTrusted());
        report.put("allFirstClassRegistryKindsSupported", allFirstClassRegistryKindsSupported());
        report.put("registeredIds", allIds());
        return report;
    }

    public boolean liveRegistryBridgeAttached() {
        return liveRegistryBridge.attached();
    }

    public String liveRegistryBridgeId() {
        return liveRegistryBridge.bridgeId();
    }

    public boolean firstClassNativeRegistry() {
        return liveRegistryBridge.firstClassNativeRegistry();
    }

    public boolean nativeRegistryProcess() {
        return liveRegistryBridge.nativeRegistryProcess();
    }

    public boolean releaseRegistryTrusted() {
        return liveRegistryBridge.releaseRegistryTrusted();
    }

    public boolean nativeRegistryMutationSupported() {
        return liveRegistryBridge.nativeRegistryMutationSupported();
    }

    public int totalRegistered() {
        return allEntries().size();
    }

    public int liveRegistryBridgeMutatedEntryCount() {
        int count = 0;
        for (RegistryEntry entry : allEntries()) {
            if (entry.liveRegistryBridgeMutated()) {
                count++;
            }
        }
        return count;
    }

    public int nativeRegistryHostMutatedEntryCount() {
        int count = 0;
        for (RegistryEntry entry : allEntries()) {
            if (entry.nativeRegistryHostMutated()) {
                count++;
            }
        }
        return count;
    }

    public int trustedRegistryMutatedEntryCount() {
        int count = 0;
        for (RegistryEntry entry : allEntries()) {
            if (entry.nativeRegistryHostMutated()) {
                count++;
            }
        }
        return count;
    }

    public List<String> trustedRegistryMutatedIds() {
        List<String> ids = new ArrayList<>();
        for (RegistryEntry entry : allEntries()) {
            if (entry.nativeRegistryHostMutated()) {
                ids.add(entry.fullId());
            }
        }
        ids.sort(Comparator.naturalOrder());
        return List.copyOf(ids);
    }

    public List<String> trustedRegistryMutationRecordIds() {
        List<String> ids = new ArrayList<>();
        for (RegistryEntry entry : allEntries()) {
            if (entry.nativeRegistryHostMutated()) {
                ids.add(registryMutationRecordKey(entry.kind(), entry.namespace(), entry.id()));
            }
        }
        ids.sort(Comparator.naturalOrder());
        return List.copyOf(ids);
    }

    public Map<String, Object> registryBridgeMutationReconciliation() {
        Map<String, Object> evidence = liveRegistryBridge.registryEvidence();
        List<String> bridgeIds = stringList(evidence.get("mutatedRecordIds")).stream()
                .map(String::trim)
                .filter(id -> !id.isBlank())
                .sorted()
                .toList();
        int bridgeRecordCount = number(evidence.get("mutatedRecordCount"), -1);
        Map<String, Map<String, Object>> bridgeRecords = stringObjectMap(evidence.get("mutatedRecords"));
        List<String> bridgeRecordKeys = bridgeRecords.keySet().stream()
                .map(String::trim)
                .filter(id -> !id.isBlank())
                .sorted()
                .toList();
        List<String> trustedRecordIds = trustedRegistryMutationRecordIds();
        List<String> missingFromBridgeEvidence = new ArrayList<>(trustedRecordIds);
        missingFromBridgeEvidence.removeAll(bridgeIds);
        List<String> bridgeEvidenceWithoutTrustedEntry = new ArrayList<>(bridgeIds);
        bridgeEvidenceWithoutTrustedEntry.removeAll(trustedRecordIds);
        List<String> trustedRecordsMissingFromBridgeRecordMap = new ArrayList<>(trustedRecordIds);
        trustedRecordsMissingFromBridgeRecordMap.removeAll(bridgeRecordKeys);
        List<String> bridgeRecordMapWithoutTrustedEntry = new ArrayList<>(bridgeRecordKeys);
        bridgeRecordMapWithoutTrustedEntry.removeAll(trustedRecordIds);
        List<String> bridgeRecordMapProofMismatches = new ArrayList<>();
        for (RegistryEntry entry : allEntries()) {
            if (!entry.nativeRegistryHostMutated()) {
                continue;
            }
            String recordKey = registryMutationRecordKey(entry.kind(), entry.namespace(), entry.id());
            if (bridgeRecords.containsKey(recordKey)
                    && !registryMutationProofMatches(
                    entry.kind(),
                    entry.namespace(),
                    entry.id(),
                    entry.liveRegistryBridgeId(),
                    bridgeRecords.get(recordKey))) {
                bridgeRecordMapProofMismatches.add(recordKey);
            }
        }
        bridgeRecordMapProofMismatches.sort(Comparator.naturalOrder());
        boolean bridgeEvidenceCountMatchesTrustedEntries = bridgeRecordCount == trustedRecordIds.size()
                && bridgeIds.size() == trustedRecordIds.size()
                && bridgeRecordKeys.size() == trustedRecordIds.size();
        Map<String, Object> reconciliation = new LinkedHashMap<>();
        reconciliation.put("bridgeId", liveRegistryBridgeId());
        reconciliation.put("trustedRegistryMutatedIds", trustedRegistryMutatedIds());
        reconciliation.put("trustedRegistryMutationRecordIds", trustedRecordIds);
        reconciliation.put("bridgeMutatedRecordIds", bridgeIds);
        reconciliation.put("bridgeMutatedRecordMapIds", bridgeRecordKeys);
        reconciliation.put("trustedRegistryMutatedIdCount", trustedRecordIds.size());
        reconciliation.put("bridgeMutatedRecordIdCount", bridgeIds.size());
        reconciliation.put("bridgeMutatedRecordCount", bridgeRecordCount);
        reconciliation.put("bridgeMutatedRecordMapIdCount", bridgeRecordKeys.size());
        reconciliation.put("missingFromBridgeEvidence", List.copyOf(missingFromBridgeEvidence));
        reconciliation.put("bridgeEvidenceWithoutTrustedEntry", List.copyOf(bridgeEvidenceWithoutTrustedEntry));
        reconciliation.put("trustedRecordsMissingFromBridgeRecordMap",
                List.copyOf(trustedRecordsMissingFromBridgeRecordMap));
        reconciliation.put("bridgeRecordMapWithoutTrustedEntry", List.copyOf(bridgeRecordMapWithoutTrustedEntry));
        reconciliation.put("bridgeRecordMapProofMismatches", List.copyOf(bridgeRecordMapProofMismatches));
        reconciliation.put("bridgeRecordMapProofMatchesTrustedEntries", bridgeRecordMapProofMismatches.isEmpty());
        reconciliation.put("bridgeEvidenceCountMatchesTrustedEntries",
                bridgeEvidenceCountMatchesTrustedEntries);
        reconciliation.put("bridgeEvidenceMatchesTrustedEntries",
                missingFromBridgeEvidence.isEmpty()
                        && bridgeEvidenceWithoutTrustedEntry.isEmpty()
                        && trustedRecordsMissingFromBridgeRecordMap.isEmpty()
                        && bridgeRecordMapWithoutTrustedEntry.isEmpty()
                        && bridgeRecordMapProofMismatches.isEmpty()
                        && bridgeEvidenceCountMatchesTrustedEntries);
        return Map.copyOf(reconciliation);
    }

    private static String registryMutationRecordKey(String registry, String namespace, String id) {
        return string(registry).trim().toLowerCase() + ":"
                + string(namespace).trim().toLowerCase() + ":"
                + string(id).trim().toLowerCase();
    }

    public Map<String, Object> registryKindReports() {
        Map<String, Object> reports = new LinkedHashMap<>();
        reports.put("item", registryKindReport("item", items));
        reports.put("block", registryKindReport("block", blocks));
        reports.put("entity", registryKindReport("entity", entities));
        reports.put("block_entity", registryKindReport("block_entity", blockEntities));
        reports.put("menu", registryKindReport("menu", menus));
        reports.put("sound", registryKindReport("sound", sounds));
        reports.put("particle", registryKindReport("particle", particles));
        reports.put("effect", registryKindReport("effect", effects));
        reports.put("creative_tab", registryKindReport("creative_tab", creativeTabs));
        reports.put("command", registryKindReport("command", commands));
        reports.put("data_component", registryKindReport("data_component", dataComponents));
        reports.put("recipe", registryKindReport("recipe", recipes));
        reports.put("biome", registryKindReport("biome", biomes));
        reports.put("worldgen", registryKindReport("worldgen", worldgen));
        reports.put("client_asset", registryKindReport("client_asset", clientAssets));
        return Map.copyOf(reports);
    }

    public Map<String, Object> registryMutationCoverage() {
        List<String> declaredKinds = declaredRegistryKinds();
        List<String> trustedDeclaredKinds = trustedDeclaredRegistryKinds();
        Map<String, Object> coverage = new LinkedHashMap<>();
        coverage.put("declaredKinds", declaredKinds);
        coverage.put("requiredFirstClassKinds", FIRST_CLASS_REGISTRY_KINDS);
        coverage.put("firstClassNativeRegistry", firstClassNativeRegistry());
        coverage.put("nativeRegistryProcess", nativeRegistryProcess());
        coverage.put("releaseRegistryTrusted", releaseRegistryTrusted());
        coverage.put("nativeRegistryMutationSupported", nativeRegistryMutationSupported());
        coverage.put("declaredKindCount", declaredKinds.size());
        coverage.put("trustedDeclaredKindCount", trustedDeclaredKinds.size());
        coverage.put("trustedDeclaredKinds", trustedDeclaredKinds);
        coverage.put("trustedRegistryMutatedIds", trustedRegistryMutatedIds());
        coverage.put("trustedRegistryMutationRecordIds", trustedRegistryMutationRecordIds());
        coverage.put("registryBridgeMutationReconciliation", registryBridgeMutationReconciliation());
        coverage.put("registeredOnlyFirstClassRegistryKinds", registeredOnlyFirstClassRegistryKinds());
        coverage.put("registeredOnlyFirstClassRegistryIds", registeredOnlyFirstClassRegistryIds());
        coverage.put("registeredOnlyFirstClassRegistryIdsByKind", registeredOnlyFirstClassRegistryIdsByKind());
        coverage.put("failedFirstClassRegistryKinds", failedFirstClassRegistryKinds());
        coverage.put("failedFirstClassRegistryIds", failedFirstClassRegistryIds());
        coverage.put("failedFirstClassRegistryIdsByKind", failedFirstClassRegistryIdsByKind());
        coverage.put("untrustedMutationFirstClassRegistryKinds", untrustedMutationFirstClassRegistryKinds());
        coverage.put("untrustedMutationReasonCounts", untrustedMutationReasonCounts());
        coverage.put("untrustedMutationReasonCountsByKind", untrustedMutationReasonCountsByKind());
        coverage.put("allDeclaredRegistryKindsTrusted", allDeclaredRegistryKindsTrusted());
        coverage.put("allFirstClassRegistryKindsSupported", allFirstClassRegistryKindsSupported());
        coverage.put("summary", "Native registry mutation is tracked independently for item, block, entity, block_entity, menu, sound, particle, effect, command, data_component, recipe, creative_tab, biome, worldgen, and client_asset declarations.");
        return Map.copyOf(coverage);
    }

    public List<String> registeredOnlyFirstClassRegistryKinds() {
        List<String> registeredOnly = new ArrayList<>();
        for (Map.Entry<String, Object> entry : registryKindReports().entrySet()) {
            if (!(entry.getValue() instanceof Map<?, ?> report)) {
                continue;
            }
            if (Boolean.TRUE.equals(report.get("firstClassRegistryKind"))
                    && report.get("registeredOnlyCount") instanceof Number count
                    && count.intValue() > 0) {
                registeredOnly.add(entry.getKey());
            }
        }
        registeredOnly.sort(Comparator.naturalOrder());
        return List.copyOf(registeredOnly);
    }

    public List<String> registeredOnlyFirstClassRegistryIds() {
        List<String> ids = new ArrayList<>();
        for (List<String> kindIds : registeredOnlyFirstClassRegistryIdsByKind().values()) {
            ids.addAll(kindIds);
        }
        ids.sort(Comparator.naturalOrder());
        return List.copyOf(ids);
    }

    public Map<String, List<String>> registeredOnlyFirstClassRegistryIdsByKind() {
        Map<String, List<String>> idsByKind = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : registryKindReports().entrySet()) {
            if (!(entry.getValue() instanceof Map<?, ?> report)
                    || !Boolean.TRUE.equals(report.get("firstClassRegistryKind"))
                    || !(report.get("registeredOnlyIds") instanceof List<?> ids)
                    || ids.isEmpty()) {
                continue;
            }
            List<String> normalizedIds = new ArrayList<>();
            for (Object id : ids) {
                String text = string(id).trim();
                if (!text.isBlank()) {
                    normalizedIds.add(text);
                }
            }
            normalizedIds.sort(Comparator.naturalOrder());
            if (!normalizedIds.isEmpty()) {
                idsByKind.put(entry.getKey(), List.copyOf(normalizedIds));
            }
        }
        return sortedListMap(idsByKind);
    }

    public List<String> failedFirstClassRegistryKinds() {
        List<String> kinds = new ArrayList<>();
        for (RegistryFailure failure : failures) {
            String kind = normalizeRegistry(failure.kind());
            if (FIRST_CLASS_REGISTRY_KINDS.contains(kind) && !kinds.contains(kind)) {
                kinds.add(kind);
            }
        }
        kinds.sort(Comparator.naturalOrder());
        return List.copyOf(kinds);
    }

    public List<String> failedFirstClassRegistryIds() {
        List<String> ids = new ArrayList<>();
        for (List<String> kindIds : failedFirstClassRegistryIdsByKind().values()) {
            ids.addAll(kindIds);
        }
        ids.sort(Comparator.naturalOrder());
        return List.copyOf(ids);
    }

    public Map<String, List<String>> failedFirstClassRegistryIdsByKind() {
        Map<String, List<String>> idsByKind = new LinkedHashMap<>();
        for (RegistryFailure failure : failures) {
            String kind = normalizeRegistry(failure.kind());
            if (!FIRST_CLASS_REGISTRY_KINDS.contains(kind)) {
                continue;
            }
            String id = string(failure.id()).trim();
            if (id.isBlank()) {
                id = "<missing-id>";
            }
            idsByKind.computeIfAbsent(kind, ignored -> new ArrayList<>()).add(id);
        }
        for (List<String> ids : idsByKind.values()) {
            ids.sort(Comparator.naturalOrder());
        }
        return sortedListMap(idsByKind);
    }

    public List<String> untrustedMutationFirstClassRegistryKinds() {
        List<String> untrusted = new ArrayList<>();
        for (Map.Entry<String, Object> entry : registryKindReports().entrySet()) {
            if (!(entry.getValue() instanceof Map<?, ?> report)) {
                continue;
            }
            if (Boolean.TRUE.equals(report.get("firstClassRegistryKind"))
                    && report.get("untrustedMutationCount") instanceof Number count
                    && count.intValue() > 0) {
                untrusted.add(entry.getKey());
            }
        }
        untrusted.sort(Comparator.naturalOrder());
        return List.copyOf(untrusted);
    }

    public Map<String, Integer> untrustedMutationReasonCounts() {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (Map<String, Integer> kindCounts : untrustedMutationReasonCountsByKind().values()) {
            for (Map.Entry<String, Integer> entry : kindCounts.entrySet()) {
                counts.merge(entry.getKey(), entry.getValue(), Integer::sum);
            }
        }
        return sortedIntegerMap(counts);
    }

    public Map<String, Map<String, Integer>> untrustedMutationReasonCountsByKind() {
        Map<String, Map<String, Integer>> countsByKind = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : registryKindReports().entrySet()) {
            if (!(entry.getValue() instanceof Map<?, ?> report)
                    || !Boolean.TRUE.equals(report.get("firstClassRegistryKind"))
                    || !(report.get("untrustedMutationReasonCounts") instanceof Map<?, ?> reasonCounts)
                    || reasonCounts.isEmpty()) {
                continue;
            }
            Map<String, Integer> counts = new LinkedHashMap<>();
            for (Map.Entry<?, ?> reasonEntry : reasonCounts.entrySet()) {
                if (reasonEntry.getValue() instanceof Number count && count.intValue() > 0) {
                    counts.put(string(reasonEntry.getKey()), count.intValue());
                }
            }
            if (!counts.isEmpty()) {
                countsByKind.put(entry.getKey(), sortedIntegerMap(counts));
            }
        }
        Map<String, Map<String, Integer>> sorted = new LinkedHashMap<>();
        countsByKind.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> sorted.put(entry.getKey(), entry.getValue()));
        return Collections.unmodifiableMap(sorted);
    }

    public boolean allDeclaredRegistryKindsTrusted() {
        List<String> declaredKinds = declaredRegistryKinds();
        return failedFirstClassRegistryKinds().isEmpty()
                && (declaredKinds.isEmpty() || trustedDeclaredRegistryKinds().containsAll(declaredKinds));
    }

    public boolean allFirstClassRegistryKindsSupported() {
        return firstClassNativeRegistry()
                && nativeRegistryProcess()
                && releaseRegistryTrusted()
                && nativeRegistryMutationSupported();
    }

    public List<String> allIds() {
        List<String> ids = new ArrayList<>();
        allEntries().forEach(e -> ids.add(e.fullId()));
        ids.sort(Comparator.naturalOrder());
        return List.copyOf(ids);
    }

    private List<RegistryEntry> allEntries() {
        List<RegistryEntry> entries = new ArrayList<>();
        entries.addAll(items.values());
        entries.addAll(blocks.values());
        entries.addAll(entities.values());
        entries.addAll(blockEntities.values());
        entries.addAll(menus.values());
        entries.addAll(sounds.values());
        entries.addAll(particles.values());
        entries.addAll(effects.values());
        entries.addAll(creativeTabs.values());
        entries.addAll(commands.values());
        entries.addAll(dataComponents.values());
        entries.addAll(recipes.values());
        entries.addAll(biomes.values());
        entries.addAll(worldgen.values());
        entries.addAll(clientAssets.values());
        return List.copyOf(entries);
    }

    private Map<String, Object> registryKindReport(String kind, Map<String, RegistryEntry> entries) {
        int liveMutated = 0;
        int nativeMutated = 0;
        int liveMutationRecordCount = 0;
        List<String> ids = new ArrayList<>();
        List<String> registeredOnlyIds = new ArrayList<>();
        List<String> untrustedMutationIds = new ArrayList<>();
        Map<String, List<String>> untrustedMutationReasonsById = new LinkedHashMap<>();
        Map<String, Integer> untrustedMutationReasonCounts = new LinkedHashMap<>();
        Map<String, Integer> statusCounts = new LinkedHashMap<>();
        for (RegistryEntry entry : entries.values()) {
            ids.add(entry.fullId());
            statusCounts.merge(entry.status().name(), 1, Integer::sum);
            if (entry.status() != EchoNativeLoadStatus.MUTATED
                    && !entry.liveRegistryBridgeMutated()
                    && !entry.nativeRegistryHostMutated()) {
                registeredOnlyIds.add(entry.fullId());
            }
            if (entry.liveRegistryBridgeMutated()) {
                liveMutated++;
            }
            if (entry.nativeRegistryHostMutated()) {
                nativeMutated++;
            }
            if (!entry.liveRegistryMutationRecord().isEmpty()) {
                liveMutationRecordCount++;
            }
            if (entry.status() == EchoNativeLoadStatus.MUTATED && !entry.nativeRegistryHostMutated()) {
                untrustedMutationIds.add(entry.fullId());
                List<String> reasons = registryMutationProofRejectionReasons(
                        entry.kind(),
                        entry.namespace(),
                        entry.id(),
                        entry.liveRegistryBridgeId(),
                        entry.liveRegistryMutationRecord()
                );
                untrustedMutationReasonsById.put(entry.fullId(), reasons);
                for (String reason : reasons) {
                    untrustedMutationReasonCounts.merge(reason, 1, Integer::sum);
                }
            }
        }
        ids.sort(Comparator.naturalOrder());
        registeredOnlyIds.sort(Comparator.naturalOrder());
        untrustedMutationIds.sort(Comparator.naturalOrder());
        int trustedMutated = 0;
        for (RegistryEntry entry : entries.values()) {
            if (entry.nativeRegistryHostMutated()) {
                trustedMutated++;
            }
        }
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("kind", kind);
        report.put("firstClassRegistryKind", FIRST_CLASS_REGISTRY_KINDS.contains(kind));
        report.put("declared", !entries.isEmpty());
        report.put("registeredCount", entries.size());
        report.put("liveRegistryBridgeMutatedCount", liveMutated);
        report.put("nativeRegistryHostMutatedCount", nativeMutated);
        report.put("liveRegistryMutationRecordCount", liveMutationRecordCount);
        report.put("trustedRegistryMutatedCount", trustedMutated);
        report.put("trustedRegistryMutationComplete", entries.isEmpty() || trustedMutated == entries.size());
        report.put("statusCounts", Map.copyOf(statusCounts));
        report.put("registeredOnlyIds", List.copyOf(registeredOnlyIds));
        report.put("registeredOnlyCount", registeredOnlyIds.size());
        report.put("untrustedMutationIds", List.copyOf(untrustedMutationIds));
        report.put("untrustedMutationCount", untrustedMutationIds.size());
        report.put("untrustedMutationReasonsById", sortedListMap(untrustedMutationReasonsById));
        report.put("untrustedMutationReasonCounts", Map.copyOf(untrustedMutationReasonCounts));
        report.put("liveRegistryBridgeAttached", liveRegistryBridgeAttached());
        report.put("liveRegistryBridgeId", liveRegistryBridgeId());
        report.put("firstClassNativeRegistry", firstClassNativeRegistry());
        report.put("nativeRegistryProcess", nativeRegistryProcess());
        report.put("releaseRegistryTrusted", releaseRegistryTrusted());
        report.put("nativeRegistryMutationSupported", nativeRegistryMutationSupported());
        report.put("ids", List.copyOf(ids));
        return Map.copyOf(report);
    }

    private static Map<String, List<String>> sortedListMap(Map<String, List<String>> values) {
        Map<String, List<String>> sorted = new LinkedHashMap<>();
        values.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> sorted.put(entry.getKey(), List.copyOf(entry.getValue())));
        return Collections.unmodifiableMap(sorted);
    }

    private static Map<String, Integer> sortedIntegerMap(Map<String, Integer> values) {
        Map<String, Integer> sorted = new LinkedHashMap<>();
        values.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> sorted.put(entry.getKey(), entry.getValue()));
        return Collections.unmodifiableMap(sorted);
    }

    private List<String> declaredRegistryKinds() {
        List<String> kinds = new ArrayList<>();
        if (!items.isEmpty()) {
            kinds.add("item");
        }
        if (!blocks.isEmpty()) {
            kinds.add("block");
        }
        if (!entities.isEmpty()) {
            kinds.add("entity");
        }
        if (!blockEntities.isEmpty()) {
            kinds.add("block_entity");
        }
        if (!menus.isEmpty()) {
            kinds.add("menu");
        }
        if (!sounds.isEmpty()) {
            kinds.add("sound");
        }
        if (!particles.isEmpty()) {
            kinds.add("particle");
        }
        if (!effects.isEmpty()) {
            kinds.add("effect");
        }
        if (!creativeTabs.isEmpty()) {
            kinds.add("creative_tab");
        }
        if (!commands.isEmpty()) {
            kinds.add("command");
        }
        if (!dataComponents.isEmpty()) {
            kinds.add("data_component");
        }
        if (!recipes.isEmpty()) {
            kinds.add("recipe");
        }
        if (!biomes.isEmpty()) {
            kinds.add("biome");
        }
        if (!worldgen.isEmpty()) {
            kinds.add("worldgen");
        }
        if (!clientAssets.isEmpty()) {
            kinds.add("client_asset");
        }
        return List.copyOf(kinds);
    }

    private List<String> trustedDeclaredRegistryKinds() {
        List<String> kinds = new ArrayList<>();
        if (registryKindTrusted(items)) {
            kinds.add("item");
        }
        if (registryKindTrusted(blocks)) {
            kinds.add("block");
        }
        if (registryKindTrusted(entities)) {
            kinds.add("entity");
        }
        if (registryKindTrusted(blockEntities)) {
            kinds.add("block_entity");
        }
        if (registryKindTrusted(menus)) {
            kinds.add("menu");
        }
        if (registryKindTrusted(sounds)) {
            kinds.add("sound");
        }
        if (registryKindTrusted(particles)) {
            kinds.add("particle");
        }
        if (registryKindTrusted(effects)) {
            kinds.add("effect");
        }
        if (registryKindTrusted(creativeTabs)) {
            kinds.add("creative_tab");
        }
        if (registryKindTrusted(commands)) {
            kinds.add("command");
        }
        if (registryKindTrusted(dataComponents)) {
            kinds.add("data_component");
        }
        if (registryKindTrusted(recipes)) {
            kinds.add("recipe");
        }
        if (registryKindTrusted(biomes)) {
            kinds.add("biome");
        }
        if (registryKindTrusted(worldgen)) {
            kinds.add("worldgen");
        }
        if (registryKindTrusted(clientAssets)) {
            kinds.add("client_asset");
        }
        return List.copyOf(kinds);
    }

    private static boolean registryKindTrusted(Map<String, RegistryEntry> entries) {
        if (entries.isEmpty()) {
            return false;
        }
        for (RegistryEntry entry : entries.values()) {
            if (!entry.nativeRegistryHostMutated()) {
                return false;
            }
        }
        return true;
    }

    private EchoNativeLoadStatus register(
            Map<String, RegistryEntry> registry,
            String kind,
            String id,
            String namespace,
            String implementationClass,
            Map<String, Object> properties
    ) {
        if (id == null || id.isBlank()) {
            failures.add(new RegistryFailure(kind, "", "id is required", sequence.incrementAndGet()));
            return EchoNativeLoadStatus.FAILED;
        }
        RegistryIdentity identity = registryIdentity(namespace, id);
        String normalizedId = identity.id();
        String normalizedNamespace = identity.namespace();
        String fullId = identity.fullId();
        RegistryEntry existing = registry.get(fullId);
        if (existing != null) {
            failures.add(new RegistryFailure(kind, fullId, "duplicate id: " + fullId, sequence.incrementAndGet()));
            return EchoNativeLoadStatus.FAILED;
        }
        String safeImplementationClass = implementationClass == null ? "" : implementationClass;
        Map<String, Object> safeProperties = properties == null ? Map.of() : Map.copyOf(properties);
        EchoNativeLoadStatus liveStatus = dispatchLive(kind, normalizedNamespace, normalizedId, safeImplementationClass, safeProperties);
        boolean liveBridgeAttached = liveRegistryBridge != null && liveRegistryBridge.attached();
        if (liveBridgeAttached && liveStatus != EchoNativeLoadStatus.FAILED) {
            Map<String, Object> liveRegistryMutationRecord = liveRegistryMutationRecord(
                    kind,
                    normalizedNamespace,
                    normalizedId,
                    liveStatus
            );
            boolean nativeRegistryHostMutated = nativeRegistryMutationAccepted(
                    liveStatus,
                    kind,
                    normalizedNamespace,
                    normalizedId,
                    liveRegistryMutationRecord,
                    liveRegistryBridgeId()
            );
            registry.put(fullId, new RegistryEntry(
                    sequence.incrementAndGet(),
                    kind,
                    normalizedId,
                  normalizedNamespace,
                  safeImplementationClass,
                  safeProperties,
                  liveStatus,
                  liveStatus == EchoNativeLoadStatus.MUTATED,
                  nativeRegistryHostMutated,
                  liveRegistryBridgeId(),
                  liveRegistryMutationRecord
            ));
            return nativeRegistryHostMutated ? EchoNativeLoadStatus.MUTATED : liveStatus;
        }
        if (liveStatus == EchoNativeLoadStatus.FAILED) {
            failures.add(new RegistryFailure(kind, fullId,
                    "live registry bridge failed for id: " + fullId, sequence.incrementAndGet()));
            return EchoNativeLoadStatus.FAILED;
        }
        registry.put(fullId, new RegistryEntry(
                sequence.incrementAndGet(),
                kind,
                normalizedId,
              normalizedNamespace,
              safeImplementationClass,
              safeProperties,
              EchoNativeLoadStatus.REGISTERED,
              false,
              false,
              "",
              Map.of()
        ));
        return EchoNativeLoadStatus.REGISTERED;
    }

    private static RegistryIdentity registryIdentity(String namespace, String id) {
        String normalizedNamespace = namespace == null ? "" : namespace.trim().toLowerCase();
        String normalizedId = id == null ? "" : id.trim().toLowerCase();
        int separator = normalizedId.indexOf(':');
        if (separator > 0 && separator + 1 < normalizedId.length()) {
            normalizedNamespace = normalizedId.substring(0, separator);
            normalizedId = normalizedId.substring(separator + 1);
        }
        return new RegistryIdentity(normalizedNamespace, normalizedId);
    }

    private Map<String, Object> liveRegistryMutationRecord(
            String kind,
            String namespace,
            String id,
            EchoNativeLoadStatus liveStatus
    ) {
        if (liveStatus != EchoNativeLoadStatus.MUTATED
                || liveRegistryBridge == null || !liveRegistryBridge.attached()) {
            return Map.of();
        }
        try {
            Map<String, Object> record = liveRegistryBridge.registryMutationRecord(kind, namespace, id);
            return record == null ? Map.of() : Map.copyOf(record);
        } catch (RuntimeException exception) {
            return Map.of();
        }
    }

    private boolean nativeRegistryMutationAccepted(
            EchoNativeLoadStatus status,
            String kind,
            String namespace,
            String id,
            Map<String, Object> mutationRecord,
            String bridgeId
    ) {
        return status == EchoNativeLoadStatus.MUTATED
                && nativeRegistryMutationSupported()
                && releaseRegistryTrusted()
                && registryMutationProofMatches(kind, namespace, id, bridgeId, mutationRecord);
    }

    private static boolean registryMutationProofMatches(
            String kind,
            String namespace,
            String id,
            String bridgeId,
            Map<String, Object> mutationRecord
    ) {
        if (mutationRecord == null || mutationRecord.isEmpty()) {
            return false;
        }
        return registryNativeTableMutationApplied(mutationRecord)
                && registryMutationStatusMutated(mutationRecord)
                && registryMutationTrustMetadataPresent(mutationRecord)
                && registryMutationIdentityMatches(kind, namespace, id, bridgeId, mutationRecord);
    }

    private static boolean registryMutationApplied(Map<String, Object> mutationRecord) {
        if (mutationRecord == null || mutationRecord.isEmpty()) {
            return false;
        }
        return Boolean.TRUE.equals(mutationRecord.get("liveRegistryMutationApplied"))
                || Boolean.TRUE.equals(mutationRecord.get("productNativeRegistryTableMutated"))
                || Boolean.TRUE.equals(mutationRecord.get("bootstrapNativeRegistryApplied"))
                || Boolean.TRUE.equals(mutationRecord.get("nativeRegistryTableMutated"));
    }

    private static boolean registryNativeTableMutationApplied(Map<String, Object> mutationRecord) {
        if (mutationRecord == null || mutationRecord.isEmpty()) {
            return false;
        }
        return Boolean.TRUE.equals(mutationRecord.get("productNativeRegistryTableMutated"))
                || Boolean.TRUE.equals(mutationRecord.get("bootstrapNativeRegistryApplied"))
                || Boolean.TRUE.equals(mutationRecord.get("nativeRegistryTableMutated"));
    }

    private static boolean registryMutationStatusMutated(Map<String, Object> mutationRecord) {
        return mutationRecord != null
                && EchoNativeLoadStatus.MUTATED.name().equals(string(mutationRecord.get("status")));
    }

    private static boolean registryMutationTrustMetadataPresent(Map<String, Object> mutationRecord) {
        return mutationRecord != null
                && Boolean.TRUE.equals(mutationRecord.get("firstClassNativeRegistry"))
                && Boolean.TRUE.equals(mutationRecord.get("nativeRegistryProcess"))
                && Boolean.TRUE.equals(mutationRecord.get("releaseRegistryTrusted"))
                && Boolean.TRUE.equals(mutationRecord.get("nativeRegistryMutationSupported"));
    }

    private static boolean registryMutationIdentityMatches(
            String kind,
            String namespace,
            String id,
            String bridgeId,
            Map<String, Object> mutationRecord
    ) {
        if (mutationRecord == null || mutationRecord.isEmpty()) {
            return false;
        }
        String expectedFullId = (namespace == null || namespace.isBlank() ? "" : namespace + ":") + id;
        String expectedNamespace = namespace == null ? "" : namespace;
        String expectedId = id == null ? "" : id;
        return kind.equals(string(mutationRecord.get("registry")))
                && expectedNamespace.equals(string(mutationRecord.get("namespace")))
                && expectedId.equals(string(mutationRecord.get("id")))
                && string(bridgeId).equals(string(mutationRecord.get("bridgeId")))
                && expectedFullId.equals(string(mutationRecord.get("fullId")));
    }

    private static List<String> registryMutationProofRejectionReasons(
            String kind,
            String namespace,
            String id,
            String bridgeId,
            Map<String, Object> mutationRecord
    ) {
        if (registryMutationProofMatches(kind, namespace, id, bridgeId, mutationRecord)) {
            return List.of();
        }
        List<String> reasons = new ArrayList<>();
        if (mutationRecord == null || mutationRecord.isEmpty()) {
            reasons.add("mutation_record_missing");
            return List.copyOf(reasons);
        }
        if (!registryMutationApplied(mutationRecord)) {
            reasons.add("mutation_applied_flag_missing");
        }
        if (!registryNativeTableMutationApplied(mutationRecord)) {
            reasons.add("native_registry_table_mutation_missing");
        }
        if (!registryMutationStatusMutated(mutationRecord)) {
            reasons.add("mutation_status_not_mutated");
        }
        if (!registryMutationTrustMetadataPresent(mutationRecord)) {
            reasons.add("record_trust_metadata_missing");
        }
        if (!registryMutationIdentityMatches(kind, namespace, id, bridgeId, mutationRecord)) {
            reasons.add("record_identity_mismatch");
        }
        return List.copyOf(reasons);
    }

    private EchoNativeLoadStatus dispatchLive(
            String registry,
            String namespace,
            String id,
            String implementationClass,
            Map<String, Object> properties
    ) {
        if (liveRegistryBridge == null || !liveRegistryBridge.attached()) {
            return EchoNativeLoadStatus.UNSUPPORTED;
        }
        try {
            EchoNativeLoadStatus status = liveRegistryBridge.register(registry, namespace, id, implementationClass, properties);
            return status == null ? EchoNativeLoadStatus.FAILED : status;
        } catch (RuntimeException exception) {
            return EchoNativeLoadStatus.FAILED;
        }
    }

    private static String normalizeRegistry(String registry) {
        String normalized = registry == null ? "" : registry.trim().toLowerCase().replace('-', '_').replace('.', '_');
        return switch (normalized) {
            case "items" -> "item";
            case "blocks" -> "block";
            case "entities" -> "entity";
            case "blockentity", "blockentities", "block_entity", "block_entities" -> "block_entity";
            case "menus" -> "menu";
            case "sounds" -> "sound";
            case "particles", "particle_profile", "particle_profiles" -> "particle";
            case "effects", "mob_effect", "mob_effects", "mobeffect", "mobeffects" -> "effect";
            case "creativegroup", "creativegroups", "creative_group", "creative_groups",
                    "creative_tab", "creative_tabs" -> "creative_tab";
            case "commands" -> "command";
            case "datacomponent", "datacomponents", "data_component", "data_components" -> "data_component";
            case "recipes" -> "recipe";
            case "biomes" -> "biome";
            case "configured_feature", "configured_features", "placed_feature", "placed_features",
                    "world_generator", "world_generators", "worldgens" -> "worldgen";
            case "asset", "assets", "clientasset", "clientassets", "client_asset", "client_assets" -> "client_asset";
            case "loot", "loottable", "loottables", "loot_table", "loot_tables" -> "loot_table";
            case "tags" -> "tag";
            case "structures" -> "structure";
            case "worldpreset", "worldpresets", "world_preset", "world_presets" -> "world_preset";
            case "worldtemplate", "worldtemplates", "world_template", "world_templates" -> "world_template";
            case "resources" -> "resource";
            case "datapack", "datapacks", "data_pack", "data_packs" -> "data_pack";
            case "resourcepack", "resourcepacks", "resource_pack", "resource_packs" -> "resource_pack";
            case "ui", "uisurface", "uisurfaces", "ui_surface", "ui_surfaces" -> "ui_surface";
            case "uioverlay", "uioverlays", "ui_overlay", "ui_overlays" -> "ui_overlay";
            case "clientoverlay", "clientoverlays", "client_overlay", "client_overlays" -> "client_overlay";
            case "huds" -> "hud";
            case "hudwidget", "hudwidgets", "hud_widget", "hud_widgets" -> "hud_widget";
            case "hudlayout", "hudlayouts", "hud_layout", "hud_layouts" -> "hud_layout";
            case "screens" -> "screen";
            case "screensurface", "screensurfaces", "screen_surface", "screen_surfaces" -> "screen_surface";
            case "loadingscreen", "loadingscreens", "loading_screen", "loading_screens" -> "loading_screen";
            case "mainmenu", "mainmenus", "main_menu", "main_menus" -> "main_menu";
            case "holomap", "holo_map", "holo_maps", "holo.map", "holo.maps" -> "holomap";
            case "minimaps" -> "minimap";
            case "themes" -> "theme";
            case "themetoken", "themetokens", "theme_token", "theme_tokens" -> "theme_tokens";
            case "uiskin", "uiskins", "ui_skin", "ui_skins" -> "ui_skin";
            case "renderprofile", "renderprofiles", "render_profile", "render_profiles" -> "render_profile";
            case "assetkit", "assetkits", "asset_kit", "asset_kits" -> "asset_kit";
            case "blockpalette", "blockpalettes", "block_palette", "block_palettes" -> "block_palette";
            case "screenaction", "screenactions", "screen_action", "screen_actions" -> "screen_action";
            case "screenbinding", "screenbindings", "screen_binding", "screen_bindings" -> "screen_binding";
            case "screencomponent", "screencomponents", "screen_component", "screen_components" -> "screen_component";
            case "screenlayout", "screenlayouts", "screen_layout", "screen_layouts" -> "screen_layout";
            case "screenmarkup", "screenmarkups", "screen_markup", "screen_markups" -> "screen_markup";
            case "themebridge", "themebridges", "theme_bridge", "theme_bridges" -> "theme_bridge";
            case "dataprovider", "dataproviders", "data_provider", "data_providers" -> "data_provider";
            case "styles" -> "style";
            case "scanners" -> "scanner";
            case "saveddata", "saved_data" -> "saved_data";
            case "services" -> "service";
            case "integrations" -> "integration";
            case "recipebackend", "recipebackends", "recipe_backend", "recipe_backends" -> "recipe_backend";
            case "recipecategory", "recipecategories", "recipe_category", "recipe_categories" -> "recipe_category";
            case "searchindex", "searchindexes", "search_index", "search_indexes" -> "search_index";
            case "networkpayload", "networkpayloads", "network_payload", "network_payloads" -> "network_payload";
            case "networking" -> "network";
            case "packets" -> "packet";
            case "payloads" -> "payload";
            case "channels" -> "channel";
            case "configs", "configuration" -> "config";
            default -> normalized;
        };
    }

    private static String namespace(String moduleId, String id) {
        if (id != null && id.contains(":")) {
            return id.substring(0, id.indexOf(':'));
        }
        return moduleId == null || moduleId.isBlank() ? "" : moduleId;
    }

    private static String localId(String id) {
        if (id == null) {
            return "";
        }
        return id.contains(":") ? id.substring(id.indexOf(':') + 1) : id;
    }

    private static String string(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static List<String> stringList(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (Object item : list) {
            String text = string(item).trim();
            if (!text.isBlank()) {
                result.add(text);
            }
        }
        return List.copyOf(result);
    }

    private static List<String> stringMapKeys(Object value) {
        if (!(value instanceof Map<?, ?> map)) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (Object key : map.keySet()) {
            String text = string(key).trim();
            if (!text.isBlank()) {
                result.add(text);
            }
        }
        return List.copyOf(result);
    }

    private static Map<String, Map<String, Object>> stringObjectMap(Object value) {
        if (!(value instanceof Map<?, ?> map)) {
            return Map.of();
        }
        Map<String, Map<String, Object>> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            String key = string(entry.getKey()).trim();
            if (key.isBlank()) {
                continue;
            }
            if (!(entry.getValue() instanceof Map<?, ?> nested)) {
                result.put(key, Map.of());
                continue;
            }
            Map<String, Object> record = new LinkedHashMap<>();
            nested.forEach((nestedKey, nestedValue) -> record.put(string(nestedKey), nestedValue));
            result.put(key, Map.copyOf(record));
        }
        return Map.copyOf(result);
    }

    private static int number(Object value, int fallback) {
        return value instanceof Number number ? number.intValue() : fallback;
    }

    private record RegistryIdentity(String namespace, String id) {
        private String fullId() {
            return (namespace.isBlank() ? "" : namespace + ":") + id;
        }
    }

    public record RegistryEntry(
            int sequence,
            String kind,
            String id,
          String namespace,
          String implementationClass,
          Map<String, Object> properties,
          EchoNativeLoadStatus status,
          boolean liveRegistryBridgeMutated,
          boolean nativeRegistryHostMutated,
          String liveRegistryBridgeId,
          Map<String, Object> liveRegistryMutationRecord
    ) {
        public String fullId() {
            return (namespace.isBlank() ? "" : namespace + ":") + id;
        }

        public Map<String, Object> toReport() {
            Map<String, Object> report = new LinkedHashMap<>();
            report.put("sequence", sequence);
            report.put("kind", kind);
            report.put("id", id);
            report.put("namespace", namespace);
            report.put("fullId", fullId());
          report.put("implementationClass", implementationClass);
          report.put("properties", properties);
          report.put("status", status.name());
          report.put("registeredOnly", status != EchoNativeLoadStatus.MUTATED
                  && !liveRegistryBridgeMutated
                  && !nativeRegistryHostMutated);
          report.put("liveRegistryBridgeMutated", liveRegistryBridgeMutated);
            report.put("nativeRegistryHostMutated", nativeRegistryHostMutated);
            report.put("liveRegistryBridgeId", liveRegistryBridgeId);
            report.put("liveRegistryMutationRecordPresent", !liveRegistryMutationRecord.isEmpty());
            report.put("liveRegistryMutationRecordStatus", string(liveRegistryMutationRecord.get("status")));
            report.put("liveRegistryMutationApplied",
                    registryMutationApplied(liveRegistryMutationRecord));
            report.put("liveRegistryNativeTableMutationApplied",
                    registryNativeTableMutationApplied(liveRegistryMutationRecord));
            report.put("liveRegistryMutationRecordIdentityMatched",
                    registryMutationIdentityMatches(kind, namespace, id, liveRegistryBridgeId, liveRegistryMutationRecord));
            report.put("liveRegistryMutationRecordTrustMetadataPresent",
                    registryMutationTrustMetadataPresent(liveRegistryMutationRecord));
            report.put("liveRegistryMutationProofAccepted", nativeRegistryHostMutated);
            report.put("liveRegistryMutationProofRejected",
                    status == EchoNativeLoadStatus.MUTATED && !nativeRegistryHostMutated);
            report.put("liveRegistryMutationProofRejectionReasons",
                    registryMutationProofRejectionReasons(kind, namespace, id, liveRegistryBridgeId, liveRegistryMutationRecord));
            report.put("liveRegistryMutationRecord", liveRegistryMutationRecord);
            return report;
        }
    }

    public record RegistryFailure(String kind, String id, String reason, int sequence) {
        public Map<String, Object> toReport() {
            Map<String, Object> report = new LinkedHashMap<>();
            report.put("sequence", sequence);
            report.put("kind", kind);
            report.put("id", id);
            report.put("reason", reason);
            return report;
        }
    }
}
