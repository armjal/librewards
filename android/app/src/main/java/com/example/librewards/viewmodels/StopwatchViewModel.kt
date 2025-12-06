package com.example.librewards.viewmodels

import android.os.SystemClock
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

sealed class StopwatchState {
    object Started : StopwatchState()

    object Stopped : StopwatchState()

    object Neutral : StopwatchState()
}

class StopwatchViewModel : ViewModel() {
    var startTime: Long = 0
    var elapsedTime: Long = 0
    private var _state = MutableLiveData<StopwatchState>(StopwatchState.Neutral)
    val state: LiveData<StopwatchState> get() = _state

    fun start() {
        if (_state.value != StopwatchState.Started) {
            elapsedTime = 0
            startTime = SystemClock.elapsedRealtime()
            _state.value = StopwatchState.Started
        }
    }

    fun stop() {
        if (_state.value != StopwatchState.Stopped) {
            elapsedTime = SystemClock.elapsedRealtime() - startTime
            startTime = 0
            _state.value = StopwatchState.Stopped
        }
    }
}
