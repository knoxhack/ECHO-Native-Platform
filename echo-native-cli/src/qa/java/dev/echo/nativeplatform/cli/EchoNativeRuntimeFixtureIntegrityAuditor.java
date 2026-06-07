package dev.echo.nativeplatform.cli;

import dev.echo.nativeplatform.contracts.EchoNativeDiagnostic;
import dev.echo.nativeplatform.contracts.EchoNativeIssueSeverity;
import dev.echo.nativeplatform.diagnostics.EchoNativeJson;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class EchoNativeRuntimeFixtureIntegrityAuditor {
    EchoNativeRuntimeFixtureIntegrityOutcome audit(
            String packId,
            Path fixture,
            Map<String, Path> requiredReports
    ) throws IOException {
        List<EchoNativeDiagnostic> diagnostics = new ArrayList<>();
        Map<String, Object> handoff = readRequiredReport(requiredReports.get("runtime-fixture-handoff.json"), packId, "runtime-fixture-handoff.json", diagnostics);
        List<Map<String, Object>> handoffItems = handoffItems(handoff);
        if (handoffItems.isEmpty()) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-RUNTIME-FIXTURE-INTEGRITY-HANDOFF-MISSING",
                    EchoNativeIssueSeverity.ERROR,
                    "Runtime fixture integrity handoff items are missing",
                    "Runtime fixture integrity audit requires runtime fixture handoff items.",
                    null,
                    packId,
                    List.of(relativeReportPath(requiredReports.get("runtime-fixture-handoff.json"))),
                    "Run phase13 prepare runtime-fixture-handoff before auditing fixture integrity."
            ));
        }

        Path approvalsPath = fixture.resolve("runtime-fixture-approvals.json");
        Map<String, Map<String, Object>> approvals = approvals(approvalsPath, packId, diagnostics);
        List<Map<String, Object>> checks = new ArrayList<>();
        List<Map<String, Object>> manifestItems = new ArrayList<>();
        for (Map<String, Object> handoffItem : handoffItems) {
            String artifactId = String.valueOf(handoffItem.getOrDefault("artifactId", ""));
            String expectedFixturePath = String.valueOf(handoffItem.getOrDefault("expectedFixturePath", ""));
            Path expectedPath = fixture.resolve(expectedFixturePath).normalize();
            boolean relative = !Path.of(expectedFixturePath).isAbsolute() && !expectedFixturePath.contains("..");
            boolean present = relative && Files.isRegularFile(expectedPath);
            long byteSize = present ? Files.size(expectedPath) : 0L;
            String sha256 = present ? sha256(expectedPath) : "";
            Map<String, Object> approval = approvals.getOrDefault(artifactId, Map.of());
            boolean approvalPresent = !approval.isEmpty();
            boolean approvalMatches = approvalMatches(approval, expectedFixturePath, byteSize, sha256);
            boolean integrityReady = present && approvalMatches;

            Map<String, Object> check = baseItem(artifactId, expectedFixturePath);
            check.put("approvalPresent", approvalPresent);
            check.put("approvalSha256", String.valueOf(approval.getOrDefault("sha256", "")));
            check.put("approvalByteSize", approval.getOrDefault("byteSize", 0));
            check.put("byteSize", byteSize);
            check.put("filePresent", present);
            check.put("hashAlgorithm", "SHA-256");
            check.put("integrityReady", integrityReady);
            check.put("pathRelative", relative);
            check.put("sha256", sha256);
            checks.add(check);

            Map<String, Object> manifestItem = baseItem(artifactId, expectedFixturePath);
            manifestItem.put("byteSize", byteSize);
            manifestItem.put("hashAlgorithm", "SHA-256");
            manifestItem.put("sha256", sha256.isBlank() ? "<pending-file>" : sha256);
            manifestItem.put("approvalFile", relativeFixturePath(fixture, approvalsPath));
            manifestItems.add(manifestItem);

            if (!present) {
                diagnostics.add(new EchoNativeDiagnostic(
                        "ECHO-NATIVE-RUNTIME-FIXTURE-INTEGRITY-FILE-MISSING",
                        EchoNativeIssueSeverity.ERROR,
                        "Runtime fixture integrity file is missing",
                        artifactId + " requires a local fixture file before SHA-256 integrity can be audited.",
                        null,
                        packId,
                        List.of(fixture.resolve(expectedFixturePath).toString().replace('\\', '/')),
                        "Supply only an already-authorized local artifact outside this CLI, then rerun the integrity audit."
                ));
            }
            if (!approvalMatches) {
                diagnostics.add(new EchoNativeDiagnostic(
                        approvalPresent
                                ? "ECHO-NATIVE-RUNTIME-FIXTURE-INTEGRITY-APPROVAL-MISMATCH"
                                : "ECHO-NATIVE-RUNTIME-FIXTURE-INTEGRITY-APPROVAL-MISSING",
                        EchoNativeIssueSeverity.ERROR,
                        approvalPresent ? "Runtime fixture integrity approval does not match" : "Runtime fixture integrity approval is missing",
                        artifactId + " requires reviewed approval evidence with matching localPath, byteSize, sha256, and blocked download/extraction flags.",
                        null,
                        packId,
                        List.of(relativeFixturePath(fixture, approvalsPath)),
                        "Update runtime-fixture-approvals.json only after reviewing the supplied local file and matching its SHA-256 and byte size."
                ));
            }
        }
        checks.sort(Comparator.comparing(item -> String.valueOf(item.get("artifactId"))));
        manifestItems.sort(Comparator.comparing(item -> String.valueOf(item.get("artifactId"))));

        boolean integrityReady = !checks.isEmpty() && checks.stream().allMatch(item -> Boolean.TRUE.equals(item.get("integrityReady")));
        List<EchoNativeDiagnostic> sortedDiagnostics = diagnostics.stream()
                .sorted(Comparator.comparing(EchoNativeDiagnostic::code)
                        .thenComparing(diagnostic -> diagnostic.moduleId() == null ? "" : diagnostic.moduleId())
                        .thenComparing(EchoNativeDiagnostic::summary))
                .toList();
        return new EchoNativeRuntimeFixtureIntegrityOutcome(
                packId,
                auditReport(packId, integrityReady, checks, approvalsPath, sortedDiagnostics),
                integrityManifest(packId, integrityReady, manifestItems, sortedDiagnostics),
                sortedDiagnostics
        );
    }

    private static Map<String, Object> auditReport(
            String packId,
            boolean integrityReady,
            List<Map<String, Object>> checks,
            Path approvalsPath,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        int presentCount = (int) checks.stream().filter(check -> Boolean.TRUE.equals(check.get("filePresent"))).count();
        int hashVerifiedCount = (int) checks.stream().filter(check -> Boolean.TRUE.equals(check.get("integrityReady"))).count();
        Map<String, Object> data = base("phase13_m17_runtime_fixture_integrity_audit", diagnostics);
        data.put("approvalFile", approvalsPath.toString().replace('\\', '/'));
        data.put("hashAlgorithm", "SHA-256");
        data.put("hashVerifiedCount", hashVerifiedCount);
        data.put("integrityCheckCount", checks.size());
        data.put("integrityChecks", checks);
        data.put("integrityReady", integrityReady);
        data.put("packId", packId);
        data.put("phase13M17LaunchBlocked", !integrityReady);
        data.put("presentFileCount", presentCount);
        data.put("summary", integrityReady
                ? "Runtime fixture integrity evidence is complete for M17."
                : "Runtime fixture integrity evidence is incomplete; M17 remains blocked.");
        return data;
    }

    private static Map<String, Object> integrityManifest(
            String packId,
            boolean integrityReady,
            List<Map<String, Object>> manifestItems,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m17_runtime_fixture_integrity_manifest", diagnostics);
        data.put("hashAlgorithm", "SHA-256");
        data.put("integrityManifestItems", manifestItems);
        data.put("integrityManifestItemCount", manifestItems.size());
        data.put("integrityReady", integrityReady);
        data.put("manualIntakeOnly", true);
        data.put("packId", packId);
        data.put("summary", "Runtime fixture integrity evidence template for reviewed local files.");
        return data;
    }

    private static boolean approvalMatches(Map<String, Object> approval, String expectedFixturePath, long byteSize, String sha256) {
        return !approval.isEmpty()
                && Boolean.TRUE.equals(approval.get("reviewed"))
                && Boolean.TRUE.equals(approval.get("approved"))
                && "approved".equals(approval.get("reviewStatus"))
                && expectedFixturePath.equals(String.valueOf(approval.getOrDefault("localPath", "")))
                && Boolean.FALSE.equals(approval.get("downloadsAllowed"))
                && Boolean.FALSE.equals(approval.get("extractionAllowed"))
                && Long.valueOf(byteSize).equals(asLong(approval.get("byteSize")))
                && !sha256.isBlank()
                && sha256.equals(String.valueOf(approval.getOrDefault("sha256", "")));
    }

    private static String sha256(Path path) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream input = Files.newInputStream(path)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = input.read(buffer)) != -1) {
                    digest.update(buffer, 0, read);
                }
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 digest is unavailable", ex);
        }
    }

    private static List<Map<String, Object>> handoffItems(Map<String, Object> handoff) {
        Map<String, Object> data = EchoNativeJson.asObject(handoff.get("data"));
        Object rawItems = data.get("handoffItems");
        if (!(rawItems instanceof List<?> list)) {
            return List.of();
        }
        List<Map<String, Object>> items = new ArrayList<>();
        for (Object raw : list) {
            Map<String, Object> item = EchoNativeJson.asObject(raw);
            if (!String.valueOf(item.getOrDefault("artifactId", "")).isBlank()) {
                items.add(item);
            }
        }
        items.sort(Comparator.comparing(item -> String.valueOf(item.get("artifactId"))));
        return List.copyOf(items);
    }

    private static Map<String, Map<String, Object>> approvals(
            Path approvalsPath,
            String packId,
            List<EchoNativeDiagnostic> diagnostics
    ) throws IOException {
        if (!Files.isRegularFile(approvalsPath)) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-RUNTIME-FIXTURE-INTEGRITY-APPROVAL-FILE-MISSING",
                    EchoNativeIssueSeverity.ERROR,
                    "Runtime fixture integrity approval file is missing",
                    "runtime-fixture-approvals.json is required before fixture hashes can be trusted for M17.",
                    null,
                    packId,
                    List.of(approvalsPath.toString().replace('\\', '/')),
                    "Create this file only after approved local runtime artifacts are supplied, hashed, and reviewed."
            ));
            return Map.of();
        }
        Map<String, Object> root = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(approvalsPath)));
        Object rawApprovals = root.get("approvals");
        if (!(rawApprovals instanceof List<?> list)) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-RUNTIME-FIXTURE-INTEGRITY-APPROVAL-FILE-INVALID",
                    EchoNativeIssueSeverity.ERROR,
                    "Runtime fixture integrity approval file is invalid",
                    "runtime-fixture-approvals.json must contain an approvals array.",
                    null,
                    packId,
                    List.of(approvalsPath.toString().replace('\\', '/')),
                    "Use runtime-fixture-integrity-manifest.json as the reviewed approval evidence source."
            ));
            return Map.of();
        }
        Map<String, Map<String, Object>> approvals = new LinkedHashMap<>();
        for (Object raw : list) {
            Map<String, Object> approval = EchoNativeJson.asObject(raw);
            String artifactId = String.valueOf(approval.getOrDefault("artifactId", ""));
            if (!artifactId.isBlank()) {
                approvals.put(artifactId, approval);
            }
        }
        return approvals;
    }

    private static Map<String, Object> readRequiredReport(
            Path reportPath,
            String packId,
            String reportName,
            List<EchoNativeDiagnostic> diagnostics
    ) throws IOException {
        if (reportPath == null || !Files.isRegularFile(reportPath)) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-RUNTIME-FIXTURE-INTEGRITY-REPORT-MISSING",
                    EchoNativeIssueSeverity.ERROR,
                    "Runtime fixture integrity required report missing",
                    "Runtime fixture integrity audit requires " + reportName + ".",
                    null,
                    packId,
                    reportPath == null ? List.of() : List.of(relativeReportPath(reportPath)),
                    "Regenerate runtime fixture handoff reports before auditing fixture integrity."
            ));
            return Map.of();
        }
        return EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(reportPath)));
    }

    private static Map<String, Object> baseItem(String artifactId, String expectedFixturePath) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("artifactId", artifactId);
        item.put("classloaderCreated", false);
        item.put("commandExecuted", false);
        item.put("downloadsAllowed", false);
        item.put("expectedFixturePath", expectedFixturePath);
        item.put("filesystemMutated", false);
        item.put("gameClassesResolved", false);
        item.put("nativeExtractionStarted", false);
        item.put("processLaunched", false);
        item.put("registryInjected", false);
        item.put("registryMutated", false);
        item.put("safeToAutoPopulate", false);
        return item;
    }

    private static Map<String, Object> base(String phase, List<EchoNativeDiagnostic> diagnostics) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("bytecodeMutated", false);
        data.put("cacheMutated", false);
        data.put("classloaderCreated", false);
        data.put("commandExecuted", false);
        data.put("diagnosticCount", diagnostics.size());
        data.put("diagnosticsCaptured", true);
        data.put("downloadAllowed", false);
        data.put("downloadsAllowed", false);
        data.put("dryRunOnly", true);
        data.put("filesystemMutated", false);
        data.put("gameClassesResolved", false);
        data.put("gameProcessLaunched", false);
        data.put("libraryDownloadStarted", false);
        data.put("minecraftLaunched", false);
        data.put("nativeExtractionStarted", false);
        data.put("processLaunched", false);
        data.put("registryInjected", false);
        data.put("registryMutated", false);
        data.put("reportOnly", true);
        data.put("safeToAutoPopulate", false);
        data.put("transformsEnabled", false);
        data.put("transformsPerformed", false);
        data.put("unsafeRuntimeWorkStarted", false);
        data.put("phase", phase);
        return data;
    }

    private static Long asLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static String relativeReportPath(Path path) {
        if (path == null) {
            return "";
        }
        Path root = Path.of("").toAbsolutePath().normalize();
        Path normalized = path.toAbsolutePath().normalize();
        if (normalized.startsWith(root)) {
            return root.relativize(normalized).toString().replace('\\', '/');
        }
        return path.toString().replace('\\', '/');
    }

    private static String relativeFixturePath(Path fixture, Path path) {
        Path normalizedFixture = fixture.toAbsolutePath().normalize();
        Path normalizedPath = path.toAbsolutePath().normalize();
        if (normalizedPath.startsWith(normalizedFixture)) {
            return normalizedFixture.relativize(normalizedPath).toString().replace('\\', '/');
        }
        return path.toString().replace('\\', '/');
    }
}
