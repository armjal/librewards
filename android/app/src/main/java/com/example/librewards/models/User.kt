package com.example.librewards.models

data class User(
    var firstname: String = "",
    var surname: String = "",
    var email: String = "",
    var university: String = "",
    var studying: String = "2",
    var redeemingReward: String = "2",
    var points: String = "0"
)
