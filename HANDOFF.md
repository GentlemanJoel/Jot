# Jot — Handoff

**Version 0.5.3**

<<<<<<< HEAD
> This version contains **both** Batch 1 (cosmetics) and Batch 2 (editor
> behavior). The earlier Batch-1-only zip was never pushed, so the repo went
> straight from 0.5.2 to 0.5.3 carrying both.

=======
>>>>>>> a6bbff61723ab3c4a8ec3d5bd781792b94949a98
---

## What changed in this version

<<<<<<< HEAD
### Batch 1 — cosmetics and layout
=======
Batch 1 of the November update set — cosmetics and layout only. No behavior
changes, no new permissions, no new dependencies.
>>>>>>> a6bbff61723ab3c4a8ec3d5bd781792b94949a98

| # | Change | Files |
|---|---|---|
| 1 | Status bar is now solid black, separating the system clock/battery row from the app | `MainActivity.kt`, `themes.xml` |
| 2 | Note title placeholder reads "Title" instead of "Title (optional)" | `JotApp.kt` |
| 3 | Pin panel header shows the pin icon only; the word "PINNED" is gone | `PinnedPanel.kt` |
| 6 | Title and "Start typing..." placeholders are dimmer, using a new `Slate.Placeholder` token | `Theme.kt`, `JotApp.kt` |
<<<<<<< HEAD
| 10 | Settings is now collapsible sections | `SettingsScreen.kt` |
| — | The gold folder icon in the editor menu bar is now a muted outline folder | `JotApp.kt` |

### Batch 2 — editor behavior

| # | Change | Files |
|---|---|---|
| 4 | Opening an existing note goes straight to edit mode, keyboard up, caret a blank line below the existing text | `JotApp.kt` |
| 5 | Action bar: Done is now a checkmark-only button, with a smaller outlined `+` to its left that saves and starts a new note | `JotApp.kt` |
| 8 | New "Reopening the app" setting: start a fresh note when returning after a chosen time away | `Prefs.kt`, `SettingsScreen.kt`, `JotApp.kt` |
| — | New **Read** button in the editor action bar, added to keep read mode reachable (see below) | `JotApp.kt` |

---

## Notes on the implementation

### Status bar
The previous build let the menu bar and pin panel colors bleed up behind the
system icons, which is why the strip was two different greys. A black scrim is
now drawn above everything at the very top, and `themes.xml` sets the window's
status bar color to black so there's no colored flash during launch.

### Caret padding without file churn (item 4)
Opening a note displays its text plus two newlines, with the caret at the end.
Those newlines are **display-only**. `bodyForSave()` compares the live body
against `padFor(padBase)` and, if they still match, saves the original text
instead. Type one character and the comparison fails, so the blank line is
kept as real content. Net effect: viewing a note never rewrites it, so
Syncthing stays quiet and modified-dates don't move.

### Read mode had to stay reachable
Item 4 required existing notes to open in edit mode. Previously `readMode` was
set to true **only** in `openNote`, and nothing else ever set it back — so
flipping that one line would have made `ReadView` permanently unreachable,
silently killing rendered markdown and tap-to-toggle checkboxes.

A **Read** button was added to the editor action bar beside Format. This was
not requested; it was added to avoid deleting a working feature by side effect.
Say the word if you'd rather it go.

### Reopen behavior (item 8)
Implemented as a Settings toggle, **off by default**, with a threshold picker
(1, 2, 5, 15 min or 1 hr; default 2 min). The existing `ON_PAUSE` lifecycle
observer now also records the time, and `ON_RESUME` starts a new note only if
the toggle is on and you were away at least that long. A cold start leaves the
timestamp at zero, so the check can't fire on first launch.

### Settings sections
All start collapsed. Say the word if you'd rather have Pin panel open by
default.

### `Slate.Placeholder`
`#5E6B77`, noticeably dimmer than `Slate.Muted` (`#8FA3B8`). Used only for
empty-field hint text, so labels and secondary copy are untouched.
=======
| 10 | Settings is now five collapsible sections: Pin panel, New note button, Notes folder, GitHub token, Updates | `SettingsScreen.kt` |
| — | The gold folder icon in the editor menu bar is now a muted outline folder, matching the other menu bar icons | `JotApp.kt` |

### Notes on the implementation

