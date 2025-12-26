package com.example.librewards.ui.main

import android.content.Intent
import android.os.Build
import android.view.View
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.viewpager2.widget.ViewPager2
import com.example.librewards.R
import com.example.librewards.data.models.User
import com.example.librewards.ui.auth.LoginActivity
import com.example.librewards.utils.BaseUiTest
import com.google.android.material.tabs.TabLayout
import com.google.firebase.database.DatabaseReference
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper.runUiThreadTasks

@Config(sdk = [Build.VERSION_CODES.P], instrumentedPackages = ["androidx.loader.content"])
@ExperimentalCoroutinesApi
class MainActivityTest : BaseUiTest() {
    @Mock
    private lateinit var mockProductsRef: DatabaseReference

    override fun setup() {
        super.setup()
        `when`(firebaseTestRule.mockRootRef.child("products")).thenReturn(mockProductsRef)
    }

    @Test
    fun `activity launches and sets up fragments`() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assert(activity != null)
                val viewPager = activity.findViewById<ViewPager2>(R.id.viewPager)
                assertEquals(2, viewPager.adapter?.itemCount)

                val tabLayout = activity.findViewById<TabLayout>(R.id.tabLayout)
                assertEquals(2, tabLayout.tabCount)

                val tab0 = tabLayout.getTabAt(0)
                val tab1 = tabLayout.getTabAt(1)

                assertNotNull(tab0?.icon)
                assertNotNull(tab1?.icon)

                assertEquals(R.drawable.timer, shadowOf(tab0!!.icon).createdFromResId)
                assertEquals(R.drawable.reward, shadowOf(tab1!!.icon).createdFromResId)
            }
        }
    }

    @Test
    fun `clicking profile image logs out user`() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val profileImage = activity.findViewById<View>(R.id.profileImage)
                profileImage.performClick()

                verify(firebaseTestRule.mockFirebaseAuth).signOut()

                val expectedIntent = Intent(activity, LoginActivity::class.java)
                val actualIntent = shadowOf(activity).nextStartedActivity
                assertEquals(expectedIntent.component, actualIntent.component)
            }
        }
    }

    @Test
    fun `username updates when user data received`() {
        val user = User("John", "Doe", "test@example.com", "Test Uni")
        `when`(mockUserSnapshot.getValue(User::class.java)).thenReturn(user)

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val usernameText = activity.findViewById<TextView>(R.id.username)

                // Let the UI update loop run
                runUiThreadTasks()

                assertEquals("John Doe", usernameText.text.toString())
            }
        }
    }

    @Test
    fun `panel slide updates ui`() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                // Simulate panel slide
                activity.mainSharedViewModel.onPanelSlide(0.5f)

                val shadowOverlay = activity.findViewById<View>(R.id.shadowOverlay)
                val profileImage = activity.findViewById<View>(R.id.profileImage)
                val viewPager = activity.findViewById<ViewPager2>(R.id.viewPager)

                // shadowAlpha = (0.5 * 0.6).coerceIn(0, 0.6) = 0.3
                assertEquals(0.3f, shadowOverlay.alpha, 0.01f)

                // isPanelActive = 0.5 > 0.01 = true
                assertFalse(profileImage.isClickable)
                assertFalse(viewPager.isUserInputEnabled)

                // Test panel closed
                activity.mainSharedViewModel.onPanelSlide(0.0f)
                assertEquals(0.0f, shadowOverlay.alpha, 0.01f)
                assertTrue(profileImage.isClickable)
                assertTrue(viewPager.isUserInputEnabled)
            }
        }
    }
}
