package dev.echo.nativeplatform.testkit;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class EchoNativeContractApiBoundaryVerifier {
    private static final Pattern PUBLIC_TOP_LEVEL = Pattern.compile(
            "(?m)^public\\s+(record|class|interface|enum|@interface)\\s+([A-Za-z0-9_]+)");
    private static final Pattern STATUS = Pattern.compile(
            "@EchoNativeApiStatus\\(value\\s*=\\s*EchoNativeApiStability\\.([A-Z_]+)");
    private static final Pattern INTERNAL_NAME = Pattern.compile(
            ".*(Plan|SafetyStatus|Rehearsal|Preflight|Boundary|Verification|Readiness|Completion|DryRun|Controlled|Dummy|SourcePolicy|BridgePrototype|PrototypeSafety|FailureContainment|Simulation|Experiment|Attempt|Capture).*");

    private EchoNativeContractApiBoundaryVerifier() {
    }

    public static void main(String[] args) throws Exception {
        Path sourceRoot = args.length == 0
                ? Path.of("echo-native-contracts", "src", "main", "java", "dev", "echo", "nativeplatform", "contracts")
                : Path.of(args[0]);
        if (!Files.isDirectory(sourceRoot)) {
            throw new IllegalStateException("Contract source root does not exist: " + sourceRoot.toAbsolutePath());
        }

        List<String> failures = new ArrayList<>();
        try (var stream = Files.walk(sourceRoot)) {
            for (Path path : stream.filter(item -> item.toString().endsWith(".java")).toList()) {
                verifyFile(sourceRoot, path, failures);
            }
        }
        if (!failures.isEmpty()) {
            throw new IllegalStateException("Native contract API boundary failures:\n" + String.join("\n", failures));
        }
    }

    private static void verifyFile(Path sourceRoot, Path path, List<String> failures) throws Exception {
        String text = Files.readString(path, StandardCharsets.UTF_8);
        Matcher publicType = PUBLIC_TOP_LEVEL.matcher(text);
        if (!publicType.find()) {
            return;
        }
        String typeName = publicType.group(2);
        if ("EchoNativeApiStatus".equals(typeName)) {
            return;
        }
        Matcher status = STATUS.matcher(text);
        if (!status.find()) {
            failures.add(sourceRoot.relativize(path) + " public type " + typeName + " is missing @EchoNativeApiStatus");
            return;
        }
        String stability = status.group(1);
        if (INTERNAL_NAME.matcher(typeName).matches() && !"INTERNAL".equals(stability)) {
            failures.add(sourceRoot.relativize(path) + " planning/audit type " + typeName
                    + " must be INTERNAL, got " + stability);
        }
        if (path.toString().contains("\\entity\\") || path.toString().contains("/entity/")) {
            if (!"BETA".equals(stability)) {
                failures.add(sourceRoot.relativize(path) + " entity runtime contract " + typeName
                        + " must be BETA, got " + stability);
            }
        }
    }
}
