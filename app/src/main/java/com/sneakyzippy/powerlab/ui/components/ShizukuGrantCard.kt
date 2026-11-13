package com.sneakyzippy.powerlab.ui.components

import android.content.pm.PackageManager
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.sneakyzippy.powerlab.core.permission.ShizukuPermissionHelper
import com.sneakyzippy.powerlab.ui.theme.AccentBlue
import com.sneakyzippy.powerlab.ui.theme.DarkBackground
import com.sneakyzippy.powerlab.ui.theme.DarkSurface
import com.sneakyzippy.powerlab.ui.theme.GreenPrimary
import com.sneakyzippy.powerlab.ui.theme.TextPrimary
import com.sneakyzippy.powerlab.ui.theme.TextSecondary
import kotlinx.coroutines.launch
import rikka.shizuku.Shizuku

@Composable
fun ShizukuGrantCard(
    allPermissionsGranted: Boolean,
    onPermissionsGranted: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (allPermissionsGranted) return

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    var isInstalled by remember { mutableStateOf(ShizukuPermissionHelper.isShizukuInstalled(context)) }
    var isRunning by remember { mutableStateOf(ShizukuPermissionHelper.isShizukuRunning()) }
    var isGranting by remember { mutableStateOf(false) }

    fun executeGrant() {
        isGranting = true
        coroutineScope.launch {
            val result = ShizukuPermissionHelper.grantAllPermissions(context)
            isGranting = false
            result.onSuccess {
                Toast.makeText(context, "All permissions granted via Shizuku!", Toast.LENGTH_SHORT).show()
                onPermissionsGranted()
            }.onFailure { err ->
                Toast.makeText(context, "Shizuku grant failed: ${err.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isInstalled = ShizukuPermissionHelper.isShizukuInstalled(context)
                isRunning = ShizukuPermissionHelper.isShizukuRunning()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        val binderReceivedListener = Shizuku.OnBinderReceivedListener {
            isRunning = true
        }
        val binderDeadListener = Shizuku.OnBinderDeadListener {
            isRunning = false
        }
        val permissionResultListener = Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
            if (requestCode == ShizukuPermissionHelper.SHIZUKU_PERMISSION_REQUEST_CODE) {
                if (grantResult == PackageManager.PERMISSION_GRANTED) {
                    executeGrant()
                } else {
                    Toast.makeText(context, "Shizuku permission denied.", Toast.LENGTH_SHORT).show()
                }
            }
        }

        Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
        Shizuku.addBinderDeadListener(binderDeadListener)
        Shizuku.addRequestPermissionResultListener(permissionResultListener)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            Shizuku.removeBinderReceivedListener(binderReceivedListener)
            Shizuku.removeBinderDeadListener(binderDeadListener)
            Shizuku.removeRequestPermissionResultListener(permissionResultListener)
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Bolt, contentDescription = null, tint = AccentBlue)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "1-Tap Grant via Shizuku (No PC)",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            when {
                isRunning -> {
                    Text(
                        text = "Shizuku is active! Tap below to grant all elevated permissions directly on your phone.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        enabled = !isGranting,
                        onClick = {
                            if (ShizukuPermissionHelper.hasShizukuPermission()) {
                                executeGrant()
                            } else {
                                ShizukuPermissionHelper.requestShizukuPermission()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        if (isGranting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = DarkBackground
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Granting...", color = DarkBackground, fontWeight = FontWeight.Bold)
                        } else {
                            Text("Grant All Permissions via Shizuku", color = DarkBackground, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                isInstalled -> {
                    Text(
                        text = "Shizuku is installed, but its service isn't running. Open Shizuku and start it via Wireless Debugging, then return here.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = {
                            if (!ShizukuPermissionHelper.openShizukuApp(context)) {
                                Toast.makeText(context, "Could not launch Shizuku.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Open Shizuku", color = AccentBlue, fontWeight = FontWeight.SemiBold)
                    }
                }

                else -> {
                    Text(
                        text = "Want to grant ADB permissions directly on your phone without a PC? Install Shizuku and start it via Android Wireless Debugging.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { ShizukuPermissionHelper.openUrl(context, ShizukuPermissionHelper.SHIZUKU_PLAY_STORE_URL) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(5.dp))
                            Text("Play Store", color = AccentBlue, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                        OutlinedButton(
                            onClick = { ShizukuPermissionHelper.openUrl(context, ShizukuPermissionHelper.SHIZUKU_GITHUB_URL) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(5.dp))
                            Text("GitHub", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}
