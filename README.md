# Filtro 400

App Android hecha en Kotlin para el RETO DAM. Su función principal es bloquear las llamadas entrantes que empiezan por el prefijo **400** (números típicos de spam comercial) usando `CallScreeningService`. Además de eso, deja añadir otros números o prefijos, poner un horario sin llamadas, activar un silencio temporal y ver un registro de las llamadas bloqueadas.

## Funcionalidades

- **Bloqueo por prefijo o número.** La lista empieza con el prefijo `400` ya activado. Se pueden añadir, editar, activar/desactivar y borrar prefijos o números completos (pestaña *Filtros*). Se guardan en una base de datos SQLite.
- **Probar un número.** En la pestaña *Filtros* se escribe un número y la app dice si se bloquearía con los prefijos activos.
- **Bloqueo de números suplantados.** Si el operador marca que no ha podido verificar el número (solo en Android 11 o superior), la llamada se bloquea.
- **Bloquear todas las llamadas desconocidas** (bloqueo total). Opcionalmente, los números bloqueados así se pueden añadir automáticamente a la lista.
- **Horario sin llamadas.** Se elige una hora de inicio y de fin (por defecto de 22:00 a 08:00, puede cruzar la medianoche).
- **Silencio temporal.** Botón "Silenciar 1 hora" en la pantalla de inicio.
- **Respuesta por SMS.** Cuando se bloquea por horario o por silencio, se puede enviar un SMS automático con un texto editable. Solo se envía si se ha dado el permiso `SEND_SMS`, y no se repite al mismo número si ya se le bloqueó una llamada en los últimos 30 minutos.
- **Registro de llamadas bloqueadas.** Pestaña *Registro* con las últimas 50 llamadas (número, motivo y fecha) y botón para borrar el historial. En la base de datos se guardan como máximo 200.
- **Contador** de llamadas bloqueadas hoy en la pantalla de inicio.
- Los ajustes se guardan con `SharedPreferences`.

## Cómo funciona

- **`ReglaBloqueo`**: es un `object` de Kotlin sin nada de Android. Aquí está toda la lógica: `normalizar()` limpia el número (quita espacios, guiones, `+34` y `0034`), `debeBloquearse()` mira si empieza por algún prefijo activo, y `decidir()` aplica las reglas en orden (suplantado → prefijo → bloqueo total → silencio → horario). La primera que coincide gana.
- **`FiltroLlamadasService`**: hereda de `CallScreeningService`. Android llama a `onScreenCall()` cada vez que entra una llamada, antes de que suene. El servicio lee los prefijos de la base de datos y los ajustes, le pregunta a `ReglaBloqueo` qué hacer, guarda la llamada en el registro si se bloquea y responde con `setDisallowCall`, `setRejectCall` y `setSkipNotification`. Las llamadas salientes nunca se bloquean.
- **`ROLE_CALL_SCREENING`**: es el rol que tiene que tener la app para que Android le pase las llamadas. La app no se lo puede dar a sí misma: con `RoleManager.createRequestRoleIntent()` se abre un diálogo del sistema y el usuario lo acepta. Sin este rol el servicio no hace nada.

## Decisiones técnicas

- **Lógica separada en `ReglaBloqueo`**: como no depende de Android, se puede probar con tests unitarios normales (JUnit) sin emulador. Además la usan tanto el servicio como la pantalla de "Probar un número", así no se repite código.
- **Normalizar `+34` y `0034`**: el mismo número puede llegar como `400123456`, `+34400123456` o `0034 400 123 456`. Si no se normaliza, un `startsWith("400")` fallaría con los dos últimos. También sirve para que `+1400123456` (un número de EE. UU.) **no** se bloquee.
- **Servicio registrado con `BIND_SCREENING_SERVICE`**: en el `AndroidManifest.xml` el servicio tiene `android:permission="android.permission.BIND_SCREENING_SERVICE"` y el `intent-filter` `android.telecom.CallScreeningService`. Así solo el sistema puede conectarse al servicio; otra app no puede llamarlo aunque sea `exported="true"`.
- **`minSdk = 29` (Android 10)**: `RoleManager.ROLE_CALL_SCREENING` y `callDirection` existen desde la API 29. La comprobación de número verificado (`callerNumberVerificationStatus`) es de la API 30, por eso va dentro de un `if (Build.VERSION.SDK_INT >= R)`.
- **SQLite con `SQLiteOpenHelper` (sin Room)**: para no añadir dependencias. La base de datos es un singleton y en la versión 2 se añadió la tabla `registro` sin borrar la de `filtros`.
- **Prefijos y ajustes se leen en cada llamada**: así un cambio en la pantalla se aplica al momento.
- **`uses-feature android.hardware.telephony required="false"`**: para que la app se pueda instalar aunque el dispositivo no tenga telefonía.
- **Interfaz con Jetpack Compose y Material 3**, con 4 pestañas: Inicio, Filtros, Horario y Registro.

