package com.gentlemanjoel.jot.ui

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.CompositionLocalProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.gentlemanjoel.jot.BuildConfig
import com.gentlemanjoel.jot.Draft
import com.gentlemanjoel.jot.NoteNode
import com.gentlemanjoel.jot.NoteStore
import com.gentlemanjoel.jot.UpdateInfo
import com.gentlemanjoel.jot.Updater
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun JotApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val bodyFocus = remember { FocusRequester() }

    var tree by remember { mutableStateOf(NoteStore.loadTree()) }
    val expanded = remember { mutableStateListOf<String>() }

    var currentFile by remember { mutableStateOf<File?>(null) }
    var currentDir by remember { mutableStateOf(NoteStore.ensureRoot()) }
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf(TextFieldValue("")) }

    var mdMode by remember { mutableStateOf(false) }
    var preview by remember { mutableStateOf(false) }
    var overflow by remember { mutableStateOf(false) }

    var dialog by remember { mutableStateOf<DialogState?>(null) }
    var status by remember { mutableStateOf<String?>(null) }
    var update by remember { mutableStateOf<UpdateInfo?>(null) }
    var progress by remember { mutableStateOf(-1) }

    fun draft() = Draft(currentFile, currentDir, title, body.text)

    fun refresh() {
        tree = NoteStore.loadTree()
    }

    fun persist() {
        val saved = NoteStore.save(draft())
        currentFile = saved
        refresh()
    }

    fun newNote(dir: File) {
        persist()
        currentFile = null
        currentDir = dir
        title = ""
        body = TextFieldValue("")
        preview = false
        scope.launch {
            drawerState.close()
            bodyFocus.requestFocus()
        }
    }

    fun openNote(file: File) {
        persist()
        val d = NoteStore.read(file)
        currentFile = d.file
        currentDir = d.dir
        title = d.title
        body = TextFieldValue(d.body)
        preview = false
        scope.launch { drawerState.close() }
    }

    // Cold start lands on a blank note with the cursor in the body.
    LaunchedEffect(Unit) {
        NoteStore.ensureRoot()
        refresh()
        delay(120)
        runCatching { bodyFocus.requestFocus() }
    }

    // Debounced autosave.
    LaunchedEffect(Unit) {
        snapshotFlow { title to body.text }.collect {
            delay(900)
            if (!draft().isBlank) persist()
        }
    }

    // Save when the app goes to the background.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) persist()
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }

    // Right-hand drawer: flip layout direction around the drawer only.
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    ModalDrawerSheet(
                        drawerContainerColor = Slate.Secondary,
                        modifier = Modifier.width(290.dp),
                    ) {
                        DrawerTree(
                            tree = tree,
                            openFile = currentFile,
                            expanded = expanded.toMutableSet(),
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
                }
            },
        ) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Editor(
                    title = title,
                    onTitleChange = { title = it },
                    body = body,
                    onBodyChange = { body = it },
                    bodyFocus = bodyFocus,
                    mdMode = mdMode,
                    preview = preview,
                    overflow = overflow,
                    onOverflow = { overflow = it },
                    onToggleMd = { mdMode = !mdMode },
                    onTogglePreview = { preview = !preview },
                    onOpenDrawer = { scope.launch { drawerState.open() } },
                    onNewNote = { newNote(NoteStore.ensureRoot()) },
                    onAbout = { dialog = DialogState.About },
                    onCheckUpdate = {
                        status = "Checking for updates…"
                        scope.launch {
                            val res = Updater.check(context)
                            res.onSuccess { info ->
                                if (info == null) {
                                    status = "You're on the latest version."
                                    update = null
                                } else {
                                    status = null
                                    update = info
                                }
                            }.onFailure { status = it.message ?: "Update check failed." }
                        }
                    },
                )
            }
        }
    }

    // ---- dialogs ----
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
                val renamed = NoteStore.rename(d.node, name)
                if (renamed != null && d.node.file == currentFile) {
                    currentFile = renamed
                    title = renamed.name.removeSuffix(".md")
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
        DialogState.About -> AboutDialog(
            onDismiss = { dialog = null },
            onSaveToken = { Updater.saveToken(context, it) },
            initialToken = Updater.savedToken(context),
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
                    Text(
                        "You're on v${BuildConfig.VERSION_NAME}.",
                        color = Slate.Muted, fontSize = 13.sp,
                    )
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
                ) { Text(if (progress >= 0) "Downloading… $progress%" else "Download & install", color = Slate.Accent) }
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

// ------------------------------------------------------------------

@Composable
private fun Editor(
    title: String,
    onTitleChange: (String) -> Unit,
    body: TextFieldValue,
    onBodyChange: (TextFieldValue) -> Unit,
    bodyFocus: FocusRequester,
    mdMode: Boolean,
    preview: Boolean,
    overflow: Boolean,
    onOverflow: (Boolean) -> Unit,
    onToggleMd: () -> Unit,
    onTogglePreview: () -> Unit,
    onOpenDrawer: () -> Unit,
    onNewNote: () -> Unit,
    onAbout: () -> Unit,
    onCheckUpdate: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Slate.Background)
            .statusBarsPadding()
            .imePadding()
    ) {
        // App header
        Row(
            Modifier
                .fillMaxWidth()
                .background(Slate.Secondary)
                .padding(start = 18.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Jot", color = Slate.Text, fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onNewNote, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Filled.Add, "New note", tint = Slate.Accent, modifier = Modifier.size(22.dp))
            }
            Box {
                IconButton(onClick = { onOverflow(true) }, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Filled.MoreVert, "More", tint = Slate.Muted, modifier = Modifier.size(21.dp))
                }
                DropdownMenu(expanded = overflow, onDismissRequest = { onOverflow(false) }) {
                    DropdownMenuItem(
                        text = { Text("Check for updates", fontSize = 14.sp) },
                        onClick = { onOverflow(false); onCheckUpdate() },
                    )
                    DropdownMenuItem(
                        text = { Text("About & settings", fontSize = 14.sp) },
                        onClick = { onOverflow(false); onAbout() },
                    )
                }
            }
            IconButton(onClick = onOpenDrawer, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Filled.Menu, "Notes", tint = Slate.Text, modifier = Modifier.size(22.dp))
            }
        }

        // Title (optional, fill it whenever)
        BasicTextField(
            value = title,
            onValueChange = onTitleChange,
            singleLine = true,
            textStyle = TextStyle(color = Slate.Text, fontSize = 18.sp, fontWeight = FontWeight.SemiBold),
            cursorBrush = SolidColor(Slate.Accent),
            decorationBox = { inner ->
                Box(Modifier.padding(horizontal = 18.dp, vertical = 12.dp)) {
                    if (title.isEmpty()) {
                        Text("Title (optional)", color = Slate.Muted, fontSize = 18.sp)
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

        // Body or preview
        Box(Modifier.weight(1f)) {
            if (preview) {
                MarkdownPreview(body.text, Modifier.fillMaxSize())
            } else {
                BasicTextField(
                    value = body,
                    onValueChange = onBodyChange,
                    textStyle = TextStyle(color = Slate.Text, fontSize = 16.sp, lineHeight = 24.sp),
                    cursorBrush = SolidColor(Slate.Accent),
                    decorationBox = { inner ->
                        Box(Modifier.padding(horizontal = 18.dp, vertical = 14.dp)) {
                            if (body.text.isEmpty()) {
                                Text("Start typing…", color = Slate.Muted, fontSize = 16.sp)
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

        // Markdown bar
        if (mdMode) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(Slate.Secondary)
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MdButton(Icons.Filled.FormatBold, "Bold") { onBodyChange(wrapSelection(body, "**")) }
                MdButton(Icons.Filled.FormatItalic, "Italic") { onBodyChange(wrapSelection(body, "*")) }
                MdButton(Icons.Filled.Code, "Code") { onBodyChange(wrapSelection(body, "`")) }
                MdButton(Icons.Filled.Title, "Heading") { onBodyChange(prefixLine(body, "## ")) }
                MdButton(Icons.Filled.FormatListBulleted, "Bullet") { onBodyChange(prefixLine(body, "- ")) }
                MdButton(Icons.Filled.Checklist, "Checkbox") { onBodyChange(prefixLine(body, "- [ ] ")) }
                MdButton(Icons.Filled.FormatQuote, "Quote") { onBodyChange(prefixLine(body, "> ")) }
            }
        }

        // Footer
        Row(
            Modifier
                .fillMaxWidth()
                .background(Slate.Secondary)
                .navigationBarsPadding()
                .padding(horizontal = 10.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onToggleMd) {
                    Text(
                        if (mdMode) "Markdown ON" else "Markdown",
                        color = if (mdMode) Slate.Accent else Slate.Muted,
                        fontSize = 13.sp,
                    )
                }
                if (mdMode) {
                    IconButton(onClick = onTogglePreview, modifier = Modifier.size(34.dp)) {
                        Icon(
                            if (preview) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            "Preview",
                            tint = if (preview) Slate.Accent else Slate.Muted,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
            Text("v${BuildConfig.VERSION_NAME}", color = Slate.Muted, fontSize = 11.sp)
        }
    }
}

@Composable
private fun MdButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick, modifier = Modifier.size(42.dp)) {
        Icon(icon, label, tint = Slate.Text, modifier = Modifier.size(19.dp))
    }
}

// ------------------------------------------------------------------

private sealed interface DialogState {
    data class NewFolder(val parent: File) : DialogState
    data class Rename(val node: NoteNode) : DialogState
    data class Delete(val node: NoteNode) : DialogState
    data object About : DialogState
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
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                singleLine = true,
                label = { Text(label, fontSize = 13.sp) },
                colors = OutlinedTextFieldDefaults.colors(
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

@Composable
private fun AboutDialog(
    onDismiss: () -> Unit,
    onSaveToken: (String) -> Unit,
    initialToken: String,
) {
    var token by remember { mutableStateOf(initialToken) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Slate.Secondary,
        title = { Text("Jot v${BuildConfig.VERSION_NAME}", color = Slate.Text, fontSize = 18.sp) },
        text = {
            Column {
                Text("Notes folder", color = Slate.Muted, fontSize = 13.sp)
                Text(NoteStore.root.absolutePath, color = Slate.Text, fontSize = 13.sp)
                Spacer(Modifier.height(14.dp))
                Text(
                    "Point Syncthing at that folder to sync with your PC.",
                    color = Slate.Muted, fontSize = 13.sp,
                )
                Spacer(Modifier.height(14.dp))
                Text("GitHub token (only needed for a private repo)", color = Slate.Muted, fontSize = 13.sp)
                OutlinedTextField(
                    value = token,
                    onValueChange = { token = it },
                    singleLine = true,
                    placeholder = { Text("ghp_…", fontSize = 13.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Slate.Accent,
                        unfocusedBorderColor = Slate.Divider,
                        focusedTextColor = Slate.Text,
                        unfocusedTextColor = Slate.Text,
                        cursorColor = Slate.Accent,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSaveToken(token); onDismiss() }) {
                Text("Save", color = Slate.Accent)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close", color = Slate.Muted) }
        },
    )
}
