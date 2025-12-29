package com.example.librewards.ui.auth

import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import com.example.librewards.R
import com.example.librewards.ui.main.MainActivity
import com.example.librewards.utils.AuthTestHelper
import com.example.librewards.utils.DbTestHelper
import com.example.librewards.utils.ViewUtils.forceClick
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.instanceOf
import org.hamcrest.Matchers.`is`
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class RegisterActivityIntegrationTest {
    @Rule
    @JvmField
    val scenarioRule = ActivityScenarioRule(RegisterActivity::class.java)

    private var testUserEmail: String? = null

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

        testUserEmail?.let { email ->
            AuthTestHelper.deleteAuth(email)
            DbTestHelper.deleteTestUser(email)
        }
    }

    @Test
    fun registration_whenSuccessfullyRegistered_navigatesToMainActivity() {
        val email = "test@example.com"
        val password = "password123"
        testUserEmail = email

        onView(withId(R.id.registrationFirstName)).perform(replaceText("test"))
        onView(withId(R.id.registrationLastName)).perform(replaceText("user"))
        onView(withId(R.id.registrationEmail)).perform(replaceText(email))
        onView(withId(R.id.registrationPassword)).perform(replaceText(password))

        onView(withId(R.id.registrationSpinner)).perform(click())
        onData(allOf(`is`(instanceOf(String::class.java)), `is`("Abertay University"))).perform(click())

        onView(withId(R.id.registerHereButton)).perform(forceClick())

        // Wait for async registration network call
        Thread.sleep(2000)

        intended(hasComponent(LoginActivity::class.java.name))

        // Wait for async login network call
        Thread.sleep(2000)

        intended(hasComponent(MainActivity::class.java.name))
    }

    @Test
    fun registration_whenPasswordNotInAcceptableFormat_remainsOnRegistrationPage() {
        val email = "test@example.com"
        val invalidPassword = "1"
        testUserEmail = email

        onView(withId(R.id.registrationFirstName)).perform(replaceText("test"))
        onView(withId(R.id.registrationLastName)).perform(replaceText("user"))
        onView(withId(R.id.registrationEmail)).perform(replaceText(email))
        onView(withId(R.id.registrationPassword)).perform(replaceText(invalidPassword))

        onView(withId(R.id.registrationSpinner)).perform(click())
        onData(allOf(`is`(instanceOf(String::class.java)), `is`("Abertay University"))).perform(click())

        onView(withId(R.id.registerHereButton)).perform(forceClick())

        // Wait for async registration network call
        Thread.sleep(2000)

        onView(withId(R.id.registrationSpinner)).check(matches(isDisplayed()))
    }
}
