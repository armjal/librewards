package com.example.librewards.viewmodels


sealed class UiEvent {
    data class Success(val message: String) : UiEvent()
    data class Failure(val message: String) : UiEvent()
}