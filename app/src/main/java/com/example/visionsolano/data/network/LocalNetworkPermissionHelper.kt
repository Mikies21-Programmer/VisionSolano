package com.example.visionsolano.data.network

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * Gestor del permiso de acceso a la red local (LAN) para Android 16/17 (Target SDK 37+).
 *
 * En Android 16 QPR / Android 17 (SDK 36/37+), las aplicaciones que acceden a hosts y
 * dispositivos en la LAN requieren el permiso en tiempo de ejecución:
 * android.permission.ACCESS_LOCAL_NETWORK
 */
object LocalNetworkPermissionHelper {
    const val PERMISSION_ACCESS_LOCAL_NETWORK = "android.permission.ACCESS_LOCAL_NETWORK"

    /**
     * Determina si la plataforma en ejecución requiere el permiso en tiempo de ejecución.
     * Activo para dispositivos ejecutando SDK >= 36 (Android 16 QPR / Android 17+).
     */
    fun isPermissionRequired(): Boolean {
        return Build.VERSION.SDK_INT >= 36
    }

    /**
     * Comprueba si el permiso ha sido otorgado por el usuario.
     * En versiones anteriores a SDK 36, el acceso a LAN está permitido por defecto en el manifiesto.
     */
    fun isPermissionGranted(context: Context): Boolean {
        if (!isPermissionRequired()) return true
        return try {
            ContextCompat.checkSelfPermission(
                context,
                PERMISSION_ACCESS_LOCAL_NETWORK
            ) == PackageManager.PERMISSION_GRANTED
        } catch (e: Exception) {
            false
        }
    }
}
