package dev.echo.nativeplatform.loader;

import dev.echo.nativeplatform.contracts.EchoNativeLifecyclePhase;
import dev.echo.nativeplatform.contracts.EchoNativeLoadStatus;
import dev.echo.nativeplatform.contracts.EchoNativeModuleDescriptor;
import dev.echo.nativeplatform.contracts.EchoNativeModuleLoadResult;
import dev.echo.nativeplatform.contracts.EchoNativeMutationReceipt;
import dev.echo.nativeplatform.contracts.EchoNativeRegisteredService;
import dev.echo.nativeplatform.contracts.EchoNativeRuntimeSide;
import dev.echo.nativeplatform.contracts.EchoNativeServiceMutation;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoNativeMutationTruthGateMain {
    private EchoNativeMutationTruthGateMain() {
    }

    public static void main(String[] args) throws Exception {
        requireMapOnlyMutationClaimRejected();
        requireTypedMutationReceiptAccepted();
        requireRegisteredReceiptDoesNotMutate();
        writeJsonReportIfRequested();
        System.out.println("native mutation truth gate PASS");
    }

    private static void writeJsonReportIfRequested() throws Exception {
        String configured = System.getProperty("echo.native.mutationTruthGateReport");
        if (configured == null || configured.isBlank()) {
            return;
        }
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("schema", "echo.native.mutation_truth_gate.v1");
        report.put("generatedAt", "1970-01-01T00:00:00Z");
        report.put("status", "PASS");
        report.put("runtime", "echo_native");
        report.put("moduleIds", List.of("truth_gate_module"));
        report.put("featureBuckets", List.of("blocks", "items", "block_actions", "networking", "save_data"));
        report.put("trustedMutations", List.of(
                "map-only mutation claims rejected",
                "typed MUTATED receipt accepted",
                "typed REGISTERED receipt does not satisfy mutation"
        ));
        report.put("visibleRoutes", List.of());
        report.put("saveEvidence", List.of("honest status downgrade is verified for non-mutating receipts"));
        report.put("networkEvidence", List.of("typed mutation receipt side is carried by EchoNativeServiceMutation"));
        report.put("blockers", List.of());
        NativeLoaderJsonSupport.writeAtomically(Path.of(configured), report);
    }

    private static void requireMapOnlyMutationClaimRejected() {
        EchoNativeModuleLoadResult result = result(
                EchoNativeLoadStatus.MUTATED,
                List.of(),
                List.of(Map.of(
                        "moduleId", "truth_gate_module",
                        "surface", "registry",
                        "action", "metadata_claim",
                        "target", "example",
                        "status", EchoNativeLoadStatus.MUTATED.name()
                ))
        );
        EchoNativeModuleLoadTruthGate.TruthReport report = new EchoNativeModuleLoadTruthGate().verify(result);
        require(!report.mutated(), "Map-only mutation claim must not satisfy MUTATED gate");
        require(report.honestStatus() == EchoNativeLoadStatus.REGISTERED,
                "Map-only mutation claim must downgrade honest status to REGISTERED: " + report.toReport());
        require(!report.statusAccurate(), "Claimed MUTATED status must be inaccurate without typed receipts");
        require(Boolean.TRUE.equals(report.evidence().get("mapOnlyMutationClaimRejected")),
                "Truth evidence must expose rejected map-only mutation claim: " + report.toReport());
    }

    private static void requireTypedMutationReceiptAccepted() {
        EchoNativeMutationReceipt receipt = EchoNativeMutationReceipt.mutated(
                "truth.registry",
                EchoNativeServiceMutation.of(
                        "truth_gate_module",
                        "registry",
                        "register_item",
                        "truth_gate:item",
                        EchoNativeRuntimeSide.COMMON
                ),
                1
        );
        EchoNativeModuleLoadResult result = result(EchoNativeLoadStatus.MUTATED, List.of(receipt), List.of(receipt.toReport()));
        EchoNativeModuleLoadTruthGate.TruthReport report = new EchoNativeModuleLoadTruthGate().verify(result);
        require(report.mutated(), "Typed MUTATED receipt must satisfy MUTATED gate: " + report.toReport());
        require(report.honestStatus() == EchoNativeLoadStatus.MUTATED,
                "Typed MUTATED receipt must keep honest status MUTATED: " + report.toReport());
        require(report.statusAccurate(), "Claimed MUTATED status must be accurate with typed receipt");
    }

    private static void requireRegisteredReceiptDoesNotMutate() {
        EchoNativeMutationReceipt receipt = EchoNativeMutationReceipt.registered(
                "truth.registry",
                EchoNativeServiceMutation.of(
                        "truth_gate_module",
                        "registry",
                        "declare_item",
                        "truth_gate:item",
                        EchoNativeRuntimeSide.COMMON
                ),
                1
        );
        EchoNativeModuleLoadResult result = result(EchoNativeLoadStatus.MUTATED, List.of(receipt), List.of(receipt.toReport()));
        EchoNativeModuleLoadTruthGate.TruthReport report = new EchoNativeModuleLoadTruthGate().verify(result);
        require(!report.mutated(), "REGISTERED typed receipt must not satisfy MUTATED gate");
        require(report.honestStatus() == EchoNativeLoadStatus.REGISTERED,
                "REGISTERED typed receipt must downgrade honest status to REGISTERED: " + report.toReport());
    }

    private static EchoNativeModuleLoadResult result(
            EchoNativeLoadStatus status,
            List<EchoNativeMutationReceipt> receipts,
            List<Map<String, Object>> mutations
    ) {
        return new EchoNativeModuleLoadResult(
                descriptor(),
                status,
                List.of(EchoNativeLifecyclePhase.LOAD_CLASSES, EchoNativeLifecyclePhase.REGISTER_SERVICES),
                List.of(),
                "qa.truth.TruthGateEntrypoint",
                "dev.echo.nativeplatform.loader.EchoNativeModuleClassLoader",
                true,
                "qa.truth.TruthGateEntrypoint",
                List.of(),
                List.of(),
                List.of(new EchoNativeRegisteredService(
                        "truth_gate_module",
                        "truth.registry",
                        EchoNativeMutationTruthGateMain.class.getName(),
                        List.of("registry")
                )),
                receipts,
                mutations,
                List.of()
        );
    }

    private static EchoNativeModuleDescriptor descriptor() {
        return new EchoNativeModuleDescriptor(
                "truth_gate_module",
                "Truth Gate Module",
                "1.0.0-RC1",
                "addon",
                "runtime",
                "qa.truth.TruthGateEntrypoint",
                EchoNativeRuntimeSide.COMMON,
                List.of(),
                List.of(),
                List.of("registry"),
                Path.of("qa/truth-gate/META-INF/echo.mod.json"),
                List.of(Path.of("qa/truth-gate/truth_gate_module.jar"))
        );
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }
}
