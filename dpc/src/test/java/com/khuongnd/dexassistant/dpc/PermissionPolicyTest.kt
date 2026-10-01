package com.khuongnd.dexassistant.dpc

import org.junit.Assert.assertEquals
import org.junit.Test

class PermissionPolicyTest {
    @Test fun `already granted dangerous permission is granted`() =
        assertEquals(PermissionPolicy.GRANTED, PermissionPolicy.classify(true, true, true, null))

    @Test fun `normal and unrequested permissions are unsupported`() {
        assertEquals(PermissionPolicy.UNSUPPORTED, PermissionPolicy.classify(true, false, false, null))
        assertEquals(PermissionPolicy.UNSUPPORTED, PermissionPolicy.classify(false, true, false, null))
    }

    @Test fun `failed grant is not reported as success`() =
        assertEquals(PermissionPolicy.FAILED, PermissionPolicy.classify(true, true, false, false))

    @Test fun `permission requiring intervention is explicit`() =
        assertEquals(PermissionPolicy.USER_ACTION_REQUIRED, PermissionPolicy.classify(true, true, false, null))
}
