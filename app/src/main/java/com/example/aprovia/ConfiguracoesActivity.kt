package com.example.aprovia

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class ConfiguracoesActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var edtNome: TextInputEditText
    private lateinit var edtEmail: TextInputEditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_configuracoes)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        edtNome = findViewById(R.id.edtNome)
        edtEmail = findViewById(R.id.edtEmail)
        val btnSalvar = findViewById<Button>(R.id.btnSalvar)
        val btnDeletarConta = findViewById<Button>(R.id.btnDeletarConta)

        loadUserData()

        btnSalvar.setOnClickListener {
            saveChanges()
        }

        btnDeletarConta.setOnClickListener {
            confirmDeleteAccount()
        }
    }

    private fun loadUserData() {
        val user = auth.currentUser
        if (user != null) {
            // Pega o email do Auth
            edtEmail.setText(user.email)

            // Pega o nome do Database
            database.reference.child("users").child(user.uid).get().addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    val nome = snapshot.child("nome").getValue(String::class.java)
                    edtNome.setText(nome)
                }
            }
        }
    }

    private fun saveChanges() {
        val user = auth.currentUser ?: return
        val novoNome = edtNome.text.toString().trim()
        val novoEmail = edtEmail.text.toString().trim()

        if (novoNome.isEmpty() || novoEmail.isEmpty()) {
            Toast.makeText(this, "Preencha todos os campos.", Toast.LENGTH_SHORT).show()
            return
        }

        // Atualiza nome no Database
        database.reference.child("users").child(user.uid).child("nome").setValue(novoNome)
            .addOnSuccessListener {
                Toast.makeText(this, "Nome atualizado!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Erro ao atualizar nome: ${it.message}", Toast.LENGTH_SHORT).show()
            }

        // Se o e-mail mudou, precisamos reautenticar
        if (novoEmail != user.email) {
            reauthenticateAndChangeEmail(novoEmail)
        }
    }

    private fun reauthenticateAndChangeEmail(novoEmail: String) {
        val user = auth.currentUser ?: return

        // Diálogo para pedir senha atual
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Confirme sua senha")
        builder.setMessage("Para alterar o e-mail, confirme sua senha atual.")

        val container = LinearLayout(this)
        container.orientation = LinearLayout.VERTICAL
        val params = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(50, 0, 50, 0)
        container.layoutParams = params
        container.setPadding(50, 20, 50, 20)

        val inputSenha = EditText(this)
        inputSenha.hint = "Senha Atual"
        inputSenha.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        container.addView(inputSenha)

        builder.setView(container)

        builder.setPositiveButton("Confirmar") { _, _ ->
            val senha = inputSenha.text.toString()
            if (senha.isNotEmpty()) {
                val credential = EmailAuthProvider.getCredential(user.email!!, senha)
                user.reauthenticate(credential).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        user.updateEmail(novoEmail).addOnCompleteListener { updateTask ->
                            if (updateTask.isSuccessful) {
                                database.reference.child("users").child(user.uid).child("email").setValue(novoEmail)
                                Toast.makeText(this, "E-mail atualizado com sucesso!", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(this, "Erro ao atualizar e-mail: ${updateTask.exception?.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    } else {
                        Toast.makeText(this, "Senha incorreta.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
        builder.setNegativeButton("Cancelar", null)
        builder.show()
    }

    private fun confirmDeleteAccount() {
        AlertDialog.Builder(this)
            .setTitle("Deletar Conta")
            .setMessage("Tem certeza? Essa ação não pode ser desfeita.")
            .setPositiveButton("Deletar") { _, _ ->
                val user = auth.currentUser
                val uid = user?.uid
                
                if (user != null && uid != null) {
                    // Remove do Database primeiro
                    database.reference.child("users").child(uid).removeValue().addOnCompleteListener { dbTask ->
                        if (dbTask.isSuccessful) {
                             // Deleta do Authentication
                            user.delete().addOnCompleteListener { authTask ->
                                if (authTask.isSuccessful) {
                                    Toast.makeText(this, "Conta deletada.", Toast.LENGTH_LONG).show()
                                    // Volta para Login, limpando a pilha
                                    val intent = android.content.Intent(this, LoginActivity::class.java)
                                    intent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
                                    startActivity(intent)
                                    finish()
                                } else {
                                    Toast.makeText(this, "Erro ao deletar autenticação: ${authTask.exception?.message}. Tente fazer login novamente antes de deletar.", Toast.LENGTH_LONG).show()
                                }
                            }
                        } else {
                             Toast.makeText(this, "Erro ao limpar dados: ${dbTask.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}
