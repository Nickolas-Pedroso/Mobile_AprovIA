package com.example.aprovia

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.Menu
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
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var userChatsRef: DatabaseReference
    private var userId: String? = null
    private var currentChatId: String? = null

    private lateinit var navigationView: NavigationView
    private lateinit var messagesContainer: LinearLayout
    private lateinit var drawerLayout: DrawerLayout

    private lateinit var logoutHandler: Handler
    private val logoutRunnable = Runnable {
        performLogout("Sessão expirada por inatividade")
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
        userId = auth.currentUser?.uid
        if (userId == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        DynamicColors.applyToActivityIfAvailable(this)
        setContentView(R.layout.activity_main)
        supportActionBar?.hide()

        logoutHandler = Handler(Looper.getMainLooper())
        database = FirebaseDatabase.getInstance()
        userChatsRef = database.getReference("chats").child(userId!!)

        drawerLayout = findViewById(R.id.drawer_layout)
        val menuIcon = findViewById<ImageView>(R.id.menuIcon)
        val input = findViewById<EditText>(R.id.etMessage)
        val btnSend = findViewById<ImageButton>(R.id.btnSend)
        navigationView = findViewById(R.id.navigation_view)
        messagesContainer = findViewById(R.id.messagesContainer)

        loadUserData(navigationView)
        setupNavigation()
        startNewChat()

        menuIcon.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
            updateHistoryMenu()
        }

        btnSend.setOnClickListener {
            val text = input.text.toString().trim()
            if (text.isNotEmpty()) {
                var isFirstMessage = false
                if (currentChatId == null) {
                    isFirstMessage = true
                    val newChatId = userChatsRef.push().key ?: return@setOnClickListener
                    currentChatId = newChatId
                    userChatsRef.child(newChatId).child("createdAt").setValue(ServerValue.TIMESTAMP)
                }

                addMessage(text, isUser = true)
                input.text?.clear()

                if (isFirstMessage) {
                    updateHistoryMenu()
                }

                messagesContainer.postDelayed({
                    addMessage("Olá, tudo bem? Como posso te ajudar hoje? 🙂", isUser = false)
                }, 400)
            }
        }
    }

    private fun setupNavigation() {
        navigationView.setNavigationItemSelectedListener { menuItem ->
            drawerLayout.closeDrawer(GravityCompat.START)

            when (menuItem.itemId) {
                R.id.nav_new_chat -> {
                    startNewChat()
                    return@setNavigationItemSelectedListener true
                }
                R.id.nav_footer -> {
                    Toast.makeText(this, "Configurações clicado!", Toast.LENGTH_SHORT).show()
                    return@setNavigationItemSelectedListener true
                }
                R.id.nav_logout -> {
                    performLogout()
                    return@setNavigationItemSelectedListener true
                }
            }

            if (menuItem.groupId == R.id.nav_history_group) {
                val selectedChatId = menuItem.titleCondensed?.toString()
                if (!selectedChatId.isNullOrEmpty() && selectedChatId != currentChatId) {
                    currentChatId = selectedChatId
                    loadChatHistory(selectedChatId)
                }
                return@setNavigationItemSelectedListener true
            }

            false
        }
    }

    private fun startNewChat() {
        currentChatId = null
        messagesContainer.removeAllViews()
    }

    private fun updateHistoryMenu() {
        val historyGroupItem = navigationView.menu.findItem(R.id.nav_history_group)
        val historySubMenu = historyGroupItem?.subMenu

        if (historySubMenu == null) return

        userChatsRef.orderByChild("createdAt").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                historySubMenu.clear()

                if (snapshot.exists()) {
                    historyGroupItem.isEnabled = true
                    val chatEntries = snapshot.children.reversed()
                    for (chatSnapshot in chatEntries) {
                        val chatId = chatSnapshot.key ?: continue
                        val timestamp = chatSnapshot.child("createdAt").getValue(Long::class.java) ?: 0
                        val formattedDate = SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault()).format(Date(timestamp))

                        val menuItem = historySubMenu.add(R.id.nav_history_group, Menu.NONE, 0, formattedDate)
                        menuItem.titleCondensed = chatId
                    }
                } else {
                    historyGroupItem.isEnabled = false
                    historySubMenu.add("Nenhum histórico").isEnabled = false
                }
            }

            override fun onCancelled(error: DatabaseError) {
                historyGroupItem.isEnabled = false
                historySubMenu.clear()
                historySubMenu.add("Erro ao carregar").isEnabled = false
                Toast.makeText(this@MainActivity, "Erro ao carregar histórico", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun loadChatHistory(chatId: String) {
        userChatsRef.child(chatId).child("messages").get().addOnSuccessListener { dataSnapshot ->
            messagesContainer.removeAllViews()
            if (dataSnapshot.exists()) {
                for (messageSnapshot in dataSnapshot.children) {
                    val text = messageSnapshot.child("text").getValue(String::class.java)
                    val isUser = messageSnapshot.child("user").getValue(Boolean::class.java)
                    if (text != null && isUser != null) {
                        addMessage(text, isUser, saveToDb = false)
                    }
                }
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

        if (userId != null) {
            database.getReference("users").child(userId!!).get()
                .addOnSuccessListener { dataSnapshot ->
                    if (dataSnapshot.exists()) {
                        val name = dataSnapshot.child("nome").getValue(String::class.java) ?: ""
                        val email = dataSnapshot.child("email").getValue(String::class.java) ?: ""
                        val avatarName = dataSnapshot.child("profileImageUrl").getValue(String::class.java) ?: ""

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
        if (userId == null) return
        database.getReference("users").child(userId!!).child("profileImageUrl").setValue(avatarName)
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
    private fun addMessage(text: String, isUser: Boolean, saveToDb: Boolean = true) {
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

        if (saveToDb && currentChatId != null) {
            val messageId = userChatsRef.child(currentChatId!!).child("messages").push().key ?: return
            val messageData = mapOf("text" to text, "user" to isUser)
            userChatsRef.child(currentChatId!!).child("messages").child(messageId).setValue(messageData)
        }
    }
}
