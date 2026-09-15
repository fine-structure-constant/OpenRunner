package cn.edu.pku.openrunner.feature.run.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import kotlin.math.min
import kotlin.math.roundToInt

/** Stores a durable, compressed copy so a picker URI does not need to stay readable. */
class RunPhotoStore(context: Context) {
    private val appContext = context.applicationContext
    private val resolver = appContext.contentResolver
    private val photoDirectory = File(appContext.filesDir, DIRECTORY_NAME)

    fun save(source: Uri, localId: String): String {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(source)?.use {
            BitmapFactory.decodeStream(it, null, bounds)
        } ?: error("无法读取所选图片")
        check(bounds.outWidth > 0 && bounds.outHeight > 0) { "所选文件不是可识别的图片" }

        val options = BitmapFactory.Options().apply {
            inSampleSize = calculateSampleSize(bounds.outWidth, bounds.outHeight)
        }
        val decoded = resolver.openInputStream(source)?.use {
            BitmapFactory.decodeStream(it, null, options)
        } ?: error("图片解码失败")
        val scale = min(
            1f,
            min(MAX_WIDTH.toFloat() / decoded.width, MAX_HEIGHT.toFloat() / decoded.height)
        )
        val output = if (scale < 1f) {
            Bitmap.createScaledBitmap(
                decoded,
                (decoded.width * scale).roundToInt().coerceAtLeast(1),
                (decoded.height * scale).roundToInt().coerceAtLeast(1),
                true
            )
        } else {
            decoded
        }

        check(photoDirectory.exists() || photoDirectory.mkdirs()) { "无法创建图片缓存目录" }
        val destination = File(photoDirectory, "$localId.jpg")
        try {
            FileOutputStream(destination).use { stream ->
                check(output.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, stream)) {
                    "图片压缩失败"
                }
            }
        } finally {
            if (output !== decoded) output.recycle()
            decoded.recycle()
        }
        return destination.absolutePath
    }

    fun delete(path: String?) {
        val target = path?.let(::File)?.canonicalFile ?: return
        val allowedDirectory = photoDirectory.canonicalFile
        if (target.parentFile == allowedDirectory && target.isFile) target.delete()
    }

    private fun calculateSampleSize(width: Int, height: Int): Int {
        var sampleSize = 1
        while (width / sampleSize > MAX_DECODE_WIDTH || height / sampleSize > MAX_DECODE_HEIGHT) {
            sampleSize *= 2
        }
        return sampleSize
    }

    companion object {
        private const val DIRECTORY_NAME = "run_photos"
        private const val MAX_WIDTH = 640
        private const val MAX_HEIGHT = 480
        private const val MAX_DECODE_WIDTH = 1_280
        private const val MAX_DECODE_HEIGHT = 960
        private const val JPEG_QUALITY = 50
    }
}
