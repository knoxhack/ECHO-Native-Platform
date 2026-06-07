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

    public static boolean removeFilePackSelection(Path optionsPath, String resourcePackFileName) throws IOException {
        if (!Files.isRegularFile(optionsPath)) {
            return false;
        }
        String target = "\"file/" + (resourcePackFileName == null ? "" : resourcePackFileName.replace("\"", "")) + "\"";
        List<String> lines = new ArrayList<>(Files.readAllLines(optionsPath, StandardCharsets.UTF_8));
        boolean changed = false;
        for (int index = 0; index < lines.size(); index++) {
            String line = lines.get(index);
            if (!line.startsWith("resourcePacks:") || !line.contains(target)) {
                continue;
            }
            String updated = line
                    .replace("," + target, "")
                    .replace(target + ",", "")
                    .replace(target, "");
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
}
