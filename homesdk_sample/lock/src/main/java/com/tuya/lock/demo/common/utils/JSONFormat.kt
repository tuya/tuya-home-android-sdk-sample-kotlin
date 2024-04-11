package com.tuya.lock.demo.common.utils

/**
 *
 * Created by HuiYao on 2024/2/29
 */
object JSONFormat {

    @JvmStatic
    fun format(mJson: String?): String? {
        if (mJson.isNullOrEmpty()) return ""
        val source = StringBuilder(mJson)
        if (mJson == "") {
            return null
        }
        var offset = 0 //目标字符串插入空格偏移量
        var bOffset = 0 //空格偏移量
        for (i in mJson.indices) {
            when (mJson[i]) {
                '{', '[' -> {
                    bOffset += 4
                    source.insert(i + offset + 1, "\n" + generateBlank(bOffset))
                    offset += bOffset + 1
                }
                ',' -> {
                    source.insert(i + offset + 1, "\n" + generateBlank(bOffset));
                    offset += bOffset + 1
                }
                '}', ']' -> {
                    bOffset -= 4
                    source.insert(i + offset, "\n" + generateBlank(bOffset));
                    offset += bOffset + 1
                }
            }
        }
        return source.toString()
    }

    private fun generateBlank(num: Int): String {
        val stringBuilder = StringBuilder()
        for (i in 0 until num) {
            stringBuilder.append(" ")
        }
        return stringBuilder.toString()
    }
}