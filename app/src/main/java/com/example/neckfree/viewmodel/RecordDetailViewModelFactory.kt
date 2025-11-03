package com.example.neckfree.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class RecordDetailViewModelFactory(private val application: Application, private val recordId: Long) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RecordDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RecordDetailViewModel(application, recordId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
