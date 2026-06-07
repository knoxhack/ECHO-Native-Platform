package dev.echo.nativeplatform.loader;

import java.util.List;
import java.util.Map;

public record EchoNativeAgent10StoryRuntimeResult(
        boolean adapterCoreBridge,
        boolean terminalArchiveUnlocked,
        boolean dataDriveRead,
        boolean storyFlagPersisted,
        boolean relicEffectApplied,
        boolean spellUsed,
        boolean ritualActivated,
        boolean curseApplied,
        boolean riftTriggered,
        boolean chapterUnlocked,
        boolean storyStateSaved,
        boolean indexWikiLoreUpdated,
        List<String> moduleReferences,
        List<String> referenceContentIds,
        List<String> executedHandlers,
        Map<String, Integer> gameplayStats,
        List<String> loreUpdates
) {
    public String parityVector() {
        return "modules=" + moduleReferences.size()
                + " contentIds=" + referenceContentIds.size()
                + " handlers=" + executedHandlers.size()
                + " loreUpdates=" + loreUpdates.size();
    }
}
