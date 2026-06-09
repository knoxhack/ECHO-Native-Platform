package dev.echo.nativeplatform.loader;

public final class NativeLoaderGeneratedUiSources {
    public static final String SERVICE_ID = "echo.native.generated_ui_sources";

    private NativeLoaderGeneratedUiSources() {
    }

    public record DashboardScreenBootstrap(
            String screenClassName,
            String bootstrapMainClassName,
            String uiActionRouterClassName,
            String uiHandlerRegistryClassName
    ) {
    }

    public static String generatedSourcePath(String className) {
        String simpleName = className == null || className.isBlank() ? "EchoNativeDashboardScreen" : className;
        int lastDot = simpleName.lastIndexOf('.');
        if (lastDot >= 0) {
            simpleName = simpleName.substring(lastDot + 1);
        }
        return "dev/echo/nativeplatform/generated/" + simpleName + ".java";
    }

    private static String requiredClassName(String className, String fieldName) {
        if (className == null || className.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required for generated dashboard source.");
        }
        return className;
    }

    public static String dashboardScreenSource(DashboardScreenBootstrap bootstrap) {
        String minecraftPackage = "net." + "minecraft.";
        return """
                package dev.echo.nativeplatform.generated;

                import ${minecraft}client.Minecraft;
                import ${minecraft}client.gui.Font;
                import ${minecraft}client.gui.GuiGraphicsExtractor;
                import ${minecraft}client.gui.screens.Screen;
                import ${minecraft}client.input.CharacterEvent;
                import ${minecraft}client.input.KeyEvent;
                import ${minecraft}client.input.MouseButtonEvent;
                import ${minecraft}network.chat.Component;
                import dev.echo.nativeplatform.loader.NativeLoaderScreenHostModel;
                import dev.echo.nativeplatform.loader.NativeLoaderRenderCoreLayout;
                import ${bootstrapMainClass};
                import ${uiActionRouterClass};
                import ${uiHandlerRegistryClass};
                import java.util.List;
                import java.util.Map;
                import org.lwjgl.glfw.GLFW;

                public final class EchoNativeDashboardScreen extends Screen {
                    private static final int BG = 0xE6071018;
                    private static final int PANEL = 0xD00B1824;
                    private static final int LINE = 0xFF28D7F4;
                    private static final int DIM = 0xFF6C8793;
                    private static final int TEXT = 0xFFE8F8FF;
                    private static final int AMBER = 0xFFFFC857;
                    private static final int GREEN = 0xFF7CFFB2;
                    private final String mode;
                    private final String packId;
                    private final int moduleCount;
                    private final int itemCount;
                    private final int missionCount;
                    private final int regionCount;
                    private final String previousMode;
                    private int ticks;
                    private boolean terminalCommandExecuted;
                    private boolean indexSearchExecuted;
                    private boolean lensScanExecuted;
                    private boolean recoveryActionExecuted;
                    private boolean mouseRouted;
                    private boolean initialFocusRouted;
                    private int selectedIndex;
                    private double settingsHudScale = 1.0D;
                    private boolean settingsSubtitles = true;
                    private double missionProgress = 0.25D;
                    private String missionStatus = "TRACKED";
                    private String missionUpdateLine = "";
                    private final Map<String, Object> hudSource = (Map<String, Object>) EchoNativeAgent5UiHandlerRegistry.dataSources().get("hud");
                    private int hudHealth = ((Number) hudSource.getOrDefault("health", 100)).intValue();
                    private String hudHazard = String.valueOf(hudSource.getOrDefault("hazard", ""));
                    private String hudMission = String.valueOf(hudSource.getOrDefault("mission", ""));
                    private String hudUpdateOutput = "";
                    private String cameraMode = "";
                    private int cameraFov = 72;
                    private String cameraTarget = "";
                    private String cinematicCue = "";
                    private int cinematicFrame = 0;
                    private boolean cinematicLetterbox = false;
                    private String cinematicSubtitle = "";
                    private String cinematicOutput = "";
                    private String focusedControl = "";
                    private String selectedOption = "";
                    private List<Map<String, Object>> notifications = (List<Map<String, Object>>) EchoNativeAgent5UiHandlerRegistry.dataSources().get("notifications");
                    private String terminalBuffer = "";
                    private String indexBuffer = "";
                    private String terminalOutput = "awaiting command input";
                    private String indexOutput = "search field focused";
                    private String lensOutput = "target awaiting scan";
                    private String mainMenuOutput = "";
                    private String holomapOutput = "";
                    private String wikiOutput = "";
                    private String recoveryOutput = "Status: WAITING";
                    private boolean renderCallbackExecuted;
                    private int renderCallbackCount;
                    private String renderCallbackMode = "";
                    private int renderCallbackLineCount;
                    private int renderCallbackWidth;
                    private int renderCallbackHeight;
                    private boolean mainMenuRenderRouted;

                    public EchoNativeDashboardScreen(String mode, String packId, int moduleCount, int itemCount, int missionCount, int regionCount) {
                        this(mode, packId, moduleCount, itemCount, missionCount, regionCount, "WIKI");
                    }

                    private EchoNativeDashboardScreen(String mode, String packId, int moduleCount, int itemCount, int missionCount, int regionCount, String previousMode) {
                        super(Component.literal("ECHO Native " + mode));
                        configureScreenHostModel();
                        this.mode = mode == null ? "TERMINAL" : mode;
                        this.packId = packId == null ? EchoNativeBootstrapMain.nativeProductNamespace() : packId;
                        this.moduleCount = moduleCount;
                        this.itemCount = itemCount;
                        this.missionCount = missionCount;
                        this.regionCount = regionCount;
                        this.previousMode = previousMode == null || previousMode.isBlank() ? "WIKI" : previousMode;
                        if ("TERMINAL".equals(this.mode) || "INDEX".equals(this.mode)) {
                            dev.echo.nativeplatform.loader.NativeLoaderLiveUiInteractionRecorder.clear();
                        }
                        Map<String, Object> initialFocus = EchoNativeAgent5UiActionRouter.routeInitialFocus(this.mode, this.previousMode);
                        this.focusedControl = String.valueOf(initialFocus.get("focusedControl"));
                        this.initialFocusRouted = Boolean.TRUE.equals(initialFocus.get("initialFocusRouted"));
                    }

                    private static void configureScreenHostModel() {
                        NativeLoaderScreenHostModel.configure(new NativeLoaderScreenHostModel.Provider() {
                            @Override
                            public Map<String, Object> dataSources() {
                                return EchoNativeAgent5UiHandlerRegistry.dataSources();
                            }

                            @Override
                            public Map<String, Object> renderSurface(String mode, Map<String, Object> state) {
                                return EchoNativeAgent5UiHandlerRegistry.renderSurface(mode, state);
                            }

                            @Override
                            public String productNamespace() {
                                return EchoNativeBootstrapMain.nativeProductNamespace();
                            }
                        });
                    }

                    @Override
                    public void tick() {
                        super.tick();
                        this.ticks++;
                    }

                    @Override
                    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
                        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
                        Font font = Minecraft.getInstance().font;
                        if ("MAIN_MENU".equals(this.mode) && !this.mainMenuRenderRouted) {
                            dev.echo.nativeplatform.contracts.EchoNativeLoadStatus menuRenderStatus =
                                    dev.echo.nativeplatform.contracts.EchoNativeClientRouteRegistries.get().renderGuiLayer(
                                            "main_menu",
                                            "menu.open",
                                            Map.of(
                                                    "source", "native_loader_generated_main_menu_render",
                                                    "eventType", "generated_main_menu_render",
                                                    "partialTick", partialTick,
                                                    "screenWidth", this.width,
                                                    "screenHeight", this.height,
                                                    "nativeRouteOwner", "EchoNativeClientRouteRegistries",
                                                    "neoForgeEventOwnershipRequired", false
                                            )
                                    );
                            this.mainMenuRenderRouted =
                                    menuRenderStatus == dev.echo.nativeplatform.contracts.EchoNativeLoadStatus.MUTATED;
                        }
                        if (!"MAIN_MENU".equals(this.mode)
                                && !dispatchGeneratedDashboardRoute("dashboard.render", "generated_dashboard_render")) {
                            return;
                        }
                        Map<String, Object> hostModel = NativeLoaderScreenHostModel.render(
                                this.mode,
                                surfaceState(),
                                this.packId,
                                this.moduleCount,
                                this.itemCount,
                                this.missionCount,
                                this.regionCount
                        );
                        List<String> headerLines = (List<String>) hostModel.get("headerLines");
                        List<String> lines = (List<String>) hostModel.get("surfaceLines");
                        this.renderCallbackExecuted = true;
                        this.renderCallbackCount++;
                        this.renderCallbackMode = this.mode;
                        this.renderCallbackLineCount = lines.size();
                        this.renderCallbackWidth = this.width;
                        this.renderCallbackHeight = this.height;
                        Map<String, Object> layout = NativeLoaderRenderCoreLayout.compute(
                                this.width,
                                this.height,
                                headerLines.size(),
                                lines.size()
                        );
                        graphics.fill(0, 0, this.width, this.height, BG);
                        int panelW = ((Number) layout.get("panelW")).intValue();
                        int panelH = ((Number) layout.get("panelH")).intValue();
                        int x = ((Number) layout.get("x")).intValue();
                        int y = ((Number) layout.get("y")).intValue();
                        int headerStartY = ((Number) layout.get("headerStartY")).intValue();
                        int bodyY = ((Number) layout.get("bodyY")).intValue();
                        int footerY = ((Number) layout.get("footerY")).intValue();
                        int bodyLinesRendered = ((Number) layout.get("bodyLinesRendered")).intValue();
                        graphics.fill(x, y, x + panelW, y + panelH, PANEL);
                        graphics.outline(x, y, panelW, panelH, LINE);
                        graphics.fill(x + 1, y + 1, x + panelW - 1, y + 28, 0xAA1A0840);
                        text(graphics, font, String.valueOf(hostModel.get("screenTitle")), x + 14, y + 10, LINE);
                        for (int index = 0; index < headerLines.size(); index++) {
                            text(graphics, font, headerLines.get(index), x + 14, headerStartY + (index * 20), headerColor(index));
                        }
                        for (int index = 0; index < bodyLinesRendered; index++) {
                            text(graphics, font, lines.get(index), x + 14, bodyY + (index * 20), lineColor(index, lines.size()));
                        }
                        text(graphics, font, String.valueOf(hostModel.get("footerLine")), x + 14, footerY, LINE);
                    }

                    @Override
                    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
                        Map<String, Object> click = EchoNativeAgent5UiActionRouter.routeMouseClick(
                                this.mode,
                                this.previousMode,
                                surfaceState()
                        );
                        if (Boolean.TRUE.equals(click.get("handled"))) {
                            if (!dispatchGeneratedDashboardRoute("dashboard.mouse", "generated_dashboard_mouse")) {
                                return false;
                            }
                            this.focusedControl = String.valueOf(click.get("focusedControl"));
                            this.mouseRouted = Boolean.TRUE.equals(click.get("mouseRouted"));
                            if (!applyAction(click)) {
                                return false;
                            }
                            dev.echo.nativeplatform.loader.NativeLoaderLiveUiInteractionRecorder.recordSubmit(this.mode, click);
                            return true;
                        }
                        return false;
                    }

                    @Override
                    public boolean charTyped(CharacterEvent event) {
                        Map<String, Object> result = EchoNativeAgent5UiActionRouter.routeCharacter(
                                this.mode,
                                this.focusedControl,
                                this.terminalBuffer,
                                this.indexBuffer,
                                (char) event.codepoint()
                        );
                        if (Boolean.TRUE.equals(result.get("handled"))) {
                            if (!dispatchGeneratedDashboardRoute("dashboard.character", "generated_dashboard_character")) {
                                return false;
                            }
                            if ("terminalBuffer".equals(result.get("targetBuffer"))) {
                                this.terminalBuffer = String.valueOf(result.get("value"));
                            }
                            if ("indexBuffer".equals(result.get("targetBuffer"))) {
                                this.indexBuffer = String.valueOf(result.get("value"));
                            }
                            dev.echo.nativeplatform.loader.NativeLoaderLiveUiInteractionRecorder.recordCharacter(this.mode, result);
                            return true;
                        }
                        return false;
                    }

                    @Override
                    public boolean keyPressed(KeyEvent event) {
                        int key = event.key();
                        if (key == GLFW.GLFW_KEY_ESCAPE) {
                            if (!dispatchGeneratedDashboardClose()) {
                                return false;
                            }
                            Minecraft.getInstance().setScreen(null);
                            return true;
                        }
                        Map<String, Object> edit = EchoNativeAgent5UiActionRouter.routeEditKey(
                                routeKeyName(key),
                                this.mode,
                                this.focusedControl,
                                this.terminalBuffer,
                                this.indexBuffer
                        );
                        if (Boolean.TRUE.equals(edit.get("handled"))) {
                            if (!dispatchGeneratedDashboardRoute("dashboard.edit", "generated_dashboard_edit")) {
                                return false;
                            }
                            if ("terminalBuffer".equals(edit.get("targetBuffer"))) {
                                this.terminalBuffer = String.valueOf(edit.get("value"));
                            }
                            if ("indexBuffer".equals(edit.get("targetBuffer"))) {
                                this.indexBuffer = String.valueOf(edit.get("value"));
                            }
                            dev.echo.nativeplatform.loader.NativeLoaderLiveUiInteractionRecorder.recordEdit(this.mode, edit);
                            return true;
                        }
                        Map<String, Object> listNavigation = EchoNativeAgent5UiActionRouter.routeListNavigation(
                                routeKeyName(key),
                                this.mode,
                                this.selectedIndex
                        );
                        if (Boolean.TRUE.equals(listNavigation.get("handled"))) {
                            if (!dispatchGeneratedDashboardRoute("dashboard.list_navigation", "generated_dashboard_list_navigation")) {
                                return false;
                            }
                            this.selectedIndex = ((Number) listNavigation.get("selectedIndex")).intValue();
                            this.selectedOption = String.valueOf(listNavigation.get("selectedOption"));
                            this.focusedControl = String.valueOf(listNavigation.get("focusPath"));
                            return true;
                        }
                        Map<String, Object> route = EchoNativeAgent5UiActionRouter.routeKey(routeKeyName(key), this.mode, this.previousMode);
                        if (Boolean.TRUE.equals(route.get("handled"))) {
                            if (!openNativeDashboardScreen(
                                    String.valueOf(route.get("destinationMode")),
                                    String.valueOf(route.get("destinationPreviousMode")),
                                    "generated_route_key"
                            )) {
                                return false;
                            }
                            return true;
                        }
                        if (key == GLFW.GLFW_KEY_ENTER) {
                            if ("MAIN_MENU".equals(this.mode)) {
                                Map<String, Object> mainMenu = EchoNativeAgent5UiActionRouter.routeMainMenuOption(this.selectedOption);
                                if (!Boolean.TRUE.equals(mainMenu.get("handled"))) {
                                    return false;
                                }
                                this.mainMenuOutput = String.valueOf(mainMenu.get("mainMenuOutput"));
                                if (Boolean.TRUE.equals(mainMenu.get("quitRequested"))) {
                                    Minecraft.getInstance().setScreen(null);
                                    return true;
                                }
                                if (!openNativeDashboardScreen(
                                        String.valueOf(mainMenu.get("destinationMode")),
                                        String.valueOf(mainMenu.get("destinationPreviousMode")),
                                        "generated_main_menu_selection"
                                )) {
                                    return false;
                                }
                                return true;
                            }
                            if ("HUD".equals(this.mode)) {
                                Map<String, Object> hud = EchoNativeAgent5UiActionRouter.routeHudUpdate(surfaceState());
                                if (!Boolean.TRUE.equals(hud.get("handled"))) {
                                    return false;
                                }
                                Map<String, Object> cameraCinematic =
                                        EchoNativeAgent5UiActionRouter.routeCameraCinematicFrame(surfaceState());
                                if (!Boolean.TRUE.equals(cameraCinematic.get("handled"))) {
                                    return false;
                                }
                                Map<String, Object> hudMutation = EchoNativeBootstrapMain.executeNativeHudRefreshFromUi(
                                        ((Number) hud.get("hudHealth")).intValue(),
                                        String.valueOf(hud.get("hudHazard")),
                                        String.valueOf(hud.get("hudMission")),
                                        String.valueOf(cameraCinematic.get("cinematicCue"))
                                );
                                if (!runtimeMutationAccepted(hudMutation)) {
                                    this.hudUpdateOutput = "HUD refresh unavailable for active runtime host";
                                    return false;
                                }
                                this.hudHealth = ((Number) hud.get("hudHealth")).intValue();
                                this.hudHazard = String.valueOf(hud.get("hudHazard"));
                                this.hudMission = String.valueOf(hud.get("hudMission"));
                                this.hudUpdateOutput = runtimeFeedback(hudMutation, String.valueOf(hud.get("hudUpdateOutput")));
                                this.cameraMode = String.valueOf(cameraCinematic.get("cameraMode"));
                                this.cameraFov = ((Number) cameraCinematic.get("cameraFov")).intValue();
                                this.cameraTarget = String.valueOf(cameraCinematic.get("cameraTarget"));
                                this.cinematicCue = String.valueOf(cameraCinematic.get("cinematicCue"));
                                this.cinematicFrame = ((Number) cameraCinematic.get("cinematicFrame")).intValue();
                                this.cinematicLetterbox = Boolean.TRUE.equals(cameraCinematic.get("cinematicLetterbox"));
                                this.cinematicSubtitle = String.valueOf(cameraCinematic.get("cinematicSubtitle"));
                                this.cinematicOutput = String.valueOf(cameraCinematic.get("cinematicOutput"));
                                return true;
                            }
                            if ("MISSION_LOG".equals(this.mode)) {
                                Map<String, Object> mission = EchoNativeAgent5UiActionRouter.routeMissionLogUpdate(surfaceState());
                                if (!Boolean.TRUE.equals(mission.get("handled"))) {
                                    return false;
                                }
                                Map<String, Object> missionMutation = EchoNativeBootstrapMain.executeNativeMissionLogUpdateFromUi(
                                        String.valueOf(mission.get("missionId")),
                                        String.valueOf(mission.get("missionTitle")),
                                        String.valueOf(mission.get("missionObjective")),
                                        ((Number) mission.get("missionProgress")).doubleValue(),
                                        String.valueOf(mission.get("missionStatus")),
                                        String.valueOf(mission.get("missionUpdateLine"))
                                );
                                if (!runtimeMutationAccepted(missionMutation)) {
                                    this.missionUpdateLine = "mission update unavailable for active runtime host";
                                    return false;
                                }
                                this.missionProgress = ((Number) mission.get("missionProgress")).doubleValue();
                                this.missionStatus = String.valueOf(mission.get("missionStatus"));
                                this.missionUpdateLine = runtimeFeedback(missionMutation, String.valueOf(mission.get("missionUpdateLine")));
                                return true;
                            }
                            if ("PAUSE".equals(this.mode)) {
                                Map<String, Object> pause = EchoNativeAgent5UiActionRouter.routePauseOption(
                                        this.selectedOption,
                                        this.previousMode
                                );
                                if (!Boolean.TRUE.equals(pause.get("handled"))) {
                                    return false;
                                }
                                if (!openNativeDashboardScreen(
                                        String.valueOf(pause.get("destinationMode")),
                                        String.valueOf(pause.get("destinationPreviousMode")),
                                        "generated_pause_selection"
                                )) {
                                    return false;
                                }
                                return true;
                            }
                            if ("SETTINGS".equals(this.mode)) {
                                Map<String, Object> settings = EchoNativeAgent5UiActionRouter.routeSettingsAdjustment(
                                        this.selectedOption,
                                        this.settingsHudScale,
                                        this.settingsSubtitles
                                );
                                if (!Boolean.TRUE.equals(settings.get("handled"))) {
                                    return false;
                                }
                                if (!dispatchGeneratedDashboardRoute("dashboard.settings", "generated_dashboard_settings")) {
                                    return false;
                                }
                                this.settingsHudScale = ((Number) settings.get("settingsHudScale")).doubleValue();
                                this.settingsSubtitles = Boolean.TRUE.equals(settings.get("settingsSubtitles"));
                                return true;
                            }
                            Map<String, Object> result = EchoNativeAgent5UiActionRouter.activate(this.mode, surfaceState());
                            if (!Boolean.TRUE.equals(result.get("handled"))) {
                                return false;
                            }
                            if (!dispatchGeneratedDashboardRoute("dashboard.submit", "generated_dashboard_submit")) {
                                return false;
                            }
                            if (!applyAction(result)) {
                                return false;
                            }
                            dev.echo.nativeplatform.loader.NativeLoaderLiveUiInteractionRecorder.recordSubmit(this.mode, result);
                            return true;
                        }
                        return super.keyPressed(event);
                    }

                    private boolean openNativeDashboardScreen(String destinationMode, String destinationPreviousMode, String source) {
                        String targetMode = destinationMode == null || destinationMode.isBlank() ? "WIKI" : destinationMode;
                        if (!dispatchNativeOpenRoute(targetMode, source)) {
                            return false;
                        }
                        Minecraft.getInstance().setScreen(new EchoNativeDashboardScreen(
                                targetMode,
                                this.packId,
                                this.moduleCount,
                                this.itemCount,
                                this.missionCount,
                                this.regionCount,
                                destinationPreviousMode
                        ));
                        return true;
                    }

                    private boolean dispatchGeneratedDashboardRoute(String actionId, String eventType) {
                        dev.echo.nativeplatform.contracts.EchoNativeLoadStatus status =
                                dev.echo.nativeplatform.contracts.EchoNativeClientRouteRegistries.get().overlayInput(
                                        "native_dashboard",
                                        actionId,
                                        Map.of(
                                                "source", "native_loader_generated_dashboard",
                                                "eventType", eventType,
                                                "mode", this.mode,
                                                "focusedControl", this.focusedControl,
                                                "selectedIndex", this.selectedIndex,
                                                "selectedOption", this.selectedOption,
                                                "nativeRouteOwner", "EchoNativeClientRouteRegistries",
                                                "neoForgeEventOwnershipRequired", false
                                        )
                                );
                        return status == dev.echo.nativeplatform.contracts.EchoNativeLoadStatus.MUTATED;
                    }

                    private boolean dispatchGeneratedDashboardClose() {
                        dev.echo.nativeplatform.contracts.EchoNativeLoadStatus status =
                                dev.echo.nativeplatform.contracts.EchoNativeClientRouteRegistries.get().closeSurface(
                                        "native_dashboard",
                                        "dashboard.close",
                                        Map.of(
                                                "source", "native_loader_generated_dashboard",
                                                "eventType", "generated_dashboard_close",
                                                "mode", this.mode,
                                                "nativeRouteOwner", "EchoNativeClientRouteRegistries",
                                                "neoForgeEventOwnershipRequired", false
                                        )
                                );
                        return status == dev.echo.nativeplatform.contracts.EchoNativeLoadStatus.MUTATED;
                    }

                    private boolean dispatchNativeOpenRoute(String destinationMode, String source) {
                        String mode = destinationMode == null ? "" : destinationMode.toUpperCase(java.util.Locale.ROOT);
                        java.util.Map<String, Object> metadata = Map.of(
                                "source", source == null || source.isBlank() ? "native_loader_generated_dashboard_navigation" : source,
                                "eventType", "generated_dashboard_navigation",
                                "fromMode", this.mode,
                                "destinationMode", mode,
                                                "nativeRouteOwner", "EchoNativeClientRouteRegistries",
                                                "neoForgeEventOwnershipRequired", false
                                        );
                        dev.echo.nativeplatform.contracts.EchoNativeLoadStatus status = switch (mode) {
                            case "TERMINAL" -> dev.echo.nativeplatform.contracts.EchoNativeClientRouteRegistries.get()
                                    .openSurface("terminal", "terminal.open", metadata);
                            case "INDEX" -> dev.echo.nativeplatform.contracts.EchoNativeClientRouteRegistries.get()
                                    .openSurface("index", "index.catalog", metadata);
                            case "LENS" -> dev.echo.nativeplatform.contracts.EchoNativeClientRouteRegistries.get()
                                    .dispatchStatus("lens", "lens.deep_scan", metadata);
                            case "HOLOMAP" -> dev.echo.nativeplatform.contracts.EchoNativeClientRouteRegistries.get()
                                    .openSurface("holomap", "holomap.open", metadata);
                            case "HUD" -> dev.echo.nativeplatform.contracts.EchoNativeClientRouteRegistries.get()
                                    .renderHudLayer("hud", "hud.render", metadata);
                            case "MAIN_MENU" -> dev.echo.nativeplatform.contracts.EchoNativeClientRouteRegistries.get()
                                    .openSurface("main_menu", "menu.open", metadata);
                            case "SIGNALOS" -> dev.echo.nativeplatform.contracts.EchoNativeClientRouteRegistries.get()
                                    .openSurface("signalos", "signalos.terminal", metadata);
                            default -> dev.echo.nativeplatform.contracts.EchoNativeLoadStatus.MUTATED;
                        };
                        if (status == dev.echo.nativeplatform.contracts.EchoNativeLoadStatus.MUTATED) {
                            return true;
                        }
                        java.util.Map<String, Object> fallbackMetadata = new java.util.LinkedHashMap<>(metadata);
                        fallbackMetadata.put("productRouteStatus", status.name());
                        fallbackMetadata.put("dashboardNavigationFallback", true);
                        return dev.echo.nativeplatform.contracts.EchoNativeClientRouteRegistries.get().overlayInput(
                                "native_dashboard",
                                "dashboard.navigate",
                                Map.copyOf(fallbackMetadata)
                        ) == dev.echo.nativeplatform.contracts.EchoNativeLoadStatus.MUTATED;
                    }

                    private boolean applyAction(Map<String, Object> result) {
                        if ("terminalOutput".equals(result.get("outputKey"))) {
                            this.terminalOutput = String.valueOf(result.get("output"));
                        }
                        if ("indexOutput".equals(result.get("outputKey"))) {
                            this.indexOutput = String.valueOf(result.get("output"));
                        }
                        if ("lensOutput".equals(result.get("outputKey"))) {
                            this.lensOutput = String.valueOf(result.get("output"));
                        }
                        if ("recoveryOutput".equals(result.get("outputKey"))) {
                            this.recoveryOutput = String.valueOf(result.get("output"));
                        }
                        if ("terminalCommandExecuted".equals(result.get("executedKey"))) {
                            Map<String, Object> terminalMutation = EchoNativeBootstrapMain.executeNativeTerminalCommandFromUi(
                                    String.valueOf(result.getOrDefault("terminalCommand", this.terminalBuffer)),
                                    this.terminalOutput
                            );
                            this.terminalCommandExecuted = runtimeMutationAccepted(terminalMutation);
                            if (!this.terminalCommandExecuted) {
                                this.terminalOutput = "terminal command unavailable for active runtime host";
                                return false;
                            }
                            this.terminalOutput = runtimeFeedback(terminalMutation, this.terminalOutput);
                        }
                        if ("indexSearchExecuted".equals(result.get("executedKey"))) {
                            Map<String, Object> indexMutation = EchoNativeBootstrapMain.executeNativeIndexSearchFromUi(
                                    String.valueOf(result.getOrDefault("indexQuery", this.indexBuffer)),
                                    this.indexOutput
                            );
                            this.indexSearchExecuted = runtimeMutationAccepted(indexMutation);
                            if (!this.indexSearchExecuted) {
                                this.indexOutput = "index search unavailable for active runtime host";
                                return false;
                            }
                            this.indexOutput = runtimeFeedback(indexMutation, this.indexOutput);
                        }
                        if ("lensScanExecuted".equals(result.get("executedKey"))) {
                            Map<String, Object> scannerMutation = EchoNativeBootstrapMain.useNativeScannerFromUiEvidence();
                            this.lensScanExecuted = runtimeMutationAccepted(scannerMutation);
                            if (!this.lensScanExecuted) {
                                this.lensOutput = "scanner unavailable for active runtime host";
                                return false;
                            }
                            this.lensOutput = runtimeFeedback(scannerMutation, this.lensOutput);
                        }
                        if ("recoveryActionExecuted".equals(result.get("executedKey"))) {
                            Object grantItemCountValue = result.get("grantItemCount");
                            int grantItemCount = grantItemCountValue instanceof Number number ? number.intValue() : 1;
                            String grantItemId = String.valueOf(result.getOrDefault(
                                    "grantItemId",
                                    EchoNativeBootstrapMain.nativeRecoveryItemId()
                            ));
                            Map<String, Object> recoveryMutation = EchoNativeBootstrapMain.grantNativeItemFromUiEvidence(
                                    grantItemId,
                                    grantItemCount
                            );
                            this.recoveryActionExecuted = runtimeMutationAccepted(recoveryMutation);
                            if (!this.recoveryActionExecuted) {
                                this.recoveryOutput = "recovery item grant unavailable for active runtime host";
                                return false;
                            }
                            this.recoveryOutput = runtimeFeedback(recoveryMutation, this.recoveryOutput);
                        }
                        return true;
                    }

                    private static boolean runtimeMutationAccepted(Map<String, Object> mutation) {
                        return Boolean.TRUE.equals(mutation.get("mutated"))
                                && Boolean.TRUE.equals(mutation.get("saveTouched"))
                                && Boolean.TRUE.equals(mutation.get("missionUpdated"))
                                && Boolean.TRUE.equals(mutation.get("feedbackEmitted"));
                    }

                    private static String runtimeFeedback(Map<String, Object> mutation, String fallback) {
                        String message = String.valueOf(mutation.getOrDefault("message", ""));
                        String status = String.valueOf(mutation.getOrDefault("status", ""));
                        Object snapshotValue = mutation.get("resultSnapshot");
                        boolean saveTouched = Boolean.TRUE.equals(mutation.get("saveTouched"));
                        boolean feedbackEmitted = Boolean.TRUE.equals(mutation.get("feedbackEmitted"));
                        boolean missionUpdated = Boolean.TRUE.equals(mutation.get("missionUpdated"));
                        if (!message.isBlank() && !"null".equals(message)) {
                            String evidence = message + " [" + status
                                    + "; save=" + saveTouched
                                    + "; feedback=" + feedbackEmitted
                                    + "; mission=" + missionUpdated + "]";
                            if (fallback != null && !fallback.isBlank() && !fallback.equals(message)) {
                                return evidence + " :: " + fallback;
                            }
                            return evidence;
                        }
                        if (snapshotValue instanceof Map<?, ?> snapshot && !snapshot.isEmpty()) {
                            return "runtime mutation accepted [" + status
                                    + "; save=" + saveTouched
                                    + "; feedback=" + feedbackEmitted
                                    + "; mission=" + missionUpdated + "]";
                        }
                        return fallback == null || fallback.isBlank() ? "runtime mutation accepted" : fallback;
                    }

                    private static String routeKeyName(int key) {
                        if (key == GLFW.GLFW_KEY_ESCAPE) {
                            return "ESCAPE";
                        }
                        if (key == GLFW.GLFW_KEY_BACKSPACE) {
                            return "BACKSPACE";
                        }
                        if (key == GLFW.GLFW_KEY_UP) {
                            return "UP";
                        }
                        if (key == GLFW.GLFW_KEY_DOWN) {
                            return "DOWN";
                        }
                        if (key == GLFW.GLFW_KEY_M) {
                            return "M";
                        }
                        if (key == GLFW.GLFW_KEY_N) {
                            return "N";
                        }
                        if (key == GLFW.GLFW_KEY_G) {
                            return "G";
                        }
                        if (key == GLFW.GLFW_KEY_J) {
                            return "J";
                        }
                        if (key == GLFW.GLFW_KEY_K) {
                            return "K";
                        }
                        if (key == GLFW.GLFW_KEY_R) {
                            return "R";
                        }
                        if (key == GLFW.GLFW_KEY_U) {
                            return "U";
                        }
                        if (key == GLFW.GLFW_KEY_B) {
                            return "B";
                        }
                        if (key == GLFW.GLFW_KEY_LEFT_ALT) {
                            return "LEFT_ALT";
                        }
                        if (key == GLFW.GLFW_KEY_RIGHT_BRACKET) {
                            return "RIGHT_BRACKET";
                        }
                        if (key == GLFW.GLFW_KEY_LEFT_BRACKET) {
                            return "LEFT_BRACKET";
                        }
                        if (key == GLFW.GLFW_KEY_BACKSLASH) {
                            return "BACKSLASH";
                        }
                        if (key == GLFW.GLFW_KEY_X) {
                            return "X";
                        }
                        if (key == GLFW.GLFW_KEY_C) {
                            return "C";
                        }
                        if (key == GLFW.GLFW_KEY_Y) {
                            return "Y";
                        }
                        if (key == GLFW.GLFW_KEY_Z) {
                            return "Z";
                        }
                        return "";
                    }

                    private Map<String, Object> surfaceState() {
                        Map<String, Object> gameplayContext = EchoNativeBootstrapMain.nativeGameplaySurfaceContextForMode(this.mode);
                        return Map.ofEntries(
                                Map.entry("mode", this.mode),
                                Map.entry("previousMode", this.previousMode),
                                Map.entry("focusedControl", this.focusedControl),
                                Map.entry("mouseRouted", this.mouseRouted),
                                Map.entry("initialFocusRouted", this.initialFocusRouted),
                                Map.entry("selectedIndex", this.selectedIndex),
                                Map.entry("selectedOption", this.selectedOption),
                                Map.entry("settingsHudScale", this.settingsHudScale),
                                Map.entry("settingsSubtitles", this.settingsSubtitles),
                                Map.entry("missionProgress", this.missionProgress),
                                Map.entry("missionStatus", this.missionStatus),
                                Map.entry("missionUpdateLine", this.missionUpdateLine),
                                Map.entry("hudHealth", this.hudHealth),
                                Map.entry("hudHazard", this.hudHazard),
                                Map.entry("hudMission", this.hudMission),
                                Map.entry("hudUpdateOutput", this.hudUpdateOutput),
                                Map.entry("cameraMode", this.cameraMode),
                                Map.entry("cameraFov", this.cameraFov),
                                Map.entry("cameraTarget", this.cameraTarget),
                                Map.entry("cinematicCue", this.cinematicCue),
                                Map.entry("cinematicFrame", this.cinematicFrame),
                                Map.entry("cinematicLetterbox", this.cinematicLetterbox),
                                Map.entry("cinematicSubtitle", this.cinematicSubtitle),
                                Map.entry("cinematicOutput", this.cinematicOutput),
                                Map.entry("notifications", this.notifications),
                                Map.entry("terminalBuffer", this.terminalBuffer),
                                Map.entry("indexBuffer", this.indexBuffer),
                                Map.entry("terminalOutput", this.terminalOutput),
                                Map.entry("indexOutput", this.indexOutput),
                                Map.entry("lensOutput", this.lensOutput),
                                Map.entry("mainMenuOutput", this.mainMenuOutput),
                                Map.entry("holomapOutput", this.holomapOutput),
                                Map.entry("wikiOutput", this.wikiOutput),
                                Map.entry("recoveryOutput", this.recoveryOutput),
                                Map.entry("terminalCommandExecuted", this.terminalCommandExecuted),
                                Map.entry("indexSearchExecuted", this.indexSearchExecuted),
                                Map.entry("lensScanExecuted", this.lensScanExecuted),
                                Map.entry("recoveryActionExecuted", this.recoveryActionExecuted),
                                Map.entry("runtimeHostActionGateActive", EchoNativeBootstrapMain.nativeUiHostActionGateActive()),
                                Map.entry("runtimeSupportedActions", EchoNativeBootstrapMain.nativeUiSupportedActionIds()),
                                Map.entry("gameplaySurfaceContext", gameplayContext),
                                Map.entry("machineBlockId", String.valueOf(gameplayContext.getOrDefault("blockId", ""))),
                                Map.entry("machinePosition", String.valueOf(gameplayContext.getOrDefault("position", ""))),
                                Map.entry("machineId", String.valueOf(gameplayContext.getOrDefault("machineId", ""))),
                                Map.entry("machineModuleId", String.valueOf(gameplayContext.getOrDefault("moduleId", ""))),
                                Map.entry("renderCallbackExecuted", this.renderCallbackExecuted),
                                Map.entry("renderCallbackCount", this.renderCallbackCount),
                                Map.entry("renderCallbackMode", this.renderCallbackMode),
                                Map.entry("renderCallbackLineCount", this.renderCallbackLineCount),
                                Map.entry("renderCallbackWidth", this.renderCallbackWidth),
                                Map.entry("renderCallbackHeight", this.renderCallbackHeight)
                        );
                    }

                    private static int lineColor(int index, int total) {
                        if (index == 0) {
                            return AMBER;
                        }
                        if (index == total - 1) {
                            return GREEN;
                        }
                        return TEXT;
                    }

                    private static int headerColor(int index) {
                        if (index == 1 || index == 4) {
                            return GREEN;
                        }
                        return TEXT;
                    }

                    private static void text(GuiGraphicsExtractor graphics, Font font, String value, int x, int y, int color) {
                        graphics.text(font, fit(font, value, Math.max(80, Minecraft.getInstance().getWindow().getGuiScaledWidth() - x - 28)), x, y, color, false);
                    }

                    private static String fit(Font font, String value, int width) {
                        if (font.width(value) <= width) {
                            return value;
                        }
                        String suffix = "...";
                        int max = Math.max(0, width - font.width(suffix));
                        return font.plainSubstrByWidth(value, max) + suffix;
                    }
                }
                """
                .replace("${minecraft}", minecraftPackage)
                .replace("${bootstrapMainClass}", requiredClassName(bootstrap.bootstrapMainClassName(), "bootstrapMainClassName"))
                .replace("${uiActionRouterClass}", requiredClassName(bootstrap.uiActionRouterClassName(), "uiActionRouterClassName"))
                .replace("${uiHandlerRegistryClass}", requiredClassName(bootstrap.uiHandlerRegistryClassName(), "uiHandlerRegistryClassName"));
    }


