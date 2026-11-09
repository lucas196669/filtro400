package com.example.filtro400

import android.os.Build
import android.telecom.Call
import android.telecom.CallScreeningService
import android.telecom.Connection
import android.util.Log
import java.time.LocalTime

// CallScreeningService: Android nos avisa de cada llamada entrante
// ANTES de que suene, y nosotros decidimos qué hacer con ella.
// Solo funciona con números que NO están en la agenda del usuario.
class FiltroLlamadasService : CallScreeningService() {

    companion object {
        // No repetimos el SMS al mismo número dentro de esta ventana.
        private const val VENTANA_SMS_MS = 30 * 60 * 1000L
    }

    // Android llama a este método automáticamente por cada llamada.
    override fun onScreenCall(callDetails: Call.Details) {

        // Solo filtramos llamadas entrantes (no las que haces tú).
        val esEntrante = callDetails.callDirection == Call.Details.DIRECTION_INCOMING

        // handle = la dirección de la llamada, algo como "tel:+34400123456".
        // schemeSpecificPart nos devuelve solo el número. Si es un número
        // oculto, viene vacío.
        val numero = callDetails.handle?.schemeSpecificPart.orEmpty()

        // Leemos prefijos y ajustes en cada llamada: así los cambios hechos
        // en la pantalla se aplican al instante, sin reiniciar nada.
        val baseDatos = BaseDatosFiltros.obtener(applicationContext)
        val prefijosActivos = baseDatos.prefijosActivos()
        val ajustes = RepositorioAjustes(applicationContext).leer()

        // Si el operador no pudo verificar el número, puede ser suplantado.
        val verificacionFallida = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
            callDetails.callerNumberVerificationStatus == Connection.VERIFICATION_STATUS_FAILED

        val ahora = LocalTime.now()
        val ahoraMs = System.currentTimeMillis()

        val decision = if (esEntrante) {
            ReglaBloqueo.decidir(
                numero = numero,
                prefijosActivos = prefijosActivos,
                ajustes = ajustes,
                ahoraMin = ahora.hour * 60 + ahora.minute,
                ahoraMs = ahoraMs,
                verificacionFallida = verificacionFallida
            )
        } else {
            // Las llamadas salientes nunca se bloquean.
            ReglaBloqueo.Decision(false)
        }

        val bloquear = decision.bloquear

        // Solo para depurar: ver la decisión en Logcat (filtra por "Filtro400").
        // Quítalo en una versión final: un número de teléfono es dato personal.
        Log.d("Filtro400", "Llamada de '$numero' -> ${if (bloquear) "BLOQUEADA" else "PERMITIDA"}")

        if (bloquear) {
            // Comprobamos el historial ANTES de registrar esta llamada,
            // para no contar dos veces el mismo intento.
            if (decision.enviarSms &&
                numero.isNotEmpty() &&
                !baseDatos.hayRespuestaReciente(numero, ahoraMs - VENTANA_SMS_MS)
            ) {
                EnviadorSms.enviar(applicationContext, numero, ajustes.textoSms)
            }

            baseDatos.registrarLlamada(
                numero = numero.ifEmpty { "Oculto" },
                motivo = decision.motivo?.texto ?: "Sin motivo"
            )

            if (decision.guardarEnLista) {
                val prefijo = ReglaBloqueo.normalizar(numero)
                if (ReglaBloqueo.esPrefijoValido(prefijo)) {
                    baseDatos.crear(prefijo)
                }
            }
        }

        // Siempre hay que responder, también cuando dejamos pasar la llamada.
        val respuesta = CallResponse.Builder()
            .setDisallowCall(bloquear)      // impide que suene
            .setRejectCall(bloquear)        // la rechaza
            .setSkipNotification(bloquear)  // sin notificación de llamada perdida
            .build()

        respondToCall(callDetails, respuesta)
    }
}
