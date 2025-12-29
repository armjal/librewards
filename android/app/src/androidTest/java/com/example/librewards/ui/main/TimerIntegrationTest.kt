package com.example.librewards.ui.main

import android.graphics.Color
import android.os.SystemClock
import android.widget.Chronometer
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry
import com.example.librewards.R
import com.example.librewards.utils.AuthTestHelper
import com.example.librewards.utils.DbTestHelper
import org.junit.After
import org.junit.Before
import org.junit.Test

class TimerIntegrationTest {
    private var testUserEmail: String? = null

    @Before
    fun setup() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val packageName = instrumentation.targetContext.packageName
        val uiAutomation = instrumentation.uiAutomation

        // Grant location permissions
        uiAutomation.executeShellCommand("pm grant $packageName android.permission.ACCESS_FINE_LOCATION")
        uiAutomation.executeShellCommand("pm grant $packageName android.permission.ACCESS_COARSE_LOCATION")
    }

    @After
    fun tearDown() {
        testUserEmail?.let { email ->
            DbTestHelper.deleteTestUser(email)
            AuthTestHelper.deleteAuth(email)
        }
    }

    @Test
    fun timer_accumulatesPoints_whenStudyingSessionCompleted() {
        val email = "test_timer@example.com"
        val password = "password123"
        val firstName = "Timer"
        val lastName = "Test"
        val university = "Abertay University"
        val currentPoints = "20"
        testUserEmail = email

        // 1. Create User with default values
        AuthTestHelper.createUser(email, password)
        DbTestHelper.createTestUser(
            email = email,
            firstname = firstName,
            surname = lastName,
            university = university,
            points = "20",
        )

        val scenario = ActivityScenario.launch(MainActivity::class.java)

        Thread.sleep(3000) // Wait for login and data load
        scenario.assertCircleShownOrColour(null)

        onView(withId(R.id.usersPoints)).check(matches(withText(currentPoints)))

        // 2. Start Timer (Simulating Admin Server: studying="1")
        val startTime = SystemClock.elapsedRealtime()
        DbTestHelper.updateUserField(email, "studying", "1")

        scenario.assertChronometerStarted(startTime)

        // 3. Wait for > 10 seconds (11s ensures we hit the 10001ms threshold and the map is ready)
        Thread.sleep(5000)
        scenario.assertCircleShownOrColour("blue")
        Thread.sleep(6000)

        // 4. Stop Timer (Simulating Admin Server: studying="0")
        DbTestHelper.updateUserField(email, "studying", "0")

        Thread.sleep(2000) // Wait for DB sync

        // 5. Verify UI changes
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val minutesSpent = 0
        val pointsEarned = 10
        val newTotalPoints = 30
        val minuteText = context.resources.getQuantityString(R.plurals.minutes_plural, minutesSpent)
        val expectedText = context.getString(
            R.string.congrats_message, minutesSpent, minuteText, pointsEarned, newTotalPoints,
        )
        onView(withText(expectedText)).check(matches(isDisplayed()))
        onView(withId(R.id.closeBtn)).perform(click())
        onView(withText(expectedText)).check(doesNotExist())
        onView(withId(R.id.usersPoints)).check(matches(withText(newTotalPoints.toString())))
        scenario.assertCircleShownOrColour(null)

        scenario.close()
    }

    private fun ActivityScenario<MainActivity>.assertCircleShownOrColour(colour: String?): ActivityScenario<MainActivity>? = onActivity {
        val circle = (it.supportFragmentManager.fragments.find { f -> f is TimerFragment } as? TimerFragment)?.getMapCircle()
        val colourMap = mapOf(
            "red" to Color.parseColor("#4dff0000"),
            "blue" to Color.parseColor("#4d318ce7"),
        )

        if (colour != null && circle == null) throw AssertionError("Map circle should be visible")
        if (colour == null && circle != null) throw AssertionError("Map circle should not be visible")
        if (colour != null && circle?.fillColor != colourMap[colour]) throw AssertionError("Map circle wrong color")
    }

    private fun ActivityScenario<MainActivity>.assertChronometerStarted(minTimestamp: Long): ActivityScenario<MainActivity>? = onActivity {
        val fragment = (it.supportFragmentManager.fragments.find { f -> f is TimerFragment } as? TimerFragment)
        val chronometer = fragment?.view?.findViewById<Chronometer>(R.id.stopwatch) ?: throw AssertionError("Chronometer view not found")

        Thread.sleep(2000)
        val base = chronometer.base
        if (base < minTimestamp) {
            throw AssertionError("Chronometer base ($base) is older than start time ($minTimestamp). Diff: ${minTimestamp - base}")
        }
    }
}
