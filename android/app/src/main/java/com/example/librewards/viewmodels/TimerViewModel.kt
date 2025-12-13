package com.example.librewards.viewmodels

import android.os.SystemClock
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

sealed class TimerState {
    object Started : TimerState()

    object Stopped : TimerState()

    object Neutral : TimerState()

    object Reset : TimerState()
}

class TimerViewModel(val mainSharedViewModel: MainSharedViewModel) : ViewModel() {
    var startTime: Long = 0
    private var _elapsedTime = MutableLiveData<Long>(0)
    val elapsedTime: LiveData<Long> = _elapsedTime
    private var _state = MutableLiveData<TimerState>(TimerState.Neutral)
    val state: LiveData<TimerState> get() = _state

    init {
        mainSharedViewModel.studyingStatus.observeForever { status -> handleStudyingStatusChange(status) }
    }

    private fun handleStudyingStatusChange(status: String?) {
        when (status) {
            "0" -> stop()
            "1" -> start()
        }
    }

    fun start() {
        if (_state.value == TimerState.Started) return
        mainSharedViewModel.updateStudying("1")
        _elapsedTime.value = 0
        startTime = SystemClock.elapsedRealtime()
        _state.value = TimerState.Started
    }

    fun stop() {
        if (_state.value == TimerState.Stopped) return
        mainSharedViewModel.updateStudying("0")
        _elapsedTime.value = SystemClock.elapsedRealtime() - startTime
        startTime = 0
        _state.value = TimerState.Stopped
    }

    fun reset() {
        if (_state.value == TimerState.Reset) return
        mainSharedViewModel.updateStudying("2")
        _elapsedTime.value = 0
        startTime = 0
        _state.value = TimerState.Reset
    }
}

class TimerViewModelFactory(
    private val mainSharedViewModel: MainSharedViewModel,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T = if (modelClass.isAssignableFrom(TimerViewModel::class.java)) {
        @Suppress("UNCHECKED_CAST")
        TimerViewModel(mainSharedViewModel) as T
    } else {
        throw IllegalArgumentException("ViewModel Not Found")
    }
}
