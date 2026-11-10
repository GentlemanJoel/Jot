package com.gentlemanjoel.jot.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.gentlemanjoel.jot.Prefs
import com.gentlemanjoel.jot.TrashItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Deleted notes, newest first, with the folder each came from. Everything
 * here is still a real file under `.trash`, so Syncthing has carried it to
 * the PC too and nothing is gone until it is purged.
 */
@Composable
fun TrashScreen(
    items: List<TrashItem>,
    onBack: () -> Unit,
    onRestore: (TrashItem) -> Unit,
    onDeleteForever: (TrashItem) -> Unit,
    onEmpty: () -> Unit,
) {
    var confirmEmpty by remember { mutableStateOf(false) }
    var confirmOne by remember { mutableStateOf<TrashItem?>(null) }

    Column(
        Modifier
            .fillMaxSize()
            .background(Slate.Background)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .background(Slate.Secondary)
                .statusBarsPadding()
                .padding(start = 4.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(44.dp)) {
                Icon(
                    Icons.Filled.ArrowBack, "Back",
                    tint = Slate.Text,
                    modifier = Modifier.size(22.dp),
                )
            }
            Text("Trash", color = Slate.Text, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            if (items.isNotEmpty()) {
                TextButton(onClick = { confirmEmpty = true }) {
                    Text("Empty", color = Slate.Danger, fontSize = 14.sp)
                }
            }
        }

        Text(
            "Deleted notes are kept for ${Prefs.TRASH_KEEP_DAYS} days, then removed for good.",
            color = Slate.Muted,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
        )

        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Slate.Divider)
        )

        if (items.isEmpty()) {
            Column(
                Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    Icons.Filled.Delete, null,
                    tint = Slate.Divider,
                    modifier = Modifier.size(34.dp),
                )
                Text(
                    "The trash is empty.",
                    color = Slate.Muted,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 10.dp),
                )
            }
        } else {
            LazyColumn(
                Modifier
                    .fillMaxSize()
                    .navigationBarsPadding(),
                contentPadding = PaddingValues(bottom = 24.dp),
            ) {
                items(items, key = { it.file.absolutePath }) { item ->
                    TrashRow(
                        item = item,
                        onRestore = { onRestore(item) },
                        onDelete = { confirmOne = item },
                    )
                }
            }
        }
    }

    if (confirmEmpty) {
        AlertDialog(
            onDismissRequest = { confirmEmpty = false },
            containerColor = Slate.Secondary,
            title = { Text("Empty the trash?", color = Slate.Text, fontSize = 17.sp) },
            text = {
                Text(
                    "All ${items.size} " +
                        (if (items.size == 1) "item is" else "items are") +
                        " removed for good. This cannot be undone.",
                    color = Slate.Muted, fontSize = 14.sp,
                )
            },
            confirmButton = {
                TextButton(onClick = { confirmEmpty = false; onEmpty() }) {
                    Text("Delete everything", color = Slate.Danger)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmEmpty = false }) {
                    Text("Cancel", color = Slate.Muted)
                }
            },
        )
    }

    confirmOne?.let { item ->
        AlertDialog(
            onDismissRequest = { confirmOne = null },
            containerColor = Slate.Secondary,
            title = { Text("Delete for good?", color = Slate.Text, fontSize = 17.sp) },
            text = {
                Text(
                    "\"${item.displayName}\" cannot be recovered after this.",
                    color = Slate.Muted, fontSize = 14.sp,
                )
            },
            confirmButton = {
                TextButton(onClick = { onDeleteForever(item); confirmOne = null }) {
                    Text("Delete for good", color = Slate.Danger)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmOne = null }) {
                    Text("Cancel", color = Slate.Muted)
                }
            },
        )
    }
}

@Composable
private fun TrashRow(
    item: TrashItem,
    onRestore: () -> Unit,
    onDelete: () -> Unit,
) {
    val stamp = remember(item.deletedAt) {
        SimpleDateFormat("MMM d, h:mm a", Locale.US).format(Date(item.deletedAt))
    }
    val from = item.originalParent.ifBlank { "Top level" }

    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 6.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                if (item.isFolder) Icons.Filled.Folder else Icons.Filled.Description,
                null,
                tint = if (item.isFolder) Slate.Sun else Slate.Muted,
                modifier = Modifier.size(17.dp),
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    item.displayName,
                    color = Slate.Text,
                    fontSize = 14.sp,
                    maxLines = 1,
                )
                Text(
                    "$from · $stamp",
                    color = Slate.Muted,
                    fontSize = 11.sp,
                    maxLines = 1,
                )
            }
            IconButton(onClick = onRestore, modifier = Modifier.size(40.dp)) {
                Icon(
                    Icons.Filled.Restore, "Restore ${item.displayName}",
                    tint = Slate.Accent,
                    modifier = Modifier.size(19.dp),
                )
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(40.dp)) {
                Icon(
                    Icons.Filled.DeleteForever, "Delete ${item.displayName} for good",
                    tint = Slate.Danger,
                    modifier = Modifier.size(19.dp),
                )
            }
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Slate.Divider.copy(alpha = 0.5f))
        )
    }
}
