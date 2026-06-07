package dev.echo.nativeplatform.loader;

public final class NativeLoaderProductItemActionExecutor {
    public static final String SERVICE_ID = "echo.native.product_item_action_executor";

    private NativeLoaderProductItemActionExecutor() {
    }

    public static boolean execute(
            String action,
            String path,
            String itemId,
            Object level,
            Object pos,
            Object player,
            Object handOrStack,
            Operations ops
    ) {
        Object stack = ops.heldItemStack(player, handOrStack);
        return switch (safe(action)) {
            case "water_bottle" -> waterBottleAction(path, itemId, level, player, handOrStack, ops);
            case "radiation_medicine" -> radiationMedicineAction(itemId, level, player, ops);
            case "mutagen_delegate" -> ops.invokeRealItemUse(level, player, handOrStack, itemId);
            case "filter_cartridge" -> filterCartridgeAction(path, itemId, level, player, ops);
            case "crude_filter" -> ops.crudeFilterUsed(level, player, handOrStack);
            case "bandage" -> healingAction(itemId, level, player, handOrStack, 4.0F, "", ops);
            case "stim_pack" -> healingAction(
                    itemId,
                    level,
                    player,
                    handOrStack,
                    8.0F,
                    "effect give @s minecraft:regeneration 8 1 true",
                    ops);
            case "hand_warmer" -> handWarmerAction(level, player, handOrStack, stack, ops);
            case "scanner" -> scannerAction(path, level, player, handOrStack, stack, ops);
            case "deploy_entity" -> deployEntityAction(level, pos, player, itemId, ops);
            case "beacon_keystone" -> path.contains("keystone")
                    && ops.invokeRealItemUse(level, player, handOrStack, itemId);
            case "weather_dampener" -> ops.invokeRealItemUse(level, player, handOrStack, itemId);
            case "archive_item" -> archiveAction(path, itemId, level, player, handOrStack, ops);
            default -> false;
        };
    }

    public static boolean handles(String action) {
        return switch (safe(action)) {
            case "water_bottle",
                    "radiation_medicine",
                    "mutagen_delegate",
                    "filter_cartridge",
                    "crude_filter",
                    "bandage",
                    "stim_pack",
                    "hand_warmer",
                    "scanner",
                    "deploy_entity",
                    "beacon_keystone",
                    "weather_dampener",
                    "archive_item" -> true;
            default -> false;
        };
    }

    private static boolean waterBottleAction(
            String path,
            String itemId,
            Object level,
            Object player,
            Object handOrStack,
            Operations ops
    ) {
        if (!ops.waterBottleUsed(level, player, handOrStack, itemId, path)) {
            return false;
        }
        if (!ops.removeConsumableItem(level, player, itemId, 1)) {
            return false;
        }
        return ops.isCreativePlayer(player) || ops.giveItem(player, "minecraft:glass_bottle", 1);
    }

    private static boolean radiationMedicineAction(String itemId, Object level, Object player, Operations ops) {
        if (!ops.radAwayUsed(level, player, "native_client_item_use")) {
            return false;
        }
        ops.executeCommand(player, "effect clear @s minecraft:poison");
        ops.executeCommand(player, "effect clear @s minecraft:wither");
        ops.executeCommand(player, "effect clear @s minecraft:nausea");
        ops.executeCommand(player, "effect give @s minecraft:regeneration 5 1 true");
        return ops.removeConsumableItem(level, player, itemId, 1);
    }

    private static boolean filterCartridgeAction(
            String path,
            String itemId,
            Object level,
            Object player,
            Operations ops
    ) {
        int refillAmount = path.contains("elite") ? 60 : path.contains("advanced") ? 35 : 20;
        int tier = path.contains("elite") ? 3 : path.contains("advanced") ? 2 : 1;
        String tierName = path.contains("elite") ? "elite" : path.contains("advanced") ? "advanced" : "basic";
        if (!ops.filterCartridgeUsed(level, player, itemId, tierName, tier, refillAmount)) {
            return false;
        }
        ops.executeCommand(player, "effect give @s minecraft:water_breathing 180 0 true");
        ops.executeCommand(player, "effect give @s minecraft:resistance 60 0 true");
        return ops.removeConsumableItem(level, player, itemId, 1);
    }

