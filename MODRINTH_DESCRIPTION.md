# PMRadar

PMRadar adds a radar layer to Xaero's World Map for ProtoManly's Weather. It uses PMWeather's radar stations so you can view storms on the map, switch radar views, and see each station's coverage area.

PMRadar adds:

- **Xaero's World Map overlay:** Shows PMWeather radar data directly on the full world map.
- **Correlation Coefficient mode:** Adds a custom Correlation Coefficient radar view, separate from PMWeather's normal radar modes. It helps show spots where debris may be mixed into stronger storms.
- **Dual Mode display:** Compare two radar views at once with a split map view.
- **WSR-88D tower detection:** Detects PMWeather WSR-88D radar towers and uses each one as a selectable radar source.
- **Broken radome handling:** If the radar shell is damaged but the core is still there, the station can still be found, but it stops producing radar returns until repaired.
- **Coverage ranges:** Each radar site covers **2,048 blocks** by default. A range upgrade module next to the nearby PMWeather radar display expands coverage to **8,192 blocks**.
- **Lightning markers:** Shows recent lightning strikes on the map and fades them out over time.
- **Radar Tools menu:** Toggle the radar layer, Dual Mode, radar locations, lightning markers, and animation options.

## Required mods

- [ProtoManly's Weather](https://modrinth.com/mod/protomanlys-weather)
- [Xaero's World Map](https://modrinth.com/mod/xaeros-world-map)
- [Xaero's Minimap](https://modrinth.com/mod/xaeros-minimap)
