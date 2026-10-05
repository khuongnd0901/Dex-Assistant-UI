package com.khuongnd.dexassistant.ui

import com.khuongnd.dexassistant.ui.screen.*
import org.junit.Assert.*
import org.junit.Test

class AppPairLayoutTest {
    private val display = ScreenRect(0,0,1920,1080)
    @Test fun acceptsTwoDistinctSideBySideApplicationWindows() {
        assertTrue(AppPairLayout.verified(listOf(
            DesktopUiAccess.AppWindow("maps",ScreenRect(0,0,960,1080)),
            DesktopUiAccess.AppWindow("youtube",ScreenRect(960,0,1920,1080))),"maps","youtube",display))
    }
    @Test fun rejectsOverlappingMissingOffscreenOrDuplicateWindows() {
        assertFalse(AppPairLayout.verified(listOf(
            DesktopUiAccess.AppWindow("maps",ScreenRect(0,0,1100,1080)),
            DesktopUiAccess.AppWindow("youtube",ScreenRect(900,0,1920,1080))),"maps","youtube",display))
        assertFalse(AppPairLayout.verified(listOf(DesktopUiAccess.AppWindow("maps",ScreenRect(0,0,960,1080))),"maps","youtube",display))
        assertFalse(AppPairLayout.verified(listOf(
            DesktopUiAccess.AppWindow("maps",ScreenRect(0,0,960,1080)),
            DesktopUiAccess.AppWindow("youtube",ScreenRect(960,0,2100,1080))),"maps","youtube",display))
        assertFalse(AppPairLayout.verified(emptyList(),"maps","maps",display))
    }
}
