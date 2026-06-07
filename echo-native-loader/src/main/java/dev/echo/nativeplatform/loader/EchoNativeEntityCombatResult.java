package dev.echo.nativeplatform.loader;

import java.util.List;
import java.util.Map;

public record EchoNativeEntityCombatResult(
        String runtime,
        boolean adapterCoreBridge,
        boolean hostileSpawned,
        boolean playerCanAttack,
        boolean entityCanAttack,
        int playerHealthAfterAttack,
        int hostileHealthAfterPlayerAttack,
        boolean deathWorks,
        boolean recoveryWorks,
        boolean lootGranted,
        boolean missionObjectiveAdvanced,
        boolean npcInteractionOpened,
        boolean encounterStarted,
        boolean encounterEnded,
        boolean familiarBehaviorExecuted,
        boolean playerStatsUpdated,
        int playerExperienceAfterCombat,
        List<String> contracts,
        Map<String, Object> parityVector
) {
}
