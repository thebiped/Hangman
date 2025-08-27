package com.example.hangman.ui.fragment

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.RenderEffect
import android.graphics.Shader
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.view.*
import android.view.animation.Animation
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import androidx.appcompat.widget.AppCompatButton
import androidx.fragment.app.Fragment
import com.example.hangman.R
import com.example.hangman.ui.LoginActivity

class ProfileFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_profile, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Botón para editar perfil, abre modal con campos editables
        view.findViewById<Button>(R.id.btnEditarPerfil).setOnClickListener {
            mostrarDialogoEditarPerfil()
        }

        // Botón para cerrar sesión, abre diálogo de confirmación
        view.findViewById<AppCompatButton>(R.id.btnCerrarSesion).setOnClickListener {
            mostrarDialogoCerrarSesion()
        }
    }

    private fun mostrarDialogoEditarPerfil() {
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_editar_perfil, null)

        val dialog = AlertDialog.Builder(requireContext(), R.style.CustomAlertDialogStyle)
            .setView(view)
            .setCancelable(false)
            .create()

        // Aplica blur al fondo o disminuye opacidad según versión Android
        val rootView = requireActivity().window.decorView.findViewById<ViewGroup>(android.R.id.content).getChildAt(0)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            rootView.setRenderEffect(RenderEffect.createBlurEffect(8f, 8f, Shader.TileMode.CLAMP))
        } else {
            rootView.alpha = 0.7f
        }

        dialog.setOnDismissListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                rootView.setRenderEffect(null)
            } else {
                rootView.alpha = 1f
            }
        }

        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setLayout((resources.displayMetrics.widthPixels * 0.85).toInt(), WindowManager.LayoutParams.WRAP_CONTENT)
            setGravity(Gravity.CENTER)
            setDimAmount(0.6f)
        }

        dialog.show()

        // Cierre del modal con icono de cerrar
        view.findViewById<ImageButton>(R.id.btnCerrarModal).setOnClickListener {
            dialog.dismiss()
        }

        // Confirmar cambios (lógica para guardar aún no implementada)
        view.findViewById<Button>(R.id.btnConfirmar).setOnClickListener {
            val nombre = view.findViewById<EditText>(R.id.etNombre).text.toString()
            val email = view.findViewById<EditText>(R.id.etEmail).text.toString()
            val descripcion = view.findViewById<EditText>(R.id.etDescripcion).text.toString()

            // Aquí puedes agregar la lógica para guardar los datos editados

            dialog.dismiss()
        }
    }

    private fun mostrarDialogoCerrarSesion() {
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_logout, null)

        val dialog = AlertDialog.Builder(requireContext(), R.style.CustomAlertDialogStyle)
            .setView(view)
            .setCancelable(false)
            .create()

        // Blur o reducción de opacidad al abrir modal
        val rootView = requireActivity().window.decorView.findViewById<ViewGroup>(android.R.id.content).getChildAt(0)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            rootView.setRenderEffect(RenderEffect.createBlurEffect(8f, 8f, Shader.TileMode.CLAMP))
        } else {
            rootView.alpha = 0.7f
        }

        dialog.setOnDismissListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                rootView.setRenderEffect(null)
            } else {
                rootView.alpha = 1f
            }
        }

        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setLayout((resources.displayMetrics.widthPixels * 0.85).toInt(), WindowManager.LayoutParams.WRAP_CONTENT)
            setGravity(Gravity.CENTER)
            setDimAmount(0.6f)
        }

        dialog.show()

        // Confirmación para cerrar sesión con animación de salida
        view.findViewById<Button>(R.id.btnCerrarSesion).setOnClickListener {
            val fadeOut = android.view.animation.AnimationUtils.loadAnimation(requireContext(), R.anim.fade_out)
            view.startAnimation(fadeOut)

            fadeOut.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation?) {}
                override fun onAnimationRepeat(animation: Animation?) {}

                override fun onAnimationEnd(animation: Animation?) {
                    dialog.dismiss()
                    cerrarSesion()
                }
            })
        }

        // Cierre simple del modal sin cerrar sesión
        view.findViewById<ImageButton>(R.id.btnCerrarModal).setOnClickListener {
            dialog.dismiss()
        }
        view.findViewById<Button>(R.id.btnCancelar).setOnClickListener {
            dialog.dismiss()
        }
    }

    private fun cerrarSesion() {
        // Limpia SharedPreferences y vuelve a LoginActivity limpiando backstack
        val sharedPref = requireActivity().getSharedPreferences("user_prefs", 0)
        sharedPref.edit().clear().apply()

        val intent = Intent(requireContext(), LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish()
    }
}
