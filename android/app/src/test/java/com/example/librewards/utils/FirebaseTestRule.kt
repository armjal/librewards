package com.example.librewards.utils

import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.mockito.ArgumentMatchers
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.Mockito.mockStatic
import org.mockito.Mockito.`when`

class FirebaseTestRule : TestWatcher() {
    private lateinit var mockedAuth: MockedStatic<FirebaseAuth>
    private lateinit var mockedDb: MockedStatic<FirebaseDatabase>
    private lateinit var mockedApp: MockedStatic<FirebaseApp>

    lateinit var mockFirebaseAuth: FirebaseAuth
    lateinit var mockFirebaseUser: FirebaseUser
    lateinit var mockFirebaseDatabase: FirebaseDatabase
    lateinit var mockRootRef: DatabaseReference
    lateinit var mockUsersRef: DatabaseReference
    lateinit var mockSpecificUserRef: DatabaseReference

    override fun starting(description: Description?) {
        super.starting(description)

        mockFirebaseAuth = Mockito.mock(FirebaseAuth::class.java)
        mockFirebaseUser = Mockito.mock(FirebaseUser::class.java)
        mockFirebaseDatabase = Mockito.mock(FirebaseDatabase::class.java)
        mockRootRef = Mockito.mock(DatabaseReference::class.java)
        mockUsersRef = Mockito.mock(DatabaseReference::class.java)
        mockSpecificUserRef = Mockito.mock(DatabaseReference::class.java)

        mockedAuth = mockStatic(FirebaseAuth::class.java)
        mockedAuth.`when`<FirebaseAuth> { FirebaseAuth.getInstance() }.thenReturn(mockFirebaseAuth)
        `when`(mockFirebaseAuth.currentUser).thenReturn(mockFirebaseUser)
        `when`(mockFirebaseUser.email).thenReturn("test@example.com")

        mockedDb = mockStatic(FirebaseDatabase::class.java)
        mockedDb.`when`<FirebaseDatabase> { FirebaseDatabase.getInstance() }.thenReturn(mockFirebaseDatabase)
        `when`(mockFirebaseDatabase.reference).thenReturn(mockRootRef)

        `when`(mockRootRef.child("users")).thenReturn(mockUsersRef)
        `when`(mockUsersRef.child(ArgumentMatchers.anyString())).thenReturn(mockSpecificUserRef)

        mockedApp = mockStatic(FirebaseApp::class.java)
        mockedApp.`when`<FirebaseApp> { FirebaseApp.getInstance() }.thenReturn(
            Mockito.mock(FirebaseApp::class.java),
        )
    }

    override fun finished(description: Description?) {
        super.finished(description)
        mockedAuth.close()
        mockedDb.close()
        mockedApp.close()
    }
}
