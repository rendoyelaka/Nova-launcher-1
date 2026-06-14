package com.cristal.bristral.tristal.mistral

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText
import com.cristal.bristral.tristal.mistral.adapter.AppGridAdapter
import com.cristal.bristral.tristal.mistral.model.AppInfo
import com.cristal.bristral.tristal.mistral.utils.AppLoader

class AppDrawerActivity : AppCompatActivity(), AppGridAdapter.OnAppClickListener {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var rvApps: RecyclerView
    private lateinit var etSearch: TextInputEditText
    private lateinit var tvNoApps: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var chipGroup: ChipGroup
    private lateinit var appAdapter: AppGridAdapter
    private var allApps = listOf<AppInfo>()
    private var currentFilter = "all"
    private lateinit var gestureDetector: GestureDetector

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_drawer)
        initViews()
        setupGestures()
        setupChips()
        loadApps()
        intent.getStringExtra("search_query")?.let { query ->
            etSearch.setText(query)
            etSearch.setSelection(query.length)
        }
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar_drawer)
        rvApps = findViewById(R.id.rv_apps)
        etSearch = findViewById(R.id.et_search_drawer)
        tvNoApps = findViewById(R.id.tv_no_apps)
        progressBar = findViewById(R.id.progress_bar)
        chipGroup = findViewById(R.id.chip_group_drawer)
        rvApps.layoutManager = GridLayoutManager(this, 4)

        toolbar.setNavigationOnClickListener {
            finish()
            overridePendingTransition(R.anim.fade_in, R.anim.slide_down)
        }

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterApps(s.toString().trim())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupChips() {
        chipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isEmpty()) return@setOnCheckedStateChangeListener
            currentFilter = when (checkedIds[0]) {
                R.id.chip_all_drawer -> "all"
                R.id.chip_user_drawer -> "user"
                R.id.chip_system_drawer -> "system"
                R.id.chip_recent_drawer -> "recent"
                else -> "all"
            }
            filterApps(etSearch.text.toString().trim())
        }
    }

    private fun setupGestures() {
        gestureDetector = GestureDetector(this,
            object : GestureDetector.SimpleOnGestureListener() {
                override fun onFling(
                    e1: MotionEvent?,
                    e2: MotionEvent,
                    velocityX: Float,
                    velocityY: Float
                ): Boolean {
                    val diffY = e2.y - (e1?.y ?: 0f)
                    if (diffY > 200 && Math.abs(velocityY) > 100) {
                        finish()
                        overridePendingTransition(R.anim.fade_in, R.anim.slide_down)
                        return true
                    }
                    return false
                }
            })
        findViewById<View>(R.id.root_drawer).setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            false
        }
    }

    private fun loadApps() {
        progressBar.visibility = View.VISIBLE
        rvApps.visibility = View.GONE
        Thread {
            allApps = AppLoader.getAllInstalledApps(this)
            runOnUiThread {
                progressBar.visibility = View.GONE
                rvApps.visibility = View.VISIBLE
                appAdapter = AppGridAdapter(this, allApps.toMutableList(), this)
                rvApps.adapter = appAdapter
                val query = etSearch.text.toString().trim()
                if (query.isNotEmpty()) filterApps(query)
            }
        }.start()
    }

    private fun filterApps(query: String) {
        var filtered = when (currentFilter) {
            "user" -> allApps.filter { !it.isSystemApp }
            "system" -> allApps.filter { it.isSystemApp }
            "recent" -> allApps.sortedByDescending { it.lastUpdateTime }
            else -> allApps
        }
        if (query.isNotEmpty()) {
            filtered = filtered.filter {
                it.name.contains(query, ignoreCase = true) ||
                        it.packageName.contains(query, ignoreCase = true)
            }
        }
        tvNoApps.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
        appAdapter.updateList(filtered.toMutableList())
    }

    override fun onAppClick(appInfo: AppInfo) {
        try {
            val intent = packageManager.getLaunchIntentForPackage(appInfo.packageName)
            if (intent != null) startActivity(intent)
            else Toast.makeText(this, "Cannot launch ${appInfo.name}", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Error launching app", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onAppLongClick(appInfo: AppInfo) {
        val intent = Intent(this, AppDetailActivity::class.java)
        intent.putExtra("package_name", appInfo.packageName)
        startActivity(intent)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.fade_in, R.anim.slide_down)
    }
}
