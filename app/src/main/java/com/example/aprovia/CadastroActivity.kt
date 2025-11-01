package com.example.aprovia

import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class CadastroActivity : AppCompatActivity() {

    private lateinit var editTextDate: TextInputEditText
    private lateinit var textInputLayoutDate: TextInputLayout

    private var selectedAvatarName: String? = null
    private lateinit var avatarImageViews: List<ImageView>

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cadastro)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        findViewById<TextView>(R.id.txtEntrar).setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }

        editTextDate = findViewById(R.id.editTextDate)
        textInputLayoutDate = findViewById(R.id.textInputDate)

        editTextDate.setOnClickListener { showDatePicker() }
        textInputLayoutDate.setEndIconOnClickListener { showDatePicker() }

        setupAvatarSelection()
        setupCadastroButton()
    }

    private fun setupAvatarSelection() {
        val avatarFAzul = findViewById<ImageView>(R.id.avatar_f_azul)
        val avatarFCastanho = findViewById<ImageView>(R.id.avatar_f_castanho)
        val avatarMVerde = findViewById<ImageView>(R.id.avatar_m_verde)
        val avatarMAzul = findViewById<ImageView>(R.id.avatar_m_azul)

        avatarImageViews = listOf(avatarFAzul, avatarFCastanho, avatarMVerde, avatarMAzul)

        val avatarMap = mapOf(
            avatarFAzul to "f_azul",
            avatarFCastanho to "f_castanho",
            avatarMVerde to "m_verde",
            avatarMAzul to "m_azul"
        )

        avatarMap.forEach { (imageView, avatarName) ->
            imageView.setOnClickListener {
                selectedAvatarName = avatarName
                updateAvatarSelection(it as ImageView)
            }
        }
    }

    private fun updateAvatarSelection(selectedImageView: ImageView) {
        avatarImageViews.forEach { iv ->
            iv.background = ColorDrawable(Color.TRANSPARENT)
            iv.setPadding(0, 0, 0, 0)
        }
        selectedImageView.setBackgroundColor(Color.parseColor("#409EEF"))
        selectedImageView.setPadding(8, 8, 8, 8)
    }

    private fun setupCadastroButton() {
        findViewById<Button>(R.id.btnCadastrar).setOnClickListener {
            // **CORREÇÃO APLICADA AQUI: Usando TextInputEditText**
            val nome = findViewById<TextInputEditText>(R.id.txtInputNome).text.toString().trim()
            val usuario = findViewById<TextInputEditText>(R.id.editTextUser).text.toString().trim()
            val dataNascimento = findViewById<TextInputEditText>(R.id.editTextDate).text.toString().trim()
            val email = findViewById<TextInputEditText>(R.id.editTextEmail).text.toString().trim()
            val senha = findViewById<TextInputEditText>(R.id.editTextSenha).text.toString().trim()
            val confirmarSenha = findViewById<TextInputEditText>(R.id.editTextConfirmarSenha).text.toString().trim()

            if (nome.isEmpty() || usuario.isEmpty() || dataNascimento.isEmpty() || email.isEmpty() || senha.isEmpty() || confirmarSenha.isEmpty()) {
                Toast.makeText(this, "Preencha todos os campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (selectedAvatarName == null) {
                Toast.makeText(this, "Por favor, selecione um avatar", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (senha.length < 6) {
                Toast.makeText(this, "A senha deve ter no mínimo 6 caracteres", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (senha != confirmarSenha) {
                Toast.makeText(this, "As senhas não coincidem", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            createUserAccount(email, senha, nome, usuario, dataNascimento, selectedAvatarName!!)
        }
    }

    private fun createUserAccount(email: String, pass: String, nome: String, usuario: String, dataNascimento: String, avatarName: String) {
        auth.createUserWithEmailAndPassword(email, pass).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val userId = auth.currentUser!!.uid
                val userMap = hashMapOf(
                    "nome" to nome,
                    "usuario" to usuario,
                    "email" to email,
                    "dataNascimento" to dataNascimento,
                    "profileImageUrl" to avatarName
                )
                saveUserToFirestore(userId, userMap)
            } else {
                val error = when (task.exception) {
                    is FirebaseAuthWeakPasswordException -> "A senha é muito fraca. Use pelo menos 6 caracteres."
                    is FirebaseAuthInvalidCredentialsException -> "O formato do e-mail é inválido."
                    is FirebaseAuthUserCollisionException -> "Este e-mail já está em uso."
                    else -> "Falha ao criar conta: ${task.exception?.message}"
                }
                Toast.makeText(this, error, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun saveUserToFirestore(userId: String, userMap: Map<String, Any>) {
        db.collection("users").document(userId).set(userMap)
            .addOnSuccessListener {
                auth.signOut()
                Toast.makeText(this, "Cadastro bem-sucedido! Faça o login.", Toast.LENGTH_LONG).show()
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Falha crítica ao salvar dados: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePicker = DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDay ->
                val selectedCalendar = Calendar.getInstance()
                selectedCalendar.set(selectedYear, selectedMonth, selectedDay)

                if (selectedCalendar.after(Calendar.getInstance())) {
                    textInputLayoutDate.error = "Data de nascimento inválida"
                    editTextDate.setText("")
                } else {
                    textInputLayoutDate.error = null
                    val date = "%02d/%02d/%04d".format(selectedDay, selectedMonth + 1, selectedYear)
                    editTextDate.setText(date)
                }
            },
            year, month, day
        )

        datePicker.datePicker.maxDate = System.currentTimeMillis()
        datePicker.show()
    }
}
