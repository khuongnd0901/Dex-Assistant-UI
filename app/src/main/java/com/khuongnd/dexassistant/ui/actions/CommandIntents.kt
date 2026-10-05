package com.khuongnd.dexassistant.ui.actions

import android.content.Intent
import android.net.Uri
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/** URI construction is separate from dispatch so a query cannot become another URI parameter. */
object CommandIntents {
    const val MAPS_PACKAGE = "com.google.android.apps.maps"
    private fun encode(text: String) = URLEncoder.encode(text, StandardCharsets.UTF_8.name()).replace("+", "%20")
    fun placeUrl(query: String, inApp: Boolean): String = if (inApp) "geo:0,0?q=${encode(query)}"
        else "https://www.google.com/maps/search/?api=1&query=${encode(query)}"
    fun navigationUrl(destination: String, travelMode: String, inApp: Boolean): String {
        require(travelMode in setOf("DRIVING", "WALKING"))
        return if (inApp) "google.navigation:q=${encode(destination)}&mode=${if (travelMode == "WALKING") "w" else "d"}"
        else "https://www.google.com/maps/dir/?api=1&destination=${encode(destination)}&travelmode=${travelMode.lowercase(java.util.Locale.ROOT)}"
    }
    fun webUrl(query: String): String = "https://www.google.com/search?q=${encode(query)}"
    fun place(query: String, inApp: Boolean) = view(placeUrl(query, inApp), if (inApp) MAPS_PACKAGE else null)
    fun navigate(destination: String, travelMode: String, inApp: Boolean) =
        view(navigationUrl(destination, travelMode, inApp), if (inApp) MAPS_PACKAGE else null)
    fun web(query: String, packageName: String? = null) = view(webUrl(query), packageName)
    private fun view(url: String, packageName: String? = null) = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
        packageName?.let { setPackage(it) }; addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}
