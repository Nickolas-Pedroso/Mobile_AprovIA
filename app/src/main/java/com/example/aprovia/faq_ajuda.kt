package com.example.aprovia

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity


class faq_ajuda : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_faq_ajuda)

        val q1 = findViewById<LinearLayout>(R.id.q1)
        val a1 = findViewById<TextView>(R.id.tvA1)

        val q2 = findViewById<LinearLayout>(R.id.q2)
        val a2 = findViewById<TextView>(R.id.tvA2)

        val q3 = findViewById<LinearLayout>(R.id.q3)
        val a3 = findViewById<TextView>(R.id.tvA3)

        val q4 = findViewById<LinearLayout>(R.id.q4)
        val a4 = findViewById<TextView>(R.id.tvA4)

        val q5 = findViewById<LinearLayout>(R.id.q5)
        val a5 = findViewById<TextView>(R.id.tvA5)

        val btnBack = findViewById<Button>(R.id.btnBack)

        // Ação de expandir/colapsar
        q1.setOnClickListener { toggleVisibility(a1) }
        q2.setOnClickListener { toggleVisibility(a2) }
        q3.setOnClickListener { toggleVisibility(a3) }
        q4.setOnClickListener { toggleVisibility(a4) }
        q5.setOnClickListener { toggleVisibility(a5) }

        // Voltar para a tela de login
        btnBack.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }
    }
    private fun toggleVisibility(view: View) {
        view.visibility = if (view.visibility == View.GONE) View.VISIBLE else View.GONE
    }
}


