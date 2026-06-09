package dev.echo.nativeplatform.loader;

import java.util.Collections;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

public record EchoNativeAgent3RuntimeCoreResult(
        String runtime,
        boolean adapterCoreBridge,
        boolean minecraftRuntimeAccessed,
        boolean registryMutated,
        String executableParityTarget,
        boolean packagedReleaseClientParityReady,
        Map<String, Boolean> playableLoopActions,
        Map<String, Object> appLoopEvidence,
        Map<String, Object> moduleGraphEvidence,
        Map<String, Object> serviceRegistryEvidence,
        Map<String, Integer> adapterCoreBindingCounts,
        Map<String, Integer> fullCatalogEvidence,
        Map<String, String> requiredSystemModuleStatuses,
        Map<String, Object> packOsEvidence,
        Map<String, Object> devToolsEvidence,
        Map<String, Object> liveGraphicsEvidence,
        Map<String, Object> headlessEvidence,
        Map<String, Object> crashBoundaryEvidence,
        Map<String, Object> playableBetaEvidence,
        Map<String, Object> packagedReleaseClientEvidence,
        List<String> phasesVerified,
        List<String> bootModes,
        List<String> tickLayers,
        List<String> runtimeModuleStatuses,
        List<String> playableLoopChecklist,
        Map<String, Object> parityVector
) {
    public EchoNativeAgent3RuntimeCoreResult {
        playableLoopActions = Collections.unmodifiableMap(new LinkedHashMap<>(playableLoopActions));
        appLoopEvidence = Collections.unmodifiableMap(new LinkedHashMap<>(appLoopEvidence));
        moduleGraphEvidence = Collections.unmodifiableMap(new LinkedHashMap<>(moduleGraphEvidence));
        serviceRegistryEvidence = Collections.unmodifiableMap(new LinkedHashMap<>(serviceRegistryEvidence));
        adapterCoreBindingCounts = Collections.unmodifiableMap(new LinkedHashMap<>(adapterCoreBindingCounts));
        fullCatalogEvidence = Collections.unmodifiableMap(new LinkedHashMap<>(fullCatalogEvidence));
        requiredSystemModuleStatuses = Collections.unmodifiableMap(new LinkedHashMap<>(requiredSystemModuleStatuses));
        packOsEvidence = Collections.unmodifiableMap(new LinkedHashMap<>(packOsEvidence));
        devToolsEvidence = Collections.unmodifiableMap(new LinkedHashMap<>(devToolsEvidence));
        liveGraphicsEvidence = Collections.unmodifiableMap(new LinkedHashMap<>(liveGraphicsEvidence));
        headlessEvidence = Collections.unmodifiableMap(new LinkedHashMap<>(headlessEvidence));
        crashBoundaryEvidence = Collections.unmodifiableMap(new LinkedHashMap<>(crashBoundaryEvidence));
        playableBetaEvidence = Collections.unmodifiableMap(new LinkedHashMap<>(playableBetaEvidence));
        packagedReleaseClientEvidence = Collections.unmodifiableMap(new LinkedHashMap<>(packagedReleaseClientEvidence));
        phasesVerified = List.copyOf(phasesVerified);
        bootModes = List.copyOf(bootModes);
        tickLayers = List.copyOf(tickLayers);
        runtimeModuleStatuses = List.copyOf(runtimeModuleStatuses);
        playableLoopChecklist = List.copyOf(playableLoopChecklist);
        parityVector = Map.copyOf(parityVector);
    }

    public boolean contractReady() {
        return adapterCoreBridge
                && !minecraftRuntimeAccessed
                && !registryMutated
                && "packaged-release-client".equals(executableParityTarget)
                && packagedReleaseClientParityReady
                && playableLoopActions.size() == 11
                && playableLoopActions.values().stream().allMatch(Boolean.TRUE::equals)
                && Boolean.TRUE.equals(appLoopEvidence.get("diagnosticWriterActive"))
                && Integer.valueOf(8).equals(appLoopEvidence.get("diagnosticCount"))
                && "headless_tick_loop_complete".equals(appLoopEvidence.get("shutdownHookReason"))
                && Boolean.FALSE.equals(appLoopEvidence.get("shutdownHookUserRequested"))
                && Boolean.FALSE.equals(appLoopEvidence.get("shutdownHookSaveBeforeExit"))
                && Boolean.TRUE.equals(appLoopEvidence.get("crashDiagnosticWriterActive"))
                && Integer.valueOf(1).equals(appLoopEvidence.get("fatalDiagnostics"))
                && Integer.valueOf(2).equals(moduleGraphEvidence.get("readyNodes"))
                && Integer.valueOf(1).equals(moduleGraphEvidence.get("failedNodes"))
                && Boolean.TRUE.equals(moduleGraphEvidence.get("resolvedRequiredEdge"))
                && Boolean.TRUE.equals(moduleGraphEvidence.get("missingRequiredContained"))
                && Boolean.TRUE.equals(moduleGraphEvidence.get("optionalUnresolvedAllowed"))
                && Boolean.TRUE.equals(moduleGraphEvidence.get("badModuleFailureContained"))
                && Boolean.TRUE.equals(serviceRegistryEvidence.get("moduleRegistry"))
                && Boolean.TRUE.equals(serviceRegistryEvidence.get("moduleGraph"))
                && Boolean.TRUE.equals(serviceRegistryEvidence.get("featureGraph"))
                && Boolean.TRUE.equals(serviceRegistryEvidence.get("sandboxPolicy"))
                && Boolean.TRUE.equals(serviceRegistryEvidence.get("minecraftFree"))
                && Boolean.TRUE.equals(serviceRegistryEvidence.get("neoForgeFree"))
                && Integer.valueOf(47).equals(adapterCoreBindingCounts.get("registry"))
                && Integer.valueOf(6).equals(adapterCoreBindingCounts.get("resources"))
                && Integer.valueOf(31).equals(adapterCoreBindingCounts.get("ui"))
                && Integer.valueOf(7).equals(adapterCoreBindingCounts.get("world"))
                && Integer.valueOf(44).equals(adapterCoreBindingCounts.get("entities"))
                && Integer.valueOf(69).equals(adapterCoreBindingCounts.get("items"))
                && Integer.valueOf(60).equals(adapterCoreBindingCounts.get("missions"))
                && Integer.valueOf(22).equals(adapterCoreBindingCounts.get("saves"))
                && Integer.valueOf(8).equals(adapterCoreBindingCounts.get("network"))
                && Integer.valueOf(9).equals(adapterCoreBindingCounts.get("commands"))
                && Integer.valueOf(17).equals(adapterCoreBindingCounts.get("diagnostics"))
                && Integer.valueOf(94).equals(fullCatalogEvidence.get("descriptorCount"))
                && Integer.valueOf(85).equals(fullCatalogEvidence.get("runtimeActive"))
                && Integer.valueOf(5).equals(fullCatalogEvidence.get("runtimeToolingOnly"))
                && Integer.valueOf(4).equals(fullCatalogEvidence.get("runtimeDevOnly"))
                && Integer.valueOf(0).equals(fullCatalogEvidence.get("runtimeDisabledWithReason"))
                && Integer.valueOf(7).equals(fullCatalogEvidence.get("systemModuleCount"))
                && "runtime-active".equals(requiredSystemModuleStatuses.get("signalos"))
                && "runtime-dev-only".equals(requiredSystemModuleStatuses.get("signalosexample"))
                && "runtime-dev-only".equals(requiredSystemModuleStatuses.get("echobridgecore"))
                && "runtime-tooling-only".equals(requiredSystemModuleStatuses.get("echoagentcore"))
                && "runtime-tooling-only".equals(requiredSystemModuleStatuses.get("echoreportcore"))
                && "runtime-tooling-only".equals(requiredSystemModuleStatuses.get("echometadatacore"))
                && "runtime-tooling-only".equals(requiredSystemModuleStatuses.get("echomodulegraph"))
                && Boolean.TRUE.equals(packOsEvidence.get("ready"))
                && Boolean.TRUE.equals(packOsEvidence.get("validLaunchAllowed"))
                && Integer.valueOf(5).equals(packOsEvidence.get("validMounts"))
                && "alpha".equals(packOsEvidence.get("validChannel"))
                && Boolean.TRUE.equals(packOsEvidence.get("validServicesBound"))
                && Boolean.FALSE.equals(packOsEvidence.get("badLaunchAllowed"))
                && Integer.valueOf(3).equals(packOsEvidence.get("badBlockers"))
                && Integer.valueOf(7).equals(packOsEvidence.get("badRepairActions"))
                && Boolean.FALSE.equals(packOsEvidence.get("repairExecutionAllowed"))
                && Boolean.TRUE.equals(devToolsEvidence.get("ready"))
                && Integer.valueOf(10).equals(devToolsEvidence.get("serviceCount"))
                && Boolean.TRUE.equals(devToolsEvidence.get("diagnosticProbeEmitted"))
                && Boolean.TRUE.equals(liveGraphicsEvidence.get("ready"))
                && "opengl".equals(liveGraphicsEvidence.get("adapterRenderTarget"))
                && Boolean.TRUE.equals(liveGraphicsEvidence.get("nativeOpenGLPresentVerified"))
                && Boolean.TRUE.equals(liveGraphicsEvidence.get("gameWindowOpenGLPresentVerified"))
                && Boolean.TRUE.equals(liveGraphicsEvidence.get("gameWindowOpenGLSessionVerified"))
                && Boolean.TRUE.equals(liveGraphicsEvidence.get("persistentGameWindowSwapchain"))
                && Boolean.TRUE.equals(liveGraphicsEvidence.get("voxelMeshUploadVerified"))
                && Boolean.TRUE.equals(liveGraphicsEvidence.get("voxelFramebufferUploadVerified"))
                && Integer.valueOf(640 * 360 * 4).equals(liveGraphicsEvidence.get("voxelFramebufferUploadBytes"))
                && Boolean.TRUE.equals(liveGraphicsEvidence.get("softwareVoxelPresenterActive"))
                && Boolean.TRUE.equals(liveGraphicsEvidence.get("visibleFallbackReady"))
                && List.of().equals(liveGraphicsEvidence.get("blockers"))
                && Boolean.TRUE.equals(headlessEvidence.get("success"))
                && Integer.valueOf(3).equals(headlessEvidence.get("ticksRun"))
                && Boolean.TRUE.equals(headlessEvidence.get("diagnosticWriterActive"))
                && Integer.valueOf(8).equals(headlessEvidence.get("diagnosticCount"))
                && "headless_tick_loop_complete".equals(headlessEvidence.get("shutdownHookReason"))
                && Boolean.FALSE.equals(headlessEvidence.get("shutdownHookUserRequested"))
                && Boolean.FALSE.equals(headlessEvidence.get("shutdownHookSaveBeforeExit"))
                && Boolean.TRUE.equals(headlessEvidence.get("adapterCoreRuntimeBridgeActive"))
                && Integer.valueOf(8).equals(headlessEvidence.get("executableSystemModules"))
                && "CRASHED".equals(crashBoundaryEvidence.get("exitCode"))
                && Boolean.TRUE.equals(crashBoundaryEvidence.get("crashHandled"))
                && "crashed".equals(crashBoundaryEvidence.get("finalLifecycle"))
                && Boolean.TRUE.equals(crashBoundaryEvidence.get("diagnosticWriterActive"))
                && Integer.valueOf(1).equals(crashBoundaryEvidence.get("fatalDiagnostics"))
                && Boolean.TRUE.equals(playableBetaEvidence.get("success"))
                && Boolean.TRUE.equals(playableBetaEvidence.get("adapterCoreRuntimeBridgeActive"))
                && Boolean.TRUE.equals(playableBetaEvidence.get("firstPlayableLoopReady"))
                && Boolean.TRUE.equals(packagedReleaseClientEvidence.get("success"))
                && "packaged-release-client".equals(packagedReleaseClientEvidence.get("mode"))
                && Boolean.TRUE.equals(packagedReleaseClientEvidence.get("adapterCoreRuntimeBridgeActive"))
                && Boolean.TRUE.equals(packagedReleaseClientEvidence.get("firstPlayableLoopReady"))
                && Boolean.TRUE.equals(packagedReleaseClientEvidence.get("liveWindowWalkthroughReady"))
                && phasesVerified.equals(List.of(
                "phase1-runtime-boundary-lock",
                "phase2-app-loop",
                "phase3-adaptercore-binding",
                "phase4-full-module-loading",
                "phase5-standalone-playable-loop"
        ))
                && bootModes.size() == 4
                && tickLayers.size() == 12
                && runtimeModuleStatuses.size() == 4
                && playableLoopChecklist.size() == 11;
    }
}
