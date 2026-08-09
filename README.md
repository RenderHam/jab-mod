# Just A Browser Mod

# !! WARNING THIS PROJECT IS STILL EXPERIMENTAL !!

**Just A Browser Mod** (JAB) adds fully functional web screens to Minecraft. Build a wall of
screen blocks, point at it and run a command, and the wall turns into a live browser panel
backed by an embedded Chromium instance (via [Rinku](https://github.com/Keksuccino/Rinku)).

## Features

- **Multiblock screen walls** — any rectangle of screen blocks (2x2 or larger) becomes a
  display, on any face of any side of the blocks (walls, floors, ceilings).
- **Real browser rendering** — pages render onto the blocks in-game with texture quality
  tied to the GUI scale.
- **Interactive browser view** — right-click a screen to open the browser GUI with a URL
  bar (`Ctrl+L` to focus) and full mouse and keyboard forwarding to the page. In-page
  navigation (links, back/forward) works natively and stays in sync with the wall.
- **Audio modes** — screens can be set to _global_ (page audio plays normally) or
  _dynamic_ (page volume is driven by your distance from the wall, with a 64-block falloff).
- **Per-face displays** — one wall can show a different page on each of its six faces.

## Requirements

- Minecraft **1.21.11** (Fabric)
- [Fabric API](https://modrinth.com/mod/fabric-api)
- [Rinku](https://github.com/Keksuccino/Rinku) (rinku-fabric, `3.0.1-1.21.11`)

> Use the **patched Rinku jar from the release** — the published 3.0.1 jar crashes at
> startup (missing mixin refmap entry; upstream [Keksuccino/Rinku#11](https://github.com/Keksuccino/Rinku/issues/11)).

> On first launch Rinku downloads its Chromium native binaries — this may take a few minutes.

## Usage

1. Craft screen blocks (1 iron ingot + 1 redstone, shapeless) and build a flat wall.
2. Look at the wall and run:

```
/jab create                     # create a display on the face you're looking at
/jab create https://youtube.com # create and load a URL immediately
/jab url <url>                  # change the page on the wall you're looking at
/jab audio global|dynamic       # switch audio mode for the wall
/jab remove                     # remove the display
/jab debug                      # print server-side wall info for debugging
```

3. Right-click the wall to open the interactive browser view.

Breaking any block of a wall also removes its display, so walls can't get stuck in a
half-broken state.

## Configuration

A `config/jab.properties` file is generated on first run:

| Key                               | Default                  | Description                               |
| --------------------------------- | ------------------------ | ----------------------------------------- |
| `maxScreenSize`                   | `8`                      | Maximum wall dimension in blocks          |
| `defaultResolutionX/Y`            | `1920x1080`              | Browser render resolution for new screens (fixed at creation) |
| `defaultUrl`                      | `https://www.google.com` | Page loaded when a screen is created      |
| `loadDistance` / `unloadDistance` | `32` / `48`              | Browser lifecycle distance from the wall  |
| `maxBrowsers`                     | `16`                     | Concurrent browser cap (extra screens park) |

## Building

```
./gradlew build
```

The built jar lands in `build/libs/`. Drop it (plus the requirements above) into your
`mods/` folder.

## Credits

This mod is heavily inspired by (and builds on) the work of others:

- **[Rinku by Keksuccino](https://github.com/Keksuccino/Rinku)** — the embedded Chromium
  framework (successor of Keksuccino's MCEF fork) that powers the browser rendering
- **[BrowserMod by Mcjunky33](https://github.com/Mcjunky33/BrowserMod)** — the original
  in-game browser concept
- **[WebDisplays by CinemaMod](https://github.com/CinemaMod/webdisplays)** — multiblock
  screen walls in Minecraft

## License

This project is released under the [CC0 1.0 Universal](LICENSE) license.
