package com.example.librewards.utilstests

import com.example.librewards.utils.calculatePointsFromTime
import org.junit.Assert.assertEquals
import org.junit.Test

class PointsUtilsTest {
    @Test
    fun `calculatePointsFromTime returns correct points`() {
        // 0..10000 -> 0
        assertEquals(0, calculatePointsFromTime(0))
        assertEquals(0, calculatePointsFromTime(10000))

        // 10001..29999 -> 10
        assertEquals(10, calculatePointsFromTime(10001))
        assertEquals(10, calculatePointsFromTime(29999))

        // 30000..59999 -> 50
        assertEquals(50, calculatePointsFromTime(30000))
        assertEquals(50, calculatePointsFromTime(59999))

        // 60000..119999 -> 75
        assertEquals(75, calculatePointsFromTime(60000))
        assertEquals(75, calculatePointsFromTime(119999))

        // 120000..179999 -> 125
        assertEquals(125, calculatePointsFromTime(120000))
        assertEquals(125, calculatePointsFromTime(179999))

        // 180000..259999 -> 225
        assertEquals(225, calculatePointsFromTime(180000))
        assertEquals(225, calculatePointsFromTime(259999))

        // 260000..399999 -> 400
        assertEquals(400, calculatePointsFromTime(260000))
        assertEquals(400, calculatePointsFromTime(399999))

        // else -> 700
        assertEquals(700, calculatePointsFromTime(400000))
    }
}
