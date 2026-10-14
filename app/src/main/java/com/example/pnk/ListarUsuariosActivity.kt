package com.example.pnk

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.text.Normalizer

/**
 * Lista usuarios desde SQLite (y MySQL vía API cuando está online).
 * Filtro por nombres/apellidos insensible a mayúsculas y acentos.
 * Al seleccionar un usuario, abre ModificarUsuarioActivity con el USER_ID (database id).
 */
class ListarUsuariosActivity : AppCompatActivity() {

    private lateinit var adapter: ArrayAdapter<String>
    private var displayList = mutableListOf<String>()
    private var userIds = mutableListOf<Int>() // Database IDs for each displayed item

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_listar_usuarios)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.listarRoot)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val etBuscar = findViewById<EditText>(R.id.etBuscar)
        val listView = findViewById<ListView>(R.id.listViewUsuarios)

        // Custom adapter with white text for dark theme
        adapter = object : ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, displayList) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent)
                val textView = view.findViewById<TextView>(android.R.id.text1)
                textView.setTextColor(resources.getColor(R.color.text_primary, theme))
                textView.textSize = 15f
                return view
            }
        }
        listView.adapter = adapter

        loadUsers("")

        etBuscar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                loadUsers(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Al seleccionar un usuario, abrir ModificarUsuarioActivity con USER_ID (database id)
        listView.setOnItemClickListener { _, _, position, _ ->
            if (position < userIds.size) {
                val userId = userIds[position]
                val intent = Intent(this, ModificarUsuarioActivity::class.java)
                intent.putExtra("USER_ID", userId)
                startActivity(intent)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val etBuscar = findViewById<EditText>(R.id.etBuscar)
        loadUsers(etBuscar.text.toString())
    }

    private fun loadUsers(query: String) {
        displayList.clear()
        userIds.clear()
        val normalizedQuery = removeAccents(query.lowercase())

        val users = UserRepository.users
        for (user in users) {
            val fullName = "${user.nombre} ${user.apellido}"
            val normalizedName = removeAccents(fullName.lowercase())

            if (normalizedQuery.isEmpty() || normalizedName.contains(normalizedQuery)) {
                val emailPart = if (user.email.isNotBlank()) " - ${user.email}" else ""
                displayList.add("${user.nombre} ${user.apellido}$emailPart")
                userIds.add(user.id)
            }
        }
        adapter.notifyDataSetChanged()
    }

    /**
     * Removes accents/diacritics for case-insensitive, accent-insensitive search.
     * Handles ñ, tildes, etc.
     */
    private fun removeAccents(text: String): String {
        val normalized = Normalizer.normalize(text, Normalizer.Form.NFD)
        return normalized.replace(Regex("[\\p{InCombiningDiacriticalMarks}]"), "")
    }
}
