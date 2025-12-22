package com.example.librewards.utils

import com.google.common.hash.Hashing
import java.nio.charset.StandardCharsets

fun generateIdFromKey(key: String): String = Hashing.sipHash24().hashString(key, StandardCharsets.UTF_8)
    .toString()
