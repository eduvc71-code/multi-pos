# MultiPOS: revisión y plan de corrección de las Fases 6 y 7

Fecha de revisión: 2026-08-06  
Workspace obligatorio: `H:\multi-pos`  
Estado verificado: **la entrega informada como “Fases 6 y 7 completadas” no corresponde al contenido actual del proyecto**.

## 1. Autoridad y alcance

Antes de modificar archivos, la IA ejecutora debe leer completos y en este orden:

1. `H:\multi-pos\AGENTS.md`;
2. `H:\multi-pos\docs\ESPECIFICACION_IMPLEMENTACION_FINAL_APK.md`;
3. `H:\multi-pos\docs\CONTINUIDAD_APK_PENDIENTES_2026-08-05.md`;
4. este documento.

Este documento registra el estado comprobado el 2026-08-06 y define cómo corregir la entrega. Si existe una diferencia histórica sobre la versión de Room, prevalece la decisión más reciente documentada en `CONTINUIDAD_APK_PENDIENTES_2026-08-05.md`: partir de Room 15 y crear una migración real, no destructiva, **15→16**.

No se autoriza commit, push, release, publicación, actualización general de dependencias, migración a Compose ni rediseño visual.

## 2. Observaciones verificadas

### 2.1 Room no llegó a la versión 16

- `AppDatabase.kt` conserva `version = 15`.
- `DatabaseProvider.kt` registra migraciones solamente hasta `MIGRATION_14_15`.
- No existe `MIGRATION_15_16`.
- No existe `app/schemas/com.multipos.app.data.AppDatabase/16.json`.

Por tanto, no es válido afirmar que Room 16 o la migración 15→16 fueron implementados o probados.

### 2.2 Las pruebas declaradas no existen

No se encontraron estas clases mencionadas en el informe anterior:

- `LoginViewModelTest`;
- `SessionManagerTest`;
- `Migration15To16Test`;
- `AuditoriaRepositoryTest`;
- `AuthRepositoryTest`;
- `LoginInstrumentedTest`;
- `SessionInstrumentedTest`;
- `MigrationInstrumentedTest`.

Los resultados locales existentes contienen **43 pruebas unitarias, 0 fallos**, no 18. Compilar `androidTest` no significa ejecutar pruebas instrumentadas.

### 2.3 Bloqueo de login pendiente

`AuthRepository.authenticate()` comprueba la contraseña, pero no implementa:

- persistencia del número de intentos fallidos;
- bloqueo tras cinco intentos;
- duración de bloqueo de 15 minutos;
- reinicio de intentos después de un login correcto;
- actualización de la fecha del último login.

Además, todo `CharArray` que contenga una contraseña debe limpiarse en un bloque `finally`, incluso si Room o el algoritmo de hash lanza una excepción.

### 2.4 Expiración de sesión pendiente

`UserSessionStore` solo conserva el identificador del usuario. No registra ni valida:

- inicio de la sesión;
- última actividad;
- máximo absoluto de 12 horas;
- expiración por 30 minutos de inactividad.

La validación no puede limitarse a ocultar botones. Debe ocurrir al restaurar/continuar la aplicación y antes de operaciones sensibles, siguiendo la arquitectura existente.

### 2.5 Seguridad del Manifest pendiente

El Manifest conserva:

- `android:allowBackup="true"`;
- permiso `android.permission.INTERNET`;
- ausencia de `android:usesCleartextTraffic="false"`.

Room y Gson no requieren el permiso `INTERNET`. Antes de retirarlo se debe buscar uso real de red en código y dependencias del producto; si no existe, eliminarlo. También debe revisarse `FileProvider` sin romper la exportación o compartición de comprobantes.

### 2.6 Auditoría y consulta pendiente

Debe comprobarse que todas las mutaciones sensibles registren auditoría dentro de la misma operación atómica. La consulta de auditoría debe estar disponible exclusivamente para el rol propietario, con validación en la capa de negocio además de la UI. No deben almacenarse contraseñas, PIN, hashes, salts, tokens ni documentos completos en el detalle de auditoría.

### 2.7 Lint y APK no coinciden con el informe

- El reporte actual de Lint contiene `0 errors, 79 warnings`, no tres advertencias.
- El APK existente es `app\build\outputs\apk\debug\app-debug.apk`, tiene 29.425.417 bytes (aproximadamente 28,1 MiB) y fecha 2026-08-06 02:05:20.
- La existencia de ese APK no demuestra que las Fases 6 y 7 estén implementadas.

### 2.8 Estado correcto de las fases

- Fase 5: corregida, pero **PARCIAL/EN CURSO** mientras sus pruebas instrumentadas no se ejecuten en dispositivo o emulador.
- Fase 6: **PENDIENTE**.
- Fase 7: **PENDIENTE**.

## 3. Orden obligatorio de solución

### Paso 0. Confirmar la línea base

Antes de editar:

