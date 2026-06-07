package dev.echo.nativeplatform.loader;

import dev.echo.nativeplatform.contracts.EchoNativeBootstrapProductProfile.NativeMachineOperationRules;

import java.util.List;

public final class NativeLoaderProductBlockActionExecutor {
    public static final String SERVICE_ID = "echo.native.product_block_action_executor";

    private NativeLoaderProductBlockActionExecutor() {
    }

    public static boolean execute(
            String action,
            String path,
            Object level,
            Object pos,
            Object player,
            NativeMachineOperationRules rules,
            Operations ops
    ) {
        return switch (safe(action)) {
            case "cache" -> cacheAction(level, pos, player, ops);
            case "relay" -> relayAction(level, pos, player, ops);
            case "power_node" -> powerNodeAction(level, pos, player, rules, ops);
            case "emergency_bunk" -> bunkAction(level, pos, player, ops);
            case "water_machine" -> waterMachineAction(path, level, pos, player, rules, ops);
            case "hazard_machine" -> hazardMachineAction(path, level, pos, player, rules, ops);
            case "research_lab" -> researchLabAction(level, pos, player, rules, ops);
            case "generator" -> generatorAction(path, level, pos, player, rules, ops);
            case "power_grid" -> gridAction(path, level, pos, player, rules, ops);
            case "processor" -> processorAction(path, level, pos, player, ops);
            case "stationary_scanner" -> ops.scannerUse(level, player, "stationary_signal_scanner", false);
            case "map_table" -> ops.terminalOpened(level, player, ops.holoMapRoute("map_table"))
                    && ops.openModuleSurface("echoholomap", "holomap");
            case "campaign_core" -> ops.machineUseBlock(level, player, pos,
                    ops.blockActionMachineId("campaign_core", path));
            default -> false;
        };
    }

    private static boolean cacheAction(Object level, Object pos, Object player, Operations ops) {
        return ops.machineUseBlock(level, player, pos, ops.blockActionMachineId("cache", "recovery_cache"));
    }

    private static boolean relayAction(Object level, Object pos, Object player, Operations ops) {
        return ops.machineUseBlock(level, player, pos, ops.blockActionMachineId("relay", "relay_station"));
    }

    private static boolean powerNodeAction(
            Object level,
            Object pos,
            Object player,
            NativeMachineOperationRules rules,
            Operations ops
    ) {
        String machineId = ops.blockActionMachineId("power_node", "power_node");
        boolean used = ops.machineUseBlock(level, player, pos, machineId);
        if (!used) {
            return false;
        }
        String fuel = ops.firstConfiguredItem(player, 1, rules.powerNodeFuelItemIds());
        if (fuel.isBlank()) {
            return false;
        }
        if (!ops.machineReceiveEnergy(level, player, pos, machineId, ops.energyItemCharge(fuel))) {
            return false;
        }
        if (!ops.removeItem(level, player, fuel, 1)) {
            return false;
        }
        boolean ticked = ops.machineTick(level, player, pos, machineId);
        boolean powered = ops.powerNodeState(level, player, pos, true, 1, "native_client_block_use");
        return ticked && powered;
    }

    private static boolean bunkAction(Object level, Object pos, Object player, Operations ops) {
        return ops.machineUseBlock(level, player, pos,
                ops.blockActionMachineId("emergency_bunk", "emergency_bunk"));
    }

