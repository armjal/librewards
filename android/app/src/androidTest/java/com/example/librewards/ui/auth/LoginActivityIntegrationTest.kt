package com.example.librewards.ui.auth

import androidx.test.espresso.Espresso.onView
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
import com.example.librewards.ui.admin.AdminActivity
import com.example.librewards.ui.main.MainActivity
import com.example.librewards.utils.AuthTestHelper
import com.example.librewards.utils.DbTestHelper
import com.example.librewards.utils.ViewUtils.finishAllActivities
import com.example.librewards.utils.ViewUtils.forceClick
import com.google.firebase.auth.FirebaseAuth
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class LoginActivityIntegrationTest {
    @Rule
    @JvmField
    val scenarioRule = ActivityScenarioRule(LoginActivity::class.java)

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
        finishAllActivities()

        testUserEmail?.let { email ->
            DbTestHelper.deleteTestUser(email)
            AuthTestHelper.deleteUser()
        }
    }

    @Test
    fun login_withInvalidCredentials_staysOnLoginScreen() {
        onView(withId(R.id.loginEmail))
            .perform(replaceText("test@test.com"))

        onView(withId(R.id.loginPassword))
            .perform(replaceText("Password123"))

        onView(withId(R.id.loginButton))
            .perform(forceClick())

        Thread.sleep(1000)
        onView(withId(R.id.loginEmail)).check(matches(isDisplayed()))
    }

    @Test
    fun login_withStudentCredentials_navigatesToAdminActivity() {
        val email = "test@example.com"
        val password = "password123"
        testUserEmail = email

        AuthTestHelper.createUser(email, password)
        DbTestHelper.createTestUser(email)
        FirebaseAuth.getInstance().signOut()

        onView(withId(R.id.loginEmail)).check(matches(isDisplayed())).perform(replaceText(email))
        onView(withId(R.id.loginPassword)).check(matches(isDisplayed())).perform(replaceText(password))
        onView(withId(R.id.loginButton)).check(matches(isDisplayed())).perform(forceClick())

        // Wait for async login network call
        Thread.sleep(1000)

        intended(hasComponent(MainActivity::class.java.name))
    }

    @Test
    fun login_withAdminCredentials_navigatesToAdminActivity() {
        val email = "admin@example.com"
        val password = "password123"
        testUserEmail = email

        AuthTestHelper.createUser(email, password)
        AuthTestHelper.setUserAsAdmin(email)
        DbTestHelper.createTestUser(email)
        FirebaseAuth.getInstance().signOut()

        onView(withId(R.id.loginEmail)).check(matches(isDisplayed())).perform(replaceText(email))
        onView(withId(R.id.loginPassword)).check(matches(isDisplayed())).perform(replaceText(password))
        onView(withId(R.id.loginButton)).check(matches(isDisplayed())).perform(forceClick())

        // Wait for async login network call
        Thread.sleep(1000)

        intended(hasComponent(AdminActivity::class.java.name))
    }
}
