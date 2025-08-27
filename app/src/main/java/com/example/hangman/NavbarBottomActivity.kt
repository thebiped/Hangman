package com.example.hangman

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.hangman.ui.fragment.HomeFragment
import com.example.hangman.ui.fragment.RankingFragment
import com.example.hangman.ui.fragment.ProfileFragment
import com.example.hangman.databinding.ActivityNavbarbottomBinding

class NavbarBottomActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNavbarbottomBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inflar layout con ViewBinding
        binding = ActivityNavbarbottomBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Cargar fragmento inicial (Home)
        replaceFragment(HomeFragment())

        // Manejar selección del bottom navigation y reemplazar fragmentos según opción
        binding.bottomNavigation.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.nav_home -> replaceFragment(HomeFragment())
                R.id.nav_profile -> replaceFragment(ProfileFragment())
                R.id.nav_ranking -> replaceFragment(RankingFragment())
            }
            true
        }
    }

    // Función para reemplazar fragmento dentro del contenedor
    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
}
