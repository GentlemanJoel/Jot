# Jot

Minimal-tap Android notes. Opens straight into a blank note, saves plain `.md` files to a normal folder so Syncthing and your PC can read them.

**Current version: 0.1.0**

## What it does

- Opens to a new, empty note with the cursor already in the body
- Quick Settings tile + side-key double-press launch from the lock screen
- Title field is optional and can be filled at any point — it becomes the filename
- Leave a note blank and navigate away, it's discarded
- Right-side drawer: pinned search + unlimited-depth folder tree
- Markdown off by default; toggle reveals a formatting bar and a preview
- In-app update check against GitHub Releases

## Storage

Notes live in `/storage/emulated/0/Jot` as plain `.md` files. Folders are folders. Nothing proprietary — point Syncthing at it and open the same folder in Obsidian on your PC.

## Setup

See [SETUP.md](SETUP.md).

## Releasing a new version

1. Bump `jotVersionName` **and** `jotVersionCode` in `app/build.gradle.kts`
2. Commit and push to `main`
3. GitHub Actions builds, signs, and publishes the release
4. Tap **⋮ → Check for updates** in the app

## Stack

Kotlin · Jetpack Compose · Material 3 · no database · no third-party dependencies
