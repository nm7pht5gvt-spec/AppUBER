package com.tuapp.tripadvisor.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.text.TextUtils
import com.tuapp.tripadvisor.service.accessibility.RideAccessibilityService

object PermissionHelper {

    fun hasOverlayPermission(context: Context): Boolean {
        return Settings.canDrawOverlays(context)
    }

    fun createOverlayPermissionIntent(context: Context): Intent {
        return Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        )
    }

    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val expectedComponentName = "${context.packageName}/${RideAccessibilityService::class.java.name}"

        val enabledServicesSetting = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        val colonSplitter = TextUtils.SimpleStringSplitter(':')
        colonSplitter.setString(enabledServicesSetting)

        while (colonSplitter.hasNext()) {
            val componentName = colonSplitter.next()
            if (componentName.equals(expectedComponentName, ignoreCase = true)) {
                return true
            }
        }
        return false
    }

    fun createAccessibilitySettingsIntent(): Intent {
        return Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
    }

    fun hasAllRequiredPermissions(context: Context): Boolean {
        return hasOverlayPermission(context) && isAccessibilityServiceEnabled(context)
    }
}
