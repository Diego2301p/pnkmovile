package com.example.pnk

import android.os.Handler
import android.os.Looper
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * Servicio de API HTTP para comunicación con el servidor PHP/MySQL.
 *
 * IMPORTANTE: El estudiante debe configurar BASE_URL con la URL de su API.
 * Ejemplo: "https://tu-servidor-aws.com/api"
 */
object ApiService {

    // ========== CONFIGURAR ESTA URL CON TU SERVIDOR ==========
    var BASE_URL = "http://34.203.5.216/api"
    // URL de la API IoT de PNK
    var SENSOR_API_URL = "https://www.pnk.cl/muestra_datos.php"
    // =========================================================

    private val mainHandler = Handler(Looper.getMainLooper())

    interface ApiCallback {
        fun onSuccess(response: JSONObject)
        fun onError(message: String)
    }

    // ==================== Auth ====================

    fun login(email: String, password: String, callback: ApiCallback) {
        val body = JSONObject().apply {
            put("email", email)
            put("password", password)
        }
        postAsync("$BASE_URL/login.php", body, callback)
    }

    fun sendRecoveryCode(email: String, callback: ApiCallback) {
        val body = JSONObject().apply {
            put("email", email)
        }
        postAsync("$BASE_URL/recuperar.php", body, callback)
    }

    fun verifyCode(email: String, code: String, callback: ApiCallback) {
        val body = JSONObject().apply {
            put("email", email)
            put("codigo", code)
        }
        postAsync("$BASE_URL/verificar_codigo.php", body, callback)
    }

    fun changePassword(email: String, newPassword: String, callback: ApiCallback) {
        val body = JSONObject().apply {
            put("email", email)
            put("password", newPassword)
        }
        postAsync("$BASE_URL/cambiar_password.php", body, callback)
    }

    // ==================== CRUD Usuarios ====================

    fun registerUser(nombre: String, apellido: String, email: String, username: String, password: String, callback: ApiCallback) {
        val body = JSONObject().apply {
            put("nombre", nombre)
            put("apellido", apellido)
            put("email", email)
            put("username", username)
            put("password", password)
        }
        postAsync("$BASE_URL/registro.php", body, callback)
    }

    fun getUsers(callback: ApiCallback) {
        getAsync("$BASE_URL/usuarios.php", callback)
    }

    fun updateUser(serverId: Int, nombre: String, apellido: String, email: String, callback: ApiCallback) {
        val body = JSONObject().apply {
            put("id", serverId)
            put("nombre", nombre)
            put("apellido", apellido)
            put("email", email)
        }
        postAsync("$BASE_URL/actualizar.php", body, callback)
    }

    fun deleteUser(serverId: Int, callback: ApiCallback) {
        val body = JSONObject().apply {
            put("id", serverId)
        }
        postAsync("$BASE_URL/eliminar.php", body, callback)
    }

    // ==================== Sensores IoT ====================

    fun getSensorData(callback: ApiCallback) {
        getAsync(SENSOR_API_URL, callback)
    }

    // ==================== Sync ====================

    fun syncInsert(user: User, callback: ApiCallback) {
        registerUser(user.nombre, user.apellido, user.email, user.username, user.password, callback)
    }

    fun syncUpdate(user: User, callback: ApiCallback) {
        val serverId = user.serverId ?: return callback.onError("Sin server_id")
        updateUser(serverId, user.nombre, user.apellido, user.email, callback)
    }

    fun syncDelete(user: User, callback: ApiCallback) {
        val serverId = user.serverId ?: return callback.onError("Sin server_id")
        deleteUser(serverId, callback)
    }

    // ==================== HTTP Helpers ====================

    private fun postAsync(urlStr: String, body: JSONObject, callback: ApiCallback) {
        Thread {
            try {
                val result = httpPost(urlStr, body)
                mainHandler.post { callback.onSuccess(result) }
            } catch (e: Exception) {
                val msg = e.message ?: "Error de conexión"
                mainHandler.post { callback.onError(msg) }
            }
        }.start()
    }

    private fun getAsync(urlStr: String, callback: ApiCallback) {
        Thread {
            try {
                val result = httpGet(urlStr)
                mainHandler.post { callback.onSuccess(result) }
            } catch (e: Exception) {
                val msg = e.message ?: "Error de conexión"
                mainHandler.post { callback.onError(msg) }
            }
        }.start()
    }

    private fun httpPost(urlStr: String, body: JSONObject): JSONObject {
        val url = URL(urlStr)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
        conn.setRequestProperty("Accept", "application/json")
        conn.doOutput = true
        conn.connectTimeout = 15000
        conn.readTimeout = 15000

        try {
            val writer = OutputStreamWriter(conn.outputStream, "UTF-8")
            writer.write(body.toString())
            writer.flush()
            writer.close()

            val responseCode = conn.responseCode
            val inputStream = if (responseCode in 200..299) conn.inputStream else conn.errorStream
            val reader = BufferedReader(InputStreamReader(inputStream, "UTF-8"))
            val response = reader.readText()
            reader.close()

            return try {
                JSONObject(response)
            } catch (e: Exception) {
                JSONObject().apply {
                    put("success", responseCode in 200..299)
                    put("raw", response)
                }
            }
        } finally {
            conn.disconnect()
        }
    }

    private fun httpGet(urlStr: String): JSONObject {
        val url = URL(urlStr)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.setRequestProperty("Accept", "application/json")
        conn.connectTimeout = 15000
        conn.readTimeout = 15000

        try {
            val responseCode = conn.responseCode
            val inputStream = if (responseCode in 200..299) conn.inputStream else conn.errorStream
            val reader = BufferedReader(InputStreamReader(inputStream, "UTF-8"))
            val response = reader.readText()
            reader.close()

            return try {
                JSONObject(response)
            } catch (e: Exception) {
                // If the response is a JSON array, wrap it
                try {
                    val arr = JSONArray(response)
                    JSONObject().apply {
                        put("success", true)
                        put("data", arr)
                    }
                } catch (e2: Exception) {
                    JSONObject().apply {
                        put("success", responseCode in 200..299)
                        put("raw", response)
                    }
                }
            }
        } finally {
            conn.disconnect()
        }
    }
}
