package com.example.service

import android.content.Context
import android.content.SharedPreferences

class BlocklistManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("arc_blocklist_prefs", Context.MODE_PRIVATE)

    companion object {
        // Safe default banking, lockscreen, or authentication package patterns to respect user privacy out of the box
        private val DEFAULT_BLOCKLIST = setOf(
            "com.android.settings",
            "com.paypal.android.p2pmobile",
            "com.chase.sig.android",
            "com.capitalone.mobile",
            "com.google.android.apps.authenticator2",
            "com.onepassword.onepassword",
            "com.lastpass.lpandroid",
            "org.keepassdroid",
            "com.binance.dev",
            "com.wallet.crypto.trustapp"
        )
        private const val BLOCKLIST_KEY = "blocked_packages"
    }

    init {
        // Initialize with standard system guidelines if never configured
        if (!prefs.contains(BLOCKLIST_KEY)) {
            prefs.edit().putStringSet(BLOCKLIST_KEY, DEFAULT_BLOCKLIST).apply()
        }
    }

    fun getBlockedPackages(): Set<String> {
        return prefs.getStringSet(BLOCKLIST_KEY, emptySet()) ?: emptySet()
    }

    fun isBlocked(packageName: String?): Boolean {
        if (packageName.isNullOrBlank()) return false
        val blocked = getBlockedPackages()
        // Match exact or sub-package names
        return blocked.contains(packageName) || blocked.any { packageName.startsWith(it) }
    }

    fun addPackage(packageName: String): Boolean {
        val trimmed = packageName.trim()
        if (trimmed.isEmpty()) return false
        val current = getBlockedPackages().toMutableSet()
        val added = current.add(trimmed)
        if (added) {
            prefs.edit().putStringSet(BLOCKLIST_KEY, current).apply()
        }
        return added
    }

    fun removePackage(packageName: String): Boolean {
        val current = getBlockedPackages().toMutableSet()
        val removed = current.remove(packageName.trim())
        if (removed) {
            prefs.edit().putStringSet(BLOCKLIST_KEY, current).apply()
        }
        return removed
    }
}
