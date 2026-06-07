package dev.echo.nativeplatform.cli;

import dev.echo.nativeplatform.contracts.EchoNativeDiagnostic;
import dev.echo.nativeplatform.contracts.EchoNativeIssueSeverity;
import dev.echo.nativeplatform.diagnostics.EchoNativeJson;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class EchoNativePublicBetaOpeningVerifier {
    EchoNativePublicBetaOpeningOutcome verify(String packId, Path fixture, Map<String, Path> requiredReports) throws IOException {
        List<EchoNativeDiagnostic> diagnostics = new ArrayList<>();
        Map<String, Map<String, Object>> reports = new LinkedHashMap<>();
        for (Map.Entry<String, Path> entry : requiredReports.entrySet()) {
            Map<String, Object> report = readRequiredReport(entry.getValue(), fixture, packId, entry.getKey(), diagnostics);
            reports.put(entry.getKey(), report);
            checkAcceptedReport(entry.getKey(), entry.getValue(), report, packId, diagnostics);
        }

        List<Map<String, Object>> gates = new ArrayList<>();
        gates.add(booleanGate(packId, reports, "phase13-m30-completion.json", "phase13M30Complete", true, diagnostics));
        gates.add(booleanGate(packId, reports, "phase13-m30-completion.json", "phase13M31Ready", true, diagnostics));
        gates.add(booleanGate(packId, reports, "phase13-m31-readiness.json", "phase13M31Ready", true, diagnostics));
        gates.add(booleanGate(packId, reports, "native-loader-public-beta-candidate-audit.json", "publicBetaCandidateReady", true, diagnostics));
        gates.add(booleanGate(packId, reports, "native-loader-public-beta-safety-gate.json", "safeForPublicBetaCandidateAudit", true, diagnostics));
        gates.add(booleanGate(packId, reports, "public-beta-tester-readiness.json", "publicBetaTesterReadiness", true, diagnostics));
        gates.add(booleanGate(packId, reports, "first-playtest-candidate-package.json", "testerSafePackageReady", true, diagnostics));
        gates.add(booleanGate(packId, reports, "first-playtest-support-bundle.json", "supportBundleExportReady", true, diagnostics));
        gates.add(booleanGate(packId, reports, "first-playtest-support-bundle.json", "supportBundleLocalOnly", true, diagnostics));
        gates.add(booleanGate(packId, reports, "first-playtest-support-bundle.json", "supportBundleUploadsAutomatically", false, diagnostics));
        gates.add(booleanGate(packId, reports, "first-playtest-rollback-notes.json", "rollbackNotesReady", true, diagnostics));
        gates.add(booleanGate(packId, reports, "first-playtest-known-limitations.json", "knownLimitationsReady", true, diagnostics));
        gates.add(booleanGate(packId, reports, "first-playtest-crash-report-collection.json", "crashReportCollectionReady", true, diagnostics));
        gates.add(booleanGate(packId, reports, "first-playtest-crash-report-collection.json", "uploadsAutomatically", false, diagnostics));
        gates.add(booleanGate(packId, reports, "first-playtest-open-gate.json", "firstPlaytestOpen", true, diagnostics));
        gates.add(booleanGate(packId, reports, "first-playtest-open-gate.json", "safeToOpenFirstPlaytest", true, diagnostics));
        gates.add(booleanGate(packId, reports, "first-playtest-open-gate.json", "publicPlaytestOpen", false, diagnostics));
        int issueCount = number(data(reports.get("native-loader-beta-known-issues.json")), "issueCount");
        int blockingKnownIssueCount = blockingKnownIssueCount(reports.get("native-loader-beta-known-issues.json"));
        gates.add(numberValueGate(packId, "native-loader-beta-known-issues.json", "blockingKnownIssueCount", 0, blockingKnownIssueCount, diagnostics));
        gates.add(numberGate(packId, reports, "native-loader-beta-crash-intake.json", "crashReportCount", 0, diagnostics));

        Map<String, Object> moduleCoverage = moduleCoverage(packId, fixture, reports, diagnostics);
        gates.add(booleanValueGate(packId, "public-beta-module-coverage.json", "allRequiredDescriptorsDiscovered", true, moduleCoverage.get("allRequiredDescriptorsDiscovered"), diagnostics));
        gates.add(booleanValueGate(packId, "public-beta-module-coverage.json", "allRequiredModulesLoadPlanned", true, moduleCoverage.get("allRequiredModulesLoadPlanned"), diagnostics));
        gates.add(booleanValueGate(packId, "public-beta-module-coverage.json", "allRequiredFeaturesProvided", true, moduleCoverage.get("allRequiredFeaturesProvided"), diagnostics));
        gates.add(booleanValueGate(packId, "public-beta-module-coverage.json", "noRequiredModuleStartupFailures", true, moduleCoverage.get("noRequiredModuleStartupFailures"), diagnostics));

        boolean gatesPass = gates.stream().allMatch(gate -> Boolean.TRUE.equals(gate.get("pass")));
        boolean publicBetaReady = gatesPass && diagnostics.stream().noneMatch(EchoNativePublicBetaOpeningVerifier::isBlocking);
        int crashReportCount = number(data(reports.get("native-loader-beta-crash-intake.json")), "crashReportCount");
        List<EchoNativeDiagnostic> sortedDiagnostics = diagnostics.stream()
                .sorted(Comparator.comparing(EchoNativeDiagnostic::code)
                        .thenComparing(diagnostic -> diagnostic.moduleId() == null ? "" : diagnostic.moduleId())
                        .thenComparing(EchoNativeDiagnostic::summary))
                .toList();

        return new EchoNativePublicBetaOpeningOutcome(
                packId,
                openingAudit(packId, publicBetaReady, issueCount, blockingKnownIssueCount, crashReportCount, gates, sortedDiagnostics),
                safetyGate(packId, publicBetaReady, gates, sortedDiagnostics),
                testerPackageReadiness(packId, publicBetaReady, reports, sortedDiagnostics),
                moduleCoverage,
                rollbackReadiness(packId, publicBetaReady, reports, sortedDiagnostics),
                knownLimitations(packId, publicBetaReady, reports, moduleCoverage, sortedDiagnostics),
                m31Completion(packId, publicBetaReady, gates, sortedDiagnostics),
                m32Readiness(packId, publicBetaReady, sortedDiagnostics),
                sortedDiagnostics
        );
    }

    private static Map<String, Object> openingAudit(String packId, boolean ready, int issueCount, int blockingKnownIssueCount, int crashReportCount, List<Map<String, Object>> gates, List<EchoNativeDiagnostic> diagnostics) {
        Map<String, Object> data = base("phase13_m31_public_beta_opening_audit", diagnostics);
        data.put("blockingKnownIssueCount", blockingKnownIssueCount);
        data.put("crashReportCount", crashReportCount);
        data.put("gateCount", gates.size());
        data.put("gates", gates);
        data.put("issueCount", issueCount);
        data.put("packId", packId);
        data.put("publicBetaOpen", ready);
        data.put("publicBetaReady", ready);
        data.put("publicReleaseReady", false);
        data.put("summary", ready
                ? "M31 public beta opening audit passed; Ashfall Native Loader Beta may open for testers while public release remains closed."
                : "M31 remains blocked until public beta candidate, support, rollback, limitation, and crash-report gates pass.");
        return data;
    }

    private static Map<String, Object> moduleCoverage(String packId, Path fixture, Map<String, Map<String, Object>> reports, List<EchoNativeDiagnostic> diagnostics) throws IOException {
        Map<String, Object> data = base("phase13_m31_public_beta_module_coverage", diagnostics);
        Map<String, Object> packProfile = readPackProfile(fixture);
        List<String> requiredModules = sortedStrings(data(reports.get("validation.json")).get("requiredModules"));
        List<String> recommendedModules = sortedStrings(packProfile.get("recommendedModules"));
        List<String> optionalModules = sortedStrings(packProfile.get("optionalModules"));
        List<String> requiredFeatures = sortedStrings(data(reports.get("validation.json")).get("requiredFeatures"));
        List<String> discoveredModules = sortedStrings(descriptorIds(reports.get("module-descriptors.json")));
        List<String> loadPlannedModules = sortedStrings(data(reports.get("module-load-plan.json")).get("moduleLoadOrder"));
        List<String> missingRequiredModules = missing(requiredModules, discoveredModules);
        List<String> missingRecommendedModules = missing(recommendedModules, discoveredModules);
        List<String> missingOptionalModules = missing(optionalModules, discoveredModules);
        List<String> notLoadPlannedRequiredModules = missing(requiredModules, loadPlannedModules);
        List<String> betaProfileModules = union(requiredModules, recommendedModules, optionalModules);
        List<String> missingBetaProfileModules = missing(betaProfileModules, discoveredModules);
        Map<String, Object> featureData = data(reports.get("feature-graph.json"));
        List<String> missingOptionalFeatures = missingOptionalFeatures(featureData);
        int missingRequiredFeatureCount = number(featureData, "missingRequired");
        boolean descriptorsCovered = missingRequiredModules.isEmpty();
        boolean loadCovered = notLoadPlannedRequiredModules.isEmpty();
        boolean featuresCovered = missingRequiredFeatureCount == 0;
        boolean noStartupFailures = descriptorsCovered && loadCovered && featuresCovered;

        if (!descriptorsCovered) {
            diagnostics.add(moduleCoverageDiagnostic(packId, "Required Ashfall beta modules are missing descriptors", "Missing required modules: " + missingRequiredModules));
        }
        if (!loadCovered) {
            diagnostics.add(moduleCoverageDiagnostic(packId, "Required Ashfall beta modules are not in the load plan", "Not load-planned required modules: " + notLoadPlannedRequiredModules));
        }
        if (!featuresCovered) {
            diagnostics.add(moduleCoverageDiagnostic(packId, "Required Ashfall beta features are missing providers", "feature-graph.json reports missingRequired=" + missingRequiredFeatureCount));
        }

        data.put("allRequiredDescriptorsDiscovered", descriptorsCovered);
        data.put("allRequiredFeaturesProvided", featuresCovered);
        data.put("allRequiredModulesLoadPlanned", loadCovered);
        data.put("allBetaProfileModulesDiscovered", missingBetaProfileModules.isEmpty());
        data.put("betaProfileModuleCount", betaProfileModules.size());
        data.put("betaProfileModules", betaProfileModules);
        data.put("discoveredModuleCount", discoveredModules.size());
        data.put("discoveredBetaProfileModuleCount", betaProfileModules.size() - missingBetaProfileModules.size());
        data.put("discoveredRequiredModuleCount", requiredModules.size() - missingRequiredModules.size());
        data.put("loadPlannedModuleCount", loadPlannedModules.size());
        data.put("loadPlannedRequiredModuleCount", requiredModules.size() - notLoadPlannedRequiredModules.size());
        data.put("missingOptionalFeatureCount", missingOptionalFeatures.size());
        data.put("missingOptionalFeatures", missingOptionalFeatures);
        data.put("missingOptionalModules", missingOptionalModules);
        data.put("missingRecommendedModules", missingRecommendedModules);
        data.put("missingBetaProfileModules", missingBetaProfileModules);
        data.put("missingRequiredFeatureCount", missingRequiredFeatureCount);
        data.put("missingRequiredModules", missingRequiredModules);
        data.put("noRequiredModuleStartupFailures", noStartupFailures);
        data.put("notLoadPlannedRequiredModules", notLoadPlannedRequiredModules);
        data.put("optionalModulesMayRemainDisabled", true);
        data.put("optionalModulesMayRemainExperimental", true);
        data.put("packId", packId);
        data.put("publicBetaOpen", noStartupFailures);
        data.put("publicBetaReady", noStartupFailures);
        data.put("publicReleaseReady", false);
        data.put("requiredFeatureCount", requiredFeatures.size());
        data.put("requiredFeatures", requiredFeatures);
        data.put("requiredModuleCount", requiredModules.size());
        data.put("requiredModules", requiredModules);
        data.put("recommendedModuleCount", recommendedModules.size());
        data.put("recommendedModules", recommendedModules);
        data.put("optionalModuleCount", optionalModules.size());
        data.put("optionalModules", optionalModules);
        data.put("summary", noStartupFailures
                ? "All required Ashfall beta-profile modules are discovered, load-planned, and provide all required features; recommended/optional gaps remain listed as beta limitations."
                : "Ashfall public beta module coverage is blocked by missing required modules or required feature providers.");
        return data;
    }

    private static Map<String, Object> safetyGate(String packId, boolean ready, List<Map<String, Object>> gates, List<EchoNativeDiagnostic> diagnostics) {
        Map<String, Object> data = base("phase13_m31_public_beta_safety_gate", diagnostics);
        data.put("gateCount", gates.size());
        data.put("packId", packId);
        data.put("publicBetaOpen", ready);
        data.put("publicBetaReady", ready);
        data.put("publicReleaseReady", false);
        data.put("safeToOpenPublicBeta", ready);
        data.put("summary", ready
                ? "Public beta safety gate passed for tester-only Ashfall Native Loader Beta."
                : "Public beta safety gate is closed.");
        return data;
    }

    private static Map<String, Object> testerPackageReadiness(String packId, boolean ready, Map<String, Map<String, Object>> reports, List<EchoNativeDiagnostic> diagnostics) {
        Map<String, Object> data = base("phase13_m31_public_beta_tester_package_readiness", diagnostics);
        Map<String, Object> packageData = data(reports.get("first-playtest-candidate-package.json"));
        Map<String, Object> supportData = data(reports.get("first-playtest-support-bundle.json"));
        data.put("candidatePackageId", string(packageData, "candidatePackageId"));
        data.put("packId", packId);
        data.put("publicBetaOpen", ready);
        data.put("publicBetaReady", ready);
        data.put("publicReleaseReady", false);
        data.put("supportBundle", supportData.getOrDefault("supportBundle", Map.of()));
        data.put("supportBundleExportReady", bool(supportData, "supportBundleExportReady"));
        data.put("supportBundleLocalOnly", bool(supportData, "supportBundleLocalOnly"));
        data.put("testerPackageReady", ready);
        data.put("testerSafePackageReady", bool(packageData, "testerSafePackageReady"));
        data.put("summary", ready
                ? "Tester package and local-only support bundle are ready for public beta testers."
                : "Tester package readiness is blocked.");
        return data;
    }

    private static Map<String, Object> rollbackReadiness(String packId, boolean ready, Map<String, Map<String, Object>> reports, List<EchoNativeDiagnostic> diagnostics) {
        Map<String, Object> data = base("phase13_m31_public_beta_rollback_readiness", diagnostics);
        Map<String, Object> rollbackData = data(reports.get("first-playtest-rollback-notes.json"));
        data.put("notes", rollbackData.getOrDefault("notes", List.of()));
        data.put("packId", packId);
        data.put("publicBetaOpen", ready);
        data.put("publicBetaReady", ready);
        data.put("publicReleaseReady", false);
        data.put("rollbackNotesReady", bool(rollbackData, "rollbackNotesReady"));
        data.put("rollbackReady", ready);
        data.put("rollbackRequiredBeforeExternalRelease", bool(rollbackData, "rollbackRequiredBeforeExternalRelease"));
        data.put("summary", ready
                ? "Rollback notes are ready for public beta testers."
                : "Rollback readiness is blocked.");
        return data;
    }

    private static Map<String, Object> knownLimitations(String packId, boolean ready, Map<String, Map<String, Object>> reports, Map<String, Object> moduleCoverage, List<EchoNativeDiagnostic> diagnostics) {
        Map<String, Object> data = base("phase13_m31_public_beta_known_limitations", diagnostics);
        Map<String, Object> limitationData = data(reports.get("first-playtest-known-limitations.json"));
        Map<String, Object> featureData = data(reports.get("feature-graph.json"));
        Map<String, Object> issueData = data(reports.get("native-loader-beta-known-issues.json"));
        data.put("betaKnownIssues", issueData.getOrDefault("issues", List.of()));
        data.put("blockingKnownIssueCount", blockingKnownIssueCount(reports.get("native-loader-beta-known-issues.json")));
        data.put("knownLimitationsReady", bool(limitationData, "knownLimitationsReady"));
        data.put("limitations", limitationData.getOrDefault("limitations", List.of()));
        data.put("missingBetaProfileModules", moduleCoverage.getOrDefault("missingBetaProfileModules", List.of()));
        data.put("missingOptionalFeatures", missingOptionalFeatures(featureData));
        data.put("missingOptionalModules", moduleCoverage.getOrDefault("missingOptionalModules", List.of()));
        data.put("missingRecommendedModules", moduleCoverage.getOrDefault("missingRecommendedModules", List.of()));
        data.put("packId", packId);
        data.put("publicBetaOpen", ready);
        data.put("publicBetaReady", ready);
        data.put("publicReleaseReady", false);
        data.put("summary", ready
                ? "Known limitations are ready for public beta testers."
                : "Known limitations are not ready.");
        return data;
    }

    private static Map<String, Object> m31Completion(String packId, boolean ready, List<Map<String, Object>> gates, List<EchoNativeDiagnostic> diagnostics) {
        Map<String, Object> data = base("phase13_m31_public_beta_opening_completion", diagnostics);
        data.put("gateCount", gates.size());
        data.put("packId", packId);
        data.put("phase13M31Complete", ready);
        data.put("phase13M32Ready", ready);
        data.put("publicBetaOpen", ready);
        data.put("publicBetaReady", ready);
        data.put("publicReleaseReady", false);
        data.put("summary", ready
                ? "M31 is complete; Ashfall Native Loader Beta may open for testers while public release remains closed."
                : "M31 remains blocked.");
        return data;
    }

    private static Map<String, Object> m32Readiness(String packId, boolean ready, List<EchoNativeDiagnostic> diagnostics) {
        Map<String, Object> data = base("phase13_m32_public_beta_monitoring_readiness", diagnostics);
        data.put("packId", packId);
        data.put("phase13M32Ready", ready);
        data.put("publicBetaOpen", ready);
        data.put("publicBetaReady", ready);
        data.put("publicReleaseReady", false);
        data.put("summary", ready
                ? "M32 may begin public beta monitoring and intake; public release remains closed."
                : "M32 remains closed until M31 passes.");
        return data;
    }

    private static Map<String, Object> booleanGate(String packId, Map<String, Map<String, Object>> reports, String reportName, String field, boolean expected, List<EchoNativeDiagnostic> diagnostics) {
        Object actual = data(reports.get(reportName)).get(field);
        boolean pass = actual instanceof Boolean bool && bool == expected;
        Map<String, Object> gate = gate(reportName, field, expected, actual, pass);
        if (!pass) {
            diagnostics.add(gateDiagnostic(packId, reportName, field, String.valueOf(expected)));
        }
        return gate;
    }

    private static Map<String, Object> numberGate(String packId, Map<String, Map<String, Object>> reports, String reportName, String field, int expected, List<EchoNativeDiagnostic> diagnostics) {
        Object actual = data(reports.get(reportName)).get(field);
        boolean pass = actual instanceof Number number && number.intValue() == expected;
        Map<String, Object> gate = gate(reportName, field, expected, actual, pass);
        if (!pass) {
            diagnostics.add(gateDiagnostic(packId, reportName, field, String.valueOf(expected)));
        }
        return gate;
    }

    private static Map<String, Object> booleanValueGate(String packId, String reportName, String field, boolean expected, Object actual, List<EchoNativeDiagnostic> diagnostics) {
        boolean pass = actual instanceof Boolean bool && bool == expected;
        Map<String, Object> gate = gate(reportName, field, expected, actual, pass);
        if (!pass) {
            diagnostics.add(gateDiagnostic(packId, reportName, field, String.valueOf(expected)));
        }
        return gate;
    }

    private static Map<String, Object> numberValueGate(String packId, String reportName, String field, int expected, int actual, List<EchoNativeDiagnostic> diagnostics) {
        boolean pass = actual == expected;
        Map<String, Object> gate = gate(reportName, field, expected, actual, pass);
        if (!pass) {
            diagnostics.add(gateDiagnostic(packId, reportName, field, String.valueOf(expected)));
        }
        return gate;
    }

    private static Map<String, Object> gate(String reportName, String field, Object expected, Object actual, boolean pass) {
        Map<String, Object> gate = new LinkedHashMap<>();
        gate.put("actual", actual == null ? "missing" : actual);
        gate.put("expected", expected);
        gate.put("field", field);
        gate.put("pass", pass);
        gate.put("report", reportName);
        return gate;
    }

    private static EchoNativeDiagnostic gateDiagnostic(String packId, String reportName, String field, String expected) {
        return new EchoNativeDiagnostic(
                "ECHO-NATIVE-M31-PUBLIC-BETA-OPENING-GATE-BLOCKED",
                EchoNativeIssueSeverity.ERROR,
                "Phase 13 M31 public beta opening gate is blocked",
                reportName + " must report " + field + "=" + expected + " before M31 can open public beta.",
                null,
                packId,
                List.of("reports/echo-native/" + packId + "/" + reportName),
                "Resolve public beta package, support, rollback, limitations, crash intake, and candidate gate evidence; rerun M31 verification."
        );
    }

    private static Map<String, Object> readRequiredReport(Path reportPath, Path fixture, String packId, String reportName, List<EchoNativeDiagnostic> diagnostics) throws IOException {
        if (!Files.isRegularFile(reportPath)) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-M31-PUBLIC-BETA-OPENING-REPORT-MISSING",
                    EchoNativeIssueSeverity.ERROR,
                    "Phase 13 M31 required report missing",
                    "The public beta opening verifier requires " + reportName + ".",
                    null,
                    packId,
                    List.of(relativePath(fixture)),
                    "Run M30 candidate verification and first-playtest package gates before M31."
            ));
            return Map.of();
        }
        return EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(reportPath)));
    }

    private static void checkAcceptedReport(String reportName, Path reportPath, Map<String, Object> report, String packId, List<EchoNativeDiagnostic> diagnostics) {
        if (report.isEmpty()) {
            return;
        }
        String status = String.valueOf(report.getOrDefault("status", "MISSING"));
        if (!"PASS".equals(status) && !"PASS_WITH_WARNINGS".equals(status)) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-M31-PUBLIC-BETA-OPENING-UPSTREAM-BLOCKED",
                    EchoNativeIssueSeverity.ERROR,
                    "Phase 13 M31 upstream report is not accepted",
                    "The public beta opening verifier requires PASS or PASS_WITH_WARNINGS " + reportName + ".",
                    null,
                    packId,
                    List.of(relativePath(reportPath)),
                    "Resolve upstream diagnostics before M31 verification."
            ));
        }
    }

    private static Map<String, Object> data(Map<String, Object> report) {
        return EchoNativeJson.asObject(report == null ? null : report.get("data"));
    }

    private static boolean bool(Map<String, Object> data, String key) {
        Object value = data.get(key);
        return value instanceof Boolean bool && bool;
    }

    private static int number(Map<String, Object> data, String key) {
        Object value = data.get(key);
        return value instanceof Number number ? number.intValue() : 0;
    }

    private static List<String> descriptorIds(Map<String, Object> report) {
        Object modules = data(report).get("modules");
        if (!(modules instanceof List<?> list)) {
            return List.of();
        }
        List<String> ids = new ArrayList<>();
        for (Object item : list) {
            Map<String, Object> module = EchoNativeJson.asObject(item);
            String id = string(module, "id");
            if (!id.isBlank()) {
                ids.add(id);
            }
        }
        return ids;
    }

    private static List<String> missing(List<String> required, List<String> actual) {
        List<String> missing = new ArrayList<>();
        for (String value : required) {
            if (!actual.contains(value)) {
                missing.add(value);
            }
        }
        return missing;
    }

    private static List<String> union(List<String> first, List<String> second, List<String> third) {
        List<String> values = new ArrayList<>();
        for (String value : first) {
            if (!values.contains(value)) {
                values.add(value);
            }
        }
        for (String value : second) {
            if (!values.contains(value)) {
                values.add(value);
            }
        }
        for (String value : third) {
            if (!values.contains(value)) {
                values.add(value);
            }
        }
        values.sort(String::compareTo);
        return values;
    }

    private static List<String> missingOptionalFeatures(Map<String, Object> featureData) {
        Object features = featureData.get("features");
        if (!(features instanceof List<?> list)) {
            return List.of();
        }
        List<String> missing = new ArrayList<>();
        for (Object item : list) {
            Map<String, Object> feature = EchoNativeJson.asObject(item);
            if ("missing_optional".equals(string(feature, "status"))) {
                missing.add(string(feature, "featureId"));
            }
        }
        missing.sort(String::compareTo);
        return missing;
    }

    private static List<String> sortedStrings(Object raw) {
        if (!(raw instanceof List<?> list)) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        for (Object item : list) {
            values.add(String.valueOf(item));
        }
        values.sort(String::compareTo);
        return values;
    }

    private static int blockingKnownIssueCount(Map<String, Object> knownIssueReport) {
        Object issues = data(knownIssueReport).get("issues");
        if (!(issues instanceof List<?> list)) {
            return 0;
        }
        int blocking = 0;
        for (Object item : list) {
            Map<String, Object> issue = EchoNativeJson.asObject(item);
            if (!isNonblockingKnownIssue(issue)) {
                blocking++;
            }
        }
        return blocking;
    }

    private static boolean isNonblockingKnownIssue(Map<String, Object> issue) {
        String severity = string(issue, "severity").toLowerCase();
        String category = string(issue, "category").toLowerCase();
        return severity.contains("nonblocking") || category.contains("known_limitation");
    }

    private static Map<String, Object> readPackProfile(Path fixture) throws IOException {
        Path profile = fixture.resolve("echo.pack.json");
        if (!Files.isRegularFile(profile)) {
            return Map.of();
        }
        return EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(profile)));
    }

    private static String string(Map<String, Object> data, String key) {
        Object value = data.get(key);
        return value == null ? "" : String.valueOf(value);
    }

    private static EchoNativeDiagnostic moduleCoverageDiagnostic(String packId, String summary, String detail) {
        return new EchoNativeDiagnostic(
                "ECHO-NATIVE-M31-PUBLIC-BETA-MODULE-COVERAGE-BLOCKED",
                EchoNativeIssueSeverity.ERROR,
                summary,
                detail,
                null,
                packId,
                List.of("reports/echo-native/" + packId + "/public-beta-module-coverage.json"),
                "Restore required Ashfall beta-profile module descriptors, load plan entries, or feature providers before opening public beta."
        );
    }

    private static boolean isBlocking(EchoNativeDiagnostic diagnostic) {
        return diagnostic.severity() == EchoNativeIssueSeverity.ERROR || diagnostic.severity() == EchoNativeIssueSeverity.FATAL;
    }

    private static Map<String, Object> base(String phase, List<EchoNativeDiagnostic> diagnostics) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("bytecodeMutated", false);
        data.put("cacheMutated", false);
        data.put("classloaderCreated", false);
        data.put("commandExecuted", false);
        data.put("configsMutated", false);
        data.put("diagnosticCount", diagnostics.size());
        data.put("diagnosticsCaptured", true);
        data.put("downloadAllowed", false);
        data.put("downloadsStarted", false);
        data.put("filesystemMutated", false);
        data.put("gameClassesResolved", false);
        data.put("generatedEvidenceAt", Instant.EPOCH.toString());
        data.put("jarsMutated", false);
        data.put("launcherInstallsMutated", false);
        data.put("libraryDownloadStarted", false);
        data.put("minecraftLaunched", false);
        data.put("nativeExtractionStarted", false);
        data.put("processLaunched", false);
        data.put("registryInjected", false);
        data.put("registryMutated", false);
        data.put("savesMutated", false);
        data.put("serviceCodeExecuted", false);
        data.put("transformsEnabled", false);
        data.put("transformsPerformed", false);
        data.put("unsafeRuntimeWorkStarted", false);
        data.put("userCachesMutated", false);
        data.put("phase", phase);
        return data;
    }

    private static String relativePath(Path path) {
        Path root = Path.of("").toAbsolutePath().normalize();
        Path normalized = path.toAbsolutePath().normalize();
        if (normalized.startsWith(root)) {
            return root.relativize(normalized).toString().replace('\\', '/');
        }
        return path.toString().replace('\\', '/');
    }
}
