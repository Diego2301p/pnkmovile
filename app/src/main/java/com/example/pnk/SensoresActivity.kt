package com.example.pnk

import android.content.Context
import android.graphics.Color
import android.graphics.PorterDuff
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Sensor data activity that:
 * - Consumes IoT API from www.pnk.cl (ítem 5, 8 pts)
 * - Toggles real phone flashlight (ítem 6, 4 pts)
 * - Toggles lightbulb image visually only (ítem 6)
 * - Auto-refreshes data every 5 seconds using Handler
 */
class SensoresActivity : AppCompatActivity() {

    private lateinit var tvHumedad: TextView
    private lateinit var tvTemperatura: TextView
    private lateinit var tvFechaHora: TextView
    private lateinit var tvEstado: TextView
    private lateinit var imgLinterna: ImageView
    private lateinit var imgAmpolleta: ImageView
    private lateinit var imgTemperatura: ImageView
    private lateinit var datos: RequestQueue

    private val mHandler = Handler(Looper.getMainLooper())
    private var isFlashlightOn = false
    private var isLightbulbOn = false

    // Auto-refresh: updates date/time and fetches sensor data every 5 seconds
    private val refreshRunnable = object : Runnable {
        override fun run() {
            val sdf = SimpleDateFormat("dd MMMM yyyy, hh:mm:ss a", Locale.getDefault())
            tvFechaHora.text = sdf.format(Date())
            obtenerDatos()
            mHandler.postDelayed(this, 5000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_sensores)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.sensoresRoot)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Casteo de elementos
        tvHumedad = findViewById(R.id.tvHumedadValor)
        tvTemperatura = findViewById(R.id.tvTemperaturaValor)
        tvFechaHora = findViewById(R.id.tvFechaHoraSensor)
        tvEstado = findViewById(R.id.tvEstadoConexion)
        imgLinterna = findViewById(R.id.imgLinterna)
        imgAmpolleta = findViewById(R.id.imgAmpolleta)
        imgTemperatura = findViewById(R.id.imgTemperatura)

        // Inicializar Volley
        datos = Volley.newRequestQueue(this)

        // Linterna: al presionar su imagen, encender/apagar la linterna REAL del teléfono
        imgLinterna.setOnClickListener {
            toggleFlashlight()
        }

        // Ampolleta: al presionar, alternar imagen (SOLO VISUAL, no activa dispositivos)
        imgAmpolleta.setOnClickListener {
            toggleLightbulb()
        }

        // Iniciar auto-refresh con Handler (como se vio en clases)
        mHandler.post(refreshRunnable)
    }

    /**
     * Obtiene datos del WebService IoT usando Volley.
     * URL: https://www.pnk.cl/muestra_datos.php
     * Retorna JSON con "temperatura" y "humedad"
     */
    private fun obtenerDatos() {
        val url = "https://www.pnk.cl/muestra_datos.php"

        val request = JsonObjectRequest(
            Request.Method.GET, url, null,
            { response ->
                try {
                    val temp = response.getString("temperatura")
                    val hum = response.getString("humedad")

                    tvTemperatura.text = "${temp}°C"
                    tvHumedad.text = "${hum}%"
                    tvEstado.text = getString(R.string.datos_actualizados)
                    tvEstado.setTextColor(Color.parseColor("#4CAF50"))

                    // Cambiar indicador de temperatura según rango
                    val tempVal = temp.toFloatOrNull() ?: 0f
                    cambiarImagenTemperatura(tempVal)

                } catch (e: Exception) {
                    tvEstado.text = "Error al leer datos: ${e.message}"
                    tvEstado.setTextColor(Color.parseColor("#FF6D00"))
                }
            },
            { error ->
                tvEstado.text = "Error de conexión: ${error.message ?: "Sin respuesta"}"
                tvEstado.setTextColor(Color.parseColor("#D32F2F"))
            }
        )
        datos.add(request)
    }

    /**
     * Cambia el color del ícono de temperatura según el rango.
     */
    private fun cambiarImagenTemperatura(valor: Float) {
        when {
            valor >= 30 -> imgTemperatura.setColorFilter(Color.parseColor("#FF1744"), PorterDuff.Mode.SRC_IN)
            valor >= 20 -> imgTemperatura.setColorFilter(Color.parseColor("#FF6D00"), PorterDuff.Mode.SRC_IN)
            else -> imgTemperatura.setColorFilter(Color.parseColor("#42A5F5"), PorterDuff.Mode.SRC_IN)
        }
    }

    /**
     * Enciende/apaga la linterna REAL del teléfono usando CameraManager.
     * Actualiza la imagen para representar el estado real.
     */
    private fun toggleFlashlight() {
        try {
            val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId = cameraManager.cameraIdList[0]
            isFlashlightOn = !isFlashlightOn
            cameraManager.setTorchMode(cameraId, isFlashlightOn)

            if (isFlashlightOn) {
                imgLinterna.setColorFilter(Color.parseColor("#FFEB3B"), PorterDuff.Mode.SRC_IN)
                imgLinterna.alpha = 1.0f
            } else {
                imgLinterna.clearColorFilter()
                imgLinterna.alpha = 0.7f
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error al controlar la linterna: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Alterna la imagen de la ampolleta entre encendida y apagada.
     * Funcionalidad ÚNICAMENTE VISUAL (no activa la linterna ni dispositivos físicos).
     */
    private fun toggleLightbulb() {
        isLightbulbOn = !isLightbulbOn
        if (isLightbulbOn) {
            imgAmpolleta.setColorFilter(Color.parseColor("#FFEB3B"), PorterDuff.Mode.SRC_IN)
            imgAmpolleta.alpha = 1.0f
        } else {
            imgAmpolleta.clearColorFilter()
            imgAmpolleta.alpha = 0.5f
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mHandler.removeCallbacks(refreshRunnable)
        // Apagar linterna al salir si está encendida
        if (isFlashlightOn) {
            try {
                val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
                val cameraId = cameraManager.cameraIdList[0]
                cameraManager.setTorchMode(cameraId, false)
            } catch (_: Exception) { }
        }
    }
}
