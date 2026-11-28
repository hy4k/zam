package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.FamilyMemberId
import com.example.security.DeviceKeystoreManager
import com.example.security.ParentalAuthManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `app name matches Holy Moly`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Holy Moly !", appName)
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

  @Test
  fun `MainActivity launches cleanly on login and logged-in workspace`() {
    val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
    assertNotNull(controller.get())
    controller.pause().stop().destroy()

    val context = ApplicationProvider.getApplicationContext<Context>()
    context.getSharedPreferences("holymoly_device_binding", Context.MODE_PRIVATE)
      .edit()
      .putString("logged_in_member_v2", FamilyMemberId.MITHUN.name)
      .commit()

    val loggedInController = Robolectric.buildActivity(MainActivity::class.java).setup()
    assertNotNull(loggedInController.get())
    loggedInController.pause().stop().destroy()
  }
}
