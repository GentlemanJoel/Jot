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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gentlemanjoel.jot.BuildConfig
import com.gentlemanjoel.jot.FabSize
import com.gentlemanjoel.jot.PanelWidth

@Composable
fun SettingsScreen(
    panelWidth: PanelWidth,
    onPanelWidth: (PanelWidth) -> Unit,
    fabSize: FabSize,
    onFabSize: (FabSize) -> Unit,
    token: String,
    onToken: (String) -> Unit,
    notesPath: String,
    onBack: () -> Unit,
    onCheckUpdate: () -> Unit,
) {
    var tokenField by remember { mutableStateOf(token) }
    var savedNote by remember { mutableStateOf(false) }
    var widthMenu by remember { mutableStateOf(false) }

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
                .padding(start = 4.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(44.dp)) {
                Icon(
                    Icons.Filled.ArrowBack, "Back",
                    tint = Slate.Text,
                    modifier = Modifier.size(23.dp),
                )
            }
            Spacer(Modifier.width(4.dp))
            Text("Settings", color = Slate.Text, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
        }

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 18.dp, vertical = 16.dp)
        ) {
            SectionLabel("Pinned panel width")
            Text(
                "Anything under ${PanelWidth.LABEL_FLOOR}dp drops the note names and shows just the icons.",
                color = Slate.Muted,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 6.dp, bottom = 8.dp),
            )

            Box {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Slate.Secondary)
                        .border(1.dp, Slate.Divider, RoundedCornerShape(12.dp))
                        .clickable { widthMenu = true }
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(panelWidth.label, color = Slate.Text, fontSize = 14.sp)
                    Spacer(Modifier.weight(1f))
                    Text(
                        "${panelWidth.widthDp}dp",
                        color = Slate.Muted,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(end = 8.dp),
                    )
                    Icon(
                        Icons.Filled.ArrowDropDown, "Change width",
                        tint = Slate.Accent,
                        modifier = Modifier.size(24.dp),
                    )
                }

                DropdownMenu(
                    expanded = widthMenu,
                    onDismissRequest = { widthMenu = false },
                ) {
                    PanelWidth.entries.forEach { w ->
                        val selected = w == panelWidth
                        DropdownMenuItem(
                            onClick = { onPanelWidth(w); widthMenu = false },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        w.label,
                                        color = if (selected) Slate.Accent else Slate.Text,
                                        fontSize = 14.sp,
                                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Text("${w.widthDp}dp", color = Slate.Muted, fontSize = 12.sp)
                                }
                            },
                        )
                    }
                }
            }

            Spacer(Modifier.height(22.dp))

            SectionLabel("New note button size")
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                FabSize.entries.forEach { s ->
                    val selected = s == fabSize
                    Column(
                        Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (selected) Slate.Accent.copy(alpha = 0.18f) else Slate.Secondary
                                )
                                .then(
                                    if (selected) Modifier.border(
                                        1.5.dp, Slate.Accent, RoundedCornerShape(12.dp)
                                    ) else Modifier
                                )
                                .clickable { onFabSize(s) },
                            contentAlignment = Alignment.Center,
                        ) {
                            // A dot scaled to the real button size, so the
                            // relative difference is visible at a glance.
                            Box(
                                Modifier
                                    .size((s.sizeDp * 0.36f).dp)
                                    .clip(androidx.compose.foundation.shape.CircleShape)
                                    .background(if (selected) Slate.Accent else Slate.Muted)
                            )
                        }
                        Text(
                            s.label,
                            color = if (selected) Slate.Accent else Slate.Muted,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 5.dp),
                        )
                    }
                }
            }

            Spacer(Modifier.height(22.dp))

            SectionLabel("Notes folder")
            Text(
                notesPath,
                color = Slate.Text,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 6.dp),
            )
            Text(
                "Point Syncthing at this folder to sync with your PC. Files are plain .md, so Obsidian opens them directly.",
                color = Slate.Muted,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 6.dp, bottom = 20.dp),
            )

            SectionLabel("GitHub token")
            Text(
                "Required because the Jot repo is private. Needs Contents: Read-only on the Jot repo.",
                color = Slate.Muted,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 6.dp, bottom = 8.dp),
            )
            OutlinedTextField(
                value = tokenField,
                onValueChange = { tokenField = it; savedNote = false },
                singleLine = true,
                placeholder = { Text("github_pat_...", fontSize = 13.sp, color = Slate.Muted) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Slate.Accent,
                    unfocusedBorderColor = Slate.Divider,
                    focusedTextColor = Slate.Text,
                    unfocusedTextColor = Slate.Text,
                    cursorColor = Slate.Accent,
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                Modifier.padding(top = 10.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(
                    onClick = { onToken(tokenField); savedNote = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Slate.Accent),
                    shape = RoundedCornerShape(12.dp),
                ) { Text("Save token", fontSize = 14.sp, color = Slate.Secondary) }

                if (savedNote) {
                    Spacer(Modifier.width(12.dp))
                    Text("Saved", color = Slate.Accent, fontSize = 13.sp)
                }
            }

            Spacer(Modifier.height(24.dp))

            SectionLabel("Updates")
            Button(
                onClick = onCheckUpdate,
                colors = ButtonDefaults.buttonColors(containerColor = Slate.Secondary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.padding(top = 8.dp),
            ) { Text("Check for updates", fontSize = 14.sp, color = Slate.Text) }

            Spacer(Modifier.height(28.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Slate.Divider)
            )
            Text(
                "Jot v${BuildConfig.VERSION_NAME}",
                color = Slate.Muted,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 14.dp, bottom = 30.dp),
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text.uppercase(),
        color = Slate.Muted,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
    )
}
