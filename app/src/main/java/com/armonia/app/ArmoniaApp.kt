package com.armonia.app

import android.app.Application
import android.content.Context
import com.armonia.app.data.AppDatabase
import com.armonia.app.data.ProfileRepository
import com.armonia.app.data.ProgressRepository

class ArmoniaApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

class AppContainer(context: Context) {

    private val database = AppDatabase.get(context)

    val profileRepository = ProfileRepository(context)
    val progressRepository = ProgressRepository(database.progressDao())
}
