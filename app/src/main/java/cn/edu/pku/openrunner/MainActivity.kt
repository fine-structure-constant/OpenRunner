package cn.edu.pku.openrunner

import android.content.Intent
import android.app.Activity
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.commit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.navigation.NavigationView
import cn.edu.pku.openrunner.feature.auth.ui.AuthActivity
import cn.edu.pku.openrunner.core.session.SessionStore
import cn.edu.pku.openrunner.feature.account.ui.AccountFragment
import cn.edu.pku.openrunner.feature.account.ui.DrawerSummaryState
import cn.edu.pku.openrunner.feature.account.ui.DrawerSummaryViewModel
import cn.edu.pku.openrunner.feature.records.ui.RecordListFragment
import cn.edu.pku.openrunner.feature.run.ui.RunFragment
import cn.edu.pku.openrunner.feature.run.ui.VirtualLocationFragment
import cn.edu.pku.openrunner.feature.run.data.LocalRunRecordStore
import cn.edu.pku.openrunner.feature.tasks.ui.TaskListFragment
import cn.edu.pku.openrunner.feature.weather.ui.WeatherFragment
import cn.edu.pku.openrunner.core.AmapPrivacyController
import cn.edu.pku.openrunner.core.AmapPrivacyStore
import cn.edu.pku.openrunner.core.AppThemeMode
import cn.edu.pku.openrunner.core.AppThemeStore
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity(R.layout.activity_main) {
    private lateinit var drawer: DrawerLayout
    private val drawerSummary: DrawerSummaryViewModel by viewModels {
        DrawerSummaryViewModel.Factory(SessionStore(applicationContext))
    }
    private val themeStore by lazy { AppThemeStore(this) }
    private val amapPrivacyStore by lazy { AmapPrivacyStore(this) }
    private var privacyDialogVisible = false
    private var pendingVirtualPage = false
    private val authLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && hasSession()) {
            findViewById<NavigationView>(R.id.main_navigation).setCheckedItem(R.id.nav_login)
            showFragment(AccountFragment())
        }
        refreshNavigation()
        drawerSummary.refresh(force = true)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(
            R.id.main_toolbar
        )
        setSupportActionBar(toolbar)

        drawer = findViewById(R.id.main_drawer)
        val toggle = ActionBarDrawerToggle(
            this,
            drawer,
            toolbar,
            R.string.drawer_open,
            R.string.drawer_close
        )
        drawer.addDrawerListener(toggle)
        drawer.addDrawerListener(object : DrawerLayout.SimpleDrawerListener() {
            override fun onDrawerOpened(drawerView: View) {
                drawerSummary.refresh(force = true)
            }
        })
        toggle.syncState()

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { drawerSummary.uiState.collect(::renderDrawerSummary) }
                launch {
                    LocalRunRecordStore(applicationContext).observeChanges().collect {
                        drawerSummary.refresh(force = true)
                    }
                }
            }
        }

        findViewById<NavigationView>(R.id.main_navigation)
            .setNavigationItemSelectedListener { item ->
                when (item.itemId) {
                    R.id.nav_run -> openRunPage()
                    R.id.nav_virtual_location -> openVirtualLocationPage()
                    R.id.nav_records -> showFragment(RecordListFragment())
                    R.id.nav_tasks -> showFragment(TaskListFragment())
                    R.id.nav_weather -> showFragment(WeatherFragment())
                    R.id.nav_login -> if (hasSession()) {
                        showFragment(AccountFragment())
                    } else {
                        authLauncher.launch(Intent(this, AuthActivity::class.java))
                    }
                    R.id.theme_system -> changeTheme(AppThemeMode.SYSTEM)
                    R.id.theme_light -> changeTheme(AppThemeMode.LIGHT)
                    R.id.theme_dark -> changeTheme(AppThemeMode.DARK)
                    else -> return@setNavigationItemSelectedListener false
                }
                drawer.closeDrawers()
                true
            }

        if (savedInstanceState == null) {
            if (amapPrivacyStore.isAgreed) {
                openRunPage()
            } else {
                findViewById<NavigationView>(R.id.main_navigation)
                    .setCheckedItem(R.id.nav_weather)
                showFragment(WeatherFragment())
                if (!amapPrivacyStore.hasDecision) showAmapPrivacyDialog()
            }
        }
        refreshNavigation()
    }

    override fun onResume() {
        super.onResume()
        refreshNavigation()
        drawerSummary.refresh()
    }

    fun refreshNavigation() {
        drawerSummary.syncSession()
        val menu = findViewById<NavigationView>(R.id.main_navigation).menu
        val item = menu.findItem(R.id.nav_login)
        item.title = getString(if (hasSession()) R.string.account_title else R.string.nav_login)
        menu.findItem(R.id.theme_system).isChecked = themeStore.mode == AppThemeMode.SYSTEM
        menu.findItem(R.id.theme_light).isChecked = themeStore.mode == AppThemeMode.LIGHT
        menu.findItem(R.id.theme_dark).isChecked = themeStore.mode == AppThemeMode.DARK
    }

    private fun renderDrawerSummary(state: DrawerSummaryState) {
        val header = findViewById<NavigationView>(R.id.main_navigation).getHeaderView(0)
        header.findViewById<TextView>(R.id.drawer_welcome).text = getString(
            R.string.drawer_welcome, state.userName ?: getString(R.string.drawer_guest)
        )
        header.findViewById<TextView>(R.id.drawer_user_id).apply {
            visibility = if (state.userId != null) View.VISIBLE else View.GONE
            text = state.userId?.let { getString(R.string.drawer_user_id, it) }.orEmpty()
        }
        val status = state.status
        header.findViewById<TextView>(R.id.drawer_mileage).text = when {
            state.userId == null -> getString(R.string.drawer_login_hint)
            status != null -> getString(
                R.string.drawer_mileage, status.current / 1000.0, status.target / 1000.0
            )
            state.loading -> getString(R.string.drawer_mileage_loading)
            else -> getString(R.string.drawer_mileage_error)
        }
        header.findViewById<TextView>(R.id.drawer_mileage_extra).apply {
            visibility = if (status != null) View.VISIBLE else View.GONE
            text = status?.let {
                getString(R.string.drawer_mileage_extra, it.bonus / 1000.0, it.validCount)
            }.orEmpty()
        }
        header.findViewById<TextView>(R.id.drawer_sync_status).apply {
            visibility = if (status != null) View.VISIBLE else View.GONE
            text = when {
                state.loading -> getString(R.string.drawer_mileage_loading)
                state.syncFailed -> getString(R.string.drawer_mileage_stale)
                else -> getString(R.string.drawer_mileage_current,
                    getString(if (status?.isPassed == true) {
                        R.string.account_passed
                    } else R.string.account_in_progress))
            }
        }
    }

    private fun hasSession(): Boolean {
        val session = SessionStore(this)
        return !session.userId.isNullOrBlank() && !session.token.isNullOrBlank()
    }

    private fun showFragment(fragment: androidx.fragment.app.Fragment) {
        supportFragmentManager.commit {
            replace(R.id.main_content, fragment)
        }
    }

    private fun changeTheme(mode: AppThemeMode) {
        drawer.closeDrawers()
        themeStore.setMode(mode)
    }

    private fun openRunPage() {
        pendingVirtualPage = false
        if (!amapPrivacyStore.isAgreed) {
            showAmapPrivacyDialog()
            return
        }
        findViewById<NavigationView>(R.id.main_navigation).setCheckedItem(R.id.nav_run)
        showFragment(RunFragment())
    }

    private fun openVirtualLocationPage() {
        pendingVirtualPage = true
        if (!amapPrivacyStore.isAgreed) {
            showAmapPrivacyDialog()
            return
        }
        findViewById<NavigationView>(R.id.main_navigation)
            .setCheckedItem(R.id.nav_virtual_location)
        showFragment(VirtualLocationFragment())
    }

    private fun showAmapPrivacyDialog() {
        if (privacyDialogVisible || isFinishing) return
        privacyDialogVisible = true
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.amap_privacy_title)
            .setMessage(R.string.amap_privacy_message)
            .setCancelable(false)
            .setPositiveButton(R.string.amap_privacy_accept) { _, _ ->
                privacyDialogVisible = false
                AmapPrivacyController.recordDecision(this, agreed = true)
                if (pendingVirtualPage) openVirtualLocationPage() else openRunPage()
            }
            .setNegativeButton(R.string.amap_privacy_decline) { _, _ ->
                privacyDialogVisible = false
                AmapPrivacyController.recordDecision(this, agreed = false)
                Toast.makeText(this, R.string.amap_privacy_required, Toast.LENGTH_LONG).show()
            }
            .setOnDismissListener { privacyDialogVisible = false }
            .show()
    }
}
