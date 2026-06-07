package dev.echo.nativeplatform.testkit;

import dev.echo.nativeplatform.cli.EchoNativeQaCli;
import dev.echo.nativeplatform.diagnostics.EchoNativeJson;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoNativeReportSnapshotVerifier {
    private static final List<String> REPORT_FILES = List.of(
            "reports/echo-native/ashfall/ai-graph.json",
            "reports/echo-native/ashfall/ai-tasks.json",
            "reports/echo-native/ashfall/boundary-failure-cases.json",
            "reports/echo-native/ashfall/bootstrap-plan.json",
            "reports/echo-native/ashfall/classloader-boundary-rehearsal.json",
            "reports/echo-native/ashfall/classpath-classloader-compatibility.json",
            "reports/echo-native/ashfall/classpath-builder-plan.json",
            "reports/echo-native/ashfall/classpath-builder-safety-status.json",
            "reports/echo-native/ashfall/classpath-source-policy.json",
            "reports/echo-native/ashfall/native-extraction-plan.json",
            "reports/echo-native/ashfall/native-extraction-safety-status.json",
            "reports/echo-native/ashfall/native-extraction-source-policy.json",
            "reports/echo-native/ashfall/launch-argument-builder-plan.json",
            "reports/echo-native/ashfall/launch-argument-safety-status.json",
            "reports/echo-native/ashfall/launch-argument-source-policy.json",
            "reports/echo-native/ashfall/controlled-dummy-process-plan.json",
            "reports/echo-native/ashfall/controlled-dummy-process-result.json",
            "reports/echo-native/ashfall/dummy-process-crash-boundary.json",
            "reports/echo-native/ashfall/dummy-process-output-capture.json",
            "reports/echo-native/ashfall/addon-runtime-discovery-plan.json",
            "reports/echo-native/ashfall/addon-runtime-descriptors.json",
            "reports/echo-native/ashfall/addon-runtime-discovery-safety-status.json",
            "reports/echo-native/ashfall/lifecycle-stub-execution-plan.json",
            "reports/echo-native/ashfall/lifecycle-stub-execution-result.json",
            "reports/echo-native/ashfall/lifecycle-stub-crash-boundary.json",
            "reports/echo-native/ashfall/lifecycle-stub-safety-status.json",
            "reports/echo-native/ashfall/service-bus-plan.json",
            "reports/echo-native/ashfall/service-bus-registry.json",
            "reports/echo-native/ashfall/service-bus-simulation-result.json",
            "reports/echo-native/ashfall/service-bus-safety-status.json",
            "reports/echo-native/ashfall/config-source-inventory.json",
            "reports/echo-native/ashfall/config-validation-result.json",
            "reports/echo-native/ashfall/config-write-plan.json",
            "reports/echo-native/ashfall/config-safety-status.json",
            "reports/echo-native/ashfall/resource-source-inventory.json",
            "reports/echo-native/ashfall/resource-namespace-validation.json",
            "reports/echo-native/ashfall/resource-pack-order-plan.json",
            "reports/echo-native/ashfall/resource-conflict-report.json",
            "reports/echo-native/ashfall/resource-bridge-safety-status.json",
            "reports/echo-native/ashfall/registry-source-inventory.json",
            "reports/echo-native/ashfall/registry-id-validation.json",
            "reports/echo-native/ashfall/sandbox-registry-model.json",
            "reports/echo-native/ashfall/registry-conflict-report.json",
            "reports/echo-native/ashfall/registry-bridge-safety-status.json",
            "reports/echo-native/ashfall/network-channel-inventory.json",
            "reports/echo-native/ashfall/network-packet-validation.json",
            "reports/echo-native/ashfall/network-schema-model.json",
            "reports/echo-native/ashfall/network-conflict-report.json",
            "reports/echo-native/ashfall/network-bridge-safety-status.json",
            "reports/echo-native/ashfall/transform-source-inventory.json",
            "reports/echo-native/ashfall/transform-allowlist-validation.json",
            "reports/echo-native/ashfall/transform-pipeline-plan.json",
            "reports/echo-native/ashfall/transform-conflict-report.json",
            "reports/echo-native/ashfall/transform-safety-status.json",
            "reports/echo-native/ashfall/crash-hardening-coverage.json",
            "reports/echo-native/ashfall/failure-containment-matrix.json",
            "reports/echo-native/ashfall/support-bundle-dry-run-plan.json",
            "reports/echo-native/ashfall/phase13-m16-safety-status.json",
            "reports/echo-native/ashfall/isolated-launch-environment-plan.json",
            "reports/echo-native/ashfall/minecraft-launch-preflight.json",
            "reports/echo-native/ashfall/launch-safety-gate.json",
            "reports/echo-native/ashfall/controlled-launch-failure-capture-plan.json",
            "reports/echo-native/ashfall/phase13-m17-readiness.json",
            "reports/echo-native/ashfall/isolated-launch-attempt-plan.json",
            "reports/echo-native/ashfall/local-runtime-artifact-check.json",
            "reports/echo-native/ashfall/controlled-launch-attempt-result.json",
            "reports/echo-native/ashfall/launch-output-capture.json",
            "reports/echo-native/ashfall/phase13-m17-launch-status.json",
            "reports/echo-native/ashfall/local-runtime-artifact-inventory.json",
            "reports/echo-native/ashfall/local-runtime-artifact-map.json",
            "reports/echo-native/ashfall/launch-artifact-resolution-status.json",
            "reports/echo-native/ashfall/isolated-launch-execution-eligibility.json",
            "reports/echo-native/ashfall/phase13-m17-artifact-readiness.json",
            "reports/echo-native/ashfall/phase13-m17-artifact-blockers.json",
            "reports/echo-native/ashfall/phase13-m17-blocker-resolution-plan.json",
            "reports/echo-native/ashfall/phase13-m17-artifact-packaging-audit.json",
            "reports/echo-native/ashfall/phase13-m17-artifact-packaging-resolution-plan.json",
            "reports/echo-native/ashfall/phase13-m18-readiness.json",
            "reports/echo-native/ashfall/runtime-fixture-presence.json",
            "reports/echo-native/ashfall/runtime-fixture-mapping-readiness.json",
            "reports/echo-native/ashfall/runtime-fixture-intake-plan.json",
            "reports/echo-native/ashfall/runtime-fixture-intake-checklist.json",
            "reports/echo-native/ashfall/runtime-fixture-approval-audit.json",
            "reports/echo-native/ashfall/runtime-fixture-approval-template.json",
            "reports/echo-native/ashfall/runtime-fixture-handoff.json",
            "reports/echo-native/ashfall/runtime-fixture-validation-runbook.json",
            "reports/echo-native/ashfall/runtime-fixture-approval-draft.json",
            "reports/echo-native/ashfall/runtime-fixture-hash-review.json",
            "reports/echo-native/ashfall/runtime-fixture-operator-packet.json",
            "reports/echo-native/ashfall/runtime-fixture-integrity-audit.json",
            "reports/echo-native/ashfall/runtime-fixture-integrity-manifest.json",
            "reports/echo-native/ashfall/phase13-m17-completion.json",
            "reports/echo-native/ashfall/phase13-m18-readiness-audit.json",
            "reports/echo-native/ashfall/smoke-session-plan.json",
            "reports/echo-native/ashfall/smoke-session-safety-gate.json",
            "reports/echo-native/ashfall/smoke-session-result.json",
            "reports/echo-native/ashfall/smoke-session-diagnostics.json",
            "reports/echo-native/ashfall/phase13-m18-completion.json",
            "reports/echo-native/ashfall/phase13-m19-readiness.json",
            "reports/echo-native/ashfall/first-playtest-candidate-package.json",
            "reports/echo-native/ashfall/first-playtest-support-bundle.json",
            "reports/echo-native/ashfall/first-playtest-rollback-notes.json",
            "reports/echo-native/ashfall/first-playtest-known-limitations.json",
            "reports/echo-native/ashfall/experimental-native-loader-label.json",
            "reports/echo-native/ashfall/first-playtest-crash-report-collection.json",
            "reports/echo-native/ashfall/phase13-m19-completion.json",
            "reports/echo-native/ashfall/first-playtest-open-gate.json",
            "reports/echo-native/ashfall/phase13-first-playtest-blockers.json",
            "reports/echo-native/ashfall/phase13-first-playtest-roadmap.json",
            "reports/echo-native/ashfall/phase13-first-playtest-next-actions.json",
            "reports/echo-native/ashfall/phase13-first-playtest-full-roadmap.json",
            "reports/echo-native/ashfall/first-playtest-post-open-intake.json",
            "reports/echo-native/ashfall/first-playtest-feedback-inventory.json",
            "reports/echo-native/ashfall/first-playtest-waiting-checklist.json",
            "reports/echo-native/ashfall/phase14-preflight-audit.json",
            "reports/echo-native/ashfall/phase14-readiness.json",
            "reports/echo-native/ashfall/phase14-next-actions.json",
            "reports/echo-native/ashfall/native-loader-reality-audit.json",
            "reports/echo-native/ashfall/native-loader-launch-command-classification.json",
            "reports/echo-native/ashfall/native-loader-beta-implementation-next-actions.json",
            "reports/echo-native/ashfall/isolated-runtime-workspace-plan.json",
            "reports/echo-native/ashfall/isolated-runtime-workspace-materialization.json",
            "reports/echo-native/ashfall/isolated-runtime-workspace-safety-status.json",
            "reports/echo-native/ashfall/real-process-launch-harness-plan.json",
            "reports/echo-native/ashfall/real-process-launch-safety-gate.json",
            "reports/echo-native/ashfall/real-process-command-line-preview.json",
            "reports/echo-native/ashfall/real-process-environment-plan.json",
            "reports/echo-native/ashfall/process-execution-readiness.json",
            "reports/echo-native/ashfall/controlled-launch-operator-checklist.json",
            "reports/echo-native/ashfall/controlled-launch-rollback-plan.json",
            "reports/echo-native/ashfall/phase13-native-loader-beta-gate.json",
            "reports/echo-native/ashfall/native-loader-beta-feedback-inventory.json",
            "reports/echo-native/ashfall/native-loader-beta-crash-intake.json",
            "reports/echo-native/ashfall/native-loader-beta-known-issues.json",
            "reports/echo-native/ashfall/native-loader-beta-next-action-queue.json",
            "reports/echo-native/ashfall/phase13-m27-completion.json",
            "reports/echo-native/ashfall/phase13-m28-readiness.json",
            "reports/echo-native/ashfall/native-loader-beta-widening-plan.json",
            "reports/echo-native/ashfall/native-loader-beta-widening-safety-gate.json",
            "reports/echo-native/ashfall/beta-tester-cohort-plan.json",
            "reports/echo-native/ashfall/phase13-m28-completion.json",
            "reports/echo-native/ashfall/phase13-m29-readiness.json",
            "reports/echo-native/ashfall/native-loader-beta-soak-plan.json",
            "reports/echo-native/ashfall/native-loader-beta-session-inventory.json",
            "reports/echo-native/ashfall/native-loader-beta-issue-triage.json",
            "reports/echo-native/ashfall/native-loader-beta-regression-watchlist.json",
            "reports/echo-native/ashfall/phase13-m29-completion.json",
            "reports/echo-native/ashfall/phase13-m30-readiness.json",
            "reports/echo-native/ashfall/native-loader-beta-soak-operator-packet.json",
            "reports/echo-native/ashfall/native-loader-beta-session-template.json",
            "reports/echo-native/ashfall/native-loader-beta-session-note-drafts.json",
            "reports/echo-native/ashfall/native-loader-beta-session-draft-files.json",
            "reports/echo-native/ashfall/native-loader-beta-session-note-validation.json",
            "reports/echo-native/ashfall/native-loader-beta-evidence-checklist.json",
            "reports/echo-native/ashfall/native-loader-beta-remaining-session-plan.json",
            "reports/echo-native/ashfall/phase13-m29-soak-operator-status.json",
            "reports/echo-native/ashfall/phase13-m29-session-draft-status.json",
            "reports/echo-native/ashfall/phase13-m29-note-validation-status.json",
            "reports/echo-native/ashfall/native-loader-beta-soak-status-dashboard.json",
            "reports/echo-native/ashfall/native-loader-beta-next-session-checklist.json",
            "reports/echo-native/ashfall/native-loader-beta-evidence-quality.json",
            "reports/echo-native/ashfall/native-loader-beta-session-proof-matrix.json",
            "reports/echo-native/ashfall/phase13-m29-evidence-gap.json",
            "reports/echo-native/ashfall/native-loader-public-beta-candidate-audit.json",
            "reports/echo-native/ashfall/native-loader-public-beta-safety-gate.json",
            "reports/echo-native/ashfall/public-beta-tester-readiness.json",
            "reports/echo-native/ashfall/phase13-m30-completion.json",
            "reports/echo-native/ashfall/phase13-m31-readiness.json",
            "reports/echo-native/ashfall/public-beta-opening-audit.json",
            "reports/echo-native/ashfall/public-beta-safety-gate.json",
            "reports/echo-native/ashfall/public-beta-tester-package-readiness.json",
            "reports/echo-native/ashfall/public-beta-module-coverage.json",
            "reports/echo-native/ashfall/public-beta-rollback-readiness.json",
            "reports/echo-native/ashfall/public-beta-known-limitations.json",
            "reports/echo-native/ashfall/phase13-m31-completion.json",
            "reports/echo-native/ashfall/phase13-m32-readiness.json",
            "reports/echo-native/ashfall/controlled-test-process-preflight.json",
            "reports/echo-native/ashfall/lockfile-status.json",
            "reports/echo-native/ashfall/classloader-boundary-plan.json",
            "reports/echo-native/ashfall/crash-boundary-simulation-result.json",
            "reports/echo-native/ashfall/crash-boundary-result.json",
            "reports/echo-native/ashfall/crash-boundary-plan.json",
            "reports/echo-native/ashfall/lifecycle-simulation-plan.json",
            "reports/echo-native/ashfall/lifecycle-simulation-result.json",
            "reports/echo-native/ashfall/loader-boundary-state-machine.json",
            "reports/echo-native/ashfall/loader-boundary-verification.json",
            "reports/echo-native/ashfall/phase12-completion.json",
            "reports/echo-native/ashfall/phase13-readiness.json",
            "reports/echo-native/ashfall/phase13-plan.json",
            "reports/echo-native/ashfall/repair-plan.json",
            "reports/echo-native/ashfall/crash-boundary-verification.json",
            "reports/echo-native/ashfall/service-attach-simulation-result.json",
            "reports/echo-native/ashfall/phase13-m1-safety-status.json",
            "reports/echo-native/ashfall/phase13-bridge-safety-status.json",
            "reports/echo-native/ashfall/phase13-m1-completion.json",
            "reports/echo-native/ashfall/phase13-m2-readiness.json",
            "reports/echo-native/ashfall/phase13-prototype-safety-gate.json",
            "reports/echo-native/ashfall/minecraft-resolver-safety-status.json",
            "reports/echo-native/ashfall/minecraft-version-resolver-plan.json",
            "reports/echo-native/ashfall/minecraft-version-source-policy.json",
            "reports/echo-native/ashfall/library-resolution-plan.json",
            "reports/echo-native/ashfall/library-resolver-safety-status.json",
            "reports/echo-native/ashfall/library-source-policy.json",
            "reports/echo-native/ashfall/registry-bridge-policy-rehearsal.json",
            "reports/echo-native/ashfall/resource-bridge-policy-rehearsal.json",
            "reports/echo-native/ashfall/test-process-boundary-verification.json",
            "reports/echo-native/ashfall/test-process-plan.json",
            "reports/echo-native/broken_pack/ai-graph.json",
            "reports/echo-native/broken_pack/ai-tasks.json",
            "reports/echo-native/broken_pack/boundary-failure-cases.json",
            "reports/echo-native/broken_pack/classloader-boundary-rehearsal.json",
            "reports/echo-native/broken_pack/classloader-boundary-plan.json",
            "reports/echo-native/broken_pack/classpath-classloader-compatibility.json",
            "reports/echo-native/broken_pack/classpath-builder-plan.json",
            "reports/echo-native/broken_pack/classpath-builder-safety-status.json",
            "reports/echo-native/broken_pack/classpath-source-policy.json",
            "reports/echo-native/broken_pack/native-extraction-plan.json",
            "reports/echo-native/broken_pack/native-extraction-safety-status.json",
            "reports/echo-native/broken_pack/native-extraction-source-policy.json",
            "reports/echo-native/broken_pack/launch-argument-builder-plan.json",
            "reports/echo-native/broken_pack/launch-argument-safety-status.json",
            "reports/echo-native/broken_pack/launch-argument-source-policy.json",
            "reports/echo-native/broken_pack/controlled-dummy-process-plan.json",
            "reports/echo-native/broken_pack/controlled-dummy-process-result.json",
            "reports/echo-native/broken_pack/dummy-process-crash-boundary.json",
            "reports/echo-native/broken_pack/dummy-process-output-capture.json",
            "reports/echo-native/broken_pack/addon-runtime-discovery-plan.json",
            "reports/echo-native/broken_pack/addon-runtime-descriptors.json",
            "reports/echo-native/broken_pack/addon-runtime-discovery-safety-status.json",
            "reports/echo-native/broken_pack/lifecycle-stub-execution-plan.json",
            "reports/echo-native/broken_pack/lifecycle-stub-execution-result.json",
            "reports/echo-native/broken_pack/lifecycle-stub-crash-boundary.json",
            "reports/echo-native/broken_pack/lifecycle-stub-safety-status.json",
            "reports/echo-native/broken_pack/service-bus-plan.json",
            "reports/echo-native/broken_pack/service-bus-registry.json",
            "reports/echo-native/broken_pack/service-bus-simulation-result.json",
            "reports/echo-native/broken_pack/service-bus-safety-status.json",
            "reports/echo-native/broken_pack/config-source-inventory.json",
            "reports/echo-native/broken_pack/config-validation-result.json",
            "reports/echo-native/broken_pack/config-write-plan.json",
            "reports/echo-native/broken_pack/config-safety-status.json",
            "reports/echo-native/broken_pack/resource-source-inventory.json",
            "reports/echo-native/broken_pack/resource-namespace-validation.json",
            "reports/echo-native/broken_pack/resource-pack-order-plan.json",
            "reports/echo-native/broken_pack/resource-conflict-report.json",
            "reports/echo-native/broken_pack/resource-bridge-safety-status.json",
            "reports/echo-native/broken_pack/registry-source-inventory.json",
            "reports/echo-native/broken_pack/registry-id-validation.json",
            "reports/echo-native/broken_pack/sandbox-registry-model.json",
            "reports/echo-native/broken_pack/registry-conflict-report.json",
            "reports/echo-native/broken_pack/registry-bridge-safety-status.json",
            "reports/echo-native/broken_pack/network-channel-inventory.json",
            "reports/echo-native/broken_pack/network-packet-validation.json",
            "reports/echo-native/broken_pack/network-schema-model.json",
            "reports/echo-native/broken_pack/network-conflict-report.json",
            "reports/echo-native/broken_pack/network-bridge-safety-status.json",
            "reports/echo-native/broken_pack/transform-source-inventory.json",
            "reports/echo-native/broken_pack/transform-allowlist-validation.json",
            "reports/echo-native/broken_pack/transform-pipeline-plan.json",
            "reports/echo-native/broken_pack/transform-conflict-report.json",
            "reports/echo-native/broken_pack/transform-safety-status.json",
            "reports/echo-native/broken_pack/crash-hardening-coverage.json",
            "reports/echo-native/broken_pack/failure-containment-matrix.json",
            "reports/echo-native/broken_pack/support-bundle-dry-run-plan.json",
            "reports/echo-native/broken_pack/phase13-m16-safety-status.json",
            "reports/echo-native/broken_pack/isolated-launch-environment-plan.json",
            "reports/echo-native/broken_pack/minecraft-launch-preflight.json",
            "reports/echo-native/broken_pack/launch-safety-gate.json",
            "reports/echo-native/broken_pack/controlled-launch-failure-capture-plan.json",
            "reports/echo-native/broken_pack/phase13-m17-readiness.json",
            "reports/echo-native/broken_pack/isolated-launch-attempt-plan.json",
            "reports/echo-native/broken_pack/local-runtime-artifact-check.json",
            "reports/echo-native/broken_pack/controlled-launch-attempt-result.json",
            "reports/echo-native/broken_pack/launch-output-capture.json",
            "reports/echo-native/broken_pack/phase13-m17-launch-status.json",
            "reports/echo-native/broken_pack/local-runtime-artifact-inventory.json",
            "reports/echo-native/broken_pack/local-runtime-artifact-map.json",
            "reports/echo-native/broken_pack/launch-artifact-resolution-status.json",
            "reports/echo-native/broken_pack/isolated-launch-execution-eligibility.json",
            "reports/echo-native/broken_pack/phase13-m17-artifact-readiness.json",
            "reports/echo-native/broken_pack/phase13-m17-artifact-blockers.json",
            "reports/echo-native/broken_pack/phase13-m17-blocker-resolution-plan.json",
            "reports/echo-native/broken_pack/phase13-m17-artifact-packaging-audit.json",
            "reports/echo-native/broken_pack/phase13-m17-artifact-packaging-resolution-plan.json",
            "reports/echo-native/broken_pack/phase13-m18-readiness.json",
            "reports/echo-native/broken_pack/runtime-fixture-presence.json",
            "reports/echo-native/broken_pack/runtime-fixture-mapping-readiness.json",
            "reports/echo-native/broken_pack/runtime-fixture-intake-plan.json",
            "reports/echo-native/broken_pack/runtime-fixture-intake-checklist.json",
            "reports/echo-native/broken_pack/runtime-fixture-approval-audit.json",
            "reports/echo-native/broken_pack/runtime-fixture-approval-template.json",
            "reports/echo-native/broken_pack/runtime-fixture-handoff.json",
            "reports/echo-native/broken_pack/runtime-fixture-validation-runbook.json",
            "reports/echo-native/broken_pack/runtime-fixture-approval-draft.json",
            "reports/echo-native/broken_pack/runtime-fixture-hash-review.json",
            "reports/echo-native/broken_pack/runtime-fixture-operator-packet.json",
            "reports/echo-native/broken_pack/runtime-fixture-integrity-audit.json",
            "reports/echo-native/broken_pack/runtime-fixture-integrity-manifest.json",
            "reports/echo-native/broken_pack/phase13-m17-completion.json",
            "reports/echo-native/broken_pack/phase13-m18-readiness-audit.json",
            "reports/echo-native/broken_pack/smoke-session-plan.json",
            "reports/echo-native/broken_pack/smoke-session-safety-gate.json",
            "reports/echo-native/broken_pack/smoke-session-result.json",
            "reports/echo-native/broken_pack/smoke-session-diagnostics.json",
            "reports/echo-native/broken_pack/phase13-m18-completion.json",
            "reports/echo-native/broken_pack/phase13-m19-readiness.json",
            "reports/echo-native/broken_pack/first-playtest-candidate-package.json",
            "reports/echo-native/broken_pack/first-playtest-support-bundle.json",
            "reports/echo-native/broken_pack/first-playtest-rollback-notes.json",
            "reports/echo-native/broken_pack/first-playtest-known-limitations.json",
            "reports/echo-native/broken_pack/experimental-native-loader-label.json",
            "reports/echo-native/broken_pack/first-playtest-crash-report-collection.json",
            "reports/echo-native/broken_pack/phase13-m19-completion.json",
            "reports/echo-native/broken_pack/first-playtest-open-gate.json",
            "reports/echo-native/broken_pack/phase13-first-playtest-blockers.json",
            "reports/echo-native/broken_pack/phase13-first-playtest-roadmap.json",
            "reports/echo-native/broken_pack/phase13-first-playtest-next-actions.json",
            "reports/echo-native/broken_pack/phase13-first-playtest-full-roadmap.json",
            "reports/echo-native/broken_pack/first-playtest-post-open-intake.json",
            "reports/echo-native/broken_pack/first-playtest-feedback-inventory.json",
            "reports/echo-native/broken_pack/first-playtest-waiting-checklist.json",
            "reports/echo-native/broken_pack/phase14-preflight-audit.json",
            "reports/echo-native/broken_pack/phase14-readiness.json",
            "reports/echo-native/broken_pack/phase14-next-actions.json",
            "reports/echo-native/broken_pack/native-loader-reality-audit.json",
            "reports/echo-native/broken_pack/native-loader-launch-command-classification.json",
            "reports/echo-native/broken_pack/native-loader-beta-implementation-next-actions.json",
            "reports/echo-native/broken_pack/isolated-runtime-workspace-plan.json",
            "reports/echo-native/broken_pack/isolated-runtime-workspace-materialization.json",
            "reports/echo-native/broken_pack/isolated-runtime-workspace-safety-status.json",
            "reports/echo-native/broken_pack/real-process-launch-harness-plan.json",
            "reports/echo-native/broken_pack/real-process-launch-safety-gate.json",
            "reports/echo-native/broken_pack/real-process-command-line-preview.json",
            "reports/echo-native/broken_pack/real-process-environment-plan.json",
            "reports/echo-native/broken_pack/process-execution-readiness.json",
            "reports/echo-native/broken_pack/controlled-launch-operator-checklist.json",
            "reports/echo-native/broken_pack/controlled-launch-rollback-plan.json",
            "reports/echo-native/broken_pack/phase13-native-loader-beta-gate.json",
            "reports/echo-native/broken_pack/native-loader-beta-feedback-inventory.json",
            "reports/echo-native/broken_pack/native-loader-beta-crash-intake.json",
            "reports/echo-native/broken_pack/native-loader-beta-known-issues.json",
            "reports/echo-native/broken_pack/native-loader-beta-next-action-queue.json",
            "reports/echo-native/broken_pack/phase13-m27-completion.json",
            "reports/echo-native/broken_pack/phase13-m28-readiness.json",
            "reports/echo-native/broken_pack/native-loader-beta-widening-plan.json",
            "reports/echo-native/broken_pack/native-loader-beta-widening-safety-gate.json",
            "reports/echo-native/broken_pack/beta-tester-cohort-plan.json",
            "reports/echo-native/broken_pack/phase13-m28-completion.json",
            "reports/echo-native/broken_pack/phase13-m29-readiness.json",
            "reports/echo-native/broken_pack/native-loader-beta-soak-plan.json",
            "reports/echo-native/broken_pack/native-loader-beta-session-inventory.json",
            "reports/echo-native/broken_pack/native-loader-beta-issue-triage.json",
            "reports/echo-native/broken_pack/native-loader-beta-regression-watchlist.json",
            "reports/echo-native/broken_pack/phase13-m29-completion.json",
            "reports/echo-native/broken_pack/phase13-m30-readiness.json",
            "reports/echo-native/broken_pack/native-loader-beta-soak-operator-packet.json",
            "reports/echo-native/broken_pack/native-loader-beta-session-template.json",
            "reports/echo-native/broken_pack/native-loader-beta-session-note-drafts.json",
            "reports/echo-native/broken_pack/native-loader-beta-session-draft-files.json",
            "reports/echo-native/broken_pack/native-loader-beta-session-note-validation.json",
            "reports/echo-native/broken_pack/native-loader-beta-evidence-checklist.json",
            "reports/echo-native/broken_pack/native-loader-beta-remaining-session-plan.json",
            "reports/echo-native/broken_pack/phase13-m29-soak-operator-status.json",
            "reports/echo-native/broken_pack/phase13-m29-session-draft-status.json",
            "reports/echo-native/broken_pack/phase13-m29-note-validation-status.json",
            "reports/echo-native/broken_pack/native-loader-beta-soak-status-dashboard.json",
            "reports/echo-native/broken_pack/native-loader-beta-next-session-checklist.json",
            "reports/echo-native/broken_pack/native-loader-beta-evidence-quality.json",
            "reports/echo-native/broken_pack/native-loader-beta-session-proof-matrix.json",
            "reports/echo-native/broken_pack/phase13-m29-evidence-gap.json",
            "reports/echo-native/broken_pack/native-loader-public-beta-candidate-audit.json",
            "reports/echo-native/broken_pack/native-loader-public-beta-safety-gate.json",
            "reports/echo-native/broken_pack/public-beta-tester-readiness.json",
            "reports/echo-native/broken_pack/phase13-m30-completion.json",
            "reports/echo-native/broken_pack/phase13-m31-readiness.json",
            "reports/echo-native/broken_pack/public-beta-opening-audit.json",
            "reports/echo-native/broken_pack/public-beta-safety-gate.json",
            "reports/echo-native/broken_pack/public-beta-tester-package-readiness.json",
            "reports/echo-native/broken_pack/public-beta-module-coverage.json",
            "reports/echo-native/broken_pack/public-beta-rollback-readiness.json",
            "reports/echo-native/broken_pack/public-beta-known-limitations.json",
            "reports/echo-native/broken_pack/phase13-m31-completion.json",
            "reports/echo-native/broken_pack/phase13-m32-readiness.json",
            "reports/echo-native/broken_pack/controlled-test-process-preflight.json",
            "reports/echo-native/broken_pack/crash-boundary-simulation-result.json",
            "reports/echo-native/broken_pack/crash-boundary-result.json",
            "reports/echo-native/broken_pack/crash-boundary-plan.json",
            "reports/echo-native/broken_pack/lifecycle-simulation-plan.json",
            "reports/echo-native/broken_pack/lifecycle-simulation-result.json",
            "reports/echo-native/broken_pack/loader-boundary-state-machine.json",
            "reports/echo-native/broken_pack/loader-boundary-verification.json",
            "reports/echo-native/broken_pack/phase12-completion.json",
            "reports/echo-native/broken_pack/phase13-readiness.json",
            "reports/echo-native/broken_pack/phase13-plan.json",
            "reports/echo-native/broken_pack/repair-plan.json",
            "reports/echo-native/broken_pack/crash-boundary-verification.json",
            "reports/echo-native/broken_pack/service-attach-simulation-result.json",
            "reports/echo-native/broken_pack/phase13-m1-safety-status.json",
            "reports/echo-native/broken_pack/phase13-bridge-safety-status.json",
            "reports/echo-native/broken_pack/phase13-m1-completion.json",
            "reports/echo-native/broken_pack/phase13-m2-readiness.json",
            "reports/echo-native/broken_pack/phase13-prototype-safety-gate.json",
            "reports/echo-native/broken_pack/minecraft-resolver-safety-status.json",
            "reports/echo-native/broken_pack/minecraft-version-resolver-plan.json",
            "reports/echo-native/broken_pack/minecraft-version-source-policy.json",
            "reports/echo-native/broken_pack/library-resolution-plan.json",
            "reports/echo-native/broken_pack/library-resolver-safety-status.json",
            "reports/echo-native/broken_pack/library-source-policy.json",
            "reports/echo-native/broken_pack/registry-bridge-policy-rehearsal.json",
            "reports/echo-native/broken_pack/resource-bridge-policy-rehearsal.json",
            "reports/echo-native/broken_pack/test-process-boundary-verification.json",
            "reports/echo-native/broken_pack/test-process-plan.json"
    );

    public static void main(String[] args) throws Exception {
        EchoNativeReportSnapshotVerifier verifier = new EchoNativeReportSnapshotVerifier();
        verifier.verify();
    }

    private void verify() throws Exception {
        runCli(0, "phase12", "verify", "fixtures/ashfall");
        runCli(1, "phase12", "verify", "fixtures/broken-pack");
        runCli(0, "phase13", "plan", "fixtures/ashfall");
        runCli(1, "phase13", "plan", "fixtures/broken-pack");
        runCli(0, "phase13", "simulate", "lifecycle", "fixtures/ashfall");
        runCli(1, "phase13", "simulate", "lifecycle", "fixtures/broken-pack");
        runCli(0, "phase13", "simulate", "services", "fixtures/ashfall");
        runCli(1, "phase13", "simulate", "services", "fixtures/broken-pack");
        runCli(0, "phase13", "simulate", "crash-boundary", "fixtures/ashfall");
        runCli(1, "phase13", "simulate", "crash-boundary", "fixtures/broken-pack");
        runCli(0, "phase13", "verify", "boundaries", "fixtures/ashfall");
        runCli(1, "phase13", "verify", "boundaries", "fixtures/broken-pack");
        runCli(0, "phase13", "verify", "test-process", "fixtures/ashfall");
        runCli(1, "phase13", "verify", "test-process", "fixtures/broken-pack");
        runCli(0, "phase13", "rehearse", "bridges", "fixtures/ashfall");
        runCli(1, "phase13", "rehearse", "bridges", "fixtures/broken-pack");
        runCli(0, "phase13", "verify", "m1", "fixtures/ashfall");
        runCli(1, "phase13", "verify", "m1", "fixtures/broken-pack");
        runCli(0, "phase13", "plan", "minecraft-resolver", "fixtures/ashfall");
        runCli(1, "phase13", "plan", "minecraft-resolver", "fixtures/broken-pack");
        runCli(0, "phase13", "plan", "library-resolver", "fixtures/ashfall");
        runCli(1, "phase13", "plan", "library-resolver", "fixtures/broken-pack");
        runCli(0, "phase13", "plan", "classpath", "fixtures/ashfall");
        runCli(1, "phase13", "plan", "classpath", "fixtures/broken-pack");
        runCli(0, "phase13", "plan", "native-extraction", "fixtures/ashfall");
        runCli(1, "phase13", "plan", "native-extraction", "fixtures/broken-pack");
        runCli(0, "phase13", "plan", "launch-arguments", "fixtures/ashfall");
        runCli(1, "phase13", "plan", "launch-arguments", "fixtures/broken-pack");
        runCli(0, "phase13", "run", "dummy-process", "fixtures/ashfall");
        runCli(1, "phase13", "run", "dummy-process", "fixtures/broken-pack");
        runCli(0, "phase13", "discover", "addons", "fixtures/ashfall");
        runCli(1, "phase13", "discover", "addons", "fixtures/broken-pack");
        runCli(0, "phase13", "execute", "lifecycle-stubs", "fixtures/ashfall");
        runCli(1, "phase13", "execute", "lifecycle-stubs", "fixtures/broken-pack");
        runCli(0, "phase13", "prototype", "service-bus", "fixtures/ashfall");
        runCli(1, "phase13", "prototype", "service-bus", "fixtures/broken-pack");
        runCli(0, "phase13", "prototype", "config", "fixtures/ashfall");
        runCli(1, "phase13", "prototype", "config", "fixtures/broken-pack");
        runCli(0, "phase13", "prototype", "resources", "fixtures/ashfall");
        runCli(1, "phase13", "prototype", "resources", "fixtures/broken-pack");
        runCli(0, "phase13", "prototype", "registry", "fixtures/ashfall");
        runCli(1, "phase13", "prototype", "registry", "fixtures/broken-pack");
        runCli(0, "phase13", "prototype", "network", "fixtures/ashfall");
        runCli(1, "phase13", "prototype", "network", "fixtures/broken-pack");
        runCli(0, "phase13", "prototype", "transforms", "fixtures/ashfall");
        runCli(1, "phase13", "prototype", "transforms", "fixtures/broken-pack");
        runCli(0, "phase13", "verify", "crash-hardening", "fixtures/ashfall");
        runCli(1, "phase13", "verify", "crash-hardening", "fixtures/broken-pack");
        runCli(0, "phase13", "launch", "preflight", "fixtures/ashfall");
        runCli(1, "phase13", "launch", "preflight", "fixtures/broken-pack");
        runCli(1, "phase13", "inventory", "artifacts", "fixtures/ashfall");
        runCli(1, "phase13", "inventory", "artifacts", "fixtures/broken-pack");
        runCli(0, "phase13", "map", "artifacts", "fixtures/ashfall");
        runCli(1, "phase13", "map", "artifacts", "fixtures/broken-pack");
        runCli(0, "phase13", "launch", "attempt", "--isolated", "fixtures/ashfall");
        runCli(1, "phase13", "launch", "attempt", "--isolated", "fixtures/broken-pack");
        runCli(0, "phase13", "verify", "artifact-blockers", "fixtures/ashfall");
        runCli(1, "phase13", "verify", "artifact-blockers", "fixtures/broken-pack");
        runCli(0, "phase13", "audit", "artifact-packaging", "fixtures/ashfall");
        runCli(1, "phase13", "audit", "artifact-packaging", "fixtures/broken-pack");
        runCli(0, "phase13", "verify", "runtime-fixtures", "fixtures/ashfall");
        runCli(1, "phase13", "verify", "runtime-fixtures", "fixtures/broken-pack");
        runCli(0, "phase13", "plan", "runtime-fixture-intake", "fixtures/ashfall");
        runCli(1, "phase13", "plan", "runtime-fixture-intake", "fixtures/broken-pack");
        runCli(0, "phase13", "audit", "runtime-fixture-approval", "fixtures/ashfall");
        runCli(1, "phase13", "audit", "runtime-fixture-approval", "fixtures/broken-pack");
        runCli(0, "phase13", "prepare", "runtime-fixture-handoff", "fixtures/ashfall");
        runCli(1, "phase13", "prepare", "runtime-fixture-handoff", "fixtures/broken-pack");
        runCli(0, "phase13", "draft", "runtime-fixture-approval", "fixtures/ashfall");
        runCli(1, "phase13", "draft", "runtime-fixture-approval", "fixtures/broken-pack");
        runCli(0, "phase13", "audit", "runtime-fixture-integrity", "fixtures/ashfall");
        runCli(1, "phase13", "audit", "runtime-fixture-integrity", "fixtures/broken-pack");
        runCli(0, "phase13", "verify", "m17", "fixtures/ashfall");
        runCli(1, "phase13", "verify", "m17", "fixtures/broken-pack");
        runCli(0, "phase13", "verify", "m18", "fixtures/ashfall");
        runCli(1, "phase13", "verify", "m18", "fixtures/broken-pack");
        runCli(0, "phase13", "package", "first-playtest", "fixtures/ashfall");
        runCli(1, "phase13", "package", "first-playtest", "fixtures/broken-pack");
        runCli(0, "phase13", "plan", "first-playtest", "fixtures/ashfall");
        runCli(1, "phase13", "plan", "first-playtest", "fixtures/broken-pack");
        runCli(0, "phase14", "preflight", "fixtures/ashfall");
        runCli(1, "phase14", "preflight", "fixtures/broken-pack");
        runCli(0, "phase13", "audit", "launch-reality", "fixtures/ashfall");
        runCli(1, "phase13", "audit", "launch-reality", "fixtures/broken-pack");
        runCli(0, "phase13", "prepare", "isolated-runtime", "fixtures/ashfall");
        runCli(1, "phase13", "prepare", "isolated-runtime", "fixtures/broken-pack");
        runCli(0, "phase13", "plan", "real-process-launch", "fixtures/ashfall");
        runCli(1, "phase13", "plan", "real-process-launch", "fixtures/broken-pack");
        runCli(1, "phase13", "verify", "execution-readiness", "fixtures/ashfall");
        runCli(1, "phase13", "verify", "execution-readiness", "fixtures/broken-pack");
        runCli(0, "phase13", "export", "runtime-fixture-operator-packet", "fixtures/ashfall");
        runCli(1, "phase13", "export", "runtime-fixture-operator-packet", "fixtures/broken-pack");
        runBetaSnapshotCommands();
        Map<String, String> first = snapshot();

        runCli(0, "phase12", "verify", "fixtures/ashfall");
        runCli(1, "phase12", "verify", "fixtures/broken-pack");
        runCli(0, "phase13", "plan", "fixtures/ashfall");
        runCli(1, "phase13", "plan", "fixtures/broken-pack");
        runCli(0, "phase13", "simulate", "lifecycle", "fixtures/ashfall");
        runCli(1, "phase13", "simulate", "lifecycle", "fixtures/broken-pack");
        runCli(0, "phase13", "simulate", "services", "fixtures/ashfall");
        runCli(1, "phase13", "simulate", "services", "fixtures/broken-pack");
        runCli(0, "phase13", "simulate", "crash-boundary", "fixtures/ashfall");
        runCli(1, "phase13", "simulate", "crash-boundary", "fixtures/broken-pack");
        runCli(0, "phase13", "verify", "boundaries", "fixtures/ashfall");
        runCli(1, "phase13", "verify", "boundaries", "fixtures/broken-pack");
        runCli(0, "phase13", "verify", "test-process", "fixtures/ashfall");
        runCli(1, "phase13", "verify", "test-process", "fixtures/broken-pack");
        runCli(0, "phase13", "rehearse", "bridges", "fixtures/ashfall");
        runCli(1, "phase13", "rehearse", "bridges", "fixtures/broken-pack");
        runCli(0, "phase13", "verify", "m1", "fixtures/ashfall");
        runCli(1, "phase13", "verify", "m1", "fixtures/broken-pack");
        runCli(0, "phase13", "plan", "minecraft-resolver", "fixtures/ashfall");
        runCli(1, "phase13", "plan", "minecraft-resolver", "fixtures/broken-pack");
        runCli(0, "phase13", "plan", "library-resolver", "fixtures/ashfall");
        runCli(1, "phase13", "plan", "library-resolver", "fixtures/broken-pack");
        runCli(0, "phase13", "plan", "classpath", "fixtures/ashfall");
        runCli(1, "phase13", "plan", "classpath", "fixtures/broken-pack");
        runCli(0, "phase13", "plan", "native-extraction", "fixtures/ashfall");
        runCli(1, "phase13", "plan", "native-extraction", "fixtures/broken-pack");
        runCli(0, "phase13", "plan", "launch-arguments", "fixtures/ashfall");
        runCli(1, "phase13", "plan", "launch-arguments", "fixtures/broken-pack");
        runCli(0, "phase13", "run", "dummy-process", "fixtures/ashfall");
        runCli(1, "phase13", "run", "dummy-process", "fixtures/broken-pack");
        runCli(0, "phase13", "discover", "addons", "fixtures/ashfall");
        runCli(1, "phase13", "discover", "addons", "fixtures/broken-pack");
        runCli(0, "phase13", "execute", "lifecycle-stubs", "fixtures/ashfall");
        runCli(1, "phase13", "execute", "lifecycle-stubs", "fixtures/broken-pack");
        runCli(0, "phase13", "prototype", "service-bus", "fixtures/ashfall");
        runCli(1, "phase13", "prototype", "service-bus", "fixtures/broken-pack");
        runCli(0, "phase13", "prototype", "config", "fixtures/ashfall");
        runCli(1, "phase13", "prototype", "config", "fixtures/broken-pack");
        runCli(0, "phase13", "prototype", "resources", "fixtures/ashfall");
        runCli(1, "phase13", "prototype", "resources", "fixtures/broken-pack");
        runCli(0, "phase13", "prototype", "registry", "fixtures/ashfall");
        runCli(1, "phase13", "prototype", "registry", "fixtures/broken-pack");
        runCli(0, "phase13", "prototype", "network", "fixtures/ashfall");
        runCli(1, "phase13", "prototype", "network", "fixtures/broken-pack");
        runCli(0, "phase13", "prototype", "transforms", "fixtures/ashfall");
        runCli(1, "phase13", "prototype", "transforms", "fixtures/broken-pack");
        runCli(0, "phase13", "verify", "crash-hardening", "fixtures/ashfall");
        runCli(1, "phase13", "verify", "crash-hardening", "fixtures/broken-pack");
        runCli(0, "phase13", "launch", "preflight", "fixtures/ashfall");
        runCli(1, "phase13", "launch", "preflight", "fixtures/broken-pack");
        runCli(1, "phase13", "inventory", "artifacts", "fixtures/ashfall");
        runCli(1, "phase13", "inventory", "artifacts", "fixtures/broken-pack");
        runCli(0, "phase13", "map", "artifacts", "fixtures/ashfall");
        runCli(1, "phase13", "map", "artifacts", "fixtures/broken-pack");
        runCli(0, "phase13", "launch", "attempt", "--isolated", "fixtures/ashfall");
        runCli(1, "phase13", "launch", "attempt", "--isolated", "fixtures/broken-pack");
        runCli(0, "phase13", "verify", "artifact-blockers", "fixtures/ashfall");
        runCli(1, "phase13", "verify", "artifact-blockers", "fixtures/broken-pack");
        runCli(0, "phase13", "audit", "artifact-packaging", "fixtures/ashfall");
        runCli(1, "phase13", "audit", "artifact-packaging", "fixtures/broken-pack");
        runCli(0, "phase13", "verify", "runtime-fixtures", "fixtures/ashfall");
        runCli(1, "phase13", "verify", "runtime-fixtures", "fixtures/broken-pack");
        runCli(0, "phase13", "plan", "runtime-fixture-intake", "fixtures/ashfall");
        runCli(1, "phase13", "plan", "runtime-fixture-intake", "fixtures/broken-pack");
        runCli(0, "phase13", "audit", "runtime-fixture-approval", "fixtures/ashfall");
        runCli(1, "phase13", "audit", "runtime-fixture-approval", "fixtures/broken-pack");
        runCli(0, "phase13", "prepare", "runtime-fixture-handoff", "fixtures/ashfall");
        runCli(1, "phase13", "prepare", "runtime-fixture-handoff", "fixtures/broken-pack");
        runCli(0, "phase13", "draft", "runtime-fixture-approval", "fixtures/ashfall");
        runCli(1, "phase13", "draft", "runtime-fixture-approval", "fixtures/broken-pack");
        runCli(0, "phase13", "audit", "runtime-fixture-integrity", "fixtures/ashfall");
        runCli(1, "phase13", "audit", "runtime-fixture-integrity", "fixtures/broken-pack");
        runCli(0, "phase13", "verify", "m17", "fixtures/ashfall");
        runCli(1, "phase13", "verify", "m17", "fixtures/broken-pack");
        runCli(0, "phase13", "verify", "m18", "fixtures/ashfall");
        runCli(1, "phase13", "verify", "m18", "fixtures/broken-pack");
        runCli(0, "phase13", "package", "first-playtest", "fixtures/ashfall");
        runCli(1, "phase13", "package", "first-playtest", "fixtures/broken-pack");
        runCli(0, "phase13", "plan", "first-playtest", "fixtures/ashfall");
        runCli(1, "phase13", "plan", "first-playtest", "fixtures/broken-pack");
        runCli(0, "phase14", "preflight", "fixtures/ashfall");
        runCli(1, "phase14", "preflight", "fixtures/broken-pack");
        runCli(0, "phase13", "audit", "launch-reality", "fixtures/ashfall");
        runCli(1, "phase13", "audit", "launch-reality", "fixtures/broken-pack");
        runCli(0, "phase13", "prepare", "isolated-runtime", "fixtures/ashfall");
        runCli(1, "phase13", "prepare", "isolated-runtime", "fixtures/broken-pack");
        runCli(0, "phase13", "plan", "real-process-launch", "fixtures/ashfall");
        runCli(1, "phase13", "plan", "real-process-launch", "fixtures/broken-pack");
        runCli(1, "phase13", "verify", "execution-readiness", "fixtures/ashfall");
        runCli(1, "phase13", "verify", "execution-readiness", "fixtures/broken-pack");
        runCli(0, "phase13", "export", "runtime-fixture-operator-packet", "fixtures/ashfall");
        runCli(1, "phase13", "export", "runtime-fixture-operator-packet", "fixtures/broken-pack");
        runBetaSnapshotCommands();
        Map<String, String> second = snapshot();

        if (!first.equals(second)) {
            throw new IllegalStateException("Native dry-run report snapshots are not deterministic across repeated generation. Mismatched reports: "
                    + mismatchedReports(first, second));
        }
        verifyEnvelope("reports/echo-native/ashfall/ai-graph.json", "PASS", 0, true);
        verifyEnvelope("reports/echo-native/ashfall/ai-tasks.json", "PASS", 0, true);
        verifyPhase12Gate("reports/echo-native/ashfall/phase12-completion.json", true);
        verifyPhase13Readiness("reports/echo-native/ashfall/phase13-readiness.json", true);
        verifyPhase13Plan("reports/echo-native/ashfall/phase13-plan.json", "PASS", true);
        verifyLifecycleSimulation("reports/echo-native/ashfall/lifecycle-simulation-result.json", "PASS", true);
        verifyServiceSimulation("reports/echo-native/ashfall/service-attach-simulation-result.json", "PASS", true);
        verifyCrashBoundaryVerification("reports/echo-native/ashfall/crash-boundary-verification.json", "PASS", true);
        verifyCrashBoundarySimulation("reports/echo-native/ashfall/crash-boundary-simulation-result.json", "PASS", true);
        verifyBoundaryFailureCases("reports/echo-native/ashfall/boundary-failure-cases.json", "PASS", true);
        verifyClassloaderBoundaryRehearsal("reports/echo-native/ashfall/classloader-boundary-rehearsal.json", "PASS", true);
        verifyBoundaryStateMachine("reports/echo-native/ashfall/loader-boundary-state-machine.json", "PASS", true);
        verifyBoundaryVerification("reports/echo-native/ashfall/loader-boundary-verification.json", "PASS", true);
        verifyClasspathClassloaderCompatibility("reports/echo-native/ashfall/classpath-classloader-compatibility.json", "PASS", true);
        verifyTestProcessBoundary("reports/echo-native/ashfall/test-process-boundary-verification.json", "PASS", true);
        verifyControlledTestProcessPreflight("reports/echo-native/ashfall/controlled-test-process-preflight.json", "PASS", true);
        verifyPhase13M1SafetyStatus("reports/echo-native/ashfall/phase13-m1-safety-status.json", "PASS", true);
        verifyResourceBridgePolicy("reports/echo-native/ashfall/resource-bridge-policy-rehearsal.json", "PASS", true);
        verifyRegistryBridgePolicy("reports/echo-native/ashfall/registry-bridge-policy-rehearsal.json", "PASS", true);
        verifyPhase13BridgeSafetyStatus("reports/echo-native/ashfall/phase13-bridge-safety-status.json", "PASS", true);
        verifyPhase13M1Completion("reports/echo-native/ashfall/phase13-m1-completion.json", "PASS", true);
        verifyPhase13M2Readiness("reports/echo-native/ashfall/phase13-m2-readiness.json", "PASS", true);
        verifyPhase13PrototypeSafetyGate("reports/echo-native/ashfall/phase13-prototype-safety-gate.json", "PASS", true);
        verifyMinecraftVersionResolverPlan("reports/echo-native/ashfall/minecraft-version-resolver-plan.json", "PASS", true);
        verifyMinecraftVersionSourcePolicy("reports/echo-native/ashfall/minecraft-version-source-policy.json", "PASS", true);
        verifyMinecraftResolverSafetyStatus("reports/echo-native/ashfall/minecraft-resolver-safety-status.json", "PASS", true);
        verifyLibraryResolutionPlan("reports/echo-native/ashfall/library-resolution-plan.json", "PASS", true);
        verifyLibrarySourcePolicy("reports/echo-native/ashfall/library-source-policy.json", "PASS", true);
        verifyLibraryResolverSafetyStatus("reports/echo-native/ashfall/library-resolver-safety-status.json", "PASS", true);
        verifyClasspathBuilderPlan("reports/echo-native/ashfall/classpath-builder-plan.json", "PASS", true);
        verifyClasspathSourcePolicy("reports/echo-native/ashfall/classpath-source-policy.json", "PASS", true);
        verifyClasspathBuilderSafetyStatus("reports/echo-native/ashfall/classpath-builder-safety-status.json", "PASS", true);
        verifyNativeExtractionPlan("reports/echo-native/ashfall/native-extraction-plan.json", "PASS", true);
        verifyNativeExtractionSourcePolicy("reports/echo-native/ashfall/native-extraction-source-policy.json", "PASS", true);
        verifyNativeExtractionSafetyStatus("reports/echo-native/ashfall/native-extraction-safety-status.json", "PASS", true);
        verifyLaunchArgumentPlan("reports/echo-native/ashfall/launch-argument-builder-plan.json", "PASS", true);
        verifyLaunchArgumentSourcePolicy("reports/echo-native/ashfall/launch-argument-source-policy.json", "PASS", true);
        verifyLaunchArgumentSafetyStatus("reports/echo-native/ashfall/launch-argument-safety-status.json", "PASS", true);
        verifyControlledDummyProcessPlan("reports/echo-native/ashfall/controlled-dummy-process-plan.json", "PASS", true);
        verifyControlledDummyProcessResult("reports/echo-native/ashfall/controlled-dummy-process-result.json", "PASS", true);
        verifyDummyProcessCrashBoundary("reports/echo-native/ashfall/dummy-process-crash-boundary.json", "PASS", true);
        verifyDummyProcessOutputCapture("reports/echo-native/ashfall/dummy-process-output-capture.json", "PASS", true);
        verifyAddonRuntimeDiscoveryPlan("reports/echo-native/ashfall/addon-runtime-discovery-plan.json", "PASS", true);
        verifyAddonRuntimeDescriptors("reports/echo-native/ashfall/addon-runtime-descriptors.json", "PASS", true);
        verifyAddonRuntimeDiscoverySafetyStatus("reports/echo-native/ashfall/addon-runtime-discovery-safety-status.json", "PASS", true);
        verifyLifecycleStubExecutionPlan("reports/echo-native/ashfall/lifecycle-stub-execution-plan.json", "PASS", true);
        verifyLifecycleStubExecutionResult("reports/echo-native/ashfall/lifecycle-stub-execution-result.json", "PASS", true);
        verifyLifecycleStubCrashBoundary("reports/echo-native/ashfall/lifecycle-stub-crash-boundary.json", "PASS", true);
        verifyLifecycleStubSafetyStatus("reports/echo-native/ashfall/lifecycle-stub-safety-status.json", "PASS", true);
        verifyServiceBusPlan("reports/echo-native/ashfall/service-bus-plan.json", "PASS", true);
        verifyServiceBusRegistry("reports/echo-native/ashfall/service-bus-registry.json", "PASS", true);
        verifyServiceBusSimulationResult("reports/echo-native/ashfall/service-bus-simulation-result.json", "PASS", true);
        verifyServiceBusSafetyStatus("reports/echo-native/ashfall/service-bus-safety-status.json", "PASS", true);
        verifyConfigSourceInventory("reports/echo-native/ashfall/config-source-inventory.json", "PASS", true);
        verifyConfigValidationResult("reports/echo-native/ashfall/config-validation-result.json", "PASS", true);
        verifyConfigWritePlan("reports/echo-native/ashfall/config-write-plan.json", "PASS", true);
        verifyConfigSafetyStatus("reports/echo-native/ashfall/config-safety-status.json", "PASS", true);
        verifyResourceSourceInventory("reports/echo-native/ashfall/resource-source-inventory.json", "PASS", true);
        verifyResourceNamespaceValidation("reports/echo-native/ashfall/resource-namespace-validation.json", "PASS", true);
        verifyResourcePackOrderPlan("reports/echo-native/ashfall/resource-pack-order-plan.json", "PASS", true);
        verifyResourceConflictReport("reports/echo-native/ashfall/resource-conflict-report.json", "PASS", true);
        verifyResourceBridgeSafetyStatus("reports/echo-native/ashfall/resource-bridge-safety-status.json", "PASS", true);
        verifyRegistrySourceInventory("reports/echo-native/ashfall/registry-source-inventory.json", "PASS", true);
        verifyRegistryIdValidation("reports/echo-native/ashfall/registry-id-validation.json", "PASS", true);
        verifySandboxRegistryModel("reports/echo-native/ashfall/sandbox-registry-model.json", "PASS", true);
        verifyRegistryConflictReport("reports/echo-native/ashfall/registry-conflict-report.json", "PASS", true);
        verifyRegistryBridgeSafetyStatus("reports/echo-native/ashfall/registry-bridge-safety-status.json", "PASS", true);
        verifyNetworkChannelInventory("reports/echo-native/ashfall/network-channel-inventory.json", "PASS", true);
        verifyNetworkPacketValidation("reports/echo-native/ashfall/network-packet-validation.json", "PASS", true);
        verifyNetworkSchemaModel("reports/echo-native/ashfall/network-schema-model.json", "PASS", true);
        verifyNetworkConflictReport("reports/echo-native/ashfall/network-conflict-report.json", "PASS", true);
        verifyNetworkBridgeSafetyStatus("reports/echo-native/ashfall/network-bridge-safety-status.json", "PASS", true);
        verifyTransformSourceInventory("reports/echo-native/ashfall/transform-source-inventory.json", "PASS", true);
        verifyTransformAllowlistValidation("reports/echo-native/ashfall/transform-allowlist-validation.json", "PASS", true);
        verifyTransformPipelinePlan("reports/echo-native/ashfall/transform-pipeline-plan.json", "PASS", true);
        verifyTransformConflictReport("reports/echo-native/ashfall/transform-conflict-report.json", "PASS", true);
        verifyTransformSafetyStatus("reports/echo-native/ashfall/transform-safety-status.json", "PASS", true);
        verifyCrashHardeningCoverage("reports/echo-native/ashfall/crash-hardening-coverage.json", "PASS", true);
        verifyFailureContainmentMatrix("reports/echo-native/ashfall/failure-containment-matrix.json", "PASS", true);
        verifySupportBundleDryRunPlan("reports/echo-native/ashfall/support-bundle-dry-run-plan.json", "PASS", true);
        verifyPhase13M16SafetyStatus("reports/echo-native/ashfall/phase13-m16-safety-status.json", "PASS", true);
        verifyIsolatedLaunchEnvironmentPlan("reports/echo-native/ashfall/isolated-launch-environment-plan.json", "PASS", true);
        verifyMinecraftLaunchPreflight("reports/echo-native/ashfall/minecraft-launch-preflight.json", "PASS", true);
        verifyLaunchSafetyGate("reports/echo-native/ashfall/launch-safety-gate.json", "PASS", true);
        verifyControlledLaunchFailureCapturePlan("reports/echo-native/ashfall/controlled-launch-failure-capture-plan.json", "PASS", true);
        verifyPhase13M17Readiness("reports/echo-native/ashfall/phase13-m17-readiness.json", "PASS", true);
        verifyIsolatedLaunchAttemptPlan("reports/echo-native/ashfall/isolated-launch-attempt-plan.json", "PASS", false);
        verifyLocalRuntimeArtifactCheck("reports/echo-native/ashfall/local-runtime-artifact-check.json", "PASS", true);
        verifyControlledLaunchAttemptResult("reports/echo-native/ashfall/controlled-launch-attempt-result.json", "PASS", false);
        verifyLaunchOutputCapture("reports/echo-native/ashfall/launch-output-capture.json", "PASS");
        verifyPhase13M17LaunchStatus("reports/echo-native/ashfall/phase13-m17-launch-status.json", "PASS", false);
        verifyLocalRuntimeArtifactInventory("reports/echo-native/ashfall/local-runtime-artifact-inventory.json", "FAILED", true);
        verifyLocalRuntimeArtifactMap("reports/echo-native/ashfall/local-runtime-artifact-map.json", "PASS", true);
        verifyLaunchArtifactResolutionStatus("reports/echo-native/ashfall/launch-artifact-resolution-status.json", "PASS", true);
        verifyIsolatedLaunchExecutionEligibility("reports/echo-native/ashfall/isolated-launch-execution-eligibility.json", "PASS", true);
        verifyPhase13M17ArtifactReadiness("reports/echo-native/ashfall/phase13-m17-artifact-readiness.json", "PASS", true);
        verifyPhase13M17ArtifactBlockers("reports/echo-native/ashfall/phase13-m17-artifact-blockers.json", "PASS", 0, true);
        verifyPhase13M17BlockerResolutionPlan("reports/echo-native/ashfall/phase13-m17-blocker-resolution-plan.json", "PASS", 0, true);
        verifyPhase13M17ArtifactPackagingAudit("reports/echo-native/ashfall/phase13-m17-artifact-packaging-audit.json", "PASS", 0, true);
        verifyPhase13M17ArtifactPackagingResolutionPlan("reports/echo-native/ashfall/phase13-m17-artifact-packaging-resolution-plan.json", "PASS", 0, true);
        verifyPhase13M18Readiness("reports/echo-native/ashfall/phase13-m18-readiness.json", "PASS", true, 0);
        verifyRuntimeFixturePresence("reports/echo-native/ashfall/runtime-fixture-presence.json", "PASS", true, 2, 0);
        verifyRuntimeFixtureMappingReadiness("reports/echo-native/ashfall/runtime-fixture-mapping-readiness.json", "PASS", true, 2, 2);
        verifyRuntimeFixtureIntakePlan("reports/echo-native/ashfall/runtime-fixture-intake-plan.json", "PASS", true, 2, 2);
        verifyRuntimeFixtureIntakeChecklist("reports/echo-native/ashfall/runtime-fixture-intake-checklist.json", "PASS", true, 2, 2);
        verifyRuntimeFixtureApprovalAudit("reports/echo-native/ashfall/runtime-fixture-approval-audit.json", "PASS", true, 2, 2);
        verifyRuntimeFixtureApprovalTemplate("reports/echo-native/ashfall/runtime-fixture-approval-template.json", "PASS", true, 2);
        verifyRuntimeFixtureHandoff("reports/echo-native/ashfall/runtime-fixture-handoff.json", "PASS", true, 2);
        verifyRuntimeFixtureValidationRunbook("reports/echo-native/ashfall/runtime-fixture-validation-runbook.json", "PASS", true, 2);
        verifyRuntimeFixtureApprovalDraft("reports/echo-native/ashfall/runtime-fixture-approval-draft.json", "PASS", true, 2, 2);
        verifyRuntimeFixtureHashReview("reports/echo-native/ashfall/runtime-fixture-hash-review.json", "PASS", true, 2, 2);
        verifyRuntimeFixtureIntegrityAudit("reports/echo-native/ashfall/runtime-fixture-integrity-audit.json", "PASS", true, 2, 2);
        verifyRuntimeFixtureIntegrityManifest("reports/echo-native/ashfall/runtime-fixture-integrity-manifest.json", "PASS", true, 2);
        verifyPhase13M17Completion("reports/echo-native/ashfall/phase13-m17-completion.json", "PASS", true, 0);
        verifyPhase13M18ReadinessAudit("reports/echo-native/ashfall/phase13-m18-readiness-audit.json", "PASS", true, 0);
        verifySmokeSessionPlan("reports/echo-native/ashfall/smoke-session-plan.json", "PASS", true);
        verifySmokeSessionSafetyGate("reports/echo-native/ashfall/smoke-session-safety-gate.json", "PASS", true);
        verifySmokeSessionResult("reports/echo-native/ashfall/smoke-session-result.json", "PASS", true);
        verifySmokeSessionDiagnostics("reports/echo-native/ashfall/smoke-session-diagnostics.json", "PASS", true);
        verifyPhase13M18Completion("reports/echo-native/ashfall/phase13-m18-completion.json", "PASS", true, true);
        verifyPhase13M19Readiness("reports/echo-native/ashfall/phase13-m19-readiness.json", "PASS", true);
        verifyFirstPlaytestCandidatePackage("reports/echo-native/ashfall/first-playtest-candidate-package.json", "PASS", true);
        verifyFirstPlaytestSupportBundle("reports/echo-native/ashfall/first-playtest-support-bundle.json", "PASS", true);
        verifyFirstPlaytestRollbackNotes("reports/echo-native/ashfall/first-playtest-rollback-notes.json", "PASS", true);
        verifyFirstPlaytestKnownLimitations("reports/echo-native/ashfall/first-playtest-known-limitations.json", "PASS", true);
        verifyExperimentalNativeLoaderLabel("reports/echo-native/ashfall/experimental-native-loader-label.json", "PASS", true);
        verifyFirstPlaytestCrashReportCollection("reports/echo-native/ashfall/first-playtest-crash-report-collection.json", "PASS", true);
        verifyPhase13M19Completion("reports/echo-native/ashfall/phase13-m19-completion.json", "PASS", true);
        verifyFirstPlaytestOpenGate("reports/echo-native/ashfall/first-playtest-open-gate.json", "PASS", true);
        verifyPhase13FirstPlaytestBlockers("reports/echo-native/ashfall/phase13-first-playtest-blockers.json", "PASS", false, 0);
        verifyPhase13FirstPlaytestRoadmap("reports/echo-native/ashfall/phase13-first-playtest-roadmap.json", "PASS", true, true, 0);
        verifyPhase13FirstPlaytestNextActions("reports/echo-native/ashfall/phase13-first-playtest-next-actions.json", "PASS", 0);
        verifyPhase13FirstPlaytestFullRoadmap("reports/echo-native/ashfall/phase13-first-playtest-full-roadmap.json", "PASS", 18, 18, "");
        verifyPhase14Preflight("reports/echo-native/ashfall/first-playtest-post-open-intake.json", "PASS_WITH_WARNINGS", false, 0);
        verifyPhase14Preflight("reports/echo-native/ashfall/first-playtest-feedback-inventory.json", "PASS_WITH_WARNINGS", false, 0);
        verifyPhase14Preflight("reports/echo-native/ashfall/first-playtest-waiting-checklist.json", "PASS_WITH_WARNINGS", false, 3);
        verifyPhase14Preflight("reports/echo-native/ashfall/phase14-preflight-audit.json", "PASS_WITH_WARNINGS", false, 0);
        verifyPhase14Preflight("reports/echo-native/ashfall/phase14-readiness.json", "PASS_WITH_WARNINGS", false, 0);
        verifyPhase14Preflight("reports/echo-native/ashfall/phase14-next-actions.json", "PASS_WITH_WARNINGS", false, 3);
        verifyLaunchRealityAudit("reports/echo-native/ashfall/native-loader-reality-audit.json", "PASS_WITH_WARNINGS", true, true, false, 0);
        verifyLaunchCommandClassification("reports/echo-native/ashfall/native-loader-launch-command-classification.json", "PASS_WITH_WARNINGS", true, false, 5);
        verifyNativeLoaderBetaNextActions("reports/echo-native/ashfall/native-loader-beta-implementation-next-actions.json", "PASS_WITH_WARNINGS", false, 3);
        verifyIsolatedRuntimeWorkspace("reports/echo-native/ashfall/isolated-runtime-workspace-plan.json", "PASS", true, true, 7, 0);
        verifyIsolatedRuntimeWorkspace("reports/echo-native/ashfall/isolated-runtime-workspace-materialization.json", "PASS", true, true, 7, 0);
        verifyIsolatedRuntimeWorkspace("reports/echo-native/ashfall/isolated-runtime-workspace-safety-status.json", "PASS", true, true, 7, 0);
        int expectedAshfallClasspathEntryCount = expectedAshfallClasspathEntryCount();
        int expectedAshfallLaunchArtifactCount = expectedAshfallClasspathEntryCount + 1;
        verifyRealProcessLaunchHarness("reports/echo-native/ashfall/real-process-launch-harness-plan.json", "PASS", true, expectedAshfallLaunchArtifactCount, expectedAshfallClasspathEntryCount, 0);
        verifyRealProcessLaunchSafetyGate("reports/echo-native/ashfall/real-process-launch-safety-gate.json", "PASS", true, expectedAshfallLaunchArtifactCount, expectedAshfallLaunchArtifactCount, 0, 0);
        verifyRealProcessCommandLinePreview("reports/echo-native/ashfall/real-process-command-line-preview.json", "PASS", 18, expectedAshfallClasspathEntryCount, 0, "net.minecraft.client.main.Main", true);
        verifyRealProcessEnvironmentPlan("reports/echo-native/ashfall/real-process-environment-plan.json", "PASS", 4, 0);
        verifyProcessExecutionReadiness("reports/echo-native/ashfall/process-execution-readiness.json", "FAILED", false, 28, 1);
        verifyControlledLaunchOperatorChecklist("reports/echo-native/ashfall/controlled-launch-operator-checklist.json", "FAILED", false, 8, 1);
        verifyControlledLaunchRollbackPlan("reports/echo-native/ashfall/controlled-launch-rollback-plan.json", "FAILED", false, 1);
        verifyNativeLoaderBetaGate("reports/echo-native/ashfall/phase13-native-loader-beta-gate.json", "FAILED", false, 1);
        verifyRuntimeFixtureOperatorPacket("reports/echo-native/ashfall/runtime-fixture-operator-packet.json", "PASS", true, true, 2, 2);
        verifyNativeLoaderBetaFeedbackInventory("reports/echo-native/ashfall/native-loader-beta-feedback-inventory.json", "FAILED", 3, 46, true, true);
        verifyNativeLoaderBetaEvidenceQuality("reports/echo-native/ashfall/native-loader-beta-evidence-quality.json", "FAILED", 3, 3, 0, true, true);
        verifyNativeLoaderBetaSoakOperatorPacket("reports/echo-native/ashfall/native-loader-beta-soak-operator-packet.json", "FAILED", false, 3, 0);
        verifyNativeLoaderBetaSessionNoteDrafts("reports/echo-native/ashfall/native-loader-beta-session-note-drafts.json", "FAILED", 3, 3, 0);
        verifyNativeLoaderBetaSessionDraftFiles("reports/echo-native/ashfall/native-loader-beta-session-draft-files.json", "FAILED", 0, false);
        verifyNativeLoaderBetaSessionDraftFiles("reports/echo-native/ashfall/phase13-m29-session-draft-status.json", "FAILED", 0, false);
        verifyNativeLoaderBetaSessionNoteValidation("reports/echo-native/ashfall/native-loader-beta-session-note-validation.json", "PASS", 6, 3, 3, 3);
        verifyNativeLoaderBetaSessionNoteValidation("reports/echo-native/ashfall/phase13-m29-note-validation-status.json", "PASS", 6, 3, 3, 3);
        verifyNativeLoaderBetaSoakStatus("reports/echo-native/ashfall/native-loader-beta-soak-status-dashboard.json", "FAILED", false, false, 3, 0, 0);
        verifyNativeLoaderBetaSoakStatus("reports/echo-native/ashfall/native-loader-beta-next-session-checklist.json", "FAILED", null, false, 3, 0, 0);
        verifyNativeLoaderBetaRemainingSessionPlan("reports/echo-native/ashfall/native-loader-beta-remaining-session-plan.json", "FAILED", 3, 3, 0);
        verifyNativeLoaderBetaSessionProofMatrix("reports/echo-native/ashfall/native-loader-beta-session-proof-matrix.json", "FAILED", 3, 3);
        verifyPhase13M29Completion("reports/echo-native/ashfall/phase13-m29-completion.json", "FAILED", false, false, 3, 3);
        verifyPhase13M30Completion("reports/echo-native/ashfall/phase13-m30-completion.json", "FAILED", false, false, 3, 3);
        verifyPublicBetaOpening("reports/echo-native/ashfall/public-beta-opening-audit.json", "FAILED", false, 1, 0);
        verifyPublicBetaOpening("reports/echo-native/ashfall/public-beta-safety-gate.json", "FAILED", false, 0, 0);
        verifyPublicBetaOpening("reports/echo-native/ashfall/public-beta-tester-package-readiness.json", "FAILED", false, 0, 0);
        verifyPublicBetaModuleCoverage(
                "reports/echo-native/ashfall/public-beta-module-coverage.json",
                "FAILED",
                true,
                Math.toIntExact(expectedAshfallRuntimeModuleCount()),
                expectedAshfallRequiredFeatureCount(),
                1);
        verifyPublicBetaOpening("reports/echo-native/ashfall/public-beta-rollback-readiness.json", "FAILED", false, 0, 0);
        verifyPublicBetaKnownLimitations("reports/echo-native/ashfall/public-beta-known-limitations.json", "FAILED", false, 1, 0);
        verifyPhase13M31Completion("reports/echo-native/ashfall/phase13-m31-completion.json", "FAILED", false, false);
        verifyPublicBetaOpening("reports/echo-native/ashfall/phase13-m32-readiness.json", "FAILED", false, 0, 0);
        verifyEnvelope("reports/echo-native/broken_pack/ai-tasks.json", "FAILED", 7, true);
        verifyPhase12Gate("reports/echo-native/broken_pack/phase12-completion.json", false);
        verifyPhase13Readiness("reports/echo-native/broken_pack/phase13-readiness.json", false);
        verifyPhase13Plan("reports/echo-native/broken_pack/phase13-plan.json", "FAILED", false);
        verifyLifecycleSimulation("reports/echo-native/broken_pack/lifecycle-simulation-result.json", "FAILED", false);
        verifyServiceSimulation("reports/echo-native/broken_pack/service-attach-simulation-result.json", "FAILED", false);
        verifyCrashBoundaryVerification("reports/echo-native/broken_pack/crash-boundary-verification.json", "FAILED", false);
        verifyCrashBoundarySimulation("reports/echo-native/broken_pack/crash-boundary-simulation-result.json", "FAILED", false);
        verifyBoundaryFailureCases("reports/echo-native/broken_pack/boundary-failure-cases.json", "FAILED", false);
        verifyClassloaderBoundaryRehearsal("reports/echo-native/broken_pack/classloader-boundary-rehearsal.json", "FAILED", false);
        verifyBoundaryStateMachine("reports/echo-native/broken_pack/loader-boundary-state-machine.json", "FAILED", false);
        verifyBoundaryVerification("reports/echo-native/broken_pack/loader-boundary-verification.json", "FAILED", false);
        verifyClasspathClassloaderCompatibility("reports/echo-native/broken_pack/classpath-classloader-compatibility.json", "FAILED", false);
        verifyTestProcessBoundary("reports/echo-native/broken_pack/test-process-boundary-verification.json", "FAILED", false);
        verifyControlledTestProcessPreflight("reports/echo-native/broken_pack/controlled-test-process-preflight.json", "FAILED", false);
        verifyPhase13M1SafetyStatus("reports/echo-native/broken_pack/phase13-m1-safety-status.json", "FAILED", false);
        verifyResourceBridgePolicy("reports/echo-native/broken_pack/resource-bridge-policy-rehearsal.json", "FAILED", false);
        verifyRegistryBridgePolicy("reports/echo-native/broken_pack/registry-bridge-policy-rehearsal.json", "FAILED", false);
        verifyPhase13BridgeSafetyStatus("reports/echo-native/broken_pack/phase13-bridge-safety-status.json", "FAILED", false);
        verifyPhase13M1Completion("reports/echo-native/broken_pack/phase13-m1-completion.json", "FAILED", false);
        verifyPhase13M2Readiness("reports/echo-native/broken_pack/phase13-m2-readiness.json", "FAILED", false);
        verifyPhase13PrototypeSafetyGate("reports/echo-native/broken_pack/phase13-prototype-safety-gate.json", "FAILED", false);
        verifyMinecraftVersionResolverPlan("reports/echo-native/broken_pack/minecraft-version-resolver-plan.json", "FAILED", false);
        verifyMinecraftVersionSourcePolicy("reports/echo-native/broken_pack/minecraft-version-source-policy.json", "FAILED", false);
        verifyMinecraftResolverSafetyStatus("reports/echo-native/broken_pack/minecraft-resolver-safety-status.json", "FAILED", false);
        verifyLibraryResolutionPlan("reports/echo-native/broken_pack/library-resolution-plan.json", "FAILED", false);
        verifyLibrarySourcePolicy("reports/echo-native/broken_pack/library-source-policy.json", "FAILED", false);
        verifyLibraryResolverSafetyStatus("reports/echo-native/broken_pack/library-resolver-safety-status.json", "FAILED", false);
        verifyClasspathBuilderPlan("reports/echo-native/broken_pack/classpath-builder-plan.json", "FAILED", false);
        verifyClasspathSourcePolicy("reports/echo-native/broken_pack/classpath-source-policy.json", "FAILED", false);
        verifyClasspathBuilderSafetyStatus("reports/echo-native/broken_pack/classpath-builder-safety-status.json", "FAILED", false);
        verifyNativeExtractionPlan("reports/echo-native/broken_pack/native-extraction-plan.json", "FAILED", false);
        verifyNativeExtractionSourcePolicy("reports/echo-native/broken_pack/native-extraction-source-policy.json", "FAILED", false);
        verifyNativeExtractionSafetyStatus("reports/echo-native/broken_pack/native-extraction-safety-status.json", "FAILED", false);
        verifyLaunchArgumentPlan("reports/echo-native/broken_pack/launch-argument-builder-plan.json", "FAILED", false);
        verifyLaunchArgumentSourcePolicy("reports/echo-native/broken_pack/launch-argument-source-policy.json", "FAILED", false);
        verifyLaunchArgumentSafetyStatus("reports/echo-native/broken_pack/launch-argument-safety-status.json", "FAILED", false);
        verifyControlledDummyProcessPlan("reports/echo-native/broken_pack/controlled-dummy-process-plan.json", "FAILED", false);
        verifyControlledDummyProcessResult("reports/echo-native/broken_pack/controlled-dummy-process-result.json", "FAILED", false);
        verifyDummyProcessCrashBoundary("reports/echo-native/broken_pack/dummy-process-crash-boundary.json", "FAILED", false);
        verifyDummyProcessOutputCapture("reports/echo-native/broken_pack/dummy-process-output-capture.json", "FAILED", false);
        verifyAddonRuntimeDiscoveryPlan("reports/echo-native/broken_pack/addon-runtime-discovery-plan.json", "FAILED", false);
        verifyAddonRuntimeDescriptors("reports/echo-native/broken_pack/addon-runtime-descriptors.json", "FAILED", false);
        verifyAddonRuntimeDiscoverySafetyStatus("reports/echo-native/broken_pack/addon-runtime-discovery-safety-status.json", "FAILED", false);
        verifyLifecycleStubExecutionPlan("reports/echo-native/broken_pack/lifecycle-stub-execution-plan.json", "FAILED", false);
        verifyLifecycleStubExecutionResult("reports/echo-native/broken_pack/lifecycle-stub-execution-result.json", "FAILED", false);
        verifyLifecycleStubCrashBoundary("reports/echo-native/broken_pack/lifecycle-stub-crash-boundary.json", "FAILED", false);
        verifyLifecycleStubSafetyStatus("reports/echo-native/broken_pack/lifecycle-stub-safety-status.json", "FAILED", false);
        verifyServiceBusPlan("reports/echo-native/broken_pack/service-bus-plan.json", "FAILED", false);
        verifyServiceBusRegistry("reports/echo-native/broken_pack/service-bus-registry.json", "FAILED", false);
        verifyServiceBusSimulationResult("reports/echo-native/broken_pack/service-bus-simulation-result.json", "FAILED", false);
        verifyServiceBusSafetyStatus("reports/echo-native/broken_pack/service-bus-safety-status.json", "FAILED", false);
        verifyConfigSourceInventory("reports/echo-native/broken_pack/config-source-inventory.json", "FAILED", false);
        verifyConfigValidationResult("reports/echo-native/broken_pack/config-validation-result.json", "FAILED", false);
        verifyConfigWritePlan("reports/echo-native/broken_pack/config-write-plan.json", "FAILED", false);
        verifyConfigSafetyStatus("reports/echo-native/broken_pack/config-safety-status.json", "FAILED", false);
        verifyResourceSourceInventory("reports/echo-native/broken_pack/resource-source-inventory.json", "FAILED", false);
        verifyResourceNamespaceValidation("reports/echo-native/broken_pack/resource-namespace-validation.json", "FAILED", false);
        verifyResourcePackOrderPlan("reports/echo-native/broken_pack/resource-pack-order-plan.json", "FAILED", false);
        verifyResourceConflictReport("reports/echo-native/broken_pack/resource-conflict-report.json", "FAILED", false);
        verifyResourceBridgeSafetyStatus("reports/echo-native/broken_pack/resource-bridge-safety-status.json", "FAILED", false);
        verifyRegistrySourceInventory("reports/echo-native/broken_pack/registry-source-inventory.json", "FAILED", false);
        verifyRegistryIdValidation("reports/echo-native/broken_pack/registry-id-validation.json", "FAILED", false);
        verifySandboxRegistryModel("reports/echo-native/broken_pack/sandbox-registry-model.json", "FAILED", false);
        verifyRegistryConflictReport("reports/echo-native/broken_pack/registry-conflict-report.json", "FAILED", false);
        verifyRegistryBridgeSafetyStatus("reports/echo-native/broken_pack/registry-bridge-safety-status.json", "FAILED", false);
        verifyNetworkChannelInventory("reports/echo-native/broken_pack/network-channel-inventory.json", "FAILED", false);
        verifyNetworkPacketValidation("reports/echo-native/broken_pack/network-packet-validation.json", "FAILED", false);
        verifyNetworkSchemaModel("reports/echo-native/broken_pack/network-schema-model.json", "FAILED", false);
        verifyNetworkConflictReport("reports/echo-native/broken_pack/network-conflict-report.json", "FAILED", false);
        verifyNetworkBridgeSafetyStatus("reports/echo-native/broken_pack/network-bridge-safety-status.json", "FAILED", false);
        verifyTransformSourceInventory("reports/echo-native/broken_pack/transform-source-inventory.json", "FAILED", false);
        verifyTransformAllowlistValidation("reports/echo-native/broken_pack/transform-allowlist-validation.json", "FAILED", false);
        verifyTransformPipelinePlan("reports/echo-native/broken_pack/transform-pipeline-plan.json", "FAILED", false);
        verifyTransformConflictReport("reports/echo-native/broken_pack/transform-conflict-report.json", "FAILED", false);
        verifyTransformSafetyStatus("reports/echo-native/broken_pack/transform-safety-status.json", "FAILED", false);
        verifyCrashHardeningCoverage("reports/echo-native/broken_pack/crash-hardening-coverage.json", "FAILED", false);
        verifyFailureContainmentMatrix("reports/echo-native/broken_pack/failure-containment-matrix.json", "FAILED", false);
        verifySupportBundleDryRunPlan("reports/echo-native/broken_pack/support-bundle-dry-run-plan.json", "FAILED", false);
        verifyPhase13M16SafetyStatus("reports/echo-native/broken_pack/phase13-m16-safety-status.json", "FAILED", false);
        verifyIsolatedLaunchEnvironmentPlan("reports/echo-native/broken_pack/isolated-launch-environment-plan.json", "FAILED", false);
        verifyMinecraftLaunchPreflight("reports/echo-native/broken_pack/minecraft-launch-preflight.json", "FAILED", false);
        verifyLaunchSafetyGate("reports/echo-native/broken_pack/launch-safety-gate.json", "FAILED", false);
        verifyControlledLaunchFailureCapturePlan("reports/echo-native/broken_pack/controlled-launch-failure-capture-plan.json", "FAILED", false);
        verifyPhase13M17Readiness("reports/echo-native/broken_pack/phase13-m17-readiness.json", "FAILED", false);
        verifyIsolatedLaunchAttemptPlan("reports/echo-native/broken_pack/isolated-launch-attempt-plan.json", "FAILED", true);
        verifyLocalRuntimeArtifactCheck("reports/echo-native/broken_pack/local-runtime-artifact-check.json", "FAILED", false);
        verifyControlledLaunchAttemptResult("reports/echo-native/broken_pack/controlled-launch-attempt-result.json", "FAILED", true);
        verifyLaunchOutputCapture("reports/echo-native/broken_pack/launch-output-capture.json", "FAILED");
        verifyPhase13M17LaunchStatus("reports/echo-native/broken_pack/phase13-m17-launch-status.json", "FAILED", true);
        verifyLocalRuntimeArtifactInventory("reports/echo-native/broken_pack/local-runtime-artifact-inventory.json", "FAILED", false);
        verifyLocalRuntimeArtifactMap("reports/echo-native/broken_pack/local-runtime-artifact-map.json", "FAILED", false);
        verifyLaunchArtifactResolutionStatus("reports/echo-native/broken_pack/launch-artifact-resolution-status.json", "FAILED", false);
        verifyIsolatedLaunchExecutionEligibility("reports/echo-native/broken_pack/isolated-launch-execution-eligibility.json", "FAILED", false);
        verifyPhase13M17ArtifactReadiness("reports/echo-native/broken_pack/phase13-m17-artifact-readiness.json", "FAILED", false);
        verifyPhase13M17ArtifactBlockers("reports/echo-native/broken_pack/phase13-m17-artifact-blockers.json", "FAILED", 0, false);
        verifyPhase13M17BlockerResolutionPlan("reports/echo-native/broken_pack/phase13-m17-blocker-resolution-plan.json", "FAILED", 0, false);
        verifyPhase13M17ArtifactPackagingAudit("reports/echo-native/broken_pack/phase13-m17-artifact-packaging-audit.json", "FAILED", 0, false);
        verifyPhase13M17ArtifactPackagingResolutionPlan("reports/echo-native/broken_pack/phase13-m17-artifact-packaging-resolution-plan.json", "FAILED", 0, false);
        verifyPhase13M18Readiness("reports/echo-native/broken_pack/phase13-m18-readiness.json", "FAILED", false, 0);
        verifyRuntimeFixturePresence("reports/echo-native/broken_pack/runtime-fixture-presence.json", "FAILED", false, 0, 0);
        verifyRuntimeFixtureMappingReadiness("reports/echo-native/broken_pack/runtime-fixture-mapping-readiness.json", "FAILED", false, 0, 0);
        verifyRuntimeFixtureIntakePlan("reports/echo-native/broken_pack/runtime-fixture-intake-plan.json", "FAILED", false, 0, 0);
        verifyRuntimeFixtureIntakeChecklist("reports/echo-native/broken_pack/runtime-fixture-intake-checklist.json", "FAILED", false, 0, 0);
        verifyRuntimeFixtureApprovalAudit("reports/echo-native/broken_pack/runtime-fixture-approval-audit.json", "FAILED", false, 0, 0);
        verifyRuntimeFixtureApprovalTemplate("reports/echo-native/broken_pack/runtime-fixture-approval-template.json", "FAILED", false, 0);
        verifyRuntimeFixtureHandoff("reports/echo-native/broken_pack/runtime-fixture-handoff.json", "FAILED", false, 0);
        verifyRuntimeFixtureValidationRunbook("reports/echo-native/broken_pack/runtime-fixture-validation-runbook.json", "FAILED", false, 0);
        verifyRuntimeFixtureApprovalDraft("reports/echo-native/broken_pack/runtime-fixture-approval-draft.json", "FAILED", false, 0, 0);
        verifyRuntimeFixtureHashReview("reports/echo-native/broken_pack/runtime-fixture-hash-review.json", "FAILED", false, 0, 0);
        verifyRuntimeFixtureIntegrityAudit("reports/echo-native/broken_pack/runtime-fixture-integrity-audit.json", "FAILED", false, 0, 0);
        verifyRuntimeFixtureIntegrityManifest("reports/echo-native/broken_pack/runtime-fixture-integrity-manifest.json", "FAILED", false, 0);
        verifyPhase13M17Completion("reports/echo-native/broken_pack/phase13-m17-completion.json", "FAILED", false, 25);
        verifyPhase13M18ReadinessAudit("reports/echo-native/broken_pack/phase13-m18-readiness-audit.json", "FAILED", false, 25);
        verifySmokeSessionPlan("reports/echo-native/broken_pack/smoke-session-plan.json", "FAILED", false);
        verifySmokeSessionSafetyGate("reports/echo-native/broken_pack/smoke-session-safety-gate.json", "FAILED", false);
        verifySmokeSessionResult("reports/echo-native/broken_pack/smoke-session-result.json", "FAILED", false);
        verifySmokeSessionDiagnostics("reports/echo-native/broken_pack/smoke-session-diagnostics.json", "FAILED", false);
        verifyPhase13M18Completion("reports/echo-native/broken_pack/phase13-m18-completion.json", "FAILED", false, false);
        verifyPhase13M19Readiness("reports/echo-native/broken_pack/phase13-m19-readiness.json", "FAILED", false);
        verifyFirstPlaytestCandidatePackage("reports/echo-native/broken_pack/first-playtest-candidate-package.json", "FAILED", false);
        verifyFirstPlaytestSupportBundle("reports/echo-native/broken_pack/first-playtest-support-bundle.json", "FAILED", false);
        verifyFirstPlaytestRollbackNotes("reports/echo-native/broken_pack/first-playtest-rollback-notes.json", "FAILED", false);
        verifyFirstPlaytestKnownLimitations("reports/echo-native/broken_pack/first-playtest-known-limitations.json", "FAILED", false);
        verifyExperimentalNativeLoaderLabel("reports/echo-native/broken_pack/experimental-native-loader-label.json", "FAILED", false);
        verifyFirstPlaytestCrashReportCollection("reports/echo-native/broken_pack/first-playtest-crash-report-collection.json", "FAILED", false);
        verifyPhase13M19Completion("reports/echo-native/broken_pack/phase13-m19-completion.json", "FAILED", false);
        verifyFirstPlaytestOpenGate("reports/echo-native/broken_pack/first-playtest-open-gate.json", "FAILED", false);
        verifyPhase13FirstPlaytestBlockers("reports/echo-native/broken_pack/phase13-first-playtest-blockers.json", "FAILED", false, 25);
        verifyPhase13FirstPlaytestRoadmap("reports/echo-native/broken_pack/phase13-first-playtest-roadmap.json", "FAILED", false, false, 1);
        verifyPhase13FirstPlaytestNextActions("reports/echo-native/broken_pack/phase13-first-playtest-next-actions.json", "FAILED", 1);
        verifyPhase13FirstPlaytestFullRoadmap("reports/echo-native/broken_pack/phase13-first-playtest-full-roadmap.json", "FAILED", 18, 0, "phase13.m2.minecraft_resolver_planning");
        verifyPhase14Preflight("reports/echo-native/broken_pack/first-playtest-post-open-intake.json", "FAILED", false, 0);
        verifyPhase14Preflight("reports/echo-native/broken_pack/first-playtest-feedback-inventory.json", "FAILED", false, 0);
        verifyPhase14Preflight("reports/echo-native/broken_pack/first-playtest-waiting-checklist.json", "FAILED", false, 3);
        verifyPhase14Preflight("reports/echo-native/broken_pack/phase14-preflight-audit.json", "FAILED", false, 0);
        verifyPhase14Preflight("reports/echo-native/broken_pack/phase14-readiness.json", "FAILED", false, 0);
        verifyPhase14Preflight("reports/echo-native/broken_pack/phase14-next-actions.json", "FAILED", false, 3);
        verifyLaunchRealityAudit("reports/echo-native/broken_pack/native-loader-reality-audit.json", "FAILED", false, false, false, 8);
        verifyLaunchCommandClassification("reports/echo-native/broken_pack/native-loader-launch-command-classification.json", "FAILED", false, false, 5);
        verifyNativeLoaderBetaNextActions("reports/echo-native/broken_pack/native-loader-beta-implementation-next-actions.json", "FAILED", false, 3);
        verifyIsolatedRuntimeWorkspace("reports/echo-native/broken_pack/isolated-runtime-workspace-plan.json", "FAILED", false, false, 7, 7);
        verifyIsolatedRuntimeWorkspace("reports/echo-native/broken_pack/isolated-runtime-workspace-materialization.json", "FAILED", false, false, 7, 7);
        verifyIsolatedRuntimeWorkspace("reports/echo-native/broken_pack/isolated-runtime-workspace-safety-status.json", "FAILED", false, false, 7, 7);
        verifyRealProcessLaunchHarness("reports/echo-native/broken_pack/real-process-launch-harness-plan.json", "FAILED", false, 0, 0, 11);
        verifyRealProcessLaunchSafetyGate("reports/echo-native/broken_pack/real-process-launch-safety-gate.json", "FAILED", false, 0, 0, 0, 11);
        verifyRealProcessCommandLinePreview("reports/echo-native/broken_pack/real-process-command-line-preview.json", "FAILED", 9, 0, 11, "minecraft-client-main-class", false);
        verifyRealProcessEnvironmentPlan("reports/echo-native/broken_pack/real-process-environment-plan.json", "FAILED", 4, 11);
        verifyProcessExecutionReadiness("reports/echo-native/broken_pack/process-execution-readiness.json", "FAILED", false, 28, 30);
        verifyControlledLaunchOperatorChecklist("reports/echo-native/broken_pack/controlled-launch-operator-checklist.json", "FAILED", false, 8, 30);
        verifyControlledLaunchRollbackPlan("reports/echo-native/broken_pack/controlled-launch-rollback-plan.json", "FAILED", false, 30);
        verifyNativeLoaderBetaGate("reports/echo-native/broken_pack/phase13-native-loader-beta-gate.json", "FAILED", false, 30);
        verifyRuntimeFixtureOperatorPacket("reports/echo-native/broken_pack/runtime-fixture-operator-packet.json", "FAILED", false, false, 0, 0);
        verifyNativeLoaderBetaFeedbackInventory("reports/echo-native/broken_pack/native-loader-beta-feedback-inventory.json", "FAILED", 0, 0, false, false);
        verifyNativeLoaderBetaEvidenceQuality("reports/echo-native/broken_pack/native-loader-beta-evidence-quality.json", "FAILED", 0, 3, 3, false, false);
        verifyNativeLoaderBetaSoakOperatorPacket("reports/echo-native/broken_pack/native-loader-beta-soak-operator-packet.json", "FAILED", false, 0, 3);
        verifyNativeLoaderBetaSessionNoteDrafts("reports/echo-native/broken_pack/native-loader-beta-session-note-drafts.json", "FAILED", 0, 3, 3);
        verifyNativeLoaderBetaSessionDraftFiles("reports/echo-native/broken_pack/native-loader-beta-session-draft-files.json", "FAILED", 0, false);
        verifyNativeLoaderBetaSessionDraftFiles("reports/echo-native/broken_pack/phase13-m29-session-draft-status.json", "FAILED", 0, false);
        verifyNativeLoaderBetaSessionNoteValidation("reports/echo-native/broken_pack/native-loader-beta-session-note-validation.json", "PASS_WITH_WARNINGS", 0, 0, 0, 0);
        verifyNativeLoaderBetaSessionNoteValidation("reports/echo-native/broken_pack/phase13-m29-note-validation-status.json", "PASS_WITH_WARNINGS", 0, 0, 0, 0);
        verifyNativeLoaderBetaSoakStatus("reports/echo-native/broken_pack/native-loader-beta-soak-status-dashboard.json", "FAILED", false, false, 0, 3, 3);
        verifyNativeLoaderBetaSoakStatus("reports/echo-native/broken_pack/native-loader-beta-next-session-checklist.json", "FAILED", null, false, 0, 3, 3);
        verifyNativeLoaderBetaRemainingSessionPlan("reports/echo-native/broken_pack/native-loader-beta-remaining-session-plan.json", "FAILED", 0, 3, 3);
        verifyNativeLoaderBetaSessionProofMatrix("reports/echo-native/broken_pack/native-loader-beta-session-proof-matrix.json", "FAILED", 0, 3);
        verifyPhase13M29Completion("reports/echo-native/broken_pack/phase13-m29-completion.json", "FAILED", false, false, 0, 3);
        verifyPhase13M30Completion("reports/echo-native/broken_pack/phase13-m30-completion.json", "FAILED", false, false, 0, 3);
        verifyPublicBetaOpening("reports/echo-native/broken_pack/public-beta-opening-audit.json", "FAILED", false, 2, 0);
        verifyPublicBetaOpening("reports/echo-native/broken_pack/public-beta-safety-gate.json", "FAILED", false, 0, 0);
        verifyPublicBetaOpening("reports/echo-native/broken_pack/public-beta-tester-package-readiness.json", "FAILED", false, 0, 0);
        verifyPublicBetaModuleCoverage("reports/echo-native/broken_pack/public-beta-module-coverage.json", "FAILED", false, 3, 2, 0);
        verifyPublicBetaOpening("reports/echo-native/broken_pack/public-beta-rollback-readiness.json", "FAILED", false, 0, 0);
        verifyPublicBetaKnownLimitations("reports/echo-native/broken_pack/public-beta-known-limitations.json", "FAILED", false, 0, 0);
        verifyPhase13M31Completion("reports/echo-native/broken_pack/phase13-m31-completion.json", "FAILED", false, false);
        verifyPublicBetaOpening("reports/echo-native/broken_pack/phase13-m32-readiness.json", "FAILED", false, 0, 0);
        verifyNoLocalOnlyPaths(first);
    }

    private static void runCli(int expectedExit, String... args) throws IOException {
        int exit = new EchoNativeQaCli().run(args);
        if (exit != expectedExit) {
            throw new IllegalStateException("echo-native " + String.join(" ", args) + " exited " + exit + "; expected " + expectedExit);
        }
    }

    private static void runBetaSnapshotCommands() throws IOException {
        runCli(1, "phase13", "intake", "beta-feedback", "fixtures/ashfall");
        runCli(1, "phase13", "intake", "beta-feedback", "fixtures/broken-pack");
        runCli(1, "phase13", "verify", "m28", "fixtures/ashfall");
        runCli(1, "phase13", "verify", "m28", "fixtures/broken-pack");
        runCli(1, "phase13", "intake", "beta-soak", "fixtures/ashfall");
        runCli(1, "phase13", "intake", "beta-soak", "fixtures/broken-pack");
        runCli(1, "phase13", "export", "beta-soak-packet", "fixtures/ashfall");
        runCli(1, "phase13", "export", "beta-soak-packet", "fixtures/broken-pack");
        runCli(1, "phase13", "prepare", "beta-session-drafts", "fixtures/ashfall");
        runCli(1, "phase13", "prepare", "beta-session-drafts", "fixtures/broken-pack");
        runCli(0, "phase13", "validate", "beta-session-notes", "fixtures/ashfall");
        runCli(0, "phase13", "validate", "beta-session-notes", "fixtures/broken-pack");
        runCli(1, "phase13", "audit", "beta-soak-evidence", "fixtures/ashfall");
        runCli(1, "phase13", "audit", "beta-soak-evidence", "fixtures/broken-pack");
        runCli(1, "phase13", "status", "beta-soak", "fixtures/ashfall");
        runCli(1, "phase13", "status", "beta-soak", "fixtures/broken-pack");
        runCli(1, "phase13", "verify", "m30", "fixtures/ashfall");
        runCli(1, "phase13", "verify", "m30", "fixtures/broken-pack");
        runCli(1, "phase13", "verify", "m31", "fixtures/ashfall");
        runCli(1, "phase13", "verify", "m31", "fixtures/broken-pack");
    }

    private static Map<String, String> snapshot() throws IOException {
        Map<String, String> snapshot = new LinkedHashMap<>();
        for (String path : REPORT_FILES) {
            Path report = Path.of(path);
            if (!Files.isRegularFile(report)) {
                throw new IllegalStateException("Missing native snapshot report: " + path);
            }
            snapshot.put(path, Files.readString(report));
        }
        return snapshot;
    }

    private static List<String> mismatchedReports(Map<String, String> first, Map<String, String> second) {
        return first.keySet().stream()
                .filter(path -> !first.get(path).equals(second.get(path)))
                .toList();
    }

    private static void verifyEnvelope(String path, String expectedStatus, int expectedTaskCount, boolean phase13Blocked) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        if (!"1970-01-01T00:00:00Z".equals(envelope.get("generatedAt"))) {
            throw new IllegalStateException(path + " has a nondeterministic generatedAt value.");
        }
        if (!expectedStatus.equals(envelope.get("status"))) {
            throw new IllegalStateException(path + " status was " + envelope.get("status") + "; expected " + expectedStatus);
        }
        if (!Boolean.valueOf(phase13Blocked).equals(data.get("phase13Blocked"))) {
            throw new IllegalStateException(path + " did not preserve the Phase 13 gate.");
        }
        Object taskCount = data.get("taskCount");
        if (taskCount instanceof Number number && number.longValue() != expectedTaskCount) {
            throw new IllegalStateException(path + " taskCount was " + number + "; expected " + expectedTaskCount);
        }
    }

    private static void verifyPhase12Gate(String path, boolean expectedComplete) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        if (!"1970-01-01T00:00:00Z".equals(envelope.get("generatedAt"))) {
            throw new IllegalStateException(path + " has a nondeterministic generatedAt value.");
        }
        if (!Boolean.valueOf(expectedComplete).equals(data.get("phase12Complete"))) {
            throw new IllegalStateException(path + " phase12Complete was " + data.get("phase12Complete") + "; expected " + expectedComplete);
        }
        if (!Boolean.FALSE.equals(data.get("phase13WorkStarted"))) {
            throw new IllegalStateException(path + " must prove Phase 13 work was not started.");
        }
    }

    private static void verifyPhase13Readiness(String path, boolean expectedReady) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        if (!"1970-01-01T00:00:00Z".equals(envelope.get("generatedAt"))) {
            throw new IllegalStateException(path + " has a nondeterministic generatedAt value.");
        }
        if (!Boolean.valueOf(expectedReady).equals(data.get("phase13Ready"))) {
            throw new IllegalStateException(path + " phase13Ready was " + data.get("phase13Ready") + "; expected " + expectedReady);
        }
        if (!Boolean.valueOf(!expectedReady).equals(data.get("phase13Blocked"))) {
            throw new IllegalStateException(path + " phase13Blocked was " + data.get("phase13Blocked") + "; expected " + !expectedReady);
        }
        if (!Boolean.FALSE.equals(data.get("phase13WorkStarted"))) {
            throw new IllegalStateException(path + " must prove Phase 13 work was not started.");
        }
    }

    private static void verifyPhase13Plan(String path, String expectedStatus, boolean expectedStarted) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        if (!"1970-01-01T00:00:00Z".equals(envelope.get("generatedAt"))) {
            throw new IllegalStateException(path + " has a nondeterministic generatedAt value.");
        }
        if (!expectedStatus.equals(envelope.get("status"))) {
            throw new IllegalStateException(path + " status was " + envelope.get("status") + "; expected " + expectedStatus);
        }
        if (!Boolean.valueOf(expectedStarted).equals(data.get("phase13PlanningStarted"))) {
            throw new IllegalStateException(path + " phase13PlanningStarted was " + data.get("phase13PlanningStarted") + "; expected " + expectedStarted);
        }
        if (!Boolean.FALSE.equals(data.get("prototypeRuntimeStarted"))) {
            throw new IllegalStateException(path + " must not start prototype runtime behavior.");
        }
        if (!Boolean.TRUE.equals(data.get("planOnly"))) {
            throw new IllegalStateException(path + " must remain plan-only.");
        }
    }

    private static void verifyLifecycleSimulation(String path, String expectedStatus, boolean expectedSimulated) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        if (!"1970-01-01T00:00:00Z".equals(envelope.get("generatedAt"))) {
            throw new IllegalStateException(path + " has a nondeterministic generatedAt value.");
        }
        if (!expectedStatus.equals(envelope.get("status"))) {
            throw new IllegalStateException(path + " status was " + envelope.get("status") + "; expected " + expectedStatus);
        }
        if (!Boolean.valueOf(expectedSimulated).equals(data.get("simulated"))) {
            throw new IllegalStateException(path + " simulated was " + data.get("simulated") + "; expected " + expectedSimulated);
        }
        if (!Boolean.FALSE.equals(data.get("classloaderCreated"))) {
            throw new IllegalStateException(path + " must not create a classloader.");
        }
        if (!Boolean.FALSE.equals(data.get("processLaunched"))) {
            throw new IllegalStateException(path + " must not launch a process.");
        }
        if (!Boolean.TRUE.equals(data.get("simulationOnly"))) {
            throw new IllegalStateException(path + " must remain simulation-only.");
        }
    }

    private static void verifyServiceSimulation(String path, String expectedStatus, boolean expectedSimulated) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        if (!"1970-01-01T00:00:00Z".equals(envelope.get("generatedAt"))) {
            throw new IllegalStateException(path + " has a nondeterministic generatedAt value.");
        }
        if (!expectedStatus.equals(envelope.get("status"))) {
            throw new IllegalStateException(path + " status was " + envelope.get("status") + "; expected " + expectedStatus);
        }
        if (!Boolean.valueOf(expectedSimulated).equals(data.get("serviceAttachSimulated"))) {
            throw new IllegalStateException(path + " serviceAttachSimulated was " + data.get("serviceAttachSimulated") + "; expected " + expectedSimulated);
        }
        if (!Boolean.FALSE.equals(data.get("classloaderCreated"))) {
            throw new IllegalStateException(path + " must not create a classloader.");
        }
        if (!Boolean.FALSE.equals(data.get("processLaunched"))) {
            throw new IllegalStateException(path + " must not launch a process.");
        }
        if (!Boolean.FALSE.equals(data.get("executedServiceCode"))) {
            throw new IllegalStateException(path + " must not execute service code.");
        }
        if (!Boolean.TRUE.equals(data.get("simulationOnly"))) {
            throw new IllegalStateException(path + " must remain simulation-only.");
        }
    }

    private static void verifyCrashBoundaryVerification(String path, String expectedStatus, boolean expectedVerified) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        if (!"1970-01-01T00:00:00Z".equals(envelope.get("generatedAt"))) {
            throw new IllegalStateException(path + " has a nondeterministic generatedAt value.");
        }
        if (!expectedStatus.equals(envelope.get("status"))) {
            throw new IllegalStateException(path + " status was " + envelope.get("status") + "; expected " + expectedStatus);
        }
        if (!Boolean.valueOf(expectedVerified).equals(data.get("verified"))) {
            throw new IllegalStateException(path + " verified was " + data.get("verified") + "; expected " + expectedVerified);
        }
        if (!Boolean.TRUE.equals(data.get("crashBoundaryActive"))) {
            throw new IllegalStateException(path + " must keep the crash boundary active.");
        }
        if (!Boolean.FALSE.equals(data.get("terminatedProcess"))) {
            throw new IllegalStateException(path + " must not terminate a process.");
        }
        if (!Boolean.FALSE.equals(data.get("mutatedState"))) {
            throw new IllegalStateException(path + " must not mutate state.");
        }
    }

    private static void verifyCrashBoundarySimulation(String path, String expectedStatus, boolean expectedSimulated) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        if (!"1970-01-01T00:00:00Z".equals(envelope.get("generatedAt"))) {
            throw new IllegalStateException(path + " has a nondeterministic generatedAt value.");
        }
        if (!expectedStatus.equals(envelope.get("status"))) {
            throw new IllegalStateException(path + " status was " + envelope.get("status") + "; expected " + expectedStatus);
        }
        if (!Boolean.valueOf(expectedSimulated).equals(data.get("simulated"))) {
            throw new IllegalStateException(path + " simulated was " + data.get("simulated") + "; expected " + expectedSimulated);
        }
        if (!Boolean.TRUE.equals(data.get("crashBoundaryActive"))) {
            throw new IllegalStateException(path + " must keep the crash boundary active.");
        }
        if (!Boolean.FALSE.equals(data.get("classloaderCreated"))) {
            throw new IllegalStateException(path + " must not create a classloader.");
        }
        if (!Boolean.FALSE.equals(data.get("processLaunched"))) {
            throw new IllegalStateException(path + " must not launch a process.");
        }
        if (!Boolean.FALSE.equals(data.get("terminatedProcess"))) {
            throw new IllegalStateException(path + " must not terminate a process.");
        }
        if (!Boolean.FALSE.equals(data.get("mutatedState"))) {
            throw new IllegalStateException(path + " must not mutate state.");
        }
    }

    private static void verifyBoundaryFailureCases(String path, String expectedStatus, boolean expectedSimulated) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        if (!"1970-01-01T00:00:00Z".equals(envelope.get("generatedAt"))) {
            throw new IllegalStateException(path + " has a nondeterministic generatedAt value.");
        }
        if (!expectedStatus.equals(envelope.get("status"))) {
            throw new IllegalStateException(path + " status was " + envelope.get("status") + "; expected " + expectedStatus);
        }
        if (!Boolean.valueOf(expectedSimulated).equals(data.get("simulated"))) {
            throw new IllegalStateException(path + " simulated was " + data.get("simulated") + "; expected " + expectedSimulated);
        }
        Object caseCount = data.get("caseCount");
        if (caseCount instanceof Number number && number.longValue() != (expectedSimulated ? 6L : 0L)) {
            throw new IllegalStateException(path + " caseCount was " + number + "; expected " + (expectedSimulated ? 6 : 0));
        }
    }

    private static void verifyClassloaderBoundaryRehearsal(String path, String expectedStatus, boolean expectedRehearsed) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        if (!"1970-01-01T00:00:00Z".equals(envelope.get("generatedAt"))) {
            throw new IllegalStateException(path + " has a nondeterministic generatedAt value.");
        }
        if (!expectedStatus.equals(envelope.get("status"))) {
            throw new IllegalStateException(path + " status was " + envelope.get("status") + "; expected " + expectedStatus);
        }
        if (!Boolean.valueOf(expectedRehearsed).equals(data.get("rehearsed"))) {
            throw new IllegalStateException(path + " rehearsed was " + data.get("rehearsed") + "; expected " + expectedRehearsed);
        }
        if (!Boolean.FALSE.equals(data.get("classloaderCreated"))) {
            throw new IllegalStateException(path + " must not create a classloader.");
        }
        if (!Boolean.FALSE.equals(data.get("productionClassloader"))) {
            throw new IllegalStateException(path + " must not create a production classloader.");
        }
        if (!Boolean.FALSE.equals(data.get("resolvesRuntimeClasses"))) {
            throw new IllegalStateException(path + " must not resolve runtime classes.");
        }
        if (!Boolean.FALSE.equals(data.get("processLaunched"))) {
            throw new IllegalStateException(path + " must not launch a process.");
        }
    }

    private static void verifyBoundaryStateMachine(String path, String expectedStatus, boolean expectedVerified) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        if (!"1970-01-01T00:00:00Z".equals(envelope.get("generatedAt"))) {
            throw new IllegalStateException(path + " has a nondeterministic generatedAt value.");
        }
        if (!expectedStatus.equals(envelope.get("status"))) {
            throw new IllegalStateException(path + " status was " + envelope.get("status") + "; expected " + expectedStatus);
        }
        if (!Boolean.valueOf(expectedVerified).equals(data.get("verified"))) {
            throw new IllegalStateException(path + " verified was " + data.get("verified") + "; expected " + expectedVerified);
        }
        Object stateCount = data.get("stateCount");
        if (stateCount instanceof Number number && number.longValue() != 6L) {
            throw new IllegalStateException(path + " stateCount was " + number + "; expected 6");
        }
        if (!Boolean.TRUE.equals(data.get("simulationOnly"))) {
            throw new IllegalStateException(path + " must remain simulation-only.");
        }
    }

    private static void verifyBoundaryVerification(String path, String expectedStatus, boolean expectedVerified) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        if (!"1970-01-01T00:00:00Z".equals(envelope.get("generatedAt"))) {
            throw new IllegalStateException(path + " has a nondeterministic generatedAt value.");
        }
        if (!expectedStatus.equals(envelope.get("status"))) {
            throw new IllegalStateException(path + " status was " + envelope.get("status") + "; expected " + expectedStatus);
        }
        if (!Boolean.valueOf(expectedVerified).equals(data.get("verified"))) {
            throw new IllegalStateException(path + " verified was " + data.get("verified") + "; expected " + expectedVerified);
        }
        if (!Boolean.FALSE.equals(data.get("classloaderCreated"))) {
            throw new IllegalStateException(path + " must not create a classloader.");
        }
        if (!Boolean.FALSE.equals(data.get("resolvesRuntimeClasses"))) {
            throw new IllegalStateException(path + " must not resolve runtime classes.");
        }
        if (!Boolean.FALSE.equals(data.get("processLaunched"))) {
            throw new IllegalStateException(path + " must not launch a process.");
        }
        if (!Boolean.FALSE.equals(data.get("mutatedFilesystem"))) {
            throw new IllegalStateException(path + " must not mutate the filesystem.");
        }
    }

    private static void verifyClasspathClassloaderCompatibility(String path, String expectedStatus, boolean expectedCompatible) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        if (!"1970-01-01T00:00:00Z".equals(envelope.get("generatedAt"))) {
            throw new IllegalStateException(path + " has a nondeterministic generatedAt value.");
        }
        if (!expectedStatus.equals(envelope.get("status"))) {
            throw new IllegalStateException(path + " status was " + envelope.get("status") + "; expected " + expectedStatus);
        }
        if (!Boolean.valueOf(expectedCompatible).equals(data.get("compatible"))) {
            throw new IllegalStateException(path + " compatible was " + data.get("compatible") + "; expected " + expectedCompatible);
        }
        if (!Boolean.TRUE.equals(data.get("classpathPlannedOnly"))) {
            throw new IllegalStateException(path + " must keep classpath entries planned only.");
        }
        if (!Boolean.FALSE.equals(data.get("classloaderCreated"))) {
            throw new IllegalStateException(path + " must not create a classloader.");
        }
        if (!Boolean.FALSE.equals(data.get("productionClassloader"))) {
            throw new IllegalStateException(path + " must not create a production classloader.");
        }
        if (!Boolean.FALSE.equals(data.get("resolvesRuntimeClasses"))) {
            throw new IllegalStateException(path + " must not resolve runtime classes.");
        }
        if (!Boolean.FALSE.equals(data.get("processLaunched"))) {
            throw new IllegalStateException(path + " must not launch a process.");
        }
        if (!Boolean.FALSE.equals(data.get("filesystemMutated"))) {
            throw new IllegalStateException(path + " must not mutate the filesystem.");
        }
    }

    private static void verifyTestProcessBoundary(String path, String expectedStatus, boolean expectedVerified) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        if (!"1970-01-01T00:00:00Z".equals(envelope.get("generatedAt"))) {
            throw new IllegalStateException(path + " has a nondeterministic generatedAt value.");
        }
        if (!expectedStatus.equals(envelope.get("status"))) {
            throw new IllegalStateException(path + " status was " + envelope.get("status") + "; expected " + expectedStatus);
        }
        if (!Boolean.valueOf(expectedVerified).equals(data.get("verified"))) {
            throw new IllegalStateException(path + " verified was " + data.get("verified") + "; expected " + expectedVerified);
        }
        verifyNoRuntimeWork(path, data);
    }

    private static void verifyControlledTestProcessPreflight(String path, String expectedStatus, boolean expectedReady) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        if (!"1970-01-01T00:00:00Z".equals(envelope.get("generatedAt"))) {
            throw new IllegalStateException(path + " has a nondeterministic generatedAt value.");
        }
        if (!expectedStatus.equals(envelope.get("status"))) {
            throw new IllegalStateException(path + " status was " + envelope.get("status") + "; expected " + expectedStatus);
        }
        if (!Boolean.valueOf(expectedReady).equals(data.get("ready"))) {
            throw new IllegalStateException(path + " ready was " + data.get("ready") + "; expected " + expectedReady);
        }
        verifyNoRuntimeWork(path, data);
    }

    private static void verifyPhase13M1SafetyStatus(String path, String expectedStatus, boolean expectedSafe) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        if (!"1970-01-01T00:00:00Z".equals(envelope.get("generatedAt"))) {
            throw new IllegalStateException(path + " has a nondeterministic generatedAt value.");
        }
        if (!expectedStatus.equals(envelope.get("status"))) {
            throw new IllegalStateException(path + " status was " + envelope.get("status") + "; expected " + expectedStatus);
        }
        if (!Boolean.valueOf(expectedSafe).equals(data.get("safeToContinue"))) {
            throw new IllegalStateException(path + " safeToContinue was " + data.get("safeToContinue") + "; expected " + expectedSafe);
        }
        verifyNoRuntimeWork(path, data);
    }

    private static void verifyResourceBridgePolicy(String path, String expectedStatus, boolean expectedRehearsed) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        if (!"1970-01-01T00:00:00Z".equals(envelope.get("generatedAt"))) {
            throw new IllegalStateException(path + " has a nondeterministic generatedAt value.");
        }
        if (!expectedStatus.equals(envelope.get("status"))) {
            throw new IllegalStateException(path + " status was " + envelope.get("status") + "; expected " + expectedStatus);
        }
        if (!Boolean.valueOf(expectedRehearsed).equals(data.get("rehearsed"))) {
            throw new IllegalStateException(path + " rehearsed was " + data.get("rehearsed") + "; expected " + expectedRehearsed);
        }
        if (!Boolean.FALSE.equals(data.get("resourceRuntimeAccessed"))) {
            throw new IllegalStateException(path + " must not access runtime resources.");
        }
        if (!Boolean.FALSE.equals(data.get("gameClassesResolved"))) {
            throw new IllegalStateException(path + " must not resolve game classes.");
        }
        verifyNoRuntimeWork(path, data);
    }

    private static void verifyRegistryBridgePolicy(String path, String expectedStatus, boolean expectedRehearsed) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        if (!"1970-01-01T00:00:00Z".equals(envelope.get("generatedAt"))) {
            throw new IllegalStateException(path + " has a nondeterministic generatedAt value.");
        }
        if (!expectedStatus.equals(envelope.get("status"))) {
            throw new IllegalStateException(path + " status was " + envelope.get("status") + "; expected " + expectedStatus);
        }
        if (!Boolean.valueOf(expectedRehearsed).equals(data.get("rehearsed"))) {
            throw new IllegalStateException(path + " rehearsed was " + data.get("rehearsed") + "; expected " + expectedRehearsed);
        }
        if (!Boolean.FALSE.equals(data.get("registryInjected"))) {
            throw new IllegalStateException(path + " must not inject registries.");
        }
        if (!Boolean.FALSE.equals(data.get("registryMutated"))) {
            throw new IllegalStateException(path + " must not mutate registries.");
        }
        if (!Boolean.FALSE.equals(data.get("gameClassesResolved"))) {
            throw new IllegalStateException(path + " must not resolve game classes.");
        }
        verifyNoRuntimeWork(path, data);
    }

    private static void verifyPhase13BridgeSafetyStatus(String path, String expectedStatus, boolean expectedSafe) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        if (!"1970-01-01T00:00:00Z".equals(envelope.get("generatedAt"))) {
            throw new IllegalStateException(path + " has a nondeterministic generatedAt value.");
        }
        if (!expectedStatus.equals(envelope.get("status"))) {
            throw new IllegalStateException(path + " status was " + envelope.get("status") + "; expected " + expectedStatus);
        }
        if (!Boolean.valueOf(expectedSafe).equals(data.get("safeToContinue"))) {
            throw new IllegalStateException(path + " safeToContinue was " + data.get("safeToContinue") + "; expected " + expectedSafe);
        }
        if (!Boolean.FALSE.equals(data.get("resourceRuntimeAccessed"))) {
            throw new IllegalStateException(path + " must not access runtime resources.");
        }
        if (!Boolean.FALSE.equals(data.get("registryInjected"))) {
            throw new IllegalStateException(path + " must not inject registries.");
        }
        if (!Boolean.FALSE.equals(data.get("registryMutated"))) {
            throw new IllegalStateException(path + " must not mutate registries.");
        }
        if (!Boolean.FALSE.equals(data.get("gameClassesResolved"))) {
            throw new IllegalStateException(path + " must not resolve game classes.");
        }
        verifyNoRuntimeWork(path, data);
    }

    private static void verifyPhase13M1Completion(String path, String expectedStatus, boolean expectedComplete) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        if (!"1970-01-01T00:00:00Z".equals(envelope.get("generatedAt"))) {
            throw new IllegalStateException(path + " has a nondeterministic generatedAt value.");
        }
        if (!expectedStatus.equals(envelope.get("status"))) {
            throw new IllegalStateException(path + " status was " + envelope.get("status") + "; expected " + expectedStatus);
        }
        if (!Boolean.valueOf(expectedComplete).equals(data.get("phase13M1Complete"))) {
            throw new IllegalStateException(path + " phase13M1Complete was " + data.get("phase13M1Complete") + "; expected " + expectedComplete);
        }
        verifyNoM2OrRuntimeWork(path, data);
    }

    private static void verifyPhase13M2Readiness(String path, String expectedStatus, boolean expectedReady) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        if (!"1970-01-01T00:00:00Z".equals(envelope.get("generatedAt"))) {
            throw new IllegalStateException(path + " has a nondeterministic generatedAt value.");
        }
        if (!expectedStatus.equals(envelope.get("status"))) {
            throw new IllegalStateException(path + " status was " + envelope.get("status") + "; expected " + expectedStatus);
        }
        if (!Boolean.valueOf(expectedReady).equals(data.get("phase13M2Ready"))) {
            throw new IllegalStateException(path + " phase13M2Ready was " + data.get("phase13M2Ready") + "; expected " + expectedReady);
        }
        verifyNoM2OrRuntimeWork(path, data);
    }

    private static void verifyPhase13PrototypeSafetyGate(String path, String expectedStatus, boolean expectedPassed) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        if (!"1970-01-01T00:00:00Z".equals(envelope.get("generatedAt"))) {
            throw new IllegalStateException(path + " has a nondeterministic generatedAt value.");
        }
        if (!expectedStatus.equals(envelope.get("status"))) {
            throw new IllegalStateException(path + " status was " + envelope.get("status") + "; expected " + expectedStatus);
        }
        if (!Boolean.valueOf(expectedPassed).equals(data.get("passed"))) {
            throw new IllegalStateException(path + " passed was " + data.get("passed") + "; expected " + expectedPassed);
        }
        verifyNoM2OrRuntimeWork(path, data);
    }

    private static void verifyMinecraftVersionResolverPlan(String path, String expectedStatus, boolean expectedReady) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        if (!"1970-01-01T00:00:00Z".equals(envelope.get("generatedAt"))) {
            throw new IllegalStateException(path + " has a nondeterministic generatedAt value.");
        }
        if (!expectedStatus.equals(envelope.get("status"))) {
            throw new IllegalStateException(path + " status was " + envelope.get("status") + "; expected " + expectedStatus);
        }
        if (!Boolean.TRUE.equals(data.get("planningOnly"))) {
            throw new IllegalStateException(path + " must remain planning-only.");
        }
        if (expectedReady && !"26.1.2".equals(data.get("targetMinecraftVersion"))) {
            throw new IllegalStateException(path + " targetMinecraftVersion was " + data.get("targetMinecraftVersion") + "; expected 26.1.2");
        }
        if (!Boolean.FALSE.equals(data.get("remoteManifestDownloaded"))) {
            throw new IllegalStateException(path + " must not download a remote Minecraft manifest.");
        }
        verifyNoM2OrRuntimeWork(path, data);
    }

    private static void verifyMinecraftVersionSourcePolicy(String path, String expectedStatus, boolean expectedReady) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        if (!"1970-01-01T00:00:00Z".equals(envelope.get("generatedAt"))) {
            throw new IllegalStateException(path + " has a nondeterministic generatedAt value.");
        }
        if (!expectedStatus.equals(envelope.get("status"))) {
            throw new IllegalStateException(path + " status was " + envelope.get("status") + "; expected " + expectedStatus);
        }
        if (!Boolean.TRUE.equals(data.get("localSourcesOnly"))) {
            throw new IllegalStateException(path + " must keep Minecraft version sources local-only.");
        }
        if (!Boolean.FALSE.equals(data.get("networkAllowed"))) {
            throw new IllegalStateException(path + " must not allow network version sources.");
        }
        if (!Boolean.FALSE.equals(data.get("remoteManifestDownloaded"))) {
            throw new IllegalStateException(path + " must not download a remote Minecraft manifest.");
        }
        if (!Boolean.FALSE.equals(data.get("cacheMutationAllowed"))) {
            throw new IllegalStateException(path + " must not allow cache mutation.");
        }
        verifyNoM2OrRuntimeWork(path, data);
    }

    private static void verifyMinecraftResolverSafetyStatus(String path, String expectedStatus, boolean expectedSafe) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        if (!"1970-01-01T00:00:00Z".equals(envelope.get("generatedAt"))) {
            throw new IllegalStateException(path + " has a nondeterministic generatedAt value.");
        }
        if (!expectedStatus.equals(envelope.get("status"))) {
            throw new IllegalStateException(path + " status was " + envelope.get("status") + "; expected " + expectedStatus);
        }
        if (!Boolean.valueOf(expectedSafe).equals(data.get("safeToContinue"))) {
            throw new IllegalStateException(path + " safeToContinue was " + data.get("safeToContinue") + "; expected " + expectedSafe);
        }
        if (!Boolean.FALSE.equals(data.get("remoteManifestDownloaded"))) {
            throw new IllegalStateException(path + " must not download a remote Minecraft manifest.");
        }
        verifyNoM2OrRuntimeWork(path, data);
    }

    private static void verifyLibraryResolutionPlan(String path, String expectedStatus, boolean expectedReady) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        if (!"1970-01-01T00:00:00Z".equals(envelope.get("generatedAt"))) {
            throw new IllegalStateException(path + " has a nondeterministic generatedAt value.");
        }
        if (!expectedStatus.equals(envelope.get("status"))) {
            throw new IllegalStateException(path + " status was " + envelope.get("status") + "; expected " + expectedStatus);
        }
        if (!Boolean.TRUE.equals(data.get("planningOnly"))) {
            throw new IllegalStateException(path + " must remain planning-only.");
        }
        Object plannedLibraryCount = data.get("plannedLibraryCount");
        if (expectedReady && plannedLibraryCount instanceof Number number && number.longValue() != 53L) {
            throw new IllegalStateException(path + " plannedLibraryCount was " + number + "; expected 53");
        }
        Object missingLibraryCount = data.get("missingLibraryCount");
        if (expectedReady && missingLibraryCount instanceof Number number && number.longValue() != 0L) {
            throw new IllegalStateException(path + " missingLibraryCount was " + number + "; expected 0");
        }
        verifyNoM3OrRuntimeWork(path, data);
    }

    private static void verifyLibrarySourcePolicy(String path, String expectedStatus, boolean expectedReady) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        if (!"1970-01-01T00:00:00Z".equals(envelope.get("generatedAt"))) {
            throw new IllegalStateException(path + " has a nondeterministic generatedAt value.");
        }
        if (!expectedStatus.equals(envelope.get("status"))) {
            throw new IllegalStateException(path + " status was " + envelope.get("status") + "; expected " + expectedStatus);
        }
        if (!Boolean.TRUE.equals(data.get("localManifestOnly"))) {
            throw new IllegalStateException(path + " must keep library sources fixture-local only.");
        }
        if (!Boolean.FALSE.equals(data.get("downloadsAllowed"))) {
            throw new IllegalStateException(path + " must not allow library downloads.");
        }
        if (!Boolean.FALSE.equals(data.get("remoteManifestAllowed"))) {
            throw new IllegalStateException(path + " must not allow remote manifests.");
        }
        if (!Boolean.FALSE.equals(data.get("cacheMutationAllowed"))) {
            throw new IllegalStateException(path + " must not allow cache mutation.");
        }
        verifyNoM3OrRuntimeWork(path, data);
    }

    private static void verifyLibraryResolverSafetyStatus(String path, String expectedStatus, boolean expectedSafe) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        if (!"1970-01-01T00:00:00Z".equals(envelope.get("generatedAt"))) {
            throw new IllegalStateException(path + " has a nondeterministic generatedAt value.");
        }
        if (!expectedStatus.equals(envelope.get("status"))) {
            throw new IllegalStateException(path + " status was " + envelope.get("status") + "; expected " + expectedStatus);
        }
        if (!Boolean.valueOf(expectedSafe).equals(data.get("safeToContinue"))) {
            throw new IllegalStateException(path + " safeToContinue was " + data.get("safeToContinue") + "; expected " + expectedSafe);
        }
        verifyNoM3OrRuntimeWork(path, data);
    }

    private static void verifyClasspathBuilderPlan(String path, String expectedStatus, boolean expectedReady) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        if (!"1970-01-01T00:00:00Z".equals(envelope.get("generatedAt"))) {
            throw new IllegalStateException(path + " has a nondeterministic generatedAt value.");
        }
        if (!expectedStatus.equals(envelope.get("status"))) {
            throw new IllegalStateException(path + " status was " + envelope.get("status") + "; expected " + expectedStatus);
        }
        if (!Boolean.TRUE.equals(data.get("planningOnly"))) {
            throw new IllegalStateException(path + " must remain planning-only.");
        }
        if (!Boolean.TRUE.equals(data.get("classpathEntriesPlannedOnly"))) {
            throw new IllegalStateException(path + " must keep classpath entries planned only.");
        }
        Object entryCount = data.get("entryCount");
        long expectedEntryCount = expectedAshfallClasspathEntryCount();
        if (expectedReady && entryCount instanceof Number number && number.longValue() != expectedEntryCount) {
            throw new IllegalStateException(path + " entryCount was " + number + "; expected " + expectedEntryCount);
        }
        verifyNoM4OrRuntimeWork(path, data);
    }

    private static void verifyClasspathSourcePolicy(String path, String expectedStatus, boolean expectedReady) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        if (!"1970-01-01T00:00:00Z".equals(envelope.get("generatedAt"))) {
            throw new IllegalStateException(path + " has a nondeterministic generatedAt value.");
        }
        if (!expectedStatus.equals(envelope.get("status"))) {
            throw new IllegalStateException(path + " status was " + envelope.get("status") + "; expected " + expectedStatus);
        }
        if (!Boolean.TRUE.equals(data.get("reportInputsOnly"))) {
            throw new IllegalStateException(path + " must use report inputs only.");
        }
        if (!Boolean.TRUE.equals(data.get("plannedEntriesOnly"))) {
            throw new IllegalStateException(path + " must keep entries planned only.");
        }
        if (!Boolean.FALSE.equals(data.get("classloaderCreationAllowed"))) {
            throw new IllegalStateException(path + " must not allow classloader creation.");
        }
        if (!Boolean.FALSE.equals(data.get("runtimeClassResolutionAllowed"))) {
            throw new IllegalStateException(path + " must not allow runtime class resolution.");
        }
        verifyNoM4OrRuntimeWork(path, data);
    }

    private static void verifyClasspathBuilderSafetyStatus(String path, String expectedStatus, boolean expectedSafe) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        if (!"1970-01-01T00:00:00Z".equals(envelope.get("generatedAt"))) {
            throw new IllegalStateException(path + " has a nondeterministic generatedAt value.");
        }
        if (!expectedStatus.equals(envelope.get("status"))) {
            throw new IllegalStateException(path + " status was " + envelope.get("status") + "; expected " + expectedStatus);
        }
        if (!Boolean.valueOf(expectedSafe).equals(data.get("safeToContinue"))) {
            throw new IllegalStateException(path + " safeToContinue was " + data.get("safeToContinue") + "; expected " + expectedSafe);
        }
        if (!Boolean.TRUE.equals(data.get("classpathEntriesPlannedOnly"))) {
            throw new IllegalStateException(path + " must keep classpath entries planned only.");
        }
        verifyNoM4OrRuntimeWork(path, data);
    }

    private static void verifyNativeExtractionPlan(String path, String expectedStatus, boolean expectedReady) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        if (!"1970-01-01T00:00:00Z".equals(envelope.get("generatedAt"))) {
            throw new IllegalStateException(path + " has a nondeterministic generatedAt value.");
        }
        if (!expectedStatus.equals(envelope.get("status"))) {
            throw new IllegalStateException(path + " status was " + envelope.get("status") + "; expected " + expectedStatus);
        }
        if (!Boolean.TRUE.equals(data.get("planningOnly"))) {
            throw new IllegalStateException(path + " must remain planning-only.");
        }
        if (!Boolean.FALSE.equals(data.get("nativeExtractionAllowed"))) {
            throw new IllegalStateException(path + " must not allow native extraction.");
        }
        if (!Boolean.FALSE.equals(data.get("nativeFilesExtracted"))) {
            throw new IllegalStateException(path + " must not extract native files.");
        }
        Object entryCount = data.get("entryCount");
        if (expectedReady && entryCount instanceof Number number && number.longValue() != 1L) {
            throw new IllegalStateException(path + " entryCount was " + number + "; expected 1");
        }
        verifyNoM5OrRuntimeWork(path, data);
    }

    private static void verifyNativeExtractionSourcePolicy(String path, String expectedStatus, boolean expectedReady) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        if (!"1970-01-01T00:00:00Z".equals(envelope.get("generatedAt"))) {
            throw new IllegalStateException(path + " has a nondeterministic generatedAt value.");
        }
        if (!expectedStatus.equals(envelope.get("status"))) {
            throw new IllegalStateException(path + " status was " + envelope.get("status") + "; expected " + expectedStatus);
        }
        if (!Boolean.TRUE.equals(data.get("reportInputsOnly"))) {
            throw new IllegalStateException(path + " must use report inputs only.");
        }
        if (!Boolean.FALSE.equals(data.get("nativeExtractionAllowed"))) {
            throw new IllegalStateException(path + " must not allow native extraction.");
        }
        if (!Boolean.FALSE.equals(data.get("runtimeNativeLookupAllowed"))) {
            throw new IllegalStateException(path + " must not allow runtime native lookup.");
        }
        if (!Boolean.FALSE.equals(data.get("filesystemMutationAllowed"))) {
            throw new IllegalStateException(path + " must not allow filesystem mutation.");
        }
        verifyNoM5OrRuntimeWork(path, data);
    }

    private static void verifyNativeExtractionSafetyStatus(String path, String expectedStatus, boolean expectedSafe) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        if (!"1970-01-01T00:00:00Z".equals(envelope.get("generatedAt"))) {
            throw new IllegalStateException(path + " has a nondeterministic generatedAt value.");
        }
        if (!expectedStatus.equals(envelope.get("status"))) {
            throw new IllegalStateException(path + " status was " + envelope.get("status") + "; expected " + expectedStatus);
        }
        if (!Boolean.valueOf(expectedSafe).equals(data.get("safeToContinue"))) {
            throw new IllegalStateException(path + " safeToContinue was " + data.get("safeToContinue") + "; expected " + expectedSafe);
        }
        if (!Boolean.FALSE.equals(data.get("nativeExtractionAllowed"))) {
            throw new IllegalStateException(path + " must not allow native extraction.");
        }
        if (!Boolean.FALSE.equals(data.get("nativeFilesExtracted"))) {
            throw new IllegalStateException(path + " must not extract native files.");
        }
        verifyNoM5OrRuntimeWork(path, data);
    }

    private static void verifyLaunchArgumentPlan(String path, String expectedStatus, boolean expectedReady) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        if (!"1970-01-01T00:00:00Z".equals(envelope.get("generatedAt"))) {
            throw new IllegalStateException(path + " has a nondeterministic generatedAt value.");
        }
        if (!expectedStatus.equals(envelope.get("status"))) {
            throw new IllegalStateException(path + " status was " + envelope.get("status") + "; expected " + expectedStatus);
        }
        if (!Boolean.TRUE.equals(data.get("planningOnly"))) {
            throw new IllegalStateException(path + " must remain planning-only.");
        }
        if (!Boolean.TRUE.equals(data.get("launchArgumentsPlannedOnly"))) {
            throw new IllegalStateException(path + " must keep launch arguments planned only.");
        }
        Object argumentCount = data.get("argumentCount");
        if (expectedReady && argumentCount instanceof Number number && number.longValue() != 12L) {
            throw new IllegalStateException(path + " argumentCount was " + number + "; expected 12");
        }
        if (expectedReady && !"26.1.2".equals(data.get("targetMinecraftVersion"))) {
            throw new IllegalStateException(path + " targetMinecraftVersion was " + data.get("targetMinecraftVersion") + "; expected 26.1.2");
        }
        verifyNoM6OrRuntimeWork(path, data);
    }

    private static void verifyLaunchArgumentSourcePolicy(String path, String expectedStatus, boolean expectedReady) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        if (!"1970-01-01T00:00:00Z".equals(envelope.get("generatedAt"))) {
            throw new IllegalStateException(path + " has a nondeterministic generatedAt value.");
        }
        if (!expectedStatus.equals(envelope.get("status"))) {
            throw new IllegalStateException(path + " status was " + envelope.get("status") + "; expected " + expectedStatus);
        }
        if (!Boolean.TRUE.equals(data.get("reportInputsOnly"))) {
            throw new IllegalStateException(path + " must use report inputs only.");
        }
        if (!Boolean.TRUE.equals(data.get("launchArgumentsPlannedOnly"))) {
            throw new IllegalStateException(path + " must keep launch arguments planned only.");
        }
        if (!Boolean.FALSE.equals(data.get("commandExecutionAllowed"))) {
            throw new IllegalStateException(path + " must not allow command execution.");
        }
        if (!Boolean.FALSE.equals(data.get("processLaunchAllowed"))) {
            throw new IllegalStateException(path + " must not allow process launch.");
        }
        verifyNoM6OrRuntimeWork(path, data);
    }

    private static void verifyLaunchArgumentSafetyStatus(String path, String expectedStatus, boolean expectedSafe) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        if (!"1970-01-01T00:00:00Z".equals(envelope.get("generatedAt"))) {
            throw new IllegalStateException(path + " has a nondeterministic generatedAt value.");
        }
        if (!expectedStatus.equals(envelope.get("status"))) {
            throw new IllegalStateException(path + " status was " + envelope.get("status") + "; expected " + expectedStatus);
        }
        if (!Boolean.valueOf(expectedSafe).equals(data.get("safeToContinue"))) {
            throw new IllegalStateException(path + " safeToContinue was " + data.get("safeToContinue") + "; expected " + expectedSafe);
        }
        if (!Boolean.TRUE.equals(data.get("launchArgumentsPlannedOnly"))) {
            throw new IllegalStateException(path + " must keep launch arguments planned only.");
        }
        verifyNoM6OrRuntimeWork(path, data);
    }

    private static void verifyControlledDummyProcessPlan(String path, String expectedStatus, boolean expectedReady) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        if (!"1970-01-01T00:00:00Z".equals(envelope.get("generatedAt"))) {
            throw new IllegalStateException(path + " has a nondeterministic generatedAt value.");
        }
        if (!expectedStatus.equals(envelope.get("status"))) {
            throw new IllegalStateException(path + " status was " + envelope.get("status") + "; expected " + expectedStatus);
        }
        if (!Boolean.TRUE.equals(data.get("dummyProcessOnly"))) {
            throw new IllegalStateException(path + " must be dummy-process only.");
        }
        if (!Boolean.valueOf(expectedReady).equals(data.get("ready"))) {
            throw new IllegalStateException(path + " ready was " + data.get("ready") + "; expected " + expectedReady);
        }
        if (expectedReady && !Boolean.TRUE.equals(data.get("processLaunchAllowed"))) {
            throw new IllegalStateException(path + " must allow only the controlled dummy process launch.");
        }
        verifyNoM7UnsafeGameWork(path, data);
    }

    private static void verifyControlledDummyProcessResult(String path, String expectedStatus, boolean expectedRan) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        if (!"1970-01-01T00:00:00Z".equals(envelope.get("generatedAt"))) {
            throw new IllegalStateException(path + " has a nondeterministic generatedAt value.");
        }
        if (!expectedStatus.equals(envelope.get("status"))) {
            throw new IllegalStateException(path + " status was " + envelope.get("status") + "; expected " + expectedStatus);
        }
        if (!Boolean.TRUE.equals(data.get("dummyProcessOnly"))) {
            throw new IllegalStateException(path + " must be dummy-process only.");
        }
        if (!Boolean.valueOf(expectedRan).equals(data.get("dummyProcessLaunched"))) {
            throw new IllegalStateException(path + " dummyProcessLaunched was " + data.get("dummyProcessLaunched") + "; expected " + expectedRan);
        }
        if (!Boolean.valueOf(expectedRan).equals(data.get("processLaunched"))) {
            throw new IllegalStateException(path + " processLaunched was " + data.get("processLaunched") + "; expected " + expectedRan);
        }
        Object exitCode = data.get("exitCode");
        if (expectedRan && (!(exitCode instanceof Number number) || number.longValue() != 0L)) {
            throw new IllegalStateException(path + " exitCode was " + data.get("exitCode") + "; expected 0");
        }
        verifyNoM7UnsafeGameWork(path, data);
    }

    private static void verifyDummyProcessCrashBoundary(String path, String expectedStatus, boolean expectedVerified) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        if (!"1970-01-01T00:00:00Z".equals(envelope.get("generatedAt"))) {
            throw new IllegalStateException(path + " has a nondeterministic generatedAt value.");
        }
        if (!expectedStatus.equals(envelope.get("status"))) {
            throw new IllegalStateException(path + " status was " + envelope.get("status") + "; expected " + expectedStatus);
        }
        if (!Boolean.valueOf(expectedVerified).equals(data.get("verified"))) {
            throw new IllegalStateException(path + " verified was " + data.get("verified") + "; expected " + expectedVerified);
        }
        if (expectedVerified && (!Boolean.TRUE.equals(data.get("stdoutCaptured")) || !Boolean.TRUE.equals(data.get("stderrCaptured")))) {
            throw new IllegalStateException(path + " must capture stdout and stderr.");
        }
        verifyNoM7UnsafeGameWork(path, data);
    }

    private static void verifyDummyProcessOutputCapture(String path, String expectedStatus, boolean expectedCaptured) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        if (!"1970-01-01T00:00:00Z".equals(envelope.get("generatedAt"))) {
            throw new IllegalStateException(path + " has a nondeterministic generatedAt value.");
        }
        if (!expectedStatus.equals(envelope.get("status"))) {
            throw new IllegalStateException(path + " status was " + envelope.get("status") + "; expected " + expectedStatus);
        }
        if (!Boolean.valueOf(expectedCaptured).equals(data.get("captured"))) {
            throw new IllegalStateException(path + " captured was " + data.get("captured") + "; expected " + expectedCaptured);
        }
        if (expectedCaptured) {
            Object stdoutLineCount = data.get("stdoutLineCount");
            if (!(stdoutLineCount instanceof Number stdoutNumber) || stdoutNumber.longValue() != 1L) {
                throw new IllegalStateException(path + " stdoutLineCount was " + data.get("stdoutLineCount") + "; expected 1");
            }
            Object stderrLineCount = data.get("stderrLineCount");
            if (!(stderrLineCount instanceof Number stderrNumber) || stderrNumber.longValue() != 1L) {
                throw new IllegalStateException(path + " stderrLineCount was " + data.get("stderrLineCount") + "; expected 1");
            }
            if (!Boolean.TRUE.equals(data.get("deterministic")) || !Boolean.TRUE.equals(data.get("secretSafe"))) {
                throw new IllegalStateException(path + " output capture must be deterministic and secret-safe.");
            }
        }
        verifyNoM7UnsafeGameWork(path, data);
    }

    private static void verifyAddonRuntimeDiscoveryPlan(String path, String expectedStatus, boolean expectedReady) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        if (!"1970-01-01T00:00:00Z".equals(envelope.get("generatedAt"))) {
            throw new IllegalStateException(path + " has a nondeterministic generatedAt value.");
        }
        if (!expectedStatus.equals(envelope.get("status"))) {
            throw new IllegalStateException(path + " status was " + envelope.get("status") + "; expected " + expectedStatus);
        }
        if (!Boolean.TRUE.equals(data.get("dataOnly"))) {
            throw new IllegalStateException(path + " must be data-only.");
        }
        if (!Boolean.TRUE.equals(data.get("deterministicOrder"))) {
            throw new IllegalStateException(path + " must require deterministic descriptor order.");
        }
        if (!Boolean.valueOf(expectedReady).equals(data.get("ready"))) {
            throw new IllegalStateException(path + " ready was " + data.get("ready") + "; expected " + expectedReady);
        }
        verifyNoM8UnsafeGameWork(path, data);
    }

    private static void verifyAddonRuntimeDescriptors(String path, String expectedStatus, boolean expectedComplete) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        if (!"1970-01-01T00:00:00Z".equals(envelope.get("generatedAt"))) {
            throw new IllegalStateException(path + " has a nondeterministic generatedAt value.");
        }
        if (!expectedStatus.equals(envelope.get("status"))) {
            throw new IllegalStateException(path + " status was " + envelope.get("status") + "; expected " + expectedStatus);
        }
        if (!Boolean.valueOf(expectedComplete).equals(data.get("complete"))) {
            throw new IllegalStateException(path + " complete was " + data.get("complete") + "; expected " + expectedComplete);
        }
        if (!Boolean.TRUE.equals(data.get("descriptorDataOnly"))) {
            throw new IllegalStateException(path + " must keep descriptors as data only.");
        }
        Object descriptorCount = data.get("descriptorCount");
        long expectedModuleCount = expectedAshfallRuntimeModuleCount();
        if (expectedComplete && (!(descriptorCount instanceof Number number) || number.longValue() != expectedModuleCount)) {
            throw new IllegalStateException(path + " descriptorCount was " + data.get("descriptorCount") + "; expected " + expectedModuleCount);
        }
        List<String> orderedIds = EchoNativeJson.stringList(data.get("orderedModuleIds"));
        List<String> sortedIds = orderedIds.stream().sorted().toList();
        if (!orderedIds.equals(sortedIds)) {
            throw new IllegalStateException(path + " orderedModuleIds must be sorted deterministically.");
        }
        verifyNoM8UnsafeGameWork(path, data);
    }

    private static void verifyAddonRuntimeDiscoverySafetyStatus(String path, String expectedStatus, boolean expectedSafe) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        if (!"1970-01-01T00:00:00Z".equals(envelope.get("generatedAt"))) {
            throw new IllegalStateException(path + " has a nondeterministic generatedAt value.");
        }
        if (!expectedStatus.equals(envelope.get("status"))) {
            throw new IllegalStateException(path + " status was " + envelope.get("status") + "; expected " + expectedStatus);
        }
        if (!Boolean.valueOf(expectedSafe).equals(data.get("safeToContinue"))) {
            throw new IllegalStateException(path + " safeToContinue was " + data.get("safeToContinue") + "; expected " + expectedSafe);
        }
        if (!Boolean.TRUE.equals(data.get("dataOnly"))) {
            throw new IllegalStateException(path + " must be data-only.");
        }
        verifyNoM8UnsafeGameWork(path, data);
    }

    private static void verifyLifecycleStubExecutionPlan(String path, String expectedStatus, boolean expectedReady) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        if (!"1970-01-01T00:00:00Z".equals(envelope.get("generatedAt"))) {
            throw new IllegalStateException(path + " has a nondeterministic generatedAt value.");
        }
        if (!expectedStatus.equals(envelope.get("status"))) {
            throw new IllegalStateException(path + " status was " + envelope.get("status") + "; expected " + expectedStatus);
        }
        if (!Boolean.TRUE.equals(data.get("stubOnly"))) {
            throw new IllegalStateException(path + " must be stub-only.");
        }
        if (!Boolean.TRUE.equals(data.get("inertHandlersOnly"))) {
            throw new IllegalStateException(path + " must allow only inert handlers.");
        }
        if (!Boolean.valueOf(expectedReady).equals(data.get("ready"))) {
            throw new IllegalStateException(path + " ready was " + data.get("ready") + "; expected " + expectedReady);
        }
        long expectedModuleCount = expectedAshfallRuntimeModuleCount();
        if (expectedReady && EchoNativeJson.stringList(data.get("moduleOrder")).size() != expectedModuleCount) {
            throw new IllegalStateException(path + " moduleOrder must contain " + expectedModuleCount + " staged Ashfall beta-profile runtime modules.");
        }
        verifyNoM9UnsafeGameWork(path, data);
    }

    private static void verifyLifecycleStubExecutionResult(String path, String expectedStatus, boolean expectedExecuted) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        if (!"1970-01-01T00:00:00Z".equals(envelope.get("generatedAt"))) {
            throw new IllegalStateException(path + " has a nondeterministic generatedAt value.");
        }
        if (!expectedStatus.equals(envelope.get("status"))) {
            throw new IllegalStateException(path + " status was " + envelope.get("status") + "; expected " + expectedStatus);
        }
        if (!Boolean.TRUE.equals(data.get("stubOnly"))) {
            throw new IllegalStateException(path + " must be stub-only.");
        }
        if (!Boolean.valueOf(expectedExecuted).equals(data.get("executed"))) {
            throw new IllegalStateException(path + " executed was " + data.get("executed") + "; expected " + expectedExecuted);
        }
        if (expectedExecuted) {
            Object moduleCount = data.get("moduleCount");
            Object stubHandlerCount = data.get("stubHandlerCount");
            long expectedModuleCount = expectedAshfallRuntimeModuleCount();
            long expectedStubHandlerCount = expectedModuleCount * 5L;
            if (!(moduleCount instanceof Number moduleNumber) || moduleNumber.longValue() != expectedModuleCount) {
                throw new IllegalStateException(path + " moduleCount was " + moduleCount + "; expected " + expectedModuleCount);
            }
            if (!(stubHandlerCount instanceof Number stubNumber) || stubNumber.longValue() != expectedStubHandlerCount) {
                throw new IllegalStateException(path + " stubHandlerCount was " + stubHandlerCount + "; expected " + expectedStubHandlerCount);
            }
            if (!Boolean.TRUE.equals(data.get("inertStubHandlersExecuted"))) {
                throw new IllegalStateException(path + " must execute inert stub handlers.");
            }
        }
        verifyNoM9UnsafeGameWork(path, data);
    }

    private static void verifyLifecycleStubCrashBoundary(String path, String expectedStatus, boolean expectedVerified) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        if (!"1970-01-01T00:00:00Z".equals(envelope.get("generatedAt"))) {
            throw new IllegalStateException(path + " has a nondeterministic generatedAt value.");
        }
        if (!expectedStatus.equals(envelope.get("status"))) {
            throw new IllegalStateException(path + " status was " + envelope.get("status") + "; expected " + expectedStatus);
        }
        if (!Boolean.valueOf(expectedVerified).equals(data.get("verified"))) {
            throw new IllegalStateException(path + " verified was " + data.get("verified") + "; expected " + expectedVerified);
        }
        if (expectedVerified && (!Boolean.TRUE.equals(data.get("stubFailureContained")) || !Boolean.TRUE.equals(data.get("shutdownBoundaryValidated")))) {
            throw new IllegalStateException(path + " must contain stub failures and validate shutdown boundary.");
        }
        verifyNoM9UnsafeGameWork(path, data);
    }

    private static void verifyLifecycleStubSafetyStatus(String path, String expectedStatus, boolean expectedSafe) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        if (!"1970-01-01T00:00:00Z".equals(envelope.get("generatedAt"))) {
            throw new IllegalStateException(path + " has a nondeterministic generatedAt value.");
        }
        if (!expectedStatus.equals(envelope.get("status"))) {
            throw new IllegalStateException(path + " status was " + envelope.get("status") + "; expected " + expectedStatus);
        }
        if (!Boolean.valueOf(expectedSafe).equals(data.get("safeToContinue"))) {
            throw new IllegalStateException(path + " safeToContinue was " + data.get("safeToContinue") + "; expected " + expectedSafe);
        }
        if (!Boolean.TRUE.equals(data.get("stubOnly")) || !Boolean.TRUE.equals(data.get("inertHandlersOnly"))) {
            throw new IllegalStateException(path + " must stay stub-only and inert.");
        }
        verifyNoM9UnsafeGameWork(path, data);
    }

    private static void verifyServiceBusPlan(String path, String expectedStatus, boolean expectedReady) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        if (!"1970-01-01T00:00:00Z".equals(envelope.get("generatedAt"))) {
            throw new IllegalStateException(path + " has a nondeterministic generatedAt value.");
        }
        if (!expectedStatus.equals(envelope.get("status"))) {
            throw new IllegalStateException(path + " status was " + envelope.get("status") + "; expected " + expectedStatus);
        }
        if (!Boolean.TRUE.equals(data.get("inMemoryOnly")) || !Boolean.TRUE.equals(data.get("inertHandlesOnly"))) {
            throw new IllegalStateException(path + " must stay in-memory and inert.");
        }
        if (!Boolean.valueOf(expectedReady).equals(data.get("ready"))) {
            throw new IllegalStateException(path + " ready was " + data.get("ready") + "; expected " + expectedReady);
        }
        if (expectedReady) {
            Object plannedServiceCount = data.get("plannedServiceCount");
            long expectedServiceCount = expectedAshfallRuntimeModuleCount();
            if (!(plannedServiceCount instanceof Number number) || number.longValue() != expectedServiceCount) {
                throw new IllegalStateException(path + " plannedServiceCount was " + plannedServiceCount + "; expected " + expectedServiceCount);
            }
        }
        verifyNoM10UnsafeGameWork(path, data);
    }

    private static void verifyServiceBusRegistry(String path, String expectedStatus, boolean expectedRegistered) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        if (!"1970-01-01T00:00:00Z".equals(envelope.get("generatedAt"))) {
            throw new IllegalStateException(path + " has a nondeterministic generatedAt value.");
        }
        if (!expectedStatus.equals(envelope.get("status"))) {
            throw new IllegalStateException(path + " status was " + envelope.get("status") + "; expected " + expectedStatus);
        }
        if (!Boolean.valueOf(expectedRegistered).equals(data.get("registered"))) {
            throw new IllegalStateException(path + " registered was " + data.get("registered") + "; expected " + expectedRegistered);
        }
        if (expectedRegistered) {
            Object serviceHandleCount = data.get("serviceHandleCount");
            long expectedServiceCount = expectedAshfallRuntimeModuleCount();
            if (!(serviceHandleCount instanceof Number number) || number.longValue() != expectedServiceCount) {
                throw new IllegalStateException(path + " serviceHandleCount was " + serviceHandleCount + "; expected " + expectedServiceCount);
            }
        }
        verifyNoM10UnsafeGameWork(path, data);
    }

    private static void verifyServiceBusSimulationResult(String path, String expectedStatus, boolean expectedSimulated) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        if (!"1970-01-01T00:00:00Z".equals(envelope.get("generatedAt"))) {
            throw new IllegalStateException(path + " has a nondeterministic generatedAt value.");
        }
        if (!expectedStatus.equals(envelope.get("status"))) {
            throw new IllegalStateException(path + " status was " + envelope.get("status") + "; expected " + expectedStatus);
        }
        if (!Boolean.valueOf(expectedSimulated).equals(data.get("simulated"))) {
            throw new IllegalStateException(path + " simulated was " + data.get("simulated") + "; expected " + expectedSimulated);
        }
        if (expectedSimulated) {
            Object registeredServiceCount = data.get("registeredServiceCount");
            Object blockedServiceCount = data.get("blockedServiceCount");
            long expectedServiceCount = expectedAshfallRuntimeModuleCount();
            if (!(registeredServiceCount instanceof Number registered) || registered.longValue() != expectedServiceCount) {
                throw new IllegalStateException(path + " registeredServiceCount was " + registeredServiceCount + "; expected " + expectedServiceCount);
            }
            if (!(blockedServiceCount instanceof Number blocked) || blocked.longValue() != 0L) {
                throw new IllegalStateException(path + " blockedServiceCount was " + blockedServiceCount + "; expected 0");
            }
        }
        verifyNoM10UnsafeGameWork(path, data);
    }

    private static void verifyServiceBusSafetyStatus(String path, String expectedStatus, boolean expectedSafe) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        if (!"1970-01-01T00:00:00Z".equals(envelope.get("generatedAt"))) {
            throw new IllegalStateException(path + " has a nondeterministic generatedAt value.");
        }
        if (!expectedStatus.equals(envelope.get("status"))) {
            throw new IllegalStateException(path + " status was " + envelope.get("status") + "; expected " + expectedStatus);
        }
        if (!Boolean.valueOf(expectedSafe).equals(data.get("safeToContinue"))) {
            throw new IllegalStateException(path + " safeToContinue was " + data.get("safeToContinue") + "; expected " + expectedSafe);
        }
        if (!Boolean.TRUE.equals(data.get("inMemoryOnly")) || !Boolean.TRUE.equals(data.get("inertHandlesOnly"))) {
            throw new IllegalStateException(path + " must stay in-memory and inert.");
        }
        verifyNoM10UnsafeGameWork(path, data);
    }

    private static void verifyConfigSourceInventory(String path, String expectedStatus, boolean expectedRead) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        if (!"1970-01-01T00:00:00Z".equals(envelope.get("generatedAt"))) {
            throw new IllegalStateException(path + " has a nondeterministic generatedAt value.");
        }
        if (!expectedStatus.equals(envelope.get("status"))) {
            throw new IllegalStateException(path + " status was " + envelope.get("status") + "; expected " + expectedStatus);
        }
        if (!Boolean.valueOf(expectedRead).equals(data.get("read"))) {
            throw new IllegalStateException(path + " read was " + data.get("read") + "; expected " + expectedRead);
        }
        if (expectedRead) {
            Object count = data.get("configSourceCount");
            if (!(count instanceof Number number) || number.longValue() != 3L) {
                throw new IllegalStateException(path + " configSourceCount was " + count + "; expected 3");
            }
        }
        verifyNoM11UnsafeConfigWork(path, data);
    }

    private static void verifyConfigValidationResult(String path, String expectedStatus, boolean expectedValid) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        if (!"1970-01-01T00:00:00Z".equals(envelope.get("generatedAt"))) {
            throw new IllegalStateException(path + " has a nondeterministic generatedAt value.");
        }
        if (!expectedStatus.equals(envelope.get("status"))) {
            throw new IllegalStateException(path + " status was " + envelope.get("status") + "; expected " + expectedStatus);
        }
        if (!Boolean.valueOf(expectedValid).equals(data.get("valid"))) {
            throw new IllegalStateException(path + " valid was " + data.get("valid") + "; expected " + expectedValid);
        }
        if (expectedValid) {
            Object count = data.get("validatedConfigCount");
            if (!(count instanceof Number number) || number.longValue() != 3L) {
                throw new IllegalStateException(path + " validatedConfigCount was " + count + "; expected 3");
            }
        }
        verifyNoM11UnsafeConfigWork(path, data);
    }

    private static void verifyConfigWritePlan(String path, String expectedStatus, boolean expectedReady) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        if (!"1970-01-01T00:00:00Z".equals(envelope.get("generatedAt"))) {
            throw new IllegalStateException(path + " has a nondeterministic generatedAt value.");
        }
        if (!expectedStatus.equals(envelope.get("status"))) {
            throw new IllegalStateException(path + " status was " + envelope.get("status") + "; expected " + expectedStatus);
        }
        if (!Boolean.valueOf(expectedReady).equals(data.get("ready"))) {
            throw new IllegalStateException(path + " ready was " + data.get("ready") + "; expected " + expectedReady);
        }
        if (!Boolean.TRUE.equals(data.get("writePlanOnly"))) {
            throw new IllegalStateException(path + " must be a write plan only.");
        }
        if (expectedReady) {
            Object count = data.get("plannedWriteCount");
            if (!(count instanceof Number number) || number.longValue() != 3L) {
                throw new IllegalStateException(path + " plannedWriteCount was " + count + "; expected 3");
            }
        }
        verifyNoM11UnsafeConfigWork(path, data);
    }

    private static void verifyConfigSafetyStatus(String path, String expectedStatus, boolean expectedSafe) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        if (!"1970-01-01T00:00:00Z".equals(envelope.get("generatedAt"))) {
            throw new IllegalStateException(path + " has a nondeterministic generatedAt value.");
        }
        if (!expectedStatus.equals(envelope.get("status"))) {
            throw new IllegalStateException(path + " status was " + envelope.get("status") + "; expected " + expectedStatus);
        }
        if (!Boolean.valueOf(expectedSafe).equals(data.get("safeToContinue"))) {
            throw new IllegalStateException(path + " safeToContinue was " + data.get("safeToContinue") + "; expected " + expectedSafe);
        }
        verifyNoM11UnsafeConfigWork(path, data);
    }

    private static void verifyResourceSourceInventory(String path, String expectedStatus, boolean expectedInventoried) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        if (!"1970-01-01T00:00:00Z".equals(envelope.get("generatedAt"))) {
            throw new IllegalStateException(path + " has a nondeterministic generatedAt value.");
        }
        if (!expectedStatus.equals(envelope.get("status"))) {
            throw new IllegalStateException(path + " status was " + envelope.get("status") + "; expected " + expectedStatus);
        }
        if (!Boolean.valueOf(expectedInventoried).equals(data.get("inventoried"))) {
            throw new IllegalStateException(path + " inventoried was " + data.get("inventoried") + "; expected " + expectedInventoried);
        }
        if (expectedInventoried) {
            Object count = data.get("resourceSourceCount");
            if (!(count instanceof Number number) || number.longValue() != 4L) {
                throw new IllegalStateException(path + " resourceSourceCount was " + count + "; expected 4");
            }
        }
        verifyNoM12UnsafeResourceWork(path, data);
    }

    private static void verifyResourceNamespaceValidation(String path, String expectedStatus, boolean expectedValid) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        if (!"1970-01-01T00:00:00Z".equals(envelope.get("generatedAt"))) {
            throw new IllegalStateException(path + " has a nondeterministic generatedAt value.");
        }
        if (!expectedStatus.equals(envelope.get("status"))) {
            throw new IllegalStateException(path + " status was " + envelope.get("status") + "; expected " + expectedStatus);
        }
        if (!Boolean.valueOf(expectedValid).equals(data.get("valid"))) {
            throw new IllegalStateException(path + " valid was " + data.get("valid") + "; expected " + expectedValid);
        }
        if (expectedValid) {
            Object namespaceCount = data.get("namespaceCount");
            Object resourceCount = data.get("validatedResourceCount");
            if (!(namespaceCount instanceof Number namespaces) || namespaces.longValue() != 1L) {
                throw new IllegalStateException(path + " namespaceCount was " + namespaceCount + "; expected 1");
            }
            if (!(resourceCount instanceof Number resources) || resources.longValue() != 4L) {
                throw new IllegalStateException(path + " validatedResourceCount was " + resourceCount + "; expected 4");
            }
        }
        verifyNoM12UnsafeResourceWork(path, data);
    }

    private static void verifyResourcePackOrderPlan(String path, String expectedStatus, boolean expectedPlanned) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        if (!"1970-01-01T00:00:00Z".equals(envelope.get("generatedAt"))) {
            throw new IllegalStateException(path + " has a nondeterministic generatedAt value.");
        }
        if (!expectedStatus.equals(envelope.get("status"))) {
            throw new IllegalStateException(path + " status was " + envelope.get("status") + "; expected " + expectedStatus);
        }
        if (!Boolean.valueOf(expectedPlanned).equals(data.get("planned"))) {
            throw new IllegalStateException(path + " planned was " + data.get("planned") + "; expected " + expectedPlanned);
        }
        if (expectedPlanned) {
            Object count = data.get("orderedResourceCount");
            if (!(count instanceof Number number) || number.longValue() != 4L) {
                throw new IllegalStateException(path + " orderedResourceCount was " + count + "; expected 4");
            }
        }
        verifyNoM12UnsafeResourceWork(path, data);
    }

    private static void verifyResourceConflictReport(String path, String expectedStatus, boolean expectedConflictFree) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        if (!"1970-01-01T00:00:00Z".equals(envelope.get("generatedAt"))) {
            throw new IllegalStateException(path + " has a nondeterministic generatedAt value.");
        }
        if (!expectedStatus.equals(envelope.get("status"))) {
            throw new IllegalStateException(path + " status was " + envelope.get("status") + "; expected " + expectedStatus);
        }
        if (!Boolean.valueOf(expectedConflictFree).equals(data.get("conflictFree"))) {
            throw new IllegalStateException(path + " conflictFree was " + data.get("conflictFree") + "; expected " + expectedConflictFree);
        }
        if (expectedConflictFree) {
            Object count = data.get("conflictCount");
            if (!(count instanceof Number number) || number.longValue() != 0L) {
                throw new IllegalStateException(path + " conflictCount was " + count + "; expected 0");
            }
        }
        verifyNoM12UnsafeResourceWork(path, data);
    }

    private static void verifyResourceBridgeSafetyStatus(String path, String expectedStatus, boolean expectedSafe) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        if (!"1970-01-01T00:00:00Z".equals(envelope.get("generatedAt"))) {
            throw new IllegalStateException(path + " has a nondeterministic generatedAt value.");
        }
        if (!expectedStatus.equals(envelope.get("status"))) {
            throw new IllegalStateException(path + " status was " + envelope.get("status") + "; expected " + expectedStatus);
        }
        if (!Boolean.valueOf(expectedSafe).equals(data.get("safeToContinue"))) {
            throw new IllegalStateException(path + " safeToContinue was " + data.get("safeToContinue") + "; expected " + expectedSafe);
        }
        verifyNoM12UnsafeResourceWork(path, data);
    }

    private static void verifyRegistrySourceInventory(String path, String expectedStatus, boolean expectedInventoried) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        if (!"1970-01-01T00:00:00Z".equals(envelope.get("generatedAt"))) {
            throw new IllegalStateException(path + " has a nondeterministic generatedAt value.");
        }
        if (!expectedStatus.equals(envelope.get("status"))) {
            throw new IllegalStateException(path + " status was " + envelope.get("status") + "; expected " + expectedStatus);
        }
        if (!Boolean.valueOf(expectedInventoried).equals(data.get("inventoried"))) {
            throw new IllegalStateException(path + " inventoried was " + data.get("inventoried") + "; expected " + expectedInventoried);
        }
        if (expectedInventoried) {
            Object count = data.get("registrySourceCount");
            if (!(count instanceof Number number) || number.longValue() != 5L) {
                throw new IllegalStateException(path + " registrySourceCount was " + count + "; expected 5");
            }
        }
        verifyNoM13UnsafeRegistryWork(path, data);
    }

    private static void verifyRegistryIdValidation(String path, String expectedStatus, boolean expectedValid) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        if (!"1970-01-01T00:00:00Z".equals(envelope.get("generatedAt"))) {
            throw new IllegalStateException(path + " has a nondeterministic generatedAt value.");
        }
        if (!expectedStatus.equals(envelope.get("status"))) {
            throw new IllegalStateException(path + " status was " + envelope.get("status") + "; expected " + expectedStatus);
        }
        if (!Boolean.valueOf(expectedValid).equals(data.get("valid"))) {
            throw new IllegalStateException(path + " valid was " + data.get("valid") + "; expected " + expectedValid);
        }
        if (expectedValid) {
            Object kindCount = data.get("registryKindCount");
            Object entryCount = data.get("validatedEntryCount");
            if (!(kindCount instanceof Number kinds) || kinds.longValue() != 4L) {
                throw new IllegalStateException(path + " registryKindCount was " + kindCount + "; expected 4");
            }
            if (!(entryCount instanceof Number entries) || entries.longValue() != 5L) {
                throw new IllegalStateException(path + " validatedEntryCount was " + entryCount + "; expected 5");
            }
        }
        verifyNoM13UnsafeRegistryWork(path, data);
    }

    private static void verifySandboxRegistryModel(String path, String expectedStatus, boolean expectedModeled) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        if (!"1970-01-01T00:00:00Z".equals(envelope.get("generatedAt"))) {
            throw new IllegalStateException(path + " has a nondeterministic generatedAt value.");
        }
        if (!expectedStatus.equals(envelope.get("status"))) {
            throw new IllegalStateException(path + " status was " + envelope.get("status") + "; expected " + expectedStatus);
        }
        if (!Boolean.valueOf(expectedModeled).equals(data.get("modeled"))) {
            throw new IllegalStateException(path + " modeled was " + data.get("modeled") + "; expected " + expectedModeled);
        }
        if (expectedModeled) {
            Object kindCount = data.get("registryKindCount");
            Object entryCount = data.get("modeledEntryCount");
            if (!(kindCount instanceof Number kinds) || kinds.longValue() != 4L) {
                throw new IllegalStateException(path + " registryKindCount was " + kindCount + "; expected 4");
            }
            if (!(entryCount instanceof Number entries) || entries.longValue() != 5L) {
                throw new IllegalStateException(path + " modeledEntryCount was " + entryCount + "; expected 5");
            }
        }
        verifyNoM13UnsafeRegistryWork(path, data);
    }

    private static void verifyRegistryConflictReport(String path, String expectedStatus, boolean expectedConflictFree) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        if (!"1970-01-01T00:00:00Z".equals(envelope.get("generatedAt"))) {
            throw new IllegalStateException(path + " has a nondeterministic generatedAt value.");
        }
        if (!expectedStatus.equals(envelope.get("status"))) {
            throw new IllegalStateException(path + " status was " + envelope.get("status") + "; expected " + expectedStatus);
        }
        if (!Boolean.valueOf(expectedConflictFree).equals(data.get("conflictFree"))) {
            throw new IllegalStateException(path + " conflictFree was " + data.get("conflictFree") + "; expected " + expectedConflictFree);
        }
        if (expectedConflictFree) {
            Object count = data.get("conflictCount");
            if (!(count instanceof Number number) || number.longValue() != 0L) {
                throw new IllegalStateException(path + " conflictCount was " + count + "; expected 0");
            }
        }
        verifyNoM13UnsafeRegistryWork(path, data);
    }

    private static void verifyRegistryBridgeSafetyStatus(String path, String expectedStatus, boolean expectedSafe) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        if (!"1970-01-01T00:00:00Z".equals(envelope.get("generatedAt"))) {
            throw new IllegalStateException(path + " has a nondeterministic generatedAt value.");
        }
        if (!expectedStatus.equals(envelope.get("status"))) {
            throw new IllegalStateException(path + " status was " + envelope.get("status") + "; expected " + expectedStatus);
        }
        if (!Boolean.valueOf(expectedSafe).equals(data.get("safeToContinue"))) {
            throw new IllegalStateException(path + " safeToContinue was " + data.get("safeToContinue") + "; expected " + expectedSafe);
        }
        verifyNoM13UnsafeRegistryWork(path, data);
    }

    private static void verifyNetworkChannelInventory(String path, String expectedStatus, boolean expectedInventoried) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        if (!"1970-01-01T00:00:00Z".equals(envelope.get("generatedAt"))) {
            throw new IllegalStateException(path + " has a nondeterministic generatedAt value.");
        }
        if (!expectedStatus.equals(envelope.get("status"))) {
            throw new IllegalStateException(path + " status was " + envelope.get("status") + "; expected " + expectedStatus);
        }
        if (!Boolean.valueOf(expectedInventoried).equals(data.get("inventoried"))) {
            throw new IllegalStateException(path + " inventoried was " + data.get("inventoried") + "; expected " + expectedInventoried);
        }
        if (expectedInventoried) {
            Object count = data.get("channelCount");
            if (!(count instanceof Number number) || number.longValue() != 2L) {
                throw new IllegalStateException(path + " channelCount was " + count + "; expected 2");
            }
        }
        verifyNoM14UnsafeNetworkWork(path, data);
    }

    private static void verifyNetworkPacketValidation(String path, String expectedStatus, boolean expectedValid) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        if (!"1970-01-01T00:00:00Z".equals(envelope.get("generatedAt"))) {
            throw new IllegalStateException(path + " has a nondeterministic generatedAt value.");
        }
        if (!expectedStatus.equals(envelope.get("status"))) {
            throw new IllegalStateException(path + " status was " + envelope.get("status") + "; expected " + expectedStatus);
        }
        if (!Boolean.valueOf(expectedValid).equals(data.get("valid"))) {
            throw new IllegalStateException(path + " valid was " + data.get("valid") + "; expected " + expectedValid);
        }
        if (expectedValid) {
            Object channelCount = data.get("channelCount");
            Object packetCount = data.get("packetCount");
            if (!(channelCount instanceof Number channels) || channels.longValue() != 2L) {
                throw new IllegalStateException(path + " channelCount was " + channelCount + "; expected 2");
            }
            if (!(packetCount instanceof Number packets) || packets.longValue() != 3L) {
                throw new IllegalStateException(path + " packetCount was " + packetCount + "; expected 3");
            }
        }
        verifyNoM14UnsafeNetworkWork(path, data);
    }

    private static void verifyNetworkSchemaModel(String path, String expectedStatus, boolean expectedModeled) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        if (!"1970-01-01T00:00:00Z".equals(envelope.get("generatedAt"))) {
            throw new IllegalStateException(path + " has a nondeterministic generatedAt value.");
        }
        if (!expectedStatus.equals(envelope.get("status"))) {
            throw new IllegalStateException(path + " status was " + envelope.get("status") + "; expected " + expectedStatus);
        }
        if (!Boolean.valueOf(expectedModeled).equals(data.get("modeled"))) {
            throw new IllegalStateException(path + " modeled was " + data.get("modeled") + "; expected " + expectedModeled);
        }
        if (expectedModeled) {
            Object schemaCount = data.get("schemaCount");
            if (!(schemaCount instanceof Number schemas) || schemas.longValue() != 3L) {
                throw new IllegalStateException(path + " schemaCount was " + schemaCount + "; expected 3");
            }
        }
        verifyNoM14UnsafeNetworkWork(path, data);
    }

    private static void verifyNetworkConflictReport(String path, String expectedStatus, boolean expectedConflictFree) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        if (!"1970-01-01T00:00:00Z".equals(envelope.get("generatedAt"))) {
            throw new IllegalStateException(path + " has a nondeterministic generatedAt value.");
        }
        if (!expectedStatus.equals(envelope.get("status"))) {
            throw new IllegalStateException(path + " status was " + envelope.get("status") + "; expected " + expectedStatus);
        }
        if (!Boolean.valueOf(expectedConflictFree).equals(data.get("conflictFree"))) {
            throw new IllegalStateException(path + " conflictFree was " + data.get("conflictFree") + "; expected " + expectedConflictFree);
        }
        if (expectedConflictFree) {
            Object count = data.get("conflictCount");
            if (!(count instanceof Number number) || number.longValue() != 0L) {
                throw new IllegalStateException(path + " conflictCount was " + count + "; expected 0");
            }
        }
        verifyNoM14UnsafeNetworkWork(path, data);
    }

    private static void verifyNetworkBridgeSafetyStatus(String path, String expectedStatus, boolean expectedSafe) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        if (!"1970-01-01T00:00:00Z".equals(envelope.get("generatedAt"))) {
            throw new IllegalStateException(path + " has a nondeterministic generatedAt value.");
        }
        if (!expectedStatus.equals(envelope.get("status"))) {
            throw new IllegalStateException(path + " status was " + envelope.get("status") + "; expected " + expectedStatus);
        }
        if (!Boolean.valueOf(expectedSafe).equals(data.get("safeToContinue"))) {
            throw new IllegalStateException(path + " safeToContinue was " + data.get("safeToContinue") + "; expected " + expectedSafe);
        }
        verifyNoM14UnsafeNetworkWork(path, data);
    }

    private static void verifyTransformSourceInventory(String path, String expectedStatus, boolean expectedInventoried) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        verifyTransformEnvelope(path, envelope, expectedStatus);
        if (!Boolean.valueOf(expectedInventoried).equals(data.get("inventoried"))) {
            throw new IllegalStateException(path + " inventoried was " + data.get("inventoried") + "; expected " + expectedInventoried);
        }
        if (expectedInventoried) {
            Object count = data.get("transformSourceCount");
            if (!(count instanceof Number number) || number.longValue() != 2L) {
                throw new IllegalStateException(path + " transformSourceCount was " + count + "; expected 2");
            }
        }
        verifyNoM15UnsafeTransformWork(path, data);
    }

    private static void verifyTransformAllowlistValidation(String path, String expectedStatus, boolean expectedValid) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        verifyTransformEnvelope(path, envelope, expectedStatus);
        if (!Boolean.valueOf(expectedValid).equals(data.get("valid"))) {
            throw new IllegalStateException(path + " valid was " + data.get("valid") + "; expected " + expectedValid);
        }
        if (expectedValid) {
            Object count = data.get("transformCount");
            if (!(count instanceof Number number) || number.longValue() != 2L) {
                throw new IllegalStateException(path + " transformCount was " + count + "; expected 2");
            }
        }
        verifyNoM15UnsafeTransformWork(path, data);
    }

    private static void verifyTransformPipelinePlan(String path, String expectedStatus, boolean expectedPlanned) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        verifyTransformEnvelope(path, envelope, expectedStatus);
        if (!Boolean.valueOf(expectedPlanned).equals(data.get("planned"))) {
            throw new IllegalStateException(path + " planned was " + data.get("planned") + "; expected " + expectedPlanned);
        }
        if (expectedPlanned) {
            Object count = data.get("plannedTransformCount");
            if (!(count instanceof Number number) || number.longValue() != 2L) {
                throw new IllegalStateException(path + " plannedTransformCount was " + count + "; expected 2");
            }
        }
        verifyNoM15UnsafeTransformWork(path, data);
    }

    private static void verifyTransformConflictReport(String path, String expectedStatus, boolean expectedConflictFree) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        verifyTransformEnvelope(path, envelope, expectedStatus);
        if (!Boolean.valueOf(expectedConflictFree).equals(data.get("conflictFree"))) {
            throw new IllegalStateException(path + " conflictFree was " + data.get("conflictFree") + "; expected " + expectedConflictFree);
        }
        if (expectedConflictFree) {
            Object count = data.get("conflictCount");
            if (!(count instanceof Number number) || number.longValue() != 0L) {
                throw new IllegalStateException(path + " conflictCount was " + count + "; expected 0");
            }
        }
        verifyNoM15UnsafeTransformWork(path, data);
    }

    private static void verifyTransformSafetyStatus(String path, String expectedStatus, boolean expectedSafe) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        verifyTransformEnvelope(path, envelope, expectedStatus);
        if (!Boolean.valueOf(expectedSafe).equals(data.get("safeToContinue"))) {
            throw new IllegalStateException(path + " safeToContinue was " + data.get("safeToContinue") + "; expected " + expectedSafe);
        }
        verifyNoM15UnsafeTransformWork(path, data);
    }

    private static void verifyCrashHardeningCoverage(String path, String expectedStatus, boolean expectedCovered) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        verifyM16Envelope(path, envelope, expectedStatus);
        if (!Boolean.valueOf(expectedCovered).equals(data.get("covered"))) {
            throw new IllegalStateException(path + " covered was " + data.get("covered") + "; expected " + expectedCovered);
        }
        if (expectedCovered) {
            Object count = data.get("coveredReportCount");
            if (!(count instanceof Number number) || number.longValue() != 13L) {
                throw new IllegalStateException(path + " coveredReportCount was " + count + "; expected 13");
            }
        }
        verifyNoM16UnsafeCrashHardeningWork(path, data);
    }

    private static void verifyFailureContainmentMatrix(String path, String expectedStatus, boolean expectedContained) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        verifyM16Envelope(path, envelope, expectedStatus);
        if (!Boolean.valueOf(expectedContained).equals(data.get("contained"))) {
            throw new IllegalStateException(path + " contained was " + data.get("contained") + "; expected " + expectedContained);
        }
        if (expectedContained) {
            Object count = data.get("failureCaseCount");
            if (!(count instanceof Number number) || number.longValue() != 9L) {
                throw new IllegalStateException(path + " failureCaseCount was " + count + "; expected 9");
            }
        }
        verifyNoM16UnsafeCrashHardeningWork(path, data);
    }

    private static void verifySupportBundleDryRunPlan(String path, String expectedStatus, boolean expectedPlanned) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        verifyM16Envelope(path, envelope, expectedStatus);
        if (!Boolean.valueOf(expectedPlanned).equals(data.get("planned"))) {
            throw new IllegalStateException(path + " planned was " + data.get("planned") + "; expected " + expectedPlanned);
        }
        if (!Boolean.TRUE.equals(data.get("supportBundlePlannedOnly"))) {
            throw new IllegalStateException(path + " must keep support bundle output planned only.");
        }
        Object bundleWritten = data.get("bundleWritten");
        if (bundleWritten != null && !Boolean.FALSE.equals(bundleWritten)) {
            throw new IllegalStateException(path + " must not write a support bundle.");
        }
        verifyNoM16UnsafeCrashHardeningWork(path, data);
    }

    private static void verifyPhase13M16SafetyStatus(String path, String expectedStatus, boolean expectedSafe) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        verifyM16Envelope(path, envelope, expectedStatus);
        if (!Boolean.valueOf(expectedSafe).equals(data.get("safeToContinue"))) {
            throw new IllegalStateException(path + " safeToContinue was " + data.get("safeToContinue") + "; expected " + expectedSafe);
        }
        if (!Boolean.TRUE.equals(data.get("diagnosticsCaptured"))) {
            throw new IllegalStateException(path + " must prove diagnostics were captured.");
        }
        verifyNoM16UnsafeCrashHardeningWork(path, data);
    }

    private static void verifyIsolatedLaunchEnvironmentPlan(String path, String expectedStatus, boolean expectedReady) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        verifyM17Envelope(path, envelope, expectedStatus);
        if (!Boolean.valueOf(expectedReady).equals(data.get("isolatedDirectoryPlanned"))) {
            throw new IllegalStateException(path + " isolatedDirectoryPlanned was " + data.get("isolatedDirectoryPlanned") + "; expected " + expectedReady);
        }
        verifyNoBetaEvidenceMutationOrUnsafeWork(path, data);
    }

    private static void verifyMinecraftLaunchPreflight(String path, String expectedStatus, boolean expectedReady) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        verifyM17Envelope(path, envelope, expectedStatus);
        if (!Boolean.valueOf(expectedReady).equals(data.get("launchPreflightComplete"))) {
            throw new IllegalStateException(path + " launchPreflightComplete was " + data.get("launchPreflightComplete") + "; expected " + expectedReady);
        }
        verifyNoBetaEvidenceMutationOrUnsafeWork(path, data);
    }

    private static void verifyLaunchSafetyGate(String path, String expectedStatus, boolean expectedReady) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        verifyM17Envelope(path, envelope, expectedStatus);
        if (!Boolean.valueOf(expectedReady).equals(data.get("safeForIsolatedLaunchAttempt"))) {
            throw new IllegalStateException(path + " safeForIsolatedLaunchAttempt was " + data.get("safeForIsolatedLaunchAttempt") + "; expected " + expectedReady);
        }
        verifyNoBetaEvidenceMutationOrUnsafeWork(path, data);
    }

    private static void verifyControlledLaunchFailureCapturePlan(String path, String expectedStatus, boolean expectedReady) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        verifyM17Envelope(path, envelope, expectedStatus);
        if (!Boolean.valueOf(expectedReady).equals(data.get("planned"))) {
            throw new IllegalStateException(path + " planned was " + data.get("planned") + "; expected " + expectedReady);
        }
        if (!Boolean.TRUE.equals(data.get("supportBundlePlannedOnly"))) {
            throw new IllegalStateException(path + " must keep support bundle output planned-only.");
        }
        verifyNoBetaEvidenceMutationOrUnsafeWork(path, data);
    }

    private static void verifyPhase13M17Readiness(String path, String expectedStatus, boolean expectedReady) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        verifyM17Envelope(path, envelope, expectedStatus);
        if (!Boolean.valueOf(expectedReady).equals(data.get("phase13M17Ready"))) {
            throw new IllegalStateException(path + " phase13M17Ready was " + data.get("phase13M17Ready") + "; expected " + expectedReady);
        }
        verifyNoBetaEvidenceMutationOrUnsafeWork(path, data);
    }

    private static void verifyIsolatedLaunchAttemptPlan(String path, String expectedStatus, boolean expectedControlledFailure) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        verifyM17Envelope(path, envelope, expectedStatus);
        if (!Boolean.valueOf(expectedControlledFailure).equals(data.get("controlledFailure"))) {
            throw new IllegalStateException(path + " controlledFailure was " + data.get("controlledFailure") + "; expected " + expectedControlledFailure);
        }
        boolean expectedLaunchAttempted = !expectedControlledFailure;
        if (!Boolean.valueOf(expectedLaunchAttempted).equals(data.get("launchAttempted"))) {
            throw new IllegalStateException(path + " launchAttempted was " + data.get("launchAttempted") + "; expected " + expectedLaunchAttempted);
        }
        verifyNoBetaEvidenceMutationOrUnsafeWork(path, data);
    }

    private static void verifyLocalRuntimeArtifactCheck(String path, String expectedStatus, boolean expectedReady) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        verifyM17Envelope(path, envelope, expectedStatus);
        if (!Boolean.valueOf(expectedReady).equals(data.get("localArtifactsReady"))) {
            throw new IllegalStateException(path + " localArtifactsReady was " + data.get("localArtifactsReady") + "; expected " + expectedReady);
        }
        if (!Boolean.TRUE.equals(data.get("missingArtifactsBecomeDiagnostics"))) {
            throw new IllegalStateException(path + " must turn missing artifacts into diagnostics.");
        }
        verifyNoM17UnsafeLaunchWork(path, data);
    }

    private static void verifyControlledLaunchAttemptResult(String path, String expectedStatus, boolean expectedControlledFailure) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        verifyM17Envelope(path, envelope, expectedStatus);
        if (!Boolean.valueOf(expectedControlledFailure).equals(data.get("controlledFailure"))) {
            throw new IllegalStateException(path + " controlledFailure was " + data.get("controlledFailure") + "; expected " + expectedControlledFailure);
        }
        if (!Boolean.FALSE.equals(data.get("mainMenuReached"))) {
            throw new IllegalStateException(path + " must not claim the main menu was reached.");
        }
        verifyNoM17UnsafeLaunchWork(path, data);
    }

    private static void verifyLaunchOutputCapture(String path, String expectedStatus) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        verifyM17Envelope(path, envelope, expectedStatus);
        if (!Boolean.TRUE.equals(data.get("secretSafe"))) {
            throw new IllegalStateException(path + " output capture must be secret-safe.");
        }
        verifyNoM17UnsafeLaunchWork(path, data);
    }

    private static void verifyPhase13M17LaunchStatus(String path, String expectedStatus, boolean expectedControlledFailure) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        verifyM17Envelope(path, envelope, expectedStatus);
        if (!Boolean.valueOf(expectedControlledFailure).equals(data.get("controlledFailure"))) {
            throw new IllegalStateException(path + " controlledFailure was " + data.get("controlledFailure") + "; expected " + expectedControlledFailure);
        }
        if (!Boolean.TRUE.equals(data.get("phase13M17AttemptComplete"))) {
            throw new IllegalStateException(path + " must record that the M17 attempt gate completed.");
        }
        verifyNoM17UnsafeLaunchWork(path, data);
    }

    private static void verifyLocalRuntimeArtifactInventory(String path, String expectedStatus, boolean expectedInventoryComplete) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        verifyM17Envelope(path, envelope, expectedStatus);
        if (!Boolean.valueOf(expectedInventoryComplete).equals(data.get("inventoryComplete"))) {
            throw new IllegalStateException(path + " inventoryComplete was " + data.get("inventoryComplete") + "; expected " + expectedInventoryComplete);
        }
        if (!Boolean.TRUE.equals(data.get("repoLocalOnly"))) {
            throw new IllegalStateException(path + " must limit candidates to repo-local approved roots.");
        }
        if (!Boolean.FALSE.equals(data.get("downloadsAllowed"))) {
            throw new IllegalStateException(path + " must not allow artifact downloads.");
        }
        if (!Boolean.FALSE.equals(data.get("filesystemMutated"))) {
            throw new IllegalStateException(path + " must not mutate the filesystem.");
        }
        Object plannedArtifactCount = data.get("plannedArtifactCount");
        if (expectedInventoryComplete && plannedArtifactCount instanceof Number number && number.longValue() <= 0) {
            throw new IllegalStateException(path + " must inventory at least one planned artifact.");
        }
        verifyNoM17UnsafeLaunchWork(path, data);
    }

    private static void verifyLocalRuntimeArtifactMap(String path, String expectedStatus, boolean expectedReady) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        verifyM17Envelope(path, envelope, expectedStatus);
        if (!Boolean.valueOf(expectedReady).equals(data.get("artifactMappingReady"))) {
            throw new IllegalStateException(path + " artifactMappingReady was " + data.get("artifactMappingReady") + "; expected " + expectedReady);
        }
        if (!Boolean.FALSE.equals(data.get("downloadsAllowed"))) {
            throw new IllegalStateException(path + " must not allow artifact downloads.");
        }
        if (!Boolean.FALSE.equals(data.get("extractionAllowed"))) {
            throw new IllegalStateException(path + " must not allow native extraction.");
        }
        verifyNoM17UnsafeLaunchWork(path, data);
    }

    private static void verifyLaunchArtifactResolutionStatus(String path, String expectedStatus, boolean expectedReady) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        verifyM17Envelope(path, envelope, expectedStatus);
        if (!Boolean.valueOf(expectedReady).equals(data.get("artifactsResolved"))) {
            throw new IllegalStateException(path + " artifactsResolved was " + data.get("artifactsResolved") + "; expected " + expectedReady);
        }
        if (!Boolean.TRUE.equals(data.get("missingArtifactsBecomeDiagnostics"))) {
            throw new IllegalStateException(path + " must keep missing artifacts as diagnostics.");
        }
        verifyNoM17UnsafeLaunchWork(path, data);
    }

    private static void verifyIsolatedLaunchExecutionEligibility(String path, String expectedStatus, boolean expectedReady) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        verifyM17Envelope(path, envelope, expectedStatus);
        if (!Boolean.valueOf(expectedReady).equals(data.get("eligibleForLaunchAttempt"))) {
            throw new IllegalStateException(path + " eligibleForLaunchAttempt was " + data.get("eligibleForLaunchAttempt") + "; expected " + expectedReady);
        }
        if (!Boolean.TRUE.equals(data.get("processLaunchStillGated"))) {
            throw new IllegalStateException(path + " must keep process launch gated.");
        }
        verifyNoM17UnsafeLaunchWork(path, data);
    }

    private static void verifyPhase13M17ArtifactReadiness(String path, String expectedStatus, boolean expectedReady) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        verifyM17Envelope(path, envelope, expectedStatus);
        if (!Boolean.valueOf(expectedReady).equals(data.get("phase13M17ArtifactReady"))) {
            throw new IllegalStateException(path + " phase13M17ArtifactReady was " + data.get("phase13M17ArtifactReady") + "; expected " + expectedReady);
        }
        if (!Boolean.valueOf(expectedReady).equals(data.get("safeForIsolatedLaunchAttempt"))) {
            throw new IllegalStateException(path + " safeForIsolatedLaunchAttempt was " + data.get("safeForIsolatedLaunchAttempt") + "; expected " + expectedReady);
        }
        verifyNoM17UnsafeLaunchWork(path, data);
    }

    private static void verifyPhase13M17ArtifactBlockers(String path, String expectedStatus, int expectedBlockerCount, boolean expectedDocumented) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        verifyM17Envelope(path, envelope, expectedStatus);
        if (!Boolean.valueOf(expectedDocumented).equals(data.get("artifactBlockersDocumented"))) {
            throw new IllegalStateException(path + " artifactBlockersDocumented was " + data.get("artifactBlockersDocumented") + "; expected " + expectedDocumented);
        }
        if (!Long.valueOf(expectedBlockerCount).equals(asLong(data.get("artifactBlockerCount")))) {
            throw new IllegalStateException(path + " artifactBlockerCount was " + data.get("artifactBlockerCount") + "; expected " + expectedBlockerCount);
        }
        verifyNoM17UnsafeLaunchWork(path, data);
    }

    private static void verifyPhase13M17BlockerResolutionPlan(String path, String expectedStatus, int expectedActionCount, boolean expectedDocumented) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        verifyM17Envelope(path, envelope, expectedStatus);
        if (!Boolean.valueOf(expectedDocumented).equals(data.get("blockerResolutionDocumented"))) {
            throw new IllegalStateException(path + " blockerResolutionDocumented was " + data.get("blockerResolutionDocumented") + "; expected " + expectedDocumented);
        }
        if (!Long.valueOf(expectedActionCount).equals(asLong(data.get("actionCount")))) {
            throw new IllegalStateException(path + " actionCount was " + data.get("actionCount") + "; expected " + expectedActionCount);
        }
        if (!Boolean.FALSE.equals(data.get("downloadsAllowed"))) {
            throw new IllegalStateException(path + " must not allow downloads.");
        }
        if (!Boolean.FALSE.equals(data.get("extractionAllowed"))) {
            throw new IllegalStateException(path + " must not allow extraction.");
        }
        verifyNoM17UnsafeLaunchWork(path, data);
    }

    private static void verifyPhase13M17ArtifactPackagingAudit(String path, String expectedStatus, int expectedBlockerCount, boolean expectedComplete) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        verifyM17Envelope(path, envelope, expectedStatus);
        if (!Boolean.valueOf(expectedComplete).equals(data.get("auditComplete"))) {
            throw new IllegalStateException(path + " auditComplete was " + data.get("auditComplete") + "; expected " + expectedComplete);
        }
        if (!Long.valueOf(expectedBlockerCount).equals(asLong(data.get("artifactBlockerCount")))) {
            throw new IllegalStateException(path + " artifactBlockerCount was " + data.get("artifactBlockerCount") + "; expected " + expectedBlockerCount);
        }
        if (!Boolean.TRUE.equals(data.get("phase13M17LaunchBlocked"))) {
            throw new IllegalStateException(path + " must keep M17 launch blocked.");
        }
        verifyNoM17UnsafeLaunchWork(path, data);
    }

    private static void verifyPhase13M17ArtifactPackagingResolutionPlan(String path, String expectedStatus, int expectedActionCount, boolean expectedComplete) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        verifyM17Envelope(path, envelope, expectedStatus);
        if (!Boolean.valueOf(expectedComplete).equals(data.get("auditComplete"))) {
            throw new IllegalStateException(path + " auditComplete was " + data.get("auditComplete") + "; expected " + expectedComplete);
        }
        if (!Long.valueOf(expectedActionCount).equals(asLong(data.get("actionCount")))) {
            throw new IllegalStateException(path + " actionCount was " + data.get("actionCount") + "; expected " + expectedActionCount);
        }
        if (!Boolean.FALSE.equals(data.get("downloadsAllowed"))) {
            throw new IllegalStateException(path + " must not allow downloads.");
        }
        if (!Boolean.FALSE.equals(data.get("extractionAllowed"))) {
            throw new IllegalStateException(path + " must not allow extraction.");
        }
        verifyNoM17UnsafeLaunchWork(path, data);
    }

    private static void verifyPhase13M18Readiness(String path, String expectedStatus, boolean expectedReady, int expectedMissingCount) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        verifyM17Envelope(path, envelope, expectedStatus);
        if (!Boolean.valueOf(expectedReady).equals(data.get("phase13M18Ready"))) {
            throw new IllegalStateException(path + " phase13M18Ready was " + data.get("phase13M18Ready") + "; expected " + expectedReady);
        }
        if (!Long.valueOf(expectedMissingCount).equals(asLong(data.get("missingArtifactCount")))) {
            throw new IllegalStateException(path + " missingArtifactCount was " + data.get("missingArtifactCount") + "; expected " + expectedMissingCount);
        }
        if (!Boolean.FALSE.equals(data.get("playtestCandidateReady"))) {
            throw new IllegalStateException(path + " must not mark playtestCandidateReady during M17 blocker documentation.");
        }
        verifyNoM17UnsafeLaunchWork(path, data);
    }

    private static void verifyRuntimeFixturePresence(String path, String expectedStatus, boolean expectedReady, int expectedCheckCount, int expectedMissingCount) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        verifyM17Envelope(path, envelope, expectedStatus);
        if (!Boolean.valueOf(expectedReady).equals(data.get("runtimeFixturesPresent"))) {
            throw new IllegalStateException(path + " runtimeFixturesPresent was " + data.get("runtimeFixturesPresent") + "; expected " + expectedReady);
        }
        if (!Long.valueOf(expectedCheckCount).equals(asLong(data.get("fixtureCheckCount")))) {
            throw new IllegalStateException(path + " fixtureCheckCount was " + data.get("fixtureCheckCount") + "; expected " + expectedCheckCount);
        }
        if (!Long.valueOf(expectedMissingCount).equals(asLong(data.get("missingFixtureCount")))) {
            throw new IllegalStateException(path + " missingFixtureCount was " + data.get("missingFixtureCount") + "; expected " + expectedMissingCount);
        }
        if (!Boolean.FALSE.equals(data.get("safeToAutoPopulate"))) {
            throw new IllegalStateException(path + " must not auto-populate runtime fixtures.");
        }
        verifyNoM17UnsafeLaunchWork(path, data);
    }

    private static void verifyRuntimeFixtureMappingReadiness(String path, String expectedStatus, boolean expectedReady, int expectedCheckCount, int expectedReadyCount) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        verifyM17Envelope(path, envelope, expectedStatus);
        if (!Boolean.valueOf(expectedReady).equals(data.get("runtimeFixtureMappingsReady"))) {
            throw new IllegalStateException(path + " runtimeFixtureMappingsReady was " + data.get("runtimeFixtureMappingsReady") + "; expected " + expectedReady);
        }
        if (!Long.valueOf(expectedCheckCount).equals(asLong(data.get("mappingCheckCount")))) {
            throw new IllegalStateException(path + " mappingCheckCount was " + data.get("mappingCheckCount") + "; expected " + expectedCheckCount);
        }
        if (!Long.valueOf(expectedReadyCount).equals(asLong(data.get("readyMappingCount")))) {
            throw new IllegalStateException(path + " readyMappingCount was " + data.get("readyMappingCount") + "; expected " + expectedReadyCount);
        }
        if (!Boolean.FALSE.equals(data.get("safeToAutoPopulate"))) {
            throw new IllegalStateException(path + " must not auto-populate runtime fixture mappings.");
        }
        verifyNoM17UnsafeLaunchWork(path, data);
    }

    private static void verifyRuntimeFixtureIntakePlan(String path, String expectedStatus, boolean expectedReady, int expectedActionCount, int expectedCompleteCount) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        verifyM17Envelope(path, envelope, expectedStatus);
        if (!Boolean.valueOf(expectedReady).equals(data.get("intakeReady"))) {
            throw new IllegalStateException(path + " intakeReady was " + data.get("intakeReady") + "; expected " + expectedReady);
        }
        if (!Long.valueOf(expectedActionCount).equals(asLong(data.get("actionCount")))) {
            throw new IllegalStateException(path + " actionCount was " + data.get("actionCount") + "; expected " + expectedActionCount);
        }
        if (!Long.valueOf(expectedCompleteCount).equals(asLong(data.get("completeActionCount")))) {
            throw new IllegalStateException(path + " completeActionCount was " + data.get("completeActionCount") + "; expected " + expectedCompleteCount);
        }
        if (!Boolean.FALSE.equals(data.get("safeToAutoPopulate"))) {
            throw new IllegalStateException(path + " must not auto-populate runtime fixture intake.");
        }
        verifyNoM17UnsafeLaunchWork(path, data);
    }

    private static void verifyRuntimeFixtureIntakeChecklist(String path, String expectedStatus, boolean expectedReady, int expectedChecklistCount, int expectedCompleteCount) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        verifyM17Envelope(path, envelope, expectedStatus);
        if (!Boolean.valueOf(expectedReady).equals(data.get("intakeReady"))) {
            throw new IllegalStateException(path + " intakeReady was " + data.get("intakeReady") + "; expected " + expectedReady);
        }
        if (!Long.valueOf(expectedChecklistCount).equals(asLong(data.get("checklistCount")))) {
            throw new IllegalStateException(path + " checklistCount was " + data.get("checklistCount") + "; expected " + expectedChecklistCount);
        }
        if (!Long.valueOf(expectedCompleteCount).equals(asLong(data.get("completeChecklistCount")))) {
            throw new IllegalStateException(path + " completeChecklistCount was " + data.get("completeChecklistCount") + "; expected " + expectedCompleteCount);
        }
        verifyNoM17UnsafeLaunchWork(path, data);
    }

    private static void verifyRuntimeFixtureApprovalAudit(String path, String expectedStatus, boolean expectedReady, int expectedApprovalCount, int expectedApprovedCount) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        verifyM17Envelope(path, envelope, expectedStatus);
        if (!Boolean.valueOf(expectedReady).equals(data.get("approvalsReady"))) {
            throw new IllegalStateException(path + " approvalsReady was " + data.get("approvalsReady") + "; expected " + expectedReady);
        }
        if (!Long.valueOf(expectedApprovalCount).equals(asLong(data.get("approvalAuditCount")))) {
            throw new IllegalStateException(path + " approvalAuditCount was " + data.get("approvalAuditCount") + "; expected " + expectedApprovalCount);
        }
        if (!Long.valueOf(expectedApprovedCount).equals(asLong(data.get("approvedCount")))) {
            throw new IllegalStateException(path + " approvedCount was " + data.get("approvedCount") + "; expected " + expectedApprovedCount);
        }
        verifyNoM17UnsafeLaunchWork(path, data);
    }

    private static void verifyRuntimeFixtureApprovalTemplate(String path, String expectedStatus, boolean expectedReady, int expectedTemplateCount) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        verifyM17Envelope(path, envelope, expectedStatus);
        if (!Boolean.valueOf(expectedReady).equals(data.get("approvalsReady"))) {
            throw new IllegalStateException(path + " approvalsReady was " + data.get("approvalsReady") + "; expected " + expectedReady);
        }
        if (!Long.valueOf(expectedTemplateCount).equals(asLong(data.get("approvalTemplateCount")))) {
            throw new IllegalStateException(path + " approvalTemplateCount was " + data.get("approvalTemplateCount") + "; expected " + expectedTemplateCount);
        }
        verifyNoM17UnsafeLaunchWork(path, data);
    }

    private static void verifyRuntimeFixtureHandoff(String path, String expectedStatus, boolean expectedReady, int expectedItemCount) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        verifyM17Envelope(path, envelope, expectedStatus);
        if (!Boolean.valueOf(expectedReady).equals(data.get("handoffReady"))) {
            throw new IllegalStateException(path + " handoffReady was " + data.get("handoffReady") + "; expected " + expectedReady);
        }
        if (!Long.valueOf(expectedItemCount).equals(asLong(data.get("handoffItemCount")))) {
            throw new IllegalStateException(path + " handoffItemCount was " + data.get("handoffItemCount") + "; expected " + expectedItemCount);
        }
        if (!Boolean.FALSE.equals(data.get("safeToAutoPopulate"))) {
            throw new IllegalStateException(path + " must not auto-populate runtime fixtures.");
        }
        verifyNoM17UnsafeLaunchWork(path, data);
    }

    private static void verifyRuntimeFixtureValidationRunbook(String path, String expectedStatus, boolean expectedReady, int expectedRequiredFileCount) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        verifyM17Envelope(path, envelope, expectedStatus);
        if (!Boolean.valueOf(expectedReady).equals(data.get("handoffReady"))) {
            throw new IllegalStateException(path + " handoffReady was " + data.get("handoffReady") + "; expected " + expectedReady);
        }
        Object rawFiles = data.get("requiredFiles");
        if (!(rawFiles instanceof List<?> files) || files.size() != expectedRequiredFileCount) {
            throw new IllegalStateException(path + " requiredFiles count was " + (rawFiles instanceof List<?> files ? files.size() : "not_a_list") + "; expected " + expectedRequiredFileCount);
        }
        Long runtimeArtifactEntryCount = asLong(data.get("requiredRuntimeArtifactsJsonEntryCount"));
        if (runtimeArtifactEntryCount != null && !Long.valueOf(expectedRequiredFileCount).equals(runtimeArtifactEntryCount)) {
            throw new IllegalStateException(path + " requiredRuntimeArtifactsJsonEntryCount was " + runtimeArtifactEntryCount + "; expected " + expectedRequiredFileCount);
        }
        Long approvalEntryCount = asLong(data.get("requiredRuntimeFixtureApprovalEntryCount"));
        if (approvalEntryCount != null && !Long.valueOf(expectedRequiredFileCount).equals(approvalEntryCount)) {
            throw new IllegalStateException(path + " requiredRuntimeFixtureApprovalEntryCount was " + approvalEntryCount + "; expected " + expectedRequiredFileCount);
        }
        Object rawWorkflow = data.get("approvalEvidenceWorkflow");
        if (!(rawWorkflow instanceof List<?> workflow) || workflow.stream().noneMatch(step -> String.valueOf(step).contains("runtime-fixture-approvals.json"))) {
            throw new IllegalStateException(path + " must include a manual approval evidence workflow.");
        }
        Object rawCommands = data.get("requiredCommands");
        if (!(rawCommands instanceof List<?> commands) || commands.stream().noneMatch(command -> String.valueOf(command).contains("phase13 draft runtime-fixture-approval"))) {
            throw new IllegalStateException(path + " must include the runtime fixture approval draft/hash-review command.");
        }
        Object rawChecklist = data.get("reviewChecklist");
        if (!(rawChecklist instanceof List<?> checklist) || checklist.stream().noneMatch(item -> String.valueOf(item).contains("byteSize and SHA-256"))) {
            throw new IllegalStateException(path + " must tell operators to compute byteSize and SHA-256 before approvals.");
        }
        if (!Boolean.TRUE.equals(data.get("manualIntakeOnly"))) {
            throw new IllegalStateException(path + " must remain manualIntakeOnly.");
        }
        verifyNoM17UnsafeLaunchWork(path, data);
    }

    private static void verifyRuntimeFixtureIntegrityAudit(String path, String expectedStatus, boolean expectedReady, int expectedCheckCount, int expectedHashVerifiedCount) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        verifyM17Envelope(path, envelope, expectedStatus);
        if (!Boolean.valueOf(expectedReady).equals(data.get("integrityReady"))) {
            throw new IllegalStateException(path + " integrityReady was " + data.get("integrityReady") + "; expected " + expectedReady);
        }
        if (!Long.valueOf(expectedCheckCount).equals(asLong(data.get("integrityCheckCount")))) {
            throw new IllegalStateException(path + " integrityCheckCount was " + data.get("integrityCheckCount") + "; expected " + expectedCheckCount);
        }
        if (!Long.valueOf(expectedHashVerifiedCount).equals(asLong(data.get("hashVerifiedCount")))) {
            throw new IllegalStateException(path + " hashVerifiedCount was " + data.get("hashVerifiedCount") + "; expected " + expectedHashVerifiedCount);
        }
        if (!"SHA-256".equals(data.get("hashAlgorithm"))) {
            throw new IllegalStateException(path + " must use SHA-256.");
        }
        verifyNoM17UnsafeLaunchWork(path, data);
    }

    private static void verifyRuntimeFixtureIntegrityManifest(String path, String expectedStatus, boolean expectedReady, int expectedItemCount) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        verifyM17Envelope(path, envelope, expectedStatus);
        if (!Boolean.valueOf(expectedReady).equals(data.get("integrityReady"))) {
            throw new IllegalStateException(path + " integrityReady was " + data.get("integrityReady") + "; expected " + expectedReady);
        }
        if (!Long.valueOf(expectedItemCount).equals(asLong(data.get("integrityManifestItemCount")))) {
            throw new IllegalStateException(path + " integrityManifestItemCount was " + data.get("integrityManifestItemCount") + "; expected " + expectedItemCount);
        }
        if (!Boolean.TRUE.equals(data.get("manualIntakeOnly"))) {
            throw new IllegalStateException(path + " must remain manualIntakeOnly.");
        }
        verifyNoM17UnsafeLaunchWork(path, data);
    }

    private static void verifyRuntimeFixtureApprovalDraft(String path, String expectedStatus, boolean expectedReady, int expectedEntryCount, int expectedHashCount) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        verifyM17Envelope(path, envelope, expectedStatus);
        if (!Boolean.valueOf(expectedReady).equals(data.get("draftReadyForHumanReview"))) {
            throw new IllegalStateException(path + " draftReadyForHumanReview was " + data.get("draftReadyForHumanReview") + "; expected " + expectedReady);
        }
        if (!Long.valueOf(expectedEntryCount).equals(asLong(data.get("draftEntryCount")))) {
            throw new IllegalStateException(path + " draftEntryCount was " + data.get("draftEntryCount") + "; expected " + expectedEntryCount);
        }
        if (!Long.valueOf(expectedHashCount).equals(asLong(data.get("hashComputedCount")))) {
            throw new IllegalStateException(path + " hashComputedCount was " + data.get("hashComputedCount") + "; expected " + expectedHashCount);
        }
        if (!Boolean.FALSE.equals(data.get("approvalFileCreated")) || !Boolean.FALSE.equals(data.get("approvalFileMutated"))) {
            throw new IllegalStateException(path + " must not create or mutate runtime-fixture-approvals.json.");
        }
        verifyNoM17UnsafeLaunchWork(path, data);
    }

    private static void verifyRuntimeFixtureHashReview(String path, String expectedStatus, boolean expectedReady, int expectedItemCount, int expectedHashCount) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        verifyM17Envelope(path, envelope, expectedStatus);
        if (!Boolean.valueOf(expectedReady).equals(data.get("reviewReady"))) {
            throw new IllegalStateException(path + " reviewReady was " + data.get("reviewReady") + "; expected " + expectedReady);
        }
        if (!Long.valueOf(expectedItemCount).equals(asLong(data.get("hashReviewItemCount")))) {
            throw new IllegalStateException(path + " hashReviewItemCount was " + data.get("hashReviewItemCount") + "; expected " + expectedItemCount);
        }
        if (!Long.valueOf(expectedHashCount).equals(asLong(data.get("hashComputedCount")))) {
            throw new IllegalStateException(path + " hashComputedCount was " + data.get("hashComputedCount") + "; expected " + expectedHashCount);
        }
        if (!Boolean.FALSE.equals(data.get("approvalFileCreated")) || !Boolean.FALSE.equals(data.get("approvalFileMutated"))) {
            throw new IllegalStateException(path + " must not create or mutate runtime-fixture-approvals.json.");
        }
        verifyNoM17UnsafeLaunchWork(path, data);
    }

    private static void verifyRuntimeFixtureOperatorPacket(String path, String expectedStatus, boolean expectedPacketReady, boolean expectedRuntimeFilesReady, int expectedFileCount, int expectedArtifactEntryCount) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        verifyM17Envelope(path, envelope, expectedStatus);
        if (!Boolean.valueOf(expectedPacketReady).equals(data.get("operatorPacketReady"))) {
            throw new IllegalStateException(path + " operatorPacketReady was " + data.get("operatorPacketReady") + "; expected " + expectedPacketReady);
        }
        if (!Boolean.valueOf(expectedRuntimeFilesReady).equals(data.get("runtimeFilesReady"))) {
            throw new IllegalStateException(path + " runtimeFilesReady was " + data.get("runtimeFilesReady") + "; expected " + expectedRuntimeFilesReady);
        }
        if (!Long.valueOf(expectedFileCount).equals(asLong(data.get("requiredFileCount")))) {
            throw new IllegalStateException(path + " requiredFileCount was " + data.get("requiredFileCount") + "; expected " + expectedFileCount);
        }
        if (!Long.valueOf(expectedArtifactEntryCount).equals(asLong(data.get("requiredRuntimeArtifactsJsonEntryCount")))) {
            throw new IllegalStateException(path + " requiredRuntimeArtifactsJsonEntryCount was " + data.get("requiredRuntimeArtifactsJsonEntryCount") + "; expected " + expectedArtifactEntryCount);
        }
        if (!Boolean.FALSE.equals(data.get("firstPlaytestOpen"))) {
            throw new IllegalStateException(path + " must keep firstPlaytestOpen=false.");
        }
        if (!Boolean.TRUE.equals(data.get("phase13M17LaunchBlocked"))) {
            throw new IllegalStateException(path + " must keep phase13M17LaunchBlocked=true until fixture evidence is present.");
        }
        verifyNoM17UnsafeLaunchWork(path, data);
    }

    private static void verifyNativeLoaderBetaFeedbackInventory(String path, String expectedStatus, int expectedFeedbackArtifactCount, int expectedScreenshotCount, boolean expectedStructuredFeedback, boolean expectedPlayableEvidence) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        verifyM17Envelope(path, envelope, expectedStatus);
        if (!Long.valueOf(expectedFeedbackArtifactCount).equals(asLong(data.get("feedbackArtifactCount")))) {
            throw new IllegalStateException(path + " feedbackArtifactCount was " + data.get("feedbackArtifactCount") + "; expected " + expectedFeedbackArtifactCount);
        }
        if (!Long.valueOf(expectedScreenshotCount).equals(asLong(data.get("screenshotCount")))) {
            throw new IllegalStateException(path + " screenshotCount was " + data.get("screenshotCount") + "; expected " + expectedScreenshotCount);
        }
        if (!Boolean.valueOf(expectedStructuredFeedback).equals(data.get("structuredFeedbackPresent"))) {
            throw new IllegalStateException(path + " structuredFeedbackPresent was " + data.get("structuredFeedbackPresent") + "; expected " + expectedStructuredFeedback);
        }
        if (!Boolean.valueOf(expectedPlayableEvidence).equals(data.get("playableBaselineEvidence"))) {
            throw new IllegalStateException(path + " playableBaselineEvidence was " + data.get("playableBaselineEvidence") + "; expected " + expectedPlayableEvidence);
        }
        verifyNoBetaEvidenceMutationOrUnsafeWork(path, data);
    }

    private static void verifyNativeLoaderBetaEvidenceQuality(String path, String expectedStatus, int expectedQualifiedSessionCount, int expectedTargetSessionCount, int expectedRemainingSessionCount, boolean expectedLatestLogPresent, boolean expectedPlayableEvidence) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        verifyM17Envelope(path, envelope, expectedStatus);
        if (!Long.valueOf(expectedQualifiedSessionCount).equals(asLong(data.get("qualifiedSessionCount")))) {
            throw new IllegalStateException(path + " qualifiedSessionCount was " + data.get("qualifiedSessionCount") + "; expected " + expectedQualifiedSessionCount);
        }
        if (!Long.valueOf(expectedTargetSessionCount).equals(asLong(data.get("targetInternalSessionCount")))) {
            throw new IllegalStateException(path + " targetInternalSessionCount was " + data.get("targetInternalSessionCount") + "; expected " + expectedTargetSessionCount);
        }
        if (!Long.valueOf(expectedRemainingSessionCount).equals(asLong(data.get("remainingSessionCount")))) {
            throw new IllegalStateException(path + " remainingSessionCount was " + data.get("remainingSessionCount") + "; expected " + expectedRemainingSessionCount);
        }
        if (!Boolean.valueOf(expectedLatestLogPresent).equals(data.get("latestLogPresent"))) {
            throw new IllegalStateException(path + " latestLogPresent was " + data.get("latestLogPresent") + "; expected " + expectedLatestLogPresent);
        }
        if (!Boolean.valueOf(expectedPlayableEvidence).equals(data.get("playableBaselineEvidence"))) {
            throw new IllegalStateException(path + " playableBaselineEvidence was " + data.get("playableBaselineEvidence") + "; expected " + expectedPlayableEvidence);
        }
        if (!Boolean.TRUE.equals(data.get("noCrashEvidence"))) {
            throw new IllegalStateException(path + " must retain noCrashEvidence=true.");
        }
        verifyNoBetaEvidenceMutationOrUnsafeWork(path, data);
    }

    private static void verifyNativeLoaderBetaSoakOperatorPacket(String path, String expectedStatus, boolean expectedPacketReady, int expectedSessionCount, int expectedRemainingSessionCount) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        verifyM17Envelope(path, envelope, expectedStatus);
        if (!Boolean.valueOf(expectedPacketReady).equals(data.get("operatorPacketReady"))) {
            throw new IllegalStateException(path + " operatorPacketReady was " + data.get("operatorPacketReady") + "; expected " + expectedPacketReady);
        }
        if (!Long.valueOf(expectedSessionCount).equals(asLong(data.get("sessionCount")))) {
            throw new IllegalStateException(path + " sessionCount was " + data.get("sessionCount") + "; expected " + expectedSessionCount);
        }
        if (!Long.valueOf(expectedRemainingSessionCount).equals(asLong(data.get("remainingSessionCount")))) {
            throw new IllegalStateException(path + " remainingSessionCount was " + data.get("remainingSessionCount") + "; expected " + expectedRemainingSessionCount);
        }
        if (!Boolean.FALSE.equals(data.get("publicBetaOpen"))) {
            throw new IllegalStateException(path + " must keep publicBetaOpen=false.");
        }
        verifyNoBetaEvidenceMutationOrUnsafeWork(path, data);
    }

    private static void verifyNativeLoaderBetaRemainingSessionPlan(String path, String expectedStatus, int expectedSessionCount, int expectedTargetSessionCount, int expectedRemainingSessionCount) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        verifyM17Envelope(path, envelope, expectedStatus);
        if (!Long.valueOf(expectedSessionCount).equals(asLong(data.get("sessionCount")))) {
            throw new IllegalStateException(path + " sessionCount was " + data.get("sessionCount") + "; expected " + expectedSessionCount);
        }
        if (!Long.valueOf(expectedTargetSessionCount).equals(asLong(data.get("targetInternalSessionCount")))) {
            throw new IllegalStateException(path + " targetInternalSessionCount was " + data.get("targetInternalSessionCount") + "; expected " + expectedTargetSessionCount);
        }
        if (!Long.valueOf(expectedRemainingSessionCount).equals(asLong(data.get("remainingSessionCount")))) {
            throw new IllegalStateException(path + " remainingSessionCount was " + data.get("remainingSessionCount") + "; expected " + expectedRemainingSessionCount);
        }
        Object rawSessions = data.get("remainingSessions");
        int actualSessionCount = rawSessions instanceof List<?> sessions ? sessions.size() : -1;
        int expectedPlanCount = Math.max(expectedRemainingSessionCount, asInt(data.get("remainingM30EvidenceCount")));
        if (actualSessionCount != expectedPlanCount) {
            throw new IllegalStateException(path + " remainingSessions size was " + (actualSessionCount < 0 ? "not a list" : actualSessionCount) + "; expected " + expectedPlanCount);
        }
        if (!Boolean.FALSE.equals(data.get("publicBetaOpen"))) {
            throw new IllegalStateException(path + " must keep publicBetaOpen=false.");
        }
        verifyNoBetaEvidenceMutationOrUnsafeWork(path, data);
    }

    private static void verifyNativeLoaderBetaSessionNoteDrafts(String path, String expectedStatus, int expectedSessionCount, int expectedTargetSessionCount, int expectedRemainingSessionCount) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        verifyM17Envelope(path, envelope, expectedStatus);
        if (!Long.valueOf(expectedSessionCount).equals(asLong(data.get("sessionCount")))) {
            throw new IllegalStateException(path + " sessionCount was " + data.get("sessionCount") + "; expected " + expectedSessionCount);
        }
        if (!Long.valueOf(expectedTargetSessionCount).equals(asLong(data.get("targetInternalSessionCount")))) {
            throw new IllegalStateException(path + " targetInternalSessionCount was " + data.get("targetInternalSessionCount") + "; expected " + expectedTargetSessionCount);
        }
        if (!Long.valueOf(expectedRemainingSessionCount).equals(asLong(data.get("remainingSessionCount")))) {
            throw new IllegalStateException(path + " remainingSessionCount was " + data.get("remainingSessionCount") + "; expected " + expectedRemainingSessionCount);
        }
        Object rawDrafts = data.get("drafts");
        int actualDraftCount = rawDrafts instanceof List<?> drafts ? drafts.size() : -1;
        int expectedDraftCount = Math.max(expectedRemainingSessionCount, asInt(data.get("remainingM30EvidenceCount")));
        if (actualDraftCount != expectedDraftCount) {
            throw new IllegalStateException(path + " drafts size was " + (actualDraftCount < 0 ? "not a list" : actualDraftCount) + "; expected " + expectedDraftCount);
        }
        if (!Boolean.FALSE.equals(data.get("publicBetaOpen"))) {
            throw new IllegalStateException(path + " must keep publicBetaOpen=false.");
        }
        verifyNoBetaEvidenceMutationOrUnsafeWork(path, data);
    }

    private static void verifyNativeLoaderBetaSessionDraftFiles(String path, String expectedStatus, int expectedDraftFileCount, boolean expectedDraftFilesPrepared) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        verifyM17Envelope(path, envelope, expectedStatus);
        if (!Long.valueOf(expectedDraftFileCount).equals(asLong(data.get("draftFileCount")))) {
            throw new IllegalStateException(path + " draftFileCount was " + data.get("draftFileCount") + "; expected " + expectedDraftFileCount);
        }
        Object rawDraftFiles = data.get("draftFiles");
        if (rawDraftFiles instanceof List<?> draftFiles && draftFiles.size() != expectedDraftFileCount) {
            throw new IllegalStateException(path + " draftFiles size was " + draftFiles.size() + "; expected " + expectedDraftFileCount);
        }
        Object prepared = data.get("fixtureDraftFilesPrepared");
        if (prepared != null && !Boolean.valueOf(expectedDraftFilesPrepared).equals(prepared)) {
            throw new IllegalStateException(path + " fixtureDraftFilesPrepared was " + prepared + "; expected " + expectedDraftFilesPrepared);
        }
        Object evidenceCreated = data.get("evidenceCreated");
        if (evidenceCreated != null && !Boolean.FALSE.equals(evidenceCreated)) {
            throw new IllegalStateException(path + " must not create qualifying tester evidence.");
        }
        Object qualifiesAsEvidence = data.get("qualifiesAsEvidence");
        if (qualifiesAsEvidence != null && !Boolean.FALSE.equals(qualifiesAsEvidence)) {
            throw new IllegalStateException(path + " must keep qualifiesAsEvidence=false.");
        }
        verifyNoBetaDraftUnsafeWork(path, data);
    }

    private static void verifyNativeLoaderBetaSessionNoteValidation(
            String path,
            String expectedStatus,
            int expectedNoteCount,
            int expectedIgnoredDraftCount,
            int expectedCurrentM29QualifiedCount,
            int expectedCompleteForM30SoakStandardCount
    ) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        verifyM17Envelope(path, envelope, expectedStatus);
        if (!Long.valueOf(expectedNoteCount).equals(asLong(data.get("noteCount")))) {
            throw new IllegalStateException(path + " noteCount was " + data.get("noteCount") + "; expected " + expectedNoteCount);
        }
        if (!Long.valueOf(expectedIgnoredDraftCount).equals(asLong(data.get("ignoredDraftCount")))) {
            throw new IllegalStateException(path + " ignoredDraftCount was " + data.get("ignoredDraftCount") + "; expected " + expectedIgnoredDraftCount);
        }
        if (!Long.valueOf(expectedCurrentM29QualifiedCount).equals(asLong(data.get("currentM29QualifiedSessionCount")))) {
            throw new IllegalStateException(path + " currentM29QualifiedSessionCount was " + data.get("currentM29QualifiedSessionCount") + "; expected " + expectedCurrentM29QualifiedCount);
        }
        if (!Long.valueOf(expectedCompleteForM30SoakStandardCount).equals(asLong(data.get("completeForM30SoakStandardCount")))) {
            throw new IllegalStateException(path + " completeForM30SoakStandardCount was " + data.get("completeForM30SoakStandardCount") + "; expected " + expectedCompleteForM30SoakStandardCount);
        }
        Object rawNotes = data.get("notes");
        if (rawNotes instanceof List<?> notes && notes.size() != expectedNoteCount) {
            throw new IllegalStateException(path + " notes size was " + notes.size() + "; expected " + expectedNoteCount);
        }
        Object gateChanged = data.get("m30EvidenceGateChanged");
        if (gateChanged != null && !Boolean.TRUE.equals(gateChanged)) {
            throw new IllegalStateException(path + " must declare the M30 evidence gate integration.");
        }
        Object validationOnly = data.get("validationOnly");
        if (validationOnly != null && !Boolean.TRUE.equals(validationOnly)) {
            throw new IllegalStateException(path + " must be validation-only.");
        }
        verifyNoBetaEvidenceMutationOrUnsafeWork(path, data);
    }

    private static void verifyNativeLoaderBetaSoakStatus(
            String path,
            String expectedStatus,
            Boolean expectedM29Active,
            boolean expectedM30Ready,
            int expectedCurrentQualifiedCount,
            int expectedRemainingCount,
            int expectedNextRequiredSessionCount
    ) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        verifyM17Envelope(path, envelope, expectedStatus);
        Object m29Active = data.get("m29Active");
        if (expectedM29Active != null && !expectedM29Active.equals(m29Active)) {
            throw new IllegalStateException(path + " m29Active was " + m29Active + "; expected " + expectedM29Active);
        }
        Object m30Ready = data.get("m30Ready");
        if (m30Ready != null && !Boolean.valueOf(expectedM30Ready).equals(m30Ready)) {
            throw new IllegalStateException(path + " m30Ready was " + m30Ready + "; expected " + expectedM30Ready);
        }
        if (!Long.valueOf(expectedCurrentQualifiedCount).equals(asLong(data.get("currentM29QualifiedSessionCount")))) {
            throw new IllegalStateException(path + " currentM29QualifiedSessionCount was " + data.get("currentM29QualifiedSessionCount") + "; expected " + expectedCurrentQualifiedCount);
        }
        if (!Long.valueOf(expectedRemainingCount).equals(asLong(data.get("remainingQualifiedSessionCount")))) {
            throw new IllegalStateException(path + " remainingQualifiedSessionCount was " + data.get("remainingQualifiedSessionCount") + "; expected " + expectedRemainingCount);
        }
        Object rawIds = data.get("nextRequiredSessionIds");
        int actualIdCount = rawIds instanceof List<?> ids ? ids.size() : -1;
        if (actualIdCount != expectedNextRequiredSessionCount) {
            throw new IllegalStateException(path + " nextRequiredSessionIds size was " + (actualIdCount < 0 ? "not a list" : actualIdCount) + "; expected " + expectedNextRequiredSessionCount);
        }
        if (!Boolean.FALSE.equals(data.get("publicBetaOpen"))) {
            throw new IllegalStateException(path + " must keep publicBetaOpen=false.");
        }
        verifyNoBetaEvidenceMutationOrUnsafeWork(path, data);
    }

    private static void verifyNativeLoaderBetaSessionProofMatrix(String path, String expectedStatus, int expectedQualifiedSessionCount, int expectedTargetSessionCount) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        verifyM17Envelope(path, envelope, expectedStatus);
        if (!Long.valueOf(expectedQualifiedSessionCount).equals(asLong(data.get("qualifiedSessionCount")))) {
            throw new IllegalStateException(path + " qualifiedSessionCount was " + data.get("qualifiedSessionCount") + "; expected " + expectedQualifiedSessionCount);
        }
        if (!Long.valueOf(expectedTargetSessionCount).equals(asLong(data.get("targetInternalSessionCount")))) {
            throw new IllegalStateException(path + " targetInternalSessionCount was " + data.get("targetInternalSessionCount") + "; expected " + expectedTargetSessionCount);
        }
        Object rawProofs = data.get("sessionProofs");
        int actualProofCount = rawProofs instanceof List<?> proofs ? proofs.size() : -1;
        if (actualProofCount != expectedQualifiedSessionCount) {
            throw new IllegalStateException(path + " sessionProofs size was " + (actualProofCount < 0 ? "not a list" : actualProofCount) + "; expected " + expectedQualifiedSessionCount);
        }
        verifyNoBetaEvidenceMutationOrUnsafeWork(path, data);
    }

    private static void verifyPhase13M29Completion(String path, String expectedStatus, boolean expectedComplete, boolean expectedM30Ready, int expectedSessionCount, int expectedTargetSessionCount) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        verifyM17Envelope(path, envelope, expectedStatus);
        if (!Boolean.valueOf(expectedComplete).equals(data.get("phase13M29Complete"))) {
            throw new IllegalStateException(path + " phase13M29Complete was " + data.get("phase13M29Complete") + "; expected " + expectedComplete);
        }
        if (!Boolean.valueOf(expectedM30Ready).equals(data.get("phase13M30Ready"))) {
            throw new IllegalStateException(path + " phase13M30Ready was " + data.get("phase13M30Ready") + "; expected " + expectedM30Ready);
        }
        if (!Long.valueOf(expectedSessionCount).equals(asLong(data.get("sessionCount")))) {
            throw new IllegalStateException(path + " sessionCount was " + data.get("sessionCount") + "; expected " + expectedSessionCount);
        }
        if (!Long.valueOf(expectedTargetSessionCount).equals(asLong(data.get("targetInternalSessionCount")))) {
            throw new IllegalStateException(path + " targetInternalSessionCount was " + data.get("targetInternalSessionCount") + "; expected " + expectedTargetSessionCount);
        }
        verifyNoBetaEvidenceMutationOrUnsafeWork(path, data);
    }

    private static void verifyPhase13M30Completion(String path, String expectedStatus, boolean expectedComplete, boolean expectedM31Ready, int expectedSessionCount, int expectedTargetSessionCount) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        verifyM17Envelope(path, envelope, expectedStatus);
        if (!Boolean.valueOf(expectedComplete).equals(data.get("phase13M30Complete"))) {
            throw new IllegalStateException(path + " phase13M30Complete was " + data.get("phase13M30Complete") + "; expected " + expectedComplete);
        }
        if (!Boolean.valueOf(expectedM31Ready).equals(data.get("phase13M31Ready"))) {
            throw new IllegalStateException(path + " phase13M31Ready was " + data.get("phase13M31Ready") + "; expected " + expectedM31Ready);
        }
        if (!Long.valueOf(expectedSessionCount).equals(asLong(data.get("sessionCount")))) {
            throw new IllegalStateException(path + " sessionCount was " + data.get("sessionCount") + "; expected " + expectedSessionCount);
        }
        if (!Long.valueOf(expectedTargetSessionCount).equals(asLong(data.get("targetInternalSessionCount")))) {
            throw new IllegalStateException(path + " targetInternalSessionCount was " + data.get("targetInternalSessionCount") + "; expected " + expectedTargetSessionCount);
        }
        if (!Boolean.FALSE.equals(data.get("publicReleaseReady"))) {
            throw new IllegalStateException(path + " must keep publicReleaseReady=false.");
        }
        verifyNoBetaEvidenceMutationOrUnsafeWork(path, data);
    }

    private static void verifyPublicBetaOpening(String path, String expectedStatus, boolean expectedReady, int expectedIssueCount, int expectedBlockingKnownIssueCount) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        verifyM17Envelope(path, envelope, expectedStatus);
        if (!Boolean.valueOf(expectedReady).equals(data.get("publicBetaReady"))) {
            throw new IllegalStateException(path + " publicBetaReady was " + data.get("publicBetaReady") + "; expected " + expectedReady);
        }
        if (!Boolean.valueOf(expectedReady).equals(data.get("publicBetaOpen"))) {
            throw new IllegalStateException(path + " publicBetaOpen was " + data.get("publicBetaOpen") + "; expected " + expectedReady);
        }
        if (!Boolean.FALSE.equals(data.get("publicReleaseReady"))) {
            throw new IllegalStateException(path + " must keep publicReleaseReady=false.");
        }
        Object issueCount = data.get("issueCount");
        if (issueCount != null && !Long.valueOf(expectedIssueCount).equals(asLong(issueCount))) {
            throw new IllegalStateException(path + " issueCount was " + issueCount + "; expected " + expectedIssueCount);
        }
        Object blockingKnownIssueCount = data.get("blockingKnownIssueCount");
        if (blockingKnownIssueCount != null && !Long.valueOf(expectedBlockingKnownIssueCount).equals(asLong(blockingKnownIssueCount))) {
            throw new IllegalStateException(path + " blockingKnownIssueCount was " + blockingKnownIssueCount + "; expected " + expectedBlockingKnownIssueCount);
        }
        verifyNoPublicBetaOpeningUnsafeWork(path, data);
    }

    private static void verifyPublicBetaModuleCoverage(String path, String expectedStatus, boolean expectedReady, int expectedRequiredModuleCount, int expectedRequiredFeatureCount, int expectedMissingOptionalFeatureCount) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        verifyM17Envelope(path, envelope, expectedStatus);
        if (!Boolean.valueOf(expectedReady).equals(data.get("publicBetaReady"))) {
            throw new IllegalStateException(path + " publicBetaReady was " + data.get("publicBetaReady") + "; expected " + expectedReady);
        }
        if (expectedReady && !Boolean.TRUE.equals(data.get("allRequiredDescriptorsDiscovered"))) {
            throw new IllegalStateException(path + " allRequiredDescriptorsDiscovered must be true.");
        }
        if (expectedReady && !Boolean.TRUE.equals(data.get("allRequiredModulesLoadPlanned"))) {
            throw new IllegalStateException(path + " allRequiredModulesLoadPlanned must be true.");
        }
        if (expectedReady && !Boolean.TRUE.equals(data.get("allRequiredFeaturesProvided"))) {
            throw new IllegalStateException(path + " allRequiredFeaturesProvided must be true.");
        }
        if (expectedReady && !Boolean.TRUE.equals(data.get("noRequiredModuleStartupFailures"))) {
            throw new IllegalStateException(path + " noRequiredModuleStartupFailures must be true.");
        }
        if (!Long.valueOf(expectedRequiredModuleCount).equals(asLong(data.get("requiredModuleCount")))) {
            throw new IllegalStateException(path + " requiredModuleCount was " + data.get("requiredModuleCount") + "; expected " + expectedRequiredModuleCount);
        }
        if (!Long.valueOf(expectedRequiredFeatureCount).equals(asLong(data.get("requiredFeatureCount")))) {
            throw new IllegalStateException(path + " requiredFeatureCount was " + data.get("requiredFeatureCount") + "; expected " + expectedRequiredFeatureCount);
        }
        if (!Long.valueOf(expectedMissingOptionalFeatureCount).equals(asLong(data.get("missingOptionalFeatureCount")))) {
            throw new IllegalStateException(path + " missingOptionalFeatureCount was " + data.get("missingOptionalFeatureCount") + "; expected " + expectedMissingOptionalFeatureCount);
        }
        if (!Boolean.FALSE.equals(data.get("publicReleaseReady"))) {
            throw new IllegalStateException(path + " must keep publicReleaseReady=false.");
        }
        verifyNoPublicBetaOpeningUnsafeWork(path, data);
    }

    private static void verifyPublicBetaKnownLimitations(String path, String expectedStatus, boolean expectedReady, int expectedMissingOptionalFeatureCount, int expectedBlockingKnownIssueCount) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        verifyM17Envelope(path, envelope, expectedStatus);
        if (!Boolean.valueOf(expectedReady).equals(data.get("publicBetaReady"))) {
            throw new IllegalStateException(path + " publicBetaReady was " + data.get("publicBetaReady") + "; expected " + expectedReady);
        }
        if (!Long.valueOf(expectedBlockingKnownIssueCount).equals(asLong(data.get("blockingKnownIssueCount")))) {
            throw new IllegalStateException(path + " blockingKnownIssueCount was " + data.get("blockingKnownIssueCount") + "; expected " + expectedBlockingKnownIssueCount);
        }
        Object rawMissing = data.get("missingOptionalFeatures");
        int actualMissing = rawMissing instanceof List<?> missing ? missing.size() : -1;
        if (actualMissing != expectedMissingOptionalFeatureCount) {
            throw new IllegalStateException(path + " missingOptionalFeatures size was " + (actualMissing < 0 ? "not a list" : actualMissing) + "; expected " + expectedMissingOptionalFeatureCount);
        }
        if (!Boolean.FALSE.equals(data.get("publicReleaseReady"))) {
            throw new IllegalStateException(path + " must keep publicReleaseReady=false.");
        }
        verifyNoPublicBetaOpeningUnsafeWork(path, data);
    }

    private static void verifyPhase13M31Completion(String path, String expectedStatus, boolean expectedComplete, boolean expectedM32Ready) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        verifyM17Envelope(path, envelope, expectedStatus);
        if (!Boolean.valueOf(expectedComplete).equals(data.get("phase13M31Complete"))) {
            throw new IllegalStateException(path + " phase13M31Complete was " + data.get("phase13M31Complete") + "; expected " + expectedComplete);
        }
        if (!Boolean.valueOf(expectedM32Ready).equals(data.get("phase13M32Ready"))) {
            throw new IllegalStateException(path + " phase13M32Ready was " + data.get("phase13M32Ready") + "; expected " + expectedM32Ready);
        }
        if (!Boolean.FALSE.equals(data.get("publicReleaseReady"))) {
            throw new IllegalStateException(path + " must keep publicReleaseReady=false.");
        }
        verifyNoPublicBetaOpeningUnsafeWork(path, data);
    }

    private static void verifyPhase13M17Completion(String path, String expectedStatus, boolean expectedComplete, int expectedBlockerCount) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        verifyM17Envelope(path, envelope, expectedStatus);
        if (!Boolean.valueOf(expectedComplete).equals(data.get("phase13M17Complete"))) {
            throw new IllegalStateException(path + " phase13M17Complete was " + data.get("phase13M17Complete") + "; expected " + expectedComplete);
        }
        if (!Boolean.valueOf(expectedComplete).equals(data.get("phase13M18Ready"))) {
            throw new IllegalStateException(path + " phase13M18Ready was " + data.get("phase13M18Ready") + "; expected " + expectedComplete);
        }
        if (!Long.valueOf(expectedBlockerCount).equals(asLong(data.get("blockedReportCount")))) {
            throw new IllegalStateException(path + " blockedReportCount was " + data.get("blockedReportCount") + "; expected " + expectedBlockerCount);
        }
        if (!Boolean.FALSE.equals(data.get("playtestCandidateReady"))) {
            throw new IllegalStateException(path + " must not mark playtestCandidateReady during M17 closeout.");
        }
        verifyNoM17UnsafeLaunchWork(path, data);
    }

    private static void verifyPhase13M18ReadinessAudit(String path, String expectedStatus, boolean expectedReady, int expectedBlockerCount) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        verifyM17Envelope(path, envelope, expectedStatus);
        if (!Boolean.valueOf(expectedReady).equals(data.get("phase13M18Ready"))) {
            throw new IllegalStateException(path + " phase13M18Ready was " + data.get("phase13M18Ready") + "; expected " + expectedReady);
        }
        if (!Long.valueOf(expectedBlockerCount).equals(asLong(data.get("blockedReportCount")))) {
            throw new IllegalStateException(path + " blockedReportCount was " + data.get("blockedReportCount") + "; expected " + expectedBlockerCount);
        }
        if (!Boolean.FALSE.equals(data.get("playtestCandidateReady"))) {
            throw new IllegalStateException(path + " must not mark playtestCandidateReady during M18 readiness audit.");
        }
        verifyNoM17UnsafeLaunchWork(path, data);
    }

    private static void verifySmokeSessionPlan(String path, String expectedStatus, boolean expectedReady) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        verifyM17Envelope(path, envelope, expectedStatus);
        if (!Boolean.valueOf(expectedReady).equals(data.get("smokeSessionPlanReady"))) {
            throw new IllegalStateException(path + " smokeSessionPlanReady was " + data.get("smokeSessionPlanReady") + "; expected " + expectedReady);
        }
        if (!Boolean.FALSE.equals(data.get("processLaunchAllowed"))) {
            throw new IllegalStateException(path + " must keep processLaunchAllowed=false.");
        }
        verifyNoM17UnsafeLaunchWork(path, data);
    }

    private static void verifySmokeSessionSafetyGate(String path, String expectedStatus, boolean expectedReady) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        verifyM17Envelope(path, envelope, expectedStatus);
        if (!Boolean.valueOf(expectedReady).equals(data.get("smokeSessionSafetyGatePassed"))) {
            throw new IllegalStateException(path + " smokeSessionSafetyGatePassed was " + data.get("smokeSessionSafetyGatePassed") + "; expected " + expectedReady);
        }
        if (!Boolean.valueOf(expectedReady).equals(data.get("m18SmokeSessionAllowed"))) {
            throw new IllegalStateException(path + " m18SmokeSessionAllowed was " + data.get("m18SmokeSessionAllowed") + "; expected " + expectedReady);
        }
        if (!Boolean.FALSE.equals(data.get("processLaunchAllowed"))) {
            throw new IllegalStateException(path + " must keep processLaunchAllowed=false.");
        }
        if (!Boolean.FALSE.equals(data.get("runtimeClassResolutionAllowed"))) {
            throw new IllegalStateException(path + " must keep runtimeClassResolutionAllowed=false.");
        }
        verifyNoM17UnsafeLaunchWork(path, data);
    }

    private static void verifySmokeSessionResult(String path, String expectedStatus, boolean expectedReady) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        verifyM17Envelope(path, envelope, expectedStatus);
        if (!Boolean.valueOf(expectedReady).equals(data.get("smokeSessionComplete"))) {
            throw new IllegalStateException(path + " smokeSessionComplete was " + data.get("smokeSessionComplete") + "; expected " + expectedReady);
        }
        if (!Boolean.valueOf(expectedReady).equals(data.get("phase13M18Complete"))) {
            throw new IllegalStateException(path + " phase13M18Complete was " + data.get("phase13M18Complete") + "; expected " + expectedReady);
        }
        if (!Boolean.FALSE.equals(data.get("mainMenuReached"))) {
            throw new IllegalStateException(path + " must keep mainMenuReached=false.");
        }
        verifyNoM17UnsafeLaunchWork(path, data);
    }

    private static void verifySmokeSessionDiagnostics(String path, String expectedStatus, boolean expectedReady) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        verifyM17Envelope(path, envelope, expectedStatus);
        if (!Boolean.valueOf(expectedReady).equals(data.get("smokeSessionDiagnosticsReady"))) {
            throw new IllegalStateException(path + " smokeSessionDiagnosticsReady was " + data.get("smokeSessionDiagnosticsReady") + "; expected " + expectedReady);
        }
        verifyNoM17UnsafeLaunchWork(path, data);
    }

    private static void verifyPhase13M18Completion(String path, String expectedStatus, boolean expectedComplete, boolean expectedM19Ready) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        verifyM17Envelope(path, envelope, expectedStatus);
        if (!Boolean.valueOf(expectedComplete).equals(data.get("phase13M18Complete"))) {
            throw new IllegalStateException(path + " phase13M18Complete was " + data.get("phase13M18Complete") + "; expected " + expectedComplete);
        }
        if (!Boolean.valueOf(expectedM19Ready).equals(data.get("phase13M19Ready"))) {
            throw new IllegalStateException(path + " phase13M19Ready was " + data.get("phase13M19Ready") + "; expected " + expectedM19Ready);
        }
        if (!Boolean.FALSE.equals(data.get("firstPlaytestOpen"))) {
            throw new IllegalStateException(path + " must keep firstPlaytestOpen=false.");
        }
        if (!Boolean.FALSE.equals(data.get("playtestCandidateReady"))) {
            throw new IllegalStateException(path + " must keep playtestCandidateReady=false.");
        }
        verifyNoM17UnsafeLaunchWork(path, data);
    }

    private static void verifyPhase13M19Readiness(String path, String expectedStatus, boolean expectedReady) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        verifyM17Envelope(path, envelope, expectedStatus);
        if (!Boolean.valueOf(expectedReady).equals(data.get("phase13M19Ready"))) {
            throw new IllegalStateException(path + " phase13M19Ready was " + data.get("phase13M19Ready") + "; expected " + expectedReady);
        }
        if (!Boolean.FALSE.equals(data.get("firstPlaytestOpen"))) {
            throw new IllegalStateException(path + " must keep firstPlaytestOpen=false.");
        }
        if (!Boolean.FALSE.equals(data.get("playtestCandidateReady"))) {
            throw new IllegalStateException(path + " must keep playtestCandidateReady=false.");
        }
        if (expectedReady && (!(data.get("requiredNextWork") instanceof List<?> requiredNextWork) || requiredNextWork.isEmpty())) {
            throw new IllegalStateException(path + " must include requiredNextWork before M19 starts.");
        }
        verifyNoM17UnsafeLaunchWork(path, data);
    }

    private static void verifyFirstPlaytestCandidatePackage(String path, String expectedStatus, boolean expectedReady) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        verifyM17Envelope(path, envelope, expectedStatus);
        if (!Boolean.valueOf(expectedReady).equals(data.get("testerSafePackageReady"))) {
            throw new IllegalStateException(path + " testerSafePackageReady was " + data.get("testerSafePackageReady") + "; expected " + expectedReady);
        }
        if (!Boolean.valueOf(expectedReady).equals(data.get("playtestCandidateReady"))) {
            throw new IllegalStateException(path + " playtestCandidateReady was " + data.get("playtestCandidateReady") + "; expected " + expectedReady);
        }
        if (!(data.get("candidateIncludes") instanceof List<?> includes) || includes.size() < 6) {
            throw new IllegalStateException(path + " must list tester package contents.");
        }
        verifyNoM17UnsafeLaunchWork(path, data);
    }

    private static void verifyFirstPlaytestSupportBundle(String path, String expectedStatus, boolean expectedReady) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        verifyM17Envelope(path, envelope, expectedStatus);
        if (!Boolean.valueOf(expectedReady).equals(data.get("supportBundleExportReady"))) {
            throw new IllegalStateException(path + " supportBundleExportReady was " + data.get("supportBundleExportReady") + "; expected " + expectedReady);
        }
        if (expectedReady && !Boolean.TRUE.equals(data.get("supportBundleLocalOnly"))) {
            throw new IllegalStateException(path + " support bundle must be local-only when ready.");
        }
        if (!Boolean.FALSE.equals(data.get("supportBundleUploadsAutomatically"))) {
            throw new IllegalStateException(path + " support bundle must not upload automatically.");
        }
        verifyNoM17UnsafeLaunchWork(path, data);
    }

    private static void verifyFirstPlaytestRollbackNotes(String path, String expectedStatus, boolean expectedReady) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        verifyM17Envelope(path, envelope, expectedStatus);
        if (!Boolean.valueOf(expectedReady).equals(data.get("rollbackNotesReady"))) {
            throw new IllegalStateException(path + " rollbackNotesReady was " + data.get("rollbackNotesReady") + "; expected " + expectedReady);
        }
        if (!(data.get("notes") instanceof List<?> notes) || notes.size() < 3) {
            throw new IllegalStateException(path + " must include rollback notes.");
        }
        verifyNoM17UnsafeLaunchWork(path, data);
    }

    private static void verifyFirstPlaytestKnownLimitations(String path, String expectedStatus, boolean expectedReady) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        verifyM17Envelope(path, envelope, expectedStatus);
        if (!Boolean.valueOf(expectedReady).equals(data.get("knownLimitationsReady"))) {
            throw new IllegalStateException(path + " knownLimitationsReady was " + data.get("knownLimitationsReady") + "; expected " + expectedReady);
        }
        if (!(data.get("limitations") instanceof List<?> limitations) || limitations.size() < 4) {
            throw new IllegalStateException(path + " must include known limitations.");
        }
        verifyNoM17UnsafeLaunchWork(path, data);
    }

    private static void verifyExperimentalNativeLoaderLabel(String path, String expectedStatus, boolean expectedReady) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        verifyM17Envelope(path, envelope, expectedStatus);
        if (!Boolean.valueOf(expectedReady).equals(data.get("experimentalNativeLoaderLabelReady"))) {
            throw new IllegalStateException(path + " experimentalNativeLoaderLabelReady was " + data.get("experimentalNativeLoaderLabelReady") + "; expected " + expectedReady);
        }
        if (!Boolean.TRUE.equals(data.get("labelRequired"))) {
            throw new IllegalStateException(path + " must require the experimental native loader label.");
        }
        if (String.valueOf(data.get("labelText")).isBlank()) {
            throw new IllegalStateException(path + " must include labelText.");
        }
        verifyNoM17UnsafeLaunchWork(path, data);
    }

    private static void verifyFirstPlaytestCrashReportCollection(String path, String expectedStatus, boolean expectedReady) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        verifyM17Envelope(path, envelope, expectedStatus);
        if (!Boolean.valueOf(expectedReady).equals(data.get("crashReportCollectionReady"))) {
            throw new IllegalStateException(path + " crashReportCollectionReady was " + data.get("crashReportCollectionReady") + "; expected " + expectedReady);
        }
        if (!Boolean.FALSE.equals(data.get("uploadsAutomatically"))) {
            throw new IllegalStateException(path + " crash/report collection must not upload automatically.");
        }
        if (!(data.get("collectionSteps") instanceof List<?> steps) || steps.size() < 3) {
            throw new IllegalStateException(path + " must include crash/report collection steps.");
        }
        verifyNoM17UnsafeLaunchWork(path, data);
    }

    private static void verifyPhase13M19Completion(String path, String expectedStatus, boolean expectedReady) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        verifyM17Envelope(path, envelope, expectedStatus);
        if (!Boolean.valueOf(expectedReady).equals(data.get("phase13M19Complete"))) {
            throw new IllegalStateException(path + " phase13M19Complete was " + data.get("phase13M19Complete") + "; expected " + expectedReady);
        }
        if (!Boolean.valueOf(expectedReady).equals(data.get("playtestCandidateReady"))) {
            throw new IllegalStateException(path + " playtestCandidateReady was " + data.get("playtestCandidateReady") + "; expected " + expectedReady);
        }
        if (!Boolean.valueOf(expectedReady).equals(data.get("firstPlaytestOpen"))) {
            throw new IllegalStateException(path + " firstPlaytestOpen was " + data.get("firstPlaytestOpen") + "; expected " + expectedReady);
        }
        verifyNoM17UnsafeLaunchWork(path, data);
    }

    private static void verifyFirstPlaytestOpenGate(String path, String expectedStatus, boolean expectedReady) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        verifyM17Envelope(path, envelope, expectedStatus);
        if (!Boolean.valueOf(expectedReady).equals(data.get("firstPlaytestOpen"))) {
            throw new IllegalStateException(path + " firstPlaytestOpen was " + data.get("firstPlaytestOpen") + "; expected " + expectedReady);
        }
        if (!Boolean.valueOf(expectedReady).equals(data.get("safeToOpenFirstPlaytest"))) {
            throw new IllegalStateException(path + " safeToOpenFirstPlaytest was " + data.get("safeToOpenFirstPlaytest") + "; expected " + expectedReady);
        }
        if (!Boolean.FALSE.equals(data.get("publicPlaytestOpen"))) {
            throw new IllegalStateException(path + " must keep publicPlaytestOpen=false.");
        }
        verifyNoM17UnsafeLaunchWork(path, data);
    }

    private static void verifyPhase13FirstPlaytestBlockers(String path, String expectedStatus, boolean expectedOpen, int expectedBlockerCount) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        verifyM17Envelope(path, envelope, expectedStatus);
        if (!Boolean.valueOf(expectedOpen).equals(data.get("firstPlaytestOpen"))) {
            throw new IllegalStateException(path + " firstPlaytestOpen was " + data.get("firstPlaytestOpen") + "; expected " + expectedOpen);
        }
        if (!Boolean.FALSE.equals(data.get("firstPlaytestCandidateReady"))) {
            throw new IllegalStateException(path + " must not mark firstPlaytestCandidateReady before M18/M19 pass.");
        }
        if (!Long.valueOf(expectedBlockerCount).equals(asLong(data.get("blockerCount")))) {
            throw new IllegalStateException(path + " blockerCount was " + data.get("blockerCount") + "; expected " + expectedBlockerCount);
        }
        verifyNoM17UnsafeLaunchWork(path, data);
    }

    private static void verifyPhase13FirstPlaytestRoadmap(String path, String expectedStatus, boolean expectedOpen, boolean expectedLaunchSafe, int expectedActionCount) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        verifyM17Envelope(path, envelope, expectedStatus);
        if (!Boolean.valueOf(expectedOpen).equals(data.get("firstPlaytestOpen"))) {
            throw new IllegalStateException(path + " firstPlaytestOpen was " + data.get("firstPlaytestOpen") + "; expected " + expectedOpen);
        }
        if (!Boolean.valueOf(expectedLaunchSafe).equals(data.get("safeToAttemptIsolatedLaunch"))) {
            throw new IllegalStateException(path + " safeToAttemptIsolatedLaunch was " + data.get("safeToAttemptIsolatedLaunch") + "; expected " + expectedLaunchSafe);
        }
        if (!Boolean.valueOf(expectedOpen).equals(data.get("safeToOpenFirstPlaytest"))) {
            throw new IllegalStateException(path + " safeToOpenFirstPlaytest was " + data.get("safeToOpenFirstPlaytest") + "; expected " + expectedOpen);
        }
        if (!Long.valueOf(expectedActionCount).equals(asLong(data.get("nextActionCount")))) {
            throw new IllegalStateException(path + " nextActionCount was " + data.get("nextActionCount") + "; expected " + expectedActionCount);
        }
        if (!(data.get("milestones") instanceof List<?> milestones) || milestones.size() != 3) {
            throw new IllegalStateException(path + " must report exactly three remaining first-playtest milestones.");
        }
        verifyNoM17UnsafeLaunchWork(path, data);
    }

    private static void verifyPhase13FirstPlaytestNextActions(String path, String expectedStatus, int expectedActionCount) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        verifyM17Envelope(path, envelope, expectedStatus);
        if (!Long.valueOf(expectedActionCount).equals(asLong(data.get("actionCount")))) {
            throw new IllegalStateException(path + " actionCount was " + data.get("actionCount") + "; expected " + expectedActionCount);
        }
        if (!(data.get("actions") instanceof List<?> actions) || actions.size() != expectedActionCount) {
            throw new IllegalStateException(path + " actions size did not match actionCount.");
        }
        if (!(data.get("validationCommands") instanceof List<?> commands) || commands.isEmpty()) {
            throw new IllegalStateException(path + " must include validationCommands.");
        }
        verifyNoM17UnsafeLaunchWork(path, data);
    }

    private static void verifyPhase13FirstPlaytestFullRoadmap(String path, String expectedStatus, int expectedMilestoneCount, int expectedCompleteCount, String expectedFirstIncomplete) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        verifyM17Envelope(path, envelope, expectedStatus);
        if (!Long.valueOf(expectedMilestoneCount).equals(asLong(data.get("milestoneCount")))) {
            throw new IllegalStateException(path + " milestoneCount was " + data.get("milestoneCount") + "; expected " + expectedMilestoneCount);
        }
        if (!Long.valueOf(expectedCompleteCount).equals(asLong(data.get("completedMilestoneCount")))) {
            throw new IllegalStateException(path + " completedMilestoneCount was " + data.get("completedMilestoneCount") + "; expected " + expectedCompleteCount);
        }
        if (!expectedFirstIncomplete.equals(data.get("firstIncompleteMilestone"))) {
            throw new IllegalStateException(path + " firstIncompleteMilestone was " + data.get("firstIncompleteMilestone") + "; expected " + expectedFirstIncomplete);
        }
        if (!(data.get("milestones") instanceof List<?> milestones) || milestones.size() != expectedMilestoneCount) {
            throw new IllegalStateException(path + " must report the complete M2-M19 roadmap.");
        }
        if (!(data.get("remainingMilestones") instanceof List<?> remainingMilestones) || remainingMilestones.size() != 3) {
            throw new IllegalStateException(path + " must report exactly three M17-M19 remaining milestones.");
        }
        boolean expectedOpen = expectedFirstIncomplete.isBlank();
        if (!Boolean.valueOf(expectedOpen).equals(data.get("safeToOpenFirstPlaytest"))) {
            throw new IllegalStateException(path + " safeToOpenFirstPlaytest was " + data.get("safeToOpenFirstPlaytest") + "; expected " + expectedOpen);
        }
        verifyNoM17UnsafeLaunchWork(path, data);
    }

    private static void verifyPhase14Preflight(String path, String expectedStatus, boolean expectedReady, int expectedActionCount) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        verifyM17Envelope(path, envelope, expectedStatus);
        Object phase14Ready = data.get("phase14Ready");
        if (phase14Ready != null && !Boolean.valueOf(expectedReady).equals(phase14Ready)) {
            throw new IllegalStateException(path + " phase14Ready was " + phase14Ready + "; expected " + expectedReady);
        }
        Object phase14Blocked = data.get("phase14Blocked");
        if (phase14Blocked != null && !Boolean.valueOf(!expectedReady).equals(phase14Blocked)) {
            throw new IllegalStateException(path + " phase14Blocked was " + phase14Blocked + "; expected " + !expectedReady);
        }
        Object standaloneRuntimeImplementationStarted = data.get("standaloneRuntimeImplementationStarted");
        if (standaloneRuntimeImplementationStarted != null && !Boolean.FALSE.equals(standaloneRuntimeImplementationStarted)) {
            throw new IllegalStateException(path + " must not start standalone runtime implementation.");
        }
        Object publicPlaytestOpen = data.get("publicPlaytestOpen");
        if (publicPlaytestOpen != null && !Boolean.FALSE.equals(publicPlaytestOpen)) {
            throw new IllegalStateException(path + " must keep publicPlaytestOpen=false.");
        }
        Object actionCount = data.get("actionCount");
        if (actionCount != null && !Long.valueOf(expectedActionCount).equals(asLong(actionCount))) {
            throw new IllegalStateException(path + " actionCount was " + actionCount + "; expected " + expectedActionCount);
        }
        verifyNoM17UnsafeLaunchWork(path, data);
    }

    private static void verifyLaunchRealityAudit(String path, String expectedStatus, boolean expectedPhase1Complete, boolean expectedHarnessOnly, boolean expectedBetaReady, int expectedBlockingDiagnostics) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        verifyM17Envelope(path, envelope, expectedStatus);
        if (!Boolean.valueOf(expectedPhase1Complete).equals(data.get("phase1Complete"))) {
            throw new IllegalStateException(path + " phase1Complete was " + data.get("phase1Complete") + "; expected " + expectedPhase1Complete);
        }
        if (!Boolean.valueOf(expectedHarnessOnly).equals(data.get("harnessOnlyLaunchAttempt"))) {
            throw new IllegalStateException(path + " harnessOnlyLaunchAttempt was " + data.get("harnessOnlyLaunchAttempt") + "; expected " + expectedHarnessOnly);
        }
        if (!Boolean.FALSE.equals(data.get("realMinecraftLaunchImplemented"))) {
            throw new IllegalStateException(path + " must keep realMinecraftLaunchImplemented=false until a real isolated process launcher exists.");
        }
        if (!Boolean.valueOf(expectedBetaReady).equals(data.get("nativeLoaderBetaReady"))) {
            throw new IllegalStateException(path + " nativeLoaderBetaReady was " + data.get("nativeLoaderBetaReady") + "; expected " + expectedBetaReady);
        }
        Map<String, Object> summary = EchoNativeJson.asObject(envelope.get("summary"));
        if (!Long.valueOf(expectedBlockingDiagnostics).equals(asLong(summary.get("blockingDiagnostics")))) {
            throw new IllegalStateException(path + " blockingDiagnostics was " + summary.get("blockingDiagnostics") + "; expected " + expectedBlockingDiagnostics);
        }
        verifyNoM17UnsafeLaunchWork(path, data);
    }

    private static void verifyLaunchCommandClassification(String path, String expectedStatus, boolean expectedHarnessOnly, boolean expectedRealLaunch, int expectedClassificationCount) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        verifyM17Envelope(path, envelope, expectedStatus);
        if (!Boolean.valueOf(expectedHarnessOnly).equals(data.get("harnessOnlyLaunchAttempt"))) {
            throw new IllegalStateException(path + " harnessOnlyLaunchAttempt was " + data.get("harnessOnlyLaunchAttempt") + "; expected " + expectedHarnessOnly);
        }
        if (!Boolean.valueOf(expectedRealLaunch).equals(data.get("realMinecraftLaunchImplemented"))) {
            throw new IllegalStateException(path + " realMinecraftLaunchImplemented was " + data.get("realMinecraftLaunchImplemented") + "; expected " + expectedRealLaunch);
        }
        if (!Long.valueOf(expectedClassificationCount).equals(asLong(data.get("classificationCount")))) {
            throw new IllegalStateException(path + " classificationCount was " + data.get("classificationCount") + "; expected " + expectedClassificationCount);
        }
        if (!(data.get("commands") instanceof List<?> commands) || commands.size() != expectedClassificationCount) {
            throw new IllegalStateException(path + " must include deterministic command classifications.");
        }
        verifyNoM17UnsafeLaunchWork(path, data);
    }

    private static void verifyNativeLoaderBetaNextActions(String path, String expectedStatus, boolean expectedBetaReady, int expectedActionCount) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        verifyM17Envelope(path, envelope, expectedStatus);
        if (!Boolean.valueOf(expectedBetaReady).equals(data.get("nativeLoaderBetaReady"))) {
            throw new IllegalStateException(path + " nativeLoaderBetaReady was " + data.get("nativeLoaderBetaReady") + "; expected " + expectedBetaReady);
        }
        if (!Long.valueOf(expectedActionCount).equals(asLong(data.get("actionCount")))) {
            throw new IllegalStateException(path + " actionCount was " + data.get("actionCount") + "; expected " + expectedActionCount);
        }
        if (!(data.get("actions") instanceof List<?> actions) || actions.size() != expectedActionCount) {
            throw new IllegalStateException(path + " must include deterministic next actions.");
        }
        verifyNoM17UnsafeLaunchWork(path, data);
    }

    private static void verifyIsolatedRuntimeWorkspace(String path, String expectedStatus, boolean expectedReady, boolean expectedMaterialized, int expectedDirectoryCount, int expectedBlockingDiagnostics) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        verifyM17Envelope(path, envelope, expectedStatus);
        Object phase2Complete = data.get("phase2Complete");
        if (phase2Complete != null && !Boolean.valueOf(expectedReady).equals(phase2Complete)) {
            throw new IllegalStateException(path + " phase2Complete was " + phase2Complete + "; expected " + expectedReady);
        }
        Object isolatedRuntimeWorkspaceReady = data.get("isolatedRuntimeWorkspaceReady");
        if (isolatedRuntimeWorkspaceReady != null && !Boolean.valueOf(expectedReady).equals(isolatedRuntimeWorkspaceReady)) {
            throw new IllegalStateException(path + " isolatedRuntimeWorkspaceReady was " + isolatedRuntimeWorkspaceReady + "; expected " + expectedReady);
        }
        Object fixtureWorkspaceMaterialized = data.get("fixtureWorkspaceMaterialized");
        if (fixtureWorkspaceMaterialized != null && !Boolean.valueOf(expectedMaterialized).equals(fixtureWorkspaceMaterialized)) {
            throw new IllegalStateException(path + " fixtureWorkspaceMaterialized was " + fixtureWorkspaceMaterialized + "; expected " + expectedMaterialized);
        }
        Object requiredDirectoryCount = data.get("requiredDirectoryCount");
        if (requiredDirectoryCount != null && !Long.valueOf(expectedDirectoryCount).equals(asLong(requiredDirectoryCount))) {
            throw new IllegalStateException(path + " requiredDirectoryCount was " + requiredDirectoryCount + "; expected " + expectedDirectoryCount);
        }
        Object requiredDirectories = data.get("requiredDirectories");
        if (requiredDirectories != null && (!(requiredDirectories instanceof List<?> directories) || directories.size() != expectedDirectoryCount)) {
            throw new IllegalStateException(path + " must include deterministic required directory entries.");
        }
        Map<String, Object> summary = EchoNativeJson.asObject(envelope.get("summary"));
        if (!Long.valueOf(expectedBlockingDiagnostics).equals(asLong(summary.get("blockingDiagnostics")))) {
            throw new IllegalStateException(path + " blockingDiagnostics was " + summary.get("blockingDiagnostics") + "; expected " + expectedBlockingDiagnostics);
        }
        verifyNoM17UnsafeLaunchWork(path, data);
    }

    private static void verifyRealProcessLaunchHarness(String path, String expectedStatus, boolean expectedReady, int expectedArtifactCount, int expectedClasspathCount, int expectedBlockingDiagnostics) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        verifyM17Envelope(path, envelope, expectedStatus);
        if (!Boolean.valueOf(expectedReady).equals(data.get("phase3Complete"))) {
            throw new IllegalStateException(path + " phase3Complete was " + data.get("phase3Complete") + "; expected " + expectedReady);
        }
        if (!Boolean.valueOf(expectedReady).equals(data.get("processLaunchHarnessReady"))) {
            throw new IllegalStateException(path + " processLaunchHarnessReady was " + data.get("processLaunchHarnessReady") + "; expected " + expectedReady);
        }
        if (!Long.valueOf(expectedArtifactCount).equals(asLong(data.get("mappedArtifactCount")))) {
            throw new IllegalStateException(path + " mappedArtifactCount was " + data.get("mappedArtifactCount") + "; expected " + expectedArtifactCount);
        }
        if (!Long.valueOf(expectedClasspathCount).equals(asLong(data.get("plannedClasspathEntryCount")))) {
            throw new IllegalStateException(path + " plannedClasspathEntryCount was " + data.get("plannedClasspathEntryCount") + "; expected " + expectedClasspathCount);
        }
        if (!Boolean.FALSE.equals(data.get("realProcessLaunchImplemented"))) {
            throw new IllegalStateException(path + " must not claim realProcessLaunchImplemented yet.");
        }
        verifyBlockingDiagnostics(path, envelope, expectedBlockingDiagnostics);
        verifyNoM17UnsafeLaunchWork(path, data);
    }

    private static void verifyRealProcessLaunchSafetyGate(String path, String expectedStatus, boolean expectedReady, int expectedMappedCount, int expectedLocalCount, int expectedUnresolvedCount, int expectedBlockingDiagnostics) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        verifyM17Envelope(path, envelope, expectedStatus);
        if (!Boolean.valueOf(expectedReady).equals(data.get("processLaunchHarnessReady"))) {
            throw new IllegalStateException(path + " processLaunchHarnessReady was " + data.get("processLaunchHarnessReady") + "; expected " + expectedReady);
        }
        if (!Long.valueOf(expectedMappedCount).equals(asLong(data.get("mappedArtifactCount")))) {
            throw new IllegalStateException(path + " mappedArtifactCount was " + data.get("mappedArtifactCount") + "; expected " + expectedMappedCount);
        }
        if (!Long.valueOf(expectedLocalCount).equals(asLong(data.get("localArtifactCount")))) {
            throw new IllegalStateException(path + " localArtifactCount was " + data.get("localArtifactCount") + "; expected " + expectedLocalCount);
        }
        if (!Long.valueOf(expectedUnresolvedCount).equals(asLong(data.get("unresolvedArtifactCount")))) {
            throw new IllegalStateException(path + " unresolvedArtifactCount was " + data.get("unresolvedArtifactCount") + "; expected " + expectedUnresolvedCount);
        }
        if (!Boolean.FALSE.equals(data.get("launchExecutionAllowed")) || !Boolean.FALSE.equals(data.get("safeForProcessExecution"))) {
            throw new IllegalStateException(path + " must keep process execution disabled.");
        }
        verifyBlockingDiagnostics(path, envelope, expectedBlockingDiagnostics);
        verifyNoM17UnsafeLaunchWork(path, data);
    }

    private static void verifyRealProcessCommandLinePreview(String path, String expectedStatus, int expectedArgumentCount, int expectedClasspathCount, int expectedBlockingDiagnostics, String expectedMainClass, boolean expectedMainClassResolved) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        verifyM17Envelope(path, envelope, expectedStatus);
        if (!Long.valueOf(expectedArgumentCount).equals(asLong(data.get("argumentCount")))) {
            throw new IllegalStateException(path + " argumentCount was " + data.get("argumentCount") + "; expected " + expectedArgumentCount);
        }
        if (!Long.valueOf(expectedClasspathCount).equals(asLong(data.get("classpathEntryCount")))) {
            throw new IllegalStateException(path + " classpathEntryCount was " + data.get("classpathEntryCount") + "; expected " + expectedClasspathCount);
        }
        if (!Boolean.TRUE.equals(data.get("commandPreviewOnly")) || !Boolean.TRUE.equals(data.get("commandLineMaterialized"))) {
            throw new IllegalStateException(path + " must materialize a preview only.");
        }
        if (!expectedMainClass.equals(String.valueOf(data.getOrDefault("mainClass", "")))) {
            throw new IllegalStateException(path + " mainClass was " + data.get("mainClass") + "; expected " + expectedMainClass);
        }
        if (!Boolean.valueOf(expectedMainClassResolved).equals(data.get("mainClassResolved"))) {
            throw new IllegalStateException(path + " mainClassResolved was " + data.get("mainClassResolved") + "; expected " + expectedMainClassResolved);
        }
        if (!(data.get("commandLinePreview") instanceof List<?> arguments) || arguments.size() != expectedArgumentCount) {
            throw new IllegalStateException(path + " must include deterministic command-line preview arguments.");
        }
        String previewMainClass = previewArgument(arguments, "mainClass");
        if (!expectedMainClass.equals(previewMainClass)) {
            throw new IllegalStateException(path + " preview mainClass argument was " + previewMainClass + "; expected " + expectedMainClass);
        }
        verifyBlockingDiagnostics(path, envelope, expectedBlockingDiagnostics);
        verifyNoM17UnsafeLaunchWork(path, data);
    }

    private static String previewArgument(List<?> arguments, String expectedId) {
        for (Object raw : arguments) {
            Map<String, Object> argument = EchoNativeJson.asObject(raw);
            if (expectedId.equals(String.valueOf(argument.get("id")))) {
                return String.valueOf(argument.getOrDefault("value", ""));
            }
        }
        return "";
    }

    private static void verifyRealProcessEnvironmentPlan(String path, String expectedStatus, int expectedEnvironmentCount, int expectedBlockingDiagnostics) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        verifyM17Envelope(path, envelope, expectedStatus);
        if (!Long.valueOf(expectedEnvironmentCount).equals(asLong(data.get("environmentVariableCount")))) {
            throw new IllegalStateException(path + " environmentVariableCount was " + data.get("environmentVariableCount") + "; expected " + expectedEnvironmentCount);
        }
        if (!Boolean.TRUE.equals(data.get("secretSafe")) || !Boolean.TRUE.equals(data.get("redactsSecrets"))) {
            throw new IllegalStateException(path + " must keep environment data secret-safe.");
        }
        if (!(data.get("environmentVariables") instanceof List<?> variables) || variables.size() != expectedEnvironmentCount) {
            throw new IllegalStateException(path + " must include deterministic environment variables.");
        }
        verifyBlockingDiagnostics(path, envelope, expectedBlockingDiagnostics);
        verifyNoM17UnsafeLaunchWork(path, data);
    }

    private static void verifyProcessExecutionReadiness(String path, String expectedStatus, boolean expectedReady, int expectedGateCount, int expectedBlockingDiagnostics) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        verifyM17Envelope(path, envelope, expectedStatus);
        if (!Boolean.valueOf(expectedReady).equals(data.get("phase4Complete"))) {
            throw new IllegalStateException(path + " phase4Complete was " + data.get("phase4Complete") + "; expected " + expectedReady);
        }
        if (!Boolean.valueOf(expectedReady).equals(data.get("executionReadinessGateReady"))) {
            throw new IllegalStateException(path + " executionReadinessGateReady was " + data.get("executionReadinessGateReady") + "; expected " + expectedReady);
        }
        if (!Boolean.FALSE.equals(data.get("processExecutionEnabled")) || !Boolean.FALSE.equals(data.get("safeForAutomaticExecution"))) {
            throw new IllegalStateException(path + " must keep automatic process execution disabled.");
        }
        if (!Boolean.TRUE.equals(data.get("operatorAuthorizationRequired")) || !Boolean.TRUE.equals(data.get("requiresSeparateLaunchCommand"))) {
            throw new IllegalStateException(path + " must require explicit operator authorization and a separate launch command.");
        }
        if (!Long.valueOf(expectedGateCount).equals(asLong(data.get("gateCount")))) {
            throw new IllegalStateException(path + " gateCount was " + data.get("gateCount") + "; expected " + expectedGateCount);
        }
        if (!Long.valueOf(0).equals(asLong(data.get("forbiddenImportMatchCount")))) {
            throw new IllegalStateException(path + " must prove forbiddenImportMatchCount=0.");
        }
        verifyBlockingDiagnostics(path, envelope, expectedBlockingDiagnostics);
        verifyNoM17UnsafeLaunchWork(path, data);
    }

    private static void verifyControlledLaunchOperatorChecklist(String path, String expectedStatus, boolean expectedReady, int expectedChecklistCount, int expectedBlockingDiagnostics) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        verifyM17Envelope(path, envelope, expectedStatus);
        if (!Boolean.valueOf(expectedReady).equals(data.get("operatorChecklistReady"))) {
            throw new IllegalStateException(path + " operatorChecklistReady was " + data.get("operatorChecklistReady") + "; expected " + expectedReady);
        }
        if (!Long.valueOf(expectedChecklistCount).equals(asLong(data.get("checklistItemCount")))) {
            throw new IllegalStateException(path + " checklistItemCount was " + data.get("checklistItemCount") + "; expected " + expectedChecklistCount);
        }
        if (!(data.get("items") instanceof List<?> items) || items.size() != expectedChecklistCount) {
            throw new IllegalStateException(path + " must include deterministic checklist entries.");
        }
        verifyBlockingDiagnostics(path, envelope, expectedBlockingDiagnostics);
        verifyNoM17UnsafeLaunchWork(path, data);
    }

    private static void verifyControlledLaunchRollbackPlan(String path, String expectedStatus, boolean expectedReady, int expectedBlockingDiagnostics) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        verifyM17Envelope(path, envelope, expectedStatus);
        if (!Boolean.valueOf(expectedReady).equals(data.get("rollbackPlanReady"))) {
            throw new IllegalStateException(path + " rollbackPlanReady was " + data.get("rollbackPlanReady") + "; expected " + expectedReady);
        }
        if (!Boolean.TRUE.equals(data.get("rollbackRequiredBeforeExternalRelease"))) {
            throw new IllegalStateException(path + " must require rollback readiness before external release.");
        }
        verifyBlockingDiagnostics(path, envelope, expectedBlockingDiagnostics);
        verifyNoM17UnsafeLaunchWork(path, data);
    }

    private static void verifyNativeLoaderBetaGate(String path, String expectedStatus, boolean expectedReady, int expectedBlockingDiagnostics) throws IOException {
        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of(path))));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        verifyM17Envelope(path, envelope, expectedStatus);
        if (!Boolean.valueOf(expectedReady).equals(data.get("nativeLoaderBetaGateReady"))) {
            throw new IllegalStateException(path + " nativeLoaderBetaGateReady was " + data.get("nativeLoaderBetaGateReady") + "; expected " + expectedReady);
        }
        if (!Boolean.valueOf(expectedReady).equals(data.get("controlledLaunchReadyForAuthorization"))) {
            throw new IllegalStateException(path + " controlledLaunchReadyForAuthorization was " + data.get("controlledLaunchReadyForAuthorization") + "; expected " + expectedReady);
        }
        if (!Boolean.FALSE.equals(data.get("publicBetaReady"))) {
            throw new IllegalStateException(path + " must keep publicBetaReady=false.");
        }
        if (!Long.valueOf(0).equals(asLong(data.get("forbiddenImportMatchCount")))) {
            throw new IllegalStateException(path + " must prove forbiddenImportMatchCount=0.");
        }
        verifyBlockingDiagnostics(path, envelope, expectedBlockingDiagnostics);
        verifyNoM17UnsafeLaunchWork(path, data);
    }

    private static void verifyBlockingDiagnostics(String path, Map<String, Object> envelope, int expectedBlockingDiagnostics) {
        Map<String, Object> summary = EchoNativeJson.asObject(envelope.get("summary"));
        if (!Long.valueOf(expectedBlockingDiagnostics).equals(asLong(summary.get("blockingDiagnostics")))) {
            throw new IllegalStateException(path + " blockingDiagnostics was " + summary.get("blockingDiagnostics") + "; expected " + expectedBlockingDiagnostics);
        }
    }

    private static void verifyM17Envelope(String path, Map<String, Object> envelope, String expectedStatus) {
        if (!"1970-01-01T00:00:00Z".equals(envelope.get("generatedAt"))) {
            throw new IllegalStateException(path + " has a nondeterministic generatedAt value.");
        }
        if (!expectedStatus.equals(envelope.get("status"))) {
            throw new IllegalStateException(path + " status was " + envelope.get("status") + "; expected " + expectedStatus);
        }
    }

    private static void verifyM16Envelope(String path, Map<String, Object> envelope, String expectedStatus) {
        if (!"1970-01-01T00:00:00Z".equals(envelope.get("generatedAt"))) {
            throw new IllegalStateException(path + " has a nondeterministic generatedAt value.");
        }
        if (!expectedStatus.equals(envelope.get("status"))) {
            throw new IllegalStateException(path + " status was " + envelope.get("status") + "; expected " + expectedStatus);
        }
    }

    private static void verifyTransformEnvelope(String path, Map<String, Object> envelope, String expectedStatus) {
        if (!"1970-01-01T00:00:00Z".equals(envelope.get("generatedAt"))) {
            throw new IllegalStateException(path + " has a nondeterministic generatedAt value.");
        }
        if (!expectedStatus.equals(envelope.get("status"))) {
            throw new IllegalStateException(path + " status was " + envelope.get("status") + "; expected " + expectedStatus);
        }
    }

    private static void verifyNoRuntimeWork(String path, Map<String, Object> data) {
        if (!Boolean.FALSE.equals(data.get("commandExecuted"))) {
            throw new IllegalStateException(path + " must not execute a command.");
        }
        if (!Boolean.FALSE.equals(data.get("processLaunched"))) {
            throw new IllegalStateException(path + " must not launch a process.");
        }
        if (!Boolean.FALSE.equals(data.get("gameProcessLaunched"))) {
            throw new IllegalStateException(path + " must not launch a game process.");
        }
        Object classloaderCreated = data.get("classloaderCreated");
        if (classloaderCreated != null && !Boolean.FALSE.equals(classloaderCreated)) {
            throw new IllegalStateException(path + " must not create a classloader.");
        }
        Object resolvesRuntimeClasses = data.get("resolvesRuntimeClasses");
        if (resolvesRuntimeClasses != null && !Boolean.FALSE.equals(resolvesRuntimeClasses)) {
            throw new IllegalStateException(path + " must not resolve runtime classes.");
        }
        Object filesystemMutated = data.get("filesystemMutated");
        if (filesystemMutated != null && !Boolean.FALSE.equals(filesystemMutated)) {
            throw new IllegalStateException(path + " must not mutate the filesystem.");
        }
        Object unsafeRuntimeWorkStarted = data.get("unsafeRuntimeWorkStarted");
        if (unsafeRuntimeWorkStarted != null && !Boolean.FALSE.equals(unsafeRuntimeWorkStarted)) {
            throw new IllegalStateException(path + " must not start unsafe runtime work.");
        }
    }

    private static void verifyNoM2OrRuntimeWork(String path, Map<String, Object> data) {
        if (!Boolean.FALSE.equals(data.get("minecraftResolverStarted"))) {
            throw new IllegalStateException(path + " must not start the Minecraft resolver.");
        }
        Object remoteManifestDownloaded = data.get("remoteManifestDownloaded");
        if (remoteManifestDownloaded != null && !Boolean.FALSE.equals(remoteManifestDownloaded)) {
            throw new IllegalStateException(path + " must not download a remote Minecraft manifest.");
        }
        if (!Boolean.FALSE.equals(data.get("libraryDownloadStarted"))) {
            throw new IllegalStateException(path + " must not start library downloads.");
        }
        if (!Boolean.FALSE.equals(data.get("nativeExtractionStarted"))) {
            throw new IllegalStateException(path + " must not start native extraction.");
        }
        if (!Boolean.FALSE.equals(data.get("registryInjected"))) {
            throw new IllegalStateException(path + " must not inject registries.");
        }
        if (!Boolean.FALSE.equals(data.get("registryMutated"))) {
            throw new IllegalStateException(path + " must not mutate registries.");
        }
        if (!Boolean.FALSE.equals(data.get("gameClassesResolved"))) {
            throw new IllegalStateException(path + " must not resolve game classes.");
        }
        verifyNoRuntimeWork(path, data);
    }

    private static void verifyNoM3OrRuntimeWork(String path, Map<String, Object> data) {
        Object libraryResolverStarted = data.get("libraryResolverStarted");
        if (libraryResolverStarted != null && !Boolean.FALSE.equals(libraryResolverStarted)) {
            throw new IllegalStateException(path + " must not start the library resolver.");
        }
        Object cacheMutated = data.get("cacheMutated");
        if (cacheMutated != null && !Boolean.FALSE.equals(cacheMutated)) {
            throw new IllegalStateException(path + " must not mutate a library cache.");
        }
        verifyNoM2OrRuntimeWork(path, data);
    }

    private static void verifyNoM4OrRuntimeWork(String path, Map<String, Object> data) {
        Object classpathBuilderStarted = data.get("classpathBuilderStarted");
        if (classpathBuilderStarted != null && !Boolean.FALSE.equals(classpathBuilderStarted)) {
            throw new IllegalStateException(path + " must not start the classpath builder.");
        }
        Object productionClassloader = data.get("productionClassloader");
        if (productionClassloader != null && !Boolean.FALSE.equals(productionClassloader)) {
            throw new IllegalStateException(path + " must not create a production classloader.");
        }
        verifyNoM3OrRuntimeWork(path, data);
    }

    private static void verifyNoM5OrRuntimeWork(String path, Map<String, Object> data) {
        Object nativeExtractionAllowed = data.get("nativeExtractionAllowed");
        if (nativeExtractionAllowed != null && !Boolean.FALSE.equals(nativeExtractionAllowed)) {
            throw new IllegalStateException(path + " must not allow native extraction.");
        }
        Object nativeFilesExtracted = data.get("nativeFilesExtracted");
        if (nativeFilesExtracted != null && !Boolean.FALSE.equals(nativeFilesExtracted)) {
            throw new IllegalStateException(path + " must not extract native files.");
        }
        verifyNoM4OrRuntimeWork(path, data);
    }

    private static void verifyNoM6OrRuntimeWork(String path, Map<String, Object> data) {
        Object launchArgumentsPlannedOnly = data.get("launchArgumentsPlannedOnly");
        if (launchArgumentsPlannedOnly != null && !Boolean.TRUE.equals(launchArgumentsPlannedOnly)) {
            throw new IllegalStateException(path + " must keep launch arguments planned only.");
        }
        verifyNoM5OrRuntimeWork(path, data);
    }

    private static void verifyNoM7UnsafeGameWork(String path, Map<String, Object> data) {
        if (!Boolean.FALSE.equals(data.get("gameProcessLaunched"))) {
            throw new IllegalStateException(path + " must not launch a game process.");
        }
        Object minecraftLaunched = data.get("minecraftLaunched");
        if (minecraftLaunched != null && !Boolean.FALSE.equals(minecraftLaunched)) {
            throw new IllegalStateException(path + " must not launch Minecraft.");
        }
        Object classloaderCreated = data.get("classloaderCreated");
        if (classloaderCreated != null && !Boolean.FALSE.equals(classloaderCreated)) {
            throw new IllegalStateException(path + " must not create a classloader.");
        }
        Object resolvesRuntimeClasses = data.get("resolvesRuntimeClasses");
        if (resolvesRuntimeClasses != null && !Boolean.FALSE.equals(resolvesRuntimeClasses)) {
            throw new IllegalStateException(path + " must not resolve runtime classes.");
        }
        Object commandExecuted = data.get("commandExecuted");
        if (commandExecuted != null && !Boolean.FALSE.equals(commandExecuted)) {
            throw new IllegalStateException(path + " must not execute a command.");
        }
        Object filesystemMutated = data.get("filesystemMutated");
        if (filesystemMutated != null && !Boolean.FALSE.equals(filesystemMutated)) {
            throw new IllegalStateException(path + " must not mutate the filesystem outside deterministic reports.");
        }
        Object unsafeRuntimeWorkStarted = data.get("unsafeRuntimeWorkStarted");
        if (unsafeRuntimeWorkStarted != null && !Boolean.FALSE.equals(unsafeRuntimeWorkStarted)) {
            throw new IllegalStateException(path + " must not start unsafe runtime work.");
        }
        Object registryInjected = data.get("registryInjected");
        if (registryInjected != null && !Boolean.FALSE.equals(registryInjected)) {
            throw new IllegalStateException(path + " must not inject registries.");
        }
        Object registryMutated = data.get("registryMutated");
        if (registryMutated != null && !Boolean.FALSE.equals(registryMutated)) {
            throw new IllegalStateException(path + " must not mutate registries.");
        }
    }

    private static void verifyNoM8UnsafeGameWork(String path, Map<String, Object> data) {
        Object addonCodeExecuted = data.get("addonCodeExecuted");
        if (addonCodeExecuted != null && !Boolean.FALSE.equals(addonCodeExecuted)) {
            throw new IllegalStateException(path + " must not execute addon code.");
        }
        if (!Boolean.FALSE.equals(data.get("processLaunched"))) {
            throw new IllegalStateException(path + " must not launch a process during addon runtime discovery.");
        }
        verifyNoM7UnsafeGameWork(path, data);
    }

    private static void verifyNoM9UnsafeGameWork(String path, Map<String, Object> data) {
        Object realAddonCodeExecuted = data.get("realAddonCodeExecuted");
        if (realAddonCodeExecuted != null && !Boolean.FALSE.equals(realAddonCodeExecuted)) {
            throw new IllegalStateException(path + " must not execute real addon code.");
        }
        Object minecraftClassesResolved = data.get("minecraftClassesResolved");
        if (minecraftClassesResolved != null && !Boolean.FALSE.equals(minecraftClassesResolved)) {
            throw new IllegalStateException(path + " must not resolve Minecraft classes.");
        }
        Object registryBridgeTouched = data.get("registryBridgeTouched");
        if (registryBridgeTouched != null && !Boolean.FALSE.equals(registryBridgeTouched)) {
            throw new IllegalStateException(path + " must not touch registry bridge.");
        }
        Object transformsPerformed = data.get("transformsPerformed");
        if (transformsPerformed != null && !Boolean.FALSE.equals(transformsPerformed)) {
            throw new IllegalStateException(path + " must not perform transforms.");
        }
        Object transformsRequested = data.get("transformsRequested");
        if (transformsRequested != null && !Boolean.FALSE.equals(transformsRequested)) {
            throw new IllegalStateException(path + " must not request transforms.");
        }
        verifyNoM8UnsafeGameWork(path, data);
    }

    private static void verifyNoM10UnsafeGameWork(String path, Map<String, Object> data) {
        Object serviceCodeExecuted = data.get("serviceCodeExecuted");
        if (serviceCodeExecuted != null && !Boolean.FALSE.equals(serviceCodeExecuted)) {
            throw new IllegalStateException(path + " must not execute service code.");
        }
        Object inMemoryOnly = data.get("inMemoryOnly");
        if (inMemoryOnly != null && !Boolean.TRUE.equals(inMemoryOnly)) {
            throw new IllegalStateException(path + " must remain in-memory only.");
        }
        Object inertHandlesOnly = data.get("inertHandlesOnly");
        if (inertHandlesOnly != null && !Boolean.TRUE.equals(inertHandlesOnly)) {
            throw new IllegalStateException(path + " must use inert service handles only.");
        }
        verifyNoM9UnsafeGameWork(path, data);
    }

    private static void verifyNoM11UnsafeConfigWork(String path, Map<String, Object> data) {
        Object localOnly = data.get("localOnly");
        if (localOnly != null && !Boolean.TRUE.equals(localOnly)) {
            throw new IllegalStateException(path + " must keep config reads local-only.");
        }
        Object writePlanOnly = data.get("writePlanOnly");
        if (writePlanOnly != null && !Boolean.TRUE.equals(writePlanOnly)) {
            throw new IllegalStateException(path + " must keep config writes as plans only.");
        }
        Object installedConfigMutated = data.get("installedConfigMutated");
        if (installedConfigMutated != null && !Boolean.FALSE.equals(installedConfigMutated)) {
            throw new IllegalStateException(path + " must not mutate installed configs.");
        }
        Object fixtureConfigMutated = data.get("fixtureConfigMutated");
        if (fixtureConfigMutated != null && !Boolean.FALSE.equals(fixtureConfigMutated)) {
            throw new IllegalStateException(path + " must not mutate fixture configs in this prototype.");
        }
        Object installedConfigMutationAllowed = data.get("installedConfigMutationAllowed");
        if (installedConfigMutationAllowed != null && !Boolean.FALSE.equals(installedConfigMutationAllowed)) {
            throw new IllegalStateException(path + " must not allow installed config mutation.");
        }
        Object fixtureConfigMutationAllowed = data.get("fixtureConfigMutationAllowed");
        if (fixtureConfigMutationAllowed != null && !Boolean.FALSE.equals(fixtureConfigMutationAllowed)) {
            throw new IllegalStateException(path + " must not allow fixture config mutation.");
        }
        verifyNoM10UnsafeGameWork(path, data);
    }

    private static void verifyNoM12UnsafeResourceWork(String path, Map<String, Object> data) {
        Object localOnly = data.get("localOnly");
        if (localOnly != null && !Boolean.TRUE.equals(localOnly)) {
            throw new IllegalStateException(path + " must keep resource reads local-only.");
        }
        Object descriptorOnly = data.get("descriptorOnly");
        if (descriptorOnly != null && !Boolean.TRUE.equals(descriptorOnly)) {
            throw new IllegalStateException(path + " must keep resource bridge work descriptor-only.");
        }
        Object resourceRuntimeAccessed = data.get("resourceRuntimeAccessed");
        if (resourceRuntimeAccessed != null && !Boolean.FALSE.equals(resourceRuntimeAccessed)) {
            throw new IllegalStateException(path + " must not access runtime resources.");
        }
        Object resourceRuntimeAccessAllowed = data.get("resourceRuntimeAccessAllowed");
        if (resourceRuntimeAccessAllowed != null && !Boolean.FALSE.equals(resourceRuntimeAccessAllowed)) {
            throw new IllegalStateException(path + " must not allow runtime resource access.");
        }
        Object minecraftResourceManagerTouched = data.get("minecraftResourceManagerTouched");
        if (minecraftResourceManagerTouched != null && !Boolean.FALSE.equals(minecraftResourceManagerTouched)) {
            throw new IllegalStateException(path + " must not touch Minecraft ResourceManager.");
        }
        Object installedPackMutationAllowed = data.get("installedPackMutationAllowed");
        if (installedPackMutationAllowed != null && !Boolean.FALSE.equals(installedPackMutationAllowed)) {
            throw new IllegalStateException(path + " must not allow installed pack mutation.");
        }
        Object fixtureResourceMutationAllowed = data.get("fixtureResourceMutationAllowed");
        if (fixtureResourceMutationAllowed != null && !Boolean.FALSE.equals(fixtureResourceMutationAllowed)) {
            throw new IllegalStateException(path + " must not allow fixture resource mutation.");
        }
        verifyNoM11UnsafeConfigWork(path, data);
    }

    private static void verifyNoM13UnsafeRegistryWork(String path, Map<String, Object> data) {
        Object localOnly = data.get("localOnly");
        if (localOnly != null && !Boolean.TRUE.equals(localOnly)) {
            throw new IllegalStateException(path + " must keep registry reads local-only.");
        }
        Object sandboxOnly = data.get("sandboxOnly");
        if (sandboxOnly != null && !Boolean.TRUE.equals(sandboxOnly)) {
            throw new IllegalStateException(path + " must keep registry bridge work sandbox-only.");
        }
        Object minecraftRegistryTouched = data.get("minecraftRegistryTouched");
        if (minecraftRegistryTouched != null && !Boolean.FALSE.equals(minecraftRegistryTouched)) {
            throw new IllegalStateException(path + " must not touch Minecraft registries.");
        }
        Object minecraftRegistryAccessAllowed = data.get("minecraftRegistryAccessAllowed");
        if (minecraftRegistryAccessAllowed != null && !Boolean.FALSE.equals(minecraftRegistryAccessAllowed)) {
            throw new IllegalStateException(path + " must not allow Minecraft registry access.");
        }
        Object registryInjected = data.get("registryInjected");
        if (registryInjected != null && !Boolean.FALSE.equals(registryInjected)) {
            throw new IllegalStateException(path + " must not inject registries.");
        }
        Object registryInjectionAllowed = data.get("registryInjectionAllowed");
        if (registryInjectionAllowed != null && !Boolean.FALSE.equals(registryInjectionAllowed)) {
            throw new IllegalStateException(path + " must not allow registry injection.");
        }
        Object registryMutated = data.get("registryMutated");
        if (registryMutated != null && !Boolean.FALSE.equals(registryMutated)) {
            throw new IllegalStateException(path + " must not mutate registries.");
        }
        Object registryMutationAllowed = data.get("registryMutationAllowed");
        if (registryMutationAllowed != null && !Boolean.FALSE.equals(registryMutationAllowed)) {
            throw new IllegalStateException(path + " must not allow registry mutation.");
        }
        verifyNoM12UnsafeResourceWork(path, data);
    }

    private static void verifyNoM14UnsafeNetworkWork(String path, Map<String, Object> data) {
        Object localOnly = data.get("localOnly");
        if (localOnly != null && !Boolean.TRUE.equals(localOnly)) {
            throw new IllegalStateException(path + " must keep network descriptors local-only.");
        }
        Object descriptorOnly = data.get("descriptorOnly");
        if (descriptorOnly != null && !Boolean.TRUE.equals(descriptorOnly)) {
            throw new IllegalStateException(path + " must keep network bridge work descriptor-only.");
        }
        Object liveNetworkingStarted = data.get("liveNetworkingStarted");
        if (liveNetworkingStarted != null && !Boolean.FALSE.equals(liveNetworkingStarted)) {
            throw new IllegalStateException(path + " must not start live networking.");
        }
        Object liveNetworkingAllowed = data.get("liveNetworkingAllowed");
        if (liveNetworkingAllowed != null && !Boolean.FALSE.equals(liveNetworkingAllowed)) {
            throw new IllegalStateException(path + " must not allow live networking.");
        }
        Object socketOpened = data.get("socketOpened");
        if (socketOpened != null && !Boolean.FALSE.equals(socketOpened)) {
            throw new IllegalStateException(path + " must not open sockets.");
        }
        Object socketAllowed = data.get("socketAllowed");
        if (socketAllowed != null && !Boolean.FALSE.equals(socketAllowed)) {
            throw new IllegalStateException(path + " must not allow sockets.");
        }
        Object clientConnectionOpened = data.get("clientConnectionOpened");
        if (clientConnectionOpened != null && !Boolean.FALSE.equals(clientConnectionOpened)) {
            throw new IllegalStateException(path + " must not open client connections.");
        }
        Object clientConnectionAllowed = data.get("clientConnectionAllowed");
        if (clientConnectionAllowed != null && !Boolean.FALSE.equals(clientConnectionAllowed)) {
            throw new IllegalStateException(path + " must not allow client connections.");
        }
        Object serverConnectionOpened = data.get("serverConnectionOpened");
        if (serverConnectionOpened != null && !Boolean.FALSE.equals(serverConnectionOpened)) {
            throw new IllegalStateException(path + " must not open server connections.");
        }
        Object serverConnectionAllowed = data.get("serverConnectionAllowed");
        if (serverConnectionAllowed != null && !Boolean.FALSE.equals(serverConnectionAllowed)) {
            throw new IllegalStateException(path + " must not allow server connections.");
        }
        Object packetSent = data.get("packetSent");
        if (packetSent != null && !Boolean.FALSE.equals(packetSent)) {
            throw new IllegalStateException(path + " must not send packets.");
        }
        Object packetReceived = data.get("packetReceived");
        if (packetReceived != null && !Boolean.FALSE.equals(packetReceived)) {
            throw new IllegalStateException(path + " must not receive packets.");
        }
        verifyNoM13UnsafeRegistryWork(path, data);
    }

    private static void verifyNoM15UnsafeTransformWork(String path, Map<String, Object> data) {
        Object localOnly = data.get("localOnly");
        if (localOnly != null && !Boolean.TRUE.equals(localOnly)) {
            throw new IllegalStateException(path + " must keep transform descriptors local-only.");
        }
        Object descriptorOnly = data.get("descriptorOnly");
        if (descriptorOnly != null && !Boolean.TRUE.equals(descriptorOnly)) {
            throw new IllegalStateException(path + " must keep transform prototyping descriptor-only.");
        }
        Object transformPlanningOnly = data.get("transformPlanningOnly");
        if (transformPlanningOnly != null && !Boolean.TRUE.equals(transformPlanningOnly)) {
            throw new IllegalStateException(path + " must keep transform prototyping planning-only.");
        }
        Object transformsEnabled = data.get("transformsEnabled");
        if (transformsEnabled != null && !Boolean.FALSE.equals(transformsEnabled)) {
            throw new IllegalStateException(path + " must not enable transforms.");
        }
        Object transformsPerformed = data.get("transformsPerformed");
        if (transformsPerformed != null && !Boolean.FALSE.equals(transformsPerformed)) {
            throw new IllegalStateException(path + " must not perform transforms.");
        }
        Object minecraftTransformAllowed = data.get("minecraftTransformAllowed");
        if (minecraftTransformAllowed != null && !Boolean.FALSE.equals(minecraftTransformAllowed)) {
            throw new IllegalStateException(path + " must not allow Minecraft transforms.");
        }
        Object addonTransformAllowed = data.get("addonTransformAllowed");
        if (addonTransformAllowed != null && !Boolean.FALSE.equals(addonTransformAllowed)) {
            throw new IllegalStateException(path + " must not allow addon transforms.");
        }
        Object minecraftBytecodeTransformed = data.get("minecraftBytecodeTransformed");
        if (minecraftBytecodeTransformed != null && !Boolean.FALSE.equals(minecraftBytecodeTransformed)) {
            throw new IllegalStateException(path + " must not transform Minecraft bytecode.");
        }
        Object addonBytecodeTransformed = data.get("addonBytecodeTransformed");
        if (addonBytecodeTransformed != null && !Boolean.FALSE.equals(addonBytecodeTransformed)) {
            throw new IllegalStateException(path + " must not transform addon bytecode.");
        }
        Object bytecodeMutationAllowed = data.get("bytecodeMutationAllowed");
        if (bytecodeMutationAllowed != null && !Boolean.FALSE.equals(bytecodeMutationAllowed)) {
            throw new IllegalStateException(path + " must not allow bytecode mutation.");
        }
        Object bytecodeMutated = data.get("bytecodeMutated");
        if (bytecodeMutated != null && !Boolean.FALSE.equals(bytecodeMutated)) {
            throw new IllegalStateException(path + " must not mutate bytecode.");
        }
        Object classloaderAllowed = data.get("classloaderAllowed");
        if (classloaderAllowed != null && !Boolean.FALSE.equals(classloaderAllowed)) {
            throw new IllegalStateException(path + " must not allow classloader creation.");
        }
        verifyNoM14UnsafeNetworkWork(path, data);
    }

    private static void verifyNoM16UnsafeCrashHardeningWork(String path, Map<String, Object> data) {
        Object diagnosticsCaptured = data.get("diagnosticsCaptured");
        if (diagnosticsCaptured != null && !Boolean.TRUE.equals(diagnosticsCaptured)) {
            throw new IllegalStateException(path + " must capture deterministic diagnostics.");
        }
        Object supportBundlePlannedOnly = data.get("supportBundlePlannedOnly");
        if (supportBundlePlannedOnly != null && !Boolean.TRUE.equals(supportBundlePlannedOnly)) {
            throw new IllegalStateException(path + " must keep support bundle work planned-only.");
        }
        Object bundleWritten = data.get("bundleWritten");
        if (bundleWritten != null && !Boolean.FALSE.equals(bundleWritten)) {
            throw new IllegalStateException(path + " must not write a support bundle.");
        }
        verifyNoM15UnsafeTransformWork(path, data);
    }

    private static void verifyNoBetaEvidenceMutationOrUnsafeWork(String path, Map<String, Object> data) {
        Object commandExecuted = data.get("commandExecuted");
        if (commandExecuted != null && !Boolean.FALSE.equals(commandExecuted)) {
            throw new IllegalStateException(path + " must not execute commands while auditing beta evidence.");
        }
        Object classloaderCreated = data.get("classloaderCreated");
        if (classloaderCreated != null && !Boolean.FALSE.equals(classloaderCreated)) {
            throw new IllegalStateException(path + " must not create a classloader while auditing beta evidence.");
        }
        Object gameClassesResolved = data.get("gameClassesResolved");
        if (gameClassesResolved != null && !Boolean.FALSE.equals(gameClassesResolved)) {
            throw new IllegalStateException(path + " must not resolve runtime classes while auditing beta evidence.");
        }
        Object serviceCodeExecuted = data.get("serviceCodeExecuted");
        if (serviceCodeExecuted != null && !Boolean.FALSE.equals(serviceCodeExecuted)) {
            throw new IllegalStateException(path + " must not execute service code while auditing beta evidence.");
        }
        Object registryInjected = data.get("registryInjected");
        if (registryInjected != null && !Boolean.FALSE.equals(registryInjected)) {
            throw new IllegalStateException(path + " must not inject registries while auditing beta evidence.");
        }
        Object registryMutated = data.get("registryMutated");
        if (registryMutated != null && !Boolean.FALSE.equals(registryMutated)) {
            throw new IllegalStateException(path + " must not mutate registries while auditing beta evidence.");
        }
        Object transformsEnabled = data.get("transformsEnabled");
        if (transformsEnabled != null && !Boolean.FALSE.equals(transformsEnabled)) {
            throw new IllegalStateException(path + " must not enable transforms while auditing beta evidence.");
        }
        Object transformsPerformed = data.get("transformsPerformed");
        if (transformsPerformed != null && !Boolean.FALSE.equals(transformsPerformed)) {
            throw new IllegalStateException(path + " must not perform transforms while auditing beta evidence.");
        }
        Object bytecodeMutated = data.get("bytecodeMutated");
        if (bytecodeMutated != null && !Boolean.FALSE.equals(bytecodeMutated)) {
            throw new IllegalStateException(path + " must not mutate bytecode while auditing beta evidence.");
        }
        Object downloadsStarted = data.get("downloadsStarted");
        if (downloadsStarted != null && !Boolean.FALSE.equals(downloadsStarted)) {
            throw new IllegalStateException(path + " must not download while auditing beta evidence.");
        }
        Object libraryDownloadStarted = data.get("libraryDownloadStarted");
        if (libraryDownloadStarted != null && !Boolean.FALSE.equals(libraryDownloadStarted)) {
            throw new IllegalStateException(path + " must not download libraries while auditing beta evidence.");
        }
        Object nativeExtractionStarted = data.get("nativeExtractionStarted");
        if (nativeExtractionStarted != null && !Boolean.FALSE.equals(nativeExtractionStarted)) {
            throw new IllegalStateException(path + " must not extract natives while auditing beta evidence.");
        }
        Object unsafeRuntimeWorkStarted = data.get("unsafeRuntimeWorkStarted");
        if (unsafeRuntimeWorkStarted != null && !Boolean.FALSE.equals(unsafeRuntimeWorkStarted)) {
            throw new IllegalStateException(path + " must not start unsafe runtime work while auditing beta evidence.");
        }
        Object filesystemMutated = data.get("filesystemMutated");
        if (filesystemMutated != null && !Boolean.FALSE.equals(filesystemMutated)) {
            throw new IllegalStateException(path + " must not mutate the filesystem while auditing beta evidence.");
        }
        Object userCachesMutated = data.get("userCachesMutated");
        if (userCachesMutated != null && !Boolean.FALSE.equals(userCachesMutated)) {
            throw new IllegalStateException(path + " must not mutate user caches while auditing beta evidence.");
        }
        Object cacheMutated = data.get("cacheMutated");
        if (cacheMutated != null && !Boolean.FALSE.equals(cacheMutated)) {
            throw new IllegalStateException(path + " must not mutate caches while auditing beta evidence.");
        }
        Object launcherInstallsMutated = data.get("launcherInstallsMutated");
        if (launcherInstallsMutated != null && !Boolean.FALSE.equals(launcherInstallsMutated)) {
            throw new IllegalStateException(path + " must not mutate launcher installs while auditing beta evidence.");
        }
        Object jarsMutated = data.get("jarsMutated");
        if (jarsMutated != null && !Boolean.FALSE.equals(jarsMutated)) {
            throw new IllegalStateException(path + " must not mutate jars while auditing beta evidence.");
        }
        Object savesMutated = data.get("savesMutated");
        if (savesMutated != null && !Boolean.FALSE.equals(savesMutated)) {
            throw new IllegalStateException(path + " must not mutate saves while auditing beta evidence.");
        }
        Object configsMutated = data.get("configsMutated");
        if (configsMutated != null && !Boolean.FALSE.equals(configsMutated)) {
            throw new IllegalStateException(path + " must not mutate configs while auditing beta evidence.");
        }
        Object publicBetaOpen = data.get("publicBetaOpen");
        if (publicBetaOpen != null && !Boolean.FALSE.equals(publicBetaOpen)) {
            throw new IllegalStateException(path + " must keep publicBetaOpen=false while beta soak is incomplete.");
        }
        Object publicReleaseReady = data.get("publicReleaseReady");
        if (publicReleaseReady != null && !Boolean.FALSE.equals(publicReleaseReady)) {
            throw new IllegalStateException(path + " must keep publicReleaseReady=false while beta soak is incomplete.");
        }
    }

    private static void verifyNoBetaDraftUnsafeWork(String path, Map<String, Object> data) {
        Map<String, Object> copy = new LinkedHashMap<>(data);
        copy.put("filesystemMutated", false);
        verifyNoBetaEvidenceMutationOrUnsafeWork(path, copy);
        Object userCachesMutated = data.get("userCachesMutated");
        if (userCachesMutated != null && !Boolean.FALSE.equals(userCachesMutated)) {
            throw new IllegalStateException(path + " must not mutate user caches while preparing beta drafts.");
        }
        Object launcherInstallsMutated = data.get("launcherInstallsMutated");
        if (launcherInstallsMutated != null && !Boolean.FALSE.equals(launcherInstallsMutated)) {
            throw new IllegalStateException(path + " must not mutate launcher installs while preparing beta drafts.");
        }
        Object savesMutated = data.get("savesMutated");
        if (savesMutated != null && !Boolean.FALSE.equals(savesMutated)) {
            throw new IllegalStateException(path + " must not mutate saves while preparing beta drafts.");
        }
        Object configsMutated = data.get("configsMutated");
        if (configsMutated != null && !Boolean.FALSE.equals(configsMutated)) {
            throw new IllegalStateException(path + " must not mutate configs while preparing beta drafts.");
        }
        Object jarsMutated = data.get("jarsMutated");
        if (jarsMutated != null && !Boolean.FALSE.equals(jarsMutated)) {
            throw new IllegalStateException(path + " must not mutate jars while preparing beta drafts.");
        }
    }

    private static void verifyNoPublicBetaOpeningUnsafeWork(String path, Map<String, Object> data) {
        Map<String, Object> copy = new LinkedHashMap<>(data);
        copy.put("publicBetaOpen", false);
        verifyNoBetaEvidenceMutationOrUnsafeWork(path, copy);
    }

    private static void verifyNoM17UnsafeLaunchWork(String path, Map<String, Object> data) {
        Object userInstallMutationAllowed = data.get("userInstallMutationAllowed");
        if (userInstallMutationAllowed != null && !Boolean.FALSE.equals(userInstallMutationAllowed)) {
            throw new IllegalStateException(path + " must not allow user install mutation.");
        }
        Object packMutationAllowed = data.get("packMutationAllowed");
        if (packMutationAllowed != null && !Boolean.FALSE.equals(packMutationAllowed)) {
            throw new IllegalStateException(path + " must not allow pack mutation.");
        }
        Object saveMutationAllowed = data.get("saveMutationAllowed");
        if (saveMutationAllowed != null && !Boolean.FALSE.equals(saveMutationAllowed)) {
            throw new IllegalStateException(path + " must not allow save mutation.");
        }
        Object configMutationAllowed = data.get("configMutationAllowed");
        if (configMutationAllowed != null && !Boolean.FALSE.equals(configMutationAllowed)) {
            throw new IllegalStateException(path + " must not allow config mutation.");
        }
        Object libraryDownloadStarted = data.get("libraryDownloadStarted");
        if (libraryDownloadStarted != null && !Boolean.FALSE.equals(libraryDownloadStarted)) {
            throw new IllegalStateException(path + " must not download libraries.");
        }
        Object nativeExtractionStarted = data.get("nativeExtractionStarted");
        if (nativeExtractionStarted != null && !Boolean.FALSE.equals(nativeExtractionStarted)) {
            throw new IllegalStateException(path + " must not extract natives.");
        }
        verifyNoM16UnsafeCrashHardeningWork(path, data);
    }

    private static Long asLong(Object value) {
        return value instanceof Number number ? number.longValue() : null;
    }

    private static long expectedAshfallRuntimeModuleCount() throws IOException {
        Path modulesRoot = Path.of("fixtures/ashfall/modules");
        if (!Files.isDirectory(modulesRoot)) {
            throw new IllegalStateException("fixtures/ashfall/modules is missing.");
        }
        try (var stream = Files.list(modulesRoot)) {
            return stream
                    .filter(Files::isDirectory)
                    .filter(path -> Files.isRegularFile(path.resolve("META-INF/echo.mod.json")))
                    .count();
        }
    }

    private static int expectedAshfallClasspathEntryCount() throws IOException {
        return Math.toIntExact(expectedAshfallRuntimeModuleCount() + 53L);
    }

    private static int expectedAshfallRequiredFeatureCount() throws IOException {
        Map<String, Object> pack = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(Path.of("fixtures/ashfall/echo.pack.json"))));
        return EchoNativeJson.stringList(pack.get("requiredFeatures")).size();
    }

    private static int asInt(Object value) {
        return value instanceof Number number ? number.intValue() : 0;
    }

    private static void verifyNoLocalOnlyPaths(Map<String, String> snapshot) {
        String home = System.getProperty("user.home", "").replace('\\', '/');
        for (Map.Entry<String, String> entry : snapshot.entrySet()) {
            String text = entry.getValue().replace('\\', '/');
            if (text.contains("C:/") || (!home.isBlank() && text.contains(home))) {
                throw new IllegalStateException(entry.getKey() + " contains a local-only absolute path.");
            }
        }
    }
}
