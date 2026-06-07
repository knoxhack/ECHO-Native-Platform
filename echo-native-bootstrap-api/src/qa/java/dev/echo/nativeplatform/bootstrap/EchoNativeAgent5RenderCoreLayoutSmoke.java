package dev.echo.nativeplatform.bootstrap;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoNativeAgent5RenderCoreLayoutSmoke {
    private EchoNativeAgent5RenderCoreLayoutSmoke() {
    }

    public static Map<String, Object> capture() {
        Map<String, Object> desktop = EchoNativeAgent5RenderCoreLayout.compute(1280, 720, 6, 12);
        Map<String, Object> compact = EchoNativeAgent5RenderCoreLayout.compute(320, 240, 6, 12);
        boolean passed = Boolean.TRUE.equals(desktop.get("serviceCodeExecuted"))
                && Boolean.TRUE.equals(compact.get("serviceCodeExecuted"))
                && "echorendercore".equals(desktop.get("moduleId"))
                && Integer.valueOf(620).equals(desktop.get("panelW"))
                && Integer.valueOf(300).equals(compact.get("panelW"))
                && intValue(compact.get("x")) >= 0
                && intValue(compact.get("y")) >= 0
                && intValue(compact.get("textMaxWidth")) >= 80
                && intValue(compact.get("bodyLinesRendered")) <= intValue(compact.get("bodyLineBudget"))
                && Boolean.TRUE.equals(desktop.get("headerBodySeparated"))
                && Boolean.TRUE.equals(desktop.get("bodyFooterSeparated"))
                && Boolean.TRUE.equals(compact.get("headerBodySeparated"))
                && Boolean.TRUE.equals(compact.get("bodyFooterSeparated"));
        Map<String, Object> smoke = new LinkedHashMap<>();
        smoke.put("renderCoreLayoutSmokeClass", EchoNativeAgent5RenderCoreLayoutSmoke.class.getSimpleName());
        smoke.put("layouts", List.of(desktop, compact));
        smoke.put("desktopPanelW", desktop.get("panelW"));
        smoke.put("compactPanelW", compact.get("panelW"));
        smoke.put("compactTextMaxWidth", compact.get("textMaxWidth"));
        smoke.put("compactBodyLinesRendered", compact.get("bodyLinesRendered"));
        smoke.put("adapterCoreBridge", true);
        smoke.put("serviceCodeExecuted", true);
        smoke.put("passed", passed);
        return Map.copyOf(smoke);
    }

    private static int intValue(Object value) {
        return value instanceof Number number ? number.intValue() : 0;
    }
}
