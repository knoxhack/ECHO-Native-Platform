package dev.echo.nativeplatform.bootstrap;

import dev.echo.nativeplatform.contracts.EchoNativeBootstrapProductProfile.NativePhysicalActionRoute;
import dev.echo.nativeplatform.contracts.EchoNativeBootstrapProductProfile.NativeUiActionRoute;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;

public final class EchoNativeAgent5UiHandlerRegistry {
    private static final Map<String, List<SourceRecord>> RESOURCE_RECORDS_CACHE = new LinkedHashMap<>();
    private static final String ROOT_SIGNALOS = "signalos";
    private static final String ROOT_PRODUCT_ACTION_MISSIONS = "productActionMissions";
    private static final String ROOT_MACHINE_RECIPES = "machineRecipes";
    private static final String ROOT_TERMINAL_PAGES = "terminalPages";
    private static final String ROOT_INDEX_ENTRIES = "indexEntries";
    private static final String ROOT_LENS_PROFILES = "lensProfiles";
    private static final String ROOT_MISSION_LOG = "missionLog";
    private static final String ROOT_RECOVERY_GRAVE_TYPES = "recoveryGraveTypes";
    private static final String ROOT_RECOVERY_PRESETS = "recoveryPresets";
    private static final String ROOT_RECOVERY_RULES = "recoveryRules";
    private static final String ROOT_HOLOMAP_LAYERS = "holomapLayers";
    private static final String ROOT_WIKI_CONTENT = "wikiContent";
    private static Map<String, Object> dataSourcesCache;

    private EchoNativeAgent5UiHandlerRegistry() {
    }

    public static synchronized Map<String, Object> dataSources() {
        if (dataSourcesCache != null) {
            return dataSourcesCache;
        }
        Map<String, Object> terminal = terminal();
        Map<String, Object> index = index();
        Map<String, Object> lens = lens();
        Map<String, Object> missionLog = missionLog();
        Map<String, Object> holomap = holomap();
        Map<String, Object> wiki = wiki();
        Map<String, Object> signalos = signalos();
        Map<String, Object> productActionSurface = productActionSurface();
        Map<String, Object> ashfallDrone = ashfallDrone();
        Map<String, Object> machine = machine();
        Map<String, Object> notifications = Map.of(
                "terminal", terminal.get("title"),
                "mission", missionLog.get("title")
        );
        Map<String, Object> dataSources = new LinkedHashMap<>();
        dataSources.put("terminal", terminal);
        dataSources.put("index", index);
        dataSources.put("lens", lens);
        Map<String, Object> hud = new LinkedHashMap<>();
        hud.put("health", 100);
        hud.put("hazard", "Mission signal: " + missionLog.get("status"));
        hud.put("missionId", missionLog.get("missionId"));
        hud.put("missionStatus", missionLog.get("status"));
        hud.put("missionTitle", missionLog.get("title"));
        hud.put("missionObjective", missionLog.get("objective"));
        hud.put("mission", missionLog.get("objective"));
        hud.put("missionProgress", missionLog.get("progress"));
        hud.put("notifications", notificationQueue(terminal, missionLog));
        hud.put("source", notifications);
        dataSources.put("hud", Map.copyOf(hud));
        dataSources.put("notifications", notificationQueue(terminal, missionLog));
        dataSources.put("missionLog", missionLog);
        dataSources.put("settings", settings());
        dataSources.put("pauseFlow", pauseFlow());
        dataSources.put("deathRecovery", deathRecovery());
        dataSources.put("holomap", holomap);
        dataSources.put("wiki", wiki);
        dataSources.put("signalos", signalos);
        dataSources.put("productActionSurface", productActionSurface);
        dataSources.put("ashfallDrone", ashfallDrone);
        dataSources.put("machine", machine);
        dataSources.put("camera", camera());
        dataSources.put("cinematic", cinematic());
        dataSources.put("mainMenu", Map.of("options", EchoNativeBootstrapMain.nativeMainMenuOptions()));
        dataSourcesCache = Map.copyOf(dataSources);
        return dataSourcesCache;
    }

    private static Map<String, Object> signalos() {
        List<SourceRecord> records = resourceRecordsFor(
                ROOT_SIGNALOS,
                "data/signalos/signalos/chapters",
                "data/signalos/signalos/missions",
                "data/signalos/signalos/archives",
                "data/signalos/signalos/apps",
                "data/signalos/signalos/data_records",
                "data/signalos/signalos/drive_templates",
                "data/signalos/signalos/net_sites"
        );
        SourceRecord record = preferred(records, defaultContentId("signalosRecord", "chapters"));
        List<Map<String, Object>> contentRecords = new ArrayList<>();
        for (SourceRecord source : records) {
            Map<String, Object> content = new LinkedHashMap<>(jsonFlatObject(source.json()));
            content.put("sourcePath", source.path());
            content.put("lines", jsonStringArray(source.json(), "lines"));
            contentRecords.add(Map.copyOf(content));
        }
        Map<String, Object> signalos = new LinkedHashMap<>();
        signalos.put("screenId", "signalos:terminal");
        signalos.put("title", jsonString(record.json(), "title", "SignalOS Terminal"));
        signalos.put("summary", jsonString(record.json(), "summary", "SignalOS content loaded"));
        signalos.put("pages", jsonStringArray(record.json(), "pages"));
        signalos.put("transport", "echonetcore:serverbound_action");
        signalos.put("packet", "SignalOsOpenTerminalPacket");
        signalos.put("status", records.isEmpty() ? "MISSING_CONTENT" : "READY");
        signalos.put("sourcePath", record.path());
        signalos.put("recordCount", records.size());
        signalos.put("records", List.copyOf(contentRecords));
        return Map.copyOf(signalos);
    }

    private static Map<String, Object> productActionSurface() {
        SourceRecord repairMission = preferred(
                resourceRecordsFor(ROOT_PRODUCT_ACTION_MISSIONS),
                defaultContentId("productActionRepairMission", "product_action_primary")
        );
        SourceRecord intelMission = preferred(
                resourceRecordsFor(ROOT_PRODUCT_ACTION_MISSIONS),
                defaultContentId("productActionIntelMission", "product_action_intel")
        );
        NativeUiActionRoute actionRoute = primaryProductActionRoute();
        String surface = actionRoute == null ? "PRODUCT_ACTION" : actionRoute.surface();
        String screenId = actionRoute == null ? EchoNativeBootstrapMain.nativeProductId("product_action") : actionRoute.screenId();
        String canonicalId = actionRoute == null ? EchoNativeBootstrapMain.nativeProductId("product_action") : actionRoute.canonicalId();
        Map<String, Object> drone = new LinkedHashMap<>();
        drone.put("screenId", screenId);
        drone.put("surface", surface);
        drone.put("canonicalId", canonicalId);
        drone.put("title", jsonString(repairMission.json(), "title", "Product Action Surface"));
        drone.put("summary", jsonString(repairMission.json(), "briefing", "Product action support loaded"));
        drone.put("intelRoute", jsonString(intelMission.json(), "id", canonicalId));
        drone.put("transport", "echonetcore:serverbound_action");
        drone.put("packet", actionRoute == null ? "ProductActionPacket" : actionRoute.packetClassName());
        drone.put("commands", productActionCommands(actionRoute, surface));
        drone.put("keys", productActionKeys(surface));
        drone.put("sourcePath", repairMission.path());
        drone.put("intelSourcePath", intelMission.path());
        drone.put("adapterCoreBridge", actionRoute == null ? canonicalId : actionRoute.source());
        return Map.copyOf(drone);
    }

