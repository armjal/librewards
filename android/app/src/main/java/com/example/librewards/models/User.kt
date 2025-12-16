package com.example.librewards.models

import com.example.librewards.resources.universities

data class User(
    var firstname: String = "",
    var surname: String = "",
    var email: String = "",
    var university: String = "",
    var studying: String = "2",
    var redeemingReward: String = "2",
    var points: String = "0",
) {
    companion object {
        fun create(
            firstname: String,
            surname: String,
            email: String,
            university: String,
        ): User {
            require(firstname.isNotBlank()) { "First name cannot be blank." }
            require(surname.isNotBlank()) { "Last name cannot be blank." }
            require(email.contains("@") && email.contains(".")) { "Email is invalid." }
            require(universities.contains(university)) { "University is invalid." }

            return User(
                firstname = firstname,
                surname = surname,
                email = email,
                university = university,
            )
        }
    }

    fun toMap(): Map<String, Any> = mapOf(
        "firstname" to firstname,
        "surname" to surname,
        "email" to email,
        "university" to university,
        "studying" to studying,
        "redeemingReward" to redeemingReward,
        "points" to points,
    )
}
