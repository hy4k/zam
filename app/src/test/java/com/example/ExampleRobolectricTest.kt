package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.security.DeviceKeystoreManager
import com.example.security.ParentalAuthManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `app name matches KinNest`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("KinNest", appName)
  }

  @Test
  fun `parental pin verification and update work securely`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    assertTrue(ParentalAuthManager.verifyPin(context, "2468"))
    assertFalse(ParentalAuthManager.verifyPin(context, "0000"))
    ParentalAuthManager.updatePin(context, "9182")
    assertTrue(ParentalAuthManager.verifyPin(context, "9182"))
    assertFalse(ParentalAuthManager.verifyPin(context, "2468"))
  }

  @Test
  fun `e2ee local vault encryption roundtrip succeeds`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val keystore = DeviceKeystoreManager(context)
    val sample = "Goodnight Zayn Adam!"
    val (ciphertext, iv) = keystore.encryptMessageForLocalVault(sample)
    val decrypted = keystore.decryptMessageFromLocalVault(ciphertext, iv)
    assertEquals(sample, decrypted)
  }
}
