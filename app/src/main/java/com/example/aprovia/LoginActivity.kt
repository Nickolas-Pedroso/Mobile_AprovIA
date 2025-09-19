package com.example.aprovia

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        // Ajuste de padding para as barras de sistema
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        // Tela de cadastro
        val cadastro = findViewById<TextView>(R.id.txtCadastro)
        cadastro.setOnClickListener {
            val intent = Intent(this, CadastroActivity::class.java)
            startActivity(intent)
        }

        // Verifica se o usuário já está logado
        val prefs = getSharedPreferences("APP_PREFS", MODE_PRIVATE)
        if (prefs.getBoolean("LOGGED_IN", false)) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        // Botão de login
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val edtUser = findViewById<EditText>(R.id.edtUser)
        val edtPass = findViewById<EditText>(R.id.edtPass)

        btnLogin.setOnClickListener {
            val user = edtUser.text.toString()
            val pass = edtPass.text.toString()

            // Simulação de login (substitua com sua lógica real)
            if (user == "admin" && pass == "1234") {
                prefs.edit().putBoolean("LOGGED_IN", true).apply()
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish() // fecha a LoginActivity
            } else {
                Toast.makeText(this, "Usuário ou senha inválidos", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
