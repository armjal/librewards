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
import com.example.librewards.R
import com.example.librewards.ui.main.MainActivity
import com.example.librewards.utils.BaseIntegrationTest
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
class RegisterActivityIntegrationTest : BaseIntegrationTest() {
    @Rule
    @JvmField
    val scenarioRule = ActivityScenarioRule(RegisterActivity::class.java)

    @Before
    override fun setup() {
        super.setup()
        Intents.init()
    }

    @After
    override fun tearDown() {
        Intents.release()
        super.tearDown()
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

        // Wait for async registration network call and navigation
        waitForCondition { intended(hasComponent(MainActivity::class.java.name)) }
    }

    @Test
    fun registration_whenPasswordNotInAcceptableFormat_remainsOnRegistrationPage() {
        val email = "test@example.com"
        val invalidPassword = "1"

        onView(withId(R.id.registrationFirstName)).perform(replaceText("test"))
        onView(withId(R.id.registrationLastName)).perform(replaceText("user"))
        onView(withId(R.id.registrationEmail)).perform(replaceText(email))
        onView(withId(R.id.registrationPassword)).perform(replaceText(invalidPassword))

        onView(withId(R.id.registrationSpinner)).perform(click())
        onData(allOf(`is`(instanceOf(String::class.java)), `is`("Abertay University"))).perform(click())

        onView(withId(R.id.registerHereButton)).perform(forceClick())

        // Ensure we are still on the registration screen
        waitForCondition { onView(withId(R.id.registrationSpinner)).check(matches(isDisplayed())) }

        onView(withId(R.id.registrationSpinner)).check(matches(isDisplayed()))
    }
}
