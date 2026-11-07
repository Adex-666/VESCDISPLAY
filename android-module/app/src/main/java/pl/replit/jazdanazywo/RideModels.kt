package pl.replit.jazdanazywo

data class RideTelemetry(
    val speedKmh: Double?,
    val batteryPercent: Double?,
    /** Podłącz odczyt RT Data o znanym znaczeniu; gdy nieznany lub brakujący, przekaż null. */
    val consumptionWhPerKm: Double?,
)

data class RideSession(val id: Long, val startedAtMs: Long, val endedAtMs: Long?)

data class RideSample(
    val rideId: Long,
    val timestampMs: Long,
    val speedKmh: Double?,
    val batteryPercent: Double?,
    val consumptionWhPerKm: Double?,
    val profile: Int,
)

data class RideSnapshot(val sessions: List<RideSession>, val samples: List<RideSample>)

data class ProfileStats(
    val profile: Int,
    val sampleCount: Int,
    val measuredDistanceKm: Double,
    val averageWhPerKm: Double?,
    val averageSpeedKmh: Double?,
    val speedVariabilityKmh: Double?,
    val estimatedRangeKm: Double?,
    val hasEnoughData: Boolean,
)

data class RideAnalysis(
    val profiles: List<ProfileStats>,
    val recommendation: String,
    val recommendedProfile: Int?,
    val latestBatteryPercent: Double?,
)

data class RideLiveUiState(
    val isConnected: Boolean = false,
    val telemetry: RideTelemetry? = null,
    val activeProfile: Int? = null,
    val isRecording: Boolean = false,
    val activeRideId: Long? = null,
    val sessions: List<RideSession> = emptyList(),
    val samples: List<RideSample> = emptyList(),
    val selectedRideId: Long? = null,
    val batteryCapacityWh: Double? = null,
    val notice: String? = null,
)
