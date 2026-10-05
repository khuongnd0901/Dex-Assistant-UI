package com.khuongnd.dexassistant.ui.screen

/** Success requires two distinct configured app windows visible and disjoint on one desktop. */
object AppPairLayout {
    fun verified(windows: List<DesktopUiAccess.AppWindow>, firstPackage: String?, secondPackage: String?, display: ScreenRect): Boolean {
        if (firstPackage.isNullOrBlank() || secondPackage.isNullOrBlank() || firstPackage == secondPackage) return false
        val first = windows.filter { it.packageName == firstPackage }
        val second = windows.filter { it.packageName == secondPackage }
        return first.any { left -> second.any { right ->
            left.bounds.inside(display) && right.bounds.inside(display) && !left.bounds.intersects(right.bounds) &&
                left.bounds.width >= display.width * 0.30f && right.bounds.width >= display.width * 0.30f
        } }
    }
}