    private static boolean healingAction(
            String itemId,
            Object level,
            Object player,
            Object handOrStack,
            float amount,
            String extraCommand,
            Operations ops
    ) {
        if (!ops.itemConsumed(level, player, handOrStack)) {
            return false;
        }
        if (!ops.healPlayer(player, amount)) {
            return false;
        }
        if (extraCommand != null && !extraCommand.isBlank()) {
            ops.executeCommand(player, extraCommand);
        }
        return ops.removeConsumableItem(level, player, itemId, 1);
    }

    private static boolean handWarmerAction(
            Object level,
            Object player,
            Object handOrStack,
            Object stack,
            Operations ops
    ) {
        if (!ops.handWarmerUsed(level, player, handOrStack, 8)) {
            return false;
        }
        if (!ops.executeCommand(player, "effect give @s minecraft:fire_resistance 180 0 true")) {
            return false;
        }
        return ops.isCreativePlayer(player) || ops.damageOrShrinkItemStack(stack, player, handOrStack, 1);
    }

    private static boolean scannerAction(
            String path,
            Object level,
            Object player,
            Object handOrStack,
            Object stack,
            Operations ops
    ) {
        boolean deepScan = ops.isShiftKeyDown(player) || path.contains("lens");
        String recoveryPath = ops.productPath(ops.recoveryItemId());
        String source = deepScan ? recoveryPath + "_deep_scan" : recoveryPath;
        if (!ops.scannerUse(level, player, source, deepScan)) {
            return false;
        }
        return ops.isCreativePlayer(player)
                || ops.damageOrShrinkItemStack(stack, player, handOrStack, deepScan ? 3 : 1);
    }

    private static boolean deployEntityAction(
            Object level,
            Object pos,
            Object player,
            String itemId,
            Operations ops
    ) {
        String entityId = ops.deployableEntityId(itemId);
        if (entityId.isBlank() || !ops.registryContains("ENTITY_TYPE", entityId)) {
            return false;
        }
        if (!ops.deployEntityRoute(level, player, pos, "native_client_item_use")) {
            return false;
        }
        boolean summoned = ops.executeCommand(player, "summon " + entityId + " ~ ~1 ~");
        return summoned && ops.removeConsumableItem(level, player, itemId, 1);
    }

    private static boolean archiveAction(
            String path,
            String itemId,
            Object level,
            Object player,
            Object handOrStack,
            Operations ops
    ) {
        boolean runtimeRecorded = path.contains("data_log")
                ? ops.dataLogRecovered(level, player, path, path)
                : ops.itemConsumed(level, player, handOrStack);
        if (!runtimeRecorded) {
            return false;
        }
        if (path.contains("rare")
                && !ops.giveItem(player, ops.productId("schematic_fragment_machines"), 1)
                && !ops.giveItem(player, ops.productId("schematic_fragment"), 1)) {
            return false;
        }
        if (!ops.removeConsumableItem(level, player, itemId, 1)) {
            return false;
        }
        return ops.openModuleSurface("echoindex", "index");
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim().toLowerCase(java.util.Locale.ROOT);
    }

