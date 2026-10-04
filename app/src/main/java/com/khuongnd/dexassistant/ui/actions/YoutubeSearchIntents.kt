package com.khuongnd.dexassistant.ui.actions

import android.content.Intent
import android.net.Uri
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

internal object YoutubeSearchIntents {
    const val PACKAGE_NAME = "com.google.android.youtube"

    fun url(query: String): String = "https://www.youtube.com/results?search_query=" +
        URLEncoder.encode(query, StandardCharsets.UTF_8.name()).replace("+", "%20")

    fun build(query: String, inApp: Boolean = true): Intent =
        Intent(Intent.ACTION_VIEW, Uri.parse(url(query))).apply {
            if (inApp) setPackage(PACKAGE_NAME)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
}
