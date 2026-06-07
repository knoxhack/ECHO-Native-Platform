package dev.echo.nativeplatform.contracts;

@EchoNativeApiStatus(value = EchoNativeApiStability.BETA, since = "0.1.0-native-beta")
public interface EchoNativeAttachmentService extends EchoNativeTypedServiceSupport {
    @Override
    default String serviceId() {
        return "echo.native.attachments";
    }

    default EchoNativeMutationReceipt attach(EchoNativeServiceMutation mutation) {
        return unsupported(mutation);
    }

    default EchoNativeMutationReceipt detach(EchoNativeServiceMutation mutation) {
        return unsupported(mutation);
    }
}
