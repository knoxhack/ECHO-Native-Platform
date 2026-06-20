package com.echo;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

final class EchoNativeDevDirectWorldPromptBypass {
    static final String ENABLED_PROPERTY = "echo.native.devDirectAutoConfirmExperimentalWorld";
    static final String QUICKPLAY_SINGLEPLAYER_PROPERTY = "echo.native.devDirectQuickPlaySingleplayer";
    private static final String AUTHORIZED_HANDOFF_PROPERTY = "echo.native.bootstrap.authorizedHandoff";
    private static final String AUTHORIZED_HANDOFF_VALUE = "startNativeClient";
    private static final String MINECRAFT_CLASS = "net.minecraft.client.Minecraft";
    private static final String TITLE_SCREEN_CLASS = "net.minecraft.client.gui.screens.TitleScreen";
    private static final String NATIVE_DASHBOARD_SCREEN_CLASS =
            "dev.echo.nativeplatform.generated.EchoNativeDashboardScreen";
    private static final String BACKUP_CONFIRM_SCREEN_CLASS = "net.minecraft.client.gui.screens.BackupConfirmScreen";
    private static final String BACKUP_CONFIRM_SCREEN_LISTENER_CLASS =
            "net.minecraft.client.gui.screens.BackupConfirmScreen$Listener";
    private static final long POLL_INTERVAL_MILLIS = 250L;
    private static final long TIMEOUT_MILLIS = 180_000L;
    private static final long QUICKPLAY_FALLBACK_GRACE_MILLIS = 12_000L;
    private static final AtomicBoolean STARTED = new AtomicBoolean(false);
    private static final AtomicBoolean SCHEDULED = new AtomicBoolean(false);
    private static final AtomicBoolean QUICKPLAY_DISPATCHED = new AtomicBoolean(false);
    private static final AtomicBoolean COMPLETED = new AtomicBoolean(false);

    private EchoNativeDevDirectWorldPromptBypass() {
    }

    static void startIfEnabled() {
        if (!enabled() || !STARTED.compareAndSet(false, true)) {
            return;
        }
        Thread worker = new Thread(EchoNativeDevDirectWorldPromptBypass::run, "echo-native-dev-direct-world-prompt-bypass");
        worker.setDaemon(true);
        worker.start();
        System.out.println("[ECHO Native Loader] Dev-direct experimental world prompt auto-confirm is armed.");
    }

    private static boolean enabled() {
        return Boolean.getBoolean(ENABLED_PROPERTY)
                && AUTHORIZED_HANDOFF_VALUE.equals(System.getProperty(AUTHORIZED_HANDOFF_PROPERTY));
    }

    private static void run() {
        long deadline = System.currentTimeMillis() + TIMEOUT_MILLIS;
        long quickPlayFallbackAt = 0L;
        while (!COMPLETED.get() && System.currentTimeMillis() < deadline) {
            try {
                Object minecraft = minecraft();
                Object screen = fieldValue(minecraft, "screen");
                if (gameplayOpen(minecraft)) {
                    COMPLETED.set(true);
                    if (QUICKPLAY_DISPATCHED.get()) {
                        System.out.println("[ECHO Native Loader] Dev-direct quick-play existing world is open.");
                    }
                    break;
                }
                if (isBackupConfirmScreen(screen) && SCHEDULED.compareAndSet(false, true)) {
                    scheduleProceed(minecraft, screen);
                }
                if (!QUICKPLAY_DISPATCHED.get() && quickPlayFallbackCandidate(minecraft, screen)) {
                    long now = System.currentTimeMillis();
                    if (quickPlayFallbackAt <= 0L) {
                        quickPlayFallbackAt = now + QUICKPLAY_FALLBACK_GRACE_MILLIS;
                        System.out.println("[ECHO Native Loader] Dev-direct quick-play fallback armed for save '"
                                + quickPlaySaveName() + "' after vanilla quick-play grace period.");
                    } else if (now >= quickPlayFallbackAt && QUICKPLAY_DISPATCHED.compareAndSet(false, true)) {
                        scheduleQuickPlayOpen(minecraft, screen);
                    }
                } else if (!QUICKPLAY_DISPATCHED.get()) {
                    quickPlayFallbackAt = 0L;
                }
            } catch (ClassNotFoundException ignored) {
                // Minecraft classes are not visible until the client main is active.
            } catch (Throwable exception) {
                System.out.println("[ECHO Native Loader] Dev-direct world prompt auto-confirm failed: "
                        + failureMessage(exception));
                COMPLETED.set(true);
            }
            sleep();
        }
        if (!COMPLETED.get()) {
            System.out.println("[ECHO Native Loader] Dev-direct experimental world prompt auto-confirm expired without finding a prompt.");
        }
    }

