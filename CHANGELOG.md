# Changelog

All notable changes to PMRadar are documented here.

## Unreleased

### Added

- Added a **Radar extension module** feature note to the project documentation.
- Added server-authoritative radar site synchronization for players joining an existing world.
- Added persistent server-side radar tower records so known towers can be sent to joining players even when their chunks are unloaded.
- Added authoritative station codes to radar sync packets so all clients use the same radar station names.
- Added a one-time client cache hint so existing stations from older PMRadar sessions can seed the server registry without a player revisiting them.

### Fixed

- Fixed a Dual Mode map render crash caused by an unbalanced scissor stack.
- Fixed the radar controls' z-order by drawing them after Xaero's final map-overlay batch, keeping the player marker and all map overlays underneath the storm bar and mode controls.
- Moved PMRadar lightning markers into the pre-Xaero map pass so PMRadar overlays stay below Xaero's player marker and radar controls.
- Matched PMRadar's radar texture rebuild cadence to PMWeather's RadarRenderer: every 60 ticks (3 seconds).
- Removed the recurring full-area radar scan that could stall the server thread, cause player timeouts, and leave saving or map teleports unresponsive in radar-heavy worlds.
- Restored the original radar control bar placement after the player-marker spacing change caused an incorrect map layout.
- Radar sites now retain their upgraded **8,192-block** coverage when a player joins while the tower is outside that player's loaded chunks.
- Placing or breaking a range upgrade module now refreshes the radar site state for connected players.
- Radar towers in chunks already loaded by the server now sync to a joining player without waiting for that player's client to load each tower chunk.
- Joining players now receive a complete radar tower snapshot, including an empty snapshot when the server has no known towers, so stale client-only stations are cleared.
