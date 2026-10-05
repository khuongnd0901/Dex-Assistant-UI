package com.khuongnd.dexassistant.ui.localdata

import com.khuongnd.dexassistant.ui.media.SeekBounds
import org.junit.Assert.*
import org.junit.Test

class LocalIdentityTest {
    @Test fun unsetHomeNeverBecomesGuessedDestination() {
        assertNull(AliasRules.resolvePlace("nhà") { null })
        assertNull(AliasRules.resolvePlace("công ty") { " " })
        assertEquals("12 Đường A", AliasRules.resolvePlace("Nhà") { "12 Đường A" })
        assertEquals("Cửa hàng A", AliasRules.resolvePlace("Cửa hàng A") { null })
    }
    @Test fun exactContactKeysPreserveAccentsAndNormalizeNfc() {
        assertEquals(AliasRules.key("Nguyễn Văn A"), AliasRules.key("  NGUYỄN   VĂN A "))
        assertNotEquals(AliasRules.key("Nguyễn Văn A"), AliasRules.key("Nguyen Van A"))
    }
    @Test fun contactNumberAcceptsFormattingButRejectsDialerCodeAndExtensions() {
        assertEquals("+84901234567", ContactIdentity.normalizedNumber("+84 (90) 123-4567"))
        listOf("*123#", "09012,34", "09012;34", "09012 ext 34", "++8490123", "12", "1234567890123456").forEach {
            assertNull(it, ContactIdentity.normalizedNumber(it))
        }
    }
    @Test fun changedNameNumberOrIdentityBlocksConfirmation() {
        val selected = ContactCandidate(12, 3, "lookup", "Nguyễn Văn A", "0901234567")
        assertTrue(ContactIdentity.unchanged(selected, selected.copy()))
        listOf(selected.copy(rowId = 13), selected.copy(contactId = 4), selected.copy(lookupKey = "changed"),
            selected.copy(name = "Nguyễn Văn B"), selected.copy(number = "0901234568"), null).forEach {
            assertFalse(ContactIdentity.unchanged(selected, it))
        }
    }
    @Test fun seekUsesPlaybackProgressAndClampsToTrackBounds() {
        assertEquals(17_000L, SeekBounds.target(5_000, 2_000, 1f, true, 30_000, 10, true))
        assertEquals(0L, SeekBounds.target(5_000, 2_000, 1f, false, 30_000, 10, false))
        assertEquals(30_000L, SeekBounds.target(25_000, 2_000, 1f, true, 30_000, 10, true))
    }
}
