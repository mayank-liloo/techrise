package com.techrise.util

import android.os.Build
import java.io.File
import java.io.BufferedReader
import java.io.InputStreamReader

object SecurityUtils {

    // 1. Comprehensive Root Detection
    fun isDeviceRooted(): Boolean {
        // Test 1: Check build tags for test-keys (usually signifies custom/root ROMs)
        val buildTags = Build.TAGS
        if (buildTags != null && buildTags.contains("test-keys")) {
            return true
        }

        // Test 2: Check for presence of su binary in standard paths
        val paths = arrayOf(
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su",
            "/su/bin/su"
        )
        for (path in paths) {
            if (File(path).exists()) {
                return true
            }
        }

        // Test 3: Try running 'su' command directly
        var process: Process? = null
        try {
            process = Runtime.getRuntime().exec(arrayOf("/system/xbin/which", "su"))
            val inReader = BufferedReader(InputStreamReader(process.inputStream))
            if (inReader.readLine() != null) {
                return true
            }
        } catch (t: Throwable) {
            // Command execution failed, su likely doesn't exist
        } finally {
            process?.destroy()
        }

        return false
    }

    // 2. Comprehensive Emulator Detection
    fun isRunOnEmulator(): Boolean {
        val brand = Build.BRAND
        val device = Build.DEVICE
        val model = Build.MODEL
        val hardware = Build.HARDWARE
        val product = Build.PRODUCT
        val manufacturer = Build.MANUFACTURER
        val fingerprint = Build.FINGERPRINT

        return (fingerprint.startsWith("generic")
                || fingerprint.startsWith("unknown")
                || model.contains("google_sdk")
                || model.contains("Emulator")
                || model.contains("Android SDK built for x86")
                || manufacturer.contains("Genymotion")
                || hardware.contains("goldfish")
                || hardware.contains("ranchu")
                || product.contains("sdk_gphone")
                || product.contains("google_sdk")
                || product.contains("sdk")
                || product.contains("sdk_x86")
                || product.contains("vbox86p")
                || device.contains("generic")
                || brand.startsWith("generic") && device.startsWith("generic")
                || "google_sdk" == product)
    }
}
