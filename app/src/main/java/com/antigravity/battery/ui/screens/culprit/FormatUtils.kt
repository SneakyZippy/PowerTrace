package com.antigravity.battery.ui.screens.culprit

import java.util.Locale

fun formatDataVolume(bytes: Long): String {
    if (bytes <= 0L) return "0 B"
    val kb = bytes / 1024.0
    if (kb < 1024.0) return "${String.format(Locale.US, "%.1f", kb)} KB"
    val mb = kb / 1024.0
    if (mb < 1024.0) return "${String.format(Locale.US, "%.1f", mb)} MB"
    val gb = mb / 1024.0
    return "${String.format(Locale.US, "%.2f", gb)} GB"
}
