# Just A Browser Mod

<<<<<<< HEAD
## WARNING THIS PROJECT IS STILL EXPERIMENTAL
=======
> **Experimental** — this project is still in active development. Expect rough edges,
> breaking changes and incomplete features between releases.
>>>>>>> ab896a0 (Docs)

**Just A Browser Mod** (JAB) adds fully functional web screens to Minecraft. Build a wall of
screen blocks, point at it and run a command, and the wall turns into a live browser panel
backed by an embedded Chromium instance (via [MCEF](https://github.com/Keksuccino/mcef)).

## Features

- **Multiblock screen walls** — any rectangle of screen blocks (2x2 or larger) becomes a
  display, on any face of any side of the blocks (walls, floors, ceilings).
- **Real browser rendering** — pages render onto the blocks in-game with texture quality
  tied to the GUI scale.
- **Interactive browser view** — right-click a screen to open the full browser GUI with
  back/forward/reload buttons, a URL bar (`Ctrl+L` to focus), and full mouse and keyboard
  forwarding to the page.
- **Audio modes** — screens can be set to *global* (page audio plays normally) or
  *dynamic* (page volume is driven by your distance from the wall, with a 64-block falloff).
- **Per-face displays** — one wall can show a different page on each of its six faces.

## Requirements

- Minecraft **1.21.11** (Fabric)
- [Fabric API](https://modrinth.com/mod/fabric-api)
- [MCEF](https://github.com/Keksuccino/mcef) (mcef-fabric, `2.2.0-1.21.11`)

## Usage

1. Craft screen blocks (4 per craft: iron ingots + redstone) and build a flat wall.
2. Look at the wall and run:

```
/jab create                     # create a display on the face you're looking at
/jab create https://youtube.com # create and load a URL immediately
/jab url <url>                  # change the page on the wall you're looking at
/jab resolution <w> <h>         # set the render resolution (default 1920x1080)
/jab audio global|dynamic       # switch audio mode for the wall
/jab remove                     # remove the display
/jab debug                      # print server-side wall info for debugging
```

3. Right-click the wall to open the interactive browser view.

Breaking any block of a wall also removes its display, so walls can't get stuck in a
half-broken state.

## Configuration

A `config/jab.properties` file is generated on first run:

| Key | Default | Description |
|---|---|---|
| `maxScreenSize` | `8` | Maximum wall dimension in blocks |
| `defaultResolutionX/Y` | `1920x1080` | Browser render resolution for new screens |
| `maxResolution` | `3840` | Hard cap on resolution |
| `defaultUrl` | `https://www.google.com` | Page loaded when a screen is created |
| `loadDistance` / `unloadDistance` | `32` / `48` | Browser lifecycle distance from the wall |

## Building

```
./gradlew build
```

The built jar lands in `build/libs/`. Drop it (plus the requirements above) into your
`mods/` folder.

## Credits

This mod is heavily inspired by (and builds on) the work of others:

- **[MCEF fork by Keksuccino](https://github.com/Keksuccino/mcef)** — the embedded Chromium
  framework that powers the browser rendering
- **[BrowserMod by Mcjunky33](https://github.com/Mcjunky33/BrowserMod)** — the original
  in-game browser concept
- **[WebDisplays by CinemaMod](https://github.com/CinemaMod/webdisplays)** — multiblock
  screen walls in Minecraft

## License

This project is released under the [CC0 1.0 Universal](LICENSE) license.
