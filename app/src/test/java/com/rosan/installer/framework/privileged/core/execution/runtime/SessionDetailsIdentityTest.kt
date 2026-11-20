// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.framework.privileged.core.execution.runtime

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SessionDetailsIdentityTest {
    @Test
    fun `external confirmation uses archive and installer metadata independently`() {
        val installer = "com.google.android.apps.nbu.files"
        val target = resolveTargetPackage(" ", "com.example.app")
        val identity = resolveSessionIdentity(target, "", "Example", installer, false, null)
        assertEquals("com.example.app", identity.packageName)
        assertEquals("Example", identity.label)

        val missing = resolveTargetPackage("", " ")
        assertEquals("", missing)
        assertEquals(installer, resolveSessionIdentity(missing, null, null, installer, false, null).sourcePackageName)
        assertNull(resolveSessionIdentity(missing, null, null, null, false, null).sourcePackageName)

        val conflict = resolveSessionIdentity(target, "Session", "Archive", installer, true, "owner")
        assertEquals("Session", conflict.label)
        assertEquals("owner", conflict.sourcePackageName)
    }
}
