# ECHO Native SDK

This folder is the author-facing documentation for writing ECHO Native addons without importing NeoForge or depending on Native Loader internals.

## Contract Boundaries

- [Public contract and versioning](PUBLIC_CONTRACT.md)
- [Runtime lanes](RUNTIME_LANES.md)
- [Unified player runtime](UNIFIED_PLAYER_RUNTIME.md)

## Addon Author Guides

- [Install guide](INSTALL_GUIDE.md)
- [Mod author guide](MOD_AUTHOR_GUIDE.md)
- [Native addon quickstart](NATIVE_ADDON_QUICKSTART.md)
- [Porting from NeoForge](PORTING_FROM_NEOFORGE.md)
- [AdapterCore service guide](ADAPTERCORE_SERVICE_GUIDE.md)
- [API reference](API_REFERENCE.md)
- [Example addon walkthrough](EXAMPLE_ADDON_WALKTHROUGH.md)
- [Registry guide](REGISTRY_GUIDE.md)
- [Events guide](EVENTS_GUIDE.md)
- [Networking guide](NETWORKING_GUIDE.md)
- [Config guide](CONFIG_GUIDE.md)
- [Client and render guide](CLIENT_RENDER_GUIDE.md)
- [Worldgen and data guide](WORLDGEN_DATA_GUIDE.md)
- [Testing and parity guide](TESTING_PARITY_GUIDE.md)
- [Troubleshooting](TROUBLESHOOTING.md)
- [Known limitations](KNOWN_LIMITATIONS.md)
- [Compatibility matrix](COMPATIBILITY_MATRIX.md)
- [Release packaging guide](RELEASE_PACKAGING_GUIDE.md)

## Definition Of Done For Addons

A native addon is done when it can compile against `echoaddonapi`, `echoadaptercore`, and `echo-native-contracts`, can be tested with `echo-native-testkit`, and can be packaged with an `echo.mod.json` descriptor without importing `echo-native-loader` or NeoForge classes.
