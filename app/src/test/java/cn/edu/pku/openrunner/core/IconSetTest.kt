package cn.edu.pku.openrunner.core

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * 守住自绘图标的三条约定。
 *
 * 设计规范里写着「统一 24dp 画布、描边 1.6dp、圆头圆角，命名 ic_or_*，禁止系统图标」，
 * 但这条规则原先只是文字 —— 于是它静默地漂了一个：`ic_or_weather.xml` 的两道光芒
 * 写成 1.5dp，其余 11 枚都是 1.6。0.1dp 在 24dp 画布上肉眼看不出来，改的人也
 * 不会收到任何提示，只有把规则变成断言才拦得住。
 *
 * 这里断言三件事：
 *   1. 画布统一（android:width/height = 24dp，viewport = 24×24）；
 *   2. 描边宽度统一（每条带 strokeWidth 的路径都是 1.6dp）；
 *   3. 全工程不再引用系统图标（@android:drawable/...）。
 *
 * 不断言 strokeLineCap：规范说「圆头」，但用两段弧画出的整圆本来就不需要 cap
 * （视觉闭合，cap 不可见），ic_or_delete 的桶身与提手更是**故意平头**顶在盖子
 * 横线上（加圆头反而会在 T 形接口处长出小疙瘩）。断言它只会制造假失败。
 *
 * 与 NavGeometryTest / FontCoverageTest 同一路数：纯 JUnit，直接读模块源码树。
 */
class IconSetTest {

    @Test
    fun everyIconUsesTheTwentyFourGrid() {
        val icons = iconFiles()
        assertTrue("src/main/res/drawable 下找不到 ic_or_*.xml，路径大概变了", icons.isNotEmpty())

        val wrong = icons.mapNotNull { file ->
            val root = parse(file)
            val width = root.attributes.getNamedItem("android:width")?.nodeValue
            val height = root.attributes.getNamedItem("android:height")?.nodeValue
            val viewportWidth = root.attributes.getNamedItem("android:viewportWidth")?.nodeValue
            val viewportHeight = root.attributes.getNamedItem("android:viewportHeight")?.nodeValue
            if (width == "24dp" && height == "24dp" &&
                viewportWidth == "24" && viewportHeight == "24"
            ) {
                null
            } else {
                "${file.name}: width=$width height=$height " +
                    "viewport=${viewportWidth}x$viewportHeight"
            }
        }

        assertTrue(
            "以下图标不在 24dp 画布上：${wrong.joinToString()}。图标必须统一 24dp 画布" +
                "（android:width/height=\"24dp\" + viewportWidth/Height=\"24\"），" +
                "否则摆进 24dp 的 ImageView 里会因为缩放而显得粗细不一 —— " +
                "这正是当初换掉系统图标的起因。",
            wrong.isEmpty()
        )
    }

    @Test
    fun everyStrokeIsOnePointSix() {
        val offenders = iconFiles().flatMap { file ->
            val root = parse(file)
            val paths = root.getElementsByTagName("path")
            (0 until paths.length).mapNotNull { index ->
                val stroke = paths.item(index).attributes.getNamedItem("android:strokeWidth")
                    ?: return@mapNotNull null
                val value = stroke.nodeValue
                if (value == "1.6") null else "${file.name} path#$index = $value"
            }
        }

        assertTrue(
            "以下描边路径的宽度不是 1.6dp：${offenders.joinToString()}。" +
                "描边宽度是整套图标看起来「同一只手画的」的全部依据；" +
                "偏差 0.1dp 在画布上肉眼不可辨，只有这里能发现。",
            offenders.isEmpty()
        )
    }

    @Test
    fun noSystemIconsRemain() {
        // 定义处是 res/drawable/ 下的文件本身，不会被 @android:drawable/ 命中。
        val sources = File("src/main").walkTopDown()
            .filter { it.isFile && it.extension in SOURCE_EXTENSIONS }
            .map { it to stripComments(it.readText()) }
            .filter { (_, text) -> text.contains("@android:drawable/") }
            .map { (file, _) -> file.path.replace('\\', '/') }
            .toList()

        assertTrue(
            "以下文件引用了 @android:drawable/ 下的系统图标：${sources.joinToString()}。" +
                "系统图标风格陈旧、粗细不一（1dp 到 2dp 混着），正是这次图标自绘的起因；" +
                "一律换成 drawable/ic_or_*.xml。",
            sources.isEmpty()
        )
    }

    private fun iconFiles(): List<File> {
        val dir = File("src/main/res/drawable")
        assertTrue("找不到 ${dir.path}", dir.isDirectory)
        return (dir.listFiles() ?: emptyArray())
            .filter { it.isFile && it.name.startsWith("ic_or_") && it.extension == "xml" }
            .sortedBy { it.name }
    }

    private fun parse(file: File) =
        DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file).documentElement

    private fun stripComments(text: String): String = text
        .replace(Regex("""<!--.*?-->""", RegexOption.DOT_MATCHES_ALL), " ")
        .replace(Regex("""/\*.*?\*/""", RegexOption.DOT_MATCHES_ALL), " ")
        .replace(Regex("""//[^\n]*"""), " ")

    private companion object {
        val SOURCE_EXTENSIONS = setOf("xml", "kt", "java")
    }
}
