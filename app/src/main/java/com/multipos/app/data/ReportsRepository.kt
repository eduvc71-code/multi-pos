package com.multipos.app.data

import com.multipos.app.data.entities.MovimientoCaja
import com.multipos.app.data.entities.MovimientoCredito
import com.multipos.app.data.entities.Venta
import com.multipos.app.security.CompanyPermission
import com.multipos.app.security.CompanyPermissions
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ReportRow(
    val id: Int = 0,
    val fecha: String,
    val categoria: String,
    val concepto: String,
    val importe: Long,
    val descripcion: String = concepto,
    val cantidad: Int = 1,
    val total: Long = importe
)

data class ReportSummary(val totals: Map<String, Long>) {
    fun total(categoria: String): Long = totals[categoria] ?: 0L
}

data class ReportData(
    val rows: List<ReportRow>,
    val summary: ReportSummary,
    val flags: Set<String> = emptySet(),
    val totalVentas: Long = 0,
    val totalGanancia: Long = 0,
    val totalVentasCount: Int = 0,
    val rentabilidad: Double = 0.0
)

enum class Unidad { MONEDA, UNIDADES }

object ReportAggregator {
    fun aggregate(rows: List<ReportRow>): ReportSummary {
        val totals = linkedMapOf<String, Long>()
        rows.forEach { row ->
            totals[row.categoria] = Math.addExact(totals[row.categoria] ?: 0L, row.importe)
        }
        return ReportSummary(totals)
    }

    fun withinLimit(desde: Long, hastaExclusive: Long, maxDays: Int = ReportsRepository.MAX_RANGE_DAYS): Boolean {
        if (desde >= hastaExclusive) return false
        val width = try {
            Math.subtractExact(hastaExclusive, desde)
        } catch (_: ArithmeticException) {
            return false
        }
        return width <= maxDays * ReportsRepository.DAY_MS
    }
}

class ReportException(message: String) : Exception(message) {
    companion object {
        const val NOT_AUTHORIZED = "NotAuthorized"
        const val INVALID_RANGE = "InvalidRange"
        const val RANGE_TOO_WIDE = "RangeTooWide"
        const val OVERFLOW = "Overflow"
    }
}

enum class ReporteTipo { VENTAS, RENTABILIDAD, CAJA, INVENTARIO, CREDITO }

class ReportsRepository(private val database: AppDatabase) {

    suspend fun compute(
        companyId: String,
        userId: Int,
        desde: Long,
        hastaExclusive: Long,
        tipo: ReporteTipo,
        medioPago: String? = null,
        vendedorId: Int? = null
    ): ReportData {
        val role = database.usuarioEmpresaDao().getActiveMembership(userId, companyId)?.rol
        if (!CompanyPermissions.allows(role, CompanyPermission.VIEW_REPORTS)) {
            throw ReportException(ReportException.NOT_AUTHORIZED)
        }
        if (desde >= hastaExclusive) {
            throw ReportException(ReportException.INVALID_RANGE)
        }
        if (!ReportAggregator.withinLimit(desde, hastaExclusive)) {
            throw ReportException(ReportException.RANGE_TOO_WIDE)
        }
        return when (tipo) {
            ReporteTipo.VENTAS -> ventas(companyId, desde, hastaExclusive, medioPago, vendedorId)
            ReporteTipo.RENTABILIDAD -> rentabilidad(companyId, desde, hastaExclusive, medioPago, vendedorId)
            ReporteTipo.CAJA -> caja(companyId, desde, hastaExclusive, vendedorId)
            ReporteTipo.INVENTARIO -> inventario(companyId, desde, hastaExclusive, vendedorId)
            ReporteTipo.CREDITO -> credito(companyId, desde, hastaExclusive, medioPago, vendedorId)
        }
    }

    suspend fun summary(rows: List<ReportRow>): ReportSummary = ReportAggregator.aggregate(rows)

