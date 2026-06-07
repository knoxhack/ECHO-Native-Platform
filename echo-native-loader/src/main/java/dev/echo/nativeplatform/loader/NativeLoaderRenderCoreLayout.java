package dev.echo.nativeplatform.loader;

import java.util.LinkedHashMap;
import java.util.Map;

public final class NativeLoaderRenderCoreLayout {
    public static final String SERVICE_ID = "echo.native.render_core_layout";

    private NativeLoaderRenderCoreLayout() {
    }

    public static Map<String, Object> compute(int viewportWidth, int viewportHeight, int headerCount, int bodyCount) {
        int safeWidth = Math.max(120, viewportWidth);
        int safeHeight = Math.max(120, viewportHeight);
        int panelW = Math.min(Math.max(300, safeWidth - 44), 620);
        int panelH = Math.min(Math.max(210, safeHeight - 48), 360);
        int x = Math.max(0, (safeWidth - panelW) / 2);
        int y = Math.max(0, (safeHeight - panelH) / 2);
        int headerStartY = y + 40;
        int bodyY = y + 156;
        int footerY = y + panelH - 24;
        int textMaxWidth = Math.max(80, safeWidth - (x + 14) - 28);
        int bodyLineBudget = Math.max(1, ((footerY - bodyY - 1) / 20) + 1);
        int bodyLinesRendered = Math.min(Math.max(0, bodyCount), bodyLineBudget);
        boolean headerBodySeparated = headerCount <= 0 || headerStartY + ((headerCount - 1) * 20) < bodyY;
        boolean bodyFooterSeparated = bodyLinesRendered <= 0 || bodyY + ((bodyLinesRendered - 1) * 20) < footerY;

        Map<String, Object> layout = new LinkedHashMap<>();
        layout.put("moduleId", "echorendercore");
        layout.put("nativeRenderCoreLayoutServiceId", SERVICE_ID);
        layout.put("viewportWidth", safeWidth);
        layout.put("viewportHeight", safeHeight);
        layout.put("panelW", panelW);
        layout.put("panelH", panelH);
        layout.put("x", x);
        layout.put("y", y);
        layout.put("headerStartY", headerStartY);
        layout.put("bodyY", bodyY);
        layout.put("footerY", footerY);
        layout.put("textMaxWidth", textMaxWidth);
        layout.put("bodyLineBudget", bodyLineBudget);
        layout.put("bodyLinesRendered", bodyLinesRendered);
        layout.put("headerBodySeparated", headerBodySeparated);
        layout.put("bodyFooterSeparated", bodyFooterSeparated);
        layout.put("adapterCoreBridge", true);
        layout.put("serviceCodeExecuted", true);
        return Map.copyOf(layout);
    }
}
