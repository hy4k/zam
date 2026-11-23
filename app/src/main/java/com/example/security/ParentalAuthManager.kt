package com.example.security

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import java.security.MessageDigest

/**
 * Protects Parent-Only Settings, Satirical Household Ledger, and Device Authorization
 * via Android BiometricPrompt + salted SHA-256 PIN verification.
 */
object ParentalAuthManager {

    private const val PREFS_NAME = "kinnest_parent_gate"
    private const val KEY_PIN_HASH = "parent_pin_hash"
    private const val SALT = "kinnest_m_m_z_salt_v1"

    fun canUseBiometrics(context: Context): Boolean {
        val manager = BiometricManager.from(context)
        val result = manager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.BIOMETRIC_WEAK
        )
        return result == BiometricManager.BIOMETRIC_SUCCESS
    }

    fun showBiometricPrompt(
        activity: FragmentActivity,
        title: String = "Parent Verification",
        subtitle: String = "Mithun or Mafitha only — unlock Parent Controls",
        onSuccess: () -> Unit,
        onFallbackToPin: () -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val prompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    onFallbackToPin()
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText("Use Family PIN")
            .build()

        prompt.authenticate(promptInfo)
    }

    fun hasCustomPin(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.contains(KEY_PIN_HASH)
    }

    fun verifyPin(context: Context, enteredPin: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val storedHash = prefs.getString(KEY_PIN_HASH, null)
        // First-time default PIN is "2468" until parents customize it inside Parental Settings
        val expectedHash = storedHash ?: sha256("2468")
        return sha256(enteredPin) == expectedHash
    }

    fun updatePin(context: Context, newPin: String) {
        if (newPin.length < 4) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_PIN_HASH, sha256(newPin)).apply()
    }

    private fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256")
            .digest("$SALT:$input".toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
