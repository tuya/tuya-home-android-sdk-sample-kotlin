package com.tuya.smart.android.demo.camera.utils

import android.os.Message

/**

 * TODO feature

 *

 * @author houqing <a href="mailto:developer@tuya.com"/>

 * @since 2021/7/26 3:37 PM

 */
class MessageUtil {
    companion object{
        fun getMessage(msgWhat: Int, arg: Int): Message{
            val msg = Message()
            msg.what = msgWhat
            msg.arg1 = arg
            return msg
        }
    }
}