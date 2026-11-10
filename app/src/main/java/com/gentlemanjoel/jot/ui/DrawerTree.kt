package com.gentlemanjoel.jot.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gentlemanjoel.jot.NoteNode
import com.gentlemanjoel.jot.NoteStore
import java.io.File

/**
 * Everything a tree row can do, bundled into one object. The tree recurses,
 * so passing these individually meant a dozen positional arguments at every
 * level - easy to transpose and hard to extend.
 */
data class TreeActions(
    val onToggleFolder: (String) -> Unit,
    val onOpenNote: (File) -> Unit,
    val onNewNoteIn: (File) -> Unit,
    val onNewFolderIn: (File) -> Unit,
    val onRename: (NoteNode) -> Unit,
    val onDelete: (NoteNode) -> Unit,
    val onMove: (NoteNode) -> Unit = {},
    val onToggleLock: (NoteNode) -> Unit = {},
    val onStartSelection: (NoteNode) -> Unit = {},
    val onToggleSelected: (NoteNode) -> Unit = {},
    val isLocked: (NoteNode) -> Boolean = { false },
)

/** Which paths are ticked. Non-null means the tree is in multi-select mode. */
data class TreeSelection(val paths: Set<String>)

/**
 * The folder tree of every note. Lives on the home screen, and is reused
 * inside the editor's slide-out drawer.
 */
@Composable
fun NoteTreeList(
    tree: List<NoteNode>,
    openFile: File?,
    expanded: Set<String>,
    rowBackground: androidx.compose.ui.graphics.Color,
    actions: TreeActions,
    selection: TreeSelection? = null,
    modifier: Modifier = Modifier,
    bottomReserve: Dp = 0.dp,
) {
    if (tree.isEmpty()) {
        Column(
            modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                Icons.Filled.Description, null,
                tint = Slate.Divider,
                modifier = Modifier.size(34.dp),
            )
            Text(
                "No notes yet.",
                color = Slate.Muted,
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 10.dp),
            )
            Text(
                "Tap + to write your first one.",
                color = Slate.Divider,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = bottomReserve),
    ) {
        renderTree(
            nodes = tree,
            depth = 0,
            expanded = expanded,
            openFile = openFile,
            rowBackground = rowBackground,
            actions = actions,
            selection = selection,
        )
    }
}

/**
 * Full-screen search: a text field that fills with results as you type, and a
 * large X in the top-right to get back out.
 */
@Composable
fun SearchPane(
    tree: List<NoteNode>,
    onOpenNote: (File) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    bottomReserve: Dp = 0.dp,
) {
    var query by remember { mutableStateOf("") }
    val results = remember(query, tree) { NoteStore.search(tree, query) }
    val focus = remember { FocusRequester() }

    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }

    Column(
        modifier
            .fillMaxSize()
            .background(Slate.Background)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 6.dp, top = 10.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                placeholder = { Text("Search notes", color = Slate.Muted, fontSize = 15.sp) },
                leadingIcon = {
                    Icon(Icons.Filled.Search, null, tint = Slate.Muted, modifier = Modifier.size(19.dp))
                },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Slate.Accent,
                    unfocusedBorderColor = Slate.Divider,
                    focusedTextColor = Slate.Text,
                    unfocusedTextColor = Slate.Text,
                    cursorColor = Slate.Accent,
                ),
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focus),
            )
            IconButton(onClick = onClose, modifier = Modifier.size(52.dp)) {
                Icon(
                    Icons.Filled.Close, "Close search",
                    tint = Slate.Text,
                    modifier = Modifier.size(30.dp),
                )
            }
        }

        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Slate.Divider)
        )

        when {
            query.isBlank() -> Text(
                "Start typing to search note names and contents.",
                color = Slate.Muted,
                fontSize = 13.sp,
                modifier = Modifier.padding(16.dp),
            )

            results.isEmpty() -> Text(
                "No matches for \"$query\".",
                color = Slate.Muted,
                fontSize = 13.sp,
                modifier = Modifier.padding(16.dp),
            )

            else -> {
                Text(
                    "${results.size} ${if (results.size == 1) "match" else "matches"}",
                    color = Slate.Muted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 16.dp, top = 10.dp, bottom = 2.dp),
                )
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = bottomReserve),
                ) {
                    items(results, key = { it.file.absolutePath }) { node ->
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onOpenNote(node.file) }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                            ) {
                                Icon(
                                    Icons.Filled.Description, null,
                                    tint = Slate.Muted,
                                    modifier = Modifier.size(17.dp),
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    node.displayName,
                                    color = Slate.Text,
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                )
                            }
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(Slate.Divider.copy(alpha = 0.5f))
                            )
                        }
                    }
                }
            }
        }
    }
}

