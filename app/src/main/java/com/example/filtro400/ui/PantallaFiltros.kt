package com.example.filtro400.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.filtro400.Ajustes
import com.example.filtro400.BaseDatosFiltros
import com.example.filtro400.Filtro
import com.example.filtro400.ReglaBloqueo
import com.example.filtro400.ui.theme.Espaciado

@Composable
fun PantallaFiltros(
    modifier: Modifier = Modifier,
    baseDatos: BaseDatosFiltros,
    ajustes: Ajustes,
    onCambiarAjustes: (Ajustes) -> Unit,
    onMensaje: (String) -> Unit = {}
) {
    var filtros by remember { mutableStateOf(baseDatos.listar()) }
    var nuevoTexto by rememberSaveable { mutableStateOf("") }
    var errorNuevo by remember { mutableStateOf<String?>(null) }
    var filtroEditando by remember { mutableStateOf<Filtro?>(null) }
    var textoEdicion by remember { mutableStateOf("") }
    var errorEdicion by remember { mutableStateOf<String?>(null) }
    var filtroABorrar by remember { mutableStateOf<Filtro?>(null) }
    var numeroPrueba by rememberSaveable { mutableStateOf("") }
    var resultadoPrueba by remember { mutableStateOf("") }

    val recargar = {
        filtros = baseDatos.listar()
    }

    ContenidoFiltros(
        modifier = modifier,
        filtros = filtros,
        nuevoTexto = nuevoTexto,
        onNuevoTextoChange = { texto ->
            nuevoTexto = texto
            errorNuevo = null
        },
        errorNuevo = errorNuevo,
        onAgregar = {
            val prefijo = ReglaBloqueo.normalizar(nuevoTexto)
            when {
                !ReglaBloqueo.esPrefijoValido(prefijo) -> {
                    errorNuevo = "Escribe al menos 2 dígitos (por ejemplo 400)."
                }

                !baseDatos.crear(prefijo) -> {
                    errorNuevo = "Ese número o prefijo ya está en la lista. " +
                        "Prueba con otro prefijo (por ejemplo, 900)."
                }

                else -> {
                    nuevoTexto = ""
                    errorNuevo = null
                    recargar()
                    onMensaje("Se ha añadido $prefijo a la lista")
                }
            }
        },
        onCambiarActivo = { filtro, activo ->
            baseDatos.cambiarActivo(filtro.id, activo)
            recargar()
        },
        onEditar = { filtro ->
            filtroEditando = filtro
            textoEdicion = filtro.prefijo
            errorEdicion = null
        },
        onBorrar = { filtro ->
            filtroABorrar = filtro
        },
        numeroPrueba = numeroPrueba,
        onNumeroPruebaChange = { numeroPrueba = it },
        resultadoPrueba = resultadoPrueba,
        onProbar = {
            val prefijosActivos = filtros
                .filter { it.activo }
                .map { it.prefijo }
            resultadoPrueba = when {
                numeroPrueba.isBlank() -> "Escribe un número para probarlo."
                ReglaBloqueo.debeBloquearse(numeroPrueba, prefijosActivos) -> {
                    "Esta llamada se bloquearía."
                }
                else -> "Esta llamada pasaría."
            }
        },
        ajustes = ajustes,
        onCambiarAjustes = onCambiarAjustes
    )

    filtroEditando?.let { filtro ->
        val errorParaMostrar = errorEdicion
        AlertDialog(
            onDismissRequest = {
                filtroEditando = null
                errorEdicion = null
            },
            title = { Text("Editar número o prefijo (ej. 400)") },
            text = {
                OutlinedTextField(
                    value = textoEdicion,
                    onValueChange = {
                        textoEdicion = it
                        errorEdicion = null
                    },
                    label = { Text("Número o prefijo (ej. 400)") },
                    singleLine = true,
                    isError = errorParaMostrar != null,
                    supportingText = {
                        Text(
                            text = errorParaMostrar
                                ?: "Escribe un número completo o solo el comienzo, por ejemplo 400."
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val prefijo = ReglaBloqueo.normalizar(textoEdicion)
                    when {
                        !ReglaBloqueo.esPrefijoValido(prefijo) -> {
                            errorEdicion = "Escribe al menos 2 dígitos (por ejemplo 400)."
                        }

                        !baseDatos.modificar(filtro.id, prefijo) -> {
                            errorEdicion = "Ese número o prefijo ya está en la lista. " +
                                "Prueba con otro prefijo (por ejemplo, 900)."
                        }

                        else -> {
                            filtroEditando = null
                            errorEdicion = null
                            recargar()
                            onMensaje("Cambios guardados: $prefijo")
                        }
                    }
                }) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    filtroEditando = null
                    errorEdicion = null
                }) {
                    Text("Cancelar")
                }
            }
        )
    }

    filtroABorrar?.let { filtro ->
        DialogoConfirmacion(
            titulo = "Borrar de la lista",
            mensaje = "Se dejará de bloquear ${filtro.prefijo}. ¿Quieres quitarlo de la lista?",
            textoConfirmar = "Borrar",
            onConfirmar = {
                baseDatos.eliminar(filtro.id)
                recargar()
                onMensaje("Se ha borrado ${filtro.prefijo}")
            },
            onCancelar = {
                filtroABorrar = null
            }
        )
    }
}