    private static Map<String, Object> ashfallDrone() {
        Map<String, Object> drone = new LinkedHashMap<>(productActionSurface());
        drone.put("screenId", "echoashfallprotocol:drone");
        drone.put("surface", "ASHFALL_DRONE");
        drone.put("canonicalId", "echoashfallprotocol:companion_drone");
        drone.put("target", "echoashfallprotocol:companion_drone");
        drone.put("packet", "com.knoxhack.echoashfallprotocol.network.DroneCommandPacket");
        drone.put("commands", List.of("recall", "scan", "scout", "status", "toggle_assist"));
        drone.put("keys", List.of("X", "C", "Y", "Z", "B"));
        drone.put("adapterCoreBridge", "native_ui_ashfall_drone");
        return Map.copyOf(drone);
    }

    private static Map<String, Object> machine() {
        List<SourceRecord> records = resourceRecordsFor(ROOT_MACHINE_RECIPES);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (SourceRecord record : records) {
            String type = jsonString(record.json(), "type", "");
            if (type.isBlank() && !record.path().contains("machine")) {
                continue;
            }
            Map<String, Object> row = new LinkedHashMap<>(jsonFlatObject(record.json()));
            row.put("sourcePath", record.path());
            row.putIfAbsent("title", titleFromPath(record.path()));
            row.putIfAbsent("summary", type.isBlank() ? "Native machine recipe/data route" : type);
            rows.add(Map.copyOf(row));
        }
        Map<String, Object> machine = new LinkedHashMap<>();
        String machineScreenId = EchoNativeBootstrapMain.nativeMachineScreenId();
        if (machineScreenId.isBlank()) {
            machineScreenId = EchoNativeBootstrapMain.nativeProductId("machine");
        }
        String machineSourcePath = EchoNativeBootstrapMain.nativeMachineRecipeCatalogSourcePath();
        machine.put("screenId", machineScreenId);
        machine.put("title", "Native Machine");
        machine.put("summary", "Native Loader machine host: power, inventory, recipe, save, and UI bridge");
        machine.put("sourcePath", rows.isEmpty()
                ? machineSourcePath
                : String.valueOf(rows.get(0).getOrDefault("sourcePath", "")));
        machine.put("recordCount", Math.max(1, rows.size()));
        machine.put("rowCount", rows.size());
        machine.put("rows", List.copyOf(rows.stream().limit(12).toList()));
        machine.put("supportedBlocks", List.of(
                "thermal_burner",
                "battery_bank",
                "ore_grinder",
                "water_purifier",
                "scrap_press",
                "research_lab",
                "factory_controller",
                "nexus_capacitor"
        ));
        machine.put("adapterCoreBridge", machineScreenId);
        return Map.copyOf(machine);
    }

    private static Map<String, Object> terminal() {
        List<SourceRecord> records = resourceRecordsFor(
                ROOT_TERMINAL_PAGES,
                "data/echoterminal/echoterminal/pages"
        );
        SourceRecord record = preferred(
                records,
                defaultContentId("terminalPage", "native_loader_overview")
        );
        String title = jsonString(record.json(), "title", "Native Terminal");
        String summary = jsonString(record.json(), "summary", "Native route page loaded");
        List<String> nextSteps = jsonStringArray(record.json(), "nextSteps");
        List<Map<String, Object>> panels = jsonObjectArray(record.json(), "panels");
        List<Map<String, Object>> pages = new ArrayList<>();
        for (SourceRecord source : records) {
            Map<String, Object> page = new LinkedHashMap<>(jsonFlatObject(source.json()));
            page.put("sourcePath", source.path());
            page.put("nextSteps", jsonStringArray(source.json(), "nextSteps"));
            page.put("panels", jsonObjectArray(source.json(), "panels"));
            pages.add(Map.copyOf(page));
        }
        Map<String, Object> terminal = new LinkedHashMap<>();
        terminal.put("command", "status");
        terminal.put("prompt", "ASH>");
        terminal.put("title", title);
        terminal.put("summary", summary);
        terminal.put("nextSteps", nextSteps);
        terminal.put("panels", panels);
        terminal.put("pages", List.copyOf(pages));
        terminal.put("pageCount", pages.size());
        terminal.put("readyLine", title + " / " + summary
                + (nextSteps.isEmpty() ? "" : " / Next: " + nextSteps.get(0)));
        terminal.put("sourceRoots", sourceRootsFor(
                ROOT_TERMINAL_PAGES,
                "data/echoterminal/echoterminal/pages"
        ));
        terminal.put("sourcePath", record.path());
        terminal.put("recordCount", records.size());
        return Map.copyOf(terminal);
    }

    private static Map<String, Object> index() {
        List<SourceRecord> records = resourceRecordsFor(
                ROOT_INDEX_ENTRIES,
                "data/echoindex/echo_index/entries"
        );
        SourceRecord record = preferred(records, defaultContentId("indexEntry", "native_loader"));
        String title = jsonString(record.json(), "title", "Native Field Index");
        String subtitle = jsonString(record.json(), "subtitle", "");
        String summary = jsonString(record.json(), "summary", "Index entry loaded");
        String result = title + (subtitle.isBlank() ? "" : " / " + subtitle) + " / " + summary;
        List<Map<String, Object>> entries = new ArrayList<>();
        for (SourceRecord source : records) {
            Map<String, Object> entry = new LinkedHashMap<>(jsonFlatObject(source.json()));
            entry.put("sourcePath", source.path());
            entry.put("body", jsonStringArray(source.json(), "body"));
            entry.put("links", jsonObject(source.json(), "links"));
            entry.put("tags", jsonStringArray(source.json(), "tags"));
            entries.add(Map.copyOf(entry));
        }
        Map<String, Object> index = new LinkedHashMap<>();
        index.put("query", "ashfall");
        index.put("title", title);
        index.put("subtitle", subtitle);
        index.put("summary", summary);
        index.put("result", result);
        index.put("entries", List.copyOf(entries));
        index.put("sourceRoots", sourceRootsFor(
                ROOT_INDEX_ENTRIES,
                "data/echoindex/echo_index/entries"
        ));
        index.put("sourcePath", record.path());
        index.put("recordCount", records.size());
        return Map.copyOf(index);
    }

