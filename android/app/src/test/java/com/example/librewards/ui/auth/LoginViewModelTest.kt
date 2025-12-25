package com.example.librewards.ui.auth

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GetTokenResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import utils.MainDispatcherRule
import utils.getOrAwaitValue
import java.util.concurrent.Executor

@ExperimentalCoroutinesApi
class LoginViewModelTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Mock
    private lateinit var mockAuth: FirebaseAuth

    @Mock
    private lateinit var mockUser: FirebaseUser

    @Mock
    private lateinit var mockAuthResultTask: Task<AuthResult>

    @Mock
    private lateinit var mockTokenTask: Task<GetTokenResult>

    @Mock
    private lateinit var mockTokenResult: GetTokenResult

    private lateinit var viewModel: LoginViewModel

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)

        // Mock default behaviors
        `when`(mockAuth.currentUser).thenReturn(null)

        // Mock task listeners
        `when`(mockAuthResultTask.addOnCompleteListener(any<Executor>(), any())).thenAnswer { invocation ->
            val listener = invocation.getArgument<OnCompleteListener<AuthResult>>(1)
            listener.onComplete(mockAuthResultTask)
            mockAuthResultTask
        }
        `when`(mockAuthResultTask.addOnCompleteListener(any())).thenAnswer { invocation ->
            val listener = invocation.getArgument<OnCompleteListener<AuthResult>>(0)
            listener.onComplete(mockAuthResultTask)
            mockAuthResultTask
        }

        `when`(mockTokenTask.addOnSuccessListener(any())).thenAnswer { invocation ->
            val listener = invocation.getArgument<OnSuccessListener<GetTokenResult>>(0)
            listener.onSuccess(mockTokenResult)
            mockTokenTask
        }

        viewModel = LoginViewModel(mockAuth)
    }

    @Test
    fun `setIsAdmin updates isAdmin`() {
        `when`(mockAuth.currentUser).thenReturn(mockUser)
        `when`(mockUser.getIdToken(true)).thenReturn(mockTokenTask)

        // Case 1: Admin
        `when`(mockTokenResult.claims).thenReturn(mapOf("admin" to true))
        viewModel.setIsAdmin()
        assertEquals(true, viewModel.isAdmin.getOrAwaitValue())

        // Case 2: Not Admin
        `when`(mockTokenResult.claims).thenReturn(emptyMap())
        viewModel.setIsAdmin()
        assertEquals(false, viewModel.isAdmin.getOrAwaitValue())
    }

    @Test
    fun `login success updates flow and checks admin`() = runTest {
        val email = "test@example.com"
        val password = "password"

        `when`(mockAuth.signInWithEmailAndPassword(email, password)).thenReturn(mockAuthResultTask)
        `when`(mockAuthResultTask.isSuccessful).thenReturn(true)
        `when`(mockAuth.currentUser).thenReturn(mockUser)
        `when`(mockUser.getIdToken(true)).thenReturn(mockTokenTask)
        `when`(mockTokenResult.claims).thenReturn(mapOf("admin" to true))

        val result = viewModel.login(email, password).first()

        assertEquals(LoginStatus.Successful, result)
        // setIsAdmin is called on success
        assertEquals(true, viewModel.isAdmin.getOrAwaitValue())
    }

    @Test
    fun `login failure updates flow`() = runTest {
        val email = "test@example.com"
        val password = "password"

        `when`(mockAuth.signInWithEmailAndPassword(email, password)).thenReturn(mockAuthResultTask)
        `when`(mockAuthResultTask.isSuccessful).thenReturn(false)
        `when`(mockAuthResultTask.exception).thenReturn(Exception("Login Failed"))

        val result = viewModel.login(email, password).first()

        assertEquals(LoginStatus.Failed, result)
    }

    @Test
    fun `logout signs out and updates state`() {
        `when`(mockAuth.currentUser).thenReturn(mockUser)

        viewModel.logout()

        verify(mockAuth).signOut()
        assertEquals(LoginStatus.LoggedOut, viewModel.loginState.getOrAwaitValue())
    }
}
