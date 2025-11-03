package com.example.neckfree

import android.app.Application

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        PoseAnalyzer.init(this)
    }
}