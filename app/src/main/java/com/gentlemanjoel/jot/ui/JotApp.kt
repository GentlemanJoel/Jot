package com.gentlemanjoel.jot.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.TextFormat
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.gentlemanjoel.jot.BuildConfig
import com.gentlemanjoel.jot.Draft
import com.gentlemanjoel.jot.FabSize
import com.gentlemanjoel.jot.NoteNode
import com.gentlemanjoel.jot.NoteStore
import com.gentlemanjoel.jot.Pin
import com.gentlemanjoel.jot.PanelWidth
import com.gentlemanjoel.jot.Prefs
import com.gentlemanjoel.jot.TrashItem
import com.gentlemanjoel.jot.UpdateInfo
import com.gentlemanjoel.jot.Updater
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

private enum class Screen { HOME, EDITOR, SETTINGS, TRASH }

/**
 * Opening a note drops the caret a blank line below whatever is already
 * there, so you can just start typing. The two newlines are display-only
 * until you actually type something — see [JotApp]'s bodyForSave — otherwise
 * merely glancing at a note would rewrite it and Syncthing would carry that
 * pointless edit to the PC.
 */
/**
 * Drops any node that already sits inside another node in the same list.
 * Moving or binning a folder takes its children with it, so acting on the
 * child afterwards would be operating on a path that no longer exists.
 */
private fun topLevelOf(nodes: List<NoteNode>): List<NoteNode> {
    val folderPaths = nodes.filter { it.isFolder }.map { it.file.absolutePath }
    return nodes.filterNot { n ->
        folderPaths.any { p -> n.file.absolutePath != p && n.file.absolutePath.startsWith("$p/") }
    }
}

private fun padFor(text: String): String {
    val base = text.trimEnd('\n')
    return if (base.isEmpty()) "" else base + "\n\n"
}

