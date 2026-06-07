package dev.echo.nativeplatform.loader;

public final class EchoNativeEntityCombatSmokeMain {
    private EchoNativeEntityCombatSmokeMain() {
    }

    public static void main(String[] args) {
        EchoNativeEntityCombatResult result = new EchoNativeEntityCombatRuntime().runReferenceScenario();
        require(result.adapterCoreBridge(), "native entity combat must be AdapterCore-backed");
        require(result.hostileSpawned(), "native hostile spawn must execute");
        require(result.playerCanAttack(), "native player attack must execute");
        require(result.entityCanAttack(), "native entity attack must execute");
        require(result.deathWorks(), "native death transition must execute");
        require(result.recoveryWorks(), "native recovery state must remain valid");
        require(result.lootGranted(), "native loot reward must execute");
        require(result.missionObjectiveAdvanced(), "native mission objective must advance");
        require(result.npcInteractionOpened(), "native NPC interaction must open an action");
        require(result.encounterStarted(), "native encounter start must execute");
        require(result.encounterEnded(), "native encounter end must execute");
        require(result.familiarBehaviorExecuted(), "native familiar behavior must execute");
        require(result.playerStatsUpdated(), "native player stats must update after combat");
        System.out.println("agent8 native entity combat smoke PASS " + result.parityVector());
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
