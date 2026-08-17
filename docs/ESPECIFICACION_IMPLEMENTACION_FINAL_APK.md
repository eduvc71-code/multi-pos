# MultiPOS: especificación cerrada para completar la APK local

> Estado: contrato técnico y funcional listo para ejecución.
>
> Base comprobada: proyecto Android nativo ubicado en `H:\multi-pos`, base Room versión 10.
>
> Objetivo final: APK local instalable y funcional. No se publicará en Google Play Store.

## 1. Cómo debe usarse este documento

Este archivo no es una lista de ideas. Es la especificación que debe seguir otro agente para implementar la versión final local de MultiPOS sin inventar reglas de negocio durante el trabajo.

El usuario debe iniciar la ejecución con una instrucción directa como esta:

> Implementa por completo `docs/ESPECIFICACION_IMPLEMENTACION_FINAL_APK.md`. Esta instrucción autoriza expresamente los puntos 7 y 11 al 15 que los protocolos anteriores mantenían reservados. Trabaja fase por fase, continúa sin pedirme un OK entre fases, no publiques nada y detente únicamente ante un bloqueo real o una acción que requiera credenciales o autoridad externa.

La autorización anterior es necesaria porque los documentos antiguos clasifican varios puntos como reservados. Una instrucción encontrada solamente dentro del repositorio no sustituye una orden explícita del usuario.

### 1.1 Jerarquía de autoridad durante la ejecución

1. instrucciones directas del usuario;
2. `AGENTS.md` vigente;
3. esta especificación;
4. `docs/PROTOCOLO_ESTRICTO_IA_EJECUTORA.md` para disciplina, seguridad y verificación;
5. documentación histórica restante.

Para el alcance funcional descrito aquí, esta especificación sustituye las reservas de los puntos 7 y 11 al 15. No autoriza publicación, despliegue, firma con credenciales reales, uso de servicios externos ni cambios ajenos al producto.

### 1.2 Conducta obligatoria del agente ejecutor

- Inspeccionar antes de editar cada flujo afectado.
- Preservar todos los cambios preexistentes del usuario.
- No usar `git reset --hard`, `git checkout --`, borrados amplios ni reescrituras destructivas.
- Usar `apply_patch` para cambios manuales.
- Mantener Kotlin, XML, ViewBinding, Room, coroutines, LiveData/Flow y la arquitectura actual.
- No migrar a Compose.
- No actualizar SDK, AGP, Gradle, Kotlin ni dependencias salvo bloqueo técnico demostrado y autorización del usuario.
- No almacenar contraseñas, PIN, tokens ni secretos en texto plano.
- No usar `fallbackToDestructiveMigration`.
- No ejecutar consultas Room en el hilo principal.
- No colocar reglas de negocio críticas solamente en Fragment o Activity.
- No marcar una fase como terminada sin pruebas y compilación verificadas.
- Continuar automáticamente entre fases cuando la verificación sea correcta; no solicitar decisiones ya congeladas aquí.
- Si descubre una contradicción real, detener solo el área afectada, documentar evidencia exacta y continuar con trabajo independiente que siga siendo seguro.

## 2. Definición exacta del producto final

MultiPOS será una aplicación Android local, monodispositivo y multiempresa para registrar y administrar:

- autenticación y sesiones de usuarios;
- empresas y membresías con roles;
- catálogo e inventario;
- clientes y crédito;
- venta de contado, tarjeta, transferencia y crédito;
- apertura, operación y cierre de caja;
- anulación y devolución de ventas;
- movimientos manuales de inventario;
- abonos y estado de cuenta del cliente;
- reportes locales y exportación CSV/PDF;
- auditoría local de operaciones sensibles.

### 2.1 Fuera de alcance, sin excepción

- Google Play Store, Play Console, AAB y publicación;
- backend, nube, sincronización entre dispositivos o acceso web;
- facturación fiscal/electrónica y normativa tributaria de un país;
- procesamiento real de tarjetas o transferencias;
- integración con bancos, QR bancario o pasarelas de pago;
- impresoras POS, gavetas, balanzas o hardware especializado;
- compras con proveedores, órdenes de compra y cuentas por pagar;
- nómina, turnos laborales o comisiones;
- múltiples monedas o conversión cambiaria;
- modo oscuro o rediseño visual integral;
- copia de seguridad automática de Android o respaldo manual de la base;
- telemetría, analítica remota o notificaciones push;
- CI/CD obligatorio;
- firma release con una clave real del usuario.

La ausencia de respaldo es una decisión cerrada de este alcance: `android:allowBackup` debe quedar en `false`, los datos permanecen únicamente en el dispositivo y la UI debe advertirlo en una sección informativa. Diseñar un respaldo portátil y cifrado será un proyecto separado.

### 2.2 Criterio de “APK terminada”

La implementación se considera completa solo cuando se cumplen simultáneamente estas condiciones:

1. todos los flujos de la sección 2 funcionan con persistencia real;
2. las operaciones compuestas son atómicas;
3. los permisos se validan en UI y repositorio/caso de uso;
4. existen migraciones consecutivas desde Room 10 hasta la versión final;
5. las migraciones conservan y hacen consultables los datos existentes;
6. los estados vacío, carga, error y éxito son visibles y accionables;
7. `testDebugUnitTest`, `assembleDebugAndroidTest`, `lintDebug` y `assembleDebug` terminan correctamente;
8. el APK final existe en `app/build/outputs/apk/debug/app-debug.apk`;
9. hay una prueba manual documentada en emulador o dispositivo para el recorrido crítico;
10. no quedan `TODO`, `FIXME`, datos simulados o métodos placeholder dentro de los flujos entregados.

No es necesario crear un release firmado. El artefacto contractual es el APK debug instalable.

## 3. Estado base que debe respetarse

Al redactar esta especificación se comprobó lo siguiente:

