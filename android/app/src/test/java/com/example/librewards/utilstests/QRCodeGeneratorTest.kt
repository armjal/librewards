package com.example.librewards.utilstests

import android.graphics.Bitmap
import com.example.librewards.utils.QRCodeGenerator
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.any
import org.mockito.Mockito.anyInt
import org.mockito.Mockito.mockStatic
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations

class QRCodeGeneratorTest {
    @Mock
    lateinit var mockBitmap: Bitmap

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
    }

    @Test
    fun `createQR returns non-null bitmap`() {
        val width = 100
        val height = 100
        val data = "Test Data"

        val mockedBitmapStatic = mockStatic(Bitmap::class.java)

        try {
            mockedBitmapStatic.`when`<Bitmap> {
                Bitmap.createBitmap(anyInt(), anyInt(), any(Bitmap.Config::class.java))
            }.thenReturn(mockBitmap)

            val generator = QRCodeGenerator()
            val result = generator.createQR(data, height, width)

            assertNotNull(result)

            // setPixel should be called width * height times.
            verify(mockBitmap, times(width * height)).setPixel(anyInt(), anyInt(), anyInt())
        } finally {
            mockedBitmapStatic.close()
        }
    }
}