- **Status bar.** The previous build let the menu bar and pin panel colors
  bleed up behind the system icons, which is why the strip was two different
  greys. A black scrim is now drawn above everything at the very top, and
  `themes.xml` sets the window's status bar color to black so there's no
  colored flash during launch.
- **Settings sections** all start collapsed. Say the word if you'd rather
  have Pin panel open by default.
- **`Slate.Placeholder`** is `#5E6B77`, noticeably dimmer than `Slate.Muted`
  (`#8FA3B8`). It is used only for empty-field hint text, so label text and
  secondary copy are untouched.
>>>>>>> a6bbff61723ab3c4a8ec3d5bd781792b94949a98

---

## Open backlog

<<<<<<< HEAD
=======
### Batch 2 — behavior (next)

| # | Item | Decision taken |
|---|---|---|
| 4 | Open the keyboard and place the cursor two lines below the last text when an existing note opens | Cursor is positioned visually; the blank lines are **not** written to the `.md` file until you actually type |
| 5 | Small `+` button beside the Done button; Done becomes a checkmark only | Layout to be proposed |
| 8 | Reopening the app lands on a new note | Implemented as a **setting with a time threshold**, not a hard default |

> **Check these two when Batch 2 lands.** See `Jot-Batch2-Testing.md` for
> what the rejected alternatives would have done differently, so you can tell
> whether the chosen behavior is actually the one you want.

>>>>>>> a6bbff61723ab3c4a8ec3d5bd781792b94949a98
### Batch 3 — experimental

| # | Item |
|---|---|
| 9 | Pin panel swipe gestures: swipe right to collapse to a toggle, swipe left for fullscreen, swipe right from fullscreen back to default |

### Batch 4 — restyle

| # | Item |
|---|---|
| 7 | Radix-inspired icon set, replacing Material icons throughout |

### Unscheduled

- Save individual notes and folders as home screen shortcuts
- Voice-to-text button in the editor (Gboard's mic key may already cover this)
- Expanded note formatting: draggable checkboxes, more heading levels, tables,
  global font size and family
- GitHub Actions workflow uses `setup-java@v4` and Node 20 actions, both
  deprecated. Builds still pass.

---

## Known bugs

None currently open.

---

## Key decisions and naming rules

### UI terminology

| Term | Meaning |
|---|---|
| **Status bar** | The Android system strip: time, battery, signal. Not part of the app, but the app colors it. |
| **Menu bar** | The app's top bar: search icon, overflow menu, Jot wordmark, trailing action |
| **Home screen** | The default view: note panel on the left, pin panel on the right |
| **Note panel** | The left panel containing the notes and folders tree |
| **Pin panel** | The right-hand panel holding pinned notes |
| **Pin tile** | A single item inside the pin panel |
| **Editor** | The screen shown when a note is open |
| **Format bar** | The scrolling row of markdown buttons above the action bar |
<<<<<<< HEAD
| **Action bar** | The bottom strip of the editor: Format, Read, version, `+`, checkmark |
=======
| **Action bar** | The bottom strip of the editor: Format, version number, Done |
>>>>>>> a6bbff61723ab3c4a8ec3d5bd781792b94949a98

### Standing rules

- Notes are plain `.md` files in a normal folder, synced to the PC by
  Syncthing. **Any feature that would require non-markdown data in the file is
  rejected** — this rules out per-word font colors and per-note font families.
<<<<<<< HEAD
- **Viewing a note must never modify it.** Any feature that adds text to the
  editor on open has to strip it again on save unless the user typed.
=======
>>>>>>> a6bbff61723ab3c4a8ec3d5bd781792b94949a98
- Styling follows the Sagan Slate palette. Color tokens live in `Theme.kt`;
  never hardcode a hex value in a composable.
- The version number is shown in the editor action bar and at the bottom of
  Settings, read from `BuildConfig.VERSION_NAME`.
- `build.gradle.kts` version fields are bumped by RepoMate at push time.
  Delivered zips must not contain `build.gradle.kts`, or the version
  double-bumps.
- Releases are built by GitHub Actions using the shared signing keystore at
  `C:\Users\Joel\.androidkeys\shared-release.jks`.

### Update workflow

Extract the zip over the project folder choosing **Replace files in
destination**. Choosing "Keep both" creates `_1` duplicate files, which breaks
the build with a duplicate resource error. This cost three failed builds in
November 2026.
