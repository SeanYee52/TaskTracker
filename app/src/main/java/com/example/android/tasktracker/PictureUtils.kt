import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import java.io.File
import kotlin.math.roundToInt

fun getScaledBitmap(bitmap: Bitmap, destWidth: Int, destHeight: Int): Bitmap {
    val srcWidth = bitmap.width
    val srcHeight = bitmap.height

    val scale = minOf(destWidth.toFloat() / srcWidth, destHeight.toFloat() / srcHeight)
    val scaledWidth = (scale * srcWidth).toInt()
    val scaledHeight = (scale * srcHeight).toInt()

    return Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true)
}

fun getRotatedBitmap(photoFile: File): Bitmap? {
    val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
    val exif = ExifInterface(photoFile.absolutePath)
    val orientation =
        exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
    val matrix = Matrix()

    when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
        ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
        ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1.0f, 1.0f)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1.0f, -1.0f)
    }

    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}
