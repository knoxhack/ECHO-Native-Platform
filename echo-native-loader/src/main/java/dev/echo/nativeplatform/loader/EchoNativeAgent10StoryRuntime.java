package dev.echo.nativeplatform.loader;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class EchoNativeAgent10StoryRuntime {
    public EchoNativeAgent10StoryRuntimeResult runReferenceScenario() {
        Set<String> archives = new LinkedHashSet<>();
        Set<String> chapters = new LinkedHashSet<>();
        Map<String, Boolean> flags = new LinkedHashMap<>();
        Map<String, Integer> stats = new LinkedHashMap<>();
        List<String> lore = new ArrayList<>();
        List<String> handlers = new ArrayList<>();

        execute(handlers, "signalos:terminal.open");
        lore(lore, "terminal:signalos");
        lore(lore, "index:signalos");
        lore(lore, "signal:signalos:signal/secure_cache");

        execute(handlers, "signalos:data_drive.read");
        archives.add("signalos:archive/field_cache");
        flags.put("signalos:story_flag/cache_secured", true);
        lore(lore, "wiki:signalos/data_drives");
        lore(lore, "lore:signalos:data_drive/handoff_drive");

        execute(handlers, "signalos:mission.start");
        lore(lore, "mission:signalos:mission/secure_cache");

        execute(handlers, "echorelictech:relic.effect.echo_mirror");
        mutate(stats, "signalClarity", 2);
        lore(lore, "relic:echorelictech:relic_effect/echo_mirror");

        execute(handlers, "echospellcore:spell.signal_pulse");
        mutate(stats, "signalClarity", 1);
        lore(lore, "spell:echospellcore:spell/signal_pulse");

        execute(handlers, "echoritualcore:ritual.relic_stabilization");
        mutate(stats, "chapterStability", 1);
        flags.put("echoritualcore:story_flag/relic_stabilized", true);
        lore(lore, "ritual:echoritualcore:ritual/relic_stabilization");

        execute(handlers, "echocursecore:curse.echo_rot");
        mutate(stats, "signalClarity", -1);
        lore(lore, "curse:echocursecore:curse/echo_rot");

        execute(handlers, "echoriftworlds:rift.cache_echo");
        flags.put("signalos:story_flag/rift_seen", true);
        lore(lore, "rift:echoriftworlds:rift_event/cache_echo");
        lore(lore, "chapter-route:signalos:chapter/cache_handoff");

        execute(handlers, "signalos:chapter.unlock");
        chapters.add("signalos:chapter/cache_handoff");
        lore(lore, "chapter:signalos:chapter/cache_handoff");

        execute(handlers, "echopresencelink:presence.signalos_cache");
        lore(lore, "presence:echopresencelink:presence/signalos_cache");

        execute(handlers, "echoorbitalremnants:data_drive.read");
        archives.add("echoblackboxprotocol:archive/core_memory");
        flags.put("echoprimecore:story_flag/prime_route_unlocked", true);
        lore(lore, "lore:echoorbitalremnants:data_drive/orbital_blackbox");

        execute(handlers, "echonexusprotocol:mission.start");
        lore(lore, "signal:echonexusprotocol:signal/nexus_handoff");
        lore(lore, "mission:echoprimecore:mission/prime_route");

        execute(handlers, "echostationfall:chapter.unlock");
        chapters.add("echostationfall:chapter/stationfall_route");
        lore(lore, "chapter:echostationfall:chapter/stationfall_route");

        execute(handlers, "echopresencelink:presence.prime_route");
        lore(lore, "presence:echopresencelink:presence/prime_route");

        execute(handlers, "signalosexample:data_drive.read");
        archives.add("echogrimoire:archive/arcane_codex");
        flags.put("echoarcanacore:story_flag/arcane_codex_unlocked", true);
        lore(lore, "lore:signalosexample:data_drive/arcane_codex_demo");

        execute(handlers, "echoarcanacore:mission.start");
        lore(lore, "signal:echoarcanacore:signal/aether_wake");
        lore(lore, "mission:echoarcanacore:mission/arcane_codex_sync");

        execute(handlers, "echorelictech:relic.effect.phase_anchor");
        mutate(stats, "aetherCharge", 2);
        lore(lore, "relic:echorelictech:relic_effect/phase_anchor");

        execute(handlers, "echoarcaneindex:chapter.unlock");
        chapters.add("echoarcaneindex:chapter/arcane_codex");
        lore(lore, "chapter:echoarcaneindex:chapter/arcane_codex");

        execute(handlers, "echoaetherworks:presence.aether_sync");
        lore(lore, "presence:echoaetherworks:presence/aether_sync");

        List<String> modules = List.of(
                "signalos",
                "echospellcore",
                "echoritualcore",
                "echocursecore",
                "echoriftworlds",
                "echoblackboxprotocol",
                "echoorbitalremnants",
                "echonexusprotocol",
                "echoprimecore",
                "echostationfall",
                "echopresencelink",
                "echogrimoire",
                "signalosexample",
                "echoarcanacore",
                "echorelictech",
                "echoarcaneindex",
                "echoaetherworks"
        );
        List<String> contentIds = List.of(
                "signalos:archive/field_cache",
                "signalos:data_drive/handoff_drive",
                "signalos:signal/secure_cache",
                "signalos:story_flag/cache_secured",
                "signalos:mission/secure_cache",
                "signalos:chapter/cache_handoff",
                "echorelictech:relic_effect/echo_mirror",
                "echospellcore:spell/signal_pulse",
                "echoritualcore:ritual/relic_stabilization",
                "echoritualcore:story_flag/relic_stabilized",
                "echocursecore:curse/echo_rot",
                "echoriftworlds:rift_event/cache_echo",
                "echopresencelink:presence/signalos_cache",
                "echoblackboxprotocol:archive/core_memory",
                "echoorbitalremnants:data_drive/orbital_blackbox",
                "echonexusprotocol:signal/nexus_handoff",
                "echoprimecore:story_flag/prime_route_unlocked",
                "echoprimecore:mission/prime_route",
                "echostationfall:chapter/stationfall_route",
                "echopresencelink:presence/prime_route",
                "echogrimoire:archive/arcane_codex",
                "signalosexample:data_drive/arcane_codex_demo",
                "echoarcanacore:signal/aether_wake",
                "echoarcanacore:story_flag/arcane_codex_unlocked",
                "echoarcanacore:mission/arcane_codex_sync",
                "echorelictech:relic_effect/phase_anchor",
                "echoarcaneindex:chapter/arcane_codex",
                "echoaetherworks:presence/aether_sync"
        );

        return new EchoNativeAgent10StoryRuntimeResult(
                true,
                archives.contains("signalos:archive/field_cache"),
                lore.contains("lore:signalos:data_drive/handoff_drive"),
                Boolean.TRUE.equals(flags.get("signalos:story_flag/cache_secured"))
                        && Boolean.TRUE.equals(flags.get("echoprimecore:story_flag/prime_route_unlocked"))
                        && Boolean.TRUE.equals(flags.get("echoarcanacore:story_flag/arcane_codex_unlocked")),
                stats.getOrDefault("signalClarity", 0) == 2 && stats.getOrDefault("aetherCharge", 0) == 2,
                lore.contains("spell:echospellcore:spell/signal_pulse"),
                Boolean.TRUE.equals(flags.get("echoritualcore:story_flag/relic_stabilized"))
                        && stats.getOrDefault("chapterStability", 0) == 1,
                lore.contains("curse:echocursecore:curse/echo_rot"),
                lore.contains("rift:echoriftworlds:rift_event/cache_echo")
                        && lore.contains("chapter-route:signalos:chapter/cache_handoff"),
                chapters.containsAll(List.of(
                        "signalos:chapter/cache_handoff",
                        "echostationfall:chapter/stationfall_route",
                        "echoarcaneindex:chapter/arcane_codex"
                )),
                !archives.isEmpty() && !flags.isEmpty() && !chapters.isEmpty(),
                lore.contains("index:signalos")
                        && lore.contains("wiki:signalos/data_drives")
                        && lore.stream().anyMatch(item -> item.startsWith("lore:")),
                modules,
                contentIds,
                List.copyOf(handlers),
                Map.copyOf(stats),
                List.copyOf(lore)
        );
    }

    private static void execute(List<String> handlers, String handler) {
        handlers.add(handler);
    }

    private static void mutate(Map<String, Integer> stats, String stat, int delta) {
        stats.merge(stat, delta, Integer::sum);
    }

    private static void lore(List<String> lore, String update) {
        if (!lore.contains(update)) {
            lore.add(update);
        }
    }
}
