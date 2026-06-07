package dev.echo.nativeplatform.loader;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Tracks live client proof targets for the Native Loader.
 *
 * <p>Each target represents a required piece of evidence that the Native Loader
 * can operate inside a live Minecraft runtime. Local/standalone harnesses may
 * satisfy some targets but must not claim full completion without real client
 * launch/attach evidence.</p>
 */
public final class EchoNativeLiveClientProof {
    private final Map<String, Boolean> targets = new LinkedHashMap<>();
    private final List<String> blockers = new ArrayList<>();
    private final List<String> satisfied = new ArrayList<>();
    private String status = "INCOMPLETE";
    private boolean complete = false;
    private String summary = "";

    public EchoNativeLiveClientProof() {
        reset();
    }

    public void reset() {
        targets.clear();
        targets.put("nativeLoaderStartsClient", false);
        targets.put("productModuleDescriptorLoaded", false);
        targets.put("adapterCoreBackendLoaded", false);
        targets.put("uiHostOpenedOrAttached", false);
        targets.put("playerOrWorldMutationThroughHost", false);
        targets.put("saveDataWritten", false);
        targets.put("hudNotificationEmitted", false);
        targets.put("serviceRegistryInsideLiveRuntime", false);
        targets.put("moduleClassesLoadedInsideLiveRuntime", false);
        targets.put("bootstrapEnteredLiveClient", false);
        blockers.clear();
        satisfied.clear();
        status = "INCOMPLETE";
        complete = false;
        summary = "Live client proof has not been started.";
    }

    public void satisfy(String target) {
        if (targets.containsKey(target)) {
            targets.put(target, true);
            if (!satisfied.contains(target)) {
                satisfied.add(target);
            }
            blockers.remove(target);
        }
        recompute();
    }

    public void block(String target, String reason) {
        if (targets.containsKey(target)) {
            targets.put(target, false);
            if (!blockers.contains(target)) {
                blockers.add(target);
            }
        }
        recompute();
    }

    public void satisfyModuleLoad(String moduleId, String loadedClassName) {
        if (moduleId != null && !moduleId.isBlank()) {
            satisfy("productModuleDescriptorLoaded");
        }
        if (!loadedClassName.isBlank()) {
            satisfy("moduleClassesLoadedInsideLiveRuntime");
        }
    }

    public void satisfyBackendLoaded(boolean resolved) {
        if (resolved) {
            satisfy("adapterCoreBackendLoaded");
            satisfy("serviceRegistryInsideLiveRuntime");
        }
    }

    public void satisfyMutation(String surface) {
        if ("inventory".equals(surface) || "world_blocks".equals(surface)) {
            satisfy("playerOrWorldMutationThroughHost");
        }
        if ("save_data".equals(surface)) {
            satisfy("saveDataWritten");
        }
        if ("hud".equals(surface) || "packets_hud".equals(surface)) {
            satisfy("hudNotificationEmitted");
        }
    }

    private void recompute() {
        boolean allSatisfied = targets.values().stream().allMatch(Boolean::booleanValue);
        boolean hasLaunchEvidence = Boolean.TRUE.equals(targets.get("nativeLoaderStartsClient"))
                && Boolean.TRUE.equals(targets.get("bootstrapEnteredLiveClient"));
        if (!hasLaunchEvidence) {
            status = "INCOMPLETE";
            complete = false;
            summary = "No live Minecraft client launch/attach evidence; all other targets are secondary until client starts.";
            return;
        }
        if (allSatisfied) {
            status = "MUTATED";
            complete = true;
            summary = "All live client proof targets satisfied including real Minecraft client launch/attach.";
        } else {
            status = "PARTIAL";
            complete = false;
            summary = "Client launched/attached; some live runtime targets still pending.";
        }
    }

    public String status() {
        return status;
    }

    public boolean complete() {
        return complete;
    }

    public Map<String, Object> toReport() {
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("status", status);
        report.put("complete", complete);
        for (Map.Entry<String, Boolean> entry : targets.entrySet()) {
            report.put(entry.getKey(), entry.getValue());
        }
        report.put("satisfiedTargets", List.copyOf(satisfied));
        report.put("missingTargets", List.copyOf(blockers));
        report.put("summary", summary);
        return report;
    }
}
