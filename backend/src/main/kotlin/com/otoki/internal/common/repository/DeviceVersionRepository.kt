package com.otoki.internal.common.repository

import com.otoki.internal.common.entity.DeviceVersion
import com.otoki.internal.common.entity.DeviceVersionId
import org.springframework.data.jpa.repository.JpaRepository

interface DeviceVersionRepository : JpaRepository<DeviceVersion, DeviceVersionId>
