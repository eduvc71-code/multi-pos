# MultiPOS: estado actual y hoja de ruta de continuidad

Fecha del traspaso: 2026-08-04  
Workspace: `H:\multi-pos`

## Propósito

Este documento permite continuar el desarrollo con otra IA sin repetir la auditoría completa. Resume el estado real del proyecto, las decisiones ya implementadas, los riesgos pendientes, las skills disponibles y los criterios de aceptación de cada siguiente paso.

Antes de modificar archivos, leer completamente `H:\multi-pos\AGENTS.md`. Es la autoridad local del proyecto.

Para trabajar con una IA sustituta, también debe leerse `H:\multi-pos\docs\INSTRUCCIONES_IA_EJECUTORA.md`. Ese documento limita el trabajo autorizado, bloquea rediseños y define qué puntos puede ejecutar paso a paso con aprobación del usuario.

## Restricciones obligatorias para la siguiente IA

Este documento es una hoja de ruta, no una autorización para implementar todos los puntos. La IA debe trabajar **un solo punto por vez y únicamente después de que el usuario lo apruebe expresamente**.

Mientras ejecuta un punto autorizado:

- aplicar el menor cambio posible y conservar el comportamiento actual que no forme parte del problema aprobado;
- no cambiar reglas de negocio, cálculos, permisos, validaciones ni flujos existentes salvo que el punto aprobado lo exija de forma explícita;
- no cambiar interfaces públicas, contratos de DAO/Repository/ViewModel, modelos ni esquemas de datos salvo necesidad demostrada del punto aprobado;
- no rediseñar pantallas, layouts, navegación, textos, colores, tipografía, iconos ni interacción sin aprobación específica del usuario;
- no migrar XML a Jetpack Compose ni sustituir ViewBinding;
- no cambiar la arquitectura, agregar dependencias o actualizar versiones por iniciativa propia;
- no aplicar mejoras colaterales, limpiezas masivas ni refactorizaciones no solicitadas;
- preservar compatibilidad con datos existentes y todos los cambios presentes en el worktree;
- antes de editar, inspeccionar el flujo y presentar al usuario el alcance concreto, los archivos previstos y cualquier cambio visible o de comportamiento;
- si una solución requiere ampliar el alcance, detenerse e informar al usuario antes de implementarla.

Los apartados de UI/UX y arquitectura que aparecen más adelante describen posibles trabajos futuros. Primero deben producir un diagnóstico o propuesta. **No autorizan por sí solos un rediseño, una migración arquitectónica ni cambios visibles.**

## Contexto técnico

- Aplicación Android nativa para punto de venta.
- Kotlin, layouts XML y ViewBinding; no usa Jetpack Compose.
- AndroidX, Material Components, Room, Coroutines y Fragments.
- Java/Kotlin 17, `compileSdk 34`, `targetSdk 34`, `minSdk 24`.
- Base Room actual: versión 8.
- CameraX + ML Kit para lectura de códigos.
- El proyecto tiene muchos cambios modificados y archivos sin seguimiento. No asumir que se pueden descartar.
- No ejecutar `git reset --hard`, `git checkout --` ni revertir cambios ajenos.
- Usar `apply_patch` para cambios manuales.

## Advertencia sobre el worktree

El repositorio está deliberadamente sucio. Hay cambios del usuario y trabajo reciente mezclados en archivos tracked/untracked. No limpiar, restaurar ni sobrescribir archivos completos sin inspeccionar primero `git diff` y `git status`.

No se creó commit ni se hizo staging durante esta fase.

## Trabajo ya completado

### Protección contra doble cobro

- El cobro rechaza un segundo envío mientras hay otro en curso.
- El botón muestra `Procesando venta…` y queda deshabilitado.
- Se toma una fotografía inmutable de productos, cantidades y precios antes de la transacción.
- El carrito no puede modificarse durante el cobro.
- Existe `SaleSubmissionGuardTest`.

Archivos relevantes:

- `app/src/main/java/com/multipos/app/ui/pos/PosFragment.kt`
- `app/src/main/java/com/multipos/app/ui/pos/SaleSubmissionGuard.kt`
- `app/src/test/java/com/multipos/app/ui/pos/SaleSubmissionGuardTest.kt`

