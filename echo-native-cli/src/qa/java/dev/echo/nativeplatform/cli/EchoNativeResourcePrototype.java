package dev.echo.nativeplatform.cli;

import dev.echo.nativeplatform.contracts.EchoNativeDiagnostic;
import dev.echo.nativeplatform.contracts.EchoNativeIssueSeverity;
import dev.echo.nativeplatform.contracts.EchoNativeResourceBridgePrototypeSafetyStatus;
import dev.echo.nativeplatform.contracts.EchoNativeResourceConflictReport;
import dev.echo.nativeplatform.contracts.EchoNativeResourceNamespaceValidation;
import dev.echo.nativeplatform.contracts.EchoNativeResourcePackOrderPlan;
import dev.echo.nativeplatform.contracts.EchoNativeResourceSourceInventory;
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

final class EchoNativeResourcePrototype {
    private static final Pattern NAMESPACE_PATTERN = Pattern.compile("[a-z0-9_.-]+");

    EchoNativeResourcePrototypeOutcome prototype(
            String packId,
            Path fixture,
            Path configSafetyPath,
            Path phase13PrototypeSafetyGatePath
    ) throws IOException {
        List<EchoNativeDiagnostic> diagnostics = new ArrayList<>();
        Map<String, Object> configSafety = readRequiredReport(configSafetyPath, fixture, packId, "ECHO-NATIVE-CONFIG-SAFETY-MISSING", "Config safety report missing", diagnostics);
        Map<String, Object> prototypeSafetyGate = readRequiredReport(phase13PrototypeSafetyGatePath, fixture, packId, "ECHO-NATIVE-PHASE13-PROTOTYPE-SAFETY-MISSING", "Phase 13 prototype safety gate missing", diagnostics);

        checkUpstream(configSafety, EchoNativeJson.asObject(configSafety.get("data")), configSafetyPath, packId, "ECHO-NATIVE-CONFIG-SAFETY-BLOCKED", "Config prototype safety is not ready for resource prototyping", diagnostics);
        checkUpstream(prototypeSafetyGate, EchoNativeJson.asObject(prototypeSafetyGate.get("data")), phase13PrototypeSafetyGatePath, packId, "ECHO-NATIVE-PROTOTYPE-SAFETY-BLOCKED", "Phase 13 prototype safety gate is not ready for resource prototyping", diagnostics);

        ResourceManifest manifest = readResourceManifest(fixture.resolve("resources").resolve("echo.native.resources.json"), fixture, packId, diagnostics);
        diagnostics = unique(diagnostics);
        boolean ready = diagnostics.isEmpty();

        List<Map<String, Object>> resources = ready ? manifest.resources() : List.of();
        List<Map<String, Object>> validatedResources = ready ? validatedResources(resources) : List.of();
        List<Map<String, Object>> resourceOrder = ready ? resourceOrder(resources) : List.of();
        List<Map<String, Object>> conflicts = ready ? conflicts(resources) : List.of();
        boolean conflictFree = ready && conflicts.isEmpty();
        boolean safe = ready && conflictFree;

        if (ready && !conflictFree) {
            diagnostics = unique(withConflictDiagnostics(diagnostics, conflicts, packId));
            safe = false;
        }

        EchoNativeResourceSourceInventory inventory = new EchoNativeResourceSourceInventory(
                "phase13.m12.resource.source.inventory",
                ready,
                true,
                true,
                false,
                false,
                false,
                resources.size(),
                resources
        );
        EchoNativeResourceNamespaceValidation validation = new EchoNativeResourceNamespaceValidation(
                "phase13.m12.resource.namespace.validation",
                ready,
                true,
                true,
                false,
                namespaces(resources).size(),
                validatedResources.size(),
                namespaces(resources),
                validatedResources
        );
        EchoNativeResourcePackOrderPlan packOrderPlan = new EchoNativeResourcePackOrderPlan(
                "phase13.m12.resource.pack.order.plan",
                ready,
                true,
                false,
                false,
                resourceOrder.size(),
                resourceOrder
        );
        EchoNativeResourceConflictReport conflictReport = new EchoNativeResourceConflictReport(
                "phase13.m12.resource.conflict.report",
                conflictFree,
                true,
                false,
                conflicts.size(),
                conflicts.size(),
                conflicts
        );
        EchoNativeResourceBridgePrototypeSafetyStatus safetyStatus = new EchoNativeResourceBridgePrototypeSafetyStatus(
                "phase13.m12.resource.bridge.safety.status",
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
                safe ? List.of(
                        "config_safety_gate",
                        "phase13_prototype_safety_gate",
                        "resource_manifest_read",
                        "namespace_validation",
                        "pack_order_planned",
                        "conflict_scan"
                ) : List.of()
        );

        return new EchoNativeResourcePrototypeOutcome(
                packId,
                resourceSourceInventory(packId, inventory, diagnostics),
                resourceNamespaceValidation(packId, validation, diagnostics),
                resourcePackOrderPlan(packId, packOrderPlan, diagnostics),
                resourceConflictReport(packId, conflictReport, diagnostics),
                resourceBridgeSafetyStatus(packId, safetyStatus, diagnostics),
                diagnostics
        );
    }

