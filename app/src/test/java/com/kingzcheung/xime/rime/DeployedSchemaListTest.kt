package com.kingzcheung.xime.rime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** [RimeConfigHelper.deployedSchemaList] 与 [RimeConfigHelper.missingSchemaReason]：区分「没编出来」和「引擎还没刷新」。 */
class DeployedSchemaListTest {
    private fun build(): File = File(System.getProperty("java.io.tmpdir"), "deployed-list-${System.nanoTime()}").apply { mkdirs() }

    private val deployed = """
        |__build_info:
        |  rime_version: 1.17.0
        |schema_list:
        |  - schema: rime_ice
        |  - schema: t9_pinyin
        |  - schema: "cyime_zhuyin_ten"
        |spacer:
        |  max_width: 1
        |style:
        |  font_point: 14
        |""".trimMargin()

    @Test
    fun `只取 schema_list 里的方案`() {
        val dir = build()
        File(dir, "default.yaml").writeText(deployed)
        assertEquals(setOf("rime_ice", "t9_pinyin", "cyime_zhuyin_ten"), RimeConfigHelper.deployedSchemaList(dir))
        dir.deleteRecursively()
    }

    @Test
    fun `没有部署产物时为空`() {
        val dir = build()
        assertTrue(RimeConfigHelper.deployedSchemaList(dir).isEmpty())
        dir.deleteRecursively()
    }

    /** 编好了、部署产物里也列着，只是引擎的可用列表还没刷新：不算失败，别把当前方案换走。 */
    @Test
    fun `编译产物齐全时以部署产物里的列表为准`() {
        val dir = build()
        File(dir, "numbers_ext.schema.yaml").writeText("schema:\n  schema_id: numbers_ext\n")
        assertNull(RimeConfigHelper.missingSchemaReason(dir, "numbers_ext", emptySet(), setOf("numbers_ext")))
        assertNull(RimeConfigHelper.missingSchemaReason(dir, "numbers_ext", setOf("numbers_ext"), emptySet()))
        assertEquals("不在部署结果的方案列表里",
            RimeConfigHelper.missingSchemaReason(dir, "numbers_ext", emptySet(), emptySet()))
        dir.deleteRecursively()
    }
}
