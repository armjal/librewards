package com.example.librewards.utils

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.view.LayoutInflater
import android.widget.Toast
import androidx.core.content.ContextCompat.getDrawable
import androidx.core.graphics.drawable.toDrawable
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.widget.ViewPager2
import com.example.librewards.databinding.PopupLayoutBinding
import com.example.librewards.ui.adapters.ViewPagerAdapter
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

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

fun Activity.startLibRewardsActivity(activityToStart: Class<*>, isLogOut: Boolean = false) {
    val intent = Intent(this, activityToStart)
    if (isLogOut) {
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
    }
    startActivity(intent)
    finish()
}

fun ViewPager2.setupWithFragments(
    activity: FragmentActivity,
    tabLayout: TabLayout,
    fragments: List<FragmentExtended>,
) {
    val viewPagerAdapter = ViewPagerAdapter(activity)
    this.adapter = viewPagerAdapter
    viewPagerAdapter.addFragments(fragments)
    TabLayoutMediator(tabLayout, this) { tab, position ->
        tab.icon = getDrawable(activity, fragments[position].icon)
    }.attach()
}
