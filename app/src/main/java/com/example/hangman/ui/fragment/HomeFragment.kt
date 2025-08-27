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
import android.widget.ImageButton
import androidx.appcompat.widget.AppCompatButton
import androidx.fragment.app.Fragment
import com.example.hangman.ModoClasicoActivity
import com.example.hangman.R
import com.example.hangman.ui.LoginActivity

class HomeFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        // Botón para iniciar juego clásico
        val btnJugarClasico = view.findViewById<Button>(R.id.btnJugarClasico)
        btnJugarClasico.setOnClickListener {
            val intent = Intent(requireContext(), ModoClasicoActivity::class.java)
            startActivity(intent)
        }

        // Botón para cerrar sesión, que muestra un diálogo de confirmación
        val btnCerrarSesion = view.findViewById<AppCompatButton>(R.id.btnCerrarSesion)
        btnCerrarSesion.setOnClickListener {
            mostrarDialogoCerrarSesion()
        }

        return view
    }

    private fun mostrarDialogoCerrarSesion() {
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_logout, null)

        val dialog = AlertDialog.Builder(requireContext(), R.style.CustomAlertDialogStyle)
            .setView(view)
            .setCancelable(false)
            .create()

        // Aplica blur al fondo en Android 12+ o reduce opacidad en versiones anteriores
        val rootView = requireActivity().window.decorView.findViewById<ViewGroup>(android.R.id.content).getChildAt(0)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            rootView.setRenderEffect(RenderEffect.createBlurEffect(8f, 8f, Shader.TileMode.CLAMP))
        } else {
            rootView.alpha = 0.7f
        }

        // Restaurar estado del fondo al cerrar el diálogo
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

        // Animación de salida y cierre de sesión cuando se confirma
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

        // Cierre del diálogo sin cerrar sesión
        view.findViewById<ImageButton>(R.id.btnCerrarModal).setOnClickListener {
            dialog.dismiss()
        }
        view.findViewById<Button>(R.id.btnCancelar).setOnClickListener {
            dialog.dismiss()
        }
    }

    private fun cerrarSesion() {
        // Limpia datos guardados y redirige a LoginActivity, cerrando todas las actividades anteriores
        val sharedPref = requireActivity().getSharedPreferences("user_prefs", 0)
        sharedPref.edit().clear().apply()

        val intent = Intent(requireContext(), LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish()
    }
}
