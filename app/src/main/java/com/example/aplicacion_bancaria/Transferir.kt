package com.example.aplicacion_bancaria

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.database
import kotlin.jvm.java
import com.google.firebase.auth.EmailAuthProvider
import kotlinx.serialization.descriptors.buildSerialDescriptor


class Transferir : AppCompatActivity() {
    private val auth = FirebaseAuth.getInstance()
    private val db = Firebase.database.reference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_tranferir)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }


        val Nombre_Usuario = intent.getStringExtra("nombre");
        if(Nombre_Usuario != null){
            val Mensaje_Bienvendia =findViewById< TextView>(R.id.Bienvenida)
            Mensaje_Bienvendia.text = "Bienvenido/a \n$Nombre_Usuario \n ¿cuanto desea transferir?"
        }
        val CamVerSaldo = findViewById<Button>(R.id.btnverSaldo)
        CamVerSaldo.setOnClickListener {
            val intent = Intent(this, VerSaldo::class.java)
            startActivity(intent)
        }
        val CamPantallaInicio = findViewById<Button>(R.id.btnVolver)
        CamPantallaInicio.setOnClickListener {
            val intent = Intent(this, PantallaPrincipal::class.java)
            startActivity(intent)
        }
        val CamEstado = findViewById< TextView>(R.id.tvEstado)
        val CamTransferir = findViewById<Button>(R.id.botonTransferir)
        val Monto = findViewById<EditText>(R.id.Monto)
        val CorreoDestino = findViewById<EditText>(R.id.etCorreo)
        val clave = findViewById<EditText>(R.id.etClave)

        CamTransferir.setOnClickListener {
            val montoTexto = Monto.text.toString().trim()
            val correoTexto = CorreoDestino.text.toString().trim()
            val claveTexto = clave.text.toString()
            if (montoTexto.isEmpty() || correoTexto.isEmpty() || claveTexto.length < 6) {
                CamEstado.text = "Llena todos los campos (clave mínimo 6)"
                return@setOnClickListener
            }
            val monto = montoTexto.toDoubleOrNull()
            if (monto == null || monto <= 0) {
                CamEstado.text = "Ingresa un monto válido"
                return@setOnClickListener
            }
            val usuario = auth.currentUser
            if (usuario != null && usuario.email != null) {
                val credencial = EmailAuthProvider.getCredential(
                    usuario.email!!,
                    claveTexto
                )
                usuario.reauthenticate(credencial)
                    .addOnSuccessListener {

                        CamEstado.text = "Contraseña correcta"
                    }
                    .addOnFailureListener {
                        CamEstado.text = "Contraseña incorrecta"
                    }
            }
            auth.fetchSignInMethodsForEmail(correoTexto)
                .addOnSuccessListener { resultado ->
                    val existe = !resultado.signInMethods.isNullOrEmpty()
                    if (!existe) {
                        CamEstado.text = "El correo no está registrado"
                    }
                }
                .addOnFailureListener {
                    CamEstado.text = "Error al verificar el correo"
                }

                val uid = usuario?.uid
                if (uid != null) {
                    val refOrigenSaldo = db.child("usuarios").child(uid).child("saldo")
                    refOrigenSaldo.get().addOnSuccessListener { snapOrigen ->
                        val saldoOrigen = snapOrigen.getValue(Double::class.java) ?: 0.0
                        if (saldoOrigen < monto) {
                            CamEstado.text = "Saldo insuficiente"
                            return@addOnSuccessListener
                        }

                        val refUsuarios = db.child("usuarios")
                        refUsuarios.orderByChild("correo").equalTo(correoTexto).get()
                            .addOnSuccessListener { snapDestinos ->
                                if (!snapDestinos.exists()) {
                                    CamEstado.text = "El correo destino no existe"
                                    return@addOnSuccessListener
                                }

                                val usuarioDestino = snapDestinos.children.firstOrNull()
                                val uidDestino = usuarioDestino?.key
                                if (usuarioDestino == null || uidDestino == null) {
                                    CamEstado.text = "No se encontró el usuario destino"
                                    return@addOnSuccessListener
                                }

                                if (uidDestino == uid) {
                                    CamEstado.text = "No puedes transferirte a tu misma cuenta"
                                    return@addOnSuccessListener
                                }

                                val saldoDestino = usuarioDestino.child("saldo").getValue(Double::class.java) ?: 0.0

                                // 1. Descontar al remitente
                                refOrigenSaldo.setValue(saldoOrigen - monto)
                                    .addOnSuccessListener {
                                        // 2. Sumar al destinatario
                                        val cambiosDestino = mapOf(
                                            "saldo" to (saldoDestino + monto)
                                        )
                                        refUsuarios.child(uidDestino).updateChildren(cambiosDestino)
                                            .addOnSuccessListener {
                                                CamEstado.text = "Transacción exitosa"
                                            }
                                            .addOnFailureListener {
                                                CamEstado.text = "Error al sumar al destinatario"
                                            }
                                    }
                                    .addOnFailureListener {
                                        CamEstado.text = "Error al descontar tu saldo"
                                    }
                            }
                            .addOnFailureListener {
                                CamEstado.text = "Error al buscar el destinatario"
                            }
                    }.addOnFailureListener {
                        CamEstado.text = "Error al consultar tu saldo"
                    }
                }

        }
    }
}