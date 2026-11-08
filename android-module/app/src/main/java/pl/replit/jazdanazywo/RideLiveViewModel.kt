package pl.replit.jazdanazywo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Przyjmuje już zdekodowane RT Data; nie implementuje ani nie zgaduje protokołu BLE VESC. */
class RideLiveViewModel(private val repository: RideRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(RideLiveUiState())
    val uiState = _uiState.asStateFlow()
    private var lastSampleAtMs = 0L

    init {
        viewModelScope.launch {
            repository.finishOpenRides(System.currentTimeMillis())
            refreshFromStorage()
        }
    }

    fun setConnectionState(connected: Boolean) {
        _uiState.update { it.copy(isConnected = connected, telemetry = if (connected) it.telemetry else null, activeProfile = if (connected) it.activeProfile else null) }
        if (!connected && _uiState.value.isRecording) stopRecording()
    }

    fun onTelemetry(telemetry: RideTelemetry?) {
        if (!_uiState.value.isConnected) return
        _uiState.update { it.copy(telemetry = telemetry) }
        persistSample(force = false)
    }

    /** Wywołaj po potwierdzeniu/odczytaniu faktycznego profilu, nie tylko po tapnięciu. */
    fun onActiveProfileChanged(profile: Int) {
        if (profile !in 1..5) return
        _uiState.update { it.copy(activeProfile = profile) }
        persistSample(force = true)
    }

    fun startRecording() {
        val current = _uiState.value
        if (!current.isConnected || current.activeProfile == null || current.isRecording) {
            _uiState.update { it.copy(notice = if (!it.isConnected) "Połącz się ze sterownikiem VESC, aby rozpocząć zapis." else "Pobierz aktualny profil ze sterownika, aby rozpocząć zapis.") }
            return
        }
        viewModelScope.launch {
            val startedAt = System.currentTimeMillis()
            val rideId = repository.createRide(startedAt)
            _uiState.update {
                it.copy(isRecording = true, activeRideId = rideId, sessions = listOf(RideSession(rideId, startedAt, null)) + it.sessions, selectedRideId = null, notice = null)
            }
            lastSampleAtMs = 0L
            persistSample(force = true)
        }
    }

    fun stopRecording() {
        val rideId = _uiState.value.activeRideId ?: return
        _uiState.update { it.copy(isRecording = false, activeRideId = null) }
        viewModelScope.launch {
            repository.finishRide(rideId, System.currentTimeMillis())
            refreshFromStorage()
        }
    }

    fun selectRide(rideId: Long?) { _uiState.update { it.copy(selectedRideId = rideId) } }

    fun saveBatteryCapacityWh(value: Double?) {
        val normalized = value?.takeIf { it.isFinite() && it > 0.0 }
        _uiState.update { it.copy(batteryCapacityWh = normalized) }
        viewModelScope.launch { repository.saveBatteryCapacityWh(normalized) }
    }

    private fun persistSample(force: Boolean) {
        val current = _uiState.value
        val rideId = current.activeRideId ?: return
        val telemetry = current.telemetry ?: return
        val profile = current.activeProfile ?: return
        val now = System.currentTimeMillis()
        if (!force && now - lastSampleAtMs < SAMPLE_INTERVAL_MS) return
        lastSampleAtMs = now
        val sample = RideSample(rideId, now, telemetry.speedKmh, telemetry.batteryPercent, telemetry.consumptionWhPerKm, profile)
        _uiState.update { it.copy(samples = it.samples + sample) }
        viewModelScope.launch { repository.addSample(sample) }
    }

    private suspend fun refreshFromStorage() {
        val snapshot = repository.loadSnapshot()
        val capacity = repository.loadBatteryCapacityWh()
        _uiState.update { it.copy(sessions = snapshot.sessions, samples = snapshot.samples, batteryCapacityWh = capacity) }
    }

    companion object { private const val SAMPLE_INTERVAL_MS = 1_000L }
}

class RideLiveViewModelFactory(private val repository: RideRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RideLiveViewModel::class.java)) return RideLiveViewModel(repository) as T
        throw IllegalArgumentException("Nieobsługiwany ViewModel: ${modelClass.name}")
    }
}
