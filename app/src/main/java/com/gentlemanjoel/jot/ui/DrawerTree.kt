package com.gentlemanjoel.jot.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gentlemanjoel.jot.NoteNode
import com.gentlemanjoel.jot.NoteStore
import java.io.File

@Composable
fun DrawerTree(
    tree: List<NoteNode>,
    openFile: File?,
    expanded: MutableSet<String>,
    onToggleFolder: (String) -> Unit,
    onOpenNote: (File) -> Unit,
    onNewNoteIn: (File) -> Unit,
    onNewFolderIn: (File) -> Unit,
    onRename: (NoteNode) -> Unit,
    onDelete: (NoteNode) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val results = remember(query, tree) { NoteStore.search(tree, query) }

    Column(
        Modifier
            .fillMaxSize()
            .background(Slate.Secondary)
            .padding(top = 36.dp)
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            singleLine = true,
            placeholder = { Text("Search notes", color = Slate.Muted, fontSize = 14.sp) },
            leadingIcon = {
                Icon(Icons.Filled.Search, null, tint = Slate.Muted, modifier = Modifier.size(18.dp))
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
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
        )

        Spacer(Modifier.height(6.dp))

        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { onNewNoteIn(NoteStore.ensureRoot()) }, modifier = Modifier.size(34.dp)) {
                Icon(Icons.Filled.Add, "New note", tint = Slate.Accent, modifier = Modifier.size(19.dp))
            }
            IconButton(onClick = { onNewFolderIn(NoteStore.ensureRoot()) }, modifier = Modifier.size(34.dp)) {
                Icon(Icons.Filled.CreateNewFolder, "New folder", tint = Slate.Accent, modifier = Modifier.size(19.dp))
            }
        }

        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Slate.Divider)
        )

        if (query.isNotBlank()) {
            LazyColumn(Modifier.fillMaxSize()) {
                items(results, key = { it.file.absolutePath }) { node ->
                    TreeRow(
                        node = node,
                        depth = 0,
                        isOpen = node.file == openFile,
                        isExpanded = false,
                        onClick = { onOpenNote(node.file) },
                        onNewNoteIn = onNewNoteIn,
                        onNewFolderIn = onNewFolderIn,
                        onRename = onRename,
                        onDelete = onDelete,
                    )
                }
                if (results.isEmpty()) {
                    items(listOf("empty")) {
                        Text(
                            "No matches",
                            color = Slate.Muted,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                renderTree(
                    nodes = tree,
                    depth = 0,
                    expanded = expanded,
                    openFile = openFile,
                    onToggleFolder = onToggleFolder,
                    onOpenNote = onOpenNote,
                    onNewNoteIn = onNewNoteIn,
                    onNewFolderIn = onNewFolderIn,
                    onRename = onRename,
                    onDelete = onDelete,
                )
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.renderTree(
    nodes: List<NoteNode>,
    depth: Int,
    expanded: MutableSet<String>,
    openFile: File?,
    onToggleFolder: (String) -> Unit,
    onOpenNote: (File) -> Unit,
    onNewNoteIn: (File) -> Unit,
    onNewFolderIn: (File) -> Unit,
    onRename: (NoteNode) -> Unit,
    onDelete: (NoteNode) -> Unit,
) {
    nodes.forEach { node ->
        val path = node.file.absolutePath
        val isExpanded = expanded.contains(path)
        item(key = path) {
            TreeRow(
                node = node,
                depth = depth,
                isOpen = node.file == openFile,
                isExpanded = isExpanded,
                onClick = {
                    if (node.isFolder) onToggleFolder(path) else onOpenNote(node.file)
                },
                onNewNoteIn = onNewNoteIn,
                onNewFolderIn = onNewFolderIn,
                onRename = onRename,
                onDelete = onDelete,
            )
        }
        if (node.isFolder && isExpanded) {
            renderTree(
                node.children, depth + 1, expanded, openFile,
                onToggleFolder, onOpenNote, onNewNoteIn, onNewFolderIn, onRename, onDelete,
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
    onClick: () -> Unit,
    onNewNoteIn: (File) -> Unit,
    onNewFolderIn: (File) -> Unit,
    onRename: (NoteNode) -> Unit,
    onDelete: (NoteNode) -> Unit,
) {
    var menu by remember { mutableStateOf(false) }

    Box {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .background(if (isOpen) Slate.Background else Slate.Secondary)
                .combinedClickable(onClick = onClick, onLongClick = { menu = true })
                .padding(
                    start = (10 + depth * 14).dp,
                    end = 10.dp,
                    top = 9.dp,
                    bottom = 9.dp
                )
        ) {
            Icon(
                imageVector = when {
                    node.isFolder && isExpanded -> Icons.Filled.FolderOpen
                    node.isFolder -> Icons.Filled.Folder
                    else -> Icons.Filled.Description
                },
                contentDescription = null,
                tint = if (node.isFolder) Slate.Accent else Slate.Muted,
                modifier = Modifier.size(17.dp),
            )
            Spacer(Modifier.width(9.dp))
            Text(
                text = node.displayName,
                color = if (isOpen) Slate.Accent else Slate.Text,
                fontSize = 14.sp,
                fontWeight = if (isOpen) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
            )
        }

        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            if (node.isFolder) {
                DropdownMenuItem(
                    text = { Text("New note here", fontSize = 14.sp) },
                    onClick = { menu = false; onNewNoteIn(node.file) },
                )
                DropdownMenuItem(
                    text = { Text("New subfolder", fontSize = 14.sp) },
                    onClick = { menu = false; onNewFolderIn(node.file) },
                )
            }
            DropdownMenuItem(
                text = { Text("Rename", fontSize = 14.sp) },
                onClick = { menu = false; onRename(node) },
            )
            DropdownMenuItem(
                text = { Text("Delete", fontSize = 14.sp, color = Slate.Danger) },
                onClick = { menu = false; onDelete(node) },
            )
        }
    }
}
