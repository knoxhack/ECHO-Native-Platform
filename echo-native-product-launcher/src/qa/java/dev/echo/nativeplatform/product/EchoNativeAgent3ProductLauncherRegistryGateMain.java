package dev.echo.nativeplatform.product;

import dev.echo.nativeplatform.contracts.EchoNativeDiagnostic;
import dev.echo.nativeplatform.contracts.EchoNativeAddonDescriptor;
import dev.echo.nativeplatform.contracts.EchoNativeApiStability;
import dev.echo.nativeplatform.contracts.EchoNativeLoadStatus;
import dev.echo.nativeplatform.contracts.EchoNativeRegisteredService;
import dev.echo.nativeplatform.contracts.EchoNativeRuntimeSide;
import dev.echo.nativeplatform.contracts.EchoNativeTrustLevel;
import dev.echo.nativeplatform.loader.EchoNativeRegistryHost;
import dev.echo.nativeplatform.loader.NativeLoaderLiveRegistryBridge;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoNativeAgent3ProductLauncherRegistryGateMain {
    private static final List<String> FIRST_CLASS_KINDS = EchoNativeRegistryHost.firstClassRegistryKinds();

    private EchoNativeAgent3ProductLauncherRegistryGateMain() {
    }

    public static void main(String[] args) throws Exception {
        assertDescriptorCreativeTabRegistrationIsDeclarationDriven();
        assertNativeRegistryServiceAdvertisesFirstClassSurfaces();
        List<String> registeredOnlyIds = probeIds("registered_only");
        Map<String, List<String>> registeredOnlyIdsByKind = probeIdsByKind("registered_only");
        EchoNativeProductLauncher.EchoNativeProductRuntimeCapabilityReport registeredOnlyCapabilities = capabilities(
                FIRST_CLASS_KINDS.size(),
                0,
                0,
                0,
                FIRST_CLASS_KINDS,
                registeredOnlyIds,
                registeredOnlyIdsByKind,
                List.of()
        );
        List<EchoNativeDiagnostic> registeredOnlyDiagnostics = releaseDiagnostics(registeredOnlyCapabilities);
        require(hasDiagnostic(registeredOnlyDiagnostics, "ECHO-NATIVE-RELEASE-REGISTRY-REGISTERED-ONLY"),
                "REGISTERED-only first-class registry kinds must emit registered-only registry diagnostic");
        require(registeredOnlyCapabilities.registeredOnlyFirstClassRegistryIds()
                        .equals(registeredOnlyIds),
                "REGISTERED-only first-class registry capabilities must expose concrete registered-only ids");
        require(registeredOnlyCapabilities.registeredOnlyFirstClassRegistryIdsByKind()
                        .equals(registeredOnlyIdsByKind),
                "REGISTERED-only first-class registry capabilities must expose concrete ids by kind");
        EchoNativeDiagnostic registeredOnlyDiagnostic = diagnostic(
                registeredOnlyDiagnostics,
                "ECHO-NATIVE-RELEASE-REGISTRY-REGISTERED-ONLY"
        );
        for (String registeredOnlyId : registeredOnlyIds) {
            require(registeredOnlyDiagnostic.summary().contains(registeredOnlyId),
                    "REGISTERED-only diagnostic must name concrete registered-only id " + registeredOnlyId);
        }
        require(hasDiagnostic(registeredOnlyDiagnostics, "ECHO-NATIVE-RELEASE-REGISTRY-DECLARATIONS-NOT-MUTATED"),
                "REGISTERED-only first-class registry kinds must fail trusted mutation coverage");
        require(!registeredOnlyCapabilities.fullReleaseRuntimeReady(),
                "REGISTERED-only first-class registry kinds must fail full release readiness");
        require(!capabilities(
                FIRST_CLASS_KINDS.size(),
                FIRST_CLASS_KINDS.size(),
                FIRST_CLASS_KINDS.size(),
                FIRST_CLASS_KINDS.size(),
                FIRST_CLASS_KINDS,
                registeredOnlyIds,
                registeredOnlyIdsByKind,
                List.of()
        ).fullReleaseRuntimeReady(),
                "REGISTERED-only blocker list must fail full release readiness even when mutation counts look complete");
        EchoNativeProductLauncher.EchoNativeProductRuntimeCapabilityReport detailOnlyRegisteredOnlyCapabilities =
                capabilities(
                        FIRST_CLASS_KINDS.size(),
                        FIRST_CLASS_KINDS.size(),
                        FIRST_CLASS_KINDS.size(),
                        FIRST_CLASS_KINDS.size(),
                        List.of(),
                        List.of("echoagent3:detail_only_registered_probe"),
                        Map.of("item", List.of("echoagent3:detail_only_registered_probe")),
                        List.of()
                );
        List<EchoNativeDiagnostic> detailOnlyRegisteredOnlyDiagnostics =
                releaseDiagnostics(detailOnlyRegisteredOnlyCapabilities);
        require(hasDiagnostic(
                        detailOnlyRegisteredOnlyDiagnostics,
                        "ECHO-NATIVE-RELEASE-REGISTRY-REGISTERED-ONLY"
                ),
                "detail-only registered registry blocker ids must still emit registered-only diagnostic");
        require(diagnostic(
                        detailOnlyRegisteredOnlyDiagnostics,
                        "ECHO-NATIVE-RELEASE-REGISTRY-REGISTERED-ONLY"
                ).summary().contains("echoagent3:detail_only_registered_probe"),
                "detail-only registered registry diagnostic must name concrete blocker id");
        require(!detailOnlyRegisteredOnlyCapabilities.fullReleaseRuntimeReady(),
                "detail-only registered registry blocker ids must fail full release readiness");
        EchoNativeDiagnostic genericMutationDiagnostic = diagnostic(
                registeredOnlyDiagnostics,
                "ECHO-NATIVE-RELEASE-REGISTRY-DECLARATIONS-NOT-MUTATED"
        );
        require(genericMutationDiagnostic.suggestedFix().contains("recipe")
                        && genericMutationDiagnostic.suggestedFix().contains("biome")
                        && genericMutationDiagnostic.suggestedFix().contains("worldgen")
                        && genericMutationDiagnostic.suggestedFix().contains("client asset"),
                "generic registry mutation diagnostic must name every expanded first-class registry family");

        EchoNativeProductLauncher.EchoNativeProductRuntimeCapabilityReport failedCapabilities = capabilitiesWithFailures(
                0,
                0,
                0,
                0,
                List.of("item"),
                List.of("echoagent3:failed_item_probe"),
                Map.of("item", List.of("echoagent3:failed_item_probe"))
        );
        List<EchoNativeDiagnostic> failedDiagnostics = releaseDiagnostics(failedCapabilities);
        require(hasDiagnostic(failedDiagnostics, "ECHO-NATIVE-RELEASE-REGISTRY-FAILED"),
                "failed first-class registry declaration must emit failed registry diagnostic");
        EchoNativeDiagnostic failedDiagnostic = diagnostic(
                failedDiagnostics,
                "ECHO-NATIVE-RELEASE-REGISTRY-FAILED"
        );
        require(failedDiagnostic.summary().contains("echoagent3:failed_item_probe"),
                "failed registry diagnostic must name concrete failed first-class id");
        require(!failedCapabilities.fullReleaseRuntimeReady(),
                "failed first-class registry declarations must fail full release readiness even with no registered entries");
        EchoNativeProductLauncher.EchoNativeProductRuntimeCapabilityReport detailOnlyFailedCapabilities =
                capabilitiesWithFailures(
                        0,
                        0,
                        0,
                        0,
                        List.of(),
                        List.of("echoagent3:detail_only_failed_probe"),
                        Map.of("item", List.of("echoagent3:detail_only_failed_probe"))
                );
        List<EchoNativeDiagnostic> detailOnlyFailedDiagnostics =
                releaseDiagnostics(detailOnlyFailedCapabilities);
        require(hasDiagnostic(
                        detailOnlyFailedDiagnostics,
                        "ECHO-NATIVE-RELEASE-REGISTRY-FAILED"
                ),
                "detail-only failed registry blocker ids must still emit failed registry diagnostic");
        require(diagnostic(
                        detailOnlyFailedDiagnostics,
                        "ECHO-NATIVE-RELEASE-REGISTRY-FAILED"
                ).summary().contains("echoagent3:detail_only_failed_probe"),
                "detail-only failed registry diagnostic must name concrete blocker id");
        require(!detailOnlyFailedCapabilities.fullReleaseRuntimeReady(),
                "detail-only failed registry blocker ids must fail full release readiness");

        EchoNativeProductLauncher.EchoNativeProductRuntimeCapabilityReport untrustedCapabilities = capabilities(
                1,
                1,
                0,
                0,
                List.of(),
                List.of("item"),
                Map.of("mutation_record_missing", 1),
                Map.of("item", Map.of("mutation_record_missing", 1))
        );
        List<EchoNativeDiagnostic> untrustedDiagnostics = releaseDiagnostics(untrustedCapabilities);
        require(!hasDiagnostic(untrustedDiagnostics, "ECHO-NATIVE-RELEASE-REGISTRY-REGISTERED-ONLY"),
                "status-only registry mutation must not be confused with REGISTERED-only evidence");
        require(hasDiagnostic(untrustedDiagnostics, "ECHO-NATIVE-RELEASE-REGISTRY-MUTATION-UNTRUSTED"),
                "status-only registry mutation must emit untrusted registry mutation diagnostic");
        EchoNativeDiagnostic untrustedDiagnostic = diagnostic(
                untrustedDiagnostics,
                "ECHO-NATIVE-RELEASE-REGISTRY-MUTATION-UNTRUSTED"
        );
        require(untrustedDiagnostic.summary().contains("mutation_record_missing"),
                "untrusted registry mutation diagnostic must expose mutation proof rejection reasons");
        require(untrustedCapabilities.untrustedMutationReasonCounts().equals(Map.of("mutation_record_missing", 1)),
                "runtime capability report must carry top-level untrusted mutation reason counts");
        require(untrustedCapabilities.untrustedMutationReasonCountsByKind().equals(
                        Map.of("item", Map.of("mutation_record_missing", 1))),
                "runtime capability report must carry per-kind untrusted mutation reason counts");
        require(hasDiagnostic(untrustedDiagnostics, "ECHO-NATIVE-RELEASE-REGISTRY-DECLARATIONS-NOT-MUTATED"),
                "status-only registry mutation must also fail trusted mutation coverage");
        require(!untrustedCapabilities.fullReleaseRuntimeReady(),
                "status-only registry mutation must fail full release readiness");
        require(!capabilities(
                1,
                1,
                1,
                1,
                List.of(),
                List.of("item")
        ).fullReleaseRuntimeReady(),
                "untrusted mutation blocker list must fail full release readiness even when mutation counts look complete");
        EchoNativeProductLauncher.EchoNativeProductRuntimeCapabilityReport detailOnlyUntrustedCapabilities =
                capabilities(
                        1,
                        1,
                        1,
                        1,
                        List.of(),
                        List.of(),
                        Map.of("mutation_record_missing", 1),
                        Map.of("item", Map.of("mutation_record_missing", 1))
                );
        List<EchoNativeDiagnostic> detailOnlyUntrustedDiagnostics =
                releaseDiagnostics(detailOnlyUntrustedCapabilities);
        require(hasDiagnostic(
                        detailOnlyUntrustedDiagnostics,
                        "ECHO-NATIVE-RELEASE-REGISTRY-MUTATION-UNTRUSTED"
                ),
                "detail-only untrusted registry reason counts must still emit untrusted mutation diagnostic");
        require(diagnostic(
                        detailOnlyUntrustedDiagnostics,
                        "ECHO-NATIVE-RELEASE-REGISTRY-MUTATION-UNTRUSTED"
                ).summary().contains("mutation_record_missing"),
                "detail-only untrusted registry diagnostic must name concrete rejection reason");
        require(!detailOnlyUntrustedCapabilities.fullReleaseRuntimeReady(),
                "detail-only untrusted registry reason counts must fail full release readiness");

        EchoNativeProductLauncher.EchoNativeProductRuntimeCapabilityReport aggregateMismatchCapabilities = capabilities(
                1,
                1,
                1,
                1,
                List.of(),
                List.of(),
                Map.of(),
                List.of(),
                List.of(),
                Map.of(),
                List.of(),
                Map.of(),
                Map.of(),
                Map.of(
                        "bridgeEvidenceMatchesTrustedEntries", false,
                        "missingFromBridgeEvidence", List.of("item:echoagent3:aggregate_mismatch_probe"),
                        "bridgeEvidenceWithoutTrustedEntry", List.of("item:echoagent3:wrong_aggregate_id")
                )
        );
        List<EchoNativeDiagnostic> aggregateMismatchDiagnostics = releaseDiagnostics(aggregateMismatchCapabilities);
        require(hasDiagnostic(aggregateMismatchDiagnostics,
                        "ECHO-NATIVE-RELEASE-REGISTRY-BRIDGE-EVIDENCE-MISMATCH"),
                "bridge aggregate evidence mismatch must emit registry bridge evidence diagnostic");
        EchoNativeDiagnostic aggregateMismatchDiagnostic = diagnostic(
                aggregateMismatchDiagnostics,
                "ECHO-NATIVE-RELEASE-REGISTRY-BRIDGE-EVIDENCE-MISMATCH"
        );
        require(aggregateMismatchDiagnostic.summary().contains("item:echoagent3:wrong_aggregate_id"),
                "bridge aggregate evidence mismatch diagnostic must name stale aggregate ids");
        require(!aggregateMismatchCapabilities.fullReleaseRuntimeReady(),
                "bridge aggregate evidence mismatch must fail full release readiness");

        EchoNativeProductLauncher.EchoNativeProductRuntimeCapabilityReport zeroEntryAggregateMismatchCapabilities = capabilities(
                0,
                0,
                0,
                0,
                List.of(),
                List.of(),
                Map.of(),
                List.of(),
                List.of(),
                Map.of(),
                List.of(),
                Map.of(),
                Map.of(),
                Map.of(
                        "bridgeEvidenceMatchesTrustedEntries", false,
                        "missingFromBridgeEvidence", List.of(),
                        "bridgeEvidenceWithoutTrustedEntry", List.of("item:echoagent3:stale_zero_entry_aggregate_id")
                )
        );
        require(hasDiagnostic(
                        releaseDiagnostics(zeroEntryAggregateMismatchCapabilities),
                        "ECHO-NATIVE-RELEASE-REGISTRY-BRIDGE-EVIDENCE-MISMATCH"
                ),
                "zero-entry stale bridge aggregate evidence must still emit registry bridge evidence diagnostic");
        require(!zeroEntryAggregateMismatchCapabilities.fullReleaseRuntimeReady(),
                "zero-entry stale bridge aggregate evidence must fail full release readiness");

        EchoNativeProductLauncher.EchoNativeProductRuntimeCapabilityReport countMapAggregateMismatchCapabilities = capabilities(
                1,
                1,
                1,
                1,
                List.of(),
                List.of(),
                Map.of(),
                List.of(),
                List.of(),
                Map.of(),
                List.of(),
                Map.of(),
                Map.of(),
                Map.of(
                        "bridgeEvidenceMatchesTrustedEntries", false,
                        "bridgeEvidenceCountMatchesTrustedEntries", false,
                        "missingFromBridgeEvidence", List.of(),
                        "bridgeEvidenceWithoutTrustedEntry", List.of(),
                        "trustedRecordsMissingFromBridgeRecordMap",
                        List.of("item:echoagent3:aggregate_count_map_probe"),
                        "bridgeRecordMapWithoutTrustedEntry", List.of()
                )
        );
        EchoNativeDiagnostic countMapAggregateMismatchDiagnostic = diagnostic(
                releaseDiagnostics(countMapAggregateMismatchCapabilities),
                "ECHO-NATIVE-RELEASE-REGISTRY-BRIDGE-EVIDENCE-MISMATCH"
        );
        require(countMapAggregateMismatchDiagnostic.summary().contains("aggregate_count_map_probe"),
                "bridge aggregate count/map mismatch diagnostic must name trusted records missing from mutatedRecords");
        require(countMapAggregateMismatchDiagnostic.suggestedFix().contains("mutatedRecordCount")
                        && countMapAggregateMismatchDiagnostic.suggestedFix().contains("mutatedRecords"),
                "bridge aggregate count/map mismatch diagnostic must mention count and record-map evidence");
        require(!countMapAggregateMismatchCapabilities.fullReleaseRuntimeReady(),
                "matching aggregate ids with stale count/map evidence must fail full release readiness");

        EchoNativeProductLauncher.EchoNativeProductRuntimeCapabilityReport payloadAggregateMismatchCapabilities = capabilities(
                1,
                1,
                1,
                1,
                List.of(),
                List.of(),
                Map.of(),
                List.of(),
                List.of(),
                Map.of(),
                List.of(),
                Map.of(),
                Map.of(),
                Map.of(
                        "bridgeEvidenceMatchesTrustedEntries", false,
                        "bridgeEvidenceCountMatchesTrustedEntries", true,
                        "bridgeRecordMapProofMatchesTrustedEntries", false,
                        "missingFromBridgeEvidence", List.of(),
                        "bridgeEvidenceWithoutTrustedEntry", List.of(),
                        "trustedRecordsMissingFromBridgeRecordMap", List.of(),
                        "bridgeRecordMapWithoutTrustedEntry", List.of(),
                        "bridgeRecordMapProofMismatches",
                        List.of("item:echoagent3:aggregate_record_payload_probe")
                )
        );
        EchoNativeDiagnostic payloadAggregateMismatchDiagnostic = diagnostic(
                releaseDiagnostics(payloadAggregateMismatchCapabilities),
                "ECHO-NATIVE-RELEASE-REGISTRY-BRIDGE-EVIDENCE-MISMATCH"
        );
        require(payloadAggregateMismatchDiagnostic.summary().contains("aggregate_record_payload_probe"),
                "bridge aggregate payload mismatch diagnostic must name stale mutatedRecords payload ids");
        require(!payloadAggregateMismatchCapabilities.fullReleaseRuntimeReady(),
                "matching aggregate ids/count/map keys with stale record payloads must fail full release readiness");

        EchoNativeProductLauncher.EchoNativeProductRuntimeCapabilityReport trustedCapabilities = capabilities(
                1,
                1,
                1,
                1,
                List.of(),
                List.of()
        );
        List<EchoNativeDiagnostic> trustedDiagnostics = releaseDiagnostics(trustedCapabilities);
        require(!hasDiagnostic(trustedDiagnostics, "ECHO-NATIVE-RELEASE-REGISTRY-REGISTERED-ONLY"),
                "proof-backed registry mutation must not emit registered-only registry diagnostic");
        require(!hasDiagnostic(trustedDiagnostics, "ECHO-NATIVE-RELEASE-REGISTRY-MUTATION-UNTRUSTED"),
                "proof-backed registry mutation must not emit untrusted registry mutation diagnostic");
        require(!hasDiagnostic(trustedDiagnostics, "ECHO-NATIVE-RELEASE-REGISTRY-DECLARATIONS-NOT-MUTATED"),
                "proof-backed registry mutation must satisfy trusted mutation coverage");
        require(trustedCapabilities.fullReleaseRuntimeReady(),
                "proof-backed registry mutation must allow full release readiness when all other synthetic capability checks pass: "
                        + readinessDebug(trustedCapabilities));
        assertRuntimeCapabilityReportCarriesActualRegistryReasonCounts();
        assertRuntimeCapabilityReportCarriesActualRegisteredOnlyIds(EchoNativeLoadStatus.DISCOVERED);
        assertRuntimeCapabilityReportCarriesActualRegisteredOnlyIds(EchoNativeLoadStatus.LOADED);
        assertRuntimeCapabilityReportCarriesActualRegisteredOnlyIds(EchoNativeLoadStatus.REGISTERED);
        assertRuntimeCapabilityReportCarriesActualRegisteredOnlyIds(EchoNativeLoadStatus.RESOLVED);
        assertRuntimeCapabilityReportCarriesActualRegisteredOnlyIds(EchoNativeLoadStatus.UNSUPPORTED);
        assertRuntimeCapabilityReportCarriesActualFailedRegistryIds();
        assertRuntimeCapabilityReportCarriesActualBridgeReconciliationMismatch();

        System.out.println("agent3 product launcher registry gate PASS");
    }

    private static void assertNativeRegistryServiceAdvertisesFirstClassSurfaces() throws Exception {
        Method servicesMethod = EchoNativeProductLauncher.class.getDeclaredMethod(
                "registerNativeHostServices",
                java.nio.file.Path.class,
                String.class,
                EchoNativeProductLauncher.EchoNativeProductLaunchOptions.class
        );
        servicesMethod.setAccessible(true);
        Object services = servicesMethod.invoke(null, java.nio.file.Path.of("."), "qa-agent3", null);
        Method serviceRegistryMethod = services.getClass().getDeclaredMethod("serviceRegistry");
        serviceRegistryMethod.setAccessible(true);
        dev.echo.nativeplatform.contracts.EchoNativeServiceRegistry serviceRegistry =
                (dev.echo.nativeplatform.contracts.EchoNativeServiceRegistry) serviceRegistryMethod.invoke(services);
        EchoNativeRegisteredService registryService = serviceRegistry.registeredServices().stream()
                .filter(service -> EchoNativeRegistryHost.SERVICE_ID.equals(service.serviceId()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("native registry host service descriptor missing"));
        for (String kind : FIRST_CLASS_KINDS) {
            require(registryService.surfaces().contains(kind),
                    "native registry host service must advertise first-class registry surface " + kind);
            String alias = serviceSurfaceAlias(kind);
            require(registryService.surfaces().contains(alias),
                    "native registry host service must advertise descriptor alias surface " + alias
                            + " for first-class registry kind " + kind);
        }
        require(registryService.surfaces().contains("registry"),
                "native registry host service must advertise the generic registry surface");
        require(registryService.surfaces().contains("worldgens"),
                "native registry host service must advertise legacy worldgens alias");
    }

    private static void assertRuntimeCapabilityReportCarriesActualBridgeReconciliationMismatch() throws Exception {
        EchoNativeProductLauncher.EchoNativeProductLaunchOptions options =
                new EchoNativeProductLauncher.EchoNativeProductLaunchOptions(
                        false,
                        false,
                        false,
                        null,
                        null,
                        new AggregateMismatchRegistryBridge(),
                        null,
                        null,
                        null
                );
        Method servicesMethod = EchoNativeProductLauncher.class.getDeclaredMethod(
                "registerNativeHostServices",
                java.nio.file.Path.class,
                String.class,
                EchoNativeProductLauncher.EchoNativeProductLaunchOptions.class
        );
        servicesMethod.setAccessible(true);
        Object services = servicesMethod.invoke(null, java.nio.file.Path.of("."), "qa-agent3", options);
        Method registryHostMethod = services.getClass().getDeclaredMethod("registryHost");
        registryHostMethod.setAccessible(true);
        EchoNativeRegistryHost registryHost = (EchoNativeRegistryHost) registryHostMethod.invoke(services);
        registryHost.registerDeclared(
                "echoagent3",
                "item",
                "echoagent3:actual_runtime_capability_aggregate_mismatch_probe",
                Map.of()
        );

        Method capabilityMethod = EchoNativeProductLauncher.class.getDeclaredMethod(
                "runtimeCapabilityReport",
                services.getClass()
        );
        capabilityMethod.setAccessible(true);
        EchoNativeProductLauncher.EchoNativeProductRuntimeCapabilityReport capabilities =
                (EchoNativeProductLauncher.EchoNativeProductRuntimeCapabilityReport)
                        capabilityMethod.invoke(null, services);
        require(!capabilities.registryBridgeMutationEvidenceReconciled(),
                "actual launcher runtime capability report must expose registry bridge reconciliation mismatch");
        require(capabilities.registryBridgeMutationReconciliationList("missingFromBridgeEvidence").equals(
                        List.of("item:echoagent3:actual_runtime_capability_aggregate_mismatch_probe")),
                "actual launcher runtime capability report must expose trusted entries missing from bridge evidence");
        require(capabilities.registryBridgeMutationReconciliationList("bridgeEvidenceWithoutTrustedEntry").equals(
                        List.of("item:echoagent3:actual_runtime_capability_wrong_aggregate_id")),
                "actual launcher runtime capability report must expose stale bridge aggregate ids");
        List<EchoNativeDiagnostic> diagnostics = releaseDiagnostics(capabilities);
        require(hasDiagnostic(diagnostics, "ECHO-NATIVE-RELEASE-REGISTRY-BRIDGE-EVIDENCE-MISMATCH"),
                "actual launcher runtime capability mismatch must emit registry bridge evidence diagnostic");
        require(diagnostic(diagnostics, "ECHO-NATIVE-RELEASE-REGISTRY-BRIDGE-EVIDENCE-MISMATCH")
                        .summary().contains("actual_runtime_capability_wrong_aggregate_id"),
                "actual launcher registry bridge evidence diagnostic must name stale aggregate ids");
    }

    private static void assertRuntimeCapabilityReportCarriesActualRegisteredOnlyIds(
            EchoNativeLoadStatus status
    ) throws Exception {
        EchoNativeProductLauncher.EchoNativeProductLaunchOptions options =
                new EchoNativeProductLauncher.EchoNativeProductLaunchOptions(
                        false,
                        false,
                        false,
                        null,
                        null,
                        new FixedStatusBridge(status),
                        null,
                        null,
                        null
                );
        Method servicesMethod = EchoNativeProductLauncher.class.getDeclaredMethod(
                "registerNativeHostServices",
                java.nio.file.Path.class,
                String.class,
                EchoNativeProductLauncher.EchoNativeProductLaunchOptions.class
        );
        servicesMethod.setAccessible(true);
        Object services = servicesMethod.invoke(null, java.nio.file.Path.of("."), "qa-agent3", options);
        Method registryHostMethod = services.getClass().getDeclaredMethod("registryHost");
        registryHostMethod.setAccessible(true);
        EchoNativeRegistryHost registryHost = (EchoNativeRegistryHost) registryHostMethod.invoke(services);
        String probePrefix = "actual_runtime_capability_" + status.name().toLowerCase() + "_only";
        List<String> expectedIds = probeIds(probePrefix);
        Map<String, List<String>> expectedIdsByKind = probeIdsByKind(probePrefix);
        for (String kind : FIRST_CLASS_KINDS) {
            registryHost.registerDeclared(
                    "echoagent3",
                    kind,
                    "echoagent3:" + probePrefix + "_" + kind + "_probe",
                    Map.of()
            );
        }

        Method capabilityMethod = EchoNativeProductLauncher.class.getDeclaredMethod(
                "runtimeCapabilityReport",
                services.getClass()
        );
        capabilityMethod.setAccessible(true);
        EchoNativeProductLauncher.EchoNativeProductRuntimeCapabilityReport capabilities =
                (EchoNativeProductLauncher.EchoNativeProductRuntimeCapabilityReport)
                        capabilityMethod.invoke(null, services);
        require(sameStringSet(capabilities.registeredOnlyFirstClassRegistryKinds(), FIRST_CLASS_KINDS),
                "actual launcher runtime capability report must carry every registered-only registry kind for "
                        + status.name() + ": " + capabilities.registeredOnlyFirstClassRegistryKinds());
        require(sameStringSet(capabilities.registeredOnlyFirstClassRegistryIds(), expectedIds),
                "actual launcher runtime capability report must carry every registered-only registry id for "
                        + status.name() + ": " + capabilities.registeredOnlyFirstClassRegistryIds());
        require(capabilities.registeredOnlyFirstClassRegistryIdsByKind().equals(
                        expectedIdsByKind),
                "actual launcher runtime capability report must carry every registered-only registry id by kind for "
                        + status.name());
        List<EchoNativeDiagnostic> diagnostics = releaseDiagnostics(capabilities);
        require(hasDiagnostic(diagnostics, "ECHO-NATIVE-RELEASE-REGISTRY-REGISTERED-ONLY"),
                "actual launcher " + status.name()
                        + " first-class registry declaration must emit registered-only registry diagnostic");
        EchoNativeDiagnostic diagnostic = diagnostic(diagnostics, "ECHO-NATIVE-RELEASE-REGISTRY-REGISTERED-ONLY");
        for (String expectedId : expectedIds) {
            require(diagnostic.summary().contains(expectedId),
                    "actual launcher registered-only diagnostic must name " + status.name()
                            + " probe id " + expectedId);
        }
        require(diagnostic.suggestedFix().contains("REGISTERED, RESOLVED, LOADED, DISCOVERED, or UNSUPPORTED"),
                "actual launcher registered-only diagnostic must explain non-mutating live statuses are insufficient");
        require(!capabilities.fullReleaseRuntimeReady(),
                "actual launcher " + status.name()
                        + " first-class registry declaration must fail full release readiness");
    }

    private static void assertRuntimeCapabilityReportCarriesActualFailedRegistryIds() throws Exception {
        EchoNativeProductLauncher.EchoNativeProductLaunchOptions options =
                new EchoNativeProductLauncher.EchoNativeProductLaunchOptions(
                        false,
                        false,
                        false,
                        null,
                        null,
                        new FixedStatusBridge(EchoNativeLoadStatus.FAILED),
                        null,
                        null,
                        null
                );
        Method servicesMethod = EchoNativeProductLauncher.class.getDeclaredMethod(
                "registerNativeHostServices",
                java.nio.file.Path.class,
                String.class,
                EchoNativeProductLauncher.EchoNativeProductLaunchOptions.class
        );
        servicesMethod.setAccessible(true);
        Object services = servicesMethod.invoke(null, java.nio.file.Path.of("."), "qa-agent3", options);
        Method registryHostMethod = services.getClass().getDeclaredMethod("registryHost");
        registryHostMethod.setAccessible(true);
        EchoNativeRegistryHost registryHost = (EchoNativeRegistryHost) registryHostMethod.invoke(services);
        List<String> expectedIds = probeIds("actual_runtime_capability_failed");
        Map<String, List<String>> expectedIdsByKind = probeIdsByKind("actual_runtime_capability_failed");
        for (String kind : FIRST_CLASS_KINDS) {
            registryHost.registerDeclared(
                    "echoagent3",
                    kind,
                    "echoagent3:actual_runtime_capability_failed_" + kind + "_probe",
                    Map.of()
            );
        }

        Method capabilityMethod = EchoNativeProductLauncher.class.getDeclaredMethod(
                "runtimeCapabilityReport",
                services.getClass()
        );
        capabilityMethod.setAccessible(true);
        EchoNativeProductLauncher.EchoNativeProductRuntimeCapabilityReport capabilities =
                (EchoNativeProductLauncher.EchoNativeProductRuntimeCapabilityReport)
                        capabilityMethod.invoke(null, services);
        require(sameStringSet(capabilities.failedFirstClassRegistryKinds(), FIRST_CLASS_KINDS),
                "actual launcher runtime capability report must carry every failed registry kind: "
                        + capabilities.failedFirstClassRegistryKinds());
        require(sameStringSet(capabilities.failedFirstClassRegistryIds(), expectedIds),
                "actual launcher runtime capability report must carry every failed registry id: "
                        + capabilities.failedFirstClassRegistryIds());
        require(capabilities.failedFirstClassRegistryIdsByKind().equals(expectedIdsByKind),
                "actual launcher runtime capability report must carry every failed registry id by kind");
        require(capabilities.registeredOnlyFirstClassRegistryKinds().isEmpty(),
                "actual launcher failed registry ids must not be mislabeled as registered-only blockers");
        require(capabilities.untrustedMutationFirstClassRegistryKinds().isEmpty(),
                "actual launcher failed registry ids must not be mislabeled as untrusted mutations");
        List<EchoNativeDiagnostic> diagnostics = releaseDiagnostics(capabilities);
        require(hasDiagnostic(diagnostics, "ECHO-NATIVE-RELEASE-REGISTRY-FAILED"),
                "actual launcher FAILED first-class registry declaration must emit failed registry diagnostic");
        EchoNativeDiagnostic diagnostic = diagnostic(diagnostics, "ECHO-NATIVE-RELEASE-REGISTRY-FAILED");
        for (String expectedId : expectedIds) {
            require(diagnostic.summary().contains(expectedId),
                    "actual launcher failed registry diagnostic must name failed probe id " + expectedId);
        }
        require(!capabilities.fullReleaseRuntimeReady(),
                "actual launcher FAILED first-class registry declarations must fail full release readiness");
    }

    private static void assertRuntimeCapabilityReportCarriesActualRegistryReasonCounts() throws Exception {
        EchoNativeProductLauncher.EchoNativeProductLaunchOptions options =
                new EchoNativeProductLauncher.EchoNativeProductLaunchOptions(
                        false,
                        false,
                        false,
                        null,
                        null,
                        new FixedStatusBridge(EchoNativeLoadStatus.MUTATED),
                        null,
                        null,
                        null
                );
        Method servicesMethod = EchoNativeProductLauncher.class.getDeclaredMethod(
                "registerNativeHostServices",
                java.nio.file.Path.class,
                String.class,
                EchoNativeProductLauncher.EchoNativeProductLaunchOptions.class
        );
        servicesMethod.setAccessible(true);
        Object services = servicesMethod.invoke(null, java.nio.file.Path.of("."), "qa-agent3", options);
        Method registryHostMethod = services.getClass().getDeclaredMethod("registryHost");
        registryHostMethod.setAccessible(true);
        EchoNativeRegistryHost registryHost = (EchoNativeRegistryHost) registryHostMethod.invoke(services);
        registryHost.registerDeclared(
                "echoagent3",
                "item",
                "echoagent3:actual_runtime_capability_reason_probe",
                Map.of()
        );

        Method capabilityMethod = EchoNativeProductLauncher.class.getDeclaredMethod(
                "runtimeCapabilityReport",
                services.getClass()
        );
        capabilityMethod.setAccessible(true);
        EchoNativeProductLauncher.EchoNativeProductRuntimeCapabilityReport capabilities =
                (EchoNativeProductLauncher.EchoNativeProductRuntimeCapabilityReport)
                        capabilityMethod.invoke(null, services);
        require(capabilities.untrustedMutationReasonCounts().equals(Map.of("mutation_record_missing", 1)),
                "actual launcher runtime capability report must carry registry-host untrusted reason counts");
        require(capabilities.untrustedMutationReasonCountsByKind().equals(
                        Map.of("item", Map.of("mutation_record_missing", 1))),
                "actual launcher runtime capability report must carry per-kind registry-host reason counts");
    }

    private static void assertDescriptorCreativeTabRegistrationIsDeclarationDriven() throws Exception {
        EchoNativeRegistryHost noDeclarationHost = new EchoNativeRegistryHost();
        noDeclarationHost.attachLiveBridge(new ProofBackedRegistryBridge());
        registerDescriptorCreativeTabIntent(noDeclarationHost, descriptorWithAccess(Map.of(
                "nativeEntrypoint", "com.knoxhack.echoashfallprotocol.EchoAshfallNativeModule"
        )));
        require(noDeclarationHost.creativeTabs().isEmpty(),
                "descriptor creative-tab intent must not synthesize a fallback tab without an explicit SDK declaration");

        EchoNativeRegistryHost declaredHost = new EchoNativeRegistryHost();
        declaredHost.attachLiveBridge(new ProofBackedRegistryBridge());
        registerDescriptorCreativeTabIntent(declaredHost, descriptorWithAccess(Map.of(
                "nativeEntrypoint", "com.knoxhack.echoashfallprotocol.EchoAshfallNativeModule",
                "nativeCreativeTabs", List.of(Map.of(
                        "id", "echoashfallprotocol:native_modules_tab",
                        "titleKey", "itemGroup.EchoAshfallNativeModules",
                        "iconItem", "echoashfallprotocol:portable_signal_scanner",
                        "itemIds", List.of("echoashfallprotocol:portable_signal_scanner"),
                        "surfaceIds", List.of("terminal", "index"),
                        "orderAnchor", "minecraft:building_blocks"
                ))
        )));
        EchoNativeRegistryHost.RegistryEntry nativeModulesTab =
                declaredHost.creativeTab("echoashfallprotocol:native_modules_tab");
        require(nativeModulesTab != null,
                "descriptor-declared Ashfall native modules tab must register under its SDK tab id");
        require(declaredHost.creativeTab("echoashfallprotocol:module") == null,
                "descriptor-declared creative tab registration must not create the old generic module fallback tab");
        require(nativeModulesTab.nativeRegistryHostMutated(),
                "descriptor-declared creative tab must be proof-backed live registry mutation");
        require(Boolean.TRUE.equals(nativeModulesTab.properties().get("descriptorDeclaredNativeCreativeTab")),
                "descriptor-declared creative tab evidence must identify descriptor SDK declaration source");
        require(Boolean.FALSE.equals(nativeModulesTab.properties().get("nativeRegistryProjection")),
                "descriptor-declared creative tab evidence must not be reported as descriptor-only projection");
        require("itemGroup.EchoAshfallNativeModules".equals(nativeModulesTab.properties().get("titleKey")),
                "descriptor-declared creative tab must preserve title key metadata");
        require(((List<?>) nativeModulesTab.properties().get("itemIds"))
                        .contains("echoashfallprotocol:portable_signal_scanner"),
                "descriptor-declared creative tab must preserve registry-backed item population metadata");
    }

    private static void registerDescriptorCreativeTabIntent(
            EchoNativeRegistryHost registryHost,
            EchoNativeAddonDescriptor descriptor
    ) throws Exception {
        Class<?> servicesClass = List.of(EchoNativeProductLauncher.class.getDeclaredClasses()).stream()
                .filter(candidate -> "NativeHostServices".equals(candidate.getSimpleName()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("NativeHostServices record not found"));
        Constructor<?> constructor = servicesClass.getDeclaredConstructors()[0];
        constructor.setAccessible(true);
        Object services = constructor.newInstance(
                null,
                registryHost,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
        Method method = EchoNativeProductLauncher.class.getDeclaredMethod(
                "registerDescriptorCreativeTabIntent",
                servicesClass,
                EchoNativeAddonDescriptor.class
        );
        method.setAccessible(true);
        method.invoke(null, services, descriptor);
    }

    private static EchoNativeAddonDescriptor descriptorWithAccess(Map<String, Object> access) {
        return new EchoNativeAddonDescriptor(
                "echo.mod.v1",
                "echoashfallprotocol",
                "ECHO: Ashfall Protocol",
                "1.0.0",
                "addon",
                "official_pack",
                "com.knoxhack.echoashfallprotocol.EchoAshfallProtocol",
                EchoNativeRuntimeSide.COMMON,
                EchoNativeTrustLevel.OFFICIAL,
                EchoNativeApiStability.BETA,
                true,
                true,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                access,
                null
        );
    }

    @SuppressWarnings("unchecked")
    private static List<EchoNativeDiagnostic> releaseDiagnostics(
            EchoNativeProductLauncher.EchoNativeProductRuntimeCapabilityReport capabilities
    ) throws Exception {
        Method method = EchoNativeProductLauncher.class.getDeclaredMethod(
                "releaseRuntimeCapabilityDiagnostics",
                String.class,
                EchoNativeProductLauncher.EchoNativeProductRuntimeCapabilityReport.class
        );
        method.setAccessible(true);
        return (List<EchoNativeDiagnostic>) method.invoke(null, "qa-agent3", capabilities);
    }

    private static EchoNativeProductLauncher.EchoNativeProductRuntimeCapabilityReport capabilities(
            int registryEntryCount,
            int liveRegistryBridgeMutatedEntryCount,
            int nativeRegistryHostMutatedEntryCount,
            int trustedRegistryMutatedEntryCount,
            List<String> registeredOnlyFirstClassRegistryKinds,
            List<String> untrustedMutationFirstClassRegistryKinds
    ) {
        return capabilities(
                registryEntryCount,
                liveRegistryBridgeMutatedEntryCount,
                nativeRegistryHostMutatedEntryCount,
                trustedRegistryMutatedEntryCount,
                registeredOnlyFirstClassRegistryKinds,
                List.of(),
                Map.of(),
                untrustedMutationFirstClassRegistryKinds,
                Map.of(),
                Map.of()
        );
    }

    private static EchoNativeProductLauncher.EchoNativeProductRuntimeCapabilityReport capabilities(
            int registryEntryCount,
            int liveRegistryBridgeMutatedEntryCount,
            int nativeRegistryHostMutatedEntryCount,
            int trustedRegistryMutatedEntryCount,
            List<String> registeredOnlyFirstClassRegistryKinds,
            List<String> registeredOnlyFirstClassRegistryIds,
            Map<String, List<String>> registeredOnlyFirstClassRegistryIdsByKind,
            List<String> untrustedMutationFirstClassRegistryKinds
    ) {
        return capabilities(
                registryEntryCount,
                liveRegistryBridgeMutatedEntryCount,
                nativeRegistryHostMutatedEntryCount,
                trustedRegistryMutatedEntryCount,
                registeredOnlyFirstClassRegistryKinds,
                registeredOnlyFirstClassRegistryIds,
                registeredOnlyFirstClassRegistryIdsByKind,
                List.of(),
                List.of(),
                Map.of(),
                untrustedMutationFirstClassRegistryKinds,
                Map.of(),
                Map.of(),
                cleanRegistryBridgeReconciliation()
        );
    }

    private static EchoNativeProductLauncher.EchoNativeProductRuntimeCapabilityReport capabilitiesWithFailures(
            int registryEntryCount,
            int liveRegistryBridgeMutatedEntryCount,
            int nativeRegistryHostMutatedEntryCount,
            int trustedRegistryMutatedEntryCount,
            List<String> failedFirstClassRegistryKinds,
            List<String> failedFirstClassRegistryIds,
            Map<String, List<String>> failedFirstClassRegistryIdsByKind
    ) {
        return capabilities(
                registryEntryCount,
                liveRegistryBridgeMutatedEntryCount,
                nativeRegistryHostMutatedEntryCount,
                trustedRegistryMutatedEntryCount,
                List.of(),
                List.of(),
                Map.of(),
                failedFirstClassRegistryKinds,
                failedFirstClassRegistryIds,
                failedFirstClassRegistryIdsByKind,
                List.of(),
                Map.of(),
                Map.of(),
                cleanRegistryBridgeReconciliation()
        );
    }

    private static EchoNativeProductLauncher.EchoNativeProductRuntimeCapabilityReport capabilities(
            int registryEntryCount,
            int liveRegistryBridgeMutatedEntryCount,
            int nativeRegistryHostMutatedEntryCount,
            int trustedRegistryMutatedEntryCount,
            List<String> registeredOnlyFirstClassRegistryKinds,
            List<String> untrustedMutationFirstClassRegistryKinds,
            Map<String, Integer> untrustedMutationReasonCounts,
            Map<String, Map<String, Integer>> untrustedMutationReasonCountsByKind
    ) {
        return capabilities(
                registryEntryCount,
                liveRegistryBridgeMutatedEntryCount,
                nativeRegistryHostMutatedEntryCount,
                trustedRegistryMutatedEntryCount,
                registeredOnlyFirstClassRegistryKinds,
                List.of(),
                Map.of(),
                List.of(),
                List.of(),
                Map.of(),
                untrustedMutationFirstClassRegistryKinds,
                untrustedMutationReasonCounts,
                untrustedMutationReasonCountsByKind,
                cleanRegistryBridgeReconciliation()
        );
    }

    private static EchoNativeProductLauncher.EchoNativeProductRuntimeCapabilityReport capabilities(
            int registryEntryCount,
            int liveRegistryBridgeMutatedEntryCount,
            int nativeRegistryHostMutatedEntryCount,
            int trustedRegistryMutatedEntryCount,
            List<String> registeredOnlyFirstClassRegistryKinds,
            List<String> registeredOnlyFirstClassRegistryIds,
            Map<String, List<String>> registeredOnlyFirstClassRegistryIdsByKind,
            List<String> untrustedMutationFirstClassRegistryKinds,
            Map<String, Integer> untrustedMutationReasonCounts,
            Map<String, Map<String, Integer>> untrustedMutationReasonCountsByKind
    ) {
        return capabilities(
                registryEntryCount,
                liveRegistryBridgeMutatedEntryCount,
                nativeRegistryHostMutatedEntryCount,
                trustedRegistryMutatedEntryCount,
                registeredOnlyFirstClassRegistryKinds,
                registeredOnlyFirstClassRegistryIds,
                registeredOnlyFirstClassRegistryIdsByKind,
                List.of(),
                List.of(),
                Map.of(),
                untrustedMutationFirstClassRegistryKinds,
                untrustedMutationReasonCounts,
                untrustedMutationReasonCountsByKind,
                cleanRegistryBridgeReconciliation()
        );
    }

    private static EchoNativeProductLauncher.EchoNativeProductRuntimeCapabilityReport capabilities(
            int registryEntryCount,
            int liveRegistryBridgeMutatedEntryCount,
            int nativeRegistryHostMutatedEntryCount,
            int trustedRegistryMutatedEntryCount,
            List<String> registeredOnlyFirstClassRegistryKinds,
            List<String> registeredOnlyFirstClassRegistryIds,
            Map<String, List<String>> registeredOnlyFirstClassRegistryIdsByKind,
            List<String> failedFirstClassRegistryKinds,
            List<String> failedFirstClassRegistryIds,
            Map<String, List<String>> failedFirstClassRegistryIdsByKind,
            List<String> untrustedMutationFirstClassRegistryKinds,
            Map<String, Integer> untrustedMutationReasonCounts,
            Map<String, Map<String, Integer>> untrustedMutationReasonCountsByKind,
            Map<String, Object> registryBridgeMutationReconciliation
    ) {
        return new EchoNativeProductLauncher.EchoNativeProductRuntimeCapabilityReport(
                true,
                true,
                false,
                true,
                true,
                true,
                true,
                true,
                true,
                registryEntryCount,
                liveRegistryBridgeMutatedEntryCount,
                nativeRegistryHostMutatedEntryCount,
                trustedRegistryMutatedEntryCount,
                registeredOnlyFirstClassRegistryKinds,
                registeredOnlyFirstClassRegistryIds,
                registeredOnlyFirstClassRegistryIdsByKind,
                failedFirstClassRegistryKinds,
                failedFirstClassRegistryIds,
                failedFirstClassRegistryIdsByKind,
                untrustedMutationFirstClassRegistryKinds,
                untrustedMutationReasonCounts,
                untrustedMutationReasonCountsByKind,
                registryBridgeMutationReconciliation,
                true, // savesDirectoryConfigured
                0, // fallbackMirrorMutationCount
                1, // mountedResourceCount
                1, // worldStartupResourceCount
                1, // worldgenResourceCount
                1, // worldPresetResourceCount
                1, // dataPackResourceCount
                1, // resourcePackResourceCount
                1, // structureResourceCount
                1, // tagResourceCount
                1, // lifecycleEventCount
                0, // failedLifecycleEventCount
                1, // publishedEventCount
                1, // eventSubscriptionCount
                1, // executedEventHandlerCount
                1, // lifecycleLiveRuntimeMutationCount
                true, // lifecycleMinecraftRuntimeAccessed
                true, // lifecycleLiveRuntimeReleaseProofSatisfied
                1, // queuedCommandCount
                0, // commandFailureCount
                1, // commandLiveRuntimeMutationCount
                true, // commandMinecraftRuntimeAccessed
                true, // commandLiveRuntimeReleaseProofSatisfied
                1, // registeredConfigCount
                1, // configLiveRuntimeMutationCount
                true, // configMinecraftRuntimeAccessed
                true, // configLiveRuntimeReleaseProofSatisfied
                1, // boundNetworkPacketCount
                0, // networkFailureCount
                1, // networkLiveRuntimeMutationCount
                true, // networkMinecraftRuntimeAccessed
                true, // networkLiveRuntimeReleaseProofSatisfied
                1, // adapterCoreMutationRecordCount
                1, // adapterCoreMutatedRecordCount
                1, // adapterCoreLiveRuntimeProofRecordCount
                EchoNativeProductLauncher.EchoNativeProductRuntimeCapabilityReport.requiredAgent5AdapterCoreLiveProofSurfaces(),
                1, // saveDataMutationCount
                List.of("client_tick"),
                List.of("client_tick"),
                true,
                true,
                true,
                false,
                true,
                true,
                false,
                true,
                true,
                true,
                true,
                true,
                true,
                true,
                true,
                1,
                1,
                1,
                1,
                1
        );
    }

    private static Map<String, Object> cleanRegistryBridgeReconciliation() {
        return Map.of(
                "bridgeEvidenceMatchesTrustedEntries", true,
                "missingFromBridgeEvidence", List.of(),
                "bridgeEvidenceWithoutTrustedEntry", List.of()
        );
    }

    private static List<String> probeIds(String prefix) {
        return FIRST_CLASS_KINDS.stream()
                .map(kind -> "echoagent3:" + prefix + "_" + kind + "_probe")
                .toList();
    }

    private static Map<String, List<String>> probeIdsByKind(String prefix) {
        LinkedHashMap<String, List<String>> idsByKind = new LinkedHashMap<>();
        for (String kind : FIRST_CLASS_KINDS) {
            idsByKind.put(kind, List.of("echoagent3:" + prefix + "_" + kind + "_probe"));
        }
        return Map.copyOf(idsByKind);
    }

    private static boolean sameStringSet(List<String> actual, List<String> expected) {
        return actual.size() == expected.size()
                && new java.util.LinkedHashSet<>(actual).equals(new java.util.LinkedHashSet<>(expected));
    }

    private static String serviceSurfaceAlias(String kind) {
        return switch (kind) {
            case "item" -> "items";
            case "block" -> "blocks";
            case "entity" -> "entities";
            case "block_entity" -> "block_entities";
            case "menu" -> "menus";
            case "sound" -> "sounds";
            case "particle" -> "particles";
            case "effect" -> "effects";
            case "command" -> "commands";
            case "data_component" -> "data_components";
            case "recipe" -> "recipes";
            case "creative_tab" -> "creative_tabs";
            case "biome" -> "biomes";
            case "worldgen" -> "world_generators";
            case "client_asset" -> "client_assets";
            default -> kind;
        };
    }

    private record FixedStatusBridge(EchoNativeLoadStatus status) implements NativeLoaderLiveRegistryBridge {
        @Override
        public boolean attached() {
            return true;
        }

        @Override
        public String bridgeId() {
            return "qa-agent3:fixed-status-" + status.name().toLowerCase();
        }

        @Override
        public boolean firstClassNativeRegistry() {
            return true;
        }

        @Override
        public boolean nativeRegistryProcess() {
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
            return status;
        }
    }

    private static final class AggregateMismatchRegistryBridge implements NativeLoaderLiveRegistryBridge {
        private final Map<String, Map<String, Object>> records = new LinkedHashMap<>();

        @Override
        public boolean attached() {
            return true;
        }

        @Override
        public String bridgeId() {
            return "qa-agent3:aggregate-mismatch-registry-bridge";
        }

        @Override
        public boolean firstClassNativeRegistry() {
            return true;
        }

        @Override
        public boolean nativeRegistryProcess() {
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
                    "mutatedRecordIds", List.of("item:echoagent3:actual_runtime_capability_wrong_aggregate_id"),
                    "mutatedRecords", Map.copyOf(records)
            );
        }

        @Override
        public EchoNativeLoadStatus register(
                String registry,
                String namespace,
                String id,
                String implementationClass,
                Map<String, Object> properties
        ) {
            String key = key(registry, namespace, id);
            String fullId = namespace == null || namespace.isBlank() ? id : namespace + ":" + id;
            Map<String, Object> record = new LinkedHashMap<>();
            record.put("registry", registry);
            record.put("namespace", namespace == null ? "" : namespace);
            record.put("id", id == null ? "" : id);
            record.put("fullId", fullId);
            record.put("implementationClass", implementationClass == null ? "" : implementationClass);
            record.put("status", EchoNativeLoadStatus.MUTATED.name());
            record.put("bridgeId", bridgeId());
            record.put("liveRegistryMutationApplied", true);
            record.put("productNativeRegistryTableMutated", true);
            record.put("firstClassNativeRegistry", true);
            record.put("nativeRegistryProcess", true);
            record.put("releaseRegistryTrusted", true);
            record.put("nativeRegistryMutationSupported", true);
            record.put("properties", properties == null ? Map.of() : Map.copyOf(properties));
            records.put(key, Map.copyOf(record));
            return EchoNativeLoadStatus.MUTATED;
        }

        @Override
        public Map<String, Object> registryMutationRecord(String registry, String namespace, String id) {
            return records.getOrDefault(key(registry, namespace, id), Map.of());
        }

        private static String key(String registry, String namespace, String id) {
            return (registry == null ? "" : registry) + ":"
                    + (namespace == null ? "" : namespace) + ":"
                    + (id == null ? "" : id);
        }
    }

    private static final class ProofBackedRegistryBridge implements NativeLoaderLiveRegistryBridge {
        private final Map<String, Map<String, Object>> records = new LinkedHashMap<>();

        @Override
        public boolean attached() {
            return true;
        }

        @Override
        public String bridgeId() {
            return "qa-agent3:descriptor-creative-tab-bridge";
        }

        @Override
        public boolean firstClassNativeRegistry() {
            return true;
        }

        @Override
        public boolean nativeRegistryProcess() {
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
            String key = key(registry, namespace, id);
            String fullId = namespace == null || namespace.isBlank() ? id : namespace + ":" + id;
            Map<String, Object> record = new LinkedHashMap<>();
            record.put("registry", registry);
            record.put("namespace", namespace == null ? "" : namespace);
            record.put("id", id == null ? "" : id);
            record.put("fullId", fullId);
            record.put("implementationClass", implementationClass == null ? "" : implementationClass);
            record.put("status", EchoNativeLoadStatus.MUTATED.name());
            record.put("bridgeId", bridgeId());
            record.put("liveRegistryMutationApplied", true);
            record.put("productNativeRegistryTableMutated", true);
            record.put("firstClassNativeRegistry", true);
            record.put("nativeRegistryProcess", true);
            record.put("releaseRegistryTrusted", true);
            record.put("nativeRegistryMutationSupported", true);
            record.put("properties", properties == null ? Map.of() : Map.copyOf(properties));
            records.put(key, Map.copyOf(record));
            return EchoNativeLoadStatus.MUTATED;
        }

        @Override
        public Map<String, Object> registryMutationRecord(String registry, String namespace, String id) {
            return records.getOrDefault(key(registry, namespace, id), Map.of());
        }

        private static String key(String registry, String namespace, String id) {
            return (registry == null ? "" : registry) + ":"
                    + (namespace == null ? "" : namespace) + ":"
                    + (id == null ? "" : id);
        }
    }

    private static boolean hasDiagnostic(List<EchoNativeDiagnostic> diagnostics, String code) {
        return diagnostics.stream().anyMatch(diagnostic -> code.equals(diagnostic.code()));
    }

    private static EchoNativeDiagnostic diagnostic(List<EchoNativeDiagnostic> diagnostics, String code) {
        return diagnostics.stream()
                .filter(diagnostic -> code.equals(diagnostic.code()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("missing diagnostic: " + code));
    }

    private static String readinessDebug(EchoNativeProductLauncher.EchoNativeProductRuntimeCapabilityReport capabilities) {
        return "nativeRuntimeDispatchReady=" + capabilities.nativeRuntimeDispatchReady()
                + ", agent5LiveRuntimeSurfaceProofReady=" + capabilities.agent5LiveRuntimeSurfaceProofReady()
                + ", nativeLifecycleReady=" + capabilities.nativeLifecycleReady()
                + ", nativeEventsReady=" + capabilities.nativeEventsReady()
                + ", nativeCommandHostReady=" + capabilities.nativeCommandHostReady()
                + ", nativeConfigHostReady=" + capabilities.nativeConfigHostReady()
                + ", nativeNetworkHostReady=" + capabilities.nativeNetworkHostReady()
                + ", adapterCoreMutationsReady=" + capabilities.adapterCoreMutationsReady()
                + ", saveDataHooksReady=" + capabilities.saveDataHooksReady()
                + ", productResourcesReady=" + capabilities.productResourcesReady()
                + ", trustedClientRenderPipelineReady=" + capabilities.trustedClientRenderPipelineReady()
                + ", clientSurfaceCount=" + capabilities.clientSurfaceCount()
                + ", trustedClientRouteMutatedSurfaceCount=" + capabilities.trustedClientRouteMutatedSurfaceCount();
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }
}