    private suspend fun ventas(
        companyId: String,
        desde: Long,
        hastaExclusive: Long,
        medioPago: String?,
        vendedorId: Int?
    ): ReportData {
        val rows = mutableListOf<ReportRow>()
        val ventas = database.ventaDao().getInRange(companyId, desde, hastaExclusive)
            .filter { medioPago == null || it.tipoPago == medioPago }
            .filter { vendedorId == null || it.idUsuario == vendedorId }
        var cantidad = 0L
        var bruto = 0L
        var descuentos = 0L
        var impuestos = 0L
        var neto = 0L
        var anuladas = 0L
        var anuladasCount = 0L
        ventas.forEach { venta ->
            val name = userName(venta.idUsuario)
            if (venta.estado == Venta.ESTADO_COMPLETADA) {
                cantidad = Math.addExact(cantidad, 1L)
                bruto = Math.addExact(bruto, venta.subtotal)
                descuentos = Math.addExact(descuentos, venta.descuento)
                impuestos = Math.addExact(impuestos, venta.impuesto)
                neto = Math.addExact(neto, venta.total)
                rows += row(venta.fecha, CAT_VENTAS, "Venta #${venta.id} · ${venta.tipoPago} · $name", venta.total)
            } else {
                anuladasCount = Math.addExact(anuladasCount, 1L)
                anuladas = Math.addExact(anuladas, venta.total)
                rows += row(venta.fecha, CAT_ANULADAS, "Anulación #${venta.id} · $name", -venta.total)
            }
        }
        var devoluciones = 0L
        database.devolucionDao().getInRange(companyId, desde, hastaExclusive)
            .filter { devolucionDeVenta(companyId, it, medioPago, vendedorId) }
            .forEach { d ->
                devoluciones = Math.addExact(devoluciones, d.monto)
                rows += row(d.fecha, CAT_DEVOLUCIONES, "Devolución #${d.id} · Venta #${d.ventaId}", -d.monto)
            }
        val totals = linkedMapOf<String, Long>()
        totals[CAT_CANTIDAD] = cantidad
        totals[CAT_BRUTO] = bruto
        totals[CAT_DESCUENTOS] = descuentos
        totals[CAT_IMPUESTOS] = impuestos
        totals[CAT_NETO] = Math.subtractExact(neto, devoluciones)
        totals[CAT_ANULADAS] = -anuladas
        totals[CAT_ANULADAS_COUNT] = anuladasCount
        totals[CAT_DEVOLUCIONES] = -devoluciones
        return ReportData(rows, ReportSummary(totals))
    }

    private suspend fun rentabilidad(
        companyId: String,
        desde: Long,
        hastaExclusive: Long,
        medioPago: String?,
        vendedorId: Int?
    ): ReportData {
        val rows = mutableListOf<ReportRow>()
        val ventas = database.ventaDao().getInRangeByEstado(companyId, Venta.ESTADO_COMPLETADA, desde, hastaExclusive)
            .filter { medioPago == null || it.tipoPago == medioPago }
            .filter { vendedorId == null || it.idUsuario == vendedorId }
        val ventasEnRango = ventas.map { it.id }.toSet()

        // Solo devoluciones del periodo (fecha >= desde AND fecha < hastaExclusive),
        // filtradas por la venta original (vendedor y forma de pago), no por Devolucion.usuarioId.
        val devoluciones = database.devolucionDao().getInRange(companyId, desde, hastaExclusive)
            .filter { devolucionDeVenta(companyId, it, medioPago, vendedorId) }

        // Cantidad devuelta por (venta, detalle) contando solo devoluciones del periodo.
        val devueltoEnPeriodo = HashMap<Pair<Int, Int>, Int>()
        devoluciones.forEach { d ->
            database.devolucionDao().getDetails(d.id).forEach { dd ->
                val key = Pair(d.ventaId, dd.detalleVentaId)
                devueltoEnPeriodo[key] = (devueltoEnPeriodo[key] ?: 0) + dd.cantidad
            }
        }

        var ingresoNeto = ventas.safeSum { it.total }
        ingresoNeto = Math.subtractExact(ingresoNeto, devoluciones.safeSum { it.monto })

        var costo = 0L
        var costoAproximado = false
        ventas.forEach { venta ->
            val detalles = database.ventaDao().getDetails(venta.id, companyId)
            var costoVenta = 0L
            detalles.forEach { d ->
                val devuelto = devueltoEnPeriodo[Pair(venta.id, d.id)] ?: 0
                val unidades = (d.cantidad - devuelto).coerceAtLeast(0)
                if (unidades > 0) {
                    if (d.costoUnitario == 0L) costoAproximado = true
                    costoVenta = Math.addExact(costoVenta, Math.multiplyExact(unidades.toLong(), d.costoUnitario))
                }
            }
            val devolucionesVenta = devoluciones.filter { it.ventaId == venta.id }.safeSum { it.monto }
            val ingresoVenta = Math.subtractExact(venta.total, devolucionesVenta)
            val gananciaVenta = Math.subtractExact(ingresoVenta, costoVenta)
            costo = Math.addExact(costo, costoVenta)
            rows += row(venta.fecha, CAT_GANANCIA, "Venta #${venta.id}", gananciaVenta)
            rows += row(venta.fecha, CAT_COSTOS, "Costo #${venta.id}", costoVenta)
        }

        // Devoluciones del periodo de ventas ANTERIORES (fuera del rango): revierten ingreso y costo.
        devoluciones.filter { it.ventaId !in ventasEnRango }.forEach { d ->
            rows += row(d.fecha, CAT_DEVOLUCIONES, "Devolución #${d.id} · Venta #${d.ventaId}", -d.monto)
            database.devolucionDao().getDetails(d.id).forEach { dd ->
                val detalle = database.ventaDao().getDetalleById(dd.detalleVentaId)
                if (detalle != null) {
                    if (detalle.costoUnitario == 0L) costoAproximado = true
                    costo = Math.subtractExact(costo, Math.multiplyExact(dd.cantidad.toLong(), detalle.costoUnitario))
                }
            }
        }

        val ganancia = Math.subtractExact(ingresoNeto, costo)
        val totals = linkedMapOf<String, Long>()
        totals[CAT_INGRESO_NETO] = ingresoNeto
        totals[CAT_COSTOS] = costo
        totals[CAT_GANANCIA] = ganancia
        totals[CAT_DEVOLUCIONES] = -devoluciones.safeSum { it.monto }
        return ReportData(rows, ReportSummary(totals), if (costoAproximado) setOf(FLAG_COSTO_APROXIMADO) else emptySet())
    }

