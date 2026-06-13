package dev.echo.nativeplatform.loader;

import com.echo.NativeLoaderClient;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public final class EchoNativeLoaderClientEntrypointGateMain {
    private static boolean fakeMainInvoked;
    private static String[] fakeMainArgs = new String[0];

    private EchoNativeLoaderClientEntrypointGateMain() {
    }

    public static void main(String[] args) throws Exception {
        requireMainContract();
        requireHandoffUsesConfiguredMainAndRedactsSecrets();
        System.out.println("native loader client entrypoint gate PASS");
    }

    private static void requireMainContract() throws NoSuchMethodException {
        Method main = NativeLoaderClient.class.getMethod("main", String[].class);
        int modifiers = main.getModifiers();
        require(Modifier.isPublic(modifiers), "NativeLoaderClient.main must be public.");
        require(Modifier.isStatic(modifiers), "NativeLoaderClient.main must be static.");
        require(main.getReturnType() == Void.TYPE, "NativeLoaderClient.main must return void.");
    }

    private static void requireHandoffUsesConfiguredMainAndRedactsSecrets() throws Exception {
        String[] args = {
                "--accessToken", "secret-token",
                "--username=PlayerName",
                "--uuid", "player-uuid",
                "--demo"
        };
        String previousMainClass = System.getProperty("echo.native.minecraftMainClass");
        PrintStream previousOut = System.out;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        fakeMainInvoked = false;
        fakeMainArgs = new String[0];
        try {
            System.setProperty("echo.native.minecraftMainClass", FakeMinecraftMain.class.getName());
            System.setOut(new PrintStream(output, true, StandardCharsets.UTF_8));
            NativeLoaderClient.main(args);
        } finally {
            System.setOut(previousOut);
            if (previousMainClass == null) {
                System.clearProperty("echo.native.minecraftMainClass");
            } else {
                System.setProperty("echo.native.minecraftMainClass", previousMainClass);
            }
        }

        String log = output.toString(StandardCharsets.UTF_8);
        require(fakeMainInvoked, "NativeLoaderClient must invoke the configured Minecraft main class.");
        require(Arrays.equals(args, fakeMainArgs), "NativeLoaderClient must pass original Minecraft args unchanged.");
        require(!log.contains("secret-token"), "NativeLoaderClient logs must redact access tokens.");
        require(!log.contains("PlayerName"), "NativeLoaderClient logs must redact usernames.");
        require(!log.contains("player-uuid"), "NativeLoaderClient logs must redact UUIDs.");
        require(log.contains("<redacted>"), "NativeLoaderClient logs must show redacted placeholders.");
    }

    public static final class FakeMinecraftMain {
        private FakeMinecraftMain() {
        }

        public static void main(String[] args) {
            fakeMainInvoked = true;
            fakeMainArgs = args == null ? new String[0] : args.clone();
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }
}