/** The editor's slide-out drawer: the same tree, with its own new note/folder row. */
@Composable
fun DrawerTree(
    tree: List<NoteNode>,
    openFile: File?,
    expanded: Set<String>,
    actions: TreeActions,
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Slate.Secondary)
            .statusBarsPadding()
            .padding(top = 8.dp)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 10.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("All notes", color = Slate.Text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            IconButton(
                onClick = { actions.onNewNoteIn(NoteStore.ensureRoot()) },
                modifier = Modifier.size(34.dp),
            ) {
                Icon(Icons.Filled.Add, "New note", tint = Slate.Accent, modifier = Modifier.size(19.dp))
            }
            IconButton(
                onClick = { actions.onNewFolderIn(NoteStore.ensureRoot()) },
                modifier = Modifier.size(34.dp),
            ) {
                Icon(
                    Icons.Filled.CreateNewFolder, "New folder",
                    tint = Slate.Accent, modifier = Modifier.size(19.dp),
                )
            }
        }

        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Slate.Divider)
        )

        NoteTreeList(
            tree = tree,
            openFile = openFile,
            expanded = expanded,
            rowBackground = Slate.Secondary,
            actions = actions,
        )
    }
}

private fun LazyListScope.renderTree(
    nodes: List<NoteNode>,
    depth: Int,
    expanded: Set<String>,
    openFile: File?,
    rowBackground: androidx.compose.ui.graphics.Color,
    actions: TreeActions,
    selection: TreeSelection?,
) {
    nodes.forEach { node ->
        val path = node.file.absolutePath
        val isExpanded = expanded.contains(path)
        item(key = path) {
            TreeRow(
                node = node,
                depth = depth,
                isOpen = openFile?.absolutePath == path,
                isExpanded = isExpanded,
                rowBackground = rowBackground,
                isLocked = actions.isLocked(node),
                selecting = selection != null,
                isSelected = selection?.paths?.contains(path) == true,
                onClick = {
                    when {
                        selection != null -> actions.onToggleSelected(node)
                        node.isFolder -> actions.onToggleFolder(path)
                        else -> actions.onOpenNote(node.file)
                    }
                },
                actions = actions,
            )
        }
        if (node.isFolder && isExpanded) {
            renderTree(
                node.children, depth + 1, expanded, openFile, rowBackground,
                actions, selection,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TreeRow(
    node: NoteNode,
    depth: Int,
    isOpen: Boolean,
    isExpanded: Boolean,
    rowBackground: androidx.compose.ui.graphics.Color,
    isLocked: Boolean,
    selecting: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    actions: TreeActions,
) {
    var menu by remember { mutableStateOf(false) }

    Box {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    when {
                        isSelected -> Slate.Accent.copy(alpha = 0.20f)
                        isOpen -> Slate.Divider.copy(alpha = 0.35f)
                        else -> rowBackground
                    }
                )
                .combinedClickable(
                    onClick = onClick,
                    // While selecting, a long press would be ambiguous, so the
                    // menu is only offered in normal mode.
                    onLongClick = { if (!selecting) menu = true },
                )
                .padding(
                    start = (12 + depth * 14).dp,
                    end = 10.dp,
                    top = 10.dp,
                    bottom = 10.dp
                )
        ) {
            if (selecting) {
                Icon(
                    if (isSelected) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
                    if (isSelected) "Selected" else "Not selected",
                    tint = if (isSelected) Slate.Accent else Slate.Muted,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(9.dp))
            }

            Icon(
                imageVector = when {
                    node.isFolder && isExpanded -> Icons.Filled.FolderOpen
                    node.isFolder -> Icons.Filled.Folder
                    else -> Icons.Filled.Description
                },
                contentDescription = null,
                tint = if (node.isFolder) Slate.Sun else Slate.Muted,
                modifier = Modifier.size(17.dp),
            )
            Spacer(Modifier.width(9.dp))
            Text(
                text = node.displayName,
                color = if (isOpen) Slate.Accent else Slate.Text,
                fontSize = 14.sp,
                fontWeight = if (isOpen) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                modifier = Modifier.weight(1f, fill = false),
            )
            if (isLocked) {
                Spacer(Modifier.width(7.dp))
                Icon(
                    Icons.Filled.Lock, "Locked",
                    tint = Slate.Muted,
                    modifier = Modifier.size(13.dp),
                )
            }
        }

        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            if (node.isFolder) {
                DropdownMenuItem(
                    text = { Text("New note here", fontSize = 14.sp) },
                    onClick = { menu = false; actions.onNewNoteIn(node.file) },
                )
                DropdownMenuItem(
                    text = { Text("New subfolder", fontSize = 14.sp) },
                    onClick = { menu = false; actions.onNewFolderIn(node.file) },
                )
            }
            DropdownMenuItem(
                text = { Text("Rename", fontSize = 14.sp) },
                onClick = { menu = false; actions.onRename(node) },
            )
            DropdownMenuItem(
                text = { Text("Move to...", fontSize = 14.sp) },
                onClick = { menu = false; actions.onMove(node) },
            )
            if (!node.isFolder) {
                DropdownMenuItem(
                    text = {
                        Text(
                            if (isLocked) "Unlock" else "Lock (prevent delete)",
                            fontSize = 14.sp,
                        )
                    },
                    onClick = { menu = false; actions.onToggleLock(node) },
                )
            }
            DropdownMenuItem(
                text = { Text("Select", fontSize = 14.sp) },
                onClick = { menu = false; actions.onStartSelection(node) },
            )
            DropdownMenuItem(
                text = { Text("Delete", fontSize = 14.sp, color = Slate.Danger) },
                onClick = { menu = false; actions.onDelete(node) },
            )
        }
    }
}
