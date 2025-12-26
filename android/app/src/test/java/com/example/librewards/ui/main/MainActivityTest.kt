package com.example.librewards.ui.main

import android.content.Intent
import android.os.Build
import android.view.View
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.viewpager2.widget.ViewPager2
import com.example.librewards.R
import com.example.librewards.data.models.User
import com.example.librewards.ui.auth.LoginActivity
import com.example.librewards.utils.MainDispatcherRule
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.MockedStatic
import org.mockito.Mockito.any
import org.mockito.Mockito.mockStatic
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper.runUiThreadTasks
import java.util.concurrent.Executor

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P], instrumentedPackages = ["androidx.loader.content"])
@ExperimentalCoroutinesApi
class MainActivityTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Mock
    private lateinit var mockFirebaseAuth: FirebaseAuth

    @Mock
    private lateinit var mockFirebaseUser: FirebaseUser

    @Mock
    private lateinit var mockFirebaseDatabase: FirebaseDatabase

    @Mock
    private lateinit var mockRootRef: DatabaseReference

    @Mock
    private lateinit var mockUsersRef: DatabaseReference

    @Mock
    private lateinit var mockProductsRef: DatabaseReference

    @Mock
    private lateinit var mockSpecificUserRef: DatabaseReference

    @Mock
    private lateinit var mockUserSnapshotTask: Task<DataSnapshot>

    @Mock
    private lateinit var mockDataSnapshot: DataSnapshot

    @Mock
    private lateinit var mockFirebaseApp: FirebaseApp

    private lateinit var mockedAuth: MockedStatic<FirebaseAuth>
    private lateinit var mockedDb: MockedStatic<FirebaseDatabase>
    private lateinit var mockedApp: MockedStatic<FirebaseApp>

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)

        // Mock Firebase Auth Static
        mockedAuth = mockStatic(FirebaseAuth::class.java)
        mockedAuth.`when`<FirebaseAuth> { FirebaseAuth.getInstance() }.thenReturn(mockFirebaseAuth)
        `when`(mockFirebaseAuth.currentUser).thenReturn(mockFirebaseUser)
        `when`(mockFirebaseUser.email).thenReturn("test@example.com")

        // Mock Firebase Database Static
        mockedDb = mockStatic(FirebaseDatabase::class.java)
        mockedDb.`when`<FirebaseDatabase> { FirebaseDatabase.getInstance() }.thenReturn(mockFirebaseDatabase)
        `when`(mockFirebaseDatabase.reference).thenReturn(mockRootRef)

        // Mock DB references structure
        `when`(mockRootRef.child("users")).thenReturn(mockUsersRef)
        `when`(mockRootRef.child("products")).thenReturn(mockProductsRef)
        `when`(mockUsersRef.child(ArgumentMatchers.anyString())).thenReturn(mockSpecificUserRef)

        // Mock getUser() call chain
        `when`(mockSpecificUserRef.get()).thenReturn(mockUserSnapshotTask)

        // Mock Task completion
        `when`(mockUserSnapshotTask.isComplete).thenReturn(true)
        `when`(mockUserSnapshotTask.isSuccessful).thenReturn(true)
        `when`(mockUserSnapshotTask.result).thenReturn(mockDataSnapshot)
        `when`(mockUserSnapshotTask.addOnCompleteListener(any<Executor>(), any())).thenAnswer { invocation ->
            val listener = invocation.getArgument<OnCompleteListener<DataSnapshot>>(1)
            listener.onComplete(mockUserSnapshotTask)
            mockUserSnapshotTask
        }

        // Mock Firebase App (for Firebase.auth extension if needed)
        mockedApp = mockStatic(FirebaseApp::class.java)
        mockedApp.`when`<FirebaseApp> { FirebaseApp.getInstance() }.thenReturn(mockFirebaseApp)
    }

    @After
    fun tearDown() {
        mockedAuth.close()
        mockedDb.close()
        mockedApp.close()
    }

    @Test
    fun `activity launches and sets up fragments`() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assert(activity != null)
                val viewPager = activity.findViewById<ViewPager2>(R.id.viewPager)
                assertEquals(2, viewPager.adapter?.itemCount)
            }
        }
    }

    @Test
    fun `clicking profile image logs out user`() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val profileImage = activity.findViewById<View>(R.id.profileImage)
                profileImage.performClick()

                verify(mockFirebaseAuth).signOut()

                val expectedIntent = Intent(activity, LoginActivity::class.java)
                val actualIntent = shadowOf(activity).nextStartedActivity
                assertEquals(expectedIntent.component, actualIntent.component)
            }
        }
    }

    @Test
    fun `username updates when user data received`() {
        val user = User("John", "Doe", "test@example.com", "Test Uni")
        `when`(mockDataSnapshot.getValue(User::class.java)).thenReturn(user)

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val usernameText = activity.findViewById<TextView>(R.id.username)

                // Let the UI update loop run
                runUiThreadTasks()

                assertEquals("John Doe", usernameText.text.toString())
            }
        }
    }

    @Test
    fun `panel slide updates ui`() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                // Simulate panel slide
                activity.mainSharedViewModel.onPanelSlide(0.5f)

                val shadowOverlay = activity.findViewById<View>(R.id.shadowOverlay)
                val profileImage = activity.findViewById<View>(R.id.profileImage)
                val viewPager = activity.findViewById<ViewPager2>(R.id.viewPager)

                // shadowAlpha = (0.5 * 0.6).coerceIn(0, 0.6) = 0.3
                assertEquals(0.3f, shadowOverlay.alpha, 0.01f)

                // isPanelActive = 0.5 > 0.01 = true
                assertFalse(profileImage.isClickable)
                assertFalse(viewPager.isUserInputEnabled)

                // Test panel closed
                activity.mainSharedViewModel.onPanelSlide(0.0f)
                assertEquals(0.0f, shadowOverlay.alpha, 0.01f)
                assertTrue(profileImage.isClickable)
                assertTrue(viewPager.isUserInputEnabled)
            }
        }
    }
}
