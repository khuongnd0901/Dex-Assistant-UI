package com.khuongnd.dexassistant.ui.screen

import org.junit.Assert.*
import org.junit.Test

class NumberedScreenTest {
    @Test fun recurringContentPreservesFrozenTargetsButFreshIdentityAndTopologyChangesInvalidate() {
        data class Frozen(val bounds: ScreenRect, val nodeIdentity: String, val windows: List<Int>)
        val frozen = Frozen(ScreenRect(20, 30, 80, 90), "window1/node3/action16/labelHash4", listOf(1, 2))
        repeat(100) { assertFalse(ScreenContentStability.invalidates(frozen, frozen.copy())) }
        assertTrue(ScreenContentStability.invalidates(frozen, frozen.copy(bounds = ScreenRect(21,30,81,90))))
        assertTrue(ScreenContentStability.invalidates(frozen, frozen.copy(nodeIdentity = "replacement")))
        assertTrue(ScreenContentStability.invalidates(frozen, frozen.copy(windows = listOf(1,2,3))))
        assertTrue(ScreenContentStability.invalidates(frozen, null))
        assertTrue(ScreenContentStability.invalidates(frozen, frozen, windowStateChanged = true))
    }
    @Test fun vietnameseWholeInputAndLocalReservation() {
        assertEquals(ScreenCommand(ScreenVerb.CLICK_TARGET, 1), ScreenCommandParser.parse("bấm vào số một"))
        assertEquals(ScreenCommand(ScreenVerb.CLICK_CELL, 11), ScreenCommandParser.parse("bấm ô mười một"))
        assertEquals(ScreenCommand(ScreenVerb.CLICK_CELL, 24), ScreenCommandParser.parse("bấm ô hai mươi tư."))
        assertEquals(ScreenCommand(ScreenVerb.SUBDIVIDE, 11), ScreenCommandParser.parse("chia ô 11"))
        listOf("bấm ô 11 rồi mở youtube", "hủy rồi mở youtube", "mở lưới rồi bấm", "không hiện lưới", "không bấm 11", "bấm số 0", "bấm ô một trăm").forEach {
            assertEquals(it, ScreenVerb.INVALID, ScreenCommandParser.parse(it)?.verb)
        }
        assertNull(ScreenCommandParser.parse("mở youtube"))
        assertEquals(ScreenCommand(ScreenVerb.CLICK_TARGET, 101), ScreenCommandParser.parse("bấm số một trăm linh một"))
    }
    @Test fun visualRowsAreNotSplitAtPixelBandBoundaries() {
        val left = ScreenRect(10, 33, 30, 55); val right = ScreenRect(60, 31, 80, 50)
        assertEquals(listOf(left, right), ScreenTargets.merge(listOf(right, left), emptyList()))
        val parent = ScreenRect(0, 0, 100, 100)
        assertEquals(listOf(left, right), ScreenTargets.merge(listOf(parent, left, right), emptyList()))
    }
    @Test fun staleSessionsAndDuplicateCommandsCannotConsume() {
        val gate = ScreenSessionGate()
        gate.activate("old"); gate.invalidate(); gate.activate("new")
        assertFalse(gate.consume("old")); assertTrue(gate.consume("new")); assertFalse(gate.consume("new"))
        gate.activate("after-resize"); gate.invalidate(); assertFalse(gate.consume("after-resize"))
    }
    @Test fun gridBoundsAndCoordinates() {
        val screen = ScreenRect(0, 0, 1920, 1080)
        assertNull(screen.cell(0)); assertNull(screen.cell(25))
        assertEquals(ScreenRect(1280, 270, 1600, 540), screen.cell(11))
        assertEquals(1440f, screen.cell(11)!!.x)
        assertEquals(405f, screen.cell(11)!!.y)
        var sub = screen
        repeat(3) { sub = sub.cell(11)!!; assertTrue(sub.inside(screen)) }
        val odd = ScreenRect(0, 0, 1919, 1079)
        assertEquals(odd.right, odd.cell(24)!!.right)
        assertEquals(odd.bottom, odd.cell(24)!!.bottom)
    }
    @Test fun occlusionAndDuplicateSuppression() {
        val first = ScreenRect(10, 10, 30, 30); val second = ScreenRect(60, 10, 80, 30)
        assertEquals(listOf(second), ScreenTargets.visible(listOf(first, second), listOf(ScreenRect(0, 0, 40, 40)), ScreenRect(0, 0, 100, 100)))
        assertEquals(listOf(first, second), ScreenTargets.merge(listOf(second, first, first), listOf(ScreenRect(12, 12, 28, 28))))
    }
    @Test fun lightDarkXAtMultipleSizesAndTextureRejection() {
        for (size in listOf(8, 12, 16, 24)) for (dark in listOf(false, true)) {
            val background = if (dark) 0xffeeeeee.toInt() else 0xff111111.toInt()
            val ink = if (dark) 0xff111111.toInt() else 0xffeeeeee.toInt()
            val pixels = IntArray(80 * 80) { background }; val h = size / 2
            for (offset in -h..h) { pixels[(40 + offset) * 80 + 40 + offset] = ink; pixels[(40 - offset) * 80 + 40 + offset] = ink }
            assertTrue("size=$size dark=$dark", ScreenImageDetector.detect(pixels, 80, 80).any { it.left < 40 && it.right > 40 })
        }
        val texture = IntArray(80 * 80) { if ((it % 80 + it / 80) % 2 == 0) 0xffffffff.toInt() else 0xff000000.toInt() }
        assertTrue(ScreenImageDetector.detect(texture, 80, 80).isEmpty())
    }
    @Test fun thickDownsampledFixtureXAndOddCenters() {
        for (cx in listOf(39, 40, 41)) for (cy in listOf(39, 40, 41)) {
            val pixels = IntArray(80 * 80) { 0xff111111.toInt() }
            val half = 12
            for (y in cy - half..cy + half) for (x in cx - half..cx + half) {
                val dx = x - cx; val dy = y - cy
                if (kotlin.math.abs(dx - dy) <= 2 || kotlin.math.abs(dx + dy) <= 2) pixels[y * 80 + x] = -1
            }
            assertTrue("Thick X center=($cx,$cy)", ScreenImageDetector.detect(pixels, 80, 80).any { it.x in cx - 2f..cx + 2f && it.y in cy - 2f..cy + 2f })
        }
    }
    @Test fun unlabeledOutlineButton() {
        val pixels = IntArray(80 * 80) { 0xff111111.toInt() }
        for (x in 20..60) { pixels[20 * 80 + x] = -1; pixels[40 * 80 + x] = -1 }
        for (y in 20..40) { pixels[y * 80 + 20] = -1; pixels[y * 80 + 60] = -1 }
        assertTrue(ScreenImageDetector.detect(pixels, 80, 80).any { it == ScreenRect(20, 20, 61, 41) })
    }
    @org.junit.Test fun unrelatedDefaultMetadataPreservesDesktopButIdentityResizeAndDuplicatesReject() {
        val desktop=ScreenRect(0,0,1920,1080)
        org.junit.Assert.assertTrue(ScreenDisplays.matches(2,desktop,listOf(0 to ScreenRect(0,0,1768,2208),2 to desktop)))
        org.junit.Assert.assertFalse(ScreenDisplays.matches(2,desktop,listOf(2 to ScreenRect(0,0,1080,1920))))
        org.junit.Assert.assertFalse(ScreenDisplays.matches(2,desktop,listOf(3 to desktop)))
        org.junit.Assert.assertFalse(ScreenDisplays.matches(2,desktop,listOf(2 to desktop,3 to desktop)))
        org.junit.Assert.assertFalse(ScreenDisplays.matches(2,desktop,emptyList()))
    }

}
