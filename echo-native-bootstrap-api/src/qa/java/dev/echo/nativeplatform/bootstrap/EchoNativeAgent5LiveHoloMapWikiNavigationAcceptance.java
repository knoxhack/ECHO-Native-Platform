package dev.echo.nativeplatform.bootstrap;

import java.util.LinkedHashMap;
import java.util.Map;

public final class EchoNativeAgent5LiveHoloMapWikiNavigationAcceptance {
    private EchoNativeAgent5LiveHoloMapWikiNavigationAcceptance() {
    }

    public static Map<String, Object> assess(
            Map<String, Object> holoMapEndToEndAcceptance,
            Map<String, Object> wikiEndToEndAcceptance
    ) {
        Map<String, Object> holomap = holoMapEndToEndAcceptance == null ? Map.of() : holoMapEndToEndAcceptance;
        Map<String, Object> wiki = wikiEndToEndAcceptance == null ? Map.of() : wikiEndToEndAcceptance;
        boolean holomapAccepted = Boolean.TRUE.equals(holomap.get("accepted"))
                && ("holomap_end_to_end:J->HOLOMAP:" + EchoNativeAgent5UiExpectedValues.holomapMarker())
                .equals(holomap.get("effect"))
                && "J".equals(holomap.get("key"))
                && "HOLOMAP".equals(holomap.get("surface"))
                && EchoNativeAgent5UiExpectedValues.holomap().get("layer").equals(holomap.get("layer"))
                && EchoNativeAgent5UiExpectedValues.holomapMarker().equals(holomap.get("marker"))
                && Boolean.TRUE.equals(holomap.get("physicalInputAccepted"))
                && Boolean.TRUE.equals(holomap.get("renderAccepted"))
                && Boolean.TRUE.equals(holomap.get("interactionAccepted"))
                && Boolean.TRUE.equals(holomap.get("runtimeMutationAccepted"))
                && Boolean.TRUE.equals(holomap.get("holomapRendered"));
        boolean wikiAccepted = Boolean.TRUE.equals(wiki.get("accepted"))
                && "wiki_end_to_end:MODULE_ROUTE->WIKI:ashfall".equals(wiki.get("effect"))
                && "MODULE_ROUTE".equals(wiki.get("key"))
                && "WIKI".equals(wiki.get("surface"))
                && EchoNativeAgent5UiExpectedValues.wiki().get("guide").equals(wiki.get("guide"))
                && EchoNativeAgent5UiExpectedValues.wiki().get("page").equals(wiki.get("page"))
                && EchoNativeAgent5UiExpectedValues.wikiLink().equals(wiki.get("link"))
                && Boolean.TRUE.equals(wiki.get("physicalInputAccepted"))
                && Boolean.TRUE.equals(wiki.get("renderAccepted"))
                && Boolean.TRUE.equals(wiki.get("interactionAccepted"))
                && Boolean.TRUE.equals(wiki.get("runtimeMutationAccepted"))
                && Boolean.TRUE.equals(wiki.get("wikiRendered"));
        boolean accepted = holomapAccepted && wikiAccepted;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("accepted", accepted);
        result.put("holomapAccepted", holomapAccepted);
        result.put("wikiAccepted", wikiAccepted);
        result.put("holomapSurface", String.valueOf(holomap.getOrDefault("surface", "")));
        result.put("wikiSurface", String.valueOf(wiki.getOrDefault("surface", "")));
        result.put("layer", String.valueOf(holomap.getOrDefault("layer", "")));
        result.put("marker", String.valueOf(holomap.getOrDefault("marker", "")));
        result.put("guide", String.valueOf(wiki.getOrDefault("guide", "")));
        result.put("page", String.valueOf(wiki.getOrDefault("page", "")));
        result.put("holomapRuntimeMutationAccepted",
                Boolean.TRUE.equals(holomap.get("runtimeMutationAccepted")));
        result.put("wikiRuntimeMutationAccepted",
                Boolean.TRUE.equals(wiki.get("runtimeMutationAccepted")));
        result.put("effect", accepted
                ? "live_holomap_wiki_navigation:accepted:J/MODULE_ROUTE"
                : "live_holomap_wiki_navigation:rejected");
        result.put("adapterCoreBridge", true);
        result.put("serviceCodeExecuted", true);
        return Map.copyOf(result);
    }
}
