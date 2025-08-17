package com.example.protosApp

import android.os.Bundle
import android.app.DatePickerDialog
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import java.text.SimpleDateFormat
import java.util.Locale
import com.example.protosApp.data.api.PushNotificationApi
import com.example.protosApp.data.models.BirthdayCrud
import com.example.protosApp.data.models.BirthdayResponse
import com.example.protosApp.utils.AppConfig
import com.google.android.material.navigation.NavigationView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import java.util.Date
import java.util.TimeZone
import android.util.Log

class BirthdayManagementActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView
    private lateinit var toggle: ActionBarDrawerToggle

    private lateinit var nameEditText: EditText
    private lateinit var dateEditText: EditText

    private lateinit var saveButton: Button
    private lateinit var clearButton: Button

    private lateinit var statusTextView: TextView
    private lateinit var upcomingRecyclerView: RecyclerView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private var crudAdapter: BirthdayCrudAdapter? = null
    private var swipeEnabled: Boolean = false
    private var selectedId: Int? = null
    private var selectedOriginalName: String? = null
    private var selectedOriginalDateIso: String? = null // yyyy-MM-dd
    private var selectedFromItem: Boolean = false

    private lateinit var api: PushNotificationApi

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_birthday_management)

        // Retrofit
        val dateDeserializer = JsonDeserializer { json: JsonElement, _, _: JsonDeserializationContext ->
            val s = json.asString
            val patterns = arrayOf(
                "yyyy-MM-dd'T'HH:mm:ss.SSSX",
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                "yyyy-MM-dd'T'HH:mm:ssX",
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
                "yyyy-MM-dd'T'HH:mm:ss",
                "yyyy-MM-dd"
            )
            for (p in patterns) {
                try {
                    val fmt = SimpleDateFormat(p, Locale.US).apply {
                        timeZone = TimeZone.getTimeZone("UTC")
                        isLenient = true
                    }
                    return@JsonDeserializer fmt.parse(s)
                } catch (_: Exception) { /* try next */ }
            }
            throw JsonParseException("Unparseable date: $s")
        }

        val gson = GsonBuilder()
            .registerTypeAdapter(Date::class.java, dateDeserializer)
            .setLenient()
            .create()

        val retrofit = Retrofit.Builder()
            .baseUrl(AppConfig.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
        api = retrofit.create(PushNotificationApi::class.java)

        initializeViews()
        setupDrawer()
        setupRecycler()
        setupClicks()
        loadUpcoming()
    }

    private fun initializeViews() {
        drawerLayout = findViewById(R.id.drawer_layout)
        navView = findViewById(R.id.nav_view)

        nameEditText = findViewById<EditText>(R.id.birthdayNameEditText)
        dateEditText = findViewById<EditText>(R.id.birthdayDateEditText)

        saveButton = findViewById<Button>(R.id.saveButton)
        clearButton = findViewById<Button>(R.id.clearButton)

        statusTextView = findViewById<TextView>(R.id.statusTextView)
        upcomingRecyclerView = findViewById<RecyclerView>(R.id.upcomingRecyclerView)
        swipeRefreshLayout = findViewById<SwipeRefreshLayout>(R.id.swipeRefreshLayout)
    }

    private fun setupDrawer() {
        toggle = ActionBarDrawerToggle(this, drawerLayout, R.string.app_name, R.string.app_name)
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)

        navView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(android.content.Intent(this, HomeActivity::class.java))
                    drawerLayout.closeDrawers()
                    true
                }
                R.id.nav_manage_birthdays -> {
                    // Already here
                    drawerLayout.closeDrawers()
                    true
                }
                R.id.nav_settings -> {
                    startActivity(android.content.Intent(this, MainActivity::class.java))
                    drawerLayout.closeDrawers()
                    true
                }
                else -> false
            }
        }
    }

    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        if (::toggle.isInitialized && toggle.onOptionsItemSelected(item)) return true
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        if (::drawerLayout.isInitialized && drawerLayout.isDrawerOpen(androidx.core.view.GravityCompat.START)) {
            drawerLayout.closeDrawer(androidx.core.view.GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    private fun setupRecycler() {
        upcomingRecyclerView.layoutManager = LinearLayoutManager(this)
        // Swipe to delete callback; enabled only when items have IDs
        val swipeCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean = false
            override fun onSwiped(vh: RecyclerView.ViewHolder, dir: Int) {
                val adapter = crudAdapter ?: return
                val position = vh.adapterPosition
                if (position == RecyclerView.NO_POSITION) {
                    return
                }
                val item = adapter.getItem(position)
                val id = item.id
                if (id == null) {
                    showStatus("Cannot delete: missing ID (use list endpoint)")
                    adapter.notifyItemChanged(position)
                    return
                }
                api.deleteBirthday(id).enqueue(object: Callback<Void> {
                    override fun onResponse(call: Call<Void>, response: Response<Void>) {
                        if (response.isSuccessful) {
                            adapter.removeAt(position)
                            showToast("Deleted birthday $id")
                        } else {
                            showStatus("Delete failed (${response.code()})")
                            adapter.notifyItemChanged(position)
                        }
                    }
                    override fun onFailure(call: Call<Void>, t: Throwable) {
                        showStatus("Network error: ${t.message}")
                        adapter.notifyItemChanged(position)
                    }
                })
            }
            override fun isItemViewSwipeEnabled(): Boolean = swipeEnabled
        }
        ItemTouchHelper(swipeCallback).attachToRecyclerView(upcomingRecyclerView)
    }

    private fun setupClicks() {
        // Date picker
        dateEditText.setOnClickListener {
            val cal = java.util.Calendar.getInstance()
            // Try to initialize the picker with the current field value (dd/MM/yyyy)
            val current = dateEditText.text?.toString()?.trim()
            if (!current.isNullOrEmpty()) {
                try {
                    val fmt = SimpleDateFormat("dd/MM/yyyy", Locale.US)
                    val parsed = fmt.parse(current)
                    if (parsed != null) cal.time = parsed
                } catch (_: Exception) { /* ignore and use today */ }
            }
            val dlg = DatePickerDialog(this,
                { _, y, m, d ->
                    val month = (m + 1).toString().padStart(2, '0')
                    val day = d.toString().padStart(2, '0')
                    dateEditText.setText("$day/$month/$y")
                },
                cal.get(java.util.Calendar.YEAR),
                cal.get(java.util.Calendar.MONTH),
                cal.get(java.util.Calendar.DAY_OF_MONTH)
            )
            dlg.show()
        }

        // Save: if an item with ID is selected -> update by ID; otherwise create
        saveButton.setOnClickListener {
            val name = nameEditText.text.toString().trim()
            val date = dateEditText.text.toString().trim()
            if (name.isEmpty() || date.isEmpty()) {
                showStatus("Name and Date are required (dd/MM/yyyy)")
                return@setOnClickListener
            }
            val backendDate = toBackendDate(date)
            val currentId = selectedId
            if (currentId != null) {
                enqueueUpdate(currentId, name, backendDate)
            } else {
                // If UI indicates update but we lost the ID, do NOT create to avoid duplicates
                val expectsUpdate = saveButton.text.toString().equals(getString(R.string.update), ignoreCase = true)
                if (expectsUpdate) {
                    showStatus("Cannot update: missing ID. Pull to refresh and reselect the item.")
                    return@setOnClickListener
                }
                // No ID and not expecting update -> create a new entry
                enqueueCreate(name, backendDate)
            }
        }

        clearButton.setOnClickListener { clearForm() }

        // Pull-to-refresh
        swipeRefreshLayout.setOnRefreshListener {
            loadUpcoming()
        }
    }

    private fun loadUpcoming() {
        statusTextView.text = "Loading..."
        statusTextView.visibility = View.VISIBLE
        upcomingRecyclerView.visibility = View.GONE
        swipeRefreshLayout.setRefreshing(true)

        // Prefer full list with IDs for swipe; fallback to upcoming
        api.listBirthdays().enqueue(object: Callback<List<com.example.protosApp.data.models.BirthdayCrud>> {
            override fun onResponse(call: Call<List<com.example.protosApp.data.models.BirthdayCrud>>, response: Response<List<com.example.protosApp.data.models.BirthdayCrud>>) {
                if (response.isSuccessful) {
                    val items = response.body() ?: emptyList()
                    crudAdapter = BirthdayCrudAdapter(items.toMutableList(), onClick = { item ->
            // Select item for potential update
            selectedId = item.id
            nameEditText.setText(item.name)
            dateEditText.setText(toUiDate(item.birthDate))
            saveButton.text = getString(R.string.update)
        }, onDelete = { item ->
            val id = item.id
            if (id == null) {
                showStatus("Cannot delete: missing ID")
            } else {
                api.deleteBirthday(id).enqueue(object: Callback<Void> {
                override fun onResponse(call: Call<Void>, response: Response<Void>) {
                    if (response.isSuccessful) {
                        crudAdapter?.removeById(id)
                        showToast("Deleted birthday $id")
                    } else {
                        showStatus("Delete failed (${response.code()})")
                    }
                }
                override fun onFailure(call: Call<Void>, t: Throwable) {
                    showStatus("Network error: ${t.message}")
                }
                })
            }
        })
                    upcomingRecyclerView.adapter = crudAdapter
                    swipeEnabled = true
                    statusTextView.visibility = View.GONE
                    upcomingRecyclerView.visibility = View.VISIBLE
                    swipeRefreshLayout.setRefreshing(false)
                    attachSwipeToDelete()
                } else {
                    showStatus("Full list failed (${response.code()}), showing upcoming (no IDs)")
                    // Fallback to upcoming (no IDs, no swipe)
                    loadUpcomingFallback(30)
                }
            }
            override fun onFailure(call: Call<List<com.example.protosApp.data.models.BirthdayCrud>>, t: Throwable) {
                showStatus("Full list error: ${t.message}. Showing upcoming (no IDs)")
                // Fallback to upcoming (no IDs, no swipe)
                loadUpcomingFallback(30)
            }
        })
    }

    // Resolve by the ORIGINAL selection (name + yyyy-MM-dd at click time) and update that record's ID
    private fun tryResolveFromOriginalAndSave(
        originalName: String,
        originalDateIso: String,
        newName: String,
        newBackendDate: String,
        createIfNotFound: Boolean = true
    ) {
        api.listBirthdays().enqueue(object: Callback<List<com.example.protosApp.data.models.BirthdayCrud>> {
            override fun onResponse(
                call: Call<List<com.example.protosApp.data.models.BirthdayCrud>>,
                response: Response<List<com.example.protosApp.data.models.BirthdayCrud>>
            ) {
                if (response.isSuccessful) {
                    val existing = response.body()?.firstOrNull {
                        it.name.trim() == originalName && it.birthDate.substringBefore('T') == originalDateIso
                    }
                    if (existing?.id != null) {
                        enqueueUpdate(existing.id, newName, newBackendDate)
                    } else {
                        // If original not found anymore
                        if (createIfNotFound) {
                            tryResolveAndSave(newName, newBackendDate, createIfNotFound = true)
                        } else {
                            showStatus("Could not find the original record to update. Pull to refresh and try again.")
                        }
                    }
                } else {
                    if (createIfNotFound) {
                        tryResolveAndSave(newName, newBackendDate, createIfNotFound = true)
                    } else {
                        showStatus("Unable to resolve record for update (server error). Pull to refresh and try again.")
                    }
                }
            }
            override fun onFailure(
                call: Call<List<com.example.protosApp.data.models.BirthdayCrud>>,
                t: Throwable
            ) {
                if (createIfNotFound) {
                    tryResolveAndSave(newName, newBackendDate, createIfNotFound = true)
                } else {
                    showStatus("Network error while resolving record to update. Pull to refresh and try again.")
                }
            }
        })
    }

    private fun enqueueUpdate(id: Int, name: String, backendDate: String) {
        Log.d("Birthday", "Updating id=$id name='$name' birthDate='$backendDate'")
        val body = BirthdayCrud(id = id, name = name, birthDate = backendDate, userId = null)
        api.updateBirthday(id, body).enqueue(object: Callback<BirthdayCrud> {
            override fun onResponse(call: Call<BirthdayCrud>, response: Response<BirthdayCrud>) {
                if (response.isSuccessful) {
                    showToast("Birthday updated")
                    clearForm()
                    loadUpcoming()
                } else {
                    val err = try { response.errorBody()?.string() } catch (e: Exception) { e.message }
                    Log.e("Birthday", "Update failed code=${response.code()} body=${err}")
                    showStatus("Update failed (${response.code()}): ${err ?: ""}")
                }
            }
            override fun onFailure(call: Call<BirthdayCrud>, t: Throwable) {
                Log.e("Birthday", "Update error", t)
                showStatus("Update error: ${t.message}")
            }
        })
    }

    private fun enqueueCreate(name: String, backendDate: String) {
        Log.d("Birthday", "Creating name='$name' birthDate='$backendDate'")
        val body = BirthdayCrud(name = name, birthDate = backendDate, userId = null)
        api.createBirthday(body).enqueue(object: Callback<BirthdayCrud> {
            override fun onResponse(call: Call<BirthdayCrud>, response: Response<BirthdayCrud>) {
                if (response.isSuccessful) {
                    showToast("Birthday created")
                    clearForm()
                    loadUpcoming()
                } else {
                    val err = try { response.errorBody()?.string() } catch (e: Exception) { e.message }
                    Log.e("Birthday", "Create failed code=${response.code()} body=${err}")
                    showStatus("Create failed (${response.code()}): ${err ?: ""}")
                }
            }
            override fun onFailure(call: Call<BirthdayCrud>, t: Throwable) {
                Log.e("Birthday", "Create error", t)
                showStatus("Create error: ${t.message}")
            }
        })
    }

    // Convert Birthday items to BirthdayCrud format so we always use the CRUD adapter with delete
    private fun useSimpleUpcomingAdapter(items: List<Birthday>) {
        val crudItems = items.map { b ->
            val dateStr = if (b.date != null) {
                SimpleDateFormat("yyyy-MM-dd", Locale.US).format(b.date)
            } else ""
            BirthdayCrud(
                id = b.id,
                name = b.name,
                birthDate = dateStr,
                userId = null
            )
        }
        
        crudAdapter = BirthdayCrudAdapter(crudItems.toMutableList(), onClick = { item ->
            // Prefill form; prefer updating by ID if present
            nameEditText.setText(item.name)
            dateEditText.setText(toUiDate(item.birthDate))
            selectedId = item.id
            selectedOriginalName = item.name
            selectedOriginalDateIso = item.birthDate.substringBefore('T')
            selectedFromItem = true

            if (item.id != null) {
                saveButton.text = getString(R.string.update)
                showStatus("Selected existing item (ID=${item.id}). You can update it.")
            } else {
                saveButton.text = getString(R.string.save)
                showStatus("Selected item has no ID; press Save to create a new entry or pull to refresh to enable updates by ID.")
            }
        }, onDelete = { item ->
            val id = item.id
            if (id == null) {
                showStatus("Cannot delete: missing ID")
            } else {
                api.deleteBirthday(id).enqueue(object: Callback<Void> {
                    override fun onResponse(call: Call<Void>, response: Response<Void>) {
                        if (response.isSuccessful) {
                            crudAdapter?.removeById(id)
                            showToast("Deleted birthday $id")
                        } else {
                            showStatus("Delete failed (${response.code()})")
                        }
                    }
                    override fun onFailure(call: Call<Void>, t: Throwable) {
                        showStatus("Network error: ${t.message}")
                    }
                })
            }
        })
        
        upcomingRecyclerView.adapter = crudAdapter
        swipeEnabled = true
        statusTextView.visibility = View.GONE
        upcomingRecyclerView.visibility = View.VISIBLE
        showStatus("Showing upcoming items with delete enabled")
        swipeRefreshLayout.setRefreshing(false)
        attachSwipeToDelete()
    }

    // Resolve an existing record by name + birthDate (date-only) to avoid duplicates when selectedId is null
    private fun tryResolveAndSave(name: String, backendDate: String, createIfNotFound: Boolean = true) {
        val dateOnly = backendDate.substringBefore('T')
        api.listBirthdays().enqueue(object: Callback<List<com.example.protosApp.data.models.BirthdayCrud>> {
            override fun onResponse(
                call: Call<List<com.example.protosApp.data.models.BirthdayCrud>>,
                response: Response<List<com.example.protosApp.data.models.BirthdayCrud>>
            ) {
                if (response.isSuccessful) {
                    val existing = response.body()?.firstOrNull {
                        it.name.trim() == name && it.birthDate.substringBefore('T') == dateOnly
                    }
                    if (existing?.id != null) {
                        // Update existing
                        enqueueUpdate(existing.id, name, backendDate)
                    } else {
                        if (createIfNotFound) {
                            enqueueCreate(name, backendDate)
                        } else {
                            showStatus("Could not find the original record to update. Pull to refresh and try again.")
                        }
                    }
                } else {
                    if (createIfNotFound) {
                        enqueueCreate(name, backendDate)
                    } else {
                        showStatus("Unable to resolve record for update (server error). Pull to refresh and try again.")
                    }
                }
            }
            override fun onFailure(
                call: Call<List<com.example.protosApp.data.models.BirthdayCrud>>,
                t: Throwable
            ) {
                if (createIfNotFound) {
                    enqueueCreate(name, backendDate)
                } else {
                    showStatus("Network error while resolving record to update. Pull to refresh and try again.")
                }
            }
        })
    }

    private fun loadUpcomingFallback(days: Int) {
        api.getUpcomingBirthdays(days = days, userId = null).enqueue(object: Callback<BirthdayResponse> {
            override fun onResponse(call: Call<BirthdayResponse>, response: Response<BirthdayResponse>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    val upcomingItems = response.body()?.data ?: emptyList()
                    // Try to enrich with IDs by fetching full list
                    api.listBirthdays().enqueue(object: Callback<List<com.example.protosApp.data.models.BirthdayCrud>> {
                        override fun onResponse(call: Call<List<com.example.protosApp.data.models.BirthdayCrud>>, resp2: Response<List<com.example.protosApp.data.models.BirthdayCrud>>) {
                            if (resp2.isSuccessful) {
                                val full = resp2.body() ?: emptyList()
                                val fmtKey = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                                val fullMap = full.associateBy { it.name.trim() + "|" + it.birthDate.substringBefore('T') }
                                val mapped: MutableList<com.example.protosApp.data.models.BirthdayCrud> = mutableListOf()
                                for (b in upcomingItems) {
                                    val key = b.name.trim() + "|" + (if (b.date != null) fmtKey.format(b.date) else "")
                                    val match = fullMap[key]
                                    if (match != null) {
                                        mapped.add(match)
                                    } else {
                                        // Fallback entry without ID so clicks still prefill, but swipe will bounce
                                        val uiFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                                        val dateStr = if (b.date != null) uiFmt.format(b.date) else ""
                                        mapped.add(com.example.protosApp.data.models.BirthdayCrud(
                                            id = null,
                                            name = b.name,
                                            birthDate = dateStr,
                                            userId = null
                                        ))
                                    }
                                }
                                crudAdapter = BirthdayCrudAdapter(mapped, onClick = { item ->
                                    selectedId = item.id
                                    selectedOriginalName = item.name
                                    selectedOriginalDateIso = item.birthDate.substringBefore('T')
                                    selectedFromItem = true
                                    nameEditText.setText(item.name)
                                    dateEditText.setText(toUiDate(item.birthDate))
                                    saveButton.text = getString(R.string.update)
                                }, onDelete = { item ->
                                    val id = item.id
                                    if (id == null) {
                                        showStatus("Cannot delete: missing ID")
                                    } else {
                                        api.deleteBirthday(id).enqueue(object: Callback<Void> {
                                            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                                                if (response.isSuccessful) {
                                                    crudAdapter?.removeById(id)
                                                    showToast("Deleted birthday $id")
                                                } else {
                                                    showStatus("Delete failed (${response.code()})")
                                                }
                                            }
                                            override fun onFailure(call: Call<Void>, t: Throwable) {
                                                showStatus("Network error: ${t.message}")
                                            }
                                        })
                                    }
                                })
                                upcomingRecyclerView.adapter = crudAdapter
                                swipeEnabled = true
                                statusTextView.visibility = View.GONE
                                upcomingRecyclerView.visibility = View.VISIBLE
                                swipeRefreshLayout.setRefreshing(false)
                                attachSwipeToDelete()
                            } else {
                                // Fall back to non-ID adapter but keep items clickable to prefill
                                useSimpleUpcomingAdapter(upcomingItems)
                            }
                        }
                        override fun onFailure(call: Call<List<com.example.protosApp.data.models.BirthdayCrud>>, t: Throwable) {
                            useSimpleUpcomingAdapter(upcomingItems)
                        }
                    })
                } else {
                    statusTextView.text = "Failed to load data (${response.code()})"
                    statusTextView.visibility = View.VISIBLE
                }
            }
            override fun onFailure(call: Call<BirthdayResponse>, t: Throwable) {
                statusTextView.text = "Network error: ${t.message}"
                statusTextView.visibility = View.VISIBLE
                swipeRefreshLayout.setRefreshing(false)
            }
        })
    }

    private fun <T> simpleCallback(
        onSuccess: (T) -> Unit,
        onError: (Int) -> Unit
    ): Callback<T> = object: Callback<T> {
        override fun onResponse(call: Call<T>, response: Response<T>) {
            if (response.isSuccessful) {
                response.body()?.let(onSuccess) ?: onError(500)
            } else onError(response.code())
        }
        override fun onFailure(call: Call<T>, t: Throwable) {
            showStatus("Network error: ${t.message}")
        }
    }

    private fun showToast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    private fun showStatus(msg: String) {
        statusTextView.text = msg
        statusTextView.visibility = View.VISIBLE
    }

    private fun clearForm() {
        nameEditText.setText("")
        dateEditText.setText("")
        selectedId = null
        selectedOriginalName = null
        selectedOriginalDateIso = null
        selectedFromItem = false
        // reset button label back to Save
        saveButton.text = getString(R.string.save)
        // keep userId and days fields as-is
    }

    // Converts UI input (dd/MM/yyyy) to backend-friendly format (yyyy-MM-dd'T'00:00:00)
    private fun toBackendDate(uiDate: String): String {
        return try {
            val inFmt = SimpleDateFormat("dd/MM/yyyy", Locale.US).apply { isLenient = false }
            val parsed = inFmt.parse(uiDate)
            val outFmt = SimpleDateFormat("yyyy-MM-dd'T'00:00:00", Locale.US)
            outFmt.format(parsed!!)
        } catch (e: Exception) {
            // Fallback to raw value if parsing fails
            uiDate
        }
    }

    // Converts backend date string to UI (dd/MM/yyyy). Accepts formats with 'T' and time.
    private fun toUiDate(serverDate: String): String {
        return try {
            val base = when {
                serverDate.contains('T') -> serverDate.substringBefore('T')
                serverDate.length >= 10 -> serverDate.substring(0, 10)
                else -> serverDate
            }
            val inFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val parsed = inFmt.parse(base)
            val outFmt = SimpleDateFormat("dd/MM/yyyy", Locale.US)
            outFmt.format(parsed!!)
        } catch (e: Exception) { serverDate }
    }

    private fun attachSwipeToDelete() {
        // Prevent attaching when swipe should be disabled
        if (!swipeEnabled) return
        val adapter = crudAdapter ?: return
        val callback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                if (position == RecyclerView.NO_POSITION) return
                val item = adapter.getItem(position)
                val id = item.id
                if (id == null) {
                    // No ID, can't delete; restore item
                    this@BirthdayManagementActivity.crudAdapter?.notifyItemChanged(position)
                    showStatus("Cannot delete: missing ID")
                    return
                }
                api.deleteBirthday(id).enqueue(object: Callback<Void> {
                    override fun onResponse(call: Call<Void>, response: Response<Void>) {
                        if (response.isSuccessful) {
                            this@BirthdayManagementActivity.crudAdapter?.removeById(id)
                            showToast("Deleted birthday $id")
                        } else {
                            showStatus("Delete failed (${response.code()})")
                            this@BirthdayManagementActivity.crudAdapter?.notifyItemChanged(position)
                        }
                    }
                    override fun onFailure(call: Call<Void>, t: Throwable) {
                        showStatus("Network error: ${t.message}")
                        this@BirthdayManagementActivity.crudAdapter?.notifyItemChanged(position)
                    }
                })
            }
        }
        ItemTouchHelper(callback).attachToRecyclerView(upcomingRecyclerView)
    }
}
