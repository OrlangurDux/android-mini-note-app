package app.mininote.mininote.util

import android.graphics.Bitmap
import androidx.core.graphics.createBitmap
import androidx.core.graphics.set
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

/**
 * Кодирует произвольный текст (здесь — `otpauth://totp/...` URI для 2FA-настройки, см.
 * AuthRepository.enableTfa) в чёрно-белый QR-битмап. Синхронно и без сторонних потоков —
 * кодирование короткой строки занимает единицы миллисекунд, отдельный воркер/корутина ради
 * этого избыточны (вызывается из `remember` на композиции).
 */
fun generateQrBitmap(content: String, sizePx: Int = 512): Bitmap? {
    return runCatching {
        val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx)
        val bitmap = createBitmap(matrix.width, matrix.height, Bitmap.Config.RGB_565)
        for (x in 0 until matrix.width) {
            for (y in 0 until matrix.height) {
                bitmap[x, y] = if (matrix[x, y]) BLACK else WHITE
            }
        }
        bitmap
    }.getOrNull()
}

private const val BLACK = android.graphics.Color.BLACK
private const val WHITE = android.graphics.Color.WHITE
