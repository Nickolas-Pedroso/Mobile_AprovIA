package com.example.aprovia

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.color.DynamicColors

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // (opcional) aplica Dynamic Colors em aparelhos compatíveis
        DynamicColors.applyToActivityIfAvailable(this)

        setContentView(R.layout.activity_main)
        supportActionBar?.hide()

        val messagesContainer = findViewById<LinearLayout>(R.id.messagesContainer)
        val scroll = findViewById<ScrollView>(R.id.scrollMessages)
        val input = findViewById<android.widget.EditText>(R.id.etMessage)
        val btnSend = findViewById<ImageButton>(R.id.btnSend)

        fun scrollToBottom() {
            scroll.post { scroll.fullScroll(View.FOCUS_DOWN) }
        }

        @SuppressLint("UseCompatLoadingForDrawables")
        fun addMessage(text: String, isUser: Boolean) {
            val tv = TextView(this).apply {
                this.text = text
                setTextColor(resources.getColor(android.R.color.white, theme))
                setPadding(24, 16, 24, 16)
                background = resources.getDrawable(
                    if (isUser) R.drawable.bg_bubble_user else R.drawable.bg_bubble_bot,
                    theme
                )
                // largura máxima para não ocupar a tela toda
                maxWidth = (resources.displayMetrics.widthPixels * 0.75).toInt()
            }

            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 12
                bottomMargin = 12
                if (isUser) {
                    gravity = Gravity.END
                } else {
                    gravity = Gravity.START
                }
            }

            tv.layoutParams = lp
            messagesContainer.addView(tv)
            scrollToBottom()
        }

        btnSend.setOnClickListener {
            val text = input.text.toString().trim()
            if (text.isNotEmpty()) {
                addMessage(text, isUser = true)
                input.text?.clear()

                //Uma resposta simples para ver o fluxo
                messagesContainer.postDelayed({
                    addMessage(" Olá, tudo bem? como posso te ajudar hoje?! 🙂", isUser = false)
                }, 400)
            }
        }
    }
}
