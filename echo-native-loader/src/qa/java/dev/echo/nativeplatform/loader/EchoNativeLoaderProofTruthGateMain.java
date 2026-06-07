package dev.echo.nativeplatform.loader;

import dev.echo.nativeplatform.diagnostics.EchoNativeJson;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class EchoNativeLoaderProofTruthGateMain {
    private static final String SAMPLE_MODULE_ID = "echoashfallnativeproof";
    private static final String LOADER_MODULE_ID = "echo-native-loader";
    private static final String SAMPLE_SERVICE_CLASS = "dev.echo.nativeplatform.samples.AshfallNativeSampleModule";
    private static final String BACKEND_CLASS = NativeLoaderAdapterCoreBackend.class.getName();
    private static final String RUNTIME_HOST_CLASS = NativeLoaderRuntimeHost.class.getName();
    private static final String RUNTIME_HOST_ID = "echoashfallnativeproof:native_loader_runtime_host";
    private static final String RUNTIME_LANE = "Native Loader";
    private static final Set<String> REQUIRED_MUTATED_SURFACES = Set.of(
            "inventory",
            "player_state",
            "world_blocks",
            "world_state",
            "structures",
            "block_entities",
            "capabilities",
            "events",
            "packets_hud",
            "save_data",
            "hud"
    );

    private EchoNativeLoaderProofTruthGateMain() {
    }

    public static void main(String[] args) throws Exception {
        Path reportPath = args.length > 0
                ? Path.of(args[0]).toAbsolutePath().normalize()
                : Path.of("build/native-loader-proof/native-loader-proof.json").toAbsolutePath().normalize();
        Map<String, Object> report = EchoNativeJson.asObject(EchoNativeJson.parse(
                Files.readString(reportPath, StandardCharsets.UTF_8)
        ));

        require("echo.native.loader_proof.v1".equals(text(report.get("schema"))), "Unexpected proof report schema.");
        require("Native Loader".equals(text(report.get("runtimeLane"))), "Proof report is not for Native Loader.");
        require("primary future mod loader".equals(text(report.get("laneRole"))), "Native Loader lane role drifted.");
        require("NeoForge compatibility backend".equals(text(report.get("fallbackLane"))), "NeoForge fallback lane role drifted.");
        require("Standalone Runtime parity/runtime harness".equals(text(report.get("parityLane"))), "Standalone parity lane role drifted.");

        Map<String, Object> module = EchoNativeJson.asObject(report.get("module"));
        require(SAMPLE_MODULE_ID.equals(text(module.get("moduleId"))), "Sample proof module was not loaded.");
        require("REGISTERED".equals(text(module.get("status"))),
                "Module load status must stop at REGISTERED until a runtime host mutation ledger proves MUTATED.");
        require(!text(module.get("loadedClassName")).isBlank(), "Proof report has no loaded class name.");
        require(!text(module.get("loadedClassLoaderName")).isBlank(), "Proof report has no classloader evidence.");
        require(EchoNativeModuleClassLoader.class.getName().equals(text(module.get("loadedClassLoaderName"))),
                "Proof module must be loaded by EchoNativeModuleClassLoader.");
        require(booleanValue(module.get("loadedByModuleClassLoader")),
                "Proof module must load from descriptor nativeClasspath, not the app classpath.");
        require(booleanValue(report.get("activationClaimAllowed")), "Activation claim must require a loaded class and registered services.");
        require(!booleanValue(module.get("nativeHostMutationClaimAllowed")),
                "Module lifecycle records must not be accepted as Native Loader host mutation proof.");
        require(!text(module.get("nativeHostMutationClaimBlocker")).isBlank(),
                "Module report must explain why lifecycle records are not host mutation evidence.");
        require(booleanValue(module.get("metadataOnlyMutationClaimRejected")),
                "Module report must reject metadata-only MUTATED lifecycle records without typed host receipts.");
        require(number(module.get("typedHostMutationReceiptCount")) == 0,
                "Proof module load must not contain typed host mutation receipts before host service execution.");
        require(booleanValue(module.get("nativeClasspathDeclared")),
                "Proof module must declare explicit nativeClasspath for release truth.");
        require(!booleanValue(module.get("inferredClasspathRequested")),
                "Proof module must not use inferred classpath token.");
        require(!booleanValue(module.get("compatibilityClasspathFallback")),
                "Proof module must not use compatibility classpath fallback.");
        requireReleaseFallbackRejected(report);
        requireReleaseSideGateRejected(report);
        require(registeredSampleModuleServices(module), "Sample addon services/content were not registered.");
        requireCompleteLifecycleHistory(module, "Proof module");
        verifyLoadedModuleState(reportPath, report);
        require(resolvedAdapterCoreServiceBridge(report), "AdapterCore calls must resolve an active Native Loader runtime service.");

        require(booleanValue(report.get("nativeHostMutationClaimAllowed")), "Host mutation claim must be true after the local host mutation proof.");
        require(registeredRuntimeHostSnapshot(report), "Host snapshot must record a registered Native Loader runtime host identity.");
        require(mutatedRequiredSurfaces(report), "Mutation ledger does not prove all required Native Loader host surfaces through the resolved Native Loader backend.");
        require(typedHostReceiptsMutateRequiredSurfaces(report),
                "Typed host mutation receipts do not prove all required Native Loader host surfaces.");
        require(!unsupportedRequiredSurfaceRecords(report), "Required AdapterCore surfaces must not be recorded as unsupported once the Native Loader backend is registered.");
        require(booleanValue(report.get("noOpMutationGuard")), "Native Loader backend must guard unchanged state from false MUTATED claims.");
        require(noOpResolvedRecordsPresent(report), "Mutation ledger must include resolved Native Loader no-op records proving unchanged state is not reported as MUTATED.");

        require(!booleanValue(report.get("gameplayReadyClaimAllowed")), "Local host proof must not claim full gameplay readiness.");
        require(!booleanValue(report.get("liveClientGameplayReadyClaimAllowed")), "Live client gameplay readiness requires live Minecraft evidence.");
        Map<String, Object> liveClientProof = EchoNativeJson.asObject(report.get("liveClientProof"));
        require(!booleanValue(liveClientProof.get("complete")), "Live client proof cannot be complete in the local host harness report.");
        require("INCOMPLETE".equals(text(liveClientProof.get("status"))), "Live client proof status must stay incomplete until live attach evidence exists.");
        require(!booleanValue(liveClientProof.get("launchOrAttachMinecraftClient")), "Local host harness must not claim Minecraft client launch/attach.");
        require(!booleanValue(liveClientProof.get("bootstrapEnteredLiveClient")), "Local host harness must not claim live client bootstrap entry.");
        require(!booleanValue(liveClientProof.get("playerOrWorldMutationInsideLiveRuntime")), "Local host harness must not claim live runtime player/world mutation.");
        requireStableShutdownUnload(report);

        System.out.println("native loader proof truth gate PASS report=" + reportPath);
    }

    private static void requireReleaseFallbackRejected(Map<String, Object> report) {
        Map<String, Object> rejection = EchoNativeJson.asObject(report.get("releaseFallbackRejection"));
        require(!rejection.isEmpty(), "Proof report must include release fallback rejection evidence.");
        require("FAILED".equals(text(rejection.get("status"))),
                "Release fallback probe must fail closed.");
        require(!booleanValue(rejection.get("nativeClasspathDeclared")),
                "Release fallback probe must prove nativeClasspath was not declared.");
        require(booleanValue(rejection.get("compatibilityClasspathFallback")),
                "Release fallback probe must expose compatibilityClasspathFallback=true.");
        require(text(rejection.get("loadedClassName")).isBlank(),
                "Release fallback probe must fail before loading the entrypoint class.");
        require(!booleanValue(rejection.get("loadedByModuleClassLoader")),
                "Release fallback probe must not load through the module classloader.");
        require(list(rejection, "diagnostics").stream()
                        .map(EchoNativeLoaderProofTruthGateMain::text)
                        .anyMatch(value -> value.contains("Release loading rejected module")
                                && value.contains("compatibility native classpath fallback")),
                "Release fallback probe diagnostics must explain release classpath rejection.");
        require(list(rejection, "lifecyclePhaseHistory").stream()
                        .map(EchoNativeJson::asObject)
                        .anyMatch(record -> "LOAD_CLASSES".equals(text(record.get("phase")))
                                && "FAILED".equals(text(record.get("status")))
                                && booleanValue(record.get("failed"))),
                "Release fallback probe must record failed LOAD_CLASSES lifecycle evidence.");
    }

    private static void requireReleaseSideGateRejected(Map<String, Object> report) {
        Map<String, Object> rejection = EchoNativeJson.asObject(report.get("releaseSideGateRejection"));
        require(!rejection.isEmpty(), "Proof report must include release side-gate rejection evidence.");
        require("FAILED".equals(text(rejection.get("status"))),
                "Release side-gate probe must fail closed.");
        require("CLIENT".equals(text(rejection.get("side"))),
                "Release side-gate probe must expose the client-only descriptor side.");
        require("SERVER".equals(text(rejection.get("releaseHostSide"))),
                "Release side-gate probe must expose the server host side.");
        require(booleanValue(rejection.get("nativeClasspathDeclared")),
                "Release side-gate probe should use explicit classpath so the failure proves side gating.");
        require(!booleanValue(rejection.get("compatibilityClasspathFallback")),
                "Release side-gate probe must not be a classpath fallback rejection.");
        require(text(rejection.get("loadedClassName")).isBlank(),
                "Release side-gate probe must fail before loading the entrypoint class.");
        require(!booleanValue(rejection.get("loadedByModuleClassLoader")),
                "Release side-gate probe must not load through the module classloader.");
        require(list(rejection, "diagnostics").stream()
                        .map(EchoNativeLoaderProofTruthGateMain::text)
                        .anyMatch(value -> value.contains("Release loading rejected module")
                                && value.contains("descriptor side CLIENT")
                                && value.contains("host side SERVER")),
                "Release side-gate diagnostics must explain the side mismatch.");
        require(list(rejection, "lifecyclePhaseHistory").stream()
                        .map(EchoNativeJson::asObject)
                        .anyMatch(record -> "RESOLVE".equals(text(record.get("phase")))
                                && "FAILED".equals(text(record.get("status")))
                                && booleanValue(record.get("failed"))),
                "Release side-gate probe must record failed RESOLVE lifecycle evidence.");
    }

    private static void requireStableShutdownUnload(Map<String, Object> report) {
        require(booleanValue(report.get("stableShutdownUnloadBehavior")),
                "Proof report must explicitly prove stable shutdown/unload behavior.");
        require("MUTATED".equals(text(report.get("shutdownStatus"))),
                "Loaded module shutdown must complete successfully once.");
        require("UNSUPPORTED".equals(text(report.get("secondShutdownStatus"))),
                "Second shutdown must be unsupported after the module handle is removed.");
        require("UNSUPPORTED".equals(text(report.get("unknownShutdownStatus"))),
                "Failed or unsupported load results must not be shutdown as loaded modules.");
    }

    private static boolean registeredSampleModuleServices(Map<String, Object> module) {
        Object services = module.get("registeredServices");
        if (!(services instanceof List<?> list)) {
            return false;
        }
        boolean firstSpawn = false;
        boolean dropPod = false;
        boolean itemService = false;
        boolean block = false;
        for (Object rawService : list) {
            Map<String, Object> service = EchoNativeJson.asObject(rawService);
            String serviceId = text(service.get("serviceId"));
            firstSpawn = firstSpawn || "ashfall.first_spawn".equals(serviceId)
                    && serviceSurfaces(service).containsAll(List.of("events", "inventory", "save_data", "hud"));
            dropPod = dropPod || "ashfall.drop_pod".equals(serviceId)
                    && serviceSurfaces(service).containsAll(List.of("world_blocks", "structures", "events"));
            itemService = itemService || "content.item.echoashfallprotocol.drop_pod_beacon".equals(serviceId);
            block = block || "content.block.echoashfallprotocol.drop_pod_marker".equals(serviceId);
        }
        return firstSpawn && dropPod && itemService && block;
    }

    private static void verifyLoadedModuleState(Path reportPath, Map<String, Object> report) throws Exception {
        Map<String, Object> inlineState = EchoNativeJson.asObject(report.get("loadedModuleState"));
        String statePathText = text(report.get("loadedModuleStatePath"));
        require(!statePathText.isBlank(), "Proof report must include a persisted loaded-module state path.");
        Path statePath = Path.of(statePathText);
        if (!statePath.isAbsolute()) {
            statePath = reportPath.getParent().resolve(statePath).normalize();
        }
        require(Files.isRegularFile(statePath), "Persisted loaded-module state file is missing: " + statePath);
        Map<String, Object> persistedState = EchoNativeJson.asObject(EchoNativeJson.parse(
                Files.readString(statePath, StandardCharsets.UTF_8)
        ));
        require("echo.native.loaded_module_state.v1".equals(text(persistedState.get("schema"))),
                "Loaded-module state schema drifted.");
        require(SAMPLE_MODULE_ID.equals(text(persistedState.get("moduleId"))),
                "Loaded-module state module id drifted.");
        require("REGISTERED".equals(text(persistedState.get("status"))),
                "Loaded-module state must record REGISTERED before host mutation.");
        require(SAMPLE_MODULE_ID.equals(text(EchoNativeJson.asObject(persistedState.get("descriptor")).get("id"))),
                "Loaded-module state descriptor id is missing.");
        require(list(persistedState, "classpath").stream().map(EchoNativeLoaderProofTruthGateMain::text)
                        .anyMatch(value -> value.endsWith("echoashfallnativeproof-module.jar")),
                "Loaded-module state must record the descriptor module jar classpath.");
        Map<String, Object> classloader = EchoNativeJson.asObject(persistedState.get("classloader"));
        require(EchoNativeModuleClassLoader.class.getName().equals(text(classloader.get("implementationClass"))),
                "Loaded-module state must record EchoNativeModuleClassLoader.");
        require(booleanValue(classloader.get("loadedByModuleClassLoader")),
                "Loaded-module state must prove descriptor classpath loading.");
        require(SAMPLE_SERVICE_CLASS.equals(text(persistedState.get("loadedClassName"))),
                "Loaded-module state loaded class drifted.");
        require(SAMPLE_SERVICE_CLASS.equals(text(persistedState.get("constructedEntrypointClassName"))),
                "Loaded-module state must record constructed entrypoint class.");
        require(Boolean.TRUE.equals(persistedState.get("entrypointConstructed")),
                "Loaded-module state must prove entrypoint construction.");
        require(list(persistedState, "resolvedDependencies").isEmpty(),
                "Sample loaded-module state should not invent resolved dependencies.");
        require(list(persistedState, "missingDependencies").isEmpty(),
                "Sample loaded-module state should not have missing dependencies.");
        require(hasStateService(persistedState, "ashfall.first_spawn"),
                "Loaded-module state must record the sample first-spawn service.");
        require(hasStateService(persistedState, "ashfall.drop_pod"),
                "Loaded-module state must record the sample drop-pod service.");
        require(hasStateService(persistedState, "content.item.echoashfallprotocol.drop_pod_beacon"),
                "Loaded-module state must record registered content item.");
        require(hasStateService(persistedState, "content.block.echoashfallprotocol.drop_pod_marker"),
                "Loaded-module state must record registered content block.");
        require(number(persistedState.get("typedHostMutationReceiptCount")) == 0,
                "Loaded-module state must not persist metadata-only lifecycle records as typed host mutations.");
        require(list(persistedState, "lifecyclePhases").containsAll(List.of(
                "DISCOVER",
                "RESOLVE",
                "LOAD_CLASSES",
                "CONSTRUCT",
                "REGISTER_SERVICES",
                "REGISTER_CONTENT",
                "COMMON_SETUP",
                "CLIENT_SETUP",
                "SERVER_SETUP",
                "READY"
        )), "Loaded-module state lifecycle phases are incomplete.");
        requireCompleteLifecycleHistory(persistedState, "Loaded-module state");
        require(!Boolean.TRUE.equals(EchoNativeJson.asObject(persistedState.get("failure")).get("failed")),
                "Loaded-module state must not record failure for the proof module.");
        require(SAMPLE_MODULE_ID.equals(text(inlineState.get("moduleId"))),
                "Inline loaded-module state must match persisted module id.");
    }

    private static void requireCompleteLifecycleHistory(Map<String, Object> state, String label) {
        Object raw = state.get("lifecyclePhaseHistory");
        require(raw instanceof List<?>, label + " must record lifecyclePhaseHistory.");
        List<?> list = (List<?>) raw;
        require(!list.isEmpty(), label + " must record lifecyclePhaseHistory.");
        Set<String> phases = new LinkedHashSet<>();
        for (Object item : list) {
            Map<String, Object> record = EchoNativeJson.asObject(item);
            String phase = text(record.get("phase"));
            String status = text(record.get("status"));
            phases.add(phase);
            require(!phase.isBlank(), label + " lifecycle record has no phase.");
            require(!status.isBlank(), label + " lifecycle record has no status for " + phase + ".");
            require(!text(record.get("detail")).isBlank(), label + " lifecycle record has no detail for " + phase + ".");
            require(!booleanValue(record.get("failed")), label + " lifecycle record unexpectedly failed for " + phase + ".");
            require(!"FAILED".equals(status), label + " lifecycle record has FAILED status for " + phase + ".");
            require(!"UNSUPPORTED".equals(status), label + " lifecycle record has UNSUPPORTED status for " + phase + ".");
        }
        require(phases.containsAll(List.of(
                "DISCOVER",
                "RESOLVE",
                "LOAD_CLASSES",
                "CONSTRUCT",
                "REGISTER_SERVICES",
                "REGISTER_CONTENT",
                "COMMON_SETUP",
                "CLIENT_SETUP",
                "SERVER_SETUP",
                "READY"
        )), label + " lifecyclePhaseHistory is incomplete.");
    }

    private static boolean hasStateService(Map<String, Object> state, String serviceId) {
        for (Object item : list(state, "registeredServices")) {
            if (serviceId.equals(text(EchoNativeJson.asObject(item).get("serviceId")))) {
                return true;
            }
        }
        for (Object item : list(state, "registeredContent")) {
            if (serviceId.equals(text(EchoNativeJson.asObject(item).get("serviceId")))) {
                return true;
            }
        }
        return false;
    }

    private static boolean mutatedRequiredSurfaces(Map<String, Object> report) {
        Object ledger = report.get("mutationLedger");
        if (!(ledger instanceof List<?> list)) {
            return false;
        }
        Set<String> mutated = new LinkedHashSet<>();
        for (Object item : list) {
            Map<String, Object> record = EchoNativeJson.asObject(item);
            if ("MUTATED".equals(text(record.get("status"))) && resolvedNativeLoaderMutationRecord(record)) {
                mutated.add(text(record.get("surface")));
            }
        }
        return mutated.containsAll(REQUIRED_MUTATED_SURFACES);
    }

    private static boolean unsupportedRequiredSurfaceRecords(Map<String, Object> report) {
        Object ledger = report.get("mutationLedger");
        if (!(ledger instanceof List<?> list)) {
            return true;
        }
        for (Object item : list) {
            Map<String, Object> record = EchoNativeJson.asObject(item);
            if (REQUIRED_MUTATED_SURFACES.contains(text(record.get("surface")))
                    && "UNSUPPORTED".equals(text(record.get("status")))) {
                return true;
            }
        }
        return false;
    }

    private static boolean typedHostReceiptsMutateRequiredSurfaces(Map<String, Object> report) {
        Object receipts = report.get("typedHostMutationReceipts");
        if (!(receipts instanceof List<?> list)) {
            return false;
        }
        Set<String> mutated = new LinkedHashSet<>();
        for (Object item : list) {
            Map<String, Object> receipt = EchoNativeJson.asObject(item);
            if ("MUTATED".equals(text(receipt.get("status")))
                    && NativeLoaderAdapterCoreBackend.SERVICE_ID.equals(text(receipt.get("serviceId")))) {
                Map<String, Object> evidence = EchoNativeJson.asObject(receipt.get("evidence"));
                if (LOADER_MODULE_ID.equals(text(evidence.get("resolvedModuleId")))
                        && BACKEND_CLASS.equals(text(evidence.get("resolvedServiceClass")))
                        && RUNTIME_HOST_ID.equals(text(evidence.get("runtimeHostId")))
                        && booleanValue(evidence.get("runtimeHostRegistered"))) {
                    mutated.add(text(receipt.get("surface")));
                }
            }
        }
        return mutated.containsAll(REQUIRED_MUTATED_SURFACES);
    }

    private static boolean noOpResolvedRecordsPresent(Map<String, Object> report) {
        Object ledger = report.get("mutationLedger");
        if (!(ledger instanceof List<?> list)) {
            return false;
        }
        boolean worldBlockResolved = false;
        boolean saveDataResolved = false;
        for (Object item : list) {
            Map<String, Object> record = EchoNativeJson.asObject(item);
            if ("RESOLVED".equals(text(record.get("status")))
                    && "world_blocks".equals(text(record.get("surface")))
                    && resolvedNativeLoaderMutationRecord(record)) {
                worldBlockResolved = true;
            }
            if ("RESOLVED".equals(text(record.get("status")))
                    && "save_data".equals(text(record.get("surface")))
                    && resolvedNativeLoaderMutationRecord(record)) {
                saveDataResolved = true;
            }
        }
        return worldBlockResolved && saveDataResolved;
    }

    private static boolean resolvedAdapterCoreServiceBridge(Map<String, Object> report) {
        Map<String, Object> serviceBridge = EchoNativeJson.asObject(report.get("serviceBridge"));
        if (!booleanValue(serviceBridge.get("resolved"))
                || !NativeLoaderAdapterCoreBackend.SERVICE_ID.equals(text(serviceBridge.get("serviceId")))) {
            return false;
        }
        Object activeRuntimeServices = serviceBridge.get("activeRuntimeServices");
        if (!(activeRuntimeServices instanceof List<?> list)) {
            return false;
        }
        for (Object item : list) {
            Map<String, Object> service = EchoNativeJson.asObject(item);
            if (NativeLoaderAdapterCoreBackend.SERVICE_ID.equals(text(service.get("serviceId")))
                    && LOADER_MODULE_ID.equals(text(service.get("moduleId")))
                    && BACKEND_CLASS.equals(text(service.get("implementationClass")))
                    && serviceSurfaces(service).containsAll(REQUIRED_MUTATED_SURFACES)) {
                return true;
            }
        }
        return false;
    }

    private static boolean resolvedNativeLoaderMutationRecord(Map<String, Object> record) {
        return NativeLoaderAdapterCoreBackend.SERVICE_ID.equals(text(record.get("serviceId")))
                && LOADER_MODULE_ID.equals(text(record.get("resolvedModuleId")))
                && BACKEND_CLASS.equals(text(record.get("resolvedServiceClass")))
                && BACKEND_CLASS.equals(text(record.get("backendClass")))
                && RUNTIME_HOST_CLASS.equals(text(record.get("runtimeHostClass")))
                && RUNTIME_LANE.equals(text(record.get("runtimeLane")))
                && RUNTIME_HOST_ID.equals(text(record.get("runtimeHostId")))
                && booleanValue(record.get("runtimeHostRegistered"));
    }

    private static boolean registeredRuntimeHostSnapshot(Map<String, Object> report) {
        Map<String, Object> hostSnapshot = EchoNativeJson.asObject(report.get("hostSnapshot"));
        Map<String, Object> runtimeHost = EchoNativeJson.asObject(hostSnapshot.get("runtimeHost"));
        return RUNTIME_HOST_ID.equals(text(runtimeHost.get("runtimeHostId")))
                && RUNTIME_HOST_CLASS.equals(text(runtimeHost.get("runtimeHostClass")))
                && RUNTIME_LANE.equals(text(runtimeHost.get("runtimeLane")))
                && booleanValue(runtimeHost.get("runtimeHostRegistered"))
                && SAMPLE_MODULE_ID.equals(text(runtimeHost.get("moduleId")));
    }

    private static Set<String> serviceSurfaces(Map<String, Object> service) {
        Object surfaces = service.get("surfaces");
        if (!(surfaces instanceof List<?> list)) {
            return Set.of();
        }
        Set<String> result = new LinkedHashSet<>();
        for (Object surface : list) {
            result.add(text(surface));
        }
        return result;
    }

    private static List<Object> list(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        return List.copyOf(list);
    }

    private static boolean booleanValue(Object value) {
        return value instanceof Boolean bool && bool;
    }

    private static long number(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value == null) {
            return 0L;
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return 0L;
        }
    }

    private static String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }
}
