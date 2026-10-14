package com.example.pnk

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/**
 * Registro CRUD (desde Gestión de Usuarios).
 * Verifica duplicados de email. Una vez registrado, notifica y vuelve al menú de gestión (imagen N°7).
 */
class RegistroCrudActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_registro_crud)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.registroCrudRoot)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val etNombres = findViewById<EditText>(R.id.etNombres)
        val etApellidos = findViewById<EditText>(R.id.etApellidos)
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val etConfirm = findViewById<EditText>(R.id.etConfirmPassword)
        val btnRegistrar = findViewById<Button>(R.id.btnRegistrar)

        btnRegistrar.setOnClickListener {
            val nombres = etNombres.text.toString().trim()
            val apellidos = etApellidos.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString()
            val confirm = etConfirm.text.toString()

            // Clear all previous errors
            etNombres.error = null
            etApellidos.error = null
            etEmail.error = null
            etPassword.error = null
            etConfirm.error = null

            var hasError = false

            if (nombres.isEmpty()) {
                etNombres.error = "El campo nombres es obligatorio"
                hasError = true
            } else if (!ValidationHelper.isOnlyLettersAndSpaces(nombres)) {
                etNombres.error = "Solo se permiten letras y espacios"
                hasError = true
            }

            if (apellidos.isEmpty()) {
                etApellidos.error = "El campo apellidos es obligatorio"
                hasError = true
            } else if (!ValidationHelper.isOnlyLettersAndSpaces(apellidos)) {
                etApellidos.error = "Solo se permiten letras y espacios"
                hasError = true
            }

            if (email.isEmpty()) {
                etEmail.error = "El campo email es obligatorio"
                hasError = true
            } else if (!ValidationHelper.isEmailValid(email)) {
                etEmail.error = "Formato de email inválido"
                hasError = true
            } else if (UserRepository.emailExists(email)) {
                etEmail.error = getString(R.string.msg_email_duplicado)
                hasError = true
            }

            if (password.isEmpty()) {
                etPassword.error = "El campo contraseña es obligatorio"
                hasError = true
            } else if (!ValidationHelper.isPasswordStrong(password)) {
                etPassword.error = "Mínimo 8 caracteres, 1 mayúscula, 1 minúscula, 1 número y 1 carácter especial"
                hasError = true
            }

            if (confirm.isEmpty()) {
                etConfirm.error = "Debe repetir la contraseña"
                hasError = true
            } else if (!ValidationHelper.passwordsMatch(password, confirm)) {
                etConfirm.error = "Las contraseñas no coinciden"
                hasError = true
            }

            if (hasError) {
                SweetAlertDialog(this, SweetAlertDialog.ERROR_TYPE)
                    .setTitleText(getString(R.string.msg_error))
                    .setContentText("Revise los campos resaltados en rojo")
                    .setConfirmText("OK")
                    .show()
            } else {
                val networkHelper = PnkApplication.getInstance().networkHelper
                val user = User(nombres, apellidos, email, password, syncStatus = DatabaseHelper.SYNC_PENDING_INSERT)

                if (networkHelper.isOnline) {
                    ApiService.registerUser(nombres, apellidos, email, email, password, object : ApiService.ApiCallback {
                        override fun onSuccess(response: org.json.JSONObject) {
                            if (response.optBoolean("success", false)) {
                                val serverId = response.optInt("id", 0)
                                user.serverId = if (serverId > 0) serverId else null
                                user.syncStatus = DatabaseHelper.SYNC_SYNCED
                                UserRepository.addUser(user)
                                mostrarExitoYVolver()
                            } else {
                                val msg = response.optString("message", "Error al registrar")
                                SweetAlertDialog(this@RegistroCrudActivity, SweetAlertDialog.ERROR_TYPE)
                                    .setTitleText(getString(R.string.msg_error))
                                    .setContentText(msg)
                                    .setConfirmText("OK")
                                    .show()
                            }
                        }
                        override fun onError(message: String) {
                            UserRepository.addUser(user)
                            mostrarExitoYVolver()
                        }
                    })
                } else {
                    UserRepository.addUser(user)
                    mostrarExitoYVolver()
                }
            }
        }
    }

    /**
     * Una vez registrado, notifica con mensaje y vuelve al menú de gestión (imagen N°7).
     */
    private fun mostrarExitoYVolver() {
        SweetAlertDialog(this, SweetAlertDialog.SUCCESS_TYPE)
            .setTitleText(getString(R.string.msg_exito))
            .setContentText(getString(R.string.msg_registro_exitoso))
            .setConfirmText("OK")
            .setConfirmClickListener { dialog ->
                dialog.dismissWithAnimation()
                val intent = Intent(this, GestionUsuariosActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                finish()
            }
            .show()
    }
}
