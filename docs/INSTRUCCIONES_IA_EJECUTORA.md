# MultiPOS: instrucciones cerradas para la IA ejecutora

Fecha: 2026-08-04  
Proyecto: `H:\multi-pos`

## Finalidad de este documento

Este archivo define el trabajo que una IA sustituta puede realizar mientras el usuario continúa el proyecto fuera de la conversación original. Su función es ejecutar tareas técnicas ya delimitadas, no redefinir MultiPOS.

Este documento tiene prioridad operativa sobre las recomendaciones abiertas de `docs/HANDOFF_ROADMAP.md`. `AGENTS.md` continúa siendo la autoridad general del repositorio.

## Lectura obligatoria

Antes de analizar o modificar el proyecto, la IA debe leer completamente:

1. `H:\multi-pos\AGENTS.md`;
2. `H:\multi-pos\docs\HANDOFF_ROADMAP.md`;
3. este archivo;
4. el `SKILL.md` correspondiente únicamente cuando el punto autorizado indique una skill.

También debe inspeccionar `git status` y el diff de cada archivo que pretenda tocar. El worktree contiene cambios del usuario y no debe limpiarse.

## Diseño actual obligatorio del proyecto

MultiPOS debe conservar la arquitectura y las decisiones técnicas existentes:

- aplicación Android nativa, no multiplataforma;
- Kotlin con Activities, Fragments, layouts XML y ViewBinding;
- AndroidX, Navigation Component y Material Components;
- Room mediante entidades, DAO, `AppDatabase` y migraciones explícitas;
- Coroutines, LiveData/ViewModel según el patrón que ya utiliza cada módulo;
- lógica de negocio fuera de la UI únicamente cuando un punto del plan autorice expresamente su extracción;
- datos separados por `empresaId` y permisos obtenidos de la membresía de la empresa activa;
- importes monetarios persistidos como `Long` en unidades mínimas, nunca `Double`;
- Java/Kotlin target 17, `compileSdk 34`, `targetSdk 34` y `minSdk 24`;
- Room versión de base de datos 8;
- AGP `8.13.2`, Gradle `8.13`, Kotlin `1.9.20` y KSP `1.9.20-1.0.14` mientras el plan no autorice otra cosa;
- pruebas locales con JUnit 4 y pruebas instrumentadas con AndroidX Test/Room Test;
- sin Jetpack Compose y sin framework de inyección de dependencias.

La IA debe adaptar su implementación a este diseño. No debe adaptar el proyecto a sus preferencias, plantillas o arquitectura favorita.

## Android Studio y actualizaciones no autorizadas

Android Studio se usa para sincronizar, compilar, ejecutar pruebas, inspeccionar errores y utilizar un emulador o dispositivo. Sus sugerencias automáticas no forman parte del plan.

La IA debe ignorar, rechazar o cerrar cualquier sugerencia del IDE para:

- ejecutar AGP Upgrade Assistant;
- actualizar Gradle, AGP, Kotlin, KSP, SDK o dependencias;
- migrar a Jetpack Compose;
- migrar a catálogos de versiones o reorganizar los scripts Gradle;
- activar K2, cambiar el JDK o modificar opciones del compilador;
- reemplazar ViewBinding, Fragments, Navigation o Room;
- introducir Hilt, Koin u otra inyección de dependencias;
- convertir código o recursos automáticamente;
- aplicar inspecciones, quick-fixes o reformateos fuera del punto activo.

No debe presionar `Update`, `Migrate`, `Upgrade`, `Convert`, `Fix all` ni una acción equivalente sin una autorización específica y separada del usuario. Una advertencia de obsolescencia debe documentarse, no corregirse por iniciativa propia.

Los archivos `.idea/`, `.gradle/`, `local.properties` y reportes generados no son cambios funcionales del proyecto. No deben editarse deliberadamente, incluirse en staging ni utilizarse para justificar una migración. Si Android Studio los modifica automáticamente, la IA debe informarlo y no intentar restaurarlos mediante comandos destructivos.

