package dev.echo.nativeplatform.contracts.entity;


import dev.echo.nativeplatform.contracts.EchoNativeApiStability;
import dev.echo.nativeplatform.contracts.EchoNativeApiStatus;
@EchoNativeApiStatus(value = EchoNativeApiStability.BETA, since = "0.1.0-native-beta")
public record EchoCombatStats(int maxHealth, int currentHealth, int attackDamage, int armor) {
    public EchoCombatStats {
        if (maxHealth <= 0) {
            throw new IllegalArgumentException("maxHealth must be positive");
        }
        if (currentHealth < 0 || currentHealth > maxHealth) {
            throw new IllegalArgumentException("currentHealth must be between zero and maxHealth");
        }
        if (attackDamage < 0 || armor < 0) {
            throw new IllegalArgumentException("combat stats must not be negative");
        }
    }

    public boolean alive() {
        return currentHealth > 0;
    }

    public EchoCombatStats damage(int rawDamage) {
        int applied = Math.max(0, rawDamage - armor);
        return new EchoCombatStats(maxHealth, Math.max(0, currentHealth - applied), attackDamage, armor);
    }
}