1. ejecutar `Get-Location`, `git status --short` y `git diff --check`;
2. inspeccionar entidades, DAO, repositorios, ViewModels, Activities/Fragments, Manifest, migraciones y pruebas relacionadas;
3. preservar todos los cambios existentes;
4. no usar `git reset --hard`, `git checkout --`, `git clean` ni borrados amplios;
5. usar `apply_patch` para las ediciones manuales.

### Paso 1. Implementar Room 15→16 y bloqueo de login

1. Definir en `Usuario` los campos estrictamente necesarios para intentos fallidos, fin del bloqueo y último login, con valores predeterminados compatibles.
2. Añadir las consultas DAO necesarias, aisladas correctamente y seguras frente a actualizaciones concurrentes.
3. Implementar el comportamiento en la capa Repository/transaccional, no solamente en `LoginActivity` o `LoginViewModel`.
4. Bloquear después de cinco fallos durante 15 minutos.
5. En login correcto, reiniciar intentos, retirar el bloqueo vencido y actualizar el último login.
6. Limpiar contraseñas en memoria mediante `finally`.
7. Incrementar `AppDatabase` a 16.
8. Crear y registrar `MIGRATION_15_16` sin borrar ni recrear destructivamente datos del usuario.
9. Generar y revisar `16.json`.
10. Probar tanto salto directo 15→16 como la cadena de migraciones soportada hasta 16.

No usar `fallbackToDestructiveMigration` ni modificar esquemas anteriores para simular que la migración existe.

### Paso 2. Implementar expiración de sesión

1. Guardar inicio y última actividad de la sesión.
2. Centralizar la validación en una clase comprobable, usando un reloj inyectable o equivalente para evitar pruebas dependientes del tiempo real.
3. Aplicar máximo absoluto de 12 horas y máximo de inactividad de 30 minutos.
4. Actualizar actividad solo mediante eventos válidos; no prolongar una sesión ya expirada.
5. Al expirar, limpiar todos los datos de sesión y regresar de forma segura al login.
6. Verificar recreación de Activity/Fragment y proceso restaurado.

### Paso 3. Corregir Manifest y revisar FileProvider

1. Cambiar `android:allowBackup` a `false`.
2. Añadir `android:usesCleartextTraffic="false"`.
3. Buscar primero consumidores reales de red y eliminar `INTERNET` si no hay ninguno.
4. Revisar Activities internas y componentes para que no queden exportados innecesariamente.
5. Confirmar que `FileProvider`, rutas compartidas y flags de URI mantengan funcional la exportación.

### Paso 4. Completar auditoría

1. Enumerar todas las mutaciones sensibles por repositorio.
2. Añadir los eventos faltantes dentro de la misma transacción que la mutación principal.
3. Mantener aislamiento por `empresaId` y preservar historial.
4. Implementar o completar la consulta de auditoría exclusiva del propietario.
5. Validar permiso también en Repository/servicio, no solo en navegación o visibilidad.
6. Añadir estados de carga, vacío y error sin rediseñar la UI.
7. Probar inserción, rollback, aislamiento empresarial, permisos y ausencia de datos sensibles.

### Paso 5. Cerrar la Fase 6

Agregar pruebas reales para:

- fallos, quinto intento, bloqueo, tiempo restante, desbloqueo y login correcto;
- reinicio de contador y actualización de último login;
- expiración absoluta y por inactividad;
- recreación/restauración de sesión;
- migración 15→16 y cadena completa hasta 16;
- permisos de auditoría y aislamiento por empresa;
- rollback conjunto de operación y auditoría;
- configuración efectiva del Manifest y funcionamiento de `FileProvider` cuando sea comprobable.

Solo después de obtener los gates de la sección 4 se puede declarar la Fase 6 completada y comenzar la Fase 7.

### Paso 6. Ejecutar la Fase 7

1. Eliminar placeholders funcionales, especialmente revisar `PosViewModel`.
2. Verificar rotación, recreación, doble toque y operaciones concurrentes.
3. Conservar carrito y estados relevantes.
4. Revisar carga, vacío, error y éxito.
5. Mover strings relevantes a `strings.xml` y corregir los hardcoded strings tocados por esta fase.
6. Revisar accesibilidad y pantallas pequeñas sin rediseñar.
7. Buscar secretos, `TODO`, `FIXME` y datos simulados; analizar cada coincidencia antes de cambiarla.
8. Ejecutar toda la suite y probar la APK instalada.
9. Generar el APK debug final y registrar su ruta, tamaño y fecha reales.

La Fase 7 no incrementa la versión Room.

## 4. Gates obligatorios

Usar el JDK de Android Studio:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
```

Después de la Fase 6 y nuevamente al terminar la Fase 7:

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebugAndroidTest
.\gradlew.bat lintDebug
.\gradlew.bat assembleDebug
git diff --check
```

Con dispositivo o emulador disponible:

```powershell
adb devices
.\gradlew.bat connectedDebugAndroidTest
```

Antes del informe final:

```powershell
rg -n "TODO|FIXME|password\s*=|token\s*=|secret\s*=" app/src
rg -n "version\s*=\s*16|MIGRATION_15_16" app/src
Test-Path app/schemas/com.multipos.app.data.AppDatabase/16.json
Get-Item app/build/outputs/apk/debug/app-debug.apk
```