- paquete: `com.multipos.app`;
- `compileSdk 34`, `targetSdk 34`, `minSdk 24`;
- Java/Kotlin 17;
- Room versión 10 con esquemas exportados;
- importes monetarios representados en unidades menores con `Long`;
- entidades actuales: `Empresa`, `Usuario`, `UsuarioEmpresa`, `Producto`, `Cliente`, `CredencialCliente`, `Venta`, `DetalleVenta` y `Abono`;
- una venta y su descuento de stock ya pasan por `SaleRepository` y `withTransaction`;
- el crédito actual aumenta `Cliente.creditoActual` dentro de la transacción de venta;
- el QR contiene un identificador opaco, no datos personales;
- existen roles `PROPIETARIO`, `ADMINISTRADOR`, `CAJERO` y `VENDEDOR`;
- existen pantallas de dashboard, POS, inventario, historial, clientes y empleados;
- ya existen pruebas unitarias, instrumentadas y de migración que no deben eliminarse;
- `PosViewModel` contiene placeholders y no debe considerarse una implementación funcional;
- `android:allowBackup` está en `true` y debe cambiarse en la fase de seguridad;
- `Venta` no está vinculada a una sesión de caja;
- `Abono.idVenta` usa valores provisionales y debe convertirse en nullable;
- no existen aún entidades de caja, devolución, movimiento de inventario, libro de crédito ni auditoría.

Antes de editar, el agente debe volver a verificar este estado porque el usuario puede haber cambiado el repositorio después de la fecha de este documento.

## 4. Reglas transversales congeladas

### 4.1 Dinero y porcentajes

- Todo importe persistido usa `Long` en unidades menores.
- Está prohibido usar `Float` o `Double` para precios, saldos, impuestos, descuentos o totales.
- El formato visible usa `Money.format`.
- El parseo usa `Money.parseMinorUnits`.
- Los porcentajes se representan en puntos base: 10000 equivale a 100,00 %.
- Cada suma y multiplicación monetaria debe detectar overflow mediante `Math.addExact`, `Math.multiplyExact` o una función segura equivalente.
- No se persisten importes negativos salvo `MovimientoCredito.importeFirmado` y cantidades firmadas explícitamente documentadas.

### 4.2 Tiempo

- Todas las fechas persistidas son epoch milliseconds en `Long`.
- La lógica de “mismo día” usa la zona horaria local del dispositivo.
- Los tests que dependan del tiempo reciben un `Clock` o proveedor de tiempo inyectable; no deben depender directamente del reloj real.

### 4.3 Multiempresa

- Toda entidad operativa contiene `empresaId`.
- Toda consulta, update y delete operativo filtra también por `empresaId`.
- Nunca se acepta `empresaId` proveniente de QR, formulario o argumento como autoridad suficiente: se compara con `ActiveCompanyStore`.
- Una operación sobre datos de otra empresa debe fallar sin revelar información de esos datos.

### 4.4 Historial e inmutabilidad

- Productos, clientes, usuarios y empresas referenciados por historial no se borran físicamente desde la UI; se archivan/desactivan.
- Ventas, detalles, sesiones cerradas, movimientos de caja, devoluciones, movimientos de crédito y auditoría son inmutables.
- Una corrección se representa con un evento inverso, nunca reescribiendo el evento original.
- Los snapshots de nombre, costo o precio necesarios para reportes históricos se guardan al registrar la operación.

### 4.5 Transacciones atómicas obligatorias

Una sola `withTransaction` debe cubrir, según corresponda:

- venta + detalles + descuento de stock + movimientos de inventario + crédito + movimiento de caja + auditoría;
- apertura/cierre de caja + movimiento inicial/final + auditoría;
- devolución/anulación + reposición de stock + reversión financiera + caja + crédito + auditoría;
- abono + actualización del saldo + libro de crédito + caja + auditoría;
- ajuste de inventario + cambio de stock + movimiento + auditoría;
- reemplazo de credencial + revocación de la anterior + emisión de la nueva + auditoría.

Si cualquier paso falla, no debe persistirse ningún cambio parcial.

## 5. Matriz de permisos definitiva

Agregar los permisos faltantes a `CompanyPermission` y mantener una única fuente de verdad en `CompanyPermissions`.

| Acción | Propietario | Administrador | Cajero | Vendedor |
|---|---:|---:|---:|---:|
| Vender | Sí | Sí | Sí | Sí |
| Ver dashboard global | Sí | Sí | No | No |
| Administrar inventario | Sí | Sí | No | No |
| Ver historial global y detalle | Sí | Sí | No | No |
| Administrar clientes/crédito | Sí | Sí | No | No |
| Administrar empleados | Sí | Sí | No | No |
| Crear empresa | Sí | No | No | No |
| Abrir y cerrar caja | Sí | Sí | Sí | No |
| Registrar ingreso/retiro manual | Sí | Sí | No | No |
| Anular venta | Sí | Sí | No | No |
| Registrar devolución | Sí | Sí | No | No |
| Ver reportes/exportar | Sí | Sí | No | No |
| Ver auditoría | Sí | No | No | No |

Reglas adicionales:

- Un cajero solo puede cerrar la sesión que abrió él mismo. Propietario o administrador puede cerrar cualquier sesión abierta.
- Ocultar un botón no es control de autorización. Cada repositorio valida nuevamente membresía activa y permiso antes de mutar datos.
- Si un permiso cambia mientras una pantalla está abierta, la siguiente acción vuelve a consultar la membresía y falla de forma segura.
- Un vendedor puede vender usando una caja que ya fue abierta por un usuario autorizado, pero no puede abrirla ni cerrarla.

## 6. Navegación y pantallas finales

Se conserva `HomeActivity` como contenedor de fragments. No introducir Navigation Component si la aplicación actual no lo utiliza realmente.

La barra principal debe contener, según permisos:

1. Resumen;
2. Vender;
3. Caja;
4. Inventario;
5. Historial;
6. Clientes;
7. Reportes;
8. Empleados.

En pantallas pequeñas la navegación debe ser desplazable horizontalmente o usar el patrón existente sin comprimir controles por debajo de 48dp. No se autoriza un rediseño general.

Fragments nuevos obligatorios:

- `CashFragment`: estado, apertura, movimientos, arqueo y cierre;
- `ReportsFragment`: filtros, indicadores y exportaciones;
- `SaleDetailFragment` o diálogo de pantalla completa: detalle, anulación y devolución;
- `ClientStatementFragment` o diálogo de pantalla completa: movimientos y abonos;
- `InventoryMovementsFragment` o sección integrada en inventario: entradas, salidas y ajustes;
- `AuditFragment`: solo propietario.

