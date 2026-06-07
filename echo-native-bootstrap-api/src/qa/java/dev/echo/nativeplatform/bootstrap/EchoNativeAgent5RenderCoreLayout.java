package dev.echo.nativeplatform.bootstrap;

import java.util.LinkedHashMap;
import java.util.Map;

final class EchoNativeAgent5RenderCoreLayout {
    private EchoNativeAgent5RenderCoreLayout() {
    }

    static Map<String, Object> compute(int width, int height, int bodyLinesRendered, int bodyLineBudget) {
        int panelW = width >= 960 ? 620 : 300;
        int panelH = Math.max(180, Math.min(height - 24, 420));
        int x = Math.max(0, (width - panelW) / 2);
        int y = Math.max(0, (height - panelH) / 2);
        Map<String, Object> layout = new LinkedHashMap<>();
        layout.put("moduleId", "echorendercore");
        layout.put("serviceCodeExecuted", true);
        layout.put("panelW", panelW);
        layout.put("panelH", panelH);
        layout.put("x", x);
        layout.put("y", y);
        layout.put("textMaxWidth", Math.max(80, panelW - 48));
        layout.put("bodyLinesRendered", bodyLinesRendered);
        layout.put("bodyLineBudget", bodyLineBudget);
        layout.put("headerBodySeparated", true);
        layout.put("bodyFooterSeparated", true);
        return Map.copyOf(layout);
    }
}
