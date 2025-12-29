package com.example.librewards.utils

import android.view.View
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.isClickable
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import com.sothree.slidinguppanel.SlidingUpPanelLayout
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

    fun expandSlidingPanel(): ViewAction = object : ViewAction {
        override fun getConstraints(): Matcher<View> = isAssignableFrom(SlidingUpPanelLayout::class.java)

        override fun getDescription(): String = "Expand sliding panel"

        override fun perform(uiController: UiController, view: View) {
            (view as SlidingUpPanelLayout).panelState = SlidingUpPanelLayout.PanelState.EXPANDED
        }
    }

    fun collapseSlidingPanel(): ViewAction = object : ViewAction {
        override fun getConstraints(): Matcher<View> = isAssignableFrom(SlidingUpPanelLayout::class.java)

        override fun getDescription(): String = "Close sliding panel"

        override fun perform(uiController: UiController, view: View) {
            (view as SlidingUpPanelLayout).panelState = SlidingUpPanelLayout.PanelState.COLLAPSED
        }
    }

    fun finishAllActivities() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val activities = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED)
            for (activity in activities) {
                if (!activity.isFinishing) {
                    activity.finish()
                }
            }
        }
    }
}
