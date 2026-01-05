package com.example.librewards.utils

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import org.junit.After
import org.junit.Before

open class BaseIntegrationTest {
    protected var testUserEmail: String? = null

    @Before
    open fun setup() {
        grantLocationPermissions()
    }

    @After
    open fun tearDown() {
        testUserEmail?.let { email ->
            runCatching { AuthTestHelper.deleteAuth(email) }
            runCatching { DbTestHelper.deleteTestUser(email) }
        }
        testUserEmail = null
    }

    protected fun grantLocationPermissions() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val packageName = instrumentation.targetContext.packageName
        val uiAutomation = instrumentation.uiAutomation

        uiAutomation.executeShellCommand("pm grant $packageName android.permission.ACCESS_FINE_LOCATION")
        uiAutomation.executeShellCommand("pm grant $packageName android.permission.ACCESS_COARSE_LOCATION")
    }

    protected fun grantMockLocationPermission() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val packageName = instrumentation.targetContext.packageName
        val uiAutomation = instrumentation.uiAutomation
        uiAutomation.executeShellCommand("appops set $packageName android:mock_location allow")
    }

    protected fun waitForCondition(timeoutMillis: Long = 5000, condition: () -> Unit) {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < timeoutMillis) {
            try {
                condition()
                return
            } catch (e: Throwable) {
                device.waitForIdle(100)
            }
        }
    }
}
