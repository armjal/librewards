package com.example.librewards.ui.admin

import android.content.Intent
import android.view.View
import android.widget.TextView
import androidx.viewpager2.widget.ViewPager2
import com.example.librewards.R
import com.example.librewards.data.models.User
import com.example.librewards.ui.auth.LoginActivity
import com.example.librewards.utils.BaseUiTest
import com.google.android.material.tabs.TabLayout
import com.google.firebase.database.DatabaseReference
import com.google.firebase.storage.StorageReference
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import org.junit.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowLooper.runUiThreadTasks

class AdminActivityTest : BaseUiTest() {
    @Mock
    private lateinit var mockProductsRef: DatabaseReference

    @Mock
    private lateinit var mockStorageRef: StorageReference

    override fun setup() {
        super.setup()
        `when`(firebaseTestRule.mockRootRef.child("products")).thenReturn(mockProductsRef)
        `when`(mockProductsRef.child(anyString())).thenReturn(Mockito.mock(DatabaseReference::class.java))

        `when`(firebaseTestRule.mockStorageRef.child(anyString())).thenReturn(mockStorageRef)
    }

    @Test
    fun `activity launches and sets up fragments`() = launchActivity<AdminActivity> { activity ->
        val viewPager = activity.findViewById<ViewPager2>(R.id.adminViewPager)
        assertEquals(2, viewPager.adapter?.itemCount)

        val tabLayout = activity.findViewById<TabLayout>(R.id.adminTabLayout)
        assertEquals(2, tabLayout.tabCount)

        val tab0 = tabLayout.getTabAt(0)
        val tab1 = tabLayout.getTabAt(1)

        assertNotNull(tab0?.icon)
        assertNotNull(tab1?.icon)

        assertEquals(R.drawable.home, shadowOf(tab0!!.icon).createdFromResId)
        assertEquals(R.drawable.reward, shadowOf(tab1!!.icon).createdFromResId)
    }

    @Test
    fun `clicking profile image logs out user`() = launchActivity<AdminActivity> { activity ->
        val profileImage = activity.findViewById<View>(R.id.adminProfileImage)
        profileImage.performClick()

        verify(firebaseTestRule.mockFirebaseAuth).signOut()

        val expectedIntent = Intent(activity, LoginActivity::class.java)
        val actualIntent = shadowOf(activity).nextStartedActivity
        assertEquals(expectedIntent.component, actualIntent.component)
    }

    @Test
    fun `username updates when user data received`() {
        val user = User("Admin", "User", "admin@example.com", "Test Uni")
        `when`(mockUserSnapshot.getValue(User::class.java)).thenReturn(user)

        launchActivity<AdminActivity> { activity ->
            val usernameText = activity.findViewById<TextView>(R.id.adminUsername)

            // Let the UI update loop run
            runUiThreadTasks()

            assertEquals("Admin User", usernameText.text.toString())
        }
    }
}
