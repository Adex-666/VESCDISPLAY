package pl.replit.jazdanazywo

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val ScreenBackground = Color(0xFF101114)
private val CardBackground = Color(0xFF1B1D22)
private val MutedText = Color(0xFFA7A9B0)
private val OrangeAccent = Color(0xFFFF8A26)
private val ProfileColors = listOf(
    Color(0xFF56C7F2), Color(0xFF78D17B), Color(0xFFFFC857), Color(0xFFFF8A26), Color(0xFFEF647C),
)

/** Dodaj jako osobny ekran/trasę w istniejącej nawigacji aplikacji. */
@Composable
fun RideLiveScreen(viewModel: RideLiveViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var capacityText by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(state.batteryCapacityWh) {
        capacityText = state.batteryCapacityWh?.let { formatNumber(it, 0) }.orEmpty()
    }
    val visibleSamples = remember(state.samples, state.selectedRideId) {
        if (state.selectedRideId == null) state.samples else state.samples.filter { it.rideId == state.selectedRideId }
    }
    val analysis = remember(visibleSamples, state.batteryCapacityWh) {
        RideAnalytics.analyze(visibleSamples, state.batteryCapacityWh)
    }

    Column(
        modifier = modifier.fillMaxSize().background(ScreenBackground).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Jazda na żywo", style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.Bold)
        ConnectionCard(state)
        TelemetryCard(state)
        RecordingButton(state, viewModel)
        CapacityCard(capacityText, onTextChange = { capacityText = it }, onSave = {
            viewModel.saveBatteryCapacityWh(capacityText.trim().replace(',', '.').toDoubleOrNull())
        })
        HistoryCard(state, viewModel)
        RecommendationCard(analysis.recommendation, analysis.recommendedProfile)
        MetricBarChart(
            title = "Średnie zużycie energii", unit = "Wh/km", profiles = analysis.profiles,
            value = { if (it.hasEnoughData) it.averageWhPerKm else null },
        )
        MetricBarChart(
            title = "Szacowany zasięg", unit = "km · przy ostatnim zapisanym poziomie baterii", profiles = analysis.profiles,
            value = { if (it.hasEnoughData) it.estimatedRangeKm else null },
        )
        Text(
            "Zasięg jest przybliżeniem. Wymaga podanej pojemności użytkowej baterii i zakłada liniowe przeliczenie poziomu baterii na energię.",
            color = MutedText, style = MaterialTheme.typography.bodySmall,
        )
        Spacer(Modifier.height(6.dp))
    }
}

