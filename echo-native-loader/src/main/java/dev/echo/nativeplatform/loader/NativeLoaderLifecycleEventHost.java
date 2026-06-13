package dev.echo.nativeplatform.loader;

import dev.echo.nativeplatform.contracts.EchoNativeLifecycleRecord;
import dev.echo.nativeplatform.contracts.EchoNativeLoadStatus;
import dev.echo.nativeplatform.contracts.EchoNativeModuleLoadResult;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class NativeLoaderLifecycleEventHost {
    public static final String LIFECYCLE_SERVICE_ID = "echo_native.lifecycle_host";
    public static final String EVENT_SERVICE_ID = "echo_native.event_host";

    private final List<LifecycleEvent> lifecycleEvents = new ArrayList<>();
    private final List<PublishedEvent> publishedEvents = new ArrayList<>();
    private final Map<String, List<EventSubscription>> subscriptionsByEventId = new LinkedHashMap<>();
    private final Map<String, EventSubscription> subscriptionsByKey = new LinkedHashMap<>();
    private final NativeLoaderLiveRuntimeBridge liveRuntimeBridge;
    private int liveRuntimeDispatchCount = 0;
    private int liveRuntimeMutationCount = 0;
    private long liveRuntimeDispatchSequence = 0L;

    public NativeLoaderLifecycleEventHost() {
        this(NativeLoaderLiveRuntimeBridge.UNATTACHED);
    }

    public NativeLoaderLifecycleEventHost(NativeLoaderLiveRuntimeBridge liveRuntimeBridge) {
        this.liveRuntimeBridge = liveRuntimeBridge == null
                ? NativeLoaderLiveRuntimeBridge.UNATTACHED
                : liveRuntimeBridge;
    }

    public void recordModuleLoad(EchoNativeModuleLoadResult result) {
        if (result == null) {
            return;
        }
        String moduleId = result.descriptor().id();
        for (EchoNativeLifecycleRecord record : result.lifecyclePhaseHistory()) {
            recordLifecycle(moduleId, record);
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("moduleId", moduleId);
        payload.put("status", result.status().name());
        payload.put("loaded", result.loaded());
        payload.put("registered", result.registered());
        payload.put("mutated", result.mutated());
        payload.put("registeredServiceCount", result.registeredServices().size());
        publish("echocore", "echo_native.module_load_completed", payload, result.status());
    }

    public LifecycleEvent recordLifecycle(String moduleId, EchoNativeLifecycleRecord record) {
        if (record == null) {
            throw new IllegalArgumentException("record is required");
        }
        Map<String, Object> evidence = new LinkedHashMap<>();
        String phaseId = record.phase().name();
        clearLiveDispatchProof(evidence);
        String liveRuntimeDispatchId = beginLiveRuntimeDispatch(
                LIFECYCLE_SERVICE_ID,
                "lifecycle_phases",
                value(moduleId) + ":" + phaseId,
                evidence
        );
        EchoNativeLoadStatus liveStatus = dispatchLifecycle(moduleId, phaseId, evidence);
        boolean liveDispatchProofSatisfied = liveDispatchProofSatisfied(
                liveStatus,
                evidence,
                liveRuntimeDispatchId,
                "lifecycle_phases"
        );
        if (liveDispatchProofSatisfied) {
            liveRuntimeMutationCount++;
        }
        Map<String, Object> enrichedEvidence = new LinkedHashMap<>(evidence);
        enrichedEvidence.put("liveRuntimeBridgeStatus", liveStatus.name());
        enrichedEvidence.put("subsystemLiveRuntimeDispatchProofSatisfied", liveDispatchProofSatisfied);
        enrichedEvidence.put("liveRuntimeAccessed", liveRuntimeBridge.liveRuntimeAccessed());
        enrichedEvidence.put("minecraftRuntimeAccessed", liveDispatchProofSatisfied);
        enrichedEvidence.put("liveMinecraftMutation", liveDispatchProofSatisfied);
        LifecycleEvent event = new LifecycleEvent(
                lifecycleEvents.size() + 1,
                value(moduleId),
                phaseId,
                record.status().name(),
                record.detail(),
                record.failed(),
                record.failures() == null ? List.of() : List.copyOf(record.failures()),
                Map.copyOf(enrichedEvidence)
        );
        lifecycleEvents.add(event);
        return event;
    }

    public LifecycleEvent recordDeclaredLifecyclePhase(String moduleId, String phaseId, Map<String, Object> evidence) {
        if (phaseId == null || phaseId.isBlank()) {
            throw new IllegalArgumentException("phaseId is required");
        }
        Map<String, Object> safeEvidence = new LinkedHashMap<>(evidence == null ? Map.of() : evidence);
        clearLiveDispatchProof(safeEvidence);
        String liveRuntimeDispatchId = beginLiveRuntimeDispatch(
                LIFECYCLE_SERVICE_ID,
                "lifecycle_phases",
                value(moduleId) + ":" + phaseId.trim(),
                safeEvidence
        );
        EchoNativeLoadStatus liveStatus = dispatchLifecycle(moduleId, phaseId, safeEvidence);
        boolean liveDispatchProofSatisfied = liveDispatchProofSatisfied(liveStatus, safeEvidence, liveRuntimeDispatchId, "lifecycle_phases");
        if (liveDispatchProofSatisfied) {
            liveRuntimeMutationCount++;
        }
        Map<String, Object> enrichedEvidence = new LinkedHashMap<>(safeEvidence);
        enrichedEvidence.put("liveRuntimeBridgeStatus", liveStatus.name());
        enrichedEvidence.put("subsystemLiveRuntimeDispatchProofSatisfied", liveDispatchProofSatisfied);
        enrichedEvidence.put("liveRuntimeAccessed", liveRuntimeBridge.liveRuntimeAccessed());
        enrichedEvidence.put("minecraftRuntimeAccessed", liveDispatchProofSatisfied);
        enrichedEvidence.put("liveMinecraftMutation", liveDispatchProofSatisfied);
        LifecycleEvent event = new LifecycleEvent(
                lifecycleEvents.size() + 1,
                value(moduleId),
                phaseId.trim(),
                EchoNativeLoadStatus.MUTATED.name(),
                declaredLifecycleDetail(phaseId, enrichedEvidence),
                false,
                List.of(),
                Map.copyOf(enrichedEvidence)
        );
        lifecycleEvents.add(event);
        return event;
    }

    public PublishedEvent publish(
            String sourceModule,
            String eventId,
            Map<String, Object> payload,
            EchoNativeLoadStatus status
    ) {
        if (eventId == null || eventId.isBlank()) {
            throw new IllegalArgumentException("eventId is required");
        }
        EventEnvelope envelope = new EventEnvelope(
                value(sourceModule),
                eventId.trim(),
                status == null ? EchoNativeLoadStatus.DISCOVERED.name() : status.name(),
                payload == null ? Map.of() : Map.copyOf(payload)
        );
        Map<String, Object> livePayloadEvidence = new LinkedHashMap<>(envelope.payload());
        clearLiveDispatchProof(livePayloadEvidence);
        String liveRuntimeDispatchId = beginLiveRuntimeDispatch(
                EVENT_SERVICE_ID,
                "events",
                envelope.sourceModule() + ":" + envelope.eventId(),
                livePayloadEvidence
        );
        EchoNativeLoadStatus liveStatus = dispatchRuntimeEvent(
                envelope.sourceModule(),
                envelope.eventId(),
                livePayloadEvidence,
                status
        );
        boolean liveDispatchProofSatisfied = liveDispatchProofSatisfied(liveStatus, livePayloadEvidence, liveRuntimeDispatchId, "events");
        livePayloadEvidence.put("subsystemLiveRuntimeDispatchProofSatisfied", liveDispatchProofSatisfied);
        livePayloadEvidence.put("liveMinecraftMutation", liveDispatchProofSatisfied);
        livePayloadEvidence.put("minecraftRuntimeAccessed", liveDispatchProofSatisfied);
        if (liveDispatchProofSatisfied) {
            liveRuntimeMutationCount++;
        }
        List<Map<String, Object>> handlerResults = executeHandlers(envelope);
        PublishedEvent event = new PublishedEvent(
                publishedEvents.size() + 1,
                envelope.sourceModule(),
                envelope.eventId(),
                envelope.status(),
                envelope.payload(),
                handlerResults.size(),
                !handlerResults.isEmpty(),
                handlerResults,
                liveStatus.name(),
                liveRuntimeBridge.liveRuntimeAccessed(),
                liveDispatchProofSatisfied,
                liveDispatchProofSatisfied,
                Map.copyOf(livePayloadEvidence)
        );
        publishedEvents.add(event);
        return event;
    }

    public List<PublishedEvent> publishSubscribedEventsForModule(
            String moduleId,
            Map<String, Object> payload,
            EchoNativeLoadStatus status
    ) {
        String safeModuleId = value(moduleId);
        if (safeModuleId.isBlank()) {
            return List.of();
        }
        List<String> eventIds = new ArrayList<>();
        for (Map.Entry<String, List<EventSubscription>> entry : subscriptionsByEventId.entrySet()) {
            boolean moduleSubscribed = entry.getValue().stream()
                    .anyMatch(subscription -> safeModuleId.equals(subscription.moduleId()));
            if (moduleSubscribed) {
                eventIds.add(entry.getKey());
            }
        }
        if (eventIds.isEmpty()) {
            return List.of();
        }
        List<PublishedEvent> events = new ArrayList<>();
        for (String eventId : eventIds) {
            Map<String, Object> dispatchPayload = new LinkedHashMap<>();
            if (payload != null) {
                dispatchPayload.putAll(payload);
            }
            dispatchPayload.put("moduleId", safeModuleId);
            dispatchPayload.put("declaredEventId", eventId);
            dispatchPayload.put("dispatchMode", "native_loader_declared_event_subscription_runtime_dispatch");
            events.add(publish(safeModuleId, eventId, dispatchPayload, status));
        }
        return List.copyOf(events);
    }

    public void subscribe(String moduleId, String eventId, NativeEventHandler handler) {
        subscribe(moduleId, eventId, handler, handler == null ? "" : handler.getClass().getName());
    }

    private void subscribe(String moduleId, String eventId, NativeEventHandler handler, String handlerId) {
        if (eventId == null || eventId.isBlank()) {
            throw new IllegalArgumentException("eventId is required");
        }
        if (handler == null) {
            throw new IllegalArgumentException("handler is required");
        }
        String checkedModuleId = value(moduleId);
        String checkedEventId = eventId.trim();
        String checkedHandlerId = handlerId == null || handlerId.isBlank()
                ? handler.getClass().getName()
                : handlerId.trim();
        String subscriptionKey = checkedModuleId + "\u0000" + checkedEventId + "\u0000" + checkedHandlerId;
        if (subscriptionsByKey.containsKey(subscriptionKey)) {
            throw new IllegalStateException("Duplicate native event subscription collision for "
                    + checkedModuleId + ":" + checkedEventId + ":" + checkedHandlerId);
        }
        EventSubscription subscription = new EventSubscription(checkedModuleId, checkedEventId, handler);
        subscriptionsByKey.put(subscriptionKey, subscription);
        subscriptionsByEventId
                .computeIfAbsent(checkedEventId, ignored -> new ArrayList<>())
                .add(subscription);
    }

    public void subscribeDeclaredHook(
            String moduleId,
            String eventId,
            String handlerId,
            Map<String, Object> evidence
    ) {
        if (handlerId == null || handlerId.isBlank()) {
            throw new IllegalArgumentException("handlerId is required");
        }
        Map<String, Object> safeEvidence = evidence == null ? Map.of() : Map.copyOf(evidence);
        subscribe(moduleId, eventId, event -> {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("moduleId", value(moduleId));
            result.put("handler", handlerId.trim());
            result.put("declaredEventId", eventId.trim());
            result.put("eventId", event.eventId());
            result.put("sourceModule", event.sourceModule());
            result.put("status", event.status());
            result.put("adaptercoreDeclaredHandlerExecuted", true);
            result.put("summary", string(safeEvidence.get("summary")));
            result.put("evidence", safeEvidence);
            return Map.copyOf(result);
        }, handlerId);
    }

    public List<LifecycleEvent> lifecycleEvents() {
        return List.copyOf(lifecycleEvents);
    }

    public List<PublishedEvent> publishedEvents() {
        return List.copyOf(publishedEvents);
    }

    public int lifecycleEventCount() {
        return lifecycleEvents.size();
    }

    public int failedLifecycleEventCount() {
        int count = 0;
        for (LifecycleEvent event : lifecycleEvents) {
            if (event.failed()) {
                count++;
            }
        }
        return count;
    }

    public int publishedEventCount() {
        return publishedEvents.size();
    }

    public int eventSubscriptionCount() {
        return subscriptionsByEventId.values().stream().mapToInt(List::size).sum();
    }

    public int executedEventHandlerCount() {
        int count = 0;
        for (PublishedEvent event : publishedEvents) {
            if (event.handlerExecuted()) {
                count++;
            }
        }
        return count;
    }

    public int liveRuntimeMutationCount() {
        return liveRuntimeMutationCount;
    }

    public boolean liveRuntimeMutationCoverageSatisfied() {
        int provedEntryCount = liveRuntimeEntryMutationProofCount();
        int eventCount = lifecycleEventCount() + publishedEventCount();
        return liveRuntimeDispatchCount > 0
                && liveRuntimeDispatchCount == eventCount
                && liveRuntimeMutationCount == liveRuntimeDispatchCount
                && provedEntryCount == liveRuntimeDispatchCount;
    }

    public boolean liveRuntimeReleaseProofSatisfied() {
        return liveRuntimeBridge.attached()
                && liveRuntimeBridge.liveRuntimeAccessed()
                && liveRuntimeBridge.minecraftRuntimeAccessed()
                && liveRuntimeBridge.liveRuntimeMutationSupported()
                && liveRuntimeMutationCoverageSatisfied();
    }

    public static Map<String, Object> markerFields(
            Map<String, Object> lifecycleBridge,
            Map<String, Object> eventBridge
    ) {
        Map<String, Object> lifecycle = lifecycleBridge == null ? Map.of() : lifecycleBridge;
        Map<String, Object> event = eventBridge == null ? Map.of() : eventBridge;
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("nativeLifecycleEventMarkerServiceIds", List.of(LIFECYCLE_SERVICE_ID, EVENT_SERVICE_ID));
        fields.put("nativeLifecycleBridgeApplied", Boolean.TRUE.equals(lifecycle.get("applied")));
        fields.put("nativeSafeLifecycleHookRunCount", intValue(lifecycle.get("safeLifecycleHookRunCount")));
        fields.put("nativeLifecycleCallbacksExecuted",
                Boolean.TRUE.equals(lifecycle.get("allRequiredCallbacksCalled")));
        fields.put("nativeLifecycleCallbackCount", intValue(lifecycle.get("calledCallbackCount")));
        fields.put("nativeLifecycleCallbackNames", lifecycle.getOrDefault("requiredCallbacks", List.of()));
        fields.put("nativeEventBridgeApplied", Boolean.TRUE.equals(event.get("applied")));
        fields.put("nativeSafeEventHookRunCount", intValue(event.get("safeEventHookRunCount")));
        fields.put("nativeEventHookAttachedCount", intValue(event.get("safeEventHookAttachedCount")));
        fields.put("nativeEventHostSubscriptionCount", intValue(event.get("nativeEventHostSubscriptionCount")));
        fields.put("nativePublishedEventCount", intValue(event.get("nativePublishedEventCount")));
        fields.put("nativePublishedEventHandlerCount", intValue(event.get("nativePublishedEventHandlerCount")));
        return Map.copyOf(fields);
    }

    public Map<String, Object> toReport() {
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("lifecycleServiceId", LIFECYCLE_SERVICE_ID);
        report.put("eventServiceId", EVENT_SERVICE_ID);
        report.put("lifecycleEventCount", lifecycleEventCount());
        report.put("failedLifecycleEventCount", failedLifecycleEventCount());
        report.put("publishedEventCount", publishedEventCount());
        report.put("eventSubscriptionCount", eventSubscriptionCount());
        report.put("executedEventHandlerCount", executedEventHandlerCount());
        report.put("liveRuntimeBridgeAttached", liveRuntimeBridge.attached());
        report.put("liveRuntimeDispatchCount", liveRuntimeDispatchCount);
        report.put("liveRuntimeMutationCount", liveRuntimeMutationCount);
        report.put("liveRuntimeUnmutatedDispatchCount", Math.max(0, liveRuntimeDispatchCount - liveRuntimeMutationCount));
        report.put("liveRuntimeUndispatchedEventCount",
                Math.max(0, lifecycleEventCount() + publishedEventCount() - liveRuntimeDispatchCount));
        report.put("liveRuntimeDispatchProofEntryCount", liveRuntimeEntryMutationProofCount());
        report.put("liveRuntimeUnprovedDispatchEntryCount",
                Math.max(0, liveRuntimeDispatchCount - liveRuntimeEntryMutationProofCount()));
        report.put("liveRuntimeMutationCoverageSatisfied", liveRuntimeMutationCoverageSatisfied());
        report.put("liveRuntimeAccessed", liveRuntimeBridge.liveRuntimeAccessed());
        report.put("minecraftRuntimeAccessed", liveRuntimeBridge.minecraftRuntimeAccessed());
        report.put("partialLiveMinecraftMutation", liveRuntimeBridge.minecraftRuntimeAccessed() && liveRuntimeMutationCount > 0);
        report.put("liveMinecraftMutation", liveRuntimeReleaseProofSatisfied());
        report.put("mirrorOnlyReleaseProof", (lifecycleEventCount() + publishedEventCount()) > 0
                && liveRuntimeMutationCount == 0);
        report.put("liveRuntimeReleaseProofSatisfied", liveRuntimeReleaseProofSatisfied());
        report.put("lifecycleEvents", lifecycleEvents.stream().map(LifecycleEvent::toReport).toList());
        report.put("publishedEvents", publishedEvents.stream().map(PublishedEvent::toReport).toList());
        return Map.copyOf(report);
    }

    private int liveRuntimeEntryMutationProofCount() {
        int count = 0;
        for (LifecycleEvent event : lifecycleEvents) {
            if (entryHasLiveRuntimeMutationProof(event.evidence())) {
                count++;
            }
        }
        for (PublishedEvent event : publishedEvents) {
            if (entryHasLiveRuntimeMutationProof(event.liveRuntimeEvidence())) {
                count++;
            }
        }
        return count;
    }

    private static boolean entryHasLiveRuntimeMutationProof(Map<String, Object> evidence) {
        Map<String, Object> safeEvidence = evidence == null ? Map.of() : evidence;
        return !string(safeEvidence.get("liveRuntimeDispatchId")).isBlank()
                && !string(safeEvidence.get("liveRuntimeSurface")).isBlank()
                && Boolean.TRUE.equals(safeEvidence.get("subsystemLiveRuntimeDispatchProofSatisfied"))
                && Boolean.TRUE.equals(safeEvidence.get("minecraftRuntimeAccessed"))
                && Boolean.TRUE.equals(safeEvidence.get("liveMinecraftMutation"));
    }

    private EchoNativeLoadStatus dispatchLifecycle(String moduleId, String phaseId, Map<String, Object> evidence) {
        if (!liveRuntimeBridge.attached()) {
            return EchoNativeLoadStatus.UNSUPPORTED;
        }
        liveRuntimeDispatchCount++;
        EchoNativeLoadStatus status;
        try {
            status = liveRuntimeBridge.lifecyclePhase(moduleId, phaseId, evidence);
        } catch (RuntimeException exception) {
            return EchoNativeLoadStatus.FAILED;
        }
        return status == null ? EchoNativeLoadStatus.FAILED : status;
    }

    private EchoNativeLoadStatus dispatchRuntimeEvent(
            String sourceModule,
            String eventId,
            Map<String, Object> payload,
            EchoNativeLoadStatus status
    ) {
        if (!liveRuntimeBridge.attached()) {
            return EchoNativeLoadStatus.UNSUPPORTED;
        }
        liveRuntimeDispatchCount++;
        EchoNativeLoadStatus liveStatus;
        try {
            liveStatus = liveRuntimeBridge.publishRuntimeEvent(sourceModule, eventId, payload, status);
        } catch (RuntimeException exception) {
            return EchoNativeLoadStatus.FAILED;
        }
        return liveStatus == null ? EchoNativeLoadStatus.FAILED : liveStatus;
    }

    private boolean liveDispatchProofSatisfied(
            EchoNativeLoadStatus status,
            Map<String, Object> evidence,
            String dispatchId,
            String surface
    ) {
        return status == EchoNativeLoadStatus.MUTATED
                && liveRuntimeBridge.liveRuntimeAccessed()
                && liveRuntimeBridge.minecraftRuntimeAccessed()
                && liveRuntimeBridge.liveRuntimeMutationSupported()
                && bool(evidence.get("liveRuntimeDispatchProofSatisfied"))
                && bool(evidence.get("liveRuntimeDispatchMinecraftAccessed"))
                && bool(evidence.get("liveRuntimeDispatchMutationSupported"))
                && bool(evidence.get("liveRuntimeDispatchLiveMutation"))
                && dispatchId != null
                && dispatchId.equals(String.valueOf(evidence.getOrDefault("liveRuntimeDispatchId", "")))
                && liveRuntimeSurfaceMatches(surface, evidence)
                && subsystemRuntimeSideEffectSatisfied(surface, evidence);
    }

    private static void clearLiveDispatchProof(Map<String, Object> evidence) {
        evidence.remove("liveRuntimeDispatchProofSatisfied");
        evidence.remove("liveRuntimeDispatchMinecraftAccessed");
        evidence.remove("liveRuntimeDispatchMutationSupported");
        evidence.remove("liveRuntimeDispatchLiveMutation");
        evidence.remove("liveRuntimeDispatchId");
        evidence.remove("liveRuntimeSurface");
        evidence.remove("liveMinecraftMutation");
        evidence.remove("minecraftRuntimeAccessed");
        clearRuntimeSideEffectProof(evidence);
    }

    private String beginLiveRuntimeDispatch(String serviceId, String surface, String key, Map<String, Object> evidence) {
        String dispatchId = serviceId + ":" + surface + ":" + (++liveRuntimeDispatchSequence);
        evidence.put("liveRuntimeDispatchId", dispatchId);
        liveRuntimeBridge.beginLiveRuntimeSurfaceDispatch(surface, dispatchId);
        return dispatchId;
    }

    private static boolean bool(Object value) {
        return Boolean.TRUE.equals(value);
    }

    private static boolean liveRuntimeSurfaceMatches(String surface, Map<String, Object> evidence) {
        String actual = String.valueOf(evidence.getOrDefault("liveRuntimeSurface", "")).trim();
        return !actual.isBlank() && actual.equals(surface == null ? "" : surface);
    }

    private static boolean subsystemRuntimeSideEffectSatisfied(String surface, Map<String, Object> evidence) {
        if (!"lifecycle_phases".equals(surface) && !"events".equals(surface)) {
            return true;
        }
        boolean saveSatisfied = Boolean.TRUE.equals(evidence.get("runtimeSurfaceSaveTouched"))
                && Boolean.TRUE.equals(evidence.get("runtimeSurfaceSaveMutated"))
                && Boolean.TRUE.equals(evidence.get("runtimeSaveDataTouched"))
                && Boolean.TRUE.equals(evidence.get("liveSaveDataFileTouched"))
                && "world_save_file".equals(String.valueOf(evidence.get("runtimeSaveDataBackend")))
                && evidence.get("saveFile") instanceof String saveFile
                && !saveFile.isBlank();
        if (!saveSatisfied) {
            return false;
        }
        if ("lifecycle_phases".equals(surface)) {
            return Boolean.TRUE.equals(evidence.get("runtimeLifecyclePhaseTouched"))
                    && Boolean.TRUE.equals(evidence.get("runtimeLifecyclePhaseMutated"));
        }
        if (!"events".equals(surface)) {
            return true;
        }
        return Boolean.TRUE.equals(evidence.get("runtimeSurfaceEventPublished"))
                && Boolean.TRUE.equals(evidence.get("runtimeSurfaceEventMutated"))
                && Boolean.TRUE.equals(evidence.get("runtimeEventTouched"))
                && Boolean.TRUE.equals(evidence.get("runtimeEventMutated"))
                && Boolean.TRUE.equals(evidence.get("runtimeEventPublished"));
    }

    private static void clearRuntimeSideEffectProof(Map<String, Object> evidence) {
        evidence.remove("runtimeSurfaceSaveTouched");
        evidence.remove("runtimeSurfaceSaveMutated");
        evidence.remove("runtimeSaveDataTouched");
        evidence.remove("runtimeSaveDataMutated");
        evidence.remove("liveSaveDataFileTouched");
        evidence.remove("runtimeSaveDataBackend");
        evidence.remove("saveFile");
        evidence.remove("runtimeSurfaceEventPublished");
        evidence.remove("runtimeSurfaceEventMutated");
        evidence.remove("runtimeLifecyclePhaseTouched");
        evidence.remove("runtimeLifecyclePhaseMutated");
        evidence.remove("runtimeLifecyclePhaseId");
        evidence.remove("runtimeLifecycleModuleId");
        evidence.remove("runtimeEventTouched");
        evidence.remove("runtimeEventMutated");
        evidence.remove("runtimeEventPublished");
        evidence.remove("runtimeEventId");
        evidence.remove("runtimeEventSourceModule");
    }

    private List<Map<String, Object>> executeHandlers(EventEnvelope event) {
        List<EventSubscription> subscriptions = subscriptionsByEventId.getOrDefault(event.eventId(), List.of());
        if (subscriptions.isEmpty()) {
            return List.of();
        }
        List<Map<String, Object>> results = new ArrayList<>();
        for (EventSubscription subscription : subscriptions) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("moduleId", subscription.moduleId());
            result.put("eventId", subscription.eventId());
            try {
                Map<String, Object> handlerResult = subscription.handler().handle(event);
                result.put("handled", true);
                result.put("result", handlerResult == null ? Map.of() : Map.copyOf(handlerResult));
            } catch (RuntimeException exception) {
                result.put("handled", false);
                result.put("error", exception.getClass().getSimpleName() + ": " + exception.getMessage());
            }
            results.add(Map.copyOf(result));
        }
        return List.copyOf(results);
    }

    private static String value(String value) {
        return value == null ? "" : value.trim();
    }

    private static String string(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private static int intValue(Object value) {
        return value instanceof Number number ? number.intValue() : 0;
    }

    private static String declaredLifecycleDetail(String phaseId, Map<String, Object> evidence) {
        String summary = string(evidence.get("summary"));
        if (!summary.isBlank()) {
            return summary;
        }
        return "Declared native lifecycle phase '" + phaseId.trim() + "' recorded by the native lifecycle host.";
    }

    public record LifecycleEvent(
            int sequence,
            String moduleId,
            String phase,
            String status,
            String detail,
            boolean failed,
            List<String> failures,
            Map<String, Object> evidence
    ) {
        public Map<String, Object> toReport() {
            Map<String, Object> safeEvidence = evidence == null ? Map.of() : Map.copyOf(evidence);
            Map<String, Object> report = new LinkedHashMap<>();
            report.put("sequence", sequence);
            report.put("moduleId", moduleId);
            report.put("phase", phase);
            report.put("status", status);
            report.put("detail", detail == null ? "" : detail);
            report.put("failed", failed);
            report.put("failures", failures == null ? List.of() : List.copyOf(failures));
            report.put("liveRuntimeDispatchId", string(safeEvidence.get("liveRuntimeDispatchId")));
            report.put("liveRuntimeSurface", string(safeEvidence.get("liveRuntimeSurface")));
            report.put("subsystemLiveRuntimeDispatchProofSatisfied",
                    Boolean.TRUE.equals(safeEvidence.get("subsystemLiveRuntimeDispatchProofSatisfied")));
            report.put("liveMinecraftMutation", Boolean.TRUE.equals(safeEvidence.get("liveMinecraftMutation")));
            report.put("minecraftRuntimeAccessed", Boolean.TRUE.equals(safeEvidence.get("minecraftRuntimeAccessed")));
            report.put("evidence", safeEvidence);
            return Map.copyOf(report);
        }
    }

    public record PublishedEvent(
            int sequence,
            String sourceModule,
            String eventId,
            String status,
            Map<String, Object> payload,
            int handlerCount,
            boolean handlerExecuted,
            List<Map<String, Object>> handlerResults,
            String liveRuntimeBridgeStatus,
            boolean liveRuntimeAccessed,
            boolean minecraftRuntimeAccessed,
            boolean liveMinecraftMutation,
            Map<String, Object> liveRuntimeEvidence
    ) {
        public Map<String, Object> toReport() {
            Map<String, Object> safeLiveEvidence = liveRuntimeEvidence == null ? Map.of() : Map.copyOf(liveRuntimeEvidence);
            Map<String, Object> report = new LinkedHashMap<>();
            report.put("sequence", sequence);
            report.put("sourceModule", sourceModule);
            report.put("eventId", eventId);
            report.put("status", status);
            report.put("payload", payload == null ? Map.of() : Map.copyOf(payload));
            report.put("handlerCount", handlerCount);
            report.put("handlerExecuted", handlerExecuted);
            report.put("handlerResults", handlerResults == null ? List.of() : List.copyOf(handlerResults));
            report.put("liveRuntimeBridgeStatus", liveRuntimeBridgeStatus == null ? "" : liveRuntimeBridgeStatus);
            report.put("liveRuntimeAccessed", liveRuntimeAccessed);
            report.put("minecraftRuntimeAccessed", minecraftRuntimeAccessed);
            report.put("liveMinecraftMutation", liveMinecraftMutation);
            report.put("liveRuntimeDispatchId", string(safeLiveEvidence.get("liveRuntimeDispatchId")));
            report.put("liveRuntimeSurface", string(safeLiveEvidence.get("liveRuntimeSurface")));
            report.put("subsystemLiveRuntimeDispatchProofSatisfied",
                    Boolean.TRUE.equals(safeLiveEvidence.get("subsystemLiveRuntimeDispatchProofSatisfied")));
            report.put("liveRuntimeEvidence", safeLiveEvidence);
            return Map.copyOf(report);
        }
    }

    public record EventEnvelope(
            String sourceModule,
            String eventId,
            String status,
            Map<String, Object> payload
    ) {
    }

    @FunctionalInterface
    public interface NativeEventHandler {
        Map<String, Object> handle(EventEnvelope event);
    }

    private record EventSubscription(
            String moduleId,
            String eventId,
            NativeEventHandler handler
    ) {
    }
}
