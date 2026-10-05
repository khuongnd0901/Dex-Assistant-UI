package com.khuongnd.dexassistant.ui.localdata

import android.content.Context
import java.text.Normalizer
import java.util.Locale

object AliasRules {
    fun key(value: String): String = Normalizer.normalize(value, Normalizer.Form.NFC)
        .lowercase(Locale.forLanguageTag("vi-VN")).trim().replace(Regex("\\s+"), " ")
    fun placeKey(value: String): String? = when (key(value)) {
        "nhà", "nha", "home" -> "home"
        "công ty", "cong ty", "work" -> "work"
        else -> null
    }
    fun resolvePlace(value: String, saved: (String) -> String?): String? =
        placeKey(value)?.let(saved)?.takeIf { it.isNotBlank() } ?: if (placeKey(value) == null) value else null
}

/** User-local aliases are never enumerated into capability descriptors or uploaded. */
class LocalAliases(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences("command_local_aliases", Context.MODE_PRIVATE)
    fun place(name: String): String? = AliasRules.resolvePlace(name) { preferences.getString("place.$it", null) }
    fun savedPlace(id: String): String = preferences.getString("place.$id", "").orEmpty()
    fun savePlace(id: String, address: String) {
        require(id == "home" || id == "work"); require(address.length <= 500)
        preferences.edit().putString("place.$id", address.trim()).apply()
    }
    fun contact(name: String): String = preferences.getString("contact.${AliasRules.key(name)}", null) ?: name
    fun saveContact(alias: String, exactName: String) {
        require(alias.isNotBlank() && alias.length <= 200 && exactName.length <= 200)
        val editor = preferences.edit()
        if (exactName.isBlank()) editor.remove("contact.${AliasRules.key(alias)}")
        else editor.putString("contact.${AliasRules.key(alias)}", exactName.trim())
        editor.apply()
    }
}