Para verificar el proyecto debe preferir el JDK 17 incluido con Android Studio y los comandos Gradle definidos en este documento. Que una versión más nueva esté disponible no significa que esté autorizada.

## Autoridad concedida

La IA solo puede:

- analizar el punto activo;
- implementar exactamente el alcance permitido de ese punto después del `OK` del usuario;
- agregar o ajustar las pruebas estrictamente necesarias;
- corregir errores introducidos por su propia implementación;
- ejecutar compilación, pruebas, lint y verificaciones de solo lectura.

La existencia de una tarea en este documento **no equivale a autorización**. Solo se autoriza cuando el usuario responde `Punto N OK`.

La autorización de un punto no autoriza el siguiente. Al terminar debe detenerse, informar y esperar otro `OK`.

## Prohibiciones absolutas

La IA no tiene autoridad para:

- proponer, crear, eliminar, fusionar o reorganizar pantallas;
- cambiar el flujo de navegación o decidir que otro flujo sería mejor;
- rediseñar layouts, componentes, formularios, menús, diálogos o identidad visual;
- cambiar colores, tipografía, iconos, espaciado o estilo por preferencia propia;
- migrar XML/ViewBinding a Jetpack Compose;
- alterar reglas de negocio, permisos, cálculos o validaciones que no pertenezcan al punto autorizado;
- inventar requisitos, roles, estados, límites, plazos, políticas de seguridad o comportamientos;
- agregar funciones “útiles”, mejoras colaterales o abstracciones no necesarias;
- hacer refactorizaciones generales, renombrados masivos o reformateos amplios;
- cambiar arquitectura, dependencias, SDK, plugins o versiones salvo autorización literal del punto;
- borrar, restaurar, descartar o sobrescribir cambios existentes;
- ejecutar `git reset --hard`, `git checkout --`, limpieza masiva, commit, push o publicación;
- interpretar una recomendación de una skill como autorización para ampliar el alcance.

Si considera que una pantalla, flujo, dependencia o cambio adicional es indispensable, debe detenerse y responder solamente con el bloqueo técnico concreto. No debe diseñar una alternativa ni implementarla.

## Protocolo obligatorio por punto

### Etapa A: análisis sin modificaciones

Cuando el usuario indique qué punto desea revisar, la IA debe:

1. inspeccionar únicamente los archivos relacionados;
2. comprobar el estado real, sin asumir que el roadmap sigue exacto;
3. informar archivos que tocaría y pruebas que ejecutaría;
4. confirmar expresamente que no cambiará pantallas, flujo, diseño ni comportamiento ajeno;
5. esperar `Punto N OK`.

No puede editar archivos durante esta etapa.

### Etapa B: ejecución autorizada

Después de recibir `Punto N OK`, debe:

1. implementar solo el alcance permitido;
2. preservar los cambios preexistentes;
3. añadir pruebas proporcionales al riesgo;
4. ejecutar las validaciones indicadas;
5. revisar el diff final para detectar cambios accidentales.

### Etapa C: cierre y detención

Debe entregar:

- resultado del punto;
- archivos modificados;
- pruebas y compilaciones ejecutadas, con su resultado real;
- limitaciones o pruebas no ejecutadas;
- confirmación de que no cambió pantallas, navegación ni diseño;
- siguiente punto pendiente, sin analizarlo ni iniciarlo.

Después debe detenerse hasta recibir otro mensaje del usuario.

## Skills permitidas

Las skills instaladas están en `H:\multi-pos\.agents\skills`.

### `testing-setup`

Ruta: `H:\multi-pos\.agents\skills\testing-setup\SKILL.md`  
Permitida exclusivamente en el punto 4 y para pruebas directamente relacionadas con otro punto aprobado. No autoriza incorporar frameworks o dependencias adicionales por iniciativa propia.

### `camerax`

Ruta: `H:\multi-pos\.agents\skills\camerax\SKILL.md`  
Permitida exclusivamente en el punto 9. Debe conservar la pantalla y el flujo actual del escáner.

