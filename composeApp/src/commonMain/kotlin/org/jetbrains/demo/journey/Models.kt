package org.jetbrains.demo.journey

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.ExperimentalTime

/**
 * Transport types supported by the planner.
 */
enum class TransportType {
    Plane, Train, Bus, Car, Boat
}

/**
 * Immutable traveler model.
 */
data class Traveler(
    val id: String,
    val name: String,
)

/**
 * Form model kept in UiState.Success; all immutable for Compose stability.
 */
data class JourneyForm(
    val fromCity: String,
    val toCity: String,
    val transport: TransportType,
    val startDate: LocalDateTime,
    val endDate: LocalDateTime,
    val travelers: ImmutableList<Traveler>,
    val details: String?,
) {
    companion object {
        @OptIn(ExperimentalTime::class)
        fun empty(): JourneyForm {
            val now = Clock.System.now()
            return JourneyForm(
                fromCity = "",
                toCity = "",
                transport = TransportType.Train,
                startDate = (now + 2.days).toLocalDateTime(TimeZone.currentSystemDefault()),
                endDate = (now + 7.days).toLocalDateTime(TimeZone.currentSystemDefault()),
                travelers = persistentListOf(Traveler(id = "initial", name = "")), 
                details = null,
            )
        }
    }
}
