# PMRadar

Bring ProtoManly's Weather to your world map. PMRadar adds a weather radar overlay to Xaero's World Map, letting you follow storms, compare radar products, and check coverage from your radar stations without leaving the map screen.

## Features

- **Xaero's World Map radar overlay:** View PMWeather storm data directly on the full world map.
- **Reflectivity mode:** Shows precipitation intensity in dBZ using familiar weather radar colors.
- **Velocity mode:** Shows wind moving toward or away from the selected radar site in mph.
- **PMRadar Correlation Coefficient mode:** Adds a custom simulated CC product that is not part of the base PMWeather radar set. This mode compares storm return strength with tornado debris information to estimate where precipitation is clean, mixed, or debris-contaminated, making it useful for spotting suspicious circulation areas during severe weather.
- **Dual Mode display:** Compare two radar products at once with a split radar view.
- **Radar station discovery:** Detects PMWeather WSR-88D radar towers, assigns station codes, and remembers discovered sites between sessions.
- **Radar site markers:** Working stations appear with purple markers, while damaged stations appear with red markers.
- **Coverage ranges:** Radar stations have a base coverage radius of **2,048 blocks**. A detected range upgrade near the PMWeather radar display increases coverage to **8,192 blocks**.
- **Lightning markers:** Shows recent detected lightning strikes on the map with markers that fade over time.
- **Radar Tools menu:** Toggle the radar display, Dual Mode, radar location markers, lightning markers, menu animations, and text-swap animations.
- **Multiplayer support:** Server-side synchronization handles radar tower status and tornado debris data when PMRadar and PMWeather are installed on the server and participating clients.

## Required mods

- [ProtoManly's Weather](https://modrinth.com/mod/protomanlys-weather)
- [Xaero's World Map](https://modrinth.com/mod/xaeros-world-map)
- [Xaero's Minimap](https://modrinth.com/mod/xaeros-minimap)

Use the Minecraft 1.21.1 NeoForge versions of these mods, along with their own required dependencies. PMRadar will not work without PMWeather.

## Build

Install a Java 21 JDK and point `JAVA_HOME` at its installation directory. On Windows, run:

```powershell
.\gradlew.bat build
```

The mod JAR is written to `build/libs/`. Dependency versions are pinned in `build.gradle`; Minecraft and NeoForge versions are in `gradle.properties`.

For development, use `runClient` or `runServer` in place of `build`. Development worlds and settings are stored in `run/`.

## Code layout

- `client/`: radar sampling, cached state, map rendering, and controls.
- `server/`: tower discovery and debris synchronization.
- `network/`: client/server payloads and handlers.
- `mixin/`: integration with map zoom and distant storm chunk loading.
- `RadarTowerScanner`: shared tower structure validation.
- `RadarDebrisFilter`: shared debris material exclusions. Client and server callers retain their additional filtering rules.

## Project metadata

The current mod metadata declares All Rights Reserved. The NeoForge template has its own notice in TEMPLATE_LICENSE.txt.

The Modrinth page copy is in MODRINTH_DESCRIPTION.md, and its short summary is in MODRINTH_SUMMARY.txt.
