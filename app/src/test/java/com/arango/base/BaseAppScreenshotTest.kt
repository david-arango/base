package com.arango.base

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.arango.base.ui.theme.BaseTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class BaseAppScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun app_screenshot() {
        composeTestRule.setContent {
            BaseTheme {
                BaseApp()
            }
        }

        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/base_app.png")
    }
}
