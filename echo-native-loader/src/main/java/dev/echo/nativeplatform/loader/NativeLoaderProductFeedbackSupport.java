package dev.echo.nativeplatform.loader;

import dev.echo.nativeplatform.contracts.EchoNativeBootstrapProductProfile.NativeRewardGrant;
import dev.echo.nativeplatform.contracts.EchoNativeBootstrapProductProfile.NativeRewardRule;

import java.util.List;
import java.util.Map;
import java.util.Set;

public final class NativeLoaderProductFeedbackSupport {
    public static final String SERVICE_ID = "echo.native.product_feedback_support";

    private static final Map<String, Long> THROTTLE = new java.util.concurrent.ConcurrentHashMap<>();
    private static final Set<String> ONE_SHOT_EVENTS = java.util.concurrent.ConcurrentHashMap.newKeySet();

    private NativeLoaderProductFeedbackSupport() {
    }

    public static boolean oneShot(String key) {
        return key != null && !key.isBlank() && ONE_SHOT_EVENTS.add(key);
    }

    public static boolean playerOneShot(Context context, Object player, String key) {
        return oneShot(playerKey(context, player) + ":" + key);
    }

    public static boolean cooldown(Context context, Object player, String key, long cooldownMillis) {
        String safeKey = "cooldown:" + playerKey(context, player) + ":" + key;
        long now = System.currentTimeMillis();
        Long previous = THROTTLE.put(safeKey, now);
        return previous == null || now - previous >= Math.max(250L, cooldownMillis);
    }

    public static void actionBar(Object player, String text, String color) {
        // Text feedback is disabled for real module runtime routing.
    }

    public static void feedbackOnce(String key, String message, long throttleMillis) {
        if (message == null || message.isBlank()) {
            return;
        }
        String safeKey = key == null || key.isBlank() ? message : key;
        long now = System.currentTimeMillis();
        Long previous = THROTTLE.put(safeKey, now);
        if (previous != null && now - previous < Math.max(250L, throttleMillis)) {
            return;
        }
        // Keep throttle state for diagnostics, but do not surface generated beta feedback to players.
    }

    public static void grantReward(Context context, Object player, String contentId, List<NativeRewardRule> rewardRules) {
        String id = lowerContentId(contentId);
        if (id.isBlank() || rewardRules == null || rewardRules.isEmpty()) {
            return;
        }
        for (NativeRewardRule rule : rewardRules) {
            if (rule == null || !hasAny(id, rule.pathHints())) {
                continue;
            }
            for (NativeRewardGrant grant : rule.grants()) {
                grantItem(context, player, grant);
            }
            return;
        }
    }

    public static void tag(Context context, Object player, String tag) {
        if (player == null || tag == null || tag.isBlank()) {
            return;
        }
        String sanitized = ("echo_"
                + context.gameplayPackId().toLowerCase(java.util.Locale.ROOT)
                + "_"
                + tag.toLowerCase(java.util.Locale.ROOT))
                .replaceAll("[^a-z0-9_]", "_");
        if (sanitized.length() > 64) {
            sanitized = sanitized.substring(0, 64);
        }
        try {
            player.getClass().getMethod("addTag", String.class).invoke(player, sanitized);
        } catch (Throwable ignored) {
            // Tags are best-effort vanilla persistence; runtime maps still hold current-session state.
        }
    }

    private static void grantItem(Context context, Object player, NativeRewardGrant grant) {
        if (grant == null) {
            return;
        }
        String itemId = lowerContentId(grant.itemId());
        if (!itemId.isBlank() && context.grantItem(player, itemId, grant.count())) {
            return;
        }
        String fallbackItemId = lowerContentId(grant.fallbackItemId());
        if (!fallbackItemId.isBlank()) {
            int fallbackCount = grant.fallbackCount() <= 0 ? grant.count() : grant.fallbackCount();
            context.grantItem(player, fallbackItemId, fallbackCount);
        }
    }

    private static String playerKey(Context context, Object player) {
        Object uuid = context.methodValue(player, "getUUID");
        return uuid == null ? "global" : String.valueOf(uuid);
    }

    private static boolean hasAny(String value, List<String> needles) {
        String haystack = lowerContentId(value);
        if (needles == null) {
            return false;
        }
        for (String needle : needles) {
            if (needle != null && !needle.isBlank() && haystack.contains(lowerContentId(needle))) {
                return true;
            }
        }
        return false;
    }

    private static String lowerContentId(String contentId) {
        return contentId == null ? "" : contentId.trim().toLowerCase(java.util.Locale.ROOT);
    }

    public static final class Context {
        private final String gameplayPackId;
        private final MethodReader methodReader;
        private final ItemGranter itemGranter;

        public Context(String gameplayPackId, MethodReader methodReader, ItemGranter itemGranter) {
            this.gameplayPackId = gameplayPackId == null ? "" : gameplayPackId;
            this.methodReader = methodReader;
            this.itemGranter = itemGranter;
        }

        private String gameplayPackId() {
            return gameplayPackId;
        }

        private Object methodValue(Object target, String methodName) {
            return methodReader.get(target, methodName);
        }

        private boolean grantItem(Object player, String itemId, int count) {
            return itemGranter.grant(player, itemId, count);
        }
    }

    @FunctionalInterface
    public interface MethodReader {
        Object get(Object target, String methodName);
    }

    @FunctionalInterface
    public interface ItemGranter {
        boolean grant(Object player, String itemId, int count);
    }
}
