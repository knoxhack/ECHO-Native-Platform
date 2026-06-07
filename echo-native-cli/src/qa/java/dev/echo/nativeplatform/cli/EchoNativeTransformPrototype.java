package dev.echo.nativeplatform.cli;

import dev.echo.nativeplatform.contracts.EchoNativeDiagnostic;
import dev.echo.nativeplatform.contracts.EchoNativeIssueSeverity;
import dev.echo.nativeplatform.contracts.EchoNativeTransformAllowlistValidation;
import dev.echo.nativeplatform.contracts.EchoNativeTransformConflictReport;
import dev.echo.nativeplatform.contracts.EchoNativeTransformPipelinePlan;
import dev.echo.nativeplatform.contracts.EchoNativeTransformSafetyStatus;
import dev.echo.nativeplatform.contracts.EchoNativeTransformSourceInventory;
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

final class EchoNativeTransformPrototype {
    private static final Pattern ID_PATTERN = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");
    private static final Set<String> ALLOWLISTED_TYPES = Set.of(
            "descriptor_metadata_projection",
            "test_fixture_noop",
            "test_fixture_safety_marker"
    );
    private static final Set<String> ALLOWLISTED_SCOPES = Set.of("fixture_descriptor", "test_fixture_metadata");

    EchoNativeTransformPrototypeOutcome prototype(
            String packId,
            Path fixture,
            List<String> discoveredModules,
            Path networkSafetyPath,
            Path networkConflictPath,
            Path phase13PrototypeSafetyGatePath
    ) throws IOException {
        List<EchoNativeDiagnostic> diagnostics = new ArrayList<>();
        Map<String, Object> networkSafety = readRequiredReport(networkSafetyPath, fixture, packId, "ECHO-NATIVE-NETWORK-SAFETY-MISSING", "Network bridge safety report missing", diagnostics);
        Map<String, Object> networkConflict = readRequiredReport(networkConflictPath, fixture, packId, "ECHO-NATIVE-NETWORK-CONFLICT-REPORT-MISSING", "Network conflict report missing", diagnostics);
        Map<String, Object> prototypeSafetyGate = readRequiredReport(phase13PrototypeSafetyGatePath, fixture, packId, "ECHO-NATIVE-PHASE13-PROTOTYPE-SAFETY-MISSING", "Phase 13 prototype safety gate missing", diagnostics);

        checkUpstream(networkSafety, EchoNativeJson.asObject(networkSafety.get("data")), networkSafetyPath, packId, "ECHO-NATIVE-NETWORK-SAFETY-BLOCKED", "Network bridge safety is not ready for transform planning", diagnostics);
        checkNetworkConflictReport(networkConflict, EchoNativeJson.asObject(networkConflict.get("data")), networkConflictPath, packId, diagnostics);
        checkUpstream(prototypeSafetyGate, EchoNativeJson.asObject(prototypeSafetyGate.get("data")), phase13PrototypeSafetyGatePath, packId, "ECHO-NATIVE-PROTOTYPE-SAFETY-BLOCKED", "Phase 13 prototype safety gate is not ready for transform planning", diagnostics);

        TransformManifest manifest = readTransformManifest(fixture.resolve("transforms").resolve("echo.native.transforms.json"), fixture, packId, Set.copyOf(discoveredModules), diagnostics);
        diagnostics = unique(diagnostics);
        boolean ready = diagnostics.isEmpty();

        List<Map<String, Object>> transforms = ready ? manifest.transforms() : List.of();
        List<Map<String, Object>> plannedTransforms = ready ? plannedTransforms(transforms) : List.of();
        List<Map<String, Object>> conflicts = ready ? conflicts(transforms) : List.of();
        boolean conflictFree = ready && conflicts.isEmpty();
        boolean safe = ready && conflictFree;

        if (ready && !conflictFree) {
            diagnostics = unique(withConflictDiagnostics(diagnostics, conflicts, packId));
            safe = false;
        }

        EchoNativeTransformSourceInventory inventory = new EchoNativeTransformSourceInventory(
                "phase13.m15.transform.source.inventory",
                ready,
                true,
                true,
                true,
                false,
                false,
                transforms.size(),
                transforms
        );
        EchoNativeTransformAllowlistValidation validation = new EchoNativeTransformAllowlistValidation(
                "phase13.m15.transform.allowlist.validation",
                ready,
                true,
                false,
                false,
                false,
                transforms.size(),
                ALLOWLISTED_TYPES.stream().sorted().toList(),
                validatedTransforms(transforms)
        );
        EchoNativeTransformPipelinePlan plan = new EchoNativeTransformPipelinePlan(
                "phase13.m15.transform.pipeline.plan",
                ready,
                true,
                false,
                false,
                plannedTransforms.size(),
                plannedTransforms
        );
        EchoNativeTransformConflictReport conflictReport = new EchoNativeTransformConflictReport(
                "phase13.m15.transform.conflict.report",
                conflictFree,
                true,
                false,
                conflicts.size(),
                conflicts.size(),
                conflicts
        );
        EchoNativeTransformSafetyStatus safetyStatus = new EchoNativeTransformSafetyStatus(
                "phase13.m15.transform.safety.status",
                safe,
                true,
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
                safe ? List.of(
                        "network_bridge_safety_gate",
                        "network_conflict_gate",
                        "phase13_prototype_safety_gate",
                        "transform_manifest_read",
                        "allowlist_validation",
                        "pipeline_planned",
                        "conflict_scan"
                ) : List.of()
        );

        return new EchoNativeTransformPrototypeOutcome(
                packId,
                transformSourceInventory(packId, inventory, diagnostics),
                transformAllowlistValidation(packId, validation, diagnostics),
                transformPipelinePlan(packId, plan, diagnostics),
                transformConflictReport(packId, conflictReport, diagnostics),
                transformSafetyStatus(packId, safetyStatus, diagnostics),
                diagnostics
        );
    }

