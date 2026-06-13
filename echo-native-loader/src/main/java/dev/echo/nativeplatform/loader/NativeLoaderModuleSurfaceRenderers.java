package dev.echo.nativeplatform.loader;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class NativeLoaderModuleSurfaceRenderers {
    public static final String SERVICE_ID = "echo.native.module_surface_renderers";

    private NativeLoaderModuleSurfaceRenderers() {
    }

    public static Map<String, Object> renderTerminal(Map<String, Object> state, Map<String, Object> dataSources) {
        return renderTerminal(state, dataSources, Context.empty());
    }

    public static Map<String, Object> renderTerminal(
            Map<String, Object> state,
            Map<String, Object> dataSources,
            Context context
    ) {
        return EchoNativeTerminalSurfaceRenderer.render(state, dataSources, context);
    }

    public static Map<String, Object> renderIndex(Map<String, Object> state, Map<String, Object> dataSources) {
        return renderIndex(state, dataSources, Context.empty());
    }

    public static Map<String, Object> renderIndex(
            Map<String, Object> state,
            Map<String, Object> dataSources,
            Context context
    ) {
        return EchoNativeIndexSurfaceRenderer.render(state, dataSources, context);
    }

    public static Map<String, Object> renderLens(Map<String, Object> state, Map<String, Object> dataSources) {
        return renderLens(state, dataSources, Context.empty());
    }

    public static Map<String, Object> renderLens(
            Map<String, Object> state,
            Map<String, Object> dataSources,
            Context context
    ) {
        return EchoNativeLensSurfaceRenderer.render(state, dataSources);
    }

    public static Map<String, Object> renderHolomap(Map<String, Object> state, Map<String, Object> dataSources) {
        return renderHolomap(state, dataSources, Context.empty());
    }

    public static Map<String, Object> renderHolomap(
            Map<String, Object> state,
            Map<String, Object> dataSources,
            Context context
    ) {
        return EchoNativeHolomapSurfaceRenderer.render(state, dataSources, context == null ? Context.empty() : context);
    }

    public static Map<String, Object> renderWiki(Map<String, Object> state, Map<String, Object> dataSources) {
        return renderWiki(state, dataSources, Context.empty());
    }

    public static Map<String, Object> renderWiki(
            Map<String, Object> state,
            Map<String, Object> dataSources,
            Context context
    ) {
        return EchoNativeWikiSurfaceRenderer.render(state, dataSources, context == null ? Context.empty() : context);
    }

    public static Map<String, Object> renderSignalos(Map<String, Object> state, Map<String, Object> dataSources) {
        return renderSignalos(state, dataSources, Context.empty());
    }

    public static Map<String, Object> renderSignalos(
            Map<String, Object> state,
            Map<String, Object> dataSources,
            Context context
    ) {
        return EchoNativeSignalOsSurfaceRenderer.render(state, dataSources);
    }

    public static Map<String, Object> renderProductActionSurface(Map<String, Object> state, Map<String, Object> dataSources) {
        return renderProductActionSurface(state, dataSources, Context.empty());
    }

    public static Map<String, Object> renderProductActionSurface(
            Map<String, Object> state,
            Map<String, Object> dataSources,
            Context context
    ) {
        return EchoNativeProductActionSurfaceRenderer.render(state, dataSources);
    }

    public static Map<String, Object> renderMachine(Map<String, Object> state, Map<String, Object> dataSources) {
        return renderMachine(state, dataSources, Context.empty());
    }

    public static Map<String, Object> renderMachine(
            Map<String, Object> state,
            Map<String, Object> dataSources,
            Context context
    ) {
        return EchoNativeMachineSurfaceRenderer.render(state, dataSources, context == null ? Context.empty() : context);
    }

    public static Map<String, Object> renderMissionLog(Map<String, Object> state, Map<String, Object> dataSources) {
        return renderMissionLog(state, dataSources, Context.empty());
    }

    public static Map<String, Object> renderMissionLog(
            Map<String, Object> state,
            Map<String, Object> dataSources,
            Context context
    ) {
        return EchoNativeMissionLogSurfaceRenderer.render(state, dataSources);
    }

    public static Map<String, Object> renderSettings(Map<String, Object> state, Map<String, Object> dataSources) {
        return renderSettings(state, dataSources, Context.empty());
    }

    public static Map<String, Object> renderSettings(
            Map<String, Object> state,
            Map<String, Object> dataSources,
            Context context
    ) {
        return EchoNativeSettingsSurfaceRenderer.render(state, dataSources);
    }

    public static Map<String, Object> renderPause(Map<String, Object> state, Map<String, Object> dataSources) {
        return renderPause(state, dataSources, Context.empty());
    }

    public static Map<String, Object> renderPause(
            Map<String, Object> state,
            Map<String, Object> dataSources,
            Context context
    ) {
        return EchoNativePauseSurfaceRenderer.render(state, dataSources);
    }

    public static Map<String, Object> renderRecovery(Map<String, Object> state, Map<String, Object> dataSources) {
        return renderRecovery(state, dataSources, Context.empty());
    }

    public static Map<String, Object> renderRecovery(
            Map<String, Object> state,
            Map<String, Object> dataSources,
            Context context
    ) {
        return EchoNativeRecoverySurfaceRenderer.render(state, dataSources);
    }

    public static Map<String, Object> renderMainMenu(Map<String, Object> state, Map<String, Object> dataSources) {
        return renderMainMenu(state, dataSources, Context.empty());
    }

    public static Map<String, Object> renderMainMenu(
            Map<String, Object> state,
            Map<String, Object> dataSources,
            Context context
    ) {
        return EchoNativeMainMenuSurfaceRenderer.render(state, dataSources);
    }

    public static Map<String, Object> renderWorldSetup(Map<String, Object> state, Map<String, Object> dataSources) {
        return renderWorldSetup(state, dataSources, Context.empty());
    }

    public static Map<String, Object> renderWorldSetup(
            Map<String, Object> state,
            Map<String, Object> dataSources,
            Context context
    ) {
        return EchoNativeWorldSetupSurfaceRenderer.render(state, dataSources);
    }

    public static Map<String, Object> renderHud(Map<String, Object> state, Map<String, Object> dataSources) {
        return renderHud(state, dataSources, Context.empty());
    }

    public static Map<String, Object> renderHud(
            Map<String, Object> state,
            Map<String, Object> dataSources,
            Context context
    ) {
        return EchoNativeHudSurfaceRenderer.render(state, dataSources);
    }

    static final class EchoNativeTerminalSurfaceRenderer {
        private EchoNativeTerminalSurfaceRenderer() {
        }

        static Map<String, Object> render(Map<String, Object> state, Map<String, Object> dataSources, Context context) {
            Map<String, Object> terminal = object(dataSources.get("terminal"));
            String focusPath = "terminal:input";
            ArrayList<String> lines = new ArrayList<>();
            lines.add("Terminal: " + terminal.get("title") + "    Source: " + terminal.get("sourcePath"));
            lines.add("Command: " + terminal.get("command") + "    Prompt: " + terminal.get("prompt")
                    + "    Pages: " + terminal.get("pageCount"));
            if (!hostSupportsAny(state, "native.ui.terminal_command")) {
                lines.add("Focus: terminal command unavailable for active runtime host");
                lines.add("Status: awaiting supported runtime save data");
                return moduleModel("echoterminal", EchoNativeTerminalSurfaceRenderer.class.getSimpleName(),
                        "terminal:surface", lines);
            }
            lines.add("Focus: " + focusLabel(focusPath, state)
                    + "    Input: " + typedOrPlaceholder(string(state, "terminalBuffer", "")));
            boolean executed = bool(state, "terminalCommandExecuted");
            String output = string(state, "terminalOutput", "awaiting command input");
            lines.add(executed ? terminal.get("command") + " -> " + output : output);
            addStringRows(lines, "Step", strings(terminal.get("nextSteps")), 1);
            addObjectRows(lines, "Page", objects(terminal.get("pages")), 2, "title", "summary");
            addObjectRows(lines, "Panel", objects(terminal.get("panels")), 1, "title", "body");
            return moduleModel("echoterminal", EchoNativeTerminalSurfaceRenderer.class.getSimpleName(), focusPath, lines);
        }
    }

    static final class EchoNativeIndexSurfaceRenderer {
        private EchoNativeIndexSurfaceRenderer() {
        }

        static Map<String, Object> render(Map<String, Object> state, Map<String, Object> dataSources, Context context) {
            Map<String, Object> index = object(dataSources.get("index"));
            String focusPath = "index:search";
            ArrayList<String> lines = new ArrayList<>();
            lines.add("Index: " + index.get("title") + "    Entries: " + index.get("recordCount"));
            lines.add("Source: " + index.get("sourcePath"));
            if (!hostSupportsAny(state, "native.ui.index_search")) {
                lines.add("Focus: Index search unavailable for active runtime host");
                lines.add("Status: awaiting supported runtime events");
                return moduleModel("echoindex", EchoNativeIndexSurfaceRenderer.class.getSimpleName(),
                        "index:surface", lines);
            }
            lines.add("Focus: " + focusLabel(focusPath, state)
                    + "    Query: " + typedOrPlaceholder(string(state, "indexBuffer", "")));
            addObjectRows(lines, "Entry", objects(index.get("entries")), 3, "title", "summary");
            boolean executed = bool(state, "indexSearchExecuted");
            String output = string(state, "indexOutput", "search field focused");
            lines.add(executed ? index.get("query") + " -> " + output : output);
            return moduleModel("echoindex", EchoNativeIndexSurfaceRenderer.class.getSimpleName(), focusPath, lines);
        }
    }

    static final class EchoNativeLensSurfaceRenderer {
        private EchoNativeLensSurfaceRenderer() {
        }

        static Map<String, Object> render(Map<String, Object> state, Map<String, Object> dataSources) {
            Map<String, Object> lens = object(dataSources.get("lens"));
            String focusPath = "lens:scan";
            ArrayList<String> lines = new ArrayList<>();
            lines.add("Lens: " + lens.get("title") + "    Target: " + lens.get("target")
                    + "    Rows: " + lens.get("rowCount") + "    Profiles: " + lens.get("recordCount"));
            lines.add("Profile source: " + lens.get("sourcePath"));
            if (!hostSupportsAny(state, "player.scanner_used", "native.ui.use_scanner")) {
                lines.add("Focus: scanner unavailable for active runtime host");
                lines.add("target awaiting supported runtime scanner");
                return moduleModel("echolens", EchoNativeLensSurfaceRenderer.class.getSimpleName(), "lens:surface", lines);
            }
            lines.add("Focus: " + focusLabel(focusPath, state));
            for (Map<String, Object> row : firstObjects(lens.get("rows"), 3)) {
                lines.add("Scan: " + stringValue(row, "target", "unknown")
                        + "    Risk: " + stringValue(row, "riskLabel", "unknown"));
                lines.add("Lens text: " + stringValue(row, "text", ""));
            }
            boolean executed = bool(state, "lensScanExecuted");
            String output = string(state, "lensOutput", "target awaiting scan");
            lines.add(executed ? "scan locked -> " + output : output);
            return moduleModel("echolens", EchoNativeLensSurfaceRenderer.class.getSimpleName(), focusPath, lines);
        }
    }

    static final class EchoNativeHolomapSurfaceRenderer {
        private EchoNativeHolomapSurfaceRenderer() {
        }

        static Map<String, Object> render(Map<String, Object> state, Map<String, Object> dataSources, Context context) {
        context = context == null ? Context.empty() : context;
        Map<String, Object> holomap = object(dataSources.get("holomap"));
        ArrayList<String> lines = new ArrayList<>();
        String output = string(state, "holomapOutput", "");
        if (output.isBlank()) {
            output = String.valueOf(context.openHolomap(
                    String.valueOf(holomap.get("layer")),
                    String.valueOf(holomap.get("marker"))
            ).get("output"));
        }
        lines.add("HoloMap: " + holomap.get("layerName") + "    Layer: " + holomap.get("layer"));
        lines.add("Layers: " + holomap.get("layerCount") + "    Markers: " + holomap.get("markerCount")
                + "    Source: " + holomap.get("sourcePath"));
        lines.add("Marker: " + holomap.get("markerLabel") + " [" + holomap.get("marker") + "]");
        lines.add("Waypoint focus: " + holomap.get("focus"));
        addObjectRows(lines, "Route", objects(holomap.get("markers")), 3, "label", "description");
        lines.add(output);
            return moduleModel("echoholomap", EchoNativeHolomapSurfaceRenderer.class.getSimpleName(), "holomap:surface", lines);
        }
    }

    static final class EchoNativeWikiSurfaceRenderer {
        private EchoNativeWikiSurfaceRenderer() {
        }

        static Map<String, Object> render(Map<String, Object> state, Map<String, Object> dataSources, Context context) {
        Map<String, Object> wiki = object(dataSources.get("wiki"));
        ArrayList<String> lines = new ArrayList<>();
        String output = string(state, "wikiOutput", "");
        if (output.isBlank()) {
            output = String.valueOf(context.openWiki(
                    String.valueOf(wiki.get("guide")),
                    String.valueOf(wiki.get("page"))
            ).get("output"));
        }
        lines.add("Wiki: " + wiki.get("page") + "    Category: " + wiki.get("guide")
                + "    Articles: " + wiki.get("articleCount") + "    Blocks: " + wiki.get("blockCount"));
        lines.add("Source: " + wiki.get("sourcePath"));
        lines.add(String.valueOf(wiki.get("summary")));
        addObjectRows(lines, "Article", objects(wiki.get("articles")), 3, "title", "summary");
        addObjectRows(lines, "Section", objects(wiki.get("blocks")), 3, "title", "body");
        addStringRows(lines, "Related item", strings(wiki.get("relatedItems")), 2);
        lines.add(output);
            return moduleModel("echowiki", EchoNativeWikiSurfaceRenderer.class.getSimpleName(), "wiki:surface", lines);
        }
    }

    static final class EchoNativeSignalOsSurfaceRenderer {
        private EchoNativeSignalOsSurfaceRenderer() {
        }

        static Map<String, Object> render(Map<String, Object> state, Map<String, Object> dataSources) {
            Map<String, Object> signalos = object(dataSources.get("signalos"));
            ArrayList<String> lines = new ArrayList<>();
            lines.add("SignalOS: " + signalos.get("title") + "    Status: " + signalos.get("status"));
            lines.add("Transport: " + signalos.get("transport"));
            lines.add("Packet: " + signalos.get("packet"));
            lines.add("Records: " + signalos.get("recordCount") + "    Source: " + signalos.get("sourcePath"));
            addStringRows(lines, "Page", strings(signalos.get("pages")), 2);
            addObjectRows(lines, "Record", objects(signalos.get("records")), 2, "title", "summary");
            return moduleModel("signalos", EchoNativeSignalOsSurfaceRenderer.class.getSimpleName(),
                    "signalos:terminal", lines);
        }
    }

    static final class EchoNativeProductActionSurfaceRenderer {
        private EchoNativeProductActionSurfaceRenderer() {
        }

        static Map<String, Object> render(Map<String, Object> state, Map<String, Object> dataSources) {
            Map<String, Object> surface = object(dataSources.get("productActionSurface"));
            ArrayList<String> lines = new ArrayList<>();
            lines.add("Product Action: " + surface.get("title"));
            lines.add("Transport: " + surface.get("transport") + "    Packet: " + surface.get("packet"));
            lines.add("Keys: " + String.join("/", strings(surface.get("keys")))
                    + "    Commands: " + String.join("/", strings(surface.get("commands"))));
            lines.add("Source: " + surface.get("sourcePath") + "    Route: " + surface.get("intelRoute"));
            return moduleModel(String.valueOf(surface.get("canonicalId")),
                    EchoNativeProductActionSurfaceRenderer.class.getSimpleName(),
                    String.valueOf(surface.get("surface")) + ":commands", lines);
        }
    }

    static final class EchoNativeMachineSurfaceRenderer {
        private EchoNativeMachineSurfaceRenderer() {
        }

        static Map<String, Object> render(Map<String, Object> state, Map<String, Object> dataSources, Context loaderContext) {
            Map<String, Object> machine = object(dataSources.get("machine"));
            Map<String, Object> surfaceContext = object(state.get("gameplaySurfaceContext"));
            String blockId = firstNonBlank(
                    string(state, "machineBlockId", ""),
                    stringValue(surfaceContext, "blockId", ""),
                    stringValue(surfaceContext, "contentId", ""));
            String machineId = firstNonBlank(
                    string(state, "machineId", ""),
                    stringValue(surfaceContext, "machineId", ""),
                    blockId);
            String moduleId = firstNonBlank(
                    string(state, "machineModuleId", ""),
                    stringValue(surfaceContext, "moduleId", ""),
                    namespaceOf(blockId),
                    loaderContext.productNamespace());
            String position = firstNonBlank(
                    string(state, "machinePosition", ""),
                    stringValue(surfaceContext, "position", ""));
            ArrayList<String> lines = new ArrayList<>();
            lines.add("Block: " + (blockId.isBlank() ? "unknown" : blockId)
                    + (position.isBlank() ? "" : "    Pos: " + position));
            lines.add("Machine: " + machineLabel(machineId, moduleId)
                    + "    Module: " + moduleId
                    + "    Routes: " + machine.get("recordCount"));
            lines.add("Runtime: inventory, recipe, save data, and UI bridge bound to "
                    + (machineId.isBlank() ? "selected block" : machineId));
            if (!hostSupportsAny(state, "native.ui.surface_open", "machine.used")) {
                lines.add("Focus: machine host unavailable for active runtime host");
                lines.add("Status: awaiting AdapterCore machine services");
                return moduleModel(moduleId, EchoNativeMachineSurfaceRenderer.class.getSimpleName(),
                        "machine:surface", lines);
            }
            lines.add("Focus: " + focusLabel("machine:surface", state));
            addStringRows(lines, "Supported peer", strings(machine.get("supportedBlocks")), 3);
            addObjectRows(lines, "Recipe", matchingMachineRows(machine, machineId, blockId), 4, "title", "summary");
            lines.add("Status: live Native Loader machine bridge attached");
            return moduleModel(moduleId, EchoNativeMachineSurfaceRenderer.class.getSimpleName(),
                    "machine:surface", lines);
        }
    }

    static final class EchoNativeMissionLogSurfaceRenderer {
        private EchoNativeMissionLogSurfaceRenderer() {
        }

        static Map<String, Object> render(Map<String, Object> state, Map<String, Object> dataSources) {
            Map<String, Object> mission = object(dataSources.get("missionLog"));
            String status = string(state, "missionStatus", String.valueOf(mission.get("status")));
            double progress = doubleValue(stateValue(state, "missionProgress", mission.get("progress")), 0.25D);
            String updateLine = string(state, "missionUpdateLine", "");
            ArrayList<String> lines = new ArrayList<>();
            lines.add("Mission: " + mission.get("title") + "    Source: " + mission.get("sourcePath"));
            lines.add("Objective: " + mission.get("objective") + "    Count: " + mission.get("objectiveCount"));
            addObjectRows(lines, "Task", objects(mission.get("objectives")), 3, "label", "description");
            if (!hostSupportsAny(state, "native.ui.mission_log_update")) {
                lines.add("Focus: mission update unavailable for active runtime host");
                lines.add("Status: awaiting supported runtime events");
                return moduleModel("echoscreencore", EchoNativeMissionLogSurfaceRenderer.class.getSimpleName(),
                        "mission_log:surface", lines);
            }
            lines.add("Status: " + status + "    Progress: " + percent(progress));
            if (!updateLine.isBlank()) {
                lines.add("Update: " + updateLine);
            }
            return moduleModel("echoscreencore", EchoNativeMissionLogSurfaceRenderer.class.getSimpleName(),
                    "mission_log:surface", lines);
        }
    }

    static final class EchoNativeSettingsSurfaceRenderer {
        private EchoNativeSettingsSurfaceRenderer() {
        }

        static Map<String, Object> render(Map<String, Object> state, Map<String, Object> dataSources) {
            Map<String, Object> settings = object(dataSources.get("settings"));
            ArrayList<String> lines = new ArrayList<>();
            lines.add("Settings: profile " + settings.get("profile"));
            lines.add("Theme: " + settings.get("theme") + "    Input: " + settings.get("inputMode"));
            lines.add("Selected: " + selectedOption(state, List.of("Profile", "Theme", "Input Mode", "HUD Scale", "Subtitles")));
            lines.add("HUD scale: " + decimal(stateValue(state, "settingsHudScale", settings.get("hudScale")))
                    + "    Subtitles: " + (booleanValue(stateValue(state, "settingsSubtitles", settings.get("subtitles"))) ? "enabled" : "disabled"));
            return moduleModel("echothemecore", EchoNativeSettingsSurfaceRenderer.class.getSimpleName(),
                    "settings:surface", lines);
        }
    }

    static final class EchoNativePauseSurfaceRenderer {
        private EchoNativePauseSurfaceRenderer() {
        }

        static Map<String, Object> render(Map<String, Object> state, Map<String, Object> dataSources) {
            String previousMode = string(state, "previousMode", "WIKI");
            ArrayList<String> lines = new ArrayList<>();
            lines.add("Pause: previous screen " + previousMode);
            lines.add("Selected: " + selectedOption(state, strings(object(dataSources.get("pauseFlow")).get("options"))));
            lines.add("Options: " + String.join(", ", strings(object(dataSources.get("pauseFlow")).get("options"))));
            lines.add("Press Esc to resume the previous Agent 5 screen.");
            return moduleModel("echoscreencore", EchoNativePauseSurfaceRenderer.class.getSimpleName(),
                    "pause:resume:" + previousMode, lines);
        }
    }

    static final class EchoNativeRecoverySurfaceRenderer {
        private EchoNativeRecoverySurfaceRenderer() {
        }

        static Map<String, Object> render(Map<String, Object> state, Map<String, Object> dataSources) {
            Map<String, Object> recovery = object(dataSources.get("deathRecovery"));
            String focusPath = "recovery:recover";
            ArrayList<String> lines = new ArrayList<>();
            lines.add("Death Recovery: press Enter to recover.");
            lines.add("Recovery point: " + recovery.get("recoveryPoint") + "    Type: " + recovery.get("displayName"));
            lines.add("Rule: " + recovery.get("rule") + "    Preset: " + recovery.get("preset"));
            addStringRows(lines, "Hazard", strings(recovery.get("hazardNotes")), 2);
            if (!hostSupportsAny(state, "player.inventory.grant")) {
                lines.add("Focus: recovery grant unavailable for active runtime host");
                lines.add("Status: awaiting supported runtime inventory");
                return moduleModel("echoscreencore", EchoNativeRecoverySurfaceRenderer.class.getSimpleName(),
                        "recovery:surface", lines);
            }
            lines.add("Focus: " + focusLabel(focusPath, state));
            lines.add(string(state, "recoveryOutput", "Status: WAITING"));
            return moduleModel("echoscreencore", EchoNativeRecoverySurfaceRenderer.class.getSimpleName(), focusPath, lines);
        }
    }

    static final class EchoNativeMainMenuSurfaceRenderer {
        private EchoNativeMainMenuSurfaceRenderer() {
        }

        static Map<String, Object> render(Map<String, Object> state, Map<String, Object> dataSources) {
            Map<String, Object> mainMenu = object(dataSources.get("mainMenu"));
            ArrayList<String> lines = new ArrayList<>();
            NativeLoaderTheme theme = NativeLoaderThemeResolver.activeTheme();
            lines.add("Main Menu: " + theme.token("identityLabel") + " routes");
            lines.add("Theme: " + theme.id() + "    Source: " + theme.source());
            lines.add("Selected: " + selectedOption(state, strings(mainMenu.get("options"))));
            lines.add("Options: " + String.join(", ", strings(mainMenu.get("options"))));
            String output = string(state, "mainMenuOutput", "");
            if (!output.isBlank()) {
                lines.add("Action: " + output);
            }
            return moduleModel("echoscreencore", EchoNativeMainMenuSurfaceRenderer.class.getSimpleName(),
                    "main_menu:surface", lines);
        }
    }

    static final class EchoNativeWorldSetupSurfaceRenderer {
        private EchoNativeWorldSetupSurfaceRenderer() {
        }

        static Map<String, Object> render(Map<String, Object> state, Map<String, Object> dataSources) {
            NativeLoaderTheme theme = NativeLoaderThemeResolver.activeTheme();
            ArrayList<String> lines = new ArrayList<>();
            lines.add("World Setup: " + theme.token("identityLabel") + " owns creation");
            lines.add("Policy: native_loader_owned_world=true    vanilla_create_world=false");
            lines.add("Preset: " + NativeLoaderAshfallWorldStartupService.WORLD_PRESET_ID);
            lines.add("World: " + NativeLoaderAshfallWorldStartupService.configuredProductWorldName()
                    + "    Folder: " + NativeLoaderAshfallWorldStartupService.configuredProductWorldFolder());
            lines.add("Theme: " + theme.id() + "    Source: " + theme.source());
            String output = string(state, "worldSetupOutput", "");
            lines.add(output.isBlank() ? "Enter: create/open native product world    Back: return to menu" : output);
            return moduleModel("echoscreencore", EchoNativeWorldSetupSurfaceRenderer.class.getSimpleName(),
                    "world_setup:surface", lines);
        }
    }

    static final class EchoNativeHudSurfaceRenderer {
        private EchoNativeHudSurfaceRenderer() {
        }

        static Map<String, Object> render(Map<String, Object> state, Map<String, Object> dataSources) {
            Map<String, Object> hud = object(dataSources.get("hud"));
            Object health = stateValue(state, "hudHealth", hud.get("health"));
            Object hazard = stateValue(state, "hudHazard", hud.get("hazard"));
            Object mission = stateValue(state, "hudMission", hud.get("mission"));
            String output = string(state, "hudUpdateOutput", "");
            ArrayList<String> lines = new ArrayList<>();
            lines.add("HUD: Health " + health);
            lines.add("Hazard: " + hazard);
            lines.add("Mission: " + mission);
            if (!hostSupportsAny(state, "native.ui.hud_refresh")) {
                lines.add("Focus: HUD refresh unavailable for active runtime host");
                lines.add("Status: awaiting supported runtime events");
                return moduleModel("echohudcore", EchoNativeHudSurfaceRenderer.class.getSimpleName(),
                        "echohudcore:hud", lines);
            }
            if (!output.isBlank()) {
                lines.add(output);
            }
            String cinematicOutput = string(state, "cinematicOutput", "");
            if (!cinematicOutput.isBlank()) {
                lines.add(cinematicOutput);
                lines.add("Cinematic: " + string(state, "cinematicCue", "")
                        + "    Camera: " + string(state, "cameraMode", "")
                        + " fov " + stateValue(state, "cameraFov", ""));
                if (bool(state, "cinematicLetterbox")) {
                    lines.add("Letterbox: active    Subtitle: " + string(state, "cinematicSubtitle", ""));
                }
            }
            return moduleModel("echohudcore", EchoNativeHudSurfaceRenderer.class.getSimpleName(),
                    "echohudcore:hud", lines);
        }
    }

    private static Map<String, Object> moduleModel(
            String moduleId,
            String rendererClass,
            String focusPath,
            List<String> lines
    ) {
        Map<String, Object> model = new LinkedHashMap<>();
        model.put("moduleId", moduleId);
        model.put("moduleRendererClass", rendererClass);
        model.put("focusPath", focusPath);
        model.put("lines", List.copyOf(lines));
        model.put("adapterCoreBridge", true);
        model.put("serviceCodeExecuted", true);
        model.put("nativeLoaderModuleSurfaceRendererServiceId", SERVICE_ID);
        return Map.copyOf(model);
    }

    public interface SurfaceOpener {
        Map<String, Object> open(String first, String second);
    }

    public interface ProductNamespaceSupplier {
        String namespace();
    }

    public static final class Context {
        private static final Context EMPTY = new Context(
                (first, second) -> Map.of("output", ""),
                (first, second) -> Map.of("output", ""),
                () -> ""
        );

        private final SurfaceOpener holomapOpener;
        private final SurfaceOpener wikiOpener;
        private final ProductNamespaceSupplier productNamespaceSupplier;

        public Context(
                SurfaceOpener holomapOpener,
                SurfaceOpener wikiOpener,
                ProductNamespaceSupplier productNamespaceSupplier
        ) {
            this.holomapOpener = holomapOpener == null ? EMPTY.holomapOpener : holomapOpener;
            this.wikiOpener = wikiOpener == null ? EMPTY.wikiOpener : wikiOpener;
            this.productNamespaceSupplier = productNamespaceSupplier == null
                    ? EMPTY.productNamespaceSupplier
                    : productNamespaceSupplier;
        }

        public static Context empty() {
            return EMPTY;
        }

        Map<String, Object> openHolomap(String layer, String marker) {
            return holomapOpener.open(layer, marker);
        }

        Map<String, Object> openWiki(String guide, String page) {
            return wikiOpener.open(guide, page);
        }

        String productNamespace() {
            String namespace = productNamespaceSupplier.namespace();
            return namespace == null ? "" : namespace;
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> object(Object value) {
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Map.of();
    }

    private static String focusLabel(String focusPath, Map<String, Object> state) {
        return focusPath.equals(string(state, "focusedControl", ""))
                && (bool(state, "mouseRouted") || bool(state, "initialFocusRouted"))
                ? focusPath + " ready"
                : focusPath + " waiting";
    }

    private static String typedOrPlaceholder(String value) {
        return value == null || value.isBlank() ? "_" : value;
    }

    private static String selectedOption(Map<String, Object> state, List<String> options) {
        String explicit = string(state, "selectedOption", "");
        if (!explicit.isBlank()) {
            return explicit;
        }
        if (options.isEmpty()) {
            return "";
        }
        int selectedIndex = integer(state == null ? null : state.get("selectedIndex"));
        if (selectedIndex < 0 || selectedIndex >= options.size()) {
            selectedIndex = 0;
        }
        return options.get(selectedIndex);
    }

    private static int integer(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null) {
            return 0;
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private static Object stateValue(Map<String, Object> state, String key, Object fallback) {
        if (state == null || !state.containsKey(key)) {
            return fallback;
        }
        return state.get(key);
    }

    private static String decimal(Object value) {
        if (value instanceof Number number) {
            return String.valueOf(number.doubleValue());
        }
        return value == null ? "0.0" : String.valueOf(value);
    }

    private static double doubleValue(Object value, double fallback) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value instanceof String string) {
            try {
                return Double.parseDouble(string);
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    private static String percent(double value) {
        return Math.round(value * 100.0D) + "%";
    }

    private static boolean booleanValue(Object value) {
        return Boolean.TRUE.equals(value);
    }

    private static String string(Map<String, Object> values, String key, String fallback) {
        if (values == null) {
            return fallback;
        }
        Object value = values.get(key);
        return value == null ? fallback : String.valueOf(value);
    }

    private static boolean bool(Map<String, Object> values, String key) {
        return values != null && Boolean.TRUE.equals(values.get(key));
    }

    private static boolean hostSupportsAny(Map<String, Object> state, String... actionIds) {
        if (!Boolean.TRUE.equals(state == null ? null : state.get("runtimeHostActionGateActive"))) {
            return true;
        }
        if (state == null || actionIds == null || actionIds.length == 0) {
            return false;
        }
        Object supported = state.get("runtimeSupportedActions");
        if (supported instanceof Iterable<?> iterable) {
            for (Object entry : iterable) {
                String action = entry == null ? "" : String.valueOf(entry).trim();
                for (String actionId : actionIds) {
                    if (action.equals(actionId)) {
                        return true;
                    }
                }
            }
            return false;
        }
        String supportedText = supported == null ? "" : String.valueOf(supported).trim();
        if (supportedText.isBlank()) {
            return false;
        }
        for (String actionId : actionIds) {
            if (supportedActionTextContains(supportedText, actionId)) {
                return true;
            }
        }
        return false;
    }

    private static boolean supportedActionTextContains(String supportedText, String actionId) {
        if (supportedText == null || actionId == null || actionId.isBlank()) {
            return false;
        }
        for (String token : supportedText.split("[,;\\s\\[\\]\"']+")) {
            if (token.trim().equals(actionId)) {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private static List<String> strings(Object value) {
        if (value instanceof List<?> list) {
            return (List<String>) list;
        }
        return List.of();
    }

    private static List<Map<String, Object>> firstObjects(Object value, int max) {
        List<Map<String, Object>> rows = objects(value);
        if (rows.size() <= max) {
            return rows;
        }
        return rows.subList(0, max);
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> objects(Object value) {
        if (value instanceof List<?> list) {
            ArrayList<Map<String, Object>> rows = new ArrayList<>();
            for (Object row : list) {
                if (row instanceof Map<?, ?> map) {
                    rows.add((Map<String, Object>) map);
                }
            }
            return List.copyOf(rows);
        }
        return List.of();
    }

    private static void addObjectRows(
            List<String> lines,
            String label,
            List<Map<String, Object>> rows,
            int max,
            String primaryKey,
            String secondaryKey
    ) {
        int limit = Math.min(max, rows.size());
        for (int index = 0; index < limit; index++) {
            Map<String, Object> row = rows.get(index);
            String primary = stringValue(row, primaryKey, stringValue(row, "id", "entry"));
            String secondary = stringValue(row, secondaryKey, stringValue(row, "summary", ""));
            lines.add(label + ": " + primary + (secondary.isBlank() ? "" : " - " + secondary));
        }
    }

    private static void addStringRows(List<String> lines, String label, List<String> rows, int max) {
        int limit = Math.min(max, rows.size());
        for (int index = 0; index < limit; index++) {
            lines.add(label + ": " + rows.get(index));
        }
    }

    private static String stringValue(Map<String, Object> values, String key, String fallback) {
        if (values == null) {
            return fallback;
        }
        Object value = values.get(key);
        return value == null ? fallback : String.valueOf(value);
    }

    private static List<Map<String, Object>> matchingMachineRows(
            Map<String, Object> machine,
            String machineId,
            String blockId
    ) {
        String wanted = (machineId + " " + blockId + " " + pathOf(machineId) + " " + pathOf(blockId))
                .toLowerCase(java.util.Locale.ROOT);
        List<Map<String, Object>> rows = objects(machine.get("rows"));
        if (wanted.isBlank()) {
            return rows;
        }
        ArrayList<Map<String, Object>> matching = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            String text = (stringValue(row, "id", "") + " "
                    + stringValue(row, "title", "") + " "
                    + stringValue(row, "summary", "") + " "
                    + stringValue(row, "source", ""))
                    .toLowerCase(java.util.Locale.ROOT);
            for (String token : wanted.split("[^a-z0-9]+")) {
                if (token.length() > 3 && text.contains(token)) {
                    matching.add(row);
                    break;
                }
            }
        }
        return matching.isEmpty() ? rows : List.copyOf(matching);
    }

    private static String machineLabel(String machineId, String moduleId) {
        String id = firstNonBlank(machineId, moduleId);
        String path = pathOf(id);
        if (path.isBlank()) {
            path = id;
        }
        StringBuilder label = new StringBuilder();
        for (String token : path.split("[_\\-]+")) {
            if (token.isBlank()) {
                continue;
            }
            if (!label.isEmpty()) {
                label.append(' ');
            }
            label.append(Character.toUpperCase(token.charAt(0)));
            if (token.length() > 1) {
                label.append(token.substring(1));
            }
        }
        return label.isEmpty() ? "Native Machine" : label.toString();
    }

    private static String namespaceOf(String contentId) {
        String id = contentId == null ? "" : contentId.trim();
        int separator = id.indexOf(':');
        return separator > 0 ? id.substring(0, separator) : "";
    }

    private static String pathOf(String contentId) {
        String id = contentId == null ? "" : contentId.trim();
        int separator = id.indexOf(':');
        return separator >= 0 && separator + 1 < id.length() ? id.substring(separator + 1) : id;
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }
}
