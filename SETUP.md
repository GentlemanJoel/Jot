# Jot — Setup Checklist

Work top to bottom. Each box is one action. Nothing here needs Android Studio.

---

## Phase 1 — Repo

- [ ] **1.1** Go to https://github.com/new
- [ ] **1.2** Repository name: `Jot` (exactly — the updater reads `GentlemanJoel/Jot`)
- [ ] **1.3** Visibility: **Public** is simpler. Private works too, but then the in-app updater needs a token (Phase 6).
- [ ] **1.4** Do **not** add a README, .gitignore, or license. Create empty.
- [ ] **1.5** On your PC, unzip `Jot.zip` to `C:\Users\Joel\Documents\Jot`
- [ ] **1.6** Open PowerShell in that folder and run:

```powershell
cd C:\Users\Joel\Documents\Jot
git init
git add .
git commit -m "Jot v0.1.0"
git branch -M main
git remote add origin https://github.com/GentlemanJoel/Jot.git
git push -u origin main
```

> The first push will trigger a build **and it will fail on purpose** — the keystore secret isn't there yet. That's expected. Phase 2 fixes it.

---

## Phase 2 — Signing keystore (do this once, never again)

> **This is the one step you cannot redo later.** If this keystore is ever lost, every future update will refuse to install and you'll have to uninstall Jot (losing nothing — your notes are plain files — but it's annoying).

- [ ] **2.1** Install a JDK if you don't have one: https://adoptium.net/temurin/releases/?version=17 → Windows x64 **.msi**
- [ ] **2.2** During install, enable **"Set JAVA_HOME variable"** and **"Add to PATH"**
- [ ] **2.3** Close and reopen PowerShell, then verify:

```powershell
keytool -help
```

- [ ] **2.4** Generate the key (pick one password and use it for both prompts):

```powershell
cd C:\Users\Joel\Documents
keytool -genkeypair -v -keystore jot.jks -keyalg RSA -keysize 2048 -validity 10000 -alias jot
```

- [ ] **2.5** **Back up `jot.jks`** somewhere permanent — a second drive, a password manager vault, anywhere that isn't just this PC. Write the password down with it.
- [ ] **2.6** Copy the base64 of the keystore to your clipboard:

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("C:\Users\Joel\Documents\jot.jks")) | Set-Clipboard
```

---

## Phase 3 — GitHub secrets

Go to: `https://github.com/GentlemanJoel/Jot/settings/secrets/actions` → **New repository secret** for each.

| Secret name | Value |
|---|---|
| `KEYSTORE_B64` | paste from clipboard (step 2.6) |
| `KEYSTORE_PASSWORD` | the password from step 2.4 |
| `KEY_ALIAS` | `jot` |
| `KEY_PASSWORD` | same password as 2.4 |

- [ ] **3.1** All four secrets added
- [ ] **3.2** Go to the **Actions** tab → open the failed run → **Re-run all jobs**
- [ ] **3.3** Wait ~4 minutes. Green check = success.

---

## Phase 4 — Install on the S26 Ultra

- [ ] **4.1** On the phone, open `https://github.com/GentlemanJoel/Jot/releases/latest`
- [ ] **4.2** Under **Assets**, tap `Jot-v0.1.0.apk`
- [ ] **4.3** Chrome/Samsung Internet will warn about unknown sources → **Settings** → enable **Allow from this source**
- [ ] **4.4** Tap the downloaded file → **Install**
- [ ] **4.5** Open Jot → tap **Grant file access** → toggle **Allow access to manage all files** → back out → tap **I've granted it — continue**
- [ ] **4.6** Type something, back out, reopen — confirm the note is in the drawer (tap the ☰ icon, top right)

---

## Phase 5 — One-button launch

**Quick Settings tile (works from the lock screen):**

- [ ] **5.1** Swipe down twice from the top → tap the **✏️ / pencil** edit icon
- [ ] **5.2** Find **New Jot** in the available tiles
- [ ] **5.3** Drag it into the **first position** of the top row
- [ ] **5.4** Done → lock the phone → swipe down → tap **New Jot** → confirm it opens to a blank note

**Side key (true one-button):**

- [ ] **5.5** Settings → **Advanced features** → **Side button** (may read "Side key")
- [ ] **5.6** Under **Double press**, choose **Open app** → tap the gear → select **Jot**
- [ ] **5.7** Test it: double-press the side button from the lock screen

---

## Phase 6 — Syncthing (phone ↔ PC)

- [ ] **6.1** PC: download Syncthing → https://syncthing.net/downloads/ → **Windows (64-bit)**, or the `SyncTrayzor` installer for a tray app
- [ ] **6.2** Phone: install **Syncthing-Fork** → https://f-droid.org/packages/com.github.catfriend1.syncthingandroid/
- [ ] **6.3** On the phone, add the folder `/storage/emulated/0/Jot` with folder ID `jot-notes`
- [ ] **6.4** On the PC, open http://127.0.0.1:8384 → **Add Remote Device** → scan/paste the phone's device ID
- [ ] **6.5** Accept the folder share on the PC → point it at `C:\Users\Joel\Documents\JotNotes`
- [ ] **6.6** Create a note on the phone → confirm the `.md` file appears on the PC within a minute

**Bonus:** open `C:\Users\Joel\Documents\JotNotes` as an Obsidian vault and you get a full desktop editor for free.

---

## Phase 7 — Private repo only (skip if public)

- [ ] **7.1** https://github.com/settings/personal-access-tokens/new
- [ ] **7.2** Repository access → **Only select repositories** → `Jot`
- [ ] **7.3** Permissions → **Contents: Read-only**
- [ ] **7.4** Generate, copy the token
- [ ] **7.5** In Jot: **⋮ → About & settings** → paste into the GitHub token field → **Save**

---

## Updating Jot later

1. Claude pushes new code to the repo
2. GitHub Actions builds and publishes a release automatically (~4 min)
3. On the phone: **⋮ → Check for updates** → **Download & install**

No cable, no re-signing, no reinstall.
