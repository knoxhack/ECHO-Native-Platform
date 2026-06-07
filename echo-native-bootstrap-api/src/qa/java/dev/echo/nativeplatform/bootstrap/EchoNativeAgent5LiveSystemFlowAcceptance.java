package dev.echo.nativeplatform.bootstrap;

import java.util.LinkedHashMap;
import java.util.Map;

public final class EchoNativeAgent5LiveSystemFlowAcceptance {
    private EchoNativeAgent5LiveSystemFlowAcceptance() {
    }

    public static Map<String, Object> assess(
            Map<String, Object> settingsEndToEndAcceptance,
            Map<String, Object> pauseEndToEndAcceptance,
            Map<String, Object> recoveryEndToEndAcceptance
    ) {
        Map<String, Object> settings = settingsEndToEndAcceptance == null ? Map.of() : settingsEndToEndAcceptance;
        Map<String, Object> pause = pauseEndToEndAcceptance == null ? Map.of() : pauseEndToEndAcceptance;
        Map<String, Object> recovery = recoveryEndToEndAcceptance == null ? Map.of() : recoveryEndToEndAcceptance;
        boolean settingsAccepted = Boolean.TRUE.equals(settings.get("accepted"))
                && "settings_end_to_end:SETTINGS_ACTION->SETTINGS:ashfall-accessible:subtitles_off".equals(settings.get("effect"))
                && "SETTINGS_ACTION".equals(settings.get("key"))
                && "SETTINGS".equals(settings.get("surface"))
                && "ashfall-accessible".equals(settings.get("settingsProfile"))
                && "ashfall-agent5".equals(settings.get("settingsTheme"))
                && Double.valueOf(1.25D).equals(settings.get("settingsHudScale"))
                && Boolean.FALSE.equals(settings.get("settingsSubtitles"))
                && Boolean.TRUE.equals(settings.get("physicalInputAccepted"))
                && Boolean.TRUE.equals(settings.get("renderAccepted"))
                && Boolean.TRUE.equals(settings.get("interactionAccepted"))
                && Boolean.TRUE.equals(settings.get("adjustmentAccepted"))
                && Boolean.TRUE.equals(settings.get("settingsRendered"));
        boolean pauseAccepted = Boolean.TRUE.equals(pause.get("accepted"))
                && "pause_end_to_end:screen_escape->PAUSE:LENS".equals(pause.get("effect"))
                && "ESCAPE".equals(pause.get("key"))
                && "PAUSE".equals(pause.get("surface"))
                && "screen".equals(pause.get("routeType"))
                && "LENS".equals(pause.get("resumeDestinationMode"))
                && Boolean.TRUE.equals(pause.get("screenRouteHandled"))
                && Boolean.TRUE.equals(pause.get("screenInputAccepted"))
                && Boolean.FALSE.equals(pause.get("physicalInputAccepted"))
                && Boolean.TRUE.equals(pause.get("renderAccepted"))
                && Boolean.TRUE.equals(pause.get("interactionAccepted"))
                && Boolean.TRUE.equals(pause.get("optionAccepted"))
                && Boolean.TRUE.equals(pause.get("pauseRendered"));
        boolean recoveryAccepted = Boolean.TRUE.equals(recovery.get("accepted"))
                && "recovery_end_to_end:RECOVERY_ACTION->RECOVERY:RECOVERED".equals(recovery.get("effect"))
                && "RECOVERY_ACTION".equals(recovery.get("key"))
                && "RECOVERY".equals(recovery.get("surface"))
                && "recovery:recover".equals(recovery.get("recoveryFocusPath"))
                && Boolean.TRUE.equals(recovery.get("physicalInputAccepted"))
                && Boolean.TRUE.equals(recovery.get("renderAccepted"))
                && Boolean.TRUE.equals(recovery.get("interactionAccepted"))
                && Boolean.TRUE.equals(recovery.get("runtimeMutationAccepted"))
                && Boolean.TRUE.equals(recovery.get("recoveryRendered"));
        boolean accepted = settingsAccepted && pauseAccepted && recoveryAccepted;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("accepted", accepted);
        result.put("settingsAccepted", settingsAccepted);
        result.put("pauseAccepted", pauseAccepted);
        result.put("recoveryAccepted", recoveryAccepted);
        result.put("settingsProfile", String.valueOf(settings.getOrDefault("settingsProfile", "")));
        result.put("settingsHudScale", settings.getOrDefault("settingsHudScale", 0.0D));
        result.put("settingsSubtitles", settings.getOrDefault("settingsSubtitles", true));
        result.put("pauseResumeDestination", String.valueOf(pause.getOrDefault("resumeDestinationMode", "")));
        result.put("recoveryFocusPath", String.valueOf(recovery.getOrDefault("recoveryFocusPath", "")));
        result.put("recoveryRuntimeMutationAccepted",
                Boolean.TRUE.equals(recovery.get("runtimeMutationAccepted")));
        result.put("effect", accepted
                ? "live_system_flow:accepted:SETTINGS_ACTION/screen_escape/RECOVERY_ACTION"
                : "live_system_flow:rejected");
        result.put("adapterCoreBridge", true);
        result.put("serviceCodeExecuted", true);
        return Map.copyOf(result);
    }
}
