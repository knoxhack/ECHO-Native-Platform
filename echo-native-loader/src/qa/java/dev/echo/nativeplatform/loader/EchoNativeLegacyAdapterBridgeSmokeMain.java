package dev.echo.nativeplatform.loader;

import dev.echo.nativeplatform.contracts.EchoNativeAddonDescriptor;
import dev.echo.nativeplatform.contracts.EchoNativeApiStability;
import dev.echo.nativeplatform.contracts.EchoNativeLoadStatus;
import dev.echo.nativeplatform.contracts.EchoNativeModuleLoadResult;
import dev.echo.nativeplatform.contracts.EchoNativeRuntimeSide;
import dev.echo.nativeplatform.contracts.EchoNativeServiceRegistry;
import dev.echo.nativeplatform.contracts.EchoNativeTrustLevel;
import dev.echo.nativeplatform.diagnostics.EchoNativeJson;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoNativeLegacyAdapterBridgeSmokeMain {
    private EchoNativeLegacyAdapterBridgeSmokeMain() {
    }

    public static void main(String[] args) throws Exception {
        Path output = args.length > 0
                ? Path.of(args[0]).toAbsolutePath().normalize()
                : Path.of("build/native-loader-legacy-adapter-bridge/native-loader-legacy-adapter-bridge.json")
                .toAbsolutePath()
                .normalize();

        EchoNativeAddonDescriptor descriptor = descriptor();
        EchoNativeServiceRegistry serviceRegistry = new EchoNativeServiceRegistry();
        EchoNativeModuleLoadResult loadResult = new EchoNativeModuleLoader().loadRelease(descriptor, serviceRegistry);
        Map<String, Object> report = EchoNativeModuleLoader.toReport(loadResult);

        require(loadResult.status() == EchoNativeLoadStatus.FAILED,
                "Legacy activateNative(Map) adapters must fail release loader construction.");
        require(!loadResult.loaded(), "Legacy activateNative(Map) adapters must not count as loaded.");
        require(!loadResult.registered(), "Legacy activateNative(Map) adapters must not register services.");
        require(!loadResult.mutated(), "Legacy activateNative(Map) adapters must not claim host mutation.");
        require(LegacyAdapterModule.class.getName().equals(loadResult.loadedClassName()),
                "Loader should discover the legacy adapter class before rejecting it.");
        require(loadResult.loadedByModuleClassLoader(),
                "Legacy release rejection proof must load the class from explicit nativeClasspath, not app classpath fallback.");
        require(loadResult.constructedEntrypointClassName().isBlank(),
                "Loader must not construct a lifecycle entrypoint for legacy activateNative(Map) classes.");
        require(loadResult.descriptor().nativeClasspathDeclared(),
                "Legacy release rejection proof must declare explicit nativeClasspath.");
        require(!loadResult.descriptor().compatibilityClasspathFallback(),
                "Legacy release rejection proof must not rely on compatibility classpath fallback.");
        require(loadResult.registeredServices().isEmpty(),
                "Legacy activateNative(Map) rejection must not leave registered services behind.");
        require(loadResult.mutations().isEmpty(),
                "Legacy activateNative(Map) rejection must not leave lifecycle mutations behind.");
        require(loadResult.diagnostics().stream()
                        .anyMatch(item -> item.contains("does not implement EchoNativeModuleEntrypoint")),
                "Loader diagnostics must name the missing EchoNativeModuleEntrypoint API.");
        require(loadResult.diagnostics().stream()
                        .anyMatch(item -> item.contains("Legacy activateNative(Map) adapters are not accepted")),
                "Loader diagnostics must explicitly reject the legacy activateNative(Map) API.");

        Files.createDirectories(output.getParent());
        Files.writeString(output, EchoNativeJson.write(withRejectionProof(report)), StandardCharsets.UTF_8);
        System.out.println("native legacy adapter rejection smoke PASS " + output);
    }

    private static EchoNativeAddonDescriptor descriptor() {
        return new EchoNativeAddonDescriptor(
                "echo.mod.v1",
                "echolegacybridgeproof",
                "ECHO Legacy Adapter Rejection Proof",
                "1.0.0",
                "module",
                "runtime",
                LegacyAdapterModule.class.getName(),
                EchoNativeRuntimeSide.COMMON,
                EchoNativeTrustLevel.OFFICIAL,
                EchoNativeApiStability.STABLE,
                true,
                false,
                List.of(),
                List.of(),
                List.of("echolegacybridgeproof:contract/data_service"),
                List.of("adaptercore"),
                List.of(),
                Map.of(
                        "nativeEntrypoint", LegacyAdapterModule.class.getName(),
                        "nativeClasspath", legacyModuleClasspath()
                ),
                Path.of("fixtures/native-loader-legacy-adapter-bridge/modules/echolegacybridgeproof/META-INF/echo.mod.json")
        );
    }

    private static Map<String, Object> withRejectionProof(Map<String, Object> moduleReport) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("schema", "echo.native.legacy_adapter_rejection_smoke.v1");
        result.put("runtimeLane", "Native Loader");
        result.put("laneRole", "primary product loader");
        result.put("status", "FAILED_AS_EXPECTED");
        result.put("releaseMode", true);
        result.put("legacyAdapterDelegateClass", LegacyAdapterModule.class.getName());
        result.put("requiredEntrypointApi", "EchoNativeModuleEntrypoint");
        result.put("legacyActivateNativeBridgeAllowed", false);
        result.put("module", moduleReport);
        result.put("claimBoundary", Map.of(
                "activationClaimAllowed", false,
                "nativeHostMutationClaimAllowed", false,
                "gameplayReadyClaimAllowed", false,
                "reason", "Release loading requires EchoNativeModuleEntrypoint; activateNative(Map) is not a product entrypoint API."
        ));
        return result;
    }

    private static List<String> legacyModuleClasspath() {
        try {
            Path location = Path.of(LegacyAdapterModule.class.getProtectionDomain()
                            .getCodeSource()
                            .getLocation()
                            .toURI())
                    .toAbsolutePath()
                    .normalize();
            return List.of(location.toString());
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to resolve legacy adapter QA classpath.", exception);
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    public static final class LegacyAdapterModule {
        public Map<String, Object> activateNative(Map<String, String> context) {
            return Map.of(
                    "activationStage", "legacy-activate-native",
                    "nativeAdapterCodeExecuted", true,
                    "moduleId", context.getOrDefault("moduleId", "")
            );
        }
    }
}
