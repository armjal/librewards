package com.example.librewards.ui.main

import android.os.Build
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import com.example.librewards.R
import com.example.librewards.data.models.Product
import com.example.librewards.data.models.User
import com.example.librewards.ui.adapters.RecyclerAdapter
import com.example.librewards.utils.FirebaseTestRule
import com.example.librewards.utils.MainDispatcherRule
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.squareup.picasso.Picasso
import com.squareup.picasso.RequestCreator
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.atLeastOnce
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowDialog
import org.robolectric.shadows.ShadowLooper
import org.robolectric.shadows.ShadowToast
import java.util.concurrent.Executor

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P], instrumentedPackages = ["androidx.loader.content"])
@ExperimentalCoroutinesApi
class RewardsFragmentTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    @get:Rule val firebaseTestRule = FirebaseTestRule()

    @Mock private lateinit var mockProductsRef: DatabaseReference

    @Mock private lateinit var mockUniversityRef: DatabaseReference

    @Mock private lateinit var mockPointsRef: DatabaseReference

    @Mock private lateinit var mockRedeemingRef: DatabaseReference

    @Mock private lateinit var mockProductsSnapshot: DataSnapshot

    @Mock private lateinit var mockProductSnapshot1: DataSnapshot

    @Mock private lateinit var mockProductSnapshot2: DataSnapshot

    @Mock private lateinit var mockPointsSnapshot: DataSnapshot

    @Mock private lateinit var mockRedeemingSnapshot: DataSnapshot

    @Mock private lateinit var mockUserTask: Task<DataSnapshot>

    @Mock private lateinit var mockUserSnapshot: DataSnapshot

    @Mock private lateinit var mockPicasso: Picasso

    @Mock private lateinit var mockRequestCreator: RequestCreator

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        setupFirebase()
        setupPicasso()
    }

    private fun setupFirebase() {
        val root = firebaseTestRule.mockRootRef
        val userRef = firebaseTestRule.mockSpecificUserRef

        `when`(root.child("products")).thenReturn(mockProductsRef)
        `when`(mockProductsRef.child(any())).thenReturn(mockUniversityRef)
        `when`(userRef.child("points")).thenReturn(mockPointsRef)
        `when`(userRef.child("redeemingReward")).thenReturn(mockRedeemingRef)
        `when`(userRef.child("studying")).thenReturn(Mockito.mock(DatabaseReference::class.java))

        `when`(mockProductsSnapshot.children).thenReturn(listOf(mockProductSnapshot1, mockProductSnapshot2))
        setupProduct(mockProductSnapshot1, "p1", Product("Coffee", "50", "url"))
        setupProduct(mockProductSnapshot2, "p2", Product("Muffin", "100", "url"))

        `when`(userRef.get()).thenReturn(mockUserTask)
        `when`(mockUserTask.isComplete).thenReturn(true)
        `when`(mockUserTask.isSuccessful).thenReturn(true)
        `when`(mockUserTask.result).thenReturn(mockUserSnapshot)
        `when`(mockUserTask.addOnCompleteListener(any<Executor>(), any<OnCompleteListener<DataSnapshot>>())).thenAnswer {
            it.getArgument<OnCompleteListener<DataSnapshot>>(1).onComplete(mockUserTask)
            mockUserTask
        }
        `when`(mockUserSnapshot.getValue(User::class.java)).thenReturn(User("Test", "User", "test@example.com", "Test Uni"))
    }

    private fun setupProduct(snapshot: DataSnapshot, key: String, product: Product) {
        `when`(snapshot.key).thenReturn(key)
        `when`(snapshot.getValue(Product::class.java)).thenReturn(product)
    }

    private fun setupPicasso() {
        `when`(mockPicasso.load(anyString())).thenReturn(mockRequestCreator)
        try {
            Picasso.setSingletonInstance(mockPicasso)
        } catch (e: Exception) {
            Picasso::class.java.getDeclaredField("singleton").apply {
                isAccessible = true
                set(null, null)
            }
            Picasso.setSingletonInstance(mockPicasso)
        }
    }

    @After
    fun tearDown() {
        try {
            Picasso::class.java.getDeclaredField("singleton").apply {
                isAccessible = true
                set(null, null)
            }
        } catch (_: Exception) {
        }
    }

    @Test
    fun `fragment displays list of products`() = launchFragment { fragment ->
        val recycler = fragment.view?.findViewById<RecyclerView>(R.id.rewardsRecycler)
        assertNotNull(recycler)
        assertEquals(2, recycler?.adapter?.itemCount)
    }

    @Test
    fun `student clicks product and is shown product details`() = launchFragment { fragment ->
        (fragment as RecyclerAdapter.OnProductListener).onProductClick(0)
        ShadowLooper.runUiThreadTasks()

        val dialog = ShadowDialog.getLatestDialog()
        assertNotNull("Popup should be visible", dialog)
        assertEquals("Coffee", dialog.findViewById<TextView>(R.id.popupText).text.toString())
        assertEquals("50 points", dialog.findViewById<TextView>(R.id.popupCost).text.toString())
    }

    @Test
    fun `student redeems reward successfully and updates points`() = launchFragment { fragment ->
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
    fun `student tries to redeem reward but insufficient funds shows toast`() = launchFragment { fragment ->
        // Setup user with 10 points (Product cost is 50)
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

    private fun setupPoints(points: String) {
        val captor = argumentCaptor<ValueEventListener>()
        verify(mockPointsRef, atLeastOnce()).addValueEventListener(captor.capture())
        `when`(mockPointsSnapshot.value).thenReturn(points)
        captor.allValues.last().onDataChange(mockPointsSnapshot)
        ShadowLooper.runUiThreadTasks()
    }
}
