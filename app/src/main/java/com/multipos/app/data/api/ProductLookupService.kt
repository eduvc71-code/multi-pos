package com.multipos.app.data.api

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.multipos.app.data.entities.Producto
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Servicio Senior para la consulta de productos en la base de datos global de Open Food Facts.
 */
object ProductLookupService {
    private val client = OkHttpClient()

    suspend fun lookupByBarcode(code: String, companyId: String): Producto? = withContext(Dispatchers.IO) {
        try {
            val url = "https://world.openfoodfacts.org/api/v0/product/$code.json"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "MultiPOS - Android - Version 2.0")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext null

            val body = response.body?.string() ?: return@withContext null
            val json = JsonParser.parseString(body).asJsonObject

            if (json.get("status")?.asInt != 1) return@withContext null

            val pJson = json.getAsJsonObject("product")
            
            // Mapeo inteligente de campos
            val name = pJson.get("product_name")?.asString ?: "Producto desconocido"
            val brand = pJson.get("brands")?.asString ?: ""
            val category = pJson.get("categories")?.asString?.split(",")?.firstOrNull() ?: "General"
            val imageUrl = pJson.get("image_url")?.asString ?: ""

            return@withContext Producto(
                nombre = if (brand.isNotEmpty()) "$name ($brand)" else name,
                codigo = code,
                codigoBarras = code,
                precioVenta = 0L, // El usuario debe definir el precio
                costoUnitario = 0L,
                stock = 0,
                stockMinimo = 5,
                categoria = category,
                fotoUrl = imageUrl,
                empresaId = companyId
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
