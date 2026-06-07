package dev.echo.nativeplatform.loader;

import dev.echo.nativeplatform.contracts.EchoNativeLoadStatus;

import java.nio.file.Path;
import java.nio.file.Files;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Native product resource host for data-pack, resource-pack, recipe, loot, tag,
 * worldgen, and UI resource declarations.
 */
public final class NativeLoaderResourceHost {
    public static final String SERVICE_ID = "echo.native.resource_host";

    private final Map<String, ResourceEntry> resources = new LinkedHashMap<>();
    private int sequence = 0;

    public synchronized EchoNativeLoadStatus registerDescriptorDomain(
            String moduleId,
            String domain,
            Map<String, Object> evidence
    ) {
        if (!isResourceDomain(domain)) {
            return EchoNativeLoadStatus.UNSUPPORTED;
        }
        return registerResource(
                moduleId,
                "descriptor." + normalized(domain),
                normalized(domain),
                evidenceWith(evidence, "source", "descriptor.adapterCore.domain")
        );
    }

    public synchronized EchoNativeLoadStatus registerResource(
            String moduleId,
            String resourceId,
            String resourceType,
            Map<String, Object> evidence
    ) {
        String safeModuleId = value(moduleId, "unknown_module");
        String safeResourceId = value(resourceId, "");
        if (safeResourceId.isBlank()) {
            return EchoNativeLoadStatus.FAILED;
        }
        String key = safeModuleId + ":" + safeResourceId;
        if (resources.containsKey(key)) {
            return EchoNativeLoadStatus.RESOLVED;
        }
        Map<String, Object> safeEvidence = new LinkedHashMap<>(evidence == null ? Map.of() : evidence);
        safeEvidence.put("nativeResourceHostMounted", true);
        safeEvidence.putIfAbsent("nativeResourceHostExecutionMode", "native_product_resource_mount");
        safeEvidence.putIfAbsent("liveMinecraftMutation", false);
        safeEvidence.putIfAbsent("minecraftRuntimeAccessed", false);
        resources.put(key, new ResourceEntry(
                ++sequence,
                safeModuleId,
                safeResourceId,
                value(resourceType, "resources"),
                Map.copyOf(safeEvidence)
        ));
        return EchoNativeLoadStatus.MUTATED;
    }

    public synchronized EchoNativeLoadStatus registerPreWorldCreationMount(
            String moduleId,
            String resourceId,
            String resourceType,
            Path mountPath,
            Map<String, Object> evidence
    ) {
        if (mountPath == null || !Files.isRegularFile(mountPath)) {
            return EchoNativeLoadStatus.FAILED;
        }
        Map<String, Object> mountedEvidence = new LinkedHashMap<>(evidence == null ? Map.of() : evidence);
        mountedEvidence.put("mountPath", mountPath.toAbsolutePath().normalize().toString());
        mountedEvidence.put("mountFilePresent", true);
        mountedEvidence.put("nativeResourceHostExecutionMode", "pre_registry_world_creation_mount");
        mountedEvidence.put("nativeResourceHostPreWorldCreationMount", true);
        mountedEvidence.put("filesystemMountedBeforeRegistryWorldCreation", true);
        mountedEvidence.put("nativeResourceMountProven", true);
        mountedEvidence.put("liveMinecraftMutation", false);
        mountedEvidence.put("minecraftRuntimeAccessed", false);
        return registerResource(moduleId, resourceId, resourceType, mountedEvidence);
    }

    public static Map<String, Object> preWorldCreationProductMountReport(
            String namespace,
            String packId,
            Path resourcePack,
            Collection<String> launchStagedDatapackPaths,
            String worldPreset,
            String source
    ) {
        NativeLoaderResourceHost host = new NativeLoaderResourceHost();
        String safeNamespace = value(namespace, "unknown_namespace");
        String safePackId = resourceIdPart(packId);
        String safeSource = value(source, SERVICE_ID);
        host.registerPreWorldCreationMount(
                safeNamespace,
                "native_loader." + safePackId + ".module_resource_pack",
                "resource_pack",
                resourcePack,
                Map.of(
                        "source", safeSource,
                        "requiredPack", true,
                        "hiddenPack", true,
                        "mountPhase", "before_registry_and_world_creation"
                ));
        if (launchStagedDatapackPaths != null) {
            for (String path : launchStagedDatapackPaths) {
                if (path == null || path.isBlank()) {
                    continue;
                }
                Path datapack = Path.of(path);
                host.registerPreWorldCreationMount(
                        safeNamespace,
                        "native_loader." + safePackId + ".launch_datapack." + resourceIdPart(datapack.getFileName().toString()),
                        "data_pack",
                        datapack,
                        Map.of(
                                "source", safeSource,
                                "requiredPack", true,
                                "mountPhase", "before_registry_and_world_creation",
                                "worldPreset", value(worldPreset, "")
                        ));
            }
        }
        return host.toReport();
    }

