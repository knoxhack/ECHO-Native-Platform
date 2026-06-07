package dev.echo.nativeplatform.diagnostics;

import dev.echo.nativeplatform.contracts.EchoNativeDiagnostic;
import dev.echo.nativeplatform.contracts.EchoNativeReportStatus;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoNativeReportWriter {
    private EchoNativeReportWriter() {
    }

    public static void writeReport(
            Path output,
            String schema,
            String generator,
            String packId,
            EchoNativeReportStatus status,
            Map<String, Object> summary,
            List<EchoNativeDiagnostic> issues,
            Map<String, Object> data
    ) throws IOException {
        Files.createDirectories(output.getParent());
        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("schema", schema);
        envelope.put("generatedAt", generatedAt());
        envelope.put("generator", generator);
        envelope.put("packId", packId);
        envelope.put("status", status.name());
        envelope.put("summary", summary);
        envelope.put("issues", issues.stream()
                .sorted(Comparator.comparing(EchoNativeDiagnostic::code).thenComparing(EchoNativeDiagnostic::summary))
                .map(EchoNativeReportWriter::diagnosticMap)
                .toList());
        envelope.put("data", data);
        Files.writeString(output, EchoNativeJson.write(envelope), StandardCharsets.UTF_8);
    }

    public static Map<String, Object> diagnosticMap(EchoNativeDiagnostic diagnostic) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("code", diagnostic.code());
        result.put("severity", diagnostic.severity().name());
        result.put("title", diagnostic.title());
        result.put("summary", diagnostic.summary());
        result.put("moduleId", diagnostic.moduleId());
        result.put("packId", diagnostic.packId());
        result.put("likelyFiles", new ArrayList<>(diagnostic.likelyFiles()));
        result.put("suggestedFix", diagnostic.suggestedFix());
        return result;
    }

    private static String generatedAt() {
        String configured = System.getProperty("echo.native.generatedAt");
        if (configured == null || configured.isBlank()) {
            configured = System.getenv("ECHO_NATIVE_GENERATED_AT");
        }
        return configured == null || configured.isBlank() ? "1970-01-01T00:00:00Z" : configured;
    }
}
