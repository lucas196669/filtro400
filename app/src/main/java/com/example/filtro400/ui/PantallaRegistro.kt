package com.example.filtro400.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
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
import androidx.compose.ui.unit.dp
import com.example.filtro400.BaseDatosFiltros
import com.example.filtro400.LlamadaBloqueada
import com.example.filtro400.ReglaBloqueo
import com.example.filtro400.ui.theme.Espaciado
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val FORMATO_DIA = DateTimeFormatter.ofPattern("dd/MM/yyyy")

@Composable
fun PantallaRegistro(
    modifier: Modifier = Modifier,
    baseDatos: BaseDatosFiltros,
    onMensaje: (String) -> Unit = {}
) {
    var llamadas by remember { mutableStateOf(baseDatos.ultimasLlamadas(50)) }
    var confirmarBorrado by remember { mutableStateOf(false) }

    ContenidoRegistro(
        modifier = modifier,
        llamadas = llamadas,
        onBorrarHistorial = {
            confirmarBorrado = true
        }
    )

    if (confirmarBorrado) {
        DialogoConfirmacion(
            titulo = "Borrar historial",
            mensaje = "Se borrarán todas las llamadas guardadas. Esta acción no se puede deshacer.",
            textoConfirmar = "Borrar",
            onConfirmar = {
                baseDatos.borrarRegistro()
                llamadas = emptyList()
                onMensaje("Historial borrado")
            },
            onCancelar = {
                confirmarBorrado = false
            }
        )
    }
}

@Composable
internal fun ContenidoRegistro(
    modifier: Modifier = Modifier,
    llamadas: List<LlamadaBloqueada>,
    onBorrarHistorial: () -> Unit
) {
    ContenedorPantalla(modifier = modifier) {
        EncabezadoPantalla(
            titulo = "Registro",
            subtitulo = "Las últimas 50 llamadas bloqueadas, ordenadas por día."
        )

        if (llamadas.isEmpty()) {
            EstadoVacio(
                icono = Icons.Filled.Call,
                titulo = "Todavía no hay llamadas bloqueadas",
                texto = "Cuando el filtro rechace una llamada, aparecerá aquí agrupada por " +
                    "día con su hora y su motivo."
            )
        } else {
            val hoy = LocalDate.now()
            val ayer = hoy.minusDays(1)
            val grupos = llamadas.groupBy { diaDe(it.fecha) }

            grupos.forEach { (dia, llamadasDelDia) ->
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(Espaciado.sm)
                ) {
                    Text(
                        text = when (dia) {
                            hoy -> "Hoy"
                            ayer -> "Ayer"
                            else -> dia.format(FORMATO_DIA)
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = Espaciado.xs)
                    )

                    llamadasDelDia.forEach { llamada ->
                        FilaLlamada(llamada = llamada)
                    }
                }
            }

            OutlinedButton(
                onClick = onBorrarHistorial,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Espaciado.sm)
                    .heightIn(min = 48.dp)
            ) {
                Text("Borrar historial")
            }
        }
    }
}

@Composable
private fun FilaLlamada(llamada: LlamadaBloqueada) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Espaciado.lg),
            verticalArrangement = Arrangement.spacedBy(Espaciado.sm)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (llamada.numero == "Oculto" || llamada.numero.isBlank()) {
                        "Número oculto"
                    } else {
                        llamada.numero
                    },
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(Espaciado.md))
                Text(
                    text = formatearHoraCorta(llamada.fecha),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            EtiquetaMotivo(texto = textoMotivo(llamada.motivo))
        }
    }
}

private fun diaDe(milisegundos: Long): LocalDate {
    return Instant.ofEpochMilli(milisegundos)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
}

private fun textoMotivo(motivo: String): String {
    return when (motivo) {
        ReglaBloqueo.Motivo.SUPLANTADO.texto -> "Número sospechoso"
        ReglaBloqueo.Motivo.PREFIJO.texto -> "Estaba en tu lista (ej. 400)"
        ReglaBloqueo.Motivo.TODO.texto -> "Bloqueo total"
        ReglaBloqueo.Motivo.HORARIO.texto -> "Horario sin llamadas"
        ReglaBloqueo.Motivo.SILENCIO.texto -> "Silencio temporal"
        else -> motivo
    }
}
