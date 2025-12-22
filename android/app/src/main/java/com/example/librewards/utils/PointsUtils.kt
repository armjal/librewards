package com.example.librewards.utils

fun calculatePointsFromTime(totalTime: Long): Int {
    val pointsEarned: Int = when (totalTime) {
        in 0..10000 -> 0
        in 10001..29999 -> 10
        in 30000..59999 -> 50
        in 60000..119999 -> 75
        in 120000..179999 -> 125
        in 180000..259999 -> 225
        in 260000..399999 -> 400
        else -> 700
    }
    return pointsEarned
}
