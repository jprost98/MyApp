package com.example.myapp.data

data class User(
    var userId: Int = 0,
    var firstName: String? = null,
    var lastName: String? = null,
    var email: String? = null,
    var fbUserId: String? = null,
    var password: String? = null
)
