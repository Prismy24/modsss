# Block Tracker (Fabric, Minecraft 1.21.11)

Client-side mod that draws coloured wireframe outlines around chosen block types near you.

## Build

1. Install JDK 21.
2. In this folder run `./gradlew build` (Windows: `gradlew build`). The Gradle wrapper is included,
   and the first build downloads Minecraft and Fabric, which takes a few minutes.
3. The jar is in `build/libs/blocktracker-1.3.0.jar` (not the `-sources` one). Put it in your `mods`
   folder together with Fabric API for 1.21.11.

## Use

- Press **K** to toggle (rebind under Controls > Block Tracker).
- `/track add <block> [#RRGGBB]` start tracking a block, e.g. `/track add iron_ore #FFFFFF`
- `/track remove <block>`
- `/track base` adds the blocks bases are usually built around (chests, furnaces, crafting tables, spawners, etc.)
- `/track list`
- `/track radius <1-64>` scan radius in blocks
- `/track walls <true|false>` outlines visible through walls (uses Minecraft's Gizmo system)
- `/track alpha <0.05-1>` outline opacity
- `/track toggle`

Settings persist in `config/blocktracker.json`. It starts with diamond ore and chests tracked.

## Notes

- The scan runs every 10 ticks and is capped at 2000 outlines.
- Through-walls mode uses vanilla's Gizmo cuboids with "always on top". If that fails at runtime,
  the mod logs a warning and falls back to ordinary depth-tested lines. Check the game log if
  outlines are hidden behind walls with `/track walls true`.
- Using wall-hack style tracking on multiplayer servers may be against their rules.
