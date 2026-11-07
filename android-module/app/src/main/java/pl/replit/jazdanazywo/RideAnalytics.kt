package pl.replit.jazdanazywo

import kotlin.math.sqrt

/** Statystyki wyliczane wyłącznie z zapisanych pomiarów. */
object RideAnalytics {
    const val MIN_VALID_SAMPLES = 30
    const val MIN_MEASURED_DISTANCE_KM = 0.5
    private const val MAX_SEGMENT_GAP_MS = 5_000L
    private const val MIN_SEGMENT_GAP_MS = 250L

    fun analyze(samples: List<RideSample>, batteryCapacityWh: Double?): RideAnalysis {
        val latestBattery = samples.asSequence()
            .filter { it.batteryPercent.isValidBattery() }
            .maxByOrNull { it.timestampMs }
            ?.batteryPercent

        val profiles = (1..5).map { profile ->
            val profileSamples = samples.filter { it.profile == profile }
            var validSampleCount = 0
            var measuredDistanceKm = 0.0
            var weightedEnergyWh = 0.0
            var consumptionDistanceKm = 0.0
            var weightedSpeed = 0.0
            var speedDurationSeconds = 0.0

            profileSamples.forEach { sample ->
                if (sample.speedKmh.isValidSpeed() && sample.consumptionWhPerKm.isValidConsumption()) validSampleCount++
            }
            // Zestawiamy kolejne próbki z całego przejazdu, a nie tylko z wybranego
            // profilu: inaczej po odfiltrowaniu profilu można błędnie połączyć
            // odcinki jazdy rozdzielone zmianą profilu.
            samples.groupBy { it.rideId }.values.forEach { rideSamples ->
                val ordered = rideSamples.sortedBy { it.timestampMs }
                for (index in 1 until ordered.size) {
                    val previous = ordered[index - 1]
                    val current = ordered[index]
                    if (previous.profile != profile || current.profile != profile) continue
                    val gapMs = current.timestampMs - previous.timestampMs
                    if (gapMs !in MIN_SEGMENT_GAP_MS..MAX_SEGMENT_GAP_MS) continue
                    val previousSpeed = previous.speedKmh.validSpeedOrNull() ?: continue
                    val currentSpeed = current.speedKmh.validSpeedOrNull() ?: continue
                    val durationSeconds = gapMs / 1_000.0
                    val segmentSpeed = (previousSpeed + currentSpeed) / 2.0
                    val segmentDistanceKm = segmentSpeed * gapMs / 3_600_000.0
                    if (segmentDistanceKm <= 0.0) continue

                    measuredDistanceKm += segmentDistanceKm
                    weightedSpeed += segmentSpeed * durationSeconds
                    speedDurationSeconds += durationSeconds
                    val previousConsumption = previous.consumptionWhPerKm.validConsumptionOrNull() ?: continue
                    val currentConsumption = current.consumptionWhPerKm.validConsumptionOrNull() ?: continue
                    val segmentConsumption = (previousConsumption + currentConsumption) / 2.0
                    weightedEnergyWh += segmentConsumption * segmentDistanceKm
                    consumptionDistanceKm += segmentDistanceKm
                }
            }

            val averageConsumption = if (consumptionDistanceKm > 0.0) weightedEnergyWh / consumptionDistanceKm else null
            val hasEnoughData = validSampleCount >= MIN_VALID_SAMPLES && consumptionDistanceKm >= MIN_MEASURED_DISTANCE_KM && averageConsumption != null
            val averageSpeed = if (speedDurationSeconds > 0.0) weightedSpeed / speedDurationSeconds else null
            val validSpeeds = profileSamples.mapNotNull { it.speedKmh.validSpeedOrNull() }
            val speedMean = validSpeeds.average().takeIf { validSpeeds.isNotEmpty() }
            val speedVariability = if (speedMean != null) {
                sqrt(validSpeeds.sumOf { (it - speedMean) * (it - speedMean) } / validSpeeds.size)
            } else null

            // Pojemność baterii nie jest zgadywana. SOC->energia jest przybliżeniem liniowym.
            val safeCapacity = batteryCapacityWh?.takeIf { it.isFinite() && it > 0.0 }
            val safeConsumption = averageConsumption?.takeIf { it > 0.0 }
            val range = if (hasEnoughData && safeCapacity != null && safeConsumption != null && latestBattery != null) {
                safeCapacity * latestBattery / 100.0 / safeConsumption
            } else null

            ProfileStats(profile, validSampleCount, consumptionDistanceKm, averageConsumption, averageSpeed, speedVariability, range, hasEnoughData)
        }

        val eligible = profiles.filter { it.hasEnoughData && it.averageWhPerKm != null }
        val best = eligible.minByOrNull { it.averageWhPerKm ?: Double.POSITIVE_INFINITY }
        val recommendation = if (best == null) {
            "Za mało danych, aby uczciwie porównać profile. Dla każdego profilu potrzeba co najmniej $MIN_VALID_SAMPLES poprawnych próbek i ${MIN_MEASURED_DISTANCE_KM} km zarejestrowanej jazdy."
        } else {
            val consumption = "%.1f".format(java.util.Locale("pl", "PL"), best.averageWhPerKm)
            val speed = best.averageSpeedKmh?.let { "%.1f".format(java.util.Locale("pl", "PL"), it) }
            val variability = best.speedVariabilityKmh?.let { "%.1f".format(java.util.Locale("pl", "PL"), it) }
            val styleNote = if (speed != null && variability != null) {
                " przy zmierzonej średniej prędkości $speed km/h i odchyleniu prędkości $variability km/h"
            } else ""
            "Najniższe zarejestrowane zużycie ma profil ${best.profile}: $consumption Wh/km$styleNote. Przy tym samym stanie baterii oznacza to najlepszy zasięg spośród porównywalnych profili; rekomendacja wynika wyłącznie z zapisanych przejazdów."
        }
        return RideAnalysis(profiles, recommendation, best?.profile, latestBattery)
    }

    private fun Double?.isValidSpeed(): Boolean = validSpeedOrNull() != null
    private fun Double?.validSpeedOrNull(): Double? = this?.takeIf { it.isFinite() && it >= 0.0 && it <= 250.0 }
    private fun Double?.isValidBattery(): Boolean = this?.let { it.isFinite() && it in 0.0..100.0 } == true
    private fun Double?.isValidConsumption(): Boolean = validConsumptionOrNull() != null
    private fun Double?.validConsumptionOrNull(): Double? = this?.takeIf { it.isFinite() && it >= 0.0 && it <= 5_000.0 }
}
