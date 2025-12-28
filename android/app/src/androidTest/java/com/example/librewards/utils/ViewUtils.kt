package com.example.librewards.utils

import android.view.View
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.matcher.ViewMatchers.isClickable
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.Matcher

object ViewUtils {
    fun forceClick(): ViewAction = object : ViewAction {
        override fun getConstraints(): Matcher<View?> = allOf(isClickable(), isEnabled(), isDisplayed())

        override fun getDescription(): String = "force click"

        override fun perform(uiController: UiController, view: View) {
            view.performClick() // perform click without checking view coordinates.
            uiController.loopMainThreadUntilIdle()
        }
    }
}
