package dev.echo.nativeplatform.loader;

public final class NativeLoaderEnvironmentFlow {
    public static final String SERVICE_ID = "echo.native.environment_flow";
    private final String loaderActiveProperty;
    private final String mainLabelProperty;
    private final String clientLabelProperty;
    private final String windowTitleProperty;
    private final String defaultMainLabel;
    private final String defaultClientLabel;
    private final String defaultWindowTitle;

    public NativeLoaderEnvironmentFlow(
            String loaderActiveProperty,
            String mainLabelProperty,
            String clientLabelProperty,
            String windowTitleProperty,
            String defaultMainLabel,
            String defaultClientLabel,
            String defaultWindowTitle
    ) {
        this.loaderActiveProperty = loaderActiveProperty == null ? "" : loaderActiveProperty;
        this.mainLabelProperty = mainLabelProperty == null ? "" : mainLabelProperty;
        this.clientLabelProperty = clientLabelProperty == null ? "" : clientLabelProperty;
        this.windowTitleProperty = windowTitleProperty == null ? "" : windowTitleProperty;
        this.defaultMainLabel = defaultMainLabel == null ? "" : defaultMainLabel;
        this.defaultClientLabel = defaultClientLabel == null ? "" : defaultClientLabel;
        this.defaultWindowTitle = defaultWindowTitle == null ? "" : defaultWindowTitle;
    }

    public boolean nativeLoaderActive() {
        return Boolean.parseBoolean(System.getProperty(loaderActiveProperty, "false"));
    }

    public String nativeLoaderMainLabel() {
        return propertyOrDefault(mainLabelProperty, defaultMainLabel);
    }

    public String nativeLoaderClientLabel() {
        return propertyOrDefault(clientLabelProperty, defaultClientLabel);
    }

    public String nativeLoaderWindowTitle() {
        return propertyOrDefault(windowTitleProperty, defaultWindowTitle);
    }

    private static String propertyOrDefault(String key, String fallback) {
        String value = System.getProperty(key, "").trim();
        return value.isBlank() ? fallback : value;
    }
}
