package dev.echo.nativeplatform.loader;

import java.util.List;

public final class EchoNativeAgent3RuntimeCoreSmokeMain {
    private EchoNativeAgent3RuntimeCoreSmokeMain() {
    }

    public static void main(String[] args) {
        EchoNativeAgent3RuntimeCoreResult result = new EchoNativeAgent3RuntimeCoreRuntime().runReferenceScenario();
        require(result.contractReady(), "Agent 3 native runtime core contract must be ready.");
        require(result.adapterCoreBridge(), "Agent 3 native runtime core must be AdapterCore-backed.");
        require(!result.minecraftRuntimeAccessed(), "Agent 3 native runtime core must not access Minecraft runtime in no-launch parity.");
        require(!result.registryMutated(), "Agent 3 native runtime core must not mutate registries in no-launch parity.");
        require("packaged-release-client".equals(result.executableParityTarget()),
                "Agent 3 native runtime core must name the packaged-release-client executable parity target.");
        require(result.packagedReleaseClientParityReady(),
                "Agent 3 native runtime core must mark packaged-release-client parity ready.");
        require(result.phasesVerified().equals(List.of(
                        "phase1-runtime-boundary-lock",
                        "phase2-app-loop",
                        "phase3-adaptercore-binding",
                        "phase4-full-module-loading",
                        "phase5-standalone-playable-loop"
                )),
                "Agent 3 native phases verified drifted.");
        require(result.bootModes().equals(List.of("headless-native", "windowed-dev", "playable-beta", "packaged-release-client")),
                "Agent 3 native boot modes drifted.");
        require(result.tickLayers().equals(List.of(
                        "pre_tick",
                        "input",
                        "network",
                        "world",
                        "entity",
                        "player",
                        "gameplay",
                        "ui",
                        "audio",
                        "render",
                        "save",
                        "post_tick"
                )),
                "Agent 3 native tick layers drifted.");
        require(result.runtimeModuleStatuses().equals(List.of(
                        "runtime-active",
                        "runtime-tooling-only",
                        "runtime-dev-only",
                        "runtime-disabled-with-reason"
                )),
                "Agent 3 native runtime module statuses drifted.");
        require(result.playableLoopChecklist().equals(List.of(
                        "new game",
                        "spawn",
                        "move",
                        "open terminal",
                        "complete objective",
                        "interact with hazard",
                        "use item",
                        "save",
                        "load",
                        "continue",
                        "exit cleanly"
                )),
                "Agent 3 native playable-loop checklist drifted.");
        require(result.playableLoopActions().keySet().equals(new java.util.LinkedHashSet<>(List.of(
                        "newGame",
                        "spawn",
                        "move",
                        "terminal",
                        "objective",
                        "hazard",
                        "item",
                        "save",
                        "load",
                        "continue",
                        "exit"
                ))),
                "Agent 3 native playable-loop action keys drifted.");
        require(result.playableLoopActions().values().stream().allMatch(Boolean.TRUE::equals),
                "Agent 3 native playable-loop actions must all execute.");
        require(result.appLoopEvidence().keySet().equals(new java.util.LinkedHashSet<>(List.of(
                        "diagnosticWriterActive",
                        "diagnosticCount",
                        "shutdownHookReason",
                        "shutdownHookUserRequested",
                        "shutdownHookSaveBeforeExit",
                        "crashDiagnosticWriterActive",
                        "fatalDiagnostics"
                ))),
                "Agent 3 native app-loop evidence keys drifted.");
        require(Boolean.TRUE.equals(result.appLoopEvidence().get("diagnosticWriterActive")),
                "Agent 3 native app-loop diagnostic writer must be active.");
        require(Integer.valueOf(8).equals(result.appLoopEvidence().get("diagnosticCount")),
                "Agent 3 native app-loop diagnostic count must match standalone.");
        require("headless_tick_loop_complete".equals(result.appLoopEvidence().get("shutdownHookReason")),
                "Agent 3 native app-loop shutdown reason must match standalone.");
        require(Boolean.FALSE.equals(result.appLoopEvidence().get("shutdownHookUserRequested")),
                "Agent 3 native app-loop shutdown must not be user requested.");
        require(Boolean.FALSE.equals(result.appLoopEvidence().get("shutdownHookSaveBeforeExit")),
                "Agent 3 native app-loop shutdown must not request save-before-exit.");
        require(Boolean.TRUE.equals(result.appLoopEvidence().get("crashDiagnosticWriterActive")),
                "Agent 3 native crash boundary diagnostic writer must be active.");
        require(Integer.valueOf(1).equals(result.appLoopEvidence().get("fatalDiagnostics")),
                "Agent 3 native crash boundary fatal diagnostic count must match standalone.");
        require(result.moduleGraphEvidence().keySet().equals(new java.util.LinkedHashSet<>(List.of(
                        "readyNodes",
                        "failedNodes",
                        "resolvedRequiredEdge",
                        "missingRequiredContained",
                        "optionalUnresolvedAllowed",
                        "badModuleFailureContained"
                ))),
                "Agent 3 native module-graph evidence keys drifted.");
        require(Integer.valueOf(2).equals(result.moduleGraphEvidence().get("readyNodes")),
                "Agent 3 native module graph ready node count must match standalone smoke.");
        require(Integer.valueOf(1).equals(result.moduleGraphEvidence().get("failedNodes")),
                "Agent 3 native module graph failed node count must match standalone smoke.");
        require(Boolean.TRUE.equals(result.moduleGraphEvidence().get("resolvedRequiredEdge")),
                "Agent 3 native module graph must prove required dependency resolution.");
        require(Boolean.TRUE.equals(result.moduleGraphEvidence().get("missingRequiredContained")),
                "Agent 3 native module graph must contain missing required dependencies.");
        require(Boolean.TRUE.equals(result.moduleGraphEvidence().get("optionalUnresolvedAllowed")),
                "Agent 3 native module graph must allow unresolved optional dependencies.");
        require(Boolean.TRUE.equals(result.moduleGraphEvidence().get("badModuleFailureContained")),
                "Agent 3 native module graph must contain bad module failures.");
        require(result.serviceRegistryEvidence().keySet().equals(new java.util.LinkedHashSet<>(List.of(
                        "moduleRegistry",
                        "moduleGraph",
                        "featureGraph",
                        "sandboxPolicy",
                        "minecraftFree",
                        "neoForgeFree"
                ))),
                "Agent 3 native service-registry evidence keys drifted.");
        require(Boolean.TRUE.equals(result.serviceRegistryEvidence().get("moduleRegistry")),
                "Agent 3 native service registry must bind module registry.");
        require(Boolean.TRUE.equals(result.serviceRegistryEvidence().get("moduleGraph")),
                "Agent 3 native service registry must bind module graph.");
        require(Boolean.TRUE.equals(result.serviceRegistryEvidence().get("featureGraph")),
                "Agent 3 native service registry must bind feature graph.");
        require(Boolean.TRUE.equals(result.serviceRegistryEvidence().get("sandboxPolicy")),
                "Agent 3 native service registry must bind sandbox policy.");
        require(Boolean.TRUE.equals(result.serviceRegistryEvidence().get("minecraftFree")),
                "Agent 3 native service registry evidence must be Minecraft-free.");
        require(Boolean.TRUE.equals(result.serviceRegistryEvidence().get("neoForgeFree")),
                "Agent 3 native service registry evidence must be NeoForge-free.");
        require(result.adapterCoreBindingCounts().keySet().equals(new java.util.LinkedHashSet<>(List.of(
                        "registry",
                        "resources",
                        "ui",
                        "world",
                        "entities",
                        "items",
                        "missions",
                        "saves",
                        "network",
                        "commands",
                        "diagnostics"
                ))),
                "Agent 3 native AdapterCore binding domains drifted.");
        require(Integer.valueOf(47).equals(result.adapterCoreBindingCounts().get("registry")),
                "Agent 3 native registry binding count must match standalone.");
        require(Integer.valueOf(6).equals(result.adapterCoreBindingCounts().get("resources")),
                "Agent 3 native resources binding count must match standalone.");
        require(Integer.valueOf(31).equals(result.adapterCoreBindingCounts().get("ui")),
                "Agent 3 native UI binding count must match standalone.");
        require(Integer.valueOf(7).equals(result.adapterCoreBindingCounts().get("world")),
                "Agent 3 native world binding count must match standalone.");
        require(Integer.valueOf(44).equals(result.adapterCoreBindingCounts().get("entities")),
                "Agent 3 native entities binding count must match standalone.");
        require(Integer.valueOf(69).equals(result.adapterCoreBindingCounts().get("items")),
                "Agent 3 native items binding count must match standalone.");
        require(Integer.valueOf(60).equals(result.adapterCoreBindingCounts().get("missions")),
                "Agent 3 native missions binding count must match standalone.");
        require(Integer.valueOf(22).equals(result.adapterCoreBindingCounts().get("saves")),
                "Agent 3 native saves binding count must match standalone.");
        require(Integer.valueOf(8).equals(result.adapterCoreBindingCounts().get("network")),
                "Agent 3 native network binding count must match standalone.");
        require(Integer.valueOf(9).equals(result.adapterCoreBindingCounts().get("commands")),
                "Agent 3 native commands binding count must match standalone.");
        require(Integer.valueOf(17).equals(result.adapterCoreBindingCounts().get("diagnostics")),
                "Agent 3 native diagnostics binding count must match standalone.");
        require(result.fullCatalogEvidence().keySet().equals(new java.util.LinkedHashSet<>(List.of(
                        "descriptorCount",
                        "runtimeActive",
                        "runtimeToolingOnly",
                        "runtimeDevOnly",
                        "runtimeDisabledWithReason",
                        "systemModuleCount"
                ))),
                "Agent 3 native full-catalog evidence keys drifted.");
        require(Integer.valueOf(95).equals(result.fullCatalogEvidence().get("descriptorCount")),
                "Agent 3 native descriptor count must match standalone.");
        require(Integer.valueOf(85).equals(result.fullCatalogEvidence().get("runtimeActive")),
                "Agent 3 native runtime-active count must match standalone.");
        require(Integer.valueOf(6).equals(result.fullCatalogEvidence().get("runtimeToolingOnly")),
                "Agent 3 native runtime-tooling-only count must match standalone.");
        require(Integer.valueOf(4).equals(result.fullCatalogEvidence().get("runtimeDevOnly")),
                "Agent 3 native runtime-dev-only count must match standalone.");
        require(Integer.valueOf(0).equals(result.fullCatalogEvidence().get("runtimeDisabledWithReason")),
                "Agent 3 native disabled-with-reason count must match standalone.");
        require(Integer.valueOf(8).equals(result.fullCatalogEvidence().get("systemModuleCount")),
                "Agent 3 native required system-module count must match standalone.");
        require(result.requiredSystemModuleStatuses().equals(new java.util.LinkedHashMap<>() {{
                    put("echomodpackcommandcenter", "runtime-tooling-only");
                    put("signalos", "runtime-active");
                    put("signalosexample", "runtime-dev-only");
                    put("echobridgecore", "runtime-dev-only");
                    put("echoagentcore", "runtime-tooling-only");
                    put("echoreportcore", "runtime-tooling-only");
                    put("echometadatacore", "runtime-tooling-only");
                    put("echomodulegraph", "runtime-tooling-only");
                }}),
                "Agent 3 native required system-module statuses must match standalone.");
        require(result.packOsEvidence().keySet().equals(new java.util.LinkedHashSet<>(List.of(
                        "ready",
                        "validLaunchAllowed",
                        "validMounts",
                        "validChannel",
                        "validServicesBound",
                        "badLaunchAllowed",
                        "badBlockers",
                        "badRepairActions",
                        "repairExecutionAllowed"
                ))),
                "Agent 3 native PackOS evidence keys drifted.");
        require(Boolean.TRUE.equals(result.packOsEvidence().get("ready")),
                "Agent 3 native PackOS ready flag must match standalone.");
        require(Boolean.TRUE.equals(result.packOsEvidence().get("validLaunchAllowed")),
                "Agent 3 native PackOS valid launch flag must match standalone.");
        require(Integer.valueOf(5).equals(result.packOsEvidence().get("validMounts")),
                "Agent 3 native PackOS mount count must match standalone.");
        require("alpha".equals(result.packOsEvidence().get("validChannel")),
                "Agent 3 native PackOS channel must match standalone.");
        require(Boolean.TRUE.equals(result.packOsEvidence().get("validServicesBound")),
                "Agent 3 native PackOS services-bound flag must match standalone.");
        require(Boolean.FALSE.equals(result.packOsEvidence().get("badLaunchAllowed")),
                "Agent 3 native PackOS bad launch block must match standalone.");
        require(Integer.valueOf(3).equals(result.packOsEvidence().get("badBlockers")),
                "Agent 3 native PackOS blocker count must match standalone.");
        require(Integer.valueOf(7).equals(result.packOsEvidence().get("badRepairActions")),
                "Agent 3 native PackOS repair action count must match standalone.");
        require(Boolean.FALSE.equals(result.packOsEvidence().get("repairExecutionAllowed")),
                "Agent 3 native PackOS repair execution block must match standalone.");
        require(result.devToolsEvidence().keySet().equals(new java.util.LinkedHashSet<>(List.of(
                        "ready",
                        "serviceCount",
                        "diagnosticProbeEmitted"
                ))),
                "Agent 3 native devtools evidence keys drifted.");
        require(Boolean.TRUE.equals(result.devToolsEvidence().get("ready")),
                "Agent 3 native devtools ready flag must match standalone.");
        require(Integer.valueOf(10).equals(result.devToolsEvidence().get("serviceCount")),
                "Agent 3 native devtools service count must match standalone.");
        require(Boolean.TRUE.equals(result.devToolsEvidence().get("diagnosticProbeEmitted")),
                "Agent 3 native devtools diagnostic probe must match standalone.");
        require(result.liveGraphicsEvidence().keySet().equals(new java.util.LinkedHashSet<>(List.of(
                        "ready",
                        "adapterRenderTarget",
                        "nativeOpenGLPresentVerified",
                        "gameWindowOpenGLPresentVerified",
                        "gameWindowOpenGLSessionVerified",
                        "persistentGameWindowSwapchain",
                        "voxelMeshUploadVerified",
                        "voxelFramebufferUploadVerified",
                        "voxelFramebufferUploadBytes",
                        "softwareVoxelPresenterActive",
                        "visibleFallbackReady",
                        "blockers"
                ))),
                "Agent 3 native live graphics evidence keys drifted.");
        require(Boolean.TRUE.equals(result.liveGraphicsEvidence().get("ready")),
                "Agent 3 native live graphics ready flag must match standalone.");
        require("opengl".equals(result.liveGraphicsEvidence().get("adapterRenderTarget")),
                "Agent 3 native live graphics render target must match standalone.");
        require(Boolean.TRUE.equals(result.liveGraphicsEvidence().get("nativeOpenGLPresentVerified")),
                "Agent 3 native OpenGL present proof must match standalone.");
        require(Boolean.TRUE.equals(result.liveGraphicsEvidence().get("gameWindowOpenGLPresentVerified")),
                "Agent 3 native game-window OpenGL present proof must match standalone.");
        require(Boolean.TRUE.equals(result.liveGraphicsEvidence().get("gameWindowOpenGLSessionVerified")),
                "Agent 3 native game-window OpenGL session proof must match standalone.");
        require(Boolean.TRUE.equals(result.liveGraphicsEvidence().get("persistentGameWindowSwapchain")),
                "Agent 3 native game-window swapchain proof must match standalone.");
        require(Boolean.TRUE.equals(result.liveGraphicsEvidence().get("voxelMeshUploadVerified")),
                "Agent 3 native voxel mesh upload proof must match standalone.");
        require(Boolean.TRUE.equals(result.liveGraphicsEvidence().get("voxelFramebufferUploadVerified")),
                "Agent 3 native voxel framebuffer upload proof must match standalone.");
        require(Integer.valueOf(640 * 360 * 4).equals(result.liveGraphicsEvidence().get("voxelFramebufferUploadBytes")),
                "Agent 3 native voxel framebuffer byte count must match standalone.");
        require(Boolean.TRUE.equals(result.liveGraphicsEvidence().get("softwareVoxelPresenterActive")),
                "Agent 3 native software voxel presenter proof must match standalone.");
        require(Boolean.TRUE.equals(result.liveGraphicsEvidence().get("visibleFallbackReady")),
                "Agent 3 native visible fallback proof must match standalone.");
        require(List.of().equals(result.liveGraphicsEvidence().get("blockers")),
                "Agent 3 native live graphics blockers must match standalone.");
        require(result.headlessEvidence().keySet().equals(new java.util.LinkedHashSet<>(List.of(
                        "success",
                        "ticksRun",
                        "diagnosticWriterActive",
                        "diagnosticCount",
                        "shutdownHookReason",
                        "shutdownHookUserRequested",
                        "shutdownHookSaveBeforeExit",
                        "adapterCoreRuntimeBridgeActive",
                        "executableSystemModules"
                ))),
                "Agent 3 native headless evidence keys drifted.");
        require(Boolean.TRUE.equals(result.headlessEvidence().get("success")),
                "Agent 3 native headless success must match standalone.");
        require(Integer.valueOf(3).equals(result.headlessEvidence().get("ticksRun")),
                "Agent 3 native headless tick count must match standalone.");
        require(Boolean.TRUE.equals(result.headlessEvidence().get("diagnosticWriterActive")),
                "Agent 3 native headless diagnostic writer must match standalone.");
        require(Integer.valueOf(8).equals(result.headlessEvidence().get("diagnosticCount")),
                "Agent 3 native headless diagnostic count must match standalone.");
        require("headless_tick_loop_complete".equals(result.headlessEvidence().get("shutdownHookReason")),
                "Agent 3 native headless shutdown reason must match standalone.");
        require(Boolean.FALSE.equals(result.headlessEvidence().get("shutdownHookUserRequested")),
                "Agent 3 native headless shutdown user flag must match standalone.");
        require(Boolean.FALSE.equals(result.headlessEvidence().get("shutdownHookSaveBeforeExit")),
                "Agent 3 native headless save-before-exit flag must match standalone.");
        require(Boolean.TRUE.equals(result.headlessEvidence().get("adapterCoreRuntimeBridgeActive")),
                "Agent 3 native headless AdapterCore bridge flag must match standalone.");
        require(Integer.valueOf(8).equals(result.headlessEvidence().get("executableSystemModules")),
                "Agent 3 native headless executable system-module count must match standalone.");
        require(result.crashBoundaryEvidence().keySet().equals(new java.util.LinkedHashSet<>(List.of(
                        "exitCode",
                        "crashHandled",
                        "finalLifecycle",
                        "diagnosticWriterActive",
                        "fatalDiagnostics"
                ))),
                "Agent 3 native crash-boundary evidence keys drifted.");
        require("CRASHED".equals(result.crashBoundaryEvidence().get("exitCode")),
                "Agent 3 native crash exit code must match standalone.");
        require(Boolean.TRUE.equals(result.crashBoundaryEvidence().get("crashHandled")),
                "Agent 3 native crash handled flag must match standalone.");
        require("crashed".equals(result.crashBoundaryEvidence().get("finalLifecycle")),
                "Agent 3 native crash lifecycle must match standalone.");
        require(Boolean.TRUE.equals(result.crashBoundaryEvidence().get("diagnosticWriterActive")),
                "Agent 3 native crash diagnostic writer must match standalone.");
        require(Integer.valueOf(1).equals(result.crashBoundaryEvidence().get("fatalDiagnostics")),
                "Agent 3 native crash fatal diagnostic count must match standalone.");
        require(Boolean.TRUE.equals(result.playableBetaEvidence().get("success")),
                "Agent 3 native playable-beta success must match standalone.");
        require(Boolean.TRUE.equals(result.playableBetaEvidence().get("adapterCoreRuntimeBridgeActive")),
                "Agent 3 native playable-beta bridge must match standalone.");
        require(Boolean.TRUE.equals(result.playableBetaEvidence().get("firstPlayableLoopReady")),
                "Agent 3 native playable-beta loop readiness must match standalone.");
        require(Boolean.TRUE.equals(result.packagedReleaseClientEvidence().get("success")),
                "Agent 3 native packaged-release-client success must match standalone.");
        require("packaged-release-client".equals(result.packagedReleaseClientEvidence().get("mode")),
                "Agent 3 native packaged-release-client mode must match standalone.");
        require(Boolean.TRUE.equals(result.packagedReleaseClientEvidence().get("adapterCoreRuntimeBridgeActive")),
                "Agent 3 native packaged-release-client bridge must match standalone.");
        require(Boolean.TRUE.equals(result.packagedReleaseClientEvidence().get("firstPlayableLoopReady")),
                "Agent 3 native packaged-release-client loop readiness must match standalone.");
        require(Boolean.TRUE.equals(result.packagedReleaseClientEvidence().get("liveWindowWalkthroughReady")),
                "Agent 3 native packaged-release-client walkthrough readiness must match standalone.");

        System.out.println("agent3 native runtime core smoke PASS " + result.parityVector());
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
