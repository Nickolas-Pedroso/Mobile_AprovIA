package com.example.aprovia

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.imageview.ShapeableImageView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.util.*

class CadastroActivity : AppCompatActivity() {

    private lateinit var pickImageLauncher: ActivityResultLauncher<String>
    private lateinit var imageView: ShapeableImageView
    private lateinit var editTextDate: TextInputEditText
    private lateinit var textInputLayoutDate: TextInputLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_cadastro)

        // Ajuste para layout edge-to-edge
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        // Vai para a tela de Login
        val login = findViewById<TextView>(R.id.txtEntrar)
        login.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }
        // Referências dos componentes
        imageView = findViewById(R.id.imageView)
        editTextDate = findViewById(R.id.editTextDate)
        textInputLayoutDate = findViewById(R.id.textInputDate)

        // ---- Seletor de Data ----
        editTextDate.setOnClickListener {
            showDatePicker()
        }
        textInputLayoutDate.setEndIconOnClickListener {
            showDatePicker()
        }

        // ---- Seletor de Imagem ----
        pickImageLauncher =
            registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
                uri?.let {
                    imageView.setImageURI(it) // mostra imagem escolhida
                }
            }

        val btnPickImage = findViewById<Button>(R.id.btnPickImage)
        btnPickImage.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        // ---- Botão Cadastrar ----
        val btnCadastrar = findViewById<Button>(R.id.btnCadastrar)
        val txtInputNome = findViewById<EditText>(R.id.txtInputNome)
        val txtInputUsuario = findViewById<EditText>(R.id.editTextUser)
        val editTextDate = findViewById<EditText>(R.id.editTextDate)
        val editTextEmail = findViewById<EditText>(R.id.editTextEmail)
        val editTextSenha = findViewById<EditText>(R.id.editTextSenha)
        val editTextConfirmarSenha = findViewById<EditText>(R.id.editTextConfirmarSenha)

        btnCadastrar.setOnClickListener {
            val nome = txtInputNome.text.toString()
            val usuario = txtInputUsuario.text.toString()
            val dataNascimento = editTextDate.text.toString()
            val email = editTextEmail.text.toString()
            val senha = editTextSenha.text.toString()
            val confirmarSenha = editTextConfirmarSenha.text.toString()

            // Validação
            if (nome.isEmpty() || usuario.isEmpty() || dataNascimento.isEmpty() || email.isEmpty() || senha.isEmpty() || confirmarSenha.isEmpty()) {
                Toast.makeText(this, "Preencha todos os campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (senha != confirmarSenha) {
                Toast.makeText(this, "As senhas não coincidem", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            else{
                // Cadastro bem-sucedido
                Toast.makeText(this, "Cadastro bem-sucedido", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Função para abrir calendário
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

                // Validação: data não pode ser futura
                if (selectedCalendar.after(Calendar.getInstance())) {
                    textInputLayoutDate.error = "Data de nascimento inválida"
                    editTextDate.setText("")
                } else {
                    textInputLayoutDate.error = null
                    val date = "%02d/%02d/%04d".format(
                        selectedDay,
                        selectedMonth + 1,
                        selectedYear
                    )
                    editTextDate.setText(date)
                }
            },
            year, month, day
        )

        // Máximo permitido: hoje
        datePicker.datePicker.maxDate = System.currentTimeMillis()
        datePicker.show()
    }

}
