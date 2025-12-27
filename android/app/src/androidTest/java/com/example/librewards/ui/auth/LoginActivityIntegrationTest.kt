package com.example.librewards.ui.auth

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.pressImeActionButton
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import com.example.librewards.R
import com.example.librewards.ui.main.MainActivity
import com.example.librewards.utils.AuthTestHelper
import com.example.librewards.utils.DbTestHelper
import com.google.firebase.auth.FirebaseAuth
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class LoginActivityIntegrationTest {
    @Before
    fun setup() {
        Intents.init()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val packageName = instrumentation.targetContext.packageName
        val uiAutomation = instrumentation.uiAutomation

        // Grant location permissions
        uiAutomation.executeShellCommand("pm grant $packageName android.permission.ACCESS_FINE_LOCATION")
        uiAutomation.executeShellCommand("pm grant $packageName android.permission.ACCESS_COARSE_LOCATION")
    }

    @After
    fun tearDown() {
        Intents.release()
    }

    @Test
    fun login_withInvalidCredentials_staysOnLoginScreen() {
        val scenario = ActivityScenario.launch(LoginActivity::class.java)

        onView(withId(R.id.loginEmail)).perform(replaceText("invalid@example.com"), closeSoftKeyboard())
        onView(withId(R.id.loginPassword)).perform(replaceText("wrong-password"))

        onView(withId(R.id.loginPassword)).perform(pressImeActionButton())

        // Check login button is still displayed (no navigation)
        onView(withId(R.id.loginButton)).check(matches(isDisplayed()))

        scenario.close()
    }

    @Test
    fun login_withValidCredentials_navigatesToMainActivity() {
        val email = "test@example.com"
        val password = "password123"

        AuthTestHelper.createUser(email, password)
        DbTestHelper.createTestUser(email)
        FirebaseAuth.getInstance().signOut()

        val scenario = ActivityScenario.launch(LoginActivity::class.java)

        try {
            onView(withId(R.id.loginEmail)).perform(replaceText(email), closeSoftKeyboard())
            onView(withId(R.id.loginPassword)).perform(replaceText(password))

            onView(withId(R.id.loginPassword)).perform(pressImeActionButton())

            // Wait for async login network call
            Thread.sleep(1000)

            intended(hasComponent(MainActivity::class.java.name))
        } finally {
            finishAllActivities(scenario)
            DbTestHelper.deleteTestUser(email)
            AuthTestHelper.deleteUser()
        }
    }

    private fun finishAllActivities(scenario: ActivityScenario<*>) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val activities = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED)
            for (activity in activities) {
                if (!activity.isFinishing) {
                    activity.finish()
                }
            }
        }
        scenario.close()
    }
}
