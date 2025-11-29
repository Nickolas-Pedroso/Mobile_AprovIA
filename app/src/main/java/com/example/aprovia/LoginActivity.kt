package com.example.aprovia

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.OAuthProvider
import com.google.firebase.database.FirebaseDatabase

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var googleSignInClient: GoogleSignInClient

    // Launcher para o resultado do login do Google
    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            account?.idToken?.let { firebaseAuthWithGoogle(it) }
        } catch (e: ApiException) {
            Toast.makeText(this, "Falha no login Google: ${e.statusCode}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        // Configuração do Google Sign In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id)) // O google-services.json gera isso
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupClickListeners()
    }

    private fun setupClickListeners() {
        findViewById<TextView>(R.id.txtCadastro).setOnClickListener {
            startActivity(Intent(this, CadastroActivity::class.java))
        }

        findViewById<TextView>(R.id.txtAjuda).setOnClickListener {
            startActivity(Intent(this, faq_ajuda::class.java))
        }

        // Botão Google
        findViewById<Button>(R.id.btnGoogle).setOnClickListener {
            // ALTERADO: Usamos signOut() em vez de revokeAccess().
            // Isso permite escolher a conta novamente, mas NÃO pede permissão (consentimento) toda vez.
            googleSignInClient.signOut().addOnCompleteListener {
                val signInIntent = googleSignInClient.signInIntent
                googleSignInLauncher.launch(signInIntent)
            }
        }

        // Botão GitHub
        findViewById<Button>(R.id.btnGithub).setOnClickListener {
            signInWithGithub()
        }

        // Lógica para Esqueci a Senha
        findViewById<TextView>(R.id.txtEsqueciSenha).setOnClickListener {
            showForgotPasswordDialog()
        }

        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val edtUser = findViewById<EditText>(R.id.edtUser)
        val edtPass = findViewById<EditText>(R.id.edtPass)

        btnLogin.setOnClickListener {
            val username = edtUser.text.toString().trim()
            val password = edtPass.text.toString().trim()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Preencha todos os campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            performLogin(username, password)
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    checkAndSaveSocialUser("google")
                } else {
                    Toast.makeText(this, "Autenticação Google falhou.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun signInWithGithub() {
        val provider = OAuthProvider.newBuilder("github.com")
        // Escopos opcionais: repo, user, etc.
        provider.addCustomParameter("login", "") 

        val pendingResultTask = auth.pendingAuthResult
        if (pendingResultTask != null) {
            // Já existe um login pendente
            pendingResultTask
                .addOnSuccessListener {
                    checkAndSaveSocialUser("github")
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Erro: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            auth.startActivityForSignInWithProvider(this, provider.build())
                .addOnSuccessListener {
                    checkAndSaveSocialUser("github")
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Erro GitHub: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    // Verifica se o usuário social já está no Realtime Database, se não, salva.
    private fun checkAndSaveSocialUser(provider: String) {
        val user = auth.currentUser ?: return
        val uid = user.uid
        val email = user.email ?: ""
        val name = user.displayName ?: "Usuário $provider"
        
        // Referência ao usuário no banco
        val userRef = database.reference.child("users").child(uid)
        
        userRef.get().addOnSuccessListener { snapshot ->
            if (!snapshot.exists()) {
                // Se é o primeiro acesso, cria o registro no banco
                // Geramos um 'usuario' baseado no email ou ID para compatibilidade
                val username = email.substringBefore("@").ifEmpty { "user_$uid" }
                
                val userData = mapOf(
                    "nome" to name,
                    "email" to email,
                    "usuario" to username,
                    "profileImageUrl" to "m_azul" // Avatar padrão
                )
                userRef.setValue(userData).addOnCompleteListener {
                     goToMain()
                }
            } else {
                goToMain()
            }
        }
    }

    private fun goToMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun performLogin(username: String, password: String) {
         database.reference.child("users")
            .orderByChild("usuario")
            .equalTo(username)
            .get()
            .addOnSuccessListener { dataSnapshot ->
                if (!dataSnapshot.exists()) {
                    Toast.makeText(baseContext, "Usuário ou senha incorretos.", Toast.LENGTH_LONG).show()
                    return@addOnSuccessListener
                }

                val userNode = dataSnapshot.children.first()
                val email = userNode.child("email").getValue(String::class.java)

                if (email == null) {
                    Toast.makeText(baseContext, "Erro crítico.", Toast.LENGTH_LONG).show()
                    return@addOnSuccessListener
                }

                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            goToMain()
                        } else {
                            val exception = task.exception
                            val errorMessage = when (exception) {
                                is FirebaseAuthInvalidCredentialsException -> "Usuário ou senha incorretos."
                                else -> "Falha: ${exception?.message}"
                            }
                            Toast.makeText(baseContext, errorMessage, Toast.LENGTH_LONG).show()
                        }
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(baseContext, "Erro de conexão: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun showForgotPasswordDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Redefinir Senha")

        val container = LinearLayout(this)
        container.orientation = LinearLayout.VERTICAL
        val params = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(50, 20, 50, 0)
        container.layoutParams = params
        container.setPadding(50, 20, 50, 20)

        val input = EditText(this)
        input.hint = "Digite seu e-mail"
        input.inputType = android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        container.addView(input)

        val errorText = TextView(this)
        errorText.text = "E-mail não encontrado."
        errorText.setTextColor(Color.RED)
        errorText.textSize = 12f
        errorText.visibility = android.view.View.GONE
        errorText.setPadding(0, 10, 0, 0)
        container.addView(errorText)

        builder.setView(container)
        builder.setPositiveButton("Enviar", null)
        builder.setNegativeButton("Cancelar") { dialog, _ -> dialog.cancel() }

        val dialog = builder.create()
        dialog.show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val email = input.text.toString().trim()
            errorText.visibility = android.view.View.GONE

            if (email.isEmpty()) {
                errorText.text = "Por favor, digite seu e-mail."
                errorText.visibility = android.view.View.VISIBLE
                return@setOnClickListener
            }

            dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false

            database.reference.child("users")
                .orderByChild("email")
                .equalTo(email)
                .get()
                .addOnSuccessListener { dataSnapshot ->
                    if (dataSnapshot.exists()) {
                        auth.sendPasswordResetEmail(email)
                            .addOnCompleteListener { task ->
                                dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = true
                                if (task.isSuccessful) {
                                    Toast.makeText(this, "E-mail enviado!", Toast.LENGTH_LONG).show()
                                    dialog.dismiss()
                                } else {
                                    errorText.text = "Erro: ${task.exception?.message}"
                                    errorText.visibility = android.view.View.VISIBLE
                                }
                            }
                    } else {
                        dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = true
                        errorText.text = "Este e-mail não está cadastrado."
                        errorText.visibility = android.view.View.VISIBLE
                    }
                }
                .addOnFailureListener { e ->
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = true
                    errorText.text = "Erro: ${e.message}"
                    errorText.visibility = android.view.View.VISIBLE
                }
        }
    }

    public override fun onStart() {
        super.onStart()
        if (auth.currentUser != null) {
            // Se quiser logar direto:
            // goToMain()
            
            // Mas seu código original fazia logout:
            auth.signOut()
        }
    }
}