    private static Map<String, Object> lens() {
        List<SourceRecord> records = resourceRecordsFor(
                ROOT_LENS_PROFILES,
                "data/echolens/echolens/scan_profiles"
        );
        SourceRecord record = preferred(records, defaultContentId("lensProfile", "native_loader_scans"));
        String title = jsonString(record.json(), "title", "Native Lens");
        List<Map<String, Object>> rows = new ArrayList<>();
        for (SourceRecord source : records) {
            String profileId = jsonString(source.json(), "id", source.path());
            String profileTitle = jsonString(source.json(), "title", "Lens Profile");
            for (Map<String, Object> row : jsonObjectArray(source.json(), "rows")) {
                Map<String, Object> enriched = new LinkedHashMap<>(row);
                enriched.put("profileId", profileId);
                enriched.put("profileTitle", profileTitle);
                enriched.put("sourcePath", source.path());
                rows.add(Map.copyOf(enriched));
            }
        }
        if (rows.isEmpty()) {
            rows = jsonObjectArray(record.json(), "rows");
        }
        Map<String, Object> first = rows.isEmpty() ? Map.of() : rows.get(0);
        String target = stringValue(first, "target",
                jsonString(record.json(), "target", EchoNativeBootstrapMain.nativeLensFallbackTarget()));
        String text = stringValue(first, "text", jsonString(record.json(), "summary", "Lens profile loaded"));
        String risk = stringValue(first, "riskLabel", jsonString(record.json(), "riskLabel", ""));
        Map<String, Object> lens = new LinkedHashMap<>();
        lens.put("target", target);
        lens.put("title", title);
        lens.put("result", risk.isBlank() ? text : risk + " / " + text);
        lens.put("summary", text);
        lens.put("riskLabel", risk);
        lens.put("rows", rows);
        lens.put("profiles", records.stream().map(source -> {
            Map<String, Object> profile = new LinkedHashMap<>(jsonFlatObject(source.json()));
            profile.put("sourcePath", source.path());
            profile.put("rowCount", jsonObjectArray(source.json(), "rows").size());
            return Map.copyOf(profile);
        }).toList());
        lens.put("sourceRoots", sourceRootsFor(
                ROOT_LENS_PROFILES,
                "data/echolens/echolens/scan_profiles"
        ));
        lens.put("sourcePath", record.path());
        lens.put("recordCount", records.size());
        lens.put("rowCount", rows.size());
        return Map.copyOf(lens);
    }

    public static Map<String, Object> executeTerminal(String command) {
        Map<String, Object> terminal = object(dataSources().get("terminal"));
        String expected = String.valueOf(terminal.get("command"));
        String normalized = normalize(command);
        Map<String, Object> selectedPage = Map.of();
        boolean commandHandled = expected.equalsIgnoreCase(normalized);
        if (!commandHandled) {
            for (Map<String, Object> page : objects(terminal.get("pages"))) {
                if (matchesRecord(page, normalized)) {
                    selectedPage = page;
                    break;
                }
            }
        }
        boolean handled = commandHandled || !selectedPage.isEmpty();
        String output;
        if (!selectedPage.isEmpty()) {
            output = stringValue(selectedPage, "title", "Terminal Page") + " / "
                    + stringValue(selectedPage, "summary", "")
                    + " / Source: " + stringValue(selectedPage, "sourcePath", "");
        } else if (handled) {
            output = String.valueOf(terminal.get("readyLine"));
        } else {
            output = "unknown command or page: " + normalized;
        }
        return result(
                "echoterminal",
                "echoterminal:terminal_service",
                "echoterminal:terminal",
                handled,
                expected,
                output,
                List.of("terminal-command:" + normalized)
        );
    }

    public static Map<String, Object> searchIndex(String query) {
        Map<String, Object> index = object(dataSources().get("index"));
        String expected = String.valueOf(index.get("query"));
        String normalized = normalize(query);
        List<String> matches = new ArrayList<>();
        for (Map<String, Object> entry : objects(index.get("entries"))) {
            if (matchesIndexEntry(entry, normalized)) {
                matches.add(stringValue(entry, "title", "Untitled Index Entry")
                        + ": " + stringValue(entry, "summary", ""));
            }
        }
        if (matches.isEmpty() && expected.equals(normalized)) {
            matches.add(String.valueOf(index.get("result")));
        }
        boolean handled = !normalized.isBlank() && !matches.isEmpty();
        return result(
                "echoindex",
                "echoindex:index_service",
                "echoindex:index",
                handled,
                expected,
                handled
                        ? matches.size() + " index result(s): " + String.join(" | ", matches.stream().limit(3).toList())
                        : "no index results for: " + normalized,
                List.of("index-search:" + normalize(query))
        );
    }

    public static Map<String, Object> scanLens(String target) {
        Map<String, Object> lens = object(dataSources().get("lens"));
        String expected = String.valueOf(lens.get("target"));
        String normalizedTarget = normalize(target);
        Map<String, Object> row = Map.of();
        for (Map<String, Object> candidate : objects(lens.get("rows"))) {
            if (stringValue(candidate, "target", "").equalsIgnoreCase(normalizedTarget)) {
                row = candidate;
                break;
            }
        }
        if (row.isEmpty() && expected.equals(normalizedTarget)) {
            row = Map.of(
                    "riskLabel", lens.get("riskLabel"),
                    "text", lens.get("summary"),
                    "indexEntry", "",
                    "holomapMarker", ""
            );
        }
        boolean handled = !row.isEmpty();
        String output = handled
                ? stringValue(row, "riskLabel", "") + " / " + stringValue(row, "text", "")
                + " / index " + stringValue(row, "indexEntry", "")
                + " / marker " + stringValue(row, "holomapMarker", "")
                : "scan target not recognized: " + normalizedTarget;
        return result(
                "echolens",
                "echolens:inspection_service",
                "echolens:lens",
                handled,
                expected,
                output,
                List.of("lens-scan:" + normalizedTarget)
        );
    }

    public static Map<String, Object> renderHud() {
        Map<String, Object> hud = object(dataSources().get("hud"));
        return result(
                "echohudcore",
                "echohudcore:hud_service",
                "echohudcore:hud",
                true,
                String.valueOf(hud.get("mission")),
                "Health " + hud.get("health") + " / " + hud.get("hazard"),
                List.of("hud-update:" + hud.get("mission"))
        );
    }

    public static Map<String, Object> renderSurface(String mode, Map<String, Object> state) {
        return EchoNativeAgent5SurfaceRenderer.render(mode, state, dataSources());
    }

    public static Map<String, Object> openHolomap(String layer, String marker) {
        Map<String, Object> holomap = object(dataSources().get("holomap"));
        String normalizedLayer = normalize(layer);
        String normalizedMarker = normalize(marker);
        Map<String, Object> selectedMarker = Map.of();
        for (Map<String, Object> candidate : objects(holomap.get("markers"))) {
            boolean layerMatches = normalizedLayer.isBlank()
                    || stringValue(candidate, "layerId", "").equalsIgnoreCase(normalizedLayer)
                    || stringValue(candidate, "layerName", "").equalsIgnoreCase(normalizedLayer)
                    || stringValue(candidate, "sourcePath", "").equalsIgnoreCase(normalizedLayer);
            boolean markerMatches = normalizedMarker.isBlank()
                    || stringValue(candidate, "id", "").equalsIgnoreCase(normalizedMarker)
                    || stringValue(candidate, "label", "").equalsIgnoreCase(normalizedMarker);
            if (layerMatches && markerMatches) {
                selectedMarker = candidate;
                break;
            }
        }
        String expectedLayer = String.valueOf(holomap.get("layer"));
        String expectedMarker = String.valueOf(holomap.get("marker"));
        boolean handled = !selectedMarker.isEmpty()
                || (expectedLayer.equalsIgnoreCase(normalizedLayer) && expectedMarker.equalsIgnoreCase(normalizedMarker));
        Map<String, Object> resolved = selectedMarker.isEmpty() ? holomap : selectedMarker;
        String output = handled
                ? "Layer " + stringValue(resolved, "layerName", String.valueOf(holomap.get("layerName")))
                + " marker " + stringValue(resolved, "label", stringValue(resolved, "markerLabel", String.valueOf(holomap.get("markerLabel"))))
                + " [" + stringValue(resolved, "id", String.valueOf(holomap.get("marker"))) + "] focus "
                + stringValue(resolved, "description", String.valueOf(holomap.get("focus")))
                : "holomap route not recognized: " + normalizedLayer + "/" + normalizedMarker;
        return result(
                "echoholomap",
                "echoholomap:holomap_service",
                "echoholomap:holomap",
                handled,
                expectedLayer + "/" + expectedMarker,
                output,
                List.of("holomap-open:" + normalizedLayer, "holomap-marker:" + normalizedMarker)
        );
    }

