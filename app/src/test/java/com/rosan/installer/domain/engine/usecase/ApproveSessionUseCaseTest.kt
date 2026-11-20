// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.domain.engine.usecase

import com.rosan.installer.domain.engine.repository.AppInstallerRepository
import com.rosan.installer.domain.history.model.OperationHistoryModel
import com.rosan.installer.domain.history.model.OperationStatus
import com.rosan.installer.domain.history.model.OperationType
import com.rosan.installer.domain.history.repository.OperationHistoryRepository
import com.rosan.installer.domain.history.usecase.RecordOperationHistoryUseCase
import com.rosan.installer.domain.settings.model.config.ConfigModel
import java.lang.reflect.Proxy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class ApproveSessionUseCaseTest {
    @Test
    fun `confirmation history records approvals and IPC failures but skips cancellation`() = runTest {
        var fail = false
        val rows = mutableListOf<OperationHistoryModel>()
        val installer = AppInstallerRepository::class.java
        val history = OperationHistoryRepository::class.java
        val useCase = ApproveSessionUseCase(
            Proxy.newProxyInstance(installer.classLoader, arrayOf(installer)) { _, method, _ ->
                check(method.name == "approveSession")
                if (fail) throw IllegalStateException("IPC failure")
                Unit
            } as AppInstallerRepository,
            RecordOperationHistoryUseCase(
                Proxy.newProxyInstance(history.classLoader, arrayOf(history)) { _, method, args ->
                    check(method.name == "insert")
                    rows.add(args[0] as OperationHistoryModel)
                    Unit
                } as OperationHistoryRepository,
            ),
        )
        val config = ConfigModel.default.copy()
        useCase(1, false, config)
        assertTrue(rows.isEmpty())
        useCase(1, true, config)
        with(rows.single()) {
            assertEquals(OperationType.SESSION_CONFIRM, operationType)
            assertEquals(OperationStatus.SUCCESS, status)
            assertEquals("unknown", packageName)
            assertNull(errorType)
            assertNull(errorSummary)
        }

        fail = true
        for (granted in listOf(true, false)) {
            rows.clear()
            assertFailsWith<IllegalStateException> { useCase(1, granted, config) }
            with(rows.single()) {
                assertEquals(OperationStatus.FAILED, status)
                assertEquals("IllegalStateException", errorType)
                assertEquals("IPC failure", errorSummary)
            }
        }
    }
}
