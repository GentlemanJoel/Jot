package com.gentlemanjoel.jot.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gentlemanjoel.jot.Pin
import com.gentlemanjoel.jot.Prefs

/**
 * The fixed panel of pinned notes on the home screen.
 *
 * Normal mode is one-tap access. The pencil toggles edit mode, which reveals
 * reorder arrows plus icon, color and unpin controls for each entry.
 */
@Composable
fun PinnedPanel(
    pins: List<Pin>,
    onOpen: (String) -> Unit,
    onMove: (String, Int) -> Unit,
    onSetIcon: (String, String) -> Unit,
    onSetColor: (String, String) -> Unit,
    onUnpin: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var editing by remember { mutableStateOf(false) }
    var iconPickerFor by remember { mutableStateOf<String?>(null) }
    var colorPickerFor by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Slate.Secondary)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 4.dp, top = 12.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Filled.PushPin, null,
                tint = Slate.Muted,
                modifier = Modifier.size(15.dp),
            )
            Spacer(Modifier.width(7.dp))
            Text(
                "PINNED",
                color = Slate.Muted,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { editing = !editing }, modifier = Modifier.size(34.dp)) {
                Icon(
                    if (editing) Icons.Filled.Check else Icons.Filled.Edit,
                    if (editing) "Done editing" else "Edit pins",
                    tint = if (editing) Slate.Accent else Slate.Muted,
                    modifier = Modifier.size(17.dp),
                )
            }
        }

        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Slate.Divider)
        )

        if (pins.isEmpty()) {
            Text(
                "Open a note and tap the pin icon to add it here.",
                color = Slate.Muted,
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 16.dp),
            )
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                items(pins, key = { it.path }) { pin ->
                    PinRow(
                        pin = pin,
                        editing = editing,
                        isFirst = pin.path == pins.first().path,
                        isLast = pin.path == pins.last().path,
                        onOpen = { onOpen(pin.path) },
                        onUp = { onMove(pin.path, -1) },
                        onDown = { onMove(pin.path, 1) },
                        onIcon = { iconPickerFor = pin.path },
                        onColor = { colorPickerFor = pin.path },
                        onUnpin = { onUnpin(pin.path) },
                    )
                }
            }
        }
    }

    iconPickerFor?.let { path ->
        PickerDialog(
            title = "Choose an icon",
            onDismiss = { iconPickerFor = null },
        ) {
            GridOfIcons(
                selected = pins.firstOrNull { it.path == path }?.icon ?: "note",
                color = colorFor(pins.firstOrNull { it.path == path }?.color ?: "blue"),
                onPick = { onSetIcon(path, it); iconPickerFor = null },
            )
        }
    }

    colorPickerFor?.let { path ->
        PickerDialog(
            title = "Choose a color",
            onDismiss = { colorPickerFor = null },
        ) {
            GridOfColors(
                selected = pins.firstOrNull { it.path == path }?.color ?: "blue",
                onPick = { onSetColor(path, it); colorPickerFor = null },
            )
        }
    }
}

@Composable
private fun PinRow(
    pin: Pin,
    editing: Boolean,
    isFirst: Boolean,
    isLast: Boolean,
    onOpen: () -> Unit,
    onUp: () -> Unit,
    onDown: () -> Unit,
    onIcon: () -> Unit,
    onColor: () -> Unit,
    onUnpin: () -> Unit,
) {
    val tint = colorFor(pin.color)

    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .then(if (editing) Modifier else Modifier.clickable { onOpen() })
                .padding(start = 12.dp, end = 8.dp, top = 12.dp, bottom = 12.dp)
        ) {
            Box(
                Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(tint.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    iconFor(pin.icon), null,
                    tint = tint,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(Modifier.width(11.dp))
            Text(
                pin.displayName,
                color = Slate.Text,
                fontSize = 14.sp,
                maxLines = 2,
                modifier = Modifier.weight(1f),
            )

            if (editing) {
                IconButton(
                    onClick = onUp,
                    enabled = !isFirst,
                    modifier = Modifier.size(28.dp),
                ) {
                    Icon(
                        Icons.Filled.KeyboardArrowUp, "Move up",
                        tint = if (isFirst) Slate.Divider else Slate.Text,
                        modifier = Modifier.size(19.dp),
                    )
                }
                IconButton(
                    onClick = onDown,
                    enabled = !isLast,
                    modifier = Modifier.size(28.dp),
                ) {
                    Icon(
                        Icons.Filled.KeyboardArrowDown, "Move down",
                        tint = if (isLast) Slate.Divider else Slate.Text,
                        modifier = Modifier.size(19.dp),
                    )
                }
            }
        }

        if (editing) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 8.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onIcon) {
                    Icon(
                        iconFor(pin.icon), null,
                        tint = Slate.Muted,
                        modifier = Modifier.size(15.dp),
                    )
                    Spacer(Modifier.width(5.dp))
                    Text("Icon", color = Slate.Muted, fontSize = 12.sp)
                }
                TextButton(onClick = onColor) {
                    Icon(
                        Icons.Filled.Palette, null,
                        tint = tint,
                        modifier = Modifier.size(15.dp),
                    )
                    Spacer(Modifier.width(5.dp))
                    Text("Color", color = Slate.Muted, fontSize = 12.sp)
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onUnpin, modifier = Modifier.size(30.dp)) {
                    Icon(
                        Icons.Filled.Close, "Unpin",
                        tint = Slate.Danger,
                        modifier = Modifier.size(16.dp),
                    )
                }
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

@Composable
private fun PickerDialog(
    title: String,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Slate.Secondary,
        title = { Text(title, color = Slate.Text, fontSize = 17.sp) },
        text = { content() },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = Slate.Muted) }
        },
    )
}

@Composable
private fun GridOfIcons(
    selected: String,
    color: androidx.compose.ui.graphics.Color,
    onPick: (String) -> Unit,
) {
    Column {
        Prefs.ICON_KEYS.chunked(4).forEach { row ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                row.forEach { key ->
                    val isSel = key == selected
                    Box(
                        Modifier
                            .size(50.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSel) color.copy(alpha = 0.22f) else Slate.Background)
                            .then(
                                if (isSel) Modifier.border(1.5.dp, color, RoundedCornerShape(12.dp))
                                else Modifier
                            )
                            .clickable { onPick(key) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            iconFor(key), key,
                            tint = if (isSel) color else Slate.Muted,
                            modifier = Modifier.size(23.dp),
                        )
                    }
                }
                repeat(4 - row.size) { Box(Modifier.size(50.dp)) }
            }
        }
    }
}

@Composable
private fun GridOfColors(
    selected: String,
    onPick: (String) -> Unit,
) {
    Column {
        Prefs.COLOR_KEYS.chunked(4).forEach { row ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                row.forEach { key ->
                    val c = colorFor(key)
                    val isSel = key == selected
                    Box(
                        Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(c)
                            .then(
                                if (isSel) Modifier.border(2.5.dp, Slate.Text, CircleShape)
                                else Modifier
                            )
                            .clickable { onPick(key) },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (isSel) {
                            Icon(
                                Icons.Filled.Check, "Selected",
                                tint = Slate.Secondary,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
                repeat(4 - row.size) { Box(Modifier.size(46.dp)) }
            }
        }
    }
}
