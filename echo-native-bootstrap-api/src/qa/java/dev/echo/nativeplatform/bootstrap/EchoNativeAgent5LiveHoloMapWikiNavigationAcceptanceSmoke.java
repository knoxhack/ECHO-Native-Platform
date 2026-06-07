package dev.echo.nativeplatform.bootstrap;

import java.util.LinkedHashMap;
import java.util.Map;

public final class EchoNativeAgent5LiveHoloMapWikiNavigationAcceptanceSmoke {
    private EchoNativeAgent5LiveHoloMapWikiNavigationAcceptanceSmoke() {
    }

    public static Map<String, Object> capture() {
        Map<String, Object> holomap = object(EchoNativeAgent5HoloMapEndToEndAcceptanceSmoke.capture().get("accepted"));
        Map<String, Object> wiki = object(EchoNativeAgent5WikiEndToEndAcceptanceSmoke.capture().get("accepted"));
        Map<String, Object> accepted = EchoNativeAgent5LiveHoloMapWikiNavigationAcceptance.assess(holomap, wiki);
        Map<String, Object> rejectedNoHoloMap = EchoNativeAgent5LiveHoloMapWikiNavigationAcceptance.assess(
                Map.of("accepted", false),
                wiki
        );
        Map<String, Object> rejectedNoWiki = EchoNativeAgent5LiveHoloMapWikiNavigationAcceptance.assess(
                holomap,
                Map.of("accepted", false)
        );
        boolean passed = Boolean.TRUE.equals(accepted.get("accepted"))
                && "live_holomap_wiki_navigation:accepted:J/MODULE_ROUTE".equals(accepted.get("effect"))
                && Boolean.FALSE.equals(rejectedNoHoloMap.get("accepted"))
                && Boolean.FALSE.equals(rejectedNoWiki.get("accepted"));

        Map<String, Object> smoke = new LinkedHashMap<>();
        smoke.put("liveHoloMapWikiNavigationAcceptanceSmokeClass",
                EchoNativeAgent5LiveHoloMapWikiNavigationAcceptanceSmoke.class.getSimpleName());
        smoke.put("accepted", accepted);
        smoke.put("rejectedNoHoloMap", rejectedNoHoloMap);
        smoke.put("rejectedNoWiki", rejectedNoWiki);
        smoke.put("adapterCoreBridge", true);
        smoke.put("serviceCodeExecuted", true);
        smoke.put("passed", passed);
        return Map.copyOf(smoke);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> object(Object value) {
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Map.of();
    }
}
