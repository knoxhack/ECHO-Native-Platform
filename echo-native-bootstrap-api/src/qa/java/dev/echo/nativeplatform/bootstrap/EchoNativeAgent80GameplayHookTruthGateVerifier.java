package dev.echo.nativeplatform.bootstrap;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

public final class EchoNativeAgent80GameplayHookTruthGateVerifier {
    private static final List<String> REQUIRED_EVENTS = List.of(
            "player_join",
            "client_tick",
            "world_tick",
            "item_use",
            "block_place",
            "block_break",
            "entity_interact",
            "screen_open",
            "command_execution",
            "save_load",
            "resource_reload"
    );

    private EchoNativeAgent80GameplayHookTruthGateVerifier() {
    }

    public static void main(String[] args) throws Exception {
        Method handlersMethod = EchoNativeBootstrapMain.class.getDeclaredMethod("ashfallGameplayHandlers");
        handlersMethod.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> handlers = (List<Map<String, Object>>) handlersMethod.invoke(null);

        require(handlers.size() == REQUIRED_EVENTS.size(),
                "Ashfall gameplay truth gate must expose every required AdapterCore gameplay event.");
        for (String event : REQUIRED_EVENTS) {
            Map<String, Object> handler = handlers.stream()
                    .filter(item -> event.equals(item.get("event")))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("missing gameplay handler descriptor for " + event));
            require(("adaptercore.gameplay_handler." + event).equals(handler.get("adapterCoreContract")),
                    event + " must keep its AdapterCore contract.");
            require(("EchoStandaloneRuntimeAdapterCoreGameplayBridge." + event)
                            .equals(handler.get("standaloneRuntimeBackend")),
                    event + " must keep its standalone runtime backend.");
            require(Boolean.TRUE.equals(handler.get("attached")),
                    event + " must remain attached for controlled AdapterCore replay.");
            require(Boolean.TRUE.equals(handler.get("adapterCoreReplayVerified")),
                    event + " must prove controlled AdapterCore replay separately from live hooks.");
            require(Boolean.FALSE.equals(handler.get("liveGameplayHookVerified")),
                    event + " must not claim live Minecraft gameplay hook verification.");
            require(Boolean.FALSE.equals(handler.get("minecraftRuntimeAccessed")),
                    event + " must not claim Minecraft runtime access.");
        }

        Method bridgeMethod = EchoNativeBootstrapMain.class.getDeclaredMethod("applyAshfallGameplayBridge", String.class);
        bridgeMethod.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Object> bridge = (Map<String, Object>) bridgeMethod.invoke(null, "ashfall");
        require(Boolean.FALSE.equals(bridge.get("liveGameplayHandlersAttached")),
                "Ashfall gameplay bridge must not claim live handler attachment in controlled replay mode.");
        require(Boolean.FALSE.equals(bridge.get("liveMinecraftProcessHooksClaimed")),
                "Ashfall gameplay bridge must not claim live Minecraft process hooks.");
        require("live_minecraft_process_hook_attachment_unproven".equals(bridge.get("liveGameplayHookBlockedReason")),
                "Ashfall gameplay bridge must keep the live hook blocked reason explicit.");

        System.out.println("agent80 gameplay hook truth gate PASS handlers=11"
                + " adapterCoreReplayVerified=true liveMinecraftProcessHooksClaimed=false");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
