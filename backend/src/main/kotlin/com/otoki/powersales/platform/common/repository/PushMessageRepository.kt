package com.otoki.powersales.platform.common.repository

import com.otoki.powersales.platform.common.entity.PushMessage
import org.springframework.data.jpa.repository.JpaRepository

interface PushMessageRepository : JpaRepository<PushMessage, Int>
