package cn.edu.pku.openrunner

import android.content.Intent
import android.app.Activity
import android.os.Bundle
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

class MainActivity : AppCompatActivity(R.layout.activity_main) {
    private lateinit var drawer: DrawerLayout
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
                    R.id.nav_run -> showFragment(RunFragment())
                    R.id.nav_records -> showFragment(RecordListFragment())
                    R.id.nav_tasks -> showFragment(TaskListFragment())
                    R.id.nav_weather -> showFragment(WeatherFragment())
                    R.id.nav_login -> if (hasSession()) {
                        showFragment(AccountFragment())
                    } else {
                        authLauncher.launch(Intent(this, AuthActivity::class.java))
                    }
                    else -> return@setNavigationItemSelectedListener false
                }
                drawer.closeDrawers()
                true
            }

        if (savedInstanceState == null) {
            findViewById<NavigationView>(R.id.main_navigation)
                .setCheckedItem(R.id.nav_run)
            showFragment(RunFragment())
        }
        refreshNavigation()
    }

    override fun onResume() {
        super.onResume()
        refreshNavigation()
    }

    fun refreshNavigation() {
        val item = findViewById<NavigationView>(R.id.main_navigation).menu.findItem(R.id.nav_login)
        item.title = getString(if (hasSession()) R.string.account_title else R.string.nav_login)
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
}
