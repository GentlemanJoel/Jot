# Jot — Handoff

**Released: 0.5.6 · This package (Session 1) becomes 0.5.7** — RepoMate bumps
the version on push, so `build.gradle.kts` is deliberately not in the zip.

---

## What changed in this version — Session 1 (fix pass)

| # | Change | Files |
|---|---|---|
| 17 | **Keyboard now raised explicitly** when a note opens, a new note starts, or Edit is tapped. Focusing the field alone did not reliably raise it. The helper `raiseKeyboard()` asks for focus, then asks for the keyboard, and retries twice. | `JotApp.kt` |
| 11 | **Pin panel header is a fixed 56dp**, equal to the menu bar's content row, at every width. The divider no longer rises when the panel narrows. | `PinnedPanel.kt` |
| 1 | **Pencil/edit icon removed from the pin panel.** The header is only the pin icon. Press and hold a pin for its menu: Change icon, Change color, Move up, Move down, Unpin. The old edit mode and its dialog are gone. | `PinnedPanel.kt` |
| 12 | **Updates section removed from Settings.** A small "Check for updates" button now sits beside the version number at the bottom. | `SettingsScreen.kt` |
| 18 | **Slide-out note panel has a 1.5dp border** on its right edge. | `JotApp.kt` |
| 19 | **Slide-out note panel stays scrollable with the keyboard open** (`imePadding`). | `JotApp.kt` |
| 21 | **Action bar is shorter**: vertical padding 8dp to 4dp, checkmark 50dp to 44dp, `+` 38dp to 40dp. Bar is about 14dp shorter, tap targets stay at 40dp or more. | `JotApp.kt` |

### Not done in Session 1, on purpose
- The setting to turn auto-keyboard on or off is **Session 4**.
- Nothing was compiled here (no Android SDK in the sandbox). Brace balance and
  imports were checked statically. If the build fails, the error log names the
  file and line.

### Verify on the phone
1. Open an existing note from Home: keyboard comes up, caret one blank line down.
2. Open one from the pin panel and from the slide-out panel: same.
3. Narrow the pin panel in Settings: divider still meets the bottom of the menu bar.
4. Press and hold a pin: menu appears; each entry works.
5. In a note, open the slide-out panel, raise the keyboard, scroll to the bottom of the list.
6. Settings bottom: version with a small update button beside it; no Updates section.

---

## Previously shipped (0.5.3 to 0.5.6)

- Solid black status bar; "Title" placeholder; dimmer placeholders (`Slate.Placeholder` `#5E6B77`).
- Collapsible Settings sections, all collapsed by default.
- Opening an existing note: edit mode, caret a blank line below the text. The
  padding is **display-only** (`padFor` / `bodyForSave`), so viewing a note
  never rewrites it.
- Action bar: Format, Read, version, small `+` (saves and starts a new note), checkmark.
- Setting "Reopening the app": new note after N minutes away (off by default).
- Muted outline folder icon in the editor menu bar.

---

## Open backlog — session plan

Run **strictly one session at a time**, testing each release on the phone
before starting the next. Sessions all edit `JotApp.kt`; running them in
parallel caused the conflict-marker build failures of November 2026.

| Session | Model | Items | Status |
|---|---|---|---|
| S1 Fix pass | Sonnet | keyboard bug, pin header alignment, pin long-press menu, update button, drawer border, drawer IME, action bar height | **Delivered (this package)** |
| S2 Data safety | **Opus** | Trash bin (`.trash`, ~30-day purge), lock a note from deletion, **Move to...** menu, mass delete (multi-select), delete from inside a note, folder-delete guards, designated folder for new notes | Next |
| S3 Menu bar and editor | Sonnet | Fixed menu bar layout, drop Read button (auto read view when the keyboard closes, behind a setting), list auto-continue on open, undo/redo, pin panel beside a new note | Queued |
| S4 Settings and launch | Sonnet | Where the app lands on open (new note / Home / a saved note), auto-keyboard toggle, merge reopen settings into one section, pin-panel-on-new-note toggle (default on) | Queued |
| S5 Customization | Sonnet | Folder icons and colors with "apply to subfolders", larger icon library with tag search | Queued |
| S6 Images | **Opus** | Attach/paste images (rated **High**; needs go-ahead; after S2) | Queued |
| S7 Gestures | **Opus** | Pin panel swipe: collapse, fullscreen, back to default | Queued |
| S8 Restyle | **Opus** | Radix-inspired styling, last | Needs spec |

