package com.example.librewards.ui.main

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.swipeLeft
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withTagValue
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import com.example.librewards.R
import com.example.librewards.data.models.Product
import com.example.librewards.ui.adapters.RecyclerAdapter
import com.example.librewards.utils.AuthTestHelper
import com.example.librewards.utils.DbTestHelper
import com.example.librewards.utils.StorageTestHelper
import org.hamcrest.Matchers.`is`
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class RewardsIntegrationTest {
    val testProducts = listOf(
        Product(
            productName = "Snickers",
            productCost = "20",
        ),
        Product(
            productName = "Coffee",
            productCost = "10",
        ),
    )

    private var testUserEmail: String? = null
    private val testUniversity = "University of Integration Tests"

    @Before
    fun setup() {
        val instrumentation = getInstrumentation()
        val packageName = instrumentation.targetContext.packageName
        val uiAutomation = instrumentation.uiAutomation

        uiAutomation.executeShellCommand("pm grant $packageName android.permission.ACCESS_FINE_LOCATION")
        uiAutomation.executeShellCommand("pm grant $packageName android.permission.ACCESS_COARSE_LOCATION")

        StorageTestHelper.createTestProducts(testUniversity, testProducts)
    }

    @After
    fun tearDown() {
        StorageTestHelper.deleteProducts(testUniversity)
        testUserEmail?.let { email ->
            AuthTestHelper.deleteAuth(email)
            DbTestHelper.deleteTestUser(email)
        }
    }

    @Test
    fun rewards_userCanSeeRewardsAndRedeem_whenFundsAreSufficient() {
        val email = "test_rewards@example.com"
        val password = "password123"
        val firstName = "Rewards"
        val lastName = "Tester"
        val initialPoints = "500"
        testUserEmail = email

        AuthTestHelper.createUser(email, password)
        DbTestHelper.createTestUser(
            email = email,
            firstname = firstName,
            surname = lastName,
            university = testUniversity,
            points = initialPoints,
        )

        val scenario = ActivityScenario.launch(MainActivity::class.java)
        Thread.sleep(3000)

        onView(withId(R.id.viewPager)).perform(swipeLeft())
        Thread.sleep(1000)

        onView(withId(R.id.rewardsPoints)).check(matches(withText(initialPoints)))

        onView(withId(R.id.rewardsRecycler)).perform(
            RecyclerViewActions.actionOnItem<RecyclerAdapter.ViewHolder>(
                hasDescendant(withText("Snickers")), click(),
            ),
        )

        Thread.sleep(1000)

        onView(withId(R.id.popupText)).check(matches(withText("Snickers")))
        onView(withId(R.id.popupCost)).check(matches(withText("20 points")))
        onView(withId(R.id.popupImageView)).check(matches(withTagValue(`is`("Snickers"))))
        onView(withId(R.id.popupQr)).check(matches(isDisplayed()))

        DbTestHelper.updateUserField(testUserEmail!!, "redeemingReward", "1")

        onView(withId(R.id.closeBtn)).perform(click())
        onView(withId(R.id.popupText)).check(doesNotExist())

        val newPointsAfterPurchase = "480"
        onView(withId(R.id.rewardsPoints)).check(matches(withText(newPointsAfterPurchase)))

        scenario.close()
    }

    @Test
    fun rewards_userCannotRedeem_whenFundsAreInsufficient() {
        val email = "test_rewards@example.com"
        val password = "password123"
        val firstName = "Rewards"
        val lastName = "Tester"
        val initialPoints = "5"
        testUserEmail = email

        AuthTestHelper.createUser(email, password)
        DbTestHelper.createTestUser(
            email = email,
            firstname = firstName,
            surname = lastName,
            university = testUniversity,
            points = initialPoints,
        )

        val scenario = ActivityScenario.launch(MainActivity::class.java)
        Thread.sleep(3000)

        onView(withId(R.id.viewPager)).perform(swipeLeft())
        Thread.sleep(1000)

        onView(withId(R.id.rewardsRecycler)).perform(
            RecyclerViewActions.actionOnItem<RecyclerAdapter.ViewHolder>(
                hasDescendant(withText("Coffee")), click(),
            ),
        )

        Thread.sleep(1000)

        onView(withId(R.id.popupText)).check(matches(withText("Coffee")))
        onView(withId(R.id.popupCost)).check(matches(withText("10 points")))
        onView(withId(R.id.popupImageView)).check(matches(withTagValue(`is`("Coffee"))))
        onView(withId(R.id.popupQr)).check(matches(isDisplayed()))

        DbTestHelper.updateUserField(testUserEmail!!, "redeemingReward", "1")

        onView(withId(R.id.closeBtn)).perform(click())
        onView(withId(R.id.popupText)).check(doesNotExist())
        onView(withId(R.id.rewardsPoints)).check(matches(withText(initialPoints)))

        scenario.close()
    }
}
