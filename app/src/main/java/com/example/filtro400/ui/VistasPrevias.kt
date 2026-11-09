package com.example.filtro400.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.filtro400.Ajustes
import com.example.filtro400.Filtro
import com.example.filtro400.LlamadaBloqueada
import com.example.filtro400.ReglaBloqueo
import com.example.filtro400.ui.theme.Filtro400Theme

private val FILTROS_DEMO = listOf(
    Filtro(id = 1L, prefijo = "400", activo = true),
    Filtro(id = 2L, prefijo = "900", activo = true),
    Filtro(id = 3L, prefijo = "695", activo = false)
)

private fun llamadasDemo(): List<LlamadaBloqueada> {
    val ahora = System.currentTimeMillis()
    return listOf(
        LlamadaBloqueada(1L, "400 123 456", ReglaBloqueo.Motivo.PREFIJO.texto, ahora - 10 * 60 * 1000L),
        LlamadaBloqueada(2L, "Oculto", ReglaBloqueo.Motivo.SUPLANTADO.texto, ahora - 30 * 60 * 1000L),
        LlamadaBloqueada(3L, "612 345 678", ReglaBloqueo.Motivo.TODO.texto, ahora - 26 * 60 * 60 * 1000L),
        LlamadaBloqueada(4L, "699 000 111", ReglaBloqueo.Motivo.HORARIO.texto, ahora - 28 * 60 * 60 * 1000L)
    )
}

@Composable
private fun MarcoVistaPrevia(
    darkTheme: Boolean,
    contenido: @Composable () -> Unit
) {
    Filtro400Theme(darkTheme = darkTheme, dynamicColor = false) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            contenido()
        }
    }
}

@Preview(
    name = "Inicio · claro",
    showBackground = true,
    widthDp = 360,
    heightDp = 1000
)
@Composable
private fun VistaPreviaInicioClaro() {
    MarcoVistaPrevia(darkTheme = false) {
        ContenidoInicio(
            proteccionActiva = true,
            ajustes = Ajustes(
                bloquearSpam = true,
                horarioActivo = true,
                inicioMin = 22 * 60,
                finMin = 8 * 60,
                respuestaSms = true
            ),
            bloqueadasHoy = 3,
            respuestaSmsDisponible = true,
            onActivarProteccion = {},
            onCambiarAjustes = { _ -> },
            onMensaje = { _ -> },
            onPedirConfirmacionBloqueoTotal = {}
        )
    }
}

@Preview(
    name = "Inicio · oscuro 130 %",
    showBackground = true,
    widthDp = 360,
    heightDp = 1000,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    fontScale = 1.3f
)
@Composable
private fun VistaPreviaInicioOscuro() {
    MarcoVistaPrevia(darkTheme = true) {
        ContenidoInicio(
            proteccionActiva = false,
            ajustes = Ajustes(
                bloquearSpam = true,
                horarioActivo = false,
                respuestaSms = false
            ),
            bloqueadasHoy = 0,
            respuestaSmsDisponible = false,
            onActivarProteccion = {},
            onCambiarAjustes = { _ -> },
            onMensaje = { _ -> },
            onPedirConfirmacionBloqueoTotal = {}
        )
    }
}

@Preview(
    name = "Filtros · claro",
    showBackground = true,
    widthDp = 360,
    heightDp = 1100
)
@Composable
private fun VistaPreviaFiltrosClaro() {
    MarcoVistaPrevia(darkTheme = false) {
        ContenidoFiltros(
            filtros = FILTROS_DEMO,
            nuevoTexto = "",
            onNuevoTextoChange = { _ -> },
            errorNuevo = null,
            onAgregar = {},
            onCambiarActivo = { _, _ -> },
            onEditar = { _ -> },
            onBorrar = { _ -> },
            numeroPrueba = "",
            onNumeroPruebaChange = { _ -> },
            resultadoPrueba = "",
            onProbar = {},
            ajustes = Ajustes(guardarEnLista = false),
            onCambiarAjustes = { _ -> }
        )
    }
}

@Preview(
    name = "Filtros · oscuro 130 %",
    showBackground = true,
    widthDp = 360,
    heightDp = 1100,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    fontScale = 1.3f
)
@Composable
private fun VistaPreviaFiltrosOscuro() {
    MarcoVistaPrevia(darkTheme = true) {
        ContenidoFiltros(
            filtros = emptyList(),
            nuevoTexto = "400",
            onNuevoTextoChange = { _ -> },
            errorNuevo = "Ese número o prefijo ya está en la lista. " +
                "Prueba con otro prefijo (por ejemplo, 900).",
            onAgregar = {},
            onCambiarActivo = { _, _ -> },
            onEditar = { _ -> },
            onBorrar = { _ -> },
            numeroPrueba = "400 123 456",
            onNumeroPruebaChange = { _ -> },
            resultadoPrueba = "Esta llamada se bloquearía.",
            onProbar = {},
            ajustes = Ajustes(guardarEnLista = true),
            onCambiarAjustes = { _ -> },
            seccionPruebaAbierta = true
        )
    }
}

@Preview(
    name = "Horario · claro",
    showBackground = true,
    widthDp = 360,
    heightDp = 1100
)
@Composable
private fun VistaPreviaHorarioClaro() {
    MarcoVistaPrevia(darkTheme = false) {
        ContenidoHorario(
            ajustes = Ajustes(
                horarioActivo = true,
                inicioMin = 22 * 60,
                finMin = 8 * 60,
                respuestaSms = true
            ),
            smsActivo = true,
            permisoConcedido = true,
            permisoDenegado = false,
            onCambiarAjustes = { _ -> },
            onActivarSms = {},
            onDesactivarSms = {},
            onSolicitarPermiso = {},
            onRestablecerMensaje = {}
        )
    }
}

@Preview(
    name = "Horario · oscuro 130 %",
    showBackground = true,
    widthDp = 360,
    heightDp = 1100,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    fontScale = 1.3f
)
@Composable
private fun VistaPreviaHorarioOscuro() {
    MarcoVistaPrevia(darkTheme = true) {
        ContenidoHorario(
            ajustes = Ajustes(
                horarioActivo = false,
                respuestaSms = false,
                textoSms = "No puedo atenderte ahora. Escríbeme y te contesto en cuanto pueda."
            ),
            smsActivo = false,
            permisoConcedido = false,
            permisoDenegado = true,
            onCambiarAjustes = { _ -> },
            onActivarSms = {},
            onDesactivarSms = {},
            onSolicitarPermiso = {},
            onRestablecerMensaje = {}
        )
    }
}

@Preview(
    name = "Registro · claro",
    showBackground = true,
    widthDp = 360,
    heightDp = 1200
)
@Composable
private fun VistaPreviaRegistroClaro() {
    MarcoVistaPrevia(darkTheme = false) {
        ContenidoRegistro(
            llamadas = llamadasDemo(),
            onBorrarHistorial = {}
        )
    }
}

@Preview(
    name = "Registro · oscuro 130 %",
    showBackground = true,
    widthDp = 360,
    heightDp = 1000,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    fontScale = 1.3f
)
@Composable
private fun VistaPreviaRegistroOscuro() {
    MarcoVistaPrevia(darkTheme = true) {
        ContenidoRegistro(
            llamadas = emptyList(),
            onBorrarHistorial = {}
        )
    }
}
