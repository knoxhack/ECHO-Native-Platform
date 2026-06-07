package dev.echo.nativeplatform.cli;

import dev.echo.nativeplatform.contracts.EchoNativeDiagnostic;
import dev.echo.nativeplatform.contracts.EchoNativeIssueSeverity;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

final class EchoNativeStaticSafetyScanner {
    private static final List<String> FORBIDDEN_PATTERNS = List.of(
            "import net." + "neoforged",
            "import net." + "minecraftforge",
            "import net." + "fabricmc",
            "import net." + "minecraft",
            "implementation \"net." + "neoforged",
            "implementation 'net." + "neoforged",
            "implementation \"net." + "minecraftforge",
            "implementation 'net." + "minecraftforge",
            "implementation \"net." + "fabricmc",
            "implementation 'net." + "fabricmc",
            "implementation \"net." + "minecraft",
            "implementation 'net." + "minecraft"
    );

    EchoNativeStaticSafetyScan scan() throws IOException {
        Path workspace = Path.of("").toAbsolutePath().normalize();
        List<Map<String, Object>> matches = new ArrayList<>();
        long checkedFiles = 0;
        try (Stream<Path> stream = Files.walk(workspace)) {
            List<Path> files = stream
                    .filter(Files::isRegularFile)
                    .filter(this::isSourceLike)
                    .filter(this::isAllowedPath)
                    .sorted()
                    .toList();
            checkedFiles = files.size();
            for (Path file : files) {
                List<String> lines = Files.readAllLines(file);
                boolean inTextBlock = false;
                for (int i = 0; i < lines.size(); i++) {
                    String line = lines.get(i);
                    boolean scanLine = !inTextBlock;
                    if (scanLine) {
                        for (String pattern : FORBIDDEN_PATTERNS) {
                            if (line.contains(pattern)) {
                                Map<String, Object> match = new LinkedHashMap<>();
                                match.put("line", i + 1);
                                match.put("path", relative(workspace, file));
                                match.put("pattern", pattern);
                                matches.add(match);
                            }
                        }
                    }
                    if (textBlockDelimiterCount(line) % 2 == 1) {
                        inTextBlock = !inTextBlock;
                    }
                }
            }
        }

        matches.sort(Comparator.comparing(match -> match.get("path") + ":" + match.get("line") + ":" + match.get("pattern")));
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("checkedFileCount", checkedFiles);
        data.put("forbiddenPatterns", FORBIDDEN_PATTERNS);
        data.put("matchCount", matches.size());
        data.put("matches", matches);
        data.put("status", matches.isEmpty() ? "PASS" : "FAILED");

        List<EchoNativeDiagnostic> diagnostics = matches.stream()
                .map(match -> new EchoNativeDiagnostic(
                        "ECHO-NATIVE-FORBIDDEN-IMPORT",
                        EchoNativeIssueSeverity.ERROR,
                        "Forbidden native dependency reference",
                        "Forbidden pattern '" + match.get("pattern") + "' found in native workspace source.",
                        null,
                        "",
                        List.of(String.valueOf(match.get("path"))),
                        "Remove NeoForge, Forge, Fabric, or Minecraft runtime dependencies from Phase 12 native modules."
                ))
                .toList();
        return new EchoNativeStaticSafetyScan(data, diagnostics);
    }

    private boolean isSourceLike(Path path) {
        String name = path.getFileName().toString();
        return name.endsWith(".java") || name.endsWith(".gradle") || name.endsWith(".properties");
    }

    private boolean isAllowedPath(Path path) {
        String normalized = path.toString().replace('\\', '/');
        return !normalized.contains("/.gradle/")
                && !normalized.contains("/build/")
                && !normalized.contains("/gradle/wrapper/");
    }

    private static int textBlockDelimiterCount(String line) {
        int count = 0;
        int index = line.indexOf("\"\"\"");
        while (index >= 0) {
            count++;
            index = line.indexOf("\"\"\"", index + 3);
        }
        return count;
    }

    private static String relative(Path workspace, Path path) {
        return workspace.relativize(path.toAbsolutePath().normalize()).toString().replace('\\', '/');
    }
}