### Punto 1: inserción segura de productos

- `ProductoDao.insert()` usa `OnConflictStrategy.ABORT`.
- Un código interno o de barras duplicado ya no reemplaza el producto original.
- Guardar queda deshabilitado durante la operación.
- La UI diferencia conflictos de unicidad de errores generales.
- Hay pruebas Room para duplicados, aislamiento por empresa y conservación del stock.

### Punto 2: permisos por empresa

La autorización usa `UsuarioEmpresa.rol` de la empresa activa, no el rol global de `Usuario` ni SharedPreferences.

Matriz actual:

| Rol | Permisos |
|---|---|
| Propietario | Todas las funciones y creación de empresas |
| Administrador | Dashboard, ventas, inventario, historial, clientes y empleados |
| Cajero | Ventas |
| Vendedor | Ventas |

Protecciones adicionales:

- Cada pantalla sensible vuelve a consultar la membresía activa.
- Desactivar una persona afecta solo su membresía en la empresa seleccionada.
- Nadie puede desactivarse a sí mismo.
- Propietarios no pueden desactivarse.
- Administradores no pueden crear ni desactivar otros administradores.
- Reactivar una membresía recupera cuentas desactivadas por la lógica anterior.
- Ya no quedan usos de `UserSessionStore.role`, `UserSessionStore.canManage` ni `Usuario.puedeAdministrar`.

Archivos relevantes:

- `app/src/main/java/com/multipos/app/security/CompanyPermissions.kt`
- `app/src/main/java/com/multipos/app/ui/home/HomeActivity.kt`
- `app/src/main/java/com/multipos/app/data/dao/UsuarioDao.kt`
- `app/src/main/java/com/multipos/app/data/dao/UsuarioEmpresaDao.kt`
- `app/src/test/java/com/multipos/app/security/CompanyPermissionsTest.kt`
- `app/src/androidTest/java/com/multipos/app/data/dao/CompanyMembershipDaoTest.kt`

### Punto 3: modelo monetario preciso

- Todos los importes persistidos usan `Long` en unidades mínimas: `12.34` se guarda como `1234`.
- Ya no queda dinero representado mediante `Double` o `toDoubleOrNull`.
- Se aceptan punto o coma decimal, con máximo dos decimales.
- Impuestos se interpretan como puntos básicos y se redondean con `HALF_UP`.
- Productos, ventas, detalles, clientes, crédito y abonos usan enteros.
- Room subió de versión 7 a 8.
- `MIGRATION_7_8` reconstruye las tablas monetarias, convierte `REAL * 100`, redondea y conserva credenciales de crédito.

Archivos relevantes:

- `app/src/main/java/com/multipos/app/util/Money.kt`
- `app/src/main/java/com/multipos/app/data/AppDatabase.kt`
- `app/src/main/java/com/multipos/app/data/DatabaseProvider.kt`
- `app/src/test/java/com/multipos/app/util/MoneyTest.kt`
- `app/src/androidTest/java/com/multipos/app/data/MoneyMigrationTest.kt`

## Validación conocida

