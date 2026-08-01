package com.tuapp.tripadvisor.ui.config

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.tuapp.tripadvisor.service.overlay.OverlayService
import com.tuapp.tripadvisor.util.PermissionHelper

@Composable
fun ConfigScreen(
    viewModel: ConfigViewModel,
    onServiceActivated: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()

    var showOverlayDialog by remember { mutableStateOf(false) }
    var showAccessibilityDialog by remember { mutableStateOf(false) }
    var awaitingSettingsReturn by remember { mutableStateOf(false) }

    fun attemptActivation() {
        when {
            !PermissionHelper.hasOverlayPermission(context) -> {
                showOverlayDialog = true
            }
            !PermissionHelper.isAccessibilityServiceEnabled(context) -> {
                showAccessibilityDialog = true
            }
            else -> {
                viewModel.saveAndActivate(onSuccess = {
                    OverlayService.start(context)
                    onServiceActivated()
                })
            }
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && awaitingSettingsReturn) {
                awaitingSettingsReturn = false
                attemptActivation()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Text("Configura tus tarifas", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(
            text = "El semáforo usará estos valores como referencia mínima.",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 32.dp)
        )

        OutlinedTextField(
            value = state.pricePerKmInput,
            onValueChange = viewModel::onPricePerKmChanged,
            label = { Text("Precio mínimo por km ($)") },
            placeholder = { Text("Ej: 1.50") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = state.earningsPerHourInput,
            onValueChange = viewModel::onEarningsPerHourChanged,
            label = { Text("Ganancia mínima por hora ($)") },
            placeholder = { Text("Ej: 25.00") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        state.errorMessage?.let { error ->
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 12.dp)
            )
        }

        if (state.saveSuccess) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 12.dp)) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Preferencias guardadas — Servicio activo", fontSize = 13.sp)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = { attemptActivation() },
            enabled = !state.isSaving,
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            if (state.isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Guardar y Activar Servicio", fontSize = 16.sp)
            }
        }
    }

    if (showOverlayDialog) {
        OverlayPermissionDialog(
            onConfirm = {
                showOverlayDialog = false
                awaitingSettingsReturn = true
                context.startActivity(PermissionHelper.createOverlayPermissionIntent(context))
            },
            onDismiss = { showOverlayDialog = false }
        )
    }

    if (showAccessibilityDialog) {
        AccessibilityPermissionDialog(
            onConfirm = {
                showAccessibilityDialog = false
                awaitingSettingsReturn = true
                context.startActivity(PermissionHelper.createAccessibilitySettingsIntent())
            },
            onDismiss = { showAccessibilityDialog = false }
        )
    }
}
