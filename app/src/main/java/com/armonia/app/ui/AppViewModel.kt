package com.armonia.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.armonia.app.ArmoniaApp
import com.armonia.app.domain.model.Instrument
import com.armonia.app.domain.model.Level
import com.armonia.app.domain.model.Profile
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface AppState {
    data object Loading : AppState
    data object NeedsOnboarding : AppState
    data class Ready(val profile: Profile) : AppState
}

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val profileRepository = (application as ArmoniaApp).container.profileRepository

    val state: StateFlow<AppState> = profileRepository.profile
        .map { profile ->
            if (profile == null) {
                AppState.NeedsOnboarding
            } else {
                AppState.Ready(profile)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AppState.Loading
        )

    fun completeOnboarding(instrument: Instrument, level: Level) {
        viewModelScope.launch {
            profileRepository.save(Profile(instrument, level))
        }
    }

    fun updateProfile(profile: Profile) {
        viewModelScope.launch {
            profileRepository.save(profile)
        }
    }
}
