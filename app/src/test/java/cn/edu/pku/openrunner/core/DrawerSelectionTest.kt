package cn.edu.pku.openrunner.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * 守住抽屉「选中槽」的归属。
 *
 * NavigationView 全局只有一个选中槽：`NavigationMenuPresenter` 的 adapter 内部记着
 * 一个 `checkedItem`，换一项就把上一项 `setChecked(false)`（反编译确认）。谁能进这个
 * 槽，取决于菜单项是否 checkable —— 而这一点的后果全是静默的：
 *
 *   - **不可选中** → `NavigationView.setCheckedItem(id)` 直接 return，什么都不做
 *     （同一处反编译：非 checkable 早退）。表现是「进了账户页，抽屉的标记还留在
 *     上一个页面」—— 不报错，只是标记指向了错的页面。
 *   - **可选中的东西一多** → 谁后点谁占槽。主题子菜单的选项一旦可选中，点一下主题
 *     就把页面的标记顶掉；而切主题会重建 Activity，恢复的正是被顶掉之后的状态，
 *     页面从此没有标记。
 *
 * 两条都属于「看得见才知道坏了」，所以在这里钉死：**只有页面（和账户）可以选中**，
 * 主题子菜单的选项必须不可选中 —— 当前主题改由「外观」这一项的标题携带。
 *
 * 与 FontCoverageTest / NavGeometryTest 同一路数：纯 JUnit 直接读源码树。
 */
class DrawerSelectionTest {

    private val menuFile = File("src/main/res/menu/main_drawer_menu.xml")

    /** 菜单项 id（去掉 `@+id/` 前缀）→ 元素。 */
    private val items: Map<String, Element> by lazy {
        assertTrue("找不到 ${menuFile.absolutePath}", menuFile.isFile)
        val nodes = parse().getElementsByTagName("item")
        val map = HashMap<String, Element>()
        for (i in 0 until nodes.length) {
            val element = nodes.item(i) as Element
            val id = element.getAttribute("android:id")
            if (id.isNotEmpty()) {
                map[id.removePrefix("@+id/").removePrefix("@id/")] = element
            }
        }
        map
    }

    private fun parse() =
        DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(menuFile)

    private fun Element.checkable(): Boolean = getAttribute("android:checkable") == "true"

    private fun item(id: String): Element =
        items[id] ?: throw AssertionError("菜单里找不到 id 为 $id 的 item")

    @Test
    fun accountEntryIsCheckable() {
        assertTrue(
            "nav_login 必须写 android:checkable=\"true\"。它不是 checkable 时，" +
                "NavigationView.setCheckedItem(R.id.nav_login) 是静默的空操作" +
                "（NavigationMenuAdapter.setCheckedItem 对非 checkable 直接 return），" +
                "于是进了账户页，抽屉的选中标记仍留在上一个页面。",
            item("nav_login").checkable()
        )
    }

    @Test
    fun pageGroupStaysSingleChoice() {
        val groups = parse().getElementsByTagName("group")
        var found = false
        for (i in 0 until groups.length) {
            val group = groups.item(i) as Element
            if (group.getAttribute("android:checkableBehavior") == "single") found = true
        }
        assertTrue(
            "页面那一组必须保持 android:checkableBehavior=\"single\"：组里五个页面正是" +
                "靠它才拿到 checkable，改掉整组都进不了选中槽。",
            found
        )
    }

    @Test
    fun themeEntriesAreNotCheckable() {
        val groups = parse().getElementsByTagName("group")
        var themeBehavior: String? = null
        for (i in 0 until groups.length) {
            val group = groups.item(i) as Element
            if (group.getAttribute("android:id").endsWith("theme_group")) {
                themeBehavior = group.getAttribute("android:checkableBehavior")
            }
        }
        assertEquals(
            "主题那一组必须是 android:checkableBehavior=\"none\"。抽屉只有一个选中槽" +
                "（adapter 内部记一个 checkedItem，换一项就把上一项取消），主题项一旦" +
                "可选中，点一下主题就会把页面的标记顶掉；而切主题会重建 Activity，" +
                "恢复的正是被顶掉之后的状态，页面从此没有标记。当前主题由「外观」的" +
                "标题携带（MainActivity.refreshNavigation）。",
            "none",
            themeBehavior
        )

        for (id in listOf("theme_system", "theme_light", "theme_dark")) {
            assertTrue(
                "$id 不能写 android:checkable=\"true\"：组的 checkableBehavior=\"none\"" +
                    " 已经说了它不可选中，再写一遍等于把它重新塞回选中槽。",
                !item(id).checkable()
            )
        }
    }

    @Test
    fun submenuParentIsNotCheckable() {
        assertTrue(
            "nav_appearance 不能可选中：它是子菜单的父项，点它只是展开子菜单。" +
                "一旦可选中，展开这个动作就会把页面的标记顶掉。",
            !item("nav_appearance").checkable()
        )
    }
}