    public static Map<String, Object> openWiki(String guide, String page) {
        Map<String, Object> wiki = object(dataSources().get("wiki"));
        String expectedGuide = String.valueOf(wiki.get("guide"));
        String expectedPage = String.valueOf(wiki.get("page"));
        String normalizedGuide = normalize(guide);
        String normalizedPage = normalize(page);
        Map<String, Object> selectedArticle = Map.of();
        for (Map<String, Object> article : objects(wiki.get("articles"))) {
            boolean guideMatches = normalizedGuide.isBlank()
                    || stringValue(article, "category", "").equalsIgnoreCase(normalizedGuide)
                    || stringValue(article, "id", "").equalsIgnoreCase(normalizedGuide)
                    || stringValue(article, "sourcePath", "").contains(normalizedGuide);
            boolean pageMatches = normalizedPage.isBlank()
                    || stringValue(article, "title", "").equalsIgnoreCase(normalizedPage)
                    || stringValue(article, "id", "").equalsIgnoreCase(normalizedPage)
                    || stringValue(article, "sourcePath", "").contains(normalizedPage);
            if (guideMatches && pageMatches) {
                selectedArticle = article;
                break;
            }
        }
        boolean handled = !selectedArticle.isEmpty()
                || (expectedGuide.equalsIgnoreCase(normalizedGuide) && expectedPage.equalsIgnoreCase(normalizedPage));
        Map<String, Object> resolved = selectedArticle.isEmpty() ? wiki : selectedArticle;
        return result(
                "echowiki",
                "echowiki:wiki_service",
                "echowiki:wiki",
                handled,
                expectedGuide + "/" + expectedPage,
                handled
                        ? "Article " + stringValue(resolved, "title", expectedPage)
                        + " category " + stringValue(resolved, "category", expectedGuide)
                        + " link " + stringValue(resolved, "link", String.valueOf(wiki.get("link")))
                        : "wiki page not recognized: " + normalizedGuide + "/" + normalizedPage,
                List.of("wiki-open:" + normalizedGuide, "wiki-page:" + normalizedPage)
        );
    }

    public static Map<String, Object> recover() {
        Map<String, Object> recovery = object(dataSources().get("deathRecovery"));
        return result(
                "echoscreencore",
                "echoscreencore:death_recovery",
                "echoscreencore:death_recovery",
                true,
                String.valueOf(recovery.get("action")),
                "Status: " + recovery.get("status") + "    Health: " + recovery.get("restoredHealth"),
                List.of("death-recovery:" + recovery.get("recoveryPoint"))
        );
    }

    static List<Map<String, Object>> notificationQueue() {
        return notificationQueue(terminal(), missionLog());
    }

    static List<Map<String, Object>> notificationQueue(Map<String, Object> terminal, Map<String, Object> mission) {
        return List.of(
                notification("echoterminal:" + terminal.get("sourcePath"), "INFO",
                        String.valueOf(terminal.get("title")), "top_left_safe_area"),
                notification("missioncore:" + mission.get("missionId"), "INFO",
                        String.valueOf(mission.get("title")), "top_left_safe_area")
        );
    }

    static Map<String, Object> missionLog() {
        List<SourceRecord> records = resourceRecordsFor(
                ROOT_MISSION_LOG,
                "data/echoashfallprotocol/missioncore/missions"
        );
        SourceRecord record = preferred(records, defaultContentId("missionLog", "secure_crash_outpost"));
        String id = normalizedMissionId(jsonString(record.json(), "id", "echoashfallprotocol:secure_crash_outpost"));
        String title = jsonString(record.json(), "title", "Anchor Pod Outpost");
        List<Map<String, Object>> objectives = jsonObjectArray(record.json(), "objectives");
        List<Map<String, Object>> rewards = jsonObjectArray(record.json(), "rewards");
        Map<String, Object> firstObjective = objectives.isEmpty() ? Map.of() : objectives.get(0);
        String objective = stringValue(firstObjective, "label",
                jsonString(record.json(), "summary",
                        jsonString(record.json(), "briefing", "Open the native product route")));
        Map<String, Object> mission = new LinkedHashMap<>();
        mission.put("screenId", "echoscreencore:mission_log");
        mission.put("missionId", id);
        mission.put("title", title);
        mission.put("objective", objective);
        mission.put("objectives", objectives);
        mission.put("objectiveCount", objectives.size());
        mission.put("rewards", rewards);
        mission.put("status", "TRACKED");
        mission.put("progress", 0.25D);
        mission.put("sourcePath", record.path());
        mission.put("recordCount", records.size());
        mission.put("adapterCoreBridge", "echoscreencore:mission_log");
        return Map.copyOf(mission);
    }

    static Map<String, Object> settings() {
        Map<String, Object> settings = new LinkedHashMap<>();
        settings.put("screenId", "echoscreencore:settings");
        settings.put("profile", "ashfall-accessible");
        settings.put("theme", "ashfall-agent5");
        settings.put("inputMode", "keyboard_mouse");
        settings.put("hudScale", 1.0D);
        settings.put("subtitles", true);
        settings.put("adapterCoreBridge", "echoscreencore:settings");
        return Map.copyOf(settings);
    }

    static Map<String, Object> pauseFlow() {
        Map<String, Object> pause = new LinkedHashMap<>();
        pause.put("screenId", "echoscreencore:pause_flow");
        pause.put("previousScreenId", "echowiki:wiki");
        pause.put("resumeTarget", "echowiki:wiki");
        pause.put("options", List.of("Resume", "Settings", "Save Snapshot", "Quit to Main Menu"));
        pause.put("adapterCoreBridge", "echoscreencore:pause_flow");
        return Map.copyOf(pause);
    }

