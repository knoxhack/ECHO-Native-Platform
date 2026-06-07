package dev.echo.nativeplatform.bootstrap;

import java.util.LinkedHashMap;
import java.util.Map;

public final class EchoNativeAgent5LiveCoreToolsAcceptanceSmoke {
    private EchoNativeAgent5LiveCoreToolsAcceptanceSmoke() {
    }

    public static Map<String, Object> capture() {
        Map<String, Object> terminal = object(EchoNativeAgent5TerminalEndToEndAcceptanceSmoke.capture()
                .get("accepted"));
        Map<String, Object> index = object(EchoNativeAgent5IndexEndToEndAcceptanceSmoke.capture()
                .get("accepted"));
        Map<String, Object> lens = object(EchoNativeAgent5LensEndToEndAcceptanceSmoke.capture()
                .get("accepted"));
        Map<String, Object> accepted = EchoNativeAgent5LiveCoreToolsAcceptance.assess(terminal, index, lens);
        Map<String, Object> rejectedNoTerminal = EchoNativeAgent5LiveCoreToolsAcceptance.assess(
                Map.of("accepted", false),
                index,
                lens
        );
        Map<String, Object> rejectedNoIndex = EchoNativeAgent5LiveCoreToolsAcceptance.assess(
                terminal,
                Map.of("accepted", false),
                lens
        );
        Map<String, Object> rejectedNoLens = EchoNativeAgent5LiveCoreToolsAcceptance.assess(
                terminal,
                index,
                Map.of("accepted", false)
        );
        boolean passed = Boolean.TRUE.equals(accepted.get("accepted"))
                && "live_core_tools:accepted:M/G/LEFT_ALT".equals(accepted.get("effect"))
                && Boolean.FALSE.equals(rejectedNoTerminal.get("accepted"))
                && Boolean.FALSE.equals(rejectedNoIndex.get("accepted"))
                && Boolean.FALSE.equals(rejectedNoLens.get("accepted"));

        Map<String, Object> smoke = new LinkedHashMap<>();
        smoke.put("liveCoreToolsAcceptanceSmokeClass",
                EchoNativeAgent5LiveCoreToolsAcceptanceSmoke.class.getSimpleName());
        smoke.put("accepted", accepted);
        smoke.put("rejectedNoTerminal", rejectedNoTerminal);
        smoke.put("rejectedNoIndex", rejectedNoIndex);
        smoke.put("rejectedNoLens", rejectedNoLens);
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
