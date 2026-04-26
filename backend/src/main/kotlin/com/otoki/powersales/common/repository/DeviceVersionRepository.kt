package com.otoki.powersales.common.repository

import com.otoki.powersales.common.entity.DeviceVersion
import com.otoki.powersales.common.entity.DeviceVersionId
import org.springframework.data.jpa.repository.JpaRepository

interface DeviceVersionRepository : JpaRepository<DeviceVersion, DeviceVersionId>
