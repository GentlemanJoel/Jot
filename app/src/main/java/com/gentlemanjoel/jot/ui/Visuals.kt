package com.gentlemanjoel.jot.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

/** Maps a stored icon key to its vector. Unknown keys fall back to a plain note. */
fun iconFor(key: String): ImageVector = when (key) {
    "check" -> Icons.Filled.CheckCircle
    "cart" -> Icons.Filled.ShoppingCart
    "star" -> Icons.Filled.Star
    "flag" -> Icons.Filled.Flag
    "bolt" -> Icons.Filled.Bolt
    "heart" -> Icons.Filled.Favorite
    "home" -> Icons.Filled.Home
    "work" -> Icons.Filled.Work
    "idea" -> Icons.Filled.Lightbulb
    "money" -> Icons.Filled.AttachMoney
    "calendar" -> Icons.Filled.CalendarMonth
    else -> Icons.Filled.Description
}

/** Maps a stored color key to its color. Unknown keys fall back to the accent blue. */
fun colorFor(key: String): Color = when (key) {
    "green" -> Color(0xFF4CAF84)
    "amber" -> Color(0xFFD9A14A)
    "red" -> Color(0xFFD9674A)
    "purple" -> Color(0xFF9B7ED9)
    "teal" -> Color(0xFF4AC0D9)
    "slate" -> Slate.Muted
    else -> Slate.Accent
}
