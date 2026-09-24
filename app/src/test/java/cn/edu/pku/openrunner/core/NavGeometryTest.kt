package cn.edu.pku.openrunner.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * 守住抽屉的横向几何。
 *
 * 为什么值得一个测试：这条几何横跨四个文件、两种语言，而它们之间唯一的约束是
 * 一个**算术关系** ——
 *
 *     or_nav_item_fill_inset == or_nav_item_inset
 *                             + or_nav_indicator_width
 *                             + or_nav_indicator_gap
 *
 * drawable 的 `android:insetLeft` 做不了加法，所以右边那个值是手算写死的。
 * 改一个忘了另一个既不会编译失败、也不会抛异常，只会画歪 —— 属于「得跑起来
 * 盯着看才发现」的那类坏结果。
 *
 * 同理，声明了却没人读的值更隐蔽：or_nav_indicator_gap 曾经就是纯粹的摆设，
 * dimens 的注释里写着「改它就能加大间隙」，改了却什么都不发生。
 *
 * 所以这里把三件事变成构建期断言：
 *   1. 等式成立；
 *   2. 内容列确实落在胶囊里面（不然图标与文字会压住胶囊边缘）；
 *   3. 每个值都真的有人读（注释里提到不算 —— 先把注释去掉再找）。
 *
 * 与 FontCoverageTest 同一路数：纯 JUnit，直接读模块源码树，不需要 Robolectric。
 */
class NavGeometryTest {

    private val dimensFile = File("src/main/res/values/dimens.xml")

    @Test
    fun capsuleInsetEqualsGutterArithmetic() {
        val values = readDimens()
        val inset = dimen(values, "or_nav_item_inset")
        val width = dimen(values, "or_nav_indicator_width")
        val gap = dimen(values, "or_nav_indicator_gap")
        val fill = dimen(values, "or_nav_item_fill_inset")

        assertEquals(
            "or_nav_item_fill_inset 必须等于 inset + width + gap。这个值是手算写死的" +
                "（drawable 的 inset 做不了加法），所以改了 or_nav_item_inset / " +
                "or_nav_indicator_width / or_nav_indicator_gap 中的任何一个，都要一起改。" +
                "否则竖装饰线要么与胶囊重叠，要么与胶囊之间裂开一道不属于任何人的缝。",
            (inset + width + gap).toLong(),
            fill.toLong()
        )
    }

    @Test
    fun contentColumnSitsInsideCapsule() {
        val values = readDimens()
        val fill = dimen(values, "or_nav_item_fill_inset")
        val content = dimen(values, "or_drawer_pad")

        assertTrue(
            "内容列 or_drawer_pad ($content dp) 必须落在胶囊左缘 " +
                "or_nav_item_fill_inset ($fill dp) 右侧，否则菜单图标与抽屉头部的印章" +
                "会压住胶囊边缘，胶囊就读不出自己的边界了。",
            content > fill
        )
    }

    @Test
    fun checkedDrawableUsesTheTokens() {
        val file = File("src/main/res/drawable/or_nav_item_checked.xml")
        assertTrue("找不到 ${file.path}", file.isFile)
        val text = file.readText()

        assertTrue(
            "or_nav_item_checked.xml 的 insetLeft 必须引用 " +
                "@dimen/or_nav_item_fill_inset，不能写死数值 —— 写死之后改 dimens " +
                "不会影响胶囊，而注释还在说它会。",
            text.contains("""android:insetLeft="@dimen/or_nav_item_fill_inset"""")
        )
        assertTrue(
            "or_nav_item_checked.xml 的 insetRight 必须引用 @dimen/or_nav_item_inset" +
                "（胶囊右内缩与竖线的左端点取同一个值，两侧留白才是对称的）。",
            text.contains("""android:insetRight="@dimen/or_nav_item_inset"""")
        )
    }

    @Test
    fun navItemPaddingComesFromTheContentColumnToken() {
        val text = File("src/main/res/layout/activity_main.xml").readText()
        assertTrue(
            "NavigationView 的 itemHorizontalPadding 必须引用 @dimen/or_drawer_pad，" +
                "不能写死数值 —— 抽屉头部用的是同一个 token，写死会让两者悄悄错开，" +
                "而且不会有人收到提示。",
            text.contains("""app:itemHorizontalPadding="@dimen/or_drawer_pad"""")
        )
    }

    @Test
    fun everyGeometryTokenHasAConsumer() {
        val values = readDimens()
        val tokens = listOf(
            "or_nav_item_inset",
            "or_nav_indicator_width",
            "or_nav_indicator_gap",
            "or_nav_indicator_height",
            "or_nav_item_fill_inset",
            "or_drawer_pad"
        ).filterNot { it in SPEC_ONLY }

        // 一次读完再逐 token 匹配，避免 token 数 × 文件数的重复 IO。
        val sources = File("src/main").walkTopDown()
            .filter { it.isFile && it.extension in SOURCE_EXTENSIONS }
            .filterNot { it.name == "dimens.xml" }   // 定义处不算引用
            .map { stripComments(it.readText()) }
            .toList()

        val orphans = tokens.filter { token -> sources.none { it.contains(token) } }

        assertTrue(
            "以下抽屉几何 token 在 src/main 里没有任何引用，属于摆设：" +
                "${orphans.joinToString()}。声明了却没人读的值会让人以为改它有效果，" +
                "然后改了发现没反应 —— 要么接上引用，要么删掉。",
            orphans.isEmpty()
        )
    }

    /** 令牌名 → dp 值。这条几何里全是整数 dp。 */
    private fun readDimens(): Map<String, Int> {
        assertTrue("找不到 ${dimensFile.absolutePath}", dimensFile.isFile)
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(dimensFile)
        val nodes = doc.getElementsByTagName("dimen")
        val values = HashMap<String, Int>()
        for (i in 0 until nodes.length) {
            val element = nodes.item(i)
            val name = element.attributes.getNamedItem("name")?.nodeValue ?: continue
            val match = Regex("""^(\d+)dp$""").find(element.textContent.trim())
            if (match != null) values[name] = match.groupValues[1].toInt()
        }
        return values
    }

    private fun dimen(values: Map<String, Int>, name: String): Int =
        values[name] ?: throw AssertionError("dimens.xml 里没有 $name，或它的值不是整数 dp")

    /**
     * 去掉注释。
     *
     * 「有引用」必须是代码或属性层面的引用 —— 光在注释里提一句不算，
     * 那正是 or_nav_indicator_gap 当初蒙混过关的方式。
     */
    private fun stripComments(text: String): String = text
        .replace(Regex("""<!--.*?-->""", RegexOption.DOT_MATCHES_ALL), " ")
        .replace(Regex("""/\*.*?\*/""", RegexOption.DOT_MATCHES_ALL), " ")
        .replace(Regex("""//[^\n]*"""), " ")

    private companion object {
        val SOURCE_EXTENSIONS = setOf("xml", "kt", "java")

        /**
         * 只作为声明值存在的 token，渲染路径不读它。
         *
         * 间隙按定义就是一段空白，画不出来，所以没有任何视图会读它；
         * 它的消费者就是上面那条等式断言 —— 由测试拿它反推胶囊内缩。
         */
        val SPEC_ONLY = setOf("or_nav_indicator_gap")
    }
}
