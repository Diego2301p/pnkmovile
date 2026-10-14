package com.example.pnk

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/**
 * Modificar/Eliminar usuario (ítem 4, 12 pts).
 * Ahora usa USER_ID (database ID) en vez de array index.
 * Validaciones: campos obligatorios, solo letras en nombres, email válido y no duplicado.
 * Eliminación requiere confirmación previa con SweetAlert.
 * Operaciones sobre SQLite con sync pendiente para MySQL vía API.
 */
class ModificarUsuarioActivity : AppCompatActivity() {

    private var userId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_modificar_usuario)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.modificarRoot)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Recibir USER_ID (database ID)
        userId = intent.getIntExtra("USER_ID", -1)

        val etNombres = findViewById<EditText>(R.id.etNombres)
        val etApellidos = findViewById<EditText>(R.id.etApellidos)
        val etEmail = findViewById<EditText>(R.id.etEmail)

        val btnModificar = findViewById<Button>(R.id.btnModificar)
        val btnEliminar = findViewById<Button>(R.id.btnEliminar)
        val btnConfirmar = findViewById<Button>(R.id.btnConfirmar)
        val btnCancelar = findViewById<Button>(R.id.btnCancelar)

        val actionButtonsContainer = findViewById<LinearLayout>(R.id.actionButtonsContainer)
        val confirmButtonsContainer = findViewById<LinearLayout>(R.id.confirmButtonsContainer)

        // Load current user data from SQLite
        val currentUser = UserRepository.getUserById(userId)
        if (currentUser != null) {
            etNombres.setText(currentUser.nombre)
            etApellidos.setText(currentUser.apellido)
            etEmail.setText(currentUser.email)
        }

        btnModificar.setOnClickListener {
            // Enable fields and show confirm/cancel buttons
            etNombres.isEnabled = true
            etApellidos.isEnabled = true
            etEmail.isEnabled = true

            actionButtonsContainer.visibility = android.view.View.GONE
            confirmButtonsContainer.visibility = android.view.View.VISIBLE
        }

        btnCancelar.setOnClickListener {
            // Reset fields and disable
            etNombres.isEnabled = false
            etApellidos.isEnabled = false
            etEmail.isEnabled = false

            etNombres.error = null
            etApellidos.error = null
            etEmail.error = null

            val user = UserRepository.getUserById(userId)
            if (user != null) {
                etNombres.setText(user.nombre)
                etApellidos.setText(user.apellido)
                etEmail.setText(user.email)
            }

            actionButtonsContainer.visibility = android.view.View.VISIBLE
            confirmButtonsContainer.visibility = android.view.View.GONE
        }

        btnConfirmar.setOnClickListener {
            val nombres = etNombres.text.toString().trim()
            val apellidos = etApellidos.text.toString().trim()
            val email = etEmail.text.toString().trim()

            // Clear previous errors
            etNombres.error = null
            etApellidos.error = null
            etEmail.error = null

            var hasError = false

            // Nombres: obligatorio, solo letras y espacios (incluye tildes y ñ)
            if (nombres.isEmpty()) {
                etNombres.error = "El campo nombres es obligatorio"
                hasError = true
            } else if (!ValidationHelper.isOnlyLettersAndSpaces(nombres)) {
                etNombres.error = "Solo se permiten letras y espacios"
                hasError = true
            }

            // Apellidos: obligatorio, solo letras y espacios
            if (apellidos.isEmpty()) {
                etApellidos.error = "El campo apellidos es obligatorio"
                hasError = true
            } else if (!ValidationHelper.isOnlyLettersAndSpaces(apellidos)) {
                etApellidos.error = "Solo se permiten letras y espacios"
                hasError = true
            }

            // Email: obligatorio, formato válido, no duplicado en otro usuario
            if (email.isEmpty()) {
                etEmail.error = "El campo email es obligatorio"
                hasError = true
            } else if (!ValidationHelper.isEmailValid(email)) {
                etEmail.error = "Formato de email inválido"
                hasError = true
            } else if (UserRepository.emailExists(email, userId)) {
                etEmail.error = getString(R.string.msg_email_duplicado)
                hasError = true
            }

            if (hasError) {
                SweetAlertDialog(this, SweetAlertDialog.ERROR_TYPE)
                    .setTitleText(getString(R.string.msg_error))
                    .setContentText("Revise los campos resaltados en rojo")
                    .setConfirmText("OK")
                    .show()
            } else {
                SweetAlertDialog(this, SweetAlertDialog.WARNING_TYPE)
                    .setTitleText("Confirmar")
                    .setContentText("¿Está seguro que desea modificar estos datos?")
                    .setConfirmText("Sí, modificar")
                    .setCancelText("No")
                    .setConfirmClickListener { dialog ->
                        dialog.dismissWithAnimation()

                        // Actualizar en SQLite (marcado como pending_update para sync)
                        UserRepository.updateUser(userId, nombres, apellidos, email)

                        // Si hay conexión, sincronizar inmediatamente
                        val networkHelper = PnkApplication.getInstance().networkHelper
                        val user = UserRepository.getUserById(userId)

                        if (networkHelper.isOnline && user?.serverId != null) {
                            ApiService.updateUser(user.serverId!!, nombres, apellidos, email, object : ApiService.ApiCallback {
                                override fun onSuccess(response: org.json.JSONObject) {
                                    if (response.optBoolean("success", false)) {
                                        val dbHelper = DatabaseHelper.getInstance(this@ModificarUsuarioActivity)
                                        dbHelper.markAsSynced(userId, user.serverId)
                                    }
                                    mostrarExitoModificacion()
                                }
                                override fun onError(message: String) {
                                    // Queda pendiente para sync automático
                                    mostrarExitoModificacion()
                                }
                            })
                        } else {
                            mostrarExitoModificacion()
                        }
                    }
                    .setCancelClickListener { dialog ->
                        dialog.dismissWithAnimation()
                    }
                    .show()
            }
        }

        // Eliminación requiere confirmación previa con SweetAlert
        btnEliminar.setOnClickListener {
            SweetAlertDialog(this, SweetAlertDialog.WARNING_TYPE)
                .setTitleText("Confirmar")
                .setContentText("¿Está seguro que desea eliminar este usuario?")
                .setConfirmText("Sí, eliminar")
                .setCancelText("No")
                .setConfirmClickListener { dialog ->
                    dialog.dismissWithAnimation()

                    val user = UserRepository.getUserById(userId)
                    val networkHelper = PnkApplication.getInstance().networkHelper

                    if (networkHelper.isOnline && user?.serverId != null) {
                        ApiService.deleteUser(user.serverId!!, object : ApiService.ApiCallback {
                            override fun onSuccess(response: org.json.JSONObject) {
                                if (response.optBoolean("success", false)) {
                                    UserRepository.deleteUserPermanently(userId)
                                } else {
                                    UserRepository.removeUser(userId)
                                }
                                mostrarExitoEliminacion()
                            }
                            override fun onError(message: String) {
                                UserRepository.removeUser(userId)
                                mostrarExitoEliminacion()
                            }
                        })
                    } else {
                        UserRepository.removeUser(userId)
                        mostrarExitoEliminacion()
                    }
                }
                .setCancelClickListener { dialog ->
                    dialog.dismissWithAnimation()
                }
                .show()
        }
    }

    private fun mostrarExitoModificacion() {
        SweetAlertDialog(this, SweetAlertDialog.SUCCESS_TYPE)
            .setTitleText(getString(R.string.msg_exito))
            .setContentText("Datos modificados correctamente")
            .setConfirmText("OK")
            .setConfirmClickListener { d2 ->
                d2.dismissWithAnimation()
                finish()
            }
            .show()
    }

    private fun mostrarExitoEliminacion() {
        SweetAlertDialog(this, SweetAlertDialog.SUCCESS_TYPE)
            .setTitleText(getString(R.string.msg_exito))
            .setContentText("Usuario eliminado correctamente")
            .setConfirmText("OK")
            .setConfirmClickListener { d2 ->
                d2.dismissWithAnimation()
                finish()
            }
            .show()
    }
}
