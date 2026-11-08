package pl.replit.jazdanazywo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Modifier
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel

private val RideColors = darkColorScheme(
    primary = Color(0xFFFF8A26),
    onPrimary = Color(0xFF1B1D22),
    background = Color(0xFF101114),
    surface = Color(0xFF1B1D22),
    onBackground = Color.White,
    onSurface = Color.White,
)

class MainActivity : ComponentActivity() {
    private val rideRepository by lazy { RideRepository(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = RideColors) {
                val factory = remember(rideRepository) { RideLiveViewModelFactory(rideRepository) }
                val rideViewModel: RideLiveViewModel = viewModel(factory = factory)
                RideLiveScreen(
                    viewModel = rideViewModel,
                    modifier = Modifier.safeDrawingPadding(),
                )
            }
        }
    }
}
