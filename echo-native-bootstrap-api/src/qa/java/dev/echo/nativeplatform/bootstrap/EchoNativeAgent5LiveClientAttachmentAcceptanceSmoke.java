package dev.echo.nativeplatform.bootstrap;

import java.util.Map;

public final class EchoNativeAgent5LiveClientAttachmentAcceptanceSmoke {
    private EchoNativeAgent5LiveClientAttachmentAcceptanceSmoke() {
    }

    public static Map<String, Object> capture() {
        Map<String, Object> accepted = EchoNativeAgent5LiveClientAttachmentAcceptance.assess(
                true,
                true,
                true,
                9442L,
                "dev.echo.nativeplatform.generated.EchoNativeDashboardScreen",
                "dev.echo.nativeplatform.generated.EchoNativeDashboardScreen"
        );
        Map<String, Object> rejectedNoClient = EchoNativeAgent5LiveClientAttachmentAcceptance.assess(
                false,
                true,
                true,
                9442L,
                "dev.echo.nativeplatform.generated.EchoNativeDashboardScreen",
                "dev.echo.nativeplatform.generated.EchoNativeDashboardScreen"
        );
        Map<String, Object> rejectedNoScreen = EchoNativeAgent5LiveClientAttachmentAcceptance.assess(
                true,
                false,
                true,
                9442L,
                "dev.echo.nativeplatform.generated.EchoNativeDashboardScreen",
                "dev.echo.nativeplatform.generated.EchoNativeDashboardScreen"
        );
        Map<String, Object> rejectedNoClientThread = EchoNativeAgent5LiveClientAttachmentAcceptance.assess(
                true,
                true,
                false,
                9442L,
                "dev.echo.nativeplatform.generated.EchoNativeDashboardScreen",
                "dev.echo.nativeplatform.generated.EchoNativeDashboardScreen"
        );
        Map<String, Object> rejectedNoWindow = EchoNativeAgent5LiveClientAttachmentAcceptance.assess(
                true,
                true,
                true,
                0L,
                "dev.echo.nativeplatform.generated.EchoNativeDashboardScreen",
                "dev.echo.nativeplatform.generated.EchoNativeDashboardScreen"
        );
        Map<String, Object> rejectedScreenMismatch = EchoNativeAgent5LiveClientAttachmentAcceptance.assess(
                true,
                true,
                true,
                9442L,
                "dev.echo.nativeplatform.generated.OtherScreen",
                "dev.echo.nativeplatform.generated.EchoNativeDashboardScreen"
        );
        boolean passed = Boolean.TRUE.equals(accepted.get("accepted"))
                && "live_client_attachment:accepted:EchoNativeDashboardScreen".equals(accepted.get("effect"))
                && Boolean.FALSE.equals(rejectedNoClient.get("accepted"))
                && Boolean.FALSE.equals(rejectedNoScreen.get("accepted"))
                && Boolean.FALSE.equals(rejectedNoClientThread.get("accepted"))
                && Boolean.FALSE.equals(rejectedNoWindow.get("accepted"))
                && Boolean.FALSE.equals(rejectedScreenMismatch.get("accepted"));
        return Map.of(
                "liveClientAttachmentAcceptanceSmokeClass",
                EchoNativeAgent5LiveClientAttachmentAcceptanceSmoke.class.getSimpleName(),
                "accepted", accepted,
                "rejectedNoClient", rejectedNoClient,
                "rejectedNoScreen", rejectedNoScreen,
                "rejectedNoClientThread", rejectedNoClientThread,
                "rejectedNoWindow", rejectedNoWindow,
                "rejectedScreenMismatch", rejectedScreenMismatch,
                "adapterCoreBridge", true,
                "serviceCodeExecuted", true,
                "passed", passed
        );
    }
}
