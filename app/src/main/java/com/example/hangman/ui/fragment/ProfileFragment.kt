package com.example.hangman.ui.fragment

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.AppCompatButton
import androidx.fragment.app.Fragment
import com.example.hangman.R

class ProfileFragment : Fragment() {

    private val PICK_IMAGE = 100
    private var selectedImageUri: Uri? = null

    // referencias al fragment
    private lateinit var txtNombre: TextView
    private lateinit var txtDescripcion: TextView
    private lateinit var imgPerfil: ImageView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_profile, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inicializamos referencias
        txtNombre = view.findViewById(R.id.txtNombre)
        txtDescripcion = view.findViewById(R.id.txtDescripcion)
        imgPerfil = view.findViewById(R.id.imgPerfil)

        // Botón para editar perfil
        view.findViewById<Button>(R.id.btnEditarPerfil).setOnClickListener {
            mostrarDialogoEditarPerfil()
        }

        // Botón para cerrar sesión
        view.findViewById<AppCompatButton>(R.id.btnCerrarSesion).setOnClickListener {
            mostrarDialogoEditarPerfil()
        }
    }

    private fun mostrarDialogoEditarPerfil() {
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_editar_perfil, null)

        val dialog = AlertDialog.Builder(requireContext(), R.style.CustomAlertDialogStyle)
            .setView(view)
            .setCancelable(false)
            .create()

        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setLayout((resources.displayMetrics.widthPixels * 0.85).toInt(), WindowManager.LayoutParams.WRAP_CONTENT)
            setGravity(Gravity.CENTER)
            setDimAmount(0.6f)
        }

        dialog.show()

        val imgPerfilModal = view.findViewById<ImageView>(R.id.imgPerfil)
        val btnCambiarFoto = view.findViewById<ImageView>(R.id.btnCambiarFoto)
        val etNombre = view.findViewById<EditText>(R.id.etNombre)
        val etDescripcion = view.findViewById<EditText>(R.id.etDescripcion)

        // Mostrar datos actuales en el modal
        etNombre.setText(txtNombre.text)
        etDescripcion.setText(txtDescripcion.text)
        selectedImageUri?.let { imgPerfilModal.setImageURI(it) }

        // Cambiar foto
        btnCambiarFoto.setOnClickListener {
            val gallery = Intent(Intent.ACTION_PICK)
            gallery.type = "image/*"
            startActivityForResult(gallery, PICK_IMAGE)
        }

        // Cerrar modal
        view.findViewById<ImageButton>(R.id.btnCerrarModal).setOnClickListener {
            dialog.dismiss()
        }

        // Confirmar cambios
        view.findViewById<Button>(R.id.btnConfirmar).setOnClickListener {
            val nuevoNombre = etNombre.text.toString()
            val nuevaDescripcion = etDescripcion.text.toString()

            // Actualizamos en el fragment
            txtNombre.text = nuevoNombre
            txtDescripcion.text = nuevaDescripcion

            selectedImageUri?.let { uri ->
                imgPerfil.setImageURI(uri)
            }

            dialog.dismiss()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE && resultCode == Activity.RESULT_OK) {
            selectedImageUri = data?.data
        }
    }
}

