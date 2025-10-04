package com.example.aprovia

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.color.DynamicColors
import com.google.android.material.navigation.NavigationView

class MainActivity : AppCompatActivity() {

    // Ele ignora os avisos de segurança do Android Studio
    @SuppressLint("UseCompatLoadingForDrawables")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DynamicColors.applyToActivityIfAvailable(this)
        setContentView(R.layout.activity_main)
        supportActionBar?.hide()

        // --- Referências aos componentes da tela (Views) ---
        val drawerLayout = findViewById<DrawerLayout>(R.id.drawer_layout)
        val menuIcon = findViewById<ImageView>(R.id.menuIcon)
        val logo = findViewById<ImageView>(R.id.logoImage)
        val messagesContainer = findViewById<LinearLayout>(R.id.messagesContainer)
        val input = findViewById<EditText>(R.id.etMessage)
        val btnSend = findViewById<ImageButton>(R.id.btnSend)
        val navigationView = findViewById<NavigationView>(R.id.navigation_view)

        // --- Configuração dos Eventos (Listeners) ---

        // Abre o menu lateral ao clicar no ícone
        menuIcon.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        // Listener para TODOS os itens do menu (Nova Conversa, Histórico, Configurações, Sair)
        navigationView.setNavigationItemSelectedListener { menuItem ->
            // Fecha o menu lateral após o clique em um item
            drawerLayout.closeDrawer(GravityCompat.START)

            // Executa uma ação baseada no item clicado
            when (menuItem.itemId) {
                R.id.nav_new_chat -> {
                    messagesContainer.removeAllViews() // Apenas limpa a tela
                    true // Indica que o clique foi tratado
                }
                R.id.nav_history -> {
                    Toast.makeText(this, "Histórico clicado!", Toast.LENGTH_SHORT).show()
                    // Futuramente, você pode abrir uma nova tela aqui
                    true
                }
                // LÓGICA DE CONFIGURAÇÕES E SAIR DE VOLTA AO LISTENER PRINCIPAL
                R.id.nav_footer -> { // ID do item "Configurações" no seu menu_drawer.xml
                    Toast.makeText(this, "Configurações clicado!", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.nav_logout -> { // ID do item "Sair" no seu menu_drawer.xml
                    logo.performClick() // Reutiliza a sua lógica de logout existente
                    true
                }
                else -> false // Indica que o clique não foi tratado
            }
        }

        // O listener antigo do "footer" foi removido pois não é mais necessário.

        // Clique no logo → sair (a ação principal de logout está aqui)
        logo.setOnClickListener {
            val prefs = getSharedPreferences("APP_PREFS", MODE_PRIVATE)
            prefs.edit {
                putBoolean("LOGGED_IN", false)
            }
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        // Enviar mensagem
        btnSend.setOnClickListener {
            val text = input.text.toString().trim()
            if (text.isNotEmpty()) {
                addMessage(text, isUser = true)
                input.text?.clear()

                // Resposta simulada do bot
                messagesContainer.postDelayed({
                    addMessage("Olá, tudo bem? Como posso te ajudar hoje? 🙂", isUser = false)
                }, 400)
            }
        }
    }

    // --- Funções Auxiliares ---

    // Função para rolar a ScrollView até o final
    private fun scrollToBottom() {
        val scroll = findViewById<ScrollView>(R.id.scrollMessages)
        scroll.post { scroll.fullScroll(View.FOCUS_DOWN) }
    }

    // Função para adicionar balões de mensagem na tela
    @SuppressLint("UseCompatLoadingForDrawables")
    private fun addMessage(text: String, isUser: Boolean) {
        val messagesContainer = findViewById<LinearLayout>(R.id.messagesContainer)
        val tv = TextView(this).apply {
            this.text = text
            setTextColor(resources.getColor(android.R.color.white, theme))
            setPadding(24, 16, 24, 16)
            background = resources.getDrawable(
                if (isUser) R.drawable.bg_bubble_user else R.drawable.bg_bubble_bot,
                theme
            )
            // Define uma largura máxima para quebrar a linha em textos longos
            maxWidth = (resources.displayMetrics.widthPixels * 0.75).toInt()
        }

        // Define os parâmetros de layout para o balão de mensagem
        val lp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            topMargin = 12
            bottomMargin = 12
            gravity = if (isUser) Gravity.END else Gravity.START
        }

        tv.layoutParams = lp
        messagesContainer.addView(tv)
        scrollToBottom()
    }
}