La elección entre fragment y diálogo solo puede seguir un patrón ya existente y no puede cambiar reglas ni omitir estados. Para recorridos con lista + formulario + confirmación se prefiere fragment.

Todas las cadenas visibles nuevas van en `strings.xml`; colores en `colors.xml`; dimensiones repetidas en recursos. Todo control interactivo tendrá objetivo mínimo de 48dp, etiqueta accesible y orden de foco coherente.

### 6.1 Protección estricta de las pantallas ya validadas

Las pantallas y componentes existentes se consideran **visualmente validados y congelados**. Esta especificación autoriza completar su comportamiento, pero no rediseñarlos.

Archivos visuales protegidos:

- `activity_home.xml`;
- `activity_login.xml`;
- `activity_scanner.xml`;
- `activity_setup.xml`;
- `fragment_dashboard.xml`;
- `fragment_pos.xml`;
- `fragment_inventory.xml`;
- `fragment_history.xml`;
- `fragment_clients.xml`;
- `fragment_employees.xml`;
- `item_cart.xml`;
- `item_client.xml`;
- `item_employee.xml`;
- `item_product_card.xml`;
- `item_sale.xml`;
- estilos, temas, colores, tipografías e iconografía actualmente usados por esos archivos.

En esos recursos está prohibido, salvo autorización posterior y específica del usuario:

- reorganizar la jerarquía, columnas, filas, tarjetas o formularios;
- cambiar paleta, tipografía, iconos, fondos, bordes, elevación o forma de componentes;
- cambiar tamaños, márgenes, paddings o alineaciones con finalidad estética;
- sustituir componentes por otros “más modernos”;
- eliminar, renombrar visualmente o mover acciones existentes;
- cambiar el orden de navegación existente;
- modificar textos visibles ya validados, excepto mensajes que sean objetivamente incorrectos para la nueva regla de negocio;
- aplicar un rediseño responsive, modo oscuro o identidad visual nueva.

Cambios permitidos dentro de una pantalla protegida, únicamente cuando sean indispensables para conectar la funcionalidad definida:

- listeners, ViewBinding, carga de datos y estados internos sin cambio visual;
- habilitar, deshabilitar o mostrar controles cuyo patrón ya exista;
- abrir una pantalla nueva al tocar una fila, tarjeta o acción que ya exista;
- correcciones técnicas invisibles de accesibilidad, lifecycle o recursos;
- agregar un acceso nuevo solo cuando no exista ningún punto de entrada reutilizable, copiando exactamente el componente, estilo, dimensiones y comportamiento de los accesos vecinos.

La adición de “Caja” y “Reportes” en la navegación es la única ampliación visual principal preautorizada en `activity_home.xml`. Debe conservar intactos los botones actuales, su orden relativo, color, tipografía y tamaño; los nuevos accesos se insertan siguiendo exactamente el mismo patrón. “Auditoría” debe abrirse desde una acción secundaria dentro de una pantalla nueva o menú ya disponible, no agregando otro botón principal.

Las funciones nuevas de una pantalla protegida usarán preferentemente navegación o diálogos nuevos en vez de ampliar o reorganizar el layout validado. Ejemplos obligatorios:

- tocar una venta existente abre su detalle; no rediseñar `fragment_history.xml` ni `item_sale.xml`;
- tocar un cliente existente abre estado de cuenta/acciones; no rediseñar `fragment_clients.xml` ni `item_client.xml`;
- los movimientos avanzados se abren desde un acceso del patrón existente; no convertir `fragment_inventory.xml` en otra interfaz;
- el PIN de crédito se solicita en un diálogo nuevo; no reorganizar el POS;
- la ausencia de caja se comunica mediante diálogo o estado ya existente; no rediseñar el carrito.

Antes de modificar cualquier archivo visual protegido, el agente debe:

1. explicar qué requisito funcional obliga a tocarlo;
2. comprobar que no puede resolverse mediante lógica, navegación o un recurso nuevo;
3. limitar el diff al cambio indispensable;
4. comparar el resultado antes/después en la misma resolución;
5. detenerse y solicitar aprobación si el cambio produce una diferencia visual material distinta de las dos ampliaciones de navegación preautorizadas.

Las pantallas completamente nuevas no están congeladas, pero deben reutilizar los recursos, componentes, espaciado, colores y jerarquía visual existentes. No se autoriza crear una segunda identidad visual.

## 7. Modelo de datos final y migraciones

La versión final de Room será **15**. Deben existir migraciones explícitas `10→11`, `11→12`, `12→13`, `13→14` y `14→15`. Cada versión exportará su JSON en `app/schemas/com.multipos.app.data.AppDatabase/`.

No combinar saltos ni reemplazar migraciones consecutivas por migración destructiva.

### 7.1 Versión 11: credencial segura y auditoría

Modificar `CredencialCliente` con:

- `pinHash: String?`;
- `pinSalt: String?`;
- `fechaVencimiento: Long?`;
- `intentosFallidos: Int = 0`;
- `bloqueadaHasta: Long?`;
- `ultimoUso: Long?`.

Crear `Auditoria`:

- `id: Long`, autogenerado;
- `empresaId: String`;
- `usuarioId: Int?`;
- `accion: String`;
- `entidad: String`;
- `entidadId: String?`;
- `detalle: String`, sin contraseñas, PIN, tokens completos ni documentos completos;
- `fecha: Long`.

Índices: `(empresaId, fecha)`, `(empresaId, accion)` y `usuarioId`.

Migración de credenciales existentes:

- conservar filas e historial;
- marcar toda credencial activa sin PIN como `REEMPLAZADA`;
- asignar `fechaRevocacion` al momento de migración;
- no inventar PIN;
- la UI indicará que se debe emitir una credencial nueva.

### 7.2 Versión 12: caja y vínculo con ventas

Crear `CajaSesion`:

- `id: Long`, autogenerado;
- `empresaId: String`;
- `abiertaPorUsuarioId: Int`;
- `cerradaPorUsuarioId: Int?`;
- `fechaApertura: Long`;
- `fechaCierre: Long?`;
- `montoApertura: Long`;
- `montoEsperadoCierre: Long?`;
- `montoContadoCierre: Long?`;
- `diferenciaCierre: Long?`;
- `estado: ABIERTA | CERRADA`;
- `notaCierre: String`.

Crear `MovimientoCaja`:

