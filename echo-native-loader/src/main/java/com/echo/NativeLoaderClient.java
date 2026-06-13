package com.echo;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Locale;

public final class NativeLoaderClient {
    private static final String MINECRAFT_CLIENT_MAIN = "net.minecraft.client.main.Main";
    private static final String MAIN_CLASS_PROPERTY = "echo.native.minecraftMainClass";
    private static final String MAIN_CLASS_ENV = "ECHO_NATIVE_MINECRAFT_MAIN_CLASS";
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
        System.out.println("[ECHO Native Loader] Starting Minecraft client handoff.");
        System.out.println("[ECHO Native Loader] Target main class: " + mainClassName);
        System.out.println("[ECHO Native Loader] Arguments: " + Arrays.toString(redactedArguments(safeArgs)));
        try {
            Class<?> minecraftMain = Class.forName(mainClassName);
            Method main = minecraftMain.getMethod("main", String[].class);
            main.invoke(null, (Object) safeArgs);
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

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return MINECRAFT_CLIENT_MAIN;
    }
}
