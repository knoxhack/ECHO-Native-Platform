package dev.echo.nativeplatform.bootstrap;

import dev.echo.nativeplatform.loader.NativeLoaderJsonSupport;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

record EchoNativeBootstrapArguments(
        String markerPath,
        String packId,
        String realMainClass,
        boolean handoff,
        List<String> modules,
        Map<String, String> nativeEntrypoints,
        List<String> remainingArgs
) {
    static EchoNativeBootstrapArguments parse(String[] args) {
        String markerPath = "";
        String handoffFile = "";
        String packId = "unknown";
        String realMainClass = "";
        boolean handoff = false;
        List<String> modules = new ArrayList<>();
        Map<String, String> nativeEntrypoints = new TreeMap<>();
        List<String> remaining = new ArrayList<>();
        for (int index = 0; index < args.length; index++) {
            String arg = args[index];
            switch (arg) {
                case "--echo-marker" -> markerPath = next(args, ++index, arg);
                case "--echo-handoff-file" -> handoffFile = next(args, ++index, arg);
                case "--echo-pack-id" -> packId = next(args, ++index, arg);
                case "--echo-real-main" -> realMainClass = next(args, ++index, arg);
                case "--echo-module" -> modules.add(next(args, ++index, arg));
                case "--echo-native-entrypoint" -> {
                    String value = next(args, ++index, arg);
                    int separator = value.indexOf('=');
                    if (separator > 0 && separator + 1 < value.length()) {
                        nativeEntrypoints.put(value.substring(0, separator), value.substring(separator + 1));
                    }
                }
                case "--echo-handoff" -> handoff = true;
                default -> remaining.add(arg);
            }
        }
        HandoffData handoffData = HandoffData.read(handoffFile);
        if (packId.equals("unknown") && !handoffData.packId().isBlank()) {
            packId = handoffData.packId();
        }
        if (modules.isEmpty()) {
            modules.addAll(handoffData.modules());
        }
        if (nativeEntrypoints.isEmpty()) {
            nativeEntrypoints.putAll(handoffData.nativeEntrypoints());
        }
        modules = modules.stream().filter(value -> !value.isBlank()).sorted(Comparator.naturalOrder()).toList();
        return new EchoNativeBootstrapArguments(
                markerPath,
                packId,
                realMainClass,
                handoff,
                modules,
                Map.copyOf(nativeEntrypoints),
                List.copyOf(remaining)
        );
    }

    private static String next(String[] args, int index, String flag) {
        if (index >= args.length) {
            throw new IllegalArgumentException(flag + " requires a value");
        }
        return args[index];
    }

    private record HandoffData(String packId, List<String> modules, Map<String, String> nativeEntrypoints) {
        static HandoffData read(String file) {
            if (file == null || file.isBlank()) {
                return empty();
            }
            Path path = Path.of(file).toAbsolutePath().normalize();
            if (!Files.isRegularFile(path)) {
                return empty();
            }
            try {
                Object parsed = NativeLoaderJsonSupport.parse(Files.readString(path, StandardCharsets.UTF_8));
                if (!(parsed instanceof Map<?, ?> object)) {
                    return empty();
                }
                List<String> modules = stringList(object.get("modules"));
                Map<String, String> entrypoints = stringMap(object.get("nativeEntrypoints"));
                return new HandoffData(text(object.get("packId")), modules, entrypoints);
            } catch (RuntimeException | java.io.IOException ignored) {
                return empty();
            }
        }

        private static HandoffData empty() {
            return new HandoffData("", List.of(), Map.of());
        }

        private static List<String> stringList(Object value) {
            if (!(value instanceof Iterable<?> iterable)) {
                return List.of();
            }
            List<String> values = new ArrayList<>();
            for (Object item : iterable) {
                String text = text(item);
                if (!text.isBlank()) {
                    values.add(text);
                }
            }
            return List.copyOf(values);
        }

        private static Map<String, String> stringMap(Object value) {
            if (!(value instanceof Map<?, ?> map)) {
                return Map.of();
            }
            Map<String, String> values = new LinkedHashMap<>();
            map.forEach((key, item) -> {
                String safeKey = text(key);
                String safeValue = text(item);
                if (!safeKey.isBlank() && !safeValue.isBlank()) {
                    values.put(safeKey, safeValue);
                }
            });
            return Map.copyOf(values);
        }

        private static String text(Object value) {
            return value == null ? "" : String.valueOf(value).trim();
        }
    }
}
