package dev.echo.nativeplatform.cli;

public final class EchoNativeDummyProcessMain {
    private EchoNativeDummyProcessMain() {
    }

    public static void main(String[] args) {
        String packId = "unknown";
        for (int i = 0; i + 1 < args.length; i++) {
            if ("--pack".equals(args[i])) {
                packId = args[i + 1];
            }
        }
        System.out.println("ECHO_NATIVE_DUMMY_STDOUT:" + packId);
        System.err.println("ECHO_NATIVE_DUMMY_STDERR:" + packId);
    }
}
