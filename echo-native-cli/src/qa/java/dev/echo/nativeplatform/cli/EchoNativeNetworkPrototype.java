package dev.echo.nativeplatform.cli;

import dev.echo.nativeplatform.contracts.EchoNativeDiagnostic;
import dev.echo.nativeplatform.contracts.EchoNativeIssueSeverity;
import dev.echo.nativeplatform.contracts.EchoNativeNetworkBridgePrototypeSafetyStatus;
import dev.echo.nativeplatform.contracts.EchoNativeNetworkChannelInventory;
import dev.echo.nativeplatform.contracts.EchoNativeNetworkConflictReport;
import dev.echo.nativeplatform.contracts.EchoNativeNetworkPacketValidation;
import dev.echo.nativeplatform.contracts.EchoNativeNetworkSchemaModel;
import dev.echo.nativeplatform.diagnostics.EchoNativeJson;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

final class EchoNativeNetworkPrototype {
    private static final Pattern ID_PATTERN = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");
    private static final Set<String> DIRECTIONS = Set.of("bidirectional", "client_to_server", "internal", "server_to_client");

    EchoNativeNetworkPrototypeOutcome prototype(
            String packId,
            Path fixture,
            List<String> discoveredModules,
            Path registrySafetyPath,
            Path registryConflictPath,
            Path phase13PrototypeSafetyGatePath
    ) throws IOException {
        List<EchoNativeDiagnostic> diagnostics = new ArrayList<>();
        Map<String, Object> registrySafety = readRequiredReport(registrySafetyPath, fixture, packId, "ECHO-NATIVE-REGISTRY-SAFETY-MISSING", "Registry bridge safety report missing", diagnostics);
        Map<String, Object> registryConflict = readRequiredReport(registryConflictPath, fixture, packId, "ECHO-NATIVE-REGISTRY-CONFLICT-REPORT-MISSING", "Registry conflict report missing", diagnostics);
        Map<String, Object> prototypeSafetyGate = readRequiredReport(phase13PrototypeSafetyGatePath, fixture, packId, "ECHO-NATIVE-PHASE13-PROTOTYPE-SAFETY-MISSING", "Phase 13 prototype safety gate missing", diagnostics);

        checkUpstream(registrySafety, EchoNativeJson.asObject(registrySafety.get("data")), registrySafetyPath, packId, "ECHO-NATIVE-REGISTRY-SAFETY-BLOCKED", "Registry bridge safety is not ready for network prototyping", diagnostics);
        checkRegistryConflictReport(registryConflict, EchoNativeJson.asObject(registryConflict.get("data")), registryConflictPath, packId, diagnostics);
        checkUpstream(prototypeSafetyGate, EchoNativeJson.asObject(prototypeSafetyGate.get("data")), phase13PrototypeSafetyGatePath, packId, "ECHO-NATIVE-PROTOTYPE-SAFETY-BLOCKED", "Phase 13 prototype safety gate is not ready for network prototyping", diagnostics);

        NetworkManifest manifest = readNetworkManifest(fixture.resolve("network").resolve("echo.native.network.json"), fixture, packId, Set.copyOf(discoveredModules), diagnostics);
        diagnostics = unique(diagnostics);
        boolean ready = diagnostics.isEmpty();

        List<Map<String, Object>> channels = ready ? manifest.channels() : List.of();
        List<Map<String, Object>> packets = ready ? manifest.packets() : List.of();
        List<Map<String, Object>> schemas = ready ? schemas(packets) : List.of();
        List<Map<String, Object>> conflicts = ready ? conflicts(channels, packets) : List.of();
        boolean conflictFree = ready && conflicts.isEmpty();
        boolean safe = ready && conflictFree;

        if (ready && !conflictFree) {
            diagnostics = unique(withConflictDiagnostics(diagnostics, conflicts, packId));
            safe = false;
        }

        EchoNativeNetworkChannelInventory inventory = new EchoNativeNetworkChannelInventory(
                "phase13.m14.network.channel.inventory",
                ready,
                true,
                true,
                false,
                false,
                false,
                false,
                channels.size(),
                channels
        );
        EchoNativeNetworkPacketValidation validation = new EchoNativeNetworkPacketValidation(
                "phase13.m14.network.packet.validation",
                ready,
                true,
                false,
                packets.size(),
                channels.size(),
                directions(channels, packets),
                validatedPackets(packets)
        );
        EchoNativeNetworkSchemaModel schemaModel = new EchoNativeNetworkSchemaModel(
                "phase13.m14.network.schema.model",
                ready,
                true,
                false,
                false,
                false,
                schemas.size(),
                schemas
        );
        EchoNativeNetworkConflictReport conflictReport = new EchoNativeNetworkConflictReport(
                "phase13.m14.network.conflict.report",
                conflictFree,
                true,
                false,
                conflicts.size(),
                conflicts.size(),
                conflicts
        );
        EchoNativeNetworkBridgePrototypeSafetyStatus safetyStatus = new EchoNativeNetworkBridgePrototypeSafetyStatus(
                "phase13.m14.network.bridge.safety.status",
                safe,
                true,
                true,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                safe ? List.of(
                        "registry_bridge_safety_gate",
                        "registry_conflict_gate",
                        "phase13_prototype_safety_gate",
                        "network_manifest_read",
                        "channel_inventory",
                        "packet_validation",
                        "schema_model",
                        "conflict_scan"
                ) : List.of()
        );

        return new EchoNativeNetworkPrototypeOutcome(
                packId,
                networkChannelInventory(packId, inventory, diagnostics),
                networkPacketValidation(packId, validation, diagnostics),
                networkSchemaModel(packId, schemaModel, diagnostics),
                networkConflictReport(packId, conflictReport, diagnostics),
                networkBridgeSafetyStatus(packId, safetyStatus, diagnostics),
                diagnostics
        );
    }

