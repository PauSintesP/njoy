package com.example.njoy

import android.app.AlertDialog
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.njoy.DataClasesApi.Event
import com.google.android.material.button.MaterialButton
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : BaseActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var btnMenu: ImageView

    private lateinit var recyclerViewEventos: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmptyView: TextView
    private lateinit var btnTodos: MaterialButton
    private lateinit var btnMusica: MaterialButton
    private lateinit var btnArte: MaterialButton
    private lateinit var btnTecnologia: MaterialButton
    private lateinit var btnComida: MaterialButton
    private lateinit var tvCategoryTitle: TextView
    
    private lateinit var eventAdapter: EventAdapter

    private var allEvents: List<Event> = listOf()
    private var currentCategory: String = "TODOS"

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        setupDrawer()
        setupListeners()
        setupBottomNavigation(R.id.nav_Main)
        // setupUserProfileClick() // Replaced by Drawer
        fetchEventos()
    }

    private fun initViews() {
        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.nav_view_drawer)
        btnMenu = findViewById(R.id.btn_menu)
        
        recyclerViewEventos = findViewById(R.id.recyclerview_eventos)
        progressBar = findViewById(R.id.progressBar)
        tvEmptyView = findViewById(R.id.tv_empty_view)
        
        btnTodos = findViewById(R.id.btn_todos)
        btnMusica = findViewById(R.id.btn_musica)
        btnArte = findViewById(R.id.btn_arte)
        btnTecnologia = findViewById(R.id.btn_tecnologia)
        btnComida = findViewById(R.id.btn_comida)
        tvCategoryTitle = findViewById(R.id.tv_category_title)
    }

    private fun setupDrawer() {
        navigationView.setNavigationItemSelectedListener(this)
        
        // Setup User Info in Header
        val headerView = navigationView.getHeaderView(0)
        val tvName = headerView.findViewById<TextView>(R.id.tv_nav_header_name)
        val tvEmail = headerView.findViewById<TextView>(R.id.tv_nav_header_email)
        
        val username = SessionManager.getUsername(this)
        val email = SessionManager.getEmail(this)
        
        tvName.text = username
        tvEmail.text = email

        // Setup Menu Visibility based on Role
        val menu = navigationView.menu
        val role = SessionManager.getUserRole(this)
        
        val managementSection = menu.findItem(R.id.menu_management_section)
        
        // Logic for visibility
        // Admin: All
        // Promotor: My Events, Create Event, Scanner
        // Scanner: Scanner only
        // User: None of management
        
        val showManagement = role in listOf("admin", "promotor", "owner", "scanner")
        managementSection.isVisible = showManagement
        
        if (showManagement) {
            menu.findItem(R.id.nav_admin_panel).isVisible = (role == "admin")
            menu.findItem(R.id.nav_create_event).isVisible = (role == "admin" || role == "promotor" || role == "owner")
            menu.findItem(R.id.nav_my_events).isVisible = (role == "admin" || role == "promotor" || role == "owner")
            menu.findItem(R.id.nav_scanner).isVisible = (role == "admin" || role == "promotor" || role == "owner" || role == "scanner")
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun setupListeners() {
        btnMenu.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }
        
        // Also open drawer on profile picture click for consistency
        findViewById<ImageView>(R.id.iv_user_profile).setOnClickListener {
             drawerLayout.openDrawer(GravityCompat.START)
        }

        btnTodos.setOnClickListener { selectCategory("TODOS", btnTodos) }
        btnMusica.setOnClickListener { selectCategory("MUSICA", btnMusica) }
        btnArte.setOnClickListener { selectCategory("ARTE", btnArte) }
        btnTecnologia.setOnClickListener { selectCategory("TECNOLOGIA", btnTecnologia) }
        btnComida.setOnClickListener { selectCategory("COMIDA", btnComida) }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_profile -> {
                 showUserProfileDialog()
            }
            R.id.nav_my_tickets -> {
                startActivity(Intent(this, TicketsActivity::class.java))
            }
            R.id.nav_my_events -> {
                // Assuming "Mis Eventos" goes to Modify List (SelectModifyEventActivity) 
                // or we could create a dedicated list for viewing only. 
                // For now, reusing SelectModifyEventActivity as it lists events.
                startActivity(Intent(this, SelectModifyEventActivity::class.java))
            }
            R.id.nav_create_event -> {
                startActivity(Intent(this, EventCreateActivity::class.java))
            }
            R.id.nav_scanner -> {
                startActivity(Intent(this, EscanearActivity::class.java))
            }
            R.id.nav_admin_panel -> {
                startActivity(Intent(this, AdminMainActivity::class.java))
            }
            R.id.nav_logout -> {
                logout()
            }
        }
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }
    
    private fun logout() {
        SessionManager.logout(this)
        val intent = Intent(this, WelcomeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun selectCategory(category: String, selectedButton: MaterialButton) {
        currentCategory = category
        
        val inactiveColor = getColorStateList(R.color.bg_card)
        val activeColor = getColorStateList(R.color.primary)
        val inactiveTextColor = getColorStateList(R.color.text_main)
        val activeTextColor = getColorStateList(R.color.white)

        listOf(btnTodos, btnMusica, btnArte, btnTecnologia, btnComida).forEach { btn ->
            btn.backgroundTintList = inactiveColor
            btn.setTextColor(inactiveTextColor)
        }

        selectedButton.backgroundTintList = activeColor
        selectedButton.setTextColor(activeTextColor)

        filterAndDisplayEvents()
    }

    private fun fetchEventos() {
        showLoading(true)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = ApiClient.getApiService(this@MainActivity).getEventos()
                withContext(Dispatchers.Main) {
                    showLoading(false)
                    if (response.isSuccessful) {
                        val eventos = response.body()
                        if (eventos?.isNotEmpty() == true) {
                            allEvents = eventos
                            filterAndDisplayEvents()
                        } else {
                            showEmptyView("No se encontraron eventos.")
                        }
                    } else {
                        showError("Error al cargar los eventos: ${response.code()}")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showLoading(false)
                    showError("Error de conexión: ${e.message}")
                }
            }
        }
    }

    private fun filterAndDisplayEvents() {
        if (allEvents.isEmpty()) {
            showEmptyView("No hay eventos disponibles")
            return
        }

        val filteredEvents = when (currentCategory) {
            "TODOS" -> allEvents
            "MUSICA" -> allEvents.filter { it.tipo.equals("concierto", ignoreCase = true) || it.tipo.equals("música", ignoreCase = true) }
            "ARTE" -> allEvents.filter { it.tipo.equals("arte", ignoreCase = true) || it.tipo.equals("exposición", ignoreCase = true) }
            "TECNOLOGIA" -> allEvents.filter { it.tipo.equals("tecnología", ignoreCase = true) || it.tipo.equals("conferencia", ignoreCase = true) }
            "COMIDA" -> allEvents.filter { it.tipo.equals("comida", ignoreCase = true) || it.tipo.equals("gastronómico", ignoreCase = true) }
            else -> allEvents
        }

        if (filteredEvents.isEmpty()) {
            showEmptyView("No hay eventos disponibles en esta categoría")
        } else {
            hideEmptyView()
            setupRecyclerView(filteredEvents)
        }
    }

    private fun setupRecyclerView(eventos: List<Event>) {
        eventAdapter = EventAdapter(eventos, { eventId ->
            val intent = Intent(this, EventActivity::class.java)
            intent.putExtra("EVENT_ID", eventId)
            startActivity(intent)
        }, { imageView, imageUrl ->
            loadImageWithGlide(imageView, imageUrl)
        })
        recyclerViewEventos.layoutManager = GridLayoutManager(this, 2)
        recyclerViewEventos.adapter = eventAdapter
    }

    fun loadImageWithGlide(imageView: ImageView, imageUrl: String?) {
        val url = imageUrl.takeIf { !it.isNullOrEmpty() }
            ?: "https://talco-punkchanka.com/wp-content/uploads/2024/06/header.jpg" 

        Glide.with(this)
            .load(url)
            .apply(RequestOptions()
                .placeholder(R.drawable.ic_event)
                .error(R.drawable.ic_event))
            .into(imageView)
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
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

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
    
    // Reuse existing dialog logic for Profile item in menu
    private fun showUserProfileDialog() {
        // ... (Keep existing implementation if desired, but maybe simplified as logout is now in menu)
        // Actually, let's keep it simple and just show info
        val user = SessionManager.getUser(this)
         val message = """
            Nombre: ${user?.nombre} ${user?.apellidos}
            Email: ${user?.email}
            Rol: ${user?.role}
        """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle("Mi Perfil")
            .setMessage(message)
            .setPositiveButton("Cerrar", null)
            .show()
    }
}