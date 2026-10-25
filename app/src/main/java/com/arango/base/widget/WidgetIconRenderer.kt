package com.arango.base.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.VectorGroup
import androidx.compose.ui.graphics.vector.VectorPath
import androidx.compose.ui.graphics.vector.toPath

object WidgetIconRenderer {

    /**
     * Renders a Compose [ImageVector] inside a circular colored badge [Bitmap]
     * suitable for [android.widget.RemoteViews.setImageViewBitmap].
     */
    fun renderCategoryBadgeBitmap(
        icon: ImageVector,
        badgeColor: Color,
        iconTint: Int = android.graphics.Color.WHITE,
        sizePx: Int = 72
    ): Bitmap {
        val safeSize = sizePx.coerceAtLeast(24)
        val bitmap = Bitmap.createBitmap(safeSize, safeSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = try {
                badgeColor.toArgb()
            } catch (_: Exception) {
                0xFF2E7D32.toInt()
            }
        }
        val radius = safeSize / 2f
        canvas.drawCircle(radius, radius, radius, bgPaint)

        val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = iconTint
        }

        val viewportWidth = icon.viewportWidth.takeIf { it > 0f } ?: 24f
        val viewportHeight = icon.viewportHeight.takeIf { it > 0f } ?: 24f
        val iconPadding = safeSize * 0.22f
        val availableWidth = safeSize - (iconPadding * 2f)
        val availableHeight = safeSize - (iconPadding * 2f)

        val scaleX = availableWidth / viewportWidth
        val scaleY = availableHeight / viewportHeight

        val matrix = Matrix().apply {
            postScale(scaleX, scaleY)
            postTranslate(iconPadding, iconPadding)
        }

        drawVectorGroup(canvas, icon.root, matrix, iconPaint)
        return bitmap
    }

    private fun drawVectorGroup(
        canvas: Canvas,
        group: VectorGroup,
        parentMatrix: Matrix,
        paint: Paint
    ) {
        val groupMatrix = Matrix(parentMatrix)
        if (group.translationX != 0f || group.translationY != 0f) {
            groupMatrix.preTranslate(group.translationX, group.translationY)
        }
        if (group.rotation != 0f) {
            groupMatrix.preRotate(group.rotation, group.pivotX, group.pivotY)
        }
        if (group.scaleX != 1f || group.scaleY != 1f) {
            groupMatrix.preScale(group.scaleX, group.scaleY, group.pivotX, group.pivotY)
        }

        for (i in 0 until group.size) {
            when (val node = group[i]) {
                is VectorPath -> {
                    val composePath = node.pathData.toPath()
                    val androidPath = android.graphics.Path(composePath.asAndroidPath())
                    androidPath.transform(groupMatrix)
                    canvas.drawPath(androidPath, paint)
                }
                is VectorGroup -> {
                    drawVectorGroup(canvas, node, groupMatrix, paint)
                }
            }
        }
    }
}
