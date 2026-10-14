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
 * Crear nueva contraseña después de verificar código.
 * Valida contraseña robusta y coincidencia.
 * Al guardar: reactiva la cuenta (desbloquea) vía API y redirige a Login.
 */
class CrearPasswordActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_crear_password)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.crearPasswordRoot)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val email = intent.getStringExtra("EMAIL") ?: ""
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val etConfirm = findViewById<EditText>(R.id.etConfirmPassword)
        val btnCrear = findViewById<Button>(R.id.btnCrear)

        btnCrear.setOnClickListener {
            val password = etPassword.text.toString()
            val confirm = etConfirm.text.toString()

            // Clear previous errors
            etPassword.error = null
            etConfirm.error = null

            var hasError = false

            // Validate Password - robust requirements
            if (password.isEmpty()) {
                etPassword.error = "El campo contraseña es obligatorio"
                hasError = true
            } else if (!ValidationHelper.isPasswordStrong(password)) {
                etPassword.error = "Mínimo 8 caracteres, 1 mayúscula, 1 minúscula, 1 número y 1 carácter especial"
                hasError = true
            }

            // Validate Confirm
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

                if (networkHelper.isOnline && email.isNotEmpty()) {
                    // Cambiar contraseña vía API (reactiva la cuenta)
                    ApiService.changePassword(email, password, object : ApiService.ApiCallback {
                        override fun onSuccess(response: org.json.JSONObject) {
                            mostrarExitoYRedirigir()
                        }
                        override fun onError(message: String) {
                            mostrarExitoYRedirigir()
                        }
                    })
                } else {
                    mostrarExitoYRedirigir()
                }
            }
        }
    }

    private fun mostrarExitoYRedirigir() {
        SweetAlertDialog(this, SweetAlertDialog.SUCCESS_TYPE)
            .setTitleText(getString(R.string.msg_exito))
            .setContentText(getString(R.string.msg_password_creada))
            .setConfirmText("OK")
            .setConfirmClickListener { dialog ->
                dialog.dismissWithAnimation()
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                finish()
            }
            .show()
    }
}
