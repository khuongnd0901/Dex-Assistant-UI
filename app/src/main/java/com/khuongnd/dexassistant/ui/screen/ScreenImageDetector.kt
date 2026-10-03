package com.khuongnd.dexassistant.ui.screen

import kotlin.math.abs

/** Conservative finite template detector. Low confidence deliberately yields no targets (grid fallback).
 * Pixels are supplied only from an in-memory, downsampled screenshot. No OCR or network path. */
object ScreenImageDetector {
    fun detect(pixels: IntArray, width: Int, height: Int): List<ScreenRect> {
        require(pixels.size == width * height)
        if (width < 16 || height < 16) return emptyList()
        fun light(x: Int, y: Int): Int { val p = pixels[y * width + x]; return (((p shr 16) and 255) * 3 + ((p shr 8) and 255) * 6 + (p and 255)) / 10 }
        val result = mutableListOf<ScreenRect>()
        // Close glyphs, light or dark, isolated on uniform background, several sizes.
        for (size in listOf(8, 12, 16, 24)) {
            val half = size / 2
            for (cy in half + 2 until height - half - 2) for (cx in half + 2 until width - half - 2) {
                val rect = ScreenRect(cx - half - 2, cy - half - 2, cx + half + 2, cy + half + 2)
                if (result.any { it.intersects(rect) }) continue
                val background = light(cx - half - 2, cy)
                var hits = 0; var misses = 0; var noise = 0
                for (offset in -half..half) {
                    val a = abs(light(cx + offset, cy + offset) - background)
                    val b = abs(light(cx + offset, cy - offset) - background)
                    if (a >= 90 && b >= 90) hits++ else misses++
                    if (abs(offset) >= maxOf(4, half / 2) && (abs(light(cx + offset, cy) - background) > 35 ||
                        abs(light(cx, cy + offset) - background) > 35)) noise++
                    if (abs(light(cx + offset, cy - half - 2) - background) > 35 ||
                        abs(light(cx + offset, cy + half + 2) - background) > 35) noise++
                }
                if (hits >= size - 1 && misses <= 2 && noise == 0) result.add(rect)
                if (result.size >= 64) return result
            }
        }
        // Axis-aligned button outlines: four uniform contrasting edges, quiet outside corners.
        // Finite sampling rejects patterned regions; only small control-sized rectangles are eligible.
        for (top in 2 until height - 14) for (left in 2 until width - 18) {
            if (result.size >= 64) return result
            val bg = light(left - 2, top - 2)
            if (abs(light(left, top) - bg) < 90) continue
            var right = left + 1
            while (right < minOf(width - 2, left + 161) && abs(light(right, top) - bg) >= 90) right++
            right--
            if (right - left !in 16..160) continue
            var bottom = top + 1
            while (bottom < minOf(height - 2, top + 65) && abs(light(left, bottom) - bg) >= 90) bottom++
            bottom--
            if (bottom - top !in 12..64) continue
            val rect = ScreenRect(left, top, right + 1, bottom + 1)
            if (result.any { it.intersects(rect) }) continue
            val horizontal = (left..right).all { x -> abs(light(x, bottom) - bg) >= 90 && abs(light(x, top - 2) - bg) < 35 && abs(light(x, bottom + 2) - bg) < 35 }
            val vertical = (top..bottom).all { y -> abs(light(right, y) - bg) >= 90 && abs(light(left - 2, y) - bg) < 35 && abs(light(right + 2, y) - bg) < 35 }
            // Interior must be quiet away from the glyph area. Ambiguous textures return no target.
            val corners = listOf(left + 3 to top + 3, right - 3 to top + 3, left + 3 to bottom - 3, right - 3 to bottom - 3)
            if (horizontal && vertical && corners.all { (x, y) -> abs(light(x, y) - bg) < 35 }) result.add(rect)
        }
        return result
    }
}
