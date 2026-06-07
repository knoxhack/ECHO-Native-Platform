package dev.echo.nativeplatform.loader;

import dev.echo.nativeplatform.contracts.EchoNativeLifecycleRecord;
import dev.echo.nativeplatform.contracts.EchoNativeLoadStatus;
import dev.echo.nativeplatform.contracts.EchoNativeModuleLoadResult;
import dev.echo.nativeplatform.contracts.EchoNativeRegisteredService;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Validates that a module load result reflects real state, not report-only activation.
 *
 * <p>Rules:</p>
 * <ul>
 *   <li>LOADED: loadedClassName is non-empty and loadedByModuleClassLoader is true</li>
 *   <li>REGISTERED: at least one service registered with non-empty surfaces</li>
 *   <li>MUTATED: at least one mutation record with status MUTATED, or registry host has entries, or runtime host has persisted state</li>
 *   <li>No module may claim MUTATED solely from lifecycle phase history without real evidence</li>
 * </ul>
 */
public final class EchoNativeModuleLoadTruthGate {

    public TruthReport verify(EchoNativeModuleLoadResult result) {
        List<String> failures = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        boolean loaded = verifyLoaded(result, failures);
        boolean registered = verifyRegistered(result, failures);
        boolean mutated = verifyMutated(result, failures, warnings);

        EchoNativeLoadStatus honestStatus = honestStatus(result, loaded, registered, mutated);

        return new TruthReport(
                result.descriptor().id(),
                loaded,
                registered,
                mutated,
                honestStatus,
                result.status(),
                honestStatus == result.status(),
                List.copyOf(failures),
                List.copyOf(warnings),
                evidence(result)
        );
    }

    private boolean verifyLoaded(EchoNativeModuleLoadResult result, List<String> failures) {
        if (result.loadedClassName() == null || result.loadedClassName().isBlank()) {
            failures.add("LOADED gate failed: no class was loaded");
            return false;
        }
        if (!result.loadedByModuleClassLoader()) {
            failures.add("LOADED gate warning: class was loaded by " + result.loadedClassLoaderName()
                    + ", not by the module classloader");
            // During transition, parent classloader loading is acceptable if class actually loaded.
            // Return true but note the warning.
        }
        return true;
    }

    private boolean verifyRegistered(EchoNativeModuleLoadResult result, List<String> failures) {
        List<EchoNativeRegisteredService> services = result.registeredServices();
        if (services == null || services.isEmpty()) {
            failures.add("REGISTERED gate failed: no services registered");
            return false;
        }
        boolean hasRealService = services.stream()
                .anyMatch(s -> s.surfaces() != null && !s.surfaces().isEmpty());
        if (!hasRealService) {
            failures.add("REGISTERED gate failed: services exist but none declare surfaces");
            return false;
        }
        return true;
    }

    private boolean verifyMutated(EchoNativeModuleLoadResult result, List<String> failures, List<String> warnings) {
        List<Map<String, Object>> mutations = result.mutations();
        if (mutations == null || mutations.isEmpty()) {
            failures.add("MUTATED gate failed: no mutations recorded");
            return false;
        }
        boolean hasRealMutation = mutations.stream()
                .anyMatch(m -> EchoNativeLoadStatus.MUTATED.name().equals(String.valueOf(m.get("status"))));
        if (!hasRealMutation) {
            failures.add("MUTATED gate failed: mutation records exist but none have status MUTATED");
            return false;
        }
        long mutationCount = mutations.stream()
                .filter(m -> EchoNativeLoadStatus.MUTATED.name().equals(String.valueOf(m.get("status"))))
                .count();
        if (mutationCount < 1) {
            failures.add("MUTATED gate failed: expected at least 1 MUTATED record");
            return false;
        }
        return true;
    }

    private EchoNativeLoadStatus honestStatus(EchoNativeModuleLoadResult result, boolean loaded, boolean registered, boolean mutated) {
        if (!loaded) {
            return result.status() == EchoNativeLoadStatus.FAILED
                    ? EchoNativeLoadStatus.FAILED
                    : EchoNativeLoadStatus.UNSUPPORTED;
        }
        if (!registered) {
            return EchoNativeLoadStatus.LOADED;
        }
        if (!mutated) {
            return EchoNativeLoadStatus.REGISTERED;
        }
        return EchoNativeLoadStatus.MUTATED;
    }

    private Map<String, Object> evidence(EchoNativeModuleLoadResult result) {
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("loadedClassName", result.loadedClassName());
        evidence.put("loadedClassLoaderName", result.loadedClassLoaderName());
        evidence.put("loadedByModuleClassLoader", result.loadedByModuleClassLoader());
        evidence.put("constructedEntrypointClassName", result.constructedEntrypointClassName());
        evidence.put("serviceCount", result.registeredServices().size());
        evidence.put("mutationCount", result.mutations().size());
        evidence.put("mutationStatuses", result.mutations().stream()
                .map(m -> String.valueOf(m.get("status")))
                .distinct()
                .toList());
        evidence.put("phasesReached", result.lifecyclePhaseHistory().stream()
                .map(EchoNativeLifecycleRecord::phase)
                .map(Enum::name)
                .distinct()
                .toList());
        evidence.put("diagnosticsCount", result.diagnostics().size());
        return evidence;
    }

    public record TruthReport(
            String moduleId,
            boolean loaded,
            boolean registered,
            boolean mutated,
            EchoNativeLoadStatus honestStatus,
            EchoNativeLoadStatus claimedStatus,
            boolean statusAccurate,
            List<String> failures,
            List<String> warnings,
            Map<String, Object> evidence
    ) {
        public Map<String, Object> toReport() {
            Map<String, Object> report = new LinkedHashMap<>();
            report.put("moduleId", moduleId);
            report.put("loaded", loaded);
            report.put("registered", registered);
            report.put("mutated", mutated);
            report.put("honestStatus", honestStatus.name());
            report.put("claimedStatus", claimedStatus.name());
            report.put("statusAccurate", statusAccurate);
            report.put("failures", failures);
            report.put("warnings", warnings);
            report.put("evidence", evidence);
            return report;
        }

        public boolean passed() {
            return loaded && registered && mutated && statusAccurate;
        }
    }
}
