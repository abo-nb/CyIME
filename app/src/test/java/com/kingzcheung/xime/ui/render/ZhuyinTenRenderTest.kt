package com.kingzcheung.xime.ui.render

import android.app.Application
import android.content.res.Configuration
import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kingzcheung.xime.settings.FrostedGlassConfig
import com.kingzcheung.xime.settings.KeysConfigHelper
import com.kingzcheung.xime.ui.keyboard.KeyboardCallbacks
import com.kingzcheung.xime.ui.keyboard.KeyboardInputPreferences
import com.kingzcheung.xime.ui.keyboard.KeyboardLayout
import com.kingzcheung.xime.ui.keyboard.LocalKeyboardExplicitWidth
import com.kingzcheung.xime.ui.keyboard.LocalKeyboardInputPreferences
import com.kingzcheung.xime.ui.theme.KeyboardThemes
import com.kingzcheung.xime.ui.theme.LocalKeyboardPalette
import com.kingzcheung.xime.ui.theme.keyboardBackground
import com.kingzcheung.xime.ui.theme.resolveKeyboardPalette
import com.kingzcheung.xime.viewmodel.KeyboardUiState
import com.kingzcheung.xime.viewmodel.KeyboardViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * 注音（大千·十列）：四行都是 10 键、上下对齐；ㄦ 和删除键在底排，第四行末尾不再被删除键挤出第 11 列。
 * 数字排的角标和其余键的字母角标是键帽提示，不是第二套键。
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w1000dp-h900dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ZhuyinTenRenderTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var vm: KeyboardViewModel

    @Before fun setUp() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        KeysConfigHelper.loadConfig(app)
        KeyboardThemes.initFromConfig(app)
        com.kingzcheung.xime.ui.keyboard.AppFonts.initialize(app)
        vm = KeyboardViewModel(app)
        KeysConfigHelper.setActiveKeyboardSchema("cyime_zhuyin_ten")
        rule.setContent {
            val theme = "soft_blue"
            val dark = false
            val palette = resolveKeyboardPalette(theme, dark)
            val glass = FrostedGlassConfig.defaults(dark, enabled = false)
            val width = 400.dp
            val height = 300.dp
            val configuration = Configuration(LocalConfiguration.current).apply {
                orientation = Configuration.ORIENTATION_PORTRAIT
                screenWidthDp = 400
                screenHeightDp = 860
                smallestScreenWidthDp = 400
            }
            CompositionLocalProvider(LocalConfiguration provides configuration,
                LocalKeyboardExplicitWidth provides true, LocalKeyboardPalette provides palette,
                LocalKeyboardInputPreferences provides KeyboardInputPreferences(frostedGlass = glass)) {
                MaterialTheme {
                    Box(Modifier.requiredSize(width, height).testTag("layout-body")
                        .keyboardBackground(KeyboardThemes.getRenderingScheme(theme).keyboardBackground, dark,
                            palette.background, glass)) {
                        KeyboardLayout({}, vm, KeyboardCallbacks(onKeyPress = { _, _ -> }, onCandidateSelect = {}),
                            KeyboardUiState(currentSchemaId = "cyime_zhuyin_ten", schemaName = "注音十列",
                                themeId = theme, isDarkTheme = dark),
                            false, Modifier.requiredSize(width, height))
                    }
                }
            }
        }
        rule.runOnIdle { vm.resetKeyboard(false, "cyime_zhuyin_ten") }
        rule.waitForIdle()
    }

    private fun bounds(tag: String): Rect = rule.onNodeWithTag(tag, true).fetchSemanticsNode().boundsInRoot

    private fun shot(): Bitmap {
        val body = rule.onNodeWithTag("layout-body", true).fetchSemanticsNode().boundsInRoot
        val root = rule.activity.window.decorView
        val full = Bitmap.createBitmap(root.width, root.height, Bitmap.Config.ARGB_8888)
        rule.runOnIdle { root.draw(android.graphics.Canvas(full)) }
        return Bitmap.createBitmap(full, body.left.toInt().coerceAtLeast(0), body.top.toInt().coerceAtLeast(0),
            body.width.toInt().coerceAtMost(full.width), body.height.toInt().coerceAtMost(full.height))
    }

    private fun save(bitmap: Bitmap) {
        val out = System.getenv("CYIME_RENDER_OUT")?.takeIf { it.isNotEmpty() } ?: return
        File(out).mkdirs()
        File(out, "zhuyin-ten.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test fun tenColumnsWithErAndDeleteOnTheBottomRow() {
        // 四行首键左边缘一致：ㄅ/ㄆ/ㄇ/ㄈ 上下对齐（十列不缩进半键）。
        val firstKeys = listOf("ㄅ", "ㄆ", "ㄇ", "ㄈ").map { bounds("qwerty-key:$it") }
        firstKeys.zipWithNext().forEach { (above, below) ->
            assertEquals(above.left, below.left, 1.5f)
            assertEquals(above.right, below.right, 1.5f)
        }
        // 四行末键右边缘一致：第一行的 ㄢ 也是 10 列的一部分，第四行没有删除键占第 11 列。
        val rowEnds = listOf("ㄢ", "ㄣ", "ㄤ", "ㄥ").map { bounds("qwerty-key:$it").right }
        assertTrue("四行末键右边缘：$rowEnds", rowEnds.max() - rowEnds.min() <= 1.5f)

        // ㄦ 和删除键在底排，ㄦ 在删除键左边；第四行 10 键下面才出现删除键。
        val er = bounds("zhuyin-er-key")
        val delete = bounds("qwerty-delete-key")
        val lastRow = bounds("qwerty-key:ㄥ")
        assertTrue("ㄦ 应在底排", er.top >= lastRow.bottom - 1.5f)
        assertTrue("删除键应在底排", delete.top >= lastRow.bottom - 1.5f)
        assertTrue("ㄦ 应在删除键左边", er.right <= delete.left + 1.5f)
        assertEquals(er.top, delete.top, 1.5f)

        // 键帽角标：数字排的 0 和字母排的 q 都只是提示文字。
        assertTrue(rule.onAllNodesWithText("0", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty())
        assertTrue(rule.onAllNodesWithText("q", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty())

        // 底排顺序：，→ 🌐 → 空格 → ㄦ → 删除 → 回车，六键从左到右排列且互不重叠。
        val bottomOrder = listOf("，" to "qwerty-punctuation-key", "🌐" to "qwerty-language-key",
            "空格" to "qwerty-space-key", "ㄦ" to "zhuyin-er-key", "删除" to "qwerty-delete-key",
            "回车" to "qwerty-enter-key")
        val bottomKeys = bottomOrder.map { (label, tag) -> Triple(label, tag, bounds(tag)) }
        bottomKeys.zipWithNext().forEach { (left, right) ->
            val (leftLabel, leftTag, leftRect) = left
            val (rightLabel, rightTag, rightRect) = right
            assertTrue("底排应从左到右为 ${bottomOrder.joinToString("") { it.first }}；" +
                "$leftLabel($leftTag).right=${leftRect.right} 超过了 $rightLabel($rightTag).left=${rightRect.left}",
                leftRect.right <= rightRect.left + 1.5f)
        }
        save(shot())
    }
}
