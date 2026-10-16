package com.gentlemanjoel.jot.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
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
import com.gentlemanjoel.jot.UpdateInfo
import com.gentlemanjoel.jot.Updater
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

private enum class Screen { HOME, EDITOR, SETTINGS }

@Composable
fun JotApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val bodyFocus = remember { FocusRequester() }

    var screen by remember { mutableStateOf(Screen.EDITOR) }
    var tree by remember { mutableStateOf(NoteStore.loadTree()) }
    val expanded = remember { mutableStateListOf<String>() }
    var pins by remember { mutableStateOf(Prefs.pins(context)) }
    var panelWidth by remember { mutableStateOf(Prefs.panelWidth(context)) }
    var fabSize by remember { mutableStateOf(Prefs.fabSize(context)) }
    var searching by remember { mutableStateOf(false) }

    var currentFile by remember { mutableStateOf<File?>(null) }
    var currentDir by remember { mutableStateOf(NoteStore.ensureRoot()) }
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf(TextFieldValue("")) }
    var readMode by remember { mutableStateOf(false) }
    var showToolbar by remember { mutableStateOf(false) }

    var overflow by remember { mutableStateOf(false) }
    var dialog by remember { mutableStateOf<DialogState?>(null) }
    var status by remember { mutableStateOf<String?>(null) }
    var update by remember { mutableStateOf<UpdateInfo?>(null) }
    var progress by remember { mutableStateOf(-1) }

    fun draft() = Draft(currentFile, currentDir, title, body.text)

    fun refresh() {
        tree = NoteStore.loadTree()
        Prefs.prune(context)
        pins = Prefs.pins(context)
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

    fun newNote(dir: File = NoteStore.ensureRoot()) {
        persist()
        currentFile = null
        currentDir = dir
        title = ""
        body = TextFieldValue("")
        readMode = false
        showToolbar = false
        screen = Screen.EDITOR
        scope.launch {
            drawerState.close()
            delay(80)
            runCatching { bodyFocus.requestFocus() }
        }
    }

    fun openNote(file: File) {
        persist()
        val d = NoteStore.read(file)
        currentFile = d.file
        currentDir = d.dir
        title = d.title
        body = TextFieldValue(d.body)
        readMode = true          // existing notes open readable + tappable
        showToolbar = false
        screen = Screen.EDITOR
        scope.launch { drawerState.close() }
    }

    fun goHome() {
        persist()
        screen = Screen.HOME
    }

    // Cold start: straight into a blank note with the caret in the body.
    LaunchedEffect(Unit) {
        NoteStore.ensureRoot()
        refresh()
        delay(140)
        runCatching { bodyFocus.requestFocus() }
    }

    // Debounced autosave while typing.
    LaunchedEffect(Unit) {
        snapshotFlow { title to body.text }.collect {
            delay(900)
            if (!draft().isBlank) persist()
        }
    }

    // Save on backgrounding.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) persist()
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
    BackHandler(enabled = drawerState.isOpen || searching || screen != Screen.HOME) {
        when {
            drawerState.isOpen -> scope.launch { drawerState.close() }
            searching -> searching = false
            screen == Screen.EDITOR -> goHome()
            screen == Screen.SETTINGS -> screen = Screen.HOME
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
                DrawerTree(
                    tree = tree,
                    openFile = currentFile,
                    expanded = expanded.toSet(),
                    onToggleFolder = { path ->
                        if (expanded.contains(path)) expanded.remove(path) else expanded.add(path)
                    },
                    onOpenNote = { openNote(it) },
                    onNewNoteIn = { newNote(it) },
                    onNewFolderIn = { dialog = DialogState.NewFolder(it) },
                    onRename = { dialog = DialogState.Rename(it) },
                    onDelete = { dialog = DialogState.Delete(it) },
                )
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
                onToggleFolder = { path ->
                    if (expanded.contains(path)) expanded.remove(path) else expanded.add(path)
                },
                onOpenNote = { openNote(it) },
                onNewFolderIn = { dialog = DialogState.NewFolder(it) },
                onRename = { dialog = DialogState.Rename(it) },
                onDelete = { dialog = DialogState.Delete(it) },
                onOpenSearch = { searching = true },
                onCloseSearch = { searching = false },
                onOverflow = { overflow = it },
                overflowOpen = overflow,
                onSettings = { screen = Screen.SETTINGS },
                onCheckUpdate = { checkForUpdate() },
                onNewNote = { searching = false; newNote() },
                onOpenPin = { path -> openNote(File(path)) },
                onMovePin = { p, d -> Prefs.movePin(context, p, d); pins = Prefs.pins(context) },
                onSetIcon = { p, i -> Prefs.updatePin(context, p, icon = i); pins = Prefs.pins(context) },
                onSetColor = { p, c -> Prefs.updatePin(context, p, color = c); pins = Prefs.pins(context) },
                onUnpin = { p -> Prefs.unpin(context, p); pins = Prefs.pins(context) },
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
                onEnterEdit = {
                    readMode = false
                    scope.launch { delay(60); runCatching { bodyFocus.requestFocus() } }
                },
                onToggleLine = { lineIndex ->
                    val updated = toggleCheckboxAt(body.text, lineIndex)
                    body = body.copy(text = updated)
                    persist()
                },
                showToolbar = showToolbar,
                onToggleToolbar = { showToolbar = !showToolbar },
                isPinned = currentFile?.let { f -> pins.any { it.path == f.absolutePath } } ?: false,
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
                onSettings = { persist(); screen = Screen.SETTINGS },
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

        is DialogState.Delete -> AlertDialog(
            onDismissRequest = { dialog = null },
            containerColor = Slate.Secondary,
            title = { Text("Delete ${d.node.displayName}?", color = Slate.Text, fontSize = 17.sp) },
            text = {
                Text(
                    if (d.node.isFolder) "This removes the folder and everything inside it."
                    else "This deletes the note file.",
                    color = Slate.Muted, fontSize = 14.sp,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    Prefs.unpin(context, d.node.file.absolutePath)
                    if (d.node.file == currentFile) {
                        currentFile = null; title = ""; body = TextFieldValue("")
                    }
                    NoteStore.delete(d.node.file)
                    refresh()
                    dialog = null
                }) { Text("Delete", color = Slate.Danger) }
            },
            dismissButton = {
                TextButton(onClick = { dialog = null }) { Text("Cancel", color = Slate.Muted) }
            },
        )

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
    onToggleFolder: (String) -> Unit,
    onOpenNote: (File) -> Unit,
    onNewFolderIn: (File) -> Unit,
    onRename: (NoteNode) -> Unit,
    onDelete: (NoteNode) -> Unit,
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
                        onOpenNote = onOpenNote,
                        onClose = onCloseSearch,
                        bottomReserve = fabClearance,
                        modifier = Modifier
                            .statusBarsPadding()
                            .navigationBarsPadding(),
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
                                onClick = { onNewFolderIn(NoteStore.ensureRoot()) },
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
                        onToggleFolder = onToggleFolder,
                        onOpenNote = onOpenNote,
                        onNewNoteIn = { onNewNote() },
                        onNewFolderIn = onNewFolderIn,
                        onRename = onRename,
                        onDelete = onDelete,
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
    onEnterEdit: () -> Unit,
    onToggleLine: (Int) -> Unit,
    showToolbar: Boolean,
    onToggleToolbar: () -> Unit,
    isPinned: Boolean,
    canPin: Boolean,
    onTogglePin: () -> Unit,
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
                        Text("Title (optional)", color = Slate.Muted, fontSize = 19.sp)
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
                                Text("Start typing...", color = Slate.Muted, fontSize = 16.sp)
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
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (readMode) {
                TextButton(onClick = onEnterEdit) {
                    Text("Edit", color = Slate.Accent, fontSize = 14.sp)
                }
            } else {
                TextButton(onClick = onToggleToolbar) {
                    Text(
                        if (showToolbar) "Format" else "Format",
                        color = if (showToolbar) Slate.Accent else Slate.Muted,
                        fontSize = 14.sp,
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            Text(
                "v${BuildConfig.VERSION_NAME}",
                color = Slate.Muted,
                fontSize = 11.sp,
                modifier = Modifier.padding(end = 12.dp),
            )

            ExtendedFloatingActionButton(
                onClick = onDone,
                containerColor = Slate.Accent,
                contentColor = Slate.Secondary,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.height(50.dp),
            ) {
                Icon(Icons.Filled.Check, null, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(8.dp))
                Text("Done", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
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
                    Icons.Filled.Folder, "Browse all notes",
                    tint = Slate.Sun,
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

        // Wordmark with its little sun - the star in Sagan Slate.
        Row(verticalAlignment = Alignment.Top) {
            Text(
                "Jot",
                color = Slate.Text,
                fontSize = 27.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Box(
                Modifier
                    .padding(start = 3.dp, top = 4.dp)
                    .size(8.dp)
                    .background(Slate.Sun, androidx.compose.foundation.shape.CircleShape)
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
