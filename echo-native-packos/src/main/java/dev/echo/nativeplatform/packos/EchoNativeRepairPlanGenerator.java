package dev.echo.nativeplatform.packos;

import dev.echo.nativeplatform.contracts.EchoNativeDiagnostic;
import dev.echo.nativeplatform.contracts.EchoNativeIssueSeverity;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoNativeRepairPlanGenerator {
    public EchoNativeRepairPlan plan(String packId, List<EchoNativeDiagnostic> diagnostics) {
        List<EchoNativeDiagnostic> sortedDiagnostics = diagnostics.stream()
                .sorted(Comparator.comparing(EchoNativeDiagnostic::code).thenComparing(EchoNativeDiagnostic::summary))
                .toList();
        List<Map<String, Object>> actions = sortedDiagnostics.stream()
                .filter(this::needsAction)
                .map(this::action)
                .toList();
        Map<String, Object> repairPlan = new LinkedHashMap<>();
        repairPlan.put("actionCount", actions.size());
        repairPlan.put("actions", actions);
        repairPlan.put("destructiveActions", 0);
        repairPlan.put("downloadAllowed", false);
        repairPlan.put("dryRunOnly", true);
        repairPlan.put("phase", "phase12_packos_dry_run");
        repairPlan.put("repairExecutionAllowed", false);
        repairPlan.put("status", actions.isEmpty() ? "no_repair_needed" : "manual_action_required");
        repairPlan.put("summary", actions.isEmpty()
                ? "No repair is needed for this dry-run fixture."
                : "Manual fixture or descriptor review is required. Native planning only recommends repairs.");
        return new EchoNativeRepairPlan(packId, repairPlan, sortedDiagnostics);
    }

    private boolean needsAction(EchoNativeDiagnostic diagnostic) {
        return diagnostic.severity() == EchoNativeIssueSeverity.ERROR || diagnostic.severity() == EchoNativeIssueSeverity.FATAL;
    }

    private Map<String, Object> action(EchoNativeDiagnostic diagnostic) {
        Map<String, Object> action = new LinkedHashMap<>();
        action.put("diagnosticCode", diagnostic.code());
        action.put("id", "repair." + slug(diagnostic.code()) + "." + actionTarget(diagnostic));
        action.put("kind", kind(diagnostic));
        action.put("moduleId", diagnostic.moduleId());
        action.put("packId", diagnostic.packId());
        action.put("reason", diagnostic.summary());
        action.put("relatedFiles", diagnostic.likelyFiles());
        action.put("requiresBackup", false);
        action.put("requiresConfirmation", true);
        action.put("risk", "manual_review");
        action.put("safeToAutomate", false);
        action.put("summary", diagnostic.title());
        action.put("suggestedFix", diagnostic.suggestedFix());
        return action;
    }

    private String kind(EchoNativeDiagnostic diagnostic) {
        return switch (diagnostic.code()) {
            case "ECHO-NATIVE-LOCKFILE-MISSING" -> "run_lock_generate";
            case "ECHO-NATIVE-LOCKFILE-DRIFT", "ECHO-NATIVE-LOCKFILE-INVALID" -> "regenerate_lockfile";
            case "ECHO-NATIVE-REQUIRED-MODULE-MISSING", "ECHO-NATIVE-ROOT-MODULE-MISSING" -> "add_descriptor_fixture";
            case "ECHO-NATIVE-REQUIRED-FEATURE-MISSING" -> "add_feature_provider";
            case "ECHO-NATIVE-MODULE-DUPLICATE" -> "deduplicate_module_descriptor";
            case "ECHO-NATIVE-DESCRIPTOR-INVALID", "ECHO-NATIVE-DESCRIPTOR-SCHEMA", "ECHO-NATIVE-DESCRIPTOR-ID-MISSING" -> "fix_descriptor_metadata";
            case "ECHO-NATIVE-TRANSFORMS-BLOCKED",
                    "ECHO-NATIVE-TRANSFORM-FORGE-INCOMPATIBLE" -> "replace_forge_transform_with_native_projection";
            case "ECHO-NATIVE-TRANSFORM-UNSUPPORTED",
                    "ECHO-NATIVE-TRANSFORM-DECLARED-REPLACEMENT-UNSUPPORTED",
                    "ECHO-NATIVE-TRANSFORM-MAPPED-REPLACEMENT-UNSUPPORTED" -> "replace_unsupported_native_transform";
            case "ECHO-NATIVE-TRANSFORM-BYTECODE-MUTATION-REQUESTED" -> "disable_bytecode_mutation";
            case "ECHO-NATIVE-TRANSFORM-INCOMPATIBILITY-POLICY-INCOMPLETE" -> "declare_transform_incompatibility_policy";
            case "ECHO-NATIVE-TRANSFORM-REPLACEMENT-COVERAGE-INCOMPLETE",
                    "ECHO-NATIVE-TRANSFORM-MAPPING-UNKNOWN-FORGE-TRANSFORM",
                    "ECHO-NATIVE-TRANSFORM-FORGE-REPLACEMENT-MISSING" -> "map_forge_transform_to_native_projection";
            default -> "manual_review";
        };
    }

    private static String actionTarget(EchoNativeDiagnostic diagnostic) {
        if (diagnostic.moduleId() != null && !diagnostic.moduleId().isBlank()) {
            return slug(diagnostic.moduleId());
        }
        return Integer.toHexString(diagnostic.summary().hashCode());
    }

    private static String slug(String value) {
        return value.toLowerCase().replace('_', '-').replace('.', '-');
    }
}
