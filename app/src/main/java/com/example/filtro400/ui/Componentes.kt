package com.example.filtro400.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.filtro400.ui.theme.Espaciado
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val FORMATO_HORA = DateTimeFormatter.ofPattern("HH:mm")
private val FORMATO_FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")

// Tarjetas con tinte azul (nada de blanco puro) y borde fino.
@Composable
private fun coloresTarjeta() = CardDefaults.cardColors(
    containerColor = if (isSystemInDarkTheme()) Color(0xFF16233D) else Color(0xFFEAF1FF)
)

@Composable
private fun bordeTarjeta() =
    BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

@Composable
fun ContenedorPantalla(
    modifier: Modifier = Modifier,
    contenido: @Composable ColumnScope.() -> Unit
) {
    val oscuro = isSystemInDarkTheme()
    // Fondo con un degradado azul suave en vez de blanco liso
    val fondo = Brush.verticalGradient(
        if (oscuro) listOf(Color(0xFF0B1220), Color(0xFF12203A))
        else listOf(Color(0xFFB4CCFF), Color(0xFFD2E1FF))
    )
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(fondo),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Espaciado.lg, vertical = Espaciado.xl),
            verticalArrangement = Arrangement.spacedBy(Espaciado.lg)
        ) {
            contenido()
        }
    }
}

@Composable
fun EncabezadoPantalla(titulo: String, subtitulo: String? = null) {
    val oscuro = isSystemInDarkTheme()
    val degradado = Brush.linearGradient(
        if (oscuro) listOf(Color(0xFF1E40AF), Color(0xFF115E59))
        else listOf(Color(0xFF2563EB), Color(0xFF0F766E))
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(degradado)
            .padding(horizontal = Espaciado.xl, vertical = Espaciado.xl)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Espaciado.xs)) {
            Text(
                text = titulo,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            if (subtitulo != null) {
                Text(
                    text = subtitulo,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.92f)
                )
            }
        }
    }
}

@Composable
fun TarjetaAjuste(
    icono: ImageVector,
    titulo: String,
    descripcion: String,
    activado: Boolean,
    onCambio: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = coloresTarjeta(),
        border = bordeTarjeta()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(
                    value = activado,
                    role = Role.Switch,
                    onValueChange = onCambio
                )
                .padding(Espaciado.lg),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF2563EB)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icono,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(Espaciado.md))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Espaciado.xs)
            ) {
                Text(
                    text = titulo,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = descripcion,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(Espaciado.md))
            Switch(
                checked = activado,
                onCheckedChange = null
            )
        }
    }
}

@Composable
fun TarjetaEstado(
    proteccionActiva: Boolean,
    onActivarProteccion: () -> Unit
) {
    // Degradado vivo: verde si está activa, naranja si falta un paso. Texto blanco.
    val inicio by animateColorAsState(
        targetValue = if (proteccionActiva) Color(0xFF15803D) else Color(0xFFB45309),
        label = "inicioEstado"
    )
    val fin by animateColorAsState(
        targetValue = if (proteccionActiva) Color(0xFF0F766E) else Color(0xFFD97706),
        label = "finEstado"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(Brush.linearGradient(listOf(inicio, fin)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Espaciado.xl),
            verticalArrangement = Arrangement.spacedBy(Espaciado.md)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (proteccionActiva) {
                            Icons.Filled.CheckCircle
                        } else {
                            Icons.Filled.Warning
                        },
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(34.dp)
                    )
                }
                Spacer(modifier = Modifier.width(Espaciado.md))
                Text(
                    text = if (proteccionActiva) "Protección activada" else "Falta un paso",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White
                )
            }

            Text(
                text = if (proteccionActiva) {
                    "Las llamadas que no quieres recibir se rechazan automáticamente."
                } else {
                    "Activa el permiso de Android para que el filtro pueda funcionar."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.95f)
            )

            if (!proteccionActiva) {
                Button(
                    onClick = onActivarProteccion,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color(0xFF92400E)
                    )
                ) {
                    Text(
                        text = "Activar protección",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}

@Composable
fun EstadoVacio(icono: ImageVector, titulo: String, texto: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = coloresTarjeta(),
        border = bordeTarjeta()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Espaciado.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Espaciado.md)
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF2563EB)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icono,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(30.dp)
                )
            }
            Text(
                text = titulo,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
            Text(
                text = texto,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun SeccionPlegable(
    titulo: String,
    modifier: Modifier = Modifier,
    inicialmenteAbierta: Boolean = false,
    contenido: @Composable ColumnScope.() -> Unit
) {
    var abierta by rememberSaveable { mutableStateOf(inicialmenteAbierta) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = coloresTarjeta(),
        border = bordeTarjeta()
    ) {
        Column(modifier = Modifier.animateContentSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clickable(role = Role.Button) { abierta = !abierta }
                    .padding(horizontal = Espaciado.lg, vertical = Espaciado.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = titulo,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (abierta) {
                        Icons.Filled.KeyboardArrowUp
                    } else {
                        Icons.Filled.KeyboardArrowDown
                    },
                    contentDescription = if (abierta) "Contraer $titulo" else "Desplegar $titulo"
                )
            }
            if (abierta) {
                Column(
                    modifier = Modifier.padding(
                        start = Espaciado.lg,
                        end = Espaciado.lg,
                        bottom = Espaciado.lg
                    ),
                    verticalArrangement = Arrangement.spacedBy(Espaciado.sm)
                ) {
                    contenido()
                }
            }
        }
    }
}

@Composable
fun EtiquetaMotivo(texto: String) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Text(
            text = texto,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(horizontal = Espaciado.sm, vertical = Espaciado.xs)
        )
    }
}

@Composable
fun DialogoConfirmacion(
    titulo: String,
    mensaje: String,
    textoConfirmar: String,
    onConfirmar: () -> Unit,
    onCancelar: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(titulo) },
        text = { Text(mensaje) },
        confirmButton = {
            TextButton(onClick = {
                onConfirmar()
                onCancelar()
            }) {
                Text(textoConfirmar)
            }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) {
                Text("Cancelar")
            }
        }
    )
}

fun minutosAHora(minutos: Int): String {
    val hora = (minutos / 60) % 24
    val minuto = minutos % 60
    return "%02d:%02d".format(hora, minuto)
}

fun formatearFechaHora(milisegundos: Long): String {
    return Instant.ofEpochMilli(milisegundos)
        .atZone(ZoneId.systemDefault())
        .format(FORMATO_FECHA_HORA)
}

fun formatearHoraCorta(milisegundos: Long): String {
    return Instant.ofEpochMilli(milisegundos)
        .atZone(ZoneId.systemDefault())
        .format(FORMATO_HORA)
}