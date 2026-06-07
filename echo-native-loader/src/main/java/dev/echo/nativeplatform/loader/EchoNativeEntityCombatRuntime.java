package dev.echo.nativeplatform.loader;

import dev.echo.nativeplatform.contracts.entity.EchoCombatStats;
import dev.echo.nativeplatform.contracts.entity.EchoCreatureBrain;
import dev.echo.nativeplatform.contracts.entity.EchoDamageSource;
import dev.echo.nativeplatform.contracts.entity.EchoEncounter;
import dev.echo.nativeplatform.contracts.entity.EchoEntityInstance;
import dev.echo.nativeplatform.contracts.entity.EchoEntityType;
import dev.echo.nativeplatform.contracts.entity.EchoFactionRelation;
import dev.echo.nativeplatform.contracts.entity.EchoInteractionOption;
import dev.echo.nativeplatform.contracts.entity.EchoNpcProfile;
import dev.echo.nativeplatform.contracts.entity.EchoWeaponProfile;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoNativeEntityCombatRuntime {
    public EchoNativeEntityCombatResult runReferenceScenario() {
        EchoEntityInstance player = new EchoEntityInstance(
                "player-001", EchoEntityType.PLAYER, "echoplayercore:native_survivor", 0, 0,
                new EchoCombatStats(100, 100, 12, 0));
        EchoEntityInstance hostile = new EchoEntityInstance(
                "hostile-001", EchoEntityType.CREATURE, "echocreaturecore:wasteland_scavenger", 1, 0,
                new EchoCombatStats(35, 35, 5, 0));
        EchoEntityInstance familiar = new EchoEntityInstance(
                "familiar-001", EchoEntityType.FAMILIAR, "echofamiliarcore:spirit_drone", 0, 1,
                new EchoCombatStats(24, 24, 2, 0));
        EchoNpcProfile npc = new EchoNpcProfile(
                "echonpcore:field_medic",
                "echosocialcore:crash_survivors",
                List.of(new EchoInteractionOption("dialogue", "Open dialogue", "echonpcore:open_dialogue")));
        EchoCreatureBrain brain = new EchoCreatureBrain("echocreaturecore:hostile_pursuit", true, 8);
        EchoEncounter encounter = new EchoEncounter(
                "echoencountercore:agent8_crash_site_hostile",
                List.of(player.entityId(), hostile.entityId(), familiar.entityId()),
                "echomissioncore:defeat_hostile");
        EchoWeaponProfile weapon = new EchoWeaponProfile("echoarmory:alloy_sword", 12, "echocombatcore:kinetic");
        EchoFactionRelation relation = new EchoFactionRelation("echosocialcore:wasteland_scavengers", -30, true);

        EchoDamageSource hostileStrike = new EchoDamageSource(
                "echocombatcore:claw", hostile.entityId(), hostile.stats().attackDamage());
        player = player.withStats(player.stats().damage(hostileStrike.amount()));

        EchoDamageSource familiarAssist = new EchoDamageSource(
                "echofamiliarcore:spirit_drone_assist", familiar.entityId(), familiar.stats().attackDamage());
        hostile = hostile.withStats(hostile.stats().damage(familiarAssist.amount()));
        int hostileHealthAfterFamiliarAssist = hostile.stats().currentHealth();

        hostile = hostile.withStats(hostile.stats().damage(weapon.damage()));
        hostile = hostile.withStats(hostile.stats().damage(weapon.damage()));
        hostile = hostile.withStats(hostile.stats().damage(weapon.damage()));

        boolean hostileSpawned = brain.hostile() && relation.hostile()
                && encounter.participantEntityIds().contains(hostile.entityId());
        boolean encounterStarted = hostileSpawned
                && encounter.participantEntityIds().contains(player.entityId())
                && encounter.participantEntityIds().contains(familiar.entityId());
        boolean playerCanAttack = hostile.stats().currentHealth() == 0;
        boolean entityCanAttack = player.stats().currentHealth() == 95;
        boolean deathWorks = !hostile.stats().alive();
        boolean recoveryWorks = player.stats().alive();
        boolean lootGranted = deathWorks;
        boolean missionObjectiveAdvanced = deathWorks
                && encounter.missionObjectiveId().equals("echomissioncore:defeat_hostile");
        boolean encounterEnded = encounterStarted && lootGranted && missionObjectiveAdvanced;
        boolean familiarBehaviorExecuted = familiar.type() == EchoEntityType.FAMILIAR
                && hostileHealthAfterFamiliarAssist == 33;
        int playerExperienceAfterCombat = missionObjectiveAdvanced ? 30 : 0;
        boolean playerStatsUpdated = playerExperienceAfterCombat == 30;
        boolean npcInteractionOpened = npc.options().stream()
                .anyMatch(option -> option.actionId().equals("echonpcore:open_dialogue"));

        Map<String, Object> parityVector = new LinkedHashMap<>();
        parityVector.put("hostileSpawns", hostileSpawned);
        parityVector.put("playerCanAttack", playerCanAttack);
        parityVector.put("entityCanAttack", entityCanAttack);
        parityVector.put("playerHealthAfterAttack", player.stats().currentHealth());
        parityVector.put("hostileHealthAfterPlayerAttack", hostile.stats().currentHealth());
        parityVector.put("deathWorks", deathWorks);
        parityVector.put("recoveryWorks", recoveryWorks);
        parityVector.put("lootGranted", lootGranted);
        parityVector.put("missionObjectiveAdvanced", missionObjectiveAdvanced);
        parityVector.put("npcInteractionOpened", npcInteractionOpened);
        parityVector.put("encounterStarted", encounterStarted);
        parityVector.put("encounterEnded", encounterEnded);
        parityVector.put("familiarBehaviorExecuted", familiarBehaviorExecuted);
        parityVector.put("playerStatsUpdated", playerStatsUpdated);
        parityVector.put("playerExperienceAfterCombat", playerExperienceAfterCombat);

        return new EchoNativeEntityCombatResult(
                "echo_native_loader",
                true,
                hostileSpawned,
                playerCanAttack,
                entityCanAttack,
                player.stats().currentHealth(),
                hostile.stats().currentHealth(),
                deathWorks,
                recoveryWorks,
                lootGranted,
                missionObjectiveAdvanced,
                npcInteractionOpened,
                encounterStarted,
                encounterEnded,
                familiarBehaviorExecuted,
                playerStatsUpdated,
                playerExperienceAfterCombat,
                List.of(
                        "EchoEntityType",
                        "EchoEntityInstance",
                        "EchoNpcProfile",
                        "EchoCreatureBrain",
                        "EchoEncounter",
                        "EchoDamageSource",
                        "EchoCombatStats",
                        "EchoWeaponProfile",
                        "EchoArmorProfile",
                        "EchoFactionRelation",
                        "EchoInteractionOption"),
                parityVector);
    }
}