@Composable
internal fun ContenidoFiltros(
    modifier: Modifier = Modifier,
    filtros: List<Filtro>,
    nuevoTexto: String,
    onNuevoTextoChange: (String) -> Unit,
    errorNuevo: String?,
    onAgregar: () -> Unit,
    onCambiarActivo: (Filtro, Boolean) -> Unit,
    onEditar: (Filtro) -> Unit,
    onBorrar: (Filtro) -> Unit,
    numeroPrueba: String,
    onNumeroPruebaChange: (String) -> Unit,
    resultadoPrueba: String,
    onProbar: () -> Unit,
    ajustes: Ajustes,
    onCambiarAjustes: (Ajustes) -> Unit,
    seccionPruebaAbierta: Boolean = false
) {
    ContenedorPantalla(modifier = modifier) {
        EncabezadoPantalla(
            titulo = "Números bloqueados",
            subtitulo = "Añade un número completo o un prefijo (por ejemplo, 400)."
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
                    text = "Añadir a la lista",
                    style = MaterialTheme.typography.titleMedium
                )

                OutlinedTextField(
                    value = nuevoTexto,
                    onValueChange = onNuevoTextoChange,
                    label = { Text("Número o prefijo (ej. 400)") },
                    placeholder = { Text("Ej. 400 o 400 123 456") },
                    singleLine = true,
                    isError = errorNuevo != null,
                    supportingText = {
                        Text(
                            text = errorNuevo
                                ?: "Escribe un número completo o solo el comienzo, por ejemplo 400."
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = onAgregar,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(Espaciado.sm))
                    Text("Añadir a la lista")
                }
            }
        }

        Text(
            text = if (filtros.isEmpty()) "Tu lista" else "Tu lista (${filtros.size})",
            style = MaterialTheme.typography.titleMedium
        )

        if (filtros.isEmpty()) {
            EstadoVacio(
                icono = Icons.AutoMirrored.Filled.List,
                titulo = "Tu lista está vacía",
                texto = "Añade un número completo o un prefijo (por ejemplo, 400) para empezar."
            )
        } else {
            filtros.forEach { filtro ->
                FilaFiltro(
                    filtro = filtro,
                    onCambiarActivo = { activo ->
                        onCambiarActivo(filtro, activo)
                    },
                    onEditar = {
                        onEditar(filtro)
                    },
                    onBorrar = {
                        onBorrar(filtro)
                    }
                )
            }
        }

        SeccionPlegable(
            titulo = "Probar un número",
            inicialmenteAbierta = seccionPruebaAbierta
        ) {
            OutlinedTextField(
                value = numeroPrueba,
                onValueChange = onNumeroPruebaChange,
                label = { Text("Número de prueba (ej. 400 123 456)") },
                placeholder = { Text("Ej. 400 123 456") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedButton(
                onClick = onProbar,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
            ) {
                Text("Probar número")
            }
            if (resultadoPrueba.isNotEmpty()) {
                Text(
                    text = resultadoPrueba,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (resultadoPrueba == "Esta llamada se bloquearía.") {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }

        Text(
            text = "Opciones avanzadas",
            style = MaterialTheme.typography.titleMedium
        )

        TarjetaAjuste(
            icono = Icons.AutoMirrored.Filled.List,
            titulo = "Guardar los números del bloqueo total",
            descripcion = "Se añadirán a la lista cuando el bloqueo total rechace una llamada.",
            activado = ajustes.guardarEnLista,
            onCambio = { activo ->
                onCambiarAjustes(ajustes.copy(guardarEnLista = activo))
            }
        )
    }
}

@Composable
private fun FilaFiltro(
    filtro: Filtro,
    onCambiarActivo: (Boolean) -> Unit,
    onEditar: () -> Unit,
    onBorrar: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp)
                .padding(
                    start = Espaciado.lg,
                    end = Espaciado.xs,
                    top = Espaciado.sm,
                    bottom = Espaciado.sm
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Espaciado.xs)
            ) {
                Text(
                    text = filtro.prefijo,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = if (filtro.activo) "Bloqueando" else "En pausa",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (filtro.activo) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
            Switch(
                checked = filtro.activo,
                onCheckedChange = onCambiarActivo
            )
            IconButton(onClick = onEditar) {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = "Editar el bloqueo de ${filtro.prefijo}"
                )
            }
            IconButton(onClick = onBorrar) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Borrar el bloqueo de ${filtro.prefijo}",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
