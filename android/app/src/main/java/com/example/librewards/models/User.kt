package com.example.librewards.models

data class User(
    var firstname: String = "",
    var surname: String = "",
    var email: String = "",
    var university: String = "",
    var studying: String = "2",
    var redeemingReward: String = "2",
    var points: String = "0"
){
    fun toMap(): Map<String, Any> {
        return mapOf(
            "firstname" to firstname,
            "surname" to surname,
            "email" to email,
            "university" to university,
            "studying" to studying,
            "redeemingReward" to redeemingReward,
            "points" to points)
    }
}
