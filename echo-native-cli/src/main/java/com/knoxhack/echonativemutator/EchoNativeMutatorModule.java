package com.knoxhack.echonativemutator;

import dev.echo.nativeplatform.contracts.EchoNativeLoadStatus;
import dev.echo.nativeplatform.contracts.EchoNativeModuleEntrypoint;
import dev.echo.nativeplatform.contracts.EchoNativeModuleLoadContext;

public final class EchoNativeMutatorModule implements EchoNativeModuleEntrypoint {
    @Override
    public void discover(EchoNativeModuleLoadContext context) {
        context.attribute("echonativemutator.discovered", true);
    }

    @Override
    public void registerServices(EchoNativeModuleLoadContext context) {
        context.registerService(
                "service.echonativemutator.mutator",
                "echonativemutator_mutator_service",
                "core",
                "lifecycle",
                "mutation");
    }

    @Override
    public void registerContent(EchoNativeModuleLoadContext context) {
        context.recordMutation(
                "registry",
                "demo_content_registered",
                "echonativemutator:demo",
                EchoNativeLoadStatus.MUTATED);
    }

    @Override
    public void ready(EchoNativeModuleLoadContext context) {
        context.recordMutation(
                "lifecycle",
                "module_ready",
                "echonativemutator:demo",
                EchoNativeLoadStatus.MUTATED);
    }
}
