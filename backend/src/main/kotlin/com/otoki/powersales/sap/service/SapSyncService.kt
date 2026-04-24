package com.otoki.powersales.sap.service

import com.otoki.powersales.sap.dto.SapSyncResult

interface SapSyncService<T> {
    fun sync(items: List<T>): SapSyncResult
}
