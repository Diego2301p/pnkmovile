package com.example.pnk

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Menú principal con:
 * - 3 opciones visibles (CRUD, Sensores, Desarrollador)
 * - Reloj en tiempo real
 * - Indicador de estado de conexión y cambios pendientes
 */
class MenuActivity : AppCompatActivity() {

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var tvFechaHora: TextView
    private lateinit var tvConexion: TextView
    private lateinit var tvPendientes: TextView

    private val updateTimeRunnable = object : Runnable {
        override fun run() {
            val sdf = SimpleDateFormat("dd-MM-yyyy, HH:mm:ss", Locale.getDefault())
            tvFechaHora.text = "Fecha/Hora:${sdf.format(Date())}"
            updateConnectionStatus()
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_menu)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.menuRoot)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        tvFechaHora = findViewById(R.id.tvFechaHora)
        tvConexion = findViewById(R.id.tvConexion)
        tvPendientes = findViewById(R.id.tvPendientes)

        val cardCrud = findViewById<LinearLayout>(R.id.cardCrud)
        val cardSensor = findViewById<LinearLayout>(R.id.cardSensor)
        val cardDesarrollador = findViewById<LinearLayout>(R.id.cardDesarrollador)

        cardCrud.setOnClickListener {
            startActivity(Intent(this, GestionUsuariosActivity::class.java))
        }

        cardSensor.setOnClickListener {
            startActivity(Intent(this, SensoresActivity::class.java))
        }

        cardDesarrollador.setOnClickListener {
            startActivity(Intent(this, DesarrolladorActivity::class.java))
        }

        // Start real-time clock
        handler.post(updateTimeRunnable)
    }

    override fun onResume() {
        super.onResume()
        updateConnectionStatus()
    }

    /**
     * Actualiza el indicador de conexión y cambios pendientes.
     * Informa al usuario si está sin conexión o si hay cambios pendientes.
     */
    private fun updateConnectionStatus() {
        try {
            val networkHelper = PnkApplication.getInstance().networkHelper
            val pendingCount = UserRepository.getPendingCount()

            if (networkHelper.isOnline) {
                tvConexion.text = getString(R.string.modo_online)
                tvConexion.setTextColor(Color.parseColor("#4CAF50"))
            } else {
                tvConexion.text = getString(R.string.modo_offline)
                tvConexion.setTextColor(Color.parseColor("#D32F2F"))
            }

            if (pendingCount > 0) {
                tvPendientes.text = "$pendingCount cambio(s) pendiente(s) de sincronización"
                tvPendientes.setTextColor(Color.parseColor("#FF6D00"))
                tvPendientes.visibility = android.view.View.VISIBLE
            } else {
                tvPendientes.visibility = android.view.View.GONE
            }
        } catch (_: Exception) {
            // PnkApplication might not be ready yet
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(updateTimeRunnable)
    }
}