    private suspend fun caja(
        companyId: String,
        desde: Long,
        hastaExclusive: Long,
        vendedorId: Int?
    ): ReportData {
        val rows = mutableListOf<ReportRow>()
        var sesiones = 0L
        var apertura = 0L
        var ingresos = 0L
        var egresos = 0L
        var esperado = 0L
        var contado = 0L
        var diferencia = 0L
        val sesionesConMovimientos = database.movimientoCajaDao()
            .getByCompanyBetween(companyId, desde, hastaExclusive)
            .map { it.cajaSesionId }
            .distinct()
        sesionesConMovimientos.forEach { sesionId ->
            val session = database.cajaSesionDao().getById(sesionId, companyId) ?: return@forEach
            if (vendedorId != null && session.abiertaPorUsuarioId != vendedorId) return@forEach
            sesiones = Math.addExact(sesiones, 1L)
            apertura = Math.addExact(apertura, session.montoApertura)
            val ingresosSesion = database.movimientoCajaDao().totalIngresosEnRango(sesionId, companyId, desde, hastaExclusive)
            val egresosSesion = database.movimientoCajaDao().totalEgresosEnRango(sesionId, companyId, desde, hastaExclusive)
            ingresos = Math.addExact(ingresos, ingresosSesion)
            egresos = Math.addExact(egresos, egresosSesion)
            val esperadoSesion = Math.subtractExact(Math.addExact(session.montoApertura, ingresosSesion), egresosSesion)
            val contadoSesion = session.montoContadoCierre
            val diferenciaSesion = session.diferenciaCierre
                ?: if (contadoSesion != null) Math.subtractExact(contadoSesion, esperadoSesion) else 0L
            esperado = Math.addExact(esperado, esperadoSesion)
            if (contadoSesion != null) contado = Math.addExact(contado, contadoSesion)
            diferencia = Math.addExact(diferencia, diferenciaSesion)
            rows += row(session.fechaApertura, CAT_APERTURA, "Apertura sesión #$sesionId", session.montoApertura)
            rows += row(session.fechaApertura, CAT_INGRESOS, "Ingresos sesión #$sesionId", ingresosSesion)
            rows += row(session.fechaApertura, CAT_EGRESOS, "Egresos sesión #$sesionId", -egresosSesion)
            rows += row(session.fechaApertura, CAT_ESPERADO, "Esperado sesión #$sesionId", esperadoSesion)
            rows += row(session.fechaApertura, CAT_CONTADO, "Contado sesión #$sesionId", contadoSesion ?: 0L)
            rows += row(session.fechaApertura, CAT_DIFERENCIA, "Diferencia sesión #$sesionId", diferenciaSesion)
        }
        val totals = linkedMapOf<String, Long>()
        totals[CAT_SESIONES] = sesiones
        totals[CAT_APERTURA] = apertura
        totals[CAT_INGRESOS] = ingresos
        totals[CAT_EGRESOS] = -egresos
        totals[CAT_ESPERADO] = esperado
        totals[CAT_CONTADO] = contado
        totals[CAT_DIFERENCIA] = diferencia
        return ReportData(rows, ReportSummary(totals))
    }

