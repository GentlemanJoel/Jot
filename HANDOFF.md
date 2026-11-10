# Jot — Handoff

**Released: 0.5.7 · This package (Session 2) becomes 0.5.8** — RepoMate bumps
the version on push, so `build.gradle.kts` is deliberately not in the zip.

---

## What changed in this version — Session 2 (data safety)

### Nothing is hard-deleted any more

| Change | Detail |
|---|---|
| **Trash** | Deleting moves the file to `Jot/.trash` instead of unlinking it. Reachable from Settings → TRASH. Restore puts a note back where it came from; if something now holds that name, the restored copy gets a numbered suffix rather than overwriting. Purged automatically after `Prefs.TRASH_KEEP_DAYS` (30) on launch. |
| **Lock** | Long-press a note → *Lock (prevent delete)*, or the editor's `...` menu. A locked note shows a padlock in the tree and cannot be deleted in-app; a folder holding one cannot be deleted either. App state only, so deleting on the PC still works. |
| **Delete guards** | The folder delete dialog now says how many notes go with it, blocks outright if anything inside is locked, and warns in red if it is your designated new-note folder. |
| **Mass delete** | Long-press → *Select* turns the tree into tick boxes and the menu bar into a selection bar (count, move, delete, cancel). Locked items in a selection are reported and skipped rather than silently deleted. |
| **Move to...** | On notes and folders, from the long-press menu, the editor menu, or the selection bar. A folder cannot be moved into itself, and children of a selected folder are dropped from the batch so nothing acts on a path that already moved. |
| **Delete from the editor** | New `...` menu beside the pin icon: Move to..., Lock/Unlock, Delete note. |
| **Default folder for new notes** | Settings → NEW NOTES. Deleting that folder warns first, and clears the setting rather than leaving it dangling. |

Pins, locks, and the default-folder setting now follow a note through renames
and moves, including notes nested inside a moved folder (`Prefs.repath`), and
are cleared when something is binned (`Prefs.forgetPath`). `Prefs.prune` also
drops locks whose file vanished (deleted on the PC, say).

### Carried in from your test notes

| Item | Detail |
|---|---|
| Caret auto-scroll | Opening a long note now scrolls down to the caret. The caret is placed **after** the field has focus; setting it at the same time as the text is why it stayed off-screen. |
| Pin panel capped | Widest option is now 170dp. The three wider ones are gone and the default moved to Narrow (170dp). A saved value pointing at a removed option falls back to the default. |
| Format icon | The word "Format" is now the `TextFormat` icon (the "A" with a bar, as in Google Keep), blue while the format bar is open. |

### Refactor worth knowing about

`DrawerTree.kt` now exposes a **`TreeActions`** holder instead of passing ten
callbacks positionally down a recursive tree. `JotApp` builds one and hands it
to both the home tree and the slide-out tree, so the two can no longer drift
apart. `TreeSelection` carries the ticked paths; null means normal browsing.

### Not compiled here
There is no Android SDK in the sandbox. Checked statically: brace balance on
every file, every `Icons.*` reference against the real Material set, every
composable's call site against its declaration, and import completeness.

---

## Open backlog — session plan

Run **strictly one session at a time**, testing each release on the phone
before starting the next. Sessions all edit `JotApp.kt`; running them in
parallel caused the conflict-marker build failures of November 2026.

| Session | Model | Items | Status |
|---|---|---|---|
| S1 Fix pass | Sonnet | keyboard bug, pin header alignment, pin long-press menu, update button, drawer border, drawer IME, action bar height | **Delivered (this package)** |
| S2 Data safety | **Opus** | Trash, locks, Move to..., mass delete, delete from the editor, folder guards, default new-note folder, caret auto-scroll, 170dp cap, format icon | **Delivered (this package)** |
| S3 Menu bar and editor | Sonnet | Fixed menu bar layout, drop Read button (auto read view when the keyboard closes, behind a setting), list auto-continue on open, undo/redo, pin panel beside a new note | Next |
| S4 Settings and launch | Sonnet | Where the app lands on open, auto-keyboard toggle, **pin panel designer** (width slider with detents, icons/text/both, label beside or below, live preview, Apply), **new-note button live preview**, **tap the pin panel header icon to jump to its settings** | Queued |
| S5 Customization | Sonnet | Folder icons and colors with "apply to subfolders", larger icon library with tag search, **one combined "Style" dialog replacing separate icon and color pickers** (shared by pins and folders) | Queued |
| S6 Images | **Opus** | Attach/paste images (rated **High**; needs go-ahead; after S2) | Queued |
| S7 Gestures | **Opus** | Pin panel swipe: collapse, fullscreen, back to default | Queued |
| S8a Theme refactor | **Opus** | Replace the fixed `Slate` object with role colors (note panel, pin panel, editor, menu bar). Invisible change; verify nothing moved. | Queued |
| S8b Restyle and themes | **Opus** | Radix-inspired styling plus selectable color themes, including a lighter one that distinguishes each section | Needs spec |

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
