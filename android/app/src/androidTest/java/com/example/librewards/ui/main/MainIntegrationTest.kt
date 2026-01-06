package com.example.librewards.ui.main

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.swipeLeft
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.example.librewards.R
import com.example.librewards.ui.auth.LoginActivity
import com.example.librewards.utils.AuthTestHelper
import com.example.librewards.utils.BaseIntegrationTest
import com.example.librewards.utils.DbTestHelper
import com.example.librewards.utils.ViewUtils.collapseSlidingPanel
import com.example.librewards.utils.ViewUtils.expandSlidingPanel
import com.example.librewards.utils.ViewUtils.forceClick
import com.google.firebase.auth.FirebaseAuth
import junit.framework.TestCase.assertEquals
import org.hamcrest.Matchers.containsString
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class MainIntegrationTest : BaseIntegrationTest() {
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
        waitForCondition {
            onView(withId(R.id.username)).check(matches(isDisplayed()))

            // Verify TimerFragment views
            onView(withId(R.id.username)).check(matches(withText("$firstName $lastName")))
            onView(withId(R.id.usersPoints)).check(matches(withText(points))) // Initial points
            onView(withId(R.id.stopwatch)).check(matches(isDisplayed()))
        }

        onView(withId(R.id.slidingPanel)).perform(expandSlidingPanel())

        waitForCondition {
            onView(withId(R.id.qrCode)).check(matches(isDisplayed()))

            onView(withId(R.id.qrCode)).check(matches(isDisplayed()))
            onView(withId(R.id.qrCodeNumber)).check(matches(isDisplayed()))
        }

        onView(withId(R.id.slidingPanel)).perform(collapseSlidingPanel())

        waitForCondition {
            onView(withId(R.id.viewPager)).perform(swipeLeft())
        }

        waitForCondition {
            onView(withId(R.id.rewardsText)).check(matches(withText(containsString("REWARDS FROM ${university.uppercase()}"))))
            onView(withId(R.id.rewardsPoints)).check(matches(withText(points)))
        }

        scenario.close()
    }

    @Test
    fun main_navigatesToLogin_whenUserIsLoggedOut() {
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
        Intents.init()

        val scenario = ActivityScenario.launch(MainActivity::class.java)

        waitForCondition {
            onView(withId(R.id.profileImage)).perform(forceClick())
        }

        waitForCondition {
            intended(hasComponent(LoginActivity::class.java.name))
        }

        assertEquals(null, FirebaseAuth.getInstance().currentUser)

        scenario.close()
        Intents.release()
    }
}