    private static Object minecraft() throws ReflectiveOperationException {
        Class<?> minecraftClass = Class.forName(MINECRAFT_CLASS);
        return minecraftClass.getMethod("getInstance").invoke(null);
    }

    private static boolean isBackupConfirmScreen(Object screen) {
        return screen != null && BACKUP_CONFIRM_SCREEN_CLASS.equals(screen.getClass().getName());
    }

    private static boolean quickPlayFallbackCandidate(Object minecraft, Object screen) {
        String saveName = quickPlaySaveName();
        if (!safeSingleSaveName(saveName) || screen == null || isBackupConfirmScreen(screen)) {
            return false;
        }
        if (gameplayOpen(minecraft)) {
            return false;
        }
        String screenClass = screen.getClass().getName();
        String normalized = screenClass.toLowerCase(java.util.Locale.ROOT);
        if (normalized.contains("loading") || normalized.contains("progress") || normalized.contains("overlay")) {
            return false;
        }
        if (TITLE_SCREEN_CLASS.equals(screenClass)
                || NATIVE_DASHBOARD_SCREEN_CLASS.equals(screenClass)
                || screenClass.endsWith(".EchoNativeDashboardScreen")
                || normalized.contains("mainmenuscreen")
                || normalized.contains("title")) {
            return true;
        }
        return true;
    }

    private static boolean gameplayOpen(Object minecraft) {
        return optionalFieldValue(minecraft, "player") != null
                || optionalFieldValue(minecraft, "level") != null
                || optionalMethodValue(minecraft, "getSingleplayerServer") != null;
    }

    private static String quickPlaySaveName() {
        return System.getProperty(QUICKPLAY_SINGLEPLAYER_PROPERTY, "").trim();
    }

    private static boolean safeSingleSaveName(String saveName) {
        return saveName != null
                && !saveName.isBlank()
                && !saveName.equals(".")
                && !saveName.equals("..")
                && saveName.indexOf('/') < 0
                && saveName.indexOf('\\') < 0
                && saveName.indexOf(':') < 0;
    }

    private static void scheduleProceed(Object minecraft, Object observedScreen) throws ReflectiveOperationException {
        Runnable task = () -> proceedCurrentScreen(minecraft, observedScreen);
        Method execute = method(minecraft.getClass(), "execute", Runnable.class);
        if (execute == null) {
            task.run();
            return;
        }
        execute.setAccessible(true);
        execute.invoke(minecraft, task);
        System.out.println("[ECHO Native Loader] Dev-direct experimental world prompt auto-confirm scheduled.");
    }

    private static void scheduleQuickPlayOpen(Object minecraft, Object observedScreen) throws ReflectiveOperationException {
        Runnable task = () -> openQuickPlayCurrentScreen(minecraft, observedScreen);
        Method execute = method(minecraft.getClass(), "execute", Runnable.class);
        if (execute == null) {
            task.run();
            return;
        }
        execute.setAccessible(true);
        execute.invoke(minecraft, task);
        System.out.println("[ECHO Native Loader] Dev-direct quick-play fallback scheduled.");
    }

    private static void proceedCurrentScreen(Object minecraft, Object observedScreen) {
        try {
            Object screen = fieldValue(minecraft, "screen");
            if (!isBackupConfirmScreen(screen) || !Objects.equals(screen, observedScreen)) {
                SCHEDULED.set(false);
                return;
            }
            Object listener = fieldValue(screen, "onProceed");
            if (listener == null) {
                throw new NoSuchFieldException("BackupConfirmScreen.onProceed is null");
            }
            Method proceed = Class.forName(BACKUP_CONFIRM_SCREEN_LISTENER_CLASS)
                    .getMethod("proceed", boolean.class, boolean.class);
            proceed.invoke(listener, false, false);
            COMPLETED.set(true);
            System.out.println("[ECHO Native Loader] Dev-direct experimental world prompt auto-confirmed with skip-backup.");
        } catch (Throwable exception) {
            COMPLETED.set(true);
            System.out.println("[ECHO Native Loader] Dev-direct world prompt auto-confirm failed: "
                    + failureMessage(exception));
        }
    }