### `ui-ux-pro-max`

Ruta: `H:\multi-pos\.agents\skills\ui-ux-pro-max\SKILL.md`  
Permitida en el punto 10 solo como lista técnica de accesibilidad y validación. No autoriza aplicar estilos, tendencias, paletas, componentes, navegación ni rediseños sugeridos por la skill.

## Estado de partida

Ya están terminados y no deben reimplementarse:

- protección contra doble cobro;
- punto 1: inserción segura de productos;
- punto 2: permisos por empresa;
- punto 3: dinero persistido en unidades mínimas mediante `Long`, Room versión 8 y migración 7→8.

Las pruebas unitarias, compilación de instrumentadas y `assembleDebug` pasaron al cerrar esos puntos. Las pruebas instrumentadas no se ejecutaron porque no había dispositivo o emulador conectado.

## Puntos ejecutables por la IA sustituta

### Punto 4. Pruebas y esquemas Room

Skill obligatoria: `testing-setup`.

Alcance permitido:

1. comprobar la configuración actual de Room, KSP y pruebas;
2. activar `exportSchema = true`;
3. configurar la exportación de esquemas en `app/schemas`;
4. utilizar esos esquemas como assets de `androidTest`;
5. crear o completar pruebas `MigrationTestHelper` de las migraciones existentes 4→5, 5→6, 6→7 y 7→8;
6. comprobar preservación de datos y validación del esquema final;
7. completar pruebas DAO ya previstas para productos, clientes, membresías, ventas y abonos;
8. crear una prueba de atomicidad para venta, detalles, stock y crédito;
9. documentar comandos de prueba si todavía no están documentados.

Límites:

- no cambiar entidades, migraciones ni lógica para “mejorarlas” salvo que una prueba demuestre un defecto real;
- si una migración existente falla, informar primero el defecto y esperar autorización antes de modificar datos o estrategia;
- no agregar Hilt, Robolectric, pruebas de captura ni otro framework nuevo;
- si falta una dependencia pequeña de Room Test ya prevista por AndroidX, informarla durante la etapa A y no agregarla hasta recibir el `OK`.

