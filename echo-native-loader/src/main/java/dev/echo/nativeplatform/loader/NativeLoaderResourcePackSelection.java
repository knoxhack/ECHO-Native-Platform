package dev.echo.nativeplatform.loader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class NativeLoaderResourcePackSelection {
    public static final String SERVICE_ID = "echo.native.resource_pack_selection";

    private NativeLoaderResourcePackSelection() {
    }

    public static boolean ensureFilePackSelection(Path optionsPath, String resourcePackFileName) throws IOException {
        String packId = filePackId(resourcePackFileName);
        List<String> lines = Files.isRegularFile(optionsPath)
                ? new ArrayList<>(Files.readAllLines(optionsPath, StandardCharsets.UTF_8))
                : new ArrayList<>();
        boolean changed = false;
        boolean resourcePacksFound = false;

        for (int index = 0; index < lines.size(); index++) {
            String line = lines.get(index);
            if (line.startsWith("resourcePacks:")) {
                resourcePacksFound = true;
                String updated = ensureListEntry(line, "resourcePacks:", packId);
                if (!updated.equals(line)) {
                    lines.set(index, updated);
                    changed = true;
                }
            } else if (line.startsWith("incompatibleResourcePacks:")) {
                String updated = removeListEntry(line, "incompatibleResourcePacks:", packId);
                if (!updated.equals(line)) {
                    lines.set(index, updated);
                    changed = true;
                }
            }
        }

        if (!resourcePacksFound) {
            lines.add("resourcePacks:" + formatList(List.of(packId)));
            changed = true;
        }
        if (changed) {
            Path parent = optionsPath.toAbsolutePath().normalize().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.write(optionsPath, lines, StandardCharsets.UTF_8);
        }
        return changed;
    }

    public static boolean removeFilePackSelection(Path optionsPath, String resourcePackFileName) throws IOException {
        if (!Files.isRegularFile(optionsPath)) {
            return false;
        }
        String packId = filePackId(resourcePackFileName);
        List<String> lines = new ArrayList<>(Files.readAllLines(optionsPath, StandardCharsets.UTF_8));
        boolean changed = false;
        for (int index = 0; index < lines.size(); index++) {
            String line = lines.get(index);
            if (!line.startsWith("resourcePacks:")) {
                continue;
            }
            String updated = removeListEntry(line, "resourcePacks:", packId);
            if (!updated.equals(line)) {
                lines.set(index, updated);
                changed = true;
            }
        }
        if (changed) {
            Files.write(optionsPath, lines, StandardCharsets.UTF_8);
        }
        return changed;
    }

    private static String ensureListEntry(String line, String prefix, String packId) {
        List<String> entries = parseList(line.substring(prefix.length()));
        entries.removeIf(packId::equals);
        entries.add(packId);
        return prefix + formatList(entries);
    }

    private static String removeListEntry(String line, String prefix, String packId) {
        List<String> entries = parseList(line.substring(prefix.length()));
        boolean removed = entries.removeIf(packId::equals);
        if (!removed) {
            return line;
        }
        return prefix + formatList(entries);
    }

    private static String filePackId(String resourcePackFileName) {
        String fileName = resourcePackFileName == null ? "" : resourcePackFileName
                .replace("\\", "")
                .replace("\"", "")
                .trim();
        return "file/" + fileName;
    }

    private static List<String> parseList(String value) {
        String trimmed = value == null ? "" : value.trim();
        if (!trimmed.startsWith("[") || !trimmed.endsWith("]")) {
            return new ArrayList<>();
        }
        List<String> entries = new ArrayList<>();
        int index = 1;
        int end = trimmed.length() - 1;
        while (index < end) {
            while (index < end && (Character.isWhitespace(trimmed.charAt(index)) || trimmed.charAt(index) == ',')) {
                index++;
            }
            if (index >= end) {
                break;
            }
            char current = trimmed.charAt(index);
            if (current == '"') {
                StringBuilder entry = new StringBuilder();
                index++;
                boolean escaping = false;
                while (index < end) {
                    char character = trimmed.charAt(index++);
                    if (escaping) {
                        entry.append(character);
                        escaping = false;
                    } else if (character == '\\') {
                        escaping = true;
                    } else if (character == '"') {
                        break;
                    } else {
                        entry.append(character);
                    }
                }
                entries.add(entry.toString());
            } else {
                int start = index;
                while (index < end && trimmed.charAt(index) != ',') {
                    index++;
                }
                String entry = trimmed.substring(start, index).trim();
                if (!entry.isEmpty()) {
                    entries.add(entry);
                }
            }
        }
        return entries;
    }

    private static String formatList(List<String> entries) {
        StringBuilder value = new StringBuilder("[");
        for (int index = 0; index < entries.size(); index++) {
            if (index > 0) {
                value.append(',');
            }
            value.append('"').append(escape(entries.get(index))).append('"');
        }
        return value.append(']').toString();
    }

    private static String escape(String value) {
        return (value == null ? "" : value)
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }
}
