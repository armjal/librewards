package com.example.librewards.ui.main

import android.os.SystemClock
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.librewards.utils.calculatePointsFromTime
import java.lang.Integer.parseInt

sealed class TimerState {
    object Started : TimerState()

    object Stopped : TimerState()

    object Neutral : TimerState()

    object Reset : TimerState()
}

data class TimerSummary(
    var minutesSpent: Int,
    val pointsEarned: Int,
    val newTotalPoints: Int,
)

class TimerViewModel(val mainSharedViewModel: MainSharedViewModel) : ViewModel() {
    var startTime: Long = 0
    var elapsedTime: Long = 0
    private var _timerSummary = MutableLiveData<TimerSummary>(null)
    val timerSummary: LiveData<TimerSummary> = _timerSummary
    private var _state = MutableLiveData<TimerState>(TimerState.Neutral)
    val state: LiveData<TimerState> get() = _state

    private val studyingStatusObserver: (String) -> Unit = { status ->
        handleStudyingStatusChange(status)
    }

    init {
        mainSharedViewModel.studyingStatus.observeForever(studyingStatusObserver)
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
        elapsedTime = 0
        startTime = SystemClock.elapsedRealtime()
        _state.value = TimerState.Started
    }

    fun stop() {
        if (_state.value == TimerState.Stopped) return
        mainSharedViewModel.updateStudying("0")
        elapsedTime = SystemClock.elapsedRealtime() - startTime
        startTime = 0

        val pointsEarned = calculatePointsFromTime(elapsedTime)
        mainSharedViewModel.addPoints(pointsEarned)
        generateTimerSummary(pointsEarned)

        _state.value = TimerState.Stopped
    }

    private fun generateTimerSummary(pointsEarned: Int) {
        val currentPoints = parseInt(mainSharedViewModel.userPoints.value!!)

        val minutesSpent = (elapsedTime / 1000 / 60).toInt()
        _timerSummary.value = TimerSummary(minutesSpent, pointsEarned, currentPoints + pointsEarned)
    }

    fun reset() {
        if (_state.value == TimerState.Reset) return
        mainSharedViewModel.updateStudying("2")
        elapsedTime = 0
        startTime = 0
        _state.value = TimerState.Reset
    }

    override fun onCleared() {
        super.onCleared()
        mainSharedViewModel.studyingStatus.removeObserver(studyingStatusObserver)
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
