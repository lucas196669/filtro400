package com.example.filtro400

// "object" = una única instancia, sin necesidad de crearla con ReglaBloqueo().
// Aquí vive la decisión de bloquear o no: sin Android, sin pantalla.
object ReglaBloqueo {

    // Motivos posibles para bloquear una llamada.
    enum class Motivo(val texto: String) {
        SUPLANTADO("Número suplantado"),
        PREFIJO("Prefijo o número bloqueado"),
        TODO("Bloqueo total"),
        HORARIO("Horario sin llamadas"),
        SILENCIO("Silencio temporal")
    }

    // Resultado completo de aplicar las reglas.
    data class Decision(
        val bloquear: Boolean,
        val motivo: Motivo? = null,
        val enviarSms: Boolean = false,
        val guardarEnLista: Boolean = false
    )

    // Un mismo número puede llegar de varias formas:
    // "400123456", "+34400123456", "0034 400 123 456", "+34 400 123 456"...
    // Lo dejamos siempre en el mismo formato antes de comparar.
    fun normalizar(numero: String): String {
        // Nos quedamos solo con dígitos y el signo "+" (fuera espacios y guiones).
        val limpio = numero.filter { it.isDigit() || it == '+' }
        return when {
            limpio.startsWith("+34") -> limpio.removePrefix("+34")
            limpio.startsWith("0034") -> limpio.removePrefix("0034")
            else -> limpio
        }
    }

    // Un prefijo válido son solo dígitos (con un "+" inicial opcional)
    // y al menos dos dígitos.
    fun esPrefijoValido(prefijo: String): Boolean {
        return Regex("\\+?\\d{2,}").matches(prefijo)
    }

    // La regla de la Fase 2: bloquear si el número empieza por algún
    // prefijo que esté activo en la base de datos.
    fun debeBloquearse(numero: String, prefijosActivos: List<String>): Boolean {
        val normalizado = normalizar(numero)
        return normalizado.isNotEmpty() && prefijosActivos.any { normalizado.startsWith(it) }
    }

    // Comprueba si "ahoraMin" cae dentro del horario. Soporta horarios que
    // cruzan la medianoche, como de 22:00 a 08:00.
    fun dentroDeHorario(inicioMin: Int, finMin: Int, ahoraMin: Int): Boolean {
        // Si inicio y fin son iguales, el horario está desactivado.
        if (inicioMin == finMin) return false

        return if (inicioMin < finMin) {
            // Horario normal, por ejemplo 09:00 a 17:00.
            ahoraMin >= inicioMin && ahoraMin < finMin
        } else {
            // Horario que cruza la medianoche, por ejemplo 22:00 a 08:00.
            ahoraMin >= inicioMin || ahoraMin < finMin
        }
    }

    // Aplica todas las reglas en orden. Gana la primera que coincide.
    fun decidir(
        numero: String,
        prefijosActivos: List<String>,
        ajustes: Ajustes,
        ahoraMin: Int,
        ahoraMs: Long,
        verificacionFallida: Boolean
    ): Decision {
        // 1) El operador no pudo verificar el número: posible suplantación.
        if (ajustes.bloquearSpam && verificacionFallida) {
            return Decision(true, Motivo.SUPLANTADO)
        }

        // 2) El número empieza por un prefijo activo.
        if (ajustes.bloquearSpam && debeBloquearse(numero, prefijosActivos)) {
            return Decision(true, Motivo.PREFIJO)
        }

        // 3) El usuario ha activado el bloqueo total.
        if (ajustes.bloquearTodo) {
            return Decision(
                bloquear = true,
                motivo = Motivo.TODO,
                guardarEnLista = ajustes.guardarEnLista
            )
        }

        // 4) Silencio temporal todavía vigente.
        if (ajustes.silencioHastaMs > ahoraMs) {
            return Decision(
                bloquear = true,
                motivo = Motivo.SILENCIO,
                enviarSms = ajustes.respuestaSms
            )
        }

        // 5) Estamos dentro del horario sin llamadas.
        if (ajustes.horarioActivo &&
            dentroDeHorario(ajustes.inicioMin, ajustes.finMin, ahoraMin)
        ) {
            return Decision(
                bloquear = true,
                motivo = Motivo.HORARIO,
                enviarSms = ajustes.respuestaSms
            )
        }

        // 6) Ninguna regla coincide: la llamada pasa.
        return Decision(false)
    }
}
