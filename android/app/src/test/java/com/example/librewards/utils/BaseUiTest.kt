package com.example.librewards.utils

import android.app.Activity
import android.os.Build
import androidx.test.core.app.ActivityScenario
import com.example.librewards.data.models.User
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.Rule
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper
import java.util.concurrent.Executor

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

    @Before
    open fun setup() {
        MockitoAnnotations.openMocks(this)

        Mockito.`when`(firebaseTestRule.mockSpecificUserRef.child("points")).thenReturn(mockPointsRef)

        Mockito.`when`(firebaseTestRule.mockSpecificUserRef.get()).thenReturn(mockUserTask)
        Mockito.`when`(mockUserTask.isComplete).thenReturn(true)
        Mockito.`when`(mockUserTask.isSuccessful).thenReturn(true)
        Mockito.`when`(mockUserTask.result).thenReturn(mockUserSnapshot)
        mockTask(mockUserTask)
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

    fun <T> mockTask(task: Task<T>) {
        Mockito.`when`(task.addOnCompleteListener(any<Executor>(), any())).thenAnswer {
            val listener = it.getArgument<OnCompleteListener<T>>(1)
            listener.onComplete(task)
            task
        }
        Mockito.`when`(task.addOnCompleteListener(any())).thenAnswer {
            val listener = it.getArgument<OnCompleteListener<T>>(0)
            listener.onComplete(task)
            task
        }
    }

    inline fun <reified T : Activity> launchActivity(crossinline block: (T) -> Unit) {
        ActivityScenario.launch(T::class.java).use { scenario ->
            scenario.onActivity { activity ->
                block(activity)
            }
        }
    }
}
