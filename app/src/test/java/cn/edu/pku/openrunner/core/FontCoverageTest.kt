package cn.edu.pku.openrunner.core

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.xml.parsers.DocumentBuilderFactory

/**
 * 守住内置宋体的字形覆盖。
 *
 * 内置字体是按"应用当前会渲染的字符"子集化的（见 tools/build_serif_font.py），
 * 好处是只有 140 KB 左右；代价是新增中文文案后如果不重新生成字体，
 * 新字会静默回退到系统字体，标题的宋体观感就断了。
 *
 * 这个测试直接读内置字体的 cmap 表，与 strings.xml 及 Kotlin 字面量逐字比对，
 * 让这种退化在 :app:testDebugUnitTest 阶段就暴露，而不是等到界面上看出来。
 *
 * 失败时执行：
 *     python tools/build_serif_font.py
 */
class FontCoverageTest {

    private val fontFile = File("src/main/res/font/or_serif_regular.ttf")

    @Test
    fun bundledSerifCoversAllUiText() {
        assertTrue("找不到内置字体：${fontFile.absolutePath}", fontFile.isFile)

        val covered = readCmapCoverage(fontFile.readBytes())
        assertTrue("内置字体 cmap 为空，字体文件可能损坏", covered.isNotEmpty())

        val missing = collectUiCharacters()
            // cmap 以码点为键，所以要拿 Char 的码点去比，而不是 Char 本身。
            .filter { it.code > 0x20 && it.code !in covered }
            .distinct()
            .sorted()

        assertTrue(
            "以下 ${missing.size} 个字符在界面文案里出现，但内置宋体没有对应字形，" +
                "会回退到系统字体。请重跑 tools/build_serif_font.py：\n" +
                missing.joinToString("\n") {
                    "  $it  U+${it.code.toString(16).uppercase().padStart(4, '0')}"
                },
            missing.isEmpty()
        )
    }

    /** strings.xml 与 Kotlin 字符串字面量里所有会渲染的字符。 */
    private fun collectUiCharacters(): Set<Char> {
        val chars = HashSet<Char>()
        chars.addAll(readStringResources())
        chars.addAll(readKotlinLiterals())
        return chars
    }

    private fun readStringResources(): Set<Char> {
        val chars = HashSet<Char>()
        val resDir = File("src/main/res")
        val valuesDirs = resDir.listFiles { f -> f.isDirectory && f.name.startsWith("values") }
            ?: return chars
        val factory = DocumentBuilderFactory.newInstance()
        for (dir in valuesDirs) {
            val strings = File(dir, "strings.xml")
            if (!strings.isFile) continue
            val doc = factory.newDocumentBuilder().parse(strings)
            val nodes = doc.getElementsByTagName("string")
            for (i in 0 until nodes.length) {
                val raw = nodes.item(i).textContent ?: continue
                chars.addAll(stripFormatSpecifiers(raw))
            }
        }
        return chars
    }

    /**
     * 只取字符串字面量里的中文，避开注释 —— 注释不渲染，不该要求字形。
     * 用简单的引号配对扫描，足以覆盖本项目的代码风格。
     */
    private fun readKotlinLiterals(): Set<Char> {
        val chars = HashSet<Char>()
        val srcDir = File("src/main/java")
        if (!srcDir.isDirectory) return chars
        srcDir.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .forEach { file ->
                file.readText(Charsets.UTF_8).lineSequence().forEach { line ->
                    // 去掉行注释后再取字面量，避免把注释里的中文算进来
                    val code = line.substringBefore("//")
                    Regex("\"([^\"\\\\]*)\"").findAll(code).forEach { m ->
                        m.groupValues[1].forEach { c ->
                            if (c.code > 0x20) chars.add(c)
                        }
                    }
                }
            }
        return chars
    }

    private fun stripFormatSpecifiers(raw: String): List<Char> =
        raw.replace(Regex("""%\d+\$[sd]"""), "")
            .replace(Regex("""%[sd]"""), "")
            .toList()

    /**
     * 解析 TrueType cmap 表，返回有实际字形的码点集合。
     * 只实现 format 4（BMP），中文子集字体都是这个格式。
     */
    private fun readCmapCoverage(data: ByteArray): Set<Int> {
        val buf = ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN)
        val numTables = buf.getShort(4).toInt() and 0xFFFF

        var cmapOffset = -1
        for (i in 0 until numTables) {
            val rec = 12 + i * 16
            if (String(data, rec, 4, Charsets.US_ASCII) == "cmap") {
                cmapOffset = buf.getInt(rec + 8)
                break
            }
        }
        require(cmapOffset in 0 until data.size) { "字体缺少 cmap 表" }

        val numSubtables = buf.getShort(cmapOffset + 2).toInt() and 0xFFFF
        var best = -1
        for (i in 0 until numSubtables) {
            val rec = cmapOffset + 4 + i * 8
            val platform = buf.getShort(rec).toInt() and 0xFFFF
            val encoding = buf.getShort(rec + 2).toInt() and 0xFFFF
            val sub = cmapOffset + buf.getInt(rec + 4)
            if (platform == 3 && encoding == 1) {       // Windows BMP
                best = sub
                break
            }
            if (platform == 0 && best < 0) best = sub   // Unicode
        }
        require(best in 0 until data.size) { "字体缺少可用的 cmap 子表" }

        val format = buf.getShort(best).toInt() and 0xFFFF
        require(format == 4) { "仅支持 cmap format 4，实际为 $format" }

        val segCountX2 = buf.getShort(best + 6).toInt() and 0xFFFF
        val segCount = segCountX2 / 2
        val endBase = best + 14
        val startBase = endBase + segCountX2 + 2
        val deltaBase = startBase + segCountX2
        val rangeBase = deltaBase + segCountX2

        val covered = HashSet<Int>()
        for (s in 0 until segCount) {
            val end = buf.getShort(endBase + s * 2).toInt() and 0xFFFF
            val start = buf.getShort(startBase + s * 2).toInt() and 0xFFFF
            if (start > end) continue
            val delta = buf.getShort(deltaBase + s * 2).toInt()
            val rangeOffset = buf.getShort(rangeBase + s * 2).toInt() and 0xFFFF
            for (code in start..end) {
                if (code == 0xFFFF) continue
                val glyph = if (rangeOffset == 0) {
                    (code + delta) and 0xFFFF
                } else {
                    val gi = rangeBase + s * 2 + rangeOffset + (code - start) * 2
                    if (gi + 1 >= data.size) continue
                    val g = buf.getShort(gi).toInt() and 0xFFFF
                    if (g == 0) 0 else (g + delta) and 0xFFFF
                }
                if (glyph != 0) covered.add(code)
            }
        }
        return covered
    }
}
