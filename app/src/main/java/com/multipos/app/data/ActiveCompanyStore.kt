package com.multipos.app.data

import android.content.Context
import com.multipos.app.data.entities.Empresa

object ActiveCompanyStore {
    private const val PREFS = "multipos_session"
    private const val KEY_COMPANY = "active_company_id"
    private const val KEY_COLOR = "active_company_color"
    private const val KEY_NAME = "active_company_name"
    fun get(context: Context): String = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_COMPANY, Empresa.DEFAULT_ID) ?: Empresa.DEFAULT_ID
    fun set(context: Context, companyId: String) { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_COMPANY, companyId).apply() }
    fun setName(context: Context, name: String) { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_NAME, name).apply() }
    fun getName(context: Context): String = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_NAME, "Mi Negocio") ?: "Mi Negocio"
    fun setColor(context: Context, colorHex: String) { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_COLOR, colorHex).apply() }
    fun color(context: Context): String = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_COLOR, "#2563EB") ?: "#2563EB"
}
