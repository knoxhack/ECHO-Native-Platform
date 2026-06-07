package dev.echo.nativeplatform.contracts.entity;


import dev.echo.nativeplatform.contracts.EchoNativeApiStability;
import dev.echo.nativeplatform.contracts.EchoNativeApiStatus;
import java.util.List;

@EchoNativeApiStatus(value = EchoNativeApiStability.BETA, since = "0.1.0-native-beta")
public record EchoEncounter(String encounterId, List<String> participantEntityIds, String missionObjectiveId) {
    public EchoEncounter {
        if (encounterId == null || encounterId.isBlank()) {
            throw new IllegalArgumentException("encounterId is required");
        }
        participantEntityIds = List.copyOf(participantEntityIds == null ? List.of() : participantEntityIds);
        if (missionObjectiveId == null || missionObjectiveId.isBlank()) {
            throw new IllegalArgumentException("missionObjectiveId is required");
        }
    }
}
