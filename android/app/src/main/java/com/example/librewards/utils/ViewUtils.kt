package com.example.librewards.utils

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.widget.Toast
import androidx.core.graphics.drawable.toDrawable
import com.example.librewards.databinding.PopupLayoutBinding

fun toastMessage(context: Context, message: String) {
    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
}

fun showPopup(context: Context, text: String?) {
    val popup = Dialog(context)
    popup.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
    val popupLayoutBinding = PopupLayoutBinding.inflate(LayoutInflater.from(context))
    popup.setContentView(popupLayoutBinding.root)
    popupLayoutBinding.popupText.text = text
    popupLayoutBinding.closeBtn.setOnClickListener { popup.dismiss() }
    popup.show()
}