- `id: Long`, autogenerado;
- `cajaSesionId: Long`;
- `empresaId: String`;
- `usuarioId: Int`;
- `tipo: APERTURA | INGRESO_VENTA | INGRESO_MANUAL | EGRESO_MANUAL | EGRESO_DEVOLUCION | REVERSO_ANULACION | INGRESO_ABONO`;
- `monto: Long`, siempre mayor que cero;
- `ventaId: Int?`;
- `abonoId: Long?`;
- `devolucionId: Long?` inicialmente nullable y enlazable después de v13;
- `concepto: String`;
- `fecha: Long`.

Agregar a `Venta`:

- `cajaSesionId: Long?`;
- `anuladaPorUsuarioId: Int?`;
- `fechaAnulacion: Long?`;
- `motivoAnulacion: String?`.

Índices mínimos:

- `CajaSesion(empresaId, estado)`;
- índice único parcial de una sola sesión `ABIERTA` por empresa, creado con SQL;
- `MovimientoCaja(cajaSesionId, fecha)`;
- `MovimientoCaja(empresaId, fecha)`;
- `Venta(cajaSesionId)`.

Las ventas históricas migradas conservan `cajaSesionId = null` y siguen visibles. No crear sesiones ficticias.

### 7.3 Versión 13: inventario y postventa

Agregar a `DetalleVenta`:

- `costoUnitario: Long`;
- `nombreProductoSnapshot: String`.

Para detalles existentes, poblar costo con el costo actual del producto si aún existe y cero si no existe; poblar nombre con el nombre actual o `Producto #<id>`. Documentar que el costo histórico previo a v13 es una aproximación migrada.

Crear `MovimientoInventario`:

- `id: Long`, autogenerado;
- `empresaId: String`;
- `productoId: Int`;
- `usuarioId: Int`;
- `tipo: VENTA | ANULACION | DEVOLUCION | ENTRADA_MANUAL | SALIDA_MANUAL | AJUSTE`;
- `cantidadFirmada: Int`, distinta de cero;
- `stockAnterior: Int`;
- `stockPosterior: Int`;
- `ventaId: Int?`;
- `devolucionId: Long?`;
- `motivo: String`;
- `fecha: Long`.

Crear `Devolucion`:

- `id: Long`, autogenerado;
- `empresaId: String`;
- `ventaId: Int`;
- `usuarioId: Int`;
- `cajaSesionId: Long?`;
- `monto: Long`;
- `medioReembolso: EFECTIVO | TARJETA | TRANSFERENCIA | CREDITO`;
- `estadoReembolso: COMPLETADO | CONFIRMADO_EXTERNAMENTE`;
- `motivo: String`;
- `fecha: Long`.

Crear `DetalleDevolucion`:

- `id: Long`, autogenerado;
- `devolucionId: Long`;
- `detalleVentaId: Int`;
- `productoId: Int`;
- `cantidad: Int`;
- `precioUnitario: Long`;
- `subtotal: Long`.

Índices en claves foráneas y en `(empresaId, fecha)` para ambos movimientos y devoluciones.

### 7.4 Versión 14: libro de crédito y abonos reales

Reconstruir `Abono` para que tenga:

- `id: Long`, autogenerado;
- `empresaId: String`;
- `idCliente: Int` obligatorio;
- `idVenta: Int?` nullable;
- `cajaSesionId: Long?`;
- `usuarioId: Int`;
- `monto: Long` mayor que cero;
- `medioPago: EFECTIVO | TARJETA | TRANSFERENCIA`;
- `fecha: Long`;
- `nota: String`.

Crear `MovimientoCredito`:

- `id: Long`, autogenerado;
- `empresaId: String`;
- `clienteId: Int`;
- `usuarioId: Int`;
- `tipo: VENTA_CREDITO | ABONO | DEVOLUCION | ANULACION`;
- `importeFirmado: Long`: positivo aumenta deuda, negativo la reduce;
- `saldoPosterior: Long`, nunca negativo;
- `ventaId: Int?`;
- `abonoId: Long?`;
- `devolucionId: Long?`;
- `fecha: Long`;
- `nota: String`.

Índices: `(empresaId, clienteId, fecha)`, `ventaId`, `abonoId`, `devolucionId`.

Migración de datos existentes:

- conservar todos los abonos;
- convertir `idVenta = 0` en `NULL`;
- derivar `idCliente` desde la venta cuando sea cero y la venta exista;
- si no se puede resolver cliente, conservar el abono en una tabla temporal de incidencias o abortar la migración con diagnóstico; jamás asociarlo a otro cliente;
- `usuarioId` legado se asigna al propietario activo más antiguo de la empresa y se registra esta aproximación en auditoría;
- `medioPago` legado será `EFECTIVO`;
- reconstruir el libro cronológicamente con ventas a crédito y abonos existentes;
- comprobar al terminar que el saldo final por cliente coincide con `Cliente.creditoActual`; si no coincide, agregar un movimiento migratorio explícito no está permitido: la migración debe fallar y el agente debe investigar la inconsistencia.

### 7.5 Versión 15: seguridad de login

Agregar a `Usuario`:

- `intentosLoginFallidos: Int = 0`;
- `bloqueadoHasta: Long?`;
- `ultimoLogin: Long?`.

No modificar ni debilitar `passwordHash`, `passwordSalt` o la migración de contraseña legacy existente.

### 7.6 Claves foráneas

Las entidades nuevas deben usar claves foráneas donde no destruyan historial. Para datos históricos, `onDelete` será `RESTRICT` o `NO_ACTION`; no usar `CASCADE` desde producto, cliente, usuario o venta hacia movimientos contables. Los DAOs de UI no ofrecerán borrado físico de entidades históricas.

Cada entidad nueva debe registrarse explícitamente en `@Database`, tener su DAO abstracto en `AppDatabase` y contar con consulta aislada por `empresaId`. Una columna que apunte a una tabla creada en una versión posterior puede existir inicialmente como nullable sin foreign key; la restricción se incorpora al reconstruir la tabla en la migración posterior.

## 8. Punto 7: seguridad de la credencial QR

### 8.1 Política cerrada