    private static NetworkManifest readNetworkManifest(
            Path manifestPath,
            Path fixture,
            String packId,
            Set<String> discoveredModules,
            List<EchoNativeDiagnostic> diagnostics
    ) throws IOException {
        if (!Files.isRegularFile(manifestPath)) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-NETWORK-MANIFEST-MISSING",
                    EchoNativeIssueSeverity.ERROR,
                    "Native network manifest missing",
                    "Network prototyping requires a fixture-local network/echo.native.network.json manifest.",
                    null,
                    packId,
                    List.of(fixture.resolve("network/echo.native.network.json").toString().replace('\\', '/')),
                    "Add a fixture-local network manifest or keep M14 blocked for this fixture."
            ));
            return new NetworkManifest(List.of(), List.of());
        }
        Map<String, Object> manifest;
        try {
            manifest = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(manifestPath)));
        } catch (RuntimeException ex) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-NETWORK-MANIFEST-INVALID",
                    EchoNativeIssueSeverity.ERROR,
                    "Native network manifest is invalid JSON",
                    ex.getMessage(),
                    null,
                    packId,
                    List.of(relativeReportPath(manifestPath)),
                    "Fix the fixture-local network manifest JSON."
            ));
            return new NetworkManifest(List.of(), List.of());
        }
        if (!"echo.native.network_manifest.v1".equals(manifest.get("schema"))) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-NETWORK-MANIFEST-SCHEMA",
                    EchoNativeIssueSeverity.ERROR,
                    "Unsupported native network manifest schema",
                    "Network manifest schema was '" + manifest.get("schema") + "'.",
                    null,
                    packId,
                    List.of(relativeReportPath(manifestPath)),
                    "Use schema echo.native.network_manifest.v1."
            ));
        }
        Map<String, Object> sourcePolicy = EchoNativeJson.asObject(manifest.get("sourcePolicy"));
        if (!Boolean.TRUE.equals(sourcePolicy.get("localOnly"))
                || !Boolean.TRUE.equals(sourcePolicy.get("descriptorOnly"))
                || Boolean.TRUE.equals(sourcePolicy.get("liveNetworkingAllowed"))
                || Boolean.TRUE.equals(sourcePolicy.get("socketAllowed"))
                || Boolean.TRUE.equals(sourcePolicy.get("clientConnectionAllowed"))
                || Boolean.TRUE.equals(sourcePolicy.get("serverConnectionAllowed"))
                || Boolean.TRUE.equals(sourcePolicy.get("packetSendAllowed"))
                || Boolean.TRUE.equals(sourcePolicy.get("packetReceiveAllowed"))) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-NETWORK-SOURCE-POLICY-UNSAFE",
                    EchoNativeIssueSeverity.ERROR,
                    "Native network source policy is unsafe",
                    "M14 requires descriptor-only local metadata with live networking, sockets, connections, and packet I/O disabled.",
                    null,
                    packId,
                    List.of(relativeReportPath(manifestPath)),
                    "Keep network bridge prototyping fixture-local and data-only."
            ));
        }

        List<Map<String, Object>> channels = new ArrayList<>();
        Object rawChannels = manifest.get("channels");
        if (rawChannels instanceof List<?> list) {
            for (Object item : list) {
                Map<String, Object> descriptor = EchoNativeJson.asObject(item);
                Map<String, Object> channel = readChannelDescriptor(descriptor, fixture, manifestPath, packId, discoveredModules, diagnostics);
                if (!channel.isEmpty()) {
                    channels.add(channel);
                }
            }
        }
        channels.sort(Comparator.comparing(item -> String.valueOf(item.get("id"))));
        if (channels.isEmpty()) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-NETWORK-CHANNELS-EMPTY",
                    EchoNativeIssueSeverity.ERROR,
                    "Native network manifest has no usable channels",
                    "Network prototyping needs at least one fixture-local channel descriptor.",
                    null,
                    packId,
                    List.of(relativeReportPath(manifestPath)),
                    "Add descriptor-only channel metadata."
            ));
        }

        Set<String> channelIds = channels.stream().map(item -> String.valueOf(item.get("id"))).collect(Collectors.toCollection(LinkedHashSet::new));
        List<Map<String, Object>> packets = new ArrayList<>();
        Object rawPackets = manifest.get("packets");
        if (rawPackets instanceof List<?> list) {
            for (Object item : list) {
                Map<String, Object> descriptor = EchoNativeJson.asObject(item);
                Map<String, Object> packet = readPacketDescriptor(descriptor, fixture, manifestPath, packId, discoveredModules, channelIds, diagnostics);
                if (!packet.isEmpty()) {
                    packets.add(packet);
                }
            }
        }
        packets.sort(Comparator.<Map<String, Object>, String>comparing(item -> String.valueOf(item.get("channelId")))
                .thenComparing(item -> String.valueOf(item.get("id"))));
        if (packets.isEmpty()) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-NETWORK-PACKETS-EMPTY",
                    EchoNativeIssueSeverity.ERROR,
                    "Native network manifest has no usable packets",
                    "Network prototyping needs at least one fixture-local packet descriptor.",
                    null,
                    packId,
                    List.of(relativeReportPath(manifestPath)),
                    "Add descriptor-only packet metadata."
            ));
        }
        return new NetworkManifest(List.copyOf(channels), List.copyOf(packets));
    }

    private static Map<String, Object> readChannelDescriptor(
            Map<String, Object> descriptor,
            Path fixture,
            Path manifestPath,
            String packId,
            Set<String> discoveredModules,
            List<EchoNativeDiagnostic> diagnostics
    ) throws IOException {
        String id = string(descriptor.get("id"));
        String ownerModule = string(descriptor.get("ownerModule"));
        String direction = string(descriptor.get("direction"));
        String sourcePath = string(descriptor.get("sourcePath")).replace('\\', '/');
        int protocolVersion = number(descriptor.get("protocolVersion"), -1);
        if (id.isBlank() || ownerModule.isBlank() || direction.isBlank() || sourcePath.isBlank() || protocolVersion < 1) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-NETWORK-CHANNEL-INCOMPLETE",
                    EchoNativeIssueSeverity.ERROR,
                    "Native network channel descriptor is incomplete",
                    "Each channel requires id, ownerModule, direction, protocolVersion, and sourcePath.",
                    id.isBlank() ? null : id,
                    packId,
                    List.of(relativeReportPath(manifestPath)),
                    "Complete the fixture-local channel descriptor."
            ));
            return Map.of();
        }
        if (!validateCommon(id, ownerModule, direction, sourcePath, manifestPath, fixture, packId, discoveredModules, "channel", diagnostics)) {
            return Map.of();
        }
        Map<String, Object> source = readSource(fixture.resolve(sourcePath).normalize(), id, "channel", packId, diagnostics);
        if (source.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> channel = new LinkedHashMap<>();
        channel.put("id", id);
        channel.put("ownerModule", ownerModule);
        channel.put("direction", direction);
        channel.put("protocolVersion", protocolVersion);
        channel.put("required", Boolean.TRUE.equals(descriptor.get("required")));
        channel.put("sourcePath", sourcePath);
        channel.put("summary", string(source.get("displayName")));
        channel.put("descriptorOnly", true);
        channel.put("liveNetworkingStarted", false);
        channel.put("socketOpened", false);
        channel.put("clientConnectionOpened", false);
        channel.put("serverConnectionOpened", false);
        return channel;
    }

    private static Map<String, Object> readPacketDescriptor(
            Map<String, Object> descriptor,
            Path fixture,
            Path manifestPath,
            String packId,
            Set<String> discoveredModules,
            Set<String> channelIds,
            List<EchoNativeDiagnostic> diagnostics
    ) throws IOException {
        String id = string(descriptor.get("id"));
        String ownerModule = string(descriptor.get("ownerModule"));
        String channelId = string(descriptor.get("channelId"));
        String direction = string(descriptor.get("direction"));
        String schemaId = string(descriptor.get("schemaId"));
        String sourcePath = string(descriptor.get("sourcePath")).replace('\\', '/');
        if (id.isBlank() || ownerModule.isBlank() || channelId.isBlank() || direction.isBlank() || schemaId.isBlank() || sourcePath.isBlank()) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-NETWORK-PACKET-INCOMPLETE",
                    EchoNativeIssueSeverity.ERROR,
                    "Native network packet descriptor is incomplete",
                    "Each packet requires id, ownerModule, channelId, direction, schemaId, and sourcePath.",
                    id.isBlank() ? null : id,
                    packId,
                    List.of(relativeReportPath(manifestPath)),
                    "Complete the fixture-local packet descriptor."
            ));
            return Map.of();
        }
        if (!validateCommon(id, ownerModule, direction, sourcePath, manifestPath, fixture, packId, discoveredModules, "packet", diagnostics)) {
            return Map.of();
        }
        if (!ID_PATTERN.matcher(channelId).matches() || !channelIds.contains(channelId)) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-NETWORK-PACKET-CHANNEL-MISSING",
                    EchoNativeIssueSeverity.ERROR,
                    "Native network packet references an unknown channel",
                    "Packet '" + id + "' references channel '" + channelId + "', which was not inventoried.",
                    id,
                    packId,
                    List.of(relativeReportPath(manifestPath)),
                    "Declare the channel before packet validation."
            ));
            return Map.of();
        }
        if (!ID_PATTERN.matcher(schemaId).matches()) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-NETWORK-SCHEMA-ID-INVALID",
                    EchoNativeIssueSeverity.ERROR,
                    "Native network packet schema id is invalid",
                    "Schema id '" + schemaId + "' must use namespace:path lowercase syntax.",
                    id,
                    packId,
                    List.of(relativeReportPath(manifestPath)),
                    "Use lowercase namespace:path schema ids."
            ));
            return Map.of();
        }
        Map<String, Object> source = readSource(fixture.resolve(sourcePath).normalize(), id, "packet", packId, diagnostics);
        if (source.isEmpty() || !channelId.equals(source.get("channelId")) || !schemaId.equals(source.get("schemaId"))) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-NETWORK-PACKET-SOURCE-MISMATCH",
                    EchoNativeIssueSeverity.ERROR,
                    "Native network packet source does not match descriptor",
                    "Packet source channelId/schemaId must match the manifest descriptor.",
                    id,
                    packId,
                    List.of(sourcePath),
                    "Keep fixture packet sources aligned with the manifest."
            ));
            return Map.of();
        }
        Map<String, Object> packet = new LinkedHashMap<>();
        packet.put("id", id);
        packet.put("ownerModule", ownerModule);
        packet.put("channelId", channelId);
        packet.put("direction", direction);
        packet.put("schemaId", schemaId);
        packet.put("required", Boolean.TRUE.equals(descriptor.get("required")));
        packet.put("sourcePath", sourcePath);
        packet.put("summary", string(source.get("displayName")));
        packet.put("descriptorOnly", true);
        packet.put("packetSent", false);
        packet.put("packetReceived", false);
        return packet;
    }

    private static boolean validateCommon(
            String id,
            String ownerModule,
            String direction,
            String sourcePath,
            Path manifestPath,
            Path fixture,
            String packId,
            Set<String> discoveredModules,
            String type,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        if (!ID_PATTERN.matcher(id).matches()) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-NETWORK-ID-INVALID",
                    EchoNativeIssueSeverity.ERROR,
                    "Native network " + type + " id is invalid",
                    "Network id '" + id + "' must use namespace:path lowercase syntax.",
                    id,
                    packId,
                    List.of(relativeReportPath(manifestPath)),
                    "Use lowercase namespace:path ids."
            ));
            return false;
        }
        if (!discoveredModules.contains(ownerModule)) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-NETWORK-OWNER-MISSING",
                    EchoNativeIssueSeverity.ERROR,
                    "Native network " + type + " owner module is missing",
                    "Owner module '" + ownerModule + "' is not present in discovered descriptors.",
                    id,
                    packId,
                    List.of(relativeReportPath(manifestPath)),
                    "Use an owner module discovered in the fixture."
            ));
            return false;
        }
        if (!DIRECTIONS.contains(direction)) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-NETWORK-DIRECTION-INVALID",
                    EchoNativeIssueSeverity.ERROR,
                    "Native network " + type + " direction is invalid",
                    "Direction '" + direction + "' is not supported by M14.",
                    id,
                    packId,
                    List.of(relativeReportPath(manifestPath)),
                    "Use bidirectional, client_to_server, server_to_client, or internal."
            ));
            return false;
        }
        if (isUnsafeRelativePath(sourcePath)) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-NETWORK-PATH-UNSAFE",
                    EchoNativeIssueSeverity.ERROR,
                    "Native network descriptor path is unsafe",
                    "Network source paths must be fixture-relative and must not escape the fixture.",
                    id,
                    packId,
                    List.of(relativeReportPath(manifestPath)),
                    "Keep M14 network descriptors fixture-local and repo-relative."
            ));
            return false;
        }
        Path source = fixture.resolve(sourcePath).normalize().toAbsolutePath();
        if (!source.startsWith(fixture.toAbsolutePath().normalize()) || !Files.isRegularFile(source)) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-NETWORK-SOURCE-MISSING",
                    EchoNativeIssueSeverity.ERROR,
                    "Native network source file missing",
                    "Network source '" + sourcePath + "' was not found under the fixture.",
                    id,
                    packId,
                    List.of(sourcePath),
                    "Add the fixture-local network source file or remove the descriptor."
            ));
            return false;
        }
        return true;
    }

    private static Map<String, Object> readSource(Path sourcePath, String id, String type, String packId, List<EchoNativeDiagnostic> diagnostics) throws IOException {
        Map<String, Object> source;
        try {
            source = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(sourcePath)));
        } catch (RuntimeException ex) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-NETWORK-SOURCE-INVALID",
                    EchoNativeIssueSeverity.ERROR,
                    "Native network source is invalid JSON",
                    ex.getMessage(),
                    id,
                    packId,
                    List.of(relativeReportPath(sourcePath)),
                    "Fix the fixture-local network source JSON."
            ));
            return Map.of();
        }
        if (!id.equals(source.get("id")) || !type.equals(source.get("descriptorType"))) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-NETWORK-SOURCE-MISMATCH",
                    EchoNativeIssueSeverity.ERROR,
                    "Native network source does not match descriptor",
                    "Network source id/descriptorType must match the manifest descriptor.",
                    id,
                    packId,
                    List.of(relativeReportPath(sourcePath)),
                    "Keep fixture network sources aligned with the manifest."
            ));
            return Map.of();
        }
        return source;
    }

    private static void checkUpstream(
            Map<String, Object> report,
            Map<String, Object> data,
            Path path,
            String packId,
            String code,
            String title,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        if (report.isEmpty()) {
            return;
        }
        if (!"PASS".equals(report.get("status")) || hasUnsafeRuntimeWork(data)) {
            diagnostics.add(new EchoNativeDiagnostic(
                    code,
                    EchoNativeIssueSeverity.ERROR,
                    title,
                    "M14 network prototyping requires PASS upstream safety reports with no unsafe runtime work.",
                    null,
                    packId,
                    List.of(relativeReportPath(path)),
                    "Regenerate M13 registry and Phase 13 safety reports before network prototyping."
            ));
        }
        diagnostics.addAll(reportDiagnostics(report, packId));
    }

    private static void checkRegistryConflictReport(
            Map<String, Object> report,
            Map<String, Object> data,
            Path path,
            String packId,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        if (report.isEmpty()) {
            return;
        }
        boolean pass = "PASS".equals(report.get("status"));
        boolean conflictFree = Boolean.TRUE.equals(data.get("conflictFree"));
        if (!pass || !conflictFree || hasUnsafeRuntimeWork(data)) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-REGISTRY-CONFLICT-GATE-BLOCKED",
                    EchoNativeIssueSeverity.ERROR,
                    "Registry conflict report is not ready for network prototyping",
                    "M14 requires PASS registry-conflict-report.json with conflictFree=true and no unsafe runtime work.",
                    null,
                    packId,
                    List.of(relativeReportPath(path)),
                    "Resolve registry conflicts before network bridge prototyping."
            ));
        }
        diagnostics.addAll(reportDiagnostics(report, packId));
    }

    private static List<Map<String, Object>> validatedPackets(List<Map<String, Object>> packets) {
        return packets.stream().map(packet -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", packet.get("id"));
            item.put("channelId", packet.get("channelId"));
            item.put("direction", packet.get("direction"));
            item.put("ownerModule", packet.get("ownerModule"));
            item.put("schemaId", packet.get("schemaId"));
            item.put("valid", true);
            return item;
        }).toList();
    }

    private static List<Map<String, Object>> schemas(List<Map<String, Object>> packets) {
        return packets.stream().map(packet -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("channelId", packet.get("channelId"));
            item.put("descriptorOnly", true);
            item.put("packetId", packet.get("id"));
            item.put("packetReceived", false);
            item.put("packetSent", false);
            item.put("schemaId", packet.get("schemaId"));
            return item;
        }).toList();
    }

    private static List<Map<String, Object>> conflicts(List<Map<String, Object>> channels, List<Map<String, Object>> packets) {
        List<Map<String, Object>> conflicts = new ArrayList<>();
        addDuplicateConflicts(conflicts, channels, "channel", "id");
        addDuplicateConflicts(conflicts, packets, "packet", "id");
        addDuplicateConflicts(conflicts, packets, "schema", "schemaId");
        conflicts.sort(Comparator.comparing(item -> String.valueOf(item.get("conflictKey"))));
        return List.copyOf(conflicts);
    }

    private static void addDuplicateConflicts(List<Map<String, Object>> conflicts, List<Map<String, Object>> items, String type, String keyName) {
        Map<String, List<Map<String, Object>>> byKey = items.stream()
                .collect(Collectors.groupingBy(item -> String.valueOf(item.get(keyName)), LinkedHashMap::new, Collectors.toList()));
        for (Map.Entry<String, List<Map<String, Object>>> entry : byKey.entrySet()) {
            if (entry.getValue().size() <= 1) {
                continue;
            }
            Map<String, Object> conflict = new LinkedHashMap<>();
            conflict.put("blocking", true);
            conflict.put("conflictKey", type + ":" + entry.getKey());
            conflict.put("ids", entry.getValue().stream().map(item -> String.valueOf(item.get("id"))).sorted().toList());
            conflicts.add(conflict);
        }
    }

    private static List<EchoNativeDiagnostic> withConflictDiagnostics(
            List<EchoNativeDiagnostic> diagnostics,
            List<Map<String, Object>> conflicts,
            String packId
    ) {
        List<EchoNativeDiagnostic> result = new ArrayList<>(diagnostics);
        for (Map<String, Object> conflict : conflicts) {
            result.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-NETWORK-CONFLICT",
                    EchoNativeIssueSeverity.ERROR,
                    "Native network descriptor conflict detected",
                    "Multiple fixture network descriptors target " + conflict.get("conflictKey") + ".",
                    null,
                    packId,
                    EchoNativeJson.stringList(conflict.get("ids")),
                    "Resolve duplicate channel, packet, or schema ids before enabling network bridge prototypes."
            ));
        }
        return result;
    }

    private static List<String> directions(List<Map<String, Object>> channels, List<Map<String, Object>> packets) {
        Set<String> directions = new LinkedHashSet<>();
        channels.stream().map(item -> String.valueOf(item.get("direction"))).sorted().forEach(directions::add);
        packets.stream().map(item -> String.valueOf(item.get("direction"))).sorted().forEach(directions::add);
        return List.copyOf(directions);
    }

    private static Map<String, Object> networkChannelInventory(
            String packId,
            EchoNativeNetworkChannelInventory inventory,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m14_network_channel_inventory", diagnostics);
        data.put("channelCount", inventory.channelCount());
        data.put("channels", inventory.channels());
        data.put("clientConnectionAllowed", inventory.clientConnectionAllowed());
        data.put("descriptorOnly", inventory.descriptorOnly());
        data.put("inventoryId", inventory.inventoryId());
        data.put("inventoried", inventory.inventoried());
        data.put("liveNetworkingAllowed", inventory.liveNetworkingAllowed());
        data.put("localOnly", inventory.localOnly());
        data.put("packId", packId);
        data.put("serverConnectionAllowed", inventory.serverConnectionAllowed());
        data.put("socketAllowed", inventory.socketAllowed());
        data.put("summary", inventory.inventoried()
                ? "Fixture-local native network channels were inventoried as descriptors only."
                : "Network channel inventory is blocked by diagnostics.");
        return data;
    }

    private static Map<String, Object> networkPacketValidation(
            String packId,
            EchoNativeNetworkPacketValidation validation,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m14_network_packet_validation", diagnostics);
        data.put("channelCount", validation.channelCount());
        data.put("descriptorOnly", validation.descriptorOnly());
        data.put("directions", validation.directions());
        data.put("liveNetworkingStarted", validation.liveNetworkingStarted());
        data.put("packId", packId);
        data.put("packetCount", validation.packetCount());
        data.put("packets", validation.packets());
        data.put("summary", validation.valid()
                ? "Network packet descriptors validated successfully without live networking."
                : "Network packet validation is blocked by diagnostics.");
        data.put("valid", validation.valid());
        data.put("validationId", validation.validationId());
        return data;
    }

    private static Map<String, Object> networkSchemaModel(
            String packId,
            EchoNativeNetworkSchemaModel model,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m14_network_schema_model", diagnostics);
        data.put("descriptorOnly", model.descriptorOnly());
        data.put("liveNetworkingStarted", model.liveNetworkingStarted());
        data.put("modelId", model.modelId());
        data.put("modeled", model.modeled());
        data.put("packId", packId);
        data.put("packetReceived", model.packetReceived());
        data.put("packetSent", model.packetSent());
        data.put("schemaCount", model.schemaCount());
        data.put("schemas", model.schemas());
        data.put("summary", model.modeled()
                ? "Network packet schemas were modeled as data only."
                : "Network schema model is blocked by diagnostics.");
        return data;
    }

    private static Map<String, Object> networkConflictReport(
            String packId,
            EchoNativeNetworkConflictReport report,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m14_network_conflict_report", diagnostics);
        data.put("blockingConflictCount", report.blockingConflictCount());
        data.put("conflictCount", report.conflictCount());
        data.put("conflictFree", report.conflictFree());
        data.put("conflicts", report.conflicts());
        data.put("descriptorOnly", report.descriptorOnly());
        data.put("liveNetworkingStarted", report.liveNetworkingStarted());
        data.put("packId", packId);
        data.put("reportId", report.reportId());
        data.put("summary", report.conflictFree()
                ? "No duplicate network channel, packet, or schema ids were found."
                : "Network descriptor conflicts block network bridge prototyping.");
        return data;
    }

    private static Map<String, Object> networkBridgeSafetyStatus(
            String packId,
            EchoNativeNetworkBridgePrototypeSafetyStatus status,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m14_network_bridge_safety_status", diagnostics);
        data.put("classloaderCreated", status.classloaderCreated());
        data.put("clientConnectionOpened", status.clientConnectionOpened());
        data.put("commandExecuted", status.commandExecuted());
        data.put("completedChecks", status.completedChecks());
        data.put("descriptorOnly", status.descriptorOnly());
        data.put("filesystemMutated", status.filesystemMutated());
        data.put("gameClassesResolved", status.gameClassesResolved());
        data.put("liveNetworkingStarted", status.liveNetworkingStarted());
        data.put("localOnly", status.localOnly());
        data.put("packId", packId);
        data.put("packetReceived", status.packetReceived());
        data.put("packetSent", status.packetSent());
        data.put("processLaunched", status.processLaunched());
        data.put("registryInjected", status.registryInjected());
        data.put("registryMutated", status.registryMutated());
        data.put("safeToContinue", status.safeToContinue());
        data.put("serverConnectionOpened", status.serverConnectionOpened());
        data.put("socketOpened", status.socketOpened());
        data.put("statusId", status.statusId());
        data.put("summary", status.safeToContinue()
                ? "M14 network bridge prototype stayed descriptor-only and safe to continue."
                : "M14 network bridge prototype is blocked by diagnostics.");
        return data;
    }

    private static Map<String, Object> base(String phase, List<EchoNativeDiagnostic> diagnostics) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("classloaderCreated", false);
        data.put("clientConnectionOpened", false);
        data.put("commandExecuted", false);
        data.put("descriptorOnly", true);
        data.put("diagnosticCount", diagnostics.size());
        data.put("dryRunOnly", true);
        data.put("filesystemMutated", false);
        data.put("gameClassesResolved", false);
        data.put("gameProcessLaunched", false);
        data.put("liveNetworkingStarted", false);
        data.put("minecraftLaunched", false);
        data.put("packetReceived", false);
        data.put("packetSent", false);
        data.put("phase", phase);
        data.put("processLaunched", false);
        data.put("registryInjected", false);
        data.put("registryMutated", false);
        data.put("serverConnectionOpened", false);
        data.put("simulationOnly", true);
        data.put("socketOpened", false);
        data.put("unsafeRuntimeWorkStarted", false);
        return data;
    }

    private static boolean hasUnsafeRuntimeWork(Map<String, Object> data) {
        return Boolean.TRUE.equals(data.get("liveNetworkingStarted"))
                || Boolean.TRUE.equals(data.get("networkStarted"))
                || Boolean.TRUE.equals(data.get("socketOpened"))
                || Boolean.TRUE.equals(data.get("clientConnectionOpened"))
                || Boolean.TRUE.equals(data.get("serverConnectionOpened"))
                || Boolean.TRUE.equals(data.get("packetSent"))
                || Boolean.TRUE.equals(data.get("packetReceived"))
                || Boolean.TRUE.equals(data.get("minecraftRegistryTouched"))
                || Boolean.TRUE.equals(data.get("registryInjected"))
                || Boolean.TRUE.equals(data.get("registryMutated"))
                || Boolean.TRUE.equals(data.get("resourceRuntimeAccessed"))
                || Boolean.TRUE.equals(data.get("minecraftResourceManagerTouched"))
                || Boolean.TRUE.equals(data.get("addonCodeExecuted"))
                || Boolean.TRUE.equals(data.get("realAddonCodeExecuted"))
                || Boolean.TRUE.equals(data.get("serviceCodeExecuted"))
                || Boolean.TRUE.equals(data.get("classloaderCreated"))
                || Boolean.TRUE.equals(data.get("productionClassloader"))
                || Boolean.TRUE.equals(data.get("resolvesRuntimeClasses"))
                || Boolean.TRUE.equals(data.get("gameClassesResolved"))
                || Boolean.TRUE.equals(data.get("minecraftClassesResolved"))
                || Boolean.TRUE.equals(data.get("gameProcessLaunched"))
                || Boolean.TRUE.equals(data.get("minecraftLaunched"))
                || Boolean.TRUE.equals(data.get("processLaunched"))
                || Boolean.TRUE.equals(data.get("commandExecuted"))
                || Boolean.TRUE.equals(data.get("filesystemMutated"))
                || Boolean.TRUE.equals(data.get("unsafeRuntimeWorkStarted"));
    }

    private static Map<String, Object> readRequiredReport(
            Path reportPath,
            Path fixture,
            String packId,
            String missingCode,
            String missingTitle,
            List<EchoNativeDiagnostic> diagnostics
    ) throws IOException {
        if (!Files.isRegularFile(reportPath)) {
            diagnostics.add(new EchoNativeDiagnostic(
                    missingCode,
                    EchoNativeIssueSeverity.ERROR,
                    missingTitle,
                    "Required M14 network prototype input report was not found.",
                    null,
                    packId,
                    List.of(fixture.toString().replace('\\', '/')),
                    "Generate M13 registry and Phase 13 safety reports before network prototyping."
            ));
            return Map.of();
        }
        return EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(reportPath)));
    }

    private static List<EchoNativeDiagnostic> reportDiagnostics(Map<String, Object> report, String packId) {
        Object issues = report.get("issues");
        if (!(issues instanceof List<?> list)) {
            return List.of();
        }
        return list.stream()
                .filter(Map.class::isInstance)
                .map(item -> EchoNativeJson.asObject(item))
                .sorted(Comparator.comparing(item -> String.valueOf(item.get("code")) + ":" + item.get("summary")))
                .map(item -> new EchoNativeDiagnostic(
                        String.valueOf(item.getOrDefault("code", "ECHO-NATIVE-UPSTREAM-DIAGNOSTIC")),
                        EchoNativeIssueSeverity.ERROR,
                        String.valueOf(item.getOrDefault("title", "Upstream diagnostic")),
                        String.valueOf(item.getOrDefault("summary", "Upstream Phase 13 report is not PASS.")),
                        item.get("moduleId") == null ? null : String.valueOf(item.get("moduleId")),
                        packId,
                        EchoNativeJson.stringList(item.get("likelyFiles")),
                        String.valueOf(item.getOrDefault("suggestedFix", "Resolve upstream diagnostics first."))
                ))
                .toList();
    }

    private static boolean isUnsafeRelativePath(String path) {
        return path.isBlank()
                || path.startsWith("/")
                || path.matches("^[A-Za-z]:.*")
                || path.contains("..")
                || path.startsWith("~");
    }

    private static int number(Object value, int fallback) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        return fallback;
    }

    private static String string(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private static List<EchoNativeDiagnostic> unique(List<EchoNativeDiagnostic> diagnostics) {
        Map<String, EchoNativeDiagnostic> byKey = new LinkedHashMap<>();
        for (EchoNativeDiagnostic diagnostic : diagnostics) {
            byKey.put(diagnostic.code() + "|" + diagnostic.moduleId() + "|" + diagnostic.summary(), diagnostic);
        }
        return byKey.values().stream()
                .sorted(Comparator.comparing(EchoNativeDiagnostic::code)
                        .thenComparing(diagnostic -> diagnostic.moduleId() == null ? "" : diagnostic.moduleId())
                        .thenComparing(EchoNativeDiagnostic::summary))
                .toList();
    }

    private static String relativeReportPath(Path path) {
        Path root = Path.of("").toAbsolutePath().normalize();
        Path normalized = path.toAbsolutePath().normalize();
        if (normalized.startsWith(root)) {
            return root.relativize(normalized).toString().replace('\\', '/');
        }
        return path.toString().replace('\\', '/');
    }

    private record NetworkManifest(List<Map<String, Object>> channels, List<Map<String, Object>> packets) {
    }
}
