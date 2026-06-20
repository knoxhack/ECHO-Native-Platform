package dev.echo.nativeplatform.loader;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.List;

public final class EchoNativeModuleClassLoader extends URLClassLoader {
    static {
        ClassLoader.registerAsParallelCapable();
    }

    public EchoNativeModuleClassLoader(List<Path> classpath, ClassLoader parent) {
        super(urls(classpath), parent);
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(name)) {
            Class<?> loaded = findLoadedClass(name);
            if (loaded == null) {
                loaded = parentFirst(name) ? loadFromParentThenModule(name) : loadFromModuleThenParent(name);
            }
            if (resolve) {
                resolveClass(loaded);
            }
            return loaded;
        }
    }

    private Class<?> loadFromModuleThenParent(String name) throws ClassNotFoundException {
        try {
            return findClass(name);
        } catch (ClassNotFoundException ignored) {
            return loadFromParent(name);
        }
    }

    private Class<?> loadFromParentThenModule(String name) throws ClassNotFoundException {
        try {
            return loadFromParent(name);
        } catch (ClassNotFoundException ignored) {
            return findClass(name);
        }
    }

    private Class<?> loadFromParent(String name) throws ClassNotFoundException {
        ClassLoader parent = getParent();
        return parent == null ? findSystemClass(name) : parent.loadClass(name);
    }

    private static boolean parentFirst(String name) {
        return name.startsWith("java.")
                || name.startsWith("javax.")
                || name.startsWith("jdk.")
                || name.startsWith("sun.")
                || name.startsWith("com.sun.")
                || name.startsWith("dev.echo.nativeplatform.contracts.")
                || name.startsWith("dev.echo.nativeplatform.diagnostics.")
                || name.startsWith("dev.echo.nativeplatform.packos.")
                || name.startsWith("dev.echo.nativeplatform.loader.NativeLoader")
                || name.startsWith("com.echoplatform.echocore.api.")
                || name.startsWith("com.knoxhack.echoscreencore.api.")
                || name.startsWith("com.knoxhack.echoterminal.api.");
    }

    private static URL[] urls(List<Path> classpath) {
        return classpath.stream()
                .map(Path::toUri)
                .map(uri -> {
                    try {
                        return uri.toURL();
                    } catch (Exception exception) {
                        throw new IllegalArgumentException("Invalid native module classpath URI " + uri, exception);
                    }
                })
                .toArray(URL[]::new);
    }
}
