package org.slf4j;

public interface Logger {
    default String getName() {
        return "echo-native";
    }

    default boolean isTraceEnabled() {
        return false;
    }

    default boolean isDebugEnabled() {
        return false;
    }

    default boolean isInfoEnabled() {
        return false;
    }

    default boolean isWarnEnabled() {
        return false;
    }

    default boolean isErrorEnabled() {
        return false;
    }

    default boolean isTraceEnabled(Marker marker) {
        return false;
    }

    default boolean isDebugEnabled(Marker marker) {
        return false;
    }

    default boolean isInfoEnabled(Marker marker) {
        return false;
    }

    default boolean isWarnEnabled(Marker marker) {
        return false;
    }

    default boolean isErrorEnabled(Marker marker) {
        return false;
    }

    default void trace(String message) {
    }

    default void trace(String message, Object argument) {
    }

    default void trace(String message, Object first, Object second) {
    }

    default void trace(String message, Object... arguments) {
    }

    default void trace(String message, Throwable throwable) {
    }

    default void trace(Marker marker, String message) {
    }

    default void trace(Marker marker, String message, Object argument) {
    }

    default void trace(Marker marker, String message, Object first, Object second) {
    }

    default void trace(Marker marker, String message, Object... arguments) {
    }

    default void trace(Marker marker, String message, Throwable throwable) {
    }

    default void debug(String message) {
    }

    default void debug(String message, Object argument) {
    }

    default void debug(String message, Object first, Object second) {
    }

    default void debug(String message, Object... arguments) {
    }

    default void debug(String message, Throwable throwable) {
    }

    default void debug(Marker marker, String message) {
    }

    default void debug(Marker marker, String message, Object argument) {
    }

    default void debug(Marker marker, String message, Object first, Object second) {
    }

    default void debug(Marker marker, String message, Object... arguments) {
    }

    default void debug(Marker marker, String message, Throwable throwable) {
    }

    default void info(String message) {
    }

    default void info(String message, Object argument) {
    }

    default void info(String message, Object first, Object second) {
    }

    default void info(String message, Object... arguments) {
    }

    default void info(String message, Throwable throwable) {
    }

    default void info(Marker marker, String message) {
    }

    default void info(Marker marker, String message, Object argument) {
    }

    default void info(Marker marker, String message, Object first, Object second) {
    }

    default void info(Marker marker, String message, Object... arguments) {
    }

    default void info(Marker marker, String message, Throwable throwable) {
    }

    default void warn(String message) {
    }

    default void warn(String message, Object argument) {
    }

    default void warn(String message, Object first, Object second) {
    }

    default void warn(String message, Object... arguments) {
    }

    default void warn(String message, Throwable throwable) {
    }

    default void warn(Marker marker, String message) {
    }

    default void warn(Marker marker, String message, Object argument) {
    }

    default void warn(Marker marker, String message, Object first, Object second) {
    }

    default void warn(Marker marker, String message, Object... arguments) {
    }

    default void warn(Marker marker, String message, Throwable throwable) {
    }

    default void error(String message) {
    }

    default void error(String message, Object argument) {
    }

    default void error(String message, Object first, Object second) {
    }

    default void error(String message, Object... arguments) {
    }

    default void error(String message, Throwable throwable) {
    }

    default void error(Marker marker, String message) {
    }

    default void error(Marker marker, String message, Object argument) {
    }

    default void error(Marker marker, String message, Object first, Object second) {
    }

    default void error(Marker marker, String message, Object... arguments) {
    }

    default void error(Marker marker, String message, Throwable throwable) {
    }
}
