package com.example.librewards.ui.admin

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.swipeLeft
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.isInternal
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withTagValue
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry
import com.example.librewards.R
import com.example.librewards.data.models.Product
import com.example.librewards.ui.adapters.RecyclerAdapter
import com.example.librewards.utils.AuthTestHelper
import com.example.librewards.utils.BaseIntegrationTest
import com.example.librewards.utils.DbTestHelper
import com.example.librewards.utils.StorageTestHelper
import com.example.librewards.utils.ViewUtils.forceClick
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.not
import org.junit.After
import org.junit.Before
import org.junit.Test

class AdminRewardsIntegrationTest : BaseIntegrationTest() {
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
        StorageTestHelper.createTestProducts(products = testProducts)
        super.setup()
    }

    @After
    override fun tearDown() {
        StorageTestHelper.deleteProducts()
        super.tearDown()
    }

    @Test
    fun adminRewards_userCanAddReward() {
        val email = "test_rewards@example.com"
        val password = "password123"
        val firstName = "Rewards"
        val lastName = "Tester"
        val initialPoints = "500"
        testUserEmail = email

        AuthTestHelper.createUser(email, password)
        AuthTestHelper.setUserAsAdmin(email)
        DbTestHelper.createTestUser(
            email = email,
            firstname = firstName,
            surname = lastName,
            university = testUniversity,
            points = initialPoints,
        )

        val scenario = ActivityScenario.launch(AdminActivity::class.java)

        // Wait for data load
        waitForCondition {
            onView(withId(R.id.adminUsername)).check(matches(isDisplayed()))
        }

        onView(withId(R.id.adminViewPager)).perform(swipeLeft())

        waitForCondition {
            onView(withId(R.id.addAProduct)).perform(click())
            chooseImageUsingChooseButtonView("books")
            onView(withId(R.id.productName)).perform(typeText("Book loan extension by 1 week"))
            onView(withId(R.id.productCost)).perform(typeText("6"))
        }

        waitForCondition {
            onView(withId(R.id.uploadButton)).perform(click())
            onView(withId(R.id.uploadProgressBar)).check(matches(not(isDisplayed())))
        }

        onView(withId(R.id.closeBtnAdmin)).perform(click())

        // Refresh UI to trigger fresh load of products
        scenario.recreate()

        waitForCondition {
            onView(withId(R.id.adminRewardsRecycler)).perform(
                RecyclerViewActions.actionOnItem<RecyclerAdapter.ViewHolder>(
                    hasDescendant(withText("Book loan extension by 1 week")), forceClick(),
                ),
            )
        }

        waitForCondition {
            onView(withId(R.id.manageProductName)).check(matches(withText("Book loan extension by 1 week")))
            onView(withId(R.id.manageProductCost)).check(matches(withText("6")))
            onView(withId(R.id.manageProductImage)).check(matches(withTagValue(`is`("Book loan extension by 1 week"))))
        }
    }

    @Test
    fun adminRewards_userCanDeleteReward() {
        val email = "test_rewards@example.com"
        val password = "password123"
        val firstName = "Rewards"
        val lastName = "Tester"

        testUserEmail = email

        AuthTestHelper.createUser(email, password)
        AuthTestHelper.setUserAsAdmin(email)

        DbTestHelper.createTestUser(
            email = email,
            firstname = firstName,
            surname = lastName,
            university = testUniversity,
        )

        val scenario = ActivityScenario.launch(AdminActivity::class.java)

        // Wait for data load
        waitForCondition {
            onView(withId(R.id.adminUsername)).check(matches(isDisplayed()))
        }

        onView(withId(R.id.adminViewPager)).perform(swipeLeft())

        waitForCondition {
            onView(withId(R.id.adminRewardsRecycler)).perform(
                RecyclerViewActions.actionOnItem<RecyclerAdapter.ViewHolder>(
                    hasDescendant(withText("Snickers")), forceClick(),
                ),
            )
        }

        waitForCondition {
            onView(withId(R.id.deleteButton)).perform(forceClick())
        }

        // Refresh UI to trigger fresh load of products
        scenario.recreate()

        waitForCondition {
            onView(withId(R.id.adminRewardsRecycler))
                .check(matches(not(hasDescendant(withText("Snickers")))))
        }
    }

    @Test
    fun adminRewards_userCanManageRewardDetails() {
        val email = "test_rewards@example.com"
        val password = "password123"
        val firstName = "Rewards"
        val lastName = "Tester"

        testUserEmail = email

        AuthTestHelper.createUser(email, password)
        AuthTestHelper.setUserAsAdmin(email)
        DbTestHelper.createTestUser(
            email = email,
            firstname = firstName,
            surname = lastName,
            university = testUniversity,
        )

        val scenario = ActivityScenario.launch(AdminActivity::class.java)

        // Wait for data load
        waitForCondition {
            onView(withId(R.id.adminUsername)).check(matches(isDisplayed()))
        }

        onView(withId(R.id.adminViewPager)).perform(swipeLeft())

        waitForCondition {
            onView(withId(R.id.adminRewardsRecycler)).perform(
                RecyclerViewActions.actionOnItem<RecyclerAdapter.ViewHolder>(
                    hasDescendant(withText("Snickers")), forceClick(),
                ),
            )
        }

        val snickersNewName = "Chocolate Bar"
        val snickersNewCost = "10"

        waitForCondition {
            onView(withId(R.id.manageProductCost)).perform(replaceText(snickersNewCost))
            onView(withId(R.id.manageProductName)).perform(replaceText(snickersNewName))
            onView(withId(R.id.updateButton)).perform(click())
        }

        // Refresh UI to trigger fresh load of products
        scenario.recreate()

        waitForCondition {
            onView(withId(R.id.adminRewardsRecycler)).perform(
                RecyclerViewActions.actionOnItem<RecyclerAdapter.ViewHolder>(
                    hasDescendant(withText(snickersNewName)), forceClick(),
                ),
            )
        }

        waitForCondition {
            onView(withId(R.id.manageProductCost)).check(matches(withText(snickersNewCost)))
            onView(withId(R.id.manageProductName)).check(matches(withText(snickersNewName)))
        }
    }

    private fun chooseImageUsingChooseButtonView(imageInTestDrawablesDirectory: String) {
        Intents.init()

        try {
            val testContext = InstrumentationRegistry.getInstrumentation().context
            val imageUri = Uri.parse("android.resource://${testContext.packageName}/drawable/$imageInTestDrawablesDirectory")

            val resultData = Intent()
            resultData.data = imageUri
            val result = Instrumentation.ActivityResult(Activity.RESULT_OK, resultData)

            intending(not(isInternal())).respondWith(result)
            onView(withId(R.id.chooseButton)).perform(click())
        } finally {
            Intents.release()
        }
    }
}
