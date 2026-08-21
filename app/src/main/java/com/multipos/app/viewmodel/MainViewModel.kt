package com.multipos.app.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.multipos.app.data.*
import com.multipos.app.data.entities.Usuario
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val db = DatabaseProvider.get(application)
    private val context = application.applicationContext

    private val _destination = MutableStateFlow<String?>(null)
    val destination: StateFlow<String?> = _destination.asStateFlow()

    fun checkAuth() {
        viewModelScope.launch {
            try {
                Log.d("MainViewModel", "Iniciando checkAuth")
                
                // Comentado o eliminado el borrado agresivo de datos demo para evitar efectos secundarios
                /*
                val demoCompany = db.empresaDao().getById("demo-grocery-store")
                if (demoCompany != null) {
                    Log.d("MainViewModel", "Eliminando datos demo")
                    db.withTransaction {
                        db.productoDao().deleteAll("demo-grocery-store")
                        db.usuarioDao().deleteByCompany("demo-grocery-store")
                        db.empresaDao().deleteById("demo-grocery-store")
                    }
                    UserSessionStore.clear(context)
                }
                */

                val companyCount = db.empresaDao().count()
                val userCount = db.usuarioDao().count()
                Log.d("MainViewModel", "Empresas: $companyCount, Usuarios: $userCount")

                if (companyCount == 0 || userCount == 0) {
                    Log.d("MainViewModel", "Navegando a SETUP")
                    _destination.value = "SETUP"
                } else if (UserSessionStore.isAuthenticated(context)) {
                    val userId = UserSessionStore.userId(context)
                    val user = db.usuarioDao().getById(userId)
                    if (user != null) {
                        Log.d("MainViewModel", "Usuario autenticado: ${user.usuario}, navegando a HOME")
                        _destination.value = "HOME"
                    } else {
                        Log.d("MainViewModel", "Sesión huérfana, limpiando y navegando a LOGIN")
                        UserSessionStore.clear(context)
                        _destination.value = "LOGIN"
                    }
                } else {
                    Log.d("MainViewModel", "No autenticado, navegando a LOGIN")
                    _destination.value = "LOGIN"
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error en checkAuth", e)
                // En caso de error crítico (ej. DB corrupta), intentamos ir a SETUP o LOGIN
                _destination.value = "LOGIN"
            }
        }
    }
    
    fun resetDestination() {
        _destination.value = null
    }
}