    private static boolean waterMachineAction(
            String path,
            Object level,
            Object pos,
            Object player,
            NativeMachineOperationRules rules,
            Operations ops
    ) {
        String machineId = ops.productId(path);
        if (hasAny(path, rules.waterCollectionPathHints())) {
            if (!ops.machineUseBlock(level, player, pos, machineId)) {
                return false;
            }
            return ops.machineTick(level, player, pos, machineId);
        }
        if (!ops.machineUseBlock(level, player, pos, machineId)) {
            return false;
        }
        String charge = ops.firstConfiguredItem(player, 1, rules.machineChargeItemIds());
        if (!charge.isBlank()) {
            if (!ops.machineReceiveEnergy(level, player, pos, machineId, ops.energyItemCharge(charge))) {
                return false;
            }
            if (!ops.removeItem(level, player, charge, 1)) {
                return false;
            }
        }
        String filter = ops.firstConfiguredItem(player, 1, rules.waterFilterItemIds());
        String dirtyWaterId = ops.configuredId(rules.dirtyWaterInputItemId());
        String cleanWaterId = ops.configuredId(rules.cleanWaterOutputItemId());
        boolean dirtyWaterAvailable = ops.hasItem(player, dirtyWaterId, 1);
        if (dirtyWaterAvailable || !filter.isBlank()) {
            if (!dirtyWaterAvailable || filter.isBlank()) {
                return false;
            }
            boolean insertedDirtyWater = ops.machineInsertItem(level, player, pos, machineId, dirtyWaterId, 1);
            boolean insertedFilter = ops.machineInsertItem(level, player, pos, machineId, filter, 1);
            if (!insertedDirtyWater || !insertedFilter) {
                return false;
            }
            if (!ops.removeItem(level, player, dirtyWaterId, 1)
                    || !ops.removeItem(level, player, filter, 1)) {
                return false;
            }
            if (!tickRepeated(level, player, pos, machineId, rules.waterFilterTicks(), ops)) {
                return false;
            }
            if (!ops.machineExtractItem(level, player, pos, machineId, cleanWaterId, 1)) {
                return false;
            }
            if (!ops.giveItem(player, cleanWaterId, 1)) {
                return false;
            }
            return ops.waterFiltered(level, player, "native_client_water_purifier");
        }
        return ops.machineTick(level, player, pos, machineId);
    }

    private static boolean hazardMachineAction(
            String path,
            Object level,
            Object pos,
            Object player,
            NativeMachineOperationRules rules,
            Operations ops
    ) {
        String machineId = ops.productId(path);
        if (!ops.machineUseBlock(level, player, pos, machineId)) {
            return false;
        }
        if (hasAny(path, rules.radiationCleanserPathHints())) {
            String filter = ops.firstConfiguredItem(player, 1, rules.radiationCleanserFilterItemIds());
            String input = ops.firstConfiguredItem(player, 1, rules.contaminatedInputItemIds());
            if (filter.isBlank() && input.isBlank()) {
                return ops.machineTick(level, player, pos, machineId);
            }
            if (filter.isBlank() || input.isBlank()) {
                return false;
            }
            boolean insertedInput = ops.machineInsertItem(level, player, pos, machineId, input, 1);
            boolean insertedFilter = ops.machineInsertItem(level, player, pos, machineId, filter, 1);
            if (!insertedInput || !insertedFilter) {
                return false;
            }
            if (!ops.removeItem(level, player, input, 1)
                    || !ops.removeItem(level, player, filter, 1)) {
                return false;
            }
            if (!ops.machineReceiveEnergy(level, player, pos, machineId, rules.radiationCleanserEnergy())) {
                return false;
            }
            if (!tickRepeated(level, player, pos, machineId, rules.radiationCleanserTicks(), ops)) {
                return false;
            }
            String output = ops.cleanOutputForContaminated(input);
            if (!ops.machineExtractItem(level, player, pos, machineId, output, 1)) {
                return false;
            }
            return ops.giveItem(player, output, 1);
        }
        if (hasAny(path, rules.medicalMachinePathHints())) {
            if (!ops.machineReceiveEnergy(level, player, pos, machineId, rules.medicalMachineEnergy())) {
                return false;
            }
            return tickRepeated(level, player, pos, machineId, rules.medicalMachineTicks(), ops);
        }
        if (!ops.machineReceiveEnergy(level, player, pos, machineId, rules.hazardMachineEnergy())) {
            return false;
        }
        return tickRepeated(level, player, pos, machineId, rules.hazardMachineTicks(), ops);
    }

    private static boolean researchLabAction(
            Object level,
            Object pos,
            Object player,
            NativeMachineOperationRules rules,
            Operations ops
    ) {
        String machineId = ops.blockActionMachineId("research_lab", "research_lab");
        if (!ops.machineUseBlock(level, player, pos, machineId)) {
            return false;
        }
        String schematic = ops.firstConfiguredItem(player, 1, rules.researchSchematicItemIds());
        if (!schematic.isBlank()) {
            return ops.researchLabAnalyze(level, player, "native_client_block_use");
        }
        return ops.machineTick(level, player, pos, machineId);
    }

