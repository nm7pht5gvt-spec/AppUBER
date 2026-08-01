package com.tuapp.tripadvisor.ui.config

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tuapp.tripadvisor.data.preferences.PreferencesRepository
import com.tuapp.tripadvisor.domain.model.UserPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ConfigUiState(
    val pricePerKmInput: String = "",
    val earningsPerHourInput: String = "",
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val errorMessage: String? = null
)

class ConfigViewModel(
    private val repository: PreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConfigUiState())
    val uiState: StateFlow<ConfigUiState> = _uiState

    init {
        viewModelScope.launch {
            repository.userPreferencesFlow.collect { prefs ->
                _uiState.value = _uiState.value.copy(
                    pricePerKmInput = formatForInput(prefs.minPricePerKm),
                    earningsPerHourInput = formatForInput(prefs.minEarningsPerHour)
                )
            }
        }
    }

    fun onPricePerKmChanged(value: String) {
        _uiState.value = _uiState.value.copy(
            pricePerKmInput = value,
            errorMessage = null,
            saveSuccess = false
        )
    }

    fun onEarningsPerHourChanged(value: String) {
        _uiState.value = _uiState.value.copy(
            earningsPerHourInput = value,
            errorMessage = null,
            saveSuccess = false
        )
    }

    fun saveAndActivate(onSuccess: () -> Unit) {
        val price = _uiState.value.pricePerKmInput.toDoubleOrNull()
        val earnings = _uiState.value.earningsPerHourInput.toDoubleOrNull()

        if (price == null || earnings == null || price <= 0.0 || earnings <= 0.0) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Ingresa valores numéricos válidos mayores a 0."
            )
            return
        }

        _uiState.value = _uiState.value.copy(isSaving = true, errorMessage = null)

        viewModelScope.launch {
            repository.savePreferences(
                UserPreferences(minPricePerKm = price, minEarningsPerHour = earnings)
            )
            _uiState.value = _uiState.value.copy(isSaving = false, saveSuccess = true)
            onSuccess()
        }
    }

    private fun formatForInput(value: Double): String {
        return if (value == value.toLong().toDouble()) {
            value.toLong().toString()
        } else {
            value.toString()
        }
    }
}

class ConfigViewModelFactory(
    private val repository: PreferencesRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ConfigViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ConfigViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
