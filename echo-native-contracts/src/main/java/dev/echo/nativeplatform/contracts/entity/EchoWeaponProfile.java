package dev.echo.nativeplatform.contracts.entity;


import dev.echo.nativeplatform.contracts.EchoNativeApiStability;
import dev.echo.nativeplatform.contracts.EchoNativeApiStatus;
@EchoNativeApiStatus(value = EchoNativeApiStability.BETA, since = "0.1.0-native-beta")
public record EchoWeaponProfile(String weaponId, int damage, String damageType) {
    public EchoWeaponProfile {
        if (weaponId == null || weaponId.isBlank()) {
            throw new IllegalArgumentException("weaponId is required");
        }
        if (damage < 0) {
            throw new IllegalArgumentException("damage must not be negative");
        }
        if (damageType == null || damageType.isBlank()) {
            throw new IllegalArgumentException("damageType is required");
        }
    }
}