    private static TransformManifest readTransformManifest(
            Path manifestPath,
            Path fixture,
            String packId,
            Set<String> discoveredModules,
            List<EchoNativeDiagnostic> diagnostics
    ) throws IOException {
        if (!Files.isRegularFile(manifestPath)) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-TRANSFORM-MANIFEST-MISSING",
                    EchoNativeIssueSeverity.ERROR,
                    "Native transform manifest missing",
                    "Transform prototyping requires a fixture-local transforms/echo.native.transforms.json manifest.",
                    null,
                    packId,
                    List.of(fixture.resolve("transforms/echo.native.transforms.json").toString().replace('\\', '/')),
                    "Add a fixture-local transform manifest or keep M15 blocked for this fixture."
            ));
            return new TransformManifest(List.of());
        }
        Map<String, Object> manifest;
        try {
            manifest = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(manifestPath)));
        } catch (RuntimeException ex) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-TRANSFORM-MANIFEST-INVALID",
                    EchoNativeIssueSeverity.ERROR,
                    "Native transform manifest is invalid JSON",
                    ex.getMessage(),
                    null,
                    packId,
                    List.of(relativeReportPath(manifestPath)),
                    "Fix the fixture-local transform manifest JSON."
            ));
            return new TransformManifest(List.of());
        }
        if (!"echo.native.transform_manifest.v1".equals(manifest.get("schema"))) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-TRANSFORM-MANIFEST-SCHEMA",
                    EchoNativeIssueSeverity.ERROR,
                    "Unsupported native transform manifest schema",
                    "Transform manifest schema was '" + manifest.get("schema") + "'.",
                    null,
                    packId,
                    List.of(relativeReportPath(manifestPath)),
                    "Use schema echo.native.transform_manifest.v1."
            ));
        }
        Map<String, Object> sourcePolicy = EchoNativeJson.asObject(manifest.get("sourcePolicy"));
        if (!Boolean.TRUE.equals(sourcePolicy.get("localOnly"))
                || !Boolean.TRUE.equals(sourcePolicy.get("descriptorOnly"))
                || !Boolean.TRUE.equals(sourcePolicy.get("transformPlanningOnly"))
                || !Boolean.TRUE.equals(sourcePolicy.get("disabledByDefault"))
                || Boolean.TRUE.equals(sourcePolicy.get("transformsEnabled"))
                || Boolean.TRUE.equals(sourcePolicy.get("minecraftTransformAllowed"))
                || Boolean.TRUE.equals(sourcePolicy.get("addonTransformAllowed"))
                || Boolean.TRUE.equals(sourcePolicy.get("bytecodeMutationAllowed"))
                || Boolean.TRUE.equals(sourcePolicy.get("classloaderAllowed"))) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-TRANSFORM-SOURCE-POLICY-UNSAFE",
                    EchoNativeIssueSeverity.ERROR,
                    "Native transform source policy is unsafe",
                    "M15 requires local descriptor-only planning, disabled transforms, and no bytecode/classloader permissions.",
                    null,
                    packId,
                    List.of(relativeReportPath(manifestPath)),
                    "Keep transform prototyping allowlisted, fixture-local, and plan-only."
            ));
        }

        List<Map<String, Object>> transforms = new ArrayList<>();
        Object rawTransforms = manifest.get("transforms");
        if (rawTransforms instanceof List<?> list) {
            for (Object item : list) {
                Map<String, Object> descriptor = EchoNativeJson.asObject(item);
                Map<String, Object> transform = readTransformDescriptor(descriptor, fixture, manifestPath, packId, discoveredModules, diagnostics);
                if (!transform.isEmpty()) {
                    transforms.add(transform);
                }
            }
        }
        transforms.sort(Comparator.comparing(item -> String.valueOf(item.get("id"))));
        if (transforms.isEmpty()) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-TRANSFORMS-EMPTY",
                    EchoNativeIssueSeverity.ERROR,
                    "Native transform manifest has no usable transforms",
                    "Transform prototyping needs at least one allowlisted fixture-local transform descriptor.",
                    null,
                    packId,
                    List.of(relativeReportPath(manifestPath)),
                    "Add descriptor-only transform metadata."
            ));
        }
        return new TransformManifest(List.copyOf(transforms));
    }

    private static Map<String, Object> readTransformDescriptor(
            Map<String, Object> descriptor,
            Path fixture,
            Path manifestPath,
            String packId,
            Set<String> discoveredModules,
            List<EchoNativeDiagnostic> diagnostics
    ) throws IOException {
        String id = string(descriptor.get("id"));
        String ownerModule = string(descriptor.get("ownerModule"));
        String transformType = string(descriptor.get("transformType"));
        String targetScope = string(descriptor.get("targetScope"));
        String targetId = string(descriptor.get("targetId"));
        String sourcePath = string(descriptor.get("sourcePath")).replace('\\', '/');
        if (id.isBlank() || ownerModule.isBlank() || transformType.isBlank() || targetScope.isBlank() || targetId.isBlank() || sourcePath.isBlank()) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-TRANSFORM-DESCRIPTOR-INCOMPLETE",
                    EchoNativeIssueSeverity.ERROR,
                    "Native transform descriptor is incomplete",
                    "Each transform requires id, ownerModule, transformType, targetScope, targetId, and sourcePath.",
                    id.isBlank() ? null : id,
                    packId,
                    List.of(relativeReportPath(manifestPath)),
                    "Complete the fixture-local transform descriptor."
            ));
            return Map.of();
        }
        if (!ID_PATTERN.matcher(id).matches()) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-TRANSFORM-ID-INVALID",
                    EchoNativeIssueSeverity.ERROR,
                    "Native transform id is invalid",
                    "Transform id '" + id + "' must use namespace:path lowercase syntax.",
                    id,
                    packId,
                    List.of(relativeReportPath(manifestPath)),
                    "Use lowercase namespace:path transform ids."
            ));
            return Map.of();
        }
        if (!discoveredModules.contains(ownerModule)) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-TRANSFORM-OWNER-MISSING",
                    EchoNativeIssueSeverity.ERROR,
                    "Native transform owner module is missing",
                    "Owner module '" + ownerModule + "' is not present in discovered descriptors.",
                    id,
                    packId,
                    List.of(relativeReportPath(manifestPath)),
                    "Use an owner module discovered in the fixture."
            ));
            return Map.of();
        }
        if (!ALLOWLISTED_TYPES.contains(transformType)) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-TRANSFORM-TYPE-NOT-ALLOWLISTED",
                    EchoNativeIssueSeverity.ERROR,
                    "Native transform type is not allowlisted",
                    "Transform type '" + transformType + "' is not allowlisted for M15 planning.",
                    id,
                    packId,
                    List.of(relativeReportPath(manifestPath)),
                    "Use only fixture-safe transform planning types."
            ));
            return Map.of();
        }
        if (!ALLOWLISTED_SCOPES.contains(targetScope)) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-TRANSFORM-SCOPE-NOT-ALLOWLISTED",
                    EchoNativeIssueSeverity.ERROR,
                    "Native transform target scope is not allowlisted",
                    "Transform target scope '" + targetScope + "' is not allowlisted for M15.",
                    id,
                    packId,
                    List.of(relativeReportPath(manifestPath)),
                    "Use fixture_descriptor or test_fixture_metadata only."
            ));
            return Map.of();
        }
        if (!Boolean.TRUE.equals(descriptor.get("disabledByDefault"))
                || !Boolean.TRUE.equals(descriptor.get("planningOnly"))
                || Boolean.TRUE.equals(descriptor.get("minecraftBytecodeTarget"))
                || Boolean.TRUE.equals(descriptor.get("addonBytecodeTarget"))) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-TRANSFORM-DESCRIPTOR-UNSAFE",
                    EchoNativeIssueSeverity.ERROR,
                    "Native transform descriptor is unsafe",
                    "M15 transform descriptors must be disabledByDefault=true, planningOnly=true, and must not target Minecraft or addon bytecode.",
                    id,
                    packId,
                    List.of(relativeReportPath(manifestPath)),
                    "Keep transform descriptors test-fixture-only and planning-only."
            ));
            return Map.of();
        }
        if (isUnsafeRelativePath(sourcePath)) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-TRANSFORM-PATH-UNSAFE",
                    EchoNativeIssueSeverity.ERROR,
                    "Native transform descriptor path is unsafe",
                    "Transform source paths must be fixture-relative and must not escape the fixture.",
                    id,
                    packId,
                    List.of(relativeReportPath(manifestPath)),
                    "Keep M15 transform descriptors fixture-local and repo-relative."
            ));
            return Map.of();
        }
        Path sourcePathResolved = fixture.resolve(sourcePath).normalize();
        if (!sourcePathResolved.toAbsolutePath().normalize().startsWith(fixture.toAbsolutePath().normalize()) || !Files.isRegularFile(sourcePathResolved)) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-TRANSFORM-SOURCE-MISSING",
                    EchoNativeIssueSeverity.ERROR,
                    "Native transform source file missing",
                    "Transform source '" + sourcePath + "' was not found under the fixture.",
                    id,
                    packId,
                    List.of(sourcePath),
                    "Add the fixture-local transform source file or remove the descriptor."
            ));
            return Map.of();
        }
        Map<String, Object> source = readSource(sourcePathResolved, id, targetId, transformType, packId, diagnostics);
        if (source.isEmpty()) {
            return Map.of();
        }

        Map<String, Object> transform = new LinkedHashMap<>();
        transform.put("id", id);
        transform.put("ownerModule", ownerModule);
        transform.put("transformType", transformType);
        transform.put("targetScope", targetScope);
        transform.put("targetId", targetId);
        transform.put("sourcePath", sourcePath);
        transform.put("summary", string(source.get("displayName")));
        transform.put("disabledByDefault", true);
        transform.put("planningOnly", true);
        transform.put("transformPlanningOnly", true);
        transform.put("transformsEnabled", false);
        transform.put("minecraftBytecodeTransformed", false);
        transform.put("addonBytecodeTransformed", false);
        transform.put("bytecodeMutated", false);
        return transform;
    }

    private static Map<String, Object> readSource(
            Path sourcePath,
            String id,
            String targetId,
            String transformType,
            String packId,
            List<EchoNativeDiagnostic> diagnostics
    ) throws IOException {
        Map<String, Object> source;
        try {
            source = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(sourcePath)));
        } catch (RuntimeException ex) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-TRANSFORM-SOURCE-INVALID",
                    EchoNativeIssueSeverity.ERROR,
                    "Native transform source is invalid JSON",
                    ex.getMessage(),
                    id,
                    packId,
                    List.of(relativeReportPath(sourcePath)),
                    "Fix the fixture-local transform source JSON."
            ));
            return Map.of();
        }
        if (!id.equals(source.get("id"))
                || !"transform".equals(source.get("descriptorType"))
                || !targetId.equals(source.get("targetId"))
                || !transformType.equals(source.get("transformType"))) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-TRANSFORM-SOURCE-MISMATCH",
                    EchoNativeIssueSeverity.ERROR,
                    "Native transform source does not match descriptor",
                    "Transform source id/descriptorType/targetId/transformType must match the manifest descriptor.",
                    id,
                    packId,
                    List.of(relativeReportPath(sourcePath)),
                    "Keep fixture transform sources aligned with the manifest."
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
                    "M15 transform prototyping requires PASS upstream safety reports with no unsafe runtime work.",
                    null,
                    packId,
                    List.of(relativeReportPath(path)),
                    "Regenerate M14 network and Phase 13 safety reports before transform planning."
            ));
        }
        diagnostics.addAll(reportDiagnostics(report, packId));
    }

    private static void checkNetworkConflictReport(
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
                    "ECHO-NATIVE-NETWORK-CONFLICT-GATE-BLOCKED",
                    EchoNativeIssueSeverity.ERROR,
                    "Network conflict report is not ready for transform planning",
                    "M15 requires PASS network-conflict-report.json with conflictFree=true and no unsafe runtime work.",
                    null,
                    packId,
                    List.of(relativeReportPath(path)),
                    "Resolve network conflicts before transform pipeline prototyping."
            ));
        }
        diagnostics.addAll(reportDiagnostics(report, packId));
    }

    private static List<Map<String, Object>> validatedTransforms(List<Map<String, Object>> transforms) {
        return transforms.stream().map(transform -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", transform.get("id"));
            item.put("ownerModule", transform.get("ownerModule"));
            item.put("targetId", transform.get("targetId"));
            item.put("targetScope", transform.get("targetScope"));
            item.put("transformType", transform.get("transformType"));
            item.put("allowlisted", true);
            item.put("disabledByDefault", true);
            item.put("planningOnly", true);
            return item;
        }).toList();
    }

    private static List<Map<String, Object>> plannedTransforms(List<Map<String, Object>> transforms) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (int index = 0; index < transforms.size(); index++) {
            Map<String, Object> transform = transforms.get(index);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("order", index);
            item.put("id", transform.get("id"));
            item.put("ownerModule", transform.get("ownerModule"));
            item.put("targetId", transform.get("targetId"));
            item.put("targetScope", transform.get("targetScope"));
            item.put("transformType", transform.get("transformType"));
            item.put("disabledByDefault", true);
            item.put("transformPlanningOnly", true);
            item.put("transformsEnabled", false);
            item.put("bytecodeMutated", false);
            result.add(item);
        }
        return List.copyOf(result);
    }

    private static List<Map<String, Object>> conflicts(List<Map<String, Object>> transforms) {
        List<Map<String, Object>> conflicts = new ArrayList<>();
        addDuplicateConflicts(conflicts, transforms, "transform", "id");
        addDuplicateConflicts(conflicts, transforms, "target", "targetId");
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
                    "ECHO-NATIVE-TRANSFORM-CONFLICT",
                    EchoNativeIssueSeverity.ERROR,
                    "Native transform descriptor conflict detected",
                    "Multiple fixture transform descriptors target " + conflict.get("conflictKey") + ".",
                    null,
                    packId,
                    EchoNativeJson.stringList(conflict.get("ids")),
                    "Resolve duplicate transform or target ids before enabling transform pipeline prototypes."
            ));
        }
        return result;
    }

    private static Map<String, Object> transformSourceInventory(
            String packId,
            EchoNativeTransformSourceInventory inventory,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m15_transform_source_inventory", diagnostics);
        data.put("bytecodeMutationAllowed", inventory.bytecodeMutationAllowed());
        data.put("descriptorOnly", inventory.descriptorOnly());
        data.put("inventoryId", inventory.inventoryId());
        data.put("inventoried", inventory.inventoried());
        data.put("localOnly", inventory.localOnly());
        data.put("packId", packId);
        data.put("summary", inventory.inventoried()
                ? "Fixture-local native transform sources were inventoried as planning descriptors only."
                : "Transform source inventory is blocked by diagnostics.");
        data.put("transformPlanningOnly", inventory.transformPlanningOnly());
        data.put("transformSourceCount", inventory.transformSourceCount());
        data.put("transformSources", inventory.transformSources());
        data.put("transformsEnabled", inventory.transformsEnabled());
        return data;
    }

    private static Map<String, Object> transformAllowlistValidation(
            String packId,
            EchoNativeTransformAllowlistValidation validation,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m15_transform_allowlist_validation", diagnostics);
        data.put("addonTransformAllowed", validation.addonTransformAllowed());
        data.put("allowlistedTypes", validation.allowlistedTypes());
        data.put("minecraftTransformAllowed", validation.minecraftTransformAllowed());
        data.put("packId", packId);
        data.put("summary", validation.valid()
                ? "Transform descriptors validated against the M15 allowlist without enabling transforms."
                : "Transform allowlist validation is blocked by diagnostics.");
        data.put("transformCount", validation.transformCount());
        data.put("transformPlanningOnly", validation.transformPlanningOnly());
        data.put("transforms", validation.transforms());
        data.put("transformsEnabled", validation.transformsEnabled());
        data.put("valid", validation.valid());
        data.put("validationId", validation.validationId());
        return data;
    }

    private static Map<String, Object> transformPipelinePlan(
            String packId,
            EchoNativeTransformPipelinePlan plan,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m15_transform_pipeline_plan", diagnostics);
        data.put("bytecodeMutated", plan.bytecodeMutated());
        data.put("packId", packId);
        data.put("planId", plan.planId());
        data.put("planned", plan.planned());
        data.put("plannedTransformCount", plan.plannedTransformCount());
        data.put("plannedTransforms", plan.plannedTransforms());
        data.put("summary", plan.planned()
                ? "Transform pipeline was planned with transforms disabled and no bytecode mutation."
                : "Transform pipeline planning is blocked by diagnostics.");
        data.put("transformPlanningOnly", plan.transformPlanningOnly());
        data.put("transformsEnabled", plan.transformsEnabled());
        return data;
    }

    private static Map<String, Object> transformConflictReport(
            String packId,
            EchoNativeTransformConflictReport report,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m15_transform_conflict_report", diagnostics);
        data.put("blockingConflictCount", report.blockingConflictCount());
        data.put("conflictCount", report.conflictCount());
        data.put("conflictFree", report.conflictFree());
        data.put("conflicts", report.conflicts());
        data.put("packId", packId);
        data.put("reportId", report.reportId());
        data.put("summary", report.conflictFree()
                ? "No duplicate transform or target ids were found."
                : "Transform descriptor conflicts block transform pipeline prototyping.");
        data.put("transformPlanningOnly", report.transformPlanningOnly());
        data.put("transformsEnabled", report.transformsEnabled());
        return data;
    }

    private static Map<String, Object> transformSafetyStatus(
            String packId,
            EchoNativeTransformSafetyStatus status,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m15_transform_safety_status", diagnostics);
        data.put("addonBytecodeTransformed", status.addonBytecodeTransformed());
        data.put("bytecodeMutated", status.bytecodeMutated());
        data.put("classloaderCreated", status.classloaderCreated());
        data.put("commandExecuted", status.commandExecuted());
        data.put("completedChecks", status.completedChecks());
        data.put("descriptorOnly", status.descriptorOnly());
        data.put("filesystemMutated", status.filesystemMutated());
        data.put("gameClassesResolved", status.gameClassesResolved());
        data.put("liveNetworkingStarted", status.liveNetworkingStarted());
        data.put("localOnly", status.localOnly());
        data.put("minecraftBytecodeTransformed", status.minecraftBytecodeTransformed());
        data.put("packId", packId);
        data.put("processLaunched", status.processLaunched());
        data.put("registryInjected", status.registryInjected());
        data.put("registryMutated", status.registryMutated());
        data.put("safeToContinue", status.safeToContinue());
        data.put("statusId", status.statusId());
        data.put("summary", status.safeToContinue()
                ? "M15 transform pipeline prototype stayed planning-only and safe to continue."
                : "M15 transform pipeline prototype is blocked by diagnostics.");
        data.put("transformPlanningOnly", status.transformPlanningOnly());
        data.put("transformsEnabled", status.transformsEnabled());
        return data;
    }

    private static Map<String, Object> base(String phase, List<EchoNativeDiagnostic> diagnostics) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("addonBytecodeTransformed", false);
        data.put("bytecodeMutated", false);
        data.put("classloaderCreated", false);
        data.put("commandExecuted", false);
        data.put("descriptorOnly", true);
        data.put("diagnosticCount", diagnostics.size());
        data.put("dryRunOnly", true);
        data.put("filesystemMutated", false);
        data.put("gameClassesResolved", false);
        data.put("gameProcessLaunched", false);
        data.put("liveNetworkingStarted", false);
        data.put("minecraftBytecodeTransformed", false);
        data.put("minecraftLaunched", false);
        data.put("phase", phase);
        data.put("processLaunched", false);
        data.put("registryInjected", false);
        data.put("registryMutated", false);
        data.put("simulationOnly", true);
        data.put("transformPlanningOnly", true);
        data.put("transformsEnabled", false);
        data.put("transformsPerformed", false);
        data.put("unsafeRuntimeWorkStarted", false);
        return data;
    }

    private static boolean hasUnsafeRuntimeWork(Map<String, Object> data) {
        return Boolean.TRUE.equals(data.get("transformsEnabled"))
                || Boolean.TRUE.equals(data.get("transformsPerformed"))
                || Boolean.TRUE.equals(data.get("minecraftBytecodeTransformed"))
                || Boolean.TRUE.equals(data.get("addonBytecodeTransformed"))
                || Boolean.TRUE.equals(data.get("bytecodeMutated"))
                || Boolean.TRUE.equals(data.get("liveNetworkingStarted"))
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
                    "Required M15 transform prototype input report was not found.",
                    null,
                    packId,
                    List.of(fixture.toString().replace('\\', '/')),
                    "Generate M14 network and Phase 13 safety reports before transform planning."
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

    private record TransformManifest(List<Map<String, Object>> transforms) {
    }
}