    private static void openQuickPlayCurrentScreen(Object minecraft, Object observedScreen) {
        try {
            if (gameplayOpen(minecraft)) {
                COMPLETED.set(true);
                return;
            }
            Object screen = fieldValue(minecraft, "screen");
            if (!Objects.equals(screen, observedScreen) && !quickPlayFallbackCandidate(minecraft, screen)) {
                QUICKPLAY_DISPATCHED.set(false);
                return;
            }
            String saveName = quickPlaySaveName();
            if (!safeSingleSaveName(saveName)) {
                throw new IllegalArgumentException("Unsafe quick-play save name: " + saveName);
            }
            Object flows = methodValue(minecraft, "createWorldOpenFlows");
            if (flows == null) {
                throw new NoSuchMethodException(minecraft.getClass().getName() + ".createWorldOpenFlows");
            }
            Method openWorld = method(flows.getClass(), "openWorld", String.class, Runnable.class);
            if (openWorld == null) {
                throw new NoSuchMethodException(flows.getClass().getName() + ".openWorld(String,Runnable)");
            }
            openWorld.setAccessible(true);
            openWorld.invoke(flows, saveName, (Runnable) () -> setScreenNull(minecraft));
            System.out.println("[ECHO Native Loader] Dev-direct quick-play fallback dispatched existing save '"
                    + saveName + "'.");
        } catch (Throwable exception) {
            COMPLETED.set(true);
            System.out.println("[ECHO Native Loader] Dev-direct quick-play fallback failed: "
                    + failureMessage(exception));
        }
    }

    private static void setScreenNull(Object minecraft) {
        try {
            Method setScreen = methodByNameParameterCount(minecraft.getClass(), "setScreen", 1);
            if (setScreen == null) {
                return;
            }
            setScreen.setAccessible(true);
            setScreen.invoke(minecraft, new Object[]{null});
        } catch (Throwable ignored) {
            // Closing the title screen is best-effort; world opening already owns the important transition.
        }
    }

    private static Object fieldValue(Object target, String name) throws ReflectiveOperationException {
        if (target == null) {
            return null;
        }
        Field field = field(target.getClass(), name);
        if (field == null) {
            throw new NoSuchFieldException(target.getClass().getName() + "." + name);
        }
        field.setAccessible(true);
        return field.get(target);
    }

    private static Object optionalFieldValue(Object target, String name) {
        try {
            return fieldValue(target, name);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Object methodValue(Object target, String name) throws ReflectiveOperationException {
        if (target == null) {
            return null;
        }
        Method method = methodByNameParameterCount(target.getClass(), name, 0);
        if (method == null) {
            return null;
        }
        method.setAccessible(true);
        return method.invoke(target);
    }

    private static Object optionalMethodValue(Object target, String name) {
        try {
            return methodValue(target, name);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Field field(Class<?> type, String name) {
        Class<?> current = type;
        while (current != null) {
            try {
                return current.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    private static Method methodByNameParameterCount(Class<?> type, String name, int parameterCount) {
        Class<?> current = type;
        while (current != null) {
            for (Method method : current.getDeclaredMethods()) {
                if (method.getName().equals(name) && method.getParameterCount() == parameterCount) {
                    return method;
                }
            }
            current = current.getSuperclass();
        }
        return null;
    }

    private static Method method(Class<?> type, String name, Class<?>... parameterTypes) {
        Class<?> current = type;
        while (current != null) {
            try {
                return current.getDeclaredMethod(name, parameterTypes);
            } catch (NoSuchMethodException ignored) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    private static void sleep() {
        try {
            Thread.sleep(POLL_INTERVAL_MILLIS);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            COMPLETED.set(true);
        }
    }

    private static String failureMessage(Throwable exception) {
        Throwable cause = exception.getCause();
        Throwable actual = cause == null ? exception : cause;
        String message = actual.getMessage();
        return actual.getClass().getSimpleName() + (message == null || message.isBlank() ? "" : ": " + message);
    }
}
