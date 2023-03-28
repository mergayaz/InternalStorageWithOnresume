package kz.kuz.internalstoragewithonresume

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Point
import kotlin.math.roundToInt

// цель данного класса - уменьшить размер фотографии под размер экрана
object PictureUtils {
    fun getScaledBitmap(path: String?, activity: Activity): Bitmap {
        // получение размера экрана (метод устаревший, но соврменный работает плохо)
        val size = Point()
        activity.windowManager.defaultDisplay.getSize(size)
        val screenWidth = size.x
        val screenHeight = size.y

        // чтение размеров изображения на диске
        var options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeFile(path, options)
        val srcWidth = options.outWidth.toFloat()
        val srcHeight = options.outHeight.toFloat()

        // вычисление степени масштабирования
        var inSampleSize = 1
        if (srcHeight > screenHeight || srcWidth > screenWidth) {
            val heightScale = srcHeight / screenHeight
            val widthScale = srcWidth / screenWidth
            inSampleSize = if (heightScale > widthScale) heightScale.toInt() else
                widthScale.roundToInt()
        }
        options = BitmapFactory.Options()
        options.inSampleSize = inSampleSize

        // чтение данных и создание итогового изображения
        return BitmapFactory.decodeFile(path, options)
    }
}