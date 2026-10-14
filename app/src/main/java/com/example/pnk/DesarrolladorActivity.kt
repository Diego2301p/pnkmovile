package com.example.pnk

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class DesarrolladorActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_desarrollador)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.desarrolladorRoot)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Social links
        findViewById<TextView?>(R.id.tvGitHub)?.setOnClickListener {
            openUrl("https://github.com")
        }
        findViewById<TextView?>(R.id.tvLinkedIn)?.setOnClickListener {
            openUrl("https://linkedin.com")
        }
        findViewById<TextView?>(R.id.tvPortfolio)?.setOnClickListener {
            openUrl("https://portfolio.dev")
        }
    }

    private fun openUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(intent)
    }
}
