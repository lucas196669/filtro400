package com.example.filtro400

import android.content.Context
import android.content.SharedPreferences

// Todos los ajustes de la app en un solo objeto.
data class Ajustes(
    val bloquearSpam: Boolean = true,
    val bloquearTodo: Boolean = false,
    val guardarEnLista: Boolean = false,
    val horarioActivo: Boolean = false,
    val inicioMin: Int = 22 * 60,
    val finMin: Int = 8 * 60,
    val respuestaSms: Boolean = true,
    val textoSms: String = "Ahora mismo no puedo atender llamadas. Escríbeme por aquí y te respondo en cuanto pueda.",
    val silencioHastaMs: Long = 0L
)

// Guarda y lee los ajustes con SharedPreferences, sin librerías nuevas.
class RepositorioAjustes(context: Context) {

    private val preferencias: SharedPreferences =
        context.applicationContext.getSharedPreferences(NOMBRE_PREFERENCIAS, Context.MODE_PRIVATE)

    fun leer(): Ajustes {
        // Partimos de los valores por defecto del data class.
        val porDefecto = Ajustes()
        return Ajustes(
            bloquearSpam = preferencias.getBoolean(CLAVE_BLOQUEAR_SPAM, porDefecto.bloquearSpam),
            bloquearTodo = preferencias.getBoolean(CLAVE_BLOQUEAR_TODO, porDefecto.bloquearTodo),
            guardarEnLista = preferencias.getBoolean(CLAVE_GUARDAR_EN_LISTA, porDefecto.guardarEnLista),
            horarioActivo = preferencias.getBoolean(CLAVE_HORARIO_ACTIVO, porDefecto.horarioActivo),
            inicioMin = preferencias.getInt(CLAVE_INICIO_MIN, porDefecto.inicioMin),
            finMin = preferencias.getInt(CLAVE_FIN_MIN, porDefecto.finMin),
            respuestaSms = preferencias.getBoolean(CLAVE_RESPUESTA_SMS, porDefecto.respuestaSms),
            textoSms = preferencias.getString(CLAVE_TEXTO_SMS, porDefecto.textoSms)
                ?: porDefecto.textoSms,
            silencioHastaMs = preferencias.getLong(CLAVE_SILENCIO_HASTA_MS, porDefecto.silencioHastaMs)
        )
    }

    fun guardar(ajustes: Ajustes) {
        preferencias.edit()
            .putBoolean(CLAVE_BLOQUEAR_SPAM, ajustes.bloquearSpam)
            .putBoolean(CLAVE_BLOQUEAR_TODO, ajustes.bloquearTodo)
            .putBoolean(CLAVE_GUARDAR_EN_LISTA, ajustes.guardarEnLista)
            .putBoolean(CLAVE_HORARIO_ACTIVO, ajustes.horarioActivo)
            .putInt(CLAVE_INICIO_MIN, ajustes.inicioMin)
            .putInt(CLAVE_FIN_MIN, ajustes.finMin)
            .putBoolean(CLAVE_RESPUESTA_SMS, ajustes.respuestaSms)
            .putString(CLAVE_TEXTO_SMS, ajustes.textoSms)
            .putLong(CLAVE_SILENCIO_HASTA_MS, ajustes.silencioHastaMs)
            .apply()
    }

    companion object {
        private const val NOMBRE_PREFERENCIAS = "ajustes_app"
        private const val CLAVE_BLOQUEAR_SPAM = "bloquear_spam"
        private const val CLAVE_BLOQUEAR_TODO = "bloquear_todo"
        private const val CLAVE_GUARDAR_EN_LISTA = "guardar_en_lista"
        private const val CLAVE_HORARIO_ACTIVO = "horario_activo"
        private const val CLAVE_INICIO_MIN = "inicio_min"
        private const val CLAVE_FIN_MIN = "fin_min"
        private const val CLAVE_RESPUESTA_SMS = "respuesta_sms"
        private const val CLAVE_TEXTO_SMS = "texto_sms"
        private const val CLAVE_SILENCIO_HASTA_MS = "silencio_hasta_ms"
    }
}
