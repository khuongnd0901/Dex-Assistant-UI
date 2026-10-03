package com.khuongnd.dexassistant.ui
import android.content.Context
internal fun restoreVoicePolicy(context: Context, original: Map<String, *>) {
    val edit = context.getSharedPreferences("voice_policy",0).edit().clear()
    original.forEach { (key,value) -> when(value) {
        is String -> edit.putString(key,value)
        is Boolean -> edit.putBoolean(key,value)
        is Int -> edit.putInt(key,value)
        is Long -> edit.putLong(key,value)
        is Float -> edit.putFloat(key,value)
        is Set<*> -> @Suppress("UNCHECKED_CAST") edit.putStringSet(key,value as Set<String>)
    } }
    org.junit.Assert.assertTrue(edit.commit())
}
