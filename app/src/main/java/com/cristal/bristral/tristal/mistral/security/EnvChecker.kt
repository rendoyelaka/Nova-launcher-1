package com.cristal.bristral.tristal.mistral.security

import android.content.Context
import android.os.Build
import android.provider.Settings
import java.io.File

/**
 * EnvChecker — detects emulators, sandboxes and analysis environments.
 *
 * Runs 7 independent checks. If 2 or more fail → treat as sandbox.
 * Silent — never throws, never logs, never crashes.
 */
object EnvChecker {

    /**
     * Returns true if environment is a real device.
     * Returns false if emulator/sandbox detected.
     */
    fun isRealDevice(context: Context): Boolean {
        var suspiciousCount = 0

        // Check 1 — Build fingerprint contains emulator markers
        try {
            val fp = Build.FINGERPRINT.lowercase()
            if (fp.contains("generic") ||
                fp.contains("unknown") ||
                fp.contains("emulator") ||
                fp.contains("sdk_gphone") ||
                fp.contains("vbox") ||
                fp.contains("test-keys")) {
                suspiciousCount++
            }
        } catch (e: Exception) { suspiciousCount++ }

        // Check 2 — Hardware name contains emulator markers
        try {
            val hw = Build.HARDWARE.lowercase()
            if (hw.contains("goldfish") ||
                hw.contains("ranchu") ||
                hw.contains("vbox") ||
                hw.contains("nox")) {
                suspiciousCount++
            }
        } catch (e: Exception) { suspiciousCount++ }

        // Check 3 — Known emulator build models
        try {
            val model = Build.MODEL.lowercase()
            if (model.contains("sdk") ||
                model.contains("emulator") ||
                model.contains("android sdk") ||
                model.contains("droid4x") ||
                model.contains("nox") ||
                model.contains("tencent")) {
                suspiciousCount++
            }
        } catch (e: Exception) { suspiciousCount++ }

        // Check 4 — Known emulator manufacturer
        try {
            val mfr = Build.MANUFACTURER.lowercase()
            if (mfr.contains("genymotion") ||
                mfr.contains("unknown") ||
                mfr == "google" && Build.BRAND.lowercase() == "generic") {
                suspiciousCount++
            }
        } catch (e: Exception) { suspiciousCount++ }

        // Check 5 — Emulator-specific system files present
        try {
            val emulatorFiles = listOf(
                "/dev/socket/qemud",
                "/dev/qemu_pipe",
                "/system/lib/libc_malloc_debug_qemu.so",
                "/sys/qemu_trace",
                "/system/bin/qemu-props",
                "/dev/socket/genyd"
            )
            if (emulatorFiles.any { File(it).exists() }) {
                suspiciousCount++
            }
        } catch (e: Exception) { }

        // Check 6 — ANDROID_ID is null, empty or known emulator default
        try {
            val androidId = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ANDROID_ID
            )
            if (androidId.isNullOrEmpty() ||
                androidId == "000000000000000" ||
                androidId == "9774d56d682e549c") {
                suspiciousCount++
            }
        } catch (e: Exception) { suspiciousCount++ }

        // Check 7 — CPU ABI is x86 (most emulators use x86)
        try {
            val abi = Build.SUPPORTED_ABIS.firstOrNull()?.lowercase() ?: ""
            if (abi.contains("x86") && !abi.contains("arm")) {
                suspiciousCount++
            }
        } catch (e: Exception) { }

        // 2 or more suspicious signals = sandbox
        return suspiciousCount < 2
    }
}
