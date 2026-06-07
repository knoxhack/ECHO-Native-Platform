package dev.echo.nativeplatform.bootstrap;

import dev.echo.nativeplatform.loader.NativeLoaderLiveInteractionProbeBridge;

import dev.echo.nativeplatform.loader.NativeLoaderProductPlayableRuntimeBridge;
import dev.echo.nativeplatform.loader.NativeLoaderAshfallWorldStartupService;
import dev.echo.nativeplatform.loader.NativeLoaderProductPlayableRuntimeActions;
import dev.echo.nativeplatform.loader.NativeLoaderLiveClientDiagnostics;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

final class EchoNativeLiveClientProbeRunner {
    private static final Set<String> WINDOW_TITLE_KEEPERS = ConcurrentHashMap.newKeySet();
    private static final Set<String> OPENED_PRODUCT_WORLDS = ConcurrentHashMap.newKeySet();

    private EchoNativeLiveClientProbeRunner() {
    }

    static Map<String, Object> apply(
            Path markerPath,
            int attempt,
            Context context
    ) throws ReflectiveOperationException, IOException {
        Class<?> minecraftClass = Class.forName(context.runtimeClass("client.Minecraft"));
        Object minecraft = minecraftClass.getMethod("getInstance").invoke(null);
        if (minecraft == null) {
            return liveClientProbe(context, false, false, false, attempt, "minecraft_instance_missing", "");
        }
        Object player = context.fieldReader.get(minecraft, "player");
        Object gui = context.fieldReader.get(minecraft, "gui");
        Object level = context.fieldReader.get(minecraft, "level");
        Object screen = context.fieldReader.get(minecraft, "screen");
        boolean sampleClientRuntime = attempt == 0 || attempt % 40 == 0;
        boolean clientThreadAccepted = false;
        boolean windowTitleApplied = false;
        if (sampleClientRuntime) {
            boolean[] titleApplied = new boolean[]{false};
            clientThreadAccepted = context.clientThreadInvoker.invoke(minecraftClass, minecraft, () -> {
                try {
                    NativeLoaderLiveClientDiagnostics.setWindowTitle(minecraft, context.windowTitle);
                    titleApplied[0] = true;
                } catch (ReflectiveOperationException ignored) {
                    titleApplied[0] = false;
                }
            });
            windowTitleApplied = clientThreadAccepted && titleApplied[0];
            if (windowTitleApplied) {
                startWindowTitleKeeper(minecraftClass, minecraft, context);
            }
        }
        if (player == null || gui == null) {
            Map<String, Object> probe = liveClientProbe(
                    context,
                    false,
                    false,
                    false,
                    attempt,
                    "client_player_or_gui_not_ready",
                    ""
            );
            probe.put("clientRuntimeAccessed", true);
            probe.put("clientThreadScheduled", clientThreadAccepted);
            probe.put("windowTitleApplied", windowTitleApplied);
            probe.put("liveClientLifecycleHookAttached", clientThreadAccepted || windowTitleApplied);
            NativeLoaderLiveClientDiagnostics.addDiagnostics(
                    probe,
                    minecraft,
                    sampleClientRuntime,
                    context.methodReader::get
            );
            NativeLoaderLiveClientDiagnostics.addState(probe, minecraft, player, gui, level, screen);
            putProductWorldEvidence(probe, context, level != null, false);
            return probe;
        }

        if (!windowTitleApplied) {
            boolean[] titleApplied = new boolean[]{false};
            windowTitleApplied = context.clientThreadInvoker.invoke(minecraftClass, minecraft, () -> {
                try {
                    NativeLoaderLiveClientDiagnostics.setWindowTitle(minecraft, context.windowTitle);
                    titleApplied[0] = true;
                } catch (ReflectiveOperationException ignored) {
                    titleApplied[0] = false;
                }
            }) && titleApplied[0];
            if (windowTitleApplied) {
                startWindowTitleKeeper(minecraftClass, minecraft, context);
            }
        }
        boolean hudSent = false;
        boolean chatSent = false;
        Map<String, Object> liveInteractions = NativeLoaderLiveInteractionProbeBridge.execute(
                minecraftClass,
                minecraft,
                player,
                context.liveInteractionConfig,
                context.clientThreadInvoker,
                context.fieldReader,
                context.methodReader,
                context.itemAction,
                context.blockPlacement,
                context.blockAction,
                context.commandExecutor
        );
        NativeLoaderProductPlayableRuntimeActions.Config playableActionConfig = context.playableActionConfig;
        Map<String, Object> playableBetaRuntime = NativeLoaderProductPlayableRuntimeBridge.apply(
                minecraftClass,
                minecraft,
                player,
                gui,
                context.playableRuntimeBridgeConfig,
                (clientMinecraftClass, clientMinecraft, action) ->
                        context.clientThreadInvoker.invoke(clientMinecraftClass, clientMinecraft, action),
                context.hostInventoryMutation,
                starterPlayer -> NativeLoaderProductPlayableRuntimeActions.grantStarterTools(
                        playableActionConfig,
                        starterPlayer,
                        context.itemGranter
                ),
                (commandMinecraft, commandPlayer, commandResult) -> NativeLoaderProductPlayableRuntimeActions.sendStarterCommands(
                        playableActionConfig,
                        commandPlayer,
                        commandResult,
                        (playerTarget, command) -> context.commandExecutor.execute(playerTarget, command)
                ),
                (serverMinecraft, serverPlayer, serverResult) -> NativeLoaderProductPlayableRuntimeActions.paintServerStarterRegion(
                        playableActionConfig,
                        serverMinecraft,
                        serverPlayer,
                        serverResult,
                        (target, methodName) -> context.methodReader.get(target, methodName),
                        context.intMethodReader,
                        context.blockSetter
                ),
                (clientMinecraft, clientPlayer, clientResult) -> NativeLoaderProductPlayableRuntimeActions.paintClientStarterRegion(
                        playableActionConfig,
                        clientMinecraft,
                        clientPlayer,
                        clientResult,
                        (target, fieldName) -> context.fieldReader.get(target, fieldName),
                        (target, methodName) -> context.methodReader.get(target, methodName),
                        context.intMethodReader,
                        context.blockSetter
                ),
                context.hostWorldBlockMutation,
                context.saveDataWriter,
                context.hudNotificationPublisher
        );
        Map<String, Object> probe = liveClientProbe(
                context,
                true,
                hudSent,
                chatSent,
                attempt,
                "executed",
                player.getClass().getName()
        );
        probe.put("windowTitleApplied", windowTitleApplied);
        probe.put("clientRuntimeAccessed", true);
        probe.put("clientThreadScheduled", true);
        probe.put("liveClientLifecycleHookAttached", true);
        probe.put("liveInteractionProbe", liveInteractions);
        probe.put(context.playableRuntimeKey, playableBetaRuntime);
        NativeLoaderLiveClientDiagnostics.addDiagnostics(
                probe,
                minecraft,
                true,
                context.methodReader::get
        );
        NativeLoaderLiveClientDiagnostics.addState(probe, minecraft, player, gui, level, screen);
        putProductWorldEvidence(probe, context, level != null, true);
        context.probeWriter.write(markerPath, probe);
        return probe;
    }