Estos comandos terminaron correctamente después de los puntos 1–3:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebugAndroidTest
.\gradlew.bat assembleDebug
```

También se verificó `git diff --check` sin errores.

No hay dispositivo ni emulador conectado en ADB. Las pruebas de `androidTest` compilan, pero todavía no se ejecutaron sobre SQLite Android. Cuando exista un dispositivo:

```powershell
.\gradlew.bat connectedDebugAndroidTest
```

## Skills instaladas y cuándo utilizarlas

Las skills locales viven en `H:\multi-pos\.agents\skills`. Antes de aplicar una skill, leer su `SKILL.md` completo.

### `testing-setup`

Ruta: `H:\multi-pos\.agents\skills\testing-setup\SKILL.md`

Usarla para:

- punto 4: infraestructura de pruebas y migraciones Room;
- pruebas unitarias, instrumentadas, de UI y end-to-end;
- bases Room en memoria y pruebas sobre SQLite de dispositivo;
- estrategia de cobertura y fakes.

No introducir Hilt, Robolectric, screenshot testing u otras dependencias masivas sin comprobar que sean necesarias para la tarea concreta. El proyecto actualmente usa JUnit 4 y AndroidX Test sin framework de inyección.

### `camerax`

Ruta: `H:\multi-pos\.agents\skills\camerax\SKILL.md`

Usarla para el punto 9:

- ciclo de vida de CameraX;
- cierre de recursos de ML Kit;
- permisos de cámara;
- errores del proveedor y ausencia de cámara trasera;
- pruebas con dispositivos, orientaciones y hardware diferente.

### `ui-ux-pro-max`

Ruta: `H:\multi-pos\.agents\skills\ui-ux-pro-max\SKILL.md`

Usarla para el punto 10 y revisiones visuales:

- accesibilidad, contraste y touch targets;
- estados de carga, error y vacío;
- formularios y mensajes accionables;
- navegación, responsive phone/tablet y modo oscuro;
- mantener XML/ViewBinding; no migrar a Compose.

Referencias importantes de esta skill:

- `references/pro-rules.md`
- `references/quick-reference.md`, especialmente secciones 1–3.

## Siguientes pasos pendientes

### 4. Infraestructura completa de pruebas y migraciones Room

Objetivo: convertir las pruebas que hoy solo compilan en una red de seguridad reproducible.

Trabajo recomendado:

1. Activar `exportSchema = true` en `AppDatabase`.
2. Configurar KSP para exportar esquemas en `app/schemas`.
3. Incluir los esquemas como assets de `androidTest`.
4. Crear pruebas con `MigrationTestHelper` para rutas soportadas hasta versión 8.
5. Validar especialmente 4→5, 5→6, 6→7 y 7→8.
6. Ejecutar pruebas de DAO para productos, clientes, membresías, ventas y abonos.
7. Crear una prueba de transacción que compruebe venta + detalles + descuento de stock + crédito atómicos.
8. Documentar los comandos de pruebas.

Criterio de aceptación:

- los esquemas quedan versionados;
- las migraciones preservan datos y Room valida el esquema final;
- un fallo de stock o crédito revierte toda la venta;
- `testDebugUnitTest`, `assembleDebugAndroidTest` y `assembleDebug` pasan;
- ejecutar `connectedDebugAndroidTest` cuando haya dispositivo.

### 5. Integridad de clientes y documentos

Objetivo: evitar identidades duplicadas y estados de crédito incoherentes.

Trabajo recomendado:

- agregar índice único `(empresaId, documento)`;
- crear migración Room con estrategia explícita para duplicados existentes;
- impedir que el límite quede por debajo del saldo actual;
- validar documento normalizado, nombre, teléfono y límites;
- distinguir errores de duplicado de otros errores de base de datos;
- agregar pruebas por empresa.

### 6. Borrado seguro e historial

Objetivo: preservar ventas históricas.

Trabajo recomendado:

- reemplazar borrado físico de productos y clientes por archivado/desactivación;
- agregar confirmación para acciones destructivas;
- impedir que registros históricos queden huérfanos;
- revisar claves foráneas de ventas, detalles y abonos;
- definir qué datos se conservan cuando una empresa o usuario se desactiva.

### 7. Seguridad de crédito QR

Objetivo: evitar que una fotografía permanente del QR autorice compras indefinidamente.

Trabajo recomendado:

- vencimiento o rotación de credenciales;
- PIN o segunda verificación para importes sensibles;
- registrar uso, usuario, venta, fecha y resultado;
- definir revocación inmediata y recuperación;
- nunca incluir datos personales directos en el payload.

### 8. Arquitectura de ventas e inventario

Objetivo: sacar lógica de negocio de Activities/Fragments.

Trabajo recomendado:

- crear Repository/UseCase para registrar ventas;
- utilizar ViewModels reales para POS e inventario;
- conservar carrito, filtros y formulario ante rotación;
- exponer estados `loading/success/error`;
- facilitar fakes y pruebas unitarias.

No migrar a Compose.

### 9. CameraX y escáner

Usar la skill `camerax`.

Trabajo recomendado:

- agregar opt-in correcto para `ImageProxy.image` o usar la API recomendada por la skill;
- cerrar el cliente de ML Kit;
- manejar errores de `ProcessCameraProvider` y `bindToLifecycle`;
- manejar ausencia de cámara trasera y linterna;
- corregir el flujo cuando el permiso se rechaza y la entrada manual está deshabilitada;
- declarar `uses-feature` de cámara correctamente;
- probar rotación y distintos formatos.

### 10. UI/UX y accesibilidad

Usar la skill `ui-ux-pro-max`.

Trabajo recomendado:

- mover textos hardcodeados a `strings.xml`;
- reemplazar formularios improvisados por campos etiquetados con errores locales;
- agregar estados de carga, error y vacío;
- comprobar contraste del color empresarial antes de usar texto blanco;
- crear recursos nocturnos reales o desactivar DayNight de forma consciente;
- asegurar touch targets de al menos 48dp;
- marcar sección de navegación activa;
- probar texto grande, teléfono pequeño, tablet y landscape.

### 11. Flujo completo de caja

- apertura y cierre de caja;
- fondo inicial, ingresos, retiros y arqueo;
- monto recibido y cambio;
- diferencias esperadas/reales;
- responsable, fecha y estado de turno.

### 12. Postventa e inventario avanzado

- detalle y reimpresión de venta;
- anulación y devolución;
- reposición atómica de stock;
- compras, entradas, salidas y ajustes;
- historial de movimientos y motivo.

### 13. Crédito e historial financiero

- estado de cuenta por cliente;
- historial de ventas a crédito y abonos;
- saldo anterior y nuevo;
- medio de pago y usuario que registró el abono;
- eliminar el uso provisional de `idVenta = 0` donde no corresponda.

### 14. Reportes y exportación

- filtros por fecha, vendedor, producto y medio de pago;
- ventas, utilidad, impuestos, stock y crédito;
- exportación PDF/CSV;
- totales consistentes con el modelo en centavos.

### 15. Seguridad general

- límite progresivo de intentos de login;
- caducidad o revalidación de sesión;
- revisar `allowBackup` y reglas de respaldo;
- auditoría de cambios administrativos;
- no almacenar secretos ni credenciales sensibles en texto plano.

### 16. Preparación para producción

El último `lintDebug` conocido falló con 3 errores y 145 advertencias. Errores principales:

1. `android:windowLightNavigationBar` requiere API 27, pero `minSdk` es 24.
2. `ImageProxy.image` requiere opt-in experimental de CameraX.
3. El permiso de cámara no tiene el `uses-feature` correspondiente.

Advertencias relevantes:

- 67 textos hardcodeados;
- 21 usos `SetTextI18n`;
- falta icono de aplicación;
- ML Kit 17.2.0 advierte sobre alineación de páginas de 16 KB;
- `targetSdk 34` y varias dependencias requieren revisión;
- `release` tiene minificación desactivada;
- `app/proguard-rules.pro` no existía durante la auditoría;
- no hay CI configurado.

Criterio de aceptación:

- `lintDebug`, pruebas y build pasan;
- icono, firma, versionado, ProGuard/R8 y pipeline CI quedan definidos;
- probar APK/AAB release en dispositivo real.

## Orden recomendado

Continuar estrictamente desde el punto 4 y no mezclar varios puntos en un mismo cambio:

1. punto 4: pruebas y migraciones Room;
2. punto 5: clientes/documentos;
3. punto 6: borrado e historial;
4. punto 7: seguridad QR;
5. punto 8: arquitectura;
6. punto 9: CameraX;
7. punto 10: UI/UX;
8. puntos 11–16 en orden.

## Formato esperado al terminar cada punto

La IA que continúe debe informar:

- resultado primero;
- archivos modificados;
- pruebas ejecutadas y resultado;
- limitaciones reales, especialmente ausencia de dispositivo;
- lista numerada actualizada con estados `completado`, `en curso` o `pendiente`.

## Primera instrucción sugerida para la siguiente IA

> Lee completamente `AGENTS.md` y `docs/HANDOFF_ROADMAP.md`. Continúa con el punto 4 solamente. Preserva el worktree existente, usa la skill `testing-setup`, configura exportación de esquemas Room y pruebas de migración sin cambiar funcionalidad ajena. Ejecuta `testDebugUnitTest`, `assembleDebugAndroidTest` y `assembleDebug`; si no hay dispositivo, indícalo claramente.
