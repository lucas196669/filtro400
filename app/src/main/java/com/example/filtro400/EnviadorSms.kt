package com.example.filtro400

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.SmsManager
import androidx.core.content.ContextCompat

// Envía un SMS de respuesta automática sin frenar el filtrado de la llamada.
object EnviadorSms {

    @Suppress("DEPRECATION")
    fun enviar(context: Context, numero: String, texto: String) {
        // Sin permiso no hacemos nada: nunca pedimos permisos desde aquí.
        val permisoConcedido = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED
        if (!permisoConcedido) return

        try {
            // Desde Android 12 (API 31) se obtiene el SmsManager del contexto.
            val smsManager: SmsManager? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(SmsManager::class.java)
            } else {
                SmsManager.getDefault()
            }
            smsManager ?: return

            // Partimos el texto por si supera el tamaño de un SMS normal.
            val partes = smsManager.divideMessage(texto)
            smsManager.sendMultipartTextMessage(numero, null, partes, null, null)
        } catch (_: Exception) {
            // Si el envío falla, la llamada ya está bloqueada y seguimos.
        }
    }
}
