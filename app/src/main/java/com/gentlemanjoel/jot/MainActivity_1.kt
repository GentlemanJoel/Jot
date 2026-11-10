package com.gentlemanjoel.jot

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gentlemanjoel.jot.ui.JotApp
import com.gentlemanjoel.jot.ui.JotTheme
import com.gentlemanjoel.jot.ui.Slate

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        // Ask the keyguard to step aside so storage is readable.
        val km = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        if (km.isKeyguardLocked) {
            km.requestDismissKeyguard(this, null)
        }

        setContent {
            JotTheme {
                var granted by remember { mutableStateOf(hasStorageAccess()) }
                if (granted) {
                    JotApp()
                } else {
                    PermissionGate(onRecheck = { granted = hasStorageAccess() })
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Returning from the settings screen re-renders through PermissionGate's recheck.
    }

    private fun hasStorageAccess(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            true
        }
}

@Composable
private fun PermissionGate(onRecheck: () -> Unit) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Slate.Background)
            .padding(28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Jot", color = Slate.Text, fontSize = 34.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(18.dp))
        Text(
            "Jot keeps your notes as plain .md files in a normal folder so Syncthing and your PC can read them. That needs all-files access.",
            color = Slate.Muted,
            fontSize = 14.sp,
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                        Uri.parse("package:${context.packageName}"),
                    )
                    context.startActivity(intent)
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = Slate.Accent),
        ) { Text("Grant file access", fontSize = 14.sp) }

        Spacer(Modifier.height(10.dp))
        Button(
            onClick = onRecheck,
            colors = ButtonDefaults.buttonColors(containerColor = Slate.Secondary),
        ) { Text("I've granted it — continue", fontSize = 14.sp, color = Slate.Text) }
    }
}
