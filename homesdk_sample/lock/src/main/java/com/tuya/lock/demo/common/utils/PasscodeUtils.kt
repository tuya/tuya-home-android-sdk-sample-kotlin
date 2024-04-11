package com.tuya.lock.demo.common.utils

import java.security.SecureRandom

/**
 *
 * Created by HuiYao on 2024/2/29
 */
object PasscodeUtils {

    /**
     * @param digits 位数
     * @return 随机生成密码
     */
    @JvmStatic
    fun getRandom(digits: Int): String {
        val randomString = StringBuilder()
        val random = SecureRandom()
        for (i in 0 until digits) {
            randomString.append(random.nextInt(9))
        }
        return randomString.toString()
    }
}