package com.example.librewards.ui.admin

import android.net.Uri
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.librewards.data.models.Product
import com.example.librewards.data.models.ProductEntry
import com.example.librewards.data.repositories.ProductRepository
import com.example.librewards.data.repositories.StorageRepository
import com.example.librewards.ui.main.UiEvent
import com.example.librewards.utils.MainDispatcherRule
import com.example.librewards.utils.TestUtils
import com.google.android.gms.tasks.Task
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any

@ExperimentalCoroutinesApi
class AdminRewardsViewModelTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Mock
    private lateinit var mockAdminSharedViewModel: AdminSharedViewModel

    @Mock
    private lateinit var mockProductRepo: ProductRepository

    @Mock
    private lateinit var mockStorageRepo: StorageRepository

    @Mock
    private lateinit var mockUploadTask: UploadTask.TaskSnapshot

    @Mock
    private lateinit var mockUploadTaskTask: UploadTask

    @Mock
    private lateinit var mockStorageRef: StorageReference

    @Mock
    private lateinit var mockUriTask: Task<Uri>

    @Mock
    private lateinit var mockUri: Uri

    private lateinit var viewModel: AdminRewardsViewModel

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)

        `when`(mockAdminSharedViewModel.productRepo).thenReturn(mockProductRepo)
        `when`(mockAdminSharedViewModel.storageRepo).thenReturn(mockStorageRepo)

        // Mock listenForProducts to avoid NPE in ViewModel init
        `when`(mockProductRepo.listenForProducts()).thenReturn(flowOf(emptyList()))

        // Mock Task<Uri> (Download URL)
        TestUtils.mockTask(mockUriTask)
        `when`(mockUriTask.result).thenReturn(mockUri)
        `when`(mockUri.toString()).thenReturn("http://download.url")

        // Mock UploadTask (which is a Task<UploadTask.TaskSnapshot>)
        TestUtils.mockTask(mockUploadTaskTask)
        `when`(mockUploadTaskTask.result).thenReturn(mockUploadTask)

        // Mock chain: upload -> snapshot -> storageRef -> downloadUrl -> task
        `when`(mockUploadTask.storage).thenReturn(mockStorageRef)
        `when`(mockStorageRef.downloadUrl).thenReturn(mockUriTask)

        viewModel = AdminRewardsViewModel(mockAdminSharedViewModel)
    }

    @Test
    fun `addProductEntry success emits success event`() = runTest {
        val product = Product("Name", "10")
        val uri = mockUri

        `when`(mockStorageRepo.uploadImage(any())).thenReturn(mockUploadTaskTask)

        val events = mutableListOf<UiEvent>()
        val job = launch {
            viewModel.addProductEntry(product, uri).collect { events.add(it) }
        }

        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        // Verify flow emissions
        assertTrue(events.any { it is UiEvent.Success && it.message == "Image successfully uploaded" })
        assertTrue(events.any { it is UiEvent.Success && it.message == "Product successfully added" })

        // Verify product was added to DB with correct URL
        verify(mockProductRepo).addProductToDb(any())
        // Note: product object was mutated inside viewmodel to include URL.

        job.cancel()
    }

    @Test
    fun `updateProductEntry success emits success`() = runTest {
        val entry = ProductEntry("1", Product("Name"))

        `when`(mockProductRepo.updateProduct(entry)).thenReturn(Unit)

        val events = mutableListOf<UiEvent>()
        viewModel.updateProductEntry(entry).collect { events.add(it) }

        assertTrue(events.any { it is UiEvent.Success })
        verify(mockProductRepo).updateProduct(entry)
    }

    @Test
    fun `deleteProductEntry success emits success`() = runTest {
        val id = "1"

        `when`(mockProductRepo.deleteProduct(id)).thenReturn(Unit)

        val events = mutableListOf<UiEvent>()
        viewModel.deleteProductEntry(id).collect { events.add(it) }

        assertTrue(events.any { it is UiEvent.Success })
        verify(mockProductRepo).deleteProduct(id)
    }
}
