package com.tuapp.tripadvisor.ui.config

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun OverlayPermissionDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.Layers, contentDescription = null) },
        title = { Text("Permiso de superposición requerido") },
        text = {
            Text(
                "Para mostrar el semáforo flotante sobre la app de Uber/DiDi, " +
                "necesitamos permiso para dibujar sobre otras aplicaciones.\n\n" +
                "Te llevaremos a Ajustes: solo activa el interruptor para " +
                "\"Semáforo de Viajes\" y regresa a la app."
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Ir a Ajustes") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
fun AccessibilityPermissionDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
        title = { Text("Activa el Servicio de Accesibilidad") },
        text = {
            Column {
                Text(
                    "Este servicio permite leer los datos de las ofertas " +
                    "de viaje en pantalla (distancia, tiempo, precio).\n"
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text("Pasos a seguir en Ajustes:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(6.dp))
                InstructionStep(number = 1, text = "Busca \"Apps instaladas\" o \"Servicios\"")
                InstructionStep(number = 2, text = "Selecciona \"Semáforo de Viajes\"")
                InstructionStep(number = 3, text = "Activa el interruptor y confirma")
                InstructionStep(number = 4, text = "Regresa a esta app con el botón atrás")
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Ir a Ajustes") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
private fun InstructionStep(number: Int, text: String) {
    Row(modifier = Modifier.padding(vertical = 2.dp)) {
        Text("$number. ", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Text(text, fontSize = 13.sp)
    }
}