Validación mínima:

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebugAndroidTest
.\gradlew.bat assembleDebug
```

Ejecutar `.\gradlew.bat connectedDebugAndroidTest` solamente si hay dispositivo o emulador disponible.

### Punto 5. Integridad de clientes y documentos

Alcance permitido:

1. inspeccionar cómo se normaliza y valida actualmente el documento;
2. detectar duplicados existentes por empresa sin modificar datos;
3. impedir nuevos duplicados de documento dentro de una misma empresa;
4. mantener permitido el mismo documento en empresas diferentes;
5. impedir que el límite de crédito quede por debajo del saldo vigente;
6. diferenciar en la UI existente el conflicto de duplicado de un error general, reutilizando el patrón visual actual;
7. agregar migración Room y pruebas solo si el cambio de índice lo requiere.

Límites:

- no crear ni rediseñar formularios;
- no agregar campos;
- no cambiar el formato aceptado del documento sin una regla ya existente;
- si existen duplicados históricos, detenerse e informar sus cantidades e identificadores; no fusionarlos, eliminarlos ni elegir cuál conservar;
- no alterar saldos ni límites existentes durante una migración sin autorización adicional.

### Punto 6. Conservación de historial al eliminar

Alcance permitido:

1. identificar borrados físicos actuales de productos y clientes;
2. comprobar relaciones y claves foráneas con ventas, detalles y abonos;
3. sustituir el borrado físico por activación/desactivación únicamente donde el modelo ya soporte ese estado;
4. asegurar que los registros desactivados no aparezcan como disponibles para operaciones nuevas;
5. conservar su representación en el historial;
6. mantener las confirmaciones existentes y agregar una confirmación mínima solo si actualmente la acción se ejecuta de inmediato;
7. agregar pruebas de conservación histórica.

Límites:

- no crear pantallas de archivo o papelera;
- no cambiar la navegación;
- no introducir un nuevo estado o columna sin detenerse e informar primero que el modelo actual no permite completar el punto;
- no modificar la presentación del historial salvo lo imprescindible para evitar un fallo.

### Punto 8. Separación interna de la operación de venta

Este punto es exclusivamente una refactorización que debe preservar comportamiento.

Alcance permitido:

1. extraer de Fragment/Activity la transacción de registro de venta a una clase interna Repository o UseCase coherente con el proyecto;
2. mantener exactamente las reglas, cálculos, mensajes y orden de operaciones actuales;
3. mantener la atomicidad de venta, detalles, stock y crédito;
4. hacer testeable la operación con dependencias explícitas y fakes pequeños;
5. mover estado al ViewModel solo cuando sea necesario para sobrevivir a recreación y sin cambiar interacción visible.

Límites:

- no cambiar el flujo del POS, carrito, cobro o inventario;
- no crear una arquitectura nueva para toda la aplicación;
- no incorporar inyección de dependencias ni librerías;
- no modificar interfaces públicas ajenas a la operación de venta;
- ante cualquier cambio observable, detenerse antes de implementarlo.

### Punto 9. Robustez del escáner CameraX

Skill obligatoria: `camerax`.

Alcance permitido:

1. corregir el opt-in requerido para `ImageProxy.image` o una sustitución equivalente sin alterar resultados;
2. cerrar correctamente `ImageProxy` y el cliente de ML Kit;
3. manejar fallos de `ProcessCameraProvider` y `bindToLifecycle` con el mecanismo de error existente;
4. evitar fallos cuando no exista cámara trasera o linterna;
5. corregir el estado interno al denegarse el permiso;
6. declarar `uses-feature` de cámara de manera compatible con la entrada manual existente;
7. agregar pruebas unitarias posibles y documentar la matriz manual pendiente.

Límites:

- no crear otra pantalla de escaneo;
- no cambiar encuadre, botones, textos, navegación ni flujo;
- no cambiar formatos admitidos salvo defecto demostrado y autorizado;
- no hacer obligatorio hardware que hoy sea opcional.

### Punto 10. Correcciones técnicas de accesibilidad sin rediseño

Skill permitida: `ui-ux-pro-max`, con las restricciones anteriores.

Alcance permitido:

1. mover textos hardcodeados a `strings.xml` conservando exactamente el texto visible;
2. corregir concatenaciones marcadas por `SetTextI18n` mediante recursos parametrizados sin cambiar su contenido;
3. agregar `contentDescription` únicamente donde falte para controles gráficos interactivos;
4. corregir touch targets menores de 48dp sin reorganizar el layout ni cambiar su apariencia de forma material;
5. corregir errores de contraste únicamente mediante recursos ya existentes; si requiere elegir otro diseño o color, detenerse;
6. asegurar estados vacíos, carga y error solamente donde ya exista el contenedor o patrón correspondiente;
7. corregir incompatibilidades de tema/API que causen error de lint sin rediseñar.

Límites:

- no sugerir ni crear pantallas;
- no reorganizar formularios, navegación o jerarquía visual;
- no cambiar paleta, tipografía, iconos, componentes ni estilo;
- no crear modo oscuro ni cambiar DayNight sin una especificación separada del usuario;
- no “modernizar” la interfaz;
- cualquier corrección que produzca un cambio visual apreciable debe informarse y esperar autorización específica.

### Punto 16A. Saneamiento técnico de build y lint

Este es solo el subconjunto seguro del punto 16.

Alcance permitido:

1. ejecutar `lintDebug` y registrar la línea base real;
2. corregir errores de lint dentro de los puntos ya autorizados;
3. verificar `testDebugUnitTest`, `assembleDebugAndroidTest` y `assembleDebug`;
4. comprobar que no existan secretos versionados;
5. documentar, sin implementar, los pendientes de firma, versionado, R8/ProGuard, CI, APK/AAB y prueba en dispositivo.

Límites:

- no actualizar SDK, Gradle, AGP, Kotlin ni dependencias;
- no crear credenciales, keystores, secretos, releases ni publicaciones;
- no activar minificación ni modificar reglas R8/ProGuard sin un punto separado aprobado;
- no crear CI ni cambiar configuración de producción;
- advertencias no relacionadas se documentan, no se corrigen por iniciativa propia.

## Puntos reservados: no ejecutables por la IA sustituta

Los siguientes puntos permanecen pendientes, pero requieren decisiones funcionales o podrían cambiar pantallas y flujos. La IA sustituta no debe analizarlos en profundidad, diseñarlos, proponer alternativas ni implementarlos aunque haya terminado los puntos anteriores.

### Punto 7. Seguridad de crédito QR

Reservado porque requiere definir vencimiento, rotación, segunda verificación, revocación y política de importes. No inventar valores ni comportamiento.

### Punto 11. Flujo completo de caja

Reservado porque apertura, cierre, arqueo, movimientos y turnos requieren reglas de negocio y UI aprobadas.

### Punto 12. Postventa e inventario avanzado

Reservado porque anulaciones, devoluciones, reposición y movimientos necesitan políticas contables e interacción definidas.

### Punto 13. Crédito e historial financiero

Reservado porque el estado de cuenta, asignación de abonos y medios de pago requieren decisiones funcionales.

### Punto 14. Reportes y exportación

Reservado porque filtros, contenido de reportes y formatos de exportación necesitan especificación visual y de negocio.

### Punto 15. Seguridad general

Reservado porque bloqueo de login, caducidad de sesión, respaldos y auditoría requieren políticas explícitas.

### Punto 16B. Producción y publicación

Reservado: firma, minificación, CI, actualización de dependencias, generación de release y publicación no están autorizadas.

Al terminar los puntos ejecutables, la IA debe indicar que los puntos reservados requieren continuar con el asistente original. No debe convertirlos en trabajo propio.

## Orden obligatorio

La IA puede avanzar únicamente en este orden y siempre con un `OK` independiente:

- [ ] Punto 4 — pruebas y esquemas Room
- [ ] Punto 5 — integridad de clientes y documentos
- [ ] Punto 6 — conservación de historial
- [ ] Punto 8 — separación interna de la operación de venta
- [ ] Punto 9 — robustez CameraX
- [ ] Punto 10 — accesibilidad técnica sin rediseño
- [ ] Punto 16A — build, lint y documentación técnica
- [ ] Regresar al asistente original para revisión integral

La IA debe marcar un punto como completado solo después de validar el resultado. No debe editar esta lista antes de concluir el punto correspondiente.

## Comandos mínimos de verificación

Usar Java 17 de Android Studio cuando el entorno no lo tenga configurado:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebugAndroidTest
.\gradlew.bat assembleDebug
```

Para puntos relacionados con lint:

```powershell
.\gradlew.bat lintDebug
```

Para pruebas instrumentadas, solo cuando exista un dispositivo o emulador:

```powershell
adb devices
.\gradlew.bat connectedDebugAndroidTest
```

Si una prueba no puede ejecutarse, debe informarse como pendiente; compilar una prueba instrumentada no equivale a haberla ejecutado.

## Prompt inicial para la nueva IA

Copiar y enviar exactamente:

> Trabajaremos en `H:\multi-pos`. Lee completamente `AGENTS.md`, `docs/HANDOFF_ROADMAP.md` y `docs/INSTRUCCIONES_IA_EJECUTORA.md`. Este último documento define tu autoridad, el diseño técnico congelado y el alcance operativo. No aceptes sugerencias de Android Studio para actualizar, migrar, convertir o aplicar correcciones generales. No modifiques nada todavía. Revisa el estado del repositorio y realiza únicamente la Etapa A del punto 4. No propongas pantallas, flujos, rediseños, nuevas funciones ni mejoras fuera del alcance. Después informa el alcance exacto y espera mi mensaje `Punto 4 OK`.
