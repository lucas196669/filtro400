package com.example.filtro400

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.time.LocalDate
import java.time.ZoneId

// Un filtro tal y como lo guardamos en la base de datos.
data class Filtro(val id: Long, val prefijo: String, val activo: Boolean)

// Una llamada que hemos bloqueado, para poder revisarla después.
data class LlamadaBloqueada(
    val id: Long,
    val numero: String,
    val motivo: String,
    val fecha: Long
)

// SQLiteOpenHelper nos crea y abre la base de datos por nosotros.
// Usamos SQLite puro (sin Room) para no añadir dependencias.
class BaseDatosFiltros private constructor(context: Context) :
    SQLiteOpenHelper(context.applicationContext, "filtros.db", null, 3) {

    companion object {
        private const val NOMBRE_TABLA = "filtros"
        private const val NOMBRE_REGISTRO = "registro"

        // Una única instancia compartida en toda la app.
        @Volatile
        private var instancia: BaseDatosFiltros? = null

        fun obtener(context: Context): BaseDatosFiltros {
            return instancia ?: synchronized(this) {
                instancia ?: BaseDatosFiltros(context.applicationContext).also {
                    instancia = it
                }
            }
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        // Cada prefijo es único para no guardar duplicados.
        db.execSQL(
            "CREATE TABLE $NOMBRE_TABLA (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "prefijo TEXT NOT NULL UNIQUE, " +
                    "activo INTEGER NOT NULL DEFAULT 1)"
        )
        // Empezamos con el "400" de la Fase 1, ya activado.
        db.execSQL("INSERT INTO $NOMBRE_TABLA (prefijo, activo) VALUES ('400', 1)")

        // Tabla nueva de la Fase 3: historial de llamadas bloqueadas.
        crearTablaRegistro(db)
    }

    // La tabla del historial se crea siempre con las mismas columnas.
    private fun crearTablaRegistro(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS $NOMBRE_REGISTRO (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "numero TEXT NOT NULL, " +
                    "motivo TEXT NOT NULL, " +
                    "fecha INTEGER NOT NULL)"
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, versionAnterior: Int, versionNueva: Int) {
        // IMPORTANTE: nunca borramos "filtros": son los prefijos del usuario.
        // Hasta la versión 2 la tabla "registro" pudo crearse sin la columna
        // "motivo". Como solo es un historial, la volvemos a crear bien.
        if (versionAnterior < 3) {
            db.execSQL("DROP TABLE IF EXISTS $NOMBRE_REGISTRO")
            crearTablaRegistro(db)
        }
    }

    // CREATE: devuelve false si el prefijo ya existe.
    fun crear(prefijo: String): Boolean {
        val valores = ContentValues().apply {
            put("prefijo", prefijo)
            put("activo", 1)
        }
        val id = writableDatabase.insertWithOnConflict(
            NOMBRE_TABLA, null, valores, SQLiteDatabase.CONFLICT_IGNORE
        )
        return id != -1L
    }

    // READ: todos los filtros, activos o no.
    fun listar(): List<Filtro> {
        val filtros = mutableListOf<Filtro>()
        readableDatabase.query(
            NOMBRE_TABLA,
            arrayOf("id", "prefijo", "activo"),
            null, null, null, null,
            "prefijo ASC"
        ).use { cursor ->
            while (cursor.moveToNext()) {
                filtros.add(
                    Filtro(
                        id = cursor.getLong(0),
                        prefijo = cursor.getString(1),
                        activo = cursor.getInt(2) == 1
                    )
                )
            }
        }
        return filtros
    }

    // READ: solo los prefijos activos, que son los que usa el servicio.
    fun prefijosActivos(): List<String> {
        val prefijos = mutableListOf<String>()
        readableDatabase.query(
            NOMBRE_TABLA,
            arrayOf("prefijo"),
            "activo = 1",
            null, null, null,
            "prefijo ASC"
        ).use { cursor ->
            while (cursor.moveToNext()) {
                prefijos.add(cursor.getString(0))
            }
        }
        return prefijos
    }

    // UPDATE: devuelve false si el nuevo prefijo choca con otro existente.
    fun modificar(id: Long, prefijo: String): Boolean {
        val valores = ContentValues().apply {
            put("prefijo", prefijo)
        }
        val filas = writableDatabase.updateWithOnConflict(
            NOMBRE_TABLA,
            valores,
            "id = ?",
            arrayOf(id.toString()),
            SQLiteDatabase.CONFLICT_IGNORE
        )
        return filas > 0
    }

    // UPDATE: activa o desactiva un filtro sin borrarlo.
    fun cambiarActivo(id: Long, activo: Boolean) {
        val valores = ContentValues().apply {
            put("activo", if (activo) 1 else 0)
        }
        writableDatabase.update(
            NOMBRE_TABLA,
            valores,
            "id = ?",
            arrayOf(id.toString())
        )
    }

    // DELETE: borra el filtro indicado.
    fun eliminar(id: Long) {
        writableDatabase.delete(
            NOMBRE_TABLA,
            "id = ?",
            arrayOf(id.toString())
        )
    }

    // CREATE: guarda una llamada bloqueada y limita el historial a 200 filas.
    fun registrarLlamada(numero: String, motivo: String) {
        val valores = ContentValues().apply {
            put("numero", numero)
            put("motivo", motivo)
            put("fecha", System.currentTimeMillis())
        }
        writableDatabase.insert(NOMBRE_REGISTRO, null, valores)

        // Borramos las más antiguas para no crecer sin límite.
        writableDatabase.execSQL(
            "DELETE FROM $NOMBRE_REGISTRO WHERE id NOT IN " +
                    "(SELECT id FROM $NOMBRE_REGISTRO ORDER BY fecha DESC, id DESC LIMIT 200)"
        )
    }

    // READ: las últimas llamadas bloqueadas, de la más reciente a la más antigua.
    fun ultimasLlamadas(limite: Int = 50): List<LlamadaBloqueada> {
        val llamadas = mutableListOf<LlamadaBloqueada>()
        readableDatabase.query(
            NOMBRE_REGISTRO,
            arrayOf("id", "numero", "motivo", "fecha"),
            null, null, null, null,
            "fecha DESC, id DESC",
            limite.coerceAtLeast(0).toString()
        ).use { cursor ->
            while (cursor.moveToNext()) {
                llamadas.add(
                    LlamadaBloqueada(
                        id = cursor.getLong(0),
                        numero = cursor.getString(1),
                        motivo = cursor.getString(2),
                        fecha = cursor.getLong(3)
                    )
                )
            }
        }
        return llamadas
    }

    // READ: cuántas llamadas se han bloqueado desde las 00:00 de hoy.
    fun contarBloqueadasHoy(): Int {
        val inicioHoy = LocalDate.now()
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        var total = 0
        readableDatabase.rawQuery(
            "SELECT COUNT(*) FROM $NOMBRE_REGISTRO WHERE fecha >= ?",
            arrayOf(inicioHoy.toString())
        ).use { cursor ->
            if (cursor.moveToFirst()) {
                total = cursor.getInt(0)
            }
        }
        return total
    }

    // READ: ¿ya registramos una respuesta a este número en la ventana indicada?
    fun hayRespuestaReciente(numero: String, desdeMs: Long): Boolean {
        var hay = false
        readableDatabase.rawQuery(
            "SELECT COUNT(*) FROM $NOMBRE_REGISTRO WHERE numero = ? AND fecha >= ?",
            arrayOf(numero, desdeMs.toString())
        ).use { cursor ->
            if (cursor.moveToFirst()) {
                hay = cursor.getInt(0) > 0
            }
        }
        return hay
    }

    // DELETE: vacía todo el historial de llamadas bloqueadas.
    fun borrarRegistro() {
        writableDatabase.delete(NOMBRE_REGISTRO, null, null)
    }
}