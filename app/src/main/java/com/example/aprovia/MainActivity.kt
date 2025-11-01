package com.example.aprovia

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.bumptech.glide.Glide
import com.google.android.material.color.DynamicColors
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var logoutHandler: Handler
    private val logoutRunnable = Runnable {
        performLogout("Sessão expirada por inatividade")
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
        if (auth.currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        DynamicColors.applyToActivityIfAvailable(this)
        setContentView(R.layout.activity_main)
        supportActionBar?.hide()

        logoutHandler = Handler(Looper.getMainLooper())
        db = FirebaseFirestore.getInstance()

        val drawerLayout = findViewById<DrawerLayout>(R.id.drawer_layout)
        val menuIcon = findViewById<ImageView>(R.id.menuIcon)
        val messagesContainer = findViewById<LinearLayout>(R.id.messagesContainer)
        val input = findViewById<EditText>(R.id.etMessage)
        val btnSend = findViewById<ImageButton>(R.id.btnSend)
        val navigationView = findViewById<NavigationView>(R.id.navigation_view)

        loadUserData(navigationView)

        menuIcon.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        navigationView.setNavigationItemSelectedListener { menuItem ->
            drawerLayout.closeDrawer(GravityCompat.START)
            when (menuItem.itemId) {
                R.id.nav_new_chat -> {
                    messagesContainer.removeAllViews()
                    true
                }
                R.id.nav_history -> {
                    Toast.makeText(this, "Histórico clicado!", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.nav_footer -> {
                    Toast.makeText(this, "Configurações clicado!", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.nav_logout -> {
                    performLogout()
                    true
                }
                else -> false
            }
        }

        btnSend.setOnClickListener {
            val text = input.text.toString().trim()
            if (text.isNotEmpty()) {
                addMessage(text, isUser = true)
                input.text?.clear()
                messagesContainer.postDelayed({
                    addMessage("Olá, tudo bem? Como posso te ajudar hoje? 🙂", isUser = false)
                }, 400)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        resetLogoutTimer()
    }

    override fun onPause() {
        super.onPause()
        stopLogoutTimer()
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        resetLogoutTimer()
    }

    private fun resetLogoutTimer() {
        logoutHandler.removeCallbacks(logoutRunnable)
        logoutHandler.postDelayed(logoutRunnable, 60 * 1000) // 1 minuto
    }

    private fun stopLogoutTimer() {
        logoutHandler.removeCallbacks(logoutRunnable)
    }

    private fun performLogout(toastMessage: String? = null) {
        auth.signOut()
        toastMessage?.let {
            Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
        }
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun loadUserData(navigationView: NavigationView) {
        val headerView = navigationView.getHeaderView(0)
        val userImage = headerView.findViewById<ImageView>(R.id.nav_user_image)
        val userName = headerView.findViewById<TextView>(R.id.nav_user_name)
        val userEmail = headerView.findViewById<TextView>(R.id.nav_user_email)

        val userId = auth.currentUser?.uid
        if (userId != null) {
            db.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val name = document.getString("nome") ?: ""
                        val email = document.getString("email") ?: ""
                        val avatarName = document.getString("profileImageUrl") ?: ""

                        userName.text = name
                        userEmail.text = email

                        if (avatarName.isNotEmpty()) {
                            val resourceId = resources.getIdentifier(avatarName, "drawable", packageName)
                            if (resourceId != 0) {
                                Glide.with(this)
                                    .load(resourceId)
                                    .circleCrop()
                                    .into(userImage)
                            }
                        }

                        userImage.setOnClickListener {
                            showAvatarSelectionDialog()
                        }
                    } else {
                        userName.text = "Usuário"
                        userEmail.text = "email@exemplo.com"
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Erro ao carregar dados do usuário", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun showAvatarSelectionDialog() {
        val avatars = mapOf(
            "f_azul" to R.drawable.f_azul,
            "f_castanho" to R.drawable.f_castanho,
            "m_verde" to R.drawable.m_verde,
            "m_azul" to R.drawable.m_azul
        )

        val inflater = layoutInflater
        val dialogView = inflater.inflate(R.layout.dialog_avatar_picker, null)
        val avatarContainer = dialogView.findViewById<LinearLayout>(R.id.avatar_container)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Escolha um novo avatar")
            .setView(dialogView)
            .setNegativeButton("Cancelar", null)
            .create()

        avatars.forEach { (avatarName, drawableId) ->
            val imageView = ImageView(this).apply {
                layoutParams = LinearLayout.LayoutParams(150, 150).apply {
                    setMargins(16, 16, 16, 16)
                }
                setImageResource(drawableId)
                setOnClickListener {
                    updateUserProfileAvatar(avatarName, dialog)
                }
            }
            avatarContainer.addView(imageView)
        }

        dialog.show()
    }

    private fun updateUserProfileAvatar(avatarName: String, dialog: AlertDialog) {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId)
            .update("profileImageUrl", avatarName)
            .addOnSuccessListener {
                Toast.makeText(this, "Avatar atualizado!", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
                loadUserData(findViewById(R.id.navigation_view))
            }
            .addOnFailureListener {
                Toast.makeText(this, "Falha ao atualizar o avatar.", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
    }

    private fun scrollToBottom() {
        val scroll = findViewById<ScrollView>(R.id.scrollMessages)
        scroll.post { scroll.fullScroll(View.FOCUS_DOWN) }
    }

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
            maxWidth = (resources.displayMetrics.widthPixels * 0.75).toInt()
        }

        val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            topMargin = 12
            bottomMargin = 12
            gravity = if (isUser) Gravity.END else Gravity.START
        }

        tv.layoutParams = lp
        messagesContainer.addView(tv)
        scrollToBottom()
    }
}
