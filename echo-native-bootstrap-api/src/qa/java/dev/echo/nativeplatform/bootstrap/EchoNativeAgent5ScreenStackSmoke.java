package dev.echo.nativeplatform.bootstrap;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoNativeAgent5ScreenStackSmoke {
    private EchoNativeAgent5ScreenStackSmoke() {
    }

    public static Map<String, Object> capture(
            String screenClass,
            String packId,
            int moduleCount,
            int itemCount,
            int missionCount,
            int regionCount
    ) {
        ArrayList<String> stack = new ArrayList<>();
        ArrayList<String> events = new ArrayList<>();
        ArrayList<String> currentModes = new ArrayList<>();
        ArrayList<Integer> stackSizes = new ArrayList<>();
        ArrayList<String> routeFocusPaths = new ArrayList<>();
        ArrayList<String> screenTitles = new ArrayList<>();
        ArrayList<Boolean> hostExecutions = new ArrayList<>();

        push(stack, "MAIN_MENU", events, currentModes, stackSizes);
        renderCurrent(stack, "WIKI", packId, moduleCount, itemCount, missionCount, regionCount,
                screenTitles, hostExecutions);

        pushRoute(stack, "M", events, currentModes, stackSizes, routeFocusPaths);
        renderCurrent(stack, "WIKI", packId, moduleCount, itemCount, missionCount, regionCount,
                screenTitles, hostExecutions);
        pushRoute(stack, "G", events, currentModes, stackSizes, routeFocusPaths);
        pushRoute(stack, "LEFT_ALT", events, currentModes, stackSizes, routeFocusPaths);

        Map<String, Object> pauseRoute = EchoNativeAgent5UiActionRouter.routeKey("ESCAPE", current(stack), "WIKI");
        push(stack, String.valueOf(pauseRoute.get("destinationMode")), events, currentModes, stackSizes);
        routeFocusPaths.add(EchoNativeAgent5UiActionRouter.focusPath(current(stack), "LENS"));
        renderCurrent(stack, "LENS", packId, moduleCount, itemCount, missionCount, regionCount,
                screenTitles, hostExecutions);

        String poppedPause = pop(stack, events, currentModes, stackSizes);
        String resumeMode = current(stack);
        replace(stack, "SETTINGS", events, currentModes, stackSizes);
        renderCurrent(stack, "WIKI", packId, moduleCount, itemCount, missionCount, regionCount,
                screenTitles, hostExecutions);
        replace(stack, "LENS", events, currentModes, stackSizes);

        pushSystemSurface(stack, "RECOVERY", events, currentModes, stackSizes, routeFocusPaths);
        renderCurrent(stack, "WIKI", packId, moduleCount, itemCount, missionCount, regionCount,
                screenTitles, hostExecutions);
        String poppedRecovery = pop(stack, events, currentModes, stackSizes);

        pushSystemSurface(stack, "MAIN_MENU", events, currentModes, stackSizes, routeFocusPaths);
        renderCurrent(stack, "WIKI", packId, moduleCount, itemCount, missionCount, regionCount,
                screenTitles, hostExecutions);

        while (!stack.isEmpty()) {
            pop(stack, events, currentModes, stackSizes);
        }
        boolean emptyPopSafe = pop(stack, events, currentModes, stackSizes).isBlank();
        push(stack, "MAIN_MENU", events, currentModes, stackSizes);

        boolean passed = events.containsAll(List.of(
                        "push:MAIN_MENU",
                        "push:TERMINAL",
                        "push:INDEX",
                        "push:LENS",
                        "push:PAUSE",
                        "pop:PAUSE",
                        "replace:SETTINGS",
                        "replace:LENS",
                        "push:RECOVERY",
                        "pop:RECOVERY",
                        "push:MAIN_MENU",
                        "empty-pop"
                ))
                && "PAUSE".equals(poppedPause)
                && "LENS".equals(resumeMode)
                && "RECOVERY".equals(poppedRecovery)
                && emptyPopSafe
                && currentModes.containsAll(List.of("MAIN_MENU", "TERMINAL", "INDEX", "LENS", "PAUSE", "SETTINGS", "RECOVERY"))
                && routeFocusPaths.containsAll(List.of("terminal:input", "index:search", "lens:scan", "pause:resume:LENS", "recovery:recover"))
                && screenTitles.containsAll(List.of("ECHO NATIVE // MAIN_MENU", "ECHO NATIVE // PAUSE", "ECHO NATIVE // RECOVERY"))
                && hostExecutions.stream().allMatch(Boolean.TRUE::equals)
                && stack.size() == 1
                && "MAIN_MENU".equals(current(stack));

        Map<String, Object> smoke = new LinkedHashMap<>();
        smoke.put("screenStackSmokeClass", EchoNativeAgent5ScreenStackSmoke.class.getSimpleName());
        smoke.put("screenClass", screenClass);
        smoke.put("events", List.copyOf(events));
        smoke.put("currentModes", List.copyOf(currentModes));
        smoke.put("stackSizes", List.copyOf(stackSizes));
        smoke.put("routeFocusPaths", List.copyOf(routeFocusPaths));
        smoke.put("screenTitles", List.copyOf(screenTitles));
        smoke.put("resumeMode", resumeMode);
        smoke.put("emptyPopSafe", emptyPopSafe);
        smoke.put("finalCurrentMode", current(stack));
        smoke.put("finalStackSize", stack.size());
        smoke.put("adapterCoreBridge", true);
        smoke.put("serviceCodeExecuted", true);
        smoke.put("passed", passed);
        return Map.copyOf(smoke);
    }

    private static void pushRoute(
            List<String> stack,
            String key,
            List<String> events,
            List<String> currentModes,
            List<Integer> stackSizes,
            List<String> routeFocusPaths
    ) {
        Map<String, Object> route = EchoNativeAgent5UiActionRouter.routeKey(key, current(stack), "WIKI");
        String destination = String.valueOf(route.get("destinationMode"));
        push(stack, destination, events, currentModes, stackSizes);
        routeFocusPaths.add(EchoNativeAgent5UiActionRouter.focusPath(destination, "WIKI"));
    }

    private static void pushSystemSurface(
            List<String> stack,
            String destination,
            List<String> events,
            List<String> currentModes,
            List<Integer> stackSizes,
            List<String> routeFocusPaths
    ) {
        push(stack, destination, events, currentModes, stackSizes);
        routeFocusPaths.add(EchoNativeAgent5UiActionRouter.focusPath(destination, "WIKI"));
    }

    private static void renderCurrent(
            List<String> stack,
            String previousMode,
            String packId,
            int moduleCount,
            int itemCount,
            int missionCount,
            int regionCount,
            List<String> screenTitles,
            List<Boolean> hostExecutions
    ) {
        Map<String, Object> hostModel = EchoNativeAgent5ScreenHostModel.render(
                current(stack),
                Map.of(
                        "previousMode", previousMode,
                        "focusedControl", EchoNativeAgent5UiActionRouter.focusPath(current(stack), previousMode),
                        "terminalBuffer", "status",
                        "indexBuffer", "ashfall",
                        "mouseRouted", true
                ),
                packId,
                moduleCount,
                itemCount,
                missionCount,
                regionCount
        );
        screenTitles.add(String.valueOf(hostModel.get("screenTitle")));
        hostExecutions.add(Boolean.TRUE.equals(hostModel.get("serviceCodeExecuted")));
    }

    private static void push(List<String> stack, String mode, List<String> events, List<String> currentModes, List<Integer> stackSizes) {
        stack.add(mode);
        events.add("push:" + mode);
        currentModes.add(current(stack));
        stackSizes.add(stack.size());
    }

    private static String pop(List<String> stack, List<String> events, List<String> currentModes, List<Integer> stackSizes) {
        if (stack.isEmpty()) {
            events.add("empty-pop");
            currentModes.add("");
            stackSizes.add(0);
            return "";
        }
        String removed = stack.removeLast();
        events.add("pop:" + removed);
        currentModes.add(current(stack));
        stackSizes.add(stack.size());
        return removed;
    }

    private static void replace(List<String> stack, String mode, List<String> events, List<String> currentModes, List<Integer> stackSizes) {
        if (!stack.isEmpty()) {
            stack.removeLast();
        }
        stack.add(mode);
        events.add("replace:" + mode);
        currentModes.add(current(stack));
        stackSizes.add(stack.size());
    }

    private static String current(List<String> stack) {
        return stack.isEmpty() ? "" : stack.getLast();
    }
}
