package dev.echo.nativeplatform.loader;

import dev.echo.nativeplatform.diagnostics.EchoNativeJson;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoNativeAgent10StorySmokeMain {
    private EchoNativeAgent10StorySmokeMain() {
    }

    public static void main(String[] args) throws Exception {
        EchoNativeAgent10StoryRuntimeResult result = new EchoNativeAgent10StoryRuntime().runReferenceScenario();
        require(result.adapterCoreBridge(), "Agent 10 native story runtime must be AdapterCore-backed.");
        require(result.terminalArchiveUnlocked(), "Native SignalOS terminal must unlock an archive.");
        require(result.dataDriveRead(), "Native data drive reading must execute.");
        require(result.storyFlagPersisted(), "Native story flags must persist.");
        require(result.relicEffectApplied(), "Native relic effects must mutate gameplay state.");
        require(result.spellUsed(), "Native spell usage must execute.");
        require(result.ritualActivated(), "Native ritual activation must execute.");
        require(result.curseApplied(), "Native curse effects must execute.");
        require(result.riftTriggered(), "Native rift triggers must execute.");
        require(result.chapterUnlocked(), "Native chapter unlocks must execute.");
        require(result.storyStateSaved(), "Native story state save/load surface must be represented.");
        require(result.indexWikiLoreUpdated(), "Native story runtime must emit Index/Wiki/Lore updates.");
        require(result.moduleReferences().size() == 17, "Native story runtime must cover 17 executable module references.");
        require(result.referenceContentIds().containsAll(List.of(
                "signalos:archive/field_cache",
                "signalos:data_drive/handoff_drive",
                "signalos:signal/secure_cache",
                "echorelictech:relic_effect/echo_mirror",
                "echospellcore:spell/signal_pulse",
                "echoritualcore:ritual/relic_stabilization",
                "echocursecore:curse/echo_rot",
                "echoriftworlds:rift_event/cache_echo",
                "echostationfall:chapter/stationfall_route",
                "echoarcaneindex:chapter/arcane_codex",
                "echoaetherworks:presence/aether_sync"
        )), "Native story runtime reference content ids drifted.");

        writeReport(args, result);
        System.out.println("agent10 native story loader smoke PASS " + result.parityVector());
    }

    private static void writeReport(String[] args, EchoNativeAgent10StoryRuntimeResult result) throws Exception {
        Path reportPath = args.length > 0
                ? Path.of(args[0])
                : Path.of("..", "reports", "echo", "agents", "agent-10-native-loader.json");
        Files.createDirectories(reportPath.toAbsolutePath().getParent());
        Files.writeString(reportPath, EchoNativeJson.write(report(result)), StandardCharsets.UTF_8);
    }

    private static Map<String, Object> report(EchoNativeAgent10StoryRuntimeResult result) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("agent", "agent-10-story-signalos-arcane");
        data.put("runtime", "echo-native-loader");
        data.put("adapterCoreBridge", result.adapterCoreBridge());
        data.put("terminalArchiveUnlocked", result.terminalArchiveUnlocked());
        data.put("dataDriveRead", result.dataDriveRead());
        data.put("storyFlagPersisted", result.storyFlagPersisted());
        data.put("relicEffectApplied", result.relicEffectApplied());
        data.put("spellUsed", result.spellUsed());
        data.put("ritualActivated", result.ritualActivated());
        data.put("curseApplied", result.curseApplied());
        data.put("riftTriggered", result.riftTriggered());
        data.put("chapterUnlocked", result.chapterUnlocked());
        data.put("storyStateSaved", result.storyStateSaved());
        data.put("indexWikiLoreUpdated", result.indexWikiLoreUpdated());
        data.put("moduleReferences", result.moduleReferences());
        data.put("referenceContentIds", result.referenceContentIds());
        data.put("executedHandlers", result.executedHandlers());
        data.put("gameplayStats", result.gameplayStats());
        data.put("loreUpdates", result.loreUpdates());
        data.put("parityVector", result.parityVector());
        return data;
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