    public record Operations(
            HeldItemStack heldItemStack,
            WaterBottleUsed waterBottleUsed,
            RemoveItem removeConsumableItem,
            PlayerPredicate isCreativePlayer,
            GiveItem giveItem,
            SourceAction radAwayUsed,
            CommandExecutor executeCommand,
            RealItemUse invokeRealItemUse,
            FilterCartridgeUsed filterCartridgeUsed,
            ItemUse crudeFilterUsed,
            ItemUse itemConsumed,
            PlayerHealer healPlayer,
            HandWarmerUsed handWarmerUsed,
            DamageOrShrinkItemStack damageOrShrinkItemStack,
            PlayerPredicate isShiftKeyDown,
            RecoveryItemId recoveryItemIdProvider,
            ProductPath productPath,
            ScannerUse scannerUse,
            RegistryContains registryContains,
            ProductId productId,
            DeployableEntityId deployableEntityId,
            SourcePositionAction deployEntityRoute,
            DataLogRecovered dataLogRecovered,
            OpenModuleSurface openModuleSurface
    ) {
        Object heldItemStack(Object player, Object handOrStack) {
            return heldItemStack.value(player, handOrStack);
        }

        boolean waterBottleUsed(Object level, Object player, Object handOrStack, String itemId, String path) {
            return waterBottleUsed.run(level, player, handOrStack, itemId, path);
        }

        boolean removeConsumableItem(Object level, Object player, String itemId, int count) {
            return removeConsumableItem.run(level, player, itemId, count);
        }

        boolean isCreativePlayer(Object player) {
            return isCreativePlayer.test(player);
        }

        boolean giveItem(Object player, String itemId, int count) {
            return giveItem.run(player, itemId, count);
        }

        boolean radAwayUsed(Object level, Object player, String source) {
            return radAwayUsed.run(level, player, source);
        }

        boolean executeCommand(Object player, String command) {
            return executeCommand.run(player, command);
        }

        boolean invokeRealItemUse(Object level, Object player, Object handOrStack, String itemId) {
            return invokeRealItemUse.run(level, player, handOrStack, itemId);
        }

        boolean filterCartridgeUsed(
                Object level,
                Object player,
                String itemId,
                String tierName,
                int tier,
                int refillAmount
        ) {
            return filterCartridgeUsed.run(level, player, itemId, tierName, tier, refillAmount);
        }

        boolean crudeFilterUsed(Object level, Object player, Object handOrStack) {
            return crudeFilterUsed.run(level, player, handOrStack);
        }

        boolean itemConsumed(Object level, Object player, Object handOrStack) {
            return itemConsumed.run(level, player, handOrStack);
        }

        boolean healPlayer(Object player, float amount) {
            return healPlayer.run(player, amount);
        }

        boolean handWarmerUsed(Object level, Object player, Object handOrStack, int warmthDelta) {
            return handWarmerUsed.run(level, player, handOrStack, warmthDelta);
        }

        boolean damageOrShrinkItemStack(Object stack, Object player, Object handOrStack, int amount) {
            return damageOrShrinkItemStack.run(stack, player, handOrStack, amount);
        }

        boolean isShiftKeyDown(Object player) {
            return isShiftKeyDown.test(player);
        }

        String recoveryItemId() {
            return recoveryItemIdProvider.value();
        }

        String productPath(String idOrPath) {
            return productPath.value(idOrPath);
        }

        boolean scannerUse(Object level, Object player, String source, boolean deepScan) {
            return scannerUse.run(level, player, source, deepScan);
        }

        boolean registryContains(String registryField, String contentId) {
            return registryContains.test(registryField, contentId);
        }

        String productId(String path) {
            return productId.value(path);
        }

        String deployableEntityId(String itemId) {
            return deployableEntityId.value(itemId);
        }

        boolean deployEntityRoute(Object level, Object player, Object pos, String source) {
            return deployEntityRoute.run(level, player, pos, source);
        }

        boolean dataLogRecovered(Object level, Object player, String logType, String title) {
            return dataLogRecovered.run(level, player, logType, title);
        }

        boolean openModuleSurface(String namespace, String path) {
            return openModuleSurface.run(namespace, path);
        }
    }

    public interface HeldItemStack {
        Object value(Object player, Object handOrStack);
    }

    public interface WaterBottleUsed {
        boolean run(Object level, Object player, Object handOrStack, String itemId, String path);
    }

    public interface RemoveItem {
        boolean run(Object level, Object player, String itemId, int count);
    }

    public interface PlayerPredicate {
        boolean test(Object player);
    }

    public interface GiveItem {
        boolean run(Object player, String itemId, int count);
    }

    public interface SourceAction {
        boolean run(Object level, Object player, String source);
    }

    public interface CommandExecutor {
        boolean run(Object player, String command);
    }

    public interface RealItemUse {
        boolean run(Object level, Object player, Object handOrStack, String itemId);
    }

    public interface FilterCartridgeUsed {
        boolean run(Object level, Object player, String itemId, String tierName, int tier, int refillAmount);
    }

    public interface ItemUse {
        boolean run(Object level, Object player, Object handOrStack);
    }

    public interface PlayerHealer {
        boolean run(Object player, float amount);
    }

    public interface HandWarmerUsed {
        boolean run(Object level, Object player, Object handOrStack, int warmthDelta);
    }

    public interface DamageOrShrinkItemStack {
        boolean run(Object stack, Object player, Object handOrStack, int amount);
    }

    public interface RecoveryItemId {
        String value();
    }

    public interface ProductPath {
        String value(String idOrPath);
    }

    public interface ScannerUse {
        boolean run(Object level, Object player, String source, boolean deepScan);
    }

    public interface RegistryContains {
        boolean test(String registryField, String contentId);
    }

    public interface DeployableEntityId {
        String value(String path);
    }

    public interface ProductId {
        String value(String path);
    }

    public interface SourcePositionAction {
        boolean run(Object level, Object player, Object pos, String source);
    }

    public interface DataLogRecovered {
        boolean run(Object level, Object player, String logType, String title);
    }

    public interface OpenModuleSurface {
        boolean run(String namespace, String path);
    }
}