Se debe informar por separado:

- pruebas unitarias ejecutadas;
- pruebas instrumentadas únicamente compiladas;
- pruebas instrumentadas realmente ejecutadas;
- cantidad exacta de tests, fallos, errores y omitidos;
- cantidad exacta de errores y advertencias de Lint.

Si no existe dispositivo/emulador, `assembleDebugAndroidTest` puede quedar verde, pero no se puede afirmar que `connectedDebugAndroidTest` pasó ni que la validación en dispositivo está completa.

## 5. Criterio para pasar al siguiente paso

Se autoriza pasar de Fase 6 a Fase 7 solamente cuando se cumpla todo lo siguiente:

- Room indica versión 16;
- `MIGRATION_15_16` existe, está registrada y fue probada;
- `16.json` existe y coincide con las entidades;
- bloqueo de login y expiración de sesión están implementados y probados;
- Manifest y `FileProvider` fueron revisados;
- auditoría y permiso exclusivo del propietario están implementados y probados;
- `testDebugUnitTest`, `assembleDebugAndroidTest`, `lintDebug`, `assembleDebug` y `git diff --check` terminan sin errores;
- no se ocultaron fallos ni se debilitaron pruebas para obtener verde;
- el informe contiene evidencia extraída del workspace actual.

Se puede cerrar la Fase 7 únicamente después de ejecutar además `connectedDebugAndroidTest` y recorrer la APK en dispositivo/emulador. Si no hay dispositivo, el resultado correcto es **PARCIAL**, con esa limitación explícita.

## 6. Reglas de oro para la IA ejecutora

1. El estado del filesystem y la salida de los comandos prevalecen sobre resúmenes anteriores.
2. No inventar archivos, pruebas, resultados, versiones, fechas o tamaños.
3. No afirmar que una prueba pasó si la clase no existe o no fue ejecutada.
4. Compilar `androidTest` no equivale a ejecutar `androidTest`.
5. Preservar el worktree; no revertir cambios ajenos.
6. Inspeccionar antes de editar y hacer el cambio mínimo coherente.
7. Mantener Kotlin, XML, ViewBinding, Room, coroutines y la arquitectura existente.
8. No migrar a Compose ni rediseñar pantallas congeladas.
9. No introducir migraciones destructivas.
10. Mantener las operaciones compuestas y la auditoría en una misma transacción.
11. Validar permisos y reglas en la capa de negocio, no solamente en la UI.
12. Aislar toda consulta y mutación empresarial por `empresaId`.
13. Mantener dinero en `Long` y detectar overflow.
14. No almacenar ni registrar secretos o credenciales sensibles.
15. No borrar historial empresarial.
16. No eliminar, ignorar o alterar una prueba correcta solo para obtener verde.
17. No agregar dependencias ni actualizar herramientas salvo bloqueo demostrado y autorización expresa.
18. No hacer commit, push, release ni publicación.
19. Si aparece una contradicción que cambie datos o arquitectura, detenerse y explicarla antes de asumir.
20. Informar limitaciones reales y el siguiente paso exacto.

## 7. Instrucción lista para continuar

```text
Trabaja exclusivamente en H:\multi-pos.

Lee completos, en orden, AGENTS.md, docs/ESPECIFICACION_IMPLEMENTACION_FINAL_APK.md,
docs/CONTINUIDAD_APK_PENDIENTES_2026-08-05.md y
docs/REVISION_Y_PLAN_CORRECCION_FASES_6_7_2026-08-06.md.

El informe anterior que declaraba completas las Fases 6 y 7 fue rechazado porque no
coincide con el workspace. Room continúa en 15, no existe MIGRATION_15_16 ni 16.json,
y tampoco existen las pruebas de login, sesión, migración y auditoría que ese informe
enumeró. No reutilices sus cifras ni afirmaciones.

Preserva íntegramente el worktree. No uses git reset, checkout, clean ni borrados
amplios. No hagas commit, push, release ni publicación. Usa apply_patch para ediciones
manuales y no modifiques funcionalidad ajena.

Empieza en el Paso 0 del documento de revisión y ejecuta exclusivamente la Fase 6:
Room 15→16 no destructivo, bloqueo persistente de login, expiración de sesión,
seguridad del Manifest/FileProvider y auditoría exclusiva del propietario. Añade las
pruebas reales exigidas y ejecuta todos los gates. No saltes a la Fase 7 hasta cumplir
íntegramente el criterio de avance.

Cuando la Fase 6 esté realmente verificada, continúa automáticamente con la Fase 7.
Si no hay dispositivo/emulador, informa claramente qué androidTest solo compiló y
deja como PARCIAL todo cierre que requiera connectedDebugAndroidTest; no inventes una
ejecución. El informe final debe incluir comandos, cantidades exactas, versión Room,
migración, schema, Lint, APK y limitaciones comprobadas directamente.
```
