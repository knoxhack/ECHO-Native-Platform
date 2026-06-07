# Known Limitations

The public beta is native-first, but not yet a stable 1.0 API.

- Java 25 is the only supported Java target.
- Windows desktop is the locally verified launch lane in this workspace; Linux and macOS need CI evidence before public support.
- NeoForge remains a compatibility backend only. Addon runtime source must not import NeoForge directly.
- Release mode rejects dev classpath fallback.
- Metadata-only mutation claims are rejected.
- Some smoke evidence is generated from local harnesses and must be refreshed for every release candidate.
- Public SDK APIs marked `BETA` may still change before the 1.0.0 API freeze.
