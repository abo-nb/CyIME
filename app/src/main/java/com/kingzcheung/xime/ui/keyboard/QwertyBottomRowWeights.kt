package com.kingzcheung.xime.ui.keyboard

internal data class QwertyBottomRowWeights(
    val mode: Float, val punctuation: Float, val space: Float, val language: Float, val enter: Float,
) {
    val total: Float get() = mode * 2 + punctuation + space + language + enter
    companion object {
        // 总权重10：逗号、地球各占1个字母键；符号/数字入口各加宽20%。
        val Standard = QwertyBottomRowWeights(1.2f, 1f, 3.9f, 1f, 1.7f)
        val Legacy = QwertyBottomRowWeights(0.7f, 0.6f, 3f, 0.8f, 1.2f)
        /** 注音（十列）底排：空格旁多 ㄦ 和删除键，两者份数另计，合起来仍和十列网格对齐。 */
        val ZhuyinTen = QwertyBottomRowWeights(0.9f, 0.8f, 2.5f, 0.8f, 1.5f)
    }
}

/** 注音（十列）底排的「ㄦ」和删除键份数：加上 [QwertyBottomRowWeights.ZhuyinTen] 正好十格。 */
internal const val ZHUYIN_TEN_ER_WEIGHT = 1f
internal const val ZHUYIN_TEN_DELETE_WEIGHT = 1.6f
