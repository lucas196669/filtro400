package com.example.filtro400

import android.app.role.RoleManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import com.example.filtro400.ui.PantallaFiltros
import com.example.filtro400.ui.PantallaHorario
import com.example.filtro400.ui.PantallaInicio
import com.example.filtro400.ui.PantallaRegistro
import com.example.filtro400.ui.theme.Filtro400Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Filtro400Theme {
                AppFiltro400()
            }
        }
    }
}

private enum class Pestana(val titulo: String, val icono: ImageVector) {
    Inicio("Inicio", Icons.Filled.Home),
    Filtros("Filtros", Icons.AutoMirrored.Filled.List),
    Horario("Horario", Icons.Filled.DateRange),
    Registro("Registro", Icons.Filled.Call)
}

@Composable
private fun AppFiltro400() {
    val context = LocalContext.current
    val roleManager = remember { context.getSystemService(RoleManager::class.java) }
    val baseDatos = remember { BaseDatosFiltros.obtener(context) }
    val repositorioAjustes = remember { RepositorioAjustes(context) }

    var indicePestana by rememberSaveable { mutableStateOf(0) }
    val pestanaSeleccionada = Pestana.values()[indicePestana]

    var ajustes by remember { mutableStateOf(repositorioAjustes.leer()) }
    var proteccionActiva by remember {
        mutableStateOf(roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING))
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val alcance = rememberCoroutineScope()
    val mostrarMensaje: (String) -> Unit = { mensaje ->
        alcance.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(mensaje)
        }
    }

    // Guarda en SharedPreferences y actualiza la pantalla al mismo tiempo.
    val actualizarAjustes: (Ajustes) -> Unit = { nuevos ->
        repositorioAjustes.guardar(nuevos)
        ajustes = nuevos
    }

    // Android muestra su propio diálogo: la app no puede darse el rol a sí misma.
    val lanzadorRol = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        proteccionActiva = roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar {
                Pestana.values().forEachIndexed { indice, pestana ->
                    NavigationBarItem(
                        selected = indice == indicePestana,
                        onClick = { indicePestana = indice },
                        icon = {
                            Icon(
                                imageVector = pestana.icono,
                                contentDescription = pestana.titulo
                            )
                        },
                        label = { Text(pestana.titulo) }
                    )
                }
            }
        }
    ) { paddingInterno ->
        Crossfade(
            targetState = pestanaSeleccionada,
            label = "cambioDePestana"
        ) { pestana ->
            val modifier = Modifier.padding(paddingInterno)

            when (pestana) {
                Pestana.Inicio -> PantallaInicio(
                    modifier = modifier,
                    proteccionActiva = proteccionActiva,
                    ajustes = ajustes,
                    baseDatos = baseDatos,
                    onActivarProteccion = {
                        lanzadorRol.launch(
                            roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
                        )
                    },
                    onCambiarAjustes = actualizarAjustes,
                    onMensaje = mostrarMensaje
                )

                Pestana.Filtros -> PantallaFiltros(
                    modifier = modifier,
                    baseDatos = baseDatos,
                    ajustes = ajustes,
                    onCambiarAjustes = actualizarAjustes,
                    onMensaje = mostrarMensaje
                )

                Pestana.Horario -> PantallaHorario(
                    modifier = modifier,
                    ajustes = ajustes,
                    onCambiarAjustes = actualizarAjustes,
                    onMensaje = mostrarMensaje
                )

                Pestana.Registro -> PantallaRegistro(
                    modifier = modifier,
                    baseDatos = baseDatos,
                    onMensaje = mostrarMensaje
                )
            }
        }
    }
}
