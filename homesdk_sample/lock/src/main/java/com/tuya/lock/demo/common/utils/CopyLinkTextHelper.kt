package com.tuya.lock.demo.common.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context

/**
 *
 * Created by HuiYao on 2024/2/29
 */
object CopyLinkTextHelper {

    fun copyText(context: Context, text: String?) {
        // 创建能够存入剪贴板的ClipData对象
        //‘Label’这是任意文字标签
        val mClipData = ClipData.newPlainText("Label", text)
        //将ClipData数据复制到剪贴板：
        val clipboardManager: ClipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboardManager.setPrimaryClip(mClipData)
    }

}