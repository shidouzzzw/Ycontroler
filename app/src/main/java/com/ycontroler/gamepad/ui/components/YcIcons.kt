package com.ycontroler.gamepad.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Uygulamanın özel ikon seti — Material varsayılanları yerine ince, geometrik çizgiler. */
enum class YcIcon {
    Back, Close, Gear, Sliders, Bluetooth, Gamepad, Grid, Palette, Info, Plus, Trash, Check,
    Search, ChevronDown, ChevronRight, Power, Refresh, Edit, Download, Upload, Warning,
    Battery, Signal, Home, Moon, Language, Sparkle, Star, Play, Link, Lock, Target, Zap, Rotate,
    Hand, Gauge, Screen, Saved
}

@Composable
fun YcIcon(
    icon: YcIcon,
    modifier: Modifier = Modifier.size(20.dp),
    tint: Color = LocalContentColor.current,
    strokeWidth: Dp = 1.7.dp
) {
    Canvas(modifier) {
        val factor = size.width / 24f
        scale(factor, factor, pivot = Offset.Zero) {
            drawYcIcon(icon, tint, strokeWidth.value)
        }
    }
}

private fun DrawScope.drawYcIcon(icon: YcIcon, color: Color, sw: Float) {
    val stroke = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round)
    fun line(x1: Float, y1: Float, x2: Float, y2: Float) =
        drawLine(color, Offset(x1, y1), Offset(x2, y2), strokeWidth = sw, cap = StrokeCap.Round)

    fun path(build: Path.() -> Unit) {
        drawPath(Path().apply(build), color, style = stroke)
    }

    when (icon) {
        YcIcon.Back -> path {
            moveTo(14.5f, 5.5f); lineTo(8f, 12f); lineTo(14.5f, 18.5f)
        }
        YcIcon.ChevronRight -> path {
            moveTo(9.5f, 5.5f); lineTo(16f, 12f); lineTo(9.5f, 18.5f)
        }
        YcIcon.ChevronDown -> path {
            moveTo(6f, 9.5f); lineTo(12f, 16f); lineTo(18f, 9.5f)
        }
        YcIcon.Close -> {
            line(6.5f, 6.5f, 17.5f, 17.5f); line(17.5f, 6.5f, 6.5f, 17.5f)
        }
        YcIcon.Plus -> {
            line(12f, 5.5f, 12f, 18.5f); line(5.5f, 12f, 18.5f, 12f)
        }
        YcIcon.Check -> path {
            moveTo(5f, 12.6f); lineTo(10f, 17.5f); lineTo(19f, 7.5f)
        }
        YcIcon.Gear -> {
            drawCircle(color, radius = 7.2f, center = Offset(12f, 12f), style = stroke)
            drawCircle(color, radius = 3f, center = Offset(12f, 12f), style = stroke)
            for (i in 0 until 8) {
                val a = Math.toRadians(i * 45.0)
                val cx = 12f + (Math.cos(a) * 9.6f).toFloat()
                val cy = 12f + (Math.sin(a) * 9.6f).toFloat()
                line(12f + (Math.cos(a) * 7.2f).toFloat(), 12f + (Math.sin(a) * 7.2f).toFloat(), cx, cy)
            }
        }
        YcIcon.Sliders -> {
            line(4f, 7f, 20f, 7f); line(4f, 12f, 20f, 12f); line(4f, 17f, 20f, 17f)
            drawCircle(color, 2.3f, Offset(9f, 7f), style = Stroke(sw, cap = StrokeCap.Round))
            drawCircle(color, 2.3f, Offset(15f, 12f), style = Stroke(sw, cap = StrokeCap.Round))
            drawCircle(color, 2.3f, Offset(7.5f, 17f), style = Stroke(sw, cap = StrokeCap.Round))
        }
        YcIcon.Bluetooth -> path {
            moveTo(8f, 7.4f); lineTo(16f, 16.6f); lineTo(12f, 20f); lineTo(12f, 4f)
            lineTo(16f, 7.4f); lineTo(8f, 16.6f)
        }
        YcIcon.Gamepad -> {
            path {
                moveTo(8.4f, 8f); lineTo(15.6f, 8f)
                cubicTo(19.5f, 8f, 21f, 12.5f, 20.2f, 16.2f)
                cubicTo(19.7f, 18.5f, 17.4f, 18.6f, 16.2f, 16.6f)
                lineTo(15f, 14.6f); lineTo(9f, 14.6f); lineTo(7.8f, 16.6f)
                cubicTo(6.6f, 18.6f, 4.3f, 18.5f, 3.8f, 16.2f)
                cubicTo(3f, 12.5f, 4.5f, 8f, 8.4f, 8f)
            }
            line(9.2f, 11.4f, 9.2f, 13.4f)
            line(8.2f, 12.4f, 10.2f, 12.4f)
            drawCircle(color, 1f, Offset(15.4f, 11.6f))
            drawCircle(color, 1f, Offset(16.9f, 13.1f))
        }
        YcIcon.Grid -> {
            drawRoundRect(color, Offset(4f, 4f), Size(6.4f, 6.4f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.8f), style = stroke)
            drawRoundRect(color, Offset(13.6f, 4f), Size(6.4f, 6.4f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.8f), style = stroke)
            drawRoundRect(color, Offset(4f, 13.6f), Size(6.4f, 6.4f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.8f), style = stroke)
            drawRoundRect(color, Offset(13.6f, 13.6f), Size(6.4f, 6.4f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.8f), style = stroke)
        }
        YcIcon.Palette -> {
            path {
                moveTo(12f, 3.6f)
                cubicTo(6.4f, 3.6f, 3.4f, 8f, 3.4f, 12.4f)
                cubicTo(3.4f, 17.4f, 7.6f, 20.4f, 11.8f, 20.4f)
                cubicTo(13.6f, 20.4f, 14.2f, 19f, 13.6f, 17.8f)
                cubicTo(12.4f, 15.6f, 15f, 14.4f, 17.4f, 14.4f)
                cubicTo(19.6f, 14.4f, 20.6f, 12.6f, 20.6f, 10.4f)
                cubicTo(20.6f, 6.2f, 16.8f, 3.6f, 12f, 3.6f)
            }
            drawCircle(color, 1.1f, Offset(8.2f, 9.4f))
            drawCircle(color, 1.1f, Offset(12f, 7.4f))
            drawCircle(color, 1.1f, Offset(15.8f, 9.4f))
        }
        YcIcon.Info -> {
            drawCircle(color, 8.4f, Offset(12f, 12f), style = stroke)
            drawCircle(color, 1.15f, Offset(12f, 7.8f))
            line(12f, 11f, 12f, 16.4f)
        }
        YcIcon.Trash -> {
            line(4.6f, 7f, 19.4f, 7f)
            path { moveTo(6.4f, 7f); lineTo(7.4f, 19.6f); lineTo(16.6f, 19.6f); lineTo(17.6f, 7f) }
            path { moveTo(9.4f, 7f); lineTo(9.4f, 4.6f); lineTo(14.6f, 4.6f); lineTo(14.6f, 7f) }
            line(10.6f, 10.6f, 10.6f, 16f); line(13.4f, 10.6f, 13.4f, 16f)
        }
        YcIcon.Search -> {
            drawCircle(color, 6.2f, Offset(10.6f, 10.6f), style = stroke)
            line(15.2f, 15.2f, 20f, 20f)
        }
        YcIcon.Power -> {
            path {
                moveTo(8.2f, 7.4f)
                cubicTo(5.4f, 9f, 4f, 12f, 4.6f, 15f)
                cubicTo(5.4f, 18.6f, 8.8f, 20.6f, 12f, 20.4f)
                cubicTo(15.2f, 20.6f, 18.6f, 18.6f, 19.4f, 15f)
                cubicTo(20f, 12f, 18.6f, 9f, 15.8f, 7.4f)
            }
            line(12f, 3.6f, 12f, 11f)
        }
        YcIcon.Refresh -> {
            path {
                moveTo(19.4f, 12f)
                cubicTo(19.4f, 7.8f, 16.1f, 4.6f, 12f, 4.6f)
                cubicTo(8.4f, 4.6f, 5.4f, 7.1f, 4.6f, 10.4f)
            }
            path { moveTo(3.4f, 6.4f); lineTo(4.6f, 10.6f); lineTo(8.6f, 9.4f) }
            path {
                moveTo(4.6f, 12f)
                cubicTo(4.6f, 16.2f, 7.9f, 19.4f, 12f, 19.4f)
                cubicTo(15.6f, 19.4f, 18.6f, 16.9f, 19.4f, 13.6f)
            }
            path { moveTo(20.6f, 17.6f); lineTo(19.4f, 13.4f); lineTo(15.4f, 14.6f) }
        }
        YcIcon.Edit -> {
            path { moveTo(4.4f, 19.6f); lineTo(5.2f, 15.4f); lineTo(15.8f, 4.8f); lineTo(19.2f, 8.2f); lineTo(8.6f, 18.8f); close() }
            line(13.6f, 7f, 17f, 10.4f)
        }
        YcIcon.Download -> {
            line(12f, 4f, 12f, 15f)
            path { moveTo(7.4f, 10.6f); lineTo(12f, 15.2f); lineTo(16.6f, 10.6f) }
            path { moveTo(4.6f, 15.6f); lineTo(4.6f, 19.4f); lineTo(19.4f, 19.4f); lineTo(19.4f, 15.6f) }
        }
        YcIcon.Upload -> {
            line(12f, 15.4f, 12f, 4.4f)
            path { moveTo(7.4f, 9f); lineTo(12f, 4.4f); lineTo(16.6f, 9f) }
            path { moveTo(4.6f, 15.6f); lineTo(4.6f, 19.4f); lineTo(19.4f, 19.4f); lineTo(19.4f, 15.6f) }
        }
        YcIcon.Warning -> {
            path {
                moveTo(12f, 4.2f); lineTo(20.6f, 19.4f); lineTo(3.4f, 19.4f); close()
            }
            line(12f, 9.4f, 12f, 13.6f)
            drawCircle(color, 1.05f, Offset(12f, 16.4f))
        }
        YcIcon.Battery -> {
            drawRoundRect(
                color = color,
                topLeft = Offset(2.6f, 8f),
                size = Size(16.4f, 8f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.2f),
                style = stroke
            )
            line(20.6f, 10.6f, 20.6f, 13.4f)
            drawRoundRect(
                color = color,
                topLeft = Offset(4.6f, 10f),
                size = Size(9f, 4f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(1f)
            )
        }
        YcIcon.Signal -> {
            line(5f, 18.4f, 5f, 14.6f); line(9.7f, 18.4f, 9.7f, 10.6f)
            line(14.4f, 18.4f, 14.4f, 7f); line(19.1f, 18.4f, 19.1f, 4.4f)
        }
        YcIcon.Home -> path {
            moveTo(3.8f, 11f); lineTo(12f, 4f); lineTo(20.2f, 11f)
            moveTo(5.8f, 10f); lineTo(5.8f, 20f); lineTo(18.2f, 20f); lineTo(18.2f, 10f)
        }
        YcIcon.Moon -> path {
            moveTo(19.4f, 14.6f)
            cubicTo(17.8f, 18.6f, 13f, 20.6f, 9f, 19f)
            cubicTo(5f, 17.4f, 3f, 12.6f, 4.6f, 8.6f)
            cubicTo(6f, 5.2f, 9.6f, 3.4f, 13f, 4f)
            cubicTo(9.8f, 6.6f, 8.6f, 11.4f, 11.2f, 14.6f)
            cubicTo(13f, 16.8f, 16.4f, 17.6f, 19.4f, 14.6f)
        }
        YcIcon.Language -> {
            drawCircle(color, 8.4f, Offset(12f, 12f), style = stroke)
            line(3.6f, 12f, 20.4f, 12f)
            path {
                moveTo(12f, 3.6f)
                cubicTo(8.6f, 7.4f, 8.6f, 16.6f, 12f, 20.4f)
                cubicTo(15.4f, 16.6f, 15.4f, 7.4f, 12f, 3.6f)
            }
        }
        YcIcon.Sparkle -> {
            path {
                moveTo(12f, 3.2f)
                cubicTo(12.8f, 8.4f, 15.6f, 11.2f, 20.8f, 12f)
                cubicTo(15.6f, 12.8f, 12.8f, 15.6f, 12f, 20.8f)
                cubicTo(11.2f, 15.6f, 8.4f, 12.8f, 3.2f, 12f)
                cubicTo(8.4f, 11.2f, 11.2f, 8.4f, 12f, 3.2f)
                close()
            }
        }
        YcIcon.Star -> path {
            moveTo(12f, 3.6f); lineTo(14.6f, 9.2f); lineTo(20.6f, 10f); lineTo(16.2f, 14.2f)
            lineTo(17.4f, 20.2f); lineTo(12f, 17.2f); lineTo(6.6f, 20.2f); lineTo(7.8f, 14.2f)
            lineTo(3.4f, 10f); lineTo(9.4f, 9.2f); close()
        }
        YcIcon.Play -> path {
            moveTo(8f, 5.2f); lineTo(19f, 12f); lineTo(8f, 18.8f); close()
        }
        YcIcon.Link -> {
            path {
                moveTo(9.6f, 14.4f)
                cubicTo(8f, 12.8f, 8f, 10.4f, 9.6f, 8.8f)
                lineTo(12.2f, 6.2f)
                cubicTo(13.8f, 4.6f, 16.2f, 4.6f, 17.8f, 6.2f)
                cubicTo(19.4f, 7.8f, 19.4f, 10.2f, 17.8f, 11.8f)
                lineTo(16.4f, 13.2f)
            }
            path {
                moveTo(14.4f, 9.6f)
                cubicTo(16f, 11.2f, 16f, 13.6f, 14.4f, 15.2f)
                lineTo(11.8f, 17.8f)
                cubicTo(10.2f, 19.4f, 7.8f, 19.4f, 6.2f, 17.8f)
                cubicTo(4.6f, 16.2f, 4.6f, 13.8f, 6.2f, 12.2f)
                lineTo(7.6f, 10.8f)
            }
        }
        YcIcon.Lock -> {
            drawRoundRect(
                color = color,
                topLeft = Offset(4.8f, 10.4f),
                size = Size(14.4f, 9.6f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.6f),
                style = stroke
            )
            path {
                moveTo(8f, 10.4f); lineTo(8f, 7.8f)
                cubicTo(8f, 4.6f, 16f, 4.6f, 16f, 7.8f)
                lineTo(16f, 10.4f)
            }
            drawCircle(color, 1.2f, Offset(12f, 15.2f))
        }
        YcIcon.Target -> {
            drawCircle(color, 8.4f, Offset(12f, 12f), style = stroke)
            drawCircle(color, 4.4f, Offset(12f, 12f), style = stroke)
            drawCircle(color, 1.3f, Offset(12f, 12f))
        }
        YcIcon.Zap -> path {
            moveTo(13.4f, 2.8f); lineTo(5.6f, 13.6f); lineTo(11f, 13.6f)
            lineTo(10.4f, 21.2f); lineTo(18.4f, 10.2f); lineTo(12.8f, 10.2f); close()
        }
        YcIcon.Rotate -> {
            path {
                moveTo(5.4f, 9.4f)
                cubicTo(7f, 5.8f, 11.4f, 4.2f, 15f, 5.8f)
                cubicTo(18.6f, 7.4f, 20f, 11.8f, 18.4f, 15.4f)
            }
            path { moveTo(4.4f, 4.6f); lineTo(5.4f, 9.6f); lineTo(10.2f, 8.4f) }
            path {
                moveTo(18.6f, 14.6f)
                cubicTo(17f, 18.2f, 12.6f, 19.8f, 9f, 18.2f)
                cubicTo(5.4f, 16.6f, 4f, 12.2f, 5.6f, 8.6f)
            }
            path { moveTo(19.6f, 19.4f); lineTo(18.6f, 14.4f); lineTo(13.8f, 15.6f) }
        }
        YcIcon.Hand -> path {
            moveTo(8f, 12.6f); lineTo(8f, 6.6f)
            cubicTo(8f, 5.2f, 10.4f, 5.2f, 10.4f, 6.6f)
            lineTo(10.4f, 11.4f)
            moveTo(10.4f, 10.6f); lineTo(10.4f, 5.4f)
            cubicTo(10.4f, 4f, 12.8f, 4f, 12.8f, 5.4f)
            lineTo(12.8f, 10.8f)
            moveTo(12.8f, 10.8f); lineTo(12.8f, 6.6f)
            cubicTo(12.8f, 5.2f, 15.2f, 5.2f, 15.2f, 6.6f)
            lineTo(15.2f, 12.4f)
            moveTo(15.2f, 12.4f); lineTo(15.2f, 9.4f)
            cubicTo(15.2f, 8f, 17.6f, 8f, 17.6f, 9.4f)
            lineTo(17.6f, 15.4f)
            cubicTo(17.6f, 19f, 14.6f, 21f, 12f, 21f)
            cubicTo(9.4f, 21f, 7.4f, 19.6f, 6.4f, 17.2f)
            lineTo(4.6f, 13.8f)
            cubicTo(4f, 12.6f, 5.6f, 11.2f, 6.6f, 12f)
            lineTo(8f, 13.4f)
        }
        YcIcon.Gauge -> {
            path {
                moveTo(4f, 17.4f)
                cubicTo(3.4f, 11.4f, 7f, 6.4f, 12f, 6.4f)
                cubicTo(17f, 6.4f, 20.6f, 11.4f, 20f, 17.4f)
            }
            line(12f, 14.4f, 15.6f, 10.6f)
            drawCircle(color, 1.4f, Offset(12f, 14.6f))
        }
        YcIcon.Screen -> {
            drawRoundRect(
                color = color,
                topLeft = Offset(2.8f, 4.6f),
                size = Size(18.4f, 12.6f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.2f),
                style = stroke
            )
            line(8.4f, 20.2f, 15.6f, 20.2f)
            line(12f, 17.2f, 12f, 20.2f)
        }
        YcIcon.Saved -> {
            path {
                moveTo(5f, 4.4f); lineTo(19f, 4.4f); lineTo(19f, 20f)
                lineTo(12f, 15.4f); lineTo(5f, 20f); close()
            }
        }
    }
}