- El QR identifica una credencial; no autoriza por sí solo una compra.
- El payload sigue siendo opaco: `multipos://credito/v1/<empresaId>/<credentialId>`.
- Está prohibido incluir nombre, documento, límite, saldo o PIN.
- Cada credencial requiere un PIN de exactamente cuatro dígitos.
- El PIN se almacena con el mismo esquema PBKDF2 seguro usado para contraseñas, con salt independiente.
- La credencial vence 365 días después de emitirse.
- Solo puede existir una credencial activa por cliente y empresa.
- Reemitir una credencial marca la anterior como `REEMPLAZADA` dentro de la misma transacción.
- Revocar la credencial marca `REVOCADA` y no afecta el historial de ventas.
- Cinco PIN incorrectos consecutivos bloquean la credencial por 15 minutos.
- Un PIN correcto reinicia contador/bloqueo y actualiza `ultimoUso`.
- La compra a crédito siempre exige escaneo QR, confirmación visual del cliente y PIN.
- No existe bypass por selección manual del cliente.

### 8.2 Emisión

En Clientes, “Generar/Reemplazar credencial” debe:

1. comprobar cliente activo y crédito en estado `ACTIVO`;
2. pedir PIN y confirmación;
3. validar exactamente cuatro dígitos y coincidencia;
4. explicar que el QR vence en 365 días y será invalidado al reemplazarlo;
5. crear credencial y revocar/reemplazar la anterior atómicamente;
6. generar el bitmap solamente después del commit;
7. permitir compartir el QR mediante `FileProvider`;
8. limpiar `CharArray` del PIN después de usarlo.

### 8.3 Uso en venta

El repositorio recibe `credentialId` y PIN, vuelve a validar empresa, cliente, estado, expiración, bloqueo y hash dentro de la transacción. La UI no es autoridad.

Mensajes diferenciados y no ambiguos:

- formato QR inválido;
- credencial de otra empresa;
- credencial revocada/reemplazada;
- credencial vencida;
- credencial bloqueada, indicando tiempo restante redondeado;
- PIN incorrecto sin indicar detalles del hash;
- crédito inactivo;
- límite insuficiente.

## 9. Punto 11: flujo completo de caja

### 9.1 Reglas de apertura

- Solo propietario, administrador o cajero puede abrir caja.
- Solo puede existir una caja abierta por empresa.
- `montoApertura` admite cero y nunca negativo.
- Una segunda apertura concurrente debe fallar por transacción e índice único.
- La apertura crea sesión, movimiento `APERTURA` y auditoría.
- El movimiento de apertura se incluye una sola vez en el efectivo esperado.

### 9.2 Regla para vender

Toda venta, incluso tarjeta, transferencia o crédito, requiere una sesión de caja abierta y queda vinculada a ella. Si no hay caja abierta, POS muestra una acción clara:

- usuarios autorizados: “Abrir caja”;
- vendedor: “Solicita a un responsable que abra la caja”.

Efecto financiero por medio:

- `EFECTIVO`: crea `INGRESO_VENTA` por el total;
- `TARJETA`: no mueve efectivo;
- `TRANSFERENCIA`: no mueve efectivo;
- `CREDITO`: aumenta deuda y no mueve efectivo.

### 9.3 Movimientos manuales

- Solo propietario/administrador.
- Tipos: ingreso y retiro.
- Monto estrictamente mayor a cero.
- Concepto obligatorio, trim, entre 3 y 200 caracteres.
- No se permite editar ni borrar movimientos.
- Un retiro puede superar el efectivo teórico; se permite porque el conteo físico es la fuente final, pero la UI exige confirmación reforzada.

### 9.4 Cierre y arqueo

El efectivo esperado se calcula exclusivamente como:

`apertura + ventas en efectivo + ingresos manuales + abonos en efectivo - retiros - devoluciones en efectivo - reversos de ventas en efectivo`.

El usuario introduce el efectivo contado. Se muestra diferencia en tiempo real:

`contado - esperado`.

Reglas:

- monto contado no negativo;
- nota obligatoria si diferencia distinta de cero, entre 5 y 300 caracteres;
- diálogo final muestra apertura, ingresos, egresos, esperado, contado y diferencia;
- cerrar fija todos los campos y estado `CERRADA` dentro de una transacción;
- una sesión cerrada no puede reabrirse ni modificarse;
- el cajero solo cierra su sesión; propietario/administrador puede cerrar cualquiera;
- una venta o abono iniciado antes del cierre y confirmado después debe serializarse con la transacción: o entra antes del cierre o falla porque la sesión ya está cerrada.

## 10. Punto 12: postventa e inventario avanzado

### 10.1 Detalle de venta

Historial debe abrir una vista con:

- número, fecha, estado y forma de pago;
- empresa, vendedor y cliente cuando aplique;
- líneas con nombre snapshot, cantidad, precio y subtotal;
- subtotal, descuento, impuesto y total;
- devoluciones previas;
- datos de anulación;
- acciones visibles según permiso y estado.

### 10.2 Anulación total

Política:

- solo propietario/administrador;
- solo venta `COMPLETADA`;
- solo durante el mismo día local de la venta;
- no puede existir una devolución previa;
- motivo obligatorio entre 5 y 300 caracteres;
- debe existir una caja abierta para registrar la reversión financiera;
- restaura todo el stock;
- crea movimientos de inventario `ANULACION`;
- cambia venta a `ANULADA`, sin borrarla;
- crea auditoría;
- efectivo: crea `REVERSO_ANULACION` en la caja abierta;
- tarjeta/transferencia: exige checkbox “El reembolso externo ya fue confirmado”; no mueve efectivo;
- crédito: solo se permite si la deuda actual del cliente es al menos el total de la venta; reduce deuda y crea movimiento de crédito `ANULACION`;
- si el crédito ya fue abonado por debajo de ese monto, se bloquea y se indica usar devolución por importe permitido o resolver fuera de la app.

### 10.3 Devolución parcial o total

Política:

- solo propietario/administrador;
- venta `COMPLETADA`, nunca `ANULADA`;
- una o más líneas con cantidad entera mayor que cero;
- acumulado devuelto por detalle no puede superar cantidad vendida;
- el importe se calcula con el precio unitario histórico, no con precio actual;
- descuento e impuesto se prorratean por centavos con un algoritmo determinista; el remanente se asigna a la última línea para que nunca se exceda el total original;
- motivo obligatorio entre 5 y 300 caracteres;
- repone inventario y registra movimientos `DEVOLUCION`;
- efectivo: requiere caja abierta y crea `EGRESO_DEVOLUCION`;
- tarjeta/transferencia: requiere confirmación de reembolso externo y no mueve efectivo;
- crédito: reduce deuda; se bloquea si el monto devuelto supera la deuda actual;
- cada devolución queda inmutable y visible en el detalle.

