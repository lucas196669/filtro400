package com.example.filtro400

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReglaBloqueoTest {

    private val prefijos = listOf("400")
    private val ahoraMs = 1_700_000_000_000L

    // Atajo para no repetir tantos parámetros en cada prueba.
    private fun decidir(
        numero: String,
        prefijosActivos: List<String> = prefijos,
        ajustes: Ajustes = Ajustes(),
        ahoraMin: Int = 12 * 60,
        verificacionFallida: Boolean = false
    ): ReglaBloqueo.Decision {
        return ReglaBloqueo.decidir(
            numero = numero,
            prefijosActivos = prefijosActivos,
            ajustes = ajustes,
            ahoraMin = ahoraMin,
            ahoraMs = ahoraMs,
            verificacionFallida = verificacionFallida
        )
    }

    @Test fun bloquea400SinPrefijoPais() =
        assertTrue(ReglaBloqueo.debeBloquearse("400123456", prefijos))

    @Test fun bloquea400ConMas34() =
        assertTrue(ReglaBloqueo.debeBloquearse("+34400123456", prefijos))

    @Test fun bloquea400Con0034() =
        assertTrue(ReglaBloqueo.debeBloquearse("0034400123456", prefijos))

    @Test fun bloquea400Con0034YEspacios() =
        assertTrue(ReglaBloqueo.debeBloquearse("0034 400 123 456", prefijos))

    @Test fun bloquea400ConEspacios() =
        assertTrue(ReglaBloqueo.debeBloquearse("+34 400 123 456", prefijos))

    @Test fun noBloqueaMovil() =
        assertFalse(ReglaBloqueo.debeBloquearse("612345678", prefijos))

    @Test fun noBloqueaFijo() =
        assertFalse(ReglaBloqueo.debeBloquearse("912345678", prefijos))

    @Test fun noBloqueaInternacionalQueContiene400() =
        assertFalse(ReglaBloqueo.debeBloquearse("+1400123456", prefijos))

    @Test fun noBloqueaNumeroVacio() =
        assertFalse(ReglaBloqueo.debeBloquearse("", prefijos))

    @Test fun noBloqueaSiNoHayPrefijosActivos() =
        assertFalse(ReglaBloqueo.debeBloquearse("400123456", emptyList()))

    @Test fun prefijoDesactivadoNoBloquea() {
        val decision = decidir("400123456", prefijosActivos = emptyList())
        assertFalse(decision.bloquear)
        assertNull(decision.motivo)
    }

    @Test fun spamDesactivadoNoBloqueaPorPrefijo() {
        val decision = decidir("400123456", ajustes = Ajustes(bloquearSpam = false))
        assertFalse(decision.bloquear)
    }

    @Test fun prefijoActivoBloqueaConMotivoPrefijo() {
        val decision = decidir("400123456")
        assertTrue(decision.bloquear)
        assertEquals(ReglaBloqueo.Motivo.PREFIJO, decision.motivo)
    }

    @Test fun verificacionFallidaBloqueaComoSuplantado() {
        val decision = decidir(
            numero = "612345678",
            verificacionFallida = true
        )
        assertTrue(decision.bloquear)
        assertEquals(ReglaBloqueo.Motivo.SUPLANTADO, decision.motivo)
    }

    @Test fun verificacionFallidaNoBloqueaSiSpamEstaDesactivado() {
        val decision = decidir(
            numero = "612345678",
            ajustes = Ajustes(bloquearSpam = false),
            verificacionFallida = true
        )
        assertFalse(decision.bloquear)
    }

    @Test fun bloqueoTotalBloqueaCualquierNumero() {
        val decision = decidir(
            numero = "612345678",
            ajustes = Ajustes(bloquearTodo = true, guardarEnLista = true)
        )
        assertTrue(decision.bloquear)
        assertEquals(ReglaBloqueo.Motivo.TODO, decision.motivo)
        assertTrue(decision.guardarEnLista)
    }

    @Test fun horarioNocturnoBloqueaA2330() {
        val decision = decidir(
            numero = "612345678",
            ajustes = ajustesHorarioNocturno(),
            ahoraMin = 23 * 60 + 30
        )
        assertTrue(decision.bloquear)
        assertEquals(ReglaBloqueo.Motivo.HORARIO, decision.motivo)
    }

    @Test fun horarioNocturnoBloqueaA0759() {
        val decision = decidir(
            numero = "612345678",
            ajustes = ajustesHorarioNocturno(),
            ahoraMin = 7 * 60 + 59
        )
        assertTrue(decision.bloquear)
        assertEquals(ReglaBloqueo.Motivo.HORARIO, decision.motivo)
    }

    @Test fun horarioNocturnoNoBloqueaA0800() {
        val decision = decidir(
            numero = "612345678",
            ajustes = ajustesHorarioNocturno(),
            ahoraMin = 8 * 60
        )
        assertFalse(decision.bloquear)
    }

    @Test fun fueraDeHorarioNoBloquea() {
        val decision = decidir(
            numero = "612345678",
            ajustes = ajustesHorarioNocturno(),
            ahoraMin = 12 * 60
        )
        assertFalse(decision.bloquear)
    }

    @Test fun horarioConInicioIgualAFinNoBloquea() {
        assertFalse(ReglaBloqueo.dentroDeHorario(22 * 60, 22 * 60, 22 * 60))
    }

    @Test fun horarioNormalDeDiaFunciona() {
        assertTrue(ReglaBloqueo.dentroDeHorario(9 * 60, 17 * 60, 10 * 60))
        assertFalse(ReglaBloqueo.dentroDeHorario(9 * 60, 17 * 60, 18 * 60))
    }

    @Test fun silencioTemporalBloqueaConSms() {
        val decision = decidir(
            numero = "612345678",
            ajustes = Ajustes(silencioHastaMs = ahoraMs + 60_000)
        )
        assertTrue(decision.bloquear)
        assertEquals(ReglaBloqueo.Motivo.SILENCIO, decision.motivo)
        assertTrue(decision.enviarSms)
    }

    @Test fun silencioExpiradoNoBloquea() {
        val decision = decidir(
            numero = "612345678",
            ajustes = Ajustes(silencioHastaMs = ahoraMs)
        )
        assertFalse(decision.bloquear)
    }

    @Test fun numeroOcultoNoCoincideConNingunPrefijo() {
        val decision = decidir("")
        assertFalse(decision.bloquear)
    }

    @Test fun numeroOcultoPuedeBloquearsePorBloqueoTotal() {
        val decision = decidir(
            numero = "",
            ajustes = Ajustes(bloquearTodo = true)
        )
        assertTrue(decision.bloquear)
        assertEquals(ReglaBloqueo.Motivo.TODO, decision.motivo)
    }

    @Test fun bloqueoPorPrefijoNoEnviaSms() {
        val decision = decidir("400123456")
        assertTrue(decision.bloquear)
        assertFalse(decision.enviarSms)
    }

    @Test fun bloqueoPorHorarioEnviaSms() {
        val decision = decidir(
            numero = "612345678",
            ajustes = ajustesHorarioNocturno(),
            ahoraMin = 23 * 60 + 30
        )
        assertTrue(decision.bloquear)
        assertTrue(decision.enviarSms)
    }

    @Test fun aceptaPrefijoDeDosDigitos() =
        assertTrue(ReglaBloqueo.esPrefijoValido("40"))

    @Test fun aceptaPrefijoConMasInicial() =
        assertTrue(ReglaBloqueo.esPrefijoValido("+400"))

    @Test fun rechazaPrefijoDeUnDigito() =
        assertFalse(ReglaBloqueo.esPrefijoValido("4"))

    @Test fun rechazaPrefijoConLetras() =
        assertFalse(ReglaBloqueo.esPrefijoValido("40A"))

    private fun ajustesHorarioNocturno() = Ajustes(
        horarioActivo = true,
        inicioMin = 22 * 60,
        finMin = 8 * 60,
        respuestaSms = true
    )
}
