package com.example.librewards.utils

import com.example.librewards.data.models.Product
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.google.firebase.database.DataSnapshot
import org.mockito.Mockito
import org.mockito.kotlin.any
import java.util.concurrent.Executor

object TestUtils {
    fun <T> mockTask(task: Task<T>, result: T? = null) {
        if (result != null) {
            Mockito.`when`(task.result).thenReturn(result)
        }

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
        Mockito.`when`(task.addOnSuccessListener(any())).thenAnswer {
            val listener = it.getArgument<OnSuccessListener<T>>(0)
            // Use the provided result if available, otherwise task.result
            val taskResult = if (result != null) result else task.result
            listener.onSuccess(taskResult)
            task
        }
        Mockito.`when`(task.addOnSuccessListener(any<Executor>(), any())).thenAnswer {
            val listener = it.getArgument<OnSuccessListener<T>>(1)
            val taskResult = if (result != null) result else task.result
            listener.onSuccess(taskResult)
            task
        }
        Mockito.`when`(task.addOnCanceledListener(any())).thenReturn(task)
        Mockito.`when`(task.addOnFailureListener(any())).thenReturn(task)

        // Specific for Void tasks which might be checked for properties
        Mockito.`when`(task.isComplete).thenReturn(true)
        Mockito.`when`(task.isSuccessful).thenReturn(true)
        Mockito.`when`(task.exception).thenReturn(null)
        Mockito.`when`(task.isCanceled).thenReturn(false)
    }

    fun createMockProductSnapshot(key: String, product: Product): DataSnapshot {
        val snapshot = Mockito.mock(DataSnapshot::class.java)
        Mockito.`when`(snapshot.key).thenReturn(key)
        Mockito.`when`(snapshot.getValue(Product::class.java)).thenReturn(product)

        val idSnapshot = Mockito.mock(DataSnapshot::class.java)
        Mockito.`when`(idSnapshot.value).thenReturn(key)
        Mockito.`when`(snapshot.child("id")).thenReturn(idSnapshot)
        return snapshot
    }
}
