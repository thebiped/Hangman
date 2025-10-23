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
import androidx.fragment.app.Fragment
import com.example.hangman.ModoClasicoActivity
import com.example.hangman.ModoContraRelojActivity
import com.example.hangman.R
import com.example.hangman.TematicaActivity
import com.example.hangman.ui.LoginActivity

class HomeFragment : Fragment() {

    // Inflamos el layout del fragmento
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        // Botón para jugar en modo clásico
        val btnJugarClasico = view.findViewById<Button>(R.id.btnJugarClasico)
        btnJugarClasico.setOnClickListener {
            val intent = Intent(requireContext(), ModoClasicoActivity::class.java)
            startActivity(intent)
        }

        // Botón para jugar en modo temático
        val btnModoTematico = view.findViewById<Button>(R.id.btnModoTematico)
        btnModoTematico.setOnClickListener {
            val intent = Intent(requireContext(), TematicaActivity::class.java)
            startActivity(intent)
        }

        // Botón para jugar en modo contrarreloj
        val btnModoReloj = view.findViewById<Button>(R.id.btnModoReloj)
        btnModoReloj.setOnClickListener {
            val intent = Intent(requireContext(), ModoContraRelojActivity::class.java)
            startActivity(intent)
        }


        return view
    }

    // Muestra un modal personalizado para cerrar sesión
    private fun mostrarDialogoCerrarSesion() {
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_logout, null)

        val dialog = AlertDialog.Builder(requireContext(), R.style.CustomAlertDialogStyle)
            .setView(view)
            .setCancelable(false)
            .create()

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

        view.findViewById<ImageButton>(R.id.btnCerrarModal).setOnClickListener {
            dialog.dismiss()
        }
        view.findViewById<Button>(R.id.btnCancelar).setOnClickListener {
            dialog.dismiss()
        }
    }

    // Cierra sesión y limpiar las preferencias
    private fun cerrarSesion() {
        val sharedPref = requireActivity().getSharedPreferences("user_prefs", 0)
        sharedPref.edit().clear().apply()

        val intent = Intent(requireContext(), LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish()
    }
}
