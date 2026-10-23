package com.khuongnd.dexassistant.ui.auth

import com.khuongnd.dexassistant.ui.security.SubscriptionProof
import org.junit.Assert.*
import org.junit.Test

class SubscriptionProofTest {
    @Test fun proofAuthorizesOnlyItsAccountRegistrationAndModel() {
        val account = "a".repeat(64)
        val proof = SubscriptionProof.value(account, "account-model")
        assertTrue(SubscriptionProof.matches(proof, account, "account-model"))
        assertFalse(SubscriptionProof.matches(proof, "b".repeat(64), "account-model"))
        assertFalse(SubscriptionProof.matches(proof, account, "other-model"))
        assertFalse(SubscriptionProof.matches(null, account, "account-model"))
        assertFalse(SubscriptionProof.matches(proof, null, "account-model"))
        assertFalse(SubscriptionProof.matches(proof, account, ""))
        assertFalse(SubscriptionProof.matches(proof, "invalid", "account-model"))
    }
}
