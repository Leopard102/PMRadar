# Changelog

All notable changes to PMRadar are documented here.

## Unreleased

### Added

- Added a **Radar extension module** feature note to the project documentation.
- Added server-authoritative radar site synchronization for players joining an existing world.

### Fixed

- Fixed a Dual Mode map render crash caused by an unbalanced scissor stack.
- Radar sites now retain their upgraded **8,192-block** coverage when a player joins while the tower is outside that player's loaded chunks.
- Placing or breaking a range upgrade module now refreshes the radar site state for connected players.
- Radar towers in chunks already loaded by the server now sync to a joining player without waiting for that player's client to load each tower chunk.
