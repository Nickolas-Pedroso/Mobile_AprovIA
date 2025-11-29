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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.database.FirebaseDatabase

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        findViewById<TextView>(R.id.txtCadastro).setOnClickListener {
            startActivity(Intent(this, CadastroActivity::class.java))
        }

        findViewById<TextView>(R.id.txtAjuda).setOnClickListener {
            startActivity(Intent(this, faq_ajuda::class.java))
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

            // ** LÓGICA DE LOGIN POR NOME DE USUÁRIO **
            // 1. Procura no Realtime Database um usuário com o 'username' fornecido.
            database.reference.child("users")
                .orderByChild("usuario")
                .equalTo(username)
                .get()
                .addOnSuccessListener { dataSnapshot ->
                    // 2. Verifica se algum usuário foi encontrado.
                    if (!dataSnapshot.exists()) {
                        Toast.makeText(baseContext, "Usuário ou senha incorretos.", Toast.LENGTH_LONG).show()
                        return@addOnSuccessListener
                    }

                    // 3. Se encontrou, pega o e-mail desse usuário.
                    val userNode = dataSnapshot.children.first()
                    val email = userNode.child("email").getValue(String::class.java)

                    if (email == null) {
                        Toast.makeText(baseContext, "Erro crítico: e-mail não encontrado para este usuário.", Toast.LENGTH_LONG).show()
                        return@addOnSuccessListener
                    }

                    // 4. Usa o e-mail e a senha para fazer o login no Firebase Auth.
                    auth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener(this) { task ->
                            if (task.isSuccessful) {
                                // 5. Login bem-sucedido, vai para a tela principal.
                                val intent = Intent(this, MainActivity::class.java)
                                startActivity(intent)
                                finish()
                            } else {
                                // Se a autenticação falhar, a senha está errada.
                                val exception = task.exception
                                val errorMessage = when (exception) {
                                    is FirebaseAuthInvalidCredentialsException -> "Usuário ou senha incorretos."
                                    else -> "Falha na autenticação: ${exception?.message}"
                                }
                                Toast.makeText(baseContext, errorMessage, Toast.LENGTH_LONG).show()
                            }
                        }
                }
                .addOnFailureListener { e ->
                    // Falha ao se comunicar com o banco de dados.
                    Toast.makeText(baseContext, "Erro ao conectar com o servidor: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }
    }

    public override fun onStart() {
        super.onStart()
        // Impede o login automático ao iniciar o app.
        // Apenas o login manual é permitido.
        if (auth.currentUser != null) {
            auth.signOut()
        }
    }
}