### Design decisions already made (user approved defaults pending)
- **Menu bar:** settings, folder, search on the left; logo centered; one contextual slot on the right (new folder on Home, note menu in the editor). On Home the folder icon collapses/expands all folders.
- **Editor note menu (one icon):** pin, lock, move, delete.
- **Pin panel on new notes:** shown on blank new notes, hidden on existing ones.
- **Lock screen shortcut and Quick Settings tile always open a new note**, regardless of the "app lands on" setting.
- **Trash only, no archive** (an archive is just a folder once Move to... exists).
- **Keep the folder icon in the editor**: left-edge swipe collides with Android's back gesture.
- **Radix icons only for the menu bar and action bar** (the set has about 320 icons); Material icons stay for the picker library.

### Unanswered questions
1. First line of the Nov 10 request, "numbered lists list bullet points": default reading is that numbered lists should auto-continue like bullet lists. Enter-continuation already exists in `continueListOnNewline`; the new item is continuing a list when a note is opened.
2. Radix scope: which of colors, corner radii, spacing, icons to carry over.
3. Whether rearranging notes also needs drag-and-drop (default: Move to... menu only).

### Unscheduled
- Home screen shortcuts for notes and folders
- Voice-to-text (try Gboard's mic key first)
- Expanded formatting: draggable checkboxes, more heading levels, tables, global font size and family
- Walkthrough: lock screen shortcut / edge swipe launch
- GitHub workflow uses `setup-java@v4` and Node 20 actions (deprecated, builds still pass)

---

## Known bugs

None open after Session 1, pending phone verification.

---

## Key decisions and naming rules

### UI terminology

| Term | Meaning |
|---|---|
| **Status bar** | The Android system strip: time, battery, signal |
| **Menu bar** | The app's top bar: icons and the Jot wordmark |
| **Home screen** | Note panel on the left, pin panel on the right |
| **Note panel** | The left panel containing the notes and folders tree |
| **Pin panel** | The right-hand panel of pinned notes |
| **Pin tile** | A single item in the pin panel |
| **Editor** | The screen shown when a note is open |
| **Format bar** | The scrolling row of markdown buttons above the action bar |
| **Action bar** | The bottom strip of the editor: Format, Read, version, `+`, checkmark |
| **Slide-out panel** | The note panel that slides over the editor |

### Standing rules

- Notes are plain `.md` files in a normal folder, synced by Syncthing. Anything
  that would put non-markdown data into the file is rejected. Per-folder or
  per-note app data (icons, colors, locks) lives in app preferences keyed by path.
- **Viewing a note must never modify it.** Anything added to the editor on open
  must be stripped on save unless the user typed.
- Sagan Slate palette; color tokens live in `Theme.kt`, never hardcode hex in a composable.
- Keep tap targets at 44dp or more where practical (user has limited hand mobility).
- Version shown in the editor action bar and at the bottom of Settings.
- `build.gradle.kts` is bumped by RepoMate at push time. Delivered update zips
  must not contain it.
- Releases are built by GitHub Actions with the shared keystore at
  `C:\Users\Joel\.androidkeys\shared-release.jks`.
- `.gitignore` blocks `*.zip`.

### Update workflow (PC or laptop)

1. Close RepoMate.
2. Extract the zip over `C:\Users\Joel\Documents\Jot` with PowerShell, which
   overwrites without creating `_1` duplicates:
   `Expand-Archive -Path "<zip path>" -DestinationPath "C:\Users\Joel\Documents\Jot" -Force`
3. Check for strays (each should print nothing):
   - `Get-ChildItem . -Recurse -File -Filter "*_1.*"`
   - `Get-ChildItem . -Recurse -File | Select-String -Pattern "^<<<<<<<|^>>>>>>>"`
4. `git pull` in the Jot folder (always, especially after using the other machine).
5. RepoMate: Push update, Patch.
6. If the build fails, copy the failing-build section from RepoMate's OUTPUT pane.
