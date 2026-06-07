package dev.echo.nativeplatform.diagnostics;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class EchoNativeJson {
    private EchoNativeJson() {
    }

    public static Object parse(String text) {
        return new Parser(text).parse();
    }

    public static String write(Object value) {
        StringBuilder builder = new StringBuilder();
        writeValue(builder, value, 0);
        builder.append('\n');
        return builder.toString();
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> asObject(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> result = new LinkedHashMap<>();
            map.forEach((key, mapValue) -> result.put(String.valueOf(key), mapValue));
            return result;
        }
        return Map.of();
    }

    public static List<String> stringList(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (Object item : list) {
            if (item != null) {
                result.add(String.valueOf(item));
            }
        }
        result.sort(String::compareTo);
        return List.copyOf(result);
    }

    private static void writeValue(StringBuilder builder, Object value, int indent) {
        if (value == null) {
            builder.append("null");
        } else if (value instanceof String string) {
            builder.append('"').append(escape(string)).append('"');
        } else if (value instanceof Number || value instanceof Boolean) {
            builder.append(value);
        } else if (value instanceof Map<?, ?> map) {
            writeObject(builder, map, indent);
        } else if (value instanceof Iterable<?> iterable) {
            writeArray(builder, iterable, indent);
        } else if (value instanceof Enum<?> enumValue) {
            builder.append('"').append(enumValue.name()).append('"');
        } else {
            builder.append('"').append(escape(String.valueOf(value))).append('"');
        }
    }

    private static void writeObject(StringBuilder builder, Map<?, ?> map, int indent) {
        Map<String, Object> sorted = new TreeMap<>();
        map.forEach((key, value) -> sorted.put(String.valueOf(key), value));
        builder.append('{');
        if (!sorted.isEmpty()) {
            int index = 0;
            for (Map.Entry<String, Object> entry : sorted.entrySet()) {
                builder.append('\n').append("  ".repeat(indent + 1));
                builder.append('"').append(escape(entry.getKey())).append("\": ");
                writeValue(builder, entry.getValue(), indent + 1);
                if (++index < sorted.size()) {
                    builder.append(',');
                }
            }
            builder.append('\n').append("  ".repeat(indent));
        }
        builder.append('}');
    }

    private static void writeArray(StringBuilder builder, Iterable<?> iterable, int indent) {
        List<Object> items = new ArrayList<>();
        iterable.forEach(items::add);
        builder.append('[');
        if (!items.isEmpty()) {
            for (int i = 0; i < items.size(); i++) {
                builder.append('\n').append("  ".repeat(indent + 1));
                writeValue(builder, items.get(i), indent + 1);
                if (i + 1 < items.size()) {
                    builder.append(',');
                }
            }
            builder.append('\n').append("  ".repeat(indent));
        }
        builder.append(']');
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private static final class Parser {
        private final String text;
        private int index;

        private Parser(String text) {
            this.text = normalize(text);
        }

        Object parse() {
            Object value = readValue();
            skipWhitespace();
            if (index != text.length()) {
                throw error("Unexpected trailing content");
            }
            return value;
        }

        private Object readValue() {
            skipWhitespace();
            if (index >= text.length()) {
                throw error("Unexpected end of JSON");
            }
            char current = text.charAt(index);
            return switch (current) {
                case '{' -> readObject();
                case '[' -> readArray();
                case '"' -> readString();
                case 't' -> readLiteral("true", Boolean.TRUE);
                case 'f' -> readLiteral("false", Boolean.FALSE);
                case 'n' -> readLiteral("null", null);
                default -> readNumber();
            };
        }

        private Map<String, Object> readObject() {
            expect('{');
            Map<String, Object> result = new LinkedHashMap<>();
            skipWhitespace();
            if (peek('}')) {
                expect('}');
                return result;
            }
            while (true) {
                String key = readString();
                skipWhitespace();
                expect(':');
                result.put(key, readValue());
                skipWhitespace();
                if (peek('}')) {
                    expect('}');
                    return result;
                }
                expect(',');
                skipWhitespace();
            }
        }

        private List<Object> readArray() {
            expect('[');
            List<Object> result = new ArrayList<>();
            skipWhitespace();
            if (peek(']')) {
                expect(']');
                return result;
            }
            while (true) {
                result.add(readValue());
                skipWhitespace();
                if (peek(']')) {
                    expect(']');
                    return result;
                }
                expect(',');
            }
        }

        private String readString() {
            expect('"');
            StringBuilder builder = new StringBuilder();
            while (index < text.length()) {
                char current = text.charAt(index++);
                if (current == '"') {
                    return builder.toString();
                }
                if (current == '\\') {
                    if (index >= text.length()) {
                        throw error("Invalid escape");
                    }
                    char escaped = text.charAt(index++);
                    switch (escaped) {
                        case '"' -> builder.append('"');
                        case '\\' -> builder.append('\\');
                        case '/' -> builder.append('/');
                        case 'b' -> builder.append('\b');
                        case 'f' -> builder.append('\f');
                        case 'n' -> builder.append('\n');
                        case 'r' -> builder.append('\r');
                        case 't' -> builder.append('\t');
                        case 'u' -> {
                            if (index + 4 > text.length()) {
                                throw error("Invalid unicode escape");
                            }
                            builder.append((char) Integer.parseInt(text.substring(index, index + 4), 16));
                            index += 4;
                        }
                        default -> throw error("Unknown escape");
                    }
                } else {
                    builder.append(current);
                }
            }
            throw error("Unterminated string");
        }

        private Object readNumber() {
            int start = index;
            while (index < text.length()) {
                char current = text.charAt(index);
                if ((current >= '0' && current <= '9') || current == '-' || current == '+' || current == '.' || current == 'e' || current == 'E') {
                    index++;
                } else {
                    break;
                }
            }
            if (start == index) {
                throw error("Expected value");
            }
            String raw = text.substring(start, index);
            if (raw.contains(".") || raw.contains("e") || raw.contains("E")) {
                return Double.parseDouble(raw);
            }
            return Long.parseLong(raw);
        }

        private Object readLiteral(String literal, Object value) {
            if (!text.startsWith(literal, index)) {
                throw error("Expected " + literal);
            }
            index += literal.length();
            return value;
        }

        private void expect(char expected) {
            skipWhitespace();
            if (index >= text.length() || text.charAt(index) != expected) {
                throw error("Expected '" + expected + "'");
            }
            index++;
        }

        private boolean peek(char expected) {
            skipWhitespace();
            return index < text.length() && text.charAt(index) == expected;
        }

        private void skipWhitespace() {
            while (index < text.length()
                    && (Character.isWhitespace(text.charAt(index)) || text.charAt(index) == '\uFEFF')) {
                index++;
            }
        }

        private IllegalArgumentException error(String message) {
            return new IllegalArgumentException(message + " at offset " + index);
        }

        private static String normalize(String text) {
            if (text == null || text.isEmpty()) {
                return "";
            }
            return text.charAt(0) == '\uFEFF' ? text.substring(1) : text;
        }
    }
}
