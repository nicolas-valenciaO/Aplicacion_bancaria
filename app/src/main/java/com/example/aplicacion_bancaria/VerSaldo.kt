package com.example.aplicacion_bancaria

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class VerSaldo : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseDatabase.getInstance().reference

    private lateinit var mostrarSaldo: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_ver_saldo)

        ViewCompat.setOnApplyWindowInsetsListener(
            findViewById(R.id.mainVerSaldo)
        ) { vista, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            vista.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBars.bottom
            )
            insets
        }

        mostrarSaldo = findViewById(R.id.mostrarsaldo)

        val botonTransferir =
            findViewById<Button>(R.id.botonTransferirdesdesaldo)

        val botonVolverInicio =
            findViewById<Button>(R.id.botonverIniciodesdesaldo)

        // Consultar y mostrar el saldo al abrir la pantalla
        cargarSaldo()
        val Nombre_Usuario = intent.getStringExtra("nombre");
        botonTransferir.setOnClickListener {
            val intent = Intent(this, Transferir::class.java)
            intent.putExtra("nombre", Nombre_Usuario)
            startActivity(intent)
        }

        botonVolverInicio.setOnClickListener {
            val intent = Intent(this, PantallaPrincipal::class.java)
            intent.putExtra("nombre", Nombre_Usuario)
            startActivity(intent)
        }
        if (Nombre_Usuario != null) {
            val Mensaje_Bienvendia = findViewById<TextView>(R.id.Bienvenida)
            Mensaje_Bienvendia.text =
                "Bienvenido/a $Nombre_Usuario \n¿cuanto desea transferir?"
        }
    }

    private fun cargarSaldo() {
        val usuario = auth.currentUser

        if (usuario == null) {
            mostrarSaldo.text = "Inicia sesión para consultar tu saldo"
            return
        }

        mostrarSaldo.text = "Consultando saldo..."

        db.child("usuarios")
            .child(usuario.uid)
            .child("saldo")
            .get()
            .addOnSuccessListener { snapshot ->
                val saldo = (snapshot.value as? Number)?.toDouble()

                mostrarSaldo.text = if (saldo != null) {
                    "Saldo: $${"%.2f".format(saldo)}"
                } else {
                    "No se encontró el saldo"
                }
            }
            .addOnFailureListener {
                mostrarSaldo.text = "Error al consultar el saldo"
            }
    }
    override fun onResume() {
        super.onResume()
        val listaConsejos = listOf<String>(
            "Nunca compartas tu informacion bancaria.",
            "Revisa tus movimentos bancarios semanalmente.",
            "Banco JH nunca te enviara un link para resolver algun asunto."
        )
        val consejo_Azar =listaConsejos.random()
        val consejos = findViewById<TextView>(R.id.Consejos)
        consejos.text = consejo_Azar
    }
}