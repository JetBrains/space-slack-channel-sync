package org.jetbrains.spaceSlackSync.slack

import space.jetbrains.api.runtime.types.MessageStyle
import kotlin.math.sqrt

fun getMessageStyle(color: String?): MessageStyle {
    color ?: return MessageStyle.PRIMARY
    val intColor = Integer.parseInt(color, 16)
    val r = (intColor shr 16) and 0xFF
    val g = (intColor shr 8) and 0xFF
    val b = intColor and 0xFF
    val colorFromSlack = Color(r, g, b)

    return spaceColors.minBy { colorDistance(it.first, colorFromSlack) }.second
}

private val SPACE_PRIMARY = Color(70, 71, 73)
private val SPACE_SUCCESS = Color(45, 64, 51)
private val SPACE_WARNING = Color(79, 67, 35)
private val SPACE_ERROR = Color(77, 49, 49)

private val spaceColors = listOf(
    SPACE_PRIMARY to MessageStyle.PRIMARY,
    SPACE_SUCCESS to MessageStyle.SUCCESS,
    SPACE_WARNING to MessageStyle.WARNING,
    SPACE_ERROR to MessageStyle.ERROR,
)

private fun colorDistance(c1: Color, c2: Color): Double {
    val rmean = (c1.r + c2.r) / 2

    val r = c1.r - c2.r
    val g = c1.g - c2.g
    val b = c1.b - c2.b

    val a = (((512 + rmean) * r * r) shr 8) + 4 * g * g + (((767 - rmean) * b * b) shr 8)
    return sqrt(a.toDouble());
}

private class Color(val r: Int, val g: Int, val b: Int)