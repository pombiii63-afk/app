package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.lang.Exception

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun testFirebaseInitialization() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    try {
      val options = FirebaseOptions.Builder()
        .setApiKey("AIzaSyB-campus-delivery-placeholderKey2")
        .setApplicationId("1:123456789012:android:abcdef1234567890")
        .setProjectId("campus-delivery-placeholder1")
        .build()
      FirebaseApp.initializeApp(context, options)
      println("FIREBASE SUCCESSFUL INITIALIZATION")
    } catch (e: Exception) {
      e.printStackTrace()
      throw e
    }
  }

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Campus Delivery", appName)
  }
}
