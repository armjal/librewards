package com.example.librewards.ui.main

import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import com.example.librewards.R
import com.example.librewards.data.models.Product
import com.example.librewards.ui.adapters.RecyclerAdapter
import com.example.librewards.utils.BaseUiTest
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import org.junit.After
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.atLeastOnce
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.robolectric.shadows.ShadowDialog
import org.robolectric.shadows.ShadowLooper
import org.robolectric.shadows.ShadowToast

class RewardsFragmentTest : BaseUiTest() {
    @Mock private lateinit var mockProductsRef: DatabaseReference

    @Mock private lateinit var mockUniversityRef: DatabaseReference

    @Mock private lateinit var mockRedeemingRef: DatabaseReference

    @Mock private lateinit var mockProductsSnapshot: DataSnapshot

    @Mock private lateinit var mockRedeemingSnapshot: DataSnapshot

    override fun setup() {
        super.setup()
        setupFirebase()
        setupPicassoMock()
    }

    private fun setupFirebase() {
        val root = firebaseTestRule.mockRootRef
        val userRef = firebaseTestRule.mockSpecificUserRef

        `when`(root.child("products")).thenReturn(mockProductsRef)
        `when`(mockProductsRef.child(any())).thenReturn(mockUniversityRef)
        `when`(userRef.child("redeemingReward")).thenReturn(mockRedeemingRef)

        val p1 = createMockProductSnapshot("p1", Product("Coffee", "50", "url"))
        val p2 = createMockProductSnapshot("p2", Product("Muffin", "100", "url"))

        `when`(mockProductsSnapshot.children).thenReturn(listOf(p1, p2))
    }

    @After
    fun tearDown() {
        resetPicassoMock()
    }

    @Test
    fun `fragment displays list of products`() = launchFragment { fragment ->
        val recycler = fragment.view?.findViewById<RecyclerView>(R.id.rewardsRecycler)
        assertNotNull(recycler)
        assertEquals(2, recycler?.adapter?.itemCount)
    }

    @Test
    fun `clicking product shows popup details`() = launchFragment { fragment ->
        (fragment as RecyclerAdapter.OnProductListener).onProductClick(0)
        ShadowLooper.runUiThreadTasks()

        val dialog = ShadowDialog.getLatestDialog()
        assertNotNull("Popup should be visible", dialog)
        assertEquals("Coffee", dialog.findViewById<TextView>(R.id.popupText).text.toString())
        assertEquals("50 points", dialog.findViewById<TextView>(R.id.popupCost).text.toString())
    }

    @Test
    fun `redeeming status updates points`() = launchFragment { fragment ->
        setupPoints("100")

        val redeemingCaptor = argumentCaptor<ValueEventListener>()
        verify(mockRedeemingRef, atLeastOnce()).addValueEventListener(redeemingCaptor.capture())

        (fragment as RecyclerAdapter.OnProductListener).onProductClick(0)

        `when`(mockRedeemingSnapshot.value).thenReturn("1")
        redeemingCaptor.allValues.last().onDataChange(mockRedeemingSnapshot)
        ShadowLooper.runUiThreadTasks()

        verify(mockPointsRef).setValue("50")
    }

    @Test
    fun `insufficient funds shows toast`() = launchFragment { fragment ->
        setupPoints("10")

        val redeemingCaptor = argumentCaptor<ValueEventListener>()
        verify(mockRedeemingRef, atLeastOnce()).addValueEventListener(redeemingCaptor.capture())

        // Click product costing 50
        (fragment as RecyclerAdapter.OnProductListener).onProductClick(0)

        // Trigger redeemed status from DB
        `when`(mockRedeemingSnapshot.value).thenReturn("1")
        redeemingCaptor.allValues.last().onDataChange(mockRedeemingSnapshot)
        ShadowLooper.runUiThreadTasks()

        // Verify toast is shown
        val latestToast = ShadowToast.getLatestToast()
        assertNotNull("Toast should be shown for insufficient funds", latestToast)
        assertEquals("You do not have sufficient points for this purchase", ShadowToast.getTextOfLatestToast())

        // Verify points were NOT updated
        verify(mockPointsRef, Mockito.never()).setValue(any())
    }

    private fun launchFragment(block: (RewardsFragment) -> Unit) {
        ActivityScenario.launch(MainActivity::class.java).use {
            it.onActivity { activity ->
                ShadowLooper.runUiThreadTasks()
                val fragment = RewardsFragment()
                activity.supportFragmentManager.beginTransaction().replace(android.R.id.content, fragment).commitNow()

                val captor = argumentCaptor<ValueEventListener>()
                verify(mockUniversityRef, atLeastOnce()).addValueEventListener(captor.capture())
                captor.allValues.last().onDataChange(mockProductsSnapshot)
                ShadowLooper.runUiThreadTasks()

                block(fragment)
            }
        }
    }
}
