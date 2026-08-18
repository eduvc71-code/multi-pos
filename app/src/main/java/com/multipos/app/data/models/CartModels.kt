package com.multipos.app.data.models

import com.multipos.app.data.entities.Producto

data class CartLine(
    val product: Producto,
    var quantity: Int
) {
    val productId: Int get() = product.id
    val productName: String get() = product.nombre
    val price: Long get() = product.precioVenta
    val subtotal: Long get() = price * quantity
}