    private suspend fun inventario(
        companyId: String,
        desde: Long,
        hastaExclusive: Long,
        vendedorId: Int?
    ): ReportData {
        val rows = mutableListOf<ReportRow>()
        var stockTotal = 0L
        var valorCosto = 0L
        var stockBajo = 0L
        database.productoDao().getAllOnce(companyId).forEach { p ->
            stockTotal = Math.addExact(stockTotal, p.stock.toLong())
            valorCosto = Math.addExact(valorCosto, Math.multiplyExact(p.stock.toLong(), p.costoUnitario))
            if (p.stock <= p.stockMinimo) stockBajo = Math.addExact(stockBajo, 1L)
            rows += row(0L, CAT_STOCK_ACTUAL, "Stock ${p.nombre}", p.stock.toLong())
        }
        var entradas = 0L
        var salidas = 0L
        var movimientos = 0L
        database.movimientoInventarioDao().getByDateRange(companyId, desde, hastaExclusive)
            .filter { vendedorId == null || it.usuarioId == vendedorId }
            .forEach { m ->
                movimientos = Math.addExact(movimientos, 1L)
                if (m.cantidadFirmada >= 0) {
                    entradas = Math.addExact(entradas, m.cantidadFirmada.toLong())
                } else {
                    salidas = Math.addExact(salidas, Math.negateExact(m.cantidadFirmada.toLong()))
                }
                rows += row(
                    m.fecha,
                    if (m.cantidadFirmada >= 0) CAT_ENTRADAS else CAT_SALIDAS,
                    "${m.tipo} · producto ${m.productoId}",
                    m.cantidadFirmada.toLong()
                )
            }
        val totals = linkedMapOf<String, Long>()
        totals[CAT_STOCK_ACTUAL] = stockTotal
        totals[CAT_VALOR_COSTO] = valorCosto
        totals[CAT_STOCK_BAJO] = stockBajo
        totals[CAT_ENTRADAS] = entradas
        totals[CAT_SALIDAS] = -salidas
        totals[CAT_MOVIMIENTOS] = movimientos
        return ReportData(rows, ReportSummary(totals))
    }

    private suspend fun credito(
        companyId: String,
        desde: Long,
        hastaExclusive: Long,
        medioPago: String?,
        vendedorId: Int?
    ): ReportData {
        val rows = mutableListOf<ReportRow>()
        var cartera = 0L
        var clientesConSaldo = 0L
        database.clienteDao().getAllOnce(companyId)
            .filter { it.creditoActual > 0 }
            .forEach { c ->
                cartera = Math.addExact(cartera, c.creditoActual)
                clientesConSaldo = Math.addExact(clientesConSaldo, 1L)
            }
        var ventasCredito = 0L
        var abonos = 0L
        var anulaciones = 0L
        var devoluciones = 0L
        database.movimientoCreditoDao().getByCompanyBetween(companyId, desde, hastaExclusive)
            .filter { vendedorId == null || it.usuarioId == vendedorId }
            .forEach { m ->
                val medio = when (m.tipo) {
                    MovimientoCredito.TIPO_ABONO -> database.abonoDao().getById(m.abonoId ?: 0L)?.medioPago
                    else -> "CREDITO"
                }
                if (medioPago != null && medio != medioPago) return@forEach
                when (m.tipo) {
                    MovimientoCredito.TIPO_VENTA_CREDITO -> {
                        ventasCredito = Math.addExact(ventasCredito, m.importeFirmado)
                        rows += row(m.fecha, CAT_VENTAS_CREDITO, "Venta a crédito #${m.ventaId}", m.importeFirmado)
                    }
                    MovimientoCredito.TIPO_ABONO -> {
                        abonos = Math.addExact(abonos, m.importeFirmado)
                        rows += row(m.fecha, CAT_ABONOS, "Abono #${m.abonoId} · ${medio ?: ""}", m.importeFirmado)
                    }
                    MovimientoCredito.TIPO_ANULACION -> {
                        anulaciones = Math.addExact(anulaciones, m.importeFirmado)
                        rows += row(m.fecha, CAT_CREDITO_ANULACION, "Anulación #${m.ventaId}", m.importeFirmado)
                    }
                    else -> {
                        devoluciones = Math.addExact(devoluciones, m.importeFirmado)
                        rows += row(m.fecha, CAT_DEVOLUCIONES, "Devolución #${m.devolucionId}", m.importeFirmado)
                    }
                }
            }
        val totals = linkedMapOf<String, Long>()
        totals[CAT_CARTERA] = cartera
        totals[CAT_CLIENTES_SALDO] = clientesConSaldo
        totals[CAT_VENTAS_CREDITO] = ventasCredito
        totals[CAT_ABONOS] = abonos
        totals[CAT_CREDITO_ANULACION] = anulaciones
        totals[CAT_DEVOLUCIONES] = devoluciones
        return ReportData(rows, ReportSummary(totals))
    }

