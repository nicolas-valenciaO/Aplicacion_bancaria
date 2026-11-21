package com.example.aplicacion_bancaria

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.database
import kotlin.jvm.java
import android.content.Intent
class PantallaPrincipal : AppCompatActivity() {

    // Conexión con Firebase
    private val auth = FirebaseAuth.getInstance()
    private val db = Firebase.database.reference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_pantalla_principal)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.Inicio)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val Correo = intent.getStringExtra("correo")
        val Nombre_Usuario = intent.getStringExtra("nombre");
        if(Nombre_Usuario != null){
            val Mensaje_Bienvendia =findViewById< TextView>(R.id.Bienvenida)
            Mensaje_Bienvendia.text = "Bienvenido/a \n$Nombre_Usuario"
        }
        val CamVerSaldo = findViewById<Button>(R.id.botonverSaldo)
        CamVerSaldo.setOnClickListener {
            val intent = Intent(this, VerSaldo::class.java)
            intent.putExtra("nombre", Nombre_Usuario)
            startActivity(intent)
            }
        val CamTransferir = findViewById<Button>(R.id.botonTransferir)
        CamTransferir.setOnClickListener {
            val intent = Intent(this, Transferir::class.java)
            intent.putExtra("nombre", Nombre_Usuario)
            startActivity(intent)
            }
        val CamCerrarSesion = findViewById<Button>(R.id.CerrarSesion)
        CamCerrarSesion.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
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
