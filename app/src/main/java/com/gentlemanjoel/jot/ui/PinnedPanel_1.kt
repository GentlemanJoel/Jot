package com.gentlemanjoel.jot.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PushPin
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gentlemanjoel.jot.PanelWidth
import com.gentlemanjoel.jot.Pin
import com.gentlemanjoel.jot.Prefs

/**
 * The fixed panel of pinned notes on the home screen.
 *
 * Normal mode is one-tap access. The pencil toggles edit mode, where tapping a
 * pin opens its editor - that keeps every control reachable even when the
 * panel is only wide enough for the icon itself.
 *
 * [bottomReserve] is the vertical space the floating new-note button occupies
 * over this panel, kept clear so no pin can hide behind it.
 */
@Composable
fun PinnedPanel(
    pins: List<Pin>,
    width: PanelWidth,
    bottomReserve: androidx.compose.ui.unit.Dp,
    onOpen: (String) -> Unit,
    onMove: (String, Int) -> Unit,
    onSetIcon: (String, String) -> Unit,
    onSetColor: (String, String) -> Unit,
    onUnpin: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var editing by remember { mutableStateOf(false) }
    var editingPin by remember { mutableStateOf<String?>(null) }
    var iconPickerFor by remember { mutableStateOf<String?>(null) }
    var colorPickerFor by remember { mutableStateOf<String?>(null) }

    val compact = width.iconsOnly
    val listState = rememberLazyListState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Slate.Secondary)
    ) {
        // ---------------- header ----------------
        if (compact) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                IconButton(onClick = { editing = !editing }, modifier = Modifier.size(32.dp)) {
                    Icon(
                        if (editing) Icons.Filled.Check else Icons.Filled.Edit,
                        if (editing) "Done editing" else "Edit pins",
                        tint = if (editing) Slate.Accent else Slate.Muted,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 4.dp, top = 12.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Filled.PushPin, null,
                    tint = Slate.Sun,
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
        }

        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Slate.Divider)
        )

        // ---------------- list ----------------
        if (pins.isEmpty()) {
            if (!compact) {
                Text(
                    "Open a note and tap the pin to add it here.",
                    color = Slate.Muted,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 16.dp),
                )
            } else {
                Icon(
                    Icons.Filled.PushPin, null,
                    tint = Slate.Divider,
                    modifier = Modifier
                        .padding(top = 18.dp)
                        .align(Alignment.CenterHorizontally)
                        .size(18.dp),
                )
            }
        } else {
            Box(Modifier.fillMaxSize()) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    // Keeps the last pin clear of the floating new-note button.
                    contentPadding = PaddingValues(bottom = bottomReserve),
                ) {
                    items(pins, key = { it.path }) { pin ->
                        PinRow(
                            pin = pin,
                            compact = compact,
                            panelWidthDp = width.widthDp,
                            editing = editing,
                            onClick = {
                                if (editing) editingPin = pin.path else onOpen(pin.path)
                            },
                        )
                    }
                }

                // Subtle fade telling you there is more below.
                if (listState.canScrollForward) {
                    Box(
                        Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(28.dp)
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Transparent, Slate.Secondary)
                                )
                            )
                    )
                }
            }
        }
    }

    // ---------------- per-pin editor ----------------
    editingPin?.let { path ->
        val pin = pins.firstOrNull { it.path == path }
        if (pin == null) {
            editingPin = null
        } else {
            PinEditorDialog(
                pin = pin,
                isFirst = pins.first().path == path,
                isLast = pins.last().path == path,
                onUp = { onMove(path, -1) },
                onDown = { onMove(path, 1) },
                onIcon = { iconPickerFor = path },
                onColor = { colorPickerFor = path },
                onUnpin = { onUnpin(path); editingPin = null },
                onDismiss = { editingPin = null },
            )
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
    compact: Boolean,
    panelWidthDp: Int,
    editing: Boolean,
    onClick: () -> Unit,
) {
    val tint = colorFor(pin.color)

    if (compact) {
        // The tile grows with the panel so a 48dp strip and a 104dp strip
        // both look deliberate rather than stretched.
        val box = (panelWidthDp - 14).coerceIn(30, 46)
        val glyph = (box * 0.55f).coerceIn(16f, 26f)

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .clickable { onClick() }
                    .padding(vertical = 7.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier
                        .size(box.dp)
                        .clip(RoundedCornerShape((box * 0.28f).dp))
                        .background(tint.copy(alpha = if (editing) 0.30f else 0.16f))
                        .then(
                            if (editing) Modifier.border(
                                1.dp, tint, RoundedCornerShape((box * 0.28f).dp)
                            ) else Modifier
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        iconFor(pin.icon), pin.displayName,
                        tint = tint,
                        modifier = Modifier.size(glyph.dp),
                    )
                }
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Slate.Divider.copy(alpha = 0.4f))
            )
        }
    } else {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onClick() }
                    .padding(start = 12.dp, end = 10.dp, top = 12.dp, bottom = 12.dp)
            ) {
                Box(
                    Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(tint.copy(alpha = if (editing) 0.30f else 0.16f)),
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
                    Icon(
                        Icons.Filled.Edit, "Edit pin",
                        tint = Slate.Muted,
                        modifier = Modifier.size(15.dp),
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
}

/** Everything you can do to one pin, in a dialog so it works at any panel width. */
@Composable
private fun PinEditorDialog(
    pin: Pin,
    isFirst: Boolean,
    isLast: Boolean,
    onUp: () -> Unit,
    onDown: () -> Unit,
    onIcon: () -> Unit,
    onColor: () -> Unit,
    onUnpin: () -> Unit,
    onDismiss: () -> Unit,
) {
    val tint = colorFor(pin.color)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Slate.Secondary,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(tint.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(iconFor(pin.icon), null, tint = tint, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(10.dp))
                Text(pin.displayName, color = Slate.Text, fontSize = 16.sp, maxLines = 2)
            }
        },
        text = {
            Column {
                Text("ORDER", color = Slate.Muted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Row(Modifier.padding(top = 6.dp, bottom = 14.dp)) {
                    TextButton(onClick = onUp, enabled = !isFirst) {
                        Icon(
                            Icons.Filled.KeyboardArrowUp, null,
                            tint = if (isFirst) Slate.Divider else Slate.Accent,
                            modifier = Modifier.size(19.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Up", color = if (isFirst) Slate.Divider else Slate.Accent, fontSize = 14.sp)
                    }
                    Spacer(Modifier.width(6.dp))
                    TextButton(onClick = onDown, enabled = !isLast) {
                        Icon(
                            Icons.Filled.KeyboardArrowDown, null,
                            tint = if (isLast) Slate.Divider else Slate.Accent,
                            modifier = Modifier.size(19.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Down", color = if (isLast) Slate.Divider else Slate.Accent, fontSize = 14.sp)
                    }
                }

                Text("APPEARANCE", color = Slate.Muted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Row(Modifier.padding(top = 6.dp, bottom = 10.dp)) {
                    TextButton(onClick = onIcon) {
                        Icon(iconFor(pin.icon), null, tint = Slate.Text, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Icon", color = Slate.Text, fontSize = 14.sp)
                    }
                    Spacer(Modifier.width(6.dp))
                    TextButton(onClick = onColor) {
                        Icon(Icons.Filled.Palette, null, tint = tint, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Color", color = Slate.Text, fontSize = 14.sp)
                    }
                }

                TextButton(onClick = onUnpin) {
                    Icon(Icons.Filled.Close, null, tint = Slate.Danger, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Unpin this note", color = Slate.Danger, fontSize = 14.sp)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done", color = Slate.Accent) }
        },
    )
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

/** The whole icon library, grouped by category and scrollable. */
@Composable
private fun GridOfIcons(
    selected: String,
    color: Color,
    onPick: (String) -> Unit,
) {
    Column(
        Modifier
            .heightIn(max = 420.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Prefs.ICON_GROUPS.forEach { group ->
            Text(
                group.label.uppercase(),
                color = Slate.Muted,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 10.dp, bottom = 2.dp),
            )
            group.keys.chunked(5).forEach { row ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    row.forEach { key ->
                        val isSel = key == selected
                        Box(
                            Modifier
                                .weight(1f)
                                .height(46.dp)
                                .clip(RoundedCornerShape(11.dp))
                                .background(if (isSel) color.copy(alpha = 0.22f) else Slate.Background)
                                .then(
                                    if (isSel) Modifier.border(1.5.dp, color, RoundedCornerShape(11.dp))
                                    else Modifier
                                )
                                .clickable { onPick(key) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                iconFor(key), key,
                                tint = if (isSel) color else Slate.Muted,
                                modifier = Modifier.size(21.dp),
                            )
                        }
                    }
                    repeat(5 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

/** The full color palette, five to a row. */
@Composable
private fun GridOfColors(
    selected: String,
    onPick: (String) -> Unit,
) {
    Column(
        Modifier
            .heightIn(max = 420.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Prefs.COLOR_KEYS.chunked(5).forEach { row ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                row.forEach { key ->
                    val c = colorFor(key)
                    val isSel = key == selected
                    Column(
                        Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            Modifier
                                .size(42.dp)
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
                                    modifier = Modifier.size(19.dp),
                                )
                            }
                        }
                        Text(
                            key,
                            color = if (isSel) Slate.Text else Slate.Muted,
                            fontSize = 10.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 3.dp),
                        )
                    }
                }
                repeat(5 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}
