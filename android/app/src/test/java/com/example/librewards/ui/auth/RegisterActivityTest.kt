package com.example.librewards.ui.auth

import android.content.Intent
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import com.example.librewards.R
import com.example.librewards.data.resources.universities
import com.example.librewards.utils.BaseUiTest
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import junit.framework.TestCase.assertEquals
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.Mockito.any
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowLooper
import org.robolectric.shadows.ShadowToast

class RegisterActivityTest : BaseUiTest() {
    @Mock
    private lateinit var mockAuthResultTask: Task<AuthResult>

    @Mock
    private lateinit var mockAuthResult: AuthResult

    @Mock
    private lateinit var mockVoidTask: Task<Void>

    override fun setup() {
        super.setup()
        `when`(firebaseTestRule.mockFirebaseAuth.createUserWithEmailAndPassword(any(), any()))
            .thenReturn(mockAuthResultTask)

        mockTask(mockAuthResultTask)
        `when`(mockAuthResultTask.result).thenReturn(mockAuthResult)

        // Mock database set value
        `when`(firebaseTestRule.mockUsersRef.child(ArgumentMatchers.anyString())).thenReturn(firebaseTestRule.mockSpecificUserRef)
        `when`(firebaseTestRule.mockSpecificUserRef.setValue(any())).thenReturn(mockVoidTask)
    }

    @Test
    fun `back button returns to LoginActivity`() = launchActivity<RegisterActivity> {
        it.findViewById<ImageView>(R.id.backToLogin).performClick()
        val expectedIntent = Intent(it, LoginActivity::class.java)
        val actualIntent = shadowOf(it).nextStartedActivity
        assertEquals(expectedIntent.component, actualIntent.component)
    }

    @Test
    fun `user inputs empty fields shows toast`() = launchActivity<RegisterActivity> {
        it.findViewById<TextView>(R.id.registerHereButton).performClick()
        assertEquals(
            it.getString(R.string.error_msg_empty_fields),
            ShadowToast.getTextOfLatestToast(),
        )
    }

    @Test
    fun `spinner selection updates selected university`() = launchActivity<RegisterActivity> {
        val spinner = it.findViewById<Spinner>(R.id.registrationSpinner)
        val randomUniversityIndex = (1 until universities.size).random()
        spinner.setSelection(randomUniversityIndex)

        ShadowLooper.runUiThreadTasks()

        assertEquals(
            it.getString(R.string.selected_uni, universities[randomUniversityIndex - 1]),
            ShadowToast.getTextOfLatestToast(),
        )
    }

    @Test
    fun `successful registration navigates to LoginActivity`() {
        `when`(mockAuthResultTask.isSuccessful).thenReturn(true)

        launchActivity<RegisterActivity> {
            // Fill details
            it.findViewById<EditText>(R.id.registrationFirstName).setText("John")
            it.findViewById<EditText>(R.id.registrationLastName).setText("Doe")
            it.findViewById<EditText>(R.id.registrationEmail).setText("john@example.com")
            it.findViewById<EditText>(R.id.registrationPassword).setText("password123")
            it.findViewById<Spinner>(R.id.registrationSpinner).setSelection(1)

            it.findViewById<TextView>(R.id.registerHereButton).performClick()

            ShadowLooper.runUiThreadTasks()

            // Verify DB interaction
            verify(firebaseTestRule.mockUsersRef).child(any())

            // Verify navigation
            val expectedIntent = Intent(it, LoginActivity::class.java)
            val actualIntent = shadowOf(it).nextStartedActivity
            assertEquals(expectedIntent.component, actualIntent.component)
        }
    }

    @Test
    fun `failed registration shows toast`() {
        `when`(mockAuthResultTask.isSuccessful).thenReturn(false)
        `when`(mockAuthResultTask.exception).thenReturn(Exception("Registration failed"))

        launchActivity<RegisterActivity> {
            // Fill details
            it.findViewById<EditText>(R.id.registrationFirstName).setText("John")
            it.findViewById<EditText>(R.id.registrationLastName).setText("Doe")
            it.findViewById<EditText>(R.id.registrationEmail).setText("john@example.com")
            it.findViewById<EditText>(R.id.registrationPassword).setText("password123")
            it.findViewById<Spinner>(R.id.registrationSpinner).setSelection(1)

            // Flush spinner toast
            ShadowLooper.runUiThreadTasks()
            ShadowToast.reset()

            it.findViewById<TextView>(R.id.registerHereButton).performClick()

            ShadowLooper.runUiThreadTasks()

            assertEquals(
                it.getString(R.string.auth_failed),
                ShadowToast.getTextOfLatestToast(),
            )
        }
    }
}