Cuando la suma devuelta alcance el total neto posible, la venta sigue `COMPLETADA` pero la UI muestra “Devuelta totalmente”. No introducir un tercer estado de venta derivado que pueda contradecir los detalles.

### 10.4 Movimientos manuales de inventario

Solo propietario/administrador puede registrar:

- `ENTRADA_MANUAL`: suma cantidad;
- `SALIDA_MANUAL`: resta cantidad y exige stock suficiente;
- `AJUSTE`: recibe nuevo stock objetivo no negativo y deriva cantidad firmada.

Todo movimiento exige producto activo, cantidad válida, motivo entre 5 y 300 caracteres, confirmación y transacción. El stock nunca puede quedar negativo. No se edita ni elimina un movimiento.

## 11. Punto 13: crédito, abonos y estado de cuenta

### 11.1 Saldo único

`Cliente.creditoActual` sigue siendo el saldo materializado para validación rápida. `MovimientoCredito` es el libro inmutable que explica cada cambio. Ambos deben coincidir después de cada transacción.

Invariante:

`creditoActual = suma de importeFirmado de todos los movimientos del cliente` y `creditoActual >= 0`.

No se implementa asignación FIFO de abonos a facturas. Un abono reduce el saldo global del cliente; `idVenta` es opcional y solo sirve como referencia informativa cuando el usuario abrió el abono desde una venta concreta.

### 11.2 Abonos

- Cliente activo y crédito activo o suspendido; un cliente suspendido puede pagar, pero no comprar a crédito.
- Monto mayor que cero y no superior a `creditoActual`.
- Medios: efectivo, tarjeta o transferencia.
- Efectivo exige caja abierta y crea `INGRESO_ABONO`.
- Tarjeta/transferencia requieren confirmación de operación externa y no mueven efectivo.
- Nota opcional hasta 300 caracteres.
- El abono, reducción de saldo, movimiento de crédito, posible movimiento de caja y auditoría son atómicos.
- Después del commit se genera comprobante PDF local con saldo anterior, abono y saldo nuevo.
- No se elimina ni edita un abono.

### 11.3 Estado de cuenta

Desde Clientes se abre una pantalla con:

- límite, deuda, disponible y estado;
- filtro de fecha desde/hasta;
- lista cronológica descendente con venta, abono, devolución o anulación;
- importe firmado, saldo posterior, fecha, usuario y referencia;
- estado vacío;
- botón “Registrar abono” según permiso;
- exportar CSV y PDF.

No mostrar el `credentialId`, hash, salt ni PIN.

## 12. Punto 14: reportes y exportación

### 12.1 Filtros comunes

- empresa activa obligatoria;
- fecha desde y hasta inclusivas;
- desde no puede ser posterior a hasta;
- rango máximo de 366 días por consulta/exportación;
- vendedor opcional;
- forma de pago opcional;
- solo propietario/administrador.

### 12.2 Reportes obligatorios

1. **Ventas**: cantidad, bruto, descuentos, impuestos, neto cobrado, anulaciones y devoluciones.
2. **Rentabilidad**: ingreso neto menos costo snapshot de unidades no devueltas. Los detalles migrados previos a v13 se rotulan como costo aproximado.
3. **Caja**: sesiones, apertura, ingresos, egresos, esperado, contado y diferencias.
4. **Inventario**: stock actual, stock mínimo, valorización a costo, productos bajos y movimientos por periodo.
5. **Crédito**: cartera total, ventas a crédito, abonos, devoluciones/anulaciones y clientes con saldo.

Las ventas anuladas no cuentan como ingreso neto. Las devoluciones reducen ventas y costo de unidades vendidas. Nunca derivar reportes desde texto visible de la UI.

### 12.3 CSV

- UTF-8 con BOM para compatibilidad con Excel;
- separador coma;
- encabezados en español;
- comillas dobles escapadas según RFC 4180;
- fechas legibles y montos en unidad mayor con dos decimales;
- nombre: `multipos_<reporte>_<empresa>_<yyyyMMdd_HHmm>.csv`;
- compartir solo con URI de `FileProvider` y permiso temporal de lectura.

### 12.4 PDF

- usar `PdfDocument` o el patrón existente, sin nueva dependencia;
- encabezado con empresa, reporte, periodo y fecha de generación;
- totales y tabla paginada sin cortar filas;
- pie con número de página;
- nombre equivalente con extensión `.pdf`;
- si no hay datos, permitir exportar un PDF que indique “Sin datos para el periodo”.

## 13. Punto 15: seguridad general

### 13.1 Login

- Username se normaliza con `trim`; conservar la política actual de mayúsculas/minúsculas de consulta.
- Cinco contraseñas incorrectas consecutivas bloquean el usuario 15 minutos.
- Un login correcto reinicia contador, borra bloqueo y actualiza `ultimoLogin`.
- Usuario inexistente devuelve el mismo mensaje genérico que contraseña incorrecta.
- El bloqueo se persiste en Room.
- Toda `CharArray` de contraseña se limpia después de verificar.
- La migración de contraseña legacy sigue obligando cambio de clave.

### 13.2 Sesión

`UserSessionStore` almacenará como mínimo:

- usuario autenticado;
- inicio de sesión;
- última actividad.

Política:

- duración absoluta máxima: 12 horas;
- inactividad máxima: 30 minutos;
- validar al abrir/reanudar `HomeActivity` y antes de cada mutación sensible;
- actualizar actividad desde interacciones reales de `HomeActivity`, no mediante timer infinito;
- al expirar: limpiar sesión, limpiar empresa activa si corresponde, volver a login y mostrar “Tu sesión venció”; 
- no persistir rol como autoridad; se consulta membresía activa en Room.

### 13.3 Manifest y almacenamiento

