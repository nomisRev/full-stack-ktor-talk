@file:OptIn(ExperimentalMaterial3Api::class)

package org.jetbrains.demo.journey

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.demo.JourneyForm
import org.jetbrains.demo.TransportType
import org.jetbrains.demo.Traveler
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun JourneySpannerRoute(
    viewModel: JourneyPlannerViewModel = koinViewModel(),
    onSubmit: (JourneyForm) -> Unit
) {
    val form by viewModel.state.collectAsStateWithLifecycle()

    JourneySpannerScreen(
        fromCity = { form.fromCity },
        toCity = { form.toCity },
        transport = { form.transport },
        startDate = { form.startDate },
        endDate = { form.endDate },
        travelers = { form.travelers },
        details = { form.details },
        onFromCityChange = viewModel::updateFromCity,
        onToCityChange = viewModel::updateToCity,
        onTransportChange = viewModel::updateTransport,
        onStartDatePicked = viewModel::setStartDate,
        onEndDatePicked = viewModel::setEndDate,
        onTravelerAdd = viewModel::addTraveler,
        onTravelerRemove = viewModel::removeTraveler,
        onTravelerNameChange = viewModel::updateTravelerName,
        onDetailsChange = viewModel::updateDetails,
        onSubmit = { viewModel.submit(onSubmit) },
    )
}

@OptIn(ExperimentalTime::class)
@Composable
fun JourneySpannerScreen(
    fromCity: () -> String,
    toCity: () -> String,
    transport: () -> TransportType,
    startDate: () -> LocalDateTime,
    endDate: () -> LocalDateTime,
    travelers: () -> ImmutableList<Traveler>,
    details: () -> String?,
    onFromCityChange: (String) -> Unit,
    onToCityChange: (String) -> Unit,
    onTransportChange: (TransportType) -> Unit,
    onStartDatePicked: (LocalDate) -> Unit,
    onEndDatePicked: (LocalDate) -> Unit,
    onTravelerAdd: () -> Unit,
    onTravelerRemove: (id: String) -> Unit,
    onTravelerNameChange: (id: String, name: String) -> Unit,
    onDetailsChange: (String?) -> Unit,
    onSubmit: () -> Unit,
) {
    val isValid by remember {
        derivedStateOf {
            val from = fromCity().isNotBlank()
            val to = toCity().isNotBlank()
            val timesOk = startDate() <= endDate()
            val travelerOk = travelers().all { it.name.isNotBlank() } && travelers().isNotEmpty()
            from && to && timesOk && travelerOk
        }
    }

    var showRangePicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Journey Planner", style = MaterialTheme.typography.headlineSmall)

        OutlinedTextField(
            value = fromCity(),
            onValueChange = onFromCityChange,
            label = { Text("From city") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = toCity(),
            onValueChange = onToCityChange,
            label = { Text("To city") },
            modifier = Modifier.fillMaxWidth()
        )

        TransportSelector(
            selected = transport,
            onSelected = onTransportChange
        )

        // Date range selector (dates only)
        Text("Dates", style = MaterialTheme.typography.titleMedium)
        OutlinedButton(onClick = { showRangePicker = true }) {
            val endDate = endDate().date
            Text("${startDate().date.day} until ${endDate.day}-${endDate.month.number}-${endDate.year}")
        }

        if (showRangePicker) {
            DatePickerDialog(
                onDismissRequest = { showRangePicker = false },
                confirmButton = { TextButton(onClick = { showRangePicker = false }) { Text("OK") } }
            ) {
                val state = rememberDateRangePickerState(
                    initialSelectedStartDateMillis = startDate().toEpochMillis(),
                    initialSelectedEndDateMillis = endDate().toEpochMillis()
                )
                DateRangePicker(state = state)
                LaunchedEffect(state.selectedStartDateMillis, state.selectedEndDateMillis) {
                    val startMillis = state.selectedStartDateMillis
                    val endMillis = state.selectedEndDateMillis
                    if (startMillis != null) {
                        val s = Instant.fromEpochMilliseconds(startMillis)
                            .toLocalDateTime(TimeZone.currentSystemDefault()).date
                        onStartDatePicked(s)
                    }
                    if (endMillis != null) {
                        val e = Instant.fromEpochMilliseconds(endMillis)
                            .toLocalDateTime(TimeZone.currentSystemDefault()).date
                        onEndDatePicked(e)
                    }
                }
            }
        }

        TravelersList(
            travelers = travelers,
            onAdd = onTravelerAdd,
            onRemove = onTravelerRemove,
            onNameChange = onTravelerNameChange
        )

        OutlinedTextField(
            value = details() ?: "",
            onValueChange = { onDetailsChange(it.ifBlank { null }) },
            label = { Text("Details (optional)") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = onSubmit,
            enabled = isValid,
            modifier = Modifier.fillMaxWidth()
        ) { Text("Submit") }
    }
}

@Composable
private fun TransportSelector(
    selected: () -> TransportType,
    onSelected: (TransportType) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected().name,
            onValueChange = {},
            readOnly = true,
            label = { Text("Transport") },
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            TransportType.entries.forEach { type ->
                DropdownMenuItem(
                    text = { Text(type.name) },
                    onClick = {
                        onSelected(type)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalTime::class)
private fun LocalDateTime.toEpochMillis(): Long {
    // Best-effort conversion using current system zone
    return this.toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
}
