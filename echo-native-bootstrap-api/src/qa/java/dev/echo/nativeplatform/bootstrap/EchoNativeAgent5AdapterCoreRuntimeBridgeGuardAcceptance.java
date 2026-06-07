package dev.echo.nativeplatform.bootstrap;

import java.util.LinkedHashMap;
import java.util.Map;

public final class EchoNativeAgent5AdapterCoreRuntimeBridgeGuardAcceptance {
    private EchoNativeAgent5AdapterCoreRuntimeBridgeGuardAcceptance() {
    }

    public static Map<String, Object> assess(
            boolean adapterCoreRuntimeBridgeActive,
            Map<String, Object> liveClientHostEvidence
    ) {
        boolean hostAccepted = Boolean.TRUE.equals(liveClientHostEvidence.get("accepted"));
        boolean accepted = adapterCoreRuntimeBridgeActive && hostAccepted;
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("accepted", accepted);
        result.put("guardCodeEvaluated", true);
        result.put("serviceCodeExecuted", accepted);
        result.put("adapterCoreRuntimeBridgeActive", adapterCoreRuntimeBridgeActive);
        result.put("liveClientHostEvidenceAccepted", hostAccepted);
        result.put("contract", "adaptercore:agent5_ui_runtime_bridge_guard");
        result.put("effect", accepted
                ? "adaptercore_runtime_bridge_guard:accepted:agent5_ui"
                : "adaptercore_runtime_bridge_guard:rejected");
        result.put("rejection", accepted ? "" : rejection(adapterCoreRuntimeBridgeActive, hostAccepted));
        return result;
    }

    public static Map<String, Object> smoke() {
        Map<String, Object> accepted = assess(true, Map.of("accepted", true));
        Map<String, Object> rejectedNoRuntime = assess(false, Map.of("accepted", true));
        Map<String, Object> rejectedNoHost = assess(true, Map.of("accepted", false));
        Map<String, Object> smoke = new LinkedHashMap<>();
        smoke.put("adapterCoreRuntimeBridgeGuardAcceptanceSmokeClass",
                EchoNativeAgent5AdapterCoreRuntimeBridgeGuardAcceptance.class.getSimpleName());
        smoke.put("guardCodeEvaluated", true);
        smoke.put("serviceCodeExecuted", Boolean.TRUE.equals(accepted.get("serviceCodeExecuted")));
        smoke.put("accepted", accepted);
        smoke.put("rejectedNoRuntimeBridge", rejectedNoRuntime);
        smoke.put("rejectedNoHostEvidence", rejectedNoHost);
        smoke.put("passed", Boolean.TRUE.equals(accepted.get("accepted"))
                && Boolean.FALSE.equals(rejectedNoRuntime.get("accepted"))
                && "adaptercore_runtime_bridge_inactive".equals(rejectedNoRuntime.get("rejection"))
                && Boolean.FALSE.equals(rejectedNoHost.get("accepted"))
                && "live_client_host_evidence_not_accepted".equals(rejectedNoHost.get("rejection")));
        return smoke;
    }

    private static String rejection(boolean adapterCoreRuntimeBridgeActive, boolean hostAccepted) {
        if (!adapterCoreRuntimeBridgeActive) {
            return "adaptercore_runtime_bridge_inactive";
        }
        if (!hostAccepted) {
            return "live_client_host_evidence_not_accepted";
        }
        return "unknown";
    }
}
