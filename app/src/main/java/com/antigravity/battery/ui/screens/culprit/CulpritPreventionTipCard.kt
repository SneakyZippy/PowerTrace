package com.antigravity.battery.ui.screens.culprit

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antigravity.battery.core.model.CulpritApp
import com.antigravity.battery.ui.theme.AccentBlue
import com.antigravity.battery.ui.theme.DarkSurface
import com.antigravity.battery.ui.theme.TextPrimary
import com.antigravity.battery.ui.theme.TextSecondary

@Composable
fun CulpritPreventionTipCard(
    culprit: CulpritApp,
    modifier: Modifier = Modifier
) {
    if (culprit.isSystemApp || culprit.primaryPackageName.isEmpty() || culprit.primaryPackageName.startsWith("android.kernel")) {
        return
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = AccentBlue,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "How To Prevent Drain From This App",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            val tips = mutableListOf<String>()
            if (culprit.wakeupAlarmCount >= 5) {
                tips.add("• Frequent Alarm Wakeups (${culprit.wakeupAlarmCount}x): Revoke 'Alarms & Reminders' permission in Android Settings or turn off in-app notification/sync polls.")
            }
            if (culprit.partialWakelockMs >= 15_000L) {
                tips.add("• Background Wakelock (${culprit.partialWakelockMs / 1000}s): Switch App Battery usage from 'Optimized' to 'Restricted' (or add to Deep Sleeping Apps).")
            }
            if (culprit.mobileRadioActiveMs >= 30_000L || culprit.mobileBytesRx + culprit.mobileBytesTx > 2 * 1024 * 1024) {
                tips.add("• Mobile Data Activity: Disable 'Background data' in Mobile Data usage settings to prevent syncing when screen is off.")
            }
            if (tips.isEmpty()) {
                tips.add("• Standby Activity: If this app should not run in background, restrict its battery profile in App Info.")
            }
            tips.forEach { tip ->
                Text(
                    text = tip,
                    fontSize = 12.sp,
                    color = TextSecondary,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }
    }
}
