package dev.echo.nativeplatform.cli;

import dev.echo.nativeplatform.diagnostics.EchoNativeJson;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public final class EchoNativeRealProcessMainClassTruthGateVerifier {
    private static final String PLACEHOLDER_MAIN_CLASS = "minecraft-client-main-class";

    private EchoNativeRealProcessMainClassTruthGateVerifier() {
    }

    public static void main(String[] args) throws Exception {
        Path fixture = args.length > 0 ? Path.of(args[0]) : Path.of("fixtures/ashfall");
        String packId = fixture.getFileName().toString();
        Path reportPath = args.length > 1 ? Path.of(args[1]) : Path.of("reports/echo-native", packId, "real-process-command-line-preview.json");
        verifyReport(fixture, reportPath, packId);
        System.out.println("real-process main class truth gate PASS fixture=" + fixture + " report=" + reportPath);
    }

    private static void verifyReport(Path fixture, Path reportPath, String packId) throws Exception {
        Path manifestPath = fixture.resolve("local-runtime/minecraft/26.1.2/metadata/26.1.2.json");
        require(Files.isRegularFile(manifestPath),
                "fixture-local version manifest is required for real-process main-class truth gate: " + manifestPath);
        require(Files.isRegularFile(reportPath),
                "real-process command-line preview report is required: " + reportPath);

        Map<String, Object> manifest = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(manifestPath)));
        String expectedMainClass = String.valueOf(manifest.getOrDefault("mainClass", "")).trim();
        require(!expectedMainClass.isBlank(),
                "fixture-local version manifest must expose mainClass: " + manifestPath);

        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(reportPath)));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        String actualMainClass = String.valueOf(data.getOrDefault("mainClass", "")).trim();

        require("PASS".equals(String.valueOf(envelope.getOrDefault("status", ""))),
                "stored real-process command preview must be PASS for " + packId);
        require(expectedMainClass.equals(actualMainClass),
                "real-process preview mainClass was " + actualMainClass + "; expected " + expectedMainClass);
        require(!PLACEHOLDER_MAIN_CLASS.equals(actualMainClass),
                "real-process preview must not expose the placeholder main class");
        require(Boolean.TRUE.equals(data.get("mainClassResolved")),
                "real-process preview must set mainClassResolved=true when the fixture manifest has mainClass");
        require("fixture_local_version_manifest".equals(String.valueOf(data.getOrDefault("mainClassSource", ""))),
                "real-process preview must identify fixture_local_version_manifest as the mainClass source");
        require(expectedMainClass.equals(previewArgument(data, "mainClass")),
                "real-process preview command argument mainClass must match the fixture manifest");
    }

    private static String previewArgument(Map<String, Object> data, String id) {
        Object rawArguments = data.get("commandLinePreview");
        if (!(rawArguments instanceof List<?> arguments)) {
            return "";
        }
        for (Object raw : arguments) {
            Map<String, Object> argument = EchoNativeJson.asObject(raw);
            if (id.equals(String.valueOf(argument.get("id")))) {
                return String.valueOf(argument.getOrDefault("value", "")).trim();
            }
        }
        return "";
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
