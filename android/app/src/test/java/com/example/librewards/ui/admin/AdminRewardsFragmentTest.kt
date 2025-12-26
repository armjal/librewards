package com.example.librewards.ui.admin

import android.content.Intent
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import com.example.librewards.R
import com.example.librewards.data.models.Product
import com.example.librewards.ui.adapters.RecyclerAdapter
import com.example.librewards.utils.BaseUiTest
import com.google.android.gms.tasks.Task
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import org.junit.After
import org.junit.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.atLeastOnce
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.kotlin.argumentCaptor
import org.robolectric.RuntimeEnvironment
import org.robolectric.shadows.ShadowDialog.getLatestDialog
import org.robolectric.shadows.ShadowLooper
import org.robolectric.shadows.ShadowToast

class AdminRewardsFragmentTest : BaseUiTest() {
    @Mock private lateinit var mockProductsRef: DatabaseReference

    @Mock private lateinit var mockStorageRef: StorageReference

    @Mock private lateinit var mockProductRef: DatabaseReference

    @Mock private lateinit var mockUploadTask: UploadTask

    @Mock private lateinit var mockTaskSnapshot: UploadTask.TaskSnapshot

    @Mock private lateinit var mockProductsSnapshot: DataSnapshot

    override fun setup() {
        super.setup()
        setupFirebaseMocks()
        setupPicassoMock()
    }

    @After
    fun tearDown() {
        resetPicassoMock()
    }

    @Test
    fun `clicking add product opens popup`() = launchFragment { fragment ->
        clickAddProduct(fragment)
        assertDialogVisible(R.id.productName, R.id.productCost, R.id.chooseButton, R.id.uploadButton)
    }

    @Test
    fun `uploading product without image shows error toast`() = launchFragment { fragment ->
        clickAddProduct(fragment)

        fillProductDetails("New Product", "100")
        clickUploadProduct()

        assertToastMessage(fragment.getString(R.string.choose_image))
    }

    @Test
    fun `clicking choose image button launches image picker`() = launchFragment { fragment ->
        clickAddProduct(fragment)

        clickChooseImage()

        val nextStartedActivity = org.robolectric.Shadows.shadowOf(RuntimeEnvironment.getApplication()).nextStartedActivityForResult
        assertNotNull(nextStartedActivity)
        assertEquals(Intent.ACTION_GET_CONTENT, nextStartedActivity.intent.action)
        assertEquals("image/*", nextStartedActivity.intent.type)
    }

    @Test
    fun `clicking product shows manage popup`() {
        setupProductList()
        launchFragment { fragment ->
            clickProductAtPosition(fragment, 0)
            assertDialogVisible(R.id.manageProductName, R.id.manageProductCost, R.id.updateButton, R.id.deleteButton)
        }
    }

    @Test
    fun `delete product shows success toast`() {
        setupProductList()
        launchFragment { fragment ->
            clickProductAtPosition(fragment, 0)
            assertDialogVisible(R.id.manageProductName, R.id.manageProductCost, R.id.updateButton, R.id.deleteButton)

            mockDeleteProduct("prod1")
            clickDeleteProduct()

            assertToastMessage("Product successfully deleted")
        }
    }

    private fun launchFragment(block: (AdminRewardsFragment) -> Unit) {
        ActivityScenario.launch(AdminActivity::class.java).use {
            it.onActivity { activity ->
                ShadowLooper.runUiThreadTasks()
                val fragment = AdminRewardsFragment()
                activity.supportFragmentManager.beginTransaction().replace(android.R.id.content, fragment).commitNow()

                triggerProductDataLoading()

                block(fragment)
            }
        }
    }

    private fun triggerProductDataLoading() {
        val captor = argumentCaptor<ValueEventListener>()
        verify(mockProductRef, atLeastOnce()).addValueEventListener(captor.capture())
        captor.allValues.last().onDataChange(mockProductsSnapshot)
        ShadowLooper.runUiThreadTasks()
    }

    // UI Actions

    private fun clickAddProduct(fragment: AdminRewardsFragment) {
        fragment.view?.findViewById<ImageView>(R.id.addAProduct)?.performClick()
        ShadowLooper.runUiThreadTasks()
    }

    private fun clickChooseImage() {
        val dialog = getLatestDialog()
        dialog.findViewById<Button>(R.id.chooseButton).performClick()
        ShadowLooper.runUiThreadTasks()
    }

    private fun fillProductDetails(name: String, cost: String) {
        val dialog = getLatestDialog()
        dialog.findViewById<EditText>(R.id.productName).setText(name)
        dialog.findViewById<EditText>(R.id.productCost).setText(cost)
    }

    private fun clickUploadProduct() {
        val dialog = getLatestDialog()
        dialog.findViewById<Button>(R.id.uploadButton).performClick()
        ShadowLooper.runUiThreadTasks()
    }

    private fun clickProductAtPosition(fragment: AdminRewardsFragment, position: Int) {
        val recyclerView = fragment.view?.findViewById<RecyclerView>(R.id.adminRewardsRecycler)
        assertNotNull(recyclerView)
        (fragment as RecyclerAdapter.OnProductListener).onProductClick(position)
        ShadowLooper.runUiThreadTasks()
    }

    private fun clickDeleteProduct() {
        val dialog = getLatestDialog()
        dialog.findViewById<Button>(R.id.deleteButton).performClick()
        ShadowLooper.runUiThreadTasks()
    }

    // Assertions

    private fun assertDialogVisible(vararg viewIds: Int) {
        val latestDialog = getLatestDialog()
        assertNotNull(latestDialog)
        assert(latestDialog.isShowing)
        viewIds.forEach { viewId ->
            assertNotNull("View with ID $viewId should be visible in dialog", latestDialog.findViewById(viewId))
        }
    }

    private fun assertToastMessage(message: String) {
        assertEquals(message, ShadowToast.getTextOfLatestToast())
    }

    // Setup & Mocks

    private fun setupFirebaseMocks() {
        `when`(firebaseTestRule.mockRootRef.child("products")).thenReturn(mockProductsRef)
        `when`(mockProductsRef.child(anyString())).thenReturn(mockProductRef)
        `when`(firebaseTestRule.mockStorageRef.child(anyString())).thenReturn(mockStorageRef)
        `when`(mockStorageRef.putFile(any())).thenReturn(mockUploadTask)
        mockTask(mockUploadTask)
        `when`(mockUploadTask.result).thenReturn(mockTaskSnapshot)
        `when`(mockProductsSnapshot.children).thenReturn(emptyList())
    }

    private fun setupProductList() {
        val prod1Snapshot = createMockProductSnapshot("prod1", Product("Coffee", "50", "url"))
        `when`(mockProductsSnapshot.children).thenReturn(listOf(prod1Snapshot))
        `when`(mockProductsSnapshot.value).thenReturn(true)
    }

    private fun mockDeleteProduct(productId: String) {
        `when`(mockProductRef.child(productId)).thenReturn(mockProductRef)
        val mockDeleteTask = Mockito.mock(Task::class.java) as Task<Void>
        `when`(mockProductRef.removeValue()).thenReturn(mockDeleteTask)
        mockTask(mockDeleteTask)
    }
}
