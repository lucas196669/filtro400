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

### 1. ¿Cómo se entera la app de que entra una llamada?

Android tiene un servicio del sistema llamado `CallScreeningService`. Cuando entra una llamada, el sistema llama al método `onScreenCall()` de mi `FiltroLlamadasService` antes de que el teléfono suene, y me pasa los datos de la llamada (`Call.Details`): el número, si es entrante o saliente y, en Android 11 o superior, si el operador ha verificado el número. Con eso decido qué hacer y se lo contesto al sistema con `respondToCall()`: dejarla pasar o rechazarla.

### 2. ¿Por qué hace falta el rol de filtrado de llamadas?

Porque una app que filtra llamadas ve quién te llama y puede cortar llamadas, y eso es delicado para la privacidad. Android no deja que una app se dé ese poder a sí misma: con `RoleManager.createRequestRoleIntent(ROLE_CALL_SCREENING)` se abre un diálogo del sistema y es el usuario quien tiene que aceptarlo. Sin el rol, aunque el servicio esté declarado en el manifest, Android no se lo pasa y la app no hace nada.

### 3. ¿Hace falta un servidor o internet?

No. La decisión se toma dentro del móvil con el número que entrega el sistema. Los prefijos están en SQLite, los ajustes en `SharedPreferences` y la regla es código normal de Kotlin. Lo único que usa la red del operador es el SMS automático, pero no hay ningún servidor mío por detrás.

### 4. ¿Qué pasa con los contactos?

Según la documentación de Android, `CallScreeningService` se usa para llamadas de números que no están en la agenda del usuario, así que los contactos no pasan por el filtro. Esto es una limitación: aunque un contacto tenga un número que empiece por 400, la llamada no se rechaza. También significa que "bloquear todas las llamadas desconocidas" solo afecta a números que no son contactos.

### 5. ¿Qué diferencia hay entre que la función funcione y que el bloqueo funcione de verdad?

`debeBloquearse()` y `decidir()` son lógica pura: reciben un texto y devuelven una decisión. Por eso se pueden probar con tests unitarios sin emulador, y mis 32 tests en verde demuestran que la regla es correcta. Pero eso no prueba que se bloquee una llamada real, porque eso depende de más cosas:

- que el usuario haya concedido el rol,
- que el servicio esté registrado en el manifest con `BIND_SCREENING_SERVICE`,
- que la app responda a tiempo, porque Android espera pocos segundos,
- que el número llegue en un formato que entienda (`+34`, `0034`, espacios...) y que no llegue oculto o vacío,
- que el número no esté en la agenda,
- la versión de Android (la verificación del número solo existe desde la API 30).

Por eso, además de los tests, hice la prueba con llamadas simuladas en el emulador y comprobé la decisión en Logcat y en la pestaña *Registro*.

### 6. ¿Qué mejorarías?

- Quitar o recortar el `Log.d` que escribe el número de teléfono, porque es un dato personal.
- Normalizar también prefijos de otros países, no solo `+34` y `0034`.
- Añadir una lista de números spam conocidos o conectarla a una base de datos de spam.
- Usar `ViewModel` y Room si la app crece.
- Hacer tests instrumentados de `BaseDatosFiltros`, porque ahora solo hay tests de `ReglaBloqueo`.
- Poder exportar el registro de llamadas bloqueadas.
- Explicar dentro de la app que los contactos no se filtran.

## Posibles mejoras

- Quitar o recortar el `Log.d` con el número de teléfono en la versión final (el número es un dato personal; el propio código ya lo avisa).
- Normalizar también otros prefijos de país, no solo el de España (`+34` / `0034`).
- El SMS no se repite si ya hubo una llamada bloqueada de ese número en 30 minutos, pero no se guarda si el SMS se llegó a enviar. Se podría registrar aparte.
- Añadir tests instrumentados para `BaseDatosFiltros` (ahora solo hay tests de `ReglaBloqueo`).
- Usar un `ViewModel` para la pantalla y Room para la base de datos si la app crece.
- `CallScreeningService` no filtra los números que están en la agenda; se podría explicar mejor en la propia app.
- Poder exportar el registro de llamadas bloqueadas.
