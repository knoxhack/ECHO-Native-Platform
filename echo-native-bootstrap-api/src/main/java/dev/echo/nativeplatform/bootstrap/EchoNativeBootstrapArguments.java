package dev.echo.nativeplatform.bootstrap;

import java.util.ArrayList;
import java.util.Comparator;
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
}
