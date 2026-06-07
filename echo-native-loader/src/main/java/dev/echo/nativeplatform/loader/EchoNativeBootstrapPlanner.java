package dev.echo.nativeplatform.loader;

import dev.echo.nativeplatform.contracts.EchoNativeAccessPolicy;
import dev.echo.nativeplatform.contracts.EchoNativeBootstrapPlan;
import dev.echo.nativeplatform.contracts.EchoNativeDiagnostic;

import java.util.List;

public final class EchoNativeBootstrapPlanner {
    private final EchoNativeValidator validator = new EchoNativeValidator();
    private final EchoNativeGraphPlanner graphPlanner = new EchoNativeGraphPlanner();

    public EchoNativeBootstrapPlan plan(EchoNativeScanResult scanResult) {
        List<EchoNativeDiagnostic> diagnostics = validator.validate(scanResult);
        if (scanResult.packProfile() == null) {
            return new EchoNativeBootstrapPlan("", "", EchoNativeAccessPolicy.nativeDryRun(), List.of(), List.of(), List.of(), List.of(), List.of(), diagnostics);
        }
        List<String> loadOrder = graphPlanner.loadOrder(scanResult);
        return new EchoNativeBootstrapPlan(
                scanResult.packProfile().id(),
                scanResult.packProfile().minecraftVersion(),
                EchoNativeAccessPolicy.nativeDryRun(),
                loadOrder.stream().map(module -> "planned://modules/" + module + ".jar").toList(),
                List.of("planned://minecraft/" + scanResult.packProfile().minecraftVersion() + "/natives"),
                List.of("--echoNativeDryRun", "--pack", scanResult.packProfile().id(), "--minecraftVersion", scanResult.packProfile().minecraftVersion()),
                loadOrder,
                graphPlanner.serviceDescriptors(scanResult),
                diagnostics
        );
    }
}
