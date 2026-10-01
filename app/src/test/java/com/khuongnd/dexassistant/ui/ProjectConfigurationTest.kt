package com.khuongnd.dexassistant.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class ProjectConfigurationTest {
    @Test
    fun packageNameRemainsStable() {
        assertEquals("com.khuongnd.dexassistant.ui", BuildConfig.APPLICATION_ID)
    }
}