    static Map<String, Object> deathRecovery() {
        List<SourceRecord> graveTypes = resourceRecordsFor(
                ROOT_RECOVERY_GRAVE_TYPES,
                "data/echorecovery/echorecovery/recovery_grave_type"
        );
        List<SourceRecord> presets = resourceRecordsFor(
                ROOT_RECOVERY_PRESETS,
                "data/echorecovery/echorecovery/recovery_preset"
        );
        List<SourceRecord> rules = resourceRecordsFor(
                ROOT_RECOVERY_RULES,
                "data/echorecovery/echorecovery/recovery_rule"
        );
        SourceRecord grave = preferred(graveTypes, defaultContentId("recoveryGraveType", "native_recovery_cache"));
        SourceRecord preset = preferred(presets, defaultContentId("recoveryPreset", "default"));
        SourceRecord rule = preferred(rules, defaultContentId("recoveryRule", "recovery_defaults"));
        Map<String, Object> recovery = new LinkedHashMap<>();
        recovery.put("screenId", "echoscreencore:death_recovery");
        recovery.put("recoveryPoint", jsonString(grave.json(), "id", "echorecovery:vanilla_grave"));
        recovery.put("displayName", jsonString(grave.json(), "display_name", "Recovery Cache"));
        recovery.put("block", jsonString(grave.json(), "block", "echorecovery:grave"));
        recovery.put("hazardNotes", jsonStringArray(grave.json(), "hazard_notes"));
        recovery.put("preset", jsonString(preset.json(), "display_name", "Recovery Preset"));
        recovery.put("rule", jsonString(rule.json(), "action", "protected"));
        recovery.put("action", "recover");
        recovery.put("contentStatus", graveTypes.isEmpty() ? "MISSING_CONTENT" : "READY");
        recovery.put("status", "RECOVERED");
        recovery.put("restoredHealth", 35);
        recovery.put("sourcePath", grave.path());
        recovery.put("recordCount", graveTypes.size() + presets.size() + rules.size());
        recovery.put("adapterCoreBridge", "echoscreencore:death_recovery");
        return Map.copyOf(recovery);
    }

    static Map<String, Object> holomap() {
        List<SourceRecord> records = resourceRecordsFor(
                ROOT_HOLOMAP_LAYERS,
                "data/echoholomap/echoholomap/layers"
        );
        SourceRecord record = preferred(records, defaultContentId("holomapLayer", "native_loader_layer"));
        String id = "echoashfallprotocol:first_month_field_intel";
        String name = jsonString(record.json(), "name", "Native HoloMap");
        List<Map<String, Object>> markers = new ArrayList<>();
        List<Map<String, Object>> layers = new ArrayList<>();
        for (SourceRecord source : records) {
            String layerId = jsonString(source.json(), "id", source.path());
            String layerName = jsonString(source.json(), "name",
                    jsonString(source.json(), "title", "HoloMap Layer"));
            Map<String, Object> layer = new LinkedHashMap<>(jsonFlatObject(source.json()));
            layer.put("sourcePath", source.path());
            layer.put("markerCount", jsonObjectArray(source.json(), "markers").size());
            layers.add(Map.copyOf(layer));
            for (Map<String, Object> markerRow : jsonObjectArray(source.json(), "markers")) {
                Map<String, Object> markerRecord = new LinkedHashMap<>(markerRow);
                markerRecord.put("layerId", layerId);
                markerRecord.put("layerName", layerName);
                markerRecord.put("sourcePath", source.path());
                markers.add(Map.copyOf(markerRecord));
            }
        }
        if (markers.isEmpty()) {
            markers = jsonObjectArray(record.json(), "markers");
        }
        Map<String, Object> firstMarker = markers.isEmpty() ? Map.of() : markers.get(0);
        String marker = stringValue(firstMarker, "id", jsonString(record.json(), "marker",
                EchoNativeBootstrapMain.nativeProductId("native_loader_marker")));
        String markerLabel = stringValue(firstMarker, "label", marker);
        String focus = stringValue(firstMarker, "description", jsonString(record.json(), "description", "Native route marker"));
        Map<String, Object> holomap = new LinkedHashMap<>();
        holomap.put("screenId", "echoholomap:holomap");
        holomap.put("layer", id);
        holomap.put("layerName", name);
        holomap.put("marker", marker);
        holomap.put("markerLabel", markerLabel);
        holomap.put("focus", focus);
        holomap.put("layers", List.copyOf(layers));
        holomap.put("layerCount", layers.size());
        holomap.put("markers", markers);
        holomap.put("markerCount", markers.size());
        holomap.put("sourceRoots", sourceRootsFor(
                ROOT_HOLOMAP_LAYERS,
                "data/echoholomap/echoholomap/layers"
        ));
        holomap.put("sourcePath", record.path());
        holomap.put("recordCount", records.size());
        holomap.put("adapterCoreBridge", "echoholomap:holomap");
        return Map.copyOf(holomap);
    }

    static Map<String, Object> wiki() {
        List<SourceRecord> records = resourceRecordsFor(
                ROOT_WIKI_CONTENT,
                "data/echowiki/echowiki/articles",
                "data/echowiki/echowiki/collections",
                "data/echowiki/echowiki/guide_books"
        );
        SourceRecord record = preferred(records, defaultContentId("wikiArticle", "guides/native_loader"));
        String id = jsonString(record.json(), "id", EchoNativeBootstrapMain.nativeProductNamespace());
        String title = jsonString(record.json(), "title", "Native Guide");
        String category = jsonString(record.json(), "category", "guide");
        String summary = jsonString(record.json(), "summary", "Wiki article loaded");
        List<Map<String, Object>> blocks = jsonObjectArray(record.json(), "blocks");
        List<Map<String, Object>> articles = new ArrayList<>();
        for (SourceRecord source : records) {
            String articleId = jsonString(source.json(), "id", source.path());
            Map<String, Object> article = new LinkedHashMap<>(jsonFlatObject(source.json()));
            article.put("sourcePath", source.path());
            article.put("blocks", jsonObjectArray(source.json(), "blocks"));
            article.put("blockCount", jsonObjectArray(source.json(), "blocks").size());
            article.put("relatedArticles", jsonStringArray(source.json(), "relatedArticles"));
            article.put("relatedItems", jsonStringArray(source.json(), "relatedItems"));
            article.put("relatedMissions", jsonStringArray(source.json(), "relatedMissions"));
            article.put("relatedRegions", jsonStringArray(source.json(), "relatedRegions"));
            article.put("link", "wiki:" + articleId.replace(':', '/'));
            articles.add(Map.copyOf(article));
        }
        Map<String, Object> wiki = new LinkedHashMap<>();
        wiki.put("screenId", "echowiki:wiki");
        wiki.put("guide", category);
        wiki.put("page", title);
        wiki.put("summary", summary);
        wiki.put("blocks", blocks);
        wiki.put("blockCount", blocks.size());
        wiki.put("articles", List.copyOf(articles));
        wiki.put("articleCount", articles.size());
        wiki.put("relatedArticles", jsonStringArray(record.json(), "relatedArticles"));
        wiki.put("relatedItems", jsonStringArray(record.json(), "relatedItems"));
        wiki.put("relatedMissions", jsonStringArray(record.json(), "relatedMissions"));
        wiki.put("relatedRegions", jsonStringArray(record.json(), "relatedRegions"));
        wiki.put("link", "wiki:" + id.replace(':', '/'));
        wiki.put("sourceRoots", sourceRootsFor(
                ROOT_WIKI_CONTENT,
                "data/echowiki/echowiki/articles",
                "data/echowiki/echowiki/collections",
                "data/echowiki/echowiki/guide_books"
        ));
        wiki.put("sourcePath", record.path());
        wiki.put("recordCount", records.size());
        wiki.put("adapterCoreBridge", "echowiki:wiki");
        return Map.copyOf(wiki);
    }

