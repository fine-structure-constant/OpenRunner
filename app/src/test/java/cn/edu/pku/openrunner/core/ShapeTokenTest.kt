package cn.edu.pku.openrunner.core

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * 守住圆角 token 的「取用路径」。
 *
 * 为什么值得一个测试：这个工程里圆角有三条取用路径，按元素类型选 ——
 *
 *   1. Material 组件（卡片 / 按钮）  → shapeAppearanceOverlay 挂 ShapeAppearance.OpenRunner.*
 *   2. 普通 `<shape>` drawable       → `<corners android:radius="@dimen/or_radius_*" />`
 *   3. TextInputLayout              → boxCornerRadiusTopStart 等四个 box 属性
 *
 * **选错是完全静默的**：`shapeAppearanceOverlay` 只有 MaterialShapeDrawable 会去解
 * 属性，给一个普通 `<shape>`（GradientDrawable）挂 overlay 不报错、也不生效。
 *
 * 这条规则原先只写在设计规范里，于是真的长出了一个取不到值的 token：
 * `ShapeAppearance.OpenRunner.Seal` —— 印章是 TextView + 普通 shape drawable，
 * 那个 wrapper 没有任何东西能读到它。它比「多余」更糟：它让人以为印章的圆角
 * 该走 overlay，照着做就会得到一个不生效的改动。
 *
 * 所以这里把三件事变成构建期断言：
 *   1. 每个 ShapeAppearance 形状 token 都真的被某处 shapeAppearanceOverlay 引用；
 *   2. 每个 or_radius_* 数值 token 都真的有人读；
 *   3. 印章的圆角走的是它唯一能走的那条路（drawable 里的 `<corners>`）。
 *
 * 与 NavGeometryTest / FontCoverageTest 同一路数：纯 JUnit，直接读模块源码树。
 */
class ShapeTokenTest {

    @Test
    fun everyShapeAppearanceTokenHasAConsumer() {
        val styles = File(STYLES_FILE)
        assertTrue("找不到 ${styles.path}", styles.isFile)

        val declared = Regex("""<style name="(ShapeAppearance\.OpenRunner\.[^"]+)"""")
            .findAll(styles.readText())
            .map { it.groupValues[1] }
            .toList()

        assertTrue(
            "styles.xml 里一个 ShapeAppearance.OpenRunner.* 都没有，解析大概出了问题",
            declared.isNotEmpty()
        )

        val sources = readSources()
        val orphans = declared.filterNot { name -> sources.hasReference("@style/$name") }

        assertTrue(
            "以下形状 token 没有被任何 shapeAppearanceOverlay 引用：${orphans.joinToString()}。" +
                "注意 shapeAppearanceOverlay 只能被 Material 组件读到 —— 如果某个元素的圆角" +
                "拿不到值，多半是它根本不是 Material 组件（普通 <shape> drawable 要在 " +
                "<corners> 里直接引用 @dimen/or_radius_*），这时该删掉这个 wrapper，" +
                "而不是把 overlay 挂上去期待它生效。",
            orphans.isEmpty()
        )
    }

    @Test
    fun everyRadiusTokenHasAConsumer() {
        val declared = readDimens().keys.filter { it.startsWith("or_radius_") }.sorted()

        assertTrue(
            "dimens.xml 里一个 or_radius_* 都没有，解析大概出了问题",
            declared.isNotEmpty()
        )

        val sources = readSources()
        val orphans = declared.filterNot { name -> sources.hasReference("@dimen/$name") }

        assertTrue(
            "以下圆角数值 token 在 src/main 里没有任何引用：${orphans.joinToString()}。" +
                "圆角的数值只有这一个来源，没人读的档位等于不存在的档位 —— " +
                "要么接上引用，要么删掉。",
            orphans.isEmpty()
        )
    }

    @Test
    fun sealTakesItsRadiusFromTheDrawableCorner() {
        val file = File("src/main/res/drawable/or_seal.xml")
        assertTrue("找不到 ${file.path}", file.isFile)

        assertTrue(
            "or_seal.xml 必须用 <corners android:radius=\"@dimen/or_radius_seal\" /> 取圆角。" +
                "印章是 TextView + 普通 <shape> drawable，shapeAppearanceOverlay 对它不生效，" +
                "所以这条路是唯一一条能生效的；改成写死数值会让 or_radius_seal 变成摆设。",
            file.readText().contains("""<corners android:radius="@dimen/or_radius_seal" />""")
        )
    }

    /** 令牌名 → 值文本。 */
    private fun readDimens(): Map<String, String> {
        val file = File(DIMENS_FILE)
        assertTrue("找不到 ${file.path}", file.isFile)

        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
        val nodes = doc.getElementsByTagName("dimen")
        val values = HashMap<String, String>()
        for (i in 0 until nodes.length) {
            val element = nodes.item(i)
            val name = element.attributes.getNamedItem("name")?.nodeValue ?: continue
            values[name] = element.textContent.trim()
        }
        return values
    }

    /** 读 src/main 下全部源码文本，去注释。 */
    private fun readSources(): List<String> =
        File("src/main").walkTopDown()
            .filter { it.isFile && it.extension in SOURCE_EXTENSIONS }
            .map { stripComments(it.readText()) }
            .toList()

    /**
     * 是否真的引用了某个资源。
     *
     * 匹配到标识符边界为止：`@dimen/or_radius_card` 是
     * `@dimen/or_radius_card_soft` 的前缀，用 `contains` 会让前者假装有引用 ——
     * 而「一个 token 靠另一个 token 的名字蹭到引用」正是这里要防的那类假通过。
     * 定义处（`<dimen name="or_radius_card">`）不含 `@dimen/`，不会自己匹配自己。
     */
    private fun List<String>.hasReference(reference: String): Boolean {
        val pattern = Regex(Regex.escape(reference) + """(?![\w.])""")
        return any { pattern.containsMatchIn(it) }
    }

    /**
     * 去掉注释。
     *
     * 「有引用」必须是属性层面的引用 —— 光在注释里提一句不算，
     * 那正是 or_nav_indicator_gap 当初蒙混过关的方式。
     */
    private fun stripComments(text: String): String = text
        .replace(Regex("""<!--.*?-->""", RegexOption.DOT_MATCHES_ALL), " ")
        .replace(Regex("""/\*.*?\*/""", RegexOption.DOT_MATCHES_ALL), " ")
        .replace(Regex("""//[^\n]*"""), " ")

    private companion object {
        const val STYLES_FILE = "src/main/res/values/styles.xml"
        const val DIMENS_FILE = "src/main/res/values/dimens.xml"
        val SOURCE_EXTENSIONS = setOf("xml", "kt", "java")
    }
}