    private static boolean generatorAction(
            String path,
            Object level,
            Object pos,
            Object player,
            NativeMachineOperationRules rules,
            Operations ops
    ) {
        String machineId = ops.productId(path);
        if (!ops.machineUseBlock(level, player, pos, machineId)) {
            return false;
        }
        String insertedFuel = "";
        boolean fuelAvailable = false;
        for (String fuel : ops.configuredIds(rules.generatorFuelItemIds())) {
            if (!ops.hasItem(player, fuel, 1)) {
                continue;
            }
            fuelAvailable = true;
            if (ops.machineInsertItem(level, player, pos, machineId, fuel, 1)) {
                insertedFuel = fuel;
                break;
            }
        }
        if (fuelAvailable && insertedFuel.isBlank()) {
            return false;
        }
        if (!insertedFuel.isBlank() && !ops.removeItem(level, player, insertedFuel, 1)) {
            return false;
        }
        return ops.machineTick(level, player, pos, machineId);
    }

    private static boolean gridAction(
            String path,
            Object level,
            Object pos,
            Object player,
            NativeMachineOperationRules rules,
            Operations ops
    ) {
        String machineId = ops.productId(path);
        boolean used = ops.machineUseBlock(level, player, pos, machineId);
        if (!used) {
            return false;
        }
        String fuel = ops.firstConfiguredItem(player, 1, rules.gridChargeItemIds());
        if (!fuel.isBlank()) {
            int amount = ops.energyItemCharge(fuel);
            if (!ops.machineReceiveEnergy(level, player, pos, machineId, amount)) {
                return false;
            }
            if (!ops.removeItem(level, player, fuel, 1)) {
                return false;
            }
            return ops.machineTick(level, player, pos, machineId);
        }
        if (!ops.machineExtractEnergy(level, player, pos, machineId, rules.gridExtractEnergy())) {
            return false;
        }
        if (!ops.giveItem(player, ops.configuredId(rules.gridExtractOutputItemId()), 1)) {
            return false;
        }
        return ops.machineTick(level, player, pos, machineId);
    }

    private static boolean processorAction(String path, Object level, Object pos, Object player, Operations ops) {
        String machineId = ops.productId(path);
        if (!ops.machineUseBlock(level, player, pos, machineId)) {
            return false;
        }
        return ops.machineTick(level, player, pos, machineId);
    }

    private static boolean tickRepeated(
            Object level,
            Object player,
            Object pos,
            String machineId,
            int ticks,
            Operations ops
    ) {
        boolean ticked = false;
        for (int tick = 0; tick < Math.max(1, ticks); tick++) {
            if (!ops.machineTick(level, player, pos, machineId)) {
                return false;
            }
            ticked = true;
        }
        return ticked;
    }

    private static boolean hasAny(String value, List<String> needles) {
        String safeValue = safe(value);
        if (safeValue.isBlank() || needles == null || needles.isEmpty()) {
            return false;
        }
        for (String needle : needles) {
            if (!safe(needle).isBlank() && safeValue.contains(safe(needle))) {
                return true;
            }
        }
        return false;
    }

    private static String safe(String value) {
        return value == null ? "" : value.toLowerCase(java.util.Locale.ROOT);
    }

