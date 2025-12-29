package com.example.librewards.ui.main

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.swipeLeft
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import com.example.librewards.R
import com.example.librewards.utils.AuthTestHelper
import com.example.librewards.utils.DbTestHelper
import com.example.librewards.utils.ViewUtils.collapseSlidingPanel
import com.example.librewards.utils.ViewUtils.expandSlidingPanel
import org.hamcrest.Matchers.containsString
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class MainIntegrationTest {
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
            AuthTestHelper.deleteAuth(email)
            DbTestHelper.deleteTestUser(email)
        }
    }

    @Test
    fun mainUi_displaysCorrectValue_whenUserIsLoggedIn() {
        val email = "test_main@example.com"
        val password = "password123"
        val firstName = "Integration"
        val lastName = "Tester"
        val university = "Abertay University"
        val points = "50"
        testUserEmail = email

        AuthTestHelper.createUser(email, password)
        DbTestHelper.createTestUser(
            email = email,
            firstname = firstName,
            surname = lastName,
            university = university,
            points = points,
        )

        val scenario = ActivityScenario.launch(MainActivity::class.java)
        // Wait for async data loading (MainActivity loads user data)
        Thread.sleep(3000)

        // Verify TimerFragment views
        onView(withId(R.id.username)).check(matches(withText("$firstName $lastName")))
        onView(withId(R.id.usersPoints)).check(matches(withText(points))) // Initial points
        onView(withId(R.id.stopwatch)).check(matches(isDisplayed()))

        onView(withId(R.id.slidingPanel)).perform(expandSlidingPanel())
        Thread.sleep(1000)

        onView(withId(R.id.qrCode)).check(matches(isDisplayed()))
        onView(withId(R.id.qrCodeNumber)).check(matches(isDisplayed()))
        onView(withId(R.id.slidingPanel)).perform(collapseSlidingPanel())
        Thread.sleep(1000)

        // Verify RewardsFragment
        onView(withId(R.id.viewPager)).perform(swipeLeft())
        Thread.sleep(1000) // Wait for swipe animation

        onView(withId(R.id.rewardsText)).check(matches(withText(containsString("REWARDS FROM ${university.uppercase()}"))))
        onView(withId(R.id.rewardsPoints)).check(matches(withText(points)))

        scenario.close()
    }
}
