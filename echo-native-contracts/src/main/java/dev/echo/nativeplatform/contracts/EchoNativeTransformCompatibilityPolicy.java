package dev.echo.nativeplatform.contracts;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class EchoNativeTransformCompatibilityPolicy {
    public static final String POLICY_ID = "echo-native-transform-compatibility.v1";
    public static final String ACCESS_COMPATIBILITY_KEY = "nativeTransformCompatibility";
    public static final String RELEASE_POLICY_SUMMARY =
            "Native Loader release mode supports descriptor-declared native projection declarations only. It does not "
                    + "execute Minecraft or addon bytecode mutation; Forge Mixins, coremods, access transformers, and "
                    + "class transformers are incompatible unless each one is mapped to a supported native projection replacement.";
    public static final String DIAGNOSTIC_PATH_SUMMARY =
            "ERROR blocks validation and release packaging, WARNING requires release-owner review, and NOTICE records "
                    + "Forge-style declarations that are explicitly replaced by native projections.";
    private static final Set<String> SUPPORTED_NATIVE_DECLARATIONS = Set.of(
            "native:client_surface_projection",
            "native:command_surface_projection",
            "native:config_schema_projection",
            "native:descriptor_metadata_projection",
            "native:event_contract_projection",
            "native:lifecycle_contract_projection",
            "native:network_contract_projection",
            "native:registry_contract_projection",
            "native:resource_pipeline_projection",
            "native:save_data_projection"
    );
    private static final List<String> FORGE_STYLE_MARKERS = List.of(
            "mixin",
            "coremod",
            "access_transformer",
            "accesstransformer",
            "accesswidener",
            "bytecode",
            "asm",
            "class_transformer",
            "transformer"
    );
    private static final List<String> SUPPORTED_INCOMPATIBILITY_POLICY = List.of(
            "no_minecraft_bytecode_mutation",
            "no_addon_bytecode_mutation",
            "no_mixin_runtime",
            "no_coremod_runtime",
            "no_access_transformer_runtime",
            "native_projection_replacement_required"
    );

    private EchoNativeTransformCompatibilityPolicy() {
    }

    public static TransformCompatibilityReport evaluate(String packId, EchoNativeAddonDescriptor descriptor) {
        List<String> supportedNativeDeclarations = new ArrayList<>();
        List<String> topLevelForgeStyleRequests = new ArrayList<>();
        List<String> unsupportedNativeRequests = new ArrayList<>();
        List<EchoNativeDiagnostic> diagnostics = new ArrayList<>();
        Map<String, Object> compatibility = compatibility(descriptor.access());
        boolean bytecodeMutationAllowed = bool(compatibility.get("bytecodeMutationAllowed"));
        boolean minecraftBytecodeMutationAllowed = bool(compatibility.get("minecraftBytecodeMutationAllowed"));
        boolean addonBytecodeMutationAllowed = bool(compatibility.get("addonBytecodeMutationAllowed"));

        for (String request : normalizedTransformRequests(descriptor.transforms())) {
            if (SUPPORTED_NATIVE_DECLARATIONS.contains(request)) {
                supportedNativeDeclarations.add(request);
            } else if (isForgeStyleTransform(request)) {
                topLevelForgeStyleRequests.add(request);
            } else {
                unsupportedNativeRequests.add(request);
            }
        }

        List<NativeTransformReplacementMapping> declaredReplacementMappings = declaredReplacementMappings(descriptor);
        List<String> declaredForgeStyleTransforms = distinct(concat(
                declaredForgeStyleTransforms(descriptor),
                topLevelForgeStyleRequests
        ));
        List<String> declaredNativeReplacements = declaredNativeReplacements(descriptor);
        List<String> unsupportedDeclaredNativeReplacements = unsupportedNativeDeclarations(declaredNativeReplacements);
        List<String> unsupportedMappedNativeReplacements = unsupportedMappedNativeReplacements(declaredReplacementMappings);
        List<String> unmappedForgeStyleTransforms = unmappedForgeStyleTransforms(declaredForgeStyleTransforms, declaredReplacementMappings);
        List<String> unknownMappedForgeStyleTransforms =
                unknownMappedForgeStyleTransforms(declaredForgeStyleTransforms, declaredReplacementMappings);
        List<String> incompatibleForgeStyleRequests = topLevelForgeStyleRequests.stream()
                .filter(request -> !mappedToSupportedNativeReplacement(request, declaredReplacementMappings))
                .toList();

        if (!incompatibleForgeStyleRequests.isEmpty()) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-TRANSFORM-FORGE-REPLACEMENT-MISSING",
                    EchoNativeIssueSeverity.ERROR,
                    "Forge-style transform request needs a native replacement mapping",
                    "Module '" + descriptor.id() + "' requests Forge/Mixin/coremod-style transforms: "
                            + String.join(", ", incompatibleForgeStyleRequests) + ".",
                    descriptor.id(),
                    packId,
                    List.of(descriptor.descriptorPath().toString().replace('\\', '/')),
                    RELEASE_POLICY_SUMMARY + " Map each transform to an explicitly supported native projection declaration."
            ));
        } else if (!topLevelForgeStyleRequests.isEmpty()) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-TRANSFORM-FORGE-MAPPED-TO-NATIVE",
                    EchoNativeIssueSeverity.NOTICE,
                    "Forge-style transform request is covered by native replacements",
                    "Module '" + descriptor.id() + "' declares Forge/Mixin/coremod-style transforms and maps them to supported native projections: "
                            + String.join(", ", topLevelForgeStyleRequests) + ".",
                    descriptor.id(),
                    packId,
                    List.of(descriptor.descriptorPath().toString().replace('\\', '/')),
                    "Native Loader release mode will use the mapped native projections and will not execute bytecode transforms."
            ));
        }
        if (!unsupportedNativeRequests.isEmpty()) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-TRANSFORM-UNSUPPORTED",
                    EchoNativeIssueSeverity.ERROR,
                    "Native transform request is not in the release allowlist",
                    "Module '" + descriptor.id() + "' requests unsupported native transforms: "
                            + String.join(", ", unsupportedNativeRequests) + ".",
                    descriptor.id(),
                    packId,
                    List.of(descriptor.descriptorPath().toString().replace('\\', '/')),
                    "Use one of the supported native projection declarations or ship the behavior through loader APIs instead."
            ));
        }
        if (bytecodeMutationAllowed || minecraftBytecodeMutationAllowed || addonBytecodeMutationAllowed) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-TRANSFORM-BYTECODE-MUTATION-REQUESTED",
                    EchoNativeIssueSeverity.ERROR,
                    "Native release descriptors cannot allow bytecode mutation",
                    "Module '" + descriptor.id()
                            + "' declares a native transform compatibility policy that allows bytecode mutation.",
                    descriptor.id(),
                    packId,
                    List.of(descriptor.descriptorPath().toString().replace('\\', '/')),
                    RELEASE_POLICY_SUMMARY + " Set bytecode mutation flags to false and provide native replacement declarations."
            ));
        }
        if (!declaredForgeStyleTransforms.isEmpty()
                && (!declaresNativeProjectionReplacement(compatibility) || declaredNativeReplacements.isEmpty())) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-TRANSFORM-INCOMPATIBILITY-POLICY-INCOMPLETE",
                    EchoNativeIssueSeverity.ERROR,
                    "Forge-style transform incompatibility needs a native replacement policy",
                    "Module '" + descriptor.id() + "' declares Forge/Mixin/coremod transform compatibility data but does not"
                            + " declare mode=native_projection_replacement with at least one native replacement.",
                    descriptor.id(),
                    packId,
                    List.of(descriptor.descriptorPath().toString().replace('\\', '/')),
                    "Add access." + ACCESS_COMPATIBILITY_KEY
                            + ".nativeReplacements using the supported native projection declarations."
            ));
        }
        if (!unsupportedDeclaredNativeReplacements.isEmpty()) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-TRANSFORM-DECLARED-REPLACEMENT-UNSUPPORTED",
                    EchoNativeIssueSeverity.ERROR,
                    "Declared native transform replacement is not supported",
                    "Module '" + descriptor.id() + "' declares unsupported native transform replacements: "
                            + String.join(", ", unsupportedDeclaredNativeReplacements) + ".",
                    descriptor.id(),
                    packId,
                    List.of(descriptor.descriptorPath().toString().replace('\\', '/')),
                    "Use one of the supported native projection declarations."
            ));
        }
        if (!unsupportedMappedNativeReplacements.isEmpty()) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-TRANSFORM-MAPPED-REPLACEMENT-UNSUPPORTED",
                    EchoNativeIssueSeverity.ERROR,
                    "Mapped native transform replacement is not supported",
                    "Module '" + descriptor.id() + "' maps Forge/Mixin/coremod transforms to unsupported native replacements: "
                            + String.join(", ", unsupportedMappedNativeReplacements) + ".",
                    descriptor.id(),
                    packId,
                    List.of(descriptor.descriptorPath().toString().replace('\\', '/')),
                    "Map every Forge-style transform to one of the supported native projection declarations."
            ));
        }
        if (!unknownMappedForgeStyleTransforms.isEmpty()) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-TRANSFORM-MAPPING-UNKNOWN-FORGE-TRANSFORM",
                    EchoNativeIssueSeverity.ERROR,
                    "Native transform replacement mapping references undeclared Forge transforms",
                    "Module '" + descriptor.id() + "' has replacement mappings for undeclared Forge/Mixin/coremod transforms: "
                            + String.join(", ", unknownMappedForgeStyleTransforms) + ".",
                    descriptor.id(),
                    packId,
                    List.of(descriptor.descriptorPath().toString().replace('\\', '/')),
                    "Keep replacementMappings.forgeStyleTransform in sync with forgeStyleTransforms."
            ));
        }
        if (!unmappedForgeStyleTransforms.isEmpty()) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-TRANSFORM-REPLACEMENT-COVERAGE-INCOMPLETE",
                    EchoNativeIssueSeverity.ERROR,
                    "Forge-style transform replacement coverage is incomplete",
                    "Module '" + descriptor.id() + "' declares Forge/Mixin/coremod transforms without explicit native replacement mappings: "
                            + String.join(", ", unmappedForgeStyleTransforms) + ".",
                    descriptor.id(),
                    packId,
                    List.of(descriptor.descriptorPath().toString().replace('\\', '/')),
                    "Add access." + ACCESS_COMPATIBILITY_KEY
                            + ".replacementMappings entries for every Forge-style transform."
            ));
        }
        for (String replacement : declaredNativeReplacements) {
            if (SUPPORTED_NATIVE_DECLARATIONS.contains(replacement)) {
                supportedNativeDeclarations.add(replacement);
            }
        }
        for (NativeTransformReplacementMapping mapping : declaredReplacementMappings) {
            if (SUPPORTED_NATIVE_DECLARATIONS.contains(mapping.nativeReplacement())) {
                supportedNativeDeclarations.add(mapping.nativeReplacement());
            }
        }

        boolean compatible = diagnostics.stream().noneMatch(diagnostic ->
                diagnostic.severity() == EchoNativeIssueSeverity.ERROR
                        || diagnostic.severity() == EchoNativeIssueSeverity.FATAL);
        List<String> distinctSupportedNativeDeclarations = distinct(supportedNativeDeclarations);
        boolean replacementCoverageComplete = incompatibleForgeStyleRequests.isEmpty()
                && unsupportedMappedNativeReplacements.isEmpty()
                && unmappedForgeStyleTransforms.isEmpty()
                && unknownMappedForgeStyleTransforms.isEmpty();
        return new TransformCompatibilityReport(
                POLICY_ID,
                descriptor.id(),
                !descriptor.transforms().isEmpty() || !declaredForgeStyleTransforms.isEmpty(),
                compatible,
                bytecodeMutationAllowed,
                minecraftBytecodeMutationAllowed,
                addonBytecodeMutationAllowed,
                compatible ? "native_projection_policy_compatible" : "native_projection_replacement_required",
                RELEASE_POLICY_SUMMARY,
                List.copyOf(SUPPORTED_INCOMPATIBILITY_POLICY),
                distinctSupportedNativeDeclarations,
                replacementCoverageComplete,
                compatible && !distinctSupportedNativeDeclarations.isEmpty(),
                distinctSupportedNativeDeclarations.size(),
                List.copyOf(declaredForgeStyleTransforms),
                List.copyOf(declaredNativeReplacements),
                declaredReplacementMappings.stream()
                        .map(NativeTransformReplacementMapping::toReport)
                        .toList(),
                List.copyOf(incompatibleForgeStyleRequests),
                List.copyOf(unsupportedNativeRequests),
                List.copyOf(unsupportedDeclaredNativeReplacements),
                List.copyOf(unsupportedMappedNativeReplacements),
                List.copyOf(unmappedForgeStyleTransforms),
                List.copyOf(unknownMappedForgeStyleTransforms),
                List.copyOf(diagnostics)
        );
    }

    public static List<String> supportedNativeDeclarations() {
        return SUPPORTED_NATIVE_DECLARATIONS.stream().sorted().toList();
    }

    public static List<String> supportedIncompatibilityPolicy() {
        return SUPPORTED_INCOMPATIBILITY_POLICY;
    }

    public static String releasePolicySummary() {
        return RELEASE_POLICY_SUMMARY;
    }

    public static String diagnosticPathSummary() {
        return DIAGNOSTIC_PATH_SUMMARY;
    }

    private static List<String> normalizedTransformRequests(List<String> transforms) {
        LinkedHashSet<String> requests = new LinkedHashSet<>();
        for (String transform : transforms == null ? List.<String>of() : transforms) {
            if (transform != null && !transform.isBlank()) {
                requests.add(transform.trim().toLowerCase(Locale.ROOT));
            }
        }
        return List.copyOf(requests);
    }

    private static boolean isForgeStyleTransform(String request) {
        for (String marker : FORGE_STYLE_MARKERS) {
            if (request.contains(marker)) {
                return true;
            }
        }
        return false;
    }

    private static List<String> declaredForgeStyleTransforms(EchoNativeAddonDescriptor descriptor) {
        return normalizedTransformRequests(stringList(compatibility(descriptor.access()).get("forgeStyleTransforms")));
    }

    private static List<String> declaredNativeReplacements(EchoNativeAddonDescriptor descriptor) {
        Object value = compatibility(descriptor.access()).get("nativeReplacements");
        LinkedHashSet<String> replacements = new LinkedHashSet<>();
        if (value instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> map) {
                    String nativeReplacement = firstString(map, "nativeReplacement", "to", "replacement", "nativeProjection");
                    if (!nativeReplacement.isBlank()) {
                        replacements.add(nativeReplacement);
                    }
                } else if (item != null && !String.valueOf(item).isBlank()) {
                    replacements.add(String.valueOf(item));
                }
            }
        }
        return normalizedTransformRequests(List.copyOf(replacements));
    }

    private static List<NativeTransformReplacementMapping> declaredReplacementMappings(EchoNativeAddonDescriptor descriptor) {
        List<NativeTransformReplacementMapping> mappings = new ArrayList<>();
        addReplacementMappings(compatibility(descriptor.access()).get("replacementMappings"), mappings);
        addReplacementMappings(compatibility(descriptor.access()).get("nativeReplacements"), mappings);
        return distinctMappings(mappings);
    }

    private static void addReplacementMappings(Object value, List<NativeTransformReplacementMapping> mappings) {
        if (!(value instanceof List<?> list)) {
            return;
        }
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> map)) {
                continue;
            }
            String forgeStyleTransform = firstString(map, "forgeStyleTransform", "from", "forgeTransform", "transform");
            String nativeReplacement = firstString(map, "nativeReplacement", "to", "replacement", "nativeProjection");
            if (!forgeStyleTransform.isBlank() || !nativeReplacement.isBlank()) {
                mappings.add(new NativeTransformReplacementMapping(forgeStyleTransform, nativeReplacement));
            }
        }
    }

    private static List<String> unsupportedNativeDeclarations(List<String> declarations) {
        return declarations.stream()
                .filter(declaration -> !SUPPORTED_NATIVE_DECLARATIONS.contains(declaration))
                .toList();
    }

    private static List<String> unsupportedMappedNativeReplacements(List<NativeTransformReplacementMapping> mappings) {
        return mappings.stream()
                .map(NativeTransformReplacementMapping::nativeReplacement)
                .filter(replacement -> replacement.isBlank() || !SUPPORTED_NATIVE_DECLARATIONS.contains(replacement))
                .distinct()
                .toList();
    }

    private static List<String> unmappedForgeStyleTransforms(
            List<String> declaredForgeStyleTransforms,
            List<NativeTransformReplacementMapping> mappings
    ) {
        Set<String> mapped = new LinkedHashSet<>();
        for (NativeTransformReplacementMapping mapping : mappings) {
            if (!mapping.forgeStyleTransform().isBlank()
                    && SUPPORTED_NATIVE_DECLARATIONS.contains(mapping.nativeReplacement())) {
                mapped.add(mapping.forgeStyleTransform());
            }
        }
        return declaredForgeStyleTransforms.stream()
                .filter(transform -> !mapped.contains(transform))
                .toList();
    }

    private static List<String> unknownMappedForgeStyleTransforms(
            List<String> declaredForgeStyleTransforms,
            List<NativeTransformReplacementMapping> mappings
    ) {
        Set<String> declared = new LinkedHashSet<>(declaredForgeStyleTransforms);
        return mappings.stream()
                .map(NativeTransformReplacementMapping::forgeStyleTransform)
                .filter(transform -> !transform.isBlank() && !declared.contains(transform))
                .distinct()
                .toList();
    }

    private static boolean declaresNativeProjectionReplacement(Map<String, Object> compatibility) {
        String mode = string(compatibility.get("mode"));
        return "native_projection_replacement".equals(mode)
                || "forge_incompatible_native_projection".equals(mode)
                || "native_projection_only".equals(mode);
    }

    private static Map<String, Object> compatibility(Map<String, Object> access) {
        if (access == null) {
            return Map.of();
        }
        Object value = access.get(ACCESS_COMPATIBILITY_KEY);
        if (!(value instanceof Map<?, ?> map)) {
            return Map.of();
        }
        Map<String, Object> result = new java.util.LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            result.put(String.valueOf(entry.getKey()), entry.getValue());
        }
        return result;
    }

    private static List<String> stringList(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (Object item : list) {
            if (item != null && !String.valueOf(item).isBlank()) {
                result.add(String.valueOf(item));
            }
        }
        return List.copyOf(result);
    }

    private static String string(Object value) {
        return value == null ? "" : String.valueOf(value).trim().toLowerCase(Locale.ROOT);
    }

    private static String firstString(Map<?, ?> map, String... keys) {
        for (String key : keys) {
            Object value = map.get(key);
            String text = string(value);
            if (!text.isBlank()) {
                return text;
            }
        }
        return "";
    }

    private static boolean bool(Object value) {
        return Boolean.TRUE.equals(value) || "true".equalsIgnoreCase(String.valueOf(value));
    }

    private static List<String> distinct(List<String> values) {
        return List.copyOf(new LinkedHashSet<>(values));
    }

    private static List<NativeTransformReplacementMapping> distinctMappings(List<NativeTransformReplacementMapping> values) {
        List<NativeTransformReplacementMapping> mappings = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (NativeTransformReplacementMapping mapping : values) {
            String key = mapping.forgeStyleTransform() + "\n" + mapping.nativeReplacement();
            if (seen.add(key)) {
                mappings.add(mapping);
            }
        }
        return List.copyOf(mappings);
    }

    private static List<String> concat(List<String> first, List<String> second) {
        List<String> values = new ArrayList<>(first);
        values.addAll(second);
        return values;
    }

    private static boolean mappedToSupportedNativeReplacement(
            String forgeStyleTransform,
            List<NativeTransformReplacementMapping> mappings
    ) {
        for (NativeTransformReplacementMapping mapping : mappings) {
            if (mapping.forgeStyleTransform().equals(forgeStyleTransform)
                    && SUPPORTED_NATIVE_DECLARATIONS.contains(mapping.nativeReplacement())) {
                return true;
            }
        }
        return false;
    }

    public record TransformCompatibilityReport(
            String policyId,
            String moduleId,
            boolean hasTransformRequests,
            boolean compatible,
            boolean bytecodeMutationAllowed,
            boolean minecraftBytecodeMutationAllowed,
            boolean addonBytecodeMutationAllowed,
            String policyDecision,
            String releasePolicySummary,
            List<String> supportedIncompatibilityPolicy,
            List<String> supportedNativeDeclarations,
            boolean replacementCoverageComplete,
            boolean nativeProjectionReplacementPlanned,
            int plannedNativeProjectionCount,
            List<String> declaredForgeStyleTransforms,
            List<String> declaredNativeReplacements,
            List<Map<String, String>> declaredReplacementMappings,
            List<String> incompatibleForgeStyleRequests,
            List<String> unsupportedNativeRequests,
            List<String> unsupportedDeclaredNativeReplacements,
            List<String> unsupportedMappedNativeReplacements,
            List<String> unmappedForgeStyleTransforms,
            List<String> unknownMappedForgeStyleTransforms,
            List<EchoNativeDiagnostic> diagnostics
    ) {
    }

    private record NativeTransformReplacementMapping(
            String forgeStyleTransform,
            String nativeReplacement
    ) {
        private NativeTransformReplacementMapping {
            forgeStyleTransform = forgeStyleTransform == null ? "" : forgeStyleTransform.trim().toLowerCase(Locale.ROOT);
            nativeReplacement = nativeReplacement == null ? "" : nativeReplacement.trim().toLowerCase(Locale.ROOT);
        }

        private Map<String, String> toReport() {
            return Map.of(
                    "forgeStyleTransform", forgeStyleTransform,
                    "nativeReplacement", nativeReplacement
            );
        }
    }
}
