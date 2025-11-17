package com.example.Rs232Validator.ViewModel

import android.app.Application
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class ValidatorViewModelFactory (
    private val application: Application,
    private val sharedPreferences: SharedPreferences
): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T{
        if(modelClass.isAssignableFrom(ValidatorViewModel::class.java)) {
            return ValidatorViewModel(application, sharedPreferences) as T
        }
        throw IllegalArgumentException("Unknown ViewModel")
    }
}