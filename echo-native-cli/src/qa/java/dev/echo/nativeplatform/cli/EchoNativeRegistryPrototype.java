package dev.echo.nativeplatform.cli;

import dev.echo.nativeplatform.contracts.EchoNativeDiagnostic;
import dev.echo.nativeplatform.contracts.EchoNativeIssueSeverity;
import dev.echo.nativeplatform.contracts.EchoNativeRegistryBridgePrototypeSafetyStatus;
import dev.echo.nativeplatform.contracts.EchoNativeRegistryConflictReport;
import dev.echo.nativeplatform.contracts.EchoNativeRegistryIdValidation;
import dev.echo.nativeplatform.contracts.EchoNativeRegistrySourceInventory;
import dev.echo.nativeplatform.contracts.EchoNativeSandboxRegistryModel;
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

final class EchoNativeRegistryPrototype {
    private static final Pattern REGISTRY_ID_PATTERN = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");
    private static final Set<String> SUPPORTED_KINDS = Set.of("block", "feature", "item", "service");

    EchoNativeRegistryPrototypeOutcome prototype(
            String packId,
            Path fixture,
            Path resourceBridgeSafetyPath,
            Path resourceConflictPath,
            Path phase13PrototypeSafetyGatePath
    ) throws IOException {
        List<EchoNativeDiagnostic> diagnostics = new ArrayList<>();
        Map<String, Object> resourceBridgeSafety = readRequiredReport(resourceBridgeSafetyPath, fixture, packId, "ECHO-NATIVE-RESOURCE-BRIDGE-SAFETY-MISSING", "Resource bridge safety report missing", diagnostics);
        Map<String, Object> resourceConflict = readRequiredReport(resourceConflictPath, fixture, packId, "ECHO-NATIVE-RESOURCE-CONFLICT-REPORT-MISSING", "Resource conflict report missing", diagnostics);
        Map<String, Object> prototypeSafetyGate = readRequiredReport(phase13PrototypeSafetyGatePath, fixture, packId, "ECHO-NATIVE-PHASE13-PROTOTYPE-SAFETY-MISSING", "Phase 13 prototype safety gate missing", diagnostics);

        checkUpstream(resourceBridgeSafety, EchoNativeJson.asObject(resourceBridgeSafety.get("data")), resourceBridgeSafetyPath, packId, "ECHO-NATIVE-RESOURCE-BRIDGE-SAFETY-BLOCKED", "Resource bridge safety is not ready for registry prototyping", diagnostics);
        checkResourceConflictReport(resourceConflict, EchoNativeJson.asObject(resourceConflict.get("data")), resourceConflictPath, packId, diagnostics);
        checkUpstream(prototypeSafetyGate, EchoNativeJson.asObject(prototypeSafetyGate.get("data")), phase13PrototypeSafetyGatePath, packId, "ECHO-NATIVE-PROTOTYPE-SAFETY-BLOCKED", "Phase 13 prototype safety gate is not ready for registry prototyping", diagnostics);

        RegistryManifest manifest = readRegistryManifest(fixture.resolve("registries").resolve("echo.native.registries.json"), fixture, packId, diagnostics);
        diagnostics = unique(diagnostics);
        boolean ready = diagnostics.isEmpty();

        List<Map<String, Object>> entries = ready ? manifest.entries() : List.of();
        List<Map<String, Object>> validatedEntries = ready ? validatedEntries(entries) : List.of();
        List<Map<String, Object>> registries = ready ? registries(entries) : List.of();
        List<Map<String, Object>> conflicts = ready ? conflicts(entries) : List.of();
        boolean conflictFree = ready && conflicts.isEmpty();
        boolean safe = ready && conflictFree;

        if (ready && !conflictFree) {
            diagnostics = unique(withConflictDiagnostics(diagnostics, conflicts, packId));
            safe = false;
        }

        EchoNativeRegistrySourceInventory inventory = new EchoNativeRegistrySourceInventory(
                "phase13.m13.registry.source.inventory",
                ready,
                true,
                true,
                false,
                false,
                false,
                entries.size(),
                entries
        );
        EchoNativeRegistryIdValidation idValidation = new EchoNativeRegistryIdValidation(
                "phase13.m13.registry.id.validation",
                ready,
                true,
                false,
                registryKinds(entries).size(),
                validatedEntries.size(),
                registryKinds(entries),
                validatedEntries
        );
        EchoNativeSandboxRegistryModel model = new EchoNativeSandboxRegistryModel(
                "phase13.m13.sandbox.registry.model",
                ready,
                true,
                false,
                false,
                false,
                registryKinds(entries).size(),
                entries.size(),
                registries
        );
        EchoNativeRegistryConflictReport conflictReport = new EchoNativeRegistryConflictReport(
                "phase13.m13.registry.conflict.report",
                conflictFree,
                true,
                false,
                conflicts.size(),
                conflicts.size(),
                conflicts
        );
        EchoNativeRegistryBridgePrototypeSafetyStatus safetyStatus = new EchoNativeRegistryBridgePrototypeSafetyStatus(
                "phase13.m13.registry.bridge.safety.status",
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
                safe ? List.of(
                        "resource_bridge_safety_gate",
                        "resource_conflict_gate",
                        "phase13_prototype_safety_gate",
                        "registry_manifest_read",
                        "registry_id_validation",
                        "sandbox_registry_model",
                        "conflict_scan"
                ) : List.of()
        );

        return new EchoNativeRegistryPrototypeOutcome(
                packId,
                registrySourceInventory(packId, inventory, diagnostics),
                registryIdValidation(packId, idValidation, diagnostics),
                sandboxRegistryModel(packId, model, diagnostics),
                registryConflictReport(packId, conflictReport, diagnostics),
                registryBridgeSafetyStatus(packId, safetyStatus, diagnostics),
                diagnostics
        );
    }

