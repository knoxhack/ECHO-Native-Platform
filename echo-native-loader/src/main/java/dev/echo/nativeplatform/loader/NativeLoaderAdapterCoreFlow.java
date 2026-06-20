package dev.echo.nativeplatform.loader;

import java.util.Map;
import java.util.function.Predicate;

public final class NativeLoaderAdapterCoreFlow {
    public static final String SERVICE_ID = "echo.native.adaptercore_flow";

    private final NativeLoaderAdapterCoreRuntimeMutations.Context mutationContext;
    private final NativeLoaderAdapterCoreMachineRuntimeActions.Context machineContext;
    private final NativeLoaderAdapterCoreScannerRuntimeActions.Context scannerContext;
    private final NativeLoaderAdapterCoreGameplayRuntimeActions.Context gameplayContext;
    private final Predicate<Object> creativePlayer;

    public NativeLoaderAdapterCoreFlow(
            NativeLoaderAdapterCoreRuntimeMutations.Context mutationContext,
            NativeLoaderAdapterCoreMachineRuntimeActions.Context machineContext,
            NativeLoaderAdapterCoreScannerRuntimeActions.Context scannerContext,
            NativeLoaderAdapterCoreGameplayRuntimeActions.Context gameplayContext,
            Predicate<Object> creativePlayer
    ) {
        this.mutationContext = mutationContext;
        this.machineContext = machineContext;
        this.scannerContext = scannerContext;
        this.gameplayContext = gameplayContext;
        this.creativePlayer = creativePlayer;
    }

    public boolean scannerUse(Object level, Object player, String source, boolean deepScan) {
        return NativeLoaderAdapterCoreScannerRuntimeActions.use(scannerContext, level, player, source, deepScan);
    }

    public Object scannerUseResult(Object level, Object player, String source, boolean deepScan) {
        return NativeLoaderAdapterCoreScannerRuntimeActions.useResult(scannerContext, level, player, source, deepScan);
    }

    public boolean waterBottleUsed(Object level, Object player, Object handOrStack, String itemId, String path) {
        return NativeLoaderAdapterCoreGameplayRuntimeActions.waterBottleUsed(
                gameplayContext,
                level,
                player,
                handOrStack,
                itemId,
                path
        );
    }

    public boolean crudeFilterUsed(Object level, Object player, Object handOrStack) {
        return NativeLoaderAdapterCoreGameplayRuntimeActions.crudeFilterUsed(
                gameplayContext,
                level,
                player,
                handOrStack
        );
    }

    public boolean handWarmerUsed(Object level, Object player, Object handOrStack, int warmthDelta) {
        return NativeLoaderAdapterCoreGameplayRuntimeActions.handWarmerUsed(
                gameplayContext,
                level,
                player,
                handOrStack,
                warmthDelta
        );
    }

    public boolean itemConsumed(Object level, Object player, Object handOrStack) {
        return NativeLoaderAdapterCoreGameplayRuntimeActions.itemConsumed(
                gameplayContext,
                level,
                player,
                handOrStack
        );
    }

    public boolean radAwayUsed(Object level, Object player, String source) {
        return NativeLoaderAdapterCoreGameplayRuntimeActions.radAwayUsed(gameplayContext, level, player, source);
    }

    public boolean filterCartridgeUsed(
            Object level,
            Object player,
            String itemId,
            String tierName,
            int tier,
            int refillAmount
    ) {
        return NativeLoaderAdapterCoreGameplayRuntimeActions.filterCartridgeUsed(
                gameplayContext,
                level,
                player,
                itemId,
                tierName,
                tier,
                refillAmount
        );
    }

    public boolean dataLogRecovered(Object level, Object player, String logType, String title) {
        return NativeLoaderAdapterCoreGameplayRuntimeActions.dataLogRecovered(
                gameplayContext,
                level,
                player,
                logType,
                title
        );
    }

    public boolean deployEntityRoute(Object level, Object player, Object pos, String source) {
        return NativeLoaderAdapterCoreGameplayRuntimeActions.deployEntityRoute(
                gameplayContext,
                level,
                player,
                pos,
                source
        );
    }

    public boolean powerNodeState(Object level, Object player, Object pos, boolean active, int activeNodeCount, String source) {
        return NativeLoaderAdapterCoreGameplayRuntimeActions.powerNodeState(
                gameplayContext,
                level,
                player,
                pos,
                active,
                activeNodeCount,
                source
        );
    }

    public boolean waterFiltered(Object level, Object player, String source) {
        return NativeLoaderAdapterCoreGameplayRuntimeActions.waterFiltered(gameplayContext, level, player, source);
    }

    public boolean researchLabAnalyze(Object level, Object player, String source) {
        return NativeLoaderAdapterCoreGameplayRuntimeActions.researchLabAnalyze(
                gameplayContext,
                level,
                player,
                source
        );
    }

    public boolean useBlock(Object level, Object player, Object pos, String machineId) {
        return NativeLoaderAdapterCoreMachineRuntimeActions.useBlock(
                machineContext,
                level,
                player,
                pos,
                machineId
        );
    }

