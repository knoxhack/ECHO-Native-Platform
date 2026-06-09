package dev.echo.nativeplatform.contracts;

import java.util.List;

public final class EchoNativeAgent3RuntimeCoreContract {
    private EchoNativeAgent3RuntimeCoreContract() {
    }

    public static List<String> bootModes() {
        return List.of("headless-native", "windowed-dev", "playable-beta", "packaged-release-client");
    }

    public static List<String> phasesVerified() {
        return List.of(
                "phase1-runtime-boundary-lock",
                "phase2-app-loop",
                "phase3-adaptercore-binding",
                "phase4-full-module-loading",
                "phase5-standalone-playable-loop"
        );
    }

    public static List<String> tickLayers() {
        return List.of(
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
        );
    }

    public static List<String> runtimeModuleStatuses() {
        return List.of(
                "runtime-active",
                "runtime-tooling-only",
                "runtime-dev-only",
                "runtime-disabled-with-reason"
        );
    }

    public static List<String> playableLoopChecklist() {
        return List.of(
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
        );
    }

    public static List<String> playableLoopActionKeys() {
        return List.of(
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
        );
    }

    public static List<String> appLoopEvidenceKeys() {
        return List.of(
                "diagnosticWriterActive",
                "diagnosticCount",
                "shutdownHookReason",
                "shutdownHookUserRequested",
                "shutdownHookSaveBeforeExit",
                "crashDiagnosticWriterActive",
                "fatalDiagnostics"
        );
    }

    public static List<String> moduleGraphEvidenceKeys() {
        return List.of(
                "readyNodes",
                "failedNodes",
                "resolvedRequiredEdge",
                "missingRequiredContained",
                "optionalUnresolvedAllowed",
                "badModuleFailureContained"
        );
    }

    public static List<String> serviceRegistryEvidenceKeys() {
        return List.of(
                "moduleRegistry",
                "moduleGraph",
                "featureGraph",
                "sandboxPolicy",
                "minecraftFree",
                "neoForgeFree"
        );
    }

    public static List<String> adapterCoreBindingDomains() {
        return List.of(
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
        );
    }

    public static List<String> fullCatalogEvidenceKeys() {
        return List.of(
                "descriptorCount",
                "runtimeActive",
                "runtimeToolingOnly",
                "runtimeDevOnly",
                "runtimeDisabledWithReason",
                "systemModuleCount"
        );
    }

    public static List<String> requiredSystemModuleIds() {
        return List.of(
                "signalos",
                "signalosexample",
                "echobridgecore",
                "echoagentcore",
                "echoreportcore",
                "echometadatacore",
                "echomodulegraph"
        );
    }

    public static List<String> packOsEvidenceKeys() {
        return List.of(
                "ready",
                "validLaunchAllowed",
                "validMounts",
                "validChannel",
                "validServicesBound",
                "badLaunchAllowed",
                "badBlockers",
                "badRepairActions",
                "repairExecutionAllowed"
        );
    }

    public static List<String> devToolsEvidenceKeys() {
        return List.of(
                "ready",
                "serviceCount",
                "diagnosticProbeEmitted"
        );
    }

    public static List<String> liveGraphicsEvidenceKeys() {
        return List.of(
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
        );
    }

    public static List<String> headlessEvidenceKeys() {
        return List.of(
                "success",
                "ticksRun",
                "diagnosticWriterActive",
                "diagnosticCount",
                "shutdownHookReason",
                "shutdownHookUserRequested",
                "shutdownHookSaveBeforeExit",
                "adapterCoreRuntimeBridgeActive",
                "executableSystemModules"
        );
    }

    public static List<String> crashBoundaryEvidenceKeys() {
        return List.of(
                "exitCode",
                "crashHandled",
                "finalLifecycle",
                "diagnosticWriterActive",
                "fatalDiagnostics"
        );
    }

    public static List<String> playableBetaEvidenceKeys() {
        return List.of(
                "success",
                "adapterCoreRuntimeBridgeActive",
                "firstPlayableLoopReady"
        );
    }

    public static List<String> packagedReleaseClientEvidenceKeys() {
        return List.of(
                "success",
                "mode",
                "adapterCoreRuntimeBridgeActive",
                "firstPlayableLoopReady",
                "liveWindowWalkthroughReady"
        );
    }
}