    public static Map<String, Object> markerFields(Map<String, Object> resourceBridge) {
        Map<String, Object> bridge = resourceBridge == null ? Map.of() : resourceBridge;
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("nativeResourceMarkerServiceId", SERVICE_ID);
        fields.put("minecraftResourcesApplied", Boolean.TRUE.equals(bridge.get("applied")));
        fields.put("nativeResourcePackApplied", Boolean.TRUE.equals(bridge.get("applied")));
        fields.put("nativeSdkResourceDeclarationCount", intValue(bridge.get("sdkResourceDeclarationCount")));
        fields.put("nativeSdkWorldStartupResourceCount", intValue(bridge.get("sdkWorldStartupResourceCount")));
        fields.put("nativeSdkResourceIds", bridge.getOrDefault("sdkResourceIds", List.of()));
        fields.put("nativeSdkResourceTypes", bridge.getOrDefault("sdkResourceTypes", List.of()));
        fields.put("nativeWorldStartupResourcesPromoted",
                Boolean.TRUE.equals(bridge.get("worldStartupResourcesPromoted")));
        return Map.copyOf(fields);
    }

    public synchronized Map<String, Object> toReport() {
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("serviceId", SERVICE_ID);
        report.put("mountedResourceCount", resources.size());
        report.put("mountedPreWorldCreationResourceCount", preWorldCreationResourceCount());
        report.put("mountedWorldStartupResourceCount", worldStartupResourceCount());
        report.put("mountedWorldgenResourceCount", worldgenResourceCount());
        report.put("mountedWorldPresetResourceCount", worldPresetResourceCount());
        report.put("mountedDataPackResourceCount", dataPackResourceCount());
        report.put("mountedResourcePackResourceCount", resourcePackResourceCount());
        report.put("mountedStructureResourceCount", structureResourceCount());
        report.put("mountedTagResourceCount", tagResourceCount());
        report.put("resources", resources.values().stream().map(ResourceEntry::toReport).toList());
        report.put("nativeResourceMountProven", preWorldCreationResourceCount() > 0);
        report.put("liveMinecraftMutation", false);
        report.put("minecraftRuntimeAccessed", false);
        return Map.copyOf(report);
    }

    public synchronized int mountedResourceCount() {
        return resources.size();
    }

    public synchronized int mountedResourceCountByType(String resourceType) {
        return mountedResourceCountMatchingTypes(List.of(resourceType));
    }

