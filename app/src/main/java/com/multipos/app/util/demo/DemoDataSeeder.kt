package com.multipos.app.util.demo

import com.multipos.app.data.AppDatabase
import com.multipos.app.data.entities.*
import com.multipos.app.security.PasswordHasher
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object DemoDataSeeder {
    
    suspend fun seed(db: AppDatabase) = withContext(Dispatchers.IO) {
        if (db.empresaDao().count() > 0) return@withContext // No borrar si ya existe data

        try {
            db.withTransaction {
                val currentCompanyId = "demo-grocery-store"

                // 1. Crear Empresa Demo Abarrotes
                val company = Empresa(
                    id = currentCompanyId,
                    nombre = "Abarrotes El Economico",
                    tipoNegocio = "TIENDA",
                    colorPrimarioHex = "#00BFA5",
                    nit = "999888777-1",
                    activa = true
                )
                db.empresaDao().insert(company)
                
                // 2. Crear Propietario (admin / admin1234)
                val ownerDigest = PasswordHasher.hash("admin1234".toCharArray())
                val ownerId = db.usuarioDao().insert(
                    Usuario(
                        nombre = "Propietario",
                        usuario = "admin",
                        passwordHash = ownerDigest.hash,
                        passwordSalt = ownerDigest.salt,
                        rol = Usuario.ROL_PROPIETARIO,
                        empresaId = company.id,
                        requiereCambioClave = false
                    )
                ).toInt()
                db.usuarioEmpresaDao().insert(UsuarioEmpresa(ownerId, company.id, Usuario.ROL_PROPIETARIO))
                
                // 3. SEED 30 PRODUCTOS DE ABARROTES
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
                            empresaId = company.id,
                            categoria = "Abarrotes",
                            stockMinimo = 5
                        )
                    )
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("DemoDataSeeder", "Error seeding data", e)
        }
    }
}
