package com.otoki.internal.sap.service

import com.otoki.internal.sap.dto.SapSyncResult

interface SapSyncService<T> {
    fun sync(items: List<T>): SapSyncResult
}
