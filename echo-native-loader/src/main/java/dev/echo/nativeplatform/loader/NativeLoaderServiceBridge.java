package dev.echo.nativeplatform.loader;

import dev.echo.nativeplatform.contracts.EchoNativeRegisteredService;
import dev.echo.nativeplatform.contracts.EchoNativeServiceRegistry;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public final class NativeLoaderServiceBridge {
    public static final String SERVICE_ID = "echo.native.service_bridge";

    private final EchoNativeServiceRegistry serviceRegistry;

    public NativeLoaderServiceBridge(EchoNativeServiceRegistry serviceRegistry) {
        this.serviceRegistry = serviceRegistry;
    }

    public boolean hasService(String serviceId) {
        return serviceRegistry.hasService(serviceId);
    }

    public boolean supportsSurface(String serviceId, String surface) {
        return resolve(serviceId, surface).isPresent();
    }

    public boolean supportsSurface(String surface) {
        return !activeRuntimeServicesForSurface(surface).isEmpty();
    }

    public Optional<NativeLoaderResolvedRuntimeService> resolve(String serviceId, String surface) {
        if (!serviceRegistry.hasService(serviceId)) {
            return Optional.empty();
        }
        return serviceRegistry.registeredServices().stream()
                .filter(service -> service.serviceId().equals(serviceId))
                .filter(service -> serviceSupportsSurface(service, surface))
                .findFirst()
                .map(this::resolvedService);
    }

    public List<NativeLoaderResolvedRuntimeService> activeRuntimeServices(String serviceId) {
        if (!serviceRegistry.hasService(serviceId)) {
            return List.of();
        }
        return serviceRegistry.registeredServices().stream()
                .filter(service -> service.serviceId().equals(serviceId))
                .map(this::resolvedService)
                .toList();
    }

    public List<NativeLoaderResolvedRuntimeService> activeRuntimeServicesForSurface(String surface) {
        if (surface == null || surface.isBlank()) {
            return List.of();
        }
        Set<String> aliases = surfaceAliases(surface);
        LinkedHashMap<String, NativeLoaderResolvedRuntimeService> services = new LinkedHashMap<>();
        serviceRegistry.registeredServices().stream()
                .filter(service -> service.surfaces().stream().anyMatch(aliases::contains))
                .map(this::resolvedService)
                .forEach(service -> services.putIfAbsent(service.moduleServiceKey(), service));
        return List.copyOf(services.values());
    }

    public Set<String> surfaceAliases(String surface) {
        if (surface == null || surface.isBlank()) {
            return Set.of();
        }
        LinkedHashSet<String> aliases = new LinkedHashSet<>();
        String normalized = surface.trim();
        aliases.add(normalized);
        switch (normalized) {
            case "inventory" -> aliases.addAll(List.of(
                    "items",
                    "item",
                    "loot",
                    "player",
                    "player.inventory.changed",
                    "player.consume.item",
                    "player.craft.item"
            ));
            case "player_state" -> aliases.addAll(List.of(
                    "player",
                    "data",
                    "saves",
                    "save.player.data",
                    "player.first.join",
                    "player.login",
                    "player.logout",
                    "player.tick",
                    "player.tick.post",
                    "player.death",
                    "player.equipment.changed",
                    "player.respawn.position"
            ));
            case "world_blocks" -> aliases.addAll(List.of(
                    "blocks",
                    "block",
                    "worldgen",
                    "world.hazard",
                    "player.block.placed",
                    "player.use.block"
            ));
            case "world_state" -> aliases.addAll(List.of(
                    "worldgen",
                    "world.hazard",
                    "world.tick",
                    "data",
                    "saves",
                    "saved.data",
                    "save.record",
                    "story.flag.save"
            ));
            case "structures" -> aliases.addAll(List.of(
                    "structures",
                    "worldgen",
                    "blocks",
                    "block"
            ));
            case "block_entities" -> aliases.addAll(List.of(
                    "block.entity",
                    "block",
                    "blocks",
                    "data"
            ));
            case "capabilities" -> aliases.addAll(List.of(
                    "capabilities",
                    "contract",
                    "contracts",
                    "player",
                    "data"
            ));
            case "missions" -> aliases.addAll(List.of(
                    "mission",
                    "missions",
                    "quest",
                    "quests",
                    "objectives",
                    "objective",
                    "story",
                    "progression",
                    "ashfall.missions"
            ));
            case "events" -> aliases.addAll(List.of(
                    "event",
                    "events",
                    "player",
                    "world.tick",
                    "client_tick",
                    "screen_events",
                    "lifecycle",
                    "relic.event",
                    "rift.event"
            ));
            case "commands" -> aliases.addAll(List.of(
                    "command",
                    "commands",
                    "server.commands",
                    "command.queue",
                    "adaptercore.native_command"
            ));
            case "network", "networking" -> aliases.addAll(List.of(
                    "network",
                    "networking",
                    "packet",
                    "packets",
                    "channels",
                    "adaptercore.native_runtime_packet"
            ));
            case "network_channels" -> aliases.addAll(List.of(
                    "network",
                    "networking",
                    "network_channels",
                    "channel",
                    "channels",
                    "packet",
                    "packets",
                    "adaptercore.native_runtime_packet"
            ));
            case "resources" -> aliases.addAll(List.of(
                    "resource",
                    "resources",
                    "resource_reloads",
                    "assets",
                    "data",
                    "resource_pack",
                    "data_pack",
                    "recipes",
                    "loot",
                    "tags",
                    "worldgen",
                    "ui.screens"
            ));
            case "config" -> aliases.addAll(List.of(
                    "config",
                    "configs",
                    "configuration",
                    "config_schema",
                    "client.config",
                    "server.config"
            ));
            case "config_reloads" -> aliases.addAll(List.of(
                    "config",
                    "configs",
                    "configuration",
                    "config_reload",
                    "config_reloads",
                    "client.config",
                    "server.config"
            ));
            case "client_tick" -> aliases.addAll(List.of(
                    "client_tick",
                    "client.tick",
                    "client.tick.start",
                    "client.tick.end",
                    "events"
            ));
            case "render_layers" -> aliases.addAll(List.of(
                    "render",
                    "render_layer",
                    "render.layers",
                    "client.render",
                    "hud",
                    "ui"
            ));
            case "screen_events" -> aliases.addAll(List.of(
                    "screen",
                    "screen_events",
                    "client.screen",
                    "client.screen.open",
                    "ui.screens",
                    "ui"
            ));
            case "keybinds" -> aliases.addAll(List.of(
                    "keybind",
                    "keybinds",
                    "input",
                    "client.input",
                    "client.key"
            ));
            case "resource_reloads" -> aliases.addAll(List.of(
                    "resource",
                    "resources",
                    "resource_reload",
                    "resource_reloads",
                    "data.reload",
                    "assets",
                    "data"
            ));
            case "save_hooks" -> aliases.addAll(List.of(
                    "save",
                    "save_hooks",
                    "save.lifecycle",
                    "save.record",
                    "saved.data",
                    "saves"
            ));
            case "lifecycle_phases" -> aliases.addAll(List.of(
                    "lifecycle",
                    "lifecycle_phases",
                    "lifecycle.phases",
                    "common_setup",
                    "client_setup",
                    "server_setup",
                    "ready",
                    "shutdown"
            ));
            case "server_client_sync" -> aliases.addAll(List.of(
                    "sync",
                    "server_client_sync",
                    "server.client.sync",
                    "network",
                    "networking",
                    "packet",
                    "packets"
            ));
            case "packets_hud" -> aliases.addAll(List.of(
                    "packet",
                    "packets",
                    "hud.packet",
                    "codex.packet",
                    "atmosphere.packet",
                    "networking",
                    "ui"
            ));
            case "hud" -> aliases.addAll(List.of(
                    "hud",
                    "hud.layout",
                    "hud.objective",
                    "hud.widget",
                    "hud.packet",
                    "ui",
                    "ui.screens",
                    "client.screen.open"
            ));
            case "save_data" -> aliases.addAll(List.of(
                    "data",
                    "saves",
                    "saved.data",
                    "save.record",
                    "save.player.data",
                    "story.flag.save",
                    "data.provider",
                    "data.reload",
                    "data.key"
            ));
            default -> {
                if (normalized.endsWith("_data")) {
                    aliases.add(normalized.substring(0, normalized.length() - "_data".length()));
                }
                if (normalized.contains("_")) {
                    aliases.add(normalized.replace('_', '.'));
                }
            }
        }
        return java.util.Collections.unmodifiableSet(aliases);
    }

    private boolean serviceSupportsSurface(EchoNativeRegisteredService service, String surface) {
        Set<String> aliases = surfaceAliases(surface);
        if (aliases.isEmpty()) {
            return false;
        }
        return service.surfaces().stream().anyMatch(aliases::contains);
    }

    public List<NativeLoaderResolvedRuntimeService> activeRuntimeServicesForExactSurface(String surface) {
        if (surface == null || surface.isBlank()) {
            return List.of();
        }
        return serviceRegistry.registeredServices().stream()
                .filter(service -> service.surfaces().contains(surface))
                .map(this::resolvedService)
                .toList();
    }

    public List<NativeLoaderResolvedRuntimeService> allActiveRuntimeServices() {
        return serviceRegistry.registeredServices().stream()
                .map(this::resolvedService)
                .toList();
    }

    public Map<String, Object> toReport(String serviceId) {
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("serviceId", serviceId);
        report.put("resolved", hasService(serviceId));
        report.put("activeRuntimeServices", activeRuntimeServices(serviceId).stream()
                .map(NativeLoaderResolvedRuntimeService::toReport)
                .toList());
        return Map.copyOf(report);
    }

    public static Map<String, Object> markerFields(Map<String, Object> serviceBridge) {
        Map<String, Object> bridge = serviceBridge == null ? Map.of() : serviceBridge;
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("nativeServiceBridgeMarkerServiceId", SERVICE_ID);
        fields.put("nativeServiceBridgeApplied", Boolean.TRUE.equals(bridge.get("applied")));
        fields.put("nativeRuntimeInitializedServiceCount", intValue(bridge.get("runtimeInitializedServiceCount")));
        fields.put("nativeAgent3GameplaySurfaceReadyCount", intValue(bridge.get("agent3ReadySurfaceCount")));
        fields.put("nativeAgent3GameplaySurfaceCoverageReady",
                Boolean.TRUE.equals(bridge.get("agent3GameplaySurfaceCoverageReady")));
        return Map.copyOf(fields);
    }

    public Map<String, Object> toReport() {
        List<NativeLoaderResolvedRuntimeService> services = allActiveRuntimeServices();
        TreeSet<String> modules = new TreeSet<>();
        TreeSet<String> surfaces = new TreeSet<>();
        TreeSet<String> serviceInstanceClasses = new TreeSet<>();
        TreeMap<String, List<Map<String, Object>>> servicesBySurface = new TreeMap<>();
        TreeMap<String, Integer> serviceCountsByModule = new TreeMap<>();
        int attachedServiceCount = 0;
        for (NativeLoaderResolvedRuntimeService service : services) {
            modules.add(service.moduleId());
            serviceCountsByModule.merge(service.moduleId(), 1, Integer::sum);
            if (service.serviceInstanceAttached()) {
                attachedServiceCount++;
                if (service.serviceInstanceClass() != null && !service.serviceInstanceClass().isBlank()) {
                    serviceInstanceClasses.add(service.serviceInstanceClass());
                }
            }
            for (String surface : service.surfaces()) {
                if (surface == null || surface.isBlank()) {
                    continue;
                }
                surfaces.add(surface);
                servicesBySurface.computeIfAbsent(surface, ignored -> new java.util.ArrayList<>())
                        .add(service.toReport());
            }
        }
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("runtimeAttached", !services.isEmpty());
        report.put("activeRuntimeServiceCount", services.size());
        report.put("activeRuntimeAttachedServiceCount", attachedServiceCount);
        report.put("activeRuntimeModuleCount", modules.size());
        report.put("activeRuntimeSurfaceCount", surfaces.size());
        report.put("activeRuntimeModules", List.copyOf(modules));
        report.put("activeRuntimeSurfaces", List.copyOf(surfaces));
        report.put("activeRuntimeServiceInstanceClasses", List.copyOf(serviceInstanceClasses));
        report.put("serviceCountsByModule", Map.copyOf(serviceCountsByModule));
        report.put("servicesBySurface", Map.copyOf(servicesBySurface));
        return Map.copyOf(report);
    }

    public Map<String, Object> toSurfaceReport(String surface) {
        List<NativeLoaderResolvedRuntimeService> services = activeRuntimeServicesForSurface(surface);
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("surface", surface);
        report.put("surfaceAliases", List.copyOf(surfaceAliases(surface)));
        report.put("resolved", !services.isEmpty());
        report.put("activeRuntimeServiceCount", services.size());
        report.put("activeRuntimeServices", services.stream()
                .map(NativeLoaderResolvedRuntimeService::toReport)
                .toList());
        return Map.copyOf(report);
    }

    private NativeLoaderResolvedRuntimeService resolvedService(EchoNativeRegisteredService service) {
        Object instance = serviceRegistry.service(service.moduleId(), service.serviceId())
                .or(() -> serviceRegistry.service(service.serviceId()))
                .orElse(null);
        return new NativeLoaderResolvedRuntimeService(
                service.moduleId(),
                service.serviceId(),
                service.implementationClass(),
                instance == null ? "" : instance.getClass().getName(),
                instance != null,
                service.surfaces()
        );
    }

    private static int intValue(Object value) {
        return value instanceof Number number ? number.intValue() : 0;
    }

}