    private static void putProductWorldEvidence(
            Map<String, Object> probe,
            Context context,
            boolean levelPresent,
            boolean playerPresent
    ) {
        Map<String, Object> rawEvidence = NativeLoaderAshfallWorldStartupService.liveProductWorldEvidence(
                context.gameDir,
                levelPresent,
                playerPresent
        );
        Map<String, Object> evidence = latchProductWorldOpened(rawEvidence);
        probe.put("nativeProductWorldEvidence", evidence);
        probe.put("nativeProductWorldOpened", evidence.get("nativeProductWorldOpened"));
        probe.put("nativeProductWorldMarkerWritten", evidence.get("productWorldMarkerWritten"));
        probe.put("nativeProductWorldDatapackStaged", evidence.get("stagedDatapackReady"));
        probe.put("nativeProductWorldLevelDatPresent", evidence.get("productWorldLevelDatPresent"));
        probe.put("nativeProductWorldPresetForced",
                NativeLoaderAshfallWorldStartupService.WORLD_PRESET_ID.equals(evidence.get("worldPreset")));
    }

    private static Map<String, Object> latchProductWorldOpened(Map<String, Object> evidence) {
        String key = String.valueOf(evidence.getOrDefault("saveDir", ""));
        if (key.isBlank()) {
            return evidence;
        }
        if (Boolean.TRUE.equals(evidence.get("nativeProductWorldOpened"))) {
            OPENED_PRODUCT_WORLDS.add(key);
            return evidence;
        }
        if (!OPENED_PRODUCT_WORLDS.contains(key)
                || !Boolean.TRUE.equals(evidence.get("productWorldMarkerValid"))
                || !Boolean.TRUE.equals(evidence.get("stagedDatapackReady"))
                || !Boolean.TRUE.equals(evidence.get("productWorldLevelDatPresent"))) {
            return evidence;
        }
        Map<String, Object> latched = new LinkedHashMap<>(evidence);
        latched.put("nativeProductWorldOpened", true);
        latched.put("nativeProductWorldOpenedLatched", true);
        latched.put("minecraftLevelObservedEarlier", true);
        latched.put("playerObservedEarlier", true);
        return Map.copyOf(latched);
    }

