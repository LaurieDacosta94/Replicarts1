package com.example

import android.content.Context
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun `read string from context and verify main activity renders 3d battlefield`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Crown and Dominion", appName)

        composeTestRule.onNodeWithTag("battlefield_3d_viewport").assertExists()
        composeTestRule.onNodeWithTag("top_resource_ribbon_container").assertExists()
    }
}
