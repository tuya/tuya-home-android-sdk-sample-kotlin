package com.tuya.appsdk.sample.device.config.util.qrcode

import android.graphics.Bitmap
import android.graphics.Color
import android.text.TextUtils
import androidx.annotation.ColorInt
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.WriterException
import com.google.zxing.qrcode.QRCodeWriter
import java.util.*
import kotlin.collections.HashMap

/**
 * TODO feature
 *
 * @author hou qing <a href="mailto:developer@tuya.com"/>
 * @since 2021/7/28 2:54 下午
 */
class QRCodeUtil {
    /**
     * Create a QR code bitmap
     * @param content content(Support Chinese)
     * @param width width, unit px
     * @param height height, unit px
     */
    fun createQRCodeBitmap(content: String?, width: Int, height: Int): Bitmap? {
        return createQRCodeBitmap(
            content,
            width,
            height,
            "UTF-8",
            "H",
            "2",
            Color.BLACK,
            Color.WHITE
        )
    }

    /**
     * Create QR code bitmap (support custom configuration and custom style)
     *
     * @param content content(Support Chinese)
     * @param width width, unit px
     * @param height height, unit px
     * @param character_set Character set/character transcoding format. When passing null, zxing source code uses "ISO-8859-1" by default
     * @param error_correction Fault tolerance level. When passing null, zxing source code uses "L" by default
     * @param margin Blank margin (can be modified, requirement: integer and >=0), when passing null, zxing source code uses "4" by default
     * @param color_black Custom color value of black color block
     * @param color_white Custom color value of white color block
     * @return
     */
    private fun createQRCodeBitmap(
        content: String?, width: Int, height: Int,
        character_set: String?, error_correction: String?, margin: String?,
        @ColorInt color_black: Int, @ColorInt color_white: Int): Bitmap? {
        /** 1.Parameter legality judgment  */
        if (TextUtils.isEmpty(content)) { // The string content is blank
            return null
        }
        if (width < 0 || height < 0) { // Both width and height need to be >=0
            return null
        }
        try {
            /** 2.Set the QR code related configuration and generate BitMatrix objects  */
            val hints = Hashtable<EncodeHintType, String?>()
            if (!TextUtils.isEmpty(character_set)) {
                hints[EncodeHintType.CHARACTER_SET] =
                    character_set // Character transcoding format setting
            }
            if (!TextUtils.isEmpty(error_correction)) {
                hints[EncodeHintType.ERROR_CORRECTION] =
                    error_correction // Fault tolerance level setting
            }
            if (!TextUtils.isEmpty(margin)) {
                hints[EncodeHintType.MARGIN] = margin // Margin settings
            }
            val bitMatrix =
                QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, width, height, hints)

            /** 3.Create a pixel array and assign color values to the array elements according to the BitMatrix object  */
            val pixels = IntArray(width * height)
            for (y in 0 until height) {
                for (x in 0 until width) {
                    if (bitMatrix[x, y]) {
                        pixels[y * width + x] = color_black // Black color block pixel settings
                    } else {
                        pixels[y * width + x] = color_white // White color block pixel setting
                    }
                }
            }
            /** 4.Create a Bitmap object, set the color value of each pixel of the Bitmap according to the pixel array, and then return the Bitmap object  */
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
            return bitmap
        } catch (e: WriterException) {
            e.printStackTrace()
        }
        return null
    }

    companion object{
        @Throws(WriterException::class)
        fun createQRCode(url: String?, widthAndHeight: Int): Bitmap {
            val hints: Hashtable<EncodeHintType, Any> = Hashtable()
            hints[EncodeHintType.CHARACTER_SET] = "utf-8"
            hints[EncodeHintType.MARGIN] = 0
            val matrix = MultiFormatWriter().encode(url,
                BarcodeFormat.QR_CODE, widthAndHeight, widthAndHeight, hints
            )
            val width = matrix.width
            val height = matrix.height
            val pixels = IntArray(width * height)
            for (y in 0 until height) {
                for (x in 0 until width) {
                    if (matrix[x, y]) {
                        pixels[y * width + x] = Color.BLACK //0xff000000
                    }
                }
            }
            val bitmap = Bitmap.createBitmap(
                width, height,
                Bitmap.Config.ARGB_8888
            )
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
            return bitmap
        }
    }
}