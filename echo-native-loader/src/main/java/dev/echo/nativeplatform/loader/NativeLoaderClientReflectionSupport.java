package dev.echo.nativeplatform.loader;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public final class NativeLoaderClientReflectionSupport {
    private NativeLoaderClientReflectionSupport() {
    }

    public static Object optionalFieldValue(Object target, String fieldName) throws IllegalAccessException {
        try {
            return target.getClass().getField(fieldName).get(target);
        } catch (NoSuchFieldException exception) {
            try {
                java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
                field.trySetAccessible();
                return field.get(target);
            } catch (NoSuchFieldException ignored) {
                return null;
            }
        }
    }

    public static Object optionalMethodValue(Object target, String methodName) {
        try {
            java.lang.reflect.Method method = target.getClass().getMethod(methodName);
            method.trySetAccessible();
            return method.invoke(target);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            return null;
        }
    }

    public static boolean invokeOnClientThread(
            Class<?> minecraftClass,
            Object minecraft,
            Runnable action
    ) throws ReflectiveOperationException {
        try {
            java.lang.reflect.Method execute = minecraftClass.getMethod("execute", Runnable.class);
            CountDownLatch latch = new CountDownLatch(1);
            RuntimeException[] failure = new RuntimeException[1];
            execute.invoke(minecraft, (Runnable) () -> {
                try {
                    action.run();
                } catch (RuntimeException exception) {
                    failure[0] = exception;
                } finally {
                    latch.countDown();
                }
            });
            try {
                if (!latch.await(5L, TimeUnit.SECONDS)) {
                    return false;
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                return false;
            }
            if (failure[0] != null) {
                Throwable cause = failure[0].getCause();
                if (cause instanceof ReflectiveOperationException reflective) {
                    throw reflective;
                }
                throw failure[0];
            }
            return true;
        } catch (NoSuchMethodException exception) {
            action.run();
            return true;
        }
    }
}
