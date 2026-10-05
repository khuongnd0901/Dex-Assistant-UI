package com.khuongnd.dexassistant.ui

import android.app.Application
import com.khuongnd.dexassistant.ui.routing.CommandRegistry

/** Compile the build-validated bundled registry once before any UI, voice or test caller. */
class DexAssistantApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        CommandRegistry.load(this)
    }
}
