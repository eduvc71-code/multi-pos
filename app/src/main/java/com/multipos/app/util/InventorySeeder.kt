package com.multipos.app.util

import com.multipos.app.data.AppDatabase
import com.multipos.app.data.entities.Producto
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object InventorySeeder {

    suspend fun seedAbarrotes(db: AppDatabase, companyId: String) = withContext(Dispatchers.IO) {
        android.util.Log.d("InventorySeeder", "Iniciando siembra de productos para empresa: $companyId")
        try {
            db.withTransaction {
                val currentCount = db.productoDao().count(companyId)
                if (currentCount > 0) {
                    android.util.Log.d("InventorySeeder", "Empresa $companyId ya tiene $currentCount productos. Saltando siembra.")
                    return@withTransaction
                }
                
                val groceryProducts = listOf(
                    Triple("Arroz Grano Largo 1kg", 1200L, 800L),
                    Triple("Frijol Negro 1kg", 1800L, 1200L),
                    Triple("Aceite Vegetal 900ml", 3500L, 2800L),
                    Triple("Azucar Refinada 1kg", 1500L, 1000L),
                    Triple("Sal Yodada 500g", 500L, 300L),
                    Triple("Leche Entera 1L", 2800L, 2200L),
                    Triple("Huevo Blanco 30pz", 4500L, 3800L),
                    Triple("Harina de Trigo 1kg", 1400L, 950L),
                    Triple("Pasta Espagueti 200g", 700L, 450L),
                    Triple("Atun en Agua 140g", 2200L, 1600L),
                    Triple("Cafe Soluble 200g", 8500L, 6500L),
                    Triple("Te Negro 20 sobres", 1200L, 800L),
                    Triple("Refresco Cola 600ml", 1500L, 1100L),
                    Triple("Agua Purificada 1.5L", 1000L, 600L),
                    Triple("Jabón de Barra 150g", 1100L, 750L),
                    Triple("Detergente Polvo 1kg", 3200L, 2500L),
                    Triple("Papel Higienico 4 rollos", 2500L, 1800L),
                    Triple("Pasta Dental 100ml", 3800L, 2900L),
                    Triple("Shampoo 400ml", 5500L, 4200L),
                    Triple("Pan de Caja Blanco", 3800L, 3100L),
                    Triple("Galletas Maria 170g", 900L, 600L),
                    Triple("Mayonesa 390g", 4200L, 3400L),
                    Triple("Salsa de Tomate 210g", 800L, 550L),
                    Triple("Mermelada Fresa 270g", 3500L, 2700L),
                    Triple("Cereal Maiz 500g", 4800L, 3800L),
                    Triple("Lentejas 500g", 1400L, 1000L),
                    Triple("Sopa de Polvo 60g", 600L, 350L),
                    Triple("Chocolate en Polvo 400g", 5200L, 4100L),
                    Triple("Avena Natural 400g", 1300L, 900L),
                    Triple("Vinagre Blanco 1L", 1100L, 700L)
                )

                groceryProducts.forEach { (name, price, cost) ->
                    db.productoDao().insert(
                        Producto(
                            nombre = name,
                            codigo = "GEN-${name.take(3).uppercase()}-${(100..999).random()}",
                            precioVenta = price,
                            costoUnitario = cost,
                            stock = (10..100).random(),
                            empresaId = companyId,
                            categoria = "Abarrotes",
                            stockMinimo = 5
                        )
                    )
                }
                android.util.Log.d("InventorySeeder", "Siembra completada exitosamente: 30 productos insertados.")
            }
        } catch (e: Exception) {
            android.util.Log.e("InventorySeeder", "Error crítico al sembrar inventario para $companyId", e)
            throw e
        }
    }
}
