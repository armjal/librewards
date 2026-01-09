package com.example.librewards.utils

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import org.junit.After
import org.junit.Before
import org.junit.BeforeClass

object NetworkState {
    const val NETWORK_CHECK_TIMEOUT = 60000L
    const val NETWORK_CHECK_INTERVAL = 2000L
    var isConnected = false
}

open class BaseIntegrationTest {
    companion object {
        @BeforeClass
        @JvmStatic
        fun waitForNetworkConnection() {
            if (NetworkState.isConnected) return

            println("Waiting for network connection...")
            val startTime = System.currentTimeMillis()
            while (!isNetworkAvailable()) {
                if (System.currentTimeMillis() - startTime > NetworkState.NETWORK_CHECK_TIMEOUT) {
                    throw RuntimeException("Failed to establish network connection within ${NetworkState.NETWORK_CHECK_TIMEOUT} ms.")
                }
                println("Network not ready, waiting...")
                Thread.sleep(NetworkState.NETWORK_CHECK_INTERVAL)
            }
            NetworkState.isConnected = true
            println("Network connection is ready.")
        }

        private fun isNetworkAvailable(): Boolean = try {
            val command = "ping -c 1 8.8.8.8"
            val process = Runtime.getRuntime().exec(command)
            val exitCode = process.waitFor()
            exitCode == 0
        } catch (e: Exception) {
            false
        }
    }

    protected var testUserEmail: String? = null
    protected var testUniversity = "University of Integration Tests"

    @Before
    open fun setup() {
        grantLocationPermissions()
    }

    @After
    open fun tearDown() {
        testUserEmail?.let { email ->
            runCatching { AuthTestHelper.deleteAuth(email) }
            runCatching { UserTestHelper.deleteTestUser(email) }
        }
        testUserEmail = null
    }

    protected fun createStudentUser(
        email: String = "test_user@example.com",
        password: String = "password123",
        firstName: String = "Test",
        lastName: String = "User",
        university: String = "University of Integration Tests",
        points: String = "0",
    ) {
        testUserEmail = email
        AuthTestHelper.createUser(email, password)
        UserTestHelper.createTestUser(
            email = email,
            firstname = firstName,
            surname = lastName,
            university = university,
            points = points,
        )
    }

    protected fun createAdminUser(
        email: String = "admin_user@example.com",
        password: String = "password123",
        firstName: String = "Admin",
        lastName: String = "User",
        university: String = "University of Integration Tests",
        points: String = "0",
    ) {
        testUserEmail = email
        AuthTestHelper.createUser(email, password)
        AuthTestHelper.setUserAsAdmin(email)
        UserTestHelper.createTestUser(
            email = email,
            firstname = firstName,
            surname = lastName,
            university = university,
            points = points,
        )
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

    protected fun waitForCondition(timeoutMillis: Long = 7000, condition: () -> Unit) {
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
        condition()
    }
}
