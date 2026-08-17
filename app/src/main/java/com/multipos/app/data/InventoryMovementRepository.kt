package com.multipos.app.data

import androidx.room.withTransaction
import com.multipos.app.data.entities.Auditoria
import com.multipos.app.data.entities.MovimientoInventario
import com.multipos.app.data.entities.Usuario

sealed class InventoryMovementException(message: String) : IllegalStateException(message) {
    object NotAuthorized : InventoryMovementException("No tienes permiso para registrar movimientos de inventario")
    object ProductNotFound : InventoryMovementException("Producto no encontrado")
    object ProductInactive : InventoryMovementException("El producto no está activo")
    object InvalidQuantity : InventoryMovementException("La cantidad debe ser mayor a cero")
    object InsufficientStock : InventoryMovementException("El ajuste supera el stock disponible")
    object InvalidReason : InventoryMovementException("El motivo debe tener entre 5 y 300 caracteres")
}

data class InventoryMovementRequest(
    val companyId: String,
    val productId: Int,
    val userId: Int,
    val tipo: String,
    val cantidad: Int,
    val motivo: String
)

class InventoryMovementRepository(private val database: AppDatabase) {

    suspend fun registerMovement(request: InventoryMovementRequest): MovimientoInventario =
        database.withTransaction {
            val role = activeRole(request.companyId, request.userId)
            if (role != Usuario.ROL_PROPIETARIO && role != Usuario.ROL_ADMINISTRADOR) {
                throw InventoryMovementException.NotAuthorized
            }
            val motivo = request.motivo.trim()
            if (motivo.length !in 5..300) {
                throw InventoryMovementException.InvalidReason
            }
            val product = database.productoDao()
                .getByIdIncludingInactive(request.productId, request.companyId)
                ?: throw InventoryMovementException.ProductNotFound
            if (!product.activo) {
                throw InventoryMovementException.ProductInactive
            }
            if (request.tipo !in SUPPORTED_TYPES) {
                throw InventoryMovementException.InvalidQuantity
            }

            val stockAnterior = product.stock
            val cantidadFirmada: Int
            val stockPosterior: Int
            when (request.tipo) {
                MovimientoInventario.TIPO_ENTRADA_MANUAL -> {
                    if (request.cantidad <= 0) throw InventoryMovementException.InvalidQuantity
                    if (
                        database.productoDao().increaseStock(
                            request.productId,
                            request.cantidad,
                            request.companyId
                        ) == 0
                    ) {
                        throw InventoryMovementException.InsufficientStock
                    }
                    cantidadFirmada = request.cantidad
                    stockPosterior = stockAnterior + request.cantidad
                }
                MovimientoInventario.TIPO_SALIDA_MANUAL -> {
                    if (request.cantidad <= 0) throw InventoryMovementException.InvalidQuantity
                    if (
                        database.productoDao().decreaseStock(
                            request.productId,
                            request.cantidad,
                            request.companyId
                        ) == 0
                    ) {
                        throw InventoryMovementException.InsufficientStock
                    }
                    cantidadFirmada = -request.cantidad
                    stockPosterior = stockAnterior - request.cantidad
                }
                else -> {
                    // AJUSTE: cantidad es el NUEVO stock objetivo (>= 0); la cantidad firmada se deriva.
                    if (request.cantidad < 0) throw InventoryMovementException.InsufficientStock
                    val target = request.cantidad
                    val firmada = target - stockAnterior
                    if (firmada == 0) throw InventoryMovementException.InvalidQuantity
                    if (
                        database.productoDao().setStock(
                            request.productId,
                            target,
                            request.companyId
                        ) == 0
                    ) {
                        throw InventoryMovementException.InsufficientStock
                    }
                    cantidadFirmada = firmada
                    stockPosterior = target
                }
            }

            val now = System.currentTimeMillis()
            val movement = MovimientoInventario(
                empresaId = request.companyId,
                productoId = request.productId,
                usuarioId = request.userId,
                tipo = request.tipo,
                cantidadFirmada = cantidadFirmada,
                stockAnterior = stockAnterior,
                stockPosterior = stockPosterior,
                motivo = motivo,
                fecha = now
            )
            val movementId = database.movimientoInventarioDao().insert(movement)
            database.auditoriaDao().insert(
                Auditoria(
                    empresaId = request.companyId,
                    usuarioId = request.userId,
                    accion = Auditoria.ACCION_MOVIMIENTO_INVENTARIO,
                    entidad = "producto",
                    entidadId = request.productId.toString(),
                    detalle = "movimiento_inventario tipo=${request.tipo} cantidad=${cantidadFirmada} stockAnterior=$stockAnterior stockPosterior=$stockPosterior",
                    fecha = now
                )
            )
            movement.copy(id = movementId)
        }

    private suspend fun activeRole(companyId: String, userId: Int): String? =
        database.usuarioEmpresaDao().getActiveMembership(userId, companyId)?.rol

    private companion object {
        val SUPPORTED_TYPES = setOf(
            MovimientoInventario.TIPO_ENTRADA_MANUAL,
            MovimientoInventario.TIPO_SALIDA_MANUAL,
            MovimientoInventario.TIPO_AJUSTE
        )
    }
}