    public boolean machineTick(Object level, Object player, Object pos, String machineId) {
        return NativeLoaderAdapterCoreMachineRuntimeActions.tick(machineContext, level, player, pos, machineId);
    }

    public boolean insertItem(Object level, Object player, Object pos, String machineId, String itemId, int count) {
        return NativeLoaderAdapterCoreMachineRuntimeActions.insertItem(
                machineContext,
                level,
                player,
                pos,
                machineId,
                itemId,
                count
        );
    }

    public boolean extractItem(Object level, Object player, Object pos, String machineId, String itemId, int count) {
        return NativeLoaderAdapterCoreMachineRuntimeActions.extractItem(
                machineContext,
                level,
                player,
                pos,
                machineId,
                itemId,
                count
        );
    }

    public boolean receiveEnergy(Object level, Object player, Object pos, String machineId, int amount) {
        return NativeLoaderAdapterCoreMachineRuntimeActions.receiveEnergy(
                machineContext,
                level,
                player,
                pos,
                machineId,
                amount
        );
    }

    public boolean extractEnergy(Object level, Object player, Object pos, String machineId, int amount) {
        return NativeLoaderAdapterCoreMachineRuntimeActions.extractEnergy(
                machineContext,
                level,
                player,
                pos,
                machineId,
                amount
        );
    }

    public boolean terminalOpened(Object level, Object player, String pageId) {
        return NativeLoaderAdapterCoreGameplayRuntimeActions.terminalOpened(gameplayContext, level, player, pageId);
    }

    public boolean removeItem(Object level, Object player, String itemId, int count) {
        return NativeLoaderAdapterCoreRuntimeMutations.removeItem(mutationContext, level, player, itemId, count);
    }

    public boolean removeConsumableItem(Object level, Object player, String itemId, int count) {
        return creativePlayer.test(player) || removeItem(level, player, itemId, count);
    }

    public boolean grantItem(Object serverPlayer, Object level, String itemId, int count) throws ReflectiveOperationException {
        return NativeLoaderAdapterCoreRuntimeMutations.grantItem(mutationContext, serverPlayer, level, itemId, count);
    }

    public boolean grantItem(Object runtimeHost, String itemId, int count) throws ReflectiveOperationException {
        return NativeLoaderAdapterCoreRuntimeMutations.grantItem(mutationContext, runtimeHost, itemId, count);
    }

    public Object grantItemResult(Object runtimeHost, String itemId, int count) throws ReflectiveOperationException {
        return NativeLoaderAdapterCoreRuntimeMutations.grantItemResult(mutationContext, runtimeHost, itemId, count);
    }

    public Map<String, Object> grantItemEvidence(Object level, Object player, String itemId, int count) {
        return NativeLoaderAdapterCoreRuntimeMutations.grantItemEvidence(mutationContext, level, player, itemId, count);
    }

    public Map<String, Object> placeWorldBlock(Object level, Object player, String blockId) {
        return NativeLoaderAdapterCoreRuntimeMutations.placeWorldBlock(mutationContext, level, player, blockId);
    }

    public Map<String, Object> placeStructure(Object level, Object player, String structureId, String anchor) {
        return NativeLoaderAdapterCoreRuntimeMutations.placeStructure(
                mutationContext,
                level,
                player,
                structureId,
                anchor
        );
    }

    public boolean writeSaveData(Object level, Object player, String scope, String key, Map<String, Object> payload) {
        return NativeLoaderAdapterCoreRuntimeMutations.writeSaveData(
                mutationContext,
                level,
                player,
                scope,
                key,
                payload
        );
    }

    public Map<String, Object> writeSaveDataEvidence(
            Object level,
            Object player,
            String scope,
            String key,
            Map<String, Object> payload
    ) {
        return NativeLoaderAdapterCoreRuntimeMutations.writeSaveDataEvidence(
                mutationContext,
                level,
                player,
                scope,
                key,
                payload
        );
    }

    public Object writeSaveData(
            Object runtimeHost,
            String scope,
            String key,
            Map<String, Object> payload,
            String idempotencyKey
    ) throws ReflectiveOperationException {
        return NativeLoaderAdapterCoreRuntimeMutations.writeSaveData(
                mutationContext,
                runtimeHost,
                scope,
                key,
                payload,
                idempotencyKey
        );
    }

    public Object publishEvent(
            Object runtimeHost,
            String eventId,
            Map<String, Object> payload,
            String idempotencyKey
    ) throws ReflectiveOperationException {
        return NativeLoaderAdapterCoreRuntimeMutations.publishEvent(
                mutationContext,
                runtimeHost,
                eventId,
                payload,
                idempotencyKey
        );
    }

    public Map<String, Object> publishHudNotification(Object level, Object player, Map<String, Object> payload) {
        return NativeLoaderAdapterCoreRuntimeMutations.publishHudNotification(mutationContext, level, player, payload);
    }
}