## Cómo ejecutarlo

1. Abrir la carpeta del proyecto en Android Studio y esperar a que sincronice Gradle.
2. Crear o elegir un emulador con Android 10 (API 29) o superior.
3. Darle a **Run**.
4. En la pantalla de inicio pulsar **"Activar protección"**.
5. En el diálogo de Android, elegir *Filtro 400* como app de identificación de llamadas y spam (conceder el rol). La tarjeta debe cambiar a "Protección activada".
6. (Opcional) En la pestaña *Horario*, activar el mensaje automático y conceder el permiso de SMS.

## Cómo probarlo

### En el emulador

1. Con el rol concedido, abrir los **Extended controls** del emulador (botón `...`).
2. Ir a **Phone**.
3. Llamar desde **400123456** → la llamada se rechaza y no suena. Aparece en la pestaña *Registro*.
4. Llamar desde **612345678** → la llamada suena normal.

### Logcat

En Android Studio, abrir **Logcat** y filtrar por `Filtro400`. Por cada llamada sale una línea así:

```
Llamada de '400123456' -> BLOQUEADA
Llamada de '612345678' -> PERMITIDA
```

### Tests unitarios

Clic derecho en `app/src/test/java/com/example/filtro400/ReglaBloqueoTest.kt` > **Run 'ReglaBloqueoTest'**.

Hay 32 tests que comprueban, entre otras cosas:
- que se bloquea `400` con y sin `+34`/`0034` y con espacios,
- que no se bloquean móviles, fijos, `+1400...` ni números vacíos,
- el orden de las reglas y los motivos (suplantado, prefijo, total, horario, silencio),
- el horario que cruza la medianoche (23:30 y 07:59 bloquean, 08:00 no),
- cuándo se envía SMS y cuándo no,
- la validación de prefijos (`esPrefijoValido`).

## Capturas de pantalla

![Pantalla principal](capturas/pantalla-principal.png)

![Rol de filtrado de llamadas concedido](capturas/rol-concedido.png)

![Llamada desde 400123456 bloqueada](capturas/llamada-400-bloqueada.png)

![Logcat filtrado por Filtro400](capturas/logcat.png)

![Tests de ReglaBloqueoTest en verde](capturas/tests-verde.png)

## Preguntas de reflexión

<!-- TODO: copiar aquí el enunciado de las 6 preguntas del reto -->

### 1. (Pregunta 1 del reto)

Mi respuesta: (escribir aquí con mis palabras)

### 2. (Pregunta 2 del reto)

Mi respuesta: (escribir aquí con mis palabras)

### 3. (Pregunta 3 del reto)

Mi respuesta: (escribir aquí con mis palabras)

### 4. (Pregunta 4 del reto)

Mi respuesta: (escribir aquí con mis palabras)

### 5. (Pregunta 5 del reto)

Mi respuesta: (escribir aquí con mis palabras)

### 6. (Pregunta 6 del reto)

Mi respuesta: (escribir aquí con mis palabras)

## Posibles mejoras

- Quitar o recortar el `Log.d` con el número de teléfono en la versión final (el número es un dato personal; el propio código ya lo avisa).
- Normalizar también otros prefijos de país, no solo el de España (`+34` / `0034`).
- El SMS no se repite si ya hubo una llamada bloqueada de ese número en 30 minutos, pero no se guarda si el SMS se llegó a enviar. Se podría registrar aparte.
- Añadir tests instrumentados para `BaseDatosFiltros` (ahora solo hay tests de `ReglaBloqueo`).
- Usar un `ViewModel` para la pantalla y Room para la base de datos si la app crece.
- `CallScreeningService` no filtra los números que están en la agenda; se podría explicar mejor en la propia app.
- Poder exportar el registro de llamadas bloqueadas.