    static Map<String, Object> camera() {
        Map<String, Object> holomap = holomap();
        Map<String, Object> camera = new LinkedHashMap<>();
        camera.put("moduleId", "echocameracore");
        camera.put("mode", "over_shoulder");
        camera.put("fov", 72);
        camera.put("target", holomap.get("marker"));
        camera.put("adapterCoreBridge", "echocameracore:camera_frame");
        return Map.copyOf(camera);
    }

    static Map<String, Object> cinematic() {
        Map<String, Object> terminal = terminal();
        Map<String, Object> cinematic = new LinkedHashMap<>();
        cinematic.put("moduleId", "echocinematiccore");
        cinematic.put("cue", terminal.get("title"));
        cinematic.put("letterbox", true);
        cinematic.put("subtitle", terminal.get("summary"));
        cinematic.put("adapterCoreBridge", "echocinematiccore:cinematic_cue");
        return Map.copyOf(cinematic);
    }

    private record SourceRecord(String path, String json) {
    }

    private static List<SourceRecord> resourceRecordsFor(String profileKey, String... fallbackRoots) {
        Map<String, SourceRecord> records = new LinkedHashMap<>();
        for (String root : sourceRootsFor(profileKey, fallbackRoots)) {
            for (SourceRecord record : resourceRecords(root)) {
                records.putIfAbsent(record.path(), record);
            }
        }
        return List.copyOf(records.values());
    }

    private static List<String> sourceRootsFor(String profileKey, String... fallbackRoots) {
        List<String> configured = EchoNativeBootstrapMain.nativeUiDataSourceRoots().get(profileKey);
        List<String> roots = new ArrayList<>();
        if (configured != null) {
            roots.addAll(configured);
        }
        roots.addAll(List.of(fallbackRoots));
        return roots.stream()
                .filter(root -> root != null && !root.isBlank())
                .distinct()
                .toList();
    }

    private static String normalizedMissionId(String id) {
        String safeId = id == null || id.isBlank() ? "secure_crash_outpost" : id.trim();
        return safeId.contains(":") ? safeId : "echoashfallprotocol:" + safeId;
    }

    private static String defaultContentId(String key, String fallback) {
        String configured = EchoNativeBootstrapMain.nativeUiDefaultContentIds().get(key);
        return configured == null || configured.isBlank() ? fallback : configured;
    }

    private static NativeUiActionRoute primaryProductActionRoute() {
        return EchoNativeBootstrapMain.nativeUiActionRoutes().stream()
                .findFirst()
                .orElse(null);
    }

    private static List<String> productActionCommands(NativeUiActionRoute route, String surface) {
        if (route != null && route.commandsByAction() != null && !route.commandsByAction().isEmpty()) {
            return route.commandsByAction().values().stream()
                    .distinct()
                    .toList();
        }
        return EchoNativeBootstrapMain.nativePhysicalActionRoutes().stream()
                .filter(action -> surface.equals(action.surface()))
                .map(NativePhysicalActionRoute::action)
                .distinct()
                .toList();
    }

    private static List<String> productActionKeys(String surface) {
        return EchoNativeBootstrapMain.nativePhysicalActionRoutes().stream()
                .filter(action -> surface.equals(action.surface()))
                .map(NativePhysicalActionRoute::key)
                .distinct()
                .toList();
    }

