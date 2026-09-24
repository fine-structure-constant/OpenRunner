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
import androidx.recyclerview.widget.RecyclerView
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
import cn.edu.pku.openrunner.ui.NavItemIndicatorDecoration
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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

    /**
     * 是否已经显示过第一个页面。
     *
     * fade through 的前提是「有一个旧页面要让路」，冷启动时并没有 —— 第一屏
     * 直接淡入会显得像加载慢了一拍。所以只从第二次切换开始做转场。进程被回收后
     * 重建时 FragmentManager 自己会把页面还原回来，那条路径也不走 showFragment，
     * 所以恢复后要把它标成「已经有页面了」，否则重建之后第一次点菜单会没有转场。
     *
     * 地图页另有豁免，见 [showFragment] 的 transition 参数。
     */
    private var firstPageShown = false
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
        // 重建（主题切换、进程被回收后恢复）时页面上已经有一个被还原的 Fragment，
        // 此时再切页面应当照常做转场。
        firstPageShown = savedInstanceState != null
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
                // Not forced: the summary only moves when an upload lands, and the upload path
                // refreshes it itself. Opening the drawer repeatedly should not query the
                // server each time — the cache in DrawerSummaryViewModel covers this.
                drawerSummary.refresh()
            }
        })
        toggle.syncState()

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { drawerSummary.uiState.collect(::renderDrawerSummary) }
                launch {
                    var settle: Job? = null
                    LocalRunRecordStore(applicationContext).observeChanges().collect {
                        // Saving or uploading a record writes several fields, so the store
                        // notifies once per write. Wait for the burst to settle before asking
                        // the server, otherwise one save fires a handful of identical requests.
                        settle?.cancel()
                        settle = launch {
                            delay(RECORD_SETTLE_MILLIS)
                            drawerSummary.refresh(force = true)
                        }
                    }
                }
            }
        }

        val navigation = findViewById<NavigationView>(R.id.main_navigation)
        attachNavIndicator(navigation)
        navigation
            .setNavigationItemSelectedListener { item ->
                val handled = when (item.itemId) {
                    R.id.nav_run -> openRunPage()
                    R.id.nav_virtual_location -> openVirtualLocationPage()
                    R.id.nav_records -> {
                        showFragment(RecordListFragment())
                        true
                    }
                    R.id.nav_tasks -> {
                        showFragment(TaskListFragment())
                        true
                    }
                    R.id.nav_weather -> {
                        showFragment(WeatherFragment())
                        true
                    }
                    R.id.nav_login -> if (hasSession()) {
                        showFragment(AccountFragment())
                        true
                    } else {
                        // 返回 false：登录页还没打开，抽屉不该把选中标记落在「登录」上。
                        // NavigationView 只在监听器返回 true 时才记选中项
                        // （反编译它的点击监听器确认：if (item.isCheckable() && result)），
                        // 所以这里的 false 正好让它什么都不动 —— 标记留在当前页面上。
                        // 登录成功后由 authLauncher 显式选中「账户」。
                        authLauncher.launch(Intent(this, AuthActivity::class.java))
                        false
                    }
                    R.id.theme_system -> {
                        changeTheme(AppThemeMode.SYSTEM)
                        true
                    }
                    R.id.theme_light -> {
                        changeTheme(AppThemeMode.LIGHT)
                        true
                    }
                    R.id.theme_dark -> {
                        changeTheme(AppThemeMode.DARK)
                        true
                    }
                    else -> return@setNavigationItemSelectedListener false
                }
                drawer.closeDrawers()
                handled
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
        // 当前主题写在「外观」的标题里，而不是让子菜单的选项去占抽屉的选中槽。
        // 抽屉只有一个选中槽（NavigationMenuPresenter 的 adapter 内部记一个
        // checkedItem，换一项就把上一项取消），主题项一旦可选中，点一下就会把页面的
        // 标记顶掉；而切主题会重建 Activity，恢复的是被顶掉之后的状态。所以主题项在
        // menu/main_drawer_menu.xml 里被设成不可选中，状态改由标题携带 —— 顺带
        // 不必展开子菜单就能看见。
        menu.findItem(R.id.nav_appearance).title = getString(
            R.string.nav_appearance_mode,
            getString(
                when (themeStore.mode) {
                    AppThemeMode.SYSTEM -> R.string.theme_system
                    AppThemeMode.LIGHT -> R.string.theme_light
                    AppThemeMode.DARK -> R.string.theme_dark
                }
            )
        )
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
            // While there are already numbers on screen, this line stays on those numbers.
            // It used to show the loading text during every refresh, so a background sync
            // flipped "官方统计 · 进行中" to "正在同步跑步里程…" and back — a flash carrying no
            // information. The loading text is only worth showing when there is nothing else
            // to look at.
            text = when {
                status != null -> if (state.syncFailed) {
                    getString(R.string.drawer_mileage_stale)
                } else {
                    getString(
                        R.string.drawer_mileage_current,
                        getString(
                            if (status.isPassed) R.string.account_passed
                            else R.string.account_in_progress
                        )
                    )
                }
                state.loading -> getString(R.string.drawer_mileage_loading)
                else -> getString(R.string.drawer_mileage_error)
            }
        }
    }

    private fun hasSession(): Boolean {
        val session = SessionStore(this)
        return !session.userId.isNullOrBlank() && !session.token.isNullOrBlank()
    }

    /**
     * 把左侧菜单选中项那根竖装饰线挂上去。
     *
     * 线画在菜单列表的 onDraw 里（[NavItemIndicatorDecoration]），所以要拿到那个
     * 列表 —— 它就是 NavigationView 内部的 NavigationMenuView，一个 RecyclerView。
     * 按类型找而不是认子 View 的下标：NavigationView 还会把 header 也塞成子 View，
     * 下标不稳。找不到就什么都不做，菜单照常能用，只是没有那根线。
     */
    private fun attachNavIndicator(navigation: NavigationView) {
        val menu = (0 until navigation.childCount)
            .map(navigation::getChildAt)
            .filterIsInstance<RecyclerView>()
            .firstOrNull() ?: return
        menu.addItemDecoration(NavItemIndicatorDecoration(this))
    }

    /**
     * 切到新页面。
     *
     * @param transition 是否做 fade through 转场。跑步页与虚拟定位页传 `false`：
     *   这两页的首帧是一张还没加载完的地图，转场里的缩放与淡入会把「地图还没画出来」
     *   的那一瞬放大给用户看 —— 表现为进页面时闪一下黑边。地图页直接硬切，
     *   地图就绪与否交给它自己的加载态交代。
     */
    private fun showFragment(
        fragment: androidx.fragment.app.Fragment,
        transition: Boolean = true
    ) {
        // 只从第二次切换开始做转场，理由见 firstPageShown 的注释。
        val animate = firstPageShown && transition
        firstPageShown = true
        supportFragmentManager.commit {
            if (animate) {
                // fade through：进入动画自带 90ms 的 startOffset，正好等退出动画走完，
                // 两段不重叠。参数见 res/anim/or_page_enter.xml 与 or_page_exit.xml。
                setCustomAnimations(R.anim.or_page_enter, R.anim.or_page_exit)
            }
            setReorderingAllowed(true)
            replace(R.id.main_content, fragment)
        }
    }

    private fun changeTheme(mode: AppThemeMode) {
        drawer.closeDrawers()
        themeStore.setMode(mode)
    }

    /**
     * 打开跑步页。
     *
     * @return 是否真的切过去了。隐私弹窗拦下来时返回 false —— 页面没换，抽屉的选中
     *   标记也就不该跟着动（菜单监听器把这个值原样返回给 NavigationView）。
     */
    private fun openRunPage(): Boolean {
        pendingVirtualPage = false
        if (!amapPrivacyStore.isAgreed) {
            showAmapPrivacyDialog()
            return false
        }
        findViewById<NavigationView>(R.id.main_navigation).setCheckedItem(R.id.nav_run)
        showFragment(RunFragment(), transition = false)
        return true
    }

    /** 打开虚拟定位页。返回值含义同 [openRunPage]。 */
    private fun openVirtualLocationPage(): Boolean {
        pendingVirtualPage = true
        if (!amapPrivacyStore.isAgreed) {
            showAmapPrivacyDialog()
            return false
        }
        findViewById<NavigationView>(R.id.main_navigation)
            .setCheckedItem(R.id.nav_virtual_location)
        showFragment(VirtualLocationFragment(), transition = false)
        return true
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

    private companion object {
        /** Quiet period after a local record write before the drawer summary is re-fetched. */
        private const val RECORD_SETTLE_MILLIS = 800L
    }
}
