package com.example.librewards.models

import android.net.Uri

data class ImageFile(
    var name: String = "",
    var uri : Uri? = null,
    var downloadUrl: Uri? = null
)
