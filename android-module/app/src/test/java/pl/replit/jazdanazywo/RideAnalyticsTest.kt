package pl.replit.jazdanazywo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RideAnalyticsTest {
    @Test
    fun averageConsumptionIsCalculatedFromRecordedDistanceWeightedSegments() {
        val result = RideAnalytics.analyze(samples(profile = 1, count = 61, consumption = 24.0), batteryCapacityWh = 600.0)
        val profile = result.profiles.first { it.profile == 1 }
        assertTrue(profile.hasEnoughData)
        assertEquals(24.0, profile.averageWhPerKm!!, 0.001)
        assertEquals(0.6, profile.measuredDistanceKm, 0.01)
        assertEquals(20.0, profile.estimatedRangeKm!!, 0.1)
    }

    @Test
    fun recommendationChoosesLowestWhPerKmAmongProfilesWithEnoughData() {
        val samples = samples(profile = 1, count = 61, consumption = 20.0) +
            samples(profile = 2, count = 61, consumption = 35.0, rideId = 2, startMs = 100_000)
        val result = RideAnalytics.analyze(samples, batteryCapacityWh = 500.0)
        assertEquals(1, result.recommendedProfile)
        assertTrue(result.recommendation.contains("Profil 1"))
        assertTrue(result.profiles.first { it.profile == 2 }.hasEnoughData)
    }

    @Test
    fun insufficientDataDoesNotProduceRecommendationOrEstimatedRange() {
        val result = RideAnalytics.analyze(samples(profile = 4, count = 12, consumption = 18.0), batteryCapacityWh = 500.0)
        val profile = result.profiles.first { it.profile == 4 }
        assertFalse(profile.hasEnoughData)
        assertNull(result.recommendedProfile)
        assertNull(profile.estimatedRangeKm)
        assertTrue(result.recommendation.contains("Za mało danych"))
    }

    private fun samples(profile: Int, count: Int, consumption: Double, rideId: Long = profile.toLong(), startMs: Long = 0L): List<RideSample> =
        (0 until count).map { index ->
            RideSample(rideId, startMs + index * 1_000L, 36.0, 80.0, consumption, profile)
        }
}