- cambiar `android:allowBackup="false"`;
- agregar `android:usesCleartextTraffic="false"`;
- revisar si `INTERNET` es realmente necesario; eliminarlo si ninguna dependencia/flujo lo requiere en runtime;
- mantener activities internas con `exported="false"` explícito;
- `LoginActivity` es la única activity exportada por el launcher;
- mantener `FileProvider` no exportado y rutas limitadas a los directorios de exportación necesarios;
- no usar almacenamiento externo amplio ni permisos legacy.

### 13.4 Auditoría

Registrar como mínimo:

- login correcto/bloqueo, sin contraseña;
- apertura/cierre de caja;
- ingreso/retiro manual;
- venta;
- emisión/reemplazo/revocación de credencial;
- cambio de estado o límite de crédito;
- abono;
- anulación/devolución;
- movimiento manual de inventario;
- alta/desactivación/cambio de rol de usuario;
- cambio de empresa activa no requiere auditoría.

`detalle` debe ser conciso y nunca incluir PIN, contraseña, hash, salt, QR completo, token completo ni documento completo. Auditoría no puede editarse ni borrarse desde la app.

## 14. Arquitectura obligatoria

### 14.1 Capas

- **Entity/DAO**: persistencia y consultas puras.
- **Repository o use case**: autorización, invariantes y transacciones.
- **ViewModel**: estado de pantalla, validación no sensible y coordinación de coroutines.
- **Fragment/Activity**: render, navegación, launchers y eventos UI.

Los Fragments no deben construir directamente ventas, devoluciones, cierres, abonos o ajustes mediante varios DAOs.

### 14.2 Repositorios mínimos

- ampliar `SaleRepository`;
- `CashRepository`;
- `ReturnRepository`;
- `InventoryMovementRepository`;
- `CreditRepository`;
- `ReportRepository` para consultas y DTO, sin acceso a vistas;
- `CredentialRepository`;
- `AuditRepository` o helper interno que participe en la transacción;
- ampliar `AuthRepository`.

No crear una abstracción genérica innecesaria. Cada request debe ser inmutable y validar sus campos antes y dentro de la transacción.

### 14.3 Estados de UI

Cada pantalla asíncrona expone un estado sellado o equivalente:

- `Loading`;
- `Content`;
- `Empty`;
- `Error` con mensaje accionable;
- `Submitting` para evitar doble toque cuando muta datos.

El estado se aloja en ViewModel cuando deba sobrevivir rotación. El carrito POS debe migrarse del Fragment a un `PosViewModel` real y conservarse durante rotación. No persistir automáticamente un carrito tras matar el proceso.

### 14.4 Errores de dominio

Usar excepciones selladas o resultados tipados diferenciando al menos:

- permiso revocado;
- empresa inválida;
- caja no abierta/cerrada;
- stock insuficiente;
- saldo/límite insuficiente;
- credencial/PIN inválido, bloqueado o vencido;
- operación duplicada;
- datos inválidos;
- conflicto de estado;
- overflow monetario.

No convertir toda excepción en “stock insuficiente”. Los mensajes visibles deben corresponder al fallo real y no filtrar datos sensibles.

## 15. Orden obligatorio de implementación

El agente ejecutará en este orden. Puede hacer ajustes mecánicos necesarios dentro de una fase, pero no adelantará UI que dependa de un modelo aún no migrado.

### Fase 0. Auditoría base y congelación

1. leer `AGENTS.md`, esta especificación y el protocolo estricto;
2. ejecutar `git status --short` y registrar cambios preexistentes;
3. ejecutar la suite base;
4. revisar entidades, DAO, repositories, fragments, layouts, manifest y esquemas;
5. crear una lista de archivos previstos;
6. no editar si la suite base falla por una regresión no relacionada sin antes informarla.

Salida: diagnóstico breve y baseline de comandos.

### Fase 1. Room 11, auditoría y QR seguro

1. entidades/DAO/migración/esquema;
2. hashing y verificación de PIN;
3. repositorio transaccional de credenciales;
4. emisión/reemplazo/revocación en Clientes;
5. PIN obligatorio en venta a crédito;
6. pruebas unitarias, DAO, transacción y migración 10→11;
7. build.

### Fase 2. Room 12 y caja

1. entidades/DAO/migración/esquema;
2. permisos;
3. `CashRepository` y ViewModel;
4. `CashFragment` y navegación;
5. vincular venta a caja y movimientos de efectivo;
6. pruebas de una sola caja, cierre, diferencia y concurrencia;
7. build.

### Fase 3. Room 13, movimientos y postventa

1. snapshots/migración/esquema;
2. movimiento de inventario en toda venta nueva;
3. ajustes manuales;
4. detalle de venta;
5. anulación;
6. devolución parcial/total;
7. pruebas de límites, prorrateo y rollback completo;
8. build.

### Fase 4. Room 14, libro de crédito y abonos

1. migración robusta de abonos;
2. libro de crédito y reconciliación;
3. `CreditRepository`;
4. estado de cuenta;
5. abono y comprobante;
6. efectos de venta/devolución/anulación sobre libro;
7. pruebas de saldo, concurrencia, caja y migración 13→14;
8. build.

### Fase 5. Reportes

1. consultas agregadas con filtros;
2. pantalla y estados;
3. CSV;
4. PDF paginado;
5. pruebas de agregación, devoluciones, anulaciones y escaping;
6. build.

### Fase 6. Room 15 y seguridad general

1. migración y bloqueo de login;
2. caducidad de sesión;
3. manifest y `FileProvider`;
4. completar auditoría de todos los repositorios;
5. `AuditFragment` solo para propietario;
6. pruebas de bloqueo, expiración, permisos y migración 14→15;
7. build y lint.

### Fase 7. Integración y acabado

1. eliminar placeholders de flujos finales, en especial `PosViewModel`;
2. verificar rotación, doble toque, procesos concurrentes y mensajes;
3. revisar strings, accesibilidad, estados vacíos y pantallas pequeñas;
4. ejecutar búsqueda de secretos, `TODO` y `FIXME`;
5. ejecutar suite final completa;
6. instalar y probar APK en dispositivo/emulador;
7. producir informe final.

## 16. Pruebas obligatorias

### 16.1 Unitarias

Como mínimo:

