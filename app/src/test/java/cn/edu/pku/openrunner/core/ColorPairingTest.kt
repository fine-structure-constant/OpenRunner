package cn.edu.pku.openrunner.core

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * 守住配色 token 的两条约定。
 *
 * **一、深浅两套一一对应。**
 * 这是整套配色里最容易静默出错的一条：`values/colors.xml` 加一个色、忘了在
 * `values-night/colors.xml` 里也加，**编译通过、浅色模式正常**，只有切到深色时
 * 那一处取不到值 —— 而 Android 的兜底是回退到 `values/`，于是在一片深色里冒出
 * 一块浅色。看的人只会觉得「这里没做完」，不会想到是漏了一个色名。
 *
 * 例外只有两组，都是**刻意的单边定义**：
 *   - `hud_*`（跑步中墨色仪表）：只有夜间生效，浅色模式下跑步页走普通宣纸主题；
 *   - `launcher_icon_background`：启动器图标不该随主题变色（`or_primary` 在夜间
 *     会被覆盖成浅粉，跟着它走图标会变粉）。
 *
 * **二、墨色板只能由 overlay 主题读。**
 * 这是上一条例外的另一半：`hud_*` 只存在于 `values/`，所以它对**两种模式都返回
 * 墨色**。谁要是从布局里写 `@color/hud_surface`，浅色模式下就会得到一块黑 ——
 * 不是「没做完」，是彻底读不出内容。落在 overlay 子树里的元素要的是主题属性
 * （`?attr/colorSurface`），由 `Theme.OpenRunner.Hud` 去重映射。
 *
 * 与 NavGeometryTest / ShapeTokenTest 同一路数：纯 JUnit，直接读模块源码树。
 */
class ColorPairingTest {

    @Test
    fun everyThemeColorExistsInBothBuckets() {
        val light = colorNames(LIGHT_FILE)
        val dark = colorNames(NIGHT_FILE)

        assertTrue("解析 $LIGHT_FILE 得到 0 个颜色，路径大概变了", light.isNotEmpty())
        assertTrue("解析 $NIGHT_FILE 得到 0 个颜色，路径大概变了", dark.isNotEmpty())

        val missingInNight = (light - dark).filterNot { it.isLightOnly() }.sorted()
        assertTrue(
            "以下颜色只定义在 $LIGHT_FILE 里，深色模式下会回退成浅色值：" +
                "${missingInNight.joinToString()}。请在 $NIGHT_FILE 里补上同名色。" +
                "（若它确实不该随主题变，那属于例外，要像 hud_* / launcher_icon_background " +
                "那样在 isLightOnly() 里写明理由。）",
            missingInNight.isEmpty()
        )

        val missingInLight = (dark - light).sorted()
        assertTrue(
            "以下颜色只定义在 $NIGHT_FILE 里，浅色模式下根本取不到：" +
                "${missingInLight.joinToString()}。定义处应该在 $LIGHT_FILE。",
            missingInLight.isEmpty()
        )
    }

    @Test
    fun theHudPaletteIsOnlyReadByTheOverlayTheme() {
        val sources = File("src/main").walkTopDown()
            .filter { it.isFile && it.extension in SOURCE_EXTENSIONS }
            .map { it to stripComments(it.readText()) }
            .filter { (_, text) -> Regex("""(@color/|R\.color\.)hud_""").containsMatchIn(text) }
            .map { (file, _) -> file.path.replace('\\', '/') }
            .sorted()
            .toList()

        val unexpected = sources.filterNot { it.endsWith(HUD_THEME_FILE) }
        assertTrue(
            "以下文件在 $HUD_THEME_FILE 之外引用了墨色板：${unexpected.joinToString()}。" +
                "hud_* 只定义在 $LIGHT_FILE（不在 values-night），所以它对浅色模式也返回墨色 —— " +
                "从布局里引用它会在浅色下画出一块黑。落在跑步页 / 虚拟定位页子树里的元素" +
                "要改用主题属性（?attr/colorSurface 等），由 Theme.OpenRunner.Hud 负责重映射。",
            unexpected.isEmpty()
        )

        assertTrue(
            "$HUD_THEME_FILE 里一个 hud_* 都没引用，墨色覆盖大概是丢了",
            sources.isNotEmpty()
        )
    }

    private fun colorNames(path: String) = readColors(path).keys

    private fun readColors(path: String): Map<String, String> {
        val file = File(path)
        assertTrue("找不到 ${file.path}", file.isFile)

        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
        val nodes = doc.getElementsByTagName("color")
        val values = LinkedHashMap<String, String>()
        for (i in 0 until nodes.length) {
            val element = nodes.item(i)
            val name = element.attributes.getNamedItem("name")?.nodeValue ?: continue
            values[name] = element.textContent.trim()
        }
        return values
    }

    /**
     * 刻意的单边定义。
     *
     * 按前缀而不是逐个列名：`hud_*` 是一整组「只有夜间生效」的墨色仪表，组里加一个
     * 成员仍然是同一条理由。`launcher_icon_background` 是唯一的单例。
     */
    private fun String.isLightOnly(): Boolean =
        startsWith("hud_") || this == "launcher_icon_background"

    private fun stripComments(text: String): String = text
        .replace(Regex("""<!--.*?-->""", RegexOption.DOT_MATCHES_ALL), " ")
        .replace(Regex("""/\*.*?\*/""", RegexOption.DOT_MATCHES_ALL), " ")
        .replace(Regex("""//[^\n]*"""), " ")

    private companion object {
        const val LIGHT_FILE = "src/main/res/values/colors.xml"
        const val NIGHT_FILE = "src/main/res/values-night/colors.xml"
        const val HUD_THEME_FILE = "src/main/res/values-night/themes.xml"
        val SOURCE_EXTENSIONS = setOf("xml", "kt", "java")
    }
}
