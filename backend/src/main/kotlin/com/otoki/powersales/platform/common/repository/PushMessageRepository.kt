package com.otoki.powersales.platform.common.repository

import com.otoki.powersales.platform.push.entity.PushMessage
import org.springframework.data.jpa.repository.JpaRepository

interface PushMessageRepository : JpaRepository<PushMessage, Int>
