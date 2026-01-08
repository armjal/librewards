package com.example.librewards.ui.main

import android.graphics.Color
import android.location.Location
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
import androidx.test.uiautomator.UiDevice
import com.example.librewards.R
import com.example.librewards.utils.BaseIntegrationTest
import com.example.librewards.utils.UserTestHelper
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.junit.After
import org.junit.Before
import org.junit.Test

class TimerIntegrationTest : BaseIntegrationTest() {
    val inZoneLatLng = LatLng(56.463057, -2.973966)
    private var locationPumpingJob: Job? = null
    private var currentMockLocation: LatLng = inZoneLatLng

    @Before
    override fun setup() {
        super.setup()
        grantMockLocationPermission()
    }

    @After
    override fun tearDown() {
        super.tearDown()
    }

    @Test
    fun timer_accumulatesPoints_whenStudyingSessionCompleted() {
        val email = "test@example.com"
        val university = "Abertay University"
        val currentPoints = "20"

        // 1. Create User with default values
        createStudentUser(
            email = email,
            university = university,
            points = currentPoints,
        )

        val scenario = ActivityScenario.launch(MainActivity::class.java)

        // Start pumping locations continuously
        startLocationPumping(scenario)
        currentMockLocation = inZoneLatLng

        // Wait for user points to be visible
        waitForCondition {
            scenario.assertCircleShownOrColour(null)
            onView(withId(R.id.usersPoints)).check(matches(withText(currentPoints)))
        }

        // 2. Start Timer (Simulating Admin Server: studying="1")
        val timerStartTime = SystemClock.elapsedRealtime()
        UserTestHelper.updateUserField(email, "studying", "1")

        scenario.assertChronometerStarted(timerStartTime)

        // 3. Wait for > 10 seconds (11s ensures we hit the 10001ms threshold and the map is ready)
        Thread.sleep(5000)
        waitForCondition { scenario.assertCircleShownOrColour("blue") }
        Thread.sleep(6000)

        // 4. Stop Timer (Simulating Admin Server: studying="0")
        UserTestHelper.updateUserField(email, "studying", "0")

        // 5. Verify UI changes
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val minutesSpent = 0
        val pointsEarned = 10
        val newTotalPoints = 30
        val minuteText = context.resources.getQuantityString(R.plurals.minutes_plural, minutesSpent)
        val expectedText = context.getString(
            R.string.congrats_message, minutesSpent, minuteText, pointsEarned, newTotalPoints,
        )

        waitForCondition { onView(withId(R.id.popupText)).check(matches(withText(expectedText))) }

        waitForCondition {
            onView(withId(R.id.closeBtn)).perform(click())
            onView(withText(expectedText)).check(doesNotExist())
        }

        waitForCondition {
            onView(withId(R.id.usersPoints)).check(matches(withText(newTotalPoints.toString())))
            scenario.assertCircleShownOrColour(null)
        }

        stopLocationPumping()
        scenario.close()
    }

    @Test
    fun timer_circleTurnsRed_whenLeavingStudyZone_andBlue_whenReturning() {
        val email = "test@example.com"

        // Coords approx 50m away (0.00045 deg lat difference is ~50m)
        val outZoneLatLng = LatLng(56.463457, -2.973966)

        createStudentUser(email = email, university = "Abertay University")

        val scenario = ActivityScenario.launch(MainActivity::class.java)

        // Start pumping locations continuously
        startLocationPumping(scenario)
        currentMockLocation = inZoneLatLng

        waitForCondition { onView(withId(R.id.usersPoints)).check(matches(isDisplayed())) }

        // 2. Start Timer
        val timerStartTime = SystemClock.elapsedRealtime()
        UserTestHelper.updateUserField(email, "studying", "1")

        scenario.assertChronometerStarted(timerStartTime)

        // Wait for initial map setup and "blue" state (Inside zone)
        waitForCondition { scenario.assertCircleShownOrColour("blue") }

        // 3. Move OUT of the zone
        currentMockLocation = outZoneLatLng

        waitForCondition { scenario.assertCircleShownOrColour("red") }

        // 4. Move BACK INTO the zone
        currentMockLocation = inZoneLatLng

        waitForCondition { scenario.assertCircleShownOrColour("blue") }

        stopLocationPumping()
        scenario.close()
    }

    @Test
    fun timer_whenStudentCompletelyLeavesStudyZone_timerStops_andCircleClears() {
        val email = "test@example.com"

        // 1. Create User inside the study zone (Abertay University coords)
        val inZoneLatLng = LatLng(56.463057, -2.973966)

        // Coords approx 70m away
        val completelyOutOfZoneLatLng = LatLng(56.463687, -2.973966)

        createStudentUser(email = email, university = "Abertay University")

        val scenario = ActivityScenario.launch(MainActivity::class.java)

        startLocationPumping(scenario)
        currentMockLocation = inZoneLatLng

        waitForCondition { onView(withId(R.id.usersPoints)).check(matches(isDisplayed())) }

        // 2. Start Timer
        val timerStartTime = SystemClock.elapsedRealtime()
        UserTestHelper.updateUserField(email, "studying", "1")

        waitForCondition { scenario.assertChronometerStarted(timerStartTime) }

        // Wait for initial map setup and "blue" state (Inside zone)
        waitForCondition { scenario.assertCircleShownOrColour("blue") }

        // 3. Move completely OUT of the zone
        currentMockLocation = completelyOutOfZoneLatLng

        waitForCondition { scenario.assertCircleShownOrColour(null) }

        // 4. Move BACK INTO the zone
        currentMockLocation = inZoneLatLng

        waitForCondition { scenario.assertCircleShownOrColour(null) }

        stopLocationPumping()
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
        if (colour != null &&
            circle?.fillColor != colourMap[colour]
        ) {
            throw AssertionError("Map circle wrong color. Expected: $colour, Actual: ${circle?.fillColor}")
        }
    }

    private fun ActivityScenario<MainActivity>.assertChronometerStarted(approxStartTime: Long): ActivityScenario<MainActivity>? =
        onActivity {
            val fragment = (it.supportFragmentManager.fragments.find { f -> f is TimerFragment } as? TimerFragment)
            val chronometer =
                fragment?.view?.findViewById<Chronometer>(R.id.stopwatch)
                    ?: throw AssertionError("Chronometer view not found")

            UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).waitForIdle(500)

            val base = chronometer.base
            val now = SystemClock.elapsedRealtime()

            if (base > now) {
                throw AssertionError("Chronometer base is in the future!")
            }
        }

    private fun startLocationPumping(scenario: ActivityScenario<MainActivity>) {
        locationPumpingJob = CoroutineScope(Dispatchers.IO).launch {
            while (true) {
                scenario.setMockLocation(currentMockLocation)
                delay(1000) // Send update every second
            }
        }
    }

    private fun stopLocationPumping() {
        locationPumpingJob?.cancel()
        locationPumpingJob = null
    }

    private fun ActivityScenario<MainActivity>.setMockLocation(latLng: LatLng) {
        this.onActivity { activity ->
            val fragment = activity.supportFragmentManager.fragments.firstOrNull { it is TimerFragment } as? TimerFragment
            fragment?.let {
                val appFusedLocationClient = it.getMapsViewModelTest().fusedLocationClient

                appFusedLocationClient.setMockMode(true)

                val mockLocation = Location("fused").apply {
                    latitude = latLng.latitude
                    longitude = latLng.longitude
                    accuracy = 5f
                    time = System.currentTimeMillis()
                    elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
                }

                appFusedLocationClient.setMockLocation(mockLocation)
            }
        }
    }
}
