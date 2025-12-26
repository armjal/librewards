package com.example.librewards.repositories

import com.example.librewards.data.models.Product
import com.example.librewards.data.models.ProductEntry
import com.example.librewards.data.repositories.ProductRepository
import com.example.librewards.utils.TestUtils
import com.google.android.gms.tasks.Task
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor

@ExperimentalCoroutinesApi
class ProductRepositoryTest {
    @Mock
    private lateinit var mockDbRef: DatabaseReference

    @Mock
    private lateinit var mockUniversityRef: DatabaseReference

    @Mock
    private lateinit var mockProductRef: DatabaseReference

    @Mock
    private lateinit var mockVoidTask: Task<Void>

    private lateinit var repository: ProductRepository

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        repository = ProductRepository(mockDbRef)
        `when`(mockDbRef.child(anyString())).thenReturn(mockUniversityRef)
        `when`(mockUniversityRef.child(anyString())).thenReturn(mockProductRef)

        TestUtils.mockTask(mockVoidTask)
    }

    @Test
    fun `setUniversityScope sets universityRef`() {
        val university = "TestUni"
        repository.setUniversityScope(university)
        verify(mockDbRef).child(university)
    }

    @Test
    fun `listenForProducts emits data`() = runTest {
        repository.setUniversityScope("TestUni")

        val mockSnapshot = mock(DataSnapshot::class.java)
        val product = Product("Name", "10", "Url")
        val mockChildSnapshot = TestUtils.createMockProductSnapshot("1", product)

        `when`(mockSnapshot.children).thenReturn(listOf(mockChildSnapshot))

        val captor = argumentCaptor<ValueEventListener>()

        val job = launch {
            repository.listenForProducts().collect {
                assertEquals(1, it.size)
                assertEquals("Name", it[0].product.productName)
                assertEquals("1", it[0].id)
            }
        }

        testScheduler.advanceUntilIdle()

        verify(mockUniversityRef).addValueEventListener(captor.capture())
        captor.firstValue.onDataChange(mockSnapshot)

        job.cancel()
    }

    @Test
    fun `addProductToDb calls setValue`() = runTest {
        repository.setUniversityScope("TestUni")
        val entry = ProductEntry("1", Product("Name", "Cost", "Url"))

        `when`(mockProductRef.setValue(entry.product)).thenReturn(mockVoidTask)

        repository.addProductToDb(entry)

        verify(mockProductRef).setValue(entry.product)
    }

    @Test
    fun `updateProduct calls updateChildren`() = runTest {
        repository.setUniversityScope("TestUni")
        val entry = ProductEntry("1", Product("Name", "Cost", "Url"))

        `when`(mockProductRef.updateChildren(any())).thenReturn(mockVoidTask)

        repository.updateProduct(entry)

        verify(mockProductRef).updateChildren(entry.product.toMap())
    }

    @Test
    fun `deleteProduct calls removeValue`() = runTest {
        repository.setUniversityScope("TestUni")
        val productId = "1"

        `when`(mockProductRef.removeValue()).thenReturn(mockVoidTask)

        repository.deleteProduct(productId)

        verify(mockProductRef).removeValue()
    }

    @Test
    fun `stopAllListeners removes listeners`() = runTest {
        repository.setUniversityScope("TestUni")

        val job = launch {
            repository.listenForProducts().collect {}
        }

        testScheduler.advanceUntilIdle()

        val captor = argumentCaptor<ValueEventListener>()
        verify(mockUniversityRef).addValueEventListener(captor.capture())

        repository.stopAllListeners()

        verify(mockUniversityRef).removeEventListener(captor.firstValue)

        job.cancel()
    }
}
