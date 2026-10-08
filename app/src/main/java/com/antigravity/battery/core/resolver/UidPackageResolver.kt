package com.antigravity.battery.core.resolver

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable

data class ResolvedAppInfo(
    val uid: Int,
    val primaryPackageName: String,
    val appLabel: String,
    val sharedPackageNames: List<String>,
    val isSystemApp: Boolean,
    val icon: Drawable? = null
)

/**
 * Resolves raw Linux UIDs into user-friendly app names, package identifiers, and icons.
 * Handles shared UIDs and system services cleanly.
 */
open class UidPackageResolver(private val context: Context? = null) {

    private val packageManager: PackageManager? = context?.packageManager
    private val appInfoCache = LinkedHashMap<Int, ResolvedAppInfo>(128)

    open fun resolve(uid: Int): ResolvedAppInfo {
        appInfoCache[uid]?.let { return it }

        val resolved = when (uid) {
            0 -> ResolvedAppInfo(
                uid = 0,
                primaryPackageName = "android.kernel",
                appLabel = "Linux Kernel",
                sharedPackageNames = emptyList(),
                isSystemApp = true
            )
            1000 -> {
                val packages = packageManager?.getPackagesForUid(1000)?.toList() ?: listOf("android")
                ResolvedAppInfo(
                    uid = 1000,
                    primaryPackageName = "android",
                    appLabel = "Android System",
                    sharedPackageNames = packages,
                    isSystemApp = true,
                    icon = getIconOrNull("android")
                )
            }
            1073 -> ResolvedAppInfo(
                uid = 1073,
                primaryPackageName = "media",
                appLabel = "Media Server / AudioMix",
                sharedPackageNames = emptyList(),
                isSystemApp = true
            )
            else -> {
                val packages = packageManager?.getPackagesForUid(uid)?.toList() ?: emptyList()
                if (packages.isEmpty()) {
                    ResolvedAppInfo(
                        uid = uid,
                        primaryPackageName = "uid.$uid",
                        appLabel = "App (UID $uid)",
                        sharedPackageNames = emptyList(),
                        isSystemApp = uid < 10000
                    )
                } else {
                    val primaryPkg = pickPrimaryPackage(packages)
                    var label = primaryPkg
                    var isSystem = false
                    var icon: Drawable? = null

                    try {
                        val appInfo = packageManager?.getApplicationInfo(primaryPkg, 0)
                        if (appInfo != null && packageManager != null) {
                            label = packageManager.getApplicationLabel(appInfo).toString()
                            isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                            icon = packageManager.getApplicationIcon(appInfo)
                        }
                    } catch (_: Exception) {
                        // Keep fallback label
                    }

                    // Special naming for Google Play Services shared UID
                    if (packages.contains("com.google.android.gms")) {
                        label = "Google Play Services"
                    }

                    ResolvedAppInfo(
                        uid = uid,
                        primaryPackageName = primaryPkg,
                        appLabel = label,
                        sharedPackageNames = packages,
                        isSystemApp = isSystem || uid < 10000,
                        icon = icon
                    )
                }
            }
        }

        appInfoCache[uid] = resolved
        return resolved
    }

    private fun pickPrimaryPackage(packages: List<String>): String {
        return packages.firstOrNull { it == "com.google.android.gms" }
            ?: packages.firstOrNull { !it.startsWith("com.android.providers.") }
            ?: packages.first()
    }

    private fun getIconOrNull(packageName: String): Drawable? {
        return try {
            packageManager?.getApplicationIcon(packageName)
        } catch (_: Exception) {
            null
        }
    }
}
