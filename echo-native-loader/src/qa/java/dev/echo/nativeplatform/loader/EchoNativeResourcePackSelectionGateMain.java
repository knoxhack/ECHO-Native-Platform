package dev.echo.nativeplatform.loader;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class EchoNativeResourcePackSelectionGateMain {
    private EchoNativeResourcePackSelectionGateMain() {
    }

    public static void main(String[] args) throws Exception {
        Path tempDir = Files.createTempDirectory("echo-native-resource-pack-selection-");
        Path options = tempDir.resolve("options.txt");
        String packName = "echo-native-ashfall-native-edition-resources.zip";
        Files.write(options, List.of(
                "version:3700",
                "resourcePacks:[\"vanilla\",\"file/older.zip\"]",
                "incompatibleResourcePacks:[\"file/" + packName + "\",\"file/legacy.zip\"]"
        ), StandardCharsets.UTF_8);

        boolean firstEnsureUpdated = NativeLoaderResourcePackSelection.ensureFilePackSelection(options, packName);
        require(firstEnsureUpdated, "Resource pack selection should update options.txt when the pack is absent.");
        String first = Files.readString(options, StandardCharsets.UTF_8);
        require(first.contains("resourcePacks:[\"vanilla\",\"file/older.zip\",\"file/" + packName + "\"]"),
                "Generated file pack must be selected after existing packs.");
        require(first.contains("incompatibleResourcePacks:[\"file/legacy.zip\"]"),
                "Generated file pack must be removed from incompatibleResourcePacks.");

        boolean secondEnsureUpdated = NativeLoaderResourcePackSelection.ensureFilePackSelection(options, packName);
        require(!secondEnsureUpdated, "Resource pack selection should be idempotent once the pack is selected.");
        String second = Files.readString(options, StandardCharsets.UTF_8);
        require(count(second, "file/" + packName) == 1,
                "Generated file pack must not be duplicated in options.txt.");

        boolean removed = NativeLoaderResourcePackSelection.removeFilePackSelection(options, packName);
        require(removed, "Legacy removeFilePackSelection should still remove the selected file pack.");
        String removedOptions = Files.readString(options, StandardCharsets.UTF_8);
        require(!removedOptions.contains("file/" + packName),
                "Legacy removeFilePackSelection left the generated file pack selected.");

        Path missingOptions = tempDir.resolve("missing-options.txt");
        boolean created = NativeLoaderResourcePackSelection.ensureFilePackSelection(missingOptions, packName);
        require(created, "Resource pack selection should create options.txt when it does not exist.");
        require(Files.readString(missingOptions, StandardCharsets.UTF_8)
                        .contains("resourcePacks:[\"file/" + packName + "\"]"),
                "Created options.txt did not select the generated file pack.");

        System.out.println("native resource pack selection gate PASS");
    }

    private static int count(String value, String needle) {
        int matches = 0;
        int index = 0;
        while ((index = value.indexOf(needle, index)) >= 0) {
            matches++;
            index += needle.length();
        }
        return matches;
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }
}
