package com.otoki.internal.repository

import com.otoki.internal.entity.DeviceVersion
import com.otoki.internal.entity.DeviceVersionId
import org.springframework.data.jpa.repository.JpaRepository

interface DeviceVersionRepository : JpaRepository<DeviceVersion, DeviceVersionId>