    private static ResourceManifest readResourceManifest(
            Path manifestPath,
            Path fixture,
            String packId,
            List<EchoNativeDiagnostic> diagnostics
    ) throws IOException {
        if (!Files.isRegularFile(manifestPath)) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-RESOURCE-MANIFEST-MISSING",
                    EchoNativeIssueSeverity.ERROR,
                    "Native resource manifest missing",
                    "Resource prototyping requires a fixture-local resources/echo.native.resources.json manifest.",
                    null,
                    packId,
                    List.of(fixture.resolve("resources/echo.native.resources.json").toString().replace('\\', '/')),
                    "Add a fixture-local resource manifest or keep M12 blocked for this fixture."
            ));
            return new ResourceManifest(List.of());
        }
        Map<String, Object> manifest;
        try {
            manifest = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(manifestPath)));
        } catch (RuntimeException ex) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-RESOURCE-MANIFEST-INVALID",
                    EchoNativeIssueSeverity.ERROR,
                    "Native resource manifest is invalid JSON",
                    ex.getMessage(),
                    null,
                    packId,
                    List.of(relativeReportPath(manifestPath)),
                    "Fix the fixture-local resource manifest JSON."
            ));
            return new ResourceManifest(List.of());
        }
        if (!"echo.native.resource_manifest.v1".equals(manifest.get("schema"))) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-RESOURCE-MANIFEST-SCHEMA",
                    EchoNativeIssueSeverity.ERROR,
                    "Unsupported native resource manifest schema",
                    "Resource manifest schema was '" + manifest.get("schema") + "'.",
                    null,
                    packId,
                    List.of(relativeReportPath(manifestPath)),
                    "Use schema echo.native.resource_manifest.v1."
            ));
        }
        Map<String, Object> sourcePolicy = EchoNativeJson.asObject(manifest.get("sourcePolicy"));
        if (!Boolean.TRUE.equals(sourcePolicy.get("localOnly"))
                || !Boolean.TRUE.equals(sourcePolicy.get("descriptorOnly"))
                || Boolean.TRUE.equals(sourcePolicy.get("runtimeResourceAccessAllowed"))
                || Boolean.TRUE.equals(sourcePolicy.get("installedPackMutationAllowed"))
                || Boolean.TRUE.equals(sourcePolicy.get("fixtureResourceMutationAllowed"))) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-RESOURCE-SOURCE-POLICY-UNSAFE",
                    EchoNativeIssueSeverity.ERROR,
                    "Native resource source policy is unsafe",
                    "M12 requires localOnly=true, descriptorOnly=true, runtime resource access disabled, and mutation disabled.",
                    null,
                    packId,
                    List.of(relativeReportPath(manifestPath)),
                    "Keep resource bridge prototyping fixture-local and descriptor-only."
            ));
        }
        List<Map<String, Object>> resources = new ArrayList<>();
        Object rawResources = manifest.get("resources");
        if (rawResources instanceof List<?> list) {
            for (Object item : list) {
                Map<String, Object> descriptor = EchoNativeJson.asObject(item);
                Map<String, Object> resource = readResourceDescriptor(descriptor, fixture, manifestPath, packId, diagnostics);
                if (!resource.isEmpty()) {
                    resources.add(resource);
                }
            }
        }
        resources.sort(Comparator.<Map<String, Object>, String>comparing(item -> String.valueOf(item.get("namespace")))
                .thenComparing(item -> String.valueOf(item.get("logicalPath")))
                .thenComparing(item -> String.valueOf(item.get("id"))));
        if (resources.isEmpty()) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-RESOURCE-MANIFEST-EMPTY",
                    EchoNativeIssueSeverity.ERROR,
                    "Native resource manifest has no usable resources",
                    "Resource prototyping needs at least one fixture-local resource descriptor.",
                    null,
                    packId,
                    List.of(relativeReportPath(manifestPath)),
                    "Add resource descriptors and fixture-local resource files."
            ));
        }
        return new ResourceManifest(List.copyOf(resources));
    }

    private static Map<String, Object> readResourceDescriptor(
            Map<String, Object> descriptor,
            Path fixture,
            Path manifestPath,
            String packId,
            List<EchoNativeDiagnostic> diagnostics
    ) throws IOException {
        String id = string(descriptor.get("id"));
        String ownerModule = string(descriptor.get("ownerModule"));
        String namespace = string(descriptor.get("namespace"));
        String type = string(descriptor.get("type"));
        String sourcePath = string(descriptor.get("sourcePath")).replace('\\', '/');
        String logicalPath = string(descriptor.get("logicalPath")).replace('\\', '/');
        String packLayer = string(descriptor.get("packLayer"));
        int packOrder = number(descriptor.get("packOrder"), -1);
        if (id.isBlank() || ownerModule.isBlank() || namespace.isBlank() || type.isBlank() || sourcePath.isBlank() || logicalPath.isBlank() || packLayer.isBlank() || packOrder < 0) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-RESOURCE-DESCRIPTOR-INCOMPLETE",
                    EchoNativeIssueSeverity.ERROR,
                    "Native resource descriptor is incomplete",
                    "Each resource requires id, ownerModule, namespace, type, sourcePath, logicalPath, packLayer, and packOrder.",
                    id.isBlank() ? null : id,
                    packId,
                    List.of(relativeReportPath(manifestPath)),
                    "Complete the fixture-local resource descriptor."
            ));
            return Map.of();
        }
        if (!NAMESPACE_PATTERN.matcher(namespace).matches()) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-RESOURCE-NAMESPACE-INVALID",
                    EchoNativeIssueSeverity.ERROR,
                    "Native resource namespace is invalid",
                    "Namespace '" + namespace + "' is not lowercase resource-safe.",
                    id,
                    packId,
                    List.of(relativeReportPath(manifestPath)),
                    "Use lowercase namespaces containing only a-z, 0-9, underscore, dot, or dash."
            ));
            return Map.of();
        }
        if (isUnsafeRelativePath(sourcePath) || isUnsafeRelativePath(logicalPath)) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-RESOURCE-PATH-UNSAFE",
                    EchoNativeIssueSeverity.ERROR,
                    "Native resource path is unsafe",
                    "Resource source and logical paths must be fixture-relative, normalized, and must not escape the fixture.",
                    id,
                    packId,
                    List.of(relativeReportPath(manifestPath)),
                    "Keep M12 resource descriptors fixture-local and repo-relative."
            ));
            return Map.of();
        }
        if (!logicalPath.startsWith("assets/" + namespace + "/") && !logicalPath.startsWith("data/" + namespace + "/")) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-RESOURCE-NAMESPACE-PATH-MISMATCH",
                    EchoNativeIssueSeverity.ERROR,
                    "Native resource logical path does not match namespace",
                    "Logical path '" + logicalPath + "' does not live under assets/" + namespace + " or data/" + namespace + ".",
                    id,
                    packId,
                    List.of(relativeReportPath(manifestPath)),
                    "Align the logical path namespace with the descriptor namespace."
            ));
            return Map.of();
        }
        Path resourcePath = fixture.resolve(sourcePath).normalize();
        Path fixtureRoot = fixture.toAbsolutePath().normalize();
        if (!resourcePath.toAbsolutePath().normalize().startsWith(fixtureRoot) || !Files.isRegularFile(resourcePath)) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-RESOURCE-SOURCE-MISSING",
                    EchoNativeIssueSeverity.ERROR,
                    "Native resource source file missing",
                    "Resource source '" + sourcePath + "' was not found under the fixture.",
                    id,
                    packId,
                    List.of(sourcePath),
                    "Add the fixture-local resource file or remove the descriptor."
            ));
            return Map.of();
        }
        if (sourcePath.endsWith(".json")) {
            try {
                EchoNativeJson.parse(Files.readString(resourcePath));
            } catch (RuntimeException ex) {
                diagnostics.add(new EchoNativeDiagnostic(
                        "ECHO-NATIVE-RESOURCE-JSON-INVALID",
                        EchoNativeIssueSeverity.ERROR,
                        "Native resource JSON source is invalid",
                        ex.getMessage(),
                        id,
                        packId,
                        List.of(relativeReportPath(resourcePath)),
                        "Fix the fixture-local resource JSON."
                ));
                return Map.of();
            }
        }
        Map<String, Object> resource = new LinkedHashMap<>();
        resource.put("descriptorOnly", true);
        resource.put("id", id);
        resource.put("localOnly", true);
        resource.put("logicalPath", logicalPath);
        resource.put("namespace", namespace);
        resource.put("ownerModule", ownerModule);
        resource.put("packLayer", packLayer);
        resource.put("packOrder", packOrder);
        resource.put("required", Boolean.TRUE.equals(descriptor.get("required")));
        resource.put("resourceRuntimeAccessed", false);
        resource.put("sourcePath", sourcePath);
        resource.put("type", type);
        return resource;
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
                    "M12 resource prototyping requires PASS upstream Phase 13 safety reports with no unsafe runtime work.",
                    null,
                    packId,
                    List.of(relativeReportPath(path)),
                    "Regenerate M11 config and Phase 13 safety reports before resource prototyping."
            ));
        }
        diagnostics.addAll(reportDiagnostics(report, packId));
    }

    private static List<Map<String, Object>> validatedResources(List<Map<String, Object>> resources) {
        return resources.stream().map(resource -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", resource.get("id"));
            item.put("logicalPath", resource.get("logicalPath"));
            item.put("namespace", resource.get("namespace"));
            item.put("ownerModule", resource.get("ownerModule"));
            item.put("type", resource.get("type"));
            item.put("valid", true);
            return item;
        }).toList();
    }

    private static List<Map<String, Object>> resourceOrder(List<Map<String, Object>> resources) {
        List<Map<String, Object>> sorted = resources.stream()
                .sorted(Comparator.<Map<String, Object>>comparingInt(item -> number(item.get("packOrder"), 0))
                        .thenComparing(item -> String.valueOf(item.get("logicalPath")))
                        .thenComparing(item -> String.valueOf(item.get("id"))))
                .toList();
        List<Map<String, Object>> result = new ArrayList<>();
        for (int index = 0; index < sorted.size(); index++) {
            Map<String, Object> source = sorted.get(index);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("descriptorOnly", true);
            item.put("id", source.get("id"));
            item.put("logicalPath", source.get("logicalPath"));
            item.put("namespace", source.get("namespace"));
            item.put("order", index);
            item.put("packLayer", source.get("packLayer"));
            item.put("packOrder", source.get("packOrder"));
            item.put("resourceRuntimeAccessed", false);
            result.add(item);
        }
        return List.copyOf(result);
    }

    private static List<Map<String, Object>> conflicts(List<Map<String, Object>> resources) {
        Map<String, List<Map<String, Object>>> byKey = resources.stream()
                .collect(Collectors.groupingBy(item -> item.get("namespace") + ":" + item.get("logicalPath"), LinkedHashMap::new, Collectors.toList()));
        List<Map<String, Object>> conflicts = new ArrayList<>();
        for (Map.Entry<String, List<Map<String, Object>>> entry : byKey.entrySet()) {
            if (entry.getValue().size() <= 1) {
                continue;
            }
            Map<String, Object> conflict = new LinkedHashMap<>();
            conflict.put("blocking", true);
            conflict.put("conflictKey", entry.getKey());
            conflict.put("resourceIds", entry.getValue().stream().map(item -> String.valueOf(item.get("id"))).sorted().toList());
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
                    "ECHO-NATIVE-RESOURCE-CONFLICT",
                    EchoNativeIssueSeverity.ERROR,
                    "Native resource conflict detected",
                    "Multiple fixture resource descriptors target " + conflict.get("conflictKey") + ".",
                    null,
                    packId,
                    EchoNativeJson.stringList(conflict.get("resourceIds")),
                    "Resolve duplicate logical resource paths before enabling resource bridge prototypes."
            ));
        }
        return result;
    }

    private static List<String> namespaces(List<Map<String, Object>> resources) {
        Set<String> namespaces = new LinkedHashSet<>();
        resources.stream()
                .map(item -> String.valueOf(item.get("namespace")))
                .sorted()
                .forEach(namespaces::add);
        return List.copyOf(namespaces);
    }

    private static Map<String, Object> resourceSourceInventory(
            String packId,
            EchoNativeResourceSourceInventory inventory,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m12_resource_source_inventory", diagnostics);
        data.put("descriptorOnly", inventory.descriptorOnly());
        data.put("fixtureResourceMutationAllowed", inventory.fixtureResourceMutationAllowed());
        data.put("installedPackMutationAllowed", inventory.installedPackMutationAllowed());
        data.put("inventoried", inventory.inventoried());
        data.put("inventoryId", inventory.inventoryId());
        data.put("localOnly", inventory.localOnly());
        data.put("packId", packId);
        data.put("resourceRuntimeAccessAllowed", inventory.runtimeResourceAccessAllowed());
        data.put("resourceRuntimeAccessed", false);
        data.put("resourceSourceCount", inventory.resourceSourceCount());
        data.put("resourceSources", inventory.resourceSources());
        data.put("summary", inventory.inventoried()
                ? "Fixture-local native resource sources were inventoried as descriptors only."
                : "Resource source inventory is blocked by diagnostics.");
        return data;
    }

    private static Map<String, Object> resourceNamespaceValidation(
            String packId,
            EchoNativeResourceNamespaceValidation validation,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m12_resource_namespace_validation", diagnostics);
        data.put("descriptorOnly", validation.descriptorOnly());
        data.put("localOnly", validation.localOnly());
        data.put("namespaceCount", validation.namespaceCount());
        data.put("namespaces", validation.namespaces());
        data.put("packId", packId);
        data.put("resourceRuntimeAccessed", validation.resourceRuntimeAccessed());
        data.put("summary", validation.valid()
                ? "Fixture resource namespaces and logical paths validated successfully."
                : "Resource namespace validation is blocked by diagnostics.");
        data.put("valid", validation.valid());
        data.put("validatedResourceCount", validation.validatedResourceCount());
        data.put("validatedResources", validation.validatedResources());
        data.put("validationId", validation.validationId());
        return data;
    }

    private static Map<String, Object> resourcePackOrderPlan(
            String packId,
            EchoNativeResourcePackOrderPlan plan,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m12_resource_pack_order_plan", diagnostics);
        data.put("descriptorOnly", plan.descriptorOnly());
        data.put("filesystemMutated", plan.filesystemMutated());
        data.put("orderedResourceCount", plan.orderedResourceCount());
        data.put("packId", packId);
        data.put("planId", plan.planId());
        data.put("planned", plan.planned());
        data.put("resourceOrder", plan.resourceOrder());
        data.put("resourceRuntimeAccessed", plan.resourceRuntimeAccessed());
        data.put("summary", plan.planned()
                ? "Resource pack order was planned deterministically without runtime resource access."
                : "Resource pack order planning is blocked by diagnostics.");
        return data;
    }

    private static Map<String, Object> resourceConflictReport(
            String packId,
            EchoNativeResourceConflictReport report,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m12_resource_conflict_report", diagnostics);
        data.put("blockingConflictCount", report.blockingConflictCount());
        data.put("conflictCount", report.conflictCount());
        data.put("conflictFree", report.conflictFree());
        data.put("conflicts", report.conflicts());
        data.put("descriptorOnly", report.descriptorOnly());
        data.put("packId", packId);
        data.put("reportId", report.reportId());
        data.put("resourceRuntimeAccessed", report.resourceRuntimeAccessed());
        data.put("summary", report.conflictFree()
                ? "No duplicate fixture resource logical paths were found."
                : "Fixture resource conflicts block resource bridge prototyping.");
        return data;
    }

    private static Map<String, Object> resourceBridgeSafetyStatus(
            String packId,
            EchoNativeResourceBridgePrototypeSafetyStatus status,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m12_resource_bridge_safety_status", diagnostics);
        data.put("classloaderCreated", status.classloaderCreated());
        data.put("commandExecuted", status.commandExecuted());
        data.put("completedChecks", status.completedChecks());
        data.put("descriptorOnly", status.descriptorOnly());
        data.put("filesystemMutated", status.filesystemMutated());
        data.put("gameClassesResolved", status.gameClassesResolved());
        data.put("localOnly", status.localOnly());
        data.put("minecraftResourceManagerTouched", status.minecraftResourceManagerTouched());
        data.put("packId", packId);
        data.put("processLaunched", status.processLaunched());
        data.put("registryInjected", status.registryInjected());
        data.put("registryMutated", status.registryMutated());
        data.put("resourceRuntimeAccessed", status.resourceRuntimeAccessed());
        data.put("safeToContinue", status.safeToContinue());
        data.put("statusId", status.statusId());
        data.put("summary", status.safeToContinue()
                ? "M12 resource bridge prototype stayed fixture-local, descriptor-only, and safe to continue."
                : "M12 resource bridge prototype is blocked by diagnostics.");
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
        data.put("minecraftResourceManagerTouched", false);
        data.put("phase", phase);
        data.put("processLaunched", false);
        data.put("registryInjected", false);
        data.put("registryMutated", false);
        data.put("resourceRuntimeAccessed", false);
        data.put("simulationOnly", true);
        data.put("unsafeRuntimeWorkStarted", false);
        return data;
    }

    private static boolean hasUnsafeRuntimeWork(Map<String, Object> data) {
        return Boolean.TRUE.equals(data.get("resourceRuntimeAccessed"))
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
                || Boolean.TRUE.equals(data.get("registryInjected"))
                || Boolean.TRUE.equals(data.get("registryMutated"))
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
                    "Required M12 resource prototype input report was not found.",
                    null,
                    packId,
                    List.of(fixture.toString().replace('\\', '/')),
                    "Generate M11 config and Phase 13 safety reports before resource prototyping."
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

    private record ResourceManifest(List<Map<String, Object>> resources) {
    }
}