    private suspend fun devolucionDeVenta(
        companyId: String,
        devolucion: com.multipos.app.data.entities.Devolucion,
        medioPago: String?,
        vendedorId: Int?
    ): Boolean {
        val venta = database.ventaDao().getById(devolucion.ventaId, companyId) ?: return false
        if (medioPago != null && venta.tipoPago != medioPago) return false
        if (vendedorId != null && venta.idUsuario != vendedorId) return false
        return true
    }

    private fun row(fecha: Long, categoria: String, concepto: String, importe: Long): ReportRow =
        ReportRow(
            fecha = if (fecha > 0L) SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(fecha)) else "",
            categoria = categoria,
            concepto = concepto,
            importe = importe
        )

    private suspend fun userName(userId: Int): String =
        database.usuarioDao().getById(userId)?.nombre ?: "#$userId"

    private inline fun <T> Iterable<T>.safeSum(selector: (T) -> Long): Long {
        var acc = 0L
        for (item in this) acc = Math.addExact(acc, selector(item))
        return acc
    }

    companion object {
        const val MAX_RANGE_DAYS = 366
        const val DAY_MS = 24 * 60 * 60 * 1000L

        const val CAT_CANTIDAD = "CANTIDAD"
        const val CAT_VENTAS = "VENTAS"
        const val CAT_ANULADAS = "ANULADAS"
        const val CAT_ANULADAS_COUNT = "ANULADAS_COUNT"
        const val CAT_BRUTO = "BRUTO"
        const val CAT_DESCUENTOS = "DESCUENTOS"
        const val CAT_IMPUESTOS = "IMPUESTOS"
        const val CAT_NETO = "NETO"
        const val CAT_DEVOLUCIONES = "DEVOLUCIONES"
        const val CAT_GANANCIA = "GANANCIA"
        const val CAT_COSTOS = "COSTOS"
        const val CAT_INGRESO_NETO = "INGRESO_NETO"
        const val CAT_SESIONES = "SESIONES"
        const val CAT_APERTURA = "APERTURA"
        const val CAT_INGRESOS = "INGRESOS"
        const val CAT_EGRESOS = "EGRESOS"
        const val CAT_ESPERADO = "ESPERADO"
        const val CAT_CONTADO = "CONTADO"
        const val CAT_DIFERENCIA = "DIFERENCIA"
        const val CAT_STOCK_ACTUAL = "STOCK_ACTUAL"
        const val CAT_VALOR_COSTO = "VALOR_COSTO"
        const val CAT_STOCK_BAJO = "STOCK_BAJO"
        const val CAT_ENTRADAS = "ENTRADAS"
        const val CAT_SALIDAS = "SALIDAS"
        const val CAT_MOVIMIENTOS = "MOVIMIENTOS"
        const val CAT_VENTAS_CREDITO = "VENTAS_CREDITO"
        const val CAT_ABONOS = "ABONOS"
        const val CAT_CREDITO_ANULACION = "CREDITO_ANULACION"
        const val CAT_CARTERA = "CARTERA"
        const val CAT_CLIENTES_SALDO = "CLIENTES_SALDO"

        const val FLAG_COSTO_APROXIMADO = "COSTO_APROXIMADO"

        private val CATEGORIAS_UNIDADES = setOf(
            CAT_CANTIDAD,
            CAT_ANULADAS_COUNT,
            CAT_SESIONES,
            CAT_STOCK_ACTUAL,
            CAT_STOCK_BAJO,
            CAT_ENTRADAS,
            CAT_SALIDAS,
            CAT_MOVIMIENTOS,
            CAT_CLIENTES_SALDO
        )

        fun unidadDe(categoria: String): Unidad =
            if (categoria in CATEGORIAS_UNIDADES) Unidad.UNIDADES else Unidad.MONEDA
    }
}
