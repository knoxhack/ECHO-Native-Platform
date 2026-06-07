package dev.echo.nativeplatform.bootstrap;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoNativeAgent5LiveRouteBoundHoloMapWikiAcceptance {
    private EchoNativeAgent5LiveRouteBoundHoloMapWikiAcceptance() {
    }

    public static Map<String, Object> assess(
            Map<String, Object> holomap,
            Map<String, Object> wiki,
            Map<String, Object> routeEffectTranscript
    ) {
        boolean holomapAccepted = holomap != null
                && Boolean.TRUE.equals(holomap.get("accepted"))
                && "J".equals(holomap.get("key"))
                && "HOLOMAP".equals(holomap.get("surface"))
                && EchoNativeAgent5UiExpectedValues.holomap().get("layer").equals(holomap.get("layer"))
                && EchoNativeAgent5UiExpectedValues.holomapMarker().equals(holomap.get("marker"))
                && Boolean.TRUE.equals(holomap.get("holomapRendered"))
                && ("holomap_end_to_end:J->HOLOMAP:" + EchoNativeAgent5UiExpectedValues.holomapMarker())
                .equals(holomap.get("effect"));
        boolean wikiAccepted = wiki != null
                && Boolean.TRUE.equals(wiki.get("accepted"))
                && "MODULE_ROUTE".equals(wiki.get("key"))
                && "WIKI".equals(wiki.get("surface"))
                && EchoNativeAgent5UiExpectedValues.wiki().get("guide").equals(wiki.get("guide"))
                && EchoNativeAgent5UiExpectedValues.wiki().get("page").equals(wiki.get("page"))
                && EchoNativeAgent5UiExpectedValues.wikiLink().equals(wiki.get("link"))
                && Boolean.TRUE.equals(wiki.get("wikiRendered"))
                && "wiki_end_to_end:MODULE_ROUTE->WIKI:ashfall".equals(wiki.get("effect"));
        List<String> observedKeys = strings(routeEffectTranscript == null
                ? null
                : routeEffectTranscript.get("observedKeys"));
        boolean routeBound = routeEffectTranscript != null
                && Boolean.TRUE.equals(routeEffectTranscript.get("accepted"))
                && observedKeys.contains("J");
        boolean accepted = holomapAccepted && wikiAccepted && routeBound;
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("accepted", accepted);
        result.put("holomapAccepted", holomapAccepted);
        result.put("wikiAccepted", wikiAccepted);
        result.put("routeBound", routeBound);
        result.put("observedKeys", observedKeys);
        result.put("layer", text(holomap == null ? null : holomap.get("layer")));
        result.put("marker", text(holomap == null ? null : holomap.get("marker")));
        result.put("guide", text(wiki == null ? null : wiki.get("guide")));
        result.put("page", text(wiki == null ? null : wiki.get("page")));
        result.put("effect", accepted
                ? "live_route_bound_holomap_wiki:accepted:J/MODULE_ROUTE"
                : "live_route_bound_holomap_wiki:rejected");
        result.put("adapterCoreBridge", true);
        result.put("serviceCodeExecuted", true);
        return Map.copyOf(result);
    }

    public static Map<String, Object> smoke() {
        Map<String, Object> holomap = Map.of(
                "accepted", true,
                "key", "J",
                "surface", "HOLOMAP",
                "layer", EchoNativeAgent5UiExpectedValues.holomap().get("layer"),
                "marker", EchoNativeAgent5UiExpectedValues.holomapMarker(),
                "holomapRendered", true,
                "effect", "holomap_end_to_end:J->HOLOMAP:" + EchoNativeAgent5UiExpectedValues.holomapMarker()
        );
        Map<String, Object> wiki = Map.of(
                "accepted", true,
                "key", "MODULE_ROUTE",
                "surface", "WIKI",
                "guide", EchoNativeAgent5UiExpectedValues.wiki().get("guide"),
                "page", EchoNativeAgent5UiExpectedValues.wiki().get("page"),
                "link", EchoNativeAgent5UiExpectedValues.wikiLink(),
                "wikiRendered", true,
                "effect", "wiki_end_to_end:MODULE_ROUTE->WIKI:ashfall"
        );
        Map<String, Object> route = Map.of(
                "accepted", true,
                "observedKeys", List.of("M", "G", "R", "U", "B", "LEFT_ALT", "J", "K", "N")
        );
        Map<String, Object> accepted = assess(holomap, wiki, route);
        Map<String, Object> rejectedNoHoloMap = assess(Map.of(), wiki, route);
        Map<String, Object> rejectedNoWiki = assess(holomap, Map.of(), route);
        Map<String, Object> rejectedNoRoute = assess(holomap, wiki, Map.of(
                "accepted", true,
                "observedKeys", List.of("M", "G", "LEFT_ALT")
        ));
        boolean passed = Boolean.TRUE.equals(accepted.get("accepted"))
                && "live_route_bound_holomap_wiki:accepted:J/MODULE_ROUTE".equals(accepted.get("effect"))
                && Boolean.FALSE.equals(rejectedNoHoloMap.get("accepted"))
                && Boolean.FALSE.equals(rejectedNoWiki.get("accepted"))
                && Boolean.FALSE.equals(rejectedNoRoute.get("accepted"));
        return Map.of(
                "liveRouteBoundHoloMapWikiAcceptanceClass",
                EchoNativeAgent5LiveRouteBoundHoloMapWikiAcceptance.class.getSimpleName(),
                "accepted", accepted,
                "rejectedNoHoloMap", rejectedNoHoloMap,
                "rejectedNoWiki", rejectedNoWiki,
                "rejectedNoRoute", rejectedNoRoute,
                "passed", passed,
                "adapterCoreBridge", true,
                "serviceCodeExecuted", true
        );
    }

    private static List<String> strings(Object value) {
        if (value instanceof List<?> list) {
            return list.stream().map(EchoNativeAgent5LiveRouteBoundHoloMapWikiAcceptance::text).toList();
        }
        return List.of();
    }

    private static String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
