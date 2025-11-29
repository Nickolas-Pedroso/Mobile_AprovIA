package com.example.aprovia

import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Editable
import android.text.Html
import android.text.TextWatcher
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.database.FirebaseDatabase
import java.util.*

class CadastroActivity : AppCompatActivity() {

    private lateinit var editTextDate: TextInputEditText
    private lateinit var textInputLayoutDate: TextInputLayout

    private var selectedAvatarName: String? = null
    private lateinit var avatarImageViews: List<ImageView>

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cadastro)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        findViewById<TextView>(R.id.txtEntrar).setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }

        editTextDate = findViewById(R.id.editTextDate)
        textInputLayoutDate = findViewById(R.id.textInputDate)

        // Use o ícone final para abrir o calendário
        textInputLayoutDate.setEndIconOnClickListener { showDatePicker() }

        // Configura a formatação automática enquanto digita
        setupDateFormatting()

        setupAvatarSelection()
        setupCadastroButton()
        setupTermsLink()
    }

    private fun setupDateFormatting() {
        editTextDate.addTextChangedListener(object : TextWatcher {
            var isUpdating = false
            var oldString = ""

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                 oldString = s.toString()
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val str = s.toString()
                if (isUpdating) {
                    isUpdating = false
                    return
                }

                // Se estamos apagando, não interferimos (permite apagar a barra)
                if (str.length < oldString.length) {
                    return
                }

                var clean = str.replace("[^\\d]".toRegex(), "")
                // Limit to 8 digits
                if (clean.length > 8) clean = clean.substring(0, 8)

                var formatted = ""
                if (clean.isNotEmpty()) {
                    if (clean.length >= 2) {
                        formatted += clean.substring(0, 2)
                        formatted += "/"
                        if (clean.length >= 4) {
                            formatted += clean.substring(2, 4)
                            formatted += "/"
                            formatted += clean.substring(4)
                        } else {
                            formatted += clean.substring(2)
                        }
                    } else {
                        formatted += clean
                    }
                }

                isUpdating = true
                editTextDate.setText(formatted)
                editTextDate.setSelection(formatted.length)
            }

            override fun afterTextChanged(s: Editable?) {
                validateDate(s.toString())
            }
        })
    }

    private fun validateDate(dateStr: String) {
        if (dateStr.length != 10) {
            textInputLayoutDate.error = null
            return
        }

        val parts = dateStr.split("/")
        if (parts.size == 3) {
            val day = parts[0].toIntOrNull()
            val month = parts[1].toIntOrNull()
            val year = parts[2].toIntOrNull()

            if (day != null && month != null && year != null) {
                val currentYear = Calendar.getInstance().get(Calendar.YEAR)

                if (year > currentYear) {
                    textInputLayoutDate.error = "Ano inválido"
                    return
                }

                if (month < 1 || month > 12 || day < 1 || day > 31) {
                     textInputLayoutDate.error = "Data inválida"
                     return
                }

                // Verifica se é maior de 12 anos
                val dob = Calendar.getInstance()
                dob.set(year, month - 1, day)
                val today = Calendar.getInstance()
                var age = today.get(Calendar.YEAR) - dob.get(Calendar.YEAR)
                if (today.get(Calendar.DAY_OF_YEAR) < dob.get(Calendar.DAY_OF_YEAR)) {
                    age--
                }

                if (age < 12) {
                    textInputLayoutDate.error = "Precisa ter mais de 12 anos"
                } else {
                    textInputLayoutDate.error = null
                }
            } else {
                textInputLayoutDate.error = "Data inválida"
            }
        }
    }

    private fun setupTermsLink() {
        val txtLerTermos = findViewById<TextView>(R.id.txtLerTermos)
        txtLerTermos.setOnClickListener {
            val termsText = getString(R.string.full_terms)

            val scrollView = ScrollView(this)
            scrollView.setBackgroundColor(Color.parseColor("#181616"))

            val textView = TextView(this)
            textView.text = Html.fromHtml(termsText, Html.FROM_HTML_MODE_COMPACT)
            textView.setPadding(50, 40, 50, 40)
            textView.textSize = 16f
            textView.setTextColor(Color.WHITE)
            
            scrollView.addView(textView)

            AlertDialog.Builder(this)
                .setTitle("Termos de Uso")
                .setView(scrollView)
                .setPositiveButton("Fechar") { dialog, _ -> dialog.dismiss() }
                .show()
        }
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
            val nome = findViewById<TextInputEditText>(R.id.txtInputNome).text.toString().trim()
            val usuario = findViewById<TextInputEditText>(R.id.editTextUser).text.toString().trim()
            val dataNascimento = findViewById<TextInputEditText>(R.id.editTextDate).text.toString().trim()
            val email = findViewById<TextInputEditText>(R.id.editTextEmail).text.toString().trim()
            val senha = findViewById<TextInputEditText>(R.id.editTextSenha).text.toString().trim()
            val confirmarSenha = findViewById<TextInputEditText>(R.id.editTextConfirmarSenha).text.toString().trim()
            val chkTermos = findViewById<CheckBox>(R.id.chkTermos)

            if (nome.isEmpty() || usuario.isEmpty() || dataNascimento.isEmpty() || email.isEmpty() || senha.isEmpty() || confirmarSenha.isEmpty()) {
                Toast.makeText(this, "Preencha todos os campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
             // Verifica se há erro na data antes de prosseguir
            if (textInputLayoutDate.error != null) {
                Toast.makeText(this, "Corrija a data de nascimento", Toast.LENGTH_SHORT).show()
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
            if (!chkTermos.isChecked) {
                Toast.makeText(this, "Você deve aceitar os Termos de Uso para continuar.", Toast.LENGTH_SHORT).show()
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
                saveUserToDatabase(userId, userMap)
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

    private fun saveUserToDatabase(userId: String, userMap: Map<String, Any>) {
        database.reference.child("users").child(userId).setValue(userMap)
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
                    // Calcula a idade também no DatePicker
                    val today = Calendar.getInstance()
                    var age = today.get(Calendar.YEAR) - selectedYear
                    if (today.get(Calendar.DAY_OF_YEAR) < selectedCalendar.get(Calendar.DAY_OF_YEAR)) {
                        age--
                    }

                    if (age < 12) {
                        textInputLayoutDate.error = "Precisa ter mais de 12 anos"
                        editTextDate.setText("") 
                    } else {
                        textInputLayoutDate.error = null
                        val date = "%02d/%02d/%04d".format(selectedDay, selectedMonth + 1, selectedYear)
                        editTextDate.setText(date)
                    }
                }
            },
            year, month, day
        )

        datePicker.datePicker.maxDate = System.currentTimeMillis()
        datePicker.show()
    }
}
