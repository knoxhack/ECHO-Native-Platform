package dev.echo.nativeplatform.loader;

import dev.echo.nativeplatform.contracts.EchoNativeLoadStatus;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public final class EchoNativeAgent3RegistryParityGateMain {
    private static final List<String> FIRST_CLASS_KINDS = EchoNativeRegistryHost.firstClassRegistryKinds();

    private EchoNativeAgent3RegistryParityGateMain() {
    }

    public static void main(String[] args) {
        assertLiveRegistryBridgeDefaultEvidenceShape();
        assertRegisteredOnlyFailsReleaseGate();
        assertResolvedOnlyFailsReleaseGate();
        assertLiveNonMutatingStatusesFailReleaseGate();
        assertFailedFirstClassRegistrationsFailReleaseGate();
        assertDuplicateFirstClassRegistrationsFailReleaseGate();
        assertStatusOnlyMutatedFailsReleaseGate();
        assertMismatchedMutationProofFailsReleaseGate();
        assertMissingTrustMetadataProofFailsReleaseGate();
        assertLifecycleOnlyMutationProofFailsReleaseGate();
        assertBridgeEvidenceMismatchIsReported();
        assertBridgeEvidenceCountAndMapMismatchIsReported();
        assertBridgeEvidenceRecordMapPayloadMismatchIsReported();
        assertRegistryHostCanonicalizesDescriptorShapedIds();
        assertMutatedFirstClassKindsPassReleaseGate();
        assertDefaultProductBridgeMutatesEveryFirstClassKind();
        System.out.println("agent3 native registry parity gate PASS firstClassKinds=" + FIRST_CLASS_KINDS.size());
    }

    private static void assertLiveRegistryBridgeDefaultEvidenceShape() {
        NativeLoaderLiveRegistryBridge bridge = new NativeLoaderLiveRegistryBridge() {
        };
        Map<String, Object> evidence = bridge.registryEvidence();
        require(((Number) evidence.get("mutatedRecordCount")).intValue() == 0,
                "default live registry bridge evidence must report zero mutated records");
        require(stringList(evidence.get("mutatedRecordIds")).isEmpty(),
                "default live registry bridge evidence must expose an empty mutatedRecordIds list");
        require(object(evidence.get("mutatedRecords")).isEmpty(),
                "default live registry bridge evidence must expose an empty mutatedRecords map");
        require(!Boolean.TRUE.equals(evidence.get("nativeRegistryMutationSupported")),
                "default live registry bridge evidence must not claim mutation support");
    }

    private static void assertRegisteredOnlyFailsReleaseGate() {
        for (String kind : FIRST_CLASS_KINDS) {
            EchoNativeRegistryHost host = new EchoNativeRegistryHost();
            host.attachLiveBridge(new FixedStatusBridge(EchoNativeLoadStatus.REGISTERED));
            String probeId = "echoagent3:registered_only_" + kind + "_probe";
            EchoNativeLoadStatus status = host.registerDeclared(
                    "echoagent3",
                    kind,
                    probeId,
                    Map.of()
            );
            require(status == EchoNativeLoadStatus.REGISTERED,
                    "registered-only bridge must preserve REGISTERED status for " + kind);
            require(host.registeredOnlyFirstClassRegistryKinds().equals(List.of(kind)),
                    "registered-only first-class " + kind + " must be reported as release-blocking");
            require(host.registeredOnlyFirstClassRegistryIds().equals(List.of(probeId)),
                    "registered-only first-class " + kind + " must expose the concrete blocked id");
            require(host.registeredOnlyFirstClassRegistryIdsByKind()
                            .equals(Map.of(kind, List.of(probeId))),
                    "registered-only first-class " + kind + " must expose the concrete blocked id by kind");
            Map<String, Object> hostReport = host.toReport();
            require(stringList(hostReport.get("registeredOnlyFirstClassRegistryIds"))
                            .equals(List.of(probeId)),
                    "registry host top-level report must expose registered-only ids for " + kind);
            require(stringListMap(hostReport.get("registeredOnlyFirstClassRegistryIdsByKind"))
                            .equals(Map.of(kind, List.of(probeId))),
                    "registry host top-level report must expose registered-only ids by kind for " + kind);
            Map<String, Object> coverage = host.registryMutationCoverage();
            require(stringList(coverage.get("registeredOnlyFirstClassRegistryIds"))
                            .equals(List.of(probeId)),
                    "registry mutation coverage must expose registered-only ids for " + kind);
            require(stringListMap(coverage.get("registeredOnlyFirstClassRegistryIdsByKind"))
                            .equals(Map.of(kind, List.of(probeId))),
                    "registry mutation coverage must expose registered-only ids by kind for " + kind);
            require(!host.allDeclaredRegistryKindsTrusted(),
                    "registered-only first-class " + kind + " must not satisfy trusted mutation coverage");
        }
    }

    private static void assertResolvedOnlyFailsReleaseGate() {
        for (String kind : FIRST_CLASS_KINDS) {
            EchoNativeRegistryHost host = new EchoNativeRegistryHost();
            host.attachLiveBridge(new FixedStatusBridge(EchoNativeLoadStatus.RESOLVED));
            String probeId = "echoagent3:resolved_only_" + kind + "_probe";
            EchoNativeLoadStatus status = host.registerDeclared(
                    "echoagent3",
                    kind,
                    probeId,
                    Map.of()
            );
            require(status == EchoNativeLoadStatus.RESOLVED,
                    "resolved-only bridge must preserve RESOLVED status for " + kind);
            require(host.registeredOnlyFirstClassRegistryKinds().equals(List.of(kind)),
                    "resolved-only first-class " + kind + " must be reported as release-blocking");
            require(host.registeredOnlyFirstClassRegistryIds().equals(List.of(probeId)),
                    "resolved-only first-class " + kind + " must expose the concrete blocked id");
            require(host.registeredOnlyFirstClassRegistryIdsByKind()
                            .equals(Map.of(kind, List.of(probeId))),
                    "resolved-only first-class " + kind + " must expose the concrete blocked id by kind");
            Map<String, Object> kindReport = object(host.registryKindReports().get(kind));
            require(object(kindReport.get("statusCounts")).equals(Map.of("RESOLVED", 1)),
                    "resolved-only first-class " + kind + " kind report must preserve RESOLVED status counts");
            require(((Number) kindReport.get("registeredOnlyCount")).intValue() == 1,
                    "resolved-only first-class " + kind
                            + " must count as registered-only/non-mutated release evidence");
            require(!host.allDeclaredRegistryKindsTrusted(),
                    "resolved-only first-class " + kind + " must not satisfy trusted mutation coverage");
            require(host.trustedRegistryMutatedEntryCount() == 0,
                    "resolved-only first-class " + kind + " must not be counted as trusted mutation");
        }
    }

    private static void assertLiveNonMutatingStatusesFailReleaseGate() {
        for (EchoNativeLoadStatus liveStatus : List.of(
                EchoNativeLoadStatus.DISCOVERED,
                EchoNativeLoadStatus.LOADED,
                EchoNativeLoadStatus.UNSUPPORTED
        )) {
            for (String kind : FIRST_CLASS_KINDS) {
                EchoNativeRegistryHost host = new EchoNativeRegistryHost();
                host.attachLiveBridge(new FixedStatusBridge(liveStatus));
                String probeId = "echoagent3:" + liveStatus.name().toLowerCase() + "_only_" + kind + "_probe";
                EchoNativeLoadStatus status = host.registerDeclared(
                        "echoagent3",
                        kind,
                        probeId,
                        Map.of()
                );
                require(status == liveStatus,
                        "live bridge status " + liveStatus.name() + " must be preserved for " + kind);
                require(host.registeredOnlyFirstClassRegistryKinds().equals(List.of(kind)),
                        liveStatus.name() + " first-class " + kind + " must be reported as release-blocking");
                require(host.registeredOnlyFirstClassRegistryIds().equals(List.of(probeId)),
                        liveStatus.name() + " first-class " + kind + " must expose the concrete blocked id");
                Map<String, Object> kindReport = object(host.registryKindReports().get(kind));
                require(object(kindReport.get("statusCounts")).equals(Map.of(liveStatus.name(), 1)),
                        liveStatus.name() + " first-class " + kind + " kind report must preserve live status counts");
                require(((Number) kindReport.get("registeredOnlyCount")).intValue() == 1,
                        liveStatus.name() + " first-class " + kind
                                + " must count as non-mutated release evidence");
                Map<String, Object> entryReport = allEntries(host).get(0).toReport();
                require(Boolean.TRUE.equals(entryReport.get("registeredOnly")),
                        liveStatus.name() + " first-class " + kind + " entry report must stay release-blocking");
                require(liveStatus.name().equals(entryReport.get("status")),
                        liveStatus.name() + " first-class " + kind + " entry report must preserve live status");
                require(!host.allDeclaredRegistryKindsTrusted(),
                        liveStatus.name() + " first-class " + kind + " must not satisfy trusted mutation coverage");
                require(host.trustedRegistryMutatedEntryCount() == 0,
                        liveStatus.name() + " first-class " + kind + " must not be counted as trusted mutation");
            }
        }
    }

    private static void assertFailedFirstClassRegistrationsFailReleaseGate() {
        for (String kind : FIRST_CLASS_KINDS) {
            EchoNativeRegistryHost host = new EchoNativeRegistryHost();
            host.attachLiveBridge(new FixedStatusBridge(EchoNativeLoadStatus.FAILED));
            String probeId = "echoagent3:failed_" + kind + "_probe";
            EchoNativeLoadStatus status = host.registerDeclared(
                    "echoagent3",
                    kind,
                    probeId,
                    Map.of()
            );
            require(status == EchoNativeLoadStatus.FAILED,
                    "failed bridge must preserve FAILED status for " + kind);
            require(host.registeredOnlyFirstClassRegistryKinds().isEmpty(),
                    "failed first-class " + kind + " must not be mislabeled as registered-only");
            require(host.untrustedMutationFirstClassRegistryKinds().isEmpty(),
                    "failed first-class " + kind + " must not be mislabeled as untrusted mutation");
            require(host.failedFirstClassRegistryKinds().equals(List.of(kind)),
                    "failed first-class " + kind + " must be reported as release-blocking failure");
            require(host.failedFirstClassRegistryIds().equals(List.of(probeId)),
                    "failed first-class " + kind + " must expose the concrete failed id");
            require(host.failedFirstClassRegistryIdsByKind().equals(Map.of(kind, List.of(probeId))),
                    "failed first-class " + kind + " must expose the concrete failed id by kind");
            Map<String, Object> hostReport = host.toReport();
            require(stringList(hostReport.get("failedFirstClassRegistryKinds")).equals(List.of(kind)),
                    "registry host top-level report must expose failed first-class kinds for " + kind);
            require(stringList(hostReport.get("failedFirstClassRegistryIds")).equals(List.of(probeId)),
                    "registry host top-level report must expose failed first-class ids for " + kind);
            require(stringListMap(hostReport.get("failedFirstClassRegistryIdsByKind"))
                            .equals(Map.of(kind, List.of(probeId))),
                    "registry host top-level report must expose failed first-class ids by kind for " + kind);
            Map<String, Object> coverage = host.registryMutationCoverage();
            require(stringList(coverage.get("failedFirstClassRegistryKinds")).equals(List.of(kind)),
                    "registry mutation coverage must expose failed first-class kinds for " + kind);
            require(stringList(coverage.get("failedFirstClassRegistryIds")).equals(List.of(probeId)),
                    "registry mutation coverage must expose failed first-class ids for " + kind);
            require(stringListMap(coverage.get("failedFirstClassRegistryIdsByKind"))
                            .equals(Map.of(kind, List.of(probeId))),
                    "registry mutation coverage must expose failed first-class ids by kind for " + kind);
            require(!host.allDeclaredRegistryKindsTrusted(),
                    "failed first-class " + kind + " must not satisfy trusted mutation coverage");
        }
    }

    private static void assertDuplicateFirstClassRegistrationsFailReleaseGate() {
        for (String kind : FIRST_CLASS_KINDS) {
            EchoNativeRegistryHost host = new EchoNativeRegistryHost();
            host.attachLiveBridge(new ProofBackedMutatingBridge());
            String probeId = "echoagent3:duplicate_" + kind + "_probe";
            EchoNativeLoadStatus firstStatus = host.registerDeclared(
                    "echoagent3",
                    kind,
                    probeId,
                    Map.of("attempt", "first")
            );
            require(firstStatus == EchoNativeLoadStatus.MUTATED,
                    "first duplicate fixture registration must mutate " + kind);
            EchoNativeLoadStatus duplicateStatus = host.registerDeclared(
                    "echoagent3",
                    kind,
                    probeId,
                    Map.of("attempt", "duplicate")
            );
            require(duplicateStatus == EchoNativeLoadStatus.FAILED,
                    "duplicate first-class " + kind + " must fail instead of resolving without mutation");
            require(host.failedFirstClassRegistryKinds().equals(List.of(kind)),
                    "duplicate first-class " + kind + " must be reported as release-blocking failure");
            require(host.failedFirstClassRegistryIds().equals(List.of(probeId)),
                    "duplicate first-class " + kind + " must expose the concrete duplicate id");
            require(host.failedFirstClassRegistryIdsByKind().equals(Map.of(kind, List.of(probeId))),
                    "duplicate first-class " + kind + " must expose the duplicate id by kind");
            require(host.registeredOnlyFirstClassRegistryKinds().isEmpty(),
                    "duplicate first-class " + kind + " must not be mislabeled as registered-only");
            require(host.untrustedMutationFirstClassRegistryKinds().isEmpty(),
                    "duplicate first-class " + kind + " must not be mislabeled as untrusted mutation");
            require(!host.allDeclaredRegistryKindsTrusted(),
                    "duplicate first-class " + kind + " must fail trusted mutation coverage");
            Map<String, Object> hostReport = host.toReport();
            require(stringList(hostReport.get("failedFirstClassRegistryKinds")).equals(List.of(kind)),
                    "registry host top-level report must expose duplicate failed kind for " + kind);
            require(stringList(hostReport.get("failedFirstClassRegistryIds")).equals(List.of(probeId)),
                    "registry host top-level report must expose duplicate failed id for " + kind);
            Map<String, Object> coverage = host.registryMutationCoverage();
            require(stringList(coverage.get("failedFirstClassRegistryIds")).equals(List.of(probeId)),
                    "registry mutation coverage must expose duplicate failed id for " + kind);
        }
    }

    private static void assertStatusOnlyMutatedFailsReleaseGate() {
        EchoNativeRegistryHost host = new EchoNativeRegistryHost();
        host.attachLiveBridge(new FixedStatusBridge(EchoNativeLoadStatus.MUTATED));
        EchoNativeLoadStatus status = host.registerDeclared(
                "echoagent3",
                "item",
                "echoagent3:status_only_mutation_probe",
                Map.of()
        );
        require(status == EchoNativeLoadStatus.MUTATED,
                "status-only bridge still reports its raw MUTATED status");
        require(host.registeredOnlyFirstClassRegistryKinds().isEmpty(),
                "status-only MUTATED first-class item must not be reported as REGISTERED-only");
        require(host.untrustedMutationFirstClassRegistryKinds().equals(List.of("item")),
                "status-only MUTATED first-class item must be reported as an untrusted mutation");
        require(untrustedReasonCounts(host).equals(Map.of("mutation_record_missing", 1)),
                "status-only MUTATED first-class item kind report must aggregate missing-record reason");
        require(host.untrustedMutationReasonCounts().equals(Map.of("mutation_record_missing", 1)),
                "status-only MUTATED first-class item coverage must aggregate missing-record reason");
        require(reportReasonCounts(host).equals(Map.of("mutation_record_missing", 1)),
                "status-only MUTATED first-class item host report must expose missing-record reason");
        require(coverageReasonCounts(host).equals(Map.of("mutation_record_missing", 1)),
                "status-only MUTATED first-class item coverage report must expose missing-record reason");
        require(!host.allDeclaredRegistryKindsTrusted(),
                "status-only MUTATED first-class item must not satisfy trusted mutation coverage");
        require(host.trustedRegistryMutatedEntryCount() == 0,
                "status-only MUTATED first-class item must not be counted as trusted mutation");
        Map<String, Object> entryReport = host.items().get(0).toReport();
        require(Boolean.TRUE.equals(entryReport.get("liveRegistryMutationProofRejected")),
                "status-only MUTATED first-class item must report rejected mutation proof");
        require(stringList(entryReport.get("liveRegistryMutationProofRejectionReasons"))
                        .equals(List.of("mutation_record_missing")),
                "status-only MUTATED first-class item must report missing mutation record reason");
    }

    private static void assertMismatchedMutationProofFailsReleaseGate() {
        EchoNativeRegistryHost host = new EchoNativeRegistryHost();
        host.attachLiveBridge(new MismatchedProofBridge());
        EchoNativeLoadStatus status = host.registerDeclared(
                "echoagent3",
                "item",
                "echoagent3:mismatched_proof_probe",
                Map.of()
        );
        require(status == EchoNativeLoadStatus.MUTATED,
                "mismatched proof bridge still reports its raw MUTATED status");
        require(host.registeredOnlyFirstClassRegistryKinds().isEmpty(),
                "mismatched proof must not be reported as REGISTERED-only");
        require(host.untrustedMutationFirstClassRegistryKinds().equals(List.of("item")),
                "mismatched proof must be reported as untrusted mutation");
        require(untrustedReasonCounts(host).equals(Map.of("record_identity_mismatch", 1)),
                "mismatched proof kind report must aggregate identity-mismatch reason");
        require(host.untrustedMutationReasonCounts().equals(Map.of("record_identity_mismatch", 1)),
                "mismatched proof coverage must aggregate identity-mismatch reason");
        require(reportReasonCounts(host).equals(Map.of("record_identity_mismatch", 1)),
                "mismatched proof host report must expose identity-mismatch reason");
        require(coverageReasonCounts(host).equals(Map.of("record_identity_mismatch", 1)),
                "mismatched proof coverage report must expose identity-mismatch reason");
        require(!host.allDeclaredRegistryKindsTrusted(),
                "mismatched proof must not satisfy trusted mutation coverage");
        require(host.trustedRegistryMutatedEntryCount() == 0,
                "mismatched proof must not be counted as trusted mutation");
        Map<String, Object> entryReport = host.items().get(0).toReport();
        require(Boolean.TRUE.equals(entryReport.get("liveRegistryMutationRecordPresent")),
                "mismatched proof must remain visible for diagnostics");
        require(!Boolean.TRUE.equals(entryReport.get("liveRegistryMutationRecordIdentityMatched")),
                "mismatched proof must report failed mutation identity correlation");
        require(Boolean.TRUE.equals(entryReport.get("liveRegistryMutationRecordTrustMetadataPresent")),
                "mismatched proof must still report trust metadata when only identity is wrong");
        require(stringList(entryReport.get("liveRegistryMutationProofRejectionReasons"))
                        .equals(List.of("record_identity_mismatch")),
                "mismatched proof must report identity mismatch as its rejection reason");
        require(!Boolean.TRUE.equals(entryReport.get("nativeRegistryHostMutated")),
                "mismatched proof must not promote the entry to host-mutated");
    }

    private static void assertMissingTrustMetadataProofFailsReleaseGate() {
        EchoNativeRegistryHost host = new EchoNativeRegistryHost();
        host.attachLiveBridge(new MissingTrustMetadataBridge());
        EchoNativeLoadStatus status = host.registerDeclared(
                "echoagent3",
                "item",
                "echoagent3:missing_trust_metadata_probe",
                Map.of()
        );
        require(status == EchoNativeLoadStatus.MUTATED,
                "missing-trust-metadata bridge still reports its raw MUTATED status");
        require(host.registeredOnlyFirstClassRegistryKinds().isEmpty(),
                "missing trust metadata proof must not be reported as REGISTERED-only");
        require(host.untrustedMutationFirstClassRegistryKinds().equals(List.of("item")),
                "missing trust metadata proof must be reported as untrusted mutation");
        require(untrustedReasonCounts(host).equals(Map.of("record_trust_metadata_missing", 1)),
                "missing trust metadata proof kind report must aggregate trust-metadata reason");
        require(host.untrustedMutationReasonCounts().equals(Map.of("record_trust_metadata_missing", 1)),
                "missing trust metadata proof coverage must aggregate trust-metadata reason");
        require(reportReasonCounts(host).equals(Map.of("record_trust_metadata_missing", 1)),
                "missing trust metadata proof host report must expose trust-metadata reason");
        require(coverageReasonCounts(host).equals(Map.of("record_trust_metadata_missing", 1)),
                "missing trust metadata proof coverage report must expose trust-metadata reason");
        require(!host.allDeclaredRegistryKindsTrusted(),
                "missing trust metadata proof must not satisfy trusted mutation coverage");
        require(host.trustedRegistryMutatedEntryCount() == 0,
                "missing trust metadata proof must not be counted as trusted mutation");
        Map<String, Object> entryReport = host.items().get(0).toReport();
        require(Boolean.TRUE.equals(entryReport.get("liveRegistryMutationRecordPresent")),
                "missing trust metadata proof must remain visible for diagnostics");
        require(Boolean.TRUE.equals(entryReport.get("liveRegistryMutationRecordIdentityMatched")),
                "missing trust metadata proof must still report valid identity correlation");
        require(!Boolean.TRUE.equals(entryReport.get("liveRegistryMutationRecordTrustMetadataPresent")),
                "missing trust metadata proof must report absent trust tuple");
        require(stringList(entryReport.get("liveRegistryMutationProofRejectionReasons"))
                        .equals(List.of("record_trust_metadata_missing")),
                "missing trust metadata proof must report missing trust metadata as its rejection reason");
        require(!Boolean.TRUE.equals(entryReport.get("nativeRegistryHostMutated")),
                "missing trust metadata proof must not promote the entry to host-mutated");
    }

    private static void assertLifecycleOnlyMutationProofFailsReleaseGate() {
        EchoNativeRegistryHost host = new EchoNativeRegistryHost();
        host.attachLiveBridge(new LifecycleOnlyMutationProofBridge());
        EchoNativeLoadStatus status = host.registerDeclared(
                "echoagent3",
                "item",
                "echoagent3:lifecycle_only_mutation_probe",
                Map.of()
        );
        require(status == EchoNativeLoadStatus.MUTATED,
                "lifecycle-only bridge still reports its raw MUTATED status");
        require(host.registeredOnlyFirstClassRegistryKinds().isEmpty(),
                "lifecycle-only proof must not be reported as REGISTERED-only");
        require(host.untrustedMutationFirstClassRegistryKinds().equals(List.of("item")),
                "lifecycle-only proof must be reported as untrusted mutation");
        require(untrustedReasonCounts(host).equals(Map.of("native_registry_table_mutation_missing", 1)),
                "lifecycle-only proof kind report must require actual native registry-table mutation");
        require(host.untrustedMutationReasonCounts().equals(Map.of("native_registry_table_mutation_missing", 1)),
                "lifecycle-only proof coverage must require actual native registry-table mutation");
        require(!host.allDeclaredRegistryKindsTrusted(),
                "lifecycle-only proof must not satisfy trusted mutation coverage");
        require(host.trustedRegistryMutatedEntryCount() == 0,
                "lifecycle-only proof must not be counted as trusted mutation");
        Map<String, Object> entryReport = host.items().get(0).toReport();
        require(Boolean.TRUE.equals(entryReport.get("liveRegistryMutationApplied")),
                "lifecycle-only proof must keep lifecycle mutation evidence visible");
        require(!Boolean.TRUE.equals(entryReport.get("liveRegistryNativeTableMutationApplied")),
                "lifecycle-only proof must expose missing native registry-table mutation evidence");
        require(Boolean.TRUE.equals(entryReport.get("liveRegistryMutationRecordIdentityMatched")),
                "lifecycle-only proof must still report valid identity correlation");
        require(Boolean.TRUE.equals(entryReport.get("liveRegistryMutationRecordTrustMetadataPresent")),
                "lifecycle-only proof must still report complete trust metadata");
        require(stringList(entryReport.get("liveRegistryMutationProofRejectionReasons"))
                        .equals(List.of("native_registry_table_mutation_missing")),
                "lifecycle-only proof must report native registry table mutation as the missing release evidence");
        require(!Boolean.TRUE.equals(entryReport.get("nativeRegistryHostMutated")),
                "lifecycle-only proof must not promote the entry to host-mutated");
    }

    private static void assertBridgeEvidenceMismatchIsReported() {
        EchoNativeRegistryHost host = new EchoNativeRegistryHost();
        host.attachLiveBridge(new AggregateMismatchProofBridge());
        EchoNativeLoadStatus status = host.registerDeclared(
                "echoagent3",
                "item",
                "echoagent3:aggregate_mismatch_probe",
                Map.of()
        );
        require(status == EchoNativeLoadStatus.MUTATED,
                "aggregate mismatch bridge must still return MUTATED for per-entry proof-backed mutation");
        require(host.allDeclaredRegistryKindsTrusted(),
                "aggregate mismatch bridge must retain trusted per-entry mutation proof");
        require(host.trustedRegistryMutatedIds().equals(List.of("echoagent3:aggregate_mismatch_probe")),
                "trusted mutation ids must expose human-readable full ids");
        require(host.trustedRegistryMutationRecordIds().equals(List.of("item:echoagent3:aggregate_mismatch_probe")),
                "trusted mutation record ids must expose bridge aggregate key shape");
        Map<String, Object> reconciliation = host.registryBridgeMutationReconciliation();
        require(!Boolean.TRUE.equals(reconciliation.get("bridgeEvidenceMatchesTrustedEntries")),
                "registry bridge reconciliation must flag aggregate bridge evidence mismatch");
        require(stringList(reconciliation.get("missingFromBridgeEvidence"))
                        .equals(List.of("item:echoagent3:aggregate_mismatch_probe")),
                "registry bridge reconciliation must name trusted entries missing from aggregate evidence");
        require(stringList(reconciliation.get("bridgeEvidenceWithoutTrustedEntry"))
                        .equals(List.of("item:echoagent3:wrong_aggregate_id")),
                "registry bridge reconciliation must name aggregate bridge ids without trusted host entries");
        Map<String, Object> reportReconciliation =
                object(host.toReport().get("registryBridgeMutationReconciliation"));
        require(stringList(reportReconciliation.get("bridgeEvidenceWithoutTrustedEntry"))
                        .equals(List.of("item:echoagent3:wrong_aggregate_id")),
                "registry host report must expose aggregate bridge ids without trusted host entries");
        Map<String, Object> coverageReconciliation =
                object(host.registryMutationCoverage().get("registryBridgeMutationReconciliation"));
        require(stringList(coverageReconciliation.get("missingFromBridgeEvidence"))
                        .equals(List.of("item:echoagent3:aggregate_mismatch_probe")),
                "registry mutation coverage must expose trusted entries missing from bridge evidence");
    }

    private static void assertBridgeEvidenceCountAndMapMismatchIsReported() {
        EchoNativeRegistryHost host = new EchoNativeRegistryHost();
        host.attachLiveBridge(new AggregateCountAndMapMismatchProofBridge());
        EchoNativeLoadStatus status = host.registerDeclared(
                "echoagent3",
                "item",
                "echoagent3:aggregate_count_map_probe",
                Map.of()
        );
        require(status == EchoNativeLoadStatus.MUTATED,
                "aggregate count/map mismatch bridge must still return MUTATED for per-entry proof-backed mutation");
        require(host.allDeclaredRegistryKindsTrusted(),
                "aggregate count/map mismatch bridge must retain trusted per-entry mutation proof");
        Map<String, Object> reconciliation = host.registryBridgeMutationReconciliation();
        require(stringList(reconciliation.get("missingFromBridgeEvidence")).isEmpty(),
                "aggregate count/map mismatch fixture must keep aggregate ids aligned with trusted entries");
        require(stringList(reconciliation.get("bridgeEvidenceWithoutTrustedEntry")).isEmpty(),
                "aggregate count/map mismatch fixture must avoid stale aggregate ids");
        require(!Boolean.TRUE.equals(reconciliation.get("bridgeEvidenceCountMatchesTrustedEntries")),
                "registry bridge reconciliation must fail when aggregate count/map shape does not match trusted entries");
        require(!Boolean.TRUE.equals(reconciliation.get("bridgeEvidenceMatchesTrustedEntries")),
                "registry bridge reconciliation must reject correct ids with stale aggregate count/map evidence");
        require(((Number) reconciliation.get("bridgeMutatedRecordCount")).intValue() == 0,
                "registry bridge reconciliation must expose stale aggregate mutatedRecordCount");
        require(stringList(reconciliation.get("trustedRecordsMissingFromBridgeRecordMap"))
                        .equals(List.of("item:echoagent3:aggregate_count_map_probe")),
                "registry bridge reconciliation must name trusted entries missing from aggregate mutatedRecords map");
        Map<String, Object> reportReconciliation =
                object(host.toReport().get("registryBridgeMutationReconciliation"));
        require(!Boolean.TRUE.equals(reportReconciliation.get("bridgeEvidenceMatchesTrustedEntries")),
                "registry host report must reject count/map-only aggregate bridge evidence drift");
        Map<String, Object> coverageReconciliation =
                object(host.registryMutationCoverage().get("registryBridgeMutationReconciliation"));
        require(!Boolean.TRUE.equals(coverageReconciliation.get("bridgeEvidenceCountMatchesTrustedEntries")),
                "registry mutation coverage must expose aggregate count/map mismatch");
    }

    private static void assertBridgeEvidenceRecordMapPayloadMismatchIsReported() {
        EchoNativeRegistryHost host = new EchoNativeRegistryHost();
        host.attachLiveBridge(new AggregateRecordMapPayloadMismatchProofBridge());
        EchoNativeLoadStatus status = host.registerDeclared(
                "echoagent3",
                "item",
                "echoagent3:aggregate_record_payload_probe",
                Map.of()
        );
        require(status == EchoNativeLoadStatus.MUTATED,
                "aggregate record-payload mismatch bridge must still return MUTATED for per-entry proof-backed mutation");
        require(host.allDeclaredRegistryKindsTrusted(),
                "aggregate record-payload mismatch bridge must retain trusted per-entry mutation proof");
        Map<String, Object> reconciliation = host.registryBridgeMutationReconciliation();
        require(Boolean.TRUE.equals(reconciliation.get("bridgeEvidenceCountMatchesTrustedEntries")),
                "aggregate record-payload mismatch fixture must keep aggregate id/count/key shape aligned");
        require(!Boolean.TRUE.equals(reconciliation.get("bridgeRecordMapProofMatchesTrustedEntries")),
                "registry bridge reconciliation must fail when aggregate mutatedRecords payloads do not match trusted entries");
        require(!Boolean.TRUE.equals(reconciliation.get("bridgeEvidenceMatchesTrustedEntries")),
                "registry bridge reconciliation must reject correct keys with stale aggregate record payloads");
        require(stringList(reconciliation.get("bridgeRecordMapProofMismatches"))
                        .equals(List.of("item:echoagent3:aggregate_record_payload_probe")),
                "registry bridge reconciliation must name aggregate mutatedRecords payload mismatches");
        Map<String, Object> reportReconciliation =
                object(host.toReport().get("registryBridgeMutationReconciliation"));
        require(!Boolean.TRUE.equals(reportReconciliation.get("bridgeRecordMapProofMatchesTrustedEntries")),
                "registry host report must expose aggregate record-payload mismatch");
        Map<String, Object> coverageReconciliation =
                object(host.registryMutationCoverage().get("registryBridgeMutationReconciliation"));
        require(stringList(coverageReconciliation.get("bridgeRecordMapProofMismatches"))
                        .equals(List.of("item:echoagent3:aggregate_record_payload_probe")),
                "registry mutation coverage must expose aggregate record-payload mismatch ids");
    }

    private static void assertRegistryHostCanonicalizesDescriptorShapedIds() {
        NativeLoaderDefaultProductBridgeProvider provider = new NativeLoaderDefaultProductBridgeProvider();
        NativeLoaderLiveRegistryBridge bridge = provider.liveRegistryBridge(new NativeLoaderProductBridgeContext(
                "ashfall",
                "echoagent3_host_full_id",
                Path.of("."),
                Path.of("."),
                Map.of()
        ));
        EchoNativeRegistryHost host = new EchoNativeRegistryHost();
        host.attachLiveBridge(bridge);
        EchoNativeLoadStatus status = host.registerDeclared(
                "echoagent3",
                "item",
                "echoagent3:host_direct_namespaced_item_probe",
                Map.of("idShape", "descriptor_full_id")
        );
        require(status == EchoNativeLoadStatus.MUTATED,
                "registry host must trust live mutation for descriptor-shaped namespaced ids");
        require(host.registeredOnlyFirstClassRegistryKinds().isEmpty(),
                "registry host must not report canonical descriptor-shaped ids as registered-only");
        require(host.untrustedMutationFirstClassRegistryKinds().isEmpty(),
                "registry host must not reject canonical descriptor-shaped id mutation proof");
        require(host.trustedRegistryMutationRecordIds().equals(List.of(
                        "item:echoagent3:host_direct_namespaced_item_probe"
                )),
                "registry host must expose canonical mutation record id for descriptor-shaped ids");
        EchoNativeRegistryHost.RegistryEntry entry = host.items().get(0);
        require("echoagent3:host_direct_namespaced_item_probe".equals(entry.fullId()),
                "registry host entry full id must not duplicate namespace for descriptor-shaped ids");
        Map<String, Object> entryReport = entry.toReport();
        require(Boolean.TRUE.equals(entryReport.get("liveRegistryMutationRecordIdentityMatched")),
                "registry host must compare mutation proof against canonical descriptor-shaped id identity");
        require(Boolean.TRUE.equals(entryReport.get("liveRegistryMutationProofAccepted")),
                "registry host must accept canonical descriptor-shaped id mutation proof");
    }

    private static void assertMutatedFirstClassKindsPassReleaseGate() {
        EchoNativeRegistryHost host = new EchoNativeRegistryHost();
        host.attachLiveBridge(new ProofBackedMutatingBridge());
        for (String kind : FIRST_CLASS_KINDS) {
            EchoNativeLoadStatus status = host.registerDeclared(
                    "echoagent3",
                    kind,
                    "echoagent3:" + kind + "_probe",
                    Map.of()
            );
            require(status == EchoNativeLoadStatus.MUTATED,
                    "mutating bridge must return MUTATED for " + kind + ", got " + status);
        }
        require(host.registeredOnlyFirstClassRegistryKinds().isEmpty(),
                "mutated first-class kinds must not leave registered-only blockers");
        require(host.registeredOnlyFirstClassRegistryIds().isEmpty(),
                "mutated first-class kinds must not leave registered-only id blockers");
        require(host.registeredOnlyFirstClassRegistryIdsByKind().isEmpty(),
                "mutated first-class kinds must not leave registered-only id blockers by kind");
        require(host.untrustedMutationFirstClassRegistryKinds().isEmpty(),
                "mutated first-class kinds must not leave untrusted mutation blockers");
        require(host.allDeclaredRegistryKindsTrusted(),
                "mutated first-class kinds must satisfy trusted mutation coverage");
        require(host.trustedRegistryMutatedEntryCount() == FIRST_CLASS_KINDS.size(),
                "every first-class declaration must be counted as trusted mutation");
        require(stringList(object(host.toReport().get("registryMutationCoverage")).get("requiredFirstClassKinds"))
                        .equals(FIRST_CLASS_KINDS),
                "registry host report must expose the authoritative first-class registry kind list");
    }

    private static void assertDefaultProductBridgeMutatesEveryFirstClassKind() {
        NativeLoaderDefaultProductBridgeProvider provider = new NativeLoaderDefaultProductBridgeProvider();
        NativeLoaderLiveRegistryBridge bridge = provider.liveRegistryBridge(new NativeLoaderProductBridgeContext(
                "ashfall",
                "echoagent3",
                Path.of("."),
                Path.of("."),
                Map.of()
        ));
        EchoNativeRegistryHost host = new EchoNativeRegistryHost();
        host.attachLiveBridge(bridge);
        for (String kind : FIRST_CLASS_KINDS) {
            EchoNativeLoadStatus status = host.registerDeclared(
                    "echoagent3",
                    kind,
                    "echoagent3:default_" + kind + "_probe",
                    Map.of()
            );
            require(status == EchoNativeLoadStatus.MUTATED,
                    "default product bridge must mutate first-class registry kind " + kind + ", got " + status);
        }
        NativeLoaderLiveRegistryBridge aliasBridge = provider.liveRegistryBridge(new NativeLoaderProductBridgeContext(
                "ashfall",
                "echoagent3_alias",
                Path.of("."),
                Path.of("."),
                Map.of()
        ));
        Map<String, String> aliases = Map.of(
                "creative_tabs", "creative_tab",
                "data_components", "data_component",
                "world_generators", "worldgen",
                "client_assets", "client_asset"
        );
        for (Map.Entry<String, String> alias : aliases.entrySet()) {
            String id = "default_alias_" + alias.getValue() + "_probe";
            EchoNativeLoadStatus status = aliasBridge.register(
                    alias.getKey(),
                    "echoagent3",
                    id,
                    "native://agent3/default-alias/" + alias.getKey(),
                    Map.of("alias", alias.getKey())
            );
            require(status == EchoNativeLoadStatus.MUTATED,
                    "default product bridge must mutate first-class alias " + alias.getKey()
                            + ", got " + status);
            Map<String, Object> record = aliasBridge.registryMutationRecord(alias.getValue(), "echoagent3", id);
            require(alias.getValue().equals(record.get("registry")),
                    "default product bridge alias mutation record must canonicalize " + alias.getKey());
            require(("echoagent3:" + id).equals(record.get("fullId")),
                    "default product bridge alias mutation record must preserve full id for " + alias.getKey());
        }
        String directNamespacedId = "echoagent3:direct_namespaced_item_probe";
        EchoNativeLoadStatus directNamespacedStatus = aliasBridge.register(
                "item",
                "echoagent3",
                directNamespacedId,
                "native://agent3/default-direct-namespaced/item",
                Map.of("idShape", "descriptor_full_id")
        );
        require(directNamespacedStatus == EchoNativeLoadStatus.MUTATED,
                "default product bridge must mutate direct namespaced descriptor-shaped ids");
        Map<String, Object> localLookupRecord = aliasBridge.registryMutationRecord(
                "item",
                "echoagent3",
                "direct_namespaced_item_probe"
        );
        Map<String, Object> fullLookupRecord = aliasBridge.registryMutationRecord(
                "item",
                "echoagent3",
                directNamespacedId
        );
        require("echoagent3:direct_namespaced_item_probe".equals(localLookupRecord.get("fullId")),
                "default product bridge must canonicalize direct namespaced ids to one namespace:id full id");
        require(localLookupRecord.equals(fullLookupRecord),
                "default product bridge mutation lookup must accept both local and namespaced id forms");
        require(((Number) aliasBridge.registryEvidence().get("mutatedRecordCount")).intValue() == aliases.size() + 1,
                "default product bridge alias evidence must count canonical alias and direct namespaced mutations");
        require(stringList(aliasBridge.registryEvidence().get("mutatedRecordIds"))
                        .contains("item:echoagent3:direct_namespaced_item_probe"),
                "default product bridge aggregate evidence must expose canonical direct namespaced mutation id");
        require(host.registeredOnlyFirstClassRegistryKinds().isEmpty(),
                "default product bridge must not leave first-class registry declarations registered-only");
        require(host.registeredOnlyFirstClassRegistryIds().isEmpty(),
                "default product bridge must not leave registered-only id blockers");
        require(host.registeredOnlyFirstClassRegistryIdsByKind().isEmpty(),
                "default product bridge must not leave registered-only id blockers by kind");
        require(untrustedReasonCounts(host).isEmpty(),
                "default product bridge must not report untrusted mutation reason rollups");
        require(host.untrustedMutationReasonCounts().isEmpty(),
                "default product bridge coverage must not report untrusted mutation reason rollups");
        require(reportReasonCounts(host).isEmpty(),
                "default product bridge host report must not expose untrusted mutation reason rollups");
        require(coverageReasonCounts(host).isEmpty(),
                "default product bridge coverage report must not expose untrusted mutation reason rollups");
        Map<String, Object> bridgeEvidence = bridge.registryEvidence();
        require(Boolean.TRUE.equals(bridgeEvidence.get("productNativeRegistryTableMutated")),
                "default product bridge must mutate an inspectable product native registry table");
        require(((Number) bridgeEvidence.get("mutatedRecordCount")).intValue() == FIRST_CLASS_KINDS.size(),
                "default product bridge host path must expose one product registry mutation per first-class kind");
        require(stringList(bridgeEvidence.get("mutatedRegistryKinds")).containsAll(FIRST_CLASS_KINDS),
                "default product bridge evidence must include every first-class registry kind");
        Map<String, Object> hostReport = host.toReport();
        Map<String, Object> hostBridgeEvidence = object(hostReport.get("liveRegistryBridgeEvidence"));
        require(Boolean.TRUE.equals(hostBridgeEvidence.get("productNativeRegistryTableMutated")),
                "registry host report must expose product native registry table mutation evidence");
        require(((Number) hostBridgeEvidence.get("mutatedRecordCount")).intValue() == FIRST_CLASS_KINDS.size(),
                "registry host report must expose every bridge mutation record");
        List<String> expectedMutationRecordIds = FIRST_CLASS_KINDS.stream()
                .map(kind -> kind + ":echoagent3:default_" + kind + "_probe")
                .sorted()
                .toList();
        require(host.trustedRegistryMutationRecordIds().equals(expectedMutationRecordIds),
                "registry host must expose trusted mutation record ids in bridge aggregate key shape");
        Map<String, Object> reconciliation = host.registryBridgeMutationReconciliation();
        require(Boolean.TRUE.equals(reconciliation.get("bridgeEvidenceMatchesTrustedEntries")),
                "default product bridge aggregate evidence must reconcile with trusted host entries");
        require(stringList(reconciliation.get("trustedRegistryMutationRecordIds")).equals(expectedMutationRecordIds),
                "default product bridge reconciliation must expose trusted mutation record ids");
        require(stringList(reconciliation.get("bridgeMutatedRecordIds")).equals(expectedMutationRecordIds),
                "default product bridge reconciliation must expose matching bridge mutation record ids");
        require(stringList(reconciliation.get("missingFromBridgeEvidence")).isEmpty(),
                "default product bridge reconciliation must not miss trusted entries from bridge evidence");
        require(stringList(reconciliation.get("bridgeEvidenceWithoutTrustedEntry")).isEmpty(),
                "default product bridge reconciliation must not report aggregate ids without trusted entries");
        Map<String, Object> coverageReconciliation =
                object(host.registryMutationCoverage().get("registryBridgeMutationReconciliation"));
        require(Boolean.TRUE.equals(coverageReconciliation.get("bridgeEvidenceMatchesTrustedEntries")),
                "registry mutation coverage must expose successful bridge/host reconciliation");
        List<EchoNativeRegistryHost.RegistryEntry> entries = allEntries(host);
        require(entries.size() == FIRST_CLASS_KINDS.size(),
                "registry host must expose one entry per first-class default bridge mutation");
        for (EchoNativeRegistryHost.RegistryEntry entry : entries) {
            Map<String, Object> entryReport = entry.toReport();
            require(Boolean.TRUE.equals(entryReport.get("liveRegistryMutationRecordPresent")),
                    "mutated entry must expose a correlated product mutation record for " + entry.fullId());
            Map<String, Object> mutationRecord = object(entryReport.get("liveRegistryMutationRecord"));
            require(EchoNativeLoadStatus.MUTATED.name().equals(mutationRecord.get("status")),
                    "entry mutation record must prove MUTATED status for " + entry.fullId());
            require(entry.kind().equals(mutationRecord.get("registry")),
                    "entry mutation record must match registry kind for " + entry.fullId());
            require(entry.fullId().equals(mutationRecord.get("fullId")),
                    "entry mutation record must match full id for " + entry.fullId());
            require(Boolean.TRUE.equals(mutationRecord.get("productNativeRegistryTableMutated")),
                    "entry mutation record must prove product registry table mutation for " + entry.fullId());
            require(Boolean.TRUE.equals(entryReport.get("liveRegistryMutationRecordIdentityMatched")),
                    "trusted entry report must expose matching mutation proof identity for " + entry.fullId());
            require(Boolean.TRUE.equals(entryReport.get("liveRegistryMutationRecordTrustMetadataPresent")),
                    "trusted entry report must expose complete mutation proof trust metadata for " + entry.fullId());
            require(Boolean.TRUE.equals(entryReport.get("liveRegistryMutationProofAccepted")),
                    "trusted entry report must mark mutation proof accepted for " + entry.fullId());
            require(stringList(entryReport.get("liveRegistryMutationProofRejectionReasons")).isEmpty(),
                    "trusted entry report must have no mutation proof rejection reasons for " + entry.fullId());
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }

    private static List<String> stringList(Object value) {
        if (!(value instanceof Iterable<?> iterable)) {
            return List.of();
        }
        java.util.ArrayList<String> values = new java.util.ArrayList<>();
        for (Object item : iterable) {
            values.add(String.valueOf(item));
        }
        return List.copyOf(values);
    }

    private static Map<String, Object> object(Object value) {
        if (!(value instanceof Map<?, ?> map)) {
            return Map.of();
        }
        java.util.LinkedHashMap<String, Object> object = new java.util.LinkedHashMap<>();
        map.forEach((key, item) -> object.put(String.valueOf(key), item));
        return Map.copyOf(object);
    }

    private static Map<String, List<String>> stringListMap(Object value) {
        if (!(value instanceof Map<?, ?> map)) {
            return Map.of();
        }
        java.util.LinkedHashMap<String, List<String>> values = new java.util.LinkedHashMap<>();
        map.forEach((key, item) -> values.put(String.valueOf(key), stringList(item)));
        return Map.copyOf(values);
    }

    private static Map<String, Integer> untrustedReasonCounts(EchoNativeRegistryHost host) {
        Map<String, Object> reports = host.registryKindReports();
        Map<String, Object> itemReport = object(reports.get("item"));
        Map<String, Object> counts = object(itemReport.get("untrustedMutationReasonCounts"));
        return integerMap(counts);
    }

    private static Map<String, Integer> coverageReasonCounts(EchoNativeRegistryHost host) {
        Map<String, Object> coverage = host.registryMutationCoverage();
        Map<String, Object> counts = object(coverage.get("untrustedMutationReasonCounts"));
        return integerMap(counts);
    }

    private static Map<String, Integer> reportReasonCounts(EchoNativeRegistryHost host) {
        Map<String, Object> report = host.toReport();
        Map<String, Object> counts = object(report.get("untrustedMutationReasonCounts"));
        return integerMap(counts);
    }

    private static Map<String, Integer> integerMap(Map<String, Object> counts) {
        java.util.LinkedHashMap<String, Integer> result = new java.util.LinkedHashMap<>();
        counts.forEach((reason, count) -> {
            if (count instanceof Number number) {
                result.put(reason, number.intValue());
            }
        });
        return Map.copyOf(result);
    }

    private static List<EchoNativeRegistryHost.RegistryEntry> allEntries(EchoNativeRegistryHost host) {
        java.util.ArrayList<EchoNativeRegistryHost.RegistryEntry> entries = new java.util.ArrayList<>();
        entries.addAll(host.items());
        entries.addAll(host.blocks());
        entries.addAll(host.entities());
        entries.addAll(host.blockEntities());
        entries.addAll(host.menus());
        entries.addAll(host.sounds());
        entries.addAll(host.particles());
        entries.addAll(host.effects());
        entries.addAll(host.commands());
        entries.addAll(host.dataComponents());
        entries.addAll(host.recipes());
        entries.addAll(host.creativeTabs());
        entries.addAll(host.biomes());
        entries.addAll(host.worldgen());
        entries.addAll(host.clientAssets());
        return List.copyOf(entries);
    }

    private record FixedStatusBridge(EchoNativeLoadStatus status) implements NativeLoaderLiveRegistryBridge {
        @Override
        public boolean attached() {
            return true;
        }

        @Override
        public String bridgeId() {
            return "echoagent3:fixed_status_" + status.name().toLowerCase();
        }

        @Override
        public boolean firstClassNativeRegistry() {
            return true;
        }

        @Override
        public EchoNativeLoadStatus register(
                String registry,
                String namespace,
                String id,
                String implementationClass,
                Map<String, Object> properties
        ) {
            return FIRST_CLASS_KINDS.contains(registry) ? status : EchoNativeLoadStatus.UNSUPPORTED;
        }
    }

    private static final class MismatchedProofBridge implements NativeLoaderLiveRegistryBridge {
        private final java.util.Map<String, Map<String, Object>> records = new java.util.LinkedHashMap<>();

        @Override
        public boolean attached() {
            return true;
        }

        @Override
        public String bridgeId() {
            return "echoagent3:mismatched_proof_bridge";
        }

        @Override
        public boolean firstClassNativeRegistry() {
            return true;
        }

        @Override
        public Map<String, Object> registryMutationRecord(String registry, String namespace, String id) {
            Map<String, Object> record = records.get(key(registry, namespace, id));
            return record == null ? Map.of() : record;
        }

        @Override
        public EchoNativeLoadStatus register(
                String registry,
                String namespace,
                String id,
                String implementationClass,
                Map<String, Object> properties
        ) {
            if (!FIRST_CLASS_KINDS.contains(registry)) {
                return EchoNativeLoadStatus.UNSUPPORTED;
            }
            records.put(key(registry, namespace, id), Map.ofEntries(
                    Map.entry("registry", registry),
                    Map.entry("namespace", "echoagent3_wrong_namespace"),
                    Map.entry("id", id),
                    Map.entry("fullId", namespace + ":" + id),
                    Map.entry("status", EchoNativeLoadStatus.MUTATED.name()),
                    Map.entry("bridgeId", "echoagent3:wrong_bridge"),
                    Map.entry("liveRegistryMutationApplied", true),
                    Map.entry("nativeRegistryTableMutated", true),
                    Map.entry("firstClassNativeRegistry", true),
                    Map.entry("nativeRegistryProcess", true),
                    Map.entry("releaseRegistryTrusted", true),
                    Map.entry("nativeRegistryMutationSupported", true)
            ));
            return EchoNativeLoadStatus.MUTATED;
        }

        private static String key(String registry, String namespace, String id) {
            return registry + ":" + namespace + ":" + id;
        }
    }

    private static final class MissingTrustMetadataBridge implements NativeLoaderLiveRegistryBridge {
        private final java.util.Map<String, Map<String, Object>> records = new java.util.LinkedHashMap<>();

        @Override
        public boolean attached() {
            return true;
        }

        @Override
        public String bridgeId() {
            return "echoagent3:missing_trust_metadata_bridge";
        }

        @Override
        public boolean firstClassNativeRegistry() {
            return true;
        }

        @Override
        public Map<String, Object> registryMutationRecord(String registry, String namespace, String id) {
            Map<String, Object> record = records.get(key(registry, namespace, id));
            return record == null ? Map.of() : record;
        }

        @Override
        public EchoNativeLoadStatus register(
                String registry,
                String namespace,
                String id,
                String implementationClass,
                Map<String, Object> properties
        ) {
            if (!FIRST_CLASS_KINDS.contains(registry)) {
                return EchoNativeLoadStatus.UNSUPPORTED;
            }
            records.put(key(registry, namespace, id), Map.of(
                    "registry", registry,
                    "namespace", namespace,
                    "id", id,
                    "fullId", namespace + ":" + id,
                    "status", EchoNativeLoadStatus.MUTATED.name(),
                    "bridgeId", bridgeId(),
                    "liveRegistryMutationApplied", true,
                    "nativeRegistryTableMutated", true
            ));
            return EchoNativeLoadStatus.MUTATED;
        }

        private static String key(String registry, String namespace, String id) {
            return registry + ":" + namespace + ":" + id;
        }
    }

    private static final class AggregateMismatchProofBridge implements NativeLoaderLiveRegistryBridge {
        private final java.util.Map<String, Map<String, Object>> records = new java.util.LinkedHashMap<>();

        @Override
        public boolean attached() {
            return true;
        }

        @Override
        public String bridgeId() {
            return "echoagent3:aggregate_mismatch_proof_bridge";
        }

        @Override
        public boolean firstClassNativeRegistry() {
            return true;
        }

        @Override
        public Map<String, Object> registryEvidence() {
            return Map.of(
                    "bridgeId", bridgeId(),
                    "attached", true,
                    "firstClassNativeRegistry", true,
                    "nativeRegistryProcess", true,
                    "releaseRegistryTrusted", true,
                    "nativeRegistryMutationSupported", true,
                    "productNativeRegistryTableMutated", true,
                    "mutatedRecordCount", 1,
                    "mutatedRecordIds", List.of("item:echoagent3:wrong_aggregate_id"),
                    "mutatedRecords", Map.copyOf(records)
            );
        }

        @Override
        public Map<String, Object> registryMutationRecord(String registry, String namespace, String id) {
            Map<String, Object> record = records.get(key(registry, namespace, id));
            return record == null ? Map.of() : record;
        }

        @Override
        public EchoNativeLoadStatus register(
                String registry,
                String namespace,
                String id,
                String implementationClass,
                Map<String, Object> properties
        ) {
            if (!FIRST_CLASS_KINDS.contains(registry)) {
                return EchoNativeLoadStatus.UNSUPPORTED;
            }
            String fullId = namespace + ":" + id;
            records.put(key(registry, namespace, id), Map.ofEntries(
                    Map.entry("registry", registry),
                    Map.entry("namespace", namespace),
                    Map.entry("id", id),
                    Map.entry("fullId", fullId),
                    Map.entry("status", EchoNativeLoadStatus.MUTATED.name()),
                    Map.entry("bridgeId", bridgeId()),
                    Map.entry("liveRegistryMutationApplied", true),
                    Map.entry("nativeRegistryTableMutated", true),
                    Map.entry("firstClassNativeRegistry", true),
                    Map.entry("nativeRegistryProcess", true),
                    Map.entry("releaseRegistryTrusted", true),
                    Map.entry("nativeRegistryMutationSupported", true)
            ));
            return EchoNativeLoadStatus.MUTATED;
        }

        private static String key(String registry, String namespace, String id) {
            return registry + ":" + namespace + ":" + id;
        }
    }

    private static final class AggregateCountAndMapMismatchProofBridge implements NativeLoaderLiveRegistryBridge {
        private final java.util.Map<String, Map<String, Object>> records = new java.util.LinkedHashMap<>();

        @Override
        public boolean attached() {
            return true;
        }

        @Override
        public String bridgeId() {
            return "echoagent3:aggregate_count_map_mismatch_proof_bridge";
        }

        @Override
        public boolean firstClassNativeRegistry() {
            return true;
        }

        @Override
        public Map<String, Object> registryEvidence() {
            return Map.of(
                    "bridgeId", bridgeId(),
                    "attached", true,
                    "firstClassNativeRegistry", true,
                    "nativeRegistryProcess", true,
                    "releaseRegistryTrusted", true,
                    "nativeRegistryMutationSupported", true,
                    "productNativeRegistryTableMutated", true,
                    "mutatedRecordCount", 0,
                    "mutatedRecordIds", records.keySet().stream().sorted().toList(),
                    "mutatedRecords", Map.of()
            );
        }

        @Override
        public Map<String, Object> registryMutationRecord(String registry, String namespace, String id) {
            Map<String, Object> record = records.get(key(registry, namespace, id));
            return record == null ? Map.of() : record;
        }

        @Override
        public EchoNativeLoadStatus register(
                String registry,
                String namespace,
                String id,
                String implementationClass,
                Map<String, Object> properties
        ) {
            if (!FIRST_CLASS_KINDS.contains(registry)) {
                return EchoNativeLoadStatus.UNSUPPORTED;
            }
            String fullId = namespace + ":" + id;
            records.put(key(registry, namespace, id), Map.ofEntries(
                    Map.entry("registry", registry),
                    Map.entry("namespace", namespace),
                    Map.entry("id", id),
                    Map.entry("fullId", fullId),
                    Map.entry("status", EchoNativeLoadStatus.MUTATED.name()),
                    Map.entry("bridgeId", bridgeId()),
                    Map.entry("liveRegistryMutationApplied", true),
                    Map.entry("nativeRegistryTableMutated", true),
                    Map.entry("firstClassNativeRegistry", true),
                    Map.entry("nativeRegistryProcess", true),
                    Map.entry("releaseRegistryTrusted", true),
                    Map.entry("nativeRegistryMutationSupported", true)
            ));
            return EchoNativeLoadStatus.MUTATED;
        }

        private static String key(String registry, String namespace, String id) {
            return registry + ":" + namespace + ":" + id;
        }
    }

    private static final class AggregateRecordMapPayloadMismatchProofBridge implements NativeLoaderLiveRegistryBridge {
        private final java.util.Map<String, Map<String, Object>> records = new java.util.LinkedHashMap<>();
        private final java.util.Map<String, Map<String, Object>> aggregateRecords = new java.util.LinkedHashMap<>();

        @Override
        public boolean attached() {
            return true;
        }

        @Override
        public String bridgeId() {
            return "echoagent3:aggregate_record_payload_mismatch_proof_bridge";
        }

        @Override
        public boolean firstClassNativeRegistry() {
            return true;
        }

        @Override
        public Map<String, Object> registryEvidence() {
            return Map.of(
                    "bridgeId", bridgeId(),
                    "attached", true,
                    "firstClassNativeRegistry", true,
                    "nativeRegistryProcess", true,
                    "releaseRegistryTrusted", true,
                    "nativeRegistryMutationSupported", true,
                    "productNativeRegistryTableMutated", true,
                    "mutatedRecordCount", aggregateRecords.size(),
                    "mutatedRecordIds", aggregateRecords.keySet().stream().sorted().toList(),
                    "mutatedRecords", Map.copyOf(aggregateRecords)
            );
        }

        @Override
        public Map<String, Object> registryMutationRecord(String registry, String namespace, String id) {
            Map<String, Object> record = records.get(key(registry, namespace, id));
            return record == null ? Map.of() : record;
        }

        @Override
        public EchoNativeLoadStatus register(
                String registry,
                String namespace,
                String id,
                String implementationClass,
                Map<String, Object> properties
        ) {
            if (!FIRST_CLASS_KINDS.contains(registry)) {
                return EchoNativeLoadStatus.UNSUPPORTED;
            }
            String key = key(registry, namespace, id);
            String fullId = namespace + ":" + id;
            records.put(key, Map.ofEntries(
                    Map.entry("registry", registry),
                    Map.entry("namespace", namespace),
                    Map.entry("id", id),
                    Map.entry("fullId", fullId),
                    Map.entry("status", EchoNativeLoadStatus.MUTATED.name()),
                    Map.entry("bridgeId", bridgeId()),
                    Map.entry("liveRegistryMutationApplied", true),
                    Map.entry("nativeRegistryTableMutated", true),
                    Map.entry("firstClassNativeRegistry", true),
                    Map.entry("nativeRegistryProcess", true),
                    Map.entry("releaseRegistryTrusted", true),
                    Map.entry("nativeRegistryMutationSupported", true)
            ));
            aggregateRecords.put(key, Map.ofEntries(
                    Map.entry("registry", registry),
                    Map.entry("namespace", namespace),
                    Map.entry("id", "stale_" + id),
                    Map.entry("fullId", namespace + ":stale_" + id),
                    Map.entry("status", EchoNativeLoadStatus.MUTATED.name()),
                    Map.entry("bridgeId", bridgeId()),
                    Map.entry("liveRegistryMutationApplied", true),
                    Map.entry("nativeRegistryTableMutated", true),
                    Map.entry("firstClassNativeRegistry", true),
                    Map.entry("nativeRegistryProcess", true),
                    Map.entry("releaseRegistryTrusted", true),
                    Map.entry("nativeRegistryMutationSupported", true)
            ));
            return EchoNativeLoadStatus.MUTATED;
        }

        private static String key(String registry, String namespace, String id) {
            return registry + ":" + namespace + ":" + id;
        }
    }

    private static final class LifecycleOnlyMutationProofBridge implements NativeLoaderLiveRegistryBridge {
        private final java.util.Map<String, Map<String, Object>> records = new java.util.LinkedHashMap<>();

        @Override
        public boolean attached() {
            return true;
        }

        @Override
        public String bridgeId() {
            return "echoagent3:lifecycle_only_mutation_proof_bridge";
        }

        @Override
        public boolean firstClassNativeRegistry() {
            return true;
        }

        @Override
        public Map<String, Object> registryMutationRecord(String registry, String namespace, String id) {
            Map<String, Object> record = records.get(key(registry, namespace, id));
            return record == null ? Map.of() : record;
        }

        @Override
        public EchoNativeLoadStatus register(
                String registry,
                String namespace,
                String id,
                String implementationClass,
                Map<String, Object> properties
        ) {
            if (!FIRST_CLASS_KINDS.contains(registry)) {
                return EchoNativeLoadStatus.UNSUPPORTED;
            }
            String fullId = namespace + ":" + id;
            records.put(key(registry, namespace, id), Map.ofEntries(
                    Map.entry("registry", registry),
                    Map.entry("namespace", namespace),
                    Map.entry("id", id),
                    Map.entry("fullId", fullId),
                    Map.entry("status", EchoNativeLoadStatus.MUTATED.name()),
                    Map.entry("bridgeId", bridgeId()),
                    Map.entry("liveRegistryMutationApplied", true),
                    Map.entry("firstClassNativeRegistry", true),
                    Map.entry("nativeRegistryProcess", true),
                    Map.entry("releaseRegistryTrusted", true),
                    Map.entry("nativeRegistryMutationSupported", true)
            ));
            return EchoNativeLoadStatus.MUTATED;
        }

        private static String key(String registry, String namespace, String id) {
            return registry + ":" + namespace + ":" + id;
        }
    }

    private static final class ProofBackedMutatingBridge implements NativeLoaderLiveRegistryBridge {
        private final java.util.Map<String, Map<String, Object>> records = new java.util.LinkedHashMap<>();

        @Override
        public boolean attached() {
            return true;
        }

        @Override
        public String bridgeId() {
            return "echoagent3:proof_backed_mutating_bridge";
        }

        @Override
        public boolean firstClassNativeRegistry() {
            return true;
        }

        @Override
        public Map<String, Object> registryMutationRecord(String registry, String namespace, String id) {
            Map<String, Object> record = records.get(key(registry, namespace, id));
            return record == null ? Map.of() : record;
        }

        @Override
        public EchoNativeLoadStatus register(
                String registry,
                String namespace,
                String id,
                String implementationClass,
                Map<String, Object> properties
        ) {
            if (!FIRST_CLASS_KINDS.contains(registry)) {
                return EchoNativeLoadStatus.UNSUPPORTED;
            }
            String key = key(registry, namespace, id);
            String fullId = namespace + ":" + id;
            records.put(key, Map.ofEntries(
                    Map.entry("registry", registry),
                    Map.entry("namespace", namespace),
                    Map.entry("id", id),
                    Map.entry("fullId", fullId),
                    Map.entry("status", EchoNativeLoadStatus.MUTATED.name()),
                    Map.entry("bridgeId", bridgeId()),
                    Map.entry("liveRegistryMutationApplied", true),
                    Map.entry("nativeRegistryTableMutated", true),
                    Map.entry("firstClassNativeRegistry", true),
                    Map.entry("nativeRegistryProcess", true),
                    Map.entry("releaseRegistryTrusted", true),
                    Map.entry("nativeRegistryMutationSupported", true)
            ));
            return EchoNativeLoadStatus.MUTATED;
        }

        private static String key(String registry, String namespace, String id) {
            return registry + ":" + namespace + ":" + id;
        }
    }
}
