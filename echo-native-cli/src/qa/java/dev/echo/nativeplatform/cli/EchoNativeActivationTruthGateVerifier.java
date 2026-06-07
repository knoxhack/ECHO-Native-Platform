package dev.echo.nativeplatform.cli;

import dev.echo.nativeplatform.contracts.EchoNativeAddonDescriptor;
import dev.echo.nativeplatform.contracts.EchoNativeApiStability;
import dev.echo.nativeplatform.contracts.EchoNativeRuntimeSide;
import dev.echo.nativeplatform.contracts.EchoNativeTrustLevel;
import dev.echo.nativeplatform.diagnostics.EchoNativeJson;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoNativeActivationTruthGateVerifier {
    private EchoNativeActivationTruthGateVerifier() {
    }

    public static void main(String[] args) throws Exception {
        Map<String, Object> broadHookOnly = invokeActivationStatus(Map.of(
                "modules", List.of(Map.of(
                        "id", "echoashfallprotocol",
                        "nativeModuleActivated", true,
                        "liveGameplayHookVerified", true,
                        "state", "native_product_gameplay_verified")),
                "adapterCoreRuntimeBridgeActive", true,
                "nativeFirstPlayableLoopReady", true,
                "nativeLiveGameplayHandlersAttached", true,
                "nativeWorldLiveHostHooksVerified", true));
        require(Integer.valueOf(0).equals(broadHookOnly.get("activeModuleCount")),
                "broad module hook flags must not count as activation without nativeActivations evidence");
        Map<String, Object> broadOnlyModule = firstModule(broadHookOnly);
        require(Boolean.FALSE.equals(broadOnlyModule.get("activeInLiveClient")),
                "module must not be active without loadedClassName evidence");
        require("pending_native_runtime_bridge".equals(broadOnlyModule.get("activationState")),
                "module without loadedClassName must stay pending");

        Map<String, Object> loadedClass = invokeActivationStatus(loadedClassMarker(Map.of()));
        require(Integer.valueOf(1).equals(loadedClass.get("activeModuleCount")),
                "loaded class evidence must count as activation");
        Map<String, Object> loadedModule = firstModule(loadedClass);
        require(Boolean.TRUE.equals(loadedModule.get("activeInLiveClient")),
                "loaded class evidence must make the module active");
        require("com.knoxhack.echoashfallprotocol.EchoAshfallProtocolNativeModule"
                        .equals(loadedModule.get("loadedClassName")),
                "activation status must expose the loaded class name");
        require(Boolean.TRUE.equals(loadedModule.get("nativeAdapterCodeExecuted")),
                "activation status must expose adapter code execution");
        require(Boolean.FALSE.equals(loadedClass.get("playableNativeProductModules")),
                "loaded class and activation flags alone must not mark native product modules playable");

        Map<String, Object> completeLiveRuntime = invokeActivationStatus(loadedClassMarker(completeNativeLoaderLiveProof()));
        require(Boolean.TRUE.equals(completeLiveRuntime.get("playableNativeProductModules")),
                "complete Native Loader live proof must be required before native product modules are marked playable");
        require(Boolean.TRUE.equals(completeLiveRuntime.get("nativeLoaderLiveProofComplete")),
                "playable activation status must expose accepted live runtime proof");

        verifyStoredReportShape();
        verifyStoredMarkerShape();

        System.out.println("native activation truth gate PASS broad_hooks_do_not_activate=true loaded_class_required=true playable_requires_live_mutation_proof=true");
    }

    private static void verifyStoredReportShape() throws Exception {
        Path reportPath = Path.of("reports/echo-native/ashfall/native-product-module-activation-status.json");
        if (!Files.isRegularFile(reportPath)) {
            return;
        }
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(reportPath)));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        Object modules = data.get("modules");
        if (!(modules instanceof List<?> list)) {
            throw new AssertionError("stored activation status report must include module rows");
        }
        int activeCount = 0;
        for (Object raw : list) {
            Map<String, Object> module = EchoNativeJson.asObject(raw);
            String reason = String.valueOf(module.getOrDefault("reason", ""));
            require(!reason.contains("Live activation marker verified"),
                    "stored activation report must not use broad live-marker activation wording");
            if (Boolean.TRUE.equals(module.get("activeInLiveClient"))) {
                activeCount++;
                require(Boolean.TRUE.equals(module.get("classLoadedInLiveClient")),
                        "active stored module rows must expose classLoadedInLiveClient=true");
                require(!String.valueOf(module.getOrDefault("loadedClassName", "")).isBlank(),
                        "active stored module rows must expose loadedClassName");
                require(Boolean.TRUE.equals(module.get("nativeAdapterCodeExecuted")),
                        "active stored module rows must expose adapter code execution");
            }
        }
        Object activeModuleCount = data.get("activeModuleCount");
        require(!(activeModuleCount instanceof Number number) || number.intValue() == activeCount,
                "stored activation report activeModuleCount must match active rows");
        if (Boolean.TRUE.equals(data.get("playableNativeProductModules"))) {
            require(Boolean.TRUE.equals(data.get("nativeLoaderLiveProofComplete")),
                    "stored activation report must not mark native product modules playable without accepted Native Loader live proof");
            require("MUTATED".equals(String.valueOf(data.getOrDefault("nativeLoaderLiveProofStatus", ""))),
                    "stored activation report must not mark native product modules playable unless Native Loader live proof status is MUTATED");
            require(Boolean.TRUE.equals(data.get("nativeLoaderLiveProofRequiredMutationSurfacesMutated")),
                    "stored activation report must not mark native product modules playable unless required mutation surfaces are mutated");
        }
    }

    private static void verifyStoredMarkerShape() throws Exception {
        Path markerPath = Path.of("fixtures/ashfall/isolated-runtime/game/echo-native/module-activation.json");
        if (!Files.isRegularFile(markerPath)) {
            return;
        }
        Map<String, Object> marker = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(markerPath)));
        int loadedActivationCount = 0;
        Object activations = marker.get("nativeActivations");
        if (activations instanceof List<?> activationList) {
            for (Object raw : activationList) {
                Map<String, Object> activation = EchoNativeJson.asObject(raw);
                boolean loaded = Boolean.TRUE.equals(activation.get("activated"))
                        && Boolean.TRUE.equals(activation.get("nativeAdapterCodeExecuted"))
                        && !String.valueOf(activation.getOrDefault("entrypoint", "")).isBlank()
                        && !String.valueOf(activation.getOrDefault("loadedClassName", "")).isBlank();
                if (loaded) {
                    loadedActivationCount++;
                }
                require(!Boolean.TRUE.equals(activation.get("activated")) || loaded,
                        "stored activation marker must not set activated=true without loadedClassName evidence");
            }
        }
        Object nativeActivationCount = marker.get("nativeActivationCount");
        require(!(nativeActivationCount instanceof Number number) || number.intValue() == loadedActivationCount,
                "stored activation marker nativeActivationCount must count loaded classes only");

        Object modules = marker.get("modules");
        if (modules instanceof List<?> moduleList) {
            for (Object raw : moduleList) {
                Map<String, Object> module = EchoNativeJson.asObject(raw);
                boolean loadedClassPresent = !String.valueOf(module.getOrDefault("loadedClassName", "")).isBlank();
                require(!Boolean.TRUE.equals(module.get("nativeModuleActivated")) || loadedClassPresent,
                        "stored marker module rows must not activate without loadedClassName evidence");
                require(!Boolean.TRUE.equals(module.get("liveGameplayHookVerified")) || loadedClassPresent,
                        "stored marker module rows must not verify gameplay hooks without loadedClassName evidence");
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> invokeActivationStatus(Map<String, Object> marker) throws Exception {
        return EchoNativeProductActivationStatus.productModuleActivationStatus(
                "ashfall",
                List.of(descriptor()),
                marker,
                List.of());
    }

    private static EchoNativeAddonDescriptor descriptor() {
        return new EchoNativeAddonDescriptor(
                "echo.native.addon.v1",
                "echoashfallprotocol",
                "Ashfall Protocol",
                "1.0.0",
                "pack_root",
                "official_pack",
                "com.knoxhack.echoashfallprotocol.EchoAshfallProtocolNativeModule",
                EchoNativeRuntimeSide.COMMON,
                EchoNativeTrustLevel.OFFICIAL,
                EchoNativeApiStability.ALPHA,
                true,
                true,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                Map.of(),
                Path.of("echo.mod.json"));
    }

    private static Map<String, Object> loadedClassMarker(Map<String, Object> nativeLoaderLiveProof) {
        Map<String, Object> marker = new LinkedHashMap<>();
        marker.put("modules", List.of(Map.of(
                "id", "echoashfallprotocol",
                "nativeModuleActivated", true,
                "liveGameplayHookVerified", true,
                "state", "native_product_gameplay_verified")));
        marker.put("nativeActivations", List.of(Map.of(
                "moduleId", "echoashfallprotocol",
                "activated", true,
                "entrypoint", "com.knoxhack.echoashfallprotocol.EchoAshfallProtocolNativeModule",
                "loadedClassName", "com.knoxhack.echoashfallprotocol.EchoAshfallProtocolNativeModule",
                "nativeAdapterCodeExecuted", true,
                "serviceCodeExecuted", true)));
        marker.put("adapterCoreRuntimeBridgeActive", true);
        marker.put("nativeFirstPlayableLoopReady", true);
        marker.put("nativeLiveGameplayHandlersAttached", true);
        marker.put("nativeWorldLiveHostHooksVerified", true);
        if (!nativeLoaderLiveProof.isEmpty()) {
            marker.put("nativeLoaderLiveProof", nativeLoaderLiveProof);
        }
        return marker;
    }

    private static Map<String, Object> completeNativeLoaderLiveProof() {
        Map<String, Object> proof = new LinkedHashMap<>();
        proof.put("status", "MUTATED");
        proof.put("complete", true);
        proof.put("gameplayReadyClaimAllowed", true);
        proof.put("liveClientGameplayReadyClaimAllowed", true);
        proof.put("nativeMutationLedgerRecorded", true);
        proof.put("requiredMutationSurfacesMutated", true);
        proof.put("livePlayerOrWorldMutation", true);
        proof.put("liveSaveDataWrite", true);
        proof.put("liveHudNotificationEmitted", true);
        proof.put("mutationLedgerMutatedSurfaces", List.of("hud", "inventory", "save_data", "world_blocks"));
        proof.put("missingTargets", List.of());
        return proof;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> firstModule(Map<String, Object> report) {
        Object modules = report.get("modules");
        if (!(modules instanceof List<?> list) || list.isEmpty()) {
            return new LinkedHashMap<>();
        }
        Object first = list.getFirst();
        if (!(first instanceof Map<?, ?> map)) {
            return new LinkedHashMap<>();
        }
        Map<String, Object> module = new LinkedHashMap<>();
        map.forEach((key, value) -> module.put(String.valueOf(key), value));
        return module;
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
