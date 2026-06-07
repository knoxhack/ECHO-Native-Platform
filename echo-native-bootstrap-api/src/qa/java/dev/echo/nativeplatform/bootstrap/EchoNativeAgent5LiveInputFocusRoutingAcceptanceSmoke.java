package dev.echo.nativeplatform.bootstrap;

import java.util.LinkedHashMap;
import java.util.Map;

public final class EchoNativeAgent5LiveInputFocusRoutingAcceptanceSmoke {
    private EchoNativeAgent5LiveInputFocusRoutingAcceptanceSmoke() {
    }

    public static Map<String, Object> capture() {
        Map<String, Object> focus = EchoNativeAgent5FocusManagerSmoke.capture();
        Map<String, Object> editing = EchoNativeAgent5TextEditingSmoke.capture();
        Map<String, Object> mouse = EchoNativeAgent5MouseActivationSmoke.capture();
        Map<String, Object> list = EchoNativeAgent5ListNavigationSmoke.capture();
        Map<String, Object> accepted = EchoNativeAgent5LiveInputFocusRoutingAcceptance.assess(
                focus,
                editing,
                mouse,
                list
        );
        Map<String, Object> rejectedNoFocus = EchoNativeAgent5LiveInputFocusRoutingAcceptance.assess(
                Map.of("passed", false),
                editing,
                mouse,
                list
        );
        Map<String, Object> rejectedNoEditing = EchoNativeAgent5LiveInputFocusRoutingAcceptance.assess(
                focus,
                Map.of("passed", false),
                mouse,
                list
        );
        Map<String, Object> rejectedNoMouse = EchoNativeAgent5LiveInputFocusRoutingAcceptance.assess(
                focus,
                editing,
                Map.of("passed", false),
                list
        );
        Map<String, Object> rejectedNoList = EchoNativeAgent5LiveInputFocusRoutingAcceptance.assess(
                focus,
                editing,
                mouse,
                Map.of("passed", false)
        );
        boolean passed = Boolean.TRUE.equals(accepted.get("accepted"))
                && "live_input_focus_routing:accepted:focus/text/mouse/list".equals(accepted.get("effect"))
                && Boolean.FALSE.equals(rejectedNoFocus.get("accepted"))
                && Boolean.FALSE.equals(rejectedNoEditing.get("accepted"))
                && Boolean.FALSE.equals(rejectedNoMouse.get("accepted"))
                && Boolean.FALSE.equals(rejectedNoList.get("accepted"));

        Map<String, Object> smoke = new LinkedHashMap<>();
        smoke.put("liveInputFocusRoutingAcceptanceSmokeClass",
                EchoNativeAgent5LiveInputFocusRoutingAcceptanceSmoke.class.getSimpleName());
        smoke.put("accepted", accepted);
        smoke.put("rejectedNoFocus", rejectedNoFocus);
        smoke.put("rejectedNoEditing", rejectedNoEditing);
        smoke.put("rejectedNoMouse", rejectedNoMouse);
        smoke.put("rejectedNoList", rejectedNoList);
        smoke.put("adapterCoreBridge", true);
        smoke.put("serviceCodeExecuted", true);
        smoke.put("passed", passed);
        return Map.copyOf(smoke);
    }
}
