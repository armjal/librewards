package com.example.librewards.ui.auth

import android.content.Intent
import android.widget.EditText
import android.widget.TextView
import com.example.librewards.R
import com.example.librewards.data.models.User
import com.example.librewards.ui.admin.AdminActivity
import com.example.librewards.ui.main.MainActivity
import com.example.librewards.utils.BaseUiTest
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.GetTokenResult
import com.google.firebase.database.DatabaseReference
import junit.framework.TestCase.assertEquals
import org.junit.Test
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.any
import org.mockito.Mockito.`when`
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowLooper
import org.robolectric.shadows.ShadowToast

class LoginActivityTest : BaseUiTest() {
    @Mock
    private lateinit var mockAuthResultTask: Task<AuthResult>

    @Mock
    private lateinit var mockAuthResult: AuthResult

    @Mock
    private lateinit var mockTokenTask: Task<GetTokenResult>

    @Mock
    private lateinit var mockTokenResult: GetTokenResult

    override fun setup() {
        super.setup()
        `when`(firebaseTestRule.mockFirebaseAuth.signInWithEmailAndPassword(any(), any()))
            .thenReturn(mockAuthResultTask)

        mockTask(mockAuthResultTask)
        `when`(mockAuthResultTask.result).thenReturn(mockAuthResult)

        // Mock token retrieval for admin check
        `when`(firebaseTestRule.mockFirebaseUser.getIdToken(anyBoolean())).thenReturn(mockTokenTask)
        `when`(mockTokenTask.addOnSuccessListener(any())).thenAnswer {
            val listener = it.getArgument<OnSuccessListener<GetTokenResult>>(0)
            listener.onSuccess(mockTokenResult)
            mockTokenTask
        }
        `when`(mockTokenResult.claims).thenReturn(mapOf())
    }

    @Test
    fun `clicking register button opens RegisterActivity`() = launchActivity<LoginActivity> {
        it.findViewById<TextView>(R.id.registerButton).performClick()
        val expectedIntent = Intent(it, RegisterActivity::class.java)
        val actualIntent = shadowOf(it).nextStartedActivity
        assertEquals(expectedIntent.component, actualIntent.component)
    }

    @Test
    fun `user tries to submit with empty fields shows toast`() = launchActivity<LoginActivity> {
        it.findViewById<TextView>(R.id.loginButton).performClick()
        assertEquals(
            it.getString(R.string.error_msg_empty_fields),
            ShadowToast.getTextOfLatestToast(),
        )
    }

    @Test
    fun `student login success navigates to MainActivity`() {
        `when`(mockAuthResultTask.isSuccessful).thenReturn(true)
        val user = User("Test", "User", "test@example.com", "Test Uni")
        `when`(mockUserSnapshot.getValue(User::class.java)).thenReturn(user)
        `when`(firebaseTestRule.mockSpecificUserRef.child("admin")).thenReturn(Mockito.mock(DatabaseReference::class.java))

        launchActivity<LoginActivity> {
            it.findViewById<EditText>(R.id.loginEmail).setText("test@example.com")
            it.findViewById<EditText>(R.id.loginPassword).setText("password")
            it.findViewById<TextView>(R.id.loginButton).performClick()

            ShadowLooper.runUiThreadTasks()
            ShadowLooper.idleMainLooper()

            val expectedIntent = Intent(it, MainActivity::class.java)
            val actualIntent = shadowOf(it).nextStartedActivity

            assertEquals(expectedIntent.component, actualIntent.component)
        }
    }

    @Test
    fun `admin login success navigates to AdminActivity`() {
        `when`(mockAuthResultTask.isSuccessful).thenReturn(true)
        `when`(mockTokenResult.claims).thenReturn(mapOf("admin" to true))

        launchActivity<LoginActivity> {
            it.findViewById<EditText>(R.id.loginEmail).setText("admin@example.com")
            it.findViewById<EditText>(R.id.loginPassword).setText("password")
            it.findViewById<TextView>(R.id.loginButton).performClick()

            ShadowLooper.runUiThreadTasks()
            ShadowLooper.idleMainLooper()

            val expectedIntent = Intent(it, AdminActivity::class.java)
            val actualIntent = shadowOf(it).nextStartedActivity

            assertEquals(expectedIntent.component, actualIntent.component)
        }
    }

    @Test
    fun `user login failure shows toast`() {
        `when`(mockAuthResultTask.isSuccessful).thenReturn(false)
        `when`(mockAuthResultTask.exception).thenReturn(Exception("Auth failed"))

        launchActivity<LoginActivity> {
            it.findViewById<EditText>(R.id.loginEmail).setText("test@example.com")
            it.findViewById<EditText>(R.id.loginPassword).setText("password")
            it.findViewById<TextView>(R.id.loginButton).performClick()

            ShadowLooper.runUiThreadTasks()
            ShadowLooper.idleMainLooper()

            assertEquals(
                it.getString(R.string.auth_failed),
                ShadowToast.getTextOfLatestToast(),
            )
        }
    }
}
