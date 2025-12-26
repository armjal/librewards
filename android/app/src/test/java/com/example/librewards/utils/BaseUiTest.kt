package com.example.librewards.utils

import android.app.Activity
import android.net.Uri
import android.os.Build
import androidx.test.core.app.ActivityScenario
import com.example.librewards.data.models.Product
import com.example.librewards.data.models.User
import com.google.android.gms.tasks.Task
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.squareup.picasso.Picasso
import com.squareup.picasso.RequestCreator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.Rule
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P], instrumentedPackages = ["androidx.loader.content"])
@ExperimentalCoroutinesApi
abstract class BaseUiTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @get:Rule
    val firebaseTestRule = FirebaseTestRule()

    @Mock
    lateinit var mockPointsRef: DatabaseReference

    @Mock
    lateinit var mockPointsSnapshot: DataSnapshot

    @Mock
    lateinit var mockUserTask: Task<DataSnapshot>

    @Mock
    lateinit var mockUserSnapshot: DataSnapshot

    @Mock
    lateinit var mockPicasso: Picasso

    @Mock
    lateinit var mockRequestCreator: RequestCreator

    @Before
    open fun setup() {
        MockitoAnnotations.openMocks(this)

        Mockito.`when`(firebaseTestRule.mockSpecificUserRef.child("points")).thenReturn(mockPointsRef)

        Mockito.`when`(firebaseTestRule.mockSpecificUserRef.get()).thenReturn(mockUserTask)
        mockTask(mockUserTask)
        Mockito.`when`(mockUserTask.result).thenReturn(mockUserSnapshot)

        Mockito.`when`(mockUserSnapshot.getValue(User::class.java))
            .thenReturn(User("Test", "User", "test@example.com", "Test Uni"))
    }

    fun setupPoints(points: String) {
        val captor = ArgumentCaptor.forClass(ValueEventListener::class.java)
        Mockito.verify(mockPointsRef, Mockito.atLeastOnce()).addValueEventListener(captor.capture())
        Mockito.`when`(mockPointsSnapshot.value).thenReturn(points)
        captor.allValues.last().onDataChange(mockPointsSnapshot)
        ShadowLooper.runUiThreadTasks()
    }

    fun setupUser(id: String, user: User?, mockRef: DatabaseReference = Mockito.mock(DatabaseReference::class.java)) {
        val mockTask = Mockito.mock(Task::class.java) as Task<DataSnapshot>
        val mockSnapshot = Mockito.mock(DataSnapshot::class.java)

        Mockito.`when`(firebaseTestRule.mockUsersRef.child(eq(id))).thenReturn(mockRef)
        Mockito.`when`(mockRef.get()).thenReturn(mockTask)
        mockTask(mockTask)
        Mockito.`when`(mockTask.result).thenReturn(mockSnapshot)
        Mockito.`when`(mockSnapshot.getValue(User::class.java)).thenReturn(user)
    }

    fun setupPicassoMock() {
        Mockito.`when`(mockPicasso.load(anyString())).thenReturn(mockRequestCreator)
        Mockito.`when`(mockPicasso.load(any(Uri::class.java))).thenReturn(mockRequestCreator)
        Mockito.`when`(mockRequestCreator.resize(anyInt(), anyInt())).thenReturn(mockRequestCreator)
        Mockito.`when`(mockRequestCreator.centerCrop()).thenReturn(mockRequestCreator)

        try {
            Picasso.setSingletonInstance(mockPicasso)
        } catch (e: Exception) {
            val singletonField = Picasso::class.java.getDeclaredField("singleton")
            singletonField.isAccessible = true
            singletonField.set(null, null)
            Picasso.setSingletonInstance(mockPicasso)
        }
    }

    fun resetPicassoMock() {
        try {
            val singletonField = Picasso::class.java.getDeclaredField("singleton")
            singletonField.isAccessible = true
            singletonField.set(null, null)
        } catch (_: Exception) {
        }
    }

    fun createMockProductSnapshot(key: String, product: Product): DataSnapshot = TestUtils.createMockProductSnapshot(key, product)

    fun <T> mockTask(task: Task<T>) {
        TestUtils.mockTask(task)
    }

    inline fun <reified T : Activity> launchActivity(crossinline block: (T) -> Unit) {
        ActivityScenario.launch(T::class.java).use { scenario ->
            scenario.onActivity { activity ->
                block(activity)
            }
        }
    }
}
