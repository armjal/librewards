package com.example.librewards.ui.admin

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.swipeLeft
import androidx.test.espresso.assertion.ViewAssertions.matches
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
import com.example.librewards.utils.ViewUtils.forceClick
import com.google.firebase.auth.FirebaseAuth
import junit.framework.TestCase.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class AdminMainIntegrationTest : BaseIntegrationTest() {
    @Test
    fun adminUi_displaysCorrectValue_whenUserIsLoggedIn() {
        val email = "test_admin@example.com"
        val password = "password123"
        val firstName = "Admin"
        val lastName = "User"
        val university = "Abertay University"
        val points = "100"
        testUserEmail = email

        AuthTestHelper.createUser(email, password)
        AuthTestHelper.setUserAsAdmin(email)
        DbTestHelper.createTestUser(
            email = email,
            firstname = firstName,
            surname = lastName,
            university = university,
            points = points,
        )

        val scenario = ActivityScenario.launch(AdminActivity::class.java)

        // Wait for async data loading (AdminActivity loads user data)
        waitForCondition {
            onView(withId(R.id.adminUsername)).check(matches(isDisplayed()))
            onView(withId(R.id.adminUsername)).check(matches(withText("$firstName $lastName")))

            // Verify AdminHomeFragment views
            onView(withId(R.id.scanTimerButton)).check(matches(isDisplayed()))
            onView(withId(R.id.scanRewardButton)).check(matches(isDisplayed()))
            onView(withId(R.id.enterQr)).check(matches(isDisplayed()))
        }

        // Swipe to Rewards Fragment
        onView(withId(R.id.adminViewPager)).perform(swipeLeft())

        waitForCondition {
            // Verify AdminRewardsFragment views
            onView(withId(R.id.addAProduct)).check(matches(isDisplayed()))
            onView(withId(R.id.adminRewardsRecycler)).check(matches(isDisplayed()))
        }

        scenario.close()
    }

    @Test
    fun admin_navigatesToLogin_whenUserIsLoggedOut() {
        val email = "test_admin@example.com"
        val password = "password123"
        val firstName = "Admin"
        val lastName = "User"
        val university = "Abertay University"
        val points = "100"
        testUserEmail = email

        AuthTestHelper.createUser(email, password)
        AuthTestHelper.setUserAsAdmin(email)
        DbTestHelper.createTestUser(
            email = email,
            firstname = firstName,
            surname = lastName,
            university = university,
            points = points,
        )

        val scenario = ActivityScenario.launch(AdminActivity::class.java)

        waitForCondition {
            onView(withId(R.id.adminProfileImage)).perform(forceClick())
        }

        waitForCondition {
            intended(hasComponent(LoginActivity::class.java.name))
        }

        assertEquals(null, FirebaseAuth.getInstance().currentUser)

        scenario.close()
    }
}