    private static List<SourceRecord> resourceRecords(String root) {
        List<SourceRecord> cached = cachedResourceRecords(root);
        if (cached != null) {
            return cached;
        }
        Map<String, SourceRecord> records = new LinkedHashMap<>();
        try {
            scanClassLoaderResources(root, records);
            scanClasspath(root, records);
            scanDevFallback(root, records);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
        return cacheResourceRecords(root, records.values().stream()
                .sorted((left, right) -> left.path().compareToIgnoreCase(right.path()))
                .toList());
    }

    private static List<SourceRecord> resourceRecords(String firstRoot, String secondRoot, String... additionalRoots) {
        String cacheKey = resourceCacheKey(firstRoot, secondRoot, additionalRoots);
        List<SourceRecord> cached = cachedResourceRecords(cacheKey);
        if (cached != null) {
            return cached;
        }
        Map<String, SourceRecord> records = new LinkedHashMap<>();
        try {
            scanResourceRoot(firstRoot, records);
            scanResourceRoot(secondRoot, records);
            for (String root : additionalRoots) {
                scanResourceRoot(root, records);
            }
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
        return cacheResourceRecords(cacheKey, records.values().stream()
                .sorted((left, right) -> left.path().compareToIgnoreCase(right.path()))
                .toList());
    }

    private static synchronized List<SourceRecord> cachedResourceRecords(String cacheKey) {
        return RESOURCE_RECORDS_CACHE.get(cacheKey);
    }

    private static synchronized List<SourceRecord> cacheResourceRecords(
            String cacheKey,
            List<SourceRecord> records
    ) {
        RESOURCE_RECORDS_CACHE.put(cacheKey, records);
        return records;
    }

    private static String resourceCacheKey(String firstRoot, String secondRoot, String... additionalRoots) {
        StringBuilder key = new StringBuilder(firstRoot).append("||").append(secondRoot);
        for (String root : additionalRoots) {
            key.append("||").append(root);
        }
        return key.toString();
    }

    private static void scanResourceRoot(String root, Map<String, SourceRecord> records) throws IOException {
        scanClassLoaderResources(root, records);
        scanClasspath(root, records);
        scanDevFallback(root, records);
    }

    private static SourceRecord preferred(List<SourceRecord> records, String pathNeedle) {
        if (records == null || records.isEmpty()) {
            return new SourceRecord("", "{}");
        }
        String needle = normalize(pathNeedle);
        return records.stream()
                .filter(record -> record.path().contains(needle))
                .findFirst()
                .orElse(records.get(0));
    }

    private static String jsonString(String json, String key, String fallback) {
        if (json == null || json.isBlank()) {
            return fallback;
        }
        Pattern pattern = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\"((?:\\\\.|[^\"])*)\"");
        Matcher matcher = pattern.matcher(json);
        if (!matcher.find()) {
            return fallback;
        }
        String value = unescapeJsonString(matcher.group(1));
        return value.isBlank() ? fallback : value;
    }

    private static List<String> jsonStringArray(String json, String key) {
        String array = jsonValueBlock(json, key, '[', ']');
        if (array.isBlank()) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        boolean inString = false;
        boolean escaped = false;
        StringBuilder current = new StringBuilder();
        for (int index = 1; index < array.length() - 1; index++) {
            char character = array.charAt(index);
            if (inString) {
                if (escaped) {
                    current.append('\\').append(character);
                    escaped = false;
                } else if (character == '\\') {
                    escaped = true;
                } else if (character == '"') {
                    String value = unescapeJsonString(current.toString());
                    if (!value.isBlank()) {
                        values.add(value);
                    }
                    current.setLength(0);
                    inString = false;
                } else {
                    current.append(character);
                }
            } else if (character == '"') {
                inString = true;
            }
        }
        return List.copyOf(values);
    }

    private static List<Map<String, Object>> jsonObjectArray(String json, String key) {
        String array = jsonValueBlock(json, key, '[', ']');
        if (array.isBlank()) {
            return List.of();
        }
        List<Map<String, Object>> objects = new ArrayList<>();
        for (int index = 1; index < array.length() - 1; index++) {
            if (array.charAt(index) != '{') {
                continue;
            }
            int end = findMatching(array, index, '{', '}');
            if (end < 0) {
                break;
            }
            objects.add(jsonFlatObject(array.substring(index, end + 1)));
            index = end;
        }
        return List.copyOf(objects);
    }

    private static Map<String, Object> jsonObject(String json, String key) {
        String object = jsonValueBlock(json, key, '{', '}');
        return object.isBlank() ? Map.of() : jsonFlatObject(object);
    }

    private static Map<String, Object> jsonFlatObject(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        Map<String, Object> values = new LinkedHashMap<>();
        Pattern strings = Pattern.compile("\"([A-Za-z0-9_:\\-/]+)\"\\s*:\\s*\"((?:\\\\.|[^\"])*)\"");
        Matcher stringMatcher = strings.matcher(json);
        while (stringMatcher.find()) {
            values.put(stringMatcher.group(1), unescapeJsonString(stringMatcher.group(2)));
        }
        Pattern primitives = Pattern.compile("\"([A-Za-z0-9_:\\-/]+)\"\\s*:\\s*(true|false|-?\\d+(?:\\.\\d+)?)");
        Matcher primitiveMatcher = primitives.matcher(json);
        while (primitiveMatcher.find()) {
            String raw = primitiveMatcher.group(2);
            if ("true".equals(raw) || "false".equals(raw)) {
                values.putIfAbsent(primitiveMatcher.group(1), Boolean.valueOf(raw));
            } else {
                values.putIfAbsent(primitiveMatcher.group(1), raw);
            }
        }
        return Map.copyOf(values);
    }

    private static String jsonValueBlock(String json, String key, char open, char close) {
        if (json == null || json.isBlank()) {
            return "";
        }
        Pattern pattern = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:");
        Matcher matcher = pattern.matcher(json);
        if (!matcher.find()) {
            return "";
        }
        int index = matcher.end();
        while (index < json.length() && Character.isWhitespace(json.charAt(index))) {
            index++;
        }
        if (index >= json.length() || json.charAt(index) != open) {
            return "";
        }
        int end = findMatching(json, index, open, close);
        return end < 0 ? "" : json.substring(index, end + 1);
    }

    private static int findMatching(String value, int start, char open, char close) {
        int depth = 0;
        boolean inString = false;
        boolean escaped = false;
        for (int index = start; index < value.length(); index++) {
            char character = value.charAt(index);
            if (inString) {
                if (escaped) {
                    escaped = false;
                } else if (character == '\\') {
                    escaped = true;
                } else if (character == '"') {
                    inString = false;
                }
                continue;
            }
            if (character == '"') {
                inString = true;
            } else if (character == open) {
                depth++;
            } else if (character == close) {
                depth--;
                if (depth == 0) {
                    return index;
                }
            }
        }
        return -1;
    }

    private static String unescapeJsonString(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("\\\"", "\"")
                .replace("\\\\", "\\")
                .replace("\\n", " ")
                .replace("\\r", " ")
                .replace("\\t", " ");
    }

    private static void scanClassLoaderResources(String root, Map<String, SourceRecord> records) throws IOException {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        if (loader == null) {
            loader = EchoNativeAgent5UiHandlerRegistry.class.getClassLoader();
        }
        java.util.Enumeration<URL> urls = loader.getResources(root);
        while (urls.hasMoreElements()) {
            scanResourceUrl(root, urls.nextElement(), records);
        }
    }

    private static void scanResourceUrl(String root, URL url, Map<String, SourceRecord> records) {
        try {
            if ("file".equals(url.getProtocol())) {
                scanDirectory(root, Path.of(url.toURI()), records);
                return;
            }
            if ("jar".equals(url.getProtocol())) {
                String path = url.getPath();
                int separator = path.indexOf("!/");
                if (separator > 0) {
                    String jarPath = path.substring(0, separator);
                    if (jarPath.startsWith("file:")) {
                        jarPath = URI.create(jarPath).getPath();
                    }
                    scanJar(Path.of(jarPath), root, records);
                }
            }
        } catch (Exception ignored) {
            // Resource scanning is best-effort; classpath and dev fallback scanning run as backups.
        }
    }

    private static void scanClasspath(String root, Map<String, SourceRecord> records) throws IOException {
        scanClasspathEntries(System.getProperty("java.class.path", ""), root, records);
        scanClasspathEntries(System.getProperty("echo.native.moduleClasspath", ""), root, records);
        scanModuleClasspathFile(root, records);
    }

    private static void scanModuleClasspathFile(String root, Map<String, SourceRecord> records) throws IOException {
        String configured = System.getProperty("echo.native.moduleClasspathFile", "");
        if (configured == null || configured.isBlank()) {
            return;
        }
        Path file = Path.of(configured.trim());
        if (!Files.isRegularFile(file)) {
            return;
        }
        String json = Files.readString(file, StandardCharsets.UTF_8);
        scanClasspathEntries(jsonString(json, "moduleClasspath", ""), root, records);
        for (String entry : jsonStringArray(json, "classpathEntries")) {
            scanClasspathEntry(entry, root, records);
        }
    }

    private static void scanClasspathEntries(String classpath, String root, Map<String, SourceRecord> records)
            throws IOException {
        if (classpath == null || classpath.isBlank()) {
            return;
        }
        for (String entry : classpath.split(Pattern.quote(File.pathSeparator))) {
            scanClasspathEntry(entry, root, records);
        }
    }

    private static void scanClasspathEntry(String entry, String root, Map<String, SourceRecord> records)
            throws IOException {
        if (entry == null || entry.isBlank()) {
            return;
        }
        String trimmed = entry.trim();
        if (trimmed.length() >= 2 && trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
            trimmed = trimmed.substring(1, trimmed.length() - 1);
        }
        Path path = Path.of(trimmed);
        if (Files.isDirectory(path)) {
            scanDirectory(root, path.resolve(root), records);
        } else if (trimmed.endsWith(".jar") || trimmed.endsWith(".echo-addon")) {
            scanJar(path, root, records);
        }
    }

    private static void scanDevFallback(String root, Map<String, SourceRecord> records) throws IOException {
        Path cursor = Path.of("").toAbsolutePath();
        for (int depth = 0; cursor != null && depth < 10; depth++) {
            scanDirectory(root, cursor.resolve("src").resolve("main").resolve("resources").resolve(root), records);
            scanDirectory(root, cursor.resolve("Echo").resolve("src").resolve("main").resolve("resources").resolve(root), records);
            scanDirectory(root, cursor.resolve("echo-native-platform").resolve("src").resolve("main").resolve("resources").resolve(root), records);
            scanDirectory(root, cursor.resolve("Echo").resolve("echo-native-platform").resolve("src").resolve("main").resolve("resources").resolve(root), records);
            scanAddonResourceDirectories(root, cursor.resolve("addons"), records);
            scanAddonResourceDirectories(root, cursor.resolve("Echo").resolve("addons"), records);
            scanAddonResourceDirectories(root, cursor.resolve("ECHO-Modules").resolve("addons"), records);
            scanAddonResourceDirectories(root, cursor.resolve("Github").resolve("ECHO-Modules").resolve("addons"), records);
            cursor = cursor.getParent();
        }
    }

    private static void scanAddonResourceDirectories(String root, Path addonsDirectory, Map<String, SourceRecord> records)
            throws IOException {
        if (!Files.isDirectory(addonsDirectory)) {
            return;
        }
        try (var stream = Files.list(addonsDirectory)) {
            for (Path addon : stream.filter(Files::isDirectory).toList()) {
                scanDirectory(root, addon.resolve("src").resolve("main").resolve("resources").resolve(root), records);
            }
        }
    }

    private static void scanDirectory(String root, Path directory, Map<String, SourceRecord> records) throws IOException {
        if (!Files.isDirectory(directory)) {
            return;
        }
        try (var stream = Files.walk(directory)) {
            stream.filter(path -> Files.isRegularFile(path) && path.getFileName().toString().endsWith(".json"))
                    .forEach(path -> {
                        try {
                            String relative = root + "/" + directory.relativize(path).toString().replace('\\', '/');
                            records.putIfAbsent(relative, new SourceRecord(relative, Files.readString(path, StandardCharsets.UTF_8)));
                        } catch (IOException exception) {
                            throw new UncheckedIOException(exception);
                        }
                    });
        } catch (UncheckedIOException exception) {
            throw exception.getCause();
        }
    }

    private static void scanJar(Path jarPath, String root, Map<String, SourceRecord> records) throws IOException {
        if (!Files.isRegularFile(jarPath)) {
            return;
        }
        try (JarFile jar = new JarFile(jarPath.toFile())) {
            var entries = jar.entries();
            while (entries.hasMoreElements()) {
                var entry = entries.nextElement();
                String name = entry.getName();
                if (entry.isDirectory() || !name.startsWith(root + "/") || !name.endsWith(".json")) {
                    if (!entry.isDirectory() && name.startsWith("lib/") && name.endsWith(".jar")) {
                        try (InputStream input = jar.getInputStream(entry)) {
                            scanNestedJar(input, root, records);
                        }
                    }
                    continue;
                }
                try (InputStream input = jar.getInputStream(entry)) {
                    String json = new String(input.readAllBytes(), StandardCharsets.UTF_8);
                    records.putIfAbsent(name, new SourceRecord(name, json));
                }
            }
        }
    }

    private static void scanNestedJar(InputStream input, String root, Map<String, SourceRecord> records)
            throws IOException {
        try (JarInputStream nested = new JarInputStream(input)) {
            java.util.jar.JarEntry entry;
            while ((entry = nested.getNextJarEntry()) != null) {
                String name = entry.getName();
                if (entry.isDirectory() || !name.startsWith(root + "/") || !name.endsWith(".json")) {
                    continue;
                }
                String json = new String(nested.readAllBytes(), StandardCharsets.UTF_8);
                records.putIfAbsent(name, new SourceRecord(name, json));
            }
        }
    }

    private static Map<String, Object> result(
            String moduleId,
            String serviceId,
            String surfaceId,
            boolean handled,
            String input,
            String output,
            List<String> effects
    ) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("moduleId", moduleId);
        result.put("serviceId", serviceId);
        result.put("surfaceId", surfaceId);
        result.put("handled", handled);
        result.put("input", input);
        result.put("output", output);
        result.put("effects", effects);
        result.put("adapterCoreBridge", true);
        result.put("serviceCodeExecuted", true);
        return Map.copyOf(result);
    }

