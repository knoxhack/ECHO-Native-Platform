package dev.echo.nativeplatform.bootstrap;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoNativeAgent5MissionLogUpdateSmoke {
    private EchoNativeAgent5MissionLogUpdateSmoke() {
    }

    public static Map<String, Object> capture() {
        Map<String, Object> dataSources = EchoNativeAgent5UiHandlerRegistry.dataSources();
        Map<String, Object> update = EchoNativeAgent5UiActionRouter.routeMissionLogUpdate(Map.of(
                "missionProgress", 0.25D,
                "missionStatus", "TRACKED"
        ));
        Map<String, Object> rendered = EchoNativeAgent5SurfaceRenderer.render("MISSION_LOG", update, dataSources);
        Map<String, Object> missionMutation = EchoNativeBootstrapMain.executeNativeMissionLogUpdateFromUi(
                String.valueOf(update.get("missionId")),
                String.valueOf(update.get("missionTitle")),
                String.valueOf(update.get("missionObjective")),
                doubleValue(update.get("missionProgress")),
                String.valueOf(update.get("missionStatus")),
                String.valueOf(update.get("missionUpdateLine"))
        );
        boolean runtimeMutationAccepted = runtimeMutationAccepted(
                missionMutation,
                "native.ui.mission_log_update",
                "mission.objective_completed"
        );

        boolean passed = Boolean.TRUE.equals(update.get("handled"))
                && "echoashfallprotocol:secure_crash_outpost".equals(update.get("missionId"))
                && Double.valueOf(0.5D).equals(update.get("missionProgress"))
                && "UPDATED".equals(update.get("missionStatus"))
                && "Drop pod signal confirmed".equals(update.get("missionUpdateLine"))
                && "mission:update:echoashfallprotocol:secure_crash_outpost".equals(update.get("effect"))
                && runtimeMutationAccepted
                && lines(rendered).stream().anyMatch(line -> line.contains("Status: UPDATED    Progress: 50%"))
                && lines(rendered).stream().anyMatch(line -> line.contains("Update: Drop pod signal confirmed"));

        Map<String, Object> smoke = new LinkedHashMap<>();
        smoke.put("missionLogUpdateSmokeClass", EchoNativeAgent5MissionLogUpdateSmoke.class.getSimpleName());
        smoke.put("serviceCodeExecuted", true);
        smoke.put("adapterCoreBridge", true);
        smoke.put("missionId", update.get("missionId"));
        smoke.put("missionStatus", update.get("missionStatus"));
        smoke.put("missionProgress", update.get("missionProgress"));
        smoke.put("effect", update.get("effect"));
        smoke.put("renderedLines", lines(rendered));
        smoke.put("missionMutation", missionMutation);
        smoke.put("runtimeMutationAccepted", runtimeMutationAccepted);
        smoke.put("runtimeActionId", String.valueOf(missionMutation.getOrDefault("runtimeActionId", "")));
        smoke.put("eventName", String.valueOf(missionMutation.getOrDefault("eventName", "")));
        smoke.put("passed", passed);
        return Map.copyOf(smoke);
    }

    @SuppressWarnings("unchecked")
    private static List<String> lines(Map<String, Object> rendered) {
        Object value = rendered.get("lines");
        if (value instanceof List<?> list) {
            return (List<String>) list;
        }
        return List.of();
    }

    private static boolean runtimeMutationAccepted(
            Map<String, Object> evidence,
            String runtimeActionId,
            String eventName
    ) {
        Map<String, Object> result = evidence == null ? Map.of() : evidence;
        return Boolean.TRUE.equals(result.get("mutated"))
                && Boolean.TRUE.equals(result.get("saveTouched"))
                && Boolean.TRUE.equals(result.get("missionUpdated"))
                && Boolean.TRUE.equals(result.get("feedbackEmitted"))
                && runtimeActionId.equals(result.get("runtimeActionId"))
                && eventName.equals(result.get("eventName"));
    }

    private static double doubleValue(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException exception) {
            return 0.0D;
        }
    }
}
