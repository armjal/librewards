package com.example.librewards.utils

import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.mockStatic
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations

class DbUtilsTest {
    @Mock
    private lateinit var mockFirebaseDatabase: FirebaseDatabase

    @Mock
    private lateinit var mockDatabaseReference: DatabaseReference

    @Mock
    private lateinit var mockChildReference: DatabaseReference

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
    }

    @Test
    fun `getDbReference returns correct reference`() {
        val path = "users"

        val mockedStaticFirebaseDatabase = mockStatic(FirebaseDatabase::class.java)

        try {
            mockedStaticFirebaseDatabase.`when`<FirebaseDatabase> { FirebaseDatabase.getInstance() }
                .thenReturn(mockFirebaseDatabase)
            `when`(mockFirebaseDatabase.reference).thenReturn(mockDatabaseReference)
            `when`(mockDatabaseReference.child(path)).thenReturn(mockChildReference)

            val result = getDbReference(path)

            assertEquals(mockChildReference, result)
        } finally {
            mockedStaticFirebaseDatabase.close()
        }
    }
}
