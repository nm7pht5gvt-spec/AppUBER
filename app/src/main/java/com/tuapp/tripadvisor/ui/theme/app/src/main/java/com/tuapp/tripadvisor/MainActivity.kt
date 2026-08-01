package com.tuapp.tripadvisor

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tuapp.tripadvisor.data.preferences.PreferencesRepository
import com.tuapp.tripadvisor.domain.evaluator.TripEvaluator
import com.tuapp.tripadvisor.domain.model.TripOffer
import com.tuapp.tripadvisor.domain.model.UserPreferences
import com.tuapp.tripadvisor.service.overlay.OverlayService
import com.tuapp.tripadvisor.ui.config.ConfigScreen
import com.tuapp.tripadvisor.ui.config.ConfigViewModel
import com.tuapp.tripadvisor.ui.config.ConfigViewModelFactory
import com.tuapp.tripadvisor.ui.overlay.SemaphoreWidget
import com.tuapp.tripadvisor.ui.theme.TripAdvisorTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val notificationLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        val repository = PreferencesRepository(applicationContext)
        val viewModelFactory = ConfigViewModelFactory(repository)

        setContent {
            TripAdvisorTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainAppContent(viewModelFactory = viewModelFactory)
                }
            }
        }
    }
}

private enum class AppScreen {
    CONFIG,
    SERVICE_ACTIVE,
    PREVIEW_DEMO
}

@Composable
fun MainAppContent(viewModelFactory: ConfigViewModelFactory) {
    val configViewModel: ConfigViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = viewModelFactory
    )

    var currentScreen by remember { mutableStateOf(AppScreen.CONFIG) }

    when (currentScreen) {
        AppScreen.CONFIG -> {
            ConfigScreen(
                viewModel = configViewModel,
                onServiceActivated = { currentScreen = AppScreen.SERVICE_ACTIVE }
            )
        }
        AppScreen.SERVICE_ACTIVE -> {
            ServiceActiveScreen(
                onStopService = {
                    OverlayService.stop(androidx.compose.ui.platform.LocalContext.current)
                    currentScreen = AppScreen.CONFIG
                },
                onOpenDemo = { currentScreen = AppScreen.PREVIEW_DEMO },
                onBackToConfig = { currentScreen = AppScreen.CONFIG }
            )
        }
        AppScreen.PREVIEW_DEMO -> {
            WidgetPreviewScreen(
                userPreferences = UserPreferences(
                    minPricePerKm = configViewModel.uiState.value.pricePerKmInput.toDoubleOrNull() ?: 1.5,
                    minEarningsPerHour = configViewModel.uiState.value.earningsPerHourInput.toDoubleOrNull() ?: 25.0
                ),
                onBack = { currentScreen = AppScreen.SERVICE_ACTIVE }
            )
        }
    }
}

@Composable
fun ServiceActiveScreen(
    onStopService: () -> Unit,
    onOpenDemo: () -> Unit,
    onBackToConfig: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text("Servicio Activo", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Abre Uber o DiDi Driver — el semáforo aparecerá automáticamente sobre las ofertas.",
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp, bottom = 32.dp)
        )

        Button(onClick = onOpenDemo, modifier = Modifier.fillMaxWidth()) {
            Text("Ver Demo del Widget")
        }
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(onClick = onBackToConfig, modifier = Modifier.fillMaxWidth()) {
            Text("Editar Tarifas")
        }
        Spacer(modifier = Modifier.height(12.dp))
        TextButton(onClick = onStopService, modifier = Modifier.fillMaxWidth()) {
            Text("Detener Servicio", color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
fun WidgetPreviewScreen(
    userPreferences: UserPreferences,
    onBack: () -> Unit
) {
    val mockOffers = remember {
        listOf(
            "🔴 Oferta baja" to TripOffer(distanceKm = 10.0, estimatedTimeMinutes = 30.0, offeredPrice = 8.0),
            "🟡 Oferta justa" to TripOffer(distanceKm = 10.0, estimatedTimeMinutes = 25.0, offeredPrice = 14.5),
            "🟢 Oferta buena" to TripOffer(distanceKm = 10.0, estimatedTimeMinutes = 20.0, offeredPrice = 20.0)
        )
    }
    var selectedIndex by remember { mutableStateOf(0) }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Preview del Widget Flotante", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
        Spacer(modifier = Modifier.height(24.dp))

        val (_, currentOffer) = mockOffers[selectedIndex]
        val evaluation = remember(currentOffer, userPreferences) {
            TripEvaluator.evaluate(currentOffer, userPreferences)
        }
        SemaphoreWidget(evaluation = evaluation)

        Spacer(modifier = Modifier.height(32.dp))

        mockOffers.forEachIndexed { index, (label, _) ->
            Button(
                onClick = { selectedIndex = index },
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            ) { Text(label) }
        }

        Spacer(modifier = Modifier.height(24.dp))
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("Volver")
        }
    }
}
