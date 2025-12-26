package com.example.librewards.repositories

import com.example.librewards.data.models.User
import com.example.librewards.data.repositories.UserRepository
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import java.util.concurrent.Executor

@ExperimentalCoroutinesApi
class UserRepositoryTest {
    @Mock
    private lateinit var mockDbRef: DatabaseReference

    @Mock
    private lateinit var mockUserRef: DatabaseReference

    @Mock
    private lateinit var mockFieldRef: DatabaseReference

    @Mock
    private lateinit var mockVoidTask: Task<Void?>

    @Mock
    private lateinit var mockDataSnapshot: DataSnapshot

    @Mock
    private lateinit var mockUserTask: Task<DataSnapshot>

    private lateinit var repository: UserRepository

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        repository = UserRepository(mockDbRef)
        `when`(mockDbRef.child(anyString())).thenReturn(mockUserRef)
        `when`(mockUserRef.child(anyString())).thenReturn(mockFieldRef)

        `when`(mockVoidTask.isComplete).thenReturn(true)
        `when`(mockVoidTask.isCanceled).thenReturn(false)
        `when`(mockVoidTask.isSuccessful).thenReturn(true)
        `when`(mockVoidTask.exception).thenReturn(null)
        `when`(mockVoidTask.addOnCompleteListener(any<Executor>(), any())).thenAnswer { invocation ->
            val listener = invocation.getArgument<OnCompleteListener<Void?>>(1)
            listener.onComplete(mockVoidTask)
            mockVoidTask
        }
    }

    @Test
    fun `addUser calls setValue`() {
        val user = User("First", "Last", "test@example.com", "Uni")
        `when`(mockUserRef.setValue(user)).thenReturn(mockVoidTask)

        repository.addUser(user)

        verify(mockDbRef).child(anyString())
        verify(mockUserRef).setValue(user)
    }

    @Test
    fun `updateField calls setValue on field reference`() {
        val userId = "123"
        val field = "points"
        val value = "100"

        repository.updateField(userId, field, value)

        verify(mockDbRef).child(userId)
        verify(mockUserRef).child(field)
        verify(mockFieldRef).setValue(value)
    }

    @Test
    fun `getUser returns user object`() = runTest {
        val userId = "123"
        val user = User("First", "Last", "test@example.com", "Uni")

        `when`(mockUserRef.get()).thenReturn(mockUserTask)
        `when`(mockUserTask.isComplete).thenReturn(true)
        `when`(mockUserTask.isCanceled).thenReturn(false)
        `when`(mockUserTask.isSuccessful).thenReturn(true)
        `when`(mockUserTask.result).thenReturn(mockDataSnapshot)
        `when`(mockUserTask.addOnCompleteListener(any<Executor>(), any())).thenAnswer { invocation ->
            val listener = invocation.getArgument<OnCompleteListener<DataSnapshot>>(1)
            listener.onComplete(mockUserTask)
            mockUserTask
        }

        `when`(mockDataSnapshot.getValue(User::class.java)).thenReturn(user)

        val result = repository.getUser(userId)

        assertEquals(user, result)
        verify(mockDbRef).child(userId)
    }

    @Test
    fun `getUser returns null on exception`() = runTest {
        val userId = "123"

        `when`(mockUserRef.get()).thenThrow(RuntimeException("DB Error"))

        val result = repository.getUser(userId)

        assertNull(result)
    }

    @Test
    fun `listenForUserField emits value`() = runTest {
        val email = "test@example.com"
        val field = "points"
        val expectedValue = "50"

        `when`(mockDataSnapshot.value).thenReturn(expectedValue)

        val captor = argumentCaptor<ValueEventListener>()

        val job = launch {
            val result = repository.listenForUserField(email, field).first()
            assertEquals(expectedValue, result)
        }

        testScheduler.advanceUntilIdle()

        verify(mockFieldRef).addValueEventListener(captor.capture())
        captor.firstValue.onDataChange(mockDataSnapshot)

        job.cancel()
    }

    @Test
    fun `stopAllListeners removes listeners`() = runTest {
        val email = "test@example.com"
        val field = "points"

        val job = launch {
            repository.listenForUserField(email, field).collect {}
        }

        testScheduler.advanceUntilIdle()

        val captor = argumentCaptor<ValueEventListener>()
        verify(mockFieldRef).addValueEventListener(captor.capture())

        repository.stopAllListeners()

        verify(mockFieldRef).removeEventListener(captor.firstValue)

        job.cancel()
    }
}
