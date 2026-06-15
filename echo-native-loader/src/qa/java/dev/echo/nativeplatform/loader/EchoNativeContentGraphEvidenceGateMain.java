package dev.echo.nativeplatform.loader;

import dev.echo.nativeplatform.contracts.EchoNativeContentGraph;
import dev.echo.nativeplatform.contracts.EchoNativeDiagnostic;
import dev.echo.nativeplatform.contracts.EchoNativeIssueSeverity;
import dev.echo.nativeplatform.diagnostics.EchoNativeJson;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public final class EchoNativeContentGraphEvidenceGateMain {
    private EchoNativeContentGraphEvidenceGateMain() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            throw new IllegalArgumentException("Expected ECHO-Modules repository root as the first argument.");
        }
        Path modulesRoot = Path.of(args[0]).toAbsolutePath().normalize();
        Path contentGraphRoot = modulesRoot.resolve("dist").resolve("echo-module-release");
        require(Files.isDirectory(contentGraphRoot),
                "Generated module release graph root is missing: " + contentGraphRoot);

        EchoNativeScanResult scanResult = new EchoNativeDescriptorScanner().scanProduct(modulesRoot);
        EchoNativeContentGraph evidence = new EchoNativeContentGraphPlanner().plan(
                contentGraphRoot,
                scanResult.descriptors().stream()
                        .map(descriptor -> descriptor.id())
                        .sorted()
                        .distinct()
                        .toList()
        );
        Path canonicalEvidencePath = contentGraphRoot.resolve("content-graph-evidence.json");
        Map<String, Object> canonicalEvidence = Files.isRegularFile(canonicalEvidencePath)
                ? EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(canonicalEvidencePath)))
                : Map.of();

        require("echo.content_graph.evidence.v1".equals(evidence.schemaVersion()),
                "Unexpected content graph evidence schema: " + evidence.schemaVersion());
        if (!canonicalEvidence.isEmpty()) {
            requireMatchesCanonical(canonicalEvidencePath, canonicalEvidence, evidence);
        }
        require(scanResult.descriptors().size() >= 100,
                "Expected at least 100 module descriptors, found " + scanResult.descriptors().size());
        require(evidence.moduleCount() >= 100,
                "Expected at least 100 content graph modules, found " + evidence.moduleCount());
        require(evidence.nodeCount() >= 4_000,
                "Expected at least 4000 content graph nodes, found " + evidence.nodeCount());
        require(evidence.edgeCount() >= 5_000,
                "Expected at least 5000 content graph edges, found " + evidence.edgeCount());
        require(evidence.featureCount() >= 1_000,
                "Expected at least 1000 content graph features, found " + evidence.featureCount());
        require(evidence.exportPlanCount() >= 100,
                "Expected at least 100 export plans, found " + evidence.exportPlanCount());
        require(evidence.hytaleBlockerCount() == 9,
                "Expected exactly 9 Hytale blocked nodes, found " + evidence.hytaleBlockerCount());
        require(openlandsSummary(evidence).map(summary -> number(summary, "hytaleBlockers") == 9).orElse(false),
                "Expected echoopenlandsprotocol to own the 9 Hytale blocked nodes.");
        require(noBlocking(evidence.diagnostics()),
                "Content graph evidence produced blocking diagnostics: " + evidence.diagnostics());

        System.out.println("native content graph evidence gate PASS modules=" + evidence.moduleCount()
                + " nodes=" + evidence.nodeCount()
                + " edges=" + evidence.edgeCount()
                + " features=" + evidence.featureCount()
                + " exportPlans=" + evidence.exportPlanCount()
                + " hytaleBlockers=" + evidence.hytaleBlockerCount());
    }

    private static void requireMatchesCanonical(
            Path canonicalEvidencePath,
            Map<String, Object> canonicalEvidence,
            EchoNativeContentGraph evidence) {
        require("echo.content_graph.evidence.v1".equals(String.valueOf(canonicalEvidence.get("schemaVersion"))),
                "Canonical content graph evidence schema mismatch in " + canonicalEvidencePath);
        require(number(canonicalEvidence, "moduleCount") == evidence.moduleCount(),
                "Canonical moduleCount mismatch: canonical=" + number(canonicalEvidence, "moduleCount")
                        + " loaded=" + evidence.moduleCount());
        if (canonicalEvidence.containsKey("graphCount")) {
            require(number(canonicalEvidence, "graphCount") == evidence.moduleCount(),
                    "Canonical graphCount mismatch: canonical=" + number(canonicalEvidence, "graphCount")
                            + " loaded=" + evidence.moduleCount());
        }
        require(number(canonicalEvidence, "nodeCount") == evidence.nodeCount(),
                "Canonical nodeCount mismatch: canonical=" + number(canonicalEvidence, "nodeCount")
                        + " loaded=" + evidence.nodeCount());
        require(number(canonicalEvidence, "edgeCount") == evidence.edgeCount(),
                "Canonical edgeCount mismatch: canonical=" + number(canonicalEvidence, "edgeCount")
                        + " loaded=" + evidence.edgeCount());
        require(number(canonicalEvidence, "featureCount") == evidence.featureCount(),
                "Canonical featureCount mismatch: canonical=" + number(canonicalEvidence, "featureCount")
                        + " loaded=" + evidence.featureCount());
        require(number(canonicalEvidence, "exportPlanCount") == evidence.exportPlanCount(),
                "Canonical exportPlanCount mismatch: canonical=" + number(canonicalEvidence, "exportPlanCount")
                        + " loaded=" + evidence.exportPlanCount());
        require(number(canonicalEvidence, "hytaleBlockerCount") == evidence.hytaleBlockerCount(),
                "Canonical hytaleBlockerCount mismatch: canonical=" + number(canonicalEvidence, "hytaleBlockerCount")
                        + " loaded=" + evidence.hytaleBlockerCount());
        require(moduleSummary(canonicalEvidence, "echoopenlandsprotocol")
                        .map(summary -> number(summary, "hytaleBlockerCount") == 9)
                        .orElse(false),
                "Canonical evidence must keep echoopenlandsprotocol at 9 Hytale blocked nodes until source changes.");
    }

    private static java.util.Optional<Map<String, Object>> openlandsSummary(EchoNativeContentGraph evidence) {
        return evidence.modules().stream()
                .filter(module -> "echoopenlandsprotocol".equals(module.get("moduleId")))
                .findFirst();
    }

    private static java.util.Optional<Map<String, Object>> moduleSummary(Map<String, Object> evidence, String moduleId) {
        Object modules = evidence.get("modules");
        if (!(modules instanceof List<?> list)) {
            return java.util.Optional.empty();
        }
        return list.stream()
                .filter(item -> item instanceof Map<?, ?>)
                .map(EchoNativeJson::asObject)
                .filter(module -> moduleId.equals(module.get("moduleId")))
                .findFirst();
    }

    private static int number(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value instanceof Number number ? number.intValue() : 0;
    }

    private static boolean noBlocking(List<EchoNativeDiagnostic> diagnostics) {
        return diagnostics.stream().noneMatch(diagnostic ->
                diagnostic.severity() == EchoNativeIssueSeverity.ERROR
                        || diagnostic.severity() == EchoNativeIssueSeverity.FATAL);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }
}