    private static RegistryManifest readRegistryManifest(
            Path manifestPath,
            Path fixture,
            String packId,
            List<EchoNativeDiagnostic> diagnostics
    ) throws IOException {
        if (!Files.isRegularFile(manifestPath)) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-REGISTRY-MANIFEST-MISSING",
                    EchoNativeIssueSeverity.ERROR,
                    "Native registry manifest missing",
                    "Registry prototyping requires a fixture-local registries/echo.native.registries.json manifest.",
                    null,
                    packId,
                    List.of(fixture.resolve("registries/echo.native.registries.json").toString().replace('\\', '/')),
                    "Add a fixture-local registry manifest or keep M13 blocked for this fixture."
            ));
            return new RegistryManifest(List.of());
        }
        Map<String, Object> manifest;
        try {
            manifest = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(manifestPath)));
        } catch (RuntimeException ex) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-REGISTRY-MANIFEST-INVALID",
                    EchoNativeIssueSeverity.ERROR,
                    "Native registry manifest is invalid JSON",
                    ex.getMessage(),
                    null,
                    packId,
                    List.of(relativeReportPath(manifestPath)),
                    "Fix the fixture-local registry manifest JSON."
            ));
            return new RegistryManifest(List.of());
        }
        if (!"echo.native.registry_manifest.v1".equals(manifest.get("schema"))) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-REGISTRY-MANIFEST-SCHEMA",
                    EchoNativeIssueSeverity.ERROR,
                    "Unsupported native registry manifest schema",
                    "Registry manifest schema was '" + manifest.get("schema") + "'.",
                    null,
                    packId,
                    List.of(relativeReportPath(manifestPath)),
                    "Use schema echo.native.registry_manifest.v1."
            ));
        }
        Map<String, Object> sourcePolicy = EchoNativeJson.asObject(manifest.get("sourcePolicy"));
        if (!Boolean.TRUE.equals(sourcePolicy.get("localOnly"))
                || !Boolean.TRUE.equals(sourcePolicy.get("sandboxOnly"))
                || Boolean.TRUE.equals(sourcePolicy.get("minecraftRegistryAccessAllowed"))
                || Boolean.TRUE.equals(sourcePolicy.get("registryInjectionAllowed"))
                || Boolean.TRUE.equals(sourcePolicy.get("registryMutationAllowed"))) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-REGISTRY-SOURCE-POLICY-UNSAFE",
                    EchoNativeIssueSeverity.ERROR,
                    "Native registry source policy is unsafe",
                    "M13 requires localOnly=true, sandboxOnly=true, and Minecraft registry access/injection/mutation disabled.",
                    null,
                    packId,
                    List.of(relativeReportPath(manifestPath)),
                    "Keep registry bridge prototyping fixture-local and sandbox-only."
            ));
        }
        List<Map<String, Object>> entries = new ArrayList<>();
        Object rawEntries = manifest.get("entries");
        if (rawEntries instanceof List<?> list) {
            for (Object item : list) {
                Map<String, Object> descriptor = EchoNativeJson.asObject(item);
                Map<String, Object> entry = readRegistryDescriptor(descriptor, fixture, manifestPath, packId, diagnostics);
                if (!entry.isEmpty()) {
                    entries.add(entry);
                }
            }
        }
        entries.sort(Comparator.<Map<String, Object>, String>comparing(item -> String.valueOf(item.get("kind")))
                .thenComparing(item -> String.valueOf(item.get("id"))));
        if (entries.isEmpty()) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-REGISTRY-MANIFEST-EMPTY",
                    EchoNativeIssueSeverity.ERROR,
                    "Native registry manifest has no usable entries",
                    "Registry prototyping needs at least one fixture-local registry descriptor.",
                    null,
                    packId,
                    List.of(relativeReportPath(manifestPath)),
                    "Add registry descriptors and fixture-local registry entry files."
            ));
        }
        return new RegistryManifest(List.copyOf(entries));
    }

    private static Map<String, Object> readRegistryDescriptor(
            Map<String, Object> descriptor,
            Path fixture,
            Path manifestPath,
            String packId,
            List<EchoNativeDiagnostic> diagnostics
    ) throws IOException {
        String id = string(descriptor.get("id"));
        String kind = string(descriptor.get("kind"));
        String ownerModule = string(descriptor.get("ownerModule"));
        String sourcePath = string(descriptor.get("sourcePath")).replace('\\', '/');
        if (id.isBlank() || kind.isBlank() || ownerModule.isBlank() || sourcePath.isBlank()) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-REGISTRY-DESCRIPTOR-INCOMPLETE",
                    EchoNativeIssueSeverity.ERROR,
                    "Native registry descriptor is incomplete",
                    "Each registry entry requires id, kind, ownerModule, and sourcePath.",
                    id.isBlank() ? null : id,
                    packId,
                    List.of(relativeReportPath(manifestPath)),
                    "Complete the fixture-local registry descriptor."
            ));
            return Map.of();
        }
        if (!SUPPORTED_KINDS.contains(kind)) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-REGISTRY-KIND-UNSUPPORTED",
                    EchoNativeIssueSeverity.ERROR,
                    "Native registry kind is unsupported",
                    "M13 supports block, item, feature, and service registry descriptors only.",
                    id,
                    packId,
                    List.of(relativeReportPath(manifestPath)),
                    "Use a supported M13 sandbox registry kind."
            ));
            return Map.of();
        }
        if (!REGISTRY_ID_PATTERN.matcher(id).matches()) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-REGISTRY-ID-INVALID",
                    EchoNativeIssueSeverity.ERROR,
                    "Native registry id is invalid",
                    "Registry id '" + id + "' must use namespace:path lowercase syntax.",
                    id,
                    packId,
                    List.of(relativeReportPath(manifestPath)),
                    "Use lowercase namespace:path ids containing only resource-safe characters."
            ));
            return Map.of();
        }
        if (isUnsafeRelativePath(sourcePath)) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-REGISTRY-PATH-UNSAFE",
                    EchoNativeIssueSeverity.ERROR,
                    "Native registry descriptor path is unsafe",
                    "Registry source paths must be fixture-relative and must not escape the fixture.",
                    id,
                    packId,
                    List.of(relativeReportPath(manifestPath)),
                    "Keep M13 registry descriptors fixture-local and repo-relative."
            ));
            return Map.of();
        }
        Path entryPath = fixture.resolve(sourcePath).normalize();
        Path fixtureRoot = fixture.toAbsolutePath().normalize();
        if (!entryPath.toAbsolutePath().normalize().startsWith(fixtureRoot) || !Files.isRegularFile(entryPath)) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-REGISTRY-SOURCE-MISSING",
                    EchoNativeIssueSeverity.ERROR,
                    "Native registry source file missing",
                    "Registry source '" + sourcePath + "' was not found under the fixture.",
                    id,
                    packId,
                    List.of(sourcePath),
                    "Add the fixture-local registry source file or remove the descriptor."
            ));
            return Map.of();
        }
        Map<String, Object> entryData;
        try {
            entryData = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(entryPath)));
        } catch (RuntimeException ex) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-REGISTRY-SOURCE-INVALID",
                    EchoNativeIssueSeverity.ERROR,
                    "Native registry source is invalid JSON",
                    ex.getMessage(),
                    id,
                    packId,
                    List.of(relativeReportPath(entryPath)),
                    "Fix the fixture-local registry source JSON."
            ));
            return Map.of();
        }
        if (!id.equals(entryData.get("id")) || !kind.equals(entryData.get("kind"))) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-REGISTRY-SOURCE-MISMATCH",
                    EchoNativeIssueSeverity.ERROR,
                    "Native registry source does not match descriptor",
                    "Registry source id/kind must match the manifest descriptor.",
                    id,
                    packId,
                    List.of(relativeReportPath(entryPath)),
                    "Keep fixture registry sources aligned with the manifest."
            ));
            return Map.of();
        }
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("id", id);
        entry.put("kind", kind);
        entry.put("ownerModule", ownerModule);
        entry.put("required", Boolean.TRUE.equals(descriptor.get("required")));
        entry.put("sandboxOnly", true);
        entry.put("sourcePath", sourcePath);
        entry.put("summary", string(entryData.get("displayName")));
        entry.put("minecraftRegistryTouched", false);
        entry.put("registryInjected", false);
        entry.put("registryMutated", false);
        return entry;
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
                    "M13 registry prototyping requires PASS upstream safety reports with no unsafe runtime work.",
                    null,
                    packId,
                    List.of(relativeReportPath(path)),
                    "Regenerate M12 resource bridge and Phase 13 safety reports before registry prototyping."
            ));
        }
        diagnostics.addAll(reportDiagnostics(report, packId));
    }

    private static void checkResourceConflictReport(
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
                    "ECHO-NATIVE-RESOURCE-CONFLICT-GATE-BLOCKED",
                    EchoNativeIssueSeverity.ERROR,
                    "Resource conflict report is not ready for registry prototyping",
                    "M13 requires PASS resource-conflict-report.json with conflictFree=true and no unsafe runtime work.",
                    null,
                    packId,
                    List.of(relativeReportPath(path)),
                    "Resolve resource conflicts before registry bridge prototyping."
            ));
        }
        diagnostics.addAll(reportDiagnostics(report, packId));
    }

    private static List<Map<String, Object>> validatedEntries(List<Map<String, Object>> entries) {
        return entries.stream().map(entry -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", entry.get("id"));
            item.put("kind", entry.get("kind"));
            item.put("ownerModule", entry.get("ownerModule"));
            item.put("sandboxOnly", true);
            item.put("valid", true);
            return item;
        }).toList();
    }

    private static List<Map<String, Object>> registries(List<Map<String, Object>> entries) {
        Map<String, List<Map<String, Object>>> byKind = entries.stream()
                .collect(Collectors.groupingBy(item -> String.valueOf(item.get("kind")), LinkedHashMap::new, Collectors.toList()));
        List<Map<String, Object>> registries = new ArrayList<>();
        for (String kind : byKind.keySet().stream().sorted().toList()) {
            List<Map<String, Object>> sortedEntries = byKind.get(kind).stream()
                    .sorted(Comparator.comparing(item -> String.valueOf(item.get("id"))))
                    .map(entry -> {
                        Map<String, Object> item = new LinkedHashMap<>();
                        item.put("id", entry.get("id"));
                        item.put("ownerModule", entry.get("ownerModule"));
                        item.put("required", entry.get("required"));
                        item.put("summary", entry.get("summary"));
                        return item;
                    })
                    .toList();
            Map<String, Object> registry = new LinkedHashMap<>();
            registry.put("entryCount", sortedEntries.size());
            registry.put("entries", sortedEntries);
            registry.put("kind", kind);
            registry.put("minecraftRegistryTouched", false);
            registry.put("registryInjected", false);
            registry.put("registryMutated", false);
            registry.put("sandboxOnly", true);
            registries.add(registry);
        }
        return List.copyOf(registries);
    }

    private static List<Map<String, Object>> conflicts(List<Map<String, Object>> entries) {
        Map<String, List<Map<String, Object>>> byKey = entries.stream()
                .collect(Collectors.groupingBy(item -> item.get("kind") + ":" + item.get("id"), LinkedHashMap::new, Collectors.toList()));
        List<Map<String, Object>> conflicts = new ArrayList<>();
        for (Map.Entry<String, List<Map<String, Object>>> entry : byKey.entrySet()) {
            if (entry.getValue().size() <= 1) {
                continue;
            }
            Map<String, Object> conflict = new LinkedHashMap<>();
            conflict.put("blocking", true);
            conflict.put("conflictKey", entry.getKey());
            conflict.put("registryIds", entry.getValue().stream().map(item -> String.valueOf(item.get("id"))).sorted().toList());
            conflicts.add(conflict);
        }
        conflicts.sort(Comparator.comparing(item -> String.valueOf(item.get("conflictKey"))));
        return List.copyOf(conflicts);
    }

    private static List<EchoNativeDiagnostic> withConflictDiagnostics(
            List<EchoNativeDiagnostic> diagnostics,
            List<Map<String, Object>> conflicts,
            String packId
    ) {
        List<EchoNativeDiagnostic> result = new ArrayList<>(diagnostics);
        for (Map<String, Object> conflict : conflicts) {
            result.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-REGISTRY-CONFLICT",
                    EchoNativeIssueSeverity.ERROR,
                    "Native registry conflict detected",
                    "Multiple fixture registry descriptors target " + conflict.get("conflictKey") + ".",
                    null,
                    packId,
                    EchoNativeJson.stringList(conflict.get("registryIds")),
                    "Resolve duplicate sandbox registry ids before enabling registry bridge prototypes."
            ));
        }
        return result;
    }

    private static List<String> registryKinds(List<Map<String, Object>> entries) {
        Set<String> kinds = new LinkedHashSet<>();
        entries.stream()
                .map(item -> String.valueOf(item.get("kind")))
                .sorted()
                .forEach(kinds::add);
        return List.copyOf(kinds);
    }

    private static Map<String, Object> registrySourceInventory(
            String packId,
            EchoNativeRegistrySourceInventory inventory,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m13_registry_source_inventory", diagnostics);
        data.put("inventoryId", inventory.inventoryId());
        data.put("inventoried", inventory.inventoried());
        data.put("localOnly", inventory.localOnly());
        data.put("minecraftRegistryAccessAllowed", inventory.minecraftRegistryAccessAllowed());
        data.put("minecraftRegistryTouched", false);
        data.put("packId", packId);
        data.put("registryInjected", false);
        data.put("registryInjectionAllowed", inventory.registryInjectionAllowed());
        data.put("registryMutated", false);
        data.put("registryMutationAllowed", inventory.registryMutationAllowed());
        data.put("registrySourceCount", inventory.registrySourceCount());
        data.put("registrySources", inventory.registrySources());
        data.put("sandboxOnly", inventory.sandboxOnly());
        data.put("summary", inventory.inventoried()
                ? "Fixture-local native registry sources were inventoried as sandbox descriptors only."
                : "Registry source inventory is blocked by diagnostics.");
        return data;
    }

    private static Map<String, Object> registryIdValidation(
            String packId,
            EchoNativeRegistryIdValidation validation,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m13_registry_id_validation", diagnostics);
        data.put("minecraftRegistryTouched", validation.minecraftRegistryTouched());
        data.put("packId", packId);
        data.put("registryKindCount", validation.registryKindCount());
        data.put("registryKinds", validation.registryKinds());
        data.put("sandboxOnly", validation.sandboxOnly());
        data.put("summary", validation.valid()
                ? "Sandbox registry ids validated successfully."
                : "Registry id validation is blocked by diagnostics.");
        data.put("valid", validation.valid());
        data.put("validatedEntries", validation.validatedEntries());
        data.put("validatedEntryCount", validation.validatedEntryCount());
        data.put("validationId", validation.validationId());
        return data;
    }

    private static Map<String, Object> sandboxRegistryModel(
            String packId,
            EchoNativeSandboxRegistryModel model,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m13_sandbox_registry_model", diagnostics);
        data.put("minecraftRegistryTouched", model.minecraftRegistryTouched());
        data.put("modelId", model.modelId());
        data.put("modeled", model.modeled());
        data.put("modeledEntryCount", model.modeledEntryCount());
        data.put("packId", packId);
        data.put("registries", model.registries());
        data.put("registryInjected", model.registryInjected());
        data.put("registryKindCount", model.registryKindCount());
        data.put("registryMutated", model.registryMutated());
        data.put("sandboxOnly", model.sandboxOnly());
        data.put("summary", model.modeled()
                ? "Sandbox registry model was built as data only without touching Minecraft registries."
                : "Sandbox registry model is blocked by diagnostics.");
        return data;
    }

    private static Map<String, Object> registryConflictReport(
            String packId,
            EchoNativeRegistryConflictReport report,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m13_registry_conflict_report", diagnostics);
        data.put("blockingConflictCount", report.blockingConflictCount());
        data.put("conflictCount", report.conflictCount());
        data.put("conflictFree", report.conflictFree());
        data.put("conflicts", report.conflicts());
        data.put("minecraftRegistryTouched", report.minecraftRegistryTouched());
        data.put("packId", packId);
        data.put("reportId", report.reportId());
        data.put("sandboxOnly", report.sandboxOnly());
        data.put("summary", report.conflictFree()
                ? "No duplicate sandbox registry ids were found."
                : "Sandbox registry conflicts block registry bridge prototyping.");
        return data;
    }

    private static Map<String, Object> registryBridgeSafetyStatus(
            String packId,
            EchoNativeRegistryBridgePrototypeSafetyStatus status,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m13_registry_bridge_safety_status", diagnostics);
        data.put("classloaderCreated", status.classloaderCreated());
        data.put("commandExecuted", status.commandExecuted());
        data.put("completedChecks", status.completedChecks());
        data.put("filesystemMutated", status.filesystemMutated());
        data.put("gameClassesResolved", status.gameClassesResolved());
        data.put("localOnly", status.localOnly());
        data.put("minecraftRegistryTouched", status.minecraftRegistryTouched());
        data.put("packId", packId);
        data.put("processLaunched", status.processLaunched());
        data.put("registryInjected", status.registryInjected());
        data.put("registryMutated", status.registryMutated());
        data.put("safeToContinue", status.safeToContinue());
        data.put("sandboxOnly", status.sandboxOnly());
        data.put("statusId", status.statusId());
        data.put("summary", status.safeToContinue()
                ? "M13 registry bridge prototype stayed sandbox-only and safe to continue."
                : "M13 registry bridge prototype is blocked by diagnostics.");
        return data;
    }

    private static Map<String, Object> base(String phase, List<EchoNativeDiagnostic> diagnostics) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("classloaderCreated", false);
        data.put("commandExecuted", false);
        data.put("diagnosticCount", diagnostics.size());
        data.put("dryRunOnly", true);
        data.put("filesystemMutated", false);
        data.put("gameClassesResolved", false);
        data.put("gameProcessLaunched", false);
        data.put("minecraftLaunched", false);
        data.put("minecraftRegistryTouched", false);
        data.put("phase", phase);
        data.put("processLaunched", false);
        data.put("registryInjected", false);
        data.put("registryMutated", false);
        data.put("sandboxOnly", true);
        data.put("simulationOnly", true);
        data.put("unsafeRuntimeWorkStarted", false);
        return data;
    }

    private static boolean hasUnsafeRuntimeWork(Map<String, Object> data) {
        return Boolean.TRUE.equals(data.get("minecraftRegistryTouched"))
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
                    "Required M13 registry prototype input report was not found.",
                    null,
                    packId,
                    List.of(fixture.toString().replace('\\', '/')),
                    "Generate M12 resource bridge and Phase 13 safety reports before registry prototyping."
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

    private record RegistryManifest(List<Map<String, Object>> entries) {
    }
}
