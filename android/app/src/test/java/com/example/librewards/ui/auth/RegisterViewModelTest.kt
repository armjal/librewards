package com.example.librewards.ui.auth

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.librewards.data.models.User
import com.example.librewards.data.repositories.UserRepository
import com.example.librewards.utils.MainDispatcherRule
import com.example.librewards.utils.getOrAwaitValue
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import java.util.concurrent.Executor

@ExperimentalCoroutinesApi
class RegisterViewModelTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Mock
    private lateinit var mockAuth: FirebaseAuth

    @Mock
    private lateinit var mockUserRepo: UserRepository

    @Mock
    private lateinit var mockAuthResultTask: Task<AuthResult>

    @Mock
    private lateinit var mockVoidTask: Task<Void?>

    private lateinit var viewModel: RegisterViewModel

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        viewModel = RegisterViewModel(mockAuth, mockUserRepo)

        // Mock task behavior
        `when`(mockAuthResultTask.addOnCompleteListener(any<Executor>(), any())).thenAnswer { invocation ->
            val listener = invocation.getArgument<OnCompleteListener<AuthResult>>(1)
            listener.onComplete(mockAuthResultTask)
            mockAuthResultTask
        }
        // Overload without executor
        `when`(mockAuthResultTask.addOnCompleteListener(any())).thenAnswer { invocation ->
            val listener = invocation.getArgument<OnCompleteListener<AuthResult>>(0)
            listener.onComplete(mockAuthResultTask)
            mockAuthResultTask
        }
    }

    @Test
    fun `signUp success updates status to Registered`() {
        val user = User("First", "Last", "test@example.com", "Uni")
        val password = "password"

        `when`(mockAuth.createUserWithEmailAndPassword(user.email, password)).thenReturn(mockAuthResultTask)
        `when`(mockAuthResultTask.isSuccessful).thenReturn(true)
        val mockFirebaseUser = mock(FirebaseUser::class.java)
        `when`(mockAuth.currentUser).thenReturn(mockFirebaseUser)

        `when`(mockUserRepo.addUser(user)).thenReturn(mockVoidTask)

        viewModel.signUp(user, password)

        val status = viewModel.registerStatus.getOrAwaitValue()
        assertEquals(RegisterStatus.Registered, status)

        verify(mockUserRepo).addUser(user)
    }

    @Test
    fun `signUp success but current user is null updates status to RegisteredWithoutLogin`() {
        val user = User("First", "Last", "test@example.com", "Uni")
        val password = "password"

        `when`(mockAuth.createUserWithEmailAndPassword(user.email, password)).thenReturn(mockAuthResultTask)
        `when`(mockAuthResultTask.isSuccessful).thenReturn(true)
        `when`(mockAuth.currentUser).thenReturn(null)

        viewModel.signUp(user, password)

        val status = viewModel.registerStatus.getOrAwaitValue()
        assertEquals(RegisterStatus.RegisteredWithoutLogin, status)
    }

    @Test
    fun `signUp failure updates status to Failed`() {
        val user = User("First", "Last", "test@example.com", "Uni")
        val password = "password"

        `when`(mockAuth.createUserWithEmailAndPassword(user.email, password)).thenReturn(mockAuthResultTask)
        `when`(mockAuthResultTask.isSuccessful).thenReturn(false)
        `when`(mockAuthResultTask.exception).thenReturn(Exception("Auth Failed"))

        viewModel.signUp(user, password)

        val status = viewModel.registerStatus.getOrAwaitValue()
        assertEquals(RegisterStatus.Failed, status)
    }
}
