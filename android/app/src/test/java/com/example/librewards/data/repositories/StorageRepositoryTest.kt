package com.example.librewards.data.repositories

import android.net.Uri
import com.example.librewards.data.models.ImageFile
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations

class StorageRepositoryTest {
    @Mock
    private lateinit var mockStorageRef: StorageReference

    @Mock
    private lateinit var mockUniversityRef: StorageReference

    @Mock
    private lateinit var mockImageRef: StorageReference

    @Mock
    private lateinit var mockUploadTask: UploadTask

    @Mock
    private lateinit var mockUri: Uri

    private lateinit var repository: StorageRepository

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        repository = StorageRepository(mockStorageRef)
        `when`(mockStorageRef.child(anyString())).thenReturn(mockUniversityRef)
        `when`(mockUniversityRef.child(anyString())).thenReturn(mockImageRef)
        `when`(mockImageRef.putFile(mockUri)).thenReturn(mockUploadTask)
    }

    @Test
    fun `setUniversityScope sets universityStorageRef`() {
        val university = "TestUni"
        repository.setUniversityScope(university)
        verify(mockStorageRef).child(university)
    }

    @Test
    fun `uploadImage calls putFile`() {
        repository.setUniversityScope("TestUni")

        val imageFile = ImageFile("test_image.jpg", mockUri)

        val result = repository.uploadImage(imageFile)

        verify(mockUniversityRef).child("images/" + imageFile.name)
        verify(mockImageRef).putFile(mockUri)
        assert(result == mockUploadTask)
    }
}
