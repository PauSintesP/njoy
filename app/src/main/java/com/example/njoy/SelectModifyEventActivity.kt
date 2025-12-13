package com.example.njoy

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.njoy.DataClasesApi.Event
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SelectModifyEventActivity : AppCompatActivity() {

    private lateinit var recyclerViewEventos: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmptyView: TextView
    private lateinit var btnBack: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_modify_event)

        initViews()
        setupListeners()
        fetchEventos()
    }

    private fun initViews() {
        recyclerViewEventos = findViewById(R.id.recyclerview_eventos)
        progressBar = findViewById(R.id.progressBar)
        tvEmptyView = findViewById(R.id.tvEmptyView)
        btnBack = findViewById(R.id.btnBack)

        recyclerViewEventos.layoutManager = LinearLayoutManager(this)
    }

    private fun setupListeners() {
        btnBack.setOnClickListener {
            finish()
        }
    }

    private fun fetchEventos() {
        showLoading(true)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = ApiClient.getApiService(this@SelectModifyEventActivity).getEventos()

                withContext(Dispatchers.Main) {
                    showLoading(false)
                    if (response.isSuccessful) {
                        val eventos = response.body()
                        if (eventos?.isNotEmpty() == true) {
                            setupRecyclerView(eventos)
                        } else {
                            showEmptyView("No se encontraron eventos.")
                        }
                    } else {
                        showEmptyView("Error al cargar los eventos: ${response.code()}")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showLoading(false)
                    showEmptyView("Error de conexión: ${e.message}")
                }
            }
        }
    }

    private fun setupRecyclerView(eventos: List<Event>) {
        val eventAdapter = EventAdapter(eventos, { eventId ->
            val intent = Intent(this, EventModifyActivity::class.java).apply {
                putExtra("EVENT_ID", eventId)
            }
            startActivity(intent)
        }, { imageView, imageUrl ->
            Glide.with(this)
                .load(imageUrl.takeIf { !it.isNullOrEmpty() }
                    ?: "https://talco-punkchanka.com/wp-content/uploads/2024/06/header.jpg")
                .placeholder(R.drawable.ic_event)
                .error(R.drawable.ic_event)
                .into(imageView)
        })
        recyclerViewEventos.adapter = eventAdapter
        hideEmptyView()
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        if (show) {
            recyclerViewEventos.visibility = View.GONE
            tvEmptyView.visibility = View.GONE
        }
    }

    private fun showEmptyView(message: String) {
        tvEmptyView.text = message
        tvEmptyView.visibility = View.VISIBLE
        recyclerViewEventos.visibility = View.GONE
    }

    private fun hideEmptyView() {
        tvEmptyView.visibility = View.GONE
        recyclerViewEventos.visibility = View.VISIBLE
    }
}