    public synchronized int mountedResourceCountMatchingTypes(Collection<String> resourceTypes) {
        if (resourceTypes == null || resourceTypes.isEmpty()) {
            return 0;
        }
        List<String> normalizedTypes = resourceTypes.stream()
                .map(NativeLoaderResourceHost::normalized)
                .filter(type -> !type.isBlank())
                .toList();
        if (normalizedTypes.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (ResourceEntry resource : resources.values()) {
            String type = normalized(resource.resourceType());
            if (normalizedTypes.contains(type)) {
                count++;
            }
        }
        return count;
    }

    public synchronized int worldStartupResourceCount() {
        return mountedResourceCountMatchingTypes(List.of(
                "data_pack",
                "datapack",
                "data.pack",
                "worldgen",
                "world_generator",
                "world.generator",
                "world_preset",
                "world.preset",
                "world_template",
                "world.template",
                "structure",
                "structures",
                "tag",
                "tags"
        ));
    }

    public synchronized int preWorldCreationResourceCount() {
        int count = 0;
        for (ResourceEntry resource : resources.values()) {
            Object mounted = resource.evidence().get("nativeResourceHostPreWorldCreationMount");
            if (Boolean.TRUE.equals(mounted)) {
                count++;
            }
        }
        return count;
    }

    public synchronized int worldgenResourceCount() {
        return mountedResourceCountMatchingTypes(List.of("worldgen", "world_generator", "world.generator"));
    }

    public synchronized int worldPresetResourceCount() {
        return mountedResourceCountMatchingTypes(List.of("world_preset", "world.preset"));
    }

    public synchronized int dataPackResourceCount() {
        return mountedResourceCountMatchingTypes(List.of("data_pack", "datapack", "data.pack"));
    }

    public synchronized int resourcePackResourceCount() {
        return mountedResourceCountMatchingTypes(List.of("resource_pack", "resourcepack", "resource.pack"));
    }

    public synchronized int structureResourceCount() {
        return mountedResourceCountMatchingTypes(List.of("structure", "structures"));
    }

    public synchronized int tagResourceCount() {
        return mountedResourceCountMatchingTypes(List.of("tag", "tags"));
    }

    private static boolean isResourceDomain(String domain) {
        String normalized = normalized(domain);
        return normalized.equals("resources")
                || normalized.equals("resource")
                || normalized.equals("resource.profile")
                || normalized.equals("resource.pack")
                || normalized.equals("data")
                || normalized.equals("data.pack")
                || normalized.equals("datapack")
                || normalized.equals("recipes")
                || normalized.equals("recipe")
                || normalized.equals("loot")
                || normalized.equals("loot.table")
                || normalized.equals("loot.tables")
                || normalized.equals("tag")
                || normalized.equals("tags")
                || normalized.equals("sound")
                || normalized.equals("sounds")
                || normalized.equals("structure")
                || normalized.equals("structures")
                || normalized.equals("worldgen")
                || normalized.equals("world.generator")
                || normalized.equals("world.preset")
                || normalized.equals("world.template")
                || normalized.equals("asset")
                || normalized.equals("assets")
                || normalized.equals("ui.screen")
                || normalized.equals("ui.screens")
                || normalized.equals("theme")
                || normalized.equals("themes")
                || normalized.equals("theme.tokens")
                || normalized.equals("ui.skin")
                || normalized.equals("ui.skins")
                || normalized.equals("render.profile")
                || normalized.equals("render.profiles")
                || normalized.equals("asset.kit")
                || normalized.equals("asset.kits")
                || normalized.equals("block.palette")
                || normalized.equals("block.palettes")
                || normalized.equals("screen.markup")
                || normalized.equals("screen.layout")
                || normalized.equals("screen.layouts")
                || normalized.equals("style")
                || normalized.equals("styles")
                || normalized.equals("data.provider")
                || normalized.equals("data.providers")
                || normalized.endsWith(".resource")
                || normalized.endsWith(".resources")
                || normalized.endsWith(".data");
    }

    private static Map<String, Object> evidenceWith(Map<String, Object> evidence, String key, Object value) {
        Map<String, Object> result = new LinkedHashMap<>(evidence == null ? Map.of() : evidence);
        result.put(key, value);
        return Map.copyOf(result);
    }

    private static String value(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private static String string(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private static String normalized(String value) {
        return string(value).toLowerCase(Locale.ROOT).replace('-', '.').replace('_', '.');
    }

    private static String resourceIdPart(String value) {
        String normalized = string(value).replaceAll("[^A-Za-z0-9._-]+", "_");
        return normalized.isBlank() ? "unknown" : normalized;
    }

    private static int intValue(Object value) {
        return value instanceof Number number ? number.intValue() : 0;
    }

    public record ResourceEntry(
            int sequence,
            String moduleId,
            String resourceId,
            String resourceType,
            Map<String, Object> evidence
    ) {
        public Map<String, Object> toReport() {
            Map<String, Object> report = new LinkedHashMap<>();
            report.put("sequence", sequence);
            report.put("moduleId", moduleId);
            report.put("resourceId", resourceId);
            report.put("resourceType", resourceType);
            report.put("evidence", evidence == null ? Map.of() : Map.copyOf(evidence));
            return Map.copyOf(report);
        }
    }
}