    public static String guiProjectionSource() {
        String minecraftPackage = "net." + "minecraft.";
        return """
                package dev.echo.nativeplatform.generated;

                import ${minecraft}client.DeltaTracker;
                import ${minecraft}client.Minecraft;
                import ${minecraft}client.gui.Gui;
                import ${minecraft}client.gui.GuiGraphicsExtractor;
                import dev.echo.nativeplatform.loader.NativeLoaderLiveHudRenderBridge;

                public final class EchoNativeGuiProjection extends Gui {
                    public EchoNativeGuiProjection(Minecraft minecraft) {
                        super(minecraft);
                    }

                    @Override
                    public void extractRenderState(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
                        super.extractRenderState(graphics, deltaTracker);
                        NativeLoaderLiveHudRenderBridge.render(graphics, deltaTracker);
                    }
                }
                """.replace("${minecraft}", minecraftPackage);
    }

    public static String loadingOverlayProjectionSource() {
        String minecraftPackage = "net." + "minecraft.";
        return """
                package dev.echo.nativeplatform.generated;

                import ${minecraft}client.Minecraft;
                import ${minecraft}client.gui.GuiGraphicsExtractor;
                import ${minecraft}client.gui.screens.Overlay;
                import dev.echo.nativeplatform.loader.NativeLoaderLiveLoadingRenderBridge;

                public final class EchoNativeLoadingOverlayProjection extends Overlay {
                    private final Minecraft minecraft;
                    private int ticks;
                    private boolean completed;

                    public EchoNativeLoadingOverlayProjection(Minecraft minecraft) {
                        this.minecraft = minecraft;
                    }

                    @Override
                    public void tick() {
                        this.ticks++;
                        if (this.ticks > 88 && !this.completed) {
                            dev.echo.nativeplatform.contracts.EchoNativeLoadStatus status =
                                    dev.echo.nativeplatform.contracts.EchoNativeClientRouteRegistries.get().renderGuiLayer(
                                            "loading_screen",
                                            "loading.complete",
                                            java.util.Map.of(
                                                    "source", "native_loader_generated_loading_overlay",
                                                    "eventType", "generated_loading_overlay_complete",
                                                    "tick", this.ticks,
                                                    "progress", 1.0D,
                                                    "phase", "complete",
                                                    "nativeRouteOwner", "EchoNativeClientRouteRegistries",
                                                    "neoForgeEventOwnershipRequired", false
                                            )
                                    );
                            if (status != dev.echo.nativeplatform.contracts.EchoNativeLoadStatus.MUTATED) {
                                return;
                            }
                            this.completed = true;
                            this.minecraft.setOverlay(null);
                        }
                    }

                    @Override
                    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
                        NativeLoaderLiveLoadingRenderBridge.render(graphics, partialTick, this.ticks);
                    }

                    @Override
                    public boolean isPauseScreen() {
                        return false;
                    }
                }
                """.replace("${minecraft}", minecraftPackage);
    }
}
