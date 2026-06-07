package dev.echo.nativeplatform.loader;

import java.util.Map;

public final class NativeLoaderRuntimeReflectionSupport {
    private NativeLoaderRuntimeReflectionSupport() {
    }

    public static boolean invokeStaticNoArg(Map<String, Object> report, String key, String className, String methodName) {
        return invokeStaticNoArgValue(report, key, className, methodName) != null || Boolean.TRUE.equals(report.get(key));
    }

    public static Object invokeStaticNoArgValue(Map<String, Object> report, String key, String className, String methodName) {
        try {
            Class<?> type = Class.forName(className);
            java.lang.reflect.Method method = type.getDeclaredMethod(methodName);
            method.trySetAccessible();
            Object value = method.invoke(null);
            report.put(key, !Boolean.FALSE.equals(value));
            report.put(key + "Value", valueSummary(value));
            return value;
        } catch (Throwable exception) {
            report.put(key, false);
            report.put(key + "Failure", failureMessage(exception));
            return null;
        }
    }

    public static Object invokeStaticOneArgValue(
            Map<String, Object> report,
            String key,
            String className,
            String methodName,
            Object arg
    ) {
        if (arg == null) {
            report.put(key, false);
            report.put(key + "Failure", "No argument instance");
            return null;
        }
        try {
            Class<?> type = Class.forName(className);
            for (java.lang.reflect.Method method : type.getMethods()) {
                if (!method.getName().equals(methodName)
                        || method.getParameterCount() != 1
                        || !java.lang.reflect.Modifier.isStatic(method.getModifiers())) {
                    continue;
                }
                Class<?> parameter = method.getParameterTypes()[0];
                if (!wrapPrimitive(parameter).isInstance(arg)) {
                    continue;
                }
                method.trySetAccessible();
                Object value = method.invoke(null, arg);
                report.put(key, !Boolean.FALSE.equals(value));
                report.put(key + "Value", valueSummary(value));
                return value;
            }
            report.put(key, false);
            report.put(key + "Failure", "No compatible static one-argument method");
            return null;
        } catch (Throwable exception) {
            report.put(key, false);
            report.put(key + "Failure", failureMessage(exception));
            return null;
        }
    }

    public static Object invokeStaticMethodValue(
            Map<String, Object> report,
            String key,
            String className,
            String methodName,
            Class<?>[] parameterTypes,
            Object... args
    ) {
        try {
            Class<?> type = Class.forName(className);
            java.lang.reflect.Method method = type.getDeclaredMethod(methodName, safeParameterTypes(parameterTypes));
            method.trySetAccessible();
            Object value = method.invoke(null, args);
            report.put(key, !Boolean.FALSE.equals(value));
            report.put(key + "Value", valueSummary(value));
            return value;
        } catch (Throwable exception) {
            report.put(key, false);
            report.put(key + "Failure", failureMessage(exception));
            return null;
        }
    }

    public static Object invokeMethodValue(
            Map<String, Object> report,
            String key,
            Object target,
            String methodName,
            Class<?>[] parameterTypes,
            Object... args
    ) {
        if (target == null) {
            report.put(key, false);
            report.put(key + "Failure", "No target instance");
            return null;
        }
        try {
            java.lang.reflect.Method method = target.getClass().getMethod(methodName, safeParameterTypes(parameterTypes));
            method.trySetAccessible();
            Object value = method.invoke(target, args);
            report.put(key, !Boolean.FALSE.equals(value));
            report.put(key + "Value", valueSummary(value));
            return value;
        } catch (Throwable exception) {
            report.put(key, false);
            report.put(key + "Failure", failureMessage(exception));
            return null;
        }
    }

    public static Object staticFieldValue(String className, String fieldName) {
        try {
            java.lang.reflect.Field field = Class.forName(className).getField(fieldName);
            field.trySetAccessible();
            return field.get(null);
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static Class<?> tryClass(String className) {
        if (className == null || className.isBlank()) {
            return null;
        }
        try {
            return Class.forName(className);
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static Object valueSummary(Object value) {
        if (value == null) {
            return "";
        }
        int size = sizeOf(value);
        if (size >= 0) {
            return value.getClass().getSimpleName() + "[" + size + "]";
        }
        if (value instanceof Boolean || value instanceof Number || value instanceof CharSequence) {
            return value;
        }
        return value.getClass().getName();
    }

    public static int sizeOf(Object value) {
        if (value instanceof java.util.Collection<?> collection) {
            return collection.size();
        }
        if (value instanceof Map<?, ?> map) {
            return map.size();
        }
        if (value instanceof Iterable<?> iterable) {
            int size = 0;
            for (Object ignored : iterable) {
                size++;
            }
            return size;
        }
        return -1;
    }

    public static int successfulCalls(Map<String, Object> report) {
        int count = 0;
        for (Map.Entry<String, Object> entry : report.entrySet()) {
            if (!entry.getKey().endsWith("Value") && Boolean.TRUE.equals(entry.getValue())) {
                count++;
            }
        }
        return count;
    }

    private static Class<?>[] safeParameterTypes(Class<?>[] parameterTypes) {
        if (parameterTypes == null || parameterTypes.length == 0) {
            return new Class<?>[0];
        }
        for (Class<?> parameterType : parameterTypes) {
            if (parameterType == null) {
                throw new IllegalArgumentException("Missing runtime parameter class");
            }
        }
        return parameterTypes;
    }

    private static Class<?> wrapPrimitive(Class<?> type) {
        if (type == null || !type.isPrimitive()) {
            return type;
        }
        if (type == boolean.class) {
            return Boolean.class;
        }
        if (type == byte.class) {
            return Byte.class;
        }
        if (type == short.class) {
            return Short.class;
        }
        if (type == int.class) {
            return Integer.class;
        }
        if (type == long.class) {
            return Long.class;
        }
        if (type == float.class) {
            return Float.class;
        }
        if (type == double.class) {
            return Double.class;
        }
        if (type == char.class) {
            return Character.class;
        }
        return Void.class;
    }

    private static String failureMessage(Throwable exception) {
        String message = exception.getMessage();
        Throwable cause = exception.getCause();
        if ((message == null || message.isBlank()) && cause != null) {
            message = cause.getClass().getSimpleName() + ": " + cause.getMessage();
        }
        return message == null || message.isBlank() ? exception.getClass().getSimpleName() : message;
    }
}
