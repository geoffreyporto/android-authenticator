package org.tokend.authenticator.accounts.add.view.util

import android.content.Context
import android.graphics.*
import android.support.annotation.ColorInt
import android.support.v4.content.ContextCompat
import android.support.v4.graphics.PaintCompat
import android.support.v4.util.LruCache
import org.tokend.authenticator.R
import org.tokend.authenticator.accounts.data.model.Account

/**
 * Creates fancy circle logos for assets.
 */
class AccountLogoFactory(private val context: Context) {
    private val colors = listOf(
            "#80CBC4",
            "#80DEEA",
            "#82B1FF",
            "#8C9EFF",
            "#90CAF9",
            "#9FA8DA",
            "#A5D6A7",
            "#B0BEC5",
            "#BCAAA4",
            "#C5E1A5",
            "#CE93D8",
            "#DCE775",
            "#EF9A9A",
            "#F48FB1",
            "#FBC02D",
            "#FF8A80",
            "#FFAB91",
            "#FFCC80",
            "#FFD54F"
    )
            .map { Color.parseColor(it) }

    fun getWithAutoBackground(account: Account,
                              size: Int,
                              @ColorInt
                              fontColor: Int = ContextCompat.getColor(context, R.color.white)
    ): Bitmap {
        val string = "${account.network.name}${account.email}"
        val code = string.toCharArray().fold(0) { result, char ->
            (31 * result + char.toInt()) % colors.size
        }
        val background = colors[code]

        val key = "${string}_$size"
        return cache.get(key)
                ?: generateForValue(account.network.name, size, background, fontColor)
                        .also { cache.put(key, it) }
    }

    /**
     * Returns [Bitmap] logo for given asset code by first letter.
     * If first letter cannot be displayed it will be replaced with emoj.
     */
    private fun generateForValue(networkName: String,
                                 size: Int,
                                 @ColorInt
                                 backgroundColor: Int,
                                 @ColorInt
                                 fontColor: Int
    ): Bitmap {
        val letter = networkName.firstOrNull()?.toString()
        return generate(letter, size, backgroundColor, fontColor)
    }

    private fun generate(content: String?,
                         size: Int,
                         backgroundColor: Int,
                         fontColor: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)

        val canvas = Canvas(bitmap)
        val paint = Paint()

        val center = size / 2f

        paint.color = backgroundColor
        paint.style = Paint.Style.FILL
        paint.isAntiAlias = true
        canvas.drawCircle(center, center, center, paint)

        paint.color = fontColor
        paint.style = Paint.Style.FILL
        paint.textSize = size * 0.65f
        paint.textAlign = Paint.Align.CENTER
        paint.typeface = Typeface.SANS_SERIF

        content
                ?.takeIf {
                    it.isNotBlank() &&
                            PaintCompat.hasGlyph(paint, it)
                }
                .let {
                    it
                            ?: "🗝".also { paint.textSize = size * 0.45f }
                }
                .let { toDraw ->
                    canvas.drawText(toDraw, center,
                            center - ((paint.descent() + paint.ascent()) / 2), paint)
                }

        return bitmap
    }

    private companion object {
        private val cache = LruCache<String, Bitmap>(25)
    }
}