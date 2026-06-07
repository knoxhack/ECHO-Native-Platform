package dev.echo.nativeplatform.bootstrap;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoNativeAgent5Phase5UiParityAcceptanceSmoke {
    private EchoNativeAgent5Phase5UiParityAcceptanceSmoke() {
    }

    public static Map<String, Object> capture() {
        Map<String, Object> terminal = EchoNativeAgent5TerminalEndToEndAcceptanceSmoke.capture();
        Map<String, Object> index = EchoNativeAgent5IndexEndToEndAcceptanceSmoke.capture();
        Map<String, Object> lens = EchoNativeAgent5LensEndToEndAcceptanceSmoke.capture();
        Map<String, Object> hud = EchoNativeAgent5HudOverlayEndToEndAcceptanceSmoke.capture();
        Map<String, Object> holomap = EchoNativeAgent5HoloMapEndToEndAcceptanceSmoke.capture();
        Map<String, Object> wiki = EchoNativeAgent5WikiEndToEndAcceptanceSmoke.capture();
        Map<String, Object> mainMenu = EchoNativeAgent5MainMenuEndToEndAcceptanceSmoke.capture();
        Map<String, Object> uiHost = EchoNativeAgent5UiHostEndToEndAcceptanceSmoke.capture();

        boolean terminalEffect = effectEquals(terminal,
                "terminal_end_to_end:M->TERMINAL:" + EchoNativeAgent5UiExpectedValues.terminalCommand());
        boolean indexEffect = effectEquals(index,
                "index_end_to_end:G->INDEX:" + EchoNativeAgent5UiExpectedValues.indexQuery());
        boolean lensEffect = effectEquals(lens,
                "lens_end_to_end:LEFT_ALT->LENS:" + EchoNativeAgent5UiExpectedValues.lensTarget());
        boolean hudEffect = effectEquals(hud, "hud_overlay_end_to_end:data_backed:85");
        boolean holomapEffect = effectEquals(holomap,
                "holomap_end_to_end:J->HOLOMAP:" + EchoNativeAgent5UiExpectedValues.holomapMarker());
        boolean wikiEffect = effectEquals(wiki, "wiki_end_to_end:MODULE_ROUTE->WIKI:ashfall");
        boolean mainMenuEffect = effectEquals(mainMenu, "main_menu_end_to_end:accepted:4");
        boolean noScreenCrash = Boolean.TRUE.equals(uiHost.get("passed"))
                && effectEquals(uiHost, "ui_host_end_to_end:M->TERMINAL:10");

        List<Map<String, Object>> checklist = List.of(
                checklistItem("terminal_opens", Boolean.TRUE.equals(terminal.get("passed")) && terminalEffect),
                checklistItem("terminal_command_executes", terminalEffect),
                checklistItem("index_opens_and_searches", Boolean.TRUE.equals(index.get("passed")) && indexEffect),
                checklistItem("lens_scans_target", Boolean.TRUE.equals(lens.get("passed")) && lensEffect),
                checklistItem("hud_updates_health_hazard_mission", Boolean.TRUE.equals(hud.get("passed")) && hudEffect),
                checklistItem("holomap_opens", Boolean.TRUE.equals(holomap.get("passed")) && holomapEffect),
                checklistItem("wiki_page_opens", Boolean.TRUE.equals(wiki.get("passed")) && wikiEffect),
                checklistItem("custom_main_menu_appears",
                        Boolean.TRUE.equals(mainMenu.get("passed")) && mainMenuEffect),
                checklistItem("no_screen_crash", noScreenCrash)
        );
        boolean passed = checklist.stream().allMatch(item -> Boolean.TRUE.equals(item.get("passed")))
                && serviceExecuted(terminal)
                && serviceExecuted(index)
                && serviceExecuted(lens)
                && serviceExecuted(hud)
                && serviceExecuted(holomap)
                && serviceExecuted(wiki)
                && serviceExecuted(mainMenu)
                && serviceExecuted(uiHost);

        Map<String, Object> smoke = new LinkedHashMap<>();
        smoke.put("phase5UiParityAcceptanceSmokeClass",
                EchoNativeAgent5Phase5UiParityAcceptanceSmoke.class.getSimpleName());
        smoke.put("checklist", checklist);
        smoke.put("terminalEffect", acceptedEffect(terminal));
        smoke.put("indexEffect", acceptedEffect(index));
        smoke.put("lensEffect", acceptedEffect(lens));
        smoke.put("hudEffect", acceptedEffect(hud));
        smoke.put("holomapEffect", acceptedEffect(holomap));
        smoke.put("wikiEffect", acceptedEffect(wiki));
        smoke.put("mainMenuEffect", acceptedEffect(mainMenu));
        smoke.put("uiHostEffect", acceptedEffect(uiHost));
        smoke.put("mainMenuAccepted", mainMenu.get("accepted"));
        smoke.put("uiHostAccepted", uiHost.get("accepted"));
        smoke.put("adapterCoreBridge", true);
        smoke.put("serviceCodeExecuted", true);
        smoke.put("passed", passed);
        return Map.copyOf(smoke);
    }

    private static Map<String, Object> checklistItem(String id, boolean passed) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", id);
        item.put("passed", passed);
        return Map.copyOf(item);
    }

    private static boolean effectEquals(Map<String, Object> smoke, String expected) {
        return expected.equals(acceptedEffect(smoke));
    }

    private static String acceptedEffect(Map<String, Object> smoke) {
        Object accepted = smoke.get("accepted");
        if (accepted instanceof Map<?, ?> map) {
            return String.valueOf(map.get("effect"));
        }
        return "";
    }

    private static boolean serviceExecuted(Map<String, Object> smoke) {
        return Boolean.TRUE.equals(smoke.get("serviceCodeExecuted"));
    }
}
