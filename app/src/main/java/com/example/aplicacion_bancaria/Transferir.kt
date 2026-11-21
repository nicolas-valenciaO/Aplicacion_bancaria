
package com.example.aplicacion_bancaria

import android.util.Log
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.database.ServerValue
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.database.database

class Transferir : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val db = Firebase.database.reference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_tranferir)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBars.bottom
            )
            insets
        }

        val Nombre_Usuario = intent.getStringExtra("nombre")

        if (Nombre_Usuario != null) {
            val Mensaje_Bienvendia = findViewById<TextView>(R.id.Bienvenida)
            Mensaje_Bienvendia.text =
                "Bienvenido/a \n$Nombre_Usuario \n ¿cuanto desea transferir?"
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

        val CamEstado = findViewById<TextView>(R.id.tvEstado)
        val CamTransferir = findViewById<Button>(R.id.botonTransferir)
        val Monto = findViewById<EditText>(R.id.Monto)
        val CorreoDestino = findViewById<EditText>(R.id.etCorreo)
        val clave = findViewById<EditText>(R.id.etClave)

        CamTransferir.setOnClickListener {

            val montoTexto = Monto.text.toString().trim()
            val correoTexto = CorreoDestino.text.toString().trim().lowercase()
            val claveTexto = clave.text.toString()

            // 1. Validar campos
            if (montoTexto.isEmpty() ||
                correoTexto.isEmpty() ||
                claveTexto.isEmpty()
            ) {
                CamEstado.text = "Llena todos los campos"
                return@setOnClickListener
            }

            // 2. Validar monto
            val monto = montoTexto.toDoubleOrNull()

            if (monto == null || !monto.isFinite() || monto <= 0) {
                CamEstado.text = "Ingresa un monto válido"
                return@setOnClickListener
            }

            // 3. Obtener usuario que envía
            val usuario = auth.currentUser

            if (usuario == null || usuario.email == null) {
                CamEstado.text = "Inicia sesión nuevamente"
                return@setOnClickListener
            }

            val uidOrigen = usuario.uid

            // DIAGNÓSTICO 1: UID autenticado
            Log.d("TRANSFERENCIA", "UID autenticado: $uidOrigen")

            // 4. Verificar contraseña
            val credencial = EmailAuthProvider.getCredential(
                usuario.email!!,
                claveTexto
            )

            usuario.reauthenticate(credencial)
                .addOnSuccessListener {

                    CamEstado.text = "Contraseña correcta"

                    // 5. Buscar correo del destinatario
                    val refUsuarios = db.child("directorioUsuarios")

                    refUsuarios.orderByChild("correo")
                        .equalTo(correoTexto)
                        .limitToFirst(2)
                        .get()
                        .addOnSuccessListener busquedaDestino@{ snapshot ->

                            if (!snapshot.exists()) {
                                CamEstado.text = "El destinatario no existe"
                                return@busquedaDestino
                            }

                            val usuariosEncontrados = snapshot.children.toList()

                            if (usuariosEncontrados.size > 1) {
                                CamEstado.text = "El correo está duplicado"
                                return@busquedaDestino
                            }

                            // 6. Obtener UID del destinatario
                            val usuarioDestino = usuariosEncontrados.first()
                            val uidDestino = usuarioDestino.key

                            if (uidDestino == null) {
                                CamEstado.text = "No se encontró el UID del destinatario"
                                return@busquedaDestino
                            }

                            if (uidOrigen == uidDestino) {
                                CamEstado.text =
                                    "No puedes transferirte a tu misma cuenta"
                                return@busquedaDestino
                            }

                            // 7. Obtener saldo del remitente
                            val refOrigenSaldo = db.child("usuarios")
                                .child(uidOrigen)
                                .child("saldo")

                            // DIAGNÓSTICO 2: ruta consultada
                            Log.d(
                                "TRANSFERENCIA",
                                "Ruta origen: $refOrigenSaldo"
                            )

                            refOrigenSaldo.get()
                                .addOnSuccessListener saldoOrigenOk@{ snapOrigen ->

                                    // DIAGNÓSTICO 3: datos recibidos
                                    Log.d(
                                        "TRANSFERENCIA",
                                        "UID origen: $uidOrigen"
                                    )

                                    Log.d(
                                        "TRANSFERENCIA",
                                        "Saldo encontrado: ${snapOrigen.value}"
                                    )

                                    val saldoOrigen =
                                        (snapOrigen.value as? Number)?.toDouble()

                                    if (saldoOrigen == null) {
                                        CamEstado.text = "No se encontró tu saldo"
                                        return@saldoOrigenOk
                                    }

                                    // 8. Verificar saldo suficiente
                                    if (saldoOrigen < monto) {
                                        CamEstado.text = "Saldo insuficiente"
                                        return@saldoOrigenOk
                                    }

                                    // 9. Obtener saldo del destinatario
                                    val cambios = mapOf<String, Any>(
                                        "usuarios/$uidOrigen/saldo" to ServerValue.increment(-monto),
                                        "usuarios/$uidDestino/saldo" to ServerValue.increment(+monto)
                                    )

                                    db.updateChildren(cambios)
                                        .addOnSuccessListener {
                                            val nuevoSaldo = saldoOrigen - monto
                                            CamEstado.text = "¡Transferencia exitosa!\nNuevo saldo: $nuevoSaldo"
                                            Monto.text.clear()
                                            CorreoDestino.text.clear()
                                            clave.text.clear()
                                        }
                                        .addOnFailureListener { error ->
                                            CamEstado.text = "Error al transferir: ${error.message}"
                                            Log.e("TRANSFERENCIA", "Error actualizando saldos", error)
                                        }
                                }
                                .addOnFailureListener { error ->
                                    CamEstado.text =
                                        "Error al consultar tu saldo: ${error.message}"

                                    Log.e(
                                        "TRANSFERENCIA",
                                        "Error al consultar saldo del remitente",
                                        error
                                    )
                                }
                        }
                        .addOnFailureListener { error ->
                            CamEstado.text =
                                "Error al buscar destinatario: ${error.message}"

                            Log.e(
                                "TRANSFERENCIA",
                                "Error al buscar destinatario",
                                error
                            )
                        }
                }
                .addOnFailureListener { error ->
                    CamEstado.text = "Contraseña incorrecta"

                    Log.e(
                        "TRANSFERENCIA",
                        "Error al verificar contraseña",
                        error
                    )
                }
        }
    }
}
