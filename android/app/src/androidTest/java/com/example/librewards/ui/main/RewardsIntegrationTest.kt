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
import com.example.librewards.R
import com.example.librewards.data.models.Product
import com.example.librewards.ui.adapters.RecyclerAdapter
import com.example.librewards.utils.BaseIntegrationTest
import com.example.librewards.utils.DbTestHelper
import com.example.librewards.utils.StorageTestHelper
import com.example.librewards.utils.ViewUtils.forceClick
import org.hamcrest.Matchers.`is`
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class RewardsIntegrationTest : BaseIntegrationTest() {
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

    @Before
    override fun setup() {
        super.setup()
        StorageTestHelper.createTestProducts(testUniversity, testProducts)
    }

    @After
    override fun tearDown() {
        StorageTestHelper.deleteProducts(testUniversity)
        super.tearDown()
    }

    @Test
    fun rewards_userCanSeeRewardsAndRedeem_whenFundsAreSufficient() {
        val initialPoints = "500"

        createStudentUser(
            university = testUniversity,
            points = initialPoints,
        )

        val scenario = ActivityScenario.launch(MainActivity::class.java)

        // Wait for data load
        waitForCondition {
            onView(withId(R.id.usersPoints)).check(matches(isDisplayed()))
        }

        onView(withId(R.id.viewPager)).perform(swipeLeft())

        // Wait for Rewards fragment to be visible
        waitForCondition {
            onView(withId(R.id.rewardsPoints)).check(matches(withText(initialPoints)))
        }

        waitForCondition {
            onView(withId(R.id.rewardsRecycler)).perform(
                RecyclerViewActions.actionOnItem<RecyclerAdapter.ViewHolder>(
                    hasDescendant(withText("Snickers")), forceClick(),
                ),
            )
        }

        // Wait for popup
        waitForCondition {
            onView(withId(R.id.popupText)).check(matches(withText("Snickers")))
            onView(withId(R.id.popupCost)).check(matches(withText("20 points")))
            onView(withId(R.id.popupImageView)).check(matches(withTagValue(`is`("Snickers"))))
            onView(withId(R.id.popupQr)).check(matches(isDisplayed()))
        }
        DbTestHelper.updateUserField(testUserEmail!!, "redeemingReward", "1")

        onView(withId(R.id.closeBtn)).perform(click())

        waitForCondition { onView(withId(R.id.popupText)).check(doesNotExist()) }

        val newPointsAfterPurchase = "480"
        waitForCondition {
            onView(withId(R.id.rewardsPoints)).check(matches(withText(newPointsAfterPurchase)))
        }

        scenario.close()
    }

    @Test
    fun rewards_userCannotRedeem_whenFundsAreInsufficient() {
        val initialPoints = "5"

        createStudentUser(points = initialPoints)

        val scenario = ActivityScenario.launch(MainActivity::class.java)

        // Wait for data load
        waitForCondition { onView(withId(R.id.usersPoints)).check(matches(isDisplayed())) }

        onView(withId(R.id.viewPager)).perform(swipeLeft())

        // Wait for Rewards fragment to be visible
        waitForCondition { onView(withId(R.id.rewardsPoints)).check(matches(isDisplayed())) }

        waitForCondition {
            onView(withId(R.id.rewardsRecycler)).perform(
                RecyclerViewActions.actionOnItem<RecyclerAdapter.ViewHolder>(
                    hasDescendant(withText("Coffee")), forceClick(),
                ),
            )
        }

        // Wait for popup
        waitForCondition {
            onView(withId(R.id.popupText)).check(matches(withText("Coffee")))
            onView(withId(R.id.popupCost)).check(matches(withText("10 points")))
            onView(withId(R.id.popupImageView)).check(matches(withTagValue(`is`("Coffee"))))
            onView(withId(R.id.popupQr)).check(matches(isDisplayed()))
        }

        DbTestHelper.updateUserField(testUserEmail!!, "redeemingReward", "1")

        onView(withId(R.id.closeBtn)).perform(click())

        waitForCondition {
            onView(withId(R.id.popupText)).check(doesNotExist())
            onView(withId(R.id.rewardsPoints)).check(matches(withText(initialPoints)))
        }

        scenario.close()
    }
}
