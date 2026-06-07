package dev.echo.nativeplatform.loader;

import dev.echo.nativeplatform.contracts.EchoNativeBootstrapProductProfile.NativeFieldActionRoute;
import dev.echo.nativeplatform.contracts.EchoNativeBootstrapProductProfile.NativeMachineScenarioRule;
import dev.echo.nativeplatform.contracts.EchoNativeBootstrapProductProfile.NativeRewardGrant;
import dev.echo.nativeplatform.contracts.EchoNativeBootstrapProductProfile.NativeWorldPaintPlacement;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class NativeLoaderProductActionRouter {
    public static final String SERVICE_ID = "echo.native.product_action_router";

    private NativeLoaderProductActionRouter() {
    }

    public static String runItem(
            String path,
            Object level,
            Object anchor,
            Object player,
            String routeKey,
            Context context
    ) {
        String productName = context.productName();
        String actionKey = context.actionKey(path);
        NativeFieldActionRoute route = context.itemFieldAction(actionKey);
        if (route == null) {
            route = context.itemFieldAction("default");
        }
        String routed = context.runFieldAction(
                route,
                path,
                level,
                anchor,
                player,
                routeKey,
                false
        );
        if (routed != null) {
            return routed;
        }
        return productName + " item executed its native field loop with distinct world support.";
    }

    public static String runBlock(
            String path,
            Object level,
            Object pos,
            Object player,
            String routeKey,
            Context context
    ) {
        String productName = context.productName();
        String actionKey = context.actionKey(path);
        NativeFieldActionRoute route = context.blockFieldAction(actionKey);
        if (route == null) {
            route = context.blockFieldAction("default");
        }
        String routed = context.runFieldAction(
                route,
                path,
                level,
                pos,
                player,
                routeKey,
                true
        );
        if (routed != null) {
            return routed;
        }
        return productName + " block executed its native gameplay role.";
    }

    private static boolean hasAny(String value, List<String> needles) {
        String haystack = value == null ? "" : value.toLowerCase(Locale.ROOT);
        if (needles == null) {
            return false;
        }
        for (String needle : needles) {
            if (needle != null && !needle.isBlank() && haystack.contains(needle.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    public static final class Context {
        private final String productName;
        private final String machineRuntimeClass;
        private final String infoFallbackItemId;
        private final List<NativeMachineScenarioRule> machineScenarioRules;
        private final List<NativeFieldActionRoute> itemFieldActionRoutes;
        private final List<NativeFieldActionRoute> blockFieldActionRoutes;
        private final ActionKeyResolver actionKeyResolver;
        private final NativeLoaderProductWorldPainter.Context worldPainter;
        private final ProductIdResolver productIdResolver;
        private final ScenarioRunner scenarioRunner;
        private final ScenarioSummarizer scenarioSummarizer;
        private final AnyItemGranter anyItemGranter;
        private final ItemGranter itemGranter;
        private final AnyBlockSetter anyBlockSetter;
        private final CommandExecutor commandExecutor;
        private final OneShotGate oneShotGate;
        private final ActionBarSender actionBarSender;
        private final BlockPositionFormatter blockPositionFormatter;
        private final BlockRewardGranter blockRewardGranter;

        public Context(
                String productName,
                String machineRuntimeClass,
                String infoFallbackItemId,
                List<NativeMachineScenarioRule> machineScenarioRules,
                List<NativeFieldActionRoute> itemFieldActionRoutes,
                List<NativeFieldActionRoute> blockFieldActionRoutes,
                ActionKeyResolver actionKeyResolver,
                NativeLoaderProductWorldPainter.Context worldPainter,
                ProductIdResolver productIdResolver,
                ScenarioRunner scenarioRunner,
                ScenarioSummarizer scenarioSummarizer,
                AnyItemGranter anyItemGranter,
                ItemGranter itemGranter,
                AnyBlockSetter anyBlockSetter,
                CommandExecutor commandExecutor,
                OneShotGate oneShotGate,
                ActionBarSender actionBarSender,
                BlockPositionFormatter blockPositionFormatter,
                BlockRewardGranter blockRewardGranter
        ) {
            this.productName = productName == null || productName.isBlank() ? "Native product" : productName;
            this.machineRuntimeClass = machineRuntimeClass == null ? "" : machineRuntimeClass;
            this.infoFallbackItemId = infoFallbackItemId == null ? "" : infoFallbackItemId;
            this.machineScenarioRules = machineScenarioRules == null ? List.of() : List.copyOf(machineScenarioRules);
            this.itemFieldActionRoutes = itemFieldActionRoutes == null ? List.of() : List.copyOf(itemFieldActionRoutes);
            this.blockFieldActionRoutes = blockFieldActionRoutes == null ? List.of() : List.copyOf(blockFieldActionRoutes);
            this.actionKeyResolver = actionKeyResolver;
            this.worldPainter = worldPainter;
            this.productIdResolver = productIdResolver;
            this.scenarioRunner = scenarioRunner;
            this.scenarioSummarizer = scenarioSummarizer;
            this.anyItemGranter = anyItemGranter;
            this.itemGranter = itemGranter;
            this.anyBlockSetter = anyBlockSetter;
            this.commandExecutor = commandExecutor;
            this.oneShotGate = oneShotGate;
            this.actionBarSender = actionBarSender;
            this.blockPositionFormatter = blockPositionFormatter;
            this.blockRewardGranter = blockRewardGranter;
        }

        String productName() {
            return productName;
        }

        String infoFallbackItemId() {
            return infoFallbackItemId;
        }

        NativeLoaderProductWorldPainter.Context worldPainter() {
            return worldPainter;
        }

        String actionKey(String path) {
            String resolved = actionKeyResolver == null ? "" : actionKeyResolver.resolve(path);
            return resolved == null ? "" : resolved.toLowerCase(Locale.ROOT);
        }

        NativeFieldActionRoute itemFieldAction(String actionKey) {
            return fieldAction(itemFieldActionRoutes, actionKey);
        }

        NativeFieldActionRoute blockFieldAction(String actionKey) {
            return fieldAction(blockFieldActionRoutes, actionKey);
        }

        private NativeFieldActionRoute fieldAction(List<NativeFieldActionRoute> routes, String actionKey) {
            String requested = actionKey == null ? "" : actionKey.toLowerCase(Locale.ROOT);
            if (requested.isBlank()) {
                return null;
            }
            for (NativeFieldActionRoute route : routes) {
                if (route != null && requested.equals(route.actionKey().toLowerCase(Locale.ROOT))) {
                    return route;
                }
            }
            return null;
        }

        String runFieldAction(
                NativeFieldActionRoute route,
                String path,
                Object level,
                Object pos,
                Object player,
                String routeKey,
                boolean blockRoute
        ) {
            if (route == null) {
                return null;
            }
            Map<String, Object> scenario = Map.of();
            String scenarioMethod = route.scenarioFromPath() ? machineScenarioFor(path) : route.scenarioMethod();
            if (scenarioMethod != null && !scenarioMethod.isBlank()) {
                scenario = runMachineScenario(scenarioMethod);
            }
            if (!route.paintStyle().isBlank()) {
                worldPainter.paintRecipe(route.paintStyle(), level, pos);
            }
            for (NativeWorldPaintPlacement placement : route.blockPlacements()) {
                worldPainter.setPlacement(level, pos, placement);
            }
            grantAll(player, route.grants());
            if (!route.oneShotSuffix().isBlank()) {
                String key = routeKey == null ? "" : routeKey;
                if (blockRoute && route.oneShotUsesBlockPosition()) {
                    key += ":" + blockPosText(pos);
                }
                key += ":" + route.oneShotSuffix();
                if (oneShot(player, key)) {
                    grantAll(player, route.oneShotGrants());
                }
            }
            for (String command : route.commands()) {
                if (command != null && !command.isBlank()) {
                    executeCommand(player, command);
                }
            }
            if (route.grantBlockReward()) {
                grantBlockReward(player, productId(path));
            }
            if (!route.actionBarText().isBlank()) {
                actionBar(player, route.actionBarText(), route.actionBarColor());
            }
            String summary = fieldSummary(route);
            if (scenario == null || scenario.isEmpty()) {
                return summary;
            }
            return scenarioSummary(scenario, summary);
        }

        private void grantAll(Object player, List<NativeRewardGrant> grants) {
            if (grants == null) {
                return;
            }
            for (NativeRewardGrant grant : grants) {
                grant(player, grant);
            }
        }

        private boolean grant(Object player, NativeRewardGrant grant) {
            if (grant == null) {
                return false;
            }
            String itemId = grant.itemId();
            String fallbackItemId = grant.fallbackItemId();
            int count = Math.max(1, grant.count());
            int fallbackCount = grant.fallbackCount() <= 0 ? count : grant.fallbackCount();
            if (itemId != null && !itemId.isBlank() && grant(player, itemId, count)) {
                return true;
            }
            return fallbackItemId != null && !fallbackItemId.isBlank()
                    && grant(player, fallbackItemId, fallbackCount);
        }

        private String fieldSummary(NativeFieldActionRoute route) {
            String summary = route.summary();
            if (summary == null || summary.isBlank()) {
                return productName + " executed configured native field action.";
            }
            return summary.replace("{product}", productName);
        }

        String productId(String path) {
            return productIdResolver.id(path);
        }

        Map<String, Object> runMachineScenario(String methodName) {
            return scenarioRunner.run(machineRuntimeClass, methodName);
        }

        String machineScenarioFor(String path) {
            String safePath = path == null ? "" : path.toLowerCase(Locale.ROOT);
            for (NativeMachineScenarioRule rule : machineScenarioRules) {
                if (rule != null && hasAny(safePath, rule.pathHints()) && !rule.scenarioMethod().isBlank()) {
                    return rule.scenarioMethod();
                }
            }
            return "runDefaultScenario";
        }

        String scenarioSummary(Map<String, Object> scenario, String fallback) {
            return scenarioSummarizer.summary(scenario, fallback);
        }

        boolean grantAny(Object player, String first, String fallback) {
            return anyItemGranter.grant(player, first, fallback);
        }

        boolean grant(Object player, String itemId, int count) {
            return itemGranter.grant(player, itemId, count);
        }

        boolean setAny(Object level, Object pos, int dx, int dy, int dz, String first, String fallback) {
            return anyBlockSetter.setNear(level, pos, dx, dy, dz, first, fallback);
        }

        boolean executeCommand(Object player, String command) {
            return commandExecutor.execute(player, command);
        }

        boolean oneShot(Object player, String key) {
            return oneShotGate.accept(player, key);
        }

        void actionBar(Object player, String text, String color) {
            actionBarSender.send(player, text, color);
        }

        String blockPosText(Object pos) {
            return blockPositionFormatter.text(pos);
        }

        void grantBlockReward(Object player, String blockId) {
            blockRewardGranter.grant(player, blockId);
        }
    }

    @FunctionalInterface
    public interface ProductIdResolver {
        String id(String path);
    }

    @FunctionalInterface
    public interface ActionKeyResolver {
        String resolve(String path);
    }

    @FunctionalInterface
    public interface ScenarioRunner {
        Map<String, Object> run(String className, String methodName);
    }

    @FunctionalInterface
    public interface ScenarioSummarizer {
        String summary(Map<String, Object> scenario, String fallback);
    }

    @FunctionalInterface
    public interface AnyItemGranter {
        boolean grant(Object player, String first, String fallback);
    }

    @FunctionalInterface
    public interface ItemGranter {
        boolean grant(Object player, String itemId, int count);
    }

    @FunctionalInterface
    public interface AnyBlockSetter {
        boolean setNear(Object level, Object pos, int dx, int dy, int dz, String first, String fallback);
    }

    @FunctionalInterface
    public interface CommandExecutor {
        boolean execute(Object player, String command);
    }

    @FunctionalInterface
    public interface OneShotGate {
        boolean accept(Object player, String key);
    }

    @FunctionalInterface
    public interface ActionBarSender {
        void send(Object player, String text, String color);
    }

    @FunctionalInterface
    public interface BlockPositionFormatter {
        String text(Object pos);
    }

    @FunctionalInterface
    public interface BlockRewardGranter {
        void grant(Object player, String blockId);
    }
}
