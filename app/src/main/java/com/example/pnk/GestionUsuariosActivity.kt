package com.example.pnk

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class GestionUsuariosActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_gestion_usuarios)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.gestionRoot)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val btnIngresar = findViewById<Button>(R.id.btnIngresarUsuarios)
        val btnListar = findViewById<Button>(R.id.btnListarUsuarios)

        btnIngresar.setOnClickListener {
            startActivity(Intent(this, RegistroCrudActivity::class.java))
        }

        btnListar.setOnClickListener {
            startActivity(Intent(this, ListarUsuariosActivity::class.java))
        }
    }
}
