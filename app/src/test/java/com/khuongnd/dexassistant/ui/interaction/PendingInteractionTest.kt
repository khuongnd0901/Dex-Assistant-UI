package com.khuongnd.dexassistant.ui.interaction

import org.junit.Assert.*
import org.junit.Test

class PendingInteractionTest {
    @Test fun selectionNeedsNewTtsAndSubsequentWholeReply() {
        val pending = PendingInteraction("parent", "call", listOf("A", "B"))
        assertEquals(InteractionResult.Invalid, pending.reply(SpokenReply.Confirm, 0))
        assertTrue(pending.ttsFinished(1, true, 100))
        assertEquals(InteractionResult.Invalid, pending.reply(SpokenReply.Select(3), 200))
        assertEquals(InteractionResult.Selected("B"), pending.reply(SpokenReply.Select(2), 200))
        assertFalse(pending.ttsFinished(1, true, 250))
        assertEquals(InteractionResult.Invalid, pending.reply(SpokenReply.Confirm, 300))
        assertTrue(pending.ttsFinished(2, true, 400))
        assertEquals(InteractionResult.Confirmed("B"), pending.reply(SpokenReply.Confirm, 500))
        assertEquals(InteractionResult.Invalid, pending.reply(SpokenReply.Confirm, 501))
    }
    @Test fun timeoutTtsErrorAndCancelAreTerminal() {
        val pending = PendingInteraction("p", "click", listOf("Continue"))
        assertTrue(pending.ttsFinished(1, true, 5))
        assertFalse(pending.expired(10_004))
        assertEquals(InteractionResult.Cancelled, pending.reply(SpokenReply.Confirm, 10_005))
        val failed = PendingInteraction("p", "call", listOf("A"))
        assertFalse(failed.ttsFinished(1, false, 0))
        assertFalse(failed.ttsFinished(1, true, 1))
        val cancelled = PendingInteraction("p", "call", listOf("A"))
        cancelled.ttsFinished(1, true, 0)
        cancelled.cancel()
        assertEquals(InteractionResult.Invalid, cancelled.reply(SpokenReply.Confirm, 1))
    }
    @Test fun newCommandCancelsOldRecipient() {
        val pending = PendingInteraction("p", "call", listOf("A"))
        pending.ttsFinished(1, true, 0)
        assertEquals(InteractionResult.NewCommand, pending.reply(null, 1))
        assertEquals(InteractionResult.Invalid, pending.reply(SpokenReply.Confirm, 2))
    }
    @Test fun pageRepliesExposeAllChoicesWithoutChangingConfirmationRecipient() {
        val pending = PendingInteraction("p", "call", (1..12).toList())
        pending.ttsFinished(1, true, 0)
        assertEquals(InteractionResult.PageChanged(1), pending.reply(SpokenReply.Next, 1))
        assertEquals(listOf(6,7,8,9,10), pending.pageChoices)
        assertFalse(pending.ttsFinished(1, true, 2))
        pending.ttsFinished(2,true,3)
        assertEquals(InteractionResult.Selected(8),pending.reply(SpokenReply.Select(8),4))
        pending.ttsFinished(3,true,5)
        assertEquals(InteractionResult.Invalid,pending.reply(SpokenReply.Next,6))
        assertEquals(InteractionResult.Confirmed(8),pending.reply(SpokenReply.Confirm,7))
    }
    @Test fun duplicateCompletionAndReplyCannotDispatchConsumedRecipient() {
        val pending = PendingInteraction("p", "call", listOf("recipient"))
        assertTrue(pending.ttsFinished(1, true, 10))
        assertFalse(pending.ttsFinished(1, true, 20))
        assertEquals(InteractionResult.Confirmed("recipient"), pending.reply(SpokenReply.Confirm, 30))
        pending.reprompt()
        assertFalse(pending.ttsFinished(pending.promptVersion, true, 40))
        assertEquals(InteractionResult.Invalid, pending.reply(SpokenReply.Confirm, 50))
    }

}