- cálculo de total, descuento, impuesto y overflow;
- prorrateo exacto de devolución;
- cálculo de efectivo esperado/diferencia;
- reglas de anulación y devolución;
- saldo/disponible de crédito;
- hashing, vencimiento, intentos y bloqueo de PIN;
- intentos/bloqueo de login;
- expiración absoluta e inactividad de sesión;
- matriz completa de roles/permisos;
- CSV con coma, comillas, saltos y caracteres Unicode;
- estado de ViewModels y guardas contra doble envío.

### 16.2 Instrumentadas/Room

Como mínimo:

- migración encadenada 10→11→12→13→14→15;
- apertura única de caja por empresa;
- empresas diferentes pueden tener caja abierta simultáneamente;
- venta atómica actualizada con caja, stock y auditoría;
- venta a crédito con QR+PIN y límite;
- PIN incorrecto no registra venta ni cambia stock;
- venta concurrente nunca deja stock negativo;
- cierre concurrente con venta no produce estado parcial;
- anulación restaura stock y finanzas;
- devolución parcial acumulada no excede venta;
- salida manual no deja stock negativo;
- abono no supera deuda y actualiza caja/libro/saldo;
- rollback inyectado en cada operación compuesta;
- consultas y reportes aislados por empresa;
- historial conserva productos/clientes/usuarios archivados;
- claves foráneas e índices esperados.

### 16.3 UI/manual crítica

Documentar evidencia de este recorrido:

1. crear o usar empresa;
2. iniciar sesión como propietario;
3. crear producto y cliente con crédito;
4. emitir QR+PIN;
5. abrir caja;
6. vender en efectivo;
7. vender a crédito escaneando QR e ingresando PIN;
8. rotar pantalla con carrito cargado;
9. registrar abono en efectivo;
10. devolver parcialmente una venta;
11. anular otra venta válida;
12. ajustar inventario;
13. cerrar caja con y sin diferencia;
14. consultar reportes y exportar CSV/PDF;
15. comprobar permisos de administrador, cajero y vendedor;
16. comprobar expiración/bloqueo de sesión de forma controlada;
17. reiniciar app y confirmar persistencia.

## 17. Comandos de verificación

Usar Java 17 de Android Studio cuando sea necesario:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebugAndroidTest
.\gradlew.bat lintDebug
.\gradlew.bat assembleDebug
```

Además:

```powershell
rg -n "TODO|FIXME|password\s*=|token\s*=|secret\s*=" app/src
Get-Item app/build/outputs/apk/debug/app-debug.apk
```

La búsqueda puede producir falsos positivos. Se revisan manualmente; no se borran coincidencias a ciegas.

### 17.1 Puerta por fase

Después de cada migración o cambio de negocio:

1. ejecutar pruebas relacionadas;
2. ejecutar `testDebugUnitTest`;
3. ejecutar `assembleDebugAndroidTest`;
4. ejecutar `assembleDebug`.

`lintDebug` es obligatorio al terminar fases 1, 5, 6 y 7, y siempre que se modifiquen layouts, manifest o recursos.

## 18. Criterios de rechazo

La implementación no se acepta si ocurre cualquiera de estos casos:

- datos destruidos mediante migración destructiva;
- venta, anulación, devolución, abono, caja o inventario con cambios parciales;
- dinero almacenado o calculado con `Double`/`Float`;
- PIN, contraseña, hash o token expuesto en logs/UI/auditoría;
- permiso comprobado solo ocultando un botón;
- venta sin caja abierta;
- stock o deuda negativos;
- más de una caja abierta por empresa;
- devolución mayor que cantidad/importe vendido;
- edición o borrado de historial financiero;
- consultas sin filtro de empresa;
- lógica crítica concentrada en Fragment/Activity;
- tests existentes eliminados o debilitados para hacerlos pasar;
- warnings de lint nuevos introducidos por la implementación sin explicación;
- build final no reproducible;
- APK no generado;
- publicación o uso de credenciales externas.

## 19. Formato obligatorio del informe del agente ejecutor

El informe final debe incluir, en este orden:

1. resultado global: completado, parcial o bloqueado;
2. funcionalidades terminadas;
3. versiones y migraciones Room creadas;
4. archivos modificados agrupados por capa;
5. pruebas agregadas y número de pruebas ejecutadas;
6. comandos exactos y resultado;
7. ubicación exacta y tamaño del APK;
8. prueba manual realizada y dispositivo/emulador usado;
9. advertencias de lint restantes, si existen;
10. limitaciones reales y trabajo no incluido por la sección 2.1;
11. cambios preexistentes preservados;
12. cualquier desviación de esta especificación, con justificación técnica y autorización del usuario.

No debe afirmar “todo listo” si falta una migración, una prueba crítica, el APK o una operación atómica.

## 20. Lista final de aceptación del revisor

- [ ] Worktree original preservado.
- [ ] Room final en versión 15.
- [ ] Esquemas 11, 12, 13, 14 y 15 exportados.
- [ ] Migración encadenada desde versión 10 aprobada.
- [ ] QR opaco, PIN seguro, vencimiento, reemplazo y bloqueo funcionales.
- [ ] Una sola caja abierta por empresa.
- [ ] Toda venta vinculada a caja.
- [ ] Arqueo y cierre inmutables.
- [ ] Venta/stock/caja/crédito/auditoría atómicos.
- [ ] Anulación total funcional.
- [ ] Devolución parcial y total funcional.
- [ ] Movimientos de inventario trazables.
- [ ] Abonos sin `idVenta = 0` y libro de crédito reconciliado.
- [ ] Reportes correctos para anulaciones y devoluciones.
- [ ] CSV/PDF compartidos con `FileProvider`.
- [ ] Bloqueo de login y expiración de sesión.
- [ ] `allowBackup=false` y cleartext deshabilitado.
- [ ] Permisos verificados en UI y repositorio.
- [ ] Rotación conserva carrito y estados relevantes.
- [ ] Sin `TODO`, `FIXME`, placeholders ni secretos en el alcance final.
- [ ] Pruebas unitarias aprobadas.
- [ ] Pruebas instrumentadas compiladas y ejecutadas cuando haya dispositivo.
- [ ] Lint sin errores y sin regresiones no justificadas.
- [ ] APK debug generado e instalable.
- [ ] No hubo publicación ni creación de credenciales release.

---

Esta especificación congela el alcance de la versión local final. Cualquier capacidad fuera de ella requiere una nueva orden explícita del usuario y no debe añadirse “por conveniencia” durante la implementación.
