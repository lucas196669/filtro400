package com.example.filtro400.ui

import android.Manifest
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.filtro400.Ajustes
import com.example.filtro400.ui.theme.Espaciado

@Composable
fun PantallaHorario(
    modifier: Modifier = Modifier,
    ajustes: Ajustes,
    onCambiarAjustes: (Ajustes) -> Unit,
    onMensaje: (String) -> Unit = {}
) {
    val context = LocalContext.current
    var permisoDenegado by remember { mutableStateOf(false) }
    var permisoConcedido by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.SEND_SMS
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    val smsActivo = ajustes.respuestaSms && permisoConcedido

    val lanzadorPermiso = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedido ->
        permisoConcedido = concedido
        permisoDenegado = !concedido
        onCambiarAjustes(ajustes.copy(respuestaSms = concedido))
        onMensaje(
            if (concedido) {
                "Permiso concedido: se enviarán mensajes automáticos."
            } else {
                "Sin permiso no se pueden enviar mensajes automáticos."
            }
        )
    }

    ContenidoHorario(
        modifier = modifier,
        ajustes = ajustes,
        smsActivo = smsActivo,
        permisoConcedido = permisoConcedido,
        permisoDenegado = permisoDenegado,
        onCambiarAjustes = onCambiarAjustes,
        onActivarSms = {
            onCambiarAjustes(ajustes.copy(respuestaSms = true))
        },
        onDesactivarSms = {
            permisoDenegado = false
            onCambiarAjustes(ajustes.copy(respuestaSms = false))
        },
        onSolicitarPermiso = {
            lanzadorPermiso.launch(Manifest.permission.SEND_SMS)
        },
        onRestablecerMensaje = {
            onCambiarAjustes(ajustes.copy(textoSms = Ajustes().textoSms))
            onMensaje("Mensaje restablecido")
        }
    )
}

@Composable
internal fun ContenidoHorario(
    modifier: Modifier = Modifier,
    ajustes: Ajustes,
    smsActivo: Boolean,
    permisoConcedido: Boolean,
    permisoDenegado: Boolean,
    onCambiarAjustes: (Ajustes) -> Unit,
    onActivarSms: () -> Unit,
    onDesactivarSms: () -> Unit,
    onSolicitarPermiso: () -> Unit,
    onRestablecerMensaje: () -> Unit
) {
    val context = LocalContext.current

    // Abre el selector de hora clásico de Android.
    fun mostrarSelectorHora(minutos: Int, alElegir: (Int) -> Unit) {
        TimePickerDialog(
            context,
            { _, hora, minuto ->
                alElegir(hora * 60 + minuto)
            },
            minutos / 60,
            minutos % 60,
            true
        ).show()
    }

    ContenedorPantalla(modifier = modifier) {
        EncabezadoPantalla(
            titulo = "Horario",
            subtitulo = "Elige a qué horas no quieres recibir llamadas desconocidas."
        )

        TarjetaAjuste(
            icono = Icons.Filled.DateRange,
            titulo = "Horario sin llamadas",
            descripcion = "Rechaza las llamadas desconocidas dentro de este horario.",
            activado = ajustes.horarioActivo,
            onCambio = { activo ->
                onCambiarAjustes(ajustes.copy(horarioActivo = activo))
            }
        )

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
                    horizontalArrangement = Arrangement.spacedBy(Espaciado.md)
                ) {
                    OutlinedButton(
                        onClick = {
                            mostrarSelectorHora(ajustes.inicioMin) { nuevo ->
                                onCambiarAjustes(ajustes.copy(inicioMin = nuevo))
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 56.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(Espaciado.xs)
                        ) {
                            Text(
                                text = "Desde",
                                style = MaterialTheme.typography.labelMedium
                            )
                            Text(
                                text = minutosAHora(ajustes.inicioMin),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                    OutlinedButton(
                        onClick = {
                            mostrarSelectorHora(ajustes.finMin) { nuevo ->
                                onCambiarAjustes(ajustes.copy(finMin = nuevo))
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 56.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(Espaciado.xs)
                        ) {
                            Text(
                                text = "Hasta",
                                style = MaterialTheme.typography.labelMedium
                            )
                            Text(
                                text = minutosAHora(ajustes.finMin),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }

                Text(
                    text = when {
                        ajustes.inicioMin == ajustes.finMin ->
                            "Elige una hora de inicio y otra de fin distintas."

                        !ajustes.horarioActivo ->
                            "El horario está desactivado. Actívalo cuando quieras usarlo."

                        else -> "Las llamadas de números desconocidos entre las " +
                            "${minutosAHora(ajustes.inicioMin)} y las " +
                            "${minutosAHora(ajustes.finMin)} se rechazan."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (ajustes.inicioMin == ajustes.finMin) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }

        TarjetaAjuste(
            icono = Icons.Filled.Email,
            titulo = "Responder con un mensaje",
            descripcion = "Envía un mensaje de texto automático cuando se rechaza una llamada.",
            activado = smsActivo,
            onCambio = { activar ->
                when {
                    !activar -> onDesactivarSms()
                    permisoConcedido -> onActivarSms()
                    else -> onSolicitarPermiso()
                }
            }
        )

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
                Text(
                    text = "Mensaje automático",
                    style = MaterialTheme.typography.titleMedium
                )

                if (!permisoConcedido) {
                    Text(
                        text = if (permisoDenegado) {
                            "No se ha dado permiso para enviar mensajes, así que la respuesta " +
                                "automática seguirá apagada."
                        } else {
                            "Al activarlo, Android pedirá permiso para enviar mensajes."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (permisoDenegado) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }

                OutlinedTextField(
                    value = ajustes.textoSms,
                    onValueChange = { texto ->
                        onCambiarAjustes(ajustes.copy(textoSms = texto.take(160)))
                    },
                    label = { Text("Texto del mensaje") },
                    supportingText = {
                        Text("${ajustes.textoSms.length}/160 caracteres")
                    },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedButton(
                    onClick = onRestablecerMensaje,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                ) {
                    Text("Restablecer mensaje")
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Espaciado.lg),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(Espaciado.sm))
                Text(
                    text = "Android no permite contestar con voz a las llamadas, por eso se " +
                        "envía un mensaje de texto. Puede tener coste según tu tarifa.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
