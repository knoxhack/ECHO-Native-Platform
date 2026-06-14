package com.echo;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Locale;

public final class NativeLoaderClient {
    private static final String MINECRAFT_CLIENT_MAIN = "net.minecraft.client.main.Main";
    private static final String BOOTSTRAP_MAIN_CLASS = "dev.echo.nativeplatform.bootstrap.EchoNativeBootstrapMain";
    private static final String BOOTSTRAP_ENABLED_PROPERTY = "echo.native.bootstrap.enabled";
    private static final String BOOTSTRAP_AUTHORIZED_HANDOFF_PROPERTY = "echo.native.bootstrap.authorizedHandoff";
    private static final String BOOTSTRAP_AUTHORIZED_HANDOFF_VALUE = "startNativeClient";
    private static final String MAIN_CLASS_PROPERTY = "echo.native.minecraftMainClass";
    private static final String MAIN_CLASS_ENV = "ECHO_NATIVE_MINECRAFT_MAIN_CLASS";
    private static final String ECHO_MARKER_ARGUMENT = "--echo-marker";
    private static final String ECHO_REAL_MAIN_ARGUMENT = "--echo-real-main";
    private static final String REDACTED = "<redacted>";
    private static final String[] SENSITIVE_ARGUMENTS = {
            "--accessToken",
            "--clientId",
            "--uuid",
            "--username",
            "--userProperties",
            "--xuid"
    };

    private NativeLoaderClient() {
    }

    public static void main(String[] args) throws Exception {
        String[] safeArgs = args == null ? new String[0] : args;
        String mainClassName = minecraftMainClass();
        if (bootstrapRequested(safeArgs, mainClassName)) {
            invokeBootstrap(safeArgs);
            return;
        }
        invokeMinecraftMain(mainClassName, safeArgs);
    }

    private static void invokeBootstrap(String[] args) throws Exception {
        if (!bootstrapEnabled()) {
            throw new IllegalStateException("Native bootstrap was requested but disabled by -D"
                    + BOOTSTRAP_ENABLED_PROPERTY + "=false.");
        }
        Class<?> bootstrapMain = bootstrapMainClass();
        System.setProperty(BOOTSTRAP_AUTHORIZED_HANDOFF_PROPERTY, BOOTSTRAP_AUTHORIZED_HANDOFF_VALUE);
        System.out.println("[ECHO Native Loader] Starting native bootstrap handoff.");
        System.out.println("[ECHO Native Loader] Bootstrap main class: " + BOOTSTRAP_MAIN_CLASS);
        System.out.println("[ECHO Native Loader] Target main class: " + realMainClass(args));
        System.out.println("[ECHO Native Loader] Arguments: " + Arrays.toString(redactedArguments(args)));
        invokeMain(bootstrapMain, args);
    }

    private static void invokeMinecraftMain(String mainClassName, String[] args) throws Exception {
        System.out.println("[ECHO Native Loader] Starting Minecraft client handoff.");
        System.out.println("[ECHO Native Loader] Target main class: " + mainClassName);
        System.out.println("[ECHO Native Loader] Arguments: " + Arrays.toString(redactedArguments(args)));
        invokeMain(Class.forName(mainClassName), args);
    }

    private static void invokeMain(Class<?> mainClass, String[] args) throws Exception {
        try {
            Method main = mainClass.getMethod("main", String[].class);
            main.invoke(null, (Object) args);
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof Exception checked) {
                throw checked;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw exception;
        }
    }

    private static Class<?> bootstrapMainClass() {
        try {
            return Class.forName(BOOTSTRAP_MAIN_CLASS);
        } catch (ClassNotFoundException exception) {
            throw new IllegalStateException("Native bootstrap was requested, but " + BOOTSTRAP_MAIN_CLASS
                    + " is not present in the Native Loader client library.", exception);
        }
    }

    static String[] redactedArguments(String[] args) {
        String[] redacted = args == null ? new String[0] : args.clone();
        boolean redactNext = false;
        for (int index = 0; index < redacted.length; index++) {
            String value = redacted[index] == null ? "" : redacted[index];
            if (redactNext) {
                redacted[index] = REDACTED;
                redactNext = false;
                continue;
            }
            int equals = value.indexOf('=');
            String argumentName = equals >= 0 ? value.substring(0, equals) : value;
            if (sensitiveArgument(argumentName)) {
                if (equals >= 0) {
                    redacted[index] = value.substring(0, equals + 1) + REDACTED;
                } else {
                    redactNext = true;
                }
            }
        }
        return redacted;
    }

    private static boolean sensitiveArgument(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        for (String argument : SENSITIVE_ARGUMENTS) {
            if (argument.toLowerCase(Locale.ROOT).equals(normalized)) {
                return true;
            }
        }
        return false;
    }

    private static String minecraftMainClass() {
        String configured = firstNonBlank(
                System.getProperty(MAIN_CLASS_PROPERTY),
                System.getenv(MAIN_CLASS_ENV),
                MINECRAFT_CLIENT_MAIN
        );
        if (!configured.matches("[A-Za-z_$][A-Za-z0-9_$]*(\\.[A-Za-z_$][A-Za-z0-9_$]*)*")) {
            throw new IllegalArgumentException("Invalid Minecraft main class: " + configured);
        }
        return configured;
    }

    private static boolean bootstrapRequested(String[] args, String mainClassName) {
        return BOOTSTRAP_MAIN_CLASS.equals(mainClassName) || containsArgument(args, ECHO_MARKER_ARGUMENT);
    }

    private static boolean bootstrapEnabled() {
        String configured = System.getProperty(BOOTSTRAP_ENABLED_PROPERTY, "true").trim();
        return !"false".equalsIgnoreCase(configured);
    }

    private static boolean containsArgument(String[] args, String expected) {
        if (args == null || expected == null) {
            return false;
        }
        for (String arg : args) {
            if (expected.equals(arg)) {
                return true;
            }
        }
        return false;
    }

    private static String realMainClass(String[] args) {
        if (args != null) {
            for (int index = 0; index < args.length - 1; index++) {
                String value = args[index + 1];
                if (ECHO_REAL_MAIN_ARGUMENT.equals(args[index]) && value != null && !value.isBlank()) {
                    return value;
                }
            }
        }
        return MINECRAFT_CLIENT_MAIN;
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return MINECRAFT_CLIENT_MAIN;
    }
}
