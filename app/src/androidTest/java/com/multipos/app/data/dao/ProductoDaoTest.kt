package com.multipos.app.data.dao

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.multipos.app.data.AppDatabase
import com.multipos.app.data.entities.Producto
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProductoDaoTest {
    private lateinit var database: AppDatabase
    private lateinit var productoDao: ProductoDao

    @Before
    fun createDatabase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        productoDao = database.productoDao()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun duplicateInternalCodeInSameCompanyIsRejected() = runBlocking {
        productoDao.insert(product(code = "SKU-001", barcode = "BAR-001"))

        expectConstraintViolation {
            productoDao.insert(product(name = "Duplicado", code = "SKU-001", barcode = "BAR-002"))
        }

        assertEquals(1, productoDao.count(COMPANY_A))
    }

    @Test
    fun duplicateBarcodeInSameCompanyIsRejected() = runBlocking {
        productoDao.insert(product(code = "SKU-001", barcode = "BAR-001"))

        expectConstraintViolation {
            productoDao.insert(product(name = "Duplicado", code = "SKU-002", barcode = "BAR-001"))
        }

        assertEquals(1, productoDao.count(COMPANY_A))
    }

    @Test
    fun sameCodesAreAllowedInDifferentCompanies() = runBlocking {
        val firstId = productoDao.insert(product(companyId = COMPANY_A))
        val secondId = productoDao.insert(product(companyId = COMPANY_B))

        assertNotEquals(firstId, secondId)
        assertEquals(1, productoDao.count(COMPANY_A))
        assertEquals(1, productoDao.count(COMPANY_B))
    }

    @Test
    fun rejectedDuplicateDoesNotReplaceOriginalProductOrStock() = runBlocking {
        val originalId = productoDao.insert(
            product(name = "Original", code = "SKU-001", barcode = "BAR-001", stock = 7)
        ).toInt()

        expectConstraintViolation {
            productoDao.insert(
                product(name = "Reemplazo", code = "SKU-001", barcode = "BAR-999", stock = 99)
            )
        }

        val stored = productoDao.getByCode(COMPANY_A, "SKU-001")
        assertNotNull(stored)
        assertEquals(originalId, stored?.id)
        assertEquals("Original", stored?.nombre)
        assertEquals(7, stored?.stock)
        assertEquals("BAR-001", stored?.codigoBarras)
    }

    @Test
    fun archiveHidesProductFromOperationsButPreservesRow() = runBlocking {
        val productId = productoDao.insert(product()).toInt()

        assertEquals(1, productoDao.archive(productId, COMPANY_A))
        assertEquals(0, productoDao.count(COMPANY_A))
        assertEquals(0, productoDao.getAll(COMPANY_A).first().size)
        assertEquals(null, productoDao.getByCode(COMPANY_A, "SKU-001"))

        val archived = productoDao.getByIdIncludingInactive(productId, COMPANY_A)
        assertNotNull(archived)
        assertEquals(false, archived?.activo)
        assertEquals("SKU-001", archived?.codigo)
    }

    private suspend fun expectConstraintViolation(block: suspend () -> Unit) {
        try {
            block()
            fail("Se esperaba una violación de unicidad")
        } catch (_: SQLiteConstraintException) {
            // Resultado esperado: Room conserva la fila original.
        }
    }

    private fun product(
        name: String = "Producto",
        code: String = "SKU-001",
        barcode: String? = "BAR-001",
        stock: Int = 10,
        companyId: String = COMPANY_A
    ) = Producto(
        nombre = name,
        codigo = code,
        precioVenta = 1_500,
        costoUnitario = 1_000,
        stock = stock,
        codigoBarras = barcode,
        empresaId = companyId
    )

    private companion object {
        const val COMPANY_A = "company-a"
        const val COMPANY_B = "company-b"
    }
}
