package com.shellmonger.apps.mynotes.models

import java.util.*

enum class TokenType {
    ID_TOKEN,
    ACCESS_TOKEN,
    REFRESH_TOKEN
}

class User(val id: String = UUID.randomUUID().toString(), var username: String = "") {
    val userAttributes: MutableMap<String,String> = HashMap()
    val tokens: MutableMap<TokenType,String> = HashMap()
}