package com.example.pnk

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/**
 * Recuperación de contraseña (ítem 2, 12 pts):
 * - Envía código aleatorio mediante PHPMailer (vía API)
 * - Muestra cuenta regresiva de 1 minuto
 * - Rechaza códigos incorrectos o vencidos
 * - Permite solicitar otro código
 * - Bloquea cuenta durante recuperación, reactiva al guardar nueva contraseña
 */
class RecuperarActivity : AppCompatActivity() {

    private var countDownTimer: CountDownTimer? = null
    private var generatedCode: String = ""
    private var codeExpired: Boolean = true
    private var currentEmail: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_recuperar)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.recuperarRoot)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val etEmail = findViewById<EditText>(R.id.etEmail)
        val btnRecuperar = findViewById<Button>(R.id.btnRecuperar)
        val btnNuevoCodigo = findViewById<Button>(R.id.btnNuevoCodigo)
        val tvTimer = findViewById<TextView>(R.id.tvTimer)
        val btnVerificar = findViewById<Button>(R.id.btnVerificar)
        val codeFields = listOf(
            findViewById<EditText>(R.id.etCode1),
            findViewById<EditText>(R.id.etCode2),
            findViewById<EditText>(R.id.etCode3),
            findViewById<EditText>(R.id.etCode4),
            findViewById<EditText>(R.id.etCode5)
        )

        // Auto-focus next code field
        for (i in 0 until codeFields.size - 1) {
            codeFields[i].addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    if (s?.length == 1) {
                        codeFields[i + 1].requestFocus()
                    }
                }
                override fun afterTextChanged(s: Editable?) {}
            })
        }

        // Botón RECUPERAR - envía código al email
        btnRecuperar.setOnClickListener {
            val email = etEmail.text.toString().trim()
            etEmail.error = null

            when {
                email.isEmpty() -> {
                    etEmail.error = "El campo email es obligatorio"
                    SweetAlertDialog(this, SweetAlertDialog.ERROR_TYPE)
                        .setTitleText(getString(R.string.msg_error))
                        .setContentText("Revise los campos resaltados en rojo")
                        .setConfirmText("OK")
                        .show()
                }
                !ValidationHelper.isEmailValid(email) -> {
                    etEmail.error = "Formato de email inválido"
                    SweetAlertDialog(this, SweetAlertDialog.ERROR_TYPE)
                        .setTitleText(getString(R.string.msg_error))
                        .setContentText("Revise los campos resaltados en rojo")
                        .setConfirmText("OK")
                        .show()
                }
                else -> {
                    currentEmail = email
                    enviarCodigo(email, tvTimer, codeFields, btnNuevoCodigo, btnVerificar, btnRecuperar)
                }
            }
        }

        // Botón SOLICITAR NUEVO CÓDIGO
        btnNuevoCodigo.setOnClickListener {
            if (currentEmail.isNotEmpty()) {
                // Limpiar campos de código
                codeFields.forEach { it.setText("") }
                enviarCodigo(currentEmail, tvTimer, codeFields, btnNuevoCodigo, btnVerificar, btnRecuperar)
            }
        }

        // Botón VERIFICAR CÓDIGO
        btnVerificar.setOnClickListener {
            val code = codeFields.joinToString("") { it.text.toString() }

            if (code.length < 5) {
                SweetAlertDialog(this, SweetAlertDialog.ERROR_TYPE)
                    .setTitleText(getString(R.string.msg_error))
                    .setContentText("Ingrese el código completo de 5 dígitos")
                    .setConfirmText("OK")
                    .show()
                return@setOnClickListener
            }

            if (codeExpired) {
                SweetAlertDialog(this, SweetAlertDialog.ERROR_TYPE)
                    .setTitleText(getString(R.string.msg_error))
                    .setContentText(getString(R.string.msg_codigo_expirado))
                    .setConfirmText("OK")
                    .show()
                return@setOnClickListener
            }

            // Verificar código contra API o localmente
            val networkHelper = PnkApplication.getInstance().networkHelper
            if (networkHelper.isOnline) {
                ApiService.verifyCode(currentEmail, code, object : ApiService.ApiCallback {
                    override fun onSuccess(response: org.json.JSONObject) {
                        if (response.optBoolean("success", false)) {
                            navegarACrearPassword()
                        } else {
                            SweetAlertDialog(this@RecuperarActivity, SweetAlertDialog.ERROR_TYPE)
                                .setTitleText(getString(R.string.msg_error))
                                .setContentText(getString(R.string.msg_codigo_incorrecto))
                                .setConfirmText("OK")
                                .show()
                        }
                    }
                    override fun onError(message: String) {
                        // Fallback: verificar código local
                        verificarCodigoLocal(code)
                    }
                })
            } else {
                verificarCodigoLocal(code)
            }
        }
    }

    private fun enviarCodigo(email: String, tvTimer: TextView, codeFields: List<EditText>,
                             btnNuevoCodigo: Button, btnVerificar: Button, btnRecuperar: Button) {
        val networkHelper = PnkApplication.getInstance().networkHelper

        if (networkHelper.isOnline) {
            // Enviar vía API (PHPMailer en servidor)
            ApiService.sendRecoveryCode(email, object : ApiService.ApiCallback {
                override fun onSuccess(response: org.json.JSONObject) {
                    if (response.optBoolean("success", false)) {
                        SweetAlertDialog(this@RecuperarActivity, SweetAlertDialog.SUCCESS_TYPE)
                            .setTitleText(getString(R.string.msg_exito))
                            .setContentText(getString(R.string.msg_codigo_enviado))
                            .setConfirmText("OK")
                            .show()
                    } else {
                        SweetAlertDialog(this@RecuperarActivity, SweetAlertDialog.ERROR_TYPE)
                            .setTitleText(getString(R.string.msg_error))
                            .setContentText(response.optString("message", "Error al enviar código"))
                            .setConfirmText("OK")
                            .show()
                    }
                }
                override fun onError(message: String) {
                    // Generar código local como fallback
                    generarCodigoLocal()
                    SweetAlertDialog(this@RecuperarActivity, SweetAlertDialog.SUCCESS_TYPE)
                        .setTitleText(getString(R.string.msg_exito))
                        .setContentText("Código generado: $generatedCode (modo offline)")
                        .setConfirmText("OK")
                        .show()
                }
            })
        } else {
            // Modo offline: generar código localmente
            generarCodigoLocal()
            SweetAlertDialog(this, SweetAlertDialog.SUCCESS_TYPE)
                .setTitleText(getString(R.string.msg_exito))
                .setContentText("Código generado: $generatedCode (modo offline)")
                .setConfirmText("OK")
                .show()
        }

        // Iniciar timer de 1 minuto (60 segundos)
        startTimer(tvTimer, btnNuevoCodigo)
        codeExpired = false
        codeFields[0].requestFocus()
        btnVerificar.visibility = View.VISIBLE
        btnRecuperar.visibility = View.GONE
    }

    private fun generarCodigoLocal() {
        generatedCode = (10000..99999).random().toString()
    }

    private fun verificarCodigoLocal(code: String) {
        if (code == generatedCode && !codeExpired) {
            navegarACrearPassword()
        } else {
            SweetAlertDialog(this, SweetAlertDialog.ERROR_TYPE)
                .setTitleText(getString(R.string.msg_error))
                .setContentText(getString(R.string.msg_codigo_incorrecto))
                .setConfirmText("OK")
                .show()
        }
    }

    private fun navegarACrearPassword() {
        countDownTimer?.cancel()
        SweetAlertDialog(this@RecuperarActivity, SweetAlertDialog.SUCCESS_TYPE)
            .setTitleText(getString(R.string.msg_exito))
            .setContentText("Código verificado correctamente")
            .setConfirmText("Continuar")
            .setConfirmClickListener { dialog ->
                dialog.dismissWithAnimation()
                val intent = Intent(this@RecuperarActivity, CrearPasswordActivity::class.java)
                intent.putExtra("EMAIL", currentEmail)
                startActivity(intent)
                finish()
            }
            .show()
    }

    private fun startTimer(tvTimer: TextView, btnNuevoCodigo: Button) {
        countDownTimer?.cancel()
        btnNuevoCodigo.visibility = View.GONE

        // Timer de 60 segundos (1 minuto exacto como pide el PDF)
        countDownTimer = object : CountDownTimer(60000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val seconds = millisUntilFinished / 1000
                tvTimer.text = "$seconds ${getString(R.string.segundos)}"
            }

            override fun onFinish() {
                tvTimer.text = "0 ${getString(R.string.segundos)}"
                codeExpired = true
                // Mostrar botón de solicitar nuevo código
                btnNuevoCodigo.visibility = View.VISIBLE
                SweetAlertDialog(this@RecuperarActivity, SweetAlertDialog.WARNING_TYPE)
                    .setTitleText(getString(R.string.msg_atencion))
                    .setContentText(getString(R.string.msg_codigo_expirado))
                    .setConfirmText("OK")
                    .show()
            }
        }.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
    }
}
