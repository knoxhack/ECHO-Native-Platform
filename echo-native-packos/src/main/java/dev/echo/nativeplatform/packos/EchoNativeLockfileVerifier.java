package dev.echo.nativeplatform.packos;

import dev.echo.nativeplatform.contracts.EchoNativeDiagnostic;
import dev.echo.nativeplatform.contracts.EchoNativeIssueSeverity;
import dev.echo.nativeplatform.contracts.EchoNativePackProfile;
import dev.echo.nativeplatform.diagnostics.EchoNativeJson;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class EchoNativeLockfileVerifier {
    public EchoNativeLockfileVerificationPlan verify(
            EchoNativePackProfile profile,
            EchoNativeLockfilePlan expected,
            Path lockfileReportPath
    ) {
        List<EchoNativeDiagnostic> diagnostics = new ArrayList<>(expected.diagnostics());
        String packId = profile == null ? expected.packId() : profile.id();
        Map<String, Object> storedLockfile = Map.of();
        boolean lockfilePresent = Files.isRegularFile(lockfileReportPath);

        if (!lockfilePresent) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-LOCKFILE-MISSING",
                    EchoNativeIssueSeverity.ERROR,
                    "Native dry-run lockfile missing",
                    "No lockfile report exists at " + safePath(lockfileReportPath) + ".",
                    null,
                    packId,
                    List.of(safePath(lockfileReportPath)),
                    "Run echo-native lock generate before lock verify."
            ));
        } else {
            try {
                Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(lockfileReportPath)));
                Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
                storedLockfile = EchoNativeJson.asObject(data.get("lockfile"));
                compareLockfile(packId, expected.lockfile(), storedLockfile, diagnostics, lockfileReportPath);
            } catch (RuntimeException | IOException ex) {
                diagnostics.add(new EchoNativeDiagnostic(
                        "ECHO-NATIVE-LOCKFILE-INVALID",
                        EchoNativeIssueSeverity.ERROR,
                        "Native dry-run lockfile invalid",
                        ex.getMessage(),
                        null,
                        packId,
                        List.of(safePath(lockfileReportPath)),
                        "Regenerate the dry-run lockfile from the current fixture descriptors."
                ));
            }
        }

        Map<String, Object> status = new LinkedHashMap<>();
        status.put("dryRunOnly", true);
        status.put("lockfilePresent", lockfilePresent);
        status.put("phase", "phase12_packos_dry_run");
        status.put("repairExecutionAllowed", false);
        status.put("status", diagnostics.stream().anyMatch(diagnostic -> diagnostic.severity() == EchoNativeIssueSeverity.ERROR || diagnostic.severity() == EchoNativeIssueSeverity.FATAL)
                ? "blocked"
                : "valid");
        status.put("verifiedModules", storedModuleCount(storedLockfile));
        return new EchoNativeLockfileVerificationPlan(packId, status, List.copyOf(diagnostics));
    }

    private static void compareLockfile(String packId, Map<String, Object> expected, Map<String, Object> stored, List<EchoNativeDiagnostic> diagnostics, Path lockfileReportPath) {
        if (!Objects.equals(expected.get("schema"), stored.get("schema"))
                || !Objects.equals(expected.get("packId"), stored.get("packId"))
                || !Objects.equals(expected.get("rootModule"), stored.get("rootModule"))
                || !Objects.equals(expected.get("minecraftVersion"), stored.get("minecraftVersion"))
                || !Objects.equals(expected.get("loaderKind"), stored.get("loaderKind"))
                || !Objects.equals(expected.get("loaderVersion"), stored.get("loaderVersion"))
                || !Objects.equals(expected.get("moduleLoadOrder"), stored.get("moduleLoadOrder"))
                || !Objects.equals(expected.get("lockedModules"), stored.get("lockedModules"))
                || !Objects.equals(expected.get("lockedFeatures"), stored.get("lockedFeatures"))) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-LOCKFILE-DRIFT",
                    EchoNativeIssueSeverity.ERROR,
                    "Native dry-run lockfile drift",
                    "The stored lockfile report no longer matches current fixture descriptors.",
                    null,
                    packId,
                    List.of(safePath(lockfileReportPath)),
                    "Regenerate the dry-run lockfile and inspect descriptor changes."
            ));
        }
    }

    private static long storedModuleCount(Map<String, Object> storedLockfile) {
        Object modules = storedLockfile.get("lockedModules");
        return modules instanceof List<?> list ? list.size() : 0;
    }

    private static String safePath(Path path) {
        Path normalized = path.toAbsolutePath().normalize();
        Path workspace = Path.of("").toAbsolutePath().normalize();
        try {
            return workspace.relativize(normalized).toString().replace('\\', '/');
        } catch (IllegalArgumentException ex) {
            return path.toString().replace('\\', '/');
        }
    }
}
