package com.example.pnk

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject

/**
 * Login con validación contra MySQL mediante API (ítem 1, 8 pts).
 * Si no hay conexión, intenta validar contra SQLite local.
 * Rechaza credenciales incorrectas y cuentas bloqueadas.
 */
class LoginActivity : AppCompatActivity() {

    private lateinit var datos: RequestQueue

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.loginRoot)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        datos = Volley.newRequestQueue(this)

        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnIngresar = findViewById<Button>(R.id.btnIngresar)
        val tvRegistrarse = findViewById<android.widget.TextView>(R.id.tvRegistrarse)
        val tvOlvidePassword = findViewById<android.widget.TextView>(R.id.tvOlvidePassword)

        btnIngresar.setOnClickListener {
            btnIngresar.isEnabled = false
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            // Clear previous errors
            etEmail.error = null
            etPassword.error = null

            var hasError = false

            if (email.isEmpty()) {
                etEmail.error = "El campo email es obligatorio"
                hasError = true
            } else if (!ValidationHelper.isEmailValid(email)) {
                etEmail.error = "Formato de email inválido"
                hasError = true
            }

            if (password.isEmpty()) {
                etPassword.error = "El campo contraseña es obligatorio"
                hasError = true
            } else if (!ValidationHelper.isPasswordStrong(password)) {
                etPassword.error = "Mínimo 8 caracteres, 1 mayúscula, 1 minúscula, 1 número y 1 carácter especial"
                hasError = true
            }

            if (hasError) {
                SweetAlertDialog(this@LoginActivity, SweetAlertDialog.ERROR_TYPE)
                    .setTitleText(getString(R.string.msg_error))
                    .setContentText("Revise los campos resaltados en rojo")
                    .setConfirmText("OK")
                    .show()
                btnIngresar.isEnabled = true
            } else {
                val networkHelper = PnkApplication.getInstance().networkHelper
                if (networkHelper.isOnline) {
                    // Consultar API con Volley (como se vio en clases)
                    consultarLogin(email, password, btnIngresar)
                } else {
                    // Modo offline: validar contra SQLite local
                    loginOffline(email, password, btnIngresar)
                }
            }
        }

        tvRegistrarse.setOnClickListener {
            startActivity(Intent(this, RegistroActivity::class.java))
        }

        tvOlvidePassword.setOnClickListener {
            startActivity(Intent(this, RecuperarActivity::class.java))
        }
    }

    /**
     * Consulta la API para validar credenciales contra MySQL.
     * Usa Volley con JsonObjectRequest (patrón visto en clases).
     */
    private fun consultarLogin(email: String, password: String, btnIngresar: Button) {
        val url = "${ApiService.BASE_URL}/login.php?usu=$email&pass=$password"

        val request = JsonObjectRequest(
            Request.Method.GET, url, null,
            { response ->
                try {
                    val estado = response.optString("estado", "0")
                    val success = response.optBoolean("success", estado != "0")
                    val message = response.optString("message", "")

                    if (success || estado != "0") {
                        // Guardar credenciales en SQLite para login offline
                        try {
                            val userObj = response.optJSONObject("user")
                            val nombre = userObj?.optString("nombre", "") ?: ""
                            val apellido = userObj?.optString("apellido", "") ?: ""
                            val userId = userObj?.optInt("id", 0) ?: 0
                            val userEmail = userObj?.optString("email", email) ?: email

                            // Verificar si ya existe en SQLite
                            val existingUsers = UserRepository.users
                            val existing = existingUsers.find { it.email.equals(userEmail, ignoreCase = true) }
                            if (existing == null) {
                                UserRepository.addUser(User(
                                    nombre = nombre,
                                    apellido = apellido,
                                    email = userEmail,
                                    password = password,
                                    serverId = userId,
                                    syncStatus = DatabaseHelper.SYNC_SYNCED
                                ))
                            } else {
                                // Actualizar password local por si cambió
                                val dbHelper = PnkApplication.getInstance().dbHelper
                                val values = android.content.ContentValues().apply {
                                    put(DatabaseHelper.COL_PASSWORD, password)
                                }
                                dbHelper.writableDatabase.update(
                                    DatabaseHelper.TABLE_USUARIOS, values,
                                    "${DatabaseHelper.COL_ID} = ?", arrayOf(existing.id.toString())
                                )
                            }
                        } catch (_: Exception) { /* No bloquear login si falla el cache */ }

                        SweetAlertDialog(this@LoginActivity, SweetAlertDialog.SUCCESS_TYPE)
                            .setTitleText(getString(R.string.msg_exito))
                            .setContentText(getString(R.string.msg_login_exitoso))
                            .setConfirmText("OK")
                            .setConfirmClickListener { dialog ->
                                dialog.dismissWithAnimation()
                                btnIngresar.isEnabled = true
                                startActivity(Intent(this@LoginActivity, MenuActivity::class.java))
                            }
                            .show()
                    } else {
                        val errorMsg = when {
                            message.contains("bloqueada", ignoreCase = true) -> getString(R.string.msg_cuenta_bloqueada)
                            message.isNotEmpty() -> message
                            else -> getString(R.string.msg_credenciales_incorrectas)
                        }
                        SweetAlertDialog(this@LoginActivity, SweetAlertDialog.ERROR_TYPE)
                            .setTitleText(getString(R.string.msg_error))
                            .setContentText(errorMsg)
                            .setConfirmText("OK")
                            .show()
                        btnIngresar.isEnabled = true
                    }
                } catch (e: Exception) {
                    SweetAlertDialog(this@LoginActivity, SweetAlertDialog.ERROR_TYPE)
                        .setTitleText(getString(R.string.msg_error))
                        .setContentText("Error al procesar respuesta: ${e.message}")
                        .setConfirmText("OK")
                        .show()
                    btnIngresar.isEnabled = true
                }
            },
            { error ->
                // Si falla la API, intentar offline
                loginOffline(email, password, btnIngresar)
            }
        )
        datos.add(request)
    }

    /**
     * Login offline: valida contra la base de datos SQLite local.
     */
    private fun loginOffline(email: String, password: String, btnIngresar: Button) {
        val users = UserRepository.users
        val user = users.find { it.email.equals(email, ignoreCase = true) && it.password == password }

        if (user != null) {
            SweetAlertDialog(this@LoginActivity, SweetAlertDialog.SUCCESS_TYPE)
                .setTitleText(getString(R.string.msg_exito))
                .setContentText("${getString(R.string.msg_login_exitoso)} (Modo offline)")
                .setConfirmText("OK")
                .setConfirmClickListener { dialog ->
                    dialog.dismissWithAnimation()
                    btnIngresar.isEnabled = true
                    startActivity(Intent(this@LoginActivity, MenuActivity::class.java))
                }
                .show()
        } else {
            SweetAlertDialog(this@LoginActivity, SweetAlertDialog.ERROR_TYPE)
                .setTitleText(getString(R.string.msg_error))
                .setContentText(getString(R.string.msg_credenciales_incorrectas))
                .setConfirmText("OK")
                .show()
            btnIngresar.isEnabled = true
        }
    }
}