    private static Map<String, Object> notification(String id, String severity, String message, String anchor) {
        Map<String, Object> notification = new LinkedHashMap<>();
        notification.put("id", id);
        notification.put("severity", severity);
        notification.put("message", message);
        notification.put("anchor", anchor);
        notification.put("delivered", true);
        notification.put("adapterCoreBridge", "echonotificationcore:queue");
        return Map.copyOf(notification);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> object(Object value) {
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Map.of();
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> objects(Object value) {
        if (value instanceof List<?> list) {
            List<Map<String, Object>> objects = new ArrayList<>();
            for (Object item : list) {
                if (item instanceof Map<?, ?> map) {
                    objects.add((Map<String, Object>) map);
                }
            }
            return List.copyOf(objects);
        }
        return List.of();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private static String titleFromPath(String path) {
        String value = path == null ? "" : path.trim();
        int slash = value.lastIndexOf('/');
        if (slash >= 0 && slash + 1 < value.length()) {
            value = value.substring(slash + 1);
        }
        if (value.endsWith(".json")) {
            value = value.substring(0, value.length() - ".json".length());
        }
        StringBuilder title = new StringBuilder();
        for (String word : value.replace('-', '_').split("_+")) {
            if (word.isBlank()) {
                continue;
            }
            if (!title.isEmpty()) {
                title.append(' ');
            }
            title.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) {
                title.append(word.substring(1));
            }
        }
        return title.isEmpty() ? "Native Machine Route" : title.toString();
    }

    private static String stringValue(Map<String, Object> values, String key, String fallback) {
        Object value = values == null ? null : values.get(key);
        if (value == null) {
            return fallback;
        }
        String text = String.valueOf(value).trim();
        return text.isBlank() ? fallback : text;
    }

    private static boolean matchesIndexEntry(Map<String, Object> entry, String query) {
        if (query == null || query.isBlank()) {
            return false;
        }
        String needle = query.toLowerCase(java.util.Locale.ROOT);
        if (stringValue(entry, "id", "").toLowerCase(java.util.Locale.ROOT).contains(needle)
                || stringValue(entry, "category", "").toLowerCase(java.util.Locale.ROOT).contains(needle)
                || stringValue(entry, "title", "").toLowerCase(java.util.Locale.ROOT).contains(needle)
                || stringValue(entry, "subtitle", "").toLowerCase(java.util.Locale.ROOT).contains(needle)
                || stringValue(entry, "summary", "").toLowerCase(java.util.Locale.ROOT).contains(needle)) {
            return true;
        }
        for (String body : stringList(entry.get("body"))) {
            if (body.toLowerCase(java.util.Locale.ROOT).contains(needle)) {
                return true;
            }
        }
        for (String tag : stringList(entry.get("tags"))) {
            if (tag.toLowerCase(java.util.Locale.ROOT).contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private static boolean matchesRecord(Map<String, Object> record, String query) {
        if (query == null || query.isBlank()) {
            return false;
        }
        String needle = query.toLowerCase(java.util.Locale.ROOT);
        for (String key : List.of("id", "title", "subtitle", "summary", "category", "sourcePath")) {
            if (stringValue(record, key, "").toLowerCase(java.util.Locale.ROOT).contains(needle)) {
                return true;
            }
        }
        for (String step : stringList(record.get("nextSteps"))) {
            if (step.toLowerCase(java.util.Locale.ROOT).contains(needle)) {
                return true;
            }
        }
        for (Map<String, Object> panel : objects(record.get("panels"))) {
            if (stringValue(panel, "title", "").toLowerCase(java.util.Locale.ROOT).contains(needle)
                    || stringValue(panel, "body", "").toLowerCase(java.util.Locale.ROOT).contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private static List<String> stringList(Object value) {
        if (value instanceof List<?> list) {
            List<String> strings = new ArrayList<>();
            for (Object item : list) {
                strings.add(String.valueOf(item));
            }
            return List.copyOf(strings);
        }
        return List.of();
    }

}
