package cn.edu.pku.openrunner

import android.content.Intent
import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.commit
import com.google.android.material.navigation.NavigationView
import cn.edu.pku.openrunner.feature.auth.ui.AuthActivity
import cn.edu.pku.openrunner.core.session.SessionStore
import cn.edu.pku.openrunner.feature.account.ui.AccountFragment
import cn.edu.pku.openrunner.feature.records.ui.RecordListFragment
import cn.edu.pku.openrunner.feature.run.ui.RunFragment
import cn.edu.pku.openrunner.feature.tasks.ui.TaskListFragment
import cn.edu.pku.openrunner.feature.weather.ui.WeatherFragment
import cn.edu.pku.openrunner.core.AmapPrivacyController
import cn.edu.pku.openrunner.core.AmapPrivacyStore
import cn.edu.pku.openrunner.core.AppThemeMode
import cn.edu.pku.openrunner.core.AppThemeStore
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class MainActivity : AppCompatActivity(R.layout.activity_main) {
    private lateinit var drawer: DrawerLayout
    private val themeStore by lazy { AppThemeStore(this) }
    private val amapPrivacyStore by lazy { AmapPrivacyStore(this) }
    private var privacyDialogVisible = false
    private val authLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && hasSession()) {
            findViewById<NavigationView>(R.id.main_navigation).setCheckedItem(R.id.nav_login)
            showFragment(AccountFragment())
        }
        refreshNavigation()
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
        toggle.syncState()

        findViewById<NavigationView>(R.id.main_navigation)
            .setNavigationItemSelectedListener { item ->
                when (item.itemId) {
                    R.id.nav_run -> openRunPage()
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
    }

    fun refreshNavigation() {
        val menu = findViewById<NavigationView>(R.id.main_navigation).menu
        val item = menu.findItem(R.id.nav_login)
        item.title = getString(if (hasSession()) R.string.account_title else R.string.nav_login)
        menu.findItem(R.id.theme_system).isChecked = themeStore.mode == AppThemeMode.SYSTEM
        menu.findItem(R.id.theme_light).isChecked = themeStore.mode == AppThemeMode.LIGHT
        menu.findItem(R.id.theme_dark).isChecked = themeStore.mode == AppThemeMode.DARK
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
        if (!amapPrivacyStore.isAgreed) {
            showAmapPrivacyDialog()
            return
        }
        findViewById<NavigationView>(R.id.main_navigation).setCheckedItem(R.id.nav_run)
        showFragment(RunFragment())
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
                openRunPage()
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
