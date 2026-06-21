package dev.echo.nativeplatform.loader;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

public final class EchoNativeSharedApiClassLoaderGateMain {
    private EchoNativeSharedApiClassLoaderGateMain() {
    }

    public static void main(String[] args) throws Exception {
        Path root = Files.createTempDirectory("echo-native-shared-api-classloader-gate");
        Path parentClasses = root.resolve("parent-classes");
        Path moduleClasses = root.resolve("module-classes");
        Files.createDirectories(parentClasses);
        Files.createDirectories(moduleClasses);

        compile(parentClasses, List.of(
                source(root, "parent-core", "com.echoplatform.echocore.api", "SharedProbe", "parent"),
                source(root, "parent-terminal", "com.knoxhack.echoterminal.api", "SharedProbe", "parent"),
                source(root, "parent-screencore", "com.knoxhack.echoscreencore.api", "SharedProbe", "parent"),
                source(root, "parent-slf4j", "org.slf4j", "SharedProbe", "parent"),
                source(root, "parent-mojang-logging", "com.mojang.logging", "SharedProbe", "parent")
        ));
        compile(moduleClasses, List.of(
                source(root, "module-core", "com.echoplatform.echocore.api", "SharedProbe", "module"),
                source(root, "module-terminal", "com.knoxhack.echoterminal.api", "SharedProbe", "module"),
                source(root, "module-screencore", "com.knoxhack.echoscreencore.api", "SharedProbe", "module"),
                source(root, "module-slf4j", "org.slf4j", "SharedProbe", "module"),
                source(root, "module-mojang-logging", "com.mojang.logging", "SharedProbe", "module"),
                source(root, "module-fallback", "com.knoxhack.echoscreencore.api", "FallbackProbe", "module"),
                source(root, "module-terminal-impl", "com.knoxhack.echoterminal.client", "ImplProbe", "module")
        ));

        try (URLClassLoader parent = new URLClassLoader(
                new URL[] { parentClasses.toUri().toURL() },
                EchoNativeSharedApiClassLoaderGateMain.class.getClassLoader());
             EchoNativeModuleClassLoader module = new EchoNativeModuleClassLoader(List.of(moduleClasses), parent)) {
            requireParent(module, "com.echoplatform.echocore.api.SharedProbe");
            requireParent(module, "com.knoxhack.echoterminal.api.SharedProbe");
            requireParent(module, "com.knoxhack.echoscreencore.api.SharedProbe");
            requireParent(module, "org.slf4j.SharedProbe");
            requireParent(module, "com.mojang.logging.SharedProbe");
            requireModule(module, "com.knoxhack.echoscreencore.api.FallbackProbe");
            requireModule(module, "com.knoxhack.echoterminal.client.ImplProbe");
        }

        System.out.println("EchoNativeSharedApiClassLoaderGateMain passed.");
    }

    private static Path source(Path root, String name, String packageName, String className, String origin) throws Exception {
        Path path = root.resolve(name).resolve(packageName.replace('.', '/')).resolve(className + ".java");
        Files.createDirectories(path.getParent());
        Files.writeString(path, """
                package %s;

                public final class %s {
                    public static String origin() {
                        return "%s";
                    }
                }
                """.formatted(packageName, className, origin));
        return path;
    }

    private static void compile(Path output, List<Path> sources) {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new IllegalStateException("A JDK compiler is required for the shared API classloader gate.");
        }
        List<String> args = sources.stream()
                .map(Path::toString)
                .toList();
        String[] compilerArgs = new String[args.size() + 2];
        compilerArgs[0] = "-d";
        compilerArgs[1] = output.toString();
        for (int index = 0; index < args.size(); index++) {
            compilerArgs[index + 2] = args.get(index);
        }
        int exitCode = compiler.run(null, null, null, compilerArgs);
        if (exitCode != 0) {
            throw new IllegalStateException("Probe compilation failed with exit code " + exitCode);
        }
    }

    private static void requireParent(ClassLoader module, String className) throws Exception {
        Class<?> type = Class.forName(className, true, module);
        require(type.getClassLoader() != module, className + " should be loaded by the parent shared API loader.");
        require("parent".equals(type.getMethod("origin").invoke(null)), className + " should resolve to parent origin.");
    }

    private static void requireModule(ClassLoader module, String className) throws Exception {
        Class<?> type = Class.forName(className, true, module);
        require(type.getClassLoader() == module, className + " should be loaded by the module loader.");
        require("module".equals(type.getMethod("origin").invoke(null)), className + " should resolve to module origin.");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }
}