    public record Operations(
            ProductId productId,
            BlockActionMachineId blockActionMachineId,
            ConfiguredId configuredId,
            ConfiguredIds configuredIds,
            FirstConfiguredItem firstConfiguredItem,
            EnergyItemCharge energyItemCharge,
            CleanOutputForContaminated cleanOutputForContaminated,
            PlayerItemCountPredicate hasItem,
            PlayerItemCountAction giveItem,
            LevelPlayerItemCountAction removeItem,
            MachineUseBlock machineUseBlock,
            MachineEnergyAction machineReceiveEnergy,
            MachineEnergyAction machineExtractEnergy,
            MachineItemAction machineInsertItem,
            MachineItemAction machineExtractItem,
            MachineTick machineTick,
            PowerNodeState powerNodeState,
            ResearchLabAnalyze researchLabAnalyze,
            ScannerUse scannerUse,
            TerminalOpened terminalOpened,
            HoloMapRoute holoMapRoute,
            OpenModuleSurface openModuleSurface,
            WaterFiltered waterFiltered
    ) {
        String productId(String path) {
            return productId.value(path);
        }

        String blockActionMachineId(String action, String fallbackPath) {
            return blockActionMachineId.value(action, fallbackPath);
        }

        String configuredId(String idOrPath) {
            return configuredId.value(idOrPath);
        }

        List<String> configuredIds(List<String> idsOrPaths) {
            return configuredIds.value(idsOrPaths);
        }

        String firstConfiguredItem(Object player, int count, List<String> idsOrPaths) {
            return firstConfiguredItem.value(player, count, idsOrPaths);
        }

        int energyItemCharge(String itemId) {
            return energyItemCharge.value(itemId);
        }

        String cleanOutputForContaminated(String input) {
            return cleanOutputForContaminated.value(input);
        }

        boolean hasItem(Object player, String itemId, int count) {
            return hasItem.test(player, itemId, count);
        }

        boolean giveItem(Object player, String itemId, int count) {
            return giveItem.run(player, itemId, count);
        }

        boolean removeItem(Object level, Object player, String itemId, int count) {
            return removeItem.run(level, player, itemId, count);
        }

        boolean machineUseBlock(Object level, Object player, Object pos, String machineId) {
            return machineUseBlock.run(level, player, pos, machineId);
        }

        boolean machineReceiveEnergy(Object level, Object player, Object pos, String machineId, int amount) {
            return machineReceiveEnergy.run(level, player, pos, machineId, amount);
        }

        boolean machineExtractEnergy(Object level, Object player, Object pos, String machineId, int amount) {
            return machineExtractEnergy.run(level, player, pos, machineId, amount);
        }

        boolean machineInsertItem(Object level, Object player, Object pos, String machineId, String itemId, int count) {
            return machineInsertItem.run(level, player, pos, machineId, itemId, count);
        }

        boolean machineExtractItem(Object level, Object player, Object pos, String machineId, String itemId, int count) {
            return machineExtractItem.run(level, player, pos, machineId, itemId, count);
        }

        boolean machineTick(Object level, Object player, Object pos, String machineId) {
            return machineTick.run(level, player, pos, machineId);
        }

        boolean powerNodeState(Object level, Object player, Object pos, boolean active, int activeNodeCount, String source) {
            return powerNodeState.run(level, player, pos, active, activeNodeCount, source);
        }

        boolean researchLabAnalyze(Object level, Object player, String source) {
            return researchLabAnalyze.run(level, player, source);
        }

        boolean scannerUse(Object level, Object player, String scannerId, boolean portable) {
            return scannerUse.run(level, player, scannerId, portable);
        }

        boolean terminalOpened(Object level, Object player, String route) {
            return terminalOpened.run(level, player, route);
        }

        String holoMapRoute(String path) {
            return holoMapRoute.value(path);
        }

        boolean openModuleSurface(String namespace, String surface) {
            return openModuleSurface.run(namespace, surface);
        }

        boolean waterFiltered(Object level, Object player, String source) {
            return waterFiltered.run(level, player, source);
        }
    }

    public interface ProductId {
        String value(String path);
    }

    public interface BlockActionMachineId {
        String value(String action, String fallbackPath);
    }

    public interface ConfiguredId {
        String value(String idOrPath);
    }

    public interface ConfiguredIds {
        List<String> value(List<String> idsOrPaths);
    }

    public interface FirstConfiguredItem {
        String value(Object player, int count, List<String> idsOrPaths);
    }

    public interface EnergyItemCharge {
        int value(String itemId);
    }

    public interface CleanOutputForContaminated {
        String value(String input);
    }

    public interface PlayerItemCountPredicate {
        boolean test(Object player, String itemId, int count);
    }

    public interface PlayerItemCountAction {
        boolean run(Object player, String itemId, int count);
    }

    public interface LevelPlayerItemCountAction {
        boolean run(Object level, Object player, String itemId, int count);
    }

    public interface MachineUseBlock {
        boolean run(Object level, Object player, Object pos, String machineId);
    }

    public interface MachineEnergyAction {
        boolean run(Object level, Object player, Object pos, String machineId, int amount);
    }

    public interface MachineItemAction {
        boolean run(Object level, Object player, Object pos, String machineId, String itemId, int count);
    }

    public interface MachineTick {
        boolean run(Object level, Object player, Object pos, String machineId);
    }

    public interface PowerNodeState {
        boolean run(Object level, Object player, Object pos, boolean active, int activeNodeCount, String source);
    }

    public interface ResearchLabAnalyze {
        boolean run(Object level, Object player, String source);
    }

    public interface ScannerUse {
        boolean run(Object level, Object player, String scannerId, boolean portable);
    }

    public interface TerminalOpened {
        boolean run(Object level, Object player, String route);
    }

    public interface HoloMapRoute {
        String value(String path);
    }

    public interface OpenModuleSurface {
        boolean run(String namespace, String surface);
    }

    public interface WaterFiltered {
        boolean run(Object level, Object player, String source);
    }
}