@Composable
private fun ConnectionCard(state: RideLiveUiState) {
    Surface(color = CardBackground, shape = RoundedCornerShape(16.dp)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(10.dp).clip(CircleShape).background(if (state.isConnected) Color(0xFF56D68B) else Color(0xFF858991)))
            Column(Modifier.weight(1f)) {
                Text(if (state.isConnected) "Połączono ze sterownikiem VESC" else "Brak połączenia · podgląd zapisanych przejazdów", color = Color.White, fontWeight = FontWeight.SemiBold)
                Text(
                    if (state.isConnected) "Profil ${state.activeProfile ?: "—"}" else "Dane na żywo są ukryte do czasu połączenia.",
                    color = MutedText, style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun TelemetryCard(state: RideLiveUiState) {
    val telemetry = if (state.isConnected) state.telemetry else null
    Card(colors = CardDefaults.cardColors(containerColor = CardBackground), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Odczyt RT Data", color = Color.White, fontWeight = FontWeight.SemiBold)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LiveValue("Prędkość", telemetry?.speedKmh?.let { "${formatNumber(it)} km/h" } ?: "—", Modifier.weight(1f))
                LiveValue("Bateria", telemetry?.batteryPercent?.let { "${formatNumber(it)}%" } ?: "—", Modifier.weight(1f))
                LiveValue("Zużycie", telemetry?.consumptionWhPerKm?.let { "${formatNumber(it)} Wh/km" } ?: "—", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun LiveValue(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier.clip(RoundedCornerShape(12.dp)).background(Color(0xFF24272D)).padding(10.dp)) {
        Text(label, color = MutedText, style = MaterialTheme.typography.labelSmall)
        Text(value, color = Color.White, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun RecordingButton(state: RideLiveUiState, viewModel: RideLiveViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Button(
            onClick = { if (state.isRecording) viewModel.stopRecording() else viewModel.startRecording() },
            enabled = state.isRecording || (state.isConnected && state.activeProfile != null),
            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
        ) { Text(if (state.isRecording) "Zakończ zapis przejazdu" else "Rozpocznij zapis przejazdu") }
        if (state.isRecording) Text("Zapisuję próbkę co około 1 s. Zmiany profilu są zapisywane od razu.", color = MutedText, style = MaterialTheme.typography.bodySmall)
        else if (state.isConnected && state.activeProfile == null) Text("Oczekiwanie na odczyt aktywnego profilu ze sterownika.", color = MutedText, style = MaterialTheme.typography.bodySmall)
        state.notice?.let { Text(it, color = OrangeAccent, style = MaterialTheme.typography.bodySmall) }
    }
}

@Composable
private fun CapacityCard(value: String, onTextChange: (String) -> Unit, onSave: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = CardBackground), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Pojemność baterii", color = Color.White, fontWeight = FontWeight.SemiBold)
            Text("Wpisz użyteczną pojemność pakietu w Wh, aby obliczyć szacowany zasięg. Bez tej wartości zasięg pozostaje niewyświetlony.", color = MutedText, style = MaterialTheme.typography.bodySmall)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = value, onValueChange = onTextChange, modifier = Modifier.weight(1f),
                    label = { Text("Pojemność użytkowa (Wh)") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { onSave() }),
                )
                Button(onClick = onSave) { Text("Zapisz") }
            }
        }
    }
}

@Composable
private fun HistoryCard(state: RideLiveUiState, viewModel: RideLiveViewModel) {
    Card(colors = CardDefaults.cardColors(containerColor = CardBackground), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Zapisane przejazdy", color = Color.White, fontWeight = FontWeight.SemiBold)
            Text("Porównanie obejmuje wszystkie przejazdy albo wybrany przejazd.", color = MutedText, style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = { viewModel.selectRide(null) }) {
                Text(if (state.selectedRideId == null) "✓ Wszystkie przejazdy" else "Wszystkie przejazdy", color = if (state.selectedRideId == null) OrangeAccent else Color.White)
            }
            if (state.sessions.isEmpty()) {
                Text("Nie ma jeszcze zapisanych przejazdów.", color = MutedText, style = MaterialTheme.typography.bodySmall)
            } else {
                state.sessions.take(8).forEach { session ->
                    val selected = state.selectedRideId == session.id
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                            .background(if (selected) Color(0xFF34271D) else Color.Transparent)
                            .clickable { viewModel.selectRide(session.id) }
                            .padding(horizontal = 10.dp, vertical = 9.dp),
                        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(session.startedAtMs.toPolishDate(), color = if (selected) OrangeAccent else Color.White)
                        Text(if (selected) "Wybrany" else "Podgląd", color = MutedText, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun RecommendationCard(message: String, profile: Int?) {
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF2A211A)), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Rekomendacja", color = OrangeAccent, fontWeight = FontWeight.Bold)
            Text(message, color = Color.White, style = MaterialTheme.typography.bodyMedium)
            if (profile != null) Text("Profil $profile · wniosek z zapisanych danych", color = OrangeAccent, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun MetricBarChart(title: String, unit: String, profiles: List<ProfileStats>, value: (ProfileStats) -> Double?) {
    val values = profiles.mapNotNull { value(it)?.takeIf { metric -> metric.isFinite() && metric >= 0.0 } }
    val maxValue = values.maxOrNull()?.takeIf { it > 0.0 } ?: 0.0
    Card(colors = CardDefaults.cardColors(containerColor = CardBackground), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(title, color = Color.White, fontWeight = FontWeight.SemiBold)
            Text(unit, color = MutedText, style = MaterialTheme.typography.bodySmall)
            Row(Modifier.fillMaxWidth().height(150.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
                profiles.forEach { profile ->
                    val metric = value(profile)?.takeIf { it.isFinite() && it >= 0.0 }
                    Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                        Text(metric?.let { formatNumber(it) } ?: "—", color = ProfileColors[profile.profile - 1], style = MaterialTheme.typography.labelSmall, maxLines = 1)
                        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
                            if (metric != null && maxValue > 0.0 && profile.hasEnoughData) {
                                val fraction = (metric / maxValue).toFloat().coerceIn(0.04f, 1f)
                                Box(Modifier.width(24.dp).fillMaxHeight(fraction).clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)).background(ProfileColors[profile.profile - 1]))
                            }
                        }
                        Text("Profil ${profile.profile}", color = Color.White, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                    }
                }
            }
            Text("Brak wartości oznacza niewystarczające próbki albo brak danych o pojemności baterii.", color = MutedText, style = MaterialTheme.typography.bodySmall)
        }
    }
}

private fun formatNumber(value: Double, decimals: Int = 1): String =
    String.format(Locale("pl", "PL"), "%.${decimals}f", value)

private fun Long.toPolishDate(): String =
    SimpleDateFormat("dd.MM.yyyy · HH:mm", Locale("pl", "PL")).format(Date(this))
