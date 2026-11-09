package com.example.filtro400.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.filtro400.Ajustes
import com.example.filtro400.BaseDatosFiltros
import com.example.filtro400.ui.theme.Espaciado

private const val UNA_HORA_MS = 60 * 60 * 1000L

@Composable
fun PantallaInicio(
    modifier: Modifier = Modifier,
    proteccionActiva: Boolean,
    ajustes: Ajustes,
    baseDatos: BaseDatosFiltros,
    onActivarProteccion: () -> Unit,
    onCambiarAjustes: (Ajustes) -> Unit,
    onMensaje: (String) -> Unit = {}
) {
    // El contador se lee al entrar en la pestaña.
    val bloqueadasHoy = remember { baseDatos.contarBloqueadasHoy() }
    val context = LocalContext.current
    val respuestaSmsDisponible = ajustes.respuestaSms &&
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED
    var confirmarBloqueoTotal by remember { mutableStateOf(false) }

    ContenidoInicio(
        modifier = modifier,
        proteccionActiva = proteccionActiva,
        ajustes = ajustes,
        bloqueadasHoy = bloqueadasHoy,
        respuestaSmsDisponible = respuestaSmsDisponible,
        onActivarProteccion = onActivarProteccion,
        onCambiarAjustes = onCambiarAjustes,
        onMensaje = onMensaje,
        onPedirConfirmacionBloqueoTotal = {
            confirmarBloqueoTotal = true
        }
    )

    if (confirmarBloqueoTotal) {
        DialogoConfirmacion(
            titulo = "Bloquear todas las llamadas desconocidas",
            mensaje = "Solo sonarán las llamadas de tus contactos. " +
                "Las demás se rechazarán. ¿Quieres activarlo?",
            textoConfirmar = "Activar bloqueo",
            onConfirmar = {
                onCambiarAjustes(ajustes.copy(bloquearTodo = true))
                onMensaje("Bloqueo total activado")
            },
            onCancelar = {
                confirmarBloqueoTotal = false
            }
        )
    }
}

@Composable
internal fun ContenidoInicio(
    modifier: Modifier = Modifier,
    proteccionActiva: Boolean,
    ajustes: Ajustes,
    bloqueadasHoy: Int,
    respuestaSmsDisponible: Boolean,
    onActivarProteccion: () -> Unit,
    onCambiarAjustes: (Ajustes) -> Unit,
    onMensaje: (String) -> Unit,
    onPedirConfirmacionBloqueoTotal: () -> Unit
) {
    val enSilencio = ajustes.silencioHastaMs > System.currentTimeMillis()

    ContenedorPantalla(modifier = modifier) {
        EncabezadoPantalla(
            titulo = "Filtro 400",
            subtitulo = "Protección sencilla contra llamadas molestas."
        )

        TarjetaEstado(
            proteccionActiva = proteccionActiva,
            onActivarProteccion = onActivarProteccion
        )

        TarjetaContador(bloqueadasHoy = bloqueadasHoy)

        Text(
            text = "Qué quieres bloquear",
            style = MaterialTheme.typography.titleMedium
        )

        TarjetaAjuste(
            icono = Icons.Filled.Warning,
            titulo = "Bloquear llamadas sospechosas",
            descripcion = "Rechaza números sospechosos y los de tu lista.",
            activado = ajustes.bloquearSpam,
            onCambio = { activo ->
                onCambiarAjustes(ajustes.copy(bloquearSpam = activo))
            }
        )

        TarjetaAjuste(
            icono = Icons.Filled.DateRange,
            titulo = "Horario sin llamadas",
            descripcion = "De ${minutosAHora(ajustes.inicioMin)} a ${minutosAHora(ajustes.finMin)}. " +
                if (respuestaSmsDisponible) {
                    "Se rechazan y se envía un mensaje de texto."
                } else {
                    "Se rechazan sin enviar mensajes."
                },
            activado = ajustes.horarioActivo,
            onCambio = { activo ->
                onCambiarAjustes(ajustes.copy(horarioActivo = activo))
            }
        )

        TarjetaAjuste(
            icono = Icons.Filled.Lock,
            titulo = "Bloquear todas las llamadas desconocidas",
            descripcion = "Solo suenan tus contactos.",
            activado = ajustes.bloquearTodo,
            onCambio = { activo ->
                if (activo) {
                    onPedirConfirmacionBloqueoTotal()
                } else {
                    onCambiarAjustes(ajustes.copy(bloquearTodo = false))
                    onMensaje("Bloqueo total desactivado")
                }
            }
        )

        TarjetaSilencio(
            enSilencio = enSilencio,
            respuestaSmsDisponible = respuestaSmsDisponible,
            ajustes = ajustes,
            onCambiarAjustes = onCambiarAjustes,
            onMensaje = onMensaje
        )
    }
}

@Composable
private fun TarjetaContador(bloqueadasHoy: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Espaciado.lg),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Call,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(Espaciado.md))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Espaciado.xs)
            ) {
                Text(
                    text = if (bloqueadasHoy == 0) {
                        "Hoy no se ha bloqueado ninguna llamada"
                    } else {
                        "Hoy has bloqueado $bloqueadasHoy " +
                            if (bloqueadasHoy == 1) "llamada" else "llamadas"
                    },
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = if (bloqueadasHoy == 0) {
                        "Cuando se bloquee una llamada, aparecerá en Registro."
                    } else {
                        "Puedes revisarlas en la pestaña Registro."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun TarjetaSilencio(
    enSilencio: Boolean,
    respuestaSmsDisponible: Boolean,
    ajustes: Ajustes,
    onCambiarAjustes: (Ajustes) -> Unit,
    onMensaje: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Espaciado.lg),
            verticalArrangement = Arrangement.spacedBy(Espaciado.md)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Notifications,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(Espaciado.md))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(Espaciado.xs)
                ) {
                    Text(
                        text = "Silencio temporal",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = if (enSilencio) {
                            "Hasta las ${formatearHoraCorta(ajustes.silencioHastaMs)}"
                        } else {
                            "Durante una hora"
                        },
                        style = MaterialTheme.typography.titleSmall,
                        color = if (enSilencio) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }

            Text(
                text = if (enSilencio) {
                    if (respuestaSmsDisponible) {
                        "Las llamadas se rechazan. También se enviará un mensaje automático."
                    } else {
                        "Las llamadas se rechazan sin enviar ningún mensaje automático."
                    }
                } else {
                    "Durante una hora se rechazan las llamadas para que puedas descansar."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedButton(
                onClick = {
                    if (enSilencio) {
                        onCambiarAjustes(ajustes.copy(silencioHastaMs = 0L))
                        onMensaje("Silencio cancelado")
                    } else {
                        onCambiarAjustes(
                            ajustes.copy(
                                silencioHastaMs = System.currentTimeMillis() + UNA_HORA_MS
                            )
                        )
                        onMensaje("Silencio activado durante 1 hora")
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
            ) {
                Text(if (enSilencio) "Cancelar silencio" else "Silenciar 1 hora")
            }
        }
    }
}