@Composable
fun JotApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val bodyFocus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val hostView = LocalView.current

    // Focusing the body field does not reliably raise the keyboard on its
    // own, especially right after a screen swap. Ask for focus, then ask for
    // the keyboard explicitly, and repeat once or twice in case the field
    // hadn't finished composing yet. Showing an already-open keyboard is a no-op.
    fun raiseKeyboard(onReady: () -> Unit = {}) {
        scope.launch {
            delay(90)
            repeat(3) {
                runCatching { bodyFocus.requestFocus() }
                keyboard?.show()
                runCatching {
                    ViewCompat.getWindowInsetsController(hostView)
                        ?.show(WindowInsetsCompat.Type.ime())
                }
                delay(130)
            }
            onReady()
        }
    }

    var screen by remember { mutableStateOf(Screen.EDITOR) }
    var tree by remember { mutableStateOf(NoteStore.loadTree()) }
    val expanded = remember { mutableStateListOf<String>() }
    var pins by remember { mutableStateOf(Prefs.pins(context)) }
    var panelWidth by remember { mutableStateOf(Prefs.panelWidth(context)) }
    var fabSize by remember { mutableStateOf(Prefs.fabSize(context)) }
    var searching by remember { mutableStateOf(false) }
    var resumeNew by remember { mutableStateOf(Prefs.resumeNew(context)) }
    var resumeMinutes by remember { mutableStateOf(Prefs.resumeMinutes(context)) }
    var lockedPaths by remember { mutableStateOf(Prefs.lockedPaths(context)) }
    var defaultDir by remember { mutableStateOf(Prefs.defaultDir(context)) }

    // Multi-select. Null means normal browsing; a set means the tree is
    // showing tick boxes and the menu bar has turned into a selection bar.
    var selected by remember { mutableStateOf<Set<String>?>(null) }
    var trash by remember { mutableStateOf<List<TrashItem>>(emptyList()) }

    var currentFile by remember { mutableStateOf<File?>(null) }
    var currentDir by remember { mutableStateOf(NoteStore.ensureRoot()) }
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf(TextFieldValue("")) }
    var readMode by remember { mutableStateOf(false) }
    var showToolbar by remember { mutableStateOf(false) }

    // The note's text exactly as it came off disk, whenever the caret
    // padding is still untouched. Null once there is nothing to strip.
    var padBase by remember { mutableStateOf<String?>(null) }

    var overflow by remember { mutableStateOf(false) }
    var noteMenu by remember { mutableStateOf(false) }
    var dialog by remember { mutableStateOf<DialogState?>(null) }
    var status by remember { mutableStateOf<String?>(null) }
    var update by remember { mutableStateOf<UpdateInfo?>(null) }
    var progress by remember { mutableStateOf(-1) }

    // If the body is still exactly "file contents + caret padding", save the
    // file contents. Type one character and this falls through to body.text,
    // which keeps the blank line the caret was sitting below.
    fun bodyForSave(): String {
        val base = padBase
        return if (base != null && body.text == padFor(base)) base else body.text
    }

    fun draft() = Draft(currentFile, currentDir, title, bodyForSave())

    fun refresh() {
        tree = NoteStore.loadTree()
        Prefs.prune(context)
        pins = Prefs.pins(context)
        lockedPaths = Prefs.lockedPaths(context)
        defaultDir = Prefs.defaultDir(context)
    }

    /** Where a new note goes when no folder was picked. */
    fun newNoteDir(): File {
        val configured = defaultDir?.let(::File)
        return if (configured != null && configured.isDirectory) configured else NoteStore.ensureRoot()
    }

    fun persist() {
        val before = currentFile
        val saved = NoteStore.save(draft())
        // Keep a pin attached across a rename triggered by a title change.
        if (before != null && saved != null && before.absolutePath != saved.absolutePath) {
            Prefs.repath(context, before.absolutePath, saved.absolutePath)
        }
        currentFile = saved
        refresh()
    }

    fun newNote(dir: File = newNoteDir()) {
        persist()
        currentFile = null
        currentDir = dir
        title = ""
        body = TextFieldValue("")
        padBase = null
        readMode = false
        showToolbar = false
        screen = Screen.EDITOR
        scope.launch { drawerState.close() }
        raiseKeyboard()
    }

    fun openNote(file: File) {
        persist()
        val d = NoteStore.read(file)
        currentFile = d.file
        currentDir = d.dir
        title = d.title
        // Caret lands a blank line under the last of the existing text,
        // keyboard up, ready to carry on writing.
        padBase = d.body
        val padded = padFor(d.body)
        // Caret at 0 for now. Moving it only after the field has focus is
        // what makes the editor scroll down to it; setting it at the same
        // time as the text leaves the caret off-screen on a long note.
        body = TextFieldValue(padded)
        readMode = false
        showToolbar = false
        screen = Screen.EDITOR
        scope.launch { drawerState.close() }
        raiseKeyboard {
            body = body.copy(selection = TextRange(body.text.length))
        }
    }

    // ---------------- deleting, moving, locking ----------------

    fun isLocked(f: File): Boolean = lockedPaths.contains(f.absolutePath)

    fun toggleLock(f: File) {
        val now = !isLocked(f)
        Prefs.setLocked(context, f.absolutePath, now)
        lockedPaths = Prefs.lockedPaths(context)
        status = if (now) "Locked. It can't be deleted until you unlock it." else "Unlocked."
    }

    /** Bins one node, clearing the editor if it was the open note. */
    fun trashNode(node: NoteNode): Boolean {
        val path = node.file.absolutePath
        val moved = NoteStore.moveToTrash(node.file)
        if (moved) {
            Prefs.forgetPath(context, path)
            if (currentFile?.absolutePath == path ||
                currentFile?.absolutePath?.startsWith("$path/") == true
            ) {
                currentFile = null
                title = ""
                body = TextFieldValue("")
                padBase = null
            }
        }
        return moved
    }

    fun moveNode(node: NoteNode, dest: File): Boolean {
        val old = node.file.absolutePath
        val moved = NoteStore.move(node, dest) ?: return false
        Prefs.repath(context, old, moved.absolutePath)
        if (currentFile?.absolutePath == old) {
            currentFile = moved
            currentDir = moved.parentFile ?: NoteStore.ensureRoot()
        }
        refresh()
        return true
    }

    fun goHome() {
        persist()
        screen = Screen.HOME
    }

    // Cold start: straight into a blank note with the caret in the body.
    LaunchedEffect(Unit) {
        NoteStore.ensureRoot()
        NoteStore.purgeTrash(Prefs.TRASH_KEEP_DAYS)
        refresh()
        raiseKeyboard()
    }

    // Debounced autosave while typing.
    LaunchedEffect(Unit) {
        snapshotFlow { title to body.text }.collect {
            delay(900)
            if (!draft().isBlank) persist()
        }
    }

    // One set of row actions shared by the home tree and the slide-out tree,
    // so the two can never drift apart.
    val treeActions = TreeActions(
        onToggleFolder = { path ->
            if (expanded.contains(path)) expanded.remove(path) else expanded.add(path)
        },
        onOpenNote = { openNote(it) },
        onNewNoteIn = { newNote(it) },
        onNewFolderIn = { dialog = DialogState.NewFolder(it) },
        onRename = { dialog = DialogState.Rename(it) },
        onDelete = { dialog = DialogState.Delete(it) },
        onMove = { dialog = DialogState.MoveTo(setOf(it.file.absolutePath)) },
        onToggleLock = { toggleLock(it.file) },
        onStartSelection = { selected = setOf(it.file.absolutePath) },
        onToggleSelected = { node ->
            val path = node.file.absolutePath
            val now = selected.orEmpty().toMutableSet()
            if (!now.remove(path)) now.add(path)
            // Dropping the last tick leaves selection mode entirely.
            selected = if (now.isEmpty()) null else now
        },
        isLocked = { lockedPaths.contains(it.file.absolutePath) },
    )

    // Save on backgrounding, and optionally hand back a fresh note if the
    // app has been out of sight long enough that you've moved on.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, resumeNew, resumeMinutes) {
        var leftAt = 0L
        val obs = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    persist()
                    leftAt = System.currentTimeMillis()
                }
                Lifecycle.Event.ON_RESUME -> {
                    // leftAt stays 0 on a cold start, which already opens a
                    // blank note, so this only fires on a real return.
                    val away = System.currentTimeMillis() - leftAt
                    if (resumeNew && leftAt > 0L && away >= resumeMinutes * 60_000L) {
                        searching = false
                        newNote()
                    }
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }

    fun checkForUpdate() {
        status = "Checking for updates..."
        scope.launch {
            Updater.check(context)
                .onSuccess { info ->
                    if (info == null) {
                        status = "You're on the latest version."
                        update = null
                    } else {
                        status = null
                        update = info
                    }
                }
                .onFailure { status = it.message ?: "Update check failed." }
        }
    }

    // Back button: leave search, close the drawer, else fall back toward Home.
    BackHandler(
        enabled = drawerState.isOpen || searching || selected != null || screen != Screen.HOME
    ) {
        when {
            drawerState.isOpen -> scope.launch { drawerState.close() }
            selected != null -> selected = null
            searching -> searching = false
            screen == Screen.EDITOR -> goHome()
            screen == Screen.SETTINGS -> screen = Screen.HOME
            screen == Screen.TRASH -> screen = Screen.SETTINGS
            else -> Unit
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        // Home already shows the tree, so the drawer is an editor-only shortcut.
        gesturesEnabled = screen == Screen.EDITOR,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Slate.Secondary,
                modifier = Modifier.width(300.dp),
            ) {
                // imePadding shrinks the list to the space above the keyboard,
                // so the bottom of the tree stays scrollable while typing.
                Box(Modifier.fillMaxSize().imePadding()) {
                DrawerTree(
                    tree = tree,
                    openFile = currentFile,
                    expanded = expanded.toSet(),
                    actions = treeActions,
                )

                // A clear edge so the panel reads as sitting on top of the screen.
                Box(
                    Modifier
                        .align(Alignment.CenterEnd)
                        .width(1.5.dp)
                        .fillMaxHeight()
                        .background(Slate.Muted.copy(alpha = 0.55f))
                )
                }
            }
        },
    ) {
        when (screen) {
            Screen.HOME -> HomeScreen(
                pins = pins,
                panelWidth = panelWidth,
                fabSize = fabSize,
                tree = tree,
                openFile = currentFile,
                expanded = expanded.toSet(),
                searching = searching,
                actions = treeActions,
                selected = selected,
                onClearSelection = { selected = null },
                onSelectDelete = { dialog = DialogState.DeleteMany(selected.orEmpty()) },
                onSelectMove = { dialog = DialogState.MoveTo(selected.orEmpty()) },
                onOpenSearch = { searching = true },
                onCloseSearch = { searching = false },
                onOverflow = { overflow = it },
                overflowOpen = overflow,
                onSettings = {
                    trash = NoteStore.trashItems()
                    screen = Screen.SETTINGS
                },
                onCheckUpdate = { checkForUpdate() },
                onNewNote = { searching = false; newNote() },
                onOpenPin = { path -> openNote(File(path)) },
                onMovePin = { p, d -> Prefs.movePin(context, p, d); pins = Prefs.pins(context) },
                onSetIcon = { p, i -> Prefs.updatePin(context, p, icon = i); pins = Prefs.pins(context) },
                onSetColor = { p, c -> Prefs.updatePin(context, p, color = c); pins = Prefs.pins(context) },
                onUnpin = { p -> Prefs.unpin(context, p); pins = Prefs.pins(context) },
            )

            Screen.TRASH -> TrashScreen(
                items = trash,
                onBack = { screen = Screen.SETTINGS },
                onRestore = { item ->
                    val back = NoteStore.restoreFromTrash(item)
                    status = if (back != null) "Restored to ${NoteStore.labelFor(back.parentFile ?: NoteStore.root)}."
                    else "Couldn't restore that."
                    trash = NoteStore.trashItems()
                    refresh()
                },
                onDeleteForever = { item ->
                    NoteStore.deleteFromTrash(item)
                    trash = NoteStore.trashItems()
                },
                onEmpty = {
                    val n = NoteStore.emptyTrash()
                    trash = NoteStore.trashItems()
                    status = "Deleted $n for good."
                },
            )

            Screen.SETTINGS -> SettingsScreen(
                panelWidth = panelWidth,
                onPanelWidth = {
                    Prefs.setPanelWidth(context, it)
                    panelWidth = it
                },
                fabSize = fabSize,
                onFabSize = {
                    Prefs.setFabSize(context, it)
                    fabSize = it
                },
                defaultDir = defaultDir,
                onDefaultDir = {
                    Prefs.setDefaultDir(context, it)
                    defaultDir = Prefs.defaultDir(context)
                },
                trashCount = trash.size,
                onOpenTrash = {
                    trash = NoteStore.trashItems()
                    screen = Screen.TRASH
                },
                resumeNew = resumeNew,
                onResumeNew = {
                    Prefs.setResumeNew(context, it)
                    resumeNew = it
                },
                resumeMinutes = resumeMinutes,
                onResumeMinutes = {
                    Prefs.setResumeMinutes(context, it)
                    resumeMinutes = it
                },
                token = Prefs.token(context),
                onToken = { Prefs.setToken(context, it) },
                notesPath = NoteStore.root.absolutePath,
                onBack = { screen = Screen.HOME },
                onCheckUpdate = { checkForUpdate() },
            )

            Screen.EDITOR -> EditorScreen(
                title = title,
                onTitleChange = { title = it },
                body = body,
                onBodyChange = { new ->
                    body = continueListOnNewline(body, new)
                },
                bodyFocus = bodyFocus,
                readMode = readMode,
                onEnterRead = {
                    persist()
                    readMode = true
                },
                onEnterEdit = {
                    readMode = false
                    raiseKeyboard()
                },
                onToggleLine = { lineIndex ->
                    val updated = toggleCheckboxAt(body.text, lineIndex)
                    body = body.copy(text = updated)
                    persist()
                },
                showToolbar = showToolbar,
                onToggleToolbar = { showToolbar = !showToolbar },
                isPinned = currentFile?.let { f -> pins.any { it.path == f.absolutePath } } ?: false,
                isLocked = currentFile?.let { isLocked(it) } ?: false,
                noteMenuOpen = noteMenu,
                onNoteMenu = { noteMenu = it },
                onToggleLock = { currentFile?.let { toggleLock(it) } },
                onMoveNote = {
                    persist()
                    currentFile?.let { dialog = DialogState.MoveTo(setOf(it.absolutePath)) }
                },
                onDeleteNote = {
                    currentFile?.let { f ->
                        dialog = DialogState.Delete(NoteNode(f, isFolder = false))
                    }
                },
                canPin = currentFile != null,
                onTogglePin = {
                    currentFile?.let { f ->
                        if (pins.any { it.path == f.absolutePath }) {
                            Prefs.unpin(context, f.absolutePath)
                        } else {
                            Prefs.pin(context, f.absolutePath)
                        }
                        pins = Prefs.pins(context)
                    }
                },
                onOpenSearch = { persist(); searching = true; screen = Screen.HOME },
                onBrowse = { scope.launch { drawerState.open() } },
                overflowOpen = overflow,
                onOverflow = { overflow = it },
                onSettings = {
                    persist()
                    trash = NoteStore.trashItems()
                    screen = Screen.SETTINGS
                },
                onCheckUpdate = { checkForUpdate() },
                onDone = { goHome() },
                onNewNote = { newNote() },
            )
        }
    }

    // ---------------- dialogs ----------------
    when (val d = dialog) {
        is DialogState.NewFolder -> TextPrompt(
            titleText = "New folder",
            label = "Folder name",
            confirm = "Create",
            onDismiss = { dialog = null },
            onConfirm = { name ->
                NoteStore.createFolder(d.parent, name)
                refresh()
                dialog = null
            },
        )

        is DialogState.Rename -> TextPrompt(
            titleText = "Rename",
            label = "New name",
            initial = d.node.displayName,
            confirm = "Rename",
            onDismiss = { dialog = null },
            onConfirm = { name ->
                val old = d.node.file.absolutePath
                val renamed = NoteStore.rename(d.node, name)
                if (renamed != null) {
                    Prefs.repath(context, old, renamed.absolutePath)
                    if (d.node.file == currentFile) {
                        currentFile = renamed
                        title = renamed.name.removeSuffix(".md")
                    }
                }
                refresh()
                dialog = null
            },
        )

        is DialogState.Delete -> {
            val node = d.node
            val path = node.file.absolutePath
            val lockedInside = if (node.isFolder) Prefs.lockedUnder(context, path) else emptyList()
            val selfLocked = !node.isFolder && lockedPaths.contains(path)
            val noteCount = if (node.isFolder) NoteStore.countNotesUnder(node.file) else 0
            val isDefaultDir = node.isFolder && defaultDir == path
            val blocked = selfLocked || lockedInside.isNotEmpty()

            AlertDialog(
                onDismissRequest = { dialog = null },
                containerColor = Slate.Secondary,
                title = {
                    Text(
                        if (blocked) "Can't delete ${node.displayName}"
                        else "Delete ${node.displayName}?",
                        color = Slate.Text, fontSize = 17.sp,
                    )
                },
                text = {
                    Column {
                        when {
                            selfLocked -> Text(
                                "This note is locked. Unlock it first if you really want it gone.",
                                color = Slate.Muted, fontSize = 14.sp,
                            )

                            lockedInside.isNotEmpty() -> Text(
                                "This folder holds ${lockedInside.size} locked " +
                                    (if (lockedInside.size == 1) "note" else "notes") +
                                    ". Unlock them first if you really want the folder gone.",
                                color = Slate.Muted, fontSize = 14.sp,
                            )

                            node.isFolder -> {
                                Text(
                                    if (noteCount == 0) "The folder is empty."
                                    else "$noteCount " +
                                        (if (noteCount == 1) "note" else "notes") +
                                        " inside will go to the trash with it.",
                                    color = if (noteCount == 0) Slate.Muted else Slate.Text,
                                    fontSize = 14.sp,
                                )
                                if (isDefaultDir) {
                                    Text(
                                        "This is your folder for new notes. Deleting it sends new notes back to the top level.",
                                        color = Slate.Danger, fontSize = 13.sp,
                                        modifier = Modifier.padding(top = 8.dp),
                                    )
                                }
                            }

                            else -> Text(
                                "The note goes to the trash.",
                                color = Slate.Muted, fontSize = 14.sp,
                            )
                        }
                        if (!blocked) {
                            Text(
                                "You can restore it from the trash for ${Prefs.TRASH_KEEP_DAYS} days.",
                                color = Slate.Muted, fontSize = 12.sp,
                                modifier = Modifier.padding(top = 10.dp),
                            )
                        }
                    }
                },
                confirmButton = {
                    if (blocked) {
                        TextButton(onClick = { dialog = null }) {
                            Text("OK", color = Slate.Accent)
                        }
                    } else {
                        TextButton(onClick = {
                            val ok = trashNode(node)
                            status = if (ok) "Moved to trash." else "Couldn't delete that."
                            refresh()
                            dialog = null
                        }) { Text("Move to trash", color = Slate.Danger) }
                    }
                },
                dismissButton = {
                    if (!blocked) {
                        TextButton(onClick = { dialog = null }) { Text("Cancel", color = Slate.Muted) }
                    }
                },
            )
        }

        is DialogState.DeleteMany -> {
            val nodes = topLevelOf(NoteStore.flatten(tree).filter { d.paths.contains(it.file.absolutePath) })
            val locked = nodes.filter { n ->
                lockedPaths.contains(n.file.absolutePath) ||
                    (n.isFolder && Prefs.lockedUnder(context, n.file.absolutePath).isNotEmpty())
            }
            val deletable = nodes - locked.toSet()

            AlertDialog(
                onDismissRequest = { dialog = null },
                containerColor = Slate.Secondary,
                title = {
                    Text(
                        "Delete ${deletable.size} " +
                            if (deletable.size == 1) "item?" else "items?",
                        color = Slate.Text, fontSize = 17.sp,
                    )
                },
                text = {
                    Column {
                        if (deletable.isEmpty()) {
                            Text(
                                "Everything you picked is locked, so nothing would be deleted.",
                                color = Slate.Muted, fontSize = 14.sp,
                            )
                        } else {
                            Text(
                                "They go to the trash and can be restored for ${Prefs.TRASH_KEEP_DAYS} days.",
                                color = Slate.Muted, fontSize = 14.sp,
                            )
                        }
                        if (locked.isNotEmpty()) {
                            Text(
                                "${locked.size} locked " +
                                    (if (locked.size == 1) "item is" else "items are") +
                                    " in your selection and will be left alone.",
                                color = Slate.Danger, fontSize = 13.sp,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                        }
                    }
                },
                confirmButton = {
                    if (deletable.isEmpty()) {
                        TextButton(onClick = { dialog = null }) { Text("OK", color = Slate.Accent) }
                    } else {
                        TextButton(onClick = {
                            // Deepest paths first, so binning a child cannot
                            // fail because its parent already moved.
                            var n = 0
                            deletable
                                .sortedByDescending { it.file.absolutePath.count { ch -> ch == '/' } }
                                .forEach { if (trashNode(it)) n++ }
                            selected = null
                            status = "Moved $n to trash."
                            refresh()
                            dialog = null
                        }) { Text("Move to trash", color = Slate.Danger) }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { dialog = null }) { Text("Cancel", color = Slate.Muted) }
                },
            )
        }

        is DialogState.MoveTo -> {
            val moving = topLevelOf(NoteStore.flatten(tree).filter { d.paths.contains(it.file.absolutePath) })
            // A folder cannot be moved inside itself, and nothing gains from
            // being "moved" into the folder it already sits in.
            val blockedPrefixes = moving.filter { it.isFolder }.map { it.file.absolutePath }
            val currentParents = moving.mapNotNull { it.file.parentFile?.absolutePath }.toSet()
            val choices = NoteStore.allFolders().filter { f ->
                val fp = f.absolutePath
                blockedPrefixes.none { fp == it || fp.startsWith("$it/") } &&
                    !(moving.size == 1 && currentParents.contains(fp))
            }

            AlertDialog(
                onDismissRequest = { dialog = null },
                containerColor = Slate.Secondary,
                title = {
                    Text(
                        if (moving.size == 1) "Move ${moving.first().displayName} to..."
                        else "Move ${moving.size} items to...",
                        color = Slate.Text, fontSize = 17.sp,
                    )
                },
                text = {
                    if (choices.isEmpty()) {
                        Text(
                            "There is nowhere else to put it. Make a folder first.",
                            color = Slate.Muted, fontSize = 14.sp,
                        )
                    } else {
                        Column(
                            Modifier
                                .heightIn(max = 380.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            choices.forEach { folder ->
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            var n = 0
                                            moving.forEach { if (moveNode(it, folder)) n++ }
                                            selected = null
                                            status = if (n == 0) "Couldn't move that."
                                            else "Moved $n to ${NoteStore.labelFor(folder)}."
                                            dialog = null
                                        }
                                        .padding(vertical = 11.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        Icons.Filled.Folder, null,
                                        tint = Slate.Sun,
                                        modifier = Modifier.size(17.dp),
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        NoteStore.labelFor(folder),
                                        color = Slate.Text, fontSize = 14.sp,
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { dialog = null }) { Text("Cancel", color = Slate.Muted) }
                },
            )
        }

        null -> Unit
    }

    update?.let { info ->
        AlertDialog(
            onDismissRequest = { if (progress < 0) update = null },
            containerColor = Slate.Secondary,
            title = { Text("Update available: v${info.version}", color = Slate.Text, fontSize = 17.sp) },
            text = {
                Column {
                    Text("You're on v${BuildConfig.VERSION_NAME}.", color = Slate.Muted, fontSize = 13.sp)
                    if (info.notes.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(info.notes, color = Slate.Text, fontSize = 13.sp)
                    }
                    if (progress >= 0) {
                        Spacer(Modifier.height(12.dp))
                        LinearProgressIndicator(
                            progress = { progress / 100f },
                            color = Slate.Accent,
                            trackColor = Slate.Divider,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = progress < 0,
                    onClick = {
                        progress = 0
                        scope.launch {
                            Updater.downloadApk(context, info) { progress = it }
                                .onSuccess { apk ->
                                    progress = -1
                                    update = null
                                    Updater.installApk(context, apk)
                                }
                                .onFailure {
                                    progress = -1
                                    status = it.message ?: "Download failed."
                                    update = null
                                }
                        }
                    },
                ) {
                    Text(
                        if (progress >= 0) "Downloading... $progress%" else "Download & install",
                        color = Slate.Accent,
                    )
                }
            },
            dismissButton = {
                TextButton(enabled = progress < 0, onClick = { update = null }) {
                    Text("Later", color = Slate.Muted)
                }
            },
        )
    }

    status?.let { msg ->
        AlertDialog(
            onDismissRequest = { status = null },
            containerColor = Slate.Secondary,
            text = { Text(msg, color = Slate.Text, fontSize = 14.sp) },
            confirmButton = {
                TextButton(onClick = { status = null }) { Text("OK", color = Slate.Accent) }
            },
        )
    }
}

// ==================================================================
// Home
// ==================================================================

@Composable
private fun HomeScreen(
    pins: List<Pin>,
    panelWidth: PanelWidth,
    fabSize: FabSize,
    tree: List<NoteNode>,
    openFile: File?,
    expanded: Set<String>,
    searching: Boolean,
    actions: TreeActions,
    selected: Set<String>?,
    onClearSelection: () -> Unit,
    onSelectDelete: () -> Unit,
    onSelectMove: () -> Unit,
    onOpenSearch: () -> Unit,
    onCloseSearch: () -> Unit,
    onOverflow: (Boolean) -> Unit,
    overflowOpen: Boolean,
    onSettings: () -> Unit,
    onCheckUpdate: () -> Unit,
    onNewNote: () -> Unit,
    onOpenPin: (String) -> Unit,
    onMovePin: (String, Int) -> Unit,
    onSetIcon: (String, String) -> Unit,
    onSetColor: (String, String) -> Unit,
    onUnpin: (String) -> Unit,
) {
    // The button floats over everything, so both scrolling columns reserve
    // enough room underneath that nothing can end up hidden behind it.
    val fabClearance = (fabSize.sizeDp + 40).dp

    Box(
        Modifier
            .fillMaxSize()
            .background(Slate.Background)
    ) {
        Row(Modifier.fillMaxSize()) {
            Column(Modifier.weight(1f)) {
                if (searching) {
                    SearchPane(
                        tree = tree,
                        onOpenNote = actions.onOpenNote,
                        onClose = onCloseSearch,
                        bottomReserve = fabClearance,
                        modifier = Modifier
                            .statusBarsPadding()
                            .navigationBarsPadding(),
                    )
                } else if (selected != null) {
                    SelectionBar(
                        count = selected.size,
                        onMove = onSelectMove,
                        onDelete = onSelectDelete,
                        onCancel = onClearSelection,
                    )
                } else {
                    TopBar(
                        onOpenSearch = onOpenSearch,
                        overflowOpen = overflowOpen,
                        onOverflow = onOverflow,
                        onSettings = onSettings,
                        onCheckUpdate = onCheckUpdate,
                        trailing = {
                            IconButton(
                                onClick = { actions.onNewFolderIn(NoteStore.ensureRoot()) },
                                modifier = Modifier.size(42.dp),
                            ) {
                                Icon(
                                    Icons.Filled.CreateNewFolder, "New folder",
                                    tint = Slate.Muted,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        },
                    )
                    NoteTreeList(
                        tree = tree,
                        openFile = openFile,
                        expanded = expanded,
                        rowBackground = Slate.Background,
                        actions = actions,
                        selection = selected?.let { TreeSelection(it) },
                        bottomReserve = fabClearance,
                        modifier = Modifier.navigationBarsPadding(),
                    )
                }
            }

            Box(
                Modifier
                    .width(1.dp)
                    .fillMaxSize()
                    .background(Slate.Divider)
            )

            PinnedPanel(
                pins = pins,
                width = panelWidth,
                bottomReserve = fabClearance,
                onOpen = onOpenPin,
                onMove = onMovePin,
                onSetIcon = onSetIcon,
                onSetColor = onSetColor,
                onUnpin = onUnpin,
                modifier = Modifier
                    .width(panelWidth.widthDp.dp)
                    .statusBarsPadding()
                    .navigationBarsPadding(),
            )
        }

        // Fixed to the bottom-right corner, sitting above the pinned panel.
        FloatingActionButton(
            onClick = onNewNote,
            containerColor = Slate.Accent,
            contentColor = Slate.Secondary,
            shape = androidx.compose.foundation.shape.CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 14.dp, bottom = 18.dp)
                .size(fabSize.sizeDp.dp),
        ) {
            Icon(Icons.Filled.Add, "New note", modifier = Modifier.size(fabSize.iconDp.dp))
        }
    }
}

// ==================================================================
// Editor
// ==================================================================

@Composable
private fun EditorScreen(
    title: String,
    onTitleChange: (String) -> Unit,
    body: TextFieldValue,
    onBodyChange: (TextFieldValue) -> Unit,
    bodyFocus: FocusRequester,
    readMode: Boolean,
    onEnterRead: () -> Unit,
    onEnterEdit: () -> Unit,
    onToggleLine: (Int) -> Unit,
    showToolbar: Boolean,
    onToggleToolbar: () -> Unit,
    isPinned: Boolean,
    canPin: Boolean,
    onTogglePin: () -> Unit,
    isLocked: Boolean,
    noteMenuOpen: Boolean,
    onNoteMenu: (Boolean) -> Unit,
    onToggleLock: () -> Unit,
    onMoveNote: () -> Unit,
    onDeleteNote: () -> Unit,
    onOpenSearch: () -> Unit,
    onBrowse: () -> Unit,
    overflowOpen: Boolean,
    onOverflow: (Boolean) -> Unit,
    onSettings: () -> Unit,
    onCheckUpdate: () -> Unit,
    onDone: () -> Unit,
    onNewNote: () -> Unit,
) {
    val transformation = remember { LiveMarkdownTransformation() }

    Column(
        Modifier
            .fillMaxSize()
            .background(Slate.Background)
            .imePadding()
    ) {
        TopBar(
            onOpenSearch = onOpenSearch,
            onBrowse = onBrowse,
            overflowOpen = overflowOpen,
            onOverflow = onOverflow,
            onSettings = onSettings,
            onCheckUpdate = onCheckUpdate,
            trailing = {
                if (canPin) {
                    IconButton(onClick = onTogglePin, modifier = Modifier.size(42.dp)) {
                        Icon(
                            if (isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                            if (isPinned) "Unpin" else "Pin to home",
                            tint = if (isPinned) Slate.Sun else Slate.Muted,
                            modifier = Modifier.size(21.dp),
                        )
                    }
                    Box {
                        IconButton(
                            onClick = { onNoteMenu(true) },
                            modifier = Modifier.size(42.dp),
                        ) {
                            Icon(
                                Icons.Filled.MoreHoriz, "Note actions",
                                tint = Slate.Muted,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                        DropdownMenu(
                            expanded = noteMenuOpen,
                            onDismissRequest = { onNoteMenu(false) },
                        ) {
                            DropdownMenuItem(
                                text = { Text("Move to...", fontSize = 14.sp) },
                                onClick = { onNoteMenu(false); onMoveNote() },
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        if (isLocked) "Unlock" else "Lock (prevent delete)",
                                        fontSize = 14.sp,
                                    )
                                },
                                onClick = { onNoteMenu(false); onToggleLock() },
                            )
                            DropdownMenuItem(
                                text = { Text("Delete note", fontSize = 14.sp, color = Slate.Danger) },
                                onClick = { onNoteMenu(false); onDeleteNote() },
                            )
                        }
                    }
                }
            },
        )

        BasicTextField(
            value = title,
            onValueChange = onTitleChange,
            singleLine = true,
            textStyle = TextStyle(
                color = Slate.Text,
                fontSize = 19.sp,
                fontWeight = FontWeight.SemiBold,
            ),
            cursorBrush = SolidColor(Slate.Accent),
            decorationBox = { inner ->
                Box(Modifier.padding(horizontal = 18.dp, vertical = 12.dp)) {
                    if (title.isEmpty()) {
                        Text("Title", color = Slate.Placeholder, fontSize = 19.sp)
                    }
                    inner()
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )

        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Slate.Divider)
        )

        Box(Modifier.weight(1f)) {
            if (readMode) {
                ReadView(
                    text = body.text,
                    onToggleLine = onToggleLine,
                    onEditRequest = onEnterEdit,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                BasicTextField(
                    value = body,
                    onValueChange = onBodyChange,
                    visualTransformation = transformation,
                    textStyle = TextStyle(color = Slate.Text, fontSize = 16.sp, lineHeight = 25.sp),
                    cursorBrush = SolidColor(Slate.Accent),
                    decorationBox = { inner ->
                        Box(Modifier.padding(horizontal = 18.dp, vertical = 14.dp)) {
                            if (body.text.isEmpty()) {
                                Text("Start typing...", color = Slate.Placeholder, fontSize = 16.sp)
                            }
                            inner()
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .focusRequester(bodyFocus),
                )
            }
        }

        AnimatedVisibility(visible = showToolbar && !readMode) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(Slate.Secondary)
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ToolButton(Icons.Filled.Checklist, "Checkbox") {
                    onBodyChange(prefixLine(body, "- [ ] "))
                }
                ToolButton(Icons.Filled.FormatListBulleted, "Bullet") {
                    onBodyChange(prefixLine(body, "- "))
                }
                ToolButton(Icons.Filled.FormatBold, "Bold") {
                    onBodyChange(wrapSelection(body, "**"))
                }
                ToolButton(Icons.Filled.FormatItalic, "Italic") {
                    onBodyChange(wrapSelection(body, "*"))
                }
                ToolButton(Icons.Filled.Title, "Heading") {
                    onBodyChange(prefixLine(body, "## "))
                }
                ToolButton(Icons.Filled.FormatQuote, "Quote") {
                    onBodyChange(prefixLine(body, "> "))
                }
                ToolButton(Icons.Filled.Code, "Code") {
                    onBodyChange(wrapSelection(body, "`"))
                }
            }
        }

        // ---------------- bottom action bar ----------------
        Row(
            Modifier
                .fillMaxWidth()
                .background(Slate.Secondary)
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (readMode) {
                TextButton(onClick = onEnterEdit) {
                    Text("Edit", color = Slate.Accent, fontSize = 14.sp)
                }
            } else {
                IconButton(onClick = onToggleToolbar, modifier = Modifier.size(42.dp)) {
                    Icon(
                        Icons.Filled.TextFormat,
                        if (showToolbar) "Hide formatting" else "Formatting",
                        tint = if (showToolbar) Slate.Accent else Slate.Muted,
                        modifier = Modifier.size(24.dp),
                    )
                }
                TextButton(onClick = onEnterRead) {
                    Text("Read", color = Slate.Muted, fontSize = 14.sp)
                }
            }

            Spacer(Modifier.weight(1f))

            Text(
                "v${BuildConfig.VERSION_NAME}",
                color = Slate.Muted,
                fontSize = 11.sp,
                modifier = Modifier.padding(end = 10.dp),
            )

            // New note: saves whatever is open, then starts a blank one.
            // Deliberately smaller and quieter than the confirm button so
            // the thumb doesn't mistake one for the other.
            FloatingActionButton(
                onClick = onNewNote,
                containerColor = Slate.Secondary,
                contentColor = Slate.Accent,
                shape = androidx.compose.foundation.shape.CircleShape,
                elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp),
                modifier = Modifier
                    .size(40.dp)
                    .border(1.dp, Slate.Divider, androidx.compose.foundation.shape.CircleShape),
            ) {
                Icon(Icons.Filled.Add, "New note", modifier = Modifier.size(20.dp))
            }

            Spacer(Modifier.width(10.dp))

            FloatingActionButton(
                onClick = onDone,
                containerColor = Slate.Accent,
                contentColor = Slate.Secondary,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.size(44.dp),
            ) {
                Icon(Icons.Filled.Check, "Done", modifier = Modifier.size(23.dp))
            }
        }
    }
}

@Composable
private fun ToolButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick, modifier = Modifier.size(46.dp)) {
        Icon(icon, label, tint = Slate.Text, modifier = Modifier.size(21.dp))
    }
}

// ==================================================================
// Shared top bar: controls on the LEFT, title after them
// ==================================================================

/**
 * Replaces the menu bar while items are ticked. Same height as the menu bar,
 * so the layout does not jump when selection mode starts.
 */
@Composable
private fun SelectionBar(
    count: Int,
    onMove: () -> Unit,
    onDelete: () -> Unit,
    onCancel: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Slate.Secondary)
            .statusBarsPadding()
            .padding(start = 4.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onCancel, modifier = Modifier.size(44.dp)) {
            Icon(
                Icons.Filled.Close, "Cancel selection",
                tint = Slate.Text,
                modifier = Modifier.size(22.dp),
            )
        }
        Text(
            "$count selected",
            color = Slate.Text,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.weight(1f))
        IconButton(onClick = onMove, modifier = Modifier.size(44.dp)) {
            Icon(
                Icons.Filled.DriveFileMove, "Move selected",
                tint = Slate.Accent,
                modifier = Modifier.size(21.dp),
            )
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(44.dp)) {
            Icon(
                Icons.Filled.Delete, "Delete selected",
                tint = Slate.Danger,
                modifier = Modifier.size(21.dp),
            )
        }
    }
}

@Composable
private fun TopBar(
    onOpenSearch: () -> Unit,
    overflowOpen: Boolean,
    onOverflow: (Boolean) -> Unit,
    onSettings: () -> Unit,
    onCheckUpdate: () -> Unit,
    trailing: @Composable () -> Unit,
    onBrowse: (() -> Unit)? = null,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Slate.Secondary)
            .statusBarsPadding()
            .padding(start = 4.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onOpenSearch, modifier = Modifier.size(44.dp)) {
            Icon(
                Icons.Filled.Search, "Search notes",
                tint = Slate.Text,
                modifier = Modifier.size(23.dp),
            )
        }

        if (onBrowse != null) {
            IconButton(onClick = onBrowse, modifier = Modifier.size(44.dp)) {
                Icon(
                    Icons.Outlined.FolderOpen, "Browse all notes",
                    tint = Slate.Muted,
                    modifier = Modifier.size(21.dp),
                )
            }
        }

        Box {
            IconButton(onClick = { onOverflow(true) }, modifier = Modifier.size(44.dp)) {
                Icon(
                    Icons.Filled.MoreVert, "More",
                    tint = Slate.Muted,
                    modifier = Modifier.size(22.dp),
                )
            }
            DropdownMenu(expanded = overflowOpen, onDismissRequest = { onOverflow(false) }) {
                DropdownMenuItem(
                    text = { Text("Settings", fontSize = 14.sp) },
                    onClick = { onOverflow(false); onSettings() },
                )
                DropdownMenuItem(
                    text = { Text("Check for updates", fontSize = 14.sp) },
                    onClick = { onOverflow(false); onCheckUpdate() },
                )
            }
        }

        Spacer(Modifier.width(6.dp))

        // Wordmark: a sunset gradient sweeping blue -> purple -> orange,
        // with its little sun glowing at the end of the sweep.
        Row(verticalAlignment = Alignment.Top) {
            Text(
                "Jot",
                style = TextStyle(
                    brush = Slate.SunsetBrush,
                    fontSize = 27.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
            )
            Box(
                Modifier
                    .padding(start = 3.dp, top = 4.dp)
                    .size(8.dp)
                    .background(Slate.SunsetOrange, androidx.compose.foundation.shape.CircleShape)
            )
        }

        Spacer(Modifier.weight(1f))
        trailing()
    }
}

// ==================================================================
// Dialog plumbing
// ==================================================================

private sealed interface DialogState {
    data class NewFolder(val parent: File) : DialogState
    data class Rename(val node: NoteNode) : DialogState
    data class Delete(val node: NoteNode) : DialogState
    data class DeleteMany(val paths: Set<String>) : DialogState
    data class MoveTo(val paths: Set<String>) : DialogState
}

@Composable
private fun TextPrompt(
    titleText: String,
    label: String,
    initial: String = "",
    confirm: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var value by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Slate.Secondary,
        title = { Text(titleText, color = Slate.Text, fontSize = 17.sp) },
        text = {
            androidx.compose.material3.OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                singleLine = true,
                label = { Text(label, fontSize = 13.sp) },
                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Slate.Accent,
                    unfocusedBorderColor = Slate.Divider,
                    focusedTextColor = Slate.Text,
                    unfocusedTextColor = Slate.Text,
                    cursorColor = Slate.Accent,
                ),
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(value) }) { Text(confirm, color = Slate.Accent) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = Slate.Muted) }
        },
    )
}
