package com.example.njoy

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

abstract class BaseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemUI()
        }
    }

    private fun hideSystemUI() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let {
                it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                it.hide(WindowInsets.Type.statusBars())
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_FULLSCREEN
                    )
        }
    }

    protected fun setupBottomNavigation(selectedItemId: Int) {
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigation?.apply {

            menu.findItem(selectedItemId).isChecked = true

            setOnItemSelectedListener { item ->
                if (selectedItemId != item.itemId) {
                    when (item.itemId) {
                        R.id.nav_Main -> {
                            startActivity(Intent(this@BaseActivity, MainActivity::class.java))
                            overridePendingTransition(0, 0)
                        }
                        R.id.nav_tickets -> {
                            startActivity(Intent(this@BaseActivity, TicketsActivity::class.java))
                            overridePendingTransition(0, 0)
                        }
                        R.id.nav_history -> {
                            startActivity(Intent(this@BaseActivity, HistoryPaymentsActivity::class.java))
                            overridePendingTransition(0, 0)
                        }
                    }
                }
                true
            }
        }
    }
}