    private static void startWindowTitleKeeper(
            Class<?> minecraftClass,
            Object minecraft,
            Context context
    ) {
        if (context.windowTitle.isBlank()) {
            return;
        }
        String key = System.identityHashCode(minecraft) + ":" + context.windowTitle;
        if (!WINDOW_TITLE_KEEPERS.add(key)) {
            return;
        }
        Thread thread = new Thread(() -> {
            for (int attempt = 0; attempt < 600; attempt++) {
                try {
                    context.clientThreadInvoker.invoke(minecraftClass, minecraft, () -> {
                        try {
                            NativeLoaderLiveClientDiagnostics.setWindowTitle(minecraft, context.windowTitle);
                        } catch (ReflectiveOperationException ignored) {
                            // Minecraft may briefly rebuild the window during handoff; the next pass retries.
                        }
                    });
                    Object running = context.methodReader.get(minecraft, "isRunning");
                    if (attempt > 10 && Boolean.FALSE.equals(running)) {
                        return;
                    }
                    Thread.sleep(1_000L);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    return;
                } catch (Throwable ignored) {
                    try {
                        Thread.sleep(1_000L);
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }
        }, "EchoNativeWindowTitleKeeper");
        thread.setDaemon(true);
        thread.start();
    }

    private static Map<String, Object> liveClientProbe(
            Context context,
            boolean executed,
            boolean hudSent,
            boolean chatSent,
            int attempt,
            String state,
            String playerClass
    ) {
        return probe(
                context.nativeLoaderActive,
                context.mainLabel,
                context.clientLabel,
                executed,
                hudSent,
                chatSent,
                attempt,
                state,
                playerClass
        );
    }

    static Map<String, Object> probe(
            boolean nativeLoaderActive,
            String mainLabel,
            String clientLabel,
            boolean executed,
            boolean hudSent,
            boolean chatSent,
            int attempt,
            String state,
            String playerClass
    ) {
        Map<String, Object> probe = new LinkedHashMap<>();
        probe.put("schema", "echo.native.live_client_probe.v1");
        probe.put("installed", true);
        probe.put("executed", executed);
        probe.put("hudProbeSent", hudSent);
        probe.put("chatProbeSent", chatSent);
        probe.put("nativeLoaderTextLabelApplied", nativeLoaderActive && executed);
        probe.put("windowTitleApplied", false);
        probe.put("hudLabelSent", nativeLoaderActive && hudSent);
        probe.put("chatLabelSent", nativeLoaderActive && chatSent);
        probe.put("mainLabelText", mainLabel);
        probe.put("labelText", clientLabel);
        probe.put("attempt", attempt);
        probe.put("state", state);
        probe.put("playerClass", playerClass);
        probe.put("moduleGameplayParityClaimed", false);
        probe.put("strategy", "minecraft_client_thread_reflection_probe");
        probe.put("summary", executed
                ? "Native bootstrap code executed inside the live Minecraft client and recorded module-route diagnostics without fake HUD/chat output."
                : "Native bootstrap live client diagnostics are waiting for the Minecraft client/player.");
        return probe;
    }

    static final class Context {
        private final String playableRuntimeKey;
        private final boolean nativeLoaderActive;
        private final String mainLabel;
        private final String clientLabel;
        private final String windowTitle;
        private final Path gameDir;
        private final RuntimeClassResolver runtimeClassResolver;
        private final NativeLoaderLiveInteractionProbeBridge.Config liveInteractionConfig;
        private final NativeLoaderProductPlayableRuntimeBridge.Config playableRuntimeBridgeConfig;
        private final NativeLoaderProductPlayableRuntimeActions.Config playableActionConfig;
        private final NativeLoaderLiveInteractionProbeBridge.ClientThreadInvoker clientThreadInvoker;
        private final NativeLoaderLiveInteractionProbeBridge.FieldReader fieldReader;
        private final NativeLoaderLiveInteractionProbeBridge.MethodReader methodReader;
        private final NativeLoaderLiveInteractionProbeBridge.ItemAction itemAction;
        private final NativeLoaderLiveInteractionProbeBridge.BlockPlacement blockPlacement;
        private final NativeLoaderLiveInteractionProbeBridge.BlockAction blockAction;
        private final NativeLoaderLiveInteractionProbeBridge.CommandExecutor commandExecutor;
        private final NativeLoaderProductPlayableRuntimeBridge.HostInventoryMutation hostInventoryMutation;
        private final NativeLoaderProductPlayableRuntimeActions.ItemGranter itemGranter;
        private final NativeLoaderProductPlayableRuntimeActions.IntMethodReader intMethodReader;
        private final NativeLoaderProductPlayableRuntimeActions.BlockSetter blockSetter;
        private final NativeLoaderProductPlayableRuntimeBridge.HostWorldBlockMutation hostWorldBlockMutation;
        private final NativeLoaderProductPlayableRuntimeBridge.SaveDataWriter saveDataWriter;
        private final NativeLoaderProductPlayableRuntimeBridge.HudNotificationPublisher hudNotificationPublisher;
        private final ProbeWriter probeWriter;

        Context(
                String playableRuntimeKey,
                boolean nativeLoaderActive,
                String mainLabel,
                String clientLabel,
                String windowTitle,
                Path gameDir,
                RuntimeClassResolver runtimeClassResolver,
                NativeLoaderLiveInteractionProbeBridge.Config liveInteractionConfig,
                NativeLoaderProductPlayableRuntimeBridge.Config playableRuntimeBridgeConfig,
                NativeLoaderProductPlayableRuntimeActions.Config playableActionConfig,
                NativeLoaderLiveInteractionProbeBridge.ClientThreadInvoker clientThreadInvoker,
                NativeLoaderLiveInteractionProbeBridge.FieldReader fieldReader,
                NativeLoaderLiveInteractionProbeBridge.MethodReader methodReader,
                NativeLoaderLiveInteractionProbeBridge.ItemAction itemAction,
                NativeLoaderLiveInteractionProbeBridge.BlockPlacement blockPlacement,
                NativeLoaderLiveInteractionProbeBridge.BlockAction blockAction,
                NativeLoaderLiveInteractionProbeBridge.CommandExecutor commandExecutor,
                NativeLoaderProductPlayableRuntimeBridge.HostInventoryMutation hostInventoryMutation,
                NativeLoaderProductPlayableRuntimeActions.ItemGranter itemGranter,
                NativeLoaderProductPlayableRuntimeActions.IntMethodReader intMethodReader,
                NativeLoaderProductPlayableRuntimeActions.BlockSetter blockSetter,
                NativeLoaderProductPlayableRuntimeBridge.HostWorldBlockMutation hostWorldBlockMutation,
                NativeLoaderProductPlayableRuntimeBridge.SaveDataWriter saveDataWriter,
                NativeLoaderProductPlayableRuntimeBridge.HudNotificationPublisher hudNotificationPublisher,
                ProbeWriter probeWriter
        ) {
            this.playableRuntimeKey = playableRuntimeKey == null ? "" : playableRuntimeKey;
            this.nativeLoaderActive = nativeLoaderActive;
            this.mainLabel = mainLabel == null ? "" : mainLabel;
            this.clientLabel = clientLabel == null ? "" : clientLabel;
            this.windowTitle = windowTitle == null ? "" : windowTitle;
            this.gameDir = gameDir == null ? Path.of(".").toAbsolutePath().normalize() : gameDir.toAbsolutePath().normalize();
            this.runtimeClassResolver = runtimeClassResolver;
            this.liveInteractionConfig = liveInteractionConfig;
            this.playableRuntimeBridgeConfig = playableRuntimeBridgeConfig;
            this.playableActionConfig = playableActionConfig;
            this.clientThreadInvoker = clientThreadInvoker;
            this.fieldReader = fieldReader;
            this.methodReader = methodReader;
            this.itemAction = itemAction;
            this.blockPlacement = blockPlacement;
            this.blockAction = blockAction;
            this.commandExecutor = commandExecutor;
            this.hostInventoryMutation = hostInventoryMutation;
            this.itemGranter = itemGranter;
            this.intMethodReader = intMethodReader;
            this.blockSetter = blockSetter;
            this.hostWorldBlockMutation = hostWorldBlockMutation;
            this.saveDataWriter = saveDataWriter;
            this.hudNotificationPublisher = hudNotificationPublisher;
            this.probeWriter = probeWriter;
        }

        private String runtimeClass(String suffix) {
            return runtimeClassResolver.resolve(suffix);
        }
    }

    @FunctionalInterface
    interface RuntimeClassResolver {
        String resolve(String suffix);
    }

    @FunctionalInterface
    interface ProbeWriter {
        void write(Path markerPath, Map<String, Object> probe) throws IOException;
    }
